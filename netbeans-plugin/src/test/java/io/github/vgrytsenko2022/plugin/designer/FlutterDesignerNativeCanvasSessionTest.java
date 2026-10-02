package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterSdk;
import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerBuildResult;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerCacheIdentity;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerLeaseTestSupport;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerProcessChannel;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import io.github.vgrytsenko2022.plugin.designer.canvas.WindowsNativeCanvasPlatformProvider;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatform;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterDesignerNativeCanvasSessionTest {
    @Test
    void deadParentHandleDuringLaunchPreparationFailsWithoutStartingAProcess()
            throws Exception {
        Harness harness = Harness.create("dead-parent-prepare", 0x107L);

        onEdt(() -> {
            harness.host.failParentWindowHandleOnCall(1);
            harness.session.show();
            harness.build.complete(harness.runner);
        });

        assertEquals(0, harness.launches.pendingCount());
        assertTrue(harness.processes.commands.isEmpty());
        assertTrue(harness.terminated.isEmpty());
        assertFalse(harness.polls.hasActivePoll());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains("native peer disappeared"));
    }

    @Test
    void deadParentHandleAfterProcessStartTerminatesBeforeOwnership()
            throws Exception {
        Harness harness = Harness.create("dead-parent-started", 0x108L);
        FakeProcess process = new FakeProcess(1108L);

        onEdt(() -> {
            harness.host.failParentWindowHandleOnCall(2);
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertTrue(harness.host.attachPids.isEmpty());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains("native peer disappeared"));
    }

    @Test
    void newlineFreeRunnerOutputIsBoundedBeforeAWholeLineCanBeAllocated()
            throws Exception {
        byte[] output = "x".repeat(512 * 1024).getBytes(StandardCharsets.UTF_8);

        String diagnostics = FlutterDesignerNativeCanvasSession.readBoundedDiagnostics(
                new ByteArrayInputStream(output));

        assertTrue(diagnostics.length() <= 64 * 1024);
        assertTrue(diagnostics.endsWith("[output truncated]"));
    }

    @Test
    void closeDuringBuildFencesTheLateBuildCompletion() throws Exception {
        Harness harness = Harness.create("close-build", 0x101L);

        onEdt(harness.session::show);
        onEdt(harness.session::close);
        onEdt(() -> harness.build.complete(harness.runner));

        assertEquals(List.of(
                FlutterDesignerNativeCanvasStatus.Stage.PREPARING,
                FlutterDesignerNativeCanvasStatus.Stage.STOPPED), harness.stages());
        assertEquals(0, harness.launches.pendingCount());
        assertTrue(harness.processes.commands.isEmpty());
        assertTrue(harness.terminated.isEmpty());
        assertEquals(0, harness.host.detachCalls);
        assertEquals(1, harness.host.closeCalls);
        assertTrue(harness.runner.runtimeLease().isClosed());
    }

    @Test
    void lateFocusClearAfterCloseCannotRepublishInteractionBarrier() throws Exception {
        Harness harness = Harness.create("late-focus-clear", 0x109L);
        List<FlutterDesignerNativeCanvasSession.InteractionBarrierState> states =
                new ArrayList<>();

        onEdt(() -> {
            harness.session.setInteractionBarrierListener(states::add);
            harness.session.close();
            FlutterDesignerNativeCanvasSession.InteractionBarrierState terminal =
                    harness.session.interactionBarrierState();
            int publicationsAfterClose = states.size();

            harness.session.clearFocusRequest();

            assertEquals(terminal, harness.session.interactionBarrierState());
            assertEquals(publicationsAfterClose, states.size(),
                    "late focus clear must not republish a closed barrier");
        });
    }

    @Test
    void closeKeepsTheRuntimeGenerationLeasedUntilTheProcessPhysicallyExits(
            @TempDir Path temporaryDirectory) throws Exception {
        CanvasRunnerBuildResult leasedRunner =
                CanvasRunnerLeaseTestSupport.createResult(temporaryDirectory);
        Harness harness = Harness.create("leased-process", 0x10bL, leasedRunner);
        FakeProcess process = new FakeProcess(1111L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.session.close();
        });

        assertTrue(leasedRunner.runtimeLease().isClosed());
        assertTrue(process.isAlive());
        assertTrue(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertTrue(harness.terminated.isEmpty());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.STOPPED));

        onEdt(() -> {
            assertEquals(1, harness.launches.pendingCount());
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);

        onEdt(() -> process.exit(0));

        assertFalse(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                harness.statuses.getLast().stage());
    }

    @Test
    void exceptionalOnExitObservationCannotReleaseTheLeaseOfALiveProcess(
            @TempDir Path temporaryDirectory) throws Exception {
        CanvasRunnerBuildResult leasedRunner =
                CanvasRunnerLeaseTestSupport.createResult(temporaryDirectory);
        Harness harness = Harness.create("failed-exit-observation", 0x10cL, leasedRunner);
        FakeProcess process = new FakeProcess(1112L);
        process.failNextOnExitObservation();

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.session.close();
            harness.launches.runNext();
        });

        assertTrue(process.isAlive());
        assertTrue(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertEquals(List.of(process), harness.terminated);

        onEdt(() -> process.exit(0));
        awaitCondition(
                () -> !CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory),
                "runtime generation lease was not released after physical process exit");
    }

    @Test
    void synchronousOnExitFailureFallsBackToPhysicalExitObservation(
            @TempDir Path temporaryDirectory) throws Exception {
        CanvasRunnerBuildResult leasedRunner =
                CanvasRunnerLeaseTestSupport.createResult(temporaryDirectory);
        Harness harness = Harness.create(
                "synchronous-exit-observation-failure", 0x10dL, leasedRunner);
        FakeProcess process = new FakeProcess(1212L);
        process.throwNextOnExitObservation();

        onEdt(() -> {
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            harness.session.close();
            harness.launches.runNext();
        });

        assertTrue(process.isAlive());
        assertTrue(CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory));
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.STOPPED));

        onEdt(() -> process.exit(0));
        awaitCondition(
                () -> !CanvasRunnerLeaseTestSupport.isGenerationLeased(temporaryDirectory),
                "runtime lease remained retained after fallback observed physical exit");
        awaitCondition(
                () -> harness.statuses.getLast().stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                "STOPPED was not published after fallback observed physical exit");
    }

    @Test
    void cleanupFailureBecomesFreshRetryReadyStatusOnlyAfterPhysicalExit()
            throws Exception {
        Harness harness = Harness.create("retry-after-cleanup-failure", 0x10eL);
        FakeProcess process = new FakeProcess(1213L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.terminationFailure = new IllegalStateException(
                    "simulated async cleanup failure");
            harness.host.fireAttachmentFailed("simulated surface ownership loss");
        });

        assertFalse(onEdt(harness.session::canRestart));
        assertTrue(process.isAlive());
        assertTrue(harness.statuses.getLast().detail().contains(
                "Retry remains unavailable while that process is alive"));

        onEdt(() -> process.exit(0));

        assertTrue(onEdt(harness.session::canRestart));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "Every previous runner PID has now physically exited"));
        assertTrue(harness.statuses.getLast().detail().contains(
                "Retry is available"));
        assertFalse(harness.statuses.getLast().detail().contains(
                "Retry remains unavailable while that process is alive"));
    }

    @Test
    void changedNativeSurfaceMetricsRefreshRunningStatusDetail() throws Exception {
        Harness harness = Harness.create("surface-metrics-refresh", 0x10fL);
        FakeProcess process = new FakeProcess(1214L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.fireSurfaceMetrics(
                    new NativeCanvasSurfaceMetrics(1_200, 900, 1_500_000));
        });

        FlutterDesignerNativeCanvasStatus status = harness.statuses.getLast();
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING, status.stage());
        assertTrue(status.detail().contains("1200×900"));
        assertTrue(status.detail().contains("1.5× native scale"));
    }

    @Test
    void peerLossWhileLaunchIsInFlightTerminatesTheLateProcess() throws Exception {
        Harness harness = Harness.create("peer-loss-launch", 0x102L);
        FakeProcess process = new FakeProcess(1102L);

        onEdt(() -> {
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            assertEquals(1, harness.launches.pendingCount());
            harness.host.firePeerLost();
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertTrue(harness.host.attachPids.isEmpty());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
    }

    @Test
    void peerLossDuringAttachCancelsPollingAndTerminatesOnlyThatGeneration() throws Exception {
        Harness harness = Harness.create("peer-loss-attach", 0x103L);
        FakeProcess process = new FakeProcess(1103L);

        onEdt(() -> {
            harness.startAttaching(process);
            assertTrue(harness.polls.hasActivePoll());
            harness.host.firePeerLost();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
    }

    @Test
    void processExitDuringAttachFailsOnceAndClosesTheHostAttachment() throws Exception {
        Harness harness = Harness.create("exit-attach", 0x104L);
        FakeProcess process = new FakeProcess(1104L);

        onEdt(() -> {
            harness.startAttaching(process);
            process.exit(23);
        });

        assertFalse(harness.polls.hasActivePoll());
        assertEquals(1, harness.host.detachCalls);
        assertEquals(0, harness.host.closeCalls);
        assertTrue(harness.terminated.isEmpty());
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.FAILED)
                .count());
        FlutterDesignerNativeCanvasStatus failure = harness.statuses.getLast();
        assertTrue(failure.detail().contains("exited with code 23"));
    }

    @Test
    void terminalCrashWaitsForExplicitRestartAndUsesAFreshProcessGeneration()
            throws Exception {
        Harness harness = Harness.create("explicit-restart", 0x10dL);
        FakeProcess first = new FakeProcess(1113L);
        FakeProcess second = new FakeProcess(2113L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(first);
            harness.polls.runActivePoll();
            first.exit(41);
            assertTrue(harness.session.canRestart());
            harness.session.show();
            assertEquals(0, harness.launches.pendingCount());

            harness.processes.enqueue(second);
            assertTrue(harness.session.restart());
            assertFalse(harness.session.canRestart());
            assertEquals(1, harness.launches.pendingCount());
            harness.launches.runNext();
            harness.polls.runActivePoll();
        });

        assertEquals(2, harness.processes.commands.size());
        assertFalse(harness.processes.commands.get(0).getLast().equals(
                harness.processes.commands.get(1).getLast()));
        assertEquals(List.of(first.pid(), second.pid()), harness.host.attachPids);
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.FAILED)
                .count());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                harness.statuses.getLast().stage());
    }

    @Test
    void transientFocusFailureDoesNotTerminateOrFailAHealthyRunner()
            throws Exception {
        Harness harness = Harness.create("focus-policy", 0x10fL);
        FakeProcess process = new FakeProcess(1115L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.focusFailure = new IllegalStateException(
                    "simulated foreground policy refusal");
            harness.session.requestFocus();
        });

        assertTrue(process.isAlive());
        assertTrue(harness.host.attached);
        assertTrue(harness.terminated.isEmpty());
        assertFalse(onEdt(harness.session::canRestart));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                harness.statuses.getLast().stage());
        onEdt(harness.session::clearFocusRequest);
    }

    @Test
    void policyRefusalIsRetriedAndEventuallyFocusesCurrentGeneration()
            throws Exception {
        Harness harness = Harness.create("focus-policy-retry", 0x112L);
        FakeProcess process = new FakeProcess(1118L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.focusRefusalsRemaining = 1;
            harness.session.requestFocus();
            assertEquals(1, harness.host.focusCalls);
        });

        awaitCondition(
                () -> harness.host.focusCalls >= 2,
                "a transient policy refusal must receive a bounded retry");
        assertTrue(harness.host.focused);
        assertTrue(process.isAlive());
        assertTrue(harness.terminated.isEmpty());
    }

    @Test
    void clearingFocusIntentCancelsPendingPolicyRetry() throws Exception {
        Harness harness = Harness.create("cancel-focus-policy-retry", 0x113L);
        FakeProcess process = new FakeProcess(1119L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.focusRefusalsRemaining = 100;
            harness.session.requestFocus();
            assertEquals(1, harness.host.focusCalls);
            harness.session.clearFocusRequest();
        });

        Thread.sleep(150);
        assertEquals(1, harness.host.focusCalls);
        assertFalse(harness.host.focused);
        assertTrue(process.isAlive());
    }

    @Test
    void persistentPolicyRefusalStopsAtTheBoundedAttemptLimit()
            throws Exception {
        Harness harness = Harness.create("bounded-focus-policy-retry", 0x114L);
        FakeProcess process = new FakeProcess(1120L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.focusRefusalsRemaining = 100;
            harness.session.requestFocus();
        });

        awaitCondition(
                () -> harness.host.focusCalls
                        == FlutterDesignerNativeCanvasSession.MAX_FOCUS_RETRY_ATTEMPTS,
                "focus retries must reach their explicit bounded limit");
        Thread.sleep(150);
        assertEquals(
                FlutterDesignerNativeCanvasSession.MAX_FOCUS_RETRY_ATTEMPTS,
                harness.host.focusCalls);
        assertFalse(harness.host.focused);
        assertTrue(process.isAlive());
        assertTrue(harness.terminated.isEmpty());
        onEdt(harness.session::clearFocusRequest);
    }

    @Test
    void activationBeforeAttachmentIsReplayedToTheExactAttachedGeneration()
            throws Exception {
        Harness harness = Harness.create("deferred-focus", 0x110L);
        FakeProcess process = new FakeProcess(1116L);

        onEdt(() -> {
            harness.startAttaching(process);
            harness.session.requestFocus();
            assertEquals(0, harness.host.focusCalls);
            harness.host.attachAvailable = true;
            harness.polls.runActivePoll();
        });

        assertEquals(1, harness.host.focusCalls);
        assertTrue(harness.host.focused);
        assertTrue(process.isAlive());
        assertTrue(harness.terminated.isEmpty());
    }

    @Test
    void deactivationBeforeLateAttachmentCancelsDeferredFocus()
            throws Exception {
        Harness harness = Harness.create("cancel-deferred-focus", 0x111L);
        FakeProcess process = new FakeProcess(1117L);

        onEdt(() -> {
            harness.startAttaching(process);
            harness.session.requestFocus();
            harness.session.clearFocusRequest();
            harness.host.attachAvailable = true;
            harness.polls.runActivePoll();
        });

        assertEquals(0, harness.host.focusCalls);
        assertFalse(harness.host.focused);
        assertTrue(process.isAlive());
        assertTrue(harness.host.attached);
    }

    @Test
    void closeWhileLaunchIsQueuedFencesAndTerminatesTheLateProcess()
            throws Exception {
        Harness harness = Harness.create("close-queued-launch", 0x10eL);
        FakeProcess process = new FakeProcess(1114L);

        onEdt(() -> {
            harness.processes.enqueue(process);
            harness.session.show();
            harness.build.complete(harness.runner);
            assertEquals(1, harness.launches.pendingCount());
            harness.session.close();
            harness.launches.runNext();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertTrue(process.isAlive());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "STOPPED will not be reported"));

        onEdt(() -> process.exit(0));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                harness.statuses.getLast().stage());
    }

    @Test
    void hostCleanupFailureOnProcessExitIsContainedAndReported() throws Exception {
        Harness harness = Harness.create("exit-cleanup-failure", 0x10fL);
        FakeProcess process = new FakeProcess(1115L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.host.detachFailure = new IllegalStateException(
                    "simulated native handle cleanup failure");
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            assertDoesNotThrow(() -> process.exit(52));
        });

        assertTrue(onEdt(harness.session::canRestart));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated native handle cleanup failure"));
    }

    @Test
    void closeWithoutProtocolTerminationFailureNeverEscapesEdtAndRetainsProcess()
            throws Exception {
        Harness harness = Harness.create("close-termination-failure", 0x110L);
        FakeProcess process = new FakeProcess(1116L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.terminationFailure = new IllegalStateException(
                    "simulated bounded termination failure");
            harness.session.close();
            assertDoesNotThrow(harness.launches::runNext);
        });

        assertTrue(process.isAlive());
        assertTrue(harness.terminated.isEmpty());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated bounded termination failure"));

        onEdt(() -> process.exit(0));
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                harness.statuses.getLast().stage());
    }

    @Test
    void hostDetachCallbacksDuringCloseCannotBypassTheGracefulDeadline()
            throws Exception {
        Harness harness = Harness.create("close-detach-callback", 0x111L);
        FakeProcess process = new FakeProcess(1117L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.closeFiresPeerLost = true;
            harness.session.close();
        });

        assertTrue(process.isAlive());
        assertTrue(harness.terminated.isEmpty());
        assertFalse(harness.polls.hasActivePoll());
        assertEquals(1, harness.launches.pendingCount());

        onEdt(harness.launches::runNext);
        assertEquals(List.of(process), harness.terminated);
    }

    @Test
    void hostCleanupFailureDuringCloseIsContainedUntilPhysicalExitAndReported()
            throws Exception {
        Harness harness = Harness.create("close-host-cleanup-failure", 0x112L);
        FakeProcess process = new FakeProcess(1118L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.host.closeFailure = new IllegalStateException(
                    "simulated close-time native cleanup failure");
            assertDoesNotThrow(harness.session::close);
            harness.launches.runNext();
            process.exit(0);
        });

        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.STOPPED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated close-time native cleanup failure"));
    }

    @Test
    void peerRestartWaitsForOldPhysicalExitAndStaleExitCannotTearDownNewGeneration()
            throws Exception {
        Harness harness = Harness.create("stale-exit", 0x105L);
        FakeProcess first = new FakeProcess(1105L);
        FakeProcess second = new FakeProcess(2105L);

        onEdt(() -> {
            harness.startAttaching(first);
            harness.host.firePeerLost();
            harness.processes.enqueue(second);
            harness.host.attachAvailable = true;
            harness.host.firePeerReady();
            assertEquals(0, harness.launches.pendingCount());

            first.exit(91);
            assertEquals(1, harness.launches.pendingCount());
            harness.launches.runNext();
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(first), harness.terminated);
        assertEquals(List.of(second.pid()), harness.host.attachPids);
        assertEquals(0, harness.host.closeCalls);
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.FAILED));
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.RUNNING)
                .count());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "isolated process " + second.pid()));
        assertTrue(harness.host.attached);
    }

    @Test
    void persistentAttachmentFailureFailsOnceWithoutAutomaticRelaunch()
            throws Exception {
        Harness harness = Harness.create("persistent-attachment-failure", 0x109L);
        FakeProcess process = new FakeProcess(1109L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.host.failAttachmentOnNextVisibility = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(process), harness.terminated);
        assertEquals(0, harness.launches.pendingCount());
        assertFalse(harness.polls.hasActivePoll());
        assertFalse(harness.hasStage(FlutterDesignerNativeCanvasStatus.Stage.RUNNING));
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.FAILED)
                .count());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated persistent attachment failure"));
        assertFalse(onEdt(harness.session::canRestart));

        onEdt(() -> process.exit(0));

        assertTrue(onEdt(harness.session::canRestart));
        assertEquals(0, harness.launches.pendingCount());
    }

    @Test
    void postAttachLivenessLossRetiresOldPidBeforeRetryBecomesAvailable()
            throws Exception {
        Harness harness = Harness.create("surface-liveness-loss", 0x119L);
        FakeProcess process = new FakeProcess(1125L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    harness.statuses.getLast().stage());
            harness.host.surfaceLive = false;
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(onEdt(harness.session::canRestart));
        assertTrue(harness.statuses.getLast().detail().contains(
                "surface is no longer live"));

        onEdt(() -> process.exit(0));

        assertTrue(onEdt(harness.session::canRestart));
    }

    @Test
    void nativeLinkageFailureDuringAttachmentTerminatesAndReportsConcreteCause()
            throws Exception {
        Harness harness = Harness.create("attachment-linkage", 0x10aL);
        FakeProcess process = new FakeProcess(1110L);

        onEdt(() -> {
            harness.host.attachFailure = new UnsatisfiedLinkError(
                    "simulated EnumChildWindows linkage failure");
            harness.startAttaching(process);
            harness.polls.runActivePoll();
        });

        assertEquals(List.of(process), harness.terminated);
        assertFalse(harness.polls.hasActivePoll());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                harness.statuses.getLast().stage());
        assertTrue(harness.statuses.getLast().detail().contains(
                "simulated EnumChildWindows linkage failure"));
    }

    @Test
    void hideAndShowToggleTheAttachedWindowWithoutRelaunching() throws Exception {
        Harness harness = Harness.create("visibility", 0x106L);
        FakeProcess process = new FakeProcess(1106L);

        onEdt(() -> {
            harness.host.attachAvailable = true;
            harness.startAttaching(process);
            harness.polls.runActivePoll();
            harness.session.hide();
            harness.session.show();
        });

        assertEquals(List.of(true, false, true), harness.host.visibility);
        assertEquals(1, harness.processes.commands.size());
        assertEquals(0, harness.launches.pendingCount());
        assertEquals(1, harness.statuses.stream()
                .filter(status -> status.stage()
                        == FlutterDesignerNativeCanvasStatus.Stage.RUNNING)
                .count());
    }

    @Test
    void twoSimultaneousSessionsKeepProcessesHostsAndTerminationIndependent()
            throws Exception {
        Harness first = Harness.create("independent-a", 0x201L);
        Harness second = Harness.create("independent-b", 0x202L);
        FakeProcess firstProcess = new FakeProcess(1201L);
        FakeProcess secondProcess = new FakeProcess(1202L);

        onEdt(() -> {
            first.host.attachAvailable = true;
            second.host.attachAvailable = true;
            first.processes.enqueue(firstProcess);
            second.processes.enqueue(secondProcess);
            first.session.show();
            second.session.show();
            first.build.complete(first.runner);
            second.build.complete(second.runner);
            first.session.show();
            second.session.show();
            assertEquals(1, first.launches.pendingCount());
            assertEquals(1, second.launches.pendingCount());
            first.launches.runNext();
            second.launches.runNext();
            first.polls.runActivePoll();
            second.polls.runActivePoll();
        });

        assertEquals(1, first.processes.commands.size());
        assertEquals(1, second.processes.commands.size());
        assertTrue(first.processes.commands.getFirst().contains(
                "--netbeans-parent-hwnd=0x0000000000000201"));
        assertTrue(second.processes.commands.getFirst().contains(
                "--netbeans-parent-hwnd=0x0000000000000202"));
        assertEquals(List.of(firstProcess.pid()), first.host.attachPids);
        assertEquals(List.of(secondProcess.pid()), second.host.attachPids);
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                first.statuses.getLast().stage());
        assertEquals(FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                second.statuses.getLast().stage());

        onEdt(first.session::close);

        assertTrue(first.terminated.isEmpty());
        onEdt(first.launches::runNext);
        assertEquals(List.of(firstProcess), first.terminated);
        assertTrue(second.terminated.isEmpty());
        assertTrue(second.host.attached);
        onEdt(() -> {
            second.session.hide();
            second.session.show();
        });
        assertEquals(List.of(true, false, true), second.host.visibility);
        onEdt(() -> {
            second.session.close();
            second.launches.runNext();
        });
    }

    @Test
    void boundedInteractionFenceQueueRejectionPublishesTimedOutState()
            throws Exception {
        Harness harness = Harness.create("bounded-interaction-fence-queue", 0x203L);
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasLayoutKey layout = new CanvasLayoutKey(
                new CanvasFrameKey(
                        new CanvasRevisionKey(
                                sessionId, 1, StableId.random(), 1),
                        0),
                0);
        CanvasRunnerProcessChannel channel = new CanvasRunnerProcessChannel(
                new FakeProcess(1203L),
                sessionId,
                Runnable::run,
                new CanvasRunnerProcessChannel.Listener() {
                    @Override
                    public void ready(CanvasEngineIdentity ignored) {
                    }

                    @Override
                    public void presented(
                            CanvasRunnerRuntimeEvent.Presented ignored) {
                    }

                    @Override
                    public void selection(
                            CanvasIntentKey ignored,
                            StableId ignoredWidget) {
                    }

                    @Override
                    public void failed(String ignored) {
                    }
                });
        try {
            onEdt(() -> {
                setPrivateField(harness.session, "processChannel", channel);
                setPrivateField(harness.session, "currentLayout", layout);
                setPrivateField(harness.session, "interactionFenceSequence", 9L);
                setPrivateField(
                        harness.session,
                        "interactionBarrierState",
                        new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                                FlutterDesignerNativeCanvasSession
                                        .InteractionBarrierPhase.SYNCHRONIZING,
                                9,
                                Optional.of(layout)));
                setPrivateField(
                        harness.session,
                        "pendingInteractionFenceLayout",
                        layout);
                setPrivateField(
                        harness.session,
                        "pendingInteractionFenceSequence",
                        9L);
                setPrivateField(
                        harness.session,
                        "interactionFenceRetryAttempts",
                        19);
                invokePrivateNoArgs(
                        harness.session, "scheduleInteractionFenceRetry");

                FlutterDesignerNativeCanvasSession.InteractionBarrierState state =
                        harness.session.interactionBarrierState();
                assertEquals(
                        FlutterDesignerNativeCanvasSession
                                .InteractionBarrierPhase.TIMED_OUT,
                        state.phase());
                assertEquals(9, state.fenceSequence());
                assertEquals(Optional.of(layout), state.layoutKey());
                assertEquals(
                        -1L,
                        privateLongField(
                                harness.session,
                                "pendingInteractionFenceSequence"));
            });
        } finally {
            channel.abort();
        }
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        onEdt(() -> {
            runnable.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return callable.call();
        }
        Holder<T> result = new Holder<>();
        Holder<Throwable> failure = new Holder<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    result.value = callable.call();
                } catch (Throwable throwable) {
                    failure.value = throwable;
                }
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return result.value;
    }

    private static void awaitCondition(BooleanSupplier condition, String message)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), message);
    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static long privateLongField(
            Object target,
            String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getLong(target);
    }

    private static void invokePrivateNoArgs(
            Object target,
            String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        try {
            method.invoke(target);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw failure;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class Harness {
        private final FakeHost host;
        private final CompletableFuture<CanvasRunnerBuildResult> build =
                new CompletableFuture<>();
        private final FakeProcessStarter processes = new FakeProcessStarter();
        private final ManualExecutor launches = new ManualExecutor();
        private final ManualPollScheduler polls = new ManualPollScheduler();
        private final ManualClock clock = new ManualClock();
        private final List<Process> terminated = new ArrayList<>();
        private final List<FlutterDesignerNativeCanvasStatus> statuses = new ArrayList<>();
        private final CanvasRunnerBuildResult runner;
        private final FlutterDesignerNativeCanvasSession session;
        private RuntimeException terminationFailure;

        private Harness(String name, long parentWindow) {
            this(name, parentWindow, null);
        }

        private Harness(
                String name,
                long parentWindow,
                CanvasRunnerBuildResult suppliedRunner) {
            host = new FakeHost(parentWindow);
            Path root = Path.of("target", "native-canvas-session-tests", name)
                    .toAbsolutePath().normalize();
            Path sdkHome = root.resolve("flutter-sdk");
            FlutterSdk sdk = new FlutterSdk(
                    sdkHome, sdkHome.resolve("bin").resolve("flutter.bat"));
            var provider = new WindowsNativeCanvasPlatformProvider();
            var contract = provider.runnerContract().orElseThrow();
            CanvasRunnerCacheIdentity identity = new CanvasRunnerCacheIdentity(
                    "a".repeat(64),
                    "test-engine",
                    contract.fingerprint(),
                    "b".repeat(64));
            Path runnerRoot = root.resolve("runner-cache");
            runner = suppliedRunner == null
                    ? new CanvasRunnerBuildResult(
                            runnerRoot.resolve(
                                    contract.buildTarget().executableName()),
                            runnerRoot,
                            identity,
                            false,
                            "")
                    : suppliedRunner;
            FlutterDesignerNativeCanvasSession.RuntimeServices runtime =
                    new FlutterDesignerNativeCanvasSession.RuntimeServices(
                            () -> new FlutterDesignerNativeCanvasSession.SdkResolution(
                                    sdk, "test Flutter SDK"),
                            ignored -> build,
                            processes,
                            launches,
                            Harness::dispatchUi,
                            polls,
                            this::terminate,
                            clock::nanoTime);
            session = new FlutterDesignerNativeCanvasSession(
                    provider,
                    host,
                    runtime,
                    statuses::add,
                    ignored -> { },
                    ignored -> Optional.empty(),
                    ignored -> { },
                    ignored -> { });
        }

        private static void dispatchUi(Runnable task) {
            if (EventQueue.isDispatchThread()) {
                task.run();
            } else {
                EventQueue.invokeLater(task);
            }
        }

        private CompletableFuture<Void> terminate(Process process) {
            if (terminationFailure != null) {
                return CompletableFuture.failedFuture(terminationFailure);
            }
            terminated.add(process);
            return CompletableFuture.completedFuture(null);
        }

        static Harness create(String name, long parentWindow) throws Exception {
            return onEdt(() -> new Harness(name, parentWindow));
        }

        static Harness create(
                String name,
                long parentWindow,
                CanvasRunnerBuildResult runner) throws Exception {
            return onEdt(() -> new Harness(name, parentWindow, runner));
        }

        void startAttaching(FakeProcess process) {
            processes.enqueue(process);
            session.show();
            build.complete(runner);
            launches.runNext();
        }

        boolean hasStage(FlutterDesignerNativeCanvasStatus.Stage stage) {
            return statuses.stream().anyMatch(status -> status.stage() == stage);
        }

        List<FlutterDesignerNativeCanvasStatus.Stage> stages() {
            return statuses.stream().map(FlutterDesignerNativeCanvasStatus::stage).toList();
        }
    }

    private static final class FakeHost implements NativeCanvasHost {
        private NativeCanvasSurfaceMetrics surfaceMetrics =
                new NativeCanvasSurfaceMetrics(800, 600, 1_000_000);

        private final long parentWindow;
        private final JPanel component = new JPanel();
        private final List<Long> attachPids = new ArrayList<>();
        private final List<Boolean> visibility = new ArrayList<>();
        private boolean ready = true;
        private boolean attachAvailable;
        private LinkageError attachFailure;
        private boolean attached;
        private boolean surfaceLive = true;
        private boolean failAttachmentOnNextVisibility;
        private boolean closeFiresPeerLost;
        private RuntimeException detachFailure;
        private RuntimeException closeFailure;
        private RuntimeException focusFailure;
        private int focusRefusalsRemaining;
        private volatile int focusCalls;
        private volatile boolean focused;
        private int parentWindowHandleCalls;
        private int failingParentWindowHandleCall = -1;
        private int detachCalls;
        private int closeCalls;
        private Runnable peerReady = () -> { };
        private Runnable peerLost = () -> { };
        private Consumer<String> attachmentFailed = ignored -> { };
        private Consumer<NativeCanvasSurfaceMetrics> surfaceMetricsChanged =
                ignored -> { };

        private FakeHost(long parentWindow) {
            this.parentWindow = parentWindow;
        }

        @Override
        public JComponent component() {
            return component;
        }

        @Override
        public NativeCanvasParentHandle parentHandle() {
            parentWindowHandleCalls++;
            if (parentWindowHandleCalls == failingParentWindowHandleCall) {
                throw new IllegalStateException("native peer disappeared");
            }
            if (!ready) {
                throw new IllegalStateException("peer is not ready");
            }
            return new NativeCanvasParentHandle(
                    NativeCanvasPlatform.WINDOWS,
                    "0x" + String.format(
                            java.util.Locale.ROOT, "%016X", parentWindow));
        }

        @Override
        public boolean attachRunner(long runnerProcessId) {
            if (attachFailure != null) {
                throw attachFailure;
            }
            if (!attachAvailable) {
                return false;
            }
            attachPids.add(runnerProcessId);
            attached = true;
            surfaceLive = true;
            return true;
        }

        @Override
        public void detachRunner() {
            detachCalls++;
            attached = false;
            if (detachFailure != null) {
                throw detachFailure;
            }
        }

        @Override
        public boolean isRunnerAttached() {
            return attached;
        }

        @Override
        public boolean isRunnerSurfaceLive() {
            return attached && surfaceLive;
        }

        @Override
        public boolean isNativePeerReady() {
            return ready;
        }

        @Override
        public Optional<NativeCanvasSurfaceMetrics> surfaceMetrics() {
            return attached ? Optional.of(surfaceMetrics) : Optional.empty();
        }

        @Override
        public void setRunnerVisible(boolean visible) {
            if (attached && failAttachmentOnNextVisibility) {
                attached = false;
                attachmentFailed.accept("simulated persistent attachment failure");
                return;
            }
            if (attached) {
                visibility.add(visible);
            }
        }

        @Override
        public boolean requestRunnerFocus() {
            focusCalls++;
            if (focusFailure != null) {
                throw focusFailure;
            }
            if (focusRefusalsRemaining > 0) {
                focusRefusalsRemaining--;
                focused = false;
                return false;
            }
            focused = attached;
            return focused;
        }

        @Override
        public boolean isRunnerFocused() {
            return attached && focused;
        }

        @Override
        public void onPeerReady(Runnable listener) {
            peerReady = listener;
        }

        @Override
        public void onPeerLost(Runnable listener) {
            peerLost = listener;
        }

        @Override
        public void onAttachmentFailed(Consumer<String> listener) {
            attachmentFailed = listener;
        }

        @Override
        public void onSurfaceMetricsChanged(
                Consumer<NativeCanvasSurfaceMetrics> listener) {
            surfaceMetricsChanged = listener;
        }

        @Override
        public void close() {
            closeCalls++;
            attached = false;
            if (closeFailure != null) {
                throw closeFailure;
            }
            if (closeFiresPeerLost) {
                ready = false;
                peerLost.run();
            }
        }

        void firePeerLost() {
            ready = false;
            attached = false;
            peerLost.run();
        }

        void firePeerReady() {
            ready = true;
            peerReady.run();
        }

        void fireAttachmentFailed(String reason) {
            attached = false;
            attachmentFailed.accept(reason);
        }

        void fireSurfaceMetrics(NativeCanvasSurfaceMetrics metrics) {
            surfaceMetrics = metrics;
            surfaceMetricsChanged.accept(metrics);
        }

        void failParentWindowHandleOnCall(int call) {
            failingParentWindowHandleCall = call;
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Deque<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.addLast(command);
        }

        int pendingCount() {
            return pending.size();
        }

        void runNext() {
            Runnable command = pending.pollFirst();
            if (command == null) {
                throw new AssertionError("No pending launch");
            }
            command.run();
        }
    }

    private static final class ManualClock {
        private long now;

        long nanoTime() {
            return now;
        }

        void advance(Duration duration) {
            now += duration.toNanos();
        }
    }

    private static final class ManualPollScheduler
            implements FlutterDesignerNativeCanvasSession.PollScheduler {
        private final List<ScheduledPoll> polls = new ArrayList<>();

        @Override
        public FlutterDesignerNativeCanvasSession.Cancellable schedule(Runnable poll) {
            ScheduledPoll scheduled = new ScheduledPoll(poll);
            polls.add(scheduled);
            return () -> scheduled.active = false;
        }

        boolean hasActivePoll() {
            return polls.stream().anyMatch(poll -> poll.active);
        }

        void runActivePoll() {
            ScheduledPoll scheduled = polls.reversed().stream()
                    .filter(poll -> poll.active)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No active attachment poll"));
            scheduled.action.run();
        }

        private static final class ScheduledPoll {
            private final Runnable action;
            private boolean active = true;

            private ScheduledPoll(Runnable action) {
                this.action = action;
            }
        }
    }

    private static final class FakeProcessStarter
            implements FlutterDesignerNativeCanvasSession.ProcessStarter {
        private final Deque<FakeProcess> queued = new ArrayDeque<>();
        private final List<List<String>> commands = new ArrayList<>();

        void enqueue(FakeProcess process) {
            queued.addLast(process);
        }

        @Override
        public Process start(List<String> command, Path workingDirectory) {
            FakeProcess process = queued.pollFirst();
            if (process == null) {
                throw new AssertionError("No fake process queued for " + command);
            }
            commands.add(List.copyOf(command));
            return process;
        }
    }

    private static final class FakeProcess extends Process {
        private final long pid;
        private final CompletableFuture<Process> onExit = new CompletableFuture<>();
        private final OutputStream output = new ByteArrayOutputStream();
        private final InputStream input = new ByteArrayInputStream(new byte[0]);
        private boolean alive = true;
        private int exitCode;
        private RuntimeException nextOnExitFailure;
        private RuntimeException nextSynchronousOnExitFailure;

        private FakeProcess(long pid) {
            this.pid = pid;
        }

        void exit(int code) {
            if (!alive) {
                return;
            }
            exitCode = code;
            alive = false;
            onExit.complete(this);
        }

        void failNextOnExitObservation() {
            nextOnExitFailure = new IllegalStateException(
                    "simulated exceptional Process.onExit completion");
        }

        void throwNextOnExitObservation() {
            nextSynchronousOnExitFailure = new IllegalStateException(
                    "simulated synchronous Process.onExit failure");
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            try {
                return onExit.thenApply(ignored -> exitCode).get();
            } catch (ExecutionException exception) {
                throw new AssertionError(exception.getCause());
            }
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            try {
                onExit.get(timeout, unit);
                return true;
            } catch (ExecutionException exception) {
                throw new AssertionError(exception.getCause());
            } catch (TimeoutException exception) {
                return false;
            }
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("process is still alive");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            exit(143);
        }

        @Override
        public Process destroyForcibly() {
            exit(137);
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            if (nextSynchronousOnExitFailure != null) {
                RuntimeException failure = nextSynchronousOnExitFailure;
                nextSynchronousOnExitFailure = null;
                throw failure;
            }
            if (nextOnExitFailure != null) {
                RuntimeException failure = nextOnExitFailure;
                nextOnExitFailure = null;
                return CompletableFuture.failedFuture(failure);
            }
            return onExit;
        }
    }
}

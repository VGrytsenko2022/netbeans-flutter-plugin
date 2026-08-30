package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WindowsWebCanvasHostTest {
    private static final String NONCE =
            "0123456789abcdef0123456789abcdef"
            + "0123456789abcdef0123456789abcdef";
    private static final String GENERATION = "a".repeat(64);
    private static final long HWND = 0x0000_7fff_abcd_ef01L;

    @TempDir
    Path temporary;

    @Test
    void lifecycleAndStreamAccessAreFencedToTheEdt() throws Exception {
        Fixture fixture = fixture("edt");

        assertEdtFailure(() -> {
            fixture.host.start(fixture.request, fixture.listener);
            return null;
        });
        assertEdtFailure(fixture.host::runnerStdout);
        assertEdtFailure(fixture.host::runnerStdin);
        assertEdtFailure(fixture.host::isBridgeReady);
        assertEdtFailure(() -> {
            fixture.host.requestControllerFocus();
            return null;
        });
        assertEdtFailure(fixture.host::closeAsync);

        onEdt(() -> {
            fixture.host.start(fixture.request, fixture.listener);
            return null;
        });
        assertEquals(1, fixture.executor.pendingCount());
    }

    @Test
    void earlyNativeEventsWaitUntilSessionPublicationAndPreserveOrder()
            throws Exception {
        Fixture fixture = fixture("early");
        fixture.api.emitControllerReadyDuringCreate = true;
        fixture.api.emitBridgeReadyDuringCreate = true;
        fixture.listener.bridgeReadyAction = () -> {
            try {
                fixture.host.runnerStdin().write(new byte[]{1, 2, 3});
                fixture.host.runnerStdin().flush();
            } catch (IOException failure) {
                throw new AssertionError(failure);
            }
        };

        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        assertEquals(List.of("nativeStarted", "documentReady", "bridgeReady"),
                fixture.listener.events);
        assertTrue(fixture.listener.allCallbacksOnEdt);
        assertTrue(onEdt(fixture.host::isBridgeReady));
        FakeSession session = fixture.api.sessions.getFirst();
        assertEquals(1, session.postedJson.size());
        assertTrue(session.postedJson.getFirst().contains(
                "\"direction\":\"host-to-runner\""));
        assertFalse(fixture.api.runtimeCalledOnEdt);
        assertFalse(fixture.api.createCalledOnEdt);
        assertTrue(session.boundsCalledOnEdt);
        assertTrue(session.visibilityCalledOnEdt);
        assertTrue(fixture.deadlines.latest().cancelled);
    }

    @Test
    void closeDuringInitializationWaitsForCreatedSessionDestruction()
            throws Exception {
        Fixture fixture = fixture("close-during-start");
        start(fixture);
        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);

        assertFalse(closed.isDone());
        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.executor.pendingCount());
        assertFalse(closed.isDone());
        assertTrue(fixture.listener.events.contains("bridgeTerminal:LOCAL_CLOSE"));
        assertFalse(fixture.listener.events.contains("nativeStarted"));

        fixture.executor.runNext();
        flushEdt();

        assertTrue(closed.isDone());
        assertFalse(closed.isCompletedExceptionally());
        assertEquals(1, fixture.api.sessions.getFirst().destroyCalls);
        assertFalse(fixture.api.sessions.getFirst().destroyCalledOnEdt);
        assertEquals(1, fixture.listener.closedCount);
    }

    @Test
    void startupFailureReportsFailureBeforeTerminalAndClose() throws Exception {
        Fixture fixture = fixture("startup-failure");
        fixture.api.createFailure = new IOException("create failed");
        start(fixture);

        fixture.executor.runNext();
        flushEdt();

        assertEquals(List.of(
                "failed:Start Web Canvas",
                "bridgeTerminal:TRANSPORT_FAILURE"), fixture.listener.events);
        assertEquals("create failed", fixture.listener.failureReason);
        assertEquals(1, fixture.executor.pendingCount());
        assertFalse(onEdt(fixture.host::isBridgeReady));

        fixture.executor.runNext();
        flushEdt();
        assertEquals("closed", fixture.listener.events.getLast());
        assertFalse(Files.exists(fixture.userDataFolder));
    }

    @Test
    void processFailureIsOneShotAndDestroysOffEdt() throws Exception {
        Fixture fixture = fixture("process-failure");
        startAndFinish(fixture);

        fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.PROCESS_FAILED,
                1,
                "",
                "renderer exited"));
        flushEdt();

        assertEquals(List.of(
                "nativeStarted",
                "documentReady",
                "bridgeReady",
                "failed:WebView2 renderer/browser process",
                "bridgeTerminal:TRANSPORT_FAILURE"),
                fixture.listener.events);
        assertTrue(fixture.listener.failureReason.contains("renderer exited"));
        assertTrue(fixture.listener.failureReason.contains(
                "RENDER_PROCESS_EXITED (1)"));
        assertFalse(fixture.listener.failureReason.contains("0x00000001"));
        assertEquals(1, fixture.executor.pendingCount());

        fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.PROCESS_FAILED,
                1,
                "",
                "late duplicate"));
        flushEdt();
        assertEquals(1, fixture.listener.failedCount);

        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.api.sessions.getFirst().destroyCalls);
        assertFalse(fixture.api.sessions.getFirst().destroyCalledOnEdt);
        assertEquals(1, fixture.listener.closedCount);
    }

    @Test
    void resizeVisibilityAndFocusArePublishedOnlyFromEdt() throws Exception {
        Fixture fixture = fixture("surface-controls");
        startAndFinish(fixture);
        FakeSession session = fixture.api.sessions.getFirst();
        session.bounds.clear();
        session.visibility.clear();
        fixture.clientBounds.bounds = new NativeCanvasWindowBounds(1_200, 900);

        onEdt(() -> {
            Canvas canvas = carrier(fixture.host);
            canvas.setSize(800, 600);
            canvas.dispatchEvent(new ComponentEvent(
                    canvas, ComponentEvent.COMPONENT_RESIZED));
            canvas.dispatchEvent(new ComponentEvent(
                    canvas, ComponentEvent.COMPONENT_SHOWN));
            canvas.dispatchEvent(new ComponentEvent(
                    canvas, ComponentEvent.COMPONENT_HIDDEN));
            fixture.host.requestControllerFocus();
            return null;
        });

        assertFalse(session.bounds.isEmpty());
        assertEquals(List.of(0, 0, 1_200, 900), session.bounds.getLast());
        assertEquals(List.of(true, false), session.visibility);
        assertEquals(1, session.focusCalls);
        assertTrue(session.boundsCalledOnEdt);
        assertTrue(session.visibilityCalledOnEdt);
        assertTrue(session.focusCalledOnEdt);
    }

    @Test
    void initialNativeRequestUsesPhysicalHwndClientPixels() throws Exception {
        Fixture fixture = fixture("physical-initial-bounds");
        fixture.clientBounds.bounds = new NativeCanvasWindowBounds(975, 720);

        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        WindowsWebView2NativeApi.CreateRequest request =
                fixture.api.requests.getFirst();
        assertEquals(975, request.width());
        assertEquals(720, request.height());
        assertEquals(fixture.artifact.files(), request.artifactFiles());
        assertEquals(HWND, fixture.clientBounds.lastWindow);
        assertEquals(List.of(0, 0, 975, 720),
                fixture.api.sessions.getFirst().bounds.getLast());
    }

    @Test
    void oldNativeCallbacksCannotEnterARestartedGeneration() throws Exception {
        Fixture fixture = fixture("generation");
        startAndFinish(fixture);
        WindowsWebView2NativeApi.Listener oldNativeListener =
                fixture.api.listeners.getFirst();

        closeAndFinish(fixture);
        RecordingListener restarted = new RecordingListener();
        onEdt(() -> {
            fixture.host.start(fixture.request, restarted);
            return null;
        });
        fixture.executor.runNext();
        flushEdt();

        oldNativeListener.event(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.DIAGNOSTIC,
                0,
                "",
                "stale diagnostic"));
        flushEdt();

        assertEquals(List.of("nativeStarted"), restarted.events);
        assertTrue(restarted.diagnostics.isEmpty());
        assertEquals(2, fixture.api.sessions.size());
    }

    @Test
    void closedCallbackMayRestartWithoutLosingTheNewListenerOrFuture()
            throws Exception {
        Fixture fixture = fixture("reentrant-restart");
        startAndFinish(fixture);
        RecordingListener restarted = new RecordingListener();
        fixture.listener.closedAction = () -> {
            fixture.host.start(fixture.request, restarted);
        };

        CompletableFuture<Void> firstClose = onEdt(fixture.host::closeAsync);
        fixture.executor.runNext();
        flushEdt();

        assertTrue(firstClose.isDone());
        assertEquals(1, fixture.executor.pendingCount());
        fixture.executor.runNext();
        flushEdt();
        assertEquals(List.of("nativeStarted"), restarted.events);
        assertEquals(2, fixture.api.sessions.size());
    }

    @Test
    void heavyweightPeerLossFencesAndSchedulesNativeTeardown()
            throws Exception {
        Fixture fixture = fixture("peer-loss");
        startAndFinish(fixture);

        onEdt(() -> {
            carrier(fixture.host).removeNotify();
            return null;
        });
        flushEdt();

        assertEquals(1, fixture.executor.pendingCount());
        assertTrue(fixture.listener.events.contains("bridgeTerminal:LOCAL_CLOSE"));
        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.api.sessions.getFirst().destroyCalls);
        assertEquals(1, fixture.listener.closedCount);
    }

    @Test
    void runnerInputRequiresDocumentAndAuthenticatedBridgeReadiness()
            throws Exception {
        Fixture fixture = fixture("stdin-gate");
        start(fixture);
        assertTrue(onEdt(() -> fixture.host.runnerStdout() != null));
        assertRunnerInputUnavailable(fixture);

        fixture.executor.runNext();
        flushEdt();
        assertRunnerInputUnavailable(fixture);

        emitBridgeReady(fixture);
        flushEdt();
        assertFalse(onEdt(fixture.host::isBridgeReady));
        assertRunnerInputUnavailable(fixture);

        emitControllerReady(fixture);
        flushEdt();
        assertTrue(onEdt(fixture.host::isBridgeReady));
        onEdt(() -> {
            fixture.host.runnerStdin().write(new byte[]{4, 5, 6});
            fixture.host.runnerStdin().flush();
            return null;
        });
        assertEquals(1, fixture.api.sessions.getFirst().postedJson.size());
    }

    @Test
    void startupDeadlineReportsNativeSessionStageAndTearsDown()
            throws Exception {
        Fixture fixture = fixture("timeout-native");
        start(fixture);

        fixture.deadlines.fireLatest();
        flushEdt();

        assertEquals("Start Web Canvas", fixture.listener.failureOperation);
        assertTrue(fixture.listener.failureReason.contains(
                "waiting for native WebView2 session creation"));
        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.executor.pendingCount());
        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.listener.closedCount);
        assertFalse(Files.exists(fixture.userDataFolder));
    }

    @Test
    void startupDeadlineReportsMissingControllerDocumentStage()
            throws Exception {
        Fixture fixture = fixture("timeout-document");
        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        emitBridgeReady(fixture);
        flushEdt();
        fixture.deadlines.fireLatest();
        flushEdt();

        assertTrue(fixture.listener.failureReason.contains(
                "controller/document readiness event"));
        fixture.executor.runNext();
        flushEdt();
        assertFalse(Files.exists(fixture.userDataFolder));
    }

    @Test
    void startupDeadlineReportsMissingAuthenticatedBridgeStage()
            throws Exception {
        Fixture fixture = fixture("timeout-bridge");
        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        emitControllerReady(fixture);
        flushEdt();
        fixture.deadlines.fireLatest();
        flushEdt();

        assertTrue(fixture.listener.failureReason.contains(
                "authenticated Web Canvas bridge readiness"));
        fixture.executor.runNext();
        flushEdt();
        assertFalse(Files.exists(fixture.userDataFolder));
    }

    @Test
    void readyAndCloseCancelTheStartupDeadline() throws Exception {
        Fixture running = fixture("deadline-ready");
        startAndFinish(running);
        List<String> readyEvents = List.copyOf(running.listener.events);

        running.deadlines.fireLatest();
        flushEdt();

        assertTrue(running.deadlines.latest().cancelled);
        assertEquals(readyEvents, running.listener.events);

        Fixture closing = fixture("deadline-close");
        start(closing);
        onEdt(closing.host::closeAsync);
        assertTrue(closing.deadlines.latest().cancelled);
    }

    @Test
    void nativeEventCountFloodProducesOneCoalescedTerminalFailure()
            throws Exception {
        Fixture fixture = fixture("native-event-count-flood");
        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        onEdt(() -> {
            Thread producer = Thread.ofVirtual().start(() -> {
                for (int index = 0; index < 65; index++) {
                    fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                            WindowsWebView2NativeApi.Kind.DIAGNOSTIC,
                            0,
                            "",
                            "diagnostic-" + index));
                }
            });
            producer.join();
            return null;
        });
        flushEdt();

        assertEquals(1, fixture.listener.failedCount);
        assertEquals("Receive native WebView2 events",
                fixture.listener.failureOperation);
        assertTrue(fixture.listener.failureReason.contains("64 events or 8 MiB"));
        assertTrue(fixture.listener.diagnostics.isEmpty());
        assertEquals(1, fixture.executor.pendingCount());
        fixture.executor.runNext();
        flushEdt();
        assertEquals(1, fixture.listener.closedCount);
    }

    @Test
    void nativeEventPayloadFloodIsBoundedBeforeEdtDrain() throws Exception {
        Fixture fixture = fixture("native-event-payload-flood");
        start(fixture);
        fixture.executor.runNext();
        flushEdt();
        String payload = "x".repeat(1_500_000);

        onEdt(() -> {
            Thread producer = Thread.ofVirtual().start(() -> {
                for (int index = 0; index < 6; index++) {
                    fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                            WindowsWebView2NativeApi.Kind.DIAGNOSTIC,
                            0,
                            "",
                            payload));
                }
            });
            producer.join();
            return null;
        });
        flushEdt();

        assertEquals(1, fixture.listener.failedCount);
        assertEquals("Receive native WebView2 events",
                fixture.listener.failureOperation);
        assertTrue(fixture.listener.failureReason.contains("8 MiB"));
        assertTrue(fixture.listener.diagnostics.isEmpty());
        fixture.executor.runNext();
        flushEdt();
    }

    @Test
    void callerCloseCannotDeleteArtifactUntilNativeDestroyAndOwnedCleanup()
            throws Exception {
        Fixture fixture = fixture("artifact-lease");
        startAndFinish(fixture);
        Path artifactRoot = fixture.artifact.root();
        Path marker = fixture.userDataFolder.resolve(
                WindowsWebCanvasHost.USER_DATA_MARKER);
        assertTrue(Files.isRegularFile(marker));

        fixture.artifact.close();
        assertTrue(Files.isDirectory(artifactRoot));

        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);
        fixture.executor.runNext();
        flushEdt();

        FakeSession session = fixture.api.sessions.getFirst();
        assertTrue(session.userDataExistedDuringDestroy);
        assertTrue(session.artifactExistedDuringDestroy);
        assertTrue(closed.isDone());
        assertFalse(closed.isCompletedExceptionally());
        assertFalse(Files.exists(fixture.userDataFolder));
        assertFalse(Files.exists(artifactRoot));
    }

    @Test
    void failedNativeDestroyRetainsUserDataAndArtifactLease() throws Exception {
        Fixture fixture = fixture("destroy-failure-retains-resources");
        startAndFinish(fixture);
        FakeSession session = fixture.api.sessions.getFirst();
        session.destroyFailure = new IOException("destroy failed");
        Path artifactRoot = fixture.artifact.root();
        fixture.artifact.close();

        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);
        fixture.executor.runNext();
        flushEdt();

        assertTrue(closed.isCompletedExceptionally());
        assertEquals(1, session.destroyCalls);
        assertTrue(Files.isDirectory(fixture.userDataFolder));
        assertTrue(Files.isDirectory(artifactRoot));
        assertTrue(fixture.cleanupRetries.failedAttempts.isEmpty());
    }

    @Test
    void preExistingUserDataFolderIsNeverClaimedOrDeleted() throws Exception {
        Fixture fixture = fixture("preexisting-user-data");
        Files.createDirectory(fixture.userDataFolder);
        Path sentinel = fixture.userDataFolder.resolve("keep.txt");
        Files.writeString(sentinel, "keep", StandardCharsets.UTF_8);

        start(fixture);
        fixture.executor.runNext();
        flushEdt();

        assertEquals(0, fixture.api.requests.size());
        assertTrue(fixture.listener.failureReason.contains("must not already exist"));
        assertEquals(1, fixture.executor.pendingCount());
        fixture.executor.runNext();
        flushEdt();
        assertEquals("keep", Files.readString(sentinel, StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(fixture.userDataFolder));
    }

    @Test
    void linkedUserDataParentIsRejectedWithoutTouchingItsTarget()
            throws Exception {
        Path realParent = Files.createDirectory(
                temporary.resolve("linked-user-data-target"));
        Path sentinel = realParent.resolve("keep.txt");
        Files.writeString(sentinel, "keep", StandardCharsets.UTF_8);
        Path linkedParent = temporary.resolve("linked-user-data-alias");
        try {
            Files.createSymbolicLink(linkedParent, realParent);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "symbolic links are not available: " + exception.getMessage());
        }
        IOException failure = assertThrows(IOException.class, () ->
                WindowsWebCanvasHost.UserDataSessionsRoot.openOrCreate(linkedParent));

        assertTrue(failure.getMessage().contains("link")
                || failure.getMessage().contains("reparse"));
        assertEquals("keep", Files.readString(sentinel, StandardCharsets.UTF_8));
        assertFalse(Files.exists(realParent.resolve("session")));
    }

    @Test
    void changedOwnershipMarkerFailsClosedAfterNativeDestroy()
            throws Exception {
        Fixture fixture = fixture("changed-marker");
        startAndFinish(fixture);
        Path marker = fixture.userDataFolder.resolve(
                WindowsWebCanvasHost.USER_DATA_MARKER);
        Files.writeString(marker, "not-owned", StandardCharsets.UTF_8);

        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);
        fixture.executor.runNext();
        flushEdt();

        assertTrue(fixture.api.sessions.getFirst().userDataExistedDuringDestroy);
        assertTrue(closed.isCompletedExceptionally());
        assertTrue(Files.isDirectory(fixture.userDataFolder));
        assertEquals(List.of(1, 2, 3), fixture.cleanupRetries.failedAttempts);
        assertTrue(fixture.listener.events.getLast().equals("closed"));
    }

    @Test
    void transientUserDataCleanupFailureIsRetriedAfterNativeDestroy()
            throws Exception {
        Fixture fixture = fixture("cleanup-retry");
        startAndFinish(fixture);
        Path marker = fixture.userDataFolder.resolve(
                WindowsWebCanvasHost.USER_DATA_MARKER);
        byte[] ownedMarker = Files.readAllBytes(marker);
        Files.writeString(marker, "temporarily-locked", StandardCharsets.UTF_8);
        fixture.cleanupRetries.retryAction = (attempt, failure) -> {
            if (attempt == 1) {
                Files.write(marker, ownedMarker);
            }
        };

        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);
        fixture.executor.runNext();
        flushEdt();

        assertEquals(List.of(1), fixture.cleanupRetries.failedAttempts);
        assertTrue(closed.isDone());
        assertFalse(closed.isCompletedExceptionally());
        assertFalse(Files.exists(fixture.userDataFolder));
    }

    @Test
    void unmarkedPreExistingSessionsRootIsNeverClaimed() throws Exception {
        Path parent = Files.createDirectory(temporary.resolve("unmarked-parent"));
        Path root = Files.createDirectory(parent.resolve("sessions"));
        Path sentinel = root.resolve("keep.txt");
        Files.writeString(sentinel, "keep", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () ->
                WindowsWebCanvasHost.UserDataSessionsRoot.openOrCreate(root));

        assertEquals("keep", Files.readString(sentinel, StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(root));
    }

    @Test
    void publicationNestedUnderSessionsRootIsRejectedBeforeNativeCreate()
            throws Exception {
        Fixture fixture = fixture("artifact-inside-sessions");
        Path nestedBase = Files.createDirectory(
                fixture.sessionsRoot.root().resolve("artifact-base"));
        WebCanvasArtifactPublisher.PublishedArtifact nestedArtifact =
                artifact(nestedBase);
        Path session = fixture.sessionsRoot.sessionPath("nested-publication");
        WindowsWebCanvasHost.StartRequest nestedRequest =
                new WindowsWebCanvasHost.StartRequest(
                        nestedArtifact, fixture.sessionsRoot, session, NONCE);

        onEdt(() -> {
            fixture.host.start(nestedRequest, fixture.listener);
            return null;
        });
        fixture.executor.runNext();
        flushEdt();

        assertTrue(fixture.listener.failureReason.contains("must be disjoint"));
        assertEquals(0, fixture.api.requests.size());
        assertFalse(Files.exists(session));
        fixture.executor.runNext();
        flushEdt();
        nestedArtifact.close();
    }

    @Test
    void sessionsRootNestedUnderPublicationIsRejectedBeforeNativeCreate()
            throws Exception {
        Fixture fixture = fixture("sessions-inside-artifact");
        Path cache = Files.createDirectory(fixture.artifact.root().resolve("cache"));
        WindowsWebCanvasHost.UserDataSessionsRoot nestedSessions =
                WindowsWebCanvasHost.UserDataSessionsRoot.openOrCreate(
                        cache.resolve("sessions"));
        Path session = nestedSessions.sessionPath("nested-session");
        WindowsWebCanvasHost.StartRequest nestedRequest =
                new WindowsWebCanvasHost.StartRequest(
                        fixture.artifact, nestedSessions, session, NONCE);

        onEdt(() -> {
            fixture.host.start(nestedRequest, fixture.listener);
            return null;
        });
        fixture.executor.runNext();
        flushEdt();

        assertTrue(fixture.listener.failureReason.contains("must be disjoint"));
        assertEquals(0, fixture.api.requests.size());
        assertFalse(Files.exists(session));
        fixture.executor.runNext();
        flushEdt();
    }

    private Fixture fixture(String name) throws Exception {
        Path root = Files.createDirectories(temporary.resolve(name));
        WebCanvasArtifactPublisher.PublishedArtifact artifact = artifact(root);
        Path cacheParent = Files.createDirectory(root.resolve("cache"));
        WindowsWebCanvasHost.UserDataSessionsRoot sessionsRoot =
                WindowsWebCanvasHost.UserDataSessionsRoot.openOrCreate(
                        cacheParent.resolve("webview2-sessions"));
        Path userDataFolder = sessionsRoot.sessionPath("session");
        WindowsWebCanvasHost.StartRequest request =
                new WindowsWebCanvasHost.StartRequest(
                        artifact, sessionsRoot, userDataFolder, NONCE);
        FakeNativeApi api = new FakeNativeApi();
        ManualExecutor executor = new ManualExecutor();
        ManualDeadlineScheduler deadlines = new ManualDeadlineScheduler();
        FakeClientBoundsResolver clientBounds = new FakeClientBoundsResolver();
        ManualCleanupRetryDelay cleanupRetries = new ManualCleanupRetryDelay();
        WindowsWebCanvasHost host = onEdt(() -> {
            WindowsWebCanvasHost created = new WindowsWebCanvasHost(
                    api, executor, ignored -> HWND, deadlines, clientBounds,
                    cleanupRetries);
            created.setSize(640, 480);
            created.doLayout();
            return created;
        });
        return new Fixture(
                host, api, executor, deadlines, clientBounds, cleanupRetries,
                artifact,
                sessionsRoot, request.userDataFolder(), request,
                new RecordingListener());
    }

    private static void start(Fixture fixture) throws Exception {
        onEdt(() -> {
            fixture.host.start(fixture.request, fixture.listener);
            return null;
        });
        assertEquals(1, fixture.executor.pendingCount());
    }

    private static void startAndFinish(Fixture fixture) throws Exception {
        start(fixture);
        fixture.executor.runNext();
        flushEdt();
        emitControllerReady(fixture);
        emitBridgeReady(fixture);
        flushEdt();
        assertEquals(List.of("nativeStarted", "documentReady", "bridgeReady"),
                fixture.listener.events);
    }

    private static void emitControllerReady(Fixture fixture) {
        fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.CONTROLLER_READY,
                0,
                fixture.api.requests.getLast().originPolicy().indexUri(),
                ""));
    }

    private static void emitBridgeReady(Fixture fixture) {
        fixture.api.emitLatest(new WindowsWebView2NativeApi.Event(
                WindowsWebView2NativeApi.Kind.WEB_MESSAGE,
                0,
                fixture.api.requests.getLast().originPolicy().indexUri(),
                readyMessage()));
    }

    private static void assertRunnerInputUnavailable(Fixture fixture) throws Exception {
        IllegalStateException failure = onEdt(() -> assertThrows(
                IllegalStateException.class, fixture.host::runnerStdin));
        assertTrue(failure.getMessage().contains("authenticated running bridge"));
    }

    private static void closeAndFinish(Fixture fixture) throws Exception {
        CompletableFuture<Void> closed = onEdt(fixture.host::closeAsync);
        assertEquals(1, fixture.executor.pendingCount());
        fixture.executor.runNext();
        flushEdt();
        assertTrue(closed.isDone());
    }

    private static Canvas carrier(WindowsWebCanvasHost host) {
        Component component = host.getComponent(0);
        assertTrue(component instanceof Canvas);
        return (Canvas) component;
    }

    private static WebCanvasArtifactPublisher.PublishedArtifact artifact(Path base)
            throws Exception {
        Path source = Files.createDirectory(base.resolve("source"));
        byte[] index = "<html>canvas</html>".getBytes(StandardCharsets.UTF_8);
        byte[] metadata = "build-id".getBytes(StandardCharsets.UTF_8);
        Files.write(source.resolve("index.html"), index);
        Files.write(source.resolve(".last_build_id"), metadata);
        WebCanvasArtifactContract.ArtifactFile indexFile = artifactFile(
                "index.html", index);
        WebCanvasArtifactContract.ArtifactFile metadataFile = artifactFile(
                ".last_build_id", metadata);
        WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                new WebCanvasArtifactContract.ArtifactSnapshot(
                        source,
                        Map.of("index.html", indexFile),
                        Map.of(".last_build_id", metadataFile),
                        index.length + metadata.length,
                        "f".repeat(64));
        WebCanvasArtifactPublisher publisher = new WebCanvasArtifactPublisher(
                base.resolve("published"));
        return publisher.publish(snapshot, GENERATION);
    }

    private static WebCanvasArtifactContract.ArtifactFile artifactFile(
            String path, byte[] bytes) throws NoSuchAlgorithmException {
        return new WebCanvasArtifactContract.ArtifactFile(
                path,
                bytes.length,
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes)));
    }

    private static String readyMessage() {
        return "{"
                + "\"format\":\"" + WebCanvasHostBridge.FORMAT + "\","
                + "\"version\":" + WebCanvasHostBridge.VERSION + ","
                + "\"sessionNonce\":\"" + NONCE + "\","
                + "\"direction\":\"runner-to-host\","
                + "\"kind\":\"ready\","
                + "\"sequence\":0}";
    }

    private static void assertEdtFailure(Callable<?> operation) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class, operation::call);
        assertTrue(failure.getMessage().contains("EDT"));
    }

    private static void flushEdt() throws Exception {
        onEdt(() -> null);
        // Bridge notifications deliberately hop once more so no product
        // callback executes while WebCanvasHostBridge holds its monitor.
        onEdt(() -> null);
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
                } catch (Throwable problem) {
                    failure.value = problem;
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (InvocationTargetException impossible) {
            throw new AssertionError(impossible.getCause());
        }
        if (failure.value instanceof Exception exception) {
            throw exception;
        }
        if (failure.value instanceof Error error) {
            throw error;
        }
        if (failure.value != null) {
            throw new AssertionError(failure.value);
        }
        return result.value;
    }

    private record Fixture(
            WindowsWebCanvasHost host,
            FakeNativeApi api,
            ManualExecutor executor,
            ManualDeadlineScheduler deadlines,
            FakeClientBoundsResolver clientBounds,
            ManualCleanupRetryDelay cleanupRetries,
            WebCanvasArtifactPublisher.PublishedArtifact artifact,
            WindowsWebCanvasHost.UserDataSessionsRoot sessionsRoot,
            Path userDataFolder,
            WindowsWebCanvasHost.StartRequest request,
            RecordingListener listener) {
    }

    private static final class Holder<T> {
        private T value;
    }

    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();

        @Override
        public synchronized void execute(Runnable command) {
            pending.addLast(command);
        }

        synchronized int pendingCount() {
            return pending.size();
        }

        void runNext() {
            Runnable command;
            synchronized (this) {
                command = pending.removeFirst();
            }
            command.run();
        }
    }

    private static final class ManualDeadlineScheduler
            implements WindowsWebCanvasHost.StartupDeadlineScheduler {
        private final List<Deadline> deadlines = new ArrayList<>();

        @Override
        public synchronized Cancellation schedule(
                java.time.Duration timeout, Runnable callback) {
            assertEquals(WindowsWebCanvasHost.STARTUP_TIMEOUT, timeout);
            Deadline deadline = new Deadline(callback);
            deadlines.add(deadline);
            return () -> deadline.cancelled = true;
        }

        synchronized Deadline latest() {
            return deadlines.getLast();
        }

        void fireLatest() {
            Deadline deadline = latest();
            if (!deadline.cancelled && !deadline.fired) {
                deadline.fired = true;
                deadline.callback.run();
            }
        }

        private static final class Deadline {
            private final Runnable callback;
            private boolean cancelled;
            private boolean fired;

            private Deadline(Runnable callback) {
                this.callback = callback;
            }
        }
    }

    private static final class FakeClientBoundsResolver
            implements WindowsWebCanvasHost.ClientBoundsResolver {
        private NativeCanvasWindowBounds bounds =
                new NativeCanvasWindowBounds(640, 480);
        private long lastWindow;

        @Override
        public NativeCanvasWindowBounds resolve(long parentWindow) {
            lastWindow = parentWindow;
            return bounds;
        }
    }

    private static final class ManualCleanupRetryDelay
            implements WindowsWebCanvasHost.CleanupRetryDelay {
        private final List<Integer> failedAttempts = new ArrayList<>();
        private RetryAction retryAction = (attempt, failure) -> { };

        @Override
        public void awaitRetry(int failedAttempt, IOException failure)
                throws IOException {
            failedAttempts.add(failedAttempt);
            retryAction.run(failedAttempt, failure);
        }
    }

    @FunctionalInterface
    private interface RetryAction {
        void run(int failedAttempt, IOException failure) throws IOException;
    }

    private static final class FakeNativeApi implements WindowsWebView2NativeApi {
        private final List<WindowsWebView2NativeApi.Listener> listeners =
                new ArrayList<>();
        private final List<WindowsWebView2NativeApi.CreateRequest> requests =
                new ArrayList<>();
        private final List<FakeSession> sessions = new ArrayList<>();
        private IOException createFailure;
        private boolean emitControllerReadyDuringCreate;
        private boolean emitBridgeReadyDuringCreate;
        private boolean runtimeCalledOnEdt;
        private boolean createCalledOnEdt;

        @Override
        public String runtimeVersion() {
            runtimeCalledOnEdt = EventQueue.isDispatchThread();
            return "151.0.4129.107";
        }

        @Override
        public NativeSession create(CreateRequest request, Listener listener)
                throws IOException {
            createCalledOnEdt = EventQueue.isDispatchThread();
            if (createFailure != null) {
                throw createFailure;
            }
            listeners.add(listener);
            requests.add(request);
            FakeSession session = new FakeSession(
                    request.userDataFolder(), request.contentRoot());
            sessions.add(session);
            if (emitControllerReadyDuringCreate) {
                listener.event(new Event(
                        Kind.CONTROLLER_READY,
                        0,
                        request.originPolicy().indexUri(),
                        ""));
            }
            if (emitBridgeReadyDuringCreate) {
                listener.event(new Event(
                        Kind.WEB_MESSAGE,
                        0,
                        request.originPolicy().indexUri(),
                        readyMessage()));
            }
            return session;
        }

        void emitLatest(Event event) {
            listeners.getLast().event(event);
        }
    }

    private static final class FakeSession
            implements WindowsWebView2NativeApi.NativeSession {
        private final Path userDataFolder;
        private final Path artifactRoot;
        private final List<String> postedJson = new ArrayList<>();
        private final List<List<Integer>> bounds = new ArrayList<>();
        private final List<Boolean> visibility = new ArrayList<>();
        private boolean boundsCalledOnEdt;
        private boolean visibilityCalledOnEdt;
        private boolean focusCalledOnEdt;
        private boolean destroyCalledOnEdt;
        private int focusCalls;
        private int destroyCalls;
        private boolean userDataExistedDuringDestroy;
        private boolean artifactExistedDuringDestroy;
        private IOException destroyFailure;

        private FakeSession(Path userDataFolder, Path artifactRoot) {
            this.userDataFolder = userDataFolder;
            this.artifactRoot = artifactRoot;
        }

        @Override
        public void postWebMessageJson(String json) {
            postedJson.add(json);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            boundsCalledOnEdt = EventQueue.isDispatchThread();
            bounds.add(List.of(x, y, width, height));
        }

        @Override
        public void setVisible(boolean visible) {
            visibilityCalledOnEdt = EventQueue.isDispatchThread();
            visibility.add(visible);
        }

        @Override
        public void requestFocus() {
            focusCalledOnEdt = EventQueue.isDispatchThread();
            focusCalls++;
        }

        @Override
        public void destroy() throws IOException {
            destroyCalledOnEdt = EventQueue.isDispatchThread();
            userDataExistedDuringDestroy = Files.isDirectory(
                    userDataFolder, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            artifactExistedDuringDestroy = Files.isDirectory(
                    artifactRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            destroyCalls++;
            if (destroyFailure != null) {
                throw destroyFailure;
            }
        }
    }

    private static final class RecordingListener
            implements WindowsWebCanvasHost.Listener {
        private final List<String> events = new ArrayList<>();
        private final List<String> diagnostics = new ArrayList<>();
        private boolean allCallbacksOnEdt = true;
        private Runnable bridgeReadyAction = () -> { };
        private Runnable closedAction = () -> { };
        private int failedCount;
        private int closedCount;
        private String failureOperation = "";
        private String failureReason = "";

        @Override
        public void nativeStarted(String runtimeVersion, String documentUri) {
            record("nativeStarted");
        }

        @Override
        public void documentReady(String documentUri) {
            record("documentReady");
        }

        @Override
        public void bridgeReady() {
            record("bridgeReady");
            bridgeReadyAction.run();
        }

        @Override
        public void diagnostic(String message) {
            record("diagnostic");
            diagnostics.add(message);
        }

        @Override
        public void bridgeTerminal(WebCanvasHostBridge.Terminal terminal) {
            record("bridgeTerminal:" + terminal.kind());
        }

        @Override
        public void failed(String operation, String reason) {
            record("failed:" + operation);
            failedCount++;
            failureOperation = operation;
            failureReason = reason;
        }

        @Override
        public void closed(String teardownFailure) {
            record("closed");
            closedCount++;
            closedAction.run();
        }

        private void record(String event) {
            allCallbacksOnEdt &= EventQueue.isDispatchThread();
            events.add(event);
        }
    }
}

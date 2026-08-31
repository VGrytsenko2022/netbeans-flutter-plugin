package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasOwnerCoordinator.Phase;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class FlutterDesignerCanvasOwnerCoordinatorTest {

    @Test
    void sameBackendRequestIsANoOp() {
        List<Backend> factoryCalls = new ArrayList<>();
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE, new TestSession());
        RecordingObserver observer = new RecordingObserver();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    return CompletableFuture.completedFuture(nativeOwner);
                },
                observer);

        coordinator.requestBackend(Backend.NATIVE);
        CallbackEpoch firstEpoch = coordinator.activeEpoch();
        coordinator.requestBackend(Backend.NATIVE);

        assertEquals(List.of(Backend.NATIVE), factoryCalls);
        assertEquals(Phase.ACTIVE, coordinator.phase());
        assertSame(nativeOwner, coordinator.activeOwner());
        assertSame(firstEpoch, coordinator.activeEpoch());
        assertEquals(1, observer.activationCount);
    }

    @Test
    void rapidNativeWebNativeCoalescesBeforeFactoryCreation() {
        CompletableFuture<Void> nativeRetirement = new CompletableFuture<>();
        TestSession firstSession = new TestSession(List.of(nativeRetirement));
        FlutterDesignerCanvasOwner firstNative = owner(
                Backend.NATIVE, firstSession);
        FlutterDesignerCanvasOwner secondNative = owner(
                Backend.NATIVE, new TestSession());
        List<Backend> factoryCalls = new ArrayList<>();
        RecordingObserver observer = new RecordingObserver();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    if (factoryCalls.size() == 1) {
                        return CompletableFuture.completedFuture(firstNative);
                    }
                    assertTrue(firstNative.closed(),
                            "the prior owner must retire before replacement creation");
                    return CompletableFuture.completedFuture(secondNative);
                },
                observer);

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.requestBackend(Backend.EXACT_WEB);
        coordinator.requestBackend(Backend.NATIVE);

        assertEquals(Phase.RETIRING, coordinator.phase());
        assertEquals(List.of(Backend.NATIVE), factoryCalls);
        assertEquals(Backend.NATIVE, coordinator.desiredBackend());
        assertFalse(firstNative.closed());

        nativeRetirement.complete(null);

        assertEquals(List.of(Backend.NATIVE, Backend.NATIVE), factoryCalls);
        assertFalse(factoryCalls.contains(Backend.EXACT_WEB));
        assertEquals(Phase.ACTIVE, coordinator.phase());
        assertSame(secondNative, coordinator.activeOwner());
        assertTrue(observer.events.indexOf("retired:NATIVE")
                < observer.events.lastIndexOf("create:NATIVE"));
    }

    @Test
    void failedRetirementRetainsOwnerUntilExplicitRetry() {
        CompletableFuture<Void> failedRetirement = new CompletableFuture<>();
        CompletableFuture<Void> recoveredRetirement = new CompletableFuture<>();
        TestSession nativeSession = new TestSession(List.of(
                failedRetirement, recoveredRetirement));
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE, nativeSession);
        FlutterDesignerCanvasOwner webOwner = owner(
                Backend.EXACT_WEB, new TestSession());
        List<Backend> factoryCalls = new ArrayList<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    return CompletableFuture.completedFuture(
                            backend == Backend.NATIVE ? nativeOwner : webOwner);
                },
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.requestBackend(Backend.EXACT_WEB);
        IllegalStateException cleanupFailure = new IllegalStateException(
                "simulated native peer cleanup failure");
        failedRetirement.completeExceptionally(cleanupFailure);

        assertEquals(Phase.POISONED, coordinator.phase());
        assertNull(coordinator.activeOwner());
        assertSame(nativeOwner, coordinator.retainedOwner());
        assertSame(cleanupFailure, coordinator.lastFailure());
        assertEquals(List.of(Backend.NATIVE), factoryCalls);

        coordinator.requestBackend(Backend.EXACT_WEB);
        assertEquals(1, nativeSession.prepareCalls);
        assertEquals(Phase.POISONED, coordinator.phase());

        coordinator.retryTransition();
        assertEquals(2, nativeSession.prepareCalls);
        assertEquals(Phase.RETIRING, coordinator.phase());
        assertEquals(List.of(Backend.NATIVE), factoryCalls);

        recoveredRetirement.complete(null);

        assertEquals(List.of(Backend.NATIVE, Backend.EXACT_WEB), factoryCalls);
        assertEquals(Phase.ACTIVE, coordinator.phase());
        assertSame(webOwner, coordinator.activeOwner());
        assertNull(coordinator.retainedOwner());
    }

    @Test
    void failedCriticalDetachRetainsRetiredOwnerUntilDetachAndCreationAreRetried()
            throws Exception {
        TestSession nativeSession = new TestSession();
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE, nativeSession);
        FlutterDesignerCanvasOwner webOwner = owner(
                Backend.EXACT_WEB, new TestSession());
        List<Backend> factoryCalls = new ArrayList<>();
        AtomicInteger detachAttempts = new AtomicInteger();
        IllegalStateException detachFailure = new IllegalStateException(
                "simulated critical component detach failure");
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    return CompletableFuture.completedFuture(
                            backend == Backend.NATIVE ? nativeOwner : webOwner);
                },
                new FlutterDesignerCanvasOwnerCoordinator.Observer() {
                    @Override
                    public void ownerRetired(
                            FlutterDesignerCanvasOwner ignoredOwner,
                            CallbackEpoch ignoredEpoch) {
                        if (detachAttempts.incrementAndGet() == 1) {
                            throw detachFailure;
                        }
                    }
                });

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.requestBackend(Backend.EXACT_WEB);

        assertEquals(Phase.POISONED, coordinator.phase());
        assertNull(coordinator.activeOwner());
        assertSame(nativeOwner, coordinator.retainedOwner());
        assertSame(detachFailure, coordinator.lastFailure());
        assertTrue(nativeOwner.closed(),
                "peer retirement succeeded before the critical detach failed");
        assertEquals(1, nativeSession.prepareCalls,
                "the physical peer must be retired exactly once");
        assertEquals(1, detachAttempts.get());
        assertEquals(List.of(Backend.NATIVE), factoryCalls,
                "a failed detach must not create the successor");

        coordinator.retryTransition();

        assertEquals(Phase.EMPTY, coordinator.phase());
        assertNull(coordinator.activeOwner());
        assertNull(coordinator.retainedOwner());
        assertEquals(1, nativeSession.prepareCalls,
                "retrying an already retired owner must only retry detach");
        assertEquals(2, detachAttempts.get());
        assertEquals(List.of(Backend.NATIVE), factoryCalls,
                "successful detach recovery must remain fail-closed");

        coordinator.retryTransition();

        assertEquals(Phase.ACTIVE, coordinator.phase());
        assertSame(webOwner, coordinator.activeOwner());
        assertEquals(List.of(Backend.NATIVE, Backend.EXACT_WEB), factoryCalls);
    }

    @Test
    void failedCriticalActivationRetiresCandidateBeforeExplicitCreationRetry() {
        TestSession rejectedSession = new TestSession();
        FlutterDesignerCanvasOwner rejectedOwner = owner(
                Backend.NATIVE, rejectedSession);
        FlutterDesignerCanvasOwner replacementOwner = owner(
                Backend.NATIVE, new TestSession());
        AtomicInteger factoryCalls = new AtomicInteger();
        AtomicInteger activationAttempts = new AtomicInteger();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> CompletableFuture.completedFuture(
                        factoryCalls.incrementAndGet() == 1
                                ? rejectedOwner
                                : replacementOwner),
                new FlutterDesignerCanvasOwnerCoordinator.Observer() {
                    @Override
                    public void ownerActivated(
                            FlutterDesignerCanvasOwner ignoredOwner,
                            CallbackEpoch ignoredEpoch) {
                        if (activationAttempts.incrementAndGet() == 1) {
                            throw new IllegalStateException(
                                    "simulated critical component activation failure");
                        }
                    }
                });

        coordinator.requestBackend(Backend.NATIVE);

        assertEquals(Phase.EMPTY, coordinator.phase());
        assertNull(coordinator.activeOwner());
        assertNull(coordinator.retainedOwner());
        assertTrue(rejectedOwner.closed());
        assertEquals(1, rejectedSession.prepareCalls);
        assertEquals(1, factoryCalls.get(),
                "activation failure cleanup must not enter a create loop");

        coordinator.retryTransition();

        assertEquals(Phase.ACTIVE, coordinator.phase());
        assertSame(replacementOwner, coordinator.activeOwner());
        assertEquals(2, factoryCalls.get());
        assertEquals(2, activationAttempts.get());
    }

    @Test
    void closeFencesLateCreationAndRetiresItsCandidate() {
        CompletableFuture<FlutterDesignerCanvasOwner> creation =
                new CompletableFuture<>();
        CompletableFuture<Void> lateRetirement = new CompletableFuture<>();
        TestSession lateSession = new TestSession(List.of(lateRetirement));
        FlutterDesignerCanvasOwner lateOwner = owner(
                Backend.NATIVE, lateSession);
        RecordingObserver observer = new RecordingObserver();
        AtomicInteger admittedCallbacks = new AtomicInteger();
        List<CallbackEpoch> epochs = new ArrayList<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, epoch) -> {
                    epochs.add(epoch);
                    return creation;
                },
                observer);

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.admitCallback(
                epochs.getFirst(), admittedCallbacks::incrementAndGet);
        coordinator.close();
        creation.complete(lateOwner);

        assertEquals(0, admittedCallbacks.get());
        assertEquals(0, observer.activationCount);
        assertEquals(1, lateSession.prepareCalls);
        assertEquals(Phase.RETIRING, coordinator.phase());
        assertSame(lateOwner, coordinator.retainedOwner());
        assertNull(coordinator.activeOwner());

        coordinator.admitCallback(
                epochs.getFirst(), admittedCallbacks::incrementAndGet);
        lateRetirement.complete(null);

        assertEquals(0, admittedCallbacks.get());
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(lateOwner.closed());
        assertNull(coordinator.retainedOwner());
        assertEquals(1, observer.events.stream()
                .filter("retired:NATIVE"::equals)
                .count());
    }

    @Test
    void callbackAdmissionRequiresTheExactActiveEpoch() {
        CompletableFuture<Void> nativeRetirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE,
                new TestSession(List.of(nativeRetirement)));
        FlutterDesignerCanvasOwner webOwner = owner(
                Backend.EXACT_WEB, new TestSession());
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> CompletableFuture.completedFuture(
                        backend == Backend.NATIVE ? nativeOwner : webOwner),
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);
        AtomicInteger nativeCallbacks = new AtomicInteger();
        AtomicInteger webCallbacks = new AtomicInteger();

        coordinator.requestBackend(Backend.NATIVE);
        CallbackEpoch nativeEpoch = coordinator.activeEpoch();
        coordinator.admitCallback(nativeEpoch, nativeCallbacks::incrementAndGet);
        coordinator.requestBackend(Backend.EXACT_WEB);
        coordinator.admitCallback(nativeEpoch, nativeCallbacks::incrementAndGet);

        nativeRetirement.complete(null);
        CallbackEpoch webEpoch = coordinator.activeEpoch();
        coordinator.admitCallback(nativeEpoch, nativeCallbacks::incrementAndGet);
        coordinator.admitCallback(webEpoch, webCallbacks::incrementAndGet);

        assertEquals(1, nativeCallbacks.get());
        assertEquals(1, webCallbacks.get());
        assertEquals(Backend.NATIVE, nativeEpoch.backend());
        assertEquals(Backend.EXACT_WEB, webEpoch.backend());
        assertTrue(nativeEpoch.sequence() < webEpoch.sequence());
    }

    @Test
    void closeDuringRetirementDoesNotCreateAReplacement() {
        CompletableFuture<Void> nativeRetirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE,
                new TestSession(List.of(nativeRetirement)));
        List<Backend> factoryCalls = new ArrayList<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    return CompletableFuture.completedFuture(nativeOwner);
                },
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.requestBackend(Backend.EXACT_WEB);
        coordinator.close();
        nativeRetirement.complete(null);

        assertEquals(List.of(Backend.NATIVE), factoryCalls);
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertNull(coordinator.desiredBackend());
        assertNull(coordinator.activeOwner());
    }

    @Test
    void poisonedCloseRetirementCanBeExplicitlyRetried() {
        CompletableFuture<Void> failedRetirement = new CompletableFuture<>();
        CompletableFuture<Void> recoveredRetirement = new CompletableFuture<>();
        TestSession session = new TestSession(List.of(
                failedRetirement, recoveredRetirement));
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE, session);
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) ->
                        CompletableFuture.completedFuture(nativeOwner),
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.NATIVE);
        CompletionStage<Void> closed = coordinator.closeAsync();
        failedRetirement.completeExceptionally(
                new IllegalStateException("simulated close failure"));

        assertEquals(Phase.POISONED, coordinator.phase());
        assertSame(nativeOwner, coordinator.retainedOwner());
        assertFalse(closed.toCompletableFuture().isDone());
        coordinator.retryTransition();
        assertEquals(Phase.RETIRING, coordinator.phase());
        assertEquals(2, session.prepareCalls);

        recoveredRetirement.complete(null);

        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(nativeOwner.closed());
        assertNull(coordinator.retainedOwner());
        assertTrue(closed.toCompletableFuture().isDone());
        closed.toCompletableFuture().join();
    }

    @Test
    void closeOfAlreadyPoisonedTransitionRetriesRetainedOwnerWithoutReplacement() {
        CompletableFuture<Void> failedSwitchRetirement = new CompletableFuture<>();
        CompletableFuture<Void> closeRetirement = new CompletableFuture<>();
        TestSession session = new TestSession(List.of(
                failedSwitchRetirement, closeRetirement));
        FlutterDesignerCanvasOwner nativeOwner = owner(
                Backend.NATIVE, session);
        List<Backend> factoryCalls = new ArrayList<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> {
                    factoryCalls.add(backend);
                    return CompletableFuture.completedFuture(nativeOwner);
                },
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.NATIVE);
        coordinator.requestBackend(Backend.EXACT_WEB);
        failedSwitchRetirement.completeExceptionally(
                new IllegalStateException("simulated switch retirement failure"));

        assertEquals(Phase.POISONED, coordinator.phase());
        assertSame(nativeOwner, coordinator.retainedOwner());
        assertEquals(List.of(Backend.NATIVE), factoryCalls);

        CompletionStage<Void> closed = coordinator.closeAsync();

        assertEquals(Phase.RETIRING, coordinator.phase());
        assertSame(nativeOwner, coordinator.retainedOwner());
        assertNull(coordinator.activeOwner());
        assertNull(coordinator.desiredBackend());
        assertEquals(2, session.prepareCalls);
        assertEquals(List.of(Backend.NATIVE), factoryCalls);
        assertFalse(closed.toCompletableFuture().isDone());

        closeRetirement.complete(null);

        closed.toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(nativeOwner.closed());
        assertNull(coordinator.retainedOwner());
        assertEquals(List.of(Backend.NATIVE), factoryCalls);
    }

    @Test
    void closeCompletionWaitsForALateCandidateAndCompletesAfterRetirement() {
        CompletableFuture<FlutterDesignerCanvasOwner> creation =
                new CompletableFuture<>();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.EXACT_WEB,
                new TestSession(List.of(retirement)));
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> creation,
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.EXACT_WEB);
        CompletionStage<Void> closed = coordinator.closeAsync();

        assertFalse(closed.toCompletableFuture().isDone());
        creation.complete(owner);
        assertEquals(Phase.RETIRING, coordinator.phase());
        assertFalse(closed.toCompletableFuture().isDone());

        retirement.complete(null);

        closed.toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(owner.closed());
    }

    @Test
    void closeCompletionFinishesWhenPendingCreationFailsWithoutAnOwner() {
        CompletableFuture<FlutterDesignerCanvasOwner> creation =
                new CompletableFuture<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) -> creation,
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);

        coordinator.requestBackend(Backend.EXACT_WEB);
        CompletionStage<Void> closed = coordinator.closeAsync();
        creation.completeExceptionally(
                new IllegalStateException("simulated creation failure"));

        closed.toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertNull(coordinator.activeOwner());
        assertNull(coordinator.retainedOwner());
    }

    @Test
    void closeRequestSynchronouslyFencesCallbacksBeforeQueuedCloseRuns() {
        ManualExecutor executor = new ManualExecutor();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.EXACT_WEB, new TestSession());
        FlutterDesignerCanvasOwnerCoordinator coordinator =
                new FlutterDesignerCanvasOwnerCoordinator(
                        (backend, ignoredEpoch) ->
                                CompletableFuture.completedFuture(owner),
                        FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP,
                        executor);
        AtomicInteger admitted = new AtomicInteger();

        coordinator.requestBackend(Backend.EXACT_WEB);
        executor.drain();
        CallbackEpoch epoch = coordinator.activeEpoch();
        assertTrue(coordinator.isActiveEpoch(epoch));

        CompletionStage<Void> closed = coordinator.closeAsync();
        coordinator.admitCallback(epoch, admitted::incrementAndGet);

        assertFalse(coordinator.isActiveEpoch(epoch));
        assertEquals(0, admitted.get());
        assertEquals(Phase.ACTIVE, coordinator.phase());
        executor.drain();
        closed.toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
    }

    @Test
    void externalCompletionCannotForgePeerSafeClose() {
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.EXACT_WEB,
                new TestSession(List.of(retirement)));
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) ->
                        CompletableFuture.completedFuture(owner),
                FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP);
        coordinator.requestBackend(Backend.EXACT_WEB);

        CompletionStage<Void> protectedClose = coordinator.closeAsync();
        CompletableFuture<Void> callerView = protectedClose.toCompletableFuture();
        callerView.complete(null);

        assertTrue(callerView.isDone());
        assertEquals(Phase.RETIRING, coordinator.phase());
        assertFalse(coordinator.closeAsync().toCompletableFuture().isDone());

        retirement.complete(null);
        coordinator.closeAsync().toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
    }

    @Test
    void rejectedInitialCloseDispatchRetainsTheRequestForExplicitRetry() {
        RejectOnceExecutor executor = new RejectOnceExecutor();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.NATIVE, new TestSession());
        FlutterDesignerCanvasOwnerCoordinator coordinator =
                new FlutterDesignerCanvasOwnerCoordinator(
                        (backend, ignoredEpoch) ->
                                CompletableFuture.completedFuture(owner),
                        FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP,
                        executor);
        coordinator.requestBackend(Backend.NATIVE);
        executor.rejectNext();

        assertThrows(RejectedExecutionException.class, coordinator::closeAsync);
        assertFalse(coordinator.isActiveEpoch(coordinator.activeEpoch()));

        coordinator.closeAsync().toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(owner.closed());
    }

    @Test
    void rejectedRetirementCompletionDispatchIsRecoveredByRepeatedClose() {
        RejectOnceExecutor executor = new RejectOnceExecutor();
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.EXACT_WEB,
                new TestSession(List.of(retirement)));
        FlutterDesignerCanvasOwnerCoordinator coordinator =
                new FlutterDesignerCanvasOwnerCoordinator(
                        (backend, ignoredEpoch) ->
                                CompletableFuture.completedFuture(owner),
                        FlutterDesignerCanvasOwnerCoordinator.Observer.NOOP,
                        executor);
        coordinator.requestBackend(Backend.EXACT_WEB);
        CompletionStage<Void> close = coordinator.closeAsync();
        executor.rejectNext();

        retirement.complete(null);

        assertEquals(Phase.RETIRING, coordinator.phase());
        assertFalse(close.toCompletableFuture().isDone());
        coordinator.closeAsync().toCompletableFuture().join();
        assertEquals(Phase.CLOSED, coordinator.phase());
        assertTrue(owner.closed());
    }

    @Test
    void ownerRetiredObserverRunsBeforeCloseCompletionIsPublished() {
        CompletableFuture<Void> retirement = new CompletableFuture<>();
        FlutterDesignerCanvasOwner owner = owner(
                Backend.EXACT_WEB,
                new TestSession(List.of(retirement)));
        List<String> events = new ArrayList<>();
        FlutterDesignerCanvasOwnerCoordinator coordinator = coordinator(
                (backend, ignoredEpoch) ->
                        CompletableFuture.completedFuture(owner),
                new FlutterDesignerCanvasOwnerCoordinator.Observer() {
                    @Override
                    public void ownerRetired(
                            FlutterDesignerCanvasOwner ignoredOwner,
                            CallbackEpoch ignoredEpoch) {
                        events.add("retired");
                    }
                });
        coordinator.requestBackend(Backend.EXACT_WEB);
        CompletionStage<Void> close = coordinator.closeAsync();
        close.whenComplete((ignored, failure) -> events.add("complete"));

        retirement.complete(null);

        assertEquals(List.of("retired", "complete"), events);
    }

    private static FlutterDesignerCanvasOwnerCoordinator coordinator(
            FlutterDesignerCanvasOwnerCoordinator.OwnerFactory factory,
            FlutterDesignerCanvasOwnerCoordinator.Observer observer) {
        return new FlutterDesignerCanvasOwnerCoordinator(
                factory, observer, Runnable::run);
    }

    private static FlutterDesignerCanvasOwner owner(
            Backend backend,
            TestSession session) {
        return FlutterDesignerCanvasOwner.adopt(backend, session);
    }

    private static final class ManualExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.addLast(command);
        }

        void drain() {
            Runnable task;
            while ((task = tasks.pollFirst()) != null) {
                task.run();
            }
        }
    }

    private static final class RejectOnceExecutor implements Executor {
        private boolean rejectNext;

        @Override
        public void execute(Runnable command) {
            if (rejectNext) {
                rejectNext = false;
                throw new RejectedExecutionException("simulated rejection");
            }
            command.run();
        }

        void rejectNext() {
            rejectNext = true;
        }
    }

    private static final class RecordingObserver
            implements FlutterDesignerCanvasOwnerCoordinator.Observer {
        private final List<String> events = new ArrayList<>();
        private int activationCount;

        @Override
        public void ownerCreationStarted(Backend backend, CallbackEpoch epoch) {
            events.add("create:" + backend);
        }

        @Override
        public void ownerActivated(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) {
            activationCount++;
            events.add("active:" + owner.backend());
        }

        @Override
        public void ownerRetirementStarted(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) {
            events.add("retiring:" + owner.backend());
        }

        @Override
        public void ownerRetired(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) {
            events.add("retired:" + owner.backend());
        }
    }

    private static final class TestSession
            implements FlutterDesignerCanvasSession {
        private final JPanel component = new JPanel();
        private final ArrayDeque<CompletionStage<Void>> retirements =
                new ArrayDeque<>();
        private int prepareCalls;
        private int closeCalls;

        private TestSession() {
        }

        private TestSession(List<CompletionStage<Void>> retirementAttempts) {
            retirements.addAll(retirementAttempts);
        }

        @Override
        public JComponent component() {
            return component;
        }

        @Override
        public boolean isSurfaceFocused() {
            return false;
        }

        @Override
        public boolean releaseSurfaceFocus() {
            return false;
        }

        @Override
        public void show() {
        }

        @Override
        public void hide() {
        }

        @Override
        public void requestFocus() {
        }

        @Override
        public void clearFocusRequest() {
        }

        @Override
        public boolean restart() {
            return false;
        }

        @Override
        public boolean canRestart() {
            return false;
        }

        @Override
        public void present(
                DesignerDocument document,
                WidgetCatalog catalog,
                CanvasPreviewMode previewMode,
                CanvasTargetPlatform targetPlatform,
                CanvasResolvedTheme resolvedTheme) {
        }

        @Override
        public void withdraw() {
        }

        @Override
        public void selectWidget(StableId widgetId) {
        }

        @Override
        public void setViewportMetricsListener(
                Consumer<CanvasViewportMetrics> listener) {
        }

        @Override
        public void setInteractionListener(Runnable listener) {
        }

        @Override
        public void setTextEditCommitListener(
                Consumer<CanvasRunnerRuntimeEvent.TextEditCommit> listener) {
        }

        @Override
        public void setInteractionBarrierListener(
                Consumer<InteractionBarrierState> listener) {
        }

        @Override
        public InteractionBarrierState interactionBarrierState() {
            return new InteractionBarrierState(
                    InteractionBarrierPhase.INACTIVE,
                    0,
                    Optional.empty());
        }

        @Override
        public void setViewportPresentation(
                CanvasViewportPresentation presentation) {
        }

        @Override
        public boolean paletteCatalogInsertDropAvailable() {
            return false;
        }

        @Override
        public boolean authorizePaletteDragSource(
                String token,
                WidgetTypeId widgetType) {
            return false;
        }

        @Override
        public void showWidgetMovePreview(
                StableId sourceWidgetId,
                WidgetPlacement destination) {
        }

        @Override
        public void clearWidgetMovePreview() {
        }

        @Override
        public CompletionStage<Void> preparePeerRemovalAsync() {
            prepareCalls++;
            if (!retirements.isEmpty()) {
                return retirements.removeFirst();
            }
            close();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}

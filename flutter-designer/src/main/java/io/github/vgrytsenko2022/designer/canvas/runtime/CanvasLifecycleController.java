package io.github.vgrytsenko2022.designer.canvas.runtime;

import io.github.vgrytsenko2022.designer.canvas.CanvasAdmission;
import io.github.vgrytsenko2022.designer.canvas.CanvasPresentationGate;
import io.github.vgrytsenko2022.designer.canvas.CanvasRenderProfile;
import io.github.vgrytsenko2022.designer.canvas.CanvasRenderRequest;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.canvas.ValidatedCanvasRevisionSnapshot;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireProtocol;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Pure per-MultiView lifecycle owner for one read-only Flutter Canvas.
 *
 * <p>The controller permits one backend render at a time and retains at most
 * one replace-only pending request. Every backend attempt receives a fresh
 * {@link CanvasSessionId}; an attempt object additionally fences callbacks
 * that arrive after failure, restart or close. The backend can only return
 * disposable presentation evidence and receives no mutation or persistence
 * authority. Backend completions are dispatched through the supplied callback
 * executor with a synchronous rejection fallback. Listener delivery remains
 * ordered but deliberately thread-neutral; UI adapters marshal to the EDT.</p>
 */
public final class CanvasLifecycleController implements AutoCloseable {
    public static final int CURRENT_PROTOCOL_VERSION = CanvasWireProtocol.VERSION;

    private final Object lock = new Object();
    private final CanvasBackendFactory backendFactory;
    private final Executor callbackExecutor;
    private final StableId documentId;
    private final int protocolVersion;
    private final Supplier<CanvasSessionId> sessionIds;
    private final CopyOnWriteArrayList<CanvasControllerListener> listeners =
            new CopyOnWriteArrayList<>();
    private final ArrayDeque<StateChange> stateChanges = new ArrayDeque<>();

    private CanvasControllerState state = new CanvasControllerState.New();
    private CanvasPresentationGate presentationGate;
    private Attempt activeAttempt;
    private DesiredPresentation desired;
    private CanvasRenderRequest pending;
    private RenderFlight inFlight;
    private long nextAttemptSequence;
    private boolean opened;
    private boolean closed;
    private boolean drainingStateChanges;

    public CanvasLifecycleController(
            CanvasBackendFactory backendFactory,
            Executor callbackExecutor,
            StableId documentId) {
        this(
                backendFactory,
                callbackExecutor,
                documentId,
                CURRENT_PROTOCOL_VERSION,
                CanvasSessionId::random);
    }

    public CanvasLifecycleController(
            CanvasBackendFactory backendFactory,
            Executor callbackExecutor,
            StableId documentId,
            int protocolVersion) {
        this(
                backendFactory,
                callbackExecutor,
                documentId,
                protocolVersion,
                CanvasSessionId::random);
    }

    CanvasLifecycleController(
            CanvasBackendFactory backendFactory,
            Executor callbackExecutor,
            StableId documentId,
            int protocolVersion,
            Supplier<CanvasSessionId> sessionIds) {
        this.backendFactory = Objects.requireNonNull(
                backendFactory, "backendFactory");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor, "callbackExecutor");
        this.documentId = Objects.requireNonNull(documentId, "documentId");
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        this.protocolVersion = protocolVersion;
        this.sessionIds = Objects.requireNonNull(sessionIds, "sessionIds");
    }

    /** Starts the first backend attempt. Repeated calls while open are no-ops. */
    public void open() {
        StartPlan plan;
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Canvas lifecycle controller is closed");
            }
            if (opened) {
                return;
            }
            AttemptPreparation preparation = prepareAttemptLocked(false);
            opened = true;
            plan = installAttemptLocked(preparation);
        }
        fire(plan.change());
        launch(plan.attempt());
    }

    /**
     * Publishes the latest desired immutable revision.
     *
     * <p>The host identity advances immediately even when another render is in
     * flight. A burst therefore invalidates the older response while retaining
     * only the newest follow-up request.</p>
     */
    public CanvasRenderRequest present(
            CanvasRenderProfile renderProfile,
            ValidatedCanvasRevisionSnapshot snapshot) {
        Objects.requireNonNull(renderProfile, "renderProfile");
        Objects.requireNonNull(snapshot, "snapshot");
        CanvasRenderRequest request;
        synchronized (lock) {
            requireOpenLocked();
            request = presentationGate.present(renderProfile, snapshot);
            desired = new DesiredPresentation(renderProfile, snapshot);
            pending = request;
        }
        dispatchIfPossible();
        return request;
    }

    /**
     * Replaces the current backend attempt with a fresh-session attempt.
     * Repeated requests while that replacement is starting are idempotent.
     */
    public boolean restart() {
        Attempt previousAttempt;
        StartPlan plan;
        synchronized (lock) {
            if (closed || !opened) {
                return false;
            }
            if (state instanceof CanvasControllerState.Starting starting
                    && starting.restarting()) {
                return true;
            }
            AttemptPreparation preparation = prepareAttemptLocked(true);
            previousAttempt = activeAttempt;
            if (previousAttempt != null) {
                previousAttempt.expectedClose = true;
            }
            activeAttempt = null;
            inFlight = null;
            pending = null;
            if (presentationGate != null) {
                presentationGate.close();
            }
            plan = installAttemptLocked(preparation);
        }
        fire(plan.change());
        safeCloseAttempt(previousAttempt);
        launch(plan.attempt());
        return true;
    }

    public CanvasControllerState state() {
        synchronized (lock) {
            return state;
        }
    }

    public void addListener(CanvasControllerListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(CanvasControllerListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /** Closes this per-view lifecycle. A closed instance cannot be reopened. */
    @Override
    public void close() {
        Attempt attempt;
        StateChange change;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            opened = false;
            attempt = activeAttempt;
            activeAttempt = null;
            if (attempt != null) {
                attempt.expectedClose = true;
            }
            inFlight = null;
            pending = null;
            desired = null;
            if (presentationGate != null) {
                presentationGate.close();
                presentationGate = null;
            }
            change = changeStateLocked(new CanvasControllerState.Closed());
        }
        fire(change);
        safeCloseAttempt(attempt);
    }

    private AttemptPreparation prepareAttemptLocked(boolean restarting) {
        if (nextAttemptSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Canvas backend attempt sequence is exhausted");
        }
        CanvasSessionId sessionId = Objects.requireNonNull(
                sessionIds.get(), "sessionIds returned null");
        CanvasBackendOpenRequest request = new CanvasBackendOpenRequest(
                sessionId,
                protocolVersion);
        long attemptSequence = nextAttemptSequence;
        CanvasPresentationGate nextGate = new CanvasPresentationGate(
                sessionId, documentId);
        CanvasRenderRequest nextPending = desired == null
                ? null
                : nextGate.present(
                        desired.renderProfile(), desired.snapshot());
        Attempt attempt = new Attempt(request, attemptSequence);
        CanvasControllerState.Starting starting = new CanvasControllerState.Starting(
                request, attemptSequence, restarting);
        return new AttemptPreparation(attempt, nextGate, nextPending, starting);
    }

    private StartPlan installAttemptLocked(AttemptPreparation preparation) {
        nextAttemptSequence++;
        presentationGate = preparation.gate();
        pending = preparation.pending();
        inFlight = null;
        activeAttempt = preparation.attempt();
        StateChange change = changeStateLocked(preparation.state());
        Attempt attempt = preparation.attempt();
        return new StartPlan(attempt, change);
    }

    private void launch(Attempt attempt) {
        synchronized (lock) {
            if (closed || activeAttempt != attempt || attempt.expectedClose) {
                return;
            }
        }
        final CanvasBackendSession openedBackend;
        try {
            openedBackend = Objects.requireNonNull(
                    backendFactory.open(attempt.request),
                    "Canvas backend factory returned null");
        } catch (RuntimeException | LinkageError failure) {
            failAttempt(attempt, CanvasFailureStage.STARTUP, reason(failure), true);
            return;
        }

        CompletionStage<CanvasBackendReady> ready = null;
        CompletionStage<Void> termination = null;
        Throwable startupFailure = null;
        synchronized (attempt.backendCallLock) {
            try {
                boolean accepted;
                synchronized (lock) {
                    accepted = !closed
                            && activeAttempt == attempt
                            && !attempt.expectedClose;
                    if (accepted) {
                        attempt.backend = openedBackend;
                    }
                }
                if (!accepted) {
                    safeClose(openedBackend);
                    return;
                }
                ready = Objects.requireNonNull(
                        openedBackend.ready(),
                        "Canvas backend returned no ready stage");
                termination = Objects.requireNonNull(
                        openedBackend.termination(),
                        "Canvas backend returned no termination stage");
            } catch (RuntimeException | LinkageError failure) {
                startupFailure = failure;
            }
        }
        if (startupFailure != null) {
            failAttempt(
                    attempt,
                    CanvasFailureStage.STARTUP,
                    reason(startupFailure),
                    true);
            return;
        }
        try {
            termination.whenComplete((ignored, failure) -> dispatchCallback(
                    attempt,
                    CanvasFailureStage.TERMINATION,
                    () -> onTermination(attempt, failure)));
            ready.whenComplete((value, failure) -> dispatchCallback(
                    attempt,
                    CanvasFailureStage.STARTUP,
                    () -> onReady(attempt, value, failure)));
        } catch (RuntimeException | LinkageError failure) {
            failAttempt(attempt, CanvasFailureStage.STARTUP, reason(failure), true);
        }
    }

    private void onReady(
            Attempt attempt,
            CanvasBackendReady ready,
            Throwable failure) {
        if (failure != null) {
            failAttempt(
                    attempt,
                    CanvasFailureStage.STARTUP,
                    reason(failure),
                    true);
            return;
        }
        if (ready == null) {
            failAttempt(
                    attempt,
                    CanvasFailureStage.STARTUP,
                    "Canvas backend completed startup without a ready handshake",
                    true);
            return;
        }

        String mismatch = handshakeMismatch(attempt.request, ready);
        if (mismatch != null) {
            failAttempt(
                    attempt,
                    CanvasFailureStage.HANDSHAKE,
                    mismatch,
                    true);
            return;
        }

        StateChange change;
        synchronized (lock) {
            if (closed || activeAttempt != attempt || attempt.expectedClose) {
                return;
            }
            attempt.ready = ready;
            change = changeStateLocked(new CanvasControllerState.Idle(ready));
        }
        fire(change);
        dispatchIfPossible();
    }

    private void dispatchIfPossible() {
        RenderFlight flight;
        CanvasBackendSession backend;
        StateChange change;
        FailurePlan failurePlan = null;
        synchronized (lock) {
            Attempt attempt = activeAttempt;
            if (closed
                    || attempt == null
                    || attempt.ready == null
                    || attempt.backend == null
                    || inFlight != null
                    || pending == null) {
                return;
            }
            if (!attempt.ready.engineIdentity().equals(
                    pending.renderProfile().engineIdentity())) {
                failurePlan = failAttemptLocked(
                        attempt,
                        CanvasFailureStage.HANDSHAKE,
                        "Canvas backend engine identity does not match "
                                + "the exact requested render profile",
                        true);
                flight = null;
                backend = null;
                change = null;
            } else {
                CanvasRenderRequest request = pending;
                pending = null;
                flight = new RenderFlight(attempt, request);
                inFlight = flight;
                backend = attempt.backend;
                change = changeStateLocked(new CanvasControllerState.Rendering(
                        attempt.ready,
                        request.revisionKey()));
            }
        }
        if (failurePlan != null) {
            fire(failurePlan.change());
            safeCloseAttempt(failurePlan.attempt());
            return;
        }

        fire(change);
        invokeRender(flight, backend);
    }

    private void invokeRender(
            RenderFlight flight,
            CanvasBackendSession backend) {
        CompletionStage<CanvasPresentationReady> completion = null;
        Throwable invocationFailure = null;
        synchronized (flight.attempt.backendCallLock) {
            synchronized (lock) {
                if (closed
                        || activeAttempt != flight.attempt
                        || flight.attempt.expectedClose
                        || flight.attempt.backend != backend
                        || inFlight != flight) {
                    return;
                }
            }
            try {
                completion = Objects.requireNonNull(
                        backend.render(flight.request),
                        "Canvas backend returned no render stage");
            } catch (RuntimeException | LinkageError failure) {
                invocationFailure = failure;
            }
        }
        if (invocationFailure != null) {
            onRenderComplete(flight, null, invocationFailure);
            return;
        }
        try {
            completion.whenComplete((value, failure) -> dispatchCallback(
                    flight.attempt,
                    CanvasFailureStage.RENDER,
                    () -> onRenderComplete(flight, value, failure)));
        } catch (RuntimeException | LinkageError failure) {
            onRenderComplete(flight, null, failure);
        }
    }

    private void onRenderComplete(
            RenderFlight flight,
            CanvasPresentationReady ready,
            Throwable failure) {
        if (failure != null) {
            boolean current;
            synchronized (lock) {
                current = !closed
                        && activeAttempt == flight.attempt
                        && inFlight == flight;
            }
            if (current) {
                failAttempt(
                        flight.attempt,
                        CanvasFailureStage.RENDER,
                        reason(failure),
                        true);
            }
            return;
        }
        if (ready == null) {
            failAttempt(
                    flight.attempt,
                    CanvasFailureStage.RENDER,
                    "Canvas backend completed a render without presentation evidence",
                    true);
            return;
        }

        StateChange change = null;
        String protocolFailure = null;
        synchronized (lock) {
            if (closed
                    || activeAttempt != flight.attempt
                    || inFlight != flight) {
                return;
            }
            inFlight = null;
            if (!flight.request.revisionKey().equals(
                    ready.frameKey().revisionKey())) {
                protocolFailure = "Canvas backend returned presentation evidence for "
                        + "a different render request";
            } else {
                CanvasAdmission admission = presentationGate.admitPresentation(
                        ready.frameKey(), ready.layoutKey());
                if (admission == CanvasAdmission.ACCEPTED) {
                    change = changeStateLocked(
                            new CanvasControllerState.Presented(
                                    flight.attempt.ready,
                                    ready.frameKey(),
                                    ready.layoutKey()));
                } else if (admission != CanvasAdmission.STALE_REVISION
                        && admission != CanvasAdmission.STALE_SESSION
                        && admission != CanvasAdmission.STALE_DOCUMENT
                        && admission != CanvasAdmission.CLOSED) {
                    protocolFailure = "Canvas backend returned inadmissible initial "
                            + "frame/layout evidence: " + admission;
                }
            }
        }
        if (protocolFailure != null) {
            failAttempt(
                    flight.attempt,
                    CanvasFailureStage.PROTOCOL,
                    protocolFailure,
                    true);
            return;
        }
        fire(change);
        dispatchIfPossible();
    }

    private void onTermination(Attempt attempt, Throwable failure) {
        boolean unexpected;
        synchronized (lock) {
            unexpected = !closed
                    && activeAttempt == attempt
                    && !attempt.expectedClose;
        }
        if (!unexpected) {
            return;
        }
        String detail = failure == null
                ? "Canvas backend terminated unexpectedly"
                : reason(failure);
        failAttempt(
                attempt,
                CanvasFailureStage.TERMINATION,
                detail,
                true);
    }

    private void failAttempt(
            Attempt attempt,
            CanvasFailureStage stage,
            String failureReason,
            boolean restartable) {
        FailurePlan plan;
        synchronized (lock) {
            plan = failAttemptLocked(
                    attempt, stage, failureReason, restartable);
        }
        if (plan == null) {
            return;
        }
        fire(plan.change());
        safeCloseAttempt(plan.attempt());
    }

    private FailurePlan failAttemptLocked(
            Attempt attempt,
            CanvasFailureStage stage,
            String failureReason,
            boolean restartable) {
        if (closed || activeAttempt != attempt) {
            return null;
        }
        CanvasControllerState.Failed failed = new CanvasControllerState.Failed(
                attempt.request,
                attempt.sequence,
                stage,
                failureReason,
                restartable);
        activeAttempt = null;
        attempt.expectedClose = true;
        inFlight = null;
        pending = null;
        return new FailurePlan(attempt, changeStateLocked(failed));
    }

    private StateChange changeStateLocked(CanvasControllerState next) {
        CanvasControllerState previous = state;
        state = Objects.requireNonNull(next, "next");
        StateChange change = new StateChange(previous, next);
        stateChanges.addLast(change);
        return change;
    }

    private void fire(StateChange change) {
        if (change == null) {
            return;
        }
        synchronized (lock) {
            if (drainingStateChanges) {
                return;
            }
            drainingStateChanges = true;
        }
        while (true) {
            StateChange queued;
            synchronized (lock) {
                queued = stateChanges.pollFirst();
                if (queued == null) {
                    drainingStateChanges = false;
                    return;
                }
            }
            for (CanvasControllerListener listener : listeners) {
                try {
                    listener.stateChanged(queued.previous(), queued.current());
                } catch (RuntimeException | LinkageError ignored) {
                    // One presentation adapter must not wedge the runtime lifecycle.
                }
            }
        }
    }

    private void requireOpenLocked() {
        if (closed) {
            throw new IllegalStateException("Canvas lifecycle controller is closed");
        }
        if (!opened || presentationGate == null) {
            throw new IllegalStateException("Canvas lifecycle controller is not open");
        }
    }

    private static String handshakeMismatch(
            CanvasBackendOpenRequest request,
            CanvasBackendReady ready) {
        if (!request.sessionId().equals(ready.sessionId())) {
            return "Canvas backend ready handshake echoed a different session";
        }
        if (request.protocolVersion() != ready.protocolVersion()) {
            return "Canvas backend ready handshake selected an unsupported protocol version";
        }
        return null;
    }

    private static String reason(Throwable failure) {
        Throwable value = unwrap(Objects.requireNonNull(failure, "failure"));
        String message = null;
        try {
            message = value.getMessage();
        } catch (RuntimeException | LinkageError ignored) {
            // A hostile backend exception must not break failure containment.
        }
        if (message != null && !message.isEmpty()) {
            return message;
        }
        String simpleName = value.getClass().getSimpleName();
        if (simpleName != null && !simpleName.isBlank()) {
            return simpleName;
        }
        String className = value.getClass().getName();
        return className == null || className.isBlank()
                ? "Canvas backend failure"
                : className;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable value = failure;
        for (int depth = 0; depth < 16 && value instanceof CompletionException;
                depth++) {
            final Throwable cause;
            try {
                cause = value.getCause();
            } catch (RuntimeException | LinkageError ignored) {
                return value;
            }
            if (cause == null || cause == value) {
                return value;
            }
            value = cause;
        }
        return value;
    }

    private void dispatchCallback(
            Attempt attempt,
            CanvasFailureStage failureStage,
            Runnable callback) {
        AtomicBoolean invoked = new AtomicBoolean();
        Runnable guarded = () -> {
            if (!invoked.compareAndSet(false, true)) {
                return;
            }
            try {
                callback.run();
            } catch (RuntimeException | LinkageError failure) {
                failAttempt(attempt, failureStage, reason(failure), true);
            }
        };
        try {
            callbackExecutor.execute(guarded);
        } catch (RuntimeException | LinkageError rejection) {
            guarded.run();
        }
    }

    private static void safeCloseAttempt(Attempt attempt) {
        if (attempt == null) {
            return;
        }
        synchronized (attempt.backendCallLock) {
            CanvasBackendSession backend = attempt.backend;
            attempt.backend = null;
            safeClose(backend);
        }
    }

    private static void safeClose(CanvasBackendSession backend) {
        if (backend == null) {
            return;
        }
        try {
            backend.close();
        } catch (RuntimeException | LinkageError ignored) {
            // Close-time failures cannot restore authority to a detached attempt.
        }
    }

    private static final class Attempt {
        final CanvasBackendOpenRequest request;
        final long sequence;
        final Object backendCallLock = new Object();
        CanvasBackendSession backend;
        CanvasBackendReady ready;
        boolean expectedClose;

        Attempt(CanvasBackendOpenRequest request, long sequence) {
            this.request = request;
            this.sequence = sequence;
        }
    }

    private record DesiredPresentation(
            CanvasRenderProfile renderProfile,
            ValidatedCanvasRevisionSnapshot snapshot) {
    }

    private record RenderFlight(
            Attempt attempt,
            CanvasRenderRequest request) {
    }

    private record StartPlan(Attempt attempt, StateChange change) {
    }

    private record AttemptPreparation(
            Attempt attempt,
            CanvasPresentationGate gate,
            CanvasRenderRequest pending,
            CanvasControllerState.Starting state) {
    }

    private record FailurePlan(Attempt attempt, StateChange change) {
    }

    private record StateChange(
            CanvasControllerState previous,
            CanvasControllerState current) {
    }
}

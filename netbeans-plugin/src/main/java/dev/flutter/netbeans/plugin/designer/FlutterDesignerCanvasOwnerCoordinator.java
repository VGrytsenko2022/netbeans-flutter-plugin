package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.plugin.designer.FlutterDesignerCanvasBackendSelector.Backend;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serial, UI-agnostic owner transition envelope for one Designer Canvas view.
 *
 * <p>The coordinator never asks its factory for a replacement until the prior
 * owner has completed peer-safe retirement. Rapid route changes update only the
 * desired backend, and callbacks are admitted only for the exact active epoch.
 * Component installation and removal remain observer-owned.</p>
 */
final class FlutterDesignerCanvasOwnerCoordinator implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerCanvasOwnerCoordinator.class.getName());

    private final OwnerFactory factory;
    private final Observer observer;
    private final Executor callbackExecutor;
    private final Object dispatchLock = new Object();
    private final ArrayDeque<Runnable> dispatched = new ArrayDeque<>();

    private volatile Phase phase = Phase.EMPTY;
    private volatile Backend desiredBackend;
    private volatile FlutterDesignerCanvasOwner activeOwner;
    private volatile CallbackEpoch activeEpoch;
    private volatile FlutterDesignerCanvasOwner retainedOwner;
    private volatile CallbackEpoch retainedEpoch;
    private volatile Throwable lastFailure;
    private boolean drainScheduled;
    private boolean closed;
    private long nextEpochSequence;
    private long creationAttempt;
    private long retirementAttempt;
    private CallbackEpoch creatingEpoch;

    FlutterDesignerCanvasOwnerCoordinator(
            OwnerFactory factory,
            Observer observer,
            Executor callbackExecutor) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.observer = Objects.requireNonNull(observer, "observer");
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor, "callbackExecutor");
    }

    /** Requests the latest backend; equal desired routes are strict no-ops. */
    void requestBackend(Backend backend) {
        Backend requested = Objects.requireNonNull(backend, "backend");
        dispatch(() -> requestBackendSerial(requested));
    }

    /** Explicitly retries either retained-owner cleanup or failed creation. */
    void retryTransition() {
        dispatch(this::retryTransitionSerial);
    }

    /**
     * Marshals and conditionally delivers one backend callback. Pending,
     * retiring, poisoned and stale epochs have no callback authority.
     */
    void admitCallback(CallbackEpoch epoch, Runnable callback) {
        CallbackEpoch candidate = Objects.requireNonNull(epoch, "epoch");
        Runnable admitted = Objects.requireNonNull(callback, "callback");
        dispatch(() -> {
            if (!closed
                    && phase == Phase.ACTIVE
                    && activeEpoch == candidate
                    && activeOwner != null) {
                admitted.run();
            }
        });
    }

    Phase phase() {
        return phase;
    }

    Backend desiredBackend() {
        return desiredBackend;
    }

    FlutterDesignerCanvasOwner activeOwner() {
        return activeOwner;
    }

    FlutterDesignerCanvasOwner retainedOwner() {
        return retainedOwner;
    }

    CallbackEpoch activeEpoch() {
        return activeEpoch;
    }

    Throwable lastFailure() {
        return lastFailure;
    }

    @Override
    public void close() {
        dispatch(this::closeSerial);
    }

    private void requestBackendSerial(Backend backend) {
        if (closed || desiredBackend == backend) {
            return;
        }
        desiredBackend = backend;
        lastFailure = null;
        switch (phase) {
            case EMPTY -> beginCreationIfNeeded();
            case ACTIVE -> {
                if (activeOwner.backend() != backend) {
                    beginActiveRetirement();
                }
            }
            case CREATING, RETIRING, POISONED -> {
                // Coalesce only. Completion or explicit retry reads the latest
                // desired backend before invoking the factory.
            }
            case CLOSED -> {
                // The closed flag above normally handles this branch.
            }
        }
    }

    private void retryTransitionSerial() {
        if (phase == Phase.POISONED && retainedOwner != null) {
            beginRetirementAttempt(retainedOwner, retainedEpoch);
        } else if (!closed
                && phase == Phase.EMPTY
                && desiredBackend != null) {
            beginCreationIfNeeded();
        }
    }

    private void closeSerial() {
        if (closed) {
            return;
        }
        closed = true;
        desiredBackend = null;
        lastFailure = null;
        switch (phase) {
            case EMPTY -> phase = Phase.CLOSED;
            case ACTIVE -> beginActiveRetirement();
            case POISONED -> beginRetirementAttempt(retainedOwner, retainedEpoch);
            case CREATING, RETIRING -> {
                // Their exact completion observes closed and cannot publish a
                // replacement. A late candidate is retired before CLOSED.
            }
            case CLOSED -> {
            }
        }
    }

    private void beginActiveRetirement() {
        FlutterDesignerCanvasOwner retiring = activeOwner;
        CallbackEpoch epoch = activeEpoch;
        activeOwner = null;
        activeEpoch = null;
        retainedOwner = retiring;
        retainedEpoch = epoch;
        beginRetirementAttempt(retiring, epoch);
    }

    private void beginRetirementAttempt(
            FlutterDesignerCanvasOwner owner,
            CallbackEpoch epoch) {
        if (owner == null || epoch == null) {
            IllegalStateException failure = new IllegalStateException(
                    "Canvas owner retirement lost its owner or epoch");
            phase = Phase.POISONED;
            lastFailure = failure;
            notifyObserver(() -> observer.ownerRetirementFailed(
                    owner, epoch, failure));
            return;
        }
        retainedOwner = owner;
        retainedEpoch = epoch;
        phase = Phase.RETIRING;
        lastFailure = null;
        long attempt = ++retirementAttempt;
        notifyObserver(() -> observer.ownerRetirementStarted(owner, epoch));

        final CompletionStage<Void> completion;
        try {
            completion = Objects.requireNonNull(
                    owner.preparePeerRemovalAsync(),
                    "Canvas owner returned no retirement completion");
        } catch (RuntimeException | LinkageError failure) {
            dispatch(() -> completeRetirement(
                    owner, epoch, attempt, failure));
            return;
        }
        try {
            completion.whenComplete((ignored, failure) -> dispatch(() ->
                    completeRetirement(owner, epoch, attempt, failure)));
        } catch (RuntimeException | LinkageError failure) {
            dispatch(() -> completeRetirement(
                    owner, epoch, attempt, failure));
        }
    }

    private void completeRetirement(
            FlutterDesignerCanvasOwner owner,
            CallbackEpoch epoch,
            long attempt,
            Throwable failure) {
        if (phase != Phase.RETIRING
                || retainedOwner != owner
                || retainedEpoch != epoch
                || retirementAttempt != attempt) {
            return;
        }
        Throwable terminal = unwrap(failure);
        if (terminal != null) {
            phase = Phase.POISONED;
            lastFailure = terminal;
            notifyObserver(() -> observer.ownerRetirementFailed(
                    owner, epoch, terminal));
            return;
        }

        retainedOwner = null;
        retainedEpoch = null;
        lastFailure = null;
        phase = closed ? Phase.CLOSED : Phase.EMPTY;
        notifyObserver(() -> observer.ownerRetired(owner, epoch));
        if (!closed) {
            beginCreationIfNeeded();
        }
    }

    private void beginCreationIfNeeded() {
        Backend backend = desiredBackend;
        if (closed || phase != Phase.EMPTY || backend == null) {
            return;
        }
        CallbackEpoch epoch = nextEpoch(backend);
        creatingEpoch = epoch;
        phase = Phase.CREATING;
        lastFailure = null;
        long attempt = ++creationAttempt;
        notifyObserver(() -> observer.ownerCreationStarted(backend, epoch));

        final CompletionStage<FlutterDesignerCanvasOwner> completion;
        try {
            completion = Objects.requireNonNull(
                    factory.create(backend, epoch),
                    "Canvas owner factory returned no completion");
        } catch (Exception | LinkageError failure) {
            dispatch(() -> completeCreation(
                    backend, epoch, attempt, null, failure));
            return;
        }
        try {
            completion.whenComplete((owner, failure) -> dispatch(() ->
                    completeCreation(backend, epoch, attempt, owner, failure)));
        } catch (RuntimeException | LinkageError failure) {
            dispatch(() -> completeCreation(
                    backend, epoch, attempt, null, failure));
        }
    }

    private void completeCreation(
            Backend backend,
            CallbackEpoch epoch,
            long attempt,
            FlutterDesignerCanvasOwner candidate,
            Throwable failure) {
        if (phase != Phase.CREATING
                || creatingEpoch != epoch
                || creationAttempt != attempt) {
            if (candidate != null) {
                retireLateCandidate(candidate, epoch);
            }
            return;
        }
        creatingEpoch = null;
        Throwable terminal = unwrap(failure);
        if (terminal == null && candidate == null) {
            terminal = new NullPointerException(
                    "Canvas owner factory completed without an owner");
        }
        if (terminal != null) {
            phase = closed ? Phase.CLOSED : Phase.EMPTY;
            lastFailure = terminal;
            if (!closed) {
                Throwable reported = terminal;
                notifyObserver(() -> observer.ownerCreationFailed(
                        backend, epoch, reported));
                if (desiredBackend != backend) {
                    beginCreationIfNeeded();
                }
            }
            return;
        }
        if (candidate.backend() != backend) {
            IllegalStateException mismatch = new IllegalStateException(
                    "Canvas owner factory returned backend " + candidate.backend()
                    + " for requested backend " + backend);
            lastFailure = mismatch;
            if (!closed) {
                notifyObserver(() -> observer.ownerCreationFailed(
                        backend, epoch, mismatch));
            }
            retireLateCandidate(candidate, epoch);
            return;
        }
        if (closed || desiredBackend != backend) {
            retireLateCandidate(candidate, epoch);
            return;
        }

        activeOwner = candidate;
        activeEpoch = epoch;
        phase = Phase.ACTIVE;
        lastFailure = null;
        notifyObserver(() -> observer.ownerActivated(candidate, epoch));
    }

    private void retireLateCandidate(
            FlutterDesignerCanvasOwner candidate,
            CallbackEpoch epoch) {
        retainedOwner = candidate;
        retainedEpoch = epoch;
        beginRetirementAttempt(candidate, epoch);
    }

    private CallbackEpoch nextEpoch(Backend backend) {
        if (nextEpochSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Canvas owner epoch space is exhausted");
        }
        return new CallbackEpoch(++nextEpochSequence, backend);
    }

    private void dispatch(Runnable task) {
        Objects.requireNonNull(task, "task");
        boolean schedule = false;
        synchronized (dispatchLock) {
            dispatched.addLast(task);
            if (!drainScheduled) {
                drainScheduled = true;
                schedule = true;
            }
        }
        if (!schedule) {
            return;
        }
        try {
            callbackExecutor.execute(this::drainDispatched);
        } catch (RuntimeException | LinkageError failure) {
            synchronized (dispatchLock) {
                dispatched.clear();
                drainScheduled = false;
            }
            throw failure;
        }
    }

    private void drainDispatched() {
        while (true) {
            Runnable next;
            synchronized (dispatchLock) {
                next = dispatched.pollFirst();
                if (next == null) {
                    drainScheduled = false;
                    return;
                }
            }
            try {
                next.run();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.log(Level.WARNING,
                        "Canvas owner transition callback failed", failure);
            }
        }
    }

    private void notifyObserver(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(Level.WARNING,
                    "Canvas owner transition observer failed", failure);
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    enum Phase {
        EMPTY,
        CREATING,
        ACTIVE,
        RETIRING,
        POISONED,
        CLOSED
    }

    /** Opaque identity captured by every callback installed for one owner. */
    static final class CallbackEpoch {
        private final long sequence;
        private final Backend backend;

        private CallbackEpoch(long sequence, Backend backend) {
            this.sequence = sequence;
            this.backend = Objects.requireNonNull(backend, "backend");
        }

        long sequence() {
            return sequence;
        }

        Backend backend() {
            return backend;
        }

        @Override
        public String toString() {
            return backend + "#" + sequence;
        }
    }

    @FunctionalInterface
    interface OwnerFactory {
        CompletionStage<FlutterDesignerCanvasOwner> create(
                Backend backend,
                CallbackEpoch epoch) throws Exception;
    }

    interface Observer {
        Observer NOOP = new Observer() { };

        default void ownerCreationStarted(Backend backend, CallbackEpoch epoch) { }

        default void ownerActivated(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) { }

        default void ownerRetirementStarted(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) { }

        default void ownerRetired(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch) { }

        default void ownerRetirementFailed(
                FlutterDesignerCanvasOwner owner,
                CallbackEpoch epoch,
                Throwable failure) { }

        default void ownerCreationFailed(
                Backend backend,
                CallbackEpoch epoch,
                Throwable failure) { }
    }
}

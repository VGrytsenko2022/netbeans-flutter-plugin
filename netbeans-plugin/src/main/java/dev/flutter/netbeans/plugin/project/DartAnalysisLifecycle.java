package dev.flutter.netbeans.plugin.project;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.RequestProcessor;

/** Owns the Dart language-server handle associated with one Flutter project. */
public final class DartAnalysisLifecycle {
    private static final Logger LOGGER = Logger.getLogger(DartAnalysisLifecycle.class.getName());
    private static final RequestProcessor WORKER = new RequestProcessor(
            DartAnalysisLifecycle.class.getName(),
            1,
            true);
    private static final RequestProcessor WATCHDOG = new RequestProcessor(
            DartAnalysisLifecycle.class.getName() + "-watchdog",
            1,
            true);
    private static final int CLEANUP_TIMEOUT_MILLIS = 2_000;

    private final CleanupExecutor cleanupExecutor;
    private final String projectName;
    private final DartAnalysisStatusReporter statusReporter;
    private boolean open;
    private long generation;
    private long nextAttempt;
    private long currentAttempt;
    private int pendingCleanups;
    private AnalysisState state = AnalysisState.CLOSED;
    private boolean currentStartIsRestart;
    private long startingAttempt;
    private String lastFailure;
    private final Set<String> notifiedFailures = new HashSet<>();
    private final Set<Ticket> startingTickets = new HashSet<>();
    private Registration server;

    public DartAnalysisLifecycle() {
        this("Flutter project", DartAnalysisStatusReporter.NONE,
                DartAnalysisLifecycle::executeCleanup);
    }

    DartAnalysisLifecycle(
            String projectName,
            DartAnalysisStatusReporter statusReporter) {
        this(projectName, statusReporter, DartAnalysisLifecycle::executeCleanup);
    }

    DartAnalysisLifecycle(CleanupExecutor cleanupExecutor) {
        this("Flutter project", DartAnalysisStatusReporter.NONE, cleanupExecutor);
    }

    DartAnalysisLifecycle(
            String projectName,
            DartAnalysisStatusReporter statusReporter,
            CleanupExecutor cleanupExecutor) {
        this.projectName = projectName == null || projectName.isBlank()
                ? "Flutter project"
                : projectName.strip();
        this.statusReporter = Objects.requireNonNull(statusReporter, "statusReporter");
        this.cleanupExecutor = Objects.requireNonNull(cleanupExecutor, "cleanupExecutor");
    }

    /** Marks the owning project as available for language-server startup. */
    public void open() {
        synchronized (this) {
            generation++;
            open = true;
            state = AnalysisState.IDLE;
            currentAttempt = 0;
            currentStartIsRestart = false;
            startingAttempt = 0;
            lastFailure = null;
            notifiedFailures.clear();
            report(statusReporter::clear);
        }
    }

    /**
     * Reserves the current open generation for a language-server startup.
     * Returns {@code null} while an older NetBeans binding is still being
     * removed from the LSP cache.
     */
    public synchronized Ticket acquire() {
        if (!open || pendingCleanups != 0) {
            return null;
        }
        currentAttempt = ++nextAttempt;
        return new Ticket(generation, currentAttempt);
    }

    /**
     * Marks a previously acquired generation as starting.
     *
     * @return {@code false} for a stale ticket or a duplicate concurrent start
     */
    public boolean beginStart(Ticket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        synchronized (this) {
            if (!open
                    || pendingCleanups != 0
                    || ticket.generation() != generation
                    || ticket.attempt() != currentAttempt
                    || state == AnalysisState.STARTING
                    && startingAttempt == ticket.attempt()) {
                return false;
            }
            boolean restarting = server != null || state == AnalysisState.RUNNING;
            state = AnalysisState.STARTING;
            currentStartIsRestart = restarting;
            startingAttempt = ticket.attempt();
            startingTickets.add(ticket);
            lastFailure = null;
            report(() -> statusReporter.starting(projectName, restarting));
        }
        return true;
    }

    /** Reports a concrete startup failure once per project generation and reason. */
    public void fail(Ticket ticket, String reason) {
        Objects.requireNonNull(ticket, "ticket");
        String normalized = reason == null || reason.isBlank()
                ? "The Dart Analysis Server process could not be started."
                : reason.strip();
        boolean notifyUser;
        synchronized (this) {
            if (startingTickets.remove(ticket)) {
                notifyAll();
            }
            if (!isCurrent(ticket)) {
                return;
            }
            if (state == AnalysisState.FAILED && normalized.equals(lastFailure)) {
                return;
            }
            state = AnalysisState.FAILED;
            currentStartIsRestart = false;
            startingAttempt = 0;
            lastFailure = normalized;
            notifyUser = notifiedFailures.add(normalized);
            report(() -> statusReporter.failed(projectName, normalized, notifyUser));
        }
    }

    /**
     * Attaches the server produced for a previously acquired generation.
     *
     * <p>A startup that crossed close/reopen is rejected and its raw process is
     * closed without touching the current NetBeans LSP cache entry.</p>
     *
     * @return {@code true} when the project accepted the owner
     */
    public boolean attach(Ticket ticket, AutoCloseable owner, Runnable restarter) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(owner, "owner");
        Registration previous = null;
        Registration cleanup = null;
        boolean accepted;
        boolean restarted = false;
        synchronized (this) {
            boolean startFinished = startingTickets.remove(ticket);
            accepted = open
                    && pendingCleanups == 0
                    && isCurrent(ticket);
            if (accepted) {
                previous = server;
                server = new Registration(owner, restarter);
                restarted = currentStartIsRestart || previous != null;
                state = AnalysisState.RUNNING;
                currentStartIsRestart = false;
                startingAttempt = 0;
                lastFailure = null;
                boolean wasRestarted = restarted;
                report(() -> statusReporter.running(projectName, wasRestarted));
            }
            if (!accepted) {
                cleanup = new Registration(owner, null);
            } else if (previous != null && previous.owner() != owner) {
                cleanup = new Registration(previous.owner(), null);
            }
            if (cleanup != null) {
                // Reserve cleanup before waking a deletion waiter. Scheduling
                // happens outside the lifecycle monitor.
                pendingCleanups++;
            }
            if (startFinished) {
                notifyAll();
            }
        }
        if (cleanup != null) {
            // A rejected owner was never cached; a replaced binding has
            // already been discarded. Neither cleanup may restart the cache.
            submitCleanup(cleanup, this::cleanupFinished);
        }
        return accepted;
    }

    public synchronized boolean isOpen() {
        return open;
    }

    /** Stops the project-owned server without blocking the NetBeans UI thread. */
    public void close() {
        Registration closing;
        synchronized (this) {
            open = false;
            generation++;
            state = AnalysisState.CLOSED;
            currentAttempt = 0;
            currentStartIsRestart = false;
            startingAttempt = 0;
            lastFailure = null;
            closing = server;
            server = null;
            if (closing != null) {
                pendingCleanups++;
            }
            report(statusReporter::clear);
        }
        if (closing != null) {
            submitCleanup(closing, this::cleanupFinished);
        }
    }

    private synchronized void cleanupFinished() {
        if (pendingCleanups > 0) {
            pendingCleanups--;
        }
        notifyAll();
    }

    /** Waits for Dart server startup and cleanup work owned by this project. */
    public boolean awaitQuiescence(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        long remainingNanos = timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        synchronized (this) {
            while (pendingCleanups != 0 || !startingTickets.isEmpty()) {
                if (remainingNanos <= 0) {
                    return false;
                }
                java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(
                        this,
                        remainingNanos);
                remainingNanos = deadline - System.nanoTime();
            }
            return true;
        }
    }

    private void submitCleanup(Registration registration, Runnable completion) {
        try {
            cleanupExecutor.execute(
                    registration.owner(),
                    registration.restarter(),
                    completion);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Could not schedule Dart language-server cleanup", ex);
            closeOwner(registration.owner());
            completion.run();
        }
    }

    private static void executeCleanup(
            AutoCloseable owner,
            Runnable restarter,
            Runnable completion) {
        WORKER.post(() -> {
            RequestProcessor.Task watchdog = WATCHDOG.post(
                    () -> closeOwner(owner),
                    CLEANUP_TIMEOUT_MILLIS);
            try {
                if (restarter != null) {
                    restarter.run();
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "Could not remove the Dart LSP binding", ex);
            } finally {
                watchdog.cancel();
                closeOwner(owner);
                completion.run();
            }
        });
    }

    private static void closeOwner(AutoCloseable owner) {
        try {
            owner.close();
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Could not stop the Dart language server", ex);
        }
    }

    private void report(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Could not update Dart analysis status", ex);
        }
    }

    private boolean isCurrent(Ticket ticket) {
        return ticket.generation() == generation
                && ticket.attempt() == currentAttempt;
    }

    /** Opaque startup generation; only this lifecycle can validate it. */
    public record Ticket(long generation, long attempt) {
    }

    private record Registration(AutoCloseable owner, Runnable restarter) {
    }

    private enum AnalysisState {
        CLOSED,
        IDLE,
        STARTING,
        RUNNING,
        FAILED
    }

    @FunctionalInterface
    interface CleanupExecutor {
        void execute(AutoCloseable owner, Runnable restarter, Runnable completion);
    }
}

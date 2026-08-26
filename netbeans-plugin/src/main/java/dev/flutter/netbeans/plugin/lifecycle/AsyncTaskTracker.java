package dev.flutter.netbeans.plugin.lifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/** Tracks submitted asynchronous work so destructive operations can await it. */
public final class AsyncTaskTracker implements Executor {
    private final Object lock = new Object();
    private final Executor delegate;
    private int activeTasks;

    public AsyncTaskTracker(Executor delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        taskStarted();
        try {
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    taskFinished();
                }
            });
        } catch (RuntimeException | Error ex) {
            taskFinished();
            throw ex;
        }
    }

    /** Tracks a process or service completion that may outlive its close call. */
    public void track(CompletionStage<?> completion) {
        Objects.requireNonNull(completion, "completion");
        taskStarted();
        try {
            completion.whenComplete((ignored, failure) -> taskFinished());
        } catch (RuntimeException | Error ex) {
            taskFinished();
            throw ex;
        }
    }

    /**
     * Waits until every task submitted before or during the wait has finished.
     * The delegate is never awaited while this tracker's monitor is held.
     */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        long remainingNanos = timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;
        synchronized (lock) {
            while (activeTasks != 0) {
                if (remainingNanos <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(lock, remainingNanos);
                remainingNanos = deadline - System.nanoTime();
            }
            return true;
        }
    }

    private void taskFinished() {
        synchronized (lock) {
            activeTasks--;
            if (activeTasks == 0) {
                lock.notifyAll();
            }
        }
    }

    private void taskStarted() {
        synchronized (lock) {
            activeTasks++;
        }
    }
}

package dev.flutter.netbeans.plugin.designer;

import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.openide.util.RequestProcessor;

/**
 * Synchronous work whose EDT caller must continue dispatching editor callbacks.
 * This helper grants no source, command or save authority. Callers must reserve
 * and recheck their own exact operation outside all document/session locks.
 */
final class DesignerSourceRestageWork {
    private DesignerSourceRestageWork() {
    }

    static <T> T run(Callable<T> operation) throws IOException {
        Objects.requireNonNull(operation, "operation");
        requireNotInterrupted();
        if (!EventQueue.isDispatchThread()) {
            return call(operation);
        }
        return runOnEdt(operation,
                Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop());
    }

    /** Injectable loop for deterministic rejection tests; production uses AWT. */
    static <T> T runOnEdt(Callable<T> operation, SecondaryLoop loop) throws IOException {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(loop, "loop");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("The secondary event loop requires an EDT caller.");
        }
        requireNotInterrupted();
        Work<T> work = new Work<>(operation, loop);
        // Admission itself must be an event in the nested loop. If enter()
        // refuses to start, cancellation below precedes this queued event and
        // prevents work from running later after its caller already failed.
        EventQueue.invokeLater(() -> {
            if (work.isDone()) return;
            try {
                // Do not serialize unrelated nested Save calls behind a worker
                // that may itself be waiting for an EDT callback to return.
                new RequestProcessor(DesignerSourceRestageWork.class.getName(), 1, true)
                        .post(work);
            } catch (RuntimeException | Error failure) {
                work.reject(failure);
            }
        });
        boolean entered;
        try {
            entered = loop.enter();
        } catch (RuntimeException | Error failure) {
            work.cancel(true);
            throw failure;
        }
        if (!entered) {
            work.cancel(true);
            throw new IOException("Cannot refresh Designer Source analysis: the secondary event loop could not start.");
        }
        if (Thread.currentThread().isInterrupted()) {
            work.cancel(true);
            throw interrupted(new InterruptedException("The Source analysis caller was interrupted."));
        }
        if (!work.isDone()) {
            work.cancel(true);
            throw new IOException("Cannot refresh Designer Source analysis: the event loop ended before work completed.");
        }
        // The completed-state check above is essential: get() must never wait
        // on the EDT while the worker may require invokeAndWait on that EDT.
        try {
            return work.get();
        } catch (InterruptedException failure) {
            throw interrupted(failure);
        } catch (ExecutionException failure) {
            throw propagate(failure.getCause());
        } catch (CancellationException failure) {
            throw new IOException("Designer Source analysis was cancelled.", failure);
        }
    }

    private static <T> T call(Callable<T> operation) throws IOException {
        requireNotInterrupted();
        try {
            return operation.call();
        } catch (InterruptedException failure) {
            throw interrupted(failure);
        } catch (Exception failure) {
            throw propagate(failure);
        }
    }

    private static IOException propagate(Throwable failure) {
        if (failure instanceof IOException io) return io;
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        return new IOException("Cannot refresh Designer Source analysis: "
                + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()), failure);
    }

    private static void requireNotInterrupted() throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw interrupted(new InterruptedException("The Source analysis caller was interrupted."));
        }
    }

    private static IOException interrupted(InterruptedException failure) {
        Thread.currentThread().interrupt();
        return new IOException("Designer Source analysis was interrupted.", failure);
    }

    private static final class Work<T> extends FutureTask<T> {
        private final SecondaryLoop loop;

        Work(Callable<T> operation, SecondaryLoop loop) {
            super(() -> call(operation));
            this.loop = loop;
        }

        void reject(Throwable failure) {
            setException(failure);
        }

        @Override
        protected void done() {
            // Even an immediately completed worker cannot exit before the EDT
            // has entered its nested loop: exit is another queued EDT event.
            EventQueue.invokeLater(loop::exit);
        }
    }
}

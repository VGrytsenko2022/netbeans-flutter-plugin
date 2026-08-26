package dev.flutter.netbeans.plugin.tooling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NetBeansFlutterExecutionBackendTest {
    private static final long TIMEOUT_SECONDS = 2;

    @Test
    void cancelledLogicalResultPreventsASelectedCallableFromCreatingAProcess() {
        var tracker = new NetBeansFlutterExecutionBackend.ProcessTerminationTracker();
        AtomicBoolean creatorCalled = new AtomicBoolean();

        tracker.requestCancellation();
        tracker.logicalResultFinished();

        assertTrue(tracker.completion().isDone());
        assertThrows(CancellationException.class, () -> tracker.start(() -> {
            creatorCalled.set(true);
            return new ControlledProcess();
        }));
        assertFalse(creatorCalled.get());
    }

    @Test
    void cancelledStartedProcessRemainsNonQuiescentUntilOnExit() throws Exception {
        var tracker = new NetBeansFlutterExecutionBackend.ProcessTerminationTracker();
        ControlledProcess process = new ControlledProcess();

        tracker.start(() -> process);
        tracker.requestCancellation();
        tracker.logicalResultFinished();

        assertTrue(process.destroyCalled());
        assertFalse(tracker.completion().isDone(),
                "logical cancellation must not complete physical termination");

        process.completeExit();
        tracker.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(tracker.completion().isDone());
    }

    private static final class ControlledProcess extends Process {
        private final CompletableFuture<Process> exit = new CompletableFuture<>();
        private final AtomicBoolean destroyCalled = new AtomicBoolean();
        private volatile boolean alive = true;

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() throws InterruptedException {
            try {
                return exit.get().exitValue();
            } catch (java.util.concurrent.ExecutionException ex) {
                throw new AssertionError(ex);
            }
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit)
                throws InterruptedException {
            try {
                exit.get(timeout, unit);
                return true;
            } catch (java.util.concurrent.ExecutionException ex) {
                throw new AssertionError(ex);
            } catch (java.util.concurrent.TimeoutException ex) {
                return false;
            }
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("process is still alive");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalled.set(true);
        }

        @Override
        public Process destroyForcibly() {
            destroyCalled.set(true);
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            return exit;
        }

        boolean destroyCalled() {
            return destroyCalled.get();
        }

        void completeExit() {
            alive = false;
            exit.complete(this);
        }
    }
}

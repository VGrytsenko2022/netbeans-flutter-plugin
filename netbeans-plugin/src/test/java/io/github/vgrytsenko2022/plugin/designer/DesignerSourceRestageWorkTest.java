package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class DesignerSourceRestageWorkTest {
    @Test
    void edtCallerPumpsWorkerRoundTripAndReceivesItsResult() throws Exception {
        assertEquals("finished", onEdt(() -> DesignerSourceRestageWork.run(() -> {
            assertFalse(EventQueue.isDispatchThread());
            assertTrue(Thread.currentThread().getName().contains(DesignerSourceRestageWork.class.getName()));
            AtomicInteger callback = new AtomicInteger();
            EventQueue.invokeAndWait(callback::incrementAndGet);
            assertEquals(1, callback.get());
            return "finished";
        })));
    }

    @Test
    void offEdtCallerExecutesDirectlyWithoutChangingThread() throws Exception {
        Thread caller = Thread.currentThread();
        assertFalse(EventQueue.isDispatchThread());
        assertSame(caller, DesignerSourceRestageWork.run(Thread::currentThread));
        assertNull(DesignerSourceRestageWork.run(() -> null));
    }

    @Test
    void nestedEdtWorkDoesNotQueueBehindWorkerWaitingForThatEdt() throws Exception {
        assertEquals("nested", onEdt(() -> DesignerSourceRestageWork.run(() -> {
            AtomicReference<String> nested = new AtomicReference<>();
            EventQueue.invokeAndWait(() -> {
                try {
                    nested.set(DesignerSourceRestageWork.run(() -> "nested"));
                } catch (IOException failure) {
                    throw new AssertionError(failure);
                }
            });
            return nested.get();
        })));
    }

    @Test
    void exceptionsRetainOriginalIdentityOrCheckedCauseOnBothCallPaths() throws Exception {
        IOException io = new IOException("exact admission failed");
        IllegalArgumentException runtime = new IllegalArgumentException("bad source");
        AssertionError error = new AssertionError("worker error");
        Exception checked = new Exception("checked failure");
        assertSame(io, assertThrows(IOException.class, () -> DesignerSourceRestageWork.run(() -> { throw io; })));
        assertSame(runtime, assertThrows(IllegalArgumentException.class,
                () -> DesignerSourceRestageWork.run(() -> { throw runtime; })));
        assertSame(error, assertThrows(AssertionError.class, () -> DesignerSourceRestageWork.run(() -> { throw error; })));
        assertSame(checked, assertThrows(IOException.class,
                () -> DesignerSourceRestageWork.run(() -> { throw checked; })).getCause());
        onEdt(() -> {
            assertSame(io, assertThrows(IOException.class, () -> DesignerSourceRestageWork.run(() -> { throw io; })));
            assertSame(runtime, assertThrows(IllegalArgumentException.class,
                    () -> DesignerSourceRestageWork.run(() -> { throw runtime; })));
            assertSame(error, assertThrows(AssertionError.class, () -> DesignerSourceRestageWork.run(() -> { throw error; })));
            assertSame(checked, assertThrows(IOException.class,
                    () -> DesignerSourceRestageWork.run(() -> { throw checked; })).getCause());
            return null;
        });
    }

    @Test
    void interruptedDirectCallerRetainsInterruptAndDoesNotAdmitFurtherWork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try {
            IOException failure = assertThrows(IOException.class, () -> DesignerSourceRestageWork.run(() -> {
                throw new InterruptedException("stop analysis");
            }));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            assertThrows(IOException.class, () -> DesignerSourceRestageWork.run(calls::incrementAndGet));
            assertEquals(0, calls.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptedEdtCallerIsRejectedBeforeAnyLoopOrTaskAdmission() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        onEdt(() -> {
            Thread.currentThread().interrupt();
            try {
                assertThrows(IOException.class, () -> DesignerSourceRestageWork.run(calls::incrementAndGet));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
            return null;
        });
        onEdt(() -> null);
        assertEquals(0, calls.get());
    }

    @Test
    void rejectedLoopCannotRunQueuedWorkAfterFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        onEdt(() -> {
            IOException failure = assertThrows(IOException.class, () -> DesignerSourceRestageWork.runOnEdt(
                    calls::incrementAndGet, loop(false, null)));
            assertTrue(failure.getMessage().contains("could not start"));
            return null;
        });
        onEdt(() -> null);
        assertEquals(0, calls.get(), "An unadmitted queued worker must not run after the caller receives failure.");
    }

    @Test
    void failedOrPrematureLoopCancelsItsQueuedWorker() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("AWT loop failed");
        onEdt(() -> {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> DesignerSourceRestageWork.runOnEdt(
                    calls::incrementAndGet, loop(false, failure))));
            IOException early = assertThrows(IOException.class, () -> DesignerSourceRestageWork.runOnEdt(
                    calls::incrementAndGet, loop(true, null)));
            assertTrue(early.getMessage().contains("before work completed"));
            return null;
        });
        onEdt(() -> null);
        assertEquals(0, calls.get());
    }

    private static SecondaryLoop loop(boolean accepted, RuntimeException failure) {
        return new SecondaryLoop() {
            @Override public boolean enter() {
                if (failure != null) throw failure;
                return accepted;
            }
            @Override public boolean exit() { return true; }
        };
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        EventQueue.invokeLater(task);
        return task.get(8, TimeUnit.SECONDS);
    }
}

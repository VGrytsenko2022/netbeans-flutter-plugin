package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FlutterDapLauncherTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void reportsReadyOnlyAfterEveryMilestoneInAnyOrder() throws Exception {
        List<JsonNode> milestones = List.of(
                response("attach", true, ""),
                response("configurationDone", true, ""),
                JSON.readTree("""
                        {"type":"event","event":"flutter.appStarted","body":{}}
                        """));
        int[][] orders = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2},
            {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
        };

        for (int[] order : orders) {
            FlutterDapLauncher.DapAttachTracker tracker =
                    new FlutterDapLauncher.DapAttachTracker();
            tracker.accept(milestones.get(order[0]));
            assertFalse(tracker.ready().isDone());
            tracker.accept(milestones.get(order[1]));
            assertFalse(tracker.ready().isDone());
            tracker.accept(milestones.get(order[2]));
            assertTrue(tracker.ready().isDone());
            assertFalse(tracker.ready().isCompletedExceptionally());
        }
    }

    @Test
    void watchdogClaimsTheDeadlineAndRunsTheAbortAction() {
        FlutterDapLauncher.DapAttachTracker tracker =
                new FlutterDapLauncher.DapAttachTracker();
        AtomicBoolean aborted = new AtomicBoolean();

        FlutterDapLauncher.watchAttachDeadline(
                tracker, 0, TimeUnit.MILLISECONDS, () -> aborted.set(true));

        assertTrue(aborted.get());
        assertTrue(tracker.ready().isCompletedExceptionally());
    }

    @Test
    void rejectsAFailedAttachResponse() throws Exception {
        FlutterDapLauncher.DapAttachTracker tracker =
                new FlutterDapLauncher.DapAttachTracker();

        tracker.accept(response("attach", false, "invalid VM service URI"));

        ExecutionException failure = assertThrows(
                ExecutionException.class,
                () -> tracker.ready().get());
        IOException cause = assertInstanceOf(IOException.class, failure.getCause());
        assertTrue(cause.getMessage().contains("invalid VM service URI"));
    }

    @Test
    void ignoresUnrelatedResponsesAndEvents() throws Exception {
        FlutterDapLauncher.DapAttachTracker tracker =
                new FlutterDapLauncher.DapAttachTracker();

        tracker.accept(JSON.readTree("""
                {"type":"event","event":"initialized"}
                """));
        tracker.accept(response("initialize", true, ""));

        assertFalse(tracker.ready().isDone());
    }

    @Test
    void forcedShutdownWaitsForTheAdapterProcessToActuallyExit() throws Exception {
        DelayedForcedExitProcess process = new DelayedForcedExitProcess();
        Thread closeThread = Thread.ofVirtual().start(() ->
                FlutterDapLauncher.destroyProcess(
                        process,
                        1,
                        TimeUnit.MILLISECONDS));

        assertTrue(process.awaitForcedShutdown(1, TimeUnit.SECONDS));
        assertTrue(closeThread.isAlive(),
                "close must remain observable as active while the OS process is alive");

        process.completeExit();
        closeThread.join(TimeUnit.SECONDS.toMillis(1));

        assertFalse(closeThread.isAlive());
        assertFalse(process.isAlive());
        assertEquals(1, process.destroyCalls.get());
        assertEquals(1, process.destroyForciblyCalls.get());
    }

    private static JsonNode response(String command, boolean success, String message)
            throws Exception {
        return JSON.readTree("""
                {"type":"response","request_seq":2,"success":%s,
                 "command":"%s","message":"%s"}
                """.formatted(success, command, message));
    }

    private static final class DelayedForcedExitProcess extends Process {
        private final CountDownLatch forcedShutdown = new CountDownLatch(1);
        private final CountDownLatch exited = new CountDownLatch(1);
        private final AtomicInteger destroyCalls = new AtomicInteger();
        private final AtomicInteger destroyForciblyCalls = new AtomicInteger();
        private volatile boolean alive = true;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            exited.await();
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit)
                throws InterruptedException {
            return exited.await(timeout, unit);
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("adapter is still alive");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalls.incrementAndGet();
            forcedShutdown.countDown();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        boolean awaitForcedShutdown(long timeout, TimeUnit unit)
                throws InterruptedException {
            return forcedShutdown.await(timeout, unit);
        }

        void completeExit() {
            alive = false;
            exited.countDown();
        }
    }
}

package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CanvasProcessTreeRetirementTest {
    @Test
    void retiresAnActualPortableJavaParentAndChild() throws Exception {
        String java = javaExecutable().toString();
        String classPath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path"));
        ProcessBuilder parentBuilder = new ProcessBuilder(
                java,
                ParentMain.class.getName(),
                java)
                .redirectErrorStream(true);
        // Windows CreateProcess has a much smaller command-line limit than a
        // full Maven/Surefire classpath. The environment is inherited by the
        // Java grandchild and keeps this regression test shell-free.
        parentBuilder.environment().put("CLASSPATH", classPath);
        Process parent = parentBuilder.start();
        ProcessHandle child = null;
        try {
            child = awaitLiveDescendant(parent, Duration.ofSeconds(5));

            CanvasProcessTreeRetirement.retire(parent);

            assertFalse(parent.isAlive());
            assertFalse(awaitAlive(child, false, Duration.ofSeconds(2)));
        } finally {
            parent.descendants().forEach(ProcessHandle::destroyForcibly);
            parent.destroyForcibly();
            parent.waitFor(2, TimeUnit.SECONDS);
            if (child != null && child.isAlive()) {
                child.destroyForcibly();
            }
        }
    }

    @Test
    void failsClosedWhenACapturedChildCannotBeConfirmedDead() {
        StubbornHandle child = new StubbornHandle(200_002L);
        TestProcess parent = new TestProcess(200_001L, List.of(child), false);

        IOException failure = assertThrows(IOException.class, () ->
                CanvasProcessTreeRetirement.retire(
                        parent,
                        Duration.ZERO,
                        Duration.ofMillis(20),
                        Duration.ofMillis(1)));

        assertTrue(failure.getMessage().contains("live descendants [200002]"));
        assertFalse(parent.isAlive());
        assertTrue(child.isAlive());
        assertTrue(child.forcedDestroyCalls.get() > 0);
    }

    @Test
    void failsClosedWhenTheProcessTreeCannotBeObserved() {
        TestProcess parent = new TestProcess(200_003L, List.of(), true);

        IOException failure = assertThrows(IOException.class, () ->
                CanvasProcessTreeRetirement.retire(
                        parent,
                        Duration.ZERO,
                        Duration.ZERO,
                        Duration.ofMillis(1)));

        assertTrue(failure.getMessage().contains(
                "the full process tree could not be observed"));
        assertTrue(failure.getSuppressed().length > 0);
        assertFalse(parent.isAlive());
    }

    @Test
    void forciblyRetiresAChildFirstDiscoveredDuringFinalTraversal()
            throws Exception {
        RetirableHandle lateChild = new RetirableHandle(200_005L);
        LateDescendantProcess parent = new LateDescendantProcess(
                200_004L, lateChild);

        CanvasProcessTreeRetirement.retire(
                parent,
                Duration.ZERO,
                Duration.ofMillis(20),
                Duration.ofMillis(1));

        assertFalse(parent.isAlive());
        assertFalse(lateChild.isAlive());
        assertTrue(lateChild.forcedDestroyCalls.get() > 0);
    }

    private static ProcessHandle awaitLiveDescendant(
            Process parent,
            Duration timeout) throws Exception {
        long started = System.nanoTime();
        while (System.nanoTime() - started < timeout.toNanos()) {
            Optional<ProcessHandle> child = parent.descendants()
                    .filter(ProcessHandle::isAlive)
                    .findFirst();
            if (child.isPresent()) {
                return child.orElseThrow();
            }
            if (!parent.isAlive()) {
                String output = new String(
                        parent.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
                throw new AssertionError(
                        "portable Java parent exited before starting its child: "
                        + output);
            }
            Thread.sleep(10);
        }
        throw new AssertionError("portable Java child was not observed");
    }

    private static boolean awaitAlive(
            ProcessHandle handle,
            boolean expected,
            Duration timeout) throws InterruptedException {
        long started = System.nanoTime();
        boolean alive = handle.isAlive();
        while (alive != expected
                && System.nanoTime() - started < timeout.toNanos()) {
            Thread.sleep(10);
            alive = handle.isAlive();
        }
        return alive;
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable)
                .toAbsolutePath()
                .normalize();
    }

    public static final class ParentMain {
        private ParentMain() {
        }

        public static void main(String[] args) throws Exception {
            Process child = new ProcessBuilder(
                    args[0],
                    ChildMain.class.getName())
                    .redirectErrorStream(true)
                    .start();
            child.waitFor();
        }
    }

    public static final class ChildMain {
        private ChildMain() {
        }

        public static void main(String[] args) throws Exception {
            Thread.sleep(Duration.ofMinutes(10).toMillis());
        }
    }

    private static final class TestProcess extends Process {
        private final long pid;
        private final List<ProcessHandle> descendants;
        private final boolean failObservation;
        private volatile boolean alive = true;

        private TestProcess(
                long pid,
                List<ProcessHandle> descendants,
                boolean failObservation) {
            this.pid = pid;
            this.descendants = List.copyOf(descendants);
            this.failObservation = failObservation;
        }

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
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            if (failObservation) {
                throw new UnsupportedOperationException(
                        "simulated process-tree observation failure");
            }
            return descendants.stream();
        }
    }

    private static final class StubbornHandle implements ProcessHandle {
        private final long pid;
        private final AtomicInteger forcedDestroyCalls = new AtomicInteger();

        private StubbornHandle(long pid) {
            this.pid = pid;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Optional<ProcessHandle> parent() {
            return Optional.empty();
        }

        @Override
        public Stream<ProcessHandle> children() {
            return Stream.empty();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }

        @Override
        public Info info() {
            return ProcessHandle.current().info();
        }

        @Override
        public CompletableFuture<ProcessHandle> onExit() {
            return new CompletableFuture<>();
        }

        @Override
        public boolean supportsNormalTermination() {
            return true;
        }

        @Override
        public boolean destroy() {
            return true;
        }

        @Override
        public boolean destroyForcibly() {
            forcedDestroyCalls.incrementAndGet();
            return true;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public int compareTo(ProcessHandle other) {
            return Long.compare(pid, other.pid());
        }
    }

    private static final class RetirableHandle implements ProcessHandle {
        private final long pid;
        private final AtomicInteger forcedDestroyCalls = new AtomicInteger();
        private volatile boolean alive = true;

        private RetirableHandle(long pid) {
            this.pid = pid;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Optional<ProcessHandle> parent() {
            return Optional.empty();
        }

        @Override
        public Stream<ProcessHandle> children() {
            return Stream.empty();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }

        @Override
        public Info info() {
            return ProcessHandle.current().info();
        }

        @Override
        public CompletableFuture<ProcessHandle> onExit() {
            return alive
                    ? new CompletableFuture<>()
                    : CompletableFuture.completedFuture(this);
        }

        @Override
        public boolean supportsNormalTermination() {
            return true;
        }

        @Override
        public boolean destroy() {
            return true;
        }

        @Override
        public boolean destroyForcibly() {
            forcedDestroyCalls.incrementAndGet();
            alive = false;
            return true;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public int compareTo(ProcessHandle other) {
            return Long.compare(pid, other.pid());
        }
    }

    private static final class LateDescendantProcess extends Process {
        private final long pid;
        private final ProcessHandle lateChild;
        private final AtomicInteger observations = new AtomicInteger();
        private volatile boolean alive = true;

        private LateDescendantProcess(long pid, ProcessHandle lateChild) {
            this.pid = pid;
            this.lateChild = lateChild;
        }

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
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return observations.incrementAndGet() <= 2
                    ? Stream.empty()
                    : Stream.of(lateChild);
        }
    }
}

package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DartAnalysisServerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void startsExpectedLspCommandInProjectDirectory() throws Exception {
        RecordingFactory factory = new RecordingFactory(new FakeProcess("", "", false));
        Path executable = temporaryDirectory.resolve("sdk/bin/dart.exe");
        Path project = temporaryDirectory.resolve("sample_app");
        DartAnalysisServer server = server(executable, project, line -> { }, factory);

        server.start();

        assertEquals(List.of(
                executable.toAbsolutePath().normalize().toString(),
                "language-server",
                "--protocol=lsp",
                "--client-id=netbeans-flutter",
                "--client-version=0.1.0"), factory.command);
        assertEquals(project.toAbsolutePath().normalize(), factory.workingDirectory);
        server.close();
    }

    @Test
    void keepsProtocolStdoutSeparateAndDrainsStderrAsynchronously() throws Exception {
        CountDownLatch stderrLines = new CountDownLatch(2);
        List<String> observedStderr = new ArrayList<>();
        FakeProcess process = new FakeProcess(
                "Content-Length: 2\r\n\r\n{}",
                "first warning\nsecond warning\n",
                false);
        RecordingFactory factory = new RecordingFactory(process);
        DartAnalysisServer server = server(
                temporaryDirectory.resolve("dart"),
                temporaryDirectory,
                line -> {
                    synchronized (observedStderr) {
                        observedStderr.add(line);
                    }
                    stderrLines.countDown();
                },
                factory);

        server.start();
        String protocol = new String(server.inputStream().readAllBytes(), StandardCharsets.UTF_8);
        server.outputStream().write("request".getBytes(StandardCharsets.UTF_8));

        assertTrue(stderrLines.await(2, TimeUnit.SECONDS));
        assertEquals("Content-Length: 2\r\n\r\n{}", protocol);
        assertFalse(protocol.contains("warning"));
        synchronized (observedStderr) {
            assertEquals(List.of("first warning", "second warning"), observedStderr);
        }
        assertEquals("request", process.stdin.toString(StandardCharsets.UTF_8));
        server.close();
    }

    @Test
    void repeatedStartReusesLiveProcessAndStartAfterCloseCreatesAnother() throws Exception {
        FakeProcess first = new FakeProcess("", "", false);
        FakeProcess second = new FakeProcess("", "", false);
        RecordingFactory factory = new RecordingFactory(first, second);
        DartAnalysisServer server = server(
                temporaryDirectory.resolve("dart"),
                temporaryDirectory,
                line -> { },
                factory);

        DartAnalysisServer.Connection firstConnection = server.start();
        DartAnalysisServer.Connection repeatedConnection = server.start();

        assertSame(firstConnection, repeatedConnection);
        assertEquals(1, factory.starts.get());
        assertSame(first, server.process());

        server.close();
        server.close();
        assertEquals(1, first.destroyCalls.get());
        assertFalse(server.isRunning());
        assertThrows(IllegalStateException.class, server::inputStream);

        DartAnalysisServer.Connection secondConnection = server.start();

        assertSame(second, secondConnection.process());
        assertEquals(2, factory.starts.get());
        assertTrue(server.isRunning());
        server.close();
    }

    @Test
    void closeForciblyTerminatesProcessThatIgnoresGracefulDestroy() throws Exception {
        FakeProcess process = new FakeProcess("", "", true);
        RecordingFactory factory = new RecordingFactory(process);
        DartAnalysisServer server = new DartAnalysisServer(
                temporaryDirectory.resolve("dart"),
                temporaryDirectory,
                line -> { },
                factory,
                Duration.ofMillis(1));

        server.start();
        server.close();

        assertEquals(1, process.destroyCalls.get());
        assertEquals(1, process.destroyForciblyCalls.get());
        assertFalse(process.isAlive());
    }

    private DartAnalysisServer server(
            Path executable,
            Path project,
            java.util.function.Consumer<String> stderr,
            RecordingFactory factory) {
        return new DartAnalysisServer(
                executable,
                project,
                stderr,
                factory,
                Duration.ofMillis(20));
    }

    private static final class RecordingFactory implements DartAnalysisServer.ProcessFactory {
        private final Deque<FakeProcess> processes;
        private final AtomicInteger starts = new AtomicInteger();
        private List<String> command;
        private Path workingDirectory;

        RecordingFactory(FakeProcess... processes) {
            this.processes = new ArrayDeque<>(List.of(processes));
        }

        @Override
        public Process start(List<String> command, Path workingDirectory) throws IOException {
            this.command = List.copyOf(command);
            this.workingDirectory = workingDirectory;
            starts.incrementAndGet();
            FakeProcess process = processes.pollFirst();
            if (process == null) {
                throw new IOException("No fake process available");
            }
            return process;
        }
    }

    private static final class FakeProcess extends Process {
        private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        private final InputStream stdout;
        private final InputStream stderr;
        private final boolean ignoreGracefulDestroy;
        private final AtomicInteger destroyCalls = new AtomicInteger();
        private final AtomicInteger destroyForciblyCalls = new AtomicInteger();
        private volatile boolean alive = true;

        FakeProcess(String stdout, String stderr, boolean ignoreGracefulDestroy) {
            this.stdout = new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8));
            this.stderr = new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8));
            this.ignoreGracefulDestroy = ignoreGracefulDestroy;
        }

        @Override
        public OutputStream getOutputStream() {
            return stdin;
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() throws InterruptedException {
            while (alive) {
                Thread.sleep(1);
            }
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
            if (!ignoreGracefulDestroy) {
                alive = false;
            }
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalls.incrementAndGet();
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }
}

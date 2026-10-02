package io.github.vgrytsenko2022.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.FlutterSdk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterCliTest {
    @Test
    void drainsLargeStdoutAndStderrWithoutBlockingProcess() throws Exception {
        FlutterCli cli = javaBackedCli();

        var result = cli.execute(
                Path.of(".").toAbsolutePath(),
                Duration.ofSeconds(15),
                "-cp",
                System.getProperty("java.class.path"),
                NoisyProcess.class.getName());

        assertEquals(0, result.exitCode());
        assertEquals(200_000, result.stdout().length());
        assertEquals(200_000, result.stderr().length());
    }

    @Test
    void terminatesCommandAndReportsTimeout() throws Exception {
        FlutterCli cli = javaBackedCli();

        var result = cli.execute(
                Path.of(".").toAbsolutePath(),
                Duration.ofMillis(100),
                "-cp",
                System.getProperty("java.class.path"),
                SleepingProcess.class.getName());

        assertEquals(-1, result.exitCode());
        assertTrue(result.stderr().contains("timed out"));
    }

    @Test
    void interruptionTerminatesOwnedCommandAndReturnsPromptly(@TempDir Path tempDirectory)
            throws Exception {
        FlutterCli cli = javaBackedCli();
        Path pidFile = tempDirectory.resolve("child.pid");
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread caller = Thread.ofPlatform().name("flutter-cli-interruption-test").start(() -> {
            try {
                cli.execute(
                        Path.of(".").toAbsolutePath(),
                        Duration.ofMinutes(1),
                        "-cp",
                        System.getProperty("java.class.path"),
                        PidProcess.class.getName(),
                        pidFile.toString());
            } catch (Throwable ex) {
                outcome.set(ex);
            }
        });
        long childPid = awaitPid(pidFile);

        caller.interrupt();
        caller.join(5_000);

        assertFalse(caller.isAlive(), "interrupted Flutter CLI caller did not return");
        assertTrue(outcome.get() instanceof InterruptedException,
                "caller must receive InterruptedException, but got " + outcome.get());
        ProcessHandle child = ProcessHandle.of(childPid).orElse(null);
        if (child != null) {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (child.isAlive() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertFalse(child.isAlive(), "interrupted Flutter CLI child process is still alive");
        }
    }

    private static long awaitPid(Path pidFile) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(pidFile)) {
                String value = Files.readString(pidFile).strip();
                if (!value.isEmpty()) {
                    return Long.parseLong(value);
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("child process did not publish its PID");
    }

    private static FlutterCli javaBackedCli() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", executable);
        return new FlutterCli(new FlutterSdk(javaExecutable.getParent(), javaExecutable));
    }

    public static final class NoisyProcess {
        public static void main(String[] args) {
            System.out.print("o".repeat(200_000));
            System.err.print("e".repeat(200_000));
        }
    }

    public static final class SleepingProcess {
        public static void main(String[] args) throws Exception {
            Thread.sleep(30_000);
        }
    }

    public static final class PidProcess {
        public static void main(String[] args) throws Exception {
            Files.writeString(Path.of(args[0]), Long.toString(ProcessHandle.current().pid()));
            Thread.sleep(30_000);
        }
    }
}

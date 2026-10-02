package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AndroidProcessSupportTest {
    @Test
    void drainsLargeStdoutAndStderrWithoutDeadlock() throws Exception {
        AndroidProcessSupport runner = new AndroidProcessSupport();

        var result = runner.execute(
                javaExecutable(), Path.of("."), Duration.ofSeconds(15), Map.of(), null,
                List.of("-cp", System.getProperty("java.class.path"), NoisyCommand.class.getName()));

        assertEquals(0, result.exitCode());
        assertEquals(200_000, result.stdout().length());
        assertEquals(200_000, result.stderr().length());
    }

    @Test
    void timeoutTerminatesOwnedProcessTree(@TempDir Path directory) throws Exception {
        Path childPidFile = directory.resolve("timeout-child.pid");
        AndroidProcessSupport runner = new AndroidProcessSupport();

        var result = runner.execute(
                javaExecutable(), directory, Duration.ofMillis(750), Map.of(), null,
                List.of("-cp", System.getProperty("java.class.path"),
                        ParentCommand.class.getName(), childPidFile.toString()));

        assertEquals(-1, result.exitCode());
        assertTrue(result.stderr().contains("timed out"));
        assertProcessStopped(awaitPid(childPidFile));
    }

    @Test
    void interruptionTerminatesOwnedProcessTreeAndPreservesInterrupt(@TempDir Path directory)
            throws Exception {
        Path childPidFile = directory.resolve("interrupt-child.pid");
        AndroidProcessSupport runner = new AndroidProcessSupport();
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread caller = Thread.ofPlatform().name("android-command-interruption-test").start(() -> {
            try {
                runner.execute(
                        javaExecutable(), directory, Duration.ofMinutes(1), Map.of(), null,
                        List.of("-cp", System.getProperty("java.class.path"),
                                ParentCommand.class.getName(), childPidFile.toString()));
            } catch (Throwable ex) {
                outcome.set(ex);
            }
        });
        long childPid = awaitPid(childPidFile);

        caller.interrupt();
        caller.join(5_000);

        assertFalse(caller.isAlive(), "interrupted Android command did not return");
        assertTrue(outcome.get() instanceof InterruptedException,
                "caller must receive InterruptedException, got " + outcome.get());
        assertProcessStopped(childPid);
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static long awaitPid(Path file) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(file)) {
                String text = Files.readString(file).strip();
                if (!text.isBlank()) {
                    return Long.parseLong(text);
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("child process did not publish its PID");
    }

    private static void assertProcessStopped(long pid) throws Exception {
        ProcessHandle child = ProcessHandle.of(pid).orElse(null);
        if (child == null) {
            return;
        }
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (child.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(child.isAlive(), "owned Android command descendant is still alive");
    }

    public static final class NoisyCommand {
        public static void main(String[] args) {
            System.out.print("o".repeat(200_000));
            System.err.print("e".repeat(200_000));
        }
    }

    public static final class ParentCommand {
        public static void main(String[] args) throws Exception {
            Process child = new ProcessBuilder(
                    javaExecutable().toString(),
                    "-cp", System.getProperty("java.class.path"),
                    SleepingChild.class.getName()).start();
            Files.writeString(Path.of(args[0]), Long.toString(child.pid()));
            Thread.sleep(30_000);
        }
    }

    public static final class SleepingChild {
        public static void main(String[] args) throws Exception {
            Thread.sleep(30_000);
        }
    }
}

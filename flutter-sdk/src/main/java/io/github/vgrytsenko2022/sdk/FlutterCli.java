package io.github.vgrytsenko2022.sdk;

import io.github.vgrytsenko2022.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FlutterCli {
    private final FlutterSdk sdk;
    public FlutterCli(FlutterSdk sdk) { this.sdk = Objects.requireNonNull(sdk); }

    public ProcessResult execute(Path workingDirectory, Duration timeout, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(sdk.flutterExecutable().toString());
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).directory(workingDirectory.toFile()).start();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        Thread stdoutReader = drain(p.getInputStream(), stdout, readFailure, "flutter-cli-stdout");
        Thread stderrReader = drain(p.getErrorStream(), stderr, readFailure, "flutter-cli-stderr");

        final boolean done;
        try {
            done = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!done) {
                destroyProcessTree(p);
                p.waitFor();
            }
            stdoutReader.join();
            stderrReader.join();
        } catch (InterruptedException ex) {
            destroyProcessTree(p);
            awaitUninterruptibly(p, stdoutReader, stderrReader);
            Thread.currentThread().interrupt();
            throw ex;
        }

        IOException failure = readFailure.get();
        if (failure != null) {
            throw failure;
        }
        String out = stdout.toString(StandardCharsets.UTF_8);
        String err = stderr.toString(StandardCharsets.UTF_8);
        if (!done) {
            String timeoutMessage = "Flutter command timed out after " + timeout + ".";
            return new ProcessResult(-1, out, err.isBlank() ? timeoutMessage : err + System.lineSeparator() + timeoutMessage);
        }
        return new ProcessResult(p.exitValue(), out, err);
    }

    private static void destroyProcessTree(Process process) {
        List<ProcessHandle> tree = new ArrayList<>(process.descendants().toList());
        tree.add(process.toHandle());
        for (ProcessHandle handle : tree) {
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        }
    }

    private static void awaitUninterruptibly(Process process, Thread... readers) {
        boolean interruptedAgain = false;
        while (process.isAlive()) {
            try {
                process.waitFor();
            } catch (InterruptedException ex) {
                interruptedAgain = true;
                destroyProcessTree(process);
            }
        }
        for (Thread reader : readers) {
            while (reader.isAlive()) {
                try {
                    reader.join();
                } catch (InterruptedException ex) {
                    interruptedAgain = true;
                }
            }
        }
        if (interruptedAgain) {
            Thread.currentThread().interrupt();
        }
    }

    private static Thread drain(
            InputStream input,
            OutputStream output,
            AtomicReference<IOException> failure,
            String threadName) {
        return Thread.ofVirtual().name(threadName).start(() -> {
            try (input) {
                input.transferTo(output);
            } catch (IOException ex) {
                failure.compareAndSet(null, ex);
            }
        });
    }

    public Process start(Path workingDirectory, List<String> args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(sdk.flutterExecutable().toString()); cmd.addAll(args);
        return new ProcessBuilder(cmd).directory(workingDirectory.toFile()).redirectErrorStream(true).start();
    }
}

package io.github.vgrytsenko2022.run;

import io.github.vgrytsenko2022.api.ProcessResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class AndroidProcessSupport implements AndroidCommandExecutor, AndroidProcessStarter {
    private static final Duration TERMINATION_GRACE = Duration.ofSeconds(2);

    @Override
    public ProcessResult execute(
            Path executable,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment,
            String standardInput,
            List<String> arguments) throws IOException, InterruptedException {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Android command timeout must be positive");
        }
        Process process = start(executable, workingDirectory, environment, arguments);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        Thread stdoutReader = drain(process.getInputStream(), stdout, readFailure,
                "android-command-stdout");
        Thread stderrReader = drain(process.getErrorStream(), stderr, readFailure,
                "android-command-stderr");

        try (OutputStream input = process.getOutputStream()) {
            if (standardInput != null && !standardInput.isEmpty()) {
                input.write(standardInput.getBytes(StandardCharsets.UTF_8));
                input.flush();
            }
        } catch (IOException ex) {
            if (process.isAlive()) {
                terminateUninterruptibly(process);
                throw ex;
            }
        }

        final boolean completed;
        try {
            completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                terminate(process, TERMINATION_GRACE);
            }
            join(stdoutReader, stderrReader);
        } catch (InterruptedException ex) {
            terminateUninterruptibly(process);
            joinUninterruptibly(stdoutReader, stderrReader);
            Thread.currentThread().interrupt();
            throw ex;
        }

        IOException failure = readFailure.get();
        if (failure != null) {
            throw failure;
        }
        String out = stdout.toString(StandardCharsets.UTF_8);
        String err = stderr.toString(StandardCharsets.UTF_8);
        if (!completed) {
            String message = "Android command timed out after " + timeout + ".";
            return new ProcessResult(-1, out,
                    err.isBlank() ? message : err + System.lineSeparator() + message);
        }
        return new ProcessResult(process.exitValue(), out, err);
    }

    @Override
    public Process start(
            Path executable,
            Path workingDirectory,
            Map<String, String> environment,
            List<String> arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable.toAbsolutePath().normalize().toString());
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toAbsolutePath().normalize().toFile());
        builder.environment().putAll(environment);
        return builder.start();
    }

    static void terminate(Process process, Duration gracePeriod) throws InterruptedException {
        List<ProcessHandle> tree = processTree(process);
        destroy(tree, false);
        process.destroy();
        if (!process.waitFor(gracePeriod.toMillis(), TimeUnit.MILLISECONDS)) {
            destroy(tree, true);
            process.destroyForcibly();
            process.waitFor();
        } else {
            destroy(tree, true);
        }
    }

    static void terminateUninterruptibly(Process process) {
        boolean interrupted = false;
        while (process.isAlive()) {
            try {
                terminate(process, TERMINATION_GRACE);
            } catch (InterruptedException ex) {
                interrupted = true;
                destroy(processTree(process), true);
                process.destroyForcibly();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<ProcessHandle> processTree(Process process) {
        List<ProcessHandle> tree = new ArrayList<>();
        try {
            tree.addAll(process.descendants().toList());
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Synthetic test processes and restricted runtimes may not expose handles.
        }
        return tree;
    }

    private static void destroy(List<ProcessHandle> tree, boolean forcibly) {
        for (int index = tree.size() - 1; index >= 0; index--) {
            ProcessHandle handle = tree.get(index);
            if (handle.isAlive()) {
                if (forcibly) {
                    handle.destroyForcibly();
                } else {
                    handle.destroy();
                }
            }
        }
    }

    private static Thread drain(
            InputStream input,
            OutputStream output,
            AtomicReference<IOException> failure,
            String name) {
        return Thread.ofVirtual().name(name).start(() -> {
            try (input) {
                input.transferTo(output);
            } catch (IOException ex) {
                failure.compareAndSet(null, ex);
            }
        });
    }

    private static void join(Thread... threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void joinUninterruptibly(Thread... threads) {
        boolean interrupted = false;
        for (Thread thread : threads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

package dev.flutter.netbeans.run;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Managed long-lived Android Emulator process with bounded diagnostic output.
 * Closing this handle only detaches the caller; use {@link #terminate()} or the service's
 * explicit Stop operation to terminate the emulator.
 */
public final class AndroidEmulatorProcess implements AutoCloseable {
    private static final int OUTPUT_LIMIT = 256 * 1024;
    private static final Duration STOP_GRACE = Duration.ofSeconds(2);

    private final String avdId;
    private final Process process;
    private final LimitedOutputStream stdout = new LimitedOutputStream(OUTPUT_LIMIT);
    private final LimitedOutputStream stderr = new LimitedOutputStream(OUTPUT_LIMIT);
    private final CompletableFuture<Integer> completion = new CompletableFuture<>();

    AndroidEmulatorProcess(
            String avdId,
            Process process,
            BiConsumer<AndroidEmulatorProcess, Integer> exitListener) {
        this.avdId = avdId;
        this.process = process;
        Thread out = drain(process.getInputStream(), stdout, "android-emulator-stdout");
        Thread err = drain(process.getErrorStream(), stderr, "android-emulator-stderr");
        Thread.ofVirtual().name("android-emulator-waiter").start(() -> {
            try {
                int code = process.waitFor();
                out.join();
                err.join();
                completion.complete(code);
                if (exitListener != null) {
                    exitListener.accept(this, code);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                completion.completeExceptionally(ex);
            }
        });
    }

    public String avdId() {
        return avdId;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public OptionalInt exitCode() {
        if (process.isAlive()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(process.exitValue());
        } catch (IllegalThreadStateException ex) {
            return OptionalInt.empty();
        }
    }

    public CompletableFuture<Integer> completion() {
        return completion;
    }

    public String standardOutput() {
        return stdout.text();
    }

    public String standardError() {
        return stderr.text();
    }

    public String diagnosticOutput() {
        String error = standardError();
        return error.isBlank() ? standardOutput() : error;
    }

    public void terminate() throws InterruptedException {
        if (process.isAlive()) {
            AndroidProcessSupport.terminate(process, STOP_GRACE);
        }
    }

    /** Detaches this handle without terminating the successfully launched emulator. */
    @Override
    public void close() {
        // The reader and waiter are virtual threads and finish when the emulator exits.
    }

    private static Thread drain(InputStream input, OutputStream output, String name) {
        return Thread.ofVirtual().name(name).start(() -> {
            try (input) {
                input.transferTo(output);
            } catch (IOException ignored) {
                // Process termination commonly closes the pipe before the reader finishes.
            }
        });
    }

    private static final class LimitedOutputStream extends OutputStream {
        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        LimitedOutputStream(int limit) {
            this.limit = limit;
        }

        @Override
        public synchronized void write(int value) {
            if (bytes.size() < limit) {
                bytes.write(value);
            }
        }

        @Override
        public synchronized void write(byte[] buffer, int offset, int length) {
            int accepted = Math.min(length, limit - bytes.size());
            if (accepted > 0) {
                bytes.write(buffer, offset, accepted);
            }
        }

        synchronized String text() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}

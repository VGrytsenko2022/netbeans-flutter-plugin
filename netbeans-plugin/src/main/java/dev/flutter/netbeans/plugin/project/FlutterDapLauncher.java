package dev.flutter.netbeans.plugin.project;

import com.fasterxml.jackson.databind.JsonNode;
import dev.flutter.netbeans.api.FlutterSdk;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.netbeans.modules.lsp.client.debugger.api.DAPConfiguration;

/** Starts Flutter's SDK DAP adapter and connects it to NetBeans' DAP client. */
final class FlutterDapLauncher implements AutoCloseable {
    private static final long ATTACH_TIMEOUT_SECONDS = 30;

    private final Consumer<String> diagnosticOutput;
    private final Consumer<Integer> unexpectedExit;
    private volatile Process adapterProcess;
    private DapAttachTracker pendingAttach;
    private boolean closed;

    FlutterDapLauncher(Consumer<String> diagnosticOutput) {
        this(diagnosticOutput, _exitCode -> {
        });
    }

    FlutterDapLauncher(
            Consumer<String> diagnosticOutput,
            Consumer<Integer> unexpectedExit) {
        this.diagnosticOutput = Objects.requireNonNull(diagnosticOutput);
        this.unexpectedExit = Objects.requireNonNull(unexpectedExit);
    }

    void attach(
            FlutterSdk sdk,
            Path projectRoot,
            String projectName,
            String deviceId,
            URI vmServiceUri) throws IOException {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IOException("a Flutter target device is required for debugging");
        }

        Process process;
        DapAttachTracker attachTracker = new DapAttachTracker();
        synchronized (this) {
            if (closed) {
                throw new IOException("the Flutter debug adapter launcher is already closed");
            }
            if (adapterProcess != null && adapterProcess.isAlive()) {
                throw new IOException("a Flutter debug adapter is already connected");
            }
            process = new ProcessBuilder(
                    sdk.flutterExecutable().toString(),
                    "debug-adapter",
                    "--no-dds")
                    .directory(projectRoot.toFile())
                    .start();
            adapterProcess = process;
            pendingAttach = attachTracker;
        }
        drainDiagnostics(process);

        Path program = projectRoot.resolve("lib").resolve("main.dart");
        Map<String, Object> configuration = Map.of(
                "name", projectName,
                "cwd", projectRoot.toString(),
                "program", program.toString(),
                "vmServiceUri", vmServiceUri.toString(),
                "toolArgs", List.of("--device-id", deviceId),
                "additionalProjectPaths", List.of(projectRoot.toString()),
                "allowAnsiColorOutput", true,
                "debugSdkLibraries", false,
                "debugExternalPackageLibraries", false);

        try {
            process.onExit().thenAccept(exited -> attachTracker.fail(
                    "Flutter's debug adapter stopped with exit code "
                    + exited.exitValue() + " while NetBeans was attaching"));
            startAttachWatchdog(process, attachTracker);
            DAPConfiguration.create(
                            new DapMessageNormalizerInputStream(
                                    process.getInputStream(), attachTracker),
                            process.getOutputStream())
                    .setSessionName("Flutter: " + projectName)
                    .addConfiguration(configuration)
                    .attach();
            awaitDebuggerReady(attachTracker);
            synchronized (this) {
                if (adapterProcess != process || !process.isAlive()) {
                    throw new IOException("Flutter's debug adapter stopped before NetBeans "
                            + "finished attaching");
                }
                if (pendingAttach == attachTracker) {
                    pendingAttach = null;
                }
            }
            monitorUnexpectedExit(process);
        } catch (RuntimeException ex) {
            close();
            throw new IOException("NetBeans could not connect to Flutter's debug adapter: "
                    + messageOf(ex), ex);
        } catch (IOException ex) {
            close();
            throw ex;
        }
    }

    private static void awaitDebuggerReady(DapAttachTracker tracker) throws IOException {
        try {
            tracker.ready().get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for the NetBeans debugger", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("the Flutter DAP attach handshake failed: "
                    + messageOf(cause), cause);
        }
    }

    private void startAttachWatchdog(Process process, DapAttachTracker tracker) {
        Thread.ofVirtual().name("flutter-dap-attach-watchdog").start(() ->
                watchAttachDeadline(
                        tracker,
                        ATTACH_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                        () -> abortAttach(process, tracker)));
    }

    static void watchAttachDeadline(
            DapAttachTracker tracker,
            long timeout,
            TimeUnit unit,
            Runnable timeoutAction) {
        try {
            tracker.ready().get(timeout, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            // Another terminal path (adapter exit, rejection or close) won.
        } catch (TimeoutException ex) {
            String message = "NetBeans did not finish the Flutter DAP attach handshake within "
                    + timeout + " " + unit.name().toLowerCase(java.util.Locale.ROOT);
            if (tracker.fail(message)) {
                timeoutAction.run();
            }
        }
    }

    private void abortAttach(Process process, DapAttachTracker tracker) {
        boolean owned;
        synchronized (this) {
            owned = adapterProcess == process && pendingAttach == tracker;
            if (owned) {
                closed = true;
                adapterProcess = null;
                pendingAttach = null;
            }
        }
        if (owned) {
            destroyProcess(process);
        }
    }

    boolean isAlive() {
        Process process = adapterProcess;
        return process != null && process.isAlive();
    }

    private void drainDiagnostics(Process process) {
        Thread.ofVirtual().name("flutter-dap-stderr").start(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    diagnosticOutput.accept("[Flutter debugger] " + line);
                }
            } catch (IOException ex) {
                if (process.isAlive()) {
                    diagnosticOutput.accept("[Flutter debugger] Cannot read adapter diagnostics: "
                            + messageOf(ex));
                }
            }
        });
    }

    private void monitorUnexpectedExit(Process process) {
        process.onExit().thenAccept(exited -> {
            boolean report;
            synchronized (this) {
                report = !closed && adapterProcess == process;
                if (report) {
                    adapterProcess = null;
                }
            }
            if (report) {
                unexpectedExit.accept(exited.exitValue());
            }
        });
    }

    @Override
    public void close() {
        Process process;
        DapAttachTracker tracker;
        synchronized (this) {
            closed = true;
            process = adapterProcess;
            adapterProcess = null;
            tracker = pendingAttach;
            pendingAttach = null;
        }
        if (tracker != null) {
            tracker.fail("the Flutter DAP attach was cancelled because its launcher was closed");
        }
        destroyProcess(process);
    }

    private static void destroyProcess(Process process) {
        destroyProcess(process, 2, TimeUnit.SECONDS);
    }

    static void destroyProcess(
            Process process,
            long gracefulTimeout,
            TimeUnit timeoutUnit) {
        if (process == null || !process.isAlive()) {
            return;
        }
        Objects.requireNonNull(timeoutUnit, "timeoutUnit");
        if (gracefulTimeout < 0) {
            throw new IllegalArgumentException("gracefulTimeout must not be negative");
        }

        boolean interrupted = false;
        process.destroy();
        try {
            if (process.waitFor(gracefulTimeout, timeoutUnit)) {
                return;
            }
        } catch (InterruptedException ex) {
            interrupted = true;
        }

        Process forced = process.isAlive()
                ? process.destroyForcibly()
                : process;
        while (forced.isAlive()) {
            try {
                forced.waitFor();
            } catch (InterruptedException ex) {
                // Closing the project must not release deletion quiescence while
                // the adapter still owns the project working directory. Preserve
                // cancellation for the caller after the OS process has exited.
                interrupted = true;
                forced.destroyForcibly();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static String messageOf(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    /** Tracks the DAP acknowledgements and Flutter event that make debugging usable. */
    static final class DapAttachTracker implements Consumer<JsonNode> {
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private boolean attachAccepted;
        private boolean configurationDoneAccepted;
        private boolean flutterAppStarted;

        @Override
        public synchronized void accept(JsonNode message) {
            if (ready.isDone()) {
                return;
            }
            if ("event".equals(message.path("type").asText())
                    && "flutter.appStarted".equals(message.path("event").asText())) {
                flutterAppStarted = true;
                completeIfReady();
                return;
            }
            if (!"response".equals(message.path("type").asText())) {
                return;
            }
            String command = message.path("command").asText();
            if (!"attach".equals(command) && !"configurationDone".equals(command)) {
                return;
            }
            if (!message.path("success").asBoolean(false)) {
                String detail = message.path("message").asText();
                fail("Flutter's debug adapter rejected the " + command + " request"
                        + (detail.isBlank() ? "" : ": " + detail));
                return;
            }
            if ("attach".equals(command)) {
                attachAccepted = true;
            } else {
                configurationDoneAccepted = true;
            }
            completeIfReady();
        }

        private void completeIfReady() {
            if (attachAccepted && configurationDoneAccepted && flutterAppStarted) {
                ready.complete(null);
            }
        }

        synchronized boolean fail(String message) {
            return ready.completeExceptionally(new IOException(message));
        }

        CompletableFuture<Void> ready() {
            return ready;
        }
    }
}

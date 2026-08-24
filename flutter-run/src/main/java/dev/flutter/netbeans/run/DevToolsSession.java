package dev.flutter.netbeans.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Managed {@code dart devtools --machine} server process. */
public final class DevToolsSession implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration STOP_GRACE_PERIOD = Duration.ofSeconds(3);
    private static final int EARLY_OUTPUT_LIMIT = 100;
    private static final AtomicLong SESSION_IDS = new AtomicLong();

    private final Process process;
    private final URI vmServiceUri;
    private final CompletableFuture<URI> browserUri = new CompletableFuture<>();
    private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();
    private final CopyOnWriteArrayList<Consumer<String>> outputListeners =
            new CopyOnWriteArrayList<>();
    private final Object outputLock = new Object();
    private final List<String> earlyOutput = new ArrayList<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean();

    DevToolsSession(Process process, URI vmServiceUri) {
        this.process = Objects.requireNonNull(process, "process");
        this.vmServiceUri = Objects.requireNonNull(vmServiceUri, "vmServiceUri");

        long id = SESSION_IDS.incrementAndGet();
        Thread stdoutReader = Thread.ofVirtual()
                .name("flutter-devtools-stdout-" + id)
                .start(() -> drain(process.getInputStream(), true));
        Thread stderrReader = Thread.ofVirtual()
                .name("flutter-devtools-stderr-" + id)
                .start(() -> drain(process.getErrorStream(), false));
        Thread.ofVirtual()
                .name("flutter-devtools-exit-" + id)
                .start(() -> awaitExit(stdoutReader, stderrReader));
    }

    public CompletableFuture<URI> browserUri() {
        return browserUri.copy();
    }

    public CompletableFuture<Integer> exitCode() {
        return exitCode.copy();
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public boolean stopRequested() {
        return stopRequested.get();
    }

    public void addOutputListener(Consumer<String> listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (outputLock) {
            outputListeners.add(listener);
            for (String line : earlyOutput) {
                notifyOutputListener(listener, line);
            }
            earlyOutput.clear();
        }
    }

    public void removeOutputListener(Consumer<String> listener) {
        outputListeners.remove(listener);
    }

    public void stop() {
        if (!stopRequested.compareAndSet(false, true)) {
            return;
        }
        destroyProcessTree(process, false);
        Thread.ofVirtual().name("flutter-devtools-stop").start(() -> {
            try {
                if (!process.waitFor(STOP_GRACE_PERIOD.toMillis(), TimeUnit.MILLISECONDS)) {
                    destroyProcessTree(process, true);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                destroyProcessTree(process, true);
            }
        });
    }

    @Override
    public void close() {
        stop();
    }

    private void drain(InputStream input, boolean machineOutput) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!machineOutput || !handleMachineLine(line)) {
                    publishOutput(line);
                }
            }
        } catch (IOException ex) {
            if (process.isAlive() && !stopRequested.get()) {
                publishOutput("Unable to read DevTools process output: " + failureMessage(ex));
            }
        }
    }

    private boolean handleMachineLine(String line) {
        JsonNode message;
        try {
            message = JSON.readTree(line.strip());
        } catch (JsonProcessingException ex) {
            return false;
        }
        if (!message.isObject()) {
            return false;
        }
        String event = message.path("event").asText(message.path("method").asText(""));
        if (!"server.started".equals(event)) {
            return "server.dtdStarted".equals(event);
        }
        JsonNode params = message.path("params");
        String host = params.path("host").asText("").strip();
        int port = params.path("port").asInt(-1);
        if (host.isEmpty() || port < 1 || port > 65_535) {
            browserUri.completeExceptionally(new IOException(
                    "DevTools returned invalid server endpoint: " + line.strip()));
            return true;
        }
        try {
            URI base = new URI("http", null, host, port, "/", null, null);
            String encodedService = URLEncoder.encode(
                    vmServiceUri.toASCIIString(), StandardCharsets.UTF_8);
            browserUri.complete(URI.create(base.toASCIIString() + "?uri=" + encodedService));
        } catch (Exception ex) {
            browserUri.completeExceptionally(new IOException(
                    "Unable to construct the DevTools browser URL for " + host + ":" + port, ex));
        }
        return true;
    }

    private void awaitExit(Thread stdoutReader, Thread stderrReader) {
        boolean interrupted = false;
        int code;
        for (;;) {
            try {
                code = process.waitFor();
                break;
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        interrupted |= joinUninterruptibly(stdoutReader);
        interrupted |= joinUninterruptibly(stderrReader);
        if (!browserUri.isDone()) {
            browserUri.completeExceptionally(new IOException(
                    "DevTools process exited with code " + code
                    + " before the server became ready."));
        }
        exitCode.complete(code);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void publishOutput(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        synchronized (outputLock) {
            if (outputListeners.isEmpty()) {
                if (earlyOutput.size() == EARLY_OUTPUT_LIMIT) {
                    earlyOutput.removeFirst();
                }
                earlyOutput.add(line);
                return;
            }
            for (Consumer<String> listener : outputListeners) {
                notifyOutputListener(listener, line);
            }
        }
    }

    private static void notifyOutputListener(Consumer<String> listener, String line) {
        try {
            listener.accept(line);
        } catch (RuntimeException ignored) {
            // One output consumer cannot break the server stream.
        }
    }

    private static void destroyProcessTree(Process process, boolean forcibly) {
        try {
            List<ProcessHandle> descendants = new ArrayList<>(process.descendants().toList());
            for (int index = descendants.size() - 1; index >= 0; index--) {
                destroy(descendants.get(index), forcibly);
            }
            destroy(process.toHandle(), forcibly);
        } catch (UnsupportedOperationException ex) {
            if (process.isAlive()) {
                if (forcibly) {
                    process.destroyForcibly();
                } else {
                    process.destroy();
                }
            }
        }
    }

    private static void destroy(ProcessHandle handle, boolean forcibly) {
        if (!handle.isAlive()) {
            return;
        }
        if (forcibly) {
            handle.destroyForcibly();
        } else {
            handle.destroy();
        }
    }

    private static boolean joinUninterruptibly(Thread thread) {
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        return interrupted;
    }

    private static String failureMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }
}

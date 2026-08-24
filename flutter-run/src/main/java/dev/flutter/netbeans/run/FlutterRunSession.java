package dev.flutter.netbeans.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.flutter.netbeans.api.RunState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** A managed long-lived {@code flutter run --machine} process. */
public final class FlutterRunSession implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration STOP_GRACE_PERIOD = Duration.ofSeconds(5);
    private static final Duration FORCE_STOP_GRACE_PERIOD = Duration.ofSeconds(2);
    private static final AtomicLong SESSION_IDS = new AtomicLong();

    private final Process process;
    private final String deviceId;
    private final boolean debugSession;
    private final AtomicReference<RunState> state = new AtomicReference<>(RunState.STARTING);
    private final AtomicReference<String> appId = new AtomicReference<>();
    private final AtomicLong requestIds = new AtomicLong();
    private final AtomicBoolean quitRequested = new AtomicBoolean();
    private final CompletableFuture<URI> vmServiceUri = new CompletableFuture<>();
    private final CompletableFuture<Integer> exitCode = new CompletableFuture<>();
    private final Map<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<String>> outputListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<RunState>> stateListeners = new CopyOnWriteArrayList<>();
    private final Object stateLock = new Object();
    private final Object writeLock = new Object();

    FlutterRunSession(Process process) {
        this(process, "unknown", false);
    }

    FlutterRunSession(Process process, String deviceId) {
        this(process, deviceId, false);
    }

    FlutterRunSession(Process process, String deviceId, boolean debugSession) {
        this.process = Objects.requireNonNull(process);
        this.deviceId = deviceId == null || deviceId.isBlank() ? "unknown" : deviceId.strip();
        this.debugSession = debugSession;

        long sessionId = SESSION_IDS.incrementAndGet();
        Thread stdoutReader = Thread.ofVirtual()
                .name("flutter-run-stdout-" + sessionId)
                .start(() -> drainStdout(process.getInputStream()));
        Thread stderrReader = Thread.ofVirtual()
                .name("flutter-run-stderr-" + sessionId)
                .start(() -> drainPlainOutput(process.getErrorStream(), "stderr"));
        Thread.ofVirtual()
                .name("flutter-run-exit-" + sessionId)
                .start(() -> awaitExit(stdoutReader, stderrReader));
    }

    public RunState state() {
        return state.get();
    }

    public void addOutputListener(Consumer<String> listener) {
        outputListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeOutputListener(Consumer<String> listener) {
        outputListeners.remove(listener);
    }

    /** Adds a listener and immediately supplies the current state. */
    public void addStateListener(Consumer<RunState> listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (stateLock) {
            stateListeners.add(listener);
            notifyStateListener(listener, state.get());
        }
    }

    public void removeStateListener(Consumer<RunState> listener) {
        synchronized (stateLock) {
            stateListeners.remove(listener);
        }
    }

    public CompletableFuture<URI> vmServiceUri() {
        return vmServiceUri.copy();
    }

    /** Returns the VM Service URI only after Flutter has supplied it successfully. */
    public Optional<URI> currentVmServiceUri() {
        if (!vmServiceUri.isDone()
                || vmServiceUri.isCompletedExceptionally()
                || vmServiceUri.isCancelled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(vmServiceUri.getNow(null));
    }

    public CompletableFuture<Integer> exitCode() {
        return exitCode.copy();
    }

    public void hotReload() throws IOException {
        sendRestart("hot reload", false);
    }

    public void hotRestart() throws IOException {
        sendRestart("hot restart", true);
    }

    public void quit() throws IOException {
        RunState current = state.get();
        if (current == RunState.STOPPED || current == RunState.FAILED) {
            return;
        }
        if (!quitRequested.compareAndSet(false, true)) {
            return;
        }

        transition(RunState.STOPPING);
        String currentAppId = appId.get();
        IOException sendFailure = null;
        if (currentAppId != null && process.isAlive()) {
            ObjectNode params = JSON.createObjectNode().put("appId", currentAppId);
            try {
                CompletableFuture<JsonNode> response = sendRequest("app.stop", params);
                response.whenComplete((result, failure) -> {
                    if (failure != null) {
                        publishOutput("Unable to stop Flutter app on device '" + deviceId
                                + "': " + failureMessage(failure));
                        terminateProcessAsync();
                    } else if (!result.asBoolean(false)) {
                        publishOutput("Unable to stop Flutter app on device '" + deviceId
                                + "': Flutter rejected app.stop.");
                        terminateProcessAsync();
                    }
                });
            } catch (IOException ex) {
                sendFailure = ex;
                terminateProcessAsync();
            }
        } else {
            terminateProcessAsync();
        }
        scheduleStopFallback();
        if (sendFailure != null) {
            throw sendFailure;
        }
    }

    private void sendRestart(String operation, boolean fullRestart) throws IOException {
        String currentAppId = requireRunningApp(operation);
        ObjectNode params = JSON.createObjectNode();
        params.put("appId", currentAppId);
        params.put("fullRestart", fullRestart);
        params.put("pause", debugSession);
        params.put("reason", "manual");
        CompletableFuture<JsonNode> response = sendRequest("app.restart", params);
        response.whenComplete((result, failure) -> {
            if (failure != null) {
                publishOutput("Flutter " + operation + " failed for device '" + deviceId
                        + "': " + failureMessage(failure));
                return;
            }
            int code = result.path("code").asInt(0);
            if (code != 0) {
                String message = result.path("message").asText("Flutter returned code " + code);
                publishOutput("Flutter " + operation + " failed for device '" + deviceId
                        + "': " + message);
            }
        });
    }

    private String requireRunningApp(String operation) throws IOException {
        RunState current = state.get();
        if (current != RunState.RUNNING) {
            throw new IOException("Unable to " + operation + " Flutter app on device '" + deviceId
                    + "': session state is " + current + ".");
        }
        String currentAppId = appId.get();
        if (currentAppId == null || currentAppId.isBlank()) {
            throw new IOException("Unable to " + operation + " Flutter app on device '" + deviceId
                    + "': the app.start event has not provided an app id.");
        }
        if (!process.isAlive()) {
            throw new IOException("Unable to " + operation + " Flutter app on device '" + deviceId
                    + "': the Flutter process has exited.");
        }
        return currentAppId;
    }

    private CompletableFuture<JsonNode> sendRequest(String method, ObjectNode params)
            throws IOException {
        String id = Long.toString(requestIds.incrementAndGet());
        ObjectNode request = JSON.createObjectNode();
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);
        ArrayNode envelope = JSON.createArrayNode().add(request);

        byte[] encoded;
        try {
            encoded = (JSON.writeValueAsString(envelope) + "\n").getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException ex) {
            throw new IOException("Unable to encode Flutter " + method + " request for device '"
                    + deviceId + "'.", ex);
        }

        CompletableFuture<JsonNode> response = new CompletableFuture<>();
        pendingRequests.put(id, response);
        try {
            synchronized (writeLock) {
                OutputStream output = process.getOutputStream();
                output.write(encoded);
                output.flush();
            }
        } catch (IOException ex) {
            pendingRequests.remove(id);
            response.completeExceptionally(ex);
            throw new IOException("Unable to send Flutter " + method + " request to app '"
                    + Objects.toString(appId.get(), "unknown") + "' on device '" + deviceId + "'.", ex);
        }
        return response;
    }

    private void drainStdout(InputStream input) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!handleMachineLine(line)) {
                    publishOutput(line);
                }
            }
        } catch (IOException ex) {
            reportReadFailure("stdout", ex);
        }
    }

    private void drainPlainOutput(InputStream input, String streamName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                publishOutput(line);
            }
        } catch (IOException ex) {
            reportReadFailure(streamName, ex);
        }
    }

    private boolean handleMachineLine(String line) {
        String candidate = line.strip();
        if (!candidate.startsWith("[") || !candidate.endsWith("]")) {
            return false;
        }

        final JsonNode envelope;
        try {
            envelope = JSON.readTree(candidate);
        } catch (JsonProcessingException ex) {
            return false;
        }
        if (!envelope.isArray()) {
            return false;
        }

        boolean recognized = false;
        for (JsonNode message : envelope) {
            if (!message.isObject()) {
                continue;
            }
            if (message.hasNonNull("event")) {
                handleEvent(message.path("event").asText(), message.path("params"));
                recognized = true;
            } else if (message.hasNonNull("id")) {
                handleResponse(message);
                recognized = true;
            }
        }
        return recognized;
    }

    private void handleEvent(String event, JsonNode params) {
        switch (event) {
            case "app.start" -> {
                String value = params.path("appId").asText("").strip();
                if (!value.isEmpty()) {
                    appId.compareAndSet(null, value);
                }
            }
            case "app.debugPort" -> completeVmServiceUri(params.path("wsUri").asText(""));
            case "app.started" -> transition(RunState.RUNNING);
            case "app.stop" -> transition(RunState.STOPPING);
            case "app.log", "daemon.log" -> publishIfPresent(params, "log");
            case "daemon.logMessage" -> {
                publishIfPresent(params, "message");
                publishIfPresent(params, "stackTrace");
            }
            case "daemon.showMessage" -> publishIfPresent(params, "message");
            case "app.warning" -> publishIfPresent(params, "warning");
            case "app.progress" -> {
                if (!params.path("finished").asBoolean(false)) {
                    publishIfPresent(params, "message");
                }
            }
            default -> {
                // Unknown events are intentionally ignored for forward compatibility.
            }
        }
    }

    private void handleResponse(JsonNode message) {
        String id = message.path("id").asText();
        CompletableFuture<JsonNode> pending = pendingRequests.remove(id);
        if (pending == null) {
            return;
        }
        JsonNode error = message.get("error");
        if (error != null && !error.isNull()) {
            String detail = error.isTextual()
                    ? error.asText()
                    : error.path("message").asText(error.toString());
            pending.completeExceptionally(new IOException(
                    "Flutter machine request " + id + " failed: " + detail));
            return;
        }
        JsonNode result = message.get("result");
        pending.complete(result == null ? JSON.nullNode() : result);
    }

    private void completeVmServiceUri(String value) {
        if (value == null || value.isBlank() || vmServiceUri.isDone()) {
            return;
        }
        try {
            vmServiceUri.complete(URI.create(value.strip()));
        } catch (IllegalArgumentException ex) {
            publishOutput("Flutter returned an invalid VM service URI for device '" + deviceId
                    + "': " + value);
        }
    }

    private void publishIfPresent(JsonNode params, String field) {
        JsonNode value = params.get(field);
        if (value != null && value.isTextual() && !value.asText().isEmpty()) {
            publishOutput(value.asText());
        }
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

        IOException exited = new IOException("Flutter process for device '" + deviceId
                + "' exited with code " + code + ".");
        pendingRequests.values().forEach(pending -> pending.completeExceptionally(exited));
        pendingRequests.clear();
        if (!vmServiceUri.isDone()) {
            vmServiceUri.completeExceptionally(exited);
        }

        if (quitRequested.get() || code == 0 || state.get() == RunState.STOPPING) {
            transition(RunState.STOPPED);
        } else {
            publishOutput(exited.getMessage());
            transition(RunState.FAILED);
        }
        exitCode.complete(code);
        if (interrupted) {
            Thread.currentThread().interrupt();
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

    private void reportReadFailure(String streamName, IOException failure) {
        if (process.isAlive() && !quitRequested.get()) {
            publishOutput("Unable to read Flutter " + streamName + " for device '" + deviceId
                    + "': " + failure.getMessage());
        }
    }

    private void transition(RunState next) {
        synchronized (stateLock) {
            RunState current = state.get();
            if (current == next || current == RunState.STOPPED || current == RunState.FAILED) {
                return;
            }
            if (current == RunState.STOPPING && next == RunState.RUNNING) {
                return;
            }
            state.set(next);
            for (Consumer<RunState> listener : stateListeners) {
                notifyStateListener(listener, next);
            }
        }
    }

    private static void notifyStateListener(Consumer<RunState> listener, RunState value) {
        try {
            listener.accept(value);
        } catch (RuntimeException ignored) {
            // One UI listener must not break session lifecycle processing.
        }
    }

    private void publishOutput(String text) {
        if (text == null) {
            return;
        }
        for (Consumer<String> listener : outputListeners) {
            try {
                listener.accept(text);
            } catch (RuntimeException ignored) {
                // One output sink must not stop process draining.
            }
        }
    }

    private void scheduleStopFallback() {
        Thread.ofVirtual().name("flutter-run-stop-timeout").start(() -> {
            try {
                Thread.sleep(STOP_GRACE_PERIOD.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            if (process.isAlive()) {
                publishOutput("Flutter app.stop timed out for device '" + deviceId
                        + "'; terminating the Flutter process.");
                terminateProcessAsync();
            }
        });
    }

    private void terminateProcessAsync() {
        if (!process.isAlive()) {
            return;
        }
        process.destroy();
        Thread.ofVirtual().name("flutter-run-force-stop").start(() -> {
            try {
                if (!process.waitFor(FORCE_STOP_GRACE_PERIOD.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        });
    }

    private static String failureMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @Override
    public void close() {
        try {
            quit();
        } catch (IOException ex) {
            publishOutput(ex.getMessage());
            terminateProcessAsync();
        }
    }
}

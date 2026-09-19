package io.github.vgrytsenko2022.dart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One bounded newline-JSON connection to Dart's native analyzer protocol. */
final class DartAnalyzerProtocolSession implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(
            DartAnalyzerProtocolSession.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLIENT_ID = "netbeans-flutter-designer";

    private final DartCandidateAnalysisLimits limits;
    private final Consumer<String> stderrConsumer;
    private final Process process;
    private final InputStream input;
    private final OutputStream output;
    private final InputStream stderr;
    private final Thread stderrThread;
    private final AtomicLong nextId = new AtomicLong();
    private final AtomicReference<String> outstandingRequest = new AtomicReference<>();
    private volatile boolean closed;

    static DartAnalyzerProtocolSession start(
            Path dartExecutable,
            Path projectRoot,
            Consumer<String> stderrConsumer,
            DartCandidateAnalysisLimits limits) throws IOException {
        return new DartAnalyzerProtocolSession(
                dartExecutable,
                projectRoot,
                stderrConsumer,
                limits,
                DartAnalyzerProtocolSession::startProcess);
    }

    DartAnalyzerProtocolSession(
            Path dartExecutable,
            Path projectRoot,
            Consumer<String> stderrConsumer,
            DartCandidateAnalysisLimits limits,
            ProcessFactory processFactory) throws IOException {
        Objects.requireNonNull(dartExecutable, "dartExecutable");
        Objects.requireNonNull(projectRoot, "projectRoot");
        this.stderrConsumer = Objects.requireNonNull(stderrConsumer, "stderrConsumer");
        this.limits = Objects.requireNonNull(limits, "limits");
        List<String> command = List.of(
                dartExecutable.toAbsolutePath().normalize().toString(),
                "language-server",
                "--protocol=analyzer",
                "--client-id=" + CLIENT_ID,
                "--client-version=" + DartAnalysisServer.CLIENT_VERSION);
        process = Objects.requireNonNull(processFactory, "processFactory")
                .start(command, projectRoot.toAbsolutePath().normalize());
        input = process.getInputStream();
        output = process.getOutputStream();
        stderr = process.getErrorStream();
        stderrThread = Thread.ofVirtual()
                .name("dart-candidate-analyzer-stderr")
                .start(this::drainStderr);
    }

    String awaitConnected() throws ProtocolFailure {
        while (true) {
            JsonNode message = readMessage();
            if (message.has("id")) {
                throw failure(
                        DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                        "Analyzer responded before server.connected.");
            }
            if ("server.connected".equals(message.path("event").asText())) {
                String version = message.path("params").path("version").asText();
                if (version.isBlank()) {
                    throw failure(
                            DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                            "server.connected did not contain a protocol version.");
                }
                return version;
            }
        }
    }

    Response request(String method, JsonNode params) throws ProtocolFailure {
        String id = Long.toString(nextId.incrementAndGet());
        ObjectNode request = JSON.createObjectNode()
                .put("id", id)
                .put("method", requireText(method, "method"));
        if (params != null) {
            request.set("params", params);
        }
        outstandingRequest.set(id);
        try {
            writeMessage(request);
            while (true) {
                JsonNode message = readMessage();
                if (message.has("method") && message.has("id")) {
                    throw failure(
                            DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                            "Analyzer sent an unsupported server-to-client request.");
                }
                if (id.equals(message.path("id").asText(null))) {
                    JsonNode error = message.get("error");
                    JsonNode result = message.get("result");
                    return new Response(result, error);
                }
            }
        } finally {
            outstandingRequest.compareAndSet(id, null);
        }
    }

    void cancelOutstanding() {
        String target = outstandingRequest.get();
        if (target == null || closed) {
            return;
        }
        String cancelId = "cancel-" + nextId.incrementAndGet();
        ObjectNode request = JSON.createObjectNode()
                .put("id", cancelId)
                .put("method", "server.cancelRequest")
                .set("params", JSON.createObjectNode().put("id", target));
        try {
            writeMessage(request);
        } catch (ProtocolFailure ex) {
            LOGGER.log(Level.FINE, "Could not send analyzer cancellation", ex);
        }
    }

    boolean isAlive() {
        return !closed && process.isAlive();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(output);
        terminate(process, limits.closeTimeout());
        closeQuietly(input);
        closeQuietly(stderr);
        stderrThread.interrupt();
        try {
            stderrThread.join(Math.min(limits.closeTimeout().toMillis(), 250L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode readMessage() throws ProtocolFailure {
        byte[] bytes;
        try {
            bytes = readBoundedLine(input, limits.maxJsonLineBytes());
        } catch (LineLimitException ex) {
            throw failure(DartCandidateAnalysisIssueCode.RESPONSE_LIMIT, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw failure(
                    DartCandidateAnalysisIssueCode.PROCESS_FAILED,
                    "Analyzer protocol stream closed before a complete response.", ex);
        }
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw failure(
                    DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                    "Analyzer response is not strict UTF-8.", ex);
        }
        try {
            JsonNode node = JSON.readTree(json);
            if (node == null || !node.isObject()) {
                throw failure(
                        DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                        "Analyzer response is not a JSON object.");
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw failure(
                    DartCandidateAnalysisIssueCode.MALFORMED_RESPONSE,
                    "Analyzer returned malformed JSON.", ex);
        }
    }

    private synchronized void writeMessage(JsonNode message) throws ProtocolFailure {
        if (closed || !process.isAlive()) {
            throw failure(
                    DartCandidateAnalysisIssueCode.PROCESS_FAILED,
                    "Analyzer process is not running.");
        }
        byte[] bytes;
        try {
            bytes = JSON.writeValueAsBytes(message);
        } catch (JsonProcessingException ex) {
            throw failure(
                    DartCandidateAnalysisIssueCode.INTERNAL_FAILURE,
                    "Could not encode an analyzer request.", ex);
        }
        if (bytes.length > limits.maxJsonLineBytes()) {
            throw failure(
                    DartCandidateAnalysisIssueCode.RESPONSE_LIMIT,
                    "Analyzer request exceeds the JSON line limit.");
        }
        try {
            output.write(bytes);
            output.write('\n');
            output.flush();
        } catch (IOException ex) {
            throw failure(
                    DartCandidateAnalysisIssueCode.PROCESS_FAILED,
                    "Could not write an analyzer request.", ex);
        }
    }

    private void drainStderr() {
        try {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int next;
            while ((next = stderr.read()) >= 0) {
                if (next == '\n') {
                    publishStderr(line);
                    line.reset();
                } else if (next != '\r' && line.size() < 64 * 1024) {
                    line.write(next);
                }
            }
            if (line.size() > 0) {
                publishStderr(line);
            }
        } catch (IOException ex) {
            if (!closed) {
                LOGGER.log(Level.FINE, "Could not drain candidate analyzer stderr", ex);
            }
        }
    }

    private void publishStderr(ByteArrayOutputStream line) {
        try {
            stderrConsumer.accept(line.toString(StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Candidate analyzer stderr consumer failed", ex);
        }
    }

    private static byte[] readBoundedLine(InputStream source, int maximum)
            throws IOException, LineLimitException {
        ByteArrayOutputStream line = new ByteArrayOutputStream(Math.min(8_192, maximum));
        while (true) {
            int next = source.read();
            if (next < 0) {
                if (line.size() == 0) {
                    throw new IOException("end of stream");
                }
                throw new IOException("unterminated JSON line");
            }
            if (next == '\n') {
                byte[] bytes = line.toByteArray();
                if (bytes.length > 0 && bytes[bytes.length - 1] == '\r') {
                    return java.util.Arrays.copyOf(bytes, bytes.length - 1);
                }
                return bytes;
            }
            if (line.size() >= maximum) {
                throw new LineLimitException(
                        "Analyzer response exceeds the " + maximum + " byte JSON line limit.");
            }
            line.write(next);
        }
    }

    private static Process startProcess(List<String> command, Path workingDirectory)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(false);
        return builder.start();
    }

    private static void terminate(Process target, Duration timeout) {
        if (!target.isAlive()) {
            return;
        }
        target.destroy();
        try {
            if (target.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        target.destroyForcibly();
    }

    private static void closeQuietly(Closeable value) {
        try {
            value.close();
        } catch (IOException ex) {
            LOGGER.log(Level.FINEST, "Could not close analyzer stream", ex);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static ProtocolFailure failure(
            DartCandidateAnalysisIssueCode code,
            String message) {
        return new ProtocolFailure(code, message, null);
    }

    private static ProtocolFailure failure(
            DartCandidateAnalysisIssueCode code,
            String message,
            Throwable cause) {
        return new ProtocolFailure(code, message, cause);
    }

    record Response(JsonNode result, JsonNode error) {
        boolean failed() {
            return error != null && !error.isNull();
        }

        String errorCode() {
            return failed() ? error.path("code").asText("") : "";
        }

        String errorMessage() {
            return failed() ? error.path("message").asText("") : "";
        }
    }

    static final class ProtocolFailure extends Exception {
        private final DartCandidateAnalysisIssueCode code;

        ProtocolFailure(
                DartCandidateAnalysisIssueCode code,
                String message,
                Throwable cause) {
            super(message, cause);
            this.code = Objects.requireNonNull(code, "code");
        }

        DartCandidateAnalysisIssueCode code() {
            return code;
        }
    }

    @FunctionalInterface
    interface ProcessFactory {
        Process start(List<String> command, Path workingDirectory) throws IOException;
    }

    private static final class LineLimitException extends Exception {
        LineLimitException(String message) {
            super(message);
        }
    }
}

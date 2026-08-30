package dev.flutter.netbeans.plugin.designer.canvas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict authenticated byte-stream adapter for the Flutter Web Canvas bridge.
 *
 * <p>The native WebView host supplies source-bound JSON messages through
 * {@link #acceptWebMessage(String, String)} and posts the JSON produced by the
 * supplied {@link MessageSink}. The exposed streams have process semantics and
 * can therefore be passed directly to {@link CanvasRunnerProcessChannel}'s
 * package-private stream constructor.</p>
 *
 * <p>This class deliberately treats the browser JSON layer as an untrusted
 * transport. Only the exact frozen envelope installed by {@code
 * web/canvas_bridge.js} is admitted. An authenticated contract violation is
 * fail-stop and produces at most one terminal callback.</p>
 */
final class WebCanvasHostBridge implements AutoCloseable {
    static final String FORMAT = "netbeans-flutter-canvas-web-bridge";
    static final int VERSION = 1;
    static final int MAXIMUM_CHUNK_BYTES = 1024 * 1024;
    static final int MAXIMUM_ENCODED_CHUNK_CHARACTERS = 1_398_104;
    static final int MAXIMUM_DIAGNOSTIC_CHARACTERS = 2_048;
    static final int MAXIMUM_PENDING_CHUNKS = 64;
    static final int MAXIMUM_PENDING_BYTES = 16 * 1024 * 1024;

    private static final long MAXIMUM_SAFE_JAVASCRIPT_INTEGER =
            9_007_199_254_740_991L;
    private static final int MAXIMUM_JSON_CHARACTERS = 1_400_512;
    private static final Pattern NONCE = Pattern.compile("^[0-9a-f]{64}$");
    private static final Set<String> BASE_FIELDS = Set.of(
            "format", "version", "sessionNonce", "direction", "kind",
            "sequence");
    private static final Set<String> CHUNK_FIELDS = with(BASE_FIELDS, "chunk");
    private static final Set<String> MESSAGE_FIELDS =
            with(BASE_FIELDS, "message");

    private static final ObjectMapper JSON = new ObjectMapper(
            JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxDocumentLength(MAXIMUM_JSON_CHARACTERS)
                            .maxNestingDepth(4)
                            .maxTokenCount(24)
                            .maxNameLength(64)
                            .maxStringLength(
                                    MAXIMUM_ENCODED_CHUNK_CHARACTERS)
                            .maxNumberLength(20)
                            .build())
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                    .build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final String exactSourceUri;
    private final String sessionNonce;
    private final MessageSink messageSink;
    private final Listener listener;
    private final BoundedRunnerInput runnerStdout;
    private final HostOutput runnerStdin = new HostOutput();

    private boolean ready;
    private long lastRunnerSequence;
    private long lastHostSequence;
    private Terminal terminal;

    WebCanvasHostBridge(
            String exactSourceUri,
            String sessionNonce,
            MessageSink messageSink,
            Listener listener) {
        this(
                exactSourceUri,
                sessionNonce,
                messageSink,
                listener,
                MAXIMUM_PENDING_CHUNKS,
                MAXIMUM_PENDING_BYTES);
    }

    WebCanvasHostBridge(
            String exactSourceUri,
            String sessionNonce,
            MessageSink messageSink,
            Listener listener,
            int maximumPendingChunks,
            int maximumPendingBytes) {
        this.exactSourceUri = requireExactSourceUri(exactSourceUri);
        if (sessionNonce == null || !NONCE.matcher(sessionNonce).matches()) {
            throw new IllegalArgumentException(
                    "Web Canvas session nonce must be 64 lowercase hexadecimal characters.");
        }
        if (maximumPendingChunks < 1
                || maximumPendingChunks > MAXIMUM_PENDING_CHUNKS) {
            throw new IllegalArgumentException(
                    "Web Canvas pending chunk bound is outside the admitted range.");
        }
        if (maximumPendingBytes < 1
                || maximumPendingBytes > MAXIMUM_PENDING_BYTES) {
            throw new IllegalArgumentException(
                    "Web Canvas pending byte bound is outside the admitted range.");
        }
        this.sessionNonce = sessionNonce;
        this.messageSink = Objects.requireNonNull(messageSink, "messageSink");
        this.listener = Objects.requireNonNull(listener, "listener");
        runnerStdout = new BoundedRunnerInput(
                maximumPendingChunks, maximumPendingBytes);
    }

    InputStream runnerStdout() {
        return runnerStdout;
    }

    OutputStream runnerStdin() {
        return runnerStdin;
    }

    synchronized boolean isReady() {
        return ready && terminal == null;
    }

    synchronized Optional<Terminal> terminal() {
        return Optional.ofNullable(terminal);
    }

    /**
     * Accepts one JSON message already attributed to a WebView document.
     * Messages from every source except the exact configured page are ignored
     * without parsing. Well-formed foreign-session messages are also ignored.
     */
    synchronized void acceptWebMessage(String sourceUri, String json) {
        if (terminal != null || !exactSourceUri.equals(sourceUri)) {
            return;
        }
        final JsonNode parsed;
        try {
            if (json == null || json.length() > MAXIMUM_JSON_CHARACTERS) {
                throw new IOException("Web Canvas JSON exceeds its document bound.");
            }
            parsed = JSON.readTree(json);
        } catch (IOException | RuntimeException failure) {
            protocolViolation();
            return;
        }
        if (parsed == null || !parsed.isObject()) {
            protocolViolation();
            return;
        }

        JsonNode nonce = parsed.get("sessionNonce");
        if (nonce != null && nonce.isTextual()
                && !sessionNonce.equals(nonce.textValue())) {
            return;
        }
        if (!validEnvelope(parsed)) {
            protocolViolation();
            return;
        }

        String kind = parsed.get("kind").textValue();
        switch (kind) {
            case "ready" -> acceptReady(parsed);
            case "chunk" -> acceptChunk(parsed);
            case "diagnostic" -> acceptDiagnostic(parsed);
            case "failure" -> acceptFailure(parsed);
            case "closed" -> acceptClosed(parsed);
            default -> protocolViolation();
        }
    }

    /** Reports a terminal native-host failure through the same one-shot gate. */
    synchronized void failTransport(String message) {
        String bounded = boundedHostMessage(message);
        terminate(TerminalKind.TRANSPORT_FAILURE, bounded, true);
    }

    @Override
    public synchronized void close() {
        terminate(
                TerminalKind.LOCAL_CLOSE,
                "The Web Canvas host bridge was closed.",
                true);
    }

    private void acceptReady(JsonNode message) {
        if (!hasExactFields(message, BASE_FIELDS)
                || ready
                || requireSafeSequence(message) != 0) {
            protocolViolation();
            return;
        }
        ready = true;
        notifyReady();
    }

    private void acceptChunk(JsonNode message) {
        long sequence = requireSafeSequence(message);
        JsonNode chunk = message.get("chunk");
        if (!hasExactFields(message, CHUNK_FIELDS)
                || !ready
                || sequence != lastRunnerSequence + 1
                || chunk == null
                || !chunk.isTextual()) {
            protocolViolation();
            return;
        }
        byte[] decoded = decodeCanonicalChunk(chunk.textValue());
        if (decoded == null) {
            protocolViolation();
            return;
        }
        if (!runnerStdout.offer(decoded)) {
            terminate(
                    TerminalKind.PROTOCOL_VIOLATION,
                    "Authenticated runner bytes exceeded the pending Web Canvas bound.",
                    true);
            return;
        }
        lastRunnerSequence = sequence;
    }

    private void acceptDiagnostic(JsonNode message) {
        JsonNode text = message.get("message");
        if (!hasExactFields(message, MESSAGE_FIELDS)
                || !ready
                || requireSafeSequence(message) != 0
                || text == null
                || !text.isTextual()
                || text.textValue().length()
                        > MAXIMUM_DIAGNOSTIC_CHARACTERS) {
            protocolViolation();
            return;
        }
        notifyDiagnostic(text.textValue());
    }

    private void acceptFailure(JsonNode message) {
        JsonNode text = message.get("message");
        long sequence = requireSafeSequence(message);
        if (!hasExactFields(message, MESSAGE_FIELDS)
                // WebView delivery is asynchronous: later host chunks may
                // already have been posted when this terminal acknowledgement
                // for an earlier delivered chunk returns.
                || sequence < 0
                || sequence > lastHostSequence
                || text == null
                || !text.isTextual()
                || text.textValue().length()
                        > MAXIMUM_DIAGNOSTIC_CHARACTERS) {
            protocolViolation();
            return;
        }
        terminate(
                TerminalKind.RUNNER_FAILURE,
                text.textValue(),
                false,
                sequence);
    }

    private void acceptClosed(JsonNode message) {
        long sequence = requireSafeSequence(message);
        if (!hasExactFields(message, BASE_FIELDS)
                || !ready
                || sequence != lastRunnerSequence) {
            protocolViolation();
            return;
        }
        terminate(
                TerminalKind.RUNNER_CLOSED,
                "The authenticated Web Canvas runner closed.",
                false,
                sequence);
    }

    private boolean validEnvelope(JsonNode message) {
        JsonNode format = message.get("format");
        JsonNode version = message.get("version");
        JsonNode nonce = message.get("sessionNonce");
        JsonNode direction = message.get("direction");
        JsonNode kind = message.get("kind");
        return format != null
                && format.isTextual()
                && FORMAT.equals(format.textValue())
                && version != null
                && version.isIntegralNumber()
                && version.canConvertToInt()
                && version.intValue() == VERSION
                && nonce != null
                && nonce.isTextual()
                && sessionNonce.equals(nonce.textValue())
                && direction != null
                && direction.isTextual()
                && "runner-to-host".equals(direction.textValue())
                && kind != null
                && kind.isTextual()
                && requireSafeSequence(message) >= 0;
    }

    private static boolean hasExactFields(JsonNode message, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        message.fieldNames().forEachRemaining(actual::add);
        return actual.equals(expected);
    }

    private static long requireSafeSequence(JsonNode message) {
        JsonNode sequence = message.get("sequence");
        if (sequence == null
                || !sequence.isIntegralNumber()
                || !sequence.canConvertToLong()) {
            return -1;
        }
        long value = sequence.longValue();
        return value >= 0 && value <= MAXIMUM_SAFE_JAVASCRIPT_INTEGER
                ? value : -1;
    }

    private static byte[] decodeCanonicalChunk(String encoded) {
        if (encoded == null
                || encoded.length() < 4
                || encoded.length() > MAXIMUM_ENCODED_CHUNK_CHARACTERS
                || encoded.length() % 4 != 0) {
            return null;
        }
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException failure) {
            return null;
        }
        if (decoded.length < 1 || decoded.length > MAXIMUM_CHUNK_BYTES) {
            return null;
        }
        return Base64.getEncoder().encodeToString(decoded).equals(encoded)
                ? decoded : null;
    }

    private void protocolViolation() {
        terminate(
                TerminalKind.PROTOCOL_VIOLATION,
                "Authenticated runner message violated the Web Canvas bridge contract.",
                true);
    }

    private void terminate(
            TerminalKind kind, String message, boolean discardPendingBytes) {
        terminate(kind, message, discardPendingBytes,
                Math.max(lastRunnerSequence, lastHostSequence));
    }

    private void terminate(
            TerminalKind kind,
            String message,
            boolean discardPendingBytes,
            long sequence) {
        if (terminal != null) {
            return;
        }
        terminal = new Terminal(kind, message, sequence);
        runnerStdout.finish(discardPendingBytes);
        runnerStdin.terminate();
        try {
            listener.terminal(terminal);
        } catch (RuntimeException ignored) {
            // Integration callbacks cannot escape or reopen the terminal gate.
        }
    }

    private void notifyReady() {
        try {
            listener.ready();
        } catch (RuntimeException failure) {
            terminate(
                    TerminalKind.TRANSPORT_FAILURE,
                    "The Web Canvas ready integration callback failed.",
                    true);
        }
    }

    private void notifyDiagnostic(String message) {
        try {
            listener.diagnostic(message);
        } catch (RuntimeException ignored) {
            // UI notification code cannot poison the authenticated transport.
        }
    }

    private void postHostChunk(byte[] bytes) throws IOException {
        if (terminal != null) {
            throw new IOException("Web Canvas bridge is terminal.");
        }
        if (lastHostSequence == MAXIMUM_SAFE_JAVASCRIPT_INTEGER) {
            terminate(
                    TerminalKind.TRANSPORT_FAILURE,
                    "Web Canvas host sequence was exhausted.",
                    true);
            throw new IOException("Web Canvas host sequence was exhausted.");
        }
        long sequence = lastHostSequence + 1;
        ObjectNode message = JSON.createObjectNode();
        message.put("format", FORMAT);
        message.put("version", VERSION);
        message.put("sessionNonce", sessionNonce);
        message.put("direction", "host-to-runner");
        message.put("kind", "chunk");
        message.put("sequence", sequence);
        message.put("chunk", Base64.getEncoder().encodeToString(bytes));
        final String encoded;
        try {
            encoded = JSON.writeValueAsString(message);
            // Advance before crossing the native boundary so a synchronous
            // callback from a test double or future adapter observes the same
            // sequence that the browser is about to receive.
            lastHostSequence = sequence;
            messageSink.postJsonMessage(encoded);
        } catch (IOException | RuntimeException failure) {
            terminate(
                    TerminalKind.TRANSPORT_FAILURE,
                    "The native Web Canvas host rejected an outbound message.",
                    true);
            IOException wrapped = new IOException(
                    "Could not post an authenticated Web Canvas message.");
            wrapped.initCause(failure);
            throw wrapped;
        }
    }

    private static String boundedHostMessage(String message) {
        String value = Objects.requireNonNullElse(
                message, "The native Web Canvas host failed.");
        return value.length() <= MAXIMUM_DIAGNOSTIC_CHARACTERS
                ? value : value.substring(0, MAXIMUM_DIAGNOSTIC_CHARACTERS);
    }

    private static String requireExactSourceUri(String source) {
        Objects.requireNonNull(source, "exactSourceUri");
        final URI parsed;
        try {
            parsed = URI.create(source);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "Web Canvas source URI is invalid.", failure);
        }
        if (!parsed.isAbsolute()
                || !"https".equals(parsed.getScheme())
                || parsed.getHost() == null
                || parsed.getRawUserInfo() != null
                || parsed.getRawQuery() != null
                || parsed.getRawFragment() != null
                || parsed.getRawPath() == null
                || parsed.getRawPath().isEmpty()) {
            throw new IllegalArgumentException(
                    "Web Canvas source must be one exact HTTPS document URI.");
        }
        return source;
    }

    private static Set<String> with(Set<String> fields, String additional) {
        Set<String> combined = new HashSet<>(fields);
        combined.add(additional);
        return Set.copyOf(combined);
    }

    /**
     * Synchronous, nonblocking native edge for WebView2
     * {@code PostWebMessageAsJson}; posting as a string is never permitted.
     * Implementations must not call back into this bridge before returning.
     */
    interface MessageSink {
        void postJsonMessage(String json) throws IOException;
    }

    /**
     * Synchronous notification-only callbacks. Implementations must be
     * nonblocking and must not call back into this bridge. A failing ready
     * callback terminates the transport because startup can no longer be
     * proven; diagnostic and terminal notification failures are isolated.
     */
    interface Listener {
        default void ready() {
        }

        default void diagnostic(String message) {
        }

        void terminal(Terminal terminal);
    }

    enum TerminalKind {
        RUNNER_FAILURE,
        RUNNER_CLOSED,
        PROTOCOL_VIOLATION,
        TRANSPORT_FAILURE,
        LOCAL_CLOSE
    }

    record Terminal(TerminalKind kind, String message, long sequence) {
        Terminal {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(message, "message");
        }
    }

    private final class HostOutput extends OutputStream {
        private final byte[] buffer = new byte[MAXIMUM_CHUNK_BYTES];
        private int size;
        private boolean closed;

        @Override
        public void write(int value) throws IOException {
            synchronized (WebCanvasHostBridge.this) {
                requireOpen();
                buffer[size++] = (byte) value;
                if (size == buffer.length) {
                    emit();
                }
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length)
                throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            synchronized (WebCanvasHostBridge.this) {
                requireOpen();
                int cursor = offset;
                int remaining = length;
                while (remaining > 0) {
                    int copied = Math.min(remaining, buffer.length - size);
                    System.arraycopy(bytes, cursor, buffer, size, copied);
                    cursor += copied;
                    remaining -= copied;
                    size += copied;
                    if (size == buffer.length) {
                        emit();
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (WebCanvasHostBridge.this) {
                requireOpen();
                if (size > 0) {
                    emit();
                }
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (WebCanvasHostBridge.this) {
                if (closed) {
                    return;
                }
                if (terminal == null && size > 0) {
                    emit();
                }
                size = 0;
                closed = true;
            }
        }

        void terminate() {
            size = 0;
            closed = true;
        }

        private void emit() throws IOException {
            byte[] chunk = new byte[size];
            System.arraycopy(buffer, 0, chunk, 0, size);
            postHostChunk(chunk);
            size = 0;
        }

        private void requireOpen() throws IOException {
            if (closed || terminal != null) {
                throw new IOException("Web Canvas runner input is closed.");
            }
        }
    }

    private static final class BoundedRunnerInput extends InputStream {
        private final int maximumChunks;
        private final int maximumBytes;
        private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
        private byte[] current;
        private int currentOffset;
        private int pendingBytes;
        private boolean finished;
        private boolean closed;

        BoundedRunnerInput(int maximumChunks, int maximumBytes) {
            this.maximumChunks = maximumChunks;
            this.maximumBytes = maximumBytes;
        }

        synchronized boolean offer(byte[] bytes) {
            if (finished || closed
                    || chunks.size() + (current == null ? 0 : 1)
                            >= maximumChunks
                    || bytes.length > maximumBytes - pendingBytes) {
                return false;
            }
            chunks.addLast(bytes);
            pendingBytes += bytes.length;
            notifyAll();
            return true;
        }

        synchronized void finish(boolean discardPendingBytes) {
            if (discardPendingBytes) {
                chunks.clear();
                current = null;
                currentOffset = 0;
                pendingBytes = 0;
            }
            finished = true;
            notifyAll();
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int count = read(one, 0, 1);
            return count < 0 ? -1 : Byte.toUnsignedInt(one[0]);
        }

        @Override
        public synchronized int read(byte[] bytes, int offset, int length)
                throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) {
                return 0;
            }
            while (!closed && current == null && chunks.isEmpty() && !finished) {
                try {
                    wait();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException failure = new InterruptedIOException(
                            "Interrupted while awaiting Web Canvas runner bytes.");
                    failure.initCause(interrupted);
                    throw failure;
                }
            }
            if (closed) {
                throw new IOException("Web Canvas runner output is closed.");
            }
            if (current == null) {
                current = chunks.pollFirst();
                currentOffset = 0;
            }
            if (current == null) {
                return -1;
            }
            int copied = Math.min(length, current.length - currentOffset);
            System.arraycopy(current, currentOffset, bytes, offset, copied);
            currentOffset += copied;
            pendingBytes -= copied;
            if (currentOffset == current.length) {
                current = null;
                currentOffset = 0;
            }
            return copied;
        }

        @Override
        public synchronized int available() {
            return pendingBytes;
        }

        @Override
        public synchronized void close() {
            closed = true;
            chunks.clear();
            current = null;
            currentOffset = 0;
            pendingBytes = 0;
            notifyAll();
        }
    }
}

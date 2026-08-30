package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebCanvasHostBridgeTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NONCE =
            "0123456789abcdef0123456789abcdef"
            + "0123456789abcdef0123456789abcdef";
    private static final String FOREIGN_NONCE = "f".repeat(64);
    private static final String SOURCE =
            "https://nbfc-0123456789abcdef.canvas.invalid/index.html";

    @Test
    void exactSessionRoundTripPreservesRunnerBytesAndLifecycle() throws Exception {
        Fixture fixture = fixture();

        fixture.accept(ready());
        fixture.accept(chunk(1, "first".getBytes(StandardCharsets.UTF_8)));
        fixture.accept(diagnostic("runner diagnostic"));
        fixture.accept(chunk(2, new byte[]{0, 1, 2, (byte) 0xff}));
        fixture.accept(closed(2));

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write("first".getBytes(StandardCharsets.UTF_8));
        expected.write(new byte[]{0, 1, 2, (byte) 0xff});
        assertArrayEquals(expected.toByteArray(),
                fixture.bridge.runnerStdout().readAllBytes());
        assertEquals(1, fixture.listener.readyCount);
        assertEquals(List.of("runner diagnostic"), fixture.listener.diagnostics);
        assertEquals(List.of(WebCanvasHostBridge.TerminalKind.RUNNER_CLOSED),
                fixture.listener.terminalKinds());
        assertEquals(2, fixture.bridge.terminal().orElseThrow().sequence());
        assertFalse(fixture.bridge.isReady());
    }

    @Test
    void wrongSourceIsIgnoredBeforeParsingAndForeignSessionIsIgnored() {
        Fixture fixture = fixture();

        fixture.bridge.acceptWebMessage(
                "https://foreign.canvas.invalid/index.html", "not-json");
        fixture.bridge.acceptWebMessage(SOURCE, object(
                FOREIGN_NONCE, "wrong-direction", "unknown", 99)
                .put("unexpected", true).toString());

        assertTrue(fixture.bridge.terminal().isEmpty());
        assertEquals(0, fixture.listener.readyCount);
    }

    @Test
    void malformedAndDuplicateJsonAreAuthenticatedFailStop() {
        List<String> malformed = List.of(
                "not-json",
                "null",
                "[]",
                "{\"sessionNonce\":\"" + NONCE + "\"",
                "{\"format\":\"" + WebCanvasHostBridge.FORMAT + "\","
                        + "\"version\":1,"
                        + "\"sessionNonce\":\"" + NONCE + "\","
                        + "\"sessionNonce\":\"" + NONCE + "\","
                        + "\"direction\":\"runner-to-host\","
                        + "\"kind\":\"ready\",\"sequence\":0}");

        for (String json : malformed) {
            Fixture fixture = fixture();
            fixture.accept(json);
            fixture.accept(ready());
            assertEquals(
                    List.of(WebCanvasHostBridge.TerminalKind.PROTOCOL_VIOLATION),
                    fixture.listener.terminalKinds(), json);
            assertEquals(0, fixture.listener.readyCount, json);
        }
    }

    @Test
    void trailingRootTokensCannotHideBehindAValidEnvelope() {
        for (String suffix : List.of("{}", "null", "[]", "true")) {
            Fixture fixture = fixture();
            fixture.accept(ready() + suffix);
            assertProtocolViolation(fixture);
        }
    }

    @Test
    void currentSessionRequiresExactEnvelopeAndFieldSet() {
        List<String> rejected = List.of(
                object(NONCE, "host-to-runner", "ready", 0).toString(),
                object(NONCE, "runner-to-host", "ready", 0)
                        .put("format", "wrong").toString(),
                object(NONCE, "runner-to-host", "ready", 0)
                        .put("version", 2).toString(),
                object(NONCE, "runner-to-host", "ready", 0)
                        .put("sequence", 0.0).toString(),
                object(NONCE, "runner-to-host", "ready", 0)
                        .put("extra", true).toString(),
                object(NONCE, "runner-to-host", "mystery", 0).toString(),
                object(NONCE, "runner-to-host", "ready", -1).toString(),
                withoutKind());

        for (String json : rejected) {
            Fixture fixture = fixture();
            fixture.accept(json);
            assertEquals(
                    WebCanvasHostBridge.TerminalKind.PROTOCOL_VIOLATION,
                    fixture.bridge.terminal().orElseThrow().kind(), json);
        }
    }

    @Test
    void readyIsAcceptedExactlyOnce() {
        Fixture fixture = fixture();
        fixture.accept(ready());
        fixture.accept(ready());

        assertEquals(1, fixture.listener.readyCount);
        assertEquals(
                List.of(WebCanvasHostBridge.TerminalKind.PROTOCOL_VIOLATION),
                fixture.listener.terminalKinds());
    }

    @Test
    void runnerChunksRequireReadyAndContiguousSafeSequences() {
        Fixture beforeReady = fixture();
        beforeReady.accept(chunk(1, new byte[]{1}));
        assertProtocolViolation(beforeReady);

        Fixture gap = fixture();
        gap.accept(ready());
        gap.accept(chunk(2, new byte[]{1}));
        assertProtocolViolation(gap);

        Fixture duplicate = fixture();
        duplicate.accept(ready());
        duplicate.accept(chunk(1, new byte[]{1}));
        duplicate.accept(chunk(1, new byte[]{2}));
        assertProtocolViolation(duplicate);

        Fixture unsafe = fixture();
        unsafe.accept(ready());
        unsafe.accept(object(
                NONCE, "runner-to-host", "chunk", Long.MAX_VALUE)
                .put("chunk", "AQ==").toString());
        assertProtocolViolation(unsafe);
    }

    @Test
    void runnerChunkMustBeNonEmptyBoundedCanonicalBase64() {
        List<String> rejectedChunks = List.of(
                "",
                "AQ",
                "%%%%",
                "AB==",
                "AAB=",
                Base64.getEncoder().encodeToString(
                        new byte[WebCanvasHostBridge.MAXIMUM_CHUNK_BYTES + 1]));

        for (String encoded : rejectedChunks) {
            Fixture fixture = fixture();
            fixture.accept(ready());
            fixture.accept(object(NONCE, "runner-to-host", "chunk", 1)
                    .put("chunk", encoded).toString());
            assertProtocolViolation(fixture);
        }
    }

    @Test
    void maximumRunnerChunkIsAdmittedWithoutMutation() throws Exception {
        byte[] maximum = new byte[WebCanvasHostBridge.MAXIMUM_CHUNK_BYTES];
        for (int index = 0; index < maximum.length; index++) {
            maximum[index] = (byte) (index * 31);
        }
        Fixture fixture = fixture();
        fixture.accept(ready());
        fixture.accept(chunk(1, maximum));
        fixture.accept(closed(1));

        assertArrayEquals(maximum, fixture.bridge.runnerStdout().readAllBytes());
    }

    @Test
    void pendingRunnerInputIsBoundedAndOverflowTerminatesOnce() throws Exception {
        RecordingSink sink = new RecordingSink();
        RecordingListener listener = new RecordingListener();
        WebCanvasHostBridge bridge = new WebCanvasHostBridge(
                SOURCE, NONCE, sink, listener, 1, 4);
        bridge.acceptWebMessage(SOURCE, ready());
        bridge.acceptWebMessage(SOURCE, chunk(1, new byte[]{1, 2, 3, 4}));
        bridge.acceptWebMessage(SOURCE, chunk(2, new byte[]{5}));
        bridge.acceptWebMessage(SOURCE, chunk(3, new byte[]{6}));

        assertEquals(-1, bridge.runnerStdout().read());
        assertEquals(
                List.of(WebCanvasHostBridge.TerminalKind.PROTOCOL_VIOLATION),
                listener.terminalKinds());
    }

    @Test
    void arbitraryHostWritesBecomeContiguousBoundedCanonicalChunks()
            throws Exception {
        Fixture fixture = fixture();
        byte[] bytes = new byte[WebCanvasHostBridge.MAXIMUM_CHUNK_BYTES + 5];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (index * 17);
        }

        fixture.bridge.runnerStdin().write(bytes, 0, 17);
        for (int index = 17; index < 21; index++) {
            fixture.bridge.runnerStdin().write(bytes[index]);
        }
        fixture.bridge.runnerStdin().write(bytes, 21, bytes.length - 21);
        fixture.bridge.runnerStdin().flush();

        assertEquals(2, fixture.sink.messages.size());
        ByteArrayOutputStream reconstructed = new ByteArrayOutputStream();
        for (int index = 0; index < fixture.sink.messages.size(); index++) {
            JsonNode message = JSON.readTree(fixture.sink.messages.get(index));
            assertEquals(WebCanvasHostBridge.FORMAT,
                    message.path("format").textValue());
            assertEquals(WebCanvasHostBridge.VERSION,
                    message.path("version").intValue());
            assertEquals(NONCE, message.path("sessionNonce").textValue());
            assertEquals("host-to-runner",
                    message.path("direction").textValue());
            assertEquals("chunk", message.path("kind").textValue());
            assertEquals(index + 1, message.path("sequence").longValue());
            byte[] decoded = Base64.getDecoder().decode(
                    message.path("chunk").textValue());
            assertTrue(decoded.length <= WebCanvasHostBridge.MAXIMUM_CHUNK_BYTES);
            assertEquals(message.path("chunk").textValue(),
                    Base64.getEncoder().encodeToString(decoded));
            reconstructed.write(decoded);
        }
        assertArrayEquals(bytes, reconstructed.toByteArray());
    }

    @Test
    void closingHostOutputFlushesResidualBytesAndIsIdempotent() throws Exception {
        Fixture fixture = fixture();
        fixture.bridge.runnerStdin().write(new byte[]{4, 5, 6});
        fixture.bridge.runnerStdin().close();
        fixture.bridge.runnerStdin().close();

        assertEquals(1, fixture.sink.messages.size());
        assertArrayEquals(new byte[]{4, 5, 6}, Base64.getDecoder().decode(
                JSON.readTree(fixture.sink.messages.get(0)).path("chunk").textValue()));
        assertThrows(IOException.class,
                () -> fixture.bridge.runnerStdin().write(7));
    }

    @Test
    void nativeSinkFailureClosesBothDirectionsAndNotifiesOnce() throws Exception {
        Fixture fixture = fixture();
        fixture.sink.failure = new IOException("native failure");

        fixture.bridge.runnerStdin().write(new byte[]{1, 2, 3});
        assertThrows(IOException.class, fixture.bridge.runnerStdin()::flush);
        fixture.bridge.failTransport("second failure");

        assertEquals(-1, fixture.bridge.runnerStdout().read());
        assertEquals(
                List.of(WebCanvasHostBridge.TerminalKind.TRANSPORT_FAILURE),
                fixture.listener.terminalKinds());
    }

    @Test
    void diagnosticsAreBoundedAndOnlyAdmittedAfterReady() {
        Fixture beforeReady = fixture();
        beforeReady.accept(diagnostic("early"));
        assertProtocolViolation(beforeReady);

        Fixture tooLong = fixture();
        tooLong.accept(ready());
        tooLong.accept(diagnostic(
                "x".repeat(WebCanvasHostBridge.MAXIMUM_DIAGNOSTIC_CHARACTERS + 1)));
        assertProtocolViolation(tooLong);
    }

    @Test
    void runnerFailureCarriesExactLastAcceptedHostSequence() throws Exception {
        Fixture accepted = fixture();
        accepted.bridge.runnerStdin().write(new byte[]{1});
        accepted.bridge.runnerStdin().flush();
        accepted.accept(failure(1, "runner failed"));

        WebCanvasHostBridge.Terminal terminal =
                accepted.bridge.terminal().orElseThrow();
        assertEquals(WebCanvasHostBridge.TerminalKind.RUNNER_FAILURE,
                terminal.kind());
        assertEquals("runner failed", terminal.message());
        assertEquals(1, terminal.sequence());

        Fixture wrongSequence = fixture();
        wrongSequence.accept(failure(1, "wrong sequence"));
        assertProtocolViolation(wrongSequence);
    }

    @Test
    void closedRequiresReadyAndExactLastRunnerSequence() {
        Fixture beforeReady = fixture();
        beforeReady.accept(closed(0));
        assertProtocolViolation(beforeReady);

        Fixture wrongSequence = fixture();
        wrongSequence.accept(ready());
        wrongSequence.accept(chunk(1, new byte[]{1}));
        wrongSequence.accept(closed(0));
        assertProtocolViolation(wrongSequence);
    }

    @Test
    void localCloseAndNativeFailureAreBoundedOneShotTerminals() throws Exception {
        Fixture local = fixture();
        local.bridge.close();
        local.bridge.close();
        local.bridge.failTransport("ignored");
        assertEquals(List.of(WebCanvasHostBridge.TerminalKind.LOCAL_CLOSE),
                local.listener.terminalKinds());
        assertThrows(IOException.class, () -> local.bridge.runnerStdin().write(1));
        assertEquals(-1, local.bridge.runnerStdout().read());

        Fixture nativeFailure = fixture();
        nativeFailure.bridge.failTransport(
                "x".repeat(WebCanvasHostBridge.MAXIMUM_DIAGNOSTIC_CHARACTERS + 50));
        assertEquals(WebCanvasHostBridge.MAXIMUM_DIAGNOSTIC_CHARACTERS,
                nativeFailure.bridge.terminal().orElseThrow().message().length());
    }

    @Test
    void readyListenerFailureTerminatesAndCannotEscapeOrReopenLifecycle() {
        RecordingSink sink = new RecordingSink();
        List<WebCanvasHostBridge.Terminal> terminals = new ArrayList<>();
        WebCanvasHostBridge bridge = new WebCanvasHostBridge(
                SOURCE,
                NONCE,
                sink,
                new WebCanvasHostBridge.Listener() {
                    @Override
                    public void ready() {
                        throw new IllegalStateException("ready callback");
                    }

                    @Override
                    public void diagnostic(String message) {
                        throw new IllegalStateException("diagnostic callback");
                    }

                    @Override
                    public void terminal(WebCanvasHostBridge.Terminal terminal) {
                        terminals.add(terminal);
                        throw new IllegalStateException("terminal callback");
                    }
                });

        bridge.acceptWebMessage(SOURCE, ready());
        bridge.acceptWebMessage(SOURCE, diagnostic("ignored after failure"));
        bridge.acceptWebMessage(SOURCE, closed(0));
        bridge.acceptWebMessage(SOURCE, "not-json");

        assertEquals(1, terminals.size());
        assertEquals(WebCanvasHostBridge.TerminalKind.TRANSPORT_FAILURE,
                terminals.get(0).kind());
    }

    @Test
    void diagnosticListenerFailureIsAnIsolatedNotificationFailure() {
        RecordingSink sink = new RecordingSink();
        List<WebCanvasHostBridge.Terminal> terminals = new ArrayList<>();
        WebCanvasHostBridge bridge = new WebCanvasHostBridge(
                SOURCE,
                NONCE,
                sink,
                new WebCanvasHostBridge.Listener() {
                    @Override
                    public void diagnostic(String message) {
                        throw new IllegalStateException("diagnostic callback");
                    }

                    @Override
                    public void terminal(WebCanvasHostBridge.Terminal terminal) {
                        terminals.add(terminal);
                    }
                });

        bridge.acceptWebMessage(SOURCE, ready());
        bridge.acceptWebMessage(SOURCE, diagnostic("notification only"));
        assertTrue(bridge.terminal().isEmpty());
        bridge.acceptWebMessage(SOURCE, closed(0));
        assertEquals(1, terminals.size());
        assertEquals(WebCanvasHostBridge.TerminalKind.RUNNER_CLOSED,
                terminals.get(0).kind());
    }

    @Test
    void constructorRejectsAmbiguousSourcesNoncesAndPendingBounds() {
        RecordingSink sink = new RecordingSink();
        RecordingListener listener = new RecordingListener();

        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasHostBridge(
                        "http://host/index.html", NONCE, sink, listener));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasHostBridge(
                        SOURCE + "?query", NONCE, sink, listener));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasHostBridge(
                        SOURCE, NONCE.toUpperCase(), sink, listener));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasHostBridge(
                        SOURCE, NONCE, sink, listener, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new WebCanvasHostBridge(
                        SOURCE, NONCE, sink, listener, 1, 0));
    }

    private static Fixture fixture() {
        RecordingSink sink = new RecordingSink();
        RecordingListener listener = new RecordingListener();
        return new Fixture(
                new WebCanvasHostBridge(SOURCE, NONCE, sink, listener),
                sink,
                listener);
    }

    private static void assertProtocolViolation(Fixture fixture) {
        assertEquals(
                List.of(WebCanvasHostBridge.TerminalKind.PROTOCOL_VIOLATION),
                fixture.listener.terminalKinds());
    }

    private static String ready() {
        return object(NONCE, "runner-to-host", "ready", 0).toString();
    }

    private static String chunk(long sequence, byte[] bytes) {
        return object(NONCE, "runner-to-host", "chunk", sequence)
                .put("chunk", Base64.getEncoder().encodeToString(bytes))
                .toString();
    }

    private static String diagnostic(String message) {
        return object(NONCE, "runner-to-host", "diagnostic", 0)
                .put("message", message)
                .toString();
    }

    private static String failure(long sequence, String message) {
        return object(NONCE, "runner-to-host", "failure", sequence)
                .put("message", message)
                .toString();
    }

    private static String closed(long sequence) {
        return object(NONCE, "runner-to-host", "closed", sequence).toString();
    }

    private static String withoutKind() {
        ObjectNode message = object(
                NONCE, "runner-to-host", "ready", 0);
        message.remove("kind");
        return message.toString();
    }

    private static ObjectNode object(
            String nonce, String direction, String kind, long sequence) {
        ObjectNode message = JSON.createObjectNode();
        message.put("format", WebCanvasHostBridge.FORMAT);
        message.put("version", WebCanvasHostBridge.VERSION);
        message.put("sessionNonce", nonce);
        message.put("direction", direction);
        message.put("kind", kind);
        message.put("sequence", sequence);
        return message;
    }

    private record Fixture(
            WebCanvasHostBridge bridge,
            RecordingSink sink,
            RecordingListener listener) {
        void accept(String json) {
            bridge.acceptWebMessage(SOURCE, json);
        }
    }

    private static final class RecordingSink
            implements WebCanvasHostBridge.MessageSink {
        private final List<String> messages = new ArrayList<>();
        private IOException failure;

        @Override
        public void postJsonMessage(String json) throws IOException {
            if (failure != null) {
                throw failure;
            }
            messages.add(json);
        }
    }

    private static final class RecordingListener
            implements WebCanvasHostBridge.Listener {
        private int readyCount;
        private final List<String> diagnostics = new ArrayList<>();
        private final List<WebCanvasHostBridge.Terminal> terminals =
                new ArrayList<>();

        @Override
        public void ready() {
            readyCount++;
        }

        @Override
        public void diagnostic(String message) {
            diagnostics.add(message);
        }

        @Override
        public void terminal(WebCanvasHostBridge.Terminal terminal) {
            terminals.add(terminal);
        }

        List<WebCanvasHostBridge.TerminalKind> terminalKinds() {
            return terminals.stream().map(WebCanvasHostBridge.Terminal::kind)
                    .toList();
        }
    }
}

package dev.flutter.netbeans.designer.canvas.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CanvasWireCodecContractTest {
    private static final CanvasSessionId SESSION = CanvasSessionId.parse(
            "80ef60ed-b108-4674-99a6-c1f3102f01ab");
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework-revision", "engine-revision", "3.12.0");
    private static final CanvasWireHandshakeLimits HANDSHAKE_LIMITS =
            CanvasWireHandshakeLimits.defaults();

    private final CanvasWireCodec codec = new CanvasWireCodec();

    @Test
    void roundTripsEveryVersionOneMessageType() throws Exception {
        List<CanvasWireMessage> messages = List.of(
                new CanvasHostHello(
                        SESSION,
                        0,
                        "0.1.3-SNAPSHOT",
                        List.of(
                                CanvasWireCapability.READ_ONLY_SELECTION,
                                CanvasWireCapability.READ_ONLY_RENDER,
                                CanvasWireCapability.READ_ONLY_LAYOUT,
                                CanvasWireCapability.SURFACE_PRESENTATION_V1,
                                CanvasWireCapability.PALETTE_DROP_CATALOG_INSERT_V1,
                                CanvasWireCapability.PALETTE_DROP_SOURCE_AWARE_V1),
                        HANDSHAKE_LIMITS),
                new CanvasRunnerHello(
                        SESSION,
                        0,
                        0,
                        "0.1.3-SNAPSHOT",
                        ENGINE,
                        List.of(
                                CanvasWireCapability.READ_ONLY_RENDER,
                                CanvasWireCapability.READ_ONLY_LAYOUT),
                        HANDSHAKE_LIMITS),
                new CanvasHostClose(
                        SESSION, 1, CanvasWireCloseReason.FORM_CLOSED),
                new CanvasRunnerClosed(SESSION, 1, 1),
                new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.of(0),
                        CanvasWireFailureCode.RUNNER_START_FAILED,
                        true,
                        "Flutter Canvas runner did not start."),
                new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INTERNAL_FAILURE,
                        true,
                        "Flutter Canvas runner stopped."));

        for (CanvasWireMessage message : messages) {
            byte[] first = codec.encode(message);
            CanvasWireMessage decoded = decoded(codec.decode(first));
            byte[] second = codec.encode(decoded);

            assertEquals(message, decoded, message.type().wireValue());
            assertArrayEquals(first, second, message.type().wireValue());
            assertFalse(new String(first, StandardCharsets.UTF_8).contains("\n"));
        }
    }

    @Test
    void hostHelloUsesStableCompactFieldOrderAndCanonicalCapabilities()
            throws Exception {
        CanvasHostHello hello = new CanvasHostHello(
                SESSION,
                0,
                "0.1.3",
                List.of(
                        CanvasWireCapability.READ_ONLY_SELECTION,
                        CanvasWireCapability.READ_ONLY_RENDER),
                HANDSHAKE_LIMITS);

        String json = new String(codec.encode(hello), StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-wire\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"sequence\":0,\"type\":\"host.hello\",\"body\":{"
                + "\"hostVersion\":\"0.1.3\","
                + "\"requestedCapabilities\":[\"readOnly.render\","
                + "\"readOnly.selection\"],"
                + "\"offeredLimits\":{"
                + "\"maxControlMessageBytes\":262144,"
                + "\"maxModelBytes\":16777216,"
                + "\"maxCatalogBytes\":4194304,"
                + "\"maxLayoutBytes\":8388608,"
                + "\"maxEncodedImageBytes\":16777216,"
                + "\"maxPhysicalDimension\":4096,"
                + "\"maxPhysicalPixels\":8388608}}}", json);
    }

    @Test
    void runnerClosedHasRequiredReplyAndAnEmptyCanonicalBody() throws Exception {
        String json = new String(
                codec.encode(new CanvasRunnerClosed(SESSION, 7, 3)),
                StandardCharsets.UTF_8);

        assertEquals("{\"format\":\"netbeans-flutter-canvas-wire\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"80ef60ed-b108-4674-99a6-c1f3102f01ab\","
                + "\"sequence\":7,\"type\":\"runner.closed\","
                + "\"replyTo\":3,\"body\":{}}", json);
    }

    @Test
    void valuesOwnCollectionsAndRejectInvalidSemanticBounds() {
        var mutable = new java.util.ArrayList<>(List.of(
                CanvasWireCapability.READ_ONLY_SELECTION));
        CanvasHostHello hello = new CanvasHostHello(
                SESSION, 0, "0.1.3", mutable, HANDSHAKE_LIMITS);
        mutable.clear();

        assertEquals(List.of(CanvasWireCapability.READ_ONLY_SELECTION),
                hello.requestedCapabilities());
        assertThrows(UnsupportedOperationException.class,
                () -> hello.requestedCapabilities().clear());
        assertThrows(IllegalArgumentException.class, () -> new CanvasHostHello(
                SESSION,
                0,
                "0.1.3",
                List.of(
                        CanvasWireCapability.READ_ONLY_RENDER,
                        CanvasWireCapability.READ_ONLY_RENDER),
                HANDSHAKE_LIMITS));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasHostClose(
                        SESSION,
                        CanvasWireProtocol.MAX_SEQUENCE + 1,
                        CanvasWireCloseReason.RESTART));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INTERNAL_FAILURE,
                        true,
                        "x".repeat(
                                CanvasWireLimits.DEFAULT_MAX_FAILURE_MESSAGE_CODE_POINTS + 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasWireHandshakeLimits(
                        CanvasWireHandshakeLimits.MAX_CONTROL_MESSAGE_BYTES + 1,
                        1, 1, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CanvasHostHello(
                SESSION, 0, "\uD800", List.of(), HANDSHAKE_LIMITS));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasEngineIdentity(
                        "3.44.8", "framework", "\uD800", "3.12.0"));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRunnerFailure(
                        SESSION,
                        0,
                        OptionalLong.empty(),
                        CanvasWireFailureCode.INTERNAL_FAILURE,
                        true,
                        "Runner \u202Efailed."));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasWireDiagnostic(
                        CanvasWireDiagnosticCode.INVALID_VALUE,
                        "/unsafe\npath",
                        "Invalid Canvas value."));
    }

    @Test
    void wireLimitsMayBeTightenedButNotRelaxedPastSafeCaps() {
        CanvasWireLimits defaults = CanvasWireLimits.defaults();
        CanvasWireLimits tightened = new CanvasWireLimits(
                1_024,
                8,
                1_024,
                64,
                1_024,
                512,
                16,
                8,
                3,
                32,
                256);

        assertEquals(tightened, new CanvasWireCodec(tightened).limits());
        assertEquals(256 * 1024, defaults.maxMessageBytes());
        assertThrows(IllegalArgumentException.class, () -> new CanvasWireLimits(
                CanvasWireLimits.DEFAULT_MAX_MESSAGE_BYTES + 1,
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasWireLimits(
                defaults.maxMessageBytes(),
                2,
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasWireLimits(
                defaults.maxMessageBytes(),
                defaults.maxJsonNestingDepth(),
                95,
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasWireLimits(
                defaults.maxMessageBytes(),
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                21,
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints()));
        assertThrows(IllegalArgumentException.class, () -> new CanvasWireLimits(
                defaults.maxMessageBytes(),
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                6,
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints()));
    }

    @Test
    void encodeRespectsTheActiveExactByteBoundary() throws Exception {
        CanvasHostClose close = new CanvasHostClose(
                SESSION, 1, CanvasWireCloseReason.RESTART);
        int size = codec.encode(close).length;
        CanvasWireCodec exact = new CanvasWireCodec(limitsWithBytes(size));
        CanvasWireCodec oneShort = new CanvasWireCodec(limitsWithBytes(size - 1));

        assertEquals(size, exact.encode(close).length);
        CanvasWireEncodeException failure = assertThrows(
                CanvasWireEncodeException.class,
                () -> oneShort.encode(close));
        assertEquals(CanvasWireDiagnosticCode.MESSAGE_LIMIT, failure.code());
    }

    @Test
    void protocolMinimumStructuralLimitsRoundTripTheLargestMessageShape()
            throws Exception {
        CanvasWireLimits defaults = CanvasWireLimits.defaults();
        CanvasWireLimits minimumStructure = new CanvasWireLimits(
                defaults.maxMessageBytes(),
                3,
                96,
                22,
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                7,
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints());
        CanvasWireCodec minimumCodec = new CanvasWireCodec(minimumStructure);
        CanvasRunnerHello hello = new CanvasRunnerHello(
                SESSION,
                0,
                0,
                "0.1.3",
                ENGINE,
                List.of(
                        CanvasWireCapability.READ_ONLY_RENDER,
                        CanvasWireCapability.READ_ONLY_LAYOUT,
                        CanvasWireCapability.READ_ONLY_SELECTION),
                HANDSHAKE_LIMITS);

        assertEquals(hello, decoded(minimumCodec.decode(
                minimumCodec.encode(hello))));
    }

    private static CanvasWireLimits limitsWithBytes(int bytes) {
        CanvasWireLimits defaults = CanvasWireLimits.defaults();
        return new CanvasWireLimits(
                bytes,
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                defaults.maxCapabilities(),
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints());
    }

    private static CanvasWireMessage decoded(CanvasWireDecodeResult result) {
        return assertInstanceOf(CanvasWireDecodeResult.Decoded.class, result).message();
    }
}

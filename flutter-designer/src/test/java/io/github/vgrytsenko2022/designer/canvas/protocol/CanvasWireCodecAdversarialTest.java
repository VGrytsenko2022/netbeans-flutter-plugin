package io.github.vgrytsenko2022.designer.canvas.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanvasWireCodecAdversarialTest {
    private static final CanvasSessionId SESSION = CanvasSessionId.parse(
            "80ef60ed-b108-4674-99a6-c1f3102f01ab");
    private final CanvasWireCodec codec = new CanvasWireCodec();

    @Test
    void acceptsTheExactByteBoundaryAndRejectsOnePastBeforeParsing() throws Exception {
        byte[] compact = codec.encode(new CanvasHostClose(
                SESSION, 1, CanvasWireCloseReason.RESTART));
        int maximum = CanvasWireLimits.DEFAULT_MAX_MESSAGE_BYTES;
        byte[] exact = Arrays.copyOf(compact, maximum);
        Arrays.fill(exact, compact.length, exact.length, (byte) ' ');
        byte[] onePast = Arrays.copyOf(exact, maximum + 1);

        assertInstanceOf(CanvasWireDecodeResult.Decoded.class, codec.decode(exact));
        assertInvalid(onePast, CanvasWireDiagnosticCode.MESSAGE_LIMIT);
    }

    @Test
    void rejectsUtfBomAndMalformedUtf8() {
        byte[] json = utf8(hostHello());
        assertInvalid(concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, json),
                CanvasWireDiagnosticCode.MALFORMED_UTF8);
        assertInvalid(concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, json),
                CanvasWireDiagnosticCode.MALFORMED_UTF8);
        assertInvalid(new byte[]{'{', '"', (byte) 0xC3, '"', '}'},
                CanvasWireDiagnosticCode.MALFORMED_UTF8);
    }

    @Test
    void rejectsDuplicateUnknownAndMissingEnvelopeFields() {
        String valid = hostHello();
        assertInvalid(utf8(valid.replaceFirst(
                "\"format\":",
                "\"format\":\"netbeans-flutter-canvas-wire\",\"format\":")),
                CanvasWireDiagnosticCode.DUPLICATE_FIELD);
        assertInvalid(utf8(valid.replaceFirst(
                "\"body\":",
                "\"future\":true,\"body\":")),
                CanvasWireDiagnosticCode.UNKNOWN_FIELD);
        assertInvalid(utf8(valid.replace(
                "\"format\":\"netbeans-flutter-canvas-wire\",", "")),
                CanvasWireDiagnosticCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void rejectsUnsupportedFormatVersionAndMessageKind() {
        assertInvalid(utf8(hostHello().replace(
                "netbeans-flutter-canvas-wire", "other-format")),
                CanvasWireDiagnosticCode.UNSUPPORTED_FORMAT);
        assertInvalid(utf8(hostHello().replace(
                "\"protocolVersion\":1", "\"protocolVersion\":2")),
                CanvasWireDiagnosticCode.UNSUPPORTED_VERSION);
        assertInvalid(utf8(hostHello().replace(
                "\"type\":\"host.hello\"", "\"type\":\"host.future\"")),
                CanvasWireDiagnosticCode.UNKNOWN_MESSAGE_TYPE);
    }

    @Test
    void rejectsUnknownDuplicateAndOverLimitBodyContent() {
        String valid = hostHello();
        assertInvalid(utf8(valid.replace(
                "\"hostVersion\":\"0.1.3\"",
                "\"hostVersion\":\"0.1.3\",\"unknown\":true")),
                CanvasWireDiagnosticCode.UNKNOWN_FIELD);
        assertInvalid(utf8(valid.replace(
                "\"hostVersion\":\"0.1.3\"",
                "\"hostVersion\":\"0.1.3\",\"hostVersion\":\"0.1.3\"")),
                CanvasWireDiagnosticCode.DUPLICATE_FIELD);

        CanvasWireLimits defaults = CanvasWireLimits.defaults();
        CanvasWireLimits twoCapabilities = new CanvasWireLimits(
                defaults.maxMessageBytes(),
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxObjectFields(),
                defaults.maxArrayElements(),
                2,
                defaults.maxCapabilityCodePoints(),
                defaults.maxFailureMessageCodePoints());
        CanvasWireDecodeResult result = new CanvasWireCodec(twoCapabilities).decode(
                utf8(valid.replace(
                        "[\"readOnly.render\"]",
                        "[\"readOnly.render\",\"readOnly.layout\","
                                + "\"readOnly.selection\"]")));
        assertEquals(CanvasWireDiagnosticCode.RESOURCE_LIMIT,
                invalid(result).diagnostic().code());
    }

    @Test
    void rejectsUnknownAndDuplicateCapabilities() {
        assertInvalid(utf8(hostHello().replace(
                "[\"readOnly.render\"]", "[\"write.files\"]")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(hostHello().replace(
                "[\"readOnly.render\"]",
                "[\"readOnly.render\",\"readOnly.render\"]")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
    }

    @Test
    void rejectsWrongTypesFractionsSequencePoisoningAndReplyShape() {
        assertInvalid(utf8(hostHello().replace(
                "\"sequence\":0", "\"sequence\":0.0")),
                CanvasWireDiagnosticCode.WRONG_VALUE_TYPE);
        assertInvalid(utf8(hostHello().replace(
                "\"sequence\":0",
                "\"sequence\":" + (CanvasWireProtocol.MAX_SEQUENCE + 1))),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(hostHello().replace(
                "\"body\":", "\"replyTo\":0,\"body\":")),
                CanvasWireDiagnosticCode.INVALID_VALUE);

        String closed = "{\"format\":\"netbeans-flutter-canvas-wire\","
                + "\"protocolVersion\":1,\"sessionId\":\"" + SESSION + "\","
                + "\"sequence\":0,\"type\":\"runner.closed\",\"body\":{}}";
        assertInvalid(utf8(closed),
                CanvasWireDiagnosticCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void rejectsInvalidSessionEnumsLimitsAndFailureText() {
        assertInvalid(utf8(hostHello().replace(
                SESSION.toString(), SESSION.toString().toUpperCase())),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(hostHello().replace(
                "\"maxPhysicalDimension\":4096",
                "\"maxPhysicalDimension\":4097")),
                CanvasWireDiagnosticCode.RESOURCE_LIMIT);
        CanvasWireDecodeResult.Invalid oversized = invalid(codec.decode(utf8(
                hostHello().replace(
                        "\"maxPhysicalDimension\":4096",
                        "\"maxPhysicalDimension\":4097"))));
        assertEquals("/body/offeredLimits/maxPhysicalDimension",
                oversized.diagnostic().path());

        CanvasWireLimits defaults = CanvasWireLimits.defaults();
        CanvasWireLimits active = new CanvasWireLimits(
                1_024,
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
        CanvasWireDiagnostic activeLimit = invalid(
                new CanvasWireCodec(active).decode(utf8(hostHello())))
                .diagnostic();
        assertEquals(CanvasWireDiagnosticCode.RESOURCE_LIMIT, activeLimit.code());
        assertEquals("/body/offeredLimits/maxControlMessageBytes",
                activeLimit.path());

        String failure = runnerFailure().replace(
                "\"internalFailure\"", "\"futureFailure\"");
        assertInvalid(utf8(failure), CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(runnerFailure().replace(
                "\"Runner failed.\"", "\" Runner failed.\"")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(runnerFailure().replace(
                "Runner failed.", "Runner\\u202ERLO.")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(runnerFailure().replace(
                "Runner failed.", "Runner\\u2028next line.")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(runnerFailure().replace(
                "Runner failed.", "Runner\\u200Dhidden.")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
        assertInvalid(utf8(runnerFailure().replace(
                "Runner failed.", "\\u00A0Runner failed.")),
                CanvasWireDiagnosticCode.INVALID_VALUE);
    }

    @Test
    void rejectsNonstandardJsonUnpairedSurrogatesDeepNestingAndTrailingContent() {
        assertInvalid(utf8("//comment\n" + hostHello()),
                CanvasWireDiagnosticCode.MALFORMED_JSON);
        assertInvalid(utf8(hostHello().replace(
                "\"hostVersion\":\"0.1.3\"",
                "\"hostVersion\":\"\\uD800\"")),
                CanvasWireDiagnosticCode.INVALID_VALUE);

        String nested = "[".repeat(CanvasWireLimits.DEFAULT_MAX_JSON_NESTING_DEPTH + 1)
                + "null"
                + "]".repeat(CanvasWireLimits.DEFAULT_MAX_JSON_NESTING_DEPTH + 1);
        assertInvalid(utf8(hostHello().replace(
                "\"hostVersion\":\"0.1.3\"",
                "\"hostVersion\":\"0.1.3\",\"nested\":" + nested)),
                CanvasWireDiagnosticCode.RESOURCE_LIMIT);
        assertInvalid(utf8(hostHello() + "{}"),
                CanvasWireDiagnosticCode.TRAILING_CONTENT);
    }

    @Test
    void escapesUntrustedUnknownFieldNamesInDiagnosticPaths() {
        String injected = hostHello().replace(
                "\"body\":",
                "\"\\n\\u202E\":true,\"body\":");

        CanvasWireDiagnostic diagnostic = invalid(
                codec.decode(utf8(injected))).diagnostic();

        assertEquals(CanvasWireDiagnosticCode.UNKNOWN_FIELD, diagnostic.code());
        assertEquals("/~uA~u202E", diagnostic.path());
        assertTrue(diagnostic.path().codePoints().noneMatch(
                codePoint -> Character.isISOControl(codePoint)
                        || Character.getType(codePoint) == Character.FORMAT));
    }

    private static String hostHello() {
        return "{\"format\":\"netbeans-flutter-canvas-wire\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"" + SESSION + "\","
                + "\"sequence\":0,\"type\":\"host.hello\",\"body\":{"
                + "\"hostVersion\":\"0.1.3\","
                + "\"requestedCapabilities\":[\"readOnly.render\"],"
                + "\"offeredLimits\":" + limitsJson() + "}}";
    }

    private static String runnerFailure() {
        return "{\"format\":\"netbeans-flutter-canvas-wire\","
                + "\"protocolVersion\":1,"
                + "\"sessionId\":\"" + SESSION + "\","
                + "\"sequence\":0,\"type\":\"runner.failure\","
                + "\"body\":{\"code\":\"internalFailure\","
                + "\"fatal\":true,\"message\":\"Runner failed.\"}}";
    }

    private static String limitsJson() {
        return "{\"maxControlMessageBytes\":262144,"
                + "\"maxModelBytes\":16777216,"
                + "\"maxCatalogBytes\":4194304,"
                + "\"maxLayoutBytes\":8388608,"
                + "\"maxEncodedImageBytes\":16777216,"
                + "\"maxPhysicalDimension\":4096,"
                + "\"maxPhysicalPixels\":8388608}";
    }

    private void assertInvalid(byte[] bytes, CanvasWireDiagnosticCode code) {
        assertEquals(code, invalid(codec.decode(bytes)).diagnostic().code());
    }

    private static CanvasWireDecodeResult.Invalid invalid(CanvasWireDecodeResult result) {
        return assertInstanceOf(CanvasWireDecodeResult.Invalid.class, result);
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}

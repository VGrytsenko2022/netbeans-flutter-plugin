package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.DesignerDocument;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdCodecAdversarialTest {
    private static final byte[][] UTF_16_OR_32_BOMS = {
        {(byte) 0xFE, (byte) 0xFF},
        {(byte) 0xFF, (byte) 0xFE},
        {0x00, 0x00, (byte) 0xFE, (byte) 0xFF},
        {(byte) 0xFF, (byte) 0xFE, 0x00, 0x00}
    };

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void rejectsUtf16AndUtf32BomSignaturesBeforeJsonParsing() throws Exception {
        byte[] current = utf8(document(widgetChain(1), ""));
        for (byte[] bom : UTF_16_OR_32_BOMS) {
            byte[] input = concat(bom, current);
            FdDecodeResult.Invalid invalid = invalid(codec.decode(input));
            assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.MALFORMED_UTF8));
            assertArrayEquals(input, invalid.original().copyBytes());
        }
    }

    @Test
    void rejectsEscapedLoneHighAndLowSurrogates() throws Exception {
        String minimal = document(widgetChain(1), "");
        for (String escape : new String[]{"\\" + "uD800", "\\" + "uDC00"}) {
            String input = "{\n  \"$schema\": \"" + escape + "\",\n"
                    + minimal.substring(2);
            FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(input)));
            assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.INVALID_VALUE),
                    () -> invalid.diagnostics().toString());
        }
    }

    @Test
    void rejectsNestedAndEscapedEquivalentDuplicateNames() throws Exception {
        String minimal = document(widgetChain(1), "");
        String nestedDuplicate = minimal.replace(
                "\"dartFile\": \"minimal.dart\",",
                "\"dartFile\": \"minimal.dart\",\n"
                + "    \"dartFile\": \"minimal.dart\",");
        String escapedFormatName = "\"fo" + "\\" + "u0072mat\"";
        String escapedEquivalent = minimal.replace(
                "\"format\": \"netbeans-flutter-designer\",",
                "\"format\": \"netbeans-flutter-designer\",\n"
                + "  " + escapedFormatName + ": \"netbeans-flutter-designer\",");

        for (String input : new String[]{nestedDuplicate, escapedEquivalent}) {
            FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(input)));
            assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.DUPLICATE_FIELD),
                    () -> invalid.diagnostics().toString());
        }
    }

    @Test
    void rejectsCommentsTrailingCommasAndOtherNonstandardJson() throws Exception {
        String minimal = document(widgetChain(1), "");
        int closingBrace = minimal.lastIndexOf('}');
        String trailingComma = minimal.substring(0, closingBrace)
                + ",\n"
                + minimal.substring(closingBrace);
        String comment = "// comments are not JSON\n" + minimal;
        String singleQuotedName = minimal.replace("\"format\"", "'format'");
        String nonFinite = minimal.replace("\"schemaVersion\": 1", "\"schemaVersion\": NaN");

        for (String input : new String[]{comment, trailingComma, singleQuotedName, nonFinite}) {
            FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(input)));
            assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.MALFORMED_JSON),
                    () -> invalid.diagnostics().toString());
        }
    }

    @Test
    void enforcesTheDefaultNumberTokenBoundaryOnReadAndWrite() throws Exception {
        String digits128 = "9".repeat(FdCodecLimits.DEFAULT_MAX_NUMBER_CHARACTERS);
        String digits129 = digits128 + "9";

        FdDecodeResult.UnsupportedNewer boundary = assertInstanceOf(
                FdDecodeResult.UnsupportedNewer.class,
                codec.decode(utf8(documentWithVersion(digits128, widgetChain(1), ""))));
        assertEquals(new BigInteger(digits128), boundary.declaredSchemaVersion());

        FdDecodeResult.Invalid onePast = invalid(
                codec.decode(utf8(documentWithVersion(digits129, widgetChain(1), ""))));
        assertTrue(hasDiagnostic(onePast, FdCodecDiagnosticCode.RESOURCE_LIMIT));

        DesignerDocument encodable = current(codec.decode(utf8(document(
                widgetWithInteger(digits128), "")))).document();
        codec.encode(encodable);

        FdDocumentCodec relaxed = new FdDocumentCodec(
                withLimit("maxNumberCharacters", digits129.length()));
        DesignerDocument tooWide = current(relaxed.decode(utf8(document(
                widgetWithInteger(digits129), "")))).document();
        FdEncodeException encodeFailure = assertThrows(
                FdEncodeException.class,
                () -> codec.encode(tooWide));
        assertEquals(FdCodecDiagnosticCode.RESOURCE_LIMIT,
                encodeFailure.diagnostic().code());
    }

    @Test
    void enforcesTheDefaultAbsoluteDecimalScaleBoundaryOnReadAndWrite() throws Exception {
        int maximum = FdCodecLimits.DEFAULT_MAX_ABSOLUTE_DECIMAL_SCALE;
        String atBoundary = document(
                widgetChain(1),
                extensionTop("1e-" + maximum));
        String onePast = document(
                widgetChain(1),
                extensionTop("1e-" + (maximum + 1)));

        DesignerDocument boundary = current(codec.decode(utf8(atBoundary))).document();
        codec.encode(boundary);

        FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(onePast)));
        assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.NUMBER_RANGE));

        FdDocumentCodec relaxed = new FdDocumentCodec(
                withLimit("maxAbsoluteDecimalScale", maximum + 1));
        DesignerDocument tooScaled = current(relaxed.decode(utf8(onePast))).document();
        FdEncodeException encodeFailure = assertThrows(
                FdEncodeException.class,
                () -> codec.encode(tooScaled));
        assertEquals(FdCodecDiagnosticCode.NUMBER_RANGE,
                encodeFailure.diagnostic().code());
    }

    @Test
    void enforcesWidgetAndSlotLimitsSymmetrically() throws Exception {
        assertSymmetricLimit(
                "maxWidgetDepth", 3,
                document(widgetChain(3), ""),
                document(widgetChain(4), ""));
        assertSymmetricLimit(
                "maxWidgetNodes", 3,
                document(widgetList(2), ""),
                document(widgetList(3), ""));
        assertSymmetricLimit(
                "maxPropertiesPerWidget", 2,
                document(widgetWithProperties(2), ""),
                document(widgetWithProperties(3), ""));
        assertSymmetricLimit(
                "maxSlotsPerWidget", 2,
                document(widgetWithSlots(2), ""),
                document(widgetWithSlots(3), ""));
        assertSymmetricLimit(
                "maxListChildren", 2,
                document(widgetList(2), ""),
                document(widgetList(3), ""));
    }

    @Test
    void defaultPropertyBudgetSupports1024AndRejects1025WithoutWeakeningExplicitLowerLimits() throws Exception {
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        String boundary = document(widgetWithProperties(1024), "");
        String excessive = document(widgetWithProperties(1025), "");
        var decoded = current(codec.decode(utf8(boundary))).document();
        assertEquals(1024, decoded.root().properties().size());
        assertEquals(decoded, current(codec.decode(codec.encode(decoded))).document());
        assertTrue(hasDiagnostic(invalid(codec.decode(utf8(excessive))), FdCodecDiagnosticCode.RESOURCE_LIMIT));
        var properties = new java.util.LinkedHashMap<>(decoded.root().properties());
        properties.put(new dev.flutter.netbeans.designer.model.PropertyName("extra"), new dev.flutter.netbeans.designer.model.PropertyValue.NullValue());
        var root = decoded.root();
        var oversized = new DesignerDocument(decoded.documentId(), decoded.source(),
                new dev.flutter.netbeans.designer.model.WidgetNode(root.id(), root.type(), properties, root.slots(), root.extensions(), root.stateBinding(), root.propertyBindings()));
        assertEquals(FdCodecDiagnosticCode.RESOURCE_LIMIT,
                assertThrows(FdEncodeException.class, () -> codec.encode(oversized)).diagnostic().code());
        assertSymmetricLimit("maxPropertiesPerWidget", 256,
                document(widgetWithProperties(256), ""), document(widgetWithProperties(257), ""));
        assertEquals(1024, FdCodecLimits.defaults().maxJsonObjectFields());
        assertEquals(16 * 1024 * 1024, FdCodecLimits.defaults().maxDocumentBytes());
    }

    @Test
    void enforcesExtensionAndGenericContainerLimitsSymmetrically() throws Exception {
        assertSymmetricLimit(
                "maxExtensionNestingDepth", 2,
                document(widgetChain(1), extensionTop("{\"a\": true}")),
                document(widgetChain(1), extensionTop("{\"a\": {\"b\": true}}")));
        assertSymmetricLimit(
                "maxExtensionValues", 2,
                document(widgetChain(1), extensionTop("{\"a\": true}")),
                document(widgetChain(1), extensionTop("{\"a\": true, \"b\": false}")));
        assertSymmetricLimit(
                "maxJsonObjectFields", 6,
                document(widgetChain(1), extensionTop(objectWithFields(6))),
                document(widgetChain(1), extensionTop(objectWithFields(7))));
        assertSymmetricLimit(
                "maxJsonArrayElements", 2,
                document(widgetChain(1), extensionTop("[true, false]")),
                document(widgetChain(1), extensionTop("[true, false, null]")));
    }

    @Test
    void canonicalWriterHonorsTheExactOutputByteBoundary() throws Exception {
        DesignerDocument document = current(
                codec.decode(utf8(document(widgetChain(1), "")))).document();
        int canonicalSize = codec.encode(document).size();

        FdDocumentCodec exact = new FdDocumentCodec(
                withLimit("maxDocumentBytes", canonicalSize));
        assertEquals(canonicalSize, exact.encode(document).size());

        FdDocumentCodec oneByteShort = new FdDocumentCodec(
                withLimit("maxDocumentBytes", canonicalSize - 1));
        FdEncodeException failure = assertThrows(
                FdEncodeException.class,
                () -> oneByteShort.encode(document));
        assertEquals(FdCodecDiagnosticCode.RESOURCE_LIMIT,
                failure.diagnostic().code());
    }

    @Test
    void defaultDepthOf256DecodesAndEncodesWithoutStackOverflow() throws Exception {
        String deepestSupported = document(
                widgetChain(FdCodecLimits.DEFAULT_MAX_WIDGET_DEPTH), "");

        FdDecodeResult.Current decoded = current(codec.decode(utf8(deepestSupported)));
        OriginalFdBytes canonical = codec.encode(decoded.document());
        FdDecodeResult.Current roundTrip = current(codec.decode(canonical));

        assertEquals(FdCodecLimits.DEFAULT_MAX_WIDGET_DEPTH,
                countSingleChildChain(roundTrip.document()));
    }

    private void assertSymmetricLimit(
            String limitName,
            int maximum,
            String atBoundary,
            String onePast) throws Exception {
        FdDocumentCodec limited = new FdDocumentCodec(withLimit(limitName, maximum));

        DesignerDocument accepted = current(limited.decode(utf8(atBoundary))).document();
        limited.encode(accepted);

        FdDecodeResult.Invalid rejected = invalid(limited.decode(utf8(onePast)));
        assertTrue(hasDiagnostic(rejected, FdCodecDiagnosticCode.RESOURCE_LIMIT),
                () -> limitName + ": " + rejected.diagnostics());

        DesignerDocument oversized = current(codec.decode(utf8(onePast))).document();
        FdEncodeException encodeFailure = assertThrows(
                FdEncodeException.class,
                () -> limited.encode(oversized),
                limitName);
        assertEquals(FdCodecDiagnosticCode.RESOURCE_LIMIT,
                encodeFailure.diagnostic().code(),
                limitName);
    }

    private static int countSingleChildChain(DesignerDocument document) {
        int depth = 0;
        var node = document.root();
        while (node != null) {
            depth++;
            var slot = node.slots().get(new dev.flutter.netbeans.designer.model.SlotName("child"));
            if (!(slot instanceof dev.flutter.netbeans.designer.model.WidgetSlot.SingleSlot single)
                    || single.child().isEmpty()) {
                node = null;
            } else {
                node = single.child().orElseThrow();
            }
        }
        return depth;
    }

    private static String document(String root, String optionalTopLevelSuffix) {
        return documentWithVersion("1", root, optionalTopLevelSuffix);
    }

    private static String documentWithVersion(
            String version,
            String root,
            String optionalTopLevelSuffix) {
        return """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": %s,
                  "documentId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                  "source": {
                    "dartFile": "minimal.dart",
                    "className": "Minimal",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {
                        "sha256": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                      },
                      "build": {
                        "sha256": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
                      }
                    }
                  },
                  "root": %s%s
                }
                """.formatted(version, root, optionalTopLevelSuffix);
    }

    private static String extensionTop(String value) {
        return ",\n  \"extensions\": {\"test.example:data\": " + value + "}";
    }

    private static String widgetChain(int depth) {
        if (depth <= 0) {
            throw new IllegalArgumentException("depth must be positive");
        }
        StringBuilder json = new StringBuilder(depth * 180);
        for (int level = 1; level <= depth; level++) {
            json.append("{\"id\":\"").append(id(level))
                    .append("\",\"type\":\"flutter.widgets.Text\",\"properties\":{},\"slots\":");
            if (level < depth) {
                json.append("{\"child\":{\"kind\":\"single\",\"child\":");
            } else {
                json.append("{}");
            }
        }
        json.append('}');
        for (int level = depth - 1; level >= 1; level--) {
            json.append("}}}");
        }
        return json.toString();
    }

    private static String widgetList(int children) {
        StringBuilder json = new StringBuilder();
        json.append("{\"id\":\"").append(id(1))
                .append("\",\"type\":\"flutter.widgets.Column\",\"properties\":{},")
                .append("\"slots\":{\"children\":{\"kind\":\"list\",\"children\":[");
        for (int index = 0; index < children; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(leaf(10 + index));
        }
        return json.append("]}}}").toString();
    }

    private static String widgetWithProperties(int properties) {
        StringBuilder json = new StringBuilder();
        json.append("{\"id\":\"").append(id(1))
                .append("\",\"type\":\"flutter.widgets.Text\",\"properties\":{");
        for (int index = 0; index < properties; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("\"p").append(index)
                    .append("\":{\"kind\":\"string\",\"value\":\"x\"}");
        }
        return json.append("},\"slots\":{}}").toString();
    }

    private static String widgetWithInteger(String digits) {
        return "{\"id\":\"" + id(1)
                + "\",\"type\":\"flutter.widgets.Text\",\"properties\":{"
                + "\"maxLines\":{\"kind\":\"integer\",\"value\":" + digits + "}},"
                + "\"slots\":{}}";
    }

    private static String widgetWithSlots(int slots) {
        StringBuilder json = new StringBuilder();
        json.append("{\"id\":\"").append(id(1))
                .append("\",\"type\":\"flutter.widgets.Container\",\"properties\":{},\"slots\":{");
        for (int index = 0; index < slots; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("\"s").append(index)
                    .append("\":{\"kind\":\"single\",\"child\":null}");
        }
        return json.append("}}").toString();
    }

    private static String leaf(int index) {
        return "{\"id\":\"" + id(index)
                + "\",\"type\":\"flutter.widgets.Text\",\"properties\":{},\"slots\":{}}";
    }

    private static String objectWithFields(int fields) {
        StringBuilder json = new StringBuilder("{");
        for (int index = 0; index < fields; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("\"k").append(index).append("\":true");
        }
        return json.append('}').toString();
    }

    private static String id(int index) {
        return "00000000-0000-4000-8000-" + String.format("%012x", index);
    }

    private static FdCodecLimits withLimit(String name, Number value) {
        try {
            FdCodecLimits defaults = FdCodecLimits.defaults();
            RecordComponent[] components = FdCodecLimits.class.getRecordComponents();
            Class<?>[] types = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);
            Object[] arguments = new Object[components.length];
            boolean found = false;
            for (int index = 0; index < components.length; index++) {
                arguments[index] = components[index].getAccessor().invoke(defaults);
                if (components[index].getName().equals(name)) {
                    if (components[index].getType() == long.class) {
                        arguments[index] = value.longValue();
                    } else {
                        arguments[index] = value.intValue();
                    }
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown FdCodecLimits component " + name);
            }
            Constructor<FdCodecLimits> constructor =
                    FdCodecLimits.class.getDeclaredConstructor(types);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Cannot construct FdCodecLimits for test", failure);
        }
    }

    private static FdDecodeResult.Current current(FdDecodeResult result) {
        return assertInstanceOf(FdDecodeResult.Current.class, result,
                () -> "Expected Current, got " + describe(result));
    }

    private static FdDecodeResult.Invalid invalid(FdDecodeResult result) {
        return assertInstanceOf(FdDecodeResult.Invalid.class, result,
                () -> "Expected Invalid, got " + describe(result));
    }

    private static String describe(FdDecodeResult result) {
        if (result instanceof FdDecodeResult.Invalid invalid) {
            return invalid.diagnostics().toString();
        }
        return result.toString();
    }

    private static boolean hasDiagnostic(
            FdDecodeResult.Invalid result,
            FdCodecDiagnosticCode code) {
        return result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code() == code);
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}

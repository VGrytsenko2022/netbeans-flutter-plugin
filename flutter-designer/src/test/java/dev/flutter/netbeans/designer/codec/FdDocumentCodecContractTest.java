package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.designer.model.ExtensionKey;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.json.JsonValue;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdDocumentCodecContractTest {
    private static final Path GOLDEN_DOCUMENT =
            Path.of("docs", "flutter-designer", "examples", "home_page.fd");
    private static final String ALL_FEATURES_RESOURCE =
            "dev/flutter/netbeans/designer/codec/all-v1-features.fd";
    private static final byte[] UTF_8_BOM = {
        (byte) 0xEF, (byte) 0xBB, (byte) 0xBF
    };

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void decodesTheDocumentedGoldenAndEncodesItByteForByteCanonically() throws Exception {
        byte[] documented = Files.readAllBytes(findRepositoryFile(GOLDEN_DOCUMENT));

        FdDecodeResult.Current first = current(codec.decode(documented));
        assertAll(
                () -> assertEquals(DesignerDocument.SCHEMA_VERSION, first.sourceSchemaVersion()),
                () -> assertFalse(first.migrated()),
                () -> assertTrue(first.original().contentEquals(documented)),
                () -> assertEquals("home_page.dart", first.document().source().dartFile()),
                () -> assertEquals("HomePage", first.document().source().className()),
                () -> assertEquals("flutter.material.Scaffold", first.document().root().type().value()));

        OriginalFdBytes encoded = codec.encode(first.document());
        assertArrayEquals(documented, encoded.copyBytes(),
                "The checked-in home_page.fd is the canonical v1 golden");

        FdDecodeResult.Current roundTrip = current(codec.decode(encoded));
        assertEquals(first.document(), roundTrip.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(roundTrip.document()).copyBytes(),
                "decode/encode must reach a stable fixed point");
        assertArrayEquals(encoded.copyBytes(), codec.encode(first.document()).copyBytes(),
                "repeated encoding of the same immutable model must be deterministic");
    }

    @Test
    void dispatchesACompleteFutureDocumentWithoutInterpretingItsV1Body() throws Exception {
        byte[] versionTwo = replaceAscii(
                Files.readAllBytes(findRepositoryFile(GOLDEN_DOCUMENT)),
                "\"schemaVersion\": 1",
                "\"schemaVersion\": 2");
        String futureJson = new String(versionTwo, StandardCharsets.UTF_8);
        int closingBrace = futureJson.lastIndexOf('}');
        byte[] future = utf8(futureJson.substring(0, closingBrace)
                + ",\n  \"futureOnly\": {\"newShape\": true}\n"
                + futureJson.substring(closingBrace));

        FdDecodeResult.UnsupportedNewer result = assertInstanceOf(
                FdDecodeResult.UnsupportedNewer.class,
                codec.decode(future));
        assertAll(
                () -> assertEquals(BigInteger.valueOf(2), result.declaredSchemaVersion()),
                () -> assertArrayEquals(future, result.original().copyBytes()));

        future[0] ^= 1;
        assertFalse(result.original().contentEquals(future),
                "UnsupportedNewer must retain its own exact immutable input snapshot");
    }

    @Test
    void malformedFutureInputIsInvalidRatherThanUnsupported() throws Exception {
        byte[] future = utf8(minimalDocument("2", "{}"));
        byte[] withTrailingGarbage = Arrays.copyOf(future, future.length + 1);
        withTrailingGarbage[withTrailingGarbage.length - 1] = 'x';

        FdDecodeResult.Invalid invalid = invalid(codec.decode(withTrailingGarbage));
        assertAll(
                () -> assertEquals(
                        BigInteger.valueOf(2),
                        invalid.declaredSchemaVersion().orElseThrow()),
                () -> assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.TRAILING_CONTENT)),
                () -> assertArrayEquals(withTrailingGarbage, invalid.original().copyBytes()));
    }

    @Test
    void rejectsDuplicateFieldsTrailingContentAndMalformedUtf8() throws Exception {
        String minimal = minimalDocument("1", "{}");
        String duplicate = minimal.replace(
                "\"format\": \"netbeans-flutter-designer\",",
                "\"format\": \"netbeans-flutter-designer\",\n"
                + "  \"format\": \"netbeans-flutter-designer\",");
        FdDecodeResult.Invalid duplicateResult = invalid(codec.decode(utf8(duplicate)));
        assertTrue(hasDiagnostic(duplicateResult, FdCodecDiagnosticCode.DUPLICATE_FIELD));

        byte[] valid = utf8(minimal);
        byte[] trailing = Arrays.copyOf(valid, valid.length + 4);
        System.arraycopy(utf8("null"), 0, trailing, valid.length, 4);
        FdDecodeResult.Invalid trailingResult = invalid(codec.decode(trailing));
        assertTrue(hasDiagnostic(trailingResult, FdCodecDiagnosticCode.TRAILING_CONTENT));

        byte[] malformedUtf8 = {'{', '"', 'x', '"', ':', '"', (byte) 0xC3, 0x28, '"', '}'};
        FdDecodeResult.Invalid utf8Result = invalid(codec.decode(malformedUtf8));
        assertTrue(hasDiagnostic(utf8Result, FdCodecDiagnosticCode.MALFORMED_UTF8));
    }

    @Test
    void acceptsOneUtf8BomPreservesItExactlyAndRejectsASecondBom() throws Exception {
        byte[] canonical = utf8(minimalDocument("1", "{}"));
        byte[] withBom = concat(UTF_8_BOM, canonical);

        FdDecodeResult.Current accepted = current(codec.decode(withBom));
        assertArrayEquals(withBom, accepted.original().copyBytes());

        byte[] encoded = codec.encode(accepted.document()).copyBytes();
        assertFalse(startsWith(encoded, UTF_8_BOM));

        FdDecodeResult.Invalid repeatedBom = invalid(codec.decode(concat(UTF_8_BOM, withBom)));
        assertArrayEquals(concat(UTF_8_BOM, withBom), repeatedBom.original().copyBytes());
    }

    @Test
    void treatsSchemaVersionAsAMathematicalInteger() throws Exception {
        for (String currentVersion : new String[]{"1", "1.0", "1e0", "10e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(currentVersion, "{}"))));
            assertEquals(1, result.sourceSchemaVersion(), currentVersion);
        }

        for (String futureVersion : new String[]{"2", "2.0", "2e0", "20e-1"}) {
            FdDecodeResult.UnsupportedNewer result = assertInstanceOf(
                    FdDecodeResult.UnsupportedNewer.class,
                    codec.decode(utf8(minimalDocument(futureVersion, "{}"))),
                    futureVersion);
            assertEquals(BigInteger.valueOf(2), result.declaredSchemaVersion(), futureVersion);
        }

        FdDecodeResult.Invalid fractional = invalid(
                codec.decode(utf8(minimalDocument("1.5", "{}"))));
        assertTrue(hasDiagnostic(fractional, FdCodecDiagnosticCode.INVALID_VALUE));
    }

    @Test
    void rejectsUnknownFieldsInCurrentV1Documents() throws Exception {
        String minimal = minimalDocument("1", "{}");
        int closingBrace = minimal.lastIndexOf('}');
        String topLevelUnknown = minimal.substring(0, closingBrace)
                + ",\n  \"futureField\": true\n"
                + minimal.substring(closingBrace);
        String sourceUnknown = minimal.replace(
                "\"className\": \"Minimal\",",
                "\"className\": \"Minimal\",\n"
                + "    \"futureField\": true,");
        String widgetUnknown = minimal.replace(
                "\"type\": \"flutter.widgets.Text\",",
                "\"type\": \"flutter.widgets.Text\",\n"
                + "    \"futureField\": true,");
        String valueUnknown = minimal.replace(
                "\"properties\": {},",
                "\"properties\": {\n"
                + "      \"data\": {\"kind\": \"string\", \"value\": \"x\", "
                + "\"futureField\": true}\n"
                + "    },");

        for (String unknown : new String[]{
            topLevelUnknown, sourceUnknown, widgetUnknown, valueUnknown
        }) {
            FdDecodeResult.Invalid result = invalid(codec.decode(utf8(unknown)));
            assertTrue(hasDiagnostic(result, FdCodecDiagnosticCode.UNKNOWN_FIELD),
                    () -> result.diagnostics().toString());
        }
    }

    @Test
    void distinguishesAnOmittedSlotFromAnExplicitlyEmptySingleSlot() throws Exception {
        DesignerDocument omitted = current(
                codec.decode(utf8(minimalDocument("1", "{}")))).document();
        DesignerDocument explicitNull = current(codec.decode(utf8(minimalDocument(
                "1",
                "{\"child\": {\"kind\": \"single\", \"child\": null}}")))).document();

        assertTrue(omitted.root().slots().isEmpty());
        WidgetSlot.SingleSlot empty = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                explicitNull.root().slots().get(new SlotName("child")));
        assertTrue(empty.child().isEmpty());
    }

    @Test
    void roundTripsEveryV1ValueAndOptionAndCanonicalizesDynamicData() throws Exception {
        byte[] source = resourceBytes(ALL_FEATURES_RESOURCE);
        FdDecodeResult.Current decoded = current(codec.decode(source));
        DesignerDocument document = decoded.document();

        Set<PropertyValueKind> actualKinds = document.root().properties().values().stream()
                .map(PropertyValue::kind)
                .collect(Collectors.toSet());
        assertEquals(EnumSet.allOf(PropertyValueKind.class), actualKinds);
        assertEquals(
                "Привіт, Flutter 🌍",
                assertInstanceOf(PropertyValue.StringValue.class,
                        property(document, "aString")).value());
        assertTrue(assertInstanceOf(PropertyValue.BooleanValue.class,
                property(document, "bBoolean")).value());
        assertEquals(
                new BigInteger("123456789012345678901234567890"),
                assertInstanceOf(PropertyValue.IntegerValue.class,
                        property(document, "cInteger")).value());
        assertEquals(
                new BigDecimal("1.23"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(document, "dDouble")).value());
        PropertyValue.EnumValue enumValue = assertInstanceOf(
                PropertyValue.EnumValue.class,
                property(document, "eEnum"));
        assertAll(
                () -> assertEquals("MainAxisAlignment", enumValue.type()),
                () -> assertEquals("spaceBetween", enumValue.value()));
        assertEquals(
                "0xFFAABBCC",
                assertInstanceOf(PropertyValue.ColorValue.class,
                        property(document, "fColor")).wireArgb());
        PropertyValue.EdgeInsetsValue insets = assertInstanceOf(
                PropertyValue.EdgeInsetsValue.class,
                property(document, "gInsets"));
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, insets.left()),
                () -> assertEquals(new BigDecimal("1E-7"), insets.top()),
                () -> assertEquals(new BigDecimal("1E+21"), insets.right()),
                () -> assertEquals(new BigDecimal("2.5"), insets.bottom()));
        assertEquals(
                "assets/images/logo.png",
                assertInstanceOf(PropertyValue.AssetValue.class,
                        property(document, "hAsset")).path());
        assertEquals(
                "_onPressed",
                assertInstanceOf(PropertyValue.CallbackValue.class,
                        property(document, "iCallback")).handler());
        assertEquals(
                "Theme.of(context).colorScheme.primary",
                assertInstanceOf(PropertyValue.DartExpressionValue.class,
                        property(document, "jExpression")).code());

        WidgetSlot.SingleSlot empty = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                document.root().slots().get(new SlotName("aEmpty")));
        WidgetSlot.ListSlot list = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                document.root().slots().get(new SlotName("zItems")));
        assertAll(
                () -> assertTrue(empty.child().isEmpty()),
                () -> assertEquals(2, list.children().size()),
                () -> assertEquals(WidgetClassKind.STATEFUL, document.source().widgetKind()),
                () -> assertEquals("feature_page.dart", document.source().dartFile()),
                () -> assertEquals("FeaturePage", document.source().className()),
                () -> assertEquals("0.1.3-test", document.source().generatorVersion().orElseThrow()),
                () -> assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                        document.source().managedRegions().imports().sha256()),
                () -> assertEquals("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
                        document.source().managedRegions().build().sha256()),
                () -> assertEquals("phone", document.canvas().orElseThrow().preset().orElseThrow()),
                () -> assertEquals(new BigDecimal("3.9E+2"),
                        document.canvas().orElseThrow().logicalWidth().orElseThrow()),
                () -> assertEquals(new BigDecimal("844"),
                        document.canvas().orElseThrow().logicalHeight().orElseThrow()),
                () -> assertEquals(new BigDecimal("2"),
                        document.canvas().orElseThrow().devicePixelRatio().orElseThrow()),
                () -> assertEquals(CanvasOrientation.LANDSCAPE,
                        document.canvas().orElseThrow().orientation().orElseThrow()),
                () -> assertEquals(DesignerThemeMode.DARK,
                        document.canvas().orElseThrow().themeMode().orElseThrow()),
                () -> assertEquals(new BigDecimal("1.25"),
                        document.canvas().orElseThrow().textScaleFactor().orElseThrow()),
                () -> assertEquals("uk-UA", document.canvas().orElseThrow().locale().orElseThrow()));
        assertEquals("urn:test:netbeans-flutter-designer:fd:1",
                document.schemaReference().orElseThrow());

        JsonValue.ObjectValue arbitrary = assertInstanceOf(
                JsonValue.ObjectValue.class,
                document.extensions().values().get(new ExtensionKey("z.example:data")));
        JsonValue.ArrayValue allJsonKinds = assertInstanceOf(
                JsonValue.ArrayValue.class,
                arbitrary.values().get("z-key"));
        assertAll(
                () -> assertInstanceOf(JsonValue.NullValue.class, allJsonKinds.values().get(0)),
                () -> assertInstanceOf(JsonValue.BooleanValue.class, allJsonKinds.values().get(1)),
                () -> assertInstanceOf(JsonValue.StringValue.class, allJsonKinds.values().get(2)),
                () -> assertInstanceOf(JsonValue.NumberValue.class, allJsonKinds.values().get(3)),
                () -> assertInstanceOf(JsonValue.ObjectValue.class, arbitrary.values().get("a-key")));

        byte[] canonical = codec.encode(document).copyBytes();
        String json = new String(canonical, StandardCharsets.UTF_8);
        assertAll(
                () -> assertFalse(startsWith(canonical, UTF_8_BOM)),
                () -> assertEquals('\n', canonical[canonical.length - 1]),
                () -> assertFalse(json.contains("\r")),
                () -> assertAppearsBefore(json, "\"aString\"", "\"bBoolean\""),
                () -> assertAppearsBefore(json, "\"bBoolean\"", "\"jExpression\""),
                () -> assertAppearsBefore(json, "\"aEmpty\"", "\"zItems\""),
                () -> assertAppearsBefore(json, "\"a.example:flag\"", "\"z.example:data\""),
                () -> assertAppearsBefore(json, "\"a.example:enabled\"", "\"z.example:widget\""),
                () -> assertAppearsBefore(json, "\"a-key\"", "\"z-key\""),
                () -> assertTrue(json.contains("\"logicalWidth\": 390")),
                () -> assertTrue(json.contains("\"value\": 1.23")),
                () -> assertTrue(json.contains("\"left\": 0")),
                () -> assertTrue(json.contains("\"top\": 1e-7")),
                () -> assertTrue(json.contains("\"right\": 1e21")),
                () -> assertTrue(json.contains("\"bottom\": 2.5")),
                () -> assertTrue(json.contains("\"number\": 123.45")));

        FdDecodeResult.Current roundTrip = current(codec.decode(canonical));
        assertEquals(document, roundTrip.document());
        assertArrayEquals(canonical, codec.encode(roundTrip.document()).copyBytes());
        assertArrayEquals(canonical, codec.encode(document).copyBytes());
    }

    private static PropertyValue property(DesignerDocument document, String name) {
        return document.root().properties().get(new PropertyName(name));
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

    private static void assertAppearsBefore(String json, String first, String second) {
        int firstIndex = json.indexOf(first);
        int secondIndex = json.indexOf(second);
        assertTrue(firstIndex >= 0, () -> "Missing " + first);
        assertTrue(secondIndex >= 0, () -> "Missing " + second);
        assertTrue(firstIndex < secondIndex,
                () -> first + " must precede " + second + " in canonical JSON");
    }

    private static String minimalDocument(String schemaVersion, String slots) {
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
                  "root": {
                    "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                    "type": "flutter.widgets.Text",
                    "properties": {},
                    "slots": %s
                  }
                }
                """.formatted(schemaVersion, slots);
    }

    private static byte[] replaceAscii(byte[] source, String oldValue, String newValue) {
        return new String(source, StandardCharsets.UTF_8)
                .replace(oldValue, newValue)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] resourceBytes(String name) throws IOException {
        try (InputStream input = FdDocumentCodecContractTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing test resource " + name);
            }
            return input.readAllBytes();
        }
    }

    private static Path findRepositoryFile(Path relativePath) {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Cannot locate repository file " + relativePath);
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }
}

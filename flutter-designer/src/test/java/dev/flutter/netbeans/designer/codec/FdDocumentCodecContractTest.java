package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.CanvasOrientation;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.designer.model.ExtensionKey;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdDocumentCodecContractTest {
    private static final Path GOLDEN_DOCUMENT =
            Path.of("docs", "flutter-designer", "examples", "home_page.fd");
    private static final String ALL_FEATURES_RESOURCE =
            "dev/flutter/netbeans/designer/codec/all-v1-features.fd";
    private static final String ALL_V2_FEATURES_RESOURCE =
            "dev/flutter/netbeans/designer/codec/all-v2-features.fd";
    private static final byte[] UTF_8_BOM = {
        (byte) 0xEF, (byte) 0xBB, (byte) 0xBF
    };

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void migratesTheDocumentedV1GoldenAndEncodesCanonicalV10() throws Exception {
        byte[] documented = Files.readAllBytes(findRepositoryFile(GOLDEN_DOCUMENT));

        FdDecodeResult.Current first = current(codec.decode(documented));
        assertAll(
                () -> assertEquals(1, first.sourceSchemaVersion()),
                () -> assertTrue(first.migrated()),
                () -> assertTrue(first.original().contentEquals(documented)),
                () -> assertEquals("../fd-v10.schema.json",
                        first.document().schemaReference().orElseThrow()),
                () -> assertEquals("home_page.dart", first.document().source().dartFile()),
                () -> assertEquals("HomePage", first.document().source().className()),
                () -> assertEquals("flutter.material.Scaffold", first.document().root().type().value()));

        OriginalFdBytes encoded = codec.encode(first.document());
        String encodedJson = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(encodedJson.contains("\"schemaVersion\": 10"));
        assertTrue(encodedJson.contains("\"$schema\": \"../fd-v10.schema.json\""));
        assertFalse(Arrays.equals(documented, encoded.copyBytes()));

        FdDecodeResult.Current roundTrip = current(codec.decode(encoded));
        assertFalse(roundTrip.migrated());
        assertEquals(10, roundTrip.sourceSchemaVersion());
        assertEquals(first.document(), roundTrip.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(roundTrip.document()).copyBytes(),
                "decode/encode must reach a stable fixed point");
        assertArrayEquals(encoded.copyBytes(), codec.encode(first.document()).copyBytes(),
                "repeated encoding of the same immutable model must be deterministic");
    }

    @Test
    void dispatchesACompleteFutureDocumentWithoutInterpretingItsBody() throws Exception {
        byte[] versionEleven = replaceAscii(
                Files.readAllBytes(findRepositoryFile(GOLDEN_DOCUMENT)),
                "\"schemaVersion\": 1",
                "\"schemaVersion\": 11");
        String futureJson = new String(versionEleven, StandardCharsets.UTF_8);
        int closingBrace = futureJson.lastIndexOf('}');
        byte[] future = utf8(futureJson.substring(0, closingBrace)
                + ",\n  \"futureOnly\": {\"newShape\": true}\n"
                + futureJson.substring(closingBrace));

        FdDecodeResult.UnsupportedNewer result = assertInstanceOf(
                FdDecodeResult.UnsupportedNewer.class,
                codec.decode(future));
        assertAll(
                () -> assertEquals(BigInteger.valueOf(11), result.declaredSchemaVersion()),
                () -> assertArrayEquals(future, result.original().copyBytes()));

        future[0] ^= 1;
        assertFalse(result.original().contentEquals(future),
                "UnsupportedNewer must retain its own exact immutable input snapshot");
    }

    @Test
    void malformedFutureInputIsInvalidRatherThanUnsupported() throws Exception {
        byte[] future = utf8(minimalDocument("11", "{}"));
        byte[] withTrailingGarbage = Arrays.copyOf(future, future.length + 1);
        withTrailingGarbage[withTrailingGarbage.length - 1] = 'x';

        FdDecodeResult.Invalid invalid = invalid(codec.decode(withTrailingGarbage));
        assertAll(
                () -> assertEquals(
                        BigInteger.valueOf(11),
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
        for (String migratedVersion : new String[]{"1", "1.0", "1e0", "10e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(1, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"2", "2.0", "2e0", "20e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(2, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"3", "3.0", "3e0", "30e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(3, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"4", "4.0", "4e0", "40e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(4, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"5", "5.0", "5e0", "50e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(5, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"6", "6.0", "6e0", "60e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(6, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"7", "7.0", "7e0", "70e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(7, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"8", "8.0", "8e0", "80e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(8, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String migratedVersion : new String[]{"9", "9.0", "9e0", "90e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(migratedVersion, "{}"))));
            assertEquals(9, result.sourceSchemaVersion(), migratedVersion);
            assertTrue(result.migrated(), migratedVersion);
        }

        for (String currentVersion : new String[]{"10", "10.0", "10e0", "100e-1"}) {
            FdDecodeResult.Current result = current(
                    codec.decode(utf8(minimalDocument(currentVersion, "{}"))));
            assertEquals(10, result.sourceSchemaVersion(), currentVersion);
            assertFalse(result.migrated(), currentVersion);
        }

        for (String futureVersion : new String[]{"11", "11.0", "11e0", "110e-1"}) {
            FdDecodeResult.UnsupportedNewer result = assertInstanceOf(
                    FdDecodeResult.UnsupportedNewer.class,
                    codec.decode(utf8(minimalDocument(futureVersion, "{}"))),
                    futureVersion);
            assertEquals(BigInteger.valueOf(11), result.declaredSchemaVersion(), futureVersion);
        }

        FdDecodeResult.Invalid fractional = invalid(
                codec.decode(utf8(minimalDocument("1.5", "{}"))));
        assertTrue(hasDiagnostic(fractional, FdCodecDiagnosticCode.INVALID_VALUE));
    }

    @Test
    void migratesKnownOlderSchemaReferencesToV10() throws Exception {
        String canonical = minimalDocument("1", "{}").replace(
                "{\n  \"format\"",
                "{\n  \"$schema\": \"urn:netbeans-flutter-designer:schema:fd:1\",\n"
                + "  \"format\"");
        FdDecodeResult.Current migrated = current(codec.decode(utf8(canonical)));
        assertEquals(
                "urn:netbeans-flutter-designer:schema:fd:10",
                migrated.document().schemaReference().orElseThrow());
        String encoded = new String(
                codec.encode(migrated.document()).copyBytes(), StandardCharsets.UTF_8);
        assertTrue(encoded.contains(
                "\"$schema\": \"urn:netbeans-flutter-designer:schema:fd:10\""));
        assertTrue(encoded.contains("\"schemaVersion\": 10"));

        String arbitrary = canonical.replace(
                "urn:netbeans-flutter-designer:schema:fd:1",
                "urn:example:custom-schema:1");
        assertEquals(
                "urn:example:custom-schema:1",
                current(codec.decode(utf8(arbitrary)))
                        .document().schemaReference().orElseThrow());
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
    void limitedBoxFiniteDimensionsAndOptionalChildRoundTripByteExactlyInV7()
            throws Exception {
        String childSlot = """
                {
                  "child": {
                    "kind": "single",
                    "child": {
                      "id": "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                      "type": "flutter.widgets.Text",
                      "properties": {
                        "data": {"kind": "string", "value": "Codec child"}
                      },
                      "slots": {}
                    }
                  }
                }
                """;
        String source = minimalDocument("7", childSlot)
                .replaceFirst(
                        "\"type\": \"flutter\\.widgets\\.Text\"",
                        "\"type\": \"flutter.widgets.LimitedBox\"")
                .replace(
                        "\"properties\": {},",
                        "\"properties\": {\n"
                        + "      \"maxWidth\": {\"kind\": \"double\", "
                        + "\"value\": 320.5},\n"
                        + "      \"maxHeight\": {\"kind\": \"double\", "
                        + "\"value\": 180.25}\n"
                        + "    },");

        FdDecodeResult.Current decoded = current(codec.decode(utf8(source)));

        assertEquals(7, decoded.sourceSchemaVersion());
        assertTrue(decoded.migrated());
        assertEquals("flutter.widgets.LimitedBox",
                decoded.document().root().type().value());
        assertEquals(new BigDecimal("320.5"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "maxWidth")).value());
        assertEquals(new BigDecimal("180.25"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "maxHeight")).value());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                decoded.document().root().slots().get(new SlotName("child")));
        assertEquals("flutter.widgets.Text",
                child.child().orElseThrow().type().value());

        OriginalFdBytes encoded = codec.encode(decoded.document());
        String canonical = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"schemaVersion\": 10"), canonical);
        assertTrue(canonical.contains(
                "\"type\": \"flutter.widgets.LimitedBox\""), canonical);
        FdDecodeResult.Current reopened = current(codec.decode(encoded));
        assertEquals(decoded.document(), reopened.document());
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(reopened.document()).copyBytes());
    }

    @Test
    void overflowBoxTypedPropertiesAndOptionalChildRoundTripByteExactlyInV7()
            throws Exception {
        String childSlot = """
                {
                  "child": {
                    "kind": "single",
                    "child": {
                      "id": "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                      "type": "flutter.widgets.Text",
                      "properties": {
                        "data": {"kind": "string", "value": "Overflow codec child"}
                      },
                      "slots": {}
                    }
                  }
                }
                """;
        String source = minimalDocument("7", childSlot)
                .replaceFirst(
                        "\"type\": \"flutter\\.widgets\\.Text\"",
                        "\"type\": \"flutter.widgets.OverflowBox\"")
                .replace(
                        "\"properties\": {},",
                        "\"properties\": {\n"
                        + "      \"alignment\": {\"kind\": \"alignmentGeometry\", "
                        + "\"basis\": \"directional\", \"horizontal\": 0.75, "
                        + "\"vertical\": -0.25},\n"
                        + "      \"minWidth\": {\"kind\": \"double\", "
                        + "\"value\": 32.5},\n"
                        + "      \"maxWidth\": {\"kind\": \"double\", "
                        + "\"value\": 640.25},\n"
                        + "      \"minHeight\": {\"kind\": \"double\", "
                        + "\"value\": 24},\n"
                        + "      \"maxHeight\": {\"kind\": \"double\", "
                        + "\"value\": 420.5},\n"
                        + "      \"fit\": {\"kind\": \"enum\", "
                        + "\"type\": \"OverflowBoxFit\", "
                        + "\"value\": \"deferToChild\"}\n"
                        + "    },");

        FdDecodeResult.Current decoded = current(codec.decode(utf8(source)));

        assertEquals(7, decoded.sourceSchemaVersion());
        assertTrue(decoded.migrated());
        assertEquals("flutter.widgets.OverflowBox",
                decoded.document().root().type().value());
        PropertyValue.AlignmentGeometryValue alignment = assertInstanceOf(
                PropertyValue.AlignmentGeometryValue.class,
                property(decoded.document(), "alignment"));
        assertEquals(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                alignment.basis());
        assertEquals(new BigDecimal("0.75"), alignment.horizontal());
        assertEquals(new BigDecimal("-0.25"), alignment.vertical());
        assertEquals(new BigDecimal("32.5"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "minWidth")).value());
        assertEquals(new BigDecimal("640.25"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "maxWidth")).value());
        assertEquals(new BigDecimal("24"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "minHeight")).value());
        assertEquals(new BigDecimal("420.5"),
                assertInstanceOf(PropertyValue.DoubleValue.class,
                        property(decoded.document(), "maxHeight")).value());
        assertEquals(new PropertyValue.EnumValue(
                        "OverflowBoxFit", "deferToChild"),
                property(decoded.document(), "fit"));
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                decoded.document().root().slots().get(new SlotName("child")));
        assertEquals("flutter.widgets.Text",
                child.child().orElseThrow().type().value());

        OriginalFdBytes encoded = codec.encode(decoded.document());
        String canonical = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"schemaVersion\": 10"), canonical);
        assertTrue(canonical.contains(
                "\"type\": \"flutter.widgets.OverflowBox\""), canonical);
        assertTrue(canonical.contains("\"basis\": \"directional\""), canonical);
        FdDecodeResult.Current reopened = current(codec.decode(encoded));
        assertEquals(decoded.document(), reopened.document());
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(reopened.document()).copyBytes());
    }

    @Test
    void roundTripsEveryV1ValueAndOptionAndCanonicalizesDynamicData() throws Exception {
        byte[] source = resourceBytes(ALL_FEATURES_RESOURCE);
        FdDecodeResult.Current decoded = current(codec.decode(source));
        DesignerDocument document = decoded.document();

        Set<PropertyValueKind> actualKinds = document.root().properties().values().stream()
                .map(PropertyValue::kind)
                .collect(Collectors.toSet());
        assertEquals(EnumSet.of(
                PropertyValueKind.STRING,
                PropertyValueKind.BOOLEAN,
                PropertyValueKind.INTEGER,
                PropertyValueKind.DOUBLE,
                PropertyValueKind.ENUM,
                PropertyValueKind.COLOR,
                PropertyValueKind.EDGE_INSETS,
                PropertyValueKind.ASSET,
                PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_EXPRESSION), actualKinds);
        assertEquals(1, decoded.sourceSchemaVersion());
        assertTrue(decoded.migrated());
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

    @Test
    void migratesEveryStructuredV2ValueToCanonicalV4() throws Exception {
        byte[] source = resourceBytes(ALL_V2_FEATURES_RESOURCE);
        FdDecodeResult.Current decoded = current(codec.decode(source));
        DesignerDocument document = decoded.document();

        assertEquals(2, decoded.sourceSchemaVersion());
        assertTrue(decoded.migrated());
        assertEquals(Set.of(
                PropertyValueKind.THEME_TOKEN,
                PropertyValueKind.PAINT,
                PropertyValueKind.SHADOW_LIST,
                PropertyValueKind.FONT_FEATURE_LIST,
                PropertyValueKind.FONT_VARIATION_LIST),
                document.root().properties().values().stream()
                        .map(PropertyValue::kind)
                        .collect(Collectors.toSet()));

        PropertyValue.PaintValue paint = assertInstanceOf(
                PropertyValue.PaintValue.class, property(document, "bPaint"));
        assertAll(
                () -> assertEquals(PropertyValue.PaintValue.BlendMode.MULTIPLY,
                        paint.blendMode()),
                () -> assertEquals(new BigDecimal("2.5"), paint.strokeWidth()),
                () -> assertEquals("0xFFAABBCC",
                        assertInstanceOf(ColorSource.Literal.class, paint.color()).wireArgb()),
                () -> assertEquals(new BigDecimal("3.5"),
                        paint.maskFilter().orElseThrow().sigma()));
        PropertyValue.ShadowListValue shadows = assertInstanceOf(
                PropertyValue.ShadowListValue.class, property(document, "cShadows"));
        assertEquals(
                "material.colorScheme.shadow",
                assertInstanceOf(ColorSource.Theme.class,
                        shadows.items().getFirst().color()).token().wireId());
        assertEquals("liga", assertInstanceOf(
                PropertyValue.FontFeatureListValue.class,
                property(document, "dFeatures")).items().getFirst().tag());
        assertEquals(new BigDecimal("7E+2"), assertInstanceOf(
                PropertyValue.FontVariationListValue.class,
                property(document, "eVariations")).items().getFirst().value());

        OriginalFdBytes encoded = codec.encode(document);
        assertFalse(Arrays.equals(source, encoded.copyBytes()));
        assertTrue(new String(encoded.copyBytes(), StandardCharsets.UTF_8)
                .contains("\"schemaVersion\": 10"));
        FdDecodeResult.Current roundTrip = current(codec.decode(encoded));
        assertEquals(document, roundTrip.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(roundTrip.document()).copyBytes());
    }

    @Test
    void roundTripsDirectionalEdgeInsetsOnlyInSchemaV3() throws Exception {
        String directional = minimalDocument("3", "{}").replace(
                "\"properties\": {}",
                "\"properties\": {\"padding\": {\"kind\": \"edgeInsets\", "
                + "\"start\": 1, \"top\": 2, \"end\": 3, \"bottom\": 4}}" );

        FdDecodeResult.Current decoded = current(codec.decode(utf8(directional)));
        PropertyValue.EdgeInsetsDirectionalValue value = assertInstanceOf(
                PropertyValue.EdgeInsetsDirectionalValue.class,
                property(decoded.document(), "padding"));
        assertEquals(BigDecimal.ONE, value.start());
        assertEquals(BigDecimal.valueOf(3), value.end());

        byte[] encoded = codec.encode(decoded.document()).copyBytes();
        String canonical = new String(encoded, StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"start\": 1"));
        assertTrue(canonical.contains("\"end\": 3"));
        assertFalse(canonical.contains("\"left\""));
        assertArrayEquals(encoded, codec.encode(
                current(codec.decode(encoded)).document()).copyBytes());

        String mislabeledV2 = directional.replace(
                "\"schemaVersion\": 3", "\"schemaVersion\": 2");
        FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(mislabeledV2)));
        assertTrue(hasDiagnostic(invalid, FdCodecDiagnosticCode.INVALID_VALUE));
    }

    @Test
    void roundTripsTextFieldUsingOnlyExistingV6ScalarKinds() throws Exception {
        String textField = minimalDocument("6", "{}").replace(
                "\"type\": \"flutter.widgets.Text\"",
                "\"type\": \"flutter.material.TextField\"").replace(
                "\"properties\": {}",
                "\"properties\": {"
                + "\"keyboardType\": {\"kind\": \"string\", \"value\": \"numberSignedDecimal\"},"
                + "\"obscuringCharacter\": {\"kind\": \"string\", \"value\": \"\\u2022\"},"
                + "\"maxLines\": {\"kind\": \"integer\", \"value\": 3},"
                + "\"cursorWidth\": {\"kind\": \"double\", \"value\": 2.5},"
                + "\"clipBehavior\": {\"kind\": \"enum\", \"type\": \"Clip\", "
                + "\"value\": \"antiAlias\"},"
                + "\"onChanged\": {\"kind\": \"callback\", "
                + "\"handler\": \"handleChanged\"}}" );

        FdDecodeResult.Current decoded = current(codec.decode(utf8(textField)));
        assertEquals("flutter.material.TextField", decoded.document().root().type().value());
        assertEquals(new PropertyValue.StringValue("numberSignedDecimal"),
                property(decoded.document(), "keyboardType"));
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(3)),
                property(decoded.document(), "maxLines"));
        assertEquals(new PropertyValue.CallbackValue("handleChanged"),
                property(decoded.document(), "onChanged"));

        OriginalFdBytes encoded = codec.encode(decoded.document());
        String canonical = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"schemaVersion\": 10"));
        assertFalse(canonical.contains("textFieldValue"));
        FdDecodeResult.Current roundTrip = current(codec.decode(encoded));
        assertEquals(decoded.document(), roundTrip.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(roundTrip.document()).copyBytes());
    }

    @Test
    void roundTripsCanonicalTypedIconDataIncludingExplicitNull() throws Exception {
        String iconJson = "{\"kind\":\"iconData\","
                + "\"codePoint\":128640,"
                + "\"fontFamily\":\"Custom Icons\","
                + "\"fontPackage\":\"my_icons\","
                + "\"matchTextDirection\":true,"
                + "\"fontFamilyFallback\":[\"Fallback One\",\"Fallback Two\"]}";
        String source = minimalDocument("4", "{}").replace(
                "\"properties\": {}",
                "\"properties\": {\"icon\":" + iconJson + "}");
        FdDecodeResult.Current decoded = current(codec.decode(utf8(source)));
        PropertyValue.IconDataValue icon = assertInstanceOf(
                PropertyValue.IconDataValue.class,
                property(decoded.document(), "icon"));
        assertEquals(0x1F680, icon.codePoint().orElseThrow());
        assertEquals("Custom Icons", icon.fontFamily().orElseThrow());
        assertEquals("my_icons", icon.fontPackage().orElseThrow());
        assertTrue(icon.matchTextDirection());
        assertEquals(List.of("Fallback One", "Fallback Two"),
                icon.fontFamilyFallback());

        byte[] canonical = codec.encode(decoded.document()).copyBytes();
        String json = new String(canonical, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"kind\": \"iconData\",\n"
                + "        \"codePoint\": 128640,\n"
                + "        \"fontFamily\": \"Custom Icons\",\n"
                + "        \"fontPackage\": \"my_icons\",\n"
                + "        \"matchTextDirection\": true,\n"
                + "        \"fontFamilyFallback\": ["));
        assertArrayEquals(canonical, codec.encode(
                current(codec.decode(canonical)).document()).copyBytes());

        String none = source.replace(iconJson,
                "{\"kind\":\"iconData\",\"codePoint\":null,"
                + "\"fontFamily\":null,\"fontPackage\":null,"
                + "\"matchTextDirection\":false,\"fontFamilyFallback\":[]}");
        PropertyValue.IconDataValue empty = assertInstanceOf(
                PropertyValue.IconDataValue.class,
                property(current(codec.decode(utf8(none))).document(), "icon"));
        assertEquals(PropertyValue.IconDataValue.none(), empty);
    }

    @Test
    void roundTripsFlattenedAppBarValuesWithoutANewWireKind() throws Exception {
        String source = minimalDocument("4", "{}").replace(
                "\"type\": \"flutter.widgets.Text\"",
                "\"type\": \"flutter.material.AppBar\"").replace(
                "\"properties\": {}",
                "\"properties\": {"
                + "\"notificationPredicate\":{\"kind\":\"string\",\"value\":\"depthZero\"},"
                + "\"shapeKind\":{\"kind\":\"string\",\"value\":\"roundedRectangle\"},"
                + "\"shapeRadiusTopLeft\":{\"kind\":\"double\",\"value\":12.5},"
                + "\"actionsPadding\":{\"kind\":\"edgeInsets\","
                + "\"left\":1,\"top\":2,\"right\":3,\"bottom\":4},"
                + "\"iconThemeShadows\":{\"kind\":\"shadowList\",\"items\":[]}"
                + "}");

        FdDecodeResult.Current decoded = current(codec.decode(utf8(source)));
        assertEquals(new PropertyValue.StringValue("depthZero"),
                property(decoded.document(), "notificationPredicate"));
        assertEquals(new BigDecimal("12.5"), assertInstanceOf(
                PropertyValue.DoubleValue.class,
                property(decoded.document(), "shapeRadiusTopLeft")).value());
        assertTrue(assertInstanceOf(
                PropertyValue.ShadowListValue.class,
                property(decoded.document(), "iconThemeShadows")).items().isEmpty());

        byte[] canonical = codec.encode(decoded.document()).copyBytes();
        FdDecodeResult.Current roundTrip = current(codec.decode(canonical));
        assertEquals(decoded.document(), roundTrip.document());
        assertArrayEquals(canonical, codec.encode(roundTrip.document()).copyBytes());
    }

    @Test
    void migratesEveryExactRegisteredLegacyIconAndLeavesOtherExpressionsOpaque()
            throws Exception {
        String legacy = minimalDocument("3", "{}")
                .replace("flutter.widgets.Text", "flutter.widgets.Icon")
                .replace(
                        "\"properties\": {}",
                        "\"properties\": {\"icon\":{\"kind\":\"dartExpression\","
                        + "\"code\":\"Icons.star\"}}");

        FdDecodeResult.Current migrated = current(codec.decode(utf8(legacy)));
        assertEquals(3, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        PropertyValue.IconDataValue star = assertInstanceOf(
                PropertyValue.IconDataValue.class,
                property(migrated.document(), "icon"));
        assertEquals(0xE5F9, star.codePoint().orElseThrow());
        assertEquals("MaterialIcons", star.fontFamily().orElseThrow());
        assertTrue(star.fontPackage().isEmpty());
        assertFalse(star.matchTextDirection());
        assertTrue(star.fontFamilyFallback().isEmpty());

        String canonical = new String(
                codec.encode(migrated.document()).copyBytes(), StandardCharsets.UTF_8);
        assertTrue(canonical.contains("\"schemaVersion\": 10"), canonical);
        assertTrue(canonical.contains("\"kind\": \"iconData\""), canonical);
        assertFalse(canonical.contains("Icons.star"), canonical);

        String favoriteLegacy = legacy.replace("Icons.star", "Icons.favorite");
        PropertyValue.IconDataValue favorite = assertInstanceOf(
                PropertyValue.IconDataValue.class,
                property(current(codec.decode(utf8(favoriteLegacy))).document(), "icon"));
        assertEquals(0xE25B, favorite.codePoint().orElseThrow());
        assertFalse(favorite.matchTextDirection());

        String rtlLegacy = legacy.replace("Icons.star", "Icons.arrow_back");
        PropertyValue.IconDataValue arrowBack = assertInstanceOf(
                PropertyValue.IconDataValue.class,
                property(current(codec.decode(utf8(rtlLegacy))).document(), "icon"));
        assertEquals(0xE092, arrowBack.codePoint().orElseThrow());
        assertTrue(arrowBack.matchTextDirection());

        String arbitrary = legacy.replace("Icons.star", "Icons.not_in_locked_registry");
        PropertyValue.DartExpressionValue untouched = assertInstanceOf(
                PropertyValue.DartExpressionValue.class,
                property(current(codec.decode(utf8(arbitrary))).document(), "icon"));
        assertEquals("Icons.not_in_locked_registry", untouched.code());

        String invocation = legacy.replace("Icons.star", "Icons.favorite.toString()");
        assertInstanceOf(PropertyValue.DartExpressionValue.class,
                property(current(codec.decode(utf8(invocation))).document(), "icon"));
    }

    @Test
    void rejectsMalformedOrUnsafeTypedIconData() throws Exception {
        String base = minimalDocument("4", "{}");
        String valid = "{\"kind\":\"iconData\",\"codePoint\":58873,"
                + "\"fontFamily\":\"MaterialIcons\",\"fontPackage\":null,"
                + "\"matchTextDirection\":false,\"fontFamilyFallback\":[]}";
        List<String> invalidValues = List.of(
                valid.replace("\"codePoint\":58873,", ""),
                valid.replace("58873", "55296"),
                valid.replace("58873", "1114112"),
                valid.replace("\"MaterialIcons\"", "\"Bad\\nFamily\""),
                valid.replace("\"MaterialIcons\"", "\"A\\u202eB\""),
                valid.replace("\"fontPackage\":null",
                        "\"fontFamily\":null,\"fontPackage\":\"pkg\"")
                        .replace("\"fontFamily\":\"MaterialIcons\",", ""),
                valid.replace("\"codePoint\":58873",
                        "\"codePoint\":null")
                        .replace("\"fontFamily\":\"MaterialIcons\"",
                                "\"fontFamily\":\"Illegal metadata\""),
                valid.replace("\"fontFamilyFallback\":[]",
                        "\"fontFamilyFallback\":[\"A\",\"A\"]"),
                valid.replace("\"fontFamilyFallback\":[]",
                        "\"fontFamilyFallback\":{}"),
                valid.replace("}", ",\"expression\":\"Icons.star\"}"));

        for (String invalidValue : invalidValues) {
            String document = base.replace(
                    "\"properties\": {}",
                    "\"properties\": {\"icon\":" + invalidValue + "}");
            assertInstanceOf(FdDecodeResult.Invalid.class,
                    codec.decode(utf8(document)), invalidValue);
        }

        String mislabeledV3 = minimalDocument("3", "{}").replace(
                "\"properties\": {}",
                "\"properties\": {\"icon\":" + valid + "}");
        FdDecodeResult.Invalid versionFailure = invalid(
                codec.decode(utf8(mislabeledV3)));
        assertTrue(versionFailure.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().equals("/root/properties/icon/kind")
                && diagnostic.message().contains("schema version 4")),
                () -> versionFailure.diagnostics().toString());
    }

    @Test
    void rejectsAmbiguousOrIncompleteSchemaV3EdgeInsetsShapes() throws Exception {
        String base = minimalDocument("3", "{}");
        String[] invalidValues = {
            // Physical and directional coordinates may never be mixed.
            "{\"kind\":\"edgeInsets\",\"left\":1,\"top\":2,"
                    + "\"right\":3,\"bottom\":4,\"start\":5,\"end\":6}",
            // Every semantic side is required.
            "{\"kind\":\"edgeInsets\",\"start\":1,\"top\":2,\"bottom\":4}",
            "{\"kind\":\"edgeInsets\",\"left\":1,\"top\":2,\"bottom\":4}",
            // Unknown leaves are rejected rather than silently ignored.
            "{\"kind\":\"edgeInsets\",\"start\":1,\"top\":2,"
                    + "\"end\":3,\"bottom\":4,\"horizontal\":5}"
        };

        for (String invalidValue : invalidValues) {
            String document = base.replace(
                    "\"properties\": {}",
                    "\"properties\": {\"padding\":" + invalidValue + "}");
            FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(document)));
            assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                    diagnostic.code() == FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD
                    || diagnostic.code() == FdCodecDiagnosticCode.UNKNOWN_FIELD),
                    () -> invalid.diagnostics().toString());
        }
    }

    @Test
    void rejectsMalformedComplexValuesAndV2KindsInsideV1() throws Exception {
        String valid = new String(resourceBytes(ALL_V2_FEATURES_RESOURCE), StandardCharsets.UTF_8);
        String[] invalidDocuments = {
            valid.replace("material.textTheme.bodyMedium", "material.textTheme.BodyMedium"),
            valid.replace("material.colorScheme.shadow", "material.textTheme.bodyMedium"),
            valid.replace("\"strokeWidth\": 2.5", "\"strokeWidth\": -1"),
            valid.replace("\"strokeWidth\": 2.5", "\"strokeWidth\": 1E+400"),
            valid.replace("\"strokeMiterLimit\": 4", "\"strokeMiterLimit\": 1E-400"),
            valid.replace("\"sigma\": 3.5", "\"sigma\": 0"),
            valid.replace(
                    "\"sigma\": 3.5",
                    "\"sigma\": 1.234567890123456789"),
            valid.replace("\"offsetX\": -1", "\"offsetX\": 1E+400"),
            valid.replace("\"offsetY\": 2", "\"offsetY\": 1E-400"),
            valid.replace("\"blurRadius\": 3", "\"blurRadius\": -1"),
            valid.replace(
                    "\"blurRadius\": 3",
                    "\"blurRadius\": 1.234567890123456789"),
            valid.replace("\"tag\": \"liga\"", "\"tag\": \"lig\""),
            valid.replace("\"value\": 1", "\"value\": 2147483648"),
            valid.replace("\"value\": 700", "\"value\": 1001"),
            valid.replace(
                    "\"value\": 700",
                    "\"value\": 700.000000000000000001"),
            valid.replace("\"axis\": \"wght\"", "\"axis\": \"badÇ\""),
            valid.replace(
                    "\"invertColors\": false,",
                    "\"invertColors\": false,\n        \"shader\": \"raw\",")
        };
        for (String invalidDocument : invalidDocuments) {
            FdDecodeResult.Invalid invalid = invalid(codec.decode(utf8(invalidDocument)));
            assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                    diagnostic.code() == FdCodecDiagnosticCode.INVALID_VALUE
                    || diagnostic.code() == FdCodecDiagnosticCode.UNKNOWN_FIELD),
                    () -> invalid.diagnostics().toString());
        }

        String v1WithThemeToken = minimalDocument("1", "{}").replace(
                "\"properties\": {},",
                "\"properties\": {\n"
                + "      \"style\": {\"kind\": \"themeToken\", "
                + "\"token\": \"material.textTheme.bodyMedium\"}\n"
                + "    },");
        FdDecodeResult.Invalid invalidV1 = invalid(codec.decode(utf8(v1WithThemeToken)));
        assertTrue(hasDiagnostic(invalidV1, FdCodecDiagnosticCode.INVALID_VALUE));
    }

    @Test
    void canonicalWriterRejectsEveryNonRepresentableComplexNumericLeaf()
            throws Exception {
        DesignerDocument base = current(codec.decode(
                resourceBytes(ALL_V2_FEATURES_RESOURCE))).document();
        BigDecimal overflow = new BigDecimal("1E+400");
        BigDecimal underflow = new BigDecimal("1E-400");
        BigDecimal excessPrecision = new BigDecimal("1.234567890123456789");

        assertEncodeRejects(base, "bPaint",
                paint(overflow, BigDecimal.valueOf(4), BigDecimal.ONE),
                "/strokeWidth");
        assertEncodeRejects(base, "bPaint",
                paint(BigDecimal.ZERO, underflow, BigDecimal.ONE),
                "/strokeMiterLimit");
        assertEncodeRejects(base, "bPaint",
                paint(BigDecimal.ZERO, BigDecimal.valueOf(4), excessPrecision),
                "/maskFilter/sigma");
        assertEncodeRejects(base, "cShadows",
                shadows(overflow, BigDecimal.ZERO, BigDecimal.ZERO),
                "/offsetX");
        assertEncodeRejects(base, "cShadows",
                shadows(BigDecimal.ZERO, underflow, BigDecimal.ZERO),
                "/offsetY");
        assertEncodeRejects(base, "cShadows",
                shadows(BigDecimal.ZERO, BigDecimal.ZERO, excessPrecision),
                "/blurRadius");
        assertEncodeRejects(base, "eVariations", variations(underflow), "/value");
        assertEncodeRejects(base, "eVariations", variations(excessPrecision), "/value");
    }

    private static PropertyValue property(DesignerDocument document, String name) {
        return document.root().properties().get(new PropertyName(name));
    }

    private void assertEncodeRejects(
            DesignerDocument base,
            String propertyName,
            PropertyValue value,
            String pointerSuffix) {
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(base.root().properties());
        properties.put(new PropertyName(propertyName), value);
        WidgetNode root = new WidgetNode(
                base.root().id(),
                base.root().type(),
                properties,
                base.root().slots(),
                base.root().extensions());
        DesignerDocument document = new DesignerDocument(
                base.schemaReference(),
                base.documentId(),
                base.source(),
                base.canvas(),
                root,
                base.extensions());

        FdEncodeException failure = assertThrows(
                FdEncodeException.class, () -> codec.encode(document));
        assertEquals(FdCodecDiagnosticCode.INVALID_VALUE,
                failure.diagnostic().code());
        assertTrue(failure.diagnostic().pointer().endsWith(pointerSuffix),
                () -> failure.diagnostic().toString());
    }

    private static PropertyValue.PaintValue paint(
            BigDecimal strokeWidth,
            BigDecimal strokeMiterLimit,
            BigDecimal sigma) {
        return new PropertyValue.PaintValue(
                new ColorSource.Literal(0xFF112233L),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.FILL,
                strokeWidth,
                PropertyValue.PaintValue.StrokeCap.BUTT,
                PropertyValue.PaintValue.StrokeJoin.MITER,
                strokeMiterLimit,
                true,
                PropertyValue.PaintValue.FilterQuality.NONE,
                false,
                Optional.of(new PropertyValue.PaintValue.BlurMask(
                        PropertyValue.PaintValue.BlurStyle.NORMAL, sigma)));
    }

    private static PropertyValue.ShadowListValue shadows(
            BigDecimal offsetX,
            BigDecimal offsetY,
            BigDecimal blurRadius) {
        return new PropertyValue.ShadowListValue(List.of(
                new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("42d383f3-6054-426d-822e-c5eb8f8ae304"),
                        new ColorSource.Literal(0xFF112233L),
                        offsetX,
                        offsetY,
                        blurRadius)));
    }

    private static PropertyValue.FontVariationListValue variations(BigDecimal value) {
        return new PropertyValue.FontVariationListValue(List.of(
                new PropertyValue.FontVariationListValue.FontVariation(
                        StableId.parse("16c65ef9-0791-49f1-9499-c2f6e626e94f"),
                        "GRAD",
                        value)));
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

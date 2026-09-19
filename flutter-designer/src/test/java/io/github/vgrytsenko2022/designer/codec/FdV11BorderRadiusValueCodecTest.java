package io.github.vgrytsenko2022.designer.codec;

import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdV11BorderRadiusValueCodecTest {
    private static final PropertyName DIRECTIONAL = new PropertyName("directionalRadius");
    private static final PropertyName PHYSICAL = new PropertyName("physicalRadius");
    private static final PropertyName CLIP_RADIUS = new PropertyName("clipRadius");

    private static final String PHYSICAL_GEOMETRY_JSON = """
            {
              "kind": "physical",
              "topLeft": {"x": 1, "y": 2},
              "topRight": {"x": 3, "y": 4},
              "bottomRight": {"x": 5, "y": 6},
              "bottomLeft": {"x": 7, "y": 8}
            }
            """.strip();

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsPhysicalAndDirectionalGeometryInCanonicalOrder() throws Exception {
        PropertyValue.BorderRadiusValue physical = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius("1.0", "2.00"),
                        radius("3", "4"),
                        radius("5", "6"),
                        radius("7", "8")));
        PropertyValue.BorderRadiusValue directional = new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        radius("9", "10"),
                        radius("11", "12"),
                        radius("14", "14"),
                        radius("15", "16")));
        OriginalFdBytes encoded = codec.encode(document(Map.of(
                PHYSICAL, physical,
                DIRECTIONAL, directional)));
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 17"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v17.schema.json\""), json);
        assertCanonicalOrder(
                json,
                "\"directionalRadius\"",
                "\"physicalRadius\"",
                "\"kind\": \"borderRadius\"",
                "\"geometry\"",
                "\"kind\": \"directional\"",
                "\"topStart\"",
                "\"topEnd\"",
                "\"bottomEnd\"",
                "\"bottomStart\"");
        assertCanonicalOrder(
                json,
                "\"physicalRadius\"",
                "\"slots\"",
                "\"kind\": \"borderRadius\"",
                "\"geometry\"",
                "\"kind\": \"physical\"",
                "\"topLeft\"",
                "\"topRight\"",
                "\"bottomRight\"",
                "\"bottomLeft\"");
        assertRadiusFieldOrder(json, "\"topLeft\"", "\"topRight\"");
        assertRadiusFieldOrder(json, "\"topStart\"", "\"topEnd\"");

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(17, decoded.sourceSchemaVersion());
        assertFalse(decoded.migrated());
        assertEquals(directional, decoded.document().root().properties().get(DIRECTIONAL));
        assertEquals(physical, decoded.document().root().properties().get(PHYSICAL));
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void rejectsBorderRadiusBeforeV11() throws Exception {
        String current = new String(codec.encode(document(Map.of(
                CLIP_RADIUS,
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                radius("1", "2"), radius("3", "4"),
                                radius("5", "6"), radius("7", "8"))))))
                .copyBytes(), StandardCharsets.UTF_8);
        String v10 = current
                .replace("\"schemaVersion\": 17", "\"schemaVersion\": 10")
                .replace("../fd-v17.schema.json", "../fd-v10.schema.json");

        assertInvalid(
                v10,
                FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipRadius/kind");
    }

    @Test
    void rejectsMissingUnknownAndMalformedFields() throws Exception {
        assertInvalid(
                propertyDocument("{\"kind\":\"borderRadius\"}"),
                FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                "/root/properties/clipRadius/geometry");
        assertInvalid(
                propertyDocument("""
                        {"kind":"borderRadius","geometry":%s,"future":true}
                        """.formatted(PHYSICAL_GEOMETRY_JSON)),
                FdCodecDiagnosticCode.UNKNOWN_FIELD,
                "/root/properties/clipRadius/future");
        assertInvalid(
                propertyDocument("{\"kind\":\"borderRadius\",\"geometry\":[]}"),
                FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                "/root/properties/clipRadius/geometry");
        assertInvalid(
                propertyDocument("""
                        {"kind":"borderRadius","geometry":{"kind":"rounded"}}
                        """),
                FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipRadius/geometry/kind");
        assertInvalid(
                propertyDocument("""
                        {"kind":"borderRadius","geometry":{
                          "kind":"physical",
                          "topLeft":{"x":1},
                          "topRight":{"x":3,"y":4},
                          "bottomRight":{"x":5,"y":6},
                          "bottomLeft":{"x":7,"y":8}
                        }}
                        """),
                FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                "/root/properties/clipRadius/geometry/topLeft/y");
        assertInvalid(
                propertyDocument("""
                        {"kind":"borderRadius","geometry":{
                          "kind":"physical",
                          "topLeft":{"x":1,"y":2,"z":3},
                          "topRight":{"x":3,"y":4},
                          "bottomRight":{"x":5,"y":6},
                          "bottomLeft":{"x":7,"y":8}
                        }}
                        """),
                FdCodecDiagnosticCode.UNKNOWN_FIELD,
                "/root/properties/clipRadius/geometry/topLeft/z");
    }

    @Test
    void rejectsUnrepresentableRadiusCoordinatesOnDecodeAndEncode() throws Exception {
        assertInvalid(
                propertyDocument("""
                        {"kind":"borderRadius","geometry":{
                          "kind":"physical",
                          "topLeft":{"x":1e309,"y":2},
                          "topRight":{"x":3,"y":4},
                          "bottomRight":{"x":5,"y":6},
                          "bottomLeft":{"x":7,"y":8}
                        }}
                        """),
                FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipRadius/geometry/topLeft/x");

        PropertyValue.BorderRadiusValue unrepresentable =
                new PropertyValue.BorderRadiusValue(
                        new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                                new PropertyValue.BoxDecorationValue.Radius(
                                        new BigDecimal("1e309"), BigDecimal.ONE),
                                radius("3", "4"), radius("5", "6"),
                                radius("7", "8")));
        FdEncodeException failure = assertThrows(
                FdEncodeException.class,
                () -> codec.encode(document(Map.of(CLIP_RADIUS, unrepresentable))));
        assertEquals(FdCodecDiagnosticCode.INVALID_VALUE, failure.diagnostic().code());
        assertEquals(
                "/root/properties/clipRadius/geometry/topLeft/x",
                failure.diagnostic().pointer());
    }

    @Test
    void migratesV10OmissionWithoutInventingABorderRadiusValue() throws Exception {
        String current = new String(codec.encode(document(Map.of())).copyBytes(),
                StandardCharsets.UTF_8);
        String legacy = current
                .replace("\"schemaVersion\": 17", "\"schemaVersion\": 10")
                .replace("../fd-v17.schema.json", "../fd-v10.schema.json");

        FdDecodeResult.Current migrated = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(legacy.getBytes(StandardCharsets.UTF_8)));
        assertEquals(10, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        assertEquals(Optional.of("../fd-v17.schema.json"),
                migrated.document().schemaReference());
        assertFalse(migrated.document().root().properties().containsKey(CLIP_RADIUS));
        assertTrue(new String(codec.encode(migrated.document()).copyBytes(),
                StandardCharsets.UTF_8).contains("\"schemaVersion\": 17"));
    }

    private void assertInvalid(
            String json,
            FdCodecDiagnosticCode code,
            String pointer) throws Exception {
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)), json);
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == code && diagnostic.pointer().equals(pointer)),
                () -> invalid.diagnostics().toString());
    }

    private static void assertCanonicalOrder(
            String json,
            String startToken,
            String endToken,
            String... tokens) {
        int start = json.indexOf(startToken);
        int end = json.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0 && end > start, json);
        int cursor = start;
        for (String token : tokens) {
            int index = json.indexOf(token, cursor);
            assertTrue(index >= cursor && index < end,
                    () -> token + " must appear in canonical order between "
                    + startToken + " and " + endToken + "\n" + json);
            cursor = index + token.length();
        }
    }

    private static void assertRadiusFieldOrder(
            String json,
            String radiusToken,
            String nextRadiusToken) {
        int start = json.indexOf(radiusToken);
        int end = json.indexOf(nextRadiusToken, start + radiusToken.length());
        int x = json.indexOf("\"x\"", start);
        int y = json.indexOf("\"y\"", x + 3);
        assertTrue(start >= 0 && x > start && y > x && y < end, json);
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(String x, String y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                new BigDecimal(x), new BigDecimal(y));
    }

    private static DesignerDocument document(
            Map<PropertyName, PropertyValue> properties) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v17.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "clip_rrect_page.dart",
                        "ClipRRectPage",
                        WidgetClassKind.STATELESS,
                        Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(),
                new WidgetNode(
                        StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                        new WidgetTypeId("flutter.widgets.ClipRRect"),
                        properties,
                        Map.of(),
                        Extensions.empty()),
                Extensions.empty());
    }

    private static String propertyDocument(String value) {
        return """
                {
                  "$schema": "../fd-v11.schema.json",
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 11,
                  "documentId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                  "source": {
                    "dartFile": "clip_rrect_page.dart",
                    "className": "ClipRRectPage",
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
                    "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                    "type": "flutter.widgets.ClipRRect",
                    "properties": {"clipRadius": %s},
                    "slots": {}
                  }
                }
                """.formatted(value);
    }
}

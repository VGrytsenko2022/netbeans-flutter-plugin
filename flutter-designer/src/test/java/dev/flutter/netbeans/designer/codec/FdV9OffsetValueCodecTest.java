package dev.flutter.netbeans.designer.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FdV9OffsetValueCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void currentV10RoundTripsNormalizedFiniteSignedOffset() throws Exception {
        PropertyValue.OffsetValue expected = new PropertyValue.OffsetValue(
                new BigDecimal("-12.500"), new BigDecimal("8.2500"));
        OriginalFdBytes first = codec.encode(document(Optional.of(expected)));
        String json = new String(first.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 10"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v10.schema.json\""), json);
        assertTrue(json.contains("\"kind\": \"offset\""), json);
        assertTrue(json.contains("\"dx\": -12.5"), json);
        assertTrue(json.contains("\"dy\": 8.25"), json);
        assertTrue(json.indexOf("\"dx\": -12.5")
                < json.indexOf("\"dy\": 8.25"), json);

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(first));
        assertEquals(10, decoded.sourceSchemaVersion());
        assertFalse(decoded.migrated());
        assertEquals(expected, origin(decoded.document()).orElseThrow());
        assertArrayEquals(first.copyBytes(),
                codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void migratesV8WithoutOffsetAndRejectsOffsetKindBeforeV9() throws Exception {
        String currentWithoutOrigin = new String(
                codec.encode(document(Optional.empty())).copyBytes(),
                StandardCharsets.UTF_8);
        String legacy = currentWithoutOrigin
                .replace("\"schemaVersion\": 10", "\"schemaVersion\": 8")
                .replace("../fd-v10.schema.json", "../fd-v8.schema.json");

        FdDecodeResult.Current migrated = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(legacy.getBytes(StandardCharsets.UTF_8)));
        assertEquals(8, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        assertEquals(Optional.of("../fd-v10.schema.json"),
                migrated.document().schemaReference());
        assertTrue(origin(migrated.document()).isEmpty());

        String currentWithOrigin = new String(
                codec.encode(document(Optional.of(new PropertyValue.OffsetValue(
                        BigDecimal.ONE.negate(), BigDecimal.ONE)))).copyBytes(),
                StandardCharsets.UTF_8);
        assertInvalid(currentWithOrigin
                        .replace("\"schemaVersion\": 10", "\"schemaVersion\": 8")
                        .replace("../fd-v10.schema.json", "../fd-v8.schema.json"),
                "/root/properties/origin/kind");
    }

    @Test
    void rejectsUnrepresentableMissingAndUnknownOffsetFieldsFailClosed()
            throws Exception {
        String current = new String(codec.encode(document(Optional.of(
                new PropertyValue.OffsetValue(
                        new BigDecimal("-12.5"), new BigDecimal("8.25")))))
                .copyBytes(), StandardCharsets.UTF_8);

        assertInvalid(current.replace("\"dx\": -12.5", "\"dx\": 1e10000"),
                "/root/properties/origin/dx");
        assertInvalid(current.replace("\"dy\": 8.25", "\"dy\": 1e-10000"),
                "/root/properties/origin/dy");
        assertInvalid(current.replaceFirst(
                        ",\\R\\s*\"dy\": 8.25", ""),
                "/root/properties/origin/dy");
        assertInvalid(current.replace(
                        "      \"dy\": 8.25",
                        "      \"dy\": 8.25,\n      \"distance\": 1"),
                "/root/properties/origin/distance");
    }

    private void assertInvalid(String json, String pointer) throws Exception {
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)), json);
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().equals(pointer)
                || diagnostic.pointer().startsWith(pointer)),
                () -> invalid.diagnostics().toString());
    }

    private static Optional<PropertyValue.OffsetValue> origin(
            DesignerDocument document) {
        return Optional.ofNullable(document.root().properties().get(
                        new PropertyName("origin")))
                .map(PropertyValue.OffsetValue.class::cast);
    }

    private static DesignerDocument document(
            Optional<PropertyValue.OffsetValue> origin) {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("transform"), identityMatrix());
        origin.ifPresent(value -> properties.put(new PropertyName("origin"), value));
        WidgetNode root = new WidgetNode(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new WidgetTypeId("flutter.widgets.Transform"),
                properties,
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v10.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "transform_page.dart", "TransformPage",
                        WidgetClassKind.STATELESS, Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(), root, Extensions.empty());
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        return new PropertyValue.Matrix4Value(List.of(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
    }
}

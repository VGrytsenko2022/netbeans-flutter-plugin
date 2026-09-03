package dev.flutter.netbeans.designer.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FdV8SizeValueCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void canonicalCurrentSchemaRoundTripsNormalizedFiniteNonNegativeSize()
            throws Exception {
        PropertyValue.SizeValue expected = new PropertyValue.SizeValue(
                new BigDecimal("120.50"), BigDecimal.ZERO);
        OriginalFdBytes first = codec.encode(document(expected));
        String json = new String(first.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 9"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v9.schema.json\""), json);
        assertTrue(json.contains("\"kind\": \"size\""), json);
        assertTrue(json.contains("\"width\": 120.5"), json);
        assertTrue(json.contains("\"height\": 0"), json);
        assertTrue(json.indexOf("\"width\": 120.5")
                < json.indexOf("\"height\": 0"), json);

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(first));
        assertEquals(9, decoded.sourceSchemaVersion());
        assertTrue(!decoded.migrated());
        assertEquals(expected, size(decoded.document()));
        assertArrayEquals(first.copyBytes(),
                codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void rejectsSizeKindBeforeV8AndNegativeOrUnrepresentableDimensions()
            throws Exception {
        String current = new String(codec.encode(document(
                new PropertyValue.SizeValue(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(100))))
                .copyBytes(), StandardCharsets.UTF_8);

        assertInvalid(current
                .replace("\"schemaVersion\": 9", "\"schemaVersion\": 7")
                .replace("../fd-v9.schema.json", "../fd-v7.schema.json"),
                "/root/properties/size/kind");
        assertInvalid(current.replace("\"width\": 100", "\"width\": -1"),
                "/root/properties/size");
        assertInvalid(current.replace("\"height\": 100", "\"height\": -1"),
                "/root/properties/size");
        assertInvalid(current.replace("\"width\": 100", "\"width\": 1e10000"),
                "/root/properties/size/width");
    }

    @Test
    void rejectsMissingAndUnknownSizeFieldsFailClosed() throws Exception {
        String current = new String(codec.encode(document(
                new PropertyValue.SizeValue(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(100))))
                .copyBytes(), StandardCharsets.UTF_8);

        assertInvalid(current.replaceFirst(
                        ",\\R\\s*\"height\": 100", ""),
                "/root/properties/size/height");
        assertInvalid(current.replace(
                        "      \"height\": 100",
                        "      \"height\": 100,\n      \"depth\": 1"),
                "/root/properties/size/depth");
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

    private static PropertyValue.SizeValue size(DesignerDocument document) {
        return assertInstanceOf(PropertyValue.SizeValue.class,
                document.root().properties().get(new PropertyName("size")));
    }

    private static DesignerDocument document(PropertyValue.SizeValue size) {
        WidgetNode root = new WidgetNode(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new WidgetTypeId("flutter.widgets.SizedOverflowBox"),
                Map.of(new PropertyName("size"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v9.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "sized_overflow_page.dart", "SizedOverflowPage",
                        WidgetClassKind.STATELESS, Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(), root, Extensions.empty());
    }
}

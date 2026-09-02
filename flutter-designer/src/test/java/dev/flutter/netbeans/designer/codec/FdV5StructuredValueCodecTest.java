package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdV5StructuredValueCodecTest {
    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsEveryPreImageStructuredKindAndNestedDecorationUnion() throws Exception {
        DesignerDocument document = document();
        OriginalFdBytes encoded = codec.encode(document);
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"schemaVersion\": 7"), json);
        assertTrue(json.contains("\"kind\": \"alignmentGeometry\""), json);
        assertTrue(json.contains("\"kind\": \"boxConstraints\""), json);
        assertTrue(json.contains("\"kind\": \"matrix4\""), json);
        assertTrue(json.contains("\"kind\": \"boxDecoration\""), json);
        assertTrue(json.contains("\"kind\": \"directional\""), json);
        assertTrue(json.contains("\"kind\": \"radial\""), json);
        assertTrue(json.contains("\"maxWidth\": null"), json);

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(7, decoded.sourceSchemaVersion());
        assertEquals(document, decoded.document());
        assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void rejectsStructuredKindsWhenTheEnvelopeClaimsV4() throws Exception {
        String json = new String(codec.encode(document()).copyBytes(), StandardCharsets.UTF_8)
                .replace("\"schemaVersion\": 7", "\"schemaVersion\": 4");
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.pointer().endsWith("/kind")
                && diagnostic.message().contains("schema version 5")));
    }

    private static DesignerDocument document() {
        PropertyValue.AlignmentGeometryValue center = alignment(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ZERO, BigDecimal.ZERO);
        ColorSource primary = new ColorSource.Theme(
                new ThemeToken("material.colorScheme.primary"));
        PropertyValue.BoxDecorationValue.BorderSide side =
                new PropertyValue.BoxDecorationValue.BorderSide(
                        primary, BigDecimal.ONE,
                        PropertyValue.BoxDecorationValue.BorderStyle.SOLID,
                        BigDecimal.ONE.negate());
        PropertyValue.BoxDecorationValue.DirectionalBorder border =
                new PropertyValue.BoxDecorationValue.DirectionalBorder(
                        side, side, side, side);
        PropertyValue.BoxDecorationValue.PhysicalBorderRadius radius =
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius(1, 2), radius(3, 4), radius(5, 6), radius(7, 8));
        PropertyValue.BoxDecorationValue.GradientStop first = stop(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", primary, BigDecimal.ZERO);
        PropertyValue.BoxDecorationValue.GradientStop second = stop(
                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                new ColorSource.Literal(0xFFFFFFFFL), BigDecimal.ONE);
        PropertyValue.BoxDecorationValue.RadialGradient gradient =
                new PropertyValue.BoxDecorationValue.RadialGradient(
                        center, new BigDecimal("0.5"),
                        Optional.of(alignment(
                                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                                new BigDecimal("0.25"), BigDecimal.ZERO)),
                        new BigDecimal("0.1"), List.of(first, second),
                        PropertyValue.BoxDecorationValue.TileMode.MIRROR,
                        Optional.of(new BigDecimal("0.125")));
        PropertyValue.BoxDecorationValue.BoxShadow shadow =
                new PropertyValue.BoxDecorationValue.BoxShadow(
                        StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                        new ColorSource.Literal(0x66000000L), BigDecimal.ONE,
                        BigDecimal.TWO, BigDecimal.valueOf(3), BigDecimal.valueOf(-1),
                        PropertyValue.PaintValue.BlurStyle.OUTER);
        PropertyValue.BoxDecorationValue decoration =
                new PropertyValue.BoxDecorationValue(
                        Optional.of(primary), Optional.of(border), Optional.of(radius),
                        List.of(shadow), Optional.of(gradient),
                        Optional.of(PropertyValue.PaintValue.BlendMode.MULTIPLY),
                        PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);

        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(new PropertyName("alignment"), center);
        properties.put(new PropertyName("constraints"),
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.TEN, Optional.empty(), BigDecimal.ZERO,
                        Optional.of(BigDecimal.valueOf(500))));
        properties.put(new PropertyName("transform"), new PropertyValue.Matrix4Value(
                java.util.stream.IntStream.range(0, 16)
                        .mapToObj(BigDecimal::valueOf).toList()));
        properties.put(new PropertyName("decoration"), decoration);

        WidgetNode root = new WidgetNode(
                StableId.parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                new WidgetTypeId("flutter.widgets.Container"), properties, Map.of());
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "container_page.dart", "ContainerPage", WidgetClassKind.STATELESS,
                Optional.of("test"), new ManagedRegions(region, region));
        return new DesignerDocument(
                Optional.of("../fd-v7.schema.json"),
                StableId.parse("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"),
                source, Optional.empty(), root,
                dev.flutter.netbeans.designer.model.Extensions.empty());
    }

    private static PropertyValue.AlignmentGeometryValue alignment(
            PropertyValue.AlignmentGeometryValue.HorizontalBasis basis,
            BigDecimal horizontal,
            BigDecimal vertical) {
        return new PropertyValue.AlignmentGeometryValue(basis, horizontal, vertical);
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(long x, long y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                BigDecimal.valueOf(x), BigDecimal.valueOf(y));
    }

    private static PropertyValue.BoxDecorationValue.GradientStop stop(
            String id, ColorSource color, BigDecimal position) {
        return new PropertyValue.BoxDecorationValue.GradientStop(
                StableId.parse(id), color, position);
    }
}

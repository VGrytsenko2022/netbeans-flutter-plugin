package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PropertyValueTest {
    @Test
    void exposesEverySchemaV1ValueKind() {
        List<PropertyValue> values = List.of(
                new PropertyValue.StringValue(""),
                new PropertyValue.BooleanValue(true),
                new PropertyValue.IntegerValue(BigInteger.TEN),
                new PropertyValue.DoubleValue(new BigDecimal("1.25")),
                new PropertyValue.EnumValue("MainAxisAlignment", "center"),
                new PropertyValue.ColorValue(0xFFAABBCCL),
                new PropertyValue.EdgeInsetsValue(
                        BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN, BigDecimal.ZERO),
                new PropertyValue.AssetValue("assets/logo.png"),
                new PropertyValue.CallbackValue("onContinue"),
                new PropertyValue.DartExpressionValue("const SizedBox.shrink()"));

        assertEquals(List.of(
                PropertyValueKind.STRING,
                PropertyValueKind.BOOLEAN,
                PropertyValueKind.INTEGER,
                PropertyValueKind.DOUBLE,
                PropertyValueKind.ENUM,
                PropertyValueKind.COLOR,
                PropertyValueKind.EDGE_INSETS,
                PropertyValueKind.ASSET,
                PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_EXPRESSION), values.stream().map(PropertyValue::kind).toList());
        assertEquals("edgeInsets", PropertyValueKind.EDGE_INSETS.wireName());
    }

    @Test
    void keepsArbitraryIntegersAndNormalizesDecimalSemanticEquality() {
        BigInteger huge = BigInteger.ONE.shiftLeft(4096);
        assertEquals(huge, new PropertyValue.IntegerValue(huge).value());
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("1.0")),
                new PropertyValue.DoubleValue(new BigDecimal("1.00000")));
        assertEquals(
                new PropertyValue.DoubleValue(new BigDecimal("-0.000")),
                new PropertyValue.DoubleValue(BigDecimal.ZERO));

        PropertyValue.EdgeInsetsValue first = new PropertyValue.EdgeInsetsValue(
                new BigDecimal("1.0"), new BigDecimal("2.00"), new BigDecimal("0.0"), new BigDecimal("-1.00"));
        PropertyValue.EdgeInsetsValue second = new PropertyValue.EdgeInsetsValue(
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, BigDecimal.ONE.negate());
        assertEquals(first, second);
    }

    @Test
    void enumUsesQualifiedIdentifierSegments() {
        assertEquals("Material.MainAxisAlignment",
                new PropertyValue.EnumValue("Material.MainAxisAlignment", "center").type());
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material..Alignment", "center"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material.Alignment.", "center"));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.EnumValue("Material.Alignment", "bad-value"));
    }

    @Test
    void colorUsesUnsignedArgbRangeAndCanonicalWireText() {
        assertEquals("0x00000000", new PropertyValue.ColorValue(0).wireArgb());
        assertEquals("0xFFFFFFFF", new PropertyValue.ColorValue(0xFFFF_FFFFL).wireArgb());
        assertEquals(0x80ABCDEFL, PropertyValue.ColorValue.fromWireArgb("0x80ABCDEF").argb());
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ColorValue(-1));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ColorValue(0x1_0000_0000L));
        assertThrows(IllegalArgumentException.class,
                () -> PropertyValue.ColorValue.fromWireArgb("0xffFFFFFF"));
    }

    @Test
    void respectsCodePointLengthAndLexicalBoundaries() {
        String assetAtLimit = "😀".repeat(4096);
        String expressionAtLimit = "x".repeat(65_536);
        assertEquals(assetAtLimit, new PropertyValue.AssetValue(assetAtLimit).path());
        assertEquals(expressionAtLimit, new PropertyValue.DartExpressionValue(expressionAtLimit).code());
        assertEquals("_onTap2", new PropertyValue.CallbackValue("_onTap2").handler());

        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.AssetValue(""));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.AssetValue("😀".repeat(4097)));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.DartExpressionValue(""));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValue.DartExpressionValue("x".repeat(65_537)));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.CallbackValue("_"));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.CallbackValue("on-tap"));
    }

    @Test
    void rejectsNullPayloadsButKeepsEmptyStringAsARealValue() {
        assertEquals("", new PropertyValue.StringValue("").value());
        assertThrows(NullPointerException.class, () -> new PropertyValue.StringValue(null));
        assertThrows(NullPointerException.class, () -> new PropertyValue.IntegerValue(null));
        assertThrows(NullPointerException.class, () -> new PropertyValue.DoubleValue(null));
        assertThrows(NullPointerException.class,
                () -> new PropertyValue.EdgeInsetsValue(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void validatesThemeTokensAndRestrictsNestedColorSourcesToColorSchemeRoles() {
        ThemeToken color = new ThemeToken("material.colorScheme.primary");
        ThemeToken text = new ThemeToken("material.textTheme.bodyMedium");

        assertTrue(color.isColorSchemeToken());
        assertTrue(text.isTextThemeToken());
        assertEquals("bodyMedium", text.role());
        assertEquals(PropertyValueKind.THEME_TOKEN,
                new PropertyValue.ThemeTokenValue(text).kind());
        assertEquals("0xFFAABBCC", new ColorSource.Literal(0xFFAABBCCL).wireArgb());
        assertEquals(color, new ColorSource.Theme(color).token());

        assertThrows(IllegalArgumentException.class, () -> new ThemeToken("primary"));
        assertThrows(IllegalArgumentException.class,
                () -> new ThemeToken("material.colorScheme.Primary"));
        assertThrows(IllegalArgumentException.class, () -> new ColorSource.Theme(text));
    }

    @Test
    void modelsOnlyTheReviewedSafePaintSurface() {
        PropertyValue.PaintValue defaults = PropertyValue.PaintValue.defaults(
                new ColorSource.Theme(new ThemeToken("material.colorScheme.onSurface")));
        assertEquals(PropertyValue.PaintValue.BlendMode.SRC_OVER, defaults.blendMode());
        assertEquals(PropertyValue.PaintValue.Style.FILL, defaults.style());
        assertEquals(BigDecimal.valueOf(4), defaults.strokeMiterLimit());
        assertTrue(defaults.maskFilter().isEmpty());

        PropertyValue.PaintValue blurred = new PropertyValue.PaintValue(
                new ColorSource.Literal(0xFF000000L),
                PropertyValue.PaintValue.BlendMode.MULTIPLY,
                PropertyValue.PaintValue.Style.STROKE,
                new BigDecimal("2.5"),
                PropertyValue.PaintValue.StrokeCap.ROUND,
                PropertyValue.PaintValue.StrokeJoin.BEVEL,
                BigDecimal.TEN,
                false,
                PropertyValue.PaintValue.FilterQuality.HIGH,
                true,
                Optional.of(new PropertyValue.PaintValue.BlurMask(
                        PropertyValue.PaintValue.BlurStyle.OUTER,
                        new BigDecimal("3.5"))));
        assertEquals(PropertyValueKind.PAINT, blurred.kind());
        assertEquals("multiply", blurred.blendMode().wireName());

        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.PaintValue.BlurMask(
                PropertyValue.PaintValue.BlurStyle.NORMAL, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.PaintValue(
                new ColorSource.Literal(0),
                PropertyValue.PaintValue.BlendMode.SRC_OVER,
                PropertyValue.PaintValue.Style.FILL,
                BigDecimal.ONE.negate(),
                PropertyValue.PaintValue.StrokeCap.BUTT,
                PropertyValue.PaintValue.StrokeJoin.MITER,
                BigDecimal.ONE,
                true,
                PropertyValue.PaintValue.FilterQuality.NONE,
                false,
                Optional.empty()));
    }

    @Test
    void validatesOrderedShadowAndOpenTypeLists() {
        StableId firstId = StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        StableId secondId = StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        ColorSource black = new ColorSource.Literal(0xFF000000L);
        PropertyValue.ShadowListValue.Shadow shadow =
                new PropertyValue.ShadowListValue.Shadow(
                        firstId, black, BigDecimal.ONE.negate(), BigDecimal.TWO, BigDecimal.ZERO);
        PropertyValue.ShadowListValue shadows = new PropertyValue.ShadowListValue(List.of(shadow));
        assertEquals(firstId, shadows.items().getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> shadows.items().add(shadow));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.ShadowListValue(List.of(shadow, shadow)));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.ShadowListValue.Shadow(
                        secondId, black, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate()));

        PropertyValue.FontFeatureListValue.FontFeature liga =
                new PropertyValue.FontFeatureListValue.FontFeature(firstId, "liga", 1);
        assertEquals(List.of(liga), new PropertyValue.FontFeatureListValue(List.of(liga)).items());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue.FontFeature(secondId, "lig", 1));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue.FontFeature(secondId, "liga", -1));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontFeatureListValue(List.of(
                        liga,
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                secondId, "liga", 0))));

        PropertyValue.FontVariationListValue.FontVariation weight =
                new PropertyValue.FontVariationListValue.FontVariation(
                        firstId, "wght", new BigDecimal("700"));
        assertEquals(List.of(weight), new PropertyValue.FontVariationListValue(List.of(weight)).items());
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "wght", new BigDecimal("1000.1")));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "slnt", new BigDecimal("90")));
        assertThrows(IllegalArgumentException.class, () ->
                new PropertyValue.FontVariationListValue.FontVariation(
                        secondId, "ABÇD", BigDecimal.ZERO));
    }
}

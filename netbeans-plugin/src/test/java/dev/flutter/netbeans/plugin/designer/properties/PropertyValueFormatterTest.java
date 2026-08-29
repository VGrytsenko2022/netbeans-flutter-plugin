package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.ThemeToken;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class PropertyValueFormatterTest {

    @Test
    void formatsEveryDesignerValueKindWithoutLosingItsMeaning() {
        assertEquals("\"line\\n\\\"quoted\\\"\\\\path\"", PropertyValueFormatter.format(
                new PropertyValue.StringValue("line\n\"quoted\"\\path")));
        assertEquals("true", PropertyValueFormatter.format(
                new PropertyValue.BooleanValue(true)));
        assertEquals("-42", PropertyValueFormatter.format(
                new PropertyValue.IntegerValue(BigInteger.valueOf(-42))));
        assertEquals("12.5", PropertyValueFormatter.format(
                new PropertyValue.DoubleValue(new BigDecimal("12.500"))));
        assertEquals("MainAxisAlignment.center", PropertyValueFormatter.format(
                new PropertyValue.EnumValue("MainAxisAlignment", "center")));
        assertEquals("0xFF0A10F0", PropertyValueFormatter.format(
                PropertyValue.ColorValue.fromWireArgb("0xFF0A10F0")));
        assertEquals("left=1, top=2.5, right=3, bottom=4", PropertyValueFormatter.format(
                new PropertyValue.EdgeInsetsValue(
                        new BigDecimal("1"),
                        new BigDecimal("2.5"),
                        new BigDecimal("3"),
                        new BigDecimal("4"))));
        assertEquals("start=1, top=2.5, end=3, bottom=4", PropertyValueFormatter.format(
                new PropertyValue.EdgeInsetsDirectionalValue(
                        new BigDecimal("1"),
                        new BigDecimal("2.5"),
                        new BigDecimal("3"),
                        new BigDecimal("4"))));
        assertEquals("asset \"images/logo.png\"", PropertyValueFormatter.format(
                new PropertyValue.AssetValue("images/logo.png")));
        assertEquals("handleTap", PropertyValueFormatter.format(
                new PropertyValue.CallbackValue("handleTap")));
        assertEquals("const SizedBox.shrink()", PropertyValueFormatter.format(
                new PropertyValue.DartExpressionValue("const SizedBox.shrink()")));
        assertEquals("Icons.star (U+E5F9)", PropertyValueFormatter.format(
                new PropertyValue.IconDataValue(
                        java.util.Optional.of(0xE5F9),
                        java.util.Optional.of("MaterialIcons"),
                        java.util.Optional.empty(), false, java.util.List.of())));
        assertEquals("None", PropertyValueFormatter.format(
                PropertyValue.IconDataValue.none()));
        assertEquals("theme material.textTheme.bodyMedium", PropertyValueFormatter.format(
                new PropertyValue.ThemeTokenValue(
                        new ThemeToken("material.textTheme.bodyMedium"))));
        assertEquals("Paint(theme:primary, stroke, srcOver, blur)",
                PropertyValueFormatter.format(new PropertyValue.PaintValue(
                        new ColorSource.Theme(
                                new ThemeToken("material.colorScheme.primary")),
                        PropertyValue.PaintValue.BlendMode.SRC_OVER,
                        PropertyValue.PaintValue.Style.STROKE,
                        BigDecimal.ONE,
                        PropertyValue.PaintValue.StrokeCap.ROUND,
                        PropertyValue.PaintValue.StrokeJoin.ROUND,
                        BigDecimal.valueOf(4), true,
                        PropertyValue.PaintValue.FilterQuality.LOW, false,
                        java.util.Optional.of(new PropertyValue.PaintValue.BlurMask(
                                PropertyValue.PaintValue.BlurStyle.NORMAL,
                                BigDecimal.valueOf(2))))));
        assertEquals("1 shadow: theme:shadow @ 1,2",
                PropertyValueFormatter.format(new PropertyValue.ShadowListValue(
                        java.util.List.of(new PropertyValue.ShadowListValue.Shadow(
                                StableId.parse("28f75a47-8617-4e20-aa3d-acdb921b71da"),
                                new ColorSource.Theme(new ThemeToken(
                                        "material.colorScheme.shadow")),
                                BigDecimal.ONE, BigDecimal.valueOf(2),
                                BigDecimal.valueOf(3))))));
        assertEquals("1 feature: liga=1", PropertyValueFormatter.format(
                new PropertyValue.FontFeatureListValue(java.util.List.of(
                        new PropertyValue.FontFeatureListValue.FontFeature(
                                StableId.parse("81860a7b-51d1-421e-9580-7eed861a8942"),
                                "liga", 1)))));
        assertEquals("1 axis: wght=600", PropertyValueFormatter.format(
                new PropertyValue.FontVariationListValue(java.util.List.of(
                        new PropertyValue.FontVariationListValue.FontVariation(
                                StableId.parse("443b1525-af59-4afd-8a6a-852b919c3513"),
                                "wght", BigDecimal.valueOf(600))))));
    }

    @Test
    void rejectsNullInsteadOfConfusingItWithAnUnsetProperty() {
        assertThrows(NullPointerException.class, () -> PropertyValueFormatter.format(null));
    }
}

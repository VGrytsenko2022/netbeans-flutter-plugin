package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
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
}

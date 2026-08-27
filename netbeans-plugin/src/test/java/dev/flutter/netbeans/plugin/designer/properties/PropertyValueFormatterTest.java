package dev.flutter.netbeans.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class PropertyValueFormatterTest {

    @Test
    void formatsEverySchemaV1ValueKindWithoutLosingItsMeaning() {
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
        assertEquals("asset \"images/logo.png\"", PropertyValueFormatter.format(
                new PropertyValue.AssetValue("images/logo.png")));
        assertEquals("handleTap", PropertyValueFormatter.format(
                new PropertyValue.CallbackValue("handleTap")));
        assertEquals("const SizedBox.shrink()", PropertyValueFormatter.format(
                new PropertyValue.DartExpressionValue("const SizedBox.shrink()")));
    }

    @Test
    void rejectsNullInsteadOfConfusingItWithAnUnsetProperty() {
        assertThrows(NullPointerException.class, () -> PropertyValueFormatter.format(null));
    }
}

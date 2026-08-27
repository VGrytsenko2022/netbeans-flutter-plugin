package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.model.PropertyValue;
import java.math.BigDecimal;
import java.util.Objects;

/** Formats immutable designer property values for the NetBeans Properties window. */
public final class PropertyValueFormatter {
    private PropertyValueFormatter() {
    }

    /**
     * Returns a concise, unambiguous presentation of an explicit model value.
     * This method does not resolve catalog creation defaults.
     *
     * @param value an explicit value stored on a widget node
     * @return a human-readable value suitable for a read-only property cell
     */
    public static String format(PropertyValue value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case PropertyValue.StringValue stringValue -> quote(stringValue.value());
            case PropertyValue.BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
            case PropertyValue.IntegerValue integerValue -> integerValue.value().toString();
            case PropertyValue.DoubleValue doubleValue -> number(doubleValue.value());
            case PropertyValue.EnumValue enumValue -> enumValue.type() + '.' + enumValue.value();
            case PropertyValue.ColorValue colorValue -> colorValue.wireArgb();
            case PropertyValue.EdgeInsetsValue edgeInsets -> "left=" + number(edgeInsets.left())
                    + ", top=" + number(edgeInsets.top())
                    + ", right=" + number(edgeInsets.right())
                    + ", bottom=" + number(edgeInsets.bottom());
            case PropertyValue.AssetValue assetValue -> "asset " + quote(assetValue.path());
            case PropertyValue.CallbackValue callbackValue -> callbackValue.handler();
            case PropertyValue.DartExpressionValue expressionValue -> expressionValue.code();
        };
    }

    private static String number(BigDecimal value) {
        return value.toPlainString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2);
        result.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (Character.isISOControl(current)) {
                        result.append(String.format("\\u%04X", (int) current));
                    } else {
                        result.append(current);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}

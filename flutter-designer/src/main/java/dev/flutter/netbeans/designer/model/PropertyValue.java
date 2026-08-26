package dev.flutter.netbeans.designer.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Closed set of typed values accepted by Flutter Designer schema version 1. */
public sealed interface PropertyValue permits
        PropertyValue.StringValue,
        PropertyValue.BooleanValue,
        PropertyValue.IntegerValue,
        PropertyValue.DoubleValue,
        PropertyValue.EnumValue,
        PropertyValue.ColorValue,
        PropertyValue.EdgeInsetsValue,
        PropertyValue.AssetValue,
        PropertyValue.CallbackValue,
        PropertyValue.DartExpressionValue {

    PropertyValueKind kind();

    record StringValue(String value) implements PropertyValue {
        public StringValue {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.STRING;
        }
    }

    record BooleanValue(boolean value) implements PropertyValue {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BOOLEAN;
        }
    }

    record IntegerValue(BigInteger value) implements PropertyValue {
        public IntegerValue {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.INTEGER;
        }
    }

    record DoubleValue(BigDecimal value) implements PropertyValue {
        public DoubleValue {
            value = ModelConstraints.normalizedNumber(value, "value");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.DOUBLE;
        }
    }

    record EnumValue(String type, String value) implements PropertyValue {
        public EnumValue {
            type = ModelConstraints.matching(
                    type, "enum type", ModelConstraints.QUALIFIED_DART_IDENTIFIER);
            value = ModelConstraints.matching(
                    value, "enum value", ModelConstraints.DART_IDENTIFIER);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ENUM;
        }
    }

    record ColorValue(long argb) implements PropertyValue {
        private static final long MAX_ARGB = 0xFFFF_FFFFL;
        private static final Pattern WIRE_ARGB = Pattern.compile("0x[0-9A-F]{8}");

        public ColorValue {
            if (argb < 0 || argb > MAX_ARGB) {
                throw new IllegalArgumentException("ARGB color must be between 0x00000000 and 0xFFFFFFFF");
            }
        }

        public static ColorValue fromWireArgb(String value) {
            Objects.requireNonNull(value, "value");
            if (!WIRE_ARGB.matcher(value).matches()) {
                throw new IllegalArgumentException("ARGB color must use the form 0xAARRGGBB: " + value);
            }
            return new ColorValue(Long.parseUnsignedLong(value.substring(2), 16));
        }

        public String wireArgb() {
            return String.format(Locale.ROOT, "0x%08X", argb);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.COLOR;
        }
    }

    record EdgeInsetsValue(
            BigDecimal left,
            BigDecimal top,
            BigDecimal right,
            BigDecimal bottom) implements PropertyValue {

        public EdgeInsetsValue {
            left = ModelConstraints.normalizedNumber(left, "left");
            top = ModelConstraints.normalizedNumber(top, "top");
            right = ModelConstraints.normalizedNumber(right, "right");
            bottom = ModelConstraints.normalizedNumber(bottom, "bottom");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.EDGE_INSETS;
        }
    }

    record AssetValue(String path) implements PropertyValue {
        public AssetValue {
            path = ModelConstraints.codePointLength(path, "asset path", 1, 4096);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ASSET;
        }
    }

    record CallbackValue(String handler) implements PropertyValue {
        public CallbackValue {
            handler = ModelConstraints.matching(
                    handler, "callback handler", ModelConstraints.CALLBACK_HANDLER);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.CALLBACK;
        }
    }

    record DartExpressionValue(String code) implements PropertyValue {
        public DartExpressionValue {
            code = ModelConstraints.codePointLength(code, "Dart expression", 1, 65_536);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.DART_EXPRESSION;
        }
    }
}

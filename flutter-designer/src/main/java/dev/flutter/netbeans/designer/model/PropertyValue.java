package dev.flutter.netbeans.designer.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Closed set of typed values accepted by the current Flutter Designer schema. */
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
        PropertyValue.DartExpressionValue,
        PropertyValue.ThemeTokenValue,
        PropertyValue.PaintValue,
        PropertyValue.ShadowListValue,
        PropertyValue.FontFeatureListValue,
        PropertyValue.FontVariationListValue {

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

    record ThemeTokenValue(ThemeToken token) implements PropertyValue {
        public ThemeTokenValue {
            Objects.requireNonNull(token, "token");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.THEME_TOKEN;
        }
    }

    /** Safe, serializable subset of {@code dart:ui Paint}. */
    record PaintValue(
            ColorSource color,
            BlendMode blendMode,
            Style style,
            BigDecimal strokeWidth,
            StrokeCap strokeCap,
            StrokeJoin strokeJoin,
            BigDecimal strokeMiterLimit,
            boolean antiAlias,
            FilterQuality filterQuality,
            boolean invertColors,
            Optional<BlurMask> maskFilter) implements PropertyValue {

        public PaintValue {
            Objects.requireNonNull(color, "color");
            Objects.requireNonNull(blendMode, "blendMode");
            Objects.requireNonNull(style, "style");
            strokeWidth = nonNegative(strokeWidth, "strokeWidth");
            Objects.requireNonNull(strokeCap, "strokeCap");
            Objects.requireNonNull(strokeJoin, "strokeJoin");
            strokeMiterLimit = nonNegative(strokeMiterLimit, "strokeMiterLimit");
            Objects.requireNonNull(filterQuality, "filterQuality");
            Objects.requireNonNull(maskFilter, "maskFilter");
        }

        public static PaintValue defaults(ColorSource color) {
            return new PaintValue(
                    color,
                    BlendMode.SRC_OVER,
                    Style.FILL,
                    BigDecimal.ZERO,
                    StrokeCap.BUTT,
                    StrokeJoin.MITER,
                    BigDecimal.valueOf(4),
                    true,
                    FilterQuality.NONE,
                    false,
                    Optional.empty());
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.PAINT;
        }

        public enum BlendMode {
            CLEAR("clear"),
            SRC("src"),
            DST("dst"),
            SRC_OVER("srcOver"),
            DST_OVER("dstOver"),
            SRC_IN("srcIn"),
            DST_IN("dstIn"),
            SRC_OUT("srcOut"),
            DST_OUT("dstOut"),
            SRC_ATOP("srcATop"),
            DST_ATOP("dstATop"),
            XOR("xor"),
            PLUS("plus"),
            MODULATE("modulate"),
            SCREEN("screen"),
            OVERLAY("overlay"),
            DARKEN("darken"),
            LIGHTEN("lighten"),
            COLOR_DODGE("colorDodge"),
            COLOR_BURN("colorBurn"),
            HARD_LIGHT("hardLight"),
            SOFT_LIGHT("softLight"),
            DIFFERENCE("difference"),
            EXCLUSION("exclusion"),
            MULTIPLY("multiply"),
            HUE("hue"),
            SATURATION("saturation"),
            COLOR("color"),
            LUMINOSITY("luminosity");

            private final String wireName;

            BlendMode(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static BlendMode fromWireName(String wireName) {
                return enumValue(BlendMode.values(), wireName, BlendMode::wireName, "blend mode");
            }
        }

        public enum Style {
            FILL("fill"),
            STROKE("stroke");

            private final String wireName;

            Style(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static Style fromWireName(String wireName) {
                return enumValue(Style.values(), wireName, Style::wireName, "paint style");
            }
        }

        public enum StrokeCap {
            BUTT("butt"),
            ROUND("round"),
            SQUARE("square");

            private final String wireName;

            StrokeCap(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static StrokeCap fromWireName(String wireName) {
                return enumValue(StrokeCap.values(), wireName, StrokeCap::wireName, "stroke cap");
            }
        }

        public enum StrokeJoin {
            MITER("miter"),
            ROUND("round"),
            BEVEL("bevel");

            private final String wireName;

            StrokeJoin(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static StrokeJoin fromWireName(String wireName) {
                return enumValue(StrokeJoin.values(), wireName, StrokeJoin::wireName, "stroke join");
            }
        }

        public enum FilterQuality {
            NONE("none"),
            LOW("low"),
            MEDIUM("medium"),
            HIGH("high");

            private final String wireName;

            FilterQuality(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static FilterQuality fromWireName(String wireName) {
                return enumValue(
                        FilterQuality.values(), wireName, FilterQuality::wireName, "filter quality");
            }
        }

        public enum BlurStyle {
            NORMAL("normal"),
            SOLID("solid"),
            OUTER("outer"),
            INNER("inner");

            private final String wireName;

            BlurStyle(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static BlurStyle fromWireName(String wireName) {
                return enumValue(BlurStyle.values(), wireName, BlurStyle::wireName, "blur style");
            }
        }

        public record BlurMask(BlurStyle style, BigDecimal sigma) {
            public BlurMask {
                Objects.requireNonNull(style, "style");
                sigma = positive(sigma, "sigma");
            }
        }
    }

    record ShadowListValue(List<Shadow> items) implements PropertyValue {
        public static final int MAX_ITEMS = 256;

        public ShadowListValue {
            items = boundedItems(items, "shadows", MAX_ITEMS);
            uniqueIds(items.stream().map(Shadow::id).toList(), "shadow");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.SHADOW_LIST;
        }

        public record Shadow(
                StableId id,
                ColorSource color,
                BigDecimal offsetX,
                BigDecimal offsetY,
                BigDecimal blurRadius) {
            public Shadow {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(color, "color");
                offsetX = ModelConstraints.normalizedNumber(offsetX, "offsetX");
                offsetY = ModelConstraints.normalizedNumber(offsetY, "offsetY");
                blurRadius = nonNegative(blurRadius, "blurRadius");
            }
        }
    }

    record FontFeatureListValue(List<FontFeature> items) implements PropertyValue {
        public static final int MAX_ITEMS = 256;

        public FontFeatureListValue {
            items = boundedItems(items, "font features", MAX_ITEMS);
            uniqueIds(items.stream().map(FontFeature::id).toList(), "font feature");
            uniqueStrings(items.stream().map(FontFeature::tag).toList(), "font feature tag");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.FONT_FEATURE_LIST;
        }

        public record FontFeature(StableId id, String tag, int value) {
            public FontFeature {
                Objects.requireNonNull(id, "id");
                tag = printableAsciiTag(tag, "font feature tag");
                if (value < 0) {
                    throw new IllegalArgumentException(
                            "Font feature value must be between 0 and 2147483647");
                }
            }
        }
    }

    record FontVariationListValue(List<FontVariation> items) implements PropertyValue {
        public static final int MAX_ITEMS = 256;
        private static final BigDecimal MIN_VALUE = BigDecimal.valueOf(-32768);
        private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(32768);

        public FontVariationListValue {
            items = boundedItems(items, "font variations", MAX_ITEMS);
            uniqueIds(items.stream().map(FontVariation::id).toList(), "font variation");
            uniqueStrings(items.stream().map(FontVariation::axis).toList(), "font variation axis");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.FONT_VARIATION_LIST;
        }

        public record FontVariation(StableId id, String axis, BigDecimal value) {
            public FontVariation {
                Objects.requireNonNull(id, "id");
                axis = printableAsciiTag(axis, "font variation axis");
                value = ModelConstraints.normalizedNumber(value, "value");
                if (value.compareTo(MIN_VALUE) < 0 || value.compareTo(MAX_VALUE) >= 0) {
                    throw new IllegalArgumentException(
                            "Font variation value must be in [-32768, 32768)");
                }
                validateRegisteredAxis(axis, value);
            }

            private static void validateRegisteredAxis(String axis, BigDecimal value) {
                switch (axis) {
                    case "ital" -> requireRange(value, BigDecimal.ZERO, true, BigDecimal.ONE, true, axis);
                    case "opsz" -> requireRange(value, BigDecimal.ZERO, false, MAX_VALUE, false, axis);
                    case "slnt" -> requireRange(
                            value, BigDecimal.valueOf(-90), false, BigDecimal.valueOf(90), false, axis);
                    case "wdth" -> requireRange(
                            value, BigDecimal.ZERO, true, MAX_VALUE, false, axis);
                    case "wght" -> requireRange(
                            value, BigDecimal.ONE, true, BigDecimal.valueOf(1000), true, axis);
                    default -> {
                        // Custom OpenType axes use only the signed 16.16 range.
                    }
                }
            }
        }
    }

    private static BigDecimal nonNegative(BigDecimal value, String label) {
        value = ModelConstraints.normalizedNumber(value, label);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    private static BigDecimal positive(BigDecimal value, String label) {
        value = ModelConstraints.normalizedNumber(value, label);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
        return value;
    }

    private static <T> List<T> boundedItems(List<T> items, String label, int maximum) {
        Objects.requireNonNull(items, label);
        if (items.size() > maximum) {
            throw new IllegalArgumentException(label + " may contain at most " + maximum + " items");
        }
        return List.copyOf(items);
    }

    private static void uniqueIds(List<StableId> ids, String label) {
        Set<StableId> unique = new HashSet<>();
        for (StableId id : ids) {
            Objects.requireNonNull(id, label + " id");
            if (!unique.add(id)) {
                throw new IllegalArgumentException("Duplicate " + label + " id: " + id);
            }
        }
    }

    private static void uniqueStrings(List<String> values, String label) {
        Set<String> unique = new HashSet<>();
        for (String value : values) {
            if (!unique.add(value)) {
                throw new IllegalArgumentException("Duplicate " + label + ": " + value);
            }
        }
    }

    private static String printableAsciiTag(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.length() != 4) {
            throw new IllegalArgumentException(label + " must contain exactly four ASCII characters");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 0x20 || character > 0x7E) {
                throw new IllegalArgumentException(label + " must contain printable ASCII characters only");
            }
        }
        return value;
    }

    private static void requireRange(
            BigDecimal value,
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive,
            String axis) {
        int lower = value.compareTo(minimum);
        int upper = value.compareTo(maximum);
        if ((minimumInclusive ? lower < 0 : lower <= 0)
                || (maximumInclusive ? upper > 0 : upper >= 0)) {
            throw new IllegalArgumentException(
                    "Font variation value is outside the registered " + axis + " axis range");
        }
    }

    private static <E> E enumValue(
            E[] values,
            String wireName,
            java.util.function.Function<E, String> name,
            String label) {
        Objects.requireNonNull(wireName, "wireName");
        for (E value : values) {
            if (name.apply(value).equals(wireName)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown " + label + ": " + wireName);
    }
}

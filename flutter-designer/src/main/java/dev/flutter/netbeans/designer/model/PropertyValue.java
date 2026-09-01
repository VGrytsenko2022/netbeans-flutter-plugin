package dev.flutter.netbeans.designer.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Normalizer;
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
        PropertyValue.EdgeInsetsDirectionalValue,
        PropertyValue.AssetValue,
        PropertyValue.CallbackValue,
        PropertyValue.DartExpressionValue,
        PropertyValue.IconDataValue,
        PropertyValue.ThemeTokenValue,
        PropertyValue.PaintValue,
        PropertyValue.ShadowListValue,
        PropertyValue.FontFeatureListValue,
        PropertyValue.FontVariationListValue,
        PropertyValue.AlignmentGeometryValue,
        PropertyValue.BoxConstraintsValue,
        PropertyValue.Matrix4Value,
        PropertyValue.ImageProviderValue,
        PropertyValue.BoxDecorationValue {

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

    /**
     * Text-direction-aware Flutter edge insets. Unlike {@link EdgeInsetsValue},
     * {@code start} and {@code end} are resolved by the ambient
     * {@code TextDirection}; they must never be projected as physical left and
     * right values.
     */
    record EdgeInsetsDirectionalValue(
            BigDecimal start,
            BigDecimal top,
            BigDecimal end,
            BigDecimal bottom) implements PropertyValue {

        public EdgeInsetsDirectionalValue {
            start = ModelConstraints.normalizedNumber(start, "start");
            top = ModelConstraints.normalizedNumber(top, "top");
            end = ModelConstraints.normalizedNumber(end, "end");
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

    /**
     * Safe, serializable representation of the nullable {@code Icon.icon}
     * value and Flutter's const {@code IconData} constructor metadata.
     */
    record IconDataValue(
            Optional<Integer> codePoint,
            Optional<String> fontFamily,
            Optional<String> fontPackage,
            boolean matchTextDirection,
            List<String> fontFamilyFallback) implements PropertyValue {
        public static final int MAX_CODE_POINT = 0x10FFFF;
        public static final int MAX_TEXT_LENGTH = 256;
        public static final int MAX_FONT_FAMILY_FALLBACKS = 32;

        public IconDataValue {
            Objects.requireNonNull(codePoint, "codePoint");
            Objects.requireNonNull(fontFamily, "fontFamily");
            Objects.requireNonNull(fontPackage, "fontPackage");
            Objects.requireNonNull(fontFamilyFallback, "fontFamilyFallback");
            codePoint = codePoint.map(IconDataValue::validCodePoint);
            fontFamily = fontFamily.map(value -> safeMetadata(
                    value, "fontFamily"));
            fontPackage = fontPackage.map(value -> safeMetadata(
                    value, "fontPackage"));
            if (fontPackage.isPresent() && fontFamily.isEmpty()) {
                throw new IllegalArgumentException(
                        "fontPackage requires fontFamily");
            }
            if (fontFamilyFallback.size() > MAX_FONT_FAMILY_FALLBACKS) {
                throw new IllegalArgumentException(
                        "fontFamilyFallback may contain at most "
                        + MAX_FONT_FAMILY_FALLBACKS + " values");
            }
            java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
            for (String fallback : fontFamilyFallback) {
                String accepted = safeMetadata(fallback, "fontFamilyFallback");
                if (!unique.add(accepted)) {
                    throw new IllegalArgumentException(
                            "Duplicate fontFamilyFallback: " + accepted);
                }
            }
            fontFamilyFallback = List.copyOf(unique);
            if (codePoint.isEmpty()
                    && (fontFamily.isPresent()
                    || fontPackage.isPresent()
                    || matchTextDirection
                    || !fontFamilyFallback.isEmpty())) {
                throw new IllegalArgumentException(
                        "Null IconData cannot carry font metadata");
            }
        }

        public static IconDataValue none() {
            return new IconDataValue(
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    false, List.of());
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ICON_DATA;
        }

        private static int validCodePoint(int value) {
            if (value < 0 || value > MAX_CODE_POINT
                    || (value >= 0xD800 && value <= 0xDFFF)) {
                throw new IllegalArgumentException(
                        "IconData codePoint must be a Unicode scalar value");
            }
            return value;
        }

        private static String safeMetadata(String value, String label) {
            Objects.requireNonNull(value, label);
            int length = value.codePointCount(0, value.length());
            if (length < 1 || length > MAX_TEXT_LENGTH
                    || !value.equals(value.strip())) {
                throw new IllegalArgumentException(
                        label + " must contain 1.." + MAX_TEXT_LENGTH
                        + " characters of trimmed printable text");
            }
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                if (Character.isISOControl(codePoint)
                        || codePoint == 0x061C
                        || codePoint == 0x200E
                        || codePoint == 0x200F
                        || (codePoint >= 0x2028 && codePoint <= 0x202E)
                        || (codePoint >= 0x2066 && codePoint <= 0x2069)
                        || codePoint == 0xFEFF
                        || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
                    throw new IllegalArgumentException(
                            label + " must contain printable characters only");
                }
                offset += Character.charCount(codePoint);
            }
            return value;
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

    /** A physical or text-direction-aware Flutter {@code AlignmentGeometry}. */
    record AlignmentGeometryValue(
            HorizontalBasis basis,
            BigDecimal horizontal,
            BigDecimal vertical) implements PropertyValue {

        public AlignmentGeometryValue {
            Objects.requireNonNull(basis, "basis");
            horizontal = ModelConstraints.normalizedNumber(horizontal, "horizontal");
            vertical = ModelConstraints.normalizedNumber(vertical, "vertical");
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ALIGNMENT_GEOMETRY;
        }

        public enum HorizontalBasis {
            PHYSICAL("physical"),
            DIRECTIONAL("directional");

            private final String wireName;

            HorizontalBasis(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static HorizontalBasis fromWireName(String wireName) {
                return enumValue(
                        HorizontalBasis.values(), wireName,
                        HorizontalBasis::wireName, "alignment horizontal basis");
            }
        }
    }

    /** Finite Flutter box-constraint bounds; empty maxima represent infinity. */
    record BoxConstraintsValue(
            BigDecimal minWidth,
            Optional<BigDecimal> maxWidth,
            BigDecimal minHeight,
            Optional<BigDecimal> maxHeight) implements PropertyValue {

        public BoxConstraintsValue {
            minWidth = nonNegative(minWidth, "minWidth");
            maxWidth = normalizedOptionalNonNegative(maxWidth, "maxWidth");
            minHeight = nonNegative(minHeight, "minHeight");
            maxHeight = normalizedOptionalNonNegative(maxHeight, "maxHeight");
            if (maxWidth.isPresent() && maxWidth.orElseThrow().compareTo(minWidth) < 0) {
                throw new IllegalArgumentException("maxWidth must not be less than minWidth");
            }
            if (maxHeight.isPresent() && maxHeight.orElseThrow().compareTo(minHeight) < 0) {
                throw new IllegalArgumentException("maxHeight must not be less than minHeight");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BOX_CONSTRAINTS;
        }
    }

    /** Flutter {@code Matrix4.storage} in column-major order. */
    record Matrix4Value(List<BigDecimal> storage) implements PropertyValue {
        public static final int STORAGE_LENGTH = 16;

        public Matrix4Value {
            Objects.requireNonNull(storage, "storage");
            if (storage.size() != STORAGE_LENGTH) {
                throw new IllegalArgumentException("Matrix4 storage must contain exactly 16 values");
            }
            storage = storage.stream()
                    .map(value -> ModelConstraints.normalizedNumber(value, "storage value"))
                    .toList();
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.MATRIX4;
        }
    }

    /**
     * Closed, asset-only Flutter image-provider contract. Project-bound asset
     * resolution remains outside this pure document model; this value carries
     * only the normalized logical asset identity and reviewed decode options.
     */
    record ImageProviderValue(
            ProviderKind providerKind,
            String assetName,
            Optional<String> packageName,
            Optional<BigDecimal> exactScale,
            Optional<ResizeImageConfig> resize) implements PropertyValue {
        public static final int MAX_ASSET_NAME_LENGTH = 4096;
        public static final int MAX_PACKAGE_NAME_LENGTH = 64;
        public static final int MAX_RESIZE_DIMENSION = 16_384;
        private static final Pattern DART_PACKAGE_NAME = Pattern.compile(
                "[a-z][a-z0-9_]{0,63}");

        public ImageProviderValue {
            Objects.requireNonNull(providerKind, "providerKind");
            assetName = normalizedAssetName(assetName);
            Objects.requireNonNull(packageName, "packageName");
            packageName = packageName.map(value -> ModelConstraints.matching(
                    value, "Dart package name", DART_PACKAGE_NAME));
            exactScale = normalizedOptional(exactScale, "exactScale");
            resize = copiedOptional(resize, "resize");
            if (providerKind == ProviderKind.EXACT_ASSET) {
                if (exactScale.isEmpty()) {
                    throw new IllegalArgumentException(
                            "ExactAssetImage requires exactScale");
                }
                exactScale = Optional.of(positive(
                        exactScale.orElseThrow(), "exactScale"));
            } else if (exactScale.isPresent()) {
                throw new IllegalArgumentException(
                        "AssetImage cannot carry exactScale");
            }
        }

        public static ImageProviderValue asset(String assetName) {
            return new ImageProviderValue(
                    ProviderKind.ASSET,
                    assetName,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        public static ImageProviderValue exactAsset(
                String assetName, BigDecimal scale) {
            return new ImageProviderValue(
                    ProviderKind.EXACT_ASSET,
                    assetName,
                    Optional.empty(),
                    Optional.of(scale),
                    Optional.empty());
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.IMAGE_PROVIDER;
        }

        public enum ProviderKind {
            ASSET("asset"),
            EXACT_ASSET("exactAsset");

            private final String wireName;

            ProviderKind(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static ProviderKind fromWireName(String wireName) {
                return enumValue(
                        values(), wireName, ProviderKind::wireName,
                        "image provider kind");
            }
        }

        public record ResizeImageConfig(
                Optional<Integer> width,
                Optional<Integer> height,
                ResizePolicy policy,
                boolean allowUpscaling) {
            public ResizeImageConfig {
                Objects.requireNonNull(width, "width");
                Objects.requireNonNull(height, "height");
                width = width.map(value -> validDimension(value, "width"));
                height = height.map(value -> validDimension(value, "height"));
                Objects.requireNonNull(policy, "policy");
                if (width.isEmpty() && height.isEmpty()) {
                    throw new IllegalArgumentException(
                            "ResizeImage requires width or height");
                }
            }

            private static int validDimension(int value, String label) {
                if (value < 1 || value > MAX_RESIZE_DIMENSION) {
                    throw new IllegalArgumentException(
                            label + " must be between 1 and "
                            + MAX_RESIZE_DIMENSION);
                }
                return value;
            }
        }

        public enum ResizePolicy {
            EXACT("exact"),
            FIT("fit");

            private final String wireName;

            ResizePolicy(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static ResizePolicy fromWireName(String wireName) {
                return enumValue(
                        values(), wireName, ResizePolicy::wireName,
                        "ResizeImage policy");
            }
        }

        private static String normalizedAssetName(String value) {
            value = ModelConstraints.codePointLength(
                    value, "assetName", 1, MAX_ASSET_NAME_LENGTH);
            if (value.isBlank() || !value.equals(value.trim())) {
                throw new IllegalArgumentException(
                        "assetName must be non-blank without surrounding whitespace");
            }
            if (!value.equals(Normalizer.normalize(value, Normalizer.Form.NFC))) {
                throw new IllegalArgumentException("assetName must use Unicode NFC");
            }
            if (value.startsWith("/") || value.startsWith("~")
                    || value.endsWith("/")) {
                throw new IllegalArgumentException(
                        "assetName must be a relative POSIX path without a trailing slash");
            }
            String[] segments = value.split("/", -1);
            for (String segment : segments) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    throw new IllegalArgumentException(
                            "assetName must not contain empty, '.' or '..' path segments");
                }
            }
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                if (codePoint == '\\' || codePoint == ':' || codePoint == '%'
                        || Character.isISOControl(codePoint)
                        || codePoint == 0x061C
                        || codePoint == 0x200E
                        || codePoint == 0x200F
                        || (codePoint >= 0x2028 && codePoint <= 0x202E)
                        || (codePoint >= 0x2066 && codePoint <= 0x2069)
                        || codePoint == 0xFEFF
                        || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
                    throw new IllegalArgumentException(
                            "assetName must contain safe POSIX path characters only");
                }
                offset += Character.charCount(codePoint);
            }
            return value;
        }
    }

    /** Complete reviewed constructor contract for Flutter {@code DecorationImage}. */
    record DecorationImageValue(
            ImageProviderValue image,
            Optional<CallbackValue> onError,
            Optional<ColorFilter> colorFilter,
            Optional<BoxFit> fit,
            AlignmentGeometryValue alignment,
            Optional<Rect> centerSlice,
            ImageRepeat repeat,
            boolean matchTextDirection,
            BigDecimal scale,
            BigDecimal opacity,
            PaintValue.FilterQuality filterQuality,
            boolean invertColors,
            boolean isAntiAlias) {

        public DecorationImageValue {
            Objects.requireNonNull(image, "image");
            onError = copiedOptional(onError, "onError");
            colorFilter = copiedOptional(colorFilter, "colorFilter");
            fit = copiedOptional(fit, "fit");
            Objects.requireNonNull(alignment, "alignment");
            centerSlice = copiedOptional(centerSlice, "centerSlice");
            Objects.requireNonNull(repeat, "repeat");
            scale = positive(scale, "scale");
            opacity = ModelConstraints.normalizedNumber(opacity, "opacity");
            if (opacity.signum() < 0 || opacity.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("opacity must be in [0, 1]");
            }
            Objects.requireNonNull(filterQuality, "filterQuality");
            if (centerSlice.isPresent() && fit.filter(value ->
                    value == BoxFit.COVER || value == BoxFit.NONE).isPresent()) {
                throw new IllegalArgumentException(
                        "centerSlice does not allow BoxFit.cover or BoxFit.none");
            }
        }

        public static DecorationImageValue defaults(ImageProviderValue image) {
            return new DecorationImageValue(
                    image,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    new AlignmentGeometryValue(
                            AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO),
                    Optional.empty(),
                    ImageRepeat.NO_REPEAT,
                    false,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    PaintValue.FilterQuality.MEDIUM,
                    false,
                    false);
        }

        public sealed interface ColorFilter
                permits Mode, Matrix, LinearToSrgbGamma,
                        SrgbToLinearGamma, Saturation {
            String wireKind();
        }

        public record Mode(
                ColorSource color,
                PaintValue.BlendMode blendMode) implements ColorFilter {
            public Mode {
                Objects.requireNonNull(color, "color");
                Objects.requireNonNull(blendMode, "blendMode");
            }

            @Override
            public String wireKind() {
                return "mode";
            }
        }

        public record Matrix(List<BigDecimal> values) implements ColorFilter {
            public static final int VALUE_COUNT = 20;

            public Matrix {
                Objects.requireNonNull(values, "values");
                if (values.size() != VALUE_COUNT) {
                    throw new IllegalArgumentException(
                            "ColorFilter.matrix requires exactly 20 values");
                }
                values = values.stream()
                        .map(value -> ModelConstraints.normalizedNumber(
                                value, "matrix value"))
                        .toList();
            }

            @Override
            public String wireKind() {
                return "matrix";
            }
        }

        public record LinearToSrgbGamma() implements ColorFilter {
            @Override
            public String wireKind() {
                return "linearToSrgbGamma";
            }
        }

        public record SrgbToLinearGamma() implements ColorFilter {
            @Override
            public String wireKind() {
                return "srgbToLinearGamma";
            }
        }

        public record Saturation(BigDecimal value) implements ColorFilter {
            public Saturation {
                value = ModelConstraints.normalizedNumber(value, "saturation");
            }

            @Override
            public String wireKind() {
                return "saturation";
            }
        }

        public record Rect(
                BigDecimal left,
                BigDecimal top,
                BigDecimal right,
                BigDecimal bottom) {
            public Rect {
                left = nonNegative(left, "left");
                top = nonNegative(top, "top");
                right = nonNegative(right, "right");
                bottom = nonNegative(bottom, "bottom");
                if (left.compareTo(right) >= 0 || top.compareTo(bottom) >= 0) {
                    throw new IllegalArgumentException(
                            "Rect must have positive width and height");
                }
            }
        }

        public enum BoxFit {
            FILL("fill"),
            CONTAIN("contain"),
            COVER("cover"),
            FIT_WIDTH("fitWidth"),
            FIT_HEIGHT("fitHeight"),
            NONE("none"),
            SCALE_DOWN("scaleDown");

            private final String wireName;

            BoxFit(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static BoxFit fromWireName(String wireName) {
                return enumValue(values(), wireName, BoxFit::wireName, "BoxFit");
            }
        }

        public enum ImageRepeat {
            REPEAT("repeat"),
            REPEAT_X("repeatX"),
            REPEAT_Y("repeatY"),
            NO_REPEAT("noRepeat");

            private final String wireName;

            ImageRepeat(String wireName) {
                this.wireName = wireName;
            }

            public String wireName() {
                return wireName;
            }

            public static ImageRepeat fromWireName(String wireName) {
                return enumValue(
                        values(), wireName, ImageRepeat::wireName,
                        "image repeat");
            }
        }
    }

    /**
     * Reviewed Flutter {@code BoxDecoration}. Every nested union keeps its
     * physical versus directional semantics explicit.
     */
    record BoxDecorationValue(
            Optional<ColorSource> color,
            Optional<DecorationImageValue> image,
            Optional<BoxBorder> border,
            Optional<BorderRadiusGeometry> borderRadius,
            List<BoxShadow> boxShadow,
            Optional<BoxGradient> gradient,
            Optional<PaintValue.BlendMode> backgroundBlendMode,
            BoxShape shape) implements PropertyValue {
        public static final int MAX_SHADOWS = 256;
        public static final int MAX_GRADIENT_STOPS = 256;

        public BoxDecorationValue {
            color = copiedOptional(color, "color");
            image = copiedOptional(image, "image");
            border = copiedOptional(border, "border");
            borderRadius = copiedOptional(borderRadius, "borderRadius");
            boxShadow = boundedItems(boxShadow, "boxShadow", MAX_SHADOWS);
            boxShadow.forEach(value -> Objects.requireNonNull(value, "boxShadow item"));
            uniqueIds(boxShadow.stream().map(BoxShadow::id).toList(), "box shadow");
            gradient = copiedOptional(gradient, "gradient");
            backgroundBlendMode = copiedOptional(backgroundBlendMode, "backgroundBlendMode");
            Objects.requireNonNull(shape, "shape");
            if (shape == BoxShape.CIRCLE && borderRadius.isPresent()) {
                throw new IllegalArgumentException("A circular BoxDecoration cannot have a borderRadius");
            }
            if (backgroundBlendMode.isPresent() && color.isEmpty() && gradient.isEmpty()) {
                throw new IllegalArgumentException(
                        "backgroundBlendMode requires a color or gradient");
            }
            if (border.isPresent()) {
                validatePaintSafeBorder(border.orElseThrow(), borderRadius, shape);
            }
        }

        /** Source-compatible constructor for schema-v5 image-free callers. */
        public BoxDecorationValue(
                Optional<ColorSource> color,
                Optional<BoxBorder> border,
                Optional<BorderRadiusGeometry> borderRadius,
                List<BoxShadow> boxShadow,
                Optional<BoxGradient> gradient,
                Optional<PaintValue.BlendMode> backgroundBlendMode,
                BoxShape shape) {
            this(
                    color,
                    Optional.empty(),
                    border,
                    borderRadius,
                    boxShadow,
                    gradient,
                    backgroundBlendMode,
                    shape);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.BOX_DECORATION;
        }

        public sealed interface BoxBorder permits PhysicalBorder, DirectionalBorder {
        }

        public record PhysicalBorder(
                BorderSide top,
                BorderSide right,
                BorderSide bottom,
                BorderSide left) implements BoxBorder {
            public PhysicalBorder {
                Objects.requireNonNull(top, "top");
                Objects.requireNonNull(right, "right");
                Objects.requireNonNull(bottom, "bottom");
                Objects.requireNonNull(left, "left");
            }
        }

        public record DirectionalBorder(
                BorderSide top,
                BorderSide start,
                BorderSide end,
                BorderSide bottom) implements BoxBorder {
            public DirectionalBorder {
                Objects.requireNonNull(top, "top");
                Objects.requireNonNull(start, "start");
                Objects.requireNonNull(end, "end");
                Objects.requireNonNull(bottom, "bottom");
            }
        }

        public record BorderSide(
                ColorSource color,
                BigDecimal width,
                BorderStyle style,
                BigDecimal strokeAlign) {
            public BorderSide {
                Objects.requireNonNull(color, "color");
                width = nonNegative(width, "width");
                Objects.requireNonNull(style, "style");
                strokeAlign = ModelConstraints.normalizedNumber(strokeAlign, "strokeAlign");
            }
        }

        public sealed interface BorderRadiusGeometry
                permits PhysicalBorderRadius, DirectionalBorderRadius {
        }

        public record PhysicalBorderRadius(
                Radius topLeft,
                Radius topRight,
                Radius bottomRight,
                Radius bottomLeft) implements BorderRadiusGeometry {
            public PhysicalBorderRadius {
                Objects.requireNonNull(topLeft, "topLeft");
                Objects.requireNonNull(topRight, "topRight");
                Objects.requireNonNull(bottomRight, "bottomRight");
                Objects.requireNonNull(bottomLeft, "bottomLeft");
            }
        }

        public record DirectionalBorderRadius(
                Radius topStart,
                Radius topEnd,
                Radius bottomEnd,
                Radius bottomStart) implements BorderRadiusGeometry {
            public DirectionalBorderRadius {
                Objects.requireNonNull(topStart, "topStart");
                Objects.requireNonNull(topEnd, "topEnd");
                Objects.requireNonNull(bottomEnd, "bottomEnd");
                Objects.requireNonNull(bottomStart, "bottomStart");
            }
        }

        public record Radius(BigDecimal x, BigDecimal y) {
            public Radius {
                x = nonNegative(x, "radius x");
                y = nonNegative(y, "radius y");
            }
        }

        public record BoxShadow(
                StableId id,
                ColorSource color,
                BigDecimal offsetX,
                BigDecimal offsetY,
                BigDecimal blurRadius,
                BigDecimal spreadRadius,
                PaintValue.BlurStyle blurStyle) {
            public BoxShadow {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(color, "color");
                offsetX = ModelConstraints.normalizedNumber(offsetX, "offsetX");
                offsetY = ModelConstraints.normalizedNumber(offsetY, "offsetY");
                blurRadius = nonNegative(blurRadius, "blurRadius");
                spreadRadius = ModelConstraints.normalizedNumber(spreadRadius, "spreadRadius");
                Objects.requireNonNull(blurStyle, "blurStyle");
            }
        }

        public sealed interface BoxGradient
                permits LinearGradient, RadialGradient, SweepGradient {
            List<GradientStop> stops();
            TileMode tileMode();
            Optional<BigDecimal> rotationRadians();
        }

        public record LinearGradient(
                AlignmentGeometryValue begin,
                AlignmentGeometryValue end,
                List<GradientStop> stops,
                TileMode tileMode,
                Optional<BigDecimal> rotationRadians) implements BoxGradient {
            public LinearGradient {
                Objects.requireNonNull(begin, "begin");
                Objects.requireNonNull(end, "end");
                stops = validGradientStops(stops);
                Objects.requireNonNull(tileMode, "tileMode");
                rotationRadians = normalizedOptional(rotationRadians, "rotationRadians");
            }
        }

        public record RadialGradient(
                AlignmentGeometryValue center,
                BigDecimal radius,
                Optional<AlignmentGeometryValue> focal,
                BigDecimal focalRadius,
                List<GradientStop> stops,
                TileMode tileMode,
                Optional<BigDecimal> rotationRadians) implements BoxGradient {
            public RadialGradient {
                Objects.requireNonNull(center, "center");
                radius = nonNegative(radius, "radius");
                focal = copiedOptional(focal, "focal");
                focalRadius = nonNegative(focalRadius, "focalRadius");
                if (focal.isEmpty() && focalRadius.signum() != 0) {
                    throw new IllegalArgumentException("focalRadius requires focal");
                }
                stops = validGradientStops(stops);
                Objects.requireNonNull(tileMode, "tileMode");
                rotationRadians = normalizedOptional(rotationRadians, "rotationRadians");
            }
        }

        public record SweepGradient(
                AlignmentGeometryValue center,
                BigDecimal startAngle,
                BigDecimal endAngle,
                List<GradientStop> stops,
                TileMode tileMode,
                Optional<BigDecimal> rotationRadians) implements BoxGradient {
            public SweepGradient {
                Objects.requireNonNull(center, "center");
                startAngle = ModelConstraints.normalizedNumber(startAngle, "startAngle");
                endAngle = ModelConstraints.normalizedNumber(endAngle, "endAngle");
                if (startAngle.compareTo(endAngle) >= 0) {
                    throw new IllegalArgumentException("startAngle must be less than endAngle");
                }
                stops = validGradientStops(stops);
                Objects.requireNonNull(tileMode, "tileMode");
                rotationRadians = normalizedOptional(rotationRadians, "rotationRadians");
            }
        }

        public record GradientStop(StableId id, ColorSource color, BigDecimal stop) {
            public GradientStop {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(color, "color");
                stop = ModelConstraints.normalizedNumber(stop, "stop");
                if (stop.signum() < 0 || stop.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException("Gradient stop must be in [0, 1]");
                }
            }
        }

        public enum BorderStyle {
            NONE("none"), SOLID("solid");
            private final String wireName;
            BorderStyle(String wireName) { this.wireName = wireName; }
            public String wireName() { return wireName; }
            public static BorderStyle fromWireName(String value) {
                return enumValue(values(), value, BorderStyle::wireName, "border style");
            }
        }

        public enum TileMode {
            CLAMP("clamp"), REPEATED("repeated"), MIRROR("mirror"), DECAL("decal");
            private final String wireName;
            TileMode(String wireName) { this.wireName = wireName; }
            public String wireName() { return wireName; }
            public static TileMode fromWireName(String value) {
                return enumValue(values(), value, TileMode::wireName, "tile mode");
            }
        }

        public enum BoxShape {
            RECTANGLE("rectangle"), CIRCLE("circle");
            private final String wireName;
            BoxShape(String wireName) { this.wireName = wireName; }
            public String wireName() { return wireName; }
            public static BoxShape fromWireName(String value) {
                return enumValue(values(), value, BoxShape::wireName, "box shape");
            }
        }

        private static List<GradientStop> validGradientStops(List<GradientStop> values) {
            Objects.requireNonNull(values, "stops");
            if (values.size() < 2 || values.size() > MAX_GRADIENT_STOPS) {
                throw new IllegalArgumentException("Gradient stops must contain 2..256 items");
            }
            List<GradientStop> copied = List.copyOf(values);
            uniqueIds(copied.stream().map(GradientStop::id).toList(), "gradient stop");
            BigDecimal previous = null;
            for (GradientStop stop : copied) {
                Objects.requireNonNull(stop, "gradient stop");
                if (previous != null && stop.stop().compareTo(previous) < 0) {
                    throw new IllegalArgumentException("Gradient stops must be non-decreasing");
                }
                previous = stop.stop();
            }
            return copied;
        }

        private static void validatePaintSafeBorder(
                BoxBorder border,
                Optional<BorderRadiusGeometry> radius,
                BoxShape shape) {
            List<BorderSide> sides = border instanceof PhysicalBorder physical
                    ? List.of(physical.top(), physical.right(), physical.bottom(), physical.left())
                    : List.of(
                            ((DirectionalBorder) border).top(),
                            ((DirectionalBorder) border).start(),
                            ((DirectionalBorder) border).end(),
                            ((DirectionalBorder) border).bottom());
            boolean uniform = sides.stream().allMatch(sides.get(0)::equals);
            if (uniform || sides.stream().map(BorderSide::style).distinct().count() == 1
                    && sides.get(0).style() == BorderStyle.NONE) {
                return;
            }
            boolean nonZeroRadius = radius.map(BoxDecorationValue::hasNonZeroRadius)
                    .orElse(false);
            List<BorderSide> visible = sides.stream()
                    .filter(side -> side.style() != BorderStyle.NONE)
                    .toList();
            boolean oneVisibleColor = !visible.isEmpty()
                    && visible.stream().map(BorderSide::color).distinct().count() == 1;
            boolean hasHairline = visible.stream().anyMatch(side -> side.width().signum() == 0);
            if (oneVisibleColor && !hasHairline
                    && (shape == BoxShape.CIRCLE || nonZeroRadius)) {
                return;
            }
            if (shape != BoxShape.RECTANGLE || nonZeroRadius) {
                throw new IllegalArgumentException(
                        "A non-uniform border requires uniform visible colors and non-hairline sides for circles or rounded rectangles");
            }
            BigDecimal inside = BigDecimal.valueOf(-1);
            if (sides.stream().anyMatch(side -> side.strokeAlign().compareTo(inside) != 0)) {
                throw new IllegalArgumentException(
                        "A non-uniform rectangular border requires strokeAlign -1 on every side");
            }
        }

        private static boolean hasNonZeroRadius(BorderRadiusGeometry geometry) {
            List<Radius> radii = geometry instanceof PhysicalBorderRadius physical
                    ? List.of(
                            physical.topLeft(), physical.topRight(),
                            physical.bottomRight(), physical.bottomLeft())
                    : List.of(
                            ((DirectionalBorderRadius) geometry).topStart(),
                            ((DirectionalBorderRadius) geometry).topEnd(),
                            ((DirectionalBorderRadius) geometry).bottomEnd(),
                            ((DirectionalBorderRadius) geometry).bottomStart());
            return radii.stream().anyMatch(radius ->
                    radius.x().signum() != 0 || radius.y().signum() != 0);
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

    private static Optional<BigDecimal> normalizedOptional(
            Optional<BigDecimal> value, String label) {
        Objects.requireNonNull(value, label);
        return value.map(number -> ModelConstraints.normalizedNumber(number, label));
    }

    private static Optional<BigDecimal> normalizedOptionalNonNegative(
            Optional<BigDecimal> value, String label) {
        Objects.requireNonNull(value, label);
        return value.map(number -> nonNegative(number, label));
    }

    private static <T> Optional<T> copiedOptional(Optional<T> value, String label) {
        Objects.requireNonNull(value, label);
        value.ifPresent(item -> Objects.requireNonNull(item, label + " value"));
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

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Declarative constraint for one persisted property-value kind. */
public sealed interface PropertyValueConstraint permits
        PropertyValueConstraint.AnyValue,
        PropertyValueConstraint.StringLength,
        PropertyValueConstraint.StringPattern,
        PropertyValueConstraint.ThemeTokenValues,
        PropertyValueConstraint.PaintValues,
        PropertyValueConstraint.ShadowListValues,
        PropertyValueConstraint.FontVariationListValues,
        PropertyValueConstraint.IntegerRange,
        PropertyValueConstraint.DoubleRange,
        PropertyValueConstraint.EnumValues,
        PropertyValueConstraint.EdgeInsetsValues,
        PropertyValueConstraint.CallbackReference {

    PropertyValueKind kind();

    boolean accepts(PropertyValue value);

    String description();

    /** Accepts the complete schema-level domain of a value kind. */
    record AnyValue(PropertyValueKind kind) implements PropertyValueConstraint {
        public AnyValue {
            Objects.requireNonNull(kind, "kind");
            if (kind == PropertyValueKind.INTEGER
                    || kind == PropertyValueKind.DOUBLE
                    || kind == PropertyValueKind.ENUM
                    || kind == PropertyValueKind.EDGE_INSETS
                    || kind == PropertyValueKind.THEME_TOKEN
                    || kind == PropertyValueKind.PAINT
                    || kind == PropertyValueKind.SHADOW_LIST
                    || kind == PropertyValueKind.FONT_VARIATION_LIST
                    || kind == PropertyValueKind.CALLBACK) {
                throw new IllegalArgumentException(
                        kind.wireName() + " requires a typed catalog constraint");
            }
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value != null && value.kind() == kind;
        }

        @Override
        public String description() {
            return kind.wireName();
        }
    }

    /** Accepts theme tokens from one reviewed semantic role registry. */
    record ThemeTokenValues(List<String> wireIds) implements PropertyValueConstraint {
        public ThemeTokenValues {
            Objects.requireNonNull(wireIds, "wireIds");
            wireIds = List.copyOf(wireIds);
            if (wireIds.isEmpty() || wireIds.size() != new HashSet<>(wireIds).size()) {
                throw new IllegalArgumentException("Theme token ids must be non-empty and unique");
            }
            wireIds.forEach(value -> new dev.flutter.netbeans.designer.model.ThemeToken(value));
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.THEME_TOKEN;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ThemeTokenValue token
                    && wireIds.contains(token.token().wireId());
        }

        @Override
        public String description() {
            return "reviewed Material theme tokens " + wireIds;
        }
    }

    /** Accepts the typed Paint subset and only reviewed nested theme colors. */
    record PaintValues(List<String> colorThemeTokenIds) implements PropertyValueConstraint {
        public PaintValues {
            colorThemeTokenIds = themeTokenIds(colorThemeTokenIds);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.PAINT;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.PaintValue paint
                    && acceptsColorSource(paint.color(), colorThemeTokenIds)
                    && DartNumericLiterals.isRepresentableDouble(paint.strokeWidth())
                    && DartNumericLiterals.isRepresentableDouble(
                            paint.strokeMiterLimit())
                    && paint.maskFilter().map(mask ->
                        DartNumericLiterals.isRepresentableDouble(mask.sigma()))
                            .orElse(true);
        }

        @Override
        public String description() {
            return "typed Paint with a literal or reviewed Material theme color";
        }
    }

    /** Accepts ordered Shadows and only reviewed nested theme colors. */
    record ShadowListValues(List<String> colorThemeTokenIds)
            implements PropertyValueConstraint {
        public ShadowListValues {
            colorThemeTokenIds = themeTokenIds(colorThemeTokenIds);
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.SHADOW_LIST;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.ShadowListValue shadows
                    && shadows.items().stream().allMatch(shadow ->
                        acceptsColorSource(shadow.color(), colorThemeTokenIds)
                        && DartNumericLiterals.isRepresentableDouble(shadow.offsetX())
                        && DartNumericLiterals.isRepresentableDouble(shadow.offsetY())
                        && DartNumericLiterals.isRepresentableDouble(
                                shadow.blurRadius()));
        }

        @Override
        public String description() {
            return "ordered Shadows with literal or reviewed Material theme colors";
        }
    }

    /** Accepts ordered variable-font axes whose values survive Dart-double emission. */
    record FontVariationListValues() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.FONT_VARIATION_LIST;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.FontVariationListValue variations
                    && variations.items().stream().allMatch(variation ->
                        DartNumericLiterals.isRepresentableDouble(variation.value()));
        }

        @Override
        public String description() {
            return "ordered FontVariation values exactly representable as Dart doubles";
        }
    }

    private static List<String> themeTokenIds(List<String> values) {
        Objects.requireNonNull(values, "colorThemeTokenIds");
        List<String> copied = List.copyOf(values);
        if (copied.isEmpty() || copied.size() != new HashSet<>(copied).size()) {
            throw new IllegalArgumentException("Theme token ids must be non-empty and unique");
        }
        copied.forEach(value -> new dev.flutter.netbeans.designer.model.ThemeToken(value));
        return copied;
    }

    private static boolean acceptsColorSource(ColorSource source, List<String> tokens) {
        return switch (source) {
            case ColorSource.Literal ignored -> true;
            case ColorSource.Theme theme -> tokens.contains(theme.token().wireId());
        };
    }

    record StringLength(int minimum, int maximum) implements PropertyValueConstraint {
        public StringLength {
            if (minimum < 0 || maximum < minimum) {
                throw new IllegalArgumentException("Invalid string length range");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.STRING;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.StringValue stringValue)) {
                return false;
            }
            int length = stringValue.value().codePointCount(0, stringValue.value().length());
            return length >= minimum && length <= maximum;
        }

        @Override
        public String description() {
            return "string length " + minimum + ".." + maximum;
        }
    }

    /** Accepts strings that fully match a catalog-owned regular expression. */
    record StringPattern(String regularExpression, String description)
            implements PropertyValueConstraint {
        public StringPattern {
            Objects.requireNonNull(regularExpression, "regularExpression");
            Objects.requireNonNull(description, "description");
            if (regularExpression.isBlank()) {
                throw new IllegalArgumentException("String pattern must not be blank");
            }
            if (description.isBlank()) {
                throw new IllegalArgumentException("String pattern description must not be blank");
            }
            try {
                Pattern.compile(regularExpression);
            } catch (PatternSyntaxException failure) {
                throw new IllegalArgumentException("Invalid string pattern", failure);
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.STRING;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.StringValue stringValue
                    && Pattern.matches(regularExpression, stringValue.value());
        }
    }

    /** Null bounds mean unbounded. */
    record IntegerRange(BigInteger minimum, BigInteger maximum) implements PropertyValueConstraint {
        public IntegerRange {
            if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
                throw new IllegalArgumentException("Integer minimum exceeds maximum");
            }
            if ((minimum != null && !DartNumericLiterals.isPortableInteger(minimum))
                    || (maximum != null && !DartNumericLiterals.isPortableInteger(maximum))) {
                throw new IllegalArgumentException(
                        "Integer range bounds must be portable Dart integer literals");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.INTEGER;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.IntegerValue integerValue)) {
                return false;
            }
            return DartNumericLiterals.isPortableInteger(integerValue.value())
                    && (minimum == null || integerValue.value().compareTo(minimum) >= 0)
                    && (maximum == null || integerValue.value().compareTo(maximum) <= 0);
        }

        @Override
        public String description() {
            return "integer range " + (minimum == null ? "-infinity" : minimum)
                    + ".." + (maximum == null ? "+infinity" : maximum);
        }
    }

    /** Null bounds mean unbounded. */
    record DoubleRange(
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) implements PropertyValueConstraint {

        public DoubleRange {
            if ((minimum != null && !DartNumericLiterals.isRepresentableDouble(minimum))
                    || (maximum != null && !DartNumericLiterals.isRepresentableDouble(maximum))) {
                throw new IllegalArgumentException(
                        "Double range bounds must be representable Dart double literals");
            }
            if (minimum != null && maximum != null) {
                int comparison = minimum.compareTo(maximum);
                if (comparison > 0 || (comparison == 0 && (!minimumInclusive || !maximumInclusive))) {
                    throw new IllegalArgumentException("Invalid double range");
                }
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.DOUBLE;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (!(value instanceof PropertyValue.DoubleValue doubleValue)) {
                return false;
            }
            if (!DartNumericLiterals.isRepresentableDouble(doubleValue.value())) {
                return false;
            }
            if (minimum != null) {
                int comparison = doubleValue.value().compareTo(minimum);
                if (comparison < 0 || (comparison == 0 && !minimumInclusive)) {
                    return false;
                }
            }
            if (maximum != null) {
                int comparison = doubleValue.value().compareTo(maximum);
                if (comparison > 0 || (comparison == 0 && !maximumInclusive)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String description() {
            return "double range " + (minimumInclusive ? '[' : '(')
                    + (minimum == null ? "-infinity" : minimum) + ','
                    + (maximum == null ? "+infinity" : maximum)
                    + (maximumInclusive ? ']' : ')');
        }
    }

    record EnumValues(DartSymbolReference dartType, List<String> values) implements PropertyValueConstraint {
        public EnumValues {
            Objects.requireNonNull(dartType, "dartType");
            Objects.requireNonNull(values, "values");
            values = List.copyOf(values);
            if (values.isEmpty() || values.size() != new HashSet<>(values).size()) {
                throw new IllegalArgumentException("Enum values must be non-empty and unique");
            }
            for (String value : values) {
                DartIdentifiers.requirePublicIdentifier(value, "Dart enum value");
            }
        }

        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.ENUM;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.EnumValue enumValue
                    && dartType.name().equals(enumValue.type())
                    && values.contains(enumValue.value());
        }

        @Override
        public String description() {
            return dartType.name() + values;
        }
    }

    record CallbackReference() implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.CALLBACK;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            return value instanceof PropertyValue.CallbackValue callback
                    && DartIdentifiers.isIdentifier(callback.handler());
        }

        @Override
        public String description() {
            return "Dart callback identifier";
        }
    }

    record EdgeInsetsValues(boolean nonNegative) implements PropertyValueConstraint {
        @Override
        public PropertyValueKind kind() {
            return PropertyValueKind.EDGE_INSETS;
        }

        @Override
        public boolean accepts(PropertyValue value) {
            if (value instanceof PropertyValue.EdgeInsetsValue edgeInsets) {
                return acceptsSides(
                        edgeInsets.left(), edgeInsets.top(),
                        edgeInsets.right(), edgeInsets.bottom());
            }
            if (value instanceof PropertyValue.EdgeInsetsDirectionalValue edgeInsets) {
                return acceptsSides(
                        edgeInsets.start(), edgeInsets.top(),
                        edgeInsets.end(), edgeInsets.bottom());
            }
            return false;
        }

        private boolean acceptsSides(
                BigDecimal first,
                BigDecimal top,
                BigDecimal third,
                BigDecimal bottom) {
            return DartNumericLiterals.isRepresentableDouble(first)
                    && DartNumericLiterals.isRepresentableDouble(top)
                    && DartNumericLiterals.isRepresentableDouble(third)
                    && DartNumericLiterals.isRepresentableDouble(bottom)
                    && (!nonNegative
                    || (first.signum() >= 0
                    && top.signum() >= 0
                    && third.signum() >= 0
                    && bottom.signum() >= 0));
        }

        @Override
        public String description() {
            return nonNegative ? "non-negative edge insets" : "edge insets";
        }
    }
}

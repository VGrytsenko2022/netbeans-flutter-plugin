package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.PropertyValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Declarative constraint for one schema-v1 property-value kind. */
public sealed interface PropertyValueConstraint permits
        PropertyValueConstraint.AnyValue,
        PropertyValueConstraint.StringLength,
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
            if (!(value instanceof PropertyValue.EdgeInsetsValue edgeInsets)) {
                return false;
            }
            return DartNumericLiterals.isRepresentableDouble(edgeInsets.left())
                    && DartNumericLiterals.isRepresentableDouble(edgeInsets.top())
                    && DartNumericLiterals.isRepresentableDouble(edgeInsets.right())
                    && DartNumericLiterals.isRepresentableDouble(edgeInsets.bottom())
                    && (!nonNegative
                    || (edgeInsets.left().signum() >= 0
                    && edgeInsets.top().signum() >= 0
                    && edgeInsets.right().signum() >= 0
                    && edgeInsets.bottom().signum() >= 0));
        }

        @Override
        public String description() {
            return nonNegative ? "non-negative edge insets" : "edge insets";
        }
    }
}

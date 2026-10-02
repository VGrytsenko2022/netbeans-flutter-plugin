package io.github.vgrytsenko2022.designer.model;

import java.util.Objects;
import java.util.Optional;

/** Closed read-only projection of a user-owned State field into one constructor argument. */
public record StatePropertyBinding(
        String fieldName,
        StateBinding.Type type,
        Optional<PropertyValue.DartObjectReferenceValue> referenceType,
        Transform transform,
        Optional<PropertyValue> comparisonValue) {

    public enum Transform {
        DIRECT("direct"), TO_STRING("toString"), TEXT("text"), EQUALS("equals"), NOT("not"), CLAMP("clamp");
        private final String wireName;
        Transform(String wireName) { this.wireName = wireName; }
        public String wireName() { return wireName; }
        public static Transform fromWireName(String value) {
            for (Transform transform : values()) if (transform.wireName.equals(value)) return transform;
            throw new IllegalArgumentException("Unknown State property transform: " + value);
        }
    }

    public StatePropertyBinding {
        fieldName = StateBinding.privateIdentifier(fieldName, "State field name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(referenceType, "referenceType");
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(comparisonValue, "comparisonValue");
        if ((type == StateBinding.Type.NULLABLE_REFERENCE) != referenceType.isPresent()) {
            throw new IllegalArgumentException("Only a nullable reference field requires a reference type");
        }
        referenceType.ifPresent(reference -> {
            if (reference.access() != PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                    || reference.member().isPresent() || reference.constant().isPresent()) {
                throw new IllegalArgumentException("State reference type requires a simple class, enum or typedef reference");
            }
        });
        if ((transform == Transform.EQUALS) != comparisonValue.isPresent()) {
            throw new IllegalArgumentException("Only Equals requires a comparison value");
        }
        if (transform == Transform.TEXT && type != StateBinding.Type.TEXT_CONTROLLER) {
            throw new IllegalArgumentException("Text requires a TextEditingController field");
        }
        if (transform == Transform.NOT && type != StateBinding.Type.BOOL) {
            throw new IllegalArgumentException("Not requires a non-nullable bool field");
        }
        if (transform == Transform.CLAMP && type != StateBinding.Type.DOUBLE && type != StateBinding.Type.INT) {
            throw new IllegalArgumentException("Clamp requires a non-nullable int or double field");
        }
        comparisonValue.ifPresent(value -> requireComparisonValue(type, value));
    }

    public StatePropertyBinding(String fieldName, StateBinding.Type type,
            Optional<PropertyValue.DartObjectReferenceValue> referenceType, Transform transform) {
        this(fieldName, type, referenceType, transform, Optional.empty());
    }

    static void requireComparisonValue(StateBinding.Type type, PropertyValue value) {
        boolean nullable = type.name().startsWith("NULLABLE_");
        if (value instanceof PropertyValue.NullValue && nullable) return;
        boolean number = value instanceof PropertyValue.IntegerValue || value instanceof PropertyValue.DoubleValue;
        boolean accepted = switch (type) {
            case BOOL, NULLABLE_BOOL -> value instanceof PropertyValue.BooleanValue;
            case STRING, NULLABLE_STRING -> value instanceof PropertyValue.StringValue;
            case INT, NULLABLE_INT -> value instanceof PropertyValue.IntegerValue;
            case DOUBLE, NULLABLE_DOUBLE, NUM, NULLABLE_NUM -> number;
            case NULLABLE_OBJECT -> number || value instanceof PropertyValue.StringValue
                    || value instanceof PropertyValue.BooleanValue;
            case NULLABLE_REFERENCE -> false;
            case RANGE_VALUES, TEXT_CONTROLLER -> false;
        };
        if (!accepted) throw new IllegalArgumentException("Selected/comparison value does not match the closed State field type");
    }

}

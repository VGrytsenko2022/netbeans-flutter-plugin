package dev.flutter.netbeans.designer.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Closed, typed relationship between a controlled widget and user-owned State
 * members. Literal widget properties remain the isolated design-time preview;
 * no Dart declaration, initializer or handler body is stored in this metadata.
 */
public record StateBinding(
        String fieldName,
        String handlerName,
        Type type,
        Optional<PropertyValue.DartObjectReferenceValue> referenceType,
        Optional<PropertyValue> previousOnChanged,
        Action action,
        Optional<PropertyValue> selectedValue) {

    public enum Action {
        CHANGE("change"), TOGGLE("toggle"), SELECT("select");
        private final String wireName;
        Action(String wireName) { this.wireName = wireName; }
        public String wireName() { return wireName; }
        public static Action fromWireName(String value) {
            for (Action action : values()) if (action.wireName.equals(value)) return action;
            throw new IllegalArgumentException("Unknown State binding action: " + value);
        }
    }

    public enum Type {
        BOOL("bool"), NULLABLE_BOOL("nullableBool"), DOUBLE("double"),
        RANGE_VALUES("rangeValues"), NULLABLE_STRING("nullableString"),
        NULLABLE_INT("nullableInt"), NULLABLE_DOUBLE("nullableDouble"),
        NULLABLE_NUM("nullableNum"), NULLABLE_OBJECT("nullableObject"),
        NULLABLE_REFERENCE("nullableReference"), STRING("string"), INT("int"),
        NUM("num"), TEXT_CONTROLLER("textController");

        private final String wireName;

        Type(String wireName) { this.wireName = wireName; }

        public String wireName() { return wireName; }

        public static Type fromWireName(String value) {
            for (Type type : values()) {
                if (type.wireName.equals(value)) return type;
            }
            throw new IllegalArgumentException("Unknown State binding type: " + value);
        }
    }

    public StateBinding {
        fieldName = privateIdentifier(fieldName, "State field name");
        handlerName = privateIdentifier(handlerName, "State handler name");
        if (fieldName.equals(handlerName)) {
            throw new IllegalArgumentException("State field and handler names must be distinct");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(referenceType, "referenceType");
        Objects.requireNonNull(previousOnChanged, "previousOnChanged");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(selectedValue, "selectedValue");
        if ((action == Action.SELECT) != selectedValue.isPresent()) {
            throw new IllegalArgumentException("Only a Select action requires a selected value");
        }
        if (action == Action.TOGGLE && type != Type.BOOL) {
            throw new IllegalArgumentException("Toggle requires a non-nullable bool field");
        }
        selectedValue.ifPresent(value -> StatePropertyBinding.requireComparisonValue(type, value));
        if ((type == Type.NULLABLE_REFERENCE) != referenceType.isPresent()) {
            throw new IllegalArgumentException("Only a nullable reference binding requires a reference type");
        }
        referenceType.ifPresent(reference -> {
            if (reference.access() != PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                    || reference.member().isPresent() || reference.constant().isPresent()) {
                throw new IllegalArgumentException("State reference type must be a simple class, enum or typedef reference");
            }
        });
        previousOnChanged.ifPresent(value -> {
            if (!(value instanceof PropertyValue.CallbackValue)
                    && !(value instanceof PropertyValue.DartObjectReferenceValue)
                    && !(value instanceof PropertyValue.NullValue)
                    && !(value instanceof PropertyValue.StringValue preset && preset.value().equals("noop"))) {
                throw new IllegalArgumentException("Previous On Changed must be a reviewed callback value");
            }
        });
    }

    public StateBinding(String fieldName, String handlerName, Type type) {
        this(fieldName, handlerName, type, Optional.empty(), Optional.empty());
    }

    public StateBinding(String fieldName, String handlerName, Type type,
            Optional<PropertyValue.DartObjectReferenceValue> referenceType,
            Optional<PropertyValue> previousOnChanged) {
        this(fieldName, handlerName, type, referenceType, previousOnChanged, Action.CHANGE, Optional.empty());
    }

    static String privateIdentifier(String value, String label) {
        ModelConstraints.codePointLength(value, label, 2, 128);
        ModelConstraints.dartIdentifier(value, label, false);
        if (!value.matches("_[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + " must be a private Dart identifier");
        }
        return value;
    }
}

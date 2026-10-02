package io.github.vgrytsenko2022.designer.state;

import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.StatePropertyBinding;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Closed Flutter 3.44.8 controlled-value contracts; literals remain Canvas previews. */
public final class WidgetStateBindingCatalog {
    public static final PropertyName ON_CHANGED = new PropertyName("onChanged");
    private static final Set<String> TYPES = Set.of("flutter.material.Checkbox",
            "flutter.material.CheckboxListTile", "flutter.material.Switch", "flutter.material.SwitchListTile", "flutter.material.Slider",
            "flutter.material.RangeSlider", "flutter.widgets.RadioGroup", "flutter.material.RadioListTile", "flutter.material.IconButton",
            "flutter.material.ListTile", "flutter.material.TextField");

    public record Descriptor(StateBinding.Type type,
            Optional<PropertyValue.DartObjectReferenceValue> referenceType,
            List<PropertyName> previewProperties, String dartType, String callbackParameterType,
            PropertyName eventProperty, String runtimeArgumentName) {
        public Descriptor {
            Objects.requireNonNull(type);
            Objects.requireNonNull(referenceType);
            previewProperties = List.copyOf(previewProperties);
            Objects.requireNonNull(dartType);
            Objects.requireNonNull(callbackParameterType);
            Objects.requireNonNull(eventProperty);
            Objects.requireNonNull(runtimeArgumentName);
        }

        public Descriptor(StateBinding.Type type, Optional<PropertyValue.DartObjectReferenceValue> referenceType,
                List<PropertyName> previewProperties, String dartType, String callbackParameterType) {
            this(type, referenceType, previewProperties, dartType, callbackParameterType, ON_CHANGED,
                    type == StateBinding.Type.RANGE_VALUES ? "values" : "value");
        }
    }

    private WidgetStateBindingCatalog() {}

    public static boolean supports(WidgetDefinition definition) {
        return TYPES.contains(definition.typeId().value());
    }

    /** The exact field and callback argument types, not a broad assignable event supertype. */
    public static Optional<Descriptor> find(WidgetNode widget) {
        Objects.requireNonNull(widget, "widget");
        return switch (widget.type().value()) {
            case "flutter.material.Checkbox", "flutter.material.CheckboxListTile" -> {
                boolean nullable = new PropertyValue.BooleanValue(true).equals(
                        widget.properties().get(new PropertyName("tristate")));
                yield Optional.of(descriptor(nullable ? StateBinding.Type.NULLABLE_BOOL : StateBinding.Type.BOOL,
                        nullable ? "bool?" : "bool", "bool?", "value"));
            }
            case "flutter.material.Switch", "flutter.material.SwitchListTile" -> Optional.of(descriptor(StateBinding.Type.BOOL, "bool", "bool", "value"));
            case "flutter.material.Slider" -> Optional.of(descriptor(StateBinding.Type.DOUBLE, "double", "double", "value"));
            case "flutter.material.RangeSlider" -> Optional.of(descriptor(StateBinding.Type.RANGE_VALUES,
                    "RangeValues", "RangeValues", "valuesStart", "valuesEnd"));
            case "flutter.widgets.RadioGroup", "flutter.material.RadioListTile" -> Optional.of(radioDescriptor(widget));
            case "flutter.material.TextField" -> Optional.of(new Descriptor(StateBinding.Type.TEXT_CONTROLLER,
                    Optional.empty(), List.of(), "TextEditingController", "String", ON_CHANGED, "controller"));
            case "flutter.material.IconButton" -> widget.properties().get(new PropertyName("isSelected")) instanceof PropertyValue.BooleanValue
                    ? Optional.of(new Descriptor(StateBinding.Type.BOOL, Optional.empty(), List.of(new PropertyName("isSelected")),
                            "bool", "", new PropertyName("onPressed"), "isSelected")) : Optional.empty();
            case "flutter.material.ListTile" -> {
                StateBinding retained = widget.stateBinding().orElse(null);
                StateBinding.Type type = retained != null && retained.action() == StateBinding.Action.SELECT
                        ? retained.type() : StateBinding.Type.BOOL;
                Optional<PropertyValue.DartObjectReferenceValue> reference = retained == null ? Optional.empty() : retained.referenceType();
                yield Optional.of(new Descriptor(type, reference, List.of(new PropertyName("selected")),
                        dartType(type, reference), "", new PropertyName("onTap"), "selected"));
            }
            default -> Optional.empty();
        };
    }

    public static StateBinding createBinding(WidgetNode widget, String fieldName, String handlerName) {
        StateBinding.Action action = Set.of("flutter.material.IconButton", "flutter.material.ListTile")
                .contains(widget.type().value()) ? StateBinding.Action.TOGGLE : StateBinding.Action.CHANGE;
        return createBinding(widget, fieldName, handlerName, action, Optional.empty(), Optional.empty());
    }

    public static StateBinding createBinding(WidgetNode widget, String fieldName, String handlerName,
            StateBinding.Action action, Optional<PropertyValue> selectedValue, Optional<StatePropertyBinding> reusedField) {
        if (widget.stateBinding().isPresent()) {
            throw new IllegalArgumentException("This widget already has a State binding. Remove that binding first.");
        }
        Descriptor descriptor = find(widget).orElseThrow(() -> new IllegalArgumentException(
                "State binding is not supported for " + widget.type().value() + "."));
        PropertyValue previous = widget.properties().get(descriptor.eventProperty());
        if (previous != null && !(previous instanceof PropertyValue.NullValue)
                && !(previous instanceof PropertyValue.StringValue preset && preset.value().equals("noop"))) {
            throw new IllegalArgumentException("Cannot create State binding: " + descriptor.eventProperty().value() + " already has an independent handler. "
                    + "Disconnect it explicitly first; existing handler code will be preserved.");
        }
        StateBinding.Type inferred = action == StateBinding.Action.SELECT && selectedValue.isPresent()
                ? selectedType(selectedValue.orElseThrow()) : descriptor.type();
        StateBinding.Type type = reusedField.map(StatePropertyBinding::type).orElse(inferred);
        Optional<PropertyValue.DartObjectReferenceValue> reference = reusedField.isPresent()
                ? reusedField.orElseThrow().referenceType() : descriptor.referenceType();
        if (reusedField.isPresent() && !reusedField.orElseThrow().fieldName().equals(fieldName)) {
            throw new IllegalArgumentException("The selected source field name differs from the binding field");
        }
        StateBinding binding = new StateBinding(fieldName, handlerName, type, reference, Optional.ofNullable(previous), action, selectedValue);
        WidgetNode prospective = new WidgetNode(widget.id(), widget.type(), widget.properties(), widget.slots(),
                widget.extensions(), Optional.of(binding), widget.propertyBindings());
        validationError(prospective).ifPresent(reason -> { throw new IllegalArgumentException(reason); });
        return binding;
    }

    /** Metadata/companion validation only; source ownership and Dart proof belong to admission. */
    public static Optional<String> validationError(WidgetNode widget) {
        if (widget.stateBinding().isEmpty()) return Optional.empty();
        try {
            Optional<Descriptor> found = find(widget);
            if (found.isEmpty()) return Optional.of("State binding is not supported for " + widget.type().value() + ".");
            Descriptor expected = found.orElseThrow();
            StateBinding actual = widget.stateBinding().orElseThrow();
            boolean actionAccepted = switch (widget.type().value()) {
                case "flutter.material.IconButton" -> actual.action() == StateBinding.Action.TOGGLE;
                case "flutter.material.ListTile" -> actual.action() == StateBinding.Action.TOGGLE
                        || actual.action() == StateBinding.Action.SELECT
                                && actual.type() != StateBinding.Type.TEXT_CONTROLLER && actual.type() != StateBinding.Type.RANGE_VALUES;
                default -> actual.action() == StateBinding.Action.CHANGE;
            };
            if (!actionAccepted) return Optional.of("The State action is not supported by this widget's reviewed event contract.");
            if (actual.type() != expected.type() || !actual.referenceType().equals(expected.referenceType())) {
                return Optional.of("State binding type does not match the current controlled value type. "
                        + "Remove the binding before changing Tristate or Value Type, then create the new typed binding.");
            }
            return Optional.empty();
        } catch (IllegalArgumentException error) {
            return Optional.of(error.getMessage());
        }
    }

    public static boolean isBoundPreview(WidgetNode widget, PropertyName property) {
        return widget.propertyBindings().containsKey(property) || widget.stateBinding().isPresent()
                && find(widget).map(value -> value.previewProperties().contains(property)).orElse(false);
    }

    private static StateBinding.Type selectedType(PropertyValue value) {
        if (value instanceof PropertyValue.BooleanValue) return StateBinding.Type.NULLABLE_BOOL;
        if (value instanceof PropertyValue.StringValue) return StateBinding.Type.NULLABLE_STRING;
        if (value instanceof PropertyValue.IntegerValue) return StateBinding.Type.NULLABLE_INT;
        if (value instanceof PropertyValue.DoubleValue) return StateBinding.Type.NULLABLE_DOUBLE;
        if (value instanceof PropertyValue.NullValue) return StateBinding.Type.NULLABLE_OBJECT;
        throw new IllegalArgumentException("Select requires a closed scalar selected value");
    }

    private static Descriptor radioDescriptor(WidgetNode widget) {
        String family = widget.type().value().equals("flutter.material.RadioListTile") ? "RadioListTile" : "RadioGroup";
        PropertyValue value = widget.properties().get(new PropertyName("valueType"));
        if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
            if (reference.access() != PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                    || reference.member().isPresent() || reference.constant().isPresent()) {
                throw new IllegalArgumentException(family + " State binding needs a simple class, enum or typedef Value Type.");
            }
            return new Descriptor(StateBinding.Type.NULLABLE_REFERENCE, Optional.of(reference),
                    List.of(new PropertyName("groupValue")), reference.rootSymbol() + "?", reference.rootSymbol() + "?",
                    ON_CHANGED, "groupValue");
        }
        if (!(value instanceof PropertyValue.StringValue builtin)) {
            throw new IllegalArgumentException("Set a supported " + family + " Value Type before creating a State binding.");
        }
        StateBinding.Type type = switch (builtin.value()) {
            case "String" -> StateBinding.Type.NULLABLE_STRING;
            case "int" -> StateBinding.Type.NULLABLE_INT;
            case "double" -> StateBinding.Type.NULLABLE_DOUBLE;
            case "num" -> StateBinding.Type.NULLABLE_NUM;
            case "bool" -> StateBinding.Type.NULLABLE_BOOL;
            case "Object" -> StateBinding.Type.NULLABLE_OBJECT;
            default -> throw new IllegalArgumentException("Unsupported " + family + " State Value Type: " + builtin.value());
        };
        return new Descriptor(type, Optional.empty(), List.of(new PropertyName("groupValue")),
                builtin.value() + "?", builtin.value() + "?", ON_CHANGED, "groupValue");
    }

    private static Descriptor descriptor(StateBinding.Type type, String dartType, String callbackType, String... properties) {
        return new Descriptor(type, Optional.empty(), java.util.Arrays.stream(properties).map(PropertyName::new).toList(),
                dartType, callbackType);
    }

    public static String dartType(StateBinding.Type type, Optional<PropertyValue.DartObjectReferenceValue> reference) {
        return switch (type) {
            case BOOL -> "bool"; case NULLABLE_BOOL -> "bool?";
            case STRING -> "String"; case NULLABLE_STRING -> "String?";
            case INT -> "int"; case NULLABLE_INT -> "int?";
            case DOUBLE -> "double"; case NULLABLE_DOUBLE -> "double?";
            case NUM -> "num"; case NULLABLE_NUM -> "num?";
            case NULLABLE_OBJECT -> "Object?";
            case NULLABLE_REFERENCE -> reference.orElseThrow().rootSymbol() + "?";
            case RANGE_VALUES -> "RangeValues"; case TEXT_CONTROLLER -> "TextEditingController";
        };
    }
}

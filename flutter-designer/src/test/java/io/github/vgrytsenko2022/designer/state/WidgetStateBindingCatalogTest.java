package io.github.vgrytsenko2022.designer.state;

import static org.junit.jupiter.api.Assertions.*;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.StateBinding;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WidgetStateBindingCatalogTest {
    private static final StableId ID = StableId.parse("e8717a85-6737-4665-a414-28cceab8a2d1");

    @Test
    void exactlyElevenExistingControlsExposeReviewedContracts() {
        var definitions = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetStateBindingCatalog::supports).toList();
        assertEquals(11, definitions.size());
        for (var definition : definitions) {
            var widget = WidgetNodePrototypeFactory.create(definition, ID);
            if (widget.type().value().equals("flutter.material.IconButton")) {
                widget = value(widget, "isSelected", new PropertyValue.BooleanValue(false));
            }
            var contract = WidgetStateBindingCatalog.find(widget).orElseThrow();
            assertEquals(widget.type().value().equals("flutter.material.TextField"), contract.previewProperties().isEmpty());
            assertTrue(contract.previewProperties().stream().allMatch(name -> definition.property(name).isPresent()));
            var binding = WidgetStateBindingCatalog.createBinding(widget, "_value", "_changed");
            assertEquals(Optional.ofNullable(widget.properties().get(contract.eventProperty())), binding.previousOnChanged());
            assertEquals(Optional.empty(), WidgetStateBindingCatalog.validationError(bound(widget, binding)));
        }
        assertFalse(WidgetStateBindingCatalog.supports(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.Radio")).orElseThrow()));
    }

    @Test
    void checkboxNullabilityAndRangePairDifferFromSwitchAndSlider() {
        for (String type : List.of("Checkbox", "CheckboxListTile")) {
            var binary = widget("flutter.material." + type);
            var contract = WidgetStateBindingCatalog.find(binary).orElseThrow();
            assertEquals("bool", contract.dartType());
            assertEquals("bool?", contract.callbackParameterType());
            var nullable = value(binary, "tristate", new PropertyValue.BooleanValue(true));
            assertEquals("bool?", WidgetStateBindingCatalog.find(nullable).orElseThrow().dartType());
            var bound = bound(nullable, WidgetStateBindingCatalog.createBinding(nullable, "_value", "_changed"));
            assertTrue(WidgetStateBindingCatalog.validationError(value(bound, "tristate", new PropertyValue.BooleanValue(false)))
                    .orElseThrow().contains("Remove the binding"));
        }
        assertEquals("bool", WidgetStateBindingCatalog.find(widget("flutter.material.Switch")).orElseThrow().callbackParameterType());
        assertEquals("double", WidgetStateBindingCatalog.find(widget("flutter.material.Slider")).orElseThrow().dartType());
        var range = WidgetStateBindingCatalog.find(widget("flutter.material.RangeSlider")).orElseThrow();
        assertEquals(StateBinding.Type.RANGE_VALUES, range.type());
        assertEquals(List.of(new PropertyName("valuesStart"), new PropertyName("valuesEnd")), range.previewProperties());
    }

    @Test
    void everyRadioGenericUsesConcreteNullableAssignmentType() {
        var group = widget("flutter.widgets.RadioGroup");
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            var typed = value(group, "valueType", new PropertyValue.StringValue(type));
            var contract = WidgetStateBindingCatalog.find(typed).orElseThrow();
            assertEquals(type + "?", contract.dartType());
            assertEquals(type + "?", contract.callbackParameterType());
            assertEquals(contract, WidgetStateBindingCatalog.find(value(typed, "nullableValueType", new PropertyValue.BooleanValue(true))).orElseThrow());
        }
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/choice.dart"),
                "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var typed = value(group, "valueType", reference);
        var contract = WidgetStateBindingCatalog.find(typed).orElseThrow();
        assertEquals(StateBinding.Type.NULLABLE_REFERENCE, contract.type());
        assertEquals(Optional.of(reference), contract.referenceType());
        assertEquals("Choice?", contract.dartType());
        var bound = bound(typed, WidgetStateBindingCatalog.createBinding(typed, "_choice", "_changed"));
        assertTrue(WidgetStateBindingCatalog.validationError(value(bound, "valueType", new PropertyValue.StringValue("String"))).isPresent());
    }

    @Test
    void independentCallbacksAreNotOverwrittenAndNoopNullOmissionAreRetained() {
        var widget = widget("flutter.material.Switch");
        assertEquals(Optional.empty(), WidgetStateBindingCatalog.createBinding(widget, "_value", "_changed").previousOnChanged());
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("noop"))) {
            var previous = value(widget, "onChanged", value);
            assertEquals(Optional.of(value), WidgetStateBindingCatalog.createBinding(previous, "_value", "_changed").previousOnChanged());
        }
        var custom = value(widget, "onChanged", new PropertyValue.CallbackValue("_custom"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> WidgetStateBindingCatalog.createBinding(custom, "_value", "_changed")).getMessage().contains("Disconnect"));
        var bound = bound(widget, WidgetStateBindingCatalog.createBinding(widget, "_value", "_changed"));
        assertTrue(WidgetStateBindingCatalog.validationError(value(bound, "onChanged", new PropertyValue.CallbackValue("_later"))).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WidgetStateBindingCatalog.createBinding(bound, "_other", "_otherChanged"));
        assertTrue(WidgetStateBindingCatalog.isBoundPreview(bound, new PropertyName("value")));
        assertFalse(WidgetStateBindingCatalog.isBoundPreview(bound, new PropertyName("enabled")));
    }

    @Test
    void unsupportedWidgetsAndMalformedRadioTypesCannotRetainBindingMetadata() {
        var binding = new StateBinding("_value", "_changed", StateBinding.Type.BOOL);
        assertTrue(WidgetStateBindingCatalog.validationError(bound(widget("flutter.material.TextField"), binding)).isPresent());
        var malformed = value(widget("flutter.widgets.RadioGroup"), "valueType", new PropertyValue.StringValue("dynamic"));
        assertThrows(IllegalArgumentException.class, () -> WidgetStateBindingCatalog.find(malformed));
        assertTrue(WidgetStateBindingCatalog.validationError(bound(malformed, binding)).isPresent());
    }

    private static WidgetNode widget(String type) {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow(), ID);
    }

    private static WidgetNode value(WidgetNode widget, String name, PropertyValue value) {
        var values = new LinkedHashMap<>(widget.properties());
        values.put(new PropertyName(name), value);
        return new WidgetNode(widget.id(), widget.type(), values, widget.slots(), widget.extensions(), widget.stateBinding());
    }

    private static WidgetNode bound(WidgetNode widget, StateBinding binding) {
        return new WidgetNode(widget.id(), widget.type(), widget.properties(), widget.slots(), widget.extensions(), Optional.of(binding));
    }
}

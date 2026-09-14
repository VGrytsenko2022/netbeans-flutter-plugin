package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateBindingValidationTest {
    @Test
    void allSixControlledWidgetsKeepTheirReviewedLiteralPreviewValidation() {
        for (String type : List.of("flutter.material.Checkbox", "flutter.material.CheckboxListTile",
                "flutter.material.Switch", "flutter.material.Slider", "flutter.material.RangeSlider",
                "flutter.widgets.RadioGroup")) {
            WidgetNode node = prototype(type, "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
            if (type.equals("flutter.widgets.RadioGroup")) {
                node = new WidgetNode(node.id(), node.type(), node.properties(),
                        Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(
                                prototype("flutter.widgets.Text", "cccccccc-cccc-4ccc-8ccc-cccccccccccc"))),
                        node.extensions());
            }
            StateBinding binding = WidgetStateBindingCatalog.createBinding(node, "_value", "_changed");
            WidgetNode bound = bind(node, binding);
            ValidationResult result = validate(bound, WidgetClassKind.STATEFUL);
            assertTrue(result.valid(), () -> type + ": " + result.issues());
            assertEquals(node.properties(), bound.properties());
        }
    }

    @Test
    void rejectsStatelessAndUnsupportedWidgetBindings() {
        WidgetNode checkbox = bind(prototype("flutter.material.Checkbox", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new StateBinding("_value", "_changed", StateBinding.Type.BOOL));
        assertStateError(validate(checkbox, WidgetClassKind.STATELESS));
        WidgetNode text = bind(prototype("flutter.widgets.Text", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new StateBinding("_value", "_changed", StateBinding.Type.BOOL));
        assertStateError(validate(text, WidgetClassKind.STATEFUL));
    }

    @Test
    void changingTristateCannotInvalidateTheRetainedFieldType() {
        WidgetNode checkbox = prototype("flutter.material.Checkbox", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        var properties = new LinkedHashMap<>(checkbox.properties());
        properties.put(new PropertyName("tristate"), new PropertyValue.BooleanValue(true));
        WidgetNode changed = new WidgetNode(checkbox.id(), checkbox.type(), properties, checkbox.slots(),
                checkbox.extensions(), Optional.of(new StateBinding("_value", "_changed", StateBinding.Type.BOOL)));
        assertStateError(validate(changed, WidgetClassKind.STATEFUL));
        assertTrue(validate(bind(changed, new StateBinding("_value", "_changed", StateBinding.Type.NULLABLE_BOOL)),
                WidgetClassKind.STATEFUL).valid());
    }

    @Test
    void duplicateStateMembersAreRejectedAcrossDifferentWidgetsAndMemberKinds() {
        WidgetNode first = bind(prototype("flutter.material.Switch", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new StateBinding("_first", "_firstChanged", StateBinding.Type.BOOL));
        WidgetNode second = bind(prototype("flutter.material.Checkbox", "cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                new StateBinding("_firstChanged", "_secondChanged", StateBinding.Type.BOOL));
        WidgetNode column = new WidgetNode(StableId.parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, second))));
        assertStateError(validate(column, WidgetClassKind.STATEFUL));
    }

    @Test
    void ordinaryIndependentOnChangedEditsDoNotDiscardTheStateBinding() {
        WidgetNode checkbox = prototype("flutter.material.Checkbox", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        var properties = new LinkedHashMap<>(checkbox.properties());
        properties.put(new PropertyName("onChanged"), new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                "_independentChanged", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        WidgetNode node = new WidgetNode(checkbox.id(), checkbox.type(), properties, checkbox.slots(), checkbox.extensions(),
                Optional.of(new StateBinding("_value", "_originalChanged", StateBinding.Type.BOOL)));
        assertTrue(validate(node, WidgetClassKind.STATEFUL).valid());
    }

    @Test
    void bindingDoesNotBypassPreviewRangeChecksOrPreviousCallbackConstraints() {
        WidgetNode slider = prototype("flutter.material.Slider", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        var properties = new LinkedHashMap<>(slider.properties());
        properties.put(new PropertyName("value"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(20)));
        WidgetNode invalidPreview = new WidgetNode(slider.id(), slider.type(), properties, slider.slots(), slider.extensions(),
                Optional.of(new StateBinding("_value", "_changed", StateBinding.Type.DOUBLE)));
        assertFalse(validate(invalidPreview, WidgetClassKind.STATEFUL).valid());
        WidgetNode invalidPrevious = bind(slider, new StateBinding("_value", "_changed", StateBinding.Type.DOUBLE,
                Optional.empty(), Optional.of(new PropertyValue.NullValue())));
        assertStateError(validate(invalidPrevious, WidgetClassKind.STATEFUL));
    }

    @Test
    void sharedFieldTypesAreConsistentAcrossActionsAndIndependentConsumers() {
        WidgetNode first = bind(prototype("flutter.material.Switch", "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                new StateBinding("_shared", "_firstChanged", StateBinding.Type.BOOL));
        WidgetNode second = bind(prototype("flutter.material.Checkbox", "cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                new StateBinding("_shared", "_secondChanged", StateBinding.Type.BOOL));
        WidgetNode column = new WidgetNode(StableId.parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, second))));
        assertTrue(validate(column, WidgetClassKind.STATEFUL).valid());
        var properties = new LinkedHashMap<>(second.properties());
        properties.put(new PropertyName("tristate"), new PropertyValue.BooleanValue(true));
        WidgetNode conflicting = new WidgetNode(second.id(), second.type(), properties, second.slots(), second.extensions(),
                Optional.of(new StateBinding("_shared", "_secondChanged", StateBinding.Type.NULLABLE_BOOL)));
        WidgetNode invalid = new WidgetNode(column.id(), column.type(), column.properties(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, conflicting))));
        assertStateError(validate(invalid, WidgetClassKind.STATEFUL));
        var consumer = new StatePropertyBinding("_shared", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.TO_STRING);
        WidgetNode text = prototype("flutter.widgets.Text", "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee");
        text = new WidgetNode(text.id(), text.type(), text.properties(), text.slots(), text.extensions(), Optional.empty(),
                Map.of(new PropertyName("data"), consumer));
        assertTrue(validate(text, WidgetClassKind.STATEFUL).valid(), "Consumer can refer to an independently declared source field");
        assertStateError(validate(text, WidgetClassKind.STATELESS));
        WidgetNode shared = new WidgetNode(column.id(), column.type(), column.properties(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, second, text))));
        assertTrue(validate(shared, WidgetClassKind.STATEFUL).valid());
    }

    private static WidgetNode prototype(String type, String id) {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow(),
                StableId.parse(id));
    }

    private static WidgetNode bind(WidgetNode node, StateBinding binding) {
        return new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(), Optional.of(binding));
    }

    private static ValidationResult validate(WidgetNode root, WidgetClassKind kind) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        var source = new DartSourceDescriptor("page.dart", "Page", kind, Optional.empty(), new ManagedRegions(region, region));
        return new WidgetTreeValidator().validate(new DesignerDocument(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), source, root), BuiltInWidgetCatalog.getDefault());
    }

    private static void assertStateError(ValidationResult result) {
        assertTrue(result.errors().stream().anyMatch(issue -> issue.code().equals(WidgetTreeValidator.STATE_BINDING)),
                () -> result.issues().toString());
    }
}

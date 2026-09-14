package dev.flutter.netbeans.designer.state;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WidgetStatePropertyBindingCatalogTest {
    private static final StableId ID = StableId.parse("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee");

    @Test
    void reviewedDirectConsumersCoverPaletteWithoutInferringSyntheticFlags() {
        int widgets = 0;
        int properties = 0;
        for (var definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            WidgetNode widget = WidgetNode.empty(ID, definition.typeId());
            var descriptors = WidgetStatePropertyBindingCatalog.descriptors(widget);
            if (!descriptors.isEmpty()) widgets++;
            properties += descriptors.size();
            for (var descriptor : descriptors) {
                assertTrue(definition.property(descriptor.propertyName()).isPresent());
                assertFalse(descriptor.allowedTransforms().isEmpty());
            }
        }
        assertEquals(56, widgets, "Reviewed widget-type coverage must change intentionally");
        assertEquals(168, properties, "Reviewed runtime-field coverage must change intentionally");
        for (String excluded : List.of("maxLines", "minLines", "maxLength", "obscureText", "expands", "obscuringCharacter")) {
            assertTrue(WidgetStatePropertyBindingCatalog.find(widget("material.TextField"), new PropertyName(excluded)).isEmpty(), excluded);
        }
        assertTrue(WidgetStatePropertyBindingCatalog.find(widget("material.Slider"), new PropertyName("min")).isEmpty());
        assertTrue(WidgetStatePropertyBindingCatalog.find(widget("material.ElevatedButton"), new PropertyName("enabled")).isEmpty());
        assertTrue(WidgetStatePropertyBindingCatalog.find(widget("widgets.IconTheme"), new PropertyName("applyTextScaling")).isEmpty());
    }

    @Test
    void tooltipThemeConsumesOnlyThreeReviewedNullableDataFields() {
        var theme = widget("material.TooltipTheme");
        assertTrue(WidgetStateBindingCatalog.find(theme).isEmpty());
        assertEquals(3, WidgetStatePropertyBindingCatalog.descriptors(theme).size());
        for (String name : List.of("preferBelow", "excludeFromSemantics", "enableFeedback")) {
            valid("material.TooltipTheme", name, binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
            valid("material.TooltipTheme", name, binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.DIRECT));
            valid("material.TooltipTheme", name, binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT));
            valid("material.TooltipTheme", name, new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT,
                    Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))));
            invalid("material.TooltipTheme", name, binding(StateBinding.Type.STRING, StatePropertyBinding.Transform.DIRECT));
        }
        for (String name : List.of("data", "textStyleInherit", "textStyleDecorationUnderline", "height", "visible", "onTriggered", "triggerMode")) {
            assertTrue(WidgetStatePropertyBindingCatalog.find(theme, new PropertyName(name)).isEmpty(), name);
        }
    }

    @Test
    void tooltipVisibilityConsumesOnlyVisibleWithExactNonnullableBooleanTransforms() {
        var scope = widget("material.TooltipVisibility");
        assertTrue(WidgetStateBindingCatalog.find(scope).isEmpty());
        valid("material.TooltipVisibility", "visible", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
        valid("material.TooltipVisibility", "visible", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT));
        valid("material.TooltipVisibility", "visible", new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT,
                Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))));
        invalid("material.TooltipVisibility", "visible", binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.DIRECT));
        invalid("material.TooltipVisibility", "visible", binding(StateBinding.Type.STRING, StatePropertyBinding.Transform.DIRECT));
        for (String name : List.of("onChanged", "onTriggered", "enabled", "maintainSemantics", "child")) {
            assertTrue(WidgetStatePropertyBindingCatalog.find(scope, new PropertyName(name)).isEmpty());
        }
    }

    @Test
    void mouseRegionConsumesOnlyOpaqueWithDirectNotOrEqualsAndNeverInventsAStateAction() {
        var mouse = widget("widgets.MouseRegion");
        assertTrue(WidgetStateBindingCatalog.find(mouse).isEmpty());
        assertEquals(List.of(new PropertyName("opaque")), WidgetStatePropertyBindingCatalog.descriptors(mouse).stream()
                .map(WidgetStatePropertyBindingCatalog.Descriptor::propertyName).toList());
        valid("widgets.MouseRegion", "opaque", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
        valid("widgets.MouseRegion", "opaque", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT));
        valid("widgets.MouseRegion", "opaque", new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT,
                Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))));
        invalid("widgets.MouseRegion", "opaque", binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.DIRECT));
        for (String name : List.of("onEnter", "onExit", "onHover", "cursor", "hitTestBehavior")) {
            assertTrue(WidgetStatePropertyBindingCatalog.find(mouse, new PropertyName(name)).isEmpty());
        }
    }

    @Test
    void gestureDetectorConsumesOnlyReviewedBooleanConfigurationWithoutInventingStateActions() {
        var gesture = widget("widgets.GestureDetector");
        assertTrue(WidgetStateBindingCatalog.find(gesture).isEmpty(), "Gesture callbacks remain ordinary user-owned events, not implicit State producers.");
        assertEquals(java.util.Set.of("excludeFromSemantics", "trackpadScrollCausesScale"),
                WidgetStatePropertyBindingCatalog.descriptors(gesture).stream().map(value -> value.propertyName().value())
                        .collect(java.util.stream.Collectors.toSet()));
        for (String property : List.of("excludeFromSemantics", "trackpadScrollCausesScale")) {
            valid("widgets.GestureDetector", property, binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
            valid("widgets.GestureDetector", property, binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT));
            invalid("widgets.GestureDetector", property, binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.DIRECT));
            invalid("widgets.GestureDetector", property, binding(StateBinding.Type.STRING, StatePropertyBinding.Transform.DIRECT));
        }
        for (String property : List.of("onTap", "onScaleStart", "behavior", "supportedDevices", "trackpadScrollToScaleFactor")) {
            assertTrue(WidgetStatePropertyBindingCatalog.find(gesture, new PropertyName(property)).isEmpty(), property);
        }
    }

    @Test
    void textControllerStringificationBooleanNegationAndScalarEqualityHaveClosedTypes() {
        valid("widgets.Text", "data", binding(StateBinding.Type.TEXT_CONTROLLER, StatePropertyBinding.Transform.TEXT));
        valid("widgets.Text", "data", binding(StateBinding.Type.NULLABLE_INT, StatePropertyBinding.Transform.TO_STRING));
        valid("widgets.Visibility", "visible", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.NOT));
        valid("widgets.Offstage", "offstage", new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT,
                Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))));
        invalid("widgets.Text", "data", binding(StateBinding.Type.NULLABLE_STRING, StatePropertyBinding.Transform.DIRECT));
        invalid("widgets.Visibility", "visible", binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.DIRECT));
        invalid("widgets.Text", "data", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
        assertThrows(IllegalArgumentException.class, () -> binding(StateBinding.Type.STRING, StatePropertyBinding.Transform.TEXT));
        assertThrows(IllegalArgumentException.class, () -> binding(StateBinding.Type.NULLABLE_BOOL, StatePropertyBinding.Transform.NOT));
        assertThrows(IllegalArgumentException.class, () -> new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_OBJECT,
                Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.EnumValue("Choice", "first"))));
    }

    @Test
    void numericRuntimeBoundsRequireExplicitClampAndCorrectNonnullableField() {
        for (String type : List.of("widgets.Opacity", "material.LinearProgressIndicator", "material.CircularProgressIndicator", "material.RefreshProgressIndicator")) {
            String property = type.equals("widgets.Opacity") ? "opacity" : "value";
            valid(type, property, binding(StateBinding.Type.DOUBLE, StatePropertyBinding.Transform.CLAMP));
            invalid(type, property, binding(StateBinding.Type.DOUBLE, StatePropertyBinding.Transform.DIRECT));
            invalid(type, property, binding(StateBinding.Type.INT, StatePropertyBinding.Transform.CLAMP));
        }
        valid("widgets.IndexedStack", "index", binding(StateBinding.Type.INT, StatePropertyBinding.Transform.CLAMP));
        invalid("widgets.IndexedStack", "index", binding(StateBinding.Type.INT, StatePropertyBinding.Transform.DIRECT));
        assertThrows(IllegalArgumentException.class, () -> binding(StateBinding.Type.NULLABLE_DOUBLE, StatePropertyBinding.Transform.CLAMP));
    }

    @Test
    void progressControllerAndTwoBindingsToSameRuntimeArgumentAreRejected() {
        WidgetNode progress = withBinding(widget("material.CircularProgressIndicator"), "value",
                binding(StateBinding.Type.DOUBLE, StatePropertyBinding.Transform.CLAMP));
        var props = new LinkedHashMap<>(progress.properties());
        props.put(new PropertyName("controller"), new PropertyValue.DartObjectReferenceValue(Optional.empty(),
                "_animation", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        progress = new WidgetNode(progress.id(), progress.type(), props, progress.slots(), progress.extensions(),
                progress.stateBinding(), progress.propertyBindings());
        assertTrue(WidgetStatePropertyBindingCatalog.validationError(progress).orElseThrow().contains("mutually exclusive"));
        WidgetNode tile = withBinding(widget("material.ListTile"), "selected", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
        tile = new WidgetNode(tile.id(), tile.type(), tile.properties(), tile.slots(), tile.extensions(), Optional.of(
                new StateBinding("_other", "_onTap", StateBinding.Type.BOOL, Optional.empty(), Optional.empty(),
                        StateBinding.Action.TOGGLE, Optional.empty())), tile.propertyBindings());
        assertTrue(WidgetStatePropertyBindingCatalog.validationError(tile).isPresent());
    }

    @Test
    void listTileSelectInfersNullableScalarButCanReuseExactNonnullableField() {
        WidgetNode tile = widget("material.ListTile");
        var value = Optional.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.ONE));
        var created = WidgetStateBindingCatalog.createBinding(tile, "_choice", "_choose", StateBinding.Action.SELECT, value, Optional.empty());
        assertEquals(StateBinding.Type.NULLABLE_INT, created.type());
        var reused = WidgetStateBindingCatalog.createBinding(tile, "_choice", "_choose", StateBinding.Action.SELECT, value,
                Optional.of(new StatePropertyBinding("_choice", StateBinding.Type.INT, Optional.empty(), StatePropertyBinding.Transform.DIRECT)));
        assertEquals(StateBinding.Type.INT, reused.type());
    }

    @Test
    void floatingActionButtonArgumentsFollowSelectedConstructorEvenWithoutLiteralPreview() {
        for (String variant : List.of("standard", "small", "large", "extended")) {
            WidgetNode fab = widget("material.FloatingActionButton");
            var props = new LinkedHashMap<>(fab.properties());
            props.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            props.remove(new PropertyName("mini"));
            props.remove(new PropertyName("isExtended"));
            fab = new WidgetNode(fab.id(), fab.type(), props, fab.slots());
            assertEquals(variant.equals("standard"), WidgetStatePropertyBindingCatalog.find(fab, new PropertyName("mini")).isPresent());
            assertEquals(variant.equals("standard") || variant.equals("extended"),
                    WidgetStatePropertyBindingCatalog.find(fab, new PropertyName("isExtended")).isPresent());
            var boundMini = withBinding(fab, "mini", binding(StateBinding.Type.BOOL, StatePropertyBinding.Transform.DIRECT));
            assertEquals(!variant.equals("standard"), WidgetStatePropertyBindingCatalog.validationError(boundMini).isPresent());
            assertTrue(WidgetStatePropertyBindingCatalog.find(fab, new PropertyName("tooltip")).isPresent());
        }
    }

    private static StatePropertyBinding binding(StateBinding.Type type, StatePropertyBinding.Transform transform) {
        return new StatePropertyBinding("_value", type, Optional.empty(), transform);
    }
    private static WidgetNode widget(String type) {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter." + type)).orElseThrow(), ID);
    }
    private static WidgetNode withBinding(WidgetNode node, String property, StatePropertyBinding binding) {
        return new WidgetNode(node.id(), node.type(), node.properties(), node.slots(), node.extensions(), node.stateBinding(),
                Map.of(new PropertyName(property), binding));
    }
    private static void valid(String type, String property, StatePropertyBinding binding) {
        assertEquals(Optional.empty(), WidgetStatePropertyBindingCatalog.validationError(withBinding(widget(type), property, binding)));
    }
    private static void invalid(String type, String property, StatePropertyBinding binding) {
        assertTrue(WidgetStatePropertyBindingCatalog.validationError(withBinding(widget(type), property, binding)).isPresent());
    }
}

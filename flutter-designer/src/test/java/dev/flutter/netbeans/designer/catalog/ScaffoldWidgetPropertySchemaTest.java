package dev.flutter.netbeans.designer.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScaffoldWidgetPropertySchemaTest {

    @Test
    void exposesExactFlutter3448ClosedConstructorSurfaceAndExistingSlots() {
        WidgetDefinition scaffold = BuiltInWidgetCatalog.getDefault()
                .find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow();

        assertEquals(ScaffoldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                scaffold.properties().size());
        assertEquals(ScaffoldWidgetPropertySchema.definitions().keySet(),
                scaffold.properties().stream()
                        .map(property -> property.name().value())
                        .collect(Collectors.toUnmodifiableSet()));
        assertEquals(List.of("appBar", "body", "floatingActionButton"),
                scaffold.slots().stream().map(slot -> slot.name().value()).toList());
        assertEquals(ScaffoldWidgetPropertySchema.SLOT_COUNT, scaffold.slots().size());
        assertTrue(scaffold.properties().stream()
                .allMatch(property -> property.creationDefault().isEmpty()));
    }

    @Test
    void staticPresetsAreCompleteClosedPublicFlutterConstants() {
        assertEquals(List.of(
                        "startTop", "miniStartTop", "centerTop", "miniCenterTop",
                        "endTop", "miniEndTop", "startFloat", "miniStartFloat",
                        "centerFloat", "miniCenterFloat", "endFloat", "miniEndFloat",
                        "startDocked", "miniStartDocked", "centerDocked",
                        "miniCenterDocked", "endDocked", "miniEndDocked", "endContained"),
                definition("floatingActionButtonLocation").presets());
        assertEquals(List.of("scaling", "noAnimation"),
                definition("floatingActionButtonAnimator").presets());
        assertEquals(List.of(
                        "topStart", "topCenter", "topEnd", "centerStart", "center",
                        "centerEnd", "bottomStart", "bottomCenter", "bottomEnd"),
                definition("persistentFooterAlignment").presets());
        assertTrue(ScaffoldWidgetPropertySchema.isStaticPreset(
                new PropertyName("floatingActionButtonLocation")));
        assertFalse(ScaffoldWidgetPropertySchema.isStaticPreset(
                new PropertyName("backgroundColor")));
    }

    @Test
    void scalarConstraintsRemainTypedAndBounded() {
        WidgetDefinition scaffold = BuiltInWidgetCatalog.getDefault()
                .find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow();

        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                scaffold.property(new PropertyName("backgroundColor"))
                        .orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                scaffold.property(new PropertyName("drawerEdgeDragWidth"))
                        .orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.CALLBACK),
                scaffold.property(new PropertyName("onDrawerChanged"))
                        .orElseThrow().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.STRING),
                scaffold.property(new PropertyName("restorationId"))
                        .orElseThrow().acceptedKinds());
    }

    @Test
    void deliberatelyExcludesRuntimeGraphsAndNewStructuralSlots() {
        WidgetDefinition scaffold = BuiltInWidgetCatalog.getDefault()
                .find(ScaffoldWidgetPropertySchema.SCAFFOLD_TYPE).orElseThrow();
        Set<String> excludedConstructorGraphs = Set.of(
                "key",
                "persistentFooterButtons",
                "persistentFooterDecoration",
                "drawer",
                "endDrawer",
                "bottomNavigationBar",
                "bottomSheet",
                "bottomSheetScrimBuilder");

        assertTrue(scaffold.properties().stream()
                .map(property -> property.name().value())
                .noneMatch(excludedConstructorGraphs::contains));
        assertTrue(scaffold.slots().stream()
                .map(SlotDefinition::name)
                .map(SlotName::value)
                .noneMatch(Set.of(
                        "persistentFooterButtons", "drawer", "endDrawer",
                        "bottomNavigationBar", "bottomSheet")::contains));
    }

    private static ScaffoldWidgetPropertySchema.Definition definition(String name) {
        return ScaffoldWidgetPropertySchema.find(name).orElseThrow();
    }
}

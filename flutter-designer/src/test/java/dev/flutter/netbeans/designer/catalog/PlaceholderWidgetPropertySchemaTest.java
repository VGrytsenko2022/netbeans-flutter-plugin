package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DefaultConstructorSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE)
                .orElseThrow();

        assertEquals("Placeholder", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 50, "Placeholder"),
                definition.palette());
        assertEquals(List.of(
                        "color", "strokeWidth", "fallbackWidth", "fallbackHeight"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(PlaceholderWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3), definition.properties().stream()
                .map(property -> property.parameter().order()).toList());

        PropertyDefinition color = definition.property(new PropertyName("color"))
                .orElseThrow();
        assertFalse(color.parameter().required());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                color.acceptedKinds());
        assertTrue(color.creationDefault().isEmpty());

        for (String name : List.of(
                "strokeWidth", "fallbackWidth", "fallbackHeight")) {
            PropertyDefinition numeric = definition.property(new PropertyName(name))
                    .orElseThrow();
            assertFalse(numeric.parameter().required(), name);
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    numeric.acceptedKinds(), name);
            assertTrue(numeric.creationDefault().isEmpty(), name);
        }

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(4, child.parameter().order());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertTrue(child.acceptance() instanceof SlotAcceptance.AnyWidget);
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(PlaceholderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                PlaceholderWidgetPropertySchema.definitions().size());
        assertEquals(List.of(
                        "color", "strokeWidth", "fallbackWidth", "fallbackHeight"),
                PlaceholderWidgetPropertySchema.definitions().keySet().stream().toList());
        assertEquals(List.of(
                        PlaceholderWidgetPropertySchema.Group.APPEARANCE,
                        PlaceholderWidgetPropertySchema.Group.APPEARANCE,
                        PlaceholderWidgetPropertySchema.Group.FALLBACK_SIZE,
                        PlaceholderWidgetPropertySchema.Group.FALLBACK_SIZE),
                PlaceholderWidgetPropertySchema.definitions().values().stream()
                        .map(PlaceholderWidgetPropertySchema.Definition::group).toList());
        assertTrue(PlaceholderWidgetPropertySchema.find(new PropertyName("color"))
                .isPresent());
        assertTrue(PlaceholderWidgetPropertySchema.find(new PropertyName("unknown"))
                .isEmpty());
        PlaceholderWidgetPropertySchema.definitions().values().forEach(value -> {
            assertFalse(value.displayName().isBlank());
            assertFalse(value.description().isBlank());
        });
    }
}

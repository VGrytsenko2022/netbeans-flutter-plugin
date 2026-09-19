package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColoredBoxWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DefaultConstructorSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE)
                .orElseThrow();

        assertEquals("ColoredBox", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 40, "ColoredBox"),
                definition.palette());

        assertEquals(List.of("color", "isAntiAlias"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(ColoredBoxWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1), definition.properties().stream()
                .map(property -> property.parameter().order()).toList());

        PropertyDefinition color = definition.property(new PropertyName("color"))
                .orElseThrow();
        assertTrue(color.parameter().required());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                color.acceptedKinds());
        assertEquals(new PropertyValue.ColorValue(0xFF2196F3L),
                color.creationDefault().orElseThrow());

        PropertyDefinition antiAlias = definition
                .property(new PropertyName("isAntiAlias")).orElseThrow();
        assertFalse(antiAlias.parameter().required());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), antiAlias.acceptedKinds());
        assertTrue(antiAlias.creationDefault().isEmpty());

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(2, child.parameter().order());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertTrue(child.acceptance() instanceof SlotAcceptance.AnyWidget);
    }

    @Test
    void presentationSchemaIsCompleteAndLookupIsFailClosed() {
        assertEquals(ColoredBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ColoredBoxWidgetPropertySchema.definitions().size());
        assertEquals(List.of("color", "isAntiAlias"),
                ColoredBoxWidgetPropertySchema.definitions().keySet().stream().toList());
        assertTrue(ColoredBoxWidgetPropertySchema.find(new PropertyName("color"))
                .isPresent());
        assertTrue(ColoredBoxWidgetPropertySchema.find(new PropertyName("unknown"))
                .isEmpty());
        ColoredBoxWidgetPropertySchema.definitions().values().forEach(value -> {
            assertEquals(ColoredBoxWidgetPropertySchema.Group.APPEARANCE, value.group());
            assertFalse(value.displayName().isBlank());
            assertFalse(value.description().isBlank());
        });
    }
}

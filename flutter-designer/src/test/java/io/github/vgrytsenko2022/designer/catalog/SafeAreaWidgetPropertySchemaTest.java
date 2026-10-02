package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeAreaWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DefaultConstructorSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE)
                .orElseThrow();

        assertEquals("SafeArea", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 240, "SafeArea"),
                definition.palette());
        assertEquals(List.of(
                        "left", "top", "right", "bottom", "minimum",
                        "maintainBottomViewPadding"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(SafeAreaWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                definition.properties().stream()
                        .map(property -> property.parameter().order()).toList());
        assertTrue(definition.properties().stream()
                .noneMatch(property -> property.parameter().required()));
        assertTrue(definition.properties().stream()
                .allMatch(property -> property.creationDefault().isEmpty()));

        for (String booleanName : List.of(
                "left", "top", "right", "bottom",
                "maintainBottomViewPadding")) {
            assertEquals(Set.of(PropertyValueKind.BOOLEAN),
                    definition.property(new PropertyName(booleanName))
                            .orElseThrow().acceptedKinds(), booleanName);
        }
        PropertyDefinition minimum = definition.property(
                new PropertyName("minimum")).orElseThrow();
        assertEquals(Set.of(PropertyValueKind.EDGE_INSETS), minimum.acceptedKinds());
        PropertyValueConstraint.EdgeInsetsValues insets = assertInstanceOf(
                PropertyValueConstraint.EdgeInsetsValues.class,
                minimum.constraints().getFirst());
        assertFalse(insets.nonNegative());
        assertFalse(insets.directionalAllowed());
        assertTrue(insets.accepts(new PropertyValue.EdgeInsetsValue(
                BigDecimal.valueOf(-1), BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4))));
        assertFalse(insets.accepts(new PropertyValue.EdgeInsetsDirectionalValue(
                BigDecimal.ONE, BigDecimal.valueOf(2),
                BigDecimal.valueOf(3), BigDecimal.valueOf(4))));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(6, child.parameter().order());
        assertTrue(child.parameter().required());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
    }

    @Test
    void prototypeOmitsFrameworkDefaultsAndRemainsDetachedUntilAtomicWrap() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertTrue(prototype.properties().isEmpty());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child")));
        assertTrue(child.child().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void schemaMetadataIsExactAndStable() {
        assertEquals(6, SafeAreaWidgetPropertySchema.definitions().size());
        assertEquals(List.of(
                        SafeAreaWidgetPropertySchema.Group.SIDES,
                        SafeAreaWidgetPropertySchema.Group.SIDES,
                        SafeAreaWidgetPropertySchema.Group.SIDES,
                        SafeAreaWidgetPropertySchema.Group.SIDES,
                        SafeAreaWidgetPropertySchema.Group.PADDING,
                        SafeAreaWidgetPropertySchema.Group.VIEW_PADDING),
                SafeAreaWidgetPropertySchema.definitions().values().stream()
                        .map(SafeAreaWidgetPropertySchema.Definition::group).toList());
        assertTrue(SafeAreaWidgetPropertySchema.find(new PropertyName("minimum"))
                .isPresent());
        assertTrue(SafeAreaWidgetPropertySchema.find(new PropertyName("unknown"))
                .isEmpty());
    }
}

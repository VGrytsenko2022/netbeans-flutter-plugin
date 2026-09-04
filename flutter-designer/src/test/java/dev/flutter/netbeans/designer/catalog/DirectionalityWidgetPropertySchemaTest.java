package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionalityWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DefaultConstructorSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE)
                .orElseThrow();

        assertEquals("Directionality", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 60, "Directionality"),
                definition.palette());
        assertEquals(List.of("textDirection"), definition.properties().stream()
                .map(property -> property.name().value()).toList());
        assertEquals(DirectionalityWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition direction = definition.property(
                new PropertyName("textDirection")).orElseThrow();
        assertEquals(DartParameter.named(0, true), direction.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), direction.acceptedKinds());
        assertEquals(new PropertyValue.EnumValue("TextDirection", "ltr"),
                direction.creationDefault().orElseThrow());
        PropertyValueConstraint.EnumValues values = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                direction.constraints().getFirst());
        assertEquals(new DartSymbolReference(
                        "package:flutter/widgets.dart", "TextDirection"),
                values.dartType());
        assertEquals(List.of("rtl", "ltr"), values.values());
        assertTrue(values.accepts(new PropertyValue.EnumValue(
                "TextDirection", "rtl")));
        assertFalse(values.accepts(new PropertyValue.EnumValue(
                "TextDirection", "up")));
        assertFalse(values.accepts(new PropertyValue.EnumValue(
                "Axis", "horizontal")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(1, child.parameter().order());
        assertTrue(child.parameter().required());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
    }

    @Test
    void prototypePersistsReviewedLtrCreationValueAndWaitsForAtomicWrap() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertEquals(Map.of(
                        new PropertyName("textDirection"),
                        new PropertyValue.EnumValue("TextDirection", "ltr")),
                prototype.properties());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child")));
        assertTrue(child.child().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(DirectionalityWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                DirectionalityWidgetPropertySchema.definitions().size());
        assertEquals(List.of("textDirection"),
                DirectionalityWidgetPropertySchema.definitions().keySet()
                        .stream().toList());
        DirectionalityWidgetPropertySchema.Definition metadata =
                DirectionalityWidgetPropertySchema.definitions()
                        .get("textDirection");
        assertEquals(DirectionalityWidgetPropertySchema.Group.DIRECTION,
                metadata.group());
        assertEquals("Text direction", metadata.displayName());
        assertEquals("textDirection", metadata.dartName());
        assertEquals(0, metadata.dartOrder());
        assertFalse(metadata.description().isBlank());
        assertTrue(DirectionalityWidgetPropertySchema.find(
                new PropertyName("textDirection")).isPresent());
        assertTrue(DirectionalityWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }
}

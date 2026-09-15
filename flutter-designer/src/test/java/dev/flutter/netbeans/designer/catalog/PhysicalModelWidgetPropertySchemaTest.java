package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.model.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalModelTestSupport.*;

class PhysicalModelWidgetPropertySchemaTest {
    @Test
    void matchesEveryNonKeyConstructorArgumentInSdkOrderAndKeepsOnlyRequiredCreationColor() {
        var definition = BuiltInWidgetCatalog.getDefault()
                .find(PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE).orElseThrow();
        assertEquals("PhysicalModel", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 130, "PhysicalModel"), definition.palette());
        assertEquals(List.of("shape", "clipBehavior", "borderRadius", "elevation", "color", "shadowColor"),
                definition.properties().stream().map(p -> p.name().value()).toList());
        for (var entry : PhysicalModelWidgetPropertySchema.definitions().entrySet()) {
            var property = definition.property(name(entry.getKey())).orElseThrow();
            assertEquals(DartParameter.named(entry.getValue().dartOrder(), entry.getKey().equals("color")),
                    property.parameter());
            assertEquals(entry.getKey().equals("color"), property.creationDefault().isPresent());
            assertEquals(entry.getValue(), PhysicalModelWidgetPropertySchema.find(name(entry.getKey())).orElseThrow());
            assertFalse(entry.getValue().displayName().isBlank());
            assertFalse(entry.getValue().description().isBlank());
            assertFalse(entry.getValue().group().setName().isBlank());
            assertFalse(entry.getValue().group().displayName().isBlank());
            assertFalse(entry.getValue().group().description().isBlank());
        }
        assertEquals(6, PhysicalModelWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, PhysicalModelWidgetPropertySchema.SLOT_COUNT);
        assertTrue(PhysicalModelWidgetPropertySchema.find(name("key")).isEmpty());
        for (String color : List.of("color", "shadowColor")) {
            assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                    definition.property(name(color)).orElseThrow().acceptedKinds());
        }
        var child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(6, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(defaults(), prototype.properties());
        assertTrue(((WidgetSlot.SingleSlot) prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void concreteRadiusConstraintRejectsDirectionalWithoutChangingHistoricalGeometryContracts() {
        var broad = new PropertyValueConstraint.BorderRadiusValues();
        var physical = new PropertyValueConstraint.BorderRadiusValues(false);
        assertTrue(broad.directionalAllowed());
        assertFalse(physical.directionalAllowed());
        assertTrue(broad.accepts(radius(true)));
        assertTrue(physical.accepts(radius(false)));
        assertFalse(physical.accepts(radius(true)));
        assertFalse(physical.accepts(new PropertyValue.StringValue("BorderRadius.circular(8)")));
        assertNotEquals(broad, physical);
        assertEquals("physical or directional finite non-negative BorderRadiusGeometry", broad.description());
        assertTrue(physical.description().contains("directional geometry is not accepted"));
        var definition = BuiltInWidgetCatalog.getDefault()
                .find(PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE).orElseThrow();
        assertEquals(List.of(physical), definition.property(name("borderRadius")).orElseThrow().constraints());
    }

    @Test
    void exportedConstraintExpansionBumpsCatalogApiButReusesPersistedAndCanvasVersions() {
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }
}

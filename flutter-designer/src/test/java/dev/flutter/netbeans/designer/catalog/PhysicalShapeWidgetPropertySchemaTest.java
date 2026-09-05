package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.flutter.netbeans.designer.catalog.PhysicalShapeTestSupport.*;

class PhysicalShapeWidgetPropertySchemaTest {
    @Test
    void matchesEveryConstructorArgumentAndUsableNoCodePrototype() {
        var definition = BuiltInWidgetCatalog.getDefault().find(PhysicalShapeWidgetPropertySchema.PHYSICAL_SHAPE_TYPE).orElseThrow();
        assertEquals("PhysicalShape", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 140, "PhysicalShape"), definition.palette());
        assertEquals(List.of("clipper", "clipBehavior", "elevation", "color", "shadowColor"),
                definition.properties().stream().map(p -> p.name().value()).toList());
        for (var entry : PhysicalShapeWidgetPropertySchema.definitions().entrySet()) {
            var property = definition.property(name(entry.getKey())).orElseThrow();
            boolean required = Set.of("clipper", "color").contains(entry.getKey());
            assertEquals(DartParameter.named(entry.getValue().dartOrder(), required), property.parameter());
            assertEquals(required, property.creationDefault().isPresent());
            assertEquals(entry.getValue(), PhysicalShapeWidgetPropertySchema.find(name(entry.getKey())).orElseThrow());
            assertFalse(entry.getValue().displayName().isBlank());
            assertFalse(entry.getValue().description().isBlank());
        }
        assertEquals(5, PhysicalShapeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, PhysicalShapeWidgetPropertySchema.SLOT_COUNT);
        assertEquals(DartParameter.named(5, false), definition.slot(new SlotName("child")).orElseThrow().parameter());
        assertEquals(Set.of(PropertyValueKind.SHAPE_BORDER_CLIPPER, PropertyValueKind.DART_OBJECT_REFERENCE),
                definition.property(name("clipper")).orElseThrow().acceptedKinds());
        assertEquals(defaults(), WidgetNodePrototypeFactory.create(definition, StableId.random()).properties());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Optional.of("shapeBorderClipper:roundedRectangle:physicalZero:none"),
                projection.propertyContracts().get(name("clipper")).creationDefaultFingerprint());
    }

    @Test
    void shapeModelFailsClosedOnMissingDirectionAndHugeRadiiButRetainsIgnoredDirection() {
        var shape = PropertyValue.ShapeBorderClipperValue.Shape.ROUNDED_RECTANGLE;
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ShapeBorderClipperValue(
                shape, radius(true).geometry(), Optional.empty()));
        assertNotNull(new PropertyValue.ShapeBorderClipperValue(
                PropertyValue.ShapeBorderClipperValue.Shape.CIRCLE, radius(false).geometry(),
                Optional.of(PropertyValue.ShapeBorderClipperValue.TextDirection.LTR)));
        var huge = new PropertyValue.BoxDecorationValue.Radius(new BigDecimal("1e10000"), BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.ShapeBorderClipperValue(shape,
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(huge, huge, huge, huge), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.AnyValue(PropertyValueKind.SHAPE_BORDER_CLIPPER));
        assertFalse(new PropertyValueConstraint.ShapeBorderClipperValues().accepts(new PropertyValue.StringValue("ShapeBorderClipper()")));
        for (var preset : PropertyValue.ShapeBorderClipperValue.Shape.values()) {
            assertTrue(new PropertyValueConstraint.ShapeBorderClipperValues().accepts(clipper(preset, true)));
        }
    }

    @Test
    void closedValueAdditionBumpsAllAffectedContractsOnly() {
        assertEquals(14, WidgetCatalog.API_VERSION);
        assertEquals(13, DesignerDocument.SCHEMA_VERSION);
        assertEquals(18, CanvasModelPayloadCodec.VERSION);
        assertEquals(1, dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol.VERSION);
    }
}

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRSuperellipseWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448UnnamedConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE)
                .orElseThrow();

        assertEquals("ClipRSuperellipse", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 120, "ClipRSuperellipse"),
                definition.palette());
        assertEquals(List.of("borderRadius", "clipper", "clipBehavior"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(
                ClipRSuperellipseWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition borderRadius = definition.property(
                new PropertyName("borderRadius")).orElseThrow();
        assertEquals(DartParameter.named(0, false), borderRadius.parameter());
        assertEquals(Set.of(PropertyValueKind.BORDER_RADIUS),
                borderRadius.acceptedKinds());
        assertTrue(borderRadius.creationDefault().isEmpty(),
                "Omission must preserve Flutter's exact BorderRadius.zero default");
        PropertyValueConstraint.BorderRadiusValues radiusConstraint = assertInstanceOf(
                PropertyValueConstraint.BorderRadiusValues.class,
                borderRadius.constraints().getFirst());
        assertTrue(radiusConstraint.accepts(physicalRadius("1", "2")));
        assertTrue(radiusConstraint.accepts(directionalRadius("3", "4")));
        assertFalse(radiusConstraint.accepts(new PropertyValue.StringValue(
                "BorderRadius.circular(8)")));
        assertFalse(radiusConstraint.accepts(physicalRadius("1e10000", "2")),
                "Dart-unrepresentable radius components must fail closed");

        PropertyDefinition clipper = definition.property(
                new PropertyName("clipper")).orElseThrow();
        assertEquals(DartParameter.named(1, false), clipper.parameter());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefault().isEmpty());
        PropertyValueConstraint.DartObjectReferenceValues clipperConstraint =
                assertInstanceOf(
                        PropertyValueConstraint.DartObjectReferenceValues.class,
                        clipper.constraints().getFirst());
        assertEquals("CustomClipper<RSuperellipse>", clipperConstraint.expectedDartType());
        assertTrue(clipperConstraint.accepts(new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:sample/rsuperellipse_clipper.dart"),
                "SampleClipper", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty())));
        assertFalse(clipperConstraint.accepts(
                new PropertyValue.DartExpressionValue("SampleClipper()")));

        PropertyDefinition clipBehavior = definition.property(
                new PropertyName("clipBehavior")).orElseThrow();
        assertEquals(DartParameter.named(2, false), clipBehavior.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefault().isEmpty(),
                "Omission must preserve Flutter's exact Clip.antiAlias default");
        PropertyValueConstraint enumConstraint = clipBehavior.constraints().getFirst();
        for (String value : List.of(
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            assertTrue(enumConstraint.accepts(
                    new PropertyValue.EnumValue("Clip", value)), value);
        }
        assertFalse(enumConstraint.accepts(
                new PropertyValue.EnumValue("Clip", "custom")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(3, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());

        assertTrue(definition.property(new PropertyName("key")).isEmpty());
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(ClipRSuperellipseWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ClipRSuperellipseWidgetPropertySchema.definitions().size());
        assertEquals(1, ClipRSuperellipseWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of("borderRadius", "clipper", "clipBehavior"),
                ClipRSuperellipseWidgetPropertySchema.definitions().keySet().stream().toList());

        ClipRSuperellipseWidgetPropertySchema.Definition radius =
                ClipRSuperellipseWidgetPropertySchema.definitions().get("borderRadius");
        assertEquals(ClipRSuperellipseWidgetPropertySchema.Group.GEOMETRY, radius.group());
        assertEquals("clipRSuperellipseGeometry", radius.group().setName());
        assertEquals("Geometry", radius.group().displayName());
        assertEquals("Rounded-superellipse clipping geometry.",
                radius.group().description());
        assertEquals("Border radius", radius.displayName());
        assertEquals("borderRadius", radius.dartName());
        assertEquals(0, radius.dartOrder());
        assertFalse(radius.description().isBlank());

        ClipRSuperellipseWidgetPropertySchema.Definition clipper =
                ClipRSuperellipseWidgetPropertySchema.definitions().get("clipper");
        assertEquals(ClipRSuperellipseWidgetPropertySchema.Group.DELEGATE, clipper.group());
        assertEquals("clipRSuperellipseDelegate", clipper.group().setName());
        assertEquals("Delegate", clipper.group().displayName());
        assertEquals("Project-declared rounded-superellipse clip delegate.",
                clipper.group().description());
        assertEquals("Clipper", clipper.displayName());
        assertEquals("clipper", clipper.dartName());
        assertEquals(1, clipper.dartOrder());
        assertTrue(clipper.description().contains("CustomClipper<RSuperellipse>"));

        ClipRSuperellipseWidgetPropertySchema.Definition clip =
                ClipRSuperellipseWidgetPropertySchema.definitions().get("clipBehavior");
        assertEquals(ClipRSuperellipseWidgetPropertySchema.Group.CLIPPING, clip.group());
        assertEquals("clipRSuperellipseClipping", clip.group().setName());
        assertEquals("Clip behavior", clip.displayName());
        assertEquals("clipBehavior", clip.dartName());
        assertEquals(2, clip.dartOrder());
        assertFalse(clip.description().isBlank());

        assertTrue(ClipRSuperellipseWidgetPropertySchema.find(
                new PropertyName("borderRadius")).isPresent());
        assertTrue(ClipRSuperellipseWidgetPropertySchema.find(
                new PropertyName("clipBehavior")).isPresent());
        assertTrue(ClipRSuperellipseWidgetPropertySchema.find(
                new PropertyName("clipper")).isPresent());
        assertTrue(ClipRSuperellipseWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }

    @Test
    void prototypeOmitsFrameworkDefaultsAndCreatesAnEmptyChildSlot() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertEquals(Map.of(), prototype.properties(),
                "The prototype must preserve BorderRadius.zero and Clip.antiAlias by omission");
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void reusesTheExistingPersistedAndCanvasValueContracts() {
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static PropertyValue.BorderRadiusValue physicalRadius(
            String x, String y) {
        PropertyValue.BoxDecorationValue.Radius radius = radius(x, y);
        return new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius, radius, radius, radius));
    }

    private static PropertyValue.BorderRadiusValue directionalRadius(
            String x, String y) {
        PropertyValue.BoxDecorationValue.Radius radius = radius(x, y);
        return new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        radius, radius, radius, radius));
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(
            String x, String y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                new BigDecimal(x), new BigDecimal(y));
    }
}

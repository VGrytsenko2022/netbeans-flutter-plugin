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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipOvalWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448UnnamedConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE)
                .orElseThrow();

        assertEquals("ClipOval", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 90, "ClipOval"),
                definition.palette());
        assertEquals(List.of("clipper", "clipBehavior"), definition.properties().stream()
                .map(property -> property.name().value()).toList());
        assertEquals(ClipOvalWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition clipper = definition.property(
                new PropertyName("clipper")).orElseThrow();
        assertEquals(DartParameter.named(0, false), clipper.parameter());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefault().isEmpty(),
                "Omission must preserve the child-bounds oval");
        PropertyValueConstraint.DartObjectReferenceValues clipperConstraint =
                assertInstanceOf(
                        PropertyValueConstraint.DartObjectReferenceValues.class,
                        clipper.constraints().getFirst());
        assertEquals("CustomClipper<Rect>", clipperConstraint.expectedDartType());

        PropertyDefinition clipBehavior = definition.property(
                new PropertyName("clipBehavior")).orElseThrow();
        assertEquals(DartParameter.named(1, false), clipBehavior.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefault().isEmpty(),
                "Omission must preserve Flutter's exact Clip.antiAlias default");
        PropertyValueConstraint enumConstraint = clipBehavior.constraints().getFirst();
        for (String value : List.of(
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            assertTrue(enumConstraint.accepts(new PropertyValue.EnumValue("Clip", value)), value);
        }
        assertFalse(enumConstraint.accepts(
                new PropertyValue.EnumValue("Clip", "custom")));
        assertFalse(enumConstraint.accepts(
                new PropertyValue.StringValue("Clip.antiAlias")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());

        assertTrue(definition.property(new PropertyName("key")).isEmpty());
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(ClipOvalWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ClipOvalWidgetPropertySchema.definitions().size());
        assertEquals(1, ClipOvalWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of("clipper", "clipBehavior"),
                ClipOvalWidgetPropertySchema.definitions().keySet().stream().toList());
        ClipOvalWidgetPropertySchema.Definition metadata =
                ClipOvalWidgetPropertySchema.definitions().get("clipBehavior");
        assertEquals(ClipOvalWidgetPropertySchema.Group.CLIPPING, metadata.group());
        assertEquals("clipOvalClipping", metadata.group().setName());
        assertEquals("Clipping", metadata.group().displayName());
        assertEquals("Oval paint clipping behavior.", metadata.group().description());
        assertEquals("Clip behavior", metadata.displayName());
        assertEquals("clipBehavior", metadata.dartName());
        assertEquals(1, metadata.dartOrder());
        assertFalse(metadata.description().isBlank());
        assertTrue(ClipOvalWidgetPropertySchema.find(
                new PropertyName("clipBehavior")).isPresent());
        ClipOvalWidgetPropertySchema.Definition clipper =
                ClipOvalWidgetPropertySchema.find(
                        new PropertyName("clipper")).orElseThrow();
        assertEquals(ClipOvalWidgetPropertySchema.Group.DELEGATE, clipper.group());
        assertEquals("clipOvalDelegate", clipper.group().setName());
        assertEquals("Clipper", clipper.displayName());
        assertEquals(0, clipper.dartOrder());
        assertTrue(ClipOvalWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }

    @Test
    void prototypeKeepsOptionalArgumentsOmittedAndCreatesAnEmptyChildSlot() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE)
                .orElseThrow();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition, StableId.random());

        assertEquals(Map.of(), prototype.properties(),
                "The prototype must preserve Flutter's Clip.antiAlias default by omission");
        assertTrue(assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child"))).child().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void remainsCompatibleWithTheCurrentDesignerContracts() {
        assertEquals(12, DesignerDocument.SCHEMA_VERSION);
        assertEquals(12, WidgetCatalog.API_VERSION);
        assertEquals(17, CanvasModelPayloadCodec.VERSION);
    }
}

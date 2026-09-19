package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipPathWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448UnnamedConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipPathWidgetPropertySchema.CLIP_PATH_TYPE)
                .orElseThrow();

        assertEquals("ClipPath", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 110, "ClipPath"),
                definition.palette());
        assertEquals(List.of("clipper", "shape", "clipBehavior"), definition.properties().stream()
                .map(property -> property.name().value()).toList());
        assertEquals(ClipPathWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition clipper = definition.property(
                new PropertyName("clipper")).orElseThrow();
        assertEquals(DartParameter.named(0, false), clipper.parameter());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE),
                clipper.acceptedKinds());
        assertTrue(clipper.creationDefault().isEmpty(),
                "Omission must preserve the child-bounds rectangle");
        PropertyValueConstraint.DartObjectReferenceValues clipperConstraint =
                assertInstanceOf(
                        PropertyValueConstraint.DartObjectReferenceValues.class,
                        clipper.constraints().getFirst());
        assertEquals("CustomClipper<Path>", clipperConstraint.expectedDartType());

        PropertyDefinition shape = definition.property(new PropertyName("shape")).orElseThrow();
        assertEquals(DartParameter.named(1, false), shape.parameter());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE), shape.acceptedKinds());
        assertTrue(shape.creationDefault().isEmpty());
        assertEquals("ShapeBorder", assertInstanceOf(
                PropertyValueConstraint.DartObjectReferenceValues.class,
                shape.constraints().getFirst()).expectedDartType());

        PropertyDefinition clipBehavior = definition.property(
                new PropertyName("clipBehavior")).orElseThrow();
        assertEquals(DartParameter.named(2, false), clipBehavior.parameter());
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
        assertEquals(DartParameter.named(3, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());

        assertTrue(definition.property(new PropertyName("key")).isEmpty());
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(ClipPathWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ClipPathWidgetPropertySchema.definitions().size());
        assertEquals(1, ClipPathWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of("clipper", "shape", "clipBehavior"),
                ClipPathWidgetPropertySchema.definitions().keySet().stream().toList());
        ClipPathWidgetPropertySchema.Definition metadata =
                ClipPathWidgetPropertySchema.definitions().get("clipBehavior");
        assertEquals(ClipPathWidgetPropertySchema.Group.CLIPPING, metadata.group());
        assertEquals("clipPathClipping", metadata.group().setName());
        assertEquals("Clipping", metadata.group().displayName());
        assertEquals("Path paint clipping behavior.", metadata.group().description());
        assertEquals("Clip behavior", metadata.displayName());
        assertEquals("clipBehavior", metadata.dartName());
        assertEquals(2, metadata.dartOrder());
        assertFalse(metadata.description().isBlank());
        assertTrue(ClipPathWidgetPropertySchema.find(
                new PropertyName("clipBehavior")).isPresent());
        ClipPathWidgetPropertySchema.Definition clipper =
                ClipPathWidgetPropertySchema.find(
                        new PropertyName("clipper")).orElseThrow();
        assertEquals(ClipPathWidgetPropertySchema.Group.DELEGATE, clipper.group());
        assertEquals("clipPathDelegate", clipper.group().setName());
        assertEquals("Clipper", clipper.displayName());
        assertEquals(0, clipper.dartOrder());
        var shape = ClipPathWidgetPropertySchema.find(new PropertyName("shape")).orElseThrow();
        assertEquals(ClipPathWidgetPropertySchema.Group.SHAPE, shape.group());
        assertEquals("clipPathShape", shape.group().setName());
        assertEquals(1, shape.dartOrder());
        assertTrue(shape.description().contains("non-const ClipPath.shape"));
        assertTrue(ClipPathWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }

    @Test
    void prototypeKeepsOptionalArgumentsOmittedAndCreatesAnEmptyChildSlot() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipPathWidgetPropertySchema.CLIP_PATH_TYPE)
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
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }
}

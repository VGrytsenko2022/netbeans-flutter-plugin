package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRectWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448UnnamedConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(ClipRectWidgetPropertySchema.CLIP_RECT_TYPE)
                .orElseThrow();

        assertEquals("ClipRect", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 80, "ClipRect"),
                definition.palette());
        assertEquals(List.of("clipBehavior"), definition.properties().stream()
                .map(property -> property.name().value()).toList());
        assertEquals(ClipRectWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition clipBehavior = definition.property(
                new PropertyName("clipBehavior")).orElseThrow();
        assertEquals(DartParameter.named(0, false), clipBehavior.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), clipBehavior.acceptedKinds());
        assertTrue(clipBehavior.creationDefault().isEmpty(),
                "Omission must preserve Flutter's exact Clip.hardEdge default");
        PropertyValueConstraint enumConstraint = clipBehavior.constraints().getFirst();
        for (String value : List.of(
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            assertTrue(enumConstraint.accepts(new PropertyValue.EnumValue("Clip", value)), value);
        }
        assertFalse(enumConstraint.accepts(
                new PropertyValue.EnumValue("Clip", "custom")));
        assertFalse(enumConstraint.accepts(
                new PropertyValue.StringValue("Clip.hardEdge")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());

        assertTrue(definition.property(new PropertyName("clipper")).isEmpty(),
                "A non-null CustomClipper delegate is not a closed typed value");
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(ClipRectWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                ClipRectWidgetPropertySchema.definitions().size());
        assertEquals(1, ClipRectWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of("clipBehavior"),
                ClipRectWidgetPropertySchema.definitions().keySet().stream().toList());
        ClipRectWidgetPropertySchema.Definition metadata =
                ClipRectWidgetPropertySchema.definitions().get("clipBehavior");
        assertEquals(ClipRectWidgetPropertySchema.Group.CLIPPING, metadata.group());
        assertEquals("Clip behavior", metadata.displayName());
        assertEquals("clipBehavior", metadata.dartName());
        assertEquals(0, metadata.dartOrder());
        assertFalse(metadata.description().isBlank());
        assertTrue(ClipRectWidgetPropertySchema.find(
                new PropertyName("clipBehavior")).isPresent());
        assertTrue(ClipRectWidgetPropertySchema.find(
                new PropertyName("clipper")).isEmpty());
        assertTrue(ClipRectWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }

    @Test
    void reusesExistingTypedValuesWithoutAdvancingSchemaOrTransportVersions() {
        assertEquals(10, DesignerDocument.SCHEMA_VERSION);
        assertEquals(10, WidgetCatalog.API_VERSION);
        assertEquals(15, CanvasModelPayloadCodec.VERSION);
    }
}

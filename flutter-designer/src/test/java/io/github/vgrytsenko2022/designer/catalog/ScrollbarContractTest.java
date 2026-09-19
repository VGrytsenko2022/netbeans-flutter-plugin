package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollbarContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = ScrollbarWidgetPropertySchema.SCROLLBAR_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void completeConstructorHasEightPropertiesAndRequiredChild() {
        WidgetDefinition definition = definition();
        assertEquals("Scrollbar", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(8, definition.properties().size());
        assertEquals(java.util.List.of("controller", "thumbVisibility", "trackVisibility",
                "thickness", "radius", "notificationPredicate", "interactive",
                "scrollbarOrientation"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(SlotCardinality.SINGLE, definition.slot(CHILD).orElseThrow().cardinality());
        assertEquals(1, definition.slot(CHILD).orElseThrow().minChildren());
        assertEquals(0, definition.slot(CHILD).orElseThrow().parameter().order());
        assertEquals("package:flutter/material.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 440, "Scrollbar"),
                definition.palette());
    }

    @Test
    void canvasProjectionKeepsNullableBooleansNumbersEnumsAndReferences() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(8, projection.propertyContracts().size());
        assertEquals(java.util.Set.of(CHILD), projection.slots());
        assertEquals(java.util.Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("thumbVisibility")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.DOUBLE, PropertyValueKind.INTEGER,
                PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("thickness")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.ENUM, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("scrollbarOrientation")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("radius")).acceptedKinds());
    }

    @Test
    void generationMapsReviewedFieldsAndRequiredChildToSdkArguments() {
        var child = text("Scrollable child");
        var root = new WidgetNode(StableId.random(), TYPE, Map.of(
                new PropertyName("thumbVisibility"), new PropertyValue.BooleanValue(true),
                new PropertyName("trackVisibility"), new PropertyValue.BooleanValue(true),
                new PropertyName("thickness"), new PropertyValue.DoubleValue(BigDecimal.valueOf(8)),
                new PropertyName("interactive"), new PropertyValue.BooleanValue(false),
                new PropertyName("scrollbarOrientation"),
                new PropertyValue.EnumValue("ScrollbarOrientation", "left")),
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(child))));
        var document = document(root);
        assertTrue(new WidgetTreeValidator().validate(document, CATALOG).valid());
        var result = new DartRegionGenerator().generate(document, CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("Scrollbar("), source);
        assertTrue(source.contains("thumbVisibility: true"), source);
        assertTrue(source.contains("trackVisibility: true"), source);
        assertTrue(source.contains("thickness: 8.0"), source);
        assertTrue(source.contains("interactive: false"), source);
        assertTrue(source.contains("scrollbarOrientation: ScrollbarOrientation.left"), source);
        assertTrue(source.contains("child: const Text('Scrollable child')"), source);
    }

    private static WidgetDefinition definition() {
        return CATALOG.find(TYPE).orElseThrow();
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }

    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("e2c23b58-3448-4c87-a6d2-f35a6d3e0cb0"),
                new DartSourceDescriptor("scrollbar.dart", "ScrollbarScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

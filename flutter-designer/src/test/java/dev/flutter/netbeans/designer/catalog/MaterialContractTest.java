package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = MaterialWidgetPropertySchema.MATERIAL_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void completeConstructorHasTwelvePropertiesAndOptionalChild() {
        WidgetDefinition definition = definition();
        assertEquals("Material", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(12, definition.properties().size());
        assertEquals(java.util.List.of("materialType", "elevation", "color", "shadowColor",
                "surfaceTintColor", "textStyle", "borderRadius", "shape", "borderOnForeground",
                "clipBehavior", "animationDurationUs", "animateColor"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals("type", MaterialWidgetPropertySchema.find("materialType").orElseThrow().dartName());
        assertEquals(SlotCardinality.SINGLE, definition.slot(CHILD).orElseThrow().cardinality());
        assertEquals(11, definition.slot(CHILD).orElseThrow().parameter().order());
        assertEquals("package:flutter/material.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 430, "Material"), definition.palette());
    }

    @Test
    void canvasProjectionKeepsTypedReferencesGeometryAndDuration() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(12, projection.propertyContracts().size());
        assertEquals(java.util.Set.of(CHILD), projection.slots());
        assertEquals(java.util.Set.of(PropertyValueKind.ENUM),
                projection.propertyContracts().get(new PropertyName("materialType")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.BORDER_RADIUS,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("borderRadius")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.INTEGER,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("animationDurationUs")).acceptedKinds());
        assertEquals(java.util.Set.of(PropertyValueKind.DART_OBJECT_REFERENCE,
                PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("shape")).acceptedKinds());
    }

    @Test
    void generationMapsMaterialTypeAndExactDurationToSdkArguments() {
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(new PropertyName("materialType"),
                new PropertyValue.EnumValue("MaterialType", "card"));
        properties.put(new PropertyName("elevation"), new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(3.5)));
        properties.put(new PropertyName("animationDurationUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(1234)));
        properties.put(new PropertyName("animateColor"), new PropertyValue.BooleanValue(true));
        WidgetNode root = new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.of(text("Surface")))));
        DesignerDocument document = document(root);
        assertTrue(new WidgetTreeValidator().validate(document, CATALOG).valid());
        var result = new DartRegionGenerator().generate(document, CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("Material("), source);
        assertTrue(source.contains("type: MaterialType.card"), source);
        assertTrue(source.contains("elevation: 3.5"), source);
        assertTrue(source.contains("animationDuration: const Duration(microseconds: 1234)"), source);
        assertTrue(source.contains("animateColor: true"), source);
        assertTrue(source.contains("Text('Surface')"), source);
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
        return new DesignerDocument(StableId.parse("4f4c3570-e13c-4aef-9a42-4d27e9c4b5ef"),
                new DartSourceDescriptor("material.dart", "MaterialScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

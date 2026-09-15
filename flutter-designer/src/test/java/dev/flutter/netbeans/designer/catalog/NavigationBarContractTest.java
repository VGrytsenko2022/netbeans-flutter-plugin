package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationBarContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE;
    private static final SlotName DESTINATIONS = new SlotName("destinations");

    @Test
    void completeConstructorHasFifteenArgumentsAndRequiredDestinationList() {
        WidgetDefinition definition = definition();
        assertEquals(List.of(
                "animationDurationUs", "selectedIndex", "onDestinationSelected",
                "backgroundColor", "elevation", "shadowColor", "surfaceTintColor",
                "indicatorColor", "indicatorShape", "height", "labelBehavior",
                "overlayColor", "labelTextStyle", "labelPadding",
                "maintainBottomViewPadding"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
                definition.properties().stream().map(value -> value.parameter().order()).toList());
        assertEquals(15, NavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals("package:flutter/material.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 370, "NavigationBar"), definition.palette());
        assertFalse(definition.constConstructor());
        assertEquals(List.of("selectedIndex"), definition.properties().stream()
                .filter(value -> value.parameter().required()).map(value -> value.name().value()).toList());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                definition.property(p("selectedIndex")).orElseThrow().creationDefault().orElseThrow());
        SlotDefinition slot = definition.slot(DESTINATIONS).orElseThrow();
        assertEquals(DartParameter.named(15, true), slot.parameter());
        assertEquals(SlotCardinality.LIST, slot.cardinality());
        assertEquals(0, slot.minChildren());
        assertEquals(10_000, slot.maxChildren());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void canvasProjectionKeepsTypedDefaultsAndAllFifteenProperties() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(15, projection.propertyContracts().size());
        assertEquals(Set.of(DESTINATIONS), projection.slots());
        var selected = projection.propertyContracts().get(p("selectedIndex"));
        assertEquals(Set.of(PropertyValueKind.INTEGER), selected.acceptedKinds());
        assertTrue(selected.required());
        assertEquals(Optional.of("integer:0"), selected.creationDefaultFingerprint());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(p("onDestinationSelected")).acceptedKinds());
        var destinations = projection.slotContracts().get(DESTINATIONS);
        assertTrue(destinations.required());
        assertEquals(0, destinations.minimumChildren());
        assertEquals(10_000, destinations.maximumChildren());
    }

    @Test
    void generationRetainsCallbackDurationAndDestinationOrder() {
        WidgetNode root = node(Map.of(
                p("selectedIndex"), new PropertyValue.IntegerValue(BigInteger.ONE),
                p("onDestinationSelected"), new PropertyValue.StringValue("noop"),
                p("animationDurationUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(-125))));
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("NavigationBar("), source);
        assertTrue(source.contains("selectedIndex: 1"), source);
        assertTrue(source.contains("onDestinationSelected: (_) {}"), source);
        assertTrue(source.contains("animationDuration: const Duration(microseconds: -125)"), source);
        assertTrue(source.contains("NavigationDestination("), source);
        assertTrue(source.contains("label: const Text('Destination 1')"), source);
        assertTrue(source.contains("label: const Text('Destination 2')"), source);
        assertTrue(source.indexOf("Home") < source.indexOf("Settings"), source);
        assertTrue(new WidgetTreeValidator().validate(document(root), CATALOG).valid());
    }

    private static WidgetDefinition definition() {
        return CATALOG.find(TYPE).orElseThrow();
    }

    private static PropertyName p(String name) {
        return new PropertyName(name);
    }

    private static WidgetNode node(Map<PropertyName, PropertyValue> overrides) {
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var properties = new LinkedHashMap<>(prototype.properties());
        properties.putAll(overrides);
        return new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(DESTINATIONS, new WidgetSlot.ListSlot(List.of(
                        text("Home"), text("Settings")))));
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }

    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("7b894c2b-ef40-4350-a518-5e6b7d0a23ad"),
                new DartSourceDescriptor("navigation.dart", "NavigationScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

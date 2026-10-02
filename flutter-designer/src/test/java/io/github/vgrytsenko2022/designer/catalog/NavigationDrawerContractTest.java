package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
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

class NavigationDrawerContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE =
            NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE;
    private static final SlotName HEADER = new SlotName("header");
    private static final SlotName FOOTER = new SlotName("footer");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void completeConstructorHasNineArgumentsAndThreeSlots() {
        WidgetDefinition definition = definition();
        assertEquals(List.of(
                "backgroundColor", "shadowColor", "surfaceTintColor", "elevation",
                "indicatorColor", "indicatorShape", "onDestinationSelected",
                "selectedIndex", "tilePadding"), definition.properties().stream()
                .map(value -> value.name().value()).toList());
        assertEquals(List.of(3, 4, 5, 6, 7, 8, 9, 10, 11), definition.properties().stream()
                .map(value -> value.parameter().order()).toList());
        assertEquals(9, NavigationDrawerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(9, definition.properties().size());
        assertFalse(definition.constConstructor());
        PropertyDefinition selectedIndex = definition.property(p("selectedIndex")).orElseThrow();
        assertTrue(selectedIndex.parameter().required());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                selectedIndex.creationDefault().orElseThrow());
        assertEquals(List.of(CHILDREN, HEADER, FOOTER), definition.slots().stream()
                .map(SlotDefinition::name).toList());
        assertEquals(DartParameter.named(1, false), definition.slot(HEADER).orElseThrow().parameter());
        assertEquals(DartParameter.named(2, false), definition.slot(FOOTER).orElseThrow().parameter());
        SlotDefinition children = definition.slot(CHILDREN).orElseThrow();
        assertEquals(DartParameter.named(0, true), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void canvasProjectionKeepsRequiredNullableSelectionAndDrawerSlots() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(9, projection.propertyContracts().size());
        assertEquals(Set.of(HEADER, FOOTER, CHILDREN), projection.slots());
        var selected = projection.propertyContracts().get(p("selectedIndex"));
        assertTrue(selected.required());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL), selected.acceptedKinds());
        assertEquals(Optional.of("integer:0"), selected.creationDefaultFingerprint());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(p("onDestinationSelected")).acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(p("indicatorShape")).acceptedKinds());
        var children = projection.slotContracts().get(CHILDREN);
        assertTrue(children.required());
        assertEquals(0, children.minimumChildren());
        assertEquals(10_000, children.maximumChildren());
    }

    @Test
    void generationRetainsSelectionCallbackAndDestinationOrder() {
        WidgetNode root = node(Map.of(
                p("selectedIndex"), new PropertyValue.IntegerValue(BigInteger.ONE),
                p("onDestinationSelected"), new PropertyValue.StringValue("noop")));
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("NavigationDrawer("), source);
        assertTrue(source.contains("selectedIndex: 1"), source);
        assertTrue(source.contains("onDestinationSelected: (_) {}"), source);
        assertTrue(source.contains("NavigationDrawerDestination("), source);
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
                Map.of(
                        HEADER, WidgetSlot.SingleSlot.empty(),
                        FOOTER, WidgetSlot.SingleSlot.empty(),
                        CHILDREN, new WidgetSlot.ListSlot(List.of(
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

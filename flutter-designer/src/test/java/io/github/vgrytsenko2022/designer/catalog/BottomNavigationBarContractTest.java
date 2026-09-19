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

class BottomNavigationBarContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE;
    private static final SlotName ITEMS = new SlotName("items");

    @Test
    void completeConstructorHasTwentyPropertiesAndRequiredItemsList() {
        WidgetDefinition definition = definition();
        assertEquals("BottomNavigationBar", definition.dartClassName());
        assertFalse(definition.constConstructor());
        assertEquals(20, definition.properties().size());
        assertEquals(List.of("onTap", "currentIndex", "elevation", "barType", "backgroundColor",
                "iconSize", "selectedItemColor", "unselectedItemColor", "selectedIconTheme",
                "unselectedIconTheme", "selectedFontSize", "unselectedFontSize", "selectedLabelStyle",
                "unselectedLabelStyle", "showSelectedLabels", "showUnselectedLabels", "mouseCursor",
                "enableFeedback", "landscapeLayout", "useLegacyColorScheme"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19),
                definition.properties().stream().map(value -> value.parameter().order()).toList());
        assertEquals("package:flutter/material.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 420, "BottomNavigationBar"), definition.palette());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                definition.property(new PropertyName("currentIndex")).orElseThrow().creationDefault().orElseThrow());
        SlotDefinition items = definition.slot(ITEMS).orElseThrow();
        assertEquals(DartParameter.named(20, true), items.parameter());
        assertEquals(SlotCardinality.LIST, items.cardinality());
        assertEquals(0, items.minChildren());
        assertEquals(10_000, items.maxChildren());
    }

    @Test
    void canvasProjectionKeepsTypedReferencesEnumsAndRequiredItems() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(20, projection.propertyContracts().size());
        assertEquals(Set.of(ITEMS), projection.slots());
        assertEquals(Set.of(PropertyValueKind.STRING, PropertyValueKind.CALLBACK,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("onTap")).acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.ENUM, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("barType")).acceptedKinds());
        assertTrue(projection.propertyContracts().get(new PropertyName("currentIndex")).required());
        assertEquals(Optional.of("integer:0"), projection.propertyContracts()
                .get(new PropertyName("currentIndex")).creationDefaultFingerprint());
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN,
                PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(new PropertyName("backgroundColor")).acceptedKinds());
        assertTrue(projection.slotContracts().get(ITEMS).required());
        assertEquals(0, projection.slotContracts().get(ITEMS).minimumChildren());
        assertEquals(10_000, projection.slotContracts().get(ITEMS).maximumChildren());
    }

    @Test
    void generationMapsInternalBarTypeToSdkTypeAndPreservesItemOrder() {
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(new PropertyName("barType"), new PropertyValue.EnumValue(
                "BottomNavigationBarType", "shifting"));
        properties.put(new PropertyName("currentIndex"), new PropertyValue.IntegerValue(BigInteger.ONE));
        properties.put(new PropertyName("onTap"), new PropertyValue.StringValue("noop"));
        WidgetNode root = new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(ITEMS, new WidgetSlot.ListSlot(List.of(text("Home"), text("Settings")))));
        DesignerDocument document = document(root);
        assertTrue(new WidgetTreeValidator().validate(document, CATALOG).valid());
        var result = new DartRegionGenerator().generate(document, CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("BottomNavigationBar("), source);
        assertTrue(source.contains("type: BottomNavigationBarType.shifting"), source);
        assertTrue(source.contains("currentIndex: 1"), source);
        assertTrue(source.contains("onTap: (_) {}"), source);
        assertTrue(source.contains("label: 'Item 1'"), source);
        assertTrue(source.contains("label: 'Item 2'"), source);
        assertTrue(source.indexOf("Item 1") < source.indexOf("Item 2"), source);
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
                new DartSourceDescriptor("bottom_navigation.dart", "BottomNavigationScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BottomAppBarContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = BottomAppBarWidgetPropertySchema.BOTTOM_APP_BAR_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void completeConstructorHasNinePropertiesAndOneOptionalChildSlot() {
        WidgetDefinition definition = definition();
        assertEquals("BottomAppBar", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(BottomAppBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                definition.properties().size());
        assertEquals(List.of("color", "elevation", "shape", "clipBehavior", "notchMargin",
                "padding", "surfaceTintColor", "shadowColor", "height"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 7, 8, 9, 10),
                definition.properties().stream().map(value -> value.parameter().order()).toList());
        assertEquals(BottomAppBarWidgetPropertySchema.SLOT_COUNT, definition.slots().size());
        var child = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(6, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
    }

    @Test
    void capabilityProjectionKeepsColorsNumbersPaddingShapeAndClip() {
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).orElseThrow();
        assertEquals(9, projection.propertyContracts().size());
        assertEquals(Map.of(CHILD, projection.slotContracts().get(CHILD)), projection.slotContracts());
        for (String name : List.of("color", "shadowColor", "surfaceTintColor")) {
            assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN,
                    PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                    projection.propertyContracts().get(new PropertyName(name)).acceptedKinds());
        }
        assertEquals(Set.of(PropertyValueKind.ENUM), projection.propertyContracts()
                .get(new PropertyName("clipBehavior")).acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE), projection.propertyContracts()
                .get(new PropertyName("notchMargin")).acceptedKinds());
    }

    @Test
    void generationRetainsTypedValuesChildAndNotchedShapeReference() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(new PropertyName("elevation"), new PropertyValue.DoubleValue(new BigDecimal("3.5")));
        properties.put(new PropertyName("notchMargin"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(8)));
        properties.put(new PropertyName("shape"), new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:app/shapes.dart"), "makeNotch", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                Optional.of(false)));
        properties.put(new PropertyName("clipBehavior"), new PropertyValue.EnumValue("Clip", "antiAlias"));
        WidgetNode root = new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Bar content"))));
        assertTrue(new WidgetTreeValidator().validate(document(root), CATALOG).valid());
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("BottomAppBar("), source);
        assertTrue(source.contains("elevation: 3.5"), source);
        assertTrue(source.contains("notchMargin: 8"), source);
        assertTrue(source.contains("shape: ") && source.contains("makeNotch()"), source);
        assertTrue(source.contains("clipBehavior: Clip.antiAlias"), source);
        assertTrue(source.contains("child: const Text('Bar content')"), source);
    }

    private static WidgetDefinition definition() { return CATALOG.find(TYPE).orElseThrow(); }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("a3f8c10d-3cf3-4f3a-bb2a-6fd7c46b4e89"),
                new DartSourceDescriptor("bottom_app_bar.dart", "BottomAppBarScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

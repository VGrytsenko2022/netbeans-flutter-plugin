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
import io.github.vgrytsenko2022.designer.model.ThemeToken;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
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

class DrawerContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = DrawerWidgetPropertySchema.DRAWER_TYPE;
    private static final SlotName CHILD = new SlotName("child");

    @Test
    void completeConstructorHasEightArgumentsAndOneOptionalChildSlot() {
        WidgetDefinition definition = definition();
        assertEquals("Drawer", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(List.of("backgroundColor", "elevation", "shadowColor",
                "surfaceTintColor", "shape", "width", "semanticLabel", "clipBehavior"),
                definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 7, 8), definition.properties().stream()
                .map(value -> value.parameter().order()).toList());
        assertEquals(DrawerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                definition.properties().size());
        assertEquals(DrawerWidgetPropertySchema.SLOT_COUNT, definition.slots().size());
        var child = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(6, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertEquals(new SlotAcceptance.AnyWidget(), child.acceptance());
    }

    @Test
    void capabilityProjectionKeepsNullableColorsReferencesNumbersAndClipEnum() {
        WidgetDefinition definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE,
                WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(8, projection.propertyContracts().size());
        assertEquals(Set.of(CHILD), projection.slots());
        for (String name : List.of("backgroundColor", "shadowColor", "surfaceTintColor")) {
            assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN,
                    PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                    projection.propertyContracts().get(p(name)).acceptedKinds());
        }
        for (String name : List.of("elevation", "width")) {
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE,
                    PropertyValueKind.NULL), projection.propertyContracts().get(p(name)).acceptedKinds());
        }
        assertEquals(Set.of(PropertyValueKind.DART_OBJECT_REFERENCE, PropertyValueKind.NULL),
                projection.propertyContracts().get(p("shape")).acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.ENUM, PropertyValueKind.NULL),
                projection.propertyContracts().get(p("clipBehavior")).acceptedKinds());
        assertEquals(0, projection.slotContracts().get(CHILD).minimumChildren());
    }

    @Test
    void generationRetainsTypedValuesChildIdentityAndShapeReference() {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(p("backgroundColor"), new PropertyValue.ThemeTokenValue(
                new ThemeToken("material.colorScheme.surface")));
        properties.put(p("elevation"), new PropertyValue.DoubleValue(new BigDecimal("4.5")));
        properties.put(p("shape"), new PropertyValue.DartObjectReferenceValue(
                Optional.of("package:app/shapes.dart"), "makeShape", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                Optional.of(false)));
        properties.put(p("width"), new PropertyValue.IntegerValue(BigInteger.valueOf(320)));
        properties.put(p("semanticLabel"), new PropertyValue.StringValue("Navigation drawer"));
        properties.put(p("clipBehavior"), new PropertyValue.EnumValue("Clip", "antiAlias"));
        WidgetNode root = new WidgetNode(StableId.random(), TYPE, properties,
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Content"))));
        assertTrue(new WidgetTreeValidator().validate(document(root), CATALOG).valid());
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        String source = result.generated().orElseThrow().build().payload();
        assertTrue(source.contains("Drawer("), source);
        assertTrue(source.contains("backgroundColor: Theme.of(context).colorScheme.surface"), source);
        assertTrue(source.contains("elevation: 4.5"), source);
        assertTrue(source.contains("shape: ") && source.contains("makeShape()"), source);
        assertTrue(source.contains("width: 320"), source);
        assertTrue(source.contains("semanticLabel: 'Navigation drawer'"), source);
        assertTrue(source.contains("clipBehavior: Clip.antiAlias"), source);
        assertTrue(source.contains("child: const Text('Content')"), source);
        assertFalse(source.contains("return const Drawer("), source);
    }

    private static WidgetDefinition definition() {
        return CATALOG.find(TYPE).orElseThrow();
    }

    private static PropertyName p(String name) {
        return new PropertyName(name);
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue(value)), Map.of());
    }

    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("a3f8c10d-3cf3-4f3a-bb2a-6fd7c46b4e88"),
                new DartSourceDescriptor("drawer.dart", "DrawerScreen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

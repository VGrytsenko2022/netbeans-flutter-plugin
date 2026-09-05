package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerticalDividerContractTest {
    private static final WidgetTypeId TYPE = VerticalDividerWidgetPropertySchema.VERTICAL_DIVIDER_TYPE;
    private static final List<String> NUMBERS = List.of("width", "thickness", "indent", "endIndent");
    private static final List<String> FIELDS = List.of("width", "thickness", "indent", "endIndent", "color", "radius");

    @Test
    void exactPinnedSixOptionalFieldsUseExistingValuesAndAnEmptyLeafPrototype() {
        var definition = definition();
        assertEquals("VerticalDivider", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/material.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.material", 100, 60, "VerticalDivider"), definition.palette());
        assertEquals(6, VerticalDividerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(0, VerticalDividerWidgetPropertySchema.SLOT_COUNT);
        assertEquals(FIELDS, definition.properties().stream().map(v -> v.name().value()).toList());
        assertTrue(definition.slots().isEmpty());
        assertTrue(definition.traits().isEmpty());
        for (int index = 0; index < FIELDS.size(); index++) {
            var property = definition.property(p(FIELDS.get(index))).orElseThrow();
            assertEquals(DartParameter.named(index, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            var hint = VerticalDividerWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(FIELDS.get(index), hint.dartName());
            assertEquals(index, hint.dartOrder());
            assertFalse(hint.description().isBlank());
        }
        for (String name : List.of("width", "indent", "endIndent")) assertEquals("verticalDividerLayout", VerticalDividerWidgetPropertySchema.find(p(name)).orElseThrow().group().setName());
        for (String name : List.of("thickness", "color", "radius")) assertEquals("verticalDividerAppearance", VerticalDividerWidgetPropertySchema.find(p(name)).orElseThrow().group().setName());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(), prototype.properties());
        assertTrue(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(13, DesignerDocument.SCHEMA_VERSION);
        assertEquals(14, WidgetCatalog.API_VERSION);
        assertEquals(18, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void verticalAxesRemainDistinctFromHorizontalDividerWhileSharedDomainsMatch() {
        var horizontal = BuiltInWidgetCatalog.getDefault().find(DividerWidgetPropertySchema.DIVIDER_TYPE).orElseThrow();
        assertTrue(horizontal.property(p("height")).isPresent());
        assertTrue(horizontal.property(p("width")).isEmpty());
        assertTrue(definition().property(p("height")).isEmpty());
        assertEquals(horizontal.property(p("height")).orElseThrow().constraints(), definition().property(p("width")).orElseThrow().constraints());
        for (String name : List.of("thickness", "indent", "endIndent", "color", "radius")) {
            assertEquals(horizontal.property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints());
        }
        var top = VerticalDividerWidgetPropertySchema.find(p("indent")).orElseThrow().description();
        var bottom = VerticalDividerWidgetPropertySchema.find(p("endIndent")).orElseThrow().description();
        assertTrue(top.contains("top inset"));
        assertTrue(bottom.contains("bottom inset"));
        assertTrue(top.contains("does not change in RTL"));
        assertTrue(bottom.contains("does not change in RTL"));
        assertFalse(top.contains("leading"));
        assertFalse(bottom.contains("trailing"));
        assertTrue(VerticalDividerWidgetPropertySchema.find(p("radius")).orElseThrow().description().contains("Directional corners follow ambient text direction"));
        assertEquals(50, horizontal.palette().itemOrder());
        assertEquals(60, definition().palette().itemOrder());
    }

    @Test
    void numericAndRadiusDomainsReusePortableFiniteBoundsWithoutInventedLayoutRelations() {
        var icon = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow();
        for (String name : NUMBERS) {
            assertEquals(icon.property(p("size")).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints());
            for (PropertyValue value : List.of(new PropertyValue.IntegerValue(BigInteger.ZERO), d("0.001"), d("1e100"))) assertTrue(accepts(name, value));
            for (PropertyValue value : List.of(d("-0.001"), d("1e999"), new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)), new PropertyValue.NullValue())) assertFalse(accepts(name, value));
        }
        assertEquals(icon.property(p("color")).orElseThrow().constraints(), definition().property(p("color")).orElseThrow().constraints());
        assertEquals(List.of(new PropertyValueConstraint.BorderRadiusValues()), definition().property(p("radius")).orElseThrow().constraints());
        // Flutter owns effective layout and paint behavior; constructor validation has no such relations.
        assertTrue(valid(node(Map.of(p("width"), d("1"), p("thickness"), d("20"), p("indent"), d("10000"), p("endIndent"), d("10000")))));
        assertTrue(valid(node(Map.of(p("thickness"), d("0"), p("radius"), radius(true, false)))));
        assertTrue(valid(node(Map.of(p("radius"), radius(false, false)))));
        assertFalse(accepts("radius", new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(r("1e999", "0"), r("0", "0"), r("0", "0"), r("0", "0")))));
    }

    @Test
    void exactCapabilityRejectsDefaultRequiredKindRadiusAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES), BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(6, projection.propertyContracts().size());
        assertTrue(projection.slotContracts().isEmpty());
        for (var property : definition.properties()) {
            var contract = projection.propertyContracts().get(property.name());
            assertFalse(contract.required());
            assertTrue(contract.creationDefaultFingerprint().isEmpty());
            assertEquals(property.acceptedKinds(), contract.acceptedKinds());
            var constraints = new ArrayList<>(property.constraints());
            constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), true), property.constraints(), Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), constraints, Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties()); properties.set(properties.indexOf(property), changed); assertNoCapability(properties, List.of());
            }
        }
        var properties = new ArrayList<>(definition.properties());
        properties.set(0, new PropertyDefinition(p("width"), DartParameter.named(0, false), properties.getFirst().constraints(), Optional.of(d("16"))));
        assertNoCapability(properties, List.of());
        properties = new ArrayList<>(definition.properties());
        properties.set(5, new PropertyDefinition(p("radius"), DartParameter.named(5, false), List.of(new PropertyValueConstraint.BorderRadiusValues(false)), Optional.empty()));
        assertNoCapability(properties, List.of());
        assertNoCapability(definition.properties().subList(0, 5), List.of());
        assertNoCapability(definition.properties(), List.of(new SlotDefinition(new SlotName("child"), DartParameter.named(6, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget())));
    }

    @Test
    void all1215PresenceZeroPositiveColorAndRadiusCombinationsPreserveExactSourceProvenanceAndCodec() throws Exception {
        var codec = new FdDocumentCodec();
        int count = 0;
        for (int numericStates = 0; numericStates < 81; numericStates++) for (int colorState = 0; colorState < 3; colorState++) for (int radiusState = 0; radiusState < 5; radiusState++) {
            count++;
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            int state = numericStates;
            for (String name : NUMBERS) { int choice = state % 3; state /= 3; if (choice > 0) values.put(p(name), choice == 1 ? new PropertyValue.IntegerValue(BigInteger.ZERO) : d("24.5")); }
            if (colorState > 0) values.put(p("color"), colorState == 1 ? new PropertyValue.ColorValue(0x80123456L) : new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outlineVariant")));
            if (radiusState > 0) values.put(p("radius"), radius(radiusState >= 3, radiusState % 2 == 1));
            var root = node(values);
            assertTrue(valid(root));
            var generated = generated(root);
            String source = generated.build().payload();
            assertEquals(colorState != 2, source.contains("return const VerticalDivider("), source);
            assertEquals(colorState == 2, source.contains("Theme.of(context).colorScheme.outlineVariant"), source);
            for (String name : FIELDS) assertEquals(values.containsKey(p(name)), source.contains(name + ":"), name + source);
            if (radiusState > 0) {
                String owner = radiusState >= 3 ? "BorderRadiusDirectional" : "BorderRadius";
                var occurrence = generated.symbolOccurrences().stream().filter(v -> v.symbolName().equals(owner)).findFirst().orElseThrow();
                assertEquals("/root/properties/radius/geometry", occurrence.modelPath());
                assertEquals(Optional.of(root.id()), occurrence.widgetId());
                assertEquals(4, generated.symbolOccurrences().stream().filter(v -> v.symbolName().equals("Radius")).count());
                for (String corner : radiusState >= 3 ? List.of("topStart", "topEnd", "bottomEnd", "bottomStart") : List.of("topLeft", "topRight", "bottomRight", "bottomLeft")) {
                    assertTrue(generated.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("Radius") && v.modelPath().equals("/root/properties/radius/geometry/" + corner)));
                }
            }
            for (var symbol : generated.symbolOccurrences()) { assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset())); assertEquals("package:flutter/material.dart", symbol.libraryUri()); }
            assertEquals(generated, generated(root));
            var document = document(root);
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated()); assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
        assertEquals(1215, count);
    }

    @Test
    void omittedDefaultsAndExplicitSquareTransparentHairlineValuesRemainDistinct() {
        assertTrue(generated(node(Map.of())).build().payload().contains("return const VerticalDivider()"));
        var explicit = node(Map.of(p("width"), d("0"), p("thickness"), d("0"), p("color"), new PropertyValue.ColorValue(0), p("radius"), radius(false, true)));
        String source = generated(explicit).build().payload();
        assertTrue(source.contains("width: 0.0"), source);
        assertTrue(source.contains("thickness: 0.0"), source);
        assertTrue(source.contains("Color(0x00000000)"), source);
        assertTrue(source.contains("BorderRadius.only("), source);
        assertEquals(4, source.split("Radius.elliptical\\(0.0, 0.0\\)", -1).length - 1);
        for (String internal : List.of("space:", "height:", "createBorderSide(", "DividerThemeData(", "outlineVariant", "dividerColor")) assertFalse(generated(node(Map.of())).build().payload().contains(internal));
    }

    @Test
    void malformedKindsUnknownSdkHelpersAndSlotsFailBeforeSourceGeneration() {
        for (String name : FIELDS) for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.BooleanValue(false), new PropertyValue.StringValue("invalid"), new PropertyValue.DartExpressionValue("Divider.createBorderSide(context)"))) {
            var root = node(Map.of(p(name), value)); assertFalse(valid(root)); assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
        }
        assertFalse(valid(node(Map.of(p("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))))));
        for (String name : List.of("height", "space", "key", "textDirection", "semanticLabel", "child", "createBorderSide", "borderRadius")) { assertFalse(valid(node(Map.of(p(name), d("1"))))); assertTrue(VerticalDividerWidgetPropertySchema.find(p(name)).isEmpty()); }
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))));
        assertThrows(IllegalArgumentException.class, () -> r("-1", "0"));
    }

    private static PropertyValue.BorderRadiusValue radius(boolean directional, boolean zero) { var a = r(zero ? "0" : "1", zero ? "0" : "2"); var b = r(zero ? "0" : "3", zero ? "0" : "4"); var c = r(zero ? "0" : "5", zero ? "0" : "6"); var d = r(zero ? "0" : "7", zero ? "0" : "8"); return new PropertyValue.BorderRadiusValue(directional ? new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,b,c,d) : new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,b,c,d)); }
    private static PropertyValue.BoxDecorationValue.Radius r(String x, String y) { return new PropertyValue.BoxDecorationValue.Radius(new BigDecimal(x), new BigDecimal(y)); }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) { return new WidgetNode(StableId.random(), TYPE, values, Map.of()); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) { var original = definition(); var changed = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(), original.importUris(), original.traits(), original.palette(), properties, slots); assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty()); assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty()); }
}

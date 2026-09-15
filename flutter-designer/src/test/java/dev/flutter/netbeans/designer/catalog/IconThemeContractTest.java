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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IconThemeContractTest {
    private static final WidgetTypeId TYPE = IconThemeWidgetPropertySchema.ICON_THEME_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final PropertyName MERGE = p("merge");
    private static final List<String> FIELDS = List.of("size", "fill", "weight", "grade", "opticalSize",
            "color", "opacity", "shadows", "applyTextScaling");
    private static final PropertyValue.ThemeTokenValue PRIMARY = new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"));

    @Test
    void exactNineOptionalDataLeavesAndRequiredModeFormRealChildWrapperWithoutFormatBump() {
        var definition = definition();
        assertEquals("IconTheme", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 220, "IconTheme"), definition.palette());
        assertEquals(10, IconThemeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, IconThemeWidgetPropertySchema.SLOT_COUNT);
        assertEquals(10, definition.properties().size());
        for (int index = 0; index < definition.properties().size(); index++) {
            var property = definition.properties().get(index);
            int order = index == 9 ? 10 : index;
            assertEquals(index == 9 ? MERGE : p(FIELDS.get(index)), property.name());
            assertEquals(DartParameter.named(order, index == 9), property.parameter());
            assertEquals(index == 9 ? Optional.of(bool(false)) : Optional.empty(), property.creationDefault());
            var hint = IconThemeWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), hint.dartName());
            assertEquals(order, hint.dartOrder());
            assertFalse(hint.description().isBlank());
        }
        assertEquals("iconThemeAppearance", IconThemeWidgetPropertySchema.find(p("opacity")).orElseThrow().group().setName());
        assertEquals("iconThemeVariableFont", IconThemeWidgetPropertySchema.find(p("weight")).orElseThrow().group().setName());
        assertEquals("iconThemeBehavior", IconThemeWidgetPropertySchema.find(MERGE).orElseThrow().group().setName());
        assertTrue(IconThemeWidgetPropertySchema.find(MERGE).orElseThrow().description().contains("cannot be reset"));
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(9, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(MERGE, bool(false)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.widgets.IconTheme|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void numericDomainsReuseRenderSafeIconBoundsButOpacityPreservesAllFiniteValues() {
        var icon = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow();
        for (String name : List.of("size", "fill", "weight", "grade", "opticalSize", "color", "shadows", "applyTextScaling")) {
            assertEquals(icon.property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints(), name);
        }
        assertTrue(accepts("size", new PropertyValue.IntegerValue(BigInteger.ZERO)));
        assertTrue(accepts("size", d("0.5")));
        assertFalse(accepts("size", d("-0.01")));
        for (String value : List.of("0", "0.5", "1")) assertTrue(accepts("fill", d(value)));
        for (String value : List.of("-0.001", "1.001")) assertFalse(accepts("fill", d(value)));
        for (String name : List.of("weight", "opticalSize")) {
            for (String value : List.of("0.001", "1", "32767.999")) assertTrue(accepts(name, d(value)));
            for (String value : List.of("-1", "0", "32768")) assertFalse(accepts(name, d(value)));
        }
        for (String value : List.of("-32768", "0", "32767.999")) assertTrue(accepts("grade", d(value)));
        for (String value : List.of("-32768.001", "32768")) assertFalse(accepts("grade", d(value)));
        var appBarOpacity = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.AppBar"))
                .orElseThrow().property(p("iconThemeOpacity")).orElseThrow().constraints().getFirst();
        for (String value : List.of("-100", "-0.5", "0", "0.5", "1", "2", "1e100")) {
            assertTrue(accepts("opacity", d(value)), value);
        }
        assertFalse(accepts("opacity", d("1e999")));
        assertFalse(appBarOpacity.accepts(d("-0.5")));
        assertFalse(appBarOpacity.accepts(d("2")));
        assertFalse(accepts("opacity", new PropertyValue.IntegerValue(BigInteger.ONE)));
        assertTrue(accepts("applyTextScaling", bool(false)));
        assertTrue(accepts("applyTextScaling", bool(true)));
    }

    @Test
    void exactCapabilityRejectsOptionalModeMissingDefaultNullableRawAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(10, projection.propertyContracts().size());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, true, 1, 1)), projection.slotContracts());
        for (var property : definition.properties()) {
            var contract = projection.propertyContracts().get(property.name());
            boolean mode = property.name().equals(MERGE);
            assertEquals(mode, contract.required());
            assertEquals(mode ? Optional.of("boolean:false") : Optional.empty(), contract.creationDefaultFingerprint());
            assertEquals(property.acceptedKinds(), contract.acceptedKinds());
            var nullable = new ArrayList<>(property.constraints());
            nullable.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), !mode), property.constraints(), property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), nullable, property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(properties.indexOf(property), changed);
                assertNoCapability(properties, definition.slots());
            }
        }
        var mode = definition.property(MERGE).orElseThrow();
        for (var defaultValue : List.of(Optional.<PropertyValue>empty(), Optional.<PropertyValue>of(bool(true)))) {
            var properties = new ArrayList<>(definition.properties());
            properties.set(9, new PropertyDefinition(MERGE, mode.parameter(), mode.constraints(), defaultValue));
            assertNoCapability(properties, definition.slots());
        }
        assertNoCapability(definition.properties().subList(0, 9), definition.slots());
        assertNoCapability(definition.properties(), List.of());
        assertNoCapability(definition.properties(), List.of(new SlotDefinition(CHILD, DartParameter.named(9, false),
                SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget())));
    }

    @Test
    void all1536OptionalFieldPresenceAndBooleanCombinationsRetainExactCompoundSourceEvidenceAndCodec() throws Exception {
        var codec = new FdDocumentCodec();
        var examples = examples();
        int count = 0;
        for (boolean merge : List.of(false, true)) for (int mask = 0; mask < 256; mask++) {
            for (var scale : List.of(Optional.<Boolean>empty(), Optional.of(false), Optional.of(true))) {
                count++;
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(MERGE, bool(merge));
                for (int index = 0; index < 8; index++) {
                    if ((mask & (1 << index)) != 0) properties.put(p(FIELDS.get(index)), examples.get(p(FIELDS.get(index))));
                }
                scale.ifPresent(value -> properties.put(p("applyTextScaling"), bool(value)));
                var root = node(properties, text());
                var document = document(root);
                assertTrue(valid(root));
                var generated = generated(root);
                String build = generated.build().payload();
                assertEquals(merge, build.contains("IconTheme.merge("), build);
                boolean theme = properties.containsKey(p("color")) || properties.containsKey(p("shadows"));
                assertEquals(!merge && !theme, build.contains("return const IconTheme("), build);
                assertEquals(!theme, build.contains("data: const IconThemeData("), build);
                assertFalse(build.contains("const IconTheme.merge("), build);
                assertFalse(build.contains("merge:"), build);
                assertFalse(build.contains("data: null"), build);
                assertTrue(build.indexOf("data:") < build.indexOf("child:"), build);
                for (String name : FIELDS) {
                    int expected = properties.containsKey(p(name)) ? 1 : 0;
                    // The typed shadow also has its own color argument, in either layout form.
                    if (name.equals("color") && properties.containsKey(p("shadows"))) expected++;
                    assertEquals(expected, build.split(name + ":", -1).length - 1, name + build);
                }
                var data = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("IconThemeData")).findFirst().orElseThrow();
                assertEquals("/root/properties/data", data.modelPath());
                assertEquals(Optional.of(root.id()), data.widgetId());
                var factory = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("merge")).findFirst();
                assertEquals(merge, factory.isPresent());
                factory.ifPresent(value -> assertEquals("/root/properties/merge", value.modelPath()));
                for (var symbol : generated.symbolOccurrences()) {
                    assertEquals(symbol.symbolName(), build.substring(symbol.offset(), symbol.endOffset()));
                    assertEquals(theme ? "package:flutter/material.dart" : "package:flutter/widgets.dart", symbol.libraryUri());
                }
                assertEquals(generated, generated(root));
                var bytes = codec.encode(document);
                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
                assertFalse(decoded.migrated());
                assertEquals(document, decoded.document());
                assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
            }
        }
        assertEquals(1536, count);
    }

    @Test
    void emptyDataIsAlwaysGeneratedAndFallbackConstructorIsFullyRepresentableAsExplicitLeaves() {
        var direct = node(Map.of(), text());
        assertTrue(build(direct).contains("data: const IconThemeData()"), build(direct));
        assertEquals(Map.of(MERGE, bool(false)), direct.properties());
        var fallback = new LinkedHashMap<PropertyName, PropertyValue>();
        fallback.put(p("size"), d("24"));
        fallback.put(p("fill"), d("0"));
        fallback.put(p("weight"), d("400"));
        fallback.put(p("grade"), d("0"));
        fallback.put(p("opticalSize"), d("48"));
        fallback.put(p("color"), new PropertyValue.ColorValue(0xFF000000L));
        fallback.put(p("opacity"), d("1"));
        fallback.put(p("applyTextScaling"), bool(false));
        for (boolean merge : List.of(false, true)) {
            fallback.put(MERGE, bool(merge));
            String build = build(node(fallback, text()));
            for (String expression : List.of("size: 24.0", "fill: 0.0", "weight: 400.0", "grade: 0.0", "opticalSize: 48.0",
                    "color: const Color(0xFF000000)", "opacity: 1.0", "applyTextScaling: false")) assertTrue(build.contains(expression), build);
            assertFalse(build.contains("shadows:"), build);
            assertFalse(build.contains("fallback"), build);
        }
        assertTrue(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.IconTheme.merge")).isEmpty());
        assertTrue(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.IconThemeData.fallback")).isEmpty());
    }

    @Test
    void rawOpacityAndExplicitEmptyShadowsAreNeverCollapsedOrNormalizedIntoOmission() throws Exception {
        for (boolean merge : List.of(false, true)) for (String opacity : List.of("-0.5", "0", "0.5", "1", "2")) {
            var root = node(Map.of(MERGE, bool(merge), p("opacity"), d(opacity), p("shadows"), new PropertyValue.ShadowListValue(List.of())), text());
            String build = build(root);
            assertTrue(build.contains("opacity: " + (opacity.contains(".") ? opacity : opacity + ".0")), build);
            assertTrue(build.contains("shadows: const <Shadow>[]"), build);
            assertFalse(build.contains("Opacity("), build);
            assertEquals(d(opacity), root.properties().get(p("opacity")));
            var bytes = new FdDocumentCodec().encode(document(root));
            assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, new FdDocumentCodec().decode(bytes)).document());
        }
    }

    @Test
    void sameThemeRoleOnDataColorShadowAndExistingTextRetainsDistinctPropertyProvenance() {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue("Themed"), p("styleColor"), PRIMARY), Map.of());
        var root = node(Map.of(p("color"), PRIMARY, p("shadows"), examples().get(p("shadows"))), text);
        var generated = generated(root);
        var themes = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("Theme")).toList();
        assertEquals(3, themes.size());
        assertEquals(3, themes.stream().map(value -> value.id()).distinct().count());
        assertEquals(3, themes.stream().map(value -> value.modelPath()).distinct().count());
        assertTrue(themes.stream().anyMatch(value -> value.modelPath().equals("/root/properties/color")));
        assertTrue(themes.stream().anyMatch(value -> value.modelPath().equals("/root/properties/shadows/items/0/color")));
        for (var theme : themes) assertEquals("Theme", generated.build().payload().substring(theme.offset(), theme.endOffset()));
        assertFalse(generated.build().payload().contains("const IconTheme("));
        assertFalse(generated.build().payload().contains("const IconThemeData("));
    }

    @Test
    void nestedModesPreserveLocalIconArgumentsAndConstPropagationWithoutInventingThemeFields() {
        var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(IconWidgetPropertySchema.ICON_TYPE).orElseThrow(), StableId.random());
        var properties = new LinkedHashMap<>(prototype.properties());
        properties.put(p("color"), new PropertyValue.ColorValue(0x80123456L));
        properties.put(p("size"), d("16"));
        properties.put(p("applyTextScaling"), bool(false));
        properties.put(p("fontWeight"), new PropertyValue.EnumValue("FontWeight", "w700"));
        var icon = new WidgetNode(prototype.id(), prototype.type(), properties, prototype.slots());
        var merged = node(Map.of(MERGE, bool(true), p("applyTextScaling"), bool(true)), icon);
        var outer = node(Map.of(p("size"), d("48"), p("opacity"), d("0.5")), merged);
        String source = build(outer);
        assertTrue(source.contains("return IconTheme("), source);
        assertTrue(source.contains("IconTheme.merge("), source);
        assertTrue(source.contains("child: const Icon("), source);
        assertTrue(source.contains("fontWeight: FontWeight.w700"), source);
        assertTrue(source.contains("size: 16.0"), source);
        assertTrue(source.contains("color: const Color(0x80123456)"), source);
        assertEquals(1, source.split("opacity:", -1).length - 1);
        assertEquals(2, source.split("applyTextScaling:", -1).length - 1);
        for (String name : List.of("fontWeight", "blendMode", "semanticLabel", "textDirection", "icon")) {
            assertTrue(IconThemeWidgetPropertySchema.find(p(name)).isEmpty());
        }
    }

    @Test
    void malformedKindsUnknownHelpersMissingModeAndRequiredChildFailClosed() {
        for (var property : definition().properties()) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("invalid"),
                    new PropertyValue.DartExpressionValue("IconThemeData.fallback()"))) {
                var root = node(Map.of(property.name(), invalid), text());
                assertFalse(valid(root));
                assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
        assertFalse(valid(node(Map.of(p("color"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))), text())));
        for (String name : List.of("data", "key", "fallback", "copyWith", "lerp", "resolve", "fontWeight", "blendMode")) {
            assertFalse(valid(node(Map.of(p(name), bool(true)), text())));
        }
        var complete = node(Map.of(), text());
        assertFalse(valid(new WidgetNode(complete.id(), TYPE, Map.of(), complete.slots())));
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text()))))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(MERGE, bool(false)), slots)));
        }
    }

    private static Map<PropertyName, PropertyValue> examples() {
        return Map.of(p("size"), d("32"), p("fill"), d("1"), p("weight"), d("650"), p("grade"), d("-25"),
                p("opticalSize"), d("40"), p("color"), PRIMARY, p("opacity"), d("1.5"),
                p("shadows"), new PropertyValue.ShadowListValue(List.of(new PropertyValue.ShadowListValue.Shadow(
                        StableId.parse("192489fb-3bbb-46c5-9bac-c988f412218c"), new ColorSource.Theme(PRIMARY.token()),
                        new BigDecimal("-1.25"), new BigDecimal("2.5"), BigDecimal.valueOf(4)))));
    }
    private static PropertyName p(String value) { return new PropertyName(value); }
    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(c -> c.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    private static String build(WidgetNode root) { return generated(root).build().payload(); }
    private static WidgetNode text() { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), new PropertyValue.StringValue("Icon theme child")), Map.of()); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties, WidgetNode child) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(MERGE, bool(false));
        values.putAll(properties);
        return new WidgetNode(StableId.random(), TYPE, values, Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.parse("a2f5a743-c8a0-49d7-a46a-52925a8f8363"),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var original = definition();
        var changed = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(),
                original.importUris(), original.traits(), original.palette(), properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
    }
}

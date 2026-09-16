package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefreshIndicatorContractTest {
    private static final WidgetTypeId TYPE = RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE;
    private static final List<String> NAMES = List.of("displacement", "edgeOffset", "onRefresh", "color", "backgroundColor",
            "notificationPredicate", "semanticsLabel", "semanticsValue", "strokeWidth", "triggerMode", "elevation", "onStatusChange", "variant");

    @Test
    void thirteenRowsCoverEveryConstructorWithoutPersistingSdkDefaults() {
        var definition = definition();
        assertEquals("RefreshIndicator", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material", 100, 130, "RefreshIndicator"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(13, RefreshIndicatorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, RefreshIndicatorWidgetPropertySchema.SLOT_COUNT);
        assertEquals(new LinkedHashSet<>(NAMES), RefreshIndicatorWidgetPropertySchema.definitions().keySet());
        for (int index = 0; index < NAMES.size(); index++) {
            String name = NAMES.get(index);
            int order = index < 11 ? index : index + 1;
            var property = definition.property(p(name)).orElseThrow();
            assertEquals(DartParameter.named(order, name.equals("variant")), property.parameter());
            assertEquals(name.equals("variant"), property.creationDefault().isPresent());
            var schema = RefreshIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(order, schema.dartOrder());
            assertEquals(name, schema.dartName());
            assertFalse(schema.description().isBlank());
        }
        var child = definition.slots().getFirst();
        assertEquals(new SlotName("child"), child.name());
        assertEquals(DartParameter.named(11, true), child.parameter());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(p("variant"), s("material")), prototype.properties());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertTrue(WidgetPlacementRules.capabilityFingerprintLines(definition).contains(
                "C|flutter.material.RefreshIndicator|paletteCreate|wrapExistingChild|child"));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void eachConstructorHasRequiredFallbackCallbackAndNeverLeaksVariantOrStatus() {
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            var output = generated(node(Map.of(p("variant"), s(variant))));
            String source = output.build().payload();
            assertTrue(source.contains("return RefreshIndicator" + (variant.equals("material") ? "(" : "." + variant + "(")), source);
            assertTrue(source.contains("onRefresh: () async {}"), source);
            assertTrue(source.contains("child: const Text('Child')"), source);
            assertFalse(source.contains("variant:"), source);
            assertFalse(source.contains("onStatusChange:"), source);
            assertFalse(source.contains("const RefreshIndicator"), source);
            assertFalse(output.imports().payload().contains("dart:async"));
            assertFalse(output.imports().payload().contains("dart:core"));
            assertEquals(variant.equals("material") ? 0 : 1, output.symbolOccurrences().stream()
                    .filter(symbol -> symbol.modelPath().equals("/root/properties/variant")).count());
            assertSymbols(output);
        }
    }

    @Test
    void everyBranchSpecificConflictFailsClosedAndEverySharedFieldWorksInAllBranches() {
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            var values = fullValues(variant);
            assertTrue(valid(node(values)), variant);
            var source = generated(node(values)).build().payload();
            for (String name : values.keySet().stream().map(PropertyName::value).toList()) {
                assertEquals(!name.equals("variant"), source.contains(name + ":"), name + source);
            }
            for (String spinner : RefreshIndicatorWidgetPropertySchema.spinnerOnlyProperties()) {
                var one = new LinkedHashMap<>(values);
                one.put(p(spinner), fullValues("material").get(p(spinner)));
                assertEquals(!variant.equals("noSpinner"), valid(node(one)), spinner);
            }
            var status = new LinkedHashMap<>(values);
            status.put(p("onStatusChange"), reference(false, false, 0));
            assertEquals(variant.equals("noSpinner"), valid(node(status)));
        }
        var missingMode = new WidgetNode(StableId.random(), TYPE, Map.of(), node(Map.of()).slots());
        assertFalse(valid(missingMode));
        for (String invalid : List.of("", "normal", "NoSpinner", "adaptive()")) assertFalse(valid(node(Map.of(p("variant"), s(invalid)))));
    }

    @Test
    void numericBoundsPreserveSignedOffsetsWidthsAndRejectNullInfinityAndRawCode() {
        for (String name : List.of("displacement", "edgeOffset", "strokeWidth", "elevation")) {
            boolean signed = name.equals("edgeOffset") || name.equals("strokeWidth");
            for (PropertyValue value : List.of(i(0), i(2), d("0.5"), d("1e308"))) assertTrue(accepts(name, value));
            for (PropertyValue value : List.of(i(-1), d("-0.5"))) assertEquals(signed, accepts(name, value));
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.EnumValue("double", "infinity"),
                    new PropertyValue.StringValue("2.5"), new PropertyValue.DartExpressionValue("2.5"),
                    d("1e999"), d("1e-999"), new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))))
                assertFalse(accepts(name, value), name + ": " + value);
        }
        assertTrue(generated(node(Map.of(p("edgeOffset"), d("-2"), p("strokeWidth"), i(-3)))).build().payload().contains("strokeWidth: -3"));
    }

    @Test
    void predicatePresetBranchesAndUnsetHaveExactSourceAndProvenance() {
        assertEquals(List.of("default", "depthZero", "all"), RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets());
        assertFalse(generated(node(Map.of())).build().payload().contains("notificationPredicate:"));
        for (String preset : RefreshIndicatorWidgetPropertySchema.notificationPredicatePresets()) {
            var output = generated(node(Map.of(p("notificationPredicate"), s(preset))));
            String expected = switch (preset) {
                case "default" -> "defaultScrollNotificationPredicate";
                case "depthZero" -> "(notification) => notification.depth == 0";
                default -> "(_) => true";
            };
            assertTrue(output.build().payload().contains("notificationPredicate: " + expected));
            assertEquals(preset.equals("default") ? 1 : 0, output.symbolOccurrences().stream()
                    .filter(symbol -> symbol.symbolName().equals("defaultScrollNotificationPredicate")).count());
            assertSymbols(output);
        }
        for (PropertyValue invalid : List.of(s("all()"), new PropertyValue.CallbackValue("predicate"),
                new PropertyValue.NullValue(), new PropertyValue.DartExpressionValue("(_) => true"))) assertFalse(accepts("notificationPredicate", invalid));
    }

    @Test
    void allThirtySixTypedFunctionReferenceFormsHaveExactExpectedTypeProofsAndSourceOffsets() throws Exception {
        var codec = new FdDocumentCodec();
        var types = Map.of("onRefresh", "RefreshCallback", "notificationPredicate", "ScrollNotificationPredicate",
                "onStatusChange", "ValueChanged<RefreshIndicatorStatus?>");
        int cases = 0;
        for (var entry : types.entrySet()) for (boolean imported : List.of(false, true))
            for (boolean member : List.of(false, true)) for (int mode = 0; mode < 3; mode++) {
                var value = reference(imported, member, mode);
                var root = node(Map.of(p("variant"), s(entry.getKey().equals("onStatusChange") ? "noSpinner" : "material"), p(entry.getKey()), value));
                var output = generated(root);
                var proofs = output.symbolOccurrences().stream().flatMap(symbol -> symbol.staticTypeRequirement().stream()).toList();
                assertEquals(1, proofs.size());
                assertEquals(entry.getValue(), proofs.getFirst().expectedDartType());
                assertEquals(imported, output.imports().payload().contains("package:refresh/handlers.dart"));
                assertEquals(entry.getKey().equals("onRefresh") && mode == 2, output.build().payload().contains("return const RefreshIndicator("));
                assertEquals(!entry.getKey().equals("onRefresh"), output.build().payload().contains("onRefresh: () async {}"));
                assertSymbols(output);
                var document = document(root);
                assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
                cases++;
            }
        assertEquals(36, cases);
    }

    @Test
    void colorThemesAndConstructorAliasesRetainExactSourceProvenance() {
        for (String variant : List.of("material", "adaptive", "noSpinner")) {
            var values = fullValues(variant);
            values.put(p("onRefresh"), new PropertyValue.DartObjectReferenceValue(Optional.of("package:refresh/handlers.dart"),
                    "RefreshIndicator", Optional.of("adaptive"), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true)));
            var output = generated(node(values));
            assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("RefreshIndicator")
                    && symbol.libraryUri().equals("package:refresh/handlers.dart") && symbol.modelPath().equals("/root/properties/onRefresh/rootSymbol")));
            if (!variant.equals("material")) assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals(variant)
                    && symbol.libraryUri().equals("package:flutter/material.dart") && symbol.modelPath().equals("/root/properties/variant")));
            if (!variant.equals("noSpinner")) {
                assertTrue(output.build().payload().contains("Theme.of(context).colorScheme.primary"));
                assertFalse(output.build().payload().contains("return const RefreshIndicator"));
                assertEquals(2, output.symbolOccurrences().stream().filter(symbol -> symbol.symbolName().equals("Theme")
                        && Set.of("/root/properties/color", "/root/properties/backgroundColor").contains(symbol.modelPath())).count());
            }
        }
    }

    @Test
    void requiredChildNeverAcceptsEmptyOrFlexParentDataAndEveryOrdinaryChildIsValid() {
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(p("variant"), s("material")), Map.of())));
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(p("variant"), s("material")),
                Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.empty())))));
        var definition = definition();
        var slot = definition.slots().getFirst();
        int accepted = 0;
        for (var child : BuiltInWidgetCatalog.getDefault().definitions()) {
            boolean expected = !Set.of("flutter.widgets.Expanded", "flutter.widgets.Flexible", "flutter.widgets.Spacer", "flutter.widgets.LayoutId", "flutter.widgets.TableRow", "flutter.widgets.TableCell", "flutter.material.DataColumn", "flutter.material.DataRow", "flutter.material.DataRow.byIndex", "flutter.material.DataCell", "flutter.material.DataCell.empty").contains(child.typeId().value()) && !WidgetPlacementRules.isStackPositionedWidget(child) && !WidgetPlacementRules.isSliverWidget(child);
            assertEquals(expected, WidgetPlacementRules.accepts(definition, slot, child));
            if (expected) accepted++;
        }
        assertEquals(183, accepted);
        assertTrue(valid(node(Map.of())), "Any Widget child is legal even without scrollables; runtime then has no pull gesture");
    }

    @Test
    void unsupportedControllerSpinnerAndLegacyCallbackArgumentsRejectBeforeGeneration() {
        for (String name : List.of("controller", "value", "valueColor", "strokeAlign", "strokeCap", "indicatorMargin", "year2023", "key")) {
            assertFalse(valid(node(Map.of(p(name), d("1")))));
            assertTrue(RefreshIndicatorWidgetPropertySchema.find(p(name)).isEmpty());
        }
        for (String name : List.of("onRefresh", "onStatusChange")) for (PropertyValue value : List.of(
                new PropertyValue.CallbackValue("refresh"), new PropertyValue.NullValue(), s("refresh"), new PropertyValue.DartExpressionValue("() async {}"))) {
            assertFalse(accepts(name, value));
        }
        for (String member : List.of("onEdge", "anywhere")) assertTrue(accepts("triggerMode", new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", member)));
        assertFalse(accepts("triggerMode", new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", "all")));
    }

    static LinkedHashMap<PropertyName, PropertyValue> fullValues(String variant) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("variant"), s(variant));
        values.put(p("onRefresh"), reference(false, false, 0));
        values.put(p("notificationPredicate"), s("all"));
        values.put(p("semanticsLabel"), s("Refresh records"));
        values.put(p("semanticsValue"), s("45%"));
        values.put(p("triggerMode"), new PropertyValue.EnumValue("RefreshIndicatorTriggerMode", "anywhere"));
        values.put(p("elevation"), i(0));
        if (variant.equals("noSpinner")) values.put(p("onStatusChange"), reference(true, true, 0));
        else {
            values.put(p("displacement"), d("24.5"));
            values.put(p("edgeOffset"), d("-2"));
            values.put(p("strokeWidth"), d("-2.5"));
            for (String name : List.of("color", "backgroundColor")) values.put(p(name), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
        }
        return values;
    }

    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean member, int mode) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:refresh/handlers.dart") : Optional.empty(),
                "refreshHandler", member ? Optional.of("configured") : Optional.empty(),
                mode == 0 ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                mode == 0 ? Optional.empty() : Optional.of(mode == 2));
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) {
        var all = new LinkedHashMap<PropertyName, PropertyValue>();
        all.put(p("variant"), s("material"));
        all.putAll(values);
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(p("data"), s("Child")), Map.of());
        return new WidgetNode(StableId.random(), TYPE, all, Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(child))));
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
}

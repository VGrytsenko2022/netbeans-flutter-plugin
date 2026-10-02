package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.ListTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class ListTileContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void completeConstructorHas176PropertiesFourOptionalSlotsAndNoCreationDefaults() {
        assertEquals(176, definition().properties().size());
        assertEquals(176, ListTileWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(33, ListTileWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(java.util.stream.IntStream.range(4, 180).boxed().toList(), definition().properties().stream().map(v -> v.parameter().order()).toList());
        assertEquals(List.of("leading", "title", "subtitle", "trailing"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(List.of(0, 1, 2, 3), definition().slots().stream().map(v -> v.parameter().order()).toList());
        assertTrue(definition().properties().stream().noneMatch(v -> v.parameter().required() || v.creationDefault().isPresent()));
        assertTrue(definition().slots().stream().noneMatch(v -> v.parameter().required()));
        assertEquals("package:flutter/material.dart", definition().dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 250, "ListTile"), definition().palette());
        assertTrue(definition().constConstructor());
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(), prototype.properties());
        assertTrue(valid(prototype));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).isEmpty());
        for (String name : List.of("style", "titleAlignment")) assertEquals("package:flutter/material.dart", definition().property(p(name)).orElseThrow().constraints().stream()
                .filter(PropertyValueConstraint.EnumValues.class::isInstance).map(PropertyValueConstraint.EnumValues.class::cast).findFirst().orElseThrow().dartType().libraryUri());
    }

    @Test
    void independentProjectionMatches181RecordsAndRejectsWeakenedInsetsMetadata() throws Exception {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        String all = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = all.indexOf("W|flutter.material.ListTile\n");
        int end = all.indexOf("W|", start + 2);
        String contract = all.substring(start, end < 0 ? all.length() : end);
        assertEquals(181, contract.lines().count());
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/list-tile-contract.txt"), contract);
        var changedProperties = definition().properties().stream().map(v -> v.name().equals(p("contentPadding"))
                ? new PropertyDefinition(v.name(), v.parameter(), List.of(new PropertyValueConstraint.EdgeInsetsValues(true)), v.creationDefault()) : v).toList();
        var changed = new WidgetDefinition(ListTileWidgetPropertySchema.LIST_TILE_TYPE, definition().dartClassName(), Optional.empty(), true,
                definition().dartLibraryUri(), definition().importUris(), Set.of(), definition().palette(), changedProperties, definition().slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
    }

    @Test
    void allSixteenOptionalSlotCombinationsPreserveIdentityAndEmptyConstructor() throws Exception {
        for (int mask = 0; mask < 16; mask++) {
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            int index = 0;
            for (String name : List.of("leading", "title", "subtitle", "trailing")) {
                if ((mask & (1 << index++)) != 0) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(text(name))));
            }
            var root = new WidgetNode(StableId.random(), ListTileWidgetPropertySchema.LIST_TILE_TYPE, Map.of(), slots);
            String source = generated(root).build().payload();
            assertTrue(source.contains("const ListTile("));
            assertFalse(source.contains("onTap:"));
            assertFalse(source.contains("onLongPress:"));
            for (String name : List.of("leading", "title", "subtitle", "trailing")) assertEquals(slots.containsKey(new SlotName(name)), source.contains("      " + name + ":"));
            roundTrip(root);
        }
        assertTrue(valid(node(Map.of(p("isThreeLine"), nil()))));
        assertTrue(valid(node(Map.of(p("isThreeLine"), b(false)))));
        assertFalse(valid(node(Map.of(p("isThreeLine"), b(true)))));
        assertTrue(valid(new WidgetNode(StableId.random(), ListTileWidgetPropertySchema.LIST_TILE_TYPE, Map.of(p("isThreeLine"), b(true)), Map.of(new SlotName("subtitle"), new WidgetSlot.SingleSlot(Optional.of(text("subtitle")))))));
    }

    @Test
    void complementaryTenShapeAndThreeTextStyleFamiliesCoverEveryOneOf176Fields() throws Exception {
        var covered = new HashSet<String>();
        for (String shape : ListTileWidgetPropertySchema.shapeKinds()) for (boolean paints : List.of(false, true)) {
            var root = fullNode(shape, paints);
            covered.addAll(root.properties().keySet().stream().map(PropertyName::value).toList());
            assertTrue(root.properties().size() <= 512);
            var output = generated(root);
            assertSymbols(output);
            for (String family : ListTileWidgetPropertySchema.styleFamilies()) assertTrue(output.build().payload().contains(family + ":"));
            roundTrip(root);
        }
        for (String name : ListTileWidgetPropertySchema.definitions().keySet()) {
            if (covered.contains(name)) continue;
            var root = node(Map.of(p(name), value(name)));
            generated(root);
            roundTrip(root);
            covered.add(name);
        }
        assertEquals(ListTileWidgetPropertySchema.definitions().keySet(), covered);
    }

    @Test
    void plainColorsRemainPlainWhileNineStateMapsHaveExplicitNonNullDefaultsAndExactPriority() throws Exception {
        for (String family : ListTileWidgetPropertySchema.stateColorFamilies()) {
            assertTrue(generated(node(Map.of(p(family), CheckboxTestValues.color()))).build().payload().contains(family + ": const "));
            var local = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String name : ListTileWidgetPropertySchema.colorStateProperties(family)) local.put(p(name), CheckboxTestValues.theme());
            var output = generated(node(local));
            String source = output.build().payload();
            assertTrue(source.contains("WidgetStateColor.fromMap"));
            assertFalse(source.contains("Color?>"));
            int previous = -1;
            for (String state : ListTileWidgetPropertySchema.statePriority()) {
                String token = state.equals("default") ? "WidgetState.any" : "WidgetState." + state;
                int position = source.indexOf(token);
                assertTrue(position > previous, token + source);
                previous = position;
            }
            assertFalse(valid(node(Map.of(p(family + "Disabled"), CheckboxTestValues.color()))));
            assertFalse(valid(node(Map.of(p(family + "Default"), nil()))));
            local.put(p(family), CheckboxTestValues.color());
            assertFalse(valid(node(local)));
        }
    }

    @Test
    void all41CursorPresetsAndReferenceStateMapsHaveNoInventedFallback() throws Exception {
        assertEquals(41, ListTileWidgetPropertySchema.mouseCursorPresets().size());
        for (String preset : ListTileWidgetPropertySchema.mouseCursorPresets()) generated(node(Map.of(p("mouseCursor"), s(preset))));
        var local = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : ListTileWidgetPropertySchema.mouseCursorStateProperties()) local.put(p(name), reference(name));
        var output = generated(node(local));
        assertTrue(output.build().payload().contains("WidgetStateMouseCursor.fromMap"));
        assertEquals(9, output.symbolOccurrences().stream().filter(v -> v.staticTypeRequirement().map(t -> t.expectedDartType().equals("MouseCursor")).orElse(false)).count());
        assertSymbols(output);
        roundTrip(node(local));
        assertFalse(valid(node(Map.of(p("mouseCursorPressed"), s("click")))));
        assertFalse(valid(node(Map.of(p("mouseCursorDefault"), nil()))));
        local.put(p("mouseCursor"), s("basic"));
        assertFalse(valid(node(local)));
    }

    @Test
    void disabledCallbacksRetainSourceSymbolsAndExactNullNoopAndOmissionSemantics() throws Exception {
        for (String name : List.of("onTap", "onLongPress", "onFocusChange")) {
            assertFalse(generated(node(Map.of(p("enabled"), b(false)))).build().payload().contains(name + ":"));
            assertTrue(generated(node(Map.of(p("enabled"), b(false), p(name), nil()))).build().payload().contains(name + ": null"));
            var noop = generated(node(Map.of(p("enabled"), b(false), p(name), s("noop"))));
            assertTrue(noop.build().payload().contains(name + (name.equals("onFocusChange") ? ": (_) {}" : ": () {}")));
            assertFalse(noop.build().payload().contains("const ListTile("));
            var root = node(Map.of(p("enabled"), b(false), p(name), reference(name), p("internalAddSemanticForOnTap"), b(true)));
            var output = generated(root);
            assertTrue(output.build().payload().contains(name + ":"));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().contains("/" + name + "/") && v.staticTypeRequirement().isPresent()));
            assertSymbols(output);
            roundTrip(root);
        }
    }

    @Test
    void signedGeometryNullAndAllThreeConstantsRemainExactWithoutCoreImports() throws Exception {
        for (String name : ListTileWidgetPropertySchema.geometryProperties()) {
            for (String member : List.of("infinity", "negativeInfinity", "nan")) {
                var root = node(Map.of(p(name), new PropertyValue.EnumValue("double", member)));
                var output = generated(root);
                assertTrue(output.build().payload().contains(member.equals("nan") ? "(0.0 / 0.0)" : member.equals("infinity") ? "(1.0 / 0.0)" : "(-1.0 / 0.0)"));
                assertFalse(output.imports().payload().contains("dart:core"));
                roundTrip(root);
            }
            for (PropertyValue value : List.of(d("-1e308"), d("1e308"), nil(), BadgeTestValues.i(9007199254740991L), BadgeTestValues.i(-9007199254740991L))) generated(node(Map.of(p(name), value)));
            for (PropertyValue value : List.of(d("1e400"), BadgeTestValues.i(9007199254740992L), new PropertyValue.EnumValue("double", "maxFinite"), new PropertyValue.DartExpressionValue("unsafe()"))) assertFalse(valid(node(Map.of(p(name), value))), name);
        }
        for (String name : List.of("isThreeLine", "dense", "enableFeedback")) assertTrue(generated(node(Map.of(p(name), nil()))).build().payload().contains(name + ": null"));
        for (String name : List.of("enabled", "selected", "autofocus", "internalAddSemanticForOnTap")) assertFalse(valid(node(Map.of(p(name), nil()))));
    }

    @Test
    void wholeReferenceUnionsPreserveStrictExpectedTypesAndAllSourceForms() throws Exception {
        var names = List.of("visualDensity", "shape", "selectedColor", "iconColor", "textColor", "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle", "contentPadding", "onTap", "onLongPress", "onFocusChange", "mouseCursor", "focusColor", "hoverColor", "splashColor", "focusNode", "tileColor", "selectedTileColor", "statesController");
        for (String name : names) for (boolean imported : List.of(false, true)) for (boolean invocation : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:tile_values/values.dart") : Optional.empty(), "Values", Optional.of(name),
                    invocation ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    invocation ? Optional.of(false) : Optional.empty());
            var root = node(Map.of(p(name), reference));
            var output = generated(root);
            String expected = definition().property(p(name)).orElseThrow().constraints().stream().filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType();
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.staticTypeRequirement().map(t -> t.expectedDartType().equals(expected)).orElse(false)), name);
            assertSymbols(output);
            roundTrip(root);
        }
    }

    @Test
    void compoundExclusivityDependenciesAndSignedInsetsAreExact() {
        assertTrue(valid(node(Map.of(p("contentPadding"), value("contentPadding")))));
        assertTrue(valid(node(Map.of(p("contentPadding"), new PropertyValue.EdgeInsetsValue(new java.math.BigDecimal("-2"), java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE, java.math.BigDecimal.TWO)))));
        assertFalse(valid(node(Map.of(p("visualDensity"), reference("density"), p("visualDensityHorizontal"), d("1")))));
        assertFalse(valid(node(Map.of(p("shape"), reference("shape"), p("shapeKind"), s("circle")))));
        for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
            assertFalse(valid(node(Map.of(p(family), reference("textStyle"), p(family + "FontSize"), d("12")))));
            assertFalse(valid(node(Map.of(p(family + "Color"), CheckboxTestValues.color(), p(family + "Foreground"), BadgeTestValues.paint()))));
            assertFalse(valid(node(Map.of(p(family + "BackgroundColor"), CheckboxTestValues.color(), p(family + "Background"), BadgeTestValues.paint()))));
            assertFalse(valid(node(Map.of(p(family + "Package"), s("fonts")))));
            assertTrue(valid(node(Map.of(p(family + "Package"), s("fonts"), p(family + "FontFamily"), s("Font")))));
        }
        for (String name : List.of("variant", "child", "title", "clipBehavior", "showDivider", "onChanged")) assertFalse(valid(node(Map.of(p(name), s("unsupported")))));
        assertFalse(valid(new WidgetNode(StableId.random(), ListTileWidgetPropertySchema.LIST_TILE_TYPE, Map.of(), Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(text("x")))))));
    }

    @Test
    void independentLiteralTextStylesAreConstAndDoNotSynthesizeThemeOrComponentDefaults() throws Exception {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String family : ListTileWidgetPropertySchema.styleFamilies()) {
            values.put(p(family + "FontSize"), d("18"));
            values.put(p(family + "Color"), CheckboxTestValues.color());
            values.put(p(family + "LocaleLanguageCode"), s("uk"));
        }
        var root = node(values);
        var output = generated(root);
        assertTrue(output.build().payload().contains("const ListTile("));
        assertFalse(output.build().payload().contains("Theme.of("));
        assertFalse(output.build().payload().contains("copyWith("));
        assertEquals(3, output.build().payload().split("TextStyle\\(", -1).length - 1);
        assertSymbols(output);
        roundTrip(root);
    }

    @Test
    void aggregateCountsAndUnchangedBudgetsAreExecutable() {
        var definitions = CATALOG.definitions();
        assertEquals(249, definitions.size());
        assertEquals(205, definitions.stream().filter(WidgetDefinition::constConstructor).count());
        assertEquals(8476, definitions.stream().mapToInt(v -> v.properties().size()).sum());
        assertEquals(742, definitions.stream().flatMap(v -> v.properties().stream()).filter(v -> v.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN))).count());
        assertEquals(58, definitions.stream().flatMap(v -> v.properties().stream()).filter(v -> v.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL))).count());
        assertEquals(75, definitions.stream().filter(v -> v.palette().categoryId().equals("flutter.material")).count());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
    }

    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics());
        return result.generated().orElseThrow();
    }
    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.id());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

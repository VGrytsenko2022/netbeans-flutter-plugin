package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.vgrytsenko2022.designer.catalog.FloatingActionButtonTestValues.*;

class FloatingActionButtonContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final SlotName CHILD = new SlotName("child"), ICON = new SlotName("icon");

    @Test
    void exactFourConstructorUnionHas78RowsTwoSlotsAndOnlyTwoCreationDefaults() {
        var definition = definition();
        assertEquals(78, definition.properties().size());
        assertEquals(78, FloatingActionButtonWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(2, FloatingActionButtonWidgetPropertySchema.SLOT_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 170, "FloatingActionButton"), definition.palette());
        assertTrue(definition.constConstructor());
        assertTrue(definition.traits().isEmpty());
        assertEquals(new ArrayList<>(FloatingActionButtonWidgetPropertySchema.definitions().keySet()), definition.properties().stream().map(p -> p.name().value()).toList());
        for (var property : definition.properties()) {
            var hint = FloatingActionButtonWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(DartParameter.named(hint.dartOrder(), Set.of("variant", "enabled").contains(property.name().value())), property.parameter());
            assertFalse(hint.description().isBlank());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        assertEquals(DartParameter.named(0, true), definition.slot(CHILD).orElseThrow().parameter());
        assertEquals(DartParameter.named(56, false), definition.slot(ICON).orElseThrow().parameter());
        assertTrue(definition.slots().stream().allMatch(s -> s.minChildren() == 0 && s.maxChildren() == 1 && s.acceptance() instanceof SlotAcceptance.AnyWidget));
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(p("variant"), s("standard"), p("enabled"), new PropertyValue.BooleanValue(true)), prototype.properties());
        assertTrue(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertTrue(generated(prototype).build().payload().contains("onPressed: () {}"));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void allFourConstructorsAreConstCapableAndDoNotInventThemeDefaults() {
        for (String variant : FloatingActionButtonWidgetPropertySchema.variants()) {
            var root = node(Map.of(p("variant"), s(variant), p("enabled"), new PropertyValue.BooleanValue(false)));
            var output = generated(root);
            String source = output.build().payload();
            String constructor = "FloatingActionButton" + (variant.equals("standard") ? "" : "." + variant);
            assertTrue(source.contains("return const " + constructor + "("), source);
            assertTrue(source.contains("onPressed: null"), source);
            assertTrue(source.contains((variant.equals("extended") ? "label" : "child") + ": const Text('Label')"), source);
            for (String absent : List.of("variant:", "enabled:", "heroTag:", "clipBehavior:", "elevation:", "extendedTextStyle:", "FloatingActionButtonTheme", "TextButton", "FilledButton", "dart:core")) assertFalse(source.contains(absent), source);
            if (!variant.equals("standard")) assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals(variant) && v.modelPath().equals("/root/properties/variant")));
            assertSymbols(output);
            var explicit = new LinkedHashMap<>(root.properties());
            explicit.put(p("clipBehavior"), new PropertyValue.EnumValue("Clip", "none"));
            assertTrue(generated(new WidgetNode(root.id(), root.type(), explicit, root.slots())).build().payload().contains("clipBehavior: Clip.none"));
            explicit.put(p("clipBehavior"), new PropertyValue.NullValue());
            assertFalse(valid(new WidgetNode(root.id(), root.type(), explicit, root.slots())));
        }
    }

    @Test
    void all80DenseConstructorShapePaintFamiliesRoundTripAndCoverEveryProjectedLeaf() throws Exception {
        Set<PropertyName> visited = new HashSet<>();
        var codec = new FdDocumentCodec();
        int cases = 0;
        for (String variant : FloatingActionButtonWidgetPropertySchema.variants()) for (String shape : FloatingActionButtonWidgetPropertySchema.shapeKinds()) for (boolean paints : List.of(false, true)) {
            var properties = full(variant, shape, paints);
            var root = node(properties);
            if (variant.equals("extended")) root = new WidgetNode(root.id(), root.type(), root.properties(), Map.of(CHILD, root.slots().get(CHILD), ICON, WidgetSlot.SingleSlot.of(text("Icon"))));
            assertTrue(valid(root), properties.toString());
            visited.addAll(root.properties().keySet());
            var output = generated(root);
            assertSymbols(output);
            assertFalse(output.build().payload().contains("shapeKind:"));
            assertEquals(variant.equals("extended"), output.build().payload().contains("extendedTextStyle:"));
            assertFalse(output.build().payload().contains("Card("));
            var document = document(root);
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document();
            assertEquals(document, decoded);
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded).copyBytes());
            assertEquals(output.build().payload(), generated(decoded.root()).build().payload());
            cases++;
        }
        visited.add(p("shape"));
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), visited);
        assertEquals(80, cases);
    }

    @Test
    void all41CursorOwnersAndMembersHaveExactPropertyProvenance() {
        for (String preset : FloatingActionButtonWidgetPropertySchema.mouseCursorPresets()) {
            var output = generated(node(Map.of(p("mouseCursor"), s(preset), p("enabled"), new PropertyValue.BooleanValue(false))));
            var symbols = output.symbolOccurrences().stream().filter(v -> v.modelPath().equals("/root/properties/mouseCursor")).toList();
            assertEquals(2, symbols.size());
            String owner = Set.of("defer", "uncontrolled").contains(preset) ? "MouseCursor"
                    : Set.of("clickable", "adaptiveClickable", "textable").contains(preset) ? "WidgetStateMouseCursor" : "SystemMouseCursors";
            assertEquals(List.of(owner, preset), symbols.stream().map(GeneratedDartSymbolOccurrence::symbolName).toList());
            assertTrue(output.build().payload().contains("return const FloatingActionButton("));
            assertSymbols(output);
        }
        assertEquals(41, FloatingActionButtonWidgetPropertySchema.mouseCursorPresets().size());
        assertFalse(valid(node(Map.of(p("mouseCursor"), s("MouseCursor.defer")))));
    }

    @Test
    void heroLiteralsExplicitNullAndAllClosedReferenceFormsStayDistinctAndDoNotAcceptNonfiniteNumbers() throws Exception {
        var values = new ArrayList<PropertyValue>(List.of(new PropertyValue.NullValue(), s("a'\\\n$tag"), s(""), i(-123), i(0),
                new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER), d("-1.25"), d("1e308"), new PropertyValue.BooleanValue(true), new PropertyValue.BooleanValue(false)));
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean call : List.of(false, true)) {
            values.add(new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/heroes.dart") : Optional.empty(),
                    member ? "Tags" : "tag", member ? Optional.of("primary") : Optional.empty(),
                    call ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    call ? Optional.of(true) : Optional.empty()));
        }
        for (PropertyValue value : values) {
            var root = node(Map.of(p("heroTag"), value, p("enabled"), new PropertyValue.BooleanValue(false)));
            assertTrue(valid(root), value.toString());
            var output = generated(root);
            assertTrue(output.build().payload().contains("heroTag:"));
            assertSymbols(output);
            assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, new FdDocumentCodec().decode(new FdDocumentCodec().encode(document(root)))).document());
            if (value instanceof PropertyValue.DartObjectReferenceValue) assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().startsWith("/root/properties/heroTag/") && v.staticTypeRequirement().map(t -> t.expectedDartType().equals("Object")).orElse(false)));
        }
        for (var invalid : List.of(d("1e400"), d("1e-400"), infinity(), new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)), new PropertyValue.ColorValue(0xff000000L), new PropertyValue.CallbackValue("callback"), new PropertyValue.DartExpressionValue("Object()"))) {
            assertFalse(valid(node(Map.of(p("heroTag"), invalid))), invalid.toString());
        }
    }

    @Test
    void inactiveCallbacksHaveNoGeneratedProofAndReenablePreservesExactReference() {
        var active = node(Map.of(p("onPressed"), reference("pressed")));
        assertTrue(generated(active).build().payload().contains("fabValues.pressed"));
        assertFalse(generated(active).build().payload().contains("() {}"));
        var disabled = node(Map.of(p("onPressed"), reference("pressed"), p("enabled"), new PropertyValue.BooleanValue(false)));
        var output = generated(disabled);
        assertFalse(output.imports().payload().contains("fab_values.dart"));
        assertTrue(output.symbolOccurrences().stream().noneMatch(v -> v.modelPath().startsWith("/root/properties/onPressed")));
        assertTrue(output.build().payload().contains("return const FloatingActionButton("));
        assertTrue(disabled.properties().containsKey(p("onPressed")));
    }

    @Test
    void exactTypedDomainsShareAnalyzerWitnessesWithoutAllowingRawCodeOrNullableOuterTypes() {
        Map<String, String> types = Map.of("onPressed", "VoidCallback", "heroTag", "Object", "shape", "ShapeBorder", "focusNode", "FocusNode", "mouseCursor", "MouseCursor");
        for (var entry : types.entrySet()) {
            var output = generated(node(Map.of(p(entry.getKey()), reference(entry.getKey()))));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().startsWith("/root/properties/" + entry.getKey() + "/") && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(entry.getValue())).orElse(false)), entry.toString());
            assertFalse(valid(node(Map.of(p(entry.getKey()), new PropertyValue.DartExpressionValue("anything()")))));
        }
    }

    @Test
    void fiveElevationsAndSpacingAdmitInfinityWithoutNewCoreImportsOrFabricatedOccurrences() {
        for (String name : List.of("elevation", "focusElevation", "hoverElevation", "highlightElevation", "disabledElevation", "extendedIconLabelSpacing")) {
            for (PropertyValue value : List.of(i(0), d("0.125"), d("1e308"), infinity())) {
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(p("enabled"), new PropertyValue.BooleanValue(false));
                properties.put(p("variant"), s("extended"));
                properties.put(p(name), value);
                var root = node(properties);
                assertTrue(valid(root));
                var output = generated(root);
                assertFalse(output.imports().payload().contains("dart:core"));
                assertTrue(output.symbolOccurrences().stream().noneMatch(v -> v.libraryUri().equals("dart:core")));
                assertEquals(value.equals(infinity()), output.build().payload().contains("(1.0 / 0.0)"));
                assertTrue(output.build().payload().contains("return const FloatingActionButton.extended("));
            }
            assertEquals(name.equals("extendedIconLabelSpacing"), valid(node(Map.of(p("variant"), s("extended"), p(name), d("-2.5")))));
            for (var invalid : List.of(d("1e400"), new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.NullValue(), s("Infinity"))) assertFalse(valid(node(Map.of(p("variant"), s("extended"), p(name), invalid))));
        }
    }

    @Test
    void constructorBranchAndConditionalChildRelationsRejectLossAndUnsupportedInheritedArguments() {
        var empty = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        for (String variant : FloatingActionButtonWidgetPropertySchema.variants()) {
            var values = new LinkedHashMap<>(empty.properties()); values.put(p("variant"), s(variant));
            assertEquals(!variant.equals("extended"), valid(new WidgetNode(empty.id(), empty.type(), values, empty.slots())));
            for (var property : definition().properties()) if (!FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(property.name().value(), variant)) {
                var invalid = new LinkedHashMap<>(values); invalid.put(property.name(), value(property.name().value()));
                assertFalse(valid(node(invalid)), variant + ":" + property.name());
            }
            var withIcon = node(values);
            withIcon = new WidgetNode(withIcon.id(), withIcon.type(), withIcon.properties(), Map.of(CHILD, withIcon.slots().get(CHILD), ICON, WidgetSlot.SingleSlot.of(text("icon"))));
            assertEquals(variant.equals("extended"), valid(withIcon));
        }
        assertFalse(valid(new WidgetNode(empty.id(), empty.type(), empty.properties(), Map.of())));
        for (String required : List.of("variant", "enabled")) {
            var missing = new LinkedHashMap<>(empty.properties()); missing.remove(p(required));
            assertFalse(valid(new WidgetNode(empty.id(), empty.type(), missing, empty.slots())));
        }
        for (String name : List.of("label", "key", "isSemanticButton", "style", "onLongPress", "iconAlignment", "statesController")) assertFalse(valid(node(Map.of(p(name), s("unsupported")))));
    }

    @Test
    void typedShapeAndBuiltInRelationshipsRemainClosedAndStrictAcrossAllKinds() {
        assertTrue(valid(node(Map.of(p("shape"), reference("shape")))));
        for (String kind : FloatingActionButtonWidgetPropertySchema.shapeKinds()) {
            for (String name : FloatingActionButtonWidgetPropertySchema.builtInShapePropertyNames()) {
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(p("shapeKind"), s(kind));
                if (!name.equals("shapeKind")) properties.put(p(name), value(name));
                assertEquals(!FloatingActionButtonWidgetPropertySchema.isShapeDetailProperty(name) || FloatingActionButtonWidgetPropertySchema.shapePropertyAppliesToKind(name, kind), valid(node(properties)), kind + ":" + name);
                properties.put(p("shape"), reference("shape"));
                assertFalse(valid(node(properties)));
            }
        }
        assertFalse(valid(node(Map.of(p("shapeKind"), s("star"), p("shapePointRounding"), d("0.75"), p("shapeValleyRounding"), d("0.5")))));
        assertTrue(valid(node(Map.of(p("shapeKind"), s("star"), p("shapePoints"), d("1e100")))));
    }

    @Test
    void textStyleCompoundPaintColorAndPackageConstraintsMatchExistingCompleteProjection() {
        for (String prefix : List.of("", "Background")) {
            String color = prefix.isEmpty() ? "extendedTextStyleColor" : "extendedTextStyleBackgroundColor";
            String paint = prefix.isEmpty() ? "extendedTextStyleForeground" : "extendedTextStyleBackground";
            assertFalse(valid(node(Map.of(p("variant"), s("extended"), p(color), BadgeTestValues.theme(), p(paint), BadgeTestValues.paint()))));
        }
        assertFalse(valid(node(Map.of(p("variant"), s("extended"), p("extendedTextStylePackage"), s("fonts")))));
        var properties = full("extended", "roundedRectangle", false);
        var output = generated(node(properties));
        assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("Theme") && v.modelPath().equals("/root/properties/foregroundColor")));
        assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals("Theme") && v.modelPath().startsWith("/root/properties/extendedTextStyleShadows")));
        assertFalse(output.build().payload().contains("return const FloatingActionButton.extended("));
    }

    @Test
    void capabilityProjectionFailsClosedWhenHeroTypeOrInfinityOrDefaultsOrSlotsDrift() {
        var definition = definition();
        assertEquals(4, BuiltInWidgetCapabilityCatalog.capabilities(definition).size());
        assertEquals(78, BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow().propertyContracts().size());
        for (String name : List.of("heroTag", "elevation", "enabled", "mouseCursor")) {
            var properties = new ArrayList<>(definition.properties());
            var original = definition.property(p(name)).orElseThrow();
            properties.set(properties.indexOf(original), new PropertyDefinition(original.name(), original.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), Optional.empty()));
            var drifted = new WidgetDefinition(definition.typeId(), definition.dartClassName(), definition.namedConstructor(), definition.constConstructor(), definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(), properties, definition.slots());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(drifted).isEmpty());
        }
    }

    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

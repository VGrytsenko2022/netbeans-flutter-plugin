package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.ExpansionTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionTileContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test void complete76RowsFiveSlotsAndRequiredTitlePreserveExactConstructorSemantics() {
        assertEquals(76, definition().properties().size()); assertEquals(28, ExpansionTileWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 290, "ExpansionTile"), definition().palette()); assertTrue(definition().constConstructor());
        assertEquals(List.of("title", "leading", "subtitle", "trailing", "children"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(java.util.stream.IntStream.range(0, 5).boxed().toList(), definition().slots().stream().map(v -> v.parameter().order()).toList());
        assertEquals(java.util.stream.IntStream.range(5, 81).boxed().toList(), definition().properties().stream().map(v -> v.parameter().order()).toList());
        assertTrue(definition().properties().stream().noneMatch(v -> v.parameter().required() || v.creationDefault().isPresent()));
        for (var property : definition().properties()) assertEquals(property.parameter().order(), ExpansionTileWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
        assertEquals(1, definition().slot(new SlotName("title")).orElseThrow().minChildren());
        assertEquals("title", WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).orElseThrow().name().value());
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition()));
        var empty = WidgetNodePrototypeFactory.create(definition(), StableId.random()); assertTrue(empty.properties().isEmpty()); assertFalse(valid(empty)); assertTrue(valid(node(Map.of())));
        assertTrue(generated(node(Map.of())).build().payload().contains("const ExpansionTile("));
    }

    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void pinnedSdkConstructorAndAll43CurvePresetsMatchTheReviewedSurface() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk"));
        String source = Files.readString(sdk.resolve("packages/flutter/lib/src/material/expansion_tile.dart"));
        int start = source.indexOf("const ExpansionTile({"); int end = source.indexOf("}) : assert", start);
        var matcher = Pattern.compile("(?:this|super)\\.(\\w+)").matcher(source.substring(start, end)); var arguments = new LinkedHashSet<String>(); while (matcher.find()) arguments.add(matcher.group(1));
        assertEquals(34, arguments.size());
        var expected = new LinkedHashSet<String>(); expected.add("key"); definition().slots().forEach(v -> expected.add(v.name().value())); definition().properties().stream().limit(28).forEach(v -> expected.add(v.name().value())); assertEquals(expected, arguments);
        String curves = Files.readString(sdk.resolve("packages/flutter/lib/src/animation/curves.dart"));
        var presets = new LinkedHashSet<String>(); var presetMatcher = Pattern.compile("static const \\w+ (\\w+) =").matcher(curves.substring(curves.indexOf("abstract final class Curves")));
        while (presetMatcher.find()) presets.add(presetMatcher.group(1)); assertEquals(new LinkedHashSet<>(ExpansionTileWidgetPropertySchema.curvePresets()), presets); assertEquals(43, presets.size());
        assertTrue(source.contains("expandedCrossAxisAlignment != CrossAxisAlignment.baseline"));
        assertFalse(source.substring(source.indexOf("void _updateAnimationDuration()")).contains(".reverseDuration"));
    }

    @Test void everyPropertyRoundTripsAndBothShapeFamiliesKeepIndependentOriginalPaths() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (String shape : ExpansionTileWidgetPropertySchema.shapeKinds()) {
            var root = fullNode(shape, shape); assertTrue(valid(root)); roundTrip(root); covered.addAll(root.properties().keySet());
            var output = generated(root); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().contains("/properties/shape")));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().contains("/properties/collapsedShape")));
            assertTrue(output.build().payload().contains("children:")); assertTrue(output.build().payload().contains("reverseDuration:"));
            assertFalse(output.build().payload().contains("expansionAnimationStyleDurationUs:"));
        }
        for (String whole : List.of("shape", "collapsedShape", "visualDensity", "expansionAnimationStyle")) { covered.add(p(whole)); var root = node(Map.of(p(whole), reference(whole))); roundTrip(root); assertSymbols(generated(root)); }
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), covered);
    }

    @Test void nullableDirectFieldsAndSparseAnimationNullsKeepOmissionDistinct() throws Exception {
        for (var property : definition().properties().subList(0, 28)) {
            boolean nullable = !ExpansionTileWidgetPropertySchema.nonNullableBooleanProperties().contains(property.name().value());
            assertEquals(nullable, accepts(property.name().value(), nil()), property.name().value());
            if (nullable) { var root = node(Map.of(property.name(), nil())); assertTrue(generated(root).build().payload().contains(property.name().value() + ": null")); roundTrip(root); }
        }
        for (String name : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties()) {
            var root = node(Map.of(p(name), nil())); String output = generated(root).build().payload();
            assertTrue(output.contains("const AnimationStyle(")); assertTrue(output.contains(": null")); roundTrip(root);
        }
        assertFalse(generated(node(Map.of())).build().payload().contains("expansionAnimationStyle:"));
        assertTrue(generated(node(Map.of(p("expansionAnimationStyle"), s("noAnimation")))).build().payload().contains("AnimationStyle.noAnimation"));
    }

    @Test void everyCurveAndSignedMicrosecondBoundaryHasConstExactSdkProvenance() throws Exception {
        for (String name : ExpansionTileWidgetPropertySchema.animationCurveProperties()) for (String curve : ExpansionTileWidgetPropertySchema.curvePresets()) {
            var root = node(Map.of(p(name), s(curve))); var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.build().payload().contains("Curves." + curve));
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.symbolName().equals(curve) && v.modelPath().endsWith("/properties/" + name)));
        }
        for (String name : ExpansionTileWidgetPropertySchema.animationDurationProperties()) for (long value : List.of(-9007199254740991L, -1L, 0L, 1L, 9007199254740991L)) {
            var root = node(Map.of(p(name), i(value))); var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.build().payload().contains("const Duration(microseconds: " + value + ")"));
            var witness = output.symbolOccurrences().stream().filter(v -> v.id().contains(":expansion-tile-core-duration:")).findFirst().orElseThrow();
            assertEquals("widget:" + root.id() + ":expansion-tile-core-duration:" + name, witness.id());
            assertEquals("dart:core", witness.libraryUri()); assertEquals("Duration", witness.symbolName()); assertTrue(witness.staticTypeRequirement().isEmpty()); assertTrue(witness.modelPath().endsWith("/properties/" + name));
            assertFalse(output.imports().payload().contains("import 'dart:core'"));
        }
        for (String name : ExpansionTileWidgetPropertySchema.animationDurationProperties()) { assertFalse(accepts(name, new PropertyValue.IntegerValue(new BigInteger("9007199254740992")))); assertFalse(accepts(name, d("1.0"))); }
    }

    @Test void everyObjectFamilyReferenceMemberAndFactoryRetainsExactStaticTypeProof() {
        var types = Map.ofEntries(Map.entry("shape", "ShapeBorder"), Map.entry("collapsedShape", "ShapeBorder"), Map.entry("tilePadding", "EdgeInsetsGeometry"), Map.entry("childrenPadding", "EdgeInsetsGeometry"),
                Map.entry("expandedAlignment", "AlignmentGeometry"), Map.entry("visualDensity", "VisualDensity"), Map.entry("controller", "ExpansibleController"), Map.entry("statesController", "WidgetStatesController"),
                Map.entry("expansionAnimationStyle", "AnimationStyle"), Map.entry("expansionAnimationStyleDurationUs", "Duration"), Map.entry("expansionAnimationStyleReverseDurationUs", "Duration"),
                Map.entry("expansionAnimationStyleCurve", "Curve"), Map.entry("expansionAnimationStyleReverseCurve", "Curve"), Map.entry("onExpansionChanged", "ValueChanged<bool>"), Map.entry("backgroundColor", "Color"));
        for (var pair : types.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/styles.dart") : Optional.empty(), "values", member ? Optional.of("item") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var output = generated(node(Map.of(p(pair.getKey()), reference))); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().endsWith("/properties/" + pair.getKey() + (member ? "/member" : "/rootSymbol"))
                    && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(pair.getValue())).orElse(false)), pair.toString());
            assertFalse(output.build().payload().contains("const ExpansionTile("));
        }
    }

    @Test void wholeLocalAnimationDensityAndBothShapesRejectConflictsWithoutCoercion() {
        for (PropertyValue whole : List.of(reference("style"), nil(), s("noAnimation"))) for (String local : ExpansionTileWidgetPropertySchema.animationStyleLocalProperties()) {
            var result = new WidgetTreeValidator().validate(document(node(Map.of(p("expansionAnimationStyle"), whole, p(local), value(local)))), CATALOG);
            assertFalse(result.valid()); assertTrue(result.issues().stream().anyMatch(v -> v.path().endsWith("/" + local) || v.path().endsWith("/expansionAnimationStyle")), result.toString());
        }
        for (String family : ExpansionTileWidgetPropertySchema.shapeFamilies()) {
            assertFalse(valid(node(Map.of(p(family), nil(), p(family + "Kind"), s("circle")))));
            assertFalse(valid(node(Map.of(p(family + "Radius"), value(family + "Radius")))));
            assertFalse(valid(node(Map.of(p(family + "Kind"), s("star"), p(family + "PointRounding"), d("0.6"), p(family + "ValleyRounding"), d("0.5")))));
        }
        assertFalse(valid(node(Map.of(p("visualDensity"), nil(), p("visualDensityHorizontal"), d("1")))));
        assertFalse(accepts("expandedCrossAxisAlignment", new PropertyValue.EnumValue("CrossAxisAlignment", "baseline")));
        for (String name : ExpansionTileWidgetPropertySchema.definitions().keySet()) assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        assertFalse(accepts("expansionAnimationStyleCurve", s("Curves.easeIn"))); assertFalse(accepts("expansionAnimationStyle", s("AnimationStyle()")));
    }

    @Test void optionalSlotsAndCallbackSeedMetadataDoNotInventExpansionStateOwnership() throws Exception {
        for (int mask = 0; mask < 16; mask++) {
            var root = node(Map.of(p("showTrailingIcon"), b(false))); var slots = new LinkedHashMap<>(root.slots()); int bit = 0;
            for (String name : List.of("leading", "subtitle", "trailing", "children")) if ((mask & (1 << bit++)) != 0) slots.put(new SlotName(name), name.equals("children")
                    ? new WidgetSlot.ListSlot(List.of(ListTileTestValues.text(name))) : new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
            root = new WidgetNode(root.id(), root.type(), root.properties(), slots); roundTrip(root); assertSymbols(generated(root));
        }
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(1, events.size()); var event = events.getFirst();
        assertEquals("onExpansionChanged", event.propertyName().value()); assertEquals("void _changed(bool isExpanded)", event.signature().declaration("_changed"));
        assertFalse(WidgetStateBindingCatalog.find(node(Map.of())).isPresent());
        assertEquals(Set.of("showTrailingIcon", "maintainState", "dense", "enableFeedback", "enabled", "internalAddSemanticForOnTap"), WidgetStatePropertyBindingCatalog.descriptors(node(Map.of())).stream().map(v -> v.propertyName().value()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(WidgetStatePropertyBindingCatalog.find(node(Map.of()), p("initiallyExpanded")).isEmpty());
        for (PropertyValue callback : List.of(s("noop"), nil(), reference("changed"))) {
            var root = node(Map.of(p("enabled"), b(false), p("onExpansionChanged"), callback)); assertTrue(generated(root).build().payload().contains("onExpansionChanged:")); roundTrip(root);
        }
        assertFalse(accepts("onExpansionChanged", new PropertyValue.CallbackValue("_raw")));
    }

    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var document = document(root); assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}

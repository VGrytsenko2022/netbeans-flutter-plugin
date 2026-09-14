package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import dev.flutter.netbeans.designer.validation.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.TooltipThemeTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class TooltipThemeContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void all47RowsRequiredChildAndFreshEmptyDataMatchCompleteProjection() {
        assertEquals(47, definition().properties().size());
        assertEquals(16, TooltipThemeWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 320, "TooltipTheme"), definition().palette());
        assertTrue(definition().constConstructor());
        assertEquals(List.of("child"), definition().slots().stream().map(v -> v.name().value()).toList());
        var slot = definition().slots().getFirst();
        assertTrue(slot.parameter().required()); assertEquals(1, slot.parameter().order());
        assertEquals(1, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(0, definition().property(p("data")).orElseThrow().parameter().order());
        assertEquals(java.util.stream.IntStream.range(2, 48).boxed().toList(), definition().properties().stream().skip(1).map(v -> v.parameter().order()).toList());
        assertTrue(definition().properties().stream().noneMatch(v -> v.parameter().required() || v.creationDefault().isPresent()));
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertTrue(prototype.properties().isEmpty()); assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition()));
        assertEquals(List.of("C|flutter.material.TooltipTheme|paletteCreate|wrapExistingChild|child"), WidgetPlacementRules.capabilityFingerprintLines(definition()));
        var root = node(Map.of()); var output = generated(root); assertSymbols(output);
        assertTrue(output.build().payload().contains("const TooltipTheme("));
        assertTrue(output.build().payload().contains("data: const TooltipThemeData()"));
        assertFalse(output.build().payload().contains("copyWith")); assertFalse(output.build().payload().contains(".lerp"));
        var symbol = output.symbolOccurrences().stream().filter(v -> v.symbolName().equals("TooltipThemeData")).findFirst().orElseThrow();
        assertEquals("widget:" + root.id() + ":compound:TooltipThemeData:/root/properties/data", symbol.id());
        assertEquals("package:flutter/material.dart", symbol.libraryUri()); assertEquals("/root/properties/data", symbol.modelPath());
        assertTrue(symbol.staticTypeRequirement().isEmpty());
        assertEquals(16, DesignerDocument.SCHEMA_VERSION); assertEquals(15, WidgetCatalog.API_VERSION);
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void pinnedSdkDataConstructorAndNearestReplacementMatchReviewedContract() throws Exception {
        String source = Files.readString(Path.of(System.getProperty("flutter.events.sdk")).resolve("packages/flutter/lib/src/material/tooltip_theme.dart"));
        assertTrue(source.contains("const TooltipTheme({super.key, required this.data, required super.child});"));
        int start = source.indexOf("const TooltipThemeData({"); int end = source.indexOf("}) : assert", start);
        var matcher = Pattern.compile("this\\.(\\w+)").matcher(source.substring(start, end));
        var actual = new LinkedHashSet<String>(); while (matcher.find()) actual.add(matcher.group(1));
        assertEquals(15, actual.size());
        assertEquals(TooltipThemeWidgetPropertySchema.dataFields().stream().map(name -> TooltipThemeWidgetPropertySchema.find(name).orElseThrow().dartName()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)), actual);
        assertTrue(source.contains("height == null || constraints == null"));
        assertTrue(source.contains("return tooltipTheme?.data ?? Theme.of(context).tooltipTheme;"));
    }
    @Test void everyLocalLeafAndWholeBranchesKeepCodecAndSourceProvenance() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (boolean paints : List.of(false, true)) {
            var root = node(full(paints)); covered.addAll(root.properties().keySet()); roundTrip(root);
            var output = generated(root); assertSymbols(output); String source = output.build().payload();
            assertTrue(source.contains("data:")); assertTrue(source.contains("TooltipThemeData("));
            assertTrue(source.contains("textStyle:")); assertTrue(source.contains("BoxDecoration("));
            assertTrue(source.contains("exitDuration: const Duration(microseconds: 123456)"));
            assertFalse(source.contains("textStyleFontSize:")); assertFalse(source.contains("exitDurationUs:"));
        }
        for (String name : List.of("data", "height", "textStyle")) {
            var root = node(Map.of(p(name), value(name))); covered.add(p(name)); roundTrip(root); assertSymbols(generated(root));
        }
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), covered);
        for (String name : TooltipThemeWidgetPropertySchema.localProperties()) {
            assertEquals(TooltipTestValues.definition().property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints(), name);
        }
    }
    @Test void wholeDataRejectsEveryLocalIncludingNullAndStateWithoutDiscardingValues() {
        for (String name : TooltipThemeWidgetPropertySchema.localProperties()) {
            var root = node(Map.of(p("data"), value("data"), p(name), value(name)));
            assertFalse(valid(root), name); assertEquals(2, root.properties().size());
            assertFalse(valid(node(Map.of(p("data"), value("data"), p(name), new PropertyValue.NullValue()))), name);
        }
        for (String name : TooltipThemeWidgetPropertySchema.booleanProperties()) {
            var original = node(Map.of(p("data"), value("data")));
            var bound = bind(original, name, new StatePropertyBinding("_flag", StateBinding.Type.NULLABLE_BOOL,
                    Optional.empty(), StatePropertyBinding.Transform.DIRECT, Optional.empty()));
            var validation = new WidgetTreeValidator().validate(document(bound), CATALOG);
            assertFalse(validation.valid());
            assertTrue(validation.issues().stream().anyMatch(v -> v.code().equals(WidgetTreeValidator.PROPERTY_CONFLICT)
                    && v.path().equals("/root/propertyBindings/" + name)), validation.issues().toString());
        }
    }
    @Test void nullableLocalsPreserveExplicitNullAndExactHeightConstraintAndStyleAssertions() throws Exception {
        for (String name : TooltipThemeWidgetPropertySchema.dataFields()) {
            assertTrue(accepts(name, new PropertyValue.NullValue()), name);
            var root = node(Map.of(p(name), new PropertyValue.NullValue())); roundTrip(root);
            assertTrue(generated(root).build().payload().contains(TooltipThemeWidgetPropertySchema.find(name).orElseThrow().dartName() + ": null"));
        }
        assertFalse(accepts("data", new PropertyValue.NullValue()));
        assertFalse(valid(node(Map.of(p("height"), value("height"), p("constraints"), value("constraints")))));
        assertTrue(valid(node(Map.of(p("height"), new PropertyValue.NullValue(), p("constraints"), value("constraints")))));
        assertTrue(valid(node(Map.of(p("height"), value("height"), p("constraints"), new PropertyValue.NullValue()))));
        for (String name : TooltipThemeWidgetPropertySchema.textStyleProperties()) {
            assertFalse(valid(node(Map.of(p("textStyle"), value("textStyle"), p(name), value(name)))), name);
            assertFalse(valid(node(Map.of(p("textStyle"), new PropertyValue.NullValue(), p(name), value(name)))), name);
        }
        for (String name : List.of("height", "verticalOffset")) for (String special : List.of("infinity", "negativeInfinity", "nan")) {
            assertSymbols(generated(node(Map.of(p(name), new PropertyValue.EnumValue("double", special)))));
        }
    }
    @Test void durationLiteralsUseTheirOwnExactCoreOccurrencesAndPortableSignedBounds() throws Exception {
        for (String name : TooltipThemeWidgetPropertySchema.durationProperties()) for (long micros : List.of(-9007199254740991L, -1L, 0L, 1L, 9007199254740991L)) {
            var root = node(Map.of(p(name), new PropertyValue.IntegerValue(BigInteger.valueOf(micros))));
            var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.build().payload().contains("const Duration(microseconds: " + micros + ")"));
            var witness = output.symbolOccurrences().stream().filter(v -> v.id().contains(":tooltip-theme-core-duration:")).findFirst().orElseThrow();
            assertEquals("widget:" + root.id() + ":tooltip-theme-core-duration:" + name, witness.id());
            assertEquals("dart:core", witness.libraryUri()); assertEquals("Duration", witness.symbolName());
            assertEquals("/root/properties/" + name, witness.modelPath()); assertTrue(witness.staticTypeRequirement().isEmpty());
            assertFalse(output.imports().payload().contains("import 'dart:core'"));
        }
        for (String name : TooltipThemeWidgetPropertySchema.durationProperties()) {
            assertFalse(accepts(name, new PropertyValue.IntegerValue(new BigInteger("9007199254740992"))));
            assertFalse(accepts(name, TooltipTestValues.d("1.5")));
        }
    }
    @Test void everyReferenceFamilyUsesStrictProofsForRootsMembersImportsAndFactories() {
        var expected = Map.of("data", "TooltipThemeData", "constraints", "BoxConstraints", "padding", "EdgeInsetsGeometry",
                "margin", "EdgeInsetsGeometry", "decoration", "Decoration", "textStyle", "TextStyle",
                "waitDurationUs", "Duration", "showDurationUs", "Duration", "exitDurationUs", "Duration");
        for (var pair : expected.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/theme_values.dart") : Optional.empty(), "values",
                    member ? Optional.of("item") : Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            var output = generated(node(Map.of(p(pair.getKey()), ref))); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.staticTypeRequirement().map(t -> t.expectedDartType().equals(pair.getValue())).orElse(false)
                    && v.modelPath().startsWith("/root/properties/" + pair.getKey())), pair.toString());
            assertFalse(output.build().payload().contains("const TooltipTheme("));
        }
        var constant = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/theme_values.dart"), "EmptyThemeData", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(true));
        assertTrue(generated(node(Map.of(p("data"), constant))).build().payload().contains("const TooltipTheme("));
        for (var property : definition().properties()) {
            assertFalse(accepts(property.name().value(), new PropertyValue.DartExpressionValue("arbitrary()")));
            assertFalse(accepts(property.name().value(), new PropertyValue.CallbackValue("_callback")));
        }
    }
    @Test void onlyThreeNullableStateConsumersRenderInsideDataWithOriginalBindingEvidence() {
        assertTrue(WidgetEventCatalog.eventsFor(definition()).isEmpty());
        assertTrue(WidgetStateBindingCatalog.find(node(Map.of())).isEmpty());
        assertEquals(new HashSet<>(TooltipThemeWidgetPropertySchema.booleanProperties()), WidgetStatePropertyBindingCatalog.descriptors(node(Map.of())).stream()
                .map(v -> v.propertyName().value()).collect(java.util.stream.Collectors.toSet()));
        for (String name : TooltipThemeWidgetPropertySchema.booleanProperties()) {
            for (StatePropertyBinding binding : List.of(
                    new StatePropertyBinding("_flag", StateBinding.Type.NULLABLE_BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT, Optional.empty()),
                    new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT, Optional.empty()),
                    new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(new PropertyValue.IntegerValue(BigInteger.ONE))))) {
                var root = bind(node(Map.of(p(name), new PropertyValue.NullValue())), name, binding);
                var output = generated(root); assertSymbols(output); String source = output.build().payload();
                assertTrue(source.contains("data: TooltipThemeData("));
                assertEquals(1, source.split(name + ":", -1).length - 1, source);
                assertTrue(source.indexOf(name + ":") > source.indexOf("TooltipThemeData("), source);
                assertFalse(source.contains("const TooltipTheme("));
                assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().equals("/root/propertyBindings/" + name + "/fieldName/rootSymbol") && v.staticTypeRequirement().isPresent()));
                assertEquals(new PropertyValue.NullValue(), root.properties().get(p(name)));
            }
        }
    }
    private static WidgetNode bind(WidgetNode original, String name, StatePropertyBinding binding) { return new WidgetNode(original.id(), original.type(), original.properties(), original.slots(), original.extensions(), Optional.empty(), Map.of(p(name), binding)); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.modelValidation().issues() + " " + result.diagnostics()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var document = document(root); var encoded = codec.encode(document); var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)); assertEquals(document, decoded.document()); assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}

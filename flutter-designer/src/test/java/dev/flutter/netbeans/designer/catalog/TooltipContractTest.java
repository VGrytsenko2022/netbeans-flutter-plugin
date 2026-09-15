package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.*;
import dev.flutter.netbeans.designer.validation.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.TooltipTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class TooltipContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    @Test void all53RowsOptionalChildAndCreationMessageMatchFullConstructorProjection() {
        assertEquals(53, definition().properties().size()); assertEquals(22, TooltipWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 300, "Tooltip"), definition().palette()); assertTrue(definition().constConstructor());
        assertEquals(List.of("child"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(0, definition().slots().getFirst().parameter().order()); assertEquals(0, definition().slots().getFirst().minChildren());
        assertEquals(java.util.stream.IntStream.range(1, 54).boxed().toList(), definition().properties().stream().map(v -> v.parameter().order()).toList());
        assertEquals(1, definition().properties().stream().filter(v -> v.creationDefault().isPresent()).count());
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random()); assertEquals(Map.of(p("message"), s("Tooltip")), prototype.properties());
        assertTrue(valid(prototype)); assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition()));
        assertTrue(generated(prototype).build().payload().contains("const Tooltip("));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION); assertEquals(16, WidgetCatalog.API_VERSION);
        for (var property : definition().properties()) assertEquals(property.parameter().order(), TooltipWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void pinnedSdkAll24ConstructorArgsAndActualTypedefsMatchReviewedMetadata() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk"));
        String source = Files.readString(sdk.resolve("packages/flutter/lib/src/material/tooltip.dart"));
        int start = source.indexOf("const Tooltip({"); int end = source.indexOf("}) : assert", start);
        var matcher = Pattern.compile("(?:this|super)\\.(\\w+)").matcher(source.substring(start, end)); var actual = new LinkedHashSet<String>(); while (matcher.find()) actual.add(matcher.group(1));
        var expected = new LinkedHashSet<String>(List.of("key", "child")); definition().properties().stream().limit(22).forEach(v -> expected.add(TooltipWidgetPropertySchema.find(v.name()).orElseThrow().dartName()));
        assertEquals(24, actual.size()); assertEquals(expected, actual);
        assertTrue(source.contains("(message == null) != (richMessage == null)")); assertTrue(source.contains("height == null || constraints == null"));
        String raw = Files.readString(sdk.resolve("packages/flutter/lib/src/widgets/raw_tooltip.dart"));
        assertTrue(raw.contains("typedef TooltipTriggeredCallback = VoidCallback;"));
        assertTrue(raw.contains("typedef TooltipPositionDelegate = Offset Function(TooltipPositionContext context);"));
    }
    @Test void everyLeafAndComplementaryRichPaintStyleBranchRoundTripsWithOriginalSourcePaths() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (boolean rich : List.of(false, true)) for (boolean paints : List.of(false, true)) {
            var root = node(full(rich, paints)); covered.addAll(root.properties().keySet()); assertTrue(valid(root)); roundTrip(root); var output = generated(root); assertSymbols(output);
            String source = output.build().payload(); assertTrue(source.contains("textStyle:")); assertTrue(source.contains("BoxDecoration(")); assertTrue(source.contains("Duration(microseconds: 123456)"));
            assertFalse(source.contains("textStyleFontSize:")); assertFalse(source.contains("waitDurationUs:"));
            assertEquals(rich, source.contains("richMessage:")); assertTrue(source.contains("WidgetStateMouseCursor.clickable"));
        }
        for (String name : List.of("height", "textStyle")) { var root = with(Map.of(p(name), value(name))); covered.add(p(name)); roundTrip(root); assertSymbols(generated(root)); }
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), covered);
        for (String name : TooltipWidgetPropertySchema.textStyleProperties()) {
            assertEquals(BuiltInWidgetCatalog.getDefault().find(BadgeWidgetPropertySchema.BADGE_TYPE).orElseThrow().property(p(name)).orElseThrow().constraints(), definition().property(p(name)).orElseThrow().constraints());
        }
    }
    @Test void contentAndSizingUseExactNonNullAssertionsNotPresenceOrTruthiness() throws Exception {
        for (String message : List.of("", "Tooltip", "quoted ' \\ \n Unicode українська")) { var root = with(Map.of(p("message"), s(message), p("richMessage"), nil())); assertTrue(valid(root)); roundTrip(root); }
        assertTrue(valid(node(Map.of(p("richMessage"), reference("span")))));
        assertTrue(valid(node(Map.of(p("message"), nil(), p("richMessage"), reference("span")))));
        for (var values : List.of(Map.<PropertyName, PropertyValue>of(), Map.<PropertyName, PropertyValue>of(p("message"), nil()), Map.<PropertyName, PropertyValue>of(p("message"), nil(), p("richMessage"), nil()), Map.<PropertyName, PropertyValue>of(p("message"), s(""), p("richMessage"), reference("span")))) assertFalse(valid(node(values)), values.toString());
        assertFalse(valid(with(Map.of(p("height"), d("0"), p("constraints"), value("constraints")))));
        assertTrue(valid(with(Map.of(p("height"), nil(), p("constraints"), value("constraints")))));
        assertTrue(valid(with(Map.of(p("height"), d("-1"), p("constraints"), nil()))));
        assertFalse(valid(with(Map.of(p("height"), i(1), p("constraints"), reference("limits")))));
        for (String name : List.of("height", "verticalOffset")) for (String special : List.of("infinity", "negativeInfinity", "nan")) assertSymbols(generated(with(Map.of(p(name), new PropertyValue.EnumValue("double", special)))));
    }
    @Test void everyNullableDirectArgumentAndNonNullableDismissFlagPreserveOmission() throws Exception {
        for (var property : definition().properties().subList(0, 22)) {
            String name = property.name().value(); assertEquals(!name.equals("enableTapToDismiss"), accepts(name, nil()), name);
            if (name.equals("enableTapToDismiss") || name.equals("message")) continue;
            var root = with(Map.of(property.name(), nil())); roundTrip(root); assertTrue(generated(root).build().payload().contains(TooltipWidgetPropertySchema.find(name).orElseThrow().dartName() + ": null"), name);
        }
        String omitted = generated(with(Map.of())).build().payload(); assertFalse(omitted.contains("showDuration:")); assertFalse(omitted.contains("onTriggered:")); assertFalse(omitted.contains("positionDelegate:"));
        for (String name : TooltipWidgetPropertySchema.textStyleProperties()) for (PropertyValue whole : List.of(nil(), reference("style"))) assertFalse(valid(with(Map.of(p("textStyle"), whole, p(name), value(name)))));
        for (var property : definition().properties()) assertFalse(accepts(property.name().value(), new PropertyValue.DartExpressionValue("arbitrary()")), property.name().value());
    }
    @Test void signedDurationMicrosecondsRemainConstWithExactCoreNavigationAndPortableBounds() throws Exception {
        for (String name : TooltipWidgetPropertySchema.durationProperties()) for (long micros : List.of(-9007199254740991L, -1L, 0L, 1L, 9007199254740991L)) {
            var root = with(Map.of(p(name), i(micros))); var output = generated(root); roundTrip(root); assertSymbols(output);
            assertTrue(output.build().payload().contains("const Duration(microseconds: " + micros + ")"));
            var witness = output.symbolOccurrences().stream().filter(v -> v.id().contains(":tooltip-core-duration:")).findFirst().orElseThrow();
            assertEquals("widget:" + root.id() + ":tooltip-core-duration:" + name, witness.id()); assertEquals("dart:core", witness.libraryUri()); assertEquals("Duration", witness.symbolName()); assertTrue(witness.staticTypeRequirement().isEmpty()); assertTrue(witness.modelPath().endsWith("/properties/" + name));
            assertFalse(output.imports().payload().contains("import 'dart:core'"));
        }
        for (String name : TooltipWidgetPropertySchema.durationProperties()) { assertFalse(accepts(name, d("1.5"))); assertFalse(accepts(name, new PropertyValue.IntegerValue(new BigInteger("9007199254740992")))); }
    }
    @Test void allReferenceFamiliesAnd41CursorsProduceStrictProofsWithoutConstFactories() {
        var types = Map.ofEntries(Map.entry("richMessage", "InlineSpan"), Map.entry("constraints", "BoxConstraints"), Map.entry("padding", "EdgeInsetsGeometry"), Map.entry("margin", "EdgeInsetsGeometry"), Map.entry("decoration", "Decoration"), Map.entry("textStyle", "TextStyle"), Map.entry("waitDurationUs", "Duration"), Map.entry("showDurationUs", "Duration"), Map.entry("exitDurationUs", "Duration"), Map.entry("onTriggered", "TooltipTriggeredCallback"), Map.entry("positionDelegate", "TooltipPositionDelegate"), Map.entry("mouseCursor", "MouseCursor"));
        for (var pair : types.entrySet()) for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var ref = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/tooltip_values.dart") : Optional.empty(), "values", member ? Optional.of("item") : Optional.empty(), factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
            var root = pair.getKey().equals("richMessage") ? node(Map.of(p("richMessage"), ref)) : with(Map.of(p(pair.getKey()), ref));
            var output = generated(root); assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.staticTypeRequirement().map(t -> t.expectedDartType().equals(pair.getValue())).orElse(false) && v.modelPath().startsWith("/root/properties/" + pair.getKey())), pair.toString());
            assertFalse(output.build().payload().contains("const Tooltip("));
        }
        for (String preset : DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets()) { var output = generated(with(Map.of(p("mouseCursor"), s(preset)))); assertSymbols(output); assertTrue(output.build().payload().contains("." + preset)); }
    }
    @Test void singleVoidEventAndNonEventDelegateDoNotInventControlledState() {
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(2, events.size());
        var event = WidgetEventCatalog.find(TooltipWidgetPropertySchema.TOOLTIP_TYPE, p("onTriggered")).orElseThrow(); assertEquals(WidgetEventDescriptor.Kind.EVENT, event.kind()); assertTrue(event.defaultEvent()); assertEquals("void _triggered()", event.signature().declaration("_triggered"));
        var delegate = WidgetEventCatalog.find(TooltipWidgetPropertySchema.TOOLTIP_TYPE, p("positionDelegate")).orElseThrow(); assertEquals(WidgetEventDescriptor.Kind.DELEGATE, delegate.kind()); assertEquals("Offset Function(TooltipPositionContext)", delegate.signature().dartFunctionType()); assertFalse(delegate.defaultEvent()); assertEquals(List.of("package:flutter/widgets.dart"), delegate.signature().importUris());
        assertTrue(WidgetStateBindingCatalog.find(with(Map.of())).isEmpty());
        assertEquals(new HashSet<>(TooltipWidgetPropertySchema.booleanProperties()), WidgetStatePropertyBindingCatalog.descriptors(with(Map.of())).stream().map(v -> v.propertyName().value()).collect(java.util.stream.Collectors.toSet()));
        assertFalse(accepts("onTriggered", new PropertyValue.CallbackValue("_raw"))); assertFalse(accepts("positionDelegate", s("noop")));
        assertTrue(generated(with(Map.of(p("onTriggered"), s("noop")))).build().payload().contains("onTriggered: () {}"));
    }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
    private static void roundTrip(WidgetNode root) throws Exception { var codec = new FdDocumentCodec(); var document = document(root); var encoded = codec.encode(document); var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded)); assertEquals(document, decoded.document()); assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes()); }
    private static void assertSymbols(GeneratedDartRegions output) { for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString()); assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count()); }
}

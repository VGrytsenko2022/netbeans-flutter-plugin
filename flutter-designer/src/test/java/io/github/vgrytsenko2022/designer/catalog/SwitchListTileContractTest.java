package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.SwitchListTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SwitchListTileContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete236PropertiesThreeSlotsAndThreeCreationDefaultsPreserveWireContracts() {
        assertEquals(236, definition().properties().size());
        assertEquals(42, SwitchListTileWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 270, "SwitchListTile"), definition().palette());
        assertTrue(definition().constConstructor());
        assertEquals(List.of("title", "subtitle", "secondary"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(List.of(0, 1, 2), definition().slots().stream().map(v -> v.parameter().order()).toList());
        assertTrue(definition().slots().stream().allMatch(v -> v.minChildren() == 0 && v.maxChildren() == 1));
        assertEquals(java.util.stream.IntStream.range(3, 239).boxed().toList(),
                definition().properties().stream().map(v -> v.parameter().order()).toList());
        for (var property : definition().properties()) {
            assertEquals(property.parameter().order(), SwitchListTileWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "onChanged", "variant").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(p("value"), b(false), p("onChanged"), s("noop"), p("variant"), s("standard")), prototype.properties());
        assertTrue(valid(prototype));
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        for (String absent : List.of("key", "enabled", "tristate", "padding", "trackOutlineWidth", "focusColor", "titleAlignment")) {
            assertTrue(definition().property(p(absent)).isEmpty(), absent);
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void pinnedSdkAdaptiveSurfaceExactlyMatchesFortyOneLeavesAndThreeSlots() throws Exception {
        Path sdk = Path.of(System.getProperty("flutter.events.sdk"));
        String source = Files.readString(sdk.resolve("packages/flutter/lib/src/material/switch_list_tile.dart"));
        String constructor = source.substring(source.indexOf("const SwitchListTile.adaptive({"));
        constructor = constructor.substring(0, constructor.indexOf("})"));
        var matcher = Pattern.compile("(?:this|super)\\.([A-Za-z][A-Za-z0-9_]*)").matcher(constructor);
        var sdkFields = new LinkedHashSet<String>();
        while (matcher.find()) sdkFields.add(matcher.group(1));
        assertEquals(45, sdkFields.size(), sdkFields.toString());
        var modelFields = new HashSet<String>();
        definition().properties().stream().filter(v -> v.parameter().order() < 44).forEach(v -> modelFields.add(v.name().value()));
        definition().slots().forEach(v -> modelFields.add(v.name().value()));
        modelFields.add("key");
        assertEquals(sdkFields, modelFields);
    }

    @Test
    void complementaryWholeAndLocalFamiliesCoverEveryFieldAndEveryShapeWithExactSymbolsAndCodec() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (String variant : SwitchListTileWidgetPropertySchema.variants()) {
            for (String shape : SwitchListTileWidgetPropertySchema.shapeKinds()) {
                var root = fullNode(variant, shape);
                covered.addAll(root.properties().keySet());
                var output = generated(root);
                assertSymbols(output);
                assertEquals(variant.equals("adaptive"), output.build().payload().contains("SwitchListTile.adaptive("));
                assertEquals(variant.equals("adaptive"), output.build().payload().contains("applyCupertinoTheme:"));
                for (String leaked : List.of("variant:", "shapeKind:", "visualDensityHorizontal:", "thumbColorDefault:", "thumbIconDefaultMode:", "mouseCursorDefault:")) {
                    assertFalse(output.build().payload().contains(leaked), leaked);
                }
                roundTrip(root);
            }
        }
        var refs = new LinkedHashMap<PropertyName, PropertyValue>();
        for (String name : List.of("thumbColor", "trackColor", "trackOutlineColor", "overlayColor", "thumbIcon", "shape", "visualDensity", "mouseCursor")) {
            refs.put(p(name), reference(name));
        }
        covered.addAll(refs.keySet());
        assertSymbols(generated(node(refs)));
        roundTrip(node(refs));
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), covered);
    }

    @Test
    void adaptiveOnlyNullableFlagIsRetainedInStandardButCannotLeakToSourceOrProofs() throws Exception {
        for (PropertyValue flag : List.of(b(true), b(false), nil())) {
            var standard = node(Map.of(p("applyCupertinoTheme"), flag, p("onChanged"), nil()));
            assertTrue(valid(standard));
            var standardOutput = generated(standard);
            assertTrue(standardOutput.build().payload().contains("const SwitchListTile("));
            assertFalse(standardOutput.build().payload().contains("applyCupertinoTheme:"));
            assertTrue(standardOutput.symbolOccurrences().stream().noneMatch(v -> v.modelPath().endsWith("/applyCupertinoTheme")));
            roundTrip(standard);
            var adaptive = node(Map.of(p("variant"), s("adaptive"), p("applyCupertinoTheme"), flag, p("onChanged"), nil()));
            assertTrue(generated(adaptive).build().payload().contains("const SwitchListTile.adaptive("));
            assertTrue(generated(adaptive).build().payload().contains("applyCupertinoTheme:"));
        }
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
        assertFalse(generated(node(Map.of())).build().payload().contains("const SwitchListTile("));
    }

    @Test
    void allEightOptionalSlotCombinationsAndThreeLineInvariantPreserveChildIdentity() throws Exception {
        for (int mask = 0; mask < 8; mask++) {
            var slots = new LinkedHashMap<SlotName, WidgetSlot>();
            int bit = 0;
            for (String name : List.of("title", "subtitle", "secondary")) {
                if ((mask & (1 << bit++)) != 0) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
            }
            var root = node(Map.of(p("onChanged"), nil()), slots);
            var output = generated(root);
            for (String name : List.of("title", "subtitle", "secondary")) assertEquals(slots.containsKey(new SlotName(name)), output.build().payload().contains("      " + name + ":"));
            roundTrip(root);
            var three = node(Map.of(p("isThreeLine"), b(true)), slots);
            assertEquals(slots.containsKey(new SlotName("subtitle")), valid(three));
        }
        assertTrue(valid(node(Map.of(p("isThreeLine"), nil()))));
        assertTrue(valid(node(Map.of(p("isThreeLine"), b(false)))));
    }

    @Test
    void imageErrorsDistinguishOmissionExplicitNullNoopAndReferenceAcrossProviders() throws Exception {
        for (String state : List.of("Active", "Inactive")) {
            String callback = "on" + state + "ThumbImageError";
            String image = Character.toLowerCase(state.charAt(0)) + state.substring(1) + "ThumbImage";
            assertTrue(valid(node(Map.of(p(callback), nil()))));
            for (PropertyValue value : List.of(s("noop"), reference(callback))) {
                assertFalse(valid(node(Map.of(p(callback), value))));
                for (var kind : PropertyValue.ImageProviderValue.ProviderKind.values()) {
                    var provider = new PropertyValue.ImageProviderValue(kind, "assets/icon.png", Optional.of("icons"),
                            kind == PropertyValue.ImageProviderValue.ProviderKind.EXACT_ASSET ? Optional.of(new java.math.BigDecimal("2.5")) : Optional.empty(),
                            Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(Optional.of(16), Optional.of(24),
                                    PropertyValue.ImageProviderValue.ResizePolicy.values()[0], false)));
                    var root = node(Map.of(p(callback), value, p(image), provider));
                    assertSymbols(generated(root)); roundTrip(root);
                }
            }
            assertFalse(accepts(image, nil()));
            assertFalse(accepts(image, reference("image")));
        }
    }

    @Test
    void allFourEventsUseExactSignaturesAndControlledBoolNeverBecomesTriState() {
        var events = WidgetEventCatalog.eventsFor(definition());
        assertEquals(4, events.size());
        assertTrue(events.stream().allMatch(v -> v.kind() == WidgetEventDescriptor.Kind.EVENT));
        var changed = WidgetEventCatalog.find(definition().typeId(), p("onChanged")).orElseThrow();
        assertEquals("void Function(bool)", changed.signature().dartFunctionType());
        assertTrue(changed.required()); assertTrue(changed.sdkRequired()); assertTrue(changed.defaultEvent()); assertTrue(changed.allowsExplicitNull());
        assertEquals("void Function(bool)", WidgetEventCatalog.find(definition().typeId(), p("onFocusChange")).orElseThrow().signature().dartFunctionType());
        for (String name : List.of("onActiveThumbImageError", "onInactiveThumbImageError")) {
            var descriptor = WidgetEventCatalog.find(definition().typeId(), p(name)).orElseThrow();
            assertEquals("ImageErrorListener", descriptor.callbackType());
            assertTrue(descriptor.allowsExplicitNull());
        }
        var state = WidgetStateBindingCatalog.find(node(Map.of())).orElseThrow();
        assertEquals("bool", state.dartType()); assertEquals("bool", state.callbackParameterType());
        assertEquals(List.of(p("value")), state.previewProperties());
        assertFalse(accepts("value", nil()));
        assertFalse(accepts("onChanged", new PropertyValue.CallbackValue("_legacy")));
    }

    @Test
    void compoundWholeLocalConflictsAndMissingCursorDefaultFailClosed() {
        for (String family : SwitchListTileWidgetPropertySchema.colorFamilies()) {
            assertFalse(valid(node(Map.of(p(family), reference(family), p(family + "Default"), SwitchTestValues.color()))));
            var output = generated(node(Map.of(p(family + "Default"), SwitchTestValues.color(), p(family + "Selected"), nil())));
            String source = output.build().payload();
            assertTrue(source.indexOf("WidgetState.selected: null") < source.indexOf("WidgetState.any:"));
        }
        for (var pair : Map.of("thumbIcon", "thumbIconDefaultMode", "shape", "shapeKind", "visualDensity", "visualDensityHorizontal", "mouseCursor", "mouseCursorDefault").entrySet()) {
            assertFalse(valid(node(Map.of(p(pair.getKey()), reference(pair.getKey()), p(pair.getValue()), value(pair.getValue())))), pair.toString());
        }
        assertFalse(valid(node(Map.of(p("mouseCursorHovered"), s("click")))));
        assertTrue(valid(node(Map.of(p("mouseCursorDefault"), s("defer"), p("mouseCursorHovered"), s("click")))));
        assertFalse(valid(node(Map.of(p("thumbIconDefaultMode"), s("inherit"), p("thumbIconDefaultSize"), d("24")))));
    }

    @Test
    void typedReferenceProofsCoverColorsObjectsCallbacksFactoriesAndAllCursorPresets() throws Exception {
        var expected = Map.ofEntries(Map.entry("activeColor", "Color"), Map.entry("activeThumbColor", "Color"),
                Map.entry("tileColor", "Color"), Map.entry("selectedTileColor", "Color"), Map.entry("hoverColor", "Color"),
                Map.entry("focusNode", "FocusNode"), Map.entry("statesController", "WidgetStatesController"),
                Map.entry("shape", "ShapeBorder"), Map.entry("visualDensity", "VisualDensity"), Map.entry("contentPadding", "EdgeInsetsGeometry"),
                Map.entry("mouseCursor", "MouseCursor"), Map.entry("thumbColor", "WidgetStateProperty<Color?>"),
                Map.entry("thumbIcon", "WidgetStateProperty<Icon?>"), Map.entry("onChanged", "ValueChanged<bool>"), Map.entry("onFocusChange", "ValueChanged<bool>"));
        for (var entry : expected.entrySet()) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/values.dart"), "values", Optional.of(entry.getKey()),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            var root = node(Map.of(p(entry.getKey()), reference));
            var output = generated(root);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().endsWith("/properties/" + entry.getKey() + "/member")
                    && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(entry.getValue())).orElse(false)), entry + ": " + output.symbolOccurrences());
            assertSymbols(output); roundTrip(root);
        }
        assertEquals(41, SwitchListTileWidgetPropertySchema.mouseCursorPresets().size());
        for (String cursor : SwitchListTileWidgetPropertySchema.mouseCursorPresets()) {
            assertSymbols(generated(node(Map.of(p("mouseCursor"), s(cursor)))));
            assertSymbols(generated(node(Map.of(p("mouseCursorDefault"), s(cursor)))));
        }
    }

    @Test
    void safeConsumerAllowlistAndClosedNumericDomainsDoNotInventStateOrRawDart() {
        var consumers = WidgetStatePropertyBindingCatalog.descriptors(node(Map.of()));
        assertEquals(Set.of("dense", "autofocus", "enableFeedback", "selected", "internalAddSemanticForOnTap", "applyCupertinoTheme"),
                consumers.stream().map(v -> v.propertyName().value()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(consumers.stream().allMatch(v -> v.allowedTransforms().equals(Set.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT, StatePropertyBinding.Transform.EQUALS))));
        for (PropertyValue value : List.of(d("-2.5"), nil(), new PropertyValue.EnumValue("double", "infinity"),
                new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.EnumValue("double", "nan"))) assertTrue(accepts("splashRadius", value));
        assertFalse(accepts("visualDensityHorizontal", d("5")));
        for (String name : SwitchListTileWidgetPropertySchema.definitions().keySet()) {
            assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        }
    }

    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode node) { return new WidgetTreeValidator().validate(document(node), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec(); var doc = document(root);
        assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

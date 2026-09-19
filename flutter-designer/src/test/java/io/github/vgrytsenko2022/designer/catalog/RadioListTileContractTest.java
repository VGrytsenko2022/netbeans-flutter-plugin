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
import static io.github.vgrytsenko2022.designer.catalog.RadioListTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioListTileContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete153RowsThreeOptionalSlotsAndFourCreationDefaultsKeepExistingWireContracts() {
        assertEquals(153, definition().properties().size());
        assertEquals(40, RadioListTileWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 280, "RadioListTile"), definition().palette());
        assertTrue(definition().constConstructor());
        assertEquals(List.of("title", "subtitle", "secondary"), definition().slots().stream().map(v -> v.name().value()).toList());
        assertEquals(List.of(0, 1, 2), definition().slots().stream().map(v -> v.parameter().order()).toList());
        assertTrue(definition().slots().stream().allMatch(v -> v.minChildren() == 0 && v.maxChildren() == 1));
        assertEquals(java.util.stream.IntStream.range(3, 156).boxed().toList(), definition().properties().stream().map(v -> v.parameter().order()).toList());
        for (var property : definition().properties()) {
            assertEquals(property.parameter().order(), RadioListTileWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "valueType", "variant").contains(property.name().value()), property.parameter().required());
            assertEquals(Set.of("value", "valueType", "variant", "onChanged").contains(property.name().value()), property.creationDefault().isPresent());
        }
        assertEquals(Map.of(p("value"), s("option"), p("valueType"), s("String"), p("variant"), s("standard"), p("onChanged"), s("noop")), node(Map.of()).properties());
        assertTrue(valid(node(Map.of())));
        for (String name : List.of("key", "groupRegistry", "focusColor", "backgroundColor", "side", "innerRadius", "tristate")) assertTrue(definition().property(p(name)).isEmpty(), name);
        assertEquals(17, DesignerDocument.SCHEMA_VERSION); assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
    void bothPinnedSdkConstructorArgumentSetsExactlyMatchDirectAndSlotMetadata() throws Exception {
        String source = Files.readString(Path.of(System.getProperty("flutter.events.sdk")).resolve("packages/flutter/lib/src/material/radio_list_tile.dart"));
        for (String variant : RadioListTileWidgetPropertySchema.variants()) {
            String ctor = source.substring(source.indexOf("const RadioListTile" + (variant.equals("adaptive") ? ".adaptive" : "") + "({"));
            ctor = ctor.substring(0, ctor.indexOf("})"));
            var matcher = Pattern.compile("(?:this|super)\\.([A-Za-z][A-Za-z0-9_]*)").matcher(ctor);
            var sdkFields = new LinkedHashSet<String>(); while (matcher.find()) sdkFields.add(matcher.group(1));
            assertEquals(variant.equals("adaptive") ? 41 : 40, sdkFields.size());
            var modelFields = new HashSet<String>();
            definition().properties().stream().filter(v -> v.parameter().order() < 40)
                    .filter(v -> RadioListTileWidgetPropertySchema.propertyAvailableInVariant(v.name().value(), variant))
                    .forEach(v -> modelFields.add(v.name().value()));
            definition().slots().forEach(v -> modelFields.add(v.name().value())); modelFields.add("key");
            assertEquals(sdkFields, modelFields);
        }
    }

    @Test
    void all153FieldsTenShapesAndBothConstructorsRoundTripWithoutRenamingProvenance() throws Exception {
        var covered = new HashSet<PropertyName>();
        for (String variant : RadioListTileWidgetPropertySchema.variants()) for (String shape : RadioListTileWidgetPropertySchema.shapeKinds()) {
            var root = fullNode(variant, shape); covered.addAll(root.properties().keySet());
            var output = generated(root); assertSymbols(output); roundTrip(root);
            assertTrue(output.build().payload().contains("RadioListTile<String?>" + (variant.equals("adaptive") ? ".adaptive" : "") + "("));
            assertEquals(variant.equals("adaptive"), output.build().payload().contains("useCupertinoCheckmarkStyle:"));
            for (String leaked : List.of("variant:", "valueType:", "nullableValueType:", "radioSideStateful:", "radioInnerRadiusDefault:", "radioBackgroundColorDefault:", "mouseCursorDefault:", "shapeKind:")) assertFalse(output.build().payload().contains(leaked), leaked);
            assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().contains("/properties/radioSide")));
            assertTrue(output.symbolOccurrences().stream().noneMatch(v -> v.modelPath().contains("/properties/side")));
        }
        for (String family : List.of("fillColor", "overlayColor", "radioBackgroundColor", "radioInnerRadius", "radioSide", "visualDensity", "shape", "mouseCursor")) {
            covered.add(p(family)); var root = node(Map.of(p(family), reference(family))); assertSymbols(generated(root)); roundTrip(root);
        }
        assertEquals(definition().properties().stream().map(PropertyDefinition::name).collect(java.util.stream.Collectors.toSet()), covered);
    }

    @Test
    void genericLiteralDomainsNullableTAndEveryProjectTypeReferenceKeepSelectedIdentity() throws Exception {
        var samples = Map.of("String", s("x"), "int", i(1), "double", d("1.5"), "num", i(2), "bool", b(true), "Object", s("x"));
        for (var sample : samples.entrySet()) for (boolean nullable : List.of(false, true)) {
            var root = node(Map.of(p("valueType"), s(sample.getKey()), p("value"), sample.getValue(), p("groupValue"), sample.getValue(),
                    p("nullableValueType"), b(nullable), p("onChanged"), nil()));
            assertTrue(generated(root).build().payload().contains("const RadioListTile<" + sample.getKey() + (nullable ? "?" : "") + ">"));
            roundTrip(root);
        }
        assertFalse(valid(node(Map.of(p("valueType"), s("bool")))));
        assertFalse(valid(node(Map.of(p("value"), nil()))));
        assertTrue(valid(node(Map.of(p("value"), nil(), p("nullableValueType"), b(true)))));
        assertTrue(valid(node(Map.of(p("groupValue"), nil()))));
        assertFalse(valid(node(Map.of(p("valueType"), s("int"), p("value"), d("1.0")))));
        for (boolean imported : List.of(false, true)) for (boolean nullable : List.of(false, true)) {
            var type = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:app/types.dart") : Optional.empty(),
                    "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var root = node(Map.of(p("valueType"), type, p("value"), reference("choice"), p("groupValue"), reference("group"),
                    p("onChanged"), reference("changed"), p("nullableValueType"), b(nullable)));
            var output = generated(root); var requirements = output.symbolOccurrences().stream().flatMap(v -> v.staticTypeRequirement().stream()).toList();
            assertEquals(4, requirements.size(), requirements.toString());
            assertEquals(Set.of("Type", "Object", "Object?", "ValueChanged<Object?>"), requirements.stream().map(GeneratedDartStaticTypeRequirement::expectedDartType).collect(java.util.stream.Collectors.toSet()));
            assertTrue(requirements.stream().allMatch(v -> v.sourceTypeOverride().orElseThrow().endsWith("Choice" + (nullable ? "?" : ""))));
            assertSymbols(output); roundTrip(root);
        }
        for (var type : List.of(reference("Choice"), new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.of("nested"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "makeChoice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            // Imported bare references are valid; only members and invocations are rejected.
            if (type.member().isPresent() || type.access() == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION) assertFalse(valid(node(Map.of(p("valueType"), type))));
        }
    }

    @Test
    void optionalCallbacksAndAdaptiveFlagKeepOmissionNullNoopAndDisabledReferencesDistinct() throws Exception {
        assertTrue(generated(without(node(Map.of()), "onChanged")).build().payload().contains("const RadioListTile<String>("));
        assertFalse(generated(without(node(Map.of()), "onChanged")).build().payload().contains("onChanged:"));
        for (String variant : RadioListTileWidgetPropertySchema.variants()) {
            var root = node(Map.of(p("variant"), s(variant), p("onChanged"), nil(), p("useCupertinoCheckmarkStyle"), b(true)));
            assertTrue(valid(root)); roundTrip(root);
            assertTrue(generated(root).build().payload().contains("onChanged: null"));
            assertEquals(variant.equals("adaptive"), generated(root).build().payload().contains("useCupertinoCheckmarkStyle: true"));
            for (PropertyValue enabled : List.of(b(true), b(false), nil())) {
                var disabled = node(Map.of(p("variant"), s(variant), p("enabled"), enabled, p("onChanged"), reference("changed"), p("onFocusChange"), reference("focused")));
                var output = generated(disabled); assertSymbols(output);
                // The shared fixture references buttonValues.<handler>: navigation is
                // recorded for the root, while the full-expression proof belongs to the member.
                assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().endsWith("/properties/onChanged/member")
                        && v.staticTypeRequirement().map(t -> t.expectedDartType().equals("ValueChanged<Object?>")
                                && t.sourceTypeOverride().equals(Optional.of("String"))).orElse(false)), output.symbolOccurrences().toString());
                assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().endsWith("/properties/onFocusChange/member")
                        && v.staticTypeRequirement().map(t -> t.expectedDartType().equals("ValueChanged<bool>")
                                && t.sourceTypeOverride().isEmpty()).orElse(false)), output.symbolOccurrences().toString());
            }
        }
        assertFalse(accepts("useCupertinoCheckmarkStyle", nil()));
        assertFalse(accepts("onChanged", new PropertyValue.CallbackValue("_raw")));
    }

    @Test
    void allOptionalSlotCombinationsEnforceOnlyExplicitThreeLineSubtitleDependency() throws Exception {
        for (int mask = 0; mask < 8; mask++) {
            var slots = new LinkedHashMap<SlotName, WidgetSlot>(); int bit = 0;
            for (String name : List.of("title", "subtitle", "secondary")) if ((mask & (1 << bit++)) != 0) slots.put(new SlotName(name), new WidgetSlot.SingleSlot(Optional.of(ListTileTestValues.text(name))));
            var root = node(Map.of(p("onChanged"), nil()), slots);
            var output = generated(root); roundTrip(root);
            for (String name : List.of("title", "subtitle", "secondary")) assertEquals(slots.containsKey(new SlotName(name)), output.build().payload().contains("      " + name + ":"));
            assertEquals(slots.containsKey(new SlotName("subtitle")), valid(node(Map.of(p("isThreeLine"), b(true)), slots)));
        }
        assertTrue(valid(node(Map.of(p("isThreeLine"), nil())))); assertTrue(valid(node(Map.of(p("isThreeLine"), b(false)))));
    }

    @Test
    void everyCompoundConflictAndStateSidePrerequisiteRejectsTheOriginalPropertyPath() {
        for (String family : RadioListTileWidgetPropertySchema.colorFamilies()) {
            assertFalse(valid(node(Map.of(p(family), reference(family), p(family + "Default"), RadioTestValues.color()))));
            String source = generated(node(Map.of(p(family + "Default"), RadioTestValues.color(), p(family + "Selected"), nil()))).build().payload();
            assertTrue(source.indexOf("WidgetState.selected: null") < source.indexOf("WidgetState.any:"));
        }
        for (var pair : Map.of("radioInnerRadius", "radioInnerRadiusDefault", "radioSide", "radioSideStateful", "shape", "shapeKind", "visualDensity", "visualDensityHorizontal", "mouseCursor", "mouseCursorDefault").entrySet()) assertFalse(valid(node(Map.of(p(pair.getKey()), reference(pair.getKey()), p(pair.getValue()), value(pair.getValue())))));
        for (String state : RadioListTileWidgetPropertySchema.sideStates()) {
            String mode = "radioSide" + state + "Mode";
            var invalid = node(Map.of(p(mode), s("border")));
            var validation = new WidgetTreeValidator().validate(document(invalid), CATALOG);
            assertFalse(validation.valid()); assertTrue(validation.issues().toString().contains(mode));
            assertFalse(valid(node(Map.of(p("radioSideStateful"), b(true), p(mode), s("inherit"), p("radioSide" + state + "Width"), d("2")))));
            assertTrue(valid(node(Map.of(p("radioSideStateful"), b(true), p(mode), s("inherit")))));
        }
        assertFalse(valid(node(Map.of(p("mouseCursorHovered"), s("click")))));
        assertTrue(valid(node(Map.of(p("mouseCursorDefault"), s("defer"), p("mouseCursorHovered"), s("click")))));
    }

    @Test
    void typedWholeRefsAndFactoriesProveTheExactPrefixedSdkTypes() {
        for (var pair : Map.ofEntries(Map.entry("activeColor", "Color"), Map.entry("tileColor", "Color"), Map.entry("shape", "ShapeBorder"),
                Map.entry("visualDensity", "VisualDensity"), Map.entry("contentPadding", "EdgeInsetsGeometry"), Map.entry("focusNode", "FocusNode"),
                Map.entry("statesController", "WidgetStatesController"), Map.entry("radioBackgroundColor", "WidgetStateProperty<Color?>"),
                Map.entry("radioSide", "BorderSide"), Map.entry("radioInnerRadius", "WidgetStateProperty<double?>"), Map.entry("mouseCursor", "MouseCursor")).entrySet()) {
            for (boolean factory : List.of(false, true)) {
                var value = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/values.dart"), "values", Optional.of(pair.getKey()),
                        factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory ? Optional.of(false) : Optional.empty());
                var output = generated(node(Map.of(p(pair.getKey()), value)));
                assertTrue(output.symbolOccurrences().stream().anyMatch(v -> v.modelPath().endsWith("/properties/" + pair.getKey() + "/member") && v.staticTypeRequirement().map(t -> t.expectedDartType().equals(pair.getValue())).orElse(false)), pair.toString());
                assertSymbols(output);
            }
        }
        for (String cursor : RadioListTileWidgetPropertySchema.mouseCursorPresets()) assertSymbols(generated(node(Map.of(p("mouseCursorDefault"), s(cursor)))));
    }

    @Test
    void exactEventsAndLegacyGenericProducerDoNotInventBoolStateOrRegistry() {
        var events = WidgetEventCatalog.eventsFor(definition()); assertEquals(2, events.size());
        var changed = events.stream().filter(v -> v.propertyName().equals(p("onChanged"))).findFirst().orElseThrow();
        assertEquals("void Function(Object?)", changed.signature().dartFunctionType());
        assertFalse(changed.required()); assertFalse(changed.sdkRequired()); assertTrue(changed.defaultEvent()); assertTrue(changed.allowsExplicitNull());
        assertEquals("void Function(bool)", WidgetEventCatalog.find(definition().typeId(), p("onFocusChange")).orElseThrow().signature().dartFunctionType());
        for (String type : RadioListTileWidgetPropertySchema.valueTypes()) {
            var state = WidgetStateBindingCatalog.find(node(Map.of(p("valueType"), s(type)))).orElseThrow();
            assertEquals(type + "?", state.dartType()); assertEquals(type + "?", state.callbackParameterType());
            assertEquals("groupValue", state.runtimeArgumentName()); assertEquals(List.of(p("groupValue")), state.previewProperties());
        }
        var consumers = WidgetStatePropertyBindingCatalog.descriptors(node(Map.of()));
        assertEquals(Set.of("toggleable", "dense", "selected", "autofocus", "enableFeedback", "enabled", "internalAddSemanticForOnTap", "useCupertinoCheckmarkStyle"),
                consumers.stream().map(v -> v.propertyName().value()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(consumers.stream().allMatch(v -> v.allowedTransforms().equals(Set.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT, StatePropertyBinding.Transform.EQUALS))));
    }

    @Test
    void numericDomainsRetainSignedValuesWithoutOpeningRawCodeOrGenericGrammar() {
        for (String field : List.of("radioScaleFactor", "splashRadius")) for (PropertyValue value : List.of(d("-2"), d("0"),
                new PropertyValue.EnumValue("double", "infinity"), new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.EnumValue("double", "nan"))) assertTrue(accepts(field, value), field);
        assertTrue(accepts("splashRadius", nil())); assertFalse(accepts("radioScaleFactor", nil()));
        assertTrue(accepts("radioInnerRadiusDefault", nil())); assertFalse(accepts("radioInnerRadiusDefault", new PropertyValue.EnumValue("double", "nan")));
        assertFalse(accepts("visualDensityHorizontal", d("5")));
        for (String name : RadioListTileWidgetPropertySchema.definitions().keySet()) assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        for (String type : List.of("Choice?", "List<String>", "prefix.Choice", "dynamic", "void")) assertFalse(accepts("valueType", s(type)));
    }

    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static boolean valid(WidgetNode node) { return new WidgetTreeValidator().validate(document(node), CATALOG).valid(); }
    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow();
    }
    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec(); var doc = document(root); assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
    }
    private static void assertSymbols(GeneratedDartRegions output) {
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

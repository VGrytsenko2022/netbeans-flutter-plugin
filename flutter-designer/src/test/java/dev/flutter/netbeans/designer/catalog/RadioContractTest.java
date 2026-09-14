package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.RadioTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void all107FieldsAndActualOptionalEnabledHaveOnlyReviewedCreationDefaults() {
        assertEquals(107, definition().properties().size());
        assertEquals(26, RadioWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 230, "Radio"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().slots().isEmpty());
        assertTrue(definition().traits().isEmpty());
        for (int index = 0; index < 107; index++) {
            var property = definition().properties().get(index);
            assertEquals(index, property.parameter().order());
            assertEquals(index, RadioWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "valueType", "variant").contains(property.name().value()), property.parameter().required());
        }
        assertEquals(Map.of(p("value"), s("option"), p("valueType"), s("String"), p("variant"), s("standard"), p("onChanged"), s("noop")), node(Map.of()).properties());
        assertTrue(valid(node(Map.of())));
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, WidgetDefinition.MAX_PROPERTIES);
    }

    @Test
    void independentCapabilityMatchesEveryFieldAndFailsClosedOnChangedContracts() throws Exception {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        String all = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        int start = all.indexOf("W|flutter.material.Radio\n");
        int end = all.indexOf("W|", start + 2);
        String contract = all.substring(start, end < 0 ? all.length() : end);
        assertEquals(108, contract.lines().count());
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/radio-contract.txt"), contract);
        for (String name : List.of("valueType", "value", "groupValue", "onChanged", "groupRegistry", "innerRadiusDefault", "visualDensity")) {
            var properties = definition().properties().stream().map(property -> property.name().value().equals(name)
                    ? new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)),
                            property.creationDefault().filter(value -> value instanceof PropertyValue.StringValue)) : property).toList();
            var changed = new WidgetDefinition(definition().typeId(), definition().dartClassName(), definition().namedConstructor(),
                    true, definition().dartLibraryUri(), definition().importUris(), definition().traits(), definition().palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty(), name);
        }
    }

    @Test
    void denseBothConstructorsAndWholeFamiliesCoverEveryRowWithExactRoundTrips() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : RadioWidgetPropertySchema.variants()) {
            var full = full(variant);
            assertEquals(variant.equals("adaptive") ? 101 : 100, full.size());
            covered.addAll(full.keySet());
            WidgetNode root = node(full);
            assertTrue(valid(root));
            var output = generated(root);
            assertTrue(output.build().payload().contains("Radio<String?>" + (variant.equals("adaptive") ? ".adaptive" : "") + "("));
            assertFalse(output.build().payload().contains("valueType:"));
            assertSymbols(output);
            roundTrip(root);
        }
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius", "side", "visualDensity")) {
            covered.add(p(family));
            var root = node(Map.of(p(family), reference(family)));
            assertSymbols(generated(root));
            roundTrip(root);
        }
        assertEquals(107, covered.size());
    }

    @Test
    void actualEnabledDoesNotSuppressCallbacksAndOmitNullNoopRemainDistinct() throws Exception {
        WidgetNode empty = without(node(Map.of()), "onChanged");
        assertTrue(generated(empty).build().payload().contains("const Radio<String>("));
        assertFalse(generated(empty).build().payload().contains("onChanged:"));
        assertFalse(generated(empty).build().payload().contains("enabled:"));
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
        for (String variant : RadioWidgetPropertySchema.variants()) {
            for (PropertyValue enabled : List.of(b(false), b(true), nil())) {
                var root = node(Map.of(p("variant"), s(variant), p("enabled"), enabled, p("onChanged"), reference("callback")));
                var output = generated(root);
                assertTrue(output.build().payload().contains("callback"));
                assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.modelPath().endsWith("/onChanged/rootSymbol")));
                roundTrip(root);
            }
            var output = generated(node(Map.of(p("variant"), s(variant), p("onChanged"), nil(), p("groupRegistry"), nil())));
            assertTrue(output.build().payload().contains("const Radio<String>"));
            assertTrue(output.build().payload().contains("onChanged: null"));
            assertTrue(output.build().payload().contains("groupRegistry: null"));
        }
    }

    @Test
    void builtinLiteralDomainsAndAtomicTypeChangesNeverCoerceOrDiscardValues() {
        for (var type : Map.of("String", s("x"), "int", i(1), "double", d("1.5"), "num", i(2), "bool", b(true), "Object", s("x")).entrySet()) {
            assertTrue(valid(node(Map.of(p("valueType"), s(type.getKey()), p("value"), type.getValue()))), type.toString());
        }
        assertTrue(valid(node(Map.of(p("valueType"), s("double"), p("value"), i(1)))));
        assertFalse(valid(node(Map.of(p("valueType"), s("int"), p("value"), d("1.0")))));
        assertFalse(valid(node(Map.of(p("value"), nil()))));
        assertTrue(valid(node(Map.of(p("value"), nil(), p("nullableValueType"), b(true)))));
        assertTrue(valid(node(Map.of(p("groupValue"), nil()))));
        var invalid = node(Map.of(p("valueType"), s("bool")));
        assertTrue(RadioWidgetPropertySchema.valueTypeError(invalid).orElseThrow().contains("not coerced"));
        assertEquals(s("option"), invalid.properties().get(p("value")));
        for (String field : List.of("value", "groupValue")) {
            assertFalse(accepts(field, d("1e400")));
            assertFalse(accepts(field, i(9007199254740992L)));
        }
    }

    @Test
    void numericIdentitySpecialsAreClosedAndNeverAddedToGeometryDomains() throws Exception {
        for (String member : List.of("infinity", "negativeInfinity", "nan")) {
            for (String type : List.of("double", "num", "Object")) {
                var value = new PropertyValue.EnumValue("double", member);
                var root = node(Map.of(p("valueType"), s(type), p("value"), value, p("groupValue"), value, p("onChanged"), nil()));
                var output = generated(root);
                assertTrue(output.build().payload().contains(member.equals("nan") ? "(0.0 / 0.0)" : member.equals("infinity") ? "(1.0 / 0.0)" : "(-1.0 / 0.0)"));
                assertFalse(output.imports().payload().contains("dart:core"));
                assertSymbols(output);
                roundTrip(root);
            }
            for (String type : List.of("String", "int", "bool")) assertFalse(valid(node(Map.of(p("valueType"), s(type), p("value"), new PropertyValue.EnumValue("double", member)))));
        }
        assertFalse(accepts("splashRadius", new PropertyValue.EnumValue("double", "nan")));
        assertFalse(accepts("innerRadiusDefault", new PropertyValue.EnumValue("double", "nan")));
        assertFalse(accepts("value", new PropertyValue.EnumValue("double", "maxFinite")));
    }

    @Test
    void genericTypeReferencesAreClosedAndEveryTypedReferenceCarriesSelectedIdentity() throws Exception {
        for (boolean imported : List.of(false, true)) {
            var type = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:radio_values/types.dart") : Optional.empty(),
                    "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var root = node(Map.of(p("valueType"), type, p("value"), reference("choice"), p("groupValue"), reference("group"),
                    p("onChanged"), reference("changed"), p("groupRegistry"), reference("registry"), p("nullableValueType"), b(true)));
            var output = generated(root);
            var typed = output.symbolOccurrences().stream().filter(symbol -> symbol.staticTypeRequirement().isPresent()).toList();
            assertEquals(Set.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>"),
                    new HashSet<>(typed.stream().map(symbol -> symbol.staticTypeRequirement().orElseThrow().expectedDartType()).toList()));
            assertEquals(1, typed.stream().map(symbol -> symbol.staticTypeRequirement().orElseThrow().sourceTypeOverride().orElseThrow()).distinct().count());
            assertTrue(typed.getFirst().staticTypeRequirement().orElseThrow().sourceTypeOverride().orElseThrow().endsWith("Choice?"));
            assertFalse(output.build().payload().contains("??"));
            assertSymbols(output);
            roundTrip(root);
        }
        for (var invalid : List.of(new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.of("first"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false)))) {
            assertFalse(valid(node(Map.of(p("valueType"), invalid))));
        }
    }

    @Test
    void everyLocalStateFamilyHasPresenceAwarePriorityAndWholeLocalExclusivity() {
        for (String family : List.of("fillColor", "overlayColor", "backgroundColor", "innerRadius")) {
            for (String state : RadioWidgetPropertySchema.statePrefixes()) {
                var fields = new LinkedHashMap<PropertyName, PropertyValue>();
                fields.put(p(family + "Default"), family.equals("innerRadius") ? d("4.5") : color());
                fields.put(p(family + state), nil());
                String source = generated(node(fields)).build().payload();
                String member = state.equals("Default") ? "any" : Character.toLowerCase(state.charAt(0)) + state.substring(1);
                assertTrue(source.contains("WidgetState." + member + ": null"));
                fields.put(p(family), reference("whole"));
                assertFalse(valid(node(fields)));
            }
        }
        assertFalse(generated(node(Map.of(p("fillColorDefault"), color()))).build().payload().contains("WidgetState.disabled"));
        assertTrue(generated(node(Map.of(p("fillColorDefault"), theme()))).build().payload().contains("Theme.of(context)"));
        for (String name : RadioWidgetPropertySchema.innerRadiusStateProperties()) {
            for (String member : List.of("infinity", "negativeInfinity")) assertTrue(valid(node(Map.of(p(name), new PropertyValue.EnumValue("double", member)))));
        }
    }

    @Test
    void sidePlainStatefulAndInheritanceRetainExactDependencies() {
        assertTrue(generated(node(Map.of(p("sideWidth"), d("2")))).build().payload().contains("side: const BorderSide(width: 2.0)"));
        assertFalse(valid(node(Map.of(p("sidePressedWidth"), d("2")))));
        assertFalse(valid(node(Map.of(p("sideStateful"), b(true), p("sidePressedMode"), s("inherit"), p("sidePressedWidth"), d("2")))));
        var stateful = node(Map.of(p("sideStateful"), b(true), p("sidePressedMode"), s("inherit"), p("sideWidth"), d("2")));
        assertTrue(generated(stateful).build().payload().contains("WidgetStateBorderSide.fromMap"));
        assertTrue(generated(stateful).build().payload().contains("WidgetState.pressed: null"));
        assertFalse(valid(node(Map.of(p("side"), reference("side"), p("sideStateful"), b(false)))));
    }

    @Test
    void completeCursorsDensityAndConstructorOnlyFlagPreserveSource() {
        for (String cursor : RadioWidgetPropertySchema.mouseCursorPresets()) assertSymbols(generated(node(Map.of(p("mouseCursor"), s(cursor)))));
        assertFalse(accepts("mouseCursor", s("unreviewed")));
        for (String axis : List.of("visualDensityHorizontal", "visualDensityVertical")) {
            assertTrue(generated(node(Map.of(p(axis), d("-4")))).build().payload().contains("const VisualDensity("));
            assertFalse(accepts(axis, d("-4.1")));
            assertFalse(valid(node(Map.of(p(axis), d("0"), p("visualDensity"), reference("density")))));
        }
        assertFalse(valid(node(Map.of(p("useCupertinoCheckmarkStyle"), b(false)))));
        assertTrue(valid(node(Map.of(p("variant"), s("adaptive"), p("useCupertinoCheckmarkStyle"), b(false)))));
    }

    @Test
    void legacyProofConstructorsShiftWithoutLosingClosedOverrideAndRejectEscapeHatches() {
        var legacy = new GeneratedDartStaticTypeRequirement(2, 3, "FocusNode");
        assertTrue(legacy.sourceTypeOverride().isEmpty());
        var selected = new GeneratedDartStaticTypeRequirement(2, 3, "Object?", Optional.of("types.Choice?"));
        assertEquals(Optional.of("types.Choice?"), selected.shifted(7).sourceTypeOverride());
        assertEquals(9, selected.shifted(7).expressionOffset());
        for (String bad : List.of("Choice??", "Choice;exit()", "types.nested.Choice", "List<Choice>")) {
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(2, 3, "Object", Optional.of(bad)));
        }
        for (String name : RadioWidgetPropertySchema.definitions().keySet()) assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")));
        for (String name : List.of("child", "shape", "semanticLabel", "tristate", "isError")) assertFalse(valid(node(Map.of(p(name), b(true)))));
    }

    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(constraint -> constraint.accepts(value)); }
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
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.SliderTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SliderContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete33RowsHaveOnlyRequiredCreationDefaultsAndNoSlotsOrNewFormats() {
        assertEquals(33, definition().properties().size());
        assertEquals(33, SliderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(24, SliderWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals("Constructor", SliderWidgetPropertySchema.find("variant").orElseThrow().displayName());
        assertEquals(new PaletteMetadata("flutter.material", 100, 210, "Slider"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().slots().isEmpty());
        assertTrue(definition().traits().isEmpty());
        for (int index = 0; index < 33; index++) {
            var property = definition().properties().get(index);
            assertEquals(index, property.parameter().order());
            assertEquals(index, SliderWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "variant", "enabled").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(p("value"), i(0), p("variant"), s("standard"), p("enabled"), b(true)), prototype.properties());
        assertTrue(valid(prototype));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).isEmpty());
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, ValidationLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void independentCapabilityProofRejectsChangedNumericPaddingReferenceAndStateDomains() {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        for (String name : List.of("value", "secondaryTrackValue", "divisions", "padding", "onChanged", "overlayColorDefault", "year2023")) {
            var properties = definition().properties().stream().map(property -> property.name().value().equals(name)
                    ? new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), Optional.empty())
                    : property).toList();
            var changed = new WidgetDefinition(definition().typeId(), definition().dartClassName(), definition().namedConstructor(),
                    true, definition().dartLibraryUri(), definition().importUris(), definition().traits(), definition().palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty(), name);
        }
        assertTrue(BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract().contains("W|flutter.material.Slider\n"));
    }

    @Test
    void bothConstructorsEmitConstDefaultAndAll33FieldsRoundTripAcrossLocalAndWholeOverlay() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : SliderWidgetPropertySchema.variants()) {
            String constructor = "Slider" + (variant.equals("adaptive") ? ".adaptive" : "");
            var base = generated(node(Map.of(p("variant"), s(variant), p("enabled"), b(false))));
            assertTrue(base.build().payload().contains("const " + constructor + "("));
            assertFalse(base.build().payload().contains("min:"));
            assertFalse(base.build().payload().contains("max:"));
            assertFalse(base.build().payload().contains("variant:"));
            assertFalse(base.build().payload().contains("enabled:"));
            var dense = full(variant);
            covered.addAll(dense.keySet());
            var root = node(dense);
            assertTrue(valid(root));
            var output = generated(root);
            assertSymbols(output);
            assertEquals(!variant.equals("adaptive"), output.build().payload().contains("padding:"));
            assertFalse(output.build().payload().contains("overlayColorDefault:"));
            roundTrip(root);
            covered.add(p("overlayColor"));
            assertSymbols(generated(node(Map.of(p("variant"), s(variant), p("overlayColor"), reference("overlayColor")))));
            roundTrip(node(Map.of(p("variant"), s(variant), p("overlayColor"), reference("overlayColor"))));
        }
        assertEquals(33, covered.size());
    }

    @Test
    void rangeValidationUsesSdkDefaultsEffectiveDoubleComparisonAndNeverClamps() {
        assertFalse(valid(node(Map.of(p("value"), d("1.01")))));
        assertFalse(valid(node(Map.of(p("min"), d("1")))));
        assertFalse(valid(node(Map.of(p("max"), d("-1")))));
        assertFalse(valid(node(Map.of(p("secondaryTrackValue"), d("-1")))));
        assertTrue(valid(node(Map.of(p("value"), d("0.8"), p("secondaryTrackValue"), d("0.2")))));
        assertTrue(valid(node(Map.of(p("min"), d("1"), p("max"), d("1"), p("value"), i(1)))));
        var rounded = node(Map.of(p("min"), d("0.100000000000000005"), p("value"), d("0.1")));
        assertTrue(SliderWidgetPropertySchema.rangeError(rounded).isEmpty());
        assertFalse(valid(rounded)); // The unchanged global shortest-literal round-trip domain rejects this decimal.
        var invalid = node(Map.of(p("min"), d("20"), p("max"), d("30"), p("value"), d("19")));
        assertTrue(SliderWidgetPropertySchema.rangeError(invalid).orElseThrow().contains("not automatically clamped"));
        assertEquals(d("19"), invalid.properties().get(p("value")));
        var overflow = node(Map.of(p("min"), d("-1.7e308"), p("max"), d("1.7e308"), p("value"), d("0")));
        assertTrue(valid(overflow));
        assertTrue(generated(overflow).build().payload().contains("1.7"));
    }

    @Test
    void signedInfinitiesRemainClosedConstAndImportFreeForAllRangeFields() throws Exception {
        for (String variant : SliderWidgetPropertySchema.variants()) {
            for (boolean negative : List.of(false, true)) {
                var infinite = infinity(negative);
                var root = node(Map.of(p("variant"), s(variant), p("enabled"), b(false),
                        p("min"), infinite, p("max"), infinite, p("value"), infinite, p("secondaryTrackValue"), infinite));
                assertTrue(valid(root));
                var output = generated(root);
                assertEquals(4, output.build().payload().split(java.util.regex.Pattern.quote(negative ? "(-1.0 / 0.0)" : "(1.0 / 0.0)"), -1).length - 1);
                assertFalse(output.imports().payload().contains("dart:core"));
                assertTrue(output.symbolOccurrences().stream().noneMatch(symbol -> symbol.symbolName().equals("double")));
                assertSymbols(output);
                roundTrip(root);
            }
        }
        assertFalse(valid(node(Map.of(p("min"), infinity(false), p("max"), infinity(true)))));
        assertTrue(valid(node(Map.of(p("max"), infinity(false)))));
        assertTrue(valid(node(Map.of(p("min"), infinity(true)))));
        for (String name : SliderWidgetPropertySchema.rangeProperties()) {
            for (String member : List.of("nan", "maxFinite", "infinity()")) {
                if (member.endsWith("()")) continue;
                assertFalse(accepts(name, new PropertyValue.EnumValue("double", member)));
            }
            assertFalse(accepts(name, d("1e400")));
        }
    }

    @Test
    void nullableNumbersEnumsAndPaddingPreserveAllOmitNullAndBoundaryBranches() throws Exception {
        for (String variant : SliderWidgetPropertySchema.variants()) {
            var root = node(Map.of(p("variant"), s(variant), p("enabled"), b(false), p("secondaryTrackValue"), nil(), p("divisions"), nil(), p("year2023"), nil()));
            var source = generated(root).build().payload();
            assertTrue(source.contains("secondaryTrackValue: null"));
            assertTrue(source.contains("divisions: null"));
            assertTrue(source.contains("year2023: null"));
            roundTrip(root);
            for (long count : List.of(1L, 9_007_199_254_740_991L)) {
                assertTrue(valid(node(Map.of(p("variant"), s(variant), p("divisions"), i(count)))));
            }
        }
        assertFalse(accepts("divisions", i(0)));
        assertFalse(accepts("divisions", i(-1)));
        assertFalse(accepts("divisions", i(9_007_199_254_740_992L)));
        assertFalse(accepts("divisions", d("1")));
        assertFalse(accepts("value", nil()));
        assertFalse(accepts("min", nil()));
        assertFalse(accepts("max", nil()));
        for (String name : List.of("allowedInteraction", "showValueIndicator")) {
            var values = (PropertyValueConstraint.EnumValues) definition().property(p(name)).orElseThrow().constraints().getFirst();
            assertEquals(name.equals("allowedInteraction") ? 4 : 6, values.values().size());
            for (String member : values.values()) assertSymbols(generated(node(Map.of(p(name), new PropertyValue.EnumValue(values.dartType().name(), member)))));
        }
        assertTrue(valid(node(Map.of(p("padding"), value("padding")))));
        assertFalse(valid(node(Map.of(p("variant"), s("adaptive"), p("padding"), value("padding")))));
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate())));
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE.negate(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    @Test
    void overlayPriorityHonorsExplicitNullAndWholeReferenceExclusivityWithoutDisabledInjection() {
        for (String state : SliderWidgetPropertySchema.statePrefixes()) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(p("enabled"), b(false));
            values.put(p("overlayColorDefault"), color());
            values.put(p("overlayColor" + state), nil());
            var source = generated(node(values)).build().payload();
            String stateName = state.equals("Default") ? "any" : Character.toLowerCase(state.charAt(0)) + state.substring(1);
            assertTrue(source.contains("WidgetState." + stateName + ": null"));
            values.put(p("overlayColor"), reference("colors"));
            assertFalse(valid(node(values)));
        }
        assertFalse(generated(node(Map.of(p("overlayColorDefault"), color()))).build().payload().contains("WidgetState.disabled"));
        assertFalse(generated(node(Map.of(p("enabled"), b(false), p("overlayColorDefault"), theme()))).build().payload().contains("const Slider("));
    }

    @Test
    void disabledSuppressesOnlyChangedButRetainsStartEndAndFormatterTypedEvidence() {
        var root = node(Map.of(p("enabled"), b(false), p("onChanged"), reference("inactiveChanged"),
                p("onChangeStart"), reference("start"), p("onChangeEnd"), reference("end"),
                p("semanticFormatterCallback"), reference("formatter")));
        var output = generated(root);
        assertTrue(output.build().payload().contains("onChanged: null"));
        assertFalse(output.build().payload().contains("inactiveChanged"));
        assertEquals(List.of("ValueChanged<double>", "ValueChanged<double>", "SemanticFormatterCallback"),
                output.symbolOccurrences().stream().flatMap(symbol -> symbol.staticTypeRequirement().stream())
                        .map(GeneratedDartStaticTypeRequirement::expectedDartType).toList());
        assertSymbols(output);
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
    }

    @Test
    void allSevenReferenceFieldsSupportMembersImportsAndFactoriesWithExactEvidence() throws Exception {
        var expected = Map.of("onChanged", "ValueChanged<double>", "onChangeStart", "ValueChanged<double>",
                "onChangeEnd", "ValueChanged<double>", "semanticFormatterCallback", "SemanticFormatterCallback",
                "overlayColor", "WidgetStateProperty<Color?>", "mouseCursor", "MouseCursor", "focusNode", "FocusNode");
        for (String name : expected.keySet()) {
            for (boolean imported : List.of(false, true)) {
                for (boolean member : List.of(false, true)) {
                    for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
                        var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:slider_values/values.dart") : Optional.empty(),
                                member ? "Slider" : "reviewedValue", member ? Optional.of("value") : Optional.empty(), access,
                                access == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION ? Optional.of(false) : Optional.empty());
                        var root = node(Map.of(p("variant"), s("adaptive"), p(name), reference));
                        var output = generated(root);
                        assertSymbols(output);
                        assertEquals(List.of(expected.get(name)), output.symbolOccurrences().stream()
                                .flatMap(symbol -> symbol.staticTypeRequirement().stream()).map(GeneratedDartStaticTypeRequirement::expectedDartType).toList());
                        roundTrip(root);
                    }
                }
            }
        }
    }

    @Test
    void allCursorPresetsUnsupportedFieldsAndRawExpressionRejectionRemainClosed() {
        for (String cursor : SliderWidgetPropertySchema.mouseCursorPresets()) assertSymbols(generated(node(Map.of(p("mouseCursor"), s(cursor)))));
        for (String name : List.of("key", "child", "thumbShape", "trackHeight", "onFocusChange", "isEnabled", "semanticLabel")) {
            assertFalse(valid(node(Map.of(p(name), b(true)))));
        }
        for (String name : SliderWidgetPropertySchema.definitions().keySet()) {
            assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        }
    }

    private static boolean accepts(String name, PropertyValue value) {
        return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(constraint -> constraint.accepts(value));
    }

    private static boolean valid(WidgetNode root) {
        return new WidgetTreeValidator().validate(document(root), CATALOG).valid();
    }

    private static GeneratedDartRegions generated(WidgetNode root) {
        var result = new DartRegionGenerator().generate(document(root), CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }

    private static void roundTrip(WidgetNode root) throws Exception {
        var codec = new FdDocumentCodec();
        assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
    }

    private static void assertSymbols(GeneratedDartRegions output) {
        for (var symbol : output.symbolOccurrences()) {
            assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        }
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

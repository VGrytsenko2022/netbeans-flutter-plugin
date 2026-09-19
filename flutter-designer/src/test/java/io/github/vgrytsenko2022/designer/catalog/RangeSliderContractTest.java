package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.RangeSliderTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class RangeSliderContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void complete37RowsHaveOnlyThreeRequiredDefaultsAndNonConstLeafConstructor() {
        assertEquals(37, definition().properties().size());
        assertEquals(37, RangeSliderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(19, RangeSliderWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals(new PaletteMetadata("flutter.material", 100, 220, "RangeSlider"), definition().palette());
        assertFalse(definition().constConstructor());
        assertTrue(definition().slots().isEmpty());
        assertTrue(definition().traits().isEmpty());
        for (int index = 0; index < 37; index++) {
            var property = definition().properties().get(index);
            assertEquals(index, property.parameter().order());
            assertEquals(index, RangeSliderWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("valuesStart", "valuesEnd", "enabled").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(p("valuesStart"), i(0), p("valuesEnd"), i(1), p("enabled"), b(true)), prototype.properties());
        assertTrue(valid(prototype));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).isEmpty());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, WidgetDefinition.MAX_PROPERTIES);
    }

    @Test
    void denseAndWholeFamiliesCoverEveryRowWithoutManufacturingConstRangeSlider() throws Exception {
        var local = full();
        assertEquals(34, local.size());
        var output = generated(node(local));
        assertTrue(output.build().payload().contains("const RangeValues(2.0, 3.0)"));
        assertTrue(output.build().payload().contains("const RangeLabels("));
        assertFalse(output.build().payload().contains("const RangeSlider("));
        assertSymbols(output);
        roundTrip(node(local));
        var covered = new HashSet<>(local.keySet());
        for (String family : List.of("labels", "overlayColor", "mouseCursor")) {
            covered.add(p(family));
            var whole = node(Map.of(p(family), reference(family), p("enabled"), b(false)));
            assertSymbols(generated(whole));
            roundTrip(whole);
        }
        assertEquals(37, covered.size());
        assertFalse(generated(node(Map.of(p("enabled"), b(false)))).build().payload().contains("const RangeSlider("));
    }

    @Test
    void rangesUseSdkEffectiveDoubleComparisonsWithoutClampsOrFiniteSpanBounds() {
        assertTrue(valid(node(Map.of())));
        assertFalse(valid(node(Map.of(p("valuesStart"), d("0.9"), p("valuesEnd"), d("0.1")))));
        assertFalse(valid(node(Map.of(p("min"), i(1)))));
        assertFalse(valid(node(Map.of(p("max"), i(0)))));
        assertTrue(valid(node(Map.of(p("min"), i(2), p("max"), i(2), p("valuesStart"), i(2), p("valuesEnd"), i(2)))));
        var overflow = node(Map.of(p("min"), d("-1.7e308"), p("max"), d("1.7e308"), p("valuesStart"), i(0), p("valuesEnd"), d("1e307")));
        assertTrue(valid(overflow));
        var invalid = node(Map.of(p("valuesStart"), i(2)));
        assertTrue(RangeSliderWidgetPropertySchema.rangeError(invalid).orElseThrow().contains("not automatically clamped"));
        assertEquals(i(2), invalid.properties().get(p("valuesStart")));
        var rounded = node(Map.of(p("min"), d("0.100000000000000005"), p("valuesStart"), d("0.1")));
        assertTrue(RangeSliderWidgetPropertySchema.rangeError(rounded).isEmpty());
        assertFalse(valid(rounded)); // Preserve shared exact shortest-literal representability.
    }

    @Test
    void signedInfinitiesUseFixedImportFreeConstantsForEveryRangeField() throws Exception {
        for (boolean negative : List.of(false, true)) {
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String name : RangeSliderWidgetPropertySchema.rangeProperties()) fields.put(p(name), infinity(negative));
            var root = node(fields);
            assertTrue(valid(root));
            var output = generated(root);
            assertEquals(4, output.build().payload().split(java.util.regex.Pattern.quote(negative ? "(-1.0 / 0.0)" : "(1.0 / 0.0)"), -1).length - 1);
            assertFalse(output.imports().payload().contains("dart:core"));
            assertTrue(output.symbolOccurrences().stream().noneMatch(symbol -> symbol.symbolName().equals("double")));
            assertSymbols(output);
            roundTrip(root);
        }
        assertTrue(valid(node(Map.of(p("min"), infinity(true), p("max"), infinity(false)))));
        assertFalse(valid(node(Map.of(p("min"), infinity(false), p("max"), infinity(true)))));
        for (String name : RangeSliderWidgetPropertySchema.rangeProperties()) {
            assertFalse(accepts(name, new PropertyValue.EnumValue("double", "maxFinite")));
            assertFalse(accepts(name, new PropertyValue.EnumValue("double", "nan")));
            assertFalse(accepts(name, nil()));
            assertFalse(accepts(name, d("1e400")));
        }
    }

    @Test
    void labelsDistinguishOmissionNullEmptyPairEachMissingPeerAndProjectWhole() throws Exception {
        assertFalse(generated(node(Map.of())).build().payload().contains("labels:"));
        assertTrue(generated(node(Map.of(p("labels"), nil()))).build().payload().contains("labels: null"));
        for (String name : RangeSliderWidgetPropertySchema.labelProperties()) {
            var empty = node(Map.of(p(name), s("")));
            assertTrue(generated(empty).build().payload().contains("labels: const RangeLabels('', '')"));
            roundTrip(empty);
            var text = node(Map.of(p(name), s("a\r\nb\t' $")));
            assertSymbols(generated(text));
            roundTrip(text);
            assertFalse(valid(node(Map.of(p("labels"), nil(), p(name), s("")))));
            assertFalse(valid(node(Map.of(p("labels"), reference("whole"), p(name), s("")))));
        }
    }

    @Test
    void nullableDivisionYearAndDirectionalPaddingKeepExactDomains() throws Exception {
        for (String name : List.of("labels", "divisions", "year2023")) {
            var root = node(Map.of(p(name), nil()));
            assertTrue(valid(root));
            assertTrue(generated(root).build().payload().contains(name + ": null"));
            roundTrip(root);
        }
        for (long count : List.of(1L, 9007199254740991L)) assertTrue(valid(node(Map.of(p("divisions"), i(count)))));
        for (var bad : List.of(i(0), i(-1), i(9007199254740992L), d("2"), infinity(false))) assertFalse(accepts("divisions", bad));
        assertTrue(valid(node(Map.of(p("padding"), value("padding")))));
        for (int index = 0; index < 4; index++) {
            BigDecimal[] sides = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
            sides[index] = BigDecimal.ONE.negate();
            assertFalse(accepts("padding", new PropertyValue.EdgeInsetsValue(sides[0], sides[1], sides[2], sides[3])));
            assertFalse(accepts("padding", new PropertyValue.EdgeInsetsDirectionalValue(sides[0], sides[1], sides[2], sides[3])));
        }
    }

    @Test
    void bothStateFamiliesRetainFirstPresentNullWithoutImplicitDisabledEntry() {
        for (String family : List.of("overlayColor", "mouseCursor")) {
            for (String state : RangeSliderWidgetPropertySchema.statePrefixes()) {
                var fields = new LinkedHashMap<PropertyName, PropertyValue>();
                fields.put(p(family + "Default"), family.equals("overlayColor") ? color() : s("click"));
                fields.put(p(family + state), nil());
                String source = generated(node(fields)).build().payload();
                String member = state.equals("Default") ? "any" : Character.toLowerCase(state.charAt(0)) + state.substring(1);
                assertTrue(source.contains("WidgetState." + member + ": null"), source);
                fields.put(p(family), reference("whole"));
                assertFalse(valid(node(fields)));
            }
        }
        var output = generated(node(Map.of(p("overlayColorDefault"), color(), p("mouseCursorDefault"), s("click"))));
        assertFalse(output.build().payload().contains("WidgetState.disabled"));
        assertSymbols(output);
        assertTrue(generated(node(Map.of(p("overlayColorDefault"), theme()))).build().payload().contains("Theme.of(context)"));
    }

    @Test
    void allCursorPresetsAndPriorityProduceCompleteUniqueSymbolEvidence() {
        for (String cursor : RangeSliderWidgetPropertySchema.mouseCursorPresets()) {
            var fields = new LinkedHashMap<PropertyName, PropertyValue>();
            for (String state : RangeSliderWidgetPropertySchema.statePrefixes()) fields.put(p("mouseCursor" + state), s(cursor));
            var output = generated(node(fields));
            assertSymbols(output);
            String source = output.build().payload();
            int previous = -1;
            for (String state : RangeSliderWidgetPropertySchema.statePriority()) {
                int offset = source.indexOf("WidgetState." + (state.equals("default") ? "any" : state));
                assertTrue(offset > previous, source);
                previous = offset;
            }
        }
        assertFalse(accepts("mouseCursor", s("click")));
        assertFalse(accepts("mouseCursorDefault", s("invented")));
    }

    @Test
    void disabledSuppressesOnlyChangedReferenceAndItsImportButRetainsOtherEvidence() {
        var absent = new PropertyValue.DartObjectReferenceValue(Optional.of("package:missing/missing.dart"), "inactiveChanged",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var root = node(Map.of(p("enabled"), b(false), p("onChanged"), absent,
                p("onChangeStart"), reference("start"), p("onChangeEnd"), reference("end"),
                p("semanticFormatterCallback"), reference("formatter")));
        var output = generated(root);
        assertTrue(output.build().payload().contains("onChanged: null"));
        assertFalse(output.build().payload().contains("inactiveChanged"));
        assertFalse(output.imports().payload().contains("package:missing"));
        assertEquals(List.of("ValueChanged<RangeValues>", "ValueChanged<RangeValues>", "SemanticFormatterCallback"),
                output.symbolOccurrences().stream().flatMap(symbol -> symbol.staticTypeRequirement().stream())
                        .map(GeneratedDartStaticTypeRequirement::expectedDartType).toList());
        assertSymbols(output);
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
    }

    @Test
    void allReferenceTypesRetainImportsMembersFactoriesAndStrictModelPaths() throws Exception {
        var expected = new LinkedHashMap<String, String>();
        for (String name : List.of("onChanged", "onChangeStart", "onChangeEnd")) expected.put(name, "ValueChanged<RangeValues>");
        expected.put("semanticFormatterCallback", "SemanticFormatterCallback");
        expected.put("labels", "RangeLabels");
        expected.put("overlayColor", "WidgetStateProperty<Color?>");
        expected.put("mouseCursor", "WidgetStateProperty<MouseCursor?>");
        RangeSliderWidgetPropertySchema.mouseCursorStateProperties().forEach(name -> expected.put(name, "MouseCursor"));
        for (var entry : expected.entrySet()) {
            for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) {
                for (var access : PropertyValue.DartObjectReferenceValue.Access.values()) {
                    var reference = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:range_values/values.dart") : Optional.empty(),
                            member ? "RangeSlider" : "reviewedValue", member ? Optional.of("value") : Optional.empty(), access,
                            access == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION ? Optional.of(false) : Optional.empty());
                    var root = node(Map.of(p(entry.getKey()), reference));
                    var output = generated(root);
                    assertSymbols(output);
                    var typed = output.symbolOccurrences().stream().filter(symbol -> symbol.staticTypeRequirement().isPresent()).toList();
                    assertEquals(1, typed.size());
                    assertEquals(entry.getValue(), typed.getFirst().staticTypeRequirement().orElseThrow().expectedDartType());
                    assertTrue(typed.getFirst().modelPath().endsWith("/properties/" + entry.getKey()
                            + (member ? "/member" : "/rootSymbol")), typed.getFirst().toString());
                    roundTrip(root);
                }
            }
        }
    }

    @Test
    void unsupportedConstructorFieldsAndRawCodeRemainClosed() {
        for (String name : List.of("variant", "values", "value", "child", "focusNode", "autofocus", "showValueIndicator", "thumbColor", "secondaryTrackValue", "allowedInteraction")) {
            assertFalse(valid(node(Map.of(p(name), b(true)))), name);
        }
        for (String name : RangeSliderWidgetPropertySchema.definitions().keySet()) assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")));
        assertFalse(accepts("onChanged", new PropertyValue.CallbackValue("legacy")));
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
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), output.build().payload().substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

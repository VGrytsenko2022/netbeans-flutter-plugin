package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.CheckboxTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class CheckboxContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void all106FieldsAndRequiredDefaultsKeepExactMetadataWithoutSlotsOrFormatChanges() {
        assertEquals(106, definition().properties().size());
        assertEquals(106, CheckboxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(22, CheckboxWidgetPropertySchema.DIRECT_PROPERTY_COUNT);
        assertEquals("Constructor", CheckboxWidgetPropertySchema.find("variant").orElseThrow().displayName());
        assertEquals(new PaletteMetadata("flutter.material", 100, 190, "Checkbox"), definition().palette());
        assertTrue(definition().constConstructor());
        assertTrue(definition().traits().isEmpty());
        assertTrue(definition().slots().isEmpty());
        for (int i = 0; i < definition().properties().size(); i++) {
            var property = definition().properties().get(i);
            assertEquals(i, property.parameter().order());
            assertEquals(i, CheckboxWidgetPropertySchema.find(property.name()).orElseThrow().dartOrder());
            assertEquals(Set.of("value", "variant", "enabled").contains(property.name().value()), property.parameter().required());
            assertEquals(property.parameter().required(), property.creationDefault().isPresent());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition(), StableId.random());
        assertEquals(Map.of(p("value"), b(false), p("variant"), s("standard"), p("enabled"), b(true)), prototype.properties());
        assertTrue(valid(prototype));
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(definition()).isEmpty());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(1024, FdCodecLimits.defaults().maxPropertiesPerWidget());
        assertEquals(1024, ValidationLimits.defaults().maxPropertiesPerWidget());
    }

    @Test
    void capabilityIsIndependentAndFailsClosedOnRelaxedNullableOrNumericDomains() {
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(definition()).isPresent());
        String contract = BuiltInWidgetCapabilityCatalog.reviewedCanvasSchemaContract();
        assertTrue(contract.contains("W|flutter.material.Checkbox\n"));
        assertTrue(contract.contains("P|value|boolean,null|1|boolean:false|-|boolean:any;null:any"));
        for (String name : List.of("splashRadius", "sideWidth", "shape", "fillColor")) {
            var properties = definition().properties().stream().map(property -> property.name().value().equals(name)
                    ? new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING)), property.creationDefault())
                    : property).toList();
            var changed = new WidgetDefinition(definition().typeId(), definition().dartClassName(), definition().namedConstructor(),
                    true, definition().dartLibraryUri(), definition().importUris(), definition().traits(), definition().palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty(), name);
        }
    }

    @Test
    void bothConstConstructorsAndAllValueTristateCombinationsFollowTheSdkAssertion() throws Exception {
        for (String variant : CheckboxWidgetPropertySchema.variants()) {
            for (PropertyValue value : List.of(b(false), b(true), nil())) {
                for (PropertyValue tristate : Arrays.asList(null, b(false), b(true))) {
                    var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                    properties.put(p("value"), value);
                    properties.put(p("variant"), s(variant));
                    properties.put(p("enabled"), b(false));
                    if (tristate != null) properties.put(p("tristate"), tristate);
                    var root = node(properties);
                    boolean expected = !(value instanceof PropertyValue.NullValue) || b(true).equals(tristate);
                    assertEquals(expected, valid(root));
                    if (!expected) continue;
                    var output = generated(root);
                    assertTrue(output.build().payload().contains("const Checkbox" + (variant.equals("adaptive") ? ".adaptive" : "") + "("));
                    assertTrue(output.build().payload().contains("onChanged: null"));
                    assertFalse(output.build().payload().contains("enabled:"));
                    assertFalse(output.build().payload().contains("variant:"));
                    assertSymbols(output);
                    roundTrip(root);
                }
            }
        }
        var root = node(Map.of());
        for (String required : List.of("value", "enabled", "variant")) {
            var properties = new LinkedHashMap<>(root.properties());
            properties.remove(p(required));
            assertFalse(valid(new WidgetNode(root.id(), root.type(), properties, root.slots())));
        }
    }

    @Test
    void enabledNoOpAndDisabledStoredCallbackHaveExactSourceConstnessAndEvidence() {
        assertTrue(generated(node(Map.of())).build().payload().contains("onChanged: (_) {}"));
        var enabled = generated(node(Map.of(p("onChanged"), reference("changed"))));
        assertTrue(enabled.build().payload().contains("buttonValues.changed"));
        assertSymbols(enabled);
        var disabled = generated(node(Map.of(p("enabled"), b(false), p("onChanged"), reference("changed"))));
        assertTrue(disabled.build().payload().contains("const Checkbox("));
        assertFalse(disabled.imports().payload().contains("package:buttons"));
        assertFalse(disabled.build().payload().contains("buttonValues"));
        assertTrue(disabled.symbolOccurrences().stream().noneMatch(symbol -> symbol.modelPath().endsWith("/onChanged")));
    }

    @Test
    void complementaryDenseFamiliesCoverEvery106RowsAndTenShapesWithExactSymbolsAndRoundTrips() throws Exception {
        Set<PropertyName> covered = new HashSet<>();
        for (String variant : CheckboxWidgetPropertySchema.variants()) {
            for (String shape : CheckboxWidgetPropertySchema.shapeKinds()) {
                var properties = full(variant, shape);
                covered.addAll(properties.keySet());
                var root = node(properties);
                assertTrue(valid(root), shape);
                var output = generated(root);
                assertSymbols(output);
                assertFalse(output.build().payload().contains("sideStateful:"));
                assertFalse(output.build().payload().contains("fillColorDefault:"));
                assertFalse(output.build().payload().contains("visualDensityHorizontal:"));
                roundTrip(root);
            }
            var references = node(Map.of(p("variant"), s(variant), p("shape"), reference("shape"),
                    p("fillColor"), reference("fillColor"), p("overlayColor"), reference("overlayColor"), p("side"), reference("side")));
            covered.addAll(references.properties().keySet());
            assertSymbols(generated(references));
            roundTrip(references);
        }
        assertEquals(106, covered.size());
    }

    @Test
    void explicitNullStateColorsWinPriorityWithoutFabricatedDisabledEntryAndLiteralMapsAreConst() {
        for (String family : List.of("fillColor", "overlayColor")) {
            String base = generated(node(Map.of(p("enabled"), b(false), p(family + "Default"), color()))).build().payload();
            assertTrue(base.contains("const WidgetStateProperty<Color?>.fromMap"), base);
            assertFalse(base.contains("WidgetState.disabled"), base);
            assertTrue(base.contains("WidgetState.any: const Color(0xFF123456)"), base);
            for (String state : CheckboxWidgetPropertySchema.sideStates()) {
                var root = node(Map.of(p("enabled"), b(false), p(family + "Default"), color(), p(family + state), nil()));
                var output = generated(root);
                String source = output.build().payload();
                String stateName = Character.toLowerCase(state.charAt(0)) + state.substring(1);
                assertTrue(source.indexOf("WidgetState." + stateName + ": null") < source.indexOf("WidgetState.any:"), source);
                assertSymbols(output);
            }
            var themed = generated(node(Map.of(p("enabled"), b(false), p(family + "Default"), theme())));
            assertFalse(themed.build().payload().contains("const Checkbox("));
            assertSymbols(themed);
        }
    }

    @Test
    void plainSideAndStatefulBaseOnlyHaveDifferentSelectedSemanticsAndInheritIsExplicitNull() {
        String plain = generated(node(Map.of(p("enabled"), b(false), p("sideWidth"), d("2")))).build().payload();
        assertTrue(plain.contains("side: const BorderSide("), plain);
        assertFalse(plain.contains("WidgetStateBorderSide"));
        var mapped = generated(node(Map.of(p("enabled"), b(false), p("sideWidth"), d("2"), p("sideStateful"), b(true))));
        assertTrue(mapped.build().payload().contains("const WidgetStateBorderSide.fromMap"));
        assertSymbols(mapped);
        for (String state : CheckboxWidgetPropertySchema.sideStates()) {
            var inherited = generated(node(Map.of(p("enabled"), b(false), p("sideStateful"), b(true),
                    p("side" + state + "Mode"), s("inherit"), p("sideColor"), color())));
            assertTrue(inherited.build().payload().contains("WidgetState." + Character.toLowerCase(state.charAt(0)) + state.substring(1) + ": null"));
            assertSymbols(inherited);
            String border = generated(node(Map.of(p("enabled"), b(false), p("sideStateful"), b(true),
                    p("side" + state + "Mode"), s("border")))).build().payload();
            assertTrue(border.contains("const BorderSide()"), border);
        }
    }

    @Test
    void sideRelationshipsAndWholeReferencesAreValidatedBeforeGeneration() {
        for (String state : CheckboxWidgetPropertySchema.sideStates()) {
            for (String name : CheckboxWidgetPropertySchema.sideBucketProperties(state)) {
                assertFalse(valid(node(Map.of(p(name), value(name)))));
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(p("sideStateful"), b(true));
                properties.put(p("side" + state + "Mode"), s("inherit"));
                if (!name.endsWith("Mode")) {
                    properties.put(p(name), value(name));
                    assertFalse(valid(node(properties)), name);
                }
            }
        }
        for (String family : List.of("fillColor", "overlayColor", "side", "shape")) {
            List<String> locals = switch (family) {
                case "shape" -> CheckboxWidgetPropertySchema.builtInShapePropertyNames();
                case "side" -> CheckboxWidgetPropertySchema.sideLocalProperties();
                default -> CheckboxWidgetPropertySchema.colorStateProperties(family);
            };
            for (String local : locals) {
                var root = node(Map.of(p(family), reference(family), p(local), value(local)));
                assertFalse(valid(root), local);
                assertFalse(new DartRegionGenerator().generate(document(root), CATALOG).successful());
            }
        }
    }

    @Test
    void numericAndTypedDomainsRemainClosedWithoutInventedSplashBoundOrCoreImport() {
        for (PropertyValue radius : List.of(d("-3"), d("0"), d("1e308"), new PropertyValue.EnumValue("double", "infinity"))) {
            var output = generated(node(Map.of(p("splashRadius"), radius, p("enabled"), b(false))));
            assertFalse(output.imports().payload().contains("dart:core"));
            assertSymbols(output);
        }
        for (String name : List.of("splashRadius", "sideWidth", "sideStrokeAlign")) {
            assertFalse(accepts(name, d("1e400")));
            assertFalse(accepts(name, new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));
        }
        assertFalse(accepts("sideWidth", d("-1")));
        assertTrue(accepts("sideStrokeAlign", d("-5")));
        assertFalse(accepts("visualDensityHorizontal", new PropertyValue.IntegerValue(BigInteger.ONE)));
        assertFalse(accepts("visualDensityHorizontal", d("4.01")));
        assertFalse(accepts("splashRadius", new PropertyValue.EnumValue("double", "maxFinite")));
        for (String name : List.of("shape", "fillColor", "overlayColor", "side", "onChanged")) {
            assertFalse(accepts(name, nil()));
            assertFalse(accepts(name, s("arbitrary()")));
            assertFalse(accepts(name, new PropertyValue.CallbackValue("legacy")));
        }
    }

    @Test
    void cursorPresetsAndSymbolAliasCollisionsPreserveOwnersMembersAndModelPaths() {
        for (String cursor : CheckboxWidgetPropertySchema.mouseCursorPresets()) {
            var output = generated(node(Map.of(p("mouseCursor"), s(cursor))));
            assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals(cursor)));
        }
        for (String collision : List.of("Checkbox", "WidgetStateProperty", "WidgetState", "WidgetStateBorderSide", "Color")) {
            var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), collision, Optional.of("changed"),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var output = generated(node(Map.of(p("variant"), s("adaptive"), p("onChanged"), reference,
                    p("fillColorDefault"), color(), p("sideStateful"), b(true), p("sideSelectedMode"), s("border"))));
            assertSymbols(output);
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("adaptive")));
            assertTrue(output.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("fromMap")));
        }
    }

    @Test
    void directDensityCompletesOnlyTheMissingAxisWithSdkZeroAndThemeTokensKeepEveryOccurrence() {
        var output = generated(node(Map.of(p("visualDensityHorizontal"), d("1"),
                p("fillColorDefault"), theme(), p("overlayColorDefault"), theme(), p("sideColor"), theme(),
                p("shapeKind"), s("roundedRectangle"), p("shapeSideColor"), theme())));
        String source = output.build().payload();
        assertTrue(source.contains("VisualDensity("));
        assertTrue(source.contains("horizontal: 1.0"));
        assertFalse(source.contains("vertical:"));
        assertFalse(source.contains("CheckboxTheme"));
        assertSymbols(output);
        assertEquals(4, output.symbolOccurrences().stream().filter(symbol -> symbol.symbolName().equals("Theme")).count());
    }

    private static boolean accepts(String name, PropertyValue value) {
        return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(c -> c.accepts(value));
    }

    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), CATALOG).valid(); }

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
        String source = output.build().payload();
        for (var symbol : output.symbolOccurrences()) {
            assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()), symbol.toString());
        }
        assertEquals(output.symbolOccurrences().size(), output.symbolOccurrences().stream().map(GeneratedDartSymbolOccurrence::id).distinct().count());
    }
}

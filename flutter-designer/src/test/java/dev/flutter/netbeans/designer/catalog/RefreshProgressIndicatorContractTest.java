package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefreshProgressIndicatorContractTest {
    private static final WidgetTypeId TYPE = RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE;
    private static final PropertyValue.EnumValue INFINITY = new PropertyValue.EnumValue("double", "infinity");
    private static final List<String> NAMES = List.of("value", "backgroundColor", "color", "valueColor", "strokeWidth",
            "strokeAlign", "semanticsLabel", "semanticsValue", "strokeCap", "elevation", "indicatorMargin", "indicatorPadding");

    @Test
    void exactTwelveOptionalConstructorFieldsKeepNoDefaultsOrSlotsAndConstantPrototype() {
        var definition = definition();
        assertEquals("RefreshProgressIndicator", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.traits().isEmpty());
        assertTrue(definition.slots().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material", 100, 120, "RefreshProgressIndicator"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(12, RefreshProgressIndicatorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(0, RefreshProgressIndicatorWidgetPropertySchema.SLOT_COUNT);
        assertEquals(new LinkedHashSet<>(NAMES), RefreshProgressIndicatorWidgetPropertySchema.definitions().keySet());
        for (int index = 0; index < NAMES.size(); index++) {
            var property = definition.property(p(NAMES.get(index))).orElseThrow();
            assertEquals(DartParameter.named(index, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            var schema = RefreshProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(index, schema.dartOrder());
            assertEquals(NAMES.get(index), schema.dartName());
            assertFalse(schema.description().isBlank());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertTrue(valid(prototype));
        assertTrue(generated(prototype).build().payload().contains("return const RefreshProgressIndicator()"));
        assertFalse(generated(prototype).imports().payload().contains("dart:core"));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
        assertEquals(19, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void signedNumbersNullableWidthAndNonnegativeElevationUseTheExactSdkDomains() {
        for (String name : List.of("value", "strokeWidth", "strokeAlign")) {
            for (PropertyValue value : List.of(i(-100), i(0), d("-0.25"), d("0.5"), d("2.5"), d("1e308"))) {
                assertTrue(accepts(name, value), name + ": " + value);
                assertTrue(valid(node(Map.of(p(name), value))));
            }
            assertEquals(name.equals("strokeWidth"), accepts(name, new PropertyValue.NullValue()));
            assertEquals(name.equals("strokeWidth"), RefreshProgressIndicatorWidgetPropertySchema.supportsExplicitNullNumber(p(name)));
        }
        for (PropertyValue value : List.of(i(0), i(2), d("0.25"), d("1e308"))) assertTrue(accepts("elevation", value));
        for (PropertyValue value : List.of(i(-1), d("-0.25"), new PropertyValue.NullValue())) assertFalse(accepts("elevation", value));
        for (String name : List.of("value", "strokeWidth", "strokeAlign", "elevation")) {
            for (PropertyValue value : List.of(INFINITY, d("1e999"), d("1e-999"),
                    new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)),
                    new PropertyValue.EnumValue("double", "nan"), new PropertyValue.EnumValue("double", "negativeInfinity"),
                    new PropertyValue.StringValue("null"), new PropertyValue.DartExpressionValue("2.5")))
                assertFalse(accepts(name, value), name + ": " + value);
        }
        String source = generated(node(Map.of(p("value"), d("-0.25"), p("strokeWidth"), i(-2),
                p("strokeAlign"), i(-4), p("elevation"), i(0)))).build().payload();
        for (String expected : List.of("value: -0.25", "strokeWidth: -2", "strokeAlign: -4", "elevation: 0")) assertTrue(source.contains(expected), source);
    }

    @Test
    void explicitNullWidthIsNotOmissionZeroOrDefaultAndAlwaysRetainsConstnessAndCodecIdentity() throws Exception {
        var codec = new FdDocumentCodec();
        var sources = new HashSet<String>();
        for (PropertyValue value : Arrays.asList(null, new PropertyValue.NullValue(), i(0), d("2.5"), d("-2"))) {
            var root = node(value == null ? Map.of() : Map.of(p("strokeWidth"), value));
            var generated = generated(root);
            String source = generated.build().payload();
            assertTrue(source.contains("return const RefreshProgressIndicator"), source);
            assertEquals(value != null, source.contains("strokeWidth:"), source);
            assertEquals(value instanceof PropertyValue.NullValue, source.contains("strokeWidth: null"), source);
            assertFalse(source.contains("AlwaysStoppedAnimation"), source);
            assertFalse(generated.imports().payload().contains("dart:core"));
            assertTrue(sources.add(source));
            var document = document(root);
            assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
        }
    }

    @Test
    void valueColorLiteralNullThemeAndOmissionHaveExactDifferentSourceSemantics() throws Exception {
        var codec = new FdDocumentCodec();
        assertFalse(generated(node(Map.of())).build().payload().contains("AlwaysStoppedAnimation"));
        for (PropertyValue value : List.of(new PropertyValue.ColorValue(0x80123456L), new PropertyValue.NullValue(),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")))) {
            var root = node(Map.of(p("valueColor"), value));
            var generated = generated(root);
            var source = generated.build().payload();
            boolean themed = value instanceof PropertyValue.ThemeTokenValue;
            assertEquals(!themed, source.contains("return const RefreshProgressIndicator("), source);
            assertEquals(!themed, source.contains("const AlwaysStoppedAnimation"), source);
            if (value instanceof PropertyValue.NullValue) assertTrue(source.contains("AlwaysStoppedAnimation<Color?>(null)"), source);
            if (value instanceof PropertyValue.ColorValue) assertTrue(source.contains("AlwaysStoppedAnimation<Color>(const Color(0x80123456))"), source);
            if (themed) assertTrue(source.contains("AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.primary)"), source);
            assertTrue(generated.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("AlwaysStoppedAnimation") && symbol.modelPath().equals("/root/properties/valueColor")));
            assertTrue(generated.symbolOccurrences().stream().anyMatch(symbol -> symbol.symbolName().equals("Color") && symbol.modelPath().equals("/root/properties/valueColor")));
            for (var symbol : generated.symbolOccurrences()) {
                assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
                assertFalse(symbol.libraryUri().equals("dart:core"));
            }
            var document = document(root);
            var bytes = codec.encode(document);
            assertArrayEquals(bytes.copyBytes(), codec.encode(assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes)).document()).copyBytes());
        }
    }

    @Test
    void allTwelveAnimationReferenceFormsRetainConstnessPathsAndTypedProof() throws Exception {
        int cases = 0;
        for (String name : List.of("valueColor")) for (boolean imported : List.of(false, true))
                for (boolean member : List.of(false, true)) for (int mode = 0; mode < 3; mode++) {
            var reference = reference(imported, member, mode);
            var root = node(Map.of(p(name), reference));
            var generated = generated(root);
            String source = generated.build().payload();
            assertEquals(mode == 2, source.contains("return const RefreshProgressIndicator("), source);
            assertEquals(imported, generated.imports().payload().contains("package:progress/animations.dart"));
            assertFalse(source.contains("AlwaysStoppedAnimation"), source);
            var proofs = generated.symbolOccurrences().stream().flatMap(value -> value.staticTypeRequirement().stream()).toList();
            assertEquals(1, proofs.size());
            var proof = proofs.getFirst();
            assertEquals("Animation<Color?>", proof.expectedDartType());
            String expression = source.substring(proof.expressionOffset(), proof.expressionEndOffset());
            assertTrue(expression.contains(member ? ".configured" : "progressAnimation"), expression);
            assertEquals(mode != 0, expression.endsWith("()"), expression);
            var occurrence = generated.symbolOccurrences().stream().filter(value -> value.staticTypeRequirement().isPresent()).findFirst().orElseThrow();
            assertEquals(Optional.of(root.id()), occurrence.widgetId());
            assertTrue(occurrence.modelPath().startsWith("/root/properties/" + name));
            for (var symbol : generated.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
            var document = document(root);
            var codec = new FdDocumentCodec();
            assertEquals(document, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document))).document());
            cases++;
        }
        assertEquals(12, cases);
    }

    @Test
    void explicitNullWidthNeverBroadensOtherProgressWidgetsAndNullableStoppedColorIsIndependent() {
        for (String type : List.of("flutter.material.LinearProgressIndicator", "flutter.material.CircularProgressIndicator")) {
            var definition = BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
            String name = type.contains("Linear") ? "minHeight" : "strokeWidth";
            assertFalse(definition.property(p(name)).orElseThrow().constraints().stream().anyMatch(value -> value.accepts(new PropertyValue.NullValue())));
        }
        var root = node(Map.of(p("strokeWidth"), new PropertyValue.NullValue(), p("valueColor"), new PropertyValue.NullValue(),
                p("color"), new PropertyValue.ColorValue(0x80112233L), p("value"), d("2")));
        String source = generated(root).build().payload();
        assertTrue(source.contains("strokeWidth: null"), source);
        assertTrue(source.contains("AlwaysStoppedAnimation<Color?>(null)"), source);
        assertTrue(source.contains("Color(0x80112233)"), source);
        assertTrue(source.contains("value: 2.0"), source);
    }

    @Test
    void allTwelveFieldsRetainInsetCoordinatesStrokeCapsAndExactProvenanceAndOrdering() {
        for (boolean directional : List.of(false, true)) for (boolean indeterminate : List.of(false, true))
                for (String cap : List.of("butt", "round", "square")) {
            var properties = fullValues(directional);
            if (indeterminate) properties.remove(p("value"));
            properties.put(p("strokeCap"), new PropertyValue.EnumValue("StrokeCap", cap));
            var root = node(properties);
            assertTrue(valid(root));
            var output = generated(root);
            String source = output.build().payload();
            assertTrue(source.contains(directional ? "EdgeInsetsDirectional.fromSTEB" : "EdgeInsets.fromLTRB"), source);
            assertTrue(source.contains("StrokeCap." + cap), source);
            assertTrue(source.contains("semanticsLabel: 'Loading records'"), source);
            assertTrue(source.contains("semanticsValue: '45%'"), source);
            int previous = -1;
            for (String name : NAMES) {
                if (!properties.containsKey(p(name))) continue;
                int position = source.indexOf(name + ":");
                assertTrue(position > previous, name + ": " + source);
                previous = position;
            }
            for (String name : List.of("backgroundColor", "color", "valueColor")) {
                assertTrue(output.symbolOccurrences().stream().anyMatch(value -> value.symbolName().equals("Theme") && value.modelPath().equals("/root/properties/" + name)));
            }
            for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
        }
    }

    @Test
    void bothInsetsRetainAllCoordinatesWithoutAddingFalseSquareOrSymmetryConstraints() throws Exception {
        for (String name : List.of("indicatorMargin", "indicatorPadding")) {
            for (PropertyValue value : List.of(
                    new PropertyValue.EdgeInsetsValue(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(3)),
                    new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(3)))) {
                assertTrue(accepts(name, value));
                var root = node(Map.of(p("value"), d("0.8"), p(name), value));
                assertTrue(valid(root));
                var codec = new FdDocumentCodec();
                assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
            }
            assertFalse(accepts(name, new PropertyValue.EdgeInsetsValue(BigDecimal.ONE.negate(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
            assertFalse(accepts(name, new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate(), BigDecimal.ZERO)));
            assertFalse(accepts(name, new PropertyValue.NullValue()));
        }
    }

    @Test
    void closedDomainsRejectForeignPropertiesSlotsValuesAndUnsupportedColorReferences() {
        for (String name : NAMES) {
            if (!List.of("valueColor", "strokeWidth").contains(name)) assertFalse(accepts(name, new PropertyValue.NullValue()), name);
            assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        }
        for (String name : List.of("backgroundColor", "color")) {
            assertFalse(accepts(name, reference(false, false, 0)));
            assertFalse(accepts(name, new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))));
        }
        for (String name : List.of("key", "child", "controller", "variant", "year2023", "constraints", "trackGap", "padding", "animationDuration", "valueAnimation")) {
            assertTrue(RefreshProgressIndicatorWidgetPropertySchema.find(p(name)).isEmpty());
            assertFalse(valid(node(Map.of(p(name), i(1)))));
        }
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))));
    }

    @Test
    void capabilityProjectionLocksEveryUnionBindingAndRejectsDomainDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES), BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(12, projection.propertyContracts().size());
        assertTrue(projection.slotContracts().isEmpty());
        assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN, PropertyValueKind.DART_OBJECT_REFERENCE),
                definition.property(p("valueColor")).orElseThrow().acceptedKinds());
        for (var property : definition.properties()) {
            assertFalse(projection.propertyContracts().get(property.name()).required());
            assertTrue(projection.propertyContracts().get(property.name()).creationDefaultFingerprint().isEmpty());
            var properties = new ArrayList<>(definition.properties());
            properties.remove(property);
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true, definition.dartLibraryUri(),
                    definition.importUris(), definition.traits(), definition.palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        }
        for (String name : List.of("strokeWidth")) {
            var properties = new ArrayList<>(definition.properties());
            var property = definition.property(p(name)).orElseThrow();
            properties.set(NAMES.indexOf(name), new PropertyDefinition(property.name(), property.parameter(),
                    property.constraints().stream().filter(value -> value.kind() != PropertyValueKind.NULL).toList(), Optional.empty()));
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true, definition.dartLibraryUri(),
                    definition.importUris(), definition.traits(), definition.palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        }
    }

    @Test
    void importedAnimationNamesRemainDistinctFromSdkConstructorAndStoppedColorTypes() {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/progress.dart"),
                "RefreshProgressIndicator", Optional.of("AlwaysStoppedAnimation"),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        var custom = node(Map.of(p("valueColor"), reference));
        var stopped = node(Map.of(p("valueColor"), new PropertyValue.NullValue(), p("strokeWidth"), new PropertyValue.NullValue()));
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(custom, stopped))));
        var output = generated(root);
        String source = output.build().payload();
        String prefix = output.importPlan().directives().stream().filter(value -> value.uri().equals("package:app/progress.dart"))
                .findFirst().orElseThrow().prefix().orElseThrow();
        assertTrue(source.contains(prefix + ".RefreshProgressIndicator.AlwaysStoppedAnimation()"), source);
        assertTrue(source.contains("const RefreshProgressIndicator("), source);
        assertTrue(source.contains("const AlwaysStoppedAnimation<Color?>(null)"), source);
        assertFalse(source.contains("return const Row("), source);
        for (var symbol : output.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
    }

    static LinkedHashMap<PropertyName, PropertyValue> fullValues(boolean directional) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("value"), d("0.4"));
        for (String name : List.of("backgroundColor", "color", "valueColor")) {
            values.put(p(name), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
        }
        values.put(p("strokeWidth"), d("-2"));
        values.put(p("strokeAlign"), d("-4"));
        values.put(p("semanticsLabel"), new PropertyValue.StringValue("Loading records"));
        values.put(p("semanticsValue"), new PropertyValue.StringValue("45%"));
        values.put(p("strokeCap"), new PropertyValue.EnumValue("StrokeCap", "round"));
        values.put(p("elevation"), d("0"));
        for (String name : List.of("indicatorMargin", "indicatorPadding")) {
            values.put(p(name), directional
                    ? new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4))
                    : new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        }
        return values;
    }
    private static PropertyValue.DartObjectReferenceValue reference(boolean imported, boolean member, int mode) {
        return new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:progress/animations.dart") : Optional.empty(),
                "progressAnimation", member ? Optional.of("configured") : Optional.empty(),
                mode == 0 ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                mode == 0 ? Optional.empty() : Optional.of(mode == 2));
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue.DoubleValue d(String value) { return new PropertyValue.DoubleValue(new BigDecimal(value)); }
    private static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static boolean accepts(String name, PropertyValue value) { return definition().property(p(name)).orElseThrow().constraints().stream().anyMatch(v -> v.accepts(value)); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) { return new WidgetNode(StableId.random(), TYPE, values, Map.of()); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
}

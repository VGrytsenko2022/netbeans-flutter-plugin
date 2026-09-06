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

class CircularProgressIndicatorContractTest {
    private static final WidgetTypeId TYPE = CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE;
    private static final PropertyValue.EnumValue INFINITY = CircularProgressIndicatorWidgetPropertySchema.POSITIVE_INFINITY;
    private static final List<String> NAMES = List.of("value", "backgroundColor", "color", "valueColor", "strokeWidth",
            "strokeAlign", "semanticsLabel", "semanticsValue", "strokeCap", "constraints", "trackGap", "year2023",
            "padding", "controller", "variant");

    @Test
    void allFourteenSdkFieldsAndRequiredDesignerVariantKeepExactConstructorContract() {
        var definition = definition();
        assertEquals("CircularProgressIndicator", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.traits().isEmpty());
        assertTrue(definition.slots().isEmpty());
        assertEquals(new PaletteMetadata("flutter.material", 100, 110, "CircularProgressIndicator"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(15, CircularProgressIndicatorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(0, CircularProgressIndicatorWidgetPropertySchema.SLOT_COUNT);
        assertEquals(new LinkedHashSet<>(NAMES), CircularProgressIndicatorWidgetPropertySchema.definitions().keySet());
        for (int index = 0; index < NAMES.size(); index++) {
            var property = definition.property(p(NAMES.get(index))).orElseThrow();
            boolean variant = property.name().value().equals("variant");
            assertEquals(DartParameter.named(index, variant), property.parameter());
            assertEquals(variant, property.creationDefault().isPresent());
            var schema = CircularProgressIndicatorWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(index, schema.dartOrder());
            assertEquals(NAMES.get(index), schema.dartName());
            assertFalse(schema.description().isBlank());
        }
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(p("variant"), new PropertyValue.StringValue("material")), prototype.properties());
        assertTrue(valid(prototype));
        assertTrue(generated(prototype).build().payload().contains("return const CircularProgressIndicator()"));
        assertFalse(generated(prototype).imports().payload().contains("dart:core"));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(definition));
        assertEquals(13, DesignerDocument.SCHEMA_VERSION);
        assertEquals(14, WidgetCatalog.API_VERSION);
        assertEquals(18, CanvasModelPayloadCodec.VERSION);
        assertEquals(302, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .flatMap(value -> value.properties().stream())
                .filter(value -> value.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN))).count());
    }

    @Test
    void signedSdkNumericDomainsPreserveNegativeWidthsAndUnboundedAlignmentWithoutInventedClamping() {
        for (String name : List.of("value", "strokeWidth", "strokeAlign", "trackGap")) {
            for (PropertyValue value : List.of(i(-100), i(0), d("-0.25"), d("0.5"), d("2.5"), d("1e308"))) {
                assertTrue(accepts(name, value), name + ": " + value);
                assertTrue(valid(node(Map.of(p(name), value))));
            }
            for (PropertyValue value : List.of(d("1e999"), d("1e-999"),
                    new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE)),
                    new PropertyValue.EnumValue("double", "nan"), new PropertyValue.EnumValue("double", "negativeInfinity"),
                    new PropertyValue.StringValue("Infinity"), new PropertyValue.DartExpressionValue("1 / 0"),
                    new PropertyValue.NullValue())) assertFalse(accepts(name, value), name + ": " + value);
            assertEquals(name.equals("trackGap"), accepts(name, INFINITY));
            assertEquals(name.equals("trackGap"), CircularProgressIndicatorWidgetPropertySchema.supportsInfinity(p(name)));
        }
        String source = generated(node(Map.of(p("value"), d("-0.25"), p("strokeWidth"), i(-2),
                p("strokeAlign"), i(-4), p("trackGap"), i(-3)))).build().payload();
        for (String expected : List.of("value: -0.25", "strokeWidth: -2", "strokeAlign: -4", "trackGap: -3")) {
            assertTrue(source.contains(expected), source);
        }
    }

    @Test
    void positiveInfinityGeometryUsesFixedConstLoweringWithoutCoreImportsOrFalseProvenance() throws Exception {
        var codec = new FdDocumentCodec();
        for (String name : List.of("trackGap")) {
            var root = node(Map.of(p(name), INFINITY));
            var generated = generated(root);
            assertTrue(generated.build().payload().contains(name + ": (1.0 / 0.0)"));
            assertTrue(generated.build().payload().contains("return const CircularProgressIndicator("));
            assertFalse(generated.imports().payload().contains("dart:core"));
            assertTrue(generated.symbolOccurrences().stream().noneMatch(value -> value.libraryUri().equals("dart:core")));
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
            assertEquals(!themed, source.contains("return const CircularProgressIndicator("), source);
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
    void allTwelveReferenceFormsOnBothAnimationPropertiesRetainConstnessPathsAndTypedProof() throws Exception {
        int cases = 0;
        for (String variant : List.of("material", "adaptive")) for (String name : List.of("valueColor", "controller")) for (boolean imported : List.of(false, true))
                for (boolean member : List.of(false, true)) for (int mode = 0; mode < 3; mode++) {
            var reference = reference(imported, member, mode);
            var root = node(Map.of(p("variant"), new PropertyValue.StringValue(variant), p(name), reference));
            var generated = generated(root);
            String source = generated.build().payload();
            assertEquals(mode == 2, source.contains("return const CircularProgressIndicator"), source);
            assertEquals(imported, generated.imports().payload().contains("package:progress/animations.dart"));
            assertFalse(source.contains("AlwaysStoppedAnimation"), source);
            var proofs = generated.symbolOccurrences().stream().flatMap(value -> value.staticTypeRequirement().stream()).toList();
            assertEquals(1, proofs.size());
            var proof = proofs.getFirst();
            assertEquals(name.equals("valueColor") ? "Animation<Color?>" : "AnimationController", proof.expectedDartType());
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
        assertEquals(48, cases);
    }

    @Test
    void valueControllerExclusivityKeepsAllSharedPropertiesAndOptionalBooleanTristate() {
        var reference = reference(false, false, 0);
        for (PropertyValue value : List.of(i(-1), i(0), i(1), d("1.5"))) {
            assertFalse(valid(node(Map.of(p("value"), value, p("controller"), reference))));
            assertTrue(valid(node(Map.of(p("value"), value, p("valueColor"), reference))));
        }
        for (Boolean flag : Arrays.asList(null, false, true)) {
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            if (flag != null) properties.put(p("year2023"), new PropertyValue.BooleanValue(flag));
            properties.put(p("strokeWidth"), i(-1));
            properties.put(p("trackGap"), i(-1));
            assertTrue(valid(node(properties)));
            var source = generated(node(properties)).build().payload();
            assertEquals(flag != null, source.contains("year2023:"), source);
            if (flag != null) assertTrue(source.contains("year2023: " + flag), source);
        }
    }

    @Test
    void bothConstructorsAndProgressModesKeepFullPaddingConstraintsCapsAndSourceOrder() {
        for (String variant : List.of("material", "adaptive")) for (boolean directional : List.of(false, true))
                for (boolean controlled : List.of(false, true)) for (String cap : List.of("butt", "round", "square")) {
            var properties = fullValues(directional);
            properties.put(p("variant"), new PropertyValue.StringValue(variant));
            properties.put(p("strokeCap"), new PropertyValue.EnumValue("StrokeCap", cap));
            if (variant.equals("adaptive")) properties.remove(p("color"));
            if (controlled) {
                properties.remove(p("value"));
                properties.put(p("controller"), reference(true, true, 0));
            }
            var root = node(properties);
            assertTrue(valid(root));
            var generated = generated(root);
            var source = generated.build().payload();
            assertTrue(source.contains(directional ? "EdgeInsetsDirectional.fromSTEB" : "EdgeInsets.fromLTRB"), source);
            assertTrue(source.contains("BoxConstraints("), source);
            assertTrue(source.contains("StrokeCap." + cap), source);
            assertTrue(source.contains("semanticsLabel: 'Loading records'"), source);
            assertTrue(source.contains("semanticsValue: '45%'"), source);
            assertTrue(source.contains("year2023: false"), source);
            assertFalse(source.contains("variant:"), source);
            assertEquals(variant.equals("adaptive"), source.contains("CircularProgressIndicator.adaptive("), source);
            assertEquals(variant.equals("material"), source.contains("\n      color:"), source);
            int previous = -1;
            for (String name : NAMES) {
                if (name.equals("variant") || !properties.containsKey(p(name))) continue;
                int position = source.indexOf(name + ":");
                assertTrue(position > previous, name + ": " + source);
                previous = position;
            }
            for (String name : List.of("backgroundColor", "valueColor")) {
                assertTrue(generated.symbolOccurrences().stream().anyMatch(value -> value.symbolName().equals("Theme") && value.modelPath().equals("/root/properties/" + name)));
            }
            for (var symbol : generated.symbolOccurrences()) assertEquals(symbol.symbolName(), source.substring(symbol.offset(), symbol.endOffset()));
        }
    }

    @Test
    void nullableGenericGrammarIsNarrowAndSharedStaticTypeRequirementRejectsEscapeForms() {
        for (String type : List.of("Animation<Color?>", "Animation<Color>", "AnimationController", "CustomClipper<RRect>")) {
            assertDoesNotThrow(() -> new PropertyValueConstraint.DartObjectReferenceValues(type));
            assertDoesNotThrow(() -> new GeneratedDartStaticTypeRequirement(0, 1, type));
        }
        for (String type : List.of("Animation<Color?>?", "Animation<Color??>", "Animation<List<Color>>", "Map<Color,Color>",
                "Animation<Color> Function()", "Animation< Color>", "prefix.Animation<Color?>", "Animation<Color?>;evil()")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyValueConstraint.DartObjectReferenceValues(type), type);
            assertThrows(IllegalArgumentException.class, () -> new GeneratedDartStaticTypeRequirement(0, 1, type), type);
        }
    }

    @Test
    void closedDomainsRejectForeignPropertiesSlotsValuesAndUnsupportedColorReferences() {
        for (String name : NAMES) {
            if (!name.equals("valueColor")) assertFalse(accepts(name, new PropertyValue.NullValue()), name);
            assertFalse(accepts(name, new PropertyValue.DartExpressionValue("arbitrary()")), name);
        }
        for (String name : List.of("backgroundColor", "color")) {
            assertFalse(accepts(name, reference(false, false, 0)));
            assertFalse(accepts(name, new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyMedium"))));
        }
        for (String name : List.of("key", "child", "animationDuration", "minHeight", "valueAnimation")) {
            assertTrue(CircularProgressIndicatorWidgetPropertySchema.find(p(name)).isEmpty());
            assertFalse(valid(node(Map.of(p(name), i(1)))));
        }
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()))));
    }

    @Test
    void capabilityProjectionLocksEveryUnionBindingAndRejectsDomainDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES), BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(15, projection.propertyContracts().size());
        assertTrue(projection.slotContracts().isEmpty());
        assertEquals(Set.of(PropertyValueKind.NULL, PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN, PropertyValueKind.DART_OBJECT_REFERENCE),
                definition.property(p("valueColor")).orElseThrow().acceptedKinds());
        for (var property : definition.properties()) {
            assertEquals(property.name().value().equals("variant"), projection.propertyContracts().get(property.name()).required());
            assertEquals(!property.name().value().equals("variant"), projection.propertyContracts().get(property.name()).creationDefaultFingerprint().isEmpty());
            var properties = new ArrayList<>(definition.properties());
            properties.remove(property);
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true, definition.dartLibraryUri(),
                    definition.importUris(), definition.traits(), definition.palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        }
        for (String name : List.of("trackGap")) {
            var properties = new ArrayList<>(definition.properties());
            var property = definition.property(p(name)).orElseThrow();
            properties.set(NAMES.indexOf(name), new PropertyDefinition(property.name(), property.parameter(),
                    property.constraints().stream().filter(value -> value.kind() != PropertyValueKind.ENUM).toList(), Optional.empty()));
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true, definition.dartLibraryUri(),
                    definition.importUris(), definition.traits(), definition.palette(), properties, List.of());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        }
    }

    @Test
    void requiredVariantAndAdaptiveColorConflictFailClosedWithoutDiscardingValues() {
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of())));
        for (String variant : List.of("", "Adaptive", "cupertino", "adaptive()", "material;evil")) {
            assertFalse(valid(node(Map.of(p("variant"), new PropertyValue.StringValue(variant)))));
        }
        var properties = fullValues(true);
        properties.put(p("variant"), new PropertyValue.StringValue("adaptive"));
        assertFalse(valid(node(properties)));
        assertFalse(new DartRegionGenerator().generate(document(node(properties)), BuiltInWidgetCatalog.getDefault()).successful());
        properties.remove(p("color"));
        assertTrue(valid(node(properties)));
        assertEquals(13, properties.size());
        for (PropertyValue color : List.of(new PropertyValue.ColorValue(0xff123456L),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")), new PropertyValue.NullValue())) {
            var adaptive = node(Map.of(p("variant"), new PropertyValue.StringValue("adaptive"), p("valueColor"), color));
            var output = generated(adaptive);
            String source = output.build().payload();
            assertEquals(!(color instanceof PropertyValue.ThemeTokenValue), source.contains("return const CircularProgressIndicator.adaptive("));
            var occurrence = output.symbolOccurrences().stream().filter(value -> value.symbolName().equals("adaptive")).findFirst().orElseThrow();
            assertEquals("/root/properties/variant", occurrence.modelPath());
            assertEquals("adaptive", source.substring(occurrence.offset(), occurrence.endOffset()));
        }
    }

    @Test
    void constraintsPaddingAndStrokeCapRetainTheCompleteExistingTypedDomains() throws Exception {
        for (var constraints : List.of(new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.empty(), BigDecimal.ZERO, Optional.empty()),
                new PropertyValue.BoxConstraintsValue(PropertyValue.BoxConstraintBound.Infinity.INSTANCE, PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                        PropertyValue.BoxConstraintBound.Infinity.INSTANCE, PropertyValue.BoxConstraintBound.Infinity.INSTANCE))) {
            var root = node(Map.of(p("constraints"), constraints));
            assertTrue(valid(root));
            var codec = new FdDocumentCodec();
            assertEquals(document(root), assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(document(root)))).document());
            assertTrue(generated(root).build().payload().contains("BoxConstraints("));
        }
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsValue(BigDecimal.ONE.negate(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        assertFalse(accepts("padding", new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate(), BigDecimal.ZERO)));
        assertFalse(accepts("strokeCap", new PropertyValue.EnumValue("StrokeJoin", "round")));
        assertFalse(accepts("strokeCap", new PropertyValue.EnumValue("StrokeCap", "none")));
        assertThrows(IllegalArgumentException.class, () -> new PropertyValue.BoxConstraintsValue(BigDecimal.TEN,
                Optional.of(BigDecimal.ONE), BigDecimal.ZERO, Optional.empty()));
    }

    @Test
    void adaptiveConstructorAndAnimationProvenanceRemainExactBesideImportedNameCollisions() {
        var imported = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/animations.dart"),
                "CircularProgressIndicator", Optional.of("AlwaysStoppedAnimation"),
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION, Optional.of(false));
        var custom = node(Map.of(p("valueColor"), imported));
        var adaptive = node(Map.of(p("variant"), new PropertyValue.StringValue("adaptive"),
                p("valueColor"), new PropertyValue.NullValue(), p("trackGap"), INFINITY));
        var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(custom, adaptive))));
        var output = generated(row);
        String source = output.build().payload();
        String prefix = output.importPlan().directives().stream().filter(value -> value.uri().equals("package:app/animations.dart"))
                .findFirst().orElseThrow().prefix().orElseThrow();
        assertTrue(source.contains(prefix + ".CircularProgressIndicator.AlwaysStoppedAnimation()"), source);
        assertTrue(source.contains("const CircularProgressIndicator.adaptive("), source);
        assertTrue(source.contains("const AlwaysStoppedAnimation<Color?>(null)"), source);
        assertFalse(source.contains("return const Row("), source);
        assertFalse(output.imports().payload().contains("dart:core"));
        for (var occurrence : output.symbolOccurrences()) {
            assertEquals(occurrence.symbolName(), source.substring(occurrence.offset(), occurrence.endOffset()));
        }
        var occurrence = output.symbolOccurrences().stream().filter(value -> value.symbolName().equals("adaptive")).findFirst().orElseThrow();
        assertEquals(Optional.of(adaptive.id()), occurrence.widgetId());
        assertEquals("/root/slots/children/children/1/properties/variant", occurrence.modelPath());
    }

    static LinkedHashMap<PropertyName, PropertyValue> fullValues(boolean directional) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        values.put(p("variant"), new PropertyValue.StringValue("material"));
        values.put(p("value"), d("0.4"));
        for (String name : List.of("backgroundColor", "color", "valueColor")) {
            values.put(p(name), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")));
        }
        values.put(p("strokeWidth"), d("-2"));
        values.put(p("strokeAlign"), d("-4"));
        values.put(p("semanticsLabel"), new PropertyValue.StringValue("Loading records"));
        values.put(p("semanticsValue"), new PropertyValue.StringValue("45%"));
        values.put(p("strokeCap"), new PropertyValue.EnumValue("StrokeCap", "round"));
        values.put(p("constraints"), new PropertyValue.BoxConstraintsValue(
                BigDecimal.ONE, Optional.of(BigDecimal.valueOf(90)), BigDecimal.TEN, Optional.empty()));
        values.put(p("padding"), directional
                ? new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4))
                : new PropertyValue.EdgeInsetsValue(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        values.put(p("trackGap"), d("4"));
        values.put(p("year2023"), new PropertyValue.BooleanValue(false));
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
    private static WidgetNode node(Map<PropertyName, PropertyValue> values) { var result = new LinkedHashMap<PropertyName, PropertyValue>(); result.put(p("variant"), new PropertyValue.StringValue("material")); result.putAll(values); return new WidgetNode(StableId.random(), TYPE, result, Map.of()); }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static DesignerDocument document(WidgetNode root) { var region = new ManagedRegion("0".repeat(64)); return new DesignerDocument(StableId.parse("2af55f57-c70c-424f-8413-4c4985e1b02b"), new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root); }
    private static GeneratedDartRegions generated(WidgetNode root) { var result = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()); assertTrue(result.successful(), result.diagnostics().toString()); return result.generated().orElseThrow(); }
}

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultTextHeightBehaviorContractTest {
    private static final WidgetTypeId TYPE = DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE;
    private static final PropertyName FIRST = new PropertyName("textHeightApplyFirstAscent");
    private static final PropertyName LAST = new PropertyName("textHeightApplyLastDescent");
    private static final PropertyName LEADING = new PropertyName("textHeightLeadingDistribution");
    private static final SlotName CHILD = new SlotName("child");
    private static final List<PropertyName> NAMES = List.of(FIRST, LAST, LEADING);
    private static final List<String> MEMBERS = List.of("applyHeightToFirstAscent", "applyHeightToLastDescent", "leadingDistribution");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));
    private static final List<Optional<String>> LEADING_STATES = List.of(Optional.empty(), Optional.of("even"), Optional.of("proportional"));

    @Test
    void exactThreeLeafSchemaKeepsCompositeDefaultsUnpersistedAndRequiresRealChild() {
        var definition = definition();
        assertEquals("DefaultTextHeightBehavior", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 200, "DefaultTextHeightBehavior"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(PropertyDefinition::name).toList());
        assertEquals(3, DefaultTextHeightBehaviorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, DefaultTextHeightBehaviorWidgetPropertySchema.SLOT_COUNT);
        for (int index = 0; index < 3; index++) {
            var property = definition.properties().get(index);
            assertEquals(DartParameter.named(index, false), property.parameter());
            assertTrue(property.creationDefault().isEmpty());
            assertEquals(Set.of(index < 2 ? PropertyValueKind.BOOLEAN : PropertyValueKind.ENUM), property.acceptedKinds());
            assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
            var hint = DefaultTextHeightBehaviorWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(MEMBERS.get(index), hint.dartName());
            assertEquals(index, hint.dartOrder());
            assertEquals("defaultTextHeightBehavior", hint.group().setName());
        }
        var leading = assertInstanceOf(PropertyValueConstraint.EnumValues.class,
                definition.property(LEADING).orElseThrow().constraints().getFirst());
        assertEquals("TextLeadingDistribution", leading.dartType().name());
        assertEquals("package:flutter/widgets.dart", leading.dartType().libraryUri());
        assertEquals(Set.of("proportional", "even"), Set.copyOf(leading.values()));
        assertTrue(DefaultTextHeightBehaviorWidgetPropertySchema.find(FIRST).orElseThrow().description().contains("do not inherit"));
        assertTrue(DefaultTextHeightBehaviorWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(3, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.widgets.DefaultTextHeightBehavior|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
    }

    @Test
    void exactCapabilityRejectsDefaultRequiredRawNullableEnumAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.copyOf(NAMES), projection.propertyContracts().keySet());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, true, 1, 1)),
                projection.slotContracts());
        for (var property : definition.properties()) {
            var contract = projection.propertyContracts().get(property.name());
            assertFalse(contract.required());
            assertTrue(contract.creationDefaultFingerprint().isEmpty());
            assertEquals(property.acceptedKinds(), contract.acceptedKinds());
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), true), property.constraints(), Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), property.constraints(),
                            Optional.of(property.name().equals(LEADING) ? leading("even") : bool(true))),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(properties.indexOf(property), changed);
                assertNoCapability(properties, definition.slots());
            }
        }
        var first = definition.property(FIRST).orElseThrow();
        var nullable = new PropertyDefinition(FIRST, first.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty());
        assertNoCapability(List.of(nullable, definition.property(LAST).orElseThrow(), definition.property(LEADING).orElseThrow()), definition.slots());
        var enumProperty = definition.property(LEADING).orElseThrow();
        var domain = assertInstanceOf(PropertyValueConstraint.EnumValues.class, enumProperty.constraints().getFirst());
        var expanded = new PropertyDefinition(LEADING, enumProperty.parameter(), List.of(
                new PropertyValueConstraint.EnumValues(domain.dartType(), List.of("even", "proportional", "invented"))), Optional.empty());
        assertNoCapability(List.of(first, definition.property(LAST).orElseThrow(), expanded), definition.slots());
        assertNoCapability(List.of(first), definition.slots());
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(3, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(3, true), SlotCardinality.LIST, 1, 1, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void all27CombinationsAlwaysBuildRequiredCompositeWithExactMembersProvenanceAndRoundTrip() throws Exception {
        int combinations = 0;
        var codec = new FdDocumentCodec();
        for (var first : STATES) for (var last : STATES) for (var leading : LEADING_STATES) {
            combinations++;
            var root = node(values(first, last, leading), text(Map.of()));
            var document = document(root);
            assertTrue(valid(root));
            var generated = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
            assertEquals(generated, new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow());
            String build = generated.build().payload();
            assertTrue(build.contains("return const DefaultTextHeightBehavior("), build);
            assertTrue(build.contains("textHeightBehavior: const TextHeightBehavior("), build);
            assertTrue(build.indexOf("textHeightBehavior:") < build.indexOf("child:"), build);
            assertEquals(first.isPresent(), build.contains("applyHeightToFirstAscent:"), build);
            assertEquals(last.isPresent(), build.contains("applyHeightToLastDescent:"), build);
            assertEquals(leading.isPresent(), build.contains("leadingDistribution:"), build);
            first.ifPresent(value -> assertTrue(build.contains("applyHeightToFirstAscent: " + value), build));
            last.ifPresent(value -> assertTrue(build.contains("applyHeightToLastDescent: " + value), build));
            leading.ifPresent(value -> assertTrue(build.contains("leadingDistribution: TextLeadingDistribution." + value), build));
            if (first.isEmpty() && last.isEmpty() && leading.isEmpty()) {
                assertTrue(build.contains("textHeightBehavior: const TextHeightBehavior()"), build);
            }
            for (var name : NAMES) assertFalse(build.contains(name.value() + ":"), build);
            assertFalse(build.contains("textHeightBehavior: null"), build);
            assertFalse(build.contains("key:"), build);
            assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
            for (String name : List.of("DefaultTextHeightBehavior", "TextHeightBehavior")) {
                var symbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals(name)).findFirst().orElseThrow();
                assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                assertEquals(name, build.substring(symbol.offset(), symbol.endOffset()));
                assertEquals(Optional.of(root.id()), symbol.widgetId());
            }
            if (leading.isPresent()) {
                var symbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("TextLeadingDistribution")).findFirst().orElseThrow();
                assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                assertEquals("TextLeadingDistribution", build.substring(symbol.offset(), symbol.endOffset()));
            }
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
        assertEquals(27, combinations);
    }

    @Test
    void textOptionalCompositeRemainsOmittedWhileWrapperUnsetCompositeOverridesAmbient() {
        var plain = text(Map.of());
        var plainBuild = new DartRegionGenerator().generate(document(plain), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
        assertFalse(plainBuild.contains("textHeightBehavior:"), plainBuild);
        var inner = node(Map.of(), plain);
        var outer = node(Map.of(FIRST, bool(false), LAST, bool(false), LEADING, leading("even")), inner);
        var build = new DartRegionGenerator().generate(document(outer), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
        assertEquals(2, build.split("textHeightBehavior:", -1).length - 1, build);
        assertTrue(build.contains("textHeightBehavior: const TextHeightBehavior()"), build);
        assertTrue(build.contains("applyHeightToFirstAscent: false"), build);
        assertTrue(inner.properties().isEmpty());
        var explicitText = text(Map.of(FIRST, bool(false), LAST, bool(true), LEADING, leading("even")));
        var explicitBuild = new DartRegionGenerator().generate(document(node(Map.of(), explicitText)), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
        assertEquals(2, explicitBuild.split("textHeightBehavior:", -1).length - 1, explicitBuild);
        assertTrue(explicitBuild.contains("applyHeightToFirstAscent: false"));
        assertTrue(explicitBuild.contains("applyHeightToLastDescent: true"));
    }

    @Test
    void nonConstChildPreventsConstWrapperButRequiredCompositeRemainsConst() {
        var child = text(Map.of(new PropertyName("styleColor"),
                new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))));
        var root = node(Map.of(FIRST, bool(false)), child);
        var build = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
        assertTrue(build.contains("return DefaultTextHeightBehavior("), build);
        assertFalse(build.contains("const DefaultTextHeightBehavior("), build);
        assertTrue(build.contains("textHeightBehavior: const TextHeightBehavior("), build);
        assertTrue(build.contains("applyHeightToFirstAscent: false"), build);
        assertTrue(build.contains("Theme.of(context).colorScheme.primary"), build);
    }

    @Test
    void smallPositiveTextHeightAndEvenLeadingHaveNoInventedInterPropertyConstraint() {
        var child = text(Map.of(new PropertyName("styleHeight"), new PropertyValue.DoubleValue(new BigDecimal("0.25"))));
        var root = node(Map.of(LEADING, leading("even"), FIRST, bool(false), LAST, bool(false)), child);
        assertTrue(valid(root));
        assertTrue(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
    }

    @Test
    void malformedScalarsForeignEnumAndRawCompositeOrMissingRequiredChildFailClosed() {
        for (var name : NAMES) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("TextHeightBehavior()"))) {
                var root = node(Map.of(name, invalid), text(Map.of()));
                assertFalse(valid(root));
                assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
        for (PropertyValue invalid : List.of(bool(true), leading("unknown"), new PropertyValue.EnumValue("OtherDistribution", "even"))) {
            assertFalse(valid(node(Map.of(LEADING, invalid), text(Map.of()))));
        }
        for (String name : List.of("key", "textHeightBehavior", "applyHeightToFirstAscent", "applyHeightToLastDescent", "leadingDistribution")) {
            assertFalse(valid(node(Map.of(new PropertyName(name), bool(true)), text(Map.of()))));
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text(Map.of())))))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), slots)));
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static PropertyValue.EnumValue leading(String value) { return new PropertyValue.EnumValue("TextLeadingDistribution", value); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var original = definition();
        var copy = new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(),
                original.importUris(), original.traits(), original.palette(), properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(copy).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(copy).isEmpty());
    }
    private static boolean valid(WidgetNode root) { return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid(); }
    private static WidgetNode text(Map<PropertyName, PropertyValue> values) {
        var properties = new LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(new PropertyName("data"), new PropertyValue.StringValue("Height child"));
        properties.putAll(values);
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), properties, Map.of());
    }
    private static Map<PropertyName, PropertyValue> values(Optional<Boolean> first, Optional<Boolean> last, Optional<String> leading) {
        Map<PropertyName, PropertyValue> values = new LinkedHashMap<>();
        first.ifPresent(value -> values.put(FIRST, bool(value)));
        last.ifPresent(value -> values.put(LAST, bool(value)));
        leading.ifPresent(value -> values.put(LEADING, leading(value)));
        return values;
    }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties, WidgetNode child) {
        return new WidgetNode(StableId.random(), TYPE, properties, Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

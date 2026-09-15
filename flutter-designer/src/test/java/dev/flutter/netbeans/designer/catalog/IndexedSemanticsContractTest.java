package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndexedSemanticsContractTest {
    private static final WidgetTypeId TYPE = IndexedSemanticsWidgetPropertySchema.INDEXED_SEMANTICS_TYPE;
    private static final PropertyName INDEX = new PropertyName("index");
    private static final SlotName CHILD = new SlotName("child");
    private static final BigInteger MIN = DartNumericLiterals.MIN_PORTABLE_INTEGER;
    private static final BigInteger MAX = DartNumericLiterals.MAX_PORTABLE_INTEGER;
    private static final List<BigInteger> INDEXES = List.of(MIN, BigInteger.valueOf(-2), BigInteger.valueOf(-1),
            BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), MAX);

    @Test
    void completeConstructorRequiresSignedIndexWithDesignerOnlyZeroPrototypeAndOptionalChild() {
        var definition = definition();
        assertEquals("IndexedSemantics", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.accessibility", 400, 40, "IndexedSemantics"), definition.palette());
        assertEquals(1, IndexedSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, IndexedSemanticsWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of(INDEX), definition.properties().stream().map(PropertyDefinition::name).toList());
        var property = definition.properties().getFirst();
        assertEquals(DartParameter.named(0, true), property.parameter());
        assertEquals(Set.of(PropertyValueKind.INTEGER), property.acceptedKinds());
        assertEquals(Optional.of(integer(BigInteger.ZERO)), property.creationDefault());
        assertEquals(List.of(new PropertyValueConstraint.IntegerRange(MIN, MAX)), property.constraints());
        var hint = IndexedSemanticsWidgetPropertySchema.find(INDEX).orElseThrow();
        assertEquals("Index", hint.displayName());
        assertEquals("indexedSemanticsBehavior", hint.group().setName());
        assertEquals("index", hint.dartName());
        assertEquals(0, hint.dartOrder());
        assertTrue(hint.description().contains("first child semantics node"));
        assertTrue(hint.description().contains("negative indexes"));
        assertTrue(hint.description().contains("not an SDK default"));
        assertTrue(hint.description().contains("Cannot be unset"));
        assertTrue(IndexedSemanticsWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(1, false), slot.parameter());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(0, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(INDEX, integer(BigInteger.ZERO)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void exactRequiredNumericProjectionRejectsRelaxedRequiredRangeKindDefaultAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.PROPERTIES, WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(INDEX), projection.propertyContracts().keySet());
        var contract = projection.propertyContracts().get(INDEX);
        assertTrue(contract.required());
        assertEquals(Optional.of("integer:0"), contract.creationDefaultFingerprint());
        assertEquals(Set.of(PropertyValueKind.INTEGER), contract.acceptedKinds());
        assertEquals(Map.of(PropertyValueKind.INTEGER, "range:-9007199254740991:1:9007199254740991:1"),
                contract.constraintFingerprints());
        assertEquals(new BigDecimal(MIN), contract.numericBounds().get(PropertyValueKind.INTEGER).minimum());
        assertEquals(new BigDecimal(MAX), contract.numericBounds().get(PropertyValueKind.INTEGER).maximum());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                SlotCardinality.SINGLE, false, 0, 1)), projection.slotContracts());
        assertNoCapability(List.of(), definition.slots());
        var original = definition.properties().getFirst();
        for (var changed : List.of(
                new PropertyDefinition(INDEX, DartParameter.named(0, false), original.constraints(), original.creationDefault()),
                new PropertyDefinition(INDEX, original.parameter(), original.constraints(), Optional.empty()),
                new PropertyDefinition(INDEX, original.parameter(), original.constraints(), Optional.of(integer(BigInteger.ONE))),
                new PropertyDefinition(INDEX, original.parameter(), List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, MAX)), original.creationDefault()),
                new PropertyDefinition(INDEX, original.parameter(), List.of(new PropertyValueConstraint.IntegerRange(MIN.add(BigInteger.ONE), MAX)), original.creationDefault()),
                new PropertyDefinition(INDEX, original.parameter(), List.of(new PropertyValueConstraint.IntegerRange(MIN, MAX),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), original.creationDefault()),
                new PropertyDefinition(INDEX, original.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()))) {
            assertNoCapability(List.of(changed), definition.slots());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.IntegerRange(MIN.subtract(BigInteger.ONE), MAX));
        assertThrows(IllegalArgumentException.class,
                () -> new PropertyValueConstraint.IntegerRange(MIN, MAX.add(BigInteger.ONE)));
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(1, true), SlotCardinality.SINGLE, 1, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(1, false), SlotCardinality.LIST, 0, 20, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void signedBoundaryIndexesGenerateDeterministicallyWithExactProvenanceAndRoundTrip() throws Exception {
        var text = text(false);
        for (BigInteger index : INDEXES) {
            for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(),
                    Map.of(CHILD, WidgetSlot.SingleSlot.empty()), Map.of(CHILD, WidgetSlot.SingleSlot.of(text)))) {
                var root = node(index, slots);
                var document = document(root);
                assertTrue(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid());
                var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
                assertTrue(result.successful(), result.diagnostics().toString());
                var generated = result.generated().orElseThrow();
                assertEquals(generated, new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow());
                String build = generated.build().payload();
                assertTrue(build.contains("return const IndexedSemantics("), build);
                assertTrue(build.contains("index: " + index), build);
                assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
                assertEquals(slots.containsKey(CHILD), build.contains("child:"), build);
                if (slots.containsKey(CHILD)) {
                    assertTrue(build.indexOf("index:") < build.indexOf("child:"), build);
                }
                assertFalse(build.contains("key:"), build);
                var symbol = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("IndexedSemantics")).findFirst().orElseThrow();
                assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                assertEquals("IndexedSemantics", build.substring(symbol.offset(), symbol.endOffset()));
                var codec = new FdDocumentCodec();
                var bytes = codec.encode(document);
                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
                assertFalse(decoded.migrated());
                assertEquals(document, decoded.document());
                assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
            }
        }
    }

    @Test
    void nonConstChildKeepsNegativeIndexAndOptionalChildIdentity() {
        var child = text(true);
        var root = node(BigInteger.valueOf(-7), Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
        var generated = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
        assertTrue(generated.build().payload().contains("return IndexedSemantics("));
        assertFalse(generated.build().payload().contains("const IndexedSemantics("));
        assertTrue(generated.build().payload().contains("index: -7"));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
        assertEquals(child.id(), ((WidgetSlot.SingleSlot) root.slots().get(CHILD)).child().orElseThrow().id());
    }

    @Test
    void missingOverflowNonintegerAndInventedFieldsFailClosedWithoutVersionChanges() {
        var validator = new WidgetTreeValidator();
        var missing = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of());
        assertFalse(validator.validate(document(missing), BuiltInWidgetCatalog.getDefault()).valid());
        for (PropertyValue value : List.of(integer(MIN.subtract(BigInteger.ONE)), integer(MAX.add(BigInteger.ONE)),
                new PropertyValue.NullValue(), new PropertyValue.BooleanValue(false), new PropertyValue.StringValue("0"),
                new PropertyValue.DoubleValue(BigDecimal.ONE), new PropertyValue.DoubleValue(new BigDecimal("1.5")),
                new PropertyValue.EnumValue("int", "zero"), new PropertyValue.DartExpressionValue("0"))) {
            var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(INDEX, value), Map.of());
            var result = validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault());
            assertFalse(result.valid(), value.toString());
            assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/index")));
            assertFalse(new DartRegionGenerator().generate(document(invalid), BuiltInWidgetCatalog.getDefault()).successful());
        }
        for (String unknown : List.of("key", "blocking", "excluding", "label", "indexInParent")) {
            var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(INDEX, integer(BigInteger.ZERO),
                    new PropertyName(unknown), new PropertyValue.BooleanValue(true)), Map.of());
            assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(CHILD, new WidgetSlot.ListSlot(List.of())),
                Map.of(new SlotName("children"), WidgetSlot.SingleSlot.empty()))) {
            assertFalse(validator.validate(document(node(BigInteger.ZERO, slots)), BuiltInWidgetCatalog.getDefault()).valid());
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var d = definition();
        var changed = new WidgetDefinition(TYPE, d.dartClassName(), Optional.empty(), true,
                d.dartLibraryUri(), d.importUris(), d.traits(), d.palette(), properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
    }
    private static PropertyValue.IntegerValue integer(BigInteger value) { return new PropertyValue.IntegerValue(value); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(BigInteger index, Map<SlotName, WidgetSlot> slots) {
        return new WidgetNode(StableId.random(), TYPE, Map.of(INDEX, integer(index)), slots);
    }
    private static WidgetNode text(boolean themed) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), themed
                ? Map.of(new PropertyName("data"), new PropertyValue.StringValue("Indexed child"),
                        new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary")))
                : Map.of(new PropertyName("data"), new PropertyValue.StringValue("Indexed child")), Map.of());
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

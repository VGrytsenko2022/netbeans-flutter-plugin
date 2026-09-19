package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockSemanticsContractTest {
    private static final WidgetTypeId TYPE = BlockSemanticsWidgetPropertySchema.BLOCK_SEMANTICS_TYPE;
    private static final PropertyName BLOCKING = new PropertyName("blocking");
    private static final SlotName CHILD = new SlotName("child");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));

    @Test
    void exactSdkConstructorAndHintsDistinguishPreviousPaintedNodesFromExcludedDescendants() {
        var definition = definition();
        assertEquals("BlockSemantics", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.accessibility", 400, 20, "BlockSemantics"), definition.palette());
        assertEquals(List.of("blocking"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(1, BlockSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, BlockSemanticsWidgetPropertySchema.SLOT_COUNT);
        var property = definition.properties().getFirst();
        assertEquals(DartParameter.named(0, false), property.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
        assertTrue(property.creationDefault().isEmpty());
        assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(true)));
        assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(false)));
        assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
        var hint = BlockSemanticsWidgetPropertySchema.find(BLOCKING).orElseThrow();
        assertEquals("blocking", hint.dartName());
        assertEquals(0, hint.dartOrder());
        assertEquals("Blocking", hint.displayName());
        assertEquals("blockSemanticsBehavior", hint.group().setName());
        assertTrue(hint.description().contains("same semantics container"));
        assertTrue(hint.description().contains("not this widget's descendants"));
        assertTrue(hint.description().contains("pointer hit testing are unchanged"));
        assertTrue(hint.description().contains("Omission preserves true"));
        assertTrue(BlockSemanticsWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(1, false), slot.parameter());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(0, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
    }

    @Test
    void exactCapabilityRejectsMissingRequiredNullableRawDefaultedAndListDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(BLOCKING), projection.propertyContracts().keySet());
        var contract = projection.propertyContracts().get(BLOCKING);
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), contract.acceptedKinds());
        assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), contract.constraintFingerprints());
        assertFalse(contract.required());
        assertTrue(contract.creationDefaultFingerprint().isEmpty());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                SlotCardinality.SINGLE, false, 0, 1)), projection.slotContracts());
        assertNoCapability(List.of(), definition.slots());
        var property = definition.properties().getFirst();
        for (var replacement : List.of(
                new PropertyDefinition(BLOCKING, DartParameter.named(0, true), property.constraints(), Optional.empty()),
                new PropertyDefinition(BLOCKING, property.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()),
                new PropertyDefinition(BLOCKING, property.parameter(), property.constraints(), Optional.of(new PropertyValue.BooleanValue(true))),
                new PropertyDefinition(BLOCKING, property.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
            assertNoCapability(List.of(replacement), definition.slots());
        }
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(1, true), SlotCardinality.SINGLE, 1, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(1, false), SlotCardinality.LIST, 0, 20, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void allThreeBooleanStatesAndOptionalChildFormsGenerateWithExactProvenanceAndRoundTrip() throws Exception {
        var codec = new FdDocumentCodec();
        for (var blocking : STATES) {
            for (boolean child : List.of(false, true)) {
                for (boolean childSlot : List.of(false, true)) {
                    if (child && !childSlot) {
                        continue;
                    }
                    var root = node(values(blocking), child);
                    if (!childSlot) {
                        root = new WidgetNode(root.id(), TYPE, root.properties(), Map.of());
                    }
                    var document = document(root);
                    assertTrue(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid());
                    var generated = generate(document);
                    assertEquals(generated, generate(document));
                    String build = generated.build().payload();
                    assertTrue(build.contains("return const BlockSemantics("), build);
                    assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
                    assertEquals(blocking.isPresent(), build.contains("blocking:"), build);
                    blocking.ifPresent(value -> assertTrue(build.contains("blocking: " + value), build));
                    assertEquals(childSlot, build.contains("child:"), build);
                    if (childSlot) {
                        assertTrue(build.contains(child ? "child: const Text('Retained child')" : "child: null"), build);
                        blocking.ifPresent(value -> assertTrue(build.indexOf("blocking:") < build.indexOf("child:"), build));
                    }
                    assertFalse(build.contains("key:"), build);
                    assertFalse(build.contains("excluding:"), build);
                    var symbol = generated.symbolOccurrences().stream().filter(s -> s.symbolName().equals("BlockSemantics"))
                            .findFirst().orElseThrow();
                    assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                    assertEquals("BlockSemantics", build.substring(symbol.offset(), symbol.endOffset()));
                    var encoded = codec.encode(document);
                    var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
                    assertFalse(decoded.migrated());
                    assertEquals(document, decoded.document());
                    assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
                    assertTrue(new String(encoded.copyBytes(), StandardCharsets.UTF_8).contains("\"schemaVersion\": 17"));
                }
            }
        }
    }

    @Test
    void nonConstDescendantPropagatesWithoutDroppingExplicitFalseOrManagedIdentity() {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Retained child"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var root = new WidgetNode(StableId.random(), TYPE, values(Optional.of(false)), Map.of(CHILD, WidgetSlot.SingleSlot.of(text)));
        var generated = generate(document(root));
        assertTrue(generated.build().payload().contains("return BlockSemantics("));
        assertFalse(generated.build().payload().contains("const BlockSemantics("));
        assertTrue(generated.build().payload().contains("blocking: false"));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
        assertFalse(generated.build().payload().contains("key:"));
        assertEquals(text.id(), ((WidgetSlot.SingleSlot) root.slots().get(CHILD)).child().orElseThrow().id());
    }

    @Test
    void malformedValuesInventedPointerSemanticsOverridesAndListChildFailClosed() {
        var validator = new WidgetTreeValidator();
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.EnumValue("bool", "false"),
                new PropertyValue.DartExpressionValue("false"))) {
            var result = validator.validate(document(node(Map.of(BLOCKING, value), false)), BuiltInWidgetCatalog.getDefault());
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/blocking")));
        }
        for (String key : List.of("key", "excluding", "ignoringSemantics", "absorbing", "blockUserActions")) {
            assertFalse(validator.validate(document(node(Map.of(new PropertyName(key), new PropertyValue.BooleanValue(true)), false)),
                    BuiltInWidgetCatalog.getDefault()).valid());
        }
        var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, new WidgetSlot.ListSlot(List.of())));
        assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var definition = definition();
        var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true,
                definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(), properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
    }

    private static Map<PropertyName, PropertyValue> values(Optional<Boolean> blocking) {
        return blocking.<Map<PropertyName, PropertyValue>>map(value -> Map.of(BLOCKING, new PropertyValue.BooleanValue(value))).orElseGet(Map::of);
    }

    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetNode node(Map<PropertyName, PropertyValue> values, boolean child) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue("Retained child")), Map.of());
        return new WidgetNode(StableId.random(), TYPE, values,
                Map.of(CHILD, child ? WidgetSlot.SingleSlot.of(text) : WidgetSlot.SingleSlot.empty()));
    }

    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }

    private static GeneratedDartRegions generate(DesignerDocument document) {
        var result = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
}

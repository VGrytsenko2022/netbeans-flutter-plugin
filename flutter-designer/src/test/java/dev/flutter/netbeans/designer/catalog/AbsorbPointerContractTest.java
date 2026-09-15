package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbsorbPointerContractTest {
    private static final WidgetTypeId TYPE = AbsorbPointerWidgetPropertySchema.ABSORB_POINTER_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));

    @Test
    void exactSdkConstructorAndPropertyHintsIncludeDeprecatedOverrideWithoutCopyingDefaults() {
        var definition = definition();
        assertEquals("AbsorbPointer", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 170, "AbsorbPointer"), definition.palette());
        assertEquals(List.of("absorbing", "ignoringSemantics"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(2, AbsorbPointerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, AbsorbPointerWidgetPropertySchema.SLOT_COUNT);
        int order = 0;
        for (var property : definition.properties()) {
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertTrue(property.creationDefault().isEmpty());
            assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(true)));
            assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(false)));
            assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
            var hint = AbsorbPointerWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), hint.dartName());
            assertFalse(hint.description().isBlank());
            assertFalse(hint.group().description().isBlank());
        }
        var deprecated = AbsorbPointerWidgetPropertySchema.find(new PropertyName("ignoringSemantics")).orElseThrow();
        assertTrue(deprecated.displayName().contains("deprecated"));
        assertTrue(deprecated.description().contains("null"));
        assertTrue(deprecated.description().contains("False preserves semantic actions"));
        assertTrue(AbsorbPointerWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        var absorbing = AbsorbPointerWidgetPropertySchema.find(new PropertyName("absorbing")).orElseThrow();
        assertTrue(absorbing.description().contains("targets behind"));
        assertTrue(absorbing.description().contains("preserving layout and painting"));
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(2, false), slot.parameter());
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
    void exactBooleanOnlyCapabilityCannotBeBroadenedByClaimingNullableOrRequiredFields() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        for (var property : projection.propertyContracts().values()) {
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), property.constraintFingerprints());
            assertFalse(property.required());
            assertTrue(property.creationDefaultFingerprint().isEmpty());
        }
        var altered = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true,
                definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(),
                List.of(definition.properties().getFirst()), definition.slots());
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(altered).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(altered).isEmpty());
        var first = definition.properties().getFirst();
        for (var replacement : List.of(
                new PropertyDefinition(first.name(), DartParameter.named(0, true), first.constraints(), Optional.empty()),
                new PropertyDefinition(first.name(), first.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()),
                new PropertyDefinition(first.name(), first.parameter(), first.constraints(), Optional.of(new PropertyValue.BooleanValue(true))),
                new PropertyDefinition(first.name(), first.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true,
                    definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(),
                    List.of(replacement, definition.properties().getLast()), definition.slots());
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
        }
        assertEquals(Set.of(new PropertyName("absorbing"), new PropertyName("ignoringSemantics")),
                projection.propertyContracts().keySet());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                SlotCardinality.SINGLE, false, 0, 1)), projection.slotContracts());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(2, true), SlotCardinality.SINGLE, 1, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(2, false), SlotCardinality.LIST, 0, 20, new SlotAcceptance.AnyWidget()))) {
            var changed = new WidgetDefinition(TYPE, definition.dartClassName(), Optional.empty(), true,
                    definition.dartLibraryUri(), definition.importUris(), definition.traits(), definition.palette(),
                    definition.properties(), List.of(slot));
            assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(changed).isEmpty());
            assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(changed).isEmpty());
        }
    }

    @Test
    void allOmissionAndBooleanCombinationsGenerateAndRoundTripWithoutLosingFalseOrChild() throws Exception {
        var codec = new FdDocumentCodec();
        for (var absorbing : STATES) {
            for (var semantics : STATES) {
                for (boolean child : List.of(false, true)) {
                    var root = node(values(absorbing, semantics), child);
                    var document = document(root);
                    assertTrue(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid());
                    var first = generate(document);
                    assertEquals(first, generate(document));
                    String build = first.build().payload();
                    assertTrue(build.contains("return const AbsorbPointer("), build);
                    assertEquals("import 'package:flutter/widgets.dart';\n", first.imports().payload());
                    assertEquals(absorbing.isPresent(), build.contains("absorbing:"), build);
                    assertEquals(semantics.isPresent(), build.contains("ignoringSemantics:"), build);
                    absorbing.ifPresent(value -> assertTrue(build.contains("absorbing: " + value), build));
                    semantics.ifPresent(value -> assertTrue(build.contains("ignoringSemantics: " + value), build));
                    assertTrue(build.contains(child ? "child: const Text('Retained child')" : "child: null"), build);
                    if (absorbing.isPresent() && semantics.isPresent()) {
                        assertTrue(build.indexOf("absorbing:") < build.indexOf("ignoringSemantics:"), build);
                    }
                    assertFalse(build.contains("key:"), build);
                    var symbol = first.symbolOccurrences().stream().filter(s -> s.symbolName().equals("AbsorbPointer")).findFirst().orElseThrow();
                    assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                    assertEquals("AbsorbPointer", build.substring(symbol.offset(), symbol.endOffset()));
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
    void nonConstDescendantPropagatesThroughAbsorbPointerWhilePreservingExplicitFalse() {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Retained child"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var root = new WidgetNode(StableId.random(), TYPE, values(Optional.of(false), Optional.of(false)),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text)));
        var generated = generate(document(root));
        assertTrue(generated.build().payload().contains("return AbsorbPointer("));
        assertFalse(generated.build().payload().contains("const AbsorbPointer("));
        assertTrue(generated.build().payload().contains("absorbing: false"));
        assertTrue(generated.build().payload().contains("ignoringSemantics: false"));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
    }

    @Test
    void malformedBooleanValuesUnknownFieldsAndListChildrenFailClosed() {
        var validator = new WidgetTreeValidator();
        for (String key : List.of("absorbing", "ignoringSemantics")) {
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.EnumValue("bool", "false"),
                    new PropertyValue.DartExpressionValue("false"))) {
                var result = validator.validate(document(node(Map.of(new PropertyName(key), value), false)), BuiltInWidgetCatalog.getDefault());
                assertFalse(result.valid());
                assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/" + key)));
            }
        }
        for (String key : List.of("key", "ignoring", "hitTestBehavior")) {
            assertFalse(validator.validate(document(node(Map.of(new PropertyName(key), new PropertyValue.BooleanValue(true)), false)),
                    BuiltInWidgetCatalog.getDefault()).valid());
        }
        var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, new WidgetSlot.ListSlot(List.of())));
        assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static Map<PropertyName, PropertyValue> values(Optional<Boolean> absorbing, Optional<Boolean> semantics) {
        var values = new LinkedHashMap<PropertyName, PropertyValue>();
        absorbing.ifPresent(value -> values.put(new PropertyName("absorbing"), new PropertyValue.BooleanValue(value)));
        semantics.ifPresent(value -> values.put(new PropertyName("ignoringSemantics"), new PropertyValue.BooleanValue(value)));
        return values;
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

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExcludeFocusContractTest {
    private static final WidgetTypeId TYPE = ExcludeFocusWidgetPropertySchema.EXCLUDE_FOCUS_TYPE;
    private static final PropertyName EXCLUDING = new PropertyName("excluding");
    private static final SlotName CHILD = new SlotName("child");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));

    @Test
    void exactConstructorKeepsOptionalBooleanButRequiresARealWrappedChild() {
        var definition = definition();
        assertEquals("ExcludeFocus", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals(Optional.empty(), definition.namedConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.accessibility", 400, 50, "ExcludeFocus"), definition.palette());
        assertEquals(List.of("excluding"), definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(1, ExcludeFocusWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, ExcludeFocusWidgetPropertySchema.SLOT_COUNT);
        var property = definition.properties().getFirst();
        assertEquals(DartParameter.named(0, false), property.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
        assertTrue(property.creationDefault().isEmpty());
        assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(true)));
        assertTrue(property.constraints().getFirst().accepts(new PropertyValue.BooleanValue(false)));
        assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
        var hint = ExcludeFocusWidgetPropertySchema.find(EXCLUDING).orElseThrow();
        assertEquals("excluding", hint.dartName());
        assertEquals(0, hint.dartOrder());
        assertEquals("Excluding", hint.displayName());
        assertEquals("excludeFocusBehavior", hint.group().setName());
        assertTrue(hint.description().contains("currently focused descendants are unfocused"));
        assertTrue(hint.description().contains("does not automatically refocus"));
        assertTrue(hint.description().contains("canRequestFocus configuration is not rewritten"));
        assertTrue(hint.description().contains("effective focus eligibility is blocked"));
        assertTrue(hint.description().contains("pointer hit testing are unchanged"));
        assertTrue(hint.description().contains("Omission preserves true"));
        assertTrue(ExcludeFocusWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        var slot = definition.slot(CHILD).orElseThrow();
        assertEquals(DartParameter.named(1, true), slot.parameter());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(1, slot.minChildren());
        assertEquals(1, slot.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, slot.acceptance());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertEquals(List.of("C|flutter.widgets.ExcludeFocus|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
        assertFalse(new WidgetTreeValidator().validate(document(prototype), BuiltInWidgetCatalog.getDefault()).valid());
    }

    @Test
    void exactCapabilityRejectsMissingRequiredNullableRawDefaultedAndListDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(EXCLUDING), projection.propertyContracts().keySet());
        var contract = projection.propertyContracts().get(EXCLUDING);
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), contract.acceptedKinds());
        assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), contract.constraintFingerprints());
        assertFalse(contract.required());
        assertTrue(contract.creationDefaultFingerprint().isEmpty());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                SlotCardinality.SINGLE, true, 1, 1)), projection.slotContracts());
        assertNoCapability(List.of(), definition.slots());
        var property = definition.properties().getFirst();
        for (var replacement : List.of(
                new PropertyDefinition(EXCLUDING, DartParameter.named(0, true), property.constraints(), Optional.empty()),
                new PropertyDefinition(EXCLUDING, property.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()),
                new PropertyDefinition(EXCLUDING, property.parameter(), property.constraints(), Optional.of(new PropertyValue.BooleanValue(true))),
                new PropertyDefinition(EXCLUDING, property.parameter(), List.of(
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
            assertNoCapability(List.of(replacement), definition.slots());
        }
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(1, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(1, false), SlotCardinality.LIST, 0, 20, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void allBooleanStatesGenerateRealRequiredChildWithExactProvenanceAndRoundTrip() throws Exception {
        var codec = new FdDocumentCodec();
        for (var excluding : STATES) {
            var root = node(values(excluding), true);
            var document = document(root);
            assertTrue(new WidgetTreeValidator().validate(document, BuiltInWidgetCatalog.getDefault()).valid());
            var generated = generate(document);
            assertEquals(generated, generate(document));
            String build = generated.build().payload();
            assertTrue(build.contains("return const ExcludeFocus("), build);
            assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
            assertEquals(excluding.isPresent(), build.contains("excluding:"), build);
            excluding.ifPresent(value -> assertTrue(build.contains("excluding: " + value), build));
            assertTrue(build.contains("child: const Text('Retained child')"), build);
            excluding.ifPresent(value -> assertTrue(build.indexOf("excluding:") < build.indexOf("child:"), build));
            assertFalse(build.contains("key:"), build);
            assertFalse(build.contains("canRequestFocus:"), build);
            var symbol = generated.symbolOccurrences().stream().filter(v -> v.symbolName().equals("ExcludeFocus")).findFirst().orElseThrow();
            assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
            assertEquals("ExcludeFocus", build.substring(symbol.offset(), symbol.endOffset()));
            var encoded = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(encoded));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(encoded.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
    }

    @Test
    void nonConstDescendantPropagatesWithoutDroppingExplicitFalseOrManagedIdentity() {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Retained child"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var root = new WidgetNode(StableId.random(), TYPE, values(Optional.of(false)), Map.of(CHILD, WidgetSlot.SingleSlot.of(text)));
        var generated = generate(document(root));
        assertTrue(generated.build().payload().contains("return ExcludeFocus("));
        assertFalse(generated.build().payload().contains("const ExcludeFocus("));
        assertTrue(generated.build().payload().contains("excluding: false"));
        assertTrue(generated.build().payload().contains("Theme.of(context).colorScheme.primary"));
        assertFalse(generated.build().payload().contains("key:"));
        assertEquals(text.id(), ((WidgetSlot.SingleSlot) root.slots().get(CHILD)).child().orElseThrow().id());
    }

    @Test
    void malformedValuesInventedInternalFocusOverridesAndListChildFailClosed() {
        var validator = new WidgetTreeValidator();
        for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.EnumValue("bool", "false"),
                new PropertyValue.DartExpressionValue("false"))) {
            var result = validator.validate(document(node(Map.of(EXCLUDING, value), true)), BuiltInWidgetCatalog.getDefault());
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(i -> i.path().equals("/root/properties/excluding")));
        }
        for (String key : List.of("key", "blocking", "canRequestFocus", "descendantsAreFocusable", "skipTraversal")) {
            assertFalse(validator.validate(document(node(Map.of(new PropertyName(key), new PropertyValue.BooleanValue(true)), true)),
                    BuiltInWidgetCatalog.getDefault()).valid());
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()), Map.of(CHILD, new WidgetSlot.ListSlot(List.of())))) {
            var invalid = new WidgetNode(StableId.random(), TYPE, Map.of(), slots);
            assertFalse(validator.validate(document(invalid), BuiltInWidgetCatalog.getDefault()).valid());
            assertFalse(new DartRegionGenerator().generate(document(invalid), BuiltInWidgetCatalog.getDefault()).successful());
        }
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

    private static Map<PropertyName, PropertyValue> values(Optional<Boolean> excluding) {
        return excluding.<Map<PropertyName, PropertyValue>>map(value -> Map.of(EXCLUDING, new PropertyValue.BooleanValue(value))).orElseGet(Map::of);
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

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.canvas.payload.CanvasModelPayloadCodec;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TickerModeContractTest {
    private static final WidgetTypeId TYPE = TickerModeWidgetPropertySchema.TICKER_MODE_TYPE;
    private static final PropertyName ENABLED = new PropertyName("enabled");
    private static final PropertyName FORCE = new PropertyName("forceFrames");
    private static final SlotName CHILD = new SlotName("child");
    private static final List<Optional<Boolean>> STATES = List.of(Optional.empty(), Optional.of(false), Optional.of(true));

    @Test
    void completeConstructorDistinguishesRequiredEnabledCreationValueFromOptionalSdkDefault() {
        var definition = definition();
        assertEquals("TickerMode", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 190, "TickerMode"), definition.palette());
        assertEquals(List.of(ENABLED, FORCE), definition.properties().stream().map(PropertyDefinition::name).toList());
        assertEquals(2, TickerModeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, TickerModeWidgetPropertySchema.SLOT_COUNT);
        var enabled = definition.property(ENABLED).orElseThrow();
        assertEquals(DartParameter.named(0, true), enabled.parameter());
        assertEquals(Optional.of(bool(true)), enabled.creationDefault());
        var force = definition.property(FORCE).orElseThrow();
        assertEquals(DartParameter.named(2, false), force.parameter());
        assertTrue(force.creationDefault().isEmpty());
        for (var property : definition.properties()) {
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertTrue(property.constraints().getFirst().accepts(bool(false)));
            assertTrue(property.constraints().getFirst().accepts(bool(true)));
            assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
            var hint = TickerModeWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), hint.dartName());
            assertEquals(property.parameter().order(), hint.dartOrder());
            assertEquals("tickerModeBehavior", hint.group().setName());
        }
        assertTrue(TickerModeWidgetPropertySchema.find(ENABLED).orElseThrow().description().contains("not a Flutter constructor default"));
        assertTrue(TickerModeWidgetPropertySchema.find(ENABLED).orElseThrow().description().contains("AND every ancestor"));
        assertTrue(TickerModeWidgetPropertySchema.find(FORCE).orElseThrow().description().contains("OR any ancestor"));
        assertTrue(TickerModeWidgetPropertySchema.find(FORCE).orElseThrow().description().contains("Omission preserves false"));
        assertTrue(TickerModeWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(1, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(ENABLED, bool(true)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.widgets.TickerMode|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
        assertEquals(Map.of(ENABLED, bool(false)), WidgetNodePrototypeFactory.create(definition, StableId.random(),
                Map.of(ENABLED, bool(false))).properties(), "Explicit false must override the Designer creation value");
    }

    @Test
    void exactCapabilityRejectsRequiredDefaultNullableRawAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(Set.of(ENABLED, FORCE), projection.propertyContracts().keySet());
        assertTrue(projection.propertyContracts().get(ENABLED).required());
        assertEquals(Optional.of("boolean:true"), projection.propertyContracts().get(ENABLED).creationDefaultFingerprint());
        assertFalse(projection.propertyContracts().get(FORCE).required());
        assertTrue(projection.propertyContracts().get(FORCE).creationDefaultFingerprint().isEmpty());
        for (var property : projection.propertyContracts().values()) {
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), property.constraintFingerprints());
        }
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, true, 1, 1)),
                projection.slotContracts());
        for (var property : definition.properties()) {
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(property.parameter().order(), !property.parameter().required()),
                            property.constraints(), property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), property.constraints(), Optional.of(bool(false))),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), property.creationDefault()),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(properties.indexOf(property), changed);
                assertNoCapability(properties, definition.slots());
            }
        }
        var enabled = definition.property(ENABLED).orElseThrow();
        var noDefault = new PropertyDefinition(ENABLED, enabled.parameter(), enabled.constraints(), Optional.empty());
        assertNoCapability(List.of(noDefault, definition.property(FORCE).orElseThrow()), definition.slots());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(copy(List.of(noDefault), definition.slots())));
        assertNoCapability(List.of(enabled), definition.slots());
        assertNoCapability(definition.properties(), List.of());
        for (var slot : List.of(
                new SlotDefinition(CHILD, DartParameter.named(1, false), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(CHILD, DartParameter.named(1, true), SlotCardinality.LIST, 1, 1, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(slot));
        }
    }

    @Test
    void everyRequiredEnabledAndOptionalForceStateGeneratesInConstructorOrderAndRoundTripsExactly() throws Exception {
        var codec = new FdDocumentCodec();
        for (boolean enabled : List.of(false, true)) {
            for (var force : STATES) {
                var root = node(enabled, force, text("Ticker child"));
                var document = document(root);
                assertTrue(valid(root), root.toString());
                var generated = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
                assertEquals(generated, new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow());
                String build = generated.build().payload();
                assertTrue(build.contains("return const TickerMode("), build);
                assertTrue(build.contains("enabled: " + enabled), build);
                assertTrue(build.contains("child: const Text('Ticker child')"), build);
                assertTrue(build.indexOf("enabled:") < build.indexOf("child:"), build);
                assertEquals(force.isPresent(), build.contains("forceFrames:"), build);
                force.ifPresent(value -> {
                    assertTrue(build.contains("forceFrames: " + value), build);
                    assertTrue(build.indexOf("child:") < build.indexOf("forceFrames:"), build);
                });
                assertFalse(build.contains("key:"), build);
                assertFalse(build.contains("TickerMode.merge"), build);
                assertEquals("import 'package:flutter/widgets.dart';\n", generated.imports().payload());
                var symbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("TickerMode")).findFirst().orElseThrow();
                assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
                assertEquals("TickerMode", build.substring(symbol.offset(), symbol.endOffset()));
                var bytes = codec.encode(document);
                var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
                assertFalse(decoded.migrated());
                assertEquals(document, decoded.document());
                assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
            }
        }
    }

    @Test
    void nestedModesPreserveRequestedIndependentValuesInsteadOfFlatteningInheritedBehavior() {
        for (boolean outerEnabled : List.of(false, true)) {
            for (boolean innerEnabled : List.of(false, true)) {
                for (boolean outerForce : List.of(false, true)) {
                    for (boolean innerForce : List.of(false, true)) {
                        var inner = node(innerEnabled, Optional.of(innerForce), text("Nested ticker child"));
                        var outer = node(outerEnabled, Optional.of(outerForce), inner);
                        assertTrue(valid(outer));
                        var build = new DartRegionGenerator().generate(document(outer), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
                        assertEquals(2, build.split("TickerMode\\(", -1).length - 1, build);
                        assertEquals(Map.of(ENABLED, bool(innerEnabled), FORCE, bool(innerForce)), inner.properties());
                        assertEquals(Map.of(ENABLED, bool(outerEnabled), FORCE, bool(outerForce)), outer.properties());
                        assertTrue(build.contains("enabled: " + innerEnabled), build);
                        assertTrue(build.contains("forceFrames: " + innerForce), build);
                    }
                }
            }
        }
    }

    @Test
    void nonConstChildPreventsConstParentWithoutDroppingFalseOrRequiredIdentity() {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Theme dependent"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var root = node(false, Optional.of(false), child);
        var build = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
        assertTrue(build.contains("return TickerMode("), build);
        assertFalse(build.contains("const TickerMode("), build);
        assertTrue(build.contains("enabled: false"), build);
        assertTrue(build.contains("forceFrames: false"), build);
        assertTrue(build.contains("Theme.of(context).colorScheme.primary"), build);
        assertEquals(child.id(), ((WidgetSlot.SingleSlot) root.slots().get(CHILD)).child().orElseThrow().id());
    }

    @Test
    void malformedValuesMissingRequiredArgumentsAndInventedMergeFieldsFailClosed() {
        for (var name : List.of(ENABLED, FORCE)) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("false"))) {
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                properties.put(ENABLED, bool(true));
                properties.put(name, invalid);
                var root = new WidgetNode(StableId.random(), TYPE, properties, Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Required"))));
                assertFalse(valid(root));
                assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Required"))))));
        for (String name : List.of("key", "merge", "muted", "ticker", "vsync", "child", "visible")) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(ENABLED, bool(true), new PropertyName(name), bool(true)),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Required"))))));
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text("Invalid")))))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(ENABLED, bool(true)), slots)));
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
        assertEquals(20, CanvasModelPayloadCodec.VERSION);
    }

    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static WidgetDefinition definition() { return BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow(); }
    private static WidgetDefinition copy(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var original = definition();
        return new WidgetDefinition(TYPE, original.dartClassName(), Optional.empty(), true, original.dartLibraryUri(),
                original.importUris(), original.traits(), original.palette(), properties, slots);
    }
    private static void assertNoCapability(List<PropertyDefinition> properties, List<SlotDefinition> slots) {
        var copy = copy(properties, slots);
        assertTrue(BuiltInWidgetCapabilityCatalog.capabilities(copy).isEmpty());
        assertTrue(BuiltInWidgetCapabilityCatalog.canvasProjection(copy).isEmpty());
    }
    private static boolean valid(WidgetNode root) {
        return new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault()).valid();
    }
    private static WidgetNode text(String data) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(data)), Map.of());
    }
    private static WidgetNode node(boolean enabled, Optional<Boolean> force, WidgetNode child) {
        Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        properties.put(ENABLED, bool(enabled));
        force.ifPresent(value -> properties.put(FORCE, bool(value)));
        return new WidgetNode(StableId.random(), TYPE, properties, Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

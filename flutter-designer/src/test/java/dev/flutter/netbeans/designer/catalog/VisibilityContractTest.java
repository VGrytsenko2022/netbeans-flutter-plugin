package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisibilityContractTest {
    private static final WidgetTypeId TYPE = VisibilityWidgetPropertySchema.VISIBILITY_TYPE;
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName REPLACEMENT = new SlotName("replacement");
    private static final List<String> NAMES = List.of("visible", "maintainState", "maintainAnimation",
            "maintainSize", "maintainSemantics", "maintainInteractivity", "maintainFocusability");
    private static final List<List<String>> DEPENDENCIES = List.of(
            List.of("maintainAnimation", "maintainState"), List.of("maintainSize", "maintainAnimation"),
            List.of("maintainSemantics", "maintainSize"), List.of("maintainInteractivity", "maintainSize"),
            List.of("maintainFocusability", "maintainState"));

    @Test
    void completeConstructorHasSevenOptionalBooleansRequiredChildAndOptionalNonNullReplacement() {
        var definition = definition();
        assertEquals("Visibility", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 180, "Visibility"), definition.palette());
        assertEquals(NAMES, definition.properties().stream().map(p -> p.name().value()).toList());
        assertEquals(7, VisibilityWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(2, VisibilityWidgetPropertySchema.SLOT_COUNT);
        for (int index = 0; index < NAMES.size(); index++) {
            var property = definition.properties().get(index);
            assertEquals(DartParameter.named(index + 2, false), property.parameter());
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), property.acceptedKinds());
            assertTrue(property.creationDefault().isEmpty());
            assertTrue(property.constraints().getFirst().accepts(bool(true)));
            assertTrue(property.constraints().getFirst().accepts(bool(false)));
            assertFalse(property.constraints().getFirst().accepts(new PropertyValue.NullValue()));
            var metadata = VisibilityWidgetPropertySchema.find(property.name()).orElseThrow();
            assertEquals(property.name().value(), metadata.dartName());
            assertEquals(index + 2, metadata.dartOrder());
            assertTrue(metadata.description().contains(index == 0 ? "Omission preserves true" : "Omission preserves false"));
        }
        assertTrue(VisibilityWidgetPropertySchema.find(new PropertyName("key")).isEmpty());
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(0, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        assertEquals(new SlotDefinition(REPLACEMENT, DartParameter.named(1, false), SlotCardinality.SINGLE, 0, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(REPLACEMENT).orElseThrow());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty(), REPLACEMENT, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.widgets.Visibility|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
    }

    @Test
    void independentCapabilityProjectionIsExactAndRejectsEveryScalarAndSlotDrift() {
        var definition = definition();
        assertEquals(Set.of(WidgetCapability.CANVAS, WidgetCapability.CREATE, WidgetCapability.DND, WidgetCapability.PROPERTIES),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        var projection = BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(7, projection.propertyContracts().size());
        assertEquals(Map.of(CHILD, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, true, 1, 1),
                REPLACEMENT, new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(SlotCardinality.SINGLE, false, 0, 1)),
                projection.slotContracts());
        for (int index = 0; index < 7; index++) {
            var property = definition.properties().get(index);
            var contract = projection.propertyContracts().get(property.name());
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), contract.acceptedKinds());
            assertEquals(Map.of(PropertyValueKind.BOOLEAN, "any"), contract.constraintFingerprints());
            assertFalse(contract.required());
            assertTrue(contract.creationDefaultFingerprint().isEmpty());
            for (var changed : List.of(
                    new PropertyDefinition(property.name(), DartParameter.named(index + 2, true), property.constraints(), Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), property.constraints(), Optional.of(bool(true))),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)), Optional.empty()),
                    new PropertyDefinition(property.name(), property.parameter(), List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()))) {
                var properties = new ArrayList<>(definition.properties());
                properties.set(index, changed);
                assertNoCapability(properties, definition.slots());
            }
        }
        assertNoCapability(List.of(), definition.slots());
        assertNoCapability(definition.properties(), List.of(definition.slot(CHILD).orElseThrow()));
        assertNoCapability(definition.properties(), List.of(definition.slot(REPLACEMENT).orElseThrow()));
        for (var changed : List.of(
                new SlotDefinition(REPLACEMENT, DartParameter.named(1, true), SlotCardinality.SINGLE, 1, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(REPLACEMENT, DartParameter.named(1, false), SlotCardinality.LIST, 0, 20, new SlotAcceptance.AnyWidget()))) {
            assertNoCapability(definition.properties(), List.of(definition.slot(CHILD).orElseThrow(), changed));
        }
    }

    @Test
    void all2187UnsetFalseTrueCombinationsEnforcePreciselyTheFiveSdkImplicationsRegardlessOfVisible() throws Exception {
        int validCount = 0;
        for (int encoded = 0; encoded < 2187; encoded++) {
            int remaining = encoded;
            Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
            for (String name : NAMES) {
                int state = remaining % 3;
                remaining /= 3;
                if (state != 0) properties.put(new PropertyName(name), bool(state == 2));
            }
            var root = node(properties, Optional.of(WidgetSlot.SingleSlot.of(text("Replacement"))));
            var result = new WidgetTreeValidator().validate(document(root), BuiltInWidgetCatalog.getDefault());
            var failed = DEPENDENCIES.stream().filter(pair -> isTrue(properties, pair.getFirst())
                    && !isTrue(properties, pair.getLast())).toList();
            assertEquals(failed.isEmpty(), result.valid(), properties.toString());
            assertEquals(failed.size(), result.errors().size(), properties.toString());
            for (var pair : failed) {
                assertTrue(result.errors().stream().anyMatch(issue ->
                        issue.path().equals("/root/properties/" + pair.getFirst())
                        && issue.message().contains(pair.getLast() + "=true")), result.errors().toString());
            }
            if (result.valid()) {
                validCount++;
                var generated = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault());
                assertTrue(generated.successful(), generated.diagnostics().toString());
                String build = generated.generated().orElseThrow().build().payload();
                for (String name : NAMES) {
                    assertEquals(properties.containsKey(new PropertyName(name)), build.contains(name + ":"), build);
                }
            }
        }
        assertTrue(validCount > 0 && validCount < 2187);
    }

    @Test
    void bothSlotBranchesRoundTripExactlyButUnsetAndEmptyReplacementBothOmitNonNullSdkArgument() throws Exception {
        var codec = new FdDocumentCodec();
        for (var replacement : List.of(Optional.<WidgetSlot>empty(), Optional.<WidgetSlot>of(WidgetSlot.SingleSlot.empty()),
                Optional.<WidgetSlot>of(WidgetSlot.SingleSlot.of(text("Fallback"))))) {
            var root = node(Map.of(new PropertyName("visible"), bool(false)), replacement);
            var document = document(root);
            assertTrue(valid(root));
            var generated = new DartRegionGenerator().generate(document, BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
            var build = generated.build().payload();
            assertTrue(build.contains("return const Visibility("), build);
            assertTrue(build.contains("child: const Text('Retained child')"), build);
            assertTrue(build.contains("visible: false"), build);
            assertFalse(build.contains("replacement: null"), build);
            boolean hasReplacement = replacement.filter(slot -> ((WidgetSlot.SingleSlot) slot).child().isPresent()).isPresent();
            assertEquals(hasReplacement, build.contains("replacement:"), build);
            if (hasReplacement) {
                assertTrue(build.contains("replacement: const Text('Fallback')"), build);
                assertTrue(build.indexOf("child:") < build.indexOf("replacement:"));
                assertTrue(build.indexOf("replacement:") < build.indexOf("visible:"));
            }
            var symbol = generated.symbolOccurrences().stream().filter(value -> value.symbolName().equals("Visibility")).findFirst().orElseThrow();
            assertEquals("package:flutter/widgets.dart", symbol.libraryUri());
            assertEquals("Visibility", build.substring(symbol.offset(), symbol.endOffset()));
            assertFalse(build.contains("key:"));
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
    }

    @Test
    void maintainNamedConstructorIsFullyRepresentableThroughAllSixExplicitFlagsWithBothVisibleValues() {
        for (boolean visible : List.of(false, true)) {
            Map<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
            NAMES.forEach(name -> properties.put(new PropertyName(name), bool(!name.equals("visible") || visible)));
            var root = node(properties, Optional.empty());
            assertTrue(valid(root));
            var build = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
            assertTrue(build.contains("const Visibility("), build);
            for (String name : NAMES.subList(1, 7)) assertTrue(build.contains(name + ": true"), build);
            assertTrue(build.contains("visible: " + visible), build);
            assertFalse(build.contains("replacement:"), build);
            assertFalse(build.contains("Visibility.maintain"), "Canonical generation uses equivalent public default arguments");
        }
        assertTrue(VisibilityWidgetPropertySchema.find(new PropertyName("maintainFocusability")).orElseThrow()
                .description().contains("exactly represent Visibility.maintain"));
    }

    @Test
    void ignoredReplacementIsPreservedAndNonConstInEitherBranchStillPreventsConstParent() {
        for (String slot : List.of("child", "replacement")) {
            var themed = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                    new PropertyName("data"), new PropertyValue.StringValue("Theme dependent"),
                    new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
            var root = new WidgetNode(StableId.random(), TYPE, Map.of(new PropertyName("maintainState"), bool(true)),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(slot.equals("child") ? themed : text("Child")),
                            REPLACEMENT, WidgetSlot.SingleSlot.of(slot.equals("replacement") ? themed : text("Ignored replacement"))));
            assertTrue(valid(root));
            var build = new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).generated().orElseThrow().build().payload();
            assertTrue(build.contains("return Visibility("), build);
            assertTrue(build.contains("replacement:"), build);
            assertTrue(build.contains("Theme.of(context).colorScheme.primary"), build);
        }
    }

    @Test
    void malformedBooleansMissingRequiredChildListSlotsAndUnknownInternalArgumentsFailClosed() {
        for (String name : NAMES) {
            for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                    new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("false"))) {
                var root = node(Map.of(new PropertyName(name), invalid), Optional.empty());
                assertFalse(valid(root));
                assertFalse(new DartRegionGenerator().generate(document(root), BuiltInWidgetCatalog.getDefault()).successful());
            }
        }
        for (String name : List.of("key", "maintain", "includeSemantics", "replacement", "child", "canRequestFocus")) {
            assertFalse(valid(node(Map.of(new PropertyName(name), bool(true)), Optional.empty())));
        }
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text("Invalid")))),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Valid")), REPLACEMENT, new WidgetSlot.ListSlot(List.of())))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(), slots)));
        }
        assertEquals(16, DesignerDocument.SCHEMA_VERSION);
        assertEquals(15, WidgetCatalog.API_VERSION);
        assertEquals(19, CanvasModelPayloadCodec.VERSION);
    }

    @Test
    void genericWrapperClassifierAcceptsOptionalSecondarySlotsButRejectsOtherRequiredShapes() {
        var definition = definition();
        var requiredChild = definition.slot(CHILD).orElseThrow();
        for (var extra : List.of(
                new SlotDefinition(REPLACEMENT, DartParameter.named(1, true), SlotCardinality.SINGLE, 0, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(REPLACEMENT, DartParameter.named(1, false), SlotCardinality.SINGLE, 1, 1, new SlotAcceptance.AnyWidget()),
                new SlotDefinition(REPLACEMENT, DartParameter.named(1, true), SlotCardinality.LIST, 1, 10, new SlotAcceptance.AnyWidget()))) {
            assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                    WidgetPlacementRules.creationMode(copy(definition.properties(), List.of(requiredChild, extra))));
        }
        var listChild = new SlotDefinition(CHILD, DartParameter.named(0, true), SlotCardinality.LIST, 1, 1, new SlotAcceptance.AnyWidget());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(copy(definition.properties(), List.of(listChild))));
        var scalar = definition.properties().getFirst();
        var requiredScalar = new PropertyDefinition(scalar.name(), DartParameter.named(2, true), scalar.constraints(), Optional.empty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(copy(List.of(requiredScalar), definition.slots())));
        var optionalList = new SlotDefinition(REPLACEMENT, DartParameter.named(1, false), SlotCardinality.LIST, 0, 10, new SlotAcceptance.AnyWidget());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(copy(definition.properties(), List.of(requiredChild, optionalList))));
    }

    private static boolean isTrue(Map<PropertyName, PropertyValue> properties, String name) {
        return bool(true).equals(properties.get(new PropertyName(name)));
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
    private static boolean valid(WidgetNode node) {
        return new WidgetTreeValidator().validate(document(node), BuiltInWidgetCatalog.getDefault()).valid();
    }
    private static WidgetNode text(String data) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(data)), Map.of());
    }
    private static WidgetNode node(Map<PropertyName, PropertyValue> properties, Optional<WidgetSlot> replacement) {
        Map<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        slots.put(CHILD, WidgetSlot.SingleSlot.of(text("Retained child")));
        replacement.ifPresent(value -> slots.put(REPLACEMENT, value));
        return new WidgetNode(StableId.random(), TYPE, properties, slots);
    }
    private static DesignerDocument document(WidgetNode root) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), root);
    }
}

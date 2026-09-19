package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.state.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipVisibilityContractTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final WidgetTypeId TYPE = TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE;
    private static final PropertyName VISIBLE = new PropertyName("visible");
    private static final SlotName CHILD = new SlotName("child");

    @Test void exactRequiredConstructorAndCreationPolicyMatchPinnedSurface() {
        var definition = CATALOG.find(TYPE).orElseThrow();
        assertEquals("TooltipVisibility", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertTrue(definition.namedConstructor().isEmpty());
        assertEquals("package:flutter/material.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/material.dart"), definition.importUris());
        assertEquals(new PaletteMetadata("flutter.material", 100, 310, "TooltipVisibility"), definition.palette());
        assertEquals(1, TooltipVisibilityWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(1, TooltipVisibilityWidgetPropertySchema.SLOT_COUNT);
        assertEquals(List.of(VISIBLE), definition.properties().stream().map(PropertyDefinition::name).toList());
        var visible = definition.property(VISIBLE).orElseThrow();
        assertEquals(DartParameter.named(0, true), visible.parameter());
        assertEquals(Optional.of(bool(true)), visible.creationDefault());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), visible.acceptedKinds());
        assertEquals(new SlotDefinition(CHILD, DartParameter.named(1, true), SlotCardinality.SINGLE, 1, 1,
                new SlotAcceptance.AnyWidget()), definition.slot(CHILD).orElseThrow());
        var schema = TooltipVisibilityWidgetPropertySchema.find(VISIBLE).orElseThrow();
        assertEquals("visible", schema.dartName());
        assertEquals(0, schema.dartOrder());
        assertEquals("tooltipVisibilityBehavior", schema.group().setName());
        assertTrue(schema.description().contains("closest TooltipVisibility wins"));
        assertTrue(schema.description().contains("not a Flutter constructor default"));
        assertTrue(schema.description().contains("tooltip-message semantic annotation"));
        assertTrue(TooltipVisibilityWidgetPropertySchema.find(new PropertyName("onChanged")).isEmpty());
        var prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(Map.of(VISIBLE, bool(true)), prototype.properties());
        assertEquals(Map.of(CHILD, WidgetSlot.SingleSlot.empty()), prototype.slots());
        assertFalse(valid(prototype));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(definition));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(definition));
        assertEquals(List.of("C|flutter.material.TooltipVisibility|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(definition));
        assertEquals(Map.of(VISIBLE, bool(false)), WidgetNodePrototypeFactory.create(definition, StableId.random(),
                Map.of(VISIBLE, bool(false))).properties());
    }

    @Test void booleanValuesGenerateDeterministicallyWithMaterialProvenanceAndExactCodecRoundTrip() throws Exception {
        for (boolean visible : List.of(false, true)) {
            var document = document(node(visible, text("Anchor")));
            var generated = new DartRegionGenerator().generate(document, CATALOG).generated().orElseThrow();
            assertEquals(generated, new DartRegionGenerator().generate(document, CATALOG).generated().orElseThrow());
            var build = generated.build().payload();
            assertTrue(build.contains("return const TooltipVisibility("), build);
            assertTrue(build.contains("visible: " + visible), build);
            assertTrue(build.contains("child: const Text('Anchor')"), build);
            assertTrue(build.indexOf("visible:") < build.indexOf("child:"));
            assertFalse(build.contains("key:"));
            assertFalse(build.contains("onChanged:"));
            var symbol = generated.symbolOccurrences().stream()
                    .filter(value -> value.symbolName().equals("TooltipVisibility")).findFirst().orElseThrow();
            assertEquals("package:flutter/material.dart", symbol.libraryUri());
            assertEquals("TooltipVisibility", build.substring(symbol.offset(), symbol.endOffset()));
            var codec = new FdDocumentCodec();
            var bytes = codec.encode(document);
            var decoded = assertInstanceOf(FdDecodeResult.Current.class, codec.decode(bytes));
            assertFalse(decoded.migrated());
            assertEquals(document, decoded.document());
            assertArrayEquals(bytes.copyBytes(), codec.encode(decoded.document()).copyBytes());
        }
    }

    @Test void nestedRequestedFlagsStayIndependentRatherThanFlatteningNearestScopePolicy() {
        for (boolean outer : List.of(false, true)) for (boolean inner : List.of(false, true)) {
            var nested = node(inner, text("Independent child"));
            var root = node(outer, nested);
            assertTrue(valid(root));
            var build = new DartRegionGenerator().generate(document(root), CATALOG).generated().orElseThrow().build().payload();
            assertEquals(2, build.split("TooltipVisibility\\(", -1).length - 1);
            assertEquals(Map.of(VISIBLE, bool(outer)), root.properties());
            assertEquals(Map.of(VISIBLE, bool(inner)), nested.properties());
            assertTrue(build.contains("visible: " + outer));
            assertTrue(build.contains("visible: " + inner));
            assertFalse(build.contains("&&"));
        }
    }

    @Test void nonConstChildMakesOnlyNecessaryAncestorsNonConst() {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(
                new PropertyName("data"), new PropertyValue.StringValue("Theme anchor"),
                new PropertyName("styleColor"), new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.primary"))), Map.of());
        var build = new DartRegionGenerator().generate(document(node(false, child)), CATALOG).generated().orElseThrow().build().payload();
        assertTrue(build.contains("return TooltipVisibility("), build);
        assertFalse(build.contains("const TooltipVisibility("), build);
        assertTrue(build.contains("visible: false"));
        assertTrue(build.contains("Theme.of(context).colorScheme.primary"));
    }

    @Test void missingRequiredArgumentsNullRawAndInventedEventPropertiesAreRejected() {
        for (PropertyValue invalid : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("false"),
                new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.DartExpressionValue("false"),
                new PropertyValue.CallbackValue("_changed"))) {
            var root = new WidgetNode(StableId.random(), TYPE, Map.of(VISIBLE, invalid),
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Child"))));
            assertFalse(valid(root));
            assertFalse(new DartRegionGenerator().generate(document(root), CATALOG).successful());
        }
        assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Child"))))));
        for (var slots : List.<Map<SlotName, WidgetSlot>>of(Map.of(), Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Map.of(CHILD, new WidgetSlot.ListSlot(List.of(text("Child")))))) {
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, Map.of(VISIBLE, bool(true)), slots)));
        }
        for (String unknown : List.of("key", "child", "onChanged", "onTriggered", "enabled", "merge", "maintainSemantics")) {
            var properties = new LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(VISIBLE, bool(true));
            properties.put(new PropertyName(unknown), bool(true));
            assertFalse(valid(new WidgetNode(StableId.random(), TYPE, properties,
                    Map.of(CHILD, WidgetSlot.SingleSlot.of(text("Child"))))), unknown);
        }
        assertEquals(17, DesignerDocument.SCHEMA_VERSION);
        assertEquals(16, WidgetCatalog.API_VERSION);
    }

    @Test void oneRequiredBooleanStateConsumerDoesNotInventEventOrStateProducer() {
        var root = node(true, text("Child"));
        assertTrue(WidgetEventCatalog.eventsFor(CATALOG.find(TYPE).orElseThrow()).isEmpty());
        assertTrue(WidgetStateBindingCatalog.find(root).isEmpty());
        assertEquals(List.of(VISIBLE), WidgetStatePropertyBindingCatalog.descriptors(root).stream()
                .map(WidgetStatePropertyBindingCatalog.Descriptor::propertyName).toList());
        assertEquals(Set.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT,
                StatePropertyBinding.Transform.EQUALS),
                Set.copyOf(WidgetStatePropertyBindingCatalog.find(root, VISIBLE).orElseThrow().allowedTransforms()));
    }

    @Test void installedSdkSignatureAndNearestScopeImplementationRemainReviewed() throws Exception {
        String sdk = System.getProperty("flutter.events.sdk");
        Assumptions.assumeTrue(sdk != null && !sdk.isBlank(), "Pinned SDK verification is opt-in");
        String source = Files.readString(Path.of(sdk, "packages/flutter/lib/src/material/tooltip_visibility.dart"));
        assertTrue(source.contains("const TooltipVisibility({super.key, required this.visible, required this.child})"));
        assertTrue(source.contains("final bool visible;"));
        assertTrue(source.contains("final Widget child;"));
        assertTrue(source.contains("return visibility?.visible ?? true;"));
        assertTrue(source.contains("return _TooltipVisibilityScope(visible: visible, child: child);"));
    }

    private static PropertyValue.BooleanValue bool(boolean value) { return new PropertyValue.BooleanValue(value); }
    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }
    private static WidgetNode node(boolean visible, WidgetNode child) {
        return new WidgetNode(StableId.random(), TYPE, Map.of(VISIBLE, bool(visible)), Map.of(CHILD, WidgetSlot.SingleSlot.of(child)));
    }
    private static boolean valid(WidgetNode node) {
        return new WidgetTreeValidator().validate(document(node), CATALOG).valid();
    }
    private static DesignerDocument document(WidgetNode node) {
        var region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(), new DartSourceDescriptor("sample.dart", "Sample",
                WidgetClassKind.STATELESS, Optional.empty(), new ManagedRegions(region, region)), node);
    }
}

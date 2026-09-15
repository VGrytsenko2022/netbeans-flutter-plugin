package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SliverCrossAxisExpandedContractTest {
    public static final WidgetTypeId TYPE = SliverCrossAxisExpandedWidgetPropertySchema.TYPE;
    public static final SlotName SLIVER = new SlotName("sliver"), SLIVERS = new SlotName("slivers");
    public static WidgetNode wrapper(StableId id, WidgetNode child, PropertyValue flex) {
        return new WidgetNode(id, TYPE, flex == null ? Map.of() : Map.of(new PropertyName("flex"), flex),
                Map.of(SLIVER, new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    public static PropertyValue.IntegerValue integer(long n) { return new PropertyValue.IntegerValue(BigInteger.valueOf(n)); }
    public static WidgetNode adapter() {
        return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), StableId.random());
    }
    public static DesignerDocument document(WidgetNode child) {
        var group = new WidgetNode(StableId.random(), SliverCrossAxisGroupWidgetPropertySchema.TYPE, Map.of(),
                Map.of(SLIVERS, new WidgetSlot.ListSlot(List.of(child))));
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.CustomScrollView"), Map.of(),
                Map.of(SLIVERS, new WidgetSlot.ListSlot(List.of(group))));
        return new DesignerDocument(Optional.empty(), StableId.random(),
                new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATELESS, Optional.empty(),
                        new ManagedRegions(new ManagedRegion("0".repeat(64)), new ManagedRegion("0".repeat(64)))),
                Optional.empty(), root, Extensions.empty());
    }
    @Test void completeRequiredConstructorAndAtomicCreationContract() {
        var d = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        assertEquals(List.of(SliverCrossAxisExpandedWidgetPropertySchema.flex()), d.properties());
        var p = d.properties().getFirst(); var slot = d.slots().getFirst();
        assertTrue(p.parameter().required()); assertEquals(integer(1), p.creationDefault().orElseThrow());
        assertEquals(Set.of(PropertyValueKind.INTEGER), p.acceptedKinds());
        assertEquals(1, d.slots().size()); assertEquals(SLIVER, slot.name());
        assertTrue(slot.parameter().required()); assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(1, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertTrue(slot.acceptance() instanceof SlotAcceptance.HasTrait);
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(d));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(d));
        assertEquals(slot, WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow());
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(d).isEmpty());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
    }
    @Test void currentInsertionMatrixRetainsAllExistingDestinations() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        long destinations = 0, accepted = 0, rejected = 0, any = 0;
        for (var parent : catalog.definitions()) for (var slot : parent.slots()) {
            if (slot.minChildren() != 0) continue;
            destinations++;
            if (slot.acceptance() instanceof SlotAcceptance.AnyWidget) any++;
            for (var child : catalog.definitions()) {
                if (WidgetPlacementRules.accepts(parent, slot, child)) accepted++;
                else rejected++;
            }
        }
        assertEquals(242, catalog.definitions().size());
        assertEquals(215, destinations); assertEquals(184, any);
        assertEquals(52030, accepted + rejected); assertEquals(33494, accepted); assertEquals(18536, rejected);
    }
    @Test void everyParentSlotAndChildTypeRespectsParentDataPlacement() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var d = catalog.find(TYPE).orElseThrow();
        for (var parent : catalog.definitions()) for (var slot : parent.slots()) {
            boolean allowed = parent.typeId().equals(SliverCrossAxisGroupWidgetPropertySchema.TYPE) && slot.name().equals(SLIVERS);
            assertEquals(allowed, WidgetPlacementRules.accepts(parent, slot, d), parent.typeId() + "." + slot.name());
        }
        for (var child : catalog.definitions())
            assertEquals(WidgetPlacementRules.isSliverWidget(child) && !child.typeId().equals(TYPE),
                    WidgetPlacementRules.accepts(d, d.slots().getFirst(), child), child.typeId().value());
    }
    @Test void positivePortableIntegerAndRequiredChildValidateRoundTripAndGenerateConstDart() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (long flex : List.of(1L, 2L, 7L, 9007199254740991L)) {
            var doc = document(wrapper(StableId.random(), adapter(), integer(flex)));
            assertTrue(new WidgetTreeValidator().validate(doc, catalog).valid());
            var codec = new FdDocumentCodec();
            assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
            var generated = new DartRegionGenerator().generate(doc, catalog);
            assertTrue(generated.successful(), () -> generated.diagnostics().toString());
            var code = generated.generated().orElseThrow().build().payload();
            assertTrue(code.contains("const SliverCrossAxisExpanded("), code);
            assertTrue(code.contains("flex: " + flex), code); assertTrue(code.contains("sliver:"), code);
        }
        for (PropertyValue bad : List.of(integer(0), integer(-1), integer(9007199254740992L),
                new PropertyValue.DoubleValue(new BigDecimal("1.5")), new PropertyValue.NullValue())) {
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), bad)), catalog).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), null)), catalog).valid());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), null, integer(1))), catalog).valid());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(),
                wrapper(StableId.random(), adapter(), integer(1)), integer(1))), catalog).valid());
    }
}

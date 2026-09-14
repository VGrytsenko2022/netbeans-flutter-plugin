package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SliverIgnorePointerContractTest {
    public static final WidgetTypeId TYPE = SliverIgnorePointerWidgetPropertySchema.TYPE;
    public static final SlotName SLIVER = new SlotName("sliver"), SLIVERS = new SlotName("slivers");
    public static WidgetNode adapter() { return SliverCrossAxisExpandedContractTest.adapter(); }
    public static DesignerDocument document(WidgetNode child) { return SliverCrossAxisExpandedContractTest.document(child); }
    public static WidgetNode wrapper(StableId id, WidgetNode child, Map<PropertyName, PropertyValue> props) {
        return new WidgetNode(id, TYPE, props, Map.of(SLIVER, new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    @Test void completeConstructorUsesNativeDefaultsAndOptionalSliver() {
        var d = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertEquals(SliverIgnorePointerWidgetPropertySchema.properties(), d.properties());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        assertEquals(List.of("ignoring", "ignoringSemantics"), d.properties().stream().map(p -> p.name().value()).toList());
        for (var p : d.properties()) { assertFalse(p.parameter().required()); assertTrue(p.creationDefault().isEmpty()); }
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), d.properties().getFirst().acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN, PropertyValueKind.NULL), d.properties().get(1).acceptedKinds());
        assertEquals(1, d.slots().size()); var slot = d.slots().getFirst(); assertEquals(SLIVER, slot.name());
        assertFalse(slot.parameter().required()); assertEquals(2, slot.parameter().order());
        assertEquals(0, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(d));
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
        assertTrue(WidgetNodePrototypeFactory.create(d, StableId.random()).properties().isEmpty());
    }
    @Test void everyParentAndChildRespectsSliverAndParentDataContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault(); var d = catalog.find(TYPE).orElseThrow();
        for (var parent : catalog.definitions()) for (var slot : parent.slots()) {
            boolean allowed = slot.acceptance() instanceof SlotAcceptance.HasTrait t && t.trait().equals(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT);
            assertEquals(allowed, WidgetPlacementRules.accepts(parent, slot, d), parent.typeId() + "." + slot.name());
        }
        for (var child : catalog.definitions())
            assertEquals(WidgetPlacementRules.isSliverWidget(child) && !child.typeId().equals(SliverCrossAxisExpandedWidgetPropertySchema.TYPE),
                    WidgetPlacementRules.accepts(d, d.slots().getFirst(), child), child.typeId().value());
    }
    @Test void everyBooleanNullAndOmissionStateRoundTripsAndGeneratesIncludingMissingSlots() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (Boolean ignoring : Arrays.asList(null, false, true))
        for (PropertyValue semantics : Arrays.asList(null, new PropertyValue.NullValue(),
                new PropertyValue.BooleanValue(false), new PropertyValue.BooleanValue(true)))
        for (int slotState : List.of(0, 1, 2)) {
            var props = new LinkedHashMap<PropertyName, PropertyValue>();
            if (ignoring != null) props.put(new PropertyName("ignoring"), new PropertyValue.BooleanValue(ignoring));
            if (semantics != null) props.put(new PropertyName("ignoringSemantics"), semantics);
            var widget = wrapper(StableId.random(), slotState == 2 ? adapter() : null, props);
            if (slotState == 0) widget = new WidgetNode(widget.id(), TYPE, props, Map.of());
            var doc = document(widget);
            assertTrue(new WidgetTreeValidator().validate(doc, catalog).valid());
            var codec = new FdDocumentCodec();
            assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
            var generated = new DartRegionGenerator().generate(doc, catalog);
            assertTrue(generated.successful(), () -> generated.diagnostics().toString());
            var code = generated.generated().orElseThrow().build().payload();
            assertTrue(code.contains("const SliverIgnorePointer("), code);
            assertTrue(code.contains("sliver:"), code);
            if (slotState != 2) assertTrue(code.contains("const SliverToBoxAdapter()"), code);
            assertEquals(ignoring != null, code.contains("ignoring:"), code);
            assertEquals(semantics != null, code.contains("ignoringSemantics:"), code);
            if (semantics instanceof PropertyValue.NullValue) assertTrue(code.contains("ignoringSemantics: null"), code);
        }
    }
    @Test void invalidBooleansAndBoxChildrenAreRejected() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var p : SliverIgnorePointerWidgetPropertySchema.properties())
        for (PropertyValue bad : List.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE), new PropertyValue.StringValue("true"))) {
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), Map.of(p.name(), bad))), catalog).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(),
                Map.of(new PropertyName("ignoring"), new PropertyValue.NullValue()))), catalog).valid());
        assertTrue(new WidgetTreeValidator().validate(document(wrapper(StableId.random(),
                wrapper(StableId.random(), adapter(), Map.of()), Map.of())), catalog).valid());
        var expanded = SliverCrossAxisExpandedContractTest.wrapper(StableId.random(), adapter(), SliverCrossAxisExpandedContractTest.integer(2));
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), expanded, Map.of())), catalog).valid());
        var box = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), box, Map.of())), catalog).valid());
    }
}


package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SliverOpacityContractTest {
    public static final WidgetTypeId TYPE = SliverOpacityWidgetPropertySchema.TYPE;
    public static final SlotName SLIVER = new SlotName("sliver"), SLIVERS = new SlotName("slivers");
    public static PropertyValue.DoubleValue number(String n) { return new PropertyValue.DoubleValue(new BigDecimal(n)); }
    public static WidgetNode wrapper(StableId id, WidgetNode child, PropertyValue value) {
        return new WidgetNode(id, TYPE, value == null ? Map.of() : Map.of(new PropertyName("opacity"), value),
                Map.of(SLIVER, new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    public static WidgetNode adapter() { return SliverCrossAxisExpandedContractTest.adapter(); }
    public static DesignerDocument document(WidgetNode child) { return SliverCrossAxisExpandedContractTest.document(child); }
    @Test void completeConstructorUsesExplicitOpacityAndOptionalSliver() {
        var d = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertEquals(SliverOpacityWidgetPropertySchema.properties(), d.properties());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        var p = d.properties().getFirst(); var slot = d.slots().getFirst();
        assertTrue(p.parameter().required()); assertEquals(number("1"), p.creationDefault().orElseThrow());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE), p.acceptedKinds());
        var semantics = d.properties().get(1);
        assertFalse(semantics.parameter().required()); assertTrue(semantics.creationDefault().isEmpty());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), semantics.acceptedKinds());
        assertEquals(1, d.slots().size()); assertEquals(SLIVER, slot.name());
        assertFalse(slot.parameter().required()); assertEquals(2, slot.parameter().order());
        assertEquals(0, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(d));
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
        assertEquals(Map.of(new PropertyName("opacity"), number("1")),
                WidgetNodePrototypeFactory.create(d, StableId.random()).properties());
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
    @Test void endpointsFractionAndBooleanStatesRoundTripAndGenerateConst() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var value : List.of(number("0"), number("0.125"), number("1"),
                new PropertyValue.IntegerValue(BigInteger.ZERO), new PropertyValue.IntegerValue(BigInteger.ONE)))
        for (Boolean semantics : Arrays.asList(null, false, true)) for (boolean empty : List.of(false, true)) {
            var widget = wrapper(StableId.random(), empty ? null : adapter(), value);
            var props = new LinkedHashMap<>(widget.properties());
            if (semantics != null) props.put(new PropertyName("alwaysIncludeSemantics"), new PropertyValue.BooleanValue(semantics));
            var doc = document(new WidgetNode(widget.id(), TYPE, props, widget.slots()));
            assertTrue(new WidgetTreeValidator().validate(doc, catalog).valid());
            var codec = new FdDocumentCodec();
            assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
            var generated = new DartRegionGenerator().generate(doc, catalog);
            assertTrue(generated.successful(), () -> generated.diagnostics().toString());
            var code = generated.generated().orElseThrow().build().payload();
            assertTrue(code.contains("const SliverOpacity("), code);
            assertTrue(code.contains("opacity:"), code);
            assertTrue(code.contains("sliver:"), code);
            if (empty) assertTrue(code.contains("const SliverToBoxAdapter()"), code);
            assertEquals(semantics != null, code.contains("alwaysIncludeSemantics:"), code);
        }
    }
    @Test void invalidValuesAndBoxChildrenAreRejected() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var bad : List.of(number("-0.01"), number("1.00001"), number("1e309"),
                new PropertyValue.IntegerValue(BigInteger.TWO), new PropertyValue.NullValue(),
                new PropertyValue.EnumValue("double", "infinity"), new PropertyValue.StringValue("0.5"))) {
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), bad)), catalog).valid(), bad.toString());
        }
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), null)), catalog).valid());
        var nested = wrapper(StableId.random(), wrapper(StableId.random(), adapter(), number("0.5")), number("0"));
        assertTrue(new WidgetTreeValidator().validate(document(nested), catalog).valid());
        var expanded = SliverCrossAxisExpandedContractTest.wrapper(StableId.random(), adapter(), SliverCrossAxisExpandedContractTest.integer(2));
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), expanded, number("1"))), catalog).valid());
        var box = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), box, number("1"))), catalog).valid());
    }
}

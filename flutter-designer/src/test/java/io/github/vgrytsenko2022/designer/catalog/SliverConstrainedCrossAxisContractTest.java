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

public class SliverConstrainedCrossAxisContractTest {
    public static final WidgetTypeId TYPE = SliverConstrainedCrossAxisWidgetPropertySchema.TYPE;
    public static final SlotName SLIVER = new SlotName("sliver"), SLIVERS = new SlotName("slivers");
    public static PropertyValue.DoubleValue number(String n) { return new PropertyValue.DoubleValue(new BigDecimal(n)); }
    public static WidgetNode wrapper(StableId id, WidgetNode child, PropertyValue value) {
        return new WidgetNode(id, TYPE, value == null ? Map.of() : Map.of(new PropertyName("maxExtent"), value),
                Map.of(SLIVER, new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    public static WidgetNode adapter() { return SliverCrossAxisExpandedContractTest.adapter(); }
    public static DesignerDocument document(WidgetNode child) { return SliverCrossAxisExpandedContractTest.document(child); }
    @Test void completeConstructorCreatesOnlyAnAtomicRequiredSliverWrapper() {
        var d = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertEquals(List.of(SliverConstrainedCrossAxisWidgetPropertySchema.maxExtent()), d.properties());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        var p = d.properties().getFirst(); var slot = d.slots().getFirst();
        assertTrue(p.parameter().required()); assertEquals(number("120"), p.creationDefault().orElseThrow());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE, PropertyValueKind.ENUM), p.acceptedKinds());
        assertEquals(1, d.slots().size()); assertEquals(SLIVER, slot.name());
        assertTrue(slot.parameter().required()); assertEquals(1, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertEquals(slot, WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow());
        assertTrue(WidgetPlacementRules.requiredAnyWidgetWrapperSlot(d).isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD, WidgetPlacementRules.creationMode(d));
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
        assertEquals(List.of("R|" + TYPE.value() + "|requiresSlotTrait|flutter.widgets.Sliver",
                "C|" + TYPE.value() + "|paletteCreate|wrapExistingChild|sliver"), WidgetPlacementRules.capabilityFingerprintLines(d));
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
    @Test void zeroFractionFiniteAndInfinityRoundTripAndGenerateWithoutRawDart() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var value : List.of(number("0"), number("0.125"), number("120"), number("1e308"),
                new PropertyValue.IntegerValue(DartNumericLiterals.MAX_PORTABLE_INTEGER), SliverConstrainedCrossAxisWidgetPropertySchema.INFINITY)) {
            var doc = document(wrapper(StableId.random(), adapter(), value));
            assertTrue(new WidgetTreeValidator().validate(doc, catalog).valid());
            var codec = new FdDocumentCodec();
            assertEquals(doc, assertInstanceOf(FdDecodeResult.Current.class, codec.decode(codec.encode(doc))).document());
            var generated = new DartRegionGenerator().generate(doc, catalog);
            assertTrue(generated.successful(), () -> generated.diagnostics().toString());
            assertFalse(generated.generated().orElseThrow().imports().payload().contains("dart:core"),
                    "Infinity is a closed constant expression; never suppress the user implicit core import.");
            var code = generated.generated().orElseThrow().build().payload();
            assertTrue(code.contains("const SliverConstrainedCrossAxis("), code);
            assertTrue(code.contains("maxExtent:"), code); assertTrue(code.contains("sliver:"), code);
            if (value instanceof PropertyValue.EnumValue) assertTrue(code.contains("(1.0 / 0.0)"), code);
        }
        for (var bad : List.of(number("-0.01"), number("1e309"), new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),
                new PropertyValue.NullValue(), new PropertyValue.EnumValue("double", "nan"),
                new PropertyValue.EnumValue("double", "negativeInfinity"), new PropertyValue.StringValue("double.infinity"))) {
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), bad)), catalog).valid(), bad.toString());
        }
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), adapter(), null)), catalog).valid());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), null, number("120"))), catalog).valid());
        var nested = wrapper(StableId.random(), wrapper(StableId.random(), adapter(), number("30")), number("90"));
        assertTrue(new WidgetTreeValidator().validate(document(nested), catalog).valid());
        var expanded = SliverCrossAxisExpandedContractTest.wrapper(StableId.random(), adapter(), SliverCrossAxisExpandedContractTest.integer(2));
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), expanded, number("120"))), catalog).valid());
    }
}

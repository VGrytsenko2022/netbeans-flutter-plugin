package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnimatedOpacityContractTest {
    public static final WidgetTypeId TYPE = AnimatedOpacityWidgetPropertySchema.TYPE;
    public static final SlotName CHILD = new SlotName("child"), CHILDREN = new SlotName("children");
    public static PropertyValue.DoubleValue number(String n) { return new PropertyValue.DoubleValue(new BigDecimal(n)); }
    public static WidgetNode wrapper(StableId id, WidgetNode child, PropertyValue value) {
        return new WidgetNode(id, TYPE, value == null ? Map.of(new PropertyName("durationUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(300000))) : Map.of(new PropertyName("opacity"), value, new PropertyName("durationUs"), new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),
                Map.of(CHILD, new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    public static WidgetNode adapter() { return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(), StableId.random()); }
    public static DesignerDocument document(WidgetNode child) { return FlexibleSpaceBarContractTest.doc(new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(child))))); }
    @Test void completeConstructorUsesExplicitOpacityAndOptionalChild() {
        var d = BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertEquals(AnimatedOpacityWidgetPropertySchema.properties(), d.properties());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertEquals(List.of("onEnd"),WidgetEventCatalog.eventsFor(d).stream().map(e->e.propertyName().value()).toList());
        var p = d.properties().getFirst(); var slot = d.slots().getFirst();
        assertTrue(p.parameter().required()); assertEquals(number("1"), p.creationDefault().orElseThrow());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE), p.acceptedKinds());
        var semantics = d.properties().get(4);
        assertFalse(semantics.parameter().required()); assertTrue(semantics.creationDefault().isEmpty());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), semantics.acceptedKinds());
        assertEquals(1, d.slots().size()); assertEquals(CHILD, slot.name());
        assertFalse(slot.parameter().required()); assertEquals(5, slot.parameter().order());
        assertEquals(0, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(d));
        assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
        assertEquals(Map.of(new PropertyName("opacity"), number("1"),new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(300000))),
                WidgetNodePrototypeFactory.create(d, StableId.random()).properties());
    }
    @Test void everyChildRespectsBoxAndParentDataContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault(); var d = catalog.find(TYPE).orElseThrow();
        var opacity = catalog.find(new WidgetTypeId("flutter.widgets.Opacity")).orElseThrow();
        for (var child : catalog.definitions()) {
            assertEquals(WidgetPlacementRules.accepts(opacity, opacity.slots().getFirst(), child),
                    WidgetPlacementRules.accepts(d, d.slots().getFirst(), child), child.typeId().value());
        }
        for (var owner : catalog.definitions()) for (var slot : owner.slots()) {
            assertEquals(WidgetPlacementRules.accepts(owner, slot, opacity),
                    WidgetPlacementRules.accepts(owner, slot, d), owner.typeId() + "." + slot.name());
        }
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
            assertTrue(code.contains("const AnimatedOpacity("), code);
            assertTrue(code.contains("opacity:"), code);
            assertEquals(empty, code.contains("child: null"), code);
            assertFalse(code.contains("SliverToBoxAdapter"), code);
            assertEquals(semantics != null, code.contains("alwaysIncludeSemantics:"), code);
        }
    }
    @Test void everyCurveAndDurationAndTypedReferenceRetainTheirGeneratedArguments() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(long us:List.of(0L,1L,300000L,9007199254740991L)){
            var base=wrapper(StableId.random(),adapter(),number("0.5"));
            var props=new LinkedHashMap<>(base.properties());
            props.put(new PropertyName("curve"),new PropertyValue.StringValue(curve));
            props.put(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(us)));
            var doc=document(new WidgetNode(base.id(),TYPE,props,base.slots()));
            assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
            var code=new DartRegionGenerator().generate(doc,catalog).generated().orElseThrow().build().payload();
            assertTrue(code.contains("curve: Curves."+curve),code);
            assertTrue(code.contains("duration: const Duration(microseconds: "+us+")"),code);
        }
        for(String name:List.of("curve","durationUs","onEnd")){
            var base=wrapper(StableId.random(),adapter(),number("1"));
            var props=new LinkedHashMap<>(base.properties());
            props.put(new PropertyName(name),new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_custom",Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()));
            var doc=document(new WidgetNode(base.id(),TYPE,props,base.slots()));
            assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
            assertTrue(new DartRegionGenerator().generate(doc,catalog).successful());
        }
        for(var bad:List.<PropertyValue>of(new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.NullValue(),number("0.5"))){
            var base=wrapper(StableId.random(),adapter(),number("1"));var props=new LinkedHashMap<>(base.properties());
            props.put(new PropertyName("durationUs"),bad);
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(base.id(),TYPE,props,base.slots())),catalog).valid());
        }
    }
    @Test void invalidValuesAndSliverChildrenAreRejected() {
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
        var box = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow(), StableId.random());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(StableId.random(), box, number("1"))), catalog).valid());
    }
}


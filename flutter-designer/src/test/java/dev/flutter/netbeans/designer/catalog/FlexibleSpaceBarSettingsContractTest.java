package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlexibleSpaceBarSettingsContractTest {
    static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    static final WidgetDefinition D = C.find(FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE).orElseThrow();
    static PropertyName p(String n) { return new PropertyName(n); }
    static PropertyValue.DoubleValue n(double v) { return new PropertyValue.DoubleValue(BigDecimal.valueOf(v)); }
    static WidgetNode child() { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
            Map.of(p("data"),new PropertyValue.StringValue("Inherited child")),Map.of()); }
    static WidgetNode node(Map<PropertyName,PropertyValue> values) {
        var properties=new LinkedHashMap<>(WidgetNodePrototypeFactory.create(D,StableId.random()).properties());
        properties.putAll(values);
        return new WidgetNode(StableId.random(),D.typeId(),properties,Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child())));
    }
    static DesignerDocument doc(WidgetNode node) { return FlexibleSpaceBarContractTest.doc(node); }
    static boolean valid(WidgetNode node) { return new WidgetTreeValidator().validate(doc(node),C).valid(); }
    @Test void completeConstructorRequiredDefaultsAndWrapperContract() {
        assertEquals(List.of("toolbarOpacity","minExtent","maxExtent","currentExtent","isScrolledUnder","hasLeading"),
                D.properties().stream().map(v->v.name().value()).toList());
        assertTrue(D.constConstructor());assertTrue(D.namedConstructor().isEmpty());assertTrue(D.traits().isEmpty());
        assertTrue(WidgetEventCatalog.eventsFor(D).isEmpty());
        for(var capability:WidgetCapability.values())assertTrue(BuiltInWidgetCapabilityCatalog.supports(D,capability));
        var prototype=WidgetNodePrototypeFactory.create(D,StableId.random());
        assertEquals(Map.of(p("toolbarOpacity"),n(1),p("minExtent"),n(56),p("maxExtent"),n(200),p("currentExtent"),n(200)),prototype.properties());
        assertFalse(valid(prototype)); // A detached wrapper is not a valid document until Child is supplied.
        assertEquals("child",WidgetPlacementRules.requiredWrapperSlot(D).orElseThrow().name().value());
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(D));assertTrue(valid(node(Map.of())));
        assertFalse(WidgetPlacementRules.accepts(D,D.slots().getFirst(),C.find(new WidgetTypeId("flutter.widgets.SliverToBoxAdapter")).orElseThrow()));
    }
    @Test void nativeExtentOrderingAndEqualZeroBoundariesAreMirrored() {
        for(double min:new double[]{0,56,200})for(double max:new double[]{0,56,200})for(double current:new double[]{0,56,200})
            assertEquals(min<=current && current<=max,valid(node(Map.of(p("minExtent"),n(min),p("maxExtent"),n(max),p("currentExtent"),n(current)))));
        for(String key:List.of("toolbarOpacity","minExtent","maxExtent","currentExtent")) {
            assertFalse(valid(node(Map.of(p(key),n(-1)))));
            assertFalse(valid(node(Map.of(p(key),new PropertyValue.NullValue()))));
            var original=node(Map.of());var props=new LinkedHashMap<>(original.properties());props.remove(p(key));
            assertFalse(valid(new WidgetNode(original.id(),original.type(),props,original.slots())));
        }
        assertTrue(valid(node(Map.of(p("toolbarOpacity"),n(2)))),"Native Settings assertion is >= 0, not <= 1");
        assertTrue(valid(node(Map.of(p("minExtent"),n(1e100),p("currentExtent"),n(1e100),p("maxExtent"),n(1e100)))));
    }
    @Test void nullableFlagsDoNotInventDefaultsAndAllFieldsRoundTrip() throws Exception {
        for(String flag:List.of("isScrolledUnder","hasLeading"))for(PropertyValue v:List.of(new PropertyValue.BooleanValue(true),
                new PropertyValue.BooleanValue(false),new PropertyValue.NullValue())) {
            var widget=node(Map.of(p(flag),v));assertTrue(valid(widget));
            var generated=new DartRegionGenerator().generate(doc(widget),C);assertTrue(generated.successful());
            String source=generated.generated().orElseThrow().build().payload();
            assertTrue(source.contains("const FlexibleSpaceBarSettings("));assertFalse(source.contains("createSettings"));
            assertTrue(source.contains(flag+": "+(v instanceof PropertyValue.NullValue?"null":((PropertyValue.BooleanValue)v).value())));
            var codec=new FdDocumentCodec();var document=doc(widget);
            assertEquals(document,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(document))).document());
        }
    }
}


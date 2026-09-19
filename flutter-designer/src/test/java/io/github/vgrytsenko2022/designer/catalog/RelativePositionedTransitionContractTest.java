package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class RelativePositionedTransitionContractTest {
    public static final WidgetTypeId TYPE=RelativePositionedTransitionWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child"),CHILDREN=new SlotName("children");
    public static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static PropertyValue.DoubleValue number(String n){return new PropertyValue.DoubleValue(new BigDecimal(n));}
    public static WidgetNode adapter(){return WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());}
    public static DesignerDocument doc(WidgetNode child){return AnimatedPositionedContractTest.doc(child);}
    public static WidgetNode node(){
        var p=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());
        return new WidgetNode(p.id(),TYPE,p.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(adapter())));
    }
    static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var copy=new LinkedHashMap<>(n.properties());copy.putAll(p);return new WidgetNode(n.id(),TYPE,copy,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){var r=new DartRegionGenerator().generate(doc(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void completeConstructorAndStackPlacementMatrix(){
        var d=C.find(TYPE).orElseThrow();assertTrue(d.constConstructor());
        assertEquals(List.of("rect","rectLeft","rectTop","rectWidth","rectHeight","size","sizeWidth","sizeHeight"),d.properties().stream().map(p->p.name().value()).toList());
        assertTrue(d.properties().stream().allMatch(p->p.parameter().required()));assertEquals(8,d.slots().getFirst().parameter().order());
        assertEquals(1,d.slots().getFirst().minChildren());assertEquals(1,d.slots().getFirst().maxChildren());
        assertEquals(CHILD,WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow().name());assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        var old=C.find(AnimatedPositionedWidgetPropertySchema.TYPE).orElseThrow();
        for(var owner:C.definitions())for(var slot:owner.slots())
            assertEquals(WidgetPlacementRules.accepts(owner,slot,old),WidgetPlacementRules.accepts(owner,slot,d),owner.typeId()+"."+slot.name());
        for(var child:C.definitions())assertEquals(WidgetPlacementRules.accepts(old,old.slots().getFirst(),child),WidgetPlacementRules.accepts(d,d.slots().getFirst(),child));
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
    }
    @Test void signedPhysicalRectangleProductRoundTripsAndGeneratesStoppedRect()throws Exception{
        for(String l:List.of("-10","0","10.5","400"))for(String t:List.of("-20","0","20.25"))
        for(String r:List.of("-30","0","30.75","500"))for(String b:List.of("-40","0","40.125")){
            var p=new LinkedHashMap<PropertyName,PropertyValue>();
            var values=List.of(l,t,r,b);for(int i=0;i<4;i++)p.put(new PropertyName(RelativePositionedTransitionWidgetPropertySchema.RECT_FIELDS.get(i)),number(values.get(i)));
            var n=with(node(),p);var document=doc(n);assertTrue(new WidgetTreeValidator().validate(document,C).valid());
            var codec=new FdDocumentCodec();assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
            var output=generated(n);var dart=output.build().payload();
            assertTrue(dart.contains("const RelativePositionedTransition("),dart);
            assertTrue(dart.contains("rect: const AlwaysStoppedAnimation<Rect?>(const Rect.fromLTWH("+values.stream().map(v->v.contains(".")?v:v+".0").collect(java.util.stream.Collectors.joining(", "))+"))"),dart);
            for(String edge:RelativePositionedTransitionWidgetPropertySchema.RECT_FIELDS)assertFalse(dart.contains(edge+":"));
            assertTrue(output.symbolOccurrences().stream().anyMatch(s->s.id().endsWith(":stopped-nullable-rect-animation")));
            assertTrue(output.symbolOccurrences().stream().anyMatch(s->s.id().endsWith(":stopped-nullable-rect-type")));
            assertTrue(dart.contains("size: const Size(48.0, 48.0)"), dart);
        }
    }
    @Test void sourceShapesRetainExactAnimationEvidenceAndInactiveLocalValues()throws Exception{
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/rects.dart"):Optional.empty(),
                member?"Rects":"rect",member?Optional.of("animation"):Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
            var n=with(node(),Map.of(new PropertyName("rect"),ref,new PropertyName("rectWidth"),number("-99")));
            var out=generated(n);assertFalse(out.build().payload().contains("AlwaysStoppedAnimation"));
            assertFalse(out.build().payload().contains("-99.0"));assertFalse(out.build().payload().contains("const RelativePositionedTransition("));
            assertTrue(out.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Animation<Rect?>")));
            var codec=new FdDocumentCodec();var document=doc(n);assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
        }
    }
    @Test void nullAnimationValueReferenceSizeModesAndFiniteLocalOffsets() {
        var nil=with(node(),Map.of(new PropertyName("rect"),new PropertyValue.StringValue("null")));
        assertTrue(generated(nil).build().payload().contains("AlwaysStoppedAnimation<Rect?>(null)"));
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_size",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        var sized=with(nil,Map.of(new PropertyName("size"),ref));
        var output=generated(sized);
        assertTrue(output.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals("Size")));
        assertFalse(output.build().payload().contains("sizeWidth:"));
        for(String value:List.of("-100","0","100.25")) {
            var local=with(node(),Map.of(new PropertyName("sizeWidth"),number(value),new PropertyName("sizeHeight"),number(value)));
            assertTrue(new WidgetTreeValidator().validate(doc(local),C).valid());generated(local);
        }
        var overflow=with(node(),Map.of(new PropertyName("rectLeft"),number("1e308"),new PropertyName("rectWidth"),number("1e308")));
        assertFalse(new WidgetTreeValidator().validate(doc(overflow),C).valid());
        var ignored=with(overflow,Map.of(new PropertyName("rect"),new PropertyValue.StringValue("null")));
        assertTrue(new WidgetTreeValidator().validate(doc(ignored),C).valid());
        var sizeOverflow=with(node(),Map.of(new PropertyName("rectLeft"),number("-1e308"),new PropertyName("rectWidth"),number("0"),new PropertyName("sizeWidth"),number("1e308")));
        assertFalse(new WidgetTreeValidator().validate(doc(sizeOverflow),C).valid());
    }
    @Test void requiredFieldsChildClosedDomainsAndNoInventedArguments(){
        var n=node();
        for(String name:List.of("rect","rectLeft","rectTop","rectWidth","rectHeight","size","sizeWidth","sizeHeight")){
            var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(name));
            assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(name),new PropertyValue.NullValue()))),C).valid());
        }
        for(String edge:RelativePositionedTransitionWidgetPropertySchema.RECT_FIELDS)
        for(var bad:List.<PropertyValue>of(number("1e309"),number("-1e309"),new PropertyValue.StringValue("3"),
            new PropertyValue.IntegerValue(new BigInteger("9007199254740992")),new PropertyValue.BooleanValue(true))){
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(edge),bad))),C).valid());
        }
        for(var bad:List.<PropertyValue>of(number("1"),new PropertyValue.StringValue("Rect.fill"),new PropertyValue.BooleanValue(true)))
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName("rect"),bad))),C).valid());
        assertFalse(new WidgetTreeValidator().validate(doc(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
        for(String extra:List.of("durationUs","curve","onEnd","textDirection","left","width","animation"))
            assertFalse(new WidgetTreeValidator().validate(doc(with(n,Map.of(new PropertyName(extra),number("1")))),C).valid());
    }
}

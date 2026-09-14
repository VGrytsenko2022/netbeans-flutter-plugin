package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AnimatedPositionedContractTest {
    public static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static final SlotName CHILD=new SlotName("child"), CHILDREN=new SlotName("children");
    public static WidgetNode adapter(){return WidgetNodePrototypeFactory.create(C.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());}
    public static PropertyValue.DoubleValue number(String s){return new PropertyValue.DoubleValue(new BigDecimal(s));}
    public static WidgetNode node(WidgetTypeId type){var n=WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random());return new WidgetNode(n.id(),type,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(adapter())));}
    public static DesignerDocument doc(WidgetNode n){return FlexibleSpaceBarContractTest.doc(new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Stack"),Map.of(),Map.of(CHILDREN,new WidgetSlot.ListSlot(List.of(n)))));}
    public static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var values=new LinkedHashMap<>(n.properties());values.putAll(p);return new WidgetNode(n.id(),n.type(),values,n.slots());}
    static boolean valid(WidgetNode n){return new WidgetTreeValidator().validate(doc(n),C).valid();}
    static GeneratedDartRegions generated(WidgetNode n){var result=new DartRegionGenerator().generate(doc(n),C);assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();}
    @Test void completeConstructorsRequiredChildEventsAndEveryPlacement() {
        for(var type:AnimatedPositionedWidgetPropertySchema.TYPES){
            var d=C.find(type).orElseThrow();boolean rect=type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE);
            assertEquals(!rect,d.constConstructor());assertEquals(rect?8:9,d.properties().size());
            assertEquals(rect?Optional.of("fromRect"):Optional.empty(),d.namedConstructor());
            assertEquals(AnimatedPositionedWidgetPropertySchema.fields(type).stream().map(AnimatedPositionedWidgetPropertySchema.Field::name).toList(),d.properties().stream().map(p->p.name().value()).toList());
            assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
            assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,WidgetPlacementRules.creationMode(d));
            var child=d.slots().getFirst();assertTrue(child.parameter().required());assertEquals(1,child.minChildren());
            for(var owner:C.definitions())for(var slot:owner.slots())
                assertEquals(owner.typeId().value().equals("flutter.widgets.Stack")&&slot.name().equals(CHILDREN),WidgetPlacementRules.accepts(owner,slot,d),owner.typeId().value()+"."+slot.name());
            for(var candidate:C.definitions()){
                var plain=C.find(new WidgetTypeId("flutter.widgets.Padding")).orElseThrow();
                assertEquals(WidgetPlacementRules.accepts(plain,plain.slots().getFirst(),candidate),WidgetPlacementRules.accepts(d,child,candidate));
            }
            var event=WidgetEventCatalog.eventsFor(d).getFirst();assertEquals("onEnd",event.propertyName().value());assertEquals("VoidCallback",event.callbackType());assertTrue(event.nullableCallback());
            assertFalse(valid(WidgetNodePrototypeFactory.create(d,StableId.random())));
        }
    }
    @Test void everyAxisCombinationAndNullableTransitionsRoundTrip() throws Exception {
        for(var type:List.of(AnimatedPositionedWidgetPropertySchema.TYPE,AnimatedPositionedWidgetPropertySchema.DIRECTIONAL_TYPE))
        for(int mask=0;mask<64;mask++){
            var n=node(type);var p=new LinkedHashMap<>(n.properties());var names=AnimatedPositionedWidgetPropertySchema.axisFields(type);
            for(int i=0;i<6;i++)p.put(new PropertyName(names.get(i)),(mask&(1<<i))==0?new PropertyValue.NullValue():number("-2.5"));
            n=new WidgetNode(n.id(),type,p,n.slots());
            boolean allowed=!((mask&21)==21 || (mask&42)==42);assertEquals(allowed,valid(n),type+" "+mask);
            if(!allowed)continue;
            var codec=new FdDocumentCodec();var document=doc(n);
            assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
            var code=generated(n).build().payload();
            assertTrue(code.contains("const "+(AnimatedPositionedWidgetPropertySchema.directional(type)?"AnimatedPositionedDirectional":"AnimatedPositioned")+"("),code);
            assertTrue(code.contains("duration: const Duration(microseconds: 300000)"),code);
            for(int i=0;i<6;i++)assertTrue(code.contains(names.get(i)+": "+((mask&(1<<i))==0?"null":"-2.5")),code);
        }
    }
    @Test void allCurvesDurationBoundariesFromRectAndReferencesKeepExactTypes() {
        for(var type:AnimatedPositionedWidgetPropertySchema.TYPES){
            for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()){
                var n=with(node(type),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve),new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.ZERO)));
                var code=generated(n).build().payload();assertTrue(code.contains("curve: Curves."+curve),code);
                if(type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE)){
                    assertTrue(code.contains("AnimatedPositioned.fromRect("),code);assertTrue(code.contains("rect: const Rect.fromLTWH(0.0, 0.0, 48.0, 48.0)"),code);
                    assertFalse(code.contains("rectLeft:"));assertFalse(code.contains("const AnimatedPositioned.fromRect"));
                }
            }
            var types=new LinkedHashMap<String,String>(Map.of("curve","Curve","durationUs","Duration","onEnd","VoidCallback"));
            if(type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE))types.put("rect","Rect");
            else for(String name:AnimatedPositionedWidgetPropertySchema.axisFields(type))types.put(name,"double?");
            for(var field:types.entrySet())for(boolean imported:List.of(false,true))for(boolean factory:List.of(false,true)){
                var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:sample/refs.dart"):Optional.empty(),"Bindings",Optional.of("value"),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
                var r=generated(with(node(type),Map.of(new PropertyName(field.getKey()),ref)));
                assertTrue(r.symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(v->v.expectedDartType().equals(field.getValue())),field.toString());
            }
        }
    }
    @Test void localRectRejectsNonfiniteDerivedEdgesButNotNegativeSizes() {
        var n = node(AnimatedPositionedWidgetPropertySchema.RECT_TYPE);
        for (var pair : List.of(List.of("rectLeft", "rectWidth"), List.of("rectTop", "rectHeight"))) {
            assertFalse(valid(with(n, Map.of(new PropertyName(pair.get(0)), number("1e308"), new PropertyName(pair.get(1)), number("1e308")))));
            assertTrue(valid(with(n, Map.of(new PropertyName(pair.get(0)), number("1e308"), new PropertyName(pair.get(1)), number("-1e308")))));
        }
    }
    @Test void durationAndNumericDomainsRejectWrongShapesWithoutWideningNullability() {
        for(var type:AnimatedPositionedWidgetPropertySchema.TYPES){
            for(PropertyValue bad:List.of(number("0.1"),new PropertyValue.NullValue(),new PropertyValue.IntegerValue(BigInteger.valueOf(-1)),new PropertyValue.IntegerValue(new BigInteger("9007199254740992"))))
                assertFalse(valid(with(node(type),Map.of(new PropertyName("durationUs"),bad))));
            for(long us:List.of(0L,9007199254740991L))assertTrue(valid(with(node(type),Map.of(new PropertyName("durationUs"),new PropertyValue.IntegerValue(BigInteger.valueOf(us))))));
            String field=type.equals(AnimatedPositionedWidgetPropertySchema.RECT_TYPE)?"rectWidth":"width";
            for(PropertyValue bad:List.of(number("1e309"),new PropertyValue.StringValue("1"),new PropertyValue.IntegerValue(new BigInteger("9007199254740992"))))
                assertFalse(valid(with(node(type),Map.of(new PropertyName(field),bad))));
            assertTrue(valid(with(node(type),Map.of(new PropertyName(field),number("-40")))));
        }
    }
}

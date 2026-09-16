package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SnackBarContractTest {
    static WidgetNode node(boolean action) { return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
            .find(action?SnackBarWidgetPropertySchema.ACTION:SnackBarWidgetPropertySchema.TYPE).orElseThrow(),StableId.random()); }
    static WidgetNode with(WidgetNode n,String p,PropertyValue v) { return DialogContractTest.with(n,p,v); }
    static PropertyValue ref(){return BottomSheetContractTest.ref();}
    static WidgetNode floating(WidgetNode n) {return with(n,"behavior",new PropertyValue.EnumValue("SnackBarBehavior","floating"));}
    @Test void everyNativeArgumentAndSafeCreation() {
        for(boolean action:List.of(false,true)) {
            var n=node(action);var def=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
            assertEquals(action?7:39,def.properties().size());assertEquals(action?0:2,def.slots().size());assertTrue(def.constConstructor());
            assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(def));
            assertTrue(WidgetPlacementRules.requiredWrapperSlot(def).isEmpty());
            assertEquals(action?7:20,def.properties().stream().filter(p->!CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())).count()+def.slots().size());
            var code=DialogContractTest.dart(n);
            assertTrue(code.contains(action?"onPressed: () {}":"AlwaysStoppedAnimation<double>(1.0)"),code);
            assertTrue(code.contains(action?"label: 'Action'":"content:"));
            var events=WidgetEventCatalog.eventsFor(def);assertEquals(1,events.size());
            assertEquals(action,events.getFirst().sdkRequired());
            assertEquals(action?"onPressed":"onVisible",WidgetEventCatalog.defaultEventFor(def).orElseThrow().propertyName().value());
        }
        var content=(WidgetSlot.SingleSlot)node(false).slots().get(new SlotName("content"));assertTrue(content.child().isPresent());
        var action=node(true);DialogContractTest.rejected(with(action,"onPressed",new PropertyValue.NullValue()));
        DialogContractTest.rejected(with(action,"label",new PropertyValue.NullValue()));
    }
    @Test void allSourceTypesHaveStrictProofAndAllShapesGenerate() {
        for(boolean action:List.of(false,true))for(var p:SnackBarWidgetPropertySchema.properties(action))
            for(var constraint:p.constraints())if(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues expected) {
                var n=with(floatingIfNeeded(node(action),p.name().value()),p.name().value(),ref());
                var generated=DialogContractTest.generated(n);
                assertTrue(generated.symbolOccurrences().stream().anyMatch(o->o.staticTypeRequirement()
                    .filter(t->t.expectedDartType().equals(expected.expectedDartType())).isPresent()),p.name().value());
            }
        for(String shape:CardWidgetPropertySchema.shapeKinds()) {
            var n=with(node(false),"shapeKind",new PropertyValue.StringValue(shape));
            for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))n=with(n,p,DialogContractTest.value(p,true));
            assertTrue(DialogContractTest.dart(n).contains("shape:"));DialogContractTest.rejected(with(n,"shape",new PropertyValue.NullValue()));
        }
        assertTrue(DialogContractTest.dart(with(node(false),"durationUs",new PropertyValue.IntegerValue(BigInteger.valueOf(-1)))).contains("Duration(microseconds: -1)"));
    }
    static WidgetNode floatingIfNeeded(WidgetNode n,String p){return Set.of("width","margin").contains(p)?floating(n):n;}
    @Test void invalidGeometryTypesAndSlotsFailClosed() {
        var n=node(false);var width=new PropertyValue.IntegerValue(BigInteger.TEN);
        DialogContractTest.rejected(with(n,"width",width));
        var good=with(floating(n),"width",width);assertTrue(DialogContractTest.dart(good).contains("width: 10"));
        DialogContractTest.rejected(with(good,"margin",ref()));
        assertTrue(DialogContractTest.dart(with(good,"margin",new PropertyValue.NullValue())).contains("margin: null"));
        for(String p:List.of("elevation","width","actionOverflowThreshold","animation")) {
            DialogContractTest.rejected(with(floating(n),p,new PropertyValue.IntegerValue(BigInteger.valueOf(-1))));
        }
        for(String p:List.of("actionOverflowThreshold","animation"))DialogContractTest.rejected(with(n,p,new PropertyValue.DoubleValue(BigDecimal.valueOf(1.1))));
        for(String p:List.of("durationUs","clipBehavior"))DialogContractTest.rejected(with(n,p,new PropertyValue.NullValue()));
        var slots=new LinkedHashMap<>(n.slots());slots.put(new SlotName("content"),WidgetSlot.SingleSlot.empty());
        DialogContractTest.rejected(new WidgetNode(n.id(),n.type(),n.properties(),slots));
        slots=new LinkedHashMap<>(n.slots());slots.put(new SlotName("action"),n.slots().get(new SlotName("content")));
        DialogContractTest.rejected(new WidgetNode(n.id(),n.type(),n.properties(),slots));
        slots.put(new SlotName("action"),WidgetSlot.SingleSlot.of(node(true)));
        assertTrue(DialogContractTest.dart(new WidgetNode(n.id(),n.type(),n.properties(),slots)).contains("SnackBarAction("));
    }
}

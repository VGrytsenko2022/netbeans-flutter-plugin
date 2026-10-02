package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BottomSheetContractTest {
    static WidgetNode node() { return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
            .find(BottomSheetWidgetPropertySchema.TYPE).orElseThrow(), StableId.random()); }
    static WidgetNode with(WidgetNode n,String name,PropertyValue v) { return DialogContractTest.with(n,name,v); }
    static PropertyValue.DartObjectReferenceValue ref() { return new PropertyValue.DartObjectReferenceValue(
            Optional.empty(),"projectValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()); }
    @Test void completeConstructorAndSafeDefaults() {
        var n=node();var def=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        assertEquals(37,def.properties().size());assertEquals(1,def.slots().size());assertTrue(def.constConstructor());
        assertEquals(16,def.properties().stream().filter(p->!CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())).count());
        String code=DialogContractTest.dart(n);
        for(String part:List.of("BottomSheet(", "enableDrag: false", "showDragHandle: false", "onClosing: () {}", "builder: (context) => const SizedBox.shrink()")) assertTrue(code.contains(part),code);
        for(String p:List.of("onClosing","builder"))DialogContractTest.rejected(with(n,p,new PropertyValue.NullValue()));
        var events=WidgetEventCatalog.eventsFor(def);
        assertEquals(4,events.size());assertEquals(3,events.stream().filter(e->e.kind()==WidgetEventDescriptor.Kind.EVENT).count());
        var end=events.stream().filter(e->e.propertyName().value().equals("onDragEnd")).findFirst().orElseThrow();
        assertEquals("BottomSheetDragEndHandler?",end.callbackType());
        assertTrue(end.signature().parameters().toString().contains("isClosing"));
        assertTrue(WidgetEventCatalog.defaultEventFor(def).orElseThrow().propertyName().value().equals("onClosing"));
    }
    @Test void controllerAndBuilderRelationshipsFailClosed() {
        for(String p:List.of("enableDrag","showDragHandle")) {
            DialogContractTest.rejected(with(node(),p,new PropertyValue.BooleanValue(true)));
            assertTrue(DialogContractTest.dart(with(with(node(),"animationController",ref()),p,new PropertyValue.BooleanValue(true))).contains("animationController: projectValue"));
        }
        var child=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        var n=node();
        n=new WidgetNode(n.id(),n.type(),n.properties(),Map.of(new SlotName("child"),new WidgetSlot.SingleSlot(Optional.of(child))));
        String code=DialogContractTest.dart(n);
        assertTrue(code.contains("builder: (context) =>"));assertTrue(code.contains("Text("));assertFalse(code.contains("child:"));
        DialogContractTest.rejected(with(n,"builder",ref()));
    }
    @Test void allShapesAndTypedSourcesHaveEvidence() {
        for(String shape:CardWidgetPropertySchema.shapeKinds()){
            var n=with(node(),"shapeKind",new PropertyValue.StringValue(shape));
            for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))
                    n=with(n,p,DialogContractTest.value(p,true));
            assertTrue(DialogContractTest.dart(n).contains("shape:"));
            DialogContractTest.rejected(with(n,"shape",new PropertyValue.NullValue()));
        }
        for(var p:BottomSheetWidgetPropertySchema.properties())for(var c:p.constraints())
            if(c instanceof PropertyValueConstraint.DartObjectReferenceValues expected) {
                assertTrue(DialogContractTest.generated(with(node(),p.name().value(),ref())).symbolOccurrences().stream()
                        .anyMatch(o->o.staticTypeRequirement().filter(r->r.expectedDartType().equals(expected.expectedDartType())).isPresent()),p.name().value());
            }
        assertTrue(DialogContractTest.dart(with(node(),"dragHandleSize",new PropertyValue.SizeValue(BigDecimal.valueOf(36),BigDecimal.valueOf(5)))).contains("Size("));
        DialogContractTest.rejected(with(node(),"elevation",new PropertyValue.IntegerValue(BigInteger.valueOf(-1))));
    }
}

package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ChipContractTest {
    static WidgetNode node(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(ChipWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    @Test void all27Arguments170RowsThreeSlotsSeedAndEvent(){
        var n=node();var d=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        assertEquals(170,d.properties().size());assertEquals(27,ChipWidgetPropertySchema.DIRECT.size()+d.slots().size());assertTrue(d.constConstructor());
        assertEquals(n,WidgetNodePrototypeFactory.create(d,n.id()));assertTrue(n.properties().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(d));
        assertEquals("onDeleted",WidgetEventCatalog.defaultEventFor(d).orElseThrow().propertyName().value());
        var code=DialogContractTest.dart(n);assertTrue(code.contains("Chip("));assertTrue(code.contains("'Chip'"));assertFalse(code.contains("onPressed:"));
    }
    @Test void sourcesHaveExactStaticProofAndNullIsRetained(){
        for(var p:ChipWidgetPropertySchema.properties()){
            for(var c:p.constraints())if(c instanceof PropertyValueConstraint.DartObjectReferenceValues ref){
                var n=node();
                if(p.name().value().startsWith("mouseCursor")&&!Set.of("mouseCursor","mouseCursorDefault").contains(p.name().value()))
                    n=DialogContractTest.with(n,"mouseCursorDefault",new PropertyValue.StringValue("basic"));
                var out=DialogContractTest.generated(DialogContractTest.with(n,p.name().value(),BottomSheetContractTest.ref()));
                assertTrue(out.symbolOccurrences().stream().anyMatch(o->o.staticTypeRequirement().filter(t->t.expectedDartType().equals(ref.expectedDartType())).isPresent()),p.name().value());
            }
            if(p.acceptedKinds().contains(PropertyValueKind.NULL)){
                var n=DialogContractTest.with(node(),p.name().value(),new PropertyValue.NullValue());
                assertNotNull(DialogContractTest.generated(n),p.name().value());
            }
        }
    }
    @Test void animationDataIsNonConstAndEveryNestedFieldIsGenerated(){
        var n=node();
        for(String part:ChipWidgetPropertySchema.ANIMATIONS)for(String name:ChipWidgetPropertySchema.animationLeaves(ChipWidgetPropertySchema.animationPrefix(part)))
            n=DialogContractTest.with(n,name,name.endsWith("Us")?new PropertyValue.IntegerValue(BigInteger.valueOf(200000)):new PropertyValue.StringValue("easeIn"));
        var code=DialogContractTest.dart(n);assertTrue(code.contains("ChipAnimationStyle("));assertFalse(code.contains("const ChipAnimationStyle"));
        for(String part:ChipWidgetPropertySchema.ANIMATIONS)assertTrue(code.contains(part+": const AnimationStyle("));
        assertFalse(code.contains("const Chip("));assertTrue(code.contains("reverseDuration: const Duration("));
    }
    @Test void familyConflictsBoundsAndRequiredLabelFailClosed(){
        for(var family:ChipWidgetPropertySchema.families().entrySet()){
            var n=DialogContractTest.with(node(),family.getKey(),new PropertyValue.NullValue());
            String leaf=family.getValue().getFirst();
            var p=ChipWidgetPropertySchema.properties().stream().filter(f->f.name().value().equals(leaf)).findFirst().orElseThrow();
            PropertyValue value=p.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)?BottomSheetContractTest.ref()
                    :p.acceptedKinds().contains(PropertyValueKind.COLOR)?new PropertyValue.ColorValue(0xff123456L)
                    :p.acceptedKinds().contains(PropertyValueKind.BOOLEAN)?new PropertyValue.BooleanValue(true)
                    :p.acceptedKinds().contains(PropertyValueKind.STRING)?new PropertyValue.StringValue(leaf.equals("shapeKind")?"circle":"basic")
                    :new PropertyValue.DoubleValue(BigDecimal.ONE);
            DialogContractTest.rejected(DialogContractTest.with(n,leaf,value));
        }
        for(String n:List.of("elevation","chipAnimationStyleEnableAnimationDurationUs"))DialogContractTest.rejected(DialogContractTest.with(node(),n,new PropertyValue.IntegerValue(BigInteger.valueOf(-1))));
        for(String n:List.of("autofocus","clipBehavior"))DialogContractTest.rejected(DialogContractTest.with(node(),n,new PropertyValue.NullValue()));
        var n=node();var slots=new LinkedHashMap<>(n.slots());slots.put(new SlotName("label"),WidgetSlot.SingleSlot.empty());
        DialogContractTest.rejected(new WidgetNode(n.id(),n.type(),n.properties(),slots));
        DialogContractTest.rejected(DialogContractTest.with(node(),"mouseCursorHovered",new PropertyValue.StringValue("click")));
    }
}

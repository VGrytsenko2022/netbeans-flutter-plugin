package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MaterialBannerContractTest {
    static WidgetNode node(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(MaterialBannerWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    @Test void all18Arguments46PropertiesAndThreeSeededSlots() {
        var n=node();var def=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        assertEquals(46,def.properties().size());assertEquals(3,def.slots().size());assertTrue(def.constConstructor());
        assertEquals(18,def.properties().stream().filter(p->AlertDialogWidgetPropertySchema.styleFamily(p.name()).isEmpty()).count()+3);
        assertTrue(n.properties().isEmpty());assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,WidgetPlacementRules.creationMode(def));
        var content=((WidgetSlot.SingleSlot)n.slots().get(new SlotName("content"))).child().orElseThrow();
        var action=((WidgetSlot.ListSlot)n.slots().get(new SlotName("actions"))).children().getFirst();
        assertEquals(TextWidgetPropertySchema.TEXT_TYPE,content.type());assertEquals(new WidgetTypeId("flutter.material.TextButton"),action.type());
        assertEquals(n,WidgetNodePrototypeFactory.create(def,n.id()));
        var event=WidgetEventCatalog.defaultEventFor(def).orElseThrow();
        assertEquals("onVisible",event.propertyName().value());assertFalse(event.sdkRequired());
        var code=DialogContractTest.dart(n);assertTrue(code.contains("MaterialBanner("));assertTrue(code.contains("TextButton("));assertFalse(code.contains("animation:"));
    }
    @Test void allSourcesRetainExactTypeProofAndNullableFields() {
        for(var p:MaterialBannerWidgetPropertySchema.properties()) {
            for(var c:p.constraints())if(c instanceof PropertyValueConstraint.DartObjectReferenceValues ref){
                var generated=DialogContractTest.generated(DialogContractTest.with(node(),p.name().value(),BottomSheetContractTest.ref()));
                assertTrue(generated.symbolOccurrences().stream().anyMatch(o->o.staticTypeRequirement().filter(t->t.expectedDartType().equals(ref.expectedDartType())).isPresent()),p.name().value());
            }
            if(p.acceptedKinds().contains(PropertyValueKind.NULL))
                assertTrue(DialogContractTest.dart(DialogContractTest.with(node(),p.name().value(),new PropertyValue.NullValue())).contains(p.name().value()+": null"));
        }
        assertTrue(DialogContractTest.dart(DialogContractTest.with(node(),"animation",new PropertyValue.DoubleValue(new BigDecimal("0.5")))).contains("AlwaysStoppedAnimation<double>(0.5)"));
        var local=DialogContractTest.with(node(),"contentTextStyleFontSize",new PropertyValue.DoubleValue(BigDecimal.TEN));
        assertTrue(DialogContractTest.dart(local).contains("contentTextStyle: const TextStyle("));
        DialogContractTest.rejected(DialogContractTest.with(local,"contentTextStyle",new PropertyValue.NullValue()));
    }
    @Test void invalidBoundsEmptyRequiredSlotsAndSliversAreRejected() {
        for(String p:List.of("animation","elevation","minActionBarHeight"))
            DialogContractTest.rejected(DialogContractTest.with(node(),p,new PropertyValue.IntegerValue(BigInteger.valueOf(-1))));
        DialogContractTest.rejected(DialogContractTest.with(node(),"animation",new PropertyValue.DoubleValue(new BigDecimal("1.01"))));
        for(String p:List.of("forceActionsBelow","overflowAlignment","minActionBarHeight"))
            DialogContractTest.rejected(DialogContractTest.with(node(),p,new PropertyValue.NullValue()));
        for(String p:List.of("padding","margin","leadingPadding")){
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(),p,new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE))).contains("EdgeInsetsDirectional"));
            DialogContractTest.rejected(DialogContractTest.with(node(),p,new PropertyValue.EdgeInsetsValue(BigDecimal.ONE.negate(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO)));
        }
        for(String name:List.of("content","actions")){
            var n=node();var slots=new LinkedHashMap<>(n.slots());slots.put(new SlotName(name),name.equals("content")?WidgetSlot.SingleSlot.empty():new WidgetSlot.ListSlot(List.of()));
            DialogContractTest.rejected(new WidgetNode(n.id(),n.type(),n.properties(),slots));
        }
        var def=BuiltInWidgetCatalog.getDefault().find(MaterialBannerWidgetPropertySchema.TYPE).orElseThrow();
        var sliver=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.SliverList")).orElseThrow();
        for(var slot:def.slots())assertFalse(WidgetPlacementRules.accepts(def,slot,sliver));
    }
}

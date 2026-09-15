package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimpleDialogContractTest {
    static WidgetNode node(boolean option) { return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
            .find(option ? SimpleDialogWidgetPropertySchema.OPTION_TYPE : SimpleDialogWidgetPropertySchema.TYPE).orElseThrow(), StableId.random()); }
    @Test void completeConstructorsSlotsAndOneNullableVoidEvent() {
        for(boolean option:List.of(false,true)) {
            var n=node(option); var d=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
            assertEquals(option?3:98,d.properties().size()); assertEquals(option?1:2,d.slots().size());
            assertEquals(option?4:17,d.properties().stream().filter(p -> !CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())
                && AlertDialogWidgetPropertySchema.styleFamily(p.name()).isEmpty()).count()+d.slots().size());
            assertTrue(d.constConstructor()); assertTrue(n.properties().isEmpty());
            assertTrue(DialogContractTest.dart(n).contains(option?"SimpleDialogOption(":"SimpleDialog("));
            assertEquals(option?1:0,WidgetEventCatalog.eventsFor(d).size());
            if(option) {
                var event=WidgetEventCatalog.eventsFor(d).getFirst();
                assertEquals("onPressed",event.propertyName().value());
                assertTrue(DialogContractTest.dart(DialogContractTest.with(n,"onPressed",new PropertyValue.NullValue())).contains("onPressed: null"));
            }
            for(var slot:d.slots()){assertEquals(0,slot.minChildren());assertFalse(slot.parameter().required());}
        }
    }
    @Test void allShapesStylesAndSourceRequirements() {
        for(String shape:CardWidgetPropertySchema.shapeKinds()) {
            var n=DialogContractTest.with(node(false),"shapeKind",new PropertyValue.StringValue(shape));
            for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))
                    n=DialogContractTest.with(n,p,DialogContractTest.value(p,true));
            assertTrue(DialogContractTest.dart(n).contains("shape:"));
            DialogContractTest.rejected(DialogContractTest.with(n,"shape",new PropertyValue.NullValue()));
        }
        for(String family:AlertDialogWidgetPropertySchema.styleFamilies()) {
            assertEquals(31,AlertDialogWidgetPropertySchema.localStyleProperties(family).size());
            var n=DialogContractTest.with(node(false),family+"FontSize",new PropertyValue.DoubleValue(BigDecimal.valueOf(19)));
            assertTrue(DialogContractTest.dart(n).contains(family+": const TextStyle("));
            DialogContractTest.rejected(DialogContractTest.with(n,family,new PropertyValue.NullValue()));
        }
        for(boolean option:List.of(false,true)) {
            var d=BuiltInWidgetCatalog.getDefault().find(node(option).type()).orElseThrow();
            for(var p:d.properties())for(var c:p.constraints())if(c instanceof PropertyValueConstraint.DartObjectReferenceValues ref) {
                var v=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
                assertTrue(DialogContractTest.generated(DialogContractTest.with(node(option),p.name().value(),v)).symbolOccurrences().stream()
                    .anyMatch(o->o.staticTypeRequirement().filter(r->r.expectedDartType().equals(ref.expectedDartType())).isPresent()),p.name().value());
            }
        }
    }
    @Test void physicalDirectionalNonnullablePaddingAndKey() {
        var directional=new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE);
        for(String p:List.of("titlePadding","contentPadding")) {
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(false),p,directional)).contains("EdgeInsetsDirectional"));
            DialogContractTest.rejected(DialogContractTest.with(node(false),p,new PropertyValue.NullValue()));
        }
        DialogContractTest.rejected(DialogContractTest.with(node(false),"insetPadding",directional));
        DialogContractTest.rejected(DialogContractTest.with(node(true),"padding",directional));
        for(boolean option:List.of(false,true)) {
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(option),"key",new PropertyValue.StringValue("choice"))).contains("ValueKey"));
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(option),option?"padding":"insetPadding",new PropertyValue.NullValue())).contains(": null"));
        }
    }
}

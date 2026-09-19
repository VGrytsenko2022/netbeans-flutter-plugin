package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AlertDialogContractTest {
    static WidgetNode node(boolean adaptive){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(adaptive?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    @Test void bothExactConstructorsSlotsNoInjectedValuesAndNoInventedEvents(){
        for(boolean adaptive:List.of(false,true)){
            var n=node(adaptive);var d=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
            assertEquals(adaptive?111:107,d.properties().size());assertEquals(4,d.slots().size());
            assertEquals(adaptive?32:28,d.properties().stream().filter(p->!CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())&&AlertDialogWidgetPropertySchema.styleFamily(p.name()).isEmpty()).count()+4);
            assertTrue(d.constConstructor());assertTrue(n.properties().isEmpty());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
            assertTrue(DialogContractTest.dart(n).contains("AlertDialog"+(adaptive?".adaptive":"")+"("));
            for(var slot:d.slots()){assertEquals(0,slot.minChildren());assertFalse(slot.parameter().required());}
            if(adaptive)assertTrue(DialogContractTest.generated(n).symbolOccurrences().stream().anyMatch(o->o.symbolName().equals("adaptive")));
        }
    }
    @Test void completeLocalStylesShapesAndStrictNullableSources(){
        for(boolean adaptive:List.of(false,true)){
            for(String shape:CardWidgetPropertySchema.shapeKinds()){
                var n=DialogContractTest.with(node(adaptive),"shapeKind",new PropertyValue.StringValue(shape));
                for(String p:CardWidgetPropertySchema.builtInShapePropertyNames())if(CardWidgetPropertySchema.isShapeDetailProperty(p)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(p,shape))
                    n=DialogContractTest.with(n,p,DialogContractTest.value(p,true));
                assertTrue(DialogContractTest.dart(n).contains("shape:"));DialogContractTest.rejected(DialogContractTest.with(n,"shape",new PropertyValue.NullValue()));
            }
            for(String family:AlertDialogWidgetPropertySchema.styleFamilies()){
                assertEquals(31,AlertDialogWidgetPropertySchema.localStyleProperties(family).size());
                var n=DialogContractTest.with(node(adaptive),family+"FontSize",new PropertyValue.DoubleValue(BigDecimal.valueOf(19)));
                assertTrue(DialogContractTest.dart(n).contains(family+": const TextStyle("),DialogContractTest.dart(n));
                DialogContractTest.rejected(DialogContractTest.with(n,family,new PropertyValue.NullValue()));
            }
            var d=BuiltInWidgetCatalog.getDefault().find(node(adaptive).type()).orElseThrow();
            for(var p:d.properties())for(var constraint:p.constraints())if(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues ref){
                var value=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
                var result=DialogContractTest.generated(DialogContractTest.with(node(adaptive),p.name().value(),value));
                assertTrue(result.symbolOccurrences().stream().anyMatch(o->o.staticTypeRequirement().filter(r->r.expectedDartType().equals(ref.expectedDartType())).isPresent()),p.name().value());
            }
        }
    }
    @Test void signedOverflowSpacingPaddingNullabilityAndAdaptiveOnlyFields(){
        for(boolean adaptive:List.of(false,true)){
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(adaptive),"actionsOverflowButtonSpacing",new PropertyValue.DoubleValue(BigDecimal.valueOf(-3)))).contains("-3"));
            var inset=new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE);
            for(String p:List.of("iconPadding","titlePadding","contentPadding","actionsPadding","buttonPadding"))assertTrue(DialogContractTest.dart(DialogContractTest.with(node(adaptive),p,inset)).contains("EdgeInsetsDirectional"));
            DialogContractTest.rejected(DialogContractTest.with(node(adaptive),"insetPadding",inset));
            if(adaptive)DialogContractTest.rejected(DialogContractTest.with(node(true),"insetPadding",new PropertyValue.NullValue()));
            else assertTrue(DialogContractTest.dart(DialogContractTest.with(node(false),"insetPadding",new PropertyValue.NullValue())).contains("insetPadding: null"));
        }
        for(String p:List.of("scrollController","actionScrollController","insetAnimationDurationUs","insetAnimationCurve"))
            assertTrue(BuiltInWidgetCatalog.getDefault().find(AlertDialogWidgetPropertySchema.TYPE).orElseThrow().property(new PropertyName(p)).isEmpty());
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())
            assertTrue(DialogContractTest.dart(DialogContractTest.with(node(true),"insetAnimationCurve",new PropertyValue.StringValue(curve))).contains("Curves."+curve));
    }
}

package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DialogContractTest {
    static WidgetNode dialog(boolean full){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(full?DialogWidgetPropertySchema.FULLSCREEN_TYPE:DialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue value){var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(name),value);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static GeneratedDartRegions generated(WidgetNode n){
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow();
    }
    static String dart(WidgetNode n){return generated(n).build().payload();}
    static void rejected(WidgetNode n){assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault()).successful());}
    @Test void exactArgumentsDefaultsCapabilitiesAndOptionalChildForBothConstructors()throws Exception {
        for(boolean full:List.of(false,true)){
            var node=dialog(full);var d=BuiltInWidgetCatalog.getDefault().find(node.type()).orElseThrow();
            assertEquals(full?5:34,d.properties().size());assertEquals(1,d.slots().size());assertTrue(d.constConstructor());
            assertEquals(full?6:14,d.properties().stream().filter(p->!CardWidgetPropertySchema.builtInShapePropertyNames().contains(p.name().value())).count()+d.slots().size());
            assertTrue(d.properties().stream().noneMatch(p->p.parameter().required()||p.creationDefault().isPresent()));
            assertTrue(node.properties().isEmpty());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
            assertEquals(Set.of(WidgetCapability.CANVAS,WidgetCapability.CREATE,WidgetCapability.DND,WidgetCapability.PROPERTIES),BuiltInWidgetCapabilityCatalog.capabilities(d));
            assertTrue(dart(node).contains("Dialog"+(full?".fullscreen":"")+"("),dart(node));
            var child=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
            node=new WidgetNode(node.id(),node.type(),node.properties(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
            assertTrue(dart(node).contains("child:"));var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(node);
            assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            assertTrue(WidgetPlacementRules.accepts(d,d.slots().getFirst(),BuiltInWidgetCatalog.getDefault().find(child.type()).orElseThrow()));
        }
        for(String name:List.of("elevation","shadowColor","surfaceTintColor","insetPadding","clipBehavior","shape","shapeKind","alignment","constraints"))
            assertTrue(BuiltInWidgetCatalog.getDefault().find(DialogWidgetPropertySchema.FULLSCREEN_TYPE).orElseThrow().property(new PropertyName(name)).isEmpty());
    }
    @Test void allRolesCurvesKeysAndDurationsGenerateNativeArgumentsAndExactSymbols(){
        assertEquals(33,DialogWidgetPropertySchema.ROLES.size());assertEquals(43,ExpansionTileWidgetPropertySchema.curvePresets().size());
        for(boolean full:List.of(false,true)){
            for(String role:DialogWidgetPropertySchema.ROLES){
                var output=generated(with(dialog(full),"semanticsRole",new PropertyValue.EnumValue("SemanticsRole",role)));
                assertTrue(output.build().payload().contains("SemanticsRole."+role));
                assertTrue(output.symbolOccurrences().stream().anyMatch(o->o.symbolName().equals("SemanticsRole")&&o.libraryUri().equals("dart:ui")));
            }
            for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())assertTrue(dart(with(dialog(full),"insetAnimationCurve",new PropertyValue.StringValue(curve))).contains("Curves."+curve));
            assertTrue(dart(with(dialog(full),"key",new PropertyValue.StringValue("dialog-key"))).contains("ValueKey('dialog-key')"));
            for(long duration:new long[]{0,100000,9007199254740991L}){
                var output=generated(with(dialog(full),"insetAnimationDurationUs",new PropertyValue.IntegerValue(BigInteger.valueOf(duration))));
                assertTrue(output.build().payload().contains("insetAnimationDuration: const Duration(microseconds: "+duration+")"));
                assertFalse(output.build().payload().contains("insetAnimationDurationUs:"));
                assertTrue(output.symbolOccurrences().stream().anyMatch(o->o.symbolName().equals("Duration")&&o.libraryUri().equals("dart:core")));
            }
            if(full)assertTrue(generated(dialog(full)).symbolOccurrences().stream().anyMatch(o->o.symbolName().equals("fullscreen")));
        }
    }
    @Test void allTenShapesWithEveryApplicableLeafRoundTripAndRejectConflicts()throws Exception {
        for(String kind:CardWidgetPropertySchema.shapeKinds())for(boolean directional:List.of(false,true)){
            var n=with(dialog(false),"shapeKind",new PropertyValue.StringValue(kind));
            for(String name:CardWidgetPropertySchema.builtInShapePropertyNames())
                if(CardWidgetPropertySchema.isShapeDetailProperty(name)&&CardWidgetPropertySchema.shapePropertyAppliesToKind(name,kind))n=with(n,name,value(name,directional));
            var source=dart(n);assertTrue(source.contains("shape:"));assertFalse(source.contains("shapeKind:"));assertFalse(source.contains("shapeRadius:"));
            var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(n);assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            rejected(with(n,"shape",new PropertyValue.NullValue()));
        }
        rejected(with(with(dialog(false),"shapeKind",new PropertyValue.StringValue("circle")),"shapeRadius",value("shapeRadius",false)));
        rejected(with(with(with(dialog(false),"shapeKind",new PropertyValue.StringValue("star")),"shapePointRounding",new PropertyValue.DoubleValue(new BigDecimal("0.8"))),"shapeValleyRounding",new PropertyValue.DoubleValue(new BigDecimal("0.8"))));
    }
    @Test void nullableValuesPhysicalInsetsAndClosedSourceTypesStayExact(){
        for(String name:List.of("key","backgroundColor","elevation","shadowColor","surfaceTintColor","insetPadding","clipBehavior","shape","alignment","constraints"))
            assertTrue(dart(with(dialog(false),name,new PropertyValue.NullValue())).contains(name+": null"));
        for(String name:List.of("insetAnimationCurve","insetAnimationDurationUs","semanticsRole"))rejected(with(dialog(false),name,new PropertyValue.NullValue()));
        for(String name:List.of("elevation","insetAnimationDurationUs"))rejected(with(dialog(false),name,new PropertyValue.IntegerValue(BigInteger.valueOf(-1))));
        var inset=new PropertyValue.EdgeInsetsValue(BigDecimal.ONE,BigDecimal.TWO,BigDecimal.ONE,BigDecimal.TWO);
        assertTrue(dart(with(dialog(false),"insetPadding",inset)).contains("EdgeInsets.fromLTRB"));
        rejected(with(dialog(false),"insetPadding",new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ONE,BigDecimal.TWO,BigDecimal.ONE,BigDecimal.TWO)));
        for(var entry:Map.of("key","Key?","backgroundColor","Color?","elevation","double?","insetAnimationDurationUs","Duration","insetAnimationCurve","Curve","insetPadding","EdgeInsets?","shape","ShapeBorder?","alignment","AlignmentGeometry?","constraints","BoxConstraints?").entrySet()){
            var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"projectValue",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            var output=generated(with(dialog(false),entry.getKey(),reference));
            // Exact static proof remains attached to the source symbol.
            assertTrue(output.symbolOccurrences().stream().anyMatch(o->o.staticTypeRequirement().filter(r->r.expectedDartType().equals(entry.getValue())).isPresent()),entry.toString());
        }
    }
    static PropertyValue value(String name,boolean directional){
        if(name.equals("shapeRadius")){var a=new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ONE,BigDecimal.TWO);return new PropertyValue.BorderRadiusValue(directional?new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(a,a,a,a):new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(a,a,a,a));}
        return switch(name){case "shapeSideColor"->new PropertyValue.ThemeTokenValue(new ThemeToken("material.colorScheme.outline"));case "shapeSideStyle"->new PropertyValue.EnumValue("BorderStyle","solid");case "shapePoints"->new PropertyValue.DoubleValue(new BigDecimal("5.5"));case "shapeRotation"->new PropertyValue.DoubleValue(new BigDecimal("-22.5"));default->new PropertyValue.DoubleValue(new BigDecimal("0.25"));};
    }
}

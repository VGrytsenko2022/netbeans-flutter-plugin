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
public class AnimatedDefaultTextStyleContractTest {
    public static final WidgetTypeId TYPE=AnimatedDefaultTextStyleWidgetPropertySchema.TYPE;
    public static final SlotName CHILD=new SlotName("child");
    static final WidgetCatalog C=BuiltInWidgetCatalog.getDefault();
    public static PropertyValue.DoubleValue number(String s){return new PropertyValue.DoubleValue(new BigDecimal(s));}
    public static PropertyValue.DartObjectReferenceValue ref(String s){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),s,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    public static WidgetNode node(){var p=WidgetNodePrototypeFactory.create(C.find(TYPE).orElseThrow(),StableId.random());return new WidgetNode(p.id(),TYPE,p.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.of(AnimatedSizeContractTest.adapter())));}
    public static WidgetNode with(WidgetNode n,Map<PropertyName,PropertyValue> p){var all=new LinkedHashMap<>(n.properties());all.putAll(p);return new WidgetNode(n.id(),TYPE,all,n.slots());}
    public static DesignerDocument document(WidgetNode n){return AnimatedSizeContractTest.document(n);}
    static GeneratedDartRegions generate(WidgetNode n){var r=new DartRegionGenerator().generate(document(n),C);assertTrue(r.successful(),r.diagnostics().toString());return r.generated().orElseThrow();}
    @Test void constructorAllLocalFieldsRequiredStyleDurationChildAndEvent(){
        var d=C.find(TYPE).orElseThrow();assertEquals(44,d.properties().size());assertTrue(d.constConstructor());
        assertEquals(31,d.properties().stream().filter(p->AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(p.name())).count());
        assertEquals(3,d.properties().stream().filter(p->AnimatedDefaultTextStyleWidgetPropertySchema.heightLeaf(p.name())).count());
        assertEquals(1,d.slots().getFirst().minChildren());assertTrue(d.slots().getFirst().parameter().required());
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isPresent());assertTrue(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertEquals("onEnd",WidgetEventCatalog.eventsFor(d).getFirst().propertyName().value());
        var code=generate(node()).build().payload();assertTrue(code.contains("style: const TextStyle("),code);
        assertTrue(code.contains("duration: const Duration(microseconds: 300000)"),code);
        for(String name:List.of("style","durationUs")){
            var n=node();var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(name));
            assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,p,n.slots())),C).valid());
        }
        var n=node();assertFalse(new WidgetTreeValidator().validate(document(new WidgetNode(n.id(),TYPE,n.properties(),Map.of(CHILD,WidgetSlot.SingleSlot.empty()))),C).valid());
    }
    @Test void curvesNullableReferencesAndThemeFallbackHaveExactProof()throws Exception{
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets()){
            var n=with(node(),Map.of(new PropertyName("curve"),new PropertyValue.StringValue(curve)));
            assertTrue(generate(n).build().payload().contains("curve: Curves."+curve));
        }
        for(var e:Map.of("style","TextStyle","textHeightBehavior","TextHeightBehavior?","maxLines","int?","durationUs","Duration","onEnd","VoidCallback").entrySet()){
            var n=with(node(),Map.of(new PropertyName(e.getKey()),ref("_binding")));
            assertTrue(generate(n).symbolOccurrences().stream().flatMap(o->o.staticTypeRequirement().stream()).anyMatch(t->t.expectedDartType().equals(e.getValue())));
            var doc=document(n);var codec=new FdDocumentCodec();assertEquals(doc,((FdDecodeResult.Current)codec.decode(codec.encode(doc))).document());
        }
        for(String field:List.of("maxLines","textAlign","textHeightBehavior","onEnd"))
            assertTrue(generate(with(node(),Map.of(new PropertyName(field),new PropertyValue.NullValue()))).build().payload().contains((field.equals("onEnd")?"onEnd":field)+": null"));
        var themed=generate(with(node(),Map.of(new PropertyName("styleThemeTextStyle"),new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge"))))).build().payload();
        assertTrue(themed.contains("?? const TextStyle()"),themed);
    }
    @Test void localProjectionAndRelationsAreStrict(){
        var n=with(node(),Map.of(new PropertyName("styleFontSize"),number("24"),new PropertyName("styleColor"),new PropertyValue.ColorValue(0xff112233L),
            new PropertyName("styleFontWeight"),new PropertyValue.EnumValue("FontWeight","w700"),new PropertyName("textHeightApplyFirstAscent"),new PropertyValue.BooleanValue(false)));
        var code=generate(n).build().payload();for(String part:List.of("fontSize: 24.0","FontWeight.w700","applyHeightToFirstAscent: false","TextHeightBehavior("))assertTrue(code.contains(part),code);
        assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("style"),ref("_style")))),C).valid());
        for(var val:List.<PropertyValue>of(ref("_height"),new PropertyValue.NullValue()))
            assertFalse(new WidgetTreeValidator().validate(document(with(n,Map.of(new PropertyName("textHeightBehavior"),val))),C).valid());
        assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("stylePackage"),new PropertyValue.StringValue("fonts")))),C).valid());
        for(var bad:List.<PropertyValue>of(number("1.5"),new PropertyValue.IntegerValue(BigInteger.ZERO),new PropertyValue.IntegerValue(new BigInteger("9007199254740992"))))
            assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName("maxLines"),bad))),C).valid());
        for(String name:List.of("style","durationUs","curve","softWrap"))assertFalse(new WidgetTreeValidator().validate(document(with(node(),Map.of(new PropertyName(name),new PropertyValue.NullValue()))),C).valid());
    }
}

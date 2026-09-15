package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.validation.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import java.util.*;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverFloatingHeaderContractTest {
    static final WidgetTypeId TYPE = SliverFloatingHeaderWidgetPropertySchema.TYPE;
    static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    static PropertyName p(String name) { return new PropertyName(name); }
    static PropertyValue.IntegerValue i(long value) { return new PropertyValue.IntegerValue(BigInteger.valueOf(value)); }
    static PropertyValue.StringValue s(String value) { return new PropertyValue.StringValue(value); }
    static PropertyValue nil() { return new PropertyValue.NullValue(); }
    static WidgetNode header(Map<PropertyName,PropertyValue> values) {
        var seed = WidgetNodePrototypeFactory.create(CATALOG.find(TYPE).orElseThrow(), StableId.random());
        return new WidgetNode(seed.id(), TYPE, values, seed.slots());
    }
    static DesignerDocument doc(WidgetNode node) { return SliverCrossAxisExpandedContractTest.document(node); }
    static String generated(WidgetNode node) {
        return new DartRegionGenerator().generate(doc(node), CATALOG).generated().orElseThrow().build().payload();
    }
    @Test void completeConstructorHasStrictRequiredBoxAndExplicitStableCreationSeed() {
        var d = CATALOG.find(TYPE).orElseThrow();
        assertEquals(6,d.properties().size());assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());
        var slot=d.slots().getFirst();assertTrue(slot.parameter().required());assertEquals(1,slot.minChildren());
        assertEquals(new SlotAcceptance.AnyWidget(),slot.acceptance());
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(d));
        assertTrue(WidgetPlacementRules.requiredWrapperSlot(d).isEmpty());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        for(var capability:WidgetCapability.values()) assertTrue(BuiltInWidgetCapabilityCatalog.supports(d,capability));
        var id=StableId.random();var seed=WidgetNodePrototypeFactory.create(d,id);
        assertEquals(seed,WidgetNodePrototypeFactory.create(d,id));
        var child=((WidgetSlot.SingleSlot)seed.slots().get(new SlotName("child"))).child().orElseThrow();
        assertNotEquals(id,child.id());assertEquals(new WidgetTypeId("flutter.widgets.SizedBox"),child.type());
        assertEquals(Map.of(p("width"),i(48),p("height"),i(48)),child.properties());
        assertTrue(new WidgetTreeValidator().validate(doc(seed),CATALOG).valid());
        for(var parent:CATALOG.definitions())for(var target:parent.slots())
            assertEquals(target.acceptance().equals(new SlotAcceptance.HasTrait(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT)),
                    WidgetPlacementRules.accepts(parent,target,d));
        for(var type:CATALOG.definitions()) assertEquals(!WidgetPlacementRules.isSliverWidget(type)
                && !Set.of("flutter.widgets.Expanded","flutter.widgets.Flexible","flutter.widgets.Spacer","flutter.widgets.LayoutId").contains(type.typeId().value()) && !WidgetPlacementRules.isStackPositionedWidget(type),
                WidgetPlacementRules.accepts(d,slot,type));
    }
    @Test void allLocalStylePresenceCombinationsAndSnapModesRoundTripAndGenerateExactArguments() throws Exception {
        var local=SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE;
        for(int mask=0;mask<16;mask++)for(String mode:List.of("unset","null","overlay","scroll")) {
            var values=new LinkedHashMap<PropertyName,PropertyValue>();
            for(int j=0;j<4;j++)if((mask&(1<<j))!=0) values.put(p(local.get(j)),local.get(j).endsWith("Us")?i(j==0?-17:0):s("easeInOut"));
            if(!mode.equals("unset"))values.put(p("snapMode"),mode.equals("null")?nil():new PropertyValue.EnumValue("FloatingHeaderSnapMode",mode));
            var node=header(values);var document=doc(node);var codec=new FdDocumentCodec();
            assertTrue(new WidgetTreeValidator().validate(document,CATALOG).valid());
            assertEquals(document,((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
            var code=generated(node);assertTrue(code.contains("SliverFloatingHeader("),code);
            assertEquals(mask!=0,code.contains("animationStyle:"),code);
            if((mask&1)!=0)assertTrue(code.contains("Duration(microseconds: -17)"),code);
            if((mask&4)!=0)assertTrue(code.contains("reverseDuration: const Duration(microseconds: 0)"),code);
            assertFalse(code.contains("animationStyleDurationUs:"),code);
            if(mode.equals("scroll"))assertTrue(code.contains("FloatingHeaderSnapMode.scroll"),code);
        }
    }
    @Test void everyCurveAndWholeReferenceAccessBranchIsRetainedWithoutRawDart() {
        for(String curve:ExpansionTileWidgetPropertySchema.curvePresets())for(String field:List.of("animationStyleCurve","animationStyleReverseCurve"))
            assertTrue(generated(header(Map.of(p(field),s(curve)))).contains("Curves."+curve));
        assertTrue(generated(header(Map.of(p("animationStyle"),s("noAnimation")))).contains("AnimationStyle.noAnimation"));
        for(String field:List.of("animationStyle","animationStyleDurationUs","animationStyleReverseDurationUs","animationStyleCurve","animationStyleReverseCurve"))
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
                var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:app/style.dart"):Optional.empty(),
                        "values",member?Optional.of("item"):Optional.empty(), factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                assertTrue(generated(header(Map.of(p(field),ref))).contains(factory?"()":"values"));
                assertTrue(generated(header(Map.of(p(field),nil()))).contains("null"));
            }
    }
    @Test void conflictsMissingChildInvalidKindsAndRequiredChildRemovalAreRejected() throws Exception {
        var validator=new WidgetTreeValidator();
        for(String local:SliverFloatingHeaderWidgetPropertySchema.LOCAL_STYLE)
            assertFalse(validator.validate(doc(header(Map.of(p("animationStyle"),nil(),p(local),nil()))),CATALOG).valid());
        var node=header(Map.of());
        for(var slots:List.<Map<SlotName,WidgetSlot>>of(Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()),
                Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(SliverCrossAxisExpandedContractTest.adapter()))))
            assertFalse(validator.validate(doc(new WidgetNode(node.id(),TYPE,Map.of(),slots)),CATALOG).valid());
        for(var value:List.<PropertyValue>of(s("bad"),new PropertyValue.EnumValue("FloatingHeaderSnapMode","invalid"),i(1)))
            assertFalse(validator.validate(doc(header(Map.of(p("snapMode"),value))),CATALOG).valid());
        assertFalse(validator.validate(doc(header(Map.of(p("animationStyleCurve"),s("raw()")))),CATALOG).valid());
    }
}

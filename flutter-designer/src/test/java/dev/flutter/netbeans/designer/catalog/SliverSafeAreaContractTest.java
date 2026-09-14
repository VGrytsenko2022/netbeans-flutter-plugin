package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.codec.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class SliverSafeAreaContractTest {
    public static final WidgetTypeId TYPE=SliverSafeAreaWidgetPropertySchema.TYPE;
    public static final SlotName SLIVER=new SlotName("sliver"),SLIVERS=new SlotName("slivers");
    public static WidgetNode adapter(){return SliverCrossAxisExpandedContractTest.adapter();}
    public static DesignerDocument document(WidgetNode child){return SliverCrossAxisExpandedContractTest.document(child);}
    public static WidgetNode wrapper(StableId id,WidgetNode child,Map<PropertyName,PropertyValue> props){
        return new WidgetNode(id,TYPE,props,Map.of(SLIVER,new WidgetSlot.SingleSlot(Optional.ofNullable(child))));
    }
    public static PropertyValue.EdgeInsetsValue minimum(){return new PropertyValue.EdgeInsetsValue(
        new BigDecimal("-3.5"),new BigDecimal("7.25"),new BigDecimal("11"),new BigDecimal("2"));}
    @Test void completeConstructorUsesNativeDefaultsAndRequiredSliver(){
        var d=BuiltInWidgetCatalog.getDefault().find(TYPE).orElseThrow();
        assertEquals(SliverSafeAreaWidgetPropertySchema.properties(),d.properties());
        assertEquals(List.of("left","top","right","bottom","minimum"),d.properties().stream().map(p->p.name().value()).toList());
        assertTrue(d.constConstructor());assertTrue(d.namedConstructor().isEmpty());assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
        for(var p:d.properties()){assertFalse(p.parameter().required());assertTrue(p.creationDefault().isEmpty());}
        assertEquals(List.of(0,1,2,3,4),d.properties().stream().map(p->p.parameter().order()).toList());
        var insets=assertInstanceOf(PropertyValueConstraint.EdgeInsetsValues.class,d.properties().getLast().constraints().getFirst());
        assertFalse(insets.nonNegative());assertFalse(insets.directionalAllowed());
        assertEquals(1,d.slots().size());var slot=d.slots().getFirst();
        assertTrue(slot.parameter().required());assertEquals(5,slot.parameter().order());assertEquals(1,slot.minChildren());assertEquals(1,slot.maxChildren());
        assertEquals(SLIVER,WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow().name());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,WidgetPlacementRules.creationMode(d));
        assertTrue(WidgetNodePrototypeFactory.create(d,StableId.random()).properties().isEmpty());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
    }
    @Test void everyParentAndChildRespectsSliverAndParentDataContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault(); var d = catalog.find(TYPE).orElseThrow();
        for (var parent : catalog.definitions()) for (var slot : parent.slots()) {
            boolean allowed = slot.acceptance() instanceof SlotAcceptance.HasTrait t && t.trait().equals(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT);
            assertEquals(allowed, WidgetPlacementRules.accepts(parent, slot, d), parent.typeId() + "." + slot.name());
        }
        for (var child : catalog.definitions())
            assertEquals(WidgetPlacementRules.isSliverWidget(child) && !child.typeId().equals(SliverCrossAxisExpandedWidgetPropertySchema.TYPE),
                    WidgetPlacementRules.accepts(d, d.slots().getFirst(), child), child.typeId().value());
    }

    @Test void allOmittedAndExplicitSideCombinationsAndPhysicalMinimumRoundTripAndGenerate() throws Exception{
        var catalog=BuiltInWidgetCatalog.getDefault();var names=List.of("left","top","right","bottom");
        for(int mask=0;mask<81;mask++)for(boolean inset:List.of(false,true)){
            var props=new LinkedHashMap<PropertyName,PropertyValue>();int remaining=mask;
            for(String name:names){int state=remaining%3;remaining/=3;if(state!=0)props.put(new PropertyName(name),new PropertyValue.BooleanValue(state==2));}
            if(inset)props.put(new PropertyName("minimum"),minimum());
            var doc=document(wrapper(StableId.random(),adapter(),props));
            assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
            var codec=new FdDocumentCodec();assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
            var generated=new DartRegionGenerator().generate(doc,catalog);assertTrue(generated.successful(),generated.diagnostics().toString());
            var code=generated.generated().orElseThrow().build().payload();
            assertTrue(code.contains("const SliverSafeArea("),code);assertTrue(code.contains("sliver:"),code);
            for(String name:names)assertEquals(props.containsKey(new PropertyName(name)),code.contains(name+":"),code);
            assertEquals(inset,code.contains("minimum:"),code);
            if(inset)assertTrue(code.contains("minimum: const EdgeInsets.fromLTRB(-3.5, 7.25, 11.0, 2.0)"),code);
            assertFalse(code.contains("maintainBottomViewPadding:"),code);
        }
    }
    @Test void invalidValuesAndMissingOrIncompatibleChildrenFailClosed(){
        var catalog=BuiltInWidgetCatalog.getDefault();var validator=new WidgetTreeValidator();
        for(String name:List.of("left","top","right","bottom"))for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("false"),minimum()))
            assertFalse(validator.validate(document(wrapper(StableId.random(),adapter(),Map.of(new PropertyName(name),bad))),catalog).valid());
        for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("EdgeInsets.zero"),
                new PropertyValue.EdgeInsetsDirectionalValue(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),
                new PropertyValue.EdgeInsetsValue(new BigDecimal("1e309"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO)))
            assertFalse(validator.validate(document(wrapper(StableId.random(),adapter(),Map.of(new PropertyName("minimum"),bad))),catalog).valid());
        assertFalse(validator.validate(document(wrapper(StableId.random(),null,Map.of())),catalog).valid());
        assertFalse(validator.validate(document(new WidgetNode(StableId.random(),TYPE,Map.of(),Map.of())),catalog).valid());
        assertFalse(validator.validate(document(wrapper(StableId.random(),adapter(),Map.of(new PropertyName("maintainBottomViewPadding"),new PropertyValue.BooleanValue(true)))),catalog).valid());
        var box=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        assertFalse(validator.validate(document(wrapper(StableId.random(),box,Map.of())),catalog).valid());
        var nested=wrapper(StableId.random(),wrapper(StableId.random(),adapter(),Map.of()),Map.of());
        assertTrue(validator.validate(document(nested),catalog).valid());
    }
}


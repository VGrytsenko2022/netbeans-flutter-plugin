package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.WidgetTreeValidator;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SliverVisibilityContractTest {
    public static final WidgetTypeId TYPE = SliverVisibilityWidgetPropertySchema.TYPE, MAINTAIN = SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE;
    public static final SlotName SLIVER = new SlotName("sliver"), REPLACEMENT = new SlotName("replacementSliver"), SLIVERS = new SlotName("slivers");
    public static WidgetNode adapter() { return SliverCrossAxisExpandedContractTest.adapter(); }
    public static DesignerDocument document(WidgetNode child) { return SliverCrossAxisExpandedContractTest.document(child); }
    public static WidgetNode wrapper(WidgetTypeId type, StableId id, WidgetNode child, WidgetNode replacement, Map<PropertyName,PropertyValue> props) {
        return new WidgetNode(id, type, props, Map.of(SLIVER, new WidgetSlot.SingleSlot(Optional.ofNullable(child)),
                REPLACEMENT, new WidgetSlot.SingleSlot(Optional.ofNullable(replacement))));
    }
    public static Map<PropertyName,PropertyValue> flags(int bits) {
        var result = new LinkedHashMap<PropertyName,PropertyValue>();
        var fields = SliverVisibilityWidgetPropertySchema.FIELDS;
        for (int i=0;i<fields.size();i++) result.put(new PropertyName(fields.get(i).name()), new PropertyValue.BooleanValue((bits & (1 << i)) != 0));
        return result;
    }
    public static boolean valid(int b) {
        return ((b & 4)==0 || (b & 2)!=0) && ((b & 8)==0 || (b & 4)!=0)
                && ((b & 16)==0 || (b & 8)!=0) && ((b & 32)==0 || (b & 8)!=0);
    }
    @Test void completeConstructorsRequiredChildAndAllPlacementContracts() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var type : List.of(TYPE, MAINTAIN)) {
            var d = catalog.find(type).orElseThrow();
            assertEquals(type.equals(MAINTAIN) ? Optional.of("maintain") : Optional.empty(), d.namedConstructor());
            assertEquals(SliverVisibilityWidgetPropertySchema.properties(type.equals(MAINTAIN)), d.properties());
            assertTrue(d.constConstructor()); assertTrue(WidgetEventCatalog.eventsFor(d).isEmpty());
            assertEquals(type.equals(MAINTAIN) ? 1 : 6, d.properties().size());
            for (var p : d.properties()) {
                assertFalse(p.parameter().required()); assertTrue(p.creationDefault().isEmpty());
                assertEquals(Set.of(PropertyValueKind.BOOLEAN),p.acceptedKinds());
            }
            assertEquals(2,d.slots().size());
            assertEquals(SLIVER,WidgetPlacementRules.requiredWrapperSlot(d).orElseThrow().name());
            assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,WidgetPlacementRules.creationMode(d));
            assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
            assertTrue(WidgetNodePrototypeFactory.create(d,StableId.random()).properties().isEmpty());
            for (var parent : catalog.definitions()) for(var slot : parent.slots())
                assertEquals(slot.acceptance() instanceof SlotAcceptance.HasTrait t && t.trait().equals(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT),
                        WidgetPlacementRules.accepts(parent,slot,d),parent.typeId()+"."+slot.name());
            for(var slot:d.slots()) for(var child:catalog.definitions())
                assertEquals(WidgetPlacementRules.isSliverWidget(child) && !child.typeId().equals(SliverCrossAxisExpandedWidgetPropertySchema.TYPE),
                        WidgetPlacementRules.accepts(d,slot,child),type+"."+slot.name()+" <- "+child.typeId());
        }
    }
    @Test void everyMaintenanceCombinationIsValidatedEvenWhenVisibleAndValidBranchesRoundTrip() throws Exception {
        int accepted = 0;
        for (int bits=0;bits<64;bits++) {
            var doc=document(wrapper(TYPE,StableId.random(),adapter(),adapter(),flags(bits)));
            var result=new WidgetTreeValidator().validate(doc,BuiltInWidgetCatalog.getDefault());
            assertEquals(valid(bits),result.valid(),"flags="+bits+" "+result);
            if(valid(bits)) { accepted++; roundTrip(doc); }
        }
        assertEquals(14,accepted);
    }
    @Test void defaultsMaintainAndAllOptionalReplacementStatesGenerateWithoutSyntheticModelChildren() throws Exception {
        for(var type:List.of(TYPE,MAINTAIN)) for(Boolean visible:Arrays.asList(null,false,true)) for(int replacement:List.of(0,1,2)) {
            var props=visible==null ? Map.<PropertyName,PropertyValue>of() : Map.<PropertyName,PropertyValue>of(new PropertyName("visible"),new PropertyValue.BooleanValue(visible));
            var node=wrapper(type,StableId.random(),adapter(),replacement==2?adapter():null,props);
            if(replacement==0)node=new WidgetNode(node.id(),type,props,Map.of(SLIVER,node.slots().get(SLIVER)));
            var code=roundTrip(document(node));
            assertTrue(code.contains(type.equals(MAINTAIN)?"const SliverVisibility.maintain(":"const SliverVisibility("),code);
            assertEquals(visible!=null,code.contains("visible:"),code);
            assertEquals(replacement==2,code.contains("replacementSliver:"),code);
        }
    }
    @Test void nullUnknownFieldsMissingRequiredChildAndBoxBranchesFailClosed() {
        var catalog=BuiltInWidgetCatalog.getDefault();
        for(var type:List.of(TYPE,MAINTAIN)) {
            for(var p:catalog.find(type).orElseThrow().properties()) for(var bad:List.<PropertyValue>of(new PropertyValue.NullValue(),new PropertyValue.StringValue("false"),new PropertyValue.IntegerValue(java.math.BigInteger.ONE)))
                assertFalse(new WidgetTreeValidator().validate(document(wrapper(type,StableId.random(),adapter(),null,Map.of(p.name(),bad))),catalog).valid());
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(type,StableId.random(),null,null,Map.of())),catalog).valid());
            var box=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(type,StableId.random(),adapter(),box,Map.of())),catalog).valid());
            assertFalse(new WidgetTreeValidator().validate(document(wrapper(type,StableId.random(),box,null,Map.of())),catalog).valid());
        }
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(MAINTAIN,StableId.random(),adapter(),null,
            Map.of(new PropertyName("maintainSize"),new PropertyValue.BooleanValue(true)))),catalog).valid());
        assertFalse(new WidgetTreeValidator().validate(document(wrapper(TYPE,StableId.random(),adapter(),null,
            Map.of(new PropertyName("maintainFocusability"),new PropertyValue.BooleanValue(true)))),catalog).valid());
    }
    private static String roundTrip(DesignerDocument doc) throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        assertTrue(new WidgetTreeValidator().validate(doc,catalog).valid());
        var codec=new FdDocumentCodec();
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
        var generated=new DartRegionGenerator().generate(doc,catalog);
        assertTrue(generated.successful(),generated.diagnostics().toString());
        return generated.generated().orElseThrow().build().payload();
    }
}


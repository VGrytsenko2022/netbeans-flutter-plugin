package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimePickerDialogContractTest {
    static WidgetNode picker(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TimePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue value){var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(name),value);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static String dart(WidgetNode n){
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow().build().payload();
    }
    @Test void allFifteenArgumentsHaveExactKindsRequirednessAndDefaults() throws Exception {
        var node=picker();var d=BuiltInWidgetCatalog.getDefault().find(node.type()).orElseThrow();
        assertEquals(Set.of("key","initialTime","cancelText","confirmText","helpText","errorInvalidText","hourLabelText",
                "minuteLabelText","restorationId","initialEntryMode","orientation","onEntryModeChanged","emptyInitialInput"),
                d.properties().stream().map(p->p.name().value()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(13,d.properties().size());assertEquals(2,d.slots().size());assertTrue(d.constConstructor());
        assertEquals(Set.of("initialTime"),d.properties().stream().filter(p->p.parameter().required()).map(p->p.name().value()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Map.of(new PropertyName("initialTime"),new PropertyValue.StringValue("09:00")),node.properties());
        String output=dart(node);assertTrue(output.contains("TimeOfDay(hour: 9, minute: 0)"),output);
        assertFalse(output.contains("onEntryModeChanged:"),output);assertFalse(output.contains("emptyInitialInput:"),output);
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(node);
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
    }
    @Test void all1440MinuteValuesAreAcceptedButOutOfDomainTextIsRejected() {
        for(int hour=0;hour<24;hour++)for(int minute=0;minute<60;minute++){
            var t=TimePickerDialogWidgetPropertySchema.time(String.format(Locale.ROOT,"%02d:%02d",hour,minute));
            assertEquals(hour,t.getHour());assertEquals(minute,t.getMinute());
        }
        for(String bad:List.of("24:00","23:60","-1:00","9:00","09:0","09:00\n","09:00\r"," 09:00","09:00:00","1e1:00","TimeOfDay.now()","٠٩:٠٠","")){
            assertThrows(IllegalArgumentException.class,()->TimePickerDialogWidgetPropertySchema.time(bad),bad);
            assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(with(picker(),"initialTime",new PropertyValue.StringValue(bad))),BuiltInWidgetCatalog.getDefault()).successful(),bad);
        }
        for(String time:List.of("00:00","12:00","23:59"))assertTrue(dart(with(picker(),"initialTime",new PropertyValue.StringValue(time))).contains("TimeOfDay("));
    }
    @Test void nativeEnumsLabelsNullAndBooleanRemainDistinct() {
        for(String mode:List.of("dial","input","dialOnly","inputOnly"))
            assertTrue(dart(with(picker(),"initialEntryMode",new PropertyValue.EnumValue("TimePickerEntryMode",mode))).contains("TimePickerEntryMode."+mode));
        for(String orientation:List.of("portrait","landscape"))
            assertTrue(dart(with(picker(),"orientation",new PropertyValue.EnumValue("Orientation",orientation))).contains("Orientation."+orientation));
        for(boolean value:List.of(false,true))assertTrue(dart(with(picker(),"emptyInitialInput",new PropertyValue.BooleanValue(value))).contains("emptyInitialInput: "+value));
        for(String name:List.of("key","orientation","onEntryModeChanged","cancelText","confirmText","helpText","errorInvalidText","hourLabelText","minuteLabelText","restorationId"))
            assertTrue(dart(with(picker(),name,new PropertyValue.NullValue())).contains(name+": null"));
        for(String name:List.of("cancelText","confirmText","helpText","errorInvalidText","hourLabelText","minuteLabelText","restorationId"))
            assertTrue(dart(with(picker(),name,new PropertyValue.StringValue(""))).contains(name+": ''"));
        for(String name:List.of("initialTime","initialEntryMode","emptyInitialInput"))
            assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(with(picker(),name,new PropertyValue.NullValue())),BuiltInWidgetCatalog.getDefault()).successful(),name);
        var node=picker();assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(new WidgetNode(node.id(),node.type(),Map.of(),node.slots())),BuiltInWidgetCatalog.getDefault()).successful());
    }
    @Test void bothSlotsAcceptOnlyIconsAndPreserveStableIds() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();var base=picker();var d=catalog.find(base.type()).orElseThrow();
        var slots=new LinkedHashMap<SlotName,WidgetSlot>();
        assertEquals(Set.of("switchToInputEntryModeIcon","switchToTimerEntryModeIcon"),d.slots().stream().map(s->s.name().value()).collect(java.util.stream.Collectors.toSet()));
        for(var slot:d.slots()){
            for(var candidate:catalog.definitions())
                assertEquals(candidate.typeId().value().equals("flutter.widgets.Icon"),WidgetPlacementRules.accepts(d,slot,candidate),candidate.typeId().value());
            var icon=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(),StableId.random());
            slots.put(slot.name(),WidgetSlot.SingleSlot.of(icon));
        }
        var node=new WidgetNode(base.id(),base.type(),base.properties(),slots);String output=dart(node);
        for(var name:slots.keySet())assertTrue(output.contains(name.value()+":"),output);
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(node);
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
    }
    @Test void modeEventHasMaterialSignatureAndDoesNotInventTimeSelectionEvents() {
        var d=BuiltInWidgetCatalog.getDefault().find(picker().type()).orElseThrow();
        var events=WidgetEventCatalog.eventsFor(d);assertEquals(1,events.size());var e=events.getFirst();
        assertEquals(WidgetEventDescriptor.Kind.EVENT,e.kind());assertFalse(e.sdkRequired());assertTrue(e.defaultEvent());assertTrue(e.allowsExplicitNull());
        assertTrue(e.createStub("_changed").contains("void _changed(TimePickerEntryMode mode)"));
        assertTrue(e.signature().importUris().contains("package:flutter/material.dart"));
        assertTrue(dart(with(picker(),"onEntryModeChanged",new PropertyValue.StringValue("noop"))).contains("onEntryModeChanged: (_) {}"));
        assertTrue(WidgetEventCatalog.find(d.typeId(),new PropertyName("onChanged")).isEmpty());
    }
}

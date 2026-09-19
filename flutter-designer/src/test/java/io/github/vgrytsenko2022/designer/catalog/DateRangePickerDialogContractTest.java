package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DateRangePickerDialogContractTest {
    static WidgetNode picker(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(DateRangePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue v){return DatePickerDialogContractTest.with(n,name,v);}
    static PropertyValue.StringValue s(String text){return new PropertyValue.StringValue(text);}
    @Test void allTwentyThreeNativeArgumentsAndNullabilityAreCovered() throws Exception {
        var n=picker();var d=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        var names=new HashSet<String>();d.properties().forEach(p->names.add(p.name().value()));d.slots().forEach(slot->names.add(slot.name().value()));
        assertEquals(Set.of("key","initialDateRange","firstDate","lastDate","currentDate","initialEntryMode","helpText","cancelText","confirmText","saveText",
                "errorInvalidRangeText","errorFormatText","errorInvalidText","fieldStartHintText","fieldEndHintText","fieldStartLabelText","fieldEndLabelText",
                "keyboardType","restorationId","switchToInputEntryModeIcon","switchToCalendarEntryModeIcon","selectableDayPredicate","calendarDelegate"),names);
        assertEquals(21,d.properties().size());assertTrue(d.constConstructor());
        assertFalse(d.property(new PropertyName("keyboardType")).orElseThrow().acceptedKinds().contains(PropertyValueKind.NULL));
        var events=WidgetEventCatalog.eventsFor(d);assertEquals(1,events.size());var predicate=events.getFirst();
        assertEquals(WidgetEventDescriptor.Kind.PREDICATE,predicate.kind());assertTrue(predicate.supportsHandlerActions());assertTrue(predicate.allowsExplicitNull());
        assertTrue(predicate.createStub("_allowed").contains("bool _allowed(DateTime day, DateTime? selectedStartDay, DateTime? selectedEndDay)"));
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(with(n,"initialDateRange",s("2024-02-29/2024-03-01")));
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
        for(var slot:d.slots()){assertTrue(slot.acceptance().accepts(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow()));
            assertFalse(slot.acceptance().accepts(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow()));}
    }
    @Test void localRangeIsClosedStrictOrderedAndBounded() {
        for(String text:List.of("2025-02-29/2025-03-01","2024-02-29/2024-02-28","0000-01-01/2024-02-29","2024-02-29","2024-02-29/2024-03-01\n"," 2024-02-29/2024-03-01"))
            assertThrows(IllegalArgumentException.class,()->DateRangePickerDialogWidgetPropertySchema.range(text),text);
        assertEquals(29,DateRangePickerDialogWidgetPropertySchema.range("2000-02-29/2000-02-29").start().getDayOfMonth());
        for(var n:List.of(with(picker(),"firstDate",s("2200-01-01")),with(picker(),"initialDateRange",s("1899-12-31/1900-01-02")),
                with(picker(),"initialDateRange",s("2100-12-31/2101-01-01")),with(picker(),"initialDateRange",s("2024-03-02/2024-03-01")),
                with(picker(),"keyboardType",new PropertyValue.NullValue()),with(picker(),"calendarDelegate",new PropertyValue.NullValue())))
            assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault()).successful());
        var n=with(with(with(picker(),"firstDate",s("2000-02-29")),"lastDate",s("2000-02-29")),"initialDateRange",s("2000-02-29/2000-02-29"));
        assertTrue(DateRangePickerDialogWidgetPropertySchema.relationshipError(n).isEmpty());
    }
    @Test void generationEmitsNativeDateRangeAndExactCoreEndpointProbes() {
        var n=with(with(with(picker(),"initialDateRange",s("2024-02-29/2024-03-01")),"calendarDelegate",s("gregorian")),"keyboardType",s("numberSignedDecimal"));
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());var generated=result.generated().orElseThrow();
        String dart=generated.build().payload();
        assertTrue(dart.contains("DateTimeRange(start: "),dart);assertTrue(dart.contains("DateTime(2024, 2, 29)"),dart);assertTrue(dart.contains("DateTime(2024, 3, 1)"),dart);
        assertFalse(dart.contains("const DateTimeRange"),dart);assertTrue(dart.contains("GregorianCalendarDelegate()"),dart);
        assertTrue(dart.contains("numberWithOptions(signed: true, decimal: true)"),dart);
        assertTrue(generated.imports().payload().contains("import 'dart:core';"));
        assertTrue(DatePickerDialogContractTest.dart(with(n,"initialDateRange",new PropertyValue.NullValue())).contains("initialDateRange: null"));
    }
}

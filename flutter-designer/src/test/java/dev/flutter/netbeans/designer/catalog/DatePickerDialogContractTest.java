package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.generation.*;
import dev.flutter.netbeans.designer.events.*;
import dev.flutter.netbeans.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatePickerDialogContractTest {
    static WidgetNode picker(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(DatePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue value){var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(name),value);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static String dart(WidgetNode n){
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow().build().payload();
    }
    @Test void allTwentyTwoArgumentsHaveExactNativeKindsAndDefaults() throws Exception {
        var n=picker();var d=BuiltInWidgetCatalog.getDefault().find(n.type()).orElseThrow();
        var names=new HashSet<String>();d.properties().forEach(p->names.add(p.name().value()));d.slots().forEach(s->names.add(s.name().value()));
        assertEquals(Set.of("key","initialDate","firstDate","lastDate","currentDate","initialEntryMode","selectableDayPredicate","cancelText","confirmText","helpText","initialCalendarMode","errorFormatText","errorInvalidText","fieldHintText","fieldLabelText","keyboardType","restorationId","onDatePickerModeChange","switchToInputEntryModeIcon","switchToCalendarEntryModeIcon","insetPadding","calendarDelegate"),names);
        assertFalse(d.constConstructor());assertEquals(20,d.properties().size());
        String output=dart(n);assertTrue(output.contains("DateTime(1900, 1, 1)"),output);assertTrue(output.contains("DateTime(2100, 12, 31)"),output);
        assertFalse(output.contains("const DateTime"),output);assertFalse(output.contains("initialDate:"),output);
        var generated=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
        String imports=generated.imports().payload();
        assertTrue(imports.contains("import 'dart:core';\n"),imports);
        assertTrue(imports.contains("import 'dart:core' as "),imports);
        assertEquals(1,imports.lines().filter(line->line.equals("import 'dart:core';")).count());
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(n);
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
    }
    @Test void calendarDatesAreStrictAndInclusiveButDoNotEvaluateCustomCalendars() {
        for(String text:List.of("2025-02-29","1900-02-29","2024-02-30","2024-13-01","0000-01-01","10000-01-01","2024-1-1","2024-01-01\n","2024-01-01\r"," 2024-01-01","2024-01-01T00:00:00","DateTime.now()"))
            assertThrows(IllegalArgumentException.class,()->DatePickerDialogWidgetPropertySchema.date(text),text);
        assertEquals(29,DatePickerDialogWidgetPropertySchema.date("2000-02-29").getDayOfMonth());
        var n=with(with(with(picker(),"firstDate",new PropertyValue.StringValue("2000-02-29")),"lastDate",new PropertyValue.StringValue("2000-02-29")),"initialDate",new PropertyValue.StringValue("2000-02-29"));
        assertTrue(DatePickerDialogWidgetPropertySchema.relationshipError(n).isEmpty());
        var outside=with(n,"initialDate",new PropertyValue.StringValue("2000-03-01"));assertTrue(DatePickerDialogWidgetPropertySchema.relationshipError(outside).isPresent());
        outside=with(outside,"calendarDelegate",new PropertyValue.DartObjectReferenceValue(Optional.empty(),"calendar",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()));
        assertTrue(DatePickerDialogWidgetPropertySchema.relationshipError(outside).isEmpty());
    }
    @Test void iconsAreActualEditableIconSlotsNotAnyWidget() {
        var c=BuiltInWidgetCatalog.getDefault();var d=c.find(picker().type()).orElseThrow();
        for(var slot:d.slots()){assertTrue(slot.acceptance().accepts(c.find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow()));
            assertFalse(slot.acceptance().accepts(c.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow()));}
        var events=WidgetEventCatalog.eventsFor(d);
        assertEquals(2,events.size());
        assertEquals(1,events.stream().filter(e->e.kind()==WidgetEventDescriptor.Kind.EVENT).count());
        assertEquals(1,events.stream().filter(e->e.kind()==WidgetEventDescriptor.Kind.PREDICATE).count());
    }
    @Test void invalidDatesBoundsAndNativeKindsAreRejectedByTheGenerationGate() {
        var base=picker();
        var invalid=List.of(
                with(base,"firstDate",new PropertyValue.StringValue("2025-02-29")),
                with(base,"firstDate",new PropertyValue.StringValue("2200-01-01")),
                with(base,"initialDate",new PropertyValue.StringValue("1899-12-31")),
                with(base,"firstDate",new PropertyValue.NullValue()),
                with(base,"calendarDelegate",new PropertyValue.NullValue()),
                with(base,"insetPadding",new PropertyValue.NullValue()));
        for(var node:invalid) assertFalse(new DartRegionGenerator().generate(
                RotationTransitionContractTest.document(node),BuiltInWidgetCatalog.getDefault()).successful());
        for(String name:DatePickerDialogWidgetPropertySchema.DATES)
            assertFalse(DatePickerDialogWidgetPropertySchema.label(name).contains("Date") &&
                    !DatePickerDialogWidgetPropertySchema.label(name).contains(" "));
        var predicate=WidgetEventCatalog.find(base.type(),new PropertyName("selectableDayPredicate")).orElseThrow();
        assertTrue(predicate.supportsHandlerActions());
        assertTrue(predicate.allowsExplicitNull());
        assertTrue(predicate.createStub("_allowDate").contains("bool _allowDate(DateTime date)"));
        assertTrue(predicate.createStub("_allowDate").contains("return true;"));
    }

    @Test void presetsReferencesAndNullsAreNotQuotedAsExpressions() {
        var n=with(with(with(picker(),"calendarDelegate",new PropertyValue.StringValue("gregorian")),"keyboardType",new PropertyValue.StringValue("numberSignedDecimal")),"onDatePickerModeChange",new PropertyValue.StringValue("noop"));
        String output=dart(n);assertTrue(output.contains("GregorianCalendarDelegate()"),output);assertTrue(output.contains("numberWithOptions(signed: true, decimal: true)"),output);
        assertTrue(output.contains("onDatePickerModeChange: (_) {}"),output);
        assertTrue(dart(with(n,"initialDate",new PropertyValue.NullValue())).contains("initialDate: null"));
    }
}

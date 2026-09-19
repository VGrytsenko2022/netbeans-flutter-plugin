package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.events.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputDatePickerFormFieldContractTest {
    static WidgetNode picker(){return WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(InputDatePickerFormFieldWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());}
    static WidgetNode with(WidgetNode n,String name,PropertyValue value){var p=new LinkedHashMap<>(n.properties());p.put(new PropertyName(name),value);return new WidgetNode(n.id(),n.type(),p,n.slots());}
    static String dart(WidgetNode n){
        var result=new DartRegionGenerator().generate(RotationTransitionContractTest.document(n),BuiltInWidgetCatalog.getDefault());
        assertTrue(result.successful(),result.diagnostics().toString());return result.generated().orElseThrow().build().payload();
    }
    @Test void allSixteenArgumentsHaveExactNativeKindsAndDefaults() throws Exception {
        var node=picker();var definition=BuiltInWidgetCatalog.getDefault().find(node.type()).orElseThrow();
        assertEquals(Set.of("key","initialDate","firstDate","lastDate","onDateSubmitted","onDateSaved",
                "errorFormatText","errorInvalidText","fieldHintText","fieldLabelText","keyboardType","autofocus","acceptEmptyDate","focusNode","selectableDayPredicate","calendarDelegate"),
                definition.properties().stream().map(p->p.name().value()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(16,definition.properties().size());assertTrue(definition.slots().isEmpty());assertFalse(definition.constConstructor());
        assertEquals(Set.of("firstDate","lastDate"),
                definition.properties().stream().filter(p->p.parameter().required()).map(p->p.name().value()).collect(java.util.stream.Collectors.toSet()));
        String output=dart(node);
        assertFalse(output.contains("initialDate:"),output);assertFalse(output.contains("onDateSubmitted:"),output);
        assertTrue(output.contains("DateTime(1900, 1, 1)"),output);assertTrue(output.contains("DateTime(2100, 12, 31)"),output);
        assertFalse(output.contains("onDateSaved:"),output);
        var codec=new FdDocumentCodec();var doc=RotationTransitionContractTest.document(node);
        assertEquals(doc,assertInstanceOf(FdDecodeResult.Current.class,codec.decode(codec.encode(doc))).document());
    }
    @Test void everyKeyboardPresetBooleanAndLabelGeneratesWithoutUndeclaredImports() {
        for(String keyboard:DatePickerDialogWidgetPropertySchema.KEYBOARDS) {
            String source=dart(with(picker(),"keyboardType",new PropertyValue.StringValue(keyboard)));
            assertTrue(source.contains("TextInputType." )||source.contains("TextInputType.numberWithOptions"),source);
        }
        for(String name:List.of("autofocus","acceptEmptyDate"))for(boolean value:List.of(false,true))
            assertTrue(dart(with(picker(),name,new PropertyValue.BooleanValue(value))).contains(name+": "+value));
        for(String name:List.of("errorFormatText","errorInvalidText","fieldHintText","fieldLabelText")) {
            assertTrue(dart(with(picker(),name,new PropertyValue.StringValue(""))).contains(name+": ''"));
            assertTrue(dart(with(picker(),name,new PropertyValue.NullValue())).contains(name+": null"));
        }
    }
    @Test void calendarDatesAreStrictAndInclusiveButDoNotEvaluateCustomCalendars() {
        for(String text:List.of("2025-02-29","1900-02-29","2024-02-30","2024-13-01","0000-01-01","10000-01-01","2024-1-1","2024-01-01\n","2024-01-01\r"," 2024-01-01","2024-01-01T00:00:00","DateTime.now()"))
            assertThrows(IllegalArgumentException.class,()->DatePickerDialogWidgetPropertySchema.date(text),text);
        assertEquals(29,DatePickerDialogWidgetPropertySchema.date("2000-02-29").getDayOfMonth());
        var n=with(with(with(picker(),"firstDate",new PropertyValue.StringValue("2000-02-29")),"lastDate",new PropertyValue.StringValue("2000-02-29")),"initialDate",new PropertyValue.StringValue("2000-02-29"));
        assertTrue(InputDatePickerFormFieldWidgetPropertySchema.relationshipError(n).isEmpty());
        var outside=with(n,"initialDate",new PropertyValue.StringValue("2000-03-01"));assertTrue(InputDatePickerFormFieldWidgetPropertySchema.relationshipError(outside).isPresent());
        outside=with(outside,"calendarDelegate",new PropertyValue.DartObjectReferenceValue(Optional.empty(),"calendar",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty()));
        assertTrue(InputDatePickerFormFieldWidgetPropertySchema.relationshipError(outside).isEmpty());
    }
    @Test void eventsHaveExactRequirednessNullabilityAndDefaultAction() {
        var d=BuiltInWidgetCatalog.getDefault().find(picker().type()).orElseThrow();
        assertEquals(3,WidgetEventCatalog.eventsFor(d).size());
        for(String name:List.of("onDateSubmitted","onDateSaved")){
            var e=WidgetEventCatalog.find(d.typeId(),new PropertyName(name)).orElseThrow();
            assertEquals(WidgetEventDescriptor.Kind.EVENT,e.kind());
            assertFalse(e.sdkRequired());
            assertEquals(name.equals("onDateSubmitted"),e.defaultEvent());
            assertTrue(e.allowsExplicitNull());
            assertTrue(e.createStub("_handle").contains("void _handle(DateTime date)"));
        }
    }
    @Test void invalidDatesBoundsAndNativeKindsAreRejectedByTheGenerationGate() {
        var base=picker();
        var invalid=List.of(
                with(base,"firstDate",new PropertyValue.StringValue("2025-02-29")),
                with(base,"firstDate",new PropertyValue.StringValue("2200-01-01")),
                with(base,"initialDate",new PropertyValue.StringValue("1899-12-31")),
                with(base,"firstDate",new PropertyValue.NullValue()),
                with(base,"calendarDelegate",new PropertyValue.NullValue()),
                with(base,"acceptEmptyDate",new PropertyValue.NullValue()));
        for(var node:invalid) assertFalse(new DartRegionGenerator().generate(
                RotationTransitionContractTest.document(node),BuiltInWidgetCatalog.getDefault()).successful());
        for(String name:DatePickerDialogWidgetPropertySchema.DATES)
            assertFalse(InputDatePickerFormFieldWidgetPropertySchema.label(name).contains("Date") &&
                    !InputDatePickerFormFieldWidgetPropertySchema.label(name).contains(" "));
        var predicate=WidgetEventCatalog.find(base.type(),new PropertyName("selectableDayPredicate")).orElseThrow();
        assertTrue(predicate.supportsHandlerActions());
        assertTrue(predicate.allowsExplicitNull());
        assertTrue(predicate.createStub("_allowDate").contains("bool _allowDate(DateTime date)"));
        assertTrue(predicate.createStub("_allowDate").contains("return true;"));
    }

    @Test void nullableDateSourcesUseAnExactCoreProofWithoutWeakeningBounds() {
        var d=BuiltInWidgetCatalog.getDefault().find(picker().type()).orElseThrow();
        for(String name:List.of("initialDate","firstDate","lastDate")){
            String expected=name.equals("initialDate")?"DateTime?":"DateTime";
            assertEquals(expected,d.property(new PropertyName(name)).orElseThrow().constraints().stream()
                    .filter(PropertyValueConstraint.DartObjectReferenceValues.class::isInstance)
                    .map(PropertyValueConstraint.DartObjectReferenceValues.class::cast).findFirst().orElseThrow().expectedDartType());
        }
        for(String bad:List.of("DateTime??","core.DateTime?","DateTime?;exit()","Unreviewed?")){
            assertThrows(IllegalArgumentException.class,()->new PropertyValueConstraint.DartObjectReferenceValues(bad));
            assertThrows(IllegalArgumentException.class,()->new GeneratedDartStaticTypeRequirement(0,1,bad));
        }
    }
    @Test void presetsReferencesNullsAndRequiredOmissionsAreNotConfused() {
        var n=with(with(picker(),"calendarDelegate",new PropertyValue.StringValue("gregorian")),"onDateSaved",new PropertyValue.StringValue("noop"));
        String output=dart(n);assertTrue(output.contains("GregorianCalendarDelegate()"),output);
        assertTrue(output.contains("onDateSaved: (_) {}"),output);
        assertTrue(dart(with(n,"initialDate",new PropertyValue.NullValue())).contains("initialDate: null"));
        for(String name:List.of("firstDate","lastDate")){
            var p=new LinkedHashMap<>(n.properties());p.remove(new PropertyName(name));
            var missing=new WidgetNode(n.id(),n.type(),p,n.slots());
            assertFalse(new DartRegionGenerator().generate(RotationTransitionContractTest.document(missing),BuiltInWidgetCatalog.getDefault()).successful(),name);
        }
    }
}

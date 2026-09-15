package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Flutter 3.44.8 DatePickerDialog: 20 properties and two Icon-only slots. */
public final class DatePickerDialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.DatePickerDialog");
    public static final Set<String> DATES = Set.of("firstDate", "lastDate", "initialDate", "currentDate");
    public static final String DESCRIPTION = "Native date-picker dialog. Confirm returns DateTime through Navigator; cancel returns null. "
            + "There is no onChanged/onConfirm constructor callback. Show it on a DialogRoute/showDialog and await the route result. "
            + "Initial date/modes are initial-only, not controlled State values. Canvas never executes project dates, predicates or calendar delegates.";
    public static final List<String> KEYBOARDS = List.of("text","multiline","number","numberSigned","numberDecimal","numberSignedDecimal",
            "phone","datetime","emailAddress","url","visiblePassword","name","streetAddress","none","webSearch","twitter");

    public static List<PropertyDefinition> properties() {
        var p = new ArrayList<PropertyDefinition>();
        add(p,"key",false,List.of(new PropertyValueConstraint.StringLength(0,4096),ref("Key?"),nil()),null);
        for(String name : List.of("initialDate","firstDate","lastDate","currentDate")) {
            boolean required = name.equals("firstDate") || name.equals("lastDate");
            var c = new ArrayList<PropertyValueConstraint>(List.of(
                    new PropertyValueConstraint.StringPattern("[0-9]{4}-[0-9]{2}-[0-9]{2}","Gregorian date YYYY-MM-DD (years 0001–9999)"),
                    ref("DateTime")));
            if(!required)c.add(nil());
            add(p,name,required,c,name.equals("firstDate") ? new PropertyValue.StringValue("1900-01-01")
                    : name.equals("lastDate") ? new PropertyValue.StringValue("2100-12-31") : null);
        }
        add(p,"initialEntryMode",false,List.of(en("DatePickerEntryMode","calendar","input","calendarOnly","inputOnly")),null);
        add(p,"selectableDayPredicate",false,List.of(ref("SelectableDayPredicate"),nil()),null);
        for(String name:List.of("cancelText","confirmText","helpText","errorFormatText","errorInvalidText","fieldHintText","fieldLabelText","restorationId"))
            add(p,name,false,List.of(new PropertyValueConstraint.StringLength(0,16384),nil()),null);
        add(p,"initialCalendarMode",false,List.of(en("DatePickerMode","day","year")),null);
        add(p,"keyboardType",false,List.of(new PropertyValueConstraint.StringPattern("(?:"+String.join("|",KEYBOARDS)+")","TextInputType preset"),ref("TextInputType"),nil()),null);
        add(p,"onDatePickerModeChange",false,List.of(new PropertyValueConstraint.StringPattern("noop","Explicit no-op callback"),ref("ValueChanged<DatePickerEntryMode>"),nil()),null);
        add(p,"insetPadding",false,List.of(new PropertyValueConstraint.EdgeInsetsValues(true,false),ref("EdgeInsets")),null);
        add(p,"calendarDelegate",false,List.of(new PropertyValueConstraint.StringPattern("gregorian","Built-in Gregorian calendar"),ref("CalendarDelegate<DateTime>")),null);
        if(p.size()!=20)throw new IllegalStateException("DatePickerDialog property count changed");
        return List.copyOf(p);
    }
    public static LocalDate date(String text) {
        try {
            if(!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException();
            var date=LocalDate.parse(text);
            if(date.getYear()<1 || date.getYear()>9999)throw new IllegalArgumentException();
            return date;
        } catch(DateTimeParseException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Use a valid Gregorian date YYYY-MM-DD, years 0001–9999. Use a typed DateTime reference for the wider Dart range.");
        }
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if(!TYPE.equals(node.type()))return Optional.empty();
        return localDateRelationshipError(node);
    }
    /** Shared validation for native date pickers; project calendar semantics remain source-owned. */
    public static Optional<String> localDateRelationshipError(WidgetNode node) {
        try {
            var local=new HashMap<String,LocalDate>();
            for(String name:DATES)if(node.properties().get(new PropertyName(name)) instanceof PropertyValue.StringValue s)local.put(name,date(s.value()));
            // Project calendar semantics cannot be evaluated safely during editing.
            if(node.properties().get(new PropertyName("calendarDelegate")) instanceof PropertyValue.DartObjectReferenceValue)return Optional.empty();
            var first=local.get("firstDate");var last=local.get("lastDate");var initial=local.get("initialDate");
            if(first!=null&&last!=null&&first.isAfter(last))return Optional.of("First date must be on or before Last date.");
            if(initial!=null && (first!=null&&initial.isBefore(first) || last!=null&&initial.isAfter(last)))
                return Optional.of("Initial date must be within First date and Last date.");
            return Optional.empty();
        } catch(IllegalArgumentException invalid){return Optional.of(invalid.getMessage());}
    }
    public static List<String> presets(String name) {
        return switch(name){case "calendarDelegate"->List.of("gregorian");case "keyboardType"->KEYBOARDS;
            case "onDatePickerModeChange"->List.of("noop");default->List.of();};
    }
    public static String label(String name) {
        if (name.equals("restorationId")) return "Restoration ID";
        String words = name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
    public static String group(String name) {
        if (DATES.contains(name) || name.equals("selectableDayPredicate") || name.equals("calendarDelegate")) return "Dates and calendar";
        if (name.endsWith("Text")) return "Labels and validation";
        if (name.equals("onDatePickerModeChange")) return "Events";
        return "Behavior and appearance";
    }
    public static String help(String name) {
        if(DATES.contains(name))return "Gregorian YYYY-MM-DD (years 0001–9999), or a strictly typed DateTime project reference/getter/factory. "
                + "Dart references support the wider native range; only the calendar's date part is used. First/last bounds are inclusive. "
                + (name.equals("currentDate")?"Omission uses calendarDelegate.now(); today can lie outside the selectable range."
                :name.equals("initialDate")?"Unset/null means no initially selected date; initial-only, not changed by rebuilding.":"Required date; cannot be reset or null.");
        return switch(name){
            case "calendarDelegate"->"Gregorian preset or any verified CalendarDelegate<DateTime> implementation, including custom calendars. Project delegates own date conversion, navigation, localization and parsing; not executed in Canvas.";
            case "selectableDayPredicate"->"Optional bool Function(DateTime). Initial date must satisfy it. Create/select a predicate handler; predicates do not report a date selection and are never run by Canvas.";
            case "onDatePickerModeChange"->"ValueChanged<DatePickerEntryMode>, fired when toggling input/calendar. Does not return the chosen date; await the Navigator route result.";
            case "initialEntryMode"->"calendar, input, calendarOnly or inputOnly. Initial/restorable value, not a controlled State field.";
            case "initialCalendarMode"->"Start in day or year selection. Initial-only; native restoration and user navigation retain their behavior.";
            case "insetPadding"->"Non-negative physical EdgeInsets or typed EdgeInsets reference. Native default: horizontal 16, vertical 24; MediaQuery.viewInsets is added.";
            case "keyboardType"->"All 16 TextInputType presets, null/default (datetime), or a typed TextInputType project reference.";
            case "restorationId"->"Restoration ID for the dialog. Stateful restoration also requires a surrounding restoration scope and a restorable DialogRoute.";
            default->"Optional native "+name+"; omission/null preserve the SDK's localized default, while an empty string is explicit.";
        };
    }
    private static PropertyValueConstraint ref(String type){return new PropertyValueConstraint.DartObjectReferenceValues(type);}
    private static PropertyValueConstraint nil(){return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL);}
    private static PropertyValueConstraint en(String type,String... values){return new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/material.dart",type),List.of(values));}
    private static void add(List<PropertyDefinition> p,String name,boolean required,List<PropertyValueConstraint> c,PropertyValue initial){
        p.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(p.size(),required),c,Optional.ofNullable(initial)));
    }
    private DatePickerDialogWidgetPropertySchema(){}
}

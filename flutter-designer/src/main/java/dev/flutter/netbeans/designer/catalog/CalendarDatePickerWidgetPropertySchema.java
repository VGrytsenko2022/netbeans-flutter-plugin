package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 CalendarDatePicker constructor projection. */
public final class CalendarDatePickerWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.CalendarDatePicker");
    public static final String DESCRIPTION = "Inline Material calendar, not a dialog. Requires Material, localization and directionality ancestors. "
            + "onDateChanged reports a selection; onDisplayedMonthChanged reports month navigation. "
            + "initialDate and initialCalendarMode are initial-only: change the Key to reset native state. "
            + "Canvas never executes project dates, callbacks, predicates or calendar delegates.";
    public static List<PropertyDefinition> properties() {
        var shared = DatePickerDialogWidgetPropertySchema.properties().stream().collect(
                java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p));
        var properties = new ArrayList<PropertyDefinition>();
        for (String name : List.of("key","initialDate","firstDate","lastDate","currentDate")) {
            var p=shared.get(name);
            var constraints=p.constraints().stream().map(c ->
                    Set.of("initialDate","currentDate").contains(name) && c instanceof PropertyValueConstraint.DartObjectReferenceValues
                            ? (PropertyValueConstraint)new PropertyValueConstraint.DartObjectReferenceValues("DateTime?") : c).toList();
            properties.add(new PropertyDefinition(p.name(),DartParameter.named(properties.size(),name.equals("initialDate")||p.parameter().required()),
                    constraints,name.equals("initialDate")?Optional.of(new PropertyValue.NullValue()):p.creationDefault()));
        }
        for (String name : List.of("onDateChanged","onDisplayedMonthChanged")) {
            boolean required=name.equals("onDateChanged");
            var constraints=new ArrayList<PropertyValueConstraint>(List.of(
                    new PropertyValueConstraint.StringPattern("noop","Explicit no-op callback"),
                    new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<DateTime>")));
            if (!required) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            properties.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(properties.size(),required),
                    constraints,required?Optional.of(new PropertyValue.StringValue("noop")):Optional.empty()));
        }
        for(String name:List.of("initialCalendarMode","selectableDayPredicate","calendarDelegate")) {
            var p=shared.get(name);
            properties.add(new PropertyDefinition(p.name(),DartParameter.named(properties.size(),false),p.constraints(),p.creationDefault()));
        }
        if(properties.size()!=10)throw new IllegalStateException("CalendarDatePicker property count changed");
        return List.copyOf(properties);
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        return TYPE.equals(node.type())?DatePickerDialogWidgetPropertySchema.localDateRelationshipError(node):Optional.empty();
    }
    public static List<String> presets(String name) {
        return name.equals("onDateChanged")||name.equals("onDisplayedMonthChanged")?List.of("noop"):DatePickerDialogWidgetPropertySchema.presets(name);
    }
    public static String label(String name) { return DatePickerDialogWidgetPropertySchema.label(name); }
    public static String group(String name) {
        return name.equals("onDateChanged")||name.equals("onDisplayedMonthChanged")?"Events":DatePickerDialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        return switch(name) {
            case "initialDate" -> "Required argument: a Gregorian YYYY-MM-DD date, typed DateTime source or explicit null (no selection). Must satisfy inclusive bounds and the predicate. Changing it does not reset native state; change Key for a new instance.";
            case "initialCalendarMode" -> "Initial day or year grid. Not controlled State; change Key to reset native selection/navigation state.";
            case "onDateChanged" -> "Required non-null ValueChanged<DateTime>. Create/select an editable handler or use the explicit no-op. Disconnect restores no-op, not null/omission. This is not a Navigator route result.";
            case "onDisplayedMonthChanged" -> "Optional ValueChanged<DateTime>. Reports a newly displayed calendar month (the delegate's first day), including year navigation; not every rebuild or date selection.";
            case "key" -> "String ValueKey, typed Key, null or omission. Changing Key creates a fresh calendar with initialDate/initialCalendarMode; same-key rebuilds retain native state.";
            default -> DatePickerDialogWidgetPropertySchema.help(name);
        };
    }
    private CalendarDatePickerWidgetPropertySchema() { }
}

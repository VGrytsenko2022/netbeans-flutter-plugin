package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 InputDatePickerFormField constructor projection. */
public final class InputDatePickerFormFieldWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.InputDatePickerFormField");
    public static final String DESCRIPTION = "Material date text field with locale-aware parsing. Requires Material and localization ancestors. "
            + "onDateSubmitted reports valid submitted dates; onDateSaved reports valid dates when FormState.save is called. "
            + "acceptEmptyDate permits empty validation but never emits null. No public controller or live onChanged callback. "
            + "Canvas disables editing/focus and never executes project dates, callbacks, predicates, focus nodes or calendar delegates.";
    public static List<PropertyDefinition> properties() {
        var shared = DatePickerDialogWidgetPropertySchema.properties().stream().collect(
                java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p));
        var result = new ArrayList<PropertyDefinition>();
        for (String name : List.of("key","initialDate","firstDate","lastDate","onDateSubmitted","onDateSaved",
                "selectableDayPredicate","errorFormatText","errorInvalidText","fieldHintText","fieldLabelText",
                "keyboardType","autofocus","acceptEmptyDate","focusNode","calendarDelegate")) {
            PropertyDefinition p = shared.get(name);
            var constraints = p == null ? new ArrayList<PropertyValueConstraint>() : new ArrayList<>(p.constraints());
            if (name.equals("initialDate")) constraints.replaceAll(c -> c instanceof PropertyValueConstraint.DartObjectReferenceValues
                    ? new PropertyValueConstraint.DartObjectReferenceValues("DateTime?") : c);
            if (name.equals("onDateSubmitted") || name.equals("onDateSaved")) constraints.addAll(List.of(
                    new PropertyValueConstraint.StringPattern("noop","Explicit no-op callback"),
                    new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<DateTime>"),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)));
            if (name.equals("autofocus") || name.equals("acceptEmptyDate"))
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
            if (name.equals("focusNode")) constraints.addAll(List.of(
                    new PropertyValueConstraint.DartObjectReferenceValues("FocusNode?"),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)));
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(),p!=null&&p.parameter().required()),
                    constraints,p==null?Optional.empty():p.creationDefault()));
        }
        if (result.size()!=16) throw new IllegalStateException("InputDatePickerFormField property count changed");
        return List.copyOf(result);
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        return TYPE.equals(node.type()) ? DatePickerDialogWidgetPropertySchema.localDateRelationshipError(node) : Optional.empty();
    }
    public static List<String> presets(String name) {
        return name.equals("onDateSubmitted")||name.equals("onDateSaved")?List.of("noop"):DatePickerDialogWidgetPropertySchema.presets(name);
    }
    public static String label(String name) { return DatePickerDialogWidgetPropertySchema.label(name); }
    public static String group(String name) {
        return name.equals("onDateSubmitted")||name.equals("onDateSaved")?"Events":DatePickerDialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        return switch(name) {
            case "initialDate" -> "Optional Gregorian YYYY-MM-DD date, typed DateTime? source or null. Must satisfy inclusive bounds and the predicate. A changed initialDate updates the text on the next frame, even with the same Key; unchanged initialDate retains typed text.";
            case "onDateSubmitted" -> "Optional ValueChanged<DateTime>. Called only for a valid date when the field is submitted. Not a live onChanged event; invalid or empty input never emits a date.";
            case "onDateSaved" -> "Optional ValueChanged<DateTime>. Called only for a valid date when the surrounding FormState.save() runs. Submission does not save the form. No null callback for empty input.";
            case "acceptEmptyDate" -> "Default false. True allows an empty field through Form validation; onDateSaved/onDateSubmitted still require a valid non-empty date.";
            case "autofocus" -> "Default false. Requests focus and selects the initial date text once in the application. Disabled in Canvas to preserve Designer focus.";
            case "focusNode" -> "Optional typed FocusNode reference/getter/factory or null. The application owns and disposes supplied nodes; do not create a fresh node on every build. Canvas never shares or executes project focus nodes.";
            case "key" -> "String ValueKey, typed Key, null or omission. A new Key resets internal controller/form state. Changing initialDate already updates the text with the same Key.";
            default -> DatePickerDialogWidgetPropertySchema.help(name);
        };
    }
    private InputDatePickerFormFieldWidgetPropertySchema() { }
}

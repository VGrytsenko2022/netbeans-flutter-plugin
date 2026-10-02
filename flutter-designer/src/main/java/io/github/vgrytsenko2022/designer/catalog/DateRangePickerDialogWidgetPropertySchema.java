package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.time.LocalDate;
import java.util.*;

/** Flutter 3.44.8 DateRangePickerDialog: all 23 arguments, including two Icon slots. */
public final class DateRangePickerDialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.DateRangePickerDialog");
    public static final Set<String> DATES = Set.of("firstDate", "lastDate", "currentDate");
    public static final List<String> TEXTS = List.of("helpText", "cancelText", "confirmText", "saveText",
            "errorInvalidRangeText", "errorFormatText", "errorInvalidText", "fieldStartHintText", "fieldEndHintText",
            "fieldStartLabelText", "fieldEndLabelText", "restorationId");
    public static final String DESCRIPTION = "Native date-range picker dialog. Save/OK returns DateTimeRange<DateTime> through Navigator; cancel returns null. "
            + "There is no onChanged/onConfirm or entry-mode callback. Show on a DialogRoute/showDialog and await its result. "
            + "Initial range/mode are initial-only, not controlled State fields. Canvas never executes project dates, ranges, predicates or calendar delegates.";

    public static List<PropertyDefinition> properties() {
        var p = new ArrayList<PropertyDefinition>();
        var shared = DatePickerDialogWidgetPropertySchema.properties().stream().collect(
                java.util.stream.Collectors.toMap(v -> v.name().value(), v -> v));
        copy(p, shared.get("key"));
        add(p, "initialDateRange", false, List.of(
                new PropertyValueConstraint.StringPattern("[0-9]{4}-[0-9]{2}-[0-9]{2}/[0-9]{4}-[0-9]{2}-[0-9]{2}", "Inclusive Gregorian start/end dates"),
                ref("DateTimeRange<DateTime>"), nil()), null);
        for (String name : List.of("firstDate", "lastDate", "currentDate", "initialEntryMode")) copy(p, shared.get(name));
        for (String name : TEXTS) add(p, name, false, List.of(new PropertyValueConstraint.StringLength(0,16384), nil()), null);
        // Unlike DatePickerDialog, keyboardType is non-nullable.
        add(p, "keyboardType", false, shared.get("keyboardType").constraints().stream().filter(c -> c.kind() != PropertyValueKind.NULL).toList(), null);
        add(p, "selectableDayPredicate", false, List.of(ref("SelectableDayForRangePredicate"), nil()), null);
        copy(p, shared.get("calendarDelegate"));
        if (p.size() != 21) throw new IllegalStateException("DateRangePickerDialog property count changed");
        return List.copyOf(p);
    }
    public record Range(LocalDate start, LocalDate end) { }
    public static Range range(String text) {
        if (text.length() != 21 || text.charAt(10) != '/') throw new IllegalArgumentException("Use inclusive Gregorian start/end dates: YYYY-MM-DD/YYYY-MM-DD.");
        var start = DatePickerDialogWidgetPropertySchema.date(text.substring(0,10));
        var end = DatePickerDialogWidgetPropertySchema.date(text.substring(11));
        if (start.isAfter(end)) throw new IllegalArgumentException("Start date must be on or before End date.");
        return new Range(start,end);
    }
    public static Optional<String> relationshipError(WidgetNode node) {
        if (!TYPE.equals(node.type())) return Optional.empty();
        try {
            var local = new HashMap<String,LocalDate>();
            for (String name : DATES) if (node.properties().get(new PropertyName(name)) instanceof PropertyValue.StringValue s)
                local.put(name, DatePickerDialogWidgetPropertySchema.date(s.value()));
            Range range = node.properties().get(new PropertyName("initialDateRange")) instanceof PropertyValue.StringValue s ? range(s.value()) : null;
            // DateTimeRange itself requires start <= end, even for a project calendar.
            if (node.properties().get(new PropertyName("calendarDelegate")) instanceof PropertyValue.DartObjectReferenceValue) return Optional.empty();
            var first = local.get("firstDate"); var last = local.get("lastDate");
            if (first != null && last != null && first.isAfter(last)) return Optional.of("First date must be on or before Last date.");
            if (range != null && (first != null && range.start().isBefore(first) || last != null && range.end().isAfter(last)))
                return Optional.of("Initial range must be within First date and Last date (inclusive).");
            return Optional.empty();
        } catch (IllegalArgumentException invalid) { return Optional.of(invalid.getMessage()); }
    }
    public static List<String> presets(String name) { return DatePickerDialogWidgetPropertySchema.presets(name); }
    public static String label(String name) { return DatePickerDialogWidgetPropertySchema.label(name); }
    public static String group(String name) {
        return name.equals("initialDateRange") ? "Dates and calendar" : DatePickerDialogWidgetPropertySchema.group(name);
    }
    public static String help(String name) {
        return switch (name) {
            case "initialDateRange" -> "Inclusive Gregorian start/end dates (years 0001–9999) or a strictly typed DateTimeRange<DateTime> reference/getter/factory. Start must not follow end and both must lie inside bounds. Unset/null means no selection; initial-only.";
            case "selectableDayPredicate" -> "bool Function(DateTime day, DateTime? selectedStartDay, DateTime? selectedEndDay). Create/select an editable predicate. Endpoints must satisfy it; it restricts dates, not a selection event. Not executed in Canvas.";
            case "keyboardType" -> "All 16 TextInputType presets or a typed non-null TextInputType reference. Omission defaults to datetime; explicit null is not supported by Flutter.";
            case "saveText" -> "Localized Save label in fullscreen calendar mode. Confirm text applies to OK in input mode.";
            default -> DatePickerDialogWidgetPropertySchema.help(name);
        };
    }
    private static PropertyValueConstraint ref(String type) { return new PropertyValueConstraint.DartObjectReferenceValues(type); }
    private static PropertyValueConstraint nil() { return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL); }
    private static void copy(List<PropertyDefinition> p, PropertyDefinition value) {
        add(p, value.name().value(), value.parameter().required(), value.constraints(), value.creationDefault().orElse(null));
    }
    private static void add(List<PropertyDefinition> p, String name, boolean required, List<PropertyValueConstraint> c, PropertyValue initial) {
        p.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(p.size(),required), c, Optional.ofNullable(initial)));
    }
    private DateRangePickerDialogWidgetPropertySchema() { }
}

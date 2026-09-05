package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete default-constructor projection of Flutter 3.44.8 Visibility. */
public final class VisibilityWidgetPropertySchema {
    public static final WidgetTypeId VISIBILITY_TYPE = new WidgetTypeId("flutter.widgets.Visibility");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 7;
    public static final int SLOT_COUNT = 2;

    public enum Group {
        VISIBILITY("visibilityBehavior", "Visibility", "Show the child or its optional replacement."),
        MAINTAIN("visibilityMaintenance", "Maintenance", "State, animation, layout and interaction retention while hidden.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }
        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();
    private VisibilityWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "visible", Group.VISIBILITY, "Visible",
                "Omission preserves true. False hides child; when maintainState is false, child is disposed and replacement is shown. An unset or empty replacement preserves SizedBox.shrink(). Designer retains both model branches for later editing.", 2);
        add(values, "maintainState", Group.MAINTAIN, "Maintain state",
                "Omission preserves false. True keeps hidden child state and ignores replacement. Required by maintainAnimation and maintainFocusability. Changing maintenance flags can discard runtime state; normally only visible changes dynamically.", 3);
        add(values, "maintainAnimation", Group.MAINTAIN, "Maintain animation",
                "Omission preserves false. True keeps hidden child tickers active and requires maintainState=true. Required by maintainSize; retaining hidden animations consumes resources.", 4);
        add(values, "maintainSize", Group.MAINTAIN, "Maintain size",
                "Omission preserves false. True preserves hidden child layout and requires maintainAnimation=true (and maintainState=true). Required by maintainSemantics and maintainInteractivity.", 5);
        add(values, "maintainSemantics", Group.MAINTAIN, "Maintain semantics",
                "Omission preserves false. True exposes hidden child accessibility semantics and requires maintainSize=true. Replacement semantics are independent when maintainState=false.", 6);
        add(values, "maintainInteractivity", Group.MAINTAIN, "Maintain interactivity",
                "Omission preserves false. True permits hidden child pointer interaction and requires maintainSize=true. This does not itself permit focus; configure maintainFocusability separately.", 7);
        add(values, "maintainFocusability", Group.MAINTAIN, "Maintain focusability",
                "Omission preserves false. True permits focus in hidden retained child and requires maintainState=true, not maintainSize. Other ancestor and local focus restrictions still apply. It does not automatically request focus. All six maintenance flags true with replacement unset exactly represent Visibility.maintain.", 8);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Visibility schema/property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate Visibility property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

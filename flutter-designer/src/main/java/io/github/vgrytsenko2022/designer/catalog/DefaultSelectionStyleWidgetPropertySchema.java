package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 DefaultSelectionStyle constructor and merge helper projection. */
public final class DefaultSelectionStyleWidgetPropertySchema {
    public static final WidgetTypeId DEFAULT_SELECTION_STYLE_TYPE =
            new WidgetTypeId("flutter.widgets.DefaultSelectionStyle");
    /** Three SDK fields plus the Designer-only direct/merge choice. */
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 4;
    public static final int SLOT_COUNT = 1;

    private static final List<String> MOUSE_CURSOR_PRESETS = List.of(
            "none", "basic", "click", "forbidden", "wait", "progress", "contextMenu", "help",
            "text", "verticalText", "cell", "precise", "move", "grab", "grabbing", "noDrop",
            "alias", "copy", "disappearing", "allScroll", "resizeLeftRight", "resizeUpDown",
            "resizeUpLeftDownRight", "resizeUpRightDownLeft", "resizeUp", "resizeDown",
            "resizeLeft", "resizeRight", "resizeUpLeft", "resizeUpRight", "resizeDownLeft",
            "resizeDownRight", "resizeColumn", "resizeRow", "zoomIn", "zoomOut", "defer", "uncontrolled",
            "clickable", "adaptiveClickable", "textable");

    public enum Group {
        SELECTION("defaultSelectionStyle", "Selection style", "Inherited cursor, selection highlight and selectable-text mouse cursor.");
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
    private DefaultSelectionStyleWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    /** All 36 SystemMouseCursors, two MouseCursor controls and three WidgetStateMouseCursor constants. */
    public static List<String> mouseCursorPresets() { return MOUSE_CURSOR_PRESETS; }
    public static String mouseCursorPattern() { return "(?:" + String.join("|", MOUSE_CURSOR_PRESETS) + ")"; }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "cursorColor", "Cursor color",
                "Optional literal ARGB color or reviewed Material ColorScheme role. Direct mode leaves an unset value null; Merge inherits that field from the nearest DefaultSelectionStyle. A descendant TextField's explicit cursor color takes precedence, with SDK error/theme fallbacks unchanged.", 0);
        add(values, "selectionColor", "Selection color",
                "Optional literal ARGB selection highlight or reviewed Material ColorScheme role. Direct mode leaves an unset value null; Merge inherits the nearest value. Descendant Text's explicit selectionColor takes precedence; fallback colors remain the SDK consumer's responsibility.", 1);
        add(values, "mouseCursor", "Mouse cursor",
                "Optional selectable-Text hover cursor: all SystemMouseCursors presets, MouseCursor.defer/uncontrolled and WidgetStateMouseCursor.clickable/adaptiveClickable/textable. Direct unset is null (selectable Text falls back to its text cursor); Merge unset inherits the nearest field. This does not make non-selectable Text selectable or override TextField's separate mouse cursor.", 2);
        add(values, "merge", "Merge inherited style",
                "Required Designer-only mode, created as false: false uses the const-capable DefaultSelectionStyle constructor and replaces all three fields, including nulls. True calls the non-const DefaultSelectionStyle.merge helper, inheriting each unset SDK field independently. It cannot explicitly clear an inherited field to null; use direct mode to clear. This flag cannot be reset and is never emitted as a Dart argument.", 4);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("DefaultSelectionStyle schema/property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(Group.SELECTION, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate DefaultSelectionStyle property schema: " + name);
        }
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

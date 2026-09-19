package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 IconButton constructor union and reviewed local ButtonStyle projection. */
public final class IconButtonWidgetPropertySchema {
    public static final WidgetTypeId ICON_BUTTON_TYPE = new WidgetTypeId("flutter.material.IconButton");
    /** SDK fields plus density flattening and two selectors, excluding the whole style reference. */
    public static final int DIRECT_PROPERTY_COUNT = 25;
    public static final int STATE_COUNT = TextButtonWidgetPropertySchema.STATE_COUNT;
    public static final int STATE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.STATE_PROPERTY_COUNT;
    public static final int COMMON_STYLE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.COMMON_STYLE_PROPERTY_COUNT;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = TextButtonWidgetPropertySchema.LOCAL_STYLE_PROPERTY_COUNT;
    public static final int FLATTENED_PROPERTY_COUNT = DIRECT_PROPERTY_COUNT + LOCAL_STYLE_PROPERTY_COUNT + 1;
    public static final int SLOT_COUNT = 2;

    public enum Group {
        EVENTS, BEHAVIOR, LAYOUT, APPEARANCE, ENABLED_STYLE, DISABLED_STYLE, ERROR_STYLE,
        DRAGGED_STYLE, PRESSED_STYLE, SELECTED_STYLE, SCROLLED_UNDER_STYLE, HOVERED_STYLE,
        FOCUSED_STYLE, COMMON_STYLE, STYLE_REFERENCE;

        public String setName() {
            if (this == LAYOUT) {
                return "iconButtonLayout";
            }
            if (this == APPEARANCE) {
                return "iconButtonAppearance";
            }
            return shared().setName().replace("textButton", "iconButton");
        }

        public String displayName() {
            return this == LAYOUT ? "Layout" : this == APPEARANCE ? "Appearance" : shared().displayName();
        }

        public String description() {
            return this == LAYOUT ? "Direct SDK layout, density and constraints."
                    : this == APPEARANCE ? "Direct SDK icon and interaction colors."
                    : shared().description().replace("TextButton", "IconButton");
        }

        private TextButtonWidgetPropertySchema.Group shared() {
            return TextButtonWidgetPropertySchema.Group.valueOf(name());
        }
    }

    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(dartName, "dartName");
            Objects.requireNonNull(encoding, "encoding");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative IconButton property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private IconButtonWidgetPropertySchema() {
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<Definition> find(PropertyName name) {
        return find(Objects.requireNonNull(name, "name").value());
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static List<String> variants() {
        return List.of("standard", "filled", "filledTonal", "outlined");
    }

    public static List<String> statePrefixes() {
        return TextButtonWidgetPropertySchema.statePrefixes();
    }

    public static List<String> statePriority() {
        return TextButtonWidgetPropertySchema.statePriority();
    }

    public static List<String> localStyleProperties() {
        return TextButtonWidgetPropertySchema.localStyleProperties();
    }

    public static List<String> mouseCursorPresets() {
        return DefaultSelectionStyleWidgetPropertySchema.mouseCursorPresets();
    }

    public static ElevatedButtonWidgetPropertySchema.Definition sharedStyleDefinition(String name) {
        return TextButtonWidgetPropertySchema.sharedStyleDefinition(name);
    }

    public static boolean isCompound(PropertyName name) {
        return name.value().startsWith("visualDensity")
                || find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static String constructorName(WidgetNode node) {
        PropertyValue value = node.properties().get(new PropertyName("variant"));
        return value instanceof PropertyValue.StringValue text && !text.value().equals("standard")
                ? text.value() : "";
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        direct(values, "iconSize", 0, Group.LAYOUT, "Icon size",
                "Signed finite size or positive infinity. Material 2 square layout and the mounted icon may reject a value that another SDK branch ignores.");
        direct(values, "visualDensityHorizontal", 1, Group.LAYOUT, "Horizontal density",
                "VisualDensity horizontal axis from -4 to 4. The omitted peer axis is zero.");
        direct(values, "visualDensityVertical", 2, Group.LAYOUT, "Vertical density",
                "VisualDensity vertical axis from -4 to 4. The omitted peer axis is zero.");
        direct(values, "padding", 3, Group.LAYOUT, "Padding", "Nonnegative physical or directional SDK padding.");
        direct(values, "alignment", 4, Group.LAYOUT, "Alignment", "Full physical or directional alignment geometry.");
        direct(values, "splashRadius", 5, Group.APPEARANCE, "Splash radius",
                "Strictly positive finite radius or positive infinity. Material 3 ignores this Material 2 splash setting.");
        int order = 6;
        for (String name : List.of("color", "focusColor", "hoverColor", "highlightColor", "splashColor", "disabledColor")) {
            direct(values, name, order++, Group.APPEARANCE, label(name),
                    "Literal color or reviewed theme token; omission preserves SDK theme behavior.");
        }
        direct(values, "onPressed", 12, Group.EVENTS, "On pressed", callback("VoidCallback"));
        direct(values, "onHover", 13, Group.EVENTS, "On hover", callback("ValueChanged<bool>"));
        direct(values, "onLongPress", 14, Group.EVENTS, "On long press", callback("VoidCallback"));
        direct(values, "mouseCursor", 15, Group.BEHAVIOR, "Mouse cursor",
                "One of 41 reviewed presets or a strictly proved non-null MouseCursor project reference.");
        direct(values, "focusNode", 16, Group.BEHAVIOR, "Focus node",
                "Strict non-null FocusNode project reference; ownership stays with the project.");
        direct(values, "autofocus", 17, Group.BEHAVIOR, "Autofocus", "Unset preserves SDK false.");
        direct(values, "tooltip", 18, Group.BEHAVIOR, "Tooltip", "SDK tooltip text.");
        direct(values, "enableFeedback", 19, Group.BEHAVIOR, "Enable feedback", "Unset preserves SDK feedback defaults.");
        direct(values, "constraints", 20, Group.LAYOUT, "Constraints", "Normalized BoxConstraints including unbounded maximums.");
        direct(values, "style", 21, Group.STYLE_REFERENCE, "Button style",
                "Strict non-null ButtonStyle reference, exclusive with all 498 local leaves. Material 2 ignores ButtonStyle.");
        direct(values, "isSelected", 22, Group.BEHAVIOR, "Selected",
                "Unset or explicit null is a non-toggle button; explicit false is an unselected toggle. Material 2 ignores selection.");
        direct(values, "statesController", 24, Group.BEHAVIOR, "States controller",
                "Strict non-null WidgetStatesController project reference. Material 2 ignores it; Canvas never executes project controllers.");
        values.put("variant", new Definition(Group.BEHAVIOR, "Constructor",
                "Required Standard, Filled, Filled tonal or Outlined constructor. All retain both icon slots and scalar fields. Cannot be reset.",
                Target.ACTIVATION, "variant", 26, Encoding.SCALAR));
        values.put("enabled", new Definition(Group.BEHAVIOR, "Enabled",
                "Required activation selector. Enabled supplies a benign onPressed when absent, including long-press-only. Disabled emits null activation callbacks while retaining their references. Cannot be reset.",
                Target.ACTIVATION, "enabled", 27, Encoding.SCALAR));
        int styleOrder = 28;
        for (String name : localStyleProperties()) {
            var shared = TextButtonWidgetPropertySchema.find(name).orElseThrow();
            values.put(name, new Definition(Group.valueOf(shared.group().name()), shared.displayName(),
                    shared.description().replace("TextButton", "IconButton"), shared.target(),
                    shared.dartName(), styleOrder++, shared.encoding()));
        }
        if (values.size() != FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("IconButton property count: " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void direct(Map<String, Definition> values, String name, int order,
            Group group, String displayName, String description) {
        values.put(name, new Definition(group, displayName, description,
                name.equals("onPressed") || name.equals("onLongPress") ? Target.ACTIVATION : Target.DIRECT,
                name, order, Encoding.SCALAR));
    }

    private static String callback(String type) {
        return "Strict non-null " + type + " project reference. Dynamic, nullable outer types and wrong signatures are rejected; Canvas never executes project callbacks.";
    }

    private static String label(String name) {
        String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}

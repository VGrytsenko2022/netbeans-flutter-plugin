package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Encoding;
import io.github.vgrytsenko2022.designer.catalog.ElevatedButtonWidgetPropertySchema.Target;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pinned Flutter 3.44.8 MenuBar and its sparse local MenuStyle projection. */
public final class MenuBarWidgetPropertySchema {
    public static final WidgetTypeId MENU_BAR_TYPE = new WidgetTypeId("flutter.material.MenuBar");
    public static final int DIRECT_PROPERTY_COUNT = 3;
    public static final int LOCAL_STYLE_PROPERTY_COUNT = MenuAnchorWidgetPropertySchema.LOCAL_STYLE_PROPERTY_COUNT;
    public static final int FLATTENED_PROPERTY_COUNT = DIRECT_PROPERTY_COUNT + LOCAL_STYLE_PROPERTY_COUNT;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        BEHAVIOR("menuBarBehavior", "Behavior", "MenuBar clipping and controller."),
        ENABLED_STYLE("menuBarEnabledStyle", "Style — enabled/default", "Sparse default MenuStyle."),
        DISABLED_STYLE("menuBarDisabledStyle", "Style — disabled", "Sparse disabled MenuStyle."),
        ERROR_STYLE("menuBarErrorStyle", "Style — error", "Sparse error MenuStyle."),
        DRAGGED_STYLE("menuBarDraggedStyle", "Style — dragged", "Sparse dragged MenuStyle."),
        PRESSED_STYLE("menuBarPressedStyle", "Style — pressed", "Sparse pressed MenuStyle."),
        SELECTED_STYLE("menuBarSelectedStyle", "Style — selected", "Sparse selected MenuStyle."),
        SCROLLED_UNDER_STYLE("menuBarScrolledUnderStyle", "Style — scrolled under", "Sparse scrolled-under MenuStyle."),
        HOVERED_STYLE("menuBarHoveredStyle", "Style — hovered", "Sparse hovered MenuStyle."),
        FOCUSED_STYLE("menuBarFocusedStyle", "Style — focused", "Sparse focused MenuStyle."),
        COMMON_STYLE("menuBarCommonStyle", "Style — layout & density", "MenuStyle alignment and VisualDensity."),
        STYLE_REFERENCE("menuBarStyleReference", "Style — project reference", "A complete MenuStyle reference, exclusive with local leaves.");

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

    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder, Encoding encoding) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(dartName, "dartName");
            Objects.requireNonNull(encoding, "encoding");
            if (dartOrder < 0) throw new IllegalArgumentException("Negative MenuBar property order");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private MenuBarWidgetPropertySchema() { }

    public static Map<String, Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String name) { return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name"))); }
    public static Optional<Definition> find(PropertyName name) { return find(Objects.requireNonNull(name, "name").value()); }
    public static List<String> localStyleProperties() { return MenuAnchorWidgetPropertySchema.localStyleProperties(); }
    public static List<String> statePrefixes() { return MenuAnchorWidgetPropertySchema.statePrefixes(); }
    public static List<String> statePriority() { return MenuAnchorWidgetPropertySchema.statePriority(); }
    public static boolean isCompound(PropertyName name) {
        return localStyleProperties().contains(Objects.requireNonNull(name, "name").value());
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        values.put("style", new Definition(Group.STYLE_REFERENCE, "Style",
                "Whole nullable MenuStyle reference/factory, exclusive with all 203 local leaves. Native MenuTheme and SDK fallback remain authoritative.",
                Target.DIRECT, "style", 0, Encoding.SCALAR));
        values.put("clipBehavior", new Definition(Group.BEHAVIOR, "Clip behavior",
                "Non-null Clip, default Clip.none. It clips the MenuBar surface and does not invent a child or button clip policy.",
                Target.DIRECT, "clipBehavior", 1, Encoding.SCALAR));
        values.put("controller", new Definition(Group.BEHAVIOR, "Controller",
                "Nullable MenuController reference/factory. Omission/null uses the native internal controller; project ownership and disposal remain external.",
                Target.DIRECT, "controller", 2, Encoding.SCALAR));
        for (String source : MenuAnchorWidgetPropertySchema.localStyleProperties()) {
            var shared = MenuAnchorWidgetPropertySchema.find(source).orElseThrow();
            Group group = switch (shared.group()) {
                case ENABLED_STYLE -> Group.ENABLED_STYLE;
                case DISABLED_STYLE -> Group.DISABLED_STYLE;
                case ERROR_STYLE -> Group.ERROR_STYLE;
                case DRAGGED_STYLE -> Group.DRAGGED_STYLE;
                case PRESSED_STYLE -> Group.PRESSED_STYLE;
                case SELECTED_STYLE -> Group.SELECTED_STYLE;
                case SCROLLED_UNDER_STYLE -> Group.SCROLLED_UNDER_STYLE;
                case HOVERED_STYLE -> Group.HOVERED_STYLE;
                case FOCUSED_STYLE -> Group.FOCUSED_STYLE;
                case COMMON_STYLE -> Group.COMMON_STYLE;
                default -> throw new IllegalStateException("Unexpected MenuStyle group: " + shared.group());
            };
            values.put(source, new Definition(group, shared.displayName(),
                    shared.description().replace("MenuAnchor", "MenuBar")
                            + " Local leaves are exclusive with whole Style; non-default state values remain stored/generated source data.",
                    shared.target(), shared.dartName(), shared.dartOrder(), shared.encoding()));
        }
        if (values.size() != FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("MenuBar schema count " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }
}

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Designer projection of the static ListWheelScrollView constructor in Flutter 3.44.8. */
public final class ListWheelScrollViewWidgetPropertySchema {
    public static final WidgetTypeId LIST_WHEEL_SCROLL_VIEW_TYPE =
            new WidgetTypeId("flutter.widgets.ListWheelScrollView");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 18;
    public static final int SLOT_COUNT = 1;
    public static final List<String> PHYSICS_PRESETS = List.of(
            "alwaysScrollable", "bouncing", "clamping", "neverScrollable", "page", "rangeMaintaining");

    public enum Group {
        SCROLLING("listWheelScrolling", "Scrolling", "Controller, physics, and drag behavior."),
        GEOMETRY("listWheelGeometry", "Wheel geometry", "Diameter, perspective, magnification, and item extent."),
        BEHAVIOR("listWheelBehavior", "Behavior", "Selection reporting, hit testing, and project callbacks."),
        APPEARANCE("listWheelAppearance", "Appearance", "Viewport clipping and off-axis rendering."),
        RESTORATION("listWheelRestoration", "Restoration", "Stable scroll-position restoration metadata.");

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

    public enum Target { DIRECT, PHYSICS_PRESET }

    public record Definition(Group group, String displayName, String description,
            Target target, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            Objects.requireNonNull(target, "target");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private ListWheelScrollViewWidgetPropertySchema() { }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static boolean isSynthesized(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        int order = 0;
        add(values, "controller", Group.SCROLLING, "Controller", "Optional ScrollController project reference.", order++);
        add(values, "physics", Group.SCROLLING, "Scroll physics", "Reviewed static ScrollPhysics preset; omission preserves Flutter inference.", Target.PHYSICS_PRESET, "physics", order++);
        add(values, "diameterRatio", Group.GEOMETRY, "Diameter ratio", "Positive ratio of the wheel diameter to the viewport height.", order++);
        add(values, "perspective", Group.GEOMETRY, "Perspective", "Positive perspective in the inclusive SDK range 0 through 0.01 (exclusive of zero).", order++);
        add(values, "offAxisFraction", Group.GEOMETRY, "Off-axis fraction", "Signed horizontal fraction shifting the wheel away from the viewport center.", order++);
        add(values, "useMagnifier", Group.GEOMETRY, "Use magnifier", "Whether the centered child is magnified.", order++);
        add(values, "magnification", Group.GEOMETRY, "Magnification", "Positive scale applied to the centered child when the magnifier is enabled.", order++);
        add(values, "overAndUnderCenterOpacity", Group.GEOMETRY, "Over/under opacity", "Opacity for children above and below the center, from 0 through 1.", order++);
        add(values, "itemExtent", Group.GEOMETRY, "Item extent", "Positive logical-pixel extent of every wheel child.", order++);
        add(values, "squeeze", Group.GEOMETRY, "Squeeze", "Positive vertical squeeze factor between wheel children.", order++);
        add(values, "onSelectedItemChanged", Group.BEHAVIOR, "On selected item changed", "Optional callback invoked when the centered child index changes.", order++);
        add(values, "renderChildrenOutsideViewport", Group.APPEARANCE, "Render outside viewport", "Whether children outside the viewport are painted; requires Clip.none in the SDK.", order++);
        add(values, "clipBehavior", Group.APPEARANCE, "Clip behavior", "How content outside the viewport is clipped.", order++);
        add(values, "hitTestBehavior", Group.BEHAVIOR, "Hit-test behavior", "Whether the wheel is opaque or translucent to pointer hit testing.", order++);
        add(values, "restorationId", Group.RESTORATION, "Restoration ID", "Stable non-empty identifier used to restore the wheel position.", order++);
        add(values, "scrollBehavior", Group.BEHAVIOR, "Scroll behavior", "Optional ScrollBehavior project reference.", order++);
        add(values, "dragStartBehavior", Group.SCROLLING, "Drag start behavior", "Whether drag coordinates begin at pointer down or drag recognition.", order++);
        add(values, "changeReportingBehavior", Group.BEHAVIOR, "Change reporting behavior", "Whether selection callbacks report during updates or only when scrolling ends.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ListWheelScrollView schema count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        add(values, name, group, displayName, description, Target.DIRECT, name, order);
    }

    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, Target target, String dartName, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, target, dartName, order)) != null) {
            throw new IllegalStateException("Duplicate ListWheelScrollView property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

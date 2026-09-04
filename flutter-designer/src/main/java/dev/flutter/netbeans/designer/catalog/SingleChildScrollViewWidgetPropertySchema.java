package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of {@code SingleChildScrollView} in Flutter
 * 3.44.8.
 *
 * <p>The runtime-owned {@code controller} and framework {@code key} are
 * deliberately excluded. The remaining ten constructor properties and the
 * optional child slot preserve their Flutter defaults when omitted.</p>
 */
public final class SingleChildScrollViewWidgetPropertySchema {
    public static final WidgetTypeId SINGLE_CHILD_SCROLL_VIEW_TYPE =
            new WidgetTypeId("flutter.widgets.SingleChildScrollView");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 10;
    public static final int SLOT_COUNT = 1;
    public static final List<String> PHYSICS_PRESETS = List.of(
            "alwaysScrollable",
            "bouncing",
            "clamping",
            "neverScrollable",
            "page",
            "rangeMaintaining");

    public enum Group {
        SCROLLING("singleChildScrollViewScrolling", "Scrolling",
                "Axis, direction, primary-controller policy, physics, and drag behavior."),
        LAYOUT("singleChildScrollViewLayout", "Layout",
                "Padding and clipping around the single scrolling child."),
        SEMANTICS("singleChildScrollViewSemantics", "Semantics",
                "Pointer hit-testing behavior for the scrollable viewport."),
        RESTORATION("singleChildScrollViewRestoration", "Restoration",
                "Stable scroll-position restoration and keyboard dismissal behavior.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return setName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    public enum Target {
        DIRECT,
        PHYSICS_PRESET
    }

    /** Metadata for one exact persisted SingleChildScrollView property. */
    public record Definition(
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int dartOrder) {

        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            Objects.requireNonNull(target, "target");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("dartOrder must be non-negative");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private SingleChildScrollViewWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static boolean isSynthesized(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    /** Returns the ten reviewed leaves in constructor argument order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        int order = 0;
        add(values, "scrollDirection", Group.SCROLLING, "Scroll direction",
                "Horizontal or vertical main scroll axis; omission preserves vertical.", order++);
        add(values, "reverse", Group.SCROLLING, "Reverse",
                "Whether scrolling proceeds in the reading-direction reverse.", order++);
        add(values, "padding", Group.LAYOUT, "Padding",
                "Non-negative insets around the scrolling child.", order++);
        add(values, "primary", Group.SCROLLING, "Primary",
                "Whether the view obtains its controller from PrimaryScrollController.", order++);
        add(values, "physics", Group.SCROLLING, "Scroll physics",
                "Reviewed static ScrollPhysics preset; omission preserves Flutter inference.",
                Target.PHYSICS_PRESET, "physics", order++);
        // The excluded controller does not consume a reviewed Designer position.
        // Reviewed constructor position 5 belongs to the child slot.
        order++;
        add(values, "dragStartBehavior", Group.SCROLLING, "Drag start behavior",
                "Whether drag coordinates begin at pointer down or drag recognition.", order++);
        add(values, "clipBehavior", Group.LAYOUT, "Clip behavior",
                "How content outside the viewport is clipped.", order++);
        add(values, "hitTestBehavior", Group.SEMANTICS, "Hit-test behavior",
                "Whether the scrollable is opaque or translucent to pointer hit testing.", order++);
        add(values, "restorationId", Group.RESTORATION, "Restoration ID",
                "Stable non-empty identifier used to restore the scroll position.", order++);
        add(values, "keyboardDismissBehavior", Group.RESTORATION,
                "Keyboard dismissal", "Whether a drag dismisses the on-screen keyboard.",
                order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "SingleChildScrollView schema must expose exactly "
                    + CONSTRUCTOR_PROPERTY_COUNT + " properties; actual=" + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            int dartOrder) {
        add(values, name, group, displayName, description, Target.DIRECT, name, dartOrder);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int dartOrder) {
        if (values.putIfAbsent(name,
                new Definition(group, displayName, description, target, dartName, dartOrder))
                != null) {
            throw new IllegalStateException(
                    "Duplicate SingleChildScrollView property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Designer projection of the static {@code PageView} constructor in Flutter 3.44.8. */
public final class PageViewWidgetPropertySchema {
    public static final WidgetTypeId PAGE_VIEW_TYPE =
            new WidgetTypeId("flutter.widgets.PageView");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 14;
    public static final int SLOT_COUNT = 1;
    public static final List<String> PHYSICS_PRESETS = List.of(
            "alwaysScrollable", "bouncing", "clamping", "neverScrollable", "page", "rangeMaintaining");

    public enum Group {
        SCROLLING("pageViewScrolling", "Scrolling", "Direction, paging, physics, and drag behavior."),
        BEHAVIOR("pageViewBehavior", "Behavior", "Implicit scrolling, hit testing, and project callbacks."),
        CACHING("pageViewCaching", "Caching", "Viewport cache extent and stable restoration metadata."),
        APPEARANCE("pageViewAppearance", "Appearance", "Viewport clipping and page edge padding.");
        private final String setName;
        private final String displayName;
        private final String description;
        Group(String setName, String displayName, String description) {
            this.setName = setName; this.displayName = displayName; this.description = description;
        }
        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public enum Target { DIRECT, PHYSICS_PRESET, SCROLL_CACHE_EXTENT_PIXELS }

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
    private PageViewWidgetPropertySchema() { }
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
        add(values, "scrollDirection", Group.SCROLLING, "Scroll direction", "Horizontal or vertical page axis.", order++);
        add(values, "reverse", Group.SCROLLING, "Reverse", "Whether pages are traversed in reverse.", order++);
        add(values, "controller", Group.SCROLLING, "Controller", "Optional PageController project reference.", order++);
        add(values, "physics", Group.SCROLLING, "Scroll physics", "Reviewed static ScrollPhysics preset; omission preserves Flutter inference.", Target.PHYSICS_PRESET, "physics", order++);
        add(values, "pageSnapping", Group.SCROLLING, "Page snapping", "Whether scrolling settles on page boundaries.", order++);
        add(values, "onPageChanged", Group.BEHAVIOR, "On page changed", "Optional callback invoked after the settled page changes.", order++);
        order++;
        add(values, "dragStartBehavior", Group.SCROLLING, "Drag start behavior", "Whether drag coordinates begin at pointer down or recognition.", order++);
        add(values, "allowImplicitScrolling", Group.BEHAVIOR, "Allow implicit scrolling", "Whether accessibility requests may move to adjacent pages.", order++);
        add(values, "scrollCacheExtent", Group.CACHING, "Cache extent", "Finite non-negative logical pixels cached around the viewport.", Target.SCROLL_CACHE_EXTENT_PIXELS, "scrollCacheExtent", order++);
        add(values, "restorationId", Group.CACHING, "Restoration ID", "Stable non-empty identifier for page-position restoration.", order++);
        add(values, "clipBehavior", Group.APPEARANCE, "Clip behavior", "How content outside the viewport is clipped.", order++);
        add(values, "hitTestBehavior", Group.BEHAVIOR, "Hit-test behavior", "Whether the viewport is opaque or translucent to pointer hit testing.", order++);
        add(values, "scrollBehavior", Group.BEHAVIOR, "Scroll behavior", "Optional ScrollBehavior project reference.", order++);
        add(values, "padEnds", Group.APPEARANCE, "Pad ends", "Whether the first and last pages receive viewport edge padding.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("PageView schema count mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String displayName, String description, int order) {
        add(values, name, group, displayName, description, Target.DIRECT, name, order);
    }
    private static void add(Map<String, Definition> values, String name, Group group, String displayName, String description, Target target, String dartName, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, target, dartName, order)) != null) throw new IllegalStateException("Duplicate PageView property: " + name);
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

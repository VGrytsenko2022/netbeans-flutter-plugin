package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Designer projection of Flutter's CustomScrollView constructor. */
public final class CustomScrollViewWidgetPropertySchema {
    public static final WidgetTypeId CUSTOM_SCROLL_VIEW_TYPE =
            new WidgetTypeId("flutter.widgets.CustomScrollView");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 15;
    public static final int SLOT_COUNT = 1;
    public static final String SLIVER_TRAIT = "flutter.widgets.Sliver";
    public static final List<String> PHYSICS_PRESETS = List.of(
            "alwaysScrollable", "bouncing", "clamping", "neverScrollable", "page", "rangeMaintaining");

    public enum Group {
        SCROLLING("customScrollScrolling", "Scrolling", "Axis, direction, primary policy, physics, and drag behavior."),
        VIEWPORT("customScrollViewport", "Viewport", "Viewport anchor, paint order, cache extent, and clipping."),
        SEMANTICS("customScrollSemantics", "Semantics", "Semantic child count and hit-testing behavior."),
        RESTORATION("customScrollRestoration", "Restoration", "Keyboard dismissal and stable scroll-position restoration metadata.");

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

    private CustomScrollViewWidgetPropertySchema() { }

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
        add(values, "scrollDirection", Group.SCROLLING, "Scroll direction", "Horizontal or vertical main scroll axis; omission preserves vertical.", order++);
        add(values, "reverse", Group.SCROLLING, "Reverse", "Whether scrolling proceeds in the reading-direction reverse.", order++);
        add(values, "controller", Group.SCROLLING, "Controller", "Optional ScrollController project reference.", order++);
        add(values, "primary", Group.SCROLLING, "Primary", "Whether the view obtains its controller from PrimaryScrollController.", order++);
        add(values, "physics", Group.SCROLLING, "Scroll physics", "Reviewed static ScrollPhysics preset; omission preserves Flutter inference.", Target.PHYSICS_PRESET, "physics", order++);
        add(values, "shrinkWrap", Group.SCROLLING, "Shrink wrap", "Whether the viewport sizes itself to its slivers along the scroll axis.", 6);
        add(values, "anchor", Group.VIEWPORT, "Anchor", "Fractional anchor for the zero scroll offset, from 0 through 1.", 8);
        add(values, "scrollCacheExtent", Group.VIEWPORT, "Cache extent", "Finite non-negative logical pixels cached before and after the viewport.", Target.SCROLL_CACHE_EXTENT_PIXELS, "scrollCacheExtent", 10);
        add(values, "paintOrder", Group.VIEWPORT, "Sliver paint order", "Whether the first or last sliver is painted on top.", 11);
        // Flutter constructor position 12 belongs to the slivers slot.
        add(values, "semanticChildCount", Group.SEMANTICS, "Semantic child count", "Non-negative count describing semantic children in the sliver list.", 13);
        add(values, "dragStartBehavior", Group.SCROLLING, "Drag start behavior", "Whether drag coordinates begin at pointer down or drag recognition.", 14);
        add(values, "keyboardDismissBehavior", Group.RESTORATION, "Keyboard dismissal", "Whether a drag dismisses the on-screen keyboard.", 15);
        add(values, "restorationId", Group.RESTORATION, "Restoration ID", "Stable non-empty identifier used to restore the scroll position.", 16);
        add(values, "clipBehavior", Group.VIEWPORT, "Clip behavior", "How content outside the viewport is clipped.", 17);
        add(values, "hitTestBehavior", Group.SEMANTICS, "Hit-test behavior", "Whether the scrollable is opaque or translucent to pointer hit testing.", 18);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("CustomScrollView schema count mismatch");
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
            throw new IllegalStateException("Duplicate CustomScrollView property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

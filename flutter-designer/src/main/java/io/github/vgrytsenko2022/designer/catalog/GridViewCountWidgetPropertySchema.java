package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of the static {@code GridView.count}
 * constructor surface in Flutter 3.44.8.
 *
 * <p>Controller-owned state, delegates, builders, the other named constructors,
 * and the deprecated {@code cacheExtent} are deliberately excluded. The
 * Designer's numeric {@code scrollCacheExtent} is emitted through the current
 * {@code ScrollCacheExtent.pixels} value object.</p>
 */
public final class GridViewCountWidgetPropertySchema {
    public static final WidgetTypeId GRID_VIEW_COUNT_TYPE =
            new WidgetTypeId("flutter.widgets.GridView");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 21;
    public static final int SLOT_COUNT = 1;
    public static final List<String> PHYSICS_PRESETS = List.of(
            "alwaysScrollable",
            "bouncing",
            "clamping",
            "neverScrollable",
            "page",
            "rangeMaintaining");

    public enum Group {
        SCROLLING("gridViewScrolling", "Scrolling",
                "Axis, direction, primary-controller policy, physics, and drag behavior."),
        LAYOUT("gridViewLayout", "Grid layout",
                "Shrink wrapping, padding, tile geometry, and clipping."),
        CACHING("gridViewCaching", "Caching and children",
                "Child lifecycle, repaint boundaries, semantic indexes, and cache extent."),
        SEMANTICS("gridViewSemantics", "Semantics",
                "Semantic child count and hit-testing behavior."),
        RESTORATION("gridViewRestoration", "Restoration",
                "Keyboard dismissal and stable scroll-position restoration metadata.");

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
        PHYSICS_PRESET,
        SCROLL_CACHE_EXTENT_PIXELS
    }

    /** Metadata for one exact persisted GridView.count property. */
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

    private GridViewCountWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static boolean isSynthesized(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    /** Returns the twenty-one reviewed leaves in constructor argument order. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        int order = 0;
        add(values, "scrollDirection", Group.SCROLLING, "Scroll direction",
                "Horizontal or vertical main scroll axis; omission preserves vertical.", order++);
        add(values, "reverse", Group.SCROLLING, "Reverse",
                "Whether scrolling proceeds in the reading-direction reverse.", order++);
        add(values, "primary", Group.SCROLLING, "Primary",
                "Whether GridView obtains its controller from PrimaryScrollController.", order++);
        add(values, "physics", Group.SCROLLING, "Scroll physics",
                "Reviewed static ScrollPhysics preset; omission preserves Flutter inference.",
                Target.PHYSICS_PRESET, "physics", order++);
        add(values, "shrinkWrap", Group.LAYOUT, "Shrink wrap",
                "Whether the scroll view sizes itself to its contents along the scroll axis.",
                order++);
        add(values, "padding", Group.LAYOUT, "Padding",
                "Non-negative insets around the ordered child list.", order++);
        add(values, "crossAxisCount", Group.LAYOUT, "Cross-axis count",
                "Positive number of tiles in the cross axis.", order++);
        add(values, "mainAxisSpacing", Group.LAYOUT, "Main-axis spacing",
                "Finite non-negative logical pixels between tiles in the main axis.", order++);
        add(values, "crossAxisSpacing", Group.LAYOUT, "Cross-axis spacing",
                "Finite non-negative logical pixels between tiles in the cross axis.", order++);
        add(values, "childAspectRatio", Group.LAYOUT, "Child aspect ratio",
                "Finite positive cross-axis to main-axis extent ratio for each tile.", order++);
        add(values, "mainAxisExtent", Group.LAYOUT, "Main-axis extent",
                "Optional finite non-negative logical-pixel extent for each tile.", order++);
        add(values, "addAutomaticKeepAlives", Group.CACHING, "Automatic keep-alives",
                "Whether the child delegate wraps children in AutomaticKeepAlive widgets.",
                order++);
        add(values, "addRepaintBoundaries", Group.CACHING, "Repaint boundaries",
                "Whether the child delegate isolates children with repaint boundaries.", order++);
        add(values, "addSemanticIndexes", Group.CACHING, "Semantic indexes",
                "Whether the child delegate annotates children with semantic indexes.", order++);
        add(values, "scrollCacheExtent", Group.CACHING, "Cache extent",
                "Finite non-negative logical pixels cached before and after the viewport.",
                Target.SCROLL_CACHE_EXTENT_PIXELS, "scrollCacheExtent", order++);
        // Flutter constructor position 15 belongs to the children slot.
        order++;
        add(values, "semanticChildCount", Group.SEMANTICS, "Semantic child count",
                "Non-negative count no greater than the current number of children.", order++);
        add(values, "dragStartBehavior", Group.SCROLLING, "Drag start behavior",
                "Whether drag coordinates begin at pointer down or drag recognition.", order++);
        add(values, "keyboardDismissBehavior", Group.RESTORATION,
                "Keyboard dismissal", "Whether a drag dismisses the on-screen keyboard.",
                order++);
        add(values, "restorationId", Group.RESTORATION, "Restoration ID",
                "Stable non-empty identifier used to restore the scroll position.", order++);
        add(values, "clipBehavior", Group.LAYOUT, "Clip behavior",
                "How content outside the viewport is clipped.", order++);
        add(values, "hitTestBehavior", Group.SEMANTICS, "Hit-test behavior",
                "Whether the scrollable is opaque or translucent to pointer hit testing.", order++);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "GridView.count schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
                    + " properties; actual=" + values.size());
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
            throw new IllegalStateException("Duplicate GridView.count property schema: " + name);
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

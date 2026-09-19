package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** All three Flutter 3.44.8 SliverPrototypeExtentList constructors. */
public final class SliverPrototypeExtentListWidgetPropertySchema {
    public enum Kind {
        LIST("flutter.widgets.SliverPrototypeExtentList", "list", 240),
        BUILDER("flutter.widgets.SliverPrototypeExtentList.builder", "builder", 250),
        DELEGATE("flutter.widgets.SliverPrototypeExtentList.delegate", null, 260);
        private final WidgetTypeId type;
        private final String constructor;
        private final int order;
        Kind(String type, String constructor, int order) {
            this.type = new WidgetTypeId(type); this.constructor = constructor; this.order = order;
        }
        public WidgetTypeId type() { return type; }
        public Optional<String> constructor() { return Optional.ofNullable(constructor); }
        public int order() { return order; }
        public boolean hasChildren() { return this == LIST; }
        public String displayName() { return "SliverPrototypeExtentList." + (constructor == null ? "new" : constructor); }
    }
    public static Optional<Kind> find(WidgetTypeId type) {
        return Arrays.stream(Kind.values()).filter(kind -> kind.type().equals(type)).findFirst();
    }
    public static List<SliverDynamicWidgetPropertySchema.Field> fields(Kind kind) {
        return SliverFixedExtentListWidgetPropertySchema.fields(
                SliverFixedExtentListWidgetPropertySchema.Kind.valueOf(kind.name())).stream()
                .filter(field -> !Set.of("itemExtent", "semanticIndexOffset").contains(field.name())).toList();
    }
    public static String description(SliverDynamicWidgetPropertySchema.Field field) {
        return switch (field.name()) {
            case "findChildIndexCallback" -> "Optional int? Function(Key) project reference or explicit null. Maps a child's key to its new item index when builder children reorder, retaining their state.";
            case "itemCount" -> "Optional non-negative number of builder items. Omission/null is unbounded; the nullable item builder may stop by returning null. A known count improves scroll-extent estimates.";
            default -> field.description();
        };
    }
    private SliverPrototypeExtentListWidgetPropertySchema() {}
}


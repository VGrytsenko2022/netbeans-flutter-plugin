package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** All three Flutter 3.44.8 SliverVariedExtentList constructors. */
public final class SliverVariedExtentListWidgetPropertySchema {
    public enum Kind {
        LIST("flutter.widgets.SliverVariedExtentList", "list", 270),
        BUILDER("flutter.widgets.SliverVariedExtentList.builder", "builder", 280),
        DELEGATE("flutter.widgets.SliverVariedExtentList.delegate", null, 290);
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
        public String displayName() { return "SliverVariedExtentList." + (constructor == null ? "new" : constructor); }
    }
    public static Optional<Kind> find(WidgetTypeId type) {
        return Arrays.stream(Kind.values()).filter(kind -> kind.type().equals(type)).findFirst();
    }
    public static List<SliverDynamicWidgetPropertySchema.Field> fields(Kind kind) {
        var fields = new ArrayList<>(SliverPrototypeExtentListWidgetPropertySchema.fields(
                SliverPrototypeExtentListWidgetPropertySchema.Kind.valueOf(kind.name())));
        fields.add(kind == Kind.LIST ? 0 : 1,
                new SliverDynamicWidgetPropertySchema.Field("itemExtentBuilder", "ItemExtentBuilder", "48"));
        return List.copyOf(fields);
    }

    public static String description(SliverDynamicWidgetPropertySchema.Field field) {
        return switch (field.name()) {
            case "itemExtentBuilder" -> "Required double? Function(int, SliverLayoutDimensions). Return a finite non-negative size for every actual child, and null only beyond the available items. Bind a typed project function/getter/zero-argument factory. The 48 preset is a Designer starting size, not an SDK default; isolated Canvas uses labeled natural sizing for project callbacks. Unset/null is not allowed.";
            case "findChildIndexCallback" -> "Optional int? Function(Key) project reference or explicit null. Maps a child's key to its new item index when builder children reorder, retaining their state.";
            case "itemCount" -> "Optional non-negative number of builder items. Omission/null is unbounded; the nullable item builder may stop by returning null. A known count improves scroll-extent estimates.";
            default -> field.description();
        };
    }
    private SliverVariedExtentListWidgetPropertySchema() {}
}


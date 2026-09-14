package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** All three Flutter 3.44.8 SliverFixedExtentList constructors. */
public final class SliverFixedExtentListWidgetPropertySchema {
    public enum Kind {
        LIST("flutter.widgets.SliverFixedExtentList", "list", 210),
        BUILDER("flutter.widgets.SliverFixedExtentList.builder", "builder", 220),
        DELEGATE("flutter.widgets.SliverFixedExtentList.delegate", null, 230);
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
        public String displayName() { return "SliverFixedExtentList." + (constructor == null ? "new" : constructor); }
    }
    public static Optional<Kind> find(WidgetTypeId type) {
        return Arrays.stream(Kind.values()).filter(kind -> kind.type().equals(type)).findFirst();
    }
    public static List<SliverDynamicWidgetPropertySchema.Field> fields(Kind kind) {
        var extent = new SliverDynamicWidgetPropertySchema.Field("itemExtent", "double", "48");
        if (kind == Kind.DELEGATE) return List.of(
                new SliverDynamicWidgetPropertySchema.Field("delegate", "SliverChildDelegate", "empty"), extent);
        if (kind == Kind.BUILDER) {
            var result = new ArrayList<>(SliverDynamicWidgetPropertySchema.fields(SliverDynamicWidgetPropertySchema.Kind.LIST_BUILDER));
            result.add(1, extent);
            return List.copyOf(result);
        }
        return List.of(extent,
                new SliverDynamicWidgetPropertySchema.Field("addAutomaticKeepAlives", "bool", null),
                new SliverDynamicWidgetPropertySchema.Field("addRepaintBoundaries", "bool", null),
                new SliverDynamicWidgetPropertySchema.Field("addSemanticIndexes", "bool", null));
    }
    public static String description(SliverDynamicWidgetPropertySchema.Field field) {
        return switch (field.name()) {
            case "itemExtent" -> "Required finite non-negative extent of every child in the scroll axis. Zero is valid. Palette creation starts at 48 logical pixels; this is a Designer preset, not a Flutter default. Cross-axis extent follows the viewport.";
            case "findChildIndexCallback" -> "Optional int? Function(Key) project reference or explicit null. Maps a child's key to its new item index when builder children reorder, retaining their state.";
            case "itemCount" -> "Optional non-negative number of builder items. Omission/null is unbounded; the nullable item builder may stop by returning null. A known count improves scroll-extent estimates.";
            default -> field.description();
        };
    }
    private SliverFixedExtentListWidgetPropertySchema() {}
}


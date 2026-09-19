package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Reviewed Flutter 3.44.8 remaining SliverList/SliverGrid constructors. */
public final class SliverDynamicWidgetPropertySchema {
    public enum Kind {
        LIST_BUILDER("flutter.widgets.SliverList.builder", "SliverList", "builder", 110),
        LIST_SEPARATED("flutter.widgets.SliverList.separated", "SliverList", "separated", 120),
        LIST_DELEGATE("flutter.widgets.SliverList.delegate", "SliverList", null, 130),
        GRID_BUILDER("flutter.widgets.SliverGrid.builder", "SliverGrid", "builder", 140),
        GRID_LIST("flutter.widgets.SliverGrid.list", "SliverGrid", "list", 150),
        GRID_DELEGATE("flutter.widgets.SliverGrid.delegate", "SliverGrid", null, 160);
        private final WidgetTypeId type;
        private final String className;
        private final String constructor;
        private final int order;
        Kind(String type, String className, String constructor, int order) {
            this.type = new WidgetTypeId(type); this.className = className;
            this.constructor = constructor; this.order = order;
        }
        public WidgetTypeId type() { return type; }
        public String className() { return className; }
        public Optional<String> constructor() { return Optional.ofNullable(constructor); }
        public int order() { return order; }
        public String displayName() { return className + "." + (constructor == null ? "new" : constructor); }
        public boolean hasChildren() { return this == GRID_LIST; }
    }
    public record Field(String name, String type, String preset) {
        public boolean required() { return preset != null; }
        public List<String> presets() {
            return preset == null || type.equals("double") ? List.of() : type.equals("SliverGridDelegate")
                    ? List.of("fixedCount", "maxExtent") : List.of(preset);
        }
        public String displayName() {
            return name.substring(0, 1).toUpperCase() + name.substring(1).replaceAll("([A-Z])", " $1").toLowerCase();
        }
        public String description() {
            return switch (name) {
                case "itemBuilder" -> "Required Widget? Function(BuildContext, int). Empty preset returns null and creates no items. Bind a typed project function/getter/factory; Canvas never executes it.";
                case "separatorBuilder" -> "Required separator callback. Flutter declares NullableIndexedWidgetBuilder but requires a non-null Widget result at runtime; Designer proves the stronger IndexedWidgetBuilder contract. Shrink preset returns SizedBox.shrink().";
                case "findChildIndexCallback" -> "Optional int? Function(Key) reference or explicit null. In separated lists this deprecated callback returns child indices INCLUDING separators; do not combine with findItemIndexCallback.";
                case "findItemIndexCallback" -> "Optional int? Function(Key) returning ITEM indices, excluding separators. Flutter translates them to child indices; do not combine with findChildIndexCallback.";
                case "delegate" -> "Required SliverChildDelegate project reference/getter/factory, including custom subclasses. Empty preset uses SliverChildListDelegate([]). User-owned delegate configuration stays in project Dart; isolated Canvas cannot execute it.";
                case "gridDelegate" -> "Required SliverGridDelegate reference/getter/factory, including custom subclasses. Fixed count preset uses 2 columns; max extent preset uses 200 logical pixels. Configure arbitrary layout parameters in the referenced project object. Canvas labels custom geometry as an approximation.";
                case "itemCount" -> "Optional non-negative item count; omission/null is unbounded and the item builder may terminate with null. Separated lists use a portable bound so 2 * itemCount - 1 does not overflow.";
                case "semanticIndexOffset" -> "Non-negative starting semantic index. Omission uses zero; coordinate offsets across neighboring slivers.";
                default -> "Optional Flutter child-delegate flag. Omission uses true.";
            };
        }
    }
    public static Optional<Kind> find(WidgetTypeId type) {
        return Arrays.stream(Kind.values()).filter(k -> k.type().equals(type)).findFirst();
    }
    public static List<Field> fields(Kind kind) {
        return switch (kind) {
            case LIST_BUILDER -> List.of(
                    new Field("itemBuilder", "NullableIndexedWidgetBuilder", "empty"),
                    new Field("findChildIndexCallback", "ChildIndexGetter?", null),
                    new Field("itemCount", "int?", null),
                    new Field("addAutomaticKeepAlives", "bool", null),
                    new Field("addRepaintBoundaries", "bool", null),
                    new Field("addSemanticIndexes", "bool", null),
                    new Field("semanticIndexOffset", "int", null));
            case LIST_SEPARATED -> List.of(
                    new Field("itemBuilder", "NullableIndexedWidgetBuilder", "empty"),
                    new Field("findChildIndexCallback", "ChildIndexGetter?", null),
                    new Field("findItemIndexCallback", "ChildIndexGetter?", null),
                    new Field("separatorBuilder", "IndexedWidgetBuilder", "shrink"),
                    new Field("itemCount", "int?", null),
                    new Field("addAutomaticKeepAlives", "bool", null),
                    new Field("addRepaintBoundaries", "bool", null),
                    new Field("addSemanticIndexes", "bool", null));
            case LIST_DELEGATE -> List.of(
                    new Field("delegate", "SliverChildDelegate", "empty"));
            case GRID_BUILDER -> List.of(
                    new Field("gridDelegate", "SliverGridDelegate", "fixedCount"),
                    new Field("itemBuilder", "NullableIndexedWidgetBuilder", "empty"),
                    new Field("findChildIndexCallback", "ChildIndexGetter?", null),
                    new Field("itemCount", "int?", null),
                    new Field("addAutomaticKeepAlives", "bool", null),
                    new Field("addRepaintBoundaries", "bool", null),
                    new Field("addSemanticIndexes", "bool", null),
                    new Field("semanticIndexOffset", "int", null));
            case GRID_LIST -> List.of(
                    new Field("gridDelegate", "SliverGridDelegate", "fixedCount"),
                    new Field("addAutomaticKeepAlives", "bool", null),
                    new Field("addRepaintBoundaries", "bool", null),
                    new Field("addSemanticIndexes", "bool", null),
                    new Field("semanticIndexOffset", "int", null));
            case GRID_DELEGATE -> List.of(
                    new Field("delegate", "SliverChildDelegate", "empty"),
                    new Field("gridDelegate", "SliverGridDelegate", "fixedCount"));
        };
    }
    public static Optional<String> conflict(WidgetNode node) {
        if (!node.type().equals(Kind.LIST_SEPARATED.type())) return Optional.empty();
        boolean child = node.properties().get(new PropertyName("findChildIndexCallback")) instanceof PropertyValue.DartObjectReferenceValue;
        boolean item = node.properties().get(new PropertyName("findItemIndexCallback")) instanceof PropertyValue.DartObjectReferenceValue;
        return child && item ? Optional.of("SliverList.separated cannot combine findChildIndexCallback with findItemIndexCallback, even nullable references. Reset one or set it to explicit null; no value was cleared.") : Optional.empty();
    }
    private SliverDynamicWidgetPropertySchema() {}
}

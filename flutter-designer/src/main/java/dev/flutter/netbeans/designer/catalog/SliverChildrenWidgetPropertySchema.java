package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Complete static children constructor surfaces in the pinned Flutter SDK. */
public final class SliverChildrenWidgetPropertySchema {
    public static final WidgetTypeId LIST = new WidgetTypeId("flutter.widgets.SliverList");
    public static final WidgetTypeId GRID_COUNT = new WidgetTypeId("flutter.widgets.SliverGrid");
    public static final WidgetTypeId GRID_EXTENT = new WidgetTypeId("flutter.widgets.SliverGrid.extent");
    public static final Set<WidgetTypeId> TYPES = Set.of(LIST, GRID_COUNT, GRID_EXTENT);

    public record Definition(String displayName, String description) { }

    private SliverChildrenWidgetPropertySchema() { }

    public static Map<String, Definition> definitions(WidgetTypeId type) {
        LinkedHashMap<String, Definition> fields = new LinkedHashMap<>();
        if (LIST.equals(type)) {
            fields.put("addAutomaticKeepAlives", new Definition("Automatic keep-alives",
                    "Preserve children that request keep-alive. Omission uses Flutter's true default."));
            fields.put("addRepaintBoundaries", new Definition("Repaint boundaries",
                    "Isolate child repainting. Omission uses Flutter's true default."));
            fields.put("addSemanticIndexes", new Definition("Semantic indexes",
                    "Annotate children for accessibility. Omission uses Flutter's true default."));
        } else if (GRID_COUNT.equals(type) || GRID_EXTENT.equals(type)) {
            boolean extent = GRID_EXTENT.equals(type);
            fields.put(extent ? "maxCrossAxisExtent" : "crossAxisCount", new Definition(
                    extent ? "Maximum cross-axis extent" : "Cross-axis count",
                    extent ? "Required positive maximum tile width on a vertical grid, or height on a horizontal grid."
                            : "Required positive number of tiles in the cross axis."));
            fields.put("mainAxisSpacing", new Definition("Main-axis spacing",
                    "Finite non-negative spacing along the scroll axis. Omission uses zero."));
            fields.put("crossAxisSpacing", new Definition("Cross-axis spacing",
                    "Finite non-negative spacing across the scroll axis. Omission uses zero."));
            fields.put("childAspectRatio", new Definition("Child aspect ratio",
                    "Finite positive ratio of cross-axis extent to main-axis extent. Omission uses one."));
        } else {
            throw new IllegalArgumentException("Not a static children sliver: " + type);
        }
        return java.util.Collections.unmodifiableMap(fields);
    }

    public static Optional<Definition> find(WidgetTypeId type, PropertyName name) {
        return Optional.ofNullable(definitions(type).get(name.value()));
    }
}


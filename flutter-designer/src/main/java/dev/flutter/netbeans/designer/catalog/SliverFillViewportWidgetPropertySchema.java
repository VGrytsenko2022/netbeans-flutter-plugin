package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Flutter 3.44.8 native constructor: visual children and project-owned delegate variants. */
public final class SliverFillViewportWidgetPropertySchema {
    public static final WidgetTypeId CHILDREN = new WidgetTypeId("flutter.widgets.SliverFillViewport");
    public static final WidgetTypeId DELEGATE = new WidgetTypeId("flutter.widgets.SliverFillViewport.delegate");
    public static final Set<WidgetTypeId> TYPES = Set.of(CHILDREN, DELEGATE);
    public record Field(String name, String label, String description) {}
    public static List<Field> fields(WidgetTypeId type) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Not SliverFillViewport: " + type);
        var result = new ArrayList<>(List.of(
                new Field("viewportFraction", "Viewport fraction",
                        "Finite positive fraction of the viewport main-axis extent per child. Omission uses 1.0. Values greater than one are valid."),
                new Field("padEnds", "Pad ends",
                        "Center the first and last child with end padding when viewport fraction is less than one. Omission uses true; no effect at fractions of one or greater."),
                new Field("allowImplicitScrolling", "Allow implicit scrolling",
                        "Allow native implicit accessibility scrolling. Omission uses true.")));
        if (DELEGATE.equals(type)) result.add(new Field("delegate", "Delegate",
                "Required SliverChildDelegate: empty preset, or typed project object/getter/zero-argument factory. Supports list, builder and custom subclasses. Configure all delegate options in project Dart. Isolated Canvas never executes project code."));
        else result.addAll(List.of(
                new Field("addAutomaticKeepAlives", "Automatic keep-alives", "SliverChildListDelegate keep-alive wrapping. Omission uses true."),
                new Field("addRepaintBoundaries", "Repaint boundaries", "SliverChildListDelegate repaint isolation. Omission uses true."),
                new Field("addSemanticIndexes", "Semantic indexes", "SliverChildListDelegate accessibility index wrapping. Omission uses true."),
                new Field("semanticIndexOffset", "Semantic index offset", "Non-negative index offset for this delegate. Omission uses zero."),
                new Field("semanticIndexCallback", "Semantic index callback", "Optional non-null SemanticIndexCallback (Widget, int) -> int? project reference/getter/factory. Omission uses Flutter's local-index callback. Project callback is preserved, not executed in isolated Canvas.")));
        return List.copyOf(result);
    }
    public static List<String> presets(String name) { return name.equals("delegate") ? List.of("empty") : List.of(); }
    private SliverFillViewportWidgetPropertySchema() {}
}


package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 constructor, including the deprecated nullable semantics override. */
public final class SliverIgnorePointerWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverIgnorePointer");
    public static final String DESCRIPTION = "Blocks native pointer hit testing while retaining sliver layout and painting. "
            + "The optional sliver accepts sliver widgets only. An empty slot generates a zero-extent SliverToBoxAdapter "
            + "because Flutter 3.44.8 cannot lay out a null proxy sliver child. Designer selection remains available.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("ignoring", "Ignoring", "Omission uses true. Native pointer events cannot hit this subtree, "
                    + "but layout, painting and scroll extent remain unchanged. This does not block keyboard focus."),
            new Field("ignoringSemantics", "Ignoring semantics (deprecated)", "Deprecated Flutter SDK override. "
                    + "Omission or explicit null uses the modern behavior: Ignoring removes semantic user actions but retains labels. "
                    + "False preserves semantic actions even when Ignoring is true; true removes the entire semantics subtree. "
                    + "Leave unset for new code. Explicit true/false uses a checkbox; null is a separate typed value."));
    public static List<PropertyDefinition> properties() {
        return List.of(new PropertyDefinition(new PropertyName("ignoring"), DartParameter.named(0, false),
                        List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()),
                new PropertyDefinition(new PropertyName("ignoringSemantics"), DartParameter.named(1, false),
                        List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()));
    }
    private SliverIgnorePointerWidgetPropertySchema() {}
}


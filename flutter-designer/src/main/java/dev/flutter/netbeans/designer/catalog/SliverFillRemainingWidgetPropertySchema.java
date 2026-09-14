package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;
import java.util.Optional;

/** Complete Flutter 3.44.8 SliverFillRemaining constructor, except shared identity/key. */
public final class SliverFillRemainingWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverFillRemaining");
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("hasScrollBody", "Has scroll body",
                    "Whether child has a scrollable body. Omission uses Flutter's true default. False measures the child's intrinsic extent and fills the remaining viewport when it is larger; use true for viewport-based children that do not support intrinsic measurement."),
            new Field("fillOverscroll", "Fill overscroll",
                    "Stretch child into overscroll under physics such as BouncingScrollPhysics. Omission uses false. Only effective when Has scroll body is false; the value is still preserved when it is true."));
    public static List<PropertyDefinition> properties() {
        return java.util.stream.IntStream.range(0, FIELDS.size())
                .mapToObj(i -> new PropertyDefinition(new PropertyName(FIELDS.get(i).name()),
                        DartParameter.named(i, false),
                        List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)),
                        Optional.empty()))
                .toList();
    }
    private SliverFillRemainingWidgetPropertySchema() {}
}

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 ScaleTransition constructor apart from shared key. */
public final class ScaleTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ScaleTransition");
    public static final String DESCRIPTION = "Scales the child's painting without changing layout, using required Animation<double>. "
            + "Local signed values create a stopped animation; project references own their live animation and lifetime. "
            + "Zero suppresses paint/hits; negative values flip both axes. Canvas previews project animation at 1 and project Alignment at center.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("scale", "Scale animation", "Required finite signed number or non-null Animation<double> reference/getter/factory. "
                + "Local creates AlwaysStoppedAnimation<double>; edits are immediate. Zero suppresses paint/hits, negative values flip both axes, and layout is unchanged. Creation and project-animation preview use 1."),
        new Field("alignment", "Alignment", "Optional physical Alignment or typed Alignment reference/getter/factory. "
                + "Unset and project-alignment preview use center. Finite coordinates outside [-1,1] are supported; AlignmentDirectional and null are not. Changes are immediate."),
        new Field("filterQuality", "Filter quality", "Optional none, low, medium, high or explicit null. "
                + "Flutter applies the filter while Animation.isAnimating is true (forward/reverse), ignoring it at dismissed/completed. AlwaysStoppedAnimation reports forward, so local constant values and Canvas previews still apply this filter."));
    public static List<PropertyDefinition> properties() {
        var implicit = AnimatedScaleWidgetPropertySchema.properties();
        var scale = implicit.getFirst();
        var constraints = new ArrayList<PropertyValueConstraint>();
        for (var constraint : scale.constraints()) {
            if (!(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues)) constraints.add(constraint);
        }
        constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<double>"));
        return List.of(new PropertyDefinition(scale.name(), scale.parameter(), constraints, scale.creationDefault()),
                implicit.get(1), implicit.get(2));
    }
    private ScaleTransitionWidgetPropertySchema() {}
}

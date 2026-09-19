package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 RotationTransition constructor apart from shared key. */
public final class RotationTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.RotationTransition");
    public static final String DESCRIPTION = "Rotates the child's painting without changing layout, using required Animation<double>. "
            + "Local signed values create a stopped animation; project references own their live animation and lifetime. "
            + "Turns are signed revolutions: 1 is 360 degrees, positive clockwise even in RTL; zero is identity. Canvas previews project animation at 0 and project Alignment at center.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("turns", "Turns animation", "Required finite signed number or non-null Animation<double> reference/getter/factory. "
                + "Local creates AlwaysStoppedAnimation<double>; edits are immediate. One turn is 360 degrees clockwise even in RTL; negative values rotate counterclockwise. No modulo or shortest-path conversion; zero is identity and layout is unchanged. Creation and project-animation preview use 0."),
        new Field("alignment", "Alignment", "Optional physical Alignment or typed Alignment reference/getter/factory. "
                + "Unset and project-alignment preview use center. Finite coordinates outside [-1,1] are supported; AlignmentDirectional and null are not. Changes are immediate."),
        new Field("filterQuality", "Filter quality", "Optional none, low, medium, high or explicit null. "
                + "Flutter applies the filter while Animation.isAnimating is true (forward/reverse), ignoring it at dismissed/completed. AlwaysStoppedAnimation reports forward, so local constant values and Canvas previews still apply this filter."));
    public static List<PropertyDefinition> properties() {
        var implicit = AnimatedRotationWidgetPropertySchema.properties();
        var turns = implicit.getFirst();
        var constraints = new ArrayList<PropertyValueConstraint>();
        for (var constraint : turns.constraints()) {
            if (!(constraint instanceof PropertyValueConstraint.DartObjectReferenceValues)) constraints.add(constraint);
        }
        constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<double>"));
        return List.of(new PropertyDefinition(turns.name(), turns.parameter(), constraints, turns.creationDefault()),
                implicit.get(1), implicit.get(2));
    }
    private RotationTransitionWidgetPropertySchema() {}
}

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Shared complete Flutter 3.44.8 box/sliver explicit opacity animation contract. */
public final class FadeTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.FadeTransition");
    public static final WidgetTypeId SLIVER_TYPE = new WidgetTypeId("flutter.widgets.SliverFadeTransition");
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || SLIVER_TYPE.equals(type); }
    public static final String DESCRIPTION = "Controls transparency with a required Animation<double>. "
            + "A local value from 0 to 1 creates a stopped animation; a typed Dart animation/getter/factory supplies live updates. "
            + "Project source owns its controller and lifetime. Canvas never executes project code and previews references at opacity 1. "
            + "Zero opacity retains layout and pointer hits. There are no Duration, Curve or On end constructor arguments.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("opacity", "Opacity animation", "Required local number from 0 to 1, or non-null Animation<double> reference/getter/factory. "
                + "Local creates AlwaysStoppedAnimation<double>; edits are immediate, not implicitly animated. Canvas previews project animations at 1."),
        new Field("alwaysIncludeSemantics", "Always include semantics", "Expose child semantics even at zero opacity. Unset uses false; explicit values use a checkbox."));
    public static List<PropertyDefinition> properties() {
        var numeric = SliverOpacityWidgetPropertySchema.opacity();
        var constraints = new ArrayList<>(numeric.constraints());
        constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<double>"));
        return List.of(new PropertyDefinition(numeric.name(), numeric.parameter(), constraints, numeric.creationDefault()),
                SliverOpacityWidgetPropertySchema.properties().get(1));
    }
    private FadeTransitionWidgetPropertySchema() {}
}

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

/** Complete Flutter 3.44.8 MatrixTransition constructor apart from shared Key. */
public final class MatrixTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.MatrixTransition");
    public static final String DESCRIPTION = "Computes a Matrix4 from required Animation<double> without changing Child layout. "
            + "Local animation is stopped; a local matrix creates a callback returning a fresh fixed matrix. "
            + "Typed project TransformCallback supports arbitrary value-dependent 2D/3D transformations. "
            + "Canvas never executes project code and labels its preview substitutions.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("animation", "Animation", "Required finite signed local value or non-null Animation<double> reference/getter/factory. "
                + "The value is passed to On transform; its units and range belong to that callback. Local uses AlwaysStoppedAnimation<double>, starting at 0. Project source preview uses 0."),
        new Field("onTransform", "On transform", "Required TransformCallback: Matrix4 Function(double animationValue). "
                + "Edit all 16 finite column-major matrix entries locally, or bind/create a typed project callback. "
                + "Local returns a fresh fixed matrix and ignores animationValue; project code can implement any value-dependent transform. "
                + "This is a computation delegate, not an Event. Project callback preview uses identity."),
        new Field("alignment", "Alignment", "Optional physical Alignment or non-null Alignment reference/getter/factory. "
                + "Unset and project preview use center. Signed finite coordinates outside [-1,1] are valid; directional alignment and explicit null are not supported by Flutter."),
        new Field("filterQuality", "Filter quality", "Optional none/low/medium/high, null or unset. "
                + "Native Flutter applies the filter only while animation.isAnimating is true. AlwaysStoppedAnimation reports forward, so local previews apply it. "
                + "Layout is unchanged, painting is not clipped, and native hit tests follow the matrix."));
    public static PropertyValue.Matrix4Value identity() {
        return new PropertyValue.Matrix4Value(IntStream.range(0, 16).mapToObj(i -> i % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO).toList());
    }
    public static List<PropertyDefinition> properties() {
        var rotation = RotationTransitionWidgetPropertySchema.properties();
        return List.of(
            new PropertyDefinition(new PropertyName("animation"), DartParameter.named(0, true), rotation.getFirst().constraints(), rotation.getFirst().creationDefault()),
            new PropertyDefinition(new PropertyName("onTransform"), DartParameter.named(1, true),
                List.of(new PropertyValueConstraint.Matrix4Values(), new PropertyValueConstraint.DartObjectReferenceValues("TransformCallback")), Optional.of(identity())),
            new PropertyDefinition(new PropertyName("alignment"), DartParameter.named(2, false), rotation.get(1).constraints(), Optional.empty()),
            new PropertyDefinition(new PropertyName("filterQuality"), DartParameter.named(3, false), rotation.get(2).constraints(), Optional.empty()));
    }
    private MatrixTransitionWidgetPropertySchema() {}
}

package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 SizeTransition constructor apart from shared key. */
public final class SizeTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SizeTransition");
    public static final String DESCRIPTION = "Animates the clipped layout size along one axis using required Animation<double>. "
            + "Local values create a constant animation. Negative size factors render as zero; tight parent constraints can prevent shrinking. "
            + "Alignment and deprecated Axis alignment are alternative strategies. Canvas does not execute project references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("axis", "Axis", "Horizontal changes width; vertical changes height. Omission uses vertical."),
        new Field("sizeFactor", "Size animation", "Required finite signed local number or non-null Animation<double> reference/getter/factory. Local values create AlwaysStoppedAnimation<double> and update immediately. Flutter uses max(value, 0); values above 1 are allowed. Creation and project-animation preview use 1."),
        new Field("axisAlignment", "Axis alignment (deprecated)", "Optional signed number, explicit null or typed double? reference/getter/factory. Still supported by Flutter 3.44.8. -1 is start/top, 0 center, 1 end/bottom; other finite values are allowed. Horizontal start/end follow RTL. Reset Alignment or set it to explicit null before using this legacy strategy. Project references preview as null."),
        new Field("alignment", "Alignment", "Optional physical Alignment, RTL-aware AlignmentDirectional, explicit null or typed AlignmentGeometry? reference/getter/factory. Reset Axis alignment or set it to explicit null first. With both absent/null, the pinned SDK centers the main axis and aligns the cross axis to directional start/top, not physical center. Project references preview as null."),
        new Field("fixedCrossAxisSizeFactor", "Fixed cross-axis size factor", "Optional finite nonnegative fraction, explicit null or typed double? reference/getter/factory. Omission/null fills bounded cross-axis space and shrink-wraps unbounded space. Values below 1 clip; above 1 expand subject to parent constraints. Project references preview as null; source code owns runtime nonnegative validity."));
    public static List<PropertyDefinition> properties() {
        return List.of(
            new PropertyDefinition(new PropertyName("axis"), DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "Axis"), List.of("horizontal", "vertical"))), Optional.empty()),
            number("sizeFactor", 1, true, false, false, "Animation<double>", Optional.of(new PropertyValue.DoubleValue(BigDecimal.ONE))),
            number("axisAlignment", 2, false, false, true, "double?", Optional.empty()),
            new PropertyDefinition(new PropertyName("alignment"), DartParameter.named(3, false),
                List.of(new PropertyValueConstraint.AlignmentGeometryValues(),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                    new PropertyValueConstraint.DartObjectReferenceValues("AlignmentGeometry?")), Optional.empty()),
            number("fixedCrossAxisSizeFactor", 4, false, true, true, "double?", Optional.empty()));
    }
    private static PropertyDefinition number(String name, int order, boolean required, boolean nonnegative, boolean nullable, String type, Optional<PropertyValue> initial) {
        var constraints = new ArrayList<PropertyValueConstraint>();
        constraints.add(new PropertyValueConstraint.IntegerRange(new BigInteger(nonnegative ? "0" : "-9007199254740991"), new BigInteger("9007199254740991")));
        constraints.add(new PropertyValueConstraint.DoubleRange(nonnegative ? BigDecimal.ZERO : null, true, null, true));
        if (nullable) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(type));
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, required), constraints, initial);
    }
    public static boolean nonNull(WidgetNode node, String name) {
        var value = node.properties().get(new PropertyName(name));
        return value != null && !(value instanceof PropertyValue.NullValue);
    }
    public static boolean conflictingAlignment(WidgetNode node) {
        return nonNull(node, "axisAlignment") && nonNull(node, "alignment");
    }
    private SizeTransitionWidgetPropertySchema() {}
}

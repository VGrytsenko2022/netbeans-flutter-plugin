package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Pinned Flutter 3.44.8 AnimatedContainer, including inherited animation arguments. */
public final class AnimatedContainerWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedContainer");
    public static final String DESCRIPTION = "Animates alignment, insets, decorations, constraints and transform. "
            + "Color is converted to a background BoxDecoration; width and height tighten constraints. "
            + "Duration is required (creation: 300 ms); child is optional. Null properties do not animate. "
            + "Clip behavior changes immediately. Canvas never executes project-owned references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("alignment", "Alignment", "Physical or directional AlignmentGeometry, explicit null, or typed reference. Animates using native text direction."),
        new Field("padding", "Padding", "Nonnegative physical or directional insets, explicit null, or typed EdgeInsetsGeometry reference. Decoration padding also contributes."),
        new Field("color", "Color", "Literal or theme color, null, or typed Color reference. Exclusive with non-null background Decoration. Flutter converts color to BoxDecoration, which can supply clipping."),
        new Field("decoration", "Decoration", "Structured BoxDecoration, null, or typed Decoration reference (including ShapeDecoration/custom Decoration). Exclusive with non-null Color."),
        new Field("foregroundDecoration", "Foreground decoration", "Structured BoxDecoration, null, or arbitrary typed Decoration reference. Painted above child; does not supply background clipping."),
        new Field("width", "Width", "Nonnegative width, positive infinity, null, or typed double reference. Tightens existing constraints using Flutter clamping; does not animate independently of Constraints."),
        new Field("height", "Height", "Nonnegative height, positive infinity, null, or typed double reference. Infinite dimensions require finite parent constraints."),
        new Field("constraints", "Constraints", "Structured BoxConstraints, null, or typed reference. Width/height tighten these bounds using Flutter's native rules."),
        new Field("margin", "Margin", "Nonnegative physical or directional outer insets, null, or typed EdgeInsetsGeometry reference."),
        new Field("transform", "Transform", "Structured Matrix4, null, or typed Matrix4 reference. Uses native Matrix4Tween; affects paint and hit testing, not layout."),
        new Field("transformAlignment", "Transform alignment", "Physical or directional transform pivot, null, or typed AlignmentGeometry reference."),
        new Field("clipBehavior", "Clip behavior", "Immediate Clip setting, default none. A non-none clip requires non-null background Color or Decoration; foreground decoration alone is insufficient."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference. Default linear. Native overshoot is preserved."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference. Creation/custom-reference preview: 300000; zero completes immediately."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Configure in Events. Called after completed transitions, not initial mount; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var tokens = MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList();
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
            nullable("alignment", 0, "AlignmentGeometry?", new PropertyValueConstraint.AlignmentGeometryValues()),
            nullable("padding", 1, "EdgeInsetsGeometry?", new PropertyValueConstraint.EdgeInsetsValues(true)),
            nullable("color", 2, "Color?", new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                new PropertyValueConstraint.ThemeTokenValues(tokens)),
            nullable("decoration", 3, "Decoration?", new PropertyValueConstraint.BoxDecorationValues(tokens)),
            nullable("foregroundDecoration", 4, "Decoration?", new PropertyValueConstraint.BoxDecorationValues(tokens)),
            dimension("width", 5), dimension("height", 6),
            nullable("constraints", 7, "BoxConstraints?", new PropertyValueConstraint.BoxConstraintsValues()),
            nullable("margin", 8, "EdgeInsetsGeometry?", new PropertyValueConstraint.EdgeInsetsValues(true)),
            nullable("transform", 9, "Matrix4?", new PropertyValueConstraint.Matrix4Values()),
            nullable("transformAlignment", 10, "AlignmentGeometry?", new PropertyValueConstraint.AlignmentGeometryValues()),
            new PropertyDefinition(new PropertyName("clipBehavior"), DartParameter.named(11, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "Clip"),
                    List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"))), Optional.empty()),
            at(animation.get(1), 12), at(animation.get(2), 13), at(animation.get(3), 14));
    }
    private static PropertyDefinition dimension(String name, int order) {
        return nullable(name, order, "double?", new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, new BigInteger("9007199254740991")),
            new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true),
            new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity")));
    }
    private static PropertyDefinition nullable(String name, int order, String type, PropertyValueConstraint... local) {
        var constraints = new ArrayList<PropertyValueConstraint>(List.of(local));
        constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(type));
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, false), constraints, Optional.empty());
    }
    private static PropertyDefinition at(PropertyDefinition property, int order) {
        return new PropertyDefinition(property.name(), DartParameter.named(order, property.parameter().required()),
            property.constraints(), property.creationDefault());
    }
    private AnimatedContainerWidgetPropertySchema() {}
}


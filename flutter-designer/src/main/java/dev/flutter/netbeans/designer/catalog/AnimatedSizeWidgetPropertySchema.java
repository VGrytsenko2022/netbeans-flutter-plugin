package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.util.*;

/** Complete public AnimatedSize constructor pinned to Flutter 3.44.8. */
public final class AnimatedSizeWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedSize");
    public static final String DESCRIPTION = "Animates its layout size when its optional child changes size. "
        + "Duration is required (creation: 300 ms). Alignment and clipping update immediately. "
        + "Tight constraints prevent size animation; continuously resizing children are tracked directly. "
        + "Canvas never executes project-owned references.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("alignment", "Alignment", "Physical or directional AlignmentGeometry or typed reference. Default center. Finite coordinates outside [-1,1] are valid; null is not. Changes immediately without a separate tween."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or typed Curve reference. Default linear. Native overshoot and parent constraints are preserved."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference. Creation/custom-reference preview: 300000; zero completes immediately."),
        new Field("reverseDurationUs", "Reverse duration (microseconds)", "Optional nonnegative portable integer microseconds, null, or typed Duration? reference. Omission/null uses Duration. Preserved exactly: the pinned Flutter 3.44.8 render object starts ordinary growth and shrink transitions forward, so both use Duration."),
        new Field("clipBehavior", "Clip behavior", "Clip applied to child overflow during size animation. Default hardEdge; none, antiAlias and antiAliasWithSaveLayer are also supported. Paint overflow does not enlarge hit-test bounds."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Configure in Events. Called after a completed size transition, not initial mount; Canvas does not execute it."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
            new PropertyDefinition(new PropertyName("alignment"), DartParameter.named(0, false),
                List.of(new PropertyValueConstraint.AlignmentGeometryValues(),
                    new PropertyValueConstraint.DartObjectReferenceValues("AlignmentGeometry")), Optional.empty()),
            at(animation.get(1), 1), at(animation.get(2), 2),
            new PropertyDefinition(new PropertyName("reverseDurationUs"), DartParameter.named(3, false),
                List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO, new BigInteger("9007199254740991")),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                    new PropertyValueConstraint.DartObjectReferenceValues("Duration?")), Optional.empty()),
            new PropertyDefinition(new PropertyName("clipBehavior"), DartParameter.named(4, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "Clip"),
                    List.of("none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"))), Optional.empty()),
            at(animation.get(3), 5));
    }
    private static PropertyDefinition at(PropertyDefinition property, int order) {
        return new PropertyDefinition(property.name(), DartParameter.named(order, property.parameter().required()),
            property.constraints(), property.creationDefault());
    }
    private AnimatedSizeWidgetPropertySchema() {}
}


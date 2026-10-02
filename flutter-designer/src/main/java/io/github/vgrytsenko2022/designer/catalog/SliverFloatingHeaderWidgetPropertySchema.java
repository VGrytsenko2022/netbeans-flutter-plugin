package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete Flutter 3.44.8 SliverFloatingHeader and AnimationStyle contract. */
public final class SliverFloatingHeaderWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverFloatingHeader");
    public static final List<String> LOCAL_STYLE = List.of("animationStyleDurationUs", "animationStyleCurve",
            "animationStyleReverseDurationUs", "animationStyleReverseCurve");
    public static final String DESCRIPTION = "Content-sized sliver that reveals on forward user scrolling and hides in reverse. "
            + "Child is required. Designer inserts an explicit SizedBox(48 x 48) starter, not an SDK default; replace it through Slots. "
            + "Overlay and Scroll snap modes, both durations and both curves are supported. Defaults are overlay, 300ms and easeInOut. "
            + "Project-owned AnimationStyle, Duration and Curve references remain source-owned and are not executed in Canvas.";
    public static final String CHILD_DESCRIPTION = "Required box child determines the header size. Replace or move another box here; "
            + "clearing or moving this child out is rejected. Slivers cannot be children. The 48 x 48 starter is an explicit Designer preset.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("animationStyle", "Animation style", "Whole typed AnimationStyle reference/factory, noAnimation preset or null. Exclusive with local animation fields. Omission/null uses 300ms and easeInOut."),
        new Field("snapMode", "Snap mode", "Overlay expands over content; Scroll also advances following content. Omission/null uses overlay. Changes affect subsequent animations."),
        new Field("animationStyleDurationUs", "Show duration (microseconds)", "Signed portable microseconds, strict Duration reference/factory or null. Zero completes immediately; negative values are stored but unsafe when native animation starts. Default 300000."),
        new Field("animationStyleCurve", "Show curve", "All 43 pinned Curves presets, strict Curve reference/factory or null. Default easeInOut."),
        new Field("animationStyleReverseDurationUs", "Hide duration (microseconds)", "Independent reverse Duration in signed portable microseconds, reference/factory or null. Default 300000; zero disables hide animation."),
        new Field("animationStyleReverseCurve", "Hide curve", "Independent reverse Curve: all 43 presets, strict Curve reference/factory or null. Default easeInOut."));
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        result.add(property("animationStyle", 0, List.of(
                new PropertyValueConstraint.StringPattern("noAnimation", "reviewed AnimationStyle preset"),
                new PropertyValueConstraint.DartObjectReferenceValues("AnimationStyle?"))));
        result.add(property("snapMode", 1, List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference("package:flutter/widgets.dart", "FloatingHeaderSnapMode"), List.of("overlay", "scroll")))));
        for (String name : LOCAL_STYLE) result.add(property(name, result.size(),
                name.endsWith("Us") ? List.of(
                    new PropertyValueConstraint.IntegerRange(DartNumericLiterals.MAX_PORTABLE_INTEGER.negate(), DartNumericLiterals.MAX_PORTABLE_INTEGER),
                    new PropertyValueConstraint.DartObjectReferenceValues("Duration?")) : List.of(
                    new PropertyValueConstraint.StringPattern(SliverAnimatedOpacityWidgetPropertySchema.curvePattern(), "reviewed Curves preset"),
                    new PropertyValueConstraint.DartObjectReferenceValues("Curve?"))));
        return List.copyOf(result);
    }
    private static PropertyDefinition property(String name, int order, List<PropertyValueConstraint> constraints) {
        var nullable = new ArrayList<>(constraints);
        nullable.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, false), nullable, Optional.empty());
    }
    public static WidgetNode starterChild(StableId owner) {
        var id = new StableId(UUID.nameUUIDFromBytes(("flutter-designer:SliverFloatingHeader:child:" + owner)
                .getBytes(StandardCharsets.UTF_8)));
        var extent = new PropertyValue.IntegerValue(BigInteger.valueOf(48));
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), extent, new PropertyName("height"), extent), Map.of());
    }
    private SliverFloatingHeaderWidgetPropertySchema() {}
}

package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete public constructor pinned to Flutter 3.44.8 (no clipBehavior parameter). */
public final class AnimatedCrossFadeWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedCrossFade");
    public static final String DESCRIPTION = "Cross-fades two required children and animates their size. "
        + "Creation uses showFirst, 300 ms and two explicit replaceable SizedBox children (48 x 48 and 48 x 80); these are Designer presets, not SDK defaults. "
        + "Both subtrees remain mounted. Prefer equal widths; native layout clips changing heights. Canvas never executes project code.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("firstCurve", "First curve", "43 pinned Curves presets or typed Curve reference/factory. Default linear. Flutter inverts this curve to fade the first child out."),
        new Field("secondCurve", "Second curve", "43 pinned Curves presets or typed Curve reference/factory. Default linear; fades the second child in."),
        new Field("sizeCurve", "Size curve", "43 pinned Curves presets or typed Curve reference/factory. Default linear; controls the native AnimatedSize transition."),
        new Field("alignment", "Alignment", "Physical or RTL-aware directional AlignmentGeometry or typed reference/factory. Default topCenter. Finite coordinates outside [-1,1] are supported."),
        new Field("crossFadeState", "Cross-fade state", "Required showFirst or showSecond. Creation uses showFirst; changing state reverses the current transition without replacing either subtree."),
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation and custom-reference Canvas preview use 300000. Zero completes immediately."),
        new Field("reverseDurationUs", "Reverse duration (microseconds)", "Optional nonnegative portable integer microseconds, null, or typed Duration? reference/factory. Controls the reverse cross-fade; omission/null uses Duration."),
        new Field("layoutBuilder", "Layout builder", "Optional non-null AnimatedCrossFadeBuilder reference/factory. Receives topChild, topChildKey, bottomChild and bottomChildKey. Preserve both children and their keys. Events creates an editable native-default layout scaffold. Canvas uses the native default layout, never project code."),
        new Field("excludeBottomFocus", "Exclude bottom focus", "Optional boolean, default true. Excludes the fading/bottom subtree from focus; false keeps its focus traversal enabled. Both subtrees remain mounted."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Configure in Events. Called on completed or dismissed cross-fades, not initial mount; Canvas never executes the handler."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        var size = AnimatedSizeWidgetPropertySchema.properties();
        return List.of(
            renamed(animation.get(1), "firstCurve", 0), renamed(animation.get(1), "secondCurve", 1),
            renamed(animation.get(1), "sizeCurve", 2), at(size.get(0), 3),
            new PropertyDefinition(new PropertyName("crossFadeState"), DartParameter.named(4, true),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/widgets.dart", "CrossFadeState"),
                    List.of("showFirst", "showSecond"))),
                Optional.of(new PropertyValue.EnumValue("CrossFadeState", "showFirst"))),
            at(animation.get(2), 5), at(size.get(3), 6),
            new PropertyDefinition(new PropertyName("layoutBuilder"), DartParameter.named(7, false),
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("AnimatedCrossFadeBuilder")), Optional.empty()),
            new PropertyDefinition(new PropertyName("excludeBottomFocus"), DartParameter.named(8, false),
                List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()),
            at(animation.get(3), 9));
    }
    private static PropertyDefinition renamed(PropertyDefinition property, String name, int order) {
        return new PropertyDefinition(new PropertyName(name), DartParameter.named(order, property.parameter().required()),
            property.constraints(), property.creationDefault());
    }
    private static PropertyDefinition at(PropertyDefinition property, int order) {
        return renamed(property, property.name().value(), order);
    }
    public static WidgetNode starterChild(StableId owner, String slot) {
        if (!Set.of("firstChild", "secondChild").contains(slot)) throw new IllegalArgumentException("Unknown cross-fade slot: " + slot);
        var id = new StableId(UUID.nameUUIDFromBytes(("flutter-designer:AnimatedCrossFade:" + slot + ":" + owner).getBytes(StandardCharsets.UTF_8)));
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.SizedBox"),
            Map.of(new PropertyName("width"), new PropertyValue.IntegerValue(BigInteger.valueOf(48)),
                new PropertyName("height"), new PropertyValue.IntegerValue(BigInteger.valueOf(slot.equals("firstChild") ? 48 : 80))), Map.of());
    }
    private AnimatedCrossFadeWidgetPropertySchema() {}
}

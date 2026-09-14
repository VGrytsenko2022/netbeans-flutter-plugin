package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete public constructor pinned to Flutter 3.44.8. */
public final class AnimatedSwitcherWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedSwitcher");
    public static final String DESCRIPTION = "Transitions between an optional current child and outgoing children. "
        + "Creation uses 300 ms. Initial child is fully visible; same Flutter type/key updates do not animate. "
        + "Durations and curves are captured for each entry. Unlike AnimatedCrossFade, size is not interpolated. "
        + "Canvas never executes project builders. Stable ID is Designer identity, not an application Flutter Key. "
        + "Flutter 3.44.8 may omit outgoing children when default transition keys repeat; Canvas isolates those preview keys and labels the difference.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("durationUs", "Duration (microseconds)", "Required nonnegative portable integer microseconds or typed Duration reference/factory. Creation and custom-reference Canvas preview use 300000. Applied to new entries only; zero completes immediately."),
        new Field("reverseDurationUs", "Reverse duration (microseconds)", "Optional nonnegative portable integer microseconds, null, or typed Duration? reference/factory. Captured for each entry; omission/null uses its Duration. Later edits do not retime existing transitions."),
        new Field("switchInCurve", "Switch-in curve", "All 43 pinned Curves presets or typed Curve reference/factory. Default linear. Captured by each incoming child; an interrupted entrance reverses the same curve."),
        new Field("switchOutCurve", "Switch-out curve", "All 43 pinned Curves presets or typed Curve reference/factory. Default linear. Captured when the child is first installed, not when it starts leaving."),
        new Field("transitionBuilder", "Transition builder", "Optional non-null AnimatedSwitcherTransitionBuilder reference/factory: Widget (Widget child, Animation<double> animation). Callable editor creates an editable native FadeTransition scaffold. Replacing a builder updates all retained transitions. Flutter 3.44.8 repeats default transition keys; an unkeyed outer KeyedSubtree in this builder preserves independent outgoing entries. Canvas uses that isolated default-fade preview."),
        new Field("layoutBuilder", "Layout builder", "Optional non-null AnimatedSwitcherLayoutBuilder reference/factory: Widget (Widget? currentChild, List<Widget> previousChildren). Preserve null current child and every previous child. Callable editor creates a native centered Stack scaffold. Canvas uses native geometry with inert outgoing Designer branches."));
    public static List<PropertyDefinition> properties() {
        var animation=AnimatedOpacityWidgetPropertySchema.properties();
        var size=AnimatedSizeWidgetPropertySchema.properties();
        return List.of(at(animation.get(2),"durationUs",0),at(size.get(3),"reverseDurationUs",1),
            at(animation.get(1),"switchInCurve",2),at(animation.get(1),"switchOutCurve",3),
            builder("transitionBuilder",4,"AnimatedSwitcherTransitionBuilder"),
            builder("layoutBuilder",5,"AnimatedSwitcherLayoutBuilder"));
    }
    private static PropertyDefinition at(PropertyDefinition p,String name,int order) {
        return new PropertyDefinition(new PropertyName(name),DartParameter.named(order,p.parameter().required()),p.constraints(),p.creationDefault());
    }
    private static PropertyDefinition builder(String name,int order,String type) {
        return new PropertyDefinition(new PropertyName(name),DartParameter.named(order,false),
            List.of(new PropertyValueConstraint.DartObjectReferenceValues(type)),Optional.empty());
    }
    private AnimatedSwitcherWidgetPropertySchema() {}
}

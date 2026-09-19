package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 AnimatedModalBarrier constructor apart from shared Key. */
public final class AnimatedModalBarrierWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.AnimatedModalBarrier");
    public static final String DESCRIPTION = "Uses a required color animation to fill bounded parent constraints and block interaction behind it. "
            + "Has no Child. Dismissible defaults to true; without On dismiss Flutter calls Navigator.maybePop. "
            + "Canvas preserves the barrier appearance but suppresses route changes, callbacks and alert sounds.";
    public record Field(String name, String label, String group, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("color", "Color", "Appearance", "Required non-null Animation<Color?>. A local literal/theme color or null color value is wrapped in AlwaysStoppedAnimation; null is a transparent animation, not a null animation. Typed project reference/getter/factory preserves live updates. Source preview uses a stopped null color; the required property cannot be unset."),
        new Field("dismissible", "Dismissible", "Behavior", "Unset preserves true. If true, tapping invokes On dismiss or Navigator.maybePop when that callback is null/unset. False blocks the tap and requests the SDK alert sound. Canvas suppresses these actions."),
        new Field("semanticsLabel", "Semantics label", "Semantics", "Optional string, including empty, null or unset. Native accessible dismissal requires Dismissible, a supported platform and a non-null label; direction comes from ambient Directionality."),
        new Field("barrierSemanticsDismissible", "Barrier semantics dismissible", "Semantics", "Unset and explicit null follow the native platform-dependent semanticsDismissible value; true requests inclusion on supported platforms. False excludes barrier semantics. Physical tapping remains controlled by Dismissible."),
        new Field("onDismiss", "On dismiss", "Events", "Optional VoidCallback? method/reference/getter/factory. Called instead of Navigator.maybePop; your callback owns dismissal. Ignored, but retained, when Dismissible is false. Canvas never executes it."),
        new Field("clipDetailsNotifier", "Clip details notifier", "Semantics", "Optional typed ValueNotifier<EdgeInsets>? project reference/getter/factory, null or unset. Clips only the semantics rect, not paint or physical hit testing. Application code owns notifier updates and disposal; Canvas omits this source-owned notifier."),
        new Field("semanticsOnTapHint", "Semantics on tap hint", "Semantics", "Optional accessibility hint string, including empty, null or unset; the assistive technology supplies the gesture prefix.")
    );
    public static List<PropertyDefinition> properties() {
        return List.of(
            requiredColor(List.of(any(PropertyValueKind.COLOR),new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("Animation<Color?>"))),
            property("dismissible",1,List.of(any(PropertyValueKind.BOOLEAN))),
            property("semanticsLabel",2,List.of(any(PropertyValueKind.STRING),any(PropertyValueKind.NULL))),
            property("barrierSemanticsDismissible",3,List.of(any(PropertyValueKind.BOOLEAN),any(PropertyValueKind.NULL))),
            property("onDismiss",4,List.of(any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback?"))),
            property("clipDetailsNotifier",5,List.of(any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("ValueNotifier<EdgeInsets>?"))),
            property("semanticsOnTapHint",6,List.of(any(PropertyValueKind.STRING),any(PropertyValueKind.NULL)))
        );
    }
    private static PropertyDefinition requiredColor(List<PropertyValueConstraint> constraints) {
        return new PropertyDefinition(new PropertyName("color"), DartParameter.named(0,true),
                constraints, Optional.of(new PropertyValue.NullValue()));
    }
    private static PropertyValueConstraint any(PropertyValueKind kind){return new PropertyValueConstraint.AnyValue(kind);}
    private static PropertyDefinition property(String name,int order,List<PropertyValueConstraint> constraints){
        return new PropertyDefinition(new PropertyName(name),DartParameter.named(order,false),constraints,Optional.empty());
    }
    private AnimatedModalBarrierWidgetPropertySchema(){}
}

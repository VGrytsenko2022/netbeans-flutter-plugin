package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 ModalBarrier constructor apart from shared Key. */
public final class ModalBarrierWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.ModalBarrier");
    public static final String DESCRIPTION = "Fills bounded parent constraints and blocks interaction with widgets behind it. "
            + "Has no Child. Dismissible defaults to true; without On dismiss Flutter calls Navigator.maybePop. "
            + "Canvas preserves the barrier appearance but suppresses route changes, callbacks and alert sounds.";
    public record Field(String name, String label, String group, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("color", "Color", "Appearance", "Optional literal/theme Color, null, or typed Color? project reference/getter/factory. Null/unset is transparent, not an absent hit barrier. Project source previews as transparent."),
        new Field("dismissible", "Dismissible", "Behavior", "Unset preserves true. If true, tapping invokes On dismiss or Navigator.maybePop when that callback is null/unset. False blocks the tap and requests the SDK alert sound. Canvas suppresses these actions."),
        new Field("onDismiss", "On dismiss", "Events", "Optional VoidCallback? method/reference/getter/factory. Called instead of Navigator.maybePop; your callback owns dismissal. Ignored, but retained, when Dismissible is false. Canvas never executes it."),
        new Field("semanticsLabel", "Semantics label", "Semantics", "Optional string, including empty, null or unset. Native accessible dismissal requires Dismissible, a supported platform and a non-null label; direction comes from ambient Directionality."),
        new Field("barrierSemanticsDismissible", "Barrier semantics dismissible", "Semantics", "Unset preserves true; explicit null instead follows the native platform-dependent semanticsDismissible value. False excludes barrier semantics. Physical tapping remains controlled by Dismissible."),
        new Field("clipDetailsNotifier", "Clip details notifier", "Semantics", "Optional typed ValueNotifier<EdgeInsets>? project reference/getter/factory, null or unset. Clips only the semantics rect, not paint or physical hit testing. Application code owns notifier updates and disposal; Canvas omits this source-owned notifier."),
        new Field("semanticsOnTapHint", "Semantics on tap hint", "Semantics", "Optional accessibility hint string, including empty, null or unset; the assistive technology supplies the gesture prefix.")
    );
    public static List<PropertyDefinition> properties() {
        return List.of(
            property("color",0,List.of(any(PropertyValueKind.COLOR),new PropertyValueConstraint.ThemeTokenValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()),any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("Color?"))),
            property("dismissible",1,List.of(any(PropertyValueKind.BOOLEAN))),
            property("onDismiss",2,List.of(any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback?"))),
            property("semanticsLabel",3,List.of(any(PropertyValueKind.STRING),any(PropertyValueKind.NULL))),
            property("barrierSemanticsDismissible",4,List.of(any(PropertyValueKind.BOOLEAN),any(PropertyValueKind.NULL))),
            property("clipDetailsNotifier",5,List.of(any(PropertyValueKind.NULL),new PropertyValueConstraint.DartObjectReferenceValues("ValueNotifier<EdgeInsets>?"))),
            property("semanticsOnTapHint",6,List.of(any(PropertyValueKind.STRING),any(PropertyValueKind.NULL)))
        );
    }
    private static PropertyValueConstraint any(PropertyValueKind kind){return new PropertyValueConstraint.AnyValue(kind);}
    private static PropertyDefinition property(String name,int order,List<PropertyValueConstraint> constraints){
        return new PropertyDefinition(new PropertyName(name),DartParameter.named(order,false),constraints,Optional.empty());
    }
    private ModalBarrierWidgetPropertySchema(){}
}

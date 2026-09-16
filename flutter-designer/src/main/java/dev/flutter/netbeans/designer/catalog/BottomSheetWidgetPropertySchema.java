package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.*;
import java.util.*;

/** Complete pinned Flutter 3.44.8 BottomSheet constructor and editable builder content. */
public final class BottomSheetWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.BottomSheet");
    public static final String DESCRIPTION = "Material bottom sheet with editable Child or a typed WidgetBuilder. "
            + "The caller owns showBottomSheet/showModalBottomSheet, dismissal and AnimationController disposal. "
            + "Creation disables dragging and the handle until a controller is supplied. "
            + "Canvas renders native appearance but never executes project builders, controllers or events.";
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        var shared = DialogWidgetPropertySchema.properties(false).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        for (String name : List.of("key", "animationController", "enableDrag", "showDragHandle", "dragHandleColor",
                "dragHandleSize", "onDragStart", "onDragEnd", "backgroundColor", "shadowColor", "elevation",
                "shape", "clipBehavior", "constraints", "onClosing", "builder")) {
            List<PropertyValueConstraint> constraints = switch (name) {
                case "animationController" -> source("AnimationController?", true);
                case "enableDrag" -> List.of(any(PropertyValueKind.BOOLEAN));
                case "showDragHandle" -> List.of(any(PropertyValueKind.BOOLEAN), any(PropertyValueKind.NULL));
                case "dragHandleColor" -> shared.get("backgroundColor");
                case "dragHandleSize" -> List.of(new PropertyValueConstraint.SizeValues(), any(PropertyValueKind.NULL),
                        new PropertyValueConstraint.DartObjectReferenceValues("Size?"));
                case "onDragStart" -> source("BottomSheetDragStartHandler?", true);
                case "onDragEnd" -> source("BottomSheetDragEndHandler?", true);
                case "onClosing" -> List.of(new PropertyValueConstraint.StringPattern("noop", "no action"),
                        new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback"));
                case "builder" -> List.of(new PropertyValueConstraint.StringPattern("child", "editable Child or empty box"),
                        new PropertyValueConstraint.DartObjectReferenceValues("WidgetBuilder"));
                default -> Objects.requireNonNull(shared.get(name), name);
            };
            Optional<PropertyValue> initial = switch (name) {
                case "enableDrag", "showDragHandle" -> Optional.of(new PropertyValue.BooleanValue(false));
                case "onClosing" -> Optional.of(new PropertyValue.StringValue("noop"));
                case "builder" -> Optional.of(new PropertyValue.StringValue("child"));
                default -> Optional.empty();
            };
            result.add(new PropertyDefinition(new PropertyName(name),
                    DartParameter.named(result.size(), name.equals("onClosing") || name.equals("builder")), constraints, initial));
        }
        for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) {
            result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), false),
                    shared.get(name), Optional.empty()));
        }
        return List.copyOf(result);
    }
    private static PropertyValueConstraint any(PropertyValueKind kind) { return new PropertyValueConstraint.AnyValue(kind); }
    private static List<PropertyValueConstraint> source(String type, boolean nullable) {
        return nullable ? List.of(new PropertyValueConstraint.DartObjectReferenceValues(type), any(PropertyValueKind.NULL))
                : List.of(new PropertyValueConstraint.DartObjectReferenceValues(type));
    }
    public static String help(String name) {
        return switch (name) {
            case "animationController" -> "Typed AnimationController? reference/getter/factory. Flutter mutates its value during dragging. The caller creates and disposes it; Canvas never evaluates it. A nullable source must resolve non-null whenever dragging or a handle is enabled.";
            case "enableDrag" -> "Native default true; Designer creation false. Dragging requires a controller. False does not disable dragging of a visible handle.";
            case "showDragHandle" -> "Native null/unset follows BottomSheetTheme (default false); Designer creation false. A visible handle always requires a controller, even when Enable drag is false.";
            case "dragHandleColor" -> "Literal/theme Color, null, or verified Color?. Omission follows BottomSheetTheme and ColorScheme.onSurfaceVariant.";
            case "dragHandleSize" -> "Non-negative finite Size, null, or verified Size?. Omission follows BottomSheetTheme, then Size(32, 4).";
            case "onClosing" -> "Required VoidCallback. No action is a valid initial/reset preset. May be called more than once and does not guarantee dismissal; the application owns route changes.";
            case "onDragStart" -> "Optional BottomSheetDragStartHandler?: void Function(DragStartDetails details). Fired when a sheet/handle drag begins.";
            case "onDragEnd" -> "Optional BottomSheetDragEndHandler?: void Function(DragEndDetails details, {required bool isClosing}). Fired before On closing when closing.";
            case "builder" -> "Required WidgetBuilder: Widget Function(BuildContext). Child preset wraps the editable Child in a builder, or returns SizedBox.shrink(). Clear Child before selecting project source. Custom source owns its subtree and is never executed by Canvas.";
            case "key" -> "String ValueKey, verified Key?, null or omission; separate from Designer stable identity.";
            case "constraints" -> "BoxConstraints, verified BoxConstraints? or null. Null/unset inherits BottomSheetTheme; Material 3 defaults to maxWidth 640, Material 2 uses parent constraints. A constraint aligns the sheet bottom-center.";
            case "shape" -> "Verified ShapeBorder? or null, exclusive with ten local shape families. Null/unset inherits BottomSheetTheme and native Material defaults.";
            case "elevation" -> "Non-negative number, verified double? or null. Null/unset inherits BottomSheetTheme and native defaults.";
            case "clipBehavior" -> "Nullable Clip override; null/unset inherits BottomSheetTheme then Clip.none.";
            default -> CardWidgetPropertySchema.builtInShapePropertyNames().contains(name)
                    ? DialogWidgetPropertySchema.help(name)
                    : "Literal/theme Color, verified Color? or null. Null/unset inherits BottomSheetTheme and native Material defaults.";
        };
    }
    private BottomSheetWidgetPropertySchema() {}
}

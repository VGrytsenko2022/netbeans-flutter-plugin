package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Flutter 3.44.8: all 17 SimpleDialog and four SimpleDialogOption arguments. */
public final class SimpleDialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.SimpleDialog");
    public static final WidgetTypeId OPTION_TYPE = new WidgetTypeId("flutter.material.SimpleDialogOption");
    public static final String DESCRIPTION = "Material choice dialog with Title and Children slots, native scrolling, "
            + "intrinsic sizing and DialogTheme defaults. Options own their onPressed Events; the caller owns showDialog "
            + "and Navigator.pop results. Canvas never executes project sources.";
    public static final String OPTION_DESCRIPTION = "Selectable dialog option with an optional Child. "
            + "Unset/null onPressed disables selection. The handler may call Navigator.pop(context, result); "
            + "Canvas shows enabled/disabled appearance without executing the handler. Requires a Material ancestor.";
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || OPTION_TYPE.equals(type); }
    public static List<PropertyDefinition> properties(boolean option) {
        var result = new ArrayList<PropertyDefinition>();
        var alert = AlertDialogWidgetPropertySchema.properties(false).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.name().value(), p -> p.constraints()));
        if (option) {
            add(result, "key", alert.get("key"));
            add(result, "onPressed", List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback?"),
                    new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)));
            add(result, "padding", alert.get("insetPadding"));
        } else {
            for (String name : List.of("key", "titlePadding", "titleTextStyle", "contentPadding", "contentTextStyle",
                    "backgroundColor", "elevation", "shadowColor", "surfaceTintColor", "semanticLabel", "insetPadding",
                    "clipBehavior", "shape", "alignment", "constraints")) {
                var constraints = alert.get(name);
                if (name.equals("titlePadding") || name.equals("contentPadding"))
                    constraints = List.of(new PropertyValueConstraint.EdgeInsetsValues(true, true),
                            new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"));
                add(result, name, constraints);
            }
            for (String name : CardWidgetPropertySchema.builtInShapePropertyNames()) add(result, name, alert.get(name));
            for (String family : AlertDialogWidgetPropertySchema.styleFamilies())
                BuiltInWidgetCatalog.appendTextStyleProperties(result, family, result.size());
        }
        if (result.size() != (option ? 3 : 98)) throw new IllegalStateException("SimpleDialog inventory changed: " + result.size());
        return List.copyOf(result);
    }
    private static void add(List<PropertyDefinition> result, String name, List<PropertyValueConstraint> constraints) {
        result.add(new PropertyDefinition(new PropertyName(name), DartParameter.named(result.size(), false),
                Objects.requireNonNull(constraints, name), Optional.empty()));
    }
    public static String help(String name) {
        return switch(name) {
            case "titlePadding" -> "Non-negative physical/directional EdgeInsetsGeometry or verified non-null source. Omission preserves native 24/24/24/0 spacing and text scaling.";
            case "contentPadding" -> "Non-negative physical/directional EdgeInsetsGeometry or verified non-null source. Omission preserves native 0/12/0/16 spacing and text scaling.";
            case "padding" -> "Non-negative physical EdgeInsets, verified EdgeInsets? or null. Null/omission uses horizontal 24 and vertical 8. Directional insets are not accepted by Flutter.";
            case "insetPadding" -> "Non-negative physical EdgeInsets, verified EdgeInsets? or null. Null/omission uses DialogTheme and native defaults.";
            case "semanticLabel" -> "Literal or verified String? for accessibility. Null/omission preserves native localized dialog/title announcements.";
            case "onPressed" -> OPTION_DESCRIPTION;
            default -> AlertDialogWidgetPropertySchema.help(name);
        };
    }
    private SimpleDialogWidgetPropertySchema() { }
}

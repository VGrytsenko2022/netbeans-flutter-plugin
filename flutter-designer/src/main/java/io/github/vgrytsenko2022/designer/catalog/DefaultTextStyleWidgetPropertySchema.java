package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Insertable DefaultTextStyle.new and the non-const static merge factory, Flutter 3.44.8. */
public final class DefaultTextStyleWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.DefaultTextStyle");
    public static final WidgetTypeId MERGE_TYPE = new WidgetTypeId("flutter.widgets.DefaultTextStyle.merge");
    public static boolean supports(WidgetTypeId type) { return TYPE.equals(type) || MERGE_TYPE.equals(type); }
    public static boolean sharesTextProjection(WidgetTypeId type) {
        return supports(type) || DefaultTextStyleTransitionWidgetPropertySchema.TYPE.equals(type)
                || AnimatedDefaultTextStyleWidgetPropertySchema.TYPE.equals(type);
    }
    public static boolean requiresStyle(WidgetTypeId type) {
        return TYPE.equals(type) || DefaultTextStyleTransitionWidgetPropertySchema.TYPE.equals(type)
                || AnimatedDefaultTextStyleWidgetPropertySchema.TYPE.equals(type);
    }
    public static String description(WidgetTypeId type) {
        return (MERGE_TYPE.equals(type)
                ? "Merges nullable overrides with the inherited DefaultTextStyle at the child location. Null means inherit, including Max lines. "
                : "Replaces the inherited default text style immediately. Style and Child are required; null Max lines removes the limit. ")
                + "All local TextStyle fields or a typed whole-style reference are supported. Wrap an existing box child. "
                + "Project references are not executed by Canvas. DefaultTextStyle.fallback is a non-insertable SDK sentinel.";
    }
    public static List<PropertyDefinition> properties(WidgetTypeId type) {
        if (!supports(type)) throw new IllegalArgumentException("Unsupported DefaultTextStyle type: " + type);
        boolean merge = MERGE_TYPE.equals(type);
        var result = new ArrayList<PropertyDefinition>();
        for (var property : AnimatedDefaultTextStyleWidgetPropertySchema.properties()) {
            String name = property.name().value();
            if (Set.of("curve", "durationUs", "onEnd").contains(name)) continue;
            if (merge && name.equals("style")) {
                result.add(new PropertyDefinition(property.name(), DartParameter.named(0, false),
                        List.of(new PropertyValueConstraint.StringPattern("local", "Local TextStyle fields"),
                                new PropertyValueConstraint.DartObjectReferenceValues("TextStyle?"),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)), Optional.empty()));
            } else if (merge && Set.of("softWrap", "overflow", "textWidthBasis").contains(name)) {
                var constraints = new ArrayList<>(property.constraints());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                result.add(new PropertyDefinition(property.name(), property.parameter(), constraints, Optional.empty()));
            } else result.add(property);
        }
        return List.copyOf(result);
    }
    public static String help(WidgetTypeId type, PropertyName name) {
        boolean merge = MERGE_TYPE.equals(type);
        return switch (name.value()) {
            case "style" -> (merge ? "Optional local TextStyle, nullable TextStyle? reference, null or unset. Null/unset inherits the parent style. "
                    : "Required local TextStyle or non-null typed TextStyle reference. No outer DefaultTextStyle merge is invented. ")
                    + "Changing source clears local leaves atomically; editing a local leaf selects local.";
            case "maxLines" -> "Positive portable integer or int? reference. "
                    + (merge ? "Unset/null inherits the ancestor limit; use the ordinary constructor to remove that limit."
                             : "Unset/null permits all lines and replaces any outer limit.");
            case "softWrap", "overflow", "textWidthBasis" -> merge
                    ? "Optional nullable override. Unset/null inherits the corresponding ancestor DefaultTextStyle field."
                    : AnimatedDefaultTextStyleWidgetPropertySchema.help(name);
            case "textHeightBehavior" -> "Whole nullable TextHeightBehavior? reference or three local fields, mutually exclusive. "
                    + (merge ? "Null/unset inherits the ancestor; a local value replaces the whole inherited behavior."
                             : "Null/unset removes this DefaultTextStyle height override; Text may still inherit DefaultTextHeightBehavior. Changes apply immediately.");
            default -> AnimatedDefaultTextStyleWidgetPropertySchema.help(name);
        };
    }
    private DefaultTextStyleWidgetPropertySchema() {}
}

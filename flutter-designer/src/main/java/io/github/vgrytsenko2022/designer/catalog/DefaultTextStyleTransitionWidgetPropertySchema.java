package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 DefaultTextStyleTransition constructor and stopped-style projection. */
public final class DefaultTextStyleTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.DefaultTextStyleTransition");
    public static final String DESCRIPTION = "Animates descendant text through a required Animation<TextStyle> and required box Child. "
            + "Local style creates an AlwaysStoppedAnimation<TextStyle>; all TextStyle fields remain editable. "
            + "A typed project animation/getter/factory supplies live updates in generated Dart; its controller, lifetime and timing remain source-owned. "
            + "Canvas never executes project animations and explicitly previews them as a stopped empty style. "
            + "No Duration, Curve, On end, Text width basis or Text height behavior arguments exist on this constructor.";
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        result.add(new PropertyDefinition(new PropertyName("style"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.StringPattern("local", "Local stopped TextStyle"),
                        new PropertyValueConstraint.DartObjectReferenceValues("Animation<TextStyle>")),
                Optional.of(new PropertyValue.StringValue("local"))));
        for (var property : DefaultTextStyleWidgetPropertySchema.properties(DefaultTextStyleWidgetPropertySchema.TYPE)) {
            if (property.name().value().equals("style")
                    || property.name().value().equals("textWidthBasis")
                    || property.name().value().equals("textHeightBehavior")
                    || AnimatedDefaultTextStyleWidgetPropertySchema.heightLeaf(property.name())) continue;
            result.add(new PropertyDefinition(property.name(), DartParameter.named(result.size(), false),
                    property.constraints(), Optional.empty()));
        }
        return List.copyOf(result);
    }
    public static String help(PropertyName name) {
        return switch (name.value()) {
            case "style" -> "Required non-null Animation<TextStyle> reference/getter/factory, or Local stopped TextStyle. "
                    + "Local creates AlwaysStoppedAnimation<TextStyle> and does not animate edits. Project source owns its controller and lifecycle. "
                    + "Switching to a reference clears local fields atomically; editing a local field selects the stopped local style.";
            case "maxLines" -> "Positive portable integer, int? reference, null or unset. Null/unset permits all lines; unknown Canvas references preview as null.";
            default -> TextWidgetPropertySchema.find(name).map(TextWidgetPropertySchema.Definition::description).orElse("Native paragraph property.");
        };
    }
    private DefaultTextStyleTransitionWidgetPropertySchema() {}
}

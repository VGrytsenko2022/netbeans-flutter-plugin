package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** All public Flutter 3.44.8 DecoratedBoxTransition inputs, excluding shared Key. */
public final class DecoratedBoxTransitionWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.DecoratedBoxTransition");
    public static final String DESCRIPTION = "Animates a required Child's decoration using Animation<Decoration>. "
        + "Local BoxDecoration creates a stopped animation; project animation supports BoxDecoration, ShapeDecoration and custom Decoration. "
        + "Position controls background/foreground painting. This widget adds no padding, clipping or layout constraints. "
        + "Canvas never executes project animations and previews them with an empty BoxDecoration.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("decoration", "Decoration animation", "Required local BoxDecoration or strict Animation<Decoration> reference/getter/factory. "
            + "Local colors, images, borders, radii, shadows, gradients and blend modes use the shared structured editor. "
            + "ShapeDecoration and custom decorations are supported by project animations. Local edits are immediate; controller/tween lifetime remains in Dart."),
        new Field("position", "Position", "Paint behind or in front of Child. Omission uses DecorationPosition.background; foreground changes painting order immediately. "
            + "Decoration does not add padding or clip Child. Both positions retain native decoration hit testing."));
    public static List<PropertyDefinition> properties() {
        var tokens = MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList();
        return List.of(
            new PropertyDefinition(new PropertyName("decoration"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.BoxDecorationValues(tokens),
                    new PropertyValueConstraint.DartObjectReferenceValues("Animation<Decoration>")),
                Optional.of(emptyDecoration())),
            new PropertyDefinition(new PropertyName("position"), DartParameter.named(1, false),
                List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference("package:flutter/rendering.dart", "DecorationPosition"),
                    List.of("background", "foreground"))), Optional.empty()));
    }
    public static PropertyValue.BoxDecorationValue emptyDecoration() {
        return new PropertyValue.BoxDecorationValue(Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }
    private DecoratedBoxTransitionWidgetPropertySchema() {}
}

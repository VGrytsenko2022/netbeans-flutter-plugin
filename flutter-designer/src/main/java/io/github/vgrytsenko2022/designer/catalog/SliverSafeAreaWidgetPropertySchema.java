package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete Flutter 3.44.8 SliverSafeArea constructor. */
public final class SliverSafeAreaWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.SliverSafeArea");
    public static final String DESCRIPTION = "Insets a required sliver using the greater of each physical Minimum inset and the selected MediaQuery padding. "
            + "Selected sides remove the consumed padding from descendants, avoiding duplicate system insets in nested safe areas. "
            + "Uses padding, not viewPadding: this sliver constructor has no maintainBottomViewPadding option. "
            + "Wrap an existing sliver; the required child cannot be cleared or moved out, but can be replaced atomically.";
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
            new Field("left", "Left", "Avoid left system padding; omission uses true. Physical left does not swap in RTL. Minimum still applies when false."),
            new Field("top", "Top", "Avoid top system padding; omission uses true. Minimum still applies when false."),
            new Field("right", "Right", "Avoid right system padding; omission uses true. Physical right does not swap in RTL. Minimum still applies when false."),
            new Field("bottom", "Bottom", "Avoid bottom MediaQuery padding; omission uses true. Does not preserve bottom viewPadding when a keyboard reduces padding. Minimum still applies when false."),
            new Field("minimum", "Minimum", "Physical EdgeInsets lower bounds, not added to system padding. Omission uses EdgeInsets.zero. Finite signed values are accepted by Flutter; negative minima cannot produce negative applied padding. Directional insets and null are not accepted."));
    public static List<PropertyDefinition> properties() {
        var result = new ArrayList<PropertyDefinition>();
        for (var field : FIELDS) result.add(new PropertyDefinition(new PropertyName(field.name()), DartParameter.named(result.size(), false),
                field.name().equals("minimum") ? List.of(new PropertyValueConstraint.EdgeInsetsValues(false, false))
                        : List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN)), Optional.empty()));
        return List.copyOf(result);
    }
    private SliverSafeAreaWidgetPropertySchema() {}
}


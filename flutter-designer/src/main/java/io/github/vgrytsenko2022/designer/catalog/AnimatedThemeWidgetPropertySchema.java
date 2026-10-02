package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;

/** Complete non-key AnimatedTheme constructor pinned to Flutter 3.44.8. */
public final class AnimatedThemeWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.AnimatedTheme");
    public static final String DESCRIPTION = "Animates a complete Material ThemeData for its required Child. "
        + "Data replaces, rather than merges with, the parent theme. Initial mount does not animate. "
        + "Duration is optional and defaults to 200 ms. Wrap an existing box child. "
        + "ThemeData presets are previewed exactly; arbitrary ThemeData references remain source-owned and are not executed in Canvas.";
    public static final List<String> PRESETS = List.of("light", "dark", "fallback", "lightM2", "darkM2", "fallbackM2");
    public record Field(String name, String label, String description) {}
    public static final List<Field> FIELDS = List.of(
        new Field("data", "Theme data", "Required complete ThemeData: light/dark/fallback SDK factories with Material 3 or Material 2, or a typed ThemeData reference/getter/member/zero-argument factory. Creation uses light (Material 3). Arbitrary colors, typography, component themes, extensions and adaptations can be configured in the referenced ThemeData; they are not individually flattened into this Properties sheet. Unknown project ThemeData uses a labeled ThemeData.fallback() in Canvas, not the parent theme."),
        new Field("curve", "Curve", "All 43 pinned Curves presets or a typed Curve reference/factory. Omission uses linear. ThemeData.lerp controls continuous and discrete fields."),
        new Field("durationUs", "Duration (microseconds)", "Optional nonnegative portable integer microseconds or typed Duration reference/factory. Omission uses kThemeAnimationDuration (200000 microseconds), not the 300 ms Designer default used by other animation widgets. Zero completes immediately."),
        new Field("onEnd", "On end", "Optional VoidCallback reference/factory or null. Invoked after each completed transition, not on initial mount. Use Events; Canvas never executes project handlers."));
    public static List<PropertyDefinition> properties() {
        var animation = AnimatedOpacityWidgetPropertySchema.properties();
        return List.of(
            new PropertyDefinition(new PropertyName("data"), DartParameter.named(0, true),
                List.of(new PropertyValueConstraint.StringPattern("(?:" + String.join("|", PRESETS) + ")", "reviewed ThemeData factory"),
                    new PropertyValueConstraint.DartObjectReferenceValues("ThemeData")),
                Optional.of(new PropertyValue.StringValue("light"))),
            at(animation.get(1), 1),
            new PropertyDefinition(new PropertyName("durationUs"), DartParameter.named(2, false), animation.get(2).constraints(), Optional.empty()),
            at(animation.get(3), 3));
    }
    private static PropertyDefinition at(PropertyDefinition p, int order) {
        return new PropertyDefinition(p.name(), DartParameter.named(order,p.parameter().required()),p.constraints(),p.creationDefault());
    }
    private AnimatedThemeWidgetPropertySchema() {}
}

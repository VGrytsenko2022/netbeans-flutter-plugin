package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.List;

/** Complete non-key Theme constructor pinned to Flutter 3.44.8. */
public final class ThemeWidgetPropertySchema {
    public static final WidgetTypeId TYPE = new WidgetTypeId("flutter.material.Theme");
    public static final String DESCRIPTION = "Applies a complete Material ThemeData to its required Child immediately, without animation. "
            + "Data replaces, rather than merges with, the parent theme. Wrap an existing box child. "
            + "ThemeData presets are previewed exactly; arbitrary ThemeData references remain source-owned and are not executed in Canvas.";
    public static final List<String> PRESETS = AnimatedThemeWidgetPropertySchema.PRESETS;

    public record Field(String name, String label, String description) {}

    public static final List<Field> FIELDS = List.of(new Field("data", "Theme data",
            AnimatedThemeWidgetPropertySchema.FIELDS.getFirst().description()));

    public static List<PropertyDefinition> properties() {
        // Share the exact closed ThemeData value domain and light creation preset.
        return List.of(AnimatedThemeWidgetPropertySchema.properties().getFirst());
    }

    private ThemeWidgetPropertySchema() {}
}

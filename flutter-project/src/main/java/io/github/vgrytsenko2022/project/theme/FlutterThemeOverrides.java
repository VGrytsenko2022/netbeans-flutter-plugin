package io.github.vgrytsenko2022.project.theme;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable per-theme role overrides; omitted entries inherit seed/Material defaults. */
public record FlutterThemeOverrides(
        Map<FlutterMaterialColorRole, Integer> colorScheme,
        Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> textTheme,
        Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> componentColors) {

    public static final FlutterThemeOverrides EMPTY =
            new FlutterThemeOverrides(Map.of(), Map.of(), Map.of());

    /** Source-compatible schema-v4 constructor with no component colors. */
    public FlutterThemeOverrides(
            Map<FlutterMaterialColorRole, Integer> colorScheme,
            Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> textTheme) {
        this(colorScheme, textTheme, Map.of());
    }

    public FlutterThemeOverrides {
        Objects.requireNonNull(colorScheme, "colorScheme");
        Objects.requireNonNull(textTheme, "textTheme");
        Objects.requireNonNull(componentColors, "componentColors");
        EnumMap<FlutterMaterialColorRole, Integer> colors =
                new EnumMap<>(FlutterMaterialColorRole.class);
        colorScheme.forEach((role, argb) -> colors.put(
                Objects.requireNonNull(role, "colorScheme role"),
                Objects.requireNonNull(argb, "colorScheme ARGB")));
        EnumMap<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> styles =
                new EnumMap<>(FlutterMaterialTextStyleRole.class);
        textTheme.forEach((role, style) -> {
            Objects.requireNonNull(role, "textTheme role");
            Objects.requireNonNull(style, "textTheme style");
            if (style.isEmpty()) {
                throw new IllegalArgumentException(
                        "Empty TextTheme role overrides must be omitted: " + role.wireName());
            }
            styles.put(role, style);
        });
        colorScheme = Map.copyOf(colors);
        textTheme = Map.copyOf(styles);
        EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> components =
                new EnumMap<>(FlutterThemeComponentColorRole.class);
        componentColors.forEach((role, color) -> components.put(
                Objects.requireNonNull(role, "component color role"),
                Objects.requireNonNull(color, "component color value")));
        componentColors = Map.copyOf(components);
    }

    public boolean isEmpty() {
        return colorScheme.isEmpty() && textTheme.isEmpty() && componentColors.isEmpty();
    }
}

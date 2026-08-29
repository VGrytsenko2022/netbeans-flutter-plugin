package dev.flutter.netbeans.project.theme;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable per-theme role overrides; omitted entries inherit seed/Material defaults. */
public record FlutterThemeOverrides(
        Map<FlutterMaterialColorRole, Integer> colorScheme,
        Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> textTheme) {

    public static final FlutterThemeOverrides EMPTY = new FlutterThemeOverrides(Map.of(), Map.of());

    public FlutterThemeOverrides {
        Objects.requireNonNull(colorScheme, "colorScheme");
        Objects.requireNonNull(textTheme, "textTheme");
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
    }

    public boolean isEmpty() {
        return colorScheme.isEmpty() && textTheme.isEmpty();
    }
}

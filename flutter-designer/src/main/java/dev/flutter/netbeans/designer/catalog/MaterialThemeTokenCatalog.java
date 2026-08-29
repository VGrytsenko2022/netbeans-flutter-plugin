package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.ThemeToken;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Reviewed Material theme roles available to persisted Designer properties. */
public final class MaterialThemeTokenCatalog {
    private static final String COLOR_PREFIX = "material.colorScheme.";
    private static final String TEXT_PREFIX = "material.textTheme.";

    private static final Map<String, String> COLORS = roles(COLOR_PREFIX,
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
            "error", "onError", "errorContainer", "onErrorContainer",
            "surface", "onSurface", "surfaceDim", "surfaceBright",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
            "surfaceContainerHigh", "surfaceContainerHighest", "onSurfaceVariant",
            "outline", "outlineVariant", "shadow", "scrim", "inverseSurface",
            "onInverseSurface", "inversePrimary", "surfaceTint");

    private static final Map<String, String> TEXT_STYLES = roles(TEXT_PREFIX,
            "displayLarge", "displayMedium", "displaySmall",
            "headlineLarge", "headlineMedium", "headlineSmall",
            "titleLarge", "titleMedium", "titleSmall",
            "bodyLarge", "bodyMedium", "bodySmall",
            "labelLarge", "labelMedium", "labelSmall");

    private MaterialThemeTokenCatalog() {
    }

    public static Map<String, String> colorRoles() {
        return COLORS;
    }

    public static Map<String, String> textStyleRoles() {
        return TEXT_STYLES;
    }

    public static Optional<String> colorRole(ThemeToken token) {
        Objects.requireNonNull(token, "token");
        return Optional.ofNullable(COLORS.get(token.wireId()));
    }

    public static Optional<String> textStyleRole(ThemeToken token) {
        Objects.requireNonNull(token, "token");
        return Optional.ofNullable(TEXT_STYLES.get(token.wireId()));
    }

    private static Map<String, String> roles(String prefix, String... roles) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (String role : roles) {
            if (values.put(prefix + role, role) != null) {
                throw new IllegalStateException("Duplicate Material theme role: " + role);
            }
        }
        return Map.copyOf(values);
    }
}

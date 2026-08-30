package dev.flutter.netbeans.project.theme;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable, validated project-wide Flutter theme model for schemas v1-v5. */
public record FlutterProjectTheme(
        boolean enabled,
        FlutterThemeMode defaultMode,
        String lightThemeId,
        String darkThemeId,
        List<FlutterProjectThemeDefinition> themes,
        FlutterGeneratedThemeArtifact generated) {

    public static final int MAX_THEMES = 64;
    public static final String DEFAULT_LIGHT_THEME_ID = "light";
    public static final String DEFAULT_DARK_THEME_ID = "dark";
    public static final int DEFAULT_SEED_ARGB = 0xFF6750A4;

    public FlutterProjectTheme {
        defaultMode = Objects.requireNonNull(defaultMode, "defaultMode");
        lightThemeId = Objects.requireNonNull(lightThemeId, "lightThemeId");
        darkThemeId = Objects.requireNonNull(darkThemeId, "darkThemeId");
        themes = List.copyOf(Objects.requireNonNull(themes, "themes"));
        generated = Objects.requireNonNull(generated, "generated");
        if (themes.isEmpty() || themes.size() > MAX_THEMES) {
            throw new IllegalArgumentException(
                    "Project theme catalog must contain 1 to " + MAX_THEMES + " themes");
        }
        Set<String> ids = new HashSet<>();
        for (FlutterProjectThemeDefinition theme : themes) {
            Objects.requireNonNull(theme, "themes must not contain null");
            if (!ids.add(theme.id())) {
                throw new IllegalArgumentException("Duplicate project theme id: " + theme.id());
            }
        }
        requireReferencedBrightness(
                themes, lightThemeId, FlutterThemeBrightness.LIGHT, "lightThemeId", enabled);
        requireReferencedBrightness(
                themes, darkThemeId, FlutterThemeBrightness.DARK, "darkThemeId", enabled);
    }

    /**
     * Source-compatible schema-v1 constructor. Project themes were always
     * enabled before the explicit schema-v2 switch was introduced.
     */
    public FlutterProjectTheme(
            FlutterThemeMode defaultMode,
            String lightThemeId,
            String darkThemeId,
            List<FlutterProjectThemeDefinition> themes,
            FlutterGeneratedThemeArtifact generated) {
        this(true, defaultMode, lightThemeId, darkThemeId, themes, generated);
    }

    public static FlutterProjectTheme defaultTheme(String generatedSha256) {
        return new FlutterProjectTheme(
                true,
                FlutterThemeMode.SYSTEM,
                DEFAULT_LIGHT_THEME_ID,
                DEFAULT_DARK_THEME_ID,
                List.of(
                        new FlutterProjectThemeDefinition(
                                DEFAULT_LIGHT_THEME_ID,
                                "Light",
                                FlutterThemeBrightness.LIGHT,
                                DEFAULT_SEED_ARGB,
                                true),
                        new FlutterProjectThemeDefinition(
                                DEFAULT_DARK_THEME_ID,
                                "Dark",
                                FlutterThemeBrightness.DARK,
                                DEFAULT_SEED_ARGB,
                                true)),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        generatedSha256));
    }

    public Optional<FlutterProjectThemeDefinition> themeById(String id) {
        Objects.requireNonNull(id, "id");
        return themes.stream().filter(theme -> theme.id().equals(id)).findFirst();
    }

    public FlutterProjectThemeDefinition lightTheme() {
        return themeById(lightThemeId).orElseThrow();
    }

    public FlutterProjectThemeDefinition darkTheme() {
        return themeById(darkThemeId).orElseThrow();
    }

    private static void requireReferencedBrightness(
            List<FlutterProjectThemeDefinition> themes,
            String id,
            FlutterThemeBrightness expected,
            String field,
            boolean requireEnabled) {
        FlutterProjectThemeDefinition referenced = themes.stream()
                .filter(theme -> theme.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        field + " references missing project theme id: " + id));
        if (referenced.brightness() != expected) {
            throw new IllegalArgumentException(
                    field + " must reference a " + expected.wireName()
                    + " theme, but " + id + " is "
                    + referenced.brightness().wireName());
        }
        if (requireEnabled && !referenced.enabled()) {
            throw new IllegalArgumentException(
                    field + " must reference an enabled theme while project themes are enabled: "
                    + id);
        }
    }
}

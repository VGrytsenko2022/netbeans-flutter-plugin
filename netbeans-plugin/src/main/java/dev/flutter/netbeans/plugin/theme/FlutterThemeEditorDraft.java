package dev.flutter.netbeans.plugin.theme;

import dev.flutter.netbeans.project.theme.FlutterGeneratedThemeArtifact;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDefinition;
import dev.flutter.netbeans.project.theme.FlutterMaterialColorRole;
import dev.flutter.netbeans.project.theme.FlutterMaterialTextStyleRole;
import dev.flutter.netbeans.project.theme.FlutterThemeColorValue;
import dev.flutter.netbeans.project.theme.FlutterThemeComponentColorRole;
import dev.flutter.netbeans.project.theme.FlutterThemeOverrides;
import dev.flutter.netbeans.project.theme.FlutterThemeTextStyleOverride;
import dev.flutter.netbeans.project.theme.FlutterThemeBrightness;
import dev.flutter.netbeans.project.theme.FlutterThemeMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Mutable dialog draft kept separate from the immutable on-disk contract. */
final class FlutterThemeEditorDraft {
    private static final String CUSTOM_ID_BASE = "custom_theme";

    private boolean enabled;
    private FlutterThemeMode defaultMode;
    private ThemeRow lightTheme;
    private ThemeRow darkTheme;
    private final List<ThemeRow> themes;

    FlutterThemeEditorDraft(FlutterProjectTheme source) {
        Objects.requireNonNull(source, "source");
        enabled = source.enabled();
        defaultMode = source.defaultMode();
        themes = new ArrayList<>(source.themes().size());
        for (FlutterProjectThemeDefinition definition : source.themes()) {
            themes.add(new ThemeRow(
                    definition.id(),
                    definition.displayName(),
                    definition.brightness(),
                    definition.seedArgb(),
                    definition.enabled(),
                    isBuiltInId(definition.id()),
                    definition.overrides()));
        }
        lightTheme = findRequired(source.lightThemeId());
        darkTheme = findRequired(source.darkThemeId());
    }

    List<ThemeRow> themes() {
        return List.copyOf(themes);
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean value) {
        enabled = value;
    }

    FlutterThemeMode defaultMode() {
        return defaultMode;
    }

    void setDefaultMode(FlutterThemeMode value) {
        defaultMode = Objects.requireNonNull(value, "value");
    }

    ThemeRow lightTheme() {
        return lightTheme;
    }

    void setLightTheme(ThemeRow value) {
        requireOwned(value);
        if (value.brightness() != FlutterThemeBrightness.LIGHT) {
            throw new IllegalArgumentException(
                    "Application light theme must reference a light theme.");
        }
        lightTheme = value;
    }

    ThemeRow darkTheme() {
        return darkTheme;
    }

    void setDarkTheme(ThemeRow value) {
        requireOwned(value);
        if (value.brightness() != FlutterThemeBrightness.DARK) {
            throw new IllegalArgumentException(
                    "Application dark theme must reference a dark theme.");
        }
        darkTheme = value;
    }

    ThemeRow addCustom() {
        requireCapacity();
        String id = uniqueId(CUSTOM_ID_BASE);
        ThemeRow created = new ThemeRow(
                id,
                "Custom Theme",
                FlutterThemeBrightness.LIGHT,
                FlutterProjectTheme.DEFAULT_SEED_ARGB,
                true,
                false,
                FlutterThemeOverrides.EMPTY);
        themes.add(created);
        return created;
    }

    ThemeRow duplicate(ThemeRow source) {
        requireOwned(source);
        requireCapacity();
        String base = source.id() + "_copy";
        if (base.codePointCount(0, base.length())
                > FlutterProjectThemeDefinition.MAX_ID_CODE_POINTS) {
            base = CUSTOM_ID_BASE;
        }
        ThemeRow duplicate = new ThemeRow(
                uniqueId(base),
                duplicateDisplayName(source.displayName()),
                source.brightness(),
                source.seedArgb(),
                true,
                false,
                source.overrides());
        themes.add(duplicate);
        return duplicate;
    }

    void removeCustom(ThemeRow target) {
        requireOwned(target);
        if (target.builtIn()) {
            throw new IllegalArgumentException(
                    "Built-in Light and Dark themes cannot be removed.");
        }
        if (lightTheme == target || darkTheme == target) {
            throw new IllegalArgumentException(
                    "Cannot remove a theme referenced by the application. "
                    + "Choose another application light or dark theme first.");
        }
        themes.remove(target);
    }

    void update(
            ThemeRow target,
            String id,
            String displayName,
            FlutterThemeBrightness brightness,
            int seedArgb) {
        requireOwned(target);
        update(target, id, displayName, brightness, seedArgb, target.enabled());
    }

    void update(
            ThemeRow target,
            String id,
            String displayName,
            FlutterThemeBrightness brightness,
            int seedArgb,
            boolean themeEnabled) {
        requireOwned(target);
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(brightness, "brightness");
        if (target.builtIn() && !target.id().equals(id)) {
            throw new IllegalArgumentException(
                    "Built-in Light and Dark theme IDs are stable and cannot be changed.");
        }
        if (target.builtIn() && target.brightness() != brightness) {
            throw new IllegalArgumentException(
                    "Built-in Light and Dark theme brightness is stable and cannot be changed.");
        }
        // Reuse the core validation contract before changing the draft.
        new FlutterProjectThemeDefinition(
                id, displayName, brightness, seedArgb, themeEnabled, target.overrides());
        for (ThemeRow candidate : themes) {
            if (candidate != target && candidate.id().equals(id)) {
                throw new IllegalArgumentException("Duplicate project theme id: " + id);
            }
        }

        if (lightTheme == target && brightness != FlutterThemeBrightness.LIGHT) {
            throw new IllegalArgumentException(
                    "The application light theme must stay light. Choose another "
                    + "application light theme before changing this brightness.");
        }
        if (darkTheme == target && brightness != FlutterThemeBrightness.DARK) {
            throw new IllegalArgumentException(
                    "The application dark theme must stay dark. Choose another "
                    + "application dark theme before changing this brightness.");
        }
        if (enabled && !themeEnabled && lightTheme == target) {
            throw new IllegalArgumentException(
                    "Choose another application light theme before disabling this theme.");
        }
        if (enabled && !themeEnabled && darkTheme == target) {
            throw new IllegalArgumentException(
                    "Choose another application dark theme before disabling this theme.");
        }
        target.update(id, displayName, brightness, seedArgb, themeEnabled);
    }

    void setColorOverride(
            ThemeRow target, FlutterMaterialColorRole role, Integer argb) {
        requireOwned(target);
        target.setColorOverride(Objects.requireNonNull(role, "role"), argb);
    }

    void setTextStyleOverride(
            ThemeRow target,
            FlutterMaterialTextStyleRole role,
            FlutterThemeTextStyleOverride style) {
        requireOwned(target);
        target.setTextStyleOverride(
                Objects.requireNonNull(role, "role"),
                Objects.requireNonNull(style, "style"));
    }

    void setComponentColorOverride(
            ThemeRow target,
            FlutterThemeComponentColorRole role,
            FlutterThemeColorValue color) {
        requireOwned(target);
        target.setComponentColorOverride(
                Objects.requireNonNull(role, "role"), color);
    }

    FlutterProjectTheme build(FlutterGeneratedThemeArtifact generated) {
        List<FlutterProjectThemeDefinition> definitions = themes.stream()
                .map(ThemeRow::toDefinition)
                .toList();
        return new FlutterProjectTheme(
                enabled,
                defaultMode,
                lightTheme.id(),
                darkTheme.id(),
                definitions,
                generated);
    }

    private ThemeRow findRequired(String id) {
        return themes.stream()
                .filter(theme -> theme.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private String uniqueId(String requestedBase) {
        Set<String> occupied = new HashSet<>();
        for (ThemeRow theme : themes) {
            occupied.add(theme.id());
        }
        if (!occupied.contains(requestedBase)) {
            return requestedBase;
        }
        for (int suffix = 2; suffix < 10_000; suffix++) {
            String candidate = requestedBase + "_" + suffix;
            if (candidate.codePointCount(0, candidate.length())
                    <= FlutterProjectThemeDefinition.MAX_ID_CODE_POINTS
                    && !occupied.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot allocate a unique custom theme id.");
    }

    private static String duplicateDisplayName(String original) {
        String suffix = " Copy";
        int maximum = FlutterProjectThemeDefinition.MAX_DISPLAY_NAME_CODE_POINTS;
        String value = original + suffix;
        if (value.codePointCount(0, value.length()) <= maximum) {
            return value;
        }
        int keep = maximum - suffix.codePointCount(0, suffix.length());
        int end = original.offsetByCodePoints(0, keep);
        return original.substring(0, end).stripTrailing() + suffix;
    }

    private void requireCapacity() {
        if (themes.size() >= FlutterProjectTheme.MAX_THEMES) {
            throw new IllegalArgumentException(
                    "A project can contain at most "
                    + FlutterProjectTheme.MAX_THEMES + " themes.");
        }
    }

    private void requireOwned(ThemeRow theme) {
        if (theme == null || !themes.contains(theme)) {
            throw new IllegalArgumentException(
                    "The selected theme does not belong to this project theme draft.");
        }
    }

    private static boolean isBuiltInId(String id) {
        return FlutterProjectTheme.DEFAULT_LIGHT_THEME_ID.equals(id)
                || FlutterProjectTheme.DEFAULT_DARK_THEME_ID.equals(id);
    }

    static final class ThemeRow {
        private String id;
        private String displayName;
        private FlutterThemeBrightness brightness;
        private int seedArgb;
        private boolean enabled;
        private final boolean builtIn;
        private FlutterThemeOverrides overrides;

        private ThemeRow(
                String id,
                String displayName,
                FlutterThemeBrightness brightness,
                int seedArgb,
                boolean enabled,
                boolean builtIn,
                FlutterThemeOverrides overrides) {
            this.id = id;
            this.displayName = displayName;
            this.brightness = brightness;
            this.seedArgb = seedArgb;
            this.enabled = enabled;
            this.builtIn = builtIn;
            this.overrides = Objects.requireNonNull(overrides, "overrides");
        }

        String id() {
            return id;
        }

        String displayName() {
            return displayName;
        }

        FlutterThemeBrightness brightness() {
            return brightness;
        }

        int seedArgb() {
            return seedArgb;
        }

        boolean enabled() {
            return enabled;
        }

        boolean builtIn() {
            return builtIn;
        }

        FlutterThemeOverrides overrides() {
            return overrides;
        }

        private void update(
                String nextId,
                String nextDisplayName,
                FlutterThemeBrightness nextBrightness,
                int nextSeedArgb,
                boolean nextEnabled) {
            id = nextId;
            displayName = nextDisplayName;
            brightness = nextBrightness;
            seedArgb = nextSeedArgb;
            enabled = nextEnabled;
        }

        private void setColorOverride(FlutterMaterialColorRole role, Integer argb) {
            java.util.EnumMap<FlutterMaterialColorRole, Integer> colors =
                    new java.util.EnumMap<>(FlutterMaterialColorRole.class);
            colors.putAll(overrides.colorScheme());
            if (argb == null) {
                colors.remove(role);
            } else {
                colors.put(role, argb);
            }
            overrides = new FlutterThemeOverrides(
                    colors, overrides.textTheme(), overrides.componentColors());
        }

        private void setTextStyleOverride(
                FlutterMaterialTextStyleRole role, FlutterThemeTextStyleOverride style) {
            java.util.EnumMap<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> styles =
                    new java.util.EnumMap<>(FlutterMaterialTextStyleRole.class);
            styles.putAll(overrides.textTheme());
            if (style.isEmpty()) {
                styles.remove(role);
            } else {
                styles.put(role, style);
            }
            overrides = new FlutterThemeOverrides(
                    overrides.colorScheme(), styles, overrides.componentColors());
        }

        private void setComponentColorOverride(
                FlutterThemeComponentColorRole role,
                FlutterThemeColorValue color) {
            java.util.EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> values =
                    new java.util.EnumMap<>(FlutterThemeComponentColorRole.class);
            values.putAll(overrides.componentColors());
            if (color == null) {
                values.remove(role);
            } else {
                values.put(role, color);
            }
            overrides = new FlutterThemeOverrides(
                    overrides.colorScheme(), overrides.textTheme(), values);
        }

        private FlutterProjectThemeDefinition toDefinition() {
            return new FlutterProjectThemeDefinition(
                    id, displayName, brightness, seedArgb, enabled, overrides);
        }

        @Override
        public String toString() {
            return displayName + " (" + id + ")";
        }
    }
}

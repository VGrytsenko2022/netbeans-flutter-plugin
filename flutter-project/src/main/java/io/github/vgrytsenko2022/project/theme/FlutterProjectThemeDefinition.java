package io.github.vgrytsenko2022.project.theme;

import java.util.Objects;
import java.util.regex.Pattern;

/** One named ThemeData definition in the project-wide theme catalog. */
public record FlutterProjectThemeDefinition(
        String id,
        String displayName,
        FlutterThemeBrightness brightness,
        int seedArgb,
        boolean enabled,
        FlutterThemeOverrides overrides) {

    public static final int MAX_ID_CODE_POINTS = 64;
    public static final int MAX_DISPLAY_NAME_CODE_POINTS = 80;
    private static final Pattern CANONICAL_ID = Pattern.compile("[a-z][a-z0-9_]*");

    public FlutterProjectThemeDefinition {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        brightness = Objects.requireNonNull(brightness, "brightness");
        overrides = Objects.requireNonNull(overrides, "overrides");
        if (!CANONICAL_ID.matcher(id).matches()
                || id.codePointCount(0, id.length()) > MAX_ID_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Theme id must be canonical lower_snake_case with at most "
                    + MAX_ID_CODE_POINTS + " code points: " + id);
        }
        if (!displayName.equals(displayName.strip())) {
            throw new IllegalArgumentException(
                    "Theme displayName must not contain leading or trailing whitespace");
        }
        int displayLength = displayName.codePointCount(0, displayName.length());
        if (displayLength == 0 || displayLength > MAX_DISPLAY_NAME_CODE_POINTS
                || displayName.codePoints().anyMatch(Character::isISOControl)
                || !containsOnlyUnicodeScalarValues(displayName)) {
            throw new IllegalArgumentException(
                    "Theme displayName must contain 1 to "
                    + MAX_DISPLAY_NAME_CODE_POINTS + " printable code points");
        }
        if ((seedArgb >>> 24) != 0xFF) {
            throw new IllegalArgumentException(
                    "Theme seedArgb must be opaque and use alpha FF");
        }
    }

    /**
     * Source-compatible constructor for schema-v1/v2 definitions, where every
     * catalog entry is enabled.
     */
    public FlutterProjectThemeDefinition(
            String id,
            String displayName,
            FlutterThemeBrightness brightness,
            int seedArgb) {
        this(id, displayName, brightness, seedArgb, true, FlutterThemeOverrides.EMPTY);
    }

    /** Source-compatible schema-v3 constructor with no role overrides. */
    public FlutterProjectThemeDefinition(
            String id,
            String displayName,
            FlutterThemeBrightness brightness,
            int seedArgb,
            boolean enabled) {
        this(id, displayName, brightness, seedArgb, enabled, FlutterThemeOverrides.EMPTY);
    }

    /** Exact canonical ARGB literal used by the JSON and Dart generators. */
    public String seedArgbLiteral() {
        return "0x%08X".formatted(seedArgb);
    }

    private static boolean containsOnlyUnicodeScalarValues(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return false;
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return false;
            }
        }
        return true;
    }
}

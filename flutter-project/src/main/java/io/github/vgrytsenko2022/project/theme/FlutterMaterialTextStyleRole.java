package io.github.vgrytsenko2022.project.theme;

import java.util.Arrays;

/** Reviewed Material 3 {@code TextTheme} roles supported by theme schema v4. */
public enum FlutterMaterialTextStyleRole {
    DISPLAY_LARGE("displayLarge"),
    DISPLAY_MEDIUM("displayMedium"),
    DISPLAY_SMALL("displaySmall"),
    HEADLINE_LARGE("headlineLarge"),
    HEADLINE_MEDIUM("headlineMedium"),
    HEADLINE_SMALL("headlineSmall"),
    TITLE_LARGE("titleLarge"),
    TITLE_MEDIUM("titleMedium"),
    TITLE_SMALL("titleSmall"),
    BODY_LARGE("bodyLarge"),
    BODY_MEDIUM("bodyMedium"),
    BODY_SMALL("bodySmall"),
    LABEL_LARGE("labelLarge"),
    LABEL_MEDIUM("labelMedium"),
    LABEL_SMALL("labelSmall");

    private final String wireName;

    FlutterMaterialTextStyleRole(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String themeTokenWireId() {
        return "material.textTheme." + wireName;
    }

    public static FlutterMaterialTextStyleRole fromWireName(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Material TextTheme role: " + value));
    }
}

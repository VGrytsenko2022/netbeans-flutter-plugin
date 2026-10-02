package io.github.vgrytsenko2022.project.theme;

import java.util.Arrays;

/** Project-wide selection policy for the generated light and dark themes. */
public enum FlutterThemeMode {
    SYSTEM("system", "ThemeMode.system"),
    LIGHT("light", "ThemeMode.light"),
    DARK("dark", "ThemeMode.dark");

    private final String wireName;
    private final String dartExpression;

    FlutterThemeMode(String wireName, String dartExpression) {
        this.wireName = wireName;
        this.dartExpression = dartExpression;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return dartExpression;
    }

    public static FlutterThemeMode fromWireName(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter theme mode: " + value));
    }
}

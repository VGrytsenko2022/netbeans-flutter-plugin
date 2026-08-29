package dev.flutter.netbeans.project.theme;

import java.util.Arrays;

/** Brightness contract of one project theme definition. */
public enum FlutterThemeBrightness {
    LIGHT("light", "Brightness.light"),
    DARK("dark", "Brightness.dark");

    private final String wireName;
    private final String dartExpression;

    FlutterThemeBrightness(String wireName, String dartExpression) {
        this.wireName = wireName;
        this.dartExpression = dartExpression;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return dartExpression;
    }

    public static FlutterThemeBrightness fromWireName(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter theme brightness: " + value));
    }
}

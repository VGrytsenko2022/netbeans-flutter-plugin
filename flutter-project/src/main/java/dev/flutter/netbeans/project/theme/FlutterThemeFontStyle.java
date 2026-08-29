package dev.flutter.netbeans.project.theme;

import java.util.Arrays;

/** Stable wire form of Flutter's {@code FontStyle}. */
public enum FlutterThemeFontStyle {
    NORMAL("normal"), ITALIC("italic");

    private final String wireName;

    FlutterThemeFontStyle(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return "FontStyle." + wireName;
    }

    public static FlutterThemeFontStyle fromWireName(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.wireName.equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter font style: " + value));
    }
}

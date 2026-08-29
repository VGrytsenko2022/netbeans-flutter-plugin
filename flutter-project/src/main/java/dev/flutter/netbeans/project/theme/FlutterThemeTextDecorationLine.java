package dev.flutter.netbeans.project.theme;

import java.util.Arrays;

/** One composable line in Flutter's {@code TextDecoration}. */
public enum FlutterThemeTextDecorationLine {
    UNDERLINE("underline"),
    OVERLINE("overline"),
    LINE_THROUGH("lineThrough");

    private final String wireName;

    FlutterThemeTextDecorationLine(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return "TextDecoration." + wireName;
    }

    public static FlutterThemeTextDecorationLine fromWireName(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.wireName.equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter text decoration line: " + value));
    }
}

package io.github.vgrytsenko2022.project.theme;

import java.util.Arrays;

/** Stable wire form of Flutter's {@code TextDecorationStyle}. */
public enum FlutterThemeTextDecorationStyle {
    SOLID("solid"), DOUBLE("double"), DOTTED("dotted"), DASHED("dashed"), WAVY("wavy");

    private final String wireName;

    FlutterThemeTextDecorationStyle(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return "TextDecorationStyle." + wireName;
    }

    public static FlutterThemeTextDecorationStyle fromWireName(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.wireName.equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter text decoration style: " + value));
    }
}

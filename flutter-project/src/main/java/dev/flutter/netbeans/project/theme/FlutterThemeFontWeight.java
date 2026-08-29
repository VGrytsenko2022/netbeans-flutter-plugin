package dev.flutter.netbeans.project.theme;

import java.util.Arrays;

/** Stable wire form of Flutter's nine canonical {@code FontWeight} values. */
public enum FlutterThemeFontWeight {
    W100("w100"), W200("w200"), W300("w300"), W400("w400"), W500("w500"),
    W600("w600"), W700("w700"), W800("w800"), W900("w900");

    private final String wireName;

    FlutterThemeFontWeight(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public String dartExpression() {
        return "FontWeight." + wireName;
    }

    public static FlutterThemeFontWeight fromWireName(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.wireName.equals(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Flutter font weight: " + value));
    }
}

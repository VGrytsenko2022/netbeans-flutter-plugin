package io.github.vgrytsenko2022.project.theme;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Typed, optional overrides applied to one seed-derived Material text role.
 * Empty values deliberately inherit Flutter's resolved Material default.
 */
public record FlutterThemeTextStyleOverride(
        Optional<FlutterThemeColorValue> color,
        Optional<FlutterThemeColorValue> backgroundColor,
        Optional<Double> fontSize,
        Optional<FlutterThemeFontWeight> fontWeight,
        Optional<FlutterThemeFontStyle> fontStyle,
        Optional<Double> letterSpacing,
        Optional<Double> wordSpacing,
        Optional<Double> height,
        Optional<String> fontFamily,
        Optional<Set<FlutterThemeTextDecorationLine>> decoration,
        Optional<FlutterThemeColorValue> decorationColor,
        Optional<FlutterThemeTextDecorationStyle> decorationStyle,
        Optional<Double> decorationThickness) {

    public static final FlutterThemeTextStyleOverride EMPTY = new FlutterThemeTextStyleOverride(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    public static final int MAX_FONT_FAMILY_CODE_POINTS = 128;

    public FlutterThemeTextStyleOverride {
        color = requiredOptional(color, "color");
        backgroundColor = requiredOptional(backgroundColor, "backgroundColor");
        fontSize = finite(fontSize, "fontSize", true, false);
        fontWeight = requiredOptional(fontWeight, "fontWeight");
        fontStyle = requiredOptional(fontStyle, "fontStyle");
        letterSpacing = finite(letterSpacing, "letterSpacing", false, false);
        wordSpacing = finite(wordSpacing, "wordSpacing", false, false);
        height = finite(height, "height", false, true);
        fontFamily = requiredOptional(fontFamily, "fontFamily").map(value -> {
            if (!value.equals(value.strip()) || value.isEmpty()
                    || value.codePointCount(0, value.length()) > MAX_FONT_FAMILY_CODE_POINTS
                    || value.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(
                        "fontFamily must contain 1 to " + MAX_FONT_FAMILY_CODE_POINTS
                        + " printable code points without surrounding whitespace");
            }
            return value;
        });
        decoration = requiredOptional(decoration, "decoration").map(lines -> {
            Objects.requireNonNull(lines, "decoration lines");
            lines.forEach(line -> Objects.requireNonNull(line, "decoration line"));
            return Set.copyOf(lines);
        });
        decorationColor = requiredOptional(decorationColor, "decorationColor");
        decorationStyle = requiredOptional(decorationStyle, "decorationStyle");
        decorationThickness = finite(
                decorationThickness, "decorationThickness", true, false);
    }

    public boolean isEmpty() {
        return color.isEmpty() && backgroundColor.isEmpty() && fontSize.isEmpty()
                && fontWeight.isEmpty() && fontStyle.isEmpty() && letterSpacing.isEmpty()
                && wordSpacing.isEmpty() && height.isEmpty() && fontFamily.isEmpty()
                && decoration.isEmpty() && decorationColor.isEmpty() && decorationStyle.isEmpty()
                && decorationThickness.isEmpty();
    }

    private static <T> Optional<T> requiredOptional(Optional<T> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(item -> Objects.requireNonNull(item, name + " value"));
        return value;
    }

    private static Optional<Double> finite(
            Optional<Double> value, String name, boolean allowZero, boolean positiveOnly) {
        requiredOptional(value, name);
        value.ifPresent(number -> {
            if (!Double.isFinite(number)
                    || (positiveOnly && number <= 0.0d)
                    || (!positiveOnly && allowZero && number < 0.0d)) {
                throw new IllegalArgumentException(name + " is outside its supported range");
            }
        });
        return value;
    }

    public static String argbLiteral(int argb) {
        return "0x%08X".formatted(argb);
    }
}

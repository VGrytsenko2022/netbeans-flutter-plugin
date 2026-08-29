package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Typed theme-role override transferred to the isolated Canvas runner. */
public record CanvasThemeTextStyleOverride(
        Optional<CanvasThemeColorValue> color,
        Optional<CanvasThemeColorValue> backgroundColor,
        Optional<Double> fontSize,
        Optional<String> fontWeight,
        Optional<String> fontStyle,
        Optional<Double> letterSpacing,
        Optional<Double> wordSpacing,
        Optional<Double> height,
        Optional<String> fontFamily,
        Optional<Set<String>> decoration,
        Optional<CanvasThemeColorValue> decorationColor,
        Optional<String> decorationStyle,
        Optional<Double> decorationThickness) {

    private static final Set<String> FONT_WEIGHTS = Set.of(
            "w100", "w200", "w300", "w400", "w500", "w600", "w700", "w800", "w900");
    private static final Set<String> FONT_STYLES = Set.of("normal", "italic");
    private static final Set<String> DECORATIONS = Set.of(
            "underline", "overline", "lineThrough");
    private static final Set<String> DECORATION_STYLES = Set.of(
            "solid", "double", "dotted", "dashed", "wavy");

    public CanvasThemeTextStyleOverride {
        color = required(color, "color");
        backgroundColor = required(backgroundColor, "backgroundColor");
        fontSize = number(fontSize, "fontSize", true, false);
        fontWeight = choice(fontWeight, "fontWeight", FONT_WEIGHTS);
        fontStyle = choice(fontStyle, "fontStyle", FONT_STYLES);
        letterSpacing = number(letterSpacing, "letterSpacing", false, false);
        wordSpacing = number(wordSpacing, "wordSpacing", false, false);
        height = number(height, "height", false, true);
        fontFamily = required(fontFamily, "fontFamily").map(value -> {
            if (!value.equals(value.strip()) || value.isEmpty()
                    || value.codePointCount(0, value.length()) > 128
                    || value.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(
                        "Canvas fontFamily must contain 1 to 128 printable code points "
                        + "without surrounding whitespace");
            }
            return value;
        });
        decoration = required(decoration, "decoration").map(values -> {
            Set<String> copy = Set.copyOf(values);
            if (!DECORATIONS.containsAll(copy)) {
                throw new IllegalArgumentException("Canvas decoration contains an unknown value");
            }
            return copy;
        });
        decorationColor = required(decorationColor, "decorationColor");
        decorationStyle = choice(decorationStyle, "decorationStyle", DECORATION_STYLES);
        decorationThickness = number(
                decorationThickness, "decorationThickness", true, false);
    }

    private static <T> Optional<T> required(Optional<T> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(item -> Objects.requireNonNull(item, name + " value"));
        return value;
    }

    private static Optional<String> choice(
            Optional<String> value, String name, Set<String> allowed) {
        required(value, name);
        value.ifPresent(item -> {
            if (!allowed.contains(item)) {
                throw new IllegalArgumentException("Unsupported Canvas " + name + ": " + item);
            }
        });
        return value;
    }

    private static Optional<Double> number(
            Optional<Double> value, String name, boolean nonNegative, boolean positive) {
        required(value, name);
        value.ifPresent(item -> {
            if (!Double.isFinite(item) || (positive && item <= 0.0d)
                    || (nonNegative && item < 0.0d)) {
                throw new IllegalArgumentException("Invalid Canvas " + name);
            }
        });
        return value;
    }
}

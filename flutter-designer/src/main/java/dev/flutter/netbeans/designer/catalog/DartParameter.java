package dev.flutter.netbeans.designer.catalog;

import java.util.Objects;

/**
 * Constructor parameter metadata shared by properties and child slots.
 * {@code required} describes argument presence, not whether a nullable child
 * slot must contain a widget.
 */
public record DartParameter(ParameterStyle style, int order, boolean required) {
    public DartParameter {
        Objects.requireNonNull(style, "style");
        if (order < 0) {
            throw new IllegalArgumentException("Parameter order must not be negative: " + order);
        }
    }

    public static DartParameter positional(int order) {
        return new DartParameter(ParameterStyle.POSITIONAL, order, true);
    }

    public static DartParameter positional(int order, boolean required) {
        return new DartParameter(ParameterStyle.POSITIONAL, order, required);
    }

    public static DartParameter named(int order, boolean required) {
        return new DartParameter(ParameterStyle.NAMED, order, required);
    }
}

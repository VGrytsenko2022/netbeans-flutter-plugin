package dev.flutter.netbeans.designer.model;

import java.util.regex.Pattern;

/** Constructor property name stored on a widget node. */
public record PropertyName(String value) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public PropertyName {
        value = ModelConstraints.matching(value, "property name", NAME);
    }

    @Override
    public String toString() {
        return value;
    }
}

package dev.flutter.netbeans.designer.model;

import java.util.regex.Pattern;

/** Namespaced owner key for top-level or widget-specific extension data. */
public record ExtensionKey(String value) {
    private static final Pattern KEY = Pattern.compile(
            "[a-z][a-z0-9.-]*:[A-Za-z0-9._-]+");

    public ExtensionKey {
        value = ModelConstraints.matching(value, "extension key", KEY);
    }

    @Override
    public String toString() {
        return value;
    }
}

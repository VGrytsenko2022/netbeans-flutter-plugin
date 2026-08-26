package dev.flutter.netbeans.designer.catalog;

import java.util.Objects;
import java.util.regex.Pattern;

/** Shared validation for Dart import URIs stored in catalog metadata. */
final class DartImportUris {
    private static final Pattern IMPORT_URI = Pattern.compile(
            "^(?:dart:[a-z][a-z0-9_]*(?:/[A-Za-z0-9_./-]+)?"
            + "|package:[a-z][a-z0-9_]*/[A-Za-z0-9_./-]+\\.dart)$");

    private DartImportUris() {
    }

    static String requireValid(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.length() > 512
                || !IMPORT_URI.matcher(value).matches()
                || value.contains("//")
                || value.contains("/./")
                || value.contains("/../")) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return value;
    }
}

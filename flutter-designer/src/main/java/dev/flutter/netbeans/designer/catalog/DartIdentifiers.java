package dev.flutter.netbeans.designer.catalog;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** One identifier policy for every Dart fragment emitted from catalog metadata. */
final class DartIdentifiers {
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Set<String> RESERVED = Set.of(
            "abstract", "as", "assert", "async", "await", "base", "break", "case", "catch",
            "class", "const", "continue", "covariant", "default", "deferred", "do", "dynamic",
            "else", "enum", "export", "extends", "extension", "external", "factory", "false",
            "final", "finally", "for", "Function", "get", "hide", "if", "implements", "import",
            "in", "interface", "is", "late", "library", "mixin", "native", "new", "null", "of",
            "on", "operator", "part", "required", "rethrow", "return", "sealed", "set", "show",
            "static", "super", "switch", "sync", "this", "throw", "true", "try", "type",
            "typedef", "var", "void", "when", "while", "with", "yield");

    private DartIdentifiers() {
    }

    static String requireIdentifier(String value, String label) {
        Objects.requireNonNull(value, label);
        if (!isIdentifier(value)) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return value;
    }

    static String requirePublicIdentifier(String value, String label) {
        requireIdentifier(value, label);
        if (value.startsWith("_")) {
            throw new IllegalArgumentException("Catalog-emitted " + label + " must be public: " + value);
        }
        return value;
    }

    static boolean isIdentifier(String value) {
        return value != null && IDENTIFIER.matcher(value).matches() && !RESERVED.contains(value);
    }
}

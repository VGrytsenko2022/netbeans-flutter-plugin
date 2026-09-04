package dev.flutter.netbeans.designer.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

final class ModelConstraints {
    static final Pattern DART_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    static final Pattern QUALIFIED_DART_IDENTIFIER = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");
    static final Pattern CALLBACK_HANDLER = Pattern.compile("_?[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern PROJECT_PACKAGE_LIBRARY_URI = Pattern.compile(
            "package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart");
    private static final Set<String> DART_RESERVED = Set.of(
            "abstract", "as", "assert", "async", "await", "base", "break", "case",
            "catch", "class", "const", "continue", "covariant", "default", "deferred",
            "do", "dynamic", "else", "enum", "export", "extends", "extension", "external",
            "factory", "false", "final", "finally", "for", "Function", "get", "hide", "if",
            "implements", "import", "in", "interface", "is", "late", "library", "mixin",
            "native", "new", "null", "of", "on", "operator", "part", "required", "rethrow",
            "return", "sealed", "set", "show", "static", "super", "switch", "sync", "this",
            "throw", "true", "try", "type", "typedef", "var", "void", "when", "while",
            "with", "yield");

    private ModelConstraints() {
    }

    static String matching(String value, String label, Pattern pattern) {
        Objects.requireNonNull(value, label);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " does not match the Flutter Designer document contract: " + value);
        }
        return value;
    }

    static String dartIdentifier(String value, String label, boolean publicOnly) {
        matching(value, label, DART_IDENTIFIER);
        if (DART_RESERVED.contains(value) || (publicOnly && value.startsWith("_"))) {
            throw new IllegalArgumentException(
                    label + " is not an accessible Dart identifier: " + value);
        }
        return value;
    }

    static String projectPackageLibraryUri(String value, String label) {
        codePointLength(value, label, 1, 512);
        if (!PROJECT_PACKAGE_LIBRARY_URI.matcher(value).matches()
                || value.contains("//")
                || value.contains("/./")
                || value.contains("/../")) {
            throw new IllegalArgumentException(
                    label + " is not a canonical package Dart library URI: " + value);
        }
        return value;
    }

    static String codePointLength(String value, String label, int minimum, int maximum) {
        Objects.requireNonNull(value, label);
        int length = value.codePointCount(0, value.length());
        if (length < minimum || length > maximum) {
            throw new IllegalArgumentException(
                    label + " must contain between " + minimum + " and " + maximum + " Unicode code points");
        }
        return value;
    }

    static BigDecimal normalizedNumber(BigDecimal value, String label) {
        Objects.requireNonNull(value, label);
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.stripTrailingZeros();
    }

    static <K, V> Map<K, V> immutableLinkedMap(Map<K, V> values, String label) {
        Objects.requireNonNull(values, label);
        LinkedHashMap<K, V> copy = new LinkedHashMap<>(values.size());
        values.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, label + " key"),
                Objects.requireNonNull(value, label + " value")));
        return Collections.unmodifiableMap(copy);
    }
}

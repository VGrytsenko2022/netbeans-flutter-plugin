package dev.flutter.netbeans.plugin.dart.wizard;

import java.util.Locale;
import java.util.regex.Pattern;

/** Naming and source generation rules shared by the Dart Class wizard. */
public final class DartClassNaming {
    private static final Pattern CLASS_NAME = Pattern.compile("_?[A-Z][A-Za-z0-9]*");

    private DartClassNaming() {
    }

    public static boolean isValidClassName(String value) {
        return value != null && CLASS_NAME.matcher(value.trim()).matches();
    }

    public static String fileName(String className) {
        String value = requireClassName(className);
        boolean privateName = value.startsWith("_");
        String core = privateName ? value.substring(1) : value;
        String snakeCase = core
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
        return (privateName ? "_" : "") + snakeCase + ".dart";
    }

    public static String source(String className) {
        String value = requireClassName(className);
        return "class " + value + " {\n"
                + "  const " + value + "();\n"
                + "}\n";
    }

    private static String requireClassName(String value) {
        if (!isValidClassName(value)) {
            throw new IllegalArgumentException(
                    "Dart class name must use UpperCamelCase, optionally prefixed with '_'.");
        }
        return value.trim();
    }
}

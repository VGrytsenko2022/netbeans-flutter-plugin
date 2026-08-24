package dev.flutter.netbeans.plugin.settings;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public record FlutterToolchainConfig(
        String flutterHome,
        boolean useBundledDart,
        String dartHome) {

    public FlutterToolchainConfig {
        flutterHome = clean(flutterHome);
        dartHome = clean(dartHome);
    }

    public static FlutterToolchainConfig defaults() {
        return new FlutterToolchainConfig("", true, "");
    }

    private static String clean(String value) {
        if (value == null) return "";
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        if (cleaned.isBlank()) return "";
        try {
            return Path.of(cleaned).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException ex) {
            return cleaned;
        }
    }
}

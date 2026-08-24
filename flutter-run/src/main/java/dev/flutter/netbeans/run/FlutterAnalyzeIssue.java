package dev.flutter.netbeans.run;

import java.util.Objects;
import java.util.Optional;

/** One source issue reported by {@code flutter analyze}. */
public record FlutterAnalyzeIssue(
        FlutterAnalyzeSeverity severity,
        String message,
        String file,
        int line,
        int column,
        Optional<String> code) {

    public FlutterAnalyzeIssue {
        Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        file = requireText(file, "file");
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Flutter analyze locations are one-based");
        }
        code = Objects.requireNonNull(code, "code")
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Flutter analyze " + name + " is required");
        }
        return value.strip();
    }
}

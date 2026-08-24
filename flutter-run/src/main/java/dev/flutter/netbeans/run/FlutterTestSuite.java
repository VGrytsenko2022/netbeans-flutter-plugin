package dev.flutter.netbeans.run;

import java.util.Objects;
import java.util.Optional;

/** Metadata for a suite announced by the Dart test JSON reporter. */
public record FlutterTestSuite(
        long id,
        String platform,
        Optional<String> path) {

    public FlutterTestSuite {
        if (id < 0) {
            throw new IllegalArgumentException("Flutter test suite id cannot be negative");
        }
        platform = platform == null || platform.isBlank() ? "unknown" : platform.strip();
        path = Objects.requireNonNull(path, "path")
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }
}

package dev.flutter.netbeans.run;

import java.util.Locale;
import java.util.Optional;

/** Severity reported by {@code flutter analyze}. */
public enum FlutterAnalyzeSeverity {
    ERROR,
    WARNING,
    INFO;

    static Optional<FlutterAnalyzeSeverity> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}

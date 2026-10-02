package io.github.vgrytsenko2022.plugin.pubspec;

/** A pubspec-specific semantic diagnostic expressed in document offsets. */
public record PubspecDiagnostic(
        String code,
        Severity severity,
        int startOffset,
        int endOffset,
        String message) {

    public enum Severity {
        ERROR,
        WARNING
    }

    public PubspecDiagnostic {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Diagnostic code must not be blank.");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Diagnostic severity must not be null.");
        }
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid diagnostic offsets.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Diagnostic message must not be blank.");
        }
    }
}

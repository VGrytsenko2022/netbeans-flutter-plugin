package dev.flutter.netbeans.dart;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** One bounded analyzer diagnostic for the exact candidate overlay. */
public record DartCandidateDiagnostic(
        DartCandidateDiagnosticSeverity severity,
        String type,
        Optional<String> code,
        String message,
        Optional<String> correction,
        Optional<String> url,
        Path file,
        int offset,
        int length,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn,
        boolean blocking) {

    public DartCandidateDiagnostic {
        Objects.requireNonNull(severity, "severity");
        type = requireText(type, "type");
        code = normalizeOptional(code, "code");
        message = requireText(message, "message");
        correction = normalizeOptional(correction, "correction");
        url = normalizeOptional(url, "url");
        Objects.requireNonNull(file, "file");
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException("file must be absolute");
        }
        file = file.normalize();
        if (offset < 0 || length < 0
                || startLine <= 0 || startColumn <= 0
                || endLine <= 0 || endColumn <= 0) {
            throw new IllegalArgumentException("diagnostic location is outside the supported range");
        }
        if (blocking != (severity == DartCandidateDiagnosticSeverity.ERROR
                || severity == DartCandidateDiagnosticSeverity.WARNING && blocking)) {
            // INFO can never be blocking; ERROR can never be downgraded.
            if (severity == DartCandidateDiagnosticSeverity.ERROR || blocking) {
                throw new IllegalArgumentException("invalid diagnostic blocking policy");
            }
        }
    }

    private static Optional<String> normalizeOptional(Optional<String> value, String name) {
        Objects.requireNonNull(value, name);
        return value.map(text -> requireText(text, name));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}

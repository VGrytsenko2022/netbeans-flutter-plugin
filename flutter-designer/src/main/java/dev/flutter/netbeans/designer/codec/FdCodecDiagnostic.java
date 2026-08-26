package dev.flutter.netbeans.designer.codec;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** One bounded codec diagnostic with a JSON Pointer and optional source location. */
public record FdCodecDiagnostic(
        FdCodecDiagnosticCode code,
        String pointer,
        String message,
        OptionalLong byteOffset,
        OptionalInt line,
        OptionalInt column) {

    public FdCodecDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(byteOffset, "byteOffset");
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(column, "column");
        if (!pointer.isEmpty() && !pointer.startsWith("/")) {
            throw new IllegalArgumentException("pointer must be empty or start with '/'");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        byteOffset.ifPresent(value -> {
            if (value < 0) {
                throw new IllegalArgumentException("byteOffset must not be negative");
            }
        });
        line.ifPresent(value -> {
            if (value <= 0) {
                throw new IllegalArgumentException("line must be greater than zero");
            }
        });
        column.ifPresent(value -> {
            if (value <= 0) {
                throw new IllegalArgumentException("column must be greater than zero");
            }
        });
    }

    public static FdCodecDiagnostic withoutLocation(
            FdCodecDiagnosticCode code,
            String pointer,
            String message) {
        return new FdCodecDiagnostic(
                code,
                pointer,
                message,
                OptionalLong.empty(),
                OptionalInt.empty(),
                OptionalInt.empty());
    }
}

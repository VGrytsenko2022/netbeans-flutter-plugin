package dev.flutter.netbeans.designer.move;

import java.util.Objects;

/** Concrete fail-closed reason for rejecting a Designer pair move. */
public record DartMoveDependencyDiagnostic(
        Code code,
        String sourceProjectRelativePath,
        int utf16Offset,
        String uri,
        String message) {

    public DartMoveDependencyDiagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(sourceProjectRelativePath, "sourceProjectRelativePath");
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(message, "message");
        if (utf16Offset < -1) {
            throw new IllegalArgumentException("utf16Offset must be -1 or greater");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public enum Code {
        INVALID_PACKAGE_NAME,
        INVALID_ORIGINAL_PATH,
        INVALID_TARGET_PATH,
        INVALID_SOURCE_PATH,
        SAME_PATH,
        TARGET_FILENAME_CHANGED,
        TARGET_BINDING_CHANGE,
        TOO_MANY_SOURCES,
        SOURCE_TOO_LARGE,
        PROJECT_TOO_LARGE,
        DUPLICATE_SOURCE_PATH,
        MOVED_SOURCE_MISSING,
        INVALID_UTF8,
        MALFORMED_DART_LEXEME,
        MALFORMED_DIRECTIVE,
        UNSUPPORTED_URI_LITERAL,
        TOO_MANY_DIRECTIVES,
        UNSUPPORTED_URI,
        OUTGOING_RELATIVE_DIRECTIVE,
        INCOMING_REFERENCE
    }
}

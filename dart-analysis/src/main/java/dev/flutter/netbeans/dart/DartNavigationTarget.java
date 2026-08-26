package dev.flutter.netbeans.dart;

import java.nio.file.Path;

/** One analyzer navigation destination for a requested symbol occurrence. */
public record DartNavigationTarget(
        String kind,
        Path file,
        int offset,
        int length,
        int startLine,
        int startColumn) {

    public DartNavigationTarget {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        kind = kind.strip();
        if (file == null || !file.isAbsolute()) {
            throw new IllegalArgumentException("file must be absolute");
        }
        file = file.normalize();
        if (offset < 0 || length <= 0 || startLine <= 0 || startColumn <= 0) {
            throw new IllegalArgumentException("navigation target location is invalid");
        }
    }
}

package io.github.vgrytsenko2022.designer.move;

import java.util.Objects;

/** One decoded URI literal found in a Dart directive. */
public record DartDirectiveReference(
        String sourceProjectRelativePath,
        DartDirectiveKind kind,
        String uri,
        int utf16Offset,
        boolean conditional) {

    public DartDirectiveReference {
        Objects.requireNonNull(sourceProjectRelativePath, "sourceProjectRelativePath");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(uri, "uri");
        if (sourceProjectRelativePath.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceProjectRelativePath must not be blank");
        }
        if (uri.isEmpty()) {
            throw new IllegalArgumentException("uri must not be empty");
        }
        if (utf16Offset < 0) {
            throw new IllegalArgumentException("utf16Offset must not be negative");
        }
    }
}

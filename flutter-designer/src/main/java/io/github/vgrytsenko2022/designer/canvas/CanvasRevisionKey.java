package io.github.vgrytsenko2022.designer.canvas;

import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/**
 * Exact identity of one host publication to a Canvas runtime.
 *
 * <p>{@code presentationSequence} is monotonically increasing within the open
 * Canvas session. It is intentionally independent of {@code logicalRevisionId},
 * which may move backward and forward during Undo/Redo or be revisited after a
 * durable re-anchor.</p>
 */
public record CanvasRevisionKey(
        CanvasSessionId sessionId,
        long presentationSequence,
        StableId documentId,
        long logicalRevisionId) {

    public CanvasRevisionKey {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(documentId, "documentId");
        if (presentationSequence < 0) {
            throw new IllegalArgumentException(
                    "presentationSequence must not be negative");
        }
        if (logicalRevisionId < 0) {
            throw new IllegalArgumentException(
                    "logicalRevisionId must not be negative");
        }
    }
}

package io.github.vgrytsenko2022.designer.canvas;

import java.util.Objects;

/** Session-scoped identity of one interaction intent emitted by the runtime. */
public record CanvasIntentId(CanvasSessionId sessionId, long intentSequence) {
    public CanvasIntentId {
        Objects.requireNonNull(sessionId, "sessionId");
        if (intentSequence < 0) {
            throw new IllegalArgumentException("intentSequence must not be negative");
        }
    }
}

package io.github.vgrytsenko2022.designer.canvas;

import java.util.Objects;

/** Identity of one runtime interaction bound to the exact layout it observed. */
public record CanvasIntentKey(
        CanvasIntentId intentId,
        CanvasLayoutKey layoutKey) {
    public CanvasIntentKey {
        Objects.requireNonNull(intentId, "intentId");
        Objects.requireNonNull(layoutKey, "layoutKey");
        if (!intentId.sessionId().equals(layoutKey.sessionId())) {
            throw new IllegalArgumentException(
                    "Canvas intent and layout must belong to the same session");
        }
    }
}

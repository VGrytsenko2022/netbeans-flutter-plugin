package io.github.vgrytsenko2022.designer.canvas.runtime;

import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import java.util.Objects;

/** Atomic frame and initial-layout evidence for one completed render request. */
public record CanvasPresentationReady(
        CanvasFrameKey frameKey,
        CanvasLayoutKey layoutKey) {

    public CanvasPresentationReady {
        Objects.requireNonNull(frameKey, "frameKey");
        Objects.requireNonNull(layoutKey, "layoutKey");
        if (!frameKey.equals(layoutKey.frameKey())) {
            throw new IllegalArgumentException(
                    "Canvas ready layout must belong to the exact ready frame");
        }
    }
}

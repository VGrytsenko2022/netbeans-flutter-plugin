package io.github.vgrytsenko2022.designer.canvas;

import java.util.Objects;

/** Exact renderer frame within one host-issued Canvas revision publication. */
public record CanvasFrameKey(CanvasRevisionKey revisionKey, long frameSequence) {
    public CanvasFrameKey {
        Objects.requireNonNull(revisionKey, "revisionKey");
        if (frameSequence < 0) {
            throw new IllegalArgumentException("frameSequence must not be negative");
        }
    }
}

package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;

/**
 * Exact runtime layout projection for one renderer frame.
 *
 * <p>The sequence is local to the exact frame. Geometry, hit-test results and
 * layout-dependent interaction intents must echo this complete key rather than
 * only a logical Designer revision.</p>
 */
public record CanvasLayoutKey(CanvasFrameKey frameKey, long layoutSequence) {
    public CanvasLayoutKey {
        Objects.requireNonNull(frameKey, "frameKey");
        if (layoutSequence < 0) {
            throw new IllegalArgumentException("layoutSequence must not be negative");
        }
    }

    public CanvasSessionId sessionId() {
        return frameKey.revisionKey().sessionId();
    }
}

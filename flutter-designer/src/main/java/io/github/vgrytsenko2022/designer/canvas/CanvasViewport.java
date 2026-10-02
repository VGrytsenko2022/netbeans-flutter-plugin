package io.github.vgrytsenko2022.designer.canvas;

/** Resolved logical-pixel viewport for one Canvas presentation. */
public record CanvasViewport(double logicalWidth, double logicalHeight) {
    private static final double MAX_LOGICAL_SIZE = 10_000.0d;

    public CanvasViewport {
        requireLogicalSize(logicalWidth, "logicalWidth");
        requireLogicalSize(logicalHeight, "logicalHeight");
    }

    private static void requireLogicalSize(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0d || value > MAX_LOGICAL_SIZE) {
            throw new IllegalArgumentException(
                    label + " must be finite, greater than zero and at most "
                    + (long) MAX_LOGICAL_SIZE);
        }
    }
}

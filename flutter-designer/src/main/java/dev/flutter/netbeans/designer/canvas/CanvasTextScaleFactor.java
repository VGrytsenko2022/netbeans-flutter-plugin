package dev.flutter.netbeans.designer.canvas;

/** Resolved text-scale factor for one Canvas presentation. */
public record CanvasTextScaleFactor(double value) {
    private static final double MAX_VALUE = 5.0d;

    public CanvasTextScaleFactor {
        if (!Double.isFinite(value) || value <= 0.0d || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    "textScaleFactor must be finite, greater than zero and at most "
                    + (long) MAX_VALUE);
        }
    }
}

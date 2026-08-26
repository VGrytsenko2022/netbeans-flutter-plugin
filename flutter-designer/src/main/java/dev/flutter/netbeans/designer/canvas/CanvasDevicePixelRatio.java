package dev.flutter.netbeans.designer.canvas;

/** Resolved logical-to-physical pixel ratio for one Canvas presentation. */
public record CanvasDevicePixelRatio(double value) {
    private static final double MAX_VALUE = 10.0d;

    public CanvasDevicePixelRatio {
        if (!Double.isFinite(value) || value <= 0.0d || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    "devicePixelRatio must be finite, greater than zero and at most "
                    + (long) MAX_VALUE);
        }
    }
}

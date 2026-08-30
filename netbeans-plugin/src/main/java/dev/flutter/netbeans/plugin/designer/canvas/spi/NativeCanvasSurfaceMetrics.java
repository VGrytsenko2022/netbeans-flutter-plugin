package dev.flutter.netbeans.plugin.designer.canvas.spi;

/** Current physical native-host bounds and device-pixel ratio. */
public record NativeCanvasSurfaceMetrics(
        int width,
        int height,
        int devicePixelRatioMicros) {
    public static final int MICROS_PER_UNIT = 1_000_000;

    public NativeCanvasSurfaceMetrics {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Native Canvas surface dimensions cannot be negative");
        }
        if (devicePixelRatioMicros <= 0) {
            throw new IllegalArgumentException(
                    "Native Canvas device-pixel ratio must be positive");
        }
    }
}

package io.github.vgrytsenko2022.designer.canvas;

/**
 * Runner-confirmed physical Flutter surface metrics for one presented layout.
 *
 * <p>Device-pixel ratio uses integer micros so the process boundary never
 * depends on a floating-point text representation.</p>
 */
public record CanvasSurfaceMetrics(
        int physicalWidth,
        int physicalHeight,
        int devicePixelRatioMicros) {

    public static final int MICROS_PER_UNIT = 1_000_000;
    public static final int MIN_DEVICE_PIXEL_RATIO_MICROS = 1;
    public static final int MAX_DEVICE_PIXEL_RATIO_MICROS = 10 * MICROS_PER_UNIT;

    public CanvasSurfaceMetrics {
        requirePhysicalDimension(physicalWidth, "physicalWidth");
        requirePhysicalDimension(physicalHeight, "physicalHeight");
        long pixels = (long) physicalWidth * physicalHeight;
        if (pixels > CanvasRenderProfile.MAX_PHYSICAL_PIXELS) {
            throw new IllegalArgumentException(
                    "Canvas surface requires " + pixels
                    + " pixels; the safe limit is "
                    + CanvasRenderProfile.MAX_PHYSICAL_PIXELS);
        }
        if (devicePixelRatioMicros < MIN_DEVICE_PIXEL_RATIO_MICROS
                || devicePixelRatioMicros
                        > MAX_DEVICE_PIXEL_RATIO_MICROS) {
            throw new IllegalArgumentException(
                    "devicePixelRatioMicros must be between "
                    + MIN_DEVICE_PIXEL_RATIO_MICROS + " and "
                    + MAX_DEVICE_PIXEL_RATIO_MICROS);
        }
    }

    private static void requirePhysicalDimension(int value, String label) {
        if (value < 1 || value > CanvasRenderProfile.MAX_PHYSICAL_DIMENSION) {
            throw new IllegalArgumentException(
                    label + " must be between 1 and "
                    + CanvasRenderProfile.MAX_PHYSICAL_DIMENSION);
        }
    }
}

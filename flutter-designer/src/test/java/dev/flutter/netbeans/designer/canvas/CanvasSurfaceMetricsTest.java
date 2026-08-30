package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CanvasSurfaceMetricsTest {

    @Test
    void ownsBoundedPhysicalDimensionsAndDeterministicPixelRatioMicros() {
        CanvasSurfaceMetrics metrics = new CanvasSurfaceMetrics(
                1_600, 900, 1_500_000);

        assertEquals(1_600, metrics.physicalWidth());
        assertEquals(900, metrics.physicalHeight());
        assertEquals(1_500_000, metrics.devicePixelRatioMicros());
    }

    @Test
    void rejectsUnsafeOrNonPositiveSurfaceValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(0, 900, 1_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(1_600, 0, 1_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(
                        CanvasRenderProfile.MAX_PHYSICAL_DIMENSION + 1,
                        1,
                        1_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(4_096, 4_096, 1_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasSurfaceMetrics(
                        1,
                        1,
                        CanvasSurfaceMetrics.MAX_DEVICE_PIXEL_RATIO_MICROS + 1));
    }
}

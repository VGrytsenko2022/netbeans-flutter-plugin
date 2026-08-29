package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.StableId;
import org.junit.jupiter.api.Test;

class CanvasViewportPresentationTest {

    @Test
    void fitAndManualFactoriesUseBoundedDeterministicMicros() {
        assertEquals(new CanvasViewportPresentation(
                CanvasZoomMode.FIT,
                CanvasViewportPresentation.MICROS_PER_UNIT,
                0,
                0), CanvasViewportPresentation.fit());
        assertEquals(new CanvasViewportPresentation(
                CanvasZoomMode.MANUAL,
                1_250_000,
                400_000,
                600_000), CanvasViewportPresentation.manual(
                        1_250_000, 400_000, 600_000));
    }

    @Test
    void rejectsZoomAndScrollOutsideTheWireDomain() {
        assertThrows(NullPointerException.class,
                () -> new CanvasViewportPresentation(null, 1_000_000, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasViewportPresentation.manual(249_999, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasViewportPresentation.manual(2_000_001, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasViewportPresentation.manual(1_000_000, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasViewportPresentation.manual(
                        1_000_000, 0, 1_000_001));
    }

    @Test
    void metricsOwnAnExactRevisionAndBoundedEffectiveScale() {
        CanvasRevisionKey revision = new CanvasRevisionKey(
                CanvasSessionId.parse("80ef60ed-b108-4674-99a6-c1f3102f01ab"),
                7,
                StableId.parse("a4b202a7-060a-4d07-ba52-54340b6a80ea"),
                11);
        CanvasViewportMetrics metrics = new CanvasViewportMetrics(
                revision,
                17,
                CanvasViewportPresentation.manual(1_250_000, 10, 20),
                1_250_000,
                true,
                false);

        assertEquals(revision, metrics.revisionKey());
        assertEquals(17, metrics.commandSequence());
        assertTrue(metrics.horizontalScrollable());
        assertFalse(metrics.verticalScrollable());
        assertEquals(125_000, new CanvasViewportMetrics(
                revision,
                0,
                CanvasViewportPresentation.fit(),
                125_000,
                false,
                false).effectiveScaleMicros());
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasViewportMetrics(
                        revision,
                        0,
                        CanvasViewportPresentation.fit(),
                        0,
                        false,
                        false));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasViewportMetrics(
                        revision,
                        -1,
                        CanvasViewportPresentation.fit(),
                        1_000_000,
                        false,
                        false));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasViewportMetrics(
                        revision,
                        dev.flutter.netbeans.designer.canvas.protocol
                                .CanvasWireProtocol.MAX_SEQUENCE + 1,
                        CanvasViewportPresentation.fit(),
                        1_000_000,
                        false,
                        false));
    }
}

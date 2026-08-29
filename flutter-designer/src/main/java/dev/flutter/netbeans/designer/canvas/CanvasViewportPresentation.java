package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;

/**
 * Host-requested visual presentation of one fixed logical Canvas viewport.
 *
 * <p>Zoom and scroll values use integer micros so the host and isolated
 * Flutter runner never exchange ambiguous binary floating-point state.</p>
 */
public record CanvasViewportPresentation(
        CanvasZoomMode mode,
        int zoomMicros,
        int horizontalScrollMicros,
        int verticalScrollMicros) {

    public static final int MICROS_PER_UNIT = 1_000_000;
    public static final int MIN_ZOOM_MICROS = 250_000;
    public static final int MAX_ZOOM_MICROS = 2_000_000;
    public static final int MIN_SCROLL_MICROS = 0;
    public static final int MAX_SCROLL_MICROS = MICROS_PER_UNIT;

    public CanvasViewportPresentation {
        Objects.requireNonNull(mode, "mode");
        if (zoomMicros < MIN_ZOOM_MICROS
                || zoomMicros > MAX_ZOOM_MICROS) {
            throw new IllegalArgumentException(
                    "zoomMicros must be between " + MIN_ZOOM_MICROS
                    + " and " + MAX_ZOOM_MICROS);
        }
        requireScroll(horizontalScrollMicros, "horizontalScrollMicros");
        requireScroll(verticalScrollMicros, "verticalScrollMicros");
    }

    /** Requests a runner-computed scale that keeps the whole viewport visible. */
    public static CanvasViewportPresentation fit() {
        return new CanvasViewportPresentation(
                CanvasZoomMode.FIT,
                MICROS_PER_UNIT,
                MIN_SCROLL_MICROS,
                MIN_SCROLL_MICROS);
    }

    /** Requests an explicit zoom and normalized scroll positions. */
    public static CanvasViewportPresentation manual(
            int zoomMicros,
            int horizontalScrollMicros,
            int verticalScrollMicros) {
        return new CanvasViewportPresentation(
                CanvasZoomMode.MANUAL,
                zoomMicros,
                horizontalScrollMicros,
                verticalScrollMicros);
    }

    private static void requireScroll(int value, String label) {
        if (value < MIN_SCROLL_MICROS || value > MAX_SCROLL_MICROS) {
            throw new IllegalArgumentException(
                    label + " must be between " + MIN_SCROLL_MICROS
                    + " and " + MAX_SCROLL_MICROS);
        }
    }
}

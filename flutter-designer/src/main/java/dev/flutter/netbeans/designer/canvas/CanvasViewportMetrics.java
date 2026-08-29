package dev.flutter.netbeans.designer.canvas;

import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireProtocol;
import java.util.Objects;

/** Runner-confirmed presentation metrics for one exact Canvas revision. */
public record CanvasViewportMetrics(
        CanvasRevisionKey revisionKey,
        long commandSequence,
        CanvasViewportPresentation presentation,
        int effectiveScaleMicros,
        boolean horizontalScrollable,
        boolean verticalScrollable) {

    /** Fit may become smaller than the minimum user-selectable manual zoom. */
    public static final int MIN_EFFECTIVE_SCALE_MICROS = 1;

    public CanvasViewportMetrics {
        Objects.requireNonNull(revisionKey, "revisionKey");
        Objects.requireNonNull(presentation, "presentation");
        if (commandSequence < 0
                || commandSequence > CanvasWireProtocol.MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "commandSequence must be between 0 and "
                    + CanvasWireProtocol.MAX_SEQUENCE);
        }
        if (effectiveScaleMicros < MIN_EFFECTIVE_SCALE_MICROS
                || effectiveScaleMicros
                        > CanvasViewportPresentation.MAX_ZOOM_MICROS) {
            throw new IllegalArgumentException(
                    "effectiveScaleMicros must be between "
                    + MIN_EFFECTIVE_SCALE_MICROS + " and "
                    + CanvasViewportPresentation.MAX_ZOOM_MICROS);
        }
    }
}

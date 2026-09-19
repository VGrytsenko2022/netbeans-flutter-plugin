package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSurfaceMetrics;
import io.github.vgrytsenko2022.designer.canvas.CanvasViewportMetrics;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireLimits;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireProtocol;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.Objects;

/** Strictly decoded event from the isolated Flutter Canvas runner. */
public sealed interface CanvasRunnerRuntimeEvent permits
        CanvasRunnerRuntimeEvent.Presented,
        CanvasRunnerRuntimeEvent.Selection,
        CanvasRunnerRuntimeEvent.Interaction,
        CanvasRunnerRuntimeEvent.InteractionFenceApplied,
        CanvasRunnerRuntimeEvent.PaletteDrop,
        CanvasRunnerRuntimeEvent.DeleteSelection,
        CanvasRunnerRuntimeEvent.TextEditCommit,
        CanvasRunnerRuntimeEvent.ViewportMetrics {

    record Presented(
            CanvasLayoutKey layoutKey,
            CanvasSurfaceMetrics metrics) implements CanvasRunnerRuntimeEvent {
        public Presented {
            Objects.requireNonNull(layoutKey, "layoutKey");
            Objects.requireNonNull(metrics, "metrics");
        }
    }

    record Selection(
            CanvasIntentKey intentKey,
            StableId widgetId) implements CanvasRunnerRuntimeEvent {
        public Selection {
            Objects.requireNonNull(intentKey, "intentKey");
            Objects.requireNonNull(widgetId, "widgetId");
        }
    }

    /** One authenticated pointer-down on the exact presented Canvas layout. */
    record Interaction(
            CanvasIntentKey intentKey,
            long interactionFenceSequence) implements CanvasRunnerRuntimeEvent {
        public Interaction {
            Objects.requireNonNull(intentKey, "intentKey");
            if (interactionFenceSequence < 0
                    || interactionFenceSequence > CanvasWireProtocol.MAX_SEQUENCE) {
                throw new IllegalArgumentException(
                        "interactionFenceSequence is outside the wire range");
            }
        }
    }

    /** Runner acknowledgement that one exact-layout input fence is active. */
    record InteractionFenceApplied(
            CanvasLayoutKey layoutKey,
            long interactionFenceSequence) implements CanvasRunnerRuntimeEvent {
        public InteractionFenceApplied {
            Objects.requireNonNull(layoutKey, "layoutKey");
            if (interactionFenceSequence < 0
                    || interactionFenceSequence > CanvasWireProtocol.MAX_SEQUENCE) {
                throw new IllegalArgumentException(
                        "interactionFenceSequence is outside the wire range");
            }
        }
    }

    /** One exact, one-shot request to insert a Palette widget into a slot. */
    record PaletteDrop(
            CanvasIntentKey intentKey,
            String token,
            StableId parentWidgetId,
            SlotName slotName,
            int insertionIndex) implements CanvasRunnerRuntimeEvent {
        public PaletteDrop {
            Objects.requireNonNull(intentKey, "intentKey");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(parentWidgetId, "parentWidgetId");
            Objects.requireNonNull(slotName, "slotName");
            if (token.isBlank()) {
                throw new IllegalArgumentException("token must not be blank");
            }
            if (insertionIndex < 0) {
                throw new IllegalArgumentException(
                        "insertionIndex must not be negative");
            }
        }
    }

    /** One exact, one-shot request to delete the currently selected widget. */
    record DeleteSelection(
            CanvasIntentKey intentKey,
            StableId widgetId) implements CanvasRunnerRuntimeEvent {
        public DeleteSelection {
            Objects.requireNonNull(intentKey, "intentKey");
            Objects.requireNonNull(widgetId, "widgetId");
        }
    }

    /** One final, composition-free inline edit of {@code Text.data}. */
    record TextEditCommit(
            CanvasIntentKey intentKey,
            long interactionFenceSequence,
            StableId widgetId,
            String text,
            boolean compositionObserved) implements CanvasRunnerRuntimeEvent {
        public TextEditCommit {
            Objects.requireNonNull(intentKey, "intentKey");
            if (interactionFenceSequence < 0
                    || interactionFenceSequence > CanvasWireProtocol.MAX_SEQUENCE) {
                throw new IllegalArgumentException(
                        "interactionFenceSequence is outside the wire range");
            }
            Objects.requireNonNull(widgetId, "widgetId");
            validateInlineText(text);
        }
    }

    /** Runner-confirmed presentation metrics for one exact Canvas revision. */
    record ViewportMetrics(
            CanvasViewportMetrics metrics) implements CanvasRunnerRuntimeEvent {
        public ViewportMetrics {
            Objects.requireNonNull(metrics, "metrics");
        }
    }

    private static void validateInlineText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > CanvasWireLimits.DEFAULT_MAX_STRING_UTF16_UNITS) {
            throw new IllegalArgumentException(
                    "text exceeds the Canvas wire UTF-16 limit");
        }
        int scalarCount = 0;
        for (int index = 0; index < text.length(); index++) {
            char unit = text.charAt(index);
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 >= text.length()
                        || !Character.isLowSurrogate(text.charAt(index + 1))) {
                    throw new IllegalArgumentException(
                            "text contains malformed UTF-16");
                }
                index++;
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException(
                        "text contains malformed UTF-16");
            }
            scalarCount++;
            if (scalarCount > CanvasWireLimits.DEFAULT_MAX_STRING_CODE_POINTS) {
                throw new IllegalArgumentException(
                        "text exceeds the Canvas wire Unicode scalar limit");
            }
        }
    }
}

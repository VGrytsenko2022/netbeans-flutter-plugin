package io.github.vgrytsenko2022.designer.canvas.runtime;

import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import java.util.Objects;

/** Immutable state of one read-only, per-MultiView Canvas controller. */
public sealed interface CanvasControllerState permits
        CanvasControllerState.New,
        CanvasControllerState.Starting,
        CanvasControllerState.Idle,
        CanvasControllerState.Rendering,
        CanvasControllerState.Presented,
        CanvasControllerState.Failed,
        CanvasControllerState.Closed {

    record New() implements CanvasControllerState {
    }

    record Starting(
            CanvasBackendOpenRequest request,
            long attemptSequence,
            boolean restarting) implements CanvasControllerState {
        public Starting {
            Objects.requireNonNull(request, "request");
            if (attemptSequence < 0) {
                throw new IllegalArgumentException(
                        "attemptSequence must not be negative");
            }
        }
    }

    record Idle(CanvasBackendReady backend) implements CanvasControllerState {
        public Idle {
            Objects.requireNonNull(backend, "backend");
        }
    }

    record Rendering(
            CanvasBackendReady backend,
            CanvasRevisionKey revisionKey) implements CanvasControllerState {
        public Rendering {
            Objects.requireNonNull(backend, "backend");
            Objects.requireNonNull(revisionKey, "revisionKey");
            requireSameSession(backend, revisionKey);
        }
    }

    record Presented(
            CanvasBackendReady backend,
            CanvasFrameKey frameKey,
            CanvasLayoutKey layoutKey) implements CanvasControllerState {
        public Presented {
            Objects.requireNonNull(backend, "backend");
            Objects.requireNonNull(frameKey, "frameKey");
            Objects.requireNonNull(layoutKey, "layoutKey");
            requireSameSession(backend, frameKey.revisionKey());
            if (!frameKey.equals(layoutKey.frameKey())) {
                throw new IllegalArgumentException(
                        "Presented layout must belong to the exact presented frame");
            }
        }
    }

    record Failed(
            CanvasBackendOpenRequest request,
            long attemptSequence,
            CanvasFailureStage stage,
            String reason,
            boolean restartable) implements CanvasControllerState {
        public Failed {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(stage, "stage");
            if (attemptSequence < 0) {
                throw new IllegalArgumentException(
                        "attemptSequence must not be negative");
            }
            reason = CanvasFailureReason.normalize(reason);
        }
    }

    record Closed() implements CanvasControllerState {
    }

    private static void requireSameSession(
            CanvasBackendReady backend,
            CanvasRevisionKey revisionKey) {
        if (!backend.sessionId().equals(revisionKey.sessionId())) {
            throw new IllegalArgumentException(
                    "Backend and Canvas revision must belong to the same session");
        }
    }

}

package io.github.vgrytsenko2022.designer.canvas.runtime;

import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.Objects;

/** Exact successful startup handshake returned by one backend attempt. */
public record CanvasBackendReady(
        CanvasSessionId sessionId,
        int protocolVersion,
        CanvasEngineIdentity engineIdentity) {

    public CanvasBackendReady {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
    }
}

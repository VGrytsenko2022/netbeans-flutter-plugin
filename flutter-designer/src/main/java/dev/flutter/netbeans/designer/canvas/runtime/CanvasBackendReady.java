package dev.flutter.netbeans.designer.canvas.runtime;

import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
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

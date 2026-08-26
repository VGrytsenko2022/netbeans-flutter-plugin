package dev.flutter.netbeans.designer.canvas.runtime;

import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.util.Objects;

/** Host-issued identity and protocol request for one backend attempt. */
public record CanvasBackendOpenRequest(
        CanvasSessionId sessionId,
        int protocolVersion) {

    public CanvasBackendOpenRequest {
        Objects.requireNonNull(sessionId, "sessionId");
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
    }
}

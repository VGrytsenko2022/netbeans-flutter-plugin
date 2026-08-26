package dev.flutter.netbeans.designer.canvas.protocol;

import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.util.Objects;

/** Host request to close one exact Canvas session. */
public record CanvasHostClose(
        CanvasSessionId sessionId,
        long sequence,
        CanvasWireCloseReason reason) implements CanvasWireMessage {

    public CanvasHostClose {
        Objects.requireNonNull(sessionId, "sessionId");
        CanvasWireProtocol.requireSequence(sequence, "sequence");
        Objects.requireNonNull(reason, "reason");
    }

    @Override
    public CanvasWireMessageType type() {
        return CanvasWireMessageType.HOST_CLOSE;
    }
}

package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
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

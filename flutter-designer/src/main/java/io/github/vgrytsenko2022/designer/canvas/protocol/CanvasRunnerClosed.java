package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.Objects;
import java.util.OptionalLong;

/** Runner acknowledgement that one exact Canvas session has closed. */
public record CanvasRunnerClosed(
        CanvasSessionId sessionId,
        long sequence,
        long repliedTo) implements CanvasWireMessage {

    public CanvasRunnerClosed {
        Objects.requireNonNull(sessionId, "sessionId");
        CanvasWireProtocol.requireSequence(sequence, "sequence");
        CanvasWireProtocol.requireSequence(repliedTo, "repliedTo");
    }

    @Override
    public CanvasWireMessageType type() {
        return CanvasWireMessageType.RUNNER_CLOSED;
    }

    @Override
    public OptionalLong replyTo() {
        return OptionalLong.of(repliedTo);
    }
}

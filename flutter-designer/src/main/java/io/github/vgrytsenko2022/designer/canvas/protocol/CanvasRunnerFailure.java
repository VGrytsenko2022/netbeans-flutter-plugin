package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.Objects;
import java.util.OptionalLong;

/** Bounded stable runner failure; implementation details remain on stderr. */
public record CanvasRunnerFailure(
        CanvasSessionId sessionId,
        long sequence,
        OptionalLong replyTo,
        CanvasWireFailureCode code,
        boolean fatal,
        String message) implements CanvasWireMessage {

    public CanvasRunnerFailure {
        Objects.requireNonNull(sessionId, "sessionId");
        CanvasWireProtocol.requireSequence(sequence, "sequence");
        Objects.requireNonNull(replyTo, "replyTo");
        if (replyTo.isPresent()) {
            CanvasWireProtocol.requireSequence(replyTo.orElseThrow(), "replyTo");
        }
        Objects.requireNonNull(code, "code");
        message = CanvasWireValues.printableText(
                message,
                "message",
                CanvasWireLimits.DEFAULT_MAX_FAILURE_MESSAGE_CODE_POINTS);
    }

    @Override
    public CanvasWireMessageType type() {
        return CanvasWireMessageType.RUNNER_FAILURE;
    }
}

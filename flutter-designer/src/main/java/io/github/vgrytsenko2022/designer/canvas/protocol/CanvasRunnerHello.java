package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/** Runner acceptance of the exact version 1 host handshake. */
public record CanvasRunnerHello(
        CanvasSessionId sessionId,
        long sequence,
        long repliedTo,
        String runnerVersion,
        CanvasEngineIdentity engineIdentity,
        List<CanvasWireCapability> acceptedCapabilities,
        CanvasWireHandshakeLimits effectiveLimits) implements CanvasWireMessage {

    public CanvasRunnerHello(
            CanvasSessionId sessionId,
            long sequence,
            long repliedTo,
            String runnerVersion,
            CanvasEngineIdentity engineIdentity,
            Collection<CanvasWireCapability> acceptedCapabilities,
            CanvasWireHandshakeLimits effectiveLimits) {
        this(sessionId, sequence, repliedTo, runnerVersion, engineIdentity,
                List.copyOf(acceptedCapabilities), effectiveLimits);
    }

    public CanvasRunnerHello {
        Objects.requireNonNull(sessionId, "sessionId");
        CanvasWireProtocol.requireSequence(sequence, "sequence");
        CanvasWireProtocol.requireSequence(repliedTo, "repliedTo");
        runnerVersion = CanvasWireValues.printableText(
                runnerVersion, "runnerVersion", CanvasWireValues.MAX_VERSION_CODE_POINTS);
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        acceptedCapabilities = CanvasWireValues.capabilities(
                acceptedCapabilities, "acceptedCapabilities");
        Objects.requireNonNull(effectiveLimits, "effectiveLimits");
    }

    @Override
    public CanvasWireMessageType type() {
        return CanvasWireMessageType.RUNNER_HELLO;
    }

    @Override
    public OptionalLong replyTo() {
        return OptionalLong.of(repliedTo);
    }
}

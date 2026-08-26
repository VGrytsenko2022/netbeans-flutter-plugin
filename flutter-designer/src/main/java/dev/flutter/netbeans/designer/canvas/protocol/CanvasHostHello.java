package dev.flutter.netbeans.designer.canvas.protocol;

import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** First host message for one new isolated Canvas session. */
public record CanvasHostHello(
        CanvasSessionId sessionId,
        long sequence,
        String hostVersion,
        List<CanvasWireCapability> requestedCapabilities,
        CanvasWireHandshakeLimits offeredLimits) implements CanvasWireMessage {

    public CanvasHostHello(
            CanvasSessionId sessionId,
            long sequence,
            String hostVersion,
            Collection<CanvasWireCapability> requestedCapabilities,
            CanvasWireHandshakeLimits offeredLimits) {
        this(sessionId, sequence, hostVersion,
                List.copyOf(requestedCapabilities), offeredLimits);
    }

    public CanvasHostHello {
        Objects.requireNonNull(sessionId, "sessionId");
        if (CanvasWireProtocol.requireSequence(sequence, "sequence") != 0) {
            throw new IllegalArgumentException("host.hello sequence must be zero");
        }
        hostVersion = CanvasWireValues.printableText(
                hostVersion, "hostVersion", CanvasWireValues.MAX_VERSION_CODE_POINTS);
        requestedCapabilities = CanvasWireValues.capabilities(
                requestedCapabilities, "requestedCapabilities");
        Objects.requireNonNull(offeredLimits, "offeredLimits");
    }

    @Override
    public CanvasWireMessageType type() {
        return CanvasWireMessageType.HOST_HELLO;
    }
}

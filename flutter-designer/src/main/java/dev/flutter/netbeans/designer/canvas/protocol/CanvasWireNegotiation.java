package dev.flutter.netbeans.designer.canvas.protocol;

import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Immutable limits and capabilities accepted by one exact runner handshake. */
public record CanvasWireNegotiation(
        String runnerVersion,
        CanvasEngineIdentity engineIdentity,
        List<CanvasWireCapability> acceptedCapabilities,
        CanvasWireHandshakeLimits effectiveLimits) {

    public CanvasWireNegotiation(
            String runnerVersion,
            CanvasEngineIdentity engineIdentity,
            Collection<CanvasWireCapability> acceptedCapabilities,
            CanvasWireHandshakeLimits effectiveLimits) {
        this(runnerVersion, engineIdentity, List.copyOf(acceptedCapabilities),
                effectiveLimits);
    }

    public CanvasWireNegotiation {
        runnerVersion = CanvasWireValues.printableText(
                runnerVersion,
                "runnerVersion",
                CanvasWireValues.MAX_VERSION_CODE_POINTS);
        Objects.requireNonNull(engineIdentity, "engineIdentity");
        acceptedCapabilities = CanvasWireValues.capabilities(
                acceptedCapabilities, "acceptedCapabilities");
        Objects.requireNonNull(effectiveLimits, "effectiveLimits");
    }
}

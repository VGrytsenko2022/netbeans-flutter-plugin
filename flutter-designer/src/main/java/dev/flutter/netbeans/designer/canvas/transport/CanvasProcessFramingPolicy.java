package dev.flutter.netbeans.designer.canvas.transport;

import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireHandshakeLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireLimits;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireNegotiation;
import dev.flutter.netbeans.designer.canvas.protocol.CanvasWireCapability;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable direction, lifecycle, capability and allocation policy for one
 * process-stream operation.
 */
public final class CanvasProcessFramingPolicy {
    private final CanvasWireLimits controlLimits;
    private final CanvasWireHandshakeLimits payloadLimits;
    private final int maxControlBytes;
    private final int maxModelBytes;
    private final int maxCatalogBytes;
    private final int maxEncodedImageBytes;
    private final CanvasProcessDirection direction;
    private final CanvasProcessFramingState state;
    private final Set<CanvasWireCapability> capabilities;

    private CanvasProcessFramingPolicy(
            CanvasWireLimits controlLimits,
            CanvasWireHandshakeLimits payloadLimits,
            CanvasProcessDirection direction,
            CanvasProcessFramingState state,
            Set<CanvasWireCapability> capabilities) {
        this.controlLimits = Objects.requireNonNull(controlLimits, "controlLimits");
        this.payloadLimits = Objects.requireNonNull(payloadLimits, "payloadLimits");
        this.maxControlBytes = Math.min(
                controlLimits.maxMessageBytes(),
                payloadLimits.maxControlMessageBytes());
        this.maxModelBytes = payloadLimits.maxModelBytes();
        this.maxCatalogBytes = payloadLimits.maxCatalogBytes();
        this.maxEncodedImageBytes = payloadLimits.maxEncodedImageBytes();
        this.direction = Objects.requireNonNull(direction, "direction");
        this.state = Objects.requireNonNull(state, "state");
        this.capabilities = capabilities.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(capabilities));
    }

    /** Only lifecycle control frames are legal before a runner hello is admitted. */
    public static CanvasProcessFramingPolicy handshake(
            CanvasWireLimits controlLimits,
            CanvasWireHandshakeLimits offeredLimits,
            CanvasProcessDirection direction) {
        return new CanvasProcessFramingPolicy(
                controlLimits,
                offeredLimits,
                direction,
                CanvasProcessFramingState.HANDSHAKE,
                Set.of());
    }

    /** Uses the exact effective limits admitted by one successful handshake. */
    public static CanvasProcessFramingPolicy negotiated(
            CanvasWireLimits controlLimits,
            CanvasWireNegotiation negotiation,
            CanvasProcessDirection direction) {
        Objects.requireNonNull(negotiation, "negotiation");
        return new CanvasProcessFramingPolicy(
                controlLimits,
                negotiation.effectiveLimits(),
                direction,
                CanvasProcessFramingState.NEGOTIATED,
                Set.copyOf(negotiation.acceptedCapabilities()));
    }

    /** Restricts either direction to lifecycle control while closing. */
    public CanvasProcessFramingPolicy closing() {
        return new CanvasProcessFramingPolicy(
                controlLimits,
                payloadLimits,
                direction,
                CanvasProcessFramingState.CLOSING,
                capabilities);
    }

    public CanvasProcessDirection direction() {
        return direction;
    }

    public CanvasProcessFramingState state() {
        return state;
    }

    public boolean payloadsNegotiated() {
        return state == CanvasProcessFramingState.NEGOTIATED;
    }

    public boolean allows(CanvasProcessFrameKind kind) {
        return rejection(kind) == null;
    }

    public int maxPayloadBytes(CanvasProcessFrameKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case CONTROL_JSON -> maxControlBytes;
            case MODEL_JSON -> maxModelBytes;
            case CATALOG_JSON -> maxCatalogBytes;
            case IMAGE_BYTES -> maxEncodedImageBytes;
        };
    }

    CanvasProcessFramingError rejection(CanvasProcessFrameKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (kind == CanvasProcessFrameKind.CONTROL_JSON) {
            return null;
        }
        if (direction != CanvasProcessDirection.HOST_TO_RUNNER) {
            return CanvasProcessFramingError.WRONG_DIRECTION;
        }
        if (state != CanvasProcessFramingState.NEGOTIATED) {
            return state == CanvasProcessFramingState.HANDSHAKE
                    ? CanvasProcessFramingError.PAYLOAD_NOT_NEGOTIATED
                    : CanvasProcessFramingError.WRONG_STATE;
        }
        if (!capabilities.contains(CanvasWireCapability.READ_ONLY_RENDER)) {
            return CanvasProcessFramingError.CAPABILITY_NOT_NEGOTIATED;
        }
        if (kind == CanvasProcessFrameKind.IMAGE_BYTES
                && !capabilities.contains(
                        CanvasWireCapability.ASSET_IMAGE_BYTES_V1)) {
            return CanvasProcessFramingError.CAPABILITY_NOT_NEGOTIATED;
        }
        return null;
    }
}

package dev.flutter.netbeans.designer.canvas.transport;

import java.util.Arrays;
import java.util.Objects;

/** One immutable already-bounded process frame. */
public final class CanvasProcessFrame {
    private final CanvasProcessFrameKind kind;
    private final byte[] payload;
    private final byte[] digest;
    private final int hashCode;

    public CanvasProcessFrame(CanvasProcessFrameKind kind, byte[] payload) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.payload = requirePayload(payload).clone();
        this.digest = CanvasProcessDigests.sha256(this.payload);
        this.hashCode = 31 * kind.hashCode() + Arrays.hashCode(this.payload);
    }

    static CanvasProcessFrame takeOwnership(
            CanvasProcessFrameKind kind,
            byte[] payload,
            byte[] digest) {
        requirePayload(payload);
        return new CanvasProcessFrame(kind, payload, digest);
    }

    private CanvasProcessFrame(
            CanvasProcessFrameKind kind,
            byte[] payload,
            byte[] digest) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.payload = payload;
        this.digest = digest;
        this.hashCode = 31 * kind.hashCode() + Arrays.hashCode(this.payload);
    }

    public CanvasProcessFrameKind kind() {
        return kind;
    }

    public int size() {
        return payload.length;
    }

    public byte[] copyPayload() {
        return payload.clone();
    }

    public CanvasProcessPayloadDescriptor descriptor() {
        return new CanvasProcessPayloadDescriptor(kind, payload.length, digest);
    }

    public boolean contentEquals(byte[] candidate) {
        return candidate != null && Arrays.equals(payload, candidate);
    }

    byte[] ownedPayload() {
        return payload;
    }

    byte[] copyDigest() {
        return digest.clone();
    }

    byte[] ownedDigest() {
        return digest;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof CanvasProcessFrame frame
                && kind == frame.kind
                && Arrays.equals(payload, frame.payload);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "CanvasProcessFrame[kind=" + kind + ", size=" + payload.length + ']';
    }

    private static byte[] requirePayload(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length == 0) {
            throw new IllegalArgumentException(
                    "Canvas process frame payload must not be empty");
        }
        return payload;
    }
}

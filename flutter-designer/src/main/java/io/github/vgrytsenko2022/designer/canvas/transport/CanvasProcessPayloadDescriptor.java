package io.github.vgrytsenko2022.designer.canvas.transport;

import java.util.HexFormat;
import java.util.Objects;

/** Exact control-plane declaration for one following non-control payload. */
public final class CanvasProcessPayloadDescriptor {
    public static final int SHA256_BYTES = 32;

    private final CanvasProcessFrameKind kind;
    private final int payloadBytes;
    private final byte[] sha256;

    public CanvasProcessPayloadDescriptor(
            CanvasProcessFrameKind kind,
            int payloadBytes,
            byte[] sha256) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (kind == CanvasProcessFrameKind.CONTROL_JSON) {
            throw new IllegalArgumentException(
                    "Control frames do not use payload descriptors");
        }
        if (payloadBytes <= 0) {
            throw new IllegalArgumentException(
                    "Canvas payload descriptor length must be positive");
        }
        Objects.requireNonNull(sha256, "sha256");
        if (sha256.length != SHA256_BYTES) {
            throw new IllegalArgumentException(
                    "Canvas payload descriptor SHA-256 must contain 32 bytes");
        }
        this.payloadBytes = payloadBytes;
        this.sha256 = sha256.clone();
    }

    public CanvasProcessFrameKind kind() {
        return kind;
    }

    public int payloadBytes() {
        return payloadBytes;
    }

    public byte[] copySha256() {
        return sha256.clone();
    }

    boolean matches(
            CanvasProcessFrameKind candidateKind,
            long candidateBytes,
            byte[] header,
            int digestOffset) {
        if (kind != candidateKind || payloadBytes != candidateBytes) {
            return false;
        }
        int difference = 0;
        for (int index = 0; index < SHA256_BYTES; index++) {
            difference |= sha256[index] ^ header[digestOffset + index];
        }
        return difference == 0;
    }

    boolean matches(CanvasProcessFrame frame) {
        return kind == frame.kind()
                && payloadBytes == frame.size()
                && java.security.MessageDigest.isEqual(
                        sha256, frame.ownedDigest());
    }

    @Override
    public String toString() {
        return "CanvasProcessPayloadDescriptor[kind=" + kind
                + ", payloadBytes=" + payloadBytes
                + ", sha256=" + HexFormat.of().formatHex(sha256) + ']';
    }
}

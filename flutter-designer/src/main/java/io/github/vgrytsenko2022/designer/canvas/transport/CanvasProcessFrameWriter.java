package io.github.vgrytsenko2022.designer.canvas.transport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/** One serialized, fail-stop writer bound to exactly one process stream. */
public final class CanvasProcessFrameWriter {
    private final OutputStream output;
    private boolean poisoned;

    CanvasProcessFrameWriter(OutputStream output) {
        this.output = output;
    }

    /** Writes one control frame. */
    public synchronized void write(
            CanvasProcessFramingPolicy policy,
            CanvasProcessFrame frame) throws IOException {
        write(policy, frame, null);
    }

    /** Writes one complete frame bound to its admitted payload descriptor. */
    public synchronized void write(
            CanvasProcessFramingPolicy policy,
            CanvasProcessFrame frame,
            CanvasProcessPayloadDescriptor expectedPayload) throws IOException {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(frame, "frame");
        requireUsable();
        try {
            validate(policy, frame, expectedPayload);
            byte[] header = header(frame);
            output.write(header);
            output.write(frame.ownedPayload());
            output.flush();
        } catch (IOException failure) {
            poisoned = true;
            throw failure;
        } catch (RuntimeException failure) {
            poisoned = true;
            throw failure;
        }
    }

    public synchronized boolean isPoisoned() {
        return poisoned;
    }

    private void requireUsable() throws CanvasProcessFramingException {
        if (poisoned) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.STREAM_POISONED,
                    "Canvas process frame writer is poisoned.");
        }
    }

    private static void validate(
            CanvasProcessFramingPolicy policy,
            CanvasProcessFrame frame,
            CanvasProcessPayloadDescriptor expectedPayload)
            throws CanvasProcessFramingException {
        CanvasProcessFramingError rejection = policy.rejection(frame.kind());
        if (rejection != null) {
            throw CanvasProcessFrameCodec.failure(
                    rejection,
                    "Canvas process frame kind " + frame.kind()
                            + " is not legal for " + policy.direction()
                            + " while " + policy.state() + '.');
        }
        if (frame.size() > policy.maxPayloadBytes(frame.kind())) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_LIMIT,
                    "Canvas process frame exceeds the negotiated payload limit.");
        }
        boolean control = frame.kind() == CanvasProcessFrameKind.CONTROL_JSON;
        if (control && expectedPayload == null) {
            return;
        }
        if (expectedPayload == null) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_REQUIRED,
                    "Canvas non-control payload has no admitted descriptor.");
        }
        if (control || !expectedPayload.matches(frame)) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH,
                    "Canvas process frame does not match its admitted descriptor.");
        }
    }

    private static byte[] header(CanvasProcessFrame frame) {
        byte[] header = new byte[CanvasProcessFrameCodec.HEADER_BYTES];
        System.arraycopy(
                CanvasProcessFrameCodec.MAGIC,
                0,
                header,
                0,
                CanvasProcessFrameCodec.MAGIC.length);
        header[4] = CanvasProcessFrameCodec.VERSION;
        header[5] = (byte) frame.kind().wireCode();
        CanvasProcessFrameCodec.writeUnsignedInt(header, 8, frame.size());
        System.arraycopy(
                frame.ownedDigest(),
                0,
                header,
                CanvasProcessFrameCodec.DIGEST_OFFSET,
                CanvasProcessFrameCodec.DIGEST_BYTES);
        return header;
    }
}

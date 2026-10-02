package io.github.vgrytsenko2022.designer.canvas.transport;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Optional;

/**
 * One serialized, fail-stop reader bound to exactly one process stream.
 * Framing failures poison the reader; byte-stream resynchronization is never
 * attempted.
 */
public final class CanvasProcessFrameReader {
    private final InputStream input;
    private boolean poisoned;
    private boolean ended;

    CanvasProcessFrameReader(InputStream input) {
        this.input = input;
    }

    /** Reads one control frame, or returns empty on clean end of stream. */
    public synchronized Optional<CanvasProcessFrame> read(
            CanvasProcessFramingPolicy policy) throws IOException {
        return read(policy, null);
    }

    /**
     * Reads one frame declared by {@code expectedPayload}; pass {@code null}
     * only when the next frame must be a control frame.
     */
    public synchronized Optional<CanvasProcessFrame> read(
            CanvasProcessFramingPolicy policy,
            CanvasProcessPayloadDescriptor expectedPayload) throws IOException {
        Objects.requireNonNull(policy, "policy");
        requireUsable();
        if (ended) {
            if (expectedPayload != null) {
                return poison(CanvasProcessFrameCodec.failure(
                        CanvasProcessFramingError.TRUNCATED_HEADER,
                        "Canvas process stream ended before its declared payload frame."));
            }
            return Optional.empty();
        }
        try {
            Optional<CanvasProcessFrame> result = readOne(policy, expectedPayload);
            if (result.isEmpty()) {
                ended = true;
                if (expectedPayload != null) {
                    throw CanvasProcessFrameCodec.failure(
                            CanvasProcessFramingError.TRUNCATED_HEADER,
                            "Canvas process stream ended before its declared payload frame.");
                }
            }
            return result;
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

    private Optional<CanvasProcessFrame> readOne(
            CanvasProcessFramingPolicy policy,
            CanvasProcessPayloadDescriptor expectedPayload) throws IOException {
        int first = input.read();
        if (first < 0) {
            return Optional.empty();
        }
        byte[] header = new byte[CanvasProcessFrameCodec.HEADER_BYTES];
        header[0] = (byte) first;
        readExact(
                header,
                1,
                CanvasProcessFrameCodec.MAGIC.length - 1,
                CanvasProcessFramingError.TRUNCATED_HEADER,
                "Canvas process frame header ended before 44 bytes.");
        validateMagic(header);
        readExact(
                header,
                CanvasProcessFrameCodec.MAGIC.length,
                CanvasProcessFrameCodec.HEADER_BYTES
                        - CanvasProcessFrameCodec.MAGIC.length,
                CanvasProcessFramingError.TRUNCATED_HEADER,
                "Canvas process frame header ended before 44 bytes.");
        if (Byte.toUnsignedInt(header[4]) != CanvasProcessFrameCodec.VERSION) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.UNSUPPORTED_VERSION,
                    "Canvas process frame version is not supported.");
        }
        CanvasProcessFrameKind kind = CanvasProcessFrameKind.fromWireCode(
                Byte.toUnsignedInt(header[5])).orElseThrow(() ->
                        CanvasProcessFrameCodec.failure(
                                CanvasProcessFramingError.UNKNOWN_KIND,
                                "Canvas process frame kind is not supported."));
        if (header[6] != 0 || header[7] != 0) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.INVALID_FLAGS,
                    "Canvas process frame flags must be zero in version 1.");
        }
        requireAllowed(kind, policy);
        long payloadLength = CanvasProcessFrameCodec.unsignedInt(header, 8);
        if (payloadLength == 0) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.EMPTY_PAYLOAD,
                    "Canvas process frame payload must not be empty.");
        }
        if (payloadLength > policy.maxPayloadBytes(kind)) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_LIMIT,
                    "Canvas process frame exceeds the negotiated payload limit.");
        }
        validateDescriptor(kind, payloadLength, header, expectedPayload);

        byte[] payload = new byte[(int) payloadLength];
        readExact(
                payload,
                0,
                payload.length,
                CanvasProcessFramingError.TRUNCATED_PAYLOAD,
                "Canvas process frame payload ended before its declared length.");
        byte[] digest = CanvasProcessDigests.sha256(payload);
        if (!digestMatchesHeader(digest, header)) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.DIGEST_MISMATCH,
                    "Canvas process frame payload digest does not match its header.");
        }
        return Optional.of(CanvasProcessFrame.takeOwnership(kind, payload, digest));
    }

    private void requireUsable() throws CanvasProcessFramingException {
        if (poisoned) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.STREAM_POISONED,
                    "Canvas process frame reader is poisoned.");
        }
    }

    private <T> T poison(IOException failure) throws IOException {
        poisoned = true;
        throw failure;
    }

    private static void requireAllowed(
            CanvasProcessFrameKind kind,
            CanvasProcessFramingPolicy policy)
            throws CanvasProcessFramingException {
        CanvasProcessFramingError rejection = policy.rejection(kind);
        if (rejection != null) {
            throw CanvasProcessFrameCodec.failure(
                    rejection,
                    "Canvas process frame kind " + kind
                            + " is not legal for " + policy.direction()
                            + " while " + policy.state() + '.');
        }
    }

    private static void validateDescriptor(
            CanvasProcessFrameKind kind,
            long payloadLength,
            byte[] header,
            CanvasProcessPayloadDescriptor expectedPayload)
            throws CanvasProcessFramingException {
        boolean control = kind == CanvasProcessFrameKind.CONTROL_JSON;
        if (control && expectedPayload == null) {
            return;
        }
        if (expectedPayload == null) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_REQUIRED,
                    "Canvas non-control payload has no admitted descriptor.");
        }
        if (control || !expectedPayload.matches(
                kind,
                payloadLength,
                header,
                CanvasProcessFrameCodec.DIGEST_OFFSET)) {
            throw CanvasProcessFrameCodec.failure(
                    CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH,
                    "Canvas process frame does not match its admitted descriptor.");
        }
    }

    private static void validateMagic(byte[] header)
            throws CanvasProcessFramingException {
        for (int index = 0; index < CanvasProcessFrameCodec.MAGIC.length; index++) {
            if (header[index] != CanvasProcessFrameCodec.MAGIC[index]) {
                throw CanvasProcessFrameCodec.failure(
                        CanvasProcessFramingError.INVALID_MAGIC,
                        "Canvas process frame magic is invalid.");
            }
        }
    }

    private void readExact(
            byte[] target,
            int offset,
            int length,
            CanvasProcessFramingError eofError,
            String eofMessage) throws IOException {
        int remaining = length;
        int cursor = offset;
        while (remaining > 0) {
            int read = input.read(target, cursor, remaining);
            if (read < 0) {
                throw CanvasProcessFrameCodec.failure(eofError, eofMessage);
            }
            if (read == 0) {
                int one = input.read();
                if (one < 0) {
                    throw CanvasProcessFrameCodec.failure(eofError, eofMessage);
                }
                target[cursor] = (byte) one;
                read = 1;
            }
            cursor += read;
            remaining -= read;
        }
    }

    private static boolean digestMatchesHeader(byte[] digest, byte[] header) {
        byte[] expected = new byte[CanvasProcessFrameCodec.DIGEST_BYTES];
        System.arraycopy(
                header,
                CanvasProcessFrameCodec.DIGEST_OFFSET,
                expected,
                0,
                expected.length);
        return MessageDigest.isEqual(digest, expected);
    }
}

package io.github.vgrytsenko2022.designer.canvas.transport;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/** Factory and stable constants for stream-bound Canvas frame readers/writers. */
public final class CanvasProcessFrameCodec {
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 44;
    static final byte[] MAGIC = {'N', 'B', 'F', 'C'};
    static final int DIGEST_OFFSET = 12;
    static final int DIGEST_BYTES = 32;

    public CanvasProcessFrameReader reader(InputStream input) {
        return new CanvasProcessFrameReader(
                Objects.requireNonNull(input, "input"));
    }

    public CanvasProcessFrameWriter writer(OutputStream output) {
        return new CanvasProcessFrameWriter(
                Objects.requireNonNull(output, "output"));
    }

    static long unsignedInt(byte[] bytes, int offset) {
        return ((long) Byte.toUnsignedInt(bytes[offset]) << 24)
                | ((long) Byte.toUnsignedInt(bytes[offset + 1]) << 16)
                | ((long) Byte.toUnsignedInt(bytes[offset + 2]) << 8)
                | Byte.toUnsignedInt(bytes[offset + 3]);
    }

    static void writeUnsignedInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    static CanvasProcessFramingException failure(
            CanvasProcessFramingError error,
            String message) {
        return new CanvasProcessFramingException(error, message);
    }
}

package io.github.vgrytsenko2022.designer.canvas.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireCapability;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireHandshakeLimits;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireLimits;
import io.github.vgrytsenko2022.designer.canvas.protocol.CanvasWireNegotiation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CanvasProcessFrameCodecTest {
    private static final CanvasWireLimits CONTROL_LIMITS =
            CanvasWireLimits.defaults();
    private static final CanvasWireHandshakeLimits PAYLOAD_LIMITS =
            CanvasWireHandshakeLimits.defaults();
    private static final CanvasEngineIdentity ENGINE = new CanvasEngineIdentity(
            "3.44.8", "framework", "engine", "3.12.0");

    private final CanvasProcessFrameCodec codec = new CanvasProcessFrameCodec();

    @Test
    void roundTripsFragmentedFramesAndCleanEndOfStream() throws Exception {
        CanvasProcessFramingPolicy policy = hostToRunner();
        List<CanvasProcessFrame> expected = List.of(
                frame(CanvasProcessFrameKind.CONTROL_JSON, "hello"),
                frame(CanvasProcessFrameKind.MODEL_JSON, "model"),
                frame(CanvasProcessFrameKind.CATALOG_JSON, "catalog"),
                frame(CanvasProcessFrameKind.IMAGE_BYTES, "image"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CanvasProcessFrameWriter writer = codec.writer(bytes);
        writer.write(policy, expected.get(0));
        writer.write(policy, expected.get(1), expected.get(1).descriptor());
        writer.write(policy, expected.get(2), expected.get(2).descriptor());
        CanvasProcessFramingPolicy imagePolicy = hostToRunnerWithImages();
        writer.write(imagePolicy, expected.get(3), expected.get(3).descriptor());

        CanvasProcessFrameReader reader = codec.reader(
                new FragmentedInputStream(bytes.toByteArray(), 2));
        assertEquals(expected.get(0), reader.read(policy).orElseThrow());
        assertEquals(expected.get(1), reader.read(
                policy, expected.get(1).descriptor()).orElseThrow());
        assertEquals(expected.get(2), reader.read(
                policy, expected.get(2).descriptor()).orElseThrow());
        assertEquals(expected.get(3), reader.read(
                imagePolicy, expected.get(3).descriptor()).orElseThrow());
        assertTrue(reader.read(policy).isEmpty());
        assertTrue(reader.read(policy).isEmpty());
        assertFalse(reader.isPoisoned());
    }

    @Test
    void directionStateAndCapabilityAreEnforcedBeforePayloadRead() throws Exception {
        CanvasProcessFrame model = frame(CanvasProcessFrameKind.MODEL_JSON, "model");
        byte[] encoded = encoded(hostToRunner(), model, model.descriptor());

        assertHeaderFailure(encoded, runnerToHost(), model.descriptor(),
                CanvasProcessFramingError.WRONG_DIRECTION);
        assertHeaderFailure(encoded, handshake(), model.descriptor(),
                CanvasProcessFramingError.PAYLOAD_NOT_NEGOTIATED);
        assertHeaderFailure(encoded, hostToRunner().closing(), model.descriptor(),
                CanvasProcessFramingError.WRONG_STATE);
        assertHeaderFailure(encoded, hostToRunnerWithoutCapabilities(),
                model.descriptor(),
                CanvasProcessFramingError.CAPABILITY_NOT_NEGOTIATED);
    }

    @Test
    void exactDescriptorIsRequiredAndBoundBeforeAllocationOrBodyRead()
            throws Exception {
        CanvasProcessFrame model = frame(CanvasProcessFrameKind.MODEL_JSON, "model");
        byte[] encoded = encoded(hostToRunner(), model, model.descriptor());

        assertHeaderFailure(encoded, hostToRunner(), null,
                CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_REQUIRED);
        CanvasProcessFrame wrongKind = frame(
                CanvasProcessFrameKind.CATALOG_JSON, "model");
        assertHeaderFailure(encoded, hostToRunner(), wrongKind.descriptor(),
                CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH);
        CanvasProcessFrame wrongLength = frame(
                CanvasProcessFrameKind.MODEL_JSON, "models");
        assertHeaderFailure(encoded, hostToRunner(), wrongLength.descriptor(),
                CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH);
        CanvasProcessFrame wrongDigest = frame(
                CanvasProcessFrameKind.MODEL_JSON, "other");
        assertHeaderFailure(encoded, hostToRunner(), wrongDigest.descriptor(),
                CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH);
    }

    @Test
    void exactNegotiatedLimitPassesAndOnePastFailsBeforeAllocation()
            throws Exception {
        CanvasWireHandshakeLimits tiny = new CanvasWireHandshakeLimits(
                128, 5, 4, 3, 2, 1, 1);
        CanvasProcessFramingPolicy policy = negotiated(
                tiny,
                CanvasProcessDirection.HOST_TO_RUNNER,
                List.of(CanvasWireCapability.READ_ONLY_RENDER));
        CanvasProcessFrame exact = new CanvasProcessFrame(
                CanvasProcessFrameKind.MODEL_JSON, new byte[5]);

        byte[] encoded = encoded(policy, exact, exact.descriptor());
        assertEquals(exact, codec.reader(new ByteArrayInputStream(encoded)).read(
                policy, exact.descriptor()).orElseThrow());

        byte[] headerOnly = encoded.clone();
        headerOnly[11] = 6;
        assertHeaderFailure(headerOnly, policy, exact.descriptor(),
                CanvasProcessFramingError.PAYLOAD_LIMIT);
    }

    @Test
    void imageFramesRequireTheirCapabilityAndUseTheNegotiatedImageBound()
            throws Exception {
        CanvasWireHandshakeLimits tiny = new CanvasWireHandshakeLimits(
                128, 32, 32, 32, 5, 1, 1);
        CanvasProcessFramingPolicy admitted = negotiated(
                tiny,
                CanvasProcessDirection.HOST_TO_RUNNER,
                List.of(
                        CanvasWireCapability.READ_ONLY_RENDER,
                        CanvasWireCapability.ASSET_IMAGE_BYTES_V1));
        CanvasProcessFrame exact = new CanvasProcessFrame(
                CanvasProcessFrameKind.IMAGE_BYTES, new byte[5]);
        byte[] encoded = encoded(admitted, exact, exact.descriptor());

        assertEquals(exact, codec.reader(new ByteArrayInputStream(encoded)).read(
                admitted, exact.descriptor()).orElseThrow());
        assertHeaderFailure(encoded, negotiated(
                        tiny,
                        CanvasProcessDirection.HOST_TO_RUNNER,
                        List.of(CanvasWireCapability.READ_ONLY_RENDER)),
                exact.descriptor(),
                CanvasProcessFramingError.CAPABILITY_NOT_NEGOTIATED);

        byte[] tooLargeHeader = encoded.clone();
        tooLargeHeader[11] = 6;
        assertHeaderFailure(tooLargeHeader, admitted, exact.descriptor(),
                CanvasProcessFramingError.PAYLOAD_LIMIT);
    }

    @Test
    void rejectsEmptyPayloadAtObjectAndWireBoundaries() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasProcessFrame(
                        CanvasProcessFrameKind.CONTROL_JSON, new byte[0]));

        CanvasProcessFrame control = frame(
                CanvasProcessFrameKind.CONTROL_JSON, "hello");
        byte[] encoded = encoded(handshake(), control, null);
        encoded[8] = 0;
        encoded[9] = 0;
        encoded[10] = 0;
        encoded[11] = 0;
        assertHeaderFailure(encoded, handshake(), null,
                CanvasProcessFramingError.EMPTY_PAYLOAD);
    }

    @Test
    void malformedInputPoisonsBoundReaderWithoutResynchronization()
            throws Exception {
        CanvasProcessFrame valid = frame(
                CanvasProcessFrameKind.CONTROL_JSON, "hello");
        byte[] validBytes = encoded(handshake(), valid, null);
        byte[] invalidThenValid = concat(
                copyWith(validBytes, 0, 'X'), validBytes);
        CanvasProcessFrameReader reader = codec.reader(
                new ByteArrayInputStream(invalidThenValid));

        assertReadFailure(reader, handshake(), null,
                CanvasProcessFramingError.INVALID_MAGIC);
        assertTrue(reader.isPoisoned());
        assertReadFailure(reader, handshake(), null,
                CanvasProcessFramingError.STREAM_POISONED);
    }

    @Test
    void invalidMagicFailsAfterFourBytesWithoutWaitingForAWholeHeader() {
        CountingInputStream input = new CountingInputStream(
                new byte[] {'N', 'O', 'P', 'E'});
        CanvasProcessFrameReader reader = codec.reader(input);

        assertReadFailure(reader, handshake(), null,
                CanvasProcessFramingError.INVALID_MAGIC);
        assertEquals(CanvasProcessFrameCodec.MAGIC.length, input.readCount());
        assertTrue(reader.isPoisoned());
    }

    @Test
    void partialOutputFailurePoisonsBoundWriter() throws Exception {
        CanvasProcessFrameWriter writer = codec.writer(new FailingOutputStream(8));
        CanvasProcessFrame control = frame(
                CanvasProcessFrameKind.CONTROL_JSON, "hello");

        assertThrows(IOException.class, () -> writer.write(handshake(), control));
        assertTrue(writer.isPoisoned());
        CanvasProcessFramingException poisoned = assertThrows(
                CanvasProcessFramingException.class,
                () -> writer.write(handshake(), control));
        assertEquals(CanvasProcessFramingError.STREAM_POISONED, poisoned.error());
    }

    @Test
    void descriptorMismatchWritesNoBytesAndPoisonsBoundWriter() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CanvasProcessFrameWriter writer = codec.writer(bytes);
        CanvasProcessFrame model = frame(CanvasProcessFrameKind.MODEL_JSON, "model");
        CanvasProcessFrame other = frame(CanvasProcessFrameKind.MODEL_JSON, "other");

        CanvasProcessFramingException mismatch = assertThrows(
                CanvasProcessFramingException.class,
                () -> writer.write(hostToRunner(), model, other.descriptor()));
        assertEquals(CanvasProcessFramingError.PAYLOAD_DESCRIPTOR_MISMATCH,
                mismatch.error());
        assertEquals(0, bytes.size());
        assertTrue(writer.isPoisoned());
    }

    @Test
    void declaredPayloadAtCleanEofPoisonsReader() {
        CanvasProcessFrame model = frame(CanvasProcessFrameKind.MODEL_JSON, "model");
        CanvasProcessFrameReader reader = codec.reader(
                new ByteArrayInputStream(new byte[0]));

        assertReadFailure(reader, hostToRunner(), model.descriptor(),
                CanvasProcessFramingError.TRUNCATED_HEADER);
        assertTrue(reader.isPoisoned());
    }

    @Test
    void rejectsMalformedHeadersTruncationAndDigestMismatch() throws Exception {
        CanvasProcessFrame validFrame = frame(
                CanvasProcessFrameKind.CONTROL_JSON, "hello");
        byte[] valid = encoded(handshake(), validFrame, null);

        assertFailure(copyWith(valid, 4, 2), handshake(),
                CanvasProcessFramingError.UNSUPPORTED_VERSION);
        assertFailure(copyWith(valid, 5, 99), handshake(),
                CanvasProcessFramingError.UNKNOWN_KIND);
        assertFailure(copyWith(valid, 6, 1), handshake(),
                CanvasProcessFramingError.INVALID_FLAGS);
        assertFailure(java.util.Arrays.copyOf(valid, 7), handshake(),
                CanvasProcessFramingError.TRUNCATED_HEADER);
        assertFailure(java.util.Arrays.copyOf(valid, valid.length - 1), handshake(),
                CanvasProcessFramingError.TRUNCATED_PAYLOAD);
        assertFailure(copyWith(valid, 12, valid[12] ^ 1), handshake(),
                CanvasProcessFramingError.DIGEST_MISMATCH);
    }

    @Test
    void concurrentWritesThroughOneBoundWriterRemainWholeFrames() throws Exception {
        CanvasProcessFramingPolicy policy = handshake();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CanvasProcessFrameWriter writer = codec.writer(bytes);
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        Set<String> expected = new HashSet<>();
        for (int index = 0; index < 32; index++) {
            String value = "frame-" + index + '-' + "x".repeat(1_000);
            expected.add(value);
            writes.add(CompletableFuture.runAsync(() -> {
                try {
                    writer.write(policy,
                            frame(CanvasProcessFrameKind.CONTROL_JSON, value));
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
            }));
        }
        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).join();

        Set<String> actual = new HashSet<>();
        CanvasProcessFrameReader reader = codec.reader(
                new ByteArrayInputStream(bytes.toByteArray()));
        while (true) {
            var next = reader.read(policy);
            if (next.isEmpty()) {
                break;
            }
            actual.add(new String(
                    next.orElseThrow().copyPayload(), StandardCharsets.UTF_8));
        }
        assertEquals(expected, actual);
    }

    private void assertHeaderFailure(
            byte[] bytes,
            CanvasProcessFramingPolicy policy,
            CanvasProcessPayloadDescriptor descriptor,
            CanvasProcessFramingError expected) {
        CountingInputStream input = new CountingInputStream(bytes);
        CanvasProcessFrameReader reader = codec.reader(input);
        assertReadFailure(reader, policy, descriptor, expected);
        assertEquals(CanvasProcessFrameCodec.HEADER_BYTES, input.readCount());
        assertTrue(reader.isPoisoned());
    }

    private void assertFailure(
            byte[] bytes,
            CanvasProcessFramingPolicy policy,
            CanvasProcessFramingError expected) {
        assertReadFailure(codec.reader(new ByteArrayInputStream(bytes)),
                policy, null, expected);
    }

    private static void assertReadFailure(
            CanvasProcessFrameReader reader,
            CanvasProcessFramingPolicy policy,
            CanvasProcessPayloadDescriptor descriptor,
            CanvasProcessFramingError expected) {
        CanvasProcessFramingException failure = assertThrows(
                CanvasProcessFramingException.class,
                () -> reader.read(policy, descriptor));
        assertEquals(expected, failure.error());
    }

    private byte[] encoded(
            CanvasProcessFramingPolicy policy,
            CanvasProcessFrame frame,
            CanvasProcessPayloadDescriptor descriptor) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        codec.writer(bytes).write(policy, frame, descriptor);
        return bytes.toByteArray();
    }

    private static CanvasProcessFrame frame(
            CanvasProcessFrameKind kind,
            String value) {
        return new CanvasProcessFrame(kind, value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] copyWith(byte[] source, int index, int value) {
        byte[] result = source.clone();
        result[index] = (byte) value;
        return result;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static CanvasProcessFramingPolicy handshake() {
        return CanvasProcessFramingPolicy.handshake(
                CONTROL_LIMITS,
                PAYLOAD_LIMITS,
                CanvasProcessDirection.HOST_TO_RUNNER);
    }

    private static CanvasProcessFramingPolicy hostToRunner() {
        return negotiated(
                PAYLOAD_LIMITS,
                CanvasProcessDirection.HOST_TO_RUNNER,
                List.of(CanvasWireCapability.READ_ONLY_RENDER));
    }

    private static CanvasProcessFramingPolicy runnerToHost() {
        return negotiated(
                PAYLOAD_LIMITS,
                CanvasProcessDirection.RUNNER_TO_HOST,
                List.of(CanvasWireCapability.READ_ONLY_RENDER));
    }

    private static CanvasProcessFramingPolicy hostToRunnerWithImages() {
        return negotiated(
                PAYLOAD_LIMITS,
                CanvasProcessDirection.HOST_TO_RUNNER,
                List.of(
                        CanvasWireCapability.READ_ONLY_RENDER,
                        CanvasWireCapability.ASSET_IMAGE_BYTES_V1));
    }

    private static CanvasProcessFramingPolicy hostToRunnerWithoutCapabilities() {
        return negotiated(
                PAYLOAD_LIMITS,
                CanvasProcessDirection.HOST_TO_RUNNER,
                List.of());
    }

    private static CanvasProcessFramingPolicy negotiated(
            CanvasWireHandshakeLimits limits,
            CanvasProcessDirection direction,
            List<CanvasWireCapability> capabilities) {
        return CanvasProcessFramingPolicy.negotiated(
                CONTROL_LIMITS,
                new CanvasWireNegotiation(
                        "0.1.3",
                        ENGINE,
                        capabilities,
                        limits),
                direction);
    }

    private static final class FragmentedInputStream extends InputStream {
        private final byte[] bytes;
        private final int maximumChunk;
        private int index;

        FragmentedInputStream(byte[] bytes, int maximumChunk) {
            this.bytes = bytes.clone();
            this.maximumChunk = maximumChunk;
        }

        @Override
        public int read() {
            return index == bytes.length ? -1 : Byte.toUnsignedInt(bytes[index++]);
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            if (index == bytes.length) {
                return -1;
            }
            int count = Math.min(
                    Math.min(length, maximumChunk), bytes.length - index);
            System.arraycopy(bytes, index, target, offset, count);
            index += count;
            return count;
        }
    }

    private static final class CountingInputStream extends ByteArrayInputStream {
        private int readCount;

        CountingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public synchronized int read() {
            int value = super.read();
            if (value >= 0) {
                readCount++;
            }
            return value;
        }

        @Override
        public synchronized int read(byte[] target, int offset, int length) {
            int count = super.read(target, offset, length);
            if (count > 0) {
                readCount += count;
            }
            return count;
        }

        int readCount() {
            return readCount;
        }
    }

    private static final class FailingOutputStream extends OutputStream {
        private int remaining;

        FailingOutputStream(int successfulBytes) {
            this.remaining = successfulBytes;
        }

        @Override
        public void write(int value) throws IOException {
            if (remaining == 0) {
                throw new IOException("synthetic partial write");
            }
            remaining--;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            int accepted = Math.min(remaining, length);
            remaining -= accepted;
            if (accepted != length) {
                throw new IOException("synthetic partial write");
            }
        }
    }
}

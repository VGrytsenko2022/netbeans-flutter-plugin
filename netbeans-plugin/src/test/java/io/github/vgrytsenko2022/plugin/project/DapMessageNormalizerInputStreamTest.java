package io.github.vgrytsenko2022.plugin.project;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DapMessageNormalizerInputStreamTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void normalizesOutputEventAcrossFragmentedReads() throws Exception {
        byte[] input = frame("""
                {"seq":7,"type":"event","event":"output","body":{"output":"building...\\n"}}
                """.trim());
        InputStream fragments = new FragmentedInputStream(input, 1, 2, 3, 1, 5);

        byte[] output = readInSmallChunks(
                new DapMessageNormalizerInputStream(fragments), 2);
        List<Frame> frames = decodeFrames(output);

        assertEquals(1, frames.size());
        JsonNode message = JSON.readTree(frames.get(0).body());
        assertEquals("building...\n", message.path("body").path("output").asText());
        assertEquals("console", message.path("body").path("category").asText());
    }

    @Test
    void normalizesExplicitNullCategory() throws Exception {
        byte[] input = frame("""
                {"type":"event","event":"output","body":{"category":null,"output":"text"}}
                """.trim());

        byte[] output = new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input)).readAllBytes();
        JsonNode message = JSON.readTree(decodeFrames(output).get(0).body());

        assertEquals("console", message.path("body").path("category").asText());
    }

    @Test
    void normalizesBlankUnknownAndNonTextCategories() throws Exception {
        byte[] input = concatenate(
                frame("""
                        {"type":"event","event":"output","body":{"category":"","output":"blank"}}
                        """.trim()),
                frame("""
                        {"type":"event","event":"output","body":{"category":"custom","output":"unknown"}}
                        """.trim()),
                frame("""
                        {"type":"event","event":"output","body":{"category":7,"output":"number"}}
                        """.trim()));

        byte[] output = new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input)).readAllBytes();
        List<Frame> frames = decodeFrames(output);

        assertEquals(3, frames.size());
        for (Frame frame : frames) {
            assertEquals("console", JSON.readTree(frame.body())
                    .path("body").path("category").asText());
        }
    }

    @Test
    void handlesMultipleFramesWithoutMergingThem() throws Exception {
        byte[] outputEvent = frame("""
                {"seq":1,"type":"event","event":"output","body":{"output":"one"}}
                """.trim());
        byte[] response = frame("""
                {"seq":2,"type":"response","request_seq":1,"success":true,"command":"attach"}
                """.trim());
        byte[] input = concatenate(outputEvent, response);

        byte[] output = new DapMessageNormalizerInputStream(
                new FragmentedInputStream(input, 7, 1, 11, 2)).readAllBytes();
        List<Frame> frames = decodeFrames(output);

        assertEquals(2, frames.size());
        assertEquals("console", JSON.readTree(frames.get(0).body())
                .path("body").path("category").asText());
        assertEquals("response", JSON.readTree(frames.get(1).body())
                .path("type").asText());
        assertArrayEquals(response, frames.get(1).encoded());
    }

    @Test
    void leavesNonOutputEventByteForByteUnchanged() throws Exception {
        byte[] input = frame("""
                { "seq": 3, "type": "event", "event": "stopped", "body": {"reason":"breakpoint"} }
                """.trim());

        byte[] output = new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input)).readAllBytes();

        assertArrayEquals(input, output);
    }

    @Test
    void preservesExistingOutputCategoryByteForByte() throws Exception {
        byte[] input = frame("""
                { "type":"event", "event":"output", "body":{"category":"stderr","output":"failure"} }
                """.trim());

        byte[] output = new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input)).readAllBytes();

        assertArrayEquals(input, output);
    }

    @Test
    void recalculatesContentLengthInUtf8Bytes() throws Exception {
        String text = "Привіт, Flutter 👋";
        byte[] input = frame("""
                {"type":"event","event":"output","body":{"output":"%s"}}
                """.formatted(text).trim());

        byte[] output = new DapMessageNormalizerInputStream(
                new FragmentedInputStream(input, 1)).readAllBytes();
        Frame frame = decodeFrames(output).get(0);
        JsonNode message = JSON.readTree(frame.body());

        assertEquals(frame.body().length, frame.contentLength());
        assertEquals(text, message.path("body").path("output").asText());
        assertEquals("console", message.path("body").path("category").asText());
        assertTrue(frame.body().length > new String(frame.body(), StandardCharsets.UTF_8).length(),
                "The fixture must exercise multi-byte UTF-8 characters");
    }

    @Test
    void reportsTheNormalizedMessageToAnOptionalListener() throws Exception {
        byte[] input = frame("""
                {"seq":8,"type":"event","event":"output","body":{"output":"ready"}}
                """.trim());
        AtomicReference<JsonNode> observed = new AtomicReference<>();

        new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input), observed::set).readAllBytes();

        assertEquals(8, observed.get().path("seq").asInt());
        assertEquals("console", observed.get().path("body").path("category").asText());
    }

    @Test
    void rejectsATruncatedMessageBody() {
        byte[] input = "Content-Length: 10\r\n\r\n{}"
                .getBytes(StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> new DapMessageNormalizerInputStream(
                new ByteArrayInputStream(input)).readAllBytes());
    }

    private static byte[] frame(String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + body.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        return concatenate(header, body);
    }

    private static byte[] concatenate(byte[]... parts) {
        int length = Arrays.stream(parts).mapToInt(part -> part.length).sum();
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static byte[] readInSmallChunks(InputStream source, int chunkSize)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[chunkSize];
        int count;
        while ((count = source.read(chunk)) >= 0) {
            output.write(chunk, 0, count);
        }
        return output.toByteArray();
    }

    private static List<Frame> decodeFrames(byte[] stream) {
        List<Frame> frames = new ArrayList<>();
        int offset = 0;
        while (offset < stream.length) {
            int headerEnd = findHeaderEnd(stream, offset);
            assertTrue(headerEnd >= 0, "DAP header terminator is missing");
            String header = new String(
                    stream, offset, headerEnd - offset, StandardCharsets.US_ASCII);
            int contentLength = Arrays.stream(header.split("\\r\\n"))
                    .filter(line -> line.regionMatches(true, 0,
                            "Content-Length:", 0, "Content-Length:".length()))
                    .map(line -> line.substring(line.indexOf(':') + 1).trim())
                    .mapToInt(Integer::parseInt)
                    .findFirst()
                    .orElseThrow();
            int bodyStart = headerEnd + 4;
            int frameEnd = bodyStart + contentLength;
            assertTrue(frameEnd <= stream.length, "DAP body is shorter than Content-Length");
            byte[] body = Arrays.copyOfRange(stream, bodyStart, frameEnd);
            byte[] encoded = Arrays.copyOfRange(stream, offset, frameEnd);
            frames.add(new Frame(contentLength, body, encoded));
            offset = frameEnd;
        }
        return frames;
    }

    private static int findHeaderEnd(byte[] stream, int offset) {
        for (int index = offset; index <= stream.length - 4; index++) {
            if (stream[index] == '\r' && stream[index + 1] == '\n'
                    && stream[index + 2] == '\r' && stream[index + 3] == '\n') {
                return index;
            }
        }
        return -1;
    }

    private record Frame(int contentLength, byte[] body, byte[] encoded) {
    }

    private static final class FragmentedInputStream extends InputStream {
        private final byte[] content;
        private final int[] fragmentSizes;
        private int offset;
        private int fragmentIndex;

        private FragmentedInputStream(byte[] content, int... fragmentSizes) {
            this.content = content;
            this.fragmentSizes = fragmentSizes;
        }

        @Override
        public int read() {
            return offset == content.length ? -1 : Byte.toUnsignedInt(content[offset++]);
        }

        @Override
        public int read(byte[] target, int targetOffset, int length) {
            if (offset == content.length) {
                return -1;
            }
            int fragmentSize = fragmentSizes[fragmentIndex++ % fragmentSizes.length];
            int count = Math.min(Math.min(length, fragmentSize), content.length - offset);
            System.arraycopy(content, offset, target, targetOffset, count);
            offset += count;
            return count;
        }
    }
}

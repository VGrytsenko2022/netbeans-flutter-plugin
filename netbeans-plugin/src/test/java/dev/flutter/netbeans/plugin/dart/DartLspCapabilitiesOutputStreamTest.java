package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DartLspCapabilitiesOutputStreamTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final byte[] HEADER_END = {'\r', '\n', '\r', '\n'};

    @Test
    void injectsWorkspaceApplyEditIntoAChunkedInitializeRequest() throws Exception {
        String initialize = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"capabilities":{"workspace":{"workspaceEdit":{"documentChanges":true}}},"clientInfo":{"name":"NetBeans 30"}}}
                """.trim();
        byte[] input = frame(initialize);
        ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(delegate)) {
            for (byte value : input) {
                output.write(value);
            }
        }

        List<Frame> frames = frames(delegate.toByteArray());
        assertEquals(1, frames.size());
        JsonNode request = JSON.readTree(frames.get(0).body());
        assertTrue(request.path("params")
                .path("capabilities")
                .path("workspace")
                .path("applyEdit")
                .asBoolean());
        assertTrue(request.path("params")
                .path("capabilities")
                .path("workspace")
                .path("workspaceEdit")
                .path("documentChanges")
                .asBoolean());
        assertEquals("NetBeans 30", request.path("params").path("clientInfo").path("name").asText());
        assertEquals(frames.get(0).body().length, frames.get(0).contentLength());
    }

    @Test
    void leavesEveryNonInitializeFrameByteExact() throws Exception {
        byte[] before = frame("{\"jsonrpc\":\"2.0\",\"method\":\"initialized\",\"params\":{}}");
        byte[] initialize = frame(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"capabilities\":{}}}");
        byte[] after = frame(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"shutdown\",\"params\":null}");
        ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(delegate)) {
            output.write(join(before, initialize, after));
        }

        List<Frame> result = frames(delegate.toByteArray());
        assertEquals(3, result.size());
        assertArrayEquals(before, result.get(0).raw());
        assertArrayEquals(after, result.get(2).raw());
        assertTrue(JSON.readTree(result.get(1).body())
                .path("params")
                .path("capabilities")
                .path("workspace")
                .path("applyEdit")
                .asBoolean());
    }

    @Test
    void leavesAnAlreadyTruthfulInitializeRequestByteExact() throws Exception {
        byte[] initialize = frame(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"capabilities\":{\"workspace\":{\"applyEdit\":true}}}}");
        ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(delegate)) {
            output.write(initialize, 0, 9);
            output.flush();
            assertEquals(0, delegate.size(), "partial frame must not reach the Dart server");
            output.write(initialize, 9, initialize.length - 9);
        }

        assertArrayEquals(initialize, delegate.toByteArray());
    }

    @Test
    void doesNotMistakeJsonTextForASecondProtocolFrame() throws Exception {
        String initialize = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"capabilities":{},"trace":"Content-Length: 999\\r\\n\\r\\n"}}
                """.trim();
        byte[] next = frame("{\"jsonrpc\":\"2.0\",\"method\":\"exit\"}");
        ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(delegate)) {
            output.write(join(frame(initialize), next));
        }

        List<Frame> result = frames(delegate.toByteArray());
        assertEquals(2, result.size());
        assertFalse(JSON.readTree(result.get(0).body()).path("params").path("trace").asText().isEmpty());
        assertArrayEquals(next, result.get(1).raw());
    }

    private static byte[] frame(String body) {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + content.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        return join(header, content);
    }

    private static byte[] join(byte[]... chunks) {
        int length = Arrays.stream(chunks).mapToInt(chunk -> chunk.length).sum();
        byte[] joined = new byte[length];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, joined, offset, chunk.length);
            offset += chunk.length;
        }
        return joined;
    }

    private static List<Frame> frames(byte[] bytes) {
        List<Frame> frames = new ArrayList<>();
        int offset = 0;
        while (offset < bytes.length) {
            int headerEnd = findHeaderEnd(bytes, offset);
            assertTrue(headerEnd >= 0, "incomplete LSP header");
            String header = new String(
                    bytes,
                    offset,
                    headerEnd - HEADER_END.length - offset,
                    StandardCharsets.US_ASCII);
            int contentLength = -1;
            for (String line : header.split("\\r\\n")) {
                if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }
            assertTrue(contentLength >= 0, "missing Content-Length");
            int frameEnd = headerEnd + contentLength;
            assertTrue(frameEnd <= bytes.length, "incomplete LSP body");
            frames.add(new Frame(
                    Arrays.copyOfRange(bytes, offset, frameEnd),
                    Arrays.copyOfRange(bytes, headerEnd, frameEnd),
                    contentLength));
            offset = frameEnd;
        }
        return frames;
    }

    private static int findHeaderEnd(byte[] bytes, int start) {
        for (int index = start; index <= bytes.length - HEADER_END.length; index++) {
            if (bytes[index] == '\r'
                    && bytes[index + 1] == '\n'
                    && bytes[index + 2] == '\r'
                    && bytes[index + 3] == '\n') {
                return index + HEADER_END.length;
            }
        }
        return -1;
    }

    private record Frame(byte[] raw, byte[] body, int contentLength) {
    }
}

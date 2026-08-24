package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DartLspCompletionCompatibilityInputStreamTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void removesInitialTextEditFromCorrelatedResolvableItems() throws Exception {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        ByteArrayOutputStream requests = new ByteArrayOutputStream();
        byte[] request = frame("""
                {"jsonrpc":"2.0","id":7,"method":"textDocument/completion","params":{}}
                """.trim());
        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(requests, compatibility)) {
            output.write(request);
        }
        assertArrayEquals(request, requests.toByteArray());

        byte[] response = frame("""
                {"jsonrpc":"2.0","id":7,"result":{"isIncomplete":false,"items":[
                  {"label":"answer","data":{"file":"helper.dart","importUris":["helper.dart"]},"textEdit":{"range":{"start":{"line":1,"character":2},"end":{"line":1,"character":5}},"newText":"answer"}},
                  {"label":"local","textEdit":{"range":{"start":{"line":1,"character":2},"end":{"line":1,"character":5}},"newText":"local"}}
                ]}}
                """.trim());
        byte[] adapted = new DartLspCompletionCompatibilityInputStream(
                new ByteArrayInputStream(response), compatibility).readAllBytes();
        Frame decoded = decode(adapted);
        JsonNode items = JSON.readTree(decoded.body()).path("result").path("items");

        assertFalse(items.get(0).has("textEdit"));
        assertEquals("1", items.get(0).path("data")
                .path("__netbeans30CompletionBridge").path("version").asText());
        assertTrue(items.get(1).has("textEdit"),
                "an item without resolve data must retain its main edit");
        assertEquals(decoded.body().length, decoded.contentLength());

        JsonNode originalEdit = JSON.readTree(responseBody(response))
                .path("result").path("items").get(0).path("textEdit");
        byte[] resolveRequest = frame(JSON.writeValueAsString(JSON.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("id", 8)
                .put("method", "completionItem/resolve")
                .set("params", items.get(0))));
        ByteArrayOutputStream restoredRequest = new ByteArrayOutputStream();
        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(restoredRequest, compatibility)) {
            output.write(resolveRequest);
        }
        JsonNode restoredItem = JSON.readTree(responseBody(restoredRequest.toByteArray()))
                .path("params");
        assertEquals(originalEdit, restoredItem.path("textEdit"));
        assertEquals(JSON.createObjectNode()
                .put("file", "helper.dart")
                .set("importUris", JSON.createArrayNode().add("helper.dart")),
                restoredItem.path("data"));
        assertFalse(restoredItem.path("data").has("__netbeans30CompletionBridge"));
    }

    @Test
    void correlatesNumericAndStringIdsWithoutCrossingResponses() throws Exception {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        recordRequest(compatibility, "\"7\"");

        byte[] numeric = frame("""
                {"jsonrpc":"2.0","id":7,"result":{"items":[{"label":"A","data":{"importUris":["a.dart"]},"textEdit":{}}]}}
                """.trim());
        byte[] string = frame("""
                {"jsonrpc":"2.0","id":"7","result":{"items":[{"label":"A","data":{"importUris":["a.dart"]},"textEdit":{}}]}}
                """.trim());
        byte[] input = join(numeric, string);
        byte[] output = new DartLspCompletionCompatibilityInputStream(
                new ByteArrayInputStream(input), compatibility).readAllBytes();

        int firstLength = numeric.length;
        assertArrayEquals(numeric, Arrays.copyOfRange(output, 0, firstLength));
        JsonNode transformed = JSON.readTree(decode(
                Arrays.copyOfRange(output, firstLength, output.length)).body());
        assertFalse(transformed.path("result").path("items").get(0).has("textEdit"));
    }

    @Test
    void leavesUnrelatedAndErrorResponsesByteExact() throws Exception {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        recordRequest(compatibility, "9");
        byte[] unrelated = frame(
                "{\"jsonrpc\":\"2.0\",\"id\":8,\"result\":{\"items\":[]}}");
        byte[] error = frame(
                "{\"jsonrpc\":\"2.0\",\"id\":9,\"error\":{\"code\":-32603,\"message\":\"failed\"}}");
        byte[] input = join(unrelated, error);

        byte[] output = new DartLspCompletionCompatibilityInputStream(
                new ByteArrayInputStream(input), compatibility).readAllBytes();

        assertArrayEquals(input, output);
        assertFalse(compatibility.hasPendingRequests());
    }

    @Test
    void serverRequestWithCollidingIdDoesNotConsumeCompletionResponse() throws Exception {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        recordRequest(compatibility, "9");
        byte[] serverRequest = frame("""
                {"jsonrpc":"2.0","id":9,"method":"workspace/applyEdit","params":{"edit":{}}}
                """.trim());
        byte[] completionResponse = frame("""
                {"jsonrpc":"2.0","id":9,"result":{"items":[
                  {"label":"File","data":{"importUris":["dart:io"]},"textEdit":{"newText":"File"}}
                ]}}
                """.trim());

        byte[] output = new DartLspCompletionCompatibilityInputStream(
                new ByteArrayInputStream(join(serverRequest, completionResponse)),
                compatibility).readAllBytes();

        assertArrayEquals(serverRequest,
                Arrays.copyOfRange(output, 0, serverRequest.length));
        JsonNode transformed = JSON.readTree(decode(
                Arrays.copyOfRange(output, serverRequest.length, output.length)).body());
        assertFalse(transformed.path("result").path("items").get(0).has("textEdit"));
        assertFalse(compatibility.hasPendingRequests());
    }

    @Test
    void rejectsTruncatedBody() {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        byte[] truncated = "Content-Length: 8\r\n\r\n{}"
                .getBytes(StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () ->
                new DartLspCompletionCompatibilityInputStream(
                        new ByteArrayInputStream(truncated), compatibility).readAllBytes());
    }

    @Test
    void leavesMalformedResolveMarkerUnchanged() throws Exception {
        DartLspCompletionCompatibility compatibility = new DartLspCompletionCompatibility();
        byte[] request = frame("""
                {"jsonrpc":"2.0","id":4,"method":"completionItem/resolve","params":{
                  "label":"A","data":{"__netbeans30CompletionBridge":{
                    "version":"1","data":"not-base64","textEdit":"also-invalid"
                  }}
                }}
                """.trim());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(sink, compatibility)) {
            output.write(request);
        }

        assertArrayEquals(request, sink.toByteArray());
    }

    private static void recordRequest(
            DartLspCompletionCompatibility compatibility,
            String id) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (DartLspCapabilitiesOutputStream output =
                new DartLspCapabilitiesOutputStream(sink, compatibility)) {
            output.write(frame("{\"jsonrpc\":\"2.0\",\"id\":" + id
                    + ",\"method\":\"textDocument/completion\",\"params\":{}}"));
        }
    }

    private static byte[] frame(String body) {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        return join(("Content-Length: " + content.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII), content);
    }

    private static Frame decode(byte[] frame) {
        int bodyStart = findHeaderEnd(frame);
        assertTrue(bodyStart >= 0, "missing LSP header terminator");
        String header = new String(frame, 0, bodyStart - 4, StandardCharsets.US_ASCII);
        int length = Arrays.stream(header.split("\\r\\n"))
                .filter(line -> line.regionMatches(true, 0,
                        "Content-Length:", 0, "Content-Length:".length()))
                .map(line -> line.substring(line.indexOf(':') + 1).trim())
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElseThrow();
        assertEquals(frame.length, bodyStart + length);
        return new Frame(Arrays.copyOfRange(frame, bodyStart, frame.length), length);
    }

    private static byte[] responseBody(byte[] frame) {
        return decode(frame).body();
    }

    private static int findHeaderEnd(byte[] bytes) {
        for (int index = 0; index <= bytes.length - 4; index++) {
            if (bytes[index] == '\r' && bytes[index + 1] == '\n'
                    && bytes[index + 2] == '\r' && bytes[index + 3] == '\n') {
                return index + 4;
            }
        }
        return -1;
    }

    private static byte[] join(byte[]... chunks) {
        int length = Arrays.stream(chunks).mapToInt(chunk -> chunk.length).sum();
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    private record Frame(byte[] body, int contentLength) {
    }
}

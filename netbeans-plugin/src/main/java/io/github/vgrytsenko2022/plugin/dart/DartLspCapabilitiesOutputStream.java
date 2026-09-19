package io.github.vgrytsenko2022.plugin.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Corrects the NetBeans 30 Dart-LSP client capabilities before initialization.
 *
 * <p>NetBeans 30 implements {@code workspace/applyEdit}, but its initializer
 * does not advertise {@code workspace.applyEdit}. Dart consequently suppresses
 * source actions and refuses command-based fixes. The first initialize request
 * is corrected, and completion resolve requests can have the paired NetBeans
 * 30 compatibility envelope restored. All other framed messages remain
 * byte-for-byte intact.</p>
 */
final class DartLspCapabilitiesOutputStream extends FilterOutputStream {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final byte[] HEADER_END = {'\r', '\n', '\r', '\n'};
    private static final byte[] COMPLETION_METHOD =
            "textDocument/completion".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] COMPLETION_RESOLVE_METHOD =
            "completionItem/resolve".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_HEADER_BYTES = 32_768;
    private static final int MAX_CONTENT_BYTES = 64 * 1024 * 1024;

    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final DartLspCompletionCompatibility completionCompatibility;
    private boolean initializeSeen;

    DartLspCapabilitiesOutputStream(OutputStream delegate) {
        this(delegate, new DartLspCompletionCompatibility());
    }

    DartLspCapabilitiesOutputStream(
            OutputStream delegate,
            DartLspCompletionCompatibility completionCompatibility) {
        super(delegate);
        this.completionCompatibility = Objects.requireNonNull(
                completionCompatibility,
                "completionCompatibility");
    }

    @Override
    public synchronized void write(int value) throws IOException {
        pending.write(value);
        drainFrames();
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        pending.write(bytes, offset, length);
        drainFrames();
    }

    @Override
    public synchronized void flush() throws IOException {
        drainFrames();
        out.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        if (pending.size() > 0) {
            pending.writeTo(out);
            pending.reset();
        }
        super.close();
    }

    private void drainFrames() throws IOException {
        byte[] bytes = pending.toByteArray();
        int consumed = 0;
        while (consumed < bytes.length) {
            int headerEnd = findHeaderEnd(bytes, consumed);
            if (headerEnd < 0) {
                if (bytes.length - consumed > MAX_HEADER_BYTES) {
                    throw new IOException("Dart LSP request header exceeds " + MAX_HEADER_BYTES + " bytes");
                }
                break;
            }
            int contentLength = contentLength(bytes, consumed, headerEnd - HEADER_END.length);
            if (contentLength < 0 || contentLength > MAX_CONTENT_BYTES) {
                throw new IOException("Invalid Dart LSP Content-Length: " + contentLength);
            }
            long frameEndLong = (long) headerEnd + contentLength;
            if (frameEndLong > bytes.length) {
                break;
            }
            int frameEnd = (int) frameEndLong;
            writeFrame(bytes, consumed, headerEnd, frameEnd);
            consumed = frameEnd;
        }
        if (consumed > 0) {
            pending.reset();
            pending.write(bytes, consumed, bytes.length - consumed);
        }
    }

    private void writeFrame(
            byte[] bytes,
            int frameStart,
            int bodyStart,
            int frameEnd) throws IOException {
        byte[] body = Arrays.copyOfRange(bytes, bodyStart, frameEnd);
        if (initializeSeen
                && !containsInPrefix(body, COMPLETION_METHOD, 1_024)
                && !containsInPrefix(body, COMPLETION_RESOLVE_METHOD, 1_024)) {
            out.write(bytes, frameStart, frameEnd - frameStart);
            return;
        }

        JsonNode message;
        try {
            message = JSON.readTree(body);
        } catch (IOException ex) {
            out.write(bytes, frameStart, frameEnd - frameStart);
            return;
        }
        completionCompatibility.recordRequest(message);
        if (DartLspCompletionCompatibility.restoreResolveTextEdit(message)) {
            byte[] updatedBody = JSON.writeValueAsBytes(message);
            writeUpdatedHeader(bytes, frameStart, bodyStart - HEADER_END.length,
                    updatedBody.length);
            out.write('\r');
            out.write('\n');
            out.write(updatedBody);
            return;
        }
        if (!(message instanceof ObjectNode request)
                || !"initialize".equals(request.path("method").asText())) {
            out.write(bytes, frameStart, frameEnd - frameStart);
            return;
        }

        initializeSeen = true;
        ObjectNode workspace = request
                .withObject("params")
                .withObject("capabilities")
                .withObject("workspace");
        if (workspace.path("applyEdit").asBoolean(false)) {
            out.write(bytes, frameStart, frameEnd - frameStart);
            return;
        }
        workspace.put("applyEdit", true);
        byte[] updatedBody = JSON.writeValueAsBytes(request);
        writeUpdatedHeader(bytes, frameStart, bodyStart - HEADER_END.length, updatedBody.length);
        out.write('\r');
        out.write('\n');
        out.write(updatedBody);
    }

    private void writeUpdatedHeader(
            byte[] bytes,
            int headerStart,
            int headerEnd,
            int updatedLength) throws IOException {
        String header = new String(
                bytes,
                headerStart,
                headerEnd - headerStart,
                StandardCharsets.US_ASCII);
        boolean lengthWritten = false;
        for (String line : header.split("\\r\\n")) {
            if (line.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                out.write(("Content-Length: " + updatedLength + "\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                lengthWritten = true;
            } else if (!line.isEmpty()) {
                out.write(line.getBytes(StandardCharsets.US_ASCII));
                out.write('\r');
                out.write('\n');
            }
        }
        if (!lengthWritten) {
            throw new IOException("Dart LSP initialize request has no Content-Length header");
        }
    }

    private static int findHeaderEnd(byte[] bytes, int start) {
        for (int index = start; index <= bytes.length - HEADER_END.length; index++) {
            if (bytes[index] == HEADER_END[0]
                    && bytes[index + 1] == HEADER_END[1]
                    && bytes[index + 2] == HEADER_END[2]
                    && bytes[index + 3] == HEADER_END[3]) {
                return index + HEADER_END.length;
            }
        }
        return -1;
    }

    private static int contentLength(byte[] bytes, int start, int end) throws IOException {
        String header = new String(bytes, start, end - start, StandardCharsets.US_ASCII);
        for (String line : header.split("\\r\\n")) {
            int separator = line.indexOf(':');
            if (separator > 0
                    && line.substring(0, separator).equalsIgnoreCase("Content-Length")) {
                try {
                    return Integer.parseInt(line.substring(separator + 1).trim());
                } catch (NumberFormatException ex) {
                    throw new IOException("Invalid Dart LSP Content-Length header", ex);
                }
            }
        }
        return -1;
    }

    private static boolean containsInPrefix(byte[] source, byte[] expected, int prefixLength) {
        int lastOffset = Math.min(source.length, Math.max(0, prefixLength)) - expected.length;
        outer:
        for (int offset = 0; offset <= lastOffset; offset++) {
            for (int index = 0; index < expected.length; index++) {
                if (source[offset + index] != expected[index]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}

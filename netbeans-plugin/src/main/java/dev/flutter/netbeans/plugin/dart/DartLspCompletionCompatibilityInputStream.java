package dev.flutter.netbeans.plugin.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapts Dart server messages for NetBeans 30 compatibility.
 *
 * <p>NetBeans 30 resolves a completion item only when its initial
 * {@code textEdit} is absent. Dart supplies that edit before resolve but adds
 * auto-import edits during resolve, so this stream temporarily hides the
 * initial edit in correlated auto-import items. The paired output stream
 * restores it before Dart receives {@code completionItem/resolve}. Dart also
 * emits the custom {@code $/analyzerStatus} notification, which the generic
 * NetBeans 30 LSP client cannot consume; this stream removes only that
 * notification before it reaches the client.</p>
 */
final class DartLspCompletionCompatibilityInputStream extends InputStream {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final byte[] HEADER_TERMINATOR = {'\r', '\n', '\r', '\n'};
    private static final byte[] ANALYZER_STATUS_METHOD =
            "\"$/analyzerStatus\"".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_HEADER_BYTES = 32_768;
    private static final int MAX_BODY_BYTES = 64 * 1024 * 1024;

    private final InputStream source;
    private final DartLspCompletionCompatibility compatibility;
    private byte[] pending = new byte[0];
    private int pendingOffset;

    DartLspCompletionCompatibilityInputStream(
            InputStream source,
            DartLspCompletionCompatibility compatibility) {
        this.source = Objects.requireNonNull(source, "source");
        this.compatibility = Objects.requireNonNull(compatibility, "compatibility");
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int count = read(one, 0, 1);
        return count < 0 ? -1 : Byte.toUnsignedInt(one[0]);
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, target.length);
        if (length == 0) {
            return 0;
        }
        if (pendingOffset == pending.length && !loadNextFrame()) {
            return -1;
        }
        int count = Math.min(length, pending.length - pendingOffset);
        System.arraycopy(pending, pendingOffset, target, offset, count);
        pendingOffset += count;
        return count;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    private boolean loadNextFrame() throws IOException {
        while (true) {
            byte[] headerBytes = readHeader();
            if (headerBytes == null) {
                return false;
            }
            Header header = parseHeader(headerBytes);
            byte[] body = readBody(header.contentLength());
            byte[] compatibleBody = adaptServerMessage(body);
            if (compatibleBody == null) {
                continue;
            }
            pending = compatibleBody == body
                    ? concatenate(headerBytes, body)
                    : encodeFrame(header.lines(), compatibleBody);
            pendingOffset = 0;
            return true;
        }
    }

    private byte[] adaptServerMessage(byte[] body) {
        boolean completionPending = compatibility.hasPendingRequests();
        if (!completionPending && !contains(body, ANALYZER_STATUS_METHOD)) {
            return body;
        }
        try {
            JsonNode root = JSON.readTree(body);
            if (isAnalyzerStatusNotification(root)) {
                return null;
            }
            if (!completionPending) {
                return body;
            }
            if (!(root instanceof ObjectNode response)
                    || !response.hasNonNull("id")
                    || response.has("method")
                    || response.has("result") == response.has("error")
                    || !compatibility.takeResponse(response.get("id"))) {
                return body;
            }
            JsonNode result = response.get("result");
            JsonNode items = result != null && result.isArray()
                    ? result
                    : result == null ? null : result.get("items");
            if (items == null || !items.isArray()) {
                return body;
            }
            boolean changed = false;
            for (JsonNode item : items) {
                if (item instanceof ObjectNode completionItem) {
                    changed |= DartLspCompletionCompatibility
                            .hideResolvableTextEdit(completionItem);
                }
            }
            return changed ? JSON.writeValueAsBytes(response) : body;
        } catch (IOException ex) {
            // Preserve malformed or unrelated server data exactly; this filter
            // must not turn a protocol problem into a different failure.
            return body;
        }
    }

    private static boolean isAnalyzerStatusNotification(JsonNode message) {
        return message instanceof ObjectNode object
                && !object.has("id")
                && "$/analyzerStatus".equals(object.path("method").asText());
    }

    private byte[] readHeader() throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream(128);
        int terminatorOffset = 0;
        while (true) {
            int next = source.read();
            if (next < 0) {
                if (header.size() == 0) {
                    return null;
                }
                throw new IOException("Unexpected end of Dart LSP stream in message header");
            }
            header.write(next);
            if (header.size() > MAX_HEADER_BYTES) {
                throw new IOException("Dart LSP message header exceeds "
                        + MAX_HEADER_BYTES + " bytes");
            }
            byte nextByte = (byte) next;
            if (nextByte == HEADER_TERMINATOR[terminatorOffset]) {
                terminatorOffset++;
                if (terminatorOffset == HEADER_TERMINATOR.length) {
                    return header.toByteArray();
                }
            } else {
                terminatorOffset = nextByte == HEADER_TERMINATOR[0] ? 1 : 0;
            }
        }
    }

    private static Header parseHeader(byte[] headerBytes) throws IOException {
        String text = new String(
                headerBytes,
                0,
                headerBytes.length - HEADER_TERMINATOR.length,
                StandardCharsets.ISO_8859_1);
        List<String> lines = new ArrayList<>(List.of(text.split("\\r\\n", -1)));
        Integer contentLength = null;
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator < 0
                    || !line.substring(0, separator).trim()
                            .equalsIgnoreCase("Content-Length")) {
                continue;
            }
            if (contentLength != null) {
                throw new IOException("Dart LSP message has multiple Content-Length headers");
            }
            try {
                contentLength = Integer.valueOf(line.substring(separator + 1).trim());
            } catch (NumberFormatException ex) {
                throw new IOException("Dart LSP message has an invalid Content-Length", ex);
            }
        }
        if (contentLength == null) {
            throw new IOException("Dart LSP message is missing Content-Length");
        }
        if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
            throw new IOException("Dart LSP Content-Length is outside the supported range: "
                    + contentLength);
        }
        return new Header(lines, contentLength);
    }

    private byte[] readBody(int contentLength) throws IOException {
        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < body.length) {
            int count = source.read(body, offset, body.length - offset);
            if (count < 0) {
                throw new IOException("Unexpected end of Dart LSP stream in message body");
            }
            if (count == 0) {
                int next = source.read();
                if (next < 0) {
                    throw new IOException("Unexpected end of Dart LSP stream in message body");
                }
                body[offset++] = (byte) next;
            } else {
                offset += count;
            }
        }
        return body;
    }

    private static byte[] encodeFrame(List<String> headerLines, byte[] body)
            throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream(body.length + 128);
        for (String line : headerLines) {
            int separator = line.indexOf(':');
            if (separator >= 0
                    && line.substring(0, separator).trim()
                            .equalsIgnoreCase("Content-Length")) {
                frame.write(("Content-Length: " + body.length)
                        .getBytes(StandardCharsets.ISO_8859_1));
            } else {
                frame.write(line.getBytes(StandardCharsets.ISO_8859_1));
            }
            frame.write('\r');
            frame.write('\n');
        }
        frame.write('\r');
        frame.write('\n');
        frame.write(body);
        return frame.toByteArray();
    }

    private static byte[] concatenate(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static boolean contains(byte[] source, byte[] expected) {
        int lastOffset = source.length - expected.length;
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

    private record Header(List<String> lines, int contentLength) {
    }
}

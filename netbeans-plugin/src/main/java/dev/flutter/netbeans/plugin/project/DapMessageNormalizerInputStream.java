package dev.flutter.netbeans.plugin.project;

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
import java.util.Set;
import java.util.function.Consumer;

/**
 * Normalizes Flutter DAP messages before they reach NetBeans 30.
 *
 * <p>Flutter may omit {@code body.category} from {@code output} events. NetBeans
 * 30 assumes that field is non-null, so the missing value otherwise causes an
 * exception in its DAP output handler.</p>
 */
final class DapMessageNormalizerInputStream extends InputStream {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final byte[] HEADER_TERMINATOR = {'\r', '\n', '\r', '\n'};
    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int MAX_BODY_BYTES = 64 * 1024 * 1024;
    private static final Set<String> NETBEANS_OUTPUT_CATEGORIES = Set.of(
            "console", "important", "stdout", "stderr", "telemetry");

    private final InputStream source;
    private final Consumer<JsonNode> messageListener;
    private byte[] pending = new byte[0];
    private int pendingOffset;

    DapMessageNormalizerInputStream(InputStream source) {
        this(source, _message -> {
        });
    }

    DapMessageNormalizerInputStream(
            InputStream source,
            Consumer<JsonNode> messageListener) {
        this.source = Objects.requireNonNull(source);
        this.messageListener = Objects.requireNonNull(messageListener);
    }

    @Override
    public int read() throws IOException {
        byte[] singleByte = new byte[1];
        int count = read(singleByte, 0, 1);
        return count < 0 ? -1 : Byte.toUnsignedInt(singleByte[0]);
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
        byte[] headerBytes = readHeader();
        if (headerBytes == null) {
            return false;
        }
        DapHeader header = parseHeader(headerBytes);
        byte[] body = readBody(header.contentLength());
        byte[] normalizedBody = normalizeBody(body);
        pending = normalizedBody == body
                ? concatenate(headerBytes, body)
                : encodeFrame(header.lines(), normalizedBody);
        pendingOffset = 0;
        return true;
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
                throw new IOException("Unexpected end of Flutter DAP stream in message header");
            }
            header.write(next);
            if (header.size() > MAX_HEADER_BYTES) {
                throw new IOException("Flutter DAP message header exceeds "
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

    private static DapHeader parseHeader(byte[] headerBytes) throws IOException {
        String text = new String(
                headerBytes, 0, headerBytes.length - HEADER_TERMINATOR.length,
                StandardCharsets.ISO_8859_1);
        String[] splitLines = text.split("\\r\\n", -1);
        List<String> lines = new ArrayList<>(List.of(splitLines));
        Integer contentLength = null;
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator < 0
                    || !line.substring(0, separator).trim().equalsIgnoreCase("Content-Length")) {
                continue;
            }
            if (contentLength != null) {
                throw new IOException("Flutter DAP message has multiple Content-Length headers");
            }
            try {
                contentLength = Integer.valueOf(line.substring(separator + 1).trim());
            } catch (NumberFormatException ex) {
                throw new IOException("Flutter DAP message has an invalid Content-Length", ex);
            }
        }
        if (contentLength == null) {
            throw new IOException("Flutter DAP message is missing Content-Length");
        }
        if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
            throw new IOException("Flutter DAP Content-Length is outside the supported range: "
                    + contentLength);
        }
        return new DapHeader(lines, contentLength);
    }

    private byte[] readBody(int contentLength) throws IOException {
        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < body.length) {
            int count = source.read(body, offset, body.length - offset);
            if (count < 0) {
                throw new IOException("Unexpected end of Flutter DAP stream in message body");
            }
            if (count == 0) {
                int next = source.read();
                if (next < 0) {
                    throw new IOException("Unexpected end of Flutter DAP stream in message body");
                }
                body[offset++] = (byte) next;
            } else {
                offset += count;
            }
        }
        return body;
    }

    private byte[] normalizeBody(byte[] body) {
        try {
            JsonNode root = JSON.readTree(body);
            byte[] normalized = body;
            if (root instanceof ObjectNode message
                    && "event".equals(message.path("type").asText())
                    && "output".equals(message.path("event").asText())
                    && message.get("body") instanceof ObjectNode outputBody) {
                JsonNode category = outputBody.get("category");
                if (category == null || !category.isTextual()
                        || !NETBEANS_OUTPUT_CATEGORIES.contains(category.textValue())) {
                    outputBody.put("category", "console");
                    normalized = JSON.writeValueAsBytes(message);
                }
            }
            notifyMessage(root);
            return normalized;
        } catch (IOException ex) {
            // Let NetBeans handle a malformed DAP payload; the compatibility
            // filter must never turn an unrelated message into a new failure.
            return body;
        }
    }

    private void notifyMessage(JsonNode message) {
        if (message == null) {
            return;
        }
        try {
            messageListener.accept(message.deepCopy());
        } catch (RuntimeException ex) {
            // Observation is optional; it must never interrupt the DAP stream.
        }
    }

    private static byte[] concatenate(byte[] header, byte[] body) {
        byte[] frame = new byte[header.length + body.length];
        System.arraycopy(header, 0, frame, 0, header.length);
        System.arraycopy(body, 0, frame, header.length, body.length);
        return frame;
    }

    private static byte[] encodeFrame(List<String> headerLines, byte[] body)
            throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream(body.length + 128);
        for (String line : headerLines) {
            int separator = line.indexOf(':');
            if (separator >= 0
                    && line.substring(0, separator).trim().equalsIgnoreCase("Content-Length")) {
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

    private record DapHeader(List<String> lines, int contentLength) {
    }
}

package io.github.vgrytsenko2022.plugin.designer.canvas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Removes the one bounded diagnostic line emitted by a directly launched
 * debug Flutter engine before Dart takes ownership of stdout.
 *
 * <p>This is deliberately not a generic framing resynchronizer. Only one
 * syntactically exact loopback Dart VM service announcement is accepted, and
 * only at byte zero. Every other byte, including the same line after the first
 * protocol byte, is passed unchanged to the fail-stop NBFC reader.</p>
 */
final class CanvasRunnerProtocolInputStream extends InputStream {
    static final int MAX_STARTUP_LINE_BYTES = 512;
    private static final int MIN_AUTH_CODE_CHARS = 8;
    private static final int MAX_AUTH_CODE_CHARS = 256;
    private static final byte[] VM_SERVICE_PREFIX = (
            "The Dart VM service is listening on ")
            .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EMPTY = new byte[0];

    private final InputStream delegate;
    private boolean probed;
    private byte[] replay = EMPTY;
    private int replayOffset;

    CanvasRunnerProtocolInputStream(InputStream delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int read() throws IOException {
        probeOnce();
        if (replayOffset < replay.length) {
            return Byte.toUnsignedInt(replay[replayOffset++]);
        }
        return delegate.read();
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, target.length);
        if (length == 0) {
            return 0;
        }
        probeOnce();
        if (replayOffset < replay.length) {
            int copied = Math.min(length, replay.length - replayOffset);
            System.arraycopy(replay, replayOffset, target, offset, copied);
            replayOffset += copied;
            return copied;
        }
        return delegate.read(target, offset, length);
    }

    @Override
    public int available() throws IOException {
        return replay.length - replayOffset + delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private void probeOnce() throws IOException {
        if (probed) {
            return;
        }
        probed = true;
        ByteArrayOutputStream candidate = new ByteArrayOutputStream(
                MAX_STARTUP_LINE_BYTES);
        for (byte expected : VM_SERVICE_PREFIX) {
            int actual = delegate.read();
            if (actual < 0) {
                replay = candidate.toByteArray();
                return;
            }
            candidate.write(actual);
            if (actual != Byte.toUnsignedInt(expected)) {
                replay = candidate.toByteArray();
                return;
            }
        }
        boolean terminated = false;
        while (candidate.size() < MAX_STARTUP_LINE_BYTES) {
            int actual = delegate.read();
            if (actual < 0) {
                break;
            }
            candidate.write(actual);
            if (actual == '\n') {
                terminated = true;
                break;
            }
        }
        byte[] line = candidate.toByteArray();
        if (!terminated || !isPermittedVmServiceLine(line)) {
            replay = line;
        }
    }

    private static boolean isPermittedVmServiceLine(byte[] line) {
        int endpointEnd = line.length - 1;
        if (endpointEnd < VM_SERVICE_PREFIX.length || line[endpointEnd] != '\n') {
            return false;
        }
        if (endpointEnd > VM_SERVICE_PREFIX.length
                && line[endpointEnd - 1] == '\r') {
            endpointEnd--;
        }
        int endpointLength = endpointEnd - VM_SERVICE_PREFIX.length;
        if (endpointLength <= 0) {
            return false;
        }
        for (int index = VM_SERVICE_PREFIX.length; index < endpointEnd; index++) {
            int value = Byte.toUnsignedInt(line[index]);
            if (value < 0x21 || value > 0x7e) {
                return false;
            }
        }
        final URI endpoint;
        try {
            endpoint = new URI(new String(
                    line,
                    VM_SERVICE_PREFIX.length,
                    endpointLength,
                    StandardCharsets.US_ASCII));
        } catch (URISyntaxException failure) {
            return false;
        }
        String host = endpoint.getHost();
        boolean loopback = "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
        return "http".equals(endpoint.getScheme())
                && loopback
                && endpoint.getPort() > 0
                && endpoint.getPort() <= 65_535
                && endpoint.getRawUserInfo() == null
                && endpoint.getRawQuery() == null
                && endpoint.getRawFragment() == null
                && isPermittedAuthCodePath(endpoint.getRawPath());
    }

    private static boolean isPermittedAuthCodePath(String rawPath) {
        if (rawPath == null || rawPath.length() < MIN_AUTH_CODE_CHARS + 2
                || rawPath.length() > MAX_AUTH_CODE_CHARS + 2
                || rawPath.charAt(0) != '/'
                || rawPath.charAt(rawPath.length() - 1) != '/') {
            return false;
        }
        int padding = 0;
        for (int index = 1; index < rawPath.length() - 1; index++) {
            char character = rawPath.charAt(index);
            boolean alphabet = character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '-'
                    || character == '_';
            if (alphabet && padding == 0) {
                continue;
            }
            if (character == '=' && ++padding <= 2) {
                continue;
            }
            return false;
        }
        return true;
    }
}

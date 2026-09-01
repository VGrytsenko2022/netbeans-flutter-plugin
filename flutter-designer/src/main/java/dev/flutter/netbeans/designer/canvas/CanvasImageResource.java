package dev.flutter.netbeans.designer.canvas;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** One immutable, content-addressed, already-bounded compressed image. */
public final class CanvasImageResource implements Comparable<CanvasImageResource> {
    public static final int MAX_ENCODED_BYTES = 16 * 1024 * 1024;
    public static final int MAX_DIMENSION = 16_384;
    public static final long MAX_PIXELS = 67_108_864L;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private final String resourceId;
    private final CanvasImageFormat format;
    private final int pixelWidth;
    private final int pixelHeight;
    private final byte[] encodedBytes;
    private final int hashCode;

    public CanvasImageResource(
            String resourceId,
            CanvasImageFormat format,
            int pixelWidth,
            int pixelHeight,
            byte[] encodedBytes) {
        Objects.requireNonNull(resourceId, "resourceId");
        if (!SHA256.matcher(resourceId).matches()) {
            throw new IllegalArgumentException(
                    "Canvas image resource id must be a lowercase SHA-256 digest");
        }
        this.format = Objects.requireNonNull(format, "format");
        if (pixelWidth <= 0 || pixelWidth > MAX_DIMENSION
                || pixelHeight <= 0 || pixelHeight > MAX_DIMENSION
                || (long) pixelWidth * pixelHeight > MAX_PIXELS) {
            throw new IllegalArgumentException(
                    "Canvas image dimensions exceed the reviewed decode budget");
        }
        Objects.requireNonNull(encodedBytes, "encodedBytes");
        if (encodedBytes.length == 0 || encodedBytes.length > MAX_ENCODED_BYTES) {
            throw new IllegalArgumentException(
                    "Canvas encoded image exceeds the reviewed byte budget");
        }
        byte[] owned = encodedBytes.clone();
        String actualDigest = sha256Hex(owned);
        if (!resourceId.equals(actualDigest)) {
            throw new IllegalArgumentException(
                    "Canvas image resource id does not match its encoded bytes");
        }
        this.resourceId = resourceId;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.encodedBytes = owned;
        this.hashCode = Objects.hash(
                resourceId, format, pixelWidth, pixelHeight);
    }

    public static CanvasImageResource create(
            CanvasImageFormat format,
            int pixelWidth,
            int pixelHeight,
            byte[] encodedBytes) {
        Objects.requireNonNull(encodedBytes, "encodedBytes");
        return new CanvasImageResource(
                sha256Hex(encodedBytes), format, pixelWidth, pixelHeight, encodedBytes);
    }

    public String resourceId() {
        return resourceId;
    }

    public CanvasImageFormat format() {
        return format;
    }

    public int pixelWidth() {
        return pixelWidth;
    }

    public int pixelHeight() {
        return pixelHeight;
    }

    public int encodedByteLength() {
        return encodedBytes.length;
    }

    public byte[] copyEncodedBytes() {
        return encodedBytes.clone();
    }

    @Override
    public int compareTo(CanvasImageResource other) {
        Objects.requireNonNull(other, "other");
        return resourceId.compareTo(other.resourceId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof CanvasImageResource resource
                && resourceId.equals(resource.resourceId)
                && format == resource.format
                && pixelWidth == resource.pixelWidth
                && pixelHeight == resource.pixelHeight
                && Arrays.equals(encodedBytes, resource.encodedBytes);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "CanvasImageResource[resourceId=" + resourceId
                + ", format=" + format
                + ", pixelWidth=" + pixelWidth
                + ", pixelHeight=" + pixelHeight
                + ", encodedByteLength=" + encodedBytes.length + ']';
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}

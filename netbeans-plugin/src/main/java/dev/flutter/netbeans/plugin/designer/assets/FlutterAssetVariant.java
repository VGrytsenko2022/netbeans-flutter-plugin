package dev.flutter.netbeans.plugin.designer.assets;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/** One immutable decoded-metadata and encoded-byte asset variant snapshot. */
public final class FlutterAssetVariant {
    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    private final String logicalPath;
    private final double scale;
    private final FlutterImageFormat format;
    private final int width;
    private final int height;
    private final long byteLength;
    private final String sha256;
    private final byte[] bytes;

    FlutterAssetVariant(
            String logicalPath,
            double scale,
            FlutterImageFormat format,
            int width,
            int height,
            String sha256,
            byte[] bytes) {
        this.logicalPath = FlutterAssetId.normalizeLogicalPath(logicalPath);
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("scale must be finite and positive");
        }
        this.scale = scale;
        this.format = Objects.requireNonNull(format, "format");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        Objects.requireNonNull(sha256, "sha256");
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException(
                    "sha256 must be 64 lowercase hexadecimal characters");
        }
        this.sha256 = sha256;
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
        this.byteLength = this.bytes.length;
    }

    public String logicalPath() {
        return logicalPath;
    }

    public double scale() {
        return scale;
    }

    public FlutterImageFormat format() {
        return format;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long byteLength() {
        return byteLength;
    }

    /** Raw lowercase SHA-256, suitable for use directly as a resource id. */
    public String sha256() {
        return sha256;
    }

    /** Returns a defensive copy of the immutable encoded image snapshot. */
    public byte[] bytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof FlutterAssetVariant other
                && logicalPath.equals(other.logicalPath)
                && Double.compare(scale, other.scale) == 0
                && format == other.format
                && width == other.width
                && height == other.height
                && sha256.equals(other.sha256)
                && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                logicalPath, scale, format, width, height, sha256);
        return 31 * result + Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "FlutterAssetVariant[logicalPath=" + logicalPath
                + ", scale=" + scale
                + ", format=" + format
                + ", width=" + width
                + ", height=" + height
                + ", byteLength=" + byteLength
                + ", sha256=" + sha256 + ']';
    }
}

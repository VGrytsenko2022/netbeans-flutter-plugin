package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasRenderProfile;

/** Bounded large-payload and physical-frame limits negotiated at hello time. */
public record CanvasWireHandshakeLimits(
        int maxControlMessageBytes,
        int maxModelBytes,
        int maxCatalogBytes,
        int maxLayoutBytes,
        int maxEncodedImageBytes,
        int maxPhysicalDimension,
        long maxPhysicalPixels) {

    public static final int MAX_CONTROL_MESSAGE_BYTES = 256 * 1024;
    public static final int MAX_MODEL_BYTES = 16 * 1024 * 1024;
    public static final int MAX_CATALOG_BYTES = 4 * 1024 * 1024;
    public static final int MAX_LAYOUT_BYTES = 8 * 1024 * 1024;
    public static final int MAX_ENCODED_IMAGE_BYTES = 16 * 1024 * 1024;
    public static final int MAX_PHYSICAL_DIMENSION =
            CanvasRenderProfile.MAX_PHYSICAL_DIMENSION;
    public static final long MAX_PHYSICAL_PIXELS =
            CanvasRenderProfile.MAX_PHYSICAL_PIXELS;

    public CanvasWireHandshakeLimits {
        bounded(maxControlMessageBytes, MAX_CONTROL_MESSAGE_BYTES,
                "maxControlMessageBytes");
        bounded(maxModelBytes, MAX_MODEL_BYTES, "maxModelBytes");
        bounded(maxCatalogBytes, MAX_CATALOG_BYTES, "maxCatalogBytes");
        bounded(maxLayoutBytes, MAX_LAYOUT_BYTES, "maxLayoutBytes");
        bounded(maxEncodedImageBytes, MAX_ENCODED_IMAGE_BYTES,
                "maxEncodedImageBytes");
        bounded(maxPhysicalDimension, MAX_PHYSICAL_DIMENSION,
                "maxPhysicalDimension");
        bounded(maxPhysicalPixels, MAX_PHYSICAL_PIXELS,
                "maxPhysicalPixels");
    }

    public static CanvasWireHandshakeLimits defaults() {
        return new CanvasWireHandshakeLimits(
                MAX_CONTROL_MESSAGE_BYTES,
                MAX_MODEL_BYTES,
                MAX_CATALOG_BYTES,
                MAX_LAYOUT_BYTES,
                MAX_ENCODED_IMAGE_BYTES,
                MAX_PHYSICAL_DIMENSION,
                MAX_PHYSICAL_PIXELS);
    }

    private static void bounded(int value, int maximum, String name) {
        if (value <= 0 || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between 1 and " + maximum);
        }
    }

    private static void bounded(long value, long maximum, String name) {
        if (value <= 0 || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between 1 and " + maximum);
        }
    }
}

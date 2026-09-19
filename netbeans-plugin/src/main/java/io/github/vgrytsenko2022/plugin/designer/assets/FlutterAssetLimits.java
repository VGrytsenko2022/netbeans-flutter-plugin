package io.github.vgrytsenko2022.plugin.designer.assets;

/** Explicit resource limits for one Flutter asset inventory operation. */
public record FlutterAssetLimits(
        int maxAssets,
        long maxFileBytes,
        long maxTotalBytes,
        int maxDimension,
        long maxPixels) {

    public static final FlutterAssetLimits DEFAULT = new FlutterAssetLimits(
            4_096,
            16L * 1024L * 1024L,
            64L * 1024L * 1024L,
            4_096,
            8_388_608L);

    public FlutterAssetLimits {
        positive(maxAssets, "maxAssets");
        positive(maxFileBytes, "maxFileBytes");
        positive(maxTotalBytes, "maxTotalBytes");
        positive(maxDimension, "maxDimension");
        positive(maxPixels, "maxPixels");
        if (maxFileBytes > maxTotalBytes) {
            throw new IllegalArgumentException(
                    "maxFileBytes must not exceed maxTotalBytes");
        }
        if (maxFileBytes > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException(
                    "maxFileBytes exceeds the largest defensive byte snapshot");
        }
    }

    private static void positive(long value, String name) {
        if (value <= 0L) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

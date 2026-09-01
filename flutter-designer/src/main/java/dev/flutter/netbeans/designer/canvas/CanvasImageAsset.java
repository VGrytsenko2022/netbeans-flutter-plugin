package dev.flutter.netbeans.designer.canvas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** One logical Flutter asset and its ordered resolution variants. */
public record CanvasImageAsset(
        CanvasImageAssetId assetId,
        String exactResourceId,
        List<CanvasImageVariant> variants) implements Comparable<CanvasImageAsset> {

    public CanvasImageAsset {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(exactResourceId, "exactResourceId");
        if (!exactResourceId.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Canvas exact image resource must be a lowercase SHA-256 digest");
        }
        Objects.requireNonNull(variants, "variants");
        if (variants.isEmpty() || variants.size() > 32) {
            throw new IllegalArgumentException(
                    "Canvas image asset must contain between 1 and 32 variants");
        }
        ArrayList<CanvasImageVariant> ordered = new ArrayList<>(variants);
        ordered.sort(null);
        Set<java.math.BigDecimal> scales = new HashSet<>();
        for (CanvasImageVariant variant : ordered) {
            Objects.requireNonNull(variant, "variant");
            if (!scales.add(variant.scale())) {
                throw new IllegalArgumentException(
                        "Canvas image asset must not contain duplicate variant scales");
            }
        }
        variants = List.copyOf(ordered);
    }

    @Override
    public int compareTo(CanvasImageAsset other) {
        Objects.requireNonNull(other, "other");
        return assetId.compareTo(other.assetId);
    }

    /**
     * Selects the same resolution variant as pinned Flutter 3.44.8 AssetImage.
     */
    public CanvasImageVariant selectVariant(double devicePixelRatio) {
        if (!Double.isFinite(devicePixelRatio) || devicePixelRatio <= 0) {
            throw new IllegalArgumentException(
                    "Device-pixel ratio must be finite and greater than zero");
        }
        CanvasImageVariant lowest = variants.get(0);
        CanvasImageVariant highest = variants.get(variants.size() - 1);
        if (devicePixelRatio <= lowest.scale().doubleValue()) {
            return lowest;
        }
        if (devicePixelRatio >= highest.scale().doubleValue()) {
            return highest;
        }
        CanvasImageVariant lower = lowest;
        for (int index = 1; index < variants.size(); index++) {
            CanvasImageVariant upper = variants.get(index);
            double upperScale = upper.scale().doubleValue();
            if (devicePixelRatio == upperScale) {
                return upper;
            }
            if (devicePixelRatio < upperScale) {
                double lowerScale = lower.scale().doubleValue();
                return devicePixelRatio < 2.0d
                        || devicePixelRatio > (lowerScale + upperScale) / 2.0d
                        ? upper
                        : lower;
            }
            lower = upper;
        }
        return highest;
    }
}

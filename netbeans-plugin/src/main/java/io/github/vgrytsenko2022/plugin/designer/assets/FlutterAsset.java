package io.github.vgrytsenko2022.plugin.designer.assets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** A logical Flutter asset and its resolution variants. */
public final class FlutterAsset {
    private static final double LOW_DPR_LIMIT = 2.0;
    private static final Comparator<FlutterAssetVariant> VARIANT_ORDER =
            Comparator.comparingDouble(FlutterAssetVariant::scale)
                    .thenComparing(FlutterAssetVariant::logicalPath);

    private final FlutterAssetId id;
    private final List<FlutterAssetVariant> variants;

    FlutterAsset(FlutterAssetId id, List<FlutterAssetVariant> variants) {
        this.id = Objects.requireNonNull(id, "id");
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("variants must not be empty");
        }
        ArrayList<FlutterAssetVariant> sorted = new ArrayList<>(variants);
        sorted.sort(VARIANT_ORDER);
        this.variants = List.copyOf(sorted);
    }

    public FlutterAssetId id() {
        return id;
    }

    public List<FlutterAssetVariant> variants() {
        return variants;
    }

    /**
     * Implements Flutter 3.44.8 {@code AssetImage._findBestVariant} exactly.
     */
    public FlutterAssetVariant selectVariant(double devicePixelRatio) {
        if (!Double.isFinite(devicePixelRatio) || devicePixelRatio <= 0.0) {
            throw new IllegalArgumentException(
                    "devicePixelRatio must be finite and positive");
        }

        NavigableMap<Double, FlutterAssetVariant> candidates = new TreeMap<>();
        for (FlutterAssetVariant variant : variants) {
            candidates.put(variant.scale(), variant);
        }
        FlutterAssetVariant exact = candidates.get(devicePixelRatio);
        if (exact != null) {
            return exact;
        }

        var lower = candidates.lowerEntry(devicePixelRatio);
        var upper = candidates.higherEntry(devicePixelRatio);
        if (lower == null) {
            return Objects.requireNonNull(upper).getValue();
        }
        if (upper == null) {
            return lower.getValue();
        }
        if (devicePixelRatio < LOW_DPR_LIMIT
                || devicePixelRatio > (lower.getKey() + upper.getKey()) / 2.0) {
            return upper.getValue();
        }
        return lower.getValue();
    }
}

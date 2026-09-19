package io.github.vgrytsenko2022.designer.canvas;

import java.math.BigDecimal;
import java.util.Objects;

/** One Flutter resolution variant mapped to a content-addressed resource. */
public record CanvasImageVariant(
        BigDecimal scale,
        String resourceId) implements Comparable<CanvasImageVariant> {
    private static final BigDecimal MAX_SCALE = new BigDecimal("100");

    public CanvasImageVariant {
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(resourceId, "resourceId");
        scale = scale.stripTrailingZeros();
        if (scale.signum() <= 0 || scale.compareTo(MAX_SCALE) > 0) {
            throw new IllegalArgumentException(
                    "Canvas image variant scale must be greater than zero and at most 100");
        }
        if (!resourceId.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Canvas image variant resource must be a lowercase SHA-256 digest");
        }
    }

    @Override
    public int compareTo(CanvasImageVariant other) {
        Objects.requireNonNull(other, "other");
        int byScale = scale.compareTo(other.scale);
        return byScale != 0 ? byScale : resourceId.compareTo(other.resourceId);
    }
}

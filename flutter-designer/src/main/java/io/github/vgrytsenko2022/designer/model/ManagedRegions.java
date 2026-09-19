package io.github.vgrytsenko2022.designer.model;

import java.util.Objects;

/** The complete managed-region set supported by schema version 1. */
public record ManagedRegions(ManagedRegion imports, ManagedRegion build) {
    public ManagedRegions {
        Objects.requireNonNull(imports, "imports");
        Objects.requireNonNull(build, "build");
    }
}

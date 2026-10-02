package io.github.vgrytsenko2022.designer.generation;

/** Managed Dart payload identifiers emitted by generation profile version 1. */
public enum DartManagedRegionId {
    IMPORTS("imports"),
    BUILD("build");

    private final String wireName;

    DartManagedRegionId(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

package io.github.vgrytsenko2022.designer.model;

/** Structural cardinality persisted for a named widget slot. */
public enum SlotCardinality {
    SINGLE("single"),
    LIST("list");

    private final String wireName;

    SlotCardinality(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

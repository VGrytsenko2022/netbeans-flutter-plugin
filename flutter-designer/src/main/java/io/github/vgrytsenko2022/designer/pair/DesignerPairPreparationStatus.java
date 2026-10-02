package io.github.vgrytsenko2022.designer.pair;

/** Aggregate outcome of preparing one immutable two-file write candidate. */
public enum DesignerPairPreparationStatus {
    READY,
    NO_TRANSITION,
    CONFLICT,
    UNSUPPORTED,
    UNAVAILABLE
}

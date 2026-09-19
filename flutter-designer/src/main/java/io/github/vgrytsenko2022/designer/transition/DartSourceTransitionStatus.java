package io.github.vgrytsenko2022.designer.transition;

/** Aggregate outcome of preparing a prospective managed-Dart transition. */
public enum DartSourceTransitionStatus {
    READY,
    NO_CHANGES,
    CONFLICT,
    UNSUPPORTED,
    UNAVAILABLE
}

package dev.flutter.netbeans.designer.transition;

/** Aggregate outcome of preparing a prospective managed-Dart transition. */
public enum DartSourceTransitionStatus {
    READY,
    NO_CHANGES,
    CONFLICT,
    UNSUPPORTED,
    UNAVAILABLE
}

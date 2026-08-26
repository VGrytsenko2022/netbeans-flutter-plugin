package dev.flutter.netbeans.dart;

/** Aggregate result of validating one exact in-memory Dart candidate. */
public enum DartCandidateAnalysisStatus {
    PASSED,
    REJECTED,
    STALE,
    UNAVAILABLE,
    TIMEOUT
}

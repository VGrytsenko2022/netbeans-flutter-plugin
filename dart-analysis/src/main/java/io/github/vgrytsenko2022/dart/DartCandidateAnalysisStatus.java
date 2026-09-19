package io.github.vgrytsenko2022.dart;

/** Aggregate result of validating one exact in-memory Dart candidate. */
public enum DartCandidateAnalysisStatus {
    PASSED,
    REJECTED,
    STALE,
    UNAVAILABLE,
    TIMEOUT
}

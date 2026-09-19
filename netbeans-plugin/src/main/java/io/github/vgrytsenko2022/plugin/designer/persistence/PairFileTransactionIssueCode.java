package io.github.vgrytsenko2022.plugin.designer.persistence;

/** Machine-readable diagnostics produced by {@link PairFileTransaction}. */
public enum PairFileTransactionIssueCode {
    INVALID_FILE,
    NON_LOCAL_FILE,
    SAME_FILE,
    FILE_VALIDATION_FAILED,
    LOCK_ACQUISITION_FAILED,
    BASELINE_READ_FAILED,
    DART_BASELINE_MISMATCH,
    DESIGNER_BASELINE_MISMATCH,
    DART_WRITE_FAILED,
    DESIGNER_WRITE_FAILED,
    POST_WRITE_READ_FAILED,
    POST_WRITE_MISMATCH,
    ATOMIC_ACTION_FAILED,
    ROLLBACK_WRITE_FAILED,
    ROLLBACK_READ_FAILED,
    ROLLBACK_MISMATCH
}

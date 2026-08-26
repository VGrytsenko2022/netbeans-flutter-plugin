package dev.flutter.netbeans.plugin.designer.persistence;

import java.util.List;
import java.util.Objects;

/** Immutable result of one pair-file transaction attempt. */
public record PairFileTransactionResult(
        PairFileTransactionStatus status,
        List<PairFileTransactionIssue> issues,
        int forwardWriteAttempts,
        boolean rollbackAttempted) {

    public PairFileTransactionResult {
        Objects.requireNonNull(status, "status");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
        if (forwardWriteAttempts < 0 || forwardWriteAttempts > 2) {
            throw new IllegalArgumentException(
                    "forwardWriteAttempts must be between zero and two");
        }
        if ((status == PairFileTransactionStatus.COMMITTED
                || status == PairFileTransactionStatus.UNCHANGED)
                && !issues.isEmpty()) {
            throw new IllegalArgumentException(
                    "successful transaction results must not contain issues");
        }
        if (status == PairFileTransactionStatus.ROLLED_BACK
                || status == PairFileTransactionStatus.RECOVERY_CONFLICT) {
            if (!rollbackAttempted || forwardWriteAttempts == 0) {
                throw new IllegalArgumentException(
                        "recovery results require a write and rollback attempt");
            }
        }
    }

    public boolean committed() {
        return status == PairFileTransactionStatus.COMMITTED
                || status == PairFileTransactionStatus.UNCHANGED;
    }
}

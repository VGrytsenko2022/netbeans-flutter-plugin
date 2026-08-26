package dev.flutter.netbeans.plugin.designer;

import java.util.Objects;
import java.util.Optional;

/** Immutable presentation-safe snapshot of the pair-save lifecycle. */
record PairSaveCoordinatorSnapshot(
        PairSaveCoordinatorStatus status,
        long epoch,
        Optional<String> reason) {

    PairSaveCoordinatorSnapshot {
        Objects.requireNonNull(status, "status");
        if (epoch < 0) {
            throw new IllegalArgumentException("epoch must not be negative");
        }
        reason = Objects.requireNonNull(reason, "reason")
                .map(value -> {
                    String normalized = value.strip();
                    if (normalized.isEmpty()) {
                        throw new IllegalArgumentException("reason must not be blank");
                    }
                    return normalized;
                });
        boolean requiresReason = status == PairSaveCoordinatorStatus.EXTERNAL_CONFLICT
                || status == PairSaveCoordinatorStatus.SAVE_FAILED
                || status == PairSaveCoordinatorStatus.RECOVERY_CONFLICT;
        if (requiresReason != reason.isPresent()) {
            throw new IllegalArgumentException(
                    status + (requiresReason
                            ? " requires a concrete reason"
                            : " cannot contain a failure reason"));
        }
    }
}

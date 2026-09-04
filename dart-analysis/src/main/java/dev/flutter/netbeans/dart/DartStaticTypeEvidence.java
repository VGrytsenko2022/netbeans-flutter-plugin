package dev.flutter.netbeans.dart;

import java.util.Objects;
import java.util.Optional;

/** Analyzer semantic evidence for one exact non-null static-type probe. */
public record DartStaticTypeEvidence(
        DartStaticTypeProbe probe,
        boolean accepted,
        Optional<String> rejectionReason) {

    public DartStaticTypeEvidence {
        Objects.requireNonNull(probe, "probe");
        rejectionReason = Objects.requireNonNull(
                rejectionReason, "rejectionReason")
                .map(reason -> {
                    if (reason.isBlank()) {
                        throw new IllegalArgumentException(
                                "rejectionReason must not be blank");
                    }
                    return reason.strip();
                });
        if (accepted != rejectionReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "accepted static-type evidence must have no rejection reason "
                    + "and rejected evidence must have one");
        }
    }
}

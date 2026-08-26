package dev.flutter.netbeans.dart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Analyzer navigation evidence for one exact requested symbol occurrence.
 * Accepted evidence means one covering navigation region resolved to exactly
 * one real target below the probe's expected root with an optional matching
 * kind; it does not by itself prove the probe's expected library URI.
 */
public record DartSymbolEvidence(
        DartSymbolProbe probe,
        List<DartNavigationTarget> targets,
        boolean accepted,
        Optional<String> rejectionReason) {

    public DartSymbolEvidence {
        Objects.requireNonNull(probe, "probe");
        targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        if (targets.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("targets contains null");
        }
        rejectionReason = Objects.requireNonNull(rejectionReason, "rejectionReason")
                .map(reason -> {
                    if (reason.isBlank()) {
                        throw new IllegalArgumentException("rejectionReason must not be blank");
                    }
                    return reason.strip();
                });
        if (accepted != rejectionReason.isEmpty()) {
            throw new IllegalArgumentException(
                    "accepted evidence must have no rejection reason and rejected evidence must have one");
        }
    }
}

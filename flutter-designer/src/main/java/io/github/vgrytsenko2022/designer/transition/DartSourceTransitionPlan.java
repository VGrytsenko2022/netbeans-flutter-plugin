package io.github.vgrytsenko2022.designer.transition;

import io.github.vgrytsenko2022.designer.generation.DartGenerationResult;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityResult;
import io.github.vgrytsenko2022.designer.source.OriginalDartBytes;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable prospective source transition. This is a candidate, not write
 * authorization: a live-document revision, analyzer evidence and lock-time
 * exact-baseline checks are still mandatory.
 */
public final class DartSourceTransitionPlan {
    private final DartThreeWayIntegrityResult baseline;
    private final DartSourceIntegrityResult liveSource;
    private final DartGenerationResult generation;
    private final DartSourceDescriptor prospectiveDescriptor;
    private final OriginalDartBytes candidate;
    private final DartSourceIntegrityResult candidateIntegrity;
    private final DartUserSourceProjection userSourceProjection;
    private final byte[] userSourceBytes;
    private final DartSourceDescriptor baselineDescriptor;

    DartSourceTransitionPlan(
            DartThreeWayIntegrityResult baseline,
            DartSourceIntegrityResult liveSource,
            DartGenerationResult generation,
            DartSourceDescriptor prospectiveDescriptor,
            OriginalDartBytes candidate,
            DartSourceIntegrityResult candidateIntegrity,
            DartUserSourceProjection userSourceProjection,
            byte[] userSourceBytes,
            DartSourceDescriptor baselineDescriptor) {
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.liveSource = Objects.requireNonNull(liveSource, "liveSource");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.prospectiveDescriptor = Objects.requireNonNull(
                prospectiveDescriptor, "prospectiveDescriptor");
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.candidateIntegrity = Objects.requireNonNull(
                candidateIntegrity, "candidateIntegrity");
        this.userSourceProjection = Objects.requireNonNull(
                userSourceProjection, "userSourceProjection");
        this.userSourceBytes = Objects.requireNonNull(userSourceBytes, "userSourceBytes").clone();
        this.baselineDescriptor = Objects.requireNonNull(baselineDescriptor, "baselineDescriptor");

        if (!baseline.onDiskThreeWayMatch()) {
            throw new IllegalArgumentException("baseline must be an on-disk three-way match");
        }
        if (!liveSource.onDiskDeclaredMatch()) {
            throw new IllegalArgumentException("live source must match the baseline descriptor");
        }
        if (!Arrays.equals(userSourceProjection.applyTo(
                liveSource.original().orElseThrow().copyBytes(), baselineDescriptor), this.userSourceBytes)
                || !userSourceProjection.targetMatches(candidate.copyBytes(), prospectiveDescriptor)) {
            throw new IllegalArgumentException(
                    "User-source proof must describe the exact live-to-candidate envelope.");
        }
        if (!generation.successful()) {
            throw new IllegalArgumentException("prospective generation must be successful");
        }
        if (!candidateIntegrity.onDiskDeclaredMatch()
                || candidateIntegrity.original().isEmpty()
                || !candidateIntegrity.original().orElseThrow()
                        .contentEquals(candidate.copyBytes())) {
            throw new IllegalArgumentException(
                    "candidate must retain a matching source-integrity proof");
        }
        GeneratedDartRegions generated = generation.generated().orElseThrow();
        if (!prospectiveDescriptor.managedRegions().imports().sha256()
                        .equals(generated.imports().normalizedSha256())
                || !prospectiveDescriptor.managedRegions().build().sha256()
                        .equals(generated.build().normalizedSha256())) {
            throw new IllegalArgumentException(
                    "prospective descriptor must contain both generated hashes");
        }
        if (liveSource.original().orElseThrow().contentEquals(candidate.copyBytes())) {
            throw new IllegalArgumentException("a transition plan must change source bytes");
        }
    }

    public DartThreeWayIntegrityResult baseline() {
        return baseline;
    }

    public DartSourceIntegrityResult liveSource() {
        return liveSource;
    }

    public DartGenerationResult generation() {
        return generation;
    }

    public DartSourceDescriptor prospectiveDescriptor() {
        return prospectiveDescriptor;
    }

    public OriginalDartBytes candidate() {
        return candidate;
    }

    public DartSourceIntegrityResult candidateIntegrity() {
        return candidateIntegrity;
    }

    public byte[] candidateBytes() {
        return candidate.copyBytes();
    }

    /** Exact live-relative proof; applying it never replaces generated payloads. */
    public DartUserSourceProjection userSourceProjection() {
        return userSourceProjection;
    }

    /** Projected user edits with the original live managed payloads, clone-safe. */
    public byte[] userSourceBytes() {
        return userSourceBytes.clone();
    }

    public DartSourceDescriptor baselineDescriptor() {
        return baselineDescriptor;
    }
}

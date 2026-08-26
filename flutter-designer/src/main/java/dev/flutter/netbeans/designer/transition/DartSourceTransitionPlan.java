package dev.flutter.netbeans.designer.transition;

import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.source.OriginalDartBytes;
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

    DartSourceTransitionPlan(
            DartThreeWayIntegrityResult baseline,
            DartSourceIntegrityResult liveSource,
            DartGenerationResult generation,
            DartSourceDescriptor prospectiveDescriptor,
            OriginalDartBytes candidate,
            DartSourceIntegrityResult candidateIntegrity) {
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.liveSource = Objects.requireNonNull(liveSource, "liveSource");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.prospectiveDescriptor = Objects.requireNonNull(
                prospectiveDescriptor, "prospectiveDescriptor");
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.candidateIntegrity = Objects.requireNonNull(
                candidateIntegrity, "candidateIntegrity");

        if (!baseline.onDiskThreeWayMatch()) {
            throw new IllegalArgumentException("baseline must be an on-disk three-way match");
        }
        if (!liveSource.onDiskDeclaredMatch()) {
            throw new IllegalArgumentException("live source must match the baseline descriptor");
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
}

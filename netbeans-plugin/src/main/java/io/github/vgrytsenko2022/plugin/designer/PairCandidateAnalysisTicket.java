package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.api.DartCandidateCapacityBudget;
import io.github.vgrytsenko2022.dart.DartCandidateAnalysisRequest;
import io.github.vgrytsenko2022.dart.DartCandidateAnalysisResult;
import io.github.vgrytsenko2022.designer.pair.PreparedDesignerPair;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-use capability for analyzing one exact prospective Dart candidate.
 *
 * <p>The payload is immutable and retains the exact loaded/prepared identities
 * and analyzer request. The only mutable fact is an atomic lifecycle edge from
 * {@code ISSUED} to either {@code ACCEPTED} or {@code CANCELLED}. A distinct
 * ticket always receives a distinct overlay version, so an analyzer result
 * cannot be replayed into another preparation even when its candidate bytes
 * happen to be equal.</p>
 */
final class PairCandidateAnalysisTicket {
    private static final AtomicLong NEXT_OVERLAY_VERSION = new AtomicLong();

    private final FlutterDesignerDocumentState.Current currentIdentity;
    private final PreparedDesignerPair preparedIdentity;
    private final DartCandidateAnalysisRequest requestIdentity;
    private final DartCandidateCapacityBudget capacityBudgetIdentity;
    private final Path trustedFlutterSdkRealRoot;
    private final Path realProjectRoot;
    private final Path realDartPath;
    private final AtomicReference<State> state = new AtomicReference<>(State.ISSUED);

    PairCandidateAnalysisTicket(
            FlutterDesignerDocumentState.Current currentIdentity,
            PreparedDesignerPair preparedIdentity,
            DartCandidateAnalysisRequest requestIdentity,
            Path trustedFlutterSdkRealRoot,
            Path realProjectRoot,
            Path realDartPath) {
        this.currentIdentity = Objects.requireNonNull(
                currentIdentity, "currentIdentity");
        this.preparedIdentity = Objects.requireNonNull(
                preparedIdentity, "preparedIdentity");
        this.requestIdentity = Objects.requireNonNull(
                requestIdentity, "requestIdentity");
        this.capacityBudgetIdentity = preparedIdentity.dartTransition()
                .generation().generated().orElseThrow()
                .candidateCapacityBudget();
        if (requestIdentity.candidateCapacityBudget()
                != capacityBudgetIdentity) {
            throw new IllegalArgumentException(
                    "analysis request lost the prepared candidate capacity identity");
        }
        if (requestIdentity.snapshot().utf8Size()
                > capacityBudgetIdentity.maxCandidateUtf8Bytes()
                || requestIdentity.symbolProbes().size()
                > capacityBudgetIdentity.maxSymbolProbes()) {
            throw new IllegalArgumentException(
                    "analysis request exceeds the prepared candidate capacity");
        }
        this.trustedFlutterSdkRealRoot = Objects.requireNonNull(
                trustedFlutterSdkRealRoot, "trustedFlutterSdkRealRoot");
        this.realProjectRoot = Objects.requireNonNull(
                realProjectRoot, "realProjectRoot");
        this.realDartPath = Objects.requireNonNull(realDartPath, "realDartPath");
    }

    static long nextOverlayVersion() {
        while (true) {
            long current = NEXT_OVERLAY_VERSION.get();
            if (current == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "The Flutter Designer analyzer overlay version space is exhausted");
            }
            if (NEXT_OVERLAY_VERSION.compareAndSet(current, current + 1)) {
                return current;
            }
        }
    }

    DartCandidateAnalysisRequest request() {
        return requestIdentity;
    }

    DartCandidateCapacityBudget capacityBudgetIdentity() {
        return capacityBudgetIdentity;
    }

    PairAnalyzedCandidateResult accept(DartCandidateAnalysisResult result) {
        Objects.requireNonNull(result, "result");
        if (!state.compareAndSet(State.ISSUED, State.CONSUMED)) {
            return alreadyConsumed();
        }
        return PairSaveEvidenceGate.evaluateAnalysis(this, result);
    }

    boolean cancel() {
        return state.compareAndSet(State.ISSUED, State.CANCELLED);
    }

    boolean consumed() {
        return state.get() != State.ISSUED;
    }

    FlutterDesignerDocumentState.Current currentIdentity() {
        return currentIdentity;
    }

    PreparedDesignerPair preparedIdentity() {
        return preparedIdentity;
    }

    Path trustedFlutterSdkRealRoot() {
        return trustedFlutterSdkRealRoot;
    }

    Path realProjectRoot() {
        return realProjectRoot;
    }

    Path realDartPath() {
        return realDartPath;
    }

    private PairAnalyzedCandidateResult alreadyConsumed() {
        State consumedState = state.get();
        PairSaveEvidenceDiagnostic diagnostic = new PairSaveEvidenceDiagnostic(
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_TICKET_ALREADY_USED,
                "analysisTicket",
                consumedState == State.CANCELLED
                        ? "The exact candidate analysis ticket was cancelled and cannot accept a result."
                        : "The exact candidate analysis ticket already consumed a result and cannot be replayed.");
        return new PairAnalyzedCandidateResult.Rejected(List.of(diagnostic));
    }

    private enum State {
        ISSUED,
        CONSUMED,
        CANCELLED
    }
}

package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable point-in-time analyzer proof for a candidate which has not yet
 * touched the editor.
 *
 * <p>Binding and delayed history replay re-resolve the exact package manifest,
 * probe roots, and navigation target real paths. They therefore fail closed on
 * deletion or path/provenance redirection. They do not claim that mutable
 * transitive Dart dependency contents at the same real paths remain frozen
 * after the analyzer process exits; such external concurrent semantic mutation
 * is outside the atomic Designer document/pair boundary and requires a fresh
 * analyzer admission cycle.</p>
 */
final class PairAnalyzedCandidate {
    private final PairCandidateAnalysisTicket ticketIdentity;
    private final FlutterDesignerDocumentState.Current currentIdentity;
    private final PreparedDesignerPair preparedIdentity;
    private final DartCandidateAnalysisRequest requestIdentity;
    private final DartCandidateAnalysisResult analysisIdentity;
    private final Path trustedFlutterSdkRealRoot;
    private final Path realProjectRoot;
    private final Path realDartPath;

    PairAnalyzedCandidate(
            PairCandidateAnalysisTicket ticketIdentity,
            DartCandidateAnalysisResult analysisIdentity) {
        this.ticketIdentity = Objects.requireNonNull(ticketIdentity, "ticketIdentity");
        this.currentIdentity = ticketIdentity.currentIdentity();
        this.preparedIdentity = ticketIdentity.preparedIdentity();
        this.requestIdentity = ticketIdentity.request();
        this.analysisIdentity = Objects.requireNonNull(
                analysisIdentity, "analysisIdentity");
        this.trustedFlutterSdkRealRoot = ticketIdentity.trustedFlutterSdkRealRoot();
        this.realProjectRoot = ticketIdentity.realProjectRoot();
        this.realDartPath = ticketIdentity.realDartPath();
    }

    boolean retainsTicket(PairCandidateAnalysisTicket ticket) {
        return ticketIdentity == ticket;
    }

    boolean retainsExactInputs(
            PairCandidateAnalysisTicket ticket,
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            DartCandidateAnalysisRequest request,
            DartCandidateAnalysisResult analysis) {
        return ticketIdentity == ticket
                && currentIdentity == current
                && preparedIdentity == prepared
                && requestIdentity == request
                && analysisIdentity == analysis;
    }

    PairCandidateAnalysisTicket ticketIdentity() {
        return ticketIdentity;
    }

    FlutterDesignerDocumentState.Current currentIdentity() {
        return currentIdentity;
    }

    PreparedDesignerPair preparedIdentity() {
        return preparedIdentity;
    }

    DartCandidateAnalysisRequest requestIdentity() {
        return requestIdentity;
    }

    DartCandidateAnalysisResult analysisIdentity() {
        return analysisIdentity;
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
}

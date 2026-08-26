package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import java.nio.file.Path;
import java.util.Objects;

/** Immutable analyzer proof for a candidate which has not yet touched the editor. */
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

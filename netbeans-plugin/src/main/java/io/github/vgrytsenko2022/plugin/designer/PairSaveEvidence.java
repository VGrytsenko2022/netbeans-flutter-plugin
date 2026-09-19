package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.DartCandidateAnalysisResult;
import io.github.vgrytsenko2022.designer.pair.PreparedDesignerPair;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.text.StyledDocument;

/**
 * Immutable binding of every exact input that passed the pair-save evidence
 * gate. This remains staging evidence, not filesystem write authority.
 */
final class PairSaveEvidence {
    private final PairAnalyzedCandidate analyzedCandidateIdentity;
    private final FlutterDesignerDocumentState.Current loadedCurrentIdentity;
    private final PreparedDesignerPair preparedPairIdentity;
    private final LiveDartDocumentSnapshot liveCandidateIdentity;
    private final DartCandidateAnalysisResult analysisIdentity;
    private final Path trustedFlutterSdkRealRoot;
    private final Path realDartPath;
    private final StyledDocument documentIdentity;
    private final long documentVersion;
    private final String candidateSha256;
    private final byte[] baselineFdBytes;
    private final byte[] baselineDartBytes;
    private final byte[] candidateDartBytes;

    PairSaveEvidence(
            PairAnalyzedCandidate analyzedCandidateIdentity,
            LiveDartDocumentSnapshot liveCandidateIdentity) {
        this.analyzedCandidateIdentity = Objects.requireNonNull(
                analyzedCandidateIdentity, "analyzedCandidateIdentity");
        this.loadedCurrentIdentity = analyzedCandidateIdentity.currentIdentity();
        this.preparedPairIdentity = analyzedCandidateIdentity.preparedIdentity();
        this.liveCandidateIdentity = Objects.requireNonNull(
                liveCandidateIdentity, "liveCandidateIdentity");
        this.analysisIdentity = analyzedCandidateIdentity.analysisIdentity();
        this.trustedFlutterSdkRealRoot =
                analyzedCandidateIdentity.trustedFlutterSdkRealRoot();
        this.realDartPath = analyzedCandidateIdentity.realDartPath();
        this.documentIdentity = liveCandidateIdentity.documentIdentity();
        this.documentVersion = liveCandidateIdentity.documentVersion();
        this.candidateSha256 = liveCandidateIdentity.markerBearingSha256();
        this.baselineFdBytes = preparedPairIdentity.baselineFdBytes();
        this.baselineDartBytes = preparedPairIdentity.baselineDartBytes();
        this.candidateDartBytes = preparedPairIdentity.prospectiveDartBytes();
    }

    PairAnalyzedCandidate analyzedCandidateIdentity() {
        return analyzedCandidateIdentity;
    }

    FlutterDesignerDocumentState.Current loadedCurrentIdentity() {
        return loadedCurrentIdentity;
    }

    PreparedDesignerPair preparedPairIdentity() {
        return preparedPairIdentity;
    }

    LiveDartDocumentSnapshot liveCandidateIdentity() {
        return liveCandidateIdentity;
    }

    DartCandidateAnalysisResult analysisIdentity() {
        return analysisIdentity;
    }

    Path trustedFlutterSdkRealRoot() {
        return trustedFlutterSdkRealRoot;
    }

    Path realDartPath() {
        return realDartPath;
    }

    StyledDocument documentIdentity() {
        return documentIdentity;
    }

    long documentVersion() {
        return documentVersion;
    }

    String candidateSha256() {
        return candidateSha256;
    }

    int candidateUtf8Size() {
        return candidateDartBytes.length;
    }

    byte[] baselineFdBytes() {
        return baselineFdBytes.clone();
    }

    byte[] baselineDartBytes() {
        return baselineDartBytes.clone();
    }

    byte[] candidateDartBytes() {
        return candidateDartBytes.clone();
    }

    boolean retainsExactInputs(
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            LiveDartDocumentSnapshot live,
            DartCandidateAnalysisResult analysis) {
        return loadedCurrentIdentity == current
                && preparedPairIdentity == prepared
                && liveCandidateIdentity == live
                && analysisIdentity == analysis;
    }

    boolean retainsExactAnalysis(
            PairCandidateAnalysisTicket ticket,
            PairAnalyzedCandidate analyzed) {
        return analyzedCandidateIdentity == analyzed
                && analyzedCandidateIdentity.retainsTicket(ticket);
    }
}

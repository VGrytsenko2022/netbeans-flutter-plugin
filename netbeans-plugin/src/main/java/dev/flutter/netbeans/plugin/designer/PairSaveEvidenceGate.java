package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateSnapshot;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure fail-closed two-step binding of prepared analyzer and applied-live evidence.
 * The only boundary I/O is {@link Path#toRealPath} for path trust checks.
 */
final class PairSaveEvidenceGate {
    private static final String REQUIRED_STATELESS_WIDGET = "StatelessWidget";
    private static final String REQUIRED_WIDGET = "Widget";
    private static final String REQUIRED_BUILD_CONTEXT = "BuildContext";

    private PairSaveEvidenceGate() {
    }

    /**
     * Creates the only request which may analyze this exact prepared candidate.
     * No live document is consulted or mutated at this boundary.
     */
    static PairCandidateAnalysisTicket prepareAnalysis(
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            Path projectRoot,
            Path dartFile,
            DartCandidateWarningPolicy warningPolicy,
            Path trustedFlutterSdkRoot) throws IOException {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(dartFile, "dartFile");
        Objects.requireNonNull(warningPolicy, "warningPolicy");
        Objects.requireNonNull(trustedFlutterSdkRoot, "trustedFlutterSdkRoot");

        ArrayList<PairSaveEvidenceDiagnostic> diagnostics = new ArrayList<>();
        verifyCurrent(current, prepared, diagnostics);
        Path trustedReal = realPath(
                trustedFlutterSdkRoot,
                "trustedFlutterSdkRealRoot",
                diagnostics);
        Path projectReal = realPath(projectRoot, "analysis.projectRoot", diagnostics);
        Path dartReal = realPath(dartFile, "analysis.dartFile", diagnostics);
        verifyDartFileName(prepared, dartReal, diagnostics);
        if (projectReal != null && dartReal != null
                && !dartReal.startsWith(projectReal)) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_DART_PATH_MISMATCH,
                    "analysis.dartFile",
                    "The real Dart file is outside the real analyzer project root.");
        }
        if (!diagnostics.isEmpty()) {
            throw new IOException("Cannot create exact Flutter Designer analysis ticket: "
                    + diagnostics.getFirst().code() + " at "
                    + diagnostics.getFirst().subject() + ": "
                    + diagnostics.getFirst().message());
        }

        byte[] candidate = prepared.prospectiveDartBytes();
        DartCandidateCapacityBudget capacity = prepared.dartTransition()
                .generation().generated().orElseThrow()
                .candidateCapacityBudget();
        if (candidate.length > capacity.maxCandidateUtf8Bytes()) {
            throw new IOException(
                    "Cannot analyze Flutter Designer candidate: "
                    + candidate.length + " UTF-8 bytes exceed shared capacity profile "
                    + capacity.profileId() + " with maxCandidateUtf8Bytes="
                    + capacity.maxCandidateUtf8Bytes());
        }
        String content = new String(candidate, StandardCharsets.UTF_8);
        if (!Arrays.equals(candidate, content.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException(
                    "Cannot analyze Flutter Designer candidate: prospective Dart is not strict UTF-8");
        }
        List<DartSymbolProbe> probes;
        try {
            probes = GeneratedDartSymbolProbePlanner.plan(prepared, trustedReal);
        } catch (RuntimeException invalidManifest) {
            throw new IOException(
                    "Cannot derive exact Flutter Designer analyzer probes: "
                    + reason(invalidManifest), invalidManifest);
        }
        long overlayVersion;
        try {
            overlayVersion = PairCandidateAnalysisTicket.nextOverlayVersion();
        } catch (IllegalStateException exhausted) {
            throw new IOException(exhausted.getMessage(), exhausted);
        }
        DartCandidateAnalysisRequest request = new DartCandidateAnalysisRequest(
                projectReal,
                dartReal,
                content,
                overlayVersion,
                sha256(candidate),
                warningPolicy,
                probes,
                capacity);
        return new PairCandidateAnalysisTicket(
                current,
                prepared,
                request,
                trustedReal,
                projectReal,
                dartReal);
    }

    /** Consumes a ticket result before any live-document mutation. */
    static PairAnalyzedCandidateResult evaluateAnalysis(
            PairCandidateAnalysisTicket ticket,
            DartCandidateAnalysisResult analysis) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(analysis, "analysis");

        ArrayList<PairSaveEvidenceDiagnostic> diagnostics = new ArrayList<>();
        verifyAnalysis(ticket, analysis, diagnostics);
        if (!diagnostics.isEmpty()) {
            return new PairAnalyzedCandidateResult.Rejected(diagnostics);
        }
        return new PairAnalyzedCandidateResult.Ready(
                new PairAnalyzedCandidate(ticket, analysis));
    }

    /** Binds a separately captured post-CAS live identity to prior analyzer proof. */
    static PairSaveEvidenceResult bindApplied(
            PairAnalyzedCandidate analyzed,
            LiveDartDocumentSnapshot appliedLive) {
        Objects.requireNonNull(analyzed, "analyzed");
        Objects.requireNonNull(appliedLive, "appliedLive");

        ArrayList<PairSaveEvidenceDiagnostic> diagnostics = new ArrayList<>();
        verifyLiveCandidate(
                analyzed.preparedIdentity(), appliedLive, diagnostics);

        if (!diagnostics.isEmpty()) {
            return new PairSaveEvidenceResult.Rejected(diagnostics);
        }
        return new PairSaveEvidenceResult.Ready(new PairSaveEvidence(
                analyzed,
                appliedLive));
    }

    private static void verifyCurrent(
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        if (!current.validation().valid()
                || !current.catalogDiagnostics().isEmpty()
                || !current.contextIssues().isEmpty()
                || current.sourceIntegrity().isEmpty()
                || current.threeWayIntegrity().isEmpty()
                || !current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch()
                || !current.threeWayIntegrity().orElseThrow().onDiskThreeWayMatch()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.CURRENT_STATE_NOT_WRITABLE,
                    "current",
                    "The loaded Designer state is not a complete writable current pair.");
        }

        if (current.decoded().original() != prepared.baselineFd()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .LOADED_FD_BASELINE_IDENTITY_MISMATCH,
                    "current.decoded.original",
                    "The prepared pair does not retain the exact loaded .fd baseline identity.");
        }
        if (!current.decoded().original().contentEquals(prepared.baselineFdBytes())
                || !current.decoded().document().equals(
                        prepared.baselineDocument())) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.FD_BASELINE_MISMATCH,
                    "prepared.baselineFd",
                    "The prepared .fd baseline differs from the loaded current document.");
        }

        if (current.threeWayIntegrity().isPresent()
                && current.threeWayIntegrity().orElseThrow()
                        != prepared.dartTransition().baseline()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .DART_BASELINE_EVIDENCE_IDENTITY_MISMATCH,
                    "current.threeWayIntegrity",
                    "The prepared transition does not retain the exact loaded Dart baseline evidence.");
        }

        if (current.sourceIntegrity().isPresent()) {
            DartSourceIntegrityResult source = current.sourceIntegrity().orElseThrow();
            if (source.original().isEmpty()
                    || !source.original().orElseThrow().contentEquals(
                            prepared.baselineDartBytes())) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.DART_BASELINE_MISMATCH,
                        "prepared.baselineDart",
                        "The prepared Dart baseline differs from the loaded source snapshot.");
            }
        }
        if (current.threeWayIntegrity().isPresent()) {
            DartThreeWayIntegrityResult threeWay =
                    current.threeWayIntegrity().orElseThrow();
            if (threeWay.original().isEmpty()
                    || !threeWay.original().orElseThrow().contentEquals(
                            prepared.baselineDartBytes())) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.DART_BASELINE_MISMATCH,
                        "prepared.transition.baseline",
                        "The prepared Dart bytes differ from the exact three-way baseline.");
            }
        }
    }

    private static byte[] verifyLiveCandidate(
            PreparedDesignerPair prepared,
            LiveDartDocumentSnapshot live,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        byte[] candidate = prepared.prospectiveDartBytes();
        byte[] liveBytes = live.markerBearingUtf8();
        String candidateSha = sha256(candidate);
        if (!Arrays.equals(candidate, liveBytes)
                || !candidateSha.equals(live.markerBearingSha256())) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.LIVE_CANDIDATE_MISMATCH,
                    "liveCandidate",
                    "The live Dart document is not the exact prepared candidate snapshot.");
        }
        return candidate;
    }

    private static void verifyDartFileName(
            PreparedDesignerPair prepared,
            Path dartReal,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        if (dartReal == null) {
            return;
        }
        String expected = prepared.prospectiveDocument().source().dartFile();
        Path fileName = dartReal.getFileName();
        if (fileName == null || !expected.equals(fileName.toString())) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_DART_PATH_MISMATCH,
                    "realDartPath",
                    "The real Dart path does not name the source file declared by the prepared pair.");
        }
    }

    private static void verifyAnalysis(
            PairCandidateAnalysisTicket ticket,
            DartCandidateAnalysisResult analysis,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        PreparedDesignerPair prepared = ticket.preparedIdentity();
        DartCandidateAnalysisRequest request = ticket.request();
        byte[] candidate = prepared.prospectiveDartBytes();
        if (!analysis.passed()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED,
                    "analysis.status",
                    "Pair-save evidence requires a PASSED candidate analysis result.");
        }

        DartCandidateSnapshot snapshot = analysis.snapshot();
        DartCandidateSnapshot requested = request.snapshot();
        if (!snapshot.equals(requested)) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_REQUEST_MISMATCH,
                    "analysis.snapshot",
                    "The analyzer result does not retain the exact ticket request snapshot.");
        }
        String candidateSha = sha256(candidate);
        if (!candidateSha.equals(snapshot.sha256())
                || snapshot.utf8Size() != candidate.length) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_CANDIDATE_MISMATCH,
                    "analysis.snapshot",
                    "The analyzer snapshot does not identify the exact prepared candidate bytes.");
        }
        if (snapshot.version() != request.version()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_VERSION_MISMATCH,
                    "analysis.snapshot.version",
                    "The analyzer version does not equal the ticket's unique overlay version.");
        }

        Path analyzedDartReal = realPath(
                snapshot.dartFile(), "analysis.snapshot.dartFile", diagnostics);
        Path analyzedProjectReal = realPath(
                snapshot.projectRoot(), "analysis.snapshot.projectRoot", diagnostics);
        if (analyzedDartReal != null
                && !ticket.realDartPath().equals(analyzedDartReal)) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_DART_PATH_MISMATCH,
                    "analysis.snapshot.dartFile",
                    "The analyzer snapshot belongs to a different real Dart file.");
        }
        if (analyzedProjectReal != null
                && !ticket.realProjectRoot().equals(analyzedProjectReal)) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ANALYSIS_DART_PATH_MISMATCH,
                    "analysis.snapshot.projectRoot",
                    "The analyzer result belongs to a different real project root.");
        }

        List<DartSymbolProbe> actualProbes = analysis.symbolEvidence().stream()
                .map(DartSymbolEvidence::probe)
                .toList();
        if (analysis.requestedSymbolProbes() != request.symbolProbes().size()
                || !actualProbes.equals(request.symbolProbes())) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .GENERATED_SYMBOL_PROBE_SET_MISMATCH,
                    "analysis.symbolEvidence",
                    "The analyzer result does not retain the ticket's exact ordered symbol probes.");
        }

        verifySymbolEvidence(
                prepared,
                analysis,
                new String(candidate, StandardCharsets.UTF_8),
                ticket.trustedFlutterSdkRealRoot(),
                diagnostics);
    }

    private static void verifySymbolEvidence(
            PreparedDesignerPair prepared,
            DartCandidateAnalysisResult analysis,
            String candidate,
            Path trustedReal,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        verifyExactProbeManifest(
                prepared,
                analysis,
                trustedReal,
                diagnostics);
        if (analysis.requestedSymbolProbes() == 0) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ZERO_SYMBOL_PROBES,
                    "analysis.requestedSymbolProbes",
                    "Pair-save evidence requires non-empty Flutter symbol probes.");
        }
        if (analysis.symbolEvidence().size()
                != analysis.requestedSymbolProbes()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.INCOMPLETE_SYMBOL_EVIDENCE,
                    "analysis.symbolEvidence",
                    "The analyzer did not return one result for every requested symbol probe.");
        }

        Set<String> ids = new HashSet<>();
        boolean statelessWidget = false;
        boolean widget = false;
        boolean buildContext = false;
        for (DartSymbolEvidence evidence : analysis.symbolEvidence()) {
            DartSymbolProbe probe = evidence.probe();
            int diagnosticsBefore = diagnostics.size();
            if (!ids.add(probe.id())) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.DUPLICATE_SYMBOL_PROBE,
                        "analysis.symbolEvidence." + probe.id(),
                        "Analyzer evidence contains a duplicate symbol probe id.");
            }
            if (!evidence.accepted()) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.INCOMPLETE_SYMBOL_EVIDENCE,
                        "analysis.symbolEvidence." + probe.id(),
                        "Every symbol probe must retain accepted analyzer evidence.");
            }
            verifyOccurrence(candidate, probe, diagnostics);
            if (!isFlutterLibraryUri(probe.expectedLibraryUri())) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.INVALID_FLUTTER_LIBRARY_URI,
                        "analysis.symbolEvidence." + probe.id(),
                        "A pair-save symbol probe must identify a package:flutter library URI.");
            }

            Path expectedRootReal = realPath(
                    probe.expectedTargetRoot(),
                    "analysis.symbolEvidence." + probe.id() + ".expectedTargetRoot",
                    diagnostics);
            if (trustedReal != null && expectedRootReal != null
                    && !expectedRootReal.startsWith(trustedReal)) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT,
                        "analysis.symbolEvidence." + probe.id(),
                        "The probe's real target root is outside the trusted Flutter SDK root.");
            }

            if (evidence.targets().size() != 1) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.INCOMPLETE_SYMBOL_EVIDENCE,
                        "analysis.symbolEvidence." + probe.id() + ".targets",
                        "Accepted analyzer evidence must retain exactly one navigation target.");
            }
            for (DartNavigationTarget target : evidence.targets()) {
                Path targetReal = realPath(
                        target.file(),
                        "analysis.symbolEvidence." + probe.id() + ".target",
                        diagnostics);
                if (targetReal != null
                        && ((trustedReal != null
                                && !targetReal.startsWith(trustedReal))
                            || (expectedRootReal != null
                                && !targetReal.startsWith(expectedRootReal)))) {
                    add(diagnostics,
                            PairSaveEvidenceDiagnostic.Code
                                    .UNTRUSTED_NAVIGATION_TARGET,
                            "analysis.symbolEvidence." + probe.id() + ".target",
                            "The analyzer navigation target is outside its trusted real roots.");
                }
                if (probe.expectedTargetKind().isPresent()
                        && !probe.expectedTargetKind().orElseThrow()
                                .equals(target.kind())) {
                    add(diagnostics,
                            PairSaveEvidenceDiagnostic.Code
                                    .UNTRUSTED_NAVIGATION_TARGET,
                            "analysis.symbolEvidence." + probe.id() + ".kind",
                            "The analyzer navigation target kind differs from the probe contract.");
                }
            }

            if (diagnostics.size() == diagnosticsBefore) {
                statelessWidget |= GeneratedDartSymbolProbePlanner
                        .DESIGNER_SUPERCLASS_PROBE_ID.equals(probe.id())
                        && REQUIRED_STATELESS_WIDGET.equals(
                                probe.expectedSymbolName());
                widget |= REQUIRED_WIDGET.equals(probe.expectedSymbolName());
                buildContext |= REQUIRED_BUILD_CONTEXT.equals(
                        probe.expectedSymbolName());
            }
        }
        if (!statelessWidget) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .REQUIRED_STATELESS_WIDGET_PROBE_MISSING,
                    "analysis.symbolEvidence",
                    "Pair-save evidence requires the accepted scanner-owned "
                    + "StatelessWidget superclass probe.");
        }
        if (!widget) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.REQUIRED_WIDGET_PROBE_MISSING,
                    "analysis.symbolEvidence",
                    "Pair-save evidence requires an accepted Widget probe.");
        }
        if (!buildContext) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .REQUIRED_BUILD_CONTEXT_PROBE_MISSING,
                    "analysis.symbolEvidence",
                    "Pair-save evidence requires an accepted BuildContext probe.");
        }
    }

    private static void verifyExactProbeManifest(
            PreparedDesignerPair prepared,
            DartCandidateAnalysisResult analysis,
            Path trustedReal,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        if (trustedReal == null) {
            return;
        }
        List<DartSymbolProbe> expected;
        try {
            expected = GeneratedDartSymbolProbePlanner.plan(prepared, trustedReal);
        } catch (RuntimeException invalidManifest) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .GENERATED_SYMBOL_PROBE_SET_MISMATCH,
                    "prepared.dartTransition.candidateIntegrity",
                    "Cannot derive exact scanner-and-generator Flutter symbol probes: "
                    + reason(invalidManifest));
            return;
        }
        List<DartSymbolProbe> actual = analysis.symbolEvidence().stream()
                .map(DartSymbolEvidence::probe)
                .toList();
        if (!expected.equals(actual)) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .GENERATED_SYMBOL_PROBE_SET_MISMATCH,
                    "analysis.symbolEvidence",
                    "Analyzer evidence does not exactly match the ordered, "
                    + "scanner-and-generator-owned Flutter symbol occurrence probe set.");
        }
    }

    private static void verifyOccurrence(
            String candidate,
            DartSymbolProbe probe,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        long end = (long) probe.offset() + probe.length();
        if (probe.offset() > candidate.length()
                || end > candidate.length()
                || !candidate.substring(probe.offset(), (int) end)
                        .equals(probe.expectedSymbolName())) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.INVALID_SYMBOL_OCCURRENCE,
                    "analysis.symbolEvidence." + probe.id() + ".occurrence",
                    "The symbol probe does not select its expected text in the exact candidate.");
        }
    }

    private static boolean isFlutterLibraryUri(String value) {
        try {
            URI uri = new URI(value);
            if (!"package".equals(uri.getScheme()) || uri.getRawFragment() != null) {
                return false;
            }
            String part = uri.getRawSchemeSpecificPart();
            if (part == null || !part.startsWith("flutter/")
                    || part.length() == "flutter/".length()
                    || part.indexOf('?') >= 0
                    || part.indexOf('#') >= 0
                    || part.indexOf('\\') >= 0) {
                return false;
            }
            for (String segment : part.substring("flutter/".length()).split("/")) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    return false;
                }
            }
            return true;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static Path realPath(
            Path path,
            String subject,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        if (!path.isAbsolute()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.PATH_VALIDATION_FAILED,
                    subject,
                    "The path must be absolute before real-path validation.");
            return null;
        }
        try {
            return path.toRealPath();
        } catch (IOException | SecurityException ex) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.PATH_VALIDATION_FAILED,
                    subject,
                    "The path cannot be resolved to an existing real path: "
                    + reason(ex));
            return null;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }

    private static void add(
            List<PairSaveEvidenceDiagnostic> diagnostics,
            PairSaveEvidenceDiagnostic.Code code,
            String subject,
            String message) {
        diagnostics.add(new PairSaveEvidenceDiagnostic(code, subject, message));
    }

    private static String reason(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.strip();
    }
}

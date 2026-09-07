package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateDiagnostic;
import dev.flutter.netbeans.dart.DartCandidateSnapshot;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.dart.DartStaticTypeEvidence;
import dev.flutter.netbeans.dart.DartStaticTypeProbe;
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
import java.util.regex.Pattern;

/**
 * Pure fail-closed two-step binding of prepared analyzer and applied-live evidence.
 * The only boundary I/O is {@link Path#toRealPath} for path trust checks.
 */
final class PairSaveEvidenceGate {
    private static final String REQUIRED_STATELESS_WIDGET = "StatelessWidget";
    private static final String REQUIRED_WIDGET = "Widget";
    private static final String REQUIRED_BUILD_CONTEXT = "BuildContext";
    private static final String CURRENT_PROJECT_LIBRARY_URI = "project:current";
    private static final Set<String> RADIO_CORE_TYPES = Set.of(
            "String", "int", "double", "num", "bool", "Object");
    private static final Pattern RADIO_CORE_TYPE_PROBE_ID = Pattern.compile(
            "widget:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:radio(?:-group)?-core-value-type");
    private static final Pattern PROJECT_PACKAGE_LIBRARY_URI = Pattern.compile(
            "package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart");
    private static final Pattern SAFE_DIAGNOSTIC_CODE = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_.-]{0,127}");
    private static final Pattern SAFE_PROBE_ID = Pattern.compile(
            "[A-Za-z0-9_:/~.-]{1,512}");
    private static final Pattern SAFE_MODEL_PATH = Pattern.compile(
            "/[A-Za-z0-9_~/.-]+");
    private static final Pattern FILE_URI = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])file:/+[^\\s,;]+");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])[a-z]:[\\\\/][^\\s,;]+");
    private static final Pattern UNC_PATH = Pattern.compile(
            "\\\\\\\\[^\\s,;]+");
    private static final Pattern POSIX_ABSOLUTE_PATH = Pattern.compile(
            "(?<![A-Za-z0-9_:])/(?:[^\\s,;]+)");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|passphrase|secret|token|api[_-]?key|"
            + "access[_-]?key|authorization)\\s*[:=]\\s*");
    private static final Pattern SECRET_TOKEN = Pattern.compile(
            "(?i)(?<![A-Za-z0-9])(?:sk-(?:proj-)?[A-Za-z0-9_-]{8,}|"
            + "gh[pousr]_[A-Za-z0-9]{8,}|xox[baprs]-[A-Za-z0-9-]{8,})");
    private static final int MAXIMUM_EXTERNAL_DETAIL_CODE_POINTS = 384;
    private static final int MAXIMUM_EXTERNAL_SOURCE_CODE_POINTS = 2_048;

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
            probes = GeneratedDartSymbolProbePlanner.plan(
                    prepared, trustedReal, projectReal);
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
        // Analyzer navigation is filesystem-backed evidence. Re-run the full
        // provenance checks at the post-CAS bind boundary so a target/root
        // deleted or redirected after analysis cannot be carried into a save.
        verifySymbolEvidence(
                analyzed.preparedIdentity(),
                analyzed.analysisIdentity(),
                analyzed.requestIdentity().content(),
                analyzed.trustedFlutterSdkRealRoot(),
                analyzed.realProjectRoot(),
                diagnostics);

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
                    concreteAnalysisFailure(analysis));
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
                ticket.realProjectRoot(),
                diagnostics);
    }

    private static void verifySymbolEvidence(
            PreparedDesignerPair prepared,
            DartCandidateAnalysisResult analysis,
            String candidate,
            Path trustedFlutterReal,
            Path trustedProjectReal,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        Path projectLibraryReal = null;
        if (analysis.symbolEvidence().stream().map(DartSymbolEvidence::probe)
                .map(DartSymbolProbe::expectedLibraryUri)
                .anyMatch(CURRENT_PROJECT_LIBRARY_URI::equals)) {
            projectLibraryReal = realPath(
                    trustedProjectReal.resolve("lib"),
                    "analysis.projectLibraryRoot",
                    diagnostics);
            if (projectLibraryReal != null
                    && !projectLibraryReal.startsWith(trustedProjectReal)) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT,
                        "analysis.projectLibraryRoot",
                        "The real project lib directory resolves outside the real project root.");
                projectLibraryReal = null;
            }
        }
        verifyExactProbeManifest(
                prepared,
                analysis,
                trustedFlutterReal,
                trustedProjectReal,
                diagnostics);
        if (analysis.requestedSymbolProbes() == 0) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.ZERO_SYMBOL_PROBES,
                    "analysis.requestedSymbolProbes",
                    "Pair-save evidence requires non-empty Dart symbol probes.");
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
                        rejectedProbeMessage(evidence));
            }
            verifyOccurrence(candidate, probe, diagnostics);
            verifyStaticTypeEvidence(candidate, evidence, diagnostics);
            boolean flutterLibrary = isFlutterLibraryUri(
                    probe.expectedLibraryUri());
            boolean radioCoreType = isRadioCoreTypeProbe(probe);
            boolean currentProjectLibrary = CURRENT_PROJECT_LIBRARY_URI.equals(
                    probe.expectedLibraryUri());
            boolean declaredPackageLibrary = isDeclaredPackageLibraryUri(
                    probe.expectedLibraryUri());
            if (!flutterLibrary && !radioCoreType && !currentProjectLibrary
                    && !declaredPackageLibrary) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.INVALID_FLUTTER_LIBRARY_URI,
                        "analysis.symbolEvidence." + probe.id(),
                        "A pair-save symbol probe must identify a package:flutter URI "
                        + "or a closed current/declared project package library URI, "
                        + "or the exact generator-owned Radio dart:core type contract.");
            }

            Path expectedRootReal = realPath(
                    probe.expectedTargetRoot(),
                    "analysis.symbolEvidence." + probe.id() + ".expectedTargetRoot",
                    diagnostics);
            Path authorizedRoot = flutterLibrary || radioCoreType
                    ? trustedFlutterReal
                    : currentProjectLibrary
                            ? projectLibraryReal
                            : declaredPackageLibrary ? expectedRootReal : null;
            boolean trustedExpectedRoot = expectedRootReal != null
                    && authorizedRoot != null
                    && (flutterLibrary
                            ? expectedRootReal.startsWith(authorizedRoot)
                            : currentProjectLibrary
                                    ? expectedRootReal.equals(authorizedRoot)
                                    : expectedRootReal.equals(authorizedRoot));
            if ((flutterLibrary || radioCoreType || currentProjectLibrary
                    || declaredPackageLibrary) && !trustedExpectedRoot) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT,
                        "analysis.symbolEvidence." + probe.id(),
                        flutterLibrary || radioCoreType
                                ? "The probe's real target root is outside the trusted Flutter SDK root."
                                : currentProjectLibrary
                                        ? "The current-library probe's real target root is not the trusted project lib directory."
                                        : "The declared-package probe has no real package library root.");
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
                        && ((authorizedRoot != null
                                && !targetReal.startsWith(authorizedRoot))
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
                if (radioCoreType && (targetReal == null
                        || !"CLASS".equals(target.kind())
                        || !isRadioCoreTypeTarget(trustedFlutterReal, targetReal,
                                probe.expectedSymbolName()))) {
                    add(diagnostics,
                            PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET,
                            "analysis.symbolEvidence." + probe.id() + ".target",
                            "The Radio or RadioGroup core type must resolve to its exact class in the trusted SDK Dart or sky_engine core library.");
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

    /** Additional closed admission; the complete prepared manifest is still compared above. */
    private static boolean isRadioCoreTypeProbe(DartSymbolProbe probe) {
        if (!"dart:core".equals(probe.expectedLibraryUri())
                || !RADIO_CORE_TYPE_PROBE_ID.matcher(probe.id()).matches()
                || !RADIO_CORE_TYPES.contains(probe.expectedSymbolName())) return false;
        DartStaticTypeProbe type = probe.staticTypeProbe().orElse(null);
        return type != null && type.expectedDartType().equals("Type")
                && type.expressionOffset() == probe.offset()
                && type.expressionLength() == probe.length()
                && probe.length() == probe.expectedSymbolName().length()
                && type.sourceTypeOverride().filter(value -> value.equals(probe.expectedSymbolName())
                        || value.equals(probe.expectedSymbolName() + '?')).isPresent();
    }

    private static boolean isRadioCoreTypeTarget(Path trustedSdk, Path target, String symbol) {
        if (trustedSdk == null) return false;
        for (String core : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
            try {
                Path declared = trustedSdk.resolve(core).resolve(
                        symbol.toLowerCase(java.util.Locale.ROOT) + ".dart").toRealPath();
                if (declared.startsWith(trustedSdk) && target.equals(declared)) return true;
            } catch (IOException unavailableCoreLibrary) {
                // A distribution need not expose both public core-library trees.
            }
        }
        return false;
    }

    private static void verifyExactProbeManifest(
            PreparedDesignerPair prepared,
            DartCandidateAnalysisResult analysis,
            Path trustedFlutterReal,
            Path trustedProjectReal,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        if (trustedFlutterReal == null || trustedProjectReal == null) {
            return;
        }
        List<DartSymbolProbe> expected;
        try {
            expected = GeneratedDartSymbolProbePlanner.plan(
                    prepared, trustedFlutterReal, trustedProjectReal);
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

    private static void verifyStaticTypeEvidence(
            String candidate,
            DartSymbolEvidence evidence,
            List<PairSaveEvidenceDiagnostic> diagnostics) {
        DartSymbolProbe symbol = evidence.probe();
        if (symbol.staticTypeProbe().isEmpty()) {
            if (evidence.staticTypeEvidence().isPresent()) {
                add(diagnostics,
                        PairSaveEvidenceDiagnostic.Code
                                .INCOMPLETE_STATIC_TYPE_EVIDENCE,
                        "analysis.symbolEvidence." + symbol.id()
                                + ".staticTypeEvidence",
                        "Untyped symbol evidence must not carry a detached static-type proof.");
            }
            return;
        }
        DartStaticTypeProbe probe = symbol.staticTypeProbe().orElseThrow();
        long expressionEnd = (long) probe.expressionOffset()
                + probe.expressionLength();
        if (expressionEnd > candidate.length()
                || probe.statementInsertionOffset() > probe.expressionOffset()
                || probe.importInsertionOffset() > probe.statementInsertionOffset()) {
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code.INVALID_SYMBOL_OCCURRENCE,
                    "analysis.symbolEvidence." + symbol.id()
                            + ".staticTypeExpression",
                    "The static-type probe does not identify a bounded expression in the exact candidate.");
        }
        DartStaticTypeEvidence staticType = evidence.staticTypeEvidence()
                .orElse(null);
        if (staticType == null
                || !staticType.probe().equals(probe)
                || !staticType.accepted()) {
            String reason = staticType == null
                    ? "The analyzer returned no static-type proof."
                    : boundedExternalDetail(
                            staticType.rejectionReason().orElse(null),
                            "The analyzer rejected the static-type proof.");
            add(diagnostics,
                    PairSaveEvidenceDiagnostic.Code
                            .INCOMPLETE_STATIC_TYPE_EVIDENCE,
                    "analysis.symbolEvidence." + symbol.id()
                            + ".staticTypeEvidence",
                    "Dart object reference at model path "
                    + probeModelPath(symbol)
                    + " (probe " + safeProbeId(symbol) + ") must have exact non-null "
                    + probe.expectedDartType() + ": " + reason);
        }
    }

    private static String concreteAnalysisFailure(
            DartCandidateAnalysisResult analysis) {
        DartCandidateDiagnostic diagnostic = analysis.diagnostics().stream()
                .filter(DartCandidateDiagnostic::blocking)
                .findFirst()
                .orElse(null);
        if (diagnostic != null) {
            String code = diagnostic.code()
                    .filter(SAFE_DIAGNOSTIC_CODE.asMatchPredicate())
                    .map(value -> " [" + value + "]")
                    .orElse("");
            return "Dart analyzer rejected the candidate at line "
                    + diagnostic.startLine() + ", column "
                    + diagnostic.startColumn() + code + ": "
                    + boundedExternalDetail(
                            diagnostic.message(),
                            "The analyzer returned no usable diagnostic detail.");
        }
        DartSymbolEvidence evidence = analysis.symbolEvidence().stream()
                .filter(value -> !value.accepted())
                .findFirst()
                .orElse(null);
        if (evidence != null) {
            return rejectedProbeMessage(evidence);
        }
        if (analysis.issue().isPresent()) {
            var issue = analysis.issue().orElseThrow();
            return "Dart analyzer could not establish save evidence ["
                    + issue.code() + "]: "
                    + boundedExternalDetail(
                            issue.message(),
                            "No usable analyzer failure detail was returned.");
        }
        return "Pair-save evidence requires a PASSED candidate analysis result.";
    }

    private static String rejectedProbeMessage(DartSymbolEvidence evidence) {
        DartSymbolProbe probe = evidence.probe();
        String expected = probe.staticTypeProbe()
                .map(value -> "exact non-null " + value.expectedDartType())
                .orElseGet(() -> expectedLibraryDescription(probe));
        return "Dart analyzer rejected model path " + probeModelPath(probe)
                + " (probe " + safeProbeId(probe) + ", expected " + expected
                + "): " + boundedExternalDetail(
                        evidence.rejectionReason().orElse(null),
                        "No concrete symbol rejection reason was returned.");
    }

    private static String probeModelPath(DartSymbolProbe probe) {
        String id = probe.id();
        String marker = ":property-reference:";
        int start = id.indexOf(marker);
        int end = id.lastIndexOf(':');
        String modelPath = start >= 0 && end > start + marker.length()
                ? id.substring(start + marker.length(), end) : "";
        return SAFE_MODEL_PATH.matcher(modelPath).matches()
                && (modelPath.equals("/root")
                    || modelPath.startsWith("/root/"))
                ? boundedCodePoints(modelPath, 512, false)
                : "[unavailable]";
    }

    private static String safeProbeId(DartSymbolProbe probe) {
        return SAFE_PROBE_ID.matcher(probe.id()).matches()
                ? probe.id() : "[unavailable]";
    }

    private static String expectedLibraryDescription(DartSymbolProbe probe) {
        String libraryUri = probe.expectedLibraryUri();
        return isFlutterLibraryUri(libraryUri)
                || CURRENT_PROJECT_LIBRARY_URI.equals(libraryUri)
                || isDeclaredPackageLibraryUri(libraryUri)
                ? "library " + libraryUri
                : "the declared Dart library";
    }

    /**
     * Normalizes external analyzer text before it crosses into mutation UI.
     * Only a small single-line prefix is retained; absolute paths and common
     * credential forms discard their complete suffix so path segments or
     * secret material cannot survive a partial replacement.
     */
    private static String boundedExternalDetail(
            String value,
            String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        StringBuilder compact = new StringBuilder(
                Math.min(value.length(), MAXIMUM_EXTERNAL_SOURCE_CODE_POINTS));
        int sourceIndex = 0;
        int sourceCodePoints = 0;
        boolean pendingSpace = false;
        while (sourceIndex < value.length()
                && sourceCodePoints < MAXIMUM_EXTERNAL_SOURCE_CODE_POINTS) {
            int codePoint = value.codePointAt(sourceIndex);
            sourceIndex += Character.charCount(codePoint);
            sourceCodePoints++;
            if (Character.isISOControl(codePoint)
                    || Character.isWhitespace(codePoint)
                    || Character.isSpaceChar(codePoint)
                    || Character.getType(codePoint) == Character.FORMAT
                    || codePoint >= Character.MIN_SURROGATE
                    && codePoint <= Character.MAX_SURROGATE) {
                pendingSpace = compact.length() > 0;
                continue;
            }
            if (pendingSpace) {
                compact.append(' ');
                pendingSpace = false;
            }
            compact.appendCodePoint(codePoint);
        }
        String normalized = compact.toString().strip();
        if (normalized.isEmpty()) {
            return fallback;
        }

        int pathOffset = firstFilesystemPathOffset(normalized);
        java.util.regex.Matcher sensitive = SENSITIVE_ASSIGNMENT.matcher(
                normalized);
        java.util.regex.Matcher token = SECRET_TOKEN.matcher(normalized);
        int sensitiveOffset = sensitive.find() ? sensitive.start() : -1;
        int tokenOffset = token.find() ? token.start() : -1;
        int firstHazard = firstNonNegative(
                pathOffset, sensitiveOffset, tokenOffset);
        if (firstHazard >= 0) {
            String safePrefix = normalized.substring(0, firstHazard)
                    .stripTrailing();
            String replacement;
            if (firstHazard == pathOffset) {
                replacement = "[path]";
            } else if (firstHazard == sensitiveOffset) {
                replacement = sensitive.group(1) + "=[redacted]";
            } else {
                replacement = "[redacted secret]";
            }
            normalized = safePrefix.isEmpty()
                    ? replacement : safePrefix + " " + replacement;
            sourceIndex = value.length();
        }
        return boundedCodePoints(
                normalized,
                MAXIMUM_EXTERNAL_DETAIL_CODE_POINTS,
                sourceIndex < value.length());
    }

    private static int firstFilesystemPathOffset(String value) {
        int first = -1;
        for (Pattern pattern : List.of(
                FILE_URI,
                WINDOWS_ABSOLUTE_PATH,
                UNC_PATH,
                POSIX_ABSOLUTE_PATH)) {
            java.util.regex.Matcher matcher = pattern.matcher(value);
            if (matcher.find() && (first < 0 || matcher.start() < first)) {
                first = matcher.start();
            }
        }
        return first;
    }

    private static int firstNonNegative(int... values) {
        int first = -1;
        for (int value : values) {
            if (value >= 0 && (first < 0 || value < first)) {
                first = value;
            }
        }
        return first;
    }

    private static String boundedCodePoints(
            String value,
            int maximum,
            boolean alreadyTruncated) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= maximum && !alreadyTruncated) {
            return value;
        }
        int retained = Math.min(codePoints, maximum - 1);
        int end = value.offsetByCodePoints(0, retained);
        return value.substring(0, end).stripTrailing() + '\u2026';
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

    private static boolean isDeclaredPackageLibraryUri(String value) {
        return !value.startsWith("package:flutter/")
                && PROJECT_PACKAGE_LIBRARY_URI.matcher(value).matches()
                && !value.contains("//")
                && !value.contains("/./")
                && !value.contains("/../");
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

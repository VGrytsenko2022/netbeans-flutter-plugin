package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalysisIssue;
import dev.flutter.netbeans.dart.DartCandidateAnalysisIssueCode;
import dev.flutter.netbeans.dart.DartCandidateAnalysisStatus;
import dev.flutter.netbeans.dart.DartCandidateDiagnostic;
import dev.flutter.netbeans.dart.DartCandidateDiagnosticSeverity;
import dev.flutter.netbeans.dart.DartCandidateSnapshot;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.dart.DartStaticTypeEvidence;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartGenerationLimits;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationPlanner;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PairSaveEvidenceGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final String PROJECT_PACKAGE_NAME = "evidence_gate_fixture";

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsOnlyTheExactLoadedLiveAndAnalyzedCandidate() throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult analysis = analysis(
                ticket, fixture.acceptedEvidence());
        PairAnalyzedCandidateResult analyzedResult = ticket.accept(analysis);

        assertTrue(analyzedResult.ready(),
                () -> analyzedResult.diagnostics().toString());
        PairAnalyzedCandidate analyzed = analyzedResult.analyzedOptional()
                .orElseThrow();
        assertTrue(analyzed.retainsTicket(ticket));
        assertTrue(analyzed.retainsExactInputs(
                ticket,
                fixture.current(),
                fixture.prepared(),
                ticket.request(),
                analysis));
        assertSame(ticket.request(), analyzed.requestIdentity());
        assertSame(analysis, analyzed.analysisIdentity());

        PairSaveEvidenceResult result = PairSaveEvidenceGate.bindApplied(
                analyzed, fixture.live());

        assertTrue(result.ready());
        assertTrue(result.diagnostics().isEmpty());
        PairSaveEvidence evidence = result.evidenceOptional().orElseThrow();
        assertTrue(evidence.retainsExactInputs(
                fixture.current(),
                fixture.prepared(),
                fixture.live(),
                analysis));
        assertTrue(evidence.retainsExactAnalysis(ticket, analyzed));
        assertSame(fixture.live().documentIdentity(), evidence.documentIdentity());
        assertEquals(fixture.live().documentVersion(), evidence.documentVersion());
        assertEquals(fixture.live().markerBearingSha256(),
                evidence.candidateSha256());
        assertEquals(fixture.prepared().prospectiveDartBytes().length,
                evidence.candidateUtf8Size());
        assertArrayEquals(fixture.prepared().baselineFdBytes(),
                evidence.baselineFdBytes());
        assertArrayEquals(fixture.prepared().baselineDartBytes(),
                evidence.baselineDartBytes());
        assertArrayEquals(fixture.prepared().prospectiveDartBytes(),
                evidence.candidateDartBytes());
        assertEquals(fixture.flutterLib().toRealPath(),
                evidence.trustedFlutterSdkRealRoot());
        assertEquals(fixture.dartFile().toRealPath(), evidence.realDartPath());
        assertEquals(
                List.of("StatelessWidget", "Widget", "BuildContext", "Text"),
                fixture.acceptedEvidence().stream()
                        .map(value -> value.probe().expectedSymbolName())
                        .toList());
        String exactCandidate = new String(
                fixture.prepared().prospectiveDartBytes(), StandardCharsets.UTF_8);
        assertTrue(fixture.acceptedEvidence().stream().allMatch(value -> {
            DartSymbolProbe probe = value.probe();
            return exactCandidate.substring(
                    probe.offset(), probe.offset() + probe.length())
                    .equals(probe.expectedSymbolName());
        }));
        DartSymbolProbe superclass = fixture.acceptedEvidence().getFirst().probe();
        assertEquals(GeneratedDartSymbolProbePlanner.DESIGNER_SUPERCLASS_PROBE_ID,
                superclass.id());
        assertEquals(exactCandidate.indexOf("StatelessWidget"),
                superclass.offset());
        assertEquals("package:flutter/widgets.dart",
                superclass.expectedLibraryUri());
        assertEquals(Optional.of("CLASS"), superclass.expectedTargetKind());

        byte[] escaped = evidence.candidateDartBytes();
        escaped[0] ^= 1;
        assertArrayEquals(fixture.prepared().prospectiveDartBytes(),
                evidence.candidateDartBytes());
    }

    @Test
    void analysisPrecedesLiveMutationAndAppliedIdentityBindsSeparately()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());

        PairAnalyzedCandidateResult analyzedResult = ticket.accept(
                analysis(ticket, fixture.acceptedEvidence()));

        assertTrue(analyzedResult.ready(),
                () -> analyzedResult.diagnostics().toString());
        PairAnalyzedCandidate analyzed = analyzedResult.analyzedOptional()
                .orElseThrow();

        Loaded baseline = load(fixture.prepared().liveDartBytes());
        LiveDartDocumentSnapshot baselineLive = LiveDartDocumentBridge.snapshot(
                baseline.document(), baseline.provider());
        assertFalse(PairSaveEvidenceGate.bindApplied(analyzed, baselineLive).ready(),
                "analyzer proof alone must not bind the pre-apply live revision");

        Loaded applied = load(fixture.prepared().prospectiveDartBytes());
        LiveDartDocumentSnapshot appliedLive = LiveDartDocumentBridge.snapshot(
                applied.document(), applied.provider());
        while (appliedLive.documentVersion() == ticket.request().version()) {
            int end = applied.document().getLength();
            applied.document().insertString(end, " ", null);
            applied.document().remove(end, 1);
            appliedLive = LiveDartDocumentBridge.snapshot(
                    applied.document(), applied.provider());
        }
        assertNotEquals(ticket.request().version(), appliedLive.documentVersion(),
                "overlay and Swing document versions are independent identities");

        PairSaveEvidenceResult bound = PairSaveEvidenceGate.bindApplied(
                analyzed, appliedLive);
        assertTrue(bound.ready(), () -> bound.diagnostics().toString());
        assertSame(applied.document(),
                bound.evidenceOptional().orElseThrow().documentIdentity());
    }

    @Test
    void ticketIsSingleUseAndCannotTransferAResultToAnotherPreparation()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket first = ticket(
                fixture, fixture.current());
        PairCandidateAnalysisTicket second = ticket(
                fixture, fixture.current());
        assertNotEquals(first.request().version(), second.request().version());
        DartCandidateAnalysisResult firstResult = analysis(
                first, fixture.acceptedEvidence());

        PairAnalyzedCandidateResult transferred = second.accept(firstResult);
        assertAnalyzedRejected(transferred,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_REQUEST_MISMATCH);
        assertAnalyzedRejected(transferred,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_VERSION_MISMATCH);

        assertTrue(first.accept(firstResult).ready());
        PairAnalyzedCandidateResult replay = first.accept(firstResult);
        assertAnalyzedRejected(replay,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_TICKET_ALREADY_USED);
        assertAnalyzedRejected(
                second.accept(analysis(second, fixture.acceptedEvidence())),
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_TICKET_ALREADY_USED);
    }

    @Test
    void cancelledTicketAndCancelledAnalyzerResultNeverPublishAnalyzedEvidence()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket cancelledTicket = ticket(
                fixture, fixture.current());
        assertTrue(cancelledTicket.cancel());
        assertFalse(cancelledTicket.cancel());
        assertAnalyzedRejected(
                cancelledTicket.accept(analysis(
                        cancelledTicket, fixture.acceptedEvidence())),
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_TICKET_ALREADY_USED);

        PairCandidateAnalysisTicket unavailableTicket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult cancelledResult =
                new DartCandidateAnalysisResult(
                        DartCandidateAnalysisStatus.UNAVAILABLE,
                        unavailableTicket.request().snapshot(),
                        Optional.empty(),
                        List.of(),
                        unavailableTicket.request().symbolProbes().size(),
                        List.of(),
                        Optional.of(new DartCandidateAnalysisIssue(
                                DartCandidateAnalysisIssueCode.CANCELLED,
                                "analysis cancelled")));
        PairAnalyzedCandidateResult unavailable = unavailableTicket.accept(
                cancelledResult);
        assertAnalyzedRejected(unavailable,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED);
        assertTrue(unavailable.analyzedOptional().isEmpty());
    }

    @Test
    void rejectedAnalysisReportsTheFirstConcreteAnalyzerCauseWithoutLeaking()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket unresolvedTicket = ticket(
                fixture, fixture.current());
        DartCandidateDiagnostic context = diagnostic(
                unresolvedTicket,
                DartCandidateDiagnosticSeverity.INFO,
                "todo",
                "Non-blocking analyzer context.",
                false,
                2,
                3);
        DartCandidateDiagnostic unresolved = diagnostic(
                unresolvedTicket,
                DartCandidateDiagnosticSeverity.ERROR,
                "undefined_identifier",
                "Undefined name 'MissingClipper'.\r\nDetails at "
                + "C:\\Users\\Jane Doe\\private-project\\home_page.dart "
                + "token=must-not-leak",
                true,
                7,
                11);
        DartCandidateDiagnostic later = diagnostic(
                unresolvedTicket,
                DartCandidateDiagnosticSeverity.ERROR,
                "later_error",
                "A later blocking diagnostic must not replace the first one.",
                true,
                9,
                2);

        PairAnalyzedCandidateResult unresolvedResult = unresolvedTicket.accept(
                rejectedAnalysis(
                        unresolvedTicket,
                        List.of(context, unresolved, later),
                        List.of()));
        PairSaveEvidenceDiagnostic unresolvedStatus = unresolvedResult
                .diagnostics().getFirst();
        assertAll(
                () -> assertEquals(
                        PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED,
                        unresolvedStatus.code()),
                () -> assertTrue(unresolvedStatus.message().contains(
                        "line 7, column 11 [undefined_identifier]"),
                        unresolvedStatus::message),
                () -> assertTrue(unresolvedStatus.message().contains(
                        "Undefined name 'MissingClipper'."),
                        unresolvedStatus::message),
                () -> assertTrue(unresolvedStatus.message().contains("[path]"),
                        unresolvedStatus::message),
                () -> assertFalse(unresolvedStatus.message().contains("Jane Doe"),
                        unresolvedStatus::message),
                () -> assertFalse(unresolvedStatus.message().contains(
                        "must-not-leak"), unresolvedStatus::message),
                () -> assertFalse(unresolvedStatus.message().contains(
                        "Non-blocking analyzer context"),
                        unresolvedStatus::message),
                () -> assertFalse(unresolvedStatus.message().contains(
                        "later blocking"), unresolvedStatus::message),
                () -> assertFalse(unresolvedStatus.message().contains("\n")),
                () -> assertTrue(unresolvedStatus.message().codePointCount(
                        0, unresolvedStatus.message().length()) <= 512));

        PairCandidateAnalysisTicket secretTicket = ticket(
                fixture, fixture.current());
        PairAnalyzedCandidateResult secretResult = secretTicket.accept(
                rejectedAnalysis(
                        secretTicket,
                        List.of(diagnostic(
                                secretTicket,
                                DartCandidateDiagnosticSeverity.ERROR,
                                "synthetic_error",
                                "Analyzer rejected credential authorization="
                                + "Bearer must-not-leak and trailing detail",
                                true,
                                1,
                                1)),
                        List.of()));
        String secretMessage = secretResult.diagnostics().getFirst().message();
        assertAll(
                () -> assertTrue(secretMessage.contains(
                        "authorization=[redacted]"), () -> secretMessage),
                () -> assertFalse(secretMessage.contains("Bearer"),
                        () -> secretMessage),
                () -> assertFalse(secretMessage.contains("must-not-leak"),
                        () -> secretMessage),
                () -> assertFalse(secretMessage.contains("trailing detail"),
                        () -> secretMessage));

        assertConcreteAnalyzerFailure(
                fixture,
                "uri_does_not_exist",
                "Target of URI doesn't exist: 'missing_clippers.dart'.",
                "missing_clippers.dart");
        PairSaveEvidenceDiagnostic requiredArgument =
                assertConcreteAnalyzerFailure(
                fixture,
                "missing_required_argument",
                "The named parameter 'radius' is required, but there's no corresponding argument. "
                + "x".repeat(2_000),
                "named parameter 'radius'");
        assertTrue(requiredArgument.message().endsWith("\u2026"),
                requiredArgument::message);
    }

    @Test
    void rejectedCustomClipperProofNamesModelPathTypeAndBoundedReason()
            throws Exception {
        PropertyValue.DartObjectReferenceValue reference =
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of("package:evidence_gate_fixture/clippers.dart"),
                        "WrongClipperFactory",
                        Optional.of("create"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true));
        Fixture fixture = fixture(Optional.of(reference));
        DartSymbolEvidence typed = fixture.acceptedEvidence().stream()
                .filter(value -> value.probe().staticTypeProbe().isPresent())
                .findFirst()
                .orElseThrow();
        String concreteReason = "The expression is not statically assignable to "
                + "non-null CustomClipper<RRect>. Analyzer detail at "
                + "/home/Jane Doe/private-project/lib/clippers.dart "
                + "api_key=must-not-leak";
        DartStaticTypeEvidence rejectedType = new DartStaticTypeEvidence(
                typed.probe().staticTypeProbe().orElseThrow(),
                false,
                Optional.of(concreteReason));
        DartSymbolEvidence rejectedTyped = new DartSymbolEvidence(
                typed.probe(),
                typed.targets(),
                false,
                Optional.of(concreteReason),
                Optional.of(rejectedType));
        List<DartSymbolEvidence> rejectedEvidence = fixture.acceptedEvidence()
                .stream()
                .map(value -> value == typed ? rejectedTyped : value)
                .toList();
        PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());

        PairAnalyzedCandidateResult result = ticket.accept(rejectedAnalysis(
                ticket, List.of(), rejectedEvidence));

        List<PairSaveEvidenceDiagnostic.Code> codes = result.diagnostics()
                .stream().map(PairSaveEvidenceDiagnostic::code).toList();
        assertEquals(PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED,
                codes.getFirst());
        assertTrue(codes.indexOf(PairSaveEvidenceDiagnostic.Code
                .INCOMPLETE_SYMBOL_EVIDENCE) < codes.indexOf(
                        PairSaveEvidenceDiagnostic.Code
                                .INCOMPLETE_STATIC_TYPE_EVIDENCE),
                () -> codes.toString());
        for (PairSaveEvidenceDiagnostic diagnostic : result.diagnostics()) {
            if (diagnostic.code()
                    != PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED
                    && diagnostic.code()
                    != PairSaveEvidenceDiagnostic.Code
                            .INCOMPLETE_SYMBOL_EVIDENCE
                    && diagnostic.code()
                    != PairSaveEvidenceDiagnostic.Code
                            .INCOMPLETE_STATIC_TYPE_EVIDENCE) {
                continue;
            }
            assertAll(
                    () -> assertTrue(diagnostic.message().contains(
                            "/root/properties/clipper"), diagnostic::message),
                    () -> assertTrue(diagnostic.message().contains(
                            typed.probe().id()), diagnostic::message),
                    () -> assertTrue(diagnostic.message().contains(
                            "CustomClipper<RRect>"), diagnostic::message),
                    () -> assertTrue(diagnostic.message().contains(
                            "not statically assignable"), diagnostic::message),
                    () -> assertTrue(diagnostic.message().contains("[path]"),
                            diagnostic::message),
                    () -> assertFalse(diagnostic.message().contains("Jane Doe"),
                            diagnostic::message),
                    () -> assertFalse(diagnostic.message().contains(
                            "must-not-leak"), diagnostic::message),
                    () -> assertTrue(diagnostic.message().codePointCount(
                            0, diagnostic.message().length()) <= 1_024));
        }
    }

    @Test
    void concurrentAcceptancePublishesExactlyOneAnalyzedToken()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult result = analysis(
                ticket, fixture.acceptedEvidence());
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return ticket.accept(result);
            });
            var second = executor.submit(() -> {
                start.await();
                return ticket.accept(result);
            });
            start.countDown();
            List<PairAnalyzedCandidateResult> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream()
                    .filter(PairAnalyzedCandidateResult::ready).count());
            assertEquals(1, outcomes.stream()
                    .filter(value -> value.diagnostics().stream().anyMatch(
                            diagnostic -> diagnostic.code()
                            == PairSaveEvidenceDiagnostic.Code
                                    .ANALYSIS_TICKET_ALREADY_USED))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsAnEqualButSubstitutedLoadedDartEvidenceIdentity()
            throws Exception {
        Fixture fixture = fixture();
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult substitutedSource = scanner.scan(
                fixture.prepared().baselineDartBytes(),
                fixture.current().decoded().document().source());
        DartThreeWayIntegrityResult substitutedThreeWay =
                new DartThreeWayIntegrityGate(scanner).evaluate(
                        substitutedSource,
                        fixture.current().decoded().document().source(),
                        fixture.prepared().dartTransition().baseline().generation());
        FlutterDesignerDocumentState.Current substituted = current(
                fixture.current().decoded(), substitutedThreeWay);

        IOException failure = assertThrows(IOException.class, () ->
                ticket(fixture, substituted));

        assertTrue(failure.getMessage().contains(
                PairSaveEvidenceDiagnostic.Code
                        .DART_BASELINE_EVIDENCE_IDENTITY_MISMATCH.name()));
    }

    @Test
    void rejectsStaleAnalyzerVersionAndSubstitutedCandidateSha()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult stale = analysis(
                ticket,
                ticket.request().version() + 1,
                sha256("x".repeat(fixture.prepared().prospectiveDartBytes().length)
                        .getBytes(StandardCharsets.UTF_8)),
                fixture.prepared().prospectiveDartBytes().length,
                fixture.dartFile(),
                fixture.acceptedEvidence());

        PairAnalyzedCandidateResult result = ticket.accept(stale);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_VERSION_MISMATCH);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_CANDIDATE_MISMATCH);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_REQUEST_MISMATCH);
    }

    @Test
    void rejectsPassedAnalysisWithZeroSymbolProbes() throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult zero = analysis(
                ticket,
                ticket.request().version(),
                ticket.request().sha256(),
                fixture.prepared().prospectiveDartBytes().length,
                fixture.dartFile(),
                List.of());

        PairAnalyzedCandidateResult result = ticket.accept(zero);

        assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.ZERO_SYMBOL_PROBES);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code
                        .REQUIRED_STATELESS_WIDGET_PROBE_MISSING);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.REQUIRED_WIDGET_PROBE_MISSING);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code
                        .REQUIRED_BUILD_CONTEXT_PROBE_MISSING);
    }

    @Test
    void rejectsCallerChosenSubsetEvenWhenMandatorySymbolsResolve()
            throws Exception {
        Fixture fixture = fixture();
        List<DartSymbolEvidence> subset = fixture.acceptedEvidence().stream()
                .filter(evidence -> evidence.probe().expectedSymbolName()
                        .equals("Widget")
                        || evidence.probe().expectedSymbolName()
                                .equals("BuildContext"))
                .toList();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult analysis = analysis(
                ticket,
                ticket.request().version(),
                ticket.request().sha256(),
                fixture.prepared().prospectiveDartBytes().length,
                fixture.dartFile(),
                subset);

        PairAnalyzedCandidateResult result = ticket.accept(analysis);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void rejectsMissingReorderedSubstitutedAndExtraScannerProbeEvidence()
            throws Exception {
        Fixture fixture = fixture();
        List<DartSymbolEvidence> exact = fixture.acceptedEvidence();

        List<DartSymbolEvidence> missing = exact.stream()
                .filter(value -> !value.probe().id().equals(
                        GeneratedDartSymbolProbePlanner
                                .DESIGNER_SUPERCLASS_PROBE_ID))
                .toList();
        PairAnalyzedCandidateResult missingResult = analyze(
                fixture, fixture.current(), missing);
        assertAnalyzedRejected(missingResult,
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        assertAnalyzedRejected(missingResult,
                PairSaveEvidenceDiagnostic.Code
                        .REQUIRED_STATELESS_WIDGET_PROBE_MISSING);

        ArrayList<DartSymbolEvidence> reordered = new ArrayList<>(exact);
        java.util.Collections.swap(reordered, 0, 1);
        assertAnalyzedRejected(analyze(
                fixture, fixture.current(), reordered),
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);

        DartSymbolProbe source = exact.getFirst().probe();
        DartSymbolEvidence substitutedSource = accepted(
                new DartSymbolProbe(
                        "source:substituted-superclass",
                        source.offset(),
                        source.length(),
                        source.expectedSymbolName(),
                        source.expectedLibraryUri(),
                        source.expectedTargetRoot(),
                        source.expectedTargetKind()),
                fixture.frameworkFile());
        ArrayList<DartSymbolEvidence> substituted = new ArrayList<>(exact);
        substituted.set(0, substitutedSource);
        PairAnalyzedCandidateResult substitutedResult = analyze(
                fixture, fixture.current(), substituted);
        assertAnalyzedRejected(substitutedResult,
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        assertAnalyzedRejected(substitutedResult,
                PairSaveEvidenceDiagnostic.Code
                        .REQUIRED_STATELESS_WIDGET_PROBE_MISSING);

        ArrayList<DartSymbolEvidence> extra = new ArrayList<>(exact);
        extra.add(accepted(
                new DartSymbolProbe(
                        "source:extra",
                        source.offset(),
                        source.length(),
                        source.expectedSymbolName(),
                        source.expectedLibraryUri(),
                        source.expectedTargetRoot(),
                        source.expectedTargetKind()),
                fixture.frameworkFile()));
        assertAnalyzedRejected(analyze(
                fixture, fixture.current(), extra),
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void narrowsOnlyTheScannerProbeWhenGivenARealFlutterSdkHome()
            throws Exception {
        Fixture fixture = fixture();
        Path sdkHome = fixture.flutterLib().getParent().getParent().getParent();

        List<DartSymbolProbe> sdkProbes = GeneratedDartSymbolProbePlanner.plan(
                fixture.prepared(), sdkHome, fixture.projectRoot());
        DartSymbolProbe superclass = sdkProbes.stream()
                .filter(probe -> probe.id().equals(
                        GeneratedDartSymbolProbePlanner
                                .DESIGNER_SUPERCLASS_PROBE_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(fixture.flutterLib().toRealPath(),
                superclass.expectedTargetRoot());
        assertTrue(sdkProbes.stream()
                .filter(probe -> !probe.id().equals(
                        GeneratedDartSymbolProbePlanner
                                .DESIGNER_SUPERCLASS_PROBE_ID))
                .allMatch(probe -> probe.expectedTargetRoot()
                        .equals(sdkHome.toAbsolutePath().normalize())));
        assertTrue(GeneratedDartSymbolProbePlanner.plan(
                fixture.prepared(), fixture.flutterLib(), fixture.projectRoot()).stream()
                .allMatch(probe -> probe.expectedTargetRoot()
                        .equals(fixture.flutterLib().toAbsolutePath().normalize())));
    }

    @Test
    void ordinaryCandidateDoesNotRequireAProjectLibDirectory()
            throws Exception {
        Fixture fixture = fixture(Optional.empty(), false);
        assertFalse(Files.exists(fixture.projectRoot().resolve("lib")));

        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());

        assertTrue(ticket.request().symbolProbes().stream()
                .noneMatch(PairSaveEvidenceGateTest::isProjectProbe));
        assertTrue(ticket.accept(analysis(
                ticket, fixture.acceptedEvidence())).ready());
    }

    @Test
    void refreshFunctionProofLibraryCoversMixedWidgetReferencesWithoutChangingNavigation() throws Exception {
        for (String property : List.of("onRefresh", "notificationPredicate", "onStatusChange")) {
            for (boolean imported : List.of(false, true)) {
                var reference = new PropertyValue.DartObjectReferenceValue(
                        imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                        "configuredFunction", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
                Fixture fixture = fixture(Optional.of(reference), true, List.of(),
                        "flutter.material.RefreshIndicator", property);
                PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
                var typed = ticket.request().symbolProbes().stream()
                        .flatMap(probe -> probe.staticTypeProbe().stream()).toList();
                assertEquals(3, typed.size());
                String library = property.equals("notificationPredicate")
                        ? "package:flutter/widgets.dart" : "package:flutter/material.dart";
                assertEquals(List.of(library), typed.stream().map(value -> value.expectedTypeLibraryUri()).distinct().toList());
                assertTrue(typed.stream().anyMatch(value -> value.expectedDartType().equals("Animation<Color?>")));
                assertTrue(typed.stream().anyMatch(value -> value.expectedDartType().equals("CustomClipper<RRect>")));
                DartSymbolProbe configured = ticket.request().symbolProbes().stream()
                        .filter(probe -> probe.expectedSymbolName().equals("configuredFunction")).findFirst().orElseThrow();
                assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current",
                        configured.expectedLibraryUri());
                assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), configured.expectedTargetRoot());
                PairAnalyzedCandidateResult analyzed = ticket.accept(analysis(ticket, fixture.acceptedEvidence()));
                assertTrue(analyzed.ready(), () -> analyzed.diagnostics().toString());
                assertTrue(PairSaveEvidenceGate.bindApplied(analyzed.analyzedOptional().orElseThrow(), fixture.live()).ready());
            }
        }
    }

    @Test
    void bindsClipPathBranchesWithExactGeometryAndStaticHelperEvidence()
            throws Exception {
        for (String property : List.of("clipper", "shape")) {
            for (boolean imported : List.of(false, true)) {
                String expectedType = property.equals("shape")
                        ? "ShapeBorder" : "CustomClipper<Path>";
                PropertyValue.DartObjectReferenceValue reference =
                        new PropertyValue.DartObjectReferenceValue(
                                imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME
                                        + "/clippers.dart") : Optional.empty(),
                                "Geometry", Optional.of("configured"),
                                PropertyValue.DartObjectReferenceValue.Access
                                        .ZERO_ARGUMENT_INVOCATION,
                                Optional.of(false));
                Fixture fixture = fixture(Optional.of(reference), true, List.of(),
                        "flutter.widgets.ClipPath", property);
                PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
                List<DartSymbolProbe> geometryProbes = ticket.request().symbolProbes()
                        .stream().filter(PairSaveEvidenceGateTest::isProjectProbe).toList();
                assertEquals(List.of("Geometry", "configured"), geometryProbes.stream()
                        .map(DartSymbolProbe::expectedSymbolName).toList());
                assertEquals(expectedType, geometryProbes.getLast()
                        .staticTypeProbe().orElseThrow().expectedDartType());
                List<DartSymbolProbe> helperProbes = ticket.request().symbolProbes().stream()
                        .filter(probe -> probe.expectedSymbolName().equals("shape")).toList();
                assertEquals(property.equals("shape") ? 1 : 0, helperProbes.size());
                for (DartSymbolProbe helper : helperProbes) {
                    assertEquals("package:flutter/widgets.dart", helper.expectedLibraryUri());
                    assertEquals(fixture.flutterLib().toRealPath(), helper.expectedTargetRoot());
                    assertTrue(helper.staticTypeProbe().isEmpty());
                }
                PairAnalyzedCandidateResult analyzed = ticket.accept(analysis(
                        ticket, fixture.acceptedEvidence()));
                assertTrue(analyzed.ready(), () -> analyzed.diagnostics().toString());
                PairSaveEvidenceResult bound = PairSaveEvidenceGate.bindApplied(
                        analyzed.analyzedOptional().orElseThrow(), fixture.live());
                assertTrue(bound.ready(), () -> bound.diagnostics().toString());
                assertArrayEquals(fixture.prepared().prospectiveDartBytes(),
                        bound.evidenceOptional().orElseThrow().candidateDartBytes());
            }
        }
    }

    @Test
    void rejectsStaleWidgetsProofLibraryForMaterialCallbackEvidence() throws Exception {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "configuredFunction",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.RefreshIndicator", "onStatusChange");
        PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
        DartSymbolEvidence original = fixture.acceptedEvidence().stream()
                .filter(value -> value.probe().staticTypeProbe().isPresent()).findFirst().orElseThrow();
        DartSymbolProbe probe = original.probe();
        var type = probe.staticTypeProbe().orElseThrow();
        assertEquals("package:flutter/material.dart", type.expectedTypeLibraryUri());
        var staleType = new dev.flutter.netbeans.dart.DartStaticTypeProbe(type.expressionOffset(), type.expressionLength(),
                type.importInsertionOffset(), type.statementInsertionOffset(), type.expectedDartType(), "package:flutter/widgets.dart");
        var staleProbe = new DartSymbolProbe(probe.id(), probe.offset(), probe.length(), probe.expectedSymbolName(),
                probe.expectedLibraryUri(), probe.expectedTargetRoot(), probe.expectedTargetKind(), Optional.of(staleType));
        var staleEvidence = new DartSymbolEvidence(staleProbe, original.targets(), true, Optional.empty(),
                Optional.of(new DartStaticTypeEvidence(staleType, true, Optional.empty())));
        var evidence = fixture.acceptedEvidence().stream().map(value -> value == original ? staleEvidence : value).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)),
                PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void bindsClipRSuperellipseWithExactTypedClipperEvidence() throws Exception {
        for (boolean imported : List.of(false, true)) {
            for (boolean constant : List.of(false, true)) {
                var reference = new PropertyValue.DartObjectReferenceValue(
                        imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME
                                + "/clippers.dart") : Optional.empty(),
                        "SuperellipseClipper", Optional.of("configured"),
                        PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                        Optional.of(constant));
                Fixture fixture = fixture(Optional.of(reference), true, List.of(),
                        "flutter.widgets.ClipRSuperellipse", "clipper");
                PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
                List<DartSymbolProbe> geometry = ticket.request().symbolProbes().stream()
                        .filter(PairSaveEvidenceGateTest::isProjectProbe).toList();
                assertEquals(List.of("SuperellipseClipper", "configured"), geometry.stream()
                        .map(DartSymbolProbe::expectedSymbolName).toList());
                assertEquals("CustomClipper<RSuperellipse>", geometry.getLast()
                        .staticTypeProbe().orElseThrow().expectedDartType());
                assertTrue(ticket.request().symbolProbes().stream().anyMatch(probe ->
                        probe.expectedSymbolName().equals("ClipRSuperellipse")
                        && probe.expectedLibraryUri().equals("package:flutter/widgets.dart")));
                String dart = new String(fixture.prepared().prospectiveDartBytes(),
                        StandardCharsets.UTF_8);
                assertEquals(constant, dart.contains("return const ClipRSuperellipse("), dart);
                PairAnalyzedCandidateResult analyzed = ticket.accept(analysis(
                        ticket, fixture.acceptedEvidence()));
                assertTrue(analyzed.ready(), () -> analyzed.diagnostics().toString());
                PairSaveEvidenceResult bound = PairSaveEvidenceGate.bindApplied(
                        analyzed.analyzedOptional().orElseThrow(), fixture.live());
                assertTrue(bound.ready(), () -> bound.diagnostics().toString());
                assertArrayEquals(fixture.prepared().prospectiveDartBytes(),
                        bound.evidenceOptional().orElseThrow().candidateDartBytes());
            }
        }
    }

    @Test
    void rejectsClipPathShapeWhenItsStaticTypeEvidenceIsAbsent() throws Exception {
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "selectedShape", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())), true, List.of(),
                "flutter.widgets.ClipPath", "shape");
        PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
        List<DartSymbolEvidence> withoutType = fixture.acceptedEvidence().stream()
                .map(evidence -> evidence.probe().staticTypeProbe().isPresent()
                        ? new DartSymbolEvidence(evidence.probe(), evidence.targets(),
                                false, Optional.of("Required ShapeBorder type evidence is absent"),
                                Optional.empty()) : evidence)
                .toList();
        assertThrows(IllegalArgumentException.class, () -> analysis(ticket, withoutType),
                "A PASSED analyzer result cannot omit required static-type evidence");
        PairAnalyzedCandidateResult rejected = ticket.accept(
                rejectedAnalysis(ticket, List.of(), withoutType));
        assertAnalyzedRejected(rejected,
                PairSaveEvidenceDiagnostic.Code.INCOMPLETE_STATIC_TYPE_EVIDENCE);
    }

    @Test
    void acceptsCurrentLibraryReferenceRootAndMemberOnlyWithinProjectLib()
            throws Exception {
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "_clipperRegistry",
                        Optional.of("rounded"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())));
        Files.delete(fixture.projectRoot().resolve(
                ".dart_tool/package_config.json"));
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());

        List<DartSymbolProbe> projectProbes = ticket.request().symbolProbes()
                .stream()
                .filter(PairSaveEvidenceGateTest::isProjectProbe)
                .toList();
        assertEquals(List.of("_clipperRegistry", "rounded"), projectProbes
                .stream().map(DartSymbolProbe::expectedSymbolName).toList());
        assertTrue(projectProbes.stream().allMatch(probe ->
                probe.expectedLibraryUri().equals(
                        DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI)));
        Path projectLibraryReal = fixture.projectRoot().resolve("lib")
                .toRealPath();
        assertTrue(projectProbes.stream().allMatch(probe ->
                probe.expectedTargetRoot().equals(projectLibraryReal)));
        assertTrue(ticket.accept(analysis(
                ticket, fixture.acceptedEvidence())).ready());
    }

    @Test
    void acceptsImportedRootAndMemberForTheOwningProjectPackage()
            throws Exception {
        String library = "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart";
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "RoundedClipperFactory",
                        Optional.of("compact"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true))));
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());

        List<DartSymbolProbe> projectProbes = ticket.request().symbolProbes()
                .stream()
                .filter(PairSaveEvidenceGateTest::isProjectProbe)
                .toList();
        assertEquals(List.of("RoundedClipperFactory", "compact"), projectProbes
                .stream().map(DartSymbolProbe::expectedSymbolName).toList());
        assertTrue(projectProbes.stream().allMatch(probe ->
                probe.expectedLibraryUri().equals(library)));
        assertTrue(ticket.accept(analysis(
                ticket, fixture.acceptedEvidence())).ready());

        Files.writeString(fixture.projectRoot().resolve("pubspec.yaml"),
                "name: renamed_fixture\n", StandardCharsets.UTF_8);
        IOException prepareMismatch = assertThrows(IOException.class,
                () -> ticket(fixture, fixture.current()));
        assertTrue(prepareMismatch.getMessage().contains(
                "self-package library roots disagree"));
    }

    @Test
    void acceptsDeclaredDependencyAtItsExactConfiguredLibraryRoot()
            throws Exception {
        Path dependencyRoot = Files.createDirectories(
                temporaryDirectory.resolve("clipper-dependency"));
        Path dependencyLibrary = Files.createDirectories(
                dependencyRoot.resolve("lib"));
        Files.writeString(dependencyLibrary.resolve("clippers.dart"),
                "class DependencyClipper {}\n", StandardCharsets.UTF_8);
        String library = "package:clipper_dependency/clippers.dart";
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "DependencyClipper",
                        Optional.of("rounded"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())), true, List.of(
                                new DeclaredPackage("clipper_dependency",
                                        dependencyRoot, "lib/"),
                                new DeclaredPackage("stale_unrelated",
                                        temporaryDirectory.resolve(
                                                "removed-dependency"),
                                        "lib/")));

        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        List<DartSymbolProbe> dependencyProbes = ticket.request().symbolProbes()
                .stream()
                .filter(probe -> probe.expectedLibraryUri().equals(library))
                .toList();
        assertEquals(List.of("DependencyClipper", "rounded"), dependencyProbes
                .stream().map(DartSymbolProbe::expectedSymbolName).toList());
        Path dependencyLibraryReal = dependencyLibrary.toRealPath();
        assertTrue(dependencyProbes.stream().allMatch(probe ->
                probe.expectedTargetRoot().equals(dependencyLibraryReal)));
        assertTrue(ticket.accept(analysis(
                ticket, fixture.acceptedEvidence())).ready());

        Path wrongDeclaredRootTarget = fixture.projectRoot().resolve(
                "lib/clippers.dart");
        List<DartSymbolEvidence> wrongDependencyTargets =
                fixture.acceptedEvidence().stream()
                        .map(value -> value.probe().expectedLibraryUri()
                                        .equals(library)
                                ? accepted(value.probe(),
                                        wrongDeclaredRootTarget)
                                : value)
                        .toList();
        assertAnalyzedRejected(analyze(fixture, fixture.current(),
                        wrongDependencyTargets),
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
    }

    @Test
    void rejectsUndeclaredPackageAndEscapingPackageLibraryRoot()
            throws Exception {
        IllegalArgumentException undeclared = assertThrows(
                IllegalArgumentException.class,
                () -> fixture(Optional.of(
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of("package:undeclared/clippers.dart"),
                                "ExternalClipper", Optional.empty(),
                                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                                Optional.empty()))));
        assertTrue(undeclared.getMessage().contains(
                "is not declared by the trusted project"));

        Path dependencyRoot = Files.createDirectories(
                temporaryDirectory.resolve("escaping-dependency"));
        Files.createDirectories(dependencyRoot.resolve("lib"));
        Files.createDirectories(temporaryDirectory.resolve("escaped-library"));
        IllegalArgumentException escaped = assertThrows(
                IllegalArgumentException.class,
                () -> fixture(Optional.of(
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of("package:escaping/clippers.dart"),
                                "EscapingClipper", Optional.empty(),
                                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                                Optional.empty())), true, List.of(
                                        new DeclaredPackage("escaping",
                                                dependencyRoot,
                                                "../escaped-library/"))));
        assertTrue(escaped.getMessage().contains("packageUri escapes"));
    }

    @Test
    void rejectsTrailingAndNonUtf8PackageConfigDocuments() throws Exception {
        String library = "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart";
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "ProjectClipper",
                        Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())));
        Path config = fixture.projectRoot().resolve(
                ".dart_tool/package_config.json");
        String canonical = Files.readString(config, StandardCharsets.UTF_8);
        Files.writeString(config,
                canonical + "\n{}\n",
                StandardCharsets.UTF_8);

        IOException trailing = assertThrows(IOException.class,
                () -> ticket(fixture, fixture.current()));
        assertTrue(trailing.getMessage().contains(
                "package_config.json cannot be resolved and read"));

        Files.write(config, ("\ufeff" + canonical).getBytes(
                StandardCharsets.UTF_16LE));
        IOException nonUtf8 = assertThrows(IOException.class,
                () -> ticket(fixture, fixture.current()));
        assertTrue(nonUtf8.getMessage().contains(
                "package_config.json is not valid strict UTF-8"));
    }

    @Test
    void rejectsPackageUriEscapeThroughAnIntermediateDirectoryLink()
            throws Exception {
        Path dependencyRoot = Files.createDirectories(
                temporaryDirectory.resolve("linked-dependency"));
        Path dependencyLibrary = Files.createDirectories(
                dependencyRoot.resolve("lib"));
        Path externalRoot = Files.createDirectories(
                temporaryDirectory.resolve("outside-dependency"));
        Files.createDirectories(externalRoot.resolve("deep"));
        Path bridge = dependencyLibrary.resolve("bridge");
        try {
            Files.createSymbolicLink(bridge, externalRoot.toAbsolutePath());
        } catch (UnsupportedOperationException | IOException | SecurityException
                unavailable) {
            assumeTrue(false,
                    "directory symlinks are unavailable for this containment proof: "
                    + unavailable);
            return;
        }

        IllegalArgumentException escaped = assertThrows(
                IllegalArgumentException.class,
                () -> fixture(Optional.of(
                        new PropertyValue.DartObjectReferenceValue(
                                Optional.of("package:linked/clippers.dart"),
                                "LinkedClipper", Optional.empty(),
                                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                                Optional.empty())), true, List.of(
                                        new DeclaredPackage("linked",
                                                dependencyRoot,
                                                "lib/bridge/deep/"))));
        assertTrue(escaped.getMessage().contains(
                "packageUri escapes through a link"));
    }

    @Test
    void revalidatesDeclaredPackageResolutionWhenBindingTheLiveCandidate()
            throws Exception {
        Path dependencyRoot = Files.createDirectories(
                temporaryDirectory.resolve("original-dependency"));
        Path dependencyLibrary = Files.createDirectories(
                dependencyRoot.resolve("lib"));
        Files.writeString(dependencyLibrary.resolve("clippers.dart"),
                "class DependencyClipper {}\n", StandardCharsets.UTF_8);
        String library = "package:clipper_dependency/clippers.dart";
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "DependencyClipper",
                        Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())), true, List.of(new DeclaredPackage(
                                "clipper_dependency", dependencyRoot, "lib/")));
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        PairAnalyzedCandidate analyzed = ticket.accept(analysis(
                ticket, fixture.acceptedEvidence()))
                .analyzedOptional().orElseThrow();

        Path replacementRoot = Files.createDirectories(
                temporaryDirectory.resolve("replacement-dependency"));
        Path replacementLibrary = Files.createDirectories(
                replacementRoot.resolve("lib"));
        Files.writeString(replacementLibrary.resolve("clippers.dart"),
                "class DependencyClipper {}\n", StandardCharsets.UTF_8);
        writePackageConfig(fixture.projectRoot(), List.of(new DeclaredPackage(
                "clipper_dependency", replacementRoot, "lib/")));

        PairSaveEvidenceResult rebound = PairSaveEvidenceGate.bindApplied(
                analyzed, fixture.live());
        assertFalse(rebound.ready());
        assertTrue(rebound.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH));
    }

    @Test
    void rejectsNavigationTargetDeletedAfterAnalysisBeforeBinding()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        PairAnalyzedCandidate analyzed = ticket.accept(analysis(
                ticket, fixture.acceptedEvidence()))
                .analyzedOptional().orElseThrow();

        Files.delete(fixture.frameworkFile());

        PairSaveEvidenceResult rebound = PairSaveEvidenceGate.bindApplied(
                analyzed, fixture.live());
        assertFalse(rebound.ready());
        assertTrue(rebound.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == PairSaveEvidenceDiagnostic.Code
                        .PATH_VALIDATION_FAILED
                && diagnostic.subject().endsWith(".target")));
    }

    @Test
    void rejectsNavigationTargetRedirectedOutsideTrustedRootBeforeBinding()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        PairAnalyzedCandidate analyzed = ticket.accept(analysis(
                ticket, fixture.acceptedEvidence()))
                .analyzedOptional().orElseThrow();
        Path outside = temporaryDirectory.resolve("outside-framework.dart");
        Files.writeString(outside, "class Widget {}\n", StandardCharsets.UTF_8);
        Files.delete(fixture.frameworkFile());
        try {
            Files.createSymbolicLink(
                    fixture.frameworkFile(), outside.toAbsolutePath());
        } catch (UnsupportedOperationException | IOException | SecurityException
                unavailable) {
            assumeTrue(false,
                    "file symlinks are unavailable for this bind-time provenance proof: "
                    + unavailable);
            return;
        }

        PairSaveEvidenceResult rebound = PairSaveEvidenceGate.bindApplied(
                analyzed, fixture.live());
        assertFalse(rebound.ready());
        assertTrue(rebound.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == PairSaveEvidenceDiagnostic.Code
                        .UNTRUSTED_NAVIGATION_TARGET
                && diagnostic.subject().endsWith(".target")));
    }

    @Test
    void rejectsProjectReferenceNavigationTargetOutsideProjectLib()
            throws Exception {
        String library = "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart";
        Fixture fixture = fixture(Optional.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of(library), "RoundedClipper",
                        Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())));
        Path externalTarget = temporaryDirectory.resolve(
                "pub-cache/external/clippers.dart");
        Files.createDirectories(externalTarget.getParent());
        Files.writeString(externalTarget, "class RoundedClipper {}\n",
                StandardCharsets.UTF_8);
        List<DartSymbolEvidence> escaped = fixture.acceptedEvidence().stream()
                .map(value -> isProjectProbe(value.probe())
                        ? accepted(value.probe(), externalTarget)
                        : value)
                .toList();

        PairAnalyzedCandidateResult result = analyze(
                fixture, fixture.current(), escaped);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
    }

    @Test
    void plannerAccepts255GeneratedPlusOneScannerAtExactCapacity()
            throws Exception {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "planner-probe-exact", 2 * 1024 * 1024, 256, 1);
        DartGenerationLimits generationLimits = new DartGenerationLimits(
                DartGenerationLimits.DEFAULT_MAX_TOTAL_PAYLOAD_UTF8_BYTES,
                DartGenerationLimits.DEFAULT_MAX_IMPORTS,
                DartGenerationLimits.DEFAULT_MAX_VALUE_CODE_POINTS,
                capacity);
        DartRegionGenerator generator = new DartRegionGenerator(generationLimits);

        DartSourceDescriptor seed = descriptor("", "");
        GeneratedDartRegions generatedBefore = generator.generate(
                boundaryDocument(seed, "before"),
                BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
        DartSourceDescriptor baselineDescriptor = descriptor(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        DesignerDocument baselineDocument = boundaryDocument(
                baselineDescriptor, "before");
        DartGenerationResult baselineGeneration = generator.generate(
                baselineDocument, BuiltInWidgetCatalog.getDefault());
        byte[] baselineSource = sourceBytes(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult source = scanner.scan(
                baselineSource, baselineDescriptor);
        DartThreeWayIntegrityResult baseline = new DartThreeWayIntegrityGate(scanner)
                .evaluate(source, baselineDescriptor, baselineGeneration);
        DartSourceTransitionPlan transition = new DartSourceTransitionPlanner(scanner)
                .plan(
                        baseline,
                        baselineSource,
                        baselineDescriptor,
                        generator.generate(
                                boundaryDocument(baselineDescriptor, "after"),
                                BuiltInWidgetCatalog.getDefault()))
                .plan().orElseThrow();
        FdDocumentCodec codec = new FdDocumentCodec();
        FdDecodeResult.Current decoded = (FdDecodeResult.Current) codec.decode(
                codec.encode(baselineDocument));
        PreparedDesignerPair prepared = new DesignerPairPreparationPlanner()
                .prepare(
                        decoded.original(),
                        boundaryDocument(
                                transition.prospectiveDescriptor(), "after"),
                        transition)
                .preparedPair().orElseThrow();

        Path plannerProject = Files.createDirectories(
                temporaryDirectory.resolve("planner-project/lib")).getParent();
        List<DartSymbolProbe> probes = GeneratedDartSymbolProbePlanner.plan(
                prepared, temporaryDirectory.toAbsolutePath(), plannerProject);

        GeneratedDartRegions generated = prepared.dartTransition().generation()
                .generated().orElseThrow();
        assertSame(capacity, generated.candidateCapacityBudget());
        assertEquals(255, generated.symbolOccurrences().size());
        assertEquals(256, probes.size());
        assertEquals(1, probes.stream().filter(probe -> probe.id().equals(
                GeneratedDartSymbolProbePlanner.DESIGNER_SUPERCLASS_PROBE_ID))
                .count());
    }

    @Test
    void ticketRetainsExactGeneratedCapacityAndRejectsDetachedEqualPolicy()
            throws Exception {
        Fixture fixture = fixture();
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateCapacityBudget capacity = fixture.prepared()
                .dartTransition().generation().generated().orElseThrow()
                .candidateCapacityBudget();
        int generatedFlutterOccurrences = (int) fixture.prepared()
                .dartTransition().generation().generated().orElseThrow()
                .symbolOccurrences().stream()
                .filter(occurrence -> occurrence.libraryUri()
                        .startsWith("package:flutter/"))
                .count();

        assertSame(capacity, ticket.capacityBudgetIdentity());
        assertSame(capacity, ticket.request().candidateCapacityBudget());
        assertEquals(1 + generatedFlutterOccurrences,
                ticket.request().symbolProbes().size());
        assertTrue(ticket.request().symbolProbes().size()
                <= capacity.maxSymbolProbes());

        DartCandidateCapacityBudget detached = new DartCandidateCapacityBudget(
                capacity.profileId(),
                capacity.maxCandidateUtf8Bytes(),
                capacity.maxSymbolProbes(),
                capacity.reservedSourceSymbolProbes());
        DartCandidateAnalysisRequest request = ticket.request();
        DartCandidateAnalysisRequest substituted =
                new DartCandidateAnalysisRequest(
                        request.projectRoot(),
                        request.dartFile(),
                        request.content(),
                        request.version(),
                        request.sha256(),
                        request.warningPolicy(),
                        request.symbolProbes(),
                        detached);

        assertThrows(IllegalArgumentException.class,
                () -> new PairCandidateAnalysisTicket(
                        fixture.current(),
                        fixture.prepared(),
                        substituted,
                        ticket.trustedFlutterSdkRealRoot(),
                        ticket.realProjectRoot(),
                        ticket.realDartPath()));
    }

    @Test
    void rejectsPackageFlutterPrefixSubstitution() throws Exception {
        Fixture fixture = fixture();
        List<DartSymbolEvidence> malicious = evidence(
                fixture,
                fixture.flutterLib(),
                fixture.frameworkFile(),
                "package:flutter_evil/widgets.dart");
        PairCandidateAnalysisTicket maliciousTicket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult analysis = analysis(
                maliciousTicket,
                maliciousTicket.request().version(),
                maliciousTicket.request().sha256(),
                fixture.prepared().prospectiveDartBytes().length,
                fixture.dartFile(),
                malicious);
        PairAnalyzedCandidateResult result = maliciousTicket.accept(analysis);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code
                        .GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void rejectsARealProbeRootOutsideTheTrustedFlutterLibrary()
            throws Exception {
        Fixture fixture = fixture();
        Path maliciousRoot = Files.createDirectories(
                fixture.flutterLib().getParent().resolve("lib-evil"));
        Path maliciousTarget = maliciousRoot.resolve("src/widgets/framework.dart");
        Files.createDirectories(maliciousTarget.getParent());
        Files.writeString(maliciousTarget, "class Widget {}\n",
                StandardCharsets.UTF_8);
        List<DartSymbolEvidence> malicious = evidence(
                fixture,
                maliciousRoot,
                maliciousTarget,
                "package:flutter/widgets.dart");
        PairCandidateAnalysisTicket maliciousTicket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult analysis = analysis(
                maliciousTicket,
                maliciousTicket.request().version(),
                maliciousTicket.request().sha256(),
                fixture.prepared().prospectiveDartBytes().length,
                fixture.dartFile(),
                malicious);

        PairAnalyzedCandidateResult result = maliciousTicket.accept(analysis);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
    }

    @Test
    void rejectsAnalyzerEvidenceForAnotherRealDartPath() throws Exception {
        Fixture fixture = fixture();
        Path other = temporaryDirectory.resolve("other-project/lib/home_page.dart");
        Files.createDirectories(other.getParent());
        Files.write(other, fixture.prepared().prospectiveDartBytes());
        PairCandidateAnalysisTicket ticket = ticket(
                fixture, fixture.current());
        DartCandidateAnalysisResult substituted = analysis(
                ticket,
                ticket.request().version(),
                ticket.request().sha256(),
                fixture.prepared().prospectiveDartBytes().length,
                other,
                fixture.acceptedEvidence());

        PairAnalyzedCandidateResult result = ticket.accept(substituted);

        assertAnalyzedRejected(result,
                PairSaveEvidenceDiagnostic.Code.ANALYSIS_DART_PATH_MISMATCH);
    }

    private PairCandidateAnalysisTicket ticket(
            Fixture fixture,
            FlutterDesignerDocumentState.Current current) throws IOException {
        return PairSaveEvidenceGate.prepareAnalysis(
                current,
                fixture.prepared(),
                fixture.projectRoot(),
                fixture.dartFile(),
                DartCandidateWarningPolicy.ALLOW,
                fixture.flutterLib());
    }

    private PairAnalyzedCandidateResult analyze(
            Fixture fixture,
            FlutterDesignerDocumentState.Current current,
            List<DartSymbolEvidence> evidence) throws IOException {
        PairCandidateAnalysisTicket ticket = ticket(fixture, current);
        return ticket.accept(analysis(ticket, evidence));
    }

    private Fixture fixture() throws Exception {
        return fixture(Optional.empty());
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference)
            throws Exception {
        return fixture(projectReference, true);
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            boolean createProjectLibrary) throws Exception {
        return fixture(projectReference, createProjectLibrary, List.of());
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            boolean createProjectLibrary,
            List<DeclaredPackage> declaredPackages) throws Exception {
        return fixture(projectReference, createProjectLibrary, declaredPackages,
                "flutter.widgets.ClipRRect", "clipper");
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            boolean createProjectLibrary,
            List<DeclaredPackage> declaredPackages,
            String widgetType,
            String propertyName) throws Exception {
        Path projectRoot = Files.createDirectories(
                temporaryDirectory.resolve("project"));
        Files.writeString(projectRoot.resolve("pubspec.yaml"),
                "name: " + PROJECT_PACKAGE_NAME + "\n",
                StandardCharsets.UTF_8);
        Path dartFile = createProjectLibrary
                ? projectRoot.resolve("lib/home_page.dart")
                : projectRoot.resolve("home_page.dart");
        Files.createDirectories(dartFile.getParent());
        if (createProjectLibrary) {
            Files.writeString(projectRoot.resolve("lib/clippers.dart"),
                    "class ProjectClipper {}\n", StandardCharsets.UTF_8);
            writePackageConfig(projectRoot, declaredPackages);
        }

        Path flutterLib = Files.createDirectories(
                temporaryDirectory.resolve("flutter/packages/flutter/lib"));
        Path framework = flutterLib.resolve("src/widgets/framework.dart");
        Files.createDirectories(framework.getParent());
        Files.writeString(framework,
                "abstract class Widget {}\n"
                + "abstract class StatelessWidget extends Widget {}\n"
                + "class BuildContext {}\n",
                StandardCharsets.UTF_8);

        DartRegionGenerator generator = new DartRegionGenerator();
        DartSourceDescriptor seedDescriptor = descriptor("", "");
        GeneratedDartRegions generatedBefore = generator.generate(
                document(seedDescriptor, "before"),
                BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
        DartSourceDescriptor baselineDescriptor = descriptor(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        DesignerDocument baselineDocument = document(
                baselineDescriptor, "before");
        DartGenerationResult baselineGeneration = generator.generate(
                baselineDocument, BuiltInWidgetCatalog.getDefault());
        byte[] baselineSource = sourceBytes(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload());
        Files.write(dartFile, baselineSource);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult sourceIntegrity = scanner.scan(
                baselineSource, baselineDescriptor);
        DartThreeWayIntegrityResult baseline =
                new DartThreeWayIntegrityGate(scanner).evaluate(
                        sourceIntegrity,
                        baselineDescriptor,
                        baselineGeneration);

        FdDocumentCodec codec = new FdDocumentCodec();
        FdDecodeResult.Current decoded = (FdDecodeResult.Current) codec.decode(
                codec.encode(baselineDocument));
        FlutterDesignerDocumentState.Current current = current(decoded, baseline);

        byte[] dirtyLive = ("// user-owned Привіт 😀\n"
                + new String(baselineSource, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        DartSourceTransitionPlan transition = new DartSourceTransitionPlanner(scanner)
                .plan(
                        baseline,
                        dirtyLive,
                        baselineDescriptor,
                        generator.generate(
                                prospectiveDocument(
                                        baselineDescriptor,
                                        "after",
                                        projectReference, widgetType, propertyName),
                                BuiltInWidgetCatalog.getDefault()))
                .plan()
                .orElseThrow();
        DesignerDocument prospective = prospectiveDocument(
                transition.prospectiveDescriptor(), "after", projectReference,
                widgetType, propertyName);
        PreparedDesignerPair prepared = new DesignerPairPreparationPlanner()
                .prepare(decoded.original(), prospective, transition)
                .preparedPair()
                .orElseThrow();

        Loaded loaded = load(prepared.prospectiveDartBytes());
        LiveDartDocumentSnapshot live = LiveDartDocumentBridge.snapshot(
                loaded.document(), loaded.provider());
        Fixture withoutAnalysis = new Fixture(
                current,
                prepared,
                live,
                projectRoot,
                dartFile,
                flutterLib,
                framework,
                List.of());
        List<DartSymbolEvidence> evidence = exactEvidence(withoutAnalysis);
        return new Fixture(
                current,
                prepared,
                live,
                projectRoot,
                dartFile,
                flutterLib,
                framework,
                evidence);
    }

    private static FlutterDesignerDocumentState.Current current(
            FdDecodeResult.Current decoded,
            DartThreeWayIntegrityResult baseline) {
        return new FlutterDesignerDocumentState.Current(
                decoded,
                new ValidationResult(List.of()),
                BuiltInWidgetCatalog.getDefault(),
                List.of(),
                List.of(),
                Optional.of(baseline.source()),
                Optional.of(baseline));
    }

    private static List<DartSymbolEvidence> evidence(
            Fixture fixture,
            Path expectedRoot,
            Path target,
            String libraryUri) {
        return GeneratedDartSymbolProbePlanner.plan(
                fixture.prepared(), expectedRoot.toAbsolutePath(),
                fixture.projectRoot()).stream()
                .map(planned -> accepted(
                        new DartSymbolProbe(
                                planned.id(),
                                planned.offset(),
                                planned.length(),
                                planned.expectedSymbolName(),
                                libraryUri,
                                expectedRoot,
                                planned.expectedTargetKind()),
                        target))
                .toList();
    }

    private static List<DartSymbolEvidence> exactEvidence(Fixture fixture) {
        return GeneratedDartSymbolProbePlanner.plan(
                fixture.prepared(), fixture.flutterLib(),
                fixture.projectRoot()).stream()
                .map(probe -> accepted(
                        probe,
                        isProjectProbe(probe)
                                ? DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI.equals(
                                        probe.expectedLibraryUri())
                                        ? fixture.dartFile()
                                        : probe.expectedTargetRoot().resolve(
                                                "clippers.dart")
                                : fixture.frameworkFile()))
                .toList();
    }

    private static void writePackageConfig(
            Path projectRoot,
            List<DeclaredPackage> declaredPackages) throws IOException {
        ObjectNode root = JSON.createObjectNode();
        root.put("configVersion", 2);
        ArrayNode packages = root.putArray("packages");
        addDeclaredPackage(packages, PROJECT_PACKAGE_NAME, projectRoot, "lib/");
        for (DeclaredPackage declaredPackage : declaredPackages) {
            addDeclaredPackage(packages,
                    declaredPackage.name(),
                    declaredPackage.root(),
                    declaredPackage.packageUri());
        }
        Path config = projectRoot.resolve(".dart_tool/package_config.json");
        Files.createDirectories(config.getParent());
        JSON.writeValue(config.toFile(), root);
    }

    private static void addDeclaredPackage(
            ArrayNode packages,
            String name,
            Path root,
            String packageUri) {
        ObjectNode entry = packages.addObject();
        entry.put("name", name);
        entry.put("rootUri", root.toAbsolutePath().normalize().toUri().toString());
        entry.put("packageUri", packageUri);
    }

    private static boolean isProjectProbe(DartSymbolProbe probe) {
        return DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI.equals(
                probe.expectedLibraryUri())
                || !probe.expectedLibraryUri().startsWith("package:flutter/");
    }

    private static DartSymbolEvidence accepted(
            DartSymbolProbe probe,
            Path target) {
        DartNavigationTarget navigation = new DartNavigationTarget(
                "CLASS", target, 0, 1, 1, 1);
        return new DartSymbolEvidence(
                probe,
                List.of(navigation),
                true,
                Optional.empty(),
                probe.staticTypeProbe().map(staticType ->
                        new DartStaticTypeEvidence(
                                staticType, true, Optional.empty())));
    }

    private static DartCandidateAnalysisResult analysis(
            PairCandidateAnalysisTicket ticket,
            long version,
            String sha,
            int size,
            Path dartFile,
            List<DartSymbolEvidence> evidence) throws IOException {
        Path projectRoot = dartFile.toAbsolutePath().normalize()
                .equals(ticket.realDartPath())
                ? ticket.realProjectRoot()
                : dartFile.getParent().getParent().toAbsolutePath().normalize();
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.PASSED,
                new DartCandidateSnapshot(
                        projectRoot.toAbsolutePath(),
                        dartFile.toAbsolutePath(),
                        version,
                        sha,
                        size),
                Optional.of("1.40.1"),
                List.of(),
                evidence.size(),
                evidence,
                Optional.empty());
    }

    private static DartCandidateAnalysisResult analysis(
            PairCandidateAnalysisTicket ticket,
            List<DartSymbolEvidence> evidence) throws IOException {
        return analysis(
                ticket,
                ticket.request().version(),
                ticket.request().sha256(),
                ticket.request().snapshot().utf8Size(),
                ticket.realDartPath(),
                evidence);
    }

    private PairSaveEvidenceDiagnostic assertConcreteAnalyzerFailure(
            Fixture fixture,
            String code,
            String message,
            String expectedDetail) throws Exception {
        PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
        PairAnalyzedCandidateResult result = ticket.accept(rejectedAnalysis(
                ticket,
                List.of(diagnostic(
                        ticket,
                        DartCandidateDiagnosticSeverity.ERROR,
                        code,
                        message,
                        true,
                        4,
                        5)),
                List.of()));
        PairSaveEvidenceDiagnostic status = result.diagnostics().getFirst();
        assertAll(
                () -> assertEquals(
                        PairSaveEvidenceDiagnostic.Code.ANALYSIS_NOT_PASSED,
                        status.code()),
                () -> assertTrue(status.message().contains("[" + code + "]"),
                        status::message),
                () -> assertTrue(status.message().contains(expectedDetail),
                        status::message),
                () -> assertTrue(status.message().codePointCount(
                        0, status.message().length()) <= 512,
                        status::message));
        return status;
    }

    private static DartCandidateAnalysisResult rejectedAnalysis(
            PairCandidateAnalysisTicket ticket,
            List<DartCandidateDiagnostic> diagnostics,
            List<DartSymbolEvidence> evidence) {
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.REJECTED,
                ticket.request().snapshot(),
                Optional.of("test"),
                diagnostics,
                ticket.request().symbolProbes().size(),
                evidence,
                Optional.empty());
    }

    private static DartCandidateDiagnostic diagnostic(
            PairCandidateAnalysisTicket ticket,
            DartCandidateDiagnosticSeverity severity,
            String code,
            String message,
            boolean blocking,
            int line,
            int column) {
        return new DartCandidateDiagnostic(
                severity,
                severity == DartCandidateDiagnosticSeverity.INFO
                        ? "HINT" : "COMPILE_TIME_ERROR",
                Optional.of(code),
                message,
                Optional.empty(),
                Optional.empty(),
                ticket.realDartPath(),
                0,
                1,
                line,
                column,
                line,
                column + 1,
                blocking);
    }

    private static Loaded load(byte[] source) throws Exception {
        StyledDocument document = (StyledDocument) new DartEditorKit()
                .createDefaultDocument();
        DartGuardedSectionsProvider provider =
                new DartGuardedSectionsProvider(() -> document);
        try (Reader reader = provider.createGuardedReader(
                new ByteArrayInputStream(source), StandardCharsets.UTF_8)) {
            document.insertString(0, readAll(reader), null);
        }
        return new Loaded(document, provider);
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder value = new StringBuilder();
        char[] buffer = new char[512];
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            value.append(buffer, 0, count);
        }
        return value.toString();
    }

    private static DesignerDocument document(
            DartSourceDescriptor descriptor,
            String text) {
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(text)),
                Map.of());
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DesignerDocument prospectiveDocument(
            DartSourceDescriptor descriptor,
            String text,
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            String widgetType,
            String propertyName) {
        if (projectReference.isEmpty()) {
            return document(descriptor, text);
        }
        if (widgetType.equals("flutter.material.RefreshIndicator")) {
            var animation = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "configuredAnimation",
                    Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var clipper = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "configuredClipper",
                    Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            var progress = new WidgetNode(StableId.parse("11111111-1111-4111-8111-111111111111"),
                    new WidgetTypeId("flutter.material.CircularProgressIndicator"),
                    Map.of(new PropertyName("variant"), new PropertyValue.StringValue("material"),
                            new PropertyName("valueColor"), animation), Map.of());
            var clipped = new WidgetNode(StableId.parse("22222222-2222-4222-8222-222222222222"),
                    new WidgetTypeId("flutter.widgets.ClipRRect"), Map.of(new PropertyName("clipper"), clipper),
                    Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(progress))));
            var refresh = new WidgetNode(StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                    new WidgetTypeId(widgetType), Map.of(new PropertyName(propertyName), projectReference.orElseThrow(),
                            new PropertyName("variant"), new PropertyValue.StringValue(
                                    propertyName.equals("onStatusChange") ? "noSpinner" : "material")),
                    Map.of(new SlotName("child"), new WidgetSlot.SingleSlot(Optional.of(clipped))));
            return new DesignerDocument(DOCUMENT_ID, descriptor, refresh);
        }
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId(widgetType),
                Map.of(new PropertyName(propertyName),
                        projectReference.orElseThrow()),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()));
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DesignerDocument boundaryDocument(
            DartSourceDescriptor descriptor,
            String valuePrefix) {
        ArrayList<WidgetNode> children = new ArrayList<>();
        for (int index = 0; index < 252; index++) {
            children.add(new WidgetNode(
                    StableId.parse(
                            "00000000-0000-4000-8000-%012x".formatted(index + 1)),
                    new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(
                            new PropertyName("data"),
                            new PropertyValue.StringValue(
                                    valuePrefix + '-' + index)),
                    Map.of()));
        }
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(
                        new SlotName("children"),
                        new WidgetSlot.ListSlot(children)));
        return new DesignerDocument(DOCUMENT_ID, descriptor, root);
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("test-profile"),
                new ManagedRegions(
                        new ManagedRegion(hash(imports)),
                        new ManagedRegion(hash(build))));
    }

    private static String hash(String payload) {
        return DartManagedRegionHashing.normalizedSha256(payload);
    }

    private static byte[] sourceBytes(String imports, String build) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + build
                + "  // </netbeans-flutter-designer>\n"
                + "}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void assertAnalyzedRejected(
            PairAnalyzedCandidateResult result,
            PairSaveEvidenceDiagnostic.Code code) {
        assertFalse(result.ready());
        assertTrue(result.analyzedOptional().isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == code),
                () -> "Expected " + code + " in " + result.diagnostics());
    }

    private record Loaded(
            StyledDocument document,
            DartGuardedSectionsProvider provider) {
    }

    private record DeclaredPackage(
            String name,
            Path root,
            String packageUri) {
    }

    private record Fixture(
            FlutterDesignerDocumentState.Current current,
            PreparedDesignerPair prepared,
            LiveDartDocumentSnapshot live,
            Path projectRoot,
            Path dartFile,
            Path flutterLib,
            Path frameworkFile,
            List<DartSymbolEvidence> acceptedEvidence) {
    }
}

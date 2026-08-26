package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalysisIssue;
import dev.flutter.netbeans.dart.DartCandidateAnalysisIssueCode;
import dev.flutter.netbeans.dart.DartCandidateAnalysisStatus;
import dev.flutter.netbeans.dart.DartCandidateSnapshot;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
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
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

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
                fixture.prepared(), sdkHome);
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
                fixture.prepared(), fixture.flutterLib()).stream()
                .allMatch(probe -> probe.expectedTargetRoot()
                        .equals(fixture.flutterLib().toAbsolutePath().normalize())));
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

        List<DartSymbolProbe> probes = GeneratedDartSymbolProbePlanner.plan(
                prepared, temporaryDirectory.toAbsolutePath());

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
                PairSaveEvidenceDiagnostic.Code.INVALID_FLUTTER_LIBRARY_URI);
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
        Path projectRoot = Files.createDirectories(
                temporaryDirectory.resolve("project"));
        Path dartFile = projectRoot.resolve("lib/home_page.dart");
        Files.createDirectories(dartFile.getParent());

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
                                document(baselineDescriptor, "after"),
                                BuiltInWidgetCatalog.getDefault()))
                .plan()
                .orElseThrow();
        DesignerDocument prospective = document(
                transition.prospectiveDescriptor(), "after");
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
        List<DartSymbolEvidence> evidence = evidence(
                withoutAnalysis,
                flutterLib,
                framework,
                "package:flutter/widgets.dart");
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
                fixture.prepared(), expectedRoot.toAbsolutePath()).stream()
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

    private static DartSymbolEvidence accepted(
            DartSymbolProbe probe,
            Path target) {
        DartNavigationTarget navigation = new DartNavigationTarget(
                "CLASS", target, 0, 1, 1, 1);
        return new DartSymbolEvidence(
                probe, List.of(navigation), true, Optional.empty());
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

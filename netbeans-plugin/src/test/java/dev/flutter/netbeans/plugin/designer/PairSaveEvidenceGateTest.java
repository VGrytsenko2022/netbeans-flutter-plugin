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
import dev.flutter.netbeans.dart.DartStaticTypeProbe;
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
    @Test void calendarDatePickerDatesRequireExactGeneratedSpansAndTrustedCoreConstructors() throws Exception {
        for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.CalendarDatePicker", "",
                    Map.of(new PropertyName("onDateChanged"), new PropertyValue.StringValue("noop"),
                           new PropertyName("firstDate"), new PropertyValue.StringValue("2000-01-01"),
                           new PropertyName("lastDate"), new PropertyValue.StringValue("2030-12-31"),
                           new PropertyName("initialDate"), new PropertyValue.StringValue("2024-02-29"),
                           new PropertyName("currentDate"), new PropertyValue.StringValue("2024-02-15")));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve(tree).resolve("date_time.dart");
            Files.createDirectories(core.getParent()); Files.writeString(core, "class DateTime {}\n");
            var model = new RadioFixture(fixture, sdk, core);
            java.util.function.Function<PairCandidateAnalysisTicket, List<DartSymbolEvidence>> evidence = ticket ->
                    radioEvidence(model, ticket).stream().map(value -> value.probe().expectedSymbolName().equals("DateTime")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget("CONSTRUCTOR", core, 0, 8, 1, 1)), true, Optional.empty()) : value).toList();
            var ticket = radioTicket(model);
            assertEquals(4, ticket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().equals("DateTime")).count());
            var result = ticket.accept(analysis(ticket, evidence.apply(ticket)));
            assertTrue(result.ready(), result.diagnostics().toString());
            assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.live()).ready());
            for (String mutation : List.of("kind", "file", "missing", "id", "span", "library")) {
                var fresh = radioTicket(model);
                var bad = evidence.apply(fresh).stream().filter(value -> !(mutation.equals("missing") && value.probe().expectedSymbolName().equals("DateTime")))
                        .map(value -> {
                            var original = value.probe();
                            if (!original.expectedSymbolName().equals("DateTime")) return value;
                            if (mutation.equals("kind") || mutation.equals("file"))
                                return new DartSymbolEvidence(original, List.of(new DartNavigationTarget(
                                        mutation.equals("kind") ? "CLASS" : "CONSTRUCTOR",
                                        mutation.equals("file") ? fixture.frameworkFile() : core, 0, 8, 1, 1)), true, Optional.empty());
                            var forged = new DartSymbolProbe(mutation.equals("id") ? original.id() + "-forged" : original.id(),
                                    mutation.equals("span") ? original.offset()+1 : original.offset(), original.length(), original.expectedSymbolName(),
                                    mutation.equals("library") ? "dart:io" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(), original.staticTypeProbe());
                            return new DartSymbolEvidence(forged, value.targets(), true, Optional.empty());
                        }).toList();
                assertFalse(fresh.accept(analysis(fresh, bad)).ready(), mutation);
            }
        }
    }
    @Test void inputDatePickerFormFieldDatesRequireExactGeneratedSpansAndTrustedCoreConstructors() throws Exception {
        for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.InputDatePickerFormField", "",
                    Map.of(new PropertyName("firstDate"), new PropertyValue.StringValue("2000-01-01"),
                           new PropertyName("lastDate"), new PropertyValue.StringValue("2030-12-31"),
                           new PropertyName("initialDate"), new PropertyValue.StringValue("2024-02-29")));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve(tree).resolve("date_time.dart");
            Files.createDirectories(core.getParent()); Files.writeString(core, "class DateTime {}\n");
            var model = new RadioFixture(fixture, sdk, core);
            java.util.function.Function<PairCandidateAnalysisTicket, List<DartSymbolEvidence>> evidence = ticket ->
                    radioEvidence(model, ticket).stream().map(value -> value.probe().expectedSymbolName().equals("DateTime")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget("CONSTRUCTOR", core, 0, 8, 1, 1)), true, Optional.empty()) : value).toList();
            var ticket = radioTicket(model);
            assertEquals(3, ticket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().equals("DateTime")).count());
            var result = ticket.accept(analysis(ticket, evidence.apply(ticket)));
            assertTrue(result.ready(), result.diagnostics().toString());
            assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.live()).ready());
            for (String mutation : List.of("kind", "file", "missing", "id", "span", "library")) {
                var fresh = radioTicket(model);
                var bad = evidence.apply(fresh).stream().filter(value -> !(mutation.equals("missing") && value.probe().expectedSymbolName().equals("DateTime")))
                        .map(value -> {
                            var original = value.probe();
                            if (!original.expectedSymbolName().equals("DateTime")) return value;
                            if (mutation.equals("kind") || mutation.equals("file"))
                                return new DartSymbolEvidence(original, List.of(new DartNavigationTarget(
                                        mutation.equals("kind") ? "CLASS" : "CONSTRUCTOR",
                                        mutation.equals("file") ? fixture.frameworkFile() : core, 0, 8, 1, 1)), true, Optional.empty());
                            var forged = new DartSymbolProbe(mutation.equals("id") ? original.id() + "-forged" : original.id(),
                                    mutation.equals("span") ? original.offset()+1 : original.offset(), original.length(), original.expectedSymbolName(),
                                    mutation.equals("library") ? "dart:io" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(), original.staticTypeProbe());
                            return new DartSymbolEvidence(forged, value.targets(), true, Optional.empty());
                        }).toList();
                assertFalse(fresh.accept(analysis(fresh, bad)).ready(), mutation);
            }
        }
    }

    @Test void timePickerRequiresExactLocalTimeSymbolEvidenceWithoutExpandingTrustRoots() throws Exception {
        var fixture=fixture(Optional.empty(),true,List.of(),"flutter.material.TimePickerDialog","",
                Map.of(new PropertyName("initialTime"),new PropertyValue.StringValue("23:59")));
        var ticket=ticket(fixture,fixture.current());
        var probes=ticket.request().symbolProbes().stream().filter(p->p.expectedSymbolName().equals("TimeOfDay")).toList();
        assertEquals(1,probes.size());
        assertEquals("package:flutter/material.dart",probes.getFirst().expectedLibraryUri());
        assertTrue(ticket.accept(analysis(ticket,fixture.acceptedEvidence())).ready());
        for(String mutation:List.of("missing","id","span","library","outside")){
            var fresh=ticket(fixture,fixture.current());
            var bad=fixture.acceptedEvidence().stream()
                .filter(value->!(mutation.equals("missing")&&value.probe().expectedSymbolName().equals("TimeOfDay")))
                .map(value->{
                    var original=value.probe();
                    if(!original.expectedSymbolName().equals("TimeOfDay"))return value;
                    if(mutation.equals("outside"))return accepted(original,fixture.dartFile());
                    var forged=new DartSymbolProbe(mutation.equals("id")?original.id()+"-forged":original.id(),
                        mutation.equals("span")?original.offset()+1:original.offset(),original.length(),original.expectedSymbolName(),
                        mutation.equals("library")?"dart:io":original.expectedLibraryUri(),original.expectedTargetRoot(),original.expectedTargetKind(),original.staticTypeProbe());
                    return new DartSymbolEvidence(forged,value.targets(),true,Optional.empty());
                }).toList();
            assertFalse(fresh.accept(analysis(fresh,bad)).ready(),mutation);
        }
    }

    @Test void datePickerDatesRequireExactGeneratedSpansAndTrustedCoreConstructors() throws Exception {
        for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.DatePickerDialog", "",
                    Map.of(new PropertyName("firstDate"), new PropertyValue.StringValue("2000-01-01"),
                           new PropertyName("lastDate"), new PropertyValue.StringValue("2030-12-31"),
                           new PropertyName("initialDate"), new PropertyValue.StringValue("2024-02-29"),
                           new PropertyName("currentDate"), new PropertyValue.StringValue("2024-02-15")));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve(tree).resolve("date_time.dart");
            Files.createDirectories(core.getParent()); Files.writeString(core, "class DateTime {}\n");
            var model = new RadioFixture(fixture, sdk, core);
            java.util.function.Function<PairCandidateAnalysisTicket, List<DartSymbolEvidence>> evidence = ticket ->
                    radioEvidence(model, ticket).stream().map(value -> value.probe().expectedSymbolName().equals("DateTime")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget("CONSTRUCTOR", core, 0, 8, 1, 1)), true, Optional.empty()) : value).toList();
            var ticket = radioTicket(model);
            assertEquals(4, ticket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().equals("DateTime")).count());
            var result = ticket.accept(analysis(ticket, evidence.apply(ticket)));
            assertTrue(result.ready(), result.diagnostics().toString());
            assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.live()).ready());
            for (String mutation : List.of("kind", "file", "missing", "id", "span", "library")) {
                var fresh = radioTicket(model);
                var bad = evidence.apply(fresh).stream().filter(value -> !(mutation.equals("missing") && value.probe().expectedSymbolName().equals("DateTime")))
                        .map(value -> {
                            var original = value.probe();
                            if (!original.expectedSymbolName().equals("DateTime")) return value;
                            if (mutation.equals("kind") || mutation.equals("file"))
                                return new DartSymbolEvidence(original, List.of(new DartNavigationTarget(
                                        mutation.equals("kind") ? "CLASS" : "CONSTRUCTOR",
                                        mutation.equals("file") ? fixture.frameworkFile() : core, 0, 8, 1, 1)), true, Optional.empty());
                            var forged = new DartSymbolProbe(mutation.equals("id") ? original.id() + "-forged" : original.id(),
                                    mutation.equals("span") ? original.offset()+1 : original.offset(), original.length(), original.expectedSymbolName(),
                                    mutation.equals("library") ? "dart:io" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(), original.staticTypeProbe());
                            return new DartSymbolEvidence(forged, value.targets(), true, Optional.empty());
                        }).toList();
                assertFalse(fresh.accept(analysis(fresh, bad)).ready(), mutation);
            }
        }
    }

    @Test void dateRangePickerEndpointsRequireExactGeneratedSpansAndTrustedCoreConstructors() throws Exception {
        for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.DateRangePickerDialog", "",
                    Map.of(new PropertyName("firstDate"), new PropertyValue.StringValue("2000-01-01"),
                           new PropertyName("lastDate"), new PropertyValue.StringValue("2030-12-31"),
                           new PropertyName("initialDateRange"), new PropertyValue.StringValue("2024-02-29/2024-03-01"),
                           new PropertyName("currentDate"), new PropertyValue.StringValue("2024-02-15")));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve(tree).resolve("date_time.dart");
            Files.createDirectories(core.getParent()); Files.writeString(core, "class DateTime {}\n");
            var model = new RadioFixture(fixture, sdk, core);
            java.util.function.Function<PairCandidateAnalysisTicket, List<DartSymbolEvidence>> evidence = ticket ->
                    radioEvidence(model, ticket).stream().map(value -> value.probe().expectedSymbolName().equals("DateTime")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget("CONSTRUCTOR", core, 0, 8, 1, 1)), true, Optional.empty()) : value).toList();
            var ticket = radioTicket(model);
            assertEquals(5, ticket.request().symbolProbes().stream().filter(p -> p.expectedSymbolName().equals("DateTime")).count());
            var result = ticket.accept(analysis(ticket, evidence.apply(ticket)));
            assertTrue(result.ready(), result.diagnostics().toString());
            assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.live()).ready());
            for (String mutation : List.of("kind", "file", "missing", "id", "span", "library")) {
                var fresh = radioTicket(model);
                var bad = evidence.apply(fresh).stream().filter(value -> !(mutation.equals("missing") && value.probe().expectedSymbolName().equals("DateTime")))
                        .map(value -> {
                            var original = value.probe();
                            if (!original.expectedSymbolName().equals("DateTime")) return value;
                            if (mutation.equals("kind") || mutation.equals("file"))
                                return new DartSymbolEvidence(original, List.of(new DartNavigationTarget(
                                        mutation.equals("kind") ? "CLASS" : "CONSTRUCTOR",
                                        mutation.equals("file") ? fixture.frameworkFile() : core, 0, 8, 1, 1)), true, Optional.empty());
                            var forged = new DartSymbolProbe(mutation.equals("id") ? original.id() + "-forged" : original.id(),
                                    mutation.equals("span") ? original.offset()+1 : original.offset(), original.length(), original.expectedSymbolName(),
                                    mutation.equals("library") ? "dart:io" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(), original.staticTypeProbe());
                            return new DartSymbolEvidence(forged, value.targets(), true, Optional.empty());
                        }).toList();
                assertFalse(fresh.accept(analysis(fresh, bad)).ready(), mutation);
            }
        }
    }

    @Test void fadeInImageBothDurationsNeedExactTrustedCoreEvidence() throws Exception {
        var fixture=fixture(Optional.empty(),true,List.of(),"flutter.widgets.FadeInImage","",
            Map.of(new PropertyName("placeholder"),PropertyValue.ImageProviderValue.asset("assets/a.png"),
                   new PropertyName("image"),PropertyValue.ImageProviderValue.asset("assets/b.png"),
                   new PropertyName("fadeOutDurationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(1000)),
                   new PropertyName("fadeInDurationUs"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(2000))));
        var sdk=fixture.flutterLib().getParent().getParent().getParent();
        var core=sdk.resolve("bin/cache/dart-sdk/lib/core/duration.dart");Files.createDirectories(core.getParent());Files.writeString(core,"class Duration {}\n");
        var model=new RadioFixture(fixture,sdk,core);var ticket=radioTicket(model);
        var evidence=radioEvidence(model,ticket);
        assertEquals(2,ticket.request().symbolProbes().stream().filter(p->p.expectedSymbolName().equals("Duration")).count());
        assertTrue(ticket.accept(analysis(ticket,evidence)).ready());
        var fresh=radioTicket(model);var bad=radioEvidence(model,fresh).stream().map(e->e.probe().expectedSymbolName().equals("Duration")?accepted(e.probe(),fixture.frameworkFile()):e).toList();
        assertFalse(fresh.accept(analysis(fresh,bad)).ready());
    }

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
    void statefulEvidenceRequiresBothExactScannerOwnedFlutterBaseClasses() throws Exception {
        Fixture fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.ClipRRect",
                "clipper", Map.of(), WidgetClassKind.STATEFUL);
        List<DartSymbolEvidence> exact = fixture.acceptedEvidence();
        assertEquals(List.of("StatefulWidget", "State", "Widget", "BuildContext", "Text"),
                exact.stream().map(value -> value.probe().expectedSymbolName()).toList());
        assertEquals("_HomePageState", fixture.prepared().dartTransition().candidateIntegrity()
                .verifiedMemberClassName().orElseThrow());
        assertTrue(analyze(fixture, fixture.current(), exact).ready());
        assertAnalyzedRejected(analyze(fixture, fixture.current(), exact.stream()
                .filter(value -> !value.probe().id().equals(GeneratedDartSymbolProbePlanner.STATE_SUPERCLASS_PROBE_ID))
                .toList()), PairSaveEvidenceDiagnostic.Code.REQUIRED_STATE_PROBE_MISSING);
        assertAnalyzedRejected(analyze(fixture, fixture.current(), exact.stream()
                .filter(value -> !value.probe().id().equals(GeneratedDartSymbolProbePlanner.DESIGNER_SUPERCLASS_PROBE_ID))
                .toList()), PairSaveEvidenceDiagnostic.Code.REQUIRED_STATEFUL_WIDGET_PROBE_MISSING);
        ArrayList<DartSymbolEvidence> swapped = new ArrayList<>(exact);
        java.util.Collections.swap(swapped, 0, 1);
        assertAnalyzedRejected(analyze(fixture, fixture.current(), swapped),
                PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        Path localShadow = fixture.projectRoot().resolve("lib/fake_state.dart");
        Files.writeString(localShadow, "class State<T> {}\n");
        ArrayList<DartSymbolEvidence> shadowed = new ArrayList<>(exact);
        shadowed.set(1, accepted(exact.get(1).probe(), localShadow));
        assertAnalyzedRejected(analyze(fixture, fixture.current(), shadowed),
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void radioCoreTypesBindBothConstructorsAndNullableTypesFromEitherPinnedCoreLibrary(boolean group) throws Exception {
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            for (boolean nullable : List.of(false, true)) {
                for (String variant : (group ? List.of("group") : List.of("standard", "adaptive"))) {
                    for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                        RadioFixture radio = radioFixture(type, nullable, variant, tree);
                        var ticket = radioTicket(radio);
                        var core = ticket.request().symbolProbes().stream()
                                .filter(probe -> probe.expectedLibraryUri().equals("dart:core")).toList();
                        assertEquals(1, core.size());
                        assertEquals(type, core.getFirst().expectedSymbolName());
                        assertEquals(Optional.of(type + (nullable ? "?" : "")), core.getFirst()
                                .staticTypeProbe().orElseThrow().sourceTypeOverride());
                        var result = ticket.accept(analysis(ticket, radioEvidence(radio, ticket)));
                        assertTrue(result.ready(), () -> type + nullable + variant + tree + result.diagnostics());
                        assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(),
                                radio.fixture().live()).ready());
                    }
                }
            }
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void radioCoreAdmissionDoesNotAcceptOtherDartLibrariesOrForgedGeneratedTypeContracts(boolean group) throws Exception {
        RadioFixture radio = radioFixture("String", false, group ? "group" : "standard", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("dart:async", "dart:io", "dart:core_evil", "id", "otherWidgetId",
                "family", "symbol", "type", "override", "missingOverride", "missingProof", "span", "library")) {
            var ticket = radioTicket(radio);
            var original = ticket.request().symbolProbes().stream()
                    .filter(probe -> probe.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            var type = original.staticTypeProbe().orElseThrow();
            Optional<DartStaticTypeProbe> modifiedType = mutation.equals("missingProof") ? Optional.empty()
                    : Optional.of(new DartStaticTypeProbe(
                            type.expressionOffset() - (mutation.equals("span") ? 1 : 0),
                            type.expressionLength() + (mutation.equals("span") ? 1 : 0),
                            type.importInsertionOffset(), type.statementInsertionOffset(),
                            mutation.equals("type") ? "Object" : type.expectedDartType(),
                            mutation.equals("library") ? "package:flutter/material.dart" : type.expectedTypeLibraryUri(),
                            mutation.equals("missingOverride") ? Optional.empty()
                                    : mutation.equals("override") ? Optional.of("Object") : type.sourceTypeOverride()));
            var forged = new DartSymbolProbe(
                    mutation.equals("family") ? original.id().replace(group ? ":radio-group-" : ":radio-", group ? ":radio-" : ":radio-group-")
                            : mutation.equals("id") ? original.id() + ":other"
                            : mutation.equals("otherWidgetId") ? original.id().replace("bbbbbbbb", "aaaaaaaa") : original.id(),
                    original.offset(), original.length(), mutation.equals("symbol") ? "Object" : original.expectedSymbolName(),
                    mutation.startsWith("dart:") ? mutation : original.expectedLibraryUri(),
                    original.expectedTargetRoot(), original.expectedTargetKind(), modifiedType);
            var evidence = radioEvidence(radio, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, radio.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
        Fixture ordinary = fixture();
        var ticket = ticket(ordinary, ordinary.current());
        var probes = ordinary.acceptedEvidence().stream().map(value -> {
            var probe = value.probe();
            return accepted(new DartSymbolProbe(probe.id(), probe.offset(), probe.length(), probe.expectedSymbolName(),
                    "dart:core", probe.expectedTargetRoot(), probe.expectedTargetKind()), ordinary.frameworkFile());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, probes)),
                PairSaveEvidenceDiagnostic.Code.INVALID_FLUTTER_LIBRARY_URI);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void radioCoreTargetMustBeItsOwnClassFileInsideTheExactTrustedSdk(boolean group) throws Exception {
        RadioFixture radio = radioFixture("String", false, group ? "group" : "standard", "bin/cache/dart-sdk/lib/core");
        Path otherCore = radio.coreTarget().resolveSibling("object.dart");
        Files.writeString(otherCore, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("outside/string.dart");
        Files.createDirectories(outside.getParent());
        Files.writeString(outside, "class String {}\n");
        Path neighboring = radio.coreTarget().getParent().resolveSibling("core-evil").resolve("string.dart");
        Files.createDirectories(neighboring.getParent());
        Files.writeString(neighboring, "class String {}\n");
        for (Path target : List.of(otherCore, outside, neighboring, radio.fixture().frameworkFile())) {
            var ticket = radioTicket(radio);
            var evidence = radioEvidence(radio, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)),
                    PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(radio);
        var wrongKind = radioEvidence(radio, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget("TOP_LEVEL_VARIABLE", radio.coreTarget(), 0, 1, 1, 1)),
                        true, Optional.empty(), value.staticTypeEvidence()) : value).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongKind)),
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        ticket = radioTicket(radio);
        var wrongRoot = radioEvidence(radio, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var probe = value.probe();
            return accepted(new DartSymbolProbe(probe.id(), probe.offset(), probe.length(), probe.expectedSymbolName(),
                    probe.expectedLibraryUri(), radio.coreTarget().getParent(), probe.expectedTargetKind(), probe.staticTypeProbe()), radio.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)),
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void radioCoreStaticProofCannotBeMissingRejectedOrDetachedFromTheSelectedType(boolean group) throws Exception {
        RadioFixture radio = radioFixture("Object", true, group ? "group" : "adaptive", "bin/cache/pkg/sky_engine/lib/core");
        for (boolean missing : List.of(false, true)) {
            var ticket = radioTicket(radio);
            var evidence = radioEvidence(radio, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), value.targets(), false, Optional.of("untrusted selected type"),
                            missing ? Optional.empty() : Optional.of(new DartStaticTypeEvidence(
                                    value.probe().staticTypeProbe().orElseThrow(), false, Optional.of("dynamic alias rejected")))) : value).toList();
            assertAnalyzedRejected(ticket.accept(rejectedAnalysis(ticket, List.of(), evidence)),
                    PairSaveEvidenceDiagnostic.Code.INCOMPLETE_STATIC_TYPE_EVIDENCE);
        }
        var ticket = radioTicket(radio);
        var core = radioEvidence(radio, ticket).stream()
                .filter(value -> value.probe().expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
        var original = core.probe().staticTypeProbe().orElseThrow();
        var detached = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(),
                original.importInsertionOffset(), original.statementInsertionOffset(), original.expectedDartType(),
                original.expectedTypeLibraryUri(), Optional.of("String?"));
        assertThrows(IllegalArgumentException.class, () -> new DartSymbolEvidence(core.probe(), core.targets(),
                true, Optional.empty(), Optional.of(new DartStaticTypeEvidence(detached, true, Optional.empty()))));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void radioCoreNavigationIsRevalidatedAtPostCasBinding(boolean group) throws Exception {
        RadioFixture radio = radioFixture("bool", false, group ? "group" : "standard", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(radio);
        var analyzed = ticket.accept(analysis(ticket, radioEvidence(radio, ticket)));
        assertTrue(analyzed.ready(), () -> analyzed.diagnostics().toString());
        Files.delete(radio.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(analyzed.analyzedOptional().orElseThrow(), radio.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code()
                == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    @Test
    void floatingHeaderDurationsAcceptOnlyTheGeneratedLiteralConstructorInEitherTrustedCoreTree() throws Exception {
        for (String property : List.of("animationStyleDurationUs", "animationStyleReverseDurationUs")) {
            for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                var fixture = floatingHeaderDurationFixture(property, tree);
                for (String kind : List.of("CLASS", "CONSTRUCTOR")) {
                    var ticket = radioTicket(fixture);
                    var core = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).toList();
                    assertEquals(1, core.size()); assertEquals("Duration", core.getFirst().expectedSymbolName());
                    assertTrue(core.getFirst().staticTypeProbe().isEmpty());
                    assertTrue(core.getFirst().id().endsWith(":sliver-floating-header-core-duration:" + property));
                    var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
                    var result = ticket.accept(analysis(ticket, evidence));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                    assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live()).ready());
                }
            }
        }
    }

    @Test
    void floatingHeaderDurationCannotForgeLibraryIdLeafSpanSymbolOrTypeMetadata() throws Exception {
        var fixture = floatingHeaderDurationFixture("animationStyleDurationUs", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("library", "id", "widget", "family", "leaf", "span", "symbol", "type")) {
            var ticket = radioTicket(fixture);
            var original = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            String id = switch (mutation) {
                case "id" -> original.id() + ":extra";
                case "widget" -> original.id().replace("bbbbbbbb", "aaaaaaaa");
                case "family" -> original.id().replace("sliver-floating-header-core-duration", "radio-core-duration");
                case "leaf" -> original.id().replace("animationStyleDurationUs", "animationStyleReverseDurationUs");
                default -> original.id();
            };
            var forged = new DartSymbolProbe(id, original.offset() + (mutation.equals("span") ? 1 : 0), original.length(),
                    mutation.equals("symbol") ? "DateTime" : original.expectedSymbolName(),
                    mutation.equals("library") ? "dart:async" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(),
                    mutation.equals("type") ? Optional.of(new DartStaticTypeProbe(original.offset(), original.length(), 0, 0,
                            "Duration", "package:flutter/material.dart")) : original.staticTypeProbe());
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, fixture.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
    }

    @Test
    void floatingHeaderDurationRejectsWrongCoreFilesRootsTargetKindsAndMissingEvidence() throws Exception {
        var fixture = floatingHeaderDurationFixture("animationStyleReverseDurationUs", "bin/cache/dart-sdk/lib/core");
        Path other = fixture.coreTarget().resolveSibling("object.dart"); Files.writeString(other, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("not-sdk/core/duration.dart"); Files.createDirectories(outside.getParent()); Files.writeString(outside, "class Duration {}\n");
        for (Path target : List.of(other, outside, fixture.fixture().frameworkFile())) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        for (String kind : List.of("FUNCTION", "TOP_LEVEL_VARIABLE", "GETTER")) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(fixture);
        var wrongRoot = radioEvidence(fixture, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var p = value.probe();
            return accepted(new DartSymbolProbe(p.id(), p.offset(), p.length(), p.expectedSymbolName(), p.expectedLibraryUri(),
                    fixture.coreTarget().getParent(), p.expectedTargetKind(), p.staticTypeProbe()), fixture.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        ticket = radioTicket(fixture);
        var missing = radioEvidence(fixture, ticket).stream().filter(value -> !value.probe().expectedLibraryUri().equals("dart:core")).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, missing)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void floatingHeaderDurationTargetIsRevalidatedAfterTheAppliedLiveCas() throws Exception {
        var fixture = floatingHeaderDurationFixture("animationStyleDurationUs", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(fixture); var result = ticket.accept(analysis(ticket, radioEvidence(fixture, ticket)));
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        Files.delete(fixture.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(d -> d.code() == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    private RadioFixture floatingHeaderDurationFixture(String property, String tree) throws Exception {
        var fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.SliverFloatingHeader", "",
                Map.of(new PropertyName(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123456))));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve("duration.dart"); Files.createDirectories(target.getParent());
        Files.writeString(target, "class Duration { const Duration({int microseconds = 0}); }\n");
        return new RadioFixture(fixture, sdk, target);
    }

    @Test
    void expansionDurationsAcceptOnlyTheGeneratedLiteralConstructorInEitherTrustedCoreTree() throws Exception {
        for (String property : List.of("expansionAnimationStyleDurationUs", "expansionAnimationStyleReverseDurationUs")) {
            for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                var fixture = expansionDurationFixture(property, tree);
                for (String kind : List.of("CLASS", "CONSTRUCTOR")) {
                    var ticket = radioTicket(fixture);
                    var core = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).toList();
                    assertEquals(1, core.size()); assertEquals("Duration", core.getFirst().expectedSymbolName());
                    assertTrue(core.getFirst().staticTypeProbe().isEmpty());
                    assertTrue(core.getFirst().id().endsWith(":expansion-tile-core-duration:" + property));
                    var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
                    var result = ticket.accept(analysis(ticket, evidence));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                    assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live()).ready());
                }
            }
        }
    }

    @Test
    void expansionDurationCannotForgeLibraryIdLeafSpanSymbolOrTypeMetadata() throws Exception {
        var fixture = expansionDurationFixture("expansionAnimationStyleDurationUs", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("library", "id", "widget", "family", "leaf", "span", "symbol", "type")) {
            var ticket = radioTicket(fixture);
            var original = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            String id = switch (mutation) {
                case "id" -> original.id() + ":extra";
                case "widget" -> original.id().replace("bbbbbbbb", "aaaaaaaa");
                case "family" -> original.id().replace("expansion-tile-core-duration", "radio-core-duration");
                case "leaf" -> original.id().replace("expansionAnimationStyleDurationUs", "expansionAnimationStyleReverseDurationUs");
                default -> original.id();
            };
            var forged = new DartSymbolProbe(id, original.offset() + (mutation.equals("span") ? 1 : 0), original.length(),
                    mutation.equals("symbol") ? "DateTime" : original.expectedSymbolName(),
                    mutation.equals("library") ? "dart:async" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(),
                    mutation.equals("type") ? Optional.of(new DartStaticTypeProbe(original.offset(), original.length(), 0, 0,
                            "Duration", "package:flutter/material.dart")) : original.staticTypeProbe());
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, fixture.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
    }

    @Test
    void expansionDurationRejectsWrongCoreFilesRootsTargetKindsAndMissingEvidence() throws Exception {
        var fixture = expansionDurationFixture("expansionAnimationStyleReverseDurationUs", "bin/cache/dart-sdk/lib/core");
        Path other = fixture.coreTarget().resolveSibling("object.dart"); Files.writeString(other, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("not-sdk/core/duration.dart"); Files.createDirectories(outside.getParent()); Files.writeString(outside, "class Duration {}\n");
        for (Path target : List.of(other, outside, fixture.fixture().frameworkFile())) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        for (String kind : List.of("FUNCTION", "TOP_LEVEL_VARIABLE", "GETTER")) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(fixture);
        var wrongRoot = radioEvidence(fixture, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var p = value.probe();
            return accepted(new DartSymbolProbe(p.id(), p.offset(), p.length(), p.expectedSymbolName(), p.expectedLibraryUri(),
                    fixture.coreTarget().getParent(), p.expectedTargetKind(), p.staticTypeProbe()), fixture.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        ticket = radioTicket(fixture);
        var missing = radioEvidence(fixture, ticket).stream().filter(value -> !value.probe().expectedLibraryUri().equals("dart:core")).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, missing)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void expansionDurationTargetIsRevalidatedAfterTheAppliedLiveCas() throws Exception {
        var fixture = expansionDurationFixture("expansionAnimationStyleDurationUs", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(fixture); var result = ticket.accept(analysis(ticket, radioEvidence(fixture, ticket)));
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        Files.delete(fixture.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(d -> d.code() == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    private RadioFixture expansionDurationFixture(String property, String tree) throws Exception {
        var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.ExpansionTile", "",
                Map.of(new PropertyName(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123456))));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve("duration.dart"); Files.createDirectories(target.getParent());
        Files.writeString(target, "class Duration { const Duration({int microseconds = 0}); }\n");
        return new RadioFixture(fixture, sdk, target);
    }

    @Test
    void sliverAnimatedOpacityDurationsAcceptOnlyTheGeneratedLiteralConstructorInEitherTrustedCoreTree() throws Exception {
        for (String property : List.of("durationUs")) {
            for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                var fixture = sliverAnimatedOpacityDurationFixture(property, tree);
                for (String kind : List.of("CLASS", "CONSTRUCTOR")) {
                    var ticket = radioTicket(fixture);
                    var core = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).toList();
                    assertEquals(1, core.size()); assertEquals("Duration", core.getFirst().expectedSymbolName());
                    assertTrue(core.getFirst().staticTypeProbe().isEmpty());
                    assertTrue(core.getFirst().id().endsWith(":sliver-animated-opacity-duration"));
                    var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
                    var result = ticket.accept(analysis(ticket, evidence));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                    assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live()).ready());
                }
            }
        }
    }

    @Test
    void sliverAnimatedOpacityDurationCannotForgeLibraryIdLeafSpanSymbolOrTypeMetadata() throws Exception {
        var fixture = sliverAnimatedOpacityDurationFixture("durationUs", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("library", "id", "widget", "family", "leaf", "span", "symbol", "type")) {
            var ticket = radioTicket(fixture);
            var original = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            String id = switch (mutation) {
                case "id" -> original.id() + ":extra";
                case "widget" -> original.id().replace("bbbbbbbb", "aaaaaaaa");
                case "family" -> original.id().replace("sliver-animated-opacity-duration", "radio-core-duration");
                case "leaf" -> original.id() + ":durationUs";
                default -> original.id();
            };
            var forged = new DartSymbolProbe(id, original.offset() + (mutation.equals("span") ? 1 : 0), original.length(),
                    mutation.equals("symbol") ? "DateTime" : original.expectedSymbolName(),
                    mutation.equals("library") ? "dart:async" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(),
                    mutation.equals("type") ? Optional.of(new DartStaticTypeProbe(original.offset(), original.length(), 0, 0,
                            "Duration", "package:flutter/material.dart")) : original.staticTypeProbe());
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, fixture.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
    }

    @Test
    void sliverAnimatedOpacityDurationRejectsWrongCoreFilesRootsTargetKindsAndMissingEvidence() throws Exception {
        var fixture = sliverAnimatedOpacityDurationFixture("durationUs", "bin/cache/dart-sdk/lib/core");
        Path other = fixture.coreTarget().resolveSibling("object.dart"); Files.writeString(other, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("not-sdk/core/duration.dart"); Files.createDirectories(outside.getParent()); Files.writeString(outside, "class Duration {}\n");
        for (Path target : List.of(other, outside, fixture.fixture().frameworkFile())) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        for (String kind : List.of("FUNCTION", "TOP_LEVEL_VARIABLE", "GETTER")) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(fixture);
        var wrongRoot = radioEvidence(fixture, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var p = value.probe();
            return accepted(new DartSymbolProbe(p.id(), p.offset(), p.length(), p.expectedSymbolName(), p.expectedLibraryUri(),
                    fixture.coreTarget().getParent(), p.expectedTargetKind(), p.staticTypeProbe()), fixture.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        ticket = radioTicket(fixture);
        var missing = radioEvidence(fixture, ticket).stream().filter(value -> !value.probe().expectedLibraryUri().equals("dart:core")).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, missing)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void sliverAnimatedOpacityDurationTargetIsRevalidatedAfterTheAppliedLiveCas() throws Exception {
        var fixture = sliverAnimatedOpacityDurationFixture("durationUs", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(fixture); var result = ticket.accept(analysis(ticket, radioEvidence(fixture, ticket)));
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        Files.delete(fixture.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(d -> d.code() == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    private RadioFixture sliverAnimatedOpacityDurationFixture(String property, String tree) throws Exception {
        var fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.SliverAnimatedOpacity", "",
                Map.of(new PropertyName(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123456))));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve("duration.dart"); Files.createDirectories(target.getParent());
        Files.writeString(target, "class Duration { const Duration({int microseconds = 0}); }\n");
        return new RadioFixture(fixture, sdk, target);
    }

    @Test
    void tooltipDurationsAcceptOnlyTheGeneratedLiteralConstructorInEitherTrustedCoreTree() throws Exception {
        for (String property : List.of("waitDurationUs", "showDurationUs", "exitDurationUs")) {
            for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                var fixture = tooltipDurationFixture(property, tree);
                for (String kind : List.of("CLASS", "CONSTRUCTOR")) {
                    var ticket = radioTicket(fixture);
                    var core = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).toList();
                    assertEquals(1, core.size()); assertEquals("Duration", core.getFirst().expectedSymbolName());
                    assertTrue(core.getFirst().staticTypeProbe().isEmpty());
                    assertTrue(core.getFirst().id().endsWith(":tooltip-core-duration:" + property));
                    var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
                    var result = ticket.accept(analysis(ticket, evidence));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                    assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live()).ready());
                }
            }
        }
    }

    @Test
    void tooltipDurationCannotForgeLibraryIdLeafSpanSymbolOrTypeMetadata() throws Exception {
        var fixture = tooltipDurationFixture("waitDurationUs", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("library", "id", "widget", "family", "leaf", "span", "symbol", "type")) {
            var ticket = radioTicket(fixture);
            var original = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            String id = switch (mutation) {
                case "id" -> original.id() + ":extra";
                case "widget" -> original.id().replace("bbbbbbbb", "aaaaaaaa");
                case "family" -> original.id().replace("tooltip-core-duration", "radio-core-duration");
                case "leaf" -> original.id().replace("waitDurationUs", "showDurationUs");
                default -> original.id();
            };
            var forged = new DartSymbolProbe(id, original.offset() + (mutation.equals("span") ? 1 : 0), original.length(),
                    mutation.equals("symbol") ? "DateTime" : original.expectedSymbolName(),
                    mutation.equals("library") ? "dart:async" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(),
                    mutation.equals("type") ? Optional.of(new DartStaticTypeProbe(original.offset(), original.length(), 0, 0,
                            "Duration", "package:flutter/material.dart")) : original.staticTypeProbe());
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, fixture.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
    }

    @Test
    void tooltipDurationRejectsWrongCoreFilesRootsTargetKindsAndMissingEvidence() throws Exception {
        var fixture = tooltipDurationFixture("showDurationUs", "bin/cache/dart-sdk/lib/core");
        Path other = fixture.coreTarget().resolveSibling("object.dart"); Files.writeString(other, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("not-sdk/core/duration.dart"); Files.createDirectories(outside.getParent()); Files.writeString(outside, "class Duration {}\n");
        for (Path target : List.of(other, outside, fixture.fixture().frameworkFile())) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        for (String kind : List.of("FUNCTION", "TOP_LEVEL_VARIABLE", "GETTER")) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(fixture);
        var wrongRoot = radioEvidence(fixture, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var p = value.probe();
            return accepted(new DartSymbolProbe(p.id(), p.offset(), p.length(), p.expectedSymbolName(), p.expectedLibraryUri(),
                    fixture.coreTarget().getParent(), p.expectedTargetKind(), p.staticTypeProbe()), fixture.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        ticket = radioTicket(fixture);
        var missing = radioEvidence(fixture, ticket).stream().filter(value -> !value.probe().expectedLibraryUri().equals("dart:core")).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, missing)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void tooltipDurationTargetIsRevalidatedAfterTheAppliedLiveCas() throws Exception {
        var fixture = tooltipDurationFixture("waitDurationUs", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(fixture); var result = ticket.accept(analysis(ticket, radioEvidence(fixture, ticket)));
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        Files.delete(fixture.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(d -> d.code() == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    private RadioFixture tooltipDurationFixture(String property, String tree) throws Exception {
        var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.Tooltip", "",
                Map.of(new PropertyName(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123456))));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve("duration.dart"); Files.createDirectories(target.getParent());
        Files.writeString(target, "class Duration { const Duration({int microseconds = 0}); }\n");
        return new RadioFixture(fixture, sdk, target);
    }

    @Test
    void tooltipThemeDurationsAcceptOnlyTheGeneratedLiteralConstructorInEitherTrustedCoreTree() throws Exception {
        for (String property : List.of("waitDurationUs", "showDurationUs", "exitDurationUs")) {
            for (String tree : List.of("bin/cache/dart-sdk/lib/core", "bin/cache/pkg/sky_engine/lib/core")) {
                var fixture = tooltipThemeDurationFixture(property, tree);
                for (String kind : List.of("CLASS", "CONSTRUCTOR")) {
                    var ticket = radioTicket(fixture);
                    var core = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).toList();
                    assertEquals(1, core.size()); assertEquals("Duration", core.getFirst().expectedSymbolName());
                    assertTrue(core.getFirst().staticTypeProbe().isEmpty());
                    assertTrue(core.getFirst().id().endsWith(":tooltip-theme-core-duration:" + property));
                    var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                            ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
                    var result = ticket.accept(analysis(ticket, evidence));
                    assertTrue(result.ready(), () -> result.diagnostics().toString());
                    assertTrue(PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live()).ready());
                }
            }
        }
    }

    @Test
    void tooltipThemeDurationCannotForgeLibraryIdLeafSpanSymbolOrTypeMetadata() throws Exception {
        var fixture = tooltipThemeDurationFixture("waitDurationUs", "bin/cache/dart-sdk/lib/core");
        for (String mutation : List.of("library", "id", "widget", "family", "leaf", "span", "symbol", "type")) {
            var ticket = radioTicket(fixture);
            var original = ticket.request().symbolProbes().stream().filter(p -> p.expectedLibraryUri().equals("dart:core")).findFirst().orElseThrow();
            String id = switch (mutation) {
                case "id" -> original.id() + ":extra";
                case "widget" -> original.id().replace("bbbbbbbb", "aaaaaaaa");
                case "family" -> original.id().replace("tooltip-theme-core-duration", "radio-core-duration");
                case "leaf" -> original.id().replace("waitDurationUs", "showDurationUs");
                default -> original.id();
            };
            var forged = new DartSymbolProbe(id, original.offset() + (mutation.equals("span") ? 1 : 0), original.length(),
                    mutation.equals("symbol") ? "DateTime" : original.expectedSymbolName(),
                    mutation.equals("library") ? "dart:async" : original.expectedLibraryUri(), original.expectedTargetRoot(), original.expectedTargetKind(),
                    mutation.equals("type") ? Optional.of(new DartStaticTypeProbe(original.offset(), original.length(), 0, 0,
                            "Duration", "package:flutter/material.dart")) : original.staticTypeProbe());
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().equals(original)
                    ? accepted(forged, fixture.coreTarget()) : value).toList();
            var result = ticket.accept(analysis(ticket, evidence));
            assertAnalyzedRejected(result, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            assertFalse(result.ready(), mutation);
        }
    }

    @Test
    void tooltipThemeDurationRejectsWrongCoreFilesRootsTargetKindsAndMissingEvidence() throws Exception {
        var fixture = tooltipThemeDurationFixture("showDurationUs", "bin/cache/dart-sdk/lib/core");
        Path other = fixture.coreTarget().resolveSibling("object.dart"); Files.writeString(other, "class Object {}\n");
        Path outside = temporaryDirectory.resolve("not-sdk/core/duration.dart"); Files.createDirectories(outside.getParent()); Files.writeString(outside, "class Duration {}\n");
        for (Path target : List.of(other, outside, fixture.fixture().frameworkFile())) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? accepted(value.probe(), target) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        for (String kind : List.of("FUNCTION", "TOP_LEVEL_VARIABLE", "GETTER")) {
            var ticket = radioTicket(fixture);
            var evidence = radioEvidence(fixture, ticket).stream().map(value -> value.probe().expectedLibraryUri().equals("dart:core")
                    ? new DartSymbolEvidence(value.probe(), List.of(new DartNavigationTarget(kind, fixture.coreTarget(), 0, 1, 1, 1)), true, Optional.empty()) : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
        var ticket = radioTicket(fixture);
        var wrongRoot = radioEvidence(fixture, ticket).stream().map(value -> {
            if (!value.probe().expectedLibraryUri().equals("dart:core")) return value;
            var p = value.probe();
            return accepted(new DartSymbolProbe(p.id(), p.offset(), p.length(), p.expectedSymbolName(), p.expectedLibraryUri(),
                    fixture.coreTarget().getParent(), p.expectedTargetKind(), p.staticTypeProbe()), fixture.coreTarget());
        }).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, wrongRoot)), PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        ticket = radioTicket(fixture);
        var missing = radioEvidence(fixture, ticket).stream().filter(value -> !value.probe().expectedLibraryUri().equals("dart:core")).toList();
        assertAnalyzedRejected(ticket.accept(analysis(ticket, missing)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
    }

    @Test
    void tooltipThemeDurationTargetIsRevalidatedAfterTheAppliedLiveCas() throws Exception {
        var fixture = tooltipThemeDurationFixture("waitDurationUs", "bin/cache/dart-sdk/lib/core");
        var ticket = radioTicket(fixture); var result = ticket.accept(analysis(ticket, radioEvidence(fixture, ticket)));
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        Files.delete(fixture.coreTarget());
        var bound = PairSaveEvidenceGate.bindApplied(result.analyzedOptional().orElseThrow(), fixture.fixture().live());
        assertFalse(bound.ready());
        assertTrue(bound.diagnostics().stream().anyMatch(d -> d.code() == PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET));
    }

    private RadioFixture tooltipThemeDurationFixture(String property, String tree) throws Exception {
        var fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.TooltipTheme", "",
                Map.of(new PropertyName(property), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(123456))));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve("duration.dart"); Files.createDirectories(target.getParent());
        Files.writeString(target, "class Duration { const Duration({int microseconds = 0}); }\n");
        return new RadioFixture(fixture, sdk, target);
    }


    @Test
    void valueListenableCoreTypeOccurrencesRequireExactProofAndTrustedClassFile() throws Exception {
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.ValueListenableBuilder", "",
                    Map.of(new PropertyName("valueType"), new PropertyValue.StringValue(type),
                        new PropertyName("valueListenable"), new PropertyValue.StringValue("constant"),
                        new PropertyName("builder"), new PropertyValue.StringValue("child")));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve("bin/cache/dart-sdk/lib/core/" + type.toLowerCase(java.util.Locale.ROOT) + ".dart");
            Files.createDirectories(core.getParent());Files.writeString(core,"class " + type + " {}\n");
            var model = new RadioFixture(fixture,sdk,core); var ticket = radioTicket(model);
            assertEquals(2,ticket.request().symbolProbes().stream().filter(probe->probe.expectedLibraryUri().equals("dart:core")).count());
            assertTrue(ticket.accept(analysis(ticket,radioEvidence(model,ticket))).ready());
            for (var original : ticket.request().symbolProbes().stream().filter(probe->probe.expectedLibraryUri().equals("dart:core")).toList()) {
                var forged = new DartSymbolProbe(original.id(),original.offset(),original.length(),original.expectedSymbolName(),
                        original.expectedLibraryUri(),original.expectedTargetRoot(),original.expectedTargetKind(),Optional.empty());
                var fresh=radioTicket(model);
                var bad=radioEvidence(model,fresh).stream().map(value->value.probe().equals(original)?accepted(forged,core):value).toList();
                assertAnalyzedRejected(fresh.accept(analysis(fresh,bad)),PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            }
            var fresh=radioTicket(model);
            var wrong=radioEvidence(model,fresh).stream().map(value->value.probe().expectedLibraryUri().equals("dart:core")
                    ?accepted(value.probe(),fixture.frameworkFile()):value).toList();
            assertFalse(fresh.accept(analysis(fresh,wrong)).ready());
        }
    }

    @Test
    void tweenAnimationCoreTypeOccurrencesRequireExactProofAndTrustedClassFile() throws Exception {
        for (String type : List.of("String", "int", "double", "num", "bool", "Object")) {
            var fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.TweenAnimationBuilder", "",
                    Map.of(new PropertyName("valueType"), new PropertyValue.StringValue(type),
                        new PropertyName("tween"), new PropertyValue.StringValue("default"),
                        new PropertyName("builder"), new PropertyValue.StringValue("child"),
                        new PropertyName("durationUs"), new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(300000))));
            var sdk = fixture.flutterLib().getParent().getParent().getParent();
            var core = sdk.resolve("bin/cache/dart-sdk/lib/core/" + type.toLowerCase(java.util.Locale.ROOT) + ".dart");
            Files.createDirectories(core.getParent());Files.writeString(core,"class " + type + " {}\n");
            var duration=sdk.resolve("bin/cache/dart-sdk/lib/core/duration.dart");Files.writeString(duration,"class Duration {}\n");
            var model = new RadioFixture(fixture,sdk,core); var ticket = radioTicket(model);
            java.util.function.Function<PairCandidateAnalysisTicket,List<DartSymbolEvidence>> evidence = request ->
                    request.request().symbolProbes().stream().map(probe -> accepted(probe,
                            probe.expectedSymbolName().equals("Duration") ? duration
                                    : probe.expectedLibraryUri().equals("dart:core") ? core : fixture.frameworkFile())).toList();
            assertEquals(type.equals("int") ? 1 : 2,ticket.request().symbolProbes().stream().filter(probe->probe.expectedSymbolName().equals(type)).count());
            assertTrue(ticket.accept(analysis(ticket,evidence.apply(ticket))).ready());
            for (var original : ticket.request().symbolProbes().stream().filter(probe->probe.expectedSymbolName().equals(type)).toList()) {
                var forged = new DartSymbolProbe(original.id(),original.offset(),original.length(),original.expectedSymbolName(),
                        original.expectedLibraryUri(),original.expectedTargetRoot(),original.expectedTargetKind(),Optional.empty());
                var fresh=radioTicket(model);
                var bad=evidence.apply(fresh).stream().map(value->value.probe().equals(original)?accepted(forged,core):value).toList();
                assertAnalyzedRejected(fresh.accept(analysis(fresh,bad)),PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            }
            var fresh=radioTicket(model);
            var wrong=evidence.apply(fresh).stream().map(value->value.probe().expectedLibraryUri().equals("dart:core")
                    ?accepted(value.probe(),fixture.frameworkFile()):value).toList();
            assertFalse(fresh.accept(analysis(fresh,wrong)).ready());
        }
    }

    private RadioFixture radioFixture(String type, boolean nullable, String variant, String tree) throws Exception {
        PropertyValue value = nullable ? new PropertyValue.NullValue() : switch (type) {
            case "int", "num" -> new PropertyValue.IntegerValue(java.math.BigInteger.ONE);
            case "double" -> new PropertyValue.DoubleValue(new java.math.BigDecimal("1.5"));
            case "bool" -> new PropertyValue.BooleanValue(false);
            default -> new PropertyValue.StringValue("option");
        };
        var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
        properties.put(new PropertyName("valueType"), new PropertyValue.StringValue(type));
        properties.put(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(nullable));
        properties.put(new PropertyName("onChanged"), new PropertyValue.StringValue("noop"));
        if (variant.equals("group")) {
            properties.put(new PropertyName("groupValue"), value);
        } else {
            properties.put(new PropertyName("value"), value);
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
        }
        Fixture fixture = fixture(Optional.empty(), true, List.of(),
                variant.equals("group") ? "flutter.widgets.RadioGroup" : "flutter.material.Radio", "", properties);
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve(tree).resolve(type.toLowerCase(java.util.Locale.ROOT) + ".dart");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "class " + type + " {}\n");
        return new RadioFixture(fixture, sdk, target);
    }

    private PairCandidateAnalysisTicket radioTicket(RadioFixture radio) throws IOException {
        Fixture fixture = radio.fixture();
        return PairSaveEvidenceGate.prepareAnalysis(fixture.current(), fixture.prepared(), fixture.projectRoot(),
                fixture.dartFile(), DartCandidateWarningPolicy.ALLOW, radio.sdk());
    }

    private static List<DartSymbolEvidence> radioEvidence(RadioFixture radio, PairCandidateAnalysisTicket ticket) {
        return ticket.request().symbolProbes().stream().map(probe -> accepted(probe,
                probe.expectedLibraryUri().equals("dart:core") ? radio.coreTarget() : radio.fixture().frameworkFile())).toList();
    }

    @Test
    void notificationSubtypeBoundIsManifestedEvenWithoutCallbackAndCannotBeDroppedOrForged() throws Exception {
        for (PropertyValue typeValue : List.<PropertyValue>of(new PropertyValue.StringValue("ScrollNotification"),
                new PropertyValue.DartObjectReferenceValue(Optional.of("package:evidence_gate_fixture/clippers.dart"),
                        "ProjectClipper", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))) {
            Fixture fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.NotificationListener", "notificationType",
                    Map.of(new PropertyName("notificationType"), typeValue));
            var ticket = ticket(fixture, fixture.current());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
            var bound = ticket.request().symbolProbes().stream().filter(probe -> probe.staticTypeProbe()
                    .filter(type -> type.sourceTypeBound().equals(Optional.of("Notification"))).isPresent()).findFirst().orElseThrow();
            var type = bound.staticTypeProbe().orElseThrow();
            assertEquals("Type", type.expectedDartType());
            for (boolean removeBound : List.of(true, false)) {
                var freshTicket = ticket(fixture, fixture.current());
                var altered = new DartStaticTypeProbe(type.expressionOffset(), type.expressionLength(), type.importInsertionOffset(),
                        type.statementInsertionOffset(), type.expectedDartType(), type.expectedTypeLibraryUri(),
                        removeBound ? type.sourceTypeOverride() : Optional.of("Notification"),
                        removeBound ? Optional.empty() : type.sourceTypeBound());
                var forged = new DartSymbolProbe(bound.id(), bound.offset(), bound.length(), bound.expectedSymbolName(),
                        bound.expectedLibraryUri(), bound.expectedTargetRoot(), bound.expectedTargetKind(), Optional.of(altered));
                var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(bound)
                        ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                                Optional.of(new DartStaticTypeEvidence(altered, true, Optional.empty()))) : value).toList();
                assertAnalyzedRejected(freshTicket.accept(analysis(freshTicket, evidence)),
                        PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            }
        }
    }

    private record RadioFixture(Fixture fixture, Path sdk, Path coreTarget) {}

    @Test
    void dartUiPointerKindsRequireEveryExactGeneratedOccurrenceAndRejectUriLookalikes() throws Exception {
        DartUiFixture ui = dartUiFixture();
        var ticket = dartUiTicket(ui);
        var evidence = dartUiEvidence(ui, ticket);
        assertEquals(3, evidence.stream().filter(value -> value.probe().expectedLibraryUri().equals("dart:ui")).count(),
                "The set type and both enum values retain separate analyzer evidence");
        assertTrue(ticket.request().symbolProbes().stream().filter(probe -> probe.expectedLibraryUri().equals("dart:ui"))
                .allMatch(probe -> probe.expectedTargetRoot().equals(ui.sdk()) && probe.expectedSymbolName().equals("PointerDeviceKind")));
        var accepted = ticket.accept(analysis(ticket, evidence));
        assertTrue(accepted.ready(), () -> accepted.diagnostics().toString());
        assertTrue(PairSaveEvidenceGate.bindApplied(accepted.analyzedOptional().orElseThrow(), ui.fixture().live()).ready());

        var missingTicket = dartUiTicket(ui);
        var missing = dartUiEvidence(ui, missingTicket).stream()
                .filter(value -> !value.probe().expectedLibraryUri().equals("dart:ui")).toList();
        assertAnalyzedRejected(missingTicket.accept(analysis(missingTicket, missing)),
                PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        for (String lookalike : List.of("dart:ui_evil", "dart:ui/", "package:ui/painting.dart")) {
            var maliciousTicket = dartUiTicket(ui);
            var malicious = dartUiEvidence(ui, maliciousTicket).stream().map(value -> {
                var original = value.probe();
                if (!original.expectedLibraryUri().equals("dart:ui")) return value;
                var changed = new DartSymbolProbe(original.id(), original.offset(), original.length(), original.expectedSymbolName(),
                        lookalike, original.expectedTargetRoot(), original.expectedTargetKind(), original.staticTypeProbe());
                return accepted(changed, ui.uiTarget());
            }).toList();
            assertAnalyzedRejected(maliciousTicket.accept(analysis(maliciousTicket, malicious)),
                    PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        }
    }

    @Test
    void dartUiExactProbesRejectNavigationOrDeclaredRootOutsideTheTrustedSdk() throws Exception {
        DartUiFixture ui = dartUiFixture();
        Path outsideRoot = Files.createDirectories(ui.sdk().resolveSibling("flutter-evil"));
        Path outsideTarget = outsideRoot.resolve("pointer.dart");
        Files.writeString(outsideTarget, "enum PointerDeviceKind { touch, mouse }\n");
        var navigationTicket = dartUiTicket(ui);
        var navigation = dartUiEvidence(ui, navigationTicket).stream().map(value ->
                value.probe().expectedLibraryUri().equals("dart:ui") ? accepted(value.probe(), outsideTarget) : value).toList();
        assertAnalyzedRejected(navigationTicket.accept(analysis(navigationTicket, navigation)),
                PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);

        var rootTicket = dartUiTicket(ui);
        var forgedRoot = dartUiEvidence(ui, rootTicket).stream().map(value -> {
            var original = value.probe();
            if (!original.expectedLibraryUri().equals("dart:ui")) return value;
            var changed = new DartSymbolProbe(original.id(), original.offset(), original.length(), original.expectedSymbolName(),
                    original.expectedLibraryUri(), outsideRoot, original.expectedTargetKind(), original.staticTypeProbe());
            return accepted(changed, outsideTarget);
        }).toList();
        var rejected = rootTicket.accept(analysis(rootTicket, forgedRoot));
        assertAnalyzedRejected(rejected, PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        assertAnalyzedRejected(rejected, PairSaveEvidenceDiagnostic.Code.UNTRUSTED_PROBE_ROOT);
        assertAnalyzedRejected(rejected, PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
    }

    @Test
    void dartUiNavigationIsRevalidatedWhenBindingAfterCandidateAnalysis() throws Exception {
        DartUiFixture ui = dartUiFixture();
        var ticket = dartUiTicket(ui);
        var analyzed = ticket.accept(analysis(ticket, dartUiEvidence(ui, ticket)));
        assertTrue(analyzed.ready(), () -> analyzed.diagnostics().toString());
        Files.delete(ui.uiTarget());
        var bound = PairSaveEvidenceGate.bindApplied(analyzed.analyzedOptional().orElseThrow(), ui.fixture().live());
        assertFalse(bound.ready());
        assertEquals(ticket.request().symbolProbes().stream()
                .filter(probe -> probe.expectedLibraryUri().equals("dart:ui"))
                .map(probe -> "analysis.symbolEvidence." + probe.id() + ".target").toList(),
                bound.diagnostics().stream().map(PairSaveEvidenceDiagnostic::subject).toList());
        assertTrue(bound.diagnostics().stream().allMatch(diagnostic ->
                diagnostic.code() == PairSaveEvidenceDiagnostic.Code.PATH_VALIDATION_FAILED),
                () -> bound.diagnostics().toString());
    }

    private DartUiFixture dartUiFixture() throws Exception {
        var devices = new PropertyValue.PointerDeviceKindSetValue(List.of(
                PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind.TOUCH,
                PropertyValue.PointerDeviceKindSetValue.PointerDeviceKind.MOUSE));
        Fixture fixture = fixture(Optional.empty(), true, List.of(), "flutter.widgets.GestureDetector", "",
                Map.of(new PropertyName("supportedDevices"), devices));
        Path sdk = fixture.flutterLib().getParent().getParent().getParent();
        Path target = sdk.resolve("bin/cache/pkg/sky_engine/lib/ui/pointer.dart");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "enum PointerDeviceKind { touch, mouse }\n");
        return new DartUiFixture(fixture, sdk, target);
    }

    private PairCandidateAnalysisTicket dartUiTicket(DartUiFixture ui) throws IOException {
        Fixture fixture = ui.fixture();
        return PairSaveEvidenceGate.prepareAnalysis(fixture.current(), fixture.prepared(), fixture.projectRoot(),
                fixture.dartFile(), DartCandidateWarningPolicy.ALLOW, ui.sdk());
    }

    private static List<DartSymbolEvidence> dartUiEvidence(DartUiFixture ui, PairCandidateAnalysisTicket ticket) {
        return ticket.request().symbolProbes().stream().map(probe -> accepted(probe,
                probe.expectedLibraryUri().equals("dart:ui") ? ui.uiTarget() : ui.fixture().frameworkFile())).toList();
    }

    private record DartUiFixture(Fixture fixture, Path sdk, Path uiTarget) {}

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
    void scaffoldAnonymousBuilderRequiresExactSignatureAndUnchangedProjectNavigation() throws Exception {
        String signature = "Widget? Function(BuildContext, Animation<double>)";
        for (boolean imported : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredScrim", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.Scaffold", "bottomSheetScrimBuilder");
            var ticket = ticket(fixture, fixture.current());
            var builder = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals("configuredScrim"))
                    .findFirst().orElseThrow();
            assertEquals(signature, builder.staticTypeProbe().orElseThrow().expectedDartType());
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", builder.expectedLibraryUri());
            assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), builder.expectedTargetRoot());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
            var fresh = ticket(fixture, fixture.current());
            var original = builder.staticTypeProbe().orElseThrow();
            var wrongType = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(), original.importInsertionOffset(),
                    original.statementInsertionOffset(), "WidgetBuilder", original.expectedTypeLibraryUri());
            var forged = new DartSymbolProbe(builder.id(), builder.offset(), builder.length(), builder.expectedSymbolName(),
                    builder.expectedLibraryUri(), builder.expectedTargetRoot(), builder.expectedTargetKind(), Optional.of(wrongType));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                            Optional.of(new DartStaticTypeEvidence(wrongType, true, Optional.empty()))) : value).toList();
            assertAnalyzedRejected(fresh.accept(analysis(fresh, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        }
    }

    @Test
    void appBarPredicateRequiresExactSignatureAndUnchangedProjectNavigation() throws Exception {
        String signature = "ScrollNotificationPredicate";
        for (boolean imported : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredPredicate", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.AppBar", "notificationPredicate");
            var ticket = ticket(fixture, fixture.current());
            var builder = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals("configuredPredicate"))
                    .findFirst().orElseThrow();
            assertEquals(signature, builder.staticTypeProbe().orElseThrow().expectedDartType());
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", builder.expectedLibraryUri());
            assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), builder.expectedTargetRoot());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
            var fresh = ticket(fixture, fixture.current());
            var original = builder.staticTypeProbe().orElseThrow();
            var wrongType = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(), original.importInsertionOffset(),
                    original.statementInsertionOffset(), "WidgetBuilder", original.expectedTypeLibraryUri());
            var forged = new DartSymbolProbe(builder.id(), builder.offset(), builder.length(), builder.expectedSymbolName(),
                    builder.expectedLibraryUri(), builder.expectedTargetRoot(), builder.expectedTargetKind(), Optional.of(wrongType));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                            Optional.of(new DartStaticTypeEvidence(wrongType, true, Optional.empty()))) : value).toList();
            assertAnalyzedRejected(fresh.accept(analysis(fresh, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        }
    }

    @Test
    void elevatedLayerBuildersRequireExactSignaturesAndUnchangedProjectNavigation() throws Exception {
        String signature = "ButtonLayerBuilder";
        for (String property : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) for (boolean imported : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredLayer", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.ElevatedButton", property);
            var ticket = ticket(fixture, fixture.current());
            var builder = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals("configuredLayer"))
                    .findFirst().orElseThrow();
            assertEquals(signature, builder.staticTypeProbe().orElseThrow().expectedDartType());
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", builder.expectedLibraryUri());
            assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), builder.expectedTargetRoot());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
            var fresh = ticket(fixture, fixture.current());
            var original = builder.staticTypeProbe().orElseThrow();
            var wrongType = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(), original.importInsertionOffset(),
                    original.statementInsertionOffset(), "WidgetBuilder", original.expectedTypeLibraryUri());
            var forged = new DartSymbolProbe(builder.id(), builder.offset(), builder.length(), builder.expectedSymbolName(),
                    builder.expectedLibraryUri(), builder.expectedTargetRoot(), builder.expectedTargetKind(), Optional.of(wrongType));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                            Optional.of(new DartStaticTypeEvidence(wrongType, true, Optional.empty()))) : value).toList();
            assertAnalyzedRejected(fresh.accept(analysis(fresh, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        }
    }

    @Test
    void textFieldBuildersRequireExactNullableSignaturesAndUnchangedProjectNavigation() throws Exception {
        for (String property : List.of("buildCounter", "contextMenuBuilder")) for (boolean imported : List.of(false, true)) {
            String signature = property.equals("buildCounter") ? "InputCounterWidgetBuilder?" : "EditableTextContextMenuBuilder?";
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredBuilder", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.TextField", property);
            var ticket = ticket(fixture, fixture.current());
            var builder = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals("configuredBuilder"))
                    .findFirst().orElseThrow();
            assertEquals(signature, builder.staticTypeProbe().orElseThrow().expectedDartType());
            assertEquals(property.equals("buildCounter") ? "package:flutter/material.dart" : "package:flutter/widgets.dart",
                    builder.staticTypeProbe().orElseThrow().expectedTypeLibraryUri());
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", builder.expectedLibraryUri());
            assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), builder.expectedTargetRoot());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
            var fresh = ticket(fixture, fixture.current());
            var original = builder.staticTypeProbe().orElseThrow();
            var wrongType = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(), original.importInsertionOffset(),
                    original.statementInsertionOffset(), "WidgetBuilder", original.expectedTypeLibraryUri());
            var forged = new DartSymbolProbe(builder.id(), builder.offset(), builder.length(), builder.expectedSymbolName(),
                    builder.expectedLibraryUri(), builder.expectedTargetRoot(), builder.expectedTargetKind(), Optional.of(wrongType));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                            Optional.of(new DartStaticTypeEvidence(wrongType, true, Optional.empty()))) : value).toList();
            assertAnalyzedRejected(fresh.accept(analysis(fresh, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
        }
    }

    @Test
    void listViewItemExtentBuilderRequiresExactNullableProofAndUnchangedProjectNavigation() throws Exception {
        for (boolean imported : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredExtent", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
            Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.widgets.ListView", "itemExtentBuilder");
            var ticket = ticket(fixture, fixture.current());
            var builder = ticket.request().symbolProbes().stream().filter(probe -> probe.expectedSymbolName().equals("configuredExtent"))
                    .findFirst().orElseThrow();
            var original = builder.staticTypeProbe().orElseThrow();
            assertEquals("ItemExtentBuilder?", original.expectedDartType());
            // Widgets owns ListView; the analyzer resolves the typedef through its fixed Rendering import.
            assertEquals("package:flutter/widgets.dart", original.expectedTypeLibraryUri());
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", builder.expectedLibraryUri());
            assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), builder.expectedTargetRoot());
            assertTrue(original.sourceTypeOverride().isEmpty());
            assertTrue(original.sourceTypeBound().isEmpty());
            var accepted = ticket.accept(analysis(ticket, fixture.acceptedEvidence()));
            assertTrue(accepted.ready(), accepted.diagnostics().toString());
            assertTrue(PairSaveEvidenceGate.bindApplied(accepted.analyzedOptional().orElseThrow(), fixture.live()).ready());
            var fresh = ticket(fixture, fixture.current());
            var wrongType = new DartStaticTypeProbe(original.expressionOffset(), original.expressionLength(), original.importInsertionOffset(),
                    original.statementInsertionOffset(), "ItemExtentBuilder", original.expectedTypeLibraryUri());
            var forged = new DartSymbolProbe(builder.id(), builder.offset(), builder.length(), builder.expectedSymbolName(),
                    builder.expectedLibraryUri(), builder.expectedTargetRoot(), builder.expectedTargetKind(), Optional.of(wrongType));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? new DartSymbolEvidence(forged, value.targets(), true, Optional.empty(),
                            Optional.of(new DartStaticTypeEvidence(wrongType, true, Optional.empty()))) : value).toList();
            assertAnalyzedRejected(fresh.accept(analysis(fresh, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            Path outsideTarget = Files.createDirectories(temporaryDirectory.resolve("outside-project")).resolve("extent.dart");
            Files.writeString(outsideTarget, "Object configuredExtent = Object();\n");
            var navigationTicket = ticket(fixture, fixture.current());
            var navigation = fixture.acceptedEvidence().stream().map(value -> value.probe().equals(builder)
                    ? accepted(value.probe(), outsideTarget) : value).toList();
            assertAnalyzedRejected(navigationTicket.accept(analysis(navigationTicket, navigation)),
                    PairSaveEvidenceDiagnostic.Code.UNTRUSTED_NAVIGATION_TARGET);
        }
    }

    @Test
    void tooltipThemeDataReferencesUseMaterialProofAndRejectStaleWidgetsEvidence() throws Exception {
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredTheme", member ? Optional.of("value") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            Fixture fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.TooltipTheme", "data",
                    Map.of(new PropertyName("data"), reference));
            PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
            var typed = ticket.request().symbolProbes().stream().flatMap(probe -> probe.staticTypeProbe().stream()).toList();
            assertEquals(1, typed.size());
            assertEquals("TooltipThemeData", typed.getFirst().expectedDartType());
            assertEquals("package:flutter/material.dart", typed.getFirst().expectedTypeLibraryUri());
            DartSymbolEvidence original = fixture.acceptedEvidence().stream()
                    .filter(value -> value.probe().staticTypeProbe().isPresent()).findFirst().orElseThrow();
            var probe = original.probe(); var type = probe.staticTypeProbe().orElseThrow();
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", probe.expectedLibraryUri());
            var staleType = new DartStaticTypeProbe(type.expressionOffset(), type.expressionLength(), type.importInsertionOffset(),
                    type.statementInsertionOffset(), type.expectedDartType(), "package:flutter/widgets.dart");
            var staleProbe = new DartSymbolProbe(probe.id(), probe.offset(), probe.length(), probe.expectedSymbolName(),
                    probe.expectedLibraryUri(), probe.expectedTargetRoot(), probe.expectedTargetKind(), Optional.of(staleType));
            var staleEvidence = new DartSymbolEvidence(staleProbe, original.targets(), true, Optional.empty(),
                    Optional.of(new DartStaticTypeEvidence(staleType, true, Optional.empty())));
            var evidence = fixture.acceptedEvidence().stream().map(value -> value == original ? staleEvidence : value).toList();
            assertAnalyzedRejected(ticket.accept(analysis(ticket, evidence)), PairSaveEvidenceDiagnostic.Code.GENERATED_SYMBOL_PROBE_SET_MISMATCH);
            var exactTicket = ticket(fixture, fixture.current());
            assertTrue(exactTicket.accept(analysis(exactTicket, fixture.acceptedEvidence())).ready());
        }
    }

    @Test
    void animatedIconReferencesUseMaterialProofWithoutChangingNavigationRoots() throws Exception {
        for (boolean imported : List.of(false, true)) for (boolean member : List.of(false, true)) for (boolean factory : List.of(false, true)) {
            var reference = new PropertyValue.DartObjectReferenceValue(
                    imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                    "configuredIcon", member ? Optional.of("value") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
            Fixture fixture = fixture(Optional.empty(), true, List.of(), "flutter.material.AnimatedIcon", "icon",
                    Map.of(new PropertyName("icon"), reference, new PropertyName("progress"), new PropertyValue.DoubleValue(java.math.BigDecimal.ZERO)));
            PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
            var typed = ticket.request().symbolProbes().stream().flatMap(probe -> probe.staticTypeProbe().stream()).toList();
            assertEquals(1, typed.size());
            assertEquals("AnimatedIconData", typed.getFirst().expectedDartType());
            assertEquals("package:flutter/material.dart", typed.getFirst().expectedTypeLibraryUri());
            var probe = ticket.request().symbolProbes().stream().filter(value -> value.staticTypeProbe().isPresent()).findFirst().orElseThrow();
            assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", probe.expectedLibraryUri());
            assertTrue(ticket.accept(analysis(ticket, fixture.acceptedEvidence())).ready());
        }
    }

    @Test
    void textButtonMaterialAndWidgetsReferencesShareExactProofContext() throws Exception {
        for (String property : List.of("onPressed", "onLongPress", "onHover", "onFocusChange",
                "focusNode", "statesController", "style", "styleBackgroundBuilder", "styleForegroundBuilder")) {
            for (boolean imported : List.of(false, true)) {
                var reference = new PropertyValue.DartObjectReferenceValue(
                        imported ? Optional.of("package:" + PROJECT_PACKAGE_NAME + "/clippers.dart") : Optional.empty(),
                        "configuredFunction", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
                Fixture fixture = fixture(Optional.of(reference), true, List.of(), "flutter.material.TextButton", property);
                PairCandidateAnalysisTicket ticket = ticket(fixture, fixture.current());
                var typed = ticket.request().symbolProbes().stream().flatMap(probe -> probe.staticTypeProbe().stream()).toList();
                assertEquals(3, typed.size(), property);
                String library = property.startsWith("style") ? "package:flutter/material.dart" : "package:flutter/widgets.dart";
                assertEquals(List.of(library), typed.stream().map(value -> value.expectedTypeLibraryUri()).distinct().toList(), property);
                assertTrue(typed.stream().anyMatch(value -> value.expectedDartType().equals("Animation<Color?>")));
                assertTrue(typed.stream().anyMatch(value -> value.expectedDartType().equals("CustomClipper<RRect>")));
                DartSymbolProbe configured = ticket.request().symbolProbes().stream()
                        .filter(probe -> probe.expectedSymbolName().equals("configuredFunction")).findFirst().orElseThrow();
                assertEquals(imported ? "package:" + PROJECT_PACKAGE_NAME + "/clippers.dart" : "project:current", configured.expectedLibraryUri());
                assertEquals(fixture.projectRoot().resolve("lib").toRealPath(), configured.expectedTargetRoot());
                PairAnalyzedCandidateResult analyzed = ticket.accept(analysis(ticket, fixture.acceptedEvidence()));
                assertTrue(analyzed.ready(), () -> property + ": " + analyzed.diagnostics());
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
        return fixture(projectReference, createProjectLibrary, declaredPackages,
                widgetType, propertyName, Map.of());
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            boolean createProjectLibrary,
            List<DeclaredPackage> declaredPackages,
            String widgetType,
            String propertyName,
            Map<PropertyName, PropertyValue> radioProperties) throws Exception {
        return fixture(projectReference, createProjectLibrary, declaredPackages,
                widgetType, propertyName, radioProperties, WidgetClassKind.STATELESS);
    }

    private Fixture fixture(
            Optional<PropertyValue.DartObjectReferenceValue> projectReference,
            boolean createProjectLibrary,
            List<DeclaredPackage> declaredPackages,
            String widgetType,
            String propertyName,
            Map<PropertyName, PropertyValue> radioProperties, WidgetClassKind kind) throws Exception {
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
        DartSourceDescriptor seedDescriptor = withKind(descriptor("", ""), kind);
        GeneratedDartRegions generatedBefore = generator.generate(
                document(seedDescriptor, "before"),
                BuiltInWidgetCatalog.getDefault()).generated().orElseThrow();
        DartSourceDescriptor baselineDescriptor = withKind(descriptor(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload()), kind);
        DesignerDocument baselineDocument = document(
                baselineDescriptor, "before");
        DartGenerationResult baselineGeneration = generator.generate(
                baselineDocument, BuiltInWidgetCatalog.getDefault());
        byte[] baselineSource = sourceBytes(
                generatedBefore.imports().payload(),
                generatedBefore.build().payload(), kind);
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
                                        projectReference, widgetType, propertyName, radioProperties),
                                BuiltInWidgetCatalog.getDefault()))
                .plan()
                .orElseThrow();
        DesignerDocument prospective = prospectiveDocument(
                transition.prospectiveDescriptor(), "after", projectReference,
                widgetType, propertyName, radioProperties);
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
            String propertyName,
            Map<PropertyName, PropertyValue> radioProperties) {
        if (widgetType.equals("flutter.material.TimePickerDialog") || widgetType.equals("flutter.material.InputDatePickerFormField") || widgetType.equals("flutter.material.CalendarDatePicker") || widgetType.equals("flutter.material.DateRangePickerDialog") || widgetType.equals("flutter.material.DatePickerDialog") || widgetType.equals("flutter.widgets.ValueListenableBuilder") || widgetType.equals("flutter.widgets.TweenAnimationBuilder")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    radioProperties, Map.of()));
        }
        if (widgetType.equals("flutter.widgets.SliverFloatingHeader")) {
            var def=BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(widgetType)).orElseThrow();
            var seed=dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory.create(def,StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"));
            var sliver=new WidgetNode(seed.id(),seed.type(),radioProperties,seed.slots());
            var viewport=new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                    new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                    Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(List.of(sliver))));
            return new DesignerDocument(DOCUMENT_ID,descriptor,viewport);
        }
        if (widgetType.equals("flutter.widgets.SliverAnimatedOpacity")) {
            var properties=new java.util.LinkedHashMap<PropertyName,PropertyValue>(radioProperties);
            properties.put(new PropertyName("opacity"),new PropertyValue.DoubleValue(java.math.BigDecimal.ONE));
            var sliver=new WidgetNode(StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId(widgetType),properties,Map.of());
            var viewport=new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                new WidgetTypeId("flutter.widgets.CustomScrollView"),Map.of(),
                Map.of(new SlotName("slivers"),new WidgetSlot.ListSlot(List.of(sliver))));
            return new DesignerDocument(DOCUMENT_ID,descriptor,viewport);
        }
        if (widgetType.equals("flutter.material.AnimatedIcon") || widgetType.equals("flutter.widgets.FadeInImage")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    radioProperties, Map.of()));
        }
        if (widgetType.equals("flutter.material.TooltipTheme")) {
            var child = new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                    new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue(text)), Map.of());
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    radioProperties, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))));
        }
        if (widgetType.equals("flutter.material.Tooltip")) {
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>(radioProperties);
            properties.put(new PropertyName("message"), new PropertyValue.StringValue(text));
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    properties, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())));
        }
        if (widgetType.equals("flutter.material.ExpansionTile")) {
            var title = new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                    new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue(text)), Map.of());
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    radioProperties, Map.of(new SlotName("title"), WidgetSlot.SingleSlot.of(title))));
        }
        if (widgetType.equals("flutter.widgets.GestureDetector")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    radioProperties, Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())));
        }
        if (widgetType.equals("flutter.material.Radio") || widgetType.equals("flutter.widgets.RadioGroup")
                || widgetType.equals("flutter.widgets.NotificationListener")) {
            var slots = !widgetType.equals("flutter.material.Radio")
                    ? Map.of(new SlotName("child"), (WidgetSlot) new WidgetSlot.SingleSlot(Optional.of(
                            new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                                    new WidgetTypeId("flutter.widgets.Text"),
                                    Map.of(new PropertyName("data"), new PropertyValue.StringValue(text)), Map.of()))))
                    : Map.<SlotName, WidgetSlot>of();
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                    new WidgetTypeId(widgetType), radioProperties, slots));
        }
        if (projectReference.isEmpty()) {
            return document(descriptor, text);
        }
        if (widgetType.equals("flutter.material.ElevatedButton")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    Map.of(new PropertyName(propertyName), projectReference.orElseThrow()),
                    Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty())));
        }
        if (widgetType.equals("flutter.material.AppBar") || widgetType.equals("flutter.material.TextField")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    Map.of(new PropertyName(propertyName), projectReference.orElseThrow()), Map.of()));
        }
        if (widgetType.equals("flutter.widgets.ListView")) {
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    Map.of(new PropertyName(propertyName), projectReference.orElseThrow()),
                    Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of()))));
        }
        if (widgetType.equals("flutter.material.Scaffold")) {
            var body = new WidgetNode(StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"), new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(new PropertyName("data"), new PropertyValue.StringValue(text)), Map.of());
            return new DesignerDocument(DOCUMENT_ID, descriptor, new WidgetNode(
                    StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), new WidgetTypeId(widgetType),
                    Map.of(new PropertyName(propertyName), projectReference.orElseThrow()),
                    Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(body))));
        }
        if (widgetType.equals("flutter.material.RefreshIndicator") || widgetType.equals("flutter.material.TextButton")) {
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
            var properties = new java.util.LinkedHashMap<PropertyName, PropertyValue>();
            properties.put(new PropertyName(propertyName), projectReference.orElseThrow());
            properties.put(new PropertyName("variant"), new PropertyValue.StringValue(
                    widgetType.equals("flutter.material.TextButton") ? "standard"
                            : propertyName.equals("onStatusChange") ? "noSpinner" : "material"));
            if (widgetType.equals("flutter.material.TextButton")) properties.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(true));
            var refresh = new WidgetNode(StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                    new WidgetTypeId(widgetType), properties,
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
        return sourceBytes(imports, build, WidgetClassKind.STATELESS);
    }

    private static DartSourceDescriptor withKind(DartSourceDescriptor source, WidgetClassKind kind) {
        return new DartSourceDescriptor(source.dartFile(), source.className(), kind,
                source.generatorVersion(), source.managedRegions());
    }

    private static byte[] sourceBytes(String imports, String build, WidgetClassKind kind) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + (kind == WidgetClassKind.STATELESS ? "class HomePage extends StatelessWidget {\n"
                        : "class HomePage extends StatefulWidget {\n"
                        + "  const HomePage({super.key});\n"
                        + "  @override\n  State<HomePage> createState() => _HomePageState();\n}\n\n"
                        + "class _HomePageState extends State<HomePage> {\n")
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

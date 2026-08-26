package dev.flutter.netbeans.designer.generation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityLimits;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionDiagnosticCode;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.ValidationSeverity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DartSourceTransitionPlannerTest {
    private static final String IMPORTS_A = "import 'package:flutter/widgets.dart';\n";
    private static final String IMPORTS_B = "import 'package:flutter/material.dart';\n";
    private static final String BUILD_A = build("return const SizedBox();");
    private static final String BUILD_B = build("return const Center(child: Text('new')); ");

    @Test
    void preparesAnAtomicOldToNewTransitionAndPreservesDirtyUserBytes() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] onDisk = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                onDisk, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        String manual = "// ручна зміна поза guards 😀\n";
        byte[] live = sourceBytes(IMPORTS_A, BUILD_A, manual, "\n", false);
        byte[] liveBefore = live.clone();
        DartGenerationResult prospective = success(generated(IMPORTS_B, BUILD_B));

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline, live, descriptor, prospective);

        assertEquals(DartSourceTransitionStatus.READY, result.status());
        assertTrue(result.diagnostics().isEmpty());
        var plan = result.plan().orElseThrow();
        assertSame(baseline, plan.baseline());
        assertSame(prospective, plan.generation());
        assertEquals(hash(IMPORTS_B),
                plan.prospectiveDescriptor().managedRegions().imports().sha256());
        assertEquals(hash(BUILD_B),
                plan.prospectiveDescriptor().managedRegions().build().sha256());
        assertEquals(descriptor.generatorVersion(),
                plan.prospectiveDescriptor().generatorVersion());
        assertTrue(plan.candidateIntegrity().onDiskDeclaredMatch());
        String candidate = new String(plan.candidateBytes(), StandardCharsets.UTF_8);
        assertTrue(candidate.contains(manual));
        assertTrue(candidate.contains(IMPORTS_B));
        assertTrue(candidate.contains(BUILD_B));
        assertFalse(candidate.contains(IMPORTS_A));
        assertFalse(candidate.contains(BUILD_A));
        assertArrayEquals(
                sourceBytes(IMPORTS_B, BUILD_B, manual, "\n", false),
                plan.candidateBytes(),
                "UTF-8 byte offsets must preserve every user-owned byte");
        assertArrayEquals(liveBefore, live, "planning must not mutate the live input");

        byte[] firstCopy = plan.candidateBytes();
        firstCopy[0] ^= 0x7F;
        assertFalse(firstCopy[0] == plan.candidateBytes()[0],
                "candidate bytes must be clone-safe");
    }

    @Test
    void reportsNoChangesWithoutPublishingAWriteCandidate() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline,
                source,
                descriptor,
                success(generated(IMPORTS_A, BUILD_A)));

        assertEquals(DartSourceTransitionStatus.NO_CHANGES, result.status());
        assertTrue(result.plan().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void rejectsAReadOnlyBaselineThatWasNotAThreeWayMatch() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult conflicting = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_B), scanner);

        var result = new DartSourceTransitionPlanner(scanner).plan(
                conflicting,
                source,
                descriptor,
                success(generated(IMPORTS_B, BUILD_B)));

        assertEquals(DartSourceTransitionStatus.CONFLICT, result.status());
        assertTrue(result.plan().isEmpty());
        assertEquals(DartSourceTransitionDiagnosticCode.BASELINE_NOT_THREE_WAY_MATCH,
                result.diagnostics().getFirst().code());
    }

    @Test
    void rejectsDescriptorSubstitutionAgainstAValidBaseline() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        DartSourceDescriptor substituted = descriptor(IMPORTS_A, BUILD_B);

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline,
                source,
                substituted,
                success(generated(IMPORTS_B, BUILD_B)));

        assertEquals(DartSourceTransitionStatus.CONFLICT, result.status());
        assertEquals(DartSourceTransitionDiagnosticCode.BASELINE_DESCRIPTOR_MISMATCH,
                result.diagnostics().getFirst().code());
    }

    @Test
    void rejectsLiveManagedRegionDriftEvenWhenTheDiskBaselineIsValid() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        byte[] driftedLive = sourceBytes(IMPORTS_A, BUILD_B, "", "\n", false);

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline,
                driftedLive,
                descriptor,
                success(generated(IMPORTS_B, BUILD_B)));

        assertEquals(DartSourceTransitionStatus.CONFLICT, result.status());
        assertEquals(DartSourceTransitionDiagnosticCode.LIVE_SOURCE_CONFLICT,
                result.diagnostics().getFirst().code());
        assertTrue(result.plan().isEmpty());
    }

    @Test
    void leavesBomAndCrLfReadableButExplicitlyNotWritable() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        byte[] crlf = sourceBytes(IMPORTS_A, BUILD_A, "", "\r\n", false);
        DartThreeWayIntegrityResult crlfBaseline = baseline(
                crlf, descriptor, generated(IMPORTS_A, BUILD_A), scanner);

        var crlfResult = new DartSourceTransitionPlanner(scanner).plan(
                crlfBaseline,
                crlf,
                descriptor,
                success(generated(IMPORTS_B, BUILD_B)));

        assertEquals(DartSourceTransitionStatus.UNSUPPORTED, crlfResult.status());
        assertEquals(DartSourceTransitionDiagnosticCode.WRITABLE_SOURCE_EOL_UNSUPPORTED,
                crlfResult.diagnostics().getFirst().code());

        byte[] bom = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", true);
        DartThreeWayIntegrityResult bomBaseline = baseline(
                bom, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        var bomResult = new DartSourceTransitionPlanner(scanner).plan(
                bomBaseline,
                bom,
                descriptor,
                success(generated(IMPORTS_B, BUILD_B)));

        assertEquals(DartSourceTransitionStatus.UNSUPPORTED, bomResult.status());
        assertEquals(DartSourceTransitionDiagnosticCode.WRITABLE_SOURCE_BOM_UNSUPPORTED,
                bomResult.diagnostics().getFirst().code());
    }

    @Test
    void preservesAtomicGenerationFailureWithoutCandidateBytes() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        DartGenerationResult failed = failedGeneration();

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline, source, descriptor, failed);

        assertEquals(DartSourceTransitionStatus.UNAVAILABLE, result.status());
        assertSame(failed, result.generation());
        assertTrue(result.plan().isEmpty());
        assertEquals(DartSourceTransitionDiagnosticCode.GENERATION_UNAVAILABLE,
                result.diagnostics().getFirst().code());
    }

    @Test
    void rejectsAProspectivePayloadThatInjectsMarkerTopology() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        String injected = "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    // </netbeans-flutter-designer>\n"
                + "    return const SizedBox();\n"
                + "  }\n";

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline,
                source,
                descriptor,
                success(generated(IMPORTS_B, injected)));

        assertEquals(DartSourceTransitionStatus.CONFLICT, result.status());
        assertTrue(result.plan().isEmpty());
        assertEquals(DartSourceTransitionDiagnosticCode.GENERATED_CANDIDATE_INVALID,
                result.diagnostics().getFirst().code());
    }

    @Test
    void refusesAnOversizedCandidateBeforeAllocatingOrPublishingIt() {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A, "", "\n", false);
        DartSourceIntegrityLimits limits = new DartSourceIntegrityLimits(
                source.length + 32, 64, 32);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(limits);
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        String hugeBuild = "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    // " + "x".repeat(source.length + 128) + "\n"
                + "    return const SizedBox();\n"
                + "  }\n";

        var result = new DartSourceTransitionPlanner(scanner).plan(
                baseline,
                source,
                descriptor,
                success(generated(IMPORTS_B, hugeBuild)));

        assertEquals(DartSourceTransitionStatus.UNAVAILABLE, result.status());
        assertTrue(result.plan().isEmpty());
        assertEquals(DartSourceTransitionDiagnosticCode.GENERATED_CANDIDATE_TOO_LARGE,
                result.diagnostics().getFirst().code());
    }

    private static DartThreeWayIntegrityResult baseline(
            byte[] source,
            DartSourceDescriptor descriptor,
            GeneratedDartRegions generated,
            DartSourceIntegrityScanner scanner) {
        DartSourceIntegrityResult integrity = scanner.scan(source, descriptor);
        return new DartThreeWayIntegrityGate(scanner).evaluate(
                integrity, descriptor, success(generated));
    }

    private static DartGenerationResult success(GeneratedDartRegions generated) {
        return new DartGenerationResult(
                new ValidationResult(List.of()), Optional.of(generated), List.of());
    }

    private static DartGenerationResult failedGeneration() {
        ValidationIssue issue = new ValidationIssue(
                "test.invalid",
                ValidationSeverity.ERROR,
                "/root",
                Optional.empty(),
                "The test model is invalid.");
        DartGenerationDiagnostic diagnostic = new DartGenerationDiagnostic(
                DartGenerationDiagnosticCode.MODEL_INVALID,
                "/root",
                Optional.empty(),
                Optional.empty(),
                "The test model is invalid.");
        return new DartGenerationResult(
                new ValidationResult(List.of(issue)),
                Optional.empty(),
                List.of(diagnostic));
    }

    private static GeneratedDartRegions generated(String imports, String build) {
        return new GeneratedDartRegions(
                GeneratedDartRegion.create(DartManagedRegionId.IMPORTS, imports),
                GeneratedDartRegion.create(DartManagedRegionId.BUILD, build),
                new DartImportPlan(List.of()),
                "test-profile");
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

    private static String build(String statement) {
        return "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    " + statement + "\n"
                + "  }\n";
    }

    private static byte[] sourceBytes(
            String imports,
            String build,
            String userOwnedPrefix,
            String separator,
            boolean bom) {
        String lf = "// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + userOwnedPrefix
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + build
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
        byte[] body = lf.replace("\n", separator).getBytes(StandardCharsets.UTF_8);
        if (!bom) {
            return body;
        }
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        return withBom;
    }
}

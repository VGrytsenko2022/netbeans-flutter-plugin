package io.github.vgrytsenko2022.designer.generation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.source.DartManagedRegionHashing;
import io.github.vgrytsenko2022.designer.source.DartManagedRegionSnapshot;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityDiagnosticCode;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityGate;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartThreeWayIntegrityStatus;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import io.github.vgrytsenko2022.designer.validation.ValidationIssue;
import io.github.vgrytsenko2022.designer.validation.ValidationSeverity;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DartThreeWayIntegrityGateTest {

    private static final String IMPORTS = "import 'package:flutter/widgets.dart';\n";
    private static final String BUILD_A = build("return const SizedBox();");
    private static final String BUILD_B = build("return const Text('generated');");
    private static final String BUILD_C = build("return const Center();");

    @Test
    void acceptsOnlyACompleteActualDeclaredGeneratedMatchAndRetainsExactBaseline() {
        GeneratedDartRegions generated = generated(IMPORTS, BUILD_A);
        DartSourceDescriptor descriptor = descriptor(IMPORTS, BUILD_A);
        byte[] source = sourceBytes(IMPORTS, BUILD_A, "\n", false);
        byte[] before = source.clone();
        DartSourceIntegrityResult actual = scan(source, descriptor);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, descriptor, success(generated));

        assertEquals(DartThreeWayIntegrityStatus.ON_DISK_THREE_WAY_MATCH,
                result.status());
        assertTrue(result.onDiskThreeWayMatch());
        assertEquals(List.of("imports", "build"), result.comparisons().stream()
                .map(comparison -> comparison.id()).toList());
        assertTrue(result.comparisons().stream().allMatch(
                comparison -> comparison.allNormalizedHashesMatch()));
        assertTrue(result.diagnostics().isEmpty());
        assertSame(actual, result.source());
        assertArrayEquals(before, result.original().orElseThrow().copyBytes());
        assertArrayEquals(before, source, "the pure gate must not mutate its input");
    }

    @Test
    void treatsCrLfAndLeadingBomAsNormalizedMatchWithoutChangingExactBytes() {
        GeneratedDartRegions generated = generated(IMPORTS, BUILD_A);
        DartSourceDescriptor descriptor = descriptor(IMPORTS, BUILD_A);
        byte[] source = sourceBytes(IMPORTS, BUILD_A, "\r\n", true);
        DartSourceIntegrityResult actual = scan(source, descriptor);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, descriptor, success(generated));

        assertTrue(result.onDiskThreeWayMatch(), () -> result.diagnostics().toString());
        assertArrayEquals(source, result.original().orElseThrow().copyBytes());
        assertEquals(0xEF, source[0] & 0xFF);
        assertTrue(new String(source, StandardCharsets.UTF_8).contains("\r\n"));
    }

    @Test
    void distinguishesActualFromMatchingDeclaredAndGeneratedHashes() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_B);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, success(generated(IMPORTS, BUILD_B)));

        assertEquals(DartThreeWayIntegrityStatus.CONFLICT, result.status());
        var build = result.comparison("build").orElseThrow();
        assertFalse(build.actualMatchesDeclared());
        assertTrue(build.declaredMatchesGenerated());
        assertTrue(result.diagnostics().isEmpty(),
                "actual/declared detail remains in the preserved scanner result");
        assertFalse(result.source().diagnostics().isEmpty());
    }

    @Test
    void reportsCurrentGeneratedModelDriftFromMatchingActualAndDeclaredHashes() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_A);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, success(generated(IMPORTS, BUILD_B)));

        assertEquals(DartThreeWayIntegrityStatus.CONFLICT, result.status());
        var build = result.comparison("build").orElseThrow();
        assertTrue(build.actualMatchesDeclared());
        assertFalse(build.declaredMatchesGenerated());
        assertEquals(
                DartThreeWayIntegrityDiagnosticCode.GENERATED_REGION_HASH_MISMATCH,
                result.diagnostics().getFirst().code());
        assertEquals("/source/managedRegions/build/sha256",
                result.diagnostics().getFirst().path());
    }

    @Test
    void doesNotMistakeMatchingActualAndGeneratedPayloadsForAThreeWayMatch() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_B);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, success(generated(IMPORTS, BUILD_A)));

        assertEquals(DartThreeWayIntegrityStatus.CONFLICT, result.status());
        var build = result.comparison("build").orElseThrow();
        assertFalse(build.actualMatchesDeclared());
        assertFalse(build.declaredMatchesGenerated());
        assertEquals(build.actual().orElseThrow().normalizedSha256(),
                build.generatedNormalizedSha256().orElseThrow());
    }

    @Test
    void retainsAllThreeDifferentHashesAsConcreteComparisonEvidence() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_B);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, success(generated(IMPORTS, BUILD_C)));

        assertEquals(DartThreeWayIntegrityStatus.CONFLICT, result.status());
        var build = result.comparison("build").orElseThrow();
        assertEquals(3, List.of(
                build.actual().orElseThrow().normalizedSha256(),
                build.declaredNormalizedSha256(),
                build.generatedNormalizedSha256().orElseThrow()).stream()
                .distinct().count());
        assertEquals(
                DartThreeWayIntegrityDiagnosticCode.GENERATED_REGION_HASH_MISMATCH,
                result.diagnostics().getFirst().code());
    }

    @Test
    void rejectsForgedDiagnosticFreeSourceEvidence() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_A);
        DartSourceIntegrityResult scanned = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);
        DartSourceIntegrityResult missingRegions = new DartSourceIntegrityResult(
                scanned.original(), List.of(), List.of());

        DartThreeWayIntegrityResult missing = gate().evaluate(
                missingRegions, declared, success(generated(IMPORTS, BUILD_A)));

        assertEquals(DartThreeWayIntegrityStatus.UNAVAILABLE, missing.status());
        assertEquals(DartThreeWayIntegrityDiagnosticCode.SOURCE_EVIDENCE_INCONSISTENT,
                missing.diagnostics().getFirst().code());

        List<DartManagedRegionSnapshot> forged = new ArrayList<>(scanned.regions());
        DartManagedRegionSnapshot imports = forged.getFirst();
        int outside = scanned.original().orElseThrow().size() + 10;
        forged.set(0, new DartManagedRegionSnapshot(
                imports.id(), outside, outside, imports.normalizedSha256()));
        DartSourceIntegrityResult outsideBaseline = new DartSourceIntegrityResult(
                scanned.original(), forged, List.of());

        DartThreeWayIntegrityResult outOfBounds = gate().evaluate(
                outsideBaseline, declared, success(generated(IMPORTS, BUILD_A)));
        assertEquals(DartThreeWayIntegrityStatus.UNAVAILABLE, outOfBounds.status());
        assertEquals(DartThreeWayIntegrityDiagnosticCode.SOURCE_EVIDENCE_INCONSISTENT,
                outOfBounds.diagnostics().getFirst().code());
    }

    @Test
    void rescansTheReconstructedCandidateAndRejectsInjectedMarkerTopology() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_A);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);
        String injected = "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    // </netbeans-flutter-designer>\n"
                + "    return const SizedBox();\n"
                + "  }\n";

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, success(generated(IMPORTS, injected)));

        assertEquals(DartThreeWayIntegrityStatus.CONFLICT, result.status());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code()
                == DartThreeWayIntegrityDiagnosticCode.GENERATED_CANDIDATE_INVALID));
        assertArrayEquals(
                sourceBytes(IMPORTS, BUILD_A, "\n", false),
                result.original().orElseThrow().copyBytes(),
                "candidate validation must not replace the exact on-disk baseline");
    }

    @Test
    void preservesAtomicGenerationFailureAndNeverPublishesAMatch() {
        DartSourceDescriptor declared = descriptor(IMPORTS, BUILD_A);
        DartSourceIntegrityResult actual = scan(
                sourceBytes(IMPORTS, BUILD_A, "\n", false), declared);
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
                "The model is invalid.");
        DartGenerationResult failed = new DartGenerationResult(
                new ValidationResult(List.of(issue)),
                Optional.empty(),
                List.of(diagnostic));

        DartThreeWayIntegrityResult result = gate().evaluate(
                actual, declared, failed);

        assertSame(failed, result.generation());
        assertEquals(DartThreeWayIntegrityStatus.UNAVAILABLE, result.status());
        assertTrue(result.comparisons().isEmpty());
        assertEquals(DartThreeWayIntegrityDiagnosticCode.GENERATION_UNAVAILABLE,
                result.diagnostics().getFirst().code());
    }

    private static DartThreeWayIntegrityGate gate() {
        return new DartThreeWayIntegrityGate();
    }

    private static DartSourceIntegrityResult scan(
            byte[] source,
            DartSourceDescriptor descriptor) {
        return new DartSourceIntegrityScanner().scan(source, descriptor);
    }

    private static DartGenerationResult success(GeneratedDartRegions generated) {
        return new DartGenerationResult(
                new ValidationResult(List.of()), Optional.of(generated), List.of());
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
            String separator,
            boolean bom) {
        String lf = "// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
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

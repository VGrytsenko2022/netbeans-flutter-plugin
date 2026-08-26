package dev.flutter.netbeans.designer.generation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationDiagnosticCode;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationPlanner;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationResult;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationStatus;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DesignerPairPreparationPlannerTest {
    private static final String IMPORTS_A =
            "import 'package:flutter/widgets.dart';\n";
    private static final String IMPORTS_B =
            "import 'package:flutter/material.dart';\n";
    private static final String BUILD_A = build("return const SizedBox();");
    private static final String BUILD_B =
            build("return const Center(child: Text('new')); ");
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void preparesCanonicalExactPairWithoutGrantingWriteAuthority() throws Exception {
        Fixture fixture = fixture();

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner()
                .prepare(
                        fixture.baselineFd(),
                        fixture.prospectiveDocument(),
                        fixture.transition());

        assertEquals(DesignerPairPreparationStatus.READY, result.status());
        assertTrue(result.diagnostics().isEmpty());
        PreparedDesignerPair pair = result.preparedPair().orElseThrow();
        assertSame(fixture.baselineFd(), pair.baselineFd());
        assertEquals(fixture.baselineDocument(), pair.baselineDocument());
        assertSame(fixture.prospectiveDocument(), pair.prospectiveDocument());
        assertSame(fixture.transition(), pair.dartTransition());
        assertEquals(
                fixture.transition().prospectiveDescriptor(),
                pair.prospectiveDocument().source());
        FdDecodeResult.Current decoded = (FdDecodeResult.Current) codec.decode(
                pair.prospectiveFd());
        assertEquals(fixture.prospectiveDocument(), decoded.document());
        assertArrayEquals(
                codec.encode(fixture.prospectiveDocument()).copyBytes(),
                pair.prospectiveFdBytes());
        assertArrayEquals(
                fixture.transition().baseline().original().orElseThrow().copyBytes(),
                pair.baselineDartBytes());
        assertArrayEquals(
                fixture.transition().liveSource().original().orElseThrow().copyBytes(),
                pair.liveDartBytes());
        assertArrayEquals(
                fixture.transition().candidateBytes(),
                pair.prospectiveDartBytes());
        assertFalse(pair.baselineFd().equals(pair.prospectiveFd()));
    }

    @Test
    void rejectsSubstitutedBaselineDocumentIdentity() throws Exception {
        Fixture fixture = fixture();
        DesignerDocument substituted = document(
                StableId.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                fixture.baselineDocument().source(),
                "before");
        OriginalFdBytes substitutedBytes = codec.encode(substituted);

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner()
                .prepare(
                        substitutedBytes,
                        fixture.prospectiveDocument(),
                        fixture.transition());

        assertEquals(DesignerPairPreparationStatus.CONFLICT, result.status());
        assertEquals(
                DesignerPairPreparationDiagnosticCode.DOCUMENT_ID_MISMATCH,
                result.diagnostics().getFirst().code());
        assertTrue(result.preparedPair().isEmpty());
    }

    @Test
    void rejectsBaselineWhoseDescriptorWasSubstituted() throws Exception {
        Fixture fixture = fixture();
        DartSourceDescriptor substituted = new DartSourceDescriptor(
                "other_page.dart",
                fixture.baselineDocument().source().className(),
                fixture.baselineDocument().source().widgetKind(),
                fixture.baselineDocument().source().generatorVersion(),
                fixture.baselineDocument().source().managedRegions());
        OriginalFdBytes substitutedBytes = codec.encode(document(
                DOCUMENT_ID, substituted, "before"));

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner()
                .prepare(
                        substitutedBytes,
                        fixture.prospectiveDocument(),
                        fixture.transition());

        assertEquals(DesignerPairPreparationStatus.CONFLICT, result.status());
        assertEquals(
                DesignerPairPreparationDiagnosticCode.BASELINE_DESCRIPTOR_MISMATCH,
                result.diagnostics().getFirst().code());
        assertTrue(result.preparedPair().isEmpty());
    }

    @Test
    void rejectsProspectiveDocumentWithAnyOtherDescriptor() throws Exception {
        Fixture fixture = fixture();
        DesignerDocument stale = document(
                DOCUMENT_ID, fixture.baselineDocument().source(), "after");

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner()
                .prepare(fixture.baselineFd(), stale, fixture.transition());

        assertEquals(DesignerPairPreparationStatus.CONFLICT, result.status());
        assertEquals(
                DesignerPairPreparationDiagnosticCode
                        .PROSPECTIVE_DESCRIPTOR_MISMATCH,
                result.diagnostics().getFirst().code());
        assertTrue(result.preparedPair().isEmpty());
    }

    @Test
    void refusesNoChangesTransitionResultWithoutInventingPairBytes() throws Exception {
        DartSourceDescriptor descriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source, descriptor, generated(IMPORTS_A, BUILD_A), scanner);
        DartSourceTransitionResult noChanges = new DartSourceTransitionPlanner(scanner)
                .plan(
                        baseline,
                        source,
                        descriptor,
                        success(generated(IMPORTS_A, BUILD_A)));
        assertEquals(DartSourceTransitionStatus.NO_CHANGES, noChanges.status());
        DesignerDocument unchanged = document(DOCUMENT_ID, descriptor, "before");

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner()
                .prepare(codec.encode(unchanged), unchanged, noChanges);

        assertEquals(DesignerPairPreparationStatus.NO_TRANSITION, result.status());
        assertEquals(
                DesignerPairPreparationDiagnosticCode.SOURCE_TRANSITION_REQUIRED,
                result.diagnostics().getFirst().code());
        assertTrue(result.preparedPair().isEmpty());
    }

    @Test
    void encodesDeterministicallyAndReturnsOnlyCloneSafeByteArrays() throws Exception {
        Fixture fixture = fixture();
        DesignerPairPreparationPlanner planner = new DesignerPairPreparationPlanner();
        PreparedDesignerPair first = planner.prepare(
                fixture.baselineFd(),
                fixture.prospectiveDocument(),
                fixture.transition()).preparedPair().orElseThrow();
        PreparedDesignerPair second = planner.prepare(
                fixture.baselineFd(),
                fixture.prospectiveDocument(),
                fixture.transition()).preparedPair().orElseThrow();

        assertArrayEquals(first.prospectiveFdBytes(), second.prospectiveFdBytes());
        byte[] fdCopy = first.prospectiveFdBytes();
        byte[] dartCopy = first.prospectiveDartBytes();
        byte[] baselineCopy = first.baselineFdBytes();
        fdCopy[0] ^= 0x7F;
        dartCopy[0] ^= 0x7F;
        baselineCopy[0] ^= 0x7F;
        assertFalse(fdCopy[0] == first.prospectiveFdBytes()[0]);
        assertFalse(dartCopy[0] == first.prospectiveDartBytes()[0]);
        assertFalse(baselineCopy[0] == first.baselineFdBytes()[0]);
    }

    @Test
    void appliesThePlannerLimitToAnExistingSnapshotBeforeDecoding() throws Exception {
        Fixture fixture = fixture();
        int maximum = fixture.baselineFd().size() - 1;
        FdCodecLimits limits = limitsWithMaximumDocumentBytes(maximum);

        DesignerPairPreparationResult result = new DesignerPairPreparationPlanner(
                new FdDocumentCodec(limits)).prepare(
                        fixture.baselineFd(),
                        fixture.prospectiveDocument(),
                        fixture.transition());

        assertEquals(DesignerPairPreparationStatus.UNAVAILABLE, result.status());
        assertEquals(
                DesignerPairPreparationDiagnosticCode.BASELINE_FD_TOO_LARGE,
                result.diagnostics().getFirst().code());
        assertTrue(result.preparedPair().isEmpty());
    }

    private Fixture fixture() throws Exception {
        DartSourceDescriptor baselineDescriptor = descriptor(IMPORTS_A, BUILD_A);
        byte[] source = sourceBytes(IMPORTS_A, BUILD_A);
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartThreeWayIntegrityResult baseline = baseline(
                source,
                baselineDescriptor,
                generated(IMPORTS_A, BUILD_A),
                scanner);
        byte[] live = ("// user-owned Привіт 😀\n"
                + new String(source, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        DartSourceTransitionResult transition = new DartSourceTransitionPlanner(scanner)
                .plan(
                        baseline,
                        live,
                        baselineDescriptor,
                        success(generated(IMPORTS_B, BUILD_B)));
        assertEquals(DartSourceTransitionStatus.READY, transition.status());
        DartSourceTransitionPlan plan = transition.plan().orElseThrow();
        DesignerDocument baselineDocument = document(
                DOCUMENT_ID, baselineDescriptor, "before");
        DesignerDocument prospectiveDocument = document(
                DOCUMENT_ID, plan.prospectiveDescriptor(), "after");
        return new Fixture(
                baselineDocument,
                codec.encode(baselineDocument),
                prospectiveDocument,
                plan);
    }

    private static DesignerDocument document(
            StableId documentId,
            DartSourceDescriptor descriptor,
            String text) {
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(text)),
                Map.of());
        return new DesignerDocument(documentId, descriptor, root);
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

    private static FdCodecLimits limitsWithMaximumDocumentBytes(int maximum) {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        return new FdCodecLimits(
                maximum,
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxNumberCharacters(),
                defaults.maxAbsoluteDecimalScale(),
                defaults.maxWidgetDepth(),
                defaults.maxWidgetNodes(),
                defaults.maxPropertiesPerWidget(),
                defaults.maxSlotsPerWidget(),
                defaults.maxListChildren(),
                defaults.maxExtensionKeysPerBag(),
                defaults.maxExtensionNestingDepth(),
                defaults.maxExtensionValues(),
                defaults.maxJsonObjectFields(),
                defaults.maxJsonArrayElements(),
                defaults.maxDiagnostics());
    }

    private record Fixture(
            DesignerDocument baselineDocument,
            OriginalFdBytes baselineFd,
            DesignerDocument prospectiveDocument,
            DartSourceTransitionPlan transition) {
    }
}

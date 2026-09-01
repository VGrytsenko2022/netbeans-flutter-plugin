package dev.flutter.netbeans.designer.copy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.template.DesignerFormTemplate;
import dev.flutter.netbeans.designer.template.DesignerFormTemplateFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DesignerPairCopyPlannerTest {
    private static final String ORIGINAL_FILE = "order_screen.dart";
    private static final String TARGET_FILE = "order_screen_copy.dart";

    private final FdDocumentCodec codec = new FdDocumentCodec();
    private final DesignerPairCopyPlanner planner =
            new DesignerPairCopyPlanner(codec);

    @Test
    void preparesCanonicalIndependentCopyAndPreservesEveryOtherField()
            throws Exception {
        OriginalFdBytes originalFd = starterFd();
        StableId targetDocumentId = StableId.random();

        DesignerPairCopyPlan plan = ready(planner.prepare(
                originalFd,
                ORIGINAL_FILE,
                TARGET_FILE,
                targetDocumentId)).plan();
        DesignerDocument original = plan.originalDocument();
        DesignerDocument target = plan.targetDocument();

        assertAll(
                () -> assertEquals(ORIGINAL_FILE, original.source().dartFile()),
                () -> assertEquals(TARGET_FILE, target.source().dartFile()),
                () -> assertEquals(targetDocumentId, plan.targetDocumentId()),
                () -> assertEquals(targetDocumentId, target.documentId()),
                () -> assertNotEquals(original.documentId(), target.documentId()),
                () -> assertEquals(original.schemaReference(), target.schemaReference()),
                () -> assertEquals(original.canvas(), target.canvas()),
                () -> assertEquals(original.root(), target.root()),
                () -> assertEquals(original.extensions(), target.extensions()),
                () -> assertEquals(original.source().className(), target.source().className()),
                () -> assertEquals(original.source().widgetKind(), target.source().widgetKind()),
                () -> assertEquals(
                        original.source().generatorVersion(),
                        target.source().generatorVersion()),
                () -> assertEquals(
                        original.source().managedRegions(),
                        target.source().managedRegions()),
                () -> assertEquals(originalFd, plan.originalFdBytes()),
                () -> assertNotEquals(plan.originalFdBytes(), plan.targetFdBytes()));

        FdDecodeResult.Current decodedTarget = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(plan.targetFdBytes()));
        assertAll(
                () -> assertEquals(target, decodedTarget.document()),
                () -> assertFalse(decodedTarget.migrated()),
                () -> assertEquals(
                        plan.targetFdBytes(),
                        codec.encode(decodedTarget.document())));
    }

    @Test
    void permitsSameDartFilenameForCopyIntoAnotherMirroredDirectory()
            throws Exception {
        StableId targetDocumentId = StableId.random();

        DesignerPairCopyPlan plan = ready(planner.prepare(
                starterFd(),
                ORIGINAL_FILE,
                ORIGINAL_FILE,
                targetDocumentId)).plan();

        assertAll(
                () -> assertEquals(
                        plan.originalDocument().source(),
                        plan.targetDocument().source()),
                () -> assertNotEquals(
                        plan.originalDocument().documentId(),
                        plan.targetDocument().documentId()),
                () -> assertEquals(targetDocumentId, plan.targetDocumentId()));
    }

    @Test
    void publicPlanRejectsChangesBeyondDocumentIdAndSourceDartFile()
            throws Exception {
        DesignerPairCopyPlan valid = ready(planner.prepare(
                starterFd(),
                ORIGINAL_FILE,
                TARGET_FILE,
                StableId.random())).plan();
        DesignerDocument original = valid.originalDocument();
        DesignerDocument target = valid.targetDocument();
        DartSourceDescriptor source = target.source();

        DesignerDocument changedClass = new DesignerDocument(
                target.schemaReference(),
                target.documentId(),
                new DartSourceDescriptor(
                        source.dartFile(),
                        "UnexpectedScreen",
                        source.widgetKind(),
                        source.generatorVersion(),
                        source.managedRegions()),
                target.canvas(),
                target.root(),
                target.extensions());
        OriginalFdBytes changedClassBytes = codec.encode(changedClass);
        OriginalFdBytes invalidOriginalBytes = OriginalFdBytes.copyOf(
                "{".getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairCopyPlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                valid.targetDocumentId(),
                                original,
                                changedClass,
                                valid.originalFdBytes(),
                                changedClassBytes)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairCopyPlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                valid.targetDocumentId(),
                                original,
                                target,
                                valid.originalFdBytes(),
                                changedClassBytes)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairCopyPlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                StableId.random(),
                                original,
                                target,
                                valid.originalFdBytes(),
                                valid.targetFdBytes())),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairCopyPlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                valid.targetDocumentId(),
                                original,
                                target,
                                invalidOriginalBytes,
                                valid.targetFdBytes())));
    }

    @Test
    void rejectsInvalidOriginalAndTargetFilenamesBeforeDecode()
            throws Exception {
        OriginalFdBytes originalFd = starterFd();

        DesignerPairCopyResult.Rejected invalidOriginal = rejected(planner.prepare(
                originalFd,
                "OrderScreen.dart",
                TARGET_FILE,
                StableId.random()));
        DesignerPairCopyResult.Rejected invalidTarget = rejected(planner.prepare(
                originalFd,
                ORIGINAL_FILE,
                "../order_screen_copy.dart",
                StableId.random()));

        assertAll(
                () -> assertEquals(
                        DesignerPairCopyResult.Code.INVALID_ORIGINAL_FILENAME,
                        invalidOriginal.code()),
                () -> assertEquals(
                        DesignerPairCopyResult.Code.INVALID_TARGET_FILENAME,
                        invalidTarget.code()));
    }

    @Test
    void rejectsSourceReferenceMismatch() throws Exception {
        DesignerPairCopyResult.Rejected result = rejected(planner.prepare(
                starterFd(),
                "other_screen.dart",
                TARGET_FILE,
                StableId.random()));

        assertEquals(
                DesignerPairCopyResult.Code.SOURCE_REFERENCE_MISMATCH,
                result.code());
    }

    @Test
    void rejectsOriginalDocumentIdAsTargetIdentity() throws Exception {
        OriginalFdBytes originalFd = starterFd();
        FdDecodeResult.Current current = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(originalFd));

        DesignerPairCopyResult.Rejected result = rejected(planner.prepare(
                originalFd,
                ORIGINAL_FILE,
                TARGET_FILE,
                current.document().documentId()));

        assertEquals(DesignerPairCopyResult.Code.SAME_DOCUMENT_ID, result.code());
    }

    @Test
    void rejectsInvalidModel() throws Exception {
        OriginalFdBytes invalid = OriginalFdBytes.copyOf(
                "{".getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        DesignerPairCopyResult.Rejected result = rejected(planner.prepare(
                invalid,
                ORIGINAL_FILE,
                TARGET_FILE,
                StableId.random()));

        assertEquals(DesignerPairCopyResult.Code.MODEL_NOT_CURRENT, result.code());
    }

    @Test
    void rejectsFutureModelWithoutRewritingIt() throws Exception {
        OriginalFdBytes current = starterFd();
        String currentJson = new String(current.copyBytes(), StandardCharsets.UTF_8);
        String futureJson = currentJson.replaceFirst(
                "\\\"schemaVersion\\\"\\s*:\\s*6",
                "\"schemaVersion\": 7");
        assertNotEquals(currentJson, futureJson);
        OriginalFdBytes future = OriginalFdBytes.copyOf(
                futureJson.getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        DesignerPairCopyResult.Rejected result = rejected(planner.prepare(
                future,
                ORIGINAL_FILE,
                TARGET_FILE,
                StableId.random()));

        assertAll(
                () -> assertEquals(
                        DesignerPairCopyResult.Code.MODEL_NOT_CURRENT,
                        result.code()),
                () -> assertEquals(
                        futureJson,
                        new String(future.copyBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsMigratedModelWithoutEncodingIt() throws Exception {
        OriginalFdBytes originalFd = starterFd();
        FdDecodeResult.Current current = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(originalFd));
        FdDecodeResult.Current migrated = new FdDecodeResult.Current(
                current.document(),
                current.sourceSchemaVersion(),
                true,
                current.original());
        DesignerPairCopyPlanner migratedPlanner = new DesignerPairCopyPlanner(
                codec,
                ignored -> migrated);

        DesignerPairCopyResult.Rejected result = rejected(migratedPlanner.prepare(
                originalFd,
                ORIGINAL_FILE,
                TARGET_FILE,
                StableId.random()));

        assertEquals(
                DesignerPairCopyResult.Code.MIGRATION_REQUIRED,
                result.code());
    }

    @Test
    void rejectsNullRequiredInputs() throws Exception {
        OriginalFdBytes originalFd = starterFd();

        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        planner.prepare(
                                null,
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                StableId.random())),
                () -> assertThrows(NullPointerException.class, () ->
                        planner.prepare(
                                originalFd,
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                null)));
    }

    private static DesignerPairCopyResult.Ready ready(
            DesignerPairCopyResult result) {
        return assertInstanceOf(DesignerPairCopyResult.Ready.class, result);
    }

    private static DesignerPairCopyResult.Rejected rejected(
            DesignerPairCopyResult result) {
        return assertInstanceOf(DesignerPairCopyResult.Rejected.class, result);
    }

    private static OriginalFdBytes starterFd() throws Exception {
        DesignerFormTemplate template = new DesignerFormTemplateFactory()
                .create(ORIGINAL_FILE, "OrderScreen");
        return OriginalFdBytes.copyOf(
                template.fdBytes(),
                FdCodecLimits.defaults());
    }
}

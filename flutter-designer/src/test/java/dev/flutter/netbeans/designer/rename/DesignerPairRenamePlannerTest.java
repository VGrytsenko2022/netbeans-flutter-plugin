package dev.flutter.netbeans.designer.rename;

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

class DesignerPairRenamePlannerTest {
    private static final String ORIGINAL_FILE = "order_screen.dart";
    private static final String TARGET_FILE = "renamed_screen.dart";

    private final FdDocumentCodec codec = new FdDocumentCodec();
    private final DesignerPairRenamePlanner planner =
            new DesignerPairRenamePlanner(codec);

    @Test
    void preparesCanonicalRenameAndPreservesEveryOtherModelField() throws Exception {
        OriginalFdBytes originalFd = starterFd();

        DesignerPairRenamePlan plan = ready(planner.prepare(
                originalFd, ORIGINAL_FILE, TARGET_FILE)).plan();
        DesignerDocument original = plan.originalDocument();
        DesignerDocument target = plan.targetDocument();

        assertAll(
                () -> assertEquals(ORIGINAL_FILE, original.source().dartFile()),
                () -> assertEquals(TARGET_FILE, target.source().dartFile()),
                () -> assertEquals(original.schemaReference(), target.schemaReference()),
                () -> assertEquals(original.documentId(), target.documentId()),
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
                () -> assertFalse(decodedTarget.migrated()));
    }

    @Test
    void publicPlanRejectsChangesBeyondSourceDartFile() throws Exception {
        DesignerPairRenamePlan valid = ready(planner.prepare(
                starterFd(), ORIGINAL_FILE, TARGET_FILE)).plan();
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
        DesignerDocument changedDocumentId = new DesignerDocument(
                target.schemaReference(),
                StableId.random(),
                target.source(),
                target.canvas(),
                target.root(),
                target.extensions());
        OriginalFdBytes changedClassBytes = codec.encode(changedClass);
        OriginalFdBytes invalidOriginalBytes = OriginalFdBytes.copyOf(
                "{".getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairRenamePlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                original,
                                changedClass,
                                valid.originalFdBytes(),
                                valid.targetFdBytes())),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairRenamePlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                original,
                                changedDocumentId,
                                valid.originalFdBytes(),
                                valid.targetFdBytes())),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairRenamePlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                original,
                                target,
                                valid.originalFdBytes(),
                                changedClassBytes)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new DesignerPairRenamePlan(
                                ORIGINAL_FILE,
                                TARGET_FILE,
                                original,
                                target,
                                invalidOriginalBytes,
                                valid.targetFdBytes())));
    }

    @Test
    void rejectsInvalidOriginalAndTargetFilenamesBeforeDecode() throws Exception {
        OriginalFdBytes originalFd = starterFd();

        DesignerPairRenameResult.Rejected invalidOriginal = rejected(planner.prepare(
                originalFd, "OrderScreen.dart", TARGET_FILE));
        DesignerPairRenameResult.Rejected invalidTarget = rejected(planner.prepare(
                originalFd, ORIGINAL_FILE, "../renamed_screen.dart"));

        assertAll(
                () -> assertEquals(
                        DesignerPairRenameResult.Code.INVALID_ORIGINAL_FILENAME,
                        invalidOriginal.code()),
                () -> assertEquals(
                        DesignerPairRenameResult.Code.INVALID_TARGET_FILENAME,
                        invalidTarget.code()));
    }

    @Test
    void rejectsUnchangedFilename() throws Exception {
        DesignerPairRenameResult.Rejected result = rejected(planner.prepare(
                starterFd(), ORIGINAL_FILE, ORIGINAL_FILE));

        assertEquals(DesignerPairRenameResult.Code.SAME_FILENAME, result.code());
    }

    @Test
    void rejectsSourceReferenceMismatch() throws Exception {
        DesignerPairRenameResult.Rejected result = rejected(planner.prepare(
                starterFd(), "other_screen.dart", TARGET_FILE));

        assertEquals(
                DesignerPairRenameResult.Code.SOURCE_REFERENCE_MISMATCH,
                result.code());
    }

    @Test
    void rejectsInvalidModel() throws Exception {
        OriginalFdBytes invalid = OriginalFdBytes.copyOf(
                "{".getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        DesignerPairRenameResult.Rejected result = rejected(planner.prepare(
                invalid, ORIGINAL_FILE, TARGET_FILE));

        assertEquals(DesignerPairRenameResult.Code.MODEL_NOT_CURRENT, result.code());
    }

    @Test
    void rejectsFutureModelWithoutRewritingIt() throws Exception {
        OriginalFdBytes current = starterFd();
        String currentJson = new String(current.copyBytes(), StandardCharsets.UTF_8);
        String futureJson = currentJson.replaceFirst(
                "\\\"schemaVersion\\\"\\s*:\\s*9",
                "\"schemaVersion\": 10");
        assertNotEquals(currentJson, futureJson);
        OriginalFdBytes future = OriginalFdBytes.copyOf(
                futureJson.getBytes(StandardCharsets.UTF_8),
                FdCodecLimits.defaults());

        DesignerPairRenameResult.Rejected result = rejected(planner.prepare(
                future, ORIGINAL_FILE, TARGET_FILE));

        assertAll(
                () -> assertEquals(
                        DesignerPairRenameResult.Code.MODEL_NOT_CURRENT,
                        result.code()),
                () -> assertEquals(futureJson,
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
        DesignerPairRenamePlanner migratedPlanner = new DesignerPairRenamePlanner(
                codec,
                ignored -> migrated);

        DesignerPairRenameResult.Rejected result = rejected(migratedPlanner.prepare(
                originalFd, ORIGINAL_FILE, TARGET_FILE));

        assertEquals(
                DesignerPairRenameResult.Code.MIGRATION_REQUIRED,
                result.code());
    }

    private static DesignerPairRenameResult.Ready ready(
            DesignerPairRenameResult result) {
        return assertInstanceOf(DesignerPairRenameResult.Ready.class, result);
    }

    private static DesignerPairRenameResult.Rejected rejected(
            DesignerPairRenameResult result) {
        return assertInstanceOf(DesignerPairRenameResult.Rejected.class, result);
    }

    private static OriginalFdBytes starterFd() throws Exception {
        DesignerFormTemplate template = new DesignerFormTemplateFactory()
                .create(ORIGINAL_FILE, "OrderScreen");
        return OriginalFdBytes.copyOf(
                template.fdBytes(),
                FdCodecLimits.defaults());
    }
}

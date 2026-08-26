package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateWarningPolicy;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionResult;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransactionStatus;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PairSaveCoordinatorOutcomeTest {

    @Test
    void unchangedSourcePlusSerializerFailureIsNotACommittedSave() {
        PairFileTransactionResult unchanged = result(
                PairFileTransactionStatus.UNCHANGED, 0);

        assertFalse(PairSaveCoordinator.acceptsSourceTransaction(
                unchanged,
                new IOException("guarded writer rejected the dirty live revision")));
        assertTrue(PairSaveCoordinator.acceptsSourceTransaction(
                unchanged, null));
    }

    @Test
    void actualDurableWriteRemainsCommittedAfterEditorFinalizationFailure() {
        PairFileTransactionResult committed = result(
                PairFileTransactionStatus.COMMITTED, 1);

        assertTrue(PairSaveCoordinator.acceptsSourceTransaction(
                committed, new IOException("provider finalization failed")));
        assertTrue(PairSaveCoordinator.acceptsPairTransaction(committed));
    }

    @Test
    void unchangedCanNeverPublishAPreparedPair() {
        assertFalse(PairSaveCoordinator.acceptsPairTransaction(
                result(PairFileTransactionStatus.UNCHANGED, 0)));
    }

    @Test
    void preparationMethodsNeverRetainALeaseMonitorAcrossEdtOrIo()
            throws Exception {
        Class<?> lease = PairSaveCoordinator.PairPreparation.class;
        assertFalse(Modifier.isSynchronized(
                lease.getDeclaredMethod(
                        "prepareAnalysis",
                        Path.class,
                        DartCandidateWarningPolicy.class)
                        .getModifiers()));
        assertFalse(Modifier.isSynchronized(
                lease.getDeclaredMethod(
                        "acceptAnalysisAndStage",
                        PairCandidateAnalysisTicket.class,
                        DartCandidateAnalysisResult.class)
                        .getModifiers()));
        assertFalse(Modifier.isSynchronized(
                lease.getDeclaredMethod("close").getModifiers()));
    }

    @Test
    void sourceNotificationsCannotReplaceAStickyPairConflict() {
        assertTrue(PairSaveCoordinator.isStickyConflict(
                PairSaveCoordinatorStatus.EXTERNAL_CONFLICT));
        assertTrue(PairSaveCoordinator.isStickyConflict(
                PairSaveCoordinatorStatus.RECOVERY_CONFLICT));
        assertFalse(PairSaveCoordinator.isStickyConflict(
                PairSaveCoordinatorStatus.DIRTY_SOURCE));
        assertFalse(PairSaveCoordinator.isStickyConflict(
                PairSaveCoordinatorStatus.SAVE_FAILED));
    }

    private static PairFileTransactionResult result(
            PairFileTransactionStatus status, int writes) {
        return new PairFileTransactionResult(status, List.of(), writes, false);
    }
}

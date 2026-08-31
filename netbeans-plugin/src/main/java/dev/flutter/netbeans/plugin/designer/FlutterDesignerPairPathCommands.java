package dev.flutter.netbeans.plugin.designer;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 * Pair-aware asynchronous command boundary for the dormant dedicated editor
 * shell. NetBeans' DataObject operations remain strictly synchronous; this
 * facade invokes them only from the exact post-close continuation.
 */
final class FlutterDesignerPairPathCommands {
    private FlutterDesignerPairPathCommands() {
    }

    static boolean requiresPostClose(DataObject initiatingObject) {
        return requiresPostClose(initiatingObject.getPrimaryFile());
    }

    static boolean requiresPostClose(FileObject initiatingMember) {
        try {
            return designerOwner(initiatingMember)
                    .getEditorSupport()
                    .hasDedicatedEditorShells();
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    static CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<FileObject>>
            rename(
                    DataObject initiatingObject,
                    String targetStem,
                    PairSaveCoordinator.PairPathReplay<FileObject> operation)
            throws IOException {
        DataObject initiated = Objects.requireNonNull(
                initiatingObject, "initiatingObject");
        String target = FlutterDesignerPairRename.validateTargetStem(targetStem);
        PairSaveCoordinator.PairPathReplay<FileObject> admittedOperation =
                Objects.requireNonNull(operation, "operation");
        FlutterDesignerDataObject owner = designerOwner(
                initiated.getPrimaryFile());
        PairSaveCoordinator coordinator = owner.getPairSaveCoordinator();
        PairSaveCoordinator.PairRenameLease lease =
                coordinator.reservePairRenameForEditorClose();
        CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<FileObject>>
                stage;
        try {
            stage = owner.getEditorSupport().requestPostCloseOperation(
                    "Rename Flutter Designer form",
                    initiated.getPrimaryFile().getPath() + " -> " + target,
                    proof -> {
                        lease.claimAfterEditorClose(proof);
                        try {
                            return coordinator.replayPairPathOperation(
                                    lease,
                                    admittedOperation);
                        } finally {
                            // The pair transaction normally finishes the same
                            // lease. This is the fail-safe for a DataObject
                            // failure before handleRename() is entered.
                            lease.finish(false);
                        }
                    });
        } catch (RuntimeException | Error failure) {
            lease.finish(false);
            throw failure;
        }
        releaseRejectedLease(stage, lease);
        return stage;
    }

    static CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<Void>>
            delete(
                    DataObject initiatingObject,
                    PairSaveCoordinator.PairPathReplay<Void> operation)
            throws IOException {
        DataObject initiated = Objects.requireNonNull(
                initiatingObject, "initiatingObject");
        FlutterDesignerDataObject owner = designerOwner(
                initiated.getPrimaryFile());
        PairSaveCoordinator.PairPathReplay<Void> admittedOperation =
                Objects.requireNonNull(operation, "operation");
        PairSaveCoordinator coordinator = owner.getPairSaveCoordinator();
        PairSaveCoordinator.PairDeleteLease lease =
                coordinator.reservePairDeleteForEditorClose();
        CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<Void>> stage;
        try {
            stage = owner.getEditorSupport().requestPostCloseOperation(
                    "Delete Flutter Designer form",
                    initiated.getPrimaryFile().getPath(),
                    proof -> {
                        lease.claimAfterEditorClose(proof);
                        try {
                            return coordinator.replayPairPathOperation(
                                    lease,
                                    admittedOperation);
                        } finally {
                            lease.finish(false);
                        }
                    });
        } catch (RuntimeException | Error failure) {
            lease.finish(false);
            throw failure;
        }
        releaseRejectedLease(stage, lease);
        return stage;
    }

    static CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<
            FlutterDesignerPairMove.MoveResult>> move(
                    FileObject member,
                    FileObject targetFolder) throws IOException {
        FileObject initiated = Objects.requireNonNull(member, "member");
        FileObject target = Objects.requireNonNull(targetFolder, "targetFolder");
        FlutterDesignerDataObject owner = designerOwner(initiated);
        PairSaveCoordinator coordinator = owner.getPairSaveCoordinator();
        PairSaveCoordinator.PairMoveLease lease =
                coordinator.reservePairMoveForEditorClose();
        CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<
                FlutterDesignerPairMove.MoveResult>> stage;
        try {
            stage = owner.getEditorSupport().requestPostCloseOperation(
                    "Move Flutter Designer form",
                    initiated.getPath() + " -> " + target.getPath(),
                    proof -> {
                        lease.claimAfterEditorClose(proof);
                        try {
                            return coordinator.replayPairPathOperation(
                                    lease,
                                    () -> FlutterDesignerPairMove.move(
                                            initiated, target));
                        } finally {
                            lease.finish(false);
                        }
                    });
        } catch (RuntimeException | Error failure) {
            lease.finish(false);
            throw failure;
        }
        releaseRejectedLease(stage, lease);
        return stage;
    }

    private static FlutterDesignerDataObject designerOwner(FileObject member)
            throws IOException {
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(Objects.requireNonNull(member, "member"))
                .orElseThrow(() -> new IOException(
                        "a complete safe Dart/.fd pair could not be resolved for "
                        + member.getPath()));
        DataObject sourceOwner = DataObject.find(pair.dartFile());
        if (!(sourceOwner instanceof FlutterDesignerDataObject designerOwner)) {
            throw new IOException(
                    "the paired Dart source is not owned by a Flutter Designer session");
        }
        return designerOwner;
    }

    private static void releaseRejectedLease(
            CompletionStage<? extends FlutterDesignerEditorSupport.PostCloseOutcome<?>>
                    stage,
            PairSaveCoordinator.PairPathOperationLease lease) {
        stage.whenComplete((outcome, failure) -> {
            if (failure != null || outcome == null || !outcome.succeeded()) {
                lease.finish(false);
            }
        });
    }
}

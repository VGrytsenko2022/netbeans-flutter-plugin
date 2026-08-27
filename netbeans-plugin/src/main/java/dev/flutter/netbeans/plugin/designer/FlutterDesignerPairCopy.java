package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.FdInputLimitException;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.copy.DesignerPairCopyPlan;
import dev.flutter.netbeans.designer.copy.DesignerPairCopyPlanner;
import dev.flutter.netbeans.designer.copy.DesignerPairCopyResult;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/** Publishes one independent duplicate of a complete Dart/.fd Designer pair. */
final class FlutterDesignerPairCopy {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerPairCopy.class.getName());
    private static final int MAX_COLLISION_ATTEMPTS = 10_000;
    private static final FlutterDesignerPairCopy DEFAULT =
            new FlutterDesignerPairCopy(
                    new NetBeansBackend(),
                    () -> UUID.randomUUID().toString(),
                    StableId::random,
                    new DesignerPairCopyPlanner(),
                    new DartSourceIntegrityScanner());

    private final Backend backend;
    private final Supplier<String> transactionIds;
    private final Supplier<StableId> documentIds;
    private final DesignerPairCopyPlanner planner;
    private final DartSourceIntegrityScanner sourceScanner;

    FlutterDesignerPairCopy(
            Backend backend,
            Supplier<String> transactionIds,
            Supplier<StableId> documentIds) {
        this(backend, transactionIds, documentIds,
                new DesignerPairCopyPlanner(),
                new DartSourceIntegrityScanner());
    }

    FlutterDesignerPairCopy(
            Backend backend,
            Supplier<String> transactionIds,
            Supplier<StableId> documentIds,
            DesignerPairCopyPlanner planner,
            DartSourceIntegrityScanner sourceScanner) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.transactionIds = Objects.requireNonNull(
                transactionIds, "transactionIds");
        this.documentIds = Objects.requireNonNull(documentIds, "documentIds");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.sourceScanner = Objects.requireNonNull(
                sourceScanner, "sourceScanner");
    }

    static boolean isAllowed(FileObject member) {
        return DEFAULT.canCopy(member);
    }

    static boolean isPasteTarget(FileObject member, FileObject targetFolder) {
        return DEFAULT.canPaste(member, targetFolder);
    }

    static CopyResult copy(FileObject member, FileObject targetFolder)
            throws IOException {
        return DEFAULT.copyMember(member, targetFolder);
    }

    boolean canCopy(FileObject member) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        return resolved.isPresent() && canCopy(resolved.orElseThrow());
    }

    boolean canCopy(FlutterDesignerPairLayout.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        return pair.dartFile().isValid()
                && pair.modelFile().isValid()
                && pair.dartFile().getParent() != null
                && pair.modelFile().getParent() != null
                && pair.dartFile().getParent().canWrite()
                && pair.modelFile().getParent().canWrite()
                && cleanDesignerOwner(pair.dartFile());
    }

    boolean canPaste(FileObject member, FileObject targetFolder) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        return resolved.isPresent()
                && validDuplicateTarget(
                        resolved.orElseThrow(), member, targetFolder)
                && canCopy(resolved.orElseThrow());
    }

    CopyResult copyMember(FileObject member, FileObject targetFolder)
            throws IOException {
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(member)
                .orElseThrow(() -> failure(
                        "Copy Flutter Designer form",
                        member,
                        "a complete safe Dart/.fd pair could not be resolved"));
        requireDuplicateTarget(pair, member, targetFolder);
        PairSaveCoordinator.PairCopyLease lease =
                acquireCopyAuthority(pair, member);
        boolean committed = false;
        try {
            CopyResult result = copyResolved(pair, member, targetFolder, lease);
            committed = result.committed();
            return result;
        } finally {
            lease.finish(committed);
        }
    }

    CopyResult copyResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            FileObject targetFolder) throws IOException {
        return copyResolved(pair, initiatingMember, targetFolder, null);
    }

    private CopyResult copyResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            FileObject targetFolder,
            PairSaveCoordinator.PairCopyLease lease) throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(initiatingMember, "initiatingMember");
        requireDuplicateTarget(pair, initiatingMember, targetFolder);

        FileSystem fileSystem = pair.dartFile().getFileSystem();
        if (!fileSystem.equals(pair.modelFile().getFileSystem())) {
            throw failure(
                    "Copy Flutter Designer form",
                    initiatingMember,
                    "the pair is split across different filesystems");
        }

        SourceMember dart = sourceMember("dart", pair.dartFile());
        SourceMember model = sourceMember("model", pair.modelFile());
        verifyDistinctPhysicalFiles(dart, model, initiatingMember);
        captureSourceBaseline(dart);
        captureSourceBaseline(model);
        verifySourceExact(dart);
        verifySourceExact(model);

        String targetStem = chooseTargetStem(
                dart.file().getName(), dart.parent(), model.parent());
        String transactionId = requireTransactionId(transactionIds.get());
        String stagingStem = ".nb-flutter-copy-" + transactionId;
        CopyMember targetDart = copyMember(
                "dart", dart.parent(), dart.file().getExt(),
                targetStem, stagingStem, dart.baseline());
        CopyMember targetModel = copyMember(
                "model", model.parent(), model.file().getExt(),
                targetStem, stagingStem, null);
        ensureVacant(targetDart);
        ensureVacant(targetModel);

        DesignerPairCopyPlan plan = preparePlan(
                dart, model, targetStem, documentIds.get());
        targetModel.expectedBytes = plan.targetFdBytes().copyBytes();
        verifySourceExact(dart);
        verifySourceExact(model);

        CopyState state = new CopyState();
        CopyAtomicAction copyAction = new CopyAtomicAction(() -> {
            try {
                createAndWrite(targetDart);
                createAndWrite(targetModel);
                publish(targetDart);
                publish(targetModel);
                verifySourceExact(dart);
                verifySourceExact(model);
                if (lease != null) {
                    lease.verifySourceStillClean();
                }
                verifyTargetPair(pair, targetDart, targetModel);
                state.committed = true;
            } catch (IOException | RuntimeException copyFailure) {
                if (!state.committed) {
                    throw rollback(
                            dart,
                            model,
                            targetDart,
                            targetModel,
                            copyFailure,
                            lease);
                }
                throw copyFailure;
            }
        });

        try {
            if (lease != null) {
                lease.bind(copyAction);
            }
            backend.runAtomicAction(fileSystem, copyAction);
        } catch (IOException | RuntimeException atomicFailure) {
            if (!state.committed) {
                throw atomicFailure;
            }
            try {
                verifySourceExact(dart);
                verifySourceExact(model);
            } catch (IOException | RuntimeException sourceVerificationFailure) {
                if (lease != null) {
                    lease.markRecoveryConflict(
                            "the atomic wrapper failed after Copy commit and "
                            + "the exact source snapshot no longer verifies: "
                            + reason(sourceVerificationFailure));
                }
                IOException inconsistent = new IOException(
                        "Copy Flutter Designer form committed its filesystem "
                        + "delegate, but the exact source pair could not be "
                        + "verified after the filesystem reported failure.",
                        sourceVerificationFailure);
                inconsistent.addSuppressed(atomicFailure);
                throw inconsistent;
            }
            try {
                verifyTargetPair(pair, targetDart, targetModel);
            } catch (IOException | RuntimeException targetVerificationFailure) {
                IOException inconsistent = new IOException(
                        "Copy Flutter Designer form committed its filesystem "
                        + "delegate, but the duplicate pair could not be "
                        + "verified after the filesystem reported failure.",
                        targetVerificationFailure);
                inconsistent.addSuppressed(atomicFailure);
                throw inconsistent;
            }
            LOGGER.log(Level.WARNING,
                    "The filesystem reported pair-Copy failure after its "
                    + "delegate committed; the exact source and duplicate "
                    + "pairs were verified and retained",
                    atomicFailure);
        } finally {
            copyAction.clear();
        }

        if (!state.committed) {
            throw new IOException(
                    "Copy Flutter Designer form did not execute its filesystem transaction.");
        }
        return new CopyResult(
                true,
                Objects.requireNonNull(targetDart.ownedFile, "target Dart"),
                Objects.requireNonNull(targetModel.ownedFile, "target model"));
    }

    private PairSaveCoordinator.PairCopyLease acquireCopyAuthority(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member) throws IOException {
        DataObject sourceOwner;
        try {
            sourceOwner = DataObject.find(pair.dartFile());
        } catch (IOException lookupFailure) {
            throw failure(
                    "Copy Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart editor owner could not be resolved: "
                    + reason(lookupFailure));
        }
        if (sourceOwner.isModified()) {
            throw failure(
                    "Copy Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source has unsaved changes; save it before copying the form");
        }
        if (!(sourceOwner instanceof FlutterDesignerDataObject designerOwner)) {
            throw failure(
                    "Copy Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source is not owned by the Flutter Designer session");
        }
        try {
            return designerOwner.getPairSaveCoordinator().beginPairCopy();
        } catch (IOException reservationFailure) {
            throw failure(
                    "Copy Flutter Designer form",
                    member,
                    reason(reservationFailure));
        }
    }

    private boolean cleanDesignerOwner(FileObject dartFile) {
        try {
            DataObject owner = DataObject.find(dartFile);
            return owner instanceof FlutterDesignerDataObject designerOwner
                    && !owner.isModified()
                    && designerOwner.getPairSaveCoordinator()
                            .canBeginPairCopy();
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private static boolean validDuplicateTarget(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member,
            FileObject targetFolder) {
        if (targetFolder == null || !targetFolder.isValid()
                || !targetFolder.isFolder() || !targetFolder.canWrite()) {
            return false;
        }
        if (member.equals(pair.dartFile())) {
            return targetFolder.equals(pair.dartFile().getParent());
        }
        if (member.equals(pair.modelFile())) {
            return targetFolder.equals(pair.modelFile().getParent());
        }
        return false;
    }

    private static void requireDuplicateTarget(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member,
            FileObject targetFolder) throws IOException {
        if (!validDuplicateTarget(pair, member, targetFolder)) {
            throw failure(
                    "Copy Flutter Designer form",
                    targetFolder,
                    "this first safe Copy/Paste slice duplicates only inside "
                    + "the source form's current lib/.fd_templates folder");
        }
        if (!pair.dartFile().getParent().canWrite()
                || !pair.modelFile().getParent().canWrite()) {
            throw failure(
                    "Copy Flutter Designer form",
                    member,
                    "one of the mirrored destination folders is read-only");
        }
    }

    private SourceMember sourceMember(String role, FileObject file)
            throws IOException {
        FileObject parent = file.getParent();
        if (parent == null || !parent.isFolder()) {
            throw failure(
                    "Copy Flutter Designer form",
                    file,
                    "the source parent folder is unavailable");
        }
        return new SourceMember(
                role,
                file,
                parent,
                file.getName(),
                file.getExt(),
                backend.realPath(file));
    }

    private static CopyMember copyMember(
            String role,
            FileObject parent,
            String extension,
            String targetStem,
            String stagingStem,
            byte[] expectedBytes) {
        return new CopyMember(
                role, parent, extension, targetStem, stagingStem, expectedBytes);
    }

    private void captureSourceBaseline(SourceMember member) throws IOException {
        if (!member.file().isValid()
                || member.file().getParent() != member.parent()
                || !member.file().getName().equals(member.name())
                || !member.file().getExt().equals(member.extension())
                || !backend.realPath(member.file()).equals(member.realPath())) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.file(),
                    "the source pair changed before its exact snapshot was captured");
        }
        long maximum = member.role().equals("dart")
                ? sourceScanner.limits().maxSourceBytes()
                : FdCodecLimits.DEFAULT_MAX_DOCUMENT_BYTES;
        if (member.file().getSize() > maximum) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.file(),
                    "the file exceeds the bounded Copy safety limit");
        }
        member.baseline = backend.read(member.file());
        if (member.baseline.length > maximum) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.file(),
                    "the file grew beyond the bounded Copy safety limit");
        }
    }

    private void verifySourceExact(SourceMember member) throws IOException {
        if (!member.file().isValid()
                || member.file().getParent() != member.parent()
                || !member.file().getName().equals(member.name())
                || !member.file().getExt().equals(member.extension())
                || !backend.realPath(member.file()).equals(member.realPath())
                || !Arrays.equals(member.baseline(), backend.read(member.file()))) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.file(),
                    "the exact source snapshot changed during Copy");
        }
    }

    private DesignerPairCopyPlan preparePlan(
            SourceMember dart,
            SourceMember model,
            String targetStem,
            StableId targetDocumentId) throws IOException {
        OriginalFdBytes originalFd;
        try {
            originalFd = OriginalFdBytes.copyOf(
                    model.baseline(), FdCodecLimits.defaults());
        } catch (FdInputLimitException limitFailure) {
            throw new IOException(
                    "Copy Flutter Designer form failed. Target: "
                    + model.path() + ". Reason: " + reason(limitFailure) + ".",
                    limitFailure);
        }
        DesignerPairCopyResult result = planner.prepare(
                originalFd,
                dart.name() + "." + dart.extension(),
                targetStem + "." + dart.extension(),
                Objects.requireNonNull(targetDocumentId, "targetDocumentId"));
        if (!(result instanceof DesignerPairCopyResult.Ready ready)) {
            DesignerPairCopyResult.Rejected rejected =
                    (DesignerPairCopyResult.Rejected) result;
            throw new IOException(
                    "Copy Flutter Designer form failed. Target: "
                    + dart.path() + " and " + model.path()
                    + ". Reason: " + rejected.code() + " at "
                    + (rejected.pointer().isBlank() ? "/" : rejected.pointer())
                    + ": " + rejected.message());
        }
        DesignerPairCopyPlan plan = ready.plan();
        requireSourceIntegrity(
                dart,
                sourceScanner.scan(dart.baseline(), plan.originalDocument().source()),
                "the current Dart source does not match the .fd metadata");
        requireSourceIntegrity(
                dart,
                sourceScanner.scan(dart.baseline(), plan.targetDocument().source()),
                "the exact Dart copy does not match the duplicate metadata");
        return plan;
    }

    private static void requireSourceIntegrity(
            SourceMember dart,
            DartSourceIntegrityResult integrity,
            String fallback) throws IOException {
        if (integrity.onDiskDeclaredMatch()) {
            return;
        }
        String detail = integrity.primaryDiagnostic()
                .map(diagnostic -> diagnostic.code() + ": " + diagnostic.message())
                .orElse(fallback);
        throw new IOException(
                "Copy Flutter Designer form failed. Target: "
                + dart.path() + ". Reason: " + detail);
    }

    private static String chooseTargetStem(
            String sourceStem,
            FileObject dartParent,
            FileObject modelParent) throws IOException {
        for (int index = 1; index <= MAX_COLLISION_ATTEMPTS; index++) {
            String candidate = index == 1
                    ? sourceStem + "_copy"
                    : sourceStem + "_copy_" + index;
            if (dartParent.getFileObject(candidate, "dart") == null
                    && modelParent.getFileObject(candidate, "fd") == null) {
                return candidate;
            }
        }
        throw failure(
                "Copy Flutter Designer form",
                dartParent,
                "no jointly free canonical duplicate filename was found");
    }

    private static void ensureVacant(CopyMember member) throws IOException {
        if (member.parent().getFileObject(
                member.targetStem(), member.extension()) != null) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.parent(),
                    "the selected duplicate target became occupied: "
                    + member.targetNameExt());
        }
        if (member.parent().getFileObject(
                member.stagingStem(), member.extension()) != null) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.parent(),
                    "the private Copy staging path already exists");
        }
    }

    private void createAndWrite(CopyMember member) throws IOException {
        ensureVacant(member);
        try {
            member.ownedFile = backend.create(
                    member.parent(), member.stagingStem(), member.extension());
        } catch (IOException | RuntimeException createFailure) {
            FileObject observed = member.parent().getFileObject(
                    member.stagingStem(), member.extension());
            if (observed != null) {
                member.unownedPathObserved = true;
            }
            throw createFailure;
        }
        if (location(member) != CopyLocation.STAGING) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.ownedFile,
                    "the created file did not retain its private staging identity");
        }
        try {
            backend.writeOwned(
                    member.ownedFile,
                    member.parent(),
                    member.stagingStem(),
                    member.extension(),
                    member.expectedBytes());
        } catch (IOException | RuntimeException writeFailure) {
            if (location(member) != CopyLocation.STAGING
                    || !exactOwnedBytes(member)) {
                throw writeFailure;
            }
            LOGGER.log(Level.FINE,
                    "A pair-Copy staging write completed before its provider reported failure",
                    writeFailure);
        }
        verifyOwnedBytes(member, "the staged duplicate bytes are not exact");
    }

    private void publish(CopyMember member) throws IOException {
        try {
            backend.publishOwned(
                    member.ownedFile,
                    member.parent(),
                    member.stagingStem(),
                    member.extension(),
                    member.expectedBytes(),
                    member.targetStem(),
                    member.extension());
        } catch (IOException | RuntimeException publishFailure) {
            if (location(member) != CopyLocation.TARGET
                    || !exactOwnedBytes(member)) {
                throw publishFailure;
            }
            LOGGER.log(Level.FINE,
                    "A pair-Copy publish completed before its provider reported failure",
                    publishFailure);
        }
        if (location(member) != CopyLocation.TARGET) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.ownedFile,
                    "the filesystem did not publish the expected duplicate path");
        }
        verifyOwnedBytes(member, "the published duplicate bytes are not exact");
    }

    private void verifyTargetPair(
            FlutterDesignerPairLayout.Pair sourcePair,
            CopyMember dart,
            CopyMember model) throws IOException {
        verifyPublished(dart);
        verifyPublished(model);
        FlutterDesignerPairLayout.Pair duplicate = FlutterDesignerPairLayout
                .findCompletePair(dart.ownedFile, sourcePair.project())
                .orElseThrow(() -> failure(
                        "Copy Flutter Designer form",
                        dart.ownedFile,
                        "the published files do not resolve as one safe mirrored pair"));
        if (duplicate.dartFile() != dart.ownedFile
                || duplicate.modelFile() != model.ownedFile) {
            throw failure(
                    "Copy Flutter Designer form",
                    dart.ownedFile,
                    "the published pair resolved to different physical members");
        }
    }

    private void verifyPublished(CopyMember member) throws IOException {
        if (location(member) != CopyLocation.TARGET) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.ownedFile,
                    "the duplicate target path is unavailable");
        }
        verifyOwnedBytes(member, "the duplicate target bytes do not match the plan");
        if (member.parent().getFileObject(
                member.stagingStem(), member.extension()) != null) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.ownedFile,
                    "a private Copy staging path remained after publish");
        }
    }

    private IOException rollback(
            SourceMember dart,
            SourceMember model,
            CopyMember targetDart,
            CopyMember targetModel,
            Throwable primary,
            PairSaveCoordinator.PairCopyLease lease) {
        List<String> recoveryFailures = new ArrayList<>();
        removeOwned(targetModel, recoveryFailures);
        removeOwned(targetDart, recoveryFailures);
        boolean sourceExact = verifySourceForRollback(dart, recoveryFailures)
                & verifySourceForRollback(model, recoveryFailures);
        if (!sourceExact && lease != null) {
            lease.markRecoveryConflict(
                    "the source pair changed while a failed Copy was being rolled back");
        }

        IOException failure = new IOException(
                "Copy Flutter Designer form failed. Target: "
                + dart.path() + " and " + model.path()
                + ". Reason: " + reason(primary),
                primary);
        if (!recoveryFailures.isEmpty()) {
            failure.addSuppressed(new IOException(
                    "Flutter Designer pair-Copy recovery conflict: "
                    + String.join("; ", recoveryFailures)));
        }
        return failure;
    }

    private void removeOwned(CopyMember member, List<String> failures) {
        if (member.ownedFile == null) {
            if (member.unownedPathObserved) {
                failures.add(member.stagingPath()
                        + ": refusing to delete a staging path whose FileObject "
                        + "identity was not returned by the failed create call");
            }
            return;
        }
        CopyLocation current = location(member);
        if (current == CopyLocation.ABSENT) {
            recordReplacementAtOwnedPath(member, failures);
            return;
        }
        if (current == CopyLocation.UNKNOWN) {
            failures.add(member.ownedFile.getPath()
                    + ": refusing to delete a file outside the owned Copy paths");
            return;
        }
        try {
            if (!exactOwnedBytes(member)) {
                failures.add(member.ownedFile.getPath()
                        + ": refusing to delete bytes that differ from the prepared Copy");
                return;
            }
            try {
                backend.deleteOwned(
                        member.ownedFile,
                        member.parent(),
                        current == CopyLocation.STAGING
                                ? member.stagingStem()
                                : member.targetStem(),
                        member.extension(),
                        member.expectedBytes());
            } catch (IOException | RuntimeException deleteFailure) {
                if (location(member) != CopyLocation.ABSENT) {
                    failures.add(member.ownedFile.getPath()
                            + ": " + reason(deleteFailure));
                }
            }
            if (location(member) != CopyLocation.ABSENT) {
                failures.add(member.ownedFile.getPath()
                        + ": the owned Copy artifact remained after rollback");
            } else {
                recordReplacementAtOwnedPath(member, failures);
            }
        } catch (IOException | RuntimeException verificationFailure) {
            failures.add(member.ownedFile.getPath()
                    + ": " + reason(verificationFailure));
        }
    }

    private static void recordReplacementAtOwnedPath(
            CopyMember member,
            List<String> failures) {
        FileObject stagingReplacement = member.parent().getFileObject(
                member.stagingStem(), member.extension());
        if (stagingReplacement != null
                && stagingReplacement != member.ownedFile) {
            failures.add(stagingReplacement.getPath()
                    + ": refusing to delete a replacement FileObject at the "
                    + "private Copy staging path");
        }
        FileObject targetReplacement = member.parent().getFileObject(
                member.targetStem(), member.extension());
        if (targetReplacement != null
                && targetReplacement != member.ownedFile) {
            failures.add(targetReplacement.getPath()
                    + ": refusing to delete a replacement FileObject at the "
                    + "prepared Copy target path");
        }
    }

    private boolean verifySourceForRollback(
            SourceMember member,
            List<String> failures) {
        try {
            verifySourceExact(member);
            return true;
        } catch (IOException | RuntimeException failure) {
            failures.add(member.path() + ": " + reason(failure));
            return false;
        }
    }

    private void verifyOwnedBytes(CopyMember member, String message)
            throws IOException {
        if (!exactOwnedBytes(member)) {
            throw failure(
                    "Copy Flutter Designer form",
                    member.ownedFile,
                    message);
        }
    }

    private boolean exactOwnedBytes(CopyMember member) throws IOException {
        return member.ownedFile != null
                && member.ownedFile.isValid()
                && Arrays.equals(
                        member.expectedBytes(), backend.read(member.ownedFile));
    }

    private static CopyLocation location(CopyMember member) {
        if (member.ownedFile == null || !member.ownedFile.isValid()) {
            return CopyLocation.ABSENT;
        }
        if (member.ownedFile.getParent() != member.parent()
                || !member.ownedFile.getExt().equals(member.extension())) {
            return CopyLocation.UNKNOWN;
        }
        if (member.ownedFile.getName().equals(member.stagingStem())) {
            return member.parent().getFileObject(
                    member.stagingStem(), member.extension())
                    == member.ownedFile
                    ? CopyLocation.STAGING
                    : CopyLocation.UNKNOWN;
        }
        if (member.ownedFile.getName().equals(member.targetStem())) {
            return member.parent().getFileObject(
                    member.targetStem(), member.extension())
                    == member.ownedFile
                    ? CopyLocation.TARGET
                    : CopyLocation.UNKNOWN;
        }
        return CopyLocation.UNKNOWN;
    }

    private void verifyDistinctPhysicalFiles(
            SourceMember dart,
            SourceMember model,
            FileObject initiatingMember) throws IOException {
        final boolean samePhysicalFile;
        try {
            samePhysicalFile = Files.isSameFile(
                    dart.realPath(), model.realPath());
        } catch (IOException identityFailure) {
            throw new IOException(
                    "Copy Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: the physical pair identity could not be verified.",
                    identityFailure);
        }
        if (samePhysicalFile) {
            throw failure(
                    "Copy Flutter Designer form",
                    initiatingMember,
                    "Dart and .fd resolve to the same physical file");
        }
    }

    private static String requireTransactionId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{8,80}")) {
            throw new IllegalStateException(
                    "Pair-Copy transaction id is unavailable or unsafe");
        }
        return value;
    }

    private static IOException failure(
            String operation,
            FileObject target,
            String reason) {
        return new IOException(operation + " failed. Target: "
                + (target == null ? "unknown Designer pair" : target.getPath())
                + ". Reason: " + reason + ".");
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    record CopyResult(
            boolean committed,
            FileObject dartFile,
            FileObject modelFile) {
        CopyResult {
            if (!committed) {
                throw new IllegalArgumentException(
                        "A returned pair-Copy result must be committed");
            }
            Objects.requireNonNull(dartFile, "dartFile");
            Objects.requireNonNull(modelFile, "modelFile");
        }
    }

    interface Backend {
        Path realPath(FileObject file) throws IOException;

        byte[] read(FileObject file) throws IOException;

        FileObject create(FileObject parent, String name, String extension)
                throws IOException;

        void writeOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] bytes) throws IOException;

        void publishOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException;

        void deleteOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException;

        void runAtomicAction(FileSystem fileSystem, FileSystem.AtomicAction action)
                throws IOException;
    }

    private enum CopyLocation {
        STAGING,
        TARGET,
        ABSENT,
        UNKNOWN
    }

    private static final class CopyState {
        private boolean committed;
    }

    private static final class CopyAtomicAction
            implements FileSystem.AtomicAction {
        private FileSystem.AtomicAction delegate;

        CopyAtomicAction(FileSystem.AtomicAction delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() throws IOException {
            FileSystem.AtomicAction current = delegate;
            if (current == null) {
                throw new IOException(
                        "The Flutter Designer pair-Copy action has already finished");
            }
            current.run();
        }

        void clear() {
            delegate = null;
        }
    }

    private static final class SourceMember {
        private final String role;
        private final FileObject file;
        private final FileObject parent;
        private final String name;
        private final String extension;
        private final Path realPath;
        private byte[] baseline;

        SourceMember(
                String role,
                FileObject file,
                FileObject parent,
                String name,
                String extension,
                Path realPath) {
            this.role = role;
            this.file = file;
            this.parent = parent;
            this.name = name;
            this.extension = extension;
            this.realPath = realPath;
        }

        String role() { return role; }
        FileObject file() { return file; }
        FileObject parent() { return parent; }
        String name() { return name; }
        String extension() { return extension; }
        Path realPath() { return realPath; }
        byte[] baseline() { return baseline; }
        String path() { return parent.getPath() + "/" + name + "." + extension; }
    }

    private static final class CopyMember {
        private final String role;
        private final FileObject parent;
        private final String extension;
        private final String targetStem;
        private final String stagingStem;
        private byte[] expectedBytes;
        private FileObject ownedFile;
        private boolean unownedPathObserved;

        CopyMember(
                String role,
                FileObject parent,
                String extension,
                String targetStem,
                String stagingStem,
                byte[] expectedBytes) {
            this.role = role;
            this.parent = parent;
            this.extension = extension;
            this.targetStem = targetStem;
            this.stagingStem = stagingStem;
            this.expectedBytes = expectedBytes;
        }

        String role() { return role; }
        FileObject parent() { return parent; }
        String extension() { return extension; }
        String targetStem() { return targetStem; }
        String stagingStem() { return stagingStem; }
        byte[] expectedBytes() {
            return Objects.requireNonNull(expectedBytes, role + " expectedBytes");
        }
        String targetNameExt() { return targetStem + "." + extension; }
        String stagingPath() {
            return parent.getPath() + "/" + stagingStem + "." + extension;
        }
    }

    private static final class NetBeansBackend implements Backend {
        @Override
        public Path realPath(FileObject file) throws IOException {
            File local;
            try {
                local = FileUtil.toFile(file);
            } catch (AssertionError invalidMasterFile) {
                throw new IOException(
                        "NetBeans could not resolve a normalized local file",
                        invalidMasterFile);
            }
            if (local == null) {
                throw new IOException("The file is not on a local filesystem");
            }
            return local.toPath().toRealPath();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            return file.asBytes();
        }

        @Override
        public FileObject create(
                FileObject parent,
                String name,
                String extension) throws IOException {
            return parent.createData(name, extension);
        }

        @Override
        public void writeOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] bytes) throws IOException {
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "write");
                if (file.getSize() != 0L || file.asBytes().length != 0) {
                    throw new IOException(
                            "Refusing pair-Copy write: the fresh staging "
                            + "artifact is no longer empty");
                }
                try (OutputStream output = file.getOutputStream(lock)) {
                    output.write(bytes);
                }
            }
        }

        @Override
        public void publishOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "publish");
                requireExactBytes(file, expectedBytes, "publish");
                if (expectedParent.getFileObject(
                        targetName, targetExtension) != null) {
                    throw new IOException(
                            "Refusing pair-Copy publish: the target path "
                            + "became occupied");
                }
                file.rename(lock, targetName, targetExtension);
            }
        }

        @Override
        public void deleteOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException {
            try (FileLock lock = file.lock()) {
                requireOwnedIdentity(
                        file,
                        expectedParent,
                        expectedName,
                        expectedExtension,
                        "delete");
                requireExactBytes(file, expectedBytes, "delete");
                file.delete(lock);
            }
        }

        private static void requireExactBytes(
                FileObject file,
                byte[] expectedBytes,
                String operation) throws IOException {
            if (!Arrays.equals(expectedBytes, file.asBytes())) {
                throw new IOException(
                        "Refusing pair-Copy " + operation
                        + ": the owned artifact bytes changed");
            }
        }

        private static void requireOwnedIdentity(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                String operation) throws IOException {
            if (!file.isValid()
                    || file.getParent() != expectedParent
                    || !file.getName().equals(expectedName)
                    || !file.getExt().equals(expectedExtension)
                    || expectedParent.getFileObject(
                            expectedName, expectedExtension) != file) {
                throw new IOException(
                        "Refusing pair-Copy " + operation
                        + ": the FileObject no longer owns its expected path");
            }
        }

        @Override
        public void runAtomicAction(
                FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            fileSystem.runAtomicAction(action);
        }
    }
}

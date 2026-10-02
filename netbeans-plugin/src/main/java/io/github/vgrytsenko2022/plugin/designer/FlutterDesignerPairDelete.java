package io.github.vgrytsenko2022.plugin.designer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

/** Deletes one complete Dart/.fd form without exposing a one-file operation. */
final class FlutterDesignerPairDelete {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerPairDelete.class.getName());
    private static final String TOMBSTONE_EXTENSION = "nbdelete";
    private static final FlutterDesignerPairDelete DEFAULT =
            new FlutterDesignerPairDelete(
                    new NetBeansBackend(),
                    () -> UUID.randomUUID().toString());

    private final Backend backend;
    private final Supplier<String> transactionIds;

    FlutterDesignerPairDelete(Backend backend, Supplier<String> transactionIds) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.transactionIds = Objects.requireNonNull(
                transactionIds, "transactionIds");
    }

    static boolean isAllowed(FileObject member) {
        return DEFAULT.canDelete(member);
    }

    static void delete(FileObject member) throws IOException {
        DEFAULT.deleteMember(member);
    }

    boolean canDelete(FileObject member) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        return resolved.isPresent() && canDelete(resolved.orElseThrow());
    }

    boolean canDelete(FlutterDesignerPairLayout.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        return pair.dartFile().isValid()
                && pair.modelFile().isValid()
                && pair.dartFile().canWrite()
                && pair.modelFile().canWrite()
                && cleanDesignerOwner(pair.dartFile());
    }

    void deleteMember(FileObject member) throws IOException {
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(member)
                .orElseThrow(() -> failure(
                        "Delete Flutter Designer form",
                        member,
                        "a complete safe Dart/.fd pair could not be resolved"));
        PairSaveCoordinator.PairDeleteLease lease =
                acquireDeleteAuthority(pair, member);
        boolean committed = false;
        try {
            DeleteResult result = deleteResolved(pair, member, lease);
            committed = result.committed();
            if (!result.cleanupFailures().isEmpty()) {
                LOGGER.log(Level.WARNING,
                        "Flutter Designer form was deleted, but {0} hidden tombstone "
                        + "file(s) could not be removed: {1}",
                        new Object[]{
                            result.cleanupFailures().size(),
                            String.join("; ", result.cleanupFailures())
                        });
            }
        } finally {
            lease.finish(committed);
        }
    }

    DeleteResult deleteResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember) throws IOException {
        return deleteResolved(pair, initiatingMember, null);
    }

    private DeleteResult deleteResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            PairSaveCoordinator.PairDeleteLease lease) throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(initiatingMember, "initiatingMember");
        if (!initiatingMember.equals(pair.dartFile())
                && !initiatingMember.equals(pair.modelFile())) {
            throw new IOException(
                    "Delete Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: the initiating file is not a member of the pair.");
        }

        String transactionId = requireTransactionId(transactionIds.get());
        Member dart = member("dart", pair.dartFile(), transactionId);
        Member model = member("model", pair.modelFile(), transactionId);
        final boolean samePhysicalFile;
        try {
            samePhysicalFile = Files.isSameFile(
                    dart.realPath(), model.realPath());
        } catch (IOException identityFailure) {
            throw new IOException(
                    "Delete Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: the physical pair identity could not be verified.",
                    identityFailure);
        }
        if (samePhysicalFile) {
            throw new IOException(
                    "Delete Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: Dart and .fd resolve to the same physical file.");
        }
        FileSystem fileSystem = pair.dartFile().getFileSystem();
        if (!fileSystem.equals(pair.modelFile().getFileSystem())) {
            throw new IOException(
                    "Delete Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: the pair is split across different filesystems.");
        }

        List<Member> lockOrder = new ArrayList<>(List.of(dart, model));
        lockOrder.sort(Comparator.comparing(
                item -> lockKey(item.realPath())));
        FileLock firstLock = null;
        FileLock secondLock = null;
        try {
            firstLock = backend.lock(lockOrder.get(0).file());
            secondLock = backend.lock(lockOrder.get(1).file());
            bindLock(dart, lockOrder, firstLock, secondLock);
            bindLock(model, lockOrder, firstLock, secondLock);
            captureLockedBaseline(dart);
            captureLockedBaseline(model);

            DeleteState state = new DeleteState();
            DeleteAtomicAction deleteAction = new DeleteAtomicAction(() -> {
                try {
                    stage(model, state);
                    stage(dart, state);
                    cleanupCommittedTombstones(
                            initiatingMember.equals(dart.file()) ? model : dart,
                            initiatingMember.equals(dart.file()) ? dart : model,
                            state);
                } catch (IOException | RuntimeException failure) {
                    if (!state.committed) {
                        throw rollbackStaging(
                                dart, model, state, failure, lease);
                    }
                    state.cleanupFailures.add(
                            "atomic cleanup: " + reason(failure));
                }
            });
            try {
                if (lease != null) {
                    lease.bind(deleteAction);
                }
                backend.runAtomicAction(fileSystem, deleteAction);
            } finally {
                // The coordinator retains this identity long enough to suppress
                // delayed DataObject atomic-action events. Do not retain the
                // delegate closure: it captures both exact-byte baselines.
                deleteAction.clear();
            }
            return new DeleteResult(
                    state.committed,
                    List.copyOf(state.cleanupFailures));
        } finally {
            release(secondLock);
            release(firstLock);
        }
    }

    private PairSaveCoordinator.PairDeleteLease acquireDeleteAuthority(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member) throws IOException {
        if (!pair.dartFile().isValid() || !pair.modelFile().isValid()) {
            throw failure(
                    "Delete Flutter Designer form",
                    member,
                    "one of the paired files is no longer valid");
        }
        if (!pair.dartFile().canWrite()) {
            throw failure(
                    "Delete Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source is read-only");
        }
        if (!pair.modelFile().canWrite()) {
            throw failure(
                    "Delete Flutter Designer form",
                    pair.modelFile(),
                    "the Flutter Designer model is read-only");
        }
        DataObject sourceOwner;
        try {
            sourceOwner = DataObject.find(pair.dartFile());
        } catch (IOException lookupFailure) {
            throw failure(
                    "Delete Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart editor owner could not be resolved: "
                    + reason(lookupFailure));
        }
        if (sourceOwner.isModified()) {
            throw failure(
                    "Delete Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source has unsaved changes; save or close "
                    + "the editor before deleting the form");
        }
        if (!(sourceOwner instanceof FlutterDesignerDataObject designerOwner)) {
            throw failure(
                    "Delete Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source is not owned by the Flutter Designer session");
        }
        try {
            return designerOwner.getPairSaveCoordinator().beginPairDelete();
        } catch (IOException reservationFailure) {
            throw failure(
                    "Delete Flutter Designer form",
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
                            .canBeginPairDelete();
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private Member member(
            String role,
            FileObject file,
            String transactionId) throws IOException {
        FileObject parent = file.getParent();
        if (parent == null || !parent.isFolder()) {
            throw failure(
                    "Delete Flutter Designer form",
                    file,
                    "the parent folder is unavailable");
        }
        Path realPath = backend.realPath(file);
        String tombstoneName = ".nb-flutter-delete-"
                + transactionId + "-" + role;
        if (parent.getFileObject(tombstoneName, TOMBSTONE_EXTENSION) != null) {
            throw failure(
                    "Delete Flutter Designer form",
                    file,
                    "the private tombstone path already exists");
        }
        return new Member(
                role,
                file,
                parent,
                file.getName(),
                file.getExt(),
                realPath,
                tombstoneName,
                TOMBSTONE_EXTENSION);
    }

    private void captureLockedBaseline(Member member) throws IOException {
        if (!member.file().isValid()
                || member.file().getParent() != member.parent()
                || !member.file().getName().equals(member.originalName())
                || !member.file().getExt().equals(member.originalExtension())
                || !backend.realPath(member.file()).equals(member.realPath())) {
            throw failure(
                    "Delete Flutter Designer form",
                    member.file(),
                    "the pair changed before both file locks were acquired");
        }
        member.baseline = backend.read(member.file());
    }

    private static void bindLock(
            Member target,
            List<Member> order,
            FileLock first,
            FileLock second) {
        target.lock = order.get(0) == target ? first : second;
    }

    private void stage(Member member, DeleteState state) throws IOException {
        try {
            backend.rename(
                    member.file(),
                    member.lock,
                    member.tombstoneName(),
                    member.tombstoneExtension());
        } finally {
            // A filesystem provider may complete the rename and then report
            // an error. Record the observable result so rollback still owns
            // that tombstone instead of leaving a hidden half-deleted pair.
            if (isAtTombstonePath(member) && !member.staged) {
                member.staged = true;
                state.staged.add(member);
            }
        }
        if (!member.file().isValid()
                || member.file().getParent() != member.parent()
                || !member.file().getName().equals(member.tombstoneName())
                || !member.file().getExt().equals(member.tombstoneExtension())
                || !Arrays.equals(member.baseline, backend.read(member.file()))) {
            throw failure(
                    "Stage Flutter Designer pair deletion",
                    member.file(),
                    "the staged tombstone did not preserve the exact file bytes");
        }
        if (state.staged.size() == 2) {
            state.committed = true;
        }
    }

    private static boolean isAtTombstonePath(Member member) {
        return member.file().isValid()
                && member.file().getParent() == member.parent()
                && member.file().getName().equals(member.tombstoneName())
                && member.file().getExt().equals(member.tombstoneExtension());
    }

    private void cleanupCommittedTombstones(
            Member nonInitiator,
            Member initiator,
            DeleteState state) {
        if (!state.committed) {
            throw new IllegalStateException(
                    "Flutter Designer pair deletion was not fully staged");
        }
        cleanup(nonInitiator, state);
        cleanup(initiator, state);
    }

    private void cleanup(Member member, DeleteState state) {
        try {
            backend.delete(member.file(), member.lock);
            member.cleaned = true;
        } catch (IOException | RuntimeException failure) {
            state.cleanupFailures.add(
                    member.file().getPath() + ": " + reason(failure));
        }
    }

    private IOException rollbackStaging(
            Member dart,
            Member model,
            DeleteState state,
            Throwable primary,
            PairSaveCoordinator.PairDeleteLease lease) {
        List<String> recoveryFailures = new ArrayList<>();
        for (int index = state.staged.size() - 1; index >= 0; index--) {
            Member member = state.staged.get(index);
            if (!member.staged || member.cleaned) {
                continue;
            }
            try {
                backend.rename(
                        member.file(),
                        member.lock,
                        member.originalName(),
                        member.originalExtension());
                member.staged = false;
            } catch (IOException | RuntimeException rollbackFailure) {
                try {
                    if (isRestoredExactly(member)) {
                        member.staged = false;
                    } else {
                        recoveryFailures.add(
                                member.file().getPath() + ": "
                                + reason(rollbackFailure));
                    }
                } catch (IOException | RuntimeException verificationFailure) {
                    recoveryFailures.add(
                            member.file().getPath() + ": "
                            + reason(rollbackFailure) + "; verification: "
                            + reason(verificationFailure));
                }
            }
        }
        verifyRestored(dart, recoveryFailures);
        verifyRestored(model, recoveryFailures);
        if (!recoveryFailures.isEmpty() && lease != null) {
            lease.markRecoveryConflict(String.join("; ", recoveryFailures));
        }

        IOException failure = new IOException(
                "Delete Flutter Designer form failed. Target: "
                + dart.originalPath() + " and " + model.originalPath()
                + ". Reason: " + reason(primary),
                primary);
        if (!recoveryFailures.isEmpty()) {
            failure.addSuppressed(new IOException(
                    "Flutter Designer pair-delete recovery conflict: "
                    + String.join("; ", recoveryFailures)));
        }
        return failure;
    }

    private void verifyRestored(
            Member member,
            List<String> recoveryFailures) {
        try {
            if (!isRestoredExactly(member)) {
                recoveryFailures.add(
                        member.originalPath()
                        + ": original path or exact bytes were not restored");
            }
        } catch (IOException | RuntimeException verificationFailure) {
            recoveryFailures.add(
                    member.originalPath() + ": " + reason(verificationFailure));
        }
    }

    private boolean isRestoredExactly(Member member) throws IOException {
        FileObject restored = member.parent().getFileObject(
                member.originalName(), member.originalExtension());
        return restored != null
                && restored.isValid()
                && Arrays.equals(member.baseline, backend.read(restored));
    }

    private static IOException failure(
            String operation,
            FileObject target,
            String reason) {
        return new IOException(operation + " failed. Target: "
                + (target == null ? "unknown Designer pair" : target.getPath())
                + ". Reason: " + reason + ".");
    }

    private static String requireTransactionId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{8,80}")) {
            throw new IllegalStateException(
                    "Pair-delete transaction id is unavailable or unsafe");
        }
        return value;
    }

    private static String lockKey(Path path) {
        String key = path.toAbsolutePath().normalize().toString();
        return isWindows() ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .startsWith("windows");
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private static void release(FileLock lock) {
        if (lock != null && lock.isValid()) {
            lock.releaseLock();
        }
    }

    /**
     * Stable event-provenance identity whose potentially large operation body
     * can be discarded as soon as the synchronous atomic action returns.
     */
    private static final class DeleteAtomicAction
            implements FileSystem.AtomicAction {
        private FileSystem.AtomicAction delegate;

        DeleteAtomicAction(FileSystem.AtomicAction delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() throws IOException {
            FileSystem.AtomicAction current = delegate;
            if (current == null) {
                throw new IOException(
                        "The Flutter Designer pair-delete action has already finished");
            }
            current.run();
        }

        void clear() {
            delegate = null;
        }
    }

    record DeleteResult(boolean committed, List<String> cleanupFailures) {
        DeleteResult {
            cleanupFailures = List.copyOf(cleanupFailures);
            if (!committed) {
                throw new IllegalArgumentException(
                        "A returned pair-delete result must be committed");
            }
        }
    }

    interface Backend {
        Path realPath(FileObject file) throws IOException;

        FileLock lock(FileObject file) throws IOException;

        byte[] read(FileObject file) throws IOException;

        void rename(FileObject file, FileLock lock, String name, String extension)
                throws IOException;

        void delete(FileObject file, FileLock lock) throws IOException;

        void runAtomicAction(FileSystem fileSystem, FileSystem.AtomicAction action)
                throws IOException;
    }

    private static final class NetBeansBackend implements Backend {
        @Override
        public Path realPath(FileObject file) throws IOException {
            File local;
            try {
                local = FileUtil.toFile(file);
            } catch (AssertionError invalidMasterFile) {
                throw new IOException(
                        "NetBeans could not resolve a normalized local file", invalidMasterFile);
            }
            if (local == null) {
                throw new IOException("The file is not on a local filesystem");
            }
            return local.toPath().toRealPath();
        }

        @Override
        public FileLock lock(FileObject file) throws IOException {
            return file.lock();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            return file.asBytes();
        }

        @Override
        public void rename(
                FileObject file,
                FileLock lock,
                String name,
                String extension) throws IOException {
            file.rename(lock, name, extension);
        }

        @Override
        public void delete(FileObject file, FileLock lock) throws IOException {
            file.delete(lock);
        }

        @Override
        public void runAtomicAction(
                FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            fileSystem.runAtomicAction(action);
        }
    }

    private static final class Member {
        private final String role;
        private final FileObject file;
        private final FileObject parent;
        private final String originalName;
        private final String originalExtension;
        private final Path realPath;
        private final String tombstoneName;
        private final String tombstoneExtension;
        private FileLock lock;
        private byte[] baseline;
        private boolean staged;
        private boolean cleaned;

        Member(
                String role,
                FileObject file,
                FileObject parent,
                String originalName,
                String originalExtension,
                Path realPath,
                String tombstoneName,
                String tombstoneExtension) {
            this.role = Objects.requireNonNull(role, "role");
            this.file = Objects.requireNonNull(file, "file");
            this.parent = Objects.requireNonNull(parent, "parent");
            this.originalName = Objects.requireNonNull(originalName, "originalName");
            this.originalExtension = Objects.requireNonNull(
                    originalExtension, "originalExtension");
            this.realPath = Objects.requireNonNull(realPath, "realPath");
            this.tombstoneName = Objects.requireNonNull(
                    tombstoneName, "tombstoneName");
            this.tombstoneExtension = Objects.requireNonNull(
                    tombstoneExtension, "tombstoneExtension");
        }

        String role() {
            return role;
        }

        FileObject file() {
            return file;
        }

        FileObject parent() {
            return parent;
        }

        String originalName() {
            return originalName;
        }

        String originalExtension() {
            return originalExtension;
        }

        Path realPath() {
            return realPath;
        }

        String tombstoneName() {
            return tombstoneName;
        }

        String tombstoneExtension() {
            return tombstoneExtension;
        }

        String originalPath() {
            String parentPath = parent.getPath();
            return parentPath.isEmpty()
                    ? originalName + "." + originalExtension
                    : parentPath + "/" + originalName + "." + originalExtension;
        }
    }

    private static final class DeleteState {
        private final List<Member> staged = new ArrayList<>(2);
        private final List<String> cleanupFailures = new ArrayList<>(2);
        private boolean committed;
    }
}

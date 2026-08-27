package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.FdInputLimitException;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.rename.DesignerPairRenamePlan;
import dev.flutter.netbeans.designer.rename.DesignerPairRenamePlanner;
import dev.flutter.netbeans.designer.rename.DesignerPairRenameResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.regex.Pattern;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/** Renames one complete Dart/.fd form without exposing a one-file operation. */
final class FlutterDesignerPairRename {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerPairRename.class.getName());
    private static final Pattern TARGET_STEM =
            Pattern.compile("_?[a-z][a-z0-9_]*");
    private static final FlutterDesignerPairRename DEFAULT =
            new FlutterDesignerPairRename(
                    new NetBeansBackend(),
                    () -> UUID.randomUUID().toString(),
                    new DesignerPairRenamePlanner(),
                    new DartSourceIntegrityScanner());

    private final Backend backend;
    private final Supplier<String> transactionIds;
    private final DesignerPairRenamePlanner planner;
    private final DartSourceIntegrityScanner sourceScanner;

    FlutterDesignerPairRename(
            Backend backend,
            Supplier<String> transactionIds) {
        this(backend, transactionIds,
                new DesignerPairRenamePlanner(),
                new DartSourceIntegrityScanner());
    }

    FlutterDesignerPairRename(
            Backend backend,
            Supplier<String> transactionIds,
            DesignerPairRenamePlanner planner,
            DartSourceIntegrityScanner sourceScanner) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.transactionIds = Objects.requireNonNull(
                transactionIds, "transactionIds");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.sourceScanner = Objects.requireNonNull(
                sourceScanner, "sourceScanner");
    }

    static boolean isAllowed(FileObject member) {
        return DEFAULT.canRename(member);
    }

    static FileObject rename(FileObject member, String targetStem)
            throws IOException {
        return DEFAULT.renameMember(member, targetStem);
    }

    boolean canRename(FileObject member) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        return resolved.isPresent() && canRename(resolved.orElseThrow());
    }

    boolean canRename(FlutterDesignerPairLayout.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        // NetBeans asks this from node/UI paths where bounded model and Dart
        // reads would still block the EDT. This is deliberately only the
        // structural/ownership capability check. Once a target name exists,
        // renameResolved revalidates current schema, source.dartFile and exact
        // Dart integrity under both file locks before the first mutation.
        return pair.dartFile().isValid()
                && pair.modelFile().isValid()
                && pair.dartFile().canWrite()
                && pair.modelFile().canWrite()
                && cleanDesignerOwner(pair.dartFile());
    }

    FileObject renameMember(FileObject member, String requestedStem)
            throws IOException {
        String targetStem = requireTargetStem(requestedStem);
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(member)
                .orElseThrow(() -> failure(
                        "Rename Flutter Designer form",
                        member,
                        "a complete safe Dart/.fd pair could not be resolved"));
        if (pair.dartFile().getName().equals(targetStem)) {
            return member;
        }

        PairSaveCoordinator.PairRenameLease lease =
                acquireRenameAuthority(pair, member);
        boolean committed = false;
        try {
            RenameResult result = renameResolved(
                    pair, member, targetStem, lease);
            committed = result.committed();
            return member.equals(pair.dartFile())
                    ? result.dartFile()
                    : result.modelFile();
        } finally {
            lease.finish(committed);
        }
    }

    RenameResult renameResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            String requestedStem) throws IOException {
        return renameResolved(pair, initiatingMember, requestedStem, null);
    }

    private RenameResult renameResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            String requestedStem,
            PairSaveCoordinator.PairRenameLease lease) throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(initiatingMember, "initiatingMember");
        String targetStem = requireTargetStem(requestedStem);
        if (!initiatingMember.equals(pair.dartFile())
                && !initiatingMember.equals(pair.modelFile())) {
            throw failure(
                    "Rename Flutter Designer form",
                    initiatingMember,
                    "the initiating file is not a member of the pair");
        }
        if (pair.dartFile().getName().equals(targetStem)) {
            return new RenameResult(
                    true, pair.dartFile(), pair.modelFile());
        }

        String transactionId = requireTransactionId(transactionIds.get());
        String tombstoneStem = ".nb-flutter-rename-" + transactionId;
        Member dart = member(
                "dart", pair.dartFile(), targetStem, tombstoneStem);
        Member model = member(
                "model", pair.modelFile(), targetStem, tombstoneStem);
        verifyDistinctPhysicalFiles(dart, model, initiatingMember);

        FileSystem fileSystem = pair.dartFile().getFileSystem();
        if (!fileSystem.equals(pair.modelFile().getFileSystem())) {
            throw failure(
                    "Rename Flutter Designer form",
                    initiatingMember,
                    "the pair is split across different filesystems");
        }

        List<Member> lockOrder = new ArrayList<>(List.of(dart, model));
        lockOrder.sort(Comparator.comparing(item -> lockKey(item.realPath())));
        FileLock firstLock = null;
        FileLock secondLock = null;
        try {
            firstLock = backend.lock(lockOrder.get(0).file());
            secondLock = backend.lock(lockOrder.get(1).file());
            bindLock(dart, lockOrder, firstLock, secondLock);
            bindLock(model, lockOrder, firstLock, secondLock);
            captureLockedBaseline(dart);
            captureLockedBaseline(model);

            DesignerPairRenamePlan plan = preparePlan(dart, model);
            byte[] targetModelBytes = plan.targetFdBytes().copyBytes();
            Member firstPath = initiatingMember.equals(dart.file())
                    ? model : dart;
            Member secondPath = initiatingMember.equals(dart.file())
                    ? dart : model;
            RenameState state = new RenameState();
            RenameAtomicAction renameAction = new RenameAtomicAction(() -> {
                try {
                    renameTo(firstPath, Location.TOMBSTONE);
                    renameTo(secondPath, Location.TOMBSTONE);
                    state.modelWriteAttempted = true;
                    backend.write(model.file(), model.lock, targetModelBytes);
                    verifyBytes(model, targetModelBytes,
                            "the staged .fd metadata write was not exact");
                    renameTo(firstPath, Location.TARGET);
                    renameTo(secondPath, Location.TARGET);
                    verifyTarget(dart, dart.baseline);
                    verifyTarget(model, targetModelBytes);
                    state.committed = true;
                } catch (IOException | RuntimeException failure) {
                    if (!state.committed) {
                        throw rollback(
                                dart,
                                model,
                                firstPath,
                                secondPath,
                                state,
                                failure,
                                lease);
                    }
                    throw failure;
                }
            });
            try {
                if (lease != null) {
                    lease.bind(renameAction);
                }
                backend.runAtomicAction(fileSystem, renameAction);
            } catch (IOException | RuntimeException atomicFailure) {
                if (!state.committed) {
                    throw atomicFailure;
                }
                // A provider may execute the complete delegate and only then
                // fail while publishing its delayed event batch. Physical
                // commit wins only after a second exact observation under the
                // still-held pair locks; otherwise the retained coordinator
                // must enter conflict instead of reporting a false rollback.
                try {
                    verifyTarget(dart, dart.baseline);
                    verifyTarget(model, targetModelBytes);
                } catch (IOException | RuntimeException verificationFailure) {
                    if (lease != null) {
                        lease.markRecoveryConflict(
                                "the atomic wrapper failed after commit and "
                                + "the target pair no longer verifies: "
                                + reason(verificationFailure));
                    }
                    IOException inconsistent = new IOException(
                            "Rename Flutter Designer form committed its atomic "
                            + "delegate, but the target pair could not be "
                            + "verified after the filesystem reported failure.",
                            verificationFailure);
                    inconsistent.addSuppressed(atomicFailure);
                    throw inconsistent;
                }
                LOGGER.log(Level.WARNING,
                        "The filesystem reported pair-Rename failure after its "
                        + "delegate committed; the exact target pair was "
                        + "verified and retained",
                        atomicFailure);
            } finally {
                // Preserve only the lightweight event identity after the
                // enclosing DataObject atomic action publishes delayed events.
                renameAction.clear();
            }
            if (!state.committed) {
                throw new IOException(
                        "Rename Flutter Designer form did not execute its "
                        + "filesystem transaction.");
            }
            return new RenameResult(
                    state.committed, dart.file(), model.file());
        } finally {
            release(secondLock);
            release(firstLock);
        }
    }

    private PairSaveCoordinator.PairRenameLease acquireRenameAuthority(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member) throws IOException {
        if (!pair.dartFile().isValid() || !pair.modelFile().isValid()) {
            throw failure(
                    "Rename Flutter Designer form",
                    member,
                    "one of the paired files is no longer valid");
        }
        if (!pair.dartFile().canWrite()) {
            throw failure(
                    "Rename Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source is read-only");
        }
        if (!pair.modelFile().canWrite()) {
            throw failure(
                    "Rename Flutter Designer form",
                    pair.modelFile(),
                    "the Flutter Designer model is read-only");
        }
        DataObject sourceOwner;
        try {
            sourceOwner = DataObject.find(pair.dartFile());
        } catch (IOException lookupFailure) {
            throw failure(
                    "Rename Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart editor owner could not be resolved: "
                    + reason(lookupFailure));
        }
        if (sourceOwner.isModified()) {
            throw failure(
                    "Rename Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source has unsaved changes; save or close "
                    + "the editor before renaming the form");
        }
        if (!(sourceOwner instanceof FlutterDesignerDataObject designerOwner)) {
            throw failure(
                    "Rename Flutter Designer form",
                    pair.dartFile(),
                    "the paired Dart source is not owned by the Flutter Designer session");
        }
        try {
            return designerOwner.getPairSaveCoordinator().beginPairRename();
        } catch (IOException reservationFailure) {
            throw failure(
                    "Rename Flutter Designer form",
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
                            .canBeginPairRename();
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private Member member(
            String role,
            FileObject file,
            String targetStem,
            String tombstoneStem) throws IOException {
        FileObject parent = file.getParent();
        if (parent == null || !parent.isFolder()) {
            throw failure(
                    "Rename Flutter Designer form",
                    file,
                    "the parent folder is unavailable");
        }
        if (parent.getFileObject(targetStem, file.getExt()) != null) {
            throw failure(
                    "Rename Flutter Designer form",
                    file,
                    "the target already exists: "
                    + targetStem + "." + file.getExt());
        }
        if (parent.getFileObject(tombstoneStem, file.getExt()) != null) {
            throw failure(
                    "Rename Flutter Designer form",
                    file,
                    "the private rename staging path already exists");
        }
        return new Member(
                role,
                file,
                parent,
                file.getName(),
                file.getExt(),
                backend.realPath(file),
                targetStem,
                tombstoneStem);
    }

    private void verifyDistinctPhysicalFiles(
            Member dart,
            Member model,
            FileObject initiatingMember) throws IOException {
        final boolean samePhysicalFile;
        try {
            samePhysicalFile = Files.isSameFile(
                    dart.realPath(), model.realPath());
        } catch (IOException identityFailure) {
            throw new IOException(
                    "Rename Flutter Designer form failed. Target: "
                    + initiatingMember.getPath()
                    + ". Reason: the physical pair identity could not be verified.",
                    identityFailure);
        }
        if (samePhysicalFile) {
            throw failure(
                    "Rename Flutter Designer form",
                    initiatingMember,
                    "Dart and .fd resolve to the same physical file");
        }
    }

    private void captureLockedBaseline(Member member) throws IOException {
        if (location(member) != Location.ORIGINAL
                || !backend.realPath(member.file()).equals(member.realPath())) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the pair changed before both file locks were acquired");
        }
        ensurePathVacant(member, Location.TARGET);
        ensurePathVacant(member, Location.TOMBSTONE);
        long maximum = member.role().equals("dart")
                ? sourceScanner.limits().maxSourceBytes()
                : FdCodecLimits.DEFAULT_MAX_DOCUMENT_BYTES;
        if (member.file().getSize() > maximum) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the file exceeds the bounded rename safety limit");
        }
        member.baseline = backend.read(member.file());
        if (member.baseline.length > maximum) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the file grew beyond the bounded rename safety limit");
        }
    }

    private DesignerPairRenamePlan preparePlan(Member dart, Member model)
            throws IOException {
        OriginalFdBytes originalFd;
        try {
            originalFd = OriginalFdBytes.copyOf(
                    model.baseline, FdCodecLimits.defaults());
        } catch (FdInputLimitException failure) {
            throw new IOException(
                    "Rename Flutter Designer form failed. Target: "
                    + model.originalPath()
                    + ". Reason: " + reason(failure) + ".",
                    failure);
        }
        DesignerPairRenameResult result = planner.prepare(
                originalFd,
                dart.originalName() + "." + dart.extension(),
                dart.targetName() + "." + dart.extension());
        if (!(result instanceof DesignerPairRenameResult.Ready ready)) {
            DesignerPairRenameResult.Rejected rejected =
                    (DesignerPairRenameResult.Rejected) result;
            throw new IOException(
                    "Rename Flutter Designer form failed. Target: "
                    + dart.originalPath() + " and " + model.originalPath()
                    + ". Reason: " + rejected.code() + " at "
                    + (rejected.pointer().isBlank() ? "/" : rejected.pointer())
                    + ": " + rejected.message());
        }
        DesignerPairRenamePlan plan = ready.plan();
        requireSourceIntegrity(
                dart,
                sourceScanner.scan(
                        dart.baseline, plan.originalDocument().source()),
                "the current Dart source does not match the .fd metadata");
        requireSourceIntegrity(
                dart,
                sourceScanner.scan(
                        dart.baseline, plan.targetDocument().source()),
                "the unchanged Dart source does not match the renamed metadata");
        return plan;
    }

    private static void requireSourceIntegrity(
            Member dart,
            DartSourceIntegrityResult integrity,
            String fallback) throws IOException {
        if (integrity.onDiskDeclaredMatch()) {
            return;
        }
        String detail = integrity.primaryDiagnostic()
                .map(diagnostic -> diagnostic.code() + ": " + diagnostic.message())
                .orElse(fallback);
        throw new IOException(
                "Rename Flutter Designer form failed. Target: "
                + dart.originalPath() + ". Reason: " + detail);
    }

    private void renameTo(Member member, Location target) throws IOException {
        String name = switch (target) {
            case ORIGINAL -> member.originalName();
            case TOMBSTONE -> member.tombstoneName();
            case TARGET -> member.targetName();
            case UNKNOWN -> throw new IllegalArgumentException(
                    "Cannot rename to an unknown location");
        };
        try {
            backend.rename(member.file(), member.lock, name, member.extension());
        } finally {
            // Filesystem providers may perform the rename before throwing.
            // The rollback reads the observable location rather than trusting
            // the exception boundary.
        }
        if (location(member) != target) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the filesystem did not publish the expected "
                    + target.name().toLowerCase(Locale.ROOT) + " path");
        }
    }

    private IOException rollback(
            Member dart,
            Member model,
            Member firstPath,
            Member secondPath,
            RenameState state,
            Throwable primary,
            PairSaveCoordinator.PairRenameLease lease) {
        List<String> recoveryFailures = new ArrayList<>();
        normalizeTargetToTombstone(secondPath, recoveryFailures);
        normalizeTargetToTombstone(firstPath, recoveryFailures);
        if (state.modelWriteAttempted) {
            restoreModelBytes(model, recoveryFailures);
        }
        restoreOriginal(secondPath, recoveryFailures);
        restoreOriginal(firstPath, recoveryFailures);
        verifyRestored(dart, recoveryFailures);
        verifyRestored(model, recoveryFailures);
        if (!recoveryFailures.isEmpty() && lease != null) {
            lease.markRecoveryConflict(String.join("; ", recoveryFailures));
        }

        IOException failure = new IOException(
                "Rename Flutter Designer form failed. Target: "
                + dart.originalPath() + " and " + model.originalPath()
                + ". Reason: " + reason(primary),
                primary);
        if (!recoveryFailures.isEmpty()) {
            failure.addSuppressed(new IOException(
                    "Flutter Designer pair-rename recovery conflict: "
                    + String.join("; ", recoveryFailures)));
        }
        return failure;
    }

    private void normalizeTargetToTombstone(
            Member member,
            List<String> failures) {
        Location current = location(member);
        if (current == Location.ORIGINAL || current == Location.TOMBSTONE) {
            return;
        }
        if (current != Location.TARGET) {
            failures.add(member.file().getPath()
                    + ": the file is outside every owned rename path");
            return;
        }
        try {
            backend.rename(
                    member.file(), member.lock,
                    member.tombstoneName(), member.extension());
        } catch (IOException | RuntimeException failure) {
            if (location(member) != Location.TOMBSTONE) {
                failures.add(member.file().getPath() + ": " + reason(failure));
            }
        }
    }

    private void restoreModelBytes(Member model, List<String> failures) {
        Location current = location(model);
        if (current == Location.UNKNOWN) {
            failures.add(model.file().getPath()
                    + ": refusing to restore .fd bytes outside the original, "
                    + "target or private staging path");
            return;
        }
        try {
            backend.write(model.file(), model.lock, model.baseline);
        } catch (IOException | RuntimeException failure) {
            try {
                if (!Arrays.equals(model.baseline, backend.read(model.file()))) {
                    failures.add(model.file().getPath() + ": " + reason(failure));
                }
            } catch (IOException | RuntimeException verificationFailure) {
                failures.add(model.file().getPath() + ": " + reason(failure)
                        + "; verification: " + reason(verificationFailure));
            }
        }
    }

    private void restoreOriginal(Member member, List<String> failures) {
        Location current = location(member);
        if (current == Location.ORIGINAL) {
            return;
        }
        if (current != Location.TOMBSTONE) {
            failures.add(member.file().getPath()
                    + ": the tombstone is unavailable for original-name restore");
            return;
        }
        try {
            backend.rename(
                    member.file(), member.lock,
                    member.originalName(), member.extension());
        } catch (IOException | RuntimeException failure) {
            if (location(member) != Location.ORIGINAL) {
                failures.add(member.file().getPath() + ": " + reason(failure));
            }
        }
    }

    private void verifyRestored(Member member, List<String> failures) {
        try {
            FileObject original = member.parent().getFileObject(
                    member.originalName(), member.extension());
            if (original == null
                    || !original.isValid()
                    || !Arrays.equals(member.baseline, backend.read(original))) {
                failures.add(member.originalPath()
                        + ": original path or exact bytes were not restored");
            }
            if (member.parent().getFileObject(
                    member.targetName(), member.extension()) != null) {
                failures.add(member.targetPath()
                        + ": target path remained after rollback");
            }
            if (member.parent().getFileObject(
                    member.tombstoneName(), member.extension()) != null) {
                failures.add(member.tombstonePath()
                        + ": staging path remained after rollback");
            }
        } catch (IOException | RuntimeException failure) {
            failures.add(member.originalPath() + ": " + reason(failure));
        }
    }

    private void verifyTarget(Member member, byte[] expected) throws IOException {
        if (location(member) != Location.TARGET) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the target path was not committed");
        }
        verifyBytes(member, expected,
                "the committed target bytes do not match the prepared rename");
        FileObject old = member.parent().getFileObject(
                member.originalName(), member.extension());
        FileObject tombstone = member.parent().getFileObject(
                member.tombstoneName(), member.extension());
        if (old != null || tombstone != null) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "an original or private staging path remained after commit");
        }
    }

    private void verifyBytes(Member member, byte[] expected, String message)
            throws IOException {
        if (!Arrays.equals(expected, backend.read(member.file()))) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    message);
        }
    }

    private static void bindLock(
            Member target,
            List<Member> order,
            FileLock first,
            FileLock second) {
        target.lock = order.get(0) == target ? first : second;
    }

    private static Location location(Member member) {
        if (!member.file().isValid()
                || member.file().getParent() != member.parent()
                || !member.file().getExt().equals(member.extension())) {
            return Location.UNKNOWN;
        }
        if (member.file().getName().equals(member.originalName())) {
            return Location.ORIGINAL;
        }
        if (member.file().getName().equals(member.tombstoneName())) {
            return Location.TOMBSTONE;
        }
        if (member.file().getName().equals(member.targetName())) {
            return Location.TARGET;
        }
        return Location.UNKNOWN;
    }

    private static void ensurePathVacant(Member member, Location location)
            throws IOException {
        String name = location == Location.TARGET
                ? member.targetName() : member.tombstoneName();
        if (member.parent().getFileObject(name, member.extension()) != null) {
            throw failure(
                    "Rename Flutter Designer form",
                    member.file(),
                    "the " + location.name().toLowerCase(Locale.ROOT)
                    + " path became occupied before commit");
        }
    }

    private static String requireTargetStem(String value) {
        if (value == null || !value.equals(value.strip())
                || !TARGET_STEM.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Flutter Designer filename must use lower_snake_case "
                    + "without an extension.");
        }
        return value;
    }

    private static String requireTransactionId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{8,80}")) {
            throw new IllegalStateException(
                    "Pair-rename transaction id is unavailable or unsafe");
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

    record RenameResult(
            boolean committed,
            FileObject dartFile,
            FileObject modelFile) {
        RenameResult {
            if (!committed) {
                throw new IllegalArgumentException(
                        "A returned pair-rename result must be committed");
            }
            Objects.requireNonNull(dartFile, "dartFile");
            Objects.requireNonNull(modelFile, "modelFile");
        }
    }

    interface Backend {
        Path realPath(FileObject file) throws IOException;

        FileLock lock(FileObject file) throws IOException;

        byte[] read(FileObject file) throws IOException;

        void write(FileObject file, FileLock lock, byte[] bytes)
                throws IOException;

        void rename(FileObject file, FileLock lock, String name, String extension)
                throws IOException;

        void runAtomicAction(FileSystem fileSystem, FileSystem.AtomicAction action)
                throws IOException;
    }

    private enum Location {
        ORIGINAL,
        TOMBSTONE,
        TARGET,
        UNKNOWN
    }

    private static final class RenameState {
        private boolean modelWriteAttempted;
        private boolean committed;
    }

    private static final class RenameAtomicAction
            implements FileSystem.AtomicAction {
        private FileSystem.AtomicAction delegate;

        RenameAtomicAction(FileSystem.AtomicAction delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() throws IOException {
            FileSystem.AtomicAction current = delegate;
            if (current == null) {
                throw new IOException(
                        "The Flutter Designer pair-rename action has already finished");
            }
            current.run();
        }

        void clear() {
            delegate = null;
        }
    }

    private static final class Member {
        private final String role;
        private final FileObject file;
        private final FileObject parent;
        private final String originalName;
        private final String extension;
        private final Path realPath;
        private final String targetName;
        private final String tombstoneName;
        private FileLock lock;
        private byte[] baseline;

        Member(
                String role,
                FileObject file,
                FileObject parent,
                String originalName,
                String extension,
                Path realPath,
                String targetName,
                String tombstoneName) {
            this.role = role;
            this.file = file;
            this.parent = parent;
            this.originalName = originalName;
            this.extension = extension;
            this.realPath = realPath;
            this.targetName = targetName;
            this.tombstoneName = tombstoneName;
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

        String extension() {
            return extension;
        }

        Path realPath() {
            return realPath;
        }

        String targetName() {
            return targetName;
        }

        String tombstoneName() {
            return tombstoneName;
        }

        String originalPath() {
            return parent.getPath() + "/" + originalName + "." + extension;
        }

        String targetPath() {
            return parent.getPath() + "/" + targetName + "." + extension;
        }

        String tombstonePath() {
            return parent.getPath() + "/" + tombstoneName + "." + extension;
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
        public FileLock lock(FileObject file) throws IOException {
            return file.lock();
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            return file.asBytes();
        }

        @Override
        public void write(FileObject file, FileLock lock, byte[] bytes)
                throws IOException {
            try (OutputStream output = file.getOutputStream(lock)) {
                output.write(bytes);
            }
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
        public void runAtomicAction(
                FileSystem fileSystem,
                FileSystem.AtomicAction action) throws IOException {
            fileSystem.runAtomicAction(action);
        }
    }
}

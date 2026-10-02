package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.codec.FdCodecLimits;
import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.FdInputLimitException;
import io.github.vgrytsenko2022.designer.codec.OriginalFdBytes;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/** Moves one complete Dart/.fd form between mirrored project folders. */
final class FlutterDesignerPairMove {
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerPairMove.class.getName());
    private static final String PRIVATE_EXTENSION = "nbmove";
    private static final FlutterDesignerPairMove DEFAULT =
            new FlutterDesignerPairMove(
                    new NetBeansBackend(),
                    () -> UUID.randomUUID().toString(),
                    new DartSourceIntegrityScanner(),
                    new FlutterDesignerMoveDependencyGuard());

    private final Backend backend;
    private final Supplier<String> transactionIds;
    private final DartSourceIntegrityScanner sourceScanner;
    private final DependencyGuard dependencyGuard;
    private final FdDocumentCodec codec = new FdDocumentCodec();

    FlutterDesignerPairMove(
            Backend backend,
            Supplier<String> transactionIds,
            DependencyGuard dependencyGuard) {
        this(backend, transactionIds,
                new DartSourceIntegrityScanner(), dependencyGuard);
    }

    FlutterDesignerPairMove(
            Backend backend,
            Supplier<String> transactionIds,
            DartSourceIntegrityScanner sourceScanner,
            DependencyGuard dependencyGuard) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.transactionIds = Objects.requireNonNull(
                transactionIds, "transactionIds");
        this.sourceScanner = Objects.requireNonNull(
                sourceScanner, "sourceScanner");
        this.dependencyGuard = Objects.requireNonNull(
                dependencyGuard, "dependencyGuard");
    }

    static boolean isAllowed(FileObject member) {
        return DEFAULT.canMove(member);
    }

    static boolean isPasteTarget(FileObject member, FileObject targetFolder) {
        return DEFAULT.canPaste(member, targetFolder);
    }

    static MoveResult move(FileObject member, FileObject targetFolder)
            throws IOException {
        return DEFAULT.moveMember(member, targetFolder);
    }

    boolean canMove(FileObject member) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        return resolved.isPresent() && canMove(resolved.orElseThrow());
    }

    boolean canMove(FlutterDesignerPairLayout.Pair pair) {
        Objects.requireNonNull(pair, "pair");
        return pair.dartFile().isValid()
                && pair.modelFile().isValid()
                && pair.dartFile().canWrite()
                && pair.modelFile().canWrite()
                && pair.dartFile().getParent() != null
                && pair.modelFile().getParent() != null
                && pair.dartFile().getParent().canWrite()
                && pair.modelFile().getParent().canWrite()
                && cleanDesignerOwner(pair.dartFile());
    }

    boolean canPaste(FileObject member, FileObject targetFolder) {
        Optional<FlutterDesignerPairLayout.Pair> resolved =
                FlutterDesignerPairLayout.findCompletePair(member);
        if (resolved.isEmpty() || !canMove(resolved.orElseThrow())) {
            return false;
        }
        try {
            TargetFolders target = resolveTarget(
                    resolved.orElseThrow(), member, targetFolder);
            return targetsVacant(resolved.orElseThrow(), target);
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    MoveResult moveMember(FileObject member, FileObject targetFolder)
            throws IOException {
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(member)
                .orElseThrow(() -> failure(
                        member,
                        "a complete safe Dart/.fd pair could not be resolved"));
        TargetFolders target = resolveTarget(pair, member, targetFolder);
        PairSaveCoordinator.PairMoveLease lease =
                acquireMoveAuthority(pair, member);
        boolean committed = false;
        try {
            MoveResult result = moveResolved(pair, member, target, lease);
            committed = result.committed();
            return result;
        } finally {
            lease.finish(committed);
        }
    }

    MoveResult moveResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            FileObject targetFolder) throws IOException {
        return moveResolved(
                pair,
                initiatingMember,
                resolveTarget(pair, initiatingMember, targetFolder),
                null);
    }

    private MoveResult moveResolved(
            FlutterDesignerPairLayout.Pair pair,
            FileObject initiatingMember,
            TargetFolders target,
            PairSaveCoordinator.PairMoveLease lease) throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(initiatingMember, "initiatingMember");
        Objects.requireNonNull(target, "target");
        if (!initiatingMember.equals(pair.dartFile())
                && !initiatingMember.equals(pair.modelFile())) {
            throw failure(initiatingMember,
                    "the initiating file is not a member of the pair");
        }

        FileSystem fileSystem = pair.dartFile().getFileSystem();
        if (!fileSystem.equals(pair.modelFile().getFileSystem())
                || !fileSystem.equals(target.dartFolder().getFileSystem())
                || !fileSystem.equals(target.modelFolder().getFileSystem())) {
            throw failure(initiatingMember,
                    "the source and mirrored destination are split across filesystems");
        }

        String transactionId = requireTransactionId(transactionIds.get());
        String privateStem = ".nb-flutter-move-" + transactionId;
        SourceMember dart = sourceMember(
                "dart", pair.dartFile(), target.dartFolder(), privateStem);
        SourceMember model = sourceMember(
                "model", pair.modelFile(), target.modelFolder(), privateStem);
        verifyTargetBinding(pair, target, initiatingMember);
        verifyDistinctPhysicalFiles(dart, model, initiatingMember);
        verifyDistinctDestinationFolders(dart, model, initiatingMember);

        List<LockedFolder> folderLocks = acquireFolderLocks(
                pair, dart, model, target, initiatingMember);
        try {
            List<SourceMember> lockOrder = new ArrayList<>(List.of(dart, model));
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
            validatePairContents(dart, model);
            dependencyGuard.verify(
                    pair,
                    target.relativeFolder(),
                    dart.baseline());
            verifySourceExact(dart);
            verifySourceExact(model);

            TargetMember targetDart = new TargetMember(
                    "dart", dart.targetParent(), dart.name(), dart.extension(),
                    privateStem, dart.baseline());
            TargetMember targetModel = new TargetMember(
                    "model", model.targetParent(), model.name(), model.extension(),
                    privateStem, model.baseline());
            ensureTargetVacant(targetDart);
            ensureTargetVacant(targetModel);

            SourceMember firstPath = initiatingMember.equals(dart.file())
                    ? model : dart;
            SourceMember secondPath = initiatingMember.equals(dart.file())
                    ? dart : model;
            MoveState state = new MoveState();
            MoveAtomicAction moveAction = new MoveAtomicAction(() -> {
                try {
                    verifySourceExact(dart);
                    verifySourceExact(model);
                    verifyTargetBinding(pair, target, initiatingMember);
                    ensureTargetVacant(targetDart);
                    ensureTargetVacant(targetModel);
                    createAndWrite(targetDart);
                    createAndWrite(targetModel);
                    verifySourceExact(dart);
                    verifySourceExact(model);
                    verifyTargetBinding(pair, target, initiatingMember);
                    // Publish the visible model first. When Dart arrives, the
                    // pair-aware loader can claim the complete pair directly;
                    // publishing Dart first creates a transient ordinary Dart
                    // owner and can provoke an editor-close prompt during
                    // ownership promotion.
                    state.firstTargetLock = publishAndRetainLock(targetModel);
                    state.secondTargetLock = publishAndRetainLock(targetDart);
                    verifyLockedPublishedTargets(
                            targetDart, targetModel, initiatingMember, state);
                    verifyTargetBinding(pair, target, initiatingMember);
                    verifyPublishedPair(pair, targetDart, targetModel);
                    renameSourceToTombstone(firstPath);
                    renameSourceToTombstone(secondPath);
                    verifySourceTombstone(dart);
                    verifySourceTombstone(model);
                    verifyTargetBinding(pair, target, initiatingMember);
                    verifyPublishedPair(pair, targetDart, targetModel);
                    // The logical Move is now durable: a verified target pair
                    // exists and both original FileObjects still own exact,
                    // non-Designer tombstones.  Cleanup must never turn this
                    // committed state into a rollback with replaced identities.
                    state.committed = true;
                    completeCommittedCleanup(
                            pair, target, initiatingMember,
                            dart, model, targetDart, targetModel,
                            firstPath, secondPath, state, lease);
                } catch (IOException | RuntimeException moveFailure) {
                    if (!state.committed) {
                        releaseTargetLocks(state);
                        throw rollback(
                                dart, model, targetDart, targetModel,
                                moveFailure, lease);
                    }
                    recordPostCommitFailure(
                            state, lease,
                            "the committed filesystem delegate reported a late failure",
                            moveFailure);
                }
            });

            try {
                if (lease != null) {
                    lease.bind(moveAction);
                }
                dependencyGuard.verifyExclusively(
                        pair,
                        target.relativeFolder(),
                        dart.baseline(),
                        callerLockedFiles(dart, model, folderLocks),
                        () -> backend.runAtomicAction(fileSystem, moveAction));
            } catch (IOException | RuntimeException atomicFailure) {
                if (!state.committed) {
                    throw atomicFailure;
                }
                boolean verifiedAfterLateFailure = false;
                try {
                    verifyTargetBinding(pair, target, initiatingMember);
                    verifyCommitted(
                            pair, dart, model, targetDart, targetModel);
                    verifiedAfterLateFailure = true;
                } catch (IOException | RuntimeException verificationFailure) {
                    verificationFailure.addSuppressed(atomicFailure);
                    recordPostCommitFailure(
                            state, lease,
                            "the exact committed pair could not be reverified after a late filesystem failure",
                            verificationFailure);
                }
                if (verifiedAfterLateFailure) {
                    LOGGER.log(Level.WARNING,
                            "The filesystem reported pair-Move failure after its "
                            + "delegate committed; the exact target pair and source "
                            + "removal were verified",
                            atomicFailure);
                }
            } finally {
                moveAction.clear();
                releaseTargetLocks(state);
            }

            if (!state.committed) {
                throw new IOException(
                        "Move Flutter Designer form did not execute its filesystem transaction.");
            }
            if (!state.cleanupFailures.isEmpty()) {
                LOGGER.log(Level.WARNING,
                        "Flutter Designer pair-Move committed, but private "
                        + "tombstone cleanup was incomplete: {0}",
                        String.join("; ", state.cleanupFailures));
            }
            if (!state.postCommitFailures.isEmpty()) {
                LOGGER.log(Level.WARNING,
                        "Flutter Designer pair-Move committed with a recovery "
                        + "warning: {0}",
                        String.join("; ", state.postCommitFailures));
            }
            return new MoveResult(
                    true,
                    Objects.requireNonNull(targetDart.ownedFile, "target Dart"),
                    Objects.requireNonNull(targetModel.ownedFile, "target model"));
            } finally {
                release(secondLock);
                release(firstLock);
            }
        } finally {
            releaseFolderLocks(folderLocks);
        }
    }

    private PairSaveCoordinator.PairMoveLease acquireMoveAuthority(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member) throws IOException {
        if (!pair.dartFile().canWrite() || !pair.modelFile().canWrite()) {
            throw failure(member, "one of the paired files is read-only");
        }
        DataObject owner;
        try {
            owner = DataObject.find(pair.dartFile());
        } catch (IOException lookupFailure) {
            throw failure(pair.dartFile(),
                    "the paired Dart editor owner could not be resolved: "
                    + reason(lookupFailure));
        }
        if (owner.isModified()) {
            throw failure(pair.dartFile(),
                    "the paired Dart source has unsaved changes; save it before moving the form");
        }
        if (!(owner instanceof FlutterDesignerDataObject designerOwner)) {
            throw failure(pair.dartFile(),
                    "the paired Dart source is not owned by the Flutter Designer session");
        }
        try {
            return designerOwner.getPairSaveCoordinator().beginPairMove();
        } catch (IOException reservationFailure) {
            throw failure(member, reason(reservationFailure));
        }
    }

    private boolean cleanDesignerOwner(FileObject dartFile) {
        try {
            DataObject owner = DataObject.find(dartFile);
            return owner instanceof FlutterDesignerDataObject designerOwner
                    && !owner.isModified()
                    && designerOwner.getPairSaveCoordinator()
                            .canBeginPairMove();
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private TargetFolders resolveTarget(
            FlutterDesignerPairLayout.Pair pair,
            FileObject member,
            FileObject requestedFolder) throws IOException {
        if (requestedFolder == null || !requestedFolder.isValid()
                || !requestedFolder.isFolder() || !requestedFolder.canWrite()) {
            throw failure(requestedFolder,
                    "the requested destination is not a writable folder");
        }
        Project destinationOwner = FileOwnerQuery.getOwner(requestedFolder);
        if (destinationOwner == null
                || !destinationOwner.getProjectDirectory().equals(
                        pair.project().getProjectDirectory())) {
            throw failure(requestedFolder,
                    "Move is limited to the same Flutter project");
        }

        FileObject project = pair.project().getProjectDirectory();
        FileObject dartRoot = project.getFileObject(
                FlutterDesignerPairLayout.DART_ROOT);
        FileObject modelRoot = project.getFileObject(
                FlutterDesignerPairLayout.MODEL_ROOT);
        if (dartRoot == null || modelRoot == null
                || !dartRoot.isFolder() || !modelRoot.isFolder()) {
            throw failure(project,
                    "the lib/.fd_templates mirror roots are unavailable");
        }

        final FileObject dartFolder;
        final FileObject modelFolder;
        final String relative;
        if (member.equals(pair.dartFile())) {
            relative = FileUtil.getRelativePath(dartRoot, requestedFolder);
            if (!validRelativeFolder(relative)) {
                throw failure(requestedFolder,
                        "a Dart form can be moved only to a folder under lib");
            }
            dartFolder = requestedFolder;
            modelFolder = mirrorFolder(modelRoot, relative);
        } else if (member.equals(pair.modelFile())) {
            relative = FileUtil.getRelativePath(modelRoot, requestedFolder);
            if (!validRelativeFolder(relative)) {
                throw failure(requestedFolder,
                        "an .fd form can be moved only to a folder under .fd_templates");
            }
            modelFolder = requestedFolder;
            dartFolder = mirrorFolder(dartRoot, relative);
        } else {
            throw failure(member, "the initiating file is not a pair member");
        }

        if (dartFolder.equals(pair.dartFile().getParent())
                || modelFolder.equals(pair.modelFile().getParent())) {
            throw failure(requestedFolder,
                    "the destination resolves to the form's current folder");
        }
        if (!dartFolder.canWrite() || !modelFolder.canWrite()) {
            throw failure(requestedFolder,
                    "one of the mirrored destination folders is read-only");
        }
        requireExactProjectOwner(
                pair, dartFolder, requestedFolder, "lib destination");
        requireExactProjectOwner(
                pair, modelFolder, requestedFolder,
                ".fd_templates destination");
        requireSafeFolder(pair.project(), dartRoot, dartFolder, "lib");
        requireSafeFolder(
                pair.project(), modelRoot, modelFolder, ".fd_templates");
        return new TargetFolders(
                dartFolder,
                modelFolder,
                relative,
                captureFolderIdentity(dartFolder),
                captureFolderIdentity(modelFolder));
    }

    private static void verifyTargetBinding(
            FlutterDesignerPairLayout.Pair pair,
            TargetFolders target,
            FileObject initiatingMember) throws IOException {
        FileObject project = pair.project().getProjectDirectory();
        FileObject dartRoot = project.getFileObject(
                FlutterDesignerPairLayout.DART_ROOT);
        FileObject modelRoot = project.getFileObject(
                FlutterDesignerPairLayout.MODEL_ROOT);
        if (dartRoot == null || modelRoot == null
                || !dartRoot.isFolder() || !modelRoot.isFolder()
                || !target.dartFolder().isValid()
                || !target.modelFolder().isValid()
                || !target.dartFolder().isFolder()
                || !target.modelFolder().isFolder()
                || !target.dartFolder().canWrite()
                || !target.modelFolder().canWrite()) {
            throw failure(initiatingMember,
                    "the mirrored destination folders changed or became unavailable");
        }

        String dartRelative = FileUtil.getRelativePath(
                dartRoot, target.dartFolder());
        String modelRelative = FileUtil.getRelativePath(
                modelRoot, target.modelFolder());
        FileObject expectedDart = target.relativeFolder().isEmpty()
                ? dartRoot : dartRoot.getFileObject(target.relativeFolder());
        FileObject expectedModel = target.relativeFolder().isEmpty()
                ? modelRoot : modelRoot.getFileObject(target.relativeFolder());
        Project dartOwner = FileOwnerQuery.getOwner(target.dartFolder());
        Project modelOwner = FileOwnerQuery.getOwner(target.modelFolder());
        if (!target.relativeFolder().equals(dartRelative)
                || !target.relativeFolder().equals(modelRelative)
                || expectedDart != target.dartFolder()
                || expectedModel != target.modelFolder()
                || dartOwner == null
                || modelOwner == null
                || !dartOwner.getProjectDirectory().equals(project)
                || !modelOwner.getProjectDirectory().equals(project)) {
            throw failure(initiatingMember,
                    "the destination no longer owns the exact mirrored project paths");
        }

        requireSafeFolder(
                pair.project(), dartRoot, target.dartFolder(), "lib");
        requireSafeFolder(
                pair.project(), modelRoot, target.modelFolder(),
                ".fd_templates");
        if (!target.dartIdentity().samePhysicalFolder(
                    captureFolderIdentity(target.dartFolder()))
                || !target.modelIdentity().samePhysicalFolder(
                    captureFolderIdentity(target.modelFolder()))) {
            throw failure(initiatingMember,
                    "a destination folder was renamed, replaced, or redirected");
        }
    }

    private static FolderIdentity captureFolderIdentity(FileObject folder)
            throws IOException {
        Path absolute = localFile(folder).toPath()
                .toAbsolutePath().normalize();
        BasicFileAttributes attributes = Files.readAttributes(
                absolute,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
            throw failure(folder,
                    "the destination is not a safe physical directory");
        }
        return new FolderIdentity(
                absolute,
                absolute.toRealPath(),
                attributes.fileKey());
    }

    private static void requireExactProjectOwner(
            FlutterDesignerPairLayout.Pair pair,
            FileObject folder,
            FileObject initiatingMember,
            String label) throws IOException {
        Project owner = FileOwnerQuery.getOwner(folder);
        if (owner == null || !owner.getProjectDirectory().equals(
                pair.project().getProjectDirectory())) {
            throw failure(initiatingMember,
                    label + " belongs to a different project");
        }
    }

    private static List<LockedFolder> acquireFolderLocks(
            FlutterDesignerPairLayout.Pair pair,
            SourceMember dart,
            SourceMember model,
            TargetFolders target,
            FileObject initiatingMember) throws IOException {
        FileObject project = pair.project().getProjectDirectory();
        Map<FileObject, FolderLockPlan> unique = new IdentityHashMap<>();
        addFolderChain(unique, project, dart.originalParent(), initiatingMember);
        addFolderChain(unique, project, model.originalParent(), initiatingMember);
        addFolderChain(unique, project, target.dartFolder(), initiatingMember);
        addFolderChain(unique, project, target.modelFolder(), initiatingMember);

        List<FolderLockPlan> plans = new ArrayList<>(unique.values());
        plans.sort(Comparator
                .comparing((FolderLockPlan plan) -> lockKey(
                        plan.identity().realPath()))
                .thenComparing(plan -> lockKey(
                        plan.identity().absolutePath())));
        List<LockedFolder> acquired = new ArrayList<>(plans.size());
        try {
            for (FolderLockPlan plan : plans) {
                FileLock lock = plan.folder().lock();
                if (!lock.isValid()) {
                    release(lock);
                    throw failure(plan.folder(),
                            "an ancestor folder lock was not retained");
                }
                acquired.add(new LockedFolder(
                        plan.folder(), plan.identity(), lock));
            }
            for (LockedFolder locked : acquired) {
                FileObject canonical = FileUtil.toFileObject(
                        locked.identity().absolutePath().toFile());
                if (!locked.folder().isValid()
                        || !locked.lock().isValid()
                        || canonical != locked.folder()
                        || !locked.identity().samePhysicalFolder(
                                captureFolderIdentity(locked.folder()))) {
                    throw failure(locked.folder(),
                            "a source or destination ancestor folder changed while locks were acquired");
                }
            }
            verifyTargetBinding(pair, target, initiatingMember);
            return List.copyOf(acquired);
        } catch (IOException | RuntimeException failure) {
            releaseFolderLocks(acquired);
            throw failure;
        }
    }

    private static void addFolderChain(
            Map<FileObject, FolderLockPlan> unique,
            FileObject project,
            FileObject leaf,
            FileObject initiatingMember) throws IOException {
        FileObject current = leaf;
        while (current != null) {
            if (!current.isValid() || !current.isFolder()) {
                throw failure(initiatingMember,
                        "a source or destination ancestor folder is unavailable");
            }
            if (!unique.containsKey(current)) {
                unique.put(current, new FolderLockPlan(
                        current, captureFolderIdentity(current)));
            }
            if (current == project) {
                return;
            }
            current = current.getParent();
        }
        throw failure(initiatingMember,
                "a source or destination folder escaped the exact project ancestry");
    }

    private static void releaseFolderLocks(List<LockedFolder> locks) {
        for (int index = locks.size() - 1; index >= 0; index--) {
            release(locks.get(index).lock());
        }
    }

    private static List<DependencyGuard.CallerLock> callerLockedFiles(
            SourceMember dart,
            SourceMember model,
            List<LockedFolder> folderLocks) {
        List<DependencyGuard.CallerLock> locked = new ArrayList<>(
                folderLocks.size() + 2);
        for (LockedFolder folder : folderLocks) {
            locked.add(new DependencyGuard.CallerLock(
                    folder.folder(), folder.lock()));
        }
        locked.add(new DependencyGuard.CallerLock(dart.file(), dart.lock()));
        locked.add(new DependencyGuard.CallerLock(model.file(), model.lock()));
        return List.copyOf(locked);
    }

    private static FileObject mirrorFolder(FileObject root, String relative)
            throws IOException {
        FileObject folder = relative.isEmpty()
                ? root : root.getFileObject(relative);
        if (folder == null || !folder.isFolder()) {
            throw failure(root,
                    "the mirrored destination folder does not exist: "
                    + (relative.isEmpty() ? "/" : relative));
        }
        return folder;
    }

    private static boolean validRelativeFolder(String relative) {
        if (relative == null || relative.indexOf('\\') >= 0
                || relative.startsWith("/") || relative.endsWith("/")) {
            return false;
        }
        if (relative.isEmpty()) {
            return true;
        }
        for (String segment : relative.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private static void requireSafeFolder(
            FlutterProject project,
            FileObject expectedRoot,
            FileObject folder,
            String label) throws IOException {
        File projectFile = localFile(project.getProjectDirectory());
        File rootFile = localFile(expectedRoot);
        File folderFile = localFile(folder);
        Path projectPath = projectFile.toPath().toAbsolutePath().normalize();
        Path rootPath = rootFile.toPath().toAbsolutePath().normalize();
        Path folderPath = folderFile.toPath().toAbsolutePath().normalize();
        if (!rootPath.startsWith(projectPath) || !folderPath.startsWith(rootPath)
                || !Files.isDirectory(rootPath, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(folderPath, LinkOption.NOFOLLOW_LINKS)
                || containsSymbolicLink(projectPath, folderPath)) {
            throw failure(folder,
                    "the " + label + " destination is not a safe physical project folder");
        }
        Path realProject = projectPath.toRealPath();
        Path realRoot = rootPath.toRealPath();
        Path realFolder = folderPath.toRealPath();
        if (!realRoot.startsWith(realProject)
                || !realFolder.startsWith(realRoot)) {
            throw failure(folder,
                    "the " + label + " destination escapes its physical project root");
        }
    }

    private static File localFile(FileObject file) throws IOException {
        final File local;
        try {
            local = FileUtil.toFile(file);
        } catch (AssertionError invalidMasterFile) {
            throw new IOException(
                    "NetBeans could not resolve a normalized local file",
                    invalidMasterFile);
        }
        if (local == null) {
            throw new IOException("The destination is not on a local filesystem");
        }
        return local;
    }

    private static boolean containsSymbolicLink(Path root, Path target) {
        if (!target.startsWith(root)) {
            return true;
        }
        Path current = root;
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean targetsVacant(
            FlutterDesignerPairLayout.Pair pair,
            TargetFolders target) {
        String stem = pair.dartFile().getName();
        return target.dartFolder().getFileObject(stem, "dart") == null
                && target.modelFolder().getFileObject(stem, "fd") == null;
    }

    private SourceMember sourceMember(
            String role,
            FileObject file,
            FileObject targetParent,
            String privateStem) throws IOException {
        FileObject parent = file.getParent();
        if (parent == null || !parent.isFolder()) {
            throw failure(file, "the source parent folder is unavailable");
        }
        if (parent.getFileObject(privateStem, PRIVATE_EXTENSION) != null) {
            throw failure(file,
                    "the private Move tombstone path is already occupied");
        }
        if (targetParent.getFileObject(privateStem, PRIVATE_EXTENSION) != null
                || targetParent.getFileObject(file.getName(), file.getExt()) != null) {
            throw failure(targetParent,
                    "the destination or private Move staging path is already occupied");
        }
        return new SourceMember(
                role, file, parent, targetParent,
                file.getName(), file.getExt(), privateStem,
                PRIVATE_EXTENSION,
                backend.realPath(file));
    }

    private void captureLockedBaseline(SourceMember member) throws IOException {
        if (sourceLocation(member) != SourceLocation.ORIGINAL
                || !backend.realPath(member.file()).equals(member.realPath())) {
            throw failure(member.file(),
                    "the pair changed before both source locks were acquired");
        }
        if (member.originalParent().getFileObject(
                member.privateStem(), member.privateExtension()) != null
                || member.targetParent().getFileObject(
                        member.privateStem(), PRIVATE_EXTENSION) != null
                || member.targetParent().getFileObject(
                        member.name(), member.extension()) != null) {
            throw failure(member.file(),
                    "a source tombstone, staging path, or destination became occupied");
        }
        long maximum = member.role().equals("dart")
                ? sourceScanner.limits().maxSourceBytes()
                : FdCodecLimits.DEFAULT_MAX_DOCUMENT_BYTES;
        if (member.file().getSize() > maximum) {
            throw failure(member.file(),
                    "the file exceeds the bounded Move safety limit");
        }
        member.baseline = backend.read(member.file());
        if (member.baseline.length > maximum) {
            throw failure(member.file(),
                    "the file grew beyond the bounded Move safety limit");
        }
    }

    private void validatePairContents(SourceMember dart, SourceMember model)
            throws IOException {
        final OriginalFdBytes originalFd;
        try {
            originalFd = OriginalFdBytes.copyOf(
                    model.baseline(), FdCodecLimits.defaults());
        } catch (FdInputLimitException limitFailure) {
            throw new IOException(
                    "Move Flutter Designer form failed. Target: "
                    + model.originalPath() + ". Reason: "
                    + reason(limitFailure) + ".",
                    limitFailure);
        }
        FdDecodeResult decoded = codec.decode(originalFd);
        if (!(decoded instanceof FdDecodeResult.Current current)
                || current.migrated()) {
            throw failure(model.file(),
                    "only a valid current-version non-migrated .fd model can be moved");
        }
        String dartName = dart.name() + "." + dart.extension();
        if (!current.document().source().dartFile().equals(dartName)) {
            throw failure(model.file(),
                    "source.dartFile does not name the exact paired Dart file");
        }
        DartSourceIntegrityResult integrity = sourceScanner.scan(
                dart.baseline(), current.document().source());
        if (!integrity.onDiskDeclaredMatch()) {
            String detail = integrity.primaryDiagnostic()
                    .map(item -> item.code() + ": " + item.message())
                    .orElse("the current Dart source does not match the .fd metadata");
            throw failure(dart.file(), detail);
        }
    }

    private void verifySourceExact(SourceMember member) throws IOException {
        if (sourceLocation(member) != SourceLocation.ORIGINAL
                || !backend.realPath(member.file()).equals(member.realPath())
                || !Arrays.equals(member.baseline(), backend.read(member.file()))) {
            throw failure(member.file(),
                    "the source pair changed after its exact Move snapshot was captured");
        }
    }

    private void renameSourceToTombstone(SourceMember member)
            throws IOException {
        try {
            backend.renameSourceOwned(
                    member.file(), member.lock,
                    member.originalParent(), member.name(), member.extension(),
                    member.baseline(), member.privateStem(),
                    member.privateExtension());
        } catch (IOException | RuntimeException renameFailure) {
            if (sourceLocation(member) != SourceLocation.TOMBSTONE
                    || !exactSourceBytes(member)) {
                throw renameFailure;
            }
            LOGGER.log(Level.FINE,
                    "A pair-Move source staging rename completed before its provider reported failure",
                    renameFailure);
        }
        if (sourceLocation(member) != SourceLocation.TOMBSTONE) {
            throw failure(member.file(),
                    "the filesystem did not publish the private Move tombstone");
        }
    }

    private void createAndWrite(TargetMember member) throws IOException {
        ensureTargetVacant(member);
        try {
            member.ownedFile = backend.create(
                    member.parent(), member.stagingStem(),
                    member.stagingExtension());
        } catch (IOException | RuntimeException createFailure) {
            if (member.parent().getFileObject(
                    member.stagingStem(), member.stagingExtension()) != null) {
                member.unownedPathObserved = true;
            }
            throw createFailure;
        }
        if (targetLocation(member) != TargetLocation.STAGING) {
            throw failure(member.ownedFile,
                    "the created target did not retain its private Move staging identity");
        }
        try {
            backend.writeOwned(
                    member.ownedFile, member.parent(), member.stagingStem(),
                    member.stagingExtension(), member.expectedBytes());
        } catch (IOException | RuntimeException writeFailure) {
            if (targetLocation(member) != TargetLocation.STAGING
                    || !exactTargetBytes(member)) {
                throw writeFailure;
            }
            LOGGER.log(Level.FINE,
                    "A pair-Move staging write completed before its provider reported failure",
                    writeFailure);
        }
        verifyTargetBytes(member, "the staged target bytes are not exact");
    }

    private FileLock publishAndRetainLock(TargetMember member)
            throws IOException {
        FileLock retained = null;
        try {
            retained = backend.publishOwned(
                    member.ownedFile, member.parent(), member.stagingStem(),
                    member.stagingExtension(), member.expectedBytes(),
                    member.targetStem(), member.extension());
        } catch (IOException | RuntimeException publishFailure) {
            if (targetLocation(member) != TargetLocation.TARGET
                    || !exactTargetBytes(member)) {
                throw publishFailure;
            }
            // A provider may report an error after the rename reached disk.
            // Normal production publication returns the original still-held
            // lock, so this recovery-only reacquire path is not the commit path.
            retained = backend.lock(member.ownedFile);
            LOGGER.log(Level.FINE,
                    "A pair-Move publish completed before its provider reported failure",
                    publishFailure);
        }
        try {
            if (retained == null || !retained.isValid()) {
                throw failure(member.ownedFile,
                        "the published target did not retain its FileLock");
            }
            verifyPublished(member);
            return retained;
        } catch (IOException | RuntimeException verificationFailure) {
            release(retained);
            throw verificationFailure;
        }
    }

    private void publishForRollbackRestore(TargetMember member)
            throws IOException {
        FileLock retained = publishAndRetainLock(member);
        release(retained);
    }

    private void deleteSourceTombstone(SourceMember member) throws IOException {
        try {
            backend.deleteSourceOwned(
                    member.file(), member.lock, member.originalParent(),
                    member.privateStem(), member.privateExtension(),
                    member.baseline());
        } catch (IOException | RuntimeException deleteFailure) {
            if (sourceLocation(member) != SourceLocation.ABSENT) {
                throw deleteFailure;
            }
            LOGGER.log(Level.FINE,
                    "A pair-Move source deletion completed before its provider reported failure",
                    deleteFailure);
        }
        if (sourceLocation(member) != SourceLocation.ABSENT) {
            throw failure(member.file(),
                    "the source tombstone remained after target publication");
        }
    }

    private void cleanupSourceTombstone(
            SourceMember member,
            MoveState state) {
        try {
            deleteSourceTombstone(member);
        } catch (IOException | RuntimeException cleanupFailure) {
            SourceLocation observed = sourceLocation(member);
            if (observed == SourceLocation.ABSENT) {
                return;
            }
            if (observed == SourceLocation.TOMBSTONE
                    && exactSourceBytesQuietly(member)) {
                state.cleanupFailures.add(member.file().getPath()
                        + ": " + reason(cleanupFailure));
                return;
            }
            state.cleanupFailures.add(member.originalPath()
                    + ": source cleanup lost exact tombstone authority: "
                    + reason(cleanupFailure));
        }
    }

    private void completeCommittedCleanup(
            FlutterDesignerPairLayout.Pair pair,
            TargetFolders target,
            FileObject initiatingMember,
            SourceMember dart,
            SourceMember model,
            TargetMember targetDart,
            TargetMember targetModel,
            SourceMember firstPath,
            SourceMember secondPath,
            MoveState state,
            PairSaveCoordinator.PairMoveLease lease) {
        if (!verifyCommittedPhase(
                pair, target, initiatingMember,
                dart, model, targetDart, targetModel,
                state, lease, "before source cleanup")) {
            return;
        }
        cleanupSourceTombstone(firstPath, state);
        if (!verifyCommittedPhase(
                pair, target, initiatingMember,
                dart, model, targetDart, targetModel,
                state, lease, "after the first source cleanup")) {
            return;
        }
        cleanupSourceTombstone(secondPath, state);
        verifyCommittedPhase(
                pair, target, initiatingMember,
                dart, model, targetDart, targetModel,
                state, lease, "after final source cleanup");
    }

    private boolean verifyCommittedPhase(
            FlutterDesignerPairLayout.Pair pair,
            TargetFolders target,
            FileObject initiatingMember,
            SourceMember dart,
            SourceMember model,
            TargetMember targetDart,
            TargetMember targetModel,
            MoveState state,
            PairSaveCoordinator.PairMoveLease lease,
            String phase) {
        try {
            verifyTargetBinding(pair, target, initiatingMember);
            verifyCommitted(
                    pair, dart, model, targetDart, targetModel);
            return true;
        } catch (IOException | RuntimeException verificationFailure) {
            recordPostCommitFailure(
                    state, lease,
                    "committed pair verification failed " + phase,
                    verificationFailure);
            return false;
        }
    }

    private static void recordPostCommitFailure(
            MoveState state,
            PairSaveCoordinator.PairMoveLease lease,
            String operation,
            Throwable failure) {
        String detail = operation + ": " + reason(failure);
        state.postCommitFailures.add(detail);
        if (lease != null) {
            lease.markRecoveryConflict(detail);
        }
        LOGGER.log(Level.WARNING,
                "Flutter Designer pair-Move is already committed; " + detail,
                failure);
    }

    private void verifySourceTombstone(SourceMember member)
            throws IOException {
        if (sourceLocation(member) != SourceLocation.TOMBSTONE
                || !exactSourceBytes(member)
                || member.originalParent().getFileObject(
                        member.name(), member.extension()) != null) {
            throw failure(member.file(),
                    "the exact non-Designer source tombstone was not published");
        }
    }

    private void verifyPublishedPair(
            FlutterDesignerPairLayout.Pair sourcePair,
            TargetMember dart,
            TargetMember model) throws IOException {
        verifyPublished(dart);
        verifyPublished(model);
        FlutterDesignerPairLayout.Pair moved = FlutterDesignerPairLayout
                .findCompletePair(dart.ownedFile, sourcePair.project())
                .orElseThrow(() -> failure(dart.ownedFile,
                        "the published files do not resolve as one safe mirrored pair"));
        if (moved.dartFile() != dart.ownedFile
                || moved.modelFile() != model.ownedFile) {
            throw failure(dart.ownedFile,
                    "the published pair resolved to different physical members");
        }
    }

    private void verifyLockedPublishedTargets(
            TargetMember dart,
            TargetMember model,
            FileObject initiatingMember,
            MoveState state) throws IOException {
        if (state.firstTargetLock == null
                || !state.firstTargetLock.isValid()
                || state.secondTargetLock == null
                || !state.secondTargetLock.isValid()) {
            throw failure(initiatingMember,
                    "both published targets are not held by retained FileLocks");
        }
        Path dartPath = backend.realPath(
                Objects.requireNonNull(dart.ownedFile, "published Dart"));
        Path modelPath = backend.realPath(
                Objects.requireNonNull(model.ownedFile, "published model"));
        if (Files.isSameFile(dartPath, modelPath)) {
            throw failure(initiatingMember,
                    "the published Dart and .fd targets share one physical identity");
        }

        verifyPublished(dart);
        verifyPublished(model);
        if (!backend.realPath(dart.ownedFile).equals(dartPath)
                || !backend.realPath(model.ownedFile).equals(modelPath)) {
            throw failure(initiatingMember,
                    "a published target changed physical identity while locks were retained");
        }
    }

    private static void releaseTargetLocks(MoveState state) {
        release(state.secondTargetLock);
        release(state.firstTargetLock);
        state.secondTargetLock = null;
        state.firstTargetLock = null;
    }

    private void verifyPublished(TargetMember member) throws IOException {
        if (targetLocation(member) != TargetLocation.TARGET) {
            throw failure(member.ownedFile,
                    "the Move target path is unavailable");
        }
        verifyTargetBytes(member,
                "the Move target bytes do not match the source snapshot");
        if (member.parent().getFileObject(
                member.stagingStem(), member.stagingExtension()) != null) {
            throw failure(member.ownedFile,
                    "a private Move staging path remained after publish");
        }
    }

    private void verifyCommitted(
            FlutterDesignerPairLayout.Pair pair,
            SourceMember dart,
            SourceMember model,
            TargetMember targetDart,
            TargetMember targetModel) throws IOException {
        verifyPublishedPair(pair, targetDart, targetModel);
        verifySourceRetired(dart);
        verifySourceRetired(model);
    }

    private void verifySourceRetired(SourceMember member)
            throws IOException {
        SourceLocation location = sourceLocation(member);
        boolean exactTombstone = location == SourceLocation.TOMBSTONE
                && exactSourceBytes(member);
        if ((location != SourceLocation.ABSENT && !exactTombstone)
                || member.originalParent().getFileObject(
                        member.name(), member.extension()) != null) {
            throw failure(member.file(),
                    "the original source path was not retired with exact authority");
        }
    }

    private IOException rollback(
            SourceMember dart,
            SourceMember model,
            TargetMember targetDart,
            TargetMember targetModel,
            Throwable primary,
            PairSaveCoordinator.PairMoveLease lease) {
        List<String> recoveryFailures = new ArrayList<>();
        boolean recreated = restoreSource(model, recoveryFailures);
        recreated |= restoreSource(dart, recoveryFailures);
        verifyRestored(model, recoveryFailures);
        verifyRestored(dart, recoveryFailures);
        // Never discard the last verified copy of either baseline.  If a
        // provider deleted one source and exact reconstruction failed, the
        // already-published target pair is the only recoverable copy and must
        // remain under explicit recovery-conflict authority.
        if (!recreated && restoredExactly(model) && restoredExactly(dart)) {
            removeOwnedTarget(targetModel, recoveryFailures);
            removeOwnedTarget(targetDart, recoveryFailures);
        } else {
            recoveryFailures.add(recreated
                    ? "the exact target pair was retained because rollback "
                    + "had to recreate at least one source FileObject"
                    : "the exact target pair was retained because both source "
                    + "members could not be proven restored");
        }
        if ((recreated || !recoveryFailures.isEmpty()) && lease != null) {
            lease.markRecoveryConflict(
                    recreated
                            ? "a failed Move required replacement source FileObjects; reopen the restored form"
                            : String.join("; ", recoveryFailures));
        }

        IOException failure = new IOException(
                "Move Flutter Designer form failed. Target: "
                + dart.originalPath() + " and " + model.originalPath()
                + ". Reason: " + reason(primary),
                primary);
        if (!recoveryFailures.isEmpty()) {
            failure.addSuppressed(new IOException(
                    "Flutter Designer pair-Move recovery conflict: "
                    + String.join("; ", recoveryFailures)));
        }
        return failure;
    }

    /** @return true when restoration had to create a replacement FileObject. */
    private boolean restoreSource(
            SourceMember member,
            List<String> failures) {
        SourceLocation current = sourceLocation(member);
        if (current == SourceLocation.ORIGINAL) {
            return false;
        }
        if (current == SourceLocation.TOMBSTONE) {
            try {
                backend.renameSourceOwned(
                        member.file(), member.lock, member.originalParent(),
                        member.privateStem(), member.privateExtension(),
                        member.baseline(), member.name(), member.extension());
            } catch (IOException | RuntimeException restoreFailure) {
                if (sourceLocation(member) != SourceLocation.ORIGINAL
                        || !exactSourceBytesQuietly(member)) {
                    failures.add(member.originalPath() + ": "
                            + reason(restoreFailure));
                }
            }
            return false;
        }
        if (current != SourceLocation.ABSENT) {
            failures.add(member.originalPath()
                    + ": refusing to restore a source outside the owned original/tombstone paths");
            return false;
        }

        String restoreStem = member.privateStem() + "-restore";
        TargetMember restore = new TargetMember(
                member.role(), member.originalParent(), member.name(),
                member.extension(), restoreStem, member.baseline());
        try {
            createAndWrite(restore);
            publishForRollbackRestore(restore);
        } catch (IOException | RuntimeException restoreFailure) {
            failures.add(member.originalPath() + ": " + reason(restoreFailure));
            removeOwnedTarget(restore, failures);
        }
        return true;
    }

    private void verifyRestored(
            SourceMember member,
            List<String> failures) {
        try {
            FileObject restored = member.originalParent().getFileObject(
                    member.name(), member.extension());
            if (restored == null || !restored.isValid()
                    || !Arrays.equals(member.baseline(), backend.read(restored))) {
                failures.add(member.originalPath()
                        + ": original path or exact bytes were not restored");
            }
            if (member.originalParent().getFileObject(
                    member.privateStem(), member.privateExtension()) != null) {
                failures.add(member.originalPath()
                        + ": private source tombstone remained after rollback");
            }
        } catch (IOException | RuntimeException verificationFailure) {
            failures.add(member.originalPath() + ": "
                    + reason(verificationFailure));
        }
    }

    private boolean restoredExactly(SourceMember member) {
        try {
            FileObject restored = member.originalParent().getFileObject(
                    member.name(), member.extension());
            return restored != null
                    && restored.isValid()
                    && Arrays.equals(member.baseline(), backend.read(restored));
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private void removeOwnedTarget(
            TargetMember member,
            List<String> failures) {
        if (member.ownedFile == null) {
            if (member.unownedPathObserved) {
                failures.add(member.stagingPath()
                        + ": refusing to delete a staging path whose identity was not returned by create");
            }
            return;
        }
        TargetLocation current = targetLocation(member);
        if (current == TargetLocation.ABSENT) {
            recordReplacementAtTargetPath(member, failures);
            return;
        }
        if (current == TargetLocation.UNKNOWN) {
            failures.add(member.ownedFile.getPath()
                    + ": refusing to delete a file outside the owned Move paths");
            return;
        }
        try {
            if (!exactTargetBytes(member)) {
                failures.add(member.ownedFile.getPath()
                        + ": refusing to delete bytes that differ from the Move snapshot");
                return;
            }
            try {
                backend.deleteOwned(
                        member.ownedFile, member.parent(),
                        current == TargetLocation.STAGING
                                ? member.stagingStem() : member.targetStem(),
                        current == TargetLocation.STAGING
                                ? member.stagingExtension() : member.extension(),
                        member.expectedBytes());
            } catch (IOException | RuntimeException deleteFailure) {
                if (targetLocation(member) != TargetLocation.ABSENT) {
                    failures.add(member.ownedFile.getPath() + ": "
                            + reason(deleteFailure));
                }
            }
            if (targetLocation(member) != TargetLocation.ABSENT) {
                failures.add(member.ownedFile.getPath()
                        + ": the owned Move artifact remained after rollback");
            } else {
                recordReplacementAtTargetPath(member, failures);
            }
        } catch (IOException | RuntimeException verificationFailure) {
            failures.add(member.ownedFile.getPath() + ": "
                    + reason(verificationFailure));
        }
    }

    private static void recordReplacementAtTargetPath(
            TargetMember member,
            List<String> failures) {
        FileObject staging = member.parent().getFileObject(
                member.stagingStem(), member.stagingExtension());
        if (staging != null && staging != member.ownedFile) {
            failures.add(staging.getPath()
                    + ": refusing to delete a replacement at the Move staging path");
        }
        FileObject target = member.parent().getFileObject(
                member.targetStem(), member.extension());
        if (target != null && target != member.ownedFile) {
            failures.add(target.getPath()
                    + ": refusing to delete a replacement at the Move target path");
        }
    }

    private static void ensureTargetVacant(TargetMember member)
            throws IOException {
        if (member.parent().getFileObject(
                member.stagingStem(), member.stagingExtension()) != null
                || member.parent().getFileObject(
                        member.targetStem(), member.extension()) != null) {
            throw failure(member.parent(),
                    "the destination or private Move staging path became occupied");
        }
    }

    private void verifyTargetBytes(TargetMember member, String message)
            throws IOException {
        if (!exactTargetBytes(member)) {
            throw failure(member.ownedFile, message);
        }
    }

    private boolean exactTargetBytes(TargetMember member) throws IOException {
        return member.ownedFile != null
                && member.ownedFile.isValid()
                && Arrays.equals(member.expectedBytes(),
                        backend.read(member.ownedFile));
    }

    private boolean exactSourceBytes(SourceMember member) throws IOException {
        return member.file().isValid()
                && Arrays.equals(member.baseline(), backend.read(member.file()));
    }

    private boolean exactSourceBytesQuietly(SourceMember member) {
        try {
            return exactSourceBytes(member);
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private static SourceLocation sourceLocation(SourceMember member) {
        FileObject file = member.file();
        if (!file.isValid()) {
            FileObject original = member.originalParent().getFileObject(
                    member.name(), member.extension());
            FileObject tombstone = member.originalParent().getFileObject(
                    member.privateStem(), member.privateExtension());
            return original == null && tombstone == null
                    ? SourceLocation.ABSENT : SourceLocation.UNKNOWN;
        }
        if (file.getParent() != member.originalParent()) {
            return SourceLocation.UNKNOWN;
        }
        if (file.getName().equals(member.name())
                && file.getExt().equals(member.extension())) {
            return member.originalParent().getFileObject(
                    member.name(), member.extension()) == file
                    ? SourceLocation.ORIGINAL : SourceLocation.UNKNOWN;
        }
        if (file.getName().equals(member.privateStem())
                && file.getExt().equals(member.privateExtension())) {
            return member.originalParent().getFileObject(
                    member.privateStem(), member.privateExtension()) == file
                    ? SourceLocation.TOMBSTONE : SourceLocation.UNKNOWN;
        }
        return SourceLocation.UNKNOWN;
    }

    private static TargetLocation targetLocation(TargetMember member) {
        if (member.ownedFile == null || !member.ownedFile.isValid()) {
            return TargetLocation.ABSENT;
        }
        if (member.ownedFile.getParent() != member.parent()) {
            return TargetLocation.UNKNOWN;
        }
        if (member.ownedFile.getName().equals(member.stagingStem())
                && member.ownedFile.getExt().equals(
                        member.stagingExtension())) {
            return member.parent().getFileObject(
                    member.stagingStem(), member.stagingExtension())
                    == member.ownedFile
                    ? TargetLocation.STAGING : TargetLocation.UNKNOWN;
        }
        if (member.ownedFile.getName().equals(member.targetStem())
                && member.ownedFile.getExt().equals(member.extension())) {
            return member.parent().getFileObject(
                    member.targetStem(), member.extension()) == member.ownedFile
                    ? TargetLocation.TARGET : TargetLocation.UNKNOWN;
        }
        return TargetLocation.UNKNOWN;
    }

    private void verifyDistinctPhysicalFiles(
            SourceMember dart,
            SourceMember model,
            FileObject initiatingMember) throws IOException {
        if (Files.isSameFile(dart.realPath(), model.realPath())) {
            throw failure(initiatingMember,
                    "Dart and .fd resolve to the same physical file");
        }
    }

    private static void verifyDistinctDestinationFolders(
            SourceMember dart,
            SourceMember model,
            FileObject initiatingMember) throws IOException {
        Path dartTarget = localFile(dart.targetParent()).toPath().toRealPath();
        Path modelTarget = localFile(model.targetParent()).toPath().toRealPath();
        if (Files.isSameFile(dartTarget, modelTarget)) {
            throw failure(initiatingMember,
                    "lib and .fd_templates destinations resolve to one physical folder");
        }
    }

    private static void bindLock(
            SourceMember member,
            List<SourceMember> order,
            FileLock first,
            FileLock second) {
        member.lock = order.get(0) == member ? first : second;
    }

    private static String requireTransactionId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{8,80}")) {
            throw new IllegalStateException(
                    "Pair-Move transaction id is unavailable or unsafe");
        }
        return value;
    }

    private static IOException failure(FileObject target, String reason) {
        return new IOException("Move Flutter Designer form failed. Target: "
                + (target == null ? "unknown Designer pair" : target.getPath())
                + ". Reason: " + reason + ".");
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
    }

    private static String lockKey(Path path) {
        String key = path.toAbsolutePath().normalize().toString();
        return isWindows() ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static void release(FileLock lock) {
        if (lock != null && lock.isValid()) {
            lock.releaseLock();
        }
    }

    record MoveResult(
            boolean committed,
            FileObject dartFile,
            FileObject modelFile) {
        MoveResult {
            if (!committed) {
                throw new IllegalArgumentException(
                        "A returned pair-Move result must be committed");
            }
            Objects.requireNonNull(dartFile, "dartFile");
            Objects.requireNonNull(modelFile, "modelFile");
        }
    }

    @FunctionalInterface
    interface DependencyGuard {
        void verify(
                FlutterDesignerPairLayout.Pair pair,
                String targetRelativeFolder,
                byte[] exactMovedDartBytes) throws IOException;

        default void verifyExclusively(
                FlutterDesignerPairLayout.Pair pair,
                String targetRelativeFolder,
                byte[] exactMovedDartBytes,
                Collection<CallerLock> callerLocks,
                ExclusiveCommit commit) throws IOException {
            Objects.requireNonNull(callerLocks, "callerLocks");
            Objects.requireNonNull(commit, "commit");
            verify(pair, targetRelativeFolder, exactMovedDartBytes);
            commit.run();
        }

        record CallerLock(FileObject file, FileLock lock) {
            public CallerLock {
                Objects.requireNonNull(file, "file");
                Objects.requireNonNull(lock, "lock");
                if (!lock.isValid()) {
                    throw new IllegalArgumentException(
                            "Caller lock must still be valid");
                }
            }
        }

        @FunctionalInterface
        interface ExclusiveCommit {
            void run() throws IOException;
        }
    }

    interface Backend {
        Path realPath(FileObject file) throws IOException;

        FileLock lock(FileObject file) throws IOException;

        byte[] read(FileObject file) throws IOException;

        FileObject create(FileObject parent, String name, String extension)
                throws IOException;

        void writeOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] bytes) throws IOException;

        FileLock publishOwned(
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

        void renameSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException;

        void deleteSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException;

        void runAtomicAction(FileSystem fileSystem, FileSystem.AtomicAction action)
                throws IOException;
    }

    private enum SourceLocation { ORIGINAL, TOMBSTONE, ABSENT, UNKNOWN }
    private enum TargetLocation { STAGING, TARGET, ABSENT, UNKNOWN }

    private static final class MoveState {
        private boolean committed;
        private FileLock firstTargetLock;
        private FileLock secondTargetLock;
        private final List<String> cleanupFailures = new ArrayList<>();
        private final List<String> postCommitFailures = new ArrayList<>();
    }

    private static final class MoveAtomicAction
            implements FileSystem.AtomicAction {
        private FileSystem.AtomicAction delegate;

        MoveAtomicAction(FileSystem.AtomicAction delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() throws IOException {
            FileSystem.AtomicAction current = delegate;
            if (current == null) {
                throw new IOException(
                        "The Flutter Designer pair-Move action has already finished");
            }
            current.run();
        }

        void clear() {
            delegate = null;
        }
    }

    private record TargetFolders(
            FileObject dartFolder,
            FileObject modelFolder,
            String relativeFolder,
            FolderIdentity dartIdentity,
            FolderIdentity modelIdentity) {
        TargetFolders {
            Objects.requireNonNull(dartFolder, "dartFolder");
            Objects.requireNonNull(modelFolder, "modelFolder");
            Objects.requireNonNull(relativeFolder, "relativeFolder");
            Objects.requireNonNull(dartIdentity, "dartIdentity");
            Objects.requireNonNull(modelIdentity, "modelIdentity");
        }
    }

    private record FolderIdentity(
            Path absolutePath,
            Path realPath,
            Object fileKey) {
        FolderIdentity {
            Objects.requireNonNull(absolutePath, "absolutePath");
            Objects.requireNonNull(realPath, "realPath");
        }

        boolean samePhysicalFolder(FolderIdentity current) {
            if (!absolutePath.equals(current.absolutePath)
                    || !realPath.equals(current.realPath)) {
                return false;
            }
            return Objects.equals(fileKey, current.fileKey);
        }
    }

    private record FolderLockPlan(
            FileObject folder,
            FolderIdentity identity) {
        FolderLockPlan {
            Objects.requireNonNull(folder, "folder");
            Objects.requireNonNull(identity, "identity");
        }
    }

    private record LockedFolder(
            FileObject folder,
            FolderIdentity identity,
            FileLock lock) {
        LockedFolder {
            Objects.requireNonNull(folder, "folder");
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(lock, "lock");
        }
    }

    private static final class SourceMember {
        private final String role;
        private final FileObject file;
        private final FileObject originalParent;
        private final FileObject targetParent;
        private final String name;
        private final String extension;
        private final String privateStem;
        private final String privateExtension;
        private final Path realPath;
        private FileLock lock;
        private byte[] baseline;

        SourceMember(
                String role,
                FileObject file,
                FileObject originalParent,
                FileObject targetParent,
                String name,
                String extension,
                String privateStem,
                String privateExtension,
                Path realPath) {
            this.role = role;
            this.file = file;
            this.originalParent = originalParent;
            this.targetParent = targetParent;
            this.name = name;
            this.extension = extension;
            this.privateStem = privateStem;
            this.privateExtension = privateExtension;
            this.realPath = realPath;
        }

        String role() { return role; }
        FileObject file() { return file; }
        FileObject originalParent() { return originalParent; }
        FileObject targetParent() { return targetParent; }
        String name() { return name; }
        String extension() { return extension; }
        String privateStem() { return privateStem; }
        String privateExtension() { return privateExtension; }
        Path realPath() { return realPath; }
        FileLock lock() { return Objects.requireNonNull(lock, "lock"); }
        byte[] baseline() { return Objects.requireNonNull(baseline, "baseline"); }
        String originalPath() {
            return originalParent.getPath() + "/" + name + "." + extension;
        }
        String targetPath() {
            return targetParent.getPath() + "/" + name + "." + extension;
        }
    }

    private static final class TargetMember {
        private final String role;
        private final FileObject parent;
        private final String targetStem;
        private final String extension;
        private final String stagingStem;
        private final String stagingExtension;
        private final byte[] expectedBytes;
        private FileObject ownedFile;
        private boolean unownedPathObserved;

        TargetMember(
                String role,
                FileObject parent,
                String targetStem,
                String extension,
                String stagingStem,
                byte[] expectedBytes) {
            this.role = role;
            this.parent = parent;
            this.targetStem = targetStem;
            this.extension = extension;
            this.stagingStem = stagingStem;
            this.stagingExtension = PRIVATE_EXTENSION;
            this.expectedBytes = Objects.requireNonNull(
                    expectedBytes, "expectedBytes").clone();
        }

        FileObject parent() { return parent; }
        String targetStem() { return targetStem; }
        String extension() { return extension; }
        String stagingStem() { return stagingStem; }
        String stagingExtension() { return stagingExtension; }
        byte[] expectedBytes() { return expectedBytes.clone(); }
        String stagingPath() {
            return parent.getPath() + "/" + stagingStem + "."
                    + stagingExtension;
        }
    }

    private static final class NetBeansBackend implements Backend {
        @Override
        public Path realPath(FileObject file) throws IOException {
            return localFile(file).toPath().toRealPath();
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
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "write");
                if (file.getSize() != 0L || file.asBytes().length != 0) {
                    throw new IOException(
                            "Refusing pair-Move write: the fresh staging artifact is no longer empty");
                }
                try (OutputStream output = file.getOutputStream(lock)) {
                    output.write(bytes);
                }
            }
        }

        @Override
        public FileLock publishOwned(
                FileObject file,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            FileLock lock = file.lock();
            boolean retained = false;
            try {
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "publish");
                requireExactBytes(file, expectedBytes, "publish");
                if (expectedParent.getFileObject(
                        targetName, targetExtension) != null) {
                    throw new IOException(
                            "Refusing pair-Move publish: the target path became occupied");
                }
                file.rename(lock, targetName, targetExtension);
                retained = true;
                return lock;
            } finally {
                if (!retained) {
                    release(lock);
                }
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
                requireOwnedIdentity(file, expectedParent, expectedName,
                        expectedExtension, "delete");
                requireExactBytes(file, expectedBytes, "delete");
                file.delete(lock);
            }
        }

        @Override
        public void renameSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes,
                String targetName,
                String targetExtension) throws IOException {
            requireOwnedIdentity(file, expectedParent, expectedName,
                    expectedExtension, "source rename");
            requireExactBytes(file, expectedBytes, "source rename");
            if (expectedParent.getFileObject(
                    targetName, targetExtension) != null) {
                throw new IOException(
                        "Refusing pair-Move source rename: the target path became occupied");
            }
            file.rename(lock, targetName, targetExtension);
        }

        @Override
        public void deleteSourceOwned(
                FileObject file,
                FileLock lock,
                FileObject expectedParent,
                String expectedName,
                String expectedExtension,
                byte[] expectedBytes) throws IOException {
            requireOwnedIdentity(file, expectedParent, expectedName,
                    expectedExtension, "source delete");
            requireExactBytes(file, expectedBytes, "source delete");
            file.delete(lock);
        }

        private static void requireExactBytes(
                FileObject file,
                byte[] expected,
                String operation) throws IOException {
            if (!Arrays.equals(expected, file.asBytes())) {
                throw new IOException(
                        "Refusing pair-Move " + operation
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
                        "Refusing pair-Move " + operation
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

package io.github.vgrytsenko2022.plugin.designer;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vgrytsenko2022.designer.move.DartMoveDependencyDiagnostic;
import io.github.vgrytsenko2022.designer.move.DartMoveDependencyLimits;
import io.github.vgrytsenko2022.designer.move.DartMoveSourceSnapshot;
import io.github.vgrytsenko2022.designer.move.DesignerPairMoveDependencyPlanner;
import io.github.vgrytsenko2022.designer.move.DesignerPairMoveDependencyResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

/** NetBeans adapter for the bounded Dart dependency proof used by pair Move. */
final class FlutterDesignerMoveDependencyGuard
        implements FlutterDesignerPairMove.DependencyGuard {
    private static final int MAX_PUBSPEC_BYTES = 512 * 1024;
    private static final int MAX_PACKAGE_CONFIG_BYTES = 4 * 1024 * 1024;
    private static final int MAX_EXCLUSIVE_FOLDER_MONITORS = 512;
    private static final String NB30_FOLDER_OBJECT =
            "org.netbeans.modules.masterfs.filebasedfs.fileobjects.FolderObj";
    private static final String NB30_FOLDER_CHILDREN_CACHE =
            NB30_FOLDER_OBJECT + "$FolderChildrenCache";
    private static final String NB30_DATA_LOCK =
            "org.netbeans.modules.masterfs.filebasedfs.fileobjects.LockForFile";
    private static final InventoryBounds DEFAULT_INVENTORY_BOUNDS =
            new InventoryBounds(65_536, 16_384, 256);
    private static final Set<String> EXCLUDED_TOP_LEVEL = Set.of(
            ".dart_tool", ".fd_templates", ".git", ".gradle", ".idea", "build");
    private static final ObjectMapper JSON = new ObjectMapper(
            new JsonFactoryBuilder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                    .build());
    private static final LoadSettings YAML_SETTINGS = LoadSettings.builder()
            .setLabel("pubspec.yaml")
            .setAllowDuplicateKeys(false)
            .setAllowRecursiveKeys(false)
            .setMaxAliasesForCollections(32)
            .setCodePointLimit(MAX_PUBSPEC_BYTES)
            .setUseMarks(true)
            .build();

    private final DesignerPairMoveDependencyPlanner planner;
    private final InventoryBounds inventoryBounds;

    FlutterDesignerMoveDependencyGuard() {
        this(new DesignerPairMoveDependencyPlanner(), DEFAULT_INVENTORY_BOUNDS);
    }

    FlutterDesignerMoveDependencyGuard(
            DesignerPairMoveDependencyPlanner planner) {
        this(planner, DEFAULT_INVENTORY_BOUNDS);
    }

    FlutterDesignerMoveDependencyGuard(
            DesignerPairMoveDependencyPlanner planner,
            InventoryBounds inventoryBounds) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.inventoryBounds = Objects.requireNonNull(
                inventoryBounds, "inventoryBounds");
    }

    @Override
    public void verify(
            FlutterDesignerPairLayout.Pair pair,
            String targetRelativeFolder,
            byte[] exactMovedDartBytes) throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(targetRelativeFolder, "targetRelativeFolder");
        Objects.requireNonNull(exactMovedDartBytes, "exactMovedDartBytes");

        Path projectRoot = requireLocalPath(
                pair.project().getProjectDirectory()).toRealPath();
        rejectModifiedProjectDart(projectRoot);
        String packageName = readPackageName(projectRoot);
        verifyPackageConfig(projectRoot, packageName);
        List<DartMoveSourceSnapshot> sources = inventory(projectRoot);
        rejectModifiedProjectDart(projectRoot);

        String movedProjectPath = normalizeProjectRelative(
                FlutterDesignerPairLayout.DART_ROOT + "/"
                + pair.relativeDartPath());
        DartMoveSourceSnapshot moved = sources.stream()
                .filter(source -> source.projectRelativePath()
                        .equals(movedProjectPath))
                .findFirst()
                .orElseThrow(() -> failure(
                        "the exact moved source is missing from the project inventory: "
                        + movedProjectPath));
        if (!java.util.Arrays.equals(
                exactMovedDartBytes, moved.copyBytes())) {
            throw failure(
                    "the moved Dart bytes changed during dependency inspection");
        }

        String targetLibPath = targetRelativeFolder.isEmpty()
                ? pair.dartFile().getNameExt()
                : targetRelativeFolder + "/" + pair.dartFile().getNameExt();
        DesignerPairMoveDependencyResult result = planner.prepare(
                packageName,
                pair.relativeDartPath(),
                targetLibPath,
                sources);
        if (result instanceof DesignerPairMoveDependencyResult.Rejected rejected) {
            DartMoveDependencyDiagnostic diagnostic = rejected.diagnostic();
            String location = diagnostic.sourceProjectRelativePath().isBlank()
                    ? "project" : diagnostic.sourceProjectRelativePath();
            if (diagnostic.utf16Offset() >= 0) {
                location += ":" + diagnostic.utf16Offset();
            }
            throw failure(diagnostic.code() + " at " + location + ": "
                    + diagnostic.message());
        }
    }

    /**
     * Repeats the dependency proof and executes the filesystem commit while
     * every in-process input to that proof is exclusively admitted.
     *
     * <p>The caller may already own locks required by the surrounding pair
     * transaction. Each explicit FileObject/FileLock pair is identity-checked
     * and its token must remain valid, but it is not locked a second time.
     * All other proof files and all relevant project directories are locked
     * in deterministic order.
     * On NetBeans 30 the adapter then recursively holds both each folder's
     * Java monitor and its actual MasterFS child-cache write mutex. The same
     * admission includes the physical ancestor chain above the project, so a
     * rename of the project root is excluded as well. The exact inventory is
     * revalidated on the event thread before the final proof and commit
     * callback run without an editor-event gap. If the exact NetBeans 30
     * MasterFS mutex shape is unavailable, Move fails closed.</p>
     */
    @Override
    public void verifyExclusively(
            FlutterDesignerPairLayout.Pair pair,
            String targetRelativeFolder,
            byte[] exactMovedDartBytes,
            Collection<FlutterDesignerPairMove.DependencyGuard.CallerLock>
                    callerLockedFiles,
            FlutterDesignerPairMove.DependencyGuard.ExclusiveCommit commit)
            throws IOException {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(targetRelativeFolder, "targetRelativeFolder");
        Objects.requireNonNull(exactMovedDartBytes, "exactMovedDartBytes");
        Objects.requireNonNull(callerLockedFiles, "callerLockedFiles");
        Objects.requireNonNull(commit, "commit");

        // Cheap fail-fast proof before acquiring a project-wide lock set. The
        // same proof is repeated below after all admissions are held.
        verify(pair, targetRelativeFolder, exactMovedDartBytes);

        Path projectRoot = requireLocalPath(
                pair.project().getProjectDirectory()).toRealPath();
        CallerLocks callerLocks = captureCallerLocks(
                projectRoot, callerLockedFiles);
        ProofLayout expected = captureProofLayout(projectRoot);
        List<ProofIdentity> boundaryFolders = captureBoundaryFolders(
                expected.directories().stream()
                        .filter(identity -> identity.relativePath().isEmpty())
                        .findFirst()
                        .orElseThrow(() -> failure(
                                "the project root is missing from the proof directory set")));
        List<ProofIdentity> required = mergeRequiredIdentities(
                expected, boundaryFolders);
        List<ProofIdentity> monitoredFolders = mergeMonitoredFolders(
                expected.directories(),
                boundaryFolders,
                callerLocks.identities());
        if (monitoredFolders.size() > MAX_EXCLUSIVE_FOLDER_MONITORS) {
            throw failure("the exclusive Move proof requires "
                    + monitoredFolders.size() + " folder monitors, exceeding "
                    + MAX_EXCLUSIVE_FOLDER_MONITORS);
        }

        List<HeldLock> acquired = new ArrayList<>();
        try {
            acquireRequiredLocks(required, callerLocks, acquired);
            runOnEventThread(() -> withFolderAdmissions(
                    captureFolderAdmissions(monitoredFolders), 0, () -> {
                        verifyHeldAdmission(
                                projectRoot,
                                expected,
                                callerLocks,
                                acquired);
                        verify(pair, targetRelativeFolder,
                                exactMovedDartBytes);
                        // Keep this as the final normal-editor state check.
                        // Mutex.EVENT prevents an editor event from appearing
                        // between this check and the caller's commit.
                        rejectModifiedProjectDart(projectRoot);
                        verifyHeldAdmission(
                                projectRoot,
                                expected,
                                callerLocks,
                                acquired);
                        commit.run();
                    }));
        } finally {
            releaseReverse(acquired);
        }
    }

    private List<DartMoveSourceSnapshot> inventory(Path projectRoot)
            throws IOException {
        DartMoveDependencyLimits limits = planner.limits();
        List<Path> candidates = boundedInventoryLayout(projectRoot).candidates();

        List<DartMoveSourceSnapshot> sources = new ArrayList<>();
        Map<Object, Path> fileKeys = new HashMap<>();
        Set<Path> realFiles = new HashSet<>();
        long total = 0L;
        for (Path path : candidates) {
            BasicFileAttributes attributes = Files.readAttributes(
                    path,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()) {
                throw failure("the Dart inventory contains a symbolic link: "
                        + display(projectRoot, path));
            }
            if (attributes.isDirectory()) {
                Path realDirectory = path.toRealPath();
                if (!realDirectory.startsWith(projectRoot)) {
                    throw failure("a project directory escapes the physical root: "
                            + display(projectRoot, path));
                }
                continue;
            }
            if (path.getFileName().toString().equals("pubspec.yaml")
                    && !path.equals(projectRoot.resolve("pubspec.yaml"))) {
                throw failure("nested pubspec.yaml packages are not supported by "
                        + "Designer Move: " + display(projectRoot, path));
            }
            if (!path.getFileName().toString().endsWith(".dart")) {
                continue;
            }
            if (!attributes.isRegularFile()) {
                throw failure("a Dart inventory entry is not a regular file: "
                        + display(projectRoot, path));
            }
            Path real = path.toRealPath();
            if (!real.startsWith(projectRoot)) {
                throw failure("a Dart source escapes the physical project root: "
                        + display(projectRoot, path));
            }
            Object fileKey = attributes.fileKey();
            if (fileKey != null) {
                Path previous = fileKeys.putIfAbsent(fileKey, path);
                if (previous != null) {
                    throw failure("hard-linked Dart sources are ambiguous: "
                            + display(projectRoot, previous) + " and "
                            + display(projectRoot, path));
                }
            } else if (!realFiles.add(real)) {
                throw failure("duplicate physical Dart source identity: "
                        + display(projectRoot, path));
            }
            if (attributes.size() > limits.maxSourceBytes()) {
                throw failure("Dart source exceeds the per-file Move limit: "
                        + display(projectRoot, path));
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > limits.maxSourceBytes()) {
                throw failure("Dart source grew beyond the per-file Move limit: "
                        + display(projectRoot, path));
            }
            total = Math.addExact(total, bytes.length);
            if (total > limits.maxTotalSourceBytes()) {
                throw failure("project Dart sources exceed the total Move scan limit");
            }
            if (sources.size() >= limits.maxSourceFiles()) {
                throw failure("project Dart source count exceeds the Move scan limit");
            }
            sources.add(new DartMoveSourceSnapshot(
                    display(projectRoot, path), bytes));
        }
        return List.copyOf(sources);
    }

    private InventoryLayout boundedInventoryLayout(Path projectRoot)
            throws IOException {
        List<Path> candidates = new ArrayList<>();
        List<Path> projectDirectories = new ArrayList<>();
        projectDirectories.add(projectRoot);
        long[] entries = {0L};
        long[] directories = {0L};
        Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path directory,
                    BasicFileAttributes attributes) throws IOException {
                if (!directory.equals(projectRoot)
                        && excluded(projectRoot, directory)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (directory.equals(projectRoot)) {
                    return FileVisitResult.CONTINUE;
                }
                int depth = projectRoot.relativize(directory).getNameCount();
                if (depth > inventoryBounds.maxDepth()) {
                    throw failure("Dart inventory directory depth exceeds "
                            + inventoryBounds.maxDepth() + ": "
                            + display(projectRoot, directory));
                }
                countEntry(entries, directory, projectRoot);
                directories[0]++;
                if (directories[0] > inventoryBounds.maxDirectories()) {
                    throw failure("Dart inventory directory count exceeds "
                            + inventoryBounds.maxDirectories() + " at "
                            + display(projectRoot, directory));
                }
                Path realDirectory = directory.toRealPath();
                if (!realDirectory.startsWith(projectRoot)) {
                    throw failure("a project directory escapes the physical root: "
                            + display(projectRoot, directory));
                }
                projectDirectories.add(directory);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attributes) throws IOException {
                if (excluded(projectRoot, file)) {
                    return FileVisitResult.CONTINUE;
                }
                countEntry(entries, file, projectRoot);
                candidates.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(
                    Path file,
                    IOException failure) throws IOException {
                throw FlutterDesignerMoveDependencyGuard.failure(
                        "Dart inventory could not inspect "
                        + display(projectRoot, file) + ": " + reason(failure));
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path directory,
                    IOException failure) throws IOException {
                if (failure != null) {
                    throw FlutterDesignerMoveDependencyGuard.failure(
                            "Dart inventory could not finish "
                            + display(projectRoot, directory) + ": "
                            + reason(failure));
                }
                return FileVisitResult.CONTINUE;
            }

            private void countEntry(
                    long[] counter,
                    Path path,
                    Path root) throws IOException {
                counter[0]++;
                if (counter[0] > inventoryBounds.maxVisitedEntries()) {
                    throw failure("Dart inventory entry count exceeds "
                            + inventoryBounds.maxVisitedEntries() + " at "
                            + display(root, path));
                }
            }
        });
        candidates.sort(Comparator.comparing(
                path -> normalizeProjectRelative(
                        projectRoot.relativize(path).toString())));
        projectDirectories.sort(Comparator.comparing(
                path -> normalizeProjectRelative(
                        projectRoot.relativize(path).toString())));
        return new InventoryLayout(
                List.copyOf(candidates), List.copyOf(projectDirectories));
    }

    private ProofLayout captureProofLayout(Path projectRoot)
            throws IOException {
        InventoryLayout inventory = boundedInventoryLayout(projectRoot);
        TreeMap<String, ProofIdentity> files = new TreeMap<>();
        for (Path candidate : inventory.candidates()) {
            if (candidate.getFileName().toString().endsWith(".dart")) {
                putIdentity(files, captureIdentity(
                        projectRoot, candidate, false, "Dart proof file"));
            }
        }
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        putIdentity(files, captureIdentity(
                projectRoot, pubspec, false, "pubspec.yaml proof file"));
        Path packageConfig = projectRoot.resolve(
                ".dart_tool/package_config.json");
        putIdentity(files, captureIdentity(
                projectRoot,
                packageConfig,
                false,
                "package_config.json proof file"));

        TreeMap<String, ProofIdentity> directories = new TreeMap<>();
        for (Path directory : inventory.directories()) {
            putIdentity(directories, captureIdentity(
                    projectRoot, directory, true, "Dart proof directory"));
        }
        putIdentity(directories, captureIdentity(
                projectRoot,
                packageConfig.getParent(),
                true,
                "package_config.json parent directory"));
        return new ProofLayout(
                List.copyOf(files.values()),
                List.copyOf(directories.values()));
    }

    private static CallerLocks captureCallerLocks(
            Path projectRoot,
            Collection<FlutterDesignerPairMove.DependencyGuard.CallerLock>
                    callerLockedFiles) throws IOException {
        TreeMap<String, ProofIdentity> identities = new TreeMap<>();
        Map<String, FileLock> locks = new TreeMap<>();
        Set<FileObject> visited = java.util.Collections.newSetFromMap(
                new IdentityHashMap<>());
        for (FlutterDesignerPairMove.DependencyGuard.CallerLock callerLock
                : callerLockedFiles) {
            if (callerLock == null) {
                throw failure("the caller lock set contains a null entry");
            }
            FileObject file = callerLock.file();
            FileLock lock = callerLock.lock();
            if (!visited.add(file)) {
                throw failure("the caller lock set contains a duplicate FileObject: "
                        + file.getPath());
            }
            if (!file.isValid() || !lock.isValid()) {
                throw failure("caller-locked file is invalid or no longer locked: "
                        + file.getPath());
            }
            Path path = requireLocalPath(file).toAbsolutePath().normalize();
            if (!path.startsWith(projectRoot)) {
                throw failure("caller-locked file is outside the Flutter project: "
                        + path);
            }
            ProofIdentity identity = captureIdentity(
                    projectRoot,
                    path,
                    file.isFolder(),
                    "caller-locked file");
            if (identity.fileObject() != file) {
                throw failure("caller-locked file identity changed before admission: "
                        + identity.relativePath());
            }
            verifyLockOwnership(identity, lock, "caller-owned");
            putIdentity(identities, identity);
            locks.put(identity.lockKey(), lock);
        }
        return new CallerLocks(
                Map.copyOf(identities), Map.copyOf(locks));
    }

    private static ProofIdentity captureIdentity(
            Path projectRoot,
            Path path,
            boolean directory,
            String label) throws IOException {
        return captureIdentity(
                projectRoot, path, directory, label, true, null);
    }

    private static ProofIdentity captureIdentity(
            Path projectRoot,
            Path path,
            boolean directory,
            String label,
            boolean requireWithinProject,
            FileObject expectedFileObject) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (requireWithinProject && !absolute.startsWith(projectRoot)) {
            throw failure(label + " escapes the physical project root: "
                    + absolute);
        }
        final BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(
                    absolute,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        } catch (IOException unavailable) {
            throw failure(label + " is unavailable: "
                    + displayIdentity(projectRoot, absolute) + ": "
                    + reason(unavailable));
        }
        if (attributes.isSymbolicLink()
                || directory != attributes.isDirectory()
                || (!directory && !attributes.isRegularFile())) {
            throw failure(label + " has no safe physical identity: "
                    + displayIdentity(projectRoot, absolute));
        }
        Path real = absolute.toRealPath();
        if (requireWithinProject && !real.startsWith(projectRoot)) {
            throw failure(label + " escapes the physical project root: "
                    + displayIdentity(projectRoot, absolute));
        }
        FileObject fileObject = FileUtil.toFileObject(
                FileUtil.normalizeFile(absolute.toFile()));
        if (fileObject == null
                || !fileObject.isValid()
                || fileObject.isFolder() != directory) {
            throw failure(label + " has no stable local NetBeans identity: "
                    + displayIdentity(projectRoot, absolute));
        }
        if (expectedFileObject != null && fileObject != expectedFileObject) {
            throw failure(label + " changed its exact NetBeans FileObject identity: "
                    + displayIdentity(projectRoot, absolute));
        }
        Path netBeansPath = requireLocalPath(fileObject)
                .toAbsolutePath().normalize();
        if (!Files.isSameFile(absolute, netBeansPath)) {
            throw failure(label + " NetBeans and physical identities disagree: "
                    + displayIdentity(projectRoot, absolute));
        }
        return new ProofIdentity(
                projectRoot,
                displayIdentity(projectRoot, absolute),
                absolute.toString(),
                absolute,
                real,
                attributes.fileKey(),
                directory,
                requireWithinProject,
                fileObject,
                fileObject.getPath());
    }

    private static List<ProofIdentity> captureBoundaryFolders(
            ProofIdentity projectRoot) throws IOException {
        List<ProofIdentity> boundary = new ArrayList<>();
        FileObject ancestor = projectRoot.fileObject().getParent();
        while (ancestor != null) {
            if (!ancestor.isFolder()) {
                throw failure("a project-root ancestor is not a folder: "
                        + ancestor.getPath());
            }
            final File local;
            try {
                local = FileUtil.toFile(ancestor);
            } catch (AssertionError invalidMasterFile) {
                throw failure("a project-root ancestor has no safe local identity: "
                        + ancestor.getPath());
            }
            if (local == null) {
                if (ancestor.isRoot()) {
                    break;
                }
                throw failure("a project-root ancestor is not local: "
                        + ancestor.getPath());
            }
            Path path = local.toPath().toAbsolutePath().normalize();
            boundary.add(captureIdentity(
                    projectRoot.projectRoot(),
                    path,
                    true,
                    "project-root boundary folder",
                    false,
                    ancestor));
            ancestor = ancestor.getParent();
        }
        boundary.sort(Comparator.comparing(ProofIdentity::lockKey));
        return List.copyOf(boundary);
    }

    private static void putIdentity(
            Map<String, ProofIdentity> identities,
            ProofIdentity identity) throws IOException {
        ProofIdentity previous = identities.putIfAbsent(
                identity.lockKey(), identity);
        if (previous != null && previous.fileObject() != identity.fileObject()) {
            throw failure("ambiguous physical proof identity: "
                    + previous.relativePath() + " and "
                    + identity.relativePath());
        }
    }

    private static List<ProofIdentity> mergeRequiredIdentities(
            ProofLayout layout,
            List<ProofIdentity> boundaryFolders) throws IOException {
        TreeMap<String, ProofIdentity> required = new TreeMap<>();
        for (ProofIdentity boundary : boundaryFolders) {
            putIdentity(required, boundary);
        }
        for (ProofIdentity directory : layout.directories()) {
            putIdentity(required, directory);
        }
        for (ProofIdentity file : layout.files()) {
            putIdentity(required, file);
        }
        return List.copyOf(required.values());
    }

    private static List<ProofIdentity> mergeMonitoredFolders(
            List<ProofIdentity> requiredDirectories,
            List<ProofIdentity> boundaryFolders,
            Map<String, ProofIdentity> callerLocks) throws IOException {
        TreeMap<String, ProofIdentity> folders = new TreeMap<>();
        for (ProofIdentity boundary : boundaryFolders) {
            putIdentity(folders, boundary);
        }
        for (ProofIdentity directory : requiredDirectories) {
            putIdentity(folders, directory);
        }
        for (ProofIdentity callerLock : callerLocks.values()) {
            if (callerLock.directory()) {
                putIdentity(folders, callerLock);
            }
        }
        return List.copyOf(folders.values());
    }

    private static void acquireRequiredLocks(
            List<ProofIdentity> required,
            CallerLocks callerLocks,
            List<HeldLock> acquired) throws IOException {
        verifyCallerLocks(callerLocks);
        for (ProofIdentity identity : required) {
            ProofIdentity callerLock = callerLocks.identities().get(
                    identity.lockKey());
            if (callerLock != null) {
                requireSameIdentity(identity, callerLock,
                        "caller lock does not own the required proof identity");
                continue;
            }
            final FileLock lock;
            try {
                lock = identity.fileObject().lock();
            } catch (IOException unavailable) {
                throw failure("could not lock proof input "
                        + identity.relativePath() + ": "
                        + reason(unavailable));
            }
            if (lock == null || !lock.isValid()) {
                if (lock != null) {
                    lock.releaseLock();
                }
                throw failure("proof input lock is invalid: "
                        + identity.relativePath());
            }
            HeldLock held = new HeldLock(identity, lock);
            acquired.add(held);
            verifyHeldLock(held);
        }
    }

    private void verifyHeldAdmission(
            Path projectRoot,
            ProofLayout expected,
            CallerLocks callerLocks,
            List<HeldLock> acquired) throws IOException {
        verifyCallerLocks(callerLocks);
        for (HeldLock held : acquired) {
            verifyHeldLock(held);
        }
        ProofLayout current = captureProofLayout(projectRoot);
        requireSameIdentitySet(
                expected.files(), current.files(), "proof file set");
        requireSameIdentitySet(
                expected.directories(),
                current.directories(),
                "proof directory set");
    }

    private static void verifyCallerLocks(CallerLocks callerLocks)
            throws IOException {
        for (ProofIdentity identity : callerLocks.identities().values()) {
            FileLock lock = callerLocks.locks().get(identity.lockKey());
            if (lock == null || !lock.isValid()) {
                throw failure("caller-owned lock was released during admission: "
                        + identity.relativePath());
            }
            verifyLockOwnership(identity, lock, "caller-owned");
            verifyIdentity(identity);
        }
    }

    private static void verifyHeldLock(HeldLock held) throws IOException {
        if (!held.lock().isValid()) {
            throw failure("proof input lock was released during admission: "
                    + held.identity().relativePath());
        }
        verifyLockOwnership(
                held.identity(), held.lock(), "acquired proof-input");
        verifyIdentity(held.identity());
    }

    private static void verifyLockOwnership(
            ProofIdentity identity,
            FileLock lock,
            String label) throws IOException {
        if (identity.directory()) {
            // NetBeans 30 MasterFS FolderObj returns an unbound FileLock.
            // Folder exclusion is supplied by the child-cache mutex below.
            return;
        }
        if (!lock.getClass().getName().equals(NB30_DATA_LOCK)) {
            throw failure(label + " data-file lock is not the exact NetBeans 30 "
                    + "MasterFS lock type: " + identity.relativePath());
        }
        Object owner = invokeNoArg(
                lock,
                "getFile",
                "NetBeans 30 MasterFS data-file lock owner",
                identity.relativePath());
        if (!(owner instanceof File ownerFile)) {
            throw failure(label + " data-file lock has no physical owner: "
                    + identity.relativePath());
        }
        Path ownerPath = ownerFile.toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(ownerPath, LinkOption.NOFOLLOW_LINKS)
                || !Files.isSameFile(identity.absolute(), ownerPath)) {
            throw failure(label + " lock belongs to a different file: "
                    + identity.relativePath());
        }
    }

    private static void verifyIdentity(ProofIdentity expected)
            throws IOException {
        ProofIdentity current = captureIdentity(
                expected.projectRoot(),
                expected.absolute(),
                expected.directory(),
                "proof identity",
                expected.withinProject(),
                expected.fileObject());
        requireSameIdentity(expected, current,
                "proof input identity changed");
    }

    private static void requireSameIdentitySet(
            List<ProofIdentity> expected,
            List<ProofIdentity> current,
            String label) throws IOException {
        if (expected.size() != current.size()) {
            throw failure(label + " changed during exclusive admission");
        }
        for (int index = 0; index < expected.size(); index++) {
            requireSameIdentity(
                    expected.get(index),
                    current.get(index),
                    label + " changed during exclusive admission");
        }
    }

    private static void requireSameIdentity(
            ProofIdentity expected,
            ProofIdentity current,
            String reason) throws IOException {
        if (!expected.relativePath().equals(current.relativePath())
                || !expected.projectRoot().equals(current.projectRoot())
                || !expected.absoluteSpelling().equals(
                        current.absoluteSpelling())
                || !expected.real().equals(current.real())
                || !Objects.equals(expected.fileKey(), current.fileKey())
                || expected.directory() != current.directory()
                || expected.withinProject() != current.withinProject()
                || expected.fileObject() != current.fileObject()
                || !expected.fileObjectPath().equals(
                        current.fileObjectPath())
                || !expected.fileObject().isValid()) {
            throw failure(reason + ": " + expected.relativePath());
        }
    }

    private static List<FolderAdmission> captureFolderAdmissions(
            List<ProofIdentity> folders) throws IOException {
        List<FolderAdmission> admissions = new ArrayList<>(folders.size());
        for (ProofIdentity folder : folders) {
            FileObject fileObject = folder.fileObject();
            if (!fileObject.isFolder() || !fileObject.isValid()) {
                throw failure("folder admission identity is invalid: "
                        + folder.relativePath());
            }
            if (!fileObject.getClass().getName().equals(NB30_FOLDER_OBJECT)) {
                throw failure("folder admission is not backed by the exact "
                        + "NetBeans 30 MasterFS FolderObj: "
                        + folder.relativePath());
            }
            Object childrenCache = invokeNoArg(
                    fileObject,
                    "getChildrenCache",
                    "MasterFS folder children cache",
                    folder.relativePath());
            if (!childrenCache.getClass().getName().equals(
                    NB30_FOLDER_CHILDREN_CACHE)) {
                throw failure("folder admission has an unexpected NetBeans 30 "
                        + "MasterFS children-cache type: "
                        + folder.relativePath());
            }
            Object privileged = invokeNoArg(
                    childrenCache,
                    "getMutexPrivileged",
                    "MasterFS folder child mutex",
                    folder.relativePath());
            if (!(privileged instanceof Mutex.Privileged mutex)) {
                throw failure("NetBeans 30 MasterFS child mutex is unavailable for: "
                        + folder.relativePath());
            }
            admissions.add(new FolderAdmission(
                    folder, childrenCache, mutex));
        }
        return List.copyOf(admissions);
    }

    private static Object invokeNoArg(
            Object receiver,
            String methodName,
            String label,
            String path) throws IOException {
        final Method method;
        try {
            method = receiver.getClass().getMethod(methodName);
        } catch (NoSuchMethodException unavailable) {
            throw failure(label + " API is unavailable for " + path);
        }
        try {
            if (!method.canAccess(receiver) && !method.trySetAccessible()) {
                throw failure(label + " API is inaccessible for " + path);
            }
            Object value = method.invoke(receiver);
            if (value == null) {
                throw failure(label + " returned no identity for " + path);
            }
            return value;
        } catch (IllegalAccessException inaccessible) {
            throw failure(label + " API is inaccessible for " + path + ": "
                    + reason(inaccessible));
        } catch (InvocationTargetException failed) {
            Throwable cause = failed.getCause();
            throw failure(label + " failed for " + path + ": "
                    + reason(cause == null ? failed : cause));
        } catch (SecurityException denied) {
            throw failure(label + " API access was denied for " + path + ": "
                    + reason(denied));
        } catch (RuntimeException inaccessible) {
            throw failure(label + " API could not be used for " + path + ": "
                    + reason(inaccessible));
        }
    }

    private static void withFolderAdmissions(
            List<FolderAdmission> folders,
            int index,
            FlutterDesignerPairMove.DependencyGuard.ExclusiveCommit action)
            throws IOException {
        if (index == folders.size()) {
            action.run();
            return;
        }
        FolderAdmission admission = folders.get(index);
        FileObject folder = admission.identity().fileObject();
        synchronized (folder) {
            FolderAdmission current = captureFolderAdmissions(
                    List.of(admission.identity())).get(0);
            if (current.childrenCache() != admission.childrenCache()
                    || current.mutex() != admission.mutex()) {
                throw failure("MasterFS folder child-mutex identity changed: "
                        + admission.identity().relativePath());
            }
            if (!admission.mutex().tryWriteAccess(0L)) {
                throw failure("MasterFS folder child mutex is busy: "
                        + admission.identity().relativePath());
            }
            try {
                withFolderAdmissions(folders, index + 1, action);
            } finally {
                admission.mutex().exitWriteAccess();
            }
        }
    }

    private static void runOnEventThread(
            FlutterDesignerPairMove.DependencyGuard.ExclusiveCommit action)
            throws IOException {
        try {
            Mutex.EVENT.writeAccess((Mutex.ExceptionAction<Void>) () -> {
                action.run();
                return null;
            });
        } catch (MutexException wrapped) {
            Exception cause = wrapped.getException();
            if (cause instanceof IOException ioFailure) {
                throw ioFailure;
            }
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw new IOException(
                    "Move Flutter Designer form exclusive callback failed",
                    cause);
        }
    }

    private static void releaseReverse(List<HeldLock> acquired) {
        for (int index = acquired.size() - 1; index >= 0; index--) {
            FileLock lock = acquired.get(index).lock();
            // releaseLock is idempotent. Always call it even if a concurrent
            // post-admission rename has already invalidated the handle; merely
            // dropping such a LockForFile still triggers NetBeans' leak alarm.
            lock.releaseLock();
        }
    }

    private static String physicalLockKey(Path path) {
        String value = Normalizer.normalize(
                path.toAbsolutePath().normalize().toString(),
                Normalizer.Form.NFC);
        return isWindows() ? value.toLowerCase(Locale.ROOT) : value;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static boolean excluded(Path projectRoot, Path path) {
        Path relative = projectRoot.relativize(path);
        return relative.getNameCount() > 0
                && EXCLUDED_TOP_LEVEL.contains(relative.getName(0).toString());
    }

    private static void rejectModifiedProjectDart(Path projectRoot)
            throws IOException {
        for (DataObject modified : DataObject.getRegistry().getModifiedSet()) {
            FileObject primary = modified.getPrimaryFile();
            if (!primary.hasExt("dart")) {
                continue;
            }
            File local;
            try {
                local = FileUtil.toFile(primary);
            } catch (AssertionError invalidMasterFile) {
                throw failure("a modified Dart editor has no safe physical identity");
            }
            if (local == null) {
                continue;
            }
            Path absolute = local.toPath().toAbsolutePath().normalize();
            Path identity;
            try {
                identity = absolute.toRealPath();
            } catch (IOException unavailableIdentity) {
                identity = absolute;
            }
            if (absolute.startsWith(projectRoot)
                    || identity.startsWith(projectRoot)) {
                throw failure("Dart source has unsaved editor changes: "
                        + display(projectRoot, identity));
            }
        }
    }

    private static String readPackageName(Path projectRoot)
            throws IOException {
        Path pubspec = projectRoot.resolve("pubspec.yaml");
        byte[] bytes = readBounded(pubspec, MAX_PUBSPEC_BYTES, "pubspec.yaml");
        String source = decodeStrictUtf8(bytes, "pubspec.yaml");
        final Node root;
        try {
            root = new Compose(YAML_SETTINGS).composeString(source).orElse(null);
        } catch (RuntimeException malformed) {
            throw failure("pubspec.yaml could not be parsed exactly: "
                    + reason(malformed));
        }
        if (!(root instanceof MappingNode mapping)) {
            throw failure("pubspec.yaml root is not a mapping");
        }
        String name = null;
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode key
                    && key.getValue().equals("name")) {
                if (!(tuple.getValueNode() instanceof ScalarNode value)) {
                    throw failure("pubspec package name is not a scalar");
                }
                name = value.getValue();
                break;
            }
        }
        if (name == null || !name.matches("[a-z][a-z0-9_]*")) {
            throw failure("pubspec package name is missing or non-canonical");
        }
        return name;
    }

    private static void verifyPackageConfig(
            Path projectRoot,
            String packageName) throws IOException {
        Path config = projectRoot.resolve(".dart_tool/package_config.json");
        byte[] bytes = readBounded(
                config, MAX_PACKAGE_CONFIG_BYTES, ".dart_tool/package_config.json");
        final JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException | RuntimeException malformed) {
            throw failure("package_config.json could not be parsed exactly: "
                    + reason(malformed));
        }
        if (root == null || !root.isObject()) {
            throw failure("package_config.json root is not an object");
        }
        JsonNode configVersion = root.get("configVersion");
        if (configVersion == null
                || !configVersion.isIntegralNumber()
                || !configVersion.canConvertToInt()
                || configVersion.intValue() != 2) {
            throw failure("package_config.json configVersion is not exactly 2");
        }
        JsonNode packages = root.get("packages");
        if (packages == null || !packages.isArray()) {
            throw failure("package_config.json has no packages array");
        }
        if (!Files.isDirectory(projectRoot.resolve("lib"), LinkOption.NOFOLLOW_LINKS)) {
            throw failure("the Flutter project lib root is missing or not a safe directory");
        }
        Path projectLib = projectRoot.resolve("lib").toRealPath();
        if (!projectLib.startsWith(projectRoot)) {
            throw failure("the Flutter project lib root escapes the physical project");
        }

        Set<String> packageNames = new HashSet<>();
        boolean foundSelf = false;
        int index = 0;
        for (JsonNode candidate : packages) {
            if (!candidate.isObject()) {
                throw failure("package_config.json package entry " + index
                        + " is not an object");
            }
            String name = requiredPackageText(candidate, "name", index);
            if (!name.matches("[a-z][a-z0-9_]*")) {
                throw failure("package_config.json package entry " + index
                        + " has a non-canonical name: " + name);
            }
            if (!packageNames.add(name)) {
                throw failure("package_config.json contains duplicate package entry: "
                        + name);
            }
            String rootUri = requiredPackageText(candidate, "rootUri", index);
            Path configuredRoot = resolveConfigRoot(
                    config.getParent(), rootUri, name);
            JsonNode packageUriNode = candidate.get("packageUri");
            String packageUri = null;
            if (packageUriNode != null && !packageUriNode.isNull()) {
                if (!packageUriNode.isTextual()
                        || packageUriNode.textValue().isBlank()) {
                    throw failure("package_config.json package entry '" + name
                            + "' has a missing or non-text packageUri");
                }
                packageUri = packageUriNode.textValue();
            }

            if (name.equals(packageName)) {
                foundSelf = true;
                String languageVersion = requiredPackageText(
                        candidate, "languageVersion", index);
                if (!languageVersion.matches("(?:2|3)\\.(?:0|[1-9][0-9]*)")) {
                    throw failure("package_config self package languageVersion '"
                            + languageVersion
                            + "' is not a canonical supported Dart 2.x or 3.x version");
                }
                if (packageUri == null
                        || !(packageUri.equals("lib/") || packageUri.equals("lib"))
                        || !Files.isSameFile(projectRoot, configuredRoot)) {
                    throw failure("pubspec and package_config self-package roots disagree");
                }
                Path configuredLib = resolvePackageLibraryRoot(
                        configuredRoot, packageUri, name);
                if (!Files.isSameFile(projectLib, configuredLib)) {
                    throw failure("pubspec and package_config self-package lib roots disagree");
                }
            } else {
                // Dart resolves an omitted packageUri directly at rootUri.
                // It is therefore still a real alias candidate, not an entry
                // that can be skipped by the ownership proof.
                Path packageLib = packageUri == null
                        ? configuredRoot.toRealPath()
                        : resolvePackageLibraryRoot(
                                configuredRoot, packageUri, name);
                if (Files.isSameFile(projectLib, packageLib)) {
                    throw failure("package_config entry '" + name
                            + "' aliases the project lib root owned by '"
                            + packageName + "'");
                }
            }
            index++;
        }
        if (!foundSelf) {
            throw failure("package_config.json does not identify the pubspec package");
        }
    }

    private static String requiredPackageText(
            JsonNode candidate,
            String field,
            int index) throws IOException {
        JsonNode value = candidate.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw failure("package_config.json package entry " + index
                    + " has a missing or non-text " + field);
        }
        return value.textValue();
    }

    private static Path resolvePackageLibraryRoot(
            Path configuredRoot,
            String packageUri,
            String packageName) throws IOException {
        if (packageUri.indexOf('\\') >= 0 || packageUri.indexOf('%') >= 0) {
            throw failure("package_config entry '" + packageName
                    + "' has an ambiguous packageUri");
        }
        final URI uri;
        try {
            uri = new URI(packageUri);
        } catch (URISyntaxException malformed) {
            throw failure("package_config entry '" + packageName
                    + "' has a malformed packageUri");
        }
        if (uri.isAbsolute()
                || uri.getRawAuthority() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getPath() == null
                || uri.getPath().isBlank()) {
            throw failure("package_config entry '" + packageName
                    + "' packageUri is not a safe relative library path");
        }
        Path root = configuredRoot.toRealPath();
        Path lexicalLibrary = configuredRoot.resolve(uri.getPath()).normalize();
        if (!lexicalLibrary.startsWith(configuredRoot.normalize())
                || !Files.isDirectory(
                        lexicalLibrary, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("package_config entry '" + packageName
                    + "' package library root is missing or escapes its package root");
        }
        Path realLibrary = lexicalLibrary.toRealPath();
        if (!realLibrary.startsWith(root)) {
            throw failure("package_config entry '" + packageName
                    + "' package library root escapes through a link");
        }
        return realLibrary;
    }

    private static Path resolveConfigRoot(
            Path configFolder,
            String value,
            String packageName) throws IOException {
        if (value.isBlank() || value.indexOf('\\') >= 0) {
            throw failure("package_config entry '" + packageName
                    + "' rootUri is missing or ambiguous");
        }
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException malformed) {
            throw failure("package_config entry '" + packageName
                    + "' rootUri is malformed");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw failure("package_config entry '" + packageName
                    + "' rootUri contains a query or fragment");
        }
        final Path resolved;
        if (uri.isAbsolute()) {
            if (!uri.getScheme().equalsIgnoreCase("file")) {
                throw failure("package_config entry '" + packageName
                        + "' rootUri is not a file URI");
            }
            resolved = Path.of(uri);
        } else {
            resolved = configFolder.resolve(uri.getPath()).normalize();
        }
        if (!Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("package_config entry '" + packageName
                    + "' rootUri is not a physical directory");
        }
        return resolved.toRealPath();
    }

    private static byte[] readBounded(Path path, int maximum, String label)
            throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) {
            throw failure(label + " is missing or not a safe regular file");
        }
        long size = Files.size(path);
        if (size > maximum) {
            throw failure(label + " exceeds the bounded Move safety limit");
        }
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length > maximum) {
            throw failure(label + " grew beyond the bounded Move safety limit");
        }
        return bytes;
    }

    private static String decodeStrictUtf8(byte[] bytes, String label)
            throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException invalid) {
            throw failure(label + " is not strict UTF-8");
        }
    }

    private static Path requireLocalPath(FileObject file) throws IOException {
        final File local;
        try {
            local = FileUtil.toFile(file);
        } catch (AssertionError invalidMasterFile) {
            throw failure("the Flutter project has no normalized local path");
        }
        if (local == null) {
            throw failure("the Flutter project is not on a local filesystem");
        }
        return local.toPath();
    }

    private static String display(Path root, Path path) {
        return normalizeProjectRelative(root.relativize(path).toString());
    }

    private static String displayIdentity(Path root, Path path) {
        return path.startsWith(root) ? display(root, path)
                : path.toAbsolutePath().normalize().toString();
    }

    private static String normalizeProjectRelative(String path) {
        return path.replace('\\', '/');
    }

    private record InventoryLayout(
            List<Path> candidates,
            List<Path> directories) {
        InventoryLayout {
            candidates = List.copyOf(candidates);
            directories = List.copyOf(directories);
        }
    }

    private record ProofLayout(
            List<ProofIdentity> files,
            List<ProofIdentity> directories) {
        ProofLayout {
            files = List.copyOf(files);
            directories = List.copyOf(directories);
        }
    }

    private record ProofIdentity(
            Path projectRoot,
            String relativePath,
            String absoluteSpelling,
            Path absolute,
            Path real,
            Object fileKey,
            boolean directory,
            boolean withinProject,
            FileObject fileObject,
            String fileObjectPath) {
        ProofIdentity {
            Objects.requireNonNull(projectRoot, "projectRoot");
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(absoluteSpelling, "absoluteSpelling");
            Objects.requireNonNull(absolute, "absolute");
            Objects.requireNonNull(real, "real");
            Objects.requireNonNull(fileObject, "fileObject");
            Objects.requireNonNull(fileObjectPath, "fileObjectPath");
        }

        String lockKey() {
            return physicalLockKey(absolute);
        }
    }

    private record CallerLocks(
            Map<String, ProofIdentity> identities,
            Map<String, FileLock> locks) {
        CallerLocks {
            identities = Map.copyOf(identities);
            locks = Map.copyOf(locks);
        }
    }

    private record HeldLock(ProofIdentity identity, FileLock lock) {
        HeldLock {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(lock, "lock");
        }
    }

    private record FolderAdmission(
            ProofIdentity identity,
            Object childrenCache,
            Mutex.Privileged mutex) {
        FolderAdmission {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(childrenCache, "childrenCache");
            Objects.requireNonNull(mutex, "mutex");
        }
    }

    record InventoryBounds(
            int maxVisitedEntries,
            int maxDirectories,
            int maxDepth) {
        InventoryBounds {
            if (maxVisitedEntries <= 0
                    || maxDirectories <= 0
                    || maxDepth <= 0) {
                throw new IllegalArgumentException(
                        "Inventory bounds must be greater than zero");
            }
        }
    }

    private static IOException failure(String reason) {
        return new IOException(
                "Move Flutter Designer form dependency check failed. Reason: "
                + reason + ".");
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
    }
}

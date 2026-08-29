package dev.flutter.netbeans.project.theme;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates the default project-wide theme descriptor, generated Dart API and
 * fresh Flutter counter-template wiring as one verified in-process transaction.
 */
public final class FlutterProjectThemeProvisioner {
    public static final int MAX_GENERATED_MAIN_BYTES = 2 * 1024 * 1024;
    private static final String EMPTY_SHA256 = "0".repeat(64);
    private static final Logger LOGGER = Logger.getLogger(
            FlutterProjectThemeProvisioner.class.getName());

    private final FlutterProjectThemeCodec codec;
    private final FlutterProjectThemeStore store;
    private final FlutterGeneratedMainThemeTransformer transformer;
    private final TransactionObserver observer;
    private final MoveOperation mover;

    public FlutterProjectThemeProvisioner() {
        this(new FlutterProjectThemeCodec(), new FlutterGeneratedMainThemeTransformer(),
                (step, target) -> { }, FlutterProjectThemeProvisioner::movePlatform);
    }

    FlutterProjectThemeProvisioner(
            FlutterProjectThemeCodec codec,
            FlutterGeneratedMainThemeTransformer transformer,
            TransactionObserver observer) {
        this(codec, transformer, observer, FlutterProjectThemeProvisioner::movePlatform);
    }

    FlutterProjectThemeProvisioner(
            FlutterProjectThemeCodec codec,
            FlutterGeneratedMainThemeTransformer transformer,
            TransactionObserver observer,
            MoveOperation mover) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.store = new FlutterProjectThemeStore(codec);
        this.transformer = Objects.requireNonNull(transformer, "transformer");
        this.observer = Objects.requireNonNull(observer, "observer");
        this.mover = Objects.requireNonNull(mover, "mover");
    }

    /** Provisions a fresh {@code flutter create} result and rejects every collision. */
    public FlutterProjectTheme provisionNewProject(Path projectRoot) throws IOException {
        return provision(projectRoot, false);
    }

    /**
     * Returns an already valid theme or safely provisions a missing descriptor.
     * Invalid, stale or colliding artifacts are never overwritten.
     */
    public FlutterProjectTheme createDefaultIfMissing(Path projectRoot) throws IOException {
        FlutterProjectThemeLoadResult current = store.load(projectRoot);
        if (current.status() == FlutterProjectThemeLoadStatus.VALID) {
            return current.theme().orElseThrow();
        }
        if (current.status() != FlutterProjectThemeLoadStatus.MISSING) {
            throw new IOException("Default project theme cannot be created for "
                    + projectRoot.toAbsolutePath().normalize() + ": " + current.detail());
        }
        return provision(projectRoot, true);
    }

    private FlutterProjectTheme provision(Path projectRoot, boolean onboarding)
            throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path root = projectRoot.toAbsolutePath().normalize();
        requireSafeDirectory(root, "Flutter project root");
        Path main = safeResolve(root, Path.of("lib", "main.dart"));
        requireSafeRegularFile(root, Path.of("lib", "main.dart"),
                "generated Flutter entry point");

        Path descriptor = safeResolve(root, FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = safeResolve(root, FlutterProjectThemePaths.GENERATED_DART_PATH);
        requireAbsent(descriptor, "project theme descriptor");
        requireAbsent(dart, "generated project theme Dart file");
        requireSafeOptionalDirectory(root, Path.of(".fd_templates"),
                "project theme descriptor directory");
        requireSafeOptionalDirectory(root, Path.of("lib", "theme"),
                "generated project theme directory");

        byte[] originalMain = readBounded(main, MAX_GENERATED_MAIN_BYTES,
                "generated Flutter entry point");
        String source = decodeUtf8(originalMain, main);
        byte[] themedMain = transformer.transform(source).getBytes(StandardCharsets.UTF_8);

        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme(EMPTY_SHA256);
        byte[] generatedDart = FlutterProjectThemeDartGenerator.generate(provisional);
        String generatedHash = FlutterProjectThemeDigests.sha256(generatedDart);
        FlutterProjectTheme theme = FlutterProjectTheme.defaultTheme(generatedHash);
        byte[] descriptorBytes = codec.encode(theme);

        boolean createdDescriptorDirectory = false;
        boolean createdDartDirectory = false;
        Path descriptorDirectory = descriptor.getParent();
        Path dartDirectory = dart.getParent();
        String transaction = UUID.randomUUID().toString();
        Path stagedDart = dartDirectory.resolve(".app_theme.dart." + transaction + ".tmp");
        Path stagedMain = main.getParent().resolve(".main.dart." + transaction + ".tmp");
        Path stagedDescriptor = descriptorDirectory.resolve(
                ".project.fdtheme." + transaction + ".tmp");
        Path mainBackup = main.getParent().resolve(".main.dart." + transaction + ".bak");
        try {
            if (!Files.exists(descriptorDirectory, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(descriptorDirectory);
                createdDescriptorDirectory = true;
            }
            if (!Files.exists(dartDirectory, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(dartDirectory);
                createdDartDirectory = true;
            }
            requireSafeDirectory(descriptorDirectory, "project theme descriptor directory");
            requireSafeDirectory(dartDirectory, "generated project theme directory");

            writeNew(stagedDart, generatedDart);
            writeNew(stagedMain, themedMain);
            writeNew(stagedDescriptor, descriptorBytes);
            Files.copy(main, mainBackup, StandardCopyOption.COPY_ATTRIBUTES);
            verifyBytes(mainBackup, originalMain, "entry-point backup");

            observer.beforePublish(TransactionStep.GENERATED_DART, dart);
            requireAbsent(dart, "generated project theme Dart file");
            mover.move(stagedDart, dart, false);
            verifyBytes(dart, generatedDart, "generated project theme Dart file");

            observer.beforePublish(TransactionStep.MAIN_DART, main);
            verifyBytes(main, originalMain,
                    "generated Flutter entry point immediately before theme wiring");
            mover.move(stagedMain, main, true);
            verifyBytes(main, themedMain, "themed Flutter entry point");

            observer.beforePublish(TransactionStep.DESCRIPTOR, descriptor);
            verifyBytes(main, themedMain,
                    "themed Flutter entry point immediately before descriptor publication");
            verifyBytes(dart, generatedDart,
                    "generated project theme Dart immediately before descriptor publication");
            requireAbsent(descriptor, "project theme descriptor");
            mover.move(stagedDescriptor, descriptor, false);
            verifyBytes(descriptor, descriptorBytes, "project theme descriptor");

            FlutterProjectThemeLoadResult loaded = store.load(root);
            if (!loaded.valid() || !loaded.theme().orElseThrow().equals(theme)) {
                throw new IOException("Provisioned project theme did not pass exact descriptor "
                        + "and generated-artifact verification: " + loaded.detail());
            }
            preserveOrRemoveCommittedTemporary(stagedDart, generatedDart);
            preserveOrRemoveCommittedTemporary(stagedMain, themedMain);
            preserveOrRemoveCommittedTemporary(stagedDescriptor, descriptorBytes);
            try {
                observer.afterCommitBeforeCleanup(mainBackup);
                deleteIfExact(mainBackup, originalMain,
                        "committed entry-point backup");
            } catch (IOException | RuntimeException cleanupFailure) {
                LOGGER.log(Level.WARNING,
                        "Project theme commit is valid, but its main.dart backup could not be "
                        + "removed: " + mainBackup,
                        cleanupFailure);
            }
            return theme;
        } catch (IOException | RuntimeException failure) {
            IOException rollbackFailure = rollback(
                    main, originalMain, themedMain, mainBackup,
                    dart, generatedDart,
                    descriptor, descriptorBytes,
                    stagedDart, stagedMain, stagedDescriptor,
                    descriptorDirectory, createdDescriptorDirectory,
                    dartDirectory, createdDartDirectory,
                    mover);
            String mode = onboarding ? "existing Flutter project" : "new Flutter application";
            IOException result = new IOException("Default light and dark project themes could not "
                    + "be provisioned for " + mode + " at " + root + ": "
                    + compact(failure.getMessage())
                    + (rollbackFailure == null
                            ? " The exact pre-provisioning files were restored."
                            : " Exact rollback could not be proven; inspect the preserved project."),
                    failure);
            if (rollbackFailure != null) {
                result.addSuppressed(rollbackFailure);
            }
            throw result;
        }
    }

    private static IOException rollback(
            Path main,
            byte[] originalMain,
            byte[] themedMain,
            Path mainBackup,
            Path dart,
            byte[] generatedDart,
            Path descriptor,
            byte[] descriptorBytes,
            Path stagedDart,
            Path stagedMain,
            Path stagedDescriptor,
            Path descriptorDirectory,
            boolean createdDescriptorDirectory,
            Path dartDirectory,
            boolean createdDartDirectory,
            MoveOperation mover) {
        List<Throwable> failures = new ArrayList<>();
        boolean mainRestored = exactBytes(main, originalMain);
        if (!mainRestored && exactBytes(main, themedMain)) {
            if (!exactBytes(mainBackup, originalMain)) {
                failures.add(new IOException(
                        "Exact entry-point backup is unavailable or changed: " + mainBackup));
            } else {
                IOException moveFailure = null;
                try {
                    mover.move(mainBackup, main, true);
                } catch (IOException ex) {
                    moveFailure = ex;
                }
                mainRestored = exactBytes(main, originalMain);
                if (!mainRestored) {
                    IOException failure = new IOException(
                            "Restored Flutter entry point does not contain the exact original "
                            + "bytes: " + main);
                    if (moveFailure != null) {
                        failure.addSuppressed(moveFailure);
                    }
                    failures.add(failure);
                }
            }
        } else if (!mainRestored) {
            failures.add(new IOException(
                    "Flutter entry point contains foreign, missing or unsafe bytes and "
                    + "was preserved: " + main));
        }
        if (!mainRestored) {
            failures.add(new IOException(
                    "Published project theme artifacts and the exact main.dart backup were "
                    + "retained because restoring the entry point was not proven"));
            return combinedRollbackFailure(failures);
        }
        restoreInitiallyAbsent(
                descriptor, descriptorBytes, "project theme descriptor", failures);
        restoreInitiallyAbsent(
                dart, generatedDart, "generated project theme Dart file", failures);
        restoreInitiallyAbsent(
                stagedDart, generatedDart, "staged generated project theme Dart", failures);
        restoreInitiallyAbsent(
                stagedMain, themedMain, "staged themed Flutter entry point", failures);
        restoreInitiallyAbsent(
                stagedDescriptor, descriptorBytes, "staged project theme descriptor", failures);
        restoreInitiallyAbsent(
                mainBackup, originalMain, "entry-point backup", failures);
        deleteOwnedEmptyDirectory(dartDirectory, createdDartDirectory, failures);
        deleteOwnedEmptyDirectory(descriptorDirectory, createdDescriptorDirectory, failures);
        if (failures.isEmpty()) {
            return null;
        }
        return combinedRollbackFailure(failures);
    }

    private static IOException combinedRollbackFailure(List<Throwable> failures) {
        IOException combined = new IOException("Project theme rollback was incomplete");
        failures.forEach(combined::addSuppressed);
        return combined;
    }

    private static void restoreInitiallyAbsent(
            Path path, byte[] expected, String label, List<Throwable> failures) {
        if (isAbsent(path)) {
            return;
        }
        if (!exactBytes(path, expected)) {
            failures.add(new IOException(label
                    + " contains foreign or unsafe bytes and was preserved: " + path));
            return;
        }
        try {
            Files.delete(path);
            if (!isAbsent(path)) {
                throw new IOException(label + " still exists after rollback deletion: " + path);
            }
        } catch (IOException ex) {
            if (!isAbsent(path)) {
                failures.add(ex);
            }
        }
    }

    private static void deleteOwnedEmptyDirectory(
            Path directory, boolean owned, List<Throwable> failures) {
        if (!owned) {
            return;
        }
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ex) {
            failures.add(ex);
        }
    }

    private static void writeNew(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
        verifyBytes(path, bytes, "staged project theme file");
    }

    static void movePlatform(
            Path source, Path target, boolean replaceExisting) throws IOException {
        if (!replaceExisting) {
            // Files.move specifies FileAlreadyExistsException for this form. Do not
            // request ATOMIC_MOVE here: when that option is present, the API leaves
            // replacement of an concurrently-created target implementation-specific.
            Files.move(source, target);
            return;
        }
        try {
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] readBounded(Path path, int maximum, String label) throws IOException {
        long size = Files.size(path);
        if (size <= 0 || size > maximum) {
            throw new IOException(label + " must contain 1 to " + maximum
                    + " bytes: " + path);
        }
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length <= 0 || bytes.length > maximum) {
            throw new IOException(label + " changed to an unsupported size while reading: "
                    + path);
        }
        return bytes;
    }

    private static String decodeUtf8(byte[] bytes, Path path) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new IOException("Generated Flutter entry point is not valid UTF-8: "
                    + path, ex);
        }
    }

    private static void verifyBytes(Path path, byte[] expected, String label) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) {
            throw new IOException(label + " is missing or unsafe: " + path);
        }
        byte[] actual = Files.readAllBytes(path);
        if (!java.util.Arrays.equals(actual, expected)) {
            throw new IOException(label + " does not contain the exact expected bytes: " + path);
        }
    }

    private static boolean exactBytes(Path path, byte[] expected) {
        try {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(path)
                    || Files.size(path) != expected.length) {
                return false;
            }
            return java.util.Arrays.equals(Files.readAllBytes(path), expected);
        } catch (IOException | RuntimeException failure) {
            return false;
        }
    }

    private static boolean isAbsent(Path path) {
        return !Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path);
    }

    private static Path safeResolve(Path root, Path relative) throws IOException {
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root)) {
            throw new IOException("Project theme path escapes project root " + root
                    + ": " + relative);
        }
        return result;
    }

    private static void requireSafeRegularFile(
            Path root, Path relative, String label) throws IOException {
        Path current = root;
        for (Path element : relative) {
            current = current.resolve(element);
            if (Files.isSymbolicLink(current)) {
                throw new IOException(label + " path contains a symbolic link: " + current);
            }
        }
        if (!Files.isRegularFile(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a regular file: " + current);
        }
    }

    private static void requireSafeOptionalDirectory(
            Path root, Path relative, String label) throws IOException {
        Path current = root;
        for (Path element : relative) {
            current = current.resolve(element);
            if (Files.isSymbolicLink(current)) {
                throw new IOException(label + " path contains a symbolic link: " + current);
            }
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(label + " is occupied by a non-directory path: " + current);
            }
        }
    }

    private static void requireSafeDirectory(Path directory, String label) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(directory)) {
            throw new IOException(label + " is not a real directory: " + directory);
        }
    }

    private static void requireAbsent(Path path, String label) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IOException(label + " already exists and will not be overwritten: " + path);
        }
    }

    private static void deleteIfExact(Path path, byte[] expected, String label)
            throws IOException {
        if (isAbsent(path)) {
            return;
        }
        if (!exactBytes(path, expected)) {
            throw new IOException(label
                    + " contains foreign or unsafe bytes and was preserved: " + path);
        }
        Files.delete(path);
        if (!isAbsent(path)) {
            throw new IOException(label + " still exists after deletion: " + path);
        }
    }

    private static void preserveOrRemoveCommittedTemporary(
            Path path, byte[] expected) {
        try {
            deleteIfExact(path, expected, "Project theme transaction temporary");
        } catch (IOException | RuntimeException failure) {
            LOGGER.log(Level.WARNING,
                    "Project theme commit is valid, but a transaction temporary contains "
                    + "foreign or unavailable bytes and was preserved: " + path,
                    failure);
        }
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "no cause was reported";
        }
        String value = message.trim().replaceAll("\\s+", " ");
        return value.length() <= 600 ? value : value.substring(0, 600) + "…";
    }

    enum TransactionStep {
        GENERATED_DART,
        MAIN_DART,
        DESCRIPTOR
    }

    @FunctionalInterface
    interface TransactionObserver {
        void beforePublish(TransactionStep step, Path target) throws IOException;

        default void afterCommitBeforeCleanup(Path mainBackup) throws IOException {
        }
    }

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target, boolean replaceExisting) throws IOException;
    }
}

package dev.flutter.netbeans.plugin.theme;

import dev.flutter.netbeans.project.theme.FlutterGeneratedThemeArtifact;
import dev.flutter.netbeans.project.theme.FlutterProjectTheme;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeCodec;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDartGenerator;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeDigests;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadResult;
import dev.flutter.netbeans.project.theme.FlutterProjectThemePaths;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;

/** Exact-baseline, fail-closed persistence for descriptor and generated Dart. */
final class FlutterThemePersistence {
    private static final int PROJECT_LOCK_STRIPE_COUNT = 64;
    private static final Object[] PROJECT_LOCKS = createProjectLocks();

    private final ThemeLoader loader;
    private final FlutterProjectThemeCodec codec;
    private final PairWriter writer;

    FlutterThemePersistence() {
        this(loader(new FlutterProjectThemeStore()), new FlutterProjectThemeCodec(),
                FlutterThemePersistence::writePair);
    }

    FlutterThemePersistence(
            FlutterProjectThemeStore store,
            FlutterProjectThemeCodec codec,
            PairWriter writer) {
        this(loader(store), codec, writer);
    }

    FlutterThemePersistence(
            ThemeLoader loader,
            FlutterProjectThemeCodec codec,
            PairWriter writer) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    Snapshot load(Path projectRoot) throws IOException {
        Path root = normalizeRoot(projectRoot);
        FlutterProjectThemeLoadResult loaded = loader.load(root);
        if (!loaded.valid()) {
            throw failure("Open Flutter theme editor", root, loaded.detail(), null);
        }
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        byte[] descriptorBytes = readExactRegularFile(
                descriptor, FlutterProjectThemeCodec.MAX_DESCRIPTOR_BYTES,
                "theme descriptor");
        byte[] dartBytes = readExactRegularFile(
                dart, FlutterProjectThemeStore.MAX_GENERATED_DART_BYTES,
                "generated theme Dart file");
        return new Snapshot(
                root,
                loaded.theme().orElseThrow(),
                descriptorBytes,
                dartBytes);
    }

    Snapshot save(Snapshot baseline, FlutterProjectTheme edited) throws IOException {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(edited, "edited");
        Path root = normalizeRoot(baseline.projectRoot());
        if (!root.equals(baseline.projectRoot())) {
            throw failure("Save Flutter project themes", root,
                    "the editor baseline belongs to a different project root", null);
        }
        Object lock = projectLock(root);
        synchronized (lock) {
            verifyBaseline(baseline);

            byte[] dartBytes = FlutterProjectThemeDartGenerator.generate(edited);
            String dartSha256 = FlutterProjectThemeDigests.sha256(dartBytes);
            FlutterProjectTheme committed = new FlutterProjectTheme(
                    edited.enabled(),
                    edited.defaultMode(),
                    edited.lightThemeId(),
                    edited.darkThemeId(),
                    edited.themes(),
                    new FlutterGeneratedThemeArtifact(
                            FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                            dartSha256));
            byte[] descriptorBytes = codec.encode(committed);

            Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
            Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
            try {
                writer.write(
                        descriptor,
                        dart,
                        baseline.descriptorBytes(),
                        baseline.dartBytes(),
                        descriptorBytes,
                        dartBytes);
            } catch (IOException failure) {
                throw failure("Save Flutter project themes", descriptor,
                        compact(failure.getMessage()), failure);
            }

            FlutterProjectThemeLoadResult verified;
            try {
                verified = loader.load(root);
            } catch (RuntimeException verificationFailure) {
                throw rollbackUnverifiedPublication(
                        descriptor,
                        dart,
                        baseline.descriptorBytes(),
                        baseline.dartBytes(),
                        descriptorBytes,
                        dartBytes,
                        "the post-publication loader failed: "
                        + compact(verificationFailure.getMessage()),
                        verificationFailure);
            }
            boolean verifiedRevision = verified.valid()
                    && verified.theme().filter(committed::equals).isPresent();
            if (!verifiedRevision) {
                String reason = verified.valid()
                        ? "the post-publication descriptor model differs from the "
                        + "revision prepared by this editor"
                        : verified.detail();
                throw rollbackUnverifiedPublication(
                        descriptor,
                        dart,
                        baseline.descriptorBytes(),
                        baseline.dartBytes(),
                        descriptorBytes,
                        dartBytes,
                        reason,
                        null);
            }
            return new Snapshot(root, committed, descriptorBytes, dartBytes);
        }
    }

    private void verifyBaseline(Snapshot baseline) throws IOException {
        FlutterProjectThemeLoadResult current = loader.load(baseline.projectRoot());
        if (!current.valid()) {
            throw failure("Save Flutter project themes", baseline.projectRoot(),
                    "the generated-hash conflict guard rejected the current files: "
                    + current.detail(), null);
        }
        Path descriptor = baseline.projectRoot().resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = baseline.projectRoot().resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH);
        byte[] currentDescriptor = readExactRegularFile(
                descriptor, FlutterProjectThemeCodec.MAX_DESCRIPTOR_BYTES,
                "theme descriptor");
        byte[] currentDart = readExactRegularFile(
                dart, FlutterProjectThemeStore.MAX_GENERATED_DART_BYTES,
                "generated theme Dart file");
        if (!Arrays.equals(currentDescriptor, baseline.descriptorBytes())) {
            throw failure("Save Flutter project themes", descriptor,
                    "the descriptor changed after this editor was opened; "
                    + "close and reopen the editor before saving", null);
        }
        if (!Arrays.equals(currentDart, baseline.dartBytes())) {
            throw failure("Save Flutter project themes", dart,
                    "the generated Dart file changed after this editor was opened; "
                    + "no user changes were overwritten", null);
        }
    }

    static void writePair(
            Path descriptor,
            Path dart,
            byte[] oldDescriptor,
            byte[] oldDart,
            byte[] newDescriptor,
            byte[] newDart) throws IOException {
        writePair(
                descriptor, dart, oldDescriptor, oldDart,
                newDescriptor, newDart,
                FlutterThemePersistence::moveReplacing);
    }

    static void writePair(
            Path descriptor,
            Path dart,
            byte[] oldDescriptor,
            byte[] oldDart,
            byte[] newDescriptor,
            byte[] newDart,
            MoveOperation mover) throws IOException {
        Objects.requireNonNull(mover, "mover");
        Path descriptorTemp = null;
        Path dartTemp = null;
        IOException publicationFailure = null;
        try {
            verifyExactPair(
                    descriptor, oldDescriptor, dart, oldDart,
                    "before project theme publication");
            descriptorTemp = stage(descriptor, newDescriptor);
            dartTemp = stage(dart, newDart);
            verifyExactPair(
                    descriptor, oldDescriptor, dart, oldDart,
                    "after staging project theme publication");
            mover.move(dartTemp, dart);
            dartTemp = null;
            verifyExactPair(
                    descriptor, oldDescriptor, dart, newDart,
                    "between generated Dart and descriptor publication");
            mover.move(descriptorTemp, descriptor);
            descriptorTemp = null;
            verifyExactPair(
                    descriptor, newDescriptor, dart, newDart,
                    "after project theme publication");
        } catch (IOException | RuntimeException thrown) {
            IOException primary = asIOException(
                    "project theme pair publication failed", thrown);
            IOException rollbackFailure = null;
            try {
                restoreIfOwned(
                        descriptor, oldDescriptor, newDescriptor, mover,
                        "theme descriptor");
            } catch (IOException failure) {
                rollbackFailure = failure;
            }
            try {
                restoreIfOwned(
                        dart, oldDart, newDart, mover,
                        "generated theme Dart file");
            } catch (IOException failure) {
                if (rollbackFailure == null) {
                    rollbackFailure = failure;
                } else {
                    rollbackFailure.addSuppressed(failure);
                }
            }
            if (rollbackFailure != null) {
                primary.addSuppressed(rollbackFailure);
                publicationFailure = new IOException(
                        "project theme pair publication failed and exact rollback "
                        + "also failed: " + compact(primary.getMessage())
                        + "; rollback: " + compact(rollbackFailure.getMessage()),
                        primary);
            } else {
                publicationFailure = primary;
            }
        }

        IOException cleanupFailure = cleanupTemporaries(
                descriptorTemp, newDescriptor,
                dartTemp, newDart);
        if (publicationFailure != null) {
            if (cleanupFailure != null) {
                publicationFailure.addSuppressed(cleanupFailure);
            }
            throw publicationFailure;
        }
        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    private static void restoreIfOwned(
            Path target,
            byte[] oldBytes,
            byte[] newBytes,
            MoveOperation mover,
            String role) throws IOException {
        RevisionState current = revisionState(target, oldBytes, newBytes);
        if (isBaseline(current)) {
            return;
        }
        if (!isPublished(current)) {
            throw new IOException(role + " contains neither the exact baseline nor "
                    + "the staged revision during rollback: " + target);
        }
        Path rollback = stage(target, oldBytes);
        Throwable moverFailure = null;
        try {
            mover.move(rollback, target);
        } catch (IOException | RuntimeException failure) {
            moverFailure = failure;
        }

        RevisionState afterRollback;
        try {
            afterRollback = revisionState(target, oldBytes, newBytes);
        } catch (IOException | RuntimeException inspectionFailure) {
            IOException failure = asIOException(
                    role + " rollback target inspection failed", inspectionFailure);
            if (moverFailure != null) {
                failure.addSuppressed(moverFailure);
            }
            throw preservedRecoveryFailure(failure, rollback, role);
        }
        if (!isBaseline(afterRollback)) {
            IOException failure = moverFailure == null
                    ? new IOException(role + " exact-byte rollback did not publish "
                            + "the baseline; target state is " + afterRollback)
                    : asIOException(role + " exact-byte rollback failed", moverFailure);
            throw preservedRecoveryFailure(failure, rollback, role);
        }

        IOException cleanupFailure = cleanupTemporary(rollback, oldBytes, role);
        if (cleanupFailure != null) {
            throw new IOException(
                    role + " reached the exact baseline but rollback temporary "
                    + "cleanup was not proven safe",
                    cleanupFailure);
        }
    }

    private static IOException preservedRecoveryFailure(
            IOException failure,
            Path recovery,
            String role) {
        String suffix = recovery != null && Files.exists(
                recovery, LinkOption.NOFOLLOW_LINKS)
                ? "; the unverified recovery temporary was preserved at " + recovery
                : "; no exact recovery temporary remains at " + recovery;
        return new IOException(role + " rollback could not be proven: "
                + compact(failure.getMessage()) + suffix, failure);
    }

    private static IOException rollbackUnverifiedPublication(
            Path descriptor,
            Path dart,
            byte[] baselineDescriptor,
            byte[] baselineDart,
            byte[] publishedDescriptor,
            byte[] publishedDart,
            String verificationReason,
            Throwable verificationCause) {
        IOException primary = failure(
                "Verify saved Flutter project themes",
                descriptor,
                verificationReason,
                verificationCause);
        try {
            RevisionState descriptorState = revisionState(
                    descriptor, baselineDescriptor, publishedDescriptor);
            RevisionState dartState = revisionState(
                    dart, baselineDart, publishedDart);
            if (isBaseline(descriptorState) && isBaseline(dartState)) {
                return failure(
                        "Verify saved Flutter project themes",
                        descriptor,
                        verificationReason + "; the exact editor baseline is still "
                        + "present, so no rollback write was needed",
                        primary);
            }
            if (!isPublished(descriptorState) || !isPublished(dartState)) {
                IOException refusal = new IOException(
                        "exact rollback was refused because the current pair is not "
                        + "fully owned by this save attempt (descriptor="
                        + descriptorState + ", generated Dart=" + dartState
                        + "); no foreign bytes were overwritten");
                primary.addSuppressed(refusal);
                return failure(
                        "Verify saved Flutter project themes",
                        descriptor,
                        verificationReason + "; " + refusal.getMessage(),
                        primary);
            }

            writePair(
                    descriptor,
                    dart,
                    publishedDescriptor,
                    publishedDart,
                    baselineDescriptor,
                    baselineDart);
            if (!isBaseline(revisionState(
                    descriptor, baselineDescriptor, publishedDescriptor))
                    || !isBaseline(revisionState(
                            dart, baselineDart, publishedDart))) {
                throw new IOException(
                        "exact rollback publication completed but baseline-byte "
                        + "verification failed");
            }
            return failure(
                    "Verify saved Flutter project themes",
                    descriptor,
                    verificationReason + "; the exact published revision was "
                    + "rolled back to the editor baseline",
                    primary);
        } catch (IOException | RuntimeException thrown) {
            IOException rollbackFailure = asIOException(
                    "post-publication exact rollback failed", thrown);
            primary.addSuppressed(rollbackFailure);
            return failure(
                    "Verify saved Flutter project themes",
                    descriptor,
                    verificationReason + "; exact rollback could not be completed: "
                    + compact(rollbackFailure.getMessage()),
                    primary);
        }
    }

    private static RevisionState revisionState(
            Path target,
            byte[] baseline,
            byte[] published) throws IOException {
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(target)) {
            return RevisionState.FOREIGN;
        }
        long size = Files.size(target);
        if (size != baseline.length && size != published.length) {
            return RevisionState.FOREIGN;
        }
        byte[] current = Files.readAllBytes(target);
        if (Arrays.equals(baseline, published)) {
            return Arrays.equals(current, baseline)
                    ? RevisionState.UNCHANGED
                    : RevisionState.FOREIGN;
        }
        if (Arrays.equals(current, baseline)) {
            return RevisionState.BASELINE;
        }
        return Arrays.equals(current, published)
                ? RevisionState.PUBLISHED
                : RevisionState.FOREIGN;
    }

    private static boolean isBaseline(RevisionState state) {
        return state == RevisionState.BASELINE
                || state == RevisionState.UNCHANGED;
    }

    private static boolean isPublished(RevisionState state) {
        return state == RevisionState.PUBLISHED
                || state == RevisionState.UNCHANGED;
    }

    private static IOException cleanupTemporaries(
            Path first,
            byte[] firstExpected,
            Path second,
            byte[] secondExpected) {
        IOException firstFailure = cleanupTemporary(
                first, firstExpected, "theme descriptor");
        IOException secondFailure = cleanupTemporary(
                second, secondExpected, "generated theme Dart file");
        if (firstFailure == null) {
            return secondFailure;
        }
        if (secondFailure != null) {
            firstFailure.addSuppressed(secondFailure);
        }
        return firstFailure;
    }

    private static IOException cleanupTemporary(
            Path temporary,
            byte[] expected,
            String role) {
        if (temporary == null
                || !Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        try {
            if (!Files.isRegularFile(temporary, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(temporary)
                    || Files.size(temporary) != expected.length
                    || !Arrays.equals(Files.readAllBytes(temporary), expected)) {
                return new IOException("refused to delete an unverified or foreign "
                        + role + " temporary: " + temporary);
            }
            Files.deleteIfExists(temporary);
            return null;
        } catch (IOException | RuntimeException thrown) {
            return asIOException(
                    "temporary " + role + " cleanup failed for " + temporary,
                    thrown);
        }
    }

    private static void verifyExactPair(
            Path descriptor,
            byte[] expectedDescriptor,
            Path dart,
            byte[] expectedDart,
            String phase) throws IOException {
        if (!isBaseline(revisionState(
                descriptor, expectedDescriptor, expectedDescriptor))
                || !isBaseline(revisionState(
                        dart, expectedDart, expectedDart))) {
            throw new IOException(phase + " refused because the current descriptor "
                    + "and generated Dart pair no longer matches the exact expected "
                    + "revision; no foreign bytes were overwritten");
        }
    }

    private static Path stage(Path target, byte[] bytes) throws IOException {
        Path parent = target.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(parent)) {
            throw new IOException("target parent is missing or unsafe: " + parent);
        }
        Path temporary = Files.createTempFile(
                parent, "." + target.getFileName() + ".", ".tmp");
        IOException writeFailure = null;
        try {
            Files.write(temporary, bytes);
        } catch (IOException | RuntimeException failure) {
            writeFailure = asIOException(
                    "stage project theme file failed for " + target, failure);
        }
        if (writeFailure != null) {
            IOException cleanupFailure = cleanupTemporary(
                    temporary, bytes, "staged project theme file");
            if (cleanupFailure != null) {
                writeFailure.addSuppressed(cleanupFailure);
            }
            throw writeFailure;
        }
        return temporary;
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static byte[] readExactRegularFile(
            Path file,
            int maximumBytes,
            String role) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)) {
            throw failure("Read Flutter project themes", file,
                    role + " is missing or is not a regular non-symbolic file", null);
        }
        long size = Files.size(file);
        if (size <= 0 || size > maximumBytes) {
            throw failure("Read Flutter project themes", file,
                    role + " size " + size + " is outside 1.." + maximumBytes
                    + " bytes", null);
        }
        return Files.readAllBytes(file);
    }

    private static Path normalizeRoot(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw failure("Resolve Flutter project themes", root,
                    "project root is missing or is not a regular non-symbolic directory", null);
        }
        return root;
    }

    /** Bounded in-process serialization shared by every persistence instance. */
    static Object projectLock(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath().normalize();
        return PROJECT_LOCKS[Math.floorMod(root.hashCode(), PROJECT_LOCKS.length)];
    }

    static int projectLockStripeCount() {
        return PROJECT_LOCKS.length;
    }

    private static Object[] createProjectLocks() {
        Object[] locks = new Object[PROJECT_LOCK_STRIPE_COUNT];
        Arrays.setAll(locks, ignored -> new Object());
        return locks;
    }

    private static ThemeLoader loader(FlutterProjectThemeStore store) {
        FlutterProjectThemeStore nonNull = Objects.requireNonNull(store, "store");
        return nonNull::load;
    }

    private static IOException failure(
            String operation,
            Path target,
            String reason,
            Throwable cause) {
        String message = operation + " failed for " + target + ". Reason: "
                + compact(reason) + ".";
        return cause == null ? new IOException(message) : new IOException(message, cause);
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "no cause was reported";
        }
        String value = message.trim().replaceAll("\\s+", " ");
        return value.length() <= 800 ? value : value.substring(0, 800) + "…";
    }

    private static IOException asIOException(String operation, Throwable failure) {
        return failure instanceof IOException io
                ? io
                : new IOException(operation + ": " + compact(failure.getMessage()), failure);
    }

    record Snapshot(
            Path projectRoot,
            FlutterProjectTheme theme,
            byte[] descriptorBytes,
            byte[] dartBytes) {
        Snapshot {
            projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
            theme = Objects.requireNonNull(theme, "theme");
            descriptorBytes = Objects.requireNonNull(
                    descriptorBytes, "descriptorBytes").clone();
            dartBytes = Objects.requireNonNull(dartBytes, "dartBytes").clone();
        }

        @Override
        public byte[] descriptorBytes() {
            return descriptorBytes.clone();
        }

        @Override
        public byte[] dartBytes() {
            return dartBytes.clone();
        }
    }

    @FunctionalInterface
    interface PairWriter {
        void write(
                Path descriptor,
                Path dart,
                byte[] oldDescriptor,
                byte[] oldDart,
                byte[] newDescriptor,
                byte[] newDart) throws IOException;
    }

    @FunctionalInterface
    interface ThemeLoader {
        FlutterProjectThemeLoadResult load(Path projectRoot);
    }

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target) throws IOException;
    }

    private enum RevisionState {
        BASELINE,
        PUBLISHED,
        UNCHANGED,
        FOREIGN
    }
}

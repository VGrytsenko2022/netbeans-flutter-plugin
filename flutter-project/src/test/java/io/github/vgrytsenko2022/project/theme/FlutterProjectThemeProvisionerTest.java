package io.github.vgrytsenko2022.project.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectThemeProvisionerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void provisionsCanonicalDescriptorGeneratedDartAndMainWiring() throws Exception {
        Path root = freshProject("complete");
        byte[] original = Files.readAllBytes(root.resolve("lib/main.dart"));
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner();

        FlutterProjectTheme theme = provisioner.provisionNewProject(root);

        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        String main = Files.readString(root.resolve("lib/main.dart"));
        byte[] generated = Files.readAllBytes(dart);
        assertTrue(Files.isRegularFile(descriptor));
        assertTrue(Files.isRegularFile(dart));
        assertTrue(main.contains("import 'theme/app_theme.dart';"));
        assertTrue(main.contains("theme: AppTheme.light"));
        assertTrue(main.contains("darkTheme: AppTheme.dark"));
        assertTrue(main.contains("themeMode: AppTheme.mode"));
        assertTrue(main.contains("_counter++;"));
        assertFalse(java.util.Arrays.equals(original, Files.readAllBytes(root.resolve("lib/main.dart"))));
        assertEquals(FlutterProjectThemeDigests.sha256(generated),
                theme.generated().sha256());
        FlutterProjectThemeLoadResult loaded = new FlutterProjectThemeStore().load(root);
        assertEquals(FlutterProjectThemeLoadStatus.VALID, loaded.status());
        assertEquals(theme, loaded.theme().orElseThrow());
        assertEquals(List.of(), transactionFiles(root));
    }

    @Test
    void createIfMissingReturnsAnExistingValidThemeWithoutRewritingFiles() throws Exception {
        Path root = freshProject("idempotent");
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner();
        FlutterProjectTheme first = provisioner.createDefaultIfMissing(root);
        byte[] descriptor = Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH));
        byte[] dart = Files.readAllBytes(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH));
        byte[] main = Files.readAllBytes(root.resolve("lib/main.dart"));

        FlutterProjectTheme second = provisioner.createDefaultIfMissing(root);

        assertEquals(first, second);
        assertArrayEquals(descriptor, Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertArrayEquals(dart, Files.readAllBytes(
                root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
        assertArrayEquals(main, Files.readAllBytes(root.resolve("lib/main.dart")));
    }

    @Test
    void malformedMainFailsBeforeCreatingAnyThemeArtifact() throws Exception {
        Path root = freshProject("malformed");
        Path main = root.resolve("lib/main.dart");
        byte[] original = "void main() {}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(main, original);

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectThemeProvisioner().provisionNewProject(root));

        assertTrue(failure.getMessage().contains("lib/main.dart"));
        assertArrayEquals(original, Files.readAllBytes(main));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
    }

    @Test
    void existingArtifactCollisionIsNeverOverwritten() throws Exception {
        Path root = freshProject("collision");
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Files.createDirectories(descriptor.getParent());
        byte[] sentinel = "user-owned\n".getBytes(StandardCharsets.UTF_8);
        Files.write(descriptor, sentinel);
        byte[] main = Files.readAllBytes(root.resolve("lib/main.dart"));

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectThemeProvisioner().provisionNewProject(root));

        assertTrue(failure.getMessage().contains("will not be overwritten"));
        assertArrayEquals(sentinel, Files.readAllBytes(descriptor));
        assertArrayEquals(main, Files.readAllBytes(root.resolve("lib/main.dart")));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
    }

    @Test
    void failureBeforeMainPublicationRollsBackGeneratedDartExactly() throws Exception {
        Path root = freshProject("rollback_before_main");
        byte[] original = Files.readAllBytes(root.resolve("lib/main.dart"));
        FlutterProjectThemeProvisioner provisioner = provisionerFailingAt(
                FlutterProjectThemeProvisioner.TransactionStep.MAIN_DART);

        IOException failure = assertThrows(IOException.class,
                () -> provisioner.provisionNewProject(root));

        assertTrue(failure.getMessage().contains("exact pre-provisioning files were restored"));
        assertArrayEquals(original, Files.readAllBytes(root.resolve("lib/main.dart")));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
        assertFalse(Files.exists(root.resolve("lib/theme")));
        assertFalse(Files.exists(root.resolve(".fd_templates")));
        assertEquals(List.of(), transactionFiles(root));
    }

    @Test
    void failureBeforeDescriptorPublicationRestoresMainAndRemovesOwnedDart() throws Exception {
        Path root = freshProject("rollback_before_descriptor");
        byte[] original = Files.readAllBytes(root.resolve("lib/main.dart"));
        FlutterProjectThemeProvisioner provisioner = provisionerFailingAt(
                FlutterProjectThemeProvisioner.TransactionStep.DESCRIPTOR);

        IOException failure = assertThrows(IOException.class,
                () -> provisioner.provisionNewProject(root));

        assertTrue(failure.getMessage().contains("exact pre-provisioning files were restored"));
        assertArrayEquals(original, Files.readAllBytes(root.resolve("lib/main.dart")));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
        assertEquals(List.of(), transactionFiles(root));
    }

    @Test
    void replaceThenThrowAtEveryPublishStepIsInferredAndRolledBackExactly()
            throws Exception {
        for (FlutterProjectThemeProvisioner.TransactionStep failureStep
                : FlutterProjectThemeProvisioner.TransactionStep.values()) {
            Path root = freshProject("after_effect_" + failureStep.name().toLowerCase());
            byte[] original = Files.readAllBytes(root.resolve("lib/main.dart"));
            AtomicBoolean injected = new AtomicBoolean();
            FlutterProjectThemeProvisioner.MoveOperation mover =
                    (source, target, replaceExisting) -> {
                        if (replaceExisting) {
                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.move(source, target);
                        }
                        if (!injected.get()
                                && publicationStep(root, target) == failureStep
                                && injected.compareAndSet(false, true)) {
                            throw new IOException(
                                    "injected after-effect " + failureStep + " failure");
                        }
                    };
            FlutterProjectThemeProvisioner provisioner =
                    new FlutterProjectThemeProvisioner(
                            new FlutterProjectThemeCodec(),
                            new FlutterGeneratedMainThemeTransformer(),
                            (step, target) -> { },
                            mover);

            IOException failure = assertThrows(IOException.class,
                    () -> provisioner.provisionNewProject(root), failureStep.toString());

            assertTrue(injected.get(), failureStep.toString());
            assertTrue(failure.getMessage().contains(
                    "exact pre-provisioning files were restored"), failureStep.toString());
            assertArrayEquals(original, Files.readAllBytes(root.resolve("lib/main.dart")),
                    failureStep.toString());
            assertFalse(Files.exists(root.resolve(
                    FlutterProjectThemePaths.DESCRIPTOR_PATH)), failureStep.toString());
            assertFalse(Files.exists(root.resolve(
                    FlutterProjectThemePaths.GENERATED_DART_PATH)), failureStep.toString());
            assertEquals(List.of(), transactionFiles(root), failureStep.toString());
        }
    }

    @Test
    void foreignBytesWrittenAfterPublishFailureArePreserved() throws Exception {
        Path root = freshProject("foreign_after_effect");
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        byte[] foreign = "// external generated Dart replacement\n"
                .getBytes(StandardCharsets.UTF_8);
        AtomicBoolean injected = new AtomicBoolean();
        FlutterProjectThemeProvisioner.MoveOperation mover =
                (source, target, replaceExisting) -> {
                    if (replaceExisting) {
                        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.move(source, target);
                    }
                    if (target.equals(dart) && injected.compareAndSet(false, true)) {
                        Files.write(target, foreign);
                        throw new IOException("injected foreign after-effect Dart failure");
                    }
                };
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner(
                new FlutterProjectThemeCodec(),
                new FlutterGeneratedMainThemeTransformer(),
                (step, target) -> { },
                mover);

        IOException failure = assertThrows(IOException.class,
                () -> provisioner.provisionNewProject(root));

        assertTrue(failure.getMessage().contains("rollback could not be proven"));
        assertArrayEquals(foreign, Files.readAllBytes(dart));
        assertFalse(Files.exists(root.resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
    }

    @Test
    void concurrentArtifactCreatedAtMoveBoundaryIsNeverOverwritten()
            throws Exception {
        Path root = freshProject("move_boundary_collision");
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        byte[] originalMain = Files.readAllBytes(root.resolve("lib/main.dart"));
        byte[] foreign = "// concurrently created user file\n"
                .getBytes(StandardCharsets.UTF_8);
        AtomicBoolean injected = new AtomicBoolean();
        FlutterProjectThemeProvisioner.MoveOperation mover =
                (source, target, replaceExisting) -> {
                    if (target.equals(dart) && injected.compareAndSet(false, true)) {
                        // This is deliberately after provisioner's requireAbsent check
                        // and immediately before the real platform move.
                        Files.write(target, foreign);
                    }
                    FlutterProjectThemeProvisioner.movePlatform(
                            source, target, replaceExisting);
                };
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner(
                new FlutterProjectThemeCodec(),
                new FlutterGeneratedMainThemeTransformer(),
                (step, target) -> { },
                mover);

        IOException failure = assertThrows(IOException.class,
                () -> provisioner.provisionNewProject(root));

        assertTrue(injected.get());
        assertTrue(failure.getMessage().contains("rollback could not be proven"));
        assertArrayEquals(foreign, Files.readAllBytes(dart));
        assertArrayEquals(originalMain, Files.readAllBytes(root.resolve("lib/main.dart")));
        assertFalse(Files.exists(root.resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
    }

    @Test
    void uncertainMainRollbackRetainsGeneratedDartAndExactBackup() throws Exception {
        Path root = freshProject("foreign_edit");
        byte[] foreign = "// external writer\nvoid main() {}\n"
                .getBytes(StandardCharsets.UTF_8);
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner(
                new FlutterProjectThemeCodec(),
                new FlutterGeneratedMainThemeTransformer(),
                (step, target) -> {
                    if (step == FlutterProjectThemeProvisioner.TransactionStep.DESCRIPTOR) {
                        Files.write(root.resolve("lib/main.dart"), foreign);
                        throw new IOException("injected external edit");
                    }
                });

        IOException failure = assertThrows(IOException.class,
                () -> provisioner.provisionNewProject(root));

        assertTrue(failure.getMessage().contains("rollback could not be proven"));
        assertArrayEquals(foreign, Files.readAllBytes(root.resolve("lib/main.dart")));
        assertTrue(Files.isRegularFile(
                root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)),
                "Dart theme must remain because the foreign main state is not understood");
        assertTrue(transactionFiles(root).stream()
                .anyMatch(path -> path.getFileName().toString().endsWith(".bak")),
                "Exact original main backup must be retained for recovery");
    }

    @Test
    void cleanupFailureAfterVerifiedCommitDoesNotRollBackValidProject() throws Exception {
        Path root = freshProject("cleanup_failure");
        AtomicBoolean cleanupReached = new AtomicBoolean();
        FlutterProjectThemeProvisioner.TransactionObserver observer =
                new FlutterProjectThemeProvisioner.TransactionObserver() {
                    @Override
                    public void beforePublish(
                            FlutterProjectThemeProvisioner.TransactionStep step,
                            Path target) {
                    }

                    @Override
                    public void afterCommitBeforeCleanup(Path mainBackup) throws IOException {
                        cleanupReached.set(true);
                        throw new IOException("injected cleanup failure");
                    }
                };
        FlutterProjectThemeProvisioner provisioner = new FlutterProjectThemeProvisioner(
                new FlutterProjectThemeCodec(),
                new FlutterGeneratedMainThemeTransformer(), observer);

        FlutterProjectTheme theme = provisioner.provisionNewProject(root);

        assertTrue(cleanupReached.get());
        assertEquals(theme, new FlutterProjectThemeStore().load(root).theme().orElseThrow());
        assertTrue(Files.readString(root.resolve("lib/main.dart"))
                .contains("theme: AppTheme.light"));
        assertTrue(transactionFiles(root).stream()
                .anyMatch(path -> path.getFileName().toString().endsWith(".bak")));
    }

    @Test
    void symbolicThemeDirectoryIsRejectedWithoutTouchingItsTarget() throws Exception {
        Path root = freshProject("symlink");
        Path external = Files.createDirectory(temporaryDirectory.resolve("external"));
        Path themeDirectory = root.resolve("lib/theme");
        try {
            Files.createSymbolicLink(themeDirectory, external);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + ex);
        }
        byte[] main = Files.readAllBytes(root.resolve("lib/main.dart"));

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectThemeProvisioner().provisionNewProject(root));

        assertTrue(failure.getMessage().contains("symbolic link"));
        assertArrayEquals(main, Files.readAllBytes(root.resolve("lib/main.dart")));
        try (var children = Files.list(external)) {
            assertEquals(List.of(), children.toList());
        }
    }

    @Test
    void oversizedMainIsRejectedWithoutMutation() throws Exception {
        Path root = freshProject("oversized");
        Path main = root.resolve("lib/main.dart");
        byte[] oversized = new byte[FlutterProjectThemeProvisioner.MAX_GENERATED_MAIN_BYTES + 1];
        Files.write(main, oversized);

        IOException failure = assertThrows(IOException.class,
                () -> new FlutterProjectThemeProvisioner().provisionNewProject(root));

        assertTrue(failure.getMessage().contains("1 to "));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertFalse(Files.exists(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)));
    }

    private FlutterProjectThemeProvisioner provisionerFailingAt(
            FlutterProjectThemeProvisioner.TransactionStep failureStep) {
        return new FlutterProjectThemeProvisioner(
                new FlutterProjectThemeCodec(),
                new FlutterGeneratedMainThemeTransformer(),
                (step, target) -> {
                    if (step == failureStep) {
                        throw new IOException("injected " + step + " failure");
                    }
                });
    }

    private static FlutterProjectThemeProvisioner.TransactionStep publicationStep(
            Path root, Path target) {
        if (target.equals(root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH))) {
            return FlutterProjectThemeProvisioner.TransactionStep.GENERATED_DART;
        }
        if (target.equals(root.resolve("lib/main.dart"))) {
            return FlutterProjectThemeProvisioner.TransactionStep.MAIN_DART;
        }
        if (target.equals(root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH))) {
            return FlutterProjectThemeProvisioner.TransactionStep.DESCRIPTOR;
        }
        return null;
    }

    private Path freshProject(String name) throws IOException {
        Path root = Files.createDirectory(temporaryDirectory.resolve(name));
        Files.createDirectory(root.resolve("lib"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                flutter:
                """);
        Files.writeString(root.resolve("lib/main.dart"),
                FlutterGeneratedMainThemeTransformerTest.generatedCounterMain("\n"));
        return root;
    }

    private static List<Path> transactionFiles(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".tmp") || name.endsWith(".bak");
                    })
                    .sorted()
                    .toList();
        }
    }
}

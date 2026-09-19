package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.move.DesignerPairMoveDependencyPlanner;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/** Project-inventory safety contract for pair-aware Designer Move. */
class FlutterDesignerMoveDependencyGuardTest {
    private static final String PACKAGE_NAME = "guard_fixture";
    private static final String SAFE_MOVED_SOURCE = """
            import 'dart:async';
            import 'package:http/http.dart';
            import 'package:guard_fixture/shared/theme.dart';

            class HomePage {}
            """;

    @TempDir
    Path temporaryDirectory;

    private final FlutterDesignerMoveDependencyGuard guard =
            new FlutterDesignerMoveDependencyGuard();

    @Test
    void acceptsCompleteSafeProjectInventory() throws Exception {
        PairFixture fixture = pair("safe_inventory", SAFE_MOVED_SOURCE);
        writeDart(fixture, "lib/shared/theme.dart", "class AppTheme {}\n");
        writeDart(fixture, "test/widget_test.dart", """
                import 'package:test/test.dart';

                void main() {}
                """);
        Path excludedNestedPackage = fixture.projectPath().resolve(
                ".dart_tool/cache/pubspec.yaml");
        Files.createDirectories(excludedNestedPackage.getParent());
        Files.writeString(excludedNestedPackage,
                "name: ignored_tool_cache\n", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> guard.verify(
                fixture.pair(),
                "destination",
                Files.readAllBytes(fixture.dartPath())));
    }

    @Test
    void rejectsOutgoingRelativeDirectiveFromMovedSource() throws Exception {
        PairFixture fixture = pair("outgoing_relative", """
                import '../shared.dart';

                class HomePage {}
                """);
        writeDart(fixture, "lib/shared.dart", "class Shared {}\n");

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "OUTGOING_RELATIVE_DIRECTIVE");
        assertContains(failure, "lib/screens/home.dart");
        assertContains(failure, "../shared.dart");
    }

    @Test
    void rejectsIncomingSelfPackageReference() throws Exception {
        PairFixture fixture = pair("incoming_self_package", SAFE_MOVED_SOURCE);
        writeDart(fixture, "lib/shared/theme.dart", "class AppTheme {}\n");
        writeDart(fixture, "lib/consumer.dart", """
                import 'package:guard_fixture/screens/home.dart';

                class Consumer {}
                """);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "INCOMING_REFERENCE");
        assertContains(failure, "lib/consumer.dart");
        assertContains(failure,
                "package:guard_fixture/screens/home.dart");
    }

    @Test
    void rejectsMissingPackageConfig() throws Exception {
        PairFixture fixture = pair("missing_package_config", SAFE_MOVED_SOURCE);
        Files.delete(fixture.projectPath().resolve(
                ".dart_tool/package_config.json"));

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                ".dart_tool/package_config.json is missing or not a safe regular file");
    }

    @Test
    void rejectsPackageConfigWhoseSelfRootDoesNotMatchProject()
            throws Exception {
        PairFixture fixture = pair("mismatched_package_config", SAFE_MOVED_SOURCE);
        writePackageConfig(fixture.projectPath(), "../../", "lib/");

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "pubspec and package_config self-package roots disagree");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "3", "\"2\"", "2.0", "null"})
    void rejectsUnsupportedOrNonIntegerPackageConfigVersion(String version)
            throws Exception {
        PairFixture fixture = pair(
                "invalid_config_version_"
                + version.replaceAll("[^A-Za-z0-9]", "_"),
                SAFE_MOVED_SOURCE);
        Path config = fixture.projectPath().resolve(
                ".dart_tool/package_config.json");
        String current = Files.readString(config, StandardCharsets.UTF_8);
        Files.writeString(config,
                current.replace("\"configVersion\": 2",
                        "\"configVersion\": " + version),
                StandardCharsets.UTF_8);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "package_config.json configVersion is not exactly 2");
    }

    @Test
    void rejectsDuplicatePackageConfigProperties() throws Exception {
        PairFixture fixture = pair(
                "duplicate_config_property", SAFE_MOVED_SOURCE);
        Path config = fixture.projectPath().resolve(
                ".dart_tool/package_config.json");
        String current = Files.readString(config, StandardCharsets.UTF_8);
        Files.writeString(config,
                current.replace("\"configVersion\": 2",
                        "\"configVersion\": 2,\n  \"configVersion\": 2"),
                StandardCharsets.UTF_8);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "package_config.json could not be parsed exactly");
        assertContains(failure, "Duplicate field 'configVersion'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.19", "3.0", "3.9"})
    void acceptsCanonicalSupportedSelfLanguageVersions(String languageVersion)
            throws Exception {
        PairFixture fixture = pair(
                "language_" + languageVersion.replace('.', '_'),
                SAFE_MOVED_SOURCE);
        writePackageConfig(
                fixture.projectPath(), "../", "lib/", languageVersion);

        assertDoesNotThrow(() -> guard.verify(
                fixture.pair(),
                "destination",
                Files.readAllBytes(fixture.dartPath())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "3", "3.0.0", "03.0", "3.01", "4.0"})
    void rejectsMalformedOrUnsupportedSelfLanguageVersion(
            String languageVersion) throws Exception {
        PairFixture fixture = pair(
                "invalid_language_"
                + languageVersion.replace('.', '_'),
                SAFE_MOVED_SOURCE);
        writePackageConfig(
                fixture.projectPath(), "../", "lib/", languageVersion);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "languageVersion '" + languageVersion + "'");
        assertContains(failure, "supported Dart 2.x or 3.x");
    }

    @Test
    void rejectsMissingSelfLanguageVersion() throws Exception {
        PairFixture fixture = pair(
                "missing_language_version", SAFE_MOVED_SOURCE);
        writePackageConfigEntries(fixture.projectPath(), """
                {
                  "name": "guard_fixture",
                  "rootUri": "../",
                  "packageUri": "lib/"
                }
                """);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "missing or non-text languageVersion");
    }

    @Test
    void rejectsNonTextSelfLanguageVersion() throws Exception {
        PairFixture fixture = pair(
                "non_text_language_version", SAFE_MOVED_SOURCE);
        writePackageConfigEntries(fixture.projectPath(), """
                {
                  "name": "guard_fixture",
                  "rootUri": "../",
                  "packageUri": "lib/",
                  "languageVersion": 3.0
                }
                """);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "missing or non-text languageVersion");
    }

    @Test
    void rejectsNonSelfPackageAliasForPhysicalProjectLib() throws Exception {
        PairFixture fixture = pair("self_lib_alias", SAFE_MOVED_SOURCE);
        writePackageConfigEntries(fixture.projectPath(), """
                {
                  "name": "guard_fixture",
                  "rootUri": "../",
                  "packageUri": "lib/",
                  "languageVersion": "3.0"
                },
                {
                  "name": "project_alias",
                  "rootUri": "../",
                  "packageUri": "./lib/",
                  "languageVersion": "3.0"
                }
                """);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "package_config entry 'project_alias' aliases the project lib root");
        assertContains(failure, "guard_fixture");
    }

    @Test
    void rejectsImplicitRootAliasWhenNonSelfPackageUriIsMissing()
            throws Exception {
        PairFixture fixture = pair("implicit_self_lib_alias", SAFE_MOVED_SOURCE);
        writePackageConfigEntries(fixture.projectPath(), """
                {
                  "name": "guard_fixture",
                  "rootUri": "../",
                  "packageUri": "lib/",
                  "languageVersion": "3.0"
                },
                {
                  "name": "implicit_project_alias",
                  "rootUri": "../lib/",
                  "languageVersion": "3.0"
                }
                """);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "package_config entry 'implicit_project_alias' aliases the project lib root");
    }

    @Test
    void acceptsDistinctPhysicalExternalPackageLibraryRoot() throws Exception {
        PairFixture fixture = pair("external_package", SAFE_MOVED_SOURCE);
        Path externalRoot = temporaryDirectory.resolve("external_dependency");
        Files.createDirectories(externalRoot.resolve("lib"));
        writePackageConfigEntries(fixture.projectPath(), """
                {
                  "name": "guard_fixture",
                  "rootUri": "../",
                  "packageUri": "lib/",
                  "languageVersion": "3.0"
                },
                {
                  "name": "external_dependency",
                  "rootUri": "%s",
                  "packageUri": "lib/",
                  "languageVersion": "3.0"
                }
                """.formatted(externalRoot.toUri().toASCIIString()));

        assertDoesNotThrow(() -> guard.verify(
                fixture.pair(),
                "destination",
                Files.readAllBytes(fixture.dartPath())));
    }

    @Test
    void rejectsNestedPubspecPackageOutsideExcludedRoots() throws Exception {
        PairFixture fixture = pair("nested_package", SAFE_MOVED_SOURCE);
        Path nestedPubspec = fixture.projectPath().resolve(
                "packages/child/pubspec.yaml");
        Files.createDirectories(nestedPubspec.getParent());
        Files.writeString(nestedPubspec,
                "name: nested_child\n", StandardCharsets.UTF_8);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "nested pubspec.yaml packages are not supported");
        assertContains(failure, "packages/child/pubspec.yaml");
    }

    @Test
    void rejectsInventoryBeforeAnUnboundedEntryCollectionCanGrow()
            throws Exception {
        PairFixture fixture = pair("bounded_walk", SAFE_MOVED_SOURCE);
        FlutterDesignerMoveDependencyGuard boundedGuard =
                new FlutterDesignerMoveDependencyGuard(
                        new DesignerPairMoveDependencyPlanner(),
                        new FlutterDesignerMoveDependencyGuard.InventoryBounds(
                                1, 32, 32));

        IOException failure = assertThrows(IOException.class, () ->
                boundedGuard.verify(
                        fixture.pair(),
                        "destination",
                        Files.readAllBytes(fixture.dartPath())));

        assertContains(failure, "Dart inventory entry count exceeds 1");
    }

    @Test
    void rejectsModifiedUnrelatedProjectDartDataObject() throws Exception {
        PairFixture fixture = pair("modified_unrelated", SAFE_MOVED_SOURCE);
        Path unrelatedPath = writeDart(
                fixture, "test/unsaved_test.dart", "void main() {}\n");
        FileObject unrelatedFile = FileUtil.toFileObject(
                unrelatedPath.toFile());
        assertNotNull(unrelatedFile);
        DataObject unrelated = DataObject.find(unrelatedFile);
        unrelated.setModified(true);
        assertTrue(unrelated.isModified());

        try {
            IOException failure = assertRejected(
                    fixture, Files.readAllBytes(fixture.dartPath()));
            assertContains(failure,
                    "Dart source has unsaved editor changes: test/unsaved_test.dart");
        } finally {
            unrelated.setModified(false);
        }
    }

    @Test
    void exclusiveProofRunsFinalVerificationAndCommitOnEventThread()
            throws Exception {
        PairFixture fixture = pair("exclusive_event_thread", SAFE_MOVED_SOURCE);
        boolean[] callbackOnEventThread = {false};

        verifyExclusivelyWithMovedDartLock(fixture, () ->
                callbackOnEventThread[0] = EventQueue.isDispatchThread());

        assertTrue(callbackOnEventThread[0]);
    }

    @Test
    void exclusiveProofPreventsAnotherDartFileFromBeingLockedForSave()
            throws Exception {
        PairFixture fixture = pair("exclusive_dart_lock", SAFE_MOVED_SOURCE);
        Path unrelatedPath = writeDart(
                fixture, "lib/shared/value.dart", "class Value {}\n");
        FileObject unrelated = FileUtil.toFileObject(unrelatedPath.toFile());
        assertNotNull(unrelated);

        verifyExclusivelyWithMovedDartLock(fixture, () -> {
            assertTrue(unrelated.isLocked());
            assertThrows(IOException.class, () -> {
                try (FileLock ignored = unrelated.lock()) {
                    // Reaching this block would be a fail-open admission.
                }
            });
        });

        assertFalse(unrelated.isLocked());
        try (FileLock released = unrelated.lock()) {
            assertTrue(released.isValid());
        }
    }

    @Test
    void exclusiveProofRejectsCallerTokenOwnedByAnotherFile()
            throws Exception {
        PairFixture fixture = pair(
                "exclusive_wrong_caller_token", SAFE_MOVED_SOURCE);
        Path unrelatedPath = writeDart(
                fixture, "lib/shared/value.dart", "class Value {}\n");
        FileObject unrelated = FileUtil.toFileObject(unrelatedPath.toFile());
        assertNotNull(unrelated);

        try (FileLock wrongToken = unrelated.lock()) {
            IOException failure = assertThrows(IOException.class, () ->
                    guard.verifyExclusively(
                            fixture.pair(),
                            "destination",
                            Files.readAllBytes(fixture.dartPath()),
                            List.of(new FlutterDesignerPairMove.DependencyGuard
                                    .CallerLock(
                                            fixture.pair().dartFile(),
                                            wrongToken)),
                            () -> {
                            }));
            assertContains(failure,
                    "caller-owned lock belongs to a different file");
        }
    }

    @Test
    void exclusiveProofPreventsARelevantFolderFromBeingRenamed()
            throws Exception {
        PairFixture fixture = pair("exclusive_folder_rename", SAFE_MOVED_SOURCE);
        Path nestedPath = writeDart(
                fixture, "lib/shared/value.dart", "class Value {}\n");
        Path folderPath = nestedPath.getParent();
        FileObject folder = FileUtil.toFileObject(folderPath.toFile());
        assertNotNull(folder);
        CountDownLatch renameEntered = new CountDownLatch(1);
        AtomicReference<Future<Void>> pendingRename = new AtomicReference<>();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            verifyExclusivelyWithMovedDartLock(fixture, () -> {
                Future<Void> pending = worker.submit(() -> {
                    renameEntered.countDown();
                    try (FileLock renameLock = folder.lock()) {
                        folder.rename(
                                renameLock, "renamed_after_admission", null);
                    }
                    return null;
                });
                pendingRename.set(pending);
                try {
                    if (!renameEntered.await(2, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "adversarial folder-rename worker did not start");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "interrupted while testing folder-rename admission",
                            interrupted);
                }
                assertThrows(TimeoutException.class, () ->
                        pending.get(250, TimeUnit.MILLISECONDS));
            });

            pendingRename.get().get(5, TimeUnit.SECONDS);
            assertFalse(Files.exists(folderPath));
            assertTrue(Files.isDirectory(folderPath.resolveSibling(
                    "renamed_after_admission")));
        } finally {
            Future<Void> pending = pendingRename.get();
            if (pending != null && !pending.isDone()) {
                try {
                    pending.get(5, TimeUnit.SECONDS);
                } catch (Exception cleanupFailure) {
                    pending.cancel(true);
                }
            }
            worker.shutdownNow();
            worker.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void exclusiveProofBlocksNetBeansDartChildCreationUntilCommitReturns()
            throws Exception {
        PairFixture fixture = pair("exclusive_child_creation", SAFE_MOVED_SOURCE);
        FileObject lib = fixture.pair().dartFile().getParent().getParent();
        assertNotNull(lib);
        CountDownLatch creationEntered = new CountDownLatch(1);
        AtomicReference<Future<FileObject>> pendingCreation =
                new AtomicReference<>();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            verifyExclusivelyWithMovedDartLock(fixture, () -> {
                Future<FileObject> pending = worker.submit(() -> {
                    creationEntered.countDown();
                    return lib.createData("created_after_admission", "dart");
                });
                pendingCreation.set(pending);
                try {
                    if (!creationEntered.await(2, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "adversarial child-creation worker did not start");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "interrupted while testing child-creation admission",
                            interrupted);
                }
                assertThrows(TimeoutException.class, () ->
                        pending.get(250, TimeUnit.MILLISECONDS));
            });

            FileObject created = pendingCreation.get().get(
                    5, TimeUnit.SECONDS);
            assertNotNull(created);
            assertTrue(created.isValid());
        } finally {
            worker.shutdownNow();
            worker.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void exclusiveProofPropagatesCallbackFailureAndReleasesEveryOwnedLock()
            throws Exception {
        PairFixture fixture = pair("exclusive_callback_failure", SAFE_MOVED_SOURCE);
        Path unrelatedPath = writeDart(
                fixture, "lib/shared/value.dart", "class Value {}\n");
        FileObject unrelated = FileUtil.toFileObject(unrelatedPath.toFile());
        assertNotNull(unrelated);

        IOException failure = assertThrows(IOException.class, () ->
                verifyExclusivelyWithMovedDartLock(fixture, () -> {
                    throw new IOException("synthetic commit failure");
                }));

        assertContains(failure, "synthetic commit failure");
        assertFalse(unrelated.isLocked());
        try (FileLock released = unrelated.lock()) {
            assertTrue(released.isValid());
        }
    }

    @Test
    void rejectsStrictInvalidUtf8InAnyDartInventoryEntry() throws Exception {
        PairFixture fixture = pair("invalid_utf8", SAFE_MOVED_SOURCE);
        Path invalid = fixture.projectPath().resolve("lib/invalid.dart");
        Files.write(invalid, new byte[] {(byte) 0xc3, (byte) 0x28});

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure, "INVALID_UTF8");
        assertContains(failure, "lib/invalid.dart");
    }

    @Test
    void rejectsSymbolicLinkInInventoryWhenPlatformSupportsIt()
            throws Exception {
        PairFixture fixture = pair("symbolic_link", SAFE_MOVED_SOURCE);
        Path target = writeDart(
                fixture, "lib/shared.dart", "class Shared {}\n");
        Path link = fixture.projectPath().resolve("lib/linked.dart");
        createSymbolicLinkOrSkip(link, target);

        IOException failure = assertRejected(
                fixture, Files.readAllBytes(fixture.dartPath()));

        assertContains(failure,
                "the Dart inventory contains a symbolic link: lib/linked.dart");
    }

    @Test
    void rejectsMovedBytesThatDoNotExactlyMatchInventory() throws Exception {
        PairFixture fixture = pair("moved_bytes_mismatch", SAFE_MOVED_SOURCE);
        byte[] changed = (SAFE_MOVED_SOURCE + "// changed after snapshot\n")
                .getBytes(StandardCharsets.UTF_8);

        IOException failure = assertRejected(fixture, changed);

        assertContains(failure,
                "the moved Dart bytes changed during dependency inspection");
    }

    private IOException assertRejected(PairFixture fixture, byte[] movedBytes) {
        return assertThrows(IOException.class, () -> guard.verify(
                fixture.pair(), "destination", movedBytes));
    }

    private void verifyExclusivelyWithMovedDartLock(
            PairFixture fixture,
            FlutterDesignerPairMove.DependencyGuard.ExclusiveCommit commit)
            throws Exception {
        FileObject movedDart = fixture.pair().dartFile();
        byte[] exactBytes = Files.readAllBytes(fixture.dartPath());
        try (FileLock callerLock = movedDart.lock()) {
            guard.verifyExclusively(
                    fixture.pair(),
                    "destination",
                    exactBytes,
                    List.of(new FlutterDesignerPairMove.DependencyGuard.CallerLock(
                            movedDart, callerLock)),
                    commit);
            assertTrue(callerLock.isValid());
        }
    }

    private PairFixture pair(String name, String movedSource) throws Exception {
        Path projectPath = temporaryDirectory.resolve(name);
        Path dartPath = projectPath.resolve("lib/screens/home.dart");
        Path modelPath = projectPath.resolve(
                ".fd_templates/screens/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.createDirectories(projectPath.resolve(".dart_tool"));
        Files.writeString(projectPath.resolve("pubspec.yaml"), """
                name: guard_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        writePackageConfig(projectPath, "../", "lib/");
        Files.writeString(dartPath, movedSource, StandardCharsets.UTF_8);
        Files.writeString(modelPath, "{}\n", StandardCharsets.UTF_8);

        FileUtil.refreshFor(projectPath.toFile());
        FlutterProject project = FlutterDesignerTestProject.own(projectPath);
        FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dartFile);
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(dartFile, project)
                .orElseThrow();
        return new PairFixture(projectPath, dartPath, pair);
    }

    private static Path writeDart(
            PairFixture fixture,
            String projectRelativePath,
            String source) throws IOException {
        Path path = fixture.projectPath().resolve(projectRelativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
        FileUtil.refreshFor(path.toFile());
        return path;
    }

    private static void writePackageConfig(
            Path projectPath,
            String rootUri,
            String packageUri) throws IOException {
        writePackageConfig(projectPath, rootUri, packageUri, "3.0");
    }

    private static void writePackageConfig(
            Path projectPath,
            String rootUri,
            String packageUri,
            String languageVersion) throws IOException {
        writePackageConfigEntries(projectPath, """
                {
                  "name": "%s",
                  "rootUri": "%s",
                  "packageUri": "%s",
                  "languageVersion": "%s"
                }
                """.formatted(
                        PACKAGE_NAME, rootUri, packageUri, languageVersion));
    }

    private static void writePackageConfigEntries(
            Path projectPath,
            String entries) throws IOException {
        Path config = projectPath.resolve(".dart_tool/package_config.json");
        Files.createDirectories(config.getParent());
        Files.writeString(config, """
                {
                  "configVersion": 2,
                  "packages": [
                    %s
                  ]
                }
                """.formatted(entries),
                StandardCharsets.UTF_8);
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false,
                    "symbolic links are unavailable: " + ex.getMessage());
        }
    }

    private static void assertContains(IOException failure, String expected) {
        assertTrue(failure.getMessage().contains(expected),
                () -> "Expected <" + failure.getMessage()
                + "> to contain <" + expected + ">");
    }

    private record PairFixture(
            Path projectPath,
            Path dartPath,
            FlutterDesignerPairLayout.Pair pair) {
    }
}

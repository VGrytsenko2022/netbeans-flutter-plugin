package io.github.vgrytsenko2022.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.project.theme.FlutterGeneratedThemeArtifact;
import io.github.vgrytsenko2022.project.theme.FlutterProjectTheme;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeCodec;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDartGenerator;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeDigests;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadResult;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadStatus;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeStore;
import io.github.vgrytsenko2022.project.theme.FlutterThemeMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterThemePersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void projectSaveLocksAreNormalizedStableAndBounded() {
        Object canonical = FlutterThemePersistence.projectLock(
                temporaryDirectory.resolve("project"));
        Object equivalent = FlutterThemePersistence.projectLock(
                temporaryDirectory.resolve("child").resolve("..").resolve("project"));
        assertTrue(canonical == equivalent,
                "equivalent normalized project roots must serialize on one lock");

        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = 0; index < 10_000; index++) {
            identities.add(FlutterThemePersistence.projectLock(
                    temporaryDirectory.resolve("project-" + index)));
        }
        assertEquals(64, FlutterThemePersistence.projectLockStripeCount());
        assertTrue(identities.size() <= FlutterThemePersistence.projectLockStripeCount(),
                "project lock identity count must remain fixed and bounded");
    }

    @Test
    void savesDescriptorAndGeneratedDartAsOneVerifiedRevision() throws Exception {
        Fixture fixture = fixture();
        FlutterThemePersistence persistence = new FlutterThemePersistence();
        FlutterThemePersistence.Snapshot baseline = persistence.load(fixture.root());
        FlutterProjectTheme edited = withMode(
                baseline.theme(), FlutterThemeMode.DARK);

        FlutterThemePersistence.Snapshot saved = persistence.save(baseline, edited);

        assertEquals(FlutterThemeMode.DARK, saved.theme().defaultMode());
        assertTrue(new FlutterProjectThemeStore().load(fixture.root()).valid());
        assertArrayEquals(
                FlutterProjectThemeDartGenerator.generate(saved.theme()),
                Files.readAllBytes(fixture.dart()));
        assertEquals(
                saved.theme(),
                new FlutterProjectThemeCodec().read(fixture.descriptor()));
    }

    @Test
    void refusesToOverwriteUserModifiedGeneratedDart() throws Exception {
        Fixture fixture = fixture();
        FlutterThemePersistence persistence = new FlutterThemePersistence();
        FlutterThemePersistence.Snapshot baseline = persistence.load(fixture.root());
        byte[] changed = "// user-owned change\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(fixture.dart(), changed);

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(
                        baseline,
                        withMode(baseline.theme(), FlutterThemeMode.LIGHT)));

        assertTrue(failure.getMessage().contains("generated-hash conflict guard"));
        assertTrue(failure.getMessage().contains(fixture.root().toString()));
        assertArrayEquals(changed, Files.readAllBytes(fixture.dart()));
        assertArrayEquals(
                baseline.descriptorBytes(),
                Files.readAllBytes(fixture.descriptor()));
    }

    @Test
    void refusesAValidButExternallyReplacedDescriptorRevision() throws Exception {
        Fixture fixture = fixture();
        FlutterThemePersistence persistence = new FlutterThemePersistence();
        FlutterThemePersistence.Snapshot baseline = persistence.load(fixture.root());
        FlutterProjectTheme externalDraft = withMode(
                baseline.theme(), FlutterThemeMode.LIGHT);
        byte[] externalDart = FlutterProjectThemeDartGenerator.generate(externalDraft);
        FlutterProjectTheme external = new FlutterProjectTheme(
                externalDraft.defaultMode(),
                externalDraft.lightThemeId(),
                externalDraft.darkThemeId(),
                externalDraft.themes(),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FlutterProjectThemeDigests.sha256(externalDart)));
        byte[] externalBytes = new FlutterProjectThemeCodec().encode(external);
        Files.write(fixture.dart(), externalDart);
        Files.write(fixture.descriptor(), externalBytes);

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(
                        baseline,
                        withMode(baseline.theme(), FlutterThemeMode.DARK)));

        assertTrue(failure.getMessage().contains("descriptor changed"));
        assertArrayEquals(externalBytes, Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(externalDart, Files.readAllBytes(fixture.dart()));
    }

    @Test
    void rollsBackExactDartBytesWhenDescriptorPublishFails() throws Exception {
        Fixture fixture = fixture();
        byte[] oldDescriptor = Files.readAllBytes(fixture.descriptor());
        byte[] oldDart = Files.readAllBytes(fixture.dart());
        byte[] nextDescriptor = "next descriptor".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        byte[] nextDart = "next dart".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);
        AtomicInteger moves = new AtomicInteger();

        IOException failure = assertThrows(IOException.class, () ->
                FlutterThemePersistence.writePair(
                        fixture.descriptor(),
                        fixture.dart(),
                        oldDescriptor,
                        oldDart,
                        nextDescriptor,
                        nextDart,
                        (source, target) -> {
                            int move = moves.incrementAndGet();
                            if (move == 2) {
                                throw new IOException("injected descriptor publish failure");
                            }
                            Files.move(source, target,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }));

        assertTrue(failure.getMessage().contains("injected descriptor"));
        assertEquals(3, moves.get(), "third move must publish the exact Dart rollback");
        assertArrayEquals(oldDescriptor, Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(oldDart, Files.readAllBytes(fixture.dart()));
    }

    @Test
    void rollsBackBothFilesWhenMoveReplacesThenThrows() throws Exception {
        Fixture fixture = fixture();
        byte[] oldDescriptor = Files.readAllBytes(fixture.descriptor());
        byte[] oldDart = Files.readAllBytes(fixture.dart());
        byte[] nextDescriptor = "next descriptor".getBytes(StandardCharsets.UTF_8);
        byte[] nextDart = "next dart".getBytes(StandardCharsets.UTF_8);
        AtomicInteger moves = new AtomicInteger();

        IOException failure = assertThrows(IOException.class, () ->
                FlutterThemePersistence.writePair(
                        fixture.descriptor(),
                        fixture.dart(),
                        oldDescriptor,
                        oldDart,
                        nextDescriptor,
                        nextDart,
                        (source, target) -> {
                            int move = moves.incrementAndGet();
                            Files.move(source, target,
                                    StandardCopyOption.REPLACE_EXISTING);
                            if (move == 2) {
                                throw new IOException(
                                        "injected failure after descriptor replacement");
                            }
                        }));

        assertTrue(failure.getMessage().contains("after descriptor replacement"));
        assertEquals(4, moves.get(),
                "both exact files must be restored after an after-effect failure");
        assertArrayEquals(oldDescriptor, Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(oldDart, Files.readAllBytes(fixture.dart()));
    }

    @Test
    void rollbackInfersSuccessWhenRollbackMoveReplacesThenThrows()
            throws Exception {
        Fixture fixture = fixture();
        byte[] oldDescriptor = Files.readAllBytes(fixture.descriptor());
        byte[] oldDart = Files.readAllBytes(fixture.dart());
        byte[] nextDescriptor = "next descriptor".getBytes(StandardCharsets.UTF_8);
        byte[] nextDart = "next dart".getBytes(StandardCharsets.UTF_8);
        AtomicInteger moves = new AtomicInteger();

        IOException failure = assertThrows(IOException.class, () ->
                FlutterThemePersistence.writePair(
                        fixture.descriptor(), fixture.dart(),
                        oldDescriptor, oldDart, nextDescriptor, nextDart,
                        (source, target) -> {
                            int move = moves.incrementAndGet();
                            if (move == 2) {
                                throw new IOException(
                                        "injected descriptor publish failure");
                            }
                            Files.move(source, target,
                                    StandardCopyOption.REPLACE_EXISTING);
                            if (move == 3) {
                                throw new IOException(
                                        "injected after-effect rollback failure");
                            }
                        }));

        assertTrue(failure.getMessage().contains("descriptor publish"));
        assertEquals(3, moves.get());
        assertArrayEquals(oldDescriptor, Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(oldDart, Files.readAllBytes(fixture.dart()));
    }

    @Test
    void failedRollbackPreservesExactRecoveryTemporary() throws Exception {
        Fixture fixture = fixture();
        byte[] oldDescriptor = Files.readAllBytes(fixture.descriptor());
        byte[] oldDart = Files.readAllBytes(fixture.dart());
        AtomicInteger moves = new AtomicInteger();

        IOException failure = assertThrows(IOException.class, () ->
                FlutterThemePersistence.writePair(
                        fixture.descriptor(), fixture.dart(),
                        oldDescriptor, oldDart,
                        "next descriptor".getBytes(StandardCharsets.UTF_8),
                        "next dart".getBytes(StandardCharsets.UTF_8),
                        (source, target) -> {
                            int move = moves.incrementAndGet();
                            if (move >= 2) {
                                throw new IOException("injected before-effect failure");
                            }
                            Files.move(source, target,
                                    StandardCopyOption.REPLACE_EXISTING);
                        }));

        assertTrue(failure.getMessage().contains("rollback"));
        assertTrue(failure.getMessage().contains("preserved"));
        try (var entries = Files.list(fixture.dart().getParent())) {
            Path recovery = entries
                    .filter(path -> path.getFileName().toString().endsWith(".tmp"))
                    .findFirst()
                    .orElseThrow();
            assertArrayEquals(oldDart, Files.readAllBytes(recovery));
        }
    }

    @Test
    void raceBetweenPairMovesNeverOverwritesForeignDescriptor()
            throws Exception {
        Fixture fixture = fixture();
        byte[] oldDescriptor = Files.readAllBytes(fixture.descriptor());
        byte[] oldDart = Files.readAllBytes(fixture.dart());
        byte[] foreignDescriptor = "foreign descriptor".getBytes(
                StandardCharsets.UTF_8);
        AtomicInteger moves = new AtomicInteger();

        IOException failure = assertThrows(IOException.class, () ->
                FlutterThemePersistence.writePair(
                        fixture.descriptor(), fixture.dart(),
                        oldDescriptor, oldDart,
                        "next descriptor".getBytes(StandardCharsets.UTF_8),
                        "next dart".getBytes(StandardCharsets.UTF_8),
                        (source, target) -> {
                            int move = moves.incrementAndGet();
                            if (move == 1) {
                                Files.write(fixture.descriptor(), foreignDescriptor);
                            }
                            Files.move(source, target,
                                    StandardCopyOption.REPLACE_EXISTING);
                        }));

        assertTrue(failure.getMessage().contains("no foreign bytes were overwritten"));
        assertArrayEquals(
                foreignDescriptor, Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(oldDart, Files.readAllBytes(fixture.dart()));
    }

    @Test
    void postPublishVerificationFailureRestoresExactEditorBaseline()
            throws Exception {
        Fixture fixture = fixture();
        FlutterThemePersistence normal = new FlutterThemePersistence();
        FlutterThemePersistence.Snapshot baseline = normal.load(fixture.root());
        AtomicInteger loads = new AtomicInteger();
        FlutterProjectThemeStore realStore = new FlutterProjectThemeStore();
        FlutterThemePersistence persistence = new FlutterThemePersistence(
                root -> loads.incrementAndGet() == 1
                        ? realStore.load(root)
                        : new FlutterProjectThemeLoadResult(
                                FlutterProjectThemeLoadStatus.IO_ERROR,
                                Optional.empty(),
                                "injected post-publication verification failure"),
                new FlutterProjectThemeCodec(),
                FlutterThemePersistence::writePair);

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(
                        baseline,
                        withMode(baseline.theme(), FlutterThemeMode.DARK)));

        assertTrue(failure.getMessage().contains(
                "injected post-publication verification failure"));
        assertTrue(failure.getMessage().contains("rolled back"));
        assertArrayEquals(
                baseline.descriptorBytes(), Files.readAllBytes(fixture.descriptor()));
        assertArrayEquals(baseline.dartBytes(), Files.readAllBytes(fixture.dart()));
        assertTrue(realStore.load(fixture.root()).valid());
    }

    @Test
    void postPublishRaceRefusesRollbackAndNeverOverwritesForeignBytes()
            throws Exception {
        Fixture fixture = fixture();
        FlutterThemePersistence normal = new FlutterThemePersistence();
        FlutterThemePersistence.Snapshot baseline = normal.load(fixture.root());
        byte[] foreignDart = "// concurrent user change\n".getBytes(
                StandardCharsets.UTF_8);
        FlutterProjectThemeStore realStore = new FlutterProjectThemeStore();
        FlutterThemePersistence persistence = new FlutterThemePersistence(
                realStore,
                new FlutterProjectThemeCodec(),
                (descriptor, dart, oldDescriptor, oldDart,
                        newDescriptor, newDart) -> {
                    Files.write(dart, newDart);
                    Files.write(descriptor, newDescriptor);
                    Files.write(dart, foreignDart);
                });

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(
                        baseline,
                        withMode(baseline.theme(), FlutterThemeMode.DARK)));

        assertTrue(failure.getMessage().contains("rollback was refused"));
        assertTrue(failure.getMessage().contains("no foreign bytes were overwritten"));
        assertArrayEquals(foreignDart, Files.readAllBytes(fixture.dart()));
        assertTrue(!java.util.Arrays.equals(
                baseline.descriptorBytes(), Files.readAllBytes(fixture.descriptor())),
                "descriptor must not be separately restored across a raced pair");
    }

    private Fixture fixture() throws Exception {
        Path root = temporaryDirectory.resolve("app");
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dart = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.createDirectories(dart.getParent());
        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme("0".repeat(64));
        byte[] dartBytes = FlutterProjectThemeDartGenerator.generate(provisional);
        FlutterProjectTheme theme = new FlutterProjectTheme(
                provisional.defaultMode(),
                provisional.lightThemeId(),
                provisional.darkThemeId(),
                provisional.themes(),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FlutterProjectThemeDigests.sha256(dartBytes)));
        Files.write(dart, dartBytes);
        Files.write(descriptor, new FlutterProjectThemeCodec().encode(theme));
        return new Fixture(root, descriptor, dart);
    }

    private static FlutterProjectTheme withMode(
            FlutterProjectTheme source,
            FlutterThemeMode mode) {
        return new FlutterProjectTheme(
                mode,
                source.lightThemeId(),
                source.darkThemeId(),
                source.themes(),
                source.generated());
    }

    private record Fixture(Path root, Path descriptor, Path dart) {
    }
}

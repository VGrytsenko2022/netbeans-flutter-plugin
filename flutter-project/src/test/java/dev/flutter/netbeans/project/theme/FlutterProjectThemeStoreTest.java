package dev.flutter.netbeans.project.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectThemeStoreTest {
    @TempDir
    Path temporaryDirectory;

    private final FlutterProjectThemeStore store = new FlutterProjectThemeStore();

    @Test
    void distinguishesMissingInvalidMissingArtifactMismatchAndValid() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("project"));
        assertEquals(FlutterProjectThemeLoadStatus.MISSING, store.load(root).status());

        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, "{}\n");
        assertEquals(FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                store.load(root).status());

        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme("0".repeat(64));
        byte[] dart = FlutterProjectThemeDartGenerator.generate(provisional);
        FlutterProjectTheme theme = FlutterProjectTheme.defaultTheme(
                FlutterProjectThemeDigests.sha256(dart));
        Files.write(descriptor, new FlutterProjectThemeCodec().encode(theme));
        FlutterProjectThemeLoadResult missingDart = store.load(root);
        assertEquals(FlutterProjectThemeLoadStatus.GENERATED_DART_MISSING,
                missingDart.status());
        assertEquals(theme, missingDart.theme().orElseThrow());

        Path dartFile = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(dartFile.getParent());
        Files.writeString(dartFile, "tampered\n");
        FlutterProjectThemeLoadResult mismatch = store.load(root);
        assertEquals(FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                mismatch.status());
        assertTrue(mismatch.detail().contains("expected"));
        assertTrue(mismatch.detail().contains("found"));

        Files.write(dartFile, dart);
        FlutterProjectThemeLoadResult valid = store.load(root);
        assertEquals(FlutterProjectThemeLoadStatus.VALID, valid.status());
        assertTrue(valid.valid());
        assertEquals(theme, valid.theme().orElseThrow());
    }

    @Test
    void rejectsDescriptorSemanticsWhoseRecordedHashStillNamesOldDart()
            throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("stale_semantics"));
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dartFile = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.createDirectories(dartFile.getParent());

        FlutterProjectTheme provisional = FlutterProjectTheme.defaultTheme("0".repeat(64));
        byte[] oldDart = FlutterProjectThemeDartGenerator.generate(provisional);
        FlutterProjectTheme original = FlutterProjectTheme.defaultTheme(
                FlutterProjectThemeDigests.sha256(oldDart));
        FlutterProjectTheme staleDescriptor = new FlutterProjectTheme(
                original.defaultMode(),
                original.lightThemeId(),
                original.darkThemeId(),
                List.of(
                        new FlutterProjectThemeDefinition(
                                "light", "Light", FlutterThemeBrightness.LIGHT,
                                0xFF123456),
                        original.darkTheme()),
                original.generated());
        Files.write(descriptor, new FlutterProjectThemeCodec().encode(staleDescriptor));
        Files.write(dartFile, oldDart);

        FlutterProjectThemeLoadResult result = store.load(root);

        assertEquals(FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                result.status());
        assertEquals(staleDescriptor, result.theme().orElseThrow());
        assertTrue(result.detail().contains("descriptor semantics require"));
        assertTrue(result.detail().contains(original.generated().sha256()));
    }

    @Test
    void validatesDisabledCatalogAndItsDeterministicNullableDartApi() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("disabled"));
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        Path dartFile = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH);
        Files.createDirectories(descriptor.getParent());
        Files.createDirectories(dartFile.getParent());

        FlutterProjectTheme defaults = FlutterProjectTheme.defaultTheme("0".repeat(64));
        FlutterProjectTheme provisional = new FlutterProjectTheme(
                false,
                defaults.defaultMode(),
                defaults.lightThemeId(),
                defaults.darkThemeId(),
                defaults.themes(),
                defaults.generated());
        byte[] dart = FlutterProjectThemeDartGenerator.generate(provisional);
        FlutterProjectTheme disabled = new FlutterProjectTheme(
                false,
                provisional.defaultMode(),
                provisional.lightThemeId(),
                provisional.darkThemeId(),
                provisional.themes(),
                new FlutterGeneratedThemeArtifact(
                        FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH,
                        FlutterProjectThemeDigests.sha256(dart)));
        Files.write(descriptor, new FlutterProjectThemeCodec().encode(disabled));
        Files.write(dartFile, dart);

        FlutterProjectThemeLoadResult result = store.load(root);

        assertEquals(FlutterProjectThemeLoadStatus.VALID, result.status());
        assertEquals(disabled, result.theme().orElseThrow());
    }
}

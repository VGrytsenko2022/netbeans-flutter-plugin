package dev.flutter.netbeans.plugin.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterToolchainServiceTest {
    @TempDir
    Path temporaryDirectory;
    private Preferences preferences;
    private FlutterToolchainService service;

    @BeforeEach
    void setUp() {
        preferences = Preferences.userRoot().node(
                "/dev/flutter/netbeans/tests/" + UUID.randomUUID());
        service = new FlutterToolchainService(
                new FlutterSettings(preferences),
                new dev.flutter.netbeans.sdk.FlutterSdkLocator(),
                new dev.flutter.netbeans.sdk.DartSdkLocator());
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    void resolvesSavedFlutterAndItsBundledDartSdk() throws IOException {
        Path flutterHome = temporaryDirectory.resolve("flutter");
        createFile(flutterHome.resolve("bin"), isWindows() ? "flutter.bat" : "flutter");
        Path dartHome = flutterHome.resolve("bin").resolve("cache").resolve("dart-sdk");
        createFile(dartHome.resolve("bin"), isWindows() ? "dart.exe" : "dart");

        FlutterToolchainStatus status = service.resolve(
                new FlutterToolchainConfig(flutterHome.toString(), true, ""));

        assertTrue(status.isReady());
        assertTrue(status.validForSave());
        assertEquals(flutterHome.toAbsolutePath().normalize(), status.flutterSdk().orElseThrow().home());
        assertEquals(dartHome.toAbsolutePath().normalize(), status.dartSdk().orElseThrow().home());
    }

    @Test
    void reportsAnExplicitInvalidFlutterPathWithoutFallingBackToEnvironment() {
        Path missingHome = temporaryDirectory.resolve("missing-flutter");

        FlutterToolchainStatus status = service.resolve(
                new FlutterToolchainConfig(missingHome.toString(), true, ""));

        assertTrue(status.flutterSdk().isEmpty());
        assertFalse(status.validForSave());
        assertTrue(status.flutterMessage().contains(missingHome.toString()));
        assertTrue(status.flutterMessage().contains("was not found"));
    }

    private static void createFile(Path directory, String name) throws IOException {
        Files.createDirectories(directory);
        Path executable = Files.createFile(directory.resolve(name));
        if (!isWindows()) executable.toFile().setExecutable(true);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}

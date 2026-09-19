package io.github.vgrytsenko2022.plugin.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.sdk.DartSdkLocator;
import io.github.vgrytsenko2022.sdk.FlutterSdkLocator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterSdkAutoDiscoveryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsFlutterAndBundledDartOnlyOnFirstPluginStart()
            throws IOException, BackingStoreException {
        Path firstFlutter = createFlutterSdk(temporaryDirectory.resolve("flutter-one"));
        Path secondFlutter = createFlutterSdk(temporaryDirectory.resolve("flutter-two"));
        Preferences preferences = Preferences.userRoot().node(
                "/io/github/vgrytsenko2022/tests/" + UUID.randomUUID());
        FlutterSettings settings = new FlutterSettings(preferences);
        String previousFlutterProperty = System.getProperty("flutter.sdk");

        synchronized (FlutterSdkAutoDiscoveryTest.class) {
            try {
                System.setProperty("flutter.sdk", firstFlutter.toString());
                FlutterSdkAutoDiscovery.initialize(
                        settings,
                        new FlutterSdkLocator(),
                        new DartSdkLocator());

                FlutterToolchainConfig imported = settings.load();
                assertEquals(firstFlutter.toAbsolutePath().normalize().toString(), imported.flutterHome());
                assertTrue(imported.useBundledDart());
                assertTrue(imported.dartHome().isBlank(),
                        "bundled Dart is derived from Flutter; custom Dart must remain separate");
                assertEquals(FlutterSdkAutoDiscovery.DISCOVERY_VERSION, settings.discoveryVersion());

                System.setProperty("flutter.sdk", secondFlutter.toString());
                FlutterSdkAutoDiscovery.initialize(
                        settings,
                        new FlutterSdkLocator(),
                        new DartSdkLocator());

                assertEquals(imported, settings.load(), "first-start discovery must preserve saved settings");
            } finally {
                restoreProperty("flutter.sdk", previousFlutterProperty);
                preferences.removeNode();
            }
        }
    }

    @Test
    void preservesExplicitAutomaticStandaloneDartMode()
            throws IOException, BackingStoreException {
        Path flutterHome = createFlutterSdk(temporaryDirectory.resolve("flutter-explicit-mode"));
        Preferences preferences = Preferences.userRoot().node(
                "/io/github/vgrytsenko2022/tests/" + UUID.randomUUID());
        FlutterSettings settings = new FlutterSettings(preferences);
        settings.save(new FlutterToolchainConfig("", false, ""));
        String previousFlutterProperty = System.getProperty("flutter.sdk");

        synchronized (FlutterSdkAutoDiscoveryTest.class) {
            try {
                System.setProperty("flutter.sdk", flutterHome.toString());
                FlutterSdkAutoDiscovery.initialize(
                        settings,
                        new FlutterSdkLocator(),
                        new DartSdkLocator());

                FlutterToolchainConfig imported = settings.load();
                assertEquals(flutterHome.toAbsolutePath().normalize().toString(), imported.flutterHome());
                assertFalse(imported.useBundledDart());
                assertTrue(imported.dartHome().isBlank());
            } finally {
                restoreProperty("flutter.sdk", previousFlutterProperty);
                preferences.removeNode();
            }
        }
    }

    private static Path createFlutterSdk(Path home) throws IOException {
        createFile(home.resolve("bin"), isWindows() ? "flutter.bat" : "flutter");
        createFile(bundledDartHome(home).resolve("bin"), isWindows() ? "dart.exe" : "dart");
        return home;
    }

    private static Path bundledDartHome(Path flutterHome) {
        return flutterHome.resolve("bin").resolve("cache").resolve("dart-sdk");
    }

    private static void createFile(Path directory, String name) throws IOException {
        Files.createDirectories(directory);
        Path executable = Files.createFile(directory.resolve(name));
        if (!isWindows()) executable.toFile().setExecutable(true);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

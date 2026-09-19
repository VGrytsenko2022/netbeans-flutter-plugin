package io.github.vgrytsenko2022.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SdkLocatorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesAndNormalizesFlutterSdkHome() throws IOException {
        Path flutterHome = temporaryDirectory.resolve("flutter");
        Path executable = createExecutable(
                flutterHome.resolve("bin"),
                FlutterSdkLocator.isWindows() ? "flutter.bat" : "flutter");

        Path nonNormalizedHome = flutterHome.resolve("child").resolve("..");
        var sdk = new FlutterSdkLocator().fromHome(nonNormalizedHome);

        assertTrue(sdk.isPresent());
        assertEquals(flutterHome.toAbsolutePath().normalize(), sdk.orElseThrow().home());
        assertEquals(executable.toAbsolutePath().normalize(), sdk.orElseThrow().flutterExecutable());
    }

    @Test
    void resolvesDartSdkBundledWithFlutter() throws IOException {
        Path flutterHome = temporaryDirectory.resolve("flutter");
        Path flutterExecutable = createExecutable(
                flutterHome.resolve("bin"),
                FlutterSdkLocator.isWindows() ? "flutter.bat" : "flutter");
        Path dartHome = flutterHome.resolve("bin").resolve("cache").resolve("dart-sdk");
        Path dartExecutable = createExecutable(
                dartHome.resolve("bin"),
                FlutterSdkLocator.isWindows() ? "dart.exe" : "dart");

        var flutterSdk = new io.github.vgrytsenko2022.api.FlutterSdk(flutterHome, flutterExecutable);
        var dartSdk = new DartSdkLocator().fromFlutterSdk(flutterSdk);

        assertTrue(dartSdk.isPresent());
        assertEquals(dartHome.toAbsolutePath().normalize(), dartSdk.orElseThrow().home());
        assertEquals(dartExecutable.toAbsolutePath().normalize(), dartSdk.orElseThrow().dartExecutable());
    }

    @Test
    void rejectsFoldersWithoutSdkExecutables() {
        assertTrue(new FlutterSdkLocator().fromHome(temporaryDirectory.resolve("missing-flutter")).isEmpty());
        assertTrue(new DartSdkLocator().fromHome(temporaryDirectory.resolve("missing-dart")).isEmpty());
    }

    @Test
    void acceptsQuotedSdkSystemProperties() throws IOException {
        Path flutterHome = temporaryDirectory.resolve("flutter sdk");
        createExecutable(flutterHome.resolve("bin"),
                FlutterSdkLocator.isWindows() ? "flutter.bat" : "flutter");
        Path dartHome = temporaryDirectory.resolve("dart sdk");
        createExecutable(dartHome.resolve("bin"),
                FlutterSdkLocator.isWindows() ? "dart.exe" : "dart");
        String previousFlutter = System.getProperty("flutter.sdk");
        String previousDart = System.getProperty("dart.sdk");

        synchronized (SdkLocatorTest.class) {
            try {
                System.setProperty("flutter.sdk", '"' + flutterHome.toString() + '"');
                System.setProperty("dart.sdk", '"' + dartHome.toString() + '"');

                assertEquals(flutterHome.toAbsolutePath().normalize(),
                        new FlutterSdkLocator().detect().orElseThrow().sdk().home());
                assertEquals(dartHome.toAbsolutePath().normalize(),
                        new DartSdkLocator().detectStandalone().orElseThrow().sdk().home());
            } finally {
                restoreProperty("flutter.sdk", previousFlutter);
                restoreProperty("dart.sdk", previousDart);
            }
        }
    }

    private static Path createExecutable(Path directory, String name) throws IOException {
        Files.createDirectories(directory);
        Path executable = Files.createFile(directory.resolve(name));
        if (!FlutterSdkLocator.isWindows()) executable.toFile().setExecutable(true);
        return executable;
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

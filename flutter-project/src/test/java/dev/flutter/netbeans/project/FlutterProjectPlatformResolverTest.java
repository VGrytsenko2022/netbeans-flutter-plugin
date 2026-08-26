package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterDevice;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectPlatformResolverTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsOnlyGeneratedPlatformDirectories() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("android"));
        Files.createDirectories(temporaryDirectory.resolve("windows"));
        Files.writeString(temporaryDirectory.resolve("web"), "not a platform directory");

        assertEquals(
                Set.of(FlutterProjectPlatform.ANDROID, FlutterProjectPlatform.WINDOWS),
                FlutterProjectPlatformResolver.configuredPlatforms(temporaryDirectory));
    }

    @Test
    void mapsFlutterTargetsToTheirRequiredProjectPlatform() {
        assertPlatform(FlutterProjectPlatform.ANDROID, "emulator-5554", "android-arm64");
        assertPlatform(FlutterProjectPlatform.IOS, "iphone", "ios");
        assertPlatform(FlutterProjectPlatform.WEB, "chrome", "unknown");
        assertPlatform(FlutterProjectPlatform.WEB, "web-server", "web-javascript");
        assertPlatform(FlutterProjectPlatform.WINDOWS, "windows", "windows-x64");
        assertPlatform(FlutterProjectPlatform.WINDOWS, "windows", "unknown");
        assertPlatform(FlutterProjectPlatform.LINUX, "linux", "linux-x64");
        assertPlatform(FlutterProjectPlatform.MACOS, "macos", "darwin-arm64");
        assertTrue(FlutterProjectPlatformResolver.platformFor(
                device("fuchsia", "fuchsia-arm64")).isEmpty());
        assertTrue(FlutterProjectPlatformResolver.platformFor(
                device("chrome", "android-arm64"))
                .filter(platform -> platform == FlutterProjectPlatform.ANDROID)
                .isPresent());
        assertTrue(FlutterProjectPlatformResolver.platformFor(
                device("windows", "fuchsia-arm64")).isEmpty());
    }

    @Test
    void supportsOnlyTargetsWhosePlatformDirectoryExists() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("android"));

        assertTrue(FlutterProjectPlatformResolver.supports(
                temporaryDirectory, device("pixel", "android-arm64")));
        assertFalse(FlutterProjectPlatformResolver.supports(
                temporaryDirectory, device("windows", "windows-x64")));
        assertFalse(FlutterProjectPlatformResolver.supports(
                temporaryDirectory, device("fuchsia", "fuchsia-arm64")));
    }

    private static void assertPlatform(
            FlutterProjectPlatform expected,
            String id,
            String targetPlatform) {
        assertEquals(expected, FlutterProjectPlatformResolver.platformFor(
                device(id, targetPlatform)).orElseThrow());
    }

    private static FlutterDevice device(String id, String targetPlatform) {
        return new FlutterDevice(id, id, targetPlatform, false);
    }
}

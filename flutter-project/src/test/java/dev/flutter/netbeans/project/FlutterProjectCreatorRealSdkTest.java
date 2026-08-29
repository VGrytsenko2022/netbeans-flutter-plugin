package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.sdk.FlutterCli;
import dev.flutter.netbeans.sdk.FlutterSdkLocator;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeLoadStatus;
import dev.flutter.netbeans.project.theme.FlutterProjectThemePaths;
import dev.flutter.netbeans.project.theme.FlutterProjectThemeStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Optional smoke test. Run with -Dflutter.it.sdk=/path/to/flutter. */
class FlutterProjectCreatorRealSdkTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @EnabledIfSystemProperty(named = "flutter.it.sdk", matches = ".+")
    void createsBaseApplicationWithRealFlutterSdk() throws Exception {
        Path sdkHome = Path.of(System.getProperty("flutter.it.sdk"));
        var sdk = new FlutterSdkLocator().fromHome(sdkHome).orElseThrow();
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "netbeans_flutter_smoke",
                "dev.flutter.netbeans",
                "NetBeans Flutter plugin smoke test.",
                Set.of(
                        FlutterProjectPlatform.ANDROID,
                        FlutterProjectPlatform.WEB));

        var cli = new FlutterCli(sdk);
        var created = new FlutterProjectCreator(cli).create(request);

        assertEquals("netbeans_flutter_smoke", created.name());
        assertTrue(Files.isRegularFile(created.root().resolve("lib/main.dart")));
        assertTrue(Files.isRegularFile(created.root().resolve("pubspec.yaml")));
        assertTrue(Files.isDirectory(created.root().resolve("android")));
        assertTrue(Files.isDirectory(created.root().resolve("web")));
        assertTrue(Files.notExists(created.root().resolve("windows")));
        assertTrue(Files.isRegularFile(created.root().resolve(
                FlutterProjectThemePaths.DESCRIPTOR_PATH)));
        assertTrue(Files.isRegularFile(created.root().resolve(
                FlutterProjectThemePaths.GENERATED_DART_PATH)));
        assertEquals(FlutterProjectThemeLoadStatus.VALID,
                new FlutterProjectThemeStore().load(created.root()).status());
        String main = Files.readString(created.root().resolve("lib/main.dart"));
        assertTrue(main.contains("import 'theme/app_theme.dart';"));
        assertTrue(main.contains("theme: AppTheme.light"));
        assertTrue(main.contains("darkTheme: AppTheme.dark"));
        assertTrue(main.contains("themeMode: AppTheme.mode"));

        var analyze = cli.execute(created.root(), Duration.ofMinutes(5), "analyze");
        assertTrue(analyze.success(), () -> "flutter analyze failed:\n"
                + analyze.stdout() + "\n" + analyze.stderr());
        var test = cli.execute(created.root(), Duration.ofMinutes(5), "test");
        assertTrue(test.success(), () -> "flutter test failed:\n"
                + test.stdout() + "\n" + test.stderr());
    }
}

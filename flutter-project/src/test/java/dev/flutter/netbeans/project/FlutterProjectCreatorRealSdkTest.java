package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.sdk.FlutterCli;
import dev.flutter.netbeans.sdk.FlutterSdkLocator;
import java.nio.file.Files;
import java.nio.file.Path;
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
                "NetBeans Flutter plugin smoke test.");

        var created = new FlutterProjectCreator(new FlutterCli(sdk)).create(request);

        assertEquals("netbeans_flutter_smoke", created.name());
        assertTrue(Files.isRegularFile(created.root().resolve("lib/main.dart")));
        assertTrue(Files.isRegularFile(created.root().resolve("pubspec.yaml")));
    }
}

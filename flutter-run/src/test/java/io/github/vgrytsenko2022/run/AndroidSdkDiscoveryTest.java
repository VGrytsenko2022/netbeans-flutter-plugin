package io.github.vgrytsenko2022.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.api.ProcessResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AndroidSdkDiscoveryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void inspectsAllToolsAndNormalizesRoot() throws Exception {
        Path sdk = completeSdk(temporaryDirectory.resolve("sdk"), "latest");

        AndroidSdkInstallation installation = new AndroidSdkDiscovery()
                .inspect(sdk.resolve("."), "manual test")
                .orElseThrow();

        assertEquals(sdk.toAbsolutePath().normalize(), installation.root());
        assertEquals("manual test", installation.source());
        assertTrue(installation.complete());
        assertEquals(sdk.resolve("cmdline-tools/latest/bin")
                        .resolve(AndroidSdkDiscovery.executableName("sdkmanager"))
                        .toAbsolutePath().normalize(),
                installation.sdkManager().orElseThrow());
    }

    @Test
    void propertyPrecedesAndroidEnvironment() throws Exception {
        Path propertySdk = completeSdk(temporaryDirectory.resolve("property-sdk"), "latest");
        Path environmentSdk = completeSdk(temporaryDirectory.resolve("environment-sdk"), "latest");
        Properties properties = new Properties();
        properties.setProperty("android.sdk", "\"" + propertySdk + "\"");
        AndroidSdkDiscovery discovery = new AndroidSdkDiscovery(
                Map.of("ANDROID_SDK_ROOT", environmentSdk.toString()), properties, null);

        AndroidSdkInstallation detected = discovery.detect().orElseThrow();

        assertEquals(propertySdk.toAbsolutePath().normalize(), detected.root());
        assertEquals("JVM property android.sdk", detected.source());
    }

    @Test
    void skipsInvalidPropertyAndUsesAndroidHome() throws Exception {
        Path sdk = completeSdk(temporaryDirectory.resolve("android-home"), "latest");
        Properties properties = new Properties();
        properties.setProperty("android.sdk", temporaryDirectory.resolve("missing").toString());
        AndroidSdkDiscovery discovery = new AndroidSdkDiscovery(
                Map.of("ANDROID_HOME", sdk.toString()), properties, null);

        AndroidSdkInstallation detected = discovery.detect().orElseThrow();

        assertEquals(sdk.toAbsolutePath().normalize(), detected.root());
        assertEquals("ANDROID_HOME", detected.source());
    }

    @Test
    void readsFlutterMachineConfigurationAsFallback() throws Exception {
        Path sdk = completeSdk(temporaryDirectory.resolve("flutter-sdk-config"), "latest");
        AndroidSdkDiscovery discovery = new AndroidSdkDiscovery(
                Map.of(), new Properties(), (workingDirectory, timeout, arguments) -> {
                    assertEquals(Duration.ofSeconds(30), timeout);
                    assertEquals(java.util.List.of("config", "--machine"),
                            java.util.List.of(arguments));
                    return new ProcessResult(0,
                            "{\"android-sdk\":\"" + escapeJson(sdk.toString()) + "\"}", "");
                });

        AndroidSdkInstallation detected = discovery.detect().orElseThrow();

        assertEquals(sdk.toAbsolutePath().normalize(), detected.root());
        assertEquals("Flutter config android-sdk", detected.source());
    }

    @Test
    void ignoresMalformedOrFailedFlutterConfiguration() throws Exception {
        AndroidSdkDiscovery malformed = new AndroidSdkDiscovery(
                Map.of(), new Properties(), (directory, timeout, arguments) ->
                        new ProcessResult(0, "not json", ""));
        AndroidSdkDiscovery failed = new AndroidSdkDiscovery(
                Map.of(), new Properties(), (directory, timeout, arguments) ->
                        new ProcessResult(1, "", "Flutter config failed"));

        assertTrue(malformed.detect().isEmpty());
        assertTrue(failed.detect().isEmpty());
    }

    @Test
    void findsPlatformDefaultAfterFlutterFallback() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user.home", temporaryDirectory.resolve("home").toString());
        Map<String, String> environment;
        Path sdk;
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) {
            Path localAppData = temporaryDirectory.resolve("local-app-data");
            sdk = completeSdk(localAppData.resolve("Android/Sdk"), "latest");
            environment = Map.of("LOCALAPPDATA", localAppData.toString());
        } else if (os.contains("mac")) {
            sdk = completeSdk(temporaryDirectory.resolve("home/Library/Android/sdk"), "latest");
            environment = Map.of();
        } else {
            sdk = completeSdk(temporaryDirectory.resolve("home/Android/Sdk"), "latest");
            environment = Map.of();
        }
        AndroidSdkDiscovery discovery = new AndroidSdkDiscovery(
                environment, properties, (directory, timeout, arguments) ->
                        new ProcessResult(0, "{}", ""));

        AndroidSdkInstallation detected = discovery.detect().orElseThrow();

        assertEquals(sdk.toAbsolutePath().normalize(), detected.root());
        assertTrue(detected.source().contains("default Android SDK path"));
    }

    @Test
    void prefersLatestThenHighestVersionedCommandLineTools() throws Exception {
        Path sdk = temporaryDirectory.resolve("versioned-sdk");
        createTool(sdk, "cmdline-tools/9.0/bin", "sdkmanager");
        createTool(sdk, "cmdline-tools/12.0/bin", "sdkmanager");
        Files.createDirectories(sdk.resolve("platforms"));

        AndroidSdkInstallation versioned = new AndroidSdkDiscovery()
                .detect(sdk).orElseThrow();

        assertTrue(versioned.sdkManager().orElseThrow().toString()
                .replace('\\', '/').contains("cmdline-tools/12.0/bin"));

        createTool(sdk, "cmdline-tools/latest/bin", "sdkmanager");
        AndroidSdkInstallation latest = new AndroidSdkDiscovery().detect(sdk).orElseThrow();
        assertTrue(latest.sdkManager().orElseThrow().toString()
                .replace('\\', '/').contains("cmdline-tools/latest/bin"));
    }

    @Test
    void reportsIncompleteSdkButRejectsUnrelatedDirectory() throws Exception {
        Path incomplete = temporaryDirectory.resolve("incomplete");
        Files.createDirectories(incomplete.resolve("platforms"));
        Path unrelated = temporaryDirectory.resolve("unrelated");
        Files.createDirectories(unrelated);

        AndroidSdkInstallation installation = new AndroidSdkDiscovery()
                .detect(incomplete).orElseThrow();

        assertFalse(installation.complete());
        assertEquals(java.util.List.of(AndroidSdkTool.SDK_MANAGER, AndroidSdkTool.AVD_MANAGER,
                AndroidSdkTool.EMULATOR, AndroidSdkTool.ADB), installation.missingTools());
        assertTrue(new AndroidSdkDiscovery().detect(unrelated).isEmpty());
    }

    private static Path completeSdk(Path sdk, String commandLineVersion) throws Exception {
        createTool(sdk, "cmdline-tools/" + commandLineVersion + "/bin", "sdkmanager");
        createTool(sdk, "cmdline-tools/" + commandLineVersion + "/bin", "avdmanager");
        createTool(sdk, "emulator", "emulator");
        createTool(sdk, "platform-tools", "adb");
        return sdk;
    }

    private static void createTool(Path sdk, String directory, String command) throws Exception {
        Path folder = sdk.resolve(directory);
        Files.createDirectories(folder);
        Path executable = folder.resolve(AndroidSdkDiscovery.executableName(command));
        Files.writeString(executable, "test");
        assertTrue(executable.toFile().setExecutable(true) || executable.toFile().canExecute());
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

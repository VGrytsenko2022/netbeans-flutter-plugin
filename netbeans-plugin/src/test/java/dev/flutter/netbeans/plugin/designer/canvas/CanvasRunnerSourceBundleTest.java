package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasPlatform;
import dev.flutter.netbeans.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.HashSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CanvasRunnerSourceBundleTest {
    @TempDir
    Path temporary;

    @Test
    void packagedSourcesExtractOnceAndAreVerifiedBeforeReuse() throws Exception {
        CanvasRunnerSourceBundle bundle = CanvasRunnerSourceBundle.packaged();
        FlutterSdk sdk = sdk("engine-a");
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                bundle, sdk, windowsContract());

        Path first = bundle.extract(temporary.resolve("cache"), identity);
        Path second = bundle.extract(temporary.resolve("cache"), identity);

        assertEquals(first, second);
        assertTrue(Files.readString(first.resolve("lib/main.dart"))
                .contains("Native Flutter Canvas"));
        assertTrue(Files.isRegularFile(first.resolve("lib/main_web.dart")));
        assertTrue(Files.isRegularFile(
                first.resolve("lib/src/canvas_web_transport.dart")));
        assertTrue(Files.isRegularFile(
                first.resolve("lib/src/canvas_web_transport_core.dart")));
        assertTrue(Files.isRegularFile(first.resolve("web/index.html")));
        assertTrue(Files.isRegularFile(first.resolve("web/canvas.css")));
        assertTrue(Files.isRegularFile(first.resolve("web/canvas_bridge.js")));
        assertTrue(Files.isRegularFile(first.resolve("web/flutter_bootstrap.js")));
        assertTrue(Files.isRegularFile(
                first.resolve("assets/fonts/Roboto-Regular.ttf")));
        assertTrue(Files.isRegularFile(
                first.resolve("assets/licenses/Roboto-LICENSE.txt")));
        assertTrue(Files.isRegularFile(first.resolve("windows/runner/main.cpp")));
    }

    @Test
    void modifiedCachedSourceIsNeverReused() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of("lib/main.dart", "void main() {}"));
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-a"), windowsContract());
        Path extracted = bundle.extract(temporary.resolve("cache"), identity);
        Files.writeString(extracted.resolve("lib/main.dart"), "tampered");

        IOException error = assertThrows(IOException.class,
                () -> bundle.extract(temporary.resolve("cache"), identity));

        assertTrue(error.getMessage().contains("missing or modified"));
    }

    @Test
    void traversalAndOversizedEntriesAreRejectedBeforeExtraction() {
        String digest = sha256("x".getBytes(StandardCharsets.UTF_8));
        CanvasRunnerSourceBundle.ResourceAccess traversal = resources(
                "../outside|1|" + digest + "\n", Map.of("../outside", "x"));
        assertThrows(IOException.class, () -> new CanvasRunnerSourceBundle(traversal, "test"));

        CanvasRunnerSourceBundle.ResourceAccess oversized = resources(
                "large|" + (CanvasRunnerSourceBundle.MAX_FILE_BYTES + 1) + "|" + digest + "\n",
                Map.of("large", "x"));
        assertThrows(IOException.class, () -> new CanvasRunnerSourceBundle(oversized, "test"));
    }

    @Test
    void engineRevisionAndSdkLocationArePartOfCacheIdentity() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of("pubspec.yaml", "name: runner\n"));

        CanvasRunnerCacheIdentity first = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-a"), windowsContract());
        CanvasRunnerCacheIdentity second = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-b"), windowsContract());

        assertNotEquals(first.cacheKey(), second.cacheKey());
        assertEquals("engine-a", first.engineRevision());
    }

    @Test
    void completeRunnerContractFingerprintIsPartOfCacheIdentity() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of("pubspec.yaml", "name: runner\n"));
        FlutterSdk sdk = sdk("engine-profile");

        NativeCanvasRunnerContract contract = windowsContract();
        CanvasRunnerCacheIdentity release = CanvasRunnerCacheIdentity.create(
                bundle, sdk, contract);
        CanvasRunnerCacheIdentity explicitRelease = CanvasRunnerCacheIdentity.create(
                bundle, sdk, contract.fingerprint());
        CanvasRunnerCacheIdentity debug = CanvasRunnerCacheIdentity.create(
                bundle, sdk, "c".repeat(64));

        assertEquals(release.cacheKey(), explicitRelease.cacheKey());
        assertNotEquals(release.cacheKey(), debug.cacheKey());
        assertEquals(contract.fingerprint(), release.runnerContractIdentity());
    }

    @Test
    void samePolicyIdentityCannotReuseAnotherPlatformTargetOrRuntimeLayout()
            throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of(
                "pubspec.yaml", "name: runner\n"));
        FlutterSdk sdk = sdk("engine-contract");
        NativeCanvasRunnerContract windows = windowsContract();
        NativeCanvasRunnerContract.BuildTarget debugTarget =
                new NativeCanvasRunnerContract.BuildTarget(
                        windows.buildTarget().flutterTarget(),
                        "debug",
                        windows.buildTarget().buildOptions(),
                        windows.buildTarget().outputDirectory(),
                        windows.buildTarget().executableName(),
                        windows.buildTarget().executableSearchDepth());
        NativeCanvasRunnerContract debug = new NativeCanvasRunnerContract(
                windows.platform(), debugTarget, windows.runtimeLayout(),
                windows.cachePolicy());
        HashSet<String> allowed = new HashSet<>(
                windows.runtimeLayout().allowedFiles());
        allowed.add("optional.dat");
        NativeCanvasRunnerContract layout = new NativeCanvasRunnerContract(
                windows.platform(),
                windows.buildTarget(),
                new NativeCanvasRunnerContract.RuntimeLayout(
                        windows.runtimeLayout().requiredFiles(),
                        allowed,
                        windows.runtimeLayout().allowedFilePrefixes(),
                        windows.runtimeLayout().allowedDirectories(),
                        windows.runtimeLayout().allowedDirectoryPrefixes(),
                        windows.runtimeLayout().ignoredFiles()),
                windows.cachePolicy());
        NativeCanvasRunnerContract linux = new NativeCanvasRunnerContract(
                NativeCanvasPlatform.LINUX,
                windows.buildTarget(),
                windows.runtimeLayout(),
                windows.cachePolicy());

        CanvasRunnerCacheIdentity baseline = CanvasRunnerCacheIdentity.create(
                bundle, sdk, windows);
        CanvasRunnerCacheIdentity targetIdentity = CanvasRunnerCacheIdentity.create(
                bundle, sdk, debug);
        CanvasRunnerCacheIdentity layoutIdentity = CanvasRunnerCacheIdentity.create(
                bundle, sdk, layout);
        CanvasRunnerCacheIdentity platformIdentity = CanvasRunnerCacheIdentity.create(
                bundle, sdk, linux);

        assertNotEquals(baseline.cacheKey(), targetIdentity.cacheKey());
        assertNotEquals(baseline.cacheKey(), layoutIdentity.cacheKey());
        assertNotEquals(baseline.cacheKey(), platformIdentity.cacheKey());
    }

    @Test
    void cachedSourceRejectsLinkedPackagedAncestor() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of(
                "lib/main.dart", "void main() {}"));
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-link-source"), windowsContract());
        Path extracted = bundle.extract(temporary.resolve("cache"), identity);
        Path outside = Files.createDirectories(temporary.resolve("outside-lib"));
        Files.writeString(outside.resolve("main.dart"), "void main() {}");
        Files.delete(extracted.resolve("lib/main.dart"));
        Files.delete(extracted.resolve("lib"));
        createDirectorySymlinkOrSkip(extracted.resolve("lib"), outside);

        IOException error = assertThrows(IOException.class,
                () -> bundle.extract(temporary.resolve("cache"), identity));

        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("junction"));
    }

    @Test
    void cachedSourceRejectsLinkedGeneratedBuildAncestor() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of(
                "lib/main.dart", "void main() {}"));
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-link-build"), windowsContract());
        Path extracted = bundle.extract(temporary.resolve("cache"), identity);
        Path outside = Files.createDirectories(temporary.resolve("outside-build"));
        createDirectorySymlinkOrSkip(extracted.resolve("build"), outside);

        IOException error = assertThrows(IOException.class,
                () -> bundle.extract(temporary.resolve("cache"), identity));

        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("junction"));
    }

    @Test
    void cachedSourceRejectsLinkedGeneratedFile() throws Exception {
        CanvasRunnerSourceBundle bundle = bundle(Map.of(
                "lib/main.dart", "void main() {}"));
        CanvasRunnerCacheIdentity identity = CanvasRunnerCacheIdentity.create(
                bundle, sdk("engine-link-generated-file"), windowsContract());
        Path extracted = bundle.extract(temporary.resolve("cache"), identity);
        Path outside = temporary.resolve("outside-generated-file");
        Files.writeString(outside, "protected\n");
        try {
            Files.createSymbolicLink(
                    extracted.resolve(".flutter-plugins-dependencies"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "file symbolic links are unavailable: " + exception.getMessage());
        }

        IOException error = assertThrows(IOException.class,
                () -> bundle.extract(temporary.resolve("cache"), identity));

        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("junction"));
    }

    private static void createDirectorySymlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "directory symbolic links are unavailable: " + exception.getMessage());
        }
    }

    private FlutterSdk sdk(String engineRevision) throws IOException {
        Path sdk = temporary.resolve("sdk-" + engineRevision);
        Path executable = sdk.resolve("bin/flutter.bat");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable, "@echo off\n");
        Path engine = sdk.resolve("bin/internal/engine.version");
        Files.createDirectories(engine.getParent());
        Files.writeString(engine, engineRevision + "\n");
        Files.writeString(sdk.resolve("version"), "3.test\n");
        return new FlutterSdk(sdk, executable);
    }

    private static CanvasRunnerSourceBundle bundle(Map<String, String> sources) throws IOException {
        StringBuilder manifest = new StringBuilder();
        sources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            manifest.append(entry.getKey()).append('|').append(bytes.length).append('|')
                    .append(sha256(bytes)).append('\n');
        });
        return new CanvasRunnerSourceBundle(resources(manifest.toString(), sources), "test");
    }

    private static CanvasRunnerSourceBundle.ResourceAccess resources(
            String manifest,
            Map<String, String> sources) {
        return new CanvasRunnerSourceBundle.ResourceAccess() {
            @Override
            public InputStream openManifest() {
                return new ByteArrayInputStream(manifest.getBytes(StandardCharsets.US_ASCII));
            }

            @Override
            public InputStream openSource(String relativePath) throws IOException {
                String content = sources.get(relativePath);
                if (content == null) {
                    throw new IOException("missing test resource");
                }
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static NativeCanvasRunnerContract windowsContract() {
        return new WindowsNativeCanvasPlatformProvider().runnerContract().orElseThrow();
    }
}

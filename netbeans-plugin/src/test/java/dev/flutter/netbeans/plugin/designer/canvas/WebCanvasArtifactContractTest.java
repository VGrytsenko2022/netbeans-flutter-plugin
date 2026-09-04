package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.canvas.runner.CanvasRunnerBundle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebCanvasArtifactContractTest {

    @Test
    void stableFileIdentityRejectsOneSidedUnavailableKeys() {
        Object key = new Object();

        assertTrue(WebCanvasArtifactContract.sameFileKey(null, null));
        assertTrue(WebCanvasArtifactContract.sameFileKey(key, key));
        assertFalse(WebCanvasArtifactContract.sameFileKey(null, key));
        assertFalse(WebCanvasArtifactContract.sameFileKey(key, null));
        assertFalse(WebCanvasArtifactContract.sameFileKey(key, new Object()));
    }

    @Test
    void defaultLimitsMatchTheNativeImmutableSnapshotBoundary() {
        WebCanvasArtifactContract.Limits limits =
                WebCanvasArtifactContract.Limits.defaults();

        assertEquals(WebCanvasArtifactContract.MAX_SNAPSHOT_FILES,
                limits.maxFiles());
        assertEquals(WebCanvasArtifactContract.MAX_SNAPSHOT_FILE_BYTES,
                limits.maxFileBytes());
        assertEquals(WebCanvasArtifactContract.MAX_SNAPSHOT_TOTAL_BYTES,
                limits.maxTotalBytes());
    }
    private static final Set<String> NON_EMPTY_FILES = Set.of(
            ".last_build_id",
            "canvas_bridge.js",
            "canvas.css",
            "flutter_bootstrap.js",
            "flutter.js",
            "index.html",
            "main.dart.js",
            "version.json",
            "assets/AssetManifest.bin",
            "assets/AssetManifest.bin.json",
            "assets/assets/fonts/Roboto-Regular.ttf",
            "assets/assets/licenses/Roboto-LICENSE.txt",
            "assets/FontManifest.json",
            "assets/NOTICES",
            "assets/fonts/MaterialIcons-Regular.otf",
            "assets/shaders/ink_sparkle.frag",
            "assets/shaders/stretch_effect.frag",
            "canvaskit/canvaskit.js",
            "canvaskit/canvaskit.js.symbols",
            "canvaskit/canvaskit.wasm",
            "canvaskit/skwasm_heavy.js",
            "canvaskit/skwasm_heavy.js.symbols",
            "canvaskit/skwasm_heavy.wasm",
            "canvaskit/skwasm.js",
            "canvaskit/skwasm.js.symbols",
            "canvaskit/skwasm.wasm",
            "canvaskit/wimp.js",
            "canvaskit/wimp.js.symbols",
            "canvaskit/wimp.wasm",
            "canvaskit/chromium/canvaskit.js",
            "canvaskit/chromium/canvaskit.js.symbols",
            "canvaskit/chromium/canvaskit.wasm",
            "canvaskit/experimental_webparagraph/canvaskit.js",
            "canvaskit/experimental_webparagraph/canvaskit.js.symbols",
            "canvaskit/experimental_webparagraph/canvaskit.wasm");
    private static final String EMPTY_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @TempDir
    Path temporary;

    @Test
    void packagedManifestPinsFlutterEngineAndEveryAllowedArtifact() throws Exception {
        WebCanvasArtifactContract.ExpectedArtifact expected;
        try (var input = CanvasRunnerBundle.openWebArtifactManifest()) {
            expected = WebCanvasArtifactContract.readExpectedArtifact(input);
        }

        assertEquals("0cd610717bde95fd88343c64f81c11ba4e5c0010",
                expected.engineRevision());
        assertEquals(35, expected.files().size());
        assertEquals(2_836_718L,
                expected.files().get("main.dart.js").size());
        assertEquals("ff2bd930d8948511e0211f96c2eb191890c1090ef0fa86011bd04cad78e5e57a",
                expected.files().get("main.dart.js").sha256());
    }

    @Test
    void acceptsClosedOfflineArtifactAndReturnsSortedStableDigest() throws Exception {
        Path firstRoot = createArtifact("first");
        Path secondRoot = createArtifact("second");
        WebCanvasArtifactContract contract = testContract(firstRoot);

        WebCanvasArtifactContract.ArtifactSnapshot first = contract.validate(firstRoot);
        WebCanvasArtifactContract.ArtifactSnapshot repeated = contract.validate(firstRoot);
        WebCanvasArtifactContract.ArtifactSnapshot second = contract.validate(secondRoot);

        assertEquals(first, repeated);
        assertEquals(first.sha256(), second.sha256());
        assertEquals(64, first.sha256().length());
        assertEquals(Set.of(".last_build_id"), first.excludedBuildMetadata().keySet());
        assertFalse(first.files().containsKey(".last_build_id"));
        assertEquals(EMPTY_SHA256,
                first.files().get("flutter_service_worker.js").sha256());
        List<String> keys = new ArrayList<>(first.files().keySet());
        List<String> sorted = keys.stream().sorted().toList();
        assertEquals(sorted, keys);
        assertThrows(UnsupportedOperationException.class,
                () -> first.files().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> first.excludedBuildMetadata().clear());
        assertTrue(contract.fingerprint().contains("flutter-3.44.8"));
    }

    @Test
    void rejectsMalformedPathDependentBuildMetadata() throws Exception {
        Path sourceRoot = createArtifact("invalid-build-metadata");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        Files.writeString(
                webRoot(sourceRoot).resolve(".last_build_id"),
                "not-a-flutter-build-id",
                StandardCharsets.US_ASCII);

        IOException failure = assertThrows(
                IOException.class,
                () -> contract.validate(sourceRoot));

        assertTrue(failure.getMessage().contains("32 lowercase hex digits"));
    }

    @Test
    void sameSizeExecutableTamperFailsPinnedValidationAndUnchangedVerification()
            throws Exception {
        Path sourceRoot = createArtifact("tamper");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        WebCanvasArtifactContract.ArtifactSnapshot before = contract.validate(sourceRoot);
        Path main = webRoot(sourceRoot).resolve("main.dart.js");
        byte[] modified = Files.readAllBytes(main);
        modified[0] ^= 1;
        Files.write(main, modified);

        IOException validationError = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));
        assertTrue(validationError.getMessage().contains("pinned Flutter 3.44.8 build"));
        assertThrows(IOException.class,
                () -> contract.verifyUnchanged(sourceRoot, before));
    }

    @Test
    void rejectsMissingAndEmptyRequiredFiles() throws Exception {
        Path missing = createArtifact("missing");
        WebCanvasArtifactContract missingContract = testContract(missing);
        Files.delete(webRoot(missing).resolve("version.json"));
        IOException missingError = assertThrows(IOException.class,
                () -> missingContract.validate(missing));
        assertTrue(missingError.getMessage().contains("missing required"));

        Path empty = createArtifact("empty");
        WebCanvasArtifactContract emptyContract = testContract(empty);
        Files.write(webRoot(empty).resolve("main.dart.js"), new byte[0]);
        IOException emptyError = assertThrows(IOException.class,
                () -> emptyContract.validate(empty));
        assertTrue(emptyError.getMessage().contains("is empty"));
    }

    @Test
    void requiresExactlyEmptyServiceWorkerPlaceholder() throws Exception {
        Path missing = createArtifact("worker-missing");
        WebCanvasArtifactContract missingContract = testContract(missing);
        Files.delete(webRoot(missing).resolve("flutter_service_worker.js"));
        assertThrows(IOException.class,
                () -> missingContract.validate(missing));

        Path populated = createArtifact("worker-populated");
        WebCanvasArtifactContract populatedContract = testContract(populated);
        Files.writeString(webRoot(populated).resolve("flutter_service_worker.js"),
                "self.addEventListener('fetch', () => {});", StandardCharsets.UTF_8);
        IOException error = assertThrows(IOException.class,
                () -> populatedContract.validate(populated));
        assertTrue(error.getMessage().contains("must be empty"));
    }

    @Test
    void rejectsUnexpectedFilesAndDirectories() throws Exception {
        Path unexpectedFile = createArtifact("unexpected-file");
        WebCanvasArtifactContract unexpectedFileContract = testContract(unexpectedFile);
        Files.writeString(webRoot(unexpectedFile).resolve("debug.map"), "map");
        IOException fileError = assertThrows(IOException.class,
                () -> unexpectedFileContract.validate(unexpectedFile));
        assertTrue(fileError.getMessage().contains("unexpected"));

        Path unexpectedDirectory = createArtifact("unexpected-directory");
        WebCanvasArtifactContract unexpectedDirectoryContract =
                testContract(unexpectedDirectory);
        Files.createDirectory(webRoot(unexpectedDirectory).resolve("debug"));
        IOException directoryError = assertThrows(IOException.class,
                () -> unexpectedDirectoryContract.validate(unexpectedDirectory));
        assertTrue(directoryError.getMessage().contains("unexpected"));

        Path unexpectedAsset = createArtifact("unexpected-asset");
        WebCanvasArtifactContract unexpectedAssetContract = testContract(unexpectedAsset);
        Files.writeString(webRoot(unexpectedAsset).resolve("assets/remote-loader.js"),
                "fetch('https://example.invalid')");
        assertThrows(IOException.class,
                () -> unexpectedAssetContract.validate(unexpectedAsset));
    }

    @Test
    void rejectsUnsafePortablePaths() {
        List<String> unsafe = List.of(
                "../escape.js",
                "/absolute.js",
                "assets\\file.js",
                "assets//file.js",
                "assets/./file.js",
                "assets/../file.js",
                "assets/data:stream",
                "assets/CON.txt",
                "assets/NUL",
                "assets/trailing.",
                "assets/trailing ",
                "assets/control\n.js",
                "assets/e\u0301.js");

        unsafe.forEach(path -> assertThrows(IOException.class,
                () -> WebCanvasArtifactContract.requirePortablePath(path), path));
    }

    @Test
    void enforcesFileCountDepthPathFileTotalAndPolicyLimits() throws Exception {
        Path sourceRoot = createArtifact("limits");
        WebCanvasArtifactContract.ExpectedArtifact expected = expectedArtifact(sourceRoot);

        assertThrows(IOException.class, () -> contractWithLimits(expected,
                1, 32, 1_024, 10_000, 1_000_000, 10_000).validate(sourceRoot));
        assertThrows(IOException.class, () -> contractWithLimits(expected,
                256, 1, 1_024, 10_000, 1_000_000, 10_000).validate(sourceRoot));
        assertThrows(IOException.class, () -> contractWithLimits(expected,
                256, 32, 10, 10_000, 1_000_000, 10_000).validate(sourceRoot));
        assertThrows(IOException.class, () -> contractWithLimits(expected,
                256, 32, 1_024, 512, 1_000_000, 512).validate(sourceRoot));
        assertThrows(IOException.class, () -> contractWithLimits(expected,
                256, 32, 1_024, 1_024, 1_024, 1_024).validate(sourceRoot));
        assertThrows(IOException.class, () -> contractWithLimits(expected,
                256, 32, 1_024, 10_000, 1_000_000, 32).validate(sourceRoot));
    }

    @Test
    void rejectsFileSymlinkEscapeWhenSupported() throws Exception {
        Path sourceRoot = createArtifact("file-link");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        Path linked = webRoot(sourceRoot).resolve("main.dart.js");
        Path outside = temporary.resolve("outside-main.dart.js");
        Files.writeString(outside, "outside");
        Files.delete(linked);
        try {
            Files.createSymbolicLink(linked, outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.assumeTrue(false,
                    "file symbolic links are unavailable: " + exception.getMessage());
        }

        IOException error = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));
        assertTrue(error.getMessage().contains("link")
                || error.getMessage().contains("reparse"));
    }

    @Test
    void rejectsRemoteIndexCssAndBridgeReferences() throws Exception {
        Path remoteIndex = createArtifact("remote-index");
        WebCanvasArtifactContract remoteIndexContract = testContract(remoteIndex);
        replaceBuildText(remoteIndex, "index.html", "canvas.css",
                "https://cdn.example.invalid/canvas.css");
        assertThrows(IOException.class,
                () -> remoteIndexContract.validate(remoteIndex));

        Path remoteCss = createArtifact("remote-css");
        WebCanvasArtifactContract remoteCssContract = testContract(remoteCss);
        appendBuildText(remoteCss, "canvas.css",
                "body { background: url(//cdn.example.invalid/image.png); }");
        assertThrows(IOException.class,
                () -> remoteCssContract.validate(remoteCss));

        Path remoteBridge = createArtifact("remote-bridge");
        WebCanvasArtifactContract remoteBridgeContract = testContract(remoteBridge);
        appendBuildText(remoteBridge, "canvas_bridge.js",
                "fetch('https://example.invalid/command');");
        assertThrows(IOException.class,
                () -> remoteBridgeContract.validate(remoteBridge));
    }

    @Test
    void rejectsUnsafeEffectiveBootstrapConfiguration() throws Exception {
        Path cdn = createArtifact("bootstrap-cdn");
        WebCanvasArtifactContract cdnContract = testContract(cdn);
        replaceBuildText(cdn, "flutter_bootstrap.js", "canvasKitBaseUrl: 'canvaskit/'",
                "canvasKitBaseUrl: 'https://cdn.example.invalid/canvaskit/'");
        assertThrows(IOException.class,
                () -> cdnContract.validate(cdn));

        Path worker = createArtifact("bootstrap-worker");
        WebCanvasArtifactContract workerContract = testContract(worker);
        replaceBuildText(worker, "flutter_bootstrap.js", "_flutter.loader.load({",
                "_flutter.loader.load({ serviceWorkerSettings: {},");
        assertThrows(IOException.class,
                () -> workerContract.validate(worker));

        Path nonLocal = createArtifact("bootstrap-non-local");
        WebCanvasArtifactContract nonLocalContract = testContract(nonLocal);
        replaceBuildText(nonLocal, "flutter_bootstrap.js", "\"useLocalCanvasKit\":true",
                "\"useLocalCanvasKit\":false");
        assertThrows(IOException.class,
                () -> nonLocalContract.validate(nonLocal));

        Path missingFonts = createArtifact("bootstrap-fonts");
        WebCanvasArtifactContract missingFontsContract = testContract(missingFonts);
        replaceBuildText(missingFonts, "flutter_bootstrap.js",
                "fontFallbackBaseUrl: 'assets/fonts/',", "");
        assertThrows(IOException.class,
                () -> missingFontsContract.validate(missingFonts));

        Path workerVersion = createArtifact("bootstrap-worker-version");
        WebCanvasArtifactContract workerVersionContract = testContract(workerVersion);
        replaceBuildText(workerVersion, "flutter_bootstrap.js", "\"useLocalCanvasKit\":true",
                "\"useLocalCanvasKit\":true,\"serviceWorkerVersion\":\"active\"");
        assertThrows(IOException.class,
                () -> workerVersionContract.validate(workerVersion));
    }

    @Test
    void rejectsExecutableBootstrapPrefixEvenWhenSemanticSuffixRemainsValid()
            throws Exception {
        Path sourceRoot = createArtifact("bootstrap-prefix");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        Path bootstrap = webRoot(sourceRoot).resolve("flutter_bootstrap.js");
        Files.writeString(bootstrap,
                "fetch('https://example.invalid/injected');\n" + Files.readString(bootstrap),
                StandardCharsets.UTF_8);

        IOException error = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));

        assertTrue(error.getMessage().contains("pinned Flutter 3.44.8 build"));
    }

    @Test
    void pinsEngineRevisionIndependentlyOfBootstrapFileHash() throws Exception {
        Path sourceRoot = createArtifact("engine-revision");
        replaceBuildText(sourceRoot, "flutter_bootstrap.js",
                "\"engineRevision\":\"test-engine\"",
                "\"engineRevision\":\"different-engine\"");
        WebCanvasArtifactContract contract = new WebCanvasArtifactContract(
                WebCanvasArtifactContract.Limits.defaults(),
                expectedArtifact(sourceRoot, "test-engine"));

        IOException error = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));

        assertTrue(error.getMessage().contains("select one local dart2js CanvasKit build"));
    }

    @Test
    void requiresBridgeBeforeBootstrapAndDeferredScripts() throws Exception {
        Path reordered = createArtifact("script-order");
        replaceBuildText(reordered, "index.html",
                "<script src=\"canvas_bridge.js\" defer></script>\n"
                        + "    <script src=\"flutter_bootstrap.js\" defer></script>",
                "<script src=\"flutter_bootstrap.js\" defer></script>\n"
                        + "    <script src=\"canvas_bridge.js\" defer></script>");
        WebCanvasArtifactContract reorderedContract = new WebCanvasArtifactContract(
                WebCanvasArtifactContract.Limits.defaults(),
                expectedArtifact(reordered));
        assertThrows(IOException.class, () -> reorderedContract.validate(reordered));

        Path eager = createArtifact("script-defer");
        replaceBuildText(eager, "index.html",
                "<script src=\"canvas_bridge.js\" defer></script>",
                "<script src=\"canvas_bridge.js\"></script>");
        WebCanvasArtifactContract eagerContract = new WebCanvasArtifactContract(
                WebCanvasArtifactContract.Limits.defaults(), expectedArtifact(eager));
        assertThrows(IOException.class, () -> eagerContract.validate(eager));
    }

    @Test
    void requiresOneExactFrozenContentSecurityPolicyBeforeActiveResources()
            throws Exception {
        String policyMeta = contentSecurityPolicyMeta();

        Path missing = createArtifact("csp-missing");
        replaceBuildText(missing, "index.html", policyMeta, "");
        IOException missingError = assertThrows(IOException.class,
                () -> testContract(missing).validate(missing));
        assertTrue(missingError.getMessage().contains("Content-Security-Policy"));

        Path changed = createArtifact("csp-changed");
        replaceBuildText(changed, "index.html", "worker-src 'none'",
                "worker-src 'self' blob:");
        IOException changedError = assertThrows(IOException.class,
                () -> testContract(changed).validate(changed));
        assertTrue(changedError.getMessage().contains("http-equiv"));

        Path duplicate = createArtifact("csp-duplicate");
        replaceBuildText(duplicate, "index.html", policyMeta,
                policyMeta + "\n    " + policyMeta);
        IOException duplicateError = assertThrows(IOException.class,
                () -> testContract(duplicate).validate(duplicate));
        assertTrue(duplicateError.getMessage().contains("exactly one"));

        Path late = createArtifact("csp-late");
        replaceBuildText(late, "index.html",
                policyMeta + "\n    <link rel=\"stylesheet\" href=\"canvas.css\">",
                "<link rel=\"stylesheet\" href=\"canvas.css\">\n    " + policyMeta);
        IOException lateError = assertThrows(IOException.class,
                () -> testContract(late).validate(late));
        assertTrue(lateError.getMessage().contains("must precede"));
    }

    @Test
    void rejectsAlternateHttpEquivMetaForm() throws Exception {
        Path alternate = createArtifact("csp-alternate-meta");
        replaceBuildText(alternate, "index.html", contentSecurityPolicyMeta(),
                "<meta content=\"" + contentSecurityPolicy()
                        + "\" http-equiv=\"Content-Security-Policy\">");

        IOException error = assertThrows(IOException.class,
                () -> testContract(alternate).validate(alternate));

        assertTrue(error.getMessage().contains("unapproved http-equiv"));
    }

    @Test
    void rejectsCommentedOrOutOfHeadContentSecurityPolicy() throws Exception {
        String policyMeta = contentSecurityPolicyMeta();
        Path commented = createArtifact("csp-commented");
        replaceBuildText(commented, "index.html", policyMeta,
                "<!-- " + policyMeta + " -->");
        IOException commentedError = assertThrows(IOException.class,
                () -> testContract(commented).validate(commented));
        assertTrue(commentedError.getMessage().contains("inert HTML"));

        Path outsideHead = createArtifact("csp-outside-head");
        replaceBuildText(outsideHead, "index.html",
                "<head>\n    <base href=\"/\">\n    " + policyMeta,
                policyMeta + "\n  <head>\n    <base href=\"/\">");
        IOException outsideError = assertThrows(IOException.class,
                () -> testContract(outsideHead).validate(outsideHead));
        assertTrue(outsideError.getMessage().contains("single document head"));
    }

    @Test
    void rejectsBuiltTrustedSourceCopyMismatch() throws Exception {
        Path sourceRoot = createArtifact("trusted-copy");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        Files.writeString(sourceRoot.resolve("web/canvas.css"),
                validCss() + "body { color: black; }", StandardCharsets.UTF_8);

        IOException error = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));

        assertTrue(error.getMessage().contains("differs from its trusted source"));
    }

    @Test
    void rejectsMalformedUtf8PolicyFile() throws Exception {
        Path sourceRoot = createArtifact("utf8");
        WebCanvasArtifactContract contract = testContract(sourceRoot);
        Files.write(webRoot(sourceRoot).resolve("index.html"),
                new byte[] {(byte) 0xc3, (byte) 0x28});

        IOException error = assertThrows(IOException.class,
                () -> contract.validate(sourceRoot));

        assertTrue(error.getMessage().contains("valid UTF-8"));
    }

    @Test
    void validatesConfiguredRealFlutterWebArtifactWhenRequested() throws Exception {
        String configuredSource = System.getProperty("canvas.runner.web.source", "");
        Assumptions.assumeFalse(configuredSource.isBlank(),
                "set -Dcanvas.runner.web.source to exercise a real Flutter Web build");

        WebCanvasArtifactContract.ArtifactSnapshot snapshot =
                new WebCanvasArtifactContract().validate(Path.of(configuredSource));

        assertTrue(snapshot.files().containsKey("main.dart.js"));
        assertTrue(snapshot.files().containsKey("canvaskit/canvaskit.wasm"));
        assertTrue(snapshot.files().containsKey(
                "assets/assets/fonts/Roboto-Regular.ttf"));
        assertTrue(snapshot.files().containsKey(
                "assets/assets/licenses/Roboto-LICENSE.txt"));
        String fontManifest = Files.readString(
                webRoot(Path.of(configuredSource)).resolve("assets/FontManifest.json"));
        assertTrue(fontManifest.contains("\"family\":\"Roboto\""));
        assertTrue(fontManifest.contains("assets/fonts/Roboto-Regular.ttf"));
        assertEquals(0, snapshot.files().get("flutter_service_worker.js").size());
        assertEquals(Set.of(".last_build_id"),
                snapshot.excludedBuildMetadata().keySet());
    }

    private Path createArtifact(String name) throws IOException {
        Path sourceRoot = temporary.resolve(name);
        Path trustedWeb = Files.createDirectories(sourceRoot.resolve("web"));
        Path output = Files.createDirectories(webRoot(sourceRoot));
        for (String relative : NON_EMPTY_FILES) {
            Path file = output.resolve(relative.replace('/', java.io.File.separatorChar));
            Files.createDirectories(file.getParent());
            Files.writeString(file, "artifact:" + relative, StandardCharsets.UTF_8);
        }
        Files.write(output.resolve("flutter_service_worker.js"), new byte[0]);
        Files.writeString(
                output.resolve(".last_build_id"),
                sha256(name.getBytes(StandardCharsets.UTF_8)).substring(0, 32),
                StandardCharsets.US_ASCII);

        writeBuildText(sourceRoot, "index.html", validIndex());
        writeBuildText(sourceRoot, "canvas.css", validCss());
        writeBuildText(sourceRoot, "canvas_bridge.js", validBridge());
        writeBuildText(sourceRoot, "flutter_bootstrap.js", validBootstrap());
        Files.writeString(output.resolve("version.json"),
                "{\"app_name\":\"netbeans_flutter_canvas_runner\"}");

        Files.writeString(trustedWeb.resolve("canvas.css"), validCss());
        Files.writeString(trustedWeb.resolve("canvas_bridge.js"), validBridge());
        return sourceRoot;
    }

    private static WebCanvasArtifactContract testContract(Path sourceRoot) throws IOException {
        return new WebCanvasArtifactContract(
                WebCanvasArtifactContract.Limits.defaults(), expectedArtifact(sourceRoot));
    }

    private static WebCanvasArtifactContract.ExpectedArtifact expectedArtifact(
            Path sourceRoot) throws IOException {
        return expectedArtifact(sourceRoot, "test-engine");
    }

    private static WebCanvasArtifactContract.ExpectedArtifact expectedArtifact(
            Path sourceRoot, String engineRevision) throws IOException {
        TreeMap<String, WebCanvasArtifactContract.ArtifactFile> files = new TreeMap<>();
        Set<String> paths = new HashSet<>(NON_EMPTY_FILES);
        paths.remove(".last_build_id");
        paths.add("flutter_service_worker.js");
        for (String relative : paths) {
            byte[] bytes = Files.readAllBytes(webRoot(sourceRoot).resolve(relative));
            files.put(relative, new WebCanvasArtifactContract.ArtifactFile(
                    relative, bytes.length, sha256(bytes)));
        }
        return new WebCanvasArtifactContract.ExpectedArtifact(engineRevision, files);
    }

    private static WebCanvasArtifactContract contractWithLimits(
            WebCanvasArtifactContract.ExpectedArtifact expected,
            int files,
            int depth,
            int pathCharacters,
            long fileBytes,
            long totalBytes,
            int policyBytes) {
        return new WebCanvasArtifactContract(new WebCanvasArtifactContract.Limits(
                files, depth, pathCharacters, fileBytes, totalBytes, policyBytes), expected);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Path webRoot(Path sourceRoot) {
        return sourceRoot.resolve("build/web");
    }

    private static void replaceBuildText(
            Path sourceRoot, String relative, String target, String replacement)
            throws IOException {
        Path file = webRoot(sourceRoot).resolve(relative);
        String content = Files.readString(file);
        assertTrue(content.contains(target), "fixture target is absent: " + target);
        Files.writeString(file, content.replace(target, replacement));
    }

    private static void appendBuildText(Path sourceRoot, String relative, String suffix)
            throws IOException {
        Path file = webRoot(sourceRoot).resolve(relative);
        Files.writeString(file, Files.readString(file) + System.lineSeparator() + suffix);
    }

    private static void writeBuildText(Path sourceRoot, String relative, String content)
            throws IOException {
        Files.writeString(webRoot(sourceRoot).resolve(relative), content);
    }

    private static String validIndex() {
        return """
                <!doctype html>
                <html>
                  <head>
                    <base href="/">
                    %s
                    <link rel="stylesheet" href="canvas.css">
                    <script src="canvas_bridge.js" defer></script>
                    <script src="flutter_bootstrap.js" defer></script>
                  </head>
                  <body><div id="flutter-host"></div></body>
                </html>
                """.formatted(contentSecurityPolicyMeta());
    }

    private static String contentSecurityPolicyMeta() {
        return "<meta http-equiv=\"Content-Security-Policy\" content=\""
                + contentSecurityPolicy() + "\">";
    }

    private static String contentSecurityPolicy() {
        return "default-src 'none'; base-uri 'none'; connect-src 'self'; "
                + "font-src 'self'; form-action 'none'; frame-ancestors 'none'; "
                + "img-src 'self' data: blob:; manifest-src 'none'; media-src 'none'; "
                + "object-src 'none'; script-src 'self' 'wasm-unsafe-eval'; "
                + "style-src 'self' 'unsafe-inline'; worker-src 'none'";
    }

    private static String validCss() {
        return "html, body, #flutter-host { width: 100%; height: 100%; margin: 0; }\n";
    }

    private static String validBridge() {
        return """
                (function (global) {
                  const bootstrap = global.__netBeansCanvasBootstrap;
                  const webview = global.chrome && global.chrome.webview;
                  global.netBeansCanvasBridge = Object.freeze({bootstrap, webview});
                })(globalThis);
                """;
    }

    private static String validBootstrap() {
        return """
                // Dormant implementation strings from Flutter's generated loader are expected:
                const dormantCdn = 'https://www.gstatic.com/flutter-canvaskit';
                function dormantWorker() { navigator.serviceWorker.register('unused'); }
                if (!window._flutter) { window._flutter = {}; }
                _flutter.buildConfig = {"engineRevision":"test-engine","builds":[{"compileTarget":"dart2js","renderer":"canvaskit","mainJsPath":"main.dart.js"}],"useLocalCanvasKit":true};
                _flutter.loader.load({
                  onEntrypointLoaded: async function (engineInitializer) {
                    const engine = await engineInitializer.initializeEngine({
                      multiViewEnabled: true,
                      canvasKitBaseUrl: 'canvaskit/',
                      fontFallbackBaseUrl: 'assets/fonts/',
                    });
                    await engine.runApp();
                  },
                });
                """;
    }
}

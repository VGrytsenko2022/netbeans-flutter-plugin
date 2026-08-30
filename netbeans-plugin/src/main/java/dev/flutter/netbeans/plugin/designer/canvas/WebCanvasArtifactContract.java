package dev.flutter.netbeans.plugin.designer.canvas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.canvas.runner.CanvasRunnerBundle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrity and offline-execution boundary for the Flutter Web Canvas artifact.
 *
 * <p>The contract deliberately accepts only the controlled {@code build/web}
 * surface produced for the designer runner. Every accepted file is bounded and
 * hashed. HTML, CSS and the effective custom bootstrap configuration are then
 * checked for local-only references. Flutter's generated loader contains
 * dormant CDN and service-worker implementation strings, so those generated
 * internals are not confused with the active bootstrap invocation.
 */
final class WebCanvasArtifactContract {
    private static final Path WEB_OUTPUT = Path.of("build", "web");
    private static final String SNAPSHOT_FORMAT = "NETBEANS_WEB_CANVAS_ARTIFACT|1";
    private static final String EXPECTED_MANIFEST_HEADER =
            "# NetBeans Flutter Web Canvas artifact v1";
    private static final String CONTRACT_FINGERPRINT =
            "flutter-web-canvas-artifact-v1-flutter-3.44.8-dart2js-canvaskit";
    private static final String SERVICE_WORKER = "flutter_service_worker.js";
    private static final String BUILD_METADATA = ".last_build_id";
    private static final Set<String> TRUSTED_SOURCE_COPIES = Set.of(
            "canvas_bridge.js", "canvas.css");
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    private static final Set<String> REQUIRED_NON_EMPTY_FILES = Set.of(
            BUILD_METADATA,
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
    private static final Set<String> ROOT_FILES = Set.of(
            BUILD_METADATA,
            "canvas_bridge.js",
            "canvas.css",
            "flutter_bootstrap.js",
            "flutter.js",
            SERVICE_WORKER,
            "index.html",
            "main.dart.js",
            "version.json");
    private static final Set<String> ROOT_DIRECTORIES = Set.of("assets", "canvaskit");
    private static final Set<String> ALLOWED_FILES = allowedFiles();
    private static final Set<String> POLICY_FILES = Set.of(
            "index.html", "canvas.css", "canvas_bridge.js", "flutter_bootstrap.js");

    private static final Pattern HTML_REFERENCE = Pattern.compile(
            "(?is)\\b(src|href|action|poster)\\s*=\\s*(['\"])(.*?)\\2");
    private static final Pattern SCRIPT_ELEMENT = Pattern.compile(
            "(?is)<script\\b([^>]*)>(.*?)</script\\s*>");
    private static final Pattern SCRIPT_SOURCE = Pattern.compile(
            "(?is)\\bsrc\\s*=\\s*(['\"])(.*?)\\1");
    private static final Pattern DEFER_ATTRIBUTE = Pattern.compile(
            "(?i)(?:^|\\s)defer(?:\\s|=|$)");
    private static final Pattern ASYNC_ATTRIBUTE = Pattern.compile(
            "(?i)(?:^|\\s)async(?:\\s|=|$)");
    private static final Pattern LINK_ELEMENT = Pattern.compile(
            "(?is)<link\\b([^>]*)>");
    private static final Pattern LINK_HREF = Pattern.compile(
            "(?is)\\bhref\\s*=\\s*(['\"])(.*?)\\1");
    private static final Pattern STYLESHEET_RELATION = Pattern.compile(
            "(?is)\\brel\\s*=\\s*(['\"])(.*?)\\1");
    private static final Pattern CSS_URL = Pattern.compile(
            "(?is)url\\s*\\(\\s*(['\"]?)(.*?)\\1\\s*\\)");
    private static final Pattern REMOTE_OR_DANGEROUS_URL = Pattern.compile(
            "(?i)(?:https?|wss?|ftp|file|jar|javascript|data|blob)\\s*:\\s*|(?<!:)//");
    private static final Pattern SERVICE_WORKER_CONFIGURATION = Pattern.compile(
            "(?i)serviceWorkerSettings|serviceWorkerVersion|navigator\\s*\\.\\s*serviceWorker"
                    + "|serviceWorker\\s*:\\s*|\\.register\\s*\\(");
    private static final Pattern MULTI_VIEW_ENABLED = Pattern.compile(
            "\\bmultiViewEnabled\\s*:\\s*true\\b");
    private static final Pattern CANVASKIT_BASE = Pattern.compile(
            "\\bcanvasKitBaseUrl\\s*:\\s*(['\"])(.*?)\\1");
    private static final Pattern FONT_FALLBACK_BASE = Pattern.compile(
            "\\bfontFallbackBaseUrl\\s*:\\s*(['\"])(.*?)\\1");
    private static final Pattern WINDOWS_RESERVED_NAME = Pattern.compile(
            "(?i)^(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$");

    private final Limits limits;
    private final ExpectedArtifact expectedArtifact;

    WebCanvasArtifactContract() {
        this(Limits.defaults(), PackagedExpectedHolder.VALUE);
    }

    WebCanvasArtifactContract(Limits limits) {
        this(limits, PackagedExpectedHolder.VALUE);
    }

    WebCanvasArtifactContract(Limits limits, ExpectedArtifact expectedArtifact) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.expectedArtifact = Objects.requireNonNull(expectedArtifact, "expectedArtifact");
    }

    String fingerprint() {
        return CONTRACT_FINGERPRINT;
    }

    /** Validates {@code runnerSourceRoot/build/web} and returns immutable evidence. */
    ArtifactSnapshot validate(Path runnerSourceRoot) throws IOException {
        Path sourceRoot = requireSafeDirectory(runnerSourceRoot,
                "Flutter Web Canvas runner source root");
        Path webRoot = sourceRoot.resolve(WEB_OUTPUT).toAbsolutePath().normalize();
        requireInside(sourceRoot, webRoot, "Flutter Web Canvas artifact");
        requireNoLinkedComponents(sourceRoot, webRoot, "Flutter Web Canvas artifact");
        ArtifactSnapshot snapshot = snapshotWebRoot(requireSafeDirectory(
                webRoot, "Flutter Web Canvas artifact directory"));
        validateTrustedSourceCopies(sourceRoot, snapshot);
        validateExpectedArtifact(snapshot);
        return snapshot;
    }

    /** Revalidates the artifact and rejects any change since {@code expected}. */
    void verifyUnchanged(Path runnerSourceRoot, ArtifactSnapshot expected) throws IOException {
        Objects.requireNonNull(expected, "expected");
        ArtifactSnapshot actual = validate(runnerSourceRoot);
        if (!expected.equals(actual)) {
            throw new IOException("Flutter Web Canvas artifact changed after validation");
        }
    }

    private ArtifactSnapshot snapshotWebRoot(Path webRoot) throws IOException {
        Path realRoot = webRoot.toRealPath();
        TreeMap<String, ArtifactFile> files = new TreeMap<>();
        Map<String, byte[]> policyBytes = new HashMap<>();
        Set<String> caseInsensitivePaths = new HashSet<>();
        Set<Object> fileKeys = new HashSet<>();
        MutableTotals totals = new MutableTotals();

        Files.walkFileTree(webRoot, Set.of(), limits.maxDepth() + 1,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory, BasicFileAttributes attributes) throws IOException {
                        rejectLinkOrReparse(directory,
                                "Flutter Web Canvas artifact directory");
                        if (!attributes.isDirectory()) {
                            throw new IOException("non-directory in Flutter Web Canvas artifact: "
                                    + directory);
                        }
                        if (!directory.equals(webRoot)) {
                            String relative = portableRelative(webRoot, directory);
                            if (directoryDepth(relative) > limits.maxDepth()) {
                                throw new IOException("Flutter Web Canvas artifact exceeds maximum "
                                        + "depth " + limits.maxDepth() + ": " + relative);
                            }
                            requireAllowedDirectory(relative);
                            requireRealPathInside(realRoot, directory,
                                    "Flutter Web Canvas artifact directory");
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attributes) throws IOException {
                        String relative = portableRelative(webRoot, file);
                        if (directoryDepth(relative) > limits.maxDepth()) {
                            throw new IOException("Flutter Web Canvas artifact exceeds maximum "
                                    + "depth " + limits.maxDepth() + ": " + relative);
                        }
                        if (attributes.isDirectory()) {
                            throw new IOException("Flutter Web Canvas artifact exceeds maximum "
                                    + "depth " + limits.maxDepth() + ": " + relative);
                        }
                        rejectLinkOrReparse(file, "Flutter Web Canvas artifact file");
                        if (!attributes.isRegularFile()) {
                            throw new IOException("non-regular file in Flutter Web Canvas artifact: "
                                    + relative);
                        }
                        requireAllowedFile(relative);
                        requireRealPathInside(realRoot, file,
                                "Flutter Web Canvas artifact file");
                        String folded = relative.toLowerCase(Locale.ROOT);
                        if (!caseInsensitivePaths.add(folded)) {
                            throw new IOException("case-insensitive path collision in Flutter Web "
                                    + "Canvas artifact: " + relative);
                        }
                        if (++totals.fileCount > limits.maxFiles()) {
                            throw new IOException("Flutter Web Canvas artifact contains more than "
                                    + limits.maxFiles() + " files");
                        }
                        if (attributes.size() > limits.maxFileBytes()) {
                            throw new IOException("Flutter Web Canvas artifact file exceeds "
                                    + limits.maxFileBytes() + " bytes: " + relative);
                        }
                        totals.totalBytes = addBounded(
                                totals.totalBytes, attributes.size(), limits.maxTotalBytes());
                        Object fileKey = attributes.fileKey();
                        if (fileKey != null && !fileKeys.add(fileKey)) {
                            throw new IOException("hard-linked files are not allowed in Flutter Web "
                                    + "Canvas artifact: " + relative);
                        }
                        HashResult hash = hashStableFile(file, attributes, relative,
                                POLICY_FILES.contains(relative));
                        files.put(relative,
                                new ArtifactFile(relative, attributes.size(), hash.sha256()));
                        if (hash.policyBytes() != null) {
                            policyBytes.put(relative, hash.policyBytes());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException failure)
                            throws IOException {
                        throw new IOException("cannot inspect Flutter Web Canvas artifact path: "
                                + file, failure);
                    }
                });

        requireCompleteArtifact(files);
        Map<String, String> policyText = decodePolicyText(policyBytes);
        validateIndex(policyText.get("index.html"), files);
        validateCss(policyText.get("canvas.css"), files);
        validateBridge(policyText.get("canvas_bridge.js"));
        validateBootstrap(policyText.get("flutter_bootstrap.js"));

        String aggregate = aggregateSha256(files);
        ArtifactFile metadata = files.remove(BUILD_METADATA);
        return new ArtifactSnapshot(
                realRoot,
                files,
                Map.of(BUILD_METADATA, metadata),
                totals.totalBytes,
                aggregate);
    }

    private void validateTrustedSourceCopies(
            Path sourceRoot, ArtifactSnapshot snapshot) throws IOException {
        Path trustedWebRoot = sourceRoot.resolve("web").toAbsolutePath().normalize();
        requireInside(sourceRoot, trustedWebRoot, "trusted Flutter Web Canvas sources");
        requireNoLinkedComponents(sourceRoot, trustedWebRoot,
                "trusted Flutter Web Canvas sources");
        requireSafeDirectory(trustedWebRoot, "trusted Flutter Web Canvas source directory");
        Path realTrustedRoot = trustedWebRoot.toRealPath();
        for (String relative : TRUSTED_SOURCE_COPIES) {
            Path trustedFile = trustedWebRoot.resolve(relative);
            rejectLinkOrReparse(trustedFile, "trusted Flutter Web Canvas source file");
            requireRealPathInside(realTrustedRoot, trustedFile,
                    "trusted Flutter Web Canvas source file");
            BasicFileAttributes attributes = Files.readAttributes(
                    trustedFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new IOException("trusted Flutter Web Canvas source is not a file: "
                        + trustedFile);
            }
            HashResult trusted = hashStableFile(
                    trustedFile, attributes, "web/" + relative, false);
            ArtifactFile built = snapshot.files().get(relative);
            if (built == null
                    || built.size() != attributes.size()
                    || !built.sha256().equals(trusted.sha256())) {
                throw new IOException("built Flutter Web Canvas " + relative
                        + " differs from its trusted source");
            }
        }
    }

    private HashResult hashStableFile(
            Path file,
            BasicFileAttributes initial,
            String relative,
            boolean retainForPolicy) throws IOException {
        if (retainForPolicy && initial.size() > limits.maxPolicyFileBytes()) {
            throw new IOException("Flutter Web Canvas policy file exceeds "
                    + limits.maxPolicyFileBytes() + " bytes: " + relative);
        }
        MessageDigest digest = sha256();
        ByteArrayOutputStream retained = retainForPolicy
                ? new ByteArrayOutputStream((int) initial.size()) : null;
        long read = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(
                file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                read = addBounded(read, count, limits.maxFileBytes());
                digest.update(buffer, 0, count);
                if (retained != null) {
                    retained.write(buffer, 0, count);
                }
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (read != initial.size()
                || after.size() != initial.size()
                || !after.lastModifiedTime().equals(initial.lastModifiedTime())
                || !sameFileKey(initial.fileKey(), after.fileKey())) {
            throw new IOException("Flutter Web Canvas artifact file changed while hashing: "
                    + relative);
        }
        return new HashResult(
                HexFormat.of().formatHex(digest.digest()),
                retained == null ? null : retained.toByteArray());
    }

    private static boolean sameFileKey(Object first, Object second) {
        return first == null || second == null || first.equals(second);
    }

    private static void requireCompleteArtifact(Map<String, ArtifactFile> files)
            throws IOException {
        for (String required : REQUIRED_NON_EMPTY_FILES) {
            ArtifactFile file = files.get(required);
            if (file == null) {
                throw new IOException("missing required Flutter Web Canvas artifact file: "
                        + required);
            }
            if (file.size() == 0) {
                throw new IOException("required Flutter Web Canvas artifact file is empty: "
                        + required);
            }
        }
        ArtifactFile serviceWorker = files.get(SERVICE_WORKER);
        if (serviceWorker == null) {
            throw new IOException("missing disabled Flutter service-worker placeholder: "
                    + SERVICE_WORKER);
        }
        if (serviceWorker.size() != 0) {
            throw new IOException("Flutter service-worker placeholder must be empty: "
                    + SERVICE_WORKER);
        }
    }

    private static Map<String, String> decodePolicyText(Map<String, byte[]> policyBytes)
            throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String required : POLICY_FILES) {
            byte[] bytes = policyBytes.get(required);
            if (bytes == null) {
                throw new IOException("missing Flutter Web Canvas policy file: " + required);
            }
            try {
                result.put(required, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString());
            } catch (CharacterCodingException exception) {
                throw new IOException("Flutter Web Canvas policy file is not valid UTF-8: "
                        + required, exception);
            }
        }
        return result;
    }

    private void validateExpectedArtifact(ArtifactSnapshot snapshot)
            throws IOException {
        TreeMap<String, ArtifactFile> files = new TreeMap<>(snapshot.files());
        files.putAll(snapshot.excludedBuildMetadata());
        if (!files.keySet().equals(expectedArtifact.files().keySet())) {
            Set<String> missing = new HashSet<>(expectedArtifact.files().keySet());
            missing.removeAll(files.keySet());
            Set<String> unexpected = new HashSet<>(files.keySet());
            unexpected.removeAll(expectedArtifact.files().keySet());
            throw new IOException("Flutter Web Canvas artifact does not match the pinned file "
                    + "manifest; missing=" + missing + ", unexpected=" + unexpected);
        }
        for (Map.Entry<String, ArtifactFile> entry : expectedArtifact.files().entrySet()) {
            ArtifactFile actual = files.get(entry.getKey());
            ArtifactFile expected = entry.getValue();
            if (actual.size() != expected.size()
                    || !actual.sha256().equals(expected.sha256())) {
                throw new IOException("Flutter Web Canvas artifact file differs from the pinned "
                        + "Flutter 3.44.8 build: " + entry.getKey());
            }
        }
    }

    private static void validateIndex(String index, Map<String, ArtifactFile> files)
            throws IOException {
        rejectRemoteOrDangerous(index, "index.html");
        rejectServiceWorkerConfiguration(index, "index.html");

        List<String> scripts = new java.util.ArrayList<>();
        Matcher scriptMatcher = SCRIPT_ELEMENT.matcher(index);
        int scriptElements = 0;
        while (scriptMatcher.find()) {
            scriptElements++;
            if (!scriptMatcher.group(2).isBlank()) {
                throw new IOException("inline script is not allowed in Flutter Web Canvas index");
            }
            String attributes = scriptMatcher.group(1);
            Matcher source = SCRIPT_SOURCE.matcher(attributes);
            if (!source.find()) {
                throw new IOException("every Flutter Web Canvas script must have one local src");
            }
            String scriptSource = source.group(2);
            if (source.find()) {
                throw new IOException("every Flutter Web Canvas script must have one local src");
            }
            if (!DEFER_ATTRIBUTE.matcher(attributes).find()
                    || ASYNC_ATTRIBUTE.matcher(attributes).find()) {
                throw new IOException("Flutter Web Canvas scripts must be ordered, deferred and "
                        + "must not be asynchronous");
            }
            scripts.add(requireLocalFileReference(
                    scriptSource, "index.html script", files));
        }
        if (scriptElements != 2
                || !scripts.equals(List.of("canvas_bridge.js", "flutter_bootstrap.js"))) {
            throw new IOException("Flutter Web Canvas index must load deferred canvas_bridge.js "
                    + "before deferred flutter_bootstrap.js");
        }

        Matcher references = HTML_REFERENCE.matcher(index);
        while (references.find()) {
            String reference = references.group(3).trim();
            if ("/".equals(reference) && "href".equalsIgnoreCase(references.group(1))) {
                continue;
            }
            requireLocalFileReference(reference, "index.html reference", files);
        }

        Matcher links = LINK_ELEMENT.matcher(index);
        int linkElements = 0;
        while (links.find()) {
            linkElements++;
            String attributes = links.group(1);
            Matcher href = LINK_HREF.matcher(attributes);
            Matcher relation = STYLESHEET_RELATION.matcher(attributes);
            if (!href.find()) {
                throw new IOException("Flutter Web Canvas index must contain exactly one local "
                        + "canvas.css stylesheet link");
            }
            String hrefValue = href.group(2);
            if (href.find() || !relation.find()) {
                throw new IOException("Flutter Web Canvas index must contain exactly one local "
                        + "canvas.css stylesheet link");
            }
            String relationValue = relation.group(2);
            if (relation.find()
                    || !"canvas.css".equals(requireLocalFileReference(
                            hrefValue, "index.html stylesheet", files))
                    || !containsHtmlToken(relationValue, "stylesheet")) {
                throw new IOException("Flutter Web Canvas index must contain exactly one local "
                        + "canvas.css stylesheet link");
            }
        }
        if (linkElements != 1) {
            throw new IOException("Flutter Web Canvas index must contain exactly one local "
                    + "canvas.css stylesheet link");
        }
        if (!index.contains("id=\"flutter-host\"")
                && !index.contains("id='flutter-host'")) {
            throw new IOException("Flutter Web Canvas index is missing flutter-host");
        }
    }

    private static boolean containsHtmlToken(String value, String expected) {
        return Pattern.compile("(?i)(?:^|\\s)" + Pattern.quote(expected) + "(?:\\s|$)")
                .matcher(value.trim())
                .find();
    }

    private static void validateCss(String css, Map<String, ArtifactFile> files)
            throws IOException {
        rejectRemoteOrDangerous(css, "canvas.css");
        if (Pattern.compile("(?i)@import\\b").matcher(css).find()) {
            throw new IOException("CSS imports are not allowed in Flutter Web Canvas artifact");
        }
        Matcher urls = CSS_URL.matcher(css);
        while (urls.find()) {
            requireLocalFileReference(urls.group(2).trim(), "canvas.css url", files);
        }
    }

    private static void validateBridge(String bridge) throws IOException {
        rejectRemoteOrDangerous(bridge, "canvas_bridge.js");
        rejectServiceWorkerConfiguration(bridge, "canvas_bridge.js");
        if (!bridge.contains("chrome.webview")
                || !bridge.contains("__netBeansCanvasBootstrap")
                || !bridge.contains("netBeansCanvasBridge")) {
            throw new IOException("Flutter Web Canvas bridge is missing authenticated WebView2 "
                    + "transport markers");
        }
    }

    private void validateBootstrap(String bootstrap) throws IOException {
        int buildConfigStart = bootstrap.lastIndexOf("_flutter.buildConfig");
        int invocationStart = bootstrap.lastIndexOf("_flutter.loader.load(");
        if (buildConfigStart < 0 || invocationStart <= buildConfigStart) {
            throw new IOException("Flutter Web Canvas bootstrap is missing the effective custom "
                    + "build configuration");
        }
        String buildConfiguration = bootstrap.substring(buildConfigStart, invocationStart);
        String invocation = bootstrap.substring(invocationStart);
        validateBuildConfiguration(buildConfiguration);
        rejectServiceWorkerConfiguration(buildConfiguration,
                "Flutter Web Canvas build configuration");
        rejectRemoteOrDangerous(invocation,
                "Flutter Web Canvas bootstrap invocation");
        rejectServiceWorkerConfiguration(invocation,
                "Flutter Web Canvas bootstrap invocation");
        if (!MULTI_VIEW_ENABLED.matcher(invocation).find()) {
            throw new IOException("Flutter Web Canvas bootstrap must enable multi-view mode");
        }
        requireExactDirectoryConfiguration(
                invocation, CANVASKIT_BASE, "canvasKitBaseUrl", "canvaskit/");
        requireExactDirectoryConfiguration(
                invocation, FONT_FALLBACK_BASE, "fontFallbackBaseUrl", "assets/fonts/");
    }

    private void validateBuildConfiguration(String assignment) throws IOException {
        int equals = assignment.indexOf('=');
        int objectStart = equals < 0 ? -1 : assignment.indexOf('{', equals + 1);
        if (objectStart < 0) {
            throw new IOException("Flutter Web Canvas build configuration is not an object");
        }
        int objectEnd = findJsonObjectEnd(assignment, objectStart);
        String remainder = assignment.substring(objectEnd + 1).trim();
        if (!remainder.isEmpty() && !";".equals(remainder)) {
            throw new IOException("unexpected content after Flutter Web Canvas build configuration");
        }
        final JsonNode configuration;
        try {
            configuration = JSON.readTree(assignment.substring(objectStart, objectEnd + 1));
        } catch (IOException exception) {
            throw new IOException("invalid Flutter Web Canvas build configuration JSON", exception);
        }
        JsonNode builds = configuration.path("builds");
        JsonNode localCanvasKit = configuration.path("useLocalCanvasKit");
        JsonNode engineRevision = configuration.path("engineRevision");
        if (!localCanvasKit.isBoolean()
                || !localCanvasKit.booleanValue()
                || !engineRevision.isTextual()
                || !expectedArtifact.engineRevision().equals(engineRevision.textValue())
                || !builds.isArray()
                || builds.size() != 1) {
            throw new IOException("Flutter Web Canvas bootstrap must select one local dart2js "
                    + "CanvasKit build");
        }
        JsonNode build = builds.get(0);
        if (!build.isObject()
                || !build.path("compileTarget").isTextual()
                || !build.path("renderer").isTextual()
                || !build.path("mainJsPath").isTextual()
                || !"dart2js".equals(build.path("compileTarget").textValue())
                || !"canvaskit".equals(build.path("renderer").textValue())
                || !"main.dart.js".equals(build.path("mainJsPath").textValue())) {
            throw new IOException("Flutter Web Canvas bootstrap must select one local dart2js "
                    + "CanvasKit build");
        }
    }

    private static int findJsonObjectEnd(String text, int objectStart) throws IOException {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = objectStart; index < text.length(); index++) {
            char value = text.charAt(index);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (value == '\\') {
                    escaped = true;
                } else if (value == '"') {
                    quoted = false;
                }
                continue;
            }
            if (value == '"') {
                quoted = true;
            } else if (value == '{') {
                depth++;
            } else if (value == '}' && --depth == 0) {
                return index;
            }
        }
        throw new IOException("unterminated Flutter Web Canvas build configuration JSON");
    }

    private static void requireExactDirectoryConfiguration(
            String invocation,
            Pattern pattern,
            String property,
        String expected) throws IOException {
        Matcher matcher = pattern.matcher(invocation);
        if (!matcher.find()) {
            throw new IOException("Flutter Web Canvas bootstrap must set " + property
                    + " to local " + expected);
        }
        String configured = matcher.group(2);
        if (matcher.find() || !expected.equals(configured)) {
            throw new IOException("Flutter Web Canvas bootstrap must set " + property
                    + " to local " + expected);
        }
    }

    private static void rejectRemoteOrDangerous(String text, String label)
            throws IOException {
        if (REMOTE_OR_DANGEROUS_URL.matcher(text).find()) {
            throw new IOException(label + " contains a remote or dangerous URL reference");
        }
    }

    private static void rejectServiceWorkerConfiguration(String text, String label)
            throws IOException {
        if (SERVICE_WORKER_CONFIGURATION.matcher(text).find()) {
            throw new IOException(label + " enables or registers a service worker");
        }
    }

    private static String requireLocalFileReference(
            String reference,
            String label,
            Map<String, ArtifactFile> files) throws IOException {
        if (reference.isEmpty()
                || reference.startsWith("/")
                || reference.contains("?")
                || reference.contains("#")
                || reference.contains("%")) {
            throw new IOException(label + " is not a closed local artifact path: " + reference);
        }
        rejectRemoteOrDangerous(reference, label);
        requirePortablePath(reference);
        if (!files.containsKey(reference)) {
            throw new IOException(label + " does not resolve inside the artifact: " + reference);
        }
        return reference;
    }

    private static void requireAllowedDirectory(String relative) throws IOException {
        String prefix = relative + "/";
        if (!ROOT_DIRECTORIES.contains(firstSegment(relative))
                || ALLOWED_FILES.stream().noneMatch(path -> path.startsWith(prefix))) {
            throw new IOException("unexpected directory in Flutter Web Canvas artifact: "
                    + relative);
        }
    }

    private static void requireAllowedFile(String relative) throws IOException {
        if (!ALLOWED_FILES.contains(relative)) {
            throw new IOException("unexpected file in Flutter Web Canvas artifact: " + relative);
        }
    }

    private static Set<String> allowedFiles() {
        Set<String> result = new HashSet<>(REQUIRED_NON_EMPTY_FILES);
        result.add(SERVICE_WORKER);
        if (!result.containsAll(ROOT_FILES)) {
            throw new ExceptionInInitializerError("Web Canvas root allowlist is incomplete");
        }
        return Set.copyOf(result);
    }

    private static ExpectedArtifact loadPackagedExpectedArtifact() {
        try (InputStream input = CanvasRunnerBundle.openWebArtifactManifest()) {
            return readExpectedArtifact(input);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "cannot load packaged Flutter Web Canvas artifact manifest", exception);
        }
    }

    static ExpectedArtifact readExpectedArtifact(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.US_ASCII.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            String header = reader.readLine();
            if (!EXPECTED_MANIFEST_HEADER.equals(header)) {
                throw new IOException("invalid Flutter Web Canvas artifact manifest header");
            }
            String engineLine = reader.readLine();
            if (engineLine == null || !engineLine.startsWith("engine|")) {
                throw new IOException("Flutter Web Canvas artifact manifest has no engine");
            }
            String engineRevision = engineLine.substring("engine|".length());
            if (!engineRevision.matches("[0-9a-f]{40}")) {
                throw new IOException("invalid pinned Flutter Web Canvas engine revision");
            }
            TreeMap<String, ArtifactFile> files = new TreeMap<>();
            Set<String> caseInsensitivePaths = new HashSet<>();
            String line;
            int lineNumber = 2;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split("\\|", -1);
                if (fields.length != 4 || !"file".equals(fields[0])) {
                    throw new IOException("invalid Flutter Web Canvas artifact manifest line "
                            + lineNumber);
                }
                String relative = fields[1];
                requirePortablePath(relative);
                if (!caseInsensitivePaths.add(relative.toLowerCase(Locale.ROOT))) {
                    throw new IOException("case-insensitive path collision in Flutter Web Canvas "
                            + "artifact manifest: " + relative);
                }
                final long size;
                try {
                    size = Long.parseLong(fields[2]);
                } catch (NumberFormatException exception) {
                    throw new IOException("invalid Flutter Web Canvas artifact size at line "
                            + lineNumber, exception);
                }
                if (size < 0 || !fields[3].matches("[0-9a-f]{64}")) {
                    throw new IOException("invalid Flutter Web Canvas artifact metadata at line "
                            + lineNumber);
                }
                ArtifactFile previous = files.put(relative,
                        new ArtifactFile(relative, size, fields[3]));
                if (previous != null) {
                    throw new IOException("duplicate Flutter Web Canvas artifact path: "
                            + relative);
                }
            }
            try {
                return new ExpectedArtifact(engineRevision, files);
            } catch (IllegalArgumentException exception) {
                throw new IOException("invalid Flutter Web Canvas artifact manifest", exception);
            }
        }
    }

    static void requirePortablePath(String relative) throws IOException {
        Objects.requireNonNull(relative, "relative");
        if (relative.isEmpty() || relative.length() > Limits.MAX_PATH_CHARACTERS) {
            throw new IOException("invalid portable Flutter Web Canvas path length");
        }
        if (relative.startsWith("/") || relative.endsWith("/")
                || relative.indexOf('\\') >= 0) {
            throw new IOException("Flutter Web Canvas path must be relative and use '/': "
                    + relative);
        }
        if (!Normalizer.normalize(relative, Normalizer.Form.NFC).equals(relative)) {
            throw new IOException("Flutter Web Canvas path is not Unicode-normalized: "
                    + relative);
        }
        String[] segments = relative.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || segment.length() > 255
                    || segment.startsWith(" ") || segment.endsWith(" ")
                    || segment.endsWith(".")
                    || WINDOWS_RESERVED_NAME.matcher(segment).matches()) {
                throw new IOException("unsafe Flutter Web Canvas path segment: " + segment);
            }
            for (int index = 0; index < segment.length(); index++) {
                char value = segment.charAt(index);
                if (value < 0x20 || value == 0x7f
                        || "<>:\"|?*".indexOf(value) >= 0) {
                    throw new IOException("unsafe character in Flutter Web Canvas path: "
                            + relative);
                }
            }
        }
    }

    private String portableRelative(Path root, Path path) throws IOException {
        Path normalized = requireInside(root, path, "Flutter Web Canvas artifact path");
        String relative = root.relativize(normalized).toString().replace('\\', '/');
        if (relative.length() > limits.maxPathCharacters()) {
            throw new IOException("Flutter Web Canvas artifact path exceeds "
                    + limits.maxPathCharacters() + " characters");
        }
        requirePortablePath(relative);
        return relative;
    }

    private static int directoryDepth(String relative) {
        return (int) relative.chars().filter(value -> value == '/').count() + 1;
    }

    private static String firstSegment(String relative) {
        int separator = relative.indexOf('/');
        return separator < 0 ? relative : relative.substring(0, separator);
    }

    private static Path requireSafeDirectory(Path directory, String label) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path normalized = directory.toAbsolutePath().normalize();
        rejectLinkOrReparse(normalized, label);
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a directory: " + normalized);
        }
        return normalized;
    }

    private static Path requireInside(Path root, Path candidate, String label)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedRoot)) {
            throw new IOException(label + " escapes its trusted root: " + candidate);
        }
        return normalized;
    }

    private static void requireNoLinkedComponents(Path root, Path target, String label)
            throws IOException {
        Path safeTarget = requireInside(root, target, label);
        Path current = root;
        rejectLinkOrReparse(current, label);
        for (Path segment : root.relativize(safeTarget)) {
            current = current.resolve(segment);
            rejectLinkOrReparse(current, label);
        }
    }

    private static void requireRealPathInside(Path realRoot, Path candidate, String label)
            throws IOException {
        Path real = candidate.toRealPath();
        if (!real.startsWith(realRoot)) {
            throw new IOException(label + " escapes the real artifact root: " + candidate);
        }
    }

    private static void rejectLinkOrReparse(Path path, String label) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link: " + path);
        }
        BasicFileAttributes noFollow = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (noFollow.isSymbolicLink() || noFollow.isOther()) {
            throw new IOException(label + " must not be a link or reparse point: " + path);
        }
        BasicFileAttributes followed = Files.readAttributes(path, BasicFileAttributes.class);
        if (noFollow.fileKey() != null && followed.fileKey() != null
                && !noFollow.fileKey().equals(followed.fileKey())) {
            throw new IOException(label + " resolves through a link or junction: " + path);
        }
        rejectWindowsReparseAttribute(path, label);
    }

    private static void rejectWindowsReparseAttribute(Path path, String label)
            throws IOException {
        try {
            Object rawAttributes = Files.getAttribute(
                    path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            if (rawAttributes instanceof Number value
                    && (value.intValue() & 0x400) != 0) {
                throw new IOException(label + " must not be a Windows reparse point: " + path);
            }
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            // Non-Windows providers do not expose the raw DOS attribute bitset.
        }
    }

    private static long addBounded(long first, long second, long maximum)
            throws IOException {
        final long result;
        try {
            result = Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            throw new IOException("Flutter Web Canvas artifact size overflow", exception);
        }
        if (result > maximum) {
            throw new IOException("Flutter Web Canvas artifact exceeds " + maximum + " bytes");
        }
        return result;
    }

    private static String aggregateSha256(Map<String, ArtifactFile> files) {
        MessageDigest digest = sha256();
        updateLengthPrefixed(digest, SNAPSHOT_FORMAT.getBytes(StandardCharsets.US_ASCII));
        updateLengthPrefixed(digest, CONTRACT_FINGERPRINT.getBytes(StandardCharsets.US_ASCII));
        files.values().forEach(file -> {
            updateLengthPrefixed(digest,
                    file.relativePath().getBytes(StandardCharsets.UTF_8));
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(file.size()).array());
            digest.update(HexFormat.of().parseHex(file.sha256()));
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateLengthPrefixed(MessageDigest digest, byte[] bytes) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    /**
     * Immutable evidence for one closed build tree.
     *
     * <p>{@code files} contains only files safe to publish. Build-only metadata
     * remains hashed in the aggregate but is exposed separately so a publisher
     * cannot accidentally serve it.
     */
    record ArtifactSnapshot(
            Path root,
            Map<String, ArtifactFile> files,
            Map<String, ArtifactFile> excludedBuildMetadata,
            long totalBytes,
            String sha256) {
        ArtifactSnapshot {
            root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
            files = Collections.unmodifiableMap(new TreeMap<>(
                    Objects.requireNonNull(files, "files")));
            excludedBuildMetadata = Collections.unmodifiableMap(new TreeMap<>(
                    Objects.requireNonNull(excludedBuildMetadata,
                            "excludedBuildMetadata")));
            if (files.isEmpty() || totalBytes < 0) {
                throw new IllegalArgumentException("invalid Web Canvas artifact snapshot");
            }
            if (!excludedBuildMetadata.keySet().equals(Set.of(BUILD_METADATA))) {
                throw new IllegalArgumentException("invalid excluded Web Canvas build metadata");
            }
            requireSha256(sha256);
        }
    }

    record ArtifactFile(String relativePath, long size, String sha256) {
        ArtifactFile {
            Objects.requireNonNull(relativePath, "relativePath");
            if (size < 0) {
                throw new IllegalArgumentException("negative Web Canvas artifact file size");
            }
            requireSha256(sha256);
        }
    }

    record ExpectedArtifact(String engineRevision, Map<String, ArtifactFile> files) {
        ExpectedArtifact {
            engineRevision = Objects.requireNonNull(engineRevision, "engineRevision");
            if (engineRevision.isBlank()
                    || engineRevision.length() > 128
                    || !engineRevision.matches("[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("invalid Flutter Web Canvas engine revision");
            }
            files = Collections.unmodifiableMap(new TreeMap<>(
                    Objects.requireNonNull(files, "files")));
            if (!files.keySet().equals(ALLOWED_FILES)) {
                throw new IllegalArgumentException(
                        "expected Flutter Web Canvas file set does not match the contract");
            }
            files.forEach((relative, file) -> {
                if (!relative.equals(file.relativePath())) {
                    throw new IllegalArgumentException(
                            "Flutter Web Canvas artifact path/key mismatch: " + relative);
                }
            });
        }
    }

    record Limits(
            int maxFiles,
            int maxDepth,
            int maxPathCharacters,
            long maxFileBytes,
            long maxTotalBytes,
            int maxPolicyFileBytes) {
        private static final int MAX_PATH_CHARACTERS = 1_024;
        private static final int DEFAULT_MAX_PATH_CHARACTERS = 512;

        Limits {
            if (maxFiles < 1
                    || maxDepth < 1
                    || maxDepth > 128
                    || maxPathCharacters < 1
                    || maxPathCharacters > MAX_PATH_CHARACTERS
                    || maxFileBytes < 1
                    || maxTotalBytes < maxFileBytes
                    || maxPolicyFileBytes < 1
                    || maxPolicyFileBytes > maxFileBytes) {
                throw new IllegalArgumentException("invalid Flutter Web Canvas artifact limits");
            }
        }

        static Limits defaults() {
            return new Limits(
                    256,
                    8,
                    DEFAULT_MAX_PATH_CHARACTERS,
                    128L * 1024 * 1024,
                    512L * 1024 * 1024,
                    1024 * 1024);
        }
    }

    private record HashResult(String sha256, byte[] policyBytes) {
    }

    private static final class MutableTotals {
        private int fileCount;
        private long totalBytes;
    }

    private static final class PackagedExpectedHolder {
        private static final ExpectedArtifact VALUE = loadPackagedExpectedArtifact();

        private PackagedExpectedHolder() {
        }
    }

    private static void requireSha256(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid SHA-256 value");
        }
    }
}

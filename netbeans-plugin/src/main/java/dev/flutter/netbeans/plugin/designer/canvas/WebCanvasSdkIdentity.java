package dev.flutter.netbeans.plugin.designer.canvas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.designer.canvas.CanvasEngineIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/** Strict reader for the exact Flutter toolchain that compiles a Web Canvas. */
final class WebCanvasSdkIdentity {
    static final int MAXIMUM_METADATA_BYTES = 16 * 1024;
    static final int MAXIMUM_VERSION_BYTES = 512;

    private static final String METADATA_PATH = "bin/cache/flutter.version.json";
    private static final String ENGINE_PATH = "bin/internal/engine.version";
    private static final ObjectMapper JSON = createMapper();

    private WebCanvasSdkIdentity() {
    }

    static CanvasEngineIdentity read(FlutterSdk sdk) throws IOException {
        Objects.requireNonNull(sdk, "sdk");
        Path home = sdk.home().toAbsolutePath().normalize();
        requireSafeDirectory(home, "configured Flutter SDK root");

        Path executable = sdk.flutterExecutable().toAbsolutePath().normalize();
        requireInside(home, executable, "configured Flutter executable");
        requireNoLinkedComponents(home, executable, "configured Flutter executable");
        requireSafeRegularFile(executable, "configured Flutter executable");

        Path metadata = home.resolve(METADATA_PATH).toAbsolutePath().normalize();
        requireNoLinkedComponents(home, metadata, "Flutter SDK version metadata");
        JsonNode document = parseMetadata(readStableFile(
                metadata, MAXIMUM_METADATA_BYTES, "Flutter SDK version metadata"));

        String flutterVersion = requiredText(document, "flutterVersion");
        String frameworkRevision = requiredText(document, "frameworkRevision");
        String engineRevision = requiredText(document, "engineRevision");
        String dartSdkVersion = requiredText(document, "dartSdkVersion");
        if (!frameworkRevision.matches("[0-9a-f]{40}")) {
            throw new IOException(
                    "Flutter SDK frameworkRevision must be a lowercase 40-digit commit");
        }
        if (!engineRevision.matches("[0-9a-f]{40}")) {
            throw new IOException(
                    "Flutter SDK engineRevision must be a lowercase 40-digit commit");
        }

        final CanvasEngineIdentity identity;
        try {
            identity = new CanvasEngineIdentity(
                    flutterVersion,
                    frameworkRevision,
                    engineRevision,
                    dartSdkVersion);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Flutter SDK version metadata contains an invalid identity",
                    failure);
        }

        Path engineFile = home.resolve(ENGINE_PATH).toAbsolutePath().normalize();
        requireNoLinkedComponents(home, engineFile, "Flutter SDK engine version");
        String recordedEngine = decodeSingleLine(readStableFile(
                engineFile, MAXIMUM_VERSION_BYTES, "Flutter SDK engine version"),
                "Flutter SDK engine version");
        if (!identity.engineRevision().equals(recordedEngine)) {
            throw new IOException(
                    "Flutter SDK engineRevision does not match bin/internal/engine.version");
        }

        Path rootVersion = home.resolve("version").toAbsolutePath().normalize();
        if (existsNoFollow(rootVersion)) {
            requireNoLinkedComponents(home, rootVersion, "Flutter SDK root version");
            String recordedFlutter = decodeSingleLine(readStableFile(
                    rootVersion, MAXIMUM_VERSION_BYTES, "Flutter SDK root version"),
                    "Flutter SDK root version");
            if (!identity.flutterVersion().equals(recordedFlutter)) {
                throw new IOException(
                        "Flutter SDK flutterVersion does not match the SDK root version");
            }
        }
        return identity;
    }

    private static boolean existsNoFollow(Path file) throws IOException {
        try {
            Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return true;
        } catch (NoSuchFileException missing) {
            return false;
        }
    }

    private static JsonNode parseMetadata(byte[] content) throws IOException {
        final JsonNode document;
        try {
            document = JSON.readTree(content);
        } catch (IOException failure) {
            throw new IOException("Flutter SDK version metadata is not strict bounded JSON",
                    failure);
        }
        if (document == null || !document.isObject()) {
            throw new IOException("Flutter SDK version metadata must be one JSON object");
        }
        return document;
    }

    private static String requiredText(JsonNode document, String field) throws IOException {
        JsonNode value = document.get(field);
        if (value == null || !value.isTextual()) {
            throw new IOException(
                    "Flutter SDK version metadata field " + field + " must be text");
        }
        return value.textValue();
    }

    private static byte[] readStableFile(Path file, int maximum, String label)
            throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        rejectLinkOrReparse(normalized, label);
        BasicFileAttributes before = Files.readAttributes(
                normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.isOther()
                || before.size() < 1 || before.size() > maximum) {
            throw new IOException(label + " has an invalid bounded file shape: " + normalized);
        }

        byte[] content = new byte[(int) before.size()];
        int offset = 0;
        try (InputStream input = Files.newInputStream(normalized, LinkOption.NOFOLLOW_LINKS)) {
            while (offset < content.length) {
                int count = input.read(content, offset, content.length - offset);
                if (count < 0) {
                    break;
                }
                offset += count;
            }
            if (offset != content.length || input.read() != -1) {
                throw new IOException(label + " changed while it was being read: " + normalized);
            }
        }

        rejectLinkOrReparse(normalized, label);
        BasicFileAttributes after = Files.readAttributes(
                normalized, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile()
                || after.isOther()
                || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !sameFileKey(before.fileKey(), after.fileKey())) {
            throw new IOException(label + " changed while it was being read: " + normalized);
        }
        return content;
    }

    private static String decodeSingleLine(byte[] content, String label) throws IOException {
        final String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw new IOException(label + " is not valid UTF-8", failure);
        }
        String value = decoded;
        if (value.endsWith("\n")) {
            value = value.substring(0, value.length() - 1);
            if (value.endsWith("\r")) {
                value = value.substring(0, value.length() - 1);
            }
        }
        if (value.isEmpty()
                || value.length() > MAXIMUM_VERSION_BYTES
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IOException(label + " must contain one bounded non-empty line");
        }
        return value;
    }

    private static void requireSafeDirectory(Path path, String label) throws IOException {
        rejectLinkOrReparse(path, label);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a safe directory: " + path);
        }
    }

    private static void requireSafeRegularFile(Path path, String label) throws IOException {
        rejectLinkOrReparse(path, label);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is not a safe regular file: " + path);
        }
    }

    private static Path requireInside(Path root, Path candidate, String label)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new IOException(label + " is outside the configured Flutter SDK");
        }
        return normalizedCandidate;
    }

    private static void requireNoLinkedComponents(Path root, Path target, String label)
            throws IOException {
        Path safeTarget = requireInside(root, target, label);
        Path current = root.toAbsolutePath().normalize();
        rejectLinkOrReparse(current, label);
        for (Path segment : current.relativize(safeTarget)) {
            current = current.resolve(segment);
            rejectLinkOrReparse(current, label);
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
        if (!sameFileKey(noFollow.fileKey(), followed.fileKey())) {
            throw new IOException(label + " resolves through a link or junction: " + path);
        }
        rejectWindowsReparseAttribute(path, label);
    }

    private static void rejectWindowsReparseAttribute(Path path, String label)
            throws IOException {
        try {
            Object attributes = Files.getAttribute(
                    path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            if (attributes instanceof Number value
                    && (value.intValue() & 0x400) != 0) {
                throw new IOException(label + " must not be a Windows reparse point: " + path);
            }
        } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
            // Non-Windows file systems need not expose raw DOS attributes.
        }
    }

    private static boolean sameFileKey(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static ObjectMapper createMapper() {
        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(MAXIMUM_METADATA_BYTES)
                        .maxNestingDepth(8)
                        .maxTokenCount(128)
                        .maxNameLength(64)
                        .maxStringLength(512)
                        .maxNumberLength(64)
                        .build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();
        return new ObjectMapper(factory)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }
}

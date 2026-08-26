package dev.flutter.netbeans.plugin.designer.canvas;

import dev.flutter.netbeans.api.FlutterSdk;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable cache identity for runner sources and the configured Flutter engine. */
public record CanvasRunnerCacheIdentity(
        String sourceDigest,
        String engineRevision,
        String cacheKey) {
    private static final int MAX_IDENTITY_FILE_BYTES = 4096;

    public CanvasRunnerCacheIdentity {
        sourceDigest = requireDigest(sourceDigest, "source digest");
        engineRevision = requireRecordValue(engineRevision, "Flutter engine revision");
        cacheKey = requireDigest(cacheKey, "cache key");
    }

    /** Short Windows-safe directory segment; the full digest remains in cache markers. */
    public String cacheDirectoryName() {
        return "r-" + cacheKey.substring(0, 20);
    }

    public static CanvasRunnerCacheIdentity create(
            CanvasRunnerSourceBundle bundle,
            FlutterSdk sdk) throws IOException {
        Objects.requireNonNull(bundle, "bundle");
        Objects.requireNonNull(sdk, "sdk");
        Path sdkHome = sdk.home().toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path executable = sdk.flutterExecutable().toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (Files.isSymbolicLink(sdkHome) || Files.isSymbolicLink(executable)) {
            throw new IOException("configured Flutter SDK paths must not be symbolic links");
        }
        if (!executable.startsWith(sdkHome)) {
            throw new IOException("configured Flutter executable is outside the configured SDK");
        }
        Path engineFile = sdkHome.resolve("bin").resolve("internal").resolve("engine.version");
        String engineRevision = readIdentityFile(engineFile, "Flutter engine revision");
        Path frameworkVersionFile = sdkHome.resolve("version");
        String frameworkVersion = Files.isRegularFile(frameworkVersionFile, LinkOption.NOFOLLOW_LINKS)
                ? readIdentityFile(frameworkVersionFile, "Flutter framework version")
                : "unknown";

        MessageDigest digest = sha256();
        update(digest, "bundle", bundle.version());
        update(digest, "sources", bundle.sourceDigest());
        update(digest, "sdk-home", sdkHome.toString());
        update(digest, "flutter-executable", executable.toString());
        update(digest, "framework", frameworkVersion);
        update(digest, "engine", engineRevision);
        return new CanvasRunnerCacheIdentity(
                bundle.sourceDigest(), engineRevision, HexFormat.of().formatHex(digest.digest()));
    }

    private static String readIdentityFile(Path file, String label) throws IOException {
        if (Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(label + " is missing from the configured Flutter SDK: " + file);
        }
        long size = Files.size(file);
        if (size <= 0 || size > MAX_IDENTITY_FILE_BYTES) {
            throw new IOException(label + " has an invalid size: " + file);
        }
        byte[] content = new byte[(int) size];
        int offset = 0;
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            while (offset < content.length) {
                int count = input.read(content, offset, content.length - offset);
                if (count < 0) {
                    break;
                }
                offset += count;
            }
            if (offset != content.length || input.read() != -1) {
                throw new IOException(label + " changed while it was being read: " + file);
            }
        }
        String value = new String(content, StandardCharsets.US_ASCII).trim();
        return requireValue(value, label);
    }

    private static void update(MessageDigest digest, String name, String value) {
        digest.update(name.getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) 0);
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String requireDigest(String value, String label) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " must be a lowercase SHA-256 digest");
        }
        return value;
    }

    private static String requireValue(String value, String label) throws IOException {
        if (value == null || value.isBlank() || value.length() > 512
                || value.indexOf('\0') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IOException(label + " is invalid");
        }
        return value;
    }

    private static String requireRecordValue(String value, String label) {
        try {
            return requireValue(value, label);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}

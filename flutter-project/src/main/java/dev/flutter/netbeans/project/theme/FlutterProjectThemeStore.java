package dev.flutter.netbeans.project.theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Read-only descriptor and generated-artifact verification for project consumers. */
public final class FlutterProjectThemeStore {
    public static final int MAX_GENERATED_DART_BYTES = 1024 * 1024;

    private final FlutterProjectThemeCodec codec;

    public FlutterProjectThemeStore() {
        this(new FlutterProjectThemeCodec());
    }

    FlutterProjectThemeStore(FlutterProjectThemeCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public FlutterProjectThemeLoadResult load(Path projectRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path root = projectRoot.toAbsolutePath().normalize();
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH).normalize();
        if (!descriptor.startsWith(root)) {
            return result(FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                    "Project theme descriptor escapes project root " + root + ".");
        }
        if (!Files.exists(descriptor, LinkOption.NOFOLLOW_LINKS)) {
            return result(FlutterProjectThemeLoadStatus.MISSING,
                    "Project theme descriptor is missing: " + descriptor + ".");
        }
        if (containsSymbolicLink(root, FlutterProjectThemePaths.DESCRIPTOR_PATH)) {
            return result(FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                    "Project theme descriptor path contains a symbolic link: "
                    + descriptor + ".");
        }

        FlutterProjectTheme theme;
        try {
            theme = codec.read(descriptor);
        } catch (IOException | RuntimeException ex) {
            return result(FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                    "Project theme descriptor is invalid at " + descriptor + ": "
                    + compact(ex.getMessage()) + ".");
        }

        byte[] expectedGenerated;
        String expectedGeneratedHash;
        try {
            expectedGenerated = FlutterProjectThemeDartGenerator.generate(theme);
            expectedGeneratedHash = FlutterProjectThemeDigests.sha256(expectedGenerated);
        } catch (RuntimeException ex) {
            return result(FlutterProjectThemeLoadStatus.INVALID_DESCRIPTOR,
                    "Project theme descriptor cannot generate its declared Dart artifact at "
                    + descriptor + ": " + compact(ex.getMessage()) + ".");
        }
        if (!expectedGeneratedHash.equals(theme.generated().sha256())) {
            return result(FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                    theme, "Project theme descriptor semantics require generated Dart hash "
                    + expectedGeneratedHash + " but " + descriptor + " records "
                    + theme.generated().sha256() + ".");
        }

        Path generated = root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH).normalize();
        if (!generated.startsWith(root)
                || !Files.isRegularFile(generated, LinkOption.NOFOLLOW_LINKS)
                || containsSymbolicLink(root, FlutterProjectThemePaths.GENERATED_DART_PATH)) {
            return result(FlutterProjectThemeLoadStatus.GENERATED_DART_MISSING, theme,
                    "Generated project theme Dart file is missing or unsafe: "
                    + generated + ".");
        }
        try {
            long size = Files.size(generated);
            if (size <= 0 || size > MAX_GENERATED_DART_BYTES) {
                return result(FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                        theme, "Generated project theme Dart file has an unsupported size at "
                        + generated + ".");
            }
            byte[] actualBytes = Files.readAllBytes(generated);
            String actual = FlutterProjectThemeDigests.sha256(actualBytes);
            if (!Arrays.equals(actualBytes, expectedGenerated)) {
                return result(FlutterProjectThemeLoadStatus.GENERATED_DART_HASH_MISMATCH,
                        theme, "Generated project theme Dart bytes do not match the deterministic "
                        + "artifact required by " + descriptor + ": expected hash "
                        + expectedGeneratedHash
                        + " but found " + actual + ".");
            }
        } catch (IOException ex) {
            return result(FlutterProjectThemeLoadStatus.IO_ERROR,
                    "Could not read generated project theme Dart file " + generated + ": "
                    + compact(ex.getMessage()) + ".");
        }
        return result(FlutterProjectThemeLoadStatus.VALID, theme,
                "Project theme descriptor and generated Dart artifact are valid.");
    }

    private static boolean containsSymbolicLink(Path root, Path relative) {
        Path current = root;
        if (Files.isSymbolicLink(current)) {
            return true;
        }
        for (Path element : relative) {
            current = current.resolve(element);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "no cause was reported";
        }
        String value = message.trim().replaceAll("\\s+", " ");
        return value.length() <= 400 ? value : value.substring(0, 400) + "…";
    }

    private static FlutterProjectThemeLoadResult result(
            FlutterProjectThemeLoadStatus status, String detail) {
        return new FlutterProjectThemeLoadResult(status, Optional.empty(), detail);
    }

    private static FlutterProjectThemeLoadResult result(
            FlutterProjectThemeLoadStatus status,
            FlutterProjectTheme theme,
            String detail) {
        return new FlutterProjectThemeLoadResult(status, Optional.of(theme), detail);
    }
}

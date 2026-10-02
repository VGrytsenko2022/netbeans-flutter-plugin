package io.github.vgrytsenko2022.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded detector for Flutter's {@code .metadata} project type. */
public final class FlutterProjectTypeDetector {
    private static final int MAX_METADATA_BYTES = 64 * 1024;
    private static final Pattern PROJECT_TYPE = Pattern.compile(
            "(?m)^project_type[ \\t]*:[ \\t]*([A-Za-z0-9_-]+)[ \\t]*(?:#.*)?$");

    private FlutterProjectTypeDetector() {
    }

    public static FlutterProjectType detect(Path projectRoot) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Flutter project directory is unavailable: " + root);
        }
        Path metadata = root.resolve(".metadata");
        if (Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(metadata)
                    || !Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Flutter project metadata is not a safe regular file: "
                        + metadata);
            }
            String contents = readBoundedUtf8(metadata, "Flutter project metadata");
            Matcher matcher = PROJECT_TYPE.matcher(contents);
            if (matcher.find()) {
                String value = matcher.group(1).toLowerCase(Locale.ROOT);
                if (matcher.find()) {
                    throw new IOException(
                            "Flutter project metadata declares project_type more than once: "
                            + metadata);
                }
                return switch (value) {
                    case "app" -> FlutterProjectType.APP;
                    case "module" -> FlutterProjectType.MODULE;
                    case "package" -> FlutterProjectType.PACKAGE;
                    case "plugin" -> FlutterProjectType.PLUGIN;
                    default -> FlutterProjectType.UNKNOWN;
                };
            }
            // A modern Flutter project with metadata but without a supported
            // project_type is not a legacy application. Flutter itself rejects
            // platform recreation for this state, so fail closed here.
            return FlutterProjectType.UNKNOWN;
        }
        return detectLegacy(root);
    }

    private static FlutterProjectType detectLegacy(Path root) throws IOException {
        if (Files.isDirectory(root.resolve(".android"), LinkOption.NOFOLLOW_LINKS)
                || Files.isDirectory(root.resolve(".ios"), LinkOption.NOFOLLOW_LINKS)) {
            return FlutterProjectType.MODULE;
        }
        Path pubspec = root.resolve("pubspec.yaml");
        if (Files.isRegularFile(pubspec, LinkOption.NOFOLLOW_LINKS)
                && legacyPubspecDeclaresPlugin(readBoundedUtf8(pubspec, "Flutter pubspec"))) {
            return FlutterProjectType.PLUGIN;
        }
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.all()) {
            if (Files.isDirectory(
                    root.resolve(platform.id()), LinkOption.NOFOLLOW_LINKS)) {
                return FlutterProjectType.APP;
            }
        }
        if (Files.isRegularFile(
                root.resolve("lib").resolve("main.dart"), LinkOption.NOFOLLOW_LINKS)) {
            return FlutterProjectType.APP;
        }
        return FlutterProjectType.PACKAGE;
    }

    private static boolean legacyPubspecDeclaresPlugin(String contents) {
        boolean inFlutterSection = false;
        for (String line : contents.split("\\R", -1)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indentation = line.length() - line.stripLeading().length();
            if (indentation == 0) {
                inFlutterSection = trimmed.startsWith("flutter:");
            } else if (inFlutterSection && trimmed.startsWith("plugin:")) {
                return true;
            }
        }
        return false;
    }

    private static String readBoundedUtf8(Path file, String label) throws IOException {
        long declaredSize = Files.size(file);
        if (declaredSize < 0 || declaredSize > MAX_METADATA_BYTES) {
            throw new IOException(label + " exceeds " + MAX_METADATA_BYTES + " bytes: " + file);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream((int) declaredSize);
        try (InputStream input = Files.newInputStream(
                file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) {
                    continue;
                }
                total = Math.addExact(total, count);
                if (total > MAX_METADATA_BYTES) {
                    throw new IOException(label + " changed beyond its size limit: " + file);
                }
                output.write(buffer, 0, count);
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(output.toByteArray()))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(label + " is not valid UTF-8: " + file, exception);
        }
    }
}

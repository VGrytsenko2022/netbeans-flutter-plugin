package io.github.vgrytsenko2022.project;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.regex.*;

public final class FlutterProjectDetector {
    private static final Pattern NAME = Pattern.compile("(?m)^name\\s*:\\s*([A-Za-z0-9_-]+)\\s*$");

    public Optional<FlutterProjectInfo> detect(Path directory) {
        Path root = directory.toAbsolutePath().normalize();
        Path pubspec = root.resolve("pubspec.yaml");
        Path lib = root.resolve("lib");
        if (!Files.isRegularFile(pubspec) || !Files.isDirectory(lib)) return Optional.empty();
        try {
            String yaml = Files.readString(pubspec);
            if (!yaml.contains("flutter:")) return Optional.empty();
            Matcher m = NAME.matcher(yaml);
            String name = m.find() ? m.group(1) : root.getFileName().toString();
            return Optional.of(new FlutterProjectInfo(root, name, pubspec));
        } catch (IOException e) { return Optional.empty(); }
    }
}

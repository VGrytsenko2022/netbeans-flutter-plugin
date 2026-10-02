package io.github.vgrytsenko2022.api;

import java.nio.file.Path;

public record FlutterProjectInfo(Path root, String name, Path pubspec) { }

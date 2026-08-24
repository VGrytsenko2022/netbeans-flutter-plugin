package dev.flutter.netbeans.api;

import java.nio.file.Path;

public record FlutterProjectInfo(Path root, String name, Path pubspec) { }

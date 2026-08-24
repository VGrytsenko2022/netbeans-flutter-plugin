package dev.flutter.netbeans.api;

import java.nio.file.Path;

public record FlutterSdk(Path home, Path flutterExecutable) {
    public FlutterSdk {
        if (home == null || flutterExecutable == null) throw new IllegalArgumentException("SDK paths are required");
    }
}

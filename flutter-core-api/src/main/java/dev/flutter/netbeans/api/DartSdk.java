package dev.flutter.netbeans.api;

import java.nio.file.Path;

public record DartSdk(Path home, Path dartExecutable) {
    public DartSdk {
        if (home == null || dartExecutable == null) {
            throw new IllegalArgumentException("SDK paths are required");
        }
    }
}

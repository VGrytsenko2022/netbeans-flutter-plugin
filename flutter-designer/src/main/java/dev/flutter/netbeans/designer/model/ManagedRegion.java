package dev.flutter.netbeans.designer.model;

import java.util.regex.Pattern;

/** Integrity metadata for one designer-managed Dart source region. */
public record ManagedRegion(String sha256) {
    private static final Pattern SHA_256 = Pattern.compile("[0-9A-F]{64}");

    public ManagedRegion {
        sha256 = ModelConstraints.matching(sha256, "sha256", SHA_256);
    }
}

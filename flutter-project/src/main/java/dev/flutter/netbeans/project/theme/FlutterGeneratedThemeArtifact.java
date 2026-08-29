package dev.flutter.netbeans.project.theme;

import java.util.Objects;
import java.util.regex.Pattern;

/** Exact generated Dart artifact referenced by a project theme descriptor. */
public record FlutterGeneratedThemeArtifact(String dartFile, String sha256) {
    private static final Pattern SHA256 = Pattern.compile("[0-9A-F]{64}");

    public FlutterGeneratedThemeArtifact {
        dartFile = Objects.requireNonNull(dartFile, "dartFile");
        sha256 = Objects.requireNonNull(sha256, "sha256");
        if (!FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH.equals(dartFile)) {
            throw new IllegalArgumentException(
                    "Generated theme dartFile must be "
                    + FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH);
        }
        if (!SHA256.matcher(sha256).matches()) {
            throw new IllegalArgumentException(
                    "Generated theme sha256 must contain exactly 64 uppercase hexadecimal characters");
        }
    }
}

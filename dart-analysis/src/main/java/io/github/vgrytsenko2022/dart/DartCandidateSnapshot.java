package io.github.vgrytsenko2022.dart;

import java.nio.file.Path;

/** Identity repeated by every result without retaining the candidate text. */
public record DartCandidateSnapshot(
        Path projectRoot,
        Path dartFile,
        long version,
        String sha256,
        int utf8Size) {

    public DartCandidateSnapshot {
        projectRoot = normalize(projectRoot, "projectRoot");
        dartFile = normalize(dartFile, "dartFile");
        if (!dartFile.startsWith(projectRoot)) {
            throw new IllegalArgumentException("dartFile must be inside projectRoot");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (sha256 == null || !sha256.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException("sha256 must be 64 uppercase hexadecimal characters");
        }
        if (utf8Size < 0) {
            throw new IllegalArgumentException("utf8Size must not be negative");
        }
    }

    private static Path normalize(Path value, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        if (!value.isAbsolute()) {
            throw new IllegalArgumentException(name + " must be absolute");
        }
        return value.normalize();
    }
}

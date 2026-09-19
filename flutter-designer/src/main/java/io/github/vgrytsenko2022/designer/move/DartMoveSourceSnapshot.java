package io.github.vgrytsenko2022.designer.move;

import io.github.vgrytsenko2022.designer.source.OriginalDartBytes;
import java.util.Arrays;
import java.util.Objects;

/** Exact immutable bytes and project-relative identity of one Dart source. */
public final class DartMoveSourceSnapshot {
    private final String projectRelativePath;
    private final byte[] bytes;
    private final int hashCode;

    public DartMoveSourceSnapshot(String projectRelativePath, byte[] bytes) {
        this.projectRelativePath = Objects.requireNonNull(
                projectRelativePath, "projectRelativePath");
        Objects.requireNonNull(bytes, "bytes");
        this.bytes = bytes.clone();
        hashCode = 31 * projectRelativePath.hashCode() + Arrays.hashCode(bytes);
    }

    public DartMoveSourceSnapshot(
            String projectRelativePath,
            OriginalDartBytes bytes) {
        this(projectRelativePath, Objects.requireNonNull(bytes, "bytes").copyBytes());
    }

    public String projectRelativePath() {
        return projectRelativePath;
    }

    public int size() {
        return bytes.length;
    }

    public byte[] copyBytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof DartMoveSourceSnapshot snapshot
                && projectRelativePath.equals(snapshot.projectRelativePath)
                && Arrays.equals(bytes, snapshot.bytes);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "DartMoveSourceSnapshot[path=" + projectRelativePath
                + ", size=" + bytes.length + "]";
    }
}

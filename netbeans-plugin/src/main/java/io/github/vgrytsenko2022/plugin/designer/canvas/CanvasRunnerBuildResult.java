package io.github.vgrytsenko2022.plugin.designer.canvas;

import java.nio.file.Path;
import java.util.Objects;

/** Successful cached or newly-built native Canvas runner executable. */
public record CanvasRunnerBuildResult(
        Path executable,
        Path sourceDirectory,
        CanvasRunnerCacheIdentity identity,
        boolean builtNow,
        String diagnostics,
        CanvasRunnerRuntimeLease runtimeLease) implements AutoCloseable {
    public CanvasRunnerBuildResult(
            Path executable,
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity,
            boolean builtNow,
            String diagnostics) {
        this(executable, sourceDirectory, identity, builtNow, diagnostics,
                CanvasRunnerRuntimeLease.detached());
    }

    public CanvasRunnerBuildResult {
        executable = Objects.requireNonNull(executable, "executable").toAbsolutePath().normalize();
        sourceDirectory = Objects.requireNonNull(sourceDirectory, "sourceDirectory")
                .toAbsolutePath().normalize();
        identity = Objects.requireNonNull(identity, "identity");
        diagnostics = Objects.requireNonNullElse(diagnostics, "");
        runtimeLease = Objects.requireNonNull(runtimeLease, "runtimeLease");
        if (!executable.startsWith(sourceDirectory)) {
            throw new IllegalArgumentException("runner executable must be inside its cache directory");
        }
        Path leasedRuntime = runtimeLease.runtimeDirectory().orElse(null);
        if (leasedRuntime != null && !executable.getParent().equals(leasedRuntime)) {
            throw new IllegalArgumentException(
                    "runner executable must belong to its runtime lease");
        }
    }

    /** Returns an independent lease for one asynchronous launch or process. */
    public CanvasRunnerRuntimeLease retainRuntimeLease() {
        return runtimeLease.retain();
    }

    /** Releases the base runtime generation lease; this method is idempotent. */
    @Override
    public void close() {
        runtimeLease.close();
    }
}

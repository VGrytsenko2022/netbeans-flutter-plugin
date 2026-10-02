package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.designer.canvas.CanvasEngineIdentity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One validated, privately published Flutter Web Canvas generation.
 *
 * <p>The mutable Flutter build tree is retained only as cache evidence. A
 * consumer receives the immutable publication and must close this result when
 * ownership has been transferred to, or released by, the browser host.
 */
final class WebCanvasBuildResult implements AutoCloseable {
    private final Path sourceDirectory;
    private final CanvasRunnerCacheIdentity identity;
    private final CanvasEngineIdentity engineIdentity;
    private final boolean builtNow;
    private final String diagnostics;
    private final WebCanvasArtifactPublisher.PublishedArtifact artifact;
    private boolean closed;

    WebCanvasBuildResult(
            Path sourceDirectory,
            CanvasRunnerCacheIdentity identity,
            CanvasEngineIdentity engineIdentity,
            boolean builtNow,
            String diagnostics,
            WebCanvasArtifactPublisher.PublishedArtifact artifact) {
        this.sourceDirectory = Objects.requireNonNull(
                sourceDirectory, "sourceDirectory").toAbsolutePath().normalize();
        this.identity = Objects.requireNonNull(identity, "identity");
        this.engineIdentity = Objects.requireNonNull(engineIdentity, "engineIdentity");
        this.builtNow = builtNow;
        this.diagnostics = Objects.requireNonNullElse(diagnostics, "");
        this.artifact = Objects.requireNonNull(artifact, "artifact");
        if (artifact.root().startsWith(this.sourceDirectory)
                || this.sourceDirectory.startsWith(artifact.root())) {
            throw new IllegalArgumentException(
                    "Web Canvas publication and mutable source cache must be disjoint");
        }
    }

    Path sourceDirectory() {
        return sourceDirectory;
    }

    CanvasRunnerCacheIdentity identity() {
        return identity;
    }

    CanvasEngineIdentity engineIdentity() {
        return engineIdentity;
    }

    boolean builtNow() {
        return builtNow;
    }

    String diagnostics() {
        return diagnostics;
    }

    synchronized WebCanvasArtifactPublisher.PublishedArtifact artifact() {
        if (closed) {
            throw new IllegalStateException("Web Canvas build result is already closed");
        }
        return artifact;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        // Publication cleanup is explicitly retryable. Do not retire the
        // result until its owner close has actually succeeded.
        artifact.close();
        closed = true;
    }
}

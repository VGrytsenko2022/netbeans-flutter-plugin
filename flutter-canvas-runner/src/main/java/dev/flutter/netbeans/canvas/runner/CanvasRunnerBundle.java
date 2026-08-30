package dev.flutter.netbeans.canvas.runner;

import java.io.InputStream;

/**
 * Stable class-loader anchor for the versioned Flutter Canvas runners.
 *
 * <p>The NetBeans module deliberately consumes the runner as packaged resources
 * instead of reading this repository or a user's Flutter project.</p>
 */
public final class CanvasRunnerBundle {
    public static final String VERSION = "1";
    public static final String RESOURCE_ROOT = "/dev/flutter/netbeans/canvas/runner/v1/";
    public static final String MANIFEST_RESOURCE =
            "/dev/flutter/netbeans/canvas/runner/CanvasRunnerBundle.manifest";
    public static final String WEB_ARTIFACT_MANIFEST_RESOURCE =
            "/dev/flutter/netbeans/canvas/runner/WebCanvasArtifact.manifest";

    private CanvasRunnerBundle() {
    }

    public static InputStream openManifest() {
        return requireResource(MANIFEST_RESOURCE);
    }

    public static InputStream openWebArtifactManifest() {
        return requireResource(WEB_ARTIFACT_MANIFEST_RESOURCE);
    }

    public static InputStream openSource(String relativePath) {
        if (relativePath == null
                || relativePath.isBlank()
                || relativePath.startsWith("/")
                || relativePath.contains("\\")
                || relativePath.contains("..")) {
            throw new IllegalArgumentException("invalid runner resource path");
        }
        return requireResource(RESOURCE_ROOT + relativePath);
    }

    private static InputStream requireResource(String resource) {
        InputStream stream = CanvasRunnerBundle.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("missing packaged Canvas runner resource: " + resource);
        }
        return stream;
    }
}

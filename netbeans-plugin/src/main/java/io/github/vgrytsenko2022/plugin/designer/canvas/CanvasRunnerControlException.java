package io.github.vgrytsenko2022.plugin.designer.canvas;

/** Invalid or unsupported runtime control message from the Canvas runner. */
public final class CanvasRunnerControlException extends Exception {
    public CanvasRunnerControlException(String message) {
        super(message);
    }

    public CanvasRunnerControlException(String message, Throwable cause) {
        super(message, cause);
    }
}

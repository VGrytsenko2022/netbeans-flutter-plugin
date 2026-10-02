package io.github.vgrytsenko2022.designer.canvas.runtime;

/** Stage at which one read-only Canvas backend attempt failed. */
public enum CanvasFailureStage {
    STARTUP,
    HANDSHAKE,
    RENDER,
    PROTOCOL,
    TERMINATION
}

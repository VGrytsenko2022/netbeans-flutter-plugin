package io.github.vgrytsenko2022.designer.canvas.protocol;

/** Host-owned lifecycle of one pure Canvas wire session. */
public enum CanvasWireSessionState {
    NEW,
    HELLO_SENT,
    READY,
    CLOSING,
    CLOSED,
    FAILED
}

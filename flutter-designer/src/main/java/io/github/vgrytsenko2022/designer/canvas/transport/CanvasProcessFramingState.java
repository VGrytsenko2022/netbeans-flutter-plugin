package io.github.vgrytsenko2022.designer.canvas.transport;

/** Lifecycle phase enforced before a frame body is allocated or written. */
public enum CanvasProcessFramingState {
    HANDSHAKE,
    NEGOTIATED,
    CLOSING
}

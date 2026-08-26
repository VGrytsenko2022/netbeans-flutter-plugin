package dev.flutter.netbeans.designer.canvas.transport;

/** Lifecycle phase enforced before a frame body is allocated or written. */
public enum CanvasProcessFramingState {
    HANDSHAKE,
    NEGOTIATED,
    CLOSING
}

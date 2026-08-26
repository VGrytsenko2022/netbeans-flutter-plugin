package dev.flutter.netbeans.designer.canvas.transport;

/** Stable failure categories for the process byte-stream boundary. */
public enum CanvasProcessFramingError {
    TRUNCATED_HEADER,
    INVALID_MAGIC,
    UNSUPPORTED_VERSION,
    UNKNOWN_KIND,
    INVALID_FLAGS,
    EMPTY_PAYLOAD,
    WRONG_DIRECTION,
    WRONG_STATE,
    CAPABILITY_NOT_NEGOTIATED,
    PAYLOAD_NOT_NEGOTIATED,
    PAYLOAD_LIMIT,
    PAYLOAD_DESCRIPTOR_REQUIRED,
    PAYLOAD_DESCRIPTOR_MISMATCH,
    TRUNCATED_PAYLOAD,
    DIGEST_MISMATCH,
    STREAM_POISONED
}

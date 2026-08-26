package dev.flutter.netbeans.designer.canvas.protocol;

/** Stable categories for rejected Canvas control-message bodies. */
public enum CanvasWireDiagnosticCode {
    MESSAGE_LIMIT,
    MALFORMED_UTF8,
    MALFORMED_JSON,
    DUPLICATE_FIELD,
    RESOURCE_LIMIT,
    ROOT_MUST_BE_OBJECT,
    MISSING_REQUIRED_FIELD,
    UNKNOWN_FIELD,
    WRONG_VALUE_TYPE,
    INVALID_VALUE,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_VERSION,
    UNKNOWN_MESSAGE_TYPE,
    TRAILING_CONTENT
}

package dev.flutter.netbeans.designer.codec;

/** Stable, machine-readable failure categories returned by the bounded codec. */
public enum FdCodecDiagnosticCode {
    MALFORMED_UTF8,
    MALFORMED_JSON,
    DUPLICATE_FIELD,
    TRAILING_CONTENT,
    ROOT_MUST_BE_OBJECT,
    MISSING_REQUIRED_FIELD,
    UNKNOWN_FIELD,
    WRONG_VALUE_TYPE,
    INVALID_VALUE,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_OLDER_VERSION,
    RESOURCE_LIMIT,
    NUMBER_RANGE
}

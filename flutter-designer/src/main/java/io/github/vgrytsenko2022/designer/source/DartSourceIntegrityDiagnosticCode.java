package io.github.vgrytsenko2022.designer.source;

/** Stable machine-readable Dart source-integrity diagnostic codes. */
public enum DartSourceIntegrityDiagnosticCode {
    SOURCE_TOO_LARGE,
    SOURCE_READ_FAILED,
    INVALID_UTF8,
    LEXICAL_NESTING_TOO_DEEP,
    UNTERMINATED_STRING,
    UNTERMINATED_BLOCK_COMMENT,
    UNMATCHED_DELIMITER,
    UNCLOSED_DELIMITER,
    MALFORMED_MARKER,
    TOO_MANY_MARKERS,
    NESTED_MARKER,
    UNMATCHED_CLOSE_MARKER,
    UNCLOSED_MARKER,
    DUPLICATE_REGION,
    UNKNOWN_REGION,
    MISSING_REGION,
    REORDERED_REGIONS,
    REGION_SCOPE_MISMATCH,
    REGION_HASH_MISMATCH,
    CLASS_MISSING,
    CLASS_DUPLICATE,
    CLASS_SCOPE_MISMATCH,
    CLASS_KIND_MISMATCH,
    LOCAL_WIDGET_BASE_SHADOWED,
    QUALIFIED_WIDGET_BASE_UNSUPPORTED,
    STATEFUL_SOURCE_BINDING_UNSUPPORTED;

    /** Whether this diagnostic identifies a source shape the v1 binder cannot prove. */
    public boolean isUnsupportedSourceShape() {
        return switch (this) {
            case INVALID_UTF8,
                    UNKNOWN_REGION,
                    LOCAL_WIDGET_BASE_SHADOWED,
                    QUALIFIED_WIDGET_BASE_UNSUPPORTED,
                    STATEFUL_SOURCE_BINDING_UNSUPPORTED -> true;
            default -> false;
        };
    }
}

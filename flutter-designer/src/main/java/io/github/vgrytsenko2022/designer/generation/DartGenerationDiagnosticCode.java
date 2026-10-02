package io.github.vgrytsenko2022.designer.generation;

/** Stable machine-readable failures produced by deterministic Dart generation. */
public enum DartGenerationDiagnosticCode {
    MODEL_INVALID,
    UNSUPPORTED_WIDGET_KIND,
    DART_EXPRESSION_UNSUPPORTED,
    INVALID_UNICODE,
    IMPORT_LIMIT,
    OUTPUT_SIZE_LIMIT,
    SYMBOL_PROBE_LIMIT,
    INTERNAL_CATALOG_INCONSISTENCY
}

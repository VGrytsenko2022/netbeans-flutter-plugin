package io.github.vgrytsenko2022.project.theme;

/** Outcome of loading and verifying the project-wide theme contract. */
public enum FlutterProjectThemeLoadStatus {
    MISSING,
    INVALID_DESCRIPTOR,
    GENERATED_DART_MISSING,
    GENERATED_DART_HASH_MISMATCH,
    IO_ERROR,
    VALID
}

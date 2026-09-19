package io.github.vgrytsenko2022.designer.move;

import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityLimits;

/** Independent resource limits for one project-wide Designer move check. */
public record DartMoveDependencyLimits(
        int maxSourceFiles,
        int maxSourceBytes,
        long maxTotalSourceBytes,
        int maxDirectivesPerSource,
        int maxUriCharacters,
        int maxLexicalNesting) {

    public static final int DEFAULT_MAX_SOURCE_FILES = 4096;
    public static final long DEFAULT_MAX_TOTAL_SOURCE_BYTES = 64L * 1024L * 1024L;
    public static final int DEFAULT_MAX_DIRECTIVES_PER_SOURCE = 1024;
    public static final int DEFAULT_MAX_URI_CHARACTERS = 4096;
    public static final int DEFAULT_MAX_LEXICAL_NESTING = 128;

    public DartMoveDependencyLimits {
        positive(maxSourceFiles, "maxSourceFiles");
        positive(maxSourceBytes, "maxSourceBytes");
        if (maxTotalSourceBytes <= 0) {
            throw new IllegalArgumentException(
                    "maxTotalSourceBytes must be greater than zero");
        }
        positive(maxDirectivesPerSource, "maxDirectivesPerSource");
        positive(maxUriCharacters, "maxUriCharacters");
        positive(maxLexicalNesting, "maxLexicalNesting");
    }

    public static DartMoveDependencyLimits defaults() {
        return new DartMoveDependencyLimits(
                DEFAULT_MAX_SOURCE_FILES,
                DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES,
                DEFAULT_MAX_TOTAL_SOURCE_BYTES,
                DEFAULT_MAX_DIRECTIVES_PER_SOURCE,
                DEFAULT_MAX_URI_CHARACTERS,
                DEFAULT_MAX_LEXICAL_NESTING);
    }

    private static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}

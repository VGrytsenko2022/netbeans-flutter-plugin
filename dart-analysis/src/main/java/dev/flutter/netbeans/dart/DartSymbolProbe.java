package dev.flutter.netbeans.dart;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Expected analyzer navigation target for one exact candidate symbol
 * occurrence.
 *
 * <p>{@code expectedLibraryUri} is retained as caller identity context. The
 * native {@code analysis.getNavigation} response does not expose an
 * import/export URI graph, so this analyzer slice proves target-root
 * containment and optional target kind but does not independently verify that
 * URI.</p>
 */
public record DartSymbolProbe(
        String id,
        int offset,
        int length,
        String expectedSymbolName,
        String expectedLibraryUri,
        Path expectedTargetRoot,
        Optional<String> expectedTargetKind,
        Optional<DartStaticTypeProbe> staticTypeProbe) {

    public DartSymbolProbe(
            String id,
            int offset,
            int length,
            String expectedSymbolName,
            String expectedLibraryUri,
            Path expectedTargetRoot,
            Optional<String> expectedTargetKind) {
        this(id, offset, length, expectedSymbolName, expectedLibraryUri,
                expectedTargetRoot, expectedTargetKind, Optional.empty());
    }

    public DartSymbolProbe {
        id = requireText(id, "id");
        if (offset < 0 || length <= 0) {
            throw new IllegalArgumentException("probe offset must be non-negative and length positive");
        }
        expectedSymbolName = requireText(expectedSymbolName, "expectedSymbolName");
        expectedLibraryUri = requireText(expectedLibraryUri, "expectedLibraryUri");
        Objects.requireNonNull(expectedTargetRoot, "expectedTargetRoot");
        if (!expectedTargetRoot.isAbsolute()) {
            throw new IllegalArgumentException("expectedTargetRoot must be absolute");
        }
        expectedTargetRoot = expectedTargetRoot.normalize();
        expectedTargetKind = Objects.requireNonNull(
                expectedTargetKind, "expectedTargetKind")
                .map(value -> requireText(value, "expectedTargetKind"));
        staticTypeProbe = Objects.requireNonNull(
                staticTypeProbe, "staticTypeProbe");
        staticTypeProbe.ifPresent(probe -> {
            if (probe.expressionOffset() > offset
                    || Math.addExact(offset, length)
                    > probe.expressionEndOffset()) {
                throw new IllegalArgumentException(
                        "static-type expression must contain its terminal symbol probe");
            }
        });
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}

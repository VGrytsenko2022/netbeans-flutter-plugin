package dev.flutter.netbeans.dart;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Exact, bounded analyzer-only proof request for one generated expression.
 *
 * <p>A separate analyzer context constructs a second in-memory overlay with
 * proof-owned strict-casts options and assigns the exact expression to the
 * exact expected type. The typed initializer preserves downward
 * inference for generic constructor and factory invocations. In conjunction
 * with original call-site analysis, it rejects {@code dynamic} and wrong generic
 * instantiations. Legacy non-null requirements also reject nullable outer types
 * and {@code null}; an explicit Radio source-type identity may request nullable
 * values and adds independent non-dynamic and registry-consumption checks. Neither result is accepted
 * in isolation.</p>
 */
public record DartStaticTypeProbe(
        int expressionOffset,
        int expressionLength,
        int importInsertionOffset,
        int statementInsertionOffset,
        String expectedDartType,
        String expectedTypeLibraryUri,
        Optional<String> sourceTypeOverride) {

    private static final Pattern EXPECTED_TYPE = Pattern.compile(
            "(?:[A-Za-z][A-Za-z0-9_]*(?:<[A-Za-z][A-Za-z0-9_]*\\??>)?|Object\\?)");
    private static final Pattern SOURCE_TYPE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\??");
    private static final Pattern LIBRARY_URI = Pattern.compile(
            "(?:dart:[a-z][a-z0-9_.]*|package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart)");

    public DartStaticTypeProbe(int expressionOffset, int expressionLength, int importInsertionOffset,
            int statementInsertionOffset, String expectedDartType, String expectedTypeLibraryUri) {
        this(expressionOffset, expressionLength, importInsertionOffset, statementInsertionOffset,
                expectedDartType, expectedTypeLibraryUri, Optional.empty());
    }

    public DartStaticTypeProbe {
        if (expressionOffset < 0 || expressionLength <= 0) {
            throw new IllegalArgumentException(
                    "static-type expression offset must be non-negative and length positive");
        }
        if (importInsertionOffset < 0 || statementInsertionOffset < 0
                || importInsertionOffset > statementInsertionOffset
                || statementInsertionOffset > expressionOffset) {
            throw new IllegalArgumentException(
                    "static-type proof insertion offsets are invalid");
        }
        expectedDartType = Objects.requireNonNull(
                expectedDartType, "expectedDartType");
        if (expectedDartType.length() > 128
                || !EXPECTED_TYPE.matcher(expectedDartType).matches()) {
            throw new IllegalArgumentException(
                    "expected Dart type must use the closed simple/generic form");
        }
        Objects.requireNonNull(sourceTypeOverride, "sourceTypeOverride");
        if (sourceTypeOverride.isPresent() && (sourceTypeOverride.orElseThrow().length() > 256
                || !SOURCE_TYPE.matcher(sourceTypeOverride.orElseThrow()).matches()
                || !java.util.Set.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>").contains(expectedDartType))) {
            throw new IllegalArgumentException("Source type override must be a closed generated Radio type identity");
        }
        expectedTypeLibraryUri = Objects.requireNonNull(
                expectedTypeLibraryUri, "expectedTypeLibraryUri");
        if (expectedTypeLibraryUri.length() > 512
                || !LIBRARY_URI.matcher(expectedTypeLibraryUri).matches()
                || expectedTypeLibraryUri.contains("//")
                || expectedTypeLibraryUri.contains("/./")
                || expectedTypeLibraryUri.contains("/../")) {
            throw new IllegalArgumentException(
                    "expected type library URI must be canonical and bounded");
        }
    }

    public int expressionEndOffset() {
        return Math.addExact(expressionOffset, expressionLength);
    }
}

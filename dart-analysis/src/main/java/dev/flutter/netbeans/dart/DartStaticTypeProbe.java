package dev.flutter.netbeans.dart;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact, bounded analyzer-only proof request for one generated expression.
 *
 * <p>A separate analyzer context constructs a second in-memory overlay with
 * proof-owned strict-casts options and assigns the exact expression to the
 * exact non-null expected type. The typed initializer preserves downward
 * inference for generic constructor and factory invocations. In conjunction
 * with original call-site analysis, it rejects {@code dynamic}, nullable outer types,
 * {@code null}, and wrong generic instantiations. Neither result is accepted
 * in isolation.</p>
 */
public record DartStaticTypeProbe(
        int expressionOffset,
        int expressionLength,
        int importInsertionOffset,
        int statementInsertionOffset,
        String expectedDartType,
        String expectedTypeLibraryUri) {

    private static final Pattern EXPECTED_TYPE = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_]*(?:<[A-Za-z][A-Za-z0-9_]*\\??>)?");
    private static final Pattern LIBRARY_URI = Pattern.compile(
            "(?:dart:[a-z][a-z0-9_.]*|package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart)");

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

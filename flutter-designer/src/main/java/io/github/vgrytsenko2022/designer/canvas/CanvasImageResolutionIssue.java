package io.github.vgrytsenko2022.designer.canvas;

import java.util.Arrays;
import java.util.Objects;

/** Concrete fail-closed reason why one logical asset has no Canvas bytes. */
public record CanvasImageResolutionIssue(
        CanvasImageAssetId assetId,
        Code code,
        String reason) implements Comparable<CanvasImageResolutionIssue> {

    public CanvasImageResolutionIssue {
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank() || reason.length() > 1024) {
            throw new IllegalArgumentException(
                    "Canvas image resolution reason must contain 1..1024 characters");
        }
        for (int index = 0; index < reason.length(); index++) {
            char value = reason.charAt(index);
            if (Character.isISOControl(value)
                    && value != '\n' && value != '\r' && value != '\t') {
                throw new IllegalArgumentException(
                        "Canvas image resolution reason contains a control character");
            }
        }
    }

    @Override
    public int compareTo(CanvasImageResolutionIssue other) {
        Objects.requireNonNull(other, "other");
        int byAsset = assetId.compareTo(other.assetId);
        return byAsset != 0 ? byAsset : code.compareTo(other.code);
    }

    public enum Code {
        UNDECLARED("undeclared"),
        MISSING("missing"),
        INVALID_PATH("invalidPath"),
        UNREADABLE("unreadable"),
        UNSUPPORTED_FORMAT("unsupportedFormat"),
        CORRUPT("corrupt"),
        BUDGET_EXCEEDED("budgetExceeded");

        private final String wireName;

        Code(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }

        public static Code fromWireName(String value) {
            Objects.requireNonNull(value, "value");
            return Arrays.stream(values())
                    .filter(candidate -> candidate.wireName.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown Canvas image resolution issue: " + value));
        }
    }
}

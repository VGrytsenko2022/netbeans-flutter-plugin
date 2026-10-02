package io.github.vgrytsenko2022.designer.canvas.runtime;

/** Bounded single-line text safe to hand from a backend boundary to UI code. */
final class CanvasFailureReason {
    static final int MAX_CODE_POINTS = 512;
    private static final int MAX_SOURCE_CODE_POINTS = 1_024;
    private static final String FALLBACK =
            "Canvas backend failed without a usable diagnostic";

    private CanvasFailureReason() {
    }

    static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return FALLBACK;
        }

        StringBuilder result = new StringBuilder(
                Math.min(value.length(), MAX_CODE_POINTS));
        int sourceIndex = 0;
        int sourceCodePoints = 0;
        int outputCodePoints = 0;
        boolean pendingSpace = false;
        boolean truncated = false;
        while (sourceIndex < value.length()
                && sourceCodePoints < MAX_SOURCE_CODE_POINTS
                && outputCodePoints < MAX_CODE_POINTS - 1) {
            int codePoint = value.codePointAt(sourceIndex);
            sourceIndex += Character.charCount(codePoint);
            sourceCodePoints++;
            if (Character.isISOControl(codePoint)
                    || Character.isWhitespace(codePoint)
                    || Character.isSpaceChar(codePoint)
                    || Character.getType(codePoint) == Character.FORMAT
                    || isSurrogate(codePoint)
                    || isBidirectionalControl(codePoint)) {
                pendingSpace = result.length() > 0;
                continue;
            }
            if (pendingSpace) {
                result.append(' ');
                outputCodePoints++;
                pendingSpace = false;
                if (outputCodePoints >= MAX_CODE_POINTS - 1) {
                    truncated = true;
                    break;
                }
            }
            result.appendCodePoint(codePoint);
            outputCodePoints++;
        }

        truncated |= sourceIndex < value.length();
        String normalized = result.toString().strip();
        if (normalized.isEmpty()) {
            return FALLBACK;
        }
        if (truncated) {
            normalized += "\u2026";
        }
        return normalized;
    }

    private static boolean isSurrogate(int codePoint) {
        return codePoint >= Character.MIN_SURROGATE
                && codePoint <= Character.MAX_SURROGATE;
    }

    private static boolean isBidirectionalControl(int codePoint) {
        return codePoint == 0x061C
                || codePoint == 0x200E
                || codePoint == 0x200F
                || (codePoint >= 0x202A && codePoint <= 0x202E)
                || (codePoint >= 0x2066 && codePoint <= 0x2069);
    }
}

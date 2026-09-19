package io.github.vgrytsenko2022.designer.canvas;

import java.util.Objects;

/** Exact Flutter toolchain identity that produced a Canvas presentation. */
public record CanvasEngineIdentity(
        String flutterVersion,
        String frameworkRevision,
        String engineRevision,
        String dartSdkVersion) {

    private static final int MAX_VERSION_CODE_POINTS = 128;

    public CanvasEngineIdentity {
        flutterVersion = boundedPrintableText(flutterVersion, "flutterVersion");
        frameworkRevision = boundedPrintableText(frameworkRevision, "frameworkRevision");
        engineRevision = boundedPrintableText(engineRevision, "engineRevision");
        dartSdkVersion = boundedPrintableText(dartSdkVersion, "dartSdkVersion");
    }

    private static String boundedPrintableText(String value, String label) {
        Objects.requireNonNull(value, label);
        requireWellFormedUnicode(value, label);
        if (!value.isEmpty()
                && (isSpace(value.codePointAt(0))
                        || isSpace(value.codePointBefore(value.length())))) {
            throw new IllegalArgumentException(
                    label + " must not contain leading or trailing whitespace");
        }
        int length = value.codePointCount(0, value.length());
        if (length == 0 || length > MAX_VERSION_CODE_POINTS) {
            throw new IllegalArgumentException(
                    label + " must contain between 1 and "
                    + MAX_VERSION_CODE_POINTS + " Unicode code points");
        }
        if (value.codePoints().anyMatch(CanvasEngineIdentity::unsafeSingleLineCodePoint)) {
            throw new IllegalArgumentException(label + " must be safe single-line text");
        }
        return value;
    }

    private static void requireWellFormedUnicode(String value, String label) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new IllegalArgumentException(
                            label + " must not contain an unpaired surrogate");
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException(
                        label + " must not contain an unpaired surrogate");
            }
        }
    }

    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    private static boolean unsafeSingleLineCodePoint(int codePoint) {
        return Character.isISOControl(codePoint)
                || codePoint == 0x2028
                || codePoint == 0x2029
                || Character.getType(codePoint) == Character.FORMAT;
    }
}

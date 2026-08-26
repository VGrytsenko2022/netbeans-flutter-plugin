package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/** Shared semantic validation that is independent from JSON parsing. */
final class CanvasWireValues {
    static final int MAX_VERSION_CODE_POINTS = 128;

    private CanvasWireValues() {
    }

    static String printableText(String value, String name, int maximumCodePoints) {
        Objects.requireNonNull(value, name);
        requireWellFormedUnicode(value, name);
        if (!value.isEmpty()
                && (isSpace(value.codePointAt(0))
                        || isSpace(value.codePointBefore(value.length())))) {
            throw new IllegalArgumentException(
                    name + " must not contain leading or trailing whitespace");
        }
        int length = value.codePointCount(0, value.length());
        if (length == 0 || length > maximumCodePoints) {
            throw new IllegalArgumentException(
                    name + " must contain between 1 and "
                            + maximumCodePoints + " Unicode code points");
        }
        if (value.codePoints().anyMatch(CanvasWireValues::unsafeSingleLineCodePoint)) {
            throw new IllegalArgumentException(
                    name + " must be safe single-line text");
        }
        return value;
    }

    static String diagnosticPath(String value, String name, int maximumCodePoints) {
        Objects.requireNonNull(value, name);
        requireWellFormedUnicode(value, name);
        int length = value.codePointCount(0, value.length());
        if (length > maximumCodePoints) {
            throw new IllegalArgumentException(
                    name + " must contain at most " + maximumCodePoints
                            + " Unicode code points");
        }
        if (value.codePoints().anyMatch(codePoint -> codePoint < 0x21
                || codePoint > 0x7E)) {
            throw new IllegalArgumentException(
                    name + " must contain only visible ASCII path characters");
        }
        return value;
    }

    static void requireWellFormedUnicode(String value, String name) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new IllegalArgumentException(
                            name + " must not contain an unpaired surrogate");
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException(
                        name + " must not contain an unpaired surrogate");
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

    static List<CanvasWireCapability> capabilities(
            Collection<CanvasWireCapability> source,
            String name) {
        Objects.requireNonNull(source, name);
        EnumSet<CanvasWireCapability> unique = EnumSet.noneOf(
                CanvasWireCapability.class);
        for (CanvasWireCapability capability : source) {
            if (!unique.add(Objects.requireNonNull(
                    capability, name + " contains null"))) {
                throw new IllegalArgumentException(
                        name + " contains a duplicate capability: " + capability.wireValue());
            }
        }
        return List.copyOf(new ArrayList<>(unique));
    }
}

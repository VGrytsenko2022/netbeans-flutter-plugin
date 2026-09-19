package io.github.vgrytsenko2022.designer.source;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Scanner-owned occurrence of the reviewed Designer class superclass.
 *
 * <p>The constructor is deliberately package-private: only the source scanner
 * package can mint this syntactic evidence. Both coordinate systems identify
 * the same token in the retained {@link OriginalDartBytes} identity. UTF-16
 * offsets are relative to the decoded Dart text after an optional UTF-8 BOM;
 * byte offsets are relative to the exact original byte snapshot and therefore
 * include that BOM.</p>
 */
public final class DartDesignerSuperclassOccurrence {
    public static final String SYMBOL_NAME = "StatelessWidget";

    private final OriginalDartBytes sourceIdentity;
    private final String className;
    private final String symbolName;
    private final int startUtf16;
    private final int endUtf16;
    private final int startByte;
    private final int endByte;

    DartDesignerSuperclassOccurrence(
            OriginalDartBytes sourceIdentity,
            String className,
            int startUtf16,
            int endUtf16,
            int startByte,
            int endByte) {
        this(sourceIdentity, className, SYMBOL_NAME, startUtf16, endUtf16, startByte, endByte);
    }

    DartDesignerSuperclassOccurrence(
            OriginalDartBytes sourceIdentity, String className, String symbolName,
            int startUtf16, int endUtf16, int startByte, int endByte) {
        this.sourceIdentity = Objects.requireNonNull(sourceIdentity, "sourceIdentity");
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
        this.className = className;
        if (!java.util.Set.of("StatelessWidget", "StatefulWidget", "State").contains(symbolName)) {
            throw new IllegalArgumentException("Unreviewed Designer superclass symbol: " + symbolName);
        }
        this.symbolName = symbolName;
        if (startUtf16 < 0 || endUtf16 <= startUtf16
                || startByte < 0 || endByte <= startByte) {
            throw new IllegalArgumentException(
                    "superclass occurrence ranges must be positive and ordered");
        }
        this.startUtf16 = startUtf16;
        this.endUtf16 = endUtf16;
        this.startByte = startByte;
        this.endByte = endByte;
        validateExactToken();
    }

    public String className() {
        return className;
    }

    public String symbolName() {
        return symbolName;
    }

    public int startUtf16() {
        return startUtf16;
    }

    public int endUtf16() {
        return endUtf16;
    }

    public int lengthUtf16() {
        return endUtf16 - startUtf16;
    }

    public int startByte() {
        return startByte;
    }

    public int endByte() {
        return endByte;
    }

    public int lengthBytes() {
        return endByte - startByte;
    }

    boolean belongsTo(OriginalDartBytes source) {
        return sourceIdentity == source;
    }

    private void validateExactToken() {
        byte[] source = sourceIdentity.copyBytes();
        if (endByte > source.length) {
            throw new IllegalArgumentException(
                    "superclass byte occurrence exceeds its source snapshot");
        }
        byte[] expected = symbolName.getBytes(StandardCharsets.UTF_8);
        if (!Arrays.equals(expected, Arrays.copyOfRange(source, startByte, endByte))) {
            throw new IllegalArgumentException(
                    "superclass byte occurrence does not select " + symbolName);
        }

        int bomBytes = utf8BomLength(source);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(source, bomBytes, source.length - bomBytes))
                    .toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IllegalArgumentException(
                    "superclass occurrence source is not strict UTF-8", invalidUtf8);
        }
        if (endUtf16 > text.length()
                || !text.substring(startUtf16, endUtf16).equals(symbolName)) {
            throw new IllegalArgumentException(
                    "superclass UTF-16 occurrence does not select " + symbolName);
        }
        if (byteOffsetAt(text, startUtf16, bomBytes) != startByte
                || byteOffsetAt(text, endUtf16, bomBytes) != endByte) {
            throw new IllegalArgumentException(
                    "superclass byte and UTF-16 occurrences select different text");
        }
    }

    private static int utf8BomLength(byte[] source) {
        return source.length >= 3
                && (source[0] & 0xFF) == 0xEF
                && (source[1] & 0xFF) == 0xBB
                && (source[2] & 0xFF) == 0xBF ? 3 : 0;
    }

    private static int byteOffsetAt(String text, int requestedUtf16, int bomBytes) {
        int utf16 = 0;
        int bytes = bomBytes;
        while (utf16 < requestedUtf16) {
            int codePoint = text.codePointAt(utf16);
            int characters = Character.charCount(codePoint);
            if (utf16 + characters > requestedUtf16) {
                throw new IllegalArgumentException(
                        "superclass UTF-16 range splits a surrogate pair");
            }
            bytes += utf8Length(codePoint);
            utf16 += characters;
        }
        return bytes;
    }

    private static int utf8Length(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        if (codePoint <= 0xFFFF) {
            return 3;
        }
        return 4;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof DartDesignerSuperclassOccurrence value
                && sourceIdentity.equals(value.sourceIdentity)
                && className.equals(value.className)
                && symbolName.equals(value.symbolName)
                && startUtf16 == value.startUtf16
                && endUtf16 == value.endUtf16
                && startByte == value.startByte
                && endByte == value.endByte;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceIdentity, className, symbolName, startUtf16, endUtf16,
                startByte, endByte);
    }

    @Override
    public String toString() {
        return "DartDesignerSuperclassOccurrence[className=" + className
                + ", symbolName=" + symbolName
                + ", startUtf16=" + startUtf16
                + ", endUtf16=" + endUtf16
                + ", startByte=" + startByte
                + ", endByte=" + endByte + "]";
    }
}

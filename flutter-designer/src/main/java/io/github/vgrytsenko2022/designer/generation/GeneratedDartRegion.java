package io.github.vgrytsenko2022.designer.generation;

import io.github.vgrytsenko2022.designer.source.DartManagedRegionHashing;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Immutable canonical UTF-8 payload for one generated managed region. */
public final class GeneratedDartRegion {
    private final DartManagedRegionId id;
    private final String payload;
    private final byte[] utf8Bytes;
    private final String normalizedSha256;

    static GeneratedDartRegion create(DartManagedRegionId id, String payload) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(payload, "payload");
        if (payload.indexOf('\r') >= 0 || !payload.endsWith("\n")
                || payload.endsWith("\n\n")) {
            throw new IllegalArgumentException(
                    "Generated Dart payload must use LF and exactly one terminal LF");
        }
        byte[] encoded;
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(payload));
            encoded = new byte[buffer.remaining()];
            buffer.get(encoded);
        } catch (CharacterCodingException malformedUnicode) {
            throw new IllegalArgumentException(
                    "Generated Dart payload contains invalid Unicode", malformedUnicode);
        }
        return new GeneratedDartRegion(
                id,
                payload,
                encoded,
                DartManagedRegionHashing.normalizedSha256(payload));
    }

    private GeneratedDartRegion(
            DartManagedRegionId id,
            String payload,
            byte[] utf8Bytes,
            String normalizedSha256) {
        this.id = id;
        this.payload = payload;
        this.utf8Bytes = utf8Bytes.clone();
        this.normalizedSha256 = normalizedSha256;
    }

    public DartManagedRegionId id() {
        return id;
    }

    public String payload() {
        return payload;
    }

    public byte[] utf8Bytes() {
        return utf8Bytes.clone();
    }

    public int utf8Size() {
        return utf8Bytes.length;
    }

    public String normalizedSha256() {
        return normalizedSha256;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof GeneratedDartRegion that
                && id == that.id
                && payload.equals(that.payload)
                && Arrays.equals(utf8Bytes, that.utf8Bytes)
                && normalizedSha256.equals(that.normalizedSha256);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, payload, normalizedSha256);
        return 31 * result + Arrays.hashCode(utf8Bytes);
    }

    @Override
    public String toString() {
        return "GeneratedDartRegion[" + id.wireName() + ", " + utf8Bytes.length + " bytes]";
    }
}

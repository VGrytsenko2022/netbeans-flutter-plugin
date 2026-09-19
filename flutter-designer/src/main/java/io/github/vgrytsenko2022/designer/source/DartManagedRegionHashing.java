package io.github.vgrytsenko2022.designer.source;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Canonical line-ending normalization and SHA-256 for managed Dart payloads. */
public final class DartManagedRegionHashing {

    private DartManagedRegionHashing() {
    }

    public static String normalizedSha256(String payload) {
        Objects.requireNonNull(payload, "payload");
        byte[] normalized;
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(normalize(payload)));
            normalized = new byte[encoded.remaining()];
            encoded.get(normalized);
        } catch (CharacterCodingException malformedUnicode) {
            throw new IllegalArgumentException(
                    "payload must contain only valid Unicode scalar values",
                    malformedUnicode);
        }
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(normalized));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }

    static String normalize(String payload) {
        StringBuilder normalized = new StringBuilder(payload.length() + 1);
        for (int index = 0; index < payload.length(); index++) {
            char current = payload.charAt(index);
            if (current == '\r') {
                if (index + 1 < payload.length() && payload.charAt(index + 1) == '\n') {
                    index++;
                }
                normalized.append('\n');
            } else {
                normalized.append(current);
            }
        }
        while (!normalized.isEmpty()
                && normalized.charAt(normalized.length() - 1) == '\n') {
            normalized.setLength(normalized.length() - 1);
        }
        normalized.append('\n');
        return normalized.toString();
    }
}

package dev.flutter.netbeans.designer.source;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Exact immutable byte snapshot of one bounded Dart source file. */
public final class OriginalDartBytes {
    private final byte[] bytes;
    private final int hashCode;

    private OriginalDartBytes(byte[] bytes) {
        this.bytes = bytes;
        hashCode = Arrays.hashCode(bytes);
    }

    static OriginalDartBytes copyOf(byte[] bytes, DartSourceIntegrityLimits limits) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(limits, "limits");
        if (bytes.length > limits.maxSourceBytes()) {
            throw new IllegalArgumentException("Dart source exceeds maxSourceBytes");
        }
        return new OriginalDartBytes(bytes.clone());
    }

    public int size() {
        return bytes.length;
    }

    public byte[] copyBytes() {
        return bytes.clone();
    }

    public boolean contentEquals(byte[] candidate) {
        return candidate != null && Arrays.equals(bytes, candidate);
    }

    public String sha256Hex() {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof OriginalDartBytes snapshot
                && Arrays.equals(bytes, snapshot.bytes);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "OriginalDartBytes[size=" + bytes.length
                + ", sha256=" + sha256Hex() + "]";
    }
}

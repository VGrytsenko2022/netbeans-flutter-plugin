package dev.flutter.netbeans.designer.codec;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Exact immutable byte snapshot of a bounded {@code .fd} file. */
public final class OriginalFdBytes {
    private final byte[] bytes;
    private final int hashCode;

    private OriginalFdBytes(byte[] bytes) {
        this.bytes = bytes;
        this.hashCode = Arrays.hashCode(bytes);
    }

    public static OriginalFdBytes copyOf(byte[] bytes, FdCodecLimits limits)
            throws FdInputLimitException {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(limits, "limits");
        if (bytes.length > limits.maxDocumentBytes()) {
            throw new FdInputLimitException(limits.maxDocumentBytes(), bytes.length);
        }
        return new OriginalFdBytes(bytes.clone());
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
            throw new IllegalStateException("The Java runtime does not provide SHA-256", impossible);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof OriginalFdBytes snapshot
                && Arrays.equals(bytes, snapshot.bytes);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "OriginalFdBytes[size=" + bytes.length + ", sha256=" + sha256Hex() + "]";
    }
}

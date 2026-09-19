package io.github.vgrytsenko2022.designer.codec;

/** Raised before copying input that exceeds the configured raw byte limit. */
public final class FdInputLimitException extends Exception {
    private final int maximumBytes;
    private final int actualBytes;

    public FdInputLimitException(int maximumBytes, int actualBytes) {
        super("Flutter Designer document is " + actualBytes
                + " bytes; the configured limit is " + maximumBytes + " bytes");
        if (maximumBytes <= 0) {
            throw new IllegalArgumentException("maximumBytes must be greater than zero");
        }
        if (actualBytes <= maximumBytes) {
            throw new IllegalArgumentException("actualBytes must exceed maximumBytes");
        }
        this.maximumBytes = maximumBytes;
        this.actualBytes = actualBytes;
    }

    public int maximumBytes() {
        return maximumBytes;
    }

    public int actualBytes() {
        return actualBytes;
    }
}

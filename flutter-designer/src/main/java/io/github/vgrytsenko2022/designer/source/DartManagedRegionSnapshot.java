package io.github.vgrytsenko2022.designer.source;

import java.util.regex.Pattern;

/** Byte-exact location and normalized hash of one discovered managed payload. */
public record DartManagedRegionSnapshot(
        String id,
        int payloadStartByte,
        int payloadEndByte,
        String normalizedSha256) {

    private static final Pattern REGION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9A-F]{64}");

    public DartManagedRegionSnapshot {
        if (id == null || !REGION_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("id must be a valid managed-region id");
        }
        if (payloadStartByte < 0 || payloadEndByte < payloadStartByte) {
            throw new IllegalArgumentException("payload byte offsets are invalid");
        }
        if (normalizedSha256 == null || !SHA_256.matcher(normalizedSha256).matches()) {
            throw new IllegalArgumentException("normalizedSha256 must be uppercase SHA-256");
        }
    }

    public int payloadLengthBytes() {
        return payloadEndByte - payloadStartByte;
    }
}

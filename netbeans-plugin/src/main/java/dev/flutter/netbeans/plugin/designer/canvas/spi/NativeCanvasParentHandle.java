package dev.flutter.netbeans.plugin.designer.canvas.spi;

import java.util.Objects;

/**
 * Opaque provider-owned identity for the native host parent.
 *
 * <p>The encoded value may be carried to a provider-specific runner launcher,
 * but domain model and persistence code must never interpret it.</p>
 */
public record NativeCanvasParentHandle(
        NativeCanvasPlatform platform,
        String encodedValue) {
    private static final int MAXIMUM_ENCODED_LENGTH = 128;

    public NativeCanvasParentHandle {
        Objects.requireNonNull(platform, "platform");
        encodedValue = Objects.requireNonNull(encodedValue, "encodedValue");
        if (platform == NativeCanvasPlatform.UNKNOWN) {
            throw new IllegalArgumentException(
                    "A native Canvas parent handle requires a concrete platform");
        }
        if (encodedValue.isBlank() || encodedValue.length() > MAXIMUM_ENCODED_LENGTH) {
            throw new IllegalArgumentException(
                    "Native Canvas parent handle encoding is blank or too long");
        }
        for (int index = 0; index < encodedValue.length(); index++) {
            char character = encodedValue.charAt(index);
            if (character < 0x21 || character > 0x7e) {
                throw new IllegalArgumentException(
                        "Native Canvas parent handle encoding must be printable ASCII");
            }
        }
    }

    @Override
    public String toString() {
        return platform + ":<opaque>";
    }
}

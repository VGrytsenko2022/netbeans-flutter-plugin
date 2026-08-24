package dev.flutter.netbeans.sdk;

import java.util.Objects;

public record SdkDetection<T>(T sdk, String source) {
    public SdkDetection {
        Objects.requireNonNull(sdk, "sdk");
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Detection source is required");
        }
    }
}

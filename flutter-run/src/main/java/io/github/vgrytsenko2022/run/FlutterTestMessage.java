package io.github.vgrytsenko2022.run;

import java.util.Objects;

/** Output or skip text emitted by one running test. */
public record FlutterTestMessage(
        long time,
        String messageType,
        String message) {

    public FlutterTestMessage {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Flutter test message type is required");
        }
        Objects.requireNonNull(message, "message");
    }
}

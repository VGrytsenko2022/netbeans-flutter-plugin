package io.github.vgrytsenko2022.run;

import java.util.Objects;

/** One error associated with a test id. */
public record FlutterTestError(
        long time,
        String message,
        String stackTrace,
        boolean failure) {

    public FlutterTestError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Flutter test error message is required");
        }
        stackTrace = Objects.requireNonNullElse(stackTrace, "");
    }
}

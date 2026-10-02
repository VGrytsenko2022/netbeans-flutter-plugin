package io.github.vgrytsenko2022.designer.canvas.transport;

import java.io.IOException;
import java.util.Objects;

/** Checked framing violation safe to report without echoing untrusted bytes. */
public final class CanvasProcessFramingException extends IOException {
    private final CanvasProcessFramingError error;

    CanvasProcessFramingException(
            CanvasProcessFramingError error,
            String message) {
        super(message);
        this.error = Objects.requireNonNull(error, "error");
    }

    public CanvasProcessFramingError error() {
        return error;
    }
}

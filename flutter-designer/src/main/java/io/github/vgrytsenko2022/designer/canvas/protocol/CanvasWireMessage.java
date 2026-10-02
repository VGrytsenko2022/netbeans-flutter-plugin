package io.github.vgrytsenko2022.designer.canvas.protocol;

import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import java.util.OptionalLong;

/** One immutable version 1 Canvas lifecycle control message. */
public sealed interface CanvasWireMessage permits
        CanvasHostHello,
        CanvasRunnerHello,
        CanvasHostClose,
        CanvasRunnerClosed,
        CanvasRunnerFailure {

    CanvasSessionId sessionId();

    long sequence();

    CanvasWireMessageType type();

    default OptionalLong replyTo() {
        return OptionalLong.empty();
    }
}

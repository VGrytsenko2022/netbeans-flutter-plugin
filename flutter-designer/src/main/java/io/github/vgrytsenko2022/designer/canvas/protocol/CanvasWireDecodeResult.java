package io.github.vgrytsenko2022.designer.canvas.protocol;

import java.util.Objects;

/** Total decode result for one already-framed, bounded control body. */
public sealed interface CanvasWireDecodeResult permits
        CanvasWireDecodeResult.Decoded,
        CanvasWireDecodeResult.Invalid {

    record Decoded(CanvasWireMessage message) implements CanvasWireDecodeResult {
        public Decoded {
            Objects.requireNonNull(message, "message");
        }
    }

    record Invalid(CanvasWireDiagnostic diagnostic) implements CanvasWireDecodeResult {
        public Invalid {
            Objects.requireNonNull(diagnostic, "diagnostic");
        }
    }
}

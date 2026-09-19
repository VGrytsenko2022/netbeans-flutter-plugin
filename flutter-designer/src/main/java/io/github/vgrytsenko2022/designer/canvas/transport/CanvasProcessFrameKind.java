package io.github.vgrytsenko2022.designer.canvas.transport;

import java.util.Arrays;
import java.util.Optional;

/** Stable payload channel encoded in one version 1 process frame header. */
public enum CanvasProcessFrameKind {
    CONTROL_JSON(1),
    MODEL_JSON(2),
    CATALOG_JSON(3),
    IMAGE_BYTES(4);

    private final int wireCode;

    CanvasProcessFrameKind(int wireCode) {
        this.wireCode = wireCode;
    }

    public int wireCode() {
        return wireCode;
    }

    static Optional<CanvasProcessFrameKind> fromWireCode(int wireCode) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireCode == wireCode)
                .findFirst();
    }
}

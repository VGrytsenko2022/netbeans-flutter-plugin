package io.github.vgrytsenko2022.designer.canvas.protocol;

import java.util.Objects;

/** One bounded non-sensitive explanation for a rejected control message. */
public record CanvasWireDiagnostic(
        CanvasWireDiagnosticCode code,
        String path,
        String message) {
    private static final int MAX_PATH_CODE_POINTS = 2_048;
    private static final int MAX_MESSAGE_CODE_POINTS = 512;

    public CanvasWireDiagnostic {
        Objects.requireNonNull(code, "code");
        path = CanvasWireValues.diagnosticPath(
                path, "path", MAX_PATH_CODE_POINTS);
        message = CanvasWireValues.printableText(
                message, "message", MAX_MESSAGE_CODE_POINTS);
    }
}

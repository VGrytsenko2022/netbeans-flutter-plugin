package io.github.vgrytsenko2022.designer.canvas.protocol;

/** Trusted message could not be encoded within the active wire policy. */
public final class CanvasWireEncodeException extends Exception {
    private final CanvasWireDiagnosticCode code;

    CanvasWireEncodeException(
            CanvasWireDiagnosticCode code,
            String message,
            Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public CanvasWireDiagnosticCode code() {
        return code;
    }
}

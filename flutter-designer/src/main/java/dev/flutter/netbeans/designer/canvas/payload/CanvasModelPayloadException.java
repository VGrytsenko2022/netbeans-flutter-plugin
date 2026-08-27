package dev.flutter.netbeans.designer.canvas.payload;

/** Failure to create one bounded canonical read-only Canvas model payload. */
public final class CanvasModelPayloadException extends Exception {
    public CanvasModelPayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public CanvasModelPayloadException(String message) {
        super(message);
    }
}

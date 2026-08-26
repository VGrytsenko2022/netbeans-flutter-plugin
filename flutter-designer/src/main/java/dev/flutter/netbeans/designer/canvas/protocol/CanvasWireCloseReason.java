package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.Arrays;

/** Host-owned reason for closing one Canvas backend session. */
public enum CanvasWireCloseReason {
    FORM_CLOSED("formClosed"),
    IDE_SHUTDOWN("ideShutdown"),
    RESTART("restart"),
    BACKEND_REPLACED("backendReplaced");

    private final String wireValue;

    CanvasWireCloseReason(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    static CanvasWireCloseReason parse(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Canvas close reason: " + value));
    }
}

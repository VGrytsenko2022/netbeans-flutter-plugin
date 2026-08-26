package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.Arrays;

/** Known control-message kinds for protocol version 1. */
public enum CanvasWireMessageType {
    HOST_HELLO("host.hello"),
    RUNNER_HELLO("runner.hello"),
    HOST_CLOSE("host.close"),
    RUNNER_CLOSED("runner.closed"),
    RUNNER_FAILURE("runner.failure");

    private final String wireValue;

    CanvasWireMessageType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    static CanvasWireMessageType parse(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Canvas message type: " + value));
    }
}

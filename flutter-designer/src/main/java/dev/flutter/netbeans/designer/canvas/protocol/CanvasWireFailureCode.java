package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.Arrays;

/** Stable runner failure categories exposed by protocol version 1. */
public enum CanvasWireFailureCode {
    UNSUPPORTED_VERSION("unsupportedVersion"),
    INCOMPATIBLE_CAPABILITIES("incompatibleCapabilities"),
    INVALID_REQUEST("invalidRequest"),
    RESOURCE_LIMIT("resourceLimit"),
    RUNNER_START_FAILED("runnerStartFailed"),
    INTERNAL_FAILURE("internalFailure");

    private final String wireValue;

    CanvasWireFailureCode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    static CanvasWireFailureCode parse(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Canvas failure code: " + value));
    }
}

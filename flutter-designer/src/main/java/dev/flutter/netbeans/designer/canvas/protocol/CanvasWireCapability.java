package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.Arrays;

/** Explicit read-only capabilities understood by protocol version 1. */
public enum CanvasWireCapability {
    READ_ONLY_RENDER("readOnly.render"),
    READ_ONLY_LAYOUT("readOnly.layout"),
    READ_ONLY_SELECTION("readOnly.selection");

    private final String wireValue;

    CanvasWireCapability(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    static CanvasWireCapability parse(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Canvas capability: " + value));
    }
}

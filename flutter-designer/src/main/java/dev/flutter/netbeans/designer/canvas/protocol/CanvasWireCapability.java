package dev.flutter.netbeans.designer.canvas.protocol;

import java.util.Arrays;

/** Explicit capabilities understood by protocol version 1. */
public enum CanvasWireCapability {
    READ_ONLY_RENDER("readOnly.render"),
    READ_ONLY_LAYOUT("readOnly.layout"),
    READ_ONLY_SELECTION("readOnly.selection"),
    SURFACE_PRESENTATION_V1("surface.presentation.v1"),
    PALETTE_DROP_CATALOG_INSERT_V1("palette.drop.catalogInsert.v1"),
    PALETTE_DROP_SOURCE_AWARE_V1("palette.drop.sourceAware.v1"),
    DELETE_SELECTED_WIDGET_V1("widget.deleteSelection.v1"),
    WIDGET_MOVE_PREVIEW_V1("widget.movePreview.v1"),
    VIEWPORT_PRESENTATION_V1("viewport.presentation.v1");

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

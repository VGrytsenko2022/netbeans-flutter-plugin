package dev.flutter.netbeans.plugin.designer;

import java.util.Objects;

/** Concrete user-facing state for one embedded native Flutter Canvas view. */
record FlutterDesignerNativeCanvasStatus(
        Stage stage,
        String summary,
        String detail) {
    FlutterDesignerNativeCanvasStatus {
        stage = Objects.requireNonNull(stage, "stage");
        summary = requireText(summary, "summary");
        detail = requireText(detail, "detail");
    }

    boolean busy() {
        return stage == Stage.PREPARING || stage == Stage.STARTING;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    enum Stage {
        PREPARING,
        STARTING,
        RUNNING,
        UNAVAILABLE,
        FAILED,
        STOPPED
    }
}

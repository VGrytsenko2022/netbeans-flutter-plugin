package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasTargetPlatform;
import java.util.Objects;

/** Selects the Canvas backend without activating or constructing either host. */
final class FlutterDesignerCanvasBackendSelector {
    private final boolean exactWebEnabled;

    /** Production default keeps the existing native Canvas route for every target. */
    static FlutterDesignerCanvasBackendSelector production() {
        return new FlutterDesignerCanvasBackendSelector(false);
    }

    /** Package-private feature seam for focused routing tests. */
    FlutterDesignerCanvasBackendSelector(boolean exactWebEnabled) {
        this.exactWebEnabled = exactWebEnabled;
    }

    boolean exactWebEnabled() {
        return exactWebEnabled;
    }

    Backend select(CanvasTargetPlatform targetPlatform) {
        Objects.requireNonNull(targetPlatform, "targetPlatform");
        return exactWebEnabled && targetPlatform == CanvasTargetPlatform.WEB
                ? Backend.EXACT_WEB
                : Backend.NATIVE;
    }

    enum Backend {
        NATIVE,
        EXACT_WEB
    }
}

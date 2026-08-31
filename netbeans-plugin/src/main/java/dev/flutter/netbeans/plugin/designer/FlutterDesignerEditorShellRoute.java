package dev.flutter.netbeans.plugin.designer;

/**
 * Single product gate for the plugin-owned Designer editor shell.
 *
 * <p>The shell deliberately remains dormant while Source/History parity,
 * restart reconstruction, in-flight edit recovery and runtime acceptance of
 * its clone-safe close permit are still being proven. Keeping this decision
 * next to the creation seam prevents an incomplete shell from becoming
 * reachable through an unrelated Canvas feature flag.</p>
 */
final class FlutterDesignerEditorShellRoute {
    static final boolean PRODUCTION_ENABLED = false;

    private FlutterDesignerEditorShellRoute() {
    }
}

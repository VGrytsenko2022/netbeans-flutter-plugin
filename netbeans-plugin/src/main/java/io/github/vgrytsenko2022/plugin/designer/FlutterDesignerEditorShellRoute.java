package io.github.vgrytsenko2022.plugin.designer;

/**
 * Single product gate for the plugin-owned Designer editor shell.
 *
 * <p>The shell deliberately remains dormant while reparent/post-removal
 * bypasses, Source/History and restart/runtime parity, exact-Web product
 * binding and physical acceptance are still being proven. Support-wide
 * CloseCookie/shell-owned Close All, operation-owned exact-proof
 * Rename/Delete/Cut-Move continuations, stale-close Canvas recovery and the
 * RELEASE300 Close-Mode latch are implemented, but they do not close those
 * product gates. Keeping this decision next to the creation seam prevents an
 * incomplete shell from becoming reachable through an unrelated Canvas feature
 * flag.</p>
 */
final class FlutterDesignerEditorShellRoute {
    static final boolean PRODUCTION_ENABLED = false;

    private FlutterDesignerEditorShellRoute() {
    }
}

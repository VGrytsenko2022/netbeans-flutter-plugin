package dev.flutter.netbeans.designer.canvas;

/** Result of checking renderer evidence or an intent against the current Canvas. */
public enum CanvasAdmission {
    ACCEPTED,
    CLOSED,
    STALE_SESSION,
    STALE_DOCUMENT,
    STALE_REVISION,
    STALE_FRAME,
    OUT_OF_ORDER_FRAME,
    STALE_LAYOUT,
    OUT_OF_ORDER_LAYOUT,
    NOT_READY
}

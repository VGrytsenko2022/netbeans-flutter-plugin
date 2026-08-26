package dev.flutter.netbeans.designer.canvas.protocol;

/** Result of checking one runner message against the host-owned session. */
public enum CanvasWireSessionAdmission {
    ACCEPTED,
    WRONG_DIRECTION,
    WRONG_STATE,
    STALE_SESSION,
    NONCONTIGUOUS_SEQUENCE,
    WRONG_REPLY,
    CAPABILITY_ESCALATION,
    LIMIT_ESCALATION,
    NON_FATAL_STARTUP_FAILURE,
    CLOSED,
    FAILED;

    public boolean accepted() {
        return this == ACCEPTED;
    }
}

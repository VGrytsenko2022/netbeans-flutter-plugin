package io.github.vgrytsenko2022.designer.command;

/** Closed status set shared by immutable command-session operations. */
public enum DesignerCommandStatus {
    READY,
    APPLIED,
    UNDONE,
    REDONE,
    NO_CHANGE,
    NOT_AVAILABLE,
    REJECTED,
    CONFLICT,
    UNSUPPORTED,
    UNAVAILABLE,
    LIMIT_EXCEEDED
}

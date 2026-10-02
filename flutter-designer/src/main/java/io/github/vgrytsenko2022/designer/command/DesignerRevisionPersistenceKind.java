package io.github.vgrytsenko2022.designer.command;

/** How a revision differs from the command session's current durable anchor. */
public enum DesignerRevisionPersistenceKind {
    /** This is the exact durable anchor and requires no persistence. */
    BASELINE,
    /** Both canonical .fd and generated Dart candidate differ from the anchor. */
    PAIRED,
    /** Canonical .fd differs while generated Dart bytes remain unchanged. */
    FD_ONLY
}

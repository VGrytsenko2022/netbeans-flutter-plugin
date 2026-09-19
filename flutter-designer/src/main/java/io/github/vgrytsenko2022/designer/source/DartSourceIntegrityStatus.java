package io.github.vgrytsenko2022.designer.source;

/** Read-only classification of one paired on-disk Dart source snapshot. */
public enum DartSourceIntegrityStatus {
    /** Persisted marker, class, scope and declared hash metadata match on disk. */
    ON_DISK_DECLARED_MATCH,
    /** The understood source shape conflicts with persisted .fd metadata. */
    CONFLICT,
    /** The source uses a valid shape not yet supported by the verifier contract. */
    UNSUPPORTED,
    /** No trustworthy bounded source snapshot is available. */
    UNAVAILABLE
}

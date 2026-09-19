package io.github.vgrytsenko2022.designer.source;

/** Read-only classification of one on-disk source/model/generator comparison. */
public enum DartThreeWayIntegrityStatus {
    /** On-disk payloads, persisted hashes and current generated payloads agree. */
    ON_DISK_THREE_WAY_MATCH,
    /** At least one understood side of the three-way comparison differs. */
    CONFLICT,
    /** A source or generation shape cannot be proved by the current contract. */
    UNSUPPORTED,
    /** A complete trustworthy bounded comparison could not be produced. */
    UNAVAILABLE
}

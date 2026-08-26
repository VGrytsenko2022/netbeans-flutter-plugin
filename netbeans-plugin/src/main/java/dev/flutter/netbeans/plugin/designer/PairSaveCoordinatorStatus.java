package dev.flutter.netbeans.plugin.designer;

/** Observable lifecycle of the one save owner for a paired designer object. */
enum PairSaveCoordinatorStatus {
    CLEAN,
    DIRTY_SOURCE,
    PREPARING_PAIR,
    PREPARING_REPLACEMENT,
    STAGED_PAIR,
    SAVING_SOURCE,
    SAVING_PAIR,
    SAVING_FD_ONLY,
    EXTERNAL_CONFLICT,
    SAVE_FAILED,
    RECOVERY_CONFLICT
}

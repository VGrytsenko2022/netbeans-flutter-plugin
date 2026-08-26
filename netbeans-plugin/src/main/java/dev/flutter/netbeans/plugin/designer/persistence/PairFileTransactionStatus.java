package dev.flutter.netbeans.plugin.designer.persistence;

/** Final, fail-closed outcome of a pair-file transaction attempt. */
public enum PairFileTransactionStatus {
    /** Both requested byte snapshots were written and verified. */
    COMMITTED,
    /** Both baselines were current and neither file needed a write. */
    UNCHANGED,
    /** The request or an exact on-disk baseline was rejected before any write. */
    REJECTED,
    /** An operational failure happened before any write was attempted. */
    FAILED,
    /** A write failed, but both exact old snapshots were restored and verified. */
    ROLLED_BACK,
    /** At least one old snapshot could not be restored and verified. */
    RECOVERY_CONFLICT
}

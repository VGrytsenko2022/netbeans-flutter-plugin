package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;

/** Atomic live-document operations shared by the Designer editor edge and tests. */
final class LiveDartDocumentBridge {
    private LiveDartDocumentBridge() {
    }

    /** Explicit post-commit policy for user-owned Dart outside managed guards. */
    enum CommittedPairContentPolicy {
        /** The entire marker-bearing live source must equal the committed candidate. */
        EXACT_FULL_CONTENT,
        /** Managed regions must match; newer unmanaged Source bytes are retained. */
        EXACT_MANAGED_CONTENT
    }

    static LiveDartDocumentSnapshot snapshot(
            StyledDocument document,
            DartGuardedSectionsProvider provider) throws IOException {
        return LiveDartDocumentSnapshot.capture(document, provider);
    }

    static LiveDartDocumentSnapshot applyManagedRegions(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes) throws IOException {
        return applyManagedRegions(
                provider,
                expected,
                generated,
                expectedCandidateBytes,
                applied -> { });
    }

    /**
     * Applies managed regions and supplies their exact fresh document identity
     * to a finalizer while the same atomic document lock is still held. A
     * checked or runtime finalizer failure rolls the live document back to the
     * exact predecessor before the failure is returned.
     */
    static LiveDartDocumentSnapshot applyManagedRegions(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes,
            AppliedSnapshotFinalizer finalizer) throws IOException {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(generated, "generated");
        Objects.requireNonNull(finalizer, "finalizer");
        byte[] candidate = LiveDartDocumentSnapshot.strictWritableUtf8(
                expectedCandidateBytes);
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Managed Dart regions may be applied only on the Event Dispatch Thread");
        }

        StyledDocument document = expected.documentIdentity();
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<LiveDartDocumentSnapshot> applied = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            boolean mutationStarted = false;
            try {
                LiveDartDocumentSnapshot current = snapshot(document, provider);
                if (!expected.sameEvidence(current)) {
                    throw new IOException(
                            "The live Dart document identity, revision or guards changed before apply");
                }

                mutationStarted = true;
                applySection(
                        expected.build(),
                        generated.build().payload());
                applySection(
                        expected.imports(),
                        generated.imports().payload());

                LiveDartDocumentSnapshot result = snapshot(document, provider);
                if (result.documentVersion() <= expected.documentVersion()) {
                    throw new IOException(
                            "The live Dart document version did not advance during apply");
                }
                if (!Arrays.equals(candidate, result.markerBearingUtf8())) {
                    throw new IOException(
                            "The applied live Dart document differs from the expected generated candidate: "
                            + mismatch(candidate, result.markerBearingUtf8()));
                }
                finalizer.finish(result);
                applied.set(result);
            } catch (IOException | RuntimeException | Error applyFailure) {
                failure.set(mutationStarted
                        ? rollback(
                                atomicDocument,
                                provider,
                                expected,
                                applyFailure)
                        : asIOException(applyFailure));
            }
        });

        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot result = applied.get();
        if (result == null) {
            throw new IOException("The managed Dart apply result is unavailable");
        }
        return result;
    }

    /**
     * Restores the exact managed-region bytes captured before a prepared pair
     * was applied. The caller must still own the preparation lease: this
     * method refuses to overwrite a document that changed after the apply.
     */
    static LiveDartDocumentSnapshot restoreManagedRegions(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expectedApplied,
            LiveDartDocumentSnapshot restore,
            Runnable finalizer) throws IOException {
        Objects.requireNonNull(finalizer, "finalizer");
        return restoreManagedRegions(
                provider,
                expectedApplied,
                restore,
                restored -> finalizer.run());
    }

    /**
     * Restores managed regions and supplies the fresh restored identity to a
     * finalizer while the same document atomic lock is still held.
     */
    static LiveDartDocumentSnapshot restoreManagedRegions(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expectedApplied,
            LiveDartDocumentSnapshot restore,
            RestoredSnapshotFinalizer finalizer) throws IOException {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(expectedApplied, "expectedApplied");
        Objects.requireNonNull(restore, "restore");
        Objects.requireNonNull(finalizer, "finalizer");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Managed Dart regions may be restored only on the Event Dispatch Thread");
        }
        if (expectedApplied.documentIdentity() != restore.documentIdentity()) {
            throw new IOException(
                    "Cannot restore managed Dart regions from another live document");
        }

        StyledDocument document = expectedApplied.documentIdentity();
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<LiveDartDocumentSnapshot> restored = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            try {
                LiveDartDocumentSnapshot current = snapshot(document, provider);
                if (!expectedApplied.sameEvidence(current)) {
                    throw new IOException(
                            "The live Dart document changed after the prepared pair was applied");
                }
                restoreSection(restore.imports());
                restoreSection(restore.build());
                LiveDartDocumentSnapshot result = snapshot(document, provider);
                if (result.documentVersion() <= expectedApplied.documentVersion()) {
                    throw new IOException(
                            "The live Dart document version did not advance during restore");
                }
                if (!Arrays.equals(
                        restore.markerBearingUtf8(), result.markerBearingUtf8())) {
                    throw new IOException(
                            "The restored live Dart document differs from its exact pre-apply snapshot: "
                            + mismatch(restore.markerBearingUtf8(), result.markerBearingUtf8()));
                }
                finalizer.finish(result);
                restored.set(result);
            } catch (IOException | RuntimeException | Error restoreFailure) {
                failure.set(asRestoreIOException(restoreFailure));
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot result = restored.get();
        if (result == null) {
            throw new IOException("The managed Dart restore result is unavailable");
        }
        return result;
    }

    /**
     * Exact document-CAS barrier used when apply failed before an applied
     * snapshot could be published. The finalizer runs while the same document
     * atomic lock still protects the verified revision.
     */
    static void verifyAndFinalize(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expected,
            Runnable finalizer) throws IOException {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(finalizer, "finalizer");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "A Dart preparation may be finalized only on the Event Dispatch Thread");
        }

        StyledDocument document = expected.documentIdentity();
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            try {
                LiveDartDocumentSnapshot current = snapshot(document, provider);
                if (!expected.sameEvidence(current)) {
                    throw new IOException(
                            "The live Dart document changed before preparation finalization");
                }
                finalizer.run();
            } catch (IOException | RuntimeException | Error finalizationFailure) {
                failure.set(asRestoreIOException(finalizationFailure));
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    /**
     * Read-only post-commit barrier for a history-preserving pair save.
     *
     * <p>The exact guarded document is snapshotted under its atomic lock. No
     * Undo entry, savepoint or document byte is changed. The fresh snapshot is
     * returned only after the atomic section has ended, so callers may perform
     * coordinator re-anchoring without holding the document lock.</p>
     */
    static LiveDartDocumentSnapshot verifyCommittedPairWithoutHistoryMutation(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expectedCommitted,
            CommittedPairContentPolicy contentPolicy) throws IOException {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(expectedCommitted, "expectedCommitted");
        Objects.requireNonNull(contentPolicy, "contentPolicy");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "A committed Dart pair may be verified only on the Event Dispatch Thread");
        }

        StyledDocument document = expectedCommitted.documentIdentity();
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<LiveDartDocumentSnapshot> verified = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            try {
                LiveDartDocumentSnapshot current = snapshot(document, provider);
                if (!expectedCommitted.sameManagedContent(current)) {
                    throw new IOException(
                            "The live managed Dart regions changed while the exact pair was committing");
                }
                if (contentPolicy
                            == CommittedPairContentPolicy.EXACT_FULL_CONTENT
                        && !Arrays.equals(
                                expectedCommitted.markerBearingUtf8(),
                                current.markerBearingUtf8())) {
                    throw new IOException(
                            "The complete live Dart source differs from the exact committed candidate");
                }
                verified.set(current);
            } catch (IOException | RuntimeException verificationFailure) {
                failure.set(asIOException(verificationFailure));
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot result = verified.get();
        if (result == null) {
            throw new IOException("The verified committed live Dart snapshot is unavailable");
        }
        return result;
    }

    /**
     * Post-commit EDT/document barrier. It verifies that no managed Undo raced
     * the durable pair commit and clears the pre-commit Undo history while the
     * document atomic lock is still held. Unmanaged edits are allowed and stay
     * dirty for a later ordinary Source save.
     */
    static LiveDartDocumentSnapshot sealCommittedPair(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expectedManaged,
            Runnable discardUndoHistory) throws IOException {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(expectedManaged, "expectedManaged");
        Objects.requireNonNull(discardUndoHistory, "discardUndoHistory");
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "A committed Dart pair may be sealed only on the Event Dispatch Thread");
        }

        StyledDocument document = expectedManaged.documentIdentity();
        AtomicLockDocument atomicDocument = LineDocumentUtils.asRequired(
                document, AtomicLockDocument.class);
        AtomicReference<LiveDartDocumentSnapshot> sealed = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        atomicDocument.runAtomic(() -> {
            try {
                LiveDartDocumentSnapshot current = snapshot(document, provider);
                if (!expectedManaged.sameManagedContent(current)) {
                    throw new IOException(
                            "The live managed Dart regions changed while the exact pair was committing");
                }
                sealed.set(current);
            } catch (IOException | RuntimeException sealFailure) {
                failure.set(asIOException(sealFailure));
            } finally {
                try {
                    discardUndoHistory.run();
                } catch (RuntimeException undoFailure) {
                    IOException converted = new IOException(
                            "Cannot establish the post-commit Dart Undo barrier: "
                            + reason(undoFailure), undoFailure);
                    if (failure.get() == null) {
                        failure.set(converted);
                    } else {
                        failure.get().addSuppressed(converted);
                    }
                }
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot result = sealed.get();
        if (result == null) {
            throw new IOException("The committed live Dart snapshot is unavailable");
        }
        return result;
    }

    private static void applySection(
            LiveDartDocumentSnapshot.ManagedSection section,
            String generatedPayload) {
        if (!section.sectionIdentity().isValid()) {
            throw new IllegalStateException(
                    "The live Dart guard '" + section.id() + "' is no longer valid");
        }
        section.sectionIdentity().setText(section.replacementText(generatedPayload));
    }

    private static IOException rollback(
            AtomicLockDocument atomicDocument,
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expected,
            Throwable applyFailure) {
        IOException primary = asIOException(applyFailure);
        try {
            // The apply and this rollback are intentionally enclosed by the
            // same outer runAtomic. atomicUndo() kills that transaction's
            // compound edit instead of recording a forward edit followed by
            // a compensating edit in the native Source Undo history.
            atomicDocument.atomicUndo();
            LiveDartDocumentSnapshot restored = snapshot(
                    expected.documentIdentity(), provider);
            verifyExactPredecessor(expected, restored);
        } catch (IOException | RuntimeException | Error rollbackFailure) {
            IOException failed = new RecoveryFailure(
                    "Managed Dart apply failed and rollback could not restore the original document",
                    primary);
            failed.addSuppressed(rollbackFailure);
            return failed;
        }
        return primary;
    }

    private static void verifyExactPredecessor(
            LiveDartDocumentSnapshot expected,
            LiveDartDocumentSnapshot restored) throws IOException {
        if (expected.documentIdentity() != restored.documentIdentity()) {
            throw new IOException(
                    "The live Dart document identity changed during rollback");
        }
        if (!Arrays.equals(
                expected.markerBearingUtf8(),
                restored.markerBearingUtf8())) {
            throw new IOException(
                    "The live Dart document does not match its pre-apply snapshot after rollback: "
                    + mismatch(expected.markerBearingUtf8(), restored.markerBearingUtf8()));
        }
        verifyExactGuard(expected.imports(), restored.imports());
        verifyExactGuard(expected.build(), restored.build());
    }

    private static void verifyExactGuard(
            LiveDartDocumentSnapshot.ManagedSection expected,
            LiveDartDocumentSnapshot.ManagedSection restored) throws IOException {
        if (!expected.sectionIdentity().isValid()
                || expected.sectionIdentity() != restored.sectionIdentity()
                || !restored.sectionIdentity().isValid()
                || !expected.id().equals(restored.id())
                || expected.sectionStartChar() != restored.sectionStartChar()
                || expected.sectionEndChar() != restored.sectionEndChar()
                || expected.payloadStartChar() != restored.payloadStartChar()
                || expected.payloadEndChar() != restored.payloadEndChar()
                || !expected.originalSectionText().equals(
                        restored.originalSectionText())) {
            throw new IOException(
                    "The live Dart guard '" + expected.id()
                    + "' does not match its exact pre-apply identity, range and content after rollback");
        }
    }

    private static void restoreSection(
            LiveDartDocumentSnapshot.ManagedSection section) {
        if (!section.sectionIdentity().isValid()) {
            throw new IllegalStateException(
                    "The live Dart guard '" + section.id()
                    + "' is invalid during rollback");
        }
        section.sectionIdentity().setText(section.originalReplacementText());
    }

    private static String reason(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.strip();
    }

    private static IOException asIOException(Throwable failure) {
        return failure instanceof IOException io
                ? io
                : new IOException("Cannot apply managed Dart regions: "
                        + reason(failure), failure);
    }

    private static IOException asRestoreIOException(Throwable failure) {
        return failure instanceof IOException io
                ? io
                : new IOException("Cannot restore managed Dart regions: "
                        + reason(failure), failure);
    }

    private static String mismatch(byte[] expected, byte[] actual) {
        int shared = Math.min(expected.length, actual.length);
        int offset = 0;
        while (offset < shared && expected[offset] == actual[offset]) {
            offset++;
        }
        String values = offset < shared
                ? ", expected byte " + (expected[offset] & 0xFF)
                + ", actual byte " + (actual[offset] & 0xFF)
                : "";
        return "first differing UTF-8 byte " + offset
                + values
                + ", expected length " + expected.length
                + ", actual length " + actual.length;
    }

    /** Exact rollback after a mutation was attempted could not be verified. */
    static final class RecoveryFailure extends IOException {
        private static final long serialVersionUID = 1L;

        RecoveryFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @FunctionalInterface
    interface AppliedSnapshotFinalizer {
        void finish(LiveDartDocumentSnapshot applied) throws IOException;
    }

    @FunctionalInterface
    interface RestoredSnapshotFinalizer {
        void finish(LiveDartDocumentSnapshot restored) throws IOException;
    }
}

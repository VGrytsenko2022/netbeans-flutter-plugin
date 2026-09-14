package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;
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
        return applyManagedRegions(provider, expected, generated,
                expectedCandidateBytes, null, finalizer);
    }

    /** Applies a proved user-member edit and generated regions under one native atomic edit. */
    static LiveDartDocumentSnapshot applyManagedRegions(
            DartGuardedSectionsProvider provider,
            LiveDartDocumentSnapshot expected,
            GeneratedDartRegions generated,
            byte[] expectedCandidateBytes,
            DartSourceTransitionPlan transition,
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

                List<UserEdit> userEdits = List.of();
                byte[] projected = expected.markerBearingUtf8();
                if (transition != null) {
                    if (!transition.liveSource().original().orElseThrow()
                                .contentEquals(expected.markerBearingUtf8())
                            || transition.generation().generated().orElseThrow() != generated
                            || !Arrays.equals(transition.candidateBytes(), candidate)) {
                        throw new IOException("The event source proof does not describe this exact live candidate.");
                    }
                    projected = transition.userSourceBytes();
                    var integrity = new DartSourceIntegrityScanner().scan(
                            projected, transition.baselineDescriptor());
                    if (!integrity.onDiskDeclaredMatch()) {
                        throw new IOException("The proved event source lost its managed-region integrity.");
                    }
                    var imports = integrity.region("imports").orElseThrow();
                    var build = integrity.region("build").orElseThrow();
                    userEdits = userEdits(expected, projected,
                            utf16Offset(projected, imports.payloadStartByte()),
                            utf16Offset(projected, imports.payloadEndByte()),
                            utf16Offset(projected, build.payloadStartByte()),
                            utf16Offset(projected, build.payloadEndByte()));
                }
                mutationStarted = true;
                applyUserEdits(document, userEdits);
                if (!Arrays.equals(projected, snapshot(document, provider).markerBearingUtf8())) {
                    throw new IOException("The applied event member differs from its exact source proof.");
                }
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
                LiveDartDocumentSnapshot managedRestored = snapshot(document, provider);
                applyUserEdits(document, userEdits(managedRestored,
                        restore.markerBearingUtf8(),
                        restore.imports().payloadStartChar(), restore.imports().payloadEndChar(),
                        restore.build().payloadStartChar(), restore.build().payloadEndChar()));
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

    private record UserEdit(int start, int end, String replacement) { }

    private static int utf16Offset(byte[] utf8, int byteOffset) {
        return new String(utf8, 0, byteOffset, StandardCharsets.UTF_8).length();
    }

    /** The three spans exclude managed payloads; exact unchanged markers are trimmed from edits. */
    private static List<UserEdit> userEdits(LiveDartDocumentSnapshot before,
            byte[] targetBytes, int importsStart, int importsEnd, int buildStart, int buildEnd)
            throws IOException {
        String source = new String(before.markerBearingUtf8(), StandardCharsets.UTF_8);
        String target = new String(LiveDartDocumentSnapshot.strictWritableUtf8(targetBytes), StandardCharsets.UTF_8);
        int[] from = {0, before.imports().payloadStartChar(),
            before.imports().payloadEndChar(), before.build().payloadStartChar(),
            before.build().payloadEndChar(), source.length()};
        int[] to = {0, importsStart, importsEnd, buildStart, buildEnd, target.length()};
        if (!source.substring(from[1], from[2]).equals(target.substring(to[1], to[2]))
                || !source.substring(from[3], from[4]).equals(target.substring(to[3], to[4]))) {
            throw new IOException("A user-source projection may not change managed payloads.");
        }
        List<UserEdit> edits = new ArrayList<>();
        for (int span = 0; span < 6; span += 2) {
            String old = source.substring(from[span], from[span + 1]);
            String next = target.substring(to[span], to[span + 1]);
            if (old.equals(next)) continue;
            int prefix = 0;
            while (prefix < old.length() && prefix < next.length()
                    && old.charAt(prefix) == next.charAt(prefix)) prefix++;
            if (prefix > 0 && Character.isHighSurrogate(old.charAt(prefix - 1))) prefix--;
            int suffix = 0;
            while (suffix < old.length() - prefix && suffix < next.length() - prefix
                    && old.charAt(old.length() - suffix - 1) == next.charAt(next.length() - suffix - 1)) suffix++;
            if (suffix > 0 && Character.isLowSurrogate(old.charAt(old.length() - suffix))) suffix--;
            int start = from[span] + prefix;
            int end = from[span + 1] - suffix;
            for (var guard : List.of(before.imports(), before.build())) {
                if (start < guard.sectionEndChar() && end > guard.sectionStartChar()
                        || start == end && start >= guard.sectionStartChar() && start < guard.sectionEndChar()) {
                    throw new IOException("An event member edit intersects the " + guard.id() + " guard.");
                }
            }
            edits.add(new UserEdit(start, end, next.substring(prefix, next.length() - suffix)));
        }
        return List.copyOf(edits);
    }

    private static void applyUserEdits(StyledDocument document, List<UserEdit> edits) throws IOException {
        try {
            for (int index = edits.size() - 1; index >= 0; index--) {
                UserEdit edit = edits.get(index);
                document.remove(edit.start(), edit.end() - edit.start());
                document.insertString(edit.start(), edit.replacement(), null);
            }
        } catch (BadLocationException failure) {
            throw new IOException("Cannot apply the proved user-owned event member edit.", failure);
        }
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

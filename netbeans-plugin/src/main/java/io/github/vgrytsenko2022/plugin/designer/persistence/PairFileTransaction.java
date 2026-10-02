package io.github.vgrytsenko2022.plugin.designer.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;

/**
 * Commits an exact Dart/FD byte pair while holding both NetBeans file locks.
 *
 * <p>The NetBeans atomic action is used only to group file-system events. This
 * class implements and verifies its own rollback; it never treats
 * {@code FileSystem.AtomicAction} as a storage transaction.</p>
 */
public final class PairFileTransaction {
    private final PairFileTransactionBackend backend;
    private final Object commitMonitor = new Object();
    private final OwnedAtomicAction ownedAtomicAction = new OwnedAtomicAction();

    public PairFileTransaction() {
        this(new NetBeansPairFileTransactionBackend());
    }

    PairFileTransaction(PairFileTransactionBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    /**
     * Returns whether an event was emitted from this transaction primitive's
     * own stable atomic-action token.
     *
     * <p>This is exact event provenance supplied by NetBeans, not a temporal
     * {@code commitInProgress} approximation. The same token is retained for
     * the lifetime of this instance, so delayed events remain recognizable.</p>
     */
    public boolean owns(FileEvent event) {
        return event != null && event.firedFrom(ownedAtomicAction);
    }

    /**
     * Executes one transaction. Attempts on the same primitive are serialized
     * so its stable atomic-action token can never carry two transaction bodies.
     */
    public PairFileTransactionResult commit(PairFileTransactionRequest request) {
        synchronized (commitMonitor) {
            return commitSerialized(request);
        }
    }

    private PairFileTransactionResult commitSerialized(
            PairFileTransactionRequest request) {
        Objects.requireNonNull(request, "request");
        Snapshot snapshots = new Snapshot(request);
        Validation validation = validate(request);
        if (!validation.issues().isEmpty()) {
            return result(
                    PairFileTransactionStatus.REJECTED,
                    validation.issues(), 0, false);
        }

        LockTarget dart = new LockTarget(
                PairFileRole.DART,
                request.dartFile(),
                validation.dartPath());
        LockTarget designer = new LockTarget(
                PairFileRole.DESIGNER,
                request.designerFile(),
                validation.designerPath());
        List<LockTarget> stableOrder = new ArrayList<>(List.of(dart, designer));
        stableOrder.sort(Comparator.comparing(
                target -> lockKey(target.path()),
                Comparator.naturalOrder()));

        FileLock firstLock = null;
        FileLock secondLock = null;
        try {
            firstLock = backend.lock(stableOrder.get(0).file());
            try {
                secondLock = backend.lock(stableOrder.get(1).file());
            } catch (IOException | RuntimeException ex) {
                return failureBeforeWrite(
                        PairFileTransactionIssueCode.LOCK_ACQUISITION_FAILED,
                        stableOrder.get(1),
                        "Cannot lock " + label(stableOrder.get(1)) + ": "
                        + reason(ex));
            }

            FileLock dartLock = stableOrder.get(0).role() == PairFileRole.DART
                    ? firstLock : secondLock;
            FileLock designerLock = stableOrder.get(0).role()
                    == PairFileRole.DESIGNER ? firstLock : secondLock;
            return commitLocked(
                    request,
                    snapshots,
                    validation,
                    dartLock,
                    designerLock,
                    stableOrder);
        } catch (IOException | RuntimeException ex) {
            return failureBeforeWrite(
                    PairFileTransactionIssueCode.LOCK_ACQUISITION_FAILED,
                    stableOrder.get(0),
                    "Cannot lock " + label(stableOrder.get(0)) + ": "
                    + reason(ex));
        } finally {
            release(secondLock);
            release(firstLock);
        }
    }

    private PairFileTransactionResult commitLocked(
            PairFileTransactionRequest request,
            Snapshot snapshots,
            Validation validation,
            FileLock dartLock,
            FileLock designerLock,
            List<LockTarget> stableOrder) {
        byte[] currentDart;
        byte[] currentDesigner;
        try {
            currentDart = backend.read(request.dartFile());
            currentDesigner = backend.read(request.designerFile());
        } catch (IOException | RuntimeException ex) {
            return result(
                    PairFileTransactionStatus.FAILED,
                    List.of(issue(
                            PairFileTransactionIssueCode.BASELINE_READ_FAILED,
                            PairFileRole.TRANSACTION,
                            "Cannot read the locked Dart/FD baselines: "
                            + reason(ex))),
                    0,
                    false);
        }

        List<PairFileTransactionIssue> stale = new ArrayList<>(2);
        if (!Arrays.equals(currentDart, snapshots.expectedDart())) {
            stale.add(issue(
                    PairFileTransactionIssueCode.DART_BASELINE_MISMATCH,
                    PairFileRole.DART,
                    "The Dart file changed since its expected byte snapshot: "
                    + validation.dartPath()));
        }
        if (!Arrays.equals(currentDesigner, snapshots.expectedDesigner())) {
            stale.add(issue(
                    PairFileTransactionIssueCode.DESIGNER_BASELINE_MISMATCH,
                    PairFileRole.DESIGNER,
                    "The designer file changed since its expected byte snapshot: "
                    + validation.designerPath()));
        }
        if (!stale.isEmpty()) {
            return result(
                    PairFileTransactionStatus.REJECTED, stale, 0, false);
        }

        boolean dartChanged = !Arrays.equals(
                snapshots.expectedDart(), snapshots.newDart());
        boolean designerChanged = !Arrays.equals(
                snapshots.expectedDesigner(), snapshots.newDesigner());
        if (!dartChanged && !designerChanged) {
            return result(
                    PairFileTransactionStatus.UNCHANGED,
                    List.of(),
                    0,
                    false);
        }

        WriteState state = new WriteState();
        try {
            runOwnedAtomicAction(
                    stableOrder.get(0).file(),
                    stableOrder.get(1).file(),
                    () -> performForwardWrites(
                            request,
                            snapshots,
                            dartLock,
                            designerLock,
                            dartChanged,
                            designerChanged,
                            state));
        } catch (IOException | RuntimeException ex) {
            if (state.result != null) {
                return state.result;
            }
            PairFileTransactionIssue primary = issue(
                    PairFileTransactionIssueCode.ATOMIC_ACTION_FAILED,
                    PairFileRole.TRANSACTION,
                    "The pair-file atomic event group failed: " + reason(ex));
            if (state.forwardWriteAttempts == 0) {
                return result(
                        PairFileTransactionStatus.FAILED,
                        List.of(primary),
                        0,
                        false);
            }
            return rollback(
                    request,
                    snapshots,
                    dartLock,
                    designerLock,
                    state,
                    primary);
        }
        if (state.result == null) {
            return result(
                    PairFileTransactionStatus.FAILED,
                    List.of(issue(
                            PairFileTransactionIssueCode.ATOMIC_ACTION_FAILED,
                            PairFileRole.TRANSACTION,
                            "The pair-file atomic event group did not execute "
                            + "the transaction body")),
                    0,
                    false);
        }
        return state.result;
    }

    private void runOwnedAtomicAction(
            FileObject stableFirst,
            FileObject stableSecond,
            FileSystem.AtomicAction body) throws IOException {
        ownedAtomicAction.bind(body);
        try {
            backend.runAtomicAction(
                    stableFirst, stableSecond, ownedAtomicAction);
        } finally {
            ownedAtomicAction.clear();
        }
    }

    private void performForwardWrites(
            PairFileTransactionRequest request,
            Snapshot snapshots,
            FileLock dartLock,
            FileLock designerLock,
            boolean dartChanged,
            boolean designerChanged,
        WriteState state) {
        if (dartChanged) {
            state.forwardWriteAttempts++;
            state.dartWriteAttempted = true;
            try {
                backend.write(
                        request.dartFile(), dartLock, snapshots.newDart());
            } catch (IOException | RuntimeException ex) {
                state.result = rollback(
                        request,
                        snapshots,
                        dartLock,
                        designerLock,
                        state,
                        issue(
                                PairFileTransactionIssueCode.DART_WRITE_FAILED,
                                PairFileRole.DART,
                                "Cannot write the new Dart byte snapshot: "
                                + reason(ex)));
                return;
            }
        }
        if (designerChanged) {
            state.forwardWriteAttempts++;
            state.designerWriteAttempted = true;
            try {
                backend.write(
                        request.designerFile(),
                        designerLock,
                        snapshots.newDesigner());
            } catch (IOException | RuntimeException ex) {
                state.result = rollback(
                        request,
                        snapshots,
                        dartLock,
                        designerLock,
                        state,
                        issue(
                                PairFileTransactionIssueCode.DESIGNER_WRITE_FAILED,
                                PairFileRole.DESIGNER,
                                "Cannot write the new designer byte snapshot: "
                                + reason(ex)));
                return;
            }
        }

        PairFileTransactionIssue verificationFailure = verifyNew(
                request, snapshots);
        if (verificationFailure != null) {
            state.result = rollback(
                    request,
                    snapshots,
                    dartLock,
                    designerLock,
                    state,
                    verificationFailure);
            return;
        }
        state.result = result(
                PairFileTransactionStatus.COMMITTED,
                List.of(),
                state.forwardWriteAttempts,
                false);
    }

    private PairFileTransactionIssue verifyNew(
            PairFileTransactionRequest request, Snapshot snapshots) {
        byte[] dart;
        byte[] designer;
        try {
            dart = backend.read(request.dartFile());
            designer = backend.read(request.designerFile());
        } catch (IOException | RuntimeException ex) {
            return issue(
                    PairFileTransactionIssueCode.POST_WRITE_READ_FAILED,
                    PairFileRole.TRANSACTION,
                    "Cannot re-read the written Dart/FD pair: " + reason(ex));
        }
        if (!Arrays.equals(dart, snapshots.newDart())) {
            return issue(
                    PairFileTransactionIssueCode.POST_WRITE_MISMATCH,
                    PairFileRole.DART,
                    "The Dart file does not match the requested new byte "
                    + "snapshot after writing");
        }
        if (!Arrays.equals(designer, snapshots.newDesigner())) {
            return issue(
                    PairFileTransactionIssueCode.POST_WRITE_MISMATCH,
                    PairFileRole.DESIGNER,
                    "The designer file does not match the requested new byte "
                    + "snapshot after writing");
        }
        return null;
    }

    private PairFileTransactionResult rollback(
            PairFileTransactionRequest request,
            Snapshot snapshots,
            FileLock dartLock,
            FileLock designerLock,
            WriteState state,
            PairFileTransactionIssue primary) {
        List<PairFileTransactionIssue> issues = new ArrayList<>();
        issues.add(primary);

        // Restore in reverse commit order, but only participants whose writer
        // was actually entered. An attempted writer may have partially changed
        // its file before throwing; a never-entered or unchanged participant
        // is verified below and must not be rewritten.
        if (state.designerWriteAttempted) {
            restore(
                    request.designerFile(),
                    designerLock,
                    snapshots.expectedDesigner(),
                    PairFileRole.DESIGNER,
                    issues);
        }
        if (state.dartWriteAttempted) {
            restore(
                    request.dartFile(),
                    dartLock,
                    snapshots.expectedDart(),
                    PairFileRole.DART,
                    issues);
        }

        verifyRollback(
                request.dartFile(),
                snapshots.expectedDart(),
                PairFileRole.DART,
                issues);
        verifyRollback(
                request.designerFile(),
                snapshots.expectedDesigner(),
                PairFileRole.DESIGNER,
                issues);

        boolean recoveryConflict = issues.stream().skip(1).anyMatch(
                issue -> issue.code()
                == PairFileTransactionIssueCode.ROLLBACK_WRITE_FAILED
                || issue.code()
                == PairFileTransactionIssueCode.ROLLBACK_READ_FAILED
                || issue.code()
                == PairFileTransactionIssueCode.ROLLBACK_MISMATCH);
        return result(
                recoveryConflict
                        ? PairFileTransactionStatus.RECOVERY_CONFLICT
                        : PairFileTransactionStatus.ROLLED_BACK,
                issues,
                state.forwardWriteAttempts,
                true);
    }

    private void restore(
            FileObject file,
            FileLock lock,
            byte[] expected,
            PairFileRole role,
            List<PairFileTransactionIssue> issues) {
        try {
            backend.write(file, lock, expected);
        } catch (IOException | RuntimeException ex) {
            issues.add(issue(
                    PairFileTransactionIssueCode.ROLLBACK_WRITE_FAILED,
                    role,
                    "Cannot restore the exact old " + roleLabel(role)
                    + " byte snapshot: " + reason(ex)));
        }
    }

    private void verifyRollback(
            FileObject file,
            byte[] expected,
            PairFileRole role,
            List<PairFileTransactionIssue> issues) {
        byte[] actual;
        try {
            actual = backend.read(file);
        } catch (IOException | RuntimeException ex) {
            issues.add(issue(
                    PairFileTransactionIssueCode.ROLLBACK_READ_FAILED,
                    role,
                    "Cannot verify the restored " + roleLabel(role)
                    + " byte snapshot: " + reason(ex)));
            return;
        }
        if (!Arrays.equals(actual, expected)) {
            issues.add(issue(
                    PairFileTransactionIssueCode.ROLLBACK_MISMATCH,
                    role,
                    "The " + roleLabel(role)
                    + " file does not match its exact old byte snapshot after "
                    + "rollback"));
        }
    }

    private Validation validate(PairFileTransactionRequest request) {
        List<PairFileTransactionIssue> issues = new ArrayList<>();
        Path dartPath = validateFile(
                request.dartFile(), PairFileRole.DART, issues);
        Path designerPath = validateFile(
                request.designerFile(), PairFileRole.DESIGNER, issues);
        if (dartPath != null && designerPath != null) {
            try {
                if (dartPath.equals(designerPath)
                        || Files.isSameFile(dartPath, designerPath)) {
                    issues.add(issue(
                            PairFileTransactionIssueCode.SAME_FILE,
                            PairFileRole.TRANSACTION,
                            "The Dart and designer transaction participants "
                            + "resolve to the same local file: " + dartPath));
                }
            } catch (IOException | RuntimeException ex) {
                issues.add(issue(
                        PairFileTransactionIssueCode.FILE_VALIDATION_FAILED,
                        PairFileRole.TRANSACTION,
                        "Cannot prove that the Dart and designer files are "
                        + "distinct: " + reason(ex)));
            }
        }
        return new Validation(dartPath, designerPath, List.copyOf(issues));
    }

    private Path validateFile(
            FileObject file,
            PairFileRole role,
            List<PairFileTransactionIssue> issues) {
        if (!file.isValid() || !file.isData()) {
            issues.add(issue(
                    PairFileTransactionIssueCode.INVALID_FILE,
                    role,
                    "The " + roleLabel(role)
                    + " transaction participant is not a valid data file: "
                    + file.getPath()));
            return null;
        }
        try {
            Path localPath = backend.localPath(file);
            if (localPath == null) {
                issues.add(issue(
                        PairFileTransactionIssueCode.NON_LOCAL_FILE,
                        role,
                        "The " + roleLabel(role)
                        + " transaction participant is not a local file: "
                        + file.getPath()));
                return null;
            }
            Path realPath = localPath.toRealPath();
            if (!Files.isRegularFile(realPath)) {
                issues.add(issue(
                        PairFileTransactionIssueCode.INVALID_FILE,
                        role,
                        "The " + roleLabel(role)
                        + " transaction participant is not a regular local "
                        + "data file: " + realPath));
                return null;
            }
            return realPath;
        } catch (IOException | RuntimeException ex) {
            issues.add(issue(
                    PairFileTransactionIssueCode.FILE_VALIDATION_FAILED,
                    role,
                    "Cannot validate the local " + roleLabel(role)
                    + " transaction participant " + file.getPath() + ": "
                    + reason(ex)));
            return null;
        }
    }

    private PairFileTransactionResult failureBeforeWrite(
            PairFileTransactionIssueCode code,
            LockTarget target,
            String message) {
        return result(
                PairFileTransactionStatus.FAILED,
                List.of(issue(code, target.role(), message)),
                0,
                false);
    }

    private static PairFileTransactionResult result(
            PairFileTransactionStatus status,
            List<PairFileTransactionIssue> issues,
            int writes,
            boolean rollback) {
        return new PairFileTransactionResult(status, issues, writes, rollback);
    }

    private static PairFileTransactionIssue issue(
            PairFileTransactionIssueCode code,
            PairFileRole role,
            String message) {
        return new PairFileTransactionIssue(code, role, message);
    }

    private static String label(LockTarget target) {
        return roleLabel(target.role()) + " file " + target.path();
    }

    private static String roleLabel(PairFileRole role) {
        return switch (role) {
            case DART -> "Dart";
            case DESIGNER -> "designer";
            case TRANSACTION -> "pair-file transaction";
        };
    }

    private static String lockKey(Path path) {
        String key = path.toString().replace('\\', '/');
        return File.separatorChar == '\\'
                ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.strip();
    }

    private static void release(FileLock lock) {
        if (lock != null) {
            lock.releaseLock();
        }
    }

    private record Snapshot(
            byte[] expectedDart,
            byte[] expectedDesigner,
            byte[] newDart,
            byte[] newDesigner) {

        Snapshot(PairFileTransactionRequest request) {
            this(
                    request.expectedDartBytes(),
                    request.expectedDesignerBytes(),
                    request.newDartBytes(),
                    request.newDesignerBytes());
        }
    }

    private record Validation(
            Path dartPath,
            Path designerPath,
            List<PairFileTransactionIssue> issues) {
    }

    private record LockTarget(PairFileRole role, FileObject file, Path path) {
    }

    private static final class WriteState {
        private int forwardWriteAttempts;
        private boolean dartWriteAttempted;
        private boolean designerWriteAttempted;
        private PairFileTransactionResult result;
    }

    /** Stable identity token passed directly to NetBeans for every commit. */
    private static final class OwnedAtomicAction
            implements FileSystem.AtomicAction {
        private FileSystem.AtomicAction body;

        void bind(FileSystem.AtomicAction body) {
            if (this.body != null) {
                throw new IllegalStateException(
                        "The pair-file atomic action is already bound");
            }
            this.body = Objects.requireNonNull(body, "body");
        }

        void clear() {
            body = null;
        }

        @Override
        public void run() throws IOException {
            FileSystem.AtomicAction current = body;
            if (current == null) {
                throw new IOException(
                        "The pair-file atomic action has no transaction body");
            }
            current.run();
        }
    }
}

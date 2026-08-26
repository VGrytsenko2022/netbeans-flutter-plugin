package dev.flutter.netbeans.plugin.designer.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

class PairFileTransactionTest {
    private static final byte[] OLD_DART = bytes("old Dart Ж\n");
    private static final byte[] OLD_DESIGNER = bytes("{\"old\":true}\n");
    private static final byte[] NEW_DART = bytes("new Dart 🌱\n");
    private static final byte[] NEW_DESIGNER = bytes("{\"new\":true}\n");

    @TempDir
    Path temporaryDirectory;

    @Test
    void commitsDartThenDesignerWhileLockingInStablePathOrder()
            throws Exception {
        Pair pair = pair("z_screen.dart", "a_screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.COMMITTED, result.status());
        assertEquals(2, result.forwardWriteAttempts());
        assertFalse(result.rollbackAttempted());
        assertTrue(result.issues().isEmpty());
        assertArrayEquals(NEW_DART, read(pair.dart()));
        assertArrayEquals(NEW_DESIGNER, read(pair.designer()));
        assertEquals(
                List.of(
                        "lock:DESIGNER",
                        "lock:DART",
                        "atomic:begin",
                        "write:DART:1",
                        "write:DESIGNER:1",
                        "atomic:end"),
                backend.mutationEvents());
    }

    @Test
    void ownsOnlyEventsFiredFromItsStableAtomicActionToken()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        PairFileTransaction transaction = transaction(backend);
        PairFileTransaction otherTransaction = new PairFileTransaction();
        List<FileEvent> events = new ArrayList<>();
        FileChangeAdapter listener = new FileChangeAdapter() {
            @Override
            public void fileChanged(FileEvent event) {
                events.add(event);
            }
        };
        pair.dart().addFileChangeListener(listener);
        pair.designer().addFileChangeListener(listener);

        PairFileTransactionResult result = transaction.commit(request(pair));

        assertEquals(PairFileTransactionStatus.COMMITTED, result.status());
        assertEquals(2, events.size(), () -> "unexpected events " + events);
        assertTrue(events.stream().allMatch(transaction::owns));
        assertTrue(events.stream().noneMatch(otherTransaction::owns));
        assertFalse(transaction.owns(null));
        List<FileEvent> firstCommitEvents = List.copyOf(events);

        events.clear();
        PairFileTransactionRequest secondRequest = new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                NEW_DART,
                NEW_DESIGNER,
                bytes("second Dart candidate\n"),
                bytes("{\"second\":true}\n"));
        PairFileTransactionResult second = transaction.commit(secondRequest);
        assertEquals(PairFileTransactionStatus.COMMITTED, second.status());
        assertEquals(2, events.size(), () -> "unexpected events " + events);
        assertTrue(events.stream().allMatch(transaction::owns));
        assertTrue(firstCommitEvents.stream().allMatch(transaction::owns),
                "events from an earlier commit must keep exact provenance");
        assertEquals(2, backend.atomicActions.size());
        assertSame(
                backend.atomicActions.get(0),
                backend.atomicActions.get(1),
                "the transaction must retain one stable AtomicAction token");

        events.clear();
        try (FileLock lock = pair.dart().lock()) {
            new NetBeansPairFileTransactionBackend().write(
                    pair.dart(), lock, bytes("external change\n"));
        }
        assertEquals(1, events.size(), () -> "unexpected events " + events);
        assertFalse(
                transaction.owns(events.get(0)),
                "an event outside the owned atomic action must not be claimed");
    }

    @Test
    void rejectsStaleDartWithZeroWrites() throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        Files.write(local(pair.dart()), bytes("external Dart change\n"));
        pair.dart().refresh();
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.REJECTED, result.status());
        assertEquals(0, result.forwardWriteAttempts());
        assertFalse(result.rollbackAttempted());
        assertIssue(result, PairFileTransactionIssueCode.DART_BASELINE_MISMATCH);
        assertTrue(backend.writeEvents().isEmpty());
        assertArrayEquals(bytes("external Dart change\n"), read(pair.dart()));
        assertArrayEquals(OLD_DESIGNER, read(pair.designer()));
    }

    @Test
    void rejectsStaleDesignerWithZeroWrites() throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        Files.write(local(pair.designer()), bytes("external FD change\n"));
        pair.designer().refresh();
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.REJECTED, result.status());
        assertEquals(0, result.forwardWriteAttempts());
        assertIssue(
                result,
                PairFileTransactionIssueCode.DESIGNER_BASELINE_MISMATCH);
        assertTrue(backend.writeEvents().isEmpty());
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(bytes("external FD change\n"), read(pair.designer()));
    }

    @Test
    void restoresBothExactOldSnapshotsWhenSecondWriteFails()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        backend.failFirstDesignerWrite = true;

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.ROLLED_BACK, result.status());
        assertEquals(2, result.forwardWriteAttempts());
        assertTrue(result.rollbackAttempted());
        assertIssue(result, PairFileTransactionIssueCode.DESIGNER_WRITE_FAILED);
        assertFalse(hasIssue(
                result, PairFileTransactionIssueCode.ROLLBACK_WRITE_FAILED));
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(OLD_DESIGNER, read(pair.designer()));
        assertEquals(
                List.of(
                        "write:DART:1",
                        "write:DESIGNER:1",
                        "write:DESIGNER:2",
                        "write:DART:2"),
                backend.writeEvents());
    }

    @Test
    void reportsRecoveryConflictAndContinuesRestoringAfterRollbackFailure()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        backend.failFirstDesignerWrite = true;
        backend.failSecondDartWrite = true;

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(
                PairFileTransactionStatus.RECOVERY_CONFLICT, result.status());
        assertTrue(result.rollbackAttempted());
        assertIssue(result, PairFileTransactionIssueCode.DESIGNER_WRITE_FAILED);
        assertIssue(result, PairFileTransactionIssueCode.ROLLBACK_WRITE_FAILED);
        assertIssue(result, PairFileTransactionIssueCode.ROLLBACK_MISMATCH);
        assertArrayEquals(
                NEW_DART,
                read(pair.dart()),
                "failed Dart restoration must remain visible as a conflict");
        assertArrayEquals(
                OLD_DESIGNER,
                read(pair.designer()),
                "FD restoration must still run after the other failure");
    }

    @Test
    void rollsBackWhenPostWriteRereadDoesNotMatchCandidate()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        backend.mismatchSecondDesignerRead = true;

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.ROLLED_BACK, result.status());
        assertIssue(result, PairFileTransactionIssueCode.POST_WRITE_MISMATCH);
        PairFileTransactionIssue mismatch = result.issues().stream()
                .filter(issue -> issue.code()
                == PairFileTransactionIssueCode.POST_WRITE_MISMATCH)
                .findFirst()
                .orElseThrow();
        assertEquals(PairFileRole.DESIGNER, mismatch.target());
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(OLD_DESIGNER, read(pair.designer()));
    }

    @Test
    void releasesFirstLockAndPerformsZeroWritesWhenSecondLockFails()
            throws Exception {
        Pair pair = pair("z_screen.dart", "a_screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        backend.failLock = pair.dart();

        PairFileTransactionResult result = transaction(backend).commit(request(pair));

        assertEquals(PairFileTransactionStatus.FAILED, result.status());
        assertEquals(0, result.forwardWriteAttempts());
        assertIssue(
                result,
                PairFileTransactionIssueCode.LOCK_ACQUISITION_FAILED);
        assertTrue(backend.writeEvents().isEmpty());
        assertEquals(List.of("lock:DESIGNER", "lock:DART"), backend.lockEvents());
        try (FileLock ignored = pair.designer().lock()) {
            assertTrue(ignored.isValid(), "the first acquired lock must be released");
        }
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(OLD_DESIGNER, read(pair.designer()));
    }

    @Test
    void sourceOnlyRollbackRestoresDartButNeverRewritesUnchangedFd()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        backend.partiallyWriteFirstDartThenFail = true;
        PairFileTransactionRequest sourceOnly = new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                OLD_DART,
                OLD_DESIGNER,
                NEW_DART,
                OLD_DESIGNER);

        PairFileTransactionResult result = transaction(backend).commit(sourceOnly);

        assertEquals(PairFileTransactionStatus.ROLLED_BACK, result.status());
        assertEquals(1, result.forwardWriteAttempts());
        assertIssue(result, PairFileTransactionIssueCode.DART_WRITE_FAILED);
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(OLD_DESIGNER, read(pair.designer()));
        assertEquals(
                List.of("write:DART:1", "write:DART:2"),
                backend.writeEvents(),
                "an unchanged FD is verified after rollback but never rewritten");
    }

    @Test
    void verifiesUnchangedRequestAgainstLockedBaselinesWithoutWriting()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        PairFileTransactionRequest unchanged = new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                OLD_DART,
                OLD_DESIGNER,
                OLD_DART,
                OLD_DESIGNER);

        PairFileTransactionResult result = transaction(backend).commit(unchanged);

        assertEquals(PairFileTransactionStatus.UNCHANGED, result.status());
        assertTrue(result.committed());
        assertEquals(0, result.forwardWriteAttempts());
        assertTrue(backend.writeEvents().isEmpty());
        assertEquals(2, backend.readEvents().size());
    }

    @Test
    void writesOnlyChangedParticipantButStillVerifiesTheWholePair()
            throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.designer());
        PairFileTransactionRequest designerOnly = new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                OLD_DART,
                OLD_DESIGNER,
                OLD_DART,
                NEW_DESIGNER);

        PairFileTransactionResult result = transaction(backend).commit(designerOnly);

        assertEquals(PairFileTransactionStatus.COMMITTED, result.status());
        assertEquals(1, result.forwardWriteAttempts());
        assertEquals(List.of("write:DESIGNER:1"), backend.writeEvents());
        assertArrayEquals(OLD_DART, read(pair.dart()));
        assertArrayEquals(NEW_DESIGNER, read(pair.designer()));
        assertEquals(4, backend.readEvents().size());
    }

    @Test
    void rejectsSamePhysicalFileBeforeLocking() throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        RecordingBackend backend = new RecordingBackend(pair.dart(), pair.dart());
        PairFileTransactionRequest request = new PairFileTransactionRequest(
                pair.dart(),
                pair.dart(),
                OLD_DART,
                OLD_DART,
                NEW_DART,
                NEW_DART);

        PairFileTransactionResult result = transaction(backend).commit(request);

        assertEquals(PairFileTransactionStatus.REJECTED, result.status());
        assertIssue(result, PairFileTransactionIssueCode.SAME_FILE);
        assertTrue(backend.lockEvents().isEmpty());
        assertTrue(backend.writeEvents().isEmpty());
    }

    @Test
    void rejectsNonLocalFilesBeforeLocking() throws Exception {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject dart = memory.getRoot().createData("screen.dart");
        FileObject designer = memory.getRoot().createData("screen.fd");
        PairFileTransactionRequest request = new PairFileTransactionRequest(
                dart,
                designer,
                new byte[0],
                new byte[0],
                NEW_DART,
                NEW_DESIGNER);

        PairFileTransactionResult result = new PairFileTransaction().commit(request);

        assertEquals(PairFileTransactionStatus.REJECTED, result.status());
        assertEquals(2, result.issues().stream()
                .filter(issue -> issue.code()
                == PairFileTransactionIssueCode.NON_LOCAL_FILE)
                .count());
    }

    @Test
    void requestAndResultCollectionsAreDefensive() throws Exception {
        Pair pair = pair("screen.dart", "screen.fd");
        byte[] expectedDart = OLD_DART.clone();
        byte[] newDart = NEW_DART.clone();
        PairFileTransactionRequest request = new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                expectedDart,
                OLD_DESIGNER,
                newDart,
                NEW_DESIGNER);
        expectedDart[0] = 0;
        newDart[0] = 0;

        PairFileTransactionResult result = new PairFileTransaction().commit(request);

        assertEquals(PairFileTransactionStatus.COMMITTED, result.status());
        assertArrayEquals(NEW_DART, read(pair.dart()));
        assertArrayEquals(NEW_DESIGNER, read(pair.designer()));
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.issues().add(new PairFileTransactionIssue(
                        PairFileTransactionIssueCode.POST_WRITE_MISMATCH,
                        PairFileRole.DART,
                        "must remain immutable")));
    }

    private Pair pair(String dartName, String designerName) throws IOException {
        Path dartPath = temporaryDirectory.resolve(dartName);
        Path designerPath = temporaryDirectory.resolve(designerName);
        Files.write(dartPath, OLD_DART);
        Files.write(designerPath, OLD_DESIGNER);
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        FileObject designer = FileUtil.toFileObject(designerPath.toFile());
        assertNotNull(dart);
        assertNotNull(designer);
        return new Pair(dart, designer);
    }

    private static PairFileTransaction transaction(RecordingBackend backend) {
        return new PairFileTransaction(backend);
    }

    private static PairFileTransactionRequest request(Pair pair) {
        return new PairFileTransactionRequest(
                pair.dart(),
                pair.designer(),
                OLD_DART,
                OLD_DESIGNER,
                NEW_DART,
                NEW_DESIGNER);
    }

    private static byte[] read(FileObject file) throws IOException {
        file.refresh();
        return file.asBytes();
    }

    private static Path local(FileObject file) {
        return FileUtil.toFile(file).toPath();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertIssue(
            PairFileTransactionResult result,
            PairFileTransactionIssueCode code) {
        assertTrue(
                hasIssue(result, code),
                () -> "Missing " + code + " in " + result.issues());
    }

    private static boolean hasIssue(
            PairFileTransactionResult result,
            PairFileTransactionIssueCode code) {
        return result.issues().stream().anyMatch(issue -> issue.code() == code);
    }

    private record Pair(FileObject dart, FileObject designer) {
    }

    private static final class RecordingBackend
            implements PairFileTransactionBackend {
        private final PairFileTransactionBackend delegate =
                new NetBeansPairFileTransactionBackend();
        private final FileObject dart;
        private final FileObject designer;
        private final List<String> events = new ArrayList<>();
        private final List<FileSystem.AtomicAction> atomicActions =
                new ArrayList<>();
        private final Map<FileObject, Integer> reads = new HashMap<>();
        private final Map<FileObject, Integer> writes = new HashMap<>();
        private FileObject failLock;
        private boolean failFirstDesignerWrite;
        private boolean failSecondDartWrite;
        private boolean mismatchSecondDesignerRead;
        private boolean partiallyWriteFirstDartThenFail;

        RecordingBackend(FileObject dart, FileObject designer) {
            this.dart = dart;
            this.designer = designer;
        }

        @Override
        public Path localPath(FileObject file) throws IOException {
            return delegate.localPath(file);
        }

        @Override
        public FileLock lock(FileObject file) throws IOException {
            events.add("lock:" + role(file));
            if (file.equals(failLock)) {
                throw new IOException("injected lock failure");
            }
            return delegate.lock(file);
        }

        @Override
        public byte[] read(FileObject file) throws IOException {
            int count = reads.merge(file, 1, Integer::sum);
            events.add("read:" + role(file) + ":" + count);
            if (file.equals(designer)
                    && count == 2
                    && mismatchSecondDesignerRead) {
                return bytes("injected post-write mismatch");
            }
            return delegate.read(file);
        }

        @Override
        public void write(FileObject file, FileLock lock, byte[] bytes)
                throws IOException {
            int count = writes.merge(file, 1, Integer::sum);
            events.add("write:" + role(file) + ":" + count);
            if (file.equals(designer)
                    && count == 1
                    && failFirstDesignerWrite) {
                throw new IOException("injected designer write failure");
            }
            if (file.equals(dart) && count == 2 && failSecondDartWrite) {
                throw new IOException("injected Dart rollback failure");
            }
            if (file.equals(dart)
                    && count == 1
                    && partiallyWriteFirstDartThenFail) {
                delegate.write(file, lock, bytes("partial"));
                throw new IOException("injected partial Dart write failure");
            }
            delegate.write(file, lock, bytes);
        }

        @Override
        public void runAtomicAction(
                FileObject stableFirst,
                FileObject stableSecond,
                FileSystem.AtomicAction action) throws IOException {
            events.add("atomic:begin");
            atomicActions.add(action);
            delegate.runAtomicAction(stableFirst, stableSecond, action);
            events.add("atomic:end");
        }

        List<String> mutationEvents() {
            return events.stream()
                    .filter(event -> event.startsWith("lock:")
                    || event.startsWith("atomic:")
                    || event.startsWith("write:"))
                    .toList();
        }

        List<String> lockEvents() {
            return events.stream()
                    .filter(event -> event.startsWith("lock:"))
                    .toList();
        }

        List<String> readEvents() {
            return events.stream()
                    .filter(event -> event.startsWith("read:"))
                    .toList();
        }

        List<String> writeEvents() {
            return events.stream()
                    .filter(event -> event.startsWith("write:"))
                    .toList();
        }

        private PairFileRole role(FileObject file) {
            if (file.equals(dart)) {
                return PairFileRole.DART;
            }
            if (file.equals(designer)) {
                return PairFileRole.DESIGNER;
            }
            throw new IllegalArgumentException("unexpected file " + file);
        }
    }
}

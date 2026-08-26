package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Process-lifetime claim on one immutable Canvas runtime generation.
 *
 * <p>Close is idempotent. After close, callers must no longer launch or use
 * {@link CanvasRunnerBuildResult#executable()}; a later cache build may remove
 * that generation once no JVM or other NetBeans process holds a lease.
 */
public final class CanvasRunnerRuntimeLease implements AutoCloseable {
    private static final Object LOCK = new Object();
    private static final Map<Path, SharedLease> SHARED = new HashMap<>();
    private static final Set<Path> CLEANING = new HashSet<>();

    private final Path runtimeDirectory;
    private final SharedLease shared;
    private final AtomicBoolean closed = new AtomicBoolean();

    private CanvasRunnerRuntimeLease(Path runtimeDirectory, SharedLease shared) {
        this.runtimeDirectory = runtimeDirectory;
        this.shared = shared;
    }

    static CanvasRunnerRuntimeLease acquire(Path runtimeDirectory) throws IOException {
        Path runtime = Objects.requireNonNull(runtimeDirectory, "runtimeDirectory")
                .toAbsolutePath().normalize();
        Path generation = Objects.requireNonNull(runtime.getParent(),
                "runtime generation directory");
        Path leaseFile = leaseFile(generation);
        synchronized (LOCK) {
            boolean interrupted = false;
            while (CLEANING.contains(generation)) {
                try {
                    LOCK.wait();
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted while waiting for Canvas runtime cleanup");
            }
            SharedLease existing = SHARED.get(generation);
            if (existing != null) {
                existing.references++;
                return new CanvasRunnerRuntimeLease(runtime, existing);
            }
            if (!java.nio.file.Files.isRegularFile(
                    leaseFile, LinkOption.NOFOLLOW_LINKS)
                    || java.nio.file.Files.isSymbolicLink(leaseFile)) {
                throw new IOException("Canvas runtime generation has no safe lease file: "
                        + leaseFile);
            }
            FileChannel channel = FileChannel.open(leaseFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
            try {
                FileLock lock = channel.lock(0L, Long.MAX_VALUE, true);
                if (!java.nio.file.Files.isDirectory(runtime, LinkOption.NOFOLLOW_LINKS)
                        || java.nio.file.Files.isSymbolicLink(runtime)) {
                    lock.release();
                    throw new IOException(
                            "Canvas runtime generation disappeared while acquiring its lease: "
                            + runtime);
                }
                SharedLease created = new SharedLease(channel, lock);
                SHARED.put(generation, created);
                return new CanvasRunnerRuntimeLease(runtime, created);
            } catch (IOException | RuntimeException failure) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        }
    }

    static CanvasRunnerRuntimeLease detached() {
        return new CanvasRunnerRuntimeLease(null, null);
    }

    static boolean isLeasedInJvm(Path generationDirectory) {
        Path generation = generationDirectory.toAbsolutePath().normalize();
        synchronized (LOCK) {
            return SHARED.containsKey(generation);
        }
    }

    static CleanupLease tryAcquireCleanup(Path generationDirectory) throws IOException {
        Path generation = generationDirectory.toAbsolutePath().normalize();
        synchronized (LOCK) {
            if (SHARED.containsKey(generation) || !CLEANING.add(generation)) {
                return null;
            }
        }
        Path leaseFile = leaseFile(generation);
        FileChannel channel = null;
        FileLock lock = null;
        try {
            channel = FileChannel.open(leaseFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
            try {
                lock = channel.tryLock(0L, Long.MAX_VALUE, false);
            } catch (java.nio.channels.OverlappingFileLockException exception) {
                lock = null;
            }
            if (lock == null) {
                channel.close();
                synchronized (LOCK) {
                    CLEANING.remove(generation);
                    LOCK.notifyAll();
                }
                return null;
            }
            return new CleanupLease(generation, leaseFile, channel, lock);
        } catch (IOException | RuntimeException failure) {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            synchronized (LOCK) {
                CLEANING.remove(generation);
                LOCK.notifyAll();
            }
            throw failure;
        }
    }

    private static Path leaseFile(Path generation) {
        return generation.resolveSibling(
                generation.getFileName() + CanvasRunnerRuntimeCache.LEASE_FILE_SUFFIX);
    }

    public Optional<Path> runtimeDirectory() {
        return Optional.ofNullable(runtimeDirectory);
    }

    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Acquires an independent child claim for an asynchronous launch/process.
     * The child is idempotently closeable and remains valid after this lease
     * closes. Retaining an already-closed lease is rejected.
     */
    public CanvasRunnerRuntimeLease retain() {
        synchronized (LOCK) {
            if (closed.get()) {
                throw new IllegalStateException("Canvas runtime lease is already closed");
            }
            if (shared == null) {
                return detached();
            }
            Path generation = runtimeDirectory.getParent();
            SharedLease current = SHARED.get(generation);
            if (current != shared) {
                throw new IllegalStateException("Canvas runtime lease is no longer registered");
            }
            current.references++;
            return new CanvasRunnerRuntimeLease(runtimeDirectory, current);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true) || shared == null) {
            return;
        }
        synchronized (LOCK) {
            Path generation = runtimeDirectory.getParent();
            SharedLease current = SHARED.get(generation);
            if (current != shared || --current.references < 0) {
                throw new IllegalStateException("Canvas runtime lease reference mismatch");
            }
            if (current.references == 0) {
                SHARED.remove(generation);
                try {
                    current.lock.release();
                } catch (IOException ignored) {
                    // The generation remains on disk; cleanup will conservatively skip it.
                }
                try {
                    current.channel.close();
                } catch (IOException ignored) {
                    // The generation remains on disk; cleanup will retry on a later build.
                }
            }
        }
    }

    private static final class SharedLease {
        private final FileChannel channel;
        private final FileLock lock;
        private int references = 1;

        private SharedLease(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }
    }

    static final class CleanupLease implements AutoCloseable {
        private final Path generation;
        private final Path leaseFile;
        private final FileChannel channel;
        private final FileLock lock;
        private final AtomicBoolean closed = new AtomicBoolean();
        private boolean deleteLeaseFile;

        private CleanupLease(
                Path generation, Path leaseFile, FileChannel channel, FileLock lock) {
            this.generation = generation;
            this.leaseFile = leaseFile;
            this.channel = channel;
            this.lock = lock;
        }

        void deleteLeaseFileOnClose() {
            if (closed.get()) {
                throw new IllegalStateException("Canvas runtime cleanup lease is closed");
            }
            deleteLeaseFile = true;
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            IOException failure = null;
            try {
                lock.release();
            } catch (IOException exception) {
                failure = exception;
            }
            try {
                channel.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            if (deleteLeaseFile) {
                try {
                    java.nio.file.Files.deleteIfExists(leaseFile);
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
            // CLEANING remains set through sidecar deletion so same-JVM
            // retain/acquire cannot enter the unlocked cleanup gap.
            synchronized (LOCK) {
                CLEANING.remove(generation);
                LOCK.notifyAll();
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}

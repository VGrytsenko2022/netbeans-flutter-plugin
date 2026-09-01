package dev.flutter.netbeans.plugin.designer;

import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/** Debounced watcher for project and package-owned Flutter asset inputs. */
final class FlutterDesignerProjectAssetWatcher implements AutoCloseable {
    private static final int REFRESH_DELAY_MILLIS = 180;
    private static final int MAX_EXTERNAL_ROOTS = 4_096;
    private static final RequestProcessor REFRESH_WORKER = new RequestProcessor(
            FlutterDesignerProjectAssetWatcher.class.getName(), 1, true);
    private static final RequestProcessor SYNCHRONIZATION_WORKER =
            new RequestProcessor(
                    FlutterDesignerProjectAssetWatcher.class.getName()
                    + "-package-listeners",
                    1,
                    true);
    private final FileObject projectDirectory;
    private final Path localProjectRoot;
    private final RefreshScheduler scheduler;
    private final RecursiveListenerRegistration recursiveListeners;
    private final SynchronizationScheduler synchronizationScheduler;
    private final Executor uiExecutor;
    private final Object listenerStateLock = new Object();
    private final Set<Path> registeredExternalRoots = new LinkedHashSet<>();
    private final FileChangeAdapter projectListener = new FileChangeAdapter() {
        @Override
        public void fileDataCreated(FileEvent event) {
            changed(event);
        }

        @Override
        public void fileFolderCreated(FileEvent event) {
            changed(event);
        }

        @Override
        public void fileChanged(FileEvent event) {
            changed(event);
        }

        @Override
        public void fileDeleted(FileEvent event) {
            changed(event);
        }

        @Override
        public void fileRenamed(FileRenameEvent event) {
            if (affectsAssets(relativePath(event.getFile()))
                    || affectsAssets(previousRelativePath(event))) {
                requestRefresh();
            }
        }
    };
    private final FileChangeAdapter externalPackageListener =
            new FileChangeAdapter() {
                @Override
                public void fileDataCreated(FileEvent event) {
                    requestRefresh();
                }

                @Override
                public void fileFolderCreated(FileEvent event) {
                    requestRefresh();
                }

                @Override
                public void fileChanged(FileEvent event) {
                    requestRefresh();
                }

                @Override
                public void fileDeleted(FileEvent event) {
                    requestRefresh();
                }

                @Override
                public void fileRenamed(FileRenameEvent event) {
                    requestRefresh();
                }
    };
    private volatile boolean open;
    private long synchronizationGeneration;
    private Cancellable inFlightSynchronization;

    FlutterDesignerProjectAssetWatcher(
            FileObject projectDirectory,
            Runnable refresh) {
        this(
                projectDirectory,
                new DebouncedEdtRefreshScheduler(
                        Objects.requireNonNull(refresh, "refresh")));
    }

    FlutterDesignerProjectAssetWatcher(
            FileObject projectDirectory,
            RefreshScheduler scheduler) {
        this(projectDirectory, scheduler, FileUtilRegistration.INSTANCE);
    }

    FlutterDesignerProjectAssetWatcher(
            FileObject projectDirectory,
            RefreshScheduler scheduler,
            RecursiveListenerRegistration recursiveListeners) {
        this(
                projectDirectory,
                scheduler,
                recursiveListeners,
                task -> {
                    RequestProcessor.Task scheduled =
                            SYNCHRONIZATION_WORKER.create(task);
                    scheduled.schedule(0);
                    return scheduled::cancel;
                },
                EventQueue::invokeLater);
    }

    FlutterDesignerProjectAssetWatcher(
            FileObject projectDirectory,
            RefreshScheduler scheduler,
            RecursiveListenerRegistration recursiveListeners,
            SynchronizationScheduler synchronizationScheduler,
            Executor uiExecutor) {
        this.projectDirectory = Objects.requireNonNull(
                projectDirectory, "projectDirectory");
        File localDirectory = FileUtil.toFile(projectDirectory);
        this.localProjectRoot = localDirectory == null
                ? null
                : localDirectory.toPath().toAbsolutePath().normalize();
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.recursiveListeners = Objects.requireNonNull(
                recursiveListeners, "recursiveListeners");
        this.synchronizationScheduler = Objects.requireNonNull(
                synchronizationScheduler, "synchronizationScheduler");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
    }

    synchronized void open() {
        if (!open) {
            projectDirectory.addRecursiveListener(projectListener);
            open = true;
        }
    }

    /**
     * Replaces recursive listeners for validated external package roots on a
     * worker and publishes only a bounded, generation-current result on the UI
     * executor. A changed result requires one fresh inventory after the
     * callback, closing the snapshot-before-listener race.
     */
    void synchronizePackageRoots(
            Collection<Path> packageRoots,
            Consumer<SynchronizationResult> completion) {
        Objects.requireNonNull(completion, "completion");
        Set<Path> desired = externalRoots(packageRoots);
        PendingSynchronization pending;
        Cancellable superseded;
        synchronized (this) {
            if (!open) {
                throw new IllegalStateException(
                        "Flutter project asset watcher is not open");
            }
            synchronizationGeneration++;
            pending = new PendingSynchronization(
                    synchronizationGeneration, desired, completion);
            superseded = inFlightSynchronization;
            inFlightSynchronization = null;
        }
        cancel(superseded);
        scheduleSynchronization(pending);
    }

    private void scheduleSynchronization(PendingSynchronization pending) {
        Cancellable scheduled;
        try {
            scheduled = Objects.requireNonNull(
                    synchronizationScheduler.schedule(
                            () -> synchronizeOnWorker(pending)),
                    "synchronizationScheduler returned null");
        } catch (RuntimeException failure) {
            publishSynchronization(
                    pending,
                    SynchronizationResult.failure());
            return;
        }
        boolean retain;
        synchronized (this) {
            retain = open
                    && synchronizationGeneration == pending.generation();
            if (retain) {
                inFlightSynchronization = scheduled;
            }
        }
        if (!retain) {
            cancel(scheduled);
        }
    }

    private void synchronizeOnWorker(PendingSynchronization pending) {
        if (EventQueue.isDispatchThread()) {
            publishSynchronization(
                    pending,
                    SynchronizationResult.failure());
            return;
        }
        synchronized (this) {
            if (!open
                    || synchronizationGeneration != pending.generation()) {
                return;
            }
        }
        SynchronizationResult result;
        try {
            result = SynchronizationResult.success(
                    replacePackageRoots(pending.desiredRoots()));
        } catch (RuntimeException failure) {
            result = SynchronizationResult.failure();
        }
        publishSynchronization(pending, result);
    }

    private boolean replacePackageRoots(Set<Path> desired) {
        synchronized (listenerStateLock) {
            if (desired.equals(registeredExternalRoots)) {
                return false;
            }

            List<Path> additions = desired.stream()
                    .filter(root -> !registeredExternalRoots.contains(root))
                    .toList();
            List<Path> removals = registeredExternalRoots.stream()
                    .filter(root -> !desired.contains(root))
                    .toList();
            List<Path> added = new ArrayList<>();
            List<Path> removed = new ArrayList<>();
            try {
                // Add before removing so a package retained by a replacement
                // configuration never has an uncovered listener interval.
                for (Path root : additions) {
                    added.add(root);
                    recursiveListeners.add(externalPackageListener, root);
                }
                for (Path root : removals) {
                    removed.add(root);
                    recursiveListeners.remove(externalPackageListener, root);
                }
            } catch (RuntimeException failure) {
                rollbackListenerReplacement(added, removed);
                // Retain every possibly registered root so close() makes one
                // final best-effort removal when rollback was partial.
                registeredExternalRoots.addAll(added);
                throw new IllegalStateException(
                        "Could not synchronize external Flutter package asset listeners",
                        failure);
            }
            registeredExternalRoots.clear();
            registeredExternalRoots.addAll(desired);
            return true;
        }
    }

    private void publishSynchronization(
            PendingSynchronization pending,
            SynchronizationResult result) {
        try {
            uiExecutor.execute(() -> {
                synchronized (FlutterDesignerProjectAssetWatcher.this) {
                    if (!open
                            || synchronizationGeneration
                            != pending.generation()) {
                        return;
                    }
                    inFlightSynchronization = null;
                }
                pending.completion().accept(result);
            });
        } catch (RuntimeException ignored) {
            // A disposed UI dispatcher cannot publish listener authority.
        }
    }

    @Override
    public void close() {
        Cancellable cancelled;
        boolean detachProjectListener;
        synchronized (this) {
            detachProjectListener = open;
            open = false;
            synchronizationGeneration++;
            cancelled = inFlightSynchronization;
            inFlightSynchronization = null;
        }
        cancel(cancelled);
        if (detachProjectListener) {
            try {
                projectDirectory.removeRecursiveListener(projectListener);
            } catch (RuntimeException ignored) {
                // Continue releasing every external listener and scheduler.
            }
        }
        scheduleExternalListenerCleanup();
        try {
            scheduler.close();
        } catch (RuntimeException ignored) {
            // Disposal is fail-closed even if a scheduler backend vanished.
        }
    }

    private void scheduleExternalListenerCleanup() {
        Runnable cleanup = this::removeAllExternalListeners;
        try {
            synchronizationScheduler.schedule(cleanup);
        } catch (RuntimeException failure) {
            // Even a disposed/custom scheduler cannot force FileUtil removal
            // onto Swing. A virtual-thread fallback remains off the EDT.
            Thread.ofVirtual()
                    .name(getClass().getSimpleName() + "-listener-cleanup")
                    .start(cleanup);
        }
    }

    private void removeAllExternalListeners() {
        if (EventQueue.isDispatchThread()) {
            Thread.ofVirtual()
                    .name(getClass().getSimpleName() + "-listener-cleanup")
                    .start(this::removeAllExternalListeners);
            return;
        }
        synchronized (listenerStateLock) {
            for (Path root : List.copyOf(registeredExternalRoots)) {
                try {
                    recursiveListeners.remove(externalPackageListener, root);
                } catch (RuntimeException ignored) {
                    // A disappearing package root must not prevent disposal.
                }
            }
            registeredExternalRoots.clear();
        }
    }

    private Set<Path> externalRoots(Collection<Path> packageRoots) {
        Objects.requireNonNull(packageRoots, "packageRoots");
        if (packageRoots.size() > MAX_EXTERNAL_ROOTS) {
            throw new IllegalArgumentException(
                    "External Flutter package root count exceeds safe limit "
                    + MAX_EXTERNAL_ROOTS);
        }
        Set<Path> result = new LinkedHashSet<>();
        packageRoots.stream()
                .map(root -> Objects.requireNonNull(root, "packageRoot")
                        .toAbsolutePath().normalize())
                .sorted()
                .filter(root -> root.getParent() != null)
                .filter(root -> localProjectRoot == null
                        || (!root.equals(localProjectRoot)
                        && !root.startsWith(localProjectRoot)))
                .forEach(result::add);
        return Collections.unmodifiableSet(result);
    }

    private void rollbackListenerReplacement(
            List<Path> added,
            List<Path> removed) {
        for (Path root : removed) {
            try {
                recursiveListeners.add(externalPackageListener, root);
            } catch (RuntimeException ignored) {
                // The caller fails closed if exact restoration is impossible.
            }
        }
        for (Path root : added) {
            try {
                recursiveListeners.remove(externalPackageListener, root);
            } catch (RuntimeException ignored) {
                // The caller fails closed if exact restoration is impossible.
            }
        }
    }

    private void changed(FileEvent event) {
        if (affectsAssets(relativePath(event.getFile()))) {
            requestRefresh();
        }
    }

    private void requestRefresh() {
        if (open) {
            scheduler.request();
        }
    }

    private String relativePath(FileObject file) {
        if (file == null) {
            return null;
        }
        String relative = FileUtil.getRelativePath(projectDirectory, file);
        if (relative != null) {
            return normalize(relative);
        }
        String root = normalize(projectDirectory.getPath());
        String candidate = normalize(file.getPath());
        String prefix = root.isEmpty() ? "" : root + "/";
        return candidate.startsWith(prefix)
                ? candidate.substring(prefix.length())
                : null;
    }

    private String previousRelativePath(FileRenameEvent event) {
        String parent = relativePath(event.getFile().getParent());
        if (parent == null) {
            return null;
        }
        String leaf = event.getName();
        if (!event.getExt().isEmpty()) {
            leaf += "." + event.getExt();
        }
        return parent.isEmpty() ? leaf : parent + "/" + leaf;
    }

    static boolean affectsAssets(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        // Flutter accepts arbitrary asset names and this resolver detects
        // image formats by magic bytes, not suffix. Therefore every project
        // entry can affect a declared image inventory.
        return true;
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    interface RefreshScheduler extends AutoCloseable {
        void request();

        @Override
        void close();
    }

    interface RecursiveListenerRegistration {
        void add(FileChangeListener listener, Path root);

        void remove(FileChangeListener listener, Path root);
    }

    @FunctionalInterface
    interface SynchronizationScheduler {
        Cancellable schedule(Runnable task);
    }

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }

    record SynchronizationResult(
            boolean changed,
            Optional<String> failureDetail) {
        private static final String FAILURE_DETAIL =
                "External Flutter package listener synchronization failed.";

        SynchronizationResult {
            Objects.requireNonNull(failureDetail, "failureDetail");
            failureDetail = failureDetail.map(detail -> {
                String compact = detail.strip().replaceAll("\\s+", " ");
                if (compact.isEmpty() || compact.length() > 256) {
                    throw new IllegalArgumentException(
                            "Synchronization failure detail must contain 1..256 characters");
                }
                return compact;
            });
            if (changed && failureDetail.isPresent()) {
                throw new IllegalArgumentException(
                        "A failed synchronization cannot report a changed watch set");
            }
        }

        static SynchronizationResult success(boolean changed) {
            return new SynchronizationResult(changed, Optional.empty());
        }

        static SynchronizationResult failure() {
            return new SynchronizationResult(
                    false, Optional.of(FAILURE_DETAIL));
        }

        boolean successful() {
            return failureDetail.isEmpty();
        }
    }

    private record PendingSynchronization(
            long generation,
            Set<Path> desiredRoots,
            Consumer<SynchronizationResult> completion) {
        private PendingSynchronization {
            if (generation <= 0) {
                throw new IllegalArgumentException(
                        "synchronization generation must be positive");
            }
            desiredRoots = Collections.unmodifiableSet(new LinkedHashSet<>(
                    Objects.requireNonNull(desiredRoots, "desiredRoots")));
            Objects.requireNonNull(completion, "completion");
        }
    }

    private enum FileUtilRegistration implements RecursiveListenerRegistration {
        INSTANCE;

        @Override
        public void add(FileChangeListener listener, Path root) {
            FileUtil.addRecursiveListener(listener, root.toFile());
        }

        @Override
        public void remove(FileChangeListener listener, Path root) {
            FileUtil.removeRecursiveListener(listener, root.toFile());
        }
    }

    private static final class DebouncedEdtRefreshScheduler
            implements RefreshScheduler {
        private final Runnable refresh;
        private final RequestProcessor.Task task;
        private volatile boolean closed;

        private DebouncedEdtRefreshScheduler(Runnable refresh) {
            this.refresh = refresh;
            task = REFRESH_WORKER.create(this::publish);
        }

        @Override
        public void request() {
            if (!closed) {
                task.schedule(REFRESH_DELAY_MILLIS);
            }
        }

        private void publish() {
            EventQueue.invokeLater(() -> {
                if (!closed) {
                    refresh.run();
                }
            });
        }

        @Override
        public void close() {
            closed = true;
            task.cancel();
        }
    }

    private static void cancel(Cancellable task) {
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
                // Cancellation is only a hint; generation fencing is authority.
            }
        }
    }
}

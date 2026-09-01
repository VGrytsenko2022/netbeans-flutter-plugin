package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

class FlutterDesignerProjectAssetWatcherTest {

    @Test
    void pathFilterIncludesEveryPossibleMagicDetectedAssetName() {
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "pubspec.yaml"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                ".dart_tool/package_config.json"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                ".pubspec.yaml.123.tmp"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                ".dart_tool/.package_config.json.123.tmp"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "assets/logo.PNG"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "assets/2.0x"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "assets/icons"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "lib/main.dart"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "build/web/logo.png"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                ".fd_templates/screens/home.fd"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "assets/image.arbitrary-extension"));
        assertTrue(FlutterDesignerProjectAssetWatcher.affectsAssets(
                "assets/extensionless"));
        assertFalse(FlutterDesignerProjectAssetWatcher.affectsAssets(null));
    }

    @Test
    void recursiveWatcherRefreshesForAssetsAndStopsAfterClose()
            throws Exception {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject root = memory.getRoot();
        FileObject lib = FileUtil.createFolder(root, "lib");
        CountingScheduler scheduler = new CountingScheduler();
        FlutterDesignerProjectAssetWatcher watcher =
                new FlutterDesignerProjectAssetWatcher(root, scheduler);
        watcher.open();

        write(lib.createData("main.dart"), new byte[]{1});
        assertTrue(scheduler.requests.get() > 0);

        FileObject assets = FileUtil.createFolder(root, "assets");
        int afterFolder = scheduler.requests.get();
        assertTrue(afterFolder > 0);
        FileObject image = assets.createData("logo.png");
        write(image, new byte[]{2});
        assertTrue(scheduler.requests.get() > afterFolder);

        int beforeClose = scheduler.requests.get();
        watcher.close();
        write(image, new byte[]{3});
        assertEquals(beforeClose, scheduler.requests.get());
        assertTrue(scheduler.closed);
    }

    @Test
    void externalPackageListenersReplaceAtomicallyAndCloseCompletely() {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject project = memory.getRoot();
        CountingScheduler scheduler = new CountingScheduler();
        FakeRecursiveListeners listeners = new FakeRecursiveListeners();
        ControlledSynchronizationScheduler synchronization =
                new ControlledSynchronizationScheduler();
        QueuedUiExecutor ui = new QueuedUiExecutor();
        FlutterDesignerProjectAssetWatcher watcher =
                new FlutterDesignerProjectAssetWatcher(
                        project, scheduler, listeners, synchronization, ui);
        watcher.open();
        Path first = Path.of("external-package-a").toAbsolutePath().normalize();
        Path second = Path.of("external-package-b").toAbsolutePath().normalize();
        List<FlutterDesignerProjectAssetWatcher.SynchronizationResult> results =
                new ArrayList<>();

        watcher.synchronizePackageRoots(List.of(first), results::add);
        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        assertEquals(List.of(
                FlutterDesignerProjectAssetWatcher.SynchronizationResult
                        .success(true)), results);
        results.clear();
        watcher.synchronizePackageRoots(List.of(first), results::add);
        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        assertEquals(List.of(
                FlutterDesignerProjectAssetWatcher.SynchronizationResult
                        .success(false)), results);
        assertEquals(List.of(first), listeners.added);
        assertTrue(listeners.everyMutationOffEdt.get());

        listeners.fireChanged(first, project);
        assertEquals(1, scheduler.requests.get());

        results.clear();
        watcher.synchronizePackageRoots(List.of(second), results::add);
        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        assertEquals(List.of(
                FlutterDesignerProjectAssetWatcher.SynchronizationResult
                        .success(true)), results);
        assertEquals(List.of(first, second), listeners.added);
        assertEquals(List.of(first), listeners.removed);
        assertFalse(listeners.active.containsKey(first));
        listeners.fireChanged(second, project);
        assertEquals(2, scheduler.requests.get());

        FileChangeListener detachedListener = listeners.active.get(second);
        watcher.close();
        synchronization.runNextOffEdt();
        assertEquals(List.of(first, second), listeners.removed);
        detachedListener.fileChanged(new FileEvent(project));
        assertEquals(2, scheduler.requests.get());
        assertTrue(scheduler.closed);
    }

    @Test
    void failedExternalRegistrationRollsBackAndFailsClosedOnClose() {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject project = memory.getRoot();
        CountingScheduler scheduler = new CountingScheduler();
        FakeRecursiveListeners listeners = new FakeRecursiveListeners();
        ControlledSynchronizationScheduler synchronization =
                new ControlledSynchronizationScheduler();
        QueuedUiExecutor ui = new QueuedUiExecutor();
        FlutterDesignerProjectAssetWatcher watcher =
                new FlutterDesignerProjectAssetWatcher(
                        project, scheduler, listeners, synchronization, ui);
        watcher.open();
        Path first = Path.of("external-package-a").toAbsolutePath().normalize();
        Path failing = Path.of("external-package-failing")
                .toAbsolutePath().normalize();
        List<FlutterDesignerProjectAssetWatcher.SynchronizationResult> results =
                new ArrayList<>();
        watcher.synchronizePackageRoots(List.of(first), results::add);
        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        assertTrue(results.getFirst().successful());
        results.clear();
        listeners.failAdd = failing;

        watcher.synchronizePackageRoots(List.of(failing), results::add);
        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        FlutterDesignerProjectAssetWatcher.SynchronizationResult failure =
                results.getFirst();
        assertFalse(failure.successful());
        assertEquals(
                "External Flutter package listener synchronization failed.",
                failure.failureDetail().orElseThrow());
        assertFalse(failure.failureDetail().orElseThrow()
                .contains(failing.toString()));

        watcher.close();
        synchronization.runNextOffEdt();
        assertTrue(scheduler.closed);
        assertFalse(listeners.active.containsKey(first));
        assertTrue(listeners.everyMutationOffEdt.get());
    }

    @Test
    void synchronizationRunsOffEdtAndOnlyLatestGenerationPublishes()
            throws Exception {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject project = memory.getRoot();
        CountingScheduler scheduler = new CountingScheduler();
        FakeRecursiveListeners listeners = new FakeRecursiveListeners();
        ControlledSynchronizationScheduler synchronization =
                new ControlledSynchronizationScheduler();
        QueuedUiExecutor ui = new QueuedUiExecutor();
        FlutterDesignerProjectAssetWatcher watcher =
                new FlutterDesignerProjectAssetWatcher(
                        project, scheduler, listeners, synchronization, ui);
        watcher.open();
        Path first = Path.of("external-package-stale")
                .toAbsolutePath().normalize();
        Path second = Path.of("external-package-current")
                .toAbsolutePath().normalize();
        List<String> publications = new ArrayList<>();

        SwingUtilities.invokeAndWait(() -> watcher.synchronizePackageRoots(
                List.of(first),
                result -> publications.add("stale:" + result.changed())));
        synchronization.runNextOffEdt();
        assertEquals(1, ui.pendingCount());

        SwingUtilities.invokeAndWait(() -> watcher.synchronizePackageRoots(
                List.of(second),
                result -> {
                    assertTrue(EventQueue.isDispatchThread());
                    publications.add("current:" + result.changed());
                }));
        ui.runNextOnEdt();
        assertTrue(publications.isEmpty(),
                "superseded completion acquired UI publication authority");

        synchronization.runNextOffEdt();
        ui.runNextOnEdt();
        assertEquals(List.of("current:true"), publications);
        assertTrue(listeners.everyMutationOffEdt.get());
        assertFalse(listeners.active.containsKey(first));
        assertTrue(listeners.active.containsKey(second));

        SwingUtilities.invokeAndWait(() -> watcher.synchronizePackageRoots(
                List.of(first),
                result -> publications.add("closed:" + result.changed())));
        synchronization.runNextOffEdt();
        watcher.close();
        ui.runNextOnEdt();
        assertEquals(List.of("current:true"), publications,
                "a completion published after watcher disposal");
        synchronization.runNextOffEdt();
        assertFalse(listeners.active.containsKey(first));
        assertFalse(listeners.active.containsKey(second));
    }

    private static void write(FileObject file, byte[] bytes) throws Exception {
        try (OutputStream output = file.getOutputStream()) {
            output.write(bytes);
        }
    }

    private static final class CountingScheduler
            implements FlutterDesignerProjectAssetWatcher.RefreshScheduler {
        private final AtomicInteger requests = new AtomicInteger();
        private boolean closed;

        @Override
        public void request() {
            requests.incrementAndGet();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeRecursiveListeners implements
            FlutterDesignerProjectAssetWatcher.RecursiveListenerRegistration {
        private final Map<Path, FileChangeListener> active =
                new LinkedHashMap<>();
        private final List<Path> added = new ArrayList<>();
        private final List<Path> removed = new ArrayList<>();
        private final AtomicBoolean everyMutationOffEdt =
                new AtomicBoolean(true);
        private Path failAdd;

        @Override
        public void add(FileChangeListener listener, Path root) {
            everyMutationOffEdt.compareAndSet(
                    true, !EventQueue.isDispatchThread());
            if (root.equals(failAdd)) {
                throw new IllegalStateException("simulated registration failure");
            }
            active.put(root, listener);
            added.add(root);
        }

        @Override
        public void remove(FileChangeListener listener, Path root) {
            everyMutationOffEdt.compareAndSet(
                    true, !EventQueue.isDispatchThread());
            active.remove(root, listener);
            removed.add(root);
        }

        private void fireChanged(Path root, FileObject source) {
            FileChangeListener listener = active.get(root);
            if (listener != null) {
                listener.fileChanged(new FileEvent(source));
            }
        }
    }

    private static final class ControlledSynchronizationScheduler implements
            FlutterDesignerProjectAssetWatcher.SynchronizationScheduler {
        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public FlutterDesignerProjectAssetWatcher.Cancellable schedule(
                Runnable task) {
            pending.add(task);
            // Deliberately do not remove cancelled tasks. The production code
            // must rely on its generation fence, not backend cancellation.
            return () -> { };
        }

        private void runNextOffEdt() {
            assertFalse(pending.isEmpty(), "no synchronization task queued");
            Runnable task = pending.removeFirst();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }, "asset-listener-test-worker");
            worker.start();
            try {
                worker.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
            if (failure.get() != null) {
                throw new AssertionError(failure.get());
            }
        }
    }

    private static final class QueuedUiExecutor implements Executor {
        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
        }

        private int pendingCount() {
            return pending.size();
        }

        private void runNextOnEdt() {
            assertFalse(pending.isEmpty(), "no UI publication queued");
            Runnable task = pending.removeFirst();
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }
    }
}

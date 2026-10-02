package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import java.awt.EventQueue;
import java.util.Objects;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 * Watches only the two files which form the project-theme transaction.
 *
 * <p>The editor persists the generated Dart file and descriptor as one
 * verified pair. Local filesystem notifications can expose the individual
 * atomic replacements, so production refresh is coalesced before the Canvas
 * resolves the pair again. Direct temporary siblings are included, while
 * unrelated {@code .fd} form edits below {@code .fd_templates} are excluded.</p>
 */
final class FlutterDesignerProjectThemeWatcher implements AutoCloseable {
    private static final int REFRESH_DELAY_MILLIS = 180;
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterDesignerProjectThemeWatcher.class.getName(), 1, true);
    private static final String DESCRIPTOR =
            FlutterProjectThemePaths.DESCRIPTOR_WIRE_PATH;
    private static final String GENERATED_DART =
            FlutterProjectThemePaths.GENERATED_DART_WIRE_PATH;
    private static final String DESCRIPTOR_PARENT = parent(DESCRIPTOR);
    private static final String GENERATED_DART_PARENT = parent(GENERATED_DART);

    private final FileObject projectDirectory;
    private final RefreshScheduler scheduler;
    private final FileChangeAdapter listener = new FileChangeAdapter() {
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
            String current = relativePath(event.getFile());
            String previous = previousRelativePath(event);
            if (affectsTheme(current) || affectsTheme(previous)) {
                requestRefresh();
            }
        }
    };
    private volatile boolean open;

    FlutterDesignerProjectThemeWatcher(
            FileObject projectDirectory,
            Runnable refresh) {
        this(
                projectDirectory,
                new DebouncedEdtRefreshScheduler(
                        Objects.requireNonNull(refresh, "refresh")));
    }

    FlutterDesignerProjectThemeWatcher(
            FileObject projectDirectory,
            RefreshScheduler scheduler) {
        this.projectDirectory = Objects.requireNonNull(
                projectDirectory, "projectDirectory");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    void open() {
        if (open) {
            return;
        }
        open = true;
        projectDirectory.addRecursiveListener(listener);
    }

    @Override
    public void close() {
        if (!open) {
            return;
        }
        open = false;
        projectDirectory.removeRecursiveListener(listener);
        scheduler.close();
    }

    private void changed(FileEvent event) {
        if (affectsTheme(relativePath(event.getFile()))) {
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
        if (candidate.equals(root)) {
            return "";
        }
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

    static boolean affectsTheme(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String candidate = normalize(relativePath);
        return candidate.equals(DESCRIPTOR)
                || candidate.equals(GENERATED_DART)
                || candidate.equals(DESCRIPTOR_PARENT)
                || candidate.equals(GENERATED_DART_PARENT)
                || isTransactionTemporary(candidate, DESCRIPTOR)
                || isTransactionTemporary(candidate, GENERATED_DART);
    }

    private static boolean isTransactionTemporary(
            String candidate,
            String target) {
        String parent = parent(target);
        if (!candidate.startsWith(parent + "/")
                || candidate.indexOf('/', parent.length() + 1) >= 0) {
            return false;
        }
        String targetName = target.substring(target.lastIndexOf('/') + 1);
        String candidateName = candidate.substring(parent.length() + 1);
        return candidateName.startsWith("." + targetName + ".")
                && candidateName.endsWith(".tmp");
    }

    private static String parent(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    interface RefreshScheduler extends AutoCloseable {
        void request();

        @Override
        void close();
    }

    private static final class DebouncedEdtRefreshScheduler
            implements RefreshScheduler {
        private final Runnable refresh;
        private final RequestProcessor.Task task;
        private volatile boolean closed;

        private DebouncedEdtRefreshScheduler(Runnable refresh) {
            this.refresh = refresh;
            task = WORKER.create(this::publish);
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
}

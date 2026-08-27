package dev.flutter.netbeans.plugin.project;

import dev.flutter.netbeans.project.FlutterProjectPlatform;
import dev.flutter.netbeans.project.FlutterProjectPlatformResolver;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

/**
 * Project-scoped, observable view of the platform directories configured for a
 * Flutter application.
 *
 * <p>The filesystem remains authoritative. A platform is published only when
 * {@link FlutterProjectPlatformResolver} accepts its real canonical directory;
 * an occupied file or symbolic link is never advertised as configured.</p>
 */
public final class FlutterProjectPlatformProvider implements AutoCloseable {
    private final FileObject projectDirectory;
    private final Path projectRoot;
    private final ChangeSupport changes = new ChangeSupport(this);
    private final Object lock = new Object();
    private final FileChangeAdapter rootListener = new FileChangeAdapter() {
        @Override
        public void fileFolderCreated(FileEvent event) {
            refreshIfCanonical(event.getFile());
        }

        @Override
        public void fileDeleted(FileEvent event) {
            refreshIfCanonical(event.getFile());
        }

        @Override
        public void fileRenamed(FileRenameEvent event) {
            if (isCanonicalDirectChild(event.getFile())
                    || isCanonicalName(oldName(event))) {
                refresh();
            }
        }
    };

    private Set<FlutterProjectPlatform> configuredPlatforms;
    private boolean started;

    FlutterProjectPlatformProvider(FileObject projectDirectory, Path projectRoot) {
        this.projectDirectory = Objects.requireNonNull(
                projectDirectory, "projectDirectory");
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        configuredPlatforms =
                FlutterProjectPlatformResolver.configuredPlatforms(this.projectRoot);
    }

    /** Returns the last immutable authoritative snapshot. */
    public Set<FlutterProjectPlatform> configuredPlatforms() {
        synchronized (lock) {
            return configuredPlatforms;
        }
    }

    /** Adds a listener notified only when the configured platform set changes. */
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    /** Starts filesystem observation and rescans the project. Idempotent. */
    void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            started = true;
            projectDirectory.addFileChangeListener(rootListener);
        }
        refresh();
    }

    /**
     * Recomputes the snapshot from the filesystem. Calls made while the project
     * service is closed are ignored; the next {@link #start()} performs a full
     * rescan.
     */
    public void refresh() {
        Set<FlutterProjectPlatform> detected =
                FlutterProjectPlatformResolver.configuredPlatforms(projectRoot);
        boolean changed;
        synchronized (lock) {
            if (!started) {
                return;
            }
            changed = !configuredPlatforms.equals(detected);
            if (changed) {
                configuredPlatforms = detected;
            }
        }
        if (changed) {
            changes.fireChange();
        }
    }

    /** Stops filesystem observation. Idempotent; the last snapshot is retained. */
    @Override
    public void close() {
        synchronized (lock) {
            if (!started) {
                return;
            }
            started = false;
            projectDirectory.removeFileChangeListener(rootListener);
        }
    }

    private void refreshIfCanonical(FileObject file) {
        if (isCanonicalDirectChild(file)) {
            refresh();
        }
    }

    private boolean isCanonicalDirectChild(FileObject file) {
        if (file == null || file.getParent() != projectDirectory) {
            return false;
        }
        String relative = FileUtil.getRelativePath(projectDirectory, file);
        return relative != null
                && relative.indexOf('/') < 0
                && isCanonicalName(relative);
    }

    private static boolean isCanonicalName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (FlutterProjectPlatform platform : FlutterProjectPlatform.values()) {
            if (platform.id().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String oldName(FileRenameEvent event) {
        return event.getExt() == null || event.getExt().isEmpty()
                ? event.getName()
                : event.getName() + '.' + event.getExt();
    }
}

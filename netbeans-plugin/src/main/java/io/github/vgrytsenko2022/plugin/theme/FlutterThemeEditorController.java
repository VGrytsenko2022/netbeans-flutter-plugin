package io.github.vgrytsenko2022.plugin.theme;

import io.github.vgrytsenko2022.api.FlutterProjectInfo;
import io.github.vgrytsenko2022.project.FlutterProjectType;
import io.github.vgrytsenko2022.project.FlutterProjectTypeDetector;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadResult;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadStatus;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeProvisioner;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeStore;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

/** Opens and persists the first safe modal project-theme editor slice. */
@Messages({
    "TTL_ThemeOpenFailure=Cannot open Flutter project themes",
    "TTL_ThemeEditFailure=Cannot edit Flutter project themes",
    "TTL_ThemeSaveFailure=Cannot save Flutter project themes",
    "TTL_ThemeCreateDefault=Create Default Flutter Themes",
    "# {0} - absolute Flutter project path",
    "# {1} - absolute descriptor path",
    "# {2} - absolute generated Dart path",
    "MSG_ThemeCreateDefault=This Flutter application at {0} has no project theme descriptor. Create default Light and Dark themes?\n\nDescriptor: {1}\nGenerated source: {2}\n\nThe operation also wires lib/main.dart transactionally. Existing or conflicting files are never overwritten.",
    "# {0} - project directory name",
    "TTL_ThemeEditor=Flutter Project Themes — {0}",
    "# {0} - absolute Flutter project path",
    "LBL_ThemeBackgroundTask=Preparing Flutter project themes for {0}",
    "# {0} - absolute Flutter project path",
    "MSG_ThemeEditorAlreadyActive=Open Flutter theme editor failed for {0}. Reason: a theme editor operation is already active for this project."
})
final class FlutterThemeEditorController {
    private static final Set<Path> ACTIVE_PROJECTS =
            ConcurrentHashMap.newKeySet();

    private final FlutterProjectThemeStore store;
    private final FlutterProjectThemeProvisioner provisioner;
    private final FlutterThemePersistence persistence;
    private final Dialogs dialogs;
    private final BackgroundExecutor background;
    private final UiAccess ui;

    FlutterThemeEditorController() {
        this(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                new FlutterThemePersistence(),
                new NetBeansDialogs(),
                new NetBeansBackgroundExecutor(),
                new EventDispatchUiAccess());
    }

    FlutterThemeEditorController(
            FlutterProjectThemeStore store,
            FlutterProjectThemeProvisioner provisioner,
            FlutterThemePersistence persistence,
            Dialogs dialogs) {
        this(
                store,
                provisioner,
                persistence,
                dialogs,
                (displayName, operation) -> operation.run(),
                new EventDispatchUiAccess());
    }

    FlutterThemeEditorController(
            FlutterProjectThemeStore store,
            FlutterProjectThemeProvisioner provisioner,
            FlutterThemePersistence persistence,
            Dialogs dialogs,
            BackgroundExecutor background,
            UiAccess ui) {
        this.store = Objects.requireNonNull(store, "store");
        this.provisioner = Objects.requireNonNull(provisioner, "provisioner");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.background = Objects.requireNonNull(background, "background");
        this.ui = Objects.requireNonNull(ui, "ui");
    }

    void openDescriptor(FileObject descriptorFile) {
        Objects.requireNonNull(descriptorFile, "descriptorFile");
        Project owner = FileOwnerQuery.getOwner(descriptorFile);
        FlutterProjectInfo info = owner == null
                ? null
                : owner.getLookup().lookup(FlutterProjectInfo.class);
        if (info == null) {
            showError(
                    Bundle.TTL_ThemeOpenFailure(),
                    "Open Flutter theme editor failed for "
                    + descriptorFile.getPath()
                    + ". Reason: the descriptor is not owned by a recognized "
                    + "Flutter project.");
            return;
        }
        File localFile = FileUtil.toFile(descriptorFile);
        Path expected = info.root().toAbsolutePath().normalize()
                .resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)
                .normalize();
        if (localFile == null
                || !localFile.toPath().toAbsolutePath().normalize().equals(expected)) {
            showError(
                    Bundle.TTL_ThemeOpenFailure(),
                    "Open Flutter theme editor failed for "
                    + descriptorFile.getPath()
                    + ". Reason: this is not the canonical project descriptor "
                    + expected + ".");
            return;
        }
        openProject(info.root());
    }

    void openProject(Path projectRoot) {
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!ACTIVE_PROJECTS.add(root)) {
            showError(
                    Bundle.TTL_ThemeOpenFailure(),
                    Bundle.MSG_ThemeEditorAlreadyActive(root.toString()));
            return;
        }
        try {
            background.execute(
                    Bundle.LBL_ThemeBackgroundTask(root.toString()),
                    () -> runEditor(root));
        } catch (RuntimeException schedulingFailure) {
            ACTIVE_PROJECTS.remove(root);
            showError(
                    Bundle.TTL_ThemeEditFailure(),
                    exactFailure("Edit Flutter project themes", root,
                            schedulingFailure));
        }
    }

    private void runEditor(Path root) {
        try {
            FlutterProjectType projectType = FlutterProjectTypeDetector.detect(root);
            if (projectType != FlutterProjectType.APP) {
                throw new IOException("Edit Flutter project themes failed for " + root
                        + ". Reason: project type is " + projectType.id()
                        + ", but project-wide MaterialApp themes require an app project.");
            }

            FlutterProjectThemeLoadResult loaded = store.load(root);
            if (loaded.status() == FlutterProjectThemeLoadStatus.MISSING) {
                if (!ui.call(() -> dialogs.confirmCreateDefault(root))) {
                    return;
                }
                provisioner.createDefaultIfMissing(root);
                FileUtil.refreshFor(root.toFile());
            } else if (!loaded.valid()) {
                throw new IOException("Edit Flutter project themes failed for " + root
                        + ". Reason: " + loaded.detail());
            }

            FlutterThemePersistence.Snapshot baseline = persistence.load(root);
            var initialTheme = baseline.theme();
            FlutterThemeEditorPanel panel = ui.call(
                    () -> new FlutterThemeEditorPanel(initialTheme));
            while (true) {
                EditResult edit = ui.call(() -> edit(root, panel));
                if (!edit.accepted()) {
                    return;
                }
                if (edit.failureReason() != null) {
                    showError(
                            Bundle.TTL_ThemeSaveFailure(),
                            "Save Flutter project themes failed for "
                            + root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)
                            + ". Reason: " + edit.failureReason());
                    continue;
                }
                try {
                    baseline = persistence.save(baseline, edit.theme());
                    FileUtil.refreshFor(
                            root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH).toFile(),
                            root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH).toFile());
                    return;
                } catch (IOException | IllegalArgumentException failure) {
                    showError(
                            Bundle.TTL_ThemeSaveFailure(),
                            exactFailure("Save Flutter project themes", root, failure));
                }
            }
        } catch (IOException | RuntimeException failure) {
            showError(
                    Bundle.TTL_ThemeEditFailure(),
                    exactFailure("Edit Flutter project themes", root, failure));
        } finally {
            ACTIVE_PROJECTS.remove(root);
        }
    }

    private EditResult edit(Path root, FlutterThemeEditorPanel panel) {
        if (!dialogs.edit(root, panel)) {
            return EditResult.cancelled();
        }
        if (!panel.isEditorValid()) {
            return EditResult.invalid(panel.validationMessage());
        }
        try {
            return EditResult.accepted(panel.buildTheme());
        } catch (IllegalArgumentException failure) {
            return EditResult.invalid(exactReason(failure));
        }
    }

    private void showError(String title, String message) {
        ui.run(() -> dialogs.error(title, message));
    }

    private static String exactFailure(
            String operation,
            Path target,
            Throwable failure) {
        String message = failure.getMessage();
        if (message != null
                && message.startsWith(operation)
                && message.contains("Reason:")) {
            return message;
        }
        String reason = message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.trim().replaceAll("\\s+", " ");
        return operation + " failed for " + target + ". Reason: " + reason;
    }

    private static String exactReason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message.trim().replaceAll("\\s+", " ");
    }

    interface Dialogs {
        boolean confirmCreateDefault(Path projectRoot);

        boolean edit(Path projectRoot, FlutterThemeEditorPanel panel);

        void error(String title, String message);
    }

    @FunctionalInterface
    interface BackgroundExecutor {
        void execute(String displayName, Runnable operation);
    }

    interface UiAccess {
        <T> T call(Supplier<T> operation);

        default void run(Runnable operation) {
            call(() -> {
                operation.run();
                return null;
            });
        }
    }

    private record EditResult(
            boolean accepted,
            io.github.vgrytsenko2022.project.theme.FlutterProjectTheme theme,
            String failureReason) {
        private static EditResult cancelled() {
            return new EditResult(false, null, null);
        }

        private static EditResult invalid(String reason) {
            return new EditResult(true, null, reason);
        }

        private static EditResult accepted(
                io.github.vgrytsenko2022.project.theme.FlutterProjectTheme theme) {
            return new EditResult(true, Objects.requireNonNull(theme, "theme"), null);
        }
    }

    static final class EventDispatchUiAccess implements UiAccess {
        @Override
        public <T> T call(Supplier<T> operation) {
            Objects.requireNonNull(operation, "operation");
            if (SwingUtilities.isEventDispatchThread()) {
                return operation.get();
            }
            FutureTask<T> task = new FutureTask<>(operation::get);
            try {
                SwingUtilities.invokeAndWait(task);
                return task.get();
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Flutter theme UI dispatch was interrupted", failure);
            } catch (ExecutionException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(
                        "Flutter theme UI dispatch failed", cause);
            } catch (java.lang.reflect.InvocationTargetException failure) {
                throw new IllegalStateException(
                        "Flutter theme UI dispatch failed", failure.getCause());
            }
        }
    }

    private static final class NetBeansBackgroundExecutor
            implements BackgroundExecutor {
        private static final RequestProcessor WORKER = new RequestProcessor(
                FlutterThemeEditorController.class.getName(), 2, true);

        @Override
        public void execute(String displayName, Runnable operation) {
            WORKER.post(() -> {
                ProgressHandle progress = ProgressHandle.createHandle(displayName);
                progress.start();
                try {
                    operation.run();
                } finally {
                    progress.finish();
                }
            });
        }
    }

    private static final class NetBeansDialogs implements Dialogs {
        @Override
        public boolean confirmCreateDefault(Path projectRoot) {
            String descriptor = projectRoot.resolve(
                    FlutterProjectThemePaths.DESCRIPTOR_PATH).toString();
            String dart = projectRoot.resolve(
                    FlutterProjectThemePaths.GENERATED_DART_PATH).toString();
            NotifyDescriptor confirmation = new NotifyDescriptor.Confirmation(
                    Bundle.MSG_ThemeCreateDefault(
                            projectRoot.toString(), descriptor, dart),
                    Bundle.TTL_ThemeCreateDefault(),
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE);
            return DialogDisplayer.getDefault().notify(confirmation)
                    == NotifyDescriptor.OK_OPTION;
        }

        @Override
        public boolean edit(Path projectRoot, FlutterThemeEditorPanel panel) {
            DialogDescriptor descriptor = new DialogDescriptor(
                    panel,
                    Bundle.TTL_ThemeEditor(projectRoot.getFileName().toString()),
                    true,
                    DialogDescriptor.OK_CANCEL_OPTION,
                    DialogDescriptor.OK_OPTION,
                    null);
            descriptor.setValid(panel.isEditorValid());
            java.beans.PropertyChangeListener validityListener = event -> {
                if (FlutterThemeEditorPanel.PROP_VALID.equals(event.getPropertyName())
                        || FlutterThemeEditorPanel.PROP_VALIDATION_MESSAGE.equals(
                                event.getPropertyName())) {
                    descriptor.setValid(panel.isEditorValid());
                }
            };
            panel.addPropertyChangeListener(validityListener);
            try {
                return DialogDisplayer.getDefault().notify(descriptor)
                        == DialogDescriptor.OK_OPTION;
            } finally {
                panel.removePropertyChangeListener(validityListener);
            }
        }

        @Override
        public void error(String title, String message) {
            NotifyDescriptor notification = new NotifyDescriptor.Message(
                    message, NotifyDescriptor.ERROR_MESSAGE);
            notification.setTitle(title);
            DialogDisplayer.getDefault().notify(notification);
        }
    }
}

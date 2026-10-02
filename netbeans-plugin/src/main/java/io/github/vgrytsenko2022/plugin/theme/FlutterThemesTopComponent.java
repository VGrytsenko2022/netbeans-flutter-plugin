package io.github.vgrytsenko2022.plugin.theme;

import io.github.vgrytsenko2022.project.FlutterProjectType;
import io.github.vgrytsenko2022.project.FlutterProjectTypeDetector;
import io.github.vgrytsenko2022.project.theme.FlutterProjectTheme;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadResult;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeLoadStatus;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemePaths;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeProvisioner;
import io.github.vgrytsenko2022.project.theme.FlutterProjectThemeStore;
import io.github.vgrytsenko2022.plugin.ui.FlutterFileIcons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Docked, project-aware editor for the canonical Flutter theme catalog. */
@TopComponent.Description(
        preferredID = FlutterThemesTopComponent.PREFERRED_ID,
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
        iconBase = FlutterFileIcons.THEME_FILE_ICON_PATH)
@TopComponent.Registration(
        mode = "commonpalette",
        openAtStartup = false,
        position = 120)
@ActionID(
        category = "Window",
        id = "io.github.vgrytsenko2022.plugin.theme.FlutterThemesTopComponent")
@ActionReference(path = "Menu/Window/Tools", position = 1260)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_FlutterThemesTopComponentAction",
        preferredID = FlutterThemesTopComponent.PREFERRED_ID)
@Messages({
    "CTL_FlutterThemesTopComponentAction=Flutter Themes",
    "CTL_FlutterThemesTopComponent=Themes",
    "HINT_FlutterThemesTopComponent=Edit the project-wide Flutter theme catalog",
    "CTL_ThemeSave=Save",
    "CTL_ThemeReload=Reload",
    "HINT_ThemeSave=Validate and save project.fdtheme and generated app_theme.dart",
    "HINT_ThemeReload=Discard this draft and reload the verified files from disk",
    "LBL_ThemeNoProject=<html><center>Open <b>.fd_templates/project.fdtheme</b><br>or choose Flutter &gt; Edit Flutter Themes...</center></html>",
    "LBL_ThemeLoading=Loading Flutter project themes...",
    "LBL_ThemeReady=Theme editor ready.",
    "LBL_ThemeSaving=Saving Flutter project themes...",
    "LBL_ThemeSaved=Project themes saved.",
    "LBL_ThemeUnsaved=Unsaved theme changes.",
    "TTL_ThemesWindowCreateDefault=Create Default Flutter Themes",
    "# {0} - absolute Flutter project path",
    "# {1} - absolute descriptor path",
    "# {2} - absolute generated Dart path",
    "MSG_ThemesWindowCreateDefault=This Flutter application at {0} has no project theme descriptor. Create default Light and Dark themes?\n\nDescriptor: {1}\nGenerated source: {2}\n\nExisting or conflicting files are never overwritten.",
    "TTL_ThemeDiscardChanges=Unsaved Flutter Theme Changes",
    "# {0} - project directory name",
    "MSG_ThemeDiscardChanges=The Flutter themes for {0} contain unsaved changes.",
    "CTL_ThemeSaveChoice=Save",
    "CTL_ThemeDiscardChoice=Discard",
    "CTL_ThemeCancelChoice=Cancel",
    "TTL_ThemeReloadChanges=Reload Flutter Themes",
    "# {0} - project directory name",
    "MSG_ThemeReloadChanges=Discard the unsaved Flutter theme changes for {0} and reload the verified files from disk?",
    "TTL_ThemeEditorFailure=Flutter Theme Editor",
    "# {0} - operation",
    "# {1} - absolute target path",
    "# {2} - exact reason",
    "MSG_ThemeOperationFailed={0} failed for {1}. Reason: {2}."
})
public final class FlutterThemesTopComponent extends TopComponent {
    public static final String PREFERRED_ID = "FlutterThemesTopComponent";

    private static final String EMPTY_CARD = "empty";
    private static final String EDITOR_CARD = "editor";
    private static final RequestProcessor WORKER = new RequestProcessor(
            FlutterThemesTopComponent.class.getName(), 1, true);

    private final FlutterProjectThemeStore store;
    private final FlutterProjectThemeProvisioner provisioner;
    private final FlutterThemePersistence persistence;
    private final Dialogs dialogs;
    private final BackgroundExecutor background;
    private final JLabel projectLabel = new JLabel(" ");
    private final JButton saveButton = new JButton(Bundle.CTL_ThemeSave());
    private final JButton reloadButton = new JButton(Bundle.CTL_ThemeReload());
    private final JLabel emptyLabel = new JLabel(
            Bundle.LBL_ThemeNoProject(), SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(Bundle.LBL_ThemeNoProject());
    private final JPanel cards = new JPanel(new CardLayout());
    private final JPanel editorHost = new JPanel(new BorderLayout());

    private Path projectRoot;
    private FlutterThemePersistence.Snapshot baseline;
    private FlutterThemeEditorPanel editor;
    private long generation;
    private boolean busy;
    private boolean dirty;

    public FlutterThemesTopComponent() {
        this(
                new FlutterProjectThemeStore(),
                new FlutterProjectThemeProvisioner(),
                new FlutterThemePersistence(),
                new NetBeansDialogs(),
                new NetBeansBackgroundExecutor());
    }

    FlutterThemesTopComponent(
            FlutterProjectThemeStore store,
            FlutterProjectThemeProvisioner provisioner,
            FlutterThemePersistence persistence,
            Dialogs dialogs,
            BackgroundExecutor background) {
        this.store = Objects.requireNonNull(store, "store");
        this.provisioner = Objects.requireNonNull(provisioner, "provisioner");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.background = Objects.requireNonNull(background, "background");

        setName(Bundle.CTL_FlutterThemesTopComponent());
        setToolTipText(Bundle.HINT_FlutterThemesTopComponent());
        setLayout(new BorderLayout());
        putClientProperty(PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);

        projectLabel.setFont(projectLabel.getFont().deriveFont(Font.BOLD));
        projectLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEADING, 4, 3));
        toolbar.add(projectLabel);
        toolbar.add(saveButton);
        toolbar.add(reloadButton);
        add(toolbar, BorderLayout.NORTH);

        emptyLabel.getAccessibleContext().setAccessibleName(
                "No Flutter theme project selected");
        emptyLabel.getAccessibleContext().setAccessibleDescription(
                "Open the canonical project.fdtheme file to load its theme catalog.");
        cards.add(emptyLabel, EMPTY_CARD);
        cards.add(editorHost, EDITOR_CARD);
        add(cards, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 5, 6));
        add(statusLabel, BorderLayout.SOUTH);

        saveButton.setToolTipText(Bundle.HINT_ThemeSave());
        reloadButton.setToolTipText(Bundle.HINT_ThemeReload());
        saveButton.addActionListener(event -> saveThen(null));
        reloadButton.addActionListener(event -> reload());
        configureAccessibility();
        showEmpty(Bundle.LBL_ThemeNoProject());
        updateActions();
    }

    /** Finds the registered singleton without creating a second editor. */
    public static FlutterThemesTopComponent findInstance() {
        TopComponent component = WindowManager.getDefault()
                .findTopComponent(PREFERRED_ID);
        return component instanceof FlutterThemesTopComponent themes
                ? themes : null;
    }

    /**
     * Synchronously persists a draft held by the registered Themes editor for
     * the supplied project. Run and Debug use this as part of their durable
     * save barrier; a draft for another project is deliberately left alone.
     */
    public static void saveProjectDraftBeforeLaunch(Path projectRoot)
            throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        FlutterThemesTopComponent component = onEdtIo(
                FlutterThemesTopComponent::findInstance);
        if (component != null) {
            component.saveBeforeLaunch(projectRoot);
        }
    }

    /** Opens and activates the registered palette-side editor for one project. */
    static boolean showProject(Path projectRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> showProject(projectRoot));
            return true;
        }
        FlutterThemesTopComponent component = findInstance();
        if (component == null) {
            showGlobalError(
                    "Open Flutter theme editor",
                    projectRoot,
                    "the registered Themes window is unavailable");
            return false;
        }
        component.open();
        component.requestActive();
        component.openProject(projectRoot);
        return true;
    }

    void openProject(Path requestedRoot) {
        requireEdt();
        Path root = requestedRoot.toAbsolutePath().normalize();
        // A verified save is a physical two-file transaction. It cannot be
        // truthfully discarded or redirected once the worker has started.
        if (busy) {
            return;
        }
        if (root.equals(projectRoot) && editor != null && !busy) {
            return;
        }
        if (dirty && projectRoot != null && !root.equals(projectRoot)) {
            switch (dialogs.unsavedChoice(projectRoot)) {
                case SAVE -> {
                    saveThen(() -> openProject(root));
                    return;
                }
                case DISCARD -> { }
                case CANCEL -> {
                    return;
                }
            }
        }
        beginLoad(root);
    }

    @Override
    public boolean canClose() {
        // Do not let a close/discard race an in-flight physical write. The
        // existing status text explains whether loading or saving is active;
        // the user can close as soon as that bounded operation completes.
        if (busy) {
            return false;
        }
        if (!dirty || projectRoot == null) {
            return true;
        }
        return switch (dialogs.unsavedChoice(projectRoot)) {
            case DISCARD -> {
                clearSession();
                yield true;
            }
            case CANCEL -> false;
            case SAVE -> {
                saveThen(this::close);
                yield false;
            }
        };
    }

    @Override
    protected void componentClosed() {
        if (!dirty) {
            clearSession();
        }
    }

    private void beginLoad(Path root) {
        long request = ++generation;
        busy = true;
        dirty = false;
        projectRoot = root;
        baseline = null;
        editor = null;
        projectLabel.setText(root.getFileName() == null
                ? root.toString() : root.getFileName().toString());
        projectLabel.setToolTipText(root.toString());
        showEmpty(Bundle.LBL_ThemeLoading());
        updateActions();
        background.execute(
                "Loading Flutter themes for " + root,
                () -> {
                    try {
                        FlutterThemePersistence.Snapshot loaded = prepare(root);
                        EventQueue.invokeLater(() -> completeLoad(request, root, loaded));
                    } catch (Cancelled ignored) {
                        EventQueue.invokeLater(() -> cancelLoad(request));
                    } catch (IOException | RuntimeException failure) {
                        EventQueue.invokeLater(() -> fail(
                                request,
                                "Open Flutter theme editor",
                                root,
                                failure));
                    }
                });
    }

    private FlutterThemePersistence.Snapshot prepare(Path root)
            throws IOException, Cancelled {
        FlutterProjectType type = FlutterProjectTypeDetector.detect(root);
        if (type != FlutterProjectType.APP) {
            throw new IOException("project type is " + type.id()
                    + ", but project-wide MaterialApp themes require an app project");
        }
        FlutterProjectThemeLoadResult loaded = store.load(root);
        if (loaded.status() == FlutterProjectThemeLoadStatus.MISSING) {
            if (!onEdt(() -> dialogs.confirmCreateDefault(root))) {
                throw new Cancelled();
            }
            provisioner.createDefaultIfMissing(root);
            FileUtil.refreshFor(root.toFile());
        } else if (!loaded.valid()) {
            throw new IOException(loaded.detail());
        }
        return persistence.load(root);
    }

    private void completeLoad(
            long request,
            Path root,
            FlutterThemePersistence.Snapshot loaded) {
        requireEdt();
        if (request != generation || !root.equals(projectRoot)) {
            return;
        }
        baseline = loaded;
        editor = new FlutterThemeEditorPanel(loaded.theme());
        editor.addPropertyChangeListener(event -> {
            if (FlutterThemeEditorPanel.PROP_CHANGED.equals(event.getPropertyName())
                    || FlutterThemeEditorPanel.PROP_VALID.equals(event.getPropertyName())
                    || FlutterThemeEditorPanel.PROP_VALIDATION_MESSAGE.equals(
                            event.getPropertyName())) {
                refreshDirtyState();
            }
        });
        editorHost.removeAll();
        JScrollPane scroll = new JScrollPane(editor);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        editorHost.add(scroll, BorderLayout.CENTER);
        editorHost.revalidate();
        editorHost.repaint();
        ((CardLayout) cards.getLayout()).show(cards, EDITOR_CARD);
        busy = false;
        dirty = false;
        statusLabel.setText(Bundle.LBL_ThemeReady());
        updateActions();
    }

    private void reload() {
        requireEdt();
        if (busy || projectRoot == null) {
            return;
        }
        if (dirty && !dialogs.confirmReload(projectRoot)) {
            return;
        }
        beginLoad(projectRoot);
    }

    /** Saves one captured live editor revision on the calling launch worker. */
    void saveBeforeLaunch(Path requestedRoot) throws IOException {
        Path root = Objects.requireNonNull(requestedRoot, "requestedRoot")
                .toAbsolutePath().normalize();
        ThemeLaunchSave request = onEdtIo(() -> prepareLaunchSave(root));
        if (request == null) {
            return;
        }

        final FlutterThemePersistence.Snapshot saved;
        try {
            saved = persistence.save(request.baseline(), request.edited());
            FileUtil.refreshFor(
                    request.descriptor().toFile(),
                    root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH).toFile());
        } catch (IOException | RuntimeException failure) {
            IOException reported = themeLaunchSaveFailure(
                    request.descriptor(), compact(failure.getMessage()), failure);
            onEdtIo(() -> {
                failLaunchSave(request, reported);
                return null;
            });
            throw reported;
        }

        onEdtIo(() -> {
            completeLaunchSave(request, saved);
            return null;
        });
    }

    private ThemeLaunchSave prepareLaunchSave(Path root) throws IOException {
        requireEdt();
        if (!root.equals(projectRoot)) {
            return null;
        }
        Path descriptor = root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH);
        if (busy) {
            throw themeLaunchSaveFailure(descriptor,
                    "the Themes editor is currently loading or saving");
        }
        refreshDirtyState();
        if (!dirty) {
            return null;
        }
        if (editor == null || baseline == null) {
            throw themeLaunchSaveFailure(descriptor,
                    "the Themes editor has no verified baseline to save against");
        }

        final FlutterProjectTheme edited;
        try {
            edited = editor.buildTheme();
        } catch (IllegalArgumentException failure) {
            throw themeLaunchSaveFailure(descriptor, compact(failure.getMessage()), failure);
        }
        if (!editor.isEditorValid()) {
            throw themeLaunchSaveFailure(descriptor,
                    compact(editor.validationMessage()));
        }
        if (sameEditableTheme(edited, baseline.theme())) {
            dirty = false;
            statusLabel.setText(Bundle.LBL_ThemeReady());
            updateActions();
            return null;
        }

        long request = ++generation;
        busy = true;
        statusLabel.setText(Bundle.LBL_ThemeSaving());
        updateActions();
        return new ThemeLaunchSave(
                request, root, descriptor, baseline, editor, edited);
    }

    private void completeLaunchSave(
            ThemeLaunchSave request,
            FlutterThemePersistence.Snapshot saved) throws IOException {
        requireEdt();
        if (!matchesLaunchSave(request)) {
            throw themeLaunchSaveFailure(request.descriptor(),
                    "the Themes editor project or revision changed while it was being saved");
        }
        baseline = saved;
        busy = false;
        refreshDirtyState();
        if (dirty || !editor.isEditorValid()) {
            throw themeLaunchSaveFailure(request.descriptor(),
                    "the live Themes editor revision changed while it was being saved");
        }
        statusLabel.setText(Bundle.LBL_ThemeSaved());
        updateActions();
    }

    private void failLaunchSave(ThemeLaunchSave request, IOException failure) {
        requireEdt();
        if (!matchesLaunchSave(request)) {
            return;
        }
        busy = false;
        refreshDirtyState();
        statusLabel.setText("Save before Flutter launch failed: "
                + compact(failure.getMessage()));
        updateActions();
    }

    private boolean matchesLaunchSave(ThemeLaunchSave request) {
        return request.request() == generation
                && request.root().equals(projectRoot)
                && request.editor() == editor;
    }

    private void saveThen(Runnable success) {
        requireEdt();
        if (busy || editor == null || baseline == null || projectRoot == null) {
            return;
        }
        final FlutterProjectTheme edited;
        try {
            edited = editor.buildTheme();
        } catch (IllegalArgumentException failure) {
            statusLabel.setText(compact(failure.getMessage()));
            updateActions();
            return;
        }
        if (!editor.isEditorValid()) {
            statusLabel.setText(editor.validationMessage());
            updateActions();
            return;
        }
        if (sameEditableTheme(edited, baseline.theme())) {
            dirty = false;
            statusLabel.setText(Bundle.LBL_ThemeReady());
            updateActions();
            if (success != null) {
                success.run();
            }
            return;
        }

        long request = ++generation;
        Path root = projectRoot;
        FlutterThemePersistence.Snapshot capturedBaseline = baseline;
        FlutterThemeEditorPanel capturedEditor = editor;
        busy = true;
        statusLabel.setText(Bundle.LBL_ThemeSaving());
        updateActions();
        background.execute(
                "Saving Flutter themes for " + root,
                () -> {
                    try {
                        FlutterThemePersistence.Snapshot saved =
                                persistence.save(capturedBaseline, edited);
                        FileUtil.refreshFor(
                                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)
                                        .toFile(),
                                root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)
                                        .toFile());
                        EventQueue.invokeLater(() -> completeSave(
                                request, root, capturedEditor, saved, success));
                    } catch (IOException | RuntimeException failure) {
                        EventQueue.invokeLater(() -> fail(
                                request,
                                "Save Flutter project themes",
                                root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH),
                                failure));
                    }
                });
    }

    private void completeSave(
            long request,
            Path root,
            FlutterThemeEditorPanel capturedEditor,
            FlutterThemePersistence.Snapshot saved,
            Runnable success) {
        requireEdt();
        if (request != generation
                || !root.equals(projectRoot)
                || capturedEditor != editor) {
            return;
        }
        baseline = saved;
        busy = false;
        refreshDirtyState();
        if (!dirty && editor.isEditorValid()) {
            statusLabel.setText(Bundle.LBL_ThemeSaved());
        }
        updateActions();
        if (success != null) {
            success.run();
        }
    }

    private void refreshDirtyState() {
        requireEdt();
        if (editor == null || baseline == null) {
            dirty = false;
        } else {
            try {
                dirty = !sameEditableTheme(editor.buildTheme(), baseline.theme());
            } catch (IllegalArgumentException failure) {
                dirty = true;
            }
        }
        if (!busy) {
            statusLabel.setText(editor != null && !editor.isEditorValid()
                    ? editor.validationMessage()
                    : dirty ? Bundle.LBL_ThemeUnsaved() : Bundle.LBL_ThemeReady());
        }
        updateActions();
    }

    /**
     * Compares only values controlled by the editor. The generated SHA is
     * persistence metadata and changes after a successful generation; treating
     * it as user input would leave an unchanged editor dirty after its first
     * save.
     */
    private static boolean sameEditableTheme(
            FlutterProjectTheme first,
            FlutterProjectTheme second) {
        return first.enabled() == second.enabled()
                && first.defaultMode() == second.defaultMode()
                && first.lightThemeId().equals(second.lightThemeId())
                && first.darkThemeId().equals(second.darkThemeId())
                && first.themes().equals(second.themes());
    }

    private void cancelLoad(long request) {
        requireEdt();
        if (request != generation) {
            return;
        }
        clearSession();
    }

    private void fail(
            long request,
            String operation,
            Path target,
            Throwable failure) {
        requireEdt();
        if (request != generation) {
            return;
        }
        busy = false;
        String reason = compact(failure.getMessage());
        statusLabel.setText(operation + " failed: " + reason);
        if (editor == null) {
            emptyLabel.setText("<html><center>" + operation
                    + " failed.<br>" + escapeHtml(reason) + "</center></html>");
        }
        updateActions();
        dialogs.error(
                Bundle.TTL_ThemeEditorFailure(),
                Bundle.MSG_ThemeOperationFailed(operation, target.toString(), reason));
    }

    private void showEmpty(String message) {
        emptyLabel.setText(message);
        statusLabel.setText(message);
        ((CardLayout) cards.getLayout()).show(cards, EMPTY_CARD);
    }

    private void clearSession() {
        requireEdt();
        generation++;
        busy = false;
        dirty = false;
        projectRoot = null;
        baseline = null;
        editor = null;
        editorHost.removeAll();
        projectLabel.setText(" ");
        projectLabel.setToolTipText(null);
        showEmpty(Bundle.LBL_ThemeNoProject());
        updateActions();
    }

    private void updateActions() {
        saveButton.setEnabled(!busy
                && dirty
                && editor != null
                && editor.isEditorValid());
        reloadButton.setEnabled(!busy && editor != null);
    }

    private void configureAccessibility() {
        getAccessibleContext().setAccessibleName(
                Bundle.CTL_FlutterThemesTopComponent());
        getAccessibleContext().setAccessibleDescription(
                Bundle.HINT_FlutterThemesTopComponent());
        saveButton.getAccessibleContext().setAccessibleName(
                Bundle.CTL_ThemeSave());
        saveButton.getAccessibleContext().setAccessibleDescription(
                Bundle.HINT_ThemeSave());
        reloadButton.getAccessibleContext().setAccessibleName(
                Bundle.CTL_ThemeReload());
        reloadButton.getAccessibleContext().setAccessibleDescription(
                Bundle.HINT_ThemeReload());
        statusLabel.getAccessibleContext().setAccessibleName(
                "Flutter theme editor status");
        statusLabel.getAccessibleContext().setAccessibleDescription(
                "Reports loading, validation, unsaved changes, save success, and exact failures.");
    }

    FlutterThemeEditorPanel editorForTest() {
        return editor;
    }

    JButton saveButtonForTest() {
        return saveButton;
    }

    JButton reloadButtonForTest() {
        return reloadButton;
    }

    JLabel statusLabelForTest() {
        return statusLabel;
    }

    Path projectRootForTest() {
        return projectRoot;
    }

    boolean dirtyForTest() {
        return dirty;
    }

    private static void requireEdt() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Flutter Themes window must be accessed on the Swing event thread");
        }
    }

    private static <T> T onEdt(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        FutureTask<T> task = new FutureTask<>(supplier::get);
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
            throw new IllegalStateException("Flutter theme UI dispatch failed", cause);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new IllegalStateException(
                    "Flutter theme UI dispatch failed", failure.getCause());
        }
    }

    private static <T> T onEdtIo(IoSupplier<T> supplier) throws IOException {
        Objects.requireNonNull(supplier, "supplier");
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        FutureTask<T> task = new FutureTask<>(supplier::get);
        try {
            SwingUtilities.invokeAndWait(task);
            return task.get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IOException("Flutter theme UI dispatch was interrupted", failure);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("Flutter theme UI dispatch failed", cause);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new IOException(
                    "Flutter theme UI dispatch failed", failure.getCause());
        }
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "no cause was reported";
        }
        String value = message.trim().replaceAll("\\s+", " ");
        return value.length() <= 600 ? value : value.substring(0, 600) + "…";
    }

    private static IOException themeLaunchSaveFailure(Path target, String reason) {
        return new IOException("cannot save accumulated Flutter theme changes in "
                + target + ": " + reason);
    }

    private static IOException themeLaunchSaveFailure(
            Path target,
            String reason,
            Throwable cause) {
        return new IOException("cannot save accumulated Flutter theme changes in "
                + target + ": " + reason, cause);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void showGlobalError(
            String operation,
            Path target,
            String reason) {
        NotifyDescriptor notification = new NotifyDescriptor.Message(
                Bundle.MSG_ThemeOperationFailed(
                        operation, target.toString(), compact(reason)),
                NotifyDescriptor.ERROR_MESSAGE);
        notification.setTitle(Bundle.TTL_ThemeEditorFailure());
        DialogDisplayer.getDefault().notify(notification);
    }

    enum UnsavedChoice {
        SAVE,
        DISCARD,
        CANCEL
    }

    interface Dialogs {
        boolean confirmCreateDefault(Path root);

        UnsavedChoice unsavedChoice(Path root);

        boolean confirmReload(Path root);

        void error(String title, String message);
    }

    @FunctionalInterface
    interface BackgroundExecutor {
        void execute(String displayName, Runnable operation);
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    private record ThemeLaunchSave(
            long request,
            Path root,
            Path descriptor,
            FlutterThemePersistence.Snapshot baseline,
            FlutterThemeEditorPanel editor,
            FlutterProjectTheme edited) {
    }

    private static final class NetBeansBackgroundExecutor
            implements BackgroundExecutor {
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
        public boolean confirmCreateDefault(Path root) {
            NotifyDescriptor confirmation = new NotifyDescriptor.Confirmation(
                    Bundle.MSG_ThemesWindowCreateDefault(
                            root.toString(),
                            root.resolve(FlutterProjectThemePaths.DESCRIPTOR_PATH)
                                    .toString(),
                            root.resolve(FlutterProjectThemePaths.GENERATED_DART_PATH)
                                    .toString()),
                    Bundle.TTL_ThemesWindowCreateDefault(),
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE);
            return DialogDisplayer.getDefault().notify(confirmation)
                    == NotifyDescriptor.OK_OPTION;
        }

        @Override
        public UnsavedChoice unsavedChoice(Path root) {
            String save = Bundle.CTL_ThemeSaveChoice();
            String discard = Bundle.CTL_ThemeDiscardChoice();
            String cancel = Bundle.CTL_ThemeCancelChoice();
            NotifyDescriptor choice = new NotifyDescriptor(
                    Bundle.MSG_ThemeDiscardChanges(displayName(root)),
                    Bundle.TTL_ThemeDiscardChanges(),
                    NotifyDescriptor.YES_NO_CANCEL_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE,
                    new Object[]{save, discard, cancel},
                    save);
            Object selected = DialogDisplayer.getDefault().notify(choice);
            if (Objects.equals(selected, save)) {
                return UnsavedChoice.SAVE;
            }
            if (Objects.equals(selected, discard)) {
                return UnsavedChoice.DISCARD;
            }
            return UnsavedChoice.CANCEL;
        }

        @Override
        public boolean confirmReload(Path root) {
            NotifyDescriptor confirmation = new NotifyDescriptor.Confirmation(
                    Bundle.MSG_ThemeReloadChanges(displayName(root)),
                    Bundle.TTL_ThemeReloadChanges(),
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE);
            return DialogDisplayer.getDefault().notify(confirmation)
                    == NotifyDescriptor.OK_OPTION;
        }

        @Override
        public void error(String title, String message) {
            NotifyDescriptor notification = new NotifyDescriptor.Message(
                    message, NotifyDescriptor.ERROR_MESSAGE);
            notification.setTitle(title);
            DialogDisplayer.getDefault().notify(notification);
        }

        private static String displayName(Path root) {
            Path name = root.getFileName();
            return name == null ? root.toString() : name.toString();
        }
    }

    private static final class Cancelled extends Exception {
        private static final long serialVersionUID = 1L;
    }
}

package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdCodecDiagnostic;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.RemoveWidget;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerPreviewPlatforms.PreviewTarget;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPalette;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDragLifecycle;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDragRegistry;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDropPlanner;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import dev.flutter.netbeans.plugin.project.FlutterProjectPlatformProvider;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.netbeans.spi.palette.PaletteController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

/** Minimal design surface proving the NetBeans multiview integration. */
@MultiViewElement.Registration(
        mimeType = FlutterDesignerMime.MIME_TYPE,
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
        displayName = "Design",
        preferredID = "flutter.designer.design",
        position = 100)
public final class FlutterDesignerMultiViewDesign
        implements MultiViewElement, PropertyChangeListener, ExplorerManager.Provider {
    static final String DELETE_WIDGET_ACTION_KEY = "delete";
    private final Lookup context;
    private final Lookup effectiveLookup;
    private final BooleanSupplier mutationUiEnabled;
    private final BooleanSupplier paletteCatalogInsertDndEnabled;
    private final FlutterDesignerDataObject dataObject;
    private final FlutterDesignerDocumentController controller;
    private final UndoRedo undoRedo;
    private final JPanel visual;
    private final JPanel statusPanel;
    private final JToolBar toolbar;
    private final JLabel statusLabel;
    private final JLabel modelLabel;
    private final JLabel detailLabel;
    private final JLabel canvasStatusLabel;
    private final JButton canvasDetailsButton;
    private final JProgressBar progress;
    private final JProgressBar canvasProgress;
    private final ExplorerManager explorerManager;
    private final Action deleteWidgetAction;
    private final FlutterDesignerPaletteDragRegistry paletteDragRegistry;
    private final FlutterDesignerPaletteDragLifecycle paletteDragLifecycle;
    private final FlutterDesignerPaletteDropPlanner paletteDropPlanner;
    private final PaletteController paletteController;
    private final BeanTreeView widgetTree;
    private final JComboBox<PreviewTarget> previewModes;
    private final FlutterProjectPlatformProvider projectPlatforms;
    private final FlutterDesignerProjectThemeResolver projectThemeResolver =
            new FlutterDesignerProjectThemeResolver();
    private final ChangeListener projectPlatformListener =
            event -> projectPlatformsChanged();
    private final WindowsNativeCanvasHost nativeCanvasHost;
    private final FlutterDesignerNativeCanvasSession nativeCanvasSession;
    private final String modelName;
    private final String sourceName;
    private final Map<StableId, Node> widgetNodes = new LinkedHashMap<>();
    private final AtomicBoolean mutationRefreshRequested = new AtomicBoolean();
    private final AtomicBoolean mutationRefreshScheduled = new AtomicBoolean();
    private final PropertyChangeListener mutationListener =
            this::mutationSnapshotChanged;
    private FlutterDesignerMutationController mutationController;
    private FlutterDesignerProjectThemeResolutionController
            projectThemeResolutionController;
    private FlutterDesignerProjectThemeWatcher projectThemeWatcher;
    private String projectThemeSetupFailure;
    private FlutterDesignerMutationController.Snapshot mutationSnapshot;
    private FlutterDesignerDocumentState durableDocumentState;
    private FlutterDesignerDocumentState.Current currentCanvasState;
    private DesignerDocument currentCanvasDocument;
    private WidgetCatalog currentCanvasCatalog;
    private boolean currentCanvasMutationEnabled;
    private DesignerDocument presentedCanvasDocument;
    private WidgetCatalog presentedCanvasCatalog;
    private PreviewTarget presentedCanvasTarget;
    private CanvasResolvedTheme presentedCanvasTheme;
    private FlutterDesignerNativeCanvasStatus lastCanvasStatus;
    private boolean previewInitialized;
    private boolean synchronizingSelection;
    private boolean updatingPreviewModes;
    private boolean listening;
    private boolean mutationListening;
    private boolean deleteWidgetSubmitting;
    private long mutationViewEpoch;
    private boolean platformListening;
    private boolean designVisible;

    public FlutterDesignerMultiViewDesign(Lookup context) {
        this(
                context,
                () -> DesignerCommandSessionOrchestrator.PUBLIC_MUTATION_UI_ENABLED,
                () -> DesignerCommandSessionOrchestrator
                        .PUBLIC_PALETTE_CATALOG_INSERT_DND_ENABLED);
    }

    FlutterDesignerMultiViewDesign(
            Lookup context,
            BooleanSupplier mutationUiEnabled) {
        this(
                context,
                mutationUiEnabled,
                () -> DesignerCommandSessionOrchestrator
                        .PUBLIC_PALETTE_CATALOG_INSERT_DND_ENABLED);
    }

    FlutterDesignerMultiViewDesign(
            Lookup context,
            BooleanSupplier mutationUiEnabled,
            BooleanSupplier paletteCatalogInsertDndEnabled) {
        this.context = context;
        this.mutationUiEnabled = Objects.requireNonNull(
                mutationUiEnabled, "mutationUiEnabled");
        this.paletteCatalogInsertDndEnabled = Objects.requireNonNull(
                paletteCatalogInsertDndEnabled, "paletteCatalogInsertDndEnabled");
        dataObject = context.lookup(FlutterDesignerDataObject.class);
        projectPlatforms = dataObject == null
                ? null
                : dataObject.getProject().getLookup()
                        .lookup(FlutterProjectPlatformProvider.class);
        modelName = dataObject == null
                ? "the .fd model"
                : dataObject.getModelFile().getNameExt();
        controller = dataObject == null ? null : dataObject.getDocumentController();
        undoRedo = dataObject == null
                ? UndoRedo.NONE : dataObject.getCombinedUndoRedo();
        sourceName = dataObject == null
                ? "the paired Dart source"
                : dataObject.getPrimaryFile().getNameExt();
        explorerManager = new ExplorerManager();
        visual = new ExplorerPanel(explorerManager);
        deleteWidgetAction = new AbstractAction("Delete Flutter Widget") {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSelectedWidget();
            }
        };
        deleteWidgetAction.putValue(
                Action.SHORT_DESCRIPTION,
                "Remove the selected non-root Flutter widget and its descendants");
        deleteWidgetAction.setEnabled(false);
        visual.getActionMap().put(DELETE_WIDGET_ACTION_KEY, deleteWidgetAction);
        paletteDragRegistry = new FlutterDesignerPaletteDragRegistry();
        paletteDragLifecycle = new FlutterDesignerPaletteDragLifecycle(
                paletteDragRegistry);
        paletteDropPlanner = new FlutterDesignerPaletteDropPlanner();
        paletteController = FlutterDesignerPalette.create(
                BuiltInWidgetCatalog.getDefault(),
                CanvasModelPayloadCodec::supports,
                paletteDragRegistry,
                this::isPaletteCatalogInsertDragEnabled,
                CanvasModelPayloadCodec::supports);
        effectiveLookup = new ProxyLookup(
                ExplorerUtils.createLookup(explorerManager, visual.getActionMap()),
                Lookups.exclude(context, Node.class),
                Lookups.singleton(paletteController));
        widgetTree = createWidgetTree();
        widgetTree.setRootVisible(true);
        widgetTree.setDefaultActionAllowed(false);
        widgetTree.setPopupAllowed(false);
        widgetTree.setMinimumSize(new java.awt.Dimension(180, 120));
        widgetTree.setPreferredSize(new java.awt.Dimension(240, 500));
        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        deleteWidgetAction.putValue(Action.ACCELERATOR_KEY, deleteKey);
        widgetTree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(deleteKey, DELETE_WIDGET_ACTION_KEY);
        widgetTree.getActionMap().put(
                DELETE_WIDGET_ACTION_KEY, deleteWidgetAction);
        visual.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(deleteKey, DELETE_WIDGET_ACTION_KEY);
        explorerManager.setRootContext(Node.EMPTY);
        explorerManager.addPropertyChangeListener(this::explorerSelectionChanged);
        statusPanel = new JPanel(new BorderLayout(16, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        statusLabel = centeredLabel("Preparing Flutter Designer model...");
        modelLabel = centeredLabel("Model: " + modelName + ". Source: " + sourceName + ".");
        detailLabel = centeredLabel("Waiting for bounded model validation.");
        modelLabel.setVisible(false);
        detailLabel.setVisible(false);
        canvasStatusLabel = centeredLabel("Native Canvas: waiting for the Design view.");
        canvasDetailsButton = new JButton();
        Mnemonics.setLocalizedText(canvasDetailsButton, "&Details...");
        canvasDetailsButton.setVisible(false);
        canvasDetailsButton.setEnabled(false);
        canvasDetailsButton.addActionListener(event -> showNativeCanvasDetails());
        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        progress.setVisible(false);
        statusLabel.setLabelFor(progress);
        canvasProgress = new JProgressBar();
        canvasProgress.setIndeterminate(true);
        canvasProgress.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        canvasProgress.setVisible(false);
        canvasStatusLabel.setLabelFor(canvasProgress);
        JPanel modelStatusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modelStatusRow.add(statusLabel);
        modelStatusRow.add(progress);
        JPanel canvasStatusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        canvasStatusRow.add(canvasStatusLabel);
        canvasStatusRow.add(canvasProgress);
        canvasStatusRow.add(canvasDetailsButton);
        JPanel hiddenStatusDetails = new JPanel();
        hiddenStatusDetails.add(modelLabel);
        hiddenStatusDetails.add(detailLabel);
        hiddenStatusDetails.setVisible(false);
        statusPanel.add(modelStatusRow, BorderLayout.WEST);
        statusPanel.add(hiddenStatusDetails, BorderLayout.CENTER);
        statusPanel.add(canvasStatusRow, BorderLayout.EAST);
        WindowsNativeCanvasHost canvasHost = null;
        FlutterDesignerNativeCanvasSession canvasSession = null;
        if (isWindows()) {
            try {
                canvasHost = new WindowsNativeCanvasHost();
                canvasSession = FlutterDesignerNativeCanvasSession.createDefault(
                        canvasHost,
                        this::renderNativeCanvasStatus,
                        this::selectWidgetFromCanvas,
                        this::consumePaletteDropToken,
                        this::applyAdmittedPaletteDrop,
                        deletion -> deleteSelectedWidgetFromCanvas(
                                deletion.widgetId()));
            } catch (IOException | RuntimeException | LinkageError failure) {
                canvasHost = null;
                canvasSession = null;
                renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                        "Native Flutter Canvas is unavailable.",
                        "Target: embedded Windows FlutterView. Reason: "
                        + failureReason(failure)));
            }
        } else {
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: embedded FlutterView. Reason: the current implementation "
                    + "is Windows-first; the platform host for this operating system "
                    + "is not available yet."));
        }
        nativeCanvasHost = canvasHost;
        nativeCanvasSession = canvasSession;
        JPanel canvasPanel = new JPanel(new BorderLayout());
        // Status copy is deliberately complete for accessibility and diagnostics,
        // but a long JLabel must not become the right side's split-pane minimum.
        // Otherwise JSplitPane clamps its minimum and maximum divider locations
        // to the same value as soon as the validated-model detail is rendered.
        canvasPanel.setMinimumSize(new Dimension(320, 120));
        if (nativeCanvasHost != null) {
            canvasPanel.add(nativeCanvasHost, BorderLayout.CENTER);
        }
        canvasPanel.add(statusPanel, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, widgetTree, canvasPanel);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.0d);
        split.setDividerLocation(240);
        visual.add(split, BorderLayout.CENTER);
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel(modelName + " — Design"));
        toolbar.addSeparator();
        JLabel previewLabel = new JLabel("Preview:");
        previewModes = new JComboBox<>(availablePreviewChoices()
                .toArray(PreviewTarget[]::new));
        previewModes.setEnabled(previewModes.getItemCount() > 0);
        int widestPreviewLabel = FlutterDesignerPreviewPlatforms.allTargets().stream()
                .mapToInt(target -> previewModes.getFontMetrics(previewModes.getFont())
                        .stringWidth(target.toString()))
                .max()
                .orElse(0);
        previewModes.setMaximumSize(new java.awt.Dimension(
                Math.max(220, widestPreviewLabel + 48),
                previewModes.getPreferredSize().height));
        previewLabel.setLabelFor(previewModes);
        toolbar.add(previewLabel);
        toolbar.add(previewModes);
        previewModes.addActionListener(event -> previewModeChanged());
        configureAccessibility();
        updatePreviewModeAccessibility();
    }

    private static JLabel centeredLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return label;
    }

    private static BeanTreeView createWidgetTree() {
        if (java.awt.EventQueue.isDispatchThread()) {
            return new BeanTreeView();
        }
        java.util.concurrent.atomic.AtomicReference<BeanTreeView> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        try {
            java.awt.EventQueue.invokeAndWait(() -> {
                try {
                    result.set(new BeanTreeView());
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while creating the Flutter Designer widget tree",
                    interrupted);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            throw new IllegalStateException(
                    "Could not create the Flutter Designer widget tree",
                    exception.getCause());
        }
        if (failure.get() != null) {
            throw new IllegalStateException(
                    "Could not create the Flutter Designer widget tree",
                    failure.get());
        }
        return result.get();
    }

    @Override
    public JComponent getVisualRepresentation() {
        return visual;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return toolbar;
    }

    @Override
    public Action[] getActions() {
        return new Action[0];
    }

    @Override
    public Lookup getLookup() {
        return effectiveLookup;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    @Override
    public void componentOpened() {
        deleteWidgetSubmitting = false;
        updateDeleteWidgetAction();
        if (paletteCatalogInsertDndEnabled.getAsBoolean()
                && mutationUiEnabled.getAsBoolean()) {
            paletteDragLifecycle.install();
        } else {
            paletteDragLifecycle.uninstall();
        }
        if (projectPlatforms != null && !platformListening) {
            platformListening = true;
            projectPlatforms.addChangeListener(projectPlatformListener);
            projectPlatforms.refresh();
            refreshPreviewChoices(selectedPreviewTarget().orElse(null), null);
        }
        if (dataObject != null
                && nativeCanvasSession != null
                && projectThemeResolutionController == null) {
            try {
                Path projectRoot = Path.of(
                        dataObject.getProject().getProjectDirectory().toURI())
                        .toAbsolutePath().normalize();
                projectThemeResolutionController =
                        new FlutterDesignerProjectThemeResolutionController(
                                projectRoot,
                                projectThemeResolver,
                                this::projectThemeResolutionChanged);
                projectThemeWatcher = new FlutterDesignerProjectThemeWatcher(
                        dataObject.getProject().getProjectDirectory(),
                        this::projectThemeFilesChanged);
                projectThemeWatcher.open();
                projectThemeSetupFailure = null;
            } catch (RuntimeException failure) {
                if (projectThemeWatcher != null) {
                    projectThemeWatcher.close();
                    projectThemeWatcher = null;
                }
                if (projectThemeResolutionController != null) {
                    projectThemeResolutionController.close();
                    projectThemeResolutionController = null;
                }
                projectThemeSetupFailure = failureReason(failure);
            }
        }
        if (dataObject != null && !mutationListening) {
            mutationController = dataObject.mutationController();
            mutationViewEpoch++;
            mutationListening = true;
            mutationController.addPropertyChangeListener(mutationListener);
            scheduleMutationSnapshotRefresh();
        }
        if (controller != null && !listening) {
            listening = true;
            controller.addPropertyChangeListener(this);
            boolean firstView = controller.viewOpened();
            render(openingState(firstView, controller.state(), modelName));
        }
    }

    @Override
    public void componentClosed() {
        designVisible = false;
        deleteWidgetSubmitting = false;
        invalidatePaletteDragAuthority();
        paletteDragLifecycle.uninstall();
        if (projectPlatforms != null && platformListening) {
            platformListening = false;
            projectPlatforms.removeChangeListener(projectPlatformListener);
        }
        if (projectThemeWatcher != null) {
            projectThemeWatcher.close();
            projectThemeWatcher = null;
        }
        if (projectThemeResolutionController != null) {
            projectThemeResolutionController.close();
            projectThemeResolutionController = null;
        }
        projectThemeSetupFailure = null;
        if (mutationController != null && mutationListening) {
            mutationViewEpoch++;
            mutationListening = false;
            mutationController.removePropertyChangeListener(mutationListener);
        }
        mutationRefreshRequested.set(false);
        mutationSnapshot = null;
        durableDocumentState = null;
        currentCanvasState = null;
        currentCanvasDocument = null;
        currentCanvasCatalog = null;
        currentCanvasMutationEnabled = false;
        clearPresentedCanvasIdentity();
        clearWidgetTree();
        if (nativeCanvasSession != null) {
            nativeCanvasSession.close();
        }
        if (controller != null && listening) {
            listening = false;
            controller.removePropertyChangeListener(this);
            controller.viewClosed();
        }
    }

    @Override
    public void componentShowing() {
        designVisible = true;
        invalidatePaletteDragAuthority();
        if (nativeCanvasSession != null) {
            nativeCanvasSession.show();
        }
    }

    @Override
    public void componentHidden() {
        designVisible = false;
        invalidatePaletteDragAuthority();
        if (nativeCanvasSession != null) {
            nativeCanvasSession.hide();
        }
    }

    @Override
    public void componentActivated() {
        FlutterDesignerAuxiliaryWindows.openDefaultOnce();
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (FlutterDesignerDocumentController.PROP_STATE.equals(event.getPropertyName())) {
            render((FlutterDesignerDocumentState) event.getNewValue());
        }
    }

    private void mutationSnapshotChanged(PropertyChangeEvent event) {
        if (FlutterDesignerMutationController.PROP_SNAPSHOT
                .equals(event.getPropertyName())) {
            scheduleMutationSnapshotRefresh();
        }
    }

    /** Coalesces background controller publications into one latest-snapshot EDT refresh. */
    private void scheduleMutationSnapshotRefresh() {
        mutationRefreshRequested.set(true);
        if (mutationRefreshScheduled.compareAndSet(false, true)) {
            java.awt.EventQueue.invokeLater(this::drainMutationSnapshotRefresh);
        }
    }

    private void drainMutationSnapshotRefresh() {
        while (mutationRefreshRequested.getAndSet(false)) {
            FlutterDesignerMutationController currentController = mutationController;
            if (currentController != null && mutationListening) {
                renderMutationSnapshot(currentController.snapshot());
            }
        }
        mutationRefreshScheduled.set(false);
        if (mutationListening
                && mutationRefreshRequested.get()
                && mutationRefreshScheduled.compareAndSet(false, true)) {
            java.awt.EventQueue.invokeLater(this::drainMutationSnapshotRefresh);
        }
    }

    private void renderMutationSnapshot(
            FlutterDesignerMutationController.Snapshot snapshot) {
        mutationSnapshot = snapshot;
        FlutterDesignerDocumentState durable = durableDocumentState;
        boolean exactDurableCurrent = durable
                instanceof FlutterDesignerDocumentState.Current current
                && canvasEligible(current);
        publishMutationCanvas(snapshot, exactDurableCurrent);
        progress.setVisible(
                durable instanceof FlutterDesignerDocumentState.Loading
                || snapshot.status() == FlutterDesignerMutationController.Status.APPLYING);
        if (durable instanceof FlutterDesignerDocumentState.Current current) {
            // Durable validation remains authoritative; mutation readiness only
            // enriches its status detail and the writable-node projection.
            renderCurrent(current);
        }
    }

    private void render(FlutterDesignerDocumentState state) {
        durableDocumentState = state;
        if (mutationController != null && mutationListening) {
            // The controller listener is registered first. Read its volatile
            // latest snapshot rather than replaying event payloads, and retain
            // its semantic presentation across durable Loading/save/reload.
            mutationSnapshot = mutationController.snapshot();
            publishMutationCanvas(mutationSnapshot, false);
        } else {
            if (state instanceof FlutterDesignerDocumentState.Current current
                    && canvasEligible(current)) {
                publishDurableCanvas(current);
            } else {
                withdrawCanvas();
            }
        }
        progress.setVisible(
                state instanceof FlutterDesignerDocumentState.Loading
                || mutationSnapshot != null
                && mutationSnapshot.status()
                == FlutterDesignerMutationController.Status.APPLYING);
        if (state instanceof FlutterDesignerDocumentState.Idle) {
            setPresentation(
                    "Flutter Designer is ready.",
                    "Model: " + modelName + ".",
                    "Open the Design view to load the model.");
        } else if (state instanceof FlutterDesignerDocumentState.Loading) {
            setPresentation(
                    "Loading Flutter Designer model...",
                    "Model: " + modelName + ". Source: " + sourceName + ".",
                    "Reading bounded UTF-8 .fd and paired Dart snapshots, validating "
                    + "the model and source structure, generating both managed payloads "
                    + "in memory, and comparing actual, declared and generated SHA-256 "
                    + "values. No files are written.");
        } else if (state instanceof FlutterDesignerDocumentState.Current current) {
            renderCurrent(current);
        } else if (state instanceof FlutterDesignerDocumentState.UnsupportedNewer newer) {
            setPresentation(
                    "Flutter Designer model opened read-only.",
                    "Model: " + modelName + ".",
                    "Schema version " + newer.decoded().declaredSchemaVersion()
                    + " is newer than supported version "
                    + DesignerDocument.SCHEMA_VERSION + ".");
        } else if (state instanceof FlutterDesignerDocumentState.Invalid invalid) {
            FdCodecDiagnostic diagnostic = invalid.decoded().diagnostics().get(0);
            String pointer = diagnostic.pointer().isEmpty() ? "/" : diagnostic.pointer();
            setPresentation(
                    "Cannot load Flutter Designer model.",
                    "Model: " + modelName + ".",
                    diagnostic.code() + " at " + pointer + ": " + diagnostic.message());
        } else if (state instanceof FlutterDesignerDocumentState.InputTooLarge tooLarge) {
            setPresentation(
                    "Cannot read Flutter Designer model.",
                    "Model: " + tooLarge.modelFileName() + ".",
                    "File size " + tooLarge.observedBytes() + " bytes exceeds the "
                    + tooLarge.maximumBytes() + " byte safety limit.");
        } else if (state instanceof FlutterDesignerDocumentState.Failure failure) {
            setPresentation(
                    failure.operation() + " failed.",
                    "Target: " + failure.target() + ".",
                    "Reason: " + failure.reason());
        }
    }

    private static boolean canvasEligible(
            FlutterDesignerDocumentState.Current current) {
        return current.validation().valid()
                && current.catalogDiagnostics().isEmpty()
                && current.contextIssues().isEmpty();
    }

    private void publishDurableCanvas(
            FlutterDesignerDocumentState.Current current) {
        publishCanvasPresentation(
                current.decoded().document(),
                current.catalog(),
                null);
        currentCanvasState = current;
    }

    private void publishMutationCanvas(
            FlutterDesignerMutationController.Snapshot snapshot,
            boolean allowMutation) {
        if (snapshot.document().isEmpty() || snapshot.catalog().isEmpty()) {
            // Binding the DataObject-owned mutation controller is asynchronous.
            // Its initial WAITING (or a fail-closed BLOCKED) snapshot therefore
            // may not have a semantic presentation yet.  Do not blank an exact,
            // already validated durable Current while that authority is being
            // resolved: publish it without a mutation handler, so the widget
            // tree, Properties and Canvas remain useful but strictly read-only.
            if (durableDocumentState
                    instanceof FlutterDesignerDocumentState.Current current
                    && canvasEligible(current)) {
                publishDurableCanvas(current);
            } else {
                withdrawCanvasPresentation();
            }
            return;
        }
        publishCanvasPresentation(
                snapshot.document().orElseThrow(),
                snapshot.catalog().orElseThrow(),
                allowMutation ? propertyMutationHandler(snapshot) : null);
    }

    private void publishCanvasPresentation(
            DesignerDocument document,
            WidgetCatalog catalog,
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler) {
        invalidatePaletteDragAuthority();
        StableId retainedSelection = selectedWidgetId().orElse(null);
        currentCanvasDocument = Objects.requireNonNull(document, "document");
        currentCanvasCatalog = Objects.requireNonNull(catalog, "catalog");
        currentCanvasMutationEnabled = mutationHandler != null;
        if (!previewInitialized) {
            previewInitialized = true;
            CanvasPreviewMode initial =
                    dev.flutter.netbeans.designer.canvas.CanvasPreviewProfileResolver
                            .initialMode(currentCanvasDocument.canvas());
            refreshPreviewChoices(null, initial);
        }
        rebuildWidgetTree(
                currentCanvasDocument.root(),
                currentCanvasCatalog,
                retainedSelection,
                mutationHandler);
        presentCurrentCanvas();
    }

    /** Read-only package seam for proving which revision feeds the Canvas. */
    DesignerDocument currentCanvasDocumentForTests() {
        return currentCanvasDocument;
    }

    private void withdrawCanvas() {
        if (currentCanvasState == null
                && currentCanvasDocument == null
                && widgetNodes.isEmpty()) {
            return;
        }
        currentCanvasState = null;
        withdrawCanvasPresentation();
    }

    private void withdrawCanvasPresentation() {
        invalidatePaletteDragAuthority();
        currentCanvasDocument = null;
        currentCanvasCatalog = null;
        currentCanvasMutationEnabled = false;
        clearPresentedCanvasIdentity();
        clearWidgetTree();
        if (nativeCanvasSession != null) {
            nativeCanvasSession.withdraw();
        }
    }

    private void previewModeChanged() {
        if (updatingPreviewModes) {
            return;
        }
        if (currentCanvasDocument != null) {
            presentCurrentCanvas();
        }
    }

    private Optional<PreviewTarget> selectedPreviewTarget() {
        Object selected = previewModes.getSelectedItem();
        return selected instanceof PreviewTarget target
                ? Optional.of(target)
                : Optional.empty();
    }

    private void presentCurrentCanvas() {
        invalidatePaletteDragAuthority();
        DesignerDocument document = currentCanvasDocument;
        WidgetCatalog catalog = currentCanvasCatalog;
        if (nativeCanvasSession == null || document == null || catalog == null) {
            return;
        }
        Optional<PreviewTarget> selected = selectedPreviewTarget();
        if (selected.isEmpty()) {
            clearPresentedCanvasIdentity();
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: " + modelName + " preview. Reason: the Flutter project "
                    + "has no configured Android, iOS, Web, Windows, macOS or Linux "
                    + "platform directory."));
            return;
        }
        PreviewTarget target = selected.orElseThrow();
        if (dataObject == null) {
            clearPresentedCanvasIdentity();
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: " + modelName + " project theme. Reason: the owning "
                    + "Flutter project is unavailable."));
            return;
        }
        if (projectThemeResolutionController == null) {
            clearPresentedCanvasIdentity();
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: " + modelName + " project theme. Reason: the project "
                    + "theme verifier is unavailable"
                    + (projectThemeSetupFailure == null
                            ? "."
                            : ": " + projectThemeSetupFailure + ".")));
            return;
        }
        Optional<DesignerThemeMode> previewOverride = document.canvas()
                .flatMap(preferences -> preferences.themeMode());
        Optional<FlutterDesignerProjectThemeResolver.Resolution> cachedTheme =
                projectThemeResolutionController.request(
                        previewOverride,
                        FlutterDesignerProjectThemeResolver.uiBrightness());
        if (cachedTheme.isEmpty()) {
            clearPresentedCanvasIdentity();
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                    "Resolving Flutter project theme...",
                    "Target: " + modelName + " project theme. Verifying the bounded "
                    + "descriptor and generated Dart hash outside the UI thread."));
            return;
        }
        FlutterDesignerProjectThemeResolver.Resolution themeResolution =
                cachedTheme.orElseThrow();
        if (!themeResolution.available()) {
            clearPresentedCanvasIdentity();
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas theme is unavailable.",
                    "Target: " + modelName + " project theme. Reason: "
                    + themeResolution.detail()));
            return;
        }
        CanvasResolvedTheme resolvedTheme = themeResolution.theme().orElseThrow();
        if (presentedCanvasDocument == document
                && presentedCanvasCatalog == catalog
                && Objects.equals(presentedCanvasTarget, target)
                && Objects.equals(presentedCanvasTheme, resolvedTheme)) {
            selectedWidgetId().ifPresent(nativeCanvasSession::selectWidget);
            return;
        }
        nativeCanvasSession.present(
                document,
                catalog,
                target.mode(),
                target.targetPlatform(),
                resolvedTheme);
        presentedCanvasDocument = document;
        presentedCanvasCatalog = catalog;
        presentedCanvasTarget = target;
        presentedCanvasTheme = resolvedTheme;
        selectedWidgetId().ifPresent(nativeCanvasSession::selectWidget);
    }

    private List<PreviewTarget> availablePreviewChoices() {
        if (projectPlatforms == null) {
            return FlutterDesignerPreviewPlatforms.allTargets();
        }
        return FlutterDesignerPreviewPlatforms.compatibleTargets(
                projectPlatforms.configuredPlatforms());
    }

    /** Replaces the combo model without publishing intermediate selection events. */
    private boolean refreshPreviewChoices(
            PreviewTarget preferred,
            CanvasPreviewMode preferredMode) {
        PreviewTarget previous = selectedPreviewTarget().orElse(null);
        List<PreviewTarget> available = availablePreviewChoices();
        PreviewTarget selected = FlutterDesignerPreviewPlatforms.preferredOrFirst(
                        available, preferred, preferredMode)
                .orElse(null);
        List<PreviewTarget> current = java.util.stream.IntStream
                .range(0, previewModes.getItemCount())
                .mapToObj(previewModes::getItemAt)
                .toList();

        updatingPreviewModes = true;
        try {
            if (!current.equals(available)) {
                previewModes.removeAllItems();
                available.forEach(previewModes::addItem);
            }
            previewModes.setSelectedItem(selected);
            previewModes.setEnabled(!available.isEmpty());
            updatePreviewModeAccessibility();
        } finally {
            updatingPreviewModes = false;
        }
        return !Objects.equals(previous, selected);
    }

    private void projectPlatformsChanged() {
        Runnable refresh = () -> {
            if (!platformListening) {
                return;
            }
            PreviewTarget retained = selectedPreviewTarget().orElse(null);
            boolean selectionChanged = refreshPreviewChoices(
                    retained, retained == null ? null : retained.mode());
            if (selectionChanged && currentCanvasDocument != null) {
                presentCurrentCanvas();
            }
        };
        if (java.awt.EventQueue.isDispatchThread()) {
            refresh.run();
        } else {
            java.awt.EventQueue.invokeLater(refresh);
        }
    }

    private void projectThemeFilesChanged() {
        if (projectThemeWatcher == null
                || projectThemeResolutionController == null) {
            return;
        }
        projectThemeResolutionController.invalidate();
        refreshCanvasAfterProjectThemeChange();
    }

    private void projectThemeResolutionChanged() {
        if (projectThemeResolutionController == null) {
            return;
        }
        refreshCanvasAfterProjectThemeChange();
    }

    private void refreshCanvasAfterProjectThemeChange() {
        if (currentCanvasDocument == null) {
            return;
        }
        // A verified theme revision is part of the presentation identity.
        // Clearing first also guarantees that a repaired theme is published
        // after an earlier fail-closed withdrawal.
        clearPresentedCanvasIdentity();
        presentCurrentCanvas();
    }

    private void updatePreviewModeAccessibility() {
        String description;
        if (projectPlatforms == null) {
            description = "Select an exact responsive viewport and Flutter adaptive "
                    + "platform target. No owning Flutter project platform filter is "
                    + "available.";
        } else if (previewModes.getItemCount() == 0) {
            description = "No preview is available because the Flutter project has no "
                    + "configured platform directory.";
        } else {
            String available = java.util.stream.IntStream
                    .range(0, previewModes.getItemCount())
                    .mapToObj(previewModes::getItemAt)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
            description = "Available previews match the configured Flutter project "
                    + "platforms: " + available + ". Android, iOS, macOS and Linux "
                    + "choices use Flutter adaptive appearance on the embedded Windows "
                    + "engine. Web uses a responsive browser-sized viewport on the same "
                    + "native engine; browser-only runtime behavior is not emulated.";
        }
        previewModes.setToolTipText(description);
        previewModes.getAccessibleContext().setAccessibleDescription(description);
    }

    private void rebuildWidgetTree(
            WidgetNode root,
            WidgetCatalog catalog,
            StableId retainedSelection,
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler) {
        synchronizingSelection = true;
        try {
            widgetNodes.clear();
            Node rootNode = buildWidgetNode(root, catalog, mutationHandler);
            explorerManager.setRootContext(rootNode);
            StableId selected = retainedSelection != null
                    && widgetNodes.containsKey(retainedSelection)
                    ? retainedSelection
                    : root.id();
            explorerManager.setSelectedNodes(new Node[]{widgetNodes.get(selected)});
        } catch (java.beans.PropertyVetoException failure) {
            throw new IllegalStateException(
                    "Could not update the Flutter Designer widget selection", failure);
        } finally {
            synchronizingSelection = false;
        }
        updateDeleteWidgetAction();
    }

    private Node buildWidgetNode(
            WidgetNode widget,
            WidgetCatalog catalog,
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler) {
        Children.Array children = new Children.Array();
        for (WidgetSlot slot : widget.slots().values()) {
            switch (slot) {
                case WidgetSlot.SingleSlot single -> single.child().ifPresent(
                        child -> children.add(new Node[]{buildWidgetNode(
                            child, catalog, mutationHandler)}));
                case WidgetSlot.ListSlot list -> {
                    for (WidgetNode child : list.children()) {
                        children.add(new Node[]{buildWidgetNode(
                            child, catalog, mutationHandler)});
                    }
                }
            }
        }
        var definition = catalog.find(widget.type()).orElseThrow(() ->
                new IllegalStateException(
                        "Validated Flutter widget type is absent from its catalog: "
                        + widget.type().value()));
        Node node = new FlutterWidgetPropertiesNode(
                children, widget, definition, mutationHandler);
        widgetNodes.put(widget.id(), node);
        return node;
    }

    private FlutterWidgetPropertiesNode.PropertyMutationHandler
            propertyMutationHandler(
                    FlutterDesignerMutationController.Snapshot candidate) {
        FlutterDesignerMutationController controllerForEdit = mutationController;
        if (!mutationUiEnabled.getAsBoolean()
                || controllerForEdit == null
                || candidate == null
                || candidate.status() != FlutterDesignerMutationController.Status.READY
                || candidate.token().isEmpty()) {
            return null;
        }
        FlutterDesignerMutationController.RevisionToken exactToken =
                candidate.token().orElseThrow();
        AtomicBoolean submitted = new AtomicBoolean();
        return command -> {
            PropertyMutationPresentation presentation =
                    propertyMutationPresentation(command);
            if (submitted.compareAndSet(false, true)) {
                submitPropertyMutation(
                        controllerForEdit,
                        exactToken,
                        command,
                        presentation);
            }
        };
    }

    private void submitPropertyMutation(
            FlutterDesignerMutationController controllerForEdit,
            FlutterDesignerMutationController.RevisionToken exactToken,
            DesignerCommand command,
            PropertyMutationPresentation presentation) {
        String target = modelName + " — widget " + presentation.widgetId()
                + ", property " + presentation.propertyName().value();
        submitDesignerMutation(
                controllerForEdit,
                exactToken,
                command,
                presentation.operation(),
                target);
    }

    private void submitDesignerMutation(
            FlutterDesignerMutationController controllerForEdit,
            FlutterDesignerMutationController.RevisionToken exactToken,
            DesignerCommand command,
            String operation,
            String target) {
        submitDesignerMutation(
                controllerForEdit,
                exactToken,
                command,
                operation,
                target,
                ignored -> {
                });
    }

    private void submitDesignerMutation(
            FlutterDesignerMutationController controllerForEdit,
            FlutterDesignerMutationController.RevisionToken exactToken,
            DesignerCommand command,
            String operation,
            String target,
            Consumer<FlutterDesignerMutationController.MutationResult>
                    activeViewCompletion) {
        Objects.requireNonNull(activeViewCompletion, "activeViewCompletion");
        long submittingViewEpoch = mutationViewEpoch;
        controllerForEdit.submit(
                        exactToken,
                        command,
                        target)
                .whenComplete((result, failure) -> {
                    FlutterDesignerMutationController.MutationResult completed = result;
                    if (failure != null) {
                        Throwable cause = failure instanceof CompletionException
                                && failure.getCause() != null
                                ? failure.getCause() : failure;
                        completed = FlutterDesignerMutationController.MutationResult.failed(
                                operation, target, failureReason(cause));
                    }
                    FlutterDesignerMutationController.MutationResult completedResult =
                            completed;
                    java.awt.EventQueue.invokeLater(() -> {
                        if (!mutationListening
                                || mutationController != controllerForEdit
                                || mutationViewEpoch != submittingViewEpoch) {
                            return;
                        }
                        activeViewCompletion.accept(completedResult);
                        if (completedResult.outcome()
                                == FlutterDesignerMutationController.Outcome.APPLIED) {
                            return;
                        }
                        if (completedResult.outcome()
                                == FlutterDesignerMutationController.Outcome.CANCELLED
                                && controllerForEdit.snapshot().status()
                                == FlutterDesignerMutationController.Status.CLOSED) {
                            return;
                        }
                        showMutationResult(completedResult);
                    });
                });
    }

    private static PropertyMutationPresentation propertyMutationPresentation(
            DesignerCommand command) {
        Objects.requireNonNull(command, "command");
        return switch (command) {
            case SetProperty set -> new PropertyMutationPresentation(
                    "Set Flutter property", set.widgetId(), set.propertyName());
            case ResetProperty reset -> new PropertyMutationPresentation(
                    "Reset Flutter property", reset.widgetId(), reset.propertyName());
            default -> throw new IllegalArgumentException(
                    "The Properties mutation handler accepts only SetProperty or ResetProperty; received "
                    + command.getClass().getSimpleName() + '.');
        };
    }

    private record PropertyMutationPresentation(
            String operation,
            StableId widgetId,
            PropertyName propertyName) {

        private PropertyMutationPresentation {
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(widgetId, "widgetId");
            Objects.requireNonNull(propertyName, "propertyName");
        }
    }

    private Optional<StableId> selectedWidgetId() {
        Node[] selected = explorerManager.getSelectedNodes();
        return selected.length == 1
                ? Optional.ofNullable(selected[0].getLookup().lookup(StableId.class))
                : Optional.empty();
    }

    private void deleteSelectedWidget() {
        DeleteWidgetAdmission admission = deleteWidgetAdmission().orElse(null);
        if (admission == null) {
            updateDeleteWidgetAction();
            return;
        }

        deleteWidgetSubmitting = true;
        updateDeleteWidgetAction();
        String target = modelName + " — widget " + admission.widgetId();
        submitDesignerMutation(
                admission.controller(),
                admission.token(),
                new RemoveWidget(admission.widgetId()),
                "Remove Flutter widget",
                target,
                result -> deleteWidgetCompleted(admission, result));
    }

    private void deleteSelectedWidgetFromCanvas(StableId expectedWidgetId) {
        Objects.requireNonNull(expectedWidgetId, "expectedWidgetId");
        if (selectedWidgetId().filter(expectedWidgetId::equals).isEmpty()) {
            return;
        }
        deleteSelectedWidget();
    }

    private Optional<DeleteWidgetAdmission> deleteWidgetAdmission() {
        FlutterDesignerMutationController controllerForDelete = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (deleteWidgetSubmitting
                || !mutationUiEnabled.getAsBoolean()
                || !mutationListening
                || controllerForDelete == null
                || candidate == null
                || candidate.status()
                != FlutterDesignerMutationController.Status.READY
                || candidate.token().isEmpty()
                || candidate.document().isEmpty()
                || candidate.catalog().isEmpty()
                || !currentCanvasMutationEnabled
                || candidate.document().orElseThrow() != currentCanvasDocument
                || candidate.catalog().orElseThrow() != currentCanvasCatalog) {
            return Optional.empty();
        }

        Node[] selected = explorerManager.getSelectedNodes();
        if (selected.length != 1) {
            return Optional.empty();
        }
        Node selectedNode = selected[0];
        StableId widgetId = selectedNode.getLookup().lookup(StableId.class);
        if (widgetId == null
                || widgetNodes.get(widgetId) != selectedNode
                || candidate.document().orElseThrow().root().id().equals(widgetId)) {
            return Optional.empty();
        }
        Node parentNode = selectedNode.getParentNode();
        StableId parentWidgetId = parentNode == null
                ? null : parentNode.getLookup().lookup(StableId.class);
        if (parentWidgetId == null || !widgetNodes.containsKey(parentWidgetId)) {
            return Optional.empty();
        }
        return Optional.of(new DeleteWidgetAdmission(
                controllerForDelete,
                candidate.token().orElseThrow(),
                widgetId,
                parentWidgetId));
    }

    private void deleteWidgetCompleted(
            DeleteWidgetAdmission admission,
            FlutterDesignerMutationController.MutationResult result) {
        deleteWidgetSubmitting = false;
        if (result.outcome() == FlutterDesignerMutationController.Outcome.APPLIED) {
            selectWidgetAfterMutation(admission.parentWidgetId());
        }
        updateDeleteWidgetAction();
    }

    private void selectWidgetAfterMutation(StableId widgetId) {
        Node node = widgetNodes.get(widgetId);
        if (node == null) {
            return;
        }
        try {
            explorerManager.setSelectedNodes(new Node[]{node});
        } catch (java.beans.PropertyVetoException failure) {
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    "Select parent Flutter widget failed.",
                    "Target: widget " + widgetId + ". Reason: "
                    + failureReason(failure)));
        }
    }

    private void updateDeleteWidgetAction() {
        deleteWidgetAction.setEnabled(deleteWidgetAdmission().isPresent());
    }

    private record DeleteWidgetAdmission(
            FlutterDesignerMutationController controller,
            FlutterDesignerMutationController.RevisionToken token,
            StableId widgetId,
            StableId parentWidgetId) {

        private DeleteWidgetAdmission {
            Objects.requireNonNull(controller, "controller");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(widgetId, "widgetId");
            Objects.requireNonNull(parentWidgetId, "parentWidgetId");
        }
    }

    private void clearWidgetTree() {
        synchronizingSelection = true;
        try {
            widgetNodes.clear();
            explorerManager.setSelectedNodes(new Node[0]);
            explorerManager.setRootContext(Node.EMPTY);
        } catch (java.beans.PropertyVetoException failure) {
            throw new IllegalStateException(
                    "Could not clear the Flutter Designer widget selection", failure);
        } finally {
            synchronizingSelection = false;
        }
        updateDeleteWidgetAction();
    }

    private void explorerSelectionChanged(PropertyChangeEvent event) {
        if (synchronizingSelection
                || !ExplorerManager.PROP_SELECTED_NODES.equals(event.getPropertyName())) {
            return;
        }
        updateDeleteWidgetAction();
        if (nativeCanvasSession != null) {
            selectedWidgetId().ifPresent(nativeCanvasSession::selectWidget);
        }
    }

    private void selectWidgetFromCanvas(StableId widgetId) {
        Node node = widgetNodes.get(widgetId);
        if (node == null) {
            return;
        }
        synchronizingSelection = true;
        try {
            explorerManager.setSelectedNodes(new Node[]{node});
        } catch (java.beans.PropertyVetoException failure) {
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    "Select Flutter widget failed.",
                    "Target: widget " + widgetId + ". Reason: "
                    + failureReason(failure)));
        } finally {
            synchronizingSelection = false;
        }
        updateDeleteWidgetAction();
    }

    private boolean isPaletteCatalogInsertDragEnabled() {
        if (!paletteCatalogInsertDndEnabled.getAsBoolean()
                || !mutationUiEnabled.getAsBoolean()
                || !designVisible
                || !paletteDragLifecycle.isInstalled()
                || nativeCanvasSession == null
                || !nativeCanvasSession.paletteCatalogInsertDropAvailable()
                || lastCanvasStatus == null
                || lastCanvasStatus.stage()
                != FlutterDesignerNativeCanvasStatus.Stage.RUNNING) {
            return false;
        }
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (mutationController == null
                || candidate == null
                || candidate.status() != FlutterDesignerMutationController.Status.READY
                || candidate.token().isEmpty()
                || candidate.document().isEmpty()
                || candidate.catalog().isEmpty()) {
            return false;
        }
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        return document == currentCanvasDocument
                && catalog == currentCanvasCatalog
                && document == presentedCanvasDocument
                && catalog == presentedCanvasCatalog
                && catalog.paletteDefinitions().stream()
                        .anyMatch(CanvasModelPayloadCodec::supports);
    }

    private Optional<WidgetTypeId> consumePaletteDropToken(String token) {
        return paletteDragLifecycle.consume(token)
                .filter(widgetType -> {
                    FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
                    if (candidate == null || candidate.catalog().isEmpty()) {
                        return false;
                    }
                    return candidate.catalog().orElseThrow().find(widgetType)
                            .filter(CanvasModelPayloadCodec::supports)
                            .isPresent();
                });
    }

    private void applyAdmittedPaletteDrop(
            FlutterDesignerNativeCanvasSession.AdmittedPaletteDrop admission) {
        Objects.requireNonNull(admission, "admission");
        CanvasRunnerRuntimeEvent.PaletteDrop drop = admission.drop();
        WidgetTypeId widgetType = admission.widgetType();
        if (!isPaletteCatalogInsertDragEnabled()) {
            return;
        }

        FlutterDesignerMutationController controllerForEdit = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (controllerForEdit == null
                || candidate == null
                || candidate.token().isEmpty()
                || candidate.document().isEmpty()
                || candidate.catalog().isEmpty()) {
            return;
        }
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        if (document != currentCanvasDocument
                || catalog != currentCanvasCatalog
                || document != presentedCanvasDocument
                || catalog != presentedCanvasCatalog
                || catalog.find(widgetType)
                        .filter(CanvasModelPayloadCodec::supports)
                        .isEmpty()) {
            return;
        }

        FlutterDesignerPaletteDropPlanner.Result planned = paletteDropPlanner.plan(
                document,
                catalog,
                widgetType,
                drop.parentWidgetId(),
                drop.slotName(),
                drop.insertionIndex(),
                StableId::random);
        if (!(planned instanceof FlutterDesignerPaletteDropPlanner.Accepted accepted)) {
            return;
        }

        String widgetDisplayName = catalog.find(widgetType).orElseThrow()
                .palette().displayName();
        String target = modelName + " — add " + widgetDisplayName + " to widget "
                + drop.parentWidgetId() + "." + drop.slotName().value()
                + " at index " + drop.insertionIndex();
        submitDesignerMutation(
                controllerForEdit,
                candidate.token().orElseThrow(),
                accepted.command(),
                "Add Flutter " + widgetDisplayName + " widget",
                target);
    }

    private void invalidatePaletteDragAuthority() {
        paletteDragLifecycle.revokeAll();
    }

    static FlutterDesignerDocumentState openingState(
            boolean firstView,
            FlutterDesignerDocumentState retainedState,
            String modelName) {
        if (firstView) {
            return new FlutterDesignerDocumentState.Loading(modelName);
        }
        return retainedState;
    }

    private void renderCurrent(FlutterDesignerDocumentState.Current current) {
        String rootType = current.decoded().document().root().type().value();
        String className = current.decoded().document().source().className();
        if (!current.contextIssues().isEmpty()) {
            FlutterDesignerDocumentState.ContextIssue issue = current.contextIssues().get(0);
            setPresentation(
                    "Flutter Designer model opened read-only.",
                    "Class " + className + "; root widget " + rootType + ".",
                    issue.code() + " at " + issue.path() + ": " + issue.message());
        } else if (current.sourceIntegrity().isEmpty()) {
            setPresentation(
                    "Flutter Designer model opened read-only.",
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    "No bounded on-disk Dart source-integrity snapshot is available.");
        } else if (!current.sourceIntegrity().orElseThrow().onDiskDeclaredMatch()) {
            DartSourceIntegrityResult integrity = current.sourceIntegrity().orElseThrow();
            DartSourceIntegrityDiagnostic diagnostic = integrity.primaryDiagnostic()
                    .orElseThrow(() -> new IllegalStateException(
                            "A non-matching integrity result must explain its status"));
            String status = switch (integrity.status()) {
                case UNSUPPORTED -> "Flutter Designer Dart source shape is unsupported.";
                case UNAVAILABLE -> "Flutter Designer Dart source is unavailable.";
                case CONFLICT -> "Flutter Designer Dart source conflict.";
                case ON_DISK_DECLARED_MATCH -> throw new IllegalStateException(
                        "A matching integrity result cannot contain diagnostics");
            };
            setPresentation(
                    status,
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    sourceDiagnosticDetail(integrity, diagnostic));
        } else if (!current.catalogDiagnostics().isEmpty()) {
            CatalogDiagnostic diagnostic = current.catalogDiagnostics().get(0);
            setPresentation(
                    "Flutter Designer model opened read-only.",
                    "Class " + className + "; root widget " + rootType + ".",
                    diagnostic.code() + " for " + diagnostic.subject()
                    + ": " + diagnostic.message());
        } else if (!current.validation().valid()) {
            ValidationIssue issue = current.validation().errors().get(0);
            setPresentation(
                    "Flutter Designer model opened read-only.",
                    "Class " + className + "; root widget " + rootType + ".",
                    issue.code() + " at " + issue.path() + ": " + issue.message());
        } else if (current.threeWayIntegrity().isEmpty()) {
            setPresentation(
                    "Flutter Designer three-way comparison is unavailable.",
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    "No bounded deterministic Dart-generation comparison is "
                    + "available for this loaded model.");
        } else if (!current.threeWayIntegrity().orElseThrow().onDiskThreeWayMatch()) {
            DartThreeWayIntegrityResult integrity =
                    current.threeWayIntegrity().orElseThrow();
            String status = switch (integrity.status()) {
                case UNSUPPORTED -> "Flutter Designer Dart generation is unsupported.";
                case UNAVAILABLE -> "Flutter Designer three-way comparison is unavailable.";
                case CONFLICT -> "Flutter Designer generated Dart conflict.";
                case ON_DISK_THREE_WAY_MATCH -> throw new IllegalStateException(
                        "A matching three-way result cannot enter the failure branch");
            };
            setPresentation(
                    status,
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    threeWayDiagnosticDetail(integrity));
        } else {
            setPresentation(
                    validatedDesignerStatus(),
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    validatedDesignerDetail());
        }
    }

    private String validatedDesignerStatus() {
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        return candidate != null
                && candidate.status() == FlutterDesignerMutationController.Status.APPLYING
                ? candidate.operation() + "..."
                : "Designer ready.";
    }

    private String validatedDesignerDetail() {
        StringBuilder detail = new StringBuilder(
                "The on-disk imports and build regions, the SHA-256 values recorded in ")
                .append(modelName)
                .append(", and the deterministic generated payloads agree. The validated ")
                .append("CORE_V1 model is published to the isolated native Flutter Canvas. ")
                .append("Viewport preview and widget-tree selection, the six-item Palette ")
                .append("and Properties are enabled. Supported properties on Column, Row, ")
                .append("Padding, Center and Text are writable when exact mutation admission ")
                .append("is ready; Scaffold properties remain read-only. ");
        if (isPaletteCatalogInsertDragEnabled()) {
            detail.append("Canvas-supported widgets can be dragged from the Palette into ")
                    .append("catalog-compatible empty single slots or terminal list slots; ")
                    .append("non-insertion drag-and-drop commands remain disabled.");
        } else {
            detail.append("Palette widget drag-and-drop is unavailable because exact mutation ")
                    .append("admission, the current rendered presentation, the owning-view AWT ")
                    .append("drag lifecycle, or the native Canvas drop capability is not ready; ")
                    .append("non-insertion drag-and-drop commands remain disabled.");
        }
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (candidate != null
                && candidate.status() != FlutterDesignerMutationController.Status.READY) {
            detail.append(' ')
                    .append(candidate.operation())
                    .append(" for ")
                    .append(candidate.target())
                    .append(": ")
                    .append(candidate.message());
        }
        return detail.toString();
    }

    private static String threeWayDiagnosticDetail(
            DartThreeWayIntegrityResult integrity) {
        DartThreeWayIntegrityDiagnostic primary = integrity.diagnostics().stream()
                .findFirst()
                .orElse(null);
        if (primary != null) {
            StringBuilder detail = new StringBuilder()
                    .append(primary.code())
                    .append(" at ")
                    .append(primary.path())
                    .append(primary.regionId()
                            .map(id -> "; region " + id)
                            .orElse(""))
                    .append(": ")
                    .append(primary.message());
            var additional = integrity.diagnostics().stream()
                    .filter(diagnostic -> diagnostic != primary)
                    .toList();
            if (!additional.isEmpty()) {
                detail.append(" Additional three-way diagnostics: ");
                int shown = Math.min(3, additional.size());
                for (int index = 0; index < shown; index++) {
                    if (index > 0) {
                        detail.append("; ");
                    }
                    DartThreeWayIntegrityDiagnostic diagnostic = additional.get(index);
                    detail.append(diagnostic.code())
                            .append(" at ")
                            .append(diagnostic.path())
                            .append(diagnostic.regionId()
                                    .map(id -> "; region " + id)
                                    .orElse(""));
                }
                if (additional.size() > shown) {
                    detail.append("; and ")
                            .append(additional.size() - shown)
                            .append(" more");
                }
                detail.append('.');
            }
            return detail.toString();
        }
        return integrity.generation().diagnostics().stream()
                .findFirst()
                .map(diagnostic -> diagnostic.code() + " at " + diagnostic.path()
                        + diagnostic.region()
                                .map(region -> "; region " + region.wireName())
                                .orElse("")
                        + ": " + diagnostic.message())
                .orElse("No complete bounded actual/declared/generated comparison "
                        + "is available for both managed Dart regions.");
    }

    private static String sourceDiagnosticDetail(
            DartSourceIntegrityResult integrity,
            DartSourceIntegrityDiagnostic primary) {
        StringBuilder detail = new StringBuilder()
                .append(diagnosticLocation(primary))
                .append(": ")
                .append(primary.message());
        var additional = integrity.diagnostics().stream()
                .filter(diagnostic -> diagnostic != primary)
                .toList();
        if (!additional.isEmpty()) {
            detail.append(" Additional source diagnostics: ");
            int shown = Math.min(3, additional.size());
            for (int index = 0; index < shown; index++) {
                if (index > 0) {
                    detail.append("; ");
                }
                detail.append(diagnosticLocation(additional.get(index)));
            }
            if (additional.size() > shown) {
                detail.append("; and ")
                        .append(additional.size() - shown)
                        .append(" more");
            }
            detail.append('.');
        }
        return detail.toString();
    }

    private static String diagnosticLocation(
            DartSourceIntegrityDiagnostic diagnostic) {
        return diagnostic.code() + " at " + diagnostic.path()
                + diagnostic.regionId().map(id -> "; region " + id).orElse("");
    }

    private void setPresentation(String status, String model, String detail) {
        statusLabel.setText(status);
        statusLabel.setToolTipText(model + " " + detail);
        modelLabel.setText(model);
        detailLabel.setText(detail);
        detailLabel.setToolTipText(detail);
        updateAccessiblePresentation(status, model, detail);
    }

    private void configureAccessibility() {
        visual.getAccessibleContext().setAccessibleName("Flutter Designer design view");
        statusPanel.getAccessibleContext().setAccessibleName("Flutter Designer model status");
        statusLabel.getAccessibleContext().setAccessibleName("Flutter Designer status");
        modelLabel.getAccessibleContext().setAccessibleName("Flutter Designer model and source");
        detailLabel.getAccessibleContext().setAccessibleName("Flutter Designer status details");
        progress.getAccessibleContext().setAccessibleName("Flutter Designer model loading progress");
        canvasStatusLabel.getAccessibleContext().setAccessibleName(
                "Native Flutter Canvas status");
        canvasProgress.getAccessibleContext().setAccessibleName(
                "Native Flutter Canvas preparation progress");
        canvasDetailsButton.getAccessibleContext().setAccessibleName(
                "Show Native Flutter Canvas details");
        canvasDetailsButton.getAccessibleContext().setAccessibleDescription(
                "Opens the complete scrollable and copyable Native Flutter Canvas status.");
        widgetTree.getAccessibleContext().setAccessibleName(
                "Flutter Designer widget tree");
        widgetTree.getAccessibleContext().setAccessibleDescription(
                "Selectable widget hierarchy for " + modelName
                + ". Supported properties on selected Column, Row, Padding, Center and "
                + "Text widgets are writable when exact mutation admission is ready; "
                + "Scaffold properties remain read-only. When the owning-view AWT drag "
                + "lifecycle and native Canvas drop capability are available, Canvas-supported "
                + "Palette widgets may be inserted into catalog-compatible empty single slots "
                + "or terminal list slots through the native Canvas.");
        previewModes.getAccessibleContext().setAccessibleName(
                "Flutter Canvas preview target");
        previewModes.getAccessibleContext().setAccessibleDescription(
                "Select an exact responsive viewport and adaptive platform target. "
                + "The concrete native Canvas engine remains Windows. Web is rendered "
                + "as a responsive browser-sized layout preview; browser-only runtime "
                + "behavior is not emulated.");
        canvasStatusLabel.getAccessibleContext().setAccessibleDescription(
                canvasStatusLabel.getText());
        canvasProgress.getAccessibleContext().setAccessibleDescription(
                canvasStatusLabel.getText());
        toolbar.getAccessibleContext().setAccessibleName("Flutter Designer toolbar");
        updateAccessiblePresentation(
                statusLabel.getText(), modelLabel.getText(), detailLabel.getText());
    }

    private void updateAccessiblePresentation(String status, String model, String detail) {
        String presentation = status + " " + model + " " + detail;
        visual.getAccessibleContext().setAccessibleDescription(presentation);
        statusPanel.getAccessibleContext().setAccessibleDescription(presentation);
        statusLabel.getAccessibleContext().setAccessibleDescription(status);
        modelLabel.getAccessibleContext().setAccessibleDescription(model);
        detailLabel.getAccessibleContext().setAccessibleDescription(detail);
        progress.getAccessibleContext().setAccessibleDescription(detail);
        toolbar.getAccessibleContext().setAccessibleDescription(
                "Designer actions for " + modelName + ".");
    }

    void renderNativeCanvasStatus(FlutterDesignerNativeCanvasStatus state) {
        lastCanvasStatus = Objects.requireNonNull(state, "state");
        // Every status transition can replace or re-layout the native surface.
        // Tokens issued for the preceding pixels must never survive it.
        invalidatePaletteDragAuthority();
        if (state.stage() == FlutterDesignerNativeCanvasStatus.Stage.FAILED) {
            // A rejected asynchronous present must remain retryable on the
            // next controller publication, even if its model identity is unchanged.
            clearPresentedCanvasIdentity();
        }
        canvasStatusLabel.setText(state.summary());
        canvasStatusLabel.setToolTipText(state.detail());
        canvasStatusLabel.getAccessibleContext().setAccessibleDescription(
                state.summary() + " " + state.detail());
        boolean detailsAvailable = state.stage()
                == FlutterDesignerNativeCanvasStatus.Stage.FAILED
                || state.stage() == FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE;
        canvasDetailsButton.setVisible(detailsAvailable);
        canvasDetailsButton.setEnabled(detailsAvailable);
        canvasDetailsButton.setToolTipText(detailsAvailable
                ? "Show the complete Native Flutter Canvas status."
                : null);
        canvasDetailsButton.getAccessibleContext().setAccessibleDescription(
                detailsAvailable
                        ? "Open complete details for " + state.summary()
                        : "No additional Native Flutter Canvas failure details are available.");
        canvasProgress.setVisible(state.busy());
        canvasProgress.getAccessibleContext().setAccessibleDescription(state.detail());
    }

    private void showNativeCanvasDetails() {
        FlutterDesignerNativeCanvasStatus state = lastCanvasStatus;
        if (state == null || (state.stage() != FlutterDesignerNativeCanvasStatus.Stage.FAILED
                && state.stage() != FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE)) {
            return;
        }
        JScrollPane details = createNativeCanvasDetailsComponent(
                state, modelName, sourceName);
        NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                details,
                state.stage() == FlutterDesignerNativeCanvasStatus.Stage.FAILED
                        ? NotifyDescriptor.ERROR_MESSAGE
                        : NotifyDescriptor.WARNING_MESSAGE);
        descriptor.setTitle("Native Flutter Canvas Details");
        DialogDisplayer.getDefault().notify(descriptor);
    }

    static JScrollPane createNativeCanvasDetailsComponent(
            FlutterDesignerNativeCanvasStatus state,
            String modelName,
            String sourceName) {
        Objects.requireNonNull(state, "state");
        String text = state.summary() + "\n\n"
                + "Model: " + Objects.requireNonNull(modelName, "modelName") + "\n"
                + "Source: " + Objects.requireNonNull(sourceName, "sourceName") + "\n\n"
                + state.detail();
        JTextArea area = new JTextArea(text, 14, 80);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.getAccessibleContext().setAccessibleName(
                "Native Flutter Canvas full status details");
        area.getAccessibleContext().setAccessibleDescription(
                "Read-only selectable text containing the complete Canvas operation, "
                + "target and reason. Use Control+A and Control+C to copy it.");

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(720, 320));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getAccessibleContext().setAccessibleName(
                "Native Flutter Canvas details");
        scroll.getAccessibleContext().setAccessibleDescription(
                "Scrollable complete status for " + state.summary());
        return scroll;
    }

    private static void showMutationResult(
            FlutterDesignerMutationController.MutationResult result) {
        JScrollPane details = createMutationResultDetailsComponent(result);
        int messageType = result.outcome()
                == FlutterDesignerMutationController.Outcome.FAILED
                ? NotifyDescriptor.ERROR_MESSAGE
                : NotifyDescriptor.WARNING_MESSAGE;
        NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                details, messageType);
        descriptor.setTitle("Flutter Designer Change Not Applied");
        // The caller has already returned to the EDT and fenced this result
        // against componentClosed().  A second notifyLater() hop would let the
        // Design view close between the fence and the actual dialog display.
        DialogDisplayer.getDefault().notify(descriptor);
    }

    static JScrollPane createMutationResultDetailsComponent(
            FlutterDesignerMutationController.MutationResult result) {
        Objects.requireNonNull(result, "result");
        String text = "Operation: " + result.operation() + "\n"
                + "Target: " + result.target() + "\n"
                + "Reason: " + result.reason();
        JTextArea area = new JTextArea(text, 10, 72);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.getAccessibleContext().setAccessibleName(
                "Flutter Designer change details");
        area.getAccessibleContext().setAccessibleDescription(
                "Read-only selectable text containing the complete operation, target "
                + "and reason. Use Control+A and Control+C to copy it.");

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(640, 240));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getAccessibleContext().setAccessibleName(
                "Flutter Designer change result");
        scroll.getAccessibleContext().setAccessibleDescription(
                "Scrollable complete result for " + result.operation()
                + " on " + result.target() + ".");
        return scroll;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .startsWith("windows");
    }

    private static String failureReason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private void clearPresentedCanvasIdentity() {
        presentedCanvasDocument = null;
        presentedCanvasCatalog = null;
        presentedCanvasTarget = null;
        presentedCanvasTheme = null;
    }

    private static final class ExplorerPanel extends JPanel
            implements ExplorerManager.Provider {
        private final ExplorerManager manager;

        private ExplorerPanel(ExplorerManager manager) {
            super(new BorderLayout());
            this.manager = Objects.requireNonNull(manager, "manager");
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return manager;
        }
    }

}

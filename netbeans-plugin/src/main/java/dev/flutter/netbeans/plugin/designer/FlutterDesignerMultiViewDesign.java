package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCapabilityCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCapability;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.CanvasResolvedTheme;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdCodecDiagnostic;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.ClearSlotChildren;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.RemoveWidget;
import dev.flutter.netbeans.designer.command.ReplaceSlotChild;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.DesignerThemeMode;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerPreviewPlatforms.PreviewTarget;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPalette;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDragLifecycle;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDragRegistry;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDropPlanner;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteTreeDropAdapter;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetSlotEditorContext;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetSlotMutation;
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
    private static final Logger LOGGER = Logger.getLogger(
            FlutterDesignerMultiViewDesign.class.getName());
    static final String DELETE_WIDGET_ACTION_KEY = "delete";
    private static final WidgetTypeId TEXT_WIDGET_TYPE =
            new WidgetTypeId("flutter.widgets.Text");
    private static final PropertyName TEXT_DATA_PROPERTY =
            new PropertyName("data");
    private static final int SWING_FOCUS_REPAIR_DELAY_MILLIS = 50;
    static final int MAX_SWING_FOCUS_REPAIR_ATTEMPTS = 8;
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
    private final JLabel canvasInteractionStatusLabel;
    private final JPanel canvasPanel;
    private final JButton canvasRetryButton;
    private final JButton canvasDetailsButton;
    private final JProgressBar progress;
    private final JProgressBar canvasProgress;
    private final ExplorerManager explorerManager;
    private final FlutterDesignerPropertiesTabController propertiesTabController;
    private final Action deleteWidgetAction;
    private final FlutterDesignerPaletteDragRegistry paletteDragRegistry;
    private final FlutterDesignerPaletteDragLifecycle paletteDragLifecycle;
    private final FlutterDesignerPaletteDropPlanner paletteDropPlanner;
    private final FlutterDesignerPaletteTreeDropAdapter paletteTreeDropAdapter;
    private final FlutterDesignerWidgetMovePlanner widgetMovePlanner =
            new FlutterDesignerWidgetMovePlanner();
    private FlutterDesignerWidgetTreeDropSupport<
            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop>
                    widgetTreeDropSupport;
    private final PaletteController paletteController;
    private final BeanTreeView widgetTree;
    private final JComboBox<PreviewTarget> previewModes;
    private final FlutterDesignerViewportControls viewportControls;
    private final Timer viewportCommandTimer;
    private final FlutterProjectPlatformProvider projectPlatforms;
    private final FlutterDesignerProjectThemeResolver projectThemeResolver =
            new FlutterDesignerProjectThemeResolver();
    private final ChangeListener projectPlatformListener =
            event -> projectPlatformsChanged();
    private final FlutterDesignerCanvasBackendSelector canvasBackendSelector;
    private final FlutterDesignerCanvasSessionFactory canvasSessionFactory;
    private record CanvasCoordinatorBinding(
            FlutterDesignerCanvasOwnerCoordinator coordinator,
            long generation) {
    }

    private FlutterDesignerCanvasOwnerCoordinator canvasOwnerCoordinator;
    private long canvasOwnerCoordinatorGeneration;
    private volatile CanvasCoordinatorBinding publishedCanvasOwnerCoordinator;
    private FlutterDesignerCanvasBackendSelector.Backend desiredCanvasBackend =
            FlutterDesignerCanvasBackendSelector.Backend.NATIVE;
    private FlutterDesignerCanvasOwner canvasOwner;
    private MultiViewElementCallback multiViewCallback;
    private Runnable legacyEditorShellCloseRetryRequest;
    private LongConsumer editorShellCloseRetryRequest;
    private BiConsumer<Long, Throwable> editorShellCloseFailureRequest;
    private long editorShellCloseAttemptId = -1;
    private final PropertyChangeListener permanentFocusOwnerListener =
            this::permanentFocusOwnerChanged;
    private final AWTEventListener swingInputFocusListener =
            this::swingInputEventDispatched;
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
    private boolean moveWidgetSubmitting;
    private boolean slotWidgetSubmitting;
    private boolean inlineTextEditSubmitting;
    private StableId pendingWidgetSelection;
    private volatile boolean componentLifecycleOpen;
    private long mutationViewEpoch;
    private boolean platformListening;
    private boolean designVisible;
    private boolean designActivated;
    private CanvasCloseGateState canvasCloseGateState = CanvasCloseGateState.OPEN;
    private boolean canvasCloseDiscardAuthorized;
    private CompletionStage<Void> canvasCloseCompletion;
    private FlutterDesignerCanvasOwnerCoordinator canvasCloseCoordinator;
    private long canvasCloseCoordinatorGeneration = -1;
    private boolean canvasCloseRecoveryRequested;
    private boolean canvasCloseRecoveryScheduled;
    private boolean canvasCloseRetryScheduled;
    private KeyboardFocusManager permanentFocusOwnerManager;
    private Toolkit swingInputFocusToolkit;
    private long swingFocusRepairEpoch;
    private long nativeCanvasFocusBootstrapEpoch;
    private boolean swingFocusClaimedForActivation;
    private JComponent retainedSwingFocusTarget;
    private SwingFocusRepairNativeReleaseMarker swingFocusRepairNativeRelease;
    private SwingFocusRepairReleasedJvmAuthority swingFocusRepairReleasedJvmAuthority;
    private boolean canvasFrameRendered;
    private boolean interactionBarrierPending;
    private long interactionBarrierTransitionEpoch;
    private final SwingFocusRepairRetryFence swingFocusRepairRetryFence =
            new SwingFocusRepairRetryFence(MAX_SWING_FOCUS_REPAIR_ATTEMPTS);
    private Timer swingFocusRepairTimer;
    private CanvasViewportPresentation pendingViewportPresentation;

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
        this(
                context,
                mutationUiEnabled,
                paletteCatalogInsertDndEnabled,
                FlutterDesignerCanvasBackendSelector.production(),
                FlutterDesignerCanvasSessionFactory.production());
    }

    FlutterDesignerMultiViewDesign(
            Lookup context,
            BooleanSupplier mutationUiEnabled,
            BooleanSupplier paletteCatalogInsertDndEnabled,
            FlutterDesignerCanvasBackendSelector canvasBackendSelector) {
        this(
                context,
                mutationUiEnabled,
                paletteCatalogInsertDndEnabled,
                canvasBackendSelector,
                FlutterDesignerCanvasSessionFactory.production());
    }

    FlutterDesignerMultiViewDesign(
            Lookup context,
            BooleanSupplier mutationUiEnabled,
            BooleanSupplier paletteCatalogInsertDndEnabled,
            FlutterDesignerCanvasBackendSelector canvasBackendSelector,
            FlutterDesignerCanvasSessionFactory canvasSessionFactory) {
        this.context = context;
        this.mutationUiEnabled = Objects.requireNonNull(
                mutationUiEnabled, "mutationUiEnabled");
        this.paletteCatalogInsertDndEnabled = Objects.requireNonNull(
                paletteCatalogInsertDndEnabled, "paletteCatalogInsertDndEnabled");
        this.canvasBackendSelector = Objects.requireNonNull(
                canvasBackendSelector, "canvasBackendSelector");
        this.canvasSessionFactory = Objects.requireNonNull(
                canvasSessionFactory, "canvasSessionFactory");
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
        propertiesTabController = new FlutterDesignerPropertiesTabController();
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
        paletteTreeDropAdapter = new FlutterDesignerPaletteTreeDropAdapter(
                paletteDragLifecycle);
        paletteController = FlutterDesignerPalette.create(
                BuiltInWidgetCatalog.getDefault(),
                definition -> BuiltInWidgetCapabilityCatalog.supports(
                        definition, WidgetCapability.CREATE),
                paletteDragRegistry,
                this::isPaletteCatalogInsertDragAuthorityEnabled,
                definition -> BuiltInWidgetCapabilityCatalog.supports(
                        definition, WidgetCapability.DND),
                this::authorizeNativeCanvasPaletteDragSource);
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
        canvasInteractionStatusLabel = centeredLabel("");
        canvasInteractionStatusLabel.setVisible(false);
        canvasInteractionStatusLabel.getAccessibleContext().setAccessibleName(
                "Native Canvas input synchronization status");
        canvasRetryButton = new JButton();
        Mnemonics.setLocalizedText(canvasRetryButton, "&Retry");
        canvasRetryButton.setVisible(false);
        canvasRetryButton.setEnabled(false);
        canvasRetryButton.addActionListener(event -> retryNativeCanvas());
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
        canvasStatusRow.add(canvasInteractionStatusLabel);
        canvasStatusRow.add(canvasProgress);
        canvasStatusRow.add(canvasRetryButton);
        canvasStatusRow.add(canvasDetailsButton);
        JPanel hiddenStatusDetails = new JPanel();
        hiddenStatusDetails.add(modelLabel);
        hiddenStatusDetails.add(detailLabel);
        hiddenStatusDetails.setVisible(false);
        statusPanel.add(modelStatusRow, BorderLayout.WEST);
        statusPanel.add(hiddenStatusDetails, BorderLayout.CENTER);
        statusPanel.add(canvasStatusRow, BorderLayout.EAST);
        viewportControls = new FlutterDesignerViewportControls(
                this::viewportPresentationChanged);
        viewportCommandTimer = new Timer(24, event -> flushViewportPresentation());
        viewportCommandTimer.setRepeats(false);
        // Enabled only after the current bundled runner confirms the negotiated
        // viewport capability with exact metrics for this presentation.
        viewportControls.setControlsEnabled(false);
        canvasPanel = new JPanel(new BorderLayout());
        // Status copy is deliberately complete for accessibility and diagnostics,
        // but a long JLabel must not become the right side's split-pane minimum.
        // Otherwise JSplitPane clamps its minimum and maximum divider locations
        // to the same value as soon as the validated-model detail is rendered.
        canvasPanel.setMinimumSize(new Dimension(320, 120));
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
        // A lightweight Swing popup is painted below the embedded native
        // Flutter child HWND. Keep this policy local to the Designer control;
        // changing PopupFactory globally would affect unrelated NetBeans UI.
        previewModes.setLightWeightPopupEnabled(false);
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
        toolbar.addSeparator();
        toolbar.add(viewportControls.toolbarComponent());
        previewModes.addActionListener(event -> previewModeChanged());
        configureAccessibility();
        updatePreviewModeAccessibility();

        // The coordinator is the sole authority allowed to replace a Canvas
        // component. It never creates a successor until the previous owner has
        // completed peer-safe retirement.
        installNewCanvasOwnerCoordinator();
        requestCanvasBackend(FlutterDesignerCanvasBackendSelector.Backend.NATIVE);
    }

    private void installNewCanvasOwnerCoordinator() {
        if (canvasOwnerCoordinatorGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Flutter Designer Canvas coordinator generation is exhausted");
        }
        long generation = ++canvasOwnerCoordinatorGeneration;
        FlutterDesignerCanvasOwnerCoordinator coordinator =
                new FlutterDesignerCanvasOwnerCoordinator(
                        (backend, epoch) -> createCanvasOwner(
                                generation, backend, epoch),
                        new CanvasOwnerObserver(generation),
                        FlutterDesignerMultiViewDesign::dispatchOnEdt);
        canvasOwnerCoordinator = coordinator;
        // Canvas runner callbacks can arrive outside the EDT. Publish the
        // coordinator and its generation as one immutable volatile identity;
        // two independently published fields could otherwise be observed as
        // a torn old/new pair and permanently reject a valid generation.
        publishedCanvasOwnerCoordinator = new CanvasCoordinatorBinding(
                coordinator, generation);
    }

    private static void dispatchOnEdt(Runnable command) {
        Objects.requireNonNull(command, "command");
        if (java.awt.EventQueue.isDispatchThread()) {
            command.run();
        } else {
            java.awt.EventQueue.invokeLater(command);
        }
    }

    private CompletionStage<FlutterDesignerCanvasOwner> createCanvasOwner(
            long coordinatorGeneration,
            FlutterDesignerCanvasBackendSelector.Backend backend,
            FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
        FlutterDesignerCanvasOwner created = null;
        try {
            created = Objects.requireNonNull(
                    canvasSessionFactory.create(
                            backend,
                            new FlutterDesignerCanvasSessionFactory.Callbacks(
                                    status -> admitCanvasCallback(
                                            coordinatorGeneration, epoch,
                                            () -> renderNativeCanvasStatus(status)),
                                    selection -> admitCanvasCallback(
                                            coordinatorGeneration, epoch,
                                            () -> selectWidgetFromCanvas(selection)),
                                    token -> isActiveCanvasEpoch(
                                            coordinatorGeneration, epoch)
                                            ? consumePaletteDropToken(token)
                                            : Optional.empty(),
                                    drop -> admitCanvasCallback(
                                            coordinatorGeneration, epoch,
                                            () -> applyAdmittedPaletteDrop(drop)),
                                    deletion -> admitCanvasCallback(
                                            coordinatorGeneration, epoch,
                                            () -> deleteSelectedWidgetFromCanvas(
                                                    deletion.widgetId())))),
                    "Canvas session factory returned no owner");
            FlutterDesignerCanvasOwner configured = created;
            configured.setTextEditCommitListener(commit -> admitCanvasCallback(
                    coordinatorGeneration, epoch,
                    () -> applyInlineTextEditCommit(commit)));
            configured.setViewportMetricsListener(metrics -> admitCanvasCallback(
                    coordinatorGeneration, epoch,
                    () -> renderViewportMetrics(metrics)));
            configured.setInteractionListener(() -> admitCanvasCallback(
                    coordinatorGeneration, epoch,
                    this::nativeCanvasInteractionFromRunner));
            configured.setInteractionBarrierListener(state -> admitCanvasCallback(
                    coordinatorGeneration, epoch,
                    () -> renderInteractionBarrierState(state)));
            return CompletableFuture.completedFuture(configured);
        } catch (FlutterDesignerCanvasSessionFactory.CreationException
                | RuntimeException | LinkageError failure) {
            if (created == null) {
                return CompletableFuture.failedFuture(failure);
            }
            // Ownership already transferred before listener configuration
            // failed. Hand the exact owner back to the coordinator so it can
            // retain a poisoned native peer and expose bounded cleanup Retry;
            // a factory-local best-effort close would lose that only handle.
            return CompletableFuture.failedFuture(
                    new FlutterDesignerCanvasOwnerCoordinator
                            .RetainedOwnerCreationFailure(created, failure));
        }
    }

    private boolean isActiveCanvasEpoch(
            long coordinatorGeneration,
            FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
        CanvasCoordinatorBinding binding = publishedCanvasOwnerCoordinator;
        return binding != null
                && binding.generation() == coordinatorGeneration
                && binding.coordinator().isActiveEpoch(epoch);
    }

    private void admitCanvasCallback(
            long coordinatorGeneration,
            FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch,
            Runnable callback) {
        CanvasCoordinatorBinding binding = publishedCanvasOwnerCoordinator;
        if (binding == null
                || binding.generation() != coordinatorGeneration) {
            return;
        }
        FlutterDesignerCanvasOwnerCoordinator coordinator = binding.coordinator();
        coordinator.admitCallback(epoch, () -> {
            if (publishedCanvasOwnerCoordinator == binding) {
                callback.run();
            }
        });
    }

    private static Throwable unwrapCompletionFailure(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private final class CanvasOwnerObserver
            implements FlutterDesignerCanvasOwnerCoordinator.Observer {
        private final long coordinatorGeneration;

        CanvasOwnerObserver(long coordinatorGeneration) {
            this.coordinatorGeneration = coordinatorGeneration;
        }

        private boolean isCurrent() {
            CanvasCoordinatorBinding binding = publishedCanvasOwnerCoordinator;
            return binding != null
                    && binding.generation() == coordinatorGeneration;
        }

        @Override
        public void ownerCreationStarted(
                FlutterDesignerCanvasBackendSelector.Backend backend,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
            if (!isCurrent()) {
                return;
            }
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                    canvasTitle(backend) + " is starting...",
                    "Target: " + canvasTarget(backend)
                    + ". Creating the requested backend for owner epoch "
                    + epoch + "."));
        }

        @Override
        public void ownerActivated(
                FlutterDesignerCanvasOwner owner,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
            if (!isCurrent()) {
                return;
            }
            canvasOwner = owner;
            canvasPanel.add(owner.component(), BorderLayout.CENTER);
            canvasPanel.revalidate();
            canvasPanel.repaint();
            if (componentLifecycleOpen && designVisible) {
                installPermanentFocusOwnerListener();
                installSwingInputFocusListener();
            }
            owner.setViewportPresentation(
                    viewportControls.currentPresentation());
            if (designVisible) {
                owner.show();
            } else {
                owner.hide();
            }
            clearPresentedCanvasIdentity();
            if (currentCanvasDocument != null) {
                presentCurrentCanvas();
            }
            selectedWidgetId().ifPresent(owner::selectWidget);
            renderInteractionBarrierState(owner.interactionBarrierState());
            if (designActivated && !swingFocusClaimedForActivation) {
                owner.requestFocus();
            }
        }

        @Override
        public void ownerRetirementStarted(
                FlutterDesignerCanvasOwner owner,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
            if (!isCurrent()) {
                return;
            }
            if (canvasOwner == owner) {
                canvasOwner = null;
            }
            nativeCanvasFocusBootstrapEpoch++;
            clearSwingFocusClaim();
            invalidatePaletteDragAuthority();
            viewportCommandTimer.stop();
            pendingViewportPresentation = null;
            clearPresentedCanvasIdentity();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                    "Releasing " + canvasTitle(owner.backend()) + "...",
                    "Target: " + canvasTarget(owner.backend())
                    + ". The component remains attached until owner epoch "
                    + epoch + " confirms peer-safe retirement."));
        }

        @Override
        public void ownerRetired(
                FlutterDesignerCanvasOwner owner,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch) {
            if (!isCurrent()) {
                return;
            }
            canvasPanel.remove(owner.component());
            canvasPanel.revalidate();
            canvasPanel.repaint();
        }

        @Override
        public void ownerRetirementFailed(
                FlutterDesignerCanvasOwner owner,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch,
                Throwable failure) {
            if (!isCurrent()) {
                return;
            }
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    canvasTitle(owner.backend()) + " release failed.",
                     "Target: " + canvasTarget(owner.backend())
                     + ". Reason: " + failureReason(failure)
                     + ". The owner remains retained and no replacement is "
                     + "installed; use Retry before switching backend or "
                     + "closing the view."));
            notifyEditorShellCloseFailure(failure);
        }

        @Override
        public void ownerCreationFailed(
                FlutterDesignerCanvasBackendSelector.Backend backend,
                FlutterDesignerCanvasOwnerCoordinator.CallbackEpoch epoch,
                Throwable failure) {
            if (!isCurrent()) {
                return;
            }
            String target = failure
                    instanceof FlutterDesignerCanvasSessionFactory.CreationException
                            creationFailure
                    ? creationFailure.target()
                    : canvasTarget(backend);
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    canvasTitle(backend) + " is unavailable.",
                    "Target: " + target + ". Reason: "
                    + failureReason(failure) + "."));
        }
    }

    private static String canvasTitle(
            FlutterDesignerCanvasBackendSelector.Backend backend) {
        return backend == FlutterDesignerCanvasBackendSelector.Backend.EXACT_WEB
                ? "Exact Flutter Web Canvas"
                : "Native Flutter Canvas";
    }

    private static String canvasTarget(
            FlutterDesignerCanvasBackendSelector.Backend backend) {
        return backend == FlutterDesignerCanvasBackendSelector.Backend.EXACT_WEB
                ? "exact Flutter Web Canvas"
                : "provider-owned native Flutter surface";
    }

    private static JLabel centeredLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return label;
    }

    private static BeanTreeView createWidgetTree() {
        if (java.awt.EventQueue.isDispatchThread()) {
            return new FlutterDesignerWidgetTreeView();
        }
        java.util.concurrent.atomic.AtomicReference<BeanTreeView> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        try {
            java.awt.EventQueue.invokeAndWait(() -> {
                try {
                    result.set(new FlutterDesignerWidgetTreeView());
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
        componentLifecycleOpen = true;
        if (canvasOwner != null && designVisible) {
            installPermanentFocusOwnerListener();
            installSwingInputFocusListener();
        }
        installWidgetTreeDropSupport();
        deleteWidgetSubmitting = false;
        moveWidgetSubmitting = false;
        slotWidgetSubmitting = false;
        inlineTextEditSubmitting = false;
        pendingWidgetSelection = null;
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
        componentLifecycleOpen = false;
        canvasCloseRecoveryRequested = false;
        canvasCloseRecoveryScheduled = false;
        designActivated = false;
        nativeCanvasFocusBootstrapEpoch++;
        clearSwingFocusClaim();
        uninstallSwingInputFocusListener();
        uninstallPermanentFocusOwnerListener();
        uninstallWidgetTreeDropSupport();
        designVisible = false;
        viewportCommandTimer.stop();
        pendingViewportPresentation = null;
        deleteWidgetSubmitting = false;
        moveWidgetSubmitting = false;
        inlineTextEditSubmitting = false;
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
        slotWidgetSubmitting = false;
        pendingWidgetSelection = null;
        propertiesTabController.reset();
        clearPresentedCanvasIdentity();
        clearWidgetTree();
        canvasOwnerCoordinator.close();
        if (controller != null && listening) {
            listening = false;
            controller.removePropertyChangeListener(this);
            controller.viewClosed();
        }
    }

    private void installWidgetTreeDropSupport() {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(this::installWidgetTreeDropSupport);
            return;
        }
        if (!componentLifecycleOpen || widgetTreeDropSupport != null) {
            return;
        }
        widgetTreeDropSupport = new FlutterDesignerWidgetTreeDropSupport<>(
                (FlutterDesignerWidgetTreeView) widgetTree,
                new WidgetTreePaletteDropAdmission(),
                new WidgetTreeMoveAdmission(),
                this::renderWidgetTreeDropFeedback);
    }

    private void uninstallWidgetTreeDropSupport() {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(this::uninstallWidgetTreeDropSupport);
            return;
        }
        FlutterDesignerWidgetTreeDropSupport<
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop> support =
                        widgetTreeDropSupport;
        widgetTreeDropSupport = null;
        if (support != null) {
            support.close();
        }
    }

    @Override
    public void componentShowing() {
        designVisible = true;
        invalidatePaletteDragAuthority();
        if (canvasOwner != null) {
            installPermanentFocusOwnerListener();
            installSwingInputFocusListener();
            canvasOwner.show();
        }
    }

    @Override
    public void componentHidden() {
        designVisible = false;
        nativeCanvasFocusBootstrapEpoch++;
        clearSwingFocusClaim();
        uninstallSwingInputFocusListener();
        uninstallPermanentFocusOwnerListener();
        invalidatePaletteDragAuthority();
        if (canvasOwner != null) {
            canvasOwner.hide();
        }
    }

    @Override
    public void componentActivated() {
        designActivated = true;
        FlutterDesignerAuxiliaryWindows.openDefaultOnce();
        if (canvasOwner != null && !swingFocusClaimedForActivation) {
            canvasOwner.requestFocus();
        }
    }

    @Override
    public void componentDeactivated() {
        designActivated = false;
        nativeCanvasFocusBootstrapEpoch++;
        clearSwingFocusClaim();
        if (canvasOwner != null) {
            canvasOwner.clearFocusRequest();
        }
    }

    private void installPermanentFocusOwnerListener() {
        if (permanentFocusOwnerManager != null) {
            return;
        }
        KeyboardFocusManager manager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addPropertyChangeListener(
                "permanentFocusOwner",
                permanentFocusOwnerListener);
        permanentFocusOwnerManager = manager;
    }

    private void uninstallPermanentFocusOwnerListener() {
        KeyboardFocusManager manager = permanentFocusOwnerManager;
        permanentFocusOwnerManager = null;
        if (manager != null) {
            manager.removePropertyChangeListener(
                        "permanentFocusOwner",
                        permanentFocusOwnerListener);
        }
    }

    private void installSwingInputFocusListener() {
        if (swingInputFocusToolkit != null) {
            return;
        }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.addAWTEventListener(
                swingInputFocusListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        swingInputFocusToolkit = toolkit;
    }

    private void uninstallSwingInputFocusListener() {
        swingFocusRepairEpoch++;
        Toolkit toolkit = swingInputFocusToolkit;
        swingInputFocusToolkit = null;
        if (toolkit != null) {
            toolkit.removeAWTEventListener(swingInputFocusListener);
        }
    }

    private void permanentFocusOwnerChanged(PropertyChangeEvent event) {
        Object candidate = event.getNewValue();
        if (!(candidate instanceof Component focusOwner)) {
            return;
        }
        KeyboardFocusManager manager = permanentFocusOwnerManager;
        if (manager == null || event.getSource() != manager) {
            return;
        }
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(
                    () -> {
                        if (manager == permanentFocusOwnerManager
                                && manager.getPermanentFocusOwner() == focusOwner) {
                            permanentFocusOwnerChangedOnEdt(focusOwner);
                        }
                    });
            return;
        }
        permanentFocusOwnerChangedOnEdt(focusOwner);
    }

    private void permanentFocusOwnerChangedOnEdt(Component focusOwner) {
        SwingFocusRepairNativeReleaseMarker releaseMarker =
                swingFocusRepairNativeRelease;
        if (canPreserveSwingClaimForIntermediateNativeRelease(
                releaseMarker,
                focusOwner,
                canvasFocusSurface(),
                swingFocusRepairEpoch,
                nativeCanvasFocusBootstrapEpoch,
                swingFocusClaimedForActivation,
                retainedSwingFocusTarget)) {
            return;
        }
        if (releaseMarker != null) {
            swingFocusRepairNativeRelease = null;
        }
        if (canvasHostComponent() != null
                && sameOrDescendant(focusOwner, canvasHostComponent())) {
            clearSwingFocusClaim();
            return;
        }
        if (swingFocusClaimedForActivation
                && !insideAnyRoot(focusOwner, visual, toolbar)) {
            clearSwingFocusClaim();
            if (canvasOwner != null) {
                canvasOwner.clearFocusRequest();
            }
            return;
        }
        cancelDeferredCanvasFocusFor(focusOwner);
    }

    private void cancelDeferredCanvasFocusFor(Component focusOwner) {
        if (canvasOwner != null
                && swingFocusClaimedForActivation
                && isDesignInteractionActive()
                && shouldCancelDeferredCanvasFocus(
                        focusOwner,
                        visual,
                        toolbar,
                        canvasHostComponent())) {
            retainedSwingFocusTarget = nearestFocusableSwingComponent(
                    focusOwner, visual, toolbar);
            canvasOwner.clearFocusRequest();
        }
    }

    private void swingInputEventDispatched(AWTEvent event) {
        Component source;
        boolean physicalPointerDown;
        Point physicalPointerScreenPoint;
        if (event instanceof MouseEvent mouseEvent
                && mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
                && mouseEvent.getSource() instanceof Component mouseSource) {
            source = mouseSource;
            physicalPointerDown = true;
            physicalPointerScreenPoint = new Point(
                    mouseEvent.getXOnScreen(), mouseEvent.getYOnScreen());
        } else if (event instanceof KeyEvent keyEvent
                && keyEvent.getID() == KeyEvent.KEY_PRESSED
                && keyEvent.getSource() instanceof Component keySource) {
            source = keySource;
            physicalPointerDown = false;
            physicalPointerScreenPoint = null;
        } else {
            return;
        }
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(
                    () -> returnFocusToSwingControl(
                            source,
                            physicalPointerDown,
                            physicalPointerScreenPoint));
            return;
        }
        returnFocusToSwingControl(
                source, physicalPointerDown, physicalPointerScreenPoint);
    }

    private void returnFocusToSwingControl(
            Component source,
            boolean physicalPointerDown,
            Point physicalPointerScreenPoint) {
        if (swingFocusRepairNativeRelease != null
                || swingFocusRepairReleasedJvmAuthority != null) {
            clearSwingFocusClaim();
        }
        long bootstrapEpoch = ++nativeCanvasFocusBootstrapEpoch;
        if (canvasOwner == null
                || !isDesignInteractionActive()) {
            return;
        }
        boolean exactNativeCanvasPress = isNativeCanvasFocusBootstrapPress(
                physicalPointerDown,
                source,
                canvasFocusSurface(),
                pointInsideShowingComponent(
                        physicalPointerScreenPoint, canvasFocusSurface()));
        FlutterDesignerCanvasSession.InteractionBarrierState barrier =
                canvasOwner.interactionBarrierState();
        boolean hostShowing = canvasHostComponent() != null
                && canvasHostComponent().isShowing();
        boolean exactSurfaceShowing = canvasFocusSurface() != null
                && canvasFocusSurface().isShowing();
        boolean bootstrapAllowed = canScheduleNativeCanvasFocusBootstrap(
                exactNativeCanvasPress,
                isDesignInteractionActive(),
                hostShowing,
                source.isShowing(),
                exactSurfaceShowing,
                canvasFrameRendered,
                barrier.inputEnabled(),
                bootstrapEpoch,
                nativeCanvasFocusBootstrapEpoch);
        if (bootstrapAllowed) {
            clearSwingFocusClaim();
            java.awt.EventQueue.invokeLater(() ->
                    completeNativeCanvasFocusBootstrap(
                            bootstrapEpoch,
                            source,
                            physicalPointerScreenPoint));
            return;
        }
        switch (classifySwingInput(
                source, visual, toolbar, canvasHostComponent())) {
            case NATIVE_CANVAS -> {
                clearSwingFocusClaim();
                return;
            }
            case OTHER_AWT -> {
                // A real input event in another NetBeans control is newer than
                // a retained target from this Design view. It must fence both
                // late Swing repair and deferred native activation.
                clearSwingFocusClaim();
                canvasOwner.clearFocusRequest();
                if (isStandardSwingMenuInteraction(source)) {
                    releaseRunnerFocusSafely(
                            canvasOwner::releaseSurfaceFocus);
                }
                return;
            }
            case DESIGN_SWING -> {
                // Continue with an exact retained Swing target below.
            }
        }
        long repairEpoch = ++swingFocusRepairEpoch;
        swingFocusClaimedForActivation = true;
        retainedSwingFocusTarget = nearestFocusableSwingComponent(
                source, visual, toolbar);
        cancelSwingFocusRepairTimer();
        canvasOwner.clearFocusRequest();
        repairClaimedSwingFocusIfRunnerFocused(repairEpoch);
    }

    private void completeNativeCanvasFocusBootstrap(
            long bootstrapEpoch,
            Component source,
            Point physicalPointerScreenPoint) {
        if (canvasOwner == null) {
            return;
        }
        FlutterDesignerCanvasSession.InteractionBarrierState barrier =
                canvasOwner.interactionBarrierState();
        boolean exactNativeCanvasPress = isNativeCanvasFocusBootstrapPress(
                true,
                source,
                canvasFocusSurface(),
                pointInsideShowingComponent(
                        physicalPointerScreenPoint, canvasFocusSurface()));
        boolean hostShowing = canvasHostComponent() != null
                && canvasHostComponent().isShowing();
        boolean exactSurfaceShowing = canvasFocusSurface() != null
                && canvasFocusSurface().isShowing();
        boolean menuPathEmpty = MenuSelectionManager.defaultManager()
                .getSelectedPath().length == 0;
        boolean bootstrapAllowed = canCompleteNativeCanvasFocusBootstrap(
                exactNativeCanvasPress,
                isDesignInteractionActive(),
                hostShowing,
                source.isShowing(),
                exactSurfaceShowing,
                canvasFrameRendered,
                barrier.inputEnabled(),
                menuPathEmpty,
                bootstrapEpoch,
                nativeCanvasFocusBootstrapEpoch);
        if (!bootstrapAllowed) {
            return;
        }
        // A lightweight ancestor can receive the first physical press after a
        // cross-process menu focus transfer even though its screen point is
        // inside the exact AWT Canvas. Wait until that AWT press is completely
        // dispatched before restoring the verified Flutter child. This owns
        // no model-mutation authority and is fenced to the synchronized frame.
        canvasOwner.requestFocus();
    }

    private void repairClaimedSwingFocusIfRunnerFocused() {
        repairClaimedSwingFocusIfRunnerFocused(swingFocusRepairEpoch);
    }

    private void repairClaimedSwingFocusIfRunnerFocused(long repairEpoch) {
        JComponent focusTarget = retainedSwingFocusTarget;
        if (!swingFocusClaimedForActivation
                || focusTarget == null
                || repairEpoch != swingFocusRepairEpoch
                || canvasOwner == null
                || !isDesignInteractionActive()
                || !canvasFrameRendered) {
            return;
        }
        if (!isUsableSwingFocusTarget(
                focusTarget, visual, toolbar, canvasHostComponent())) {
            clearSwingFocusClaim();
            return;
        }
        Component currentOwner = KeyboardFocusManager
                .getCurrentKeyboardFocusManager()
                .getPermanentFocusOwner();
        if (currentOwner != null
                && currentOwner != focusTarget
                && !sameOrDescendant(currentOwner, focusTarget)) {
            // Do not steal focus from a newer AWT owner if its property event
            // has not reached this listener yet.
            clearSwingFocusClaim();
            canvasOwner.clearFocusRequest();
            return;
        }
        final boolean runnerFocused;
        try {
            runnerFocused = canvasOwner.isSurfaceFocused();
        } catch (RuntimeException | LinkageError ignored) {
            // Focus repair is intentionally non-terminal. The host/session
            // remain the authorities for reporting attachment failures.
            scheduleSwingFocusRepair(repairEpoch, focusTarget);
            return;
        }
        if (!runnerFocused) {
            boolean canReuseRelease = canReuseVerifiedRunnerRelease(
                    swingFocusRepairReleasedJvmAuthority,
                    repairEpoch,
                    focusTarget,
                    swingFocusClaimedForActivation,
                    retainedSwingFocusTarget,
                    isDesignInteractionActive(),
                    canvasFrameRendered);
            if (canReuseRelease) {
                requestRetainedSwingFocusDeferred(
                        repairEpoch, focusTarget, null);
                return;
            }
            scheduleSwingFocusRepair(repairEpoch, focusTarget);
            return;
        }
        // Native focus returned to this runner after any earlier release, so
        // that JVM-authority proof is no longer current. A new proof is issued
        // only after the exact host completes another verified release.
        swingFocusRepairReleasedJvmAuthority = null;
        SwingFocusRepairNativeReleaseMarker releaseMarker =
                new SwingFocusRepairNativeReleaseMarker(
                        repairEpoch,
                        nativeCanvasFocusBootstrapEpoch,
                        focusTarget);
        swingFocusRepairNativeRelease = releaseMarker;
        if (!beginSwingFocusRepairFromRunner(
                canvasOwner::releaseSurfaceFocus,
                () -> clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                        () -> KeyboardFocusManager
                                .getCurrentKeyboardFocusManager()
                                .getPermanentFocusOwner(),
                        focusTarget,
                        () -> KeyboardFocusManager
                                .getCurrentKeyboardFocusManager()
                                .clearGlobalFocusOwner()))) {
            if (swingFocusRepairNativeRelease == releaseMarker) {
                swingFocusRepairNativeRelease = null;
            }
            scheduleSwingFocusRepair(repairEpoch, focusTarget);
            return;
        }
        swingFocusRepairReleasedJvmAuthority =
                new SwingFocusRepairReleasedJvmAuthority(
                        repairEpoch, focusTarget);
        requestRetainedSwingFocusDeferred(
                repairEpoch, focusTarget, releaseMarker);
    }

    private void requestRetainedSwingFocusDeferred(
            long repairEpoch,
            JComponent focusTarget,
            SwingFocusRepairNativeReleaseMarker releaseMarker) {
        stopSwingFocusRepairTimerPreservingAttempts();
        long barrierTransitionEpoch = interactionBarrierTransitionEpoch;
        java.awt.EventQueue.invokeLater(() -> {
            try {
                Component owner = KeyboardFocusManager
                        .getCurrentKeyboardFocusManager()
                        .getPermanentFocusOwner();
                boolean continuationCurrent = canApplySwingFocusRepairContinuation(
                        repairEpoch,
                        swingFocusRepairEpoch,
                        focusTarget,
                        retainedSwingFocusTarget,
                        barrierTransitionEpoch,
                        interactionBarrierTransitionEpoch,
                        swingFocusClaimedForActivation,
                        isDesignInteractionActive(),
                        canvasFrameRendered);
                if (continuationCurrent
                        && isUsableSwingFocusTarget(
                                focusTarget,
                                visual,
                                toolbar,
                                canvasHostComponent())
                        && currentOwnerAllowsSwingFocusRepair(owner, focusTarget)) {
                    try {
                        boolean accepted = focusTarget.requestFocusInWindow();
                        if (accepted
                                && repairEpoch == swingFocusRepairEpoch
                                && retainedSwingFocusTarget == focusTarget) {
                            // Swing accepted the request, but Windows can still
                            // transfer native focus to the child FlutterView while
                            // attach or first-frame delivery completes. Retain the
                            // exact target for the bounded typed-rendered window.
                            scheduleSwingFocusRepair(repairEpoch, focusTarget);
                        } else {
                            scheduleSwingFocusRepair(repairEpoch, focusTarget);
                        }
                    } catch (RuntimeException | LinkageError ignored) {
                        scheduleSwingFocusRepair(repairEpoch, focusTarget);
                    }
                } else if (continuationCurrent
                        && owner != null
                        && !currentOwnerAllowsSwingFocusRepair(owner, focusTarget)) {
                    clearSwingFocusClaim();
                    canvasOwner.clearFocusRequest();
                }
            } finally {
                if (swingFocusRepairNativeRelease == releaseMarker) {
                    swingFocusRepairNativeRelease = null;
                }
            }
        });
    }

    private void scheduleSwingFocusRepair(
            long repairEpoch,
            JComponent focusTarget) {
        if (!canScheduleSwingFocusRepair(
                canvasFrameRendered,
                swingFocusClaimedForActivation,
                repairEpoch,
                swingFocusRepairEpoch,
                focusTarget,
                retainedSwingFocusTarget,
                isDesignInteractionActive(),
                swingFocusRepairTimer != null)) {
            return;
        }
        Optional<SwingFocusRepairTicket> reserved =
                swingFocusRepairRetryFence.reserve(repairEpoch, focusTarget);
        if (reserved.isEmpty()) {
            return;
        }
        SwingFocusRepairTicket ticket = reserved.orElseThrow();
        Timer timer = new Timer(SWING_FOCUS_REPAIR_DELAY_MILLIS, null);
        timer.setRepeats(false);
        timer.addActionListener(event -> retryClaimedSwingFocus(timer, ticket));
        swingFocusRepairTimer = timer;
        timer.start();
    }

    private void retryClaimedSwingFocus(
            Timer timer,
            SwingFocusRepairTicket ticket) {
        if (timer != swingFocusRepairTimer
                || !swingFocusRepairRetryFence.isActive(ticket)) {
            return;
        }
        timer.stop();
        swingFocusRepairTimer = null;
        swingFocusRepairRetryFence.consume(ticket);
        repairClaimedSwingFocusIfRunnerFocused(ticket.repairEpoch());
    }

    private void stopSwingFocusRepairTimerPreservingAttempts() {
        Timer timer = swingFocusRepairTimer;
        swingFocusRepairTimer = null;
        if (timer != null) {
            timer.stop();
        }
        swingFocusRepairRetryFence.clearActive();
    }

    private void cancelSwingFocusRepairTimer() {
        Timer timer = swingFocusRepairTimer;
        swingFocusRepairTimer = null;
        if (timer != null) {
            timer.stop();
        }
        swingFocusRepairRetryFence.reset();
    }

    private void clearSwingFocusClaim() {
        swingFocusRepairEpoch++;
        swingFocusClaimedForActivation = false;
        retainedSwingFocusTarget = null;
        swingFocusRepairNativeRelease = null;
        swingFocusRepairReleasedJvmAuthority = null;
        cancelSwingFocusRepairTimer();
    }

    private boolean isDesignInteractionActive() {
        return designVisible || visual.isShowing();
    }

    static JComponent nearestFocusableSwingComponent(
            Component source,
            Component... allowedRoots) {
        if (source == null || !insideAnyRoot(source, allowedRoots)) {
            return null;
        }
        Component candidate = source;
        while (candidate != null && insideAnyRoot(candidate, allowedRoots)) {
            if (candidate instanceof JComponent swing
                    && swing.isEnabled()
                    && swing.isFocusable()
                    && swing.isRequestFocusEnabled()) {
                return swing;
            }
            candidate = candidate.getParent();
        }
        return null;
    }

    static boolean isUsableSwingFocusTarget(
            JComponent target,
            Component designRoot,
            Component toolbarRoot,
            Component nativeCanvasRoot) {
        return target != null
                && target.isShowing()
                && target.isEnabled()
                && target.isFocusable()
                && target.isRequestFocusEnabled()
                && shouldCancelDeferredCanvasFocus(
                        target, designRoot, toolbarRoot, nativeCanvasRoot);
    }

    static boolean currentOwnerAllowsSwingFocusRepair(
            Component currentOwner,
            JComponent retainedTarget) {
        return currentOwner == null
                || currentOwner == retainedTarget
                || sameOrDescendant(currentOwner, retainedTarget);
    }

    static boolean canScheduleSwingFocusRepair(
            boolean rendered,
            boolean claimed,
            long expectedEpoch,
            long currentEpoch,
            JComponent expectedTarget,
            JComponent retainedTarget,
            boolean interactionActive,
            boolean timerScheduled) {
        return rendered
                && claimed
                && expectedEpoch == currentEpoch
                && expectedTarget != null
                && expectedTarget == retainedTarget
                && interactionActive
                && !timerScheduled;
    }

    static boolean canApplySwingFocusRepairContinuation(
            long expectedRepairEpoch,
            long currentRepairEpoch,
            JComponent expectedTarget,
            JComponent retainedTarget,
            long expectedBarrierTransitionEpoch,
            long currentBarrierTransitionEpoch,
            boolean claimed,
            boolean interactionActive,
            boolean rendered) {
        return expectedRepairEpoch == currentRepairEpoch
                && expectedTarget != null
                && expectedTarget == retainedTarget
                && expectedBarrierTransitionEpoch == currentBarrierTransitionEpoch
                && claimed
                && interactionActive
                && rendered;
    }

    static boolean shouldRearmSwingFocusRepairOnRenderedTransition(
            boolean wasFrameRendered,
            boolean frameRendered,
            boolean swingFocusClaimed,
            JComponent retainedTarget) {
        return !wasFrameRendered
                && frameRendered
                && swingFocusClaimed
                && retainedTarget != null;
    }

    static boolean beginSwingFocusRepairFromRunner(
            BooleanSupplier releaseRunnerFocus,
            Runnable clearGlobalFocusOwner) {
        BooleanSupplier release = Objects.requireNonNull(
                releaseRunnerFocus, "releaseRunnerFocus");
        Runnable clear = Objects.requireNonNull(
                clearGlobalFocusOwner, "clearGlobalFocusOwner");
        final boolean released;
        try {
            released = release.getAsBoolean();
        } catch (ThreadDeath | VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable failure) {
            // Provider code runs on the NetBeans EDT. An ordinary extension
            // failure cannot escape; the bounded repair loop may retry while
            // host-owned identity/detach failures remain fail-closed there.
            LOGGER.log(
                    Level.FINE,
                    "Native Canvas provider could not release runner focus "
                    + "for a retained Swing interaction",
                    failure);
            return false;
        }
        if (!released) {
            return false;
        }
        try {
            clear.run();
            return true;
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    static void clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
            java.util.function.Supplier<Component> currentOwner,
            JComponent retainedTarget,
            Runnable clearGlobalFocusOwner) {
        java.util.function.Supplier<Component> ownerSupplier =
                Objects.requireNonNull(currentOwner, "currentOwner");
        JComponent target = Objects.requireNonNull(
                retainedTarget, "retainedTarget");
        Runnable clear = Objects.requireNonNull(
                clearGlobalFocusOwner, "clearGlobalFocusOwner");
        Component owner = ownerSupplier.get();
        if (owner != null && currentOwnerAllowsSwingFocusRepair(owner, target)) {
            // The user's exact Swing target still owns AWT focus. The native
            // release has already returned Win32 authority to the JVM parent;
            // clearing this owner would introduce an observable transient gap.
            return;
        }
        clear.run();
    }

    static boolean canPreserveSwingClaimForIntermediateNativeRelease(
            SwingFocusRepairNativeReleaseMarker marker,
            Component focusOwner,
            Component exactHostCanvas,
            long currentRepairEpoch,
            long currentInputEpoch,
            boolean swingFocusClaimed,
            JComponent retainedTarget) {
        return marker != null
                && focusOwner != null
                && exactHostCanvas != null
                && marker.repairEpoch() == currentRepairEpoch
                && marker.inputEpoch() == currentInputEpoch
                && swingFocusClaimed
                && marker.retainedTarget() == retainedTarget
                && sameOrDescendant(focusOwner, exactHostCanvas);
    }

    static boolean canReuseVerifiedRunnerRelease(
            SwingFocusRepairReleasedJvmAuthority authority,
            long currentRepairEpoch,
            JComponent expectedTarget,
            boolean swingFocusClaimed,
            JComponent retainedTarget,
            boolean interactionActive,
            boolean rendered) {
        return authority != null
                && authority.repairEpoch() == currentRepairEpoch
                && authority.retainedTarget() == expectedTarget
                && expectedTarget == retainedTarget
                && swingFocusClaimed
                && interactionActive
                && rendered;
    }

    record SwingFocusRepairNativeReleaseMarker(
            long repairEpoch,
            long inputEpoch,
            JComponent retainedTarget) {
        SwingFocusRepairNativeReleaseMarker {
            if (repairEpoch < 0 || inputEpoch < 0) {
                throw new IllegalArgumentException(
                        "focus release marker epochs must not be negative");
            }
            Objects.requireNonNull(retainedTarget, "retainedTarget");
        }
    }

    record SwingFocusRepairReleasedJvmAuthority(
            long repairEpoch,
            JComponent retainedTarget) {
        SwingFocusRepairReleasedJvmAuthority {
            if (repairEpoch < 0) {
                throw new IllegalArgumentException(
                        "released focus authority epoch must not be negative");
            }
            Objects.requireNonNull(retainedTarget, "retainedTarget");
        }
    }

    record SwingFocusRepairTicket(
            long ticketId,
            long repairEpoch,
            JComponent focusTarget) {
        SwingFocusRepairTicket {
            if (ticketId <= 0 || repairEpoch < 0) {
                throw new IllegalArgumentException(
                        "Swing focus repair ticket identity is invalid");
            }
            Objects.requireNonNull(focusTarget, "focusTarget");
        }
    }

    static final class SwingFocusRepairRetryFence {
        private final int maximumAttempts;
        private long nextTicketId;
        private long repairEpoch = -1;
        private JComponent focusTarget;
        private int attempts;
        private SwingFocusRepairTicket activeTicket;

        SwingFocusRepairRetryFence(int maximumAttempts) {
            if (maximumAttempts <= 0) {
                throw new IllegalArgumentException(
                        "maximumAttempts must be positive");
            }
            this.maximumAttempts = maximumAttempts;
        }

        Optional<SwingFocusRepairTicket> reserve(
                long candidateEpoch,
                JComponent candidateTarget) {
            Objects.requireNonNull(candidateTarget, "candidateTarget");
            if (candidateEpoch < 0) {
                throw new IllegalArgumentException(
                        "candidateEpoch must not be negative");
            }
            if (activeTicket != null) {
                return Optional.empty();
            }
            if (repairEpoch != candidateEpoch || focusTarget != candidateTarget) {
                repairEpoch = candidateEpoch;
                focusTarget = candidateTarget;
                attempts = 0;
            }
            if (attempts >= maximumAttempts) {
                return Optional.empty();
            }
            attempts++;
            activeTicket = new SwingFocusRepairTicket(
                    ++nextTicketId, candidateEpoch, candidateTarget);
            return Optional.of(activeTicket);
        }

        boolean isActive(SwingFocusRepairTicket ticket) {
            return activeTicket == ticket;
        }

        void consume(SwingFocusRepairTicket ticket) {
            if (activeTicket == ticket) {
                activeTicket = null;
            }
        }

        void clearActive() {
            activeTicket = null;
        }

        void reset() {
            activeTicket = null;
            repairEpoch = -1;
            focusTarget = null;
            attempts = 0;
        }

        int attempts() {
            return attempts;
        }
    }

    private static boolean insideAnyRoot(
            Component candidate,
            Component... allowedRoots) {
        if (candidate == null || allowedRoots == null) {
            return false;
        }
        for (Component root : allowedRoots) {
            if (root != null && sameOrDescendant(candidate, root)) {
                return true;
            }
        }
        return false;
    }

    static boolean shouldCancelDeferredCanvasFocus(
            Component focusOwner,
            Component designRoot,
            Component toolbarRoot,
            Component nativeCanvasRoot) {
        if (!(focusOwner instanceof JComponent)
                || (designRoot == null && toolbarRoot == null)
                || (!sameOrDescendant(focusOwner, designRoot)
                        && !sameOrDescendant(focusOwner, toolbarRoot))) {
            return false;
        }
        return nativeCanvasRoot == null
                || !sameOrDescendant(focusOwner, nativeCanvasRoot);
    }

    static SwingInputDisposition classifySwingInput(
            Component source,
            Component designRoot,
            Component toolbarRoot,
            Component nativeCanvasRoot) {
        if (nativeCanvasRoot != null
                && sameOrDescendant(source, nativeCanvasRoot)) {
            return SwingInputDisposition.NATIVE_CANVAS;
        }
        return shouldCancelDeferredCanvasFocus(
                source, designRoot, toolbarRoot, nativeCanvasRoot)
                ? SwingInputDisposition.DESIGN_SWING
                : SwingInputDisposition.OTHER_AWT;
    }

    static boolean isStandardSwingMenuInteraction(Component source) {
        Component candidate = source;
        while (candidate != null) {
            if (candidate instanceof MenuElement) {
                return true;
            }
            candidate = candidate.getParent();
        }
        return false;
    }

    static boolean isNativeCanvasFocusBootstrapPress(
            boolean physicalPointerDown,
            Component source,
            Component exactHostCanvas,
            boolean pointerInsideExactHostCanvas) {
        return physicalPointerDown
                && source != null
                && exactHostCanvas != null
                && sameOrDescendant(exactHostCanvas, source)
                && pointerInsideExactHostCanvas;
    }

    static boolean canScheduleNativeCanvasFocusBootstrap(
            boolean exactNativeCanvasPress,
            boolean designActive,
            boolean hostShowing,
            boolean sourceShowing,
            boolean exactHostCanvasShowing,
            boolean frameRendered,
            boolean interactionInputSynchronized,
            long expectedEpoch,
            long currentEpoch) {
        return exactNativeCanvasPress
                && designActive
                && hostShowing
                && sourceShowing
                && exactHostCanvasShowing
                && frameRendered
                && interactionInputSynchronized
                && expectedEpoch == currentEpoch;
    }

    static boolean canCompleteNativeCanvasFocusBootstrap(
            boolean exactNativeCanvasPress,
            boolean designActive,
            boolean hostShowing,
            boolean sourceShowing,
            boolean exactHostCanvasShowing,
            boolean frameRendered,
            boolean interactionInputSynchronized,
            boolean menuSelectionPathEmpty,
            long expectedEpoch,
            long currentEpoch) {
        return menuSelectionPathEmpty
                && canScheduleNativeCanvasFocusBootstrap(
                        exactNativeCanvasPress,
                        designActive,
                        hostShowing,
                        sourceShowing,
                        exactHostCanvasShowing,
                        frameRendered,
                        interactionInputSynchronized,
                        expectedEpoch,
                        currentEpoch);
    }

    private static boolean pointInsideShowingComponent(
            Point screenPoint,
            Component component) {
        if (screenPoint == null
                || component == null
                || !component.isShowing()
                || component.getWidth() <= 0
                || component.getHeight() <= 0) {
            return false;
        }
        try {
            Point location = component.getLocationOnScreen();
            return screenPoint.x >= location.x
                    && screenPoint.y >= location.y
                    && screenPoint.x < location.x + component.getWidth()
                    && screenPoint.y < location.y + component.getHeight();
        } catch (java.awt.IllegalComponentStateException unavailable) {
            return false;
        }
    }

    private JComponent canvasHostComponent() {
        return canvasOwner == null ? null : canvasOwner.component();
    }

    private Component canvasFocusSurface() {
        return canvasOwner == null ? null : canvasOwner.focusSurface();
    }

    void requestCanvasBackendForTests(
            FlutterDesignerCanvasBackendSelector.Backend backend) {
        requestCanvasBackend(backend);
    }

    private void requestCanvasBackend(
            FlutterDesignerCanvasBackendSelector.Backend backend) {
        desiredCanvasBackend = Objects.requireNonNull(backend, "backend");
        canvasOwnerCoordinator.requestBackend(backend);
    }

    void retryCanvasOwnerTransitionForTests() {
        canvasOwnerCoordinator.retryTransition();
    }

    FlutterDesignerCanvasOwnerCoordinator.Phase canvasOwnerPhaseForTests() {
        return canvasOwnerCoordinator.phase();
    }

    FlutterDesignerCanvasOwner canvasOwnerForTests() {
        return canvasOwner;
    }

    FlutterDesignerNativeCanvasStatus lastCanvasStatusForTests() {
        return lastCanvasStatus;
    }

    boolean canvasFocusListenersInstalledForTests() {
        return permanentFocusOwnerManager != null
                || swingInputFocusToolkit != null;
    }

    static void releaseRunnerFocusSafely(Runnable release) {
        Objects.requireNonNull(release, "release");
        try {
            release.run();
        } catch (ThreadDeath | VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable failure) {
            // This callback runs inside the global AWT listener. Ordinary
            // provider failures, including extension AssertionError, must not
            // escape onto the NetBeans EDT; native focus release remains
            // best-effort and the host owns attachment-invalid reporting.
            LOGGER.log(
                    Level.FINE,
                    "Native Canvas provider could not release runner focus",
                    failure);
        }
    }

    enum SwingInputDisposition {
        DESIGN_SWING,
        NATIVE_CANVAS,
        OTHER_AWT
    }

    private static boolean sameOrDescendant(Component child, Component root) {
        return root != null
                && (child == root || SwingUtilities.isDescendingFrom(child, root));
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoRedo;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        MultiViewElementCallback admitted = Objects.requireNonNull(
                callback, "callback");
        multiViewCallback = admitted;
        legacyEditorShellCloseRetryRequest = null;
        editorShellCloseRetryRequest = null;
        editorShellCloseFailureRequest = null;
        editorShellCloseAttemptId = -1;
    }

    /**
     * Binds the close retry to a plugin-owned editor shell without attempting
     * to construct NetBeans' package-private MultiView callback.
     */
    void setEditorShellCloseRetryRequest(Runnable retryRequest) {
        legacyEditorShellCloseRetryRequest = Objects.requireNonNull(
                retryRequest, "retryRequest");
        editorShellCloseRetryRequest = null;
        editorShellCloseFailureRequest = null;
        editorShellCloseAttemptId = -1;
        multiViewCallback = null;
    }

    void setEditorShellCloseCallbacks(
            LongConsumer retryRequest,
            BiConsumer<Long, Throwable> failureRequest) {
        editorShellCloseRetryRequest = Objects.requireNonNull(
                retryRequest, "retryRequest");
        editorShellCloseFailureRequest = Objects.requireNonNull(
                failureRequest, "failureRequest");
        legacyEditorShellCloseRetryRequest = null;
        editorShellCloseAttemptId = -1;
        multiViewCallback = null;
    }

    FlutterDesignerEditorPerspective.CloseBarrierState
            editorShellCloseBarrierState(long attemptId) {
        requireEditorShellCloseAttemptId(attemptId);
        if (!componentLifecycleOpen || !canvasBackendSelector.exactWebEnabled()) {
            return FlutterDesignerEditorPerspective.CloseBarrierState.NOT_REQUIRED;
        }
        if (editorShellCloseAttemptId < 0) {
            return FlutterDesignerEditorPerspective.CloseBarrierState.OPEN;
        }
        if (editorShellCloseAttemptId != attemptId) {
            return FlutterDesignerEditorPerspective.CloseBarrierState.STALE;
        }
        return switch (canvasCloseGateState) {
            case READY_SAVE, READY_DISCARD ->
                FlutterDesignerEditorPerspective.CloseBarrierState.READY;
            case OPEN, RETIRING ->
                FlutterDesignerEditorPerspective.CloseBarrierState.PENDING;
        };
    }

    void beginEditorShellClose(long attemptId) {
        requireEditorShellCloseAttemptId(attemptId);
        if (!java.awt.EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Designer editor close must begin on the Event Dispatch Thread");
        }
        if (!componentLifecycleOpen || !canvasBackendSelector.exactWebEnabled()) {
            return;
        }
        canvasCloseRecoveryRequested = false;
        if (editorShellCloseAttemptId < 0) {
            editorShellCloseAttemptId = attemptId;
        } else if (editorShellCloseAttemptId != attemptId) {
            throw new IllegalStateException(
                    "Another Designer editor close attempt owns the Canvas barrier");
        }
        beginCanvasClose(false);
    }

    void abandonEditorShellClose(long attemptId) {
        requireEditorShellCloseAttemptId(attemptId);
        if (editorShellCloseAttemptId == attemptId) {
            editorShellCloseAttemptId = -1;
            if (componentLifecycleOpen
                    && canvasBackendSelector.exactWebEnabled()
                    && canvasCloseGateState != CanvasCloseGateState.OPEN) {
                canvasCloseRecoveryRequested = true;
                scheduleAbandonedCanvasRecovery();
            }
        }
    }

    private static void requireEditorShellCloseAttemptId(long attemptId) {
        if (attemptId <= 0) {
            throw new IllegalArgumentException(
                    "Designer editor close attempt id must be positive");
        }
    }

    @Override
    public CloseOperationState canCloseElement() {
        if (!componentLifecycleOpen || !canvasBackendSelector.exactWebEnabled()) {
            return CloseOperationState.STATE_OK;
        }
        FlutterDesignerAsyncCloseOperationHandler.GatePhase phase = switch (
                canvasCloseGateState) {
            case OPEN, RETIRING ->
                FlutterDesignerAsyncCloseOperationHandler.GatePhase.PENDING;
            case READY_SAVE ->
                FlutterDesignerAsyncCloseOperationHandler.GatePhase.READY_SAVE;
            case READY_DISCARD ->
                FlutterDesignerAsyncCloseOperationHandler.GatePhase.READY_DISCARD;
        };
        Action proceed = canvasCloseAction(false);
        Action discard = canvasCloseAction(true);
        return FlutterDesignerAsyncCloseOperationHandler.canvasState(
                phase, proceed, discard);
    }

    private Action canvasCloseAction(boolean discard) {
        return new AbstractAction(
                discard ? "Discard and release Canvas" : "Release Canvas") {
            {
                putValue(Action.LONG_DESCRIPTION,
                        "Prepare the Flutter Designer Canvas native peer for safe close.");
                putValue(Action.SHORT_DESCRIPTION,
                        "Await Canvas peer-safe retirement before closing this editor.");
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                beginCanvasClose(discard);
            }
        };
    }

    private void beginCanvasClose(boolean discard) {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(() -> beginCanvasClose(discard));
            return;
        }
        if (!componentLifecycleOpen) {
            return;
        }
        if (discard) {
            canvasCloseDiscardAuthorized = true;
        }
        if (canvasCloseGateState == CanvasCloseGateState.READY_SAVE
                || canvasCloseGateState == CanvasCloseGateState.READY_DISCARD) {
            canvasCloseGateState = canvasCloseDiscardAuthorized
                    ? CanvasCloseGateState.READY_DISCARD
                    : CanvasCloseGateState.READY_SAVE;
            scheduleCanvasCloseRetry();
            return;
        }
        canvasCloseGateState = CanvasCloseGateState.RETIRING;
        FlutterDesignerCanvasOwnerCoordinator closingCoordinator =
                canvasOwnerCoordinator;
        long closingGeneration = canvasOwnerCoordinatorGeneration;
        if (canvasCloseCompletion == null) {
            // Register the exact identity before invoking closeAsync(). Its
            // implementation may fail synchronously, and the common terminal
            // path must still reject every stale or duplicate completion.
            canvasCloseCoordinator = closingCoordinator;
            canvasCloseCoordinatorGeneration = closingGeneration;
        } else if (canvasCloseCoordinator != closingCoordinator
                || canvasCloseCoordinatorGeneration != closingGeneration) {
            throw new IllegalStateException(
                    "Canvas close completion belongs to another coordinator generation");
        }
        final CompletionStage<Void> close;
        try {
            // Re-ping an already pending close as well. The coordinator keeps
            // its serial queue on a transient executor rejection, so a fresh
            // close action is the explicit recovery signal that reschedules
            // that retained work.
            if (closingCoordinator.phase()
                    == FlutterDesignerCanvasOwnerCoordinator.Phase.POISONED) {
                closingCoordinator.retryTransition();
            }
            close = closingCoordinator.closeAsync();
        } catch (RuntimeException | LinkageError failure) {
            canvasCloseCompleted(
                    closingCoordinator, closingGeneration, failure);
            return;
        }
        if (canvasCloseCompletion == null) {
            canvasCloseCompletion = close;
            close.whenComplete((ignored, failure) -> dispatchOnEdt(() ->
                    canvasCloseCompleted(
                            closingCoordinator,
                            closingGeneration,
                            failure)));
        }
    }

    private void canvasCloseCompleted(
            FlutterDesignerCanvasOwnerCoordinator closingCoordinator,
            long closingGeneration,
            Throwable failure) {
        CanvasCoordinatorBinding published = publishedCanvasOwnerCoordinator;
        if (canvasCloseCoordinator != closingCoordinator
                || canvasCloseCoordinatorGeneration != closingGeneration
                || canvasOwnerCoordinator != closingCoordinator
                || canvasOwnerCoordinatorGeneration != closingGeneration
                || published == null
                || published.coordinator() != closingCoordinator
                || published.generation() != closingGeneration) {
            return;
        }
        Throwable terminal = unwrapCompletionFailure(failure);
        if (terminal != null) {
            canvasCloseCompletion = null;
            canvasCloseCoordinator = null;
            canvasCloseCoordinatorGeneration = -1;
            canvasCloseRecoveryRequested = false;
            canvasCloseGateState = CanvasCloseGateState.OPEN;
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Flutter Designer Canvas release failed.",
                    "Operation: close Flutter Designer view. Target: "
                    + modelName + ". Reason: " + failureReason(terminal) + "."));
            notifyEditorShellCloseFailure(terminal);
            return;
        }
        canvasCloseGateState = canvasCloseDiscardAuthorized
                ? CanvasCloseGateState.READY_DISCARD
                : CanvasCloseGateState.READY_SAVE;
        if (canvasCloseRecoveryRequested && editorShellCloseAttemptId < 0) {
            recoverAbandonedCanvasIfReady();
            return;
        }
        scheduleCanvasCloseRetry();
    }

    private void scheduleAbandonedCanvasRecovery() {
        if (canvasCloseRecoveryScheduled) {
            return;
        }
        canvasCloseRecoveryScheduled = true;
        java.awt.EventQueue.invokeLater(() -> {
            canvasCloseRecoveryScheduled = false;
            recoverAbandonedCanvasIfReady();
        });
    }

    private void recoverAbandonedCanvasIfReady() {
        if (!canvasCloseRecoveryRequested
                || editorShellCloseAttemptId >= 0) {
            return;
        }
        if (!componentLifecycleOpen) {
            canvasCloseRecoveryRequested = false;
            return;
        }
        if (canvasCloseGateState == CanvasCloseGateState.RETIRING) {
            // A failed retirement deliberately stays POISONED and attached.
            // Explicit Retry must establish the peer-safe terminal completion
            // before any replacement generation is allowed to start.
            return;
        }
        if (canvasCloseGateState != CanvasCloseGateState.READY_SAVE
                && canvasCloseGateState != CanvasCloseGateState.READY_DISCARD) {
            canvasCloseRecoveryRequested = false;
            return;
        }
        FlutterDesignerCanvasOwnerCoordinator retiredCoordinator =
                canvasCloseCoordinator;
        if (retiredCoordinator == null
                || retiredCoordinator != canvasOwnerCoordinator
                || canvasCloseCoordinatorGeneration
                        != canvasOwnerCoordinatorGeneration
                || retiredCoordinator.phase()
                        != FlutterDesignerCanvasOwnerCoordinator.Phase.CLOSED
                || retiredCoordinator.activeOwner() != null
                || retiredCoordinator.retainedOwner() != null) {
            return;
        }

        FlutterDesignerCanvasBackendSelector.Backend recoveryBackend =
                desiredCanvasBackend;
        installNewCanvasOwnerCoordinator();
        canvasCloseCompletion = null;
        canvasCloseCoordinator = null;
        canvasCloseCoordinatorGeneration = -1;
        canvasCloseRecoveryRequested = false;
        canvasCloseDiscardAuthorized = false;
        canvasCloseGateState = CanvasCloseGateState.OPEN;
        requestCanvasBackend(recoveryBackend);
    }

    private void scheduleCanvasCloseRetry() {
        if (canvasCloseRetryScheduled || !componentLifecycleOpen) {
            return;
        }
        LongConsumer retryRequest = editorShellCloseRetryRequest;
        Runnable legacyRetryRequest = legacyEditorShellCloseRetryRequest;
        long attemptId = editorShellCloseAttemptId;
        MultiViewElementCallback callback = multiViewCallback;
        if (retryRequest == null && legacyRetryRequest == null && callback == null) {
            return;
        }
        canvasCloseRetryScheduled = true;
        java.awt.EventQueue.invokeLater(() -> {
            canvasCloseRetryScheduled = false;
            if (!componentLifecycleOpen) {
                return;
            }
            if (retryRequest != null
                    && attemptId > 0
                    && editorShellCloseRetryRequest == retryRequest
                    && editorShellCloseAttemptId == attemptId) {
                retryRequest.accept(attemptId);
            } else if (retryRequest == null
                    && legacyRetryRequest != null
                    && legacyEditorShellCloseRetryRequest == legacyRetryRequest) {
                legacyRetryRequest.run();
            } else if (retryRequest == null
                    && legacyRetryRequest == null
                    && multiViewCallback == callback) {
                callback.getTopComponent().close();
            } else if (canvasCloseGateState == CanvasCloseGateState.READY_SAVE
                    || canvasCloseGateState
                            == CanvasCloseGateState.READY_DISCARD) {
                // The queued wakeup belongs to an attempt that was abandoned
                // or whose callback was replaced.  A newer token may already
                // own the same peer-safe Canvas barrier.  Hand the wakeup to
                // that exact current owner instead of letting the stale event
                // consume the single scheduling bit.  Do not reschedule an
                // abandoned tokenized route with no current attempt: that
                // would spin the EDT forever while the barrier remains ready.
                boolean currentRetryAvailable =
                        editorShellCloseRetryRequest != null
                                ? editorShellCloseAttemptId > 0
                                : legacyEditorShellCloseRetryRequest != null
                                || multiViewCallback != null;
                if (currentRetryAvailable) {
                    scheduleCanvasCloseRetry();
                }
            }
        });
    }

    private void notifyEditorShellCloseFailure(Throwable failure) {
        long attemptId = editorShellCloseAttemptId;
        BiConsumer<Long, Throwable> failureRequest =
                editorShellCloseFailureRequest;
        if (attemptId <= 0 || failureRequest == null) {
            return;
        }
        try {
            failureRequest.accept(attemptId, Objects.requireNonNull(
                    failure, "failure"));
        } catch (RuntimeException | LinkageError callbackFailure) {
            LOGGER.log(Level.WARNING,
                    "Designer editor close-failure callback failed",
                    callbackFailure);
        } finally {
            if (editorShellCloseAttemptId == attemptId) {
                abandonEditorShellClose(attemptId);
            }
        }
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
                null,
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
                allowMutation ? propertyMutationHandler(snapshot) : null,
                allowMutation ? slotMutationHandler(snapshot) : null);
    }

    private void publishCanvasPresentation(
            DesignerDocument document,
            WidgetCatalog catalog,
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler,
            FlutterWidgetPropertiesNode.SlotMutationHandler slotMutationHandler) {
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
                mutationHandler,
                new FlutterWidgetSlotEditorContext(
                        currentCanvasDocument,
                        currentCanvasCatalog,
                        currentCanvasCatalog.paletteDefinitions().stream()
                                .filter(definition -> BuiltInWidgetCapabilityCatalog.supports(
                                        definition, WidgetCapability.CREATE))
                                .map(definition -> definition.typeId())
                                .toList()),
                slotMutationHandler);
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
        if (canvasOwner != null) {
            canvasOwner.withdraw();
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
        if (document == null || catalog == null) {
            return;
        }
        Optional<PreviewTarget> selected = selectedPreviewTarget();
        if (selected.isEmpty()) {
            clearPresentedCanvasIdentity();
            if (canvasOwner != null) {
                canvasOwner.withdraw();
            }
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: " + modelName + " preview. Reason: the Flutter project "
                    + "has no configured Android, iOS, Web, Windows, macOS or Linux "
                    + "platform directory."));
            return;
        }
        PreviewTarget target = selected.orElseThrow();
        FlutterDesignerCanvasBackendSelector.Backend requestedBackend =
                canvasBackendSelector.select(target.targetPlatform());
        requestCanvasBackend(requestedBackend);
        FlutterDesignerCanvasOwner owner = canvasOwner;
        if (owner == null || owner.backend() != requestedBackend) {
            clearPresentedCanvasIdentity();
            return;
        }
        if (dataObject == null) {
            clearPresentedCanvasIdentity();
            owner.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    canvasTitle(owner.backend()) + " is unavailable.",
                    "Target: " + modelName + " project theme. Reason: the owning "
                    + "Flutter project is unavailable."));
            return;
        }
        if (projectThemeResolutionController == null) {
            clearPresentedCanvasIdentity();
            owner.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    canvasTitle(owner.backend()) + " is unavailable.",
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
            owner.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.STARTING,
                    "Resolving " + canvasTitle(owner.backend())
                    + " project theme...",
                    "Target: " + modelName + " project theme. Verifying the bounded "
                    + "descriptor and generated Dart hash outside the UI thread."));
            return;
        }
        FlutterDesignerProjectThemeResolver.Resolution themeResolution =
                cachedTheme.orElseThrow();
        if (!themeResolution.available()) {
            clearPresentedCanvasIdentity();
            owner.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    canvasTitle(owner.backend()) + " theme is unavailable.",
                    "Target: " + modelName + " project theme. Reason: "
                    + themeResolution.detail()));
            return;
        }
        CanvasResolvedTheme resolvedTheme = themeResolution.theme().orElseThrow();
        if (presentedCanvasDocument == document
                && presentedCanvasCatalog == catalog
                && Objects.equals(presentedCanvasTarget, target)
                && Objects.equals(presentedCanvasTheme, resolvedTheme)) {
            selectedWidgetId().ifPresent(owner::selectWidget);
            return;
        }
        owner.present(
                document,
                catalog,
                target.mode(),
                target.targetPlatform(),
                resolvedTheme);
        presentedCanvasDocument = document;
        presentedCanvasCatalog = catalog;
        presentedCanvasTarget = target;
        presentedCanvasTheme = resolvedTheme;
        selectedWidgetId().ifPresent(owner::selectWidget);
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

    private void viewportPresentationChanged(
            CanvasViewportPresentation presentation) {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(
                    () -> viewportPresentationChanged(presentation));
            return;
        }
        pendingViewportPresentation = Objects.requireNonNull(
                presentation, "presentation");
        // Coalesce rapid preset changes so the bounded protocol queue always
        // carries the latest transform. Native scrolling remains Flutter-owned.
        viewportCommandTimer.restart();
    }

    private void flushViewportPresentation() {
        CanvasViewportPresentation presentation = pendingViewportPresentation;
        pendingViewportPresentation = null;
        if (presentation != null && canvasOwner != null) {
            canvasOwner.setViewportPresentation(presentation);
        }
    }

    private void renderViewportMetrics(CanvasViewportMetrics metrics) {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(() -> renderViewportMetrics(metrics));
            return;
        }
        viewportControls.setMetrics(Objects.requireNonNull(metrics, "metrics"));
        viewportControls.setControlsEnabled(true);
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
                    + "choices use Flutter adaptive appearance on the active embedded "
                    + "native engine. Web uses a responsive browser-sized viewport on the same "
                    + "native engine; browser-only runtime behavior is not emulated.";
        }
        previewModes.setToolTipText(description);
        previewModes.getAccessibleContext().setAccessibleDescription(description);
    }

    private void rebuildWidgetTree(
            WidgetNode root,
            WidgetCatalog catalog,
            StableId retainedSelection,
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            FlutterWidgetPropertiesNode.SlotMutationHandler slotMutationHandler) {
        synchronizingSelection = true;
        try {
            widgetNodes.clear();
            Node rootNode = buildWidgetNode(
                    root,
                    catalog,
                    mutationHandler,
                    slotEditorContext,
                    slotMutationHandler);
            explorerManager.setRootContext(rootNode);
            StableId requested = pendingWidgetSelection != null
                    && widgetNodes.containsKey(pendingWidgetSelection)
                    ? pendingWidgetSelection : null;
            StableId selected = requested != null
                    ? requested
                    : retainedSelection != null
                    && widgetNodes.containsKey(retainedSelection)
                    ? retainedSelection
                    : root.id();
            explorerManager.setSelectedNodes(new Node[]{widgetNodes.get(selected)});
            if (requested != null) {
                pendingWidgetSelection = null;
            }
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
            FlutterWidgetPropertiesNode.PropertyMutationHandler mutationHandler,
            FlutterWidgetSlotEditorContext slotEditorContext,
            FlutterWidgetPropertiesNode.SlotMutationHandler slotMutationHandler) {
        Children.Array children = new Children.Array();
        for (WidgetSlot slot : widget.slots().values()) {
            switch (slot) {
                case WidgetSlot.SingleSlot single -> single.child().ifPresent(
                        child -> children.add(new Node[]{buildWidgetNode(
                            child,
                            catalog,
                            mutationHandler,
                            slotEditorContext,
                            slotMutationHandler)}));
                case WidgetSlot.ListSlot list -> {
                    for (WidgetNode child : list.children()) {
                        children.add(new Node[]{buildWidgetNode(
                            child,
                            catalog,
                            mutationHandler,
                            slotEditorContext,
                            slotMutationHandler)});
                    }
                }
            }
        }
        var definition = catalog.find(widget.type()).orElseThrow(() ->
                new IllegalStateException(
                        "Validated Flutter widget type is absent from its catalog: "
                        + widget.type().value()));
        Node node = new FlutterWidgetPropertiesNode(
                children,
                widget,
                definition,
                mutationHandler,
                slotEditorContext,
                slotMutationHandler);
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

    private FlutterWidgetPropertiesNode.SlotMutationHandler
            slotMutationHandler(
                    FlutterDesignerMutationController.Snapshot candidate) {
        return slotMutationHandler(candidate, null, null);
    }

    /** Package-private seam for exercising the real revision-bound bridge. */
    FlutterWidgetPropertiesNode.SlotMutationHandler slotMutationHandlerForTests(
            BiConsumer<FlutterWidgetSlotMutation, String> failurePresenter) {
        return slotMutationHandler(
                mutationSnapshot,
                Objects.requireNonNull(failurePresenter, "failurePresenter"),
                null);
    }

    /** Package-private seam which also observes the exact controller result. */
    FlutterWidgetPropertiesNode.SlotMutationHandler slotMutationHandlerForTests(
            BiConsumer<FlutterWidgetSlotMutation, String> failurePresenter,
            Consumer<FlutterDesignerMutationController.MutationResult>
                    completion) {
        return slotMutationHandler(
                mutationSnapshot,
                Objects.requireNonNull(failurePresenter, "failurePresenter"),
                Objects.requireNonNull(completion, "completion"));
    }

    private FlutterWidgetPropertiesNode.SlotMutationHandler
            slotMutationHandler(
                    FlutterDesignerMutationController.Snapshot candidate,
                    BiConsumer<FlutterWidgetSlotMutation, String>
                            injectedFailurePresenter,
                    Consumer<FlutterDesignerMutationController.MutationResult>
                            injectedCompletion) {
        FlutterDesignerMutationController controllerForEdit = mutationController;
        if (!mutationUiEnabled.getAsBoolean()
                || controllerForEdit == null
                || candidate == null
                || controllerForEdit.snapshot() != candidate
                || candidate.status() != FlutterDesignerMutationController.Status.READY
                || candidate.token().isEmpty()
                || candidate.document().isEmpty()
                || candidate.catalog().isEmpty()) {
            return null;
        }
        FlutterDesignerMutationController.RevisionToken exactToken =
                candidate.token().orElseThrow();
        long exactViewEpoch = mutationViewEpoch;
        BiConsumer<FlutterWidgetSlotMutation, String> failurePresenter =
                injectedFailurePresenter != null
                        ? injectedFailurePresenter
                        : (intent, reason) -> showSlotMutationFailure(
                                controllerForEdit,
                                exactViewEpoch,
                                intent,
                                reason);
        AtomicBoolean submitted = new AtomicBoolean();
        return intent -> {
            Objects.requireNonNull(intent, "intent");
            if (!submitted.compareAndSet(false, true)) {
                return;
            }
            if (!slotMutationAuthorityMatches(
                    controllerForEdit, candidate, exactToken)) {
                failurePresenter.accept(
                        intent,
                        "The slot editor belongs to an older Designer revision; "
                        + "reopen it from the current Properties view.");
                return;
            }

            SlotMutationPlan plan;
            try {
                plan = planSlotMutation(candidate, intent);
            } catch (IllegalArgumentException failure) {
                failurePresenter.accept(intent, failureReason(failure));
                return;
            }

            slotWidgetSubmitting = true;
            updateDeleteWidgetAction();
            try {
                submitDesignerMutation(
                        controllerForEdit,
                        exactToken,
                        plan.command(),
                        plan.operation(),
                        plan.target(),
                        result -> {
                            slotMutationCompleted(plan.selectionAfterApply(), result);
                            if (injectedCompletion != null) {
                                injectedCompletion.accept(result);
                            }
                        });
            } catch (RuntimeException failure) {
                slotWidgetSubmitting = false;
                updateDeleteWidgetAction();
                failurePresenter.accept(intent, failureReason(failure));
            }
        };
    }

    private boolean slotMutationAuthorityMatches(
            FlutterDesignerMutationController controllerForEdit,
            FlutterDesignerMutationController.Snapshot candidate,
            FlutterDesignerMutationController.RevisionToken exactToken) {
        FlutterDesignerMutationController.Snapshot latest = mutationSnapshot;
        return java.awt.EventQueue.isDispatchThread()
                && !slotWidgetSubmitting
                && !moveWidgetSubmitting
                && !deleteWidgetSubmitting
                && !inlineTextEditSubmitting
                && componentLifecycleOpen
                && mutationListening
                && mutationController == controllerForEdit
                && controllerForEdit.snapshot() == candidate
                && latest == candidate
                && latest.status() == FlutterDesignerMutationController.Status.READY
                && latest.token().filter(exactToken::equals).isPresent()
                && latest.document().filter(document -> document == currentCanvasDocument)
                        .isPresent()
                && latest.catalog().filter(catalog -> catalog == currentCanvasCatalog)
                        .isPresent()
                && currentCanvasMutationEnabled;
    }

    private SlotMutationPlan planSlotMutation(
            FlutterDesignerMutationController.Snapshot candidate,
            FlutterWidgetSlotMutation intent) {
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        String exactSlot = intent.ownerId() + "." + intent.slotName().value();
        return switch (intent) {
            case FlutterWidgetSlotMutation.Add add -> {
                FlutterDesignerPaletteDropPlanner.Result planned =
                        paletteDropPlanner.plan(
                                document,
                                catalog,
                                add.widgetType(),
                                add.ownerId(),
                                add.slotName(),
                                add.index(),
                                StableId::random);
                if (planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected) {
                    throw new IllegalArgumentException(rejected.reason());
                }
                AddWidget command = ((FlutterDesignerPaletteDropPlanner.Accepted) planned)
                        .command();
                String displayName = catalog.find(add.widgetType()).orElseThrow()
                        .palette().displayName();
                yield new SlotMutationPlan(
                        command,
                        "Add Flutter " + displayName + " widget",
                        modelName + " — add " + displayName + " to " + exactSlot
                        + " at index " + command.destination().index(),
                        command.widget().id());
            }
            case FlutterWidgetSlotMutation.Move move -> {
                FlutterDesignerWidgetMovePlanner.Result planned =
                        widgetMovePlanner.plan(
                                document,
                                catalog,
                                move.sourceId(),
                                new FlutterDesignerWidgetMovePlanner.IntoSlot(
                                        move.ownerId(),
                                        move.slotName(),
                                        move.postRemovalIndex()));
                if (planned instanceof FlutterDesignerWidgetMovePlanner.Rejected rejected) {
                    throw new IllegalArgumentException(rejected.reason());
                }
                MoveWidget command = ((FlutterDesignerWidgetMovePlanner.Accepted) planned)
                        .command();
                yield new SlotMutationPlan(
                        command,
                        "Move Flutter widget",
                        modelName + " — move widget " + move.sourceId()
                        + " to " + exactSlot + " at index "
                        + command.destination().index(),
                        move.sourceId());
            }
            case FlutterWidgetSlotMutation.Remove remove -> {
                WidgetNode owner = findWidget(document.root(), remove.ownerId())
                        .orElseThrow(() -> new IllegalArgumentException(
                        "Slot owner '" + remove.ownerId()
                        + "' does not exist in the current Designer revision."));
                var ownerDefinition = catalog.find(owner.type())
                        .orElseThrow(() -> new IllegalArgumentException(
                        "Catalog has no definition for slot owner type '"
                        + owner.type().value() + "'."));
                var slotDefinition = ownerDefinition.slot(remove.slotName())
                        .orElseThrow(() -> new IllegalArgumentException(
                        "Catalog definition '" + owner.type().value()
                        + "' has no slot '" + remove.slotName().value() + "'."));
                WidgetSlot value = owner.slots().get(remove.slotName());
                if (value == null
                        || value.cardinality() != slotDefinition.cardinality()
                        || directSlotChildren(value).stream()
                                .noneMatch(child -> child.id().equals(remove.childId()))) {
                    throw new IllegalArgumentException(
                            "Widget '" + remove.childId()
                            + "' is no longer a direct child of slot '"
                            + exactSlot + "'.");
                }
                int remaining = directSlotChildren(value).size() - 1;
                if (remaining < slotDefinition.minChildren()) {
                    throw new IllegalArgumentException(
                            "Removing widget '" + remove.childId()
                            + "' would leave slot '" + exactSlot
                            + "' below its minimum of "
                            + slotDefinition.minChildren() + " children.");
                }
                yield new SlotMutationPlan(
                        new RemoveWidget(remove.childId()),
                        "Remove Flutter widget from slot",
                        modelName + " — remove widget " + remove.childId()
                        + " from " + exactSlot,
                        remove.ownerId());
            }
            case FlutterWidgetSlotMutation.Replace replace -> {
                RevisionSlot slot = revisionSlot(
                        document, catalog, replace.ownerId(), replace.slotName());
                WidgetNode current = exactSingleChild(
                        slot, replace.expectedChildId(), exactSlot);
                ReplaceSlotChild.Replacement replacement;
                StableId selection;
                String replacementLabel;
                if (replace.replacement()
                        instanceof FlutterWidgetSlotMutation.Replace.NewWidget fresh) {
                    WidgetDefinition definition = catalog.find(fresh.widgetType())
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Catalog has no definition for replacement widget type '"
                            + fresh.widgetType().value() + "'."));
                    if (!slot.definition().acceptance().accepts(definition)) {
                        throw new IllegalArgumentException(
                                "Slot '" + exactSlot + "' does not accept widget type '"
                                + fresh.widgetType().value() + "'.");
                    }
                    WidgetNode prototype = WidgetNodePrototypeFactory.create(
                            definition, StableId.random());
                    replacement = new ReplaceSlotChild.NewSubtree(prototype);
                    selection = prototype.id();
                    replacementLabel = "new " + definition.palette().displayName()
                            + " widget " + prototype.id();
                } else {
                    StableId sourceId = ((FlutterWidgetSlotMutation.Replace.ExistingWidget)
                            replace.replacement()).sourceId();
                    WidgetNode source = findWidget(document.root(), sourceId)
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Replacement widget '" + sourceId
                            + "' does not exist in the current Designer revision."));
                    if (source.id().equals(document.root().id())) {
                        throw new IllegalArgumentException(
                                "The required Designer root cannot replace a slot child.");
                    }
                    if (source.id().equals(current.id())) {
                        throw new IllegalArgumentException(
                                "Widget '" + sourceId
                                + "' is already the exact child of slot '"
                                + exactSlot + "'.");
                    }
                    WidgetDefinition definition = catalog.find(source.type())
                            .orElseThrow(() -> new IllegalArgumentException(
                            "Catalog has no definition for replacement widget type '"
                            + source.type().value() + "'."));
                    if (!slot.definition().acceptance().accepts(definition)) {
                        throw new IllegalArgumentException(
                                "Slot '" + exactSlot + "' does not accept widget type '"
                                + source.type().value() + "'.");
                    }
                    if (containsWidget(source, replace.ownerId())) {
                        throw new IllegalArgumentException(
                                "Widget '" + sourceId
                                + "' cannot replace a child of a widget in its own subtree.");
                    }
                    replacement = new ReplaceSlotChild.ExistingWidget(sourceId);
                    selection = sourceId;
                    replacementLabel = "existing "
                            + definition.palette().displayName() + " widget " + sourceId;
                }
                yield new SlotMutationPlan(
                        new ReplaceSlotChild(
                                replace.ownerId(),
                                replace.slotName(),
                                replace.expectedChildId(),
                                replacement),
                        "Replace Flutter single-slot child",
                        modelName + " — replace widget " + current.id() + " in "
                        + exactSlot + " with " + replacementLabel,
                        selection);
            }
            case FlutterWidgetSlotMutation.ClearAll clear -> {
                RevisionSlot slot = revisionSlot(
                        document, catalog, clear.ownerId(), clear.slotName());
                if (slot.definition().cardinality()
                                != dev.flutter.netbeans.designer.model.SlotCardinality.LIST
                        || slot.value() == null
                        || slot.value().cardinality()
                                != dev.flutter.netbeans.designer.model.SlotCardinality.LIST) {
                    throw new IllegalArgumentException(
                            "Slot '" + exactSlot + "' is not a valid list slot.");
                }
                List<StableId> actual = slot.children().stream()
                        .map(WidgetNode::id)
                        .toList();
                if (!actual.equals(clear.expectedChildIds())) {
                    throw new IllegalArgumentException(
                            "Slot '" + exactSlot
                            + "' changed after the editor opened; expected direct child ids "
                            + clear.expectedChildIds() + " but found " + actual + ".");
                }
                if (actual.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Slot '" + exactSlot + "' is already empty.");
                }
                if (slot.definition().minChildren() > 0) {
                    throw new IllegalArgumentException(
                            "Clearing slot '" + exactSlot
                            + "' would leave it below its minimum of "
                            + slot.definition().minChildren() + " children.");
                }
                yield new SlotMutationPlan(
                        new ClearSlotChildren(
                                clear.ownerId(),
                                clear.slotName(),
                                clear.expectedChildIds()),
                        "Clear all Flutter slot widgets",
                        modelName + " — clear all " + actual.size()
                        + " widgets from " + exactSlot,
                        clear.ownerId());
            }
        };
    }

    private static RevisionSlot revisionSlot(
            DesignerDocument document,
            WidgetCatalog catalog,
            StableId ownerId,
            SlotName slotName) {
        WidgetNode owner = findWidget(document.root(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException(
                "Slot owner '" + ownerId
                + "' does not exist in the current Designer revision."));
        WidgetDefinition ownerDefinition = catalog.find(owner.type())
                .orElseThrow(() -> new IllegalArgumentException(
                "Catalog has no definition for slot owner type '"
                + owner.type().value() + "'."));
        SlotDefinition definition = ownerDefinition.slot(slotName)
                .orElseThrow(() -> new IllegalArgumentException(
                "Catalog definition '" + owner.type().value()
                + "' has no slot '" + slotName.value() + "'."));
        WidgetSlot value = owner.slots().get(slotName);
        return new RevisionSlot(
                definition,
                value,
                value == null ? List.of() : directSlotChildren(value));
    }

    private static WidgetNode exactSingleChild(
            RevisionSlot slot,
            StableId expectedChildId,
            String exactSlot) {
        if (slot.definition().cardinality()
                        != dev.flutter.netbeans.designer.model.SlotCardinality.SINGLE
                || !(slot.value() instanceof WidgetSlot.SingleSlot single)
                || single.child().isEmpty()
                || !single.child().orElseThrow().id().equals(expectedChildId)) {
            List<StableId> actual = slot.children().stream()
                    .map(WidgetNode::id)
                    .toList();
            throw new IllegalArgumentException(
                    "Slot '" + exactSlot
                    + "' changed after the editor opened; expected single child '"
                    + expectedChildId + "' but found " + actual + ".");
        }
        return single.child().orElseThrow();
    }

    private static boolean containsWidget(WidgetNode root, StableId id) {
        if (root.id().equals(id)) {
            return true;
        }
        return root.slots().values().stream()
                .flatMap(slot -> directSlotChildren(slot).stream())
                .anyMatch(child -> containsWidget(child, id));
    }

    private void slotMutationCompleted(
            StableId selectionAfterApply,
            FlutterDesignerMutationController.MutationResult result) {
        slotWidgetSubmitting = false;
        if (result.outcome() == FlutterDesignerMutationController.Outcome.APPLIED) {
            pendingWidgetSelection = selectionAfterApply;
            selectWidgetAfterMutation(selectionAfterApply);
        }
        updateDeleteWidgetAction();
    }

    private void showSlotMutationFailure(
            FlutterDesignerMutationController controllerForEdit,
            long exactViewEpoch,
            FlutterWidgetSlotMutation intent,
            String reason) {
        if (!componentLifecycleOpen
                || !mutationListening
                || mutationController != controllerForEdit
                || mutationViewEpoch != exactViewEpoch) {
            return;
        }
        String target = modelName + " — widget " + intent.ownerId()
                + ", slot " + intent.slotName().value();
        showMutationResult(FlutterDesignerMutationController.MutationResult.failed(
                "Manage Flutter widget slot", target, reason));
    }

    private static Optional<WidgetNode> findWidget(
            WidgetNode root,
            StableId id) {
        if (root.id().equals(id)) {
            return Optional.of(root);
        }
        for (WidgetSlot slot : root.slots().values()) {
            for (WidgetNode child : directSlotChildren(slot)) {
                Optional<WidgetNode> found = findWidget(child, id);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static List<WidgetNode> directSlotChildren(WidgetSlot slot) {
        return switch (slot) {
            case WidgetSlot.SingleSlot single -> single.child().stream().toList();
            case WidgetSlot.ListSlot list -> list.children();
        };
    }

    private record RevisionSlot(
            SlotDefinition definition,
            WidgetSlot value,
            List<WidgetNode> children) {
        private RevisionSlot {
            Objects.requireNonNull(definition, "definition");
            children = List.copyOf(children);
        }
    }

    private record SlotMutationPlan(
            DesignerCommand command,
            String operation,
            String target,
            StableId selectionAfterApply) {
        private SlotMutationPlan {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(selectionAfterApply, "selectionAfterApply");
        }
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

    private void applyInlineTextEditCommit(
            CanvasRunnerRuntimeEvent.TextEditCommit commit) {
        Objects.requireNonNull(commit, "commit");
        FlutterDesignerMutationController controllerForEdit = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (!java.awt.EventQueue.isDispatchThread()
                || inlineTextEditSubmitting
                || deleteWidgetSubmitting
                || moveWidgetSubmitting
                || slotWidgetSubmitting
                || !componentLifecycleOpen
                || !mutationUiEnabled.getAsBoolean()
                || !mutationListening
                || controllerForEdit == null
                || candidate == null
                || candidate.status()
                        != FlutterDesignerMutationController.Status.READY
                || candidate.token().isEmpty()
                || candidate.document().isEmpty()
                || candidate.catalog().isEmpty()
                || !currentCanvasMutationEnabled
                || candidate.document().orElseThrow() != currentCanvasDocument
                || candidate.catalog().orElseThrow() != currentCanvasCatalog
                || candidate.document().orElseThrow() != presentedCanvasDocument
                || candidate.catalog().orElseThrow() != presentedCanvasCatalog
                || selectedWidgetId().filter(commit.widgetId()::equals)
                        .isEmpty()) {
            return;
        }

        WidgetNode widget = findWidget(
                        candidate.document().orElseThrow().root(),
                        commit.widgetId())
                .orElse(null);
        if (widget == null || !widget.type().equals(TEXT_WIDGET_TYPE)) {
            return;
        }
        PropertyValue current = widget.properties().get(TEXT_DATA_PROPERTY);
        if (!(current instanceof PropertyValue.StringValue stringValue)) {
            return;
        }
        if (stringValue.value().equals(commit.text())) {
            return;
        }

        inlineTextEditSubmitting = true;
        updateDeleteWidgetAction();
        String target = modelName + " — widget " + commit.widgetId()
                + ", property " + TEXT_DATA_PROPERTY.value();
        try {
            submitDesignerMutation(
                    controllerForEdit,
                    candidate.token().orElseThrow(),
                    new SetProperty(
                            commit.widgetId(),
                            TEXT_DATA_PROPERTY,
                            new PropertyValue.StringValue(commit.text())),
                    "Edit Flutter text on Canvas",
                    target,
                    result -> inlineTextEditCompleted(commit.widgetId(), result));
        } catch (RuntimeException failure) {
            inlineTextEditSubmitting = false;
            updateDeleteWidgetAction();
            showMutationResult(FlutterDesignerMutationController.MutationResult.failed(
                    "Edit Flutter text on Canvas",
                    target,
                    failureReason(failure)));
        }
    }

    private void inlineTextEditCompleted(
            StableId widgetId,
            FlutterDesignerMutationController.MutationResult result) {
        inlineTextEditSubmitting = false;
        if (result.outcome() == FlutterDesignerMutationController.Outcome.APPLIED) {
            selectWidgetAfterMutation(widgetId);
        }
        updateDeleteWidgetAction();
    }

    private Optional<DeleteWidgetAdmission> deleteWidgetAdmission() {
        FlutterDesignerMutationController controllerForDelete = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (deleteWidgetSubmitting
                || moveWidgetSubmitting
                || slotWidgetSubmitting
                || inlineTextEditSubmitting
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
                    "Select Flutter widget after mutation failed.",
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
        if (!ExplorerManager.PROP_SELECTED_NODES.equals(event.getPropertyName())) {
            return;
        }
        propertiesTabController.selectionChanged(
                selectedWidgetId().orElse(null));
        if (synchronizingSelection) {
            return;
        }
        updateDeleteWidgetAction();
        if (canvasOwner != null) {
            selectedWidgetId().ifPresent(canvasOwner::selectWidget);
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
        if (!isPaletteCatalogInsertDragAuthorityEnabled()
                || canvasOwner == null
                || !canvasOwner.paletteCatalogInsertDropAvailable()
                || lastCanvasStatus == null
                || lastCanvasStatus.stage()
                != FlutterDesignerNativeCanvasStatus.Stage.RUNNING) {
            return false;
        }
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        return document == presentedCanvasDocument
                && catalog == presentedCanvasCatalog;
    }

    private boolean authorizeNativeCanvasPaletteDragSource(
            String token,
            WidgetDefinition definition) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(definition, "definition");
        if (!java.awt.EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Palette drag source must be authorized on the event-dispatch thread.");
        }
        FlutterDesignerCanvasSession session = canvasOwner;
        return session != null
                && isPaletteCatalogInsertDragEnabled()
                && session.authorizePaletteDragSource(
                        token, definition.typeId());
    }

    private boolean isPaletteCatalogInsertDragAuthorityEnabled() {
        if (!paletteCatalogInsertDndEnabled.getAsBoolean()
                || !mutationUiEnabled.getAsBoolean()
                || slotWidgetSubmitting
                || moveWidgetSubmitting
                || deleteWidgetSubmitting
                || inlineTextEditSubmitting
                || !designVisible
                || !paletteDragLifecycle.isInstalled()) {
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
                && catalog.paletteDefinitions().stream()
                        .anyMatch(definition -> BuiltInWidgetCapabilityCatalog.supports(
                                definition, WidgetCapability.DND));
    }

    private Optional<WidgetTypeId> consumePaletteDropToken(String token) {
        return paletteDragLifecycle.consume(token)
                .filter(widgetType -> {
                    FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
                    if (candidate == null || candidate.catalog().isEmpty()) {
                        return false;
                    }
                    return candidate.catalog().orElseThrow().find(widgetType)
                            .filter(definition -> BuiltInWidgetCapabilityCatalog.supports(
                                    definition, WidgetCapability.DND))
                            .isPresent();
                });
    }

    private FlutterDesignerWidgetTreeDropSupport.Preview<
            FlutterDesignerPaletteTreeDropAdapter.PreparedDrop>
            previewWidgetTreePaletteDrop(
                    java.awt.datatransfer.Transferable transferable,
                    int action,
                    StableId targetId) {
        if (!isPaletteCatalogInsertDragAuthorityEnabled()) {
            return FlutterDesignerWidgetTreeDropSupport.Preview.rejected(
                    "Widget-tree drop is unavailable: the active Designer "
                    + "mutation revision or Palette drag authority is not ready.");
        }
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        FlutterDesignerPaletteTreeDropAdapter.PreviewResult result =
                paletteTreeDropAdapter.preview(
                        transferable, action, document, catalog, targetId);
        if (result instanceof FlutterDesignerPaletteTreeDropAdapter.Rejected rejected) {
            return FlutterDesignerWidgetTreeDropSupport.Preview.rejected(
                    rejected.reason());
        }
        FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared =
                (FlutterDesignerPaletteTreeDropAdapter.PreparedDrop) result;
        String widgetDisplayName = catalog.find(prepared.widgetType())
                .orElseThrow().palette().displayName();
        return FlutterDesignerWidgetTreeDropSupport.Preview.accepted(
                prepared,
                "Drop " + widgetDisplayName + " on widget " + targetId + "."
                + prepared.slotName().value() + " at index "
                + prepared.insertionIndex() + '.');
    }

    private FlutterDesignerWidgetTreeDropSupport.Decision
            commitWidgetTreePaletteDrop(
                    FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared,
                    java.awt.datatransfer.Transferable transferable,
                    int action,
                    StableId targetId) {
        if (!prepared.parentId().equals(targetId)) {
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget-tree drop target changed after preview; expected "
                    + prepared.parentId() + " but received " + targetId + '.');
        }
        if (!isPaletteCatalogInsertDragAuthorityEnabled()) {
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget-tree drop was not applied: the active Designer "
                    + "mutation revision or Palette drag authority is no longer ready.");
        }
        FlutterDesignerMutationController controllerForEdit = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        DesignerDocument document = candidate.document().orElseThrow();
        WidgetCatalog catalog = candidate.catalog().orElseThrow();
        FlutterDesignerPaletteTreeDropAdapter.CommitResult result =
                paletteTreeDropAdapter.commit(
                        prepared,
                        transferable,
                        action,
                        document,
                        catalog,
                        StableId::random);
        if (result instanceof FlutterDesignerPaletteTreeDropAdapter.Rejected rejected) {
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    rejected.reason());
        }
        FlutterDesignerPaletteTreeDropAdapter.Committed committed =
                (FlutterDesignerPaletteTreeDropAdapter.Committed) result;
        String widgetDisplayName = catalog.find(prepared.widgetType())
                .orElseThrow().palette().displayName();
        String target = modelName + " — add " + widgetDisplayName + " to widget "
                + committed.command().destination().parentId() + "."
                + committed.command().destination().slotName().value()
                + " at index " + committed.command().destination().index();
        submitDesignerMutation(
                controllerForEdit,
                candidate.token().orElseThrow(),
                committed.command(),
                "Add Flutter " + widgetDisplayName + " widget",
                target);
        return FlutterDesignerWidgetTreeDropSupport.Decision.accepted(
                "Adding " + widgetDisplayName + " to widget "
                + committed.command().destination().parentId() + "."
                + committed.command().destination().slotName().value()
                + " at index " + committed.command().destination().index() + '.');
    }

    private void renderWidgetTreeDropFeedback(
            FlutterDesignerWidgetTreeDropSupport.Feedback feedback) {
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(
                    () -> renderWidgetTreeDropFeedback(feedback));
            return;
        }
        String summary = feedback.accepted()
                ? feedback.committed()
                        ? "Widget drop accepted."
                        : "Widget drop available."
                : "Widget drop rejected.";
        statusLabel.setText(summary);
        statusLabel.setToolTipText(feedback.message());
        statusLabel.getAccessibleContext().setAccessibleDescription(
                feedback.message());
    }

    private final class WidgetTreePaletteDropAdmission
            implements FlutterDesignerWidgetTreeDropSupport.Admission<
                    FlutterDesignerPaletteTreeDropAdapter.PreparedDrop> {
        @Override
        public FlutterDesignerWidgetTreeDropSupport.Preview<
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop> preview(
                        java.awt.datatransfer.Transferable transferable,
                        int action,
                        StableId targetId) {
            return previewWidgetTreePaletteDrop(transferable, action, targetId);
        }

        @Override
        public FlutterDesignerWidgetTreeDropSupport.Decision commit(
                FlutterDesignerPaletteTreeDropAdapter.PreparedDrop prepared,
                java.awt.datatransfer.Transferable transferable,
                int action,
                StableId targetId) {
            return commitWidgetTreePaletteDrop(
                    prepared, transferable, action, targetId);
        }
    }

    private void nativeCanvasInteractionFromRunner() {
        // A strict runner.interaction event is admitted only for the current
        // session/revision/frame/layout and the latest host interaction fence.
        // It therefore supersedes only an older Swing claim, including clicks
        // in the margins outside the modeled viewport. Selection delivery is
        // not model-mutation authority; the admitted physical pointer-down is
        // bounded focus intent for this exact native presentation only.
        applyAdmittedNativeInteractionFocus(
                this::clearSwingFocusClaim,
                canvasOwner::requestFocus);
    }

    static void applyAdmittedNativeInteractionFocus(
            Runnable clearSwingClaim,
            Runnable requestNativeFocus) {
        Runnable clear = Objects.requireNonNull(
                clearSwingClaim, "clearSwingClaim");
        Runnable request = Objects.requireNonNull(
                requestNativeFocus, "requestNativeFocus");
        clear.run();
        request.run();
    }

    void renderInteractionBarrierState(
            FlutterDesignerCanvasSession.InteractionBarrierState state) {
        Objects.requireNonNull(state, "state");
        if (!java.awt.EventQueue.isDispatchThread()) {
            java.awt.EventQueue.invokeLater(
                    () -> renderInteractionBarrierState(state));
            return;
        }
        interactionBarrierTransitionEpoch++;
        nativeCanvasFocusBootstrapEpoch++;
        swingFocusRepairNativeRelease = null;
        boolean wasPending = interactionBarrierPending;
        switch (state.phase()) {
            case SYNCHRONIZING -> {
                interactionBarrierPending = true;
                // Canvas input is fail-closed on both sides of the protocol
                // while this acknowledgement is pending. Keep the bounded
                // Swing repair active so late native child activation cannot
                // steal an explicit Swing claim. The transition epoch fences an
                // already queued continuation when ACK or timeout arrives.
                String target = state.layoutKey()
                        .map(layout -> "layout " + layout)
                        .orElse("the current Canvas layout");
                renderCanvasInteractionStatus(
                        "Input sync…",
                        "Synchronizing Canvas input fence "
                        + state.fenceSequence() + " for " + target + ".");
                // A replacement layout can enter this phase without a new
                // Swing input event. Ensure its retained claim receives the
                // same bounded repair opportunity as the initial presentation.
                scheduleRetainedSwingFocusRepair();
            }
            case SYNCHRONIZED -> {
                interactionBarrierPending = false;
                // Fence every pending-era timer before opening a fresh delayed
                // ticket. A serial ACK-following Canvas interaction can then
                // cancel the retained claim before that ticket is delivered.
                // This also covers a late exact ACK after TIMED_OUT.
                stopSwingFocusRepairTimerPreservingAttempts();
                clearCanvasInteractionStatus();
                scheduleRetainedSwingFocusRepair();
            }
            case TIMED_OUT -> {
                interactionBarrierPending = false;
                String target = state.layoutKey()
                        .map(layout -> "layout " + layout)
                        .orElse("the current Canvas layout");
                renderCanvasInteractionStatus(
                        "Input sync timed out",
                        "Canvas input fence " + state.fenceSequence()
                        + " for " + target
                        + " was not acknowledged; Canvas input remains gated "
                        + "and Swing focus was retained.");
                if (wasPending) {
                    scheduleRetainedSwingFocusRepair();
                }
            }
            case INACTIVE -> {
                interactionBarrierPending = false;
                clearCanvasInteractionStatus();
            }
        }
    }

    private void scheduleRetainedSwingFocusRepair() {
        JComponent target = retainedSwingFocusTarget;
        if (swingFocusClaimedForActivation && target != null) {
            scheduleSwingFocusRepair(swingFocusRepairEpoch, target);
        }
    }

    private void renderCanvasInteractionStatus(String summary, String detail) {
        canvasInteractionStatusLabel.setText(summary);
        canvasInteractionStatusLabel.setToolTipText(detail);
        canvasInteractionStatusLabel.getAccessibleContext()
                .setAccessibleDescription(summary + " " + detail);
        canvasInteractionStatusLabel.setVisible(true);
    }

    private void clearCanvasInteractionStatus() {
        canvasInteractionStatusLabel.setText("");
        canvasInteractionStatusLabel.setToolTipText(null);
        canvasInteractionStatusLabel.getAccessibleContext()
                .setAccessibleDescription(
                        "Native Canvas input synchronization is idle.");
        canvasInteractionStatusLabel.setVisible(false);
    }

    private Optional<WidgetMoveSnapshot> widgetMoveSnapshot(StableId sourceId) {
        Objects.requireNonNull(sourceId, "sourceId");
        FlutterDesignerMutationController controllerForMove = mutationController;
        FlutterDesignerMutationController.Snapshot candidate = mutationSnapshot;
        if (!java.awt.EventQueue.isDispatchThread()
                || moveWidgetSubmitting
                || deleteWidgetSubmitting
                || slotWidgetSubmitting
                || inlineTextEditSubmitting
                || !componentLifecycleOpen
                || !mutationUiEnabled.getAsBoolean()
                || !mutationListening
                || controllerForMove == null
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

        DesignerDocument document = candidate.document().orElseThrow();
        Node sourceNode = widgetNodes.get(sourceId);
        if (sourceNode == null
                || !sourceId.equals(
                        sourceNode.getLookup().lookup(StableId.class))
                || document.root().id().equals(sourceId)) {
            return Optional.empty();
        }
        Node parentNode = sourceNode.getParentNode();
        StableId parentId = parentNode == null
                ? null : parentNode.getLookup().lookup(StableId.class);
        if (parentId == null || !widgetNodes.containsKey(parentId)) {
            return Optional.empty();
        }
        return Optional.of(new WidgetMoveSnapshot(
                controllerForMove,
                candidate.token().orElseThrow(),
                document,
                candidate.catalog().orElseThrow()));
    }

    private FlutterDesignerWidgetTreeDropSupport.Preview<MoveWidget>
            previewWidgetMove(
                    StableId sourceId,
                    FlutterDesignerWidgetTreeDropSupport.TreeDropTarget target) {
        WidgetMoveSnapshot snapshot = widgetMoveSnapshot(sourceId).orElse(null);
        if (snapshot == null) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Preview.rejected(
                    "Widget move is unavailable: the active Designer mutation "
                    + "revision is not ready or the root widget is selected.");
        }

        FlutterDesignerWidgetMovePlanner.Result result = widgetMovePlanner.plan(
                snapshot.document(),
                snapshot.catalog(),
                sourceId,
                widgetMoveTarget(target));
        if (result instanceof FlutterDesignerWidgetMovePlanner.Rejected rejected) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Preview.rejected(
                    rejected.reason());
        }

        MoveWidget command = ((FlutterDesignerWidgetMovePlanner.Accepted) result)
                .command();
        if (canvasOwner != null) {
            canvasOwner.showWidgetMovePreview(
                    sourceId, command.destination());
        }
        return FlutterDesignerWidgetTreeDropSupport.Preview.accepted(
                command, moveWidgetDescription(command));
    }

    private FlutterDesignerWidgetTreeDropSupport.Decision commitWidgetMove(
            MoveWidget prepared,
            StableId sourceId,
            FlutterDesignerWidgetTreeDropSupport.TreeDropTarget target) {
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(target, "target");
        if (!prepared.widgetId().equals(sourceId)) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget move source changed after preview; expected "
                    + prepared.widgetId() + " but received " + sourceId + '.');
        }

        WidgetMoveSnapshot snapshot = widgetMoveSnapshot(sourceId).orElse(null);
        if (snapshot == null) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget move was not applied: the active Designer mutation "
                    + "revision is no longer ready.");
        }
        FlutterDesignerWidgetMovePlanner.Result replanned = widgetMovePlanner.plan(
                snapshot.document(),
                snapshot.catalog(),
                sourceId,
                widgetMoveTarget(target));
        if (replanned instanceof FlutterDesignerWidgetMovePlanner.Rejected rejected) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    rejected.reason());
        }
        MoveWidget exact = ((FlutterDesignerWidgetMovePlanner.Accepted) replanned)
                .command();
        if (!exact.equals(prepared)) {
            clearWidgetMovePreview();
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget move target changed after preview; retry the drag "
                    + "against the current widget tree.");
        }

        moveWidgetSubmitting = true;
        updateDeleteWidgetAction();
        clearWidgetMovePreview();
        String targetDescription = modelName + " — move widget " + sourceId
                + " to " + exact.destination().parentId() + '.'
                + exact.destination().slotName().value() + " at index "
                + exact.destination().index();
        try {
            submitDesignerMutation(
                    snapshot.controller(),
                    snapshot.token(),
                    exact,
                    "Move Flutter widget",
                    targetDescription,
                    result -> widgetMoveCompleted(sourceId, result));
        } catch (RuntimeException failure) {
            moveWidgetSubmitting = false;
            updateDeleteWidgetAction();
            return FlutterDesignerWidgetTreeDropSupport.Decision.rejected(
                    "Widget move could not be queued for " + sourceId + ": "
                    + failureReason(failure) + '.');
        }
        return FlutterDesignerWidgetTreeDropSupport.Decision.accepted(
                "Moving widget " + sourceId + " to "
                + exact.destination().parentId() + '.'
                + exact.destination().slotName().value() + " at index "
                + exact.destination().index() + '.');
    }

    private void widgetMoveCompleted(
            StableId sourceId,
            FlutterDesignerMutationController.MutationResult result) {
        moveWidgetSubmitting = false;
        if (result.outcome() == FlutterDesignerMutationController.Outcome.APPLIED) {
            selectWidgetAfterMutation(sourceId);
        }
        updateDeleteWidgetAction();
    }

    private void clearWidgetMovePreview() {
        if (canvasOwner != null) {
            canvasOwner.clearWidgetMovePreview();
        }
    }

    private static FlutterDesignerWidgetMovePlanner.Target widgetMoveTarget(
            FlutterDesignerWidgetTreeDropSupport.TreeDropTarget target) {
        Objects.requireNonNull(target, "target");
        return target.insertion()
                ? new FlutterDesignerWidgetMovePlanner.Insert(
                        target.widgetId(), target.childIndex())
                : new FlutterDesignerWidgetMovePlanner.On(target.widgetId());
    }

    private static String moveWidgetDescription(MoveWidget command) {
        return "Move widget " + command.widgetId() + " to "
                + command.destination().parentId() + '.'
                + command.destination().slotName().value() + " at index "
                + command.destination().index() + '.';
    }

    private final class WidgetTreeMoveAdmission
            implements FlutterDesignerWidgetTreeDropSupport.MoveAdmission {
        @Override
        public boolean canStart(StableId sourceId) {
            return widgetMoveSnapshot(sourceId).isPresent();
        }

        @Override
        public FlutterDesignerWidgetTreeDropSupport.Preview<MoveWidget> preview(
                StableId sourceId,
                FlutterDesignerWidgetTreeDropSupport.TreeDropTarget target) {
            return previewWidgetMove(sourceId, target);
        }

        @Override
        public FlutterDesignerWidgetTreeDropSupport.Decision commit(
                MoveWidget prepared,
                StableId sourceId,
                FlutterDesignerWidgetTreeDropSupport.TreeDropTarget target) {
            return commitWidgetMove(prepared, sourceId, target);
        }

        @Override
        public void clearPreview() {
            clearWidgetMovePreview();
        }
    }

    private record WidgetMoveSnapshot(
            FlutterDesignerMutationController controller,
            FlutterDesignerMutationController.RevisionToken token,
            DesignerDocument document,
            WidgetCatalog catalog) {
        private WidgetMoveSnapshot {
            Objects.requireNonNull(controller, "controller");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(catalog, "catalog");
        }
    }

    private void applyAdmittedPaletteDrop(
            FlutterDesignerCanvasSession.AdmittedPaletteDrop admission) {
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
                        .filter(definition -> BuiltInWidgetCapabilityCatalog.supports(
                                definition, WidgetCapability.DND))
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
                .append("reviewed model is published to the isolated native Flutter Canvas. ")
                .append("Viewport preview, widget-tree selection, the capability-gated Palette ")
                .append("and Properties are enabled. Reviewed properties are writable when ")
                .append("exact mutation admission is ready; unsupported property slices remain ")
                .append("read-only. ");
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
        canvasRetryButton.getAccessibleContext().setAccessibleName(
                "Retry Native Flutter Canvas");
        canvasRetryButton.getAccessibleContext().setAccessibleDescription(
                "Starts one explicit fresh Canvas runner generation after a terminal failure.");
        canvasDetailsButton.getAccessibleContext().setAccessibleName(
                "Show Native Flutter Canvas details");
        canvasDetailsButton.getAccessibleContext().setAccessibleDescription(
                "Opens the complete scrollable and copyable Native Flutter Canvas status.");
        widgetTree.getAccessibleContext().setAccessibleName(
                "Flutter Designer widget tree");
        widgetTree.getAccessibleContext().setAccessibleDescription(
                "Selectable widget hierarchy for " + modelName
                + ". Capability-reviewed properties are writable when exact mutation "
                + "admission is ready; unsupported property slices remain read-only. "
                + "When the owning-view AWT drag "
                + "lifecycle and native Canvas drop capability are available, Canvas-supported "
                + "Palette widgets may be inserted into catalog-compatible empty single slots "
                + "or terminal list slots through the native Canvas.");
        previewModes.getAccessibleContext().setAccessibleName(
                "Flutter Canvas preview target");
        previewModes.getAccessibleContext().setAccessibleDescription(
                "Select an exact responsive viewport and adaptive platform target. "
                + "The concrete Canvas engine is supplied by the active native provider. "
                + "Web is rendered "
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
        boolean wasFrameRendered = canvasFrameRendered;
        canvasFrameRendered = state.rendered();
        if (shouldRearmSwingFocusRepairOnRenderedTransition(
                wasFrameRendered,
                canvasFrameRendered,
                swingFocusClaimedForActivation,
                retainedSwingFocusTarget)) {
            // A pre-render Swing interaction owns a fresh typed-rendered retry
            // window. Fence earlier continuations and reset the bounded budget
            // exactly once for this false-to-true transition.
            swingFocusRepairEpoch++;
            swingFocusRepairReleasedJvmAuthority = null;
            cancelSwingFocusRepairTimer();
        } else if (!canvasFrameRendered) {
            // A new presentation or terminal status invalidates the prior
            // frame's late-focus window. Keep the explicit Swing claim so the
            // next typed rendered confirmation can reconcile it afresh.
            cancelSwingFocusRepairTimer();
        }
        // Every status transition can replace or re-layout the native surface.
        // Tokens issued for the preceding pixels must never survive it.
        invalidatePaletteDragAuthority();
        if (state.stage() == FlutterDesignerNativeCanvasStatus.Stage.FAILED) {
            // A rejected asynchronous present must remain retryable on the
            // next controller publication, even if its model identity is unchanged.
            clearPresentedCanvasIdentity();
        }
        if (state.stage() != FlutterDesignerNativeCanvasStatus.Stage.RUNNING) {
            viewportControls.setControlsEnabled(false);
        }
        canvasStatusLabel.setText(state.summary());
        canvasStatusLabel.setToolTipText(state.detail());
        canvasStatusLabel.getAccessibleContext().setAccessibleDescription(
                state.summary() + " " + state.detail());
        boolean detailsAvailable = state.stage()
                == FlutterDesignerNativeCanvasStatus.Stage.FAILED
                || state.stage() == FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE;
        FlutterDesignerCanvasOwnerCoordinator.Phase ownerPhase =
                canvasOwnerCoordinator.phase();
        boolean transitionRetryAvailable =
                ownerPhase == FlutterDesignerCanvasOwnerCoordinator.Phase.POISONED
                || (ownerPhase == FlutterDesignerCanvasOwnerCoordinator.Phase.EMPTY
                        && canvasOwnerCoordinator.desiredBackend() != null);
        boolean runtimeRetryAvailable = state.stage()
                == FlutterDesignerNativeCanvasStatus.Stage.FAILED
                && canvasOwner != null
                && canvasOwner.canRestart();
        boolean retryAvailable = transitionRetryAvailable
                || runtimeRetryAvailable;
        canvasRetryButton.setVisible(retryAvailable);
        canvasRetryButton.setEnabled(retryAvailable);
        canvasRetryButton.setToolTipText(retryAvailable
                ? transitionRetryAvailable
                        ? "Retry the failed Canvas owner transition."
                        : "Start one fresh isolated Canvas runner after this failure."
                : null);
        canvasRetryButton.getAccessibleContext().setAccessibleDescription(
                retryAvailable
                        ? "Retry " + state.summary()
                        : "The current Canvas state has no explicit process retry available.");
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
        if (state.rendered()) {
            // The child FLUTTERVIEW HWND can receive native focus only after
            // attach or first-frame delivery. Reconcile that late native event
            // with the most recent explicit Swing interaction.
            repairClaimedSwingFocusIfRunnerFocused();
        }
    }

    private void retryNativeCanvas() {
        FlutterDesignerCanvasOwnerCoordinator.Phase ownerPhase =
                canvasOwnerCoordinator.phase();
        if (ownerPhase == FlutterDesignerCanvasOwnerCoordinator.Phase.POISONED
                || (ownerPhase == FlutterDesignerCanvasOwnerCoordinator.Phase.EMPTY
                        && canvasOwnerCoordinator.desiredBackend() != null)) {
            canvasRetryButton.setEnabled(false);
            canvasOwnerCoordinator.retryTransition();
            return;
        }
        FlutterDesignerCanvasSession session = canvasOwner;
        if (session == null || !session.canRestart()) {
            return;
        }
        canvasRetryButton.setEnabled(false);
        if (!session.restart()) {
            canvasRetryButton.setEnabled(session.canRestart());
        }
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

    private enum CanvasCloseGateState {
        OPEN,
        RETIRING,
        READY_SAVE,
        READY_DISCARD
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

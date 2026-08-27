package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.canvas.CanvasPreviewMode;
import dev.flutter.netbeans.designer.canvas.payload.CanvasModelPayloadCodec;
import dev.flutter.netbeans.designer.codec.FdCodecDiagnostic;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasHost;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerPreviewPlatforms.PreviewTarget;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPalette;
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
    private final Lookup context;
    private final Lookup effectiveLookup;
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
    private final PaletteController paletteController;
    private final BeanTreeView widgetTree;
    private final JComboBox<PreviewTarget> previewModes;
    private final FlutterProjectPlatformProvider projectPlatforms;
    private final ChangeListener projectPlatformListener =
            event -> projectPlatformsChanged();
    private final WindowsNativeCanvasHost nativeCanvasHost;
    private final FlutterDesignerNativeCanvasSession nativeCanvasSession;
    private final String modelName;
    private final String sourceName;
    private final Map<StableId, Node> widgetNodes = new LinkedHashMap<>();
    private FlutterDesignerDocumentState.Current currentCanvasState;
    private FlutterDesignerNativeCanvasStatus lastCanvasStatus;
    private boolean previewInitialized;
    private boolean synchronizingSelection;
    private boolean updatingPreviewModes;
    private boolean listening;
    private boolean platformListening;

    public FlutterDesignerMultiViewDesign(Lookup context) {
        this.context = context;
        FlutterDesignerDataObject dataObject = context.lookup(FlutterDesignerDataObject.class);
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
        paletteController = FlutterDesignerPalette.create(
                BuiltInWidgetCatalog.getDefault(),
                CanvasModelPayloadCodec::supports);
        effectiveLookup = new ProxyLookup(
                context,
                ExplorerUtils.createLookup(explorerManager, visual.getActionMap()),
                Lookups.singleton(paletteController));
        widgetTree = createWidgetTree();
        widgetTree.setRootVisible(true);
        widgetTree.setDefaultActionAllowed(false);
        widgetTree.setPopupAllowed(false);
        widgetTree.setMinimumSize(new java.awt.Dimension(180, 120));
        widgetTree.setPreferredSize(new java.awt.Dimension(240, 500));
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
                        this::selectWidgetFromCanvas);
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
        previewModes.setMaximumSize(new java.awt.Dimension(
                220, previewModes.getPreferredSize().height));
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
        if (projectPlatforms != null && !platformListening) {
            platformListening = true;
            projectPlatforms.addChangeListener(projectPlatformListener);
            projectPlatforms.refresh();
            refreshPreviewChoices(selectedPreviewTarget().orElse(null), null);
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
        if (projectPlatforms != null && platformListening) {
            platformListening = false;
            projectPlatforms.removeChangeListener(projectPlatformListener);
        }
        currentCanvasState = null;
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
        if (nativeCanvasSession != null) {
            nativeCanvasSession.show();
        }
    }

    @Override
    public void componentHidden() {
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

    private void render(FlutterDesignerDocumentState state) {
        progress.setVisible(state instanceof FlutterDesignerDocumentState.Loading);
        if (state instanceof FlutterDesignerDocumentState.Current current
                && canvasEligible(current)) {
            publishCanvas(current);
        } else {
            withdrawCanvas();
        }
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
                    + " is newer than supported version 1.");
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

    private void publishCanvas(FlutterDesignerDocumentState.Current current) {
        StableId retainedSelection = selectedWidgetId().orElse(null);
        currentCanvasState = current;
        if (!previewInitialized) {
            previewInitialized = true;
            CanvasPreviewMode initial =
                    dev.flutter.netbeans.designer.canvas.CanvasPreviewProfileResolver
                            .initialMode(current.decoded().document().canvas());
            refreshPreviewChoices(null, initial);
        }
        rebuildWidgetTree(
                current.decoded().document().root(),
                current.catalog(),
                retainedSelection);
        presentCurrentCanvas(current);
    }

    private void withdrawCanvas() {
        if (currentCanvasState == null && widgetNodes.isEmpty()) {
            return;
        }
        currentCanvasState = null;
        clearWidgetTree();
        if (nativeCanvasSession != null) {
            nativeCanvasSession.withdraw();
        }
    }

    private void previewModeChanged() {
        if (updatingPreviewModes) {
            return;
        }
        FlutterDesignerDocumentState.Current current = currentCanvasState;
        if (current != null) {
            presentCurrentCanvas(current);
        }
    }

    private Optional<PreviewTarget> selectedPreviewTarget() {
        Object selected = previewModes.getSelectedItem();
        return selected instanceof PreviewTarget target
                ? Optional.of(target)
                : Optional.empty();
    }

    private void presentCurrentCanvas(FlutterDesignerDocumentState.Current current) {
        if (nativeCanvasSession == null) {
            return;
        }
        Optional<PreviewTarget> selected = selectedPreviewTarget();
        if (selected.isEmpty()) {
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
        if (target.requiresBrowserBackend()) {
            nativeCanvasSession.withdraw();
            renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Flutter Web Canvas is unavailable.",
                    "Target: " + modelName + " — Web. Reason: Web is a separate "
                    + "browser-compiled Flutter runtime; the embedded browser Canvas "
                    + "backend is not implemented. The Windows Flutter engine is not "
                    + "used as a false Web substitute."));
            return;
        }
        nativeCanvasSession.present(
                current.decoded().document(),
                current.catalog(),
                target.mode(),
                target.targetPlatform());
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
            FlutterDesignerDocumentState.Current current = currentCanvasState;
            if (selectionChanged && current != null) {
                presentCurrentCanvas(current);
            }
        };
        if (java.awt.EventQueue.isDispatchThread()) {
            refresh.run();
        } else {
            java.awt.EventQueue.invokeLater(refresh);
        }
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
                    + "engine. Web requires a separate browser backend.";
        }
        previewModes.setToolTipText(description);
        previewModes.getAccessibleContext().setAccessibleDescription(description);
    }

    private void rebuildWidgetTree(
            WidgetNode root,
            WidgetCatalog catalog,
            StableId retainedSelection) {
        synchronizingSelection = true;
        try {
            widgetNodes.clear();
            Node rootNode = buildWidgetNode(root, catalog);
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
    }

    private Node buildWidgetNode(WidgetNode widget, WidgetCatalog catalog) {
        Children.Array children = new Children.Array();
        for (WidgetSlot slot : widget.slots().values()) {
            switch (slot) {
                case WidgetSlot.SingleSlot single -> single.child().ifPresent(
                        child -> children.add(new Node[]{buildWidgetNode(child, catalog)}));
                case WidgetSlot.ListSlot list -> {
                    for (WidgetNode child : list.children()) {
                        children.add(new Node[]{buildWidgetNode(child, catalog)});
                    }
                }
            }
        }
        var definition = catalog.find(widget.type()).orElseThrow(() ->
                new IllegalStateException(
                        "Validated Flutter widget type is absent from its catalog: "
                        + widget.type().value()));
        Node node = new FlutterWidgetPropertiesNode(children, widget, definition);
        widgetNodes.put(widget.id(), node);
        return node;
    }

    private Optional<StableId> selectedWidgetId() {
        Node[] selected = explorerManager.getSelectedNodes();
        return selected.length == 1
                ? Optional.ofNullable(selected[0].getLookup().lookup(StableId.class))
                : Optional.empty();
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
    }

    private void explorerSelectionChanged(PropertyChangeEvent event) {
        if (synchronizingSelection
                || !ExplorerManager.PROP_SELECTED_NODES.equals(event.getPropertyName())
                || nativeCanvasSession == null) {
            return;
        }
        selectedWidgetId().ifPresent(nativeCanvasSession::selectWidget);
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
                    "Designer ready.",
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    "The on-disk imports and build regions, the SHA-256 values "
                    + "recorded in " + modelName + ", and the deterministic generated "
                    + "payloads agree. The validated CORE_V1 model is published to the "
                    + "isolated native Flutter Canvas. Viewport preview and read-only "
                    + "widget-tree selection, the six-item Palette and read-only "
                    + "Properties are enabled; drag-and-drop, property mutation "
                    + "and Designer commands remain disabled.");
        }
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
                "Read-only widget hierarchy for " + modelName + ".");
        previewModes.getAccessibleContext().setAccessibleName(
                "Flutter Canvas preview target");
        previewModes.getAccessibleContext().setAccessibleDescription(
                "Select an exact responsive viewport and adaptive platform target. "
                + "The concrete native Canvas engine remains Windows; Web requires "
                + "a separate browser backend.");
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

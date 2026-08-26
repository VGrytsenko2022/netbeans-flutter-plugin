package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.codec.FdCodecDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasHost;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/** Minimal design surface proving the NetBeans multiview integration. */
@MultiViewElement.Registration(
        mimeType = FlutterDesignerMime.MIME_TYPE,
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
        displayName = "Design",
        preferredID = "flutter.designer.design",
        position = 100)
public final class FlutterDesignerMultiViewDesign
        implements MultiViewElement, PropertyChangeListener {
    private final Lookup context;
    private final FlutterDesignerDocumentController controller;
    private final UndoRedo undoRedo;
    private final JPanel visual;
    private final JPanel statusPanel;
    private final JToolBar toolbar;
    private final JLabel statusLabel;
    private final JLabel modelLabel;
    private final JLabel detailLabel;
    private final JLabel canvasStatusLabel;
    private final JProgressBar progress;
    private final JProgressBar canvasProgress;
    private final WindowsNativeCanvasHost nativeCanvasHost;
    private final FlutterDesignerNativeCanvasSession nativeCanvasSession;
    private final String modelName;
    private final String sourceName;
    private boolean listening;

    public FlutterDesignerMultiViewDesign(Lookup context) {
        this.context = context;
        FlutterDesignerDataObject dataObject = context.lookup(FlutterDesignerDataObject.class);
        modelName = dataObject == null
                ? "the .fd model"
                : dataObject.getModelFile().getNameExt();
        controller = dataObject == null ? null : dataObject.getDocumentController();
        undoRedo = dataObject == null
                ? UndoRedo.NONE : dataObject.getCombinedUndoRedo();
        sourceName = dataObject == null
                ? "the paired Dart source"
                : dataObject.getPrimaryFile().getNameExt();
        visual = new JPanel(new BorderLayout());
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusLabel = centeredLabel("Preparing Flutter Designer model...");
        modelLabel = centeredLabel("Model: " + modelName + ". Source: " + sourceName + ".");
        detailLabel = centeredLabel("Waiting for bounded model validation.");
        canvasStatusLabel = centeredLabel("Native Canvas: waiting for the Design view.");
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
        statusPanel.add(statusLabel);
        statusPanel.add(modelLabel);
        statusPanel.add(detailLabel);
        statusPanel.add(progress);
        statusPanel.add(canvasStatusLabel);
        statusPanel.add(canvasProgress);
        WindowsNativeCanvasHost canvasHost = null;
        FlutterDesignerNativeCanvasSession canvasSession = null;
        if (isWindows()) {
            try {
                canvasHost = new WindowsNativeCanvasHost();
                canvasSession = FlutterDesignerNativeCanvasSession.createDefault(
                        canvasHost, this::renderNativeCanvasStatus);
            } catch (IOException | RuntimeException | LinkageError failure) {
                canvasHost = null;
                canvasSession = null;
                canvasStatusLabel.setText(
                        "Native Canvas unavailable: " + failureReason(failure));
                canvasStatusLabel.setToolTipText(canvasStatusLabel.getText());
            }
        } else {
            canvasStatusLabel.setText(
                    "Native Canvas: the current implementation is Windows-first; "
                    + "the platform host for this operating system is not available yet.");
        }
        nativeCanvasHost = canvasHost;
        nativeCanvasSession = canvasSession;
        if (nativeCanvasHost == null) {
            visual.add(statusPanel, BorderLayout.CENTER);
        } else {
            visual.add(nativeCanvasHost, BorderLayout.CENTER);
            visual.add(statusPanel, BorderLayout.SOUTH);
        }
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel(modelName + " — Design"));
        configureAccessibility();
    }

    private static JLabel centeredLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return label;
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
        return context;
    }

    @Override
    public void componentOpened() {
        if (controller != null && !listening) {
            listening = true;
            controller.addPropertyChangeListener(this);
            boolean firstView = controller.viewOpened();
            render(openingState(firstView, controller.state(), modelName));
        }
    }

    @Override
    public void componentClosed() {
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
                    "Flutter Designer on-disk three-way match verified.",
                    "Source " + sourceName + "; class " + className
                    + "; root widget " + rootType + ".",
                    "The on-disk imports and build regions, the SHA-256 values "
                    + "recorded in " + modelName + ", and the deterministic generated "
                    + "payloads agree. The native Flutter Canvas host is installed, "
                    + "but validated model publication, Palette, tree, properties, "
                    + "selection, drag-and-drop and Designer mutation remain disabled "
                    + "until their staged pair-aware workflows are complete.");
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

    private void renderNativeCanvasStatus(FlutterDesignerNativeCanvasStatus state) {
        canvasStatusLabel.setText(state.summary() + " " + state.detail());
        canvasStatusLabel.setToolTipText(state.detail());
        canvasStatusLabel.getAccessibleContext().setAccessibleDescription(
                canvasStatusLabel.getText());
        canvasProgress.setVisible(state.busy());
        canvasProgress.getAccessibleContext().setAccessibleDescription(state.detail());
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
}

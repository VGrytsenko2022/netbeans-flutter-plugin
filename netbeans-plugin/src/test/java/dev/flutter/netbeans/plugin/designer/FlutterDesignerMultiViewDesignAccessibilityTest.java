package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnosticCode;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnosticCode;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteItem;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectPlatformProvider;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
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
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.palette.PaletteController;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterDesignerMultiViewDesignAccessibilityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void registersStandardDeleteActionAndTreeKeyWithoutSelectionAuthority()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComponent visual = design.getVisualRepresentation();
            Action delete = visual.getActionMap().get(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY);
            assertNotNull(delete);
            assertEquals("Delete Flutter Widget", delete.getValue(Action.NAME));
            assertFalse(delete.isEnabled(),
                    "Delete must be disabled without one exact non-root selection");

            BeanTreeView tree = findByType(visual, BeanTreeView.class);
            KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            assertEquals(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY,
                    tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                            .get(deleteKey));
            assertSame(delete, tree.getActionMap().get(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY));

            delete.actionPerformed(new ActionEvent(
                    tree, ActionEvent.ACTION_PERFORMED, "delete"));
            assertFalse(delete.isEnabled(),
                    "Direct invocation must remain a no-op without selection");
        });
    }

    @Test
    void publishesExactCoreV1PaletteAndSelectedWidgetPropertiesInDesignLookup()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);

            PaletteController palette = design.getLookup().lookup(PaletteController.class);
            assertNotNull(palette);
            Node paletteRoot = palette.getRoot().lookup(Node.class);
            assertNotNull(paletteRoot);
            assertEquals(List.of(
                    "flutter.material.Scaffold",
                    "flutter.widgets.Column",
                    "flutter.widgets.Row",
                    "flutter.widgets.Padding",
                    "flutter.widgets.Center",
                    "flutter.widgets.Text"),
                    java.util.Arrays.stream(paletteRoot.getChildren().getNodes(true))
                            .flatMap(category -> java.util.Arrays.stream(
                                    category.getChildren().getNodes(true)))
                            .map(item -> item.getLookup()
                                    .lookup(FlutterDesignerPaletteItem.class)
                                    .typeId().value())
                            .toList());

            publish(design, currentState(List.of()));

            Node selected = design.getLookup().lookup(Node.class);
            assertNotNull(selected);
            assertNotNull(selected.getLookup().lookup(WidgetDefinition.class));
            assertEquals("flutter.widgets.SizedBox",
                    selected.getLookup().lookup(WidgetDefinition.class)
                            .typeId().value());
            Node.PropertySet properties = java.util.Arrays.stream(
                            selected.getPropertySets())
                    .filter(set -> FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME
                            .equals(set.getName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(List.of("width", "height"),
                    java.util.Arrays.stream(properties.getProperties())
                            .map(Node.Property::getName)
                            .toList());
            for (Node.Property<?> property : properties.getProperties()) {
                assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        property.getValue());
                assertFalse(property.canWrite());
            }
        });
    }

    @Test
    void excludesTheFileNodeFromTheDesignLookupAndPublishesOnlyTheWidgetNode()
            throws Exception {
        onEdt(() -> {
            record ContextMarker() { }
            Node fileNode = new AbstractNode(Children.LEAF);
            fileNode.setDisplayName("new_screen.dart");
            ContextMarker retainedContextValue = new ContextMarker();
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookups.fixed(
                            fileNode,
                            retainedContextValue));

            publish(design, currentState(List.of()));

            List<? extends Node> activeNodes = List.copyOf(
                    design.getLookup().lookupAll(Node.class));
            assertEquals(1, activeNodes.size());
            assertInstanceOf(FlutterWidgetPropertiesNode.class, activeNodes.get(0));
            assertFalse(activeNodes.contains(fileNode));
            assertNotNull(activeNodes.get(0).getLookup().lookup(WidgetDefinition.class));
            assertEquals("flutter.widgets.SizedBox",
                    activeNodes.get(0).getLookup()
                            .lookup(WidgetDefinition.class)
                            .typeId()
                            .value());
            assertSame(retainedContextValue,
                    design.getLookup().lookup(ContextMarker.class));
            assertNotNull(design.getLookup().lookup(PaletteController.class));
            assertTrue(java.util.Arrays.stream(activeNodes.get(0).getPropertySets())
                    .anyMatch(set -> set.getProperties().length > 0));
        });
    }

    @Test
    void retainsEligibleDurableCanvasReadOnlyAcrossEmptyMutationSnapshots()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY, () -> true);
            FlutterDesignerDocumentState.Current current = currentState(List.of());
            publish(design, current);

            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);

            renderMutationSnapshot(
                    design,
                    FlutterDesignerMutationController.Snapshot.waiting(
                            "Waiting for exact mutation admission."));
            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);

            renderMutationSnapshot(
                    design,
                    FlutterDesignerMutationController.Snapshot.unavailable(
                            FlutterDesignerMutationController.Status.BLOCKED,
                            "Prepare Flutter Designer Properties",
                            "sample.fd",
                            "Flutter/Dart SDK integration is unavailable."));
            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);
        });
    }

    @Test
    void keepsCanvasFailureInlineConciseAndExposesFullDetailsAction() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JLabel canvasStatus = findNamed(
                    visual, JLabel.class, "Native Flutter Canvas status");
            JButton details = findNamed(
                    visual, JButton.class, "Show Native Flutter Canvas details");
            String summary = "Prepare native Flutter Canvas launch failed.";
            String detail = "Target: embedded Windows FlutterView. Reason: "
                    + "the generated runner command produced a deliberately long "
                    + "diagnostic that must remain available without stretching or "
                    + "clipping the Design view status row.";

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    summary,
                    detail));

            assertEquals(summary, canvasStatus.getText());
            assertEquals(detail, canvasStatus.getToolTipText());
            assertEquals(summary + " " + detail,
                    canvasStatus.getAccessibleContext().getAccessibleDescription());
            assertTrue(details.isVisible());
            assertTrue(details.isEnabled());
            assertTrue(details.isFocusable());
            assertTrue(details.getAccessibleContext().getAccessibleDescription()
                    .contains(summary));

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    "Native Flutter Canvas is running.",
                    "Verified embedded FlutterView."));
            assertEquals("Native Flutter Canvas is running.", canvasStatus.getText());
            assertFalse(details.isVisible());
            assertFalse(details.isEnabled());

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: embedded FlutterView. Reason: no platform host."));
            assertEquals("Native Flutter Canvas is unavailable.",
                    canvasStatus.getText());
            assertTrue(details.isVisible());
            assertTrue(details.isEnabled());
        });
    }

    @Test
    void rendersValidatedSuccessAsOneCompactHorizontalStatusRow() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JPanel statusPanel = findNamed(
                    visual, JPanel.class, "Flutter Designer model status");
            JLabel status = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer status");
            JLabel model = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer model and source");
            JLabel detail = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer status details");
            JLabel canvasStatus = findNamed(
                    statusPanel, JLabel.class, "Native Flutter Canvas status");
            JButton canvasDetails = findNamed(
                    statusPanel, JButton.class, "Show Native Flutter Canvas details");

            publish(design, currentState(List.of()));
            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    "Native Flutter Canvas rendered.",
                    "Validated revision 7 is visible in the embedded FlutterView."));

            assertEquals("Designer ready.", status.getText());
            assertTrue(status.isVisible());
            assertTrue(canvasStatus.isVisible());
            assertFalse(isVisibleWithin(model, statusPanel),
                    "model/source diagnostics must not occupy a second banner row");
            assertFalse(isVisibleWithin(detail, statusPanel),
                    "success details must remain metadata rather than banner copy");
            assertFalse(canvasDetails.isVisible());
            assertFalse(canvasDetails.isEnabled());
            assertNotNull(status.getToolTipText());
            assertTrue(status.getToolTipText().contains("root widget"));
            assertTrue(status.getToolTipText().contains("on-disk imports and build regions"));
            assertTrue(statusPanel.getAccessibleContext().getAccessibleDescription()
                    .contains("on-disk imports and build regions"));

            int tallestSummary = Math.max(
                    status.getPreferredSize().height,
                    canvasStatus.getPreferredSize().height);
            int verticalInsets = statusPanel.getInsets().top
                    + statusPanel.getInsets().bottom;
            assertTrue(statusPanel.getPreferredSize().height
                            <= tallestSummary + verticalInsets + 4,
                    "normal success must retain status-bar height instead of a banner");

            statusPanel.setSize(1200, statusPanel.getPreferredSize().height);
            layoutRecursively(statusPanel);
            int statusCenterY = SwingUtilities.convertPoint(
                    status, 0, status.getHeight() / 2, statusPanel).y;
            int canvasCenterY = SwingUtilities.convertPoint(
                    canvasStatus, 0, canvasStatus.getHeight() / 2, statusPanel).y;
            assertTrue(Math.abs(statusCenterY - canvasCenterY) <= 2,
                    "model and Canvas summaries must share one horizontal row");
        });
    }

    @Test
    void keepsModelFailureConciseWhileRetainingTargetAndReasonAsMetadata()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JPanel statusPanel = findNamed(
                    visual, JPanel.class, "Flutter Designer model status");
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JLabel model = findNamed(
                    visual, JLabel.class, "Flutter Designer model and source");
            JLabel detail = findNamed(
                    visual, JLabel.class, "Flutter Designer status details");

            publish(design, new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    "sample.fd",
                    "The file is no longer available"));

            assertEquals("Read Flutter Designer model failed.", status.getText());
            assertTrue(status.isVisible());
            assertFalse(isVisibleWithin(model, statusPanel));
            assertFalse(isVisibleWithin(detail, statusPanel));
            assertEquals(
                    "Target: sample.fd. Reason: The file is no longer available",
                    status.getToolTipText());
            assertEquals(
                    "Read Flutter Designer model failed. " + status.getToolTipText(),
                    statusPanel.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void createsScrollableSelectableReadOnlyCanvasDetails() throws Exception {
        onEdt(() -> {
            FlutterDesignerNativeCanvasStatus state =
                    new FlutterDesignerNativeCanvasStatus(
                            FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                            "Native Flutter Canvas is unavailable.",
                            "Target: embedded Windows FlutterView. Reason: Flutter SDK "
                            + "could not be resolved from the configured toolchain.");
            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createNativeCanvasDetailsComponent(
                            state, "home.fd", "home.dart");
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Native Flutter Canvas is unavailable.\n\n"
                    + "Model: home.fd\n"
                    + "Source: home.dart\n\n"
                    + "Target: embedded Windows FlutterView. Reason: Flutter SDK "
                    + "could not be resolved from the configured toolchain.",
                    details.getText());
            assertEquals("Native Flutter Canvas full status details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Native Flutter Canvas details",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void createsCompleteScrollablePropertyMutationResultDetails() throws Exception {
        onEdt(() -> {
            FlutterDesignerMutationController.MutationResult result =
                    FlutterDesignerMutationController.MutationResult.rejected(
                            "Set Flutter property",
                            "home.fd — widget 35ca8ca5, property data",
                            "The selected Flutter Designer revision is stale; "
                            + "select the widget again.");

            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createMutationResultDetailsComponent(result);
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Operation: Set Flutter property\n"
                    + "Target: home.fd — widget 35ca8ca5, property data\n"
                    + "Reason: The selected Flutter Designer revision is stale; "
                    + "select the widget again.",
                    details.getText());
            assertEquals("Flutter Designer change details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer change result",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void namesResetPropertyMutationInCompleteScrollableResultDetails()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMutationController.MutationResult result =
                    FlutterDesignerMutationController.MutationResult.rejected(
                            "Reset Flutter property",
                            "home.fd — widget 35ca8ca5, property softWrap",
                            "The selected Flutter Designer revision is stale; "
                            + "select the widget again.");

            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createMutationResultDetailsComponent(result);
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Operation: Reset Flutter property\n"
                    + "Target: home.fd — widget 35ca8ca5, property softWrap\n"
                    + "Reason: The selected Flutter Designer revision is stale; "
                    + "select the widget again.",
                    details.getText());
            assertEquals("Flutter Designer change details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer change result",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void namesTheDesignSurfaceAndAssociatesStatusWithLoadingProgress() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");
            BeanTreeView widgetTree = findNamed(
                    visual,
                    BeanTreeView.class,
                    "Flutter Designer widget tree");
            JComboBox<?> previewMode = findNamed(
                    toolbar,
                    JComboBox.class,
                    "Flutter Canvas preview target");

            assertEquals("Flutter Designer design view",
                    visual.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer toolbar",
                    toolbar.getAccessibleContext().getAccessibleName());
            assertSame(progress, status.getLabelFor());
            assertFalse(progress.isVisible());
            assertNotNull(progress.getAccessibleContext().getAccessibleDescription());
            assertTrue(widgetTree.getAccessibleContext().getAccessibleDescription()
                    .contains("Column, Row, Padding, Center and Text widgets are writable"));
            assertEquals(8, previewMode.getItemCount());
            assertTrue(previewMode.getMaximumSize().width
                    >= previewMode.getPreferredSize().width);
            assertNotNull(previewMode.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void routesWebPreviewThroughTheNativeCanvasPresentationPipeline()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            try {
                JPanel visual = (JPanel) design.getVisualRepresentation();
                JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
                publish(design, currentState(List.of()));
                JComboBox<?> previews = findNamed(
                        toolbar,
                        JComboBox.class,
                        "Flutter Canvas preview target");
                int web = java.util.stream.IntStream
                        .range(0, previews.getItemCount())
                        .filter(index -> "Web — 1440×900".equals(
                                previews.getItemAt(index).toString()))
                        .findFirst()
                        .orElseThrow();
                previews.setSelectedIndex(web);

                JLabel canvasStatus = findNamed(
                        visual,
                        JLabel.class,
                        "Native Flutter Canvas status");
                assertEquals("Native Flutter Canvas is unavailable.",
                        canvasStatus.getText());
                assertTrue(canvasStatus.getToolTipText()
                        .contains("owning Flutter project is unavailable"));
                assertFalse(canvasStatus.getToolTipText()
                        .contains("Web Canvas is unavailable"));
            } finally {
                design.componentClosed();
            }
        });
    }

    @Test
    void synchronizesPreviewChoicesWithLiveFlutterProjectPlatforms() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("desktop_only"));
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve(".fd_templates"));
        Files.createDirectories(root.resolve("windows"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: desktop_only
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        Path dartPath = root.resolve("lib/home.dart");
        Path modelPath = root.resolve(".fd_templates/home.fd");
        Files.writeString(dartPath, "class Home {}\n", StandardCharsets.UTF_8);
        Files.writeString(modelPath, "{}\n", StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(root);
        FileUtil.refreshFor(root.toFile());
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dart);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject.dataObject(
                dart, project);
        FlutterProjectPlatformProvider platforms = project.getLookup()
                .lookup(FlutterProjectPlatformProvider.class);
        assertNotNull(platforms);
        var start = FlutterProjectPlatformProvider.class.getDeclaredMethod("start");
        start.setAccessible(true);
        start.invoke(platforms);

        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        AtomicReference<JComboBox<?>> previewsRef = new AtomicReference<>();
        try {
            onEdt(() -> {
                FlutterDesignerMultiViewDesign design =
                        new FlutterDesignerMultiViewDesign(dataObject.getLookup());
                designRef.set(design);
                JComboBox<?> previews = findNamed(
                        (JToolBar) design.getToolbarRepresentation(),
                        JComboBox.class,
                        "Flutter Canvas preview target");
                previewsRef.set(previews);
                assertPreviewChoices(
                        previews,
                        List.of("Windows Desktop — 1280×800"),
                        "Windows Desktop — 1280×800");
                assertTrue(previews.getAccessibleContext().getAccessibleDescription()
                        .contains("Desktop"));
                assertFalse(previews.getAccessibleContext().getAccessibleDescription()
                        .contains("Mobile,"));
                design.componentOpened();
            });

            project.getProjectDirectory().createFolder("android");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "Android Tablet — 800×1280",
                            "Windows Desktop — 1280×800"),
                    "Windows Desktop — 1280×800"));

            project.getProjectDirectory().createFolder("ios");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "iPhone — 390×844",
                            "Android Tablet — 800×1280",
                            "iPad — 800×1280",
                            "Windows Desktop — 1280×800"),
                    "Windows Desktop — 1280×800"));

            project.getProjectDirectory().getFileObject("windows").delete();
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "iPhone — 390×844",
                            "Android Tablet — 800×1280",
                            "iPad — 800×1280"),
                    "Android Phone — 390×844"));

            project.getProjectDirectory().getFileObject("android").delete();
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of("iPhone — 390×844", "iPad — 800×1280"),
                    "iPhone — 390×844"));

            onEdt(() -> designRef.get().componentClosed());
            project.getProjectDirectory().createFolder("web");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of("iPhone — 390×844", "iPad — 800×1280"),
                    "iPhone — 390×844"));
        } finally {
            FlutterDesignerMultiViewDesign design = designRef.get();
            if (design != null) {
                onEdt(() -> design.componentClosed());
            }
            platforms.close();
        }
    }

    @Test
    void longValidatedModelStatusDoesNotLockTheWidgetTreeDivider() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JSplitPane split = findByType(visual, JSplitPane.class);

            publish(design, currentState(List.of()));
            split.setSize(1200, 700);
            split.doLayout();

            assertTrue(split.isContinuousLayout(),
                    "native resize must remain live while the divider is dragged");
            assertTrue(split.getMaximumDividerLocation()
                            > split.getMinimumDividerLocation(),
                    "complete status copy must not consume the divider's resize range");
            split.setDividerLocation(400);
            split.doLayout();
            assertEquals(400, split.getDividerLocation());
        });
    }

    @Test
    void firstReopenedViewNeverPresentsARetainedCurrentState() throws Exception {
        FlutterDesignerDocumentState.Current retained = currentState(List.of());

        FlutterDesignerDocumentState first = FlutterDesignerMultiViewDesign.openingState(
                true, retained, "home_page.fd");
        FlutterDesignerDocumentState clone = FlutterDesignerMultiViewDesign.openingState(
                false, retained, "home_page.fd");

        FlutterDesignerDocumentState.Loading loading = assertInstanceOf(
                FlutterDesignerDocumentState.Loading.class, first);
        assertEquals("home_page.fd", loading.modelFileName());
        assertSame(retained, clone);
    }

    @Test
    void refreshesAccessibleDescriptionsWithEveryRenderedState() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");

            publish(design, new FlutterDesignerDocumentState.Loading("sample.fd"));

            String loadingDetail =
                    "Reading bounded UTF-8 .fd and paired Dart snapshots, validating "
                    + "the model and source structure, generating both managed payloads "
                    + "in memory, and comparing actual, declared and generated SHA-256 "
                    + "values. No files are written.";
            assertTrue(progress.isVisible());
            assertEquals(loadingDetail,
                    progress.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Loading Flutter Designer model... Model: the .fd model. "
                    + "Source: the paired Dart source. "
                    + loadingDetail,
                    visual.getAccessibleContext().getAccessibleDescription());

            publish(design, new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    "sample.fd",
                    "The file is no longer available"));

            assertFalse(progress.isVisible());
            assertEquals(
                    "Read Flutter Designer model failed. Target: sample.fd. "
                    + "Reason: The file is no longer available",
                    visual.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Reason: The file is no longer available",
                    progress.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void exposesConcreteAccessibleReadOnlyAndInputFailureStates() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JLabel detail = findNamed(
                    visual, JLabel.class, "Flutter Designer status details");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");
            FdDocumentCodec codec = new FdDocumentCodec();
            FlutterDesignerDocumentState.Current current = currentState(List.of());
            publish(design, current);
            assertEquals("Designer ready.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "The on-disk imports and build regions, the SHA-256 values "
                    + "recorded in the .fd model, and the deterministic generated "
                    + "payloads agree. The validated CORE_V1 model is published to the "
                    + "isolated native Flutter Canvas. Viewport preview and widget-tree "
                    + "selection, the six-item Palette and Properties are enabled. "
                    + "Supported properties on Column, Row, Padding, Center and Text are "
                    + "writable when exact mutation admission is ready; Scaffold properties "
                    + "remain read-only. Palette widget drag-and-drop is unavailable because "
                    + "exact mutation admission, the current rendered presentation, the "
                    + "owning-view AWT drag lifecycle, or the native Canvas drop capability "
                    + "is not ready; non-insertion drag-and-drop commands remain disabled.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.empty()));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "No bounded deterministic Dart-generation comparison is "
                    + "available for this loaded model.",
                    detail.getAccessibleContext().getAccessibleDescription());

            FdDecodeResult.Current driftDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("flutter.widgets.SizedBox", "flutter.widgets.Center")
                            .getBytes(StandardCharsets.UTF_8)));
            var driftGeneration = new DartRegionGenerator().generate(
                    driftDecoded.document(), BuiltInWidgetCatalog.getDefault());
            var driftThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    driftDecoded.document().source(),
                    driftGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    driftDecoded,
                    driftGeneration.modelValidation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(driftThreeWay)));
            assertEquals("Flutter Designer generated Dart conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("GENERATED_REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));

            DartGenerationResult unsupportedGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.DART_EXPRESSION_UNSUPPORTED,
                    "/root/properties/value",
                    "Opaque Dart expressions are not generated.");
            var unsupportedThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unsupportedGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unsupportedThreeWay)));
            assertEquals("Flutter Designer Dart generation is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("DART_EXPRESSION_UNSUPPORTED at /root/properties/value"));

            DartGenerationResult unavailableGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                    "/source/managedRegions",
                    "Generated payload exceeds the configured limit.");
            var unavailableThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unavailableGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unavailableThreeWay)));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("OUTPUT_SIZE_LIMIT at /source/managedRegions"));

            DartSourceIntegrityResult conflict = new DartSourceIntegrityScanner().scan(
                    validDartSource().replace("const SizedBox", "const Text")
                            .getBytes(StandardCharsets.UTF_8),
                    current.decoded().document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(conflict),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));
            assertFalse(progress.isVisible());

            FdDecodeResult.Current statefulDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("\"stateless\"", "\"stateful\"")
                            .getBytes(StandardCharsets.UTF_8)));
            DartSourceIntegrityResult mixedUnsupported =
                    new DartSourceIntegrityScanner().scan(
                            validDartSource()
                                    .replace("StatelessWidget", "StatefulWidget")
                                    .replace("const SizedBox", "const Text")
                                    .getBytes(StandardCharsets.UTF_8),
                            statefulDecoded.document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    statefulDecoded,
                    new ValidationResult(List.of()),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(mixedUnsupported),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source shape is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("STATEFUL_SOURCE_BINDING_UNSUPPORTED at /source/widgetKind"));
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("Additional source diagnostics: REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"),
                    "known conflicts must remain visible behind the primary unsupported cause");

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(DartSourceIntegrityResult.readFailure("Access denied")),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("SOURCE_READ_FAILED at /source/dartFile: "
                            + "The paired Dart source could not be read: Access denied"));

            CatalogDiagnostic catalogDiagnostic = new CatalogDiagnostic(
                    CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                    "com.example.widgets",
                    List.of("com.example"),
                    "The widget contributor could not be loaded.");
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(catalogDiagnostic),
                    List.of(),
                    current.sourceIntegrity(),
                    current.threeWayIntegrity()));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "INVALID_CONTRIBUTOR for com.example.widgets: "
                    + "The widget contributor could not be loaded.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.UnsupportedNewer future = assertInstanceOf(
                    FdDecodeResult.UnsupportedNewer.class,
                    codec.decode(("{\"format\":\"netbeans-flutter-designer\","
                            + "\"schemaVersion\":4}")
                            .getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.UnsupportedNewer(future));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("Schema version 4 is newer than supported version 3.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.Invalid invalid = assertInstanceOf(
                    FdDecodeResult.Invalid.class,
                    codec.decode("{".getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.Invalid(invalid));
            assertEquals("Cannot load Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains(invalid.diagnostics().get(0).code().toString()));
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.InputTooLarge(
                    "sample.fd", 257, 256));
            assertEquals("Cannot read Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("File size 257 bytes exceeds the 256 byte safety limit.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());
        });
    }

    private static String validDocument() {
        return """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "home_page.dart",
                    "className": "HomePage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {
                        "sha256": "%s"
                      },
                      "build": {
                        "sha256": "%s"
                      }
                    }
                  },
                  "root": {
                    "id": "35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce",
                    "type": "flutter.widgets.SizedBox",
                    "properties": {},
                    "slots": {}
                  }
                }
                """.formatted(
                DartManagedRegionHashing.normalizedSha256(importsPayload()),
                DartManagedRegionHashing.normalizedSha256(buildPayload()));
    }

    private static FlutterDesignerDocumentState.Current currentState(
            List<CatalogDiagnostic> catalogDiagnostics) throws Exception {
        FdDecodeResult.Current decodedCurrent = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(
                        validDocument().getBytes(StandardCharsets.UTF_8)));
        DartSourceIntegrityScanner sourceScanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult sourceIntegrity = sourceScanner.scan(
                validDartSource().getBytes(StandardCharsets.UTF_8),
                decodedCurrent.document().source());
        var generation = new DartRegionGenerator().generate(
                decodedCurrent.document(), BuiltInWidgetCatalog.getDefault());
        var threeWayIntegrity = new DartThreeWayIntegrityGate(sourceScanner).evaluate(
                sourceIntegrity, decodedCurrent.document().source(), generation);
        return new FlutterDesignerDocumentState.Current(
                decodedCurrent,
                new ValidationResult(List.of()),
                BuiltInWidgetCatalog.getDefault(),
                catalogDiagnostics,
                List.of(),
                Optional.of(sourceIntegrity),
                Optional.of(threeWayIntegrity));
    }

    private static String validDartSource() {
        return "// <netbeans-flutter-designer region=\"imports\">\n"
                + importsPayload()
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + buildPayload()
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
    }

    private static DartGenerationResult generationFailure(
            ValidationResult validation,
            DartGenerationDiagnosticCode code,
            String path,
            String message) {
        return new DartGenerationResult(
                validation,
                Optional.empty(),
                List.of(new DartGenerationDiagnostic(
                        code,
                        path,
                        Optional.empty(),
                        Optional.empty(),
                        message)));
    }

    private static String importsPayload() {
        return "import 'package:flutter/widgets.dart';\n";
    }

    private static String buildPayload() {
        return "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    return const SizedBox();\n"
                + "  }\n";
    }

    private static void publish(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerDocumentState state) {
        design.propertyChange(new PropertyChangeEvent(
                design,
                FlutterDesignerDocumentController.PROP_STATE,
                null,
                state));
    }

    private static void renderMutationSnapshot(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerMutationController.Snapshot snapshot)
            throws ReflectiveOperationException {
        var method = FlutterDesignerMultiViewDesign.class.getDeclaredMethod(
                "renderMutationSnapshot",
                FlutterDesignerMutationController.Snapshot.class);
        method.setAccessible(true);
        method.invoke(design, snapshot);
    }

    private static void assertSingleReadOnlyWidgetNode(
            FlutterDesignerMultiViewDesign design) throws Exception {
        List<? extends Node> nodes = List.copyOf(
                design.getLookup().lookupAll(Node.class));
        assertEquals(1, nodes.size());
        Node widget = assertInstanceOf(
                FlutterWidgetPropertiesNode.class, nodes.get(0));
        assertEquals("SizedBox", widget.getDisplayName());
        for (Node.PropertySet set : widget.getPropertySets()) {
            for (Node.Property<?> property : set.getProperties()) {
                assertFalse(property.canWrite(), property.getName());
            }
        }
    }

    private static void assertPreviewChoices(
            JComboBox<?> previews,
            List<String> expected,
            String selected) {
        assertEquals(expected,
                java.util.stream.IntStream.range(0, previews.getItemCount())
                        .mapToObj(index -> previews.getItemAt(index).toString())
                        .toList());
        assertNotNull(previews.getSelectedItem());
        assertEquals(selected, previews.getSelectedItem().toString());
    }

    private static <T extends Component> T findNamed(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName()
                + " named '" + accessibleName + "'");
    }

    private static <T extends Component> T findByType(
            Container root,
            Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findByTypeOrNull(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " found");
    }

    private static <T extends Component> T findByTypeOrNull(
            Container root,
            Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findByTypeOrNull(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void layoutRecursively(Container root) {
        root.doLayout();
        for (Component component : root.getComponents()) {
            if (component instanceof Container child) {
                layoutRecursively(child);
            }
        }
    }

    private static boolean isVisibleWithin(Component component, Container ancestor) {
        Component cursor = component;
        while (cursor != ancestor) {
            if (cursor == null || !cursor.isVisible()) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return ancestor.isVisible();
    }

    private static <T extends Component> T findNamedOrNull(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("EDT assertion failed", failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

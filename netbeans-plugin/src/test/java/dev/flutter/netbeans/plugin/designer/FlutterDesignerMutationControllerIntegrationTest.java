package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.dart.DartCandidateAnalysisOperation;
import dev.flutter.netbeans.dart.DartCandidateAnalysisRequest;
import dev.flutter.netbeans.dart.DartCandidateAnalysisResult;
import dev.flutter.netbeans.dart.DartCandidateAnalysisStatus;
import dev.flutter.netbeans.dart.DartNavigationTarget;
import dev.flutter.netbeans.dart.DartSymbolEvidence;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.ColoredBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DecoratedBoxWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.DirectionalityWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.ExcludeSemanticsWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.IndexedStackWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PlaceholderWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SingleChildScrollViewWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.SafeAreaWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommandRevision;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.RemoveWidget;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import dev.flutter.netbeans.plugin.designer.persistence.PairFileTransaction;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDropPlanner;
import dev.flutter.netbeans.plugin.designer.properties.FlutterPropertyCellValue;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetSlotMutation;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectSavePreflight;
import dev.flutter.netbeans.plugin.settings.FlutterSettings;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.cookies.SaveCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Task;

/** Exact DataObject-owned mutation path through analyzer and pair staging. */
class FlutterDesignerMutationControllerIntegrationTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId TEXT_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final StableId COLUMN_ID = StableId.parse(
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final StableId FIRST_ID = StableId.parse(
            "dddddddd-dddd-4ddd-8ddd-dddddddddddd");
    private static final StableId SECOND_ID = StableId.parse(
            "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee");
    private static final StableId SCAFFOLD_ID = StableId.parse(
            "11111111-1111-4111-8111-111111111111");
    private static final StableId CENTER_ID = StableId.parse(
            "22222222-2222-4222-8222-222222222222");
    private static final StableId NESTED_TEXT_ID = StableId.parse(
            "33333333-3333-4333-8333-333333333333");
    private static final StableId INDEXED_STACK_ID = StableId.parse(
            "44444444-4444-4444-8444-444444444441");
    private static final StableId INDEXED_FIRST_ID = StableId.parse(
            "44444444-4444-4444-8444-444444444442");
    private static final StableId INDEXED_SECOND_ID = StableId.parse(
            "44444444-4444-4444-8444-444444444443");
    private static final PropertyName DATA = new PropertyName("data");
    private static final PropertyName SOFT_WRAP = new PropertyName("softWrap");
    private static final PropertyName TEXT_ALIGN = new PropertyName("textAlign");
    private static final PropertyName SIZE = new PropertyName("size");
    private static final PropertyName TRANSFORM = new PropertyName("transform");
    private static final PropertyName ORIGIN = new PropertyName("origin");
    private static final PropertyName QUARTER_TURNS =
            new PropertyName("quarterTurns");
    private static final PropertyName MAIN_AXIS = new PropertyName("mainAxis");
    private static final PropertyName REVERSE = new PropertyName("reverse");
    private static final PropertyName SPACING = new PropertyName("spacing");
    private static final PropertyName OVERFLOW_DIRECTION =
            new PropertyName("overflowDirection");
    private static final PropertyName CROSS_AXIS_COUNT =
            new PropertyName("crossAxisCount");
    private static final PropertyName COLOR = new PropertyName("color");
    private static final PropertyName IS_ANTI_ALIAS =
            new PropertyName("isAntiAlias");
    private static final PropertyName SAFE_LEFT = new PropertyName("left");
    private static final PropertyName SAFE_MINIMUM = new PropertyName("minimum");
    private static final PropertyName MAINTAIN_BOTTOM_VIEW_PADDING =
            new PropertyName("maintainBottomViewPadding");
    private static final PropertyName FALLBACK_WIDTH =
            new PropertyName("fallbackWidth");
    private static final PropertyName TEXT_DIRECTION =
            new PropertyName("textDirection");
    private static final PropertyName DECORATION =
            new PropertyName("decoration");
    private static final PropertyName DECORATION_POSITION =
            new PropertyName("position");
    private static final PropertyName EXCLUDING = new PropertyName("excluding");
    private static final PropertyName INDEX = new PropertyName("index");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName BODY = new SlotName("body");
    private static final SlotName CHILD = new SlotName("child");

    @TempDir
    Path temporaryDirectory;

    @Test
    void unavailableToolchainKeepsValidatedCurrentAsReadOnlyPresentation()
            throws Exception {
        MutationFixture fixture = fixture("mutation_unavailable_toolchain");
        try (fixture) {
            FlutterDesignerMutationController.Snapshot initial = fixture.ready();
            assertEquals(FlutterDesignerMutationController.Status.READY,
                    initial.status());
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive());
            assertNull(sessionOwner(fixture.mutations()));

            Path unavailableFlutter = temporaryDirectory.resolve(
                    "missing-flutter-sdk");
            assertFalse(Files.exists(unavailableFlutter));
            fixture.settings().save(new FlutterToolchainConfig(
                    unavailableFlutter.toString(), true, ""));
            fixture.controller().reload();

            FlutterDesignerDocumentState.Current reloaded =
                    awaitCurrentAfter(fixture.controller(), fixture.current());
            assertTrue(reloaded.validation().valid());
            assertTrue(reloaded.threeWayIntegrity().orElseThrow()
                    .onDiskThreeWayMatch());
            FlutterDesignerMutationController.Snapshot blocked = awaitStatus(
                    fixture.mutations(),
                    FlutterDesignerMutationController.Status.BLOCKED);

            assertSame(reloaded.decoded().document(),
                    blocked.document().orElseThrow(),
                    "a blocked SDK must retain the exact validated document");
            assertSame(reloaded.catalog(), blocked.catalog().orElseThrow(),
                    "a blocked SDK must retain the exact validated catalog");
            assertTrue(blocked.token().isEmpty(),
                    "read-only presentation must not expose mutation authority");
            assertTrue(blocked.message().contains(
                    "Flutter/Dart SDK integration is unavailable"),
                    blocked::message);
            assertEquals(0, fixture.analysisCalls().get());
            assertNull(sessionOwner(fixture.mutations()),
                    "SDK resolution must not create a command owner");
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive(),
                    "SDK resolution must not claim semantic Undo authority");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertSame(reloaded, fixture.controller().state());
        }
    }

    @Test
    void unavailableToolchainUsesNewCurrentInsteadOfPriorUnsavedOwnerPresentation()
            throws Exception {
        MutationFixture fixture = fixture("mutation_unavailable_new_current");
        try (fixture) {
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("unsaved prior owner"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot unsaved =
                    awaitReadyWithData(
                            fixture.mutations(), "unsaved prior owner");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertNotNull(sessionOwner(fixture.mutations()));

            Path unavailableFlutter = temporaryDirectory.resolve(
                    "missing-flutter-sdk-after-owner");
            fixture.settings().save(new FlutterToolchainConfig(
                    unavailableFlutter.toString(), true, ""));
            fixture.controller().reload();

            FlutterDesignerDocumentState.Current reloaded =
                    awaitCurrentAfter(fixture.controller(), fixture.current());
            FlutterDesignerMutationController.Snapshot blocked = awaitStatus(
                    fixture.mutations(),
                    FlutterDesignerMutationController.Status.BLOCKED);

            assertSame(reloaded.decoded().document(),
                    blocked.document().orElseThrow(),
                    "an SDK failure for a new Current must show that exact Current");
            assertSame(reloaded.catalog(), blocked.catalog().orElseThrow());
            assertEquals("before", textData(blocked));
            assertFalse(Objects.equals(
                    textData(unsaved), textData(blocked)),
                    "the prior owner's unsaved revision is not authority for the new Current");
            assertTrue(blocked.token().isEmpty());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status(),
                    "read-only rebinding must not mutate the retained staged pair");
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void multiViewTextDataMutationRefreshesPropertiesTreeAndCanvasFromExactSnapshot()
            throws Exception {
        MutationFixture fixture = fixture("mutation_multiview_properties");
        CompletableFuture<DartCandidateAnalysisResult> analyzerGate =
                new CompletableFuture<>();
        CountDownLatch requestIssued = new CountDownLatch(1);
        AtomicInteger viewAnalysisCalls = new AtomicInteger();
        AtomicReference<DartCandidateAnalysisRequest> exactRequest =
                new AtomicReference<>();
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        viewAnalysisCalls.incrementAndGet();
                        exactRequest.set(request);
                        requestIssued.countDown();
                        return gatedAnalysis(analyzerGate);
                    });
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node baselineNode = awaitSelectedTextNode(
                        design, "before", true);
                onEdt(() -> {
                    assertTextProperties(baselineNode, "before", true);
                    assertEquals("before", textData(
                            design.currentCanvasDocumentForTests()));

                    Node.Property<FlutterPropertyCellValue> data =
                            textDataProperty(baselineNode);
                    data.setValue(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue("after")));
                    data.setValue(FlutterPropertyCellValue.explicit(
                            new PropertyValue.StringValue("must be ignored")));
                });

                assertTrue(requestIssued.await(10, TimeUnit.SECONDS),
                        "the Properties setter never reached the analyzer barrier");
                FlutterDesignerMutationController.Snapshot applying = awaitStatus(
                        fixture.mutations(),
                        FlutterDesignerMutationController.Status.APPLYING);
                assertEquals("before", textData(applying),
                        "an unverified candidate must not replace the presentation");
                Node applyingNode = awaitSelectedTextNode(
                        design, "before", false);
                onEdt(() -> {
                    assertSame(baselineNode, applyingNode,
                            "APPLYING must disable the stable Node without replacing it");
                    assertTextProperties(applyingNode, "before", false);
                    assertTrue(mutationProgress(design).isVisible(),
                            "APPLYING must be visible in the standard progress indicator");
                    assertEquals("before", textData(
                            design.currentCanvasDocumentForTests()),
                            "Canvas must retain the last confirmed snapshot while analysis runs");
                });
                assertEquals(1, viewAnalysisCalls.get(),
                        "one published Properties node may submit only once");
                DartCandidateAnalysisRequest request = exactRequest.get();
                assertNotNull(request);
                assertTrue(request.content().contains("after"), request::content);
                assertFalse(request.content().contains("must be ignored"),
                        request::content);

                analyzerGate.complete(passingAnalysis(
                        request, fixture.frameworkFile()));
                FlutterDesignerMutationController.Snapshot applied =
                        awaitReadyWithData(fixture.mutations(), "after");
                assertEquals(FlutterDesignerMutationController.Status.READY,
                        applied.status());
                Node appliedNode = awaitSelectedTextNode(
                        design, "after", true);
                onEdt(() -> {
                    assertSame(baselineNode, appliedNode,
                            "a property-only snapshot must refresh the selected node in place");
                    assertEquals(TEXT_ID,
                            appliedNode.getLookup().lookup(StableId.class),
                            "selection must remain anchored to the same StableId");
                    assertTextProperties(appliedNode, "after", true);
                    assertFalse(mutationProgress(design).isVisible());
                    assertEquals("after", textData(
                            design.currentCanvasDocumentForTests()),
                            "Canvas must consume the exact confirmed mutation snapshot");
                });
                assertEquals(1, viewAnalysisCalls.get(),
                        "the ignored duplicate must never start another analysis");
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
                analyzerGate.cancel(false);
            }
        }
    }

    @Test
    void propertyOnlyMutationPreservesSelectedNodeAndActivePropertySheetEditor()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_property_sheet_editor_continuity");
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        AtomicReference<PropertySheet> sheetRef = new AtomicReference<>();
        AtomicReference<JTable> tableRef = new AtomicReference<>();
        AtomicReference<Lookup.Result<Node>> selectionRef =
                new AtomicReference<>();
        AtomicReference<LookupListener> selectionListenerRef =
                new AtomicReference<>();
        AtomicReference<PropertyChangeListener> explorerListenerRef =
                new AtomicReference<>();
        AtomicInteger rootContextChanges = new AtomicInteger();
        AtomicInteger selectedNodeChanges = new AtomicInteger();
        AtomicReference<Component> editorRef = new AtomicReference<>();
        AtomicReference<Node.Property<?>> dataPropertyRef =
                new AtomicReference<>();
        AtomicReference<Node.Property<?>> softWrapPropertyRef =
                new AtomicReference<>();

        try (fixture) {
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node baselineNode = awaitSelectedTextSoftWrap(
                        design, null, true);
                onEdt(() -> {
                    PropertySheet sheet = new PropertySheet();
                    sheet.setDescriptionAreaVisible(false);
                    sheet.setSize(520, 900);
                    sheet.addNotify();

                    Lookup.Result<Node> selection =
                            design.getLookup().lookupResult(Node.class);
                    LookupListener listener = ignored ->
                            publishSelectedNodes(sheet, selection);
                    selection.addLookupListener(listener);
                    publishSelectedNodes(sheet, selection);

                    PropertyChangeListener explorerListener = event -> {
                        if (ExplorerManager.PROP_ROOT_CONTEXT.equals(
                                event.getPropertyName())) {
                            rootContextChanges.incrementAndGet();
                        } else if (ExplorerManager.PROP_SELECTED_NODES.equals(
                                event.getPropertyName())) {
                            selectedNodeChanges.incrementAndGet();
                        }
                    };
                    design.getExplorerManager().addPropertyChangeListener(
                            explorerListener);

                    sheetRef.set(sheet);
                    selectionRef.set(selection);
                    selectionListenerRef.set(listener);
                    explorerListenerRef.set(explorerListener);
                    JTable table = findFirst(sheet, JTable.class);
                    assertNotNull(table);
                    tableRef.set(table);
                });

                JTable table = tableRef.get();
                int dataRow = awaitPropertyRow(table, "data");
                awaitPropertySheetSetNodesSettled(
                        sheetRef.get(), baselineNode);
                onEdt(() -> {
                    table.changeSelection(dataRow, 1, false, false);
                    assertTrue(table.editCellAt(dataRow, 1),
                            "the ordinary text property must start in-place editing");
                    Component editor = table.getEditorComponent();
                    assertNotNull(editor);
                    editorRef.set(editor);
                    assertEquals(dataRow, table.getEditingRow());
                    assertEquals(dataRow, table.getSelectedRow());

                    Node.Property<?> data = findPropertyOrNull(
                            baselineNode, "data");
                    assertNotNull(data);
                    dataPropertyRef.set(data);
                    Node.Property<FlutterPropertyCellValue> softWrap =
                            cellProperty(baselineNode, "softWrap");
                    softWrapPropertyRef.set(softWrap);
                    softWrap.setValue(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)));
                });

                awaitReadyWithSoftWrap(fixture.mutations(), false);
                Node refreshedNode = awaitSelectedTextSoftWrap(
                        design, false, true);
                onEdt(() -> {
                    assertTrue(table.isEditing(),
                            () -> "publishing the confirmed property value must not cancel "
                            + "another editor; sameNode=" + (baselineNode == refreshedNode)
                            + ", rootContextChanges=" + rootContextChanges.get()
                            + ", selectedNodeChanges=" + selectedNodeChanges.get());
                    assertSame(editorRef.get(), table.getEditorComponent(),
                            "the active in-place editor component must be preserved");
                    assertEquals(dataRow, table.getEditingRow(),
                            "the active property row must be preserved");
                    assertEquals(dataRow, table.getSelectedRow(),
                            "row selection/focus must remain on the edited property");
                    assertSame(baselineNode, refreshedNode,
                            "a property-only mutation must refresh the existing selected Node");
                    assertSame(dataPropertyRef.get(),
                            findPropertyOrNull(refreshedNode, "data"),
                            "unrelated Property objects must retain identity");
                    assertSame(softWrapPropertyRef.get(),
                            findPropertyOrNull(refreshedNode, "softWrap"),
                            "the changed Property must refresh in place");
                    assertEquals(0, rootContextChanges.get(),
                            "a property-only mutation must not replace the Explorer root");
                    assertEquals(0, selectedNodeChanges.get(),
                            "a property-only mutation must not republish selected Nodes");
                });
            } finally {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design = designRef.get();
                    PropertyChangeListener explorerListener =
                            explorerListenerRef.get();
                    if (design != null && explorerListener != null) {
                        design.getExplorerManager().removePropertyChangeListener(
                                explorerListener);
                    }
                    Lookup.Result<Node> selection = selectionRef.get();
                    LookupListener listener = selectionListenerRef.get();
                    if (selection != null && listener != null) {
                        selection.removeLookupListener(listener);
                    }
                    JTable table = tableRef.get();
                    if (table != null && table.isEditing()) {
                        table.getCellEditor().cancelCellEditing();
                    }
                    PropertySheet sheet = sheetRef.get();
                    if (sheet != null) {
                        sheet.removeNotify();
                    }
                    if (design != null) {
                        design.componentClosed();
                    }
                });
            }
        }
    }

    @Test
    void savedBooleanRemainsEditableAfterLastViewCloseAndSameDataObjectReopen()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_saved_boolean_same_data_object_reopen");
        AtomicReference<FlutterDesignerMultiViewDesign> firstDesignRef =
                new AtomicReference<>();
        AtomicReference<FlutterDesignerMultiViewDesign> reopenedDesignRef =
                new AtomicReference<>();
        AtomicReference<PropertySheet> sheetRef = new AtomicReference<>();

        // fixture() primes the DataObject-owned controllers through one
        // synthetic view. Release it so the two Design components below model
        // a real last-view close followed by reopening the same DataObject.
        fixture.controller().viewClosed();
        try (fixture) {
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    firstDesignRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign firstDesign = firstDesignRef.get();
                Node firstNode = awaitSelectedTextSoftWrap(
                        firstDesign, null, true);
                Node.Property<FlutterPropertyCellValue> firstSoftWrap =
                        cellProperty(firstNode, "softWrap");
                onEdt(() -> firstSoftWrap.setValue(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(true))));

                FlutterDesignerMutationController.Snapshot changed =
                        awaitReadyWithSoftWrap(fixture.mutations(), true);
                PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
                assertNotNull(evidence);
                byte[] savedDart = evidence.candidateDartBytes();
                byte[] savedFd = evidence.preparedPairIdentity()
                        .prospectiveFdBytes();
                SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
                assertNotNull(save);
                save.save();
                awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
                FlutterDesignerMutationController.Snapshot savedReady =
                        awaitReadyWithSoftWrap(fixture.mutations(), true);
                awaitPairStatus(
                        fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
                FlutterDesignerDocumentState.Current savedCurrent =
                        assertInstanceOf(
                                FlutterDesignerDocumentState.Current.class,
                                fixture.controller().state());
                DesignerCommandSessionOrchestrator savedOwner =
                        assertInstanceOf(
                                DesignerCommandSessionOrchestrator.class,
                                sessionOwner(fixture.mutations()));
                DesignerCombinedUndoRedo combined =
                        fixture.dataObject().getCombinedUndoRedo();
                StyledDocument originalDocument = fixture.editor().getDocument();
                assertNotNull(originalDocument);

                onEdt(() -> {
                    firstDesign.componentClosed();
                    firstDesignRef.set(null);
                });

                AtomicBoolean editorCloseAccepted = new AtomicBoolean();
                onEdt(() -> editorCloseAccepted.set(fixture.editor().close()));
                assertTrue(editorCloseAccepted.get());
                onEdt(() -> { });
                if (fixture.editor().getDocument() != null) {
                    // The synthetic openDocument() fixture owns no editor pane,
                    // so complete the same last-pane callback explicitly.
                    onEdt(fixture.editor()::notifyClosed);
                }
                assertTrue(MutationFixture.awaitEditorDocumentClosed(
                        fixture.editor()));
                assertNull(fixture.editor().getDocument());
                assertSame(fixture.mutations(),
                        fixture.dataObject().mutationController());
                assertSame(savedOwner, sessionOwner(fixture.mutations()));
                assertTrue(combined.designerSessionActive());

                StyledDocument reopenedDocument = openGuardedSourceDocument(
                        fixture.editor());
                assertNotSame(originalDocument, reopenedDocument);
                assertArrayEquals(savedDart,
                        fixture.editor().liveSnapshot().markerBearingUtf8());

                CountDownLatch freshReloadPublished = new CountDownLatch(1);
                PropertyChangeListener reloadListener = event -> {
                    if (FlutterDesignerDocumentController.PROP_STATE.equals(
                            event.getPropertyName())
                            && event.getNewValue()
                            instanceof FlutterDesignerDocumentState.Current current
                            && current != savedCurrent) {
                        freshReloadPublished.countDown();
                    }
                };
                fixture.controller().addPropertyChangeListener(reloadListener);
                try {
                    onEdt(() -> {
                        FlutterDesignerMultiViewDesign reopened =
                                new FlutterDesignerMultiViewDesign(
                                        fixture.dataObject().getLookup());
                        reopenedDesignRef.set(reopened);
                        reopened.componentOpened();
                    });
                    assertTrue(freshReloadPublished.await(10, TimeUnit.SECONDS),
                            "reopen must verify a fresh durable pair snapshot");
                    awaitReadyWithSoftWrap(fixture.mutations(), true);
                    assertSame(savedCurrent, fixture.controller().state(),
                            "an exact reload must restore the retained command anchor identity");
                    assertSame(savedOwner, sessionOwner(fixture.mutations()));
                    assertTrue(combined.designerSessionActive());
                    assertNotSame(
                            savedReady.token().orElseThrow(),
                            fixture.mutations().snapshot().token().orElseThrow());
                } finally {
                    fixture.controller().removePropertyChangeListener(
                            reloadListener);
                }

                FlutterDesignerMultiViewDesign reopenedDesign =
                        reopenedDesignRef.get();
                Node reopenedNode = awaitSelectedTextSoftWrap(
                        reopenedDesign, true, true);
                Node.Property<FlutterPropertyCellValue> reopenedSoftWrap =
                        cellProperty(reopenedNode, "softWrap");
                assertTrue(reopenedSoftWrap.getPropertyEditor().isPaintable(),
                        "the saved explicit boolean must retain its checkbox renderer");
                assertNull(reopenedSoftWrap.getPropertyEditor().getTags(),
                        "the saved explicit boolean must not regress to a combo editor");

                AtomicReference<JTable> tableRef = new AtomicReference<>();
                onEdt(() -> {
                    PropertySheet sheet = new PropertySheet();
                    sheet.setDescriptionAreaVisible(false);
                    sheet.setSize(520, 900);
                    sheet.addNotify();
                    sheet.setNodes(new Node[]{reopenedNode});
                    sheetRef.set(sheet);
                    JTable table = findFirst(sheet, JTable.class);
                    assertNotNull(table);
                    tableRef.set(table);
                });
                JTable table = tableRef.get();
                int row = awaitPropertyRow(table, "softWrap");
                awaitPropertySheetSetNodesSettled(sheetRef.get(), reopenedNode);
                onEdt(() -> {
                    table.changeSelection(row, 1, false, false);
                    assertTrue(table.editCellAt(row, 1),
                            "the reopened saved boolean must start editing");
                    JCheckBox checkbox = assertInstanceOf(
                            JCheckBox.class, table.getEditorComponent());
                    assertTrue(checkbox.isSelected());
                    assertEquals(javax.swing.SwingConstants.CENTER,
                            checkbox.getHorizontalAlignment());
                    assertEquals(javax.swing.SwingConstants.CENTER,
                            checkbox.getVerticalAlignment());
                    var editor = table.getCellEditor();
                    checkbox.doClick();
                    assertFalse(checkbox.isSelected());
                    if (table.isEditing()) {
                        assertTrue(editor.stopCellEditing(),
                                "the reopened checkbox value must commit through "
                                + "the real PropertySheet editor");
                    }
                });

                FlutterDesignerMutationController.Snapshot editedAgain =
                        awaitReadyWithSoftWrap(fixture.mutations(), false);
                assertNotSame(changed.token().orElseThrow(),
                        editedAgain.token().orElseThrow(),
                        "editing after reopen must publish a new revision");
                awaitSelectedTextSoftWrap(reopenedDesign, false, true);
                assertTrue(combined.canUndo());
                onEdt(combined::undo);
                awaitReadyWithSoftWrap(fixture.mutations(), true);
                awaitSelectedTextSoftWrap(reopenedDesign, true, true);

                onEdt(() -> {
                    assertTrue(combined.canRedo());
                    combined.redo();
                });
                awaitReadyWithSoftWrap(fixture.mutations(), false);
                awaitSelectedTextSoftWrap(reopenedDesign, false, true);
            } finally {
                onEdt(() -> {
                    PropertySheet sheet = sheetRef.get();
                    if (sheet != null) {
                        JTable table = findFirst(sheet, JTable.class);
                        if (table != null && table.isEditing()) {
                            table.getCellEditor().cancelCellEditing();
                        }
                        sheet.removeNotify();
                    }
                    FlutterDesignerMultiViewDesign reopened =
                            reopenedDesignRef.getAndSet(null);
                    if (reopened != null) {
                        reopened.componentClosed();
                    }
                    FlutterDesignerMultiViewDesign first =
                            firstDesignRef.getAndSet(null);
                    if (first != null) {
                        first.componentClosed();
                    }
                });
            }
        }
    }

    @Test
    void savedSizedOverflowBoxSizeSurvivesReloadAndAcceptsFurtherEdit()
            throws Exception {
        PropertyValue.SizeValue initial = new PropertyValue.SizeValue(
                java.math.BigDecimal.valueOf(100),
                java.math.BigDecimal.valueOf(100));
        PropertyValue.SizeValue saved = new PropertyValue.SizeValue(
                new java.math.BigDecimal("120.5"),
                java.math.BigDecimal.valueOf(64));
        ExactPair durablePair;
        try (MutationFixture fixture = fixture(
                "mutation_saved_size_reload_further_edit",
                sizedOverflowBoxExactPair(initial))) {
            awaitReadyWithSize(fixture.mutations(), initial);
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.mutations().snapshot().token().orElseThrow(),
                            new SetProperty(TEXT_ID, SIZE, saved),
                            "SizedOverflowBox.size")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitReadyWithSize(fixture.mutations(), saved);

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] savedDart = evidence.candidateDartBytes();
            byte[] savedFd = evidence.preparedPairIdentity().prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
            awaitPairStatus(fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()));
            durablePair = new ExactPair(savedDart, savedFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_saved_size_reload_further_edit_reopened",
                durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened =
                    awaitReadyWithSize(fixture.mutations(), saved);

            PropertyValue.SizeValue editedAgain = new PropertyValue.SizeValue(
                    java.math.BigDecimal.valueOf(48),
                    java.math.BigDecimal.valueOf(32));
            FlutterDesignerMutationController.MutationResult second =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(TEXT_ID, SIZE, editedAgain),
                            "SizedOverflowBox.size")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    second.outcome(), second::reason);
            FlutterDesignerMutationController.Snapshot furtherEdit =
                    awaitReadyWithSize(fixture.mutations(), editedAgain);
            assertNotSame(reopened.token().orElseThrow(),
                    furtherEdit.token().orElseThrow(),
                    "editing Size after reload must publish a new revision");
        }
    }

    @Test
    void savedTransformOriginSurvivesReloadAndAcceptsFurtherSignedEdit()
            throws Exception {
        PropertyValue.OffsetValue initial = new PropertyValue.OffsetValue(
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO);
        PropertyValue.OffsetValue saved = new PropertyValue.OffsetValue(
                new java.math.BigDecimal("-12.5"),
                new java.math.BigDecimal("7.25"));
        ExactPair durablePair;
        try (MutationFixture fixture = fixture(
                "mutation_saved_transform_origin_reload_further_edit",
                transformExactPair(initial))) {
            awaitReadyWithOrigin(fixture.mutations(), initial);
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("Matrix4.fromList"));
                        assertFalse(request.symbolProbes().stream().anyMatch(
                                        probe -> probe.expectedSymbolName()
                                                .equals("Matrix4")),
                                "re-exported vector_math Matrix4 must not be trusted as "
                                + "a Flutter-SDK-owned navigation target");
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.mutations().snapshot().token().orElseThrow(),
                            new SetProperty(TEXT_ID, ORIGIN, saved),
                            "Transform.origin")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitReadyWithOrigin(fixture.mutations(), saved);

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] savedDart = evidence.candidateDartBytes();
            byte[] savedFd = evidence.preparedPairIdentity().prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
            awaitPairStatus(fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()));
            durablePair = new ExactPair(savedDart, savedFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_saved_transform_origin_reload_further_edit_reopened",
                durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened =
                    awaitReadyWithOrigin(fixture.mutations(), saved);

            PropertyValue.OffsetValue editedAgain =
                    new PropertyValue.OffsetValue(
                            java.math.BigDecimal.valueOf(4),
                            new java.math.BigDecimal("-8.5"));
            FlutterDesignerMutationController.MutationResult second =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(TEXT_ID, ORIGIN, editedAgain),
                            "Transform.origin")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    second.outcome(), second::reason);
            FlutterDesignerMutationController.Snapshot furtherEdit =
                    awaitReadyWithOrigin(fixture.mutations(), editedAgain);
            assertNotSame(reopened.token().orElseThrow(),
                    furtherEdit.token().orElseThrow(),
                    "editing Offset after reload must publish a new revision");
        }
    }

    @Test
    void externalPairEventPreventsRetainedCurrentReaffirmationAfterReopen()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_reopen_external_event_fence");
        try (fixture) {
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            new SetProperty(
                                    TEXT_ID,
                                    SOFT_WRAP,
                                    new PropertyValue.BooleanValue(true)),
                            "Text.softWrap")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitReadyWithSoftWrap(fixture.mutations(), true);

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] savedDart = evidence.candidateDartBytes();
            byte[] savedFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
            awaitReadyWithSoftWrap(fixture.mutations(), true);
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);

            FlutterDesignerDocumentState.Current retainedCurrent =
                    assertInstanceOf(
                            FlutterDesignerDocumentState.Current.class,
                            fixture.controller().state());
            Object retainedOwner = sessionOwner(fixture.mutations());
            assertNotNull(retainedOwner);
            PairSaveCoordinator.CloseRevision retainedPairRevision =
                    fixture.coordinator().closeRevision();

            fixture.controller().viewClosed();
            assertFalse(fixture.coordinator().handleFileEvent(
                    new FileEvent(fixture.dataObject().getModelFile())));
            PairSaveCoordinator.CloseRevision afterExternalEvent =
                    fixture.coordinator().closeRevision();
            assertFalse(retainedPairRevision.sameRevision(afterExternalEvent));
            assertEquals(retainedPairRevision.stateEpoch(),
                    afterExternalEvent.stateEpoch(),
                    "a clean event must be caught even without a state epoch change");

            CountDownLatch freshReloadPublished = new CountDownLatch(1);
            PropertyChangeListener reloadListener = event -> {
                if (FlutterDesignerDocumentController.PROP_STATE.equals(
                        event.getPropertyName())
                        && event.getNewValue()
                        instanceof FlutterDesignerDocumentState.Current current
                        && current != retainedCurrent) {
                    freshReloadPublished.countDown();
                }
            };
            fixture.controller().addPropertyChangeListener(reloadListener);
            try {
                assertTrue(fixture.controller().viewOpened());
                assertTrue(freshReloadPublished.await(10, TimeUnit.SECONDS));

                FlutterDesignerMutationController.Snapshot blocked = awaitStatus(
                        fixture.mutations(),
                        FlutterDesignerMutationController.Status.BLOCKED);
                assertNotSame(retainedCurrent, fixture.controller().state());
                assertTrue(blocked.token().isEmpty());
                assertTrue(blocked.message().contains(
                        "differs from retained Designer Undo/Redo history"),
                        blocked::message);
                assertSame(retainedOwner, sessionOwner(fixture.mutations()));
            } finally {
                fixture.controller().removePropertyChangeListener(
                        reloadListener);
            }
        }
    }

    @Test
    void externalPairEventDuringReaffirmationNeverPublishesRetainedCurrent()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_reopen_external_event_during_reaffirmation");
        AtomicInteger reaffirmationCalls = new AtomicInteger();
        AtomicBoolean eventSuppressed = new AtomicBoolean(true);
        AtomicReference<PairSaveCoordinator.CloseRevision> eventRevision =
                new AtomicReference<>();
        try (fixture) {
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            new SetProperty(
                                    TEXT_ID,
                                    SOFT_WRAP,
                                    new PropertyValue.BooleanValue(true)),
                            "Text.softWrap")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitReadyWithSoftWrap(fixture.mutations(), true);

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] savedDart = evidence.candidateDartBytes();
            byte[] savedFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
            awaitReadyWithSoftWrap(fixture.mutations(), true);
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);

            FlutterDesignerDocumentState.Current retainedCurrent =
                    assertInstanceOf(
                            FlutterDesignerDocumentState.Current.class,
                            fixture.controller().state());
            Object retainedOwner = sessionOwner(fixture.mutations());
            assertNotNull(retainedOwner);
            PairSaveCoordinator.CloseRevision retainedPairRevision =
                    fixture.coordinator().closeRevision();

            fixture.controller().viewClosed();

            AtomicBoolean retainedCurrentPublished = new AtomicBoolean();
            CountDownLatch freshReloadPublished = new CountDownLatch(1);
            PropertyChangeListener reloadListener = event -> {
                if (!FlutterDesignerDocumentController.PROP_STATE.equals(
                        event.getPropertyName())) {
                    return;
                }
                if (event.getNewValue() == retainedCurrent) {
                    retainedCurrentPublished.set(true);
                } else if (event.getNewValue()
                        instanceof FlutterDesignerDocumentState.Current) {
                    freshReloadPublished.countDown();
                }
            };
            fixture.controller().addPropertyChangeListener(reloadListener);
            fixture.mutations().setReaffirmationHookForTests(() -> {
                reaffirmationCalls.incrementAndGet();
                eventSuppressed.set(fixture.coordinator().handleFileEvent(
                        new FileEvent(fixture.dataObject().getModelFile())));
                eventRevision.set(fixture.coordinator().closeRevision());
            });
            try {
                assertTrue(fixture.controller().viewOpened());
                assertTrue(freshReloadPublished.await(10, TimeUnit.SECONDS),
                        "reaffirmation rejection must publish a fresh reload");

                FlutterDesignerMutationController.Snapshot blocked = awaitStatus(
                        fixture.mutations(),
                        FlutterDesignerMutationController.Status.BLOCKED);
                assertEquals(1, reaffirmationCalls.get(),
                        "the race must be injected after exactly one private adoption");
                assertFalse(eventSuppressed.get(),
                        "a clean external event must allow the mandatory reload");
                assertNotNull(eventRevision.get());
                assertFalse(retainedPairRevision.sameRevision(
                        eventRevision.get()));
                assertFalse(retainedCurrentPublished.get(),
                        "the privately adopted retained Current must never be "
                        + "published after the fence changes");
                assertNotSame(retainedCurrent, fixture.controller().state());
                assertTrue(blocked.token().isEmpty());
                assertTrue(blocked.message().contains(
                        "differs from retained Designer Undo/Redo history"),
                        blocked::message);
                assertSame(retainedOwner, sessionOwner(fixture.mutations()));
            } finally {
                fixture.mutations().setReaffirmationHookForTests(null);
                fixture.controller().removePropertyChangeListener(
                        reloadListener);
            }
        }
    }

    @Test
    void paletteTextInsertionUsesExactMutationAnalyzerPairSaveAndUndoPipeline()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_text_append", columnExactPair());
        StableId appendedId = StableId.parse(
                "ffffffff-ffff-4fff-8fff-ffffffffffff");

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            new WidgetTypeId("flutter.widgets.Text"),
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned);

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append Text to Column.children at index 2")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetSlot.ListSlot children = assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    applied.document().orElseThrow().root().slots().get(CHILDREN));
            assertEquals(3, children.children().size());
            WidgetNode appended = children.children().get(2);
            assertEquals(appendedId, appended.id());
            assertEquals(new WidgetTypeId("flutter.widgets.Text"), appended.type());
            assertEquals("Text", ((PropertyValue.StringValue)
                    appended.properties().get(DATA)).value());
            assertEquals(1, fixture.analysisCalls().get());
            String analyzedDart = fixture.analyzedContents().getFirst();
            assertTrue(analyzedDart.contains("Text("), analyzedDart);
            assertTrue(analyzedDart.contains("'Text'"), analyzedDart);
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence appliedEvidence =
                    fixture.coordinator().stagedEvidence();
            assertNotNull(appliedEvidence);
            assertEquals(applied.document().orElseThrow(),
                    appliedEvidence.preparedPairIdentity()
                            .prospectiveDocument());
            byte[] appendedDart = appliedEvidence.candidateDartBytes();
            byte[] appendedFd = appliedEvidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            assertArrayEquals(appendedDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                assertFalse(combined.canRedo());
                combined.undo();
            });

            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            applied.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertFalse(fixture.editor().sourceModified());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });

            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetSlot.ListSlot redoneChildren = assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    redone.document().orElseThrow().root().slots().get(CHILDREN));
            WidgetNode redoneAppended = redoneChildren.children().get(2);
            assertEquals(new WidgetTypeId("flutter.widgets.Text"),
                    redoneAppended.type());
            assertEquals("Text", ((PropertyValue.StringValue)
                    redoneAppended.properties().get(DATA)).value());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence redoneEvidence =
                    fixture.coordinator().stagedEvidence();
            assertNotNull(redoneEvidence);
            assertEquals(redone.document().orElseThrow(),
                    redoneEvidence.preparedPairIdentity()
                            .prospectiveDocument());
            assertArrayEquals(appendedDart, redoneEvidence.candidateDartBytes());
            assertArrayEquals(appendedFd, redoneEvidence.preparedPairIdentity()
                    .prospectiveFdBytes());
            assertArrayEquals(appendedDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertEquals(1, fixture.analysisCalls().get(),
                    "Redo must replay the analyzed pair without another analysis");

            fixture.dataObject().getCookie(SaveCookie.class).save();
            awaitCurrentWithPair(
                    fixture.controller(), appendedFd, appendedDart);
            awaitReadyWithColumnChildIdsAfterToken(
                    fixture.mutations(),
                    redone.token().orElseThrow(),
                    List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertFalse(fixture.editor().sourceModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(appendedDart,
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(appendedFd,
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void emptyNestedColumnTextDropPersistsExactPairThroughRunSavePreflight()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_empty_nested_column_text_preflight",
                scaffoldCenterEmptyColumnExactPair());
        StableId appendedId = StableId.parse(
                "abababab-abab-4bab-8bab-abababababab");

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            assertScaffoldCenterColumnText(
                    ready.document().orElseThrow(), null);

            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            new WidgetTypeId("flutter.widgets.Text"),
                            COLUMN_ID,
                            CHILDREN,
                            0,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected Text at empty nested Column.children[0]");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — add Text to empty nested Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);

            FlutterDesignerMutationController.Snapshot applied =
                    awaitReady(fixture.mutations());
            assertNotSame(ready.token().orElseThrow(),
                    applied.token().orElseThrow());
            WidgetNode appended = assertScaffoldCenterColumnText(
                    applied.document().orElseThrow(), appendedId);
            assertEquals(new PropertyValue.StringValue("Text"),
                    appended.properties().get(DATA));
            assertEquals(1, fixture.analysisCalls().get());

            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            assertEquals(applied.document().orElseThrow(),
                    evidence.preparedPairIdentity().prospectiveDocument());
            byte[] expectedDart = evidence.candidateDartBytes();
            byte[] expectedFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            GeneratedDartRegions exactGenerated = new DartRegionGenerator()
                    .generate(applied.document().orElseThrow(),
                            applied.catalog().orElseThrow())
                    .generated().orElseThrow();
            assertArrayEquals(
                    source(exactGenerated).getBytes(StandardCharsets.UTF_8),
                    expectedDart,
                    "the staged Dart must be the exact nested-tree generation");
            assertArrayEquals(
                    new FdDocumentCodec().encode(
                            applied.document().orElseThrow()).copyBytes(),
                    expectedFd,
                    "the staged .fd must encode the exact nested tree");
            assertGeneratedScaffoldCenterColumnText(expectedDart);
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertTrue(fixture.dataObject().isModified());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

            Path projectRoot = fixture.dartPath().getParent().getParent();
            FlutterProjectSavePreflight.save(projectRoot);

            awaitCurrentWithPair(
                    fixture.controller(), expectedFd, expectedDart);
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertFalse(fixture.dataObject().isModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(expectedDart,
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(expectedFd,
                    Files.readAllBytes(fixture.fdPath()));
            FdDecodeResult.Current savedFd = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(
                            Files.readAllBytes(fixture.fdPath())));
            assertScaffoldCenterColumnText(savedFd.document(), appendedId);
            assertGeneratedScaffoldCenterColumnText(
                    Files.readAllBytes(fixture.dartPath()));
        }
    }

    @Test
    void palettePaddingAppendUsesExactAnalysisUndoRedoAndSavedPair()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_padding_append", columnExactPair());
        StableId appendedId = StableId.parse(
                "45454545-4545-4545-8545-454545454545");
        WidgetTypeId paddingType = new WidgetTypeId(
                "flutter.widgets.Padding");

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            paddingType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected a terminal Column.children Padding placement");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append Padding to Column.children at index 2")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode appended = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(paddingType, appended.type());
            assertInstanceOf(PropertyValue.EdgeInsetsValue.class,
                    appended.properties().get(new PropertyName("padding")));
            WidgetSlot.SingleSlot emptyChild = assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    appended.slots().get(CHILD));
            assertTrue(emptyChild.child().isEmpty());
            assertEquals(1, fixture.analysisCalls().get());
            String analyzedDart = fixture.analyzedContents().getFirst();
            assertTrue(analyzedDart.contains("Padding("), analyzedDart);

            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence appliedEvidence =
                    fixture.coordinator().stagedEvidence();
            assertNotNull(appliedEvidence);
            assertEquals(applied.document().orElseThrow(),
                    appliedEvidence.preparedPairIdentity()
                            .prospectiveDocument());
            byte[] appendedDart = appliedEvidence.candidateDartBytes();
            byte[] appendedFd = appliedEvidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            GeneratedDartRegions exactGenerated = new DartRegionGenerator()
                    .generate(applied.document().orElseThrow(),
                            applied.catalog().orElseThrow())
                    .generated().orElseThrow();
            assertArrayEquals(
                    source(exactGenerated).getBytes(StandardCharsets.UTF_8),
                    appendedDart,
                    "the staged Dart candidate must be the exact generated Padding source");
            assertArrayEquals(
                    new FdDocumentCodec().encode(
                            applied.document().orElseThrow()).copyBytes(),
                    appendedFd,
                    "the staged .fd candidate must encode the exact applied model");
            DesignerDocument decodedFd = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(appendedFd)).document();
            assertEquals(paddingType,
                    findModelWidget(decodedFd.root(), appendedId).type(),
                    "the physical .fd candidate must retain the exact Padding type id");
            assertArrayEquals(appendedDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            applied.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(paddingType,
                    findModelWidget(redone.document().orElseThrow().root(),
                            appendedId).type());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence redoneEvidence =
                    fixture.coordinator().stagedEvidence();
            assertNotNull(redoneEvidence);
            assertArrayEquals(appendedDart, redoneEvidence.candidateDartBytes());
            assertArrayEquals(appendedFd, redoneEvidence.preparedPairIdentity()
                    .prospectiveFdBytes());
            assertEquals(1, fixture.analysisCalls().get(),
                    "Undo and Redo must replay the exact analyzed Padding pair");

            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(
                    fixture.controller(), appendedFd, appendedDart);
            awaitReadyWithColumnChildIdsAfterToken(
                    fixture.mutations(),
                    redone.token().orElseThrow(),
                    List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(appendedDart,
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(appendedFd,
                    Files.readAllBytes(fixture.fdPath()));
            FdDecodeResult.Current savedFd = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(
                            Files.readAllBytes(fixture.fdPath())));
            assertEquals(paddingType,
                    findModelWidget(savedFd.document().root(), appendedId).type(),
                    "Pair Save must persist the exact non-Text widget type");
        }
    }

    @Test
    void paletteSizedBoxInsertionStagesUndoesRedoesAndPersistsExactPair()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_sized_box_append", columnExactPair());
        StableId appendedId = StableId.parse(
                "67676767-6767-4767-8767-676767676767");
        WidgetTypeId sizedBoxType = new WidgetTypeId(
                "flutter.widgets.SizedBox");

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            sizedBoxType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal SizedBox insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append SizedBox to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);

            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode sizedBox = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(sizedBoxType, sizedBox.type());
            assertEquals(Map.of(), sizedBox.properties());
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    sizedBox.slots().get(CHILD)).child().isEmpty());
            assertEquals(1, fixture.analysisCalls().get());
            assertTrue(fixture.analyzedContents().getFirst().contains("SizedBox("));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            DesignerDocument decodedCandidate = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(sizedBoxType,
                    findModelWidget(decodedCandidate.root(), appendedId).type());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(combined::undo);
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            applied.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());

            onEdt(combined::redo);
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(sizedBoxType, findModelWidget(
                    redone.document().orElseThrow().root(), appendedId).type());
            assertArrayEquals(candidateDart,
                    fixture.coordinator().stagedEvidence().candidateDartBytes());
            assertArrayEquals(candidateFd,
                    fixture.coordinator().stagedEvidence()
                            .preparedPairIdentity().prospectiveFdBytes());
            assertEquals(1, fixture.analysisCalls().get(),
                    "Redo must replay the analyzed SizedBox pair");

            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(
                    fixture.controller(), candidateFd, candidateDart);
            awaitReadyWithColumnChildIdsAfterToken(
                    fixture.mutations(),
                    redone.token().orElseThrow(),
                    List.of(FIRST_ID, SECOND_ID, appendedId));
            assertArrayEquals(candidateDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(candidateFd, Files.readAllBytes(fixture.fdPath()));
            FdDecodeResult.Current saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(
                            Files.readAllBytes(fixture.fdPath())));
            assertEquals(sizedBoxType,
                    findModelWidget(saved.document().root(), appendedId).type());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteTransformInsertionPassesPairSaveWithoutTrustingPubCache()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_transform_append", columnExactPair());
        StableId appendedId = StableId.parse(
                "68686868-6868-4868-8868-686868686868");
        WidgetTypeId transformType = new WidgetTypeId(
                "flutter.widgets.Transform");
        AtomicInteger transformAnalysisCalls = new AtomicInteger();

        try (fixture) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        transformAnalysisCalls.incrementAndGet();
                        assertTrue(request.content().contains("Transform("));
                        assertTrue(request.content().contains("Matrix4.fromList"));
                        assertTrue(request.symbolProbes().stream().anyMatch(
                                probe -> probe.expectedSymbolName()
                                        .equals("Transform")));
                        assertFalse(request.symbolProbes().stream().anyMatch(
                                        probe -> probe.expectedSymbolName()
                                                .equals("Matrix4")),
                                "the vector_math declaration must not become a "
                                + "Flutter-SDK-owned navigation probe");
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            transformType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal Transform insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append Transform to Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode transform = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(transformType, transform.type());
            assertEquals(identityMatrix(), transform.properties().get(TRANSFORM));
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    transform.slots().get(CHILD)).child().isEmpty());
            assertEquals(1, transformAnalysisCalls.get());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            assertArrayEquals(candidateDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());

            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            assertArrayEquals(candidateDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(candidateFd, Files.readAllBytes(fixture.fdPath()));
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedTransform = findModelWidget(
                    saved.root(), appendedId);
            assertEquals(transformType, savedTransform.type());
            assertEquals(identityMatrix(),
                    savedTransform.properties().get(TRANSFORM));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteRotatedBoxInsertionSurvivesSaveReloadAndFurtherSignedEdit()
            throws Exception {
        StableId appendedId = StableId.parse(
                "67676767-6767-4767-8767-676767676767");
        WidgetTypeId rotatedBoxType = new WidgetTypeId(
                "flutter.widgets.RotatedBox");
        PropertyValue.IntegerValue initialTurns =
                new PropertyValue.IntegerValue(java.math.BigInteger.ONE);
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_rotated_box_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("RotatedBox("));
                        assertTrue(request.content().contains("quarterTurns: 1"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            rotatedBoxType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal RotatedBox insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append RotatedBox to Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode rotatedBox = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(rotatedBoxType, rotatedBox.type());
            assertEquals(initialTurns,
                    rotatedBox.properties().get(QUARTER_TURNS));
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    rotatedBox.slots().get(CHILD)).child().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            assertArrayEquals(candidateDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(candidateFd, Files.readAllBytes(fixture.fdPath()));
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_rotated_box_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedRotatedBox = findModelWidget(
                    reopened.document().orElseThrow().root(), appendedId);
            assertNotNull(reopenedRotatedBox);
            assertEquals(initialTurns,
                    reopenedRotatedBox.properties().get(QUARTER_TURNS));

            PropertyValue.IntegerValue editedTurns =
                    new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(-3));
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("quarterTurns: -3"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.MutationResult edited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    appendedId, QUARTER_TURNS, editedTurns),
                            "RotatedBox.quarterTurns")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    edited.outcome(), edited::reason);
            FlutterDesignerMutationController.Snapshot changed =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(editedTurns,
                    findModelWidget(changed.document().orElseThrow().root(), appendedId)
                            .properties().get(QUARTER_TURNS));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(editedTurns,
                    findModelWidget(saved.root(), appendedId)
                            .properties().get(QUARTER_TURNS));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteListBodyInsertionSurvivesSaveReloadAndFurtherAxisEdits()
            throws Exception {
        StableId appendedId = StableId.parse(
                "65656565-6565-4565-8565-656565656565");
        WidgetTypeId listBodyType = new WidgetTypeId(
                "flutter.widgets.ListBody");
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_list_body_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("ListBody("));
                        assertTrue(request.content().contains("children: []"));
                        assertFalse(request.content().contains("mainAxis:"));
                        assertFalse(request.content().contains("reverse:"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            listBodyType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal ListBody insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append ListBody to Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode listBody = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(listBodyType, listBody.type());
            assertEquals(Map.of(), listBody.properties(),
                    "omission preserves vertical and false Flutter defaults");
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    listBody.slots().get(CHILDREN)).children().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            assertArrayEquals(candidateDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(candidateFd, Files.readAllBytes(fixture.fdPath()));
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_list_body_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedListBody = findModelWidget(
                    reopened.document().orElseThrow().root(), appendedId);
            assertEquals(listBodyType, reopenedListBody.type());
            assertEquals(Map.of(), reopenedListBody.properties());
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    reopenedListBody.slots().get(CHILDREN)).children().isEmpty());

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "mainAxis: Axis.horizontal"));
                        if (call == 1) {
                            assertFalse(request.content().contains("reverse: true"));
                        } else {
                            assertTrue(request.content().contains("reverse: true"));
                        }
                        assertTrue(request.content().contains("children: []"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            PropertyValue.EnumValue horizontal =
                    new PropertyValue.EnumValue("Axis", "horizontal");
            FlutterDesignerMutationController.MutationResult axisEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(appendedId, MAIN_AXIS, horizontal),
                            "ListBody.mainAxis")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    axisEdited.outcome(), axisEdited::reason);
            FlutterDesignerMutationController.Snapshot horizontalSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(horizontal,
                    findModelWidget(
                            horizontalSnapshot.document().orElseThrow().root(), appendedId)
                            .properties().get(MAIN_AXIS));

            PropertyValue.BooleanValue reversed =
                    new PropertyValue.BooleanValue(true);
            FlutterDesignerMutationController.MutationResult reverseEdited =
                    fixture.mutations().submit(
                            horizontalSnapshot.token().orElseThrow(),
                            new SetProperty(appendedId, REVERSE, reversed),
                            "ListBody.reverse")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    reverseEdited.outcome(), reverseEdited::reason);
            FlutterDesignerMutationController.Snapshot changed =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            horizontalSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode changedListBody = findModelWidget(
                    changed.document().orElseThrow().root(), appendedId);
            assertEquals(horizontal, changedListBody.properties().get(MAIN_AXIS));
            assertEquals(reversed, changedListBody.properties().get(REVERSE));
            assertEquals(2, editAnalyses.get());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedListBody = findModelWidget(saved.root(), appendedId);
            assertEquals(horizontal, savedListBody.properties().get(MAIN_AXIS));
            assertEquals(reversed, savedListBody.properties().get(REVERSE));
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    savedListBody.slots().get(CHILDREN)).children().isEmpty());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteOverflowBarInsertionSurvivesSaveReloadAndFurtherEdits()
            throws Exception {
        StableId appendedId = StableId.parse(
                "67676767-6767-4767-8767-676767676767");
        WidgetTypeId overflowBarType = new WidgetTypeId(
                "flutter.widgets.OverflowBar");
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_overflow_bar_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("OverflowBar("));
                        assertTrue(request.content().contains("children: []"));
                        assertFalse(request.content().contains("spacing:"));
                        assertFalse(request.content().contains(
                                "overflowDirection:"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            overflowBarType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> appendedId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal OverflowBar insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append OverflowBar to Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode overflowBar = findModelWidget(
                    applied.document().orElseThrow().root(), appendedId);
            assertEquals(overflowBarType, overflowBar.type());
            assertEquals(Map.of(), overflowBar.properties(),
                    "omission preserves Flutter's zero spacing, start, down, "
                            + "and ambient direction defaults");
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    overflowBar.slots().get(CHILDREN)).children().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            assertArrayEquals(candidateDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(candidateFd, Files.readAllBytes(fixture.fdPath()));
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_overflow_bar_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedOverflowBar = findModelWidget(
                    reopened.document().orElseThrow().root(), appendedId);
            assertEquals(overflowBarType, reopenedOverflowBar.type());
            assertEquals(Map.of(), reopenedOverflowBar.properties());
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    reopenedOverflowBar.slots().get(CHILDREN))
                    .children().isEmpty());

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("spacing: -4.5"));
                        if (call == 1) {
                            assertFalse(request.content().contains(
                                    "overflowDirection: VerticalDirection.up"));
                        } else {
                            assertTrue(request.content().contains(
                                    "overflowDirection: VerticalDirection.up"));
                        }
                        assertTrue(request.content().contains("children: []"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            PropertyValue.DoubleValue signedSpacing =
                    new PropertyValue.DoubleValue(
                            new java.math.BigDecimal("-4.5"));
            FlutterDesignerMutationController.MutationResult spacingEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(appendedId, SPACING, signedSpacing),
                            "OverflowBar.spacing")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    spacingEdited.outcome(), spacingEdited::reason);
            FlutterDesignerMutationController.Snapshot spacingSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            assertEquals(signedSpacing,
                    findModelWidget(
                            spacingSnapshot.document().orElseThrow().root(),
                            appendedId).properties().get(SPACING));

            PropertyValue.EnumValue upward = new PropertyValue.EnumValue(
                    "VerticalDirection", "up");
            FlutterDesignerMutationController.MutationResult directionEdited =
                    fixture.mutations().submit(
                            spacingSnapshot.token().orElseThrow(),
                            new SetProperty(
                                    appendedId, OVERFLOW_DIRECTION, upward),
                            "OverflowBar.overflowDirection")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    directionEdited.outcome(), directionEdited::reason);
            FlutterDesignerMutationController.Snapshot changed =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            spacingSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, appendedId));
            WidgetNode changedOverflowBar = findModelWidget(
                    changed.document().orElseThrow().root(), appendedId);
            assertEquals(signedSpacing,
                    changedOverflowBar.properties().get(SPACING));
            assertEquals(upward,
                    changedOverflowBar.properties().get(OVERFLOW_DIRECTION));
            assertEquals(2, editAnalyses.get());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedOverflowBar = findModelWidget(
                    saved.root(), appendedId);
            assertEquals(signedSpacing,
                    savedOverflowBar.properties().get(SPACING));
            assertEquals(upward,
                    savedOverflowBar.properties().get(OVERFLOW_DIRECTION));
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    savedOverflowBar.slots().get(CHILDREN)).children().isEmpty());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteGridViewCountReopensWithEditablePropertyAndUndoableChildSlot()
            throws Exception {
        StableId gridId = StableId.parse(
                "70707070-7070-4070-8070-707070707070");
        StableId tileId = StableId.parse(
                "71717171-7171-4171-8171-717171717171");
        WidgetTypeId gridType =
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE;
        WidgetTypeId textType = new WidgetTypeId("flutter.widgets.Text");
        PropertyValue.IntegerValue two = new PropertyValue.IntegerValue(
                java.math.BigInteger.valueOf(2));
        PropertyValue.IntegerValue three = new PropertyValue.IntegerValue(
                java.math.BigInteger.valueOf(3));
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_grid_view_count_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("GridView.count("));
                        assertTrue(request.content().contains("crossAxisCount: 2"));
                        assertTrue(request.content().contains("children: []"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            gridType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> gridId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal GridView.count insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append GridView.count to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            WidgetNode grid = findModelWidget(
                    applied.document().orElseThrow().root(), gridId);
            assertEquals(gridType, grid.type());
            assertEquals(Map.of(CROSS_AXIS_COUNT, two), grid.properties());
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    grid.slots().get(CHILDREN)).children().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_grid_view_count_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedGrid = findModelWidget(
                    reopened.document().orElseThrow().root(), gridId);
            assertEquals(Map.of(CROSS_AXIS_COUNT, two), reopenedGrid.properties());
            var definition = reopened.catalog().orElseThrow()
                    .find(gridType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedGrid,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> crossAxisCount =
                    cellProperty(propertiesNode, "crossAxisCount");
            assertTrue(crossAxisCount.canWrite(),
                    "reopened GridView.count must retain its numeric editor");
            assertEquals(FlutterPropertyCellValue.explicit(two),
                    crossAxisCount.getValue());

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("GridView.count("));
                        assertTrue(request.content().contains("crossAxisCount: 3"));
                        if (call == 1) {
                            assertTrue(request.content().contains("children: []"));
                        } else {
                            assertTrue(request.content().contains("Text("));
                            assertTrue(request.content().contains("children: ["));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult propertyEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(gridId, CROSS_AXIS_COUNT, three),
                            "GridView.count.crossAxisCount")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    propertyEdited.outcome(), propertyEdited::reason);
            FlutterDesignerMutationController.Snapshot propertySnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            assertEquals(three, findModelWidget(
                    propertySnapshot.document().orElseThrow().root(), gridId)
                    .properties().get(CROSS_AXIS_COUNT));

            FlutterDesignerPaletteDropPlanner.Result childPlan =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            propertySnapshot.document().orElseThrow(),
                            propertySnapshot.catalog().orElseThrow(),
                            textType,
                            gridId,
                            CHILDREN,
                            0,
                            () -> tileId);
            FlutterDesignerPaletteDropPlanner.Accepted childAccepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, childPlan);
            FlutterDesignerMutationController.MutationResult childAdded =
                    fixture.mutations().submit(
                            propertySnapshot.token().orElseThrow(),
                            childAccepted.command(),
                            "GridView.count.children — append Text")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    childAdded.outcome(), childAdded::reason);
            FlutterDesignerMutationController.Snapshot childSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertySnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            assertEquals(List.of(tileId), assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    findModelWidget(
                            childSnapshot.document().orElseThrow().root(), gridId)
                            .slots().get(CHILDREN)).children().stream()
                    .map(WidgetNode::id).toList());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot childUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            WidgetNode withoutTile = findModelWidget(
                    childUndone.document().orElseThrow().root(), gridId);
            assertEquals(three, withoutTile.properties().get(CROSS_AXIS_COUNT));
            assertTrue(assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    withoutTile.slots().get(CHILDREN)).children().isEmpty());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot propertyUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            assertEquals(two, findModelWidget(
                    propertyUndone.document().orElseThrow().root(), gridId)
                    .properties().get(CROSS_AXIS_COUNT));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot propertyRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            assertEquals(three, findModelWidget(
                    propertyRedone.document().orElseThrow().root(), gridId)
                    .properties().get(CROSS_AXIS_COUNT));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot childRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyRedone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, gridId));
            WidgetNode finalGrid = findModelWidget(
                    childRedone.document().orElseThrow().root(), gridId);
            assertEquals(three, finalGrid.properties().get(CROSS_AXIS_COUNT));
            assertEquals(List.of(tileId), assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    finalGrid.slots().get(CHILDREN)).children().stream()
                    .map(WidgetNode::id).toList());
            assertEquals(2, editAnalyses.get(),
                    "Undo/Redo must replay the two exact analyzed GridView.count pairs");

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedGrid = findModelWidget(saved.root(), gridId);
            assertEquals(three, savedGrid.properties().get(CROSS_AXIS_COUNT));
            assertEquals(List.of(tileId), assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    savedGrid.slots().get(CHILDREN)).children().stream()
                    .map(WidgetNode::id).toList());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteSingleChildScrollViewReopensForFurtherEditAndUndoableChildSlot()
            throws Exception {
        StableId scrollId = StableId.parse(
                "72727272-7272-4272-8272-727272727272");
        StableId childId = StableId.parse(
                "73737373-7373-4373-8373-737373737373");
        WidgetTypeId scrollType = SingleChildScrollViewWidgetPropertySchema
                .SINGLE_CHILD_SCROLL_VIEW_TYPE;
        WidgetTypeId textType = new WidgetTypeId("flutter.widgets.Text");
        PropertyValue.BooleanValue reversed =
                new PropertyValue.BooleanValue(true);
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_single_child_scroll_view_append",
                columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("SingleChildScrollView("));
                        assertTrue(request.content().contains("child: null"));
                        assertFalse(request.content().contains("reverse:"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            scrollType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> scrollId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal SingleChildScrollView insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append SingleChildScrollView to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            WidgetNode scroll = findModelWidget(
                    applied.document().orElseThrow().root(), scrollId);
            assertEquals(scrollType, scroll.type());
            assertTrue(scroll.properties().isEmpty());
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    scroll.slots().get(CHILD)).child().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_single_child_scroll_view_reopened",
                durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedScroll = findModelWidget(
                    reopened.document().orElseThrow().root(), scrollId);
            assertTrue(reopenedScroll.properties().isEmpty());
            var definition = reopened.catalog().orElseThrow()
                    .find(scrollType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedScroll,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> reverse =
                    cellProperty(propertiesNode, "reverse");
            assertTrue(reverse.canWrite(),
                    "reopened SingleChildScrollView must retain its boolean editor");
            assertEquals(FlutterPropertyCellValue.unset(), reverse.getValue());

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("SingleChildScrollView("));
                        assertTrue(request.content().contains("reverse: true"));
                        if (call == 1) {
                            assertTrue(request.content().contains("child: null"));
                        } else {
                            assertTrue(request.content().contains("child: const Text("));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult propertyEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(scrollId, REVERSE, reversed),
                            "SingleChildScrollView.reverse")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    propertyEdited.outcome(), propertyEdited::reason);
            FlutterDesignerMutationController.Snapshot propertySnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            assertEquals(reversed, findModelWidget(
                    propertySnapshot.document().orElseThrow().root(), scrollId)
                    .properties().get(REVERSE));

            FlutterDesignerPaletteDropPlanner.Result childPlan =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            propertySnapshot.document().orElseThrow(),
                            propertySnapshot.catalog().orElseThrow(),
                            textType,
                            scrollId,
                            CHILD,
                            0,
                            () -> childId);
            FlutterDesignerPaletteDropPlanner.Accepted childAccepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, childPlan);
            FlutterDesignerMutationController.MutationResult childAdded =
                    fixture.mutations().submit(
                            propertySnapshot.token().orElseThrow(),
                            childAccepted.command(),
                            "SingleChildScrollView.child — add Text")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    childAdded.outcome(), childAdded::reason);
            FlutterDesignerMutationController.Snapshot childSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertySnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    findModelWidget(
                            childSnapshot.document().orElseThrow().root(), scrollId)
                            .slots().get(CHILD)).child().orElseThrow().id());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot childUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            WidgetNode withoutChild = findModelWidget(
                    childUndone.document().orElseThrow().root(), scrollId);
            assertEquals(reversed, withoutChild.properties().get(REVERSE));
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    withoutChild.slots().get(CHILD)).child().isEmpty());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot propertyUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            assertFalse(findModelWidget(
                    propertyUndone.document().orElseThrow().root(), scrollId)
                    .properties().containsKey(REVERSE));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot propertyRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            assertEquals(reversed, findModelWidget(
                    propertyRedone.document().orElseThrow().root(), scrollId)
                    .properties().get(REVERSE));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot childRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyRedone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, scrollId));
            WidgetNode finalScroll = findModelWidget(
                    childRedone.document().orElseThrow().root(), scrollId);
            assertEquals(reversed, finalScroll.properties().get(REVERSE));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    finalScroll.slots().get(CHILD)).child().orElseThrow().id());
            assertEquals(2, editAnalyses.get(),
                    "Undo/Redo must replay the two exact analyzed SingleChildScrollView pairs");

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedScroll = findModelWidget(saved.root(), scrollId);
            assertEquals(reversed, savedScroll.properties().get(REVERSE));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    savedScroll.slots().get(CHILD)).child().orElseThrow().id());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteColoredBoxReopensWithEditableCheckboxAndUndoableChildSlot()
            throws Exception {
        StableId coloredBoxId = StableId.parse(
                "74747474-7474-4474-8474-747474747474");
        StableId childId = StableId.parse(
                "75757575-7575-4575-8575-757575757575");
        WidgetTypeId coloredBoxType =
                ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE;
        WidgetTypeId textType = new WidgetTypeId("flutter.widgets.Text");
        PropertyValue.ColorValue creationBlue =
                new PropertyValue.ColorValue(0xFF2196F3L);
        PropertyValue.BooleanValue antiAliasDisabled =
                new PropertyValue.BooleanValue(false);
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_colored_box_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("const ColoredBox("));
                        assertTrue(request.content().contains(
                                "color: const Color(0xFF2196F3)"));
                        assertTrue(request.content().contains("child: null"));
                        assertFalse(request.content().contains("isAntiAlias:"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            coloredBoxType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> coloredBoxId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal ColoredBox insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append ColoredBox to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            WidgetNode coloredBox = findModelWidget(
                    applied.document().orElseThrow().root(), coloredBoxId);
            assertEquals(coloredBoxType, coloredBox.type());
            assertEquals(Map.of(COLOR, creationBlue), coloredBox.properties());
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    coloredBox.slots().get(CHILD)).child().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_colored_box_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedColoredBox = findModelWidget(
                    reopened.document().orElseThrow().root(), coloredBoxId);
            assertEquals(Map.of(COLOR, creationBlue),
                    reopenedColoredBox.properties());
            var definition = reopened.catalog().orElseThrow()
                    .find(coloredBoxType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedColoredBox,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> color =
                    cellProperty(propertiesNode, "color");
            assertTrue(color.canWrite(),
                    "reopened ColoredBox must retain its required color editor");
            assertEquals(FlutterPropertyCellValue.explicit(creationBlue),
                    color.getValue());
            Node.Property<FlutterPropertyCellValue> isAntiAlias =
                    cellProperty(propertiesNode, "isAntiAlias");
            assertTrue(isAntiAlias.canWrite(),
                    "reopened ColoredBox must retain its boolean editor");
            assertEquals(FlutterPropertyCellValue.unset(), isAntiAlias.getValue());
            assertTrue(isAntiAlias.getPropertyEditor().isPaintable(),
                    "reopened optional boolean must retain its checkbox renderer");
            assertNull(isAntiAlias.getPropertyEditor().getTags(),
                    "reopened optional boolean must not regress to a combo editor");

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("const ColoredBox("));
                        assertTrue(request.content().contains(
                                "color: const Color(0xFF2196F3)"));
                        assertTrue(request.content().contains(
                                "isAntiAlias: false"));
                        if (call == 1) {
                            assertTrue(request.content().contains("child: null"));
                        } else {
                            assertTrue(request.content().contains(
                                    "child: const Text("));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult propertyEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    coloredBoxId,
                                    IS_ANTI_ALIAS,
                                    antiAliasDisabled),
                            "ColoredBox.isAntiAlias")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    propertyEdited.outcome(), propertyEdited::reason);
            FlutterDesignerMutationController.Snapshot propertySnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            WidgetNode propertyEditedColoredBox = findModelWidget(
                    propertySnapshot.document().orElseThrow().root(),
                    coloredBoxId);
            assertEquals(creationBlue,
                    propertyEditedColoredBox.properties().get(COLOR));
            assertEquals(antiAliasDisabled,
                    propertyEditedColoredBox.properties().get(IS_ANTI_ALIAS));

            FlutterDesignerPaletteDropPlanner.Result childPlan =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            propertySnapshot.document().orElseThrow(),
                            propertySnapshot.catalog().orElseThrow(),
                            textType,
                            coloredBoxId,
                            CHILD,
                            0,
                            () -> childId);
            FlutterDesignerPaletteDropPlanner.Accepted childAccepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, childPlan);
            FlutterDesignerMutationController.MutationResult childAdded =
                    fixture.mutations().submit(
                            propertySnapshot.token().orElseThrow(),
                            childAccepted.command(),
                            "ColoredBox.child — add Text")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    childAdded.outcome(), childAdded::reason);
            FlutterDesignerMutationController.Snapshot childSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertySnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    findModelWidget(
                            childSnapshot.document().orElseThrow().root(),
                            coloredBoxId)
                            .slots().get(CHILD)).child().orElseThrow().id());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot childUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            WidgetNode withoutChild = findModelWidget(
                    childUndone.document().orElseThrow().root(), coloredBoxId);
            assertEquals(creationBlue, withoutChild.properties().get(COLOR));
            assertEquals(antiAliasDisabled,
                    withoutChild.properties().get(IS_ANTI_ALIAS));
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    withoutChild.slots().get(CHILD)).child().isEmpty());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot propertyUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            WidgetNode withoutAntiAlias = findModelWidget(
                    propertyUndone.document().orElseThrow().root(),
                    coloredBoxId);
            assertEquals(creationBlue, withoutAntiAlias.properties().get(COLOR));
            assertFalse(withoutAntiAlias.properties().containsKey(IS_ANTI_ALIAS));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot propertyRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            assertEquals(antiAliasDisabled, findModelWidget(
                    propertyRedone.document().orElseThrow().root(),
                    coloredBoxId).properties().get(IS_ANTI_ALIAS));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot childRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyRedone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, coloredBoxId));
            WidgetNode finalColoredBox = findModelWidget(
                    childRedone.document().orElseThrow().root(), coloredBoxId);
            assertEquals(creationBlue, finalColoredBox.properties().get(COLOR));
            assertEquals(antiAliasDisabled,
                    finalColoredBox.properties().get(IS_ANTI_ALIAS));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    finalColoredBox.slots().get(CHILD)).child().orElseThrow().id());
            assertEquals(2, editAnalyses.get(),
                    "Undo/Redo must replay the two exact analyzed ColoredBox pairs");

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedColoredBox = findModelWidget(
                    saved.root(), coloredBoxId);
            assertEquals(creationBlue,
                    savedColoredBox.properties().get(COLOR));
            assertEquals(antiAliasDisabled,
                    savedColoredBox.properties().get(IS_ANTI_ALIAS));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    savedColoredBox.slots().get(CHILD)).child().orElseThrow().id());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void palettePlaceholderReopensForFurtherPropertyChildUndoRedoAndSave()
            throws Exception {
        StableId placeholderId = StableId.parse(
                "78787878-7878-4878-8878-787878787878");
        StableId childId = StableId.parse(
                "79797979-7979-4979-8979-797979797979");
        WidgetTypeId placeholderType =
                PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE;
        WidgetTypeId textType = new WidgetTypeId("flutter.widgets.Text");
        PropertyValue.IntegerValue fallbackWidth =
                new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(320));
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_placeholder_append", columnExactPair())) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        assertTrue(request.content().contains("const Placeholder("));
                        assertTrue(request.content().contains("child: null"));
                        assertFalse(request.content().contains("color:"));
                        assertFalse(request.content().contains("strokeWidth:"));
                        assertFalse(request.content().contains("fallbackWidth:"));
                        assertFalse(request.content().contains("fallbackHeight:"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            placeholderType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> placeholderId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal Placeholder insertion");

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append Placeholder to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot applied =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            WidgetNode placeholder = findModelWidget(
                    applied.document().orElseThrow().root(), placeholderId);
            assertEquals(placeholderType, placeholder.type());
            assertTrue(placeholder.properties().isEmpty(),
                    "creation must preserve all four Placeholder defaults by omission");
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    placeholder.slots().get(CHILD)).child().isEmpty());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_placeholder_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedPlaceholder = findModelWidget(
                    reopened.document().orElseThrow().root(), placeholderId);
            assertTrue(reopenedPlaceholder.properties().isEmpty());
            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(placeholderType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedPlaceholder,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> color =
                    cellProperty(propertiesNode, "color");
            Node.Property<FlutterPropertyCellValue> fallback =
                    cellProperty(propertiesNode, "fallbackWidth");
            assertTrue(color.canWrite());
            assertEquals(FlutterPropertyCellValue.unset(), color.getValue());
            assertTrue(fallback.canWrite());
            assertEquals(FlutterPropertyCellValue.unset(), fallback.getValue());
            assertTrue(fallback.supportsDefaultValue());

            AtomicInteger editAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = editAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("const Placeholder("));
                        assertTrue(request.content().contains("fallbackWidth: 320"));
                        assertFalse(request.content().contains("color:"));
                        assertFalse(request.content().contains("strokeWidth:"));
                        assertFalse(request.content().contains("fallbackHeight:"));
                        if (call == 1) {
                            assertTrue(request.content().contains("child: null"));
                        } else {
                            assertTrue(request.content().contains(
                                    "child: const Text("));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult propertyEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    placeholderId,
                                    FALLBACK_WIDTH,
                                    fallbackWidth),
                            "Placeholder.fallbackWidth")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    propertyEdited.outcome(), propertyEdited::reason);
            FlutterDesignerMutationController.Snapshot propertySnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            assertEquals(fallbackWidth, findModelWidget(
                    propertySnapshot.document().orElseThrow().root(),
                    placeholderId).properties().get(FALLBACK_WIDTH));

            FlutterDesignerPaletteDropPlanner.Result childPlan =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            propertySnapshot.document().orElseThrow(),
                            propertySnapshot.catalog().orElseThrow(),
                            textType,
                            placeholderId,
                            CHILD,
                            0,
                            () -> childId);
            FlutterDesignerPaletteDropPlanner.Accepted childAccepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, childPlan);
            FlutterDesignerMutationController.MutationResult childAdded =
                    fixture.mutations().submit(
                            propertySnapshot.token().orElseThrow(),
                            childAccepted.command(),
                            "Placeholder.child — add Text")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    childAdded.outcome(), childAdded::reason);
            FlutterDesignerMutationController.Snapshot childSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertySnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    findModelWidget(
                            childSnapshot.document().orElseThrow().root(),
                            placeholderId).slots().get(CHILD))
                    .child().orElseThrow().id());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot childUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            WidgetNode withoutChild = findModelWidget(
                    childUndone.document().orElseThrow().root(), placeholderId);
            assertEquals(fallbackWidth,
                    withoutChild.properties().get(FALLBACK_WIDTH));
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    withoutChild.slots().get(CHILD)).child().isEmpty());

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot propertyUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            childUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            assertTrue(findModelWidget(
                    propertyUndone.document().orElseThrow().root(),
                    placeholderId).properties().isEmpty());

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot propertyRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyUndone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            assertEquals(fallbackWidth, findModelWidget(
                    propertyRedone.document().orElseThrow().root(),
                    placeholderId).properties().get(FALLBACK_WIDTH));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot childRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            propertyRedone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, placeholderId));
            WidgetNode finalPlaceholder = findModelWidget(
                    childRedone.document().orElseThrow().root(), placeholderId);
            assertEquals(fallbackWidth,
                    finalPlaceholder.properties().get(FALLBACK_WIDTH));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    finalPlaceholder.slots().get(CHILD))
                    .child().orElseThrow().id());
            assertEquals(2, editAnalyses.get(),
                    "Undo/Redo must replay the two exact analyzed Placeholder pairs");

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedPlaceholder = findModelWidget(
                    saved.root(), placeholderId);
            assertEquals(fallbackWidth,
                    savedPlaceholder.properties().get(FALLBACK_WIDTH));
            assertEquals(childId, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    savedPlaceholder.slots().get(CHILD))
                    .child().orElseThrow().id());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteSafeAreaWrapsExistingChildThenReopensForFurtherUndoableEdits()
            throws Exception {
        StableId safeAreaId = StableId.parse(
                "76767676-7676-4676-8676-767676767676");
        WidgetTypeId safeAreaType = SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE;
        PropertyValue.BooleanValue leftDisabled =
                new PropertyValue.BooleanValue(false);
        PropertyValue.EdgeInsetsValue minimum = new PropertyValue.EdgeInsetsValue(
                java.math.BigDecimal.ONE,
                java.math.BigDecimal.valueOf(2),
                java.math.BigDecimal.valueOf(3),
                java.math.BigDecimal.valueOf(4));
        PropertyValue.BooleanValue maintainBottom =
                new PropertyValue.BooleanValue(true);
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_safe_area_wrap", columnExactPair())) {
            AtomicInteger initialAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = initialAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("const SafeArea("));
                        assertTrue(request.content().contains("child: const Text("));
                        assertFalse(request.content().contains("child: null"));
                        assertFalse(request.content().contains("minimum:"));
                        assertFalse(request.content().contains(
                                "maintainBottomViewPadding:"));
                        if (call == 1) {
                            assertFalse(request.content().contains("left:"));
                        } else {
                            assertTrue(request.content().contains("left: false"));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().planWrapTarget(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            safeAreaType,
                            FIRST_ID,
                            () -> safeAreaId);
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected SafeArea wrapper around the existing Text");

            FlutterDesignerMutationController.MutationResult wrapApplied =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            wrapped.command(),
                            "home_page.fd — wrap Text with SafeArea")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    wrapApplied.outcome(), wrapApplied::reason);
            FlutterDesignerMutationController.Snapshot wrappedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            WidgetNode safeArea = findModelWidget(
                    wrappedSnapshot.document().orElseThrow().root(), safeAreaId);
            assertTrue(safeArea.properties().isEmpty());
            assertEquals(FIRST_ID, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    safeArea.slots().get(CHILD)).child().orElseThrow().id());

            FlutterDesignerMutationController.MutationResult leftEdited =
                    fixture.mutations().submit(
                            wrappedSnapshot.token().orElseThrow(),
                            new SetProperty(safeAreaId, SAFE_LEFT, leftDisabled),
                            "SafeArea.left")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    leftEdited.outcome(), leftEdited::reason);
            FlutterDesignerMutationController.Snapshot editedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            wrappedSnapshot.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            assertEquals(leftDisabled, findModelWidget(
                    editedSnapshot.document().orElseThrow().root(), safeAreaId)
                    .properties().get(SAFE_LEFT));
            assertEquals(2, initialAnalyses.get());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_safe_area_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedSafeArea = findModelWidget(
                    reopened.document().orElseThrow().root(), safeAreaId);
            assertEquals(Map.of(SAFE_LEFT, leftDisabled),
                    reopenedSafeArea.properties());
            assertEquals(FIRST_ID, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    reopenedSafeArea.slots().get(CHILD)).child().orElseThrow().id());

            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(safeAreaType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedSafeArea,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> left =
                    cellProperty(propertiesNode, "left");
            assertEquals(FlutterPropertyCellValue.explicit(leftDisabled), left.getValue());
            assertTrue(left.getPropertyEditor().isPaintable());
            assertNull(left.getPropertyEditor().getTags());
            Node.Property<FlutterPropertyCellValue> minimumProperty =
                    cellProperty(propertiesNode, "minimum");
            assertEquals(FlutterPropertyCellValue.unset(), minimumProperty.getValue());
            assertTrue(minimumProperty.getPropertyEditor().supportsCustomEditor());
            Node.Property<FlutterPropertyCellValue> maintain =
                    cellProperty(propertiesNode, "maintainBottomViewPadding");
            assertEquals(FlutterPropertyCellValue.unset(), maintain.getValue());
            assertTrue(maintain.getPropertyEditor().isPaintable());
            assertEquals("Text", java.util.Arrays.stream(propertiesNode.getPropertySets())
                    .flatMap(set -> java.util.Arrays.stream(set.getProperties()))
                    .filter(property -> "child".equals(property.getName()))
                    .findFirst().orElseThrow().getValue());

            AtomicInteger furtherAnalyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        int call = furtherAnalyses.incrementAndGet();
                        assertTrue(request.content().contains("const SafeArea("));
                        assertTrue(request.content().contains("left: false"));
                        assertTrue(request.content().contains(
                                "minimum: const EdgeInsets.fromLTRB(1.0, 2.0, 3.0, 4.0)"));
                        assertTrue(request.content().contains("child: const Text("));
                        if (call == 1) {
                            assertFalse(request.content().contains(
                                    "maintainBottomViewPadding:"));
                        } else {
                            assertTrue(request.content().contains(
                                    "maintainBottomViewPadding: true"));
                        }
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult minimumEdited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(safeAreaId, SAFE_MINIMUM, minimum),
                            "SafeArea.minimum")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    minimumEdited.outcome(), minimumEdited::reason);
            FlutterDesignerMutationController.Snapshot minimumSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));

            FlutterDesignerMutationController.MutationResult maintainEdited =
                    fixture.mutations().submit(
                            minimumSnapshot.token().orElseThrow(),
                            new SetProperty(
                                    safeAreaId,
                                    MAINTAIN_BOTTOM_VIEW_PADDING,
                                    maintainBottom),
                            "SafeArea.maintainBottomViewPadding")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    maintainEdited.outcome(), maintainEdited::reason);
            FlutterDesignerMutationController.Snapshot maintainSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            minimumSnapshot.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            WidgetNode fullyEdited = findModelWidget(
                    maintainSnapshot.document().orElseThrow().root(), safeAreaId);
            assertEquals(leftDisabled, fullyEdited.properties().get(SAFE_LEFT));
            assertEquals(minimum, fullyEdited.properties().get(SAFE_MINIMUM));
            assertEquals(maintainBottom,
                    fullyEdited.properties().get(MAINTAIN_BOTTOM_VIEW_PADDING));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot maintainUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            maintainSnapshot.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            WidgetNode withoutMaintain = findModelWidget(
                    maintainUndone.document().orElseThrow().root(), safeAreaId);
            assertEquals(minimum, withoutMaintain.properties().get(SAFE_MINIMUM));
            assertFalse(withoutMaintain.properties().containsKey(
                    MAINTAIN_BOTTOM_VIEW_PADDING));

            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot minimumUndone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            maintainUndone.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            WidgetNode persistedBaseline = findModelWidget(
                    minimumUndone.document().orElseThrow().root(), safeAreaId);
            assertEquals(Map.of(SAFE_LEFT, leftDisabled),
                    persistedBaseline.properties());

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot minimumRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            minimumUndone.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            assertEquals(minimum, findModelWidget(
                    minimumRedone.document().orElseThrow().root(), safeAreaId)
                    .properties().get(SAFE_MINIMUM));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot maintainRedone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            minimumRedone.token().orElseThrow(),
                            List.of(safeAreaId, SECOND_ID));
            WidgetNode finalSafeArea = findModelWidget(
                    maintainRedone.document().orElseThrow().root(), safeAreaId);
            assertEquals(leftDisabled, finalSafeArea.properties().get(SAFE_LEFT));
            assertEquals(minimum, finalSafeArea.properties().get(SAFE_MINIMUM));
            assertEquals(maintainBottom,
                    finalSafeArea.properties().get(MAINTAIN_BOTTOM_VIEW_PADDING));
            assertEquals(FIRST_ID, assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    finalSafeArea.slots().get(CHILD)).child().orElseThrow().id());
            assertEquals(2, furtherAnalyses.get(),
                    "Undo/Redo must replay the two exact analyzed SafeArea pairs");

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            WidgetNode savedSafeArea = findModelWidget(saved.root(), safeAreaId);
            assertEquals(finalSafeArea, savedSafeArea);
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteDirectionalityWrapSaveReopenEditUndoRedoAndSaveRemainExact()
            throws Exception {
        StableId directionalityId = StableId.parse(
                "78787878-7878-4878-8878-787878787878");
        WidgetTypeId directionalityType =
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE;
        PropertyValue.EnumValue ltr =
                new PropertyValue.EnumValue("TextDirection", "ltr");
        PropertyValue.EnumValue rtl =
                new PropertyValue.EnumValue("TextDirection", "rtl");
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_directionality_wrap", columnExactPair())) {
            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const Directionality("));
                        assertTrue(request.content().contains(
                                "textDirection: TextDirection.ltr"));
                        assertTrue(request.content().contains(
                                "child: const Text("));
                        assertFalse(request.content().contains("child: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().planWrapTarget(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            directionalityType,
                            FIRST_ID,
                            () -> directionalityId);
            FlutterDesignerPaletteDropPlanner.Wrapped wrapped = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Wrapped.class,
                    planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected Directionality around existing Text");

            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            wrapped.command(),
                            "home_page.fd — wrap Text with Directionality")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot wrappedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(directionalityId, SECOND_ID));
            WidgetNode directionality = findModelWidget(
                    wrappedSnapshot.document().orElseThrow().root(),
                    directionalityId);
            assertAll(
                    () -> assertEquals(Map.of(TEXT_DIRECTION, ltr),
                            directionality.properties()),
                    () -> assertEquals(FIRST_ID, assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            directionality.slots().get(CHILD))
                            .child().orElseThrow().id()),
                    () -> assertEquals(1, analyses.get()));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_directionality_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedDirectionality = findModelWidget(
                    reopened.document().orElseThrow().root(), directionalityId);
            assertAll(
                    () -> assertEquals(Map.of(TEXT_DIRECTION, ltr),
                            reopenedDirectionality.properties()),
                    () -> assertEquals(FIRST_ID, assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            reopenedDirectionality.slots().get(CHILD))
                            .child().orElseThrow().id()));

            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(directionalityType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedDirectionality,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> textDirection =
                    cellProperty(propertiesNode, "textDirection");
            assertAll(
                    () -> assertEquals(
                            FlutterPropertyCellValue.explicit(ltr),
                            textDirection.getValue()),
                    () -> assertArrayEquals(
                            new String[] {"rtl", "ltr"},
                            textDirection.getPropertyEditor().getTags()),
                    () -> assertEquals("Text", java.util.Arrays.stream(
                            propertiesNode.getPropertySets())
                            .flatMap(set -> java.util.Arrays.stream(
                                    set.getProperties()))
                            .filter(property -> "child".equals(
                                    property.getName()))
                            .findFirst().orElseThrow().getValue()));

            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const Directionality("));
                        assertTrue(request.content().contains(
                                "textDirection: TextDirection.rtl"));
                        assertFalse(request.content().contains(
                                "textDirection: TextDirection.ltr"));
                        assertTrue(request.content().contains(
                                "child: const Text("));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult edited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    directionalityId, TEXT_DIRECTION, rtl),
                            "Directionality.textDirection")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    edited.outcome(), edited::reason);
            FlutterDesignerMutationController.Snapshot editedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(directionalityId, SECOND_ID));
            assertEquals(rtl, findModelWidget(
                    editedSnapshot.document().orElseThrow().root(),
                    directionalityId).properties().get(TEXT_DIRECTION));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            editedSnapshot.token().orElseThrow(),
                            List.of(directionalityId, SECOND_ID));
            assertEquals(ltr, findModelWidget(
                    undone.document().orElseThrow().root(), directionalityId)
                    .properties().get(TEXT_DIRECTION));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(directionalityId, SECOND_ID));
            WidgetNode finalDirectionality = findModelWidget(
                    redone.document().orElseThrow().root(), directionalityId);
            assertAll(
                    () -> assertEquals(rtl,
                            finalDirectionality.properties().get(TEXT_DIRECTION)),
                    () -> assertEquals(FIRST_ID, assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            finalDirectionality.slots().get(CHILD))
                            .child().orElseThrow().id()),
                    () -> assertEquals(1, analyses.get(),
                            "Undo/Redo replays the exact analyzed pair"));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(finalDirectionality,
                    findModelWidget(saved.root(), directionalityId));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteDecoratedBoxSaveReopenEditUndoRedoAndSaveRemainExact()
            throws Exception {
        StableId decoratedBoxId = StableId.parse(
                "79797979-7979-4979-8979-797979797979");
        WidgetTypeId decoratedBoxType =
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE;
        PropertyValue.EnumValue foreground =
                new PropertyValue.EnumValue(
                        "DecorationPosition", "foreground");
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_decorated_box_append", columnExactPair())) {
            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const DecoratedBox("));
                        assertTrue(request.content().contains(
                                "decoration: const BoxDecoration("));
                        assertFalse(request.content().contains("position:"));
                        assertTrue(request.content().contains("child: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            decoratedBoxType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> decoratedBoxId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal DecoratedBox insertion");

            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append DecoratedBox to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot added =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, decoratedBoxId));
            WidgetNode decoratedBox = findModelWidget(
                    added.document().orElseThrow().root(), decoratedBoxId);
            PropertyValue.BoxDecorationValue emptyDecoration = assertInstanceOf(
                    PropertyValue.BoxDecorationValue.class,
                    decoratedBox.properties().get(DECORATION));
            assertAll(
                    () -> assertEquals(1, decoratedBox.properties().size()),
                    () -> assertTrue(emptyDecoration.color().isEmpty()),
                    () -> assertTrue(emptyDecoration.image().isEmpty()),
                    () -> assertTrue(emptyDecoration.border().isEmpty()),
                    () -> assertTrue(emptyDecoration.borderRadius().isEmpty()),
                    () -> assertTrue(emptyDecoration.boxShadow().isEmpty()),
                    () -> assertTrue(emptyDecoration.gradient().isEmpty()),
                    () -> assertTrue(
                            emptyDecoration.backgroundBlendMode().isEmpty()),
                    () -> assertEquals(
                            PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE,
                            emptyDecoration.shape()),
                    () -> assertTrue(assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            decoratedBox.slots().get(CHILD)).child().isEmpty()),
                    () -> assertEquals(1, analyses.get()));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_decorated_box_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedDecoratedBox = findModelWidget(
                    reopened.document().orElseThrow().root(), decoratedBoxId);
            PropertyValue.BoxDecorationValue emptyDecoration = assertInstanceOf(
                    PropertyValue.BoxDecorationValue.class,
                    reopenedDecoratedBox.properties().get(DECORATION));
            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(decoratedBoxType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedDecoratedBox,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> decoration =
                    cellProperty(propertiesNode, "decoration");
            Node.Property<FlutterPropertyCellValue> position =
                    cellProperty(propertiesNode, "position");
            assertAll(
                    () -> assertEquals(
                            FlutterPropertyCellValue.explicit(emptyDecoration),
                            decoration.getValue()),
                    () -> assertFalse(decoration.supportsDefaultValue(),
                            "required decoration cannot be reset"),
                    () -> assertEquals(
                            FlutterPropertyCellValue.unset(),
                            position.getValue()),
                    () -> assertTrue(position.supportsDefaultValue()),
                    () -> assertArrayEquals(
                            new String[] {
                                FlutterWidgetPropertiesNode.NOT_SET,
                                "background", "foreground"
                            },
                            position.getPropertyEditor().getTags()),
                    () -> assertEquals("Empty", java.util.Arrays.stream(
                            propertiesNode.getPropertySets())
                            .flatMap(set -> java.util.Arrays.stream(
                                    set.getProperties()))
                            .filter(property -> "child".equals(
                                    property.getName()))
                            .findFirst().orElseThrow().getValue()));

            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const DecoratedBox("));
                        assertTrue(request.content().contains(
                                "decoration: const BoxDecoration("));
                        assertTrue(request.content().contains(
                                ".DecorationPosition.foreground"));
                        assertTrue(request.content().contains("child: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult edited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    decoratedBoxId,
                                    DECORATION_POSITION,
                                    foreground),
                            "DecoratedBox.position")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    edited.outcome(), edited::reason);
            FlutterDesignerMutationController.Snapshot editedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, decoratedBoxId));
            assertEquals(foreground, findModelWidget(
                    editedSnapshot.document().orElseThrow().root(),
                    decoratedBoxId).properties().get(DECORATION_POSITION));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            editedSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, decoratedBoxId));
            assertFalse(findModelWidget(
                    undone.document().orElseThrow().root(),
                    decoratedBoxId).properties().containsKey(
                            DECORATION_POSITION));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, decoratedBoxId));
            WidgetNode finalDecoratedBox = findModelWidget(
                    redone.document().orElseThrow().root(), decoratedBoxId);
            assertAll(
                    () -> assertEquals(foreground,
                            finalDecoratedBox.properties().get(
                                    DECORATION_POSITION)),
                    () -> assertEquals(emptyDecoration,
                            finalDecoratedBox.properties().get(DECORATION)),
                    () -> assertTrue(assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            finalDecoratedBox.slots().get(CHILD))
                            .child().isEmpty()),
                    () -> assertEquals(1, analyses.get(),
                            "Undo/Redo replays the exact analyzed pair"));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(finalDecoratedBox,
                    findModelWidget(saved.root(), decoratedBoxId));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void paletteExcludeSemanticsSaveReopenEditUndoRedoAndSaveRemainExact()
            throws Exception {
        StableId excludeSemanticsId = StableId.parse(
                "7a7a7a7a-7a7a-4a7a-8a7a-7a7a7a7a7a7a");
        WidgetTypeId excludeSemanticsType =
                ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE;
        PropertyValue.BooleanValue explicitFalse =
                new PropertyValue.BooleanValue(false);
        ExactPair durablePair;

        try (MutationFixture fixture = fixture(
                "mutation_palette_exclude_semantics_append", columnExactPair())) {
            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const ExcludeSemantics("));
                        assertFalse(request.content().contains("excluding:"),
                                "omission must preserve Flutter's true default");
                        assertTrue(request.content().contains("child: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            ready.document().orElseThrow(),
                            ready.catalog().orElseThrow(),
                            excludeSemanticsType,
                            COLUMN_ID,
                            CHILDREN,
                            2,
                            () -> excludeSemanticsId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class,
                    planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected terminal ExcludeSemantics insertion");

            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — append ExcludeSemantics to Column.children")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot added =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, excludeSemanticsId));
            WidgetNode excludeSemantics = findModelWidget(
                    added.document().orElseThrow().root(), excludeSemanticsId);
            assertAll(
                    () -> assertTrue(excludeSemantics.properties().isEmpty()),
                    () -> assertTrue(assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            excludeSemantics.slots().get(CHILD)).child().isEmpty()),
                    () -> assertEquals(1, analyses.get()));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durablePair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_palette_exclude_semantics_reopened", durablePair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedExcludeSemantics = findModelWidget(
                    reopened.document().orElseThrow().root(), excludeSemanticsId);
            assertTrue(reopenedExcludeSemantics.properties().isEmpty());
            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(excludeSemanticsType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedExcludeSemantics,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> excluding =
                    cellProperty(propertiesNode, "excluding");
            assertAll(
                    () -> assertTrue(excluding.canWrite(),
                            "reopened ExcludeSemantics must retain its boolean editor"),
                    () -> assertEquals(
                            FlutterPropertyCellValue.unset(),
                            excluding.getValue()),
                    () -> assertTrue(excluding.getPropertyEditor().isPaintable(),
                            "explicit booleans must retain the checkbox renderer"),
                    () -> assertNull(excluding.getPropertyEditor().getTags(),
                            "optional booleans must not regress to a combo editor"),
                    () -> assertEquals("Empty", java.util.Arrays.stream(
                            propertiesNode.getPropertySets())
                            .flatMap(set -> java.util.Arrays.stream(
                                    set.getProperties()))
                            .filter(property -> "child".equals(
                                    property.getName()))
                            .findFirst().orElseThrow().getValue()));

            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const ExcludeSemantics("));
                        assertTrue(request.content().contains(
                                "excluding: false"));
                        assertTrue(request.content().contains("child: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult edited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    excludeSemanticsId,
                                    EXCLUDING,
                                    explicitFalse),
                            "ExcludeSemantics.excluding")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    edited.outcome(), edited::reason);
            FlutterDesignerMutationController.Snapshot editedSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, excludeSemanticsId));
            assertEquals(explicitFalse, findModelWidget(
                    editedSnapshot.document().orElseThrow().root(),
                    excludeSemanticsId).properties().get(EXCLUDING));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            editedSnapshot.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, excludeSemanticsId));
            assertFalse(findModelWidget(
                    undone.document().orElseThrow().root(),
                    excludeSemanticsId).properties().containsKey(EXCLUDING));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, excludeSemanticsId));
            WidgetNode finalExcludeSemantics = findModelWidget(
                    redone.document().orElseThrow().root(), excludeSemanticsId);
            assertAll(
                    () -> assertEquals(explicitFalse,
                            finalExcludeSemantics.properties().get(EXCLUDING)),
                    () -> assertTrue(assertInstanceOf(
                            WidgetSlot.SingleSlot.class,
                            finalExcludeSemantics.slots().get(CHILD))
                            .child().isEmpty()),
                    () -> assertEquals(1, analyses.get(),
                            "Undo/Redo replays the exact analyzed pair"));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(finalExcludeSemantics,
                    findModelWidget(saved.root(), excludeSemanticsId));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void indexedStackSaveReopenNullIntegerUndoRedoRelationAndSaveRemainExact()
            throws Exception {
        WidgetTypeId indexedStackType =
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE;
        PropertyValue.NullValue explicitNull = new PropertyValue.NullValue();
        PropertyValue.IntegerValue selectedSecond =
                new PropertyValue.IntegerValue(java.math.BigInteger.ONE);
        ExactPair durableNullPair;

        try (MutationFixture fixture = fixture(
                "mutation_indexed_stack_save_null",
                indexedStackColumnExactPair())) {
            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const IndexedStack("));
                        assertTrue(request.content().contains("index: null"));
                        assertTrue(request.content().contains(
                                "'indexed first'"));
                        assertTrue(request.content().contains(
                                "'indexed second'"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            WidgetNode baseline = findModelWidget(
                    ready.document().orElseThrow().root(), INDEXED_STACK_ID);
            assertAll(
                    () -> assertFalse(baseline.properties().containsKey(INDEX),
                            "the durable baseline must preserve omitted index=0"),
                    () -> assertEquals(2, assertInstanceOf(
                            WidgetSlot.ListSlot.class,
                            baseline.slots().get(CHILDREN)).children().size()));

            FlutterDesignerMutationController.MutationResult setNull =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            new SetProperty(INDEXED_STACK_ID, INDEX, explicitNull),
                            "IndexedStack.index = null")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    setNull.outcome(), setNull::reason);
            FlutterDesignerMutationController.Snapshot nullSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, INDEXED_STACK_ID));
            assertEquals(explicitNull, findModelWidget(
                    nullSnapshot.document().orElseThrow().root(),
                    INDEXED_STACK_ID).properties().get(INDEX));
            assertEquals(1, analyses.get());

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            durableNullPair = new ExactPair(candidateDart, candidateFd);
        }

        try (MutationFixture fixture = fixture(
                "mutation_indexed_stack_reopen_integer",
                durableNullPair)) {
            FlutterDesignerMutationController.Snapshot reopened = fixture.ready();
            WidgetNode reopenedIndexedStack = findModelWidget(
                    reopened.document().orElseThrow().root(), INDEXED_STACK_ID);
            WidgetDefinition definition = reopened.catalog().orElseThrow()
                    .find(indexedStackType).orElseThrow();
            FlutterWidgetPropertiesNode propertiesNode =
                    new FlutterWidgetPropertiesNode(
                            Children.LEAF,
                            reopenedIndexedStack,
                            definition,
                            ignored -> { });
            Node.Property<FlutterPropertyCellValue> index =
                    cellProperty(propertiesNode, "index");
            java.beans.PropertyEditor indexEditor = index.getPropertyEditor();
            indexEditor.setValue(index.getValue());
            Node.Property<?> children = java.util.Arrays.stream(
                    propertiesNode.getPropertySets())
                    .flatMap(set -> java.util.Arrays.stream(set.getProperties()))
                    .filter(property -> "children".equals(property.getName()))
                    .findFirst().orElseThrow();
            assertAll(
                    () -> assertTrue(index.canWrite(),
                            "reopened IndexedStack must retain its index editor"),
                    () -> assertEquals(
                            FlutterPropertyCellValue.explicit(explicitNull),
                            index.getValue()),
                    () -> assertEquals("null", indexEditor.getAsText()),
                    () -> assertTrue(indexEditor.supportsCustomEditor()),
                    () -> assertEquals("2 widgets", children.getValue()),
                    () -> assertEquals(List.of(
                            INDEXED_FIRST_ID, INDEXED_SECOND_ID),
                            assertInstanceOf(
                                    WidgetSlot.ListSlot.class,
                                    reopenedIndexedStack.slots().get(CHILDREN))
                                    .children().stream()
                                    .map(WidgetNode::id)
                                    .toList()));

            AtomicInteger analyses = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analyses.incrementAndGet();
                        assertTrue(request.content().contains(
                                "const IndexedStack("));
                        assertTrue(request.content().contains("index: 1"));
                        assertFalse(request.content().contains("index: null"));
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });

            FlutterDesignerMutationController.MutationResult edited =
                    fixture.mutations().submit(
                            reopened.token().orElseThrow(),
                            new SetProperty(
                                    INDEXED_STACK_ID, INDEX, selectedSecond),
                            "IndexedStack.index = 1")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    edited.outcome(), edited::reason);
            FlutterDesignerMutationController.Snapshot integerSnapshot =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reopened.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, INDEXED_STACK_ID));
            assertEquals(selectedSecond, findModelWidget(
                    integerSnapshot.document().orElseThrow().root(),
                    INDEXED_STACK_ID).properties().get(INDEX));
            assertEquals(1, analyses.get());

            PropertyValue.IntegerValue outsideRange =
                    new PropertyValue.IntegerValue(
                            java.math.BigInteger.valueOf(2));
            FlutterDesignerMutationController.MutationResult rejected =
                    fixture.mutations().submit(
                            integerSnapshot.token().orElseThrow(),
                            new SetProperty(
                                    INDEXED_STACK_ID, INDEX, outsideRange),
                            "IndexedStack.index = 2")
                            .get(10, TimeUnit.SECONDS);
            FlutterDesignerMutationController.Snapshot afterRejected =
                    fixture.mutations().snapshot();
            assertAll(
                    () -> assertEquals(
                            FlutterDesignerMutationController.Outcome.REJECTED,
                            rejected.outcome(), rejected::reason),
                    () -> assertTrue(rejected.reason().contains(
                            "IndexedStack index 2 is outside the valid range for 2 children"),
                            rejected::reason),
                    () -> assertEquals(1, analyses.get(),
                            "relationship rejection must happen before analyzer admission"),
                    () -> assertEquals(
                            FlutterDesignerMutationController.Status.READY,
                            afterRejected.status()),
                    () -> assertEquals(selectedSecond, findModelWidget(
                            afterRejected.document().orElseThrow().root(),
                            INDEXED_STACK_ID).properties().get(INDEX)));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            afterRejected.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, INDEXED_STACK_ID));
            assertEquals(explicitNull, findModelWidget(
                    undone.document().orElseThrow().root(),
                    INDEXED_STACK_ID).properties().get(INDEX),
                    "chronological Undo must restore the explicit null state");

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            List.of(FIRST_ID, SECOND_ID, INDEXED_STACK_ID));
            WidgetNode finalIndexedStack = findModelWidget(
                    redone.document().orElseThrow().root(), INDEXED_STACK_ID);
            assertAll(
                    () -> assertEquals(selectedSecond,
                            finalIndexedStack.properties().get(INDEX),
                            "chronological Redo must restore the integer state"),
                    () -> assertEquals(List.of(
                            INDEXED_FIRST_ID, INDEXED_SECOND_ID),
                            assertInstanceOf(
                                    WidgetSlot.ListSlot.class,
                                    finalIndexedStack.slots().get(CHILDREN))
                                    .children().stream()
                                    .map(WidgetNode::id)
                                    .toList()),
                    () -> assertEquals(1, analyses.get(),
                            "Undo/Redo must replay the exact analyzed pair"));

            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] candidateDart = evidence.candidateDartBytes();
            byte[] candidateFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), candidateFd, candidateDart);
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(candidateFd)).document();
            assertEquals(finalIndexedStack,
                    findModelWidget(saved.root(), INDEXED_STACK_ID));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void deletedCenterTextCanBeDroppedBackThroughTheExactMutationPipeline()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_palette_center_text_readd",
                scaffoldCenterTextExactPair());
        StableId replacementTextId = StableId.parse(
                "44444444-4444-4444-8444-444444444444");

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            FlutterDesignerMutationController.MutationResult removed =
                    fixture.mutations().submit(
                            ready.token().orElseThrow(),
                            new RemoveWidget(NESTED_TEXT_ID),
                            "home_page.fd — remove Text from Center.child")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    removed.outcome(), removed::reason);
            FlutterDesignerMutationController.Snapshot emptyCenter =
                    awaitReadyWithCenterChildAfterToken(
                            fixture.mutations(),
                            ready.token().orElseThrow(),
                            null);

            FlutterDesignerPaletteDropPlanner.Result planned =
                    new FlutterDesignerPaletteDropPlanner().plan(
                            emptyCenter.document().orElseThrow(),
                            emptyCenter.catalog().orElseThrow(),
                            new WidgetTypeId("flutter.widgets.Text"),
                            CENTER_ID,
                            CHILD,
                            0,
                            () -> replacementTextId);
            FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                    FlutterDesignerPaletteDropPlanner.Accepted.class, planned,
                    () -> planned instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                            ? rejected.code() + ": " + rejected.reason()
                            : "Expected a Center.child Text placement");

            FlutterDesignerMutationController.MutationResult added =
                    fixture.mutations().submit(
                            emptyCenter.token().orElseThrow(),
                            accepted.command(),
                            "home_page.fd — add Text to Center.child")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    added.outcome(), added::reason);
            FlutterDesignerMutationController.Snapshot readded =
                    awaitReadyWithCenterChildAfterToken(
                            fixture.mutations(),
                            emptyCenter.token().orElseThrow(),
                            replacementTextId);

            WidgetNode center = findModelWidget(
                    readded.document().orElseThrow().root(), CENTER_ID);
            WidgetSlot.SingleSlot childSlot = assertInstanceOf(
                    WidgetSlot.SingleSlot.class, center.slots().get(CHILD));
            WidgetNode replacement = childSlot.child().orElseThrow();
            assertEquals(replacementTextId, replacement.id());
            assertEquals(new WidgetTypeId("flutter.widgets.Text"),
                    replacement.type());
            assertEquals(new PropertyValue.StringValue("Text"),
                    replacement.properties().get(DATA));
            assertEquals(2, fixture.analysisCalls().get(),
                    "Delete and re-add must each analyze exactly one candidate");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertEquals(readded.document().orElseThrow(),
                    fixture.coordinator().stagedEvidence()
                            .preparedPairIdentity().prospectiveDocument());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(combined::undo);
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithCenterChildAfterToken(
                            fixture.mutations(),
                            readded.token().orElseThrow(),
                            null);
            onEdt(combined::redo);
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithCenterChildAfterToken(
                            fixture.mutations(),
                            undone.token().orElseThrow(),
                            replacementTextId);
            assertEquals(readded.document().orElseThrow(),
                    redone.document().orElseThrow());
            assertEquals(2, fixture.analysisCalls().get(),
                    "Undo and Redo must reuse the exact analyzed pairs");
        }
    }

    @Test
    void deleteActionRemovesSelectedColumnChildAndPreservesExactUndoRedoPipeline()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_delete_widget", columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node first = awaitWidgetNode(design, FIRST_ID);
                Action delete = deleteWidgetAction(design);
                onEdt(() -> {
                    design.getExplorerManager().setSelectedNodes(
                            new Node[]{first});
                    assertTrue(delete.isEnabled(),
                            "exactly one selected non-root widget must enable Delete");
                    delete.actionPerformed(new ActionEvent(
                            design,
                            ActionEvent.ACTION_PERFORMED,
                            FlutterDesignerMultiViewDesign
                                    .DELETE_WIDGET_ACTION_KEY));
                });

                FlutterDesignerMutationController.Snapshot applied =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                ready.token().orElseThrow(),
                                List.of(SECOND_ID));
                awaitSelectedWidget(design, COLUMN_ID);
                onEdt(() -> {
                    assertEquals(
                            COLUMN_ID,
                            design.getLookup().lookup(Node.class)
                                    .getLookup().lookup(StableId.class));
                    assertEquals(
                            applied.document().orElseThrow(),
                            design.currentCanvasDocumentForTests(),
                            "tree selection and Canvas must consume the confirmed delete revision");
                    assertFalse(delete.isEnabled(),
                            "the root selected after deletion must not be deletable");
                });
                assertEquals(1, fixture.analysisCalls().get());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                assertNotNull(fixture.coordinator().stagedEvidence());
                assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

                DesignerCombinedUndoRedo combined = fixture.dataObject()
                        .getCombinedUndoRedo();
                onEdt(() -> {
                    assertTrue(combined.canUndo());
                    assertFalse(combined.canRedo());
                    combined.undo();
                });

                FlutterDesignerMutationController.Snapshot undone =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                applied.token().orElseThrow(),
                                List.of(FIRST_ID, SECOND_ID));
                assertEquals(ready.document().orElseThrow(),
                        undone.document().orElseThrow(),
                        "Undo must restore the exact deleted widget identity and order");
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.coordinator().stagedEvidence());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));

                onEdt(() -> {
                    assertTrue(combined.canRedo());
                    combined.redo();
                });

                FlutterDesignerMutationController.Snapshot redone =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                undone.token().orElseThrow(),
                                List.of(SECOND_ID));
                awaitSelectedWidget(design, COLUMN_ID);
                assertEquals(applied.document().orElseThrow(),
                        redone.document().orElseThrow(),
                        "Redo must replay the exact analyzed deletion");
                assertEquals(1, fixture.analysisCalls().get(),
                        "Undo and Redo must not rerun Dart analysis");
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void deleteActionCascadesAcrossSelectedCenterSubtreeWithExactUndoRedo()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_delete_widget_subtree",
                scaffoldCenterTextExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            DesignerDocument exactNestedDocument =
                    ready.document().orElseThrow();
            assertTrue(containsWidget(exactNestedDocument.root(), CENTER_ID));
            assertTrue(containsWidget(exactNestedDocument.root(), NESTED_TEXT_ID));
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node center = awaitWidgetNode(design, CENTER_ID);
                Action delete = deleteWidgetAction(design);
                onEdt(() -> {
                    design.getExplorerManager().setSelectedNodes(
                            new Node[]{center});
                    assertTrue(delete.isEnabled());
                    delete.actionPerformed(new ActionEvent(
                            design,
                            ActionEvent.ACTION_PERFORMED,
                            FlutterDesignerMultiViewDesign
                                    .DELETE_WIDGET_ACTION_KEY));
                });

                FlutterDesignerMutationController.Snapshot applied =
                        awaitReadyWithScaffoldBodyAfterToken(
                                fixture.mutations(),
                                ready.token().orElseThrow(),
                                false);
                DesignerDocument deletedDocument =
                        applied.document().orElseThrow();
                assertEquals(SCAFFOLD_ID, deletedDocument.root().id(),
                        "cascade deletion must preserve the required root");
                assertFalse(containsWidget(deletedDocument.root(), CENTER_ID));
                assertFalse(containsWidget(
                        deletedDocument.root(), NESTED_TEXT_ID),
                        "deleting Center must delete its nested Text atomically");
                awaitSelectedWidget(design, SCAFFOLD_ID);
                onEdt(() -> assertFalse(delete.isEnabled(),
                        "the preserved Scaffold root must remain non-deletable"));

                assertEquals(1, fixture.analysisCalls().get());
                String analyzedAfterDelete = fixture.analyzedContents().getFirst();
                assertTrue(analyzedAfterDelete.contains("Scaffold("),
                        analyzedAfterDelete);
                assertFalse(analyzedAfterDelete.contains("Center("),
                        analyzedAfterDelete);
                assertFalse(analyzedAfterDelete.contains("Text("),
                        analyzedAfterDelete);
                assertFalse(analyzedAfterDelete.contains("nested subtree"),
                        analyzedAfterDelete);
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                assertNotNull(fixture.coordinator().stagedEvidence());
                assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

                DesignerCombinedUndoRedo combined = fixture.dataObject()
                        .getCombinedUndoRedo();
                onEdt(() -> {
                    assertTrue(combined.canUndo());
                    combined.undo();
                });

                FlutterDesignerMutationController.Snapshot undone =
                        awaitReadyWithScaffoldBodyAfterToken(
                                fixture.mutations(),
                                applied.token().orElseThrow(),
                                true);
                assertEquals(exactNestedDocument,
                        undone.document().orElseThrow(),
                        "Undo must restore the exact nested Center/Text subtree");
                assertTrue(containsWidget(
                        undone.document().orElseThrow().root(), CENTER_ID));
                assertTrue(containsWidget(
                        undone.document().orElseThrow().root(), NESTED_TEXT_ID));
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());

                onEdt(() -> {
                    assertTrue(combined.canRedo());
                    combined.redo();
                });

                FlutterDesignerMutationController.Snapshot redone =
                        awaitReadyWithScaffoldBodyAfterToken(
                                fixture.mutations(),
                                undone.token().orElseThrow(),
                                false);
                assertEquals(deletedDocument,
                        redone.document().orElseThrow(),
                        "Redo must replay the exact analyzed cascade deletion");
                assertFalse(containsWidget(
                        redone.document().orElseThrow().root(), CENTER_ID));
                assertFalse(containsWidget(
                        redone.document().orElseThrow().root(), NESTED_TEXT_ID));
                awaitSelectedWidget(design, SCAFFOLD_ID);
                assertEquals(1, fixture.analysisCalls().get(),
                        "semantic Undo/Redo must reuse the analyzed pair");
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void deleteActionRejectsRootAndEmptySelectionBeforeAnalyzerAdmission()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_delete_widget_no_selection",
                columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot ready = fixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node root = awaitWidgetNode(design, COLUMN_ID);
                Action delete = deleteWidgetAction(design);
                onEdt(() -> {
                    design.getExplorerManager().setSelectedNodes(
                            new Node[]{root});
                    assertFalse(delete.isEnabled(),
                            "the document root must never enable widget deletion");
                    delete.actionPerformed(new ActionEvent(
                            design,
                            ActionEvent.ACTION_PERFORMED,
                            FlutterDesignerMultiViewDesign
                                    .DELETE_WIDGET_ACTION_KEY));

                    design.getExplorerManager().setSelectedNodes(new Node[0]);
                    assertFalse(delete.isEnabled(),
                            "an empty selection must keep Delete disabled");
                    delete.actionPerformed(new ActionEvent(
                            design,
                            ActionEvent.ACTION_PERFORMED,
                            FlutterDesignerMultiViewDesign
                                    .DELETE_WIDGET_ACTION_KEY));
                });

                onEdt(() -> {
                    // Drain action/listener work posted by the MultiView.
                });
                assertEquals(0, fixture.analysisCalls().get(),
                        "root and empty selection must reject before analyzer admission");
                assertSame(ready.token().orElseThrow(),
                        fixture.mutations().snapshot().token().orElseThrow());
                assertEquals(ready.document().orElseThrow(),
                        fixture.mutations().snapshot().document().orElseThrow());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.coordinator().stagedEvidence());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));
                assertFalse(fixture.dataObject().getCombinedUndoRedo()
                        .designerSessionActive());
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void canvasSelectionRefreshesDeleteActionForChildAndRoot() throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_delete_canvas_selection_action",
                columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, FIRST_ID);
                Action delete = deleteWidgetAction(design);

                onEdt(() -> {
                    selectWidgetFromCanvas(design, FIRST_ID);
                    assertEquals(FIRST_ID, selectedWidgetId(design));
                    assertTrue(delete.isEnabled(),
                            "Canvas selection of a child must enable Delete");

                    selectWidgetFromCanvas(design, COLUMN_ID);
                    assertEquals(COLUMN_ID, selectedWidgetId(design));
                    assertFalse(delete.isEnabled(),
                            "Canvas selection of the root must disable Delete");
                });

                assertEquals(0, fixture.analysisCalls().get(),
                        "selection synchronization must not submit a mutation");
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void multiViewTypedBooleanSetAndResetAreOneShotAndRefreshExactProperties()
            throws Exception {
        MutationFixture fixture = fixture("mutation_multiview_typed_properties");
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);

                Node baselineNode = awaitSelectedTextSoftWrap(
                        design, null, true);
                onEdt(() -> {
                    Node.Property<FlutterPropertyCellValue> softWrap =
                            cellProperty(baselineNode, "softWrap");
                    assertTrue(softWrap.supportsDefaultValue());
                    assertTrue(softWrap.isDefaultValue());
                    softWrap.setValue(FlutterPropertyCellValue.explicit(
                            new PropertyValue.BooleanValue(false)));

                    // Every Node in one immutable presentation shares the same
                    // admission guard. A second Property Sheet event from that
                    // stale presentation must not become another command.
                    cellProperty(baselineNode, "textAlign").setValue(
                            FlutterPropertyCellValue.explicit(
                                    new PropertyValue.EnumValue(
                                            "TextAlign", "center")));
                });

                FlutterDesignerMutationController.Snapshot applied =
                        awaitReadyWithSoftWrap(fixture.mutations(), false);
                assertEquals(1, fixture.analysisCalls().get(),
                        "one immutable Properties presentation may submit only once");
                assertTrue(fixture.analyzedContents().getFirst()
                        .contains("softWrap: false"));
                assertFalse(fixture.analyzedContents().getFirst()
                        .contains("textAlign:"));
                Node appliedNode = awaitSelectedTextSoftWrap(
                        design, false, true);
                assertSame(baselineNode, appliedNode,
                        "the confirmed typed value must refresh the selected Node in place");
                assertEquals("before", textData(applied));

                onEdt(() -> {
                    Node.Property<FlutterPropertyCellValue> softWrap =
                            cellProperty(appliedNode, "softWrap");
                    assertFalse(softWrap.isDefaultValue());
                    softWrap.restoreDefaultValue();
                });

                FlutterDesignerMutationController.Snapshot reset =
                        awaitReadyWithSoftWrap(fixture.mutations(), null);
                assertEquals(1, fixture.analysisCalls().get(),
                        "resetting to the exact durable baseline must reuse its validation");
                Node resetNode = awaitSelectedTextSoftWrap(
                        design, null, true);
                assertSame(appliedNode, resetNode,
                        "ResetProperty must refresh the selected Properties Node in place");
                assertEquals("before", textData(reset));
                assertArrayEquals(fixture.baselineDart(),
                        fixture.editor().liveSnapshot().markerBearingUtf8());
                assertArrayEquals(fixture.baselineFd(),
                        ((DesignerCommandSessionOrchestrator) sessionOwner(
                                fixture.mutations())).currentRevision().fdBytes());
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void scaffoldPropertySetResetPersistAndReplayThroughUndoRedo()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_scaffold_property_history",
                scaffoldCenterTextExactPair());
        PropertyName primary = new PropertyName("primary");
        PropertyValue.BooleanValue explicitFalse =
                new PropertyValue.BooleanValue(false);
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.MutationResult set =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            new SetProperty(SCAFFOLD_ID, primary, explicitFalse),
                            "Scaffold.primary")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    set.outcome(), set::reason);
            FlutterDesignerMutationController.Snapshot setSnapshot =
                    awaitReadyWithScaffoldPropertyAfterToken(
                            fixture.mutations(), primary, explicitFalse,
                            baseline.token().orElseThrow());
            assertTrue(fixture.analyzedContents().getLast()
                    .contains("primary: false"));
            PairSaveEvidence setEvidence = fixture.coordinator().stagedEvidence();
            assertNotNull(setEvidence);
            SaveCookie saveSet = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(saveSet);
            saveSet.save();
            awaitCurrentWithPair(
                    fixture.controller(),
                    setEvidence.preparedPairIdentity().prospectiveFdBytes(),
                    setEvidence.candidateDartBytes());
            FlutterDesignerMutationController.Snapshot savedSet =
                    awaitReadyWithScaffoldPropertyAfterToken(
                            fixture.mutations(), primary, explicitFalse,
                            setSnapshot.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            DesignerDocument durableSet = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(Files.readAllBytes(
                            fixture.fdPath()))).document();
            assertEquals(explicitFalse,
                    durableSet.root().properties().get(primary));

            FlutterDesignerMutationController.MutationResult reset =
                    fixture.mutations().submit(
                            savedSet.token().orElseThrow(),
                            new ResetProperty(SCAFFOLD_ID, primary),
                            "Scaffold.primary")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    reset.outcome(), reset::reason);
            FlutterDesignerMutationController.Snapshot resetSnapshot =
                    awaitReadyWithScaffoldPropertyAfterToken(
                            fixture.mutations(), primary, null,
                            savedSet.token().orElseThrow());
            assertFalse(fixture.analyzedContents().getLast()
                    .contains("primary:"));
            assertEquals(2, fixture.analysisCalls().get());

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithScaffoldPropertyAfterToken(
                            fixture.mutations(), primary, explicitFalse,
                            resetSnapshot.token().orElseThrow());
            assertTrue(new String(
                    fixture.editor().liveSnapshot().markerBearingUtf8(),
                    StandardCharsets.UTF_8).contains("primary: false"));

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithScaffoldPropertyAfterToken(
                            fixture.mutations(), primary, null,
                            undone.token().orElseThrow());
            assertFalse(new String(
                    fixture.editor().liveSnapshot().markerBearingUtf8(),
                    StandardCharsets.UTF_8).contains("primary:"));
            assertEquals(2, fixture.analysisCalls().get(),
                    "Undo/Redo must replay validated paired revisions");

            PairSaveEvidence resetEvidence = fixture.coordinator().stagedEvidence();
            assertNotNull(resetEvidence);
            SaveCookie saveReset = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(saveReset);
            saveReset.save();
            awaitCurrentWithPair(
                    fixture.controller(),
                    resetEvidence.preparedPairIdentity().prospectiveFdBytes(),
                    resetEvidence.candidateDartBytes());
            awaitReadyWithScaffoldPropertyAfterToken(
                    fixture.mutations(), primary, null,
                    redone.token().orElseThrow());
            DesignerDocument durableReset = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(Files.readAllBytes(
                            fixture.fdPath()))).document();
            assertFalse(durableReset.root().properties().containsKey(primary));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void multiViewRefreshesStablePropertyNodeWithNextRevisionAuthority()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_stable_property_node_authority");
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();

        try (fixture) {
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);
                Node stableNode = awaitSelectedTextSoftWrap(design, null, true);
                Node.Property<FlutterPropertyCellValue> stableSoftWrap =
                        cellProperty(stableNode, "softWrap");

                FlutterDesignerMutationController.Snapshot baseline =
                        fixture.mutations().snapshot();
                FlutterDesignerMutationController.MutationResult external =
                        fixture.mutations().submit(
                                baseline.token().orElseThrow(),
                                setText("external"),
                                "Text.data")
                                .get(10, TimeUnit.SECONDS);
                assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                        external.outcome(), external::reason);
                awaitReadyWithData(fixture.mutations(), "external");
                Node currentNode = awaitSelectedTextNode(design, "external", true);
                assertSame(stableNode, currentNode,
                        "an external property-only revision must refresh the Node in place");
                assertSame(stableSoftWrap,
                        cellProperty(currentNode, "softWrap"),
                        "property identity must remain stable across READY revisions");
                assertEquals(1, fixture.analysisCalls().get());

                FlutterPropertyCellValue explicitFalse =
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(false));
                onEdt(() -> {
                    stableSoftWrap.setValue(explicitFalse);
                    stableSoftWrap.setValue(explicitFalse);
                });
                awaitReadyWithSoftWrap(fixture.mutations(), false);
                Node falseNode = awaitSelectedTextSoftWrap(design, false, true);
                assertSame(stableNode, falseNode);
                assertSame(stableSoftWrap, cellProperty(falseNode, "softWrap"));
                assertEquals(explicitFalse, stableSoftWrap.getValue(),
                        "the stable Property must expose the confirmed value");
                assertEquals(2, fixture.analysisCalls().get(),
                        "one refreshed revision lease must still submit only once");

                FlutterPropertyCellValue explicitTrue =
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(true));
                onEdt(() -> stableSoftWrap.setValue(explicitTrue));
                awaitReadyWithSoftWrap(fixture.mutations(), true);
                Node trueNode = awaitSelectedTextSoftWrap(design, true, true);
                assertSame(stableNode, trueNode);
                assertSame(stableSoftWrap, cellProperty(trueNode, "softWrap"));
                assertEquals(explicitTrue, stableSoftWrap.getValue(),
                        "the same Property must be rearmed for the next READY revision");
                assertEquals(3, fixture.analysisCalls().get());
                assertEquals("external", textData(fixture.mutations().snapshot()));
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void multiViewSlotBridgeMapsMoveAddRemoveSelectionSaveAndUndoRedo()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_slot_bridge", columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        List<String> failures = new CopyOnWriteArrayList<>();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, COLUMN_ID);
                awaitStableSlotPresentation(design, fixture.mutations());

                assertApplied(submitCurrentSlotMutation(
                        design,
                        failures,
                        new FlutterWidgetSlotMutation.Move(
                                COLUMN_ID, CHILDREN, FIRST_ID, 1)));
                awaitSelectedWidget(design, FIRST_ID);
                byte[] movedFd = Files.readAllBytes(fixture.fdPath());
                assertFalse(Arrays.equals(fixture.baselineFd(), movedFd));
                awaitCurrentWithPair(
                        fixture.controller(), movedFd, fixture.baselineDart());
                FlutterDesignerMutationController.Snapshot moved =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                baseline.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID));
                awaitCanvasDocument(design, moved.document().orElseThrow());
                assertEquals(0, fixture.analysisCalls().get(),
                        "same-shape reorder must retain the exact FD_ONLY path");
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());

                awaitStableSlotPresentation(design, fixture.mutations());
                assertApplied(submitCurrentSlotMutation(
                        design,
                        failures,
                        new FlutterWidgetSlotMutation.Add(
                                COLUMN_ID,
                                CHILDREN,
                                new WidgetTypeId("flutter.widgets.Text"),
                                2)));
                assertEquals(List.of(), failures,
                        "the fresh Add handler must pass exact slot admission");
                FlutterDesignerMutationController.Snapshot added =
                        awaitReadyWithColumnChildCountAfterToken(
                                fixture.mutations(),
                                moved.token().orElseThrow(),
                                3);
                WidgetSlot.ListSlot addedChildren = assertInstanceOf(
                        WidgetSlot.ListSlot.class,
                        added.document().orElseThrow().root().slots().get(CHILDREN));
                assertEquals(List.of(SECOND_ID, FIRST_ID),
                        addedChildren.children().subList(0, 2).stream()
                                .map(WidgetNode::id)
                                .toList());
                WidgetNode appended = addedChildren.children().get(2);
                StableId addedId = appended.id();
                assertEquals(new WidgetTypeId("flutter.widgets.Text"),
                        appended.type());
                assertEquals(new PropertyValue.StringValue("Text"),
                        appended.properties().get(DATA));
                awaitSelectedWidget(design, addedId);
                awaitCanvasDocument(design, added.document().orElseThrow());
                assertEquals(1, fixture.analysisCalls().get());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                PairSaveEvidence addEvidence = fixture.coordinator().stagedEvidence();
                assertNotNull(addEvidence);
                byte[] addedDart = addEvidence.candidateDartBytes();
                byte[] addedFd = addEvidence.preparedPairIdentity()
                        .prospectiveFdBytes();
                SaveCookie addSave = fixture.dataObject().getCookie(SaveCookie.class);
                assertNotNull(addSave);
                addSave.save();
                awaitCurrentWithPair(fixture.controller(), addedFd, addedDart);
                FlutterDesignerMutationController.Snapshot savedAdd =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                added.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID, addedId));
                awaitCanvasDocument(design, savedAdd.document().orElseThrow());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());

                awaitStableSlotPresentation(design, fixture.mutations());
                assertApplied(submitCurrentSlotMutation(
                        design,
                        failures,
                        new FlutterWidgetSlotMutation.Remove(
                                COLUMN_ID, CHILDREN, addedId)));
                FlutterDesignerMutationController.Snapshot removed =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                savedAdd.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID));
                awaitSelectedWidget(design, COLUMN_ID);
                awaitCanvasDocument(design, removed.document().orElseThrow());
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                PairSaveEvidence removeEvidence =
                        fixture.coordinator().stagedEvidence();
                assertNotNull(removeEvidence);
                byte[] removedDart = removeEvidence.candidateDartBytes();
                byte[] removedFd = removeEvidence.preparedPairIdentity()
                        .prospectiveFdBytes();

                DesignerCombinedUndoRedo combined = fixture.dataObject()
                        .getCombinedUndoRedo();
                onEdt(() -> {
                    assertTrue(combined.canUndo());
                    combined.undo();
                });
                FlutterDesignerMutationController.Snapshot undone =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                removed.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID, addedId));
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                onEdt(() -> {
                    assertTrue(combined.canRedo());
                    combined.redo();
                });
                FlutterDesignerMutationController.Snapshot redone =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                undone.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID));
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());
                assertArrayEquals(removedDart,
                        fixture.coordinator().stagedEvidence().candidateDartBytes());
                assertArrayEquals(removedFd,
                        fixture.coordinator().stagedEvidence()
                                .preparedPairIdentity().prospectiveFdBytes());

                SaveCookie removeSave =
                        fixture.dataObject().getCookie(SaveCookie.class);
                assertNotNull(removeSave);
                removeSave.save();
                awaitCurrentWithPair(fixture.controller(), removedFd, removedDart);
                FlutterDesignerMutationController.Snapshot savedRemove =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                redone.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID));
                awaitCanvasDocument(design, savedRemove.document().orElseThrow());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));
                assertColumnChildren(
                        assertInstanceOf(
                                FdDecodeResult.Current.class,
                                new FdDocumentCodec().decode(
                                        Files.readAllBytes(fixture.fdPath())))
                                .document(),
                        List.of(SECOND_ID, FIRST_ID));
                assertEquals(List.of(), failures);
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void multiViewSlotBridgeMapsAtomicReplaceAndClearAllWithRevisionAndSubmitGuards()
            throws Exception {
        MutationFixture replaceFixture = fixture(
                "mutation_multiview_slot_atomic_replace",
                scaffoldCenterTextExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> replaceDesignRef =
                new AtomicReference<>();
        List<String> replaceFailures = new CopyOnWriteArrayList<>();

        try (replaceFixture) {
            FlutterDesignerMutationController.Snapshot baseline =
                    replaceFixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    replaceFixture.dataObject().getLookup());
                    replaceDesignRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = replaceDesignRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, SCAFFOLD_ID);
                awaitStableSlotPresentation(design, replaceFixture.mutations());
                CompletableFuture<FlutterDesignerMutationController.MutationResult>
                        completion = new CompletableFuture<>();
                AtomicReference<FlutterWidgetPropertiesNode.SlotMutationHandler>
                        handler = new AtomicReference<>();
                FlutterWidgetSlotMutation.Replace replace =
                        new FlutterWidgetSlotMutation.Replace(
                                SCAFFOLD_ID,
                                BODY,
                                CENTER_ID,
                                new FlutterWidgetSlotMutation.Replace.NewWidget(
                                        new WidgetTypeId("flutter.widgets.Text")));
                onEdt(() -> {
                    handler.set(design.slotMutationHandlerForTests(
                            (intent, reason) -> replaceFailures.add(reason),
                            completion::complete));
                    assertNotNull(handler.get());
                    handler.get().submit(replace);
                    handler.get().submit(replace);
                });
                assertApplied(completion.get(10, TimeUnit.SECONDS));
                FlutterDesignerMutationController.Snapshot replaced =
                        awaitReadyWithScaffoldBodyAfterToken(
                                replaceFixture.mutations(),
                                baseline.token().orElseThrow(),
                                false);
                WidgetSlot.SingleSlot body = assertInstanceOf(
                        WidgetSlot.SingleSlot.class,
                        replaced.document().orElseThrow().root().slots().get(BODY));
                WidgetNode replacement = body.child().orElseThrow();
                assertEquals(new WidgetTypeId("flutter.widgets.Text"),
                        replacement.type());
                assertEquals(new PropertyValue.StringValue("Text"),
                        replacement.properties().get(DATA));
                awaitSelectedWidget(design, replacement.id());
                assertEquals(1, replaceFixture.analysisCalls().get(),
                        "double submit must admit one atomic replacement only");
                assertEquals(List.of(), replaceFailures);
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        replaceFixture.coordinator().state().status());
            } finally {
                FlutterDesignerMultiViewDesign design = replaceDesignRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }

        MutationFixture clearFixture = fixture(
                "mutation_multiview_slot_atomic_clear_all",
                columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> clearDesignRef =
                new AtomicReference<>();
        List<String> clearFailures = new CopyOnWriteArrayList<>();

        try (clearFixture) {
            FlutterDesignerMutationController.Snapshot baseline =
                    clearFixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    clearFixture.dataObject().getLookup());
                    clearDesignRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = clearDesignRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, COLUMN_ID);
                awaitStableSlotPresentation(design, clearFixture.mutations());

                FlutterWidgetPropertiesNode.SlotMutationHandler stale =
                        awaitSlotMutationHandler(design, clearFailures);
                onEdt(() -> stale.submit(new FlutterWidgetSlotMutation.ClearAll(
                        COLUMN_ID,
                        CHILDREN,
                        List.of(SECOND_ID, FIRST_ID))));
                assertEquals(1, clearFailures.size());
                assertTrue(clearFailures.getFirst().contains(
                        "changed after the editor opened"), clearFailures::toString);
                assertSame(baseline.token().orElseThrow(),
                        clearFixture.mutations().snapshot().token().orElseThrow());
                assertEquals(0, clearFixture.analysisCalls().get(),
                        "stale ordered ids must reject before analyzer admission");

                awaitStableSlotPresentation(design, clearFixture.mutations());
                assertApplied(submitCurrentSlotMutation(
                        design,
                        clearFailures,
                        new FlutterWidgetSlotMutation.ClearAll(
                                COLUMN_ID,
                                CHILDREN,
                                List.of(FIRST_ID, SECOND_ID))));
                FlutterDesignerMutationController.Snapshot cleared =
                        awaitReadyWithEmptyColumnAfterToken(
                                clearFixture.mutations(),
                                baseline.token().orElseThrow());
                awaitSelectedWidget(design, COLUMN_ID);
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        clearFixture.coordinator().state().status());

                DesignerCombinedUndoRedo combined = clearFixture.dataObject()
                        .getCombinedUndoRedo();
                onEdt(() -> {
                    assertTrue(combined.canUndo());
                    combined.undo();
                });
                FlutterDesignerMutationController.Snapshot undone =
                        awaitReadyWithColumnChildIdsAfterToken(
                                clearFixture.mutations(),
                                cleared.token().orElseThrow(),
                                List.of(FIRST_ID, SECOND_ID));
                onEdt(() -> {
                    assertTrue(combined.canRedo());
                    combined.redo();
                });
                awaitReadyWithEmptyColumnAfterToken(
                        clearFixture.mutations(),
                        undone.token().orElseThrow());
                assertEquals(1, clearFixture.analysisCalls().get(),
                        "Clear All and exact Undo/Redo reuse one generated pair");
                assertEquals(1, clearFailures.size(),
                        "only the deliberate stale intent may report a failure");
            } finally {
                FlutterDesignerMultiViewDesign design = clearDesignRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void multiViewSlotBridgeMovesExistingReplacementThroughSaveAndReopen()
            throws Exception {
        ExactPair durablePair;
        MutationFixture fixture = fixture(
                "mutation_multiview_slot_existing_replace_save",
                centerAndSourceExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        List<String> failures = new CopyOnWriteArrayList<>();

        try (fixture) {
            try {
                FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, CENTER_ID);
                awaitStableSlotPresentation(design, fixture.mutations());

                assertApplied(submitCurrentSlotMutation(
                        design,
                        failures,
                        new FlutterWidgetSlotMutation.Replace(
                                CENTER_ID,
                                CHILD,
                                FIRST_ID,
                                new FlutterWidgetSlotMutation.Replace.ExistingWidget(
                                        SECOND_ID))));
                FlutterDesignerMutationController.Snapshot replaced =
                        awaitReadyWithCenterChildAfterToken(
                                fixture.mutations(),
                                baseline.token().orElseThrow(),
                                SECOND_ID);
                assertExistingReplacement(replaced.document().orElseThrow());
                awaitSelectedWidget(design, SECOND_ID);
                assertEquals(1, fixture.analysisCalls().get());
                assertEquals(List.of(), failures);
                assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                        fixture.coordinator().state().status());

                PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
                assertNotNull(evidence);
                byte[] savedDart = evidence.candidateDartBytes();
                byte[] savedFd = evidence.preparedPairIdentity().prospectiveFdBytes();
                SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
                assertNotNull(save);
                save.save();
                awaitCurrentWithPair(
                        fixture.controller(), savedFd, savedDart);
                FlutterDesignerMutationController.Snapshot saved =
                        awaitReadyWithCenterChildAfterToken(
                                fixture.mutations(),
                                replaced.token().orElseThrow(),
                                SECOND_ID);
                assertExistingReplacement(saved.document().orElseThrow());
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));
                assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()));
                assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()));
                durablePair = new ExactPair(savedDart, savedFd);
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }

        // Reopen through a clean DataObject/controller session. Reusing the
        // first DataObject would intentionally retain its Designer Undo/Redo
        // identity and would not model an application-level reopen.
        MutationFixture reopened = fixture(
                "mutation_multiview_slot_existing_replace_reopen",
                durablePair);
        AtomicReference<FlutterDesignerMultiViewDesign> reopenedDesignRef =
                new AtomicReference<>();
        try (reopened) {
            try {
                assertArrayEquals(durablePair.dartBytes(),
                        Files.readAllBytes(reopened.dartPath()));
                assertArrayEquals(durablePair.fdBytes(),
                        Files.readAllBytes(reopened.fdPath()));
                assertExistingReplacement(
                        reopened.ready().document().orElseThrow());
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign reopenedDesign =
                            new FlutterDesignerMultiViewDesign(
                                    reopened.dataObject().getLookup());
                    reopenedDesignRef.set(reopenedDesign);
                    reopenedDesign.componentOpened();
                });
                FlutterDesignerMultiViewDesign reopenedDesign =
                        reopenedDesignRef.get();
                assertNotNull(reopenedDesign);
                awaitWidgetNode(reopenedDesign, CENTER_ID);
                awaitWidgetNode(reopenedDesign, SECOND_ID);
                awaitStableSlotPresentation(reopenedDesign, reopened.mutations());
                assertExistingReplacement(
                        reopened.mutations().snapshot().document().orElseThrow());
                assertEquals(0, reopened.analysisCalls().get(),
                        "reopening the exact saved pair must not synthesize a mutation");
            } finally {
                FlutterDesignerMultiViewDesign reopenedDesign =
                        reopenedDesignRef.get();
                if (reopenedDesign != null) {
                    onEdt(reopenedDesign::componentClosed);
                }
            }
        }
    }

    @Test
    void multiViewSlotBridgeRejectsCapturedHandlerAfterRevisionReplacement()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_multiview_stale_slot_bridge", columnExactPair());
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        List<String> failures = new CopyOnWriteArrayList<>();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            try {
                onEdt(() -> {
                    FlutterDesignerMultiViewDesign design =
                            new FlutterDesignerMultiViewDesign(
                                    fixture.dataObject().getLookup());
                    designRef.set(design);
                    design.componentOpened();
                });
                FlutterDesignerMultiViewDesign design = designRef.get();
                assertNotNull(design);
                awaitWidgetNode(design, COLUMN_ID);
                awaitStableSlotPresentation(design, fixture.mutations());
                FlutterWidgetPropertiesNode.SlotMutationHandler stale =
                        awaitSlotMutationHandler(design, failures);

                FlutterDesignerMutationController.MutationResult external =
                        fixture.mutations().submit(
                                baseline.token().orElseThrow(),
                                new MoveWidget(
                                        FIRST_ID,
                                        new WidgetPlacement(
                                                COLUMN_ID, CHILDREN, 1)),
                                "external Column.children reorder")
                                .get(10, TimeUnit.SECONDS);
                assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                        external.outcome(), external::reason);
                byte[] movedFd = Files.readAllBytes(fixture.fdPath());
                assertFalse(Arrays.equals(fixture.baselineFd(), movedFd));
                awaitCurrentWithPair(
                        fixture.controller(), movedFd, fixture.baselineDart());
                FlutterDesignerMutationController.Snapshot current =
                        awaitReadyWithColumnChildIdsAfterToken(
                                fixture.mutations(),
                                baseline.token().orElseThrow(),
                                List.of(SECOND_ID, FIRST_ID));
                awaitCanvasDocument(design, current.document().orElseThrow());
                awaitStableSlotPresentation(design, fixture.mutations());
                int analysisBeforeStaleIntent = fixture.analysisCalls().get();

                onEdt(() -> stale.submit(new FlutterWidgetSlotMutation.Add(
                        COLUMN_ID,
                        CHILDREN,
                        new WidgetTypeId("flutter.widgets.Text"),
                        2)));

                assertEquals(1, failures.size());
                assertTrue(failures.getFirst().contains(
                        "older Designer revision"), failures::toString);
                assertSame(current.token().orElseThrow(),
                        fixture.mutations().snapshot().token().orElseThrow());
                assertColumnChildren(
                        fixture.mutations().snapshot().document().orElseThrow(),
                        List.of(SECOND_ID, FIRST_ID));
                assertEquals(analysisBeforeStaleIntent,
                        fixture.analysisCalls().get(),
                        "a stale slot intent must reject before analyzer admission");
                assertEquals(PairSaveCoordinatorStatus.CLEAN,
                        fixture.coordinator().state().status());
                assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null) {
                    onEdt(design::componentClosed);
                }
            }
        }
    }

    @Test
    void lazyOwnerStagesExactTextMutationsAndRejectsAStalePresentationToken()
            throws Exception {
        MutationFixture fixture = fixture("mutation_controller");
        DesignerCombinedUndoRedo combined = fixture.dataObject()
                .getCombinedUndoRedo();

        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            assertEquals("before", textData(baseline));
            assertFalse(combined.designerSessionActive(),
                    "loading a writable presentation must not claim semantic Undo");
            assertSame(fixture.current(), fixture.controller().state(),
                    "the durable Current remains the baseline presentation owner");

            FlutterDesignerMutationController.RevisionToken baselineToken =
                    baseline.token().orElseThrow();
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baselineToken,
                            setText("first"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(fixture.mutations(), "first");
            FlutterDesignerMutationController.RevisionToken c1Token =
                    c1.token().orElseThrow();
            assertNotSame(baselineToken, c1Token,
                    "the adopted C1 presentation needs a fresh identity token");
            assertTrue(combined.designerSessionActive(),
                    "the first explicit mutation acquires semantic Undo ownership");
            assertEquals(1, fixture.analysisCalls().get());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertSame(fixture.current(), fixture.controller().state(),
                    "unsaved C1 must not replace the durable document Current");
            PairSaveEvidence c1Evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(c1Evidence);
            assertEquals(c1.document().orElseThrow(),
                    c1Evidence.preparedPairIdentity().prospectiveDocument());
            assertArrayEquals(c1Evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

            FlutterDesignerMutationController.MutationResult stale =
                    fixture.mutations().submit(
                            baselineToken,
                            setText("stale"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    stale.outcome());
            assertTrue(stale.reason().contains("stale"), stale::reason);
            assertEquals(1, fixture.analysisCalls().get(),
                    "a stale editor token must be rejected before analyzer admission");
            FlutterDesignerMutationController.Snapshot afterStale =
                    fixture.mutations().snapshot();
            assertSame(c1Token, afterStale.token().orElseThrow());
            assertEquals("first", textData(afterStale));
            assertSame(c1Evidence, fixture.coordinator().stagedEvidence(),
                    "stale UI input must not disturb exact staged C1 evidence");

            FlutterDesignerMutationController.MutationResult second =
                    fixture.mutations().submit(
                            c1Token,
                            setText("second"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    second.outcome(), second::reason);
            FlutterDesignerMutationController.Snapshot c2 =
                    awaitReadyWithData(fixture.mutations(), "second");
            assertNotSame(c1Token, c2.token().orElseThrow(),
                    "the adopted replacement C2 needs a fresh identity token");
            assertEquals(2, fixture.analysisCalls().get());
            assertEquals(2, fixture.analyzedContents().size());
            assertTrue(fixture.analyzedContents().get(0).contains("first"));
            assertTrue(fixture.analyzedContents().get(1).contains("second"));
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence c2Evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(c2Evidence);
            assertNotSame(c1Evidence, c2Evidence,
                    "C1 evidence must be replaced atomically by exact C2 evidence");
            assertEquals(c2.document().orElseThrow(),
                    c2Evidence.preparedPairIdentity().prospectiveDocument());
            assertArrayEquals(c2Evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertSame(fixture.current(), fixture.controller().state());

            DesignerCommandSessionOrchestrator retainedOwner =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertNotNull(retainedOwner);
            fixture.mutations().close();
            assertNull(sessionOwner(fixture.mutations()));
            assertTrue(retainedOwner.retainsOpenHistoryAuthority(),
                    "ordinary close must transfer a completed staged owner to Pair history");

            fixture.dataObject().getCookie(SaveCookie.class).save();
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            PairSaveCoordinator.PairRenameLease renameLease =
                    fixture.coordinator().beginPairRename();
            renameLease.finish(true);
            awaitDesignerSessionInactive(combined);
            assertFalse(retainedOwner.retainsOpenHistoryAuthority(),
                    "ordinary close must observe final Pair-history invalidation");
        }

        assertFalse(combined.designerSessionActive(),
                "closing the DataObject-owned controller releases semantic Undo");
    }

    @Test
    void consecutiveGrowingTextMutationsRetainAnExactPairedCandidate()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_controller_growing_text",
                exactPair("Hello from NetBeans"));
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();

            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("Hello from NetBeans1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans1");

            FlutterDesignerMutationController.MutationResult second =
                    fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("Hello from NetBeans12"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    second.outcome(), second::reason);
            FlutterDesignerMutationController.Snapshot c2 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans12");
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            assertEquals(c2.document().orElseThrow(),
                    evidence.preparedPairIdentity().prospectiveDocument());
            assertArrayEquals(evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertEquals(2, fixture.analysisCalls().get());
        }
    }

    @Test
    void secondTextMutationMayReturnToTheExactDurableValue()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_controller_return_to_baseline",
                exactPair("Hello from NetBeans"));
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("Hello from NetBeans1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans1");

            FlutterDesignerMutationController.MutationResult restored =
                    fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("Hello from NetBeans"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    restored.outcome(), restored::reason);
            FlutterDesignerMutationController.Snapshot c2 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans");
            DesignerCommandSessionOrchestrator owner =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    owner.currentRevision().persistenceKind());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineFd(),
                    owner.currentRevision().fdBytes());
            assertEquals(1, fixture.analysisCalls().get(),
                    "restoring the already validated durable pair must not rerun analysis");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertTrue(fixture.editor().sourceModified(),
                    "a forward edit back to baseline bytes remains at a dirty native history position");
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(combined::undo);
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans1");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertNotNull(fixture.coordinator().stagedEvidence());

            onEdt(combined::redo);
            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "Hello from NetBeans",
                            undone.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());

            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(
                    fixture.controller(),
                    fixture.baselineFd(),
                    fixture.baselineDart());
            FlutterDesignerMutationController.Snapshot saved =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "Hello from NetBeans",
                            redone.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                    owner.currentRevision().persistenceKind());
            assertFalse(owner.dirty());

            onEdt(combined::undo);
            FlutterDesignerMutationController.Snapshot postSaveUndone =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "Hello from NetBeans1",
                            saved.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence(),
                    "saved history must not fabricate fresh analyzer evidence");
            PairSaveCoordinator.StagedPairProofSnapshot savedUndoProof =
                    fixture.coordinator().stagedProofSnapshot();
            assertNotNull(savedUndoProof);
            assertEquals(
                    PairSaveCoordinator.StagedPairProofKind
                            .REANCHORED_ANALYZED,
                    savedUndoProof.kind());
            assertTrue(fixture.editor().sourceModified());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));

            onEdt(combined::redo);
            FlutterDesignerMutationController.Snapshot postSaveRedone =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "Hello from NetBeans",
                            postSaveUndone.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertFalse(fixture.editor().sourceModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertFalse(owner.dirty());
            assertEquals(1, fixture.analysisCalls().get());

            FlutterDesignerMutationController.MutationResult third =
                    fixture.mutations().submit(
                            postSaveRedone.token().orElseThrow(),
                            setText("Hello from NetBeans2"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    third.outcome(), third::reason);
            awaitReadyWithData(
                    fixture.mutations(), "Hello from NetBeans2");
            assertEquals(2, fixture.analysisCalls().get());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());

            onEdt(combined::undo);
            awaitReadyWithData(
                    fixture.mutations(), "Hello from NetBeans");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertFalse(fixture.editor().sourceModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));

            onEdt(combined::redo);
            awaitReadyWithData(
                    fixture.mutations(), "Hello from NetBeans2");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertNotNull(fixture.coordinator().stagedEvidence());
            assertEquals(2, fixture.analysisCalls().get());
        }
    }

    @Test
    void thirdMutationFromUnsavedSemanticBaselineCreatesFreshPairedBranch()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_controller_branch_from_unsaved_baseline",
                exactPair("Hello from NetBeans"));
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline =
                    fixture.ready();
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("Hello from NetBeans1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans1");

            FlutterDesignerMutationController.MutationResult restored =
                    fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("Hello from NetBeans"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    restored.outcome(), restored::reason);
            FlutterDesignerMutationController.Snapshot c2 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    fixture.coordinator().state().status());
            assertEquals(1, fixture.analysisCalls().get());

            FlutterDesignerMutationController.MutationResult third =
                    fixture.mutations().submit(
                            c2.token().orElseThrow(),
                            setText("Hello from NetBeans2"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    third.outcome(), third::reason);
            FlutterDesignerMutationController.Snapshot c3 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans2");
            DesignerCommandSessionOrchestrator owner =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                    owner.currentRevision().persistenceKind());
            assertTrue(owner.dirty());
            assertEquals(2, fixture.analysisCalls().get());
            assertEquals(2, fixture.analyzedContents().size());
            assertTrue(fixture.analyzedContents().get(0)
                    .contains("Hello from NetBeans1"));
            assertTrue(fixture.analyzedContents().get(1)
                    .contains("Hello from NetBeans2"));
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence c3Evidence =
                    fixture.coordinator().stagedEvidence();
            assertNotNull(c3Evidence);
            assertEquals(c3.document().orElseThrow(),
                    c3Evidence.preparedPairIdentity()
                            .prospectiveDocument());
            assertArrayEquals(c3Evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(combined::undo);
            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "Hello from NetBeans",
                            c3.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertTrue(fixture.editor().sourceModified());

            onEdt(combined::redo);
            awaitReadyWithDataAfterToken(
                    fixture.mutations(),
                    "Hello from NetBeans2",
                    undone.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertNotNull(fixture.coordinator().stagedEvidence());
            assertEquals(2, fixture.analysisCalls().get());
        }
    }

    @Test
    void savedFirstMutationReplaysPresentationWithoutRewritingDurableC1()
            throws Exception {
        MutationFixture fixture = fixture("mutation_save_undo_redo");
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("saved C1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot stagedC1 =
                    awaitReadyWithData(fixture.mutations(), "saved C1");
            PairSaveEvidence c1Evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(c1Evidence);
            byte[] savedDart = c1Evidence.candidateDartBytes();
            byte[] savedFd = c1Evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);

            save.save();

            FlutterDesignerDocumentState.Current savedCurrent =
                    awaitCurrentWithPair(
                            fixture.controller(), savedFd, savedDart);
            FlutterDesignerMutationController.Snapshot savedPresentation =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "saved C1",
                            stagedC1.token().orElseThrow());
            assertNotSame(stagedC1.token().orElseThrow(),
                    savedPresentation.token().orElseThrow(),
                    "durable re-anchoring must publish a new presentation epoch");
            assertSame(savedCurrent, fixture.controller().state());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertFalse(fixture.editor().sourceModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()));

            DesignerCombinedUndoRedo combined = fixture.dataObject()
                    .getCombinedUndoRedo();
            onEdt(() -> {
                assertTrue(combined.canUndo());
                combined.undo();
            });

            FlutterDesignerMutationController.Snapshot undone =
                    awaitReadyWithData(fixture.mutations(), "before");
            assertNotSame(savedPresentation.token().orElseThrow(),
                    undone.token().orElseThrow());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            assertTrue(fixture.editor().sourceModified());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()),
                    "semantic Undo must not rewrite durable C1 Dart");
            assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()),
                    "semantic Undo must not rewrite durable C1 .fd");
            assertSame(savedCurrent, fixture.controller().state(),
                    "history presentation stays separate from durable Current");

            onEdt(() -> {
                assertTrue(combined.canRedo());
                combined.redo();
            });

            FlutterDesignerMutationController.Snapshot redone =
                    awaitReadyWithData(fixture.mutations(), "saved C1");
            assertNotSame(undone.token().orElseThrow(),
                    redone.token().orElseThrow());
            assertArrayEquals(savedDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertFalse(fixture.editor().sourceModified());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(savedDart, Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(savedFd, Files.readAllBytes(fixture.fdPath()));
            assertSame(savedCurrent, fixture.controller().state());
        }
    }

    @Test
    void submitVsUndoRejectsTheStaleRevisionBeforeAnalyzerAdmission()
            throws Exception {
        MutationFixture fixture = fixture("mutation_submit_undo_race");
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("C1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(fixture.mutations(), "C1");
            int callsBeforeRace = fixture.analysisCalls().get();
            CountDownLatch admissionEntered = new CountDownLatch(1);
            CountDownLatch releaseAdmission = new CountDownLatch(1);
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                admissionEntered.countDown();
                if (!releaseAdmission.await(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException(
                            "Timed out waiting to release session admission");
                }
            });

            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("C2 must not publish"),
                            "Text.data");
            assertTrue(admissionEntered.await(10, TimeUnit.SECONDS),
                    "the mutation worker never reached session admission");
            try {
                onEdt(() -> {
                    DesignerCombinedUndoRedo combined = fixture.dataObject()
                            .getCombinedUndoRedo();
                    assertTrue(combined.canUndo());
                    combined.undo();
                });
            } finally {
                releaseAdmission.countDown();
            }

            FlutterDesignerMutationController.MutationResult rejected =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    rejected.outcome(), rejected::reason);
            assertTrue(rejected.reason().contains("changed before"),
                    rejected::reason);
            assertEquals(callsBeforeRace, fixture.analysisCalls().get(),
                    "a stale command must be rejected before analyzer admission");
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithData(fixture.mutations(), "before");
            assertNotSame(c1.token().orElseThrow(),
                    recovered.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void submitVsUndoRedoAbaRejectsOldPairEpochBeforeAnalyzerAdmission()
            throws Exception {
        MutationFixture fixture = fixture("mutation_submit_undo_redo_aba");
        try (fixture) {
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("C1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(fixture.mutations(), "C1");
            int callsBeforeRace = fixture.analysisCalls().get();
            CountDownLatch admissionEntered = new CountDownLatch(1);
            CountDownLatch releaseAdmission = new CountDownLatch(1);
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                admissionEntered.countDown();
                if (!releaseAdmission.await(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException(
                            "Timed out waiting to release ABA admission");
                }
            });

            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("C2 must not publish"),
                            "Text.data");
            assertTrue(admissionEntered.await(10, TimeUnit.SECONDS),
                    "the mutation worker never reached ABA admission");
            try {
                onEdt(() -> {
                    DesignerCombinedUndoRedo combined = fixture.dataObject()
                            .getCombinedUndoRedo();
                    assertTrue(combined.canUndo());
                    combined.undo();
                    assertTrue(combined.canRedo());
                    combined.redo();
                });
            } finally {
                releaseAdmission.countDown();
            }

            FlutterDesignerMutationController.MutationResult rejected =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    rejected.outcome(), rejected::reason);
            assertTrue(rejected.reason().contains("pair epoch changed"),
                    rejected::reason);
            assertEquals(callsBeforeRace, fixture.analysisCalls().get(),
                    "an ABA-stale token must be rejected before analyzer admission");
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithData(fixture.mutations(), "C1");
            assertNotSame(c1.token().orElseThrow(),
                    recovered.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            assertArrayEquals(evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void savedSubmitVsUndoRedoAbaRejectsOldEpochAtCleanReservation()
            throws Exception {
        MutationFixture fixture = fixture("mutation_saved_submit_undo_redo_aba");
        try (fixture) {
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("saved C1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot staged =
                    awaitReadyWithData(fixture.mutations(), "saved C1");
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] savedDart = evidence.candidateDartBytes();
            byte[] savedFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            save.save();
            awaitCurrentWithPair(fixture.controller(), savedFd, savedDart);
            FlutterDesignerMutationController.Snapshot saved =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "saved C1",
                            staged.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());

            int callsBeforeRace = fixture.analysisCalls().get();
            CountDownLatch admissionEntered = new CountDownLatch(1);
            CountDownLatch releaseAdmission = new CountDownLatch(1);
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                admissionEntered.countDown();
                if (!releaseAdmission.await(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException(
                            "Timed out waiting to release saved ABA admission");
                }
            });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            saved.token().orElseThrow(),
                            setText("C2 must not publish"),
                            "Text.data");
            assertTrue(admissionEntered.await(10, TimeUnit.SECONDS),
                    "the mutation worker never reached saved ABA admission");
            try {
                onEdt(() -> {
                    DesignerCombinedUndoRedo combined = fixture.dataObject()
                            .getCombinedUndoRedo();
                    assertTrue(combined.canUndo());
                    combined.undo();
                    assertTrue(combined.canRedo());
                    combined.redo();
                });
            } finally {
                releaseAdmission.countDown();
            }

            FlutterDesignerMutationController.MutationResult rejected =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    rejected.outcome(), rejected::reason);
            assertTrue(rejected.reason().contains(
                    "pair epoch changed before command reservation"),
                    rejected::reason);
            assertEquals(callsBeforeRace, fixture.analysisCalls().get(),
                    "a CLEAN ABA-stale token must never reach the analyzer");
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithData(fixture.mutations(), "saved C1");
            assertNotSame(saved.token().orElseThrow(),
                    recovered.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertArrayEquals(savedDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(savedDart,
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(savedFd,
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void submitVsCleanExternalEventRejectsBeforePairedReservation()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_submit_clean_external_event");
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.RevisionToken baselineToken =
                    baseline.token().orElseThrow();
            PairSaveCoordinator.CloseRevision before =
                    fixture.coordinator().closeRevision();
            AtomicInteger hookCalls = new AtomicInteger();
            AtomicBoolean eventSuppressed = new AtomicBoolean(true);
            AtomicReference<PairSaveCoordinator.CloseRevision> after =
                    new AtomicReference<>();
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                hookCalls.incrementAndGet();
                eventSuppressed.set(fixture.coordinator().handleFileEvent(
                        new FileEvent(fixture.dataObject().getModelFile())));
                after.set(fixture.coordinator().closeRevision());
            });

            FlutterDesignerMutationController.MutationResult rejected =
                    fixture.mutations().submit(
                            baselineToken,
                            setText("must not publish"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    rejected.outcome(), rejected::reason);
            assertTrue(rejected.reason().contains(
                    "full pair authority revision became stale"),
                    rejected::reason);
            assertEquals(1, hookCalls.get());
            assertFalse(eventSuppressed.get());
            assertNotNull(after.get());
            assertEquals(before.stateEpoch(), after.get().stateEpoch(),
                    "a clean event must not rely on the visible pair epoch");
            assertFalse(before.sameRevision(after.get()));
            assertEquals(0, fixture.analysisCalls().get());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertNull(fixture.coordinator().stagedProofSnapshot());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(), "before", baselineToken);
            assertNotSame(baselineToken, recovered.token().orElseThrow());
            assertNull(sessionOwner(fixture.mutations()));
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive());
        }
    }

    @Test
    void controllerRecoversAfterExternalPairPreparationReturnsClean()
            throws Exception {
        MutationFixture fixture = fixture("mutation_preparation_recovery");
        try (fixture) {
            FlutterDesignerMutationController.Snapshot before = fixture.ready();
            FlutterDesignerMutationController.RevisionToken oldToken =
                    before.token().orElseThrow();
            FlutterDesignerDocumentState.Current current = fixture.current();
            byte[] baselineDart = current.sourceIntegrity().orElseThrow()
                    .original().orElseThrow().copyBytes();
            DesignerCommandSession session = DesignerCommandSession.openVerified(
                    current.decoded().original(),
                    baselineDart,
                    current.catalog(),
                    current.sourceIntegrity().orElseThrow(),
                    current.threeWayIntegrity().orElseThrow())
                    .session().orElseThrow();

            try (DesignerCommandSessionOrchestrator owner =
                    new DesignerCommandSessionOrchestrator(
                            session,
                            fixture.dataObject().getCombinedUndoRedo())) {
                var attempt = owner.beginCommand(setText("transient"));
                try (var commandLease = attempt.lease().orElseThrow();
                        var preparation = fixture.coordinator()
                                .beginPairPreparation(
                                        current,
                                        commandLease,
                                        fixture.editor().liveSnapshot())) {
                    assertEquals(PairSaveCoordinatorStatus.PREPARING_PAIR,
                            fixture.coordinator().state().status());
                    FlutterDesignerMutationController.Snapshot blocked =
                            awaitStatus(
                                    fixture.mutations(),
                                    FlutterDesignerMutationController.Status.BLOCKED);
                    assertEquals("before", textData(blocked));
                    assertTrue(blocked.token().isEmpty());
                    assertTrue(blocked.message().contains("transition"),
                            blocked::message);
                }
            }

            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(), "before", oldToken);
            assertNotSame(oldToken, recovered.token().orElseThrow());
            assertEquals(0, fixture.analysisCalls().get());
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    @Test
    void controllerRecoversAfterGatedPairSaveReturnsClean()
            throws Exception {
        MutationFixture fixture = fixture("mutation_save_recovery");
        try (fixture) {
            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("saved C1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            FlutterDesignerMutationController.Snapshot staged =
                    awaitReadyWithData(fixture.mutations(), "saved C1");
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            byte[] expectedDart = evidence.candidateDartBytes();
            byte[] expectedFd = evidence.preparedPairIdentity()
                    .prospectiveFdBytes();

            Field transactionField = PairSaveCoordinator.class
                    .getDeclaredField("transaction");
            transactionField.setAccessible(true);
            PairFileTransaction exactTransaction = (PairFileTransaction)
                    transactionField.get(fixture.coordinator());
            CountDownLatch transactionEntered = new CountDownLatch(1);
            CountDownLatch releaseTransaction = new CountDownLatch(1);
            AtomicReference<Throwable> saveFailure = new AtomicReference<>();
            fixture.coordinator().setPairTransactionForTests(request -> {
                transactionEntered.countDown();
                try {
                    if (!releaseTransaction.await(10, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "Timed out waiting to release Pair Save");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Pair Save gate was interrupted", interrupted);
                }
                return exactTransaction.commit(request);
            });
            SaveCookie save = fixture.dataObject().getCookie(SaveCookie.class);
            assertNotNull(save);
            Thread saveThread = new Thread(() -> {
                try {
                    save.save();
                } catch (Throwable failure) {
                    saveFailure.set(failure);
                }
            }, "mutation-controller-saving-pair");
            saveThread.setDaemon(true);

            try {
                saveThread.start();
                assertTrue(transactionEntered.await(10, TimeUnit.SECONDS),
                        "Pair Save never reached the transaction gate");
                assertEquals(PairSaveCoordinatorStatus.SAVING_PAIR,
                        fixture.coordinator().state().status());
                FlutterDesignerMutationController.Snapshot blocked =
                        awaitStatus(
                                fixture.mutations(),
                                FlutterDesignerMutationController.Status.BLOCKED);
                assertEquals("saved C1", textData(blocked));
                assertTrue(blocked.token().isEmpty());
                assertTrue(blocked.message().contains("saved"),
                        blocked::message);
            } finally {
                releaseTransaction.countDown();
                saveThread.join(TimeUnit.SECONDS.toMillis(10));
                fixture.coordinator().setPairTransactionForTests(null);
            }

            assertFalse(saveThread.isAlive(),
                    "Pair Save did not finish after releasing the transaction");
            assertNull(saveFailure.get(), () -> "Pair Save failed: "
                    + saveFailure.get());
            FlutterDesignerDocumentState.Current savedCurrent =
                    awaitCurrentWithPair(
                            fixture.controller(), expectedFd, expectedDart);
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithDataAfterToken(
                            fixture.mutations(),
                            "saved C1",
                            staged.token().orElseThrow());
            assertSame(savedCurrent, fixture.controller().state());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(expectedDart,
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(expectedFd,
                    Files.readAllBytes(fixture.fdPath()));

            FlutterDesignerMutationController.MutationResult next =
                    fixture.mutations().submit(
                            recovered.token().orElseThrow(),
                            setText("C2 after Save"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    next.outcome(), next::reason);
            awaitReadyWithData(fixture.mutations(), "C2 after Save");
        }
    }

    @Test
    void submitVsCleanExternalEventRejectsBeforeFdOnlyAdoption()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_clean_external_event", columnExactPair());
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.RevisionToken baselineToken =
                    baseline.token().orElseThrow();
            PairSaveCoordinator.CloseRevision before =
                    fixture.coordinator().closeRevision();
            AtomicInteger hookCalls = new AtomicInteger();
            AtomicInteger commitCalls = new AtomicInteger();
            AtomicBoolean eventSuppressed = new AtomicBoolean(true);
            AtomicReference<PairSaveCoordinator.CloseRevision> after =
                    new AtomicReference<>();
            fixture.mutations().setFdOnlyCommitterForTests((
                    expectedRevision, current, lease) -> {
                commitCalls.incrementAndGet();
                fixture.coordinator().commitFdOnly(
                        expectedRevision, current, lease);
            });
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                hookCalls.incrementAndGet();
                eventSuppressed.set(fixture.coordinator().handleFileEvent(
                        new FileEvent(fixture.dataObject().getModelFile())));
                after.set(fixture.coordinator().closeRevision());
            });

            FlutterDesignerMutationController.MutationResult rejected =
                    fixture.mutations().submit(
                            baselineToken,
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.REJECTED,
                    rejected.outcome(), rejected::reason);
            assertTrue(rejected.reason().contains(
                    "full pair authority revision became stale"),
                    rejected::reason);
            assertEquals(1, hookCalls.get());
            assertEquals(0, commitCalls.get(),
                    "a stale FD_ONLY command must fail before durable commit");
            assertFalse(eventSuppressed.get());
            assertNotNull(after.get());
            assertEquals(before.stateEpoch(), after.get().stateEpoch(),
                    "a clean event must not rely on the visible pair epoch");
            assertFalse(before.sameRevision(after.get()));
            assertEquals(0, fixture.analysisCalls().get());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertNull(fixture.coordinator().stagedProofSnapshot());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            baselineToken,
                            List.of(FIRST_ID, SECOND_ID));
            assertNotSame(baselineToken, recovered.token().orElseThrow());
            assertNull(sessionOwner(fixture.mutations()));
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive());
        }
    }

    @Test
    void fdOnlyMoveAdoptsExactSavedRevisionAndAllowsNextPairedMutation()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_move", columnExactPair());
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.RevisionToken baselineToken =
                    baseline.token().orElseThrow();
            byte[] exactDart = fixture.baselineDart().clone();
            List<FlutterDesignerMutationController.Snapshot> publications =
                    new CopyOnWriteArrayList<>();
            fixture.mutations().addPropertyChangeListener(event -> {
                if (FlutterDesignerMutationController.PROP_SNAPSHOT.equals(
                        event.getPropertyName())) {
                    publications.add((FlutterDesignerMutationController.Snapshot)
                            event.getNewValue());
                }
            });

            FlutterDesignerMutationController.MutationResult applied =
                    fixture.mutations().submit(
                            baselineToken,
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            assertEquals(0, fixture.analysisCalls().get(),
                    "FD_ONLY must bypass Dart analyzer admission");
            assertArrayEquals(exactDart,
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(exactDart,
                    Files.readAllBytes(fixture.dartPath()),
                    "the exact Dart file must not be rewritten");
            byte[] committedFd = Files.readAllBytes(fixture.fdPath());
            assertFalse(Arrays.equals(fixture.baselineFd(), committedFd));
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(committedFd)).document();
            assertColumnChildren(saved, List.of(SECOND_ID, FIRST_ID));

            awaitCurrentWithPair(fixture.controller(), committedFd, exactDart);
            FlutterDesignerMutationController.Snapshot reloaded =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            baselineToken,
                            List.of(SECOND_ID, FIRST_ID));
            onEdt(() -> { });
            assertNotSame(baselineToken, reloaded.token().orElseThrow());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertNull(fixture.coordinator().stagedProofSnapshot());
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertFalse(fixture.dataObject().isModified());

            int applyingIndex = firstStatusIndex(
                    publications,
                    FlutterDesignerMutationController.Status.APPLYING);
            int waitingIndex = firstStatusIndexAfter(
                    publications,
                    FlutterDesignerMutationController.Status.WAITING,
                    applyingIndex);
            int readyIndex = firstStatusIndexAfter(
                    publications,
                    FlutterDesignerMutationController.Status.READY,
                    waitingIndex);
            assertTrue(applyingIndex >= 0,
                    "FD_ONLY must first publish APPLYING");
            assertTrue(waitingIndex > applyingIndex,
                    "the durable FD_ONLY adoption must wait for exact rebind");
            assertTrue(readyIndex > waitingIndex,
                    "READY must be published only after exact rebind");
            assertFalse(publications.subList(applyingIndex + 1, waitingIndex)
                    .stream()
                    .anyMatch(snapshot -> snapshot.status()
                            == FlutterDesignerMutationController.Status.READY),
                    "no transient writable READY may expose the old Current");

            DesignerCommandSessionOrchestrator retained =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            DesignerCommandRevision retainedRevision =
                    retained.currentRevision();
            FlutterDesignerDocumentState.Current adopted = assertInstanceOf(
                    FlutterDesignerDocumentState.Current.class,
                    fixture.controller().state());
            assertTrue(retained.ownsExactBaselineCurrent(
                    retainedRevision, adopted));
            assertTrue(retained.canUndo(),
                    "the saved reorder must remain in Designer history");

            FlutterDesignerMutationController.MutationResult paired =
                    fixture.mutations().submit(
                            reloaded.token().orElseThrow(),
                            new SetProperty(
                                    FIRST_ID,
                                    DATA,
                                    new PropertyValue.StringValue("after move")),
                            "First Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    paired.outcome(), paired::reason);
            FlutterDesignerMutationController.Snapshot changed =
                    awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            reloaded.token().orElseThrow(),
                            List.of(SECOND_ID, FIRST_ID));
            WidgetSlot.ListSlot children = assertInstanceOf(
                    WidgetSlot.ListSlot.class,
                    changed.document().orElseThrow().root().slots().get(CHILDREN));
            assertEquals(new PropertyValue.StringValue("after move"),
                    children.children().get(1).properties().get(DATA));
            assertEquals(1, fixture.analysisCalls().get(),
                    "the next source-changing command must use paired analysis");
            assertEquals(PairSaveCoordinatorStatus.STAGED_PAIR,
                    fixture.coordinator().state().status());
        }
    }

    @Test
    void fdOnlyCommitFailureLeavesBothFilesExactAndRetainsRecoverableRevision()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_failure", columnExactPair());
        try (fixture) {
            fixture.mutations().setFdOnlyCommitterForTests((
                    expectedRevision, current, lease) -> {
                assertSame(fixture.current(), current);
                assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                        lease.revision().persistenceKind());
                throw new IOException("Synthetic Designer-only commit failure");
            });
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();

            FlutterDesignerMutationController.MutationResult failed =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.FAILED,
                    failed.outcome(), failed::reason);
            assertTrue(failed.reason().contains(
                    "Synthetic Designer-only commit failure"), failed::reason);
            assertEquals(0, fixture.analysisCalls().get());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());

            awaitReadyWithColumnChildIdsAfterToken(
                            fixture.mutations(),
                            baseline.token().orElseThrow(),
                            List.of(SECOND_ID, FIRST_ID));
            DesignerCommandSessionOrchestrator retained =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                    retained.currentRevision().persistenceKind());
            assertTrue(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive());
            assertTrue(retained.canUndo(),
                    "the failed durable lease must retain its exact semantic predecessor");
        }
    }

    @Test
    void fdOnlyMoveAgainstStaleFdNeverOverwritesExternalBytes()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_stale", columnExactPair());
        try (fixture) {
            byte[] suffix = "\n ".getBytes(StandardCharsets.UTF_8);
            byte[] staleFd = Arrays.copyOf(
                    fixture.baselineFd(),
                    fixture.baselineFd().length + suffix.length);
            System.arraycopy(
                    suffix, 0, staleFd, fixture.baselineFd().length, suffix.length);
            fixture.mutations().setCommitBoundaryHookForTests(() -> {
                try {
                    Files.write(fixture.fdPath(), staleFd);
                } catch (IOException failure) {
                    throw new IllegalStateException(
                            "Cannot create the stale .fd test baseline", failure);
                }
            });
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();

            FlutterDesignerMutationController.MutationResult failed =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.FAILED,
                    failed.outcome(), failed::reason);
            assertEquals(0, fixture.analysisCalls().get());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(staleFd, Files.readAllBytes(fixture.fdPath()),
                    "the external .fd bytes must remain authoritative");
            assertTrue(PairSaveCoordinator.isStickyConflict(
                    fixture.coordinator().state().status()));
            assertTrue(fixture.mutations().snapshot().token().isEmpty());
        }
    }

    @Test
    void closingBeforeFdOnlyCommitBoundaryCancelsWithoutChangingEitherFile()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_cancel", columnExactPair());
        try (fixture) {
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            fixture.mutations().setSessionAdmissionHookForTests(() -> {
                entered.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to release FD_ONLY admission");
                }
            });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children");
            assertTrue(entered.await(10, TimeUnit.SECONDS));

            fixture.mutations().close();
            release.countDown();
            FlutterDesignerMutationController.MutationResult cancelled =
                    pending.get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.CANCELLED,
                    cancelled.outcome(), cancelled::reason);
            assertEquals(0, fixture.analysisCalls().get());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
        }
    }

    @Test
    void closingAfterFdOnlyCommitBoundaryLetsExactCommitFinish()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_close_after_boundary", columnExactPair());
        try (fixture) {
            CountDownLatch enteredCommit = new CountDownLatch(1);
            CountDownLatch releaseCommit = new CountDownLatch(1);
            fixture.mutations().setFdOnlyCommitterForTests((
                    expectedRevision, current, lease) -> {
                enteredCommit.countDown();
                try {
                    if (!releaseCommit.await(10, TimeUnit.SECONDS)) {
                        throw new IOException(
                                "Timed out waiting to release FD_ONLY commit");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "FD_ONLY commit gate was interrupted", interrupted);
                }
                fixture.coordinator().commitFdOnly(
                        expectedRevision, current, lease);
            });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            new MoveWidget(
                                    FIRST_ID,
                                    new WidgetPlacement(
                                            COLUMN_ID, CHILDREN, 1)),
                            "Column.children");
            assertTrue(enteredCommit.await(10, TimeUnit.SECONDS));

            fixture.mutations().close();
            assertEquals(FlutterDesignerMutationController.Status.CLOSED,
                    fixture.mutations().snapshot().status());
            assertFalse(pending.isDone(),
                    "close after the commit boundary must not report cancellation");
            releaseCommit.countDown();

            FlutterDesignerMutationController.MutationResult applied =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            byte[] committedFd = Files.readAllBytes(fixture.fdPath());
            assertFalse(Arrays.equals(fixture.baselineFd(), committedFd));
            DesignerDocument saved = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    new FdDocumentCodec().decode(committedFd)).document();
            assertColumnChildren(saved, List.of(SECOND_ID, FIRST_ID));
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
        }
    }

    @Test
    void liveSourceChangeWhileAnalyzerIsGatedRejectsCandidateAndPreservesUserEdit()
            throws Exception {
        MutationFixture fixture = fixture("mutation_source_race");
        try (fixture) {
            CompletableFuture<DartCandidateAnalysisResult> analyzerGate =
                    new CompletableFuture<>();
            CountDownLatch requestIssued = new CountDownLatch(1);
            AtomicReference<DartCandidateAnalysisRequest> exactRequest =
                    new AtomicReference<>();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        exactRequest.set(request);
                        requestIssued.countDown();
                        return gatedAnalysis(analyzerGate);
                    });

            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("must not publish"),
                            "Text.data");
            assertTrue(requestIssued.await(10, TimeUnit.SECONDS),
                    "the mutation never reached the analyzer gate");
            assertNotNull(exactRequest.get());

            String userEdit = "// user Source edit during analysis\n";
            onEdt(() -> fixture.document().insertString(
                    fixture.document().getLength(), userEdit, null));
            byte[] expectedLive = (new String(
                    fixture.baselineDart(), StandardCharsets.UTF_8) + userEdit)
                    .getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(expectedLive,
                    fixture.editor().liveSnapshot().markerBearingUtf8());

            analyzerGate.complete(passingAnalysis(
                    exactRequest.get(), fixture.frameworkFile()));
            FlutterDesignerMutationController.MutationResult result =
                    pending.get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.FAILED,
                    result.outcome(), result::reason);
            FlutterDesignerMutationController.Snapshot blocked = awaitStatus(
                    fixture.mutations(),
                    FlutterDesignerMutationController.Status.BLOCKED);
            assertEquals("before", textData(blocked),
                    "the rejected candidate must never become presentation state");
            assertTrue(blocked.token().isEmpty(),
                    "a dirty Source presentation must not retain a writable token");
            assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertNull(fixture.coordinator().stagedProofSnapshot());
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive(),
                    "failed predecessor CAS must not adopt a semantic revision");
            assertTrue(fixture.editor().sourceModified());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertArrayEquals(expectedLive,
                    fixture.editor().liveSnapshot().markerBearingUtf8(),
                    "the user's newer Source edit must remain authoritative");
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertSame(fixture.current(), fixture.controller().state());
        }
    }

    @Test
    void fatalAnalyzerFailureCompletesAndReleasesAnUnusedCommandOwner()
            throws Exception {
        MutationFixture fixture = fixture("mutation_analyzer_fatal");
        try (fixture) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        throw new AssertionError("synthetic analyzer fatal");
                    });
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("must not publish"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.FAILED,
                    result.outcome());
            assertTrue(result.reason().contains("synthetic analyzer fatal"),
                    result::reason);
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithData(fixture.mutations(), "before");
            assertEquals(FlutterDesignerMutationController.Status.READY,
                    recovered.status());
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive(),
                    "a failed first command must not retain semantic Undo ownership");
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> completedAnalysis(
                            passingAnalysis(request, fixture.frameworkFile())));
            FlutterDesignerMutationController.MutationResult next =
                    fixture.mutations().submit(
                            recovered.token().orElseThrow(),
                            setText("worker survived AssertionError"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    next.outcome(), next::reason);
            awaitReadyWithData(
                    fixture.mutations(), "worker survived AssertionError");
        }
    }

    @Test
    void fatalClassifierUnwrapsVmTerminationButNotOrdinaryAssertionError()
            throws Exception {
        Method fatalError = FlutterDesignerMutationController.class
                .getDeclaredMethod("fatalError", Throwable.class);
        fatalError.setAccessible(true);
        SyntheticVirtualMachineError fatal =
                new SyntheticVirtualMachineError("synthetic VM termination");
        SyntheticVirtualMachineError suppressedFatal =
                new SyntheticVirtualMachineError(
                        "synthetic suppressed VM termination");
        IOException cleanupWrapper = new IOException(
                "pair cleanup wrapper");
        cleanupWrapper.addSuppressed(suppressedFatal);

        Object unwrapped = fatalError.invoke(
                null, new IOException("coordinator wrapper", fatal));
        Object unwrappedSuppressed = fatalError.invoke(null, cleanupWrapper);
        Object ordinary = fatalError.invoke(
                null, new IOException(
                        "ordinary wrapper", new AssertionError("ordinary")));

        assertSame(fatal, unwrapped,
                "fatal JVM termination must survive coordinator IOException wrapping");
        assertSame(suppressedFatal, unwrappedSuppressed,
                "fatal JVM termination suppressed during pair cleanup must be rethrown");
        assertNull(ordinary,
                "ordinary AssertionError must complete the operation without killing the worker");
    }

    @Test
    void wrappedVirtualMachineErrorCompletesFutureBeforeWorkerRethrows()
            throws Exception {
        MutationFixture fixture = fixture("mutation_wrapped_vm_error");
        try (fixture) {
            SyntheticVirtualMachineError fatal =
                    new SyntheticVirtualMachineError(
                            "synthetic wrapped VM termination");
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> completedExceptionally(fatal));

            FlutterDesignerMutationController.MutationResult result =
                    fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("must not publish"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);

            assertEquals(FlutterDesignerMutationController.Outcome.FAILED,
                    result.outcome(), result::reason);
            assertTrue(result.reason().contains(
                    "synthetic wrapped VM termination"), result::reason);
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReadyWithData(fixture.mutations(), "before");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(sessionOwner(fixture.mutations()));
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());

            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> completedAnalysis(
                            passingAnalysis(request, fixture.frameworkFile())));
            FlutterDesignerMutationController.MutationResult next =
                    fixture.mutations().submit(
                            recovered.token().orElseThrow(),
                            setText("replacement worker survived"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    next.outcome(), next::reason);
            awaitReadyWithData(
                    fixture.mutations(), "replacement worker survived");
        }
    }

    @Test
    void closeWhileAnalyzerFactoryReturnsCancelsUnregisteredOperationAndNeverApplies()
            throws Exception {
        MutationFixture fixture = fixture("mutation_close_analyzer_registration");
        CountDownLatch factoryEntered = new CountDownLatch(1);
        CountDownLatch releaseFactory = new CountDownLatch(1);
        AtomicInteger cancellationCalls = new AtomicInteger();
        CompletableFuture<DartCandidateAnalysisResult> never =
                new CompletableFuture<>();
        try (fixture) {
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        factoryEntered.countDown();
                        awaitLatchUnchecked(
                                releaseFactory,
                                "Timed out waiting to return analyzer operation");
                        return new DartCandidateAnalysisOperation() {
                            @Override
                            public CompletionStage<DartCandidateAnalysisResult>
                                    result() {
                                return never;
                            }

                            @Override
                            public boolean cancel() {
                                cancellationCalls.incrementAndGet();
                                return never.cancel(false);
                            }
                        };
                    });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("must never apply"),
                            "Text.data");
            assertTrue(factoryEntered.await(10, TimeUnit.SECONDS),
                    "the analyzer factory never reached its return race");

            fixture.mutations().close();
            FlutterDesignerMutationController.MutationResult cancelled =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.CANCELLED,
                    cancelled.outcome(), cancelled::reason);
            assertEquals(FlutterDesignerMutationController.Status.CLOSED,
                    fixture.mutations().snapshot().status());

            releaseFactory.countDown();
            awaitAtomicValue(cancellationCalls, 1,
                    "the unregistered analyzer operation was not cancelled");
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
            assertNull(sessionOwner(fixture.mutations()));
            assertNull(fixture.coordinator().stagedEvidence());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        } finally {
            releaseFactory.countDown();
        }
    }

    @Test
    void closeAfterCommitBoundaryReturnsAppliedAndDefersOwnerCleanup()
            throws Exception {
        MutationFixture fixture = fixture("mutation_close_after_commit_boundary");
        CountDownLatch boundaryCrossed = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        try (fixture) {
            fixture.mutations().setCommitBoundaryHookForTests(() -> {
                boundaryCrossed.countDown();
                if (!releaseCommit.await(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException(
                            "Timed out waiting to release commit boundary");
                }
            });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            fixture.ready().token().orElseThrow(),
                            setText("committed while closing"),
                            "Text.data");
            assertTrue(boundaryCrossed.await(10, TimeUnit.SECONDS),
                    "the mutation never crossed its post-analysis commit boundary");
            DesignerCommandSessionOrchestrator retainedOwner =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertNotNull(retainedOwner);

            fixture.mutations().close();
            assertEquals(FlutterDesignerMutationController.Status.CLOSED,
                    fixture.mutations().snapshot().status());
            assertFalse(pending.isDone(),
                    "close after the commit edge must not publish false cancellation");

            releaseCommit.countDown();
            FlutterDesignerMutationController.MutationResult applied =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.STAGED_PAIR);
            assertFalse(fixture.coordinator().state().status()
                    == PairSaveCoordinatorStatus.RECOVERY_CONFLICT,
                    "deferred owner cleanup must not turn a committed pair into recovery conflict");
            assertNull(sessionOwner(fixture.mutations()),
                    "the closed controller must not retain the command owner");
            PairSaveEvidence evidence = fixture.coordinator().stagedEvidence();
            assertNotNull(evidence);
            assertTrue(retainedOwner.retainsOpenHistoryAuthority(),
                    "the coordinator must retain an open owner for Save and semantic replay");
            assertArrayEquals(evidence.candidateDartBytes(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()),
                    "the commit boundary stages only; it must not perform Pair Save");
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));

            SaveCookie closeRaceSave = fixture.dataObject()
                    .getCookie(SaveCookie.class);
            assertNotNull(closeRaceSave);
            closeRaceSave.save();
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertFalse(fixture.editor().sourceModified());
            assertArrayEquals(evidence.candidateDartBytes(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(
                    evidence.preparedPairIdentity().prospectiveFdBytes(),
                    Files.readAllBytes(fixture.fdPath()));
            PairSaveCoordinator.PairRenameLease renameLease =
                    fixture.coordinator().beginPairRename();
            renameLease.finish(true);
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
            assertFalse(retainedOwner.retainsOpenHistoryAuthority(),
                    "pair invalidation must release the transferred command owner");
        } finally {
            releaseCommit.countDown();
        }
    }

    @Test
    void closeAfterBaselineReplacementCommitBoundaryReturnsApplied()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_close_after_baseline_commit_boundary",
                exactPair("Hello from NetBeans"));
        CountDownLatch boundaryCrossed = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline =
                    fixture.ready();
            FlutterDesignerMutationController.MutationResult first =
                    fixture.mutations().submit(
                            baseline.token().orElseThrow(),
                            setText("Hello from NetBeans1"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    first.outcome(), first::reason);
            FlutterDesignerMutationController.Snapshot c1 =
                    awaitReadyWithData(
                            fixture.mutations(), "Hello from NetBeans1");

            fixture.mutations().setCommitBoundaryHookForTests(() -> {
                boundaryCrossed.countDown();
                if (!releaseCommit.await(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException(
                            "Timed out waiting to release baseline commit boundary");
                }
            });
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            c1.token().orElseThrow(),
                            setText("Hello from NetBeans"),
                            "Text.data");
            assertTrue(boundaryCrossed.await(10, TimeUnit.SECONDS),
                    "the BASELINE replacement never crossed its commit boundary");
            DesignerCommandSessionOrchestrator retainedOwner =
                    (DesignerCommandSessionOrchestrator) sessionOwner(
                            fixture.mutations());
            assertNotNull(retainedOwner);

            fixture.mutations().close();
            assertEquals(FlutterDesignerMutationController.Status.CLOSED,
                    fixture.mutations().snapshot().status());
            assertFalse(pending.isDone(),
                    "close after the BASELINE commit edge must not publish false cancellation");

            releaseCommit.countDown();
            FlutterDesignerMutationController.MutationResult applied =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.APPLIED,
                    applied.outcome(), applied::reason);
            awaitPairStatus(
                    fixture.coordinator(),
                    PairSaveCoordinatorStatus.DIRTY_SOURCE);
            assertFalse(fixture.coordinator().state().status()
                    == PairSaveCoordinatorStatus.RECOVERY_CONFLICT);
            assertNull(fixture.coordinator().stagedEvidence());
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            assertTrue(fixture.editor().sourceModified());
            assertNotNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertNull(sessionOwner(fixture.mutations()));
            assertTrue(retainedOwner.retainsOpenHistoryAuthority(),
                    "the semantic BASELINE owner must remain usable by Save after controller close");

            fixture.dataObject().getCookie(SaveCookie.class).save();
            awaitPairStatus(
                    fixture.coordinator(), PairSaveCoordinatorStatus.CLEAN);
            assertNull(fixture.dataObject().getCookie(SaveCookie.class));
            assertFalse(fixture.editor().sourceModified());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
            fixture.mutations().closeForDataObjectDisposal();
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
            assertFalse(retainedOwner.retainsOpenHistoryAuthority(),
                    "DataObject disposal must release the transferred BASELINE owner");
        } finally {
            releaseCommit.countDown();
        }
    }

    @Test
    void dataObjectDisposeCancelsActiveAnalysisAndPreventsControllerRecreation()
            throws Exception {
        MutationFixture fixture = fixture("mutation_dispose_active");
        try (fixture) {
            CompletableFuture<DartCandidateAnalysisResult> analyzerGate =
                    new CompletableFuture<>();
            CountDownLatch requestIssued = new CountDownLatch(1);
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        requestIssued.countDown();
                        return gatedAnalysis(analyzerGate);
                    });
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.RevisionToken retainedToken =
                    baseline.token().orElseThrow();
            CompletableFuture<FlutterDesignerMutationController.MutationResult>
                    pending = fixture.mutations().submit(
                            retainedToken,
                            setText("must not publish"),
                            "Text.data");
            assertTrue(requestIssued.await(10, TimeUnit.SECONDS),
                    "the mutation never reached the analyzer gate");

            fixture.dataObject().dispose();

            assertEquals(FlutterDesignerMutationController.Status.CLOSED,
                    fixture.mutations().snapshot().status());
            assertTrue(fixture.mutations().snapshot().token().isEmpty());
            FlutterDesignerMutationController.MutationResult cancelled =
                    pending.get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.CANCELLED,
                    cancelled.outcome(), cancelled::reason);
            assertThrows(IllegalStateException.class,
                    fixture.dataObject()::mutationController);
            FlutterDesignerMutationController.MutationResult afterClose =
                    fixture.mutations().submit(
                            retainedToken,
                            setText("still closed"),
                            "Text.data")
                            .get(10, TimeUnit.SECONDS);
            assertEquals(FlutterDesignerMutationController.Outcome.CANCELLED,
                    afterClose.outcome());
            awaitDesignerSessionInactive(
                    fixture.dataObject().getCombinedUndoRedo());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
        }
    }

    private MutationFixture fixture(String name) throws Exception {
        return fixture(name, exactPair());
    }

    private MutationFixture fixture(String name, ExactPair exactPair)
            throws Exception {
        Path projectRoot = Files.createDirectory(temporaryDirectory.resolve(name));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path modelRoot = Files.createDirectories(
                projectRoot.resolve(".fd_templates"));
        Files.writeString(projectRoot.resolve("pubspec.yaml"), """
                name: mutation_controller_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);

        FakeSdk sdk = fakeSdk(name + "-flutter");
        FlutterSettings settings = FlutterSettings.getDefault();
        FlutterToolchainConfig previousSettings = settings.load();
        settings.save(new FlutterToolchainConfig(
                sdk.flutterRoot().toString(), true, ""));

        FlutterDesignerDocumentController controller = null;
        FlutterDesignerMutationController mutations = null;
        try {
            FlutterProject project = FlutterDesignerTestProject.own(projectRoot);
            Path dartPath = lib.resolve("home_page.dart");
            Path fdPath = modelRoot.resolve("home_page.fd");
            ExactPair pair = Objects.requireNonNull(exactPair, "exactPair");
            Files.write(dartPath, pair.dartBytes());
            Files.write(fdPath, pair.fdBytes());
            FileUtil.refreshFor(projectRoot.toFile());

            FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
            assertNotNull(dartFile);
            FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                    .dataObject(dartFile, project);
            mutations = dataObject.mutationController();
            AtomicInteger analysisCalls = new AtomicInteger();
            List<String> analyzedContents = new CopyOnWriteArrayList<>();
            Path expectedDartExecutable = sdk.dartExecutable().toRealPath();
            mutations.setAnalyzerFactoryForTests((dartExecutable, request) -> {
                if (!expectedDartExecutable.equals(dartExecutable)) {
                    throw new AssertionError("Unexpected Dart executable: "
                            + dartExecutable);
                }
                analysisCalls.incrementAndGet();
                analyzedContents.add(request.content());
                return completedAnalysis(passingAnalysis(
                        request, sdk.frameworkFile()));
            });

            controller = dataObject.getDocumentController();
            assertTrue(controller.viewOpened());
            FlutterDesignerDocumentState.Current current = awaitCurrent(controller);
            assertTrue(current.threeWayIntegrity().orElseThrow()
                    .onDiskThreeWayMatch());
            FlutterDesignerEditorSupport editor = dataObject.getEditorSupport();
            StyledDocument document = openGuardedSourceDocument(editor);
            assertArrayEquals(pair.dartBytes(),
                    editor.liveSnapshot().markerBearingUtf8());
            FlutterDesignerMutationController.Snapshot ready =
                    awaitReady(mutations);
            return new MutationFixture(
                    dataObject,
                    controller,
                    mutations,
                    dataObject.getPairSaveCoordinator(),
                    editor,
                    document,
                    dartPath,
                    fdPath,
                    pair.dartBytes(),
                    pair.fdBytes(),
                    sdk.frameworkFile(),
                    current,
                    ready,
                    settings,
                    previousSettings,
                    analysisCalls,
                    analyzedContents);
        } catch (Exception | Error failure) {
            if (mutations != null) {
                mutations.close();
            }
            if (controller != null) {
                controller.viewClosed();
            }
            settings.save(previousSettings);
            throw failure;
        }
    }

    private ExactPair exactPair() throws Exception {
        return exactPair("before");
    }

    private ExactPair exactPair(String text) throws Exception {
        DesignerDocument provisional = document(
                descriptor("0".repeat(64), "0".repeat(64)), text);
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = document(
                descriptor(
                        provisionalGenerated.imports().normalizedSha256(),
                        provisionalGenerated.build().normalizedSha256()),
                text);
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair sizedOverflowBoxExactPair(PropertyValue.SizeValue size)
            throws Exception {
        DesignerDocument provisional = sizedOverflowBoxDocument(
                descriptor("0".repeat(64), "0".repeat(64)), size);
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = sizedOverflowBoxDocument(
                descriptor(
                        provisionalGenerated.imports().normalizedSha256(),
                        provisionalGenerated.build().normalizedSha256()),
                size);
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair transformExactPair(PropertyValue.OffsetValue origin)
            throws Exception {
        DesignerDocument provisional = transformDocument(
                descriptor("0".repeat(64), "0".repeat(64)), origin);
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = transformDocument(
                descriptor(
                        provisionalGenerated.imports().normalizedSha256(),
                        provisionalGenerated.build().normalizedSha256()),
                origin);
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair columnExactPair() throws Exception {
        DesignerDocument provisional = columnDocument(
                descriptor("0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = columnDocument(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair indexedStackColumnExactPair() throws Exception {
        DesignerDocument provisional = indexedStackColumnDocument(
                descriptor("0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = indexedStackColumnDocument(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair scaffoldCenterTextExactPair() throws Exception {
        DesignerDocument provisional = scaffoldCenterTextDocument(
                descriptor("0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = scaffoldCenterTextDocument(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair centerAndSourceExactPair() throws Exception {
        DesignerDocument provisional = centerAndSourceDocument(
                descriptor("0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = centerAndSourceDocument(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private ExactPair scaffoldCenterEmptyColumnExactPair() throws Exception {
        DesignerDocument provisional = scaffoldCenterEmptyColumnDocument(
                descriptor("0".repeat(64), "0".repeat(64)));
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = scaffoldCenterEmptyColumnDocument(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()));
        GeneratedDartRegions generated = new DartRegionGenerator()
                .generate(exact, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        return new ExactPair(
                source(generated).getBytes(StandardCharsets.UTF_8),
                new FdDocumentCodec().encode(exact).copyBytes());
    }

    private FakeSdk fakeSdk(String name) throws Exception {
        Path flutterRoot = Files.createDirectories(
                temporaryDirectory.resolve(name));
        createExecutable(
                flutterRoot.resolve("bin").resolve(
                        isWindows() ? "flutter.bat" : "flutter"));
        Path dartExecutable = createExecutable(
                flutterRoot.resolve("bin/cache/dart-sdk/bin").resolve(
                        isWindows() ? "dart.exe" : "dart"));
        Path framework = flutterRoot.resolve(
                "packages/flutter/lib/src/widgets/framework.dart");
        Files.createDirectories(framework.getParent());
        Files.writeString(framework, """
                abstract class Widget {}
                class BuildContext {}
                class StatelessWidget extends Widget {}
                class Text extends Widget {}
                """, StandardCharsets.UTF_8);
        return new FakeSdk(
                flutterRoot.toRealPath(),
                dartExecutable.toRealPath(),
                framework.toRealPath());
    }

    private static Path createExecutable(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, isWindows() ? "@echo off\r\n" : "#!/bin/sh\n",
                StandardCharsets.UTF_8);
        if (!isWindows()) {
            assertTrue(path.toFile().setExecutable(true));
        }
        return path;
    }

    private static FlutterDesignerDocumentState.Current awaitCurrent(
            FlutterDesignerDocumentController controller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError("Designer load failed: " + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Current: "
                + controller.state());
    }

    private static FlutterDesignerDocumentState.Current awaitCurrentAfter(
            FlutterDesignerDocumentController controller,
            FlutterDesignerDocumentState.Current previous) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current
                    && current != previous) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError("Designer reload failed: " + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for a reloaded Current: "
                + controller.state());
    }

    private static FlutterDesignerDocumentState.Current awaitCurrentWithPair(
            FlutterDesignerDocumentController controller,
            byte[] expectedFd,
            byte[] expectedDart) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerDocumentState state = controller.state();
            if (state instanceof FlutterDesignerDocumentState.Current current
                    && current.decoded().original().contentEquals(expectedFd)
                    && current.sourceIntegrity().isPresent()
                    && current.sourceIntegrity().orElseThrow().original().isPresent()
                    && current.sourceIntegrity().orElseThrow().original()
                            .orElseThrow().contentEquals(expectedDart)) {
                return current;
            }
            if (state instanceof FlutterDesignerDocumentState.Failure failure) {
                throw new AssertionError("Designer load failed: " + failure);
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for exact Current pair: "
                + controller.state());
    }

    private static FlutterDesignerMutationController.Snapshot awaitReady(
            FlutterDesignerMutationController controller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for mutation READY: "
                + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithColumnChildIdsAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken,
                    List<StableId> expectedChildIds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()) {
                WidgetSlot slot = current.document().orElseThrow()
                        .root().slots().get(CHILDREN);
                if (slot instanceof WidgetSlot.ListSlot children
                        && expectedChildIds.equals(children.children().stream()
                                .map(WidgetNode::id)
                                .toList())) {
                    return current;
                }
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Column.children="
                + expectedChildIds + " after revision token " + oldToken
                + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithColumnChildCountAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken,
                    int expectedChildCount) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()) {
                WidgetSlot slot = current.document().orElseThrow()
                        .root().slots().get(CHILDREN);
                if (slot instanceof WidgetSlot.ListSlot children
                        && children.children().size() == expectedChildCount) {
                    return current;
                }
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Column.children count="
                + expectedChildCount + " after revision token " + oldToken
                + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithEmptyColumnAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken)
                    throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()) {
                WidgetNode root = current.document().orElseThrow().root();
                WidgetSlot slot = root.slots().get(CHILDREN);
                boolean empty = slot == null
                        || slot instanceof WidgetSlot.ListSlot list
                                && list.children().isEmpty();
                if (COLUMN_ID.equals(root.id()) && empty) {
                    return current;
                }
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for empty Column.children after revision token "
                + oldToken + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithScaffoldBodyAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken,
                    boolean expectedNestedSubtree) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()) {
                WidgetNode root = current.document().orElseThrow().root();
                boolean hasCenter = containsWidget(root, CENTER_ID);
                boolean hasText = containsWidget(root, NESTED_TEXT_ID);
                if (SCAFFOLD_ID.equals(root.id())
                        && hasCenter == expectedNestedSubtree
                        && hasText == expectedNestedSubtree) {
                    return current;
                }
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Scaffold subtree present="
                + expectedNestedSubtree + " after revision token " + oldToken
                + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithCenterChildAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken,
                    StableId expectedChildId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()) {
                WidgetNode center = findModelWidget(
                        current.document().orElseThrow().root(), CENTER_ID);
                WidgetSlot slot = center == null
                        ? null : center.slots().get(CHILD);
                StableId actualChildId = slot instanceof WidgetSlot.SingleSlot single
                        && single.child().isPresent()
                                ? single.child().orElseThrow().id()
                                : null;
                if (Objects.equals(expectedChildId, actualChildId)) {
                    return current;
                }
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Center.child="
                + expectedChildId + " after revision token " + oldToken
                + ": " + controller.snapshot());
    }

    private static WidgetNode findModelWidget(
            WidgetNode node,
            StableId widgetId) {
        if (widgetId.equals(node.id())) {
            return node;
        }
        for (WidgetSlot slot : node.slots().values()) {
            for (WidgetNode child : switch (slot) {
                case WidgetSlot.SingleSlot single ->
                    single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            }) {
                WidgetNode found = findModelWidget(child, widgetId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean containsWidget(WidgetNode node, StableId widgetId) {
        if (widgetId.equals(node.id())) {
            return true;
        }
        for (WidgetSlot slot : node.slots().values()) {
            switch (slot) {
                case WidgetSlot.SingleSlot single -> {
                    if (single.child().isPresent()
                            && containsWidget(
                                    single.child().orElseThrow(), widgetId)) {
                        return true;
                    }
                }
                case WidgetSlot.ListSlot list -> {
                    for (WidgetNode child : list.children()) {
                        if (containsWidget(child, widgetId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static FlutterDesignerMutationController.Snapshot awaitReadyWithData(
            FlutterDesignerMutationController controller,
            String expectedData) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && expectedData.equals(textData(current))) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Text.data="
                + expectedData + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot awaitReadyWithSoftWrap(
            FlutterDesignerMutationController controller,
            Boolean expectedValue) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && Objects.equals(expectedValue, textSoftWrap(current))) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Text.softWrap="
                + expectedValue + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot awaitReadyWithSize(
            FlutterDesignerMutationController controller,
            PropertyValue.SizeValue expectedValue) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            PropertyValue value = current.document().isPresent()
                    ? current.document().orElseThrow().root().properties().get(SIZE)
                    : null;
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && expectedValue.equals(value)) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for SizedOverflowBox.size="
                + expectedValue + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot awaitReadyWithOrigin(
            FlutterDesignerMutationController controller,
            PropertyValue.OffsetValue expectedValue) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            PropertyValue value = current.document().isPresent()
                    ? current.document().orElseThrow().root().properties()
                            .get(ORIGIN)
                    : null;
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && expectedValue.equals(value)) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Transform.origin="
                + expectedValue + ": " + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithScaffoldPropertyAfterToken(
                    FlutterDesignerMutationController controller,
                    PropertyName property,
                    PropertyValue expectedValue,
                    FlutterDesignerMutationController.RevisionToken oldToken)
                    throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && current.document().isPresent()
                    && SCAFFOLD_ID.equals(current.document().orElseThrow()
                            .root().id())
                    && Objects.equals(expectedValue,
                            current.document().orElseThrow().root()
                                    .properties().get(property))) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for Scaffold."
                + property.value() + "=" + expectedValue
                + " after revision token " + oldToken + ": "
                + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot
            awaitReadyWithDataAfterToken(
                    FlutterDesignerMutationController controller,
                    String expectedData,
                    FlutterDesignerMutationController.RevisionToken oldToken)
                    throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && expectedData.equals(textData(current))
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken) {
                return current;
            }
            if (current.status()
                    == FlutterDesignerMutationController.Status.BLOCKED) {
                throw new AssertionError(current.operation() + " failed for "
                        + current.target() + ": " + current.message());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for a new presentation "
                + "token with Text.data=" + expectedData + ": "
                + controller.snapshot());
    }

    private static FlutterDesignerMutationController.Snapshot awaitStatus(
            FlutterDesignerMutationController controller,
            FlutterDesignerMutationController.Status expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status() == expected) {
                return current;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for mutation " + expected
                + ": " + controller.snapshot());
    }

    private static Action deleteWidgetAction(
            FlutterDesignerMultiViewDesign design) {
        Action action = design.getVisualRepresentation().getActionMap().get(
                FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY);
        assertNotNull(action,
                "the MultiView ActionMap must publish its Delete command");
        return action;
    }

    private static void selectWidgetFromCanvas(
            FlutterDesignerMultiViewDesign design,
            StableId widgetId) throws Exception {
        Method method = FlutterDesignerMultiViewDesign.class.getDeclaredMethod(
                "selectWidgetFromCanvas", StableId.class);
        method.setAccessible(true);
        method.invoke(design, widgetId);
    }

    private static StableId selectedWidgetId(
            FlutterDesignerMultiViewDesign design) {
        Node[] selected = design.getExplorerManager().getSelectedNodes();
        assertEquals(1, selected.length);
        return selected[0].getLookup().lookup(StableId.class);
    }

    private static Node awaitWidgetNode(
            FlutterDesignerMultiViewDesign design,
            StableId widgetId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<Node> matching = new AtomicReference<>();
        while (System.nanoTime() < deadline) {
            onEdt(() -> matching.set(findWidgetNode(
                    design.getExplorerManager().getRootContext(), widgetId)));
            if (matching.get() != null) {
                return matching.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for widget Node " + widgetId);
    }

    private static void awaitSelectedWidget(
            FlutterDesignerMultiViewDesign design,
            StableId widgetId) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<StableId> selected = new AtomicReference<>();
        while (System.nanoTime() < deadline) {
            onEdt(() -> {
                Node[] nodes = design.getExplorerManager().getSelectedNodes();
                selected.set(nodes.length == 1
                        ? nodes[0].getLookup().lookup(StableId.class)
                        : null);
            });
            if (widgetId.equals(selected.get())) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for selected widget "
                + widgetId + "; last selection=" + selected.get());
    }

    private static FlutterWidgetPropertiesNode.SlotMutationHandler
            awaitSlotMutationHandler(
                    FlutterDesignerMultiViewDesign design,
                    List<String> failures) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<FlutterWidgetPropertiesNode.SlotMutationHandler>
                handler = new AtomicReference<>();
        while (System.nanoTime() < deadline) {
            onEdt(() -> handler.set(design.slotMutationHandlerForTests(
                    (intent, reason) -> failures.add(reason))));
            if (handler.get() != null) {
                return handler.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for the revision-bound slot mutation handler");
    }

    private static FlutterDesignerMutationController.MutationResult
            submitCurrentSlotMutation(
            FlutterDesignerMultiViewDesign design,
            List<String> failures,
            FlutterWidgetSlotMutation mutation) throws Exception {
        AtomicReference<FlutterWidgetPropertiesNode.SlotMutationHandler>
                handler = new AtomicReference<>();
        CompletableFuture<FlutterDesignerMutationController.MutationResult>
                completion = new CompletableFuture<>();
        onEdt(() -> {
            handler.set(design.slotMutationHandlerForTests(
                    (intent, reason) -> failures.add(reason),
                    completion::complete));
            assertNotNull(handler.get(),
                    "the current Properties revision must expose Slots");
            handler.get().submit(mutation);
        });
        return completion.get(10, TimeUnit.SECONDS);
    }

    private static void assertApplied(
            FlutterDesignerMutationController.MutationResult result) {
        assertEquals(
                FlutterDesignerMutationController.Outcome.APPLIED,
                result.outcome(),
                result::reason);
    }

    private static void awaitCanvasDocument(
            FlutterDesignerMultiViewDesign design,
            DesignerDocument expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<DesignerDocument> actual = new AtomicReference<>();
        while (System.nanoTime() < deadline) {
            onEdt(() -> actual.set(design.currentCanvasDocumentForTests()));
            if (actual.get() == expected) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for the exact confirmed Canvas document");
    }

    private static void awaitStableSlotPresentation(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerMutationController controller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        FlutterDesignerMutationController.RevisionToken stableToken = null;
        long stableSince = 0L;
        AtomicReference<DesignerDocument> canvas = new AtomicReference<>();
        AtomicBoolean slotHandlerAvailable = new AtomicBoolean();
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot snapshot =
                    controller.snapshot();
            onEdt(() -> {
                canvas.set(design.currentCanvasDocumentForTests());
                slotHandlerAvailable.set(design.slotMutationHandlerForTests(
                        (intent, reason) -> {
                        }) != null);
            });
            boolean aligned = snapshot.status()
                    == FlutterDesignerMutationController.Status.READY
                    && snapshot.token().isPresent()
                    && snapshot.document().isPresent()
                    && snapshot.document().orElseThrow() == canvas.get()
                    && slotHandlerAvailable.get();
            FlutterDesignerMutationController.RevisionToken token = aligned
                    ? snapshot.token().orElseThrow() : null;
            if (token != null && token == stableToken) {
                if (System.nanoTime() - stableSince
                        >= TimeUnit.MILLISECONDS.toNanos(200)) {
                    return;
                }
            } else {
                stableToken = token;
                stableSince = System.nanoTime();
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for a stable revision-bound slot presentation");
    }

    private static Node findWidgetNode(Node node, StableId widgetId) {
        if (widgetId.equals(node.getLookup().lookup(StableId.class))) {
            return node;
        }
        for (Node child : node.getChildren().getNodes(true)) {
            Node matching = findWidgetNode(child, widgetId);
            if (matching != null) {
                return matching;
            }
        }
        return null;
    }

    private static Node awaitSelectedTextNode(
            FlutterDesignerMultiViewDesign design,
            String expectedData,
            boolean expectedWritable) throws Exception {
        Object expectedDisplay = textDataDisplay(expectedData);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<Node> matching = new AtomicReference<>();
        AtomicReference<String> lastPresentation = new AtomicReference<>("<none>");
        while (System.nanoTime() < deadline) {
            onEdt(() -> {
                Node node = design.getLookup().lookup(Node.class);
                if (node == null) {
                    lastPresentation.set("<no selected Node>");
                    return;
                }
                Node.Property<?> data = findPropertyOrNull(node, "data");
                if (data == null) {
                    lastPresentation.set(node.getDisplayName() + " without data");
                    return;
                }
                Object value = data.getValue();
                lastPresentation.set(node.getDisplayName() + ".data=" + value
                        + ", writable=" + data.canWrite());
                if (TEXT_ID.equals(node.getLookup().lookup(StableId.class))
                        && expectedDisplay.equals(value)
                        && data.canWrite() == expectedWritable) {
                    matching.set(node);
                }
            });
            if (matching.get() != null) {
                return matching.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for selected Text.data="
                + expectedData + ", writable=" + expectedWritable + ": "
                + lastPresentation.get());
    }

    private static Node awaitSelectedTextSoftWrap(
            FlutterDesignerMultiViewDesign design,
            Boolean expectedValue,
            boolean expectedWritable) throws Exception {
        Object expectedDisplay = expectedWritable
                ? expectedValue == null
                        ? FlutterPropertyCellValue.unset()
                        : FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(expectedValue))
                : expectedValue == null
                        ? FlutterWidgetPropertiesNode.NOT_SET
                        : Boolean.toString(expectedValue);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        AtomicReference<Node> matching = new AtomicReference<>();
        AtomicReference<String> lastPresentation = new AtomicReference<>("<none>");
        while (System.nanoTime() < deadline) {
            onEdt(() -> {
                Node node = design.getLookup().lookup(Node.class);
                if (node == null) {
                    lastPresentation.set("<no selected Node>");
                    return;
                }
                Node.Property<?> softWrap = findPropertyOrNull(node, "softWrap");
                if (softWrap == null) {
                    lastPresentation.set(node.getDisplayName() + " without softWrap");
                    return;
                }
                Object value = softWrap.getValue();
                lastPresentation.set(node.getDisplayName() + ".softWrap=" + value
                        + ", writable=" + softWrap.canWrite());
                if (TEXT_ID.equals(node.getLookup().lookup(StableId.class))
                        && expectedDisplay.equals(value)
                        && softWrap.canWrite() == expectedWritable) {
                    matching.set(node);
                }
            });
            if (matching.get() != null) {
                return matching.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for selected Text.softWrap="
                + expectedValue + ", writable=" + expectedWritable + ": "
                + lastPresentation.get());
    }

    private static void assertTextProperties(
            Node node,
            String expectedData,
            boolean dataWritable) throws Exception {
        assertEquals("Text", node.getDisplayName());
        assertEquals(TEXT_ID, node.getLookup().lookup(StableId.class));
        Node.PropertySet properties = Arrays.stream(node.getPropertySets())
                .filter(set -> TextWidgetPropertySchema.Group.CONTENT.setName()
                        .equals(set.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "The selected Text node has no Text property set"));
        assertTrue(properties.getProperties().length >= 5,
                "the Text integration fixture must expose the typed Properties surface");

        int writable = 0;
        for (Node.PropertySet set : node.getPropertySets()) {
            for (Node.Property<?> property : set.getProperties()) {
                if (property.canWrite()) {
                    writable++;
                }
            }
        }
        Node.Property<?> data = findPropertyOrNull(node, "data");
        assertNotNull(data);
        assertEquals(textDataDisplay(expectedData), data.getValue());
        assertEquals(dataWritable, data.canWrite());
        int projectedProperties = Arrays.stream(node.getPropertySets())
                .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME
                        .equals(set.getName()))
                .mapToInt(set -> set.getProperties().length)
                .sum();
        assertEquals(dataWritable ? projectedProperties : 0, writable,
                "every property in the admitted Text Node must share the exact snapshot writer");
        assertEquals(FlutterPropertyCellValue.class, data.getValueType(),
                "APPLYING must disable the stable typed Property, not replace its descriptor");
    }

    private static Object textDataDisplay(String value) {
        return FlutterPropertyCellValue.explicit(
                new PropertyValue.StringValue(value));
    }

    private static Node.Property<?> findPropertyOrNull(Node node, String name) {
        return Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(property -> name.equals(property.getName()))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterPropertyCellValue> textDataProperty(
            Node node) {
        Node.Property<?> property = findPropertyOrNull(node, "data");
        if (property == null) {
            throw new AssertionError("The selected Text node has no data property");
        }
        assertEquals(FlutterPropertyCellValue.class, property.getValueType());
        return (Node.Property<FlutterPropertyCellValue>) property;
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterPropertyCellValue> cellProperty(
            Node node,
            String name) {
        Node.Property<?> property = findPropertyOrNull(node, name);
        if (property == null) {
            throw new AssertionError("The selected node has no " + name + " property");
        }
        assertEquals(FlutterPropertyCellValue.class, property.getValueType());
        assertTrue(property.canWrite(), name);
        return (Node.Property<FlutterPropertyCellValue>) property;
    }

    private static JProgressBar mutationProgress(
            FlutterDesignerMultiViewDesign design) throws Exception {
        Field field = FlutterDesignerMultiViewDesign.class.getDeclaredField("progress");
        field.setAccessible(true);
        return (JProgressBar) field.get(design);
    }

    private static void publishSelectedNodes(
            PropertySheet sheet,
            Lookup.Result<Node> selection) {
        Runnable publish = () -> sheet.setNodes(
                selection.allInstances().toArray(Node[]::new));
        if (EventQueue.isDispatchThread()) {
            publish.run();
        } else {
            EventQueue.invokeLater(publish);
        }
    }

    private static int awaitPropertyRow(JTable table, String name)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            AtomicInteger matchingRow = new AtomicInteger(-1);
            onEdt(() -> {
                for (int row = 0; row < table.getRowCount(); row++) {
                    Object value = table.getValueAt(row, 1);
                    if (value instanceof FeatureDescriptor descriptor
                            && name.equals(descriptor.getName())) {
                        matchingRow.set(row);
                        break;
                    }
                }
            });
            if (matchingRow.get() >= 0) {
                return matchingRow.get();
            }
            Thread.sleep(10);
        }
        throw new AssertionError("PropertySheet row not loaded: " + name);
    }

    private static void awaitPropertySheetSetNodesSettled(
            PropertySheet sheet,
            Node expectedNode) throws Exception {
        assertNotNull(sheet);
        Field scheduleTaskField = PropertySheet.class.getDeclaredField(
                "scheduleTask");
        scheduleTaskField.setAccessible(true);
        Field listenerField = PropertySheet.class.getDeclaredField(
                "pclistener");
        listenerField.setAccessible(true);

        AtomicReference<Task> scheduled = new AtomicReference<>();
        onEdt(() -> scheduled.set((Task) scheduleTaskField.get(sheet)));
        Task initialSelection = scheduled.get();
        assertNotNull(initialSelection,
                "PropertySheet must have scheduled its initial Node publication");
        assertTrue(initialSelection.waitFinished(
                TimeUnit.SECONDS.toMillis(5)),
                "PropertySheet initial Node publication did not finish");

        // The RequestProcessor task publishes doSetNodes through invokeLater.
        // Crossing the EDT after it finishes drains that final publication, so
        // an editor cannot be opened between PropertySheet's immediate and
        // delayed initializers.
        onEdt(() -> { });

        AtomicReference<Node> currentNode = new AtomicReference<>();
        AtomicBoolean taskFinished = new AtomicBoolean();
        onEdt(() -> {
            Task latest = (Task) scheduleTaskField.get(sheet);
            taskFinished.set(latest != null && latest.isFinished());
            Object listener = listenerField.get(sheet);
            Field currentNodeField = listener.getClass().getDeclaredField(
                    "currNode");
            currentNodeField.setAccessible(true);
            currentNode.set((Node) currentNodeField.get(listener));
        });
        assertTrue(taskFinished.get(),
                "PropertySheet selection scheduler must be quiescent");
        assertSame(expectedNode, currentNode.get(),
                "PropertySheet must listen to the selected Node before editing");
    }

    private static <T extends Component> T findFirst(
            Component root,
            Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findFirst(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void awaitDesignerSessionInactive(
            DesignerCombinedUndoRedo combined) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (!combined.designerSessionActive()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError(
                "Timed out waiting for semantic Undo ownership to be released");
    }

    private static void awaitPairStatus(
            PairSaveCoordinator coordinator,
            PairSaveCoordinatorStatus expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            PairSaveCoordinatorStatus actual = coordinator.state().status();
            if (actual == expected) {
                return;
            }
            if (actual == PairSaveCoordinatorStatus.RECOVERY_CONFLICT) {
                throw new AssertionError(
                        "Pair entered recovery conflict while waiting for "
                        + expected + ": " + coordinator.state());
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for pair " + expected
                + ": " + coordinator.state());
    }

    private static void awaitAtomicValue(
            AtomicInteger value,
            int expected,
            String timeoutMessage) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (value.get() == expected) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError(timeoutMessage + "; expected=" + expected
                + ", actual=" + value.get());
    }

    private static void awaitLatchUnchecked(
            CountDownLatch latch,
            String timeoutMessage) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError(timeoutMessage);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(
                    "Interrupted while awaiting analyzer test gate",
                    interrupted);
        }
    }

    private static DartCandidateAnalysisOperation completedAnalysis(
            DartCandidateAnalysisResult result) {
        return new DartCandidateAnalysisOperation() {
            @Override
            public CompletionStage<DartCandidateAnalysisResult> result() {
                return CompletableFuture.completedFuture(result);
            }

            @Override
            public boolean cancel() {
                return false;
            }
        };
    }

    private static DartCandidateAnalysisOperation completedExceptionally(
            Throwable failure) {
        CompletableFuture<DartCandidateAnalysisResult> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(failure);
        return new DartCandidateAnalysisOperation() {
            @Override
            public CompletionStage<DartCandidateAnalysisResult> result() {
                return failed;
            }

            @Override
            public boolean cancel() {
                return failed.cancel(false);
            }
        };
    }

    private static DartCandidateAnalysisOperation gatedAnalysis(
            CompletableFuture<DartCandidateAnalysisResult> gate) {
        return new DartCandidateAnalysisOperation() {
            @Override
            public CompletionStage<DartCandidateAnalysisResult> result() {
                return gate;
            }

            @Override
            public boolean cancel() {
                return gate.cancel(false);
            }
        };
    }

    private static void onEdt(ThrowingRunnable operation) throws Exception {
        if (EventQueue.isDispatchThread()) {
            operation.run();
            return;
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                operation.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
    }

    private static DartCandidateAnalysisResult passingAnalysis(
            DartCandidateAnalysisRequest request,
            Path navigationTarget) {
        List<DartSymbolEvidence> symbolEvidence = request.symbolProbes().stream()
                .map(probe -> acceptedSymbol(probe, navigationTarget))
                .toList();
        return new DartCandidateAnalysisResult(
                DartCandidateAnalysisStatus.PASSED,
                request.snapshot(),
                Optional.of("test"),
                List.of(),
                symbolEvidence.size(),
                symbolEvidence,
                Optional.empty());
    }

    private static DartSymbolEvidence acceptedSymbol(
            DartSymbolProbe probe,
            Path navigationTarget) {
        return new DartSymbolEvidence(
                probe,
                List.of(new DartNavigationTarget(
                        probe.expectedTargetKind().orElse("CLASS"),
                        navigationTarget,
                        0,
                        1,
                        1,
                        1)),
                true,
                Optional.empty());
    }

    private static StyledDocument openGuardedSourceDocument(
            FlutterDesignerEditorSupport editor) throws Exception {
        assertNull(editor.getDocument());
        Field kitField = CloneableEditorSupport.class.getDeclaredField("kit");
        kitField.setAccessible(true);
        kitField.set(editor, new DartEditorKit());
        Class<?> bridgeClass = Class.forName(
                FlutterDesignerEditorSupport.class.getName()
                + "$GuardedDocumentBridge");
        Constructor<?> bridgeConstructor = bridgeClass.getDeclaredConstructor();
        bridgeConstructor.setAccessible(true);
        GuardedEditorSupport guardedEditor = (GuardedEditorSupport)
                bridgeConstructor.newInstance();
        DartGuardedSectionsProvider provider =
                new DartGuardedSectionsProvider(guardedEditor);
        Field editorField = FlutterDesignerEditorSupport.class
                .getDeclaredField("guardedEditor");
        editorField.setAccessible(true);
        editorField.set(editor, guardedEditor);
        Field providerField = FlutterDesignerEditorSupport.class
                .getDeclaredField("guardedProvider");
        providerField.setAccessible(true);
        providerField.set(editor, provider);
        StyledDocument document = editor.openDocument();
        Field wrappers = BaseDocument.class.getDeclaredField(
                "undoEditWrappers");
        wrappers.setAccessible(true);
        wrappers.set(document, List.of(new DesignerUndoableEditWrapper()));
        return document;
    }

    private static SetProperty setText(String value) {
        return new SetProperty(
                TEXT_ID,
                DATA,
                new PropertyValue.StringValue(value));
    }

    private static String textData(
            FlutterDesignerMutationController.Snapshot snapshot) {
        return textData(snapshot.document().orElseThrow());
    }

    private static String textData(DesignerDocument document) {
        PropertyValue value = document.root().properties().get(DATA);
        return ((PropertyValue.StringValue) value).value();
    }

    private static Boolean textSoftWrap(
            FlutterDesignerMutationController.Snapshot snapshot) {
        PropertyValue value = snapshot.document().orElseThrow()
                .root().properties().get(SOFT_WRAP);
        return value instanceof PropertyValue.BooleanValue booleanValue
                ? booleanValue.value() : null;
    }

    private static Object sessionOwner(
            FlutterDesignerMutationController controller)
            throws ReflectiveOperationException {
        Field field = FlutterDesignerMutationController.class
                .getDeclaredField("sessionOwner");
        field.setAccessible(true);
        return field.get(controller);
    }

    private static DesignerDocument document(
            DartSourceDescriptor source,
            String text) {
        WidgetNode root = new WidgetNode(
                TEXT_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue(text)),
                Map.of());
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static DesignerDocument sizedOverflowBoxDocument(
            DartSourceDescriptor source,
            PropertyValue.SizeValue size) {
        WidgetNode root = new WidgetNode(
                TEXT_ID,
                new WidgetTypeId("flutter.widgets.SizedOverflowBox"),
                Map.of(SIZE, size),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static DesignerDocument transformDocument(
            DartSourceDescriptor source,
            PropertyValue.OffsetValue origin) {
        WidgetNode root = new WidgetNode(
                TEXT_ID,
                new WidgetTypeId("flutter.widgets.Transform"),
                Map.of(
                        TRANSFORM, identityMatrix(),
                        ORIGIN, origin),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static PropertyValue.Matrix4Value identityMatrix() {
        java.util.ArrayList<java.math.BigDecimal> storage =
                new java.util.ArrayList<>(16);
        for (int index = 0; index < 16; index++) {
            storage.add(index % 5 == 0
                    ? java.math.BigDecimal.ONE
                    : java.math.BigDecimal.ZERO);
        }
        return new PropertyValue.Matrix4Value(storage);
    }

    private static DesignerDocument columnDocument(DartSourceDescriptor source) {
        WidgetNode first = new WidgetNode(
                FIRST_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode second = new WidgetNode(
                SECOND_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode root = new WidgetNode(
                COLUMN_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second))));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static DesignerDocument indexedStackColumnDocument(
            DartSourceDescriptor source) {
        WidgetNode first = new WidgetNode(
                FIRST_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode second = new WidgetNode(
                SECOND_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("same")),
                Map.of());
        WidgetNode indexedFirst = new WidgetNode(
                INDEXED_FIRST_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("indexed first")),
                Map.of());
        WidgetNode indexedSecond = new WidgetNode(
                INDEXED_SECOND_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("indexed second")),
                Map.of());
        WidgetNode indexedStack = new WidgetNode(
                INDEXED_STACK_ID,
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE,
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(
                        List.of(indexedFirst, indexedSecond))));
        WidgetNode root = new WidgetNode(
                COLUMN_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(
                                List.of(first, second, indexedStack))));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }

    private static void assertColumnChildren(
            DesignerDocument document,
            List<StableId> expectedIds) {
        assertEquals(COLUMN_ID, document.root().id());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                document.root().slots().get(CHILDREN));
        assertEquals(expectedIds, children.children().stream()
                .map(WidgetNode::id)
                .toList());
    }

    private static int firstStatusIndex(
            List<FlutterDesignerMutationController.Snapshot> snapshots,
            FlutterDesignerMutationController.Status status) {
        return firstStatusIndexAfter(snapshots, status, -1);
    }

    private static int firstStatusIndexAfter(
            List<FlutterDesignerMutationController.Snapshot> snapshots,
            FlutterDesignerMutationController.Status status,
            int precedingIndex) {
        for (int index = precedingIndex + 1; index < snapshots.size(); index++) {
            if (snapshots.get(index).status() == status) {
                return index;
            }
        }
        return -1;
    }

    private static DesignerDocument scaffoldCenterTextDocument(
            DartSourceDescriptor source) {
        WidgetNode text = new WidgetNode(
                NESTED_TEXT_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA,
                        new PropertyValue.StringValue("nested subtree")),
                Map.of());
        WidgetNode center = new WidgetNode(
                CENTER_ID,
                new WidgetTypeId("flutter.widgets.Center"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text)));
        WidgetNode scaffold = new WidgetNode(
                SCAFFOLD_ID,
                new WidgetTypeId("flutter.material.Scaffold"),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(center)));
        return new DesignerDocument(DOCUMENT_ID, source, scaffold);
    }

    private static DesignerDocument centerAndSourceDocument(
            DartSourceDescriptor source) {
        WidgetNode obsolete = new WidgetNode(
                FIRST_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("obsolete")),
                Map.of());
        WidgetNode replacement = new WidgetNode(
                SECOND_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("replacement")),
                Map.of());
        WidgetNode center = new WidgetNode(
                CENTER_ID,
                new WidgetTypeId("flutter.widgets.Center"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(obsolete)));
        WidgetNode column = new WidgetNode(
                COLUMN_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(center, replacement))));
        return new DesignerDocument(DOCUMENT_ID, source, column);
    }

    private static void assertExistingReplacement(DesignerDocument document) {
        assertColumnChildren(document, List.of(CENTER_ID));
        WidgetNode center = findModelWidget(document.root(), CENTER_ID);
        assertNotNull(center);
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class, center.slots().get(CHILD));
        WidgetNode replacement = child.child().orElseThrow();
        assertEquals(SECOND_ID, replacement.id());
        assertEquals(new PropertyValue.StringValue("replacement"),
                replacement.properties().get(DATA));
        assertFalse(containsWidget(document.root(), FIRST_ID),
                "replacement must remove the exact former single-slot subtree");
    }

    private static DesignerDocument scaffoldCenterEmptyColumnDocument(
            DartSourceDescriptor source) {
        WidgetNode column = new WidgetNode(
                COLUMN_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())));
        WidgetNode center = new WidgetNode(
                CENTER_ID,
                new WidgetTypeId("flutter.widgets.Center"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(column)));
        WidgetNode scaffold = new WidgetNode(
                SCAFFOLD_ID,
                new WidgetTypeId("flutter.material.Scaffold"),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(center)));
        return new DesignerDocument(DOCUMENT_ID, source, scaffold);
    }

    private static WidgetNode assertScaffoldCenterColumnText(
            DesignerDocument document,
            StableId expectedTextId) {
        WidgetNode scaffold = document.root();
        assertEquals(SCAFFOLD_ID, scaffold.id());
        assertEquals(new WidgetTypeId("flutter.material.Scaffold"),
                scaffold.type());
        WidgetSlot.SingleSlot body = assertInstanceOf(
                WidgetSlot.SingleSlot.class, scaffold.slots().get(BODY));
        WidgetNode center = body.child().orElseThrow();
        assertEquals(CENTER_ID, center.id());
        assertEquals(new WidgetTypeId("flutter.widgets.Center"), center.type());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class, center.slots().get(CHILD));
        WidgetNode column = child.child().orElseThrow();
        assertEquals(COLUMN_ID, column.id());
        assertEquals(new WidgetTypeId("flutter.widgets.Column"), column.type());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class, column.slots().get(CHILDREN));
        if (expectedTextId == null) {
            assertTrue(children.children().isEmpty());
            return null;
        }
        assertEquals(1, children.children().size());
        WidgetNode text = children.children().getFirst();
        assertEquals(expectedTextId, text.id());
        assertEquals(new WidgetTypeId("flutter.widgets.Text"), text.type());
        return text;
    }

    private static void assertGeneratedScaffoldCenterColumnText(byte[] dart) {
        String source = new String(dart, StandardCharsets.UTF_8);
        int scaffold = source.indexOf("Scaffold(");
        int center = source.indexOf("Center(", scaffold + 1);
        int column = source.indexOf("Column(", center + 1);
        int children = source.indexOf("children: [", column + 1);
        int text = source.indexOf("Text(", children + 1);
        assertTrue(scaffold >= 0, source);
        assertTrue(center > scaffold, source);
        assertTrue(column > center, source);
        assertTrue(children > column, source);
        assertTrue(text > children, source);
        assertTrue(source.indexOf("'Text'", text) > text, source);
    }

    private static DartSourceDescriptor descriptor(
            String importsHash,
            String buildHash) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static String source(GeneratedDartRegions generated) {
        return "// <netbeans-flutter-designer region=\"imports\">\n"
                + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record ExactPair(byte[] dartBytes, byte[] fdBytes) {
    }

    private record FakeSdk(
            Path flutterRoot,
            Path dartExecutable,
            Path frameworkFile) {
    }

    private static final class SyntheticVirtualMachineError
            extends VirtualMachineError {
        private SyntheticVirtualMachineError(String message) {
            super(message);
        }
    }

    private record MutationFixture(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerDocumentController controller,
            FlutterDesignerMutationController mutations,
            PairSaveCoordinator coordinator,
            FlutterDesignerEditorSupport editor,
            StyledDocument document,
            Path dartPath,
            Path fdPath,
            byte[] baselineDart,
            byte[] baselineFd,
            Path frameworkFile,
            FlutterDesignerDocumentState.Current current,
            FlutterDesignerMutationController.Snapshot ready,
            FlutterSettings settings,
            FlutterToolchainConfig previousSettings,
            AtomicInteger analysisCalls,
            List<String> analyzedContents) implements AutoCloseable {

        @Override
        public void close() {
            Throwable cleanupFailure = null;
            try {
                mutations.closeForDataObjectDisposal();
            } catch (RuntimeException | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
            try {
                controller.viewClosed();
            } catch (RuntimeException | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
            try {
                // Tests intentionally leave staged and dirty editor state. Discard
                // it before @TempDir removal so NetBeans cannot open a delayed
                // read-only-close question from a later filesystem event.
                dataObject.setModified(false);
                if (!editor.close()) {
                    cleanupFailure = appendCleanupFailure(
                            cleanupFailure,
                            new AssertionError(
                                    "the synthetic Designer editor refused test cleanup"));
                }
                if (editor.getDocument() != null) {
                    // openDocument() does not create an editor pane, so the
                    // standard close() has no last pane from which to deliver
                    // CloneableEditorSupport.notifyClosed(). Complete that
                    // lifecycle callback explicitly for this synthetic fixture.
                    onEdt(editor::notifyClosed);
                }
                if (!awaitEditorDocumentClosed(editor)) {
                    cleanupFailure = appendCleanupFailure(
                            cleanupFailure,
                            new AssertionError(
                                    "the synthetic Designer editor retained its document after cleanup"));
                }
                if (dataObject.isValid()) {
                    // The fixture creates the DataObject directly and therefore
                    // must complete its lifecycle explicitly. Otherwise its
                    // staged PairSaveCoordinator remains in DataObjectPool and
                    // can react to an unrelated later test's file rename by
                    // opening NetBeans' read-only-close confirmation dialog.
                    dataObject.dispose();
                }
            } catch (Exception | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
            try {
                settings.save(previousSettings);
            } catch (RuntimeException | Error failure) {
                cleanupFailure = appendCleanupFailure(cleanupFailure, failure);
            }
            if (cleanupFailure instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cleanupFailure instanceof Error error) {
                throw error;
            }
        }

        private static boolean awaitEditorDocumentClosed(
                FlutterDesignerEditorSupport editor) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                if (editor.getDocument() == null) {
                    return true;
                }
                onEdt(() -> { });
                Thread.sleep(10);
            }
            return editor.getDocument() == null;
        }

        private static Throwable appendCleanupFailure(
                Throwable accumulated,
                Throwable failure) {
            if (accumulated == null) {
                return failure;
            }
            accumulated.addSuppressed(failure);
            return accumulated;
        }
    }
}

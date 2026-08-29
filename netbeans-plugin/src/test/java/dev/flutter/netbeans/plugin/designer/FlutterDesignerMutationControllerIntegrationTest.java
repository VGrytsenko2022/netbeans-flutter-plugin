package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import dev.flutter.netbeans.designer.catalog.TextWidgetPropertySchema;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerRevisionPersistenceKind;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.RemoveWidget;
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
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.settings.FlutterSettings;
import dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
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
import javax.swing.JProgressBar;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;

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
    private static final PropertyName DATA = new PropertyName("data");
    private static final PropertyName SOFT_WRAP = new PropertyName("softWrap");
    private static final PropertyName TEXT_ALIGN = new PropertyName("textAlign");
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
    void multiViewTextDataMutationRebuildsPropertiesTreeAndCanvasFromExactSnapshot()
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
                    assertNotSame(baselineNode, appliedNode,
                            "a confirmed immutable snapshot must rebuild the selected node");
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
    void multiViewTypedBooleanSetAndResetAreOneShotAndRebuildExactProperties()
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
                assertNotSame(baselineNode, appliedNode,
                        "the confirmed typed value must rebuild the selected Node");
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
                assertNotSame(appliedNode, resetNode,
                        "ResetProperty must rebuild the immutable Properties Node");
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
    void multiViewRejectsAnUnusedStalePropertyNodeBeforeAnalyzerAdmission()
            throws Exception {
        MutationFixture fixture = fixture("mutation_multiview_stale_property_node");
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        AtomicBoolean designClosed = new AtomicBoolean();

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
                Node staleNode = awaitSelectedTextSoftWrap(design, null, true);

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
                assertNotSame(staleNode, currentNode);
                assertEquals(1, fixture.analysisCalls().get());

                // Closing only the view suppresses the expected stale-result
                // dialog; the DataObject-owned controller remains alive so the
                // captured exact token is still checked by the real bridge.
                onEdt(design::componentClosed);
                designClosed.set(true);
                onEdt(() -> cellProperty(staleNode, "softWrap").setValue(
                        FlutterPropertyCellValue.explicit(
                                new PropertyValue.BooleanValue(false))));

                assertEquals(1, fixture.analysisCalls().get(),
                        "the stale MultiView token must reject before analyzer admission");
                assertEquals("external", textData(fixture.mutations().snapshot()));
                assertNull(textSoftWrap(fixture.mutations().snapshot()));
            } finally {
                FlutterDesignerMultiViewDesign design = designRef.get();
                if (design != null && !designClosed.get()) {
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
    void fdOnlyCommandIsRejectedBeforeAnalyzerAndReleasesItsUnusedOwner()
            throws Exception {
        MutationFixture fixture = fixture(
                "mutation_fd_only_rejection", columnExactPair());
        try (fixture) {
            FlutterDesignerMutationController.Snapshot baseline = fixture.ready();
            FlutterDesignerMutationController.RevisionToken baselineToken =
                    baseline.token().orElseThrow();

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
            assertTrue(rejected.reason().contains(".fd-only"),
                    rejected::reason);
            FlutterDesignerMutationController.Snapshot recovered =
                    awaitReady(fixture.mutations());
            assertNotSame(baselineToken, recovered.token().orElseThrow());
            assertEquals(0, fixture.analysisCalls().get(),
                    "FD_ONLY must be rejected before analyzer admission");
            assertEquals(PairSaveCoordinatorStatus.CLEAN,
                    fixture.coordinator().state().status());
            assertNull(fixture.coordinator().stagedEvidence());
            assertNull(fixture.coordinator().stagedProofSnapshot());
            assertFalse(fixture.dataObject().getCombinedUndoRedo()
                    .designerSessionActive(),
                    "a rejected first command must not retain semantic Undo ownership");
            assertArrayEquals(fixture.baselineDart(),
                    fixture.editor().liveSnapshot().markerBearingUtf8());
            assertArrayEquals(fixture.baselineDart(),
                    Files.readAllBytes(fixture.dartPath()));
            assertArrayEquals(fixture.baselineFd(),
                    Files.readAllBytes(fixture.fdPath()));
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
        Object expectedDisplay = textDataDisplay(expectedData, expectedWritable);
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
        assertEquals(textDataDisplay(expectedData, dataWritable), data.getValue());
        assertEquals(dataWritable, data.canWrite());
        int projectedProperties = Arrays.stream(node.getPropertySets())
                .filter(set -> !FlutterWidgetPropertiesNode.IDENTITY_SET_NAME
                        .equals(set.getName()))
                .mapToInt(set -> set.getProperties().length)
                .sum();
        assertEquals(dataWritable ? projectedProperties : 0, writable,
                "every property in the admitted Text Node must share the exact snapshot writer");
        if (dataWritable) {
            assertEquals(FlutterPropertyCellValue.class, data.getValueType());
        } else {
            assertEquals(String.class, data.getValueType());
        }
    }

    private static Object textDataDisplay(String value, boolean writable) {
        return writable
                ? FlutterPropertyCellValue.explicit(
                        new PropertyValue.StringValue(value))
                : '"' + value + '"';
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

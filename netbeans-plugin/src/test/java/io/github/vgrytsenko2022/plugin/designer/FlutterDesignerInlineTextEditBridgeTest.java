package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.dart.DartCandidateAnalysisOperation;
import io.github.vgrytsenko2022.dart.DartCandidateAnalysisRequest;
import io.github.vgrytsenko2022.dart.DartCandidateAnalysisResult;
import io.github.vgrytsenko2022.dart.DartCandidateAnalysisStatus;
import io.github.vgrytsenko2022.dart.DartNavigationTarget;
import io.github.vgrytsenko2022.dart.DartSymbolEvidence;
import io.github.vgrytsenko2022.dart.DartSymbolProbe;
import io.github.vgrytsenko2022.designer.canvas.CanvasFrameKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentId;
import io.github.vgrytsenko2022.designer.canvas.CanvasIntentKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasLayoutKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasRevisionKey;
import io.github.vgrytsenko2022.designer.canvas.CanvasSessionId;
import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.dart.DartEditorKit;
import io.github.vgrytsenko2022.plugin.designer.canvas.CanvasRunnerRuntimeEvent;
import io.github.vgrytsenko2022.plugin.designer.guard.DartGuardedSectionsProvider;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import io.github.vgrytsenko2022.plugin.settings.FlutterSettings;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainConfig;
import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Lookup;

/**
 * Product-view admission tests for final inline Canvas edits of
 * {@code Text.data}.
 *
 * <p>The harness starts at the post-session listener boundary: wire capability,
 * session, fence and replay admission belong to their narrower tests. These
 * tests prove that an already admitted event crosses the view mutation boundary
 * once, and that stale view identity or local selection/model mismatches do not.
 * </p>
 */
class FlutterDesignerInlineTextEditBridgeTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId COLUMN_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final StableId TEXT_ID = StableId.parse(
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final StableId SIZED_BOX_ID = StableId.parse(
            "dddddddd-dddd-4ddd-8ddd-dddddddddddd");
    private static final PropertyName DATA = new PropertyName("data");
    private static final SlotName CHILDREN = new SlotName("children");

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptedCompositionCommitSubmitsExactlyOneTextDataMutation()
            throws Exception {
        try (MutationFixture fixture = fixture("inline_text_accepted");
                DesignHarness harness = designHarness(fixture)) {
            AtomicInteger analysisCalls = new AtomicInteger();
            AtomicReference<DartCandidateAnalysisRequest> requestRef =
                    new AtomicReference<>();
            CountDownLatch analysisStarted = new CountDownLatch(1);
            CompletableFuture<DartCandidateAnalysisResult> analysisGate =
                    new CompletableFuture<>();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analysisCalls.incrementAndGet();
                        requestRef.set(request);
                        analysisStarted.countDown();
                        return gatedAnalysis(analysisGate);
                    });

            FlutterDesignerMutationController.Snapshot before =
                    fixture.mutations().snapshot();
            CanvasRunnerRuntimeEvent.TextEditCommit commit = commit(
                    TEXT_ID, "after composition", true);
            assertTrue(commit.compositionObserved(),
                    "the accepted product event must retain the runner's IME fact");

            invokeCommit(harness.design(), commit);
            invokeCommit(harness.design(), commit);

            assertTrue(analysisStarted.await(10, TimeUnit.SECONDS),
                    "the accepted view event never reached the mutation analyzer");
            assertEquals(1, analysisCalls.get(),
                    "a repeated final event while the first edit is active must not resubmit");
            assertEquals(FlutterDesignerMutationController.Status.APPLYING,
                    fixture.mutations().snapshot().status());
            DartCandidateAnalysisRequest request = requestRef.get();
            assertNotNull(request);
            assertTrue(request.content().contains("after composition"),
                    request::content);
            assertFalse(request.content().contains("'before'"),
                    request::content);

            analysisGate.complete(passingAnalysis(
                    request, fixture.frameworkFile()));
            FlutterDesignerMutationController.Snapshot after =
                    awaitReadyWithTextAfterToken(
                            fixture.mutations(), before.token().orElseThrow(),
                            "after composition");
            awaitInlineSubmissionFinished(harness.design());

            assertEquals(1, analysisCalls.get());
            assertTextDataOnlyChange(
                    before.document().orElseThrow(),
                    after.document().orElseThrow(),
                    "after composition");
        }
    }

    @Test
    void staleSelectionWrongWidgetAndNoOpNeverSubmitFromTheView()
            throws Exception {
        try (MutationFixture fixture = fixture("inline_text_rejected");
                DesignHarness harness = designHarness(fixture)) {
            AtomicInteger analysisCalls = new AtomicInteger();
            fixture.mutations().setAnalyzerFactoryForTests(
                    (dartExecutable, request) -> {
                        analysisCalls.incrementAndGet();
                        return completedAnalysis(passingAnalysis(
                                request, fixture.frameworkFile()));
                    });
            FlutterDesignerMutationController.Snapshot ready =
                    fixture.mutations().snapshot();
            Object exactToken = ready.token().orElseThrow();
            DesignerDocument exactDocument = ready.document().orElseThrow();

            CanvasRunnerRuntimeEvent.TextEditCommit noOp =
                    commit(TEXT_ID, "before", true);
            assertTrue(noOp.compositionObserved());
            invokeCommit(harness.design(), noOp);

            selectWidget(harness.design(), COLUMN_ID);
            invokeCommit(harness.design(),
                    commit(TEXT_ID, "wrong selection", true));
            invokeCommit(harness.design(),
                    commit(COLUMN_ID, "wrong widget", true));

            selectWidget(harness.design(), TEXT_ID);
            DesignerDocument stalePresentation = new DesignerDocument(
                    exactDocument.documentId(),
                    exactDocument.source(),
                    exactDocument.root());
            setField(harness.design(), "presentedCanvasDocument",
                    stalePresentation);
            invokeCommit(harness.design(),
                    commit(TEXT_ID, "stale presentation", true));

            assertEquals(0, analysisCalls.get());
            assertEquals(FlutterDesignerMutationController.Status.READY,
                    fixture.mutations().snapshot().status());
            assertSame(exactToken,
                    fixture.mutations().snapshot().token().orElseThrow());
            assertSame(exactDocument,
                    fixture.mutations().snapshot().document().orElseThrow());
            assertFalse(booleanField(harness.design(),
                    "inlineTextEditSubmitting"));
        }
    }

    private MutationFixture fixture(String name) throws Exception {
        Path projectRoot = Files.createDirectory(temporaryDirectory.resolve(name));
        Path lib = Files.createDirectories(projectRoot.resolve("lib"));
        Path modelRoot = Files.createDirectories(
                projectRoot.resolve(".fd_templates"));
        Files.writeString(projectRoot.resolve("pubspec.yaml"), """
                name: inline_text_bridge_fixture
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
            ExactPair pair = exactPair();
            Files.write(dartPath, pair.dartBytes());
            Files.write(fdPath, pair.fdBytes());
            FileUtil.refreshFor(projectRoot.toFile());

            FileObject dartFile = FileUtil.toFileObject(dartPath.toFile());
            assertNotNull(dartFile);
            FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                    .dataObject(dartFile, project);
            mutations = dataObject.mutationController();
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
                    editor,
                    document,
                    current,
                    ready,
                    sdk.frameworkFile(),
                    settings,
                    previousSettings);
        } catch (Exception | Error failure) {
            if (mutations != null) {
                mutations.closeForDataObjectDisposal();
            }
            if (controller != null) {
                controller.viewClosed();
            }
            settings.save(previousSettings);
            throw failure;
        }
    }

    private DesignHarness designHarness(MutationFixture fixture)
            throws Exception {
        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY, () -> true);
            setField(design, "mutationController", fixture.mutations());
            setField(design, "mutationSnapshot", fixture.ready());
            setField(design, "durableDocumentState", fixture.current());
            setField(design, "mutationListening", true);
            setField(design, "componentLifecycleOpen", true);
            invokeRenderMutationSnapshot(design, fixture.ready());
            setField(design, "presentedCanvasDocument",
                    fixture.ready().document().orElseThrow());
            setField(design, "presentedCanvasCatalog",
                    fixture.ready().catalog().orElseThrow());
            selectWidgetNow(design, TEXT_ID);
            designRef.set(design);
        });
        return new DesignHarness(designRef.get());
    }

    private ExactPair exactPair() throws Exception {
        DesignerDocument provisional = document(descriptor(
                "0".repeat(64), "0".repeat(64)), "before");
        GeneratedDartRegions provisionalGenerated = new DartRegionGenerator()
                .generate(provisional, BuiltInWidgetCatalog.getDefault())
                .generated().orElseThrow();
        DesignerDocument exact = document(descriptor(
                provisionalGenerated.imports().normalizedSha256(),
                provisionalGenerated.build().normalizedSha256()), "before");
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
        createExecutable(flutterRoot.resolve("bin").resolve(
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
                class Column extends Widget {}
                class SizedBox extends Widget {}
                """, StandardCharsets.UTF_8);
        return new FakeSdk(
                flutterRoot.toRealPath(),
                dartExecutable.toRealPath(),
                framework.toRealPath());
    }

    private static DesignerDocument document(
            DartSourceDescriptor source,
            String text) {
        WidgetNode textNode = new WidgetNode(
                TEXT_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue(text)),
                Map.of());
        WidgetNode sizedBox = new WidgetNode(
                SIZED_BOX_ID,
                new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(),
                Map.of());
        WidgetNode root = new WidgetNode(
                COLUMN_ID,
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(textNode, sizedBox))));
        return new DesignerDocument(DOCUMENT_ID, source, root);
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

    private static CanvasRunnerRuntimeEvent.TextEditCommit commit(
            StableId widgetId,
            String text,
            boolean compositionObserved) {
        CanvasSessionId sessionId = CanvasSessionId.random();
        CanvasRevisionKey revision = new CanvasRevisionKey(
                sessionId, 1, DOCUMENT_ID, 1);
        CanvasIntentKey intentKey = new CanvasIntentKey(
                new CanvasIntentId(sessionId, 1),
                new CanvasLayoutKey(new CanvasFrameKey(revision, 1), 1));
        return new CanvasRunnerRuntimeEvent.TextEditCommit(
                intentKey, 1, widgetId, text, compositionObserved);
    }

    private static void assertTextDataOnlyChange(
            DesignerDocument before,
            DesignerDocument after,
            String expectedText) {
        assertEquals(before.documentId(), after.documentId());
        assertEquals(before.source().dartFile(), after.source().dartFile());
        assertEquals(before.source().className(), after.source().className());
        assertEquals(before.source().widgetKind(), after.source().widgetKind());
        assertEquals(before.source().generatorVersion(),
                after.source().generatorVersion());
        assertEquals(before.source().managedRegions().imports(),
                after.source().managedRegions().imports());
        assertNotEquals(before.source().managedRegions().build(),
                after.source().managedRegions().build(),
                "Text.data must regenerate the exact managed build hash");
        assertEquals(before.root().id(), after.root().id());
        assertEquals(before.root().type(), after.root().type());
        assertEquals(before.root().properties(), after.root().properties());

        WidgetSlot.ListSlot beforeChildren = (WidgetSlot.ListSlot)
                before.root().slots().get(CHILDREN);
        WidgetSlot.ListSlot afterChildren = (WidgetSlot.ListSlot)
                after.root().slots().get(CHILDREN);
        assertEquals(2, afterChildren.children().size());
        WidgetNode beforeText = beforeChildren.children().get(0);
        WidgetNode afterText = afterChildren.children().get(0);
        assertEquals(beforeText.id(), afterText.id());
        assertEquals(beforeText.type(), afterText.type());
        assertEquals(beforeText.slots(), afterText.slots());
        assertEquals(Map.of(DATA, new PropertyValue.StringValue(expectedText)),
                afterText.properties());
        assertEquals(beforeChildren.children().get(1),
                afterChildren.children().get(1),
                "the non-Text sibling must not be part of the mutation");
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
            awaitReadyWithTextAfterToken(
                    FlutterDesignerMutationController controller,
                    FlutterDesignerMutationController.RevisionToken oldToken,
                    String expectedText) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            FlutterDesignerMutationController.Snapshot current =
                    controller.snapshot();
            if (current.status()
                    == FlutterDesignerMutationController.Status.READY
                    && current.token().isPresent()
                    && current.token().orElseThrow() != oldToken
                    && expectedText.equals(textData(
                            current.document().orElseThrow()))) {
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
                + expectedText + ": " + controller.snapshot());
    }

    private static void awaitInlineSubmissionFinished(
            FlutterDesignerMultiViewDesign design) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> submitting = new AtomicReference<>();
            onEdt(() -> submitting.set(booleanField(
                    design, "inlineTextEditSubmitting")));
            if (!submitting.get()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("inline Text edit submission did not finish");
    }

    private static String textData(DesignerDocument document) {
        WidgetNode text = ((WidgetSlot.ListSlot) document.root().slots()
                .get(CHILDREN)).children().get(0);
        return ((PropertyValue.StringValue) text.properties().get(DATA)).value();
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
                Optional.empty(),
                probe.staticTypeProbe().map(staticType ->
                        new io.github.vgrytsenko2022.dart.DartStaticTypeEvidence(
                                staticType, true, Optional.empty())));
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
        Field wrappers = BaseDocument.class.getDeclaredField("undoEditWrappers");
        wrappers.setAccessible(true);
        wrappers.set(document, List.of(new DesignerUndoableEditWrapper()));
        return document;
    }

    private static void invokeCommit(
            FlutterDesignerMultiViewDesign design,
            CanvasRunnerRuntimeEvent.TextEditCommit commit) throws Exception {
        onEdt(() -> {
            Method method = FlutterDesignerMultiViewDesign.class
                    .getDeclaredMethod(
                            "applyInlineTextEditCommit",
                            CanvasRunnerRuntimeEvent.TextEditCommit.class);
            method.setAccessible(true);
            method.invoke(design, commit);
        });
    }

    private static void invokeRenderMutationSnapshot(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerMutationController.Snapshot snapshot)
            throws Exception {
        Method method = FlutterDesignerMultiViewDesign.class.getDeclaredMethod(
                "renderMutationSnapshot",
                FlutterDesignerMutationController.Snapshot.class);
        method.setAccessible(true);
        method.invoke(design, snapshot);
    }

    private static void selectWidget(
            FlutterDesignerMultiViewDesign design,
            StableId widgetId) throws Exception {
        onEdt(() -> selectWidgetNow(design, widgetId));
    }

    private static void selectWidgetNow(
            FlutterDesignerMultiViewDesign design,
            StableId widgetId) throws Exception {
        Method method = FlutterDesignerMultiViewDesign.class.getDeclaredMethod(
                "selectWidgetFromCanvas", StableId.class);
        method.setAccessible(true);
        method.invoke(design, widgetId);
    }

    private static void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = FlutterDesignerMultiViewDesign.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static boolean booleanField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = FlutterDesignerMultiViewDesign.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
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
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        Throwable thrown = failure.get();
        if (thrown instanceof Exception exception) {
            throw exception;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
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

    private record DesignHarness(
            FlutterDesignerMultiViewDesign design) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            onEdt(design::componentClosed);
        }
    }

    private record MutationFixture(
            FlutterDesignerDataObject dataObject,
            FlutterDesignerDocumentController controller,
            FlutterDesignerMutationController mutations,
            FlutterDesignerEditorSupport editor,
            StyledDocument document,
            FlutterDesignerDocumentState.Current current,
            FlutterDesignerMutationController.Snapshot ready,
            Path frameworkFile,
            FlutterSettings settings,
            FlutterToolchainConfig previousSettings) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            Throwable failure = null;
            try {
                mutations.closeForDataObjectDisposal();
            } catch (RuntimeException | Error thrown) {
                failure = thrown;
            }
            try {
                controller.viewClosed();
                dataObject.setModified(false);
                if (!editor.close()) {
                    throw new AssertionError(
                            "the synthetic Designer editor refused test cleanup");
                }
                if (editor.getDocument() != null) {
                    onEdt(editor::notifyClosed);
                }
                if (dataObject.isValid()) {
                    dataObject.dispose();
                }
            } catch (Exception | Error thrown) {
                if (failure == null) {
                    failure = thrown;
                } else {
                    failure.addSuppressed(thrown);
                }
            }
            try {
                settings.save(previousSettings);
            } catch (RuntimeException | Error thrown) {
                if (failure == null) {
                    failure = thrown;
                } else {
                    failure.addSuppressed(thrown);
                }
            }
            if (failure instanceof Exception exception) {
                throw exception;
            }
            if (failure instanceof Error error) {
                throw error;
            }
        }
    }
}

package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.plugin.dart.DartEditorKit;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ActionMap;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.PrintCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.SaveAsCapable;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.DataEditorSupport;
import org.openide.windows.CloneableTopComponent;

/** Structural contract for the designer-specific source editor support. */
class FlutterDesignerEditorSupportTest {
    private static final String IMPORTS_PAYLOAD =
            "import 'package:flutter/widgets.dart';\n";
    private static final String BUILD_PAYLOAD = "  @override\n"
            + "  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n"
            + "  }\n";
    private static final String SOURCE =
            "// <netbeans-flutter-designer region=\"imports\">\n"
            + IMPORTS_PAYLOAD
            + "// </netbeans-flutter-designer>\n\n"
            + "class HomePage extends StatelessWidget {\n"
            + "  // <netbeans-flutter-designer region=\"build\">\n"
            + BUILD_PAYLOAD
            + "  // </netbeans-flutter-designer>\n"
            + "}\n";

    @TempDir
    Path temporaryDirectory;

    private final List<EditorFixture> openFixtures = new ArrayList<>();

    @AfterEach
    void closeEditorFixturesBeforeTemporaryFilesAreRemoved() {
        AssertionError cleanupFailure = null;
        for (int index = openFixtures.size() - 1; index >= 0; index--) {
            EditorFixture fixture = openFixtures.get(index);
            try {
                // These tests deliberately exercise dirty and recovery states.
                // Discard that synthetic state before JUnit deletes @TempDir;
                // otherwise NetBeans delivers a later file-delete event to a
                // modified DataEditorSupport and opens a modal read-only-close
                // question in an unrelated following test.
                fixture.editor().getDataObject().setModified(false);
                if (!fixture.editor().close()) {
                    throw new AssertionError(
                            "the synthetic Designer editor refused test cleanup");
                }
            } catch (RuntimeException | AssertionError failure) {
                if (cleanupFailure == null) {
                    cleanupFailure = new AssertionError(
                            "Cannot clean up a Designer editor test fixture");
                }
                cleanupFailure.addSuppressed(failure);
            }
        }
        openFixtures.clear();
        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    @Test
    void providesStandardEditorCookiesWithoutUnsafeDartOnlySaveAs() {
        Class<FlutterDesignerEditorSupport> type = FlutterDesignerEditorSupport.class;

        assertTrue(DataEditorSupport.class.isAssignableFrom(type));
        assertTrue(OpenCookie.class.isAssignableFrom(type));
        assertTrue(EditCookie.class.isAssignableFrom(type));
        assertTrue(EditorCookie.Observable.class.isAssignableFrom(type));
        assertTrue(PrintCookie.class.isAssignableFrom(type));
        assertTrue(CloseCookie.class.isAssignableFrom(type));
        assertFalse(SaveAsCapable.class.isAssignableFrom(type),
                "Dart-only Save As would orphan the paired .fd model");
    }

    @Test
    void ownsAGuardedEditorBridgeForTheDocumentBeingLoaded() {
        assertTrue(Arrays.stream(FlutterDesignerEditorSupport.class.getDeclaredClasses())
                .anyMatch(GuardedEditorSupport.class::isAssignableFrom));
    }

    @Test
    void newDesignerPaneStartsWithVisibleModelTitleAndAnnotatesPairedDirtyState()
            throws Exception {
        EditorFixture fixture = createEditorFixture("pane_title");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerDataObject dataObject = (FlutterDesignerDataObject)
                editor.getDataObject();

        CloneableEditorSupport.Pane pane = onEdt(editor::createPane);
        CloneableTopComponent component = pane.getComponent();

        assertTrue(dataObject.isValid(),
                "a non-empty title must not mask an invalid DataObject");
        assertFalse(dataObject.getNodeDelegate().getDisplayName().isBlank(),
                "the regression is not an empty or invalid DataObject node");
        assertEquals("home_page.fd", component.getDisplayName(),
                "the clean Design-first pane must be titled before Source is created");
        assertTrue(component.getHtmlDisplayName().contains("home_page.fd"));
        assertTrue(component.getToolTipText().contains(
                FileUtil.getFileDisplayName(dataObject.getModelFile())),
                "the tooltip must identify the visible .fd model");
        assertTrue(component.getToolTipText().contains(
                FileUtil.getFileDisplayName(dataObject.getPrimaryFile())),
                "the tooltip must identify the paired Dart source");

        onEdt(() -> {
            fixture.document().insertString(
                    fixture.document().getLength(),
                    "// title dirty-state edit\n",
                    null);
            return null;
        });

        assertNotNull(dataObject.getCookie(SaveCookie.class),
                "a paired dirty form must publish its stable SaveCookie");
        assertTrue(editor.messageHtmlName().contains("<b>home_page.fd</b>"),
                "the title annotation must follow the paired DataObject dirty state");
        assertTrue(editor.messageToolTip().contains("unsaved changes"),
                "the tooltip must explain why the paired form is dirty");
    }

    @Test
    void dedicatedShellIsCloneableButCannotEnterNetBeansSplitPaths()
            throws Exception {
        EditorFixture fixture = createEditorFixture("dedicated_shell");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerDataObject dataObject = (FlutterDesignerDataObject)
                editor.getDataObject();

        CloneableEditorSupport.Pane pane = onEdt(() -> {
            CloneableEditorSupport.Pane created = editor.createDedicatedPane();
            assertFalse(editor.editorClosePermits().cloneCreationAllowed(),
                    "the reservation must outlive createPane until NetBeans can register Ref");
            return created;
        });
        onEdt(() -> {
            assertTrue(editor.editorClosePermits().cloneCreationAllowed(),
                    "the next EDT turn follows the caller's Ref registration point");
            return null;
        });
        CloneableTopComponent component = pane.getComponent();
        Class<?> splitable = Class.forName("org.netbeans.core.multiview.Splitable");

        assertTrue(component instanceof FlutterDesignerCloneableEditor);
        assertFalse(splitable.isInstance(component),
                "the plugin-owned pane must not expose Split/Clear Split reparenting");
        assertEquals(CloneableTopComponent.PERSISTENCE_NEVER,
                component.getPersistenceType(),
                "restart reconstruction remains disabled until its runtime gate is proven");
        assertEquals(Boolean.TRUE,
                component.getClientProperty(CloneableTopComponent.PROP_DRAGGING_DISABLED));
        assertEquals(Boolean.TRUE,
                component.getClientProperty(CloneableTopComponent.PROP_UNDOCKING_DISABLED));
        assertEquals(Boolean.TRUE,
                component.getClientProperty(CloneableTopComponent.PROP_SLIDING_DISABLED));
        assertEquals(Boolean.TRUE,
                component.getClientProperty(CloneableTopComponent.PROP_MAXIMIZATION_DISABLED));
        assertEquals(Boolean.TRUE,
                component.getClientProperty(CloneableTopComponent.PROP_DND_COPY_DISABLED));
        assertSame(dataObject,
                component.getLookup().lookup(FlutterDesignerDataObject.class));
        assertSame(component.getActionMap(),
                component.getLookup().lookup(ActionMap.class),
                "the dedicated Source shell must retain global editor actions");
        assertEquals("home_page.fd", component.getDisplayName());
        assertTrue(component.getToolTipText().contains(
                FileUtil.getFileDisplayName(dataObject.getPrimaryFile())));
        assertFalse(FlutterDesignerEditorShellRoute.PRODUCTION_ENABLED,
                "phase one must not silently replace the proven MultiView route");

        CloneableTopComponent clone = onEdt(component::cloneTopComponent);
        assertTrue(clone instanceof FlutterDesignerCloneableEditor);
        assertSame(component.getReference(), clone.getReference(),
                "dedicated shells must still share the CES clone group");
        assertSame(clone, clone.getClientProperty("CloneableEditorSupport.Pane"),
                "CES must install its private Pane identity on every clone");
        assertSame(dataObject,
                clone.getLookup().lookup(FlutterDesignerDataObject.class));
        assertFalse(splitable.isInstance(clone));
    }

    @Test
    void dedicatedShellBlocksReentrantCloseCookieDuringOpenedPanesEvent()
            throws Exception {
        EditorFixture fixture = createEditorFixture("opening_close_gate");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerCloneableEditor component =
                (FlutterDesignerCloneableEditor) onEdt(
                        () -> editor.createDedicatedPane().getComponent());
        // The next EDT turn is the first point after the construction caller
        // could have registered the component in its clone Ref.
        onEdt(() -> null);
        AtomicBoolean openedPanesObserved = new AtomicBoolean();
        AtomicReference<Boolean> reentrantClose = new AtomicReference<>();
        java.beans.PropertyChangeListener listener = event -> {
            if (EditorCookie.Observable.PROP_OPENED_PANES.equals(
                    event.getPropertyName())) {
                openedPanesObserved.set(true);
                reentrantClose.set(editor.close());
            }
        };
        editor.addPropertyChangeListener(listener);
        try {
            onEdt(() -> {
                component.open();
                return null;
            });
            assertTrue(openedPanesObserved.get());
            assertEquals(Boolean.FALSE, reentrantClose.get(),
                    "the dedicated gate must exist before super.componentOpened fires listeners");
            assertTrue(component.isOpened());
        } finally {
            editor.removePropertyChangeListener(listener);
            onEdt(() -> {
                if (component.isOpened()) {
                    component.close();
                }
                return null;
            });
        }
    }

    @Test
    void directCloseDuringOpenedPanesCannotTearDownOpeningShell()
            throws Exception {
        EditorFixture fixture = createEditorFixture("opening_direct_close_gate");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerCloneableEditor component =
                (FlutterDesignerCloneableEditor) onEdt(
                        () -> editor.createDedicatedPane().getComponent());
        onEdt(() -> null);
        AtomicBoolean openedPanesObserved = new AtomicBoolean();
        AtomicReference<Boolean> directClose = new AtomicReference<>();
        java.beans.PropertyChangeListener listener = event -> {
            if (EditorCookie.Observable.PROP_OPENED_PANES.equals(
                    event.getPropertyName())) {
                openedPanesObserved.set(true);
                directClose.set(component.close());
            }
        };
        editor.addPropertyChangeListener(listener);
        try {
            onEdt(() -> {
                component.open();
                return null;
            });
            assertTrue(openedPanesObserved.get());
            assertEquals(Boolean.TRUE, directClose.get(),
                    "TopComponent reports this pre-registry close as an already-closed no-op");
            assertTrue(component.isOpened());
        } finally {
            editor.removePropertyChangeListener(listener);
            onEdt(() -> {
                if (component.isOpened()) {
                    component.close();
                }
                return null;
            });
        }
    }

    @Test
    void throwingOpenedPanesListenerCannotRemoveDedicatedCloseGate()
            throws Exception {
        EditorFixture fixture = createEditorFixture("opening_listener_failure");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerCloneableEditor component =
                (FlutterDesignerCloneableEditor) onEdt(
                        () -> editor.createDedicatedPane().getComponent());
        onEdt(() -> null);
        AtomicBoolean openedPanesObserved = new AtomicBoolean();
        RuntimeException listenerFailure = new RuntimeException(
                "synthetic opened-panes listener failure");
        java.beans.PropertyChangeListener listener = event -> {
            if (EditorCookie.Observable.PROP_OPENED_PANES.equals(
                    event.getPropertyName())) {
                openedPanesObserved.set(true);
                throw listenerFailure;
            }
        };
        editor.addPropertyChangeListener(listener);
        try {
            onEdt(() -> {
                component.open();
                return null;
            });
            assertTrue(openedPanesObserved.get());
            assertTrue(component.isOpened(),
                    "WindowManager retains the component after a listener failure");
            assertFalse(editor.close(),
                    "a listener failure must leave the support CloseCookie fail-closed");
        } finally {
            editor.removePropertyChangeListener(listener);
            onEdt(() -> {
                if (component.isOpened()) {
                    component.close();
                }
                return null;
            });
        }
    }

    @Test
    void offEdtCloseQueuedBehindShellOpenRechecksGateOnEdt()
            throws Exception {
        EditorFixture fixture = createEditorFixture("queued_open_close_gate");
        FlutterDesignerEditorSupport editor = fixture.editor();
        FlutterDesignerCloneableEditor component =
                (FlutterDesignerCloneableEditor) onEdt(
                        () -> editor.createDedicatedPane().getComponent());
        onEdt(() -> null);
        CountDownLatch closeStarted = new CountDownLatch(1);
        AtomicReference<Boolean> closeResult = new AtomicReference<>();
        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        Thread closeThread = new Thread(() -> {
            closeStarted.countDown();
            try {
                closeResult.set(editor.close());
            } catch (Throwable failure) {
                closeFailure.set(failure);
            }
        }, "off-edt-close-behind-dedicated-open");
        closeThread.setDaemon(true);

        try {
            onEdt(() -> {
                EventQueue.invokeLater(component::open);
                closeThread.start();
                assertTrue(closeStarted.await(2, TimeUnit.SECONDS));
                return null;
            });
            closeThread.join(TimeUnit.SECONDS.toMillis(2));
            assertFalse(closeThread.isAlive());
            assertNull(closeFailure.get());
            assertEquals(Boolean.FALSE, closeResult.get(),
                    "the close must recheck after the earlier shell-open event");
            assertTrue(onEdt(() -> Boolean.valueOf(
                    component.isOpened())).booleanValue());
        } finally {
            onEdt(() -> {
                if (component.isOpened()) {
                    component.close();
                }
                return null;
            });
        }
    }

    @Test
    void replacementRestoreFinalizerReceivesExactSnapshotUnderEditorDocumentLock()
            throws Exception {
        EditorFixture fixture = createEditorFixture("restore_lock");
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot before = editor.liveSnapshot();
        GeneratedDartRegions generated = generatedExampleRegions();
        byte[] candidate = candidate(generated);
        LiveDartDocumentSnapshot applied = onEdt(() ->
                editor.applyPreparedRegions(before, generated, candidate));
        CountDownLatch editStarted = new CountDownLatch(1);
        CountDownLatch editFinished = new CountDownLatch(1);
        AtomicReference<Throwable> editFailure = new AtomicReference<>();
        AtomicReference<Thread> editThread = new AtomicReference<>();
        AtomicReference<LiveDartDocumentSnapshot> finalized =
                new AtomicReference<>();
        String userEdit = "// queued after editor replacement rollback\n";

        LiveDartDocumentSnapshot restored = onEdt(() ->
                editor.restoreAndFinalizeReplacement(
                        applied,
                        before,
                        exactRestored -> {
                            finalized.set(exactRestored);
                            assertArrayEquals(before.markerBearingUtf8(),
                                    exactRestored.markerBearingUtf8());
                            Thread worker = new Thread(() -> {
                                editStarted.countDown();
                                try {
                                    fixture.document().insertString(
                                            fixture.document().getLength(),
                                            userEdit,
                                            null);
                                } catch (Throwable failure) {
                                    editFailure.set(failure);
                                } finally {
                                    editFinished.countDown();
                                }
                            }, "queued-edit-after-editor-replacement-rollback");
                            worker.setDaemon(true);
                            editThread.set(worker);
                            worker.start();
                            try {
                                assertTrue(editStarted.await(2, TimeUnit.SECONDS));
                                assertFalse(editFinished.await(
                                        100, TimeUnit.MILLISECONDS),
                                        "the editor finalizer must retain the document lock");
                            } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(interrupted);
                            }
                        }));

        assertSame(finalized.get(), restored);
        Thread worker = editThread.get();
        assertNotNull(worker);
        worker.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(worker.isAlive());
        assertNull(editFailure.get());
        assertArrayEquals(
                (SOURCE + userEdit).getBytes(StandardCharsets.UTF_8),
                editor.liveSnapshot().markerBearingUtf8());
    }

    @Test
    void failedEditorApplyFinalizerRollsBackAndPublishesNoCandidateIdentity()
            throws Exception {
        EditorFixture fixture = createEditorFixture("apply_failure");
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot predecessor = editor.liveSnapshot();
        boolean nativeUndoBefore = editor
                .nativeUndoRedoManagerForCombinedBridge().canUndo();
        GeneratedDartRegions generated = generatedExampleRegions();
        AtomicReference<LiveDartDocumentSnapshot> published =
                new AtomicReference<>();

        IOException failure = assertThrows(IOException.class, () ->
                published.set(onEdt(() ->
                        editor.applyPreparedRegionsAndFinalize(
                                predecessor,
                                generated,
                                candidate(generated),
                                exactApplied -> {
                                    throw new IOException(
                                            "synthetic editor C2 binding failure");
                                }))));

        assertTrue(failure.getMessage().contains("editor C2 binding failure"));
        assertNull(published.get());
        assertArrayEquals(predecessor.markerBearingUtf8(),
                editor.liveSnapshot().markerBearingUtf8());
        assertTrue(editor.sourceModified(),
                "an observed apply/rollback stays dirty until explicit recovery");
        assertEquals(nativeUndoBefore,
                editor.nativeUndoRedoManagerForCombinedBridge().canUndo(),
                "atomic rollback must admit no native history entry");
    }

    @Test
    void semanticForwardAdmissionCommitsAfterNativeAckAndPublishesAfterUnlock()
            throws Exception {
        EditorFixture fixture = createEditorFixture(
                "semantic_forward_admission", true);
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot predecessor = editor.liveSnapshot();
        GeneratedDartRegions generated = generatedExampleRegions();
        AtomicInteger semanticRevision = new AtomicInteger(40);
        AtomicInteger jointCommits = new AtomicInteger();
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        AtomicReference<Thread> readerThread = new AtomicReference<>();
        CountDownLatch readerStarted = new CountDownLatch(1);
        CountDownLatch readerFinished = new CountDownLatch(1);
        List<String> callbacks = new ArrayList<>();
        DesignerCombinedUndoRedo combined =
                ((FlutterDesignerDataObject) fixture.editor().getDataObject())
                        .getCombinedUndoRedo();

        LiveDartDocumentSnapshot applied = onEdt(() -> {
            javax.swing.event.ChangeListener combinedListener =
                    event -> callbacks.add("combined");
            combined.addChangeListener(combinedListener);
            try {
                return editor.applyPreparedRegionsAndFinalize(
                        predecessor,
                        generated,
                        candidate(generated),
                        new FlutterDesignerEditorSupport.ForwardSemanticEdge(
                                40,
                                41,
                                "Apply Flutter visual change",
                                (direction, currentRevisionId,
                                        targetRevisionId) -> {
                                    throw new AssertionError(
                                            "forward-admission fixture must not replay");
                                }),
                        exactApplied -> {
                            assertArrayEquals(
                                    candidate(generated),
                                    exactApplied.markerBearingUtf8());
                            Thread reader = new Thread(() -> {
                                readerStarted.countDown();
                                try {
                                    fixture.document().render(() -> {
                                        try {
                                            fixture.document().getText(
                                                    0,
                                                    fixture.document().getLength());
                                        } catch (BadLocationException failure) {
                                            throw new IllegalStateException(failure);
                                        }
                                    });
                                } catch (Throwable failure) {
                                    readerFailure.set(failure);
                                } finally {
                                    readerFinished.countDown();
                                }
                            }, "semantic-forward-admission-reader");
                            reader.setDaemon(true);
                            readerThread.set(reader);
                            reader.start();
                            assertTrue(await(readerStarted, 2, TimeUnit.SECONDS));
                            assertFalse(await(
                                    readerFinished, 100, TimeUnit.MILLISECONDS),
                                    "the evidence finalizer must retain the outer write lock");

                            return () -> {
                                assertEquals(40, semanticRevision.get());
                                assertFalse(await(
                                        readerFinished,
                                        100,
                                        TimeUnit.MILLISECONDS),
                                        "native acknowledgement must commit before outer unlock");
                                semanticRevision.set(41);
                                jointCommits.incrementAndGet();
                                return () -> {
                                    assertTrue(await(
                                            readerFinished,
                                            2,
                                            TimeUnit.SECONDS),
                                            "semantic publication must run after outer unlock");
                                    assertNull(readerFailure.get());
                                    callbacks.add("semantic");
                                };
                            };
                        });
            } finally {
                combined.removeChangeListener(combinedListener);
            }
        });

        Thread reader = readerThread.get();
        assertNotNull(reader);
        reader.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(reader.isAlive());
        assertNull(readerFailure.get());
        assertArrayEquals(candidate(generated), applied.markerBearingUtf8());
        assertEquals(41, semanticRevision.get());
        assertEquals(1, jointCommits.get());
        assertEquals(List.of("semantic", "combined"), callbacks,
                "semantic state must publish before one native presentation edge");
        assertTrue(editor.nativeUndoRedoManagerForCombinedBridge().canUndo());
        assertEquals("Undo Apply Flutter visual change",
                editor.nativeUndoRedoManagerForCombinedBridge()
                        .getUndoPresentationName());
    }

    @Test
    void committedPairVerificationRetainsSemanticNativeEdgeAndExplicitSourceS2()
            throws Exception {
        EditorFixture fixture = createEditorFixture(
                "committed_pair_history_verification", true);
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot predecessor = editor.liveSnapshot();
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                editor.applyPreparedRegionsAndFinalize(
                        predecessor,
                        generated,
                        candidate(generated),
                        semanticEdge(60, 61, "Saved Flutter visual change"),
                        exactApplied -> () -> () -> { }));
        String undoName = editor.nativeUndoRedoManagerForCombinedBridge()
                .getUndoPresentationName();
        assertTrue(editor.nativeUndoRedoManagerForCombinedBridge().canUndo());

        LiveDartDocumentSnapshot exact = onEdt(() ->
                editor.verifyCommittedPairWithoutHistoryMutation(
                        applied,
                        LiveDartDocumentBridge.CommittedPairContentPolicy
                                .EXACT_FULL_CONTENT));

        assertTrue(applied.sameEvidence(exact));
        assertTrue(editor.nativeUndoRedoManagerForCombinedBridge().canUndo());
        assertEquals(undoName, editor.nativeUndoRedoManagerForCombinedBridge()
                .getUndoPresentationName(),
                "commit verification must not replace or clear the semantic edge");

        String sourceS2 = "// unmanaged Source S2 after serialized M1\n";
        onEdt(() -> {
            fixture.document().insertString(
                    fixture.document().getLength(), sourceS2, null);
            return null;
        });
        LiveDartDocumentSnapshot retained = onEdt(() ->
                editor.verifyCommittedPairWithoutHistoryMutation(
                        applied,
                        LiveDartDocumentBridge.CommittedPairContentPolicy
                                .EXACT_MANAGED_CONTENT));
        IOException exactMismatch = assertThrows(IOException.class, () ->
                onEdt(() -> editor.verifyCommittedPairWithoutHistoryMutation(
                        applied,
                        LiveDartDocumentBridge.CommittedPairContentPolicy
                                .EXACT_FULL_CONTENT)));

        assertTrue(applied.sameManagedContent(retained));
        assertTrue(new String(retained.markerBearingUtf8(), StandardCharsets.UTF_8)
                .endsWith(sourceS2));
        assertTrue(exactMismatch.getMessage().contains(
                "complete live Dart source differs"));
        assertTrue(editor.nativeUndoRedoManagerForCombinedBridge().canUndo());
    }

    @Test
    void committedPairVerificationRejectsAnotherEditorDocumentIdentity()
            throws Exception {
        EditorFixture owner = createEditorFixture("committed_pair_owner");
        EditorFixture foreign = createEditorFixture("committed_pair_foreign");
        LiveDartDocumentSnapshot foreignSnapshot = foreign.editor().liveSnapshot();

        IOException mismatch = assertThrows(IOException.class, () -> onEdt(() ->
                owner.editor().verifyCommittedPairWithoutHistoryMutation(
                        foreignSnapshot,
                        LiveDartDocumentBridge.CommittedPairContentPolicy
                                .EXACT_MANAGED_CONTENT)));

        assertTrue(mismatch.getMessage().contains("identity changed"));
    }

    @Test
    void rejectedSemanticForwardFinalizerRollsBackAndReleasesCaptureToken()
            throws Exception {
        EditorFixture fixture = createEditorFixture(
                "semantic_forward_rejection", true);
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot predecessor = editor.liveSnapshot();
        GeneratedDartRegions generated = generatedExampleRegions();
        boolean nativeUndoBefore = editor
                .nativeUndoRedoManagerForCombinedBridge().canUndo();
        boolean nativeRedoBefore = editor
                .nativeUndoRedoManagerForCombinedBridge().canRedo();

        IOException failure = assertThrows(IOException.class, () -> onEdt(() ->
                editor.applyPreparedRegionsAndFinalize(
                        predecessor,
                        generated,
                        candidate(generated),
                        semanticEdge(50, 51, "Rejected Flutter visual change"),
                        applied -> {
                            throw new IOException(
                                    "synthetic semantic evidence rejection");
                        })));

        assertTrue(failure.getMessage().contains(
                "synthetic semantic evidence rejection"));
        assertArrayEquals(predecessor.markerBearingUtf8(),
                editor.liveSnapshot().markerBearingUtf8());
        assertTrue(editor.sourceModified(),
                "an observed apply/atomicUndo remains fail-closed dirty");
        assertEquals(nativeUndoBefore,
                editor.nativeUndoRedoManagerForCombinedBridge().canUndo());
        assertEquals(nativeRedoBefore,
                editor.nativeUndoRedoManagerForCombinedBridge().canRedo());

        LiveDartDocumentSnapshot exactRetryPredecessor = editor.liveSnapshot();
        AtomicBoolean retriedCommit = new AtomicBoolean();
        LiveDartDocumentSnapshot retried = onEdt(() ->
                editor.applyPreparedRegionsAndFinalize(
                        exactRetryPredecessor,
                        generated,
                        candidate(generated),
                        semanticEdge(50, 51, "Retried Flutter visual change"),
                        applied -> () -> {
                            retriedCommit.set(true);
                            return () -> { };
                        }));

        assertTrue(retriedCommit.get(),
                "a rejected apply must release both capture token halves");
        assertArrayEquals(candidate(generated), retried.markerBearingUtf8());
        assertEquals("Undo Retried Flutter visual change",
                editor.nativeUndoRedoManagerForCombinedBridge()
                        .getUndoPresentationName());
    }

    @Test
    void failedEditorRestoreFinalizerKeepsPredecessorSemanticStateWhole()
            throws Exception {
        EditorFixture fixture = createEditorFixture("restore_failure");
        FlutterDesignerEditorSupport editor = fixture.editor();
        LiveDartDocumentSnapshot predecessor = editor.liveSnapshot();
        GeneratedDartRegions generated = generatedExampleRegions();
        LiveDartDocumentSnapshot applied = onEdt(() ->
                editor.applyPreparedRegions(
                        predecessor, generated, candidate(generated)));
        AtomicReference<LiveDartDocumentSnapshot> semanticPredecessor =
                new AtomicReference<>(predecessor);
        AtomicReference<LiveDartDocumentSnapshot> published =
                new AtomicReference<>();

        IOException failure = assertThrows(IOException.class, () ->
                published.set(onEdt(() ->
                        editor.restoreAndFinalizeReplacement(
                                applied,
                                predecessor,
                                exactRestored -> {
                                    assertArrayEquals(
                                            semanticPredecessor.get()
                                                    .markerBearingUtf8(),
                                            exactRestored.markerBearingUtf8());
                                    throw new IOException(
                                            "synthetic predecessor evidence failure");
                                }))));

        assertTrue(failure.getMessage().contains(
                "predecessor evidence failure"));
        assertNull(published.get());
        assertSame(predecessor, semanticPredecessor.get(),
                "the failed finalizer must not publish a partial semantic revision");
        assertArrayEquals(predecessor.markerBearingUtf8(),
                editor.liveSnapshot().markerBearingUtf8());
    }

    @Test
    void nativeReplayMutationUsesCesDirtyStateWithoutExternalSourceClassification()
            throws Exception {
        EditorFixture fixture = createEditorFixture("native_replay_notification");
        FlutterDesignerEditorSupport editor = fixture.editor();
        PairSaveCoordinator coordinator = fixture.coordinator();
        PairSaveCoordinatorSnapshot before = coordinator.state();
        AtomicReference<LiveDartDocumentSnapshot> finalized =
                new AtomicReference<>();
        String replayedText = "// replayed semantic source\n";

        LiveDartDocumentSnapshot verified = onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                fixture.document().insertString(
                        fixture.document().getLength(), replayedText, null);
                LiveDartDocumentSnapshot target = editor.liveSnapshot();
                return editor.verifyNativeReplayAndFinalize(
                        fixture.document(),
                        target.markerBearingUtf8(),
                        finalized::set);
            }
        });

        assertSame(verified, finalized.get());
        assertSame(fixture.document(), verified.documentIdentity());
        assertTrue(editor.sourceModified(),
                "CES must still own the modified transition for the raw edit");
        assertEquals(before, coordinator.state(),
                "the raw half of a semantic replay is not an external Source edge");
    }

    @Test
    void nativeReplayVerificationRequiresExactDocumentIdentityAndBytes()
            throws Exception {
        EditorFixture fixture = createEditorFixture("native_replay_exactness");
        FlutterDesignerEditorSupport editor = fixture.editor();
        byte[] exact = editor.liveSnapshot().markerBearingUtf8();
        AtomicBoolean finalized = new AtomicBoolean();

        IOException identityFailure = onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                IOException failure = assertThrows(IOException.class, () ->
                        editor.verifyNativeReplayAndFinalize(
                                new DefaultStyledDocument(),
                                exact,
                                snapshot -> finalized.set(true)));
                assertThrows(IOException.class, () ->
                        editor.beginNativeHistoryReplay(fixture.document()),
                        "failed verification must retain the owning replay token");
                return failure;
            }
        });
        assertTrue(identityFailure.getMessage().contains("identity changed"));
        assertFalse(finalized.get());

        IOException mismatch = onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                IOException failure = assertThrows(IOException.class, () ->
                        editor.verifyNativeReplayAndFinalize(
                                fixture.document(),
                                (SOURCE + "// not present\n")
                                        .getBytes(StandardCharsets.UTF_8),
                                snapshot -> finalized.set(true)));
                assertThrows(IOException.class, () ->
                        editor.beginNativeHistoryReplay(fixture.document()),
                        "byte mismatch must not close the replay before raw recovery");
                return failure;
            }
        });
        assertTrue(mismatch.getMessage().contains("exact expected Dart bytes"));
        assertFalse(finalized.get());

        LiveDartDocumentSnapshot verified = onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                return editor.verifyNativeReplayAndFinalize(
                        fixture.document(), exact, snapshot -> { });
            }
        });
        assertArrayEquals(exact, verified.markerBearingUtf8(),
                "a failed exactness check must close its replay context");

        onEdt(() -> {
            fixture.document().insertString(
                    fixture.document().getLength(),
                    "// ordinary source after successful replay context\n",
                    null);
            return null;
        });
        assertEquals(PairSaveCoordinatorStatus.DIRTY_SOURCE,
                fixture.coordinator().state().status(),
                "ordinary Source notifications must resume after replay cleanup");
    }

    @Test
    void nativeReplayRejectsDocumentMutationByFinalizerAndClosesContext()
            throws Exception {
        EditorFixture fixture = createEditorFixture("native_replay_mutating_finalizer");
        FlutterDesignerEditorSupport editor = fixture.editor();
        byte[] exact = editor.liveSnapshot().markerBearingUtf8();

        IOException failure = assertThrows(IOException.class, () -> onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                return editor.verifyNativeReplayAndFinalize(
                        fixture.document(), exact, snapshot -> {
                            try {
                                fixture.document().insertString(
                                        fixture.document().getLength(),
                                        "// forbidden finalizer mutation\n",
                                        null);
                            } catch (BadLocationException ex) {
                                throw new IOException(ex);
                            }
                        });
            }
        }));
        assertTrue(failure.getMessage().contains("finalizer mutated"));

        LiveDartDocumentSnapshot current = editor.liveSnapshot();
        onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                return editor.verifyNativeReplayAndFinalize(
                        fixture.document(),
                        current.markerBearingUtf8(),
                        snapshot -> { });
            }
        });
    }

    @Test
    void throwingNativeReplayFinalizerCannotLeakInternalNotificationContext()
            throws Exception {
        EditorFixture fixture = createEditorFixture("native_replay_throwing_finalizer");
        FlutterDesignerEditorSupport editor = fixture.editor();
        PairSaveCoordinator coordinator = fixture.coordinator();
        byte[] exact = editor.liveSnapshot().markerBearingUtf8();
        PairSaveCoordinatorSnapshot before = coordinator.state();
        String replayed = "// raw replay requiring recovery\n";

        IOException failure = onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                int predecessorLength = fixture.document().getLength();
                fixture.document().insertString(
                        predecessorLength, replayed, null);
                byte[] target = editor.liveSnapshot().markerBearingUtf8();
                IOException expected = assertThrows(IOException.class, () ->
                        editor.verifyNativeReplayAndFinalize(
                                fixture.document(), target, snapshot -> {
                                    throw new IOException(
                                            "synthetic replay finalizer failure");
                                }));
                assertThrows(IOException.class, () ->
                        editor.beginNativeHistoryReplay(fixture.document()),
                        "a throwing finalizer must retain context through inverse replay");
                // DesignerSemanticUndoableEdit performs this exact raw
                // recovery before PreparedReplay.abort closes the token.
                fixture.document().remove(
                        predecessorLength, replayed.length());
                return expected;
            }
        });
        assertTrue(failure.getMessage().contains(
                "synthetic replay finalizer failure"));
        assertArrayEquals(exact, editor.liveSnapshot().markerBearingUtf8());
        assertEquals(before, coordinator.state(),
                "both the failed raw move and its recovery remain internal");

        onEdt(() -> {
            try (FlutterDesignerEditorSupport.NativeHistoryReplay ignored =
                    editor.beginNativeHistoryReplay(fixture.document())) {
                return editor.verifyNativeReplayAndFinalize(
                        fixture.document(), exact, snapshot -> { });
            }
        });
    }

    private EditorFixture createEditorFixture(String name) throws Exception {
        return createEditorFixture(name, false);
    }

    private EditorFixture createEditorFixture(
            String name,
            boolean installForwardCaptureWrapper) throws Exception {
        Path folder = Files.createDirectory(temporaryDirectory.resolve(name));
        Files.createDirectories(folder.resolve("lib"));
        Files.createDirectories(folder.resolve(".fd_templates"));
        Files.writeString(folder.resolve("pubspec.yaml"), """
                name: editor_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(folder);
        Path dartPath = folder.resolve("lib/home_page.dart");
        Path fdPath = folder.resolve(".fd_templates/home_page.fd");
        Files.writeString(dartPath, SOURCE, StandardCharsets.UTF_8);
        Files.write(fdPath, fdBytes());
        FileUtil.refreshFor(folder.toFile());
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dart);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                .dataObject(dart, project);
        FlutterDesignerEditorSupport editor = dataObject.getEditorSupport();
        StyledDocument document = openGuardedSourceDocument(
                editor, installForwardCaptureWrapper);
        EditorFixture fixture = new EditorFixture(
                editor,
                document,
                dataObject.getPairSaveCoordinator());
        openFixtures.add(fixture);
        return fixture;
    }

    private static StyledDocument openGuardedSourceDocument(
            FlutterDesignerEditorSupport editor) throws Exception {
        return openGuardedSourceDocument(editor, false);
    }

    private static StyledDocument openGuardedSourceDocument(
            FlutterDesignerEditorSupport editor,
            boolean installForwardCaptureWrapper) throws Exception {
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
        if (installForwardCaptureWrapper) {
            Field wrappers = BaseDocument.class.getDeclaredField(
                    "undoEditWrappers");
            wrappers.setAccessible(true);
            wrappers.set(document, List.of(new DesignerUndoableEditWrapper()));
        }
        return document;
    }

    private static GeneratedDartRegions generatedExampleRegions() throws Exception {
        String modelJson = """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "home_page.dart",
                    "className": "HomePage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {"sha256": "%s"},
                      "build": {"sha256": "%s"}
                    }
                  },
                  "root": {
                    "id": "35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce",
                    "type": "example.widgets.SampleWidget",
                    "properties": {},
                    "slots": {}
                  }
                }
                """.formatted("0".repeat(64), "0".repeat(64));
        FdDecodeResult.Current decoded = (FdDecodeResult.Current)
                new FdDocumentCodec().decode(
                        modelJson.getBytes(StandardCharsets.UTF_8));
        WidgetDefinition definition = new WidgetDefinition(
                new WidgetTypeId("example.widgets.SampleWidget"),
                "SampleWidget",
                Optional.empty(),
                true,
                "package:example/widgets.dart",
                List.of("package:example/widgets.dart"),
                Set.of(),
                new PaletteMetadata("example", 10, 10, "Sample Widget"),
                List.of(),
                List.of());
        return new DartRegionGenerator()
                .generate(decoded.document(), WidgetCatalog.strict(List.of(definition)))
                .generated()
                .orElseThrow();
    }

    private static byte[] candidate(GeneratedDartRegions generated) {
        return SOURCE
                .replace(IMPORTS_PAYLOAD, generated.imports().payload())
                .replace(BUILD_PAYLOAD, generated.build().payload())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static FlutterDesignerEditorSupport.ForwardSemanticEdge semanticEdge(
            long beforeRevisionId,
            long afterRevisionId,
            String presentationName) {
        return new FlutterDesignerEditorSupport.ForwardSemanticEdge(
                beforeRevisionId,
                afterRevisionId,
                presentationName,
                (direction, currentRevisionId, targetRevisionId) -> {
                    throw new AssertionError(
                            "forward-admission fixture must not replay");
                });
    }

    private static boolean await(
            CountDownLatch latch,
            long timeout,
            TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static byte[] fdBytes() throws Exception {
        DartSourceDescriptor descriptor = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("test-profile"),
                new ManagedRegions(
                        new ManagedRegion(DartManagedRegionHashing
                                .normalizedSha256(IMPORTS_PAYLOAD)),
                        new ManagedRegion(DartManagedRegionHashing
                                .normalizedSha256(BUILD_PAYLOAD))));
        WidgetNode root = new WidgetNode(
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(
                        new PropertyName("data"),
                        new PropertyValue.StringValue("fixture")),
                Map.of());
        DesignerDocument document = new DesignerDocument(
                StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                descriptor,
                root);
        return new FdDocumentCodec().encode(document).copyBytes();
    }

    private static <T> T onEdt(Callable<T> operation) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return operation.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                result.set(operation.call());
            } catch (Throwable caught) {
                failure.set(caught);
            }
        });
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        return result.get();
    }

    private record EditorFixture(
            FlutterDesignerEditorSupport editor,
            StyledDocument document,
            PairSaveCoordinator coordinator) {
    }
}

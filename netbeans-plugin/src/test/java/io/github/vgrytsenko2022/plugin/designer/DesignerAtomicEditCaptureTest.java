package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.document.AtomicLockDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.editor.BaseDocument;
import org.openide.awt.UndoRedo;

class DesignerAtomicEditCaptureTest {
    @Test
    void productionLayerRegistersTheDartMimeUndoWrapper() throws Exception {
        try (InputStream stream = DesignerAtomicEditCaptureTest.class
                .getClassLoader()
                .getResourceAsStream("META-INF/generated-layer.xml")) {
            if (stream == null) {
                throw new AssertionError(
                        "The compiled NetBeans generated layer is unavailable");
            }
            String layer = new String(
                    stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(layer.contains(
                    "DesignerUndoableEditWrapper.instance"));
            assertTrue(layer.contains(
                    "org.netbeans.spi.editor.document.UndoableEditWrapper"));
        }
    }

    @Test
    void unchangedSourceAdmissionIsOneDeferredSemanticEditWithoutDocumentEvents()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(nativeHistory);
            AtomicInteger documentEvents = new AtomicInteger();
            document.addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { documentEvents.incrementAndGet(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { documentEvents.incrementAndGet(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { documentEvents.incrementAndGet(); }
            });
            AtomicInteger modelRevision = new AtomicInteger();
            AtomicInteger published = new AtomicInteger();
            try (var barrier = combined.deferNotifications();
                    var capture = DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                document.runAtomic(() -> {
                    capture.seal(0, 1, "Metadata property", replay(modelRevision, published),
                            combined::enqueueSemanticPublication, () -> {
                                modelRevision.set(1);
                                return published::incrementAndGet;
                            });
                    capture.admitUnchangedSource();
                    assertEquals(1, modelRevision.get());
                    assertEquals(0, published.get());
                });
                capture.verifyCompleted();
                capture.verifyCompleted();
                assertEquals(0, published.get(), "publication waits for the outward notification barrier");
                assertTrue(capture.committed());
                assertTrue(nativeHistory.canUndo());
                assertEquals(0, documentEvents.get());
                assertEquals("B", text(document));
                assertThrows(IllegalStateException.class, capture::admitUnchangedSource);
            }
            assertEquals(1, published.get());
            combined.undo();
            assertEquals(0, modelRevision.get());
            assertEquals(2, published.get());
            assertFalse(nativeHistory.canUndo(), "only one semantic entry was admitted");
            assertTrue(nativeHistory.canRedo());
            combined.redo();
            assertEquals(1, modelRevision.get());
            assertEquals(3, published.get());
            assertEquals("B", text(document));
            assertEquals(0, documentEvents.get(), "forward, Undo and Redo never fabricate Source edits");
            assertOnlyNativeListener(document, nativeHistory);
        });
    }

    @Test
    void nativeListenerIdentitySurvivesSuccessAbortAndClose()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            assertOnlyNativeListener(document, nativeHistory);

            appendCommittedDesignerEdit(document, nativeHistory, "M1");
            assertOnlyNativeListener(document, nativeHistory);

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                capture.abort();
            }
            assertOnlyNativeListener(document, nativeHistory);
        });
    }

    @Test
    void foreignDocumentUndoListenerRejectsBeginBeforeMutation()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            UndoableEditListener foreign = event -> { };
            document.addUndoableEditListener(nativeHistory);
            document.addUndoableEditListener(foreign);

            assertThrows(IOException.class,
                    () -> DesignerAtomicEditCapture.begin(
                            document, nativeHistory));
            assertEquals("B", text(document));
            assertEquals(1, exactListenerCount(document, nativeHistory));
            assertEquals(1, exactListenerCount(document, foreign));

            document.removeUndoableEditListener(foreign);
            try (DesignerAtomicEditCapture fresh =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                fresh.abort();
            }
            assertOnlyNativeListener(document, nativeHistory);
        });
    }

    @Test
    void activeTokenCannotWrapAnEditFromAnotherDartDocument()
            throws Exception {
        onEdt(() -> {
            BaseDocument owner = document("owner");
            BaseDocument foreign = document("foreign");
            UndoRedo.Manager ownerHistory = new UndoRedo.Manager();
            UndoRedo.Manager foreignHistory = new UndoRedo.Manager();
            owner.addUndoableEditListener(ownerHistory);
            foreign.addUndoableEditListener(foreignHistory);

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(owner, ownerHistory)) {
                foreign.insertString(foreign.getLength(), "-S1", null);
                capture.abort();
            }

            assertEquals("foreign-S1", text(foreign));
            assertFalse(ownerHistory.canUndo());
            assertTrue(foreignHistory.canUndo());
            foreignHistory.undo();
            assertEquals("foreign", text(foreign));
            assertOnlyNativeListener(owner, ownerHistory);
            assertOnlyNativeListener(foreign, foreignHistory);
        });
    }

    @Test
    void offEdtBeginIsRejectedWithoutInstallingAToken() throws Exception {
        AtomicReference<BaseDocument> documentRef = new AtomicReference<>();
        AtomicReference<UndoRedo.Manager> historyRef = new AtomicReference<>();
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager history = new UndoRedo.Manager();
            document.addUndoableEditListener(history);
            documentRef.set(document);
            historyRef.set(history);
        });

        assertFalse(EventQueue.isDispatchThread());
        assertThrows(IllegalStateException.class,
                () -> DesignerAtomicEditCapture.begin(
                        documentRef.get(), historyRef.get()));

        onEdt(() -> {
            try (DesignerAtomicEditCapture fresh =
                    DesignerAtomicEditCapture.begin(
                            documentRef.get(), historyRef.get())) {
                fresh.abort();
            }
            assertOnlyNativeListener(documentRef.get(), historyRef.get());
        });
    }

    @Test
    void closingUnfinishedTokenAllowsFreshBeginOnSameDocument()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);

            try (DesignerAtomicEditCapture unfinished =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                // Deliberately neither sealed nor aborted.
            }
            assertOnlyNativeListener(document, nativeHistory);

            try (DesignerAtomicEditCapture fresh =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                fresh.abort();
            }
            assertOnlyNativeListener(document, nativeHistory);
        });
    }

    @Test
    void failedManagerListenerInstallRollsBackBothTokenHalves()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            FailingInstallManager nativeHistory =
                    new FailingInstallManager();
            document.addUndoableEditListener(nativeHistory);

            assertThrows(IllegalStateException.class,
                    () -> DesignerAtomicEditCapture.begin(
                            document, nativeHistory));
            assertEquals("B", text(document));
            assertEquals(1, nativeHistory.captureListenerRemovals());
            assertOnlyNativeListener(document, nativeHistory);

            nativeHistory.allowInstall();
            appendCommittedDesignerEdit(document, nativeHistory, "M1");
            assertEquals("BM1", text(document));
            assertOnlyNativeListener(document, nativeHistory);
        });
    }

    @Test
    void throwingLogHandlerCannotBreakCommittedReplayOrDieRelease() {
        Logger logger = Logger.getLogger(
                DesignerSemanticUndoableEdit.class.getName());
        Handler throwingHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                throw new AssertionError("synthetic logging failure");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        boolean usedParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(throwingHandler);
        try {
            AtomicInteger revision = new AtomicInteger(1);
            DesignerSemanticUndoableEdit committed =
                    new DesignerSemanticUndoableEdit(
                            0,
                            1,
                            "Logged replay",
                            null,
                            replay(revision, new AtomicInteger()),
                            publication -> {
                                throw new AssertionError(
                                        "synthetic publication queue failure");
                            });

            committed.undo();
            assertEquals(0, revision.get());
            assertTrue(committed.canRedo(),
                    "logging cannot reopen an already committed replay");

            AtomicInteger releases = new AtomicInteger();
            UndoableEdit brokenDelegate = new AbstractUndoableEdit() {
                @Override
                public void die() {
                    throw new AssertionError(
                            "synthetic delegate cleanup failure");
                }
            };
            DesignerSemanticUndoableEdit cleanup =
                    new DesignerSemanticUndoableEdit(
                            1,
                            2,
                            "Logged cleanup",
                            brokenDelegate,
                            new DesignerSemanticUndoableEdit.ReplayController() {
                                @Override
                                public DesignerSemanticUndoableEdit.PreparedReplay
                                        prepare(
                                                DesignerSemanticUndoableEdit.Direction direction,
                                                long currentRevisionId,
                                                long targetRevisionId) {
                                    throw new AssertionError(
                                            "cleanup must not replay");
                                }

                                @Override
                                public void release(
                                        long beforeRevisionId,
                                        long afterRevisionId) {
                                    releases.incrementAndGet();
                                }
                            },
                            publication -> { });

            cleanup.die();
            cleanup.die();
            assertEquals(1, releases.get(),
                    "logging and repeated die() cannot skip or duplicate release");
        } finally {
            logger.removeHandler(throwingHandler);
            logger.setUseParentHandlers(usedParentHandlers);
        }
    }

    @Test
    void rejectedPublicationQueueFallsBackExactlyOnceAfterAtomicUnlock()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            AtomicBoolean outerAtomicRunning = new AtomicBoolean();
            AtomicBoolean publishedBeforeAtomicReturn = new AtomicBoolean();
            AtomicInteger enqueueAttempts = new AtomicInteger();
            AtomicInteger publications = new AtomicInteger();

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                outerAtomicRunning.set(true);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "M1", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    capture.seal(
                            0,
                            1,
                            "Fallback Designer apply",
                            replay(new AtomicInteger(1), new AtomicInteger()),
                            publication -> {
                                enqueueAttempts.incrementAndGet();
                                throw new IllegalStateException(
                                        "synthetic pre-enqueue rejection");
                            },
                            () -> () -> {
                                publishedBeforeAtomicReturn.set(
                                        outerAtomicRunning.get());
                                publications.incrementAndGet();
                            });
                });

                assertEquals(1, enqueueAttempts.get());
                assertEquals(0, publications.get(),
                        "stateChanged must not publish under the document write lock");
                outerAtomicRunning.set(false);

                capture.verifyCompleted();
                capture.verifyCompleted();

                assertTrue(capture.committed());
                assertFalse(publishedBeforeAtomicReturn.get());
                assertEquals(1, publications.get(),
                        "the post-write fallback must publish exactly once");
            }
            assertTrue(nativeHistory.canUndo());
            assertEquals("BM1", text(document));
        });
    }

    @Test
    void acceptedPublicationQueueNeverAlsoUsesDirectFallback()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            ArrayDeque<DesignerSemanticUndoableEdit.DeferredPublication>
                    queued = new ArrayDeque<>();
            AtomicInteger publications = new AtomicInteger();

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "M1", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    capture.seal(
                            0,
                            1,
                            "Queued Designer apply",
                            replay(new AtomicInteger(1), new AtomicInteger()),
                            queued::addLast,
                            () -> publications::incrementAndGet);
                });

                capture.verifyCompleted();
                capture.verifyCompleted();

                assertEquals(0, publications.get(),
                        "a successful queue handoff owns the only publication");
                assertEquals(1, queued.size());
            }

            drain(queued);
            assertEquals(1, publications.get());
        });
    }

    @Test
    void failingPostWriteFallbackAndLoggerAreContainedWithoutRetry()
            throws Exception {
        Logger logger = Logger.getLogger(
                DesignerAtomicEditCapture.class.getName());
        Handler throwingHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                throw new AssertionError("synthetic capture logging failure");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        boolean usedParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(throwingHandler);
        try {
            onEdt(() -> {
                BaseDocument document = document("B");
                UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
                document.addUndoableEditListener(nativeHistory);
                AtomicInteger publications = new AtomicInteger();

                try (DesignerAtomicEditCapture capture =
                        DesignerAtomicEditCapture.begin(
                                document, nativeHistory)) {
                    AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                            document, AtomicLockDocument.class);
                    atomic.runAtomic(() -> {
                        try {
                            document.insertString(
                                    document.getLength(), "M1", null);
                        } catch (BadLocationException failure) {
                            throw new IllegalStateException(failure);
                        }
                        capture.seal(
                                0,
                                1,
                                "Failing fallback Designer apply",
                                replay(new AtomicInteger(1),
                                        new AtomicInteger()),
                                publication -> {
                                    throw new AssertionError(
                                            "synthetic queue rejection");
                                },
                                () -> () -> {
                                    publications.incrementAndGet();
                                    throw new AssertionError(
                                            "synthetic publication failure");
                                });
                    });

                    capture.verifyCompleted();
                    capture.verifyCompleted();

                    assertTrue(capture.committed());
                    assertEquals(1, publications.get(),
                            "a fatal fallback callback must be contained and never retried");
                }
                assertTrue(nativeHistory.canUndo(),
                        "presentation failure cannot reopen native admission");
            });
        } finally {
            logger.removeHandler(throwingHandler);
            logger.setUseParentHandlers(usedParentHandlers);
        }
    }

    @Test
    void sourceModelSourceUsesOneNativeChronologicalTimeline() throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            ArrayDeque<DesignerSemanticUndoableEdit.DeferredPublication>
                    publications = new ArrayDeque<>();
            AtomicInteger published = new AtomicInteger();
            AtomicInteger modelRevision = new AtomicInteger(0);

            document.insertString(document.getLength(), "S1", null);

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "M1", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    capture.seal(
                            0,
                            1,
                            "Set Flutter Property",
                            replay(modelRevision, published),
                            publications::addLast,
                            () -> {
                                assertEquals(0, modelRevision.get());
                                modelRevision.set(1);
                                return published::incrementAndGet;
                            });
                });
                capture.verifyCompleted();
                assertTrue(capture.committed());
            }
            drain(publications);
            assertEquals(1, published.get());
            assertEquals(1, modelRevision.get());

            document.insertString(document.getLength(), "S2", null);
            assertEquals("BS1M1S2", text(document));

            nativeHistory.undo();
            drain(publications);
            assertEquals("BS1M1", text(document));
            assertEquals(1, modelRevision.get());
            assertEquals("Undo Set Flutter Property",
                    nativeHistory.getUndoPresentationName());

            nativeHistory.undo();
            drain(publications);
            assertEquals("BS1", text(document));
            assertEquals(0, modelRevision.get());

            nativeHistory.undo();
            drain(publications);
            assertEquals("B", text(document));
            assertFalse(nativeHistory.canUndo());

            nativeHistory.redo();
            drain(publications);
            assertEquals("BS1", text(document));
            assertEquals(0, modelRevision.get());

            nativeHistory.redo();
            drain(publications);
            assertEquals("BS1M1", text(document));
            assertEquals(1, modelRevision.get());

            nativeHistory.redo();
            drain(publications);
            assertEquals("BS1M1S2", text(document));
            assertFalse(nativeHistory.canRedo());
            assertEquals(3, published.get(),
                    "initial apply, Undo and Redo each publish exactly once");
        });
    }

    @Test
    void rolledBackAtomicMutationCreatesNoPhantomNativeEntry()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            document.insertString(document.getLength(), "S1", null);

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "candidate", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    atomic.atomicUndo();
                });
                capture.abort();
                assertTrue(capture.aborted());
            }

            assertEquals("BS1", text(document));
            assertTrue(nativeHistory.canUndo());
            nativeHistory.undo();
            assertEquals("B", text(document));
            assertFalse(nativeHistory.canUndo(),
                    "the forward/rollback compound edit must have been killed");
        });
    }

    @Test
    void endedNativeManagerIsRejectedBeforeDocumentMutation()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager endedHistory = new UndoRedo.Manager();
            endedHistory.end();
            document.addUndoableEditListener(endedHistory);

            assertThrows(IOException.class,
                    () -> DesignerAtomicEditCapture.begin(
                            document, endedHistory));
            assertEquals("B", text(document));
            assertEquals(1, exactListenerCount(document, endedHistory));
        });
    }

    @Test
    void replayCommitFailureRestoresDocumentAndNativeEditCursor()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            AtomicInteger modelRevision = new AtomicInteger(1);

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "M1", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    capture.seal(
                            0,
                            1,
                            "Set Flutter Property",
                            (direction, current, target) -> new
                                    DesignerSemanticUndoableEdit.PreparedReplay() {
                                @Override
                                public DesignerSemanticUndoableEdit
                                        .DeferredPublication commit()
                                        throws Exception {
                                    throw new IOException(
                                            "synthetic semantic commit failure");
                                }

                                @Override
                                public void abort() {
                                    assertEquals(1, modelRevision.get());
                                }

                                @Override
                                public void invalidate(Throwable failure) {
                                    modelRevision.set(-1);
                                }
                            },
                            publication -> { },
                            () -> () -> { });
                });
                capture.verifyCompleted();
            }

            assertThrows(CannotUndoException.class, nativeHistory::undo);
            assertEquals("BM1", text(document),
                    "a failed semantic commit must redo the raw document edge");
            assertEquals(1, modelRevision.get());
            assertTrue(nativeHistory.canUndo(),
                    "the same chronological entry must remain retryable");
        });
    }

    @Test
    void nativeBranchTruncationReleasesSemanticRevisionExactlyOnce()
            throws Exception {
        onEdt(() -> {
            BaseDocument document = document("B");
            UndoRedo.Manager nativeHistory = new UndoRedo.Manager();
            document.addUndoableEditListener(nativeHistory);
            AtomicInteger modelRevision = new AtomicInteger(0);
            AtomicInteger releases = new AtomicInteger();
            DesignerSemanticUndoableEdit.ReplayController replayController =
                    new DesignerSemanticUndoableEdit.ReplayController() {
                @Override
                public DesignerSemanticUndoableEdit.PreparedReplay prepare(
                        DesignerSemanticUndoableEdit.Direction direction,
                        long currentRevisionId,
                        long targetRevisionId) {
                    assertEquals(currentRevisionId, modelRevision.get());
                    return new DesignerSemanticUndoableEdit.PreparedReplay() {
                        @Override
                        public DesignerSemanticUndoableEdit.DeferredPublication
                                commit() {
                            modelRevision.set(Math.toIntExact(targetRevisionId));
                            return () -> { };
                        }

                        @Override
                        public void abort() {
                            assertEquals(currentRevisionId,
                                    modelRevision.get());
                        }

                        @Override
                        public void invalidate(Throwable failure) {
                            modelRevision.set(-1);
                        }
                    };
                }

                @Override
                public void release(long beforeRevisionId, long afterRevisionId) {
                    assertEquals(0, beforeRevisionId);
                    assertEquals(1, afterRevisionId);
                    releases.incrementAndGet();
                }
            };

            try (DesignerAtomicEditCapture capture =
                    DesignerAtomicEditCapture.begin(document, nativeHistory)) {
                AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                        document, AtomicLockDocument.class);
                atomic.runAtomic(() -> {
                    try {
                        document.insertString(document.getLength(), "M1", null);
                    } catch (BadLocationException failure) {
                        throw new IllegalStateException(failure);
                    }
                    capture.seal(
                            0,
                            1,
                            "Set Flutter Property",
                            replayController,
                            publication -> { },
                            () -> {
                                modelRevision.set(1);
                                return () -> { };
                            });
                });
                capture.verifyCompleted();
            }

            assertEquals("BM1", text(document));
            assertEquals(1, modelRevision.get());
            assertEquals(0, releases.get());

            nativeHistory.undo();
            assertEquals("B", text(document));
            assertEquals(0, modelRevision.get());
            assertEquals(0, releases.get(),
                    "Undo retains the semantic edge for Redo");

            document.insertString(document.getLength(), "S2", null);
            assertEquals("BS2", text(document));
            assertFalse(nativeHistory.canRedo());
            assertEquals(1, releases.get(),
                    "a new Source branch must release the removed Designer edge");

            nativeHistory.discardAllEdits();
            assertEquals(1, releases.get(),
                    "a dead semantic edge must never release twice");
        });
    }

    private static DesignerSemanticUndoableEdit.ReplayController replay(
            AtomicInteger modelRevision,
            AtomicInteger published) {
        return (direction, current, target) -> {
            assertEquals(current, modelRevision.get());
            return new DesignerSemanticUndoableEdit.PreparedReplay() {
                @Override
                public DesignerSemanticUndoableEdit.DeferredPublication commit() {
                    assertEquals(current, modelRevision.get());
                    modelRevision.set(Math.toIntExact(target));
                    return published::incrementAndGet;
                }

                @Override
                public void abort() {
                    assertEquals(current, modelRevision.get());
                }

                @Override
                public void invalidate(Throwable failure) {
                    modelRevision.set(-1);
                }
            };
        };
    }

    private static void appendCommittedDesignerEdit(
            BaseDocument document,
            UndoRedo.Manager nativeHistory,
            String suffix) throws Exception {
        try (DesignerAtomicEditCapture capture =
                DesignerAtomicEditCapture.begin(document, nativeHistory)) {
            AtomicLockDocument atomic = LineDocumentUtils.asRequired(
                    document, AtomicLockDocument.class);
            atomic.runAtomic(() -> {
                try {
                    document.insertString(document.getLength(), suffix, null);
                } catch (BadLocationException failure) {
                    throw new IllegalStateException(failure);
                }
                capture.seal(
                        0,
                        1,
                        "Adversarial Designer edit",
                        (direction, currentRevisionId, targetRevisionId) -> {
                            throw new AssertionError(
                                    "the helper edit must not be replayed");
                        },
                        publication -> { },
                        () -> () -> { });
            });
            capture.verifyCompleted();
        }
    }

    private static BaseDocument document(String initial) throws Exception {
        BaseDocument document = new ProductionWrapperBaseDocument();
        document.insertString(0, initial, null);
        return document;
    }

    /**
     * Plain Surefire does not boot the NetBeans module system, so it does not
     * mount {@code META-INF/generated-layer.xml} into MIME lookup. This test
     * document executes the production wrapper at the exact BaseDocument
     * delivery seam; the real module uses the generated MIME registration
     * asserted above. Listener identity and the native manager remain intact.
     */
    private static final class ProductionWrapperBaseDocument
            extends BaseDocument {
        private final DesignerUndoableEditWrapper productionWrapper =
                new DesignerUndoableEditWrapper();

        ProductionWrapperBaseDocument() {
            // Use a private test MIME so BaseDocument cannot apply the same
            // generated wrapper a second time if another test happens to boot
            // NetBeans' global module lookup in this JVM.
            super(false, "text/x-designer-atomic-capture-test");
        }

        @Override
        protected void fireUndoableEditUpdate(UndoableEditEvent event) {
            UndoableEdit wrapped = productionWrapper.wrap(
                    event.getEdit(), this);
            UndoableEditEvent delivered = wrapped == event.getEdit()
                    ? event : new UndoableEditEvent(event.getSource(), wrapped);
            super.fireUndoableEditUpdate(delivered);
        }
    }

    private static String text(BaseDocument document) {
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static int exactListenerCount(
            BaseDocument document,
            UndoableEditListener expected) {
        int count = 0;
        for (var listener : document.getUndoableEditListeners()) {
            if (listener == expected) {
                count++;
            }
        }
        return count;
    }

    private static void assertOnlyNativeListener(
            BaseDocument document,
            UndoableEditListener nativeHistory) {
        UndoableEditListener[] listeners =
                document.getUndoableEditListeners();
        assertEquals(1, listeners.length,
                "capture must not replace or duplicate the CES Undo listener");
        assertSame(nativeHistory, listeners[0],
                "the exact native Undo listener identity must survive");
    }

    private static void drain(
            ArrayDeque<DesignerSemanticUndoableEdit.DeferredPublication>
                    publications) {
        while (!publications.isEmpty()) {
            publications.removeFirst().publish();
        }
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
            return;
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            try {
                runnable.run();
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
    }

    private static final class FailingInstallManager
            extends UndoRedo.Manager {
        private boolean failInstall = true;
        private int captureListenerRemovals;

        @Override
        public void addChangeListener(ChangeListener listener) {
            super.addChangeListener(listener);
            if (failInstall) {
                throw new IllegalStateException(
                        "synthetic ChangeListener installation failure");
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            if (listener instanceof DesignerAtomicEditCapture) {
                captureListenerRemovals++;
            }
            super.removeChangeListener(listener);
        }

        void allowInstall() {
            failInstall = false;
        }

        int captureListenerRemovals() {
            return captureListenerRemovals;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

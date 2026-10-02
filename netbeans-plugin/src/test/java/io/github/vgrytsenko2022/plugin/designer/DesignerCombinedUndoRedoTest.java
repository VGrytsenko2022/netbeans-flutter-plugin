package io.github.vgrytsenko2022.plugin.designer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import org.junit.jupiter.api.Test;
import org.openide.awt.UndoRedo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignerCombinedUndoRedoTest {

    @Test
    void nativeSourceRemainsTheOnlyDelegateDuringDesignerLifetimeBinding() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        UndoRedo.Manager designer = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicInteger changes = new AtomicInteger();
        combined.addChangeListener(event -> changes.incrementAndGet());

        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Source edit")));
        assertTrue(combined.canUndo());
        assertEquals("Undo Source edit", combined.getUndoPresentationName());
        int beforeBinding = changes.get();

        try (DesignerCombinedUndoRedo.SessionBinding ignored =
                combined.bindDesignerSession(designer)) {
            assertTrue(combined.designerSessionActive());
            assertTrue(combined.canUndo());
            assertEquals("Undo Source edit", combined.getUndoPresentationName());
            assertEquals(beforeBinding, changes.get(),
                    "a lifetime token must not perturb native presentation");

            designer.undoableEditHappened(new UndoableEditEvent(
                    designer, edit("Designer-only edit")));
            assertEquals(beforeBinding, changes.get(),
                    "the bridge must not listen to command-session history");
            assertEquals("Undo Source edit", combined.getUndoPresentationName());

            source.undoableEditHappened(new UndoableEditEvent(
                    source, edit("Second source edit")));
            assertEquals(beforeBinding + 1, changes.get());
            assertEquals("Undo Second source edit",
                    combined.getUndoPresentationName());
            combined.undo();
            assertTrue(combined.canRedo());
            assertEquals("Redo Second source edit",
                    combined.getRedoPresentationName());
            assertTrue(designer.canUndo(),
                    "native Undo must never consume the command-session history");
        }

        assertFalse(combined.designerSessionActive());
        assertTrue(combined.canUndo());
        assertEquals("Undo Source edit", combined.getUndoPresentationName());
        assertEquals(beforeBinding + 2, changes.get(),
                "binding close is presentation-silent; only native add/Undo publish");
    }

    @Test
    void sessionLifetimeBindingIsExclusiveAndIdempotent() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        DesignerCombinedUndoRedo.SessionBinding first =
                combined.bindDesignerSession(new UndoRedo.Manager());

        assertThrows(IllegalStateException.class,
                () -> combined.bindDesignerSession(new UndoRedo.Manager()));
        assertThrows(IllegalArgumentException.class,
                () -> combined.bindDesignerSession(source));

        first.close();
        first.close();
        assertFalse(combined.designerSessionActive());

        try (DesignerCombinedUndoRedo.SessionBinding ignored =
                combined.bindDesignerSession(new UndoRedo.Manager())) {
            assertTrue(combined.designerSessionActive());
        }
    }

    @Test
    void nestedDeferralCoalescesNativeManagerNotificationsUntilOutermostClose() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicInteger changes = new AtomicInteger();
        combined.addChangeListener(event -> changes.incrementAndGet());

        DesignerCombinedUndoRedo.NotificationDeferral outer =
                combined.deferNotifications();
        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("First source edit")));
        assertEquals(0, changes.get());

        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                combined.deferNotifications()) {
            source.discardAllEdits();
            source.undoableEditHappened(new UndoableEditEvent(
                    source, edit("Replacement source edit")));
            assertEquals(0, changes.get());
        }
        assertEquals(0, changes.get(),
                "a nested close must not publish the pending event");

        outer.close();
        outer.close();
        assertEquals(1, changes.get(),
                "the outer close must publish one coalesced notification");
        assertTrue(combined.canUndo());

        source.discardAllEdits();
        assertEquals(2, changes.get(),
                "ordinary source notifications remain immediate");
    }

    @Test
    void notificationCallbackCannotOpenAnOverlappingDocumentDeferral() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicInteger rejected = new AtomicInteger();
        combined.addChangeListener(event -> {
            assertThrows(IllegalStateException.class,
                    combined::deferNotifications);
            rejected.incrementAndGet();
        });

        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Source edit")));

        assertEquals(1, rejected.get());
    }

    @Test
    void semanticPublicationRunsAfterBarrierAndBeforeUndoPresentation() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        ArrayList<String> order = new ArrayList<>();
        combined.addChangeListener(event -> order.add("presentation"));

        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                combined.deferNotifications()) {
            source.addEdit(edit("Designer edge"));
            combined.enqueueSemanticPublication(() -> order.add("semantic"));
            assertEquals(List.of(), order,
                    "neither semantic nor presentation callback may run under the barrier");
        }

        assertEquals(List.of("semantic", "presentation"), order);
    }

    @Test
    void fatalSemanticPublicationCannotStrandPresentationOwnership() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicInteger semanticAttempts = new AtomicInteger();
        AtomicInteger presentations = new AtomicInteger();
        combined.addChangeListener(event -> presentations.incrementAndGet());

        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                combined.deferNotifications()) {
            source.addEdit(edit("Designer edge"));
            combined.enqueueSemanticPublication(() -> {
                semanticAttempts.incrementAndGet();
                throw new AssertionError("synthetic semantic publication failure");
            });
            combined.enqueueSemanticPublication(semanticAttempts::incrementAndGet);
        }

        assertEquals(2, semanticAttempts.get(),
                "a fatal semantic callback must not cancel later committed publications");
        assertEquals(1, presentations.get());
        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Source edge")));
        assertEquals(2, presentations.get(),
                "the failed semantic callback must release publication ownership");
    }

    @Test
    void fatalNativePresentationListenerCannotOrphanBindingOwnership() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        UndoRedo.Manager designer = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicInteger callbacks = new AtomicInteger();
        combined.addChangeListener(event -> {
            callbacks.incrementAndGet();
            throw new AssertionError("synthetic fatal presentation failure");
        });

        DesignerCombinedUndoRedo.SessionBinding binding =
                combined.bindDesignerSession(designer);
        assertTrue(combined.designerSessionActive());

        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Native edit")));
        assertTrue(combined.designerSessionActive());

        binding.close();

        assertFalse(combined.designerSessionActive());
        source.discardAllEdits();
        assertEquals(2, callbacks.get());
    }

    @Test
    void repeatedCachedFatalListenerReleasesReentrantPublicationOwnership() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AssertionError cachedFailure = new AssertionError(
                "synthetic cached presentation failure");
        AtomicInteger fatalCallbacks = new AtomicInteger();
        javax.swing.event.ChangeListener fatalListener = event -> {
            int invocation = fatalCallbacks.incrementAndGet();
            if (invocation == 1) {
                source.undoableEditHappened(new UndoableEditEvent(
                        source, edit("Reentrant source edit")));
            }
            throw cachedFailure;
        };
        combined.addChangeListener(fatalListener);

        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Initial source edit")));

        assertEquals(2, fatalCallbacks.get(),
                "the reentrant publication must drain after the same cached "
                + "Error fails both callbacks");
        combined.removeChangeListener(fatalListener);

        AtomicInteger recoveredCallbacks = new AtomicInteger();
        combined.addChangeListener(event -> recoveredCallbacks.incrementAndGet());
        try (DesignerCombinedUndoRedo.SessionBinding ignored =
                combined.bindDesignerSession(new UndoRedo.Manager())) {
            assertTrue(combined.designerSessionActive());
        }
        assertFalse(combined.designerSessionActive());
        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Post-failure source edit")));

        assertEquals(1, recoveredCallbacks.get(),
                "lifetime bind/unbind stays silent and the next native event "
                + "must publish after the failed drain releases ownership");
    }

    @Test
    void activeSourceUndoRejectsAConcurrentDesignerBinding() throws Exception {
        BlockingUndoManager source = new BlockingUndoManager();
        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Source edit")));
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread undo = new Thread(() -> {
            try {
                combined.undo();
            } catch (Throwable caught) {
                failure.set(caught);
            }
        }, "owned-source-undo");
        undo.setDaemon(true);
        undo.start();
        assertTrue(source.entered.await(2, TimeUnit.SECONDS));

        assertThrows(IllegalStateException.class,
                () -> combined.bindDesignerSession(new UndoRedo.Manager()));

        source.release.countDown();
        undo.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(undo.isAlive());
        assertNull(failure.get());
        assertFalse(combined.designerSessionActive());
        assertTrue(combined.canRedo());
    }

    @Test
    void bindingCloseWaitsForItsOwnedNativeUndoToFinish() throws Exception {
        BlockingUndoManager source = new BlockingUndoManager();
        source.undoableEditHappened(new UndoableEditEvent(
                source, edit("Native edit")));
        UndoRedo.Manager designer = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        DesignerCombinedUndoRedo.SessionBinding binding =
                combined.bindDesignerSession(designer);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread undo = new Thread(() -> {
            try {
                combined.undo();
            } catch (Throwable caught) {
                failure.set(caught);
            }
        }, "owned-designer-undo");
        undo.setDaemon(true);
        undo.start();
        assertTrue(source.entered.await(2, TimeUnit.SECONDS));

        binding.close();
        assertTrue(combined.designerSessionActive(),
                "the exact session lifetime token must survive in-flight native Undo");

        source.release.countDown();
        undo.join(TimeUnit.SECONDS.toMillis(2));
        assertFalse(undo.isAlive());
        assertNull(failure.get());
        assertFalse(combined.designerSessionActive());
        assertFalse(combined.canUndo());
    }

    @Test
    void bindingCloseWaitsForAnInternalDocumentDeferral() {
        UndoRedo.Manager source = new UndoRedo.Manager();
        DesignerCombinedUndoRedo combined = new DesignerCombinedUndoRedo(source);
        DesignerCombinedUndoRedo.SessionBinding binding =
                combined.bindDesignerSession(new UndoRedo.Manager());

        try (DesignerCombinedUndoRedo.NotificationDeferral ignored =
                combined.deferNotifications()) {
            binding.close();
            assertTrue(combined.designerSessionActive());
        }

        assertFalse(combined.designerSessionActive());
    }

    private static AbstractUndoableEdit edit(String name) {
        return new AbstractUndoableEdit() {
            @Override
            public String getPresentationName() {
                return name;
            }
        };
    }

    private static final class BlockingUndoManager extends UndoRedo.Manager {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public void undo() throws CannotUndoException {
            entered.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new CannotUndoException();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new CannotUndoException();
            }
            super.undo();
        }
    }
}

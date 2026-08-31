package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterDesignerEditorPaneContentTest {
    @Test
    void bracketsEveryDesignPerspectiveTransitionExactlyOnce() {
        RecordingPerspective design = new RecordingPerspective();
        List<FlutterDesignerEditorPaneContent.Perspective> selections =
                new ArrayList<>();
        FlutterDesignerEditorPaneContent content =
                new FlutterDesignerEditorPaneContent(
                        new JPanel(), design, ignored -> { },
                        (ignored, failure) -> { }, selections::add);

        content.opened();
        content.opened();
        content.showing();
        content.showing();
        content.activated();
        content.activated();
        assertEquals(List.of("bind", "opened", "showing", "activated"),
                design.events);

        content.selectSource();
        content.selectSource();
        assertEquals(List.of(
                "bind", "opened", "showing", "activated",
                "deactivated", "hidden"), design.events);
        assertEquals(List.of(
                FlutterDesignerEditorPaneContent.Perspective.SOURCE),
                selections);

        content.selectDesign();
        assertEquals(List.of(
                "bind", "opened", "showing", "activated",
                "deactivated", "hidden", "showing", "activated"),
                design.events);

        content.closed();
        content.closed();
        assertEquals(List.of(
                "bind", "opened", "showing", "activated",
                "deactivated", "hidden", "showing", "activated",
                "deactivated", "hidden", "closed"), design.events);
    }

    @Test
    void keepsDesignCloseAuthorityWhileSourceIsSelected() {
        RecordingPerspective design = new RecordingPerspective();
        AtomicReference<Long> retriedAttempt = new AtomicReference<>();
        AtomicReference<Long> failedAttempt = new AtomicReference<>();
        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        FlutterDesignerEditorPaneContent content =
                new FlutterDesignerEditorPaneContent(
                        new JPanel(), design, retriedAttempt::set,
                        (attemptId, failure) -> {
                            failedAttempt.set(attemptId);
                            closeFailure.set(failure);
                        }, ignored -> { });

        assertSame(design.lookup, content.activeLookup());
        content.selectSource();
        assertSame(Lookup.EMPTY, content.activeLookup());
        assertSame(design.closeState, content.closeState());

        design.closeBarrierState =
                FlutterDesignerEditorPerspective.CloseBarrierState.PENDING;
        assertSame(FlutterDesignerEditorPerspective.CloseBarrierState.PENDING,
                content.closeBarrierState(41));
        assertEquals(List.of(41L), design.barrierStateAttempts);

        content.beginCloseBarrier(41);
        content.abandonCloseBarrier(41);
        assertEquals(List.of(41L), design.begunAttempts);
        assertEquals(List.of(41L), design.abandonedAttempts);

        Throwable failure = new IllegalStateException("close failed");
        design.retry.accept(41);
        design.failure.accept(41L, failure);
        assertEquals(41L, retriedAttempt.get());
        assertEquals(41L, failedAttempt.get());
        assertSame(failure, closeFailure.get());

        content.opened();
        content.closed();
        assertEquals(1, design.events.stream()
                .filter("closed"::equals)
                .count());
    }

    private static final class RecordingPerspective
            implements FlutterDesignerEditorPerspective {
        private final List<String> events = new ArrayList<>();
        private final JComponent visual = new JPanel();
        private final JComponent toolbar = new JPanel();
        private final Lookup lookup = Lookups.singleton(this);
        private final CloseOperationState closeState =
                CloseOperationState.STATE_OK;
        private LongConsumer retry;
        private BiConsumer<Long, Throwable> failure;
        private FlutterDesignerEditorPerspective.CloseBarrierState
                closeBarrierState =
                        FlutterDesignerEditorPerspective.CloseBarrierState.OPEN;
        private final List<Long> barrierStateAttempts = new ArrayList<>();
        private final List<Long> begunAttempts = new ArrayList<>();
        private final List<Long> abandonedAttempts = new ArrayList<>();

        @Override
        public JComponent visual() {
            return visual;
        }

        @Override
        public JComponent toolbar() {
            return toolbar;
        }

        @Override
        public Action[] actions() {
            return new Action[0];
        }

        @Override
        public Lookup lookup() {
            return lookup;
        }

        @Override
        public CloseOperationState closeState() {
            return closeState;
        }

        @Override
        public void opened() {
            events.add("opened");
        }

        @Override
        public void closed() {
            events.add("closed");
        }

        @Override
        public void showing() {
            events.add("showing");
        }

        @Override
        public void hidden() {
            events.add("hidden");
        }

        @Override
        public void activated() {
            events.add("activated");
        }

        @Override
        public void deactivated() {
            events.add("deactivated");
        }

        @Override
        public void bindCloseCallbacks(
                LongConsumer retry,
                BiConsumer<Long, Throwable> failure) {
            this.retry = retry;
            this.failure = failure;
            events.add("bind");
        }

        @Override
        public FlutterDesignerEditorPerspective.CloseBarrierState
                closeBarrierState(long attemptId) {
            barrierStateAttempts.add(attemptId);
            return closeBarrierState;
        }

        @Override
        public void beginCloseBarrier(long attemptId) {
            begunAttempts.add(attemptId);
        }

        @Override
        public void abandonCloseBarrier(long attemptId) {
            abandonedAttempts.add(attemptId);
        }
    }
}

package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
                        new JPanel(), design, () -> { }, selections::add);

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
        AtomicReference<Runnable> closeRetry = new AtomicReference<>();
        Runnable retry = () -> { };
        FlutterDesignerEditorPaneContent content =
                new FlutterDesignerEditorPaneContent(
                        new JPanel(), design, retry, ignored -> { });
        closeRetry.set(design.retry);

        assertSame(design.lookup, content.activeLookup());
        content.selectSource();
        assertSame(Lookup.EMPTY, content.activeLookup());
        assertSame(design.closeState, content.closeState());
        assertSame(retry, closeRetry.get());

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
        private Runnable retry;

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
        public void bindCloseRetry(Runnable retry) {
            this.retry = retry;
            events.add("bind");
        }
    }
}

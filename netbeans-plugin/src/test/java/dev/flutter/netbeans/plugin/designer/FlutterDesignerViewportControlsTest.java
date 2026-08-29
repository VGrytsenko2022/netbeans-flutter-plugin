package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
import dev.flutter.netbeans.designer.model.StableId;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class FlutterDesignerViewportControlsTest {

    @Test
    void stepsFromFitScaleAndAcrossManualPresets() throws Exception {
        onEdt(() -> {
            List<CanvasViewportPresentation> emitted = new ArrayList<>();
            FlutterDesignerViewportControls controls =
                    new FlutterDesignerViewportControls(emitted::add);
            controls.setMetrics(metrics(
                    CanvasViewportPresentation.fit(), 630_000, false, false));

            button(controls, "+").doClick();
            assertEquals(CanvasViewportPresentation.manual(
                    750_000, 0, 0), emitted.getLast());

            button(controls, "−").doClick();
            assertEquals(CanvasViewportPresentation.manual(
                    500_000, 0, 0), emitted.getLast());

            zoomSelector(controls).setSelectedIndex(4); // 100%
            button(controls, "+").doClick();
            assertEquals(CanvasViewportPresentation.manual(
                    1_250_000, 0, 0), emitted.getLast());
        });
    }

    @Test
    void appliesMetricsAsFeedbackWithoutEmittingACommand() throws Exception {
        onEdt(() -> {
            List<CanvasViewportPresentation> emitted = new ArrayList<>();
            FlutterDesignerViewportControls controls =
                    new FlutterDesignerViewportControls(emitted::add);
            CanvasViewportPresentation confirmed =
                    CanvasViewportPresentation.manual(1_100_000, 230_000, 670_000);

            controls.setMetrics(metrics(confirmed, 1_100_000, true, false));

            assertTrue(emitted.isEmpty());
            assertEquals(confirmed, controls.currentPresentation());
            assertEquals(230_000,
                    controls.currentPresentation().horizontalScrollMicros());
            assertEquals(670_000,
                    controls.currentPresentation().verticalScrollMicros());
            assertEquals("110%", zoomSelector(controls).getSelectedItem().toString());
            assertNotNull(findLabel(controls, "(110%)"));
        });
    }

    @Test
    void fitFeedbackShowsTheActualRunnerComputedScale() throws Exception {
        onEdt(() -> {
            List<CanvasViewportPresentation> emitted = new ArrayList<>();
            FlutterDesignerViewportControls controls =
                    new FlutterDesignerViewportControls(emitted::add);
            controls.setMetrics(metrics(
                    CanvasViewportPresentation.fit(), 134_900, false, false));

            assertNotNull(findLabel(controls, "(13%)"));
            assertTrue(emitted.isEmpty());
        });
    }

    @Test
    void disablesEveryInteractiveControlAndPublishesAccessibleCopy()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerViewportControls controls =
                    new FlutterDesignerViewportControls(ignored -> { });
            controls.setMetrics(metrics(
                    CanvasViewportPresentation.manual(
                            1_000_000, 0, 0),
                    1_000_000, true, true));
            controls.setControlsEnabled(false);

            assertFalse(zoomSelector(controls).isEnabled());
            assertFalse(button(controls, "+").isEnabled());
            assertFalse(button(controls, "−").isEnabled());
            assertFalse(controls.toolbarComponent()
                    .getAccessibleContext().getAccessibleName().isBlank());
            assertFalse(zoomSelector(controls)
                    .getAccessibleContext().getAccessibleDescription().isBlank());
        });
    }

    private static CanvasViewportMetrics metrics(
            CanvasViewportPresentation presentation,
            int effectiveScaleMicros,
            boolean horizontalScrollable,
            boolean verticalScrollable) {
        return new CanvasViewportMetrics(
                new CanvasRevisionKey(
                        CanvasSessionId.random(), 1, StableId.random(), 1),
                0,
                presentation,
                effectiveScaleMicros,
                horizontalScrollable,
                verticalScrollable);
    }

    private static JComboBox<?> zoomSelector(
            FlutterDesignerViewportControls controls) {
        return find(controls.toolbarComponent(), JComboBox.class);
    }

    private static JButton button(
            FlutterDesignerViewportControls controls,
            String text) {
        return descendants(controls.toolbarComponent()).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow();
    }

    private static JLabel findLabel(
            FlutterDesignerViewportControls controls,
            String text) {
        return descendants(controls.toolbarComponent()).stream()
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElse(null);
    }

    private static <T extends Component> T find(
            Component root,
            Class<T> type) {
        return descendants(root).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    private static List<Component> descendants(Component root) {
        List<Component> result = new ArrayList<>();
        result.add(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                result.addAll(descendants(child));
            }
        }
        return result;
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            });
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

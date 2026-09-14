package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.canvas.CanvasViewportMetrics;
import dev.flutter.netbeans.designer.canvas.CanvasViewportPresentation;
import dev.flutter.netbeans.designer.canvas.CanvasZoomMode;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Per-designer-tab controls for the native Canvas presentation transform.
 *
 * <p>The controls deliberately keep their state in memory. Zoom and scroll are
 * presentation concerns and must never become part of the persisted {@code .fd}
 * model.</p>
 */
final class FlutterDesignerViewportControls {

    private static final int[] ZOOM_PRESETS_MICROS = {
        250_000,
        500_000,
        750_000,
        1_000_000,
        1_250_000,
        1_500_000,
        2_000_000
    };

    private final Consumer<CanvasViewportPresentation> presentationListener;
    private final JPanel toolbar = new JPanel();
    private final JLabel zoomLabel = new JLabel("Zoom:");
    private final DefaultComboBoxModel<ZoomChoice> zoomModel =
            new DefaultComboBoxModel<>();
    private final JComboBox<ZoomChoice> zoomSelector = new JComboBox<>(zoomModel);
    private final JButton zoomOut = new JButton("−");
    private final JButton zoomIn = new JButton("+");
    private final JLabel effectiveScale = new JLabel("(100%)");
    private CanvasViewportPresentation presentation =
            CanvasViewportPresentation.fit();
    private int effectiveScaleMicros = CanvasViewportPresentation.MICROS_PER_UNIT;
    private boolean controlsEnabled = true;
    private boolean applyingFeedback;
    private ZoomChoice customChoice;

    FlutterDesignerViewportControls(
            Consumer<CanvasViewportPresentation> presentationListener) {
        this.presentationListener = Objects.requireNonNull(
                presentationListener, "presentationListener");
        populateZoomChoices();
        configureToolbar();
        configureAccessibility();
        installListeners();
        applyPresentationToControls();
    }

    JComponent toolbarComponent() {
        return toolbar;
    }

    CanvasViewportPresentation currentPresentation() {
        return presentation;
    }

    /** Returns the next Canvas viewport presentation to the deterministic Fit state. */
    void resetToFit() {
        publish(CanvasViewportPresentation.fit());
    }

    /** Applies authoritative runner feedback without producing another command. */
    void setMetrics(CanvasViewportMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        applyingFeedback = true;
        try {
            presentation = metrics.presentation();
            effectiveScaleMicros = metrics.effectiveScaleMicros();
            applyPresentationToControls();
        } finally {
            applyingFeedback = false;
        }
        refreshEnabledState();
    }

    void setControlsEnabled(boolean enabled) {
        controlsEnabled = enabled;
        refreshEnabledState();
    }

    private void populateZoomChoices() {
        zoomModel.addElement(ZoomChoice.fit());
        Arrays.stream(ZOOM_PRESETS_MICROS)
                .mapToObj(ZoomChoice::manual)
                .forEach(zoomModel::addElement);
    }

    private void configureToolbar() {
        toolbar.setLayout(new javax.swing.BoxLayout(
                toolbar, javax.swing.BoxLayout.X_AXIS));
        toolbar.setOpaque(false);
        zoomLabel.setLabelFor(zoomSelector);
        toolbar.add(zoomLabel);
        toolbar.add(Box.createHorizontalStrut(4));

        Dimension selectorSize = zoomSelector.getPreferredSize();
        zoomSelector.setMaximumSize(new Dimension(
                Math.max(selectorSize.width, 82), selectorSize.height));
        toolbar.add(zoomSelector);
        toolbar.add(Box.createHorizontalStrut(3));
        makeCompact(zoomOut);
        makeCompact(zoomIn);
        toolbar.add(zoomOut);
        toolbar.add(zoomIn);
        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(effectiveScale);
        Dimension toolbarSize = toolbar.getPreferredSize();
        toolbar.setMaximumSize(new Dimension(
                toolbarSize.width, toolbarSize.height));
    }

    private static void makeCompact(JButton button) {
        Dimension preferred = button.getPreferredSize();
        button.setPreferredSize(new Dimension(
                Math.max(24, preferred.height), preferred.height));
        button.setMaximumSize(button.getPreferredSize());
        button.setFocusable(false);
    }

    private void configureAccessibility() {
        toolbar.getAccessibleContext().setAccessibleName(
                "Flutter Canvas zoom controls");
        toolbar.getAccessibleContext().setAccessibleDescription(
                "Change only the in-IDE presentation scale of the fixed logical "
                + "Flutter viewport.");
        zoomSelector.setToolTipText(
                "Choose Fit or a fixed Canvas zoom percentage.");
        zoomSelector.getAccessibleContext().setAccessibleName(
                "Flutter Canvas zoom");
        zoomSelector.getAccessibleContext().setAccessibleDescription(
                "Choose Fit or a fixed zoom percentage. This does not change the "
                + "Flutter logical viewport.");
        zoomOut.setToolTipText("Use the next smaller Canvas zoom preset.");
        zoomOut.getAccessibleContext().setAccessibleName(
                "Zoom Flutter Canvas out");
        zoomOut.getAccessibleContext().setAccessibleDescription(
                "Use the next smaller Canvas zoom preset.");
        zoomIn.setToolTipText("Use the next larger Canvas zoom preset.");
        zoomIn.getAccessibleContext().setAccessibleName(
                "Zoom Flutter Canvas in");
        zoomIn.getAccessibleContext().setAccessibleDescription(
                "Use the next larger Canvas zoom preset.");
        effectiveScale.getAccessibleContext().setAccessibleName(
                "Effective Flutter Canvas scale");
        effectiveScale.getAccessibleContext().setAccessibleDescription(
                "The scale currently reported by the native Flutter Canvas.");
    }

    private void installListeners() {
        zoomSelector.addActionListener(event -> {
            if (applyingFeedback) {
                return;
            }
            ZoomChoice choice = (ZoomChoice) zoomSelector.getSelectedItem();
            if (choice == null) {
                return;
            }
            if (choice.mode() == CanvasZoomMode.FIT) {
                publish(CanvasViewportPresentation.fit());
            } else {
                publish(CanvasViewportPresentation.manual(
                        choice.zoomMicros(),
                        presentation.horizontalScrollMicros(),
                        presentation.verticalScrollMicros()));
            }
        });
        zoomOut.addActionListener(event -> stepZoom(false));
        zoomIn.addActionListener(event -> stepZoom(true));
    }

    private void stepZoom(boolean larger) {
        int base = presentation.mode() == CanvasZoomMode.FIT
                ? effectiveScaleMicros
                : presentation.zoomMicros();
        int target = adjacentPreset(base, larger);
        if (target < 0) {
            return;
        }
        publish(CanvasViewportPresentation.manual(
                target,
                presentation.horizontalScrollMicros(),
                presentation.verticalScrollMicros()));
    }

    private void publish(CanvasViewportPresentation next) {
        if (next.equals(presentation)) {
            refreshEnabledState();
            return;
        }
        presentation = next;
        applyPresentationToControls();
        presentationListener.accept(next);
    }

    private void applyPresentationToControls() {
        boolean previousGuard = applyingFeedback;
        applyingFeedback = true;
        try {
            selectPresentationChoice();
            effectiveScale.setText("(" + percent(effectiveScaleMicros) + "%)");
            effectiveScale.setToolTipText("Effective Canvas scale: "
                    + percent(effectiveScaleMicros) + "%");
        } finally {
            applyingFeedback = previousGuard;
        }
        refreshEnabledState();
    }

    private void selectPresentationChoice() {
        if (customChoice != null) {
            zoomModel.removeElement(customChoice);
            customChoice = null;
        }
        if (presentation.mode() == CanvasZoomMode.FIT) {
            zoomSelector.setSelectedIndex(0);
            return;
        }
        for (int index = 1; index < zoomModel.getSize(); index++) {
            ZoomChoice choice = zoomModel.getElementAt(index);
            if (choice.zoomMicros() == presentation.zoomMicros()) {
                zoomSelector.setSelectedItem(choice);
                return;
            }
        }
        customChoice = ZoomChoice.manual(presentation.zoomMicros());
        zoomModel.insertElementAt(customChoice, 1);
        zoomSelector.setSelectedItem(customChoice);
    }

    private void refreshEnabledState() {
        zoomLabel.setEnabled(controlsEnabled);
        zoomSelector.setEnabled(controlsEnabled);
        effectiveScale.setEnabled(controlsEnabled);
        int base = presentation.mode() == CanvasZoomMode.FIT
                ? effectiveScaleMicros
                : presentation.zoomMicros();
        zoomOut.setEnabled(controlsEnabled && adjacentPreset(base, false) >= 0);
        zoomIn.setEnabled(controlsEnabled && adjacentPreset(base, true) >= 0);
    }

    private static int adjacentPreset(int zoomMicros, boolean larger) {
        if (larger) {
            for (int preset : ZOOM_PRESETS_MICROS) {
                if (preset > zoomMicros) {
                    return preset;
                }
            }
            return -1;
        }
        for (int index = ZOOM_PRESETS_MICROS.length - 1; index >= 0; index--) {
            if (ZOOM_PRESETS_MICROS[index] < zoomMicros) {
                return ZOOM_PRESETS_MICROS[index];
            }
        }
        return -1;
    }

    private static int percent(int micros) {
        return (int) Math.round(micros * 100.0d
                / CanvasViewportPresentation.MICROS_PER_UNIT);
    }

    private record ZoomChoice(
            CanvasZoomMode mode,
            int zoomMicros,
            String label) {

        static ZoomChoice fit() {
            return new ZoomChoice(CanvasZoomMode.FIT,
                    CanvasViewportPresentation.MICROS_PER_UNIT, "Fit");
        }

        static ZoomChoice manual(int zoomMicros) {
            return new ZoomChoice(CanvasZoomMode.MANUAL, zoomMicros,
                    percent(zoomMicros) + "%");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

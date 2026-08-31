package dev.flutter.netbeans.plugin.designer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.openide.util.Lookup;

/**
 * Plugin-owned Design/Source switcher used by the dedicated CES pane.
 *
 * <p>The Source component remains the one and only editor component created by
 * {@code CloneableEditor}. This class adds a Design perspective around it; it
 * never creates a second editor pane or a second document listener graph.</p>
 */
final class FlutterDesignerEditorPaneContent extends JPanel {
    enum Perspective {
        DESIGN,
        SOURCE
    }

    private final FlutterDesignerEditorPerspective design;
    private final JPanel sourcePanel = new JPanel(new BorderLayout());
    private final JTabbedPane perspectives = new JTabbedPane();
    private final Consumer<Perspective> perspectiveListener;
    private Perspective selected = Perspective.DESIGN;
    private boolean opened;
    private boolean showing;
    private boolean activated;
    private boolean designShowing;
    private boolean designActivated;
    private boolean closed;
    private boolean synchronizingSelection;

    FlutterDesignerEditorPaneContent(
            Component sourceComponent,
            FlutterDesignerEditorPerspective design,
            Runnable closeRetry,
            Consumer<Perspective> perspectiveListener) {
        super(new BorderLayout());
        this.design = Objects.requireNonNull(design, "design");
        this.perspectiveListener = Objects.requireNonNull(
                perspectiveListener, "perspectiveListener");
        Objects.requireNonNull(closeRetry, "closeRetry");
        sourcePanel.add(Objects.requireNonNull(
                sourceComponent, "sourceComponent"), BorderLayout.CENTER);

        JPanel designPanel = new JPanel(new BorderLayout());
        designPanel.add(design.toolbar(), BorderLayout.NORTH);
        designPanel.add(design.visual(), BorderLayout.CENTER);
        perspectives.addTab("Design", designPanel);
        perspectives.addTab("Source", sourcePanel);
        perspectives.getAccessibleContext().setAccessibleName(
                "Flutter Designer perspectives");
        perspectives.getAccessibleContext().setAccessibleDescription(
                "Switch between the visual Flutter Designer and its paired Dart source.");
        perspectives.addChangeListener(event -> tabSelectionChanged());
        add(perspectives, BorderLayout.CENTER);
        design.bindCloseRetry(closeRetry);
        setPerspective(Perspective.DESIGN);
    }

    void setSourceToolbar(JToolBar toolbar) {
        Objects.requireNonNull(toolbar, "toolbar");
        sourcePanel.add(toolbar, BorderLayout.NORTH);
        sourcePanel.revalidate();
        sourcePanel.repaint();
    }

    Perspective perspective() {
        return selected;
    }

    void selectSource() {
        setPerspective(Perspective.SOURCE);
    }

    void selectDesign() {
        setPerspective(Perspective.DESIGN);
    }

    void setPerspective(Perspective perspective) {
        Objects.requireNonNull(perspective, "perspective");
        if (selected == perspective
                && perspectives.getSelectedIndex() == perspective.ordinal()) {
            return;
        }
        synchronizingSelection = true;
        try {
            perspectives.setSelectedIndex(perspective.ordinal());
        } finally {
            synchronizingSelection = false;
        }
        transitionTo(perspective);
    }

    Lookup activeLookup() {
        return selected == Perspective.DESIGN ? design.lookup() : Lookup.EMPTY;
    }

    Action[] activeActions() {
        return selected == Perspective.DESIGN
                ? design.actions().clone()
                : new Action[0];
    }

    CloseOperationState closeState() {
        return design.closeState();
    }

    boolean requestPerspectiveFocus() {
        if (selected != Perspective.DESIGN) {
            return false;
        }
        // The native Canvas focus handoff is initiated from Design activation
        // and may not make this Swing container report focus success. Never
        // fall through to the hidden Source editor while Design is selected.
        design.visual().requestFocusInWindow();
        return true;
    }

    void opened() {
        if (opened || closed) {
            return;
        }
        opened = true;
        design.opened();
    }

    void showing() {
        if (showing || closed) {
            return;
        }
        showing = true;
        showDesignIfSelected();
    }

    void activated() {
        if (activated || closed) {
            return;
        }
        activated = true;
        activateDesignIfSelected();
    }

    void deactivated() {
        if (!activated) {
            return;
        }
        deactivateDesign();
        activated = false;
    }

    void hidden() {
        if (!showing) {
            return;
        }
        deactivateDesign();
        hideDesign();
        activated = false;
        showing = false;
    }

    void closed() {
        if (closed) {
            return;
        }
        deactivated();
        hidden();
        if (opened) {
            design.closed();
            opened = false;
        }
        closed = true;
    }

    private void tabSelectionChanged() {
        if (synchronizingSelection) {
            return;
        }
        int index = perspectives.getSelectedIndex();
        if (index >= 0) {
            transitionTo(Perspective.values()[index]);
        }
    }

    private void transitionTo(Perspective perspective) {
        if (selected == perspective) {
            return;
        }
        deactivateDesign();
        hideDesign();
        selected = perspective;
        perspectiveListener.accept(perspective);
        showDesignIfSelected();
        activateDesignIfSelected();
    }

    private void showDesignIfSelected() {
        if (selected == Perspective.DESIGN
                && showing
                && opened
                && !designShowing) {
            design.showing();
            designShowing = true;
        }
    }

    private void hideDesign() {
        if (designShowing) {
            design.hidden();
            designShowing = false;
        }
    }

    private void activateDesignIfSelected() {
        if (selected == Perspective.DESIGN
                && activated
                && designShowing
                && !designActivated) {
            design.activated();
            designActivated = true;
        }
    }

    private void deactivateDesign() {
        if (designActivated) {
            design.deactivated();
            designActivated = false;
        }
    }
}

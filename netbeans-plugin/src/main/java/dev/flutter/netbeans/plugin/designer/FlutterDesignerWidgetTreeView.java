package dev.flutter.netbeans.plugin.designer;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.openide.explorer.view.BeanTreeView;

/**
 * Designer-scoped widget hierarchy view that remains fully expanded.
 *
 * <p>The Canvas hierarchy is an editor outline rather than a navigation tree:
 * hiding descendants makes the visible model misleading. Connector painting is
 * kept local to this view so the active NetBeans look and feel and every other
 * tree remain untouched.</p>
 */
final class FlutterDesignerWidgetTreeView extends BeanTreeView {

    private static final Color GOLD_ON_LIGHT = new Color(0xA87408);
    private static final Color GOLD_ON_DARK = new Color(0xD8A72B);
    private static final float CONNECTOR_WIDTH = 1.0f;

    private final TreeModelListener expansionModelListener =
            new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent event) {
                    scheduleExpandAll();
                }

                @Override
                public void treeNodesInserted(TreeModelEvent event) {
                    scheduleExpandAll();
                }

                @Override
                public void treeNodesRemoved(TreeModelEvent event) {
                    scheduleExpandAll();
                }

                @Override
                public void treeStructureChanged(TreeModelEvent event) {
                    scheduleExpandAll();
                }
            };

    private TreeModel listenedModel;
    private boolean expansionScheduled;

    FlutterDesignerWidgetTreeView() {
        // TreeView intentionally skips viewport installation in a headless
        // environment. Installing the already-created JTree keeps the component
        // deterministic for headless module tests without changing production.
        if (getViewport().getView() != tree) {
            setViewportView(tree);
        }
        // BasicTreeUI honors this client property; other LAFs may ignore it.
        // Either way it is scoped to this exact JTree and never touches
        // UIManager defaults used by the rest of NetBeans.
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.setToggleClickCount(0);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addPropertyChangeListener(
                "UI", event -> hideExpansionControls());
        hideExpansionControls();
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                // Expansion is always admitted.
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event)
                    throws ExpandVetoException {
                throw new ExpandVetoException(
                        event, "Flutter widget hierarchy remains expanded");
            }
        });
        tree.addPropertyChangeListener("model", event -> {
            attachModelListener((TreeModel) event.getNewValue());
            scheduleExpandAll();
        });
        attachModelListener(tree.getModel());
        scheduleExpandAll();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        scheduleExpandAll();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (tree != null) {
            hideExpansionControls();
            scheduleExpandAll();
        }
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        paintGoldenConnectors(graphics);
    }

    JTree treeComponent() {
        return tree;
    }

    List<Line2D.Double> connectorSegments() {
        List<Line2D.Double> segments = new ArrayList<>();
        if (tree.getRowCount() < 2 || getViewport().getView() != tree) {
            return List.of();
        }
        Point treeOrigin = SwingUtilities.convertPoint(tree, 0, 0, this);
        boolean leftToRight = tree.getComponentOrientation().isLeftToRight();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath childPath = tree.getPathForRow(row);
            TreePath parentPath = childPath == null
                    ? null : childPath.getParentPath();
            if (parentPath == null) {
                continue;
            }
            Rectangle parent = tree.getPathBounds(parentPath);
            Rectangle child = tree.getPathBounds(childPath);
            if (parent == null || child == null) {
                continue;
            }
            double parentCenterY = treeOrigin.y + parent.getCenterY();
            double childCenterY = treeOrigin.y + child.getCenterY();
            double elbowX;
            double childEdgeX;
            if (leftToRight) {
                elbowX = treeOrigin.x + parent.x
                        + Math.max(4.0d, (child.x - parent.x) / 2.0d);
                childEdgeX = treeOrigin.x + child.x - 3.0d;
            } else {
                double parentRight = parent.getMaxX();
                double childRight = child.getMaxX();
                elbowX = treeOrigin.x + parentRight
                        - Math.max(4.0d, (parentRight - childRight) / 2.0d);
                childEdgeX = treeOrigin.x + childRight + 3.0d;
            }
            segments.add(new Line2D.Double(
                    elbowX, parentCenterY, elbowX, childCenterY));
            segments.add(new Line2D.Double(
                    elbowX, childCenterY, childEdgeX, childCenterY));
        }
        return List.copyOf(segments);
    }

    Color connectorColor() {
        Color background = tree.getBackground();
        if (background == null) {
            return GOLD_ON_LIGHT;
        }
        double luminance = (0.2126d * background.getRed())
                + (0.7152d * background.getGreen())
                + (0.0722d * background.getBlue());
        return luminance < 128.0d ? GOLD_ON_DARK : GOLD_ON_LIGHT;
    }

    void expandAllNow() {
        // Expanding a row can reveal more rows, therefore rowCount must be
        // queried on every iteration rather than captured before the loop.
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }
    }

    private void attachModelListener(TreeModel next) {
        if (listenedModel == next) {
            return;
        }
        if (listenedModel != null) {
            listenedModel.removeTreeModelListener(expansionModelListener);
        }
        listenedModel = next;
        if (listenedModel != null) {
            listenedModel.addTreeModelListener(expansionModelListener);
        }
    }

    private void hideExpansionControls() {
        // NetBeans-supported Tree UIs (Metal, Windows and FlatLaf variants)
        // derive from BasicTreeUI. Nulling these two per-instance icons removes
        // only this outline's redundant controls while retaining the LAF's row
        // geometry, indentation, renderer and drop feedback.
        if (tree.getUI() instanceof BasicTreeUI basicTreeUI) {
            basicTreeUI.setExpandedIcon(null);
            basicTreeUI.setCollapsedIcon(null);
        }
    }

    private void scheduleExpandAll() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::scheduleExpandAll);
            return;
        }
        if (expansionScheduled) {
            return;
        }
        expansionScheduled = true;
        EventQueue.invokeLater(() -> {
            expansionScheduled = false;
            expandAllNow();
            repaint();
        });
    }

    private void paintGoldenConnectors(Graphics graphics) {
        if (!(graphics instanceof Graphics2D original)) {
            return;
        }
        List<Line2D.Double> segments = connectorSegments();
        if (segments.isEmpty()) {
            return;
        }
        Graphics2D painter = (Graphics2D) original.create();
        try {
            Rectangle viewport = getViewport().getBounds();
            painter.clip(viewport);
            painter.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            painter.setColor(connectorColor());
            painter.setStroke(new java.awt.BasicStroke(CONNECTOR_WIDTH));
            for (Line2D.Double segment : segments) {
                painter.draw(segment);
            }
        } finally {
            painter.dispose();
        }
    }
}

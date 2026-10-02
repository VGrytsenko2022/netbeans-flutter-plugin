package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.geom.Line2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.junit.jupiter.api.Test;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

class FlutterDesignerWidgetTreeViewTest {

    @Test
    void keepsOneDeterministicWidgetSelectionForPropertiesAndDragSource()
            throws Exception {
        onEdt(() -> {
            Branch root = branch("Column", leaf("Text"), leaf("Text"));
            JTree tree = harness(root.node()).view().treeComponent();
            assertEquals(TreeSelectionModel.SINGLE_TREE_SELECTION,
                    tree.getSelectionModel().getSelectionMode());
        });
    }

    @Test
    void expandsInitialAndInsertedBranchesAndVetoesCollapse() throws Exception {
        AtomicReference<FlutterDesignerWidgetTreeView> viewRef =
                new AtomicReference<>();
        AtomicReference<Branch> parentRef = new AtomicReference<>();
        onEdt(() -> {
            Branch parent = branch("Center", leaf("Text"));
            Branch root = branch("Scaffold", parent.node());
            Harness harness = harness(root.node());
            viewRef.set(harness.view());
            parentRef.set(parent);
        });
        drainExpansionQueue();

        onEdt(() -> {
            JTree tree = viewRef.get().treeComponent();
            TreePath parentPath = pathFor(tree, parentRef.get().node());
            assertTrue(viewRef.get().isExpanded(parentRef.get().node()));

            tree.collapsePath(parentPath);
            assertTrue(tree.isExpanded(parentPath),
                    "A user action must not leave a widget branch collapsed");

            Branch inserted = branch("Padding", leaf("Text"));
            parentRef.get().children().add(new Node[]{inserted.node()});
        });
        drainExpansionQueue();

        onEdt(() -> {
            Node inserted = parentRef.get().children().getNodes(true)[1];
            assertTrue(viewRef.get().isExpanded(inserted),
                    "A branch introduced by a model mutation must expand");
        });
    }

    @Test
    void reexpandsEveryBranchAfterAWholeModelRebuild() throws Exception {
        AtomicReference<FlutterDesignerWidgetTreeView> viewRef =
                new AtomicReference<>();
        AtomicReference<Branch> nestedRef = new AtomicReference<>();
        onEdt(() -> {
            Branch firstRoot = branch(
                    "Scaffold", branch("Center", leaf("Text")).node());
            Harness harness = harness(firstRoot.node());
            Branch nested = branch(
                    "Column", branch("Padding", leaf("Text")).node());
            Branch replacement = branch("Scaffold", nested.node());
            harness.manager().setRootContext(replacement.node());
            viewRef.set(harness.view());
            nestedRef.set(nested);
        });
        drainExpansionQueue();

        onEdt(() -> assertTrue(viewRef.get().isExpanded(
                nestedRef.get().node())));
    }

    @Test
    void providesThinGoldenParentChildSegmentsWithoutChangingGlobalTreeStyle()
            throws Exception {
        Object globalHash = UIManager.get("Tree.hash");
        Object globalExpandedIcon = UIManager.get("Tree.expandedIcon");
        Object globalCollapsedIcon = UIManager.get("Tree.collapsedIcon");
        JTree unrelatedTree = new JTree();
        AtomicReference<FlutterDesignerWidgetTreeView> viewRef =
                new AtomicReference<>();
        onEdt(() -> {
            Branch root = branch(
                    "Scaffold", branch("Center", leaf("Text")).node());
            FlutterDesignerWidgetTreeView view = harness(root.node()).view();
            view.setSize(320, 240);
            view.doLayout();
            view.treeComponent().setSize(300, 200);
            viewRef.set(view);
        });
        drainExpansionQueue();

        onEdt(() -> {
            FlutterDesignerWidgetTreeView view = viewRef.get();
            List<Line2D.Double> segments = view.connectorSegments();
            assertFalse(segments.isEmpty());
            assertEquals(0, segments.size() % 2,
                    "Each visible edge has a vertical and horizontal segment");
            assertTrue(segments.stream().anyMatch(segment ->
                    segment.x1 == segment.x2 && segment.y1 != segment.y2));
            assertTrue(segments.stream().anyMatch(segment ->
                    segment.y1 == segment.y2 && segment.x1 != segment.x2));

            view.treeComponent().setBackground(Color.WHITE);
            Color onLight = view.connectorColor();
            view.treeComponent().setBackground(Color.BLACK);
            Color onDark = view.connectorColor();
            assertNotEquals(onLight, onDark);
            assertEquals("None", view.treeComponent()
                    .getClientProperty("JTree.lineStyle"));
            BasicTreeUI treeUi = (BasicTreeUI) view.treeComponent().getUI();
            assertNull(treeUi.getExpandedIcon());
            assertNull(treeUi.getCollapsedIcon());

            view.treeComponent().updateUI();
            BasicTreeUI refreshedUi = (BasicTreeUI) view.treeComponent().getUI();
            assertNull(refreshedUi.getExpandedIcon(),
                    "A look-and-feel refresh must not restore expand controls");
            assertNull(refreshedUi.getCollapsedIcon(),
                    "A look-and-feel refresh must not restore collapse controls");
        });

        assertEquals(globalHash, UIManager.get("Tree.hash"));
        assertEquals(globalExpandedIcon, UIManager.get("Tree.expandedIcon"));
        assertEquals(globalCollapsedIcon, UIManager.get("Tree.collapsedIcon"));
        assertNull(unrelatedTree.getClientProperty("JTree.lineStyle"));
    }

    private static Harness harness(Node root) {
        ExplorerManager manager = new ExplorerManager();
        ProviderPanel provider = new ProviderPanel(manager);
        FlutterDesignerWidgetTreeView view =
                new FlutterDesignerWidgetTreeView();
        view.setRootVisible(true);
        provider.add(view);
        view.addNotify();
        manager.setRootContext(root);
        return new Harness(manager, provider, view);
    }

    private static Branch branch(String name, Node... children) {
        Children.Array hierarchy = new Children.Array();
        hierarchy.add(children);
        AbstractNode node = new AbstractNode(hierarchy);
        node.setName(name);
        node.setDisplayName(name);
        return new Branch(node, hierarchy);
    }

    private static Node leaf(String name) {
        AbstractNode node = new AbstractNode(Children.LEAF);
        node.setName(name);
        node.setDisplayName(name);
        return node;
    }

    private static TreePath pathFor(JTree tree, Node node) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath candidate = tree.getPathForRow(row);
            if (candidate != null && Visualizer.findNode(
                    candidate.getLastPathComponent()) == node) {
                return candidate;
            }
        }
        throw new AssertionError("Node is not visible: " + node.getDisplayName());
    }

    private static void drainExpansionQueue() throws Exception {
        // A model event coalesces to one invokeLater expansion. A second drain
        // also covers an expansion-triggered lazy child event.
        onEdt(() -> { });
        onEdt(() -> { });
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
            return;
        }
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

    private record Branch(AbstractNode node, Children.Array children) { }

    private record Harness(
            ExplorerManager manager,
            ProviderPanel provider,
            FlutterDesignerWidgetTreeView view) { }

    private static final class ProviderPanel extends JPanel
            implements ExplorerManager.Provider {
        private final ExplorerManager manager;

        private ProviderPanel(ExplorerManager manager) {
            this.manager = manager;
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return manager;
        }
    }
}

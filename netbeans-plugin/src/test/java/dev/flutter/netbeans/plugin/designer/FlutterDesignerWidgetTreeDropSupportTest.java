package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import org.junit.jupiter.api.Test;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

class FlutterDesignerWidgetTreeDropSupportTest {

    @Test
    void resolvesExactTreePathAndCommitsOnlyAfterAcceptedPreview()
            throws Exception {
        onEdt(() -> {
            StableId centerId = StableId.random();
            Harness harness = harness(widget("Center", centerId));
            List<FlutterDesignerWidgetTreeDropSupport.Feedback> feedback =
                    new ArrayList<>();
            AtomicInteger previews = new AtomicInteger();
            AtomicInteger commits = new AtomicInteger();
            FlutterDesignerWidgetTreeDropSupport<String> support =
                    new FlutterDesignerWidgetTreeDropSupport<>(
                            harness.view(),
                            new FlutterDesignerWidgetTreeDropSupport.Admission<>() {
                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Preview<String>
                                        preview(
                                                Transferable transferable,
                                                int action,
                                                StableId targetId) {
                                    previews.incrementAndGet();
                                    assertEquals(centerId, targetId);
                                    assertEquals(DnDConstants.ACTION_MOVE, action);
                                    return FlutterDesignerWidgetTreeDropSupport.Preview
                                            .accepted("prepared", "Add Text to Center.child");
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Decision
                                        commit(
                                                String prepared,
                                                Transferable transferable,
                                                int action,
                                                StableId targetId) {
                                    commits.incrementAndGet();
                                    assertEquals("prepared", prepared);
                                    assertEquals(centerId, targetId);
                                    return FlutterDesignerWidgetTreeDropSupport.Decision
                                            .accepted("Text added to Center.child");
                                }
                            },
                            feedback::add);
            TreePath target = pathFor(harness.view(), centerId);
            StringSelection token = new StringSelection("nbfdnd:v1:test-token");

            var preview = support.previewForTest(
                    token, DnDConstants.ACTION_MOVE, target);
            assertTrue(preview.accepted());
            assertEquals(1, previews.get());
            assertEquals(0, commits.get());

            var committed = support.commitForTest(
                    token, DnDConstants.ACTION_MOVE, target);
            assertTrue(committed.accepted());
            assertEquals(2, previews.get(),
                    "Commit must re-preview the latest exact target state");
            assertEquals(1, commits.get());
            assertEquals(target, harness.view().treeComponent().getSelectionPath());
            assertTrue(feedback.getLast().committed());
            assertTrue(feedback.getLast().message().contains("Center.child"));
            support.close();
        });
    }

    @Test
    void rejectionNeverCallsCommitAndPublishesExactReason() throws Exception {
        onEdt(() -> {
            StableId textId = StableId.random();
            Harness harness = harness(widget("Text", textId));
            List<FlutterDesignerWidgetTreeDropSupport.Feedback> feedback =
                    new ArrayList<>();
            AtomicInteger commits = new AtomicInteger();
            FlutterDesignerWidgetTreeDropSupport<String> support =
                    new FlutterDesignerWidgetTreeDropSupport<>(
                            harness.view(),
                            new FlutterDesignerWidgetTreeDropSupport.Admission<>() {
                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Preview<String>
                                        preview(
                                                Transferable transferable,
                                                int action,
                                                StableId targetId) {
                                    return FlutterDesignerWidgetTreeDropSupport.Preview
                                            .rejected("Text has no child slot.");
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Decision
                                        commit(
                                                String prepared,
                                                Transferable transferable,
                                                int action,
                                                StableId targetId) {
                                    commits.incrementAndGet();
                                    return FlutterDesignerWidgetTreeDropSupport.Decision
                                            .accepted("must not run");
                                }
                            },
                            feedback::add);

            var result = support.commitForTest(
                    new StringSelection("nbfdnd:v1:test-token"),
                    DnDConstants.ACTION_MOVE,
                    pathFor(harness.view(), textId));

            assertFalse(result.accepted());
            assertEquals("Text has no child slot.", result.message());
            assertEquals(0, commits.get());
            assertEquals(result.message(), feedback.getLast().message());
            assertEquals(result.message(),
                    harness.view().treeComponent().getToolTipText());
            support.close();
        });
    }

    @Test
    void installsScopedOnRowDropUiAndRejectsInvalidTargetsBeforeAdmission()
            throws Exception {
        AtomicReference<FlutterDesignerWidgetTreeDropSupport<String>> supportRef =
                new AtomicReference<>();
        AtomicReference<TreePath> validPath = new AtomicReference<>();
        AtomicInteger admissionCalls = new AtomicInteger();
        AtomicInteger feedbackCalls = new AtomicInteger();
        onEdt(() -> {
            StableId columnId = StableId.random();
            Harness harness = harness(widget("Column", columnId));
            TransferHandler previous = harness.view().treeComponent()
                    .getTransferHandler();
            boolean previousDropTarget = harness.view().isDropTarget();
            DropTarget previousAwtDropTarget = harness.view().treeComponent()
                    .getDropTarget();
            boolean previousAwtDropTargetActive = previousAwtDropTarget != null
                    && previousAwtDropTarget.isActive();
            FlutterDesignerWidgetTreeDropSupport<String> support =
                    new FlutterDesignerWidgetTreeDropSupport<>(
                            harness.view(),
                            new RejectingAdmission(admissionCalls),
                            ignored -> feedbackCalls.incrementAndGet());
            assertEquals(DropMode.ON_OR_INSERT,
                    harness.view().treeComponent().getDropMode());
            assertNotNull(harness.view().treeComponent().getTransferHandler());
            assertFalse(previous == harness.view().treeComponent()
                    .getTransferHandler());
            assertTrue(support.moveGestureInstalledForTest());
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                assertNotNull(support.installedDropTargetForTest());
                assertFalse(previousAwtDropTarget
                                == support.installedDropTargetForTest(),
                        "the inactive BeanTreeView target must not block Swing");
                assertTrue(support.installedDropTargetForTest().isActive());
            }

            var wrongAction = support.previewForTest(
                    new StringSelection("nbfdnd:v1:test-token"),
                    DnDConstants.ACTION_COPY,
                    pathFor(harness.view(), columnId));
            assertFalse(wrongAction.accepted());
            assertTrue(wrongAction.message().contains("MOVE"));

            AbstractNode noStableId = new AbstractNode(Children.LEAF);
            noStableId.setDisplayName("not a Flutter widget");
            var noTarget = support.previewForTest(
                    new StringSelection("nbfdnd:v1:test-token"),
                    DnDConstants.ACTION_MOVE,
                    new TreePath(Visualizer.findVisualizer(noStableId)));
            assertFalse(noTarget.accepted());
            assertTrue(noTarget.message().contains("exact Flutter widget row"));
            assertEquals(0, admissionCalls.get());
            assertEquals(2, feedbackCalls.get());

            supportRef.set(support);
            validPath.set(pathFor(harness.view(), columnId));
            support.close();
            assertSame(previous,
                    harness.view().treeComponent().getTransferHandler());
            assertEquals(previousDropTarget, harness.view().isDropTarget());
            assertFalse(support.moveGestureInstalledForTest());
            assertSame(previousAwtDropTarget,
                    harness.view().treeComponent().getDropTarget());
            if (previousAwtDropTarget != null) {
                assertEquals(previousAwtDropTargetActive,
                        previousAwtDropTarget.isActive());
            }
        });

        // The synchronous test hook also fails closed without mutating Swing
        // or invoking feedback when a caller violates the EDT contract.
        var offEdt = supportRef.get().previewForTest(
                new StringSelection("nbfdnd:v1:test-token"),
                DnDConstants.ACTION_MOVE,
                validPath.get());
        assertFalse(offEdt.accepted());
        assertTrue(offEdt.message().contains("EDT"));
        assertEquals(2, feedbackCalls.get());
    }

    @Test
    void explicitMouseGestureExportsOneLocalMoveAfterSystemThreshold()
            throws Exception {
        onEdt(() -> {
            StableId columnId = StableId.random();
            StableId textId = StableId.random();
            Children.Array children = new Children.Array();
            children.add(new Node[]{widget("Text", textId)});
            AbstractNode column = new AbstractNode(
                    children, Lookups.singleton(columnId));
            column.setDisplayName("Column");
            Harness harness = harness(column);
            AtomicInteger exports = new AtomicInteger();
            AtomicInteger previews = new AtomicInteger();
            int threshold = 6;
            MoveWidget command = new MoveWidget(
                    textId,
                    new WidgetPlacement(
                            columnId, new SlotName("children"), 0));

            FlutterDesignerWidgetTreeDropSupport<String> support =
                    new FlutterDesignerWidgetTreeDropSupport<>(
                            harness.view(),
                            new RejectingAdmission(new AtomicInteger()),
                            new FlutterDesignerWidgetTreeDropSupport.MoveAdmission() {
                                @Override
                                public boolean canStart(StableId sourceId) {
                                    return textId.equals(sourceId);
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Preview<MoveWidget>
                                        preview(
                                                StableId sourceId,
                                                FlutterDesignerWidgetTreeDropSupport
                                                        .TreeDropTarget target) {
                                    previews.incrementAndGet();
                                    assertEquals(textId, sourceId);
                                    assertEquals(columnId, target.widgetId());
                                    assertEquals(0, target.childIndex());
                                    return FlutterDesignerWidgetTreeDropSupport.Preview
                                            .accepted(command, "prepared move");
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Decision commit(
                                        MoveWidget prepared,
                                        StableId sourceId,
                                        FlutterDesignerWidgetTreeDropSupport
                                                .TreeDropTarget target) {
                                    throw new AssertionError("preview must not commit");
                                }

                                @Override
                                public void clearPreview() {
                                    // No native Canvas is attached in this gesture test.
                                }
                            },
                            ignored -> { },
                            (handler, source, trigger, action) -> {
                                exports.incrementAndGet();
                                assertSame(harness.view().treeComponent(), source);
                                assertEquals(DnDConstants.ACTION_MOVE, action);
                                assertEquals(TransferHandler.MOVE,
                                        handler.getSourceActions(source));
                            },
                            threshold);

            JTree tree = harness.view().treeComponent();
            TreePath rootPath = pathFor(harness.view(), columnId);
            TreePath textPath = pathFor(harness.view(), textId);

            // The root is selected by the press, but admission keeps it from
            // becoming a source gesture.
            dispatchDrag(tree, rootPath, threshold + 2);
            assertEquals(0, exports.get());

            Point origin = rowCenter(tree, textPath);
            tree.dispatchEvent(mouse(
                    tree, MouseEvent.MOUSE_PRESSED, origin,
                    InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1));
            tree.dispatchEvent(mouse(
                    tree, MouseEvent.MOUSE_DRAGGED,
                    new Point(origin.x + threshold - 1, origin.y),
                    InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON));
            assertEquals(0, exports.get(),
                    "ordinary click jitter must not start a move");
            tree.dispatchEvent(mouse(
                    tree, MouseEvent.MOUSE_DRAGGED,
                    new Point(origin.x + threshold, origin.y),
                    InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON));
            tree.dispatchEvent(mouse(
                    tree, MouseEvent.MOUSE_DRAGGED,
                    new Point(origin.x + threshold + 4, origin.y),
                    InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON));
            assertEquals(1, exports.get(),
                    "one physical gesture must export exactly once");
            tree.dispatchEvent(mouse(
                    tree, MouseEvent.MOUSE_RELEASED,
                    new Point(origin.x + threshold + 4, origin.y),
                    0, MouseEvent.BUTTON1));

            Transferable transferable = support.moveTransferableForTest(textPath);
            assertNotNull(transferable,
                    "the explicit export must use a concrete local move transfer");
            var preview = support.previewMoveTransferForTest(
                    transferable, rootPath, 0);
            assertTrue(preview.accepted());
            assertEquals(1, previews.get());

            support.close();
            dispatchDrag(tree, textPath, threshold + 2);
            assertEquals(1, exports.get(),
                    "closing support must remove its gesture listeners");
        });
    }

    @Test
    void movesExistingWidgetAtExactTreeInsertionAndClearsCanvasPreview()
            throws Exception {
        onEdt(() -> {
            StableId columnId = StableId.random();
            StableId textId = StableId.random();
            Children.Array children = new Children.Array();
            children.add(new Node[]{widget("Text", textId)});
            AbstractNode column = new AbstractNode(
                    children, Lookups.singleton(columnId));
            column.setDisplayName("Column");
            Harness harness = harness(column);
            AtomicInteger previews = new AtomicInteger();
            AtomicInteger commits = new AtomicInteger();
            AtomicInteger clears = new AtomicInteger();
            SlotName childSlot = new SlotName("children");
            MoveWidget command = new MoveWidget(
                    textId,
                    new WidgetPlacement(columnId, childSlot, 0));

            FlutterDesignerWidgetTreeDropSupport<String> support =
                    new FlutterDesignerWidgetTreeDropSupport<>(
                            harness.view(),
                            new RejectingAdmission(new AtomicInteger()),
                            new FlutterDesignerWidgetTreeDropSupport.MoveAdmission() {
                                @Override
                                public boolean canStart(StableId sourceId) {
                                    return textId.equals(sourceId);
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Preview<MoveWidget>
                                        preview(
                                                StableId sourceId,
                                                FlutterDesignerWidgetTreeDropSupport
                                                        .TreeDropTarget target) {
                                    previews.incrementAndGet();
                                    assertEquals(textId, sourceId);
                                    assertEquals(columnId, target.widgetId());
                                    assertEquals(0, target.childIndex());
                                    assertTrue(target.insertion());
                                    return FlutterDesignerWidgetTreeDropSupport.Preview.accepted(
                                            command,
                                            "Move Text to Column.children at index 0.");
                                }

                                @Override
                                public FlutterDesignerWidgetTreeDropSupport.Decision commit(
                                        MoveWidget prepared,
                                        StableId sourceId,
                                        FlutterDesignerWidgetTreeDropSupport
                                                .TreeDropTarget target) {
                                    commits.incrementAndGet();
                                    assertEquals(command, prepared);
                                    assertEquals(textId, sourceId);
                                    return FlutterDesignerWidgetTreeDropSupport.Decision.accepted(
                                            "Moving Text to Column.children at index 0.");
                                }

                                @Override
                                public void clearPreview() {
                                    clears.incrementAndGet();
                                }
                            },
                            ignored -> { });
            TreePath columnPath = pathFor(harness.view(), columnId);

            var preview = support.previewMoveForTest(textId, columnPath, 0);
            assertTrue(preview.accepted());
            assertEquals(1, previews.get());
            assertEquals(0, commits.get());
            assertEquals(0, clears.get());

            var committed = support.commitMoveForTest(textId, columnPath, 0);
            assertTrue(committed.accepted());
            assertEquals(2, previews.get(),
                    "Commit must re-plan against the latest exact tree target");
            assertEquals(1, commits.get());
            assertEquals(1, clears.get());
            support.close();
            assertEquals(2, clears.get());
        });
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
        view.expandAllNow();
        return new Harness(manager, provider, view);
    }

    private static Node widget(String name, StableId id) {
        AbstractNode node = new AbstractNode(
                Children.LEAF, Lookups.singleton(id));
        node.setName(name);
        node.setDisplayName(name);
        return node;
    }

    private static TreePath pathFor(
            FlutterDesignerWidgetTreeView view,
            StableId id) {
        for (int row = 0; row < view.treeComponent().getRowCount(); row++) {
            TreePath path = view.treeComponent().getPathForRow(row);
            if (path == null) {
                continue;
            }
            Node node = Visualizer.findNode(path.getLastPathComponent());
            if (id.equals(node.getLookup().lookup(StableId.class))) {
                return path;
            }
        }
        throw new AssertionError("Stable widget row is not visible: " + id);
    }

    private static void dispatchDrag(
            JTree tree,
            TreePath path,
            int horizontalDistance) {
        Point origin = rowCenter(tree, path);
        tree.dispatchEvent(mouse(
                tree, MouseEvent.MOUSE_PRESSED, origin,
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1));
        tree.dispatchEvent(mouse(
                tree, MouseEvent.MOUSE_DRAGGED,
                new Point(origin.x + horizontalDistance, origin.y),
                InputEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON));
        tree.dispatchEvent(mouse(
                tree, MouseEvent.MOUSE_RELEASED,
                new Point(origin.x + horizontalDistance, origin.y),
                0, MouseEvent.BUTTON1));
    }

    private static Point rowCenter(JTree tree, TreePath path) {
        Rectangle bounds = tree.getPathBounds(path);
        assertNotNull(bounds, "tree row must have bounds");
        return new Point(
                bounds.x + Math.max(1, bounds.width / 2),
                bounds.y + Math.max(1, bounds.height / 2));
    }

    private static MouseEvent mouse(
            JTree tree,
            int id,
            Point point,
            int modifiers,
            int button) {
        return new MouseEvent(
                tree,
                id,
                System.currentTimeMillis(),
                modifiers,
                point.x,
                point.y,
                1,
                false,
                button);
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

    private static final class RejectingAdmission
            implements FlutterDesignerWidgetTreeDropSupport.Admission<String> {
        private final AtomicInteger calls;

        private RejectingAdmission(AtomicInteger calls) {
            this.calls = calls;
        }

        @Override
        public FlutterDesignerWidgetTreeDropSupport.Preview<String> preview(
                Transferable transferable,
                int action,
                StableId targetId) {
            calls.incrementAndGet();
            return FlutterDesignerWidgetTreeDropSupport.Preview.rejected(
                    "not admitted");
        }

        @Override
        public FlutterDesignerWidgetTreeDropSupport.Decision commit(
                String prepared,
                Transferable transferable,
                int action,
                StableId targetId) {
            throw new AssertionError("rejected preview cannot commit");
        }
    }

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

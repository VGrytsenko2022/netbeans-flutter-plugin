package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.command.MoveWidget;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreePath;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;

/**
 * Scoped Swing drag source/drop target for the Designer widget tree.
 *
 * <p>Palette tokens may be dropped onto an exact container row. Existing tree
 * widgets may additionally be moved onto a container or to an insertion line.
 * This class resolves only Swing paths and a same-tree, JVM-local transfer; the
 * supplied admissions remain authoritative for catalog compatibility,
 * placement, revision freshness, Canvas preview and mutation.</p>
 */
final class FlutterDesignerWidgetTreeDropSupport<P> implements AutoCloseable {

    private static final int MAX_TRANSFER_TEXT_LENGTH = 512;
    private static final DataFlavor WIDGET_MOVE_FLAVOR = widgetMoveFlavor();

    private final FlutterDesignerWidgetTreeView view;
    private final JTree tree;
    private final Admission<P> admission;
    private final MoveAdmission moveAdmission;
    private final Consumer<Feedback> feedback;
    private final UUID sourceOwner = UUID.randomUUID();
    private final TransferHandler previousTransferHandler;
    private final DropMode previousDropMode;
    private final boolean previousDropTarget;
    private final boolean previousDragSource;
    private final boolean previousDragEnabled;
    private final DropTarget previousAwtDropTarget;
    private final boolean previousAwtDropTargetActive;
    private final TreeTransferHandler handler = new TreeTransferHandler();
    private final DragExporter dragExporter;
    private final int dragThreshold;
    private final MouseInputAdapter moveDragGesture = new MoveDragGesture();
    private Point armedDragOrigin;
    private TreePath armedDragPath;
    private StableId armedDragWidgetId;
    private boolean dragExportStarted;
    private boolean closed;

    FlutterDesignerWidgetTreeDropSupport(
            FlutterDesignerWidgetTreeView view,
            Admission<P> admission,
            Consumer<Feedback> feedback) {
        this(view, admission, MoveAdmission.disabled(), feedback);
    }

    FlutterDesignerWidgetTreeDropSupport(
            FlutterDesignerWidgetTreeView view,
            Admission<P> admission,
            MoveAdmission moveAdmission,
            Consumer<Feedback> feedback) {
        this(
                view,
                admission,
                moveAdmission,
                feedback,
                TransferHandler::exportAsDrag,
                Math.max(1, DragSource.getDragThreshold()));
    }

    FlutterDesignerWidgetTreeDropSupport(
            FlutterDesignerWidgetTreeView view,
            Admission<P> admission,
            MoveAdmission moveAdmission,
            Consumer<Feedback> feedback,
            DragExporter dragExporter,
            int dragThreshold) {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "Flutter widget-tree drop support must be installed on the EDT");
        }
        this.view = Objects.requireNonNull(view, "view");
        this.tree = view.treeComponent();
        this.admission = Objects.requireNonNull(admission, "admission");
        this.moveAdmission = Objects.requireNonNull(moveAdmission, "moveAdmission");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.dragExporter = Objects.requireNonNull(dragExporter, "dragExporter");
        if (dragThreshold < 1) {
            throw new IllegalArgumentException("dragThreshold must be positive");
        }
        this.dragThreshold = dragThreshold;
        previousTransferHandler = tree.getTransferHandler();
        previousDropMode = tree.getDropMode();
        previousDropTarget = view.isDropTarget();
        previousDragSource = view.isDragSource();
        previousDragEnabled = tree.getDragEnabled();
        previousAwtDropTarget = tree.getDropTarget();
        previousAwtDropTargetActive = previousAwtDropTarget != null
                && previousAwtDropTarget.isActive();

        // BeanTreeView owns a separate NetBeans node DropTarget and drag source.
        // They must not compete with this exact Palette-token/tree-move target.
        // ExplorerTree does not initiate a replacement Swing TransferHandler
        // after its NetBeans drag source is disabled, so this support owns one
        // small threshold-based mouse gesture and explicitly calls exportAsDrag.
        view.setDropTarget(false);
        view.setDragSource(false);
        // TreeViewDropSupport.activate(false) only deactivates NetBeans' AWT
        // target. A non-UIResource target left attached prevents Swing from
        // installing the TransferHandler target, so detach it explicitly.
        tree.setDropTarget(null);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(handler);
        tree.setDragEnabled(false);
        tree.addMouseListener(moveDragGesture);
        tree.addMouseMotionListener(moveDragGesture);
    }

    @Override
    public void close() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::close);
            return;
        }
        if (closed) {
            return;
        }
        closed = true;
        tree.removeMouseMotionListener(moveDragGesture);
        tree.removeMouseListener(moveDragGesture);
        resetMoveDragGesture();
        moveAdmission.clearPreview();
        tree.setTransferHandler(null);
        tree.setDropTarget(null);
        tree.setDropMode(previousDropMode);
        view.setDragSource(previousDragSource);
        if (previousAwtDropTarget != null) {
            tree.setDropTarget(previousAwtDropTarget);
        }
        view.setDropTarget(previousDropTarget);
        if (previousAwtDropTarget != null) {
            previousAwtDropTarget.setActive(previousAwtDropTargetActive);
        }
        tree.setTransferHandler(previousTransferHandler);
        if (!GraphicsEnvironment.isHeadless()) {
            tree.setDragEnabled(previousDragEnabled);
        }
    }

    Decision previewForTest(
            Transferable transferable,
            int sourceActions,
            TreePath targetPath) {
        return evaluatePalette(
                transferable, sourceActions, dropTarget(targetPath, -1), false);
    }

    Decision commitForTest(
            Transferable transferable,
            int sourceActions,
            TreePath targetPath) {
        return evaluatePalette(
                transferable, sourceActions, dropTarget(targetPath, -1), true);
    }

    Decision previewMoveForTest(
            StableId sourceId,
            TreePath targetPath,
            int childIndex) {
        return evaluateMove(
                new WidgetMoveTransfer(sourceOwner, sourceId),
                DnDConstants.ACTION_MOVE,
                dropTarget(targetPath, childIndex),
                false);
    }

    Decision commitMoveForTest(
            StableId sourceId,
            TreePath targetPath,
            int childIndex) {
        return evaluateMove(
                new WidgetMoveTransfer(sourceOwner, sourceId),
                DnDConstants.ACTION_MOVE,
                dropTarget(targetPath, childIndex),
                true);
    }

    Transferable moveTransferableForTest(TreePath sourcePath) {
        if (!EventQueue.isDispatchThread() || closed || sourcePath == null) {
            return null;
        }
        selectPath(sourcePath);
        return handler.createTransferable(tree);
    }

    Decision previewMoveTransferForTest(
            Transferable transferable,
            TreePath targetPath,
            int childIndex) {
        return evaluateMove(
                moveTransfer(transferable).orElse(null),
                DnDConstants.ACTION_MOVE,
                dropTarget(targetPath, childIndex),
                false);
    }

    boolean moveGestureInstalledForTest() {
        return java.util.Arrays.stream(tree.getMouseListeners())
                        .anyMatch(listener -> listener == moveDragGesture)
                && java.util.Arrays.stream(tree.getMouseMotionListeners())
                        .anyMatch(listener -> listener == moveDragGesture);
    }

    DropTarget installedDropTargetForTest() {
        return tree.getDropTarget();
    }

    private Decision evaluatePalette(
            Transferable transferable,
            int sourceActions,
            TreeDropTarget target,
            boolean commit) {
        if (!EventQueue.isDispatchThread()) {
            // TransferHandler is an EDT contract. Fail closed without touching
            // Swing state or invoking UI feedback from the calling thread.
            return Decision.rejected(
                    "Flutter widget-tree drops are accepted only on the EDT.");
        }
        if (closed) {
            return report(Decision.rejected(
                    "Flutter widget-tree drop target is closed."), null, commit);
        }
        if ((sourceActions & DnDConstants.ACTION_MOVE) == 0) {
            return report(Decision.rejected(
                    "Flutter Palette insertion requires the MOVE drag action."),
                    null,
                    commit);
        }
        if (!hasBoundedStringTransfer(transferable)) {
            return report(Decision.rejected(
                    "Drop rejected: the transferable is not an active Flutter "
                    + "Designer Palette token."), null, commit);
        }
        if (target == null || target.insertion()) {
            return report(Decision.rejected(
                    "Palette drop rejected: point to one exact Flutter widget row."),
                    null,
                    commit);
        }
        StableId targetId = target.widgetId();

        Preview<P> preview;
        try {
            preview = Objects.requireNonNull(
                    admission.preview(
                            transferable, DnDConstants.ACTION_MOVE, targetId),
                    "admission.preview result");
        } catch (RuntimeException failure) {
            return report(Decision.rejected(
                    "Drop preview failed for widget " + targetId + ": "
                    + concreteMessage(failure) + '.'), targetId, commit);
        }
        if (!preview.accepted()) {
            return report(Decision.rejected(preview.message()), targetId, commit);
        }
        if (!commit) {
            return report(Decision.accepted(preview.message()), targetId, false);
        }

        Decision result;
        try {
            result = Objects.requireNonNull(
                    admission.commit(
                            preview.prepared(),
                            transferable,
                            DnDConstants.ACTION_MOVE,
                            targetId),
                    "admission.commit result");
        } catch (RuntimeException failure) {
            result = Decision.rejected(
                    "Drop commit failed for widget " + targetId + ": "
                    + concreteMessage(failure) + '.');
        }
        if (result.accepted()) {
            selectPath(target.path());
        }
        return report(result, targetId, true);
    }

    private Decision evaluateMove(
            WidgetMoveTransfer transfer,
            int sourceActions,
            TreeDropTarget target,
            boolean commit) {
        if (!EventQueue.isDispatchThread()) {
            return Decision.rejected(
                    "Flutter widget moves are accepted only on the EDT.");
        }
        if (closed) {
            moveAdmission.clearPreview();
            return report(Decision.rejected(
                    "Flutter widget-tree drop target is closed."), null, commit);
        }
        if ((sourceActions & DnDConstants.ACTION_MOVE) == 0) {
            moveAdmission.clearPreview();
            return report(Decision.rejected(
                    "Flutter widget reordering requires the MOVE drag action."),
                    null,
                    commit);
        }
        if (transfer == null || !sourceOwner.equals(transfer.owner())) {
            moveAdmission.clearPreview();
            return report(Decision.rejected(
                    "Widget move rejected: the drag does not belong to this Designer tree."),
                    null,
                    commit);
        }
        if (target == null) {
            moveAdmission.clearPreview();
            return report(Decision.rejected(
                    "Widget move rejected: point to an exact widget row or insertion line."),
                    null,
                    commit);
        }

        Preview<MoveWidget> preview;
        try {
            preview = Objects.requireNonNull(
                    moveAdmission.preview(transfer.widgetId(), target),
                    "moveAdmission.preview result");
        } catch (RuntimeException failure) {
            moveAdmission.clearPreview();
            return report(Decision.rejected(
                    "Widget move preview failed for " + transfer.widgetId() + ": "
                    + concreteMessage(failure) + '.'), target.widgetId(), commit);
        }
        if (!preview.accepted()) {
            moveAdmission.clearPreview();
            return report(
                    Decision.rejected(preview.message()), target.widgetId(), commit);
        }
        if (!commit) {
            return report(
                    Decision.accepted(preview.message()), target.widgetId(), false);
        }

        Decision result;
        try {
            result = Objects.requireNonNull(
                    moveAdmission.commit(
                            preview.prepared(), transfer.widgetId(), target),
                    "moveAdmission.commit result");
        } catch (RuntimeException failure) {
            result = Decision.rejected(
                    "Widget move commit failed for " + transfer.widgetId() + ": "
                    + concreteMessage(failure) + '.');
        } finally {
            moveAdmission.clearPreview();
        }
        return report(result, target.widgetId(), true);
    }

    private void selectPath(TreePath path) {
        if (path == null) {
            return;
        }
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    private Decision report(
            Decision decision,
            StableId targetId,
            boolean committed) {
        tree.setToolTipText(decision.message());
        feedback.accept(new Feedback(
                decision.accepted(), committed, targetId, decision.message()));
        return decision;
    }

    private static boolean hasBoundedStringTransfer(Transferable transferable) {
        if (transferable == null) {
            return false;
        }
        try {
            if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            Object data = transferable.getTransferData(DataFlavor.stringFlavor);
            return data instanceof String text
                    && !text.isBlank()
                    && text.length() <= MAX_TRANSFER_TEXT_LENGTH;
        } catch (java.awt.datatransfer.UnsupportedFlavorException
                | java.io.IOException | RuntimeException failure) {
            return false;
        }
    }

    private static TreeDropTarget dropTarget(
            TreePath targetPath,
            int childIndex) {
        if (targetPath == null || targetPath.getLastPathComponent() == null) {
            return null;
        }
        try {
            Node node = Visualizer.findNode(targetPath.getLastPathComponent());
            StableId widgetId = node.getLookup().lookup(StableId.class);
            return widgetId == null
                    ? null
                    : new TreeDropTarget(widgetId, childIndex, targetPath);
        } catch (RuntimeException invalidPath) {
            return null;
        }
    }

    private static DataFlavor widgetMoveFlavor() {
        try {
            return new DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType + ";class="
                    + WidgetMoveTransfer.class.getName(),
                    "Flutter Designer widget move",
                    WidgetMoveTransfer.class.getClassLoader());
        } catch (ClassNotFoundException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String concreteMessage(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
    }

    interface Admission<P> {
        Preview<P> preview(
                Transferable transferable,
                int action,
                StableId targetId);

        Decision commit(
                P prepared,
                Transferable transferable,
                int action,
                StableId targetId);
    }

    interface MoveAdmission {
        boolean canStart(StableId sourceId);

        Preview<MoveWidget> preview(
                StableId sourceId,
                TreeDropTarget target);

        Decision commit(
                MoveWidget prepared,
                StableId sourceId,
                TreeDropTarget target);

        void clearPreview();

        static MoveAdmission disabled() {
            return DisabledMoveAdmission.INSTANCE;
        }
    }

    @FunctionalInterface
    interface DragExporter {
        void export(
                TransferHandler handler,
                JComponent source,
                MouseEvent trigger,
                int action);
    }

    private enum DisabledMoveAdmission implements MoveAdmission {
        INSTANCE;

        @Override
        public boolean canStart(StableId sourceId) {
            return false;
        }

        @Override
        public Preview<MoveWidget> preview(
                StableId sourceId,
                TreeDropTarget target) {
            return Preview.rejected("Widget reordering is unavailable.");
        }

        @Override
        public Decision commit(
                MoveWidget prepared,
                StableId sourceId,
                TreeDropTarget target) {
            return Decision.rejected("Widget reordering is unavailable.");
        }

        @Override
        public void clearPreview() {
            // No preview exists.
        }
    }

    record TreeDropTarget(
            StableId widgetId,
            int childIndex,
            TreePath path) {
        TreeDropTarget {
            Objects.requireNonNull(widgetId, "widgetId");
            Objects.requireNonNull(path, "path");
            if (childIndex < -1) {
                throw new IllegalArgumentException(
                        "childIndex must be -1 for ON or non-negative for INSERT");
            }
        }

        boolean insertion() {
            return childIndex >= 0;
        }
    }

    record Preview<P>(P prepared, String message) {
        Preview {
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }

        boolean accepted() {
            return prepared != null;
        }

        static <P> Preview<P> accepted(P prepared, String message) {
            return new Preview<>(Objects.requireNonNull(prepared, "prepared"), message);
        }

        static <P> Preview<P> rejected(String message) {
            return new Preview<>(null, message);
        }
    }

    record Decision(boolean accepted, String message) {
        Decision {
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }

        static Decision accepted(String message) {
            return new Decision(true, message);
        }

        static Decision rejected(String message) {
            return new Decision(false, message);
        }
    }

    record Feedback(
            boolean accepted,
            boolean committed,
            StableId targetId,
            String message) {
        Feedback {
            Objects.requireNonNull(message, "message");
            if (message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }

    private final class TreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            if (closed || component != tree) {
                return NONE;
            }
            StableId sourceId = dragSourceWidgetId();
            return sourceId != null && moveAdmission.canStart(sourceId)
                    ? MOVE : NONE;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            if (closed || component != tree) {
                return null;
            }
            StableId sourceId = dragSourceWidgetId();
            if (sourceId == null || !moveAdmission.canStart(sourceId)) {
                return null;
            }
            return new WidgetMoveTransferable(
                    new WidgetMoveTransfer(sourceOwner, sourceId));
        }

        @Override
        protected void exportDone(
                JComponent source,
                Transferable data,
                int action) {
            StableId failedSource = dragExportStarted && data == null
                    ? armedDragWidgetId : null;
            resetMoveDragGesture();
            moveAdmission.clearPreview();
            if (failedSource != null && !closed) {
                report(Decision.rejected(
                        "Widget move could not start for " + failedSource
                        + ": the active Designer revision is no longer ready."),
                        failedSource,
                        false);
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (support == null || !support.isDrop()) {
                moveAdmission.clearPreview();
                return false;
            }
            TreeDropTarget target = dropTarget(support);
            Optional<WidgetMoveTransfer> move = moveTransfer(
                    support.getTransferable());
            Decision decision;
            if (move.isPresent()) {
                decision = evaluateMove(
                        move.orElseThrow(),
                        support.getSourceDropActions(),
                        target,
                        false);
            } else {
                moveAdmission.clearPreview();
                decision = evaluatePalette(
                        support.getTransferable(),
                        support.getSourceDropActions(),
                        target,
                        false);
            }
            support.setShowDropLocation(decision.accepted());
            if (decision.accepted()) {
                support.setDropAction(DnDConstants.ACTION_MOVE);
            }
            return decision.accepted();
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support == null || !support.isDrop()) {
                moveAdmission.clearPreview();
                return false;
            }
            TreeDropTarget target = dropTarget(support);
            Optional<WidgetMoveTransfer> move = moveTransfer(
                    support.getTransferable());
            Decision decision = move.isPresent()
                    ? evaluateMove(
                            move.orElseThrow(),
                            support.getSourceDropActions(),
                            target,
                            true)
                    : evaluatePalette(
                            support.getTransferable(),
                            support.getSourceDropActions(),
                            target,
                            true);
            return decision.accepted();
        }

        private TreeDropTarget dropTarget(TransferSupport support) {
            if (support.getComponent() != tree
                    || !(support.getDropLocation()
                            instanceof JTree.DropLocation location)) {
                return null;
            }
            return FlutterDesignerWidgetTreeDropSupport.dropTarget(
                    location.getPath(), location.getChildIndex());
        }

        private StableId selectedWidgetId() {
            TreePath selection = tree.getSelectionPath();
            TreeDropTarget target = FlutterDesignerWidgetTreeDropSupport.dropTarget(
                    selection, -1);
            return target == null ? null : target.widgetId();
        }
        private StableId dragSourceWidgetId() {
            return dragExportStarted && armedDragWidgetId != null
                    ? armedDragWidgetId : selectedWidgetId();
        }
    }

    private final class MoveDragGesture extends MouseInputAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            resetMoveDragGesture();
            if (closed || event == null
                    || !javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                return;
            }
            TreePath sourcePath = tree.getPathForLocation(
                    event.getX(), event.getY());
            TreeDropTarget source = dropTarget(sourcePath, -1);
            if (source == null) {
                return;
            }
            selectPath(sourcePath);
            armedDragOrigin = event.getPoint();
            armedDragPath = sourcePath;
            armedDragWidgetId = source.widgetId();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (closed || event == null || dragExportStarted
                    || armedDragOrigin == null || armedDragPath == null
                    || armedDragWidgetId == null) {
                return;
            }
            if ((event.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0) {
                resetMoveDragGesture();
                moveAdmission.clearPreview();
                return;
            }
            long horizontal = (long) event.getX() - armedDragOrigin.x;
            long vertical = (long) event.getY() - armedDragOrigin.y;
            long threshold = dragThreshold;
            if ((horizontal * horizontal) + (vertical * vertical)
                    < threshold * threshold) {
                return;
            }
            TreeDropTarget selected = dropTarget(tree.getSelectionPath(), -1);
            if (selected == null
                    || !armedDragWidgetId.equals(selected.widgetId())
                    || !armedDragPath.equals(selected.path())
                    || !moveAdmission.canStart(armedDragWidgetId)) {
                StableId rejectedId = armedDragWidgetId;
                resetMoveDragGesture();
                moveAdmission.clearPreview();
                report(Decision.rejected(
                        "Widget move is unavailable for " + rejectedId
                        + ": the selection or active Designer revision changed."),
                        rejectedId,
                        false);
                return;
            }
            dragExportStarted = true;
            try {
                dragExporter.export(
                        handler, tree, event, DnDConstants.ACTION_MOVE);
            } catch (RuntimeException failure) {
                StableId rejectedId = armedDragWidgetId;
                resetMoveDragGesture();
                moveAdmission.clearPreview();
                report(Decision.rejected(
                        "Widget move could not start for " + rejectedId + ": "
                        + concreteMessage(failure) + '.'), rejectedId, false);
            }
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            resetMoveDragGesture();
            moveAdmission.clearPreview();
        }
    }

    private void resetMoveDragGesture() {
        armedDragOrigin = null;
        armedDragPath = null;
        armedDragWidgetId = null;
        dragExportStarted = false;
    }

    private Optional<WidgetMoveTransfer> moveTransfer(Transferable transferable) {
        if (transferable == null
                || !transferable.isDataFlavorSupported(WIDGET_MOVE_FLAVOR)) {
            return Optional.empty();
        }
        try {
            Object value = transferable.getTransferData(WIDGET_MOVE_FLAVOR);
            return value instanceof WidgetMoveTransfer move
                    && sourceOwner.equals(move.owner())
                    ? Optional.of(move) : Optional.empty();
        } catch (UnsupportedFlavorException | IOException | RuntimeException failure) {
            return Optional.empty();
        }
    }

    private record WidgetMoveTransfer(UUID owner, StableId widgetId) {
        private WidgetMoveTransfer {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(widgetId, "widgetId");
        }
    }

    private record WidgetMoveTransferable(WidgetMoveTransfer transfer)
            implements Transferable {
        private static final DataFlavor[] FLAVORS = {WIDGET_MOVE_FLAVOR};

        private WidgetMoveTransferable {
            Objects.requireNonNull(transfer, "transfer");
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return WIDGET_MOVE_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return transfer;
        }
    }
}

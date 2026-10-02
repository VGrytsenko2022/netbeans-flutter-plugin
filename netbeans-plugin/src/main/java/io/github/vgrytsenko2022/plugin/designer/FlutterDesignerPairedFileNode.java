package io.github.vgrytsenko2022.plugin.designer;

import java.awt.EventQueue;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.nodes.Sheet;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.Lookup;

/** Data node that keeps every filename mutation on the pair-aware path. */
final class FlutterDesignerPairedFileNode extends DataNode {
    // NetBeans DataNode keeps this key private. Its default writable property
    // directly renames one FileObject extension and bypasses handleRename.
    private static final String EXTENSION_PROPERTY = "extension";
    private static final DataFlavor CUT_SESSION_FLAVOR = new DataFlavor(
            CutSession.class, "Flutter Designer pair cut session");
    private static final PairCopyOperations DEFAULT_PAIR_COPY =
            new PairCopyOperations() {
                @Override
                public boolean isAllowed(FileObject member) {
                    return FlutterDesignerPairCopy.isAllowed(member);
                }

                @Override
                public boolean isPasteTarget(
                        FileObject member,
                        FileObject targetFolder) {
                    return FlutterDesignerPairCopy.isPasteTarget(
                            member, targetFolder);
                }

                @Override
                public void copy(
                        FileObject member,
                        FileObject targetFolder) throws IOException {
                    FlutterDesignerPairCopy.copy(member, targetFolder);
                }
            };
    private static final PairMoveOperations DEFAULT_PAIR_MOVE =
            new PairMoveOperations() {
                @Override
                public boolean isAllowed(FileObject member) {
                    return FlutterDesignerPairMove.isAllowed(member);
                }

                @Override
                public boolean isPasteTarget(
                        FileObject member,
                        FileObject targetFolder) {
                    return FlutterDesignerPairMove.isPasteTarget(
                            member, targetFolder);
                }

                @Override
                public void move(
                        FileObject member,
                        FileObject targetFolder) throws IOException {
                    FlutterDesignerPairMove.move(member, targetFolder);
                }
            };

    private final PairCopyOperations pairCopy;
    private final PairMoveOperations pairMove;

    FlutterDesignerPairedFileNode(DataObject dataObject, String iconPath) {
        this(dataObject, iconPath, DEFAULT_PAIR_COPY, DEFAULT_PAIR_MOVE);
    }

    FlutterDesignerPairedFileNode(
            DataObject dataObject,
            String iconPath,
            PairCopyOperations pairCopy) {
        this(dataObject, iconPath, pairCopy, DEFAULT_PAIR_MOVE);
    }

    FlutterDesignerPairedFileNode(
            DataObject dataObject,
            String iconPath,
            PairCopyOperations pairCopy,
            PairMoveOperations pairMove) {
        super(Objects.requireNonNull(dataObject, "dataObject"), Children.LEAF);
        setIconBaseWithExtension(Objects.requireNonNull(iconPath, "iconPath"));
        this.pairCopy = Objects.requireNonNull(pairCopy, "pairCopy");
        this.pairMove = Objects.requireNonNull(pairMove, "pairMove");
    }

    @Override
    public boolean canCopy() {
        return pairCopy.isAllowed(getDataObject().getPrimaryFile());
    }

    @Override
    public boolean canCut() {
        return pairMove.isAllowed(getDataObject().getPrimaryFile());
    }

    /**
     * NetBeans' DataObject rename contract is synchronous. The dedicated shell
     * therefore closes first and invokes the real DataObject operation only
     * from its exact continuation; ordinary MultiView keeps the stock path.
     */
    @Override
    public void setName(String name) {
        if (!FlutterDesignerPairPathCommands.requiresPostClose(getDataObject())) {
            super.setName(name);
            return;
        }
        try {
            observe("Rename Flutter Designer form",
                    getDataObject().getPrimaryFile().getPath(),
                    FlutterDesignerPairPathCommands.rename(
                            getDataObject(),
                            name,
                            () -> {
                                // Node.setName() intentionally reports loader
                                // failures through the UI instead of throwing
                                // them. The post-close command needs the real
                                // synchronous DataObject contract so its
                                // outcome cannot claim success after a failed
                                // pair rename.
                                getDataObject().rename(name);
                                FileObject renamed =
                                        getDataObject().getPrimaryFile();
                                if (!name.equals(renamed.getName())) {
                                    throw new IOException(
                                            "the paired DataObject did not "
                                            + "commit the requested target stem");
                                }
                                return renamed;
                            }));
        } catch (IOException | RuntimeException failure) {
            reportFailure(
                    "Rename Flutter Designer form",
                    getDataObject().getPrimaryFile().getPath(),
                    reason(failure));
        }
    }

    /** Same asynchronous command boundary as rename, with subtree identity intact. */
    @Override
    public void destroy() throws IOException {
        if (!FlutterDesignerPairPathCommands.requiresPostClose(getDataObject())) {
            super.destroy();
            return;
        }
        observe("Delete Flutter Designer form",
                getDataObject().getPrimaryFile().getPath(),
                FlutterDesignerPairPathCommands.delete(
                        getDataObject(),
                        () -> {
                            FlutterDesignerPairedFileNode.super.destroy();
                            return null;
                        }));
    }

    /**
     * Exposes only NetBeans' node and pair-aware paste flavors. In particular,
     * the standard DataNode LoaderTransfer and operating-system file-list
     * flavors would copy only the visible member and must not escape.
     */
    @Override
    public Transferable clipboardCopy() throws IOException {
        FileObject member = getDataObject().getPrimaryFile();
        if (!pairCopy.isAllowed(member)) {
            throw new IOException("Cannot copy Flutter Designer form "
                    + member.getNameExt()
                    + ". Reason: the mirrored pair is not currently copyable.");
        }

        ExTransferable transferable = ExTransferable.create(
                NodeTransfer.transferable(
                        this, NodeTransfer.CLIPBOARD_COPY));
        transferable.put(NodeTransfer.createPaste(
                target -> pasteTypes(member, target, pairCopy)));
        return transferable;
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        FileObject member = getDataObject().getPrimaryFile();
        if (!pairMove.isAllowed(member)) {
            throw new IOException("Cannot move Flutter Designer form "
                    + member.getNameExt()
                    + ". Reason: the mirrored pair is not currently movable.");
        }

        CutSession cutSession = new CutSession();
        ExTransferable transferable = ExTransferable.create(
                NodeTransfer.transferable(
                        this, NodeTransfer.CLIPBOARD_CUT));
        transferable.put(new ExTransferable.Single(CUT_SESSION_FLAVOR) {
            @Override
            protected Object getData() {
                return cutSession;
            }
        });
        transferable.put(NodeTransfer.createPaste(
                target -> movePasteTypes(
                        member, target, pairMove, cutSession)));
        return transferable;
    }

    private static PasteType[] movePasteTypes(
            FileObject member,
            Node target,
            PairMoveOperations pairMove,
            CutSession cutSession) {
        // DataNode already forwards file-node paste queries to its parent.
        // Accepting a file's parent here as well would publish duplicate Move
        // PasteTypes in the real Explorer integration.
        FileObject folder = target == null ? null : folderFromLookup(target);
        if (folder == null || !pairMove.isPasteTarget(member, folder)) {
            return new PasteType[0];
        }
        return new PasteType[]{new PairMovePasteType(
                member, folder, pairMove, cutSession)};
    }

    private static PasteType[] pasteTypes(
            FileObject member,
            Node target,
            PairCopyOperations pairCopy) {
        FileObject folder = resolvePasteFolder(target);
        if (folder == null
                || !pairCopy.isPasteTarget(member, folder)) {
            return new PasteType[0];
        }
        return new PasteType[]{new PairCopyPasteType(
                member, folder, pairCopy)};
    }

    /**
     * NetBeans may ask the paste provider about either a folder node or a file
     * node whose parent is the intended destination.
     */
    static FileObject resolvePasteFolder(Node target) {
        if (target == null) {
            return null;
        }
        FileObject direct = folderFromLookup(target);
        if (direct != null) {
            return direct;
        }
        Node parent = target.getParentNode();
        return parent == null ? null : folderFromLookup(parent);
    }

    private static FileObject folderFromLookup(Node node) {
        Collection<? extends FileObject> files = node.getLookup()
                .lookupAll(FileObject.class);
        if (files.size() == 1) {
            FileObject file = files.iterator().next();
            if (file.isFolder()) {
                return file;
            }
        }

        Collection<? extends DataObject> objects = node.getLookup()
                .lookupAll(DataObject.class);
        if (objects.size() == 1) {
            FileObject file = objects.iterator().next().getPrimaryFile();
            if (file.isFolder()) {
                return file;
            }
        }
        return null;
    }

    private static final class PairCopyPasteType extends PasteType {
        private final FileObject member;
        private final FileObject targetFolder;
        private final PairCopyOperations pairCopy;

        PairCopyPasteType(
                FileObject member,
                FileObject targetFolder,
                PairCopyOperations pairCopy) {
            this.member = member;
            this.targetFolder = targetFolder;
            this.pairCopy = pairCopy;
        }

        @Override
        public String getName() {
            return "Copy Flutter Designer Form";
        }

        @Override
        public Transferable paste() throws IOException {
            pairCopy.copy(member, targetFolder);
            return null;
        }
    }

    private static final class PairMovePasteType extends PasteType {
        private final FileObject member;
        private final FileObject targetFolder;
        private final PairMoveOperations pairMove;
        private final CutSession cutSession;

        PairMovePasteType(
                FileObject member,
                FileObject targetFolder,
                PairMoveOperations pairMove,
                CutSession cutSession) {
            this.member = member;
            this.targetFolder = targetFolder;
            this.pairMove = pairMove;
            this.cutSession = cutSession;
        }

        @Override
        public String getName() {
            return "Move Flutter Designer Form";
        }

        @Override
        public Transferable paste() throws IOException {
            if (pairMove == DEFAULT_PAIR_MOVE) {
                if (!cutSession.begin()) {
                    throw new IOException(
                            "Move Flutter Designer form is already waiting for "
                            + "its editor close or filesystem transaction");
                }
                if (!FlutterDesignerPairPathCommands.requiresPostClose(member)) {
                    try {
                        pairMove.move(member, targetFolder);
                        cutSession.commit();
                        clearClipboardIfCurrent(cutSession);
                        // We already cleared this exact CutSession. Returning
                        // EMPTY would let NetBeans clear a newer clipboard
                        // value installed between that identity check and the
                        // PasteType return.
                        return null;
                    } catch (IOException | RuntimeException failure) {
                        cutSession.retry();
                        throw failure;
                    }
                }
                CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<
                        FlutterDesignerPairMove.MoveResult>> stage;
                try {
                    stage = FlutterDesignerPairPathCommands.move(
                            member, targetFolder);
                } catch (IOException | RuntimeException failure) {
                    cutSession.retry();
                    throw failure;
                }
                stage.whenComplete((outcome, failure) -> {
                    if (failure != null) {
                        cutSession.retry();
                        reportFailure(
                                "Move Flutter Designer form",
                                member.getPath() + " -> " + targetFolder.getPath(),
                                reason(failure));
                    } else if (outcome != null && outcome.succeeded()) {
                        cutSession.commit();
                        clearClipboardIfCurrent(cutSession);
                    } else {
                        cutSession.retry();
                        reportOutcome(
                                "Move Flutter Designer form",
                                member.getPath() + " -> " + targetFolder.getPath(),
                                outcome);
                    }
                });
                // PasteType has no PENDING value. null is its documented
                // clipboard-preserving result; the exact CutSession clears the
                // clipboard later only after a committed Move.
                return null;
            }
            pairMove.move(member, targetFolder);
            // PasteType's null result preserves the clipboard.  A successful
            // Cut must clear its one-shot transferable instead.
            return ExTransferable.EMPTY;
        }
    }

    private static <T> void observe(
            String operation,
            String target,
            CompletionStage<FlutterDesignerEditorSupport.PostCloseOutcome<T>>
                    stage) {
        stage.whenComplete((outcome, failure) -> {
            if (failure != null) {
                reportFailure(operation, target, reason(failure));
            } else {
                reportOutcome(operation, target, outcome);
            }
        });
    }

    private static void reportOutcome(
            String operation,
            String target,
            FlutterDesignerEditorSupport.PostCloseOutcome<?> outcome) {
        if (outcome == null || outcome.succeeded()
                || outcome.status()
                        == FlutterDesignerEditorSupport.PostCloseStatus.CANCELLED) {
            return;
        }
        reportFailure(operation, target, outcome.reason());
    }

    private static void reportFailure(
            String operation,
            String target,
            String reason) {
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                "Operation: " + operation + ". Target: " + target
                + ". Reason: " + reason + ".",
                NotifyDescriptor.ERROR_MESSAGE));
    }

    private static String reason(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private static void clearClipboardIfCurrent(CutSession expected) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> clearClipboardIfCurrent(expected));
            return;
        }
        Clipboard clipboard = Lookup.getDefault().lookup(Clipboard.class);
        if (clipboard == null) {
            return;
        }
        try {
            // Clipboard#getContents and setContents are individually
            // synchronized, but the identity check and replacement form one
            // no-clobber transaction.  Use the Clipboard monitor as the
            // compound boundary and the EDT as the single IDE clipboard
            // mutation lane, so a newer in-process Copy/Cut cannot be erased
            // between the check and the clear.
            synchronized (clipboard) {
                Transferable current = clipboard.getContents(null);
                if (current != null
                        && current.isDataFlavorSupported(CUT_SESSION_FLAVOR)
                        && current.getTransferData(CUT_SESSION_FLAVOR)
                                == expected) {
                    clipboard.setContents(ExTransferable.EMPTY, null);
                }
            }
        } catch (Exception failure) {
            reportFailure(
                    "Clear committed Flutter Designer Cut",
                    "system clipboard",
                    reason(failure));
        }
    }

    private static final class CutSession {
        private boolean active;
        private boolean committed;

        synchronized boolean begin() {
            if (active || committed) {
                return false;
            }
            active = true;
            return true;
        }

        synchronized void retry() {
            if (!committed) {
                active = false;
            }
        }

        synchronized void commit() {
            committed = true;
            active = false;
        }
    }

    interface PairCopyOperations {
        boolean isAllowed(FileObject member);

        boolean isPasteTarget(FileObject member, FileObject targetFolder);

        void copy(FileObject member, FileObject targetFolder)
                throws IOException;
    }

    interface PairMoveOperations {
        boolean isAllowed(FileObject member);

        boolean isPasteTarget(FileObject member, FileObject targetFolder);

        void move(FileObject member, FileObject targetFolder)
                throws IOException;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set properties = sheet.get(Sheet.PROPERTIES);
        if (properties != null) {
            properties.remove(EXTENSION_PROPERTY);
        }
        return sheet;
    }
}

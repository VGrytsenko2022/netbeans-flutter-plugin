package dev.flutter.netbeans.plugin.designer;

import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.nodes.Sheet;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;

/** Data node that keeps every filename mutation on the pair-aware path. */
final class FlutterDesignerPairedFileNode extends DataNode {
    // NetBeans DataNode keeps this key private. Its default writable property
    // directly renames one FileObject extension and bypasses handleRename.
    private static final String EXTENSION_PROPERTY = "extension";
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

        ExTransferable transferable = ExTransferable.create(
                NodeTransfer.transferable(
                        this, NodeTransfer.CLIPBOARD_CUT));
        transferable.put(NodeTransfer.createPaste(
                target -> movePasteTypes(member, target, pairMove)));
        return transferable;
    }

    private static PasteType[] movePasteTypes(
            FileObject member,
            Node target,
            PairMoveOperations pairMove) {
        // DataNode already forwards file-node paste queries to its parent.
        // Accepting a file's parent here as well would publish duplicate Move
        // PasteTypes in the real Explorer integration.
        FileObject folder = target == null ? null : folderFromLookup(target);
        if (folder == null || !pairMove.isPasteTarget(member, folder)) {
            return new PasteType[0];
        }
        return new PasteType[]{new PairMovePasteType(
                member, folder, pairMove)};
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

        PairMovePasteType(
                FileObject member,
                FileObject targetFolder,
                PairMoveOperations pairMove) {
            this.member = member;
            this.targetFolder = targetFolder;
            this.pairMove = pairMove;
        }

        @Override
        public String getName() {
            return "Move Flutter Designer Form";
        }

        @Override
        public Transferable paste() throws IOException {
            pairMove.move(member, targetFolder);
            // PasteType's null result preserves the clipboard.  A successful
            // Cut must clear its one-shot transferable instead.
            return ExTransferable.EMPTY;
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

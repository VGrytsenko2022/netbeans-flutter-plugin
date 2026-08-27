package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.plugin.ui.FlutterFileIcons;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.LoaderTransfer;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;

/** Clipboard contract for a visible pair-aware Dart or FD node. */
class FlutterDesignerPairedFileNodeTest {
    private static final DataFlavor URI_LIST_FLAVOR = uriListFlavor();

    @TempDir
    Path temporaryDirectory;

    @Test
    void copyTransferContainsOnlyNodeAndPairAwarePasteFlavors()
            throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairCopy pairCopy = new RecordingPairCopy(
                fixture.sourceFolder());
        Node node = node(fixture.member(), pairCopy);

        assertTrue(node.canCopy());
        assertFalse(node.canCut());

        Transferable transferable = node.clipboardCopy();

        assertSame(node, NodeTransfer.node(
                transferable, NodeTransfer.CLIPBOARD_COPY));
        assertNotNull(NodeTransfer.findPaste(transferable));
        assertNull(LoaderTransfer.getDataObject(
                transferable, LoaderTransfer.CLIPBOARD_COPY));
        assertFalse(transferable.isDataFlavorSupported(
                DataFlavor.javaFileListFlavor));
        assertFalse(transferable.isDataFlavorSupported(URI_LIST_FLAVOR));
        assertThrows(IOException.class, node::clipboardCut);
        assertFalse(hasProperty(node, "extension"));
    }

    @Test
    void pairPasteTypeUsesTheResolvedFolderAndPreservesClipboard()
            throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairCopy pairCopy = new RecordingPairCopy(
                fixture.sourceFolder());
        Node source = node(fixture.member(), pairCopy);
        NodeTransfer.Paste provider = NodeTransfer.findPaste(
                source.clipboardCopy());
        assertNotNull(provider);

        Node target = DataFolder.findFolder(fixture.sourceFolder())
                .getNodeDelegate();
        PasteType[] types = provider.types(target);
        assertEquals(1, types.length);
        assertEquals("Copy Flutter Designer Form", types[0].getName());

        assertNull(types[0].paste());
        assertEquals(1, pairCopy.copyCalls);
        assertSame(fixture.member(), pairCopy.copiedMember);
        assertSame(fixture.sourceFolder(), pairCopy.copiedTarget);
    }

    @Test
    void pasteResolvesAFileParentAndRejectsAnInvalidFolder()
            throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairCopy pairCopy = new RecordingPairCopy(
                fixture.sourceFolder());
        Node source = node(fixture.member(), pairCopy);
        NodeTransfer.Paste provider = NodeTransfer.findPaste(
                source.clipboardCopy());
        assertNotNull(provider);

        Node folderNode = DataFolder.findFolder(fixture.sourceFolder())
                .getNodeDelegate();
        Node fileNode = Arrays.stream(
                        folderNode.getChildren().getNodes(true))
                .filter(node -> fixture.member().equals(
                        node.getLookup().lookup(FileObject.class)))
                .findFirst()
                .orElseThrow();
        assertSame(fixture.sourceFolder(),
                FlutterDesignerPairedFileNode.resolvePasteFolder(fileNode));
        assertEquals(1, provider.types(fileNode).length);

        Node invalid = DataFolder.findFolder(fixture.invalidFolder())
                .getNodeDelegate();
        assertEquals(0, provider.types(invalid).length);
    }

    @Test
    void disabledPairCannotPublishAClipboardTransfer() throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairCopy pairCopy = new RecordingPairCopy(
                fixture.sourceFolder());
        pairCopy.allowed = false;
        Node node = node(fixture.member(), pairCopy);

        assertFalse(node.canCopy());
        IOException failure = assertThrows(
                IOException.class, node::clipboardCopy);
        assertTrue(failure.getMessage().contains("home.dart"));
    }

    @Test
    void cutTransferContainsOnlyCutNodeAndPairMovePasteFlavor()
            throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairCopy pairCopy = new RecordingPairCopy(
                fixture.sourceFolder());
        RecordingPairMove pairMove = new RecordingPairMove(
                fixture.invalidFolder());
        Node node = node(fixture.member(), pairCopy, pairMove);

        assertTrue(node.canCut());
        Transferable transferable = node.clipboardCut();

        assertSame(node, NodeTransfer.node(
                transferable, NodeTransfer.CLIPBOARD_CUT));
        assertNull(NodeTransfer.node(
                transferable, NodeTransfer.CLIPBOARD_COPY));
        assertNotNull(NodeTransfer.findPaste(transferable));
        assertNull(LoaderTransfer.getDataObject(
                transferable, LoaderTransfer.CLIPBOARD_CUT));
        assertNull(LoaderTransfer.getDataObject(
                transferable, LoaderTransfer.CLIPBOARD_COPY));
        assertFalse(transferable.isDataFlavorSupported(
                DataFlavor.javaFileListFlavor));
        assertFalse(transferable.isDataFlavorSupported(URI_LIST_FLAVOR));
    }

    @Test
    void successfulMovePasteClearsClipboardAndUsesDirectFolderOnly()
            throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairMove pairMove = new RecordingPairMove(
                fixture.invalidFolder());
        Node source = node(
                fixture.member(),
                new RecordingPairCopy(fixture.sourceFolder()),
                pairMove);
        NodeTransfer.Paste provider = NodeTransfer.findPaste(
                source.clipboardCut());
        assertNotNull(provider);

        Node folderNode = DataFolder.findFolder(fixture.invalidFolder())
                .getNodeDelegate();
        PasteType[] types = provider.types(folderNode);
        assertEquals(1, types.length);
        assertEquals("Move Flutter Designer Form", types[0].getName());
        assertSame(ExTransferable.EMPTY, types[0].paste());
        assertEquals(1, pairMove.moveCalls);
        assertSame(fixture.member(), pairMove.movedMember);
        assertSame(fixture.invalidFolder(), pairMove.movedTarget);

        Node sourceFolder = DataFolder.findFolder(fixture.sourceFolder())
                .getNodeDelegate();
        Node fileNode = Arrays.stream(
                        sourceFolder.getChildren().getNodes(true))
                .filter(candidate -> fixture.member().equals(
                        candidate.getLookup().lookup(FileObject.class)))
                .findFirst()
                .orElseThrow();
        assertEquals(0, provider.types(fileNode).length,
                "the provider must not duplicate DataNode parent forwarding");
    }

    @Test
    void disabledPairCannotPublishACutTransfer() throws Exception {
        NodeFixture fixture = fixture();
        RecordingPairMove pairMove = new RecordingPairMove(
                fixture.invalidFolder());
        pairMove.allowed = false;
        Node node = node(
                fixture.member(),
                new RecordingPairCopy(fixture.sourceFolder()),
                pairMove);

        assertFalse(node.canCut());
        IOException failure = assertThrows(
                IOException.class, node::clipboardCut);
        assertTrue(failure.getMessage().contains("home.dart"));
    }

    private NodeFixture fixture() throws Exception {
        Path sourcePath = temporaryDirectory.resolve("source");
        Path invalidPath = temporaryDirectory.resolve("invalid");
        Files.createDirectories(sourcePath);
        Files.createDirectories(invalidPath);
        Path memberPath = sourcePath.resolve("home.dart");
        Files.writeString(memberPath, "class Home {}\n");
        FileUtil.refreshFor(temporaryDirectory.toFile());

        FileObject member = FileUtil.toFileObject(memberPath.toFile());
        FileObject sourceFolder = FileUtil.toFileObject(sourcePath.toFile());
        FileObject invalidFolder = FileUtil.toFileObject(invalidPath.toFile());
        assertNotNull(member);
        assertNotNull(sourceFolder);
        assertNotNull(invalidFolder);
        return new NodeFixture(member, sourceFolder, invalidFolder);
    }

    private static Node node(
            FileObject member,
            FlutterDesignerPairedFileNode.PairCopyOperations pairCopy)
            throws Exception {
        return node(member, pairCopy, new RecordingPairMove(null, false));
    }

    private static Node node(
            FileObject member,
            FlutterDesignerPairedFileNode.PairCopyOperations pairCopy,
            FlutterDesignerPairedFileNode.PairMoveOperations pairMove)
            throws Exception {
        return new FlutterDesignerPairedFileNode(
                DataObject.find(member),
                FlutterFileIcons.DART_FILE_ICON_PATH,
                pairCopy,
                pairMove);
    }

    private static boolean hasProperty(Node node, String name) {
        return Arrays.stream(node.getPropertySets())
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .anyMatch(property -> property.getName().equals(name));
    }

    private static DataFlavor uriListFlavor() {
        try {
            return new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private record NodeFixture(
            FileObject member,
            FileObject sourceFolder,
            FileObject invalidFolder) {
    }

    private static final class RecordingPairCopy
            implements FlutterDesignerPairedFileNode.PairCopyOperations {
        private final FileObject allowedTarget;
        private boolean allowed = true;
        private int copyCalls;
        private FileObject copiedMember;
        private FileObject copiedTarget;

        RecordingPairCopy(FileObject allowedTarget) {
            this.allowedTarget = allowedTarget;
        }

        @Override
        public boolean isAllowed(FileObject member) {
            return allowed;
        }

        @Override
        public boolean isPasteTarget(
                FileObject member,
                FileObject targetFolder) {
            return allowed && targetFolder.equals(allowedTarget);
        }

        @Override
        public void copy(
                FileObject member,
                FileObject targetFolder) {
            copyCalls++;
            copiedMember = member;
            copiedTarget = targetFolder;
        }
    }

    private static final class RecordingPairMove
            implements FlutterDesignerPairedFileNode.PairMoveOperations {
        private final FileObject allowedTarget;
        private boolean allowed;
        private int moveCalls;
        private FileObject movedMember;
        private FileObject movedTarget;

        RecordingPairMove(FileObject allowedTarget) {
            this(allowedTarget, true);
        }

        RecordingPairMove(FileObject allowedTarget, boolean allowed) {
            this.allowedTarget = allowedTarget;
            this.allowed = allowed;
        }

        @Override
        public boolean isAllowed(FileObject member) {
            return allowed;
        }

        @Override
        public boolean isPasteTarget(
                FileObject member,
                FileObject targetFolder) {
            return allowed && targetFolder.equals(allowedTarget);
        }

        @Override
        public void move(FileObject member, FileObject targetFolder) {
            moveCalls++;
            movedMember = member;
            movedTarget = targetFolder;
        }
    }
}

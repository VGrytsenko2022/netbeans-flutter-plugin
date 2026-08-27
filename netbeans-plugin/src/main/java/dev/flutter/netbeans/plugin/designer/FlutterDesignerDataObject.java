package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.ui.FlutterFileIcons;
import java.io.IOException;
import java.util.Objects;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/** One Dart editing session coordinating its mirrored, separately visible FD model. */
public final class FlutterDesignerDataObject extends MultiDataObject {
    private final FlutterProject project;
    private final FileObject modelFile;
    private final FlutterDesignerDocumentController documentController;
    private final FlutterDesignerEditorSupport editorSupport;
    private final DesignerCombinedUndoRedo combinedUndoRedo;
    private final PairSaveCoordinator pairSaveCoordinator;

    public FlutterDesignerDataObject(FileObject primaryDart, MultiFileLoader loader)
            throws DataObjectExistsException, IOException {
        this(primaryDart, loader, resolvePair(primaryDart));
    }

    /** Test seam for the lightweight unit container, which has no project owner query. */
    FlutterDesignerDataObject(
            FileObject primaryDart,
            MultiFileLoader loader,
            FlutterProject project)
            throws DataObjectExistsException, IOException {
        this(primaryDart, loader, resolvePair(primaryDart, project));
    }

    private FlutterDesignerDataObject(
            FileObject primaryDart,
            MultiFileLoader loader,
            FlutterDesignerPairLayout.Pair pair)
            throws DataObjectExistsException, IOException {
        super(primaryDart, loader);
        // Do not register the model as a MultiDataObject secondary entry.
        // FolderChildren intentionally suppresses secondary entries, which
        // made .fd_templates look empty in NetBeans' physical Files view.
        FlutterDesignerPairLayout.Pair resolvedPair = Objects.requireNonNull(
                pair, "pair");
        project = resolvedPair.project();
        modelFile = resolvedPair.modelFile();
        documentController = new FlutterDesignerDocumentController(
                primaryDart, modelFile);
        getCookieSet().add(documentController);

        editorSupport = new FlutterDesignerEditorSupport(
                this,
                getPrimaryEntry(),
                getCookieSet());
        combinedUndoRedo = new DesignerCombinedUndoRedo(
                editorSupport.nativeUndoRedoManagerForCombinedBridge());
        editorSupport.bindCombinedUndoRedo(combinedUndoRedo);
        pairSaveCoordinator = new PairSaveCoordinator(
                this,
                editorSupport,
                documentController,
                primaryDart,
                modelFile,
                getCookieSet());
        editorSupport.bindPairSaveCoordinator(pairSaveCoordinator);
        documentController.bindPairSaveCoordinator(pairSaveCoordinator);
        getCookieSet().add((Node.Cookie) editorSupport);
        getCookieSet().add(pairSaveCoordinator);
    }

    private static FlutterDesignerPairLayout.Pair resolvePair(FileObject primaryDart)
            throws IOException {
        return FlutterDesignerPairLayout.findCompletePair(primaryDart)
                .orElseThrow(() -> missingModel(primaryDart));
    }

    private static FlutterDesignerPairLayout.Pair resolvePair(
            FileObject primaryDart,
            FlutterProject project) throws IOException {
        return FlutterDesignerPairLayout.findCompletePair(primaryDart, project)
                .orElseThrow(() -> missingModel(primaryDart));
    }

    private static IOException missingModel(FileObject primaryDart) {
        return new IOException("Missing mirrored Flutter Designer model under "
                + FlutterDesignerPairLayout.MODEL_ROOT + " for "
                + primaryDart.getPath());
    }

    public FileObject getModelFile() {
        return modelFile;
    }

    FlutterProject getProject() {
        return project;
    }

    public FlutterDesignerDocumentController getDocumentController() {
        return documentController;
    }

    FlutterDesignerEditorSupport getEditorSupport() {
        return editorSupport;
    }

    DesignerCombinedUndoRedo getCombinedUndoRedo() {
        return combinedUndoRedo;
    }

    PairSaveCoordinator getPairSaveCoordinator() {
        return pairSaveCoordinator;
    }

    @Override
    protected Node createNodeDelegate() {
        return new FlutterDesignerPairedFileNode(
                this, FlutterFileIcons.DART_FILE_ICON_PATH);
    }

    /**
     * Rename must also update source.dartFile and is enabled only when that
     * operation can be performed transactionally for the complete pair.
     */
    @Override
    public boolean isRenameAllowed() {
        return FlutterDesignerPairRename.isAllowed(getPrimaryFile());
    }

    @Override
    protected FileObject handleRename(String name) throws IOException {
        return FlutterDesignerPairRename.rename(getPrimaryFile(), name);
    }

    /**
     * Pair Copy/Paste is exposed by the node-level paste provider. Keeping the
     * generic DataObject capability disabled prevents LoaderTransfer from
     * copying only this technical Dart primary.
     */
    @Override
    public boolean isCopyAllowed() {
        return false;
    }

    @Override
    protected DataObject handleCopy(DataFolder folder) throws IOException {
        throw new IOException(
                "Generic DataObject Copy is unavailable for a Flutter Designer "
                + "pair; use the pair-aware Node Copy/Paste action.");
    }

    /**
     * Pair Cut/Move is exposed only by the custom node transfer.  Keeping the
     * generic capability disabled prevents LoaderTransfer from moving this
     * technical Dart primary without its mirrored model.
     */
    @Override
    public boolean isMoveAllowed() {
        return false;
    }

    @Override
    protected FileObject handleMove(DataFolder folder) throws IOException {
        throw new IOException(
                "Generic DataObject Move is unavailable for a Flutter Designer "
                + "pair; use the pair-aware Node Cut/Paste action.");
    }

    /** Delete keeps the mirrored form atomic even though the model is a visible DataObject. */
    @Override
    public boolean isDeleteAllowed() {
        return FlutterDesignerPairDelete.isAllowed(getPrimaryFile());
    }

    @Override
    protected void handleDelete() throws IOException {
        FlutterDesignerPairDelete.delete(getPrimaryFile());
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

}

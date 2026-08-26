package dev.flutter.netbeans.plugin.designer;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/** One NetBeans data object owning a Dart source and its secondary FD model. */
public final class FlutterDesignerDataObject extends MultiDataObject {
    private final Entry modelEntry;
    private final FlutterDesignerDocumentController documentController;
    private final FlutterDesignerEditorSupport editorSupport;
    private final DesignerCombinedUndoRedo combinedUndoRedo;
    private final PairSaveCoordinator pairSaveCoordinator;

    public FlutterDesignerDataObject(FileObject primaryDart, MultiFileLoader loader)
            throws DataObjectExistsException, IOException {
        super(primaryDart, loader);
        FileObject model = FileUtil.findBrother(
                primaryDart,
                FlutterDesignerMime.MODEL_EXTENSION);
        if (model == null) {
            throw new IOException("Missing Flutter designer model for " + primaryDart.getPath());
        }
        modelEntry = registerEntry(model);
        documentController = new FlutterDesignerDocumentController(
                primaryDart, modelEntry.getFile());
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
                modelEntry.getFile(),
                getCookieSet());
        editorSupport.bindPairSaveCoordinator(pairSaveCoordinator);
        documentController.bindPairSaveCoordinator(pairSaveCoordinator);
        getCookieSet().add((Node.Cookie) editorSupport);
        getCookieSet().add(pairSaveCoordinator);
    }

    public FileObject getModelFile() {
        return modelEntry.getFile();
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

    /**
     * Rename must also update source.dartFile and is enabled only when that
     * operation can be performed transactionally for the complete pair.
     */
    @Override
    public boolean isRenameAllowed() {
        return false;
    }

    /** Copy can choose a collision suffix, so the FD source reference must be updated first. */
    @Override
    public boolean isCopyAllowed() {
        return false;
    }

    /** Move can also choose a collision suffix and is withheld for the same reason as Copy. */
    @Override
    public boolean isMoveAllowed() {
        return false;
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}

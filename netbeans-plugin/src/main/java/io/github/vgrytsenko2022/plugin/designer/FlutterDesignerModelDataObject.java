package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.plugin.ui.FlutterFileIcons;
import java.beans.PropertyVetoException;
import java.io.IOException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.DataFolder;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/**
 * Visible physical representation of one {@code .fd} model.
 *
 * <p>The Dart-side {@link FlutterDesignerDataObject} owns the only live editor
 * document and the atomic pair-save coordinator. This model object therefore
 * never opens a second editor: its Open action resolves the mirrored Dart file
 * and delegates to that single designer session.</p>
 */
@DataObject.Registration(
        mimeType = FlutterDesignerMime.MIME_TYPE,
        displayName = "Flutter Designer Form",
        iconBase = FlutterFileIcons.DESIGNER_FILE_ICON_PATH,
        position = 100)
public final class FlutterDesignerModelDataObject extends MultiDataObject {

    public FlutterDesignerModelDataObject(
            FileObject modelFile,
            MultiFileLoader loader) throws DataObjectExistsException {
        super(modelFile, loader);
        getCookieSet().add((OpenCookie) this::openDesigner);
        // Preserve the established late-pair behavior: if an ordinary Dart
        // DataObject was cached before its .fd model appeared, an unmodified
        // instance can be safely re-recognized by the pair-aware Dart loader.
        // A modified editor may veto invalidation and is never disturbed.
        tryPromotePairedDartDataObject();
    }

    private void openDesigner() {
        try {
            DataObject sourceObject = pairedDesignerDataObject();
            OpenCookie open = sourceObject.getLookup().lookup(OpenCookie.class);
            if (open == null) {
                throw new IOException("The paired Dart designer editor is unavailable.");
            }
            open.open();
        } catch (IOException | RuntimeException failure) {
            String reason = failure.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = failure.getClass().getSimpleName();
            }
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                    "Cannot open Flutter Designer form "
                    + getPrimaryFile().getNameExt() + ". Reason: " + reason,
                    NotifyDescriptor.ERROR_MESSAGE));
        }
    }

    private void tryPromotePairedDartDataObject() {
        try {
            promotePairedDartDataObject(false);
        } catch (IOException ignored) {
            // Physical visibility of the .fd model must not depend on whether
            // its editor can already be resolved. Open reports a concrete
            // error and retries after project recognition has settled.
        }
    }

    private DataObject pairedDesignerDataObject() throws IOException {
        DataObject sourceObject = promotePairedDartDataObject(true);
        if (!(sourceObject instanceof FlutterDesignerDataObject)) {
            throw new IOException(
                    "The paired Dart source has unsaved edits in its ordinary editor. "
                    + "Save or close that editor, then open the .fd form again.");
        }
        return sourceObject;
    }

    private DataObject promotePairedDartDataObject(boolean requirePair)
            throws IOException {
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(getPrimaryFile())
                .orElse(null);
        if (pair == null) {
            if (requirePair) {
                throw new IOException(
                        "The mirrored Dart source under lib is missing or unsafe.");
            }
            return null;
        }
        DataObject sourceObject = DataObject.find(pair.dartFile());
        if (sourceObject instanceof FlutterDesignerDataObject
                || sourceObject.isModified()) {
            return sourceObject;
        }
        try {
            sourceObject.setValid(false);
        } catch (PropertyVetoException ex) {
            return sourceObject;
        }
        return DataObject.find(pair.dartFile());
    }

    @Override
    protected Node createNodeDelegate() {
        return new FlutterDesignerPairedFileNode(
                this, FlutterFileIcons.DESIGNER_FILE_ICON_PATH);
    }

    @Override
    public boolean isRenameAllowed() {
        return FlutterDesignerPairRename.isAllowed(getPrimaryFile());
    }

    @Override
    protected FileObject handleRename(String name) throws IOException {
        return FlutterDesignerPairRename.rename(getPrimaryFile(), name);
    }

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

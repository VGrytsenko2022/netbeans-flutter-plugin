package io.github.vgrytsenko2022.plugin.dart;

import io.github.vgrytsenko2022.plugin.ui.FlutterFileIcons;
import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;

/** Ordinary Dart source that is not owned by a paired Flutter Designer form. */
@DataObject.Registration(
        mimeType = DartTokenId.MIME_TYPE,
        displayName = "Dart Source File",
        iconBase = FlutterFileIcons.DART_FILE_ICON_PATH,
        position = 200)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.PasteAction"),
            position = 500,
            separatorAfter = 600),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 700),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 800,
            separatorAfter = 900),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(
                    category = "System",
                    id = "org.openide.actions.SaveAsTemplateAction"),
            position = 1000,
            separatorAfter = 1100),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(
                    category = "System",
                    id = "org.openide.actions.FileSystemAction"),
            position = 1200,
            separatorAfter = 1300),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1400),
    @ActionReference(
            path = "Loaders/text/x-dart/Actions",
            id = @ActionID(
                    category = "System",
                    id = "org.openide.actions.PropertiesAction"),
            position = 1500)
})
public final class DartDataObject extends MultiDataObject {

    public DartDataObject(FileObject primaryFile, MultiFileLoader loader)
            throws DataObjectExistsException, IOException {
        super(primaryFile, loader);
        // Designer-backed Dart sources have their own MultiView DataObject.
        // An ordinary Dart source must open in the standard NetBeans editor.
        registerEditor(DartTokenId.MIME_TYPE, false);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}

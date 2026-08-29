package dev.flutter.netbeans.plugin.theme;

import dev.flutter.netbeans.plugin.ui.FlutterFileIcons;
import java.io.IOException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;

/** Canonical project theme descriptor whose preferred Open action is the safe editor. */
@MIMEResolver.ExtensionRegistration(
        displayName = "Flutter Project Theme Descriptor",
        extension = FlutterThemeMime.EXTENSION,
        mimeType = FlutterThemeMime.MIME_TYPE,
        position = 354)
@DataObject.Registration(
        mimeType = FlutterThemeMime.MIME_TYPE,
        displayName = "Flutter Project Theme",
        iconBase = FlutterFileIcons.THEME_FILE_ICON_PATH,
        position = 100)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-flutter-project-theme/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = "Loaders/text/x-flutter-project-theme/Actions",
            id = @ActionID(
                    category = "System",
                    id = "org.openide.actions.FileSystemAction"),
            position = 300,
            separatorAfter = 400),
    @ActionReference(
            path = "Loaders/text/x-flutter-project-theme/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 500),
    @ActionReference(
            path = "Loaders/text/x-flutter-project-theme/Actions",
            id = @ActionID(
                    category = "System",
                    id = "org.openide.actions.PropertiesAction"),
            position = 600)
})
public final class FlutterThemeDataObject extends MultiDataObject {
    public FlutterThemeDataObject(
            FileObject primaryFile,
            MultiFileLoader loader) throws DataObjectExistsException {
        super(primaryFile, loader);
        getCookieSet().add((OpenCookie) () ->
                FlutterThemesEditorOpener.openDescriptor(getPrimaryFile()));
    }

    @Override
    public boolean isRenameAllowed() {
        return false;
    }

    @Override
    public boolean isDeleteAllowed() {
        return false;
    }

    @Override
    public boolean isMoveAllowed() {
        return false;
    }

    @Override
    public boolean isCopyAllowed() {
        return false;
    }

    @Override
    protected int associateLookup() {
        return 1;
    }
}

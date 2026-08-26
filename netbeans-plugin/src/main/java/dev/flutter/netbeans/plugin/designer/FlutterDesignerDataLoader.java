package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.plugin.dart.DartTokenId;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;

/**
 * Recognizes a same-directory {@code .dart + .fd} pair as one designer file.
 * Dart remains the technical primary so its editor MIME and language services
 * continue to work without special forwarding.
 */
@MIMEResolver.ExtensionRegistration(
        displayName = "Flutter Designer Model Files",
        extension = FlutterDesignerMime.MODEL_EXTENSION,
        mimeType = FlutterDesignerMime.MIME_TYPE,
        position = 350)
@DataObject.Registrations({
    @DataObject.Registration(
            mimeType = DartTokenId.MIME_TYPE,
            displayName = "Flutter Designer Screen",
            position = 100),
    @DataObject.Registration(
            mimeType = FlutterDesignerMime.MIME_TYPE,
            displayName = "Flutter Designer Screen",
            position = 100)
})
public final class FlutterDesignerDataLoader extends MultiFileLoader {
    private static final long serialVersionUID = 1L;

    public FlutterDesignerDataLoader() {
        super("dev.flutter.netbeans.plugin.designer.FlutterDesignerDataObject");
    }

    @Override
    protected FileObject findPrimaryFile(FileObject file) {
        if (file == null || file.isFolder()) {
            return null;
        }
        if (file.hasExt(FlutterDesignerMime.DART_EXTENSION)) {
            return FileUtil.findBrother(file, FlutterDesignerMime.MODEL_EXTENSION) == null
                    ? null
                    : file;
        }
        if (file.hasExt(FlutterDesignerMime.MODEL_EXTENSION)) {
            return FileUtil.findBrother(file, FlutterDesignerMime.DART_EXTENSION);
        }
        return null;
    }

    @Override
    protected MultiDataObject createMultiObject(FileObject primaryFile)
            throws DataObjectExistsException, IOException {
        return new FlutterDesignerDataObject(primaryFile, this);
    }

    @Override
    protected MultiDataObject.Entry createPrimaryEntry(
            MultiDataObject object,
            FileObject primaryFile) {
        return new FileEntry(object, primaryFile);
    }

    @Override
    protected MultiDataObject.Entry createSecondaryEntry(
            MultiDataObject object,
            FileObject secondaryFile) {
        return new FileEntry(object, secondaryFile);
    }

    @Override
    protected String actionsContext() {
        return "Loaders/text/x-dart/Actions";
    }
}

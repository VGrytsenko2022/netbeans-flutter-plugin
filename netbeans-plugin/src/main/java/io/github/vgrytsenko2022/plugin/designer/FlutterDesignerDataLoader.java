package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.plugin.dart.DartTokenId;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import io.github.vgrytsenko2022.plugin.ui.FlutterFileIcons;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;

/**
 * Recognizes a Dart file below {@code lib} when its mirrored model exists below
 * {@code .fd_templates}.
 *
 * <p>Dart remains the technical primary of the editing session so its editor
 * MIME, document identity and language services continue to work without
 * forwarding. The model is deliberately <em>not</em> registered as a secondary
 * entry: {@link FlutterDesignerModelDataObject} keeps the physical {@code .fd}
 * file visible in NetBeans' Files view and delegates opening it to this shared
 * editing session.</p>
 */
@MIMEResolver.ExtensionRegistration(
        displayName = "Flutter Designer Model Files",
        extension = FlutterDesignerMime.MODEL_EXTENSION,
        mimeType = FlutterDesignerMime.MIME_TYPE,
        position = 352)
@DataObject.Registration(
        mimeType = DartTokenId.MIME_TYPE,
        displayName = "Flutter Designer Screen",
        iconBase = FlutterFileIcons.DART_FILE_ICON_PATH,
        position = 100)
public final class FlutterDesignerDataLoader extends MultiFileLoader {
    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<FlutterProject> EXPLICIT_PROJECT =
            new ThreadLocal<>();

    public FlutterDesignerDataLoader() {
        super("io.github.vgrytsenko2022.plugin.designer.FlutterDesignerDataObject");
    }

    @Override
    protected FileObject findPrimaryFile(FileObject file) {
        if (file == null || !file.hasExt(FlutterDesignerMime.DART_EXTENSION)) {
            return null;
        }
        FlutterProject project = EXPLICIT_PROJECT.get();
        return (project == null
                ? FlutterDesignerPairLayout.findCompletePair(file)
                : FlutterDesignerPairLayout.findCompletePair(file, project))
                .map(FlutterDesignerPairLayout.Pair::dartFile)
                .orElse(null);
    }

    FileObject findPrimaryFile(FileObject file, FlutterProject project) {
        if (file == null || !file.hasExt(FlutterDesignerMime.DART_EXTENSION)) {
            return null;
        }
        return FlutterDesignerPairLayout.findCompletePair(file, project)
                .map(FlutterDesignerPairLayout.Pair::dartFile)
                .orElse(null);
    }

    /**
     * Runs normal DataObject recognition with an already resolved project.
     * This is used by lightweight unit containers that intentionally omit the
     * NetBeans project-owner service; production recognition uses FileOwnerQuery.
     */
    DataObject findDataObject(FileObject file, FlutterProject project)
            throws IOException {
        Objects.requireNonNull(project, "project");
        if (EXPLICIT_PROJECT.get() != null) {
            throw new IllegalStateException("Nested explicit project recognition");
        }
        EXPLICIT_PROJECT.set(project);
        try {
            return findDataObject(file, new HashSet<>());
        } finally {
            EXPLICIT_PROJECT.remove();
        }
    }

    @Override
    protected MultiDataObject createMultiObject(FileObject primaryFile)
            throws DataObjectExistsException, IOException {
        FlutterProject project = EXPLICIT_PROJECT.get();
        return project == null
                ? new FlutterDesignerDataObject(primaryFile, this)
                : new FlutterDesignerDataObject(primaryFile, this, project);
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

package io.github.vgrytsenko2022.plugin.designer.wizard;

import io.github.vgrytsenko2022.designer.template.DesignerFormTemplate;
import io.github.vgrytsenko2022.designer.template.DesignerFormTemplateFactory;
import io.github.vgrytsenko2022.designer.codec.FdEncodeException;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.plugin.dart.wizard.DartClassNaming;
import io.github.vgrytsenko2022.plugin.designer.FlutterDesignerPairLayout;
import io.github.vgrytsenko2022.plugin.project.FlutterProject;
import io.github.vgrytsenko2022.plugin.ui.FlutterFileIcons;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;

/** Creates one mirrored Designer pair below {@code lib} and {@code .fd_templates}. */
@TemplateRegistration(
        folder = "Flutter Designer",
        position = 100,
        displayName = "#LBL_FlutterDesignerForm",
        category = FlutterDesignerFormWizardIterator.TEMPLATE_CATEGORY,
        iconBase = FlutterFileIcons.DESIGNER_FILE_ICON_PATH,
        description = "FlutterDesignerFormDescription.html")
@Messages({
    "LBL_FlutterDesignerForm=Flutter Designer Form",
    "LBL_DesignerFormWizardStep=Name and Location",
    "LBL_DesignerFormWizardTitle=New Flutter Designer Form",
    "LBL_DesignerClassName=Widget &Class Name:",
    "LBL_DesignerWidgetKind=Widget &Kind:",
    "LBL_DesignerKindStateless=Stateless",
    "LBL_DesignerKindStateful=Stateful",
    "LBL_DesignerLocation=&Folder in lib:",
    "LBL_BrowseDesignerLocation=&Browse...",
    "LBL_DesignerDartFile=Dart &File:",
    "LBL_DesignerModelFile=Designer &Model:",
    "LBL_DesignerFormHint=<html>Dart stays under lib; the .fd model is mirrored under .fd_templates.<br>Stateful adds a separate State class, not automatic value bindings.</html>",
    "TTL_SelectDesignerLocation=Select Folder inside lib",
    "ACS_DesignerClassName=Flutter Designer widget class name",
    "ACD_DesignerClassName=UpperCamelCase name of the generated Flutter widget class.",
    "ACS_DesignerWidgetKind=Flutter Designer widget kind",
    "ACD_DesignerWidgetKind=Choose Stateless or Stateful. Stateful creates a separate State class for user state and event handlers; it does not add value bindings automatically.",
    "ACS_DesignerLocation=Folder inside lib",
    "ACD_DesignerLocation=Optional folder relative to the Flutter project's lib directory.",
    "ACS_BrowseDesignerLocation=Browse inside lib",
    "ACD_BrowseDesignerLocation=Choose an existing folder inside the Flutter project's lib directory.",
    "ACS_DesignerDartFile=Generated Dart source file",
    "ACS_DesignerModelFile=Generated Flutter Designer model file"
})
public final class FlutterDesignerFormWizardIterator
        implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {
    public static final String TEMPLATE_CATEGORY = "flutter-designer";

    private final boolean openAfterCreate;
    private final DesignerFormTemplateFactory templateFactory;
    private WizardDescriptor wizard;
    private FlutterDesignerFormWizardPanel panel;

    public FlutterDesignerFormWizardIterator() {
        this(true, new DesignerFormTemplateFactory());
    }

    FlutterDesignerFormWizardIterator(boolean openAfterCreate) {
        this(openAfterCreate, new DesignerFormTemplateFactory());
    }

    FlutterDesignerFormWizardIterator(
            boolean openAfterCreate,
            DesignerFormTemplateFactory templateFactory) {
        this.openAfterCreate = openAfterCreate;
        this.templateFactory = templateFactory;
    }

    @Override
    public Set<?> instantiate() throws IOException {
        FlutterProject project = requireFlutterProject();
        String className = requiredString(
                FlutterDesignerFormWizardPanel.PROP_CLASS_NAME);
        String fileName = DartClassNaming.fileName(className);
        Path projectRoot = localProjectRoot(project);
        String location = optionalString(
                FlutterDesignerFormWizardPanel.PROP_LOCATION);
        final DesignerFormTemplate template;
        try {
            template = templateFactory.create(fileName, className, selectedWidgetKind());
        } catch (FdEncodeException ex) {
            throw new IOException(
                    "Cannot encode the initial Flutter Designer model: "
                    + ex.getMessage(), ex);
        }

        CreatedPair[] created = new CreatedPair[1];
        project.getProjectDirectory().getFileSystem().runAtomicAction(
                (FileSystem.AtomicAction) () -> {
                    TargetPaths targets = resolveTargets(
                            projectRoot, location, fileName);
                    created[0] = createPair(project, targets, template);
                });
        CreatedPair pair = created[0];
        if (pair == null) {
            throw new IOException("Flutter Designer form creation produced no file pair.");
        }

        if (openAfterCreate) {
            DataObject dataObject = DataObject.find(pair.modelFile());
            OpenCookie open = dataObject.getLookup().lookup(OpenCookie.class);
            if (open != null) {
                EventQueue.invokeLater(open::open);
            }
        }
        // The visible designer model is the artifact created by this wizard;
        // its Open action delegates to the single Dart-owned editing session.
        return Set.of(pair.modelFile());
    }

    @Override
    public void initialize(WizardDescriptor descriptor) {
        wizard = descriptor;
        FlutterProject project = requireFlutterProjectUnchecked();
        panel = new FlutterDesignerFormWizardPanel(
                project,
                initialLocation(project, descriptor));
        descriptor.putProperty("NewFileWizard_Title", Bundle.LBL_DesignerFormWizardTitle());
        JComponent component = (JComponent) panel.getComponent();
        component.putClientProperty(
                WizardDescriptor.PROP_CONTENT_DATA,
                new String[]{Bundle.LBL_DesignerFormWizardStep()});
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, 0);
        component.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
        component.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
    }

    @Override
    public void uninitialize(WizardDescriptor descriptor) {
        wizard = null;
        panel = null;
    }

    @Override public WizardDescriptor.Panel<WizardDescriptor> current() { return panel; }
    @Override public String name() { return Bundle.LBL_DesignerFormWizardStep(); }
    @Override public boolean hasNext() { return false; }
    @Override public boolean hasPrevious() { return false; }
    @Override public void nextPanel() { throw new NoSuchElementException(); }
    @Override public void previousPanel() { throw new NoSuchElementException(); }
    @Override public void addChangeListener(ChangeListener listener) { }
    @Override public void removeChangeListener(ChangeListener listener) { }

    static TargetPaths resolveTargets(
            Path projectRoot,
            String location,
            String dartFileName) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Flutter project directory is unavailable: " + root + ".");
        }
        String relative = normalizeLocation(location);
        Path libRoot = root.resolve(FlutterDesignerPairLayout.DART_ROOT).normalize();
        if (!Files.isDirectory(libRoot, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(libRoot)) {
            throw new IOException("Flutter Designer forms require a real project lib directory: "
                    + libRoot + ".");
        }
        Path modelRoot = root.resolve(FlutterDesignerPairLayout.MODEL_ROOT).normalize();
        Path relativePath;
        try {
            relativePath = relative.isBlank() ? Path.of("") : Path.of(relative);
        } catch (InvalidPathException ex) {
            throw new IOException("Folder inside lib is invalid: " + ex.getReason() + ".", ex);
        }
        Path dartFolder = libRoot.resolve(relativePath).normalize();
        Path modelFolder = modelRoot.resolve(relativePath).normalize();
        requireSafeDirectoryPath(root, libRoot, dartFolder, "Dart source folder");
        requireSafeDirectoryPath(root, modelRoot, modelFolder, "Designer model folder");

        if (dartFileName == null
                || !dartFileName.matches("_?[a-z][a-z0-9_]*\\.dart")) {
            throw new IOException("Dart filename is not canonical: " + dartFileName + ".");
        }
        String modelFileName = dartFileName.substring(
                0, dartFileName.length() - ".dart".length()) + ".fd";
        Path dartFile = dartFolder.resolve(dartFileName).normalize();
        Path modelFile = modelFolder.resolve(modelFileName).normalize();
        if (!dartFile.startsWith(libRoot) || !modelFile.startsWith(modelRoot)) {
            throw new IOException("Flutter Designer form paths escaped their managed roots.");
        }
        return new TargetPaths(
                root,
                relative,
                dartFolder,
                modelFolder,
                dartFile,
                modelFile);
    }

    static String normalizeLocation(String value) throws IOException {
        String normalized = value == null ? "" : value.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.equals(".")) {
            return "";
        }
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new IOException("Designer form folder must be relative to lib.");
        }
        for (String segment : normalized.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Designer form folder contains an invalid path segment: "
                        + value + ".");
            }
        }
        return normalized;
    }

    private static void requireSafeDirectoryPath(
            Path projectRoot,
            Path managedRoot,
            Path target,
            String label) throws IOException {
        if (!managedRoot.startsWith(projectRoot)
                || !target.startsWith(managedRoot)) {
            throw new IOException(label + " must remain inside the Flutter project.");
        }
        Path current = projectRoot;
        Path relative = projectRoot.relativize(target);
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                break;
            }
            if (Files.isSymbolicLink(current)) {
                throw new IOException(label + " contains a symbolic link: " + current + ".");
            }
            if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(label + " contains a file where a folder is required: "
                        + current + ".");
            }
        }
        Path existing = target;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            if (existing == null || !existing.startsWith(projectRoot)) {
                throw new IOException("Cannot resolve " + label + " inside the Flutter project.");
            }
        }
        if (!existing.toRealPath().startsWith(projectRoot.toRealPath())) {
            throw new IOException(label + " resolves outside the Flutter project.");
        }
    }

    private CreatedPair createPair(
            FlutterProject project,
            TargetPaths targets,
            DesignerFormTemplate template) throws IOException {
        FileObject projectDirectory = project.getProjectDirectory();
        String sourceFolderPath = FlutterDesignerPairLayout.DART_ROOT
                + suffix(targets.relativeFolder());
        String modelFolderPath = FlutterDesignerPairLayout.MODEL_ROOT
                + suffix(targets.relativeFolder());
        FileObject model = null;
        FileObject dart = null;
        try {
            FileObject sourceFolder = FileUtil.createFolder(
                    projectDirectory, sourceFolderPath);
            FileObject modelFolder = FileUtil.createFolder(
                    projectDirectory, modelFolderPath);
            String dartName = targets.dartFile().getFileName().toString();
            String modelName = targets.modelFile().getFileName().toString();

            // Re-resolve after folder creation so a replaced symlink, junction,
            // or file cannot redirect either half between validation and write.
            TargetPaths verified = resolveTargets(
                    targets.projectRoot(), targets.relativeFolder(), dartName);
            if (!verified.dartFolder().equals(targets.dartFolder())
                    || !verified.modelFolder().equals(targets.modelFolder())
                    || !verified.modelFile().getFileName().toString().equals(modelName)) {
                throw new IOException(
                        "Flutter Designer form paths changed during creation.");
            }
            if (sourceFolder.getFileObject(dartName) != null) {
                throw new IOException("Cannot create Flutter Designer form: Dart source already exists at "
                        + targets.dartFile() + ".");
            }
            if (modelFolder.getFileObject(modelName) != null) {
                throw new IOException("Cannot create Flutter Designer form: model already exists at "
                        + targets.modelFile() + ".");
            }

            model = modelFolder.createData(modelName);
            write(model, template.fdBytes());
            dart = sourceFolder.createData(dartName);
            write(dart, template.dartBytes());
            return new CreatedPair(dart, model);
        } catch (IOException | RuntimeException failure) {
            rollback(dart, failure);
            rollback(model, failure);
            if (failure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw new IOException("Cannot create Flutter Designer form pair.", failure);
        }
    }

    private static void write(FileObject file, byte[] bytes) throws IOException {
        try (FileLock lock = file.lock();
                OutputStream output = file.getOutputStream(lock)) {
            output.write(bytes);
        }
    }

    private static void rollback(FileObject file, Exception failure) {
        if (file == null || !file.isValid()) {
            return;
        }
        try {
            file.delete();
        } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static String suffix(String relativeFolder) {
        return relativeFolder.isBlank() ? "" : "/" + relativeFolder;
    }

    private FlutterProject requireFlutterProject() throws IOException {
        Project selected = Templates.getProject(wizard);
        if (selected instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        throw new IOException(
                "Flutter Designer Form can be created only inside an open Flutter project.");
    }

    private FlutterProject requireFlutterProjectUnchecked() {
        Project selected = Templates.getProject(wizard);
        if (selected instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        throw new IllegalStateException(
                "Flutter Designer Form wizard requires an open Flutter project, but the selected project is "
                + (selected == null ? "missing" : selected.getProjectDirectory().getPath()) + ".");
    }

    private static Path localProjectRoot(FlutterProject project) throws IOException {
        java.io.File local = FileUtil.toFile(project.getProjectDirectory());
        if (local == null) {
            throw new IOException("Flutter Designer Form requires a local Flutter project.");
        }
        return local.toPath().toAbsolutePath().normalize();
    }

    private String requiredString(String key) throws IOException {
        Object value = wizard.getProperty(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        throw new IOException("Cannot create Flutter Designer form: wizard value '"
                + key + "' is missing.");
    }

    private String optionalString(String key) throws IOException {
        Object value = wizard.getProperty(key);
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text.trim();
        }
        throw new IOException("Cannot create Flutter Designer form: wizard value '"
                + key + "' is invalid.");
    }

    private WidgetClassKind selectedWidgetKind() throws IOException {
        Object value = wizard.getProperty(FlutterDesignerFormWizardPanel.PROP_WIDGET_KIND);
        if (value == null) return WidgetClassKind.STATELESS;
        if (value instanceof WidgetClassKind kind) return kind;
        throw new IOException("Cannot create Flutter Designer form: widget kind must be Stateless or Stateful.");
    }

    private static String initialLocation(
            FlutterProject project,
            WizardDescriptor descriptor) {
        FileObject lib = project.getProjectDirectory().getFileObject(
                FlutterDesignerPairLayout.DART_ROOT);
        FileObject selected = Templates.getTargetFolder(descriptor);
        if (lib == null || selected == null) {
            return "";
        }
        String relative = FileUtil.getRelativePath(lib, selected);
        return relative == null ? "" : relative;
    }

    record TargetPaths(
            Path projectRoot,
            String relativeFolder,
            Path dartFolder,
            Path modelFolder,
            Path dartFile,
            Path modelFile) {
    }

    private record CreatedPair(FileObject dartFile, FileObject modelFile) {
    }
}

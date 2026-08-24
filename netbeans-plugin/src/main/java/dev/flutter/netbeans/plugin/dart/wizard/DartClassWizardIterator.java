package dev.flutter.netbeans.plugin.dart.wizard;

import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectFactory;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;

/** Creates a lower_snake_case Dart file containing an UpperCamelCase class. */
@TemplateRegistration(
        folder = "Dart",
        position = 100,
        displayName = "#LBL_DartClass",
        category = "dart",
        iconBase = FlutterProjectFactory.ICON_PATH,
        description = "DartClassDescription.html")
@Messages({
    "LBL_DartClass=Dart Class",
    "LBL_DartClassWizardStep=Name and Location",
    "LBL_DartClassWizardTitle=New Dart Class",
    "LBL_DartClassName=Class &Name:",
    "LBL_DartClassLocation=&Location:",
    "LBL_BrowseDartLocation=&Browse...",
    "LBL_DartCreatedFile=Created &File:",
    "LBL_DartClassHint=The class name is converted to a lower_snake_case .dart file and opened in the Dart editor.",
    "TTL_SelectDartClassLocation=Select Dart Class Location",
    "ACS_DartClassName=Dart class name",
    "ACD_DartClassName=UpperCamelCase Dart class name, optionally beginning with an underscore.",
    "ACS_DartClassLocation=Dart class location",
    "ACD_DartClassLocation=Folder relative to the current Flutter project.",
    "ACS_BrowseDartLocation=Browse for Dart class location",
    "ACD_BrowseDartLocation=Choose an existing folder inside the current Flutter project.",
    "ACS_DartCreatedFile=Created Dart file",
    "ACD_DartCreatedFile=Exact lower_snake_case Dart file that the wizard will create."
})
public final class DartClassWizardIterator
        implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {
    private final boolean openAfterCreate;
    private WizardDescriptor wizard;
    private DartClassWizardPanel panel;

    public DartClassWizardIterator() {
        this(true);
    }

    DartClassWizardIterator(boolean openAfterCreate) {
        this.openAfterCreate = openAfterCreate;
    }

    @Override
    public Set<?> instantiate() throws IOException {
        FlutterProject project = requireFlutterProject();
        String className = stringProperty(DartClassWizardPanel.PROP_CLASS_NAME);
        Path projectRoot = FileUtil.toFile(project.getProjectDirectory())
                .toPath().toAbsolutePath().normalize();
        Path targetFolder = resolveLocation(
                projectRoot,
                stringProperty(DartClassWizardPanel.PROP_LOCATION));
        String location = projectRoot.relativize(targetFolder).toString().replace('\\', '/');
        FileObject folder = FileUtil.createFolder(project.getProjectDirectory(), location);
        String fileName = DartClassNaming.fileName(className);
        if (folder.getFileObject(fileName) != null) {
            throw new IOException("Cannot create Dart class " + className + ": file already exists at "
                    + folder.getPath() + "/" + fileName + ".");
        }

        FileObject created = folder.createData(fileName);
        boolean written = false;
        try (FileLock lock = created.lock(); OutputStream output = created.getOutputStream(lock)) {
            output.write(DartClassNaming.source(className).getBytes(StandardCharsets.UTF_8));
            written = true;
        } finally {
            if (!written) {
                created.delete();
            }
        }

        if (openAfterCreate) {
            DataObject dataObject = DataObject.find(created);
            OpenCookie open = dataObject.getLookup().lookup(OpenCookie.class);
            if (open != null) {
                EventQueue.invokeLater(open::open);
            }
        }
        return Set.of(created);
    }

    @Override
    public void initialize(WizardDescriptor descriptor) {
        wizard = descriptor;
        FlutterProject project = requireFlutterProjectUnchecked();
        panel = new DartClassWizardPanel(project, initialLocation(project, descriptor));
        descriptor.putProperty("NewFileWizard_Title", Bundle.LBL_DartClassWizardTitle());
        JComponent component = (JComponent) panel.getComponent();
        component.putClientProperty(
                WizardDescriptor.PROP_CONTENT_DATA,
                new String[]{Bundle.LBL_DartClassWizardStep()});
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
    @Override public String name() { return Bundle.LBL_DartClassWizardStep(); }
    @Override public boolean hasNext() { return false; }
    @Override public boolean hasPrevious() { return false; }
    @Override public void nextPanel() { throw new NoSuchElementException(); }
    @Override public void previousPanel() { throw new NoSuchElementException(); }
    @Override public void addChangeListener(ChangeListener listener) { }
    @Override public void removeChangeListener(ChangeListener listener) { }

    private FlutterProject requireFlutterProject() throws IOException {
        Project selected = Templates.getProject(wizard);
        if (selected instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        throw new IOException("Dart Class can be created only inside an open Flutter project.");
    }

    private FlutterProject requireFlutterProjectUnchecked() {
        Project selected = Templates.getProject(wizard);
        if (selected instanceof FlutterProject flutterProject) {
            return flutterProject;
        }
        throw new IllegalStateException(
                "Dart Class wizard requires an open Flutter project, but the selected project is "
                + (selected == null ? "missing" : selected.getProjectDirectory().getPath()) + ".");
    }

    private String stringProperty(String key) throws IOException {
        Object value = wizard.getProperty(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        throw new IOException("Cannot create Dart class: wizard value '" + key + "' is missing.");
    }

    static String normalizeLocation(String value) throws IOException {
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.isBlank() || normalized.startsWith("/")
                || normalized.matches("^[A-Za-z]:.*")) {
            throw new IOException("Dart class location must be relative to the Flutter project.");
        }
        for (String segment : normalized.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Dart class location contains an invalid path segment: "
                        + value + ".");
            }
        }
        if (normalized.equals("build") || normalized.startsWith("build/")
                || normalized.equals(".dart_tool") || normalized.startsWith(".dart_tool/")) {
            throw new IOException("Dart classes cannot be created in generated folder '"
                    + normalized + "'.");
        }
        return normalized;
    }

    static Path resolveLocation(Path projectRoot, String value) throws IOException {
        String normalized = normalizeLocation(value);
        Path root = projectRoot.toAbsolutePath().normalize();
        final Path target;
        try {
            Path entered = Path.of(normalized);
            if (entered.isAbsolute()) {
                throw new IOException("Dart class location must be relative to the Flutter project.");
            }
            target = root.resolve(entered).normalize();
        } catch (InvalidPathException ex) {
            throw new IOException("Dart class location is invalid: " + ex.getReason() + ".", ex);
        }
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IOException("Dart class location must be inside the Flutter project.");
        }

        Path existing = target;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            if (existing == null || !existing.startsWith(root)) {
                throw new IOException("Cannot resolve Dart class location inside the Flutter project.");
            }
        }
        Path realRoot = root.toRealPath();
        Path realExisting = existing.toRealPath();
        if (!realExisting.startsWith(realRoot)) {
            throw new IOException("Dart class location resolves outside the Flutter project: "
                    + value + ".");
        }
        if (!Files.isDirectory(realExisting)) {
            throw new IOException("Dart class location has a file where a folder is required: "
                    + existing + ".");
        }
        return target;
    }

    private static String initialLocation(FlutterProject project, WizardDescriptor descriptor) {
        FileObject selected = Templates.getTargetFolder(descriptor);
        String relative = selected == null
                ? null
                : FileUtil.getRelativePath(project.getProjectDirectory(), selected);
        if (relative != null && !relative.isBlank()
                && !relative.equals("build") && !relative.startsWith("build/")
                && !relative.equals(".dart_tool") && !relative.startsWith(".dart_tool/")) {
            return relative;
        }
        return project.getProjectDirectory().getFileObject("lib") == null ? "test" : "lib";
    }
}

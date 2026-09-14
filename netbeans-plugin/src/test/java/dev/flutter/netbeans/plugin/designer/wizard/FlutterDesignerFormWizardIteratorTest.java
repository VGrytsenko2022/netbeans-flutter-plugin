package dev.flutter.netbeans.plugin.designer.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.plugin.designer.FlutterDesignerPairLayout;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;

class FlutterDesignerFormWizardIteratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsToSelectedLibFolderAndCreatesMirroredValidPair() throws Exception {
        ProjectFiles project = project("create", "lib/features/orders");
        TestWizardDescriptor wizard = wizard(project, "lib/features/orders");

        FlutterDesignerFormWizardIterator iterator =
                new FlutterDesignerFormWizardIterator(false);
        iterator.initialize(wizard);
        FlutterDesignerFormWizardPanel panel =
                (FlutterDesignerFormWizardPanel) iterator.current();
        panel.readSettings(wizard);
        FlutterDesignerFormWizardVisual visual =
                (FlutterDesignerFormWizardVisual) panel.getComponent();
        assertEquals(WidgetClassKind.STATELESS, visual.widgetKind());
        assertEquals("features/orders",
                visual.relativeLocation().replace('\\', '/'));
        assertTrue(visual.displayedDartFile().replace('\\', '/')
                .endsWith("/lib/features/orders/new_screen.dart"));
        assertTrue(visual.displayedModelFile().replace('\\', '/')
                .endsWith("/.fd_templates/features/orders/new_screen.fd"));

        wizard.putProperty(
                FlutterDesignerFormWizardPanel.PROP_CLASS_NAME,
                "OrderScreen");
        wizard.putProperty(
                FlutterDesignerFormWizardPanel.PROP_LOCATION,
                visual.relativeLocation());
        Set<?> created = iterator.instantiate();

        Path dart = project.path().resolve(
                "lib/features/orders/order_screen.dart");
        Path model = project.path().resolve(
                ".fd_templates/features/orders/order_screen.fd");
        assertEquals(1, created.size());
        assertTrue(created.stream().anyMatch(file -> file instanceof FileObject object
                && object.getPath().endsWith(
                        ".fd_templates/features/orders/order_screen.fd")));
        assertTrue(Files.isRegularFile(dart));
        assertTrue(Files.isRegularFile(model));

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(Files.readAllBytes(model)));
        assertEquals("order_screen.dart", decoded.document().source().dartFile());
        assertEquals("OrderScreen", decoded.document().source().className());
        assertEquals(WidgetClassKind.STATELESS, decoded.document().source().widgetKind());
        assertTrue(new DartSourceIntegrityScanner()
                .scan(Files.readAllBytes(dart), decoded.document().source())
                .onDiskDeclaredMatch());

        FileObject dartObject = project.root().getFileObject(
                "lib/features/orders/order_screen.dart");
        FlutterDesignerPairLayout.Pair pair = FlutterDesignerPairLayout
                .findCompletePair(dartObject, project.project())
                .orElseThrow();
        assertEquals("features/orders/order_screen.fd", pair.relativeModelPath());
        iterator.uninitialize(wizard);
    }

    @Test
    void projectRootOrNonLibSelectionDefaultsToLibItself() throws Exception {
        ProjectFiles project = project("defaults", "test");
        TestWizardDescriptor wizard = wizard(project, "test");
        FlutterDesignerFormWizardIterator iterator =
                new FlutterDesignerFormWizardIterator(false);
        iterator.initialize(wizard);
        FlutterDesignerFormWizardPanel panel =
                (FlutterDesignerFormWizardPanel) iterator.current();
        panel.readSettings(wizard);
        FlutterDesignerFormWizardVisual visual =
                (FlutterDesignerFormWizardVisual) panel.getComponent();

        assertEquals("", visual.relativeLocation());
        assertTrue(visual.displayedDartFile().replace('\\', '/')
                .endsWith("/lib/new_screen.dart"));
        assertTrue(visual.displayedModelFile().replace('\\', '/')
                .endsWith("/.fd_templates/new_screen.fd"));
    }

    @Test
    void targetResolutionRejectsEveryPathOutsideLib() throws Exception {
        ProjectFiles project = project("paths", "lib");
        Path root = project.path();

        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, "../test", "screen.dart"));
        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, "a/../../test", "screen.dart"));
        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, root.resolve("test").toString(), "screen.dart"));
        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, "screens", "../screen.dart"));

        FlutterDesignerFormWizardIterator.TargetPaths nested =
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, "screens/profile", "profile_screen.dart");
        assertTrue(nested.dartFile().startsWith(root.resolve("lib")));
        assertTrue(nested.modelFile().startsWith(root.resolve(".fd_templates")));
    }

    @Test
    void existingEitherHalfBlocksCreationWithoutOverwritingIt() throws Exception {
        ProjectFiles project = project("collision", "lib/screens");
        Path existingModel = project.path().resolve(
                ".fd_templates/screens/profile_screen.fd");
        FileObject existingModelObject = FileUtil.createData(project.root(),
                ".fd_templates/screens/profile_screen.fd");
        try (java.io.OutputStream output = existingModelObject.getOutputStream()) {
            output.write("keep-model".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        TestWizardDescriptor wizard = wizard(project, "lib/screens");
        FlutterDesignerFormWizardIterator iterator =
                new FlutterDesignerFormWizardIterator(false);
        iterator.initialize(wizard);
        wizard.putProperty(
                FlutterDesignerFormWizardPanel.PROP_CLASS_NAME,
                "ProfileScreen");
        wizard.putProperty(
                FlutterDesignerFormWizardPanel.PROP_LOCATION,
                "screens");

        assertThrows(IOException.class, iterator::instantiate);
        assertEquals("keep-model", Files.readString(existingModel));
        assertFalse(Files.exists(project.path().resolve(
                "lib/screens/profile_screen.dart")));
    }

    @Test
    void missingLibDirectoryDisablesTargetResolution() throws Exception {
        Path root = temporaryDirectory.resolve("missing_lib");
        Files.createDirectories(root);
        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        root, "", "screen.dart"));
    }

    @Test
    void targetResolutionRejectsLinkedFoldersBelowLib() throws Exception {
        ProjectFiles project = project("linked_lib_folder", "lib");
        Path externalSource = temporaryDirectory.resolve("external_source");
        Files.createDirectories(externalSource);
        createDirectoryLinkOrSkip(
                project.path().resolve("lib/screens"), externalSource);

        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        project.path(), "screens", "screen.dart"));
    }

    @Test
    void targetResolutionRejectsLinkedModelRoot() throws Exception {
        ProjectFiles project = project("linked_model_root", "lib");
        Path externalModels = temporaryDirectory.resolve("external_models");
        Files.createDirectories(externalModels);
        createDirectoryLinkOrSkip(
                project.path().resolve(FlutterDesignerPairLayout.MODEL_ROOT),
                externalModels);

        assertThrows(IOException.class, () ->
                FlutterDesignerFormWizardIterator.resolveTargets(
                        project.path(), "screens", "screen.dart"));
    }

    @Test
    void nonLocalFilesystemIsHandledWithoutDereferencingANullFile() throws Exception {
        FileSystem memory = FileUtil.createMemoryFileSystem();
        FileObject root = memory.getRoot();
        assertNull(FlutterDesignerFormWizardPanel.localProjectRoot(root));

        FlutterDesignerFormWizardVisual visual =
                new FlutterDesignerFormWizardVisual(() -> { });
        visual.initialize(null, null, "MemoryScreen", "nested");
        assertEquals("", visual.displayedDartFile());
        assertEquals("", visual.displayedModelFile());
    }

    @Test
    void explicitStatefulChoiceIsStoredAndCreatesMirroredStateOwnedBuild() throws Exception {
        ProjectFiles project = project("stateful_choice", "lib/forms");
        TestWizardDescriptor wizard = wizard(project, "lib/forms");
        FlutterDesignerFormWizardIterator iterator = new FlutterDesignerFormWizardIterator(false);
        iterator.initialize(wizard);
        FlutterDesignerFormWizardPanel panel = (FlutterDesignerFormWizardPanel) iterator.current();
        panel.readSettings(wizard);
        var visual = (FlutterDesignerFormWizardVisual) panel.getComponent();
        String displayedSource = visual.displayedDartFile();
        String displayedModel = visual.displayedModelFile();
        AtomicInteger changes = new AtomicInteger();
        panel.addChangeListener(event -> changes.incrementAndGet());
        SwingUtilities.invokeAndWait(() -> visual.setWidgetKind(WidgetClassKind.STATEFUL));
        assertTrue(changes.get() > 0);
        assertEquals(displayedSource, visual.displayedDartFile());
        assertEquals(displayedModel, visual.displayedModelFile());
        panel.storeSettings(wizard);
        assertEquals(WidgetClassKind.STATEFUL, wizard.getProperty(FlutterDesignerFormWizardPanel.PROP_WIDGET_KIND));
        iterator.instantiate();
        Path source = project.path().resolve("lib/forms/new_screen.dart");
        Path model = project.path().resolve(".fd_templates/forms/new_screen.fd");
        var decoded = assertInstanceOf(FdDecodeResult.Current.class, new FdDocumentCodec().decode(Files.readAllBytes(model)));
        assertEquals(WidgetClassKind.STATEFUL, decoded.document().source().widgetKind());
        assertTrue(Files.readString(source).contains("State<NewScreen> createState() => _NewScreenState();"));
        assertTrue(Files.readString(source).contains("class _NewScreenState extends State<NewScreen>"));
        var integrity = new DartSourceIntegrityScanner().scan(Files.readAllBytes(source), decoded.document().source());
        assertTrue(integrity.onDiskDeclaredMatch(), integrity.diagnostics().toString());
        iterator.uninitialize(wizard);
    }

    @Test
    void restoresExplicitKindAndRejectsUnknownKindBeforeCreatingFiles() throws Exception {
        ProjectFiles project = project("stateful_settings", "lib");
        TestWizardDescriptor wizard = wizard(project, "lib");
        wizard.putProperty(FlutterDesignerFormWizardPanel.PROP_WIDGET_KIND, WidgetClassKind.STATEFUL);
        FlutterDesignerFormWizardIterator iterator = new FlutterDesignerFormWizardIterator(false);
        iterator.initialize(wizard);
        FlutterDesignerFormWizardPanel panel = (FlutterDesignerFormWizardPanel) iterator.current();
        panel.readSettings(wizard);
        assertEquals(WidgetClassKind.STATEFUL,
                ((FlutterDesignerFormWizardVisual) panel.getComponent()).widgetKind());
        panel.storeSettings(wizard);
        wizard.putProperty(FlutterDesignerFormWizardPanel.PROP_WIDGET_KIND, "unsupported-kind");
        IOException failure = assertThrows(IOException.class, iterator::instantiate);
        assertTrue(failure.getMessage().contains("widget kind"));
        assertFalse(Files.exists(project.path().resolve("lib/new_screen.dart")));
        assertFalse(Files.exists(project.path().resolve(".fd_templates/new_screen.fd")));
    }

    private ProjectFiles project(String name, String selectedFolder) throws Exception {
        Path directory = temporaryDirectory.resolve(name);
        Files.createDirectories(directory.resolve("lib"));
        Files.createDirectories(directory.resolve(selectedFolder));
        Files.writeString(directory.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                """);
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(directory.toFile());
        FileObject root = fileSystem.getRoot();
        FlutterProject project = (FlutterProject) new FlutterProjectFactory()
                .loadProject(root, new TestProjectState());
        assertNotNull(project);
        return new ProjectFiles(project, root, directory);
    }

    private static TestWizardDescriptor wizard(
            ProjectFiles project,
            String targetFolder) throws Exception {
        TestWizardDescriptor wizard = new TestWizardDescriptor();
        wizard.putProperty("project", project.project());
        FileObject selected = project.root().getFileObject(targetFolder);
        assertNotNull(selected);
        Templates.setTargetFolder(wizard, selected);
        return wizard;
    }

    private static void createDirectoryLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target.toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false,
                    "Directory symbolic links are unavailable: " + ex.getMessage());
        }
    }

    private record ProjectFiles(
            FlutterProject project,
            FileObject root,
            Path path) {
    }

    private static final class TestWizardDescriptor extends WizardDescriptor {
        private TestWizardDescriptor() {
        }
    }

    private static final class TestProjectState implements ProjectState {
        @Override public void markModified() { }
        @Override public void notifyDeleted() { }
    }
}

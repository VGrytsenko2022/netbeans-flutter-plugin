package dev.flutter.netbeans.plugin.dart.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.plugin.project.FlutterProjectFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.LocalFileSystem;

class DartClassWizardIteratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsToSelectedFolderAndCreatesLowerSnakeCaseDartFile() throws Exception {
        Path root = temporaryDirectory.resolve("sample_app");
        Files.createDirectories(root.resolve("lib/features/orders"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                """);

        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject projectDirectory = fileSystem.getRoot();
        Project project = new FlutterProjectFactory().loadProject(
                projectDirectory,
                new TestProjectState());
        assertNotNull(project);

        TestWizardDescriptor wizard = new TestWizardDescriptor();
        wizard.putProperty("project", project);
        FileObject selectedFolder = projectDirectory.getFileObject("lib/features/orders");
        Templates.setTargetFolder(wizard, selectedFolder);

        DartClassWizardIterator iterator = new DartClassWizardIterator(false);
        iterator.initialize(wizard);
        DartClassWizardPanel panel = (DartClassWizardPanel) iterator.current();
        panel.readSettings(wizard);
        DartClassWizardVisual visual = (DartClassWizardVisual) panel.getComponent();
        assertEquals("lib/features/orders", visual.relativeLocation().replace('\\', '/'));

        wizard.putProperty(DartClassWizardPanel.PROP_CLASS_NAME, "OrderRepository");
        wizard.putProperty(DartClassWizardPanel.PROP_LOCATION, visual.relativeLocation());
        Set<?> created = iterator.instantiate();

        assertEquals(1, created.size());
        Path createdFile = root.resolve("lib/features/orders/order_repository.dart");
        assertTrue(Files.isRegularFile(createdFile));
        assertEquals("""
                class OrderRepository {
                  const OrderRepository();
                }
                """, Files.readString(createdFile));
        iterator.uninitialize(wizard);
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

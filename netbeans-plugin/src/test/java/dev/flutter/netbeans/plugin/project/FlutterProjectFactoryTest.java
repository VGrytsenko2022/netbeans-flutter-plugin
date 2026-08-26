package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterDevice;
import dev.flutter.netbeans.api.FlutterProjectInfo;
import dev.flutter.netbeans.plugin.tooling.FlutterToolingController;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.LocalFileSystem;

class FlutterProjectFactoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsFlutterDirectoryWithProjectServices() throws Exception {
        Path root = temporaryDirectory.resolve("sample_app");
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve("test"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                flutter:
                """);
        Files.writeString(root.resolve("lib/main.dart"), "void main() {}\n");
        LocalFileSystem fileSystem = new LocalFileSystem();
        fileSystem.setRootDirectory(root.toFile());
        FileObject directory = fileSystem.getRoot();
        assertNotNull(directory);

        FlutterProjectFactory factory = new FlutterProjectFactory();
        assertTrue(factory.isProject(directory));
        assertEquals("sample_app", factory.isProject2(directory).getDisplayName());

        var project = factory.loadProject(directory, new TestProjectState());
        assertNotNull(project);
        assertEquals("sample_app",
                project.getLookup().lookup(FlutterProjectInfo.class).name());
        assertEquals("sample_app",
                project.getLookup().lookup(ProjectInformation.class).getDisplayName());
        FlutterRunController runController =
                project.getLookup().lookup(FlutterRunController.class);
        assertNotNull(runController);
        FlutterToolingController toolingController =
                project.getLookup().lookup(FlutterToolingController.class);
        assertNotNull(toolingController);
        assertNotNull(project.getLookup().lookup(DartAnalysisLifecycle.class));
        assertNotNull(project.getLookup().lookup(ProjectConfigurationProvider.class));
        assertNotNull(project.getLookup().lookup(AuxiliaryConfiguration.class));
        assertNotNull(project.getLookup().lookup(AuxiliaryProperties.class));
        assertNotNull(project.getLookup().lookup(DeleteOperationImplementation.class));
        assertNotNull(project.getLookup().lookup(MoveOrRenameOperationImplementation.class));
        assertArrayEquals(
                new String[]{FlutterRecommendedTemplates.DART_TEMPLATE_CATEGORY},
                project.getLookup().lookup(RecommendedTemplates.class).getRecommendedTypes());

        ActionProvider actions = project.getLookup().lookup(ActionProvider.class);
        assertNotNull(actions);
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_RUN));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_DEBUG));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_BUILD));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_REBUILD));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_CLEAN));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(ActionProvider.COMMAND_TEST));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(
                FlutterProjectActionProvider.COMMAND_PUB_GET));
        assertTrue(java.util.List.of(actions.getSupportedActions()).contains(
                FlutterProjectActionProvider.COMMAND_ANALYZE));
        assertTrue(actions.isActionEnabled(ActionProvider.COMMAND_RUN, org.openide.util.Lookup.EMPTY));
        assertTrue(actions.isActionEnabled(ActionProvider.COMMAND_CLEAN, org.openide.util.Lookup.EMPTY));
        assertTrue(actions.isActionEnabled(ActionProvider.COMMAND_BUILD, org.openide.util.Lookup.EMPTY));
        assertTrue(actions.isActionEnabled(
                ActionProvider.COMMAND_BUILD,
                org.openide.util.lookup.Lookups.singleton(new FlutterTargetConfiguration(
                        new FlutterDevice("windows", "Windows", "windows-x64", false)))));
        assertTrue(!actions.isActionEnabled(
                FlutterProjectActionProvider.COMMAND_HOT_RELOAD,
                org.openide.util.Lookup.EMPTY));

        Sources sources = project.getLookup().lookup(Sources.class);
        assertEquals(1, sources.getSourceGroups(Sources.TYPE_GENERIC).length);
        assertEquals(2, sources.getSourceGroups(FlutterProjectSources.TYPE_DART).length);

        assertNotNull(ProjectManager.mutex());
        LogicalViewProvider view = project.getLookup().lookup(LogicalViewProvider.class);
        var rootNode = view.createLogicalView();
        assertEquals("sample_app", rootNode.getDisplayName());
        assertNotNull(view.findPath(rootNode, directory.getFileObject("lib/main.dart")));
        javax.swing.Action[] popup = rootNode.getActions(false);
        assertNull(popup[1]);
        assertEquals("projectCommand:build", popup[2].getValue(javax.swing.Action.NAME));
        assertEquals("projectCommand:rebuild", popup[3].getValue(javax.swing.Action.NAME));
        assertEquals("projectCommand:clean", popup[4].getValue(javax.swing.Action.NAME));
        assertEquals("Build", FlutterLogicalViewProvider.BUILD_ACTION_LABEL);
        assertEquals("Clean and Build", FlutterLogicalViewProvider.REBUILD_ACTION_LABEL);
        assertEquals("Clean", FlutterLogicalViewProvider.CLEAN_ACTION_LABEL);
        assertNull(popup[5]);

        runController.close();
        toolingController.close();
        assertTrue(!runController.isCommandEnabled(ActionProvider.COMMAND_RUN));
        assertTrue(!toolingController.isCommandEnabled(ActionProvider.COMMAND_TEST));
        runController.open();
        toolingController.open();
        assertTrue(runController.isCommandEnabled(ActionProvider.COMMAND_RUN));
        assertTrue(toolingController.isCommandEnabled(ActionProvider.COMMAND_TEST));
    }

    private static final class TestProjectState implements ProjectState {
        @Override public void markModified() { }
        @Override public void notifyDeleted() { }
    }
}

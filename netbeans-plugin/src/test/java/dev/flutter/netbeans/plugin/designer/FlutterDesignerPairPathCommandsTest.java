package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.template.DesignerFormTemplate;
import dev.flutter.netbeans.designer.template.DesignerFormTemplateFactory;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

/** Command-boundary checks that do not require a live TopComponent shell. */
class FlutterDesignerPairPathCommandsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void noDedicatedShellRejectsTheAsyncRouteAndReleasesItsPairLease()
            throws Exception {
        CommandFixture fixture = fixture("ordinary_route");
        FlutterDesignerDataObject dataObject = fixture.dataObject();

        assertFalse(FlutterDesignerPairPathCommands.requiresPostClose(
                dataObject));

        FlutterDesignerEditorSupport.PostCloseOutcome<FileObject> outcome =
                FlutterDesignerPairPathCommands.rename(
                                dataObject,
                                "renamed_home",
                                () -> {
                                    throw new AssertionError(
                                            "NOT_REQUIRED must not run the callback");
                                })
                        .toCompletableFuture()
                        .get(5, TimeUnit.SECONDS);

        assertEquals(
                FlutterDesignerEditorSupport.PostCloseStatus.NOT_REQUIRED,
                outcome.status());
        assertTrue(dataObject.getPairSaveCoordinator().canBeginPairRename(),
                "a rejected async route must release its reserved lease");
        assertTrue(Files.exists(fixture.dartPath()));
        assertTrue(Files.exists(fixture.modelPath()));
        assertFalse(Files.exists(fixture.dartPath().resolveSibling(
                "renamed_home.dart")));
        assertFalse(Files.exists(fixture.modelPath().resolveSibling(
                "renamed_home.fd")));
    }

    @Test
    void mismatchedDedicatedShellTopologyFailsBeforeTheCallbackAndReleasesLease()
            throws Exception {
        CommandFixture fixture = fixture("invalid_shell_topology");
        FlutterDesignerDataObject dataObject = fixture.dataObject();
        FlutterDesignerEditorClosePermitCoordinator permits = dataObject
                .getEditorSupport().editorClosePermits();
        Object unregisteredShell = new Object();
        permits.editorShellOpenedOrClosing(unregisteredShell);
        AtomicBoolean callbackRan = new AtomicBoolean();
        try {
            assertTrue(FlutterDesignerPairPathCommands.requiresPostClose(
                    dataObject));

            FlutterDesignerEditorSupport.PostCloseOutcome<FileObject> outcome =
                    FlutterDesignerPairPathCommands.rename(
                                    dataObject,
                                    "renamed_home",
                                    () -> {
                                        callbackRan.set(true);
                                        return dataObject.getPrimaryFile();
                                    })
                            .toCompletableFuture()
                            .get(5, TimeUnit.SECONDS);

            assertEquals(
                    FlutterDesignerEditorSupport.PostCloseStatus.INVALID_TOPOLOGY,
                    outcome.status());
            assertFalse(callbackRan.get());
            assertTrue(dataObject.getPairSaveCoordinator().canBeginPairRename(),
                    "failed close admission must release its reserved lease");
            assertTrue(Files.exists(fixture.dartPath()));
            assertTrue(Files.exists(fixture.modelPath()));
        } finally {
            permits.editorShellClosed(unregisteredShell);
        }
    }

    private CommandFixture fixture(String name) throws Exception {
        Path projectPath = temporaryDirectory.resolve(name);
        Path dartPath = projectPath.resolve("lib/home.dart");
        Path modelPath = projectPath.resolve(".fd_templates/home.fd");
        Files.createDirectories(dartPath.getParent());
        Files.createDirectories(modelPath.getParent());
        Files.writeString(projectPath.resolve("pubspec.yaml"), """
                name: pair_path_command_fixture
                dependencies:
                  flutter:
                    sdk: flutter
                """);
        DesignerFormTemplate template = new DesignerFormTemplateFactory()
                .create("home.dart", "HomePage");
        Files.write(dartPath, template.dartBytes());
        Files.write(modelPath, template.fdBytes());
        FileUtil.refreshFor(projectPath.toFile());
        FlutterProject project = FlutterDesignerTestProject.own(projectPath);
        TestOwnerQuery.register(projectPath, project);
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dart);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject
                .dataObject(dart, project);
        return new CommandFixture(dataObject, dartPath, modelPath);
    }

    private record CommandFixture(
            FlutterDesignerDataObject dataObject,
            Path dartPath,
            Path modelPath) {
    }

    /** Owner provider for plain Surefire, which does not open project UI. */
    @ServiceProvider(service = FileOwnerQueryImplementation.class, position = 2)
    public static final class TestOwnerQuery
            implements FileOwnerQueryImplementation {
        private static final Map<Path, Project> OWNERS =
                new ConcurrentHashMap<>();

        public TestOwnerQuery() {
        }

        static void register(Path root, Project owner) {
            OWNERS.put(root.toAbsolutePath().normalize(), owner);
        }

        @Override
        public Project getOwner(URI uri) {
            if (uri == null || !"file".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            try {
                return find(Path.of(uri));
            } catch (RuntimeException invalidUri) {
                return null;
            }
        }

        @Override
        public Project getOwner(FileObject file) {
            File local = FileUtil.toFile(file);
            return local == null ? null : find(local.toPath());
        }

        private static Project find(Path candidate) {
            Path normalized = candidate.toAbsolutePath().normalize();
            Path best = null;
            Project owner = null;
            for (Map.Entry<Path, Project> entry : OWNERS.entrySet()) {
                if (normalized.startsWith(entry.getKey())
                        && (best == null
                        || entry.getKey().getNameCount()
                        > best.getNameCount())) {
                    best = entry.getKey();
                    owner = entry.getValue();
                }
            }
            return owner;
        }
    }
}

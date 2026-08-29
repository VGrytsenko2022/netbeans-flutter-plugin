package dev.flutter.netbeans.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.api.FlutterProjectInfo;
import java.nio.file.Path;
import javax.swing.Action;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterThemeProjectResolverTest {
    @Test
    void selectedFlutterProjectWinsOverMainAndOpenFallbacks() {
        Project selected = project("selected");
        Project main = project("main");

        FlutterThemeProjectResolver.Resolution result =
                new FlutterThemeProjectResolver(
                        Lookups.fixed(selected),
                        () -> main,
                        () -> new Project[]{main}).resolve();

        assertTrue(result.resolved());
        assertEquals(root("selected"), result.projectRoot());
    }

    @Test
    void refusesAmbiguousSelectedOrOpenFlutterProjects() {
        Project first = project("first");
        Project second = project("second");

        FlutterThemeProjectResolver.Resolution selected =
                new FlutterThemeProjectResolver(
                        Lookups.fixed(first, second),
                        () -> null,
                        () -> new Project[0]).resolve();
        assertFalse(selected.resolved());
        assertTrue(selected.reason().contains("multiple Flutter projects"));

        FlutterThemeProjectResolver.Resolution opened =
                new FlutterThemeProjectResolver(
                        Lookup.EMPTY,
                        () -> null,
                        () -> new Project[]{first, second}).resolve();
        assertFalse(opened.resolved());
        assertTrue(opened.reason().contains("more than one Flutter project"));
    }

    @Test
    void usesOnlyOpenFlutterProjectAndIgnoresOtherProjectKinds() {
        Project flutter = project("flutter");
        Project unrelated = new Project() {
            @Override
            public FileObject getProjectDirectory() {
                return null;
            }

            @Override
            public Lookup getLookup() {
                return Lookup.EMPTY;
            }
        };

        FlutterThemeProjectResolver.Resolution result =
                new FlutterThemeProjectResolver(
                        Lookup.EMPTY,
                        () -> unrelated,
                        () -> new Project[]{unrelated, flutter}).resolve();

        assertTrue(result.resolved());
        assertEquals(root("flutter"), result.projectRoot());
    }

    @Test
    void contextAwareEditActionIsEnabledOnlyForOneResolvableFlutterProject() {
        Project first = project("first-action");
        Project second = project("second-action");
        EditFlutterThemesAction global = new EditFlutterThemesAction();

        Action enabled = global.createContextAwareInstance(Lookups.fixed(first));
        Action ambiguous = global.createContextAwareInstance(
                Lookups.fixed(first, second));

        assertTrue(enabled.isEnabled());
        assertFalse(ambiguous.isEnabled());
    }

    private static Project project(String name) {
        FlutterProjectInfo info = new FlutterProjectInfo(
                root(name), name, root(name).resolve("pubspec.yaml"));
        return new Project() {
            @Override
            public FileObject getProjectDirectory() {
                return null;
            }

            @Override
            public Lookup getLookup() {
                return Lookups.singleton(info);
            }
        };
    }

    private static Path root(String name) {
        return Path.of("build", "theme-resolver", name)
                .toAbsolutePath().normalize();
    }
}

package io.github.vgrytsenko2022.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectTypeDetectorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsCanonicalMetadataTypes() throws Exception {
        for (FlutterProjectType type : new FlutterProjectType[] {
            FlutterProjectType.APP,
            FlutterProjectType.MODULE,
            FlutterProjectType.PACKAGE,
            FlutterProjectType.PLUGIN
        }) {
            Path project = Files.createDirectory(temporaryDirectory.resolve(type.id()));
            Files.writeString(project.resolve(".metadata"), "project_type: " + type.id() + "\n");

            assertEquals(type, FlutterProjectTypeDetector.detect(project));
        }
    }

    @Test
    void legacyFallbackDistinguishesAppModulePluginAndPackage() throws Exception {
        Path app = Files.createDirectory(temporaryDirectory.resolve("legacy-app"));
        Files.createDirectories(app.resolve("lib"));
        Files.writeString(app.resolve("lib/main.dart"), "void main() {}\n");

        Path module = Files.createDirectory(temporaryDirectory.resolve("legacy-module"));
        Files.createDirectory(module.resolve(".android"));

        Path plugin = Files.createDirectory(temporaryDirectory.resolve("legacy-plugin"));
        Files.writeString(plugin.resolve("pubspec.yaml"), """
                name: sample_plugin
                flutter:
                  plugin:
                    platforms:
                      windows:
                """);

        Path library = Files.createDirectory(temporaryDirectory.resolve("legacy-package"));
        Files.createDirectories(library.resolve("lib"));

        assertEquals(FlutterProjectType.APP, FlutterProjectTypeDetector.detect(app));
        assertEquals(FlutterProjectType.MODULE, FlutterProjectTypeDetector.detect(module));
        assertEquals(FlutterProjectType.PLUGIN, FlutterProjectTypeDetector.detect(plugin));
        assertEquals(FlutterProjectType.PACKAGE, FlutterProjectTypeDetector.detect(library));
    }

    @Test
    void rejectsUnsafeOrOversizedMetadata() throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("unsafe"));
        Files.writeString(project.resolve(".metadata"), "x".repeat(64 * 1024 + 1));

        assertThrows(java.io.IOException.class,
                () -> FlutterProjectTypeDetector.detect(project));
    }

    @Test
    void existingMetadataWithoutProjectTypeIsNotTreatedAsLegacyApp() throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("unknown-modern"));
        Files.createDirectories(project.resolve("lib"));
        Files.writeString(project.resolve("lib/main.dart"), "void main() {}\n");
        Files.writeString(project.resolve(".metadata"), "version:\n  revision: test\n");

        assertEquals(FlutterProjectType.UNKNOWN, FlutterProjectTypeDetector.detect(project));
    }
}

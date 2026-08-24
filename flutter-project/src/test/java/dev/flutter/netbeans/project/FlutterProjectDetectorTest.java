package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectDetectorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsFlutterProjectAndReadsPackageName() throws Exception {
        Path project = temporaryDirectory.resolve("sample-folder");
        Files.createDirectories(project.resolve("lib"));
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: sample_app
                dependencies:
                  flutter:
                    sdk: flutter
                flutter:
                  uses-material-design: true
                """);

        var detected = new FlutterProjectDetector().detect(project).orElseThrow();

        assertEquals("sample_app", detected.name());
        assertEquals(project.toAbsolutePath().normalize(), detected.root());
    }

    @Test
    void rejectsPlainDartPackage() throws Exception {
        Path project = temporaryDirectory.resolve("dart-package");
        Files.createDirectories(project.resolve("lib"));
        Files.writeString(project.resolve("pubspec.yaml"), "name: dart_package\n");

        assertTrue(new FlutterProjectDetector().detect(project).isEmpty());
    }
}

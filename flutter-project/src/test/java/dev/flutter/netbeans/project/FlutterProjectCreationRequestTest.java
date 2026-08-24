package dev.flutter.netbeans.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterProjectCreationRequestTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void normalizesInputsAndBuildsTargetDirectory() {
        var request = new FlutterProjectCreationRequest(
                temporaryDirectory.resolve("."),
                " sample_app ",
                " com.example ",
                " Sample application. ");

        assertEquals("sample_app", request.projectName());
        assertEquals("com.example", request.organization());
        assertEquals(temporaryDirectory.resolve("sample_app").normalize(), request.targetDirectory());
    }

    @Test
    void rejectsInvalidDartPackageName() {
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectCreationRequest(
                temporaryDirectory,
                "Sample-App",
                "com.example",
                "Sample application."));
    }

    @Test
    void rejectsInvalidOrganization() {
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "example",
                "Sample application."));
    }
}

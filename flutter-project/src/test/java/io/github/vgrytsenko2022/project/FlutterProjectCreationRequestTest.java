package io.github.vgrytsenko2022.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Set;
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
        assertEquals(FlutterProjectPlatform.all(), request.platforms());
        assertEquals("android,ios,web,windows,macos,linux", request.platformsArgument());
    }

    @Test
    void copiesSelectionAndFormatsItInCanonicalOrder() {
        var selected = java.util.EnumSet.of(
                FlutterProjectPlatform.LINUX,
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.WEB);

        var request = new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "com.example",
                "Sample application.",
                selected);
        selected.clear();

        assertEquals(Set.of(
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.WEB,
                FlutterProjectPlatform.LINUX), request.platforms());
        assertEquals("android,web,linux", request.platformsArgument());
        assertThrows(UnsupportedOperationException.class,
                () -> request.platforms().clear());
    }

    @Test
    void rejectsEmptyPlatformSelection() {
        assertThrows(IllegalArgumentException.class, () -> new FlutterProjectCreationRequest(
                temporaryDirectory,
                "sample_app",
                "com.example",
                "Sample application.",
                Set.of()));
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

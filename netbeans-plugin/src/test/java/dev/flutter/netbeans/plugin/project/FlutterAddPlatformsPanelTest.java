package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterAddPlatformsPanelTest {
    @TempDir
    Path temporary;

    @Test
    void detectsCanonicalPlatformDirectoriesAndProtectsAnyOccupiedPath() throws Exception {
        Path project = Files.createDirectories(temporary.resolve("app"));
        Files.createDirectories(project.resolve("android"));
        Files.createDirectories(project.resolve("web"));
        Files.writeString(project.resolve("windows"), "user-owned path");
        Files.createDirectories(project.resolve("unrelated"));

        assertEquals(
                Set.of(
                        FlutterProjectPlatform.ANDROID,
                        FlutterProjectPlatform.WEB,
                        FlutterProjectPlatform.WINDOWS),
                FlutterAddPlatformsPanel.detectPresentPlatforms(project));
    }

    @Test
    void revalidationRemovesPlatformsWhosePathsAppearedAfterSelection() throws Exception {
        Path project = Files.createDirectories(temporary.resolve("app"));
        Set<FlutterProjectPlatform> requested = Set.of(
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.WEB,
                FlutterProjectPlatform.LINUX);
        Files.createDirectories(project.resolve("web"));

        assertEquals(
                Set.of(FlutterProjectPlatform.ANDROID, FlutterProjectPlatform.LINUX),
                FlutterAddPlatformsPanel.stillMissingSelection(project, requested));
    }

    @Test
    void allPresentCopyAlsoDescribesOccupiedNonDirectories() {
        assertEquals(
                "Cannot add platforms to C:/sample: all supported platform paths "
                + "already exist or are occupied.",
                Bundle.MSG_AllPlatformsPresent("C:/sample"));
    }
}

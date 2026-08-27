package dev.flutter.netbeans.plugin.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.flutter.netbeans.project.FlutterProjectPlatform;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

class FlutterProjectPlatformProviderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void publishesOnlyRealCanonicalDirectoriesAndIgnoresUnrelatedChanges()
            throws Exception {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Files.createDirectories(projectRoot.resolve("windows"));
        FileObject projectDirectory = fileObject(projectRoot);
        FlutterProjectPlatformProvider provider =
                new FlutterProjectPlatformProvider(projectDirectory, projectRoot);
        AtomicInteger changes = new AtomicInteger();
        provider.addChangeListener(event -> changes.incrementAndGet());

        assertEquals(Set.of(FlutterProjectPlatform.WINDOWS),
                provider.configuredPlatforms());
        provider.start();
        provider.start();
        assertEquals(Set.of(FlutterProjectPlatform.WINDOWS),
                provider.configuredPlatforms());
        assertEquals(0, changes.get());

        FileObject unrelated = projectDirectory.createFolder("assets");
        unrelated.createData("image.txt");
        assertEquals(Set.of(FlutterProjectPlatform.WINDOWS),
                provider.configuredPlatforms());
        assertEquals(0, changes.get());

        FileObject occupiedWebPath = projectDirectory.createData("web");
        assertEquals(Set.of(FlutterProjectPlatform.WINDOWS),
                provider.configuredPlatforms());
        assertEquals(0, changes.get());
        occupiedWebPath.delete();
        assertEquals(0, changes.get());

        projectDirectory.createFolder("android");
        assertEquals(Set.of(
                FlutterProjectPlatform.ANDROID,
                FlutterProjectPlatform.WINDOWS), provider.configuredPlatforms());
        assertEquals(1, changes.get());

        projectDirectory.getFileObject("android").delete();
        assertEquals(Set.of(FlutterProjectPlatform.WINDOWS),
                provider.configuredPlatforms());
        assertEquals(2, changes.get());

        projectDirectory.createFolder("ios");
        assertEquals(Set.of(
                FlutterProjectPlatform.IOS,
                FlutterProjectPlatform.WINDOWS), provider.configuredPlatforms());
        assertEquals(3, changes.get());

        projectDirectory.getFileObject("windows").delete();
        assertEquals(Set.of(FlutterProjectPlatform.IOS),
                provider.configuredPlatforms());
        assertEquals(4, changes.get());

        provider.close();
    }

    @Test
    void closeDetachesAndReopenRescansExactlyOnce() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        FileObject projectDirectory = fileObject(projectRoot);
        FlutterProjectPlatformProvider provider =
                new FlutterProjectPlatformProvider(projectDirectory, projectRoot);
        AtomicInteger changes = new AtomicInteger();
        provider.addChangeListener(event -> changes.incrementAndGet());

        provider.start();
        provider.start();
        assertEquals(Set.of(), provider.configuredPlatforms());
        assertEquals(0, changes.get());

        provider.close();
        provider.close();
        projectDirectory.createFolder("web");
        provider.refresh();
        assertEquals(Set.of(), provider.configuredPlatforms());
        assertEquals(0, changes.get());

        provider.start();
        provider.start();
        assertEquals(Set.of(FlutterProjectPlatform.WEB),
                provider.configuredPlatforms());
        assertEquals(1, changes.get());

        provider.close();
        projectDirectory.getFileObject("web").delete();
        assertEquals(Set.of(FlutterProjectPlatform.WEB),
                provider.configuredPlatforms());
        assertEquals(1, changes.get());

        provider.start();
        assertEquals(Set.of(), provider.configuredPlatforms());
        assertEquals(2, changes.get());
        provider.close();
    }

    private static FileObject fileObject(Path path) {
        FileUtil.refreshFor(path.toFile());
        FileObject result = FileUtil.toFileObject(path.toFile());
        if (result == null) {
            throw new IllegalStateException("No FileObject for " + path);
        }
        return result;
    }
}

package io.github.vgrytsenko2022.plugin.pubspec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPackageScannerTest {
    @TempDir
    Path workspace;

    @Test
    void findsValidPackagesAndSkipsCurrentExcludedAndMalformedDirectories() throws Exception {
        Path current = packageAt("apps/current", "current_app");
        packageAt("packages/alpha", "alpha");
        packageAt("packages/beta", "beta_package");
        packageAt("build/generated", "generated_package");
        packageAt(".dart_tool/cache", "cached_package");
        Path malformed = workspace.resolve("packages/malformed");
        Files.createDirectories(malformed);
        Files.writeString(malformed.resolve("pubspec.yaml"), "name: [broken\n");
        Path invalidName = workspace.resolve("packages/invalid_name");
        Files.createDirectories(invalidName);
        Files.writeString(invalidName.resolve("pubspec.yaml"), "name: Invalid-Name\n");

        List<LocalPackageScanner.LocalPackage> packages = new LocalPackageScanner()
                .scan(workspace, current, () -> false);

        assertEquals(List.of(
                new LocalPackageScanner.LocalPackage("alpha", "../../packages/alpha"),
                new LocalPackageScanner.LocalPackage("beta_package", "../../packages/beta")),
                packages);
    }

    @Test
    void choosesDeterministicallyWhenTwoDirectoriesDeclareTheSameName() throws Exception {
        Path current = packageAt("app", "current_app");
        packageAt("a/short", "shared_package");
        packageAt("z/short", "shared_package");

        List<LocalPackageScanner.LocalPackage> packages = new LocalPackageScanner()
                .scan(workspace, current, () -> false);

        assertEquals(List.of(new LocalPackageScanner.LocalPackage(
                "shared_package", "../a/short")), packages);
    }

    @Test
    void honoursDepthAndDirectoryBounds() throws Exception {
        Path current = packageAt("current", "current_app");
        packageAt("direct", "direct_package");
        packageAt("nested/deep", "deep_package");

        assertEquals(List.of(new LocalPackageScanner.LocalPackage(
                        "direct_package", "../direct")),
                new LocalPackageScanner(1, 512)
                        .scan(workspace, current, () -> false));
        assertEquals(List.of(), new LocalPackageScanner(8, 1)
                .scan(workspace, current, () -> false));
    }

    @Test
    void returnsNothingWhenCancelled() throws Exception {
        Path current = packageAt("current", "current_app");
        packageAt("packages/alpha", "alpha");

        assertEquals(List.of(), new LocalPackageScanner()
                .scan(workspace, current, () -> true));
    }

    private Path packageAt(String relativePath, String name) throws Exception {
        Path directory = workspace.resolve(relativePath);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("pubspec.yaml"), "name: " + name + "\n");
        return directory;
    }
}

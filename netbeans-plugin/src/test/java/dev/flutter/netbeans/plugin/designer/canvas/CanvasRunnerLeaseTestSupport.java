package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cross-package fixture for exercising real runtime leases in session tests. */
public final class CanvasRunnerLeaseTestSupport {
    private CanvasRunnerLeaseTestSupport() {
    }

    public static CanvasRunnerBuildResult createResult(Path root) throws IOException {
        Path source = root.toAbsolutePath().normalize();
        Path generation = source.resolve("runtime-test-generation");
        Path runtime = generation.resolve("runtime");
        Files.createDirectories(runtime);
        Files.write(
                generation.resolveSibling(generation.getFileName() + ".lease"),
                new byte[] {1});
        CanvasRunnerRuntimeLease lease = CanvasRunnerRuntimeLease.acquire(runtime);
        return new CanvasRunnerBuildResult(
                runtime.resolve("runner.exe"),
                source,
                new CanvasRunnerCacheIdentity(
                        "a".repeat(64), "test-engine", "b".repeat(64)),
                false,
                "",
                lease);
    }

    public static boolean isGenerationLeased(Path root) {
        return CanvasRunnerRuntimeLease.isLeasedInJvm(
                root.toAbsolutePath().normalize().resolve("runtime-test-generation"));
    }
}

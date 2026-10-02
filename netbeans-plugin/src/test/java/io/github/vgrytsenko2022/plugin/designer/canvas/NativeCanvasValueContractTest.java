package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatform;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerLaunch;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasSurfaceMetrics;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

class NativeCanvasValueContractTest {
    @Test
    void parentIdentityIsBoundedOpaqueAndPlatformSpecific() {
        NativeCanvasParentHandle handle = new NativeCanvasParentHandle(
                NativeCanvasPlatform.WINDOWS,
                "0x0000000000000123");

        assertEquals("WINDOWS:<opaque>", handle.toString());
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasParentHandle(NativeCanvasPlatform.UNKNOWN, "value"));
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasParentHandle(NativeCanvasPlatform.WINDOWS, "line\nbreak"));
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasParentHandle(NativeCanvasPlatform.WINDOWS, "x".repeat(129)));
    }

    @Test
    void surfaceMetricsRejectInvalidPhysicalValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasSurfaceMetrics(-1, 0, 1_000_000));
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasSurfaceMetrics(0, 0, 0));
    }

    @Test
    void runnerContractRejectsWindowsAdsDriveAndTraversalPaths() {
        assertThrows(IllegalArgumentException.class, () -> runtimeLayout(
                "data/app.so:evil"));
        assertThrows(IllegalArgumentException.class, () -> runtimeLayout(
                "C:/data/app.so"));
        assertThrows(IllegalArgumentException.class, () -> runtimeLayout(
                "data/app?.so"));
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasRunnerContract.BuildTarget(
                        "windows",
                        "release",
                        List.of("--output=../evil"),
                        "build/windows",
                        "runner.exe",
                        12));
    }

    @Test
    void runtimeFilePrefixRequiresItsOwnDirectoryToBeAllowed() {
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasRunnerContract.RuntimeLayout(
                        Set.of("runner.exe"),
                        Set.of("runner.exe"),
                        Set.of("plugins/"),
                        Set.of(""),
                        Set.of(),
                        Set.of()));
    }

    @Test
    void runnerLaunchCannotReplaceExecutableWorkingDirectoryOrFirstArgument() {
        Path executable = Path.of("target", "runner", "runner.exe")
                .toAbsolutePath().normalize();

        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasRunnerLaunch(
                        executable,
                        executable.getParent().resolve("other"),
                        List.of(executable.toString())));
        assertThrows(IllegalArgumentException.class, () ->
                new NativeCanvasRunnerLaunch(
                        executable,
                        executable.getParent(),
                        List.of(executable.resolveSibling("other.exe").toString())));
    }

    private static NativeCanvasRunnerContract.RuntimeLayout runtimeLayout(
            String required) {
        return new NativeCanvasRunnerContract.RuntimeLayout(
                Set.of(required),
                Set.of(required),
                Set.of(),
                Set.of("", "data"),
                Set.of(),
                Set.of());
    }
}

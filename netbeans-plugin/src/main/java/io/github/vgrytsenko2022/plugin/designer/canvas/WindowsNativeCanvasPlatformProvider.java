package io.github.vgrytsenko2022.plugin.designer.canvas;

import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasHost;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasParentHandle;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatform;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasPlatformProvider;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerContract;
import io.github.vgrytsenko2022.plugin.designer.canvas.spi.NativeCanvasRunnerLaunch;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.openide.util.lookup.ServiceProvider;

/** Native child-window Canvas provider for Windows. */
@ServiceProvider(service = NativeCanvasPlatformProvider.class, position = 100)
public final class WindowsNativeCanvasPlatformProvider
        implements NativeCanvasPlatformProvider {
    private static final NativeCanvasRunnerContract RUNNER_CONTRACT =
            new NativeCanvasRunnerContract(
                    NativeCanvasPlatform.WINDOWS,
                    new NativeCanvasRunnerContract.BuildTarget(
                            "windows",
                            "release",
                            List.of("--no-tree-shake-icons"),
                            "build/windows",
                            "netbeans_flutter_canvas_runner.exe",
                            12),
                    new NativeCanvasRunnerContract.RuntimeLayout(
                            Set.of(
                                    "netbeans_flutter_canvas_runner.exe",
                                    "flutter_windows.dll",
                                    "data/icudtl.dat",
                                    "data/app.so",
                                    "data/flutter_assets/AssetManifest.bin",
                                    "data/flutter_assets/FontManifest.json",
                                    "data/flutter_assets/NativeAssetsManifest.json",
                                    "data/flutter_assets/NOTICES.Z"),
                            Set.of(
                                    "netbeans_flutter_canvas_runner.exe",
                                    "flutter_windows.dll",
                                    "data/icudtl.dat",
                                    "data/app.so"),
                            Set.of("data/flutter_assets/"),
                            Set.of("", "data", "data/flutter_assets"),
                            Set.of("data/flutter_assets/"),
                            Set.of("netbeans_flutter_canvas_runner.pdb")),
                    new NativeCanvasRunnerContract.CachePolicy(
                            NativeCanvasRunnerContract.CacheRoot.USER_TEMPORARY,
                            "nb-fcr",
                            "windows-release-dynamic-icons-no-tree-shake-v2"));

    @Override
    public NativeCanvasPlatform platform() {
        return NativeCanvasPlatform.WINDOWS;
    }

    @Override
    public boolean isFallbackProvider() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String availabilityReason() {
        return "Windows native child-surface hosting is available.";
    }

    @Override
    public NativeCanvasHost createHost() {
        return new WindowsNativeCanvasHost();
    }

    @Override
    public Optional<NativeCanvasRunnerContract> runnerContract() {
        return Optional.of(RUNNER_CONTRACT);
    }

    @Override
    public String targetDescription() {
        return "embedded Windows FlutterView";
    }

    @Override
    public NativeCanvasRunnerLaunch createLaunch(
            Path executable,
            NativeCanvasParentHandle parentHandle,
            long hostProcessId,
            long surfaceEpoch,
            String nonce) {
        Objects.requireNonNull(executable, "executable");
        Objects.requireNonNull(parentHandle, "parentHandle");
        NativeCanvasRunnerContract contract = RUNNER_CONTRACT;
        if (!executable.isAbsolute()
                || !contract.buildTarget().executableName().equals(
                        executable.getFileName().toString())
                || parentHandle.platform() != NativeCanvasPlatform.WINDOWS
                || !parentHandle.encodedValue().matches("0x[0-9A-F]{16}")
                || hostProcessId <= 0 || hostProcessId > 0xffff_ffffL
                || surfaceEpoch <= 0 || nonce == null
                || !nonce.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid Windows native Canvas launch identity");
        }
        List<String> command = new ArrayList<>(5);
        command.add(executable.normalize().toString());
        command.add("--netbeans-parent-hwnd=" + parentHandle.encodedValue());
        command.add("--netbeans-host-pid=" + hostProcessId);
        command.add("--netbeans-surface-epoch=" + surfaceEpoch);
        command.add("--netbeans-session-nonce=" + nonce);
        Path normalized = executable.toAbsolutePath().normalize();
        return new NativeCanvasRunnerLaunch(
                normalized, normalized.getParent(), command);
    }
}

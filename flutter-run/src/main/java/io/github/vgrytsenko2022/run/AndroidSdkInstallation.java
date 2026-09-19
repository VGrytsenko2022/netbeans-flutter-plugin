package io.github.vgrytsenko2022.run;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** An immutable snapshot of an Android SDK root and the tools found below it. */
public record AndroidSdkInstallation(
        Path root,
        Map<AndroidSdkTool, Path> executables,
        String source) {

    public AndroidSdkInstallation {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(executables, "executables");
        root = root.toAbsolutePath().normalize();
        EnumMap<AndroidSdkTool, Path> normalized = new EnumMap<>(AndroidSdkTool.class);
        executables.forEach((tool, path) -> {
            if (tool != null && path != null) {
                normalized.put(tool, path.toAbsolutePath().normalize());
            }
        });
        executables = Map.copyOf(normalized);
        source = source == null || source.isBlank() ? "unknown" : source.strip();
    }

    public Optional<Path> executable(AndroidSdkTool tool) {
        return Optional.ofNullable(executables.get(Objects.requireNonNull(tool, "tool")));
    }

    public Optional<Path> sdkManager() {
        return executable(AndroidSdkTool.SDK_MANAGER);
    }

    public Optional<Path> avdManager() {
        return executable(AndroidSdkTool.AVD_MANAGER);
    }

    public Optional<Path> emulator() {
        return executable(AndroidSdkTool.EMULATOR);
    }

    public Optional<Path> adb() {
        return executable(AndroidSdkTool.ADB);
    }

    public List<AndroidSdkTool> missingTools() {
        return java.util.Arrays.stream(AndroidSdkTool.values())
                .filter(tool -> !executables.containsKey(tool))
                .toList();
    }

    public boolean complete() {
        return missingTools().isEmpty();
    }
}

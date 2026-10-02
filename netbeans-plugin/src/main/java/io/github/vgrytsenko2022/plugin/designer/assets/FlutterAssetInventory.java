package io.github.vgrytsenko2022.plugin.designer.assets;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable result of one bounded Flutter asset inventory operation. */
public final class FlutterAssetInventory {
    static final int MAX_PACKAGE_WATCH_ROOTS = 4_096;
    public static final String SELECTION_ALGORITHM =
            "flutter-3.44.8-058e0af2c2b57e369d905a03ac9748b0ebf543c6-"
                    + "asset-image-dpr-low-limit-2.0";

    private final List<FlutterAsset> assets;
    private final List<FlutterAssetDiagnostic> diagnostics;
    private final String fingerprintSha256;
    private final Map<FlutterAssetId, FlutterAsset> assetsById;
    private final List<Path> packageWatchRoots;

    FlutterAssetInventory(
            List<FlutterAsset> assets,
            List<FlutterAssetDiagnostic> diagnostics,
            String fingerprintSha256) {
        this(assets, diagnostics, fingerprintSha256, List.of());
    }

    FlutterAssetInventory(
            List<FlutterAsset> assets,
            List<FlutterAssetDiagnostic> diagnostics,
            String fingerprintSha256,
            List<Path> packageWatchRoots) {
        this.assets = List.copyOf(Objects.requireNonNull(assets, "assets"));
        this.diagnostics = List.copyOf(
                Objects.requireNonNull(diagnostics, "diagnostics"));
        this.fingerprintSha256 = Objects.requireNonNull(
                fingerprintSha256, "fingerprintSha256");
        this.assetsById = this.assets.stream().collect(Collectors.toUnmodifiableMap(
                FlutterAsset::id,
                Function.identity()));
        Objects.requireNonNull(packageWatchRoots, "packageWatchRoots");
        if (packageWatchRoots.size() > MAX_PACKAGE_WATCH_ROOTS) {
            throw new IllegalArgumentException(
                    "Package watch root count exceeds "
                    + MAX_PACKAGE_WATCH_ROOTS);
        }
        this.packageWatchRoots = packageWatchRoots.stream()
                .map(root -> Objects.requireNonNull(root, "packageWatchRoot")
                        .toAbsolutePath().normalize())
                .distinct()
                .sorted()
                .toList();
    }

    public List<FlutterAsset> assets() {
        return assets;
    }

    public List<FlutterAssetDiagnostic> diagnostics() {
        return diagnostics;
    }

    public String fingerprintSha256() {
        return fingerprintSha256;
    }

    public String selectionAlgorithm() {
        return SELECTION_ALGORITHM;
    }

    public Optional<FlutterAsset> find(FlutterAssetId id) {
        return Optional.ofNullable(assetsById.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * Returns bounded, real package roots used only to maintain live cache
     * invalidation. These process-local paths are deliberately excluded from
     * the inventory fingerprint and every persisted or Canvas-facing model.
     */
    public List<Path> internalPackageWatchRoots() {
        return packageWatchRoots;
    }
}

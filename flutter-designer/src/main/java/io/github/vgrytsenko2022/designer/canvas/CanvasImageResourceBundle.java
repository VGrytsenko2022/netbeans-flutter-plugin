package io.github.vgrytsenko2022.designer.canvas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable revision-scoped project-asset snapshot for one Canvas publication.
 *
 * <p>Resources are content-addressed, deduplicated and bounded as a whole.
 * The bundle never retains filesystem paths or performs I/O.</p>
 */
public final class CanvasImageResourceBundle {
    public static final int MAX_ASSETS = 256;
    public static final int MAX_RESOURCES = 256;
    public static final int MAX_TOTAL_ENCODED_BYTES = 16 * 1024 * 1024;
    private static final CanvasImageResourceBundle EMPTY =
            new CanvasImageResourceBundle(List.of(), List.of(), List.of());

    private final List<CanvasImageAsset> assets;
    private final List<CanvasImageResource> resources;
    private final Map<CanvasImageAssetId, CanvasImageAsset> assetsById;
    private final Map<String, CanvasImageResource> resourcesById;
    private final List<CanvasImageResolutionIssue> issues;
    private final Map<CanvasImageAssetId, CanvasImageResolutionIssue> issuesById;
    private final int totalEncodedBytes;
    private final String fingerprintSha256;

    public CanvasImageResourceBundle(
            List<CanvasImageAsset> assets,
            List<CanvasImageResource> resources) {
        this(assets, resources, List.of());
    }

    public CanvasImageResourceBundle(
            List<CanvasImageAsset> assets,
            List<CanvasImageResource> resources,
            List<CanvasImageResolutionIssue> issues) {
        Objects.requireNonNull(assets, "assets");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(issues, "issues");
        if (assets.size() > MAX_ASSETS) {
            throw new IllegalArgumentException(
                    "Canvas image bundle contains too many logical assets");
        }
        if (resources.size() > MAX_RESOURCES) {
            throw new IllegalArgumentException(
                    "Canvas image bundle contains too many encoded resources");
        }
        if (issues.size() > MAX_ASSETS) {
            throw new IllegalArgumentException(
                    "Canvas image bundle contains too many resolution issues");
        }

        ArrayList<CanvasImageResource> orderedResources =
                new ArrayList<>(resources);
        orderedResources.sort(null);
        LinkedHashMap<String, CanvasImageResource> resourceIndex =
                new LinkedHashMap<>();
        long byteTotal = 0;
        for (CanvasImageResource resource : orderedResources) {
            Objects.requireNonNull(resource, "resource");
            if (resourceIndex.putIfAbsent(resource.resourceId(), resource) != null) {
                throw new IllegalArgumentException(
                        "Canvas image bundle contains a duplicate resource id");
            }
            byteTotal += resource.encodedByteLength();
            if (byteTotal > MAX_TOTAL_ENCODED_BYTES) {
                throw new IllegalArgumentException(
                        "Canvas image bundle exceeds the total encoded-image budget");
            }
        }

        ArrayList<CanvasImageAsset> orderedAssets = new ArrayList<>(assets);
        orderedAssets.sort(null);
        LinkedHashMap<CanvasImageAssetId, CanvasImageAsset> assetIndex =
                new LinkedHashMap<>();
        Set<String> referencedResources = new HashSet<>();
        for (CanvasImageAsset asset : orderedAssets) {
            Objects.requireNonNull(asset, "asset");
            if (assetIndex.putIfAbsent(asset.assetId(), asset) != null) {
                throw new IllegalArgumentException(
                        "Canvas image bundle contains a duplicate logical asset");
            }
            requireResource(resourceIndex, asset.exactResourceId(), asset.assetId());
            referencedResources.add(asset.exactResourceId());
            for (CanvasImageVariant variant : asset.variants()) {
                requireResource(resourceIndex, variant.resourceId(), asset.assetId());
                referencedResources.add(variant.resourceId());
            }
        }
        if (!referencedResources.equals(resourceIndex.keySet())) {
            throw new IllegalArgumentException(
                    "Canvas image bundle must not contain unreferenced resources");
        }

        ArrayList<CanvasImageResolutionIssue> orderedIssues =
                new ArrayList<>(issues);
        orderedIssues.sort(null);
        LinkedHashMap<CanvasImageAssetId, CanvasImageResolutionIssue> issueIndex =
                new LinkedHashMap<>();
        for (CanvasImageResolutionIssue issue : orderedIssues) {
            Objects.requireNonNull(issue, "issue");
            if (assetIndex.containsKey(issue.assetId())) {
                throw new IllegalArgumentException(
                        "A resolved Canvas image asset cannot also be unavailable");
            }
            if (issueIndex.putIfAbsent(issue.assetId(), issue) != null) {
                throw new IllegalArgumentException(
                        "Canvas image bundle contains duplicate issues for one asset");
            }
        }

        this.assets = List.copyOf(orderedAssets);
        this.resources = List.copyOf(orderedResources);
        this.assetsById = Map.copyOf(assetIndex);
        this.resourcesById = Map.copyOf(resourceIndex);
        this.issues = List.copyOf(orderedIssues);
        this.issuesById = Map.copyOf(issueIndex);
        this.totalEncodedBytes = (int) byteTotal;
        this.fingerprintSha256 = fingerprint(
                this.assets, this.resources, this.issues);
    }

    public static CanvasImageResourceBundle empty() {
        return EMPTY;
    }

    public List<CanvasImageAsset> assets() {
        return assets;
    }

    public List<CanvasImageResource> resources() {
        return resources;
    }

    public Optional<CanvasImageAsset> find(CanvasImageAssetId assetId) {
        Objects.requireNonNull(assetId, "assetId");
        return Optional.ofNullable(assetsById.get(assetId));
    }

    public Optional<CanvasImageResource> findResource(String resourceId) {
        Objects.requireNonNull(resourceId, "resourceId");
        return Optional.ofNullable(resourcesById.get(resourceId));
    }

    public List<CanvasImageResolutionIssue> issues() {
        return issues;
    }

    public Optional<CanvasImageResolutionIssue> findIssue(
            CanvasImageAssetId assetId) {
        Objects.requireNonNull(assetId, "assetId");
        return Optional.ofNullable(issuesById.get(assetId));
    }

    public int totalEncodedBytes() {
        return totalEncodedBytes;
    }

    public String fingerprintSha256() {
        return fingerprintSha256;
    }

    public boolean isEmpty() {
        return assets.isEmpty() && issues.isEmpty();
    }

    private static void requireResource(
            Map<String, CanvasImageResource> resources,
            String resourceId,
            CanvasImageAssetId assetId) {
        if (!resources.containsKey(resourceId)) {
            throw new IllegalArgumentException(
                    "Canvas asset " + assetId.externalName()
                    + " references a missing content-addressed resource");
        }
    }

    private static String fingerprint(
            List<CanvasImageAsset> assets,
            List<CanvasImageResource> resources,
            List<CanvasImageResolutionIssue> issues) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (CanvasImageResource resource : resources) {
                update(digest, "resource\0");
                update(digest, resource.resourceId());
                update(digest, "\0" + resource.format().name());
                update(digest, "\0" + resource.pixelWidth());
                update(digest, "\0" + resource.pixelHeight() + "\n");
            }
            for (CanvasImageAsset asset : assets) {
                update(digest, "asset\0");
                update(digest, asset.assetId().externalName());
                update(digest, "\0" + asset.exactResourceId());
                for (CanvasImageVariant variant : asset.variants()) {
                    update(digest, "\0" + variant.scale().toPlainString());
                    update(digest, "\0" + variant.resourceId());
                }
                update(digest, "\n");
            }
            for (CanvasImageResolutionIssue issue : issues) {
                update(digest, "issue\0");
                update(digest, issue.assetId().externalName());
                update(digest, "\0" + issue.code().wireName());
                update(digest, "\0" + issue.reason() + "\n");
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }
}

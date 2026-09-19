package io.github.vgrytsenko2022.plugin.designer.assets;

import io.github.vgrytsenko2022.designer.canvas.CanvasImageAsset;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageAssetId;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageFormat;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResolutionIssue;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResource;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResourceBundle;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageVariant;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterImageAssetChoices;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Projects only image assets referenced by one typed Designer document into a
 * strict, content-addressed Canvas snapshot.
 */
public final class FlutterDesignerCanvasImageProjector {
    private static final WidgetTypeId IMAGE_WIDGET_TYPE =
            new WidgetTypeId("flutter.widgets.Image");
    private static final PropertyName IMAGE_PROPERTY = new PropertyName("image");
    private static final String UNRESOLVED_IMAGE_ASSET_NAME =
            PropertyValue.ImageProviderValue.unresolved().assetName();
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern FILE_URI = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])file:/+[^\\s,;]+");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])[a-z]:[\\\\/][^\\s,;]+");
    private static final Pattern UNC_PATH = Pattern.compile(
            "\\\\\\\\[^\\s,;]+");
    private static final Pattern POSIX_ABSOLUTE_PATH = Pattern.compile(
            "(?<![A-Za-z0-9_])/(?:[^\\s,;]+)");
    private static final int MAX_REASON_ASSET_NAME = 512;
    private static final int MAX_EXTERNAL_FAILURE_REASON = 384;

    /** Builds deterministic property choices without exposing filesystem paths. */
    public FlutterImageAssetChoices choices(FlutterAssetInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        List<FlutterImageAssetChoices.Choice> choices = inventory.assets().stream()
                .filter(asset -> !asset.id().logicalPath().equals(
                        UNRESOLVED_IMAGE_ASSET_NAME))
                .map(asset -> new FlutterImageAssetChoices.Choice(
                        Optional.ofNullable(asset.id().packageName()),
                        asset.id().logicalPath(),
                        asset.id().displayName()))
                .toList();
        boolean reservedIdentityExcluded = choices.isEmpty()
                && inventory.assets().stream().anyMatch(asset ->
                        asset.id().logicalPath().equals(
                                UNRESOLVED_IMAGE_ASSET_NAME));
        Optional<String> unavailableReason = choices.isEmpty()
                ? Optional.of(choiceUnavailableReason(
                        inventory, reservedIdentityExcluded))
                : Optional.empty();
        return new FlutterImageAssetChoices(choices, unavailableReason);
    }

    /** Returns whether the typed document references at least one image asset. */
    public boolean referencesImages(DesignerDocument document) {
        Objects.requireNonNull(document, "document");
        return !referencedProviders(document).isEmpty();
    }

    /** Redacts filesystem locations and bounds a reason before it reaches UI. */
    public String sanitizeExternalReason(String reason) {
        return boundedExternalReason(reason);
    }

    /**
     * Builds an exact-coverage, resource-free placeholder bundle when the
     * project inventory operation itself is unavailable.
     */
    public CanvasImageResourceBundle unavailable(
            DesignerDocument document,
            CanvasImageResolutionIssue.Code code,
            String reason) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(code, "code");
        String boundedReason = sanitizeExternalReason(reason);
        TreeMap<CanvasImageAssetId, ProviderUse> referenced =
                referencedProviders(document);
        if (referenced.size() > CanvasImageResourceBundle.MAX_ASSETS) {
            throw new IllegalArgumentException(
                    "Project unavailable Canvas images: " + referenced.size()
                            + " unique logical assets exceed strict bundle limit "
                            + CanvasImageResourceBundle.MAX_ASSETS);
        }
        List<CanvasImageResolutionIssue> issues = referenced.entrySet().stream()
                .map(entry -> entry.getValue().unresolved()
                        ? unresolvedIssue(entry.getKey())
                        : new CanvasImageResolutionIssue(
                                entry.getKey(),
                                code,
                                "Resolve Canvas image "
                                        + boundedAssetExternalName(entry.getKey())
                                        + ": " + boundedReason))
                .toList();
        return new CanvasImageResourceBundle(List.of(), List.of(), issues);
    }

    /**
     * Resolves all and only referenced image providers for the supplied DPR.
     */
    public ProjectionResult project(
            DesignerDocument document,
            FlutterAssetInventory inventory,
            double devicePixelRatio) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(inventory, "inventory");
        if (!Double.isFinite(devicePixelRatio) || devicePixelRatio <= 0.0) {
            throw new IllegalArgumentException(
                    "devicePixelRatio must be finite and positive");
        }

        TreeMap<CanvasImageAssetId, ProviderUse> referenced =
                referencedProviders(document);
        if (referenced.size() > CanvasImageResourceBundle.MAX_ASSETS) {
            throw new IllegalArgumentException(
                    "Project Canvas images: " + referenced.size()
                            + " unique logical assets exceed strict bundle limit "
                            + CanvasImageResourceBundle.MAX_ASSETS);
        }

        InventoryIndex index = InventoryIndex.create(inventory);
        List<CanvasImageAsset> assets = new ArrayList<>();
        List<CanvasImageResolutionIssue> issues = new ArrayList<>();
        TreeMap<String, CanvasImageResource> resources = new TreeMap<>();
        int encodedBytes = 0;

        for (Map.Entry<CanvasImageAssetId, ProviderUse> entry
                : referenced.entrySet()) {
            CanvasImageAssetId assetId = entry.getKey();
            if (entry.getValue().unresolved()) {
                issues.add(unresolvedIssue(assetId));
                continue;
            }
            Resolution resolution = resolve(
                    assetId,
                    entry.getValue(),
                    index,
                    inventory,
                    devicePixelRatio);
            if (resolution.issueCode().isPresent()) {
                issues.add(issue(assetId, resolution.issueCode().orElseThrow()));
                continue;
            }

            AssetPlan plan = resolution.plan().orElseThrow();
            boolean collision = plan.resources().stream().anyMatch(resource -> {
                CanvasImageResource existing = resources.get(resource.resourceId());
                return existing != null && !existing.equals(resource);
            });
            if (collision) {
                issues.add(issue(assetId, CanvasImageResolutionIssue.Code.CORRUPT));
                continue;
            }
            List<CanvasImageResource> additions = plan.resources().stream()
                    .filter(resource -> !resources.containsKey(resource.resourceId()))
                    .toList();
            long addedBytes = additions.stream()
                    .mapToLong(CanvasImageResource::encodedByteLength)
                    .sum();
            if (assets.size() >= CanvasImageResourceBundle.MAX_ASSETS
                    || resources.size() + additions.size()
                    > CanvasImageResourceBundle.MAX_RESOURCES
                    || addedBytes > CanvasImageResourceBundle.MAX_TOTAL_ENCODED_BYTES
                    || encodedBytes > CanvasImageResourceBundle.MAX_TOTAL_ENCODED_BYTES
                            - addedBytes) {
                issues.add(issue(
                        assetId,
                        CanvasImageResolutionIssue.Code.BUDGET_EXCEEDED));
                continue;
            }
            assets.add(plan.asset());
            for (CanvasImageResource resource : additions) {
                resources.put(resource.resourceId(), resource);
                encodedBytes += resource.encodedByteLength();
            }
        }

        CanvasImageResourceBundle bundle = new CanvasImageResourceBundle(
                assets,
                List.copyOf(resources.values()),
                issues);
        FlutterImageAssetChoices assetChoices = choices(inventory);
        return new ProjectionResult(
                inventory.fingerprintSha256(),
                projectionFingerprint(inventory.fingerprintSha256(), bundle),
                assetChoices,
                bundle);
    }

    private static TreeMap<CanvasImageAssetId, ProviderUse> referencedProviders(
            DesignerDocument document) {
        TreeMap<CanvasImageAssetId, ProviderUse> referenced = new TreeMap<>();
        ArrayDeque<WidgetNode> pending = new ArrayDeque<>();
        Set<WidgetNode> visited = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        pending.add(document.root());
        while (!pending.isEmpty()) {
            WidgetNode node = pending.removeFirst();
            if (!visited.add(node)) {
                continue;
            }
            for (PropertyValue value : node.properties().values()) {
                if (value instanceof PropertyValue.ImageProviderValue provider) {
                    addProvider(referenced, provider);
                } else if (value instanceof PropertyValue.BoxDecorationValue decoration
                        && decoration.image().isPresent()) {
                    addDecorationImage(
                            referenced,
                            decoration.image().orElseThrow());
                }
            }
            addDirectImageCenterSlice(referenced, node);
            for (WidgetSlot slot : node.slots().values()) {
                switch (slot) {
                    case WidgetSlot.SingleSlot single ->
                        single.child().ifPresent(pending::addLast);
                    case WidgetSlot.ListSlot list -> pending.addAll(list.children());
                }
            }
        }
        return referenced;
    }

    private static void addProvider(
            TreeMap<CanvasImageAssetId, ProviderUse> referenced,
            PropertyValue.ImageProviderValue provider) {
        CanvasImageAssetId id = new CanvasImageAssetId(
                provider.packageName(), provider.assetName());
        ProviderUse current = referenced.getOrDefault(id, ProviderUse.NONE);
        if (provider.isUnresolved()) {
            referenced.put(id, current.withUnresolved());
            return;
        }
        referenced.put(id, switch (provider.providerKind()) {
            case ASSET -> current.withAsset();
            case EXACT_ASSET -> current.withExact();
        });
    }

    private static void addDecorationImage(
            TreeMap<CanvasImageAssetId, ProviderUse> referenced,
            PropertyValue.DecorationImageValue decorationImage) {
        PropertyValue.ImageProviderValue provider = decorationImage.image();
        addProvider(referenced, provider);
        if (decorationImage.centerSlice().isEmpty()) {
            return;
        }
        CanvasImageAssetId id = new CanvasImageAssetId(
                provider.packageName(), provider.assetName());
        referenced.computeIfPresent(id, (ignored, current) ->
                current.withCenterSlice(new CenterSliceUse(
                        provider,
                        decorationImage.centerSlice().orElseThrow(),
                        decorationImage.scale())));
    }

    private static void addDirectImageCenterSlice(
            TreeMap<CanvasImageAssetId, ProviderUse> referenced,
            WidgetNode node) {
        if (!IMAGE_WIDGET_TYPE.equals(node.type())
                || !(node.properties().get(IMAGE_PROPERTY)
                instanceof PropertyValue.ImageProviderValue provider)) {
            return;
        }
        Optional<BigDecimal> left = numericProperty(node, "centerSliceLeft");
        Optional<BigDecimal> top = numericProperty(node, "centerSliceTop");
        Optional<BigDecimal> right = numericProperty(node, "centerSliceRight");
        Optional<BigDecimal> bottom = numericProperty(node, "centerSliceBottom");
        if (left.isEmpty() || top.isEmpty() || right.isEmpty() || bottom.isEmpty()) {
            return;
        }
        PropertyValue.DecorationImageValue.Rect slice =
                new PropertyValue.DecorationImageValue.Rect(
                        left.orElseThrow(),
                        top.orElseThrow(),
                        right.orElseThrow(),
                        bottom.orElseThrow());
        CanvasImageAssetId id = new CanvasImageAssetId(
                provider.packageName(), provider.assetName());
        referenced.computeIfPresent(id, (ignored, current) ->
                current.withCenterSlice(new CenterSliceUse(
                        provider, slice, BigDecimal.ONE)));
    }

    private static Optional<BigDecimal> numericProperty(
            WidgetNode node,
            String name) {
        PropertyValue value = node.properties().get(
                new PropertyName(name));
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case PropertyValue.DoubleValue number -> Optional.of(number.value());
            case PropertyValue.IntegerValue number ->
                Optional.of(new BigDecimal(number.value()));
            default -> Optional.empty();
        };
    }

    private static Resolution resolve(
            CanvasImageAssetId assetId,
            ProviderUse use,
            InventoryIndex index,
            FlutterAssetInventory inventory,
            double devicePixelRatio) {
        FlutterAsset logicalAsset = index.logicalAssets().get(assetId);
        ExactVariant exactVariant = index.exactVariants().get(assetId);
        if (exactVariant != null && exactVariant.ambiguous()) {
            return Resolution.issue(CanvasImageResolutionIssue.Code.CORRUPT);
        }
        if (use.asset() && logicalAsset == null) {
            return Resolution.issue(classifyUnavailable(
                    assetId, inventory));
        }
        if (use.exact() && exactVariant == null) {
            CanvasImageResolutionIssue.Code code = classifyUnavailable(
                    assetId, inventory);
            if (code == CanvasImageResolutionIssue.Code.UNDECLARED
                    && logicalAsset != null) {
                code = CanvasImageResolutionIssue.Code.MISSING;
            }
            return Resolution.issue(code);
        }

        FlutterAssetVariant selected = use.asset()
                ? logicalAsset.selectVariant(devicePixelRatio)
                : null;
        FlutterAssetVariant exact = use.exact()
                ? exactVariant.variant()
                : null;

        if (!centerSlicesFit(use, selected, exact)) {
            return Resolution.issue(CanvasImageResolutionIssue.Code.CORRUPT);
        }

        TreeMap<String, CanvasImageResource> resources = new TreeMap<>();
        if (selected != null) {
            Optional<CanvasImageResolutionIssue.Code> failure = addResource(
                    selected, resources);
            if (failure.isPresent()) {
                return Resolution.issue(failure.orElseThrow());
            }
        }
        if (exact != null) {
            Optional<CanvasImageResolutionIssue.Code> failure = addResource(
                    exact, resources);
            if (failure.isPresent()) {
                return Resolution.issue(failure.orElseThrow());
            }
        }

        String exactResourceId = exact != null
                ? exact.sha256()
                : Objects.requireNonNull(selected).sha256();
        FlutterAssetVariant variantSource = selected != null ? selected : exact;
        BigDecimal variantScale = selected != null
                ? BigDecimal.valueOf(selected.scale())
                : BigDecimal.ONE;
        CanvasImageAsset descriptor;
        try {
            descriptor = new CanvasImageAsset(
                    assetId,
                    exactResourceId,
                    List.of(new CanvasImageVariant(
                            variantScale,
                            Objects.requireNonNull(variantSource).sha256())));
        } catch (IllegalArgumentException exception) {
            return Resolution.issue(
                    CanvasImageResolutionIssue.Code.BUDGET_EXCEEDED);
        }
        return Resolution.plan(new AssetPlan(
                descriptor,
                List.copyOf(resources.values())));
    }

    /**
     * Validates nine-patch coordinates against the smallest decoded extent
     * produced by either pinned Flutter 3.44.8 image-codec implementation.
     * Native and Web disagree on a few derived ResizeImage dimensions and Web
     * always disables codec upscaling, so accepting only their intersection
     * prevents a document that is valid in one Canvas backend from closing the
     * other backend's authenticated render session.
     */
    private static boolean centerSlicesFit(
            ProviderUse use,
            FlutterAssetVariant selected,
            FlutterAssetVariant exact) {
        for (CenterSliceUse centerSliceUse : use.centerSlices()) {
            PropertyValue.ImageProviderValue provider = centerSliceUse.provider();
            FlutterAssetVariant source;
            BigDecimal resolvedScale;
            switch (provider.providerKind()) {
                case ASSET -> {
                    source = selected;
                    resolvedScale = selected == null
                            ? null : BigDecimal.valueOf(selected.scale());
                }
                case EXACT_ASSET -> {
                    source = exact;
                    resolvedScale = provider.exactScale().orElse(null);
                }
                default -> throw new IllegalStateException(
                        "Unhandled image provider kind: "
                        + provider.providerKind());
            }
            if (source == null || resolvedScale == null) {
                return false;
            }
            Optional<DecodedImageSize> nativeSize = decodedImageSize(
                    source.width(), source.height(), provider.resize(), false);
            Optional<DecodedImageSize> webSize = decodedImageSize(
                    source.width(), source.height(), provider.resize(), true);
            if (nativeSize.isEmpty() || webSize.isEmpty()) {
                return false;
            }
            int conservativeWidth = Math.min(
                    nativeSize.orElseThrow().width(),
                    webSize.orElseThrow().width());
            int conservativeHeight = Math.min(
                    nativeSize.orElseThrow().height(),
                    webSize.orElseThrow().height());
            BigDecimal combinedScale = resolvedScale.multiply(
                    centerSliceUse.decorationScale());
            PropertyValue.DecorationImageValue.Rect slice =
                    centerSliceUse.centerSlice();
            if (slice.right().multiply(combinedScale).compareTo(
                    BigDecimal.valueOf(conservativeWidth)) > 0
                    || slice.bottom().multiply(combinedScale).compareTo(
                            BigDecimal.valueOf(conservativeHeight)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static Optional<DecodedImageSize> decodedImageSize(
            int intrinsicWidth,
            int intrinsicHeight,
            Optional<PropertyValue.ImageProviderValue.ResizeImageConfig> resize,
            boolean webCodec) {
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            return Optional.empty();
        }
        if (resize.isEmpty()) {
            return Optional.of(new DecodedImageSize(
                    intrinsicWidth, intrinsicHeight));
        }
        PropertyValue.ImageProviderValue.ResizeImageConfig value =
                resize.orElseThrow();
        int targetWidth;
        int targetHeight;
        if (value.policy()
                == PropertyValue.ImageProviderValue.ResizePolicy.FIT) {
            int maxWidth = value.width().orElse(intrinsicWidth);
            int maxHeight = value.height().orElse(intrinsicHeight);
            targetWidth = intrinsicWidth;
            targetHeight = intrinsicHeight;
            if (targetWidth > maxWidth) {
                targetWidth = maxWidth;
                targetHeight = scaledFloor(
                        targetWidth, intrinsicHeight, intrinsicWidth);
            }
            if (targetHeight > maxHeight) {
                targetHeight = maxHeight;
                targetWidth = scaledFloor(
                        targetHeight, intrinsicWidth, intrinsicHeight);
            }
            if (value.allowUpscaling()) {
                if (value.width().isEmpty()) {
                    targetHeight = value.height().orElseThrow();
                    targetWidth = scaledFloor(
                            targetHeight, intrinsicWidth, intrinsicHeight);
                } else if (value.height().isEmpty()) {
                    targetWidth = value.width().orElseThrow();
                    targetHeight = scaledFloor(
                            targetWidth, intrinsicHeight, intrinsicWidth);
                } else {
                    int derivedMaxWidth = scaledFloor(
                            maxHeight, intrinsicWidth, intrinsicHeight);
                    int derivedMaxHeight = scaledFloor(
                            maxWidth, intrinsicHeight, intrinsicWidth);
                    targetWidth = Math.min(maxWidth, derivedMaxWidth);
                    targetHeight = Math.min(maxHeight, derivedMaxHeight);
                }
            }
        } else {
            Integer requestedWidth = value.width().orElse(null);
            Integer requestedHeight = value.height().orElse(null);
            if (!value.allowUpscaling()) {
                if (requestedWidth != null) {
                    requestedWidth = Math.min(requestedWidth, intrinsicWidth);
                }
                if (requestedHeight != null) {
                    requestedHeight = Math.min(requestedHeight, intrinsicHeight);
                }
            }
            if (requestedWidth == null) {
                targetHeight = Objects.requireNonNull(requestedHeight);
                targetWidth = scaledRound(
                        targetHeight, intrinsicWidth, intrinsicHeight);
            } else if (requestedHeight == null) {
                targetWidth = requestedWidth;
                targetHeight = webCodec
                        ? scaledRound(targetWidth, intrinsicHeight, intrinsicWidth)
                        : scaledFloor(targetWidth, intrinsicHeight, intrinsicWidth);
            } else {
                targetWidth = requestedWidth;
                targetHeight = requestedHeight;
            }
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            return Optional.empty();
        }
        // The pinned Web engine's instantiateImageCodecWithSize path forces
        // allowUpscaling:false after ResizeImage computes its requested size.
        if (webCodec && (targetWidth > intrinsicWidth
                || targetHeight > intrinsicHeight)) {
            return Optional.of(new DecodedImageSize(
                    intrinsicWidth, intrinsicHeight));
        }
        return Optional.of(new DecodedImageSize(targetWidth, targetHeight));
    }

    private static int scaledFloor(int value, int numerator, int denominator) {
        return Math.toIntExact((long) value * numerator / denominator);
    }

    private static int scaledRound(int value, int numerator, int denominator) {
        long product = (long) value * numerator;
        return Math.toIntExact((product + denominator / 2L) / denominator);
    }

    private static Optional<CanvasImageResolutionIssue.Code> addResource(
            FlutterAssetVariant variant,
            TreeMap<String, CanvasImageResource> resources) {
        if (variant.byteLength() > CanvasImageResource.MAX_ENCODED_BYTES
                || variant.width() > CanvasImageResource.MAX_DIMENSION
                || variant.height() > CanvasImageResource.MAX_DIMENSION
                || (long) variant.width() * variant.height()
                > CanvasImageResource.MAX_PIXELS) {
            return Optional.of(
                    CanvasImageResolutionIssue.Code.BUDGET_EXCEEDED);
        }
        CanvasImageResource resource;
        try {
            resource = new CanvasImageResource(
                    variant.sha256(),
                    canvasFormat(variant.format()),
                    variant.width(),
                    variant.height(),
                    variant.bytes());
        } catch (IllegalArgumentException exception) {
            return Optional.of(CanvasImageResolutionIssue.Code.CORRUPT);
        }
        CanvasImageResource existing = resources.putIfAbsent(
                resource.resourceId(), resource);
        if (existing != null && !existing.equals(resource)) {
            return Optional.of(CanvasImageResolutionIssue.Code.CORRUPT);
        }
        return Optional.empty();
    }

    private static CanvasImageFormat canvasFormat(FlutterImageFormat format) {
        return switch (format) {
            case PNG -> CanvasImageFormat.PNG;
            case JPEG -> CanvasImageFormat.JPEG;
            case GIF -> CanvasImageFormat.GIF;
            case WEBP -> CanvasImageFormat.WEBP;
        };
    }

    private static CanvasImageResolutionIssue.Code classifyUnavailable(
            CanvasImageAssetId assetId,
            FlutterAssetInventory inventory) {
        List<FlutterAssetDiagnostic> diagnostics = inventory.diagnostics().stream()
                .filter(diagnostic -> appliesTo(diagnostic, assetId))
                .toList();
        if (diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.operation().startsWith("enforce "))) {
            return CanvasImageResolutionIssue.Code.BUDGET_EXCEEDED;
        }
        if (diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.operation().contains("normalize")
                || diagnostic.operation().equals("resolve package root")
                || diagnostic.reason().contains("escapes package root")
                || diagnostic.reason().contains("symbolic-link"))) {
            return CanvasImageResolutionIssue.Code.INVALID_PATH;
        }
        if (diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("inspect asset image")
                && diagnostic.reason().contains("unsupported image magic"))) {
            return CanvasImageResolutionIssue.Code.UNSUPPORTED_FORMAT;
        }
        if (diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("inspect asset image"))) {
            return CanvasImageResolutionIssue.Code.CORRUPT;
        }
        if (diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.reason().contains("does not exist")
                || diagnostic.reason().contains("no regular base file")
                || diagnostic.reason().contains("not found"))) {
            return CanvasImageResolutionIssue.Code.MISSING;
        }
        if (!diagnostics.isEmpty()) {
            return CanvasImageResolutionIssue.Code.UNREADABLE;
        }
        return CanvasImageResolutionIssue.Code.UNDECLARED;
    }

    private static boolean appliesTo(
            FlutterAssetDiagnostic diagnostic,
            CanvasImageAssetId assetId) {
        String target = diagnostic.target();
        String externalName = assetId.externalName();
        if (target.equals(externalName)
                || target.startsWith(externalName + " ->")) {
            return true;
        }
        String scope = assetId.packageName()
                .map(value -> "package:" + value)
                .orElse("app");
        int variantSeparator = target.indexOf(" -> ");
        if (variantSeparator >= 0
                && target.startsWith(scope + ':')
                && target.substring(variantSeparator + 4)
                        .equals(assetId.assetName())) {
            return true;
        }
        if (target.equals(scope) || target.startsWith(scope + ":pubspec.yaml")) {
            return true;
        }
        return target.contains("('" + assetId.assetName() + "')");
    }

    private static CanvasImageResolutionIssue issue(
            CanvasImageAssetId assetId,
            CanvasImageResolutionIssue.Code code) {
        String externalName = boundedAssetExternalName(assetId);
        String reason = switch (code) {
            case UNDECLARED -> "Resolve Canvas image " + externalName
                    + ": no matching declared logical Flutter image asset is available.";
            case MISSING -> "Resolve Canvas image " + externalName
                    + ": the declared logical file or requested exact variant is missing.";
            case INVALID_PATH -> "Resolve Canvas image " + externalName
                    + ": the asset failed relative-POSIX or package-root containment validation.";
            case UNREADABLE -> "Resolve Canvas image " + externalName
                    + ": encoded bytes could not be read from the owning Flutter package.";
            case UNSUPPORTED_FORMAT -> "Resolve Canvas image " + externalName
                    + ": encoded bytes are not PNG, JPEG, GIF, or WebP.";
            case CORRUPT -> "Resolve Canvas image " + externalName
                    + ": encoded image metadata, content digest, or "
                    + "centerSlice bounds are corrupt.";
            case BUDGET_EXCEEDED -> "Resolve Canvas image " + externalName
                    + ": selected bytes exceed the strict Canvas image count, byte, dimension, "
                    + "pixel, or variant-scale budget.";
        };
        return new CanvasImageResolutionIssue(assetId, code, reason);
    }

    private static CanvasImageResolutionIssue unresolvedIssue(
            CanvasImageAssetId assetId) {
        return new CanvasImageResolutionIssue(
                assetId,
                CanvasImageResolutionIssue.Code.UNDECLARED,
                "Resolve Canvas Image placeholder: choose a declared Flutter image "
                        + "asset in the Image image property.");
    }

    private static String choiceUnavailableReason(
            FlutterAssetInventory inventory,
            boolean reservedIdentityExcluded) {
        if (reservedIdentityExcluded) {
            return "Resolve declared Flutter image choices: the discovered image "
                    + "asset uses a Designer-reserved placeholder identity; rename "
                    + "that asset path.";
        }
        if (inventory.diagnostics().isEmpty()) {
            return "Resolve declared Flutter image choices: the owning project declares "
                    + "no safe PNG, JPEG, GIF, or WebP assets.";
        }
        FlutterAssetDiagnostic first = inventory.diagnostics().getFirst();
        String target = safeDiagnosticTarget(first.target());
        return "Resolve declared Flutter image choices for " + target + ": operation '"
                + first.operation() + "' failed; " + inventory.diagnostics().size()
                + " bounded diagnostic(s) were recorded.";
    }

    private static String safeDiagnosticTarget(String target) {
        Objects.requireNonNull(target, "target");
        int arrow = target.indexOf(" -> ");
        String logical = arrow < 0 ? target : target.substring(0, arrow);
        try {
            if (logical.equals("app")) {
                return logical;
            }
            if (logical.startsWith("app:")) {
                String assetName = logical.substring("app:".length());
                String canonical = FlutterAssetId.app(assetName).wireName();
                return canonical.equals(logical) ? boundedDiagnosticTarget(canonical)
                        : "the owning Flutter project";
            }
            if (logical.startsWith("package:")) {
                String remainder = logical.substring("package:".length());
                int assetSeparator = remainder.indexOf(':');
                if (assetSeparator < 0) {
                    String packageName = FlutterAssetId.validatePackageName(remainder);
                    String canonical = "package:" + packageName;
                    return canonical.equals(logical) ? canonical
                            : "the owning Flutter project";
                }
                String canonical = FlutterAssetId.packageAsset(
                        remainder.substring(0, assetSeparator),
                        remainder.substring(assetSeparator + 1)).wireName();
                return canonical.equals(logical) ? boundedDiagnosticTarget(canonical)
                        : "the owning Flutter project";
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            // Diagnostics cross a UI trust boundary. Any non-canonical target,
            // including a package label with an appended file URI, is hidden.
        }
        return "the owning Flutter project";
    }

    private static String boundedDiagnosticTarget(String target) {
        return target.length() <= 512 ? target : target.substring(0, 511) + '\u2026';
    }

    private static String boundedExternalReason(String reason) {
        Objects.requireNonNull(reason, "reason");
        String compact = reason.strip().replaceAll("\\s+", " ");
        if (compact.isEmpty()) {
            throw new IllegalArgumentException(
                    "Canvas image inventory failure reason must not be blank");
        }
        for (int index = 0; index < compact.length(); index++) {
            if (Character.isISOControl(compact.charAt(index))) {
                throw new IllegalArgumentException(
                        "Canvas image inventory failure reason contains a control character");
            }
        }
        int pathOffset = firstFilesystemPathOffset(compact);
        if (pathOffset >= 0) {
            String safePrefix = compact.substring(0, pathOffset).stripTrailing();
            compact = safePrefix.isEmpty()
                    ? "Filesystem operation failed at [project path]"
                    : safePrefix + " [project path]";
        }
        if (compact.length() > MAX_EXTERNAL_FAILURE_REASON) {
            compact = compact.substring(0, MAX_EXTERNAL_FAILURE_REASON - 1) + '\u2026';
        }
        return compact;
    }

    /**
     * Finds only the start of the first absolute filesystem-looking value.
     * The caller deliberately discards the complete suffix rather than trying
     * to guess where a path containing spaces ends; a partial replacement
     * could otherwise expose the remaining directories or file name.
     */
    private static int firstFilesystemPathOffset(String value) {
        int first = -1;
        for (Pattern pattern : List.of(
                FILE_URI,
                WINDOWS_ABSOLUTE_PATH,
                UNC_PATH,
                POSIX_ABSOLUTE_PATH)) {
            java.util.regex.Matcher matcher = pattern.matcher(value);
            if (matcher.find() && (first < 0 || matcher.start() < first)) {
                first = matcher.start();
            }
        }
        return first;
    }

    private static String boundedAssetExternalName(CanvasImageAssetId assetId) {
        String externalName = assetId.externalName();
        return externalName.length() <= MAX_REASON_ASSET_NAME
                ? externalName
                : externalName.substring(0, MAX_REASON_ASSET_NAME - 1) + '\u2026';
    }

    private static String projectionFingerprint(
            String inventoryFingerprint,
            CanvasImageResourceBundle bundle) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("inventory\0".getBytes(StandardCharsets.UTF_8));
            digest.update(inventoryFingerprint.getBytes(StandardCharsets.US_ASCII));
            digest.update("\nbundle\0".getBytes(StandardCharsets.UTF_8));
            digest.update(bundle.fingerprintSha256().getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /** Immutable result suitable for one revision-scoped Canvas publication. */
    public record ProjectionResult(
            String inventoryFingerprintSha256,
            String fingerprintSha256,
            FlutterImageAssetChoices choices,
            CanvasImageResourceBundle bundle) {

        public ProjectionResult {
            requireSha256(inventoryFingerprintSha256, "inventoryFingerprintSha256");
            requireSha256(fingerprintSha256, "fingerprintSha256");
            Objects.requireNonNull(choices, "choices");
            Objects.requireNonNull(bundle, "bundle");
        }

        private static void requireSha256(String value, String name) {
            Objects.requireNonNull(value, name);
            if (!SHA_256.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        name + " must be a lowercase SHA-256 digest");
            }
        }
    }

    private record ProviderUse(
            boolean asset,
            boolean exact,
            boolean unresolved,
            List<CenterSliceUse> centerSlices) {
        private static final ProviderUse NONE = new ProviderUse(
                false, false, false, List.of());

        ProviderUse {
            centerSlices = List.copyOf(centerSlices);
        }

        ProviderUse withAsset() {
            return new ProviderUse(true, exact, unresolved, centerSlices);
        }

        ProviderUse withExact() {
            return new ProviderUse(asset, true, unresolved, centerSlices);
        }

        ProviderUse withUnresolved() {
            return new ProviderUse(asset, exact, true, centerSlices);
        }

        ProviderUse withCenterSlice(CenterSliceUse centerSlice) {
            ArrayList<CenterSliceUse> uses = new ArrayList<>(centerSlices);
            uses.add(Objects.requireNonNull(centerSlice, "centerSlice"));
            return new ProviderUse(asset, exact, unresolved, uses);
        }
    }

    private record CenterSliceUse(
            PropertyValue.ImageProviderValue provider,
            PropertyValue.DecorationImageValue.Rect centerSlice,
            BigDecimal decorationScale) {

        CenterSliceUse {
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(centerSlice, "centerSlice");
            Objects.requireNonNull(decorationScale, "decorationScale");
        }
    }

    private record DecodedImageSize(int width, int height) {
    }

    private record ExactVariant(FlutterAssetVariant variant, boolean ambiguous) {
    }

    private record InventoryIndex(
            Map<CanvasImageAssetId, FlutterAsset> logicalAssets,
            Map<CanvasImageAssetId, ExactVariant> exactVariants) {

        static InventoryIndex create(FlutterAssetInventory inventory) {
            TreeMap<CanvasImageAssetId, FlutterAsset> logical = new TreeMap<>();
            TreeMap<CanvasImageAssetId, ExactVariant> exact = new TreeMap<>();
            for (FlutterAsset asset : inventory.assets()) {
                CanvasImageAssetId logicalId = canvasId(
                        asset.id(), asset.id().logicalPath());
                logical.put(logicalId, asset);
                for (FlutterAssetVariant variant : asset.variants()) {
                    CanvasImageAssetId exactId = canvasId(
                            asset.id(), variant.logicalPath());
                    ExactVariant existing = exact.get(exactId);
                    if (existing == null) {
                        exact.put(exactId, new ExactVariant(variant, false));
                    } else if (!sameEncodedResource(
                            existing.variant(), variant)) {
                        exact.put(exactId, new ExactVariant(
                                existing.variant(), true));
                    }
                }
            }
            return new InventoryIndex(Map.copyOf(logical), Map.copyOf(exact));
        }

        private static CanvasImageAssetId canvasId(
                FlutterAssetId id,
                String logicalPath) {
            return new CanvasImageAssetId(
                    Optional.ofNullable(id.packageName()), logicalPath);
        }

        private static boolean sameEncodedResource(
                FlutterAssetVariant first,
                FlutterAssetVariant second) {
            return first.logicalPath().equals(second.logicalPath())
                    && first.format() == second.format()
                    && first.width() == second.width()
                    && first.height() == second.height()
                    && first.byteLength() == second.byteLength()
                    && first.sha256().equals(second.sha256())
                    && Arrays.equals(first.bytes(), second.bytes());
        }
    }

    private record AssetPlan(
            CanvasImageAsset asset,
            List<CanvasImageResource> resources) {
    }

    private record Resolution(
            Optional<AssetPlan> plan,
            Optional<CanvasImageResolutionIssue.Code> issueCode) {

        static Resolution plan(AssetPlan plan) {
            return new Resolution(Optional.of(plan), Optional.empty());
        }

        static Resolution issue(CanvasImageResolutionIssue.Code code) {
            return new Resolution(Optional.empty(), Optional.of(code));
        }
    }
}

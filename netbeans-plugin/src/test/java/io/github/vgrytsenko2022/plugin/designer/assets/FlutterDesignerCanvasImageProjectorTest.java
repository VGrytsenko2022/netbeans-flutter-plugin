package io.github.vgrytsenko2022.plugin.designer.assets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.canvas.CanvasImageAsset;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageAssetId;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResolutionIssue;
import io.github.vgrytsenko2022.designer.canvas.CanvasImageResource;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterImageAssetChoices;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterDesignerCanvasImageProjectorTest {
    @TempDir
    Path temporaryDirectory;

    private final FlutterAssetResolver resolver = new FlutterAssetResolver();
    private final FlutterDesignerCanvasImageProjector projector =
            new FlutterDesignerCanvasImageProjector();

    @Test
    void switchProjectsBothThumbProvidersWithoutExecutingErrorCallbacksAndEmptyCreationNeedsNoAssets() throws Exception {
        Path project = newProject("switch", "switch_app", """
                  assets:
                    - assets/active.png
                    - assets/inactive.png
                """);
        write(project, "assets/active.png", png(10, 10)); write(project, "assets/2x/active.png", png(20, 20));
        write(project, "assets/inactive.png", png(13, 17)); writePackageConfig(project, List.of(new PackageEntry("switch_app", "../")));
        var inventory = resolver.resolve(project);
        var def = io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.material.Switch")).orElseThrow();
        var seed = io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory.create(def, StableId.random());
        assertFalse(projector.referencesImages(document(seed)));
        for (String variant : List.of("standard", "adaptive")) {
            var values = new LinkedHashMap<>(seed.properties()); values.put(new PropertyName("variant"), new PropertyValue.StringValue(variant));
            values.put(new PropertyName("activeThumbImage"), PropertyValue.ImageProviderValue.asset("assets/active.png"));
            values.put(new PropertyName("inactiveThumbImage"), PropertyValue.ImageProviderValue.exactAsset("assets/inactive.png", new BigDecimal("2")));
            values.put(new PropertyName("onActiveThumbImageError"), io.github.vgrytsenko2022.plugin.designer.properties.SwitchPropertyContractTest.reference("_onActiveThumbImageError"));
            values.put(new PropertyName("onInactiveThumbImageError"), io.github.vgrytsenko2022.plugin.designer.properties.SwitchPropertyContractTest.reference("_onInactiveThumbImageError"));
            var document = document(new WidgetNode(seed.id(), def.typeId(), values, Map.of()));
            assertTrue(projector.referencesImages(document)); var result = projector.project(document, inventory, 2);
            assertTrue(result.bundle().issues().isEmpty()); assertEquals(2, result.bundle().assets().size()); assertEquals(2, result.bundle().resources().size());
            assertEquals(java.util.Set.of("app:assets/active.png", "app:assets/inactive.png"), result.bundle().assets().stream().map(asset -> asset.assetId().externalName()).collect(Collectors.toSet()));
            assertEquals(java.util.Set.of(20, 13), result.bundle().resources().stream().map(CanvasImageResource::pixelWidth).collect(Collectors.toSet()));
        }
    }

    @Test
    void imageIconNullHasNoResourcesWhileProvidersUseExactSharedProjection() throws Exception {
        Path project = newProject("imageicon", "imageicon_app", """
                  assets:
                    - assets/logo.png
                """);
        write(project, "assets/logo.png", png(10, 10));
        write(project, "assets/2x/logo.png", png(20, 20));
        writePackageConfig(project, List.of(new PackageEntry("imageicon_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        WidgetNode seed = node(Map.of(), Map.of());
        for (PropertyValue image : List.of(new PropertyValue.NullValue(),
                PropertyValue.ImageProviderValue.asset("assets/logo.png"),
                PropertyValue.ImageProviderValue.exactAsset("assets/2x/logo.png", new BigDecimal("2")))) {
            DesignerDocument document = document(new WidgetNode(seed.id(),
                    new WidgetTypeId("flutter.widgets.ImageIcon"), Map.of(new PropertyName("image"), image), Map.of()));
            boolean present = image instanceof PropertyValue.ImageProviderValue;
            assertEquals(present, projector.referencesImages(document));
            var result = projector.project(document, inventory, 2);
            assertEquals(present ? 1 : 0, result.bundle().assets().size());
            assertEquals(present ? 1 : 0, result.bundle().resources().size());
            assertTrue(result.bundle().issues().isEmpty());
            if (present) {
                assertEquals(20, result.bundle().resources().getFirst().pixelWidth());
            }
        }
    }

    @Test
    void recursivelyProjectsOnlyThePinnedSelectedVariant() throws Exception {
        Path project = newProject("selected", "selected_app", """
                  assets:
                    - assets/logo.png
                    - assets/unused.png
                """);
        write(project, "assets/logo.png", png(10, 10));
        write(project, "assets/2x/logo.png", png(20, 20));
        write(project, "assets/4x/logo.png", png(40, 40));
        write(project, "assets/unused.png", png(99, 98));
        writePackageConfig(project, List.of(
                new PackageEntry("selected_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        FlutterAsset logo = inventory.find(
                FlutterAssetId.app("assets/logo.png")).orElseThrow();
        FlutterAssetVariant selected = logo.selectVariant(1.5);
        FlutterAssetVariant unused = inventory.find(
                FlutterAssetId.app("assets/unused.png"))
                .orElseThrow().variants().getFirst();

        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.asset("assets/logo.png");
        WidgetNode child = node(
                Map.of(new PropertyName("image"), provider),
                Map.of());
        WidgetNode root = node(
                Map.of(new PropertyName("decoration"), decoration(provider)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));

        FlutterDesignerCanvasImageProjector.ProjectionResult result =
                projector.project(document(root), inventory, 1.5);

        assertTrue(result.bundle().issues().isEmpty(),
                result.bundle().issues().toString());
        assertEquals(1, result.bundle().assets().size());
        assertEquals(1, result.bundle().resources().size());
        CanvasImageAsset asset = result.bundle().assets().getFirst();
        assertEquals("app:assets/logo.png", asset.assetId().externalName());
        assertEquals(selected.sha256(), asset.exactResourceId());
        assertEquals(new BigDecimal("2"), asset.variants().getFirst().scale());
        assertEquals(selected.sha256(),
                asset.variants().getFirst().resourceId());
        CanvasImageResource resource = result.bundle().resources().getFirst();
        assertEquals(selected.sha256(), resource.resourceId());
        assertEquals(20, resource.pixelWidth());
        assertEquals(20, resource.pixelHeight());
        assertArrayEquals(selected.bytes(), resource.copyEncodedBytes());
        assertFalse(result.bundle().resources().stream().anyMatch(candidate ->
                candidate.resourceId().equals(unused.sha256())));
        assertEquals(List.of(
                "app:assets/logo.png",
                "app:assets/unused.png"),
                result.choices().choices().stream()
                        .map(FlutterImageAssetChoices.Choice::externalName)
                        .toList());
        assertEquals(inventory.fingerprintSha256(),
                result.inventoryFingerprintSha256());
        assertEquals(64, result.fingerprintSha256().length());
    }

    @Test
    void exactProviderCanAddressAVariantOnlyLogicalFile() throws Exception {
        Path project = newProject("variant-only", "variant_app", """
                  assets:
                    - assets/only.png
                """);
        write(project, "assets/2x/only.png", png(22, 24));
        writePackageConfig(project, List.of(
                new PackageEntry("variant_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        FlutterAssetVariant exact = inventory.find(
                FlutterAssetId.app("assets/only.png"))
                .orElseThrow().variants().getFirst();
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/2x/only.png", new BigDecimal("101"));

        FlutterDesignerCanvasImageProjector.ProjectionResult result =
                projector.project(document(node(
                        Map.of(new PropertyName("image"), provider), Map.of())),
                        inventory,
                        3.0);

        CanvasImageAsset asset = result.bundle().find(
                CanvasImageAssetId.application("assets/2x/only.png"))
                .orElseThrow();
        assertTrue(result.bundle().issues().isEmpty());
        assertEquals(exact.sha256(), asset.exactResourceId());
        assertEquals(BigDecimal.ONE, asset.variants().getFirst().scale());
        assertEquals(exact.sha256(), asset.variants().getFirst().resourceId());
        assertEquals(1, result.bundle().resources().size());
        assertTrue(result.choices().find(
                Optional.empty(), "assets/only.png").isPresent());
        assertTrue(result.choices().find(
                Optional.empty(), "assets/2x/only.png").isEmpty());
    }

    @Test
    void exactPhysicalFileDiscoveredAtTwoScalesIsNotAmbiguous()
            throws Exception {
        Path project = newProject("exact-dedup", "exact_dedup_app", """
                  assets:
                    - assets/only.png
                    - assets/2x/only.png
                """);
        write(project, "assets/2x/only.png", png(18, 20));
        writePackageConfig(project, List.of(
                new PackageEntry("exact_dedup_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/2x/only.png", new BigDecimal("101"));

        var result = projector.project(document(node(
                Map.of(new PropertyName("image"), provider), Map.of())),
                inventory,
                1.0);

        assertTrue(result.bundle().issues().isEmpty(),
                result.bundle().issues().toString());
        assertEquals(1, result.bundle().assets().size());
        assertEquals(1, result.bundle().resources().size());
    }

    @Test
    void sameLogicalIdUsedAsAssetAndExactIncludesOnlyBothNeededResources()
            throws Exception {
        Path project = newProject("both-kinds", "both_app", """
                  assets:
                    - assets/logo.png
                    - assets/unused.png
                """);
        write(project, "assets/logo.png", png(10, 11));
        write(project, "assets/2x/logo.png", png(20, 22));
        write(project, "assets/unused.png", png(30, 31));
        writePackageConfig(project, List.of(new PackageEntry("both_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        FlutterAsset logo = inventory.find(
                FlutterAssetId.app("assets/logo.png")).orElseThrow();
        FlutterAssetVariant base = logo.variants().stream()
                .filter(variant -> variant.scale() == 1.0)
                .findFirst().orElseThrow();
        FlutterAssetVariant selected = logo.selectVariant(2.0);
        FlutterAssetVariant unused = inventory.find(
                FlutterAssetId.app("assets/unused.png"))
                .orElseThrow().variants().getFirst();
        PropertyValue.ImageProviderValue assetProvider =
                PropertyValue.ImageProviderValue.asset("assets/logo.png");
        PropertyValue.ImageProviderValue exactProvider =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/logo.png", new BigDecimal("3"));
        WidgetNode root = node(Map.of(
                new PropertyName("image"), assetProvider,
                new PropertyName("decoration"), decoration(exactProvider)), Map.of());

        FlutterDesignerCanvasImageProjector.ProjectionResult first =
                projector.project(document(root), inventory, 2.0);
        FlutterDesignerCanvasImageProjector.ProjectionResult second =
                projector.project(document(root), inventory, 2.0);

        CanvasImageAsset descriptor = first.bundle().assets().getFirst();
        assertEquals(base.sha256(), descriptor.exactResourceId());
        assertEquals(List.of(selected.sha256()), descriptor.variants().stream()
                .map(variant -> variant.resourceId()).toList());
        assertEquals(List.of(base.sha256(), selected.sha256()).stream().sorted().toList(),
                first.bundle().resources().stream()
                        .map(CanvasImageResource::resourceId).toList());
        assertFalse(first.bundle().resources().stream().anyMatch(resource ->
                resource.resourceId().equals(unused.sha256())));
        assertEquals(first.bundle().fingerprintSha256(),
                second.bundle().fingerprintSha256());
        assertEquals(first.fingerprintSha256(), second.fingerprintSha256());
        assertNotEquals(inventory.fingerprintSha256(), first.fingerprintSha256());
    }

    @Test
    void centerSliceUsesSelectedVariantScaleAndRecoversAtExactBoundary()
            throws Exception {
        Path project = newProject("center-slice-scale", "slice_app", """
                  assets:
                    - assets/panel.png
                """);
        write(project, "assets/panel.png", png(4, 4));
        write(project, "assets/2x/panel.png", png(8, 8));
        writePackageConfig(project, List.of(new PackageEntry("slice_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.asset("assets/panel.png");

        var rejected = projector.project(document(node(Map.of(
                new PropertyName("image"), provider,
                new PropertyName("decoration"), centerSliceDecoration(
                        provider,
                        rect("0", "0", "5", "4"),
                        BigDecimal.ONE)), Map.of())), inventory, 2.0);

        assertTrue(rejected.bundle().assets().isEmpty());
        assertTrue(rejected.bundle().resources().isEmpty());
        assertEquals(1, rejected.bundle().issues().size());
        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                rejected.bundle().issues().getFirst().code());
        assertEquals("app:assets/panel.png",
                rejected.bundle().issues().getFirst().assetId().externalName());
        assertTrue(rejected.bundle().issues().getFirst().reason()
                .contains("centerSlice bounds"));
        assertFalse(rejected.bundle().issues().getFirst().reason()
                .contains(project.toAbsolutePath().toString()));

        var corrected = projector.project(document(node(Map.of(
                new PropertyName("image"), provider,
                new PropertyName("decoration"), centerSliceDecoration(
                        provider,
                        rect("0", "0", "4", "4"),
                        BigDecimal.ONE)), Map.of())), inventory, 2.0);

        assertTrue(corrected.bundle().issues().isEmpty(),
                corrected.bundle().issues().toString());
        assertEquals(1, corrected.bundle().assets().size());
        assertEquals(1, corrected.bundle().resources().size());
        assertEquals(8, corrected.bundle().resources().getFirst().pixelWidth());
        assertEquals(8, corrected.bundle().resources().getFirst().pixelHeight());
    }

    @Test
    void centerSliceIncludesDecorationScaleAndExactProviderScale()
            throws Exception {
        Path project = newProject("center-slice-exact", "slice_exact_app", """
                  assets:
                    - assets/panel.png
                """);
        write(project, "assets/panel.png", png(8, 8));
        writePackageConfig(project,
                List.of(new PackageEntry("slice_exact_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/panel.png", new BigDecimal("2"));

        var rejected = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        provider,
                        rect("0", "0", "3", "2"),
                        new BigDecimal("2"))), Map.of())), inventory, 1.0);

        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                rejected.bundle().issues().getFirst().code());
        assertTrue(rejected.bundle().resources().isEmpty());

        var accepted = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        provider,
                        rect("0", "0", "2", "2"),
                        new BigDecimal("2"))), Map.of())), inventory, 1.0);

        assertTrue(accepted.bundle().issues().isEmpty());
        assertEquals(1, accepted.bundle().resources().size());
    }

    @Test
    void centerSliceUsesConservativeNativeAndWebResizeBounds()
            throws Exception {
        Path project = newProject("center-slice-resize", "slice_resize_app", """
                  assets:
                    - assets/panel.png
                """);
        write(project, "assets/panel.png", png(4, 3));
        writePackageConfig(project,
                List.of(new PackageEntry("slice_resize_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue roundedOnlyOnWeb = resizedAsset(
                "assets/panel.png",
                Optional.of(2),
                Optional.empty(),
                PropertyValue.ImageProviderValue.ResizePolicy.EXACT,
                false);

        var rejectedNativeHeight = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        roundedOnlyOnWeb,
                        rect("0", "0", "2", "1.5"),
                        BigDecimal.ONE)), Map.of())), inventory, 1.0);
        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                rejectedNativeHeight.bundle().issues().getFirst().code());
        assertTrue(rejectedNativeHeight.bundle().resources().isEmpty());

        var acceptedNativeBoundary = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        roundedOnlyOnWeb,
                        rect("0", "0", "2", "1"),
                        BigDecimal.ONE)), Map.of())), inventory, 1.0);
        assertTrue(acceptedNativeBoundary.bundle().issues().isEmpty());

        PropertyValue.ImageProviderValue upscaleOnNativeOnly = resizedAsset(
                "assets/panel.png",
                Optional.of(8),
                Optional.empty(),
                PropertyValue.ImageProviderValue.ResizePolicy.EXACT,
                true);
        var rejectedWebClamp = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        upscaleOnNativeOnly,
                        rect("0", "0", "5", "3"),
                        BigDecimal.ONE)), Map.of())), inventory, 1.0);
        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                rejectedWebClamp.bundle().issues().getFirst().code());
        assertTrue(rejectedWebClamp.bundle().resources().isEmpty());
    }

    @Test
    void directImageCenterSliceUsesProviderResizeDecodeBounds()
            throws Exception {
        Path project = newProject("direct-image-center-slice", "direct_slice_app", """
                  assets:
                    - assets/panel.png
                """);
        write(project, "assets/panel.png", png(4, 3));
        writePackageConfig(project,
                List.of(new PackageEntry("direct_slice_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue provider = resizedAsset(
                "assets/panel.png",
                Optional.of(2),
                Optional.empty(),
                PropertyValue.ImageProviderValue.ResizePolicy.EXACT,
                false);

        var rejected = projector.project(
                document(imageNode(provider, "0", "0", "2", "1.1")),
                inventory,
                1.0);
        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                rejected.bundle().issues().getFirst().code());
        assertTrue(rejected.bundle().resources().isEmpty());

        var accepted = projector.project(
                document(imageNode(provider, "0", "0", "2", "1")),
                inventory,
                1.0);
        assertTrue(accepted.bundle().issues().isEmpty(),
                accepted.bundle().issues().toString());
        assertEquals(1, accepted.bundle().resources().size());
    }

    @Test
    void centerSliceRejectsResizeThatDerivesAZeroTargetDimension()
            throws Exception {
        Path project = newProject("center-slice-zero", "slice_zero_app", """
                  assets:
                    - assets/panel.png
                """);
        write(project, "assets/panel.png", png(100, 1));
        writePackageConfig(project,
                List.of(new PackageEntry("slice_zero_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        PropertyValue.ImageProviderValue provider = resizedAsset(
                "assets/panel.png",
                Optional.of(1),
                Optional.empty(),
                PropertyValue.ImageProviderValue.ResizePolicy.FIT,
                false);

        var result = projector.project(document(node(Map.of(
                new PropertyName("decoration"), centerSliceDecoration(
                        provider,
                        rect("0", "0", "0.5", "0.5"),
                        BigDecimal.ONE)), Map.of())), inventory, 1.0);

        assertEquals(CanvasImageResolutionIssue.Code.CORRUPT,
                result.bundle().issues().getFirst().code());
        assertTrue(result.bundle().assets().isEmpty());
        assertTrue(result.bundle().resources().isEmpty());
    }

    @Test
    void mapsEveryUnavailableClassToTheClosedCanvasCodes() {
        String target = "app:assets/broken.png";
        LinkedHashMap<CanvasImageResolutionIssue.Code, FlutterAssetDiagnostic>
                cases = new LinkedHashMap<>();
        cases.put(CanvasImageResolutionIssue.Code.UNDECLARED, null);
        cases.put(CanvasImageResolutionIssue.Code.MISSING,
                new FlutterAssetDiagnostic(
                        "resolve asset variants", target,
                        "declared file does not exist"));
        cases.put(CanvasImageResolutionIssue.Code.INVALID_PATH,
                new FlutterAssetDiagnostic(
                        "normalize asset path", target,
                        "parent traversal is not allowed"));
        cases.put(CanvasImageResolutionIssue.Code.UNREADABLE,
                new FlutterAssetDiagnostic(
                        "read asset file", target,
                        "access was denied"));
        cases.put(CanvasImageResolutionIssue.Code.UNSUPPORTED_FORMAT,
                new FlutterAssetDiagnostic(
                        "inspect asset image", target,
                        "unsupported image magic; expected PNG, JPEG, GIF, or WebP"));
        cases.put(CanvasImageResolutionIssue.Code.CORRUPT,
                new FlutterAssetDiagnostic(
                        "inspect asset image", target,
                        "malformed PNG dimensions"));
        cases.put(CanvasImageResolutionIssue.Code.BUDGET_EXCEEDED,
                new FlutterAssetDiagnostic(
                        "enforce asset file bytes", target,
                        "encoded file exceeds the configured limit"));
        DesignerDocument document = document(node(Map.of(
                new PropertyName("image"),
                PropertyValue.ImageProviderValue.asset("assets/broken.png")),
                Map.of()));

        for (Map.Entry<CanvasImageResolutionIssue.Code, FlutterAssetDiagnostic>
                entry : cases.entrySet()) {
            List<FlutterAssetDiagnostic> diagnostics = entry.getValue() == null
                    ? List.of() : List.of(entry.getValue());
            FlutterAssetInventory inventory = new FlutterAssetInventory(
                    List.of(), diagnostics, "0".repeat(64));

            FlutterDesignerCanvasImageProjector.ProjectionResult result =
                    projector.project(document, inventory, 1.0);

            assertTrue(result.bundle().assets().isEmpty());
            assertTrue(result.bundle().resources().isEmpty());
            CanvasImageResolutionIssue issue =
                    result.bundle().issues().getFirst();
            assertEquals(entry.getKey(), issue.code());
            assertTrue(issue.reason().contains(
                    "Resolve Canvas image app:assets/broken.png"));
            assertTrue(issue.reason().length() <= 1024);
            assertFalse(issue.reason().contains(
                    temporaryDirectory.toAbsolutePath().toString()));
            assertFalse(issue.reason().contains("onImageError"));
        }

        DesignerDocument exactVariantDocument = document(node(Map.of(
                new PropertyName("image"),
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/2x/broken.png", BigDecimal.valueOf(2))), Map.of()));
        FlutterAssetInventory exactVariantInventory = new FlutterAssetInventory(
                List.of(),
                List.of(new FlutterAssetDiagnostic(
                        "inspect asset image",
                        "app:assets/broken.png -> assets/2x/broken.png",
                        "unsupported image magic")),
                "1".repeat(64));
        assertEquals(
                CanvasImageResolutionIssue.Code.UNSUPPORTED_FORMAT,
                projector.project(
                        exactVariantDocument, exactVariantInventory, 1.0)
                        .bundle().issues().getFirst().code());
    }

    @Test
    void unresolvedProviderAlwaysProjectsAsAPathFreePlaceholderIssue()
            throws Exception {
        PropertyValue.ImageProviderValue provider =
                PropertyValue.ImageProviderValue.unresolved();
        Path project = newProject("unresolved", "unresolved_app", """
                  assets:
                    - %s
                """.formatted(provider.assetName()));
        write(project, provider.assetName(), png(8, 8));
        writePackageConfig(project, List.of(
                new PackageEntry("unresolved_app", "../")));
        FlutterAssetInventory inventory = resolver.resolve(project);
        assertTrue(inventory.find(
                FlutterAssetId.app(provider.assetName())).isPresent());
        DesignerDocument document = document(node(Map.of(
                new PropertyName("image"), provider), Map.of()));

        var projected = projector.project(document, inventory, 1.0);

        assertTrue(projected.bundle().assets().isEmpty());
        assertTrue(projected.bundle().resources().isEmpty());
        assertEquals(1, projected.bundle().issues().size());
        CanvasImageResolutionIssue projectedIssue =
                projected.bundle().issues().getFirst();
        assertEquals(CanvasImageResolutionIssue.Code.UNDECLARED,
                projectedIssue.code());
        assertTrue(projectedIssue.reason().contains("choose a declared Flutter image asset"));
        assertFalse(projectedIssue.reason().contains(provider.assetName()));

        var inventoryUnavailable = projector.unavailable(
                document,
                CanvasImageResolutionIssue.Code.UNREADABLE,
                "the declared asset inventory could not be read");
        CanvasImageResolutionIssue unavailableIssue =
                inventoryUnavailable.issues().getFirst();
        assertEquals(CanvasImageResolutionIssue.Code.UNDECLARED,
                unavailableIssue.code());
        assertEquals(projectedIssue.reason(), unavailableIssue.reason());
        assertFalse(unavailableIssue.reason().contains(provider.assetName()));
    }

    @Test
    void choicesCoverAppAndPackageButDoNotPrefetchEither() throws Exception {
        Path project = newProject("choices", "choices_app", """
                  assets:
                    - assets/app.png
                """);
        write(project, "assets/app.png", png(7, 8));
        Path imagePackage = project.resolve("packages/image_pack");
        Files.createDirectories(imagePackage);
        Files.writeString(imagePackage.resolve("pubspec.yaml"), """
                name: image_pack
                flutter:
                  assets:
                    - icons/package.png
                """);
        write(imagePackage, "icons/package.png", png(9, 10));
        writePackageConfig(project, List.of(
                new PackageEntry("choices_app", "../"),
                new PackageEntry("image_pack", "../packages/image_pack/")));
        FlutterAssetInventory inventory = resolver.resolve(project);

        FlutterDesignerCanvasImageProjector.ProjectionResult result =
                projector.project(document(node(Map.of(), Map.of())), inventory, 2.0);

        assertEquals(List.of(
                "app:assets/app.png",
                "package:image_pack:icons/package.png"),
                result.choices().choices().stream()
                        .map(FlutterImageAssetChoices.Choice::externalName)
                        .toList());
        assertTrue(result.choices().unavailableReason().isEmpty());
        assertTrue(result.bundle().isEmpty());
        assertTrue(result.bundle().resources().isEmpty());
        assertTrue(result.choices().choices().stream().noneMatch(choice ->
                choice.displayName().contains(
                        temporaryDirectory.toAbsolutePath().toString())));
    }

    @Test
    void choicesExcludeDesignerReservedPlaceholderIdentity() throws Exception {
        String reserved = PropertyValue.ImageProviderValue.unresolved().assetName();
        Path project = newProject("reserved-choices", "reserved_choices_app", """
                  assets:
                    - %s
                """.formatted(reserved));
        write(project, reserved, png(8, 8));
        Path imagePackage = project.resolve("packages/image_pack");
        Files.createDirectories(imagePackage);
        Files.writeString(imagePackage.resolve("pubspec.yaml"), """
                name: image_pack
                flutter:
                  assets:
                    - %s
                """.formatted(reserved));
        write(imagePackage, reserved, png(8, 8));
        writePackageConfig(project, List.of(
                new PackageEntry("reserved_choices_app", "../"),
                new PackageEntry("image_pack", "../packages/image_pack/")));

        FlutterImageAssetChoices choices = projector.choices(
                resolver.resolve(project));

        assertTrue(choices.choices().isEmpty());
        String reason = choices.unavailableReason().orElseThrow();
        assertTrue(reason.contains("Designer-reserved placeholder identity"));
        assertFalse(reason.contains(reserved));
    }

    @Test
    void reportsImageReferencesAndBuildsExactCoverageUnavailableBundle() {
        DesignerDocument empty = document(node(Map.of(), Map.of()));
        assertFalse(projector.referencesImages(empty));
        assertTrue(projector.unavailable(
                empty,
                CanvasImageResolutionIssue.Code.UNREADABLE,
                "the declared asset inventory could not be read").isEmpty());

        PropertyValue.ImageProviderValue first =
                PropertyValue.ImageProviderValue.asset("assets/first.png");
        PropertyValue.ImageProviderValue second =
                PropertyValue.ImageProviderValue.exactAsset(
                        "assets/2x/second.png", new BigDecimal("2"));
        WidgetNode child = node(
                Map.of(new PropertyName("image"), second), Map.of());
        DesignerDocument referenced = document(node(
                Map.of(
                        new PropertyName("image"), first,
                        new PropertyName("decoration"), decoration(first)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child))));

        assertTrue(projector.referencesImages(referenced));
        var bundle = projector.unavailable(
                referenced,
                CanvasImageResolutionIssue.Code.UNREADABLE,
                "the declared asset inventory could not be read");

        assertTrue(bundle.assets().isEmpty());
        assertTrue(bundle.resources().isEmpty());
        assertEquals(List.of(
                "app:assets/2x/second.png",
                "app:assets/first.png"),
                bundle.issues().stream()
                        .map(issue -> issue.assetId().externalName()).toList());
        assertTrue(bundle.issues().stream().allMatch(issue ->
                issue.code() == CanvasImageResolutionIssue.Code.UNREADABLE
                && issue.reason().startsWith(
                        "Resolve Canvas image " + issue.assetId().externalName())
                && issue.reason().endsWith(
                        "the declared asset inventory could not be read")));

        var redacted = projector.unavailable(
                referenced,
                CanvasImageResolutionIssue.Code.UNREADABLE,
                "read failed at " + temporaryDirectory.resolve("pubspec.yaml"));
        assertTrue(redacted.issues().stream().allMatch(issue ->
                issue.reason().contains("[project path]")
                && !issue.reason().contains(
                temporaryDirectory.toAbsolutePath().toString())));
    }

    @Test
    void redactsTheWholeSuffixOfAbsolutePathsContainingSpaces() {
        List<String> externalFailures = List.of(
                "read failed at C:\\Users\\Jane Doe\\secret\\asset.png: denied",
                "read failed at \\\\server\\Team Share\\secret\\asset.png: denied",
                "read failed at /home/Jane Doe/secret/asset.png: denied",
                "read failed at file:///C:/Program Files/secret/asset.png: denied");

        for (String externalFailure : externalFailures) {
            String sanitized = projector.sanitizeExternalReason(externalFailure);

            assertEquals("read failed at [project path]", sanitized);
            assertFalse(sanitized.contains("Jane"));
            assertFalse(sanitized.contains("Doe"));
            assertFalse(sanitized.contains("secret"));
            assertFalse(sanitized.contains("asset.png"));
        }
        assertEquals(
                "Filesystem operation failed at [project path]",
                projector.sanitizeExternalReason(
                        "C:\\Users\\Jane Doe\\secret\\asset.png was unreadable"));
    }

    @Test
    void rejectsNonCanonicalPackageDiagnosticTargetsBeforeTheyReachChoices() {
        String leakedTarget =
                "package:private_pack ('file:///C:/Users/Jane Doe/Private Assets/')";
        FlutterAssetInventory inventory = new FlutterAssetInventory(
                List.of(),
                List.of(new FlutterAssetDiagnostic(
                        "resolve package root",
                        leakedTarget,
                        "invalid URI at file:///C:/Users/Jane Doe/Private Assets/")),
                "0".repeat(64));
        DesignerDocument document = document(node(Map.of(
                new PropertyName("image"),
                new PropertyValue.ImageProviderValue(
                        PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                        "icons/mark.png",
                        Optional.of("private_pack"),
                        Optional.empty(),
                        Optional.empty())), Map.of()));

        FlutterDesignerCanvasImageProjector.ProjectionResult result =
                projector.project(document, inventory, 1.0);
        String choiceReason = result.choices().unavailableReason().orElseThrow();
        String canvasReason = result.bundle().issues().getFirst().reason();

        assertTrue(choiceReason.contains("the owning Flutter project"));
        assertFalse(choiceReason.contains("Jane"));
        assertFalse(choiceReason.contains("Private Assets"));
        assertFalse(choiceReason.contains("file:///"));
        assertFalse(canvasReason.contains("Jane"));
        assertFalse(canvasReason.contains("Private Assets"));
        assertFalse(canvasReason.contains("file:///"));
    }

    private static PropertyValue.BoxDecorationValue decoration(
            PropertyValue.ImageProviderValue provider) {
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(),
                Optional.of(PropertyValue.DecorationImageValue.defaults(provider)),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static PropertyValue.BoxDecorationValue centerSliceDecoration(
            PropertyValue.ImageProviderValue provider,
            PropertyValue.DecorationImageValue.Rect centerSlice,
            BigDecimal scale) {
        PropertyValue.DecorationImageValue defaults =
                PropertyValue.DecorationImageValue.defaults(provider);
        PropertyValue.DecorationImageValue image =
                new PropertyValue.DecorationImageValue(
                        provider,
                        defaults.onError(),
                        defaults.colorFilter(),
                        defaults.fit(),
                        defaults.alignment(),
                        Optional.of(centerSlice),
                        defaults.repeat(),
                        defaults.matchTextDirection(),
                        scale,
                        defaults.opacity(),
                        defaults.filterQuality(),
                        defaults.invertColors(),
                        defaults.isAntiAlias());
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(),
                Optional.of(image),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    private static PropertyValue.DecorationImageValue.Rect rect(
            String left, String top, String right, String bottom) {
        return new PropertyValue.DecorationImageValue.Rect(
                new BigDecimal(left),
                new BigDecimal(top),
                new BigDecimal(right),
                new BigDecimal(bottom));
    }

    private static PropertyValue.ImageProviderValue resizedAsset(
            String assetName,
            Optional<Integer> width,
            Optional<Integer> height,
            PropertyValue.ImageProviderValue.ResizePolicy policy,
            boolean allowUpscaling) {
        return new PropertyValue.ImageProviderValue(
                PropertyValue.ImageProviderValue.ProviderKind.ASSET,
                assetName,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new PropertyValue.ImageProviderValue.ResizeImageConfig(
                        width, height, policy, allowUpscaling)));
    }

    private static WidgetNode node(
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Container"),
                properties,
                slots);
    }

    private static WidgetNode imageNode(
            PropertyValue.ImageProviderValue provider,
            String left,
            String top,
            String right,
            String bottom) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Image"),
                Map.of(
                        new PropertyName("image"), provider,
                        new PropertyName("centerSliceLeft"),
                        new PropertyValue.DoubleValue(new BigDecimal(left)),
                        new PropertyName("centerSliceTop"),
                        new PropertyValue.DoubleValue(new BigDecimal(top)),
                        new PropertyName("centerSliceRight"),
                        new PropertyValue.DoubleValue(new BigDecimal(right)),
                        new PropertyName("centerSliceBottom"),
                        new PropertyValue.DoubleValue(new BigDecimal(bottom))),
                Map.of());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegions regions = new ManagedRegions(
                new ManagedRegion("A".repeat(64)),
                new ManagedRegion("B".repeat(64)));
        return new DesignerDocument(
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart",
                        "Sample",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        regions),
                root);
    }

    private Path newProject(String directory, String packageName, String flutterBody)
            throws IOException {
        Path root = temporaryDirectory.resolve(directory);
        Files.createDirectories(root);
        String indentedFlutterBody = flutterBody.lines()
                .map(line -> line.isEmpty() ? line : "  " + line)
                .collect(Collectors.joining("\n"));
        Files.writeString(root.resolve("pubspec.yaml"),
                "name: " + packageName + "\nflutter:\n"
                        + indentedFlutterBody + "\n");
        return root;
    }

    private static void writePackageConfig(
            Path project,
            List<PackageEntry> packages) throws IOException {
        String packageJson = packages.stream()
                .map(entry -> """
                        {"name":%s,"rootUri":%s,"packageUri":"lib/"}
                        """.formatted(json(entry.name()), json(entry.rootUri())).trim())
                .collect(Collectors.joining(","));
        Files.createDirectories(project.resolve(".dart_tool"));
        Files.writeString(project.resolve(".dart_tool/package_config.json"), """
                {"configVersion":2,"packages":[%s]}
                """.formatted(packageJson));
    }

    private static String json(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static void write(Path root, String logicalPath, byte[] bytes)
            throws IOException {
        Path file = root.resolve(logicalPath.replace(
                '/', root.getFileSystem().getSeparator().charAt(0)));
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }

    private static byte[] png(int width, int height) {
        byte[] bytes = new byte[33];
        byte[] signature = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        putBigEndian(bytes, 8, 13);
        bytes[12] = 'I';
        bytes[13] = 'H';
        bytes[14] = 'D';
        bytes[15] = 'R';
        putBigEndian(bytes, 16, width);
        putBigEndian(bytes, 20, height);
        bytes[24] = 8;
        bytes[25] = 6;
        return bytes;
    }

    private static void putBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private record PackageEntry(String name, String rootUri) {
    }
}

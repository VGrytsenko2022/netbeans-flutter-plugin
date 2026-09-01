package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanvasImageResourceBundleTest {

    @Test
    void ownsSortsAndFingerprintsARevisionScopedAssetSnapshot() {
        byte[] oneBytes = {1, 2, 3};
        byte[] twoBytes = {4, 5, 6, 7};
        CanvasImageResource one = CanvasImageResource.create(
                CanvasImageFormat.PNG, 12, 8, oneBytes);
        CanvasImageResource two = CanvasImageResource.create(
                CanvasImageFormat.WEBP, 24, 16, twoBytes);
        CanvasImageAssetId id = CanvasImageAssetId.packageAsset(
                "sample_icons", "assets/background.webp");
        CanvasImageAsset asset = new CanvasImageAsset(
                id,
                one.resourceId(),
                List.of(
                        new CanvasImageVariant(
                                new BigDecimal("3.0"), one.resourceId()),
                        new CanvasImageVariant(
                                new BigDecimal("2.0"), two.resourceId()),
                        new CanvasImageVariant(
                                BigDecimal.ONE, one.resourceId())));

        CanvasImageResourceBundle bundle = new CanvasImageResourceBundle(
                List.of(asset), List.of(two, one));
        oneBytes[0] = 99;
        twoBytes[0] = 99;

        assertEquals(List.of(one, two), bundle.resources());
        assertEquals(List.of(
                BigDecimal.ONE,
                new BigDecimal("2").stripTrailingZeros(),
                new BigDecimal("3").stripTrailingZeros()),
                bundle.find(id).orElseThrow().variants().stream()
                        .map(CanvasImageVariant::scale)
                        .toList());
        assertEquals(new BigDecimal("2").stripTrailingZeros(),
                asset.selectVariant(1.2).scale());
        assertEquals(new BigDecimal("2").stripTrailingZeros(),
                asset.selectVariant(2.2).scale());
        assertEquals(new BigDecimal("3").stripTrailingZeros(),
                asset.selectVariant(2.75).scale());
        assertEquals(7, bundle.totalEncodedBytes());
        assertArrayEquals(new byte[] {1, 2, 3},
                bundle.findResource(one.resourceId())
                        .orElseThrow().copyEncodedBytes());
        byte[] leaked = one.copyEncodedBytes();
        leaked[0] = 88;
        assertArrayEquals(new byte[] {1, 2, 3}, one.copyEncodedBytes());
        assertTrue(bundle.fingerprintSha256().matches("[0-9a-f]{64}"));
        assertEquals(bundle.fingerprintSha256(),
                new CanvasImageResourceBundle(
                        List.of(asset), List.of(one, two))
                        .fingerprintSha256());
        assertFalse(bundle.isEmpty());
    }

    @Test
    void rejectsAmbiguousUnsafeAndUnboundedBundles() {
        assertThrows(IllegalArgumentException.class, () ->
                CanvasImageAssetId.application("../secret.png"));
        assertThrows(IllegalArgumentException.class, () ->
                CanvasImageAssetId.application("assets\\secret.png"));
        assertThrows(IllegalArgumentException.class, () ->
                CanvasImageAssetId.packageAsset("Bad-Package", "asset.png"));
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasImageVariant(BigDecimal.ZERO, "0".repeat(64)));

        CanvasImageResource resource = CanvasImageResource.create(
                CanvasImageFormat.JPEG, 1, 1, new byte[] {42});
        CanvasImageAssetId id = CanvasImageAssetId.application("asset.jpg");
        CanvasImageAsset asset = new CanvasImageAsset(
                id,
                resource.resourceId(),
                List.of(new CanvasImageVariant(
                        BigDecimal.ONE, resource.resourceId())));

        assertThrows(IllegalArgumentException.class, () ->
                new CanvasImageResourceBundle(List.of(asset), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasImageResourceBundle(
                        List.of(), List.of(resource)));
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasImageResourceBundle(
                        List.of(asset, asset), List.of(resource)));
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasImageAsset(
                        id,
                        resource.resourceId(),
                        List.of(
                                new CanvasImageVariant(
                                        BigDecimal.ONE, resource.resourceId()),
                                new CanvasImageVariant(
                                        new BigDecimal("1.0"),
                                        resource.resourceId()))));
    }

    @Test
    void emptyBundleIsCanonicalAndLookupIsExact() {
        CanvasImageResourceBundle empty = CanvasImageResourceBundle.empty();

        assertTrue(empty.isEmpty());
        assertEquals(0, empty.totalEncodedBytes());
        assertEquals(Optional.empty(), empty.find(
                CanvasImageAssetId.application("assets/unknown.png")));
        assertNotEquals("", empty.fingerprintSha256());
    }

    @Test
    void carriesOneConcreteUnavailableReasonWithoutPretendingItResolved() {
        CanvasImageAssetId id = CanvasImageAssetId.application(
                "assets/missing.png");
        CanvasImageResolutionIssue issue = new CanvasImageResolutionIssue(
                id,
                CanvasImageResolutionIssue.Code.MISSING,
                "The declared project asset does not exist.");

        CanvasImageResourceBundle bundle = new CanvasImageResourceBundle(
                List.of(), List.of(), List.of(issue));

        assertFalse(bundle.isEmpty());
        assertEquals(issue, bundle.findIssue(id).orElseThrow());
        assertEquals(Optional.empty(), bundle.find(id));
        assertEquals("missing", issue.code().wireName());
    }
}

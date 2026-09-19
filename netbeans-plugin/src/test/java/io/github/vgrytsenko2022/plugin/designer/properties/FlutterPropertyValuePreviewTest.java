package io.github.vgrytsenko2022.plugin.designer.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.MaterialIconRegistry;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyEditor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterPropertyValuePreviewTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsTheExactMaterialFontContractFromThePackagedArtifactManifest() {
        FlutterPropertyValuePreview.MaterialFontContract contract =
                FlutterPropertyValuePreview.materialFontContract();

        assertEquals(Path.of("bin", "cache", "artifacts", "material_fonts",
                "MaterialIcons-Regular.otf"), contract.relativePath());
        assertEquals(1_645_184L, contract.size());
        assertEquals(
                "d9865b671a09d683d13a863089d8825e0f61a37696ce5d7d448bc8023aa62453",
                contract.sha256());
    }

    @Test
    void resolvesOnlyExactBundledMaterialIconMetadata() {
        MaterialIconRegistry.MaterialIcon star = MaterialIconRegistry.bundled()
                .find("star").orElseThrow();
        PropertyValue.IconDataValue exact = value(star);

        assertEquals(star,
                FlutterPropertyValuePreview.findExactMaterialIcon(exact)
                        .orElseThrow());
        assertTrue(FlutterPropertyValuePreview.findExactMaterialIcon(
                new PropertyValue.IconDataValue(
                        exact.codePoint(), exact.fontFamily(),
                        Optional.of("foreign_package"), false, List.of()))
                .isEmpty());
        assertTrue(FlutterPropertyValuePreview.findExactMaterialIcon(
                new PropertyValue.IconDataValue(
                        exact.codePoint(), exact.fontFamily(),
                        Optional.empty(), true, List.of()))
                .isEmpty());
        assertTrue(FlutterPropertyValuePreview.findExactMaterialIcon(
                PropertyValue.IconDataValue.none()).isEmpty());
    }

    @Test
    void verifiedLoaderReadsOnlyTheCanonicalStableBytesBeforeDecode()
            throws Exception {
        byte[] bytes = {1, 3, 5, 7, 9};
        Path relative = Path.of("bin", "cache", "artifacts", "material_fonts",
                "MaterialIcons-Regular.otf");
        Path font = temporaryDirectory.resolve(relative);
        Files.createDirectories(font.getParent());
        Files.write(font, bytes);
        Font decoded = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        AtomicReference<byte[]> decodedBytes = new AtomicReference<>();
        AtomicInteger decodeCalls = new AtomicInteger();
        FlutterPropertyValuePreview.VerifiedMaterialFontLoader loader =
                new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                        () -> Optional.of(temporaryDirectory),
                        new FlutterPropertyValuePreview.MaterialFontContract(
                                relative, bytes.length, sha256(bytes)),
                        candidate -> {
                            decodeCalls.incrementAndGet();
                            decodedBytes.set(candidate.clone());
                            return Optional.of(decoded);
                        });

        assertSame(decoded, loader.load().orElseThrow());
        assertEquals(List.of(1, 3, 5, 7, 9),
                java.util.stream.IntStream.range(0, decodedBytes.get().length)
                        .map(index -> decodedBytes.get()[index]).boxed().toList());
        assertEquals(1, decodeCalls.get());
    }

    @Test
    void verifiedLoaderFailsClosedBeforeDecodeForMissingWrongOrUnconfiguredFont()
            throws Exception {
        byte[] bytes = {2, 4, 6, 8};
        Path relative = Path.of("bin", "cache", "artifacts", "material_fonts",
                "MaterialIcons-Regular.otf");
        AtomicInteger decodeCalls = new AtomicInteger();
        var contract = new FlutterPropertyValuePreview.MaterialFontContract(
                relative, bytes.length, sha256(bytes));

        var unconfigured = new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                Optional::<Path>empty, contract,
                ignored -> {
                    decodeCalls.incrementAndGet();
                    return Optional.empty();
                });
        assertTrue(unconfigured.load().isEmpty());

        var missing = new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                () -> Optional.of(temporaryDirectory), contract,
                ignored -> {
                    decodeCalls.incrementAndGet();
                    return Optional.empty();
                });
        assertThrows(java.io.IOException.class, missing::load);

        Path font = temporaryDirectory.resolve(relative);
        Files.createDirectories(font.getParent());
        Files.write(font, new byte[]{2, 4, 6});
        assertThrows(java.io.IOException.class, missing::load);

        Files.write(font, bytes);
        var wrongHash = new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                () -> Optional.of(temporaryDirectory),
                new FlutterPropertyValuePreview.MaterialFontContract(
                        relative, bytes.length, "0".repeat(64)),
                ignored -> {
                    decodeCalls.incrementAndGet();
                    return Optional.empty();
                });
        assertTrue(wrongHash.load().isEmpty());
        assertEquals(0, decodeCalls.get());
    }

    @Test
    void firstPreviewRequestLoadsOffTheSwingEdtAndCachesSuccess()
            throws Exception {
        Font decoded = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        CountDownLatch loaderStarted = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        AtomicBoolean loadedOnEdt = new AtomicBoolean(true);
        AtomicInteger loadCalls = new AtomicInteger();
        CountDownLatch loadCompleted = new CountDownLatch(1);
        FlutterPropertyValuePreview.MaterialFontSource source =
                new FlutterPropertyValuePreview.MaterialFontSource(() -> {
                    loadedOnEdt.set(SwingUtilities.isEventDispatchThread());
                    loadCalls.incrementAndGet();
                    loaderStarted.countDown();
                    try {
                        if (!allowCompletion.await(5, TimeUnit.SECONDS)) {
                            throw new java.io.IOException("test loader timed out");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new java.io.IOException(
                                "test loader was interrupted", interrupted);
                    }
                    return Optional.of(decoded);
                }, loadCompleted::countDown);

        AtomicReference<Optional<Font>> first = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> first.set(source.font()));
        assertTrue(first.get().isEmpty(),
                "the first EDT paint must return without waiting for SDK I/O");
        assertTrue(loaderStarted.await(5, TimeUnit.SECONDS));
        assertFalse(loadedOnEdt.get());
        allowCompletion.countDown();
        assertTrue(loadCompleted.await(5, TimeUnit.SECONDS));

        assertSame(decoded, source.font().orElseThrow());
        assertSame(decoded, source.font().orElseThrow());
        assertEquals(1, loadCalls.get());
    }

    @Test
    void fontContractResolutionRunsOnTheWorkerAndLinkageFailureFailsClosed()
            throws Exception {
        CountDownLatch contractRequested = new CountDownLatch(1);
        AtomicBoolean requestedOnEdt = new AtomicBoolean(true);
        AtomicInteger contractCalls = new AtomicInteger();
        CountDownLatch loadCompleted = new CountDownLatch(1);
        FlutterPropertyValuePreview.VerifiedMaterialFontLoader loader =
                new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                        Optional::<Path>empty,
                        () -> {
                            requestedOnEdt.set(
                                    SwingUtilities.isEventDispatchThread());
                            contractCalls.incrementAndGet();
                            contractRequested.countDown();
                            throw new ExceptionInInitializerError(
                                    "synthetic missing manifest");
                        },
                        FlutterPropertyValuePreview::decodeMaterialFont);
        FlutterPropertyValuePreview.MaterialFontSource source =
                new FlutterPropertyValuePreview.MaterialFontSource(
                        loader, loadCompleted::countDown);

        AtomicReference<Optional<Font>> first = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> first.set(source.font()));

        assertTrue(first.get().isEmpty());
        assertTrue(contractRequested.await(5, TimeUnit.SECONDS));
        assertFalse(requestedOnEdt.get(),
                "the manifest contract must never be resolved while painting");
        assertTrue(loadCompleted.await(5, TimeUnit.SECONDS));
        assertTrue(source.font().isEmpty());
        assertEquals(1, contractCalls.get(),
                "an unavailable contract must not be retried by every paint");
    }

    @Test
    void explicitRefreshRecoversAnUnavailablePreviewAfterSettingsChange()
            throws Exception {
        Font decoded = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        CountDownLatch firstCompleted = new CountDownLatch(1);
        CountDownLatch secondCompleted = new CountDownLatch(1);
        FlutterPropertyValuePreview.MaterialFontSource source =
                new FlutterPropertyValuePreview.MaterialFontSource(
                        () -> attempts.incrementAndGet() == 1
                                ? Optional.empty() : Optional.of(decoded),
                        () -> {
                            if (completions.incrementAndGet() == 1) {
                                firstCompleted.countDown();
                            } else {
                                secondCompleted.countDown();
                            }
                        });

        assertTrue(source.font().isEmpty());
        assertTrue(firstCompleted.await(5, TimeUnit.SECONDS));
        assertTrue(source.font().isEmpty());
        assertEquals(1, attempts.get());

        source.refresh();
        assertTrue(secondCompleted.await(5, TimeUnit.SECONDS));
        assertSame(decoded, source.font().orElseThrow());
        assertEquals(2, attempts.get());
    }

    @Test
    void settingsRefreshDuringAnInFlightFailureQueuesAReplacementLoad()
            throws Exception {
        Font decoded = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondCompleted = new CountDownLatch(1);
        FlutterPropertyValuePreview.MaterialFontSource source =
                new FlutterPropertyValuePreview.MaterialFontSource(
                        () -> {
                            int attempt = attempts.incrementAndGet();
                            if (attempt == 1) {
                                firstStarted.countDown();
                                try {
                                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                                        throw new java.io.IOException(
                                                "test refresh timed out");
                                    }
                                } catch (InterruptedException interrupted) {
                                    Thread.currentThread().interrupt();
                                    throw new java.io.IOException(
                                            "test refresh was interrupted",
                                            interrupted);
                                }
                                return Optional.empty();
                            }
                            return Optional.of(decoded);
                        },
                        () -> {
                            if (completions.incrementAndGet() == 2) {
                                secondCompleted.countDown();
                            }
                        });

        assertTrue(source.font().isEmpty());
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
        source.refresh();
        releaseFirst.countDown();

        assertTrue(secondCompleted.await(5, TimeUnit.SECONDS));
        assertSame(decoded, source.font().orElseThrow());
        assertEquals(2, attempts.get());
        assertEquals(2, completions.get());
    }

    @Test
    void decodesConfiguredPinnedMaterialFontWhenRequested() throws Exception {
        String configuredSdk = System.getProperty(
                "material.icon.preview.flutter.sdk", "").trim();
        assumeTrue(!configuredSdk.isEmpty(), "run with "
                + "-Dmaterial.icon.preview.flutter.sdk=<sdk> for the real-font check");
        Path sdkRoot = Path.of(configuredSdk).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(sdkRoot),
                "the configured Flutter SDK directory must exist");

        FlutterPropertyValuePreview.VerifiedMaterialFontLoader loader =
                new FlutterPropertyValuePreview.VerifiedMaterialFontLoader(
                        () -> Optional.of(sdkRoot),
                        FlutterPropertyValuePreview.materialFontContract(),
                        FlutterPropertyValuePreview::decodeMaterialFont);

        Font decoded = loader.load().orElseThrow();
        assertEquals("MaterialIcons-Regular",
                decoded.getFontName(java.util.Locale.ROOT));
        assertEquals("Material Icons",
                decoded.getFamily(java.util.Locale.ROOT));
        List<Integer> codePoints = MaterialIconRegistry.bundled().entries()
                .stream().map(MaterialIconRegistry.MaterialIcon::codePoint)
                .distinct().toList();
        assertEquals(8_622, codePoints.size());
        FontRenderContext context = new FontRenderContext(
                new AffineTransform(), true, true);
        for (int codePoint : codePoints) {
            assertTrue(decoded.canDisplay(codePoint),
                    () -> "font cannot display U+"
                            + Integer.toHexString(codePoint).toUpperCase());
            char[] characters = Character.toChars(codePoint);
            GlyphVector vector = decoded.layoutGlyphVector(
                    context, characters, 0, characters.length,
                    Font.LAYOUT_LEFT_TO_RIGHT);
            assertEquals(1, vector.getNumGlyphs());
            assertNotEquals(decoded.getMissingGlyphCode(),
                    vector.getGlyphCode(0));
            assertFalse(vector.getVisualBounds().isEmpty());
        }
    }

    @Test
    void paintsAnInjectedVectorGlyphOutline() {
        Icon preview = FlutterPropertyValuePreview.materialIconForTesting(
                'A', 20, new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        BufferedImage image = new BufferedImage(
                24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            JLabel label = new JLabel();
            label.setForeground(Color.BLACK);
            preview.paintIcon(label, graphics, 2, 2);
        } finally {
            graphics.dispose();
        }

        assertEquals(20, preview.getIconWidth());
        assertEquals(20, preview.getIconHeight());
        assertTrue(nonTransparentPixels(image, new Rectangle(0, 0, 24, 24)) > 12);
    }

    @Test
    void iconDataPropertySheetCellsArePaintableAndKeepReadableText() {
        var definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow()
                .properties().stream()
                .filter(property -> property.name().value().equals("icon"))
                .findFirst().orElseThrow();
        FlutterTypedPropertyEditors.Binding binding =
                FlutterTypedPropertyEditors.binding(definition).orElseThrow();
        PropertyEditor editor = binding.createEditor();
        editor.setValue(FlutterPropertyCellValue.explicit(
                PropertyValue.IconDataValue.none()));

        BufferedImage image = new BufferedImage(
                240, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            assertTrue(editor.isPaintable());
            editor.paintValue(graphics,
                    new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        } finally {
            graphics.dispose();
        }

        assertTrue(nonWhitePixels(image, new Rectangle(0, 0, 24, 24)) > 4,
                "the explicit None preview must be visible");
        assertTrue(nonWhitePixels(image, new Rectangle(24, 0, 100, 24)) > 4,
                "the readable None label must remain visible");
        assertFalse(binding.optional(),
                "Icon.icon is required positional nullable, not an omitted default");
    }

    private static PropertyValue.IconDataValue value(
            MaterialIconRegistry.MaterialIcon icon) {
        return new PropertyValue.IconDataValue(
                Optional.of(icon.codePoint()),
                Optional.of(icon.fontFamily()),
                Optional.empty(), icon.matchTextDirection(), List.of());
    }

    private static int nonTransparentPixels(BufferedImage image, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int nonWhitePixels(BufferedImage image, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                if (image.getRGB(x, y) != Color.WHITE.getRGB()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}

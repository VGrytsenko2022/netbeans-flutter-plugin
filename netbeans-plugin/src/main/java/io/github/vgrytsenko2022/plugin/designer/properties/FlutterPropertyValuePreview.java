package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.canvas.runner.CanvasRunnerBundle;
import io.github.vgrytsenko2022.designer.catalog.MaterialIconRegistry;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.plugin.settings.FlutterSettings;
import io.github.vgrytsenko2022.plugin.settings.FlutterToolchainService;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.openide.util.RequestProcessor;

/** Shared NetBeans Properties previews for closed visual property values. */
final class FlutterPropertyValuePreview {
    static final int MATERIAL_ICON_LIST_SIZE = 20;

    private static final Logger LOGGER = Logger.getLogger(
            FlutterPropertyValuePreview.class.getName());
    private FlutterPropertyValuePreview() {
    }

    static boolean isPaintable(FlutterTypedPropertyEditors.EditorKind kind) {
        return kind == FlutterTypedPropertyEditors.EditorKind.ICON_DATA;
    }

    static void paintValue(
            Graphics graphics,
            Rectangle box,
            FlutterPropertyCellValue cell) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(cell, "cell");

        String text = cell.explicitValue()
                .map(PropertyValueFormatter::format)
                .orElse(FlutterPropertyCellValue.NOT_SET_TEXT);
        if (!(graphics instanceof Graphics2D source)) {
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(text, box.x + 2,
                    centeredBaseline(box, metrics));
            return;
        }

        Graphics2D copy = (Graphics2D) source.create();
        try {
            copy.clip(box);
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = box.x + 2;
            Optional<PropertyValue> explicit = cell.explicitValue();
            if (explicit.orElse(null) instanceof PropertyValue.IconDataValue iconData) {
                int previewSize = Math.max(12, Math.min(18, box.height - 2));
                int y = box.y + Math.max(0, (box.height - previewSize) / 2);
                if (iconData.equals(PropertyValue.IconDataValue.none())) {
                    paintNone(copy, x, y, previewSize);
                } else {
                    Optional<MaterialIconRegistry.MaterialIcon> icon =
                            findExactMaterialIcon(iconData);
                    if (icon.isPresent()) {
                        paintGlyphOrUnavailable(copy, icon.orElseThrow().codePoint(),
                                x, y, previewSize, materialFontSource().font());
                    } else {
                        paintUnavailable(copy, x, y, previewSize);
                    }
                }
                x += previewSize + 6;
            }

            FontMetrics metrics = copy.getFontMetrics();
            copy.drawString(text, x, centeredBaseline(box, metrics));
        } finally {
            copy.dispose();
        }
    }

    static Icon materialIcon(MaterialIconRegistry.MaterialIcon icon) {
        Objects.requireNonNull(icon, "icon");
        return new MaterialGlyphIcon(
                icon.codePoint(), MATERIAL_ICON_LIST_SIZE, materialFontSource());
    }

    static void refreshMaterialIconFont() {
        materialFontSource().refresh();
    }

    static Optional<MaterialIconRegistry.MaterialIcon> findExactMaterialIcon(
            PropertyValue.IconDataValue value) {
        Objects.requireNonNull(value, "value");
        MaterialIconRegistry materialIcons = materialIcons();
        if (!value.fontFamily().equals(Optional.of(
                materialIcons.metadata().fontFamily()))
                || value.fontPackage().isPresent()
                || !value.fontFamilyFallback().isEmpty()
                || value.codePoint().isEmpty()) {
            return Optional.empty();
        }
        int codePoint = value.codePoint().orElseThrow();
        return materialIcons.find(codePoint, value.matchTextDirection());
    }

    static MaterialFontContract materialFontContract() {
        return MaterialFontContractHolder.CONTRACT;
    }

    static Icon materialIconForTesting(
            int codePoint,
            int size,
            Font font) {
        Objects.requireNonNull(font, "font");
        return new MaterialGlyphIcon(codePoint, size, () -> Optional.of(font));
    }

    private static MaterialFontSource materialFontSource() {
        return MaterialFontSourceHolder.SOURCE;
    }

    private static int centeredBaseline(Rectangle box, FontMetrics metrics) {
        return box.y + (box.height + metrics.getAscent()
                - metrics.getDescent()) / 2;
    }

    private static void paintGlyphOrUnavailable(
            Graphics2D graphics,
            int codePoint,
            int x,
            int y,
            int size,
            Optional<Font> sourceFont) {
        if (sourceFont.isEmpty()) {
            paintUnavailable(graphics, x, y, size);
            return;
        }
        Font font = sourceFont.orElseThrow().deriveFont((float) size);
        String glyph = new String(Character.toChars(codePoint));
        if (!font.canDisplay(codePoint)) {
            paintUnavailable(graphics, x, y, size);
            return;
        }
        char[] characters = glyph.toCharArray();
        GlyphVector vector = font.layoutGlyphVector(
                graphics.getFontRenderContext(), characters, 0,
                characters.length, Font.LAYOUT_LEFT_TO_RIGHT);
        if (vector.getNumGlyphs() != 1
                || vector.getGlyphCode(0) == font.getMissingGlyphCode()) {
            paintUnavailable(graphics, x, y, size);
            return;
        }
        Rectangle2D bounds = vector.getVisualBounds();
        if (bounds.isEmpty()) {
            paintUnavailable(graphics, x, y, size);
            return;
        }
        float glyphX = (float) (x + (size - bounds.getWidth()) / 2.0
                - bounds.getX());
        float glyphY = (float) (y + (size - bounds.getHeight()) / 2.0
                - bounds.getY());
        graphics.drawGlyphVector(vector, glyphX, glyphY);
    }

    private static void paintNone(
            Graphics2D graphics,
            int x,
            int y,
            int size) {
        int inset = Math.max(2, size / 8);
        int diameter = Math.max(2, size - (inset * 2) - 1);
        graphics.drawOval(x + inset, y + inset, diameter, diameter);
        graphics.drawLine(x + inset + 1, y + size - inset - 2,
                x + size - inset - 2, y + inset + 1);
    }

    private static void paintUnavailable(
            Graphics2D graphics,
            int x,
            int y,
            int size) {
        int inset = Math.max(2, size / 8);
        int edge = Math.max(2, size - (inset * 2) - 1);
        graphics.drawRoundRect(x + inset, y + inset, edge, edge, 3, 3);
        graphics.drawLine(x + inset + 2, y + size - inset - 3,
                x + size - inset - 3, y + inset + 2);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("The Java runtime does not provide SHA-256",
                    impossible);
        }
    }

    record MaterialFontContract(Path relativePath, long size, String sha256) {
        MaterialFontContract {
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(sha256, "sha256");
            if (relativePath.isAbsolute()
                    || relativePath.getNameCount() != 5
                    || size <= 0
                    || size > 4L * 1024 * 1024
                    || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "Invalid Material icon font contract");
            }
        }
    }

    @FunctionalInterface
    private interface FontProvider {
        Optional<Font> font();
    }

    @FunctionalInterface
    interface CheckedFontLoader {
        Optional<Font> load() throws IOException, FontFormatException;
    }

    @FunctionalInterface
    interface FontDecoder {
        Optional<Font> decode(byte[] bytes) throws IOException, FontFormatException;
    }

    private record MaterialGlyphIcon(
            int codePoint,
            int size,
            FontProvider fontProvider) implements Icon {
        MaterialGlyphIcon {
            if (!Character.isValidCodePoint(codePoint)
                    || codePoint == 0
                    || size < 8
                    || size > 64) {
                throw new IllegalArgumentException(
                        "Invalid Material icon preview glyph");
            }
            Objects.requireNonNull(fontProvider, "fontProvider");
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (!(graphics instanceof Graphics2D source)) {
                return;
            }
            Graphics2D copy = (Graphics2D) source.create();
            try {
                Color color = component != null ? component.getForeground() : null;
                if (color == null) {
                    color = UIManager.getColor("Label.foreground");
                }
                if (color != null) {
                    copy.setColor(color);
                }
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintGlyphOrUnavailable(copy, codePoint, x, y, size,
                        fontProvider.font());
            } finally {
                copy.dispose();
            }
        }
    }

    static final class MaterialFontSource implements FontProvider {
        private static final RequestProcessor LOADER = new RequestProcessor(
                FlutterPropertyValuePreview.class.getName()
                + "-material-icon-font", 1, true);

        private final CheckedFontLoader fontLoader;
        private final Runnable loadCompleted;
        private volatile Font loaded;
        private LoadState state = LoadState.IDLE;
        private long generation;
        private boolean refreshPending;

        MaterialFontSource() {
            this(new VerifiedMaterialFontLoader(
                    () -> new FlutterToolchainService().resolve().flutterSdk()
                            .map(sdk -> sdk.home()),
                    FlutterPropertyValuePreview::materialFontContractIfAvailable,
                    FlutterPropertyValuePreview::decodeMaterialFont));
        }

        MaterialFontSource(CheckedFontLoader fontLoader) {
            this(fontLoader, () -> {
            });
        }

        MaterialFontSource(
                CheckedFontLoader fontLoader,
                Runnable loadCompleted) {
            this.fontLoader = Objects.requireNonNull(fontLoader, "fontLoader");
            this.loadCompleted = Objects.requireNonNull(
                    loadCompleted, "loadCompleted");
        }

        @Override
        public Optional<Font> font() {
            Font cached = loaded;
            if (cached != null) {
                return Optional.of(cached);
            }
            requestLoad();
            return Optional.empty();
        }

        private void requestLoad() {
            long ticket;
            synchronized (this) {
                if (state == LoadState.READY || state == LoadState.LOADING) {
                    return;
                }
                if (state == LoadState.UNAVAILABLE) {
                    return;
                }
                state = LoadState.LOADING;
                ticket = ++generation;
            }
            LOADER.post(() -> loadInBackground(ticket));
        }

        private void loadInBackground(long ticket) {
            Optional<Font> candidate = Optional.empty();
            try {
                candidate = fontLoader.load();
            } catch (IOException | FontFormatException
                    | RuntimeException | LinkageError failure) {
                LOGGER.log(Level.FINE,
                        "Material icon property preview is unavailable",
                        failure);
            }
            Optional<Font> completed = candidate;
            SwingUtilities.invokeLater(() -> complete(ticket, completed));
        }

        private void complete(long ticket, Optional<Font> candidate) {
            boolean retry;
            synchronized (this) {
                if (ticket != generation || state != LoadState.LOADING) {
                    return;
                }
                retry = refreshPending;
                refreshPending = false;
                if (retry) {
                    loaded = null;
                    state = LoadState.IDLE;
                } else if (candidate.isPresent()) {
                    loaded = candidate.orElseThrow();
                    state = LoadState.READY;
                } else {
                    state = LoadState.UNAVAILABLE;
                }
            }
            if (retry) {
                requestLoad();
            } else if (candidate.isPresent()) {
                for (Window window : Window.getWindows()) {
                    if (window.isDisplayable()) {
                        window.repaint();
                    }
                }
            }
            try {
                loadCompleted.run();
            } catch (RuntimeException failure) {
                LOGGER.log(Level.FINE,
                        "A Material icon preview completion callback failed",
                        failure);
            }
        }

        void refresh() {
            synchronized (this) {
                if (state == LoadState.READY) {
                    return;
                }
                if (state == LoadState.LOADING) {
                    refreshPending = true;
                    return;
                }
                state = LoadState.IDLE;
            }
            requestLoad();
        }

        private enum LoadState {
            IDLE,
            LOADING,
            READY,
            UNAVAILABLE
        }
    }

    static final class VerifiedMaterialFontLoader implements CheckedFontLoader {
        private final Supplier<Optional<Path>> sdkRootSupplier;
        private final Supplier<Optional<MaterialFontContract>> contractSupplier;
        private final FontDecoder decoder;

        VerifiedMaterialFontLoader(
                Supplier<Optional<Path>> sdkRootSupplier,
                MaterialFontContract contract,
                FontDecoder decoder) {
            this(sdkRootSupplier, () -> Optional.of(contract), decoder);
        }

        VerifiedMaterialFontLoader(
                Supplier<Optional<Path>> sdkRootSupplier,
                Supplier<Optional<MaterialFontContract>> contractSupplier,
                FontDecoder decoder) {
            this.sdkRootSupplier = Objects.requireNonNull(
                    sdkRootSupplier, "sdkRootSupplier");
            this.contractSupplier = Objects.requireNonNull(
                    contractSupplier, "contractSupplier");
            this.decoder = Objects.requireNonNull(decoder, "decoder");
        }

        @Override
        public Optional<Font> load() throws IOException, FontFormatException {
            Optional<MaterialFontContract> configuredContract =
                    contractSupplier.get();
            if (configuredContract.isEmpty()) {
                return Optional.empty();
            }
            MaterialFontContract contract = configuredContract.orElseThrow();
            Optional<Path> configuredRoot = sdkRootSupplier.get();
            if (configuredRoot.isEmpty()) {
                return Optional.empty();
            }
            Path sdkRoot = configuredRoot.orElseThrow()
                    .toAbsolutePath().normalize();
            Path fontFile = sdkRoot.resolve(contract.relativePath()).normalize();
            if (!fontFile.startsWith(sdkRoot)) {
                return Optional.empty();
            }
            requireNoLinkedComponents(sdkRoot, fontFile);
            byte[] bytes = readStableFont(fontFile, contract.size());
            if (!sha256(bytes).equals(contract.sha256())) {
                return Optional.empty();
            }
            return decoder.decode(bytes);
        }

        private static void requireNoLinkedComponents(Path root, Path target)
                throws IOException {
            rejectLinkOrReparse(root);
            Path current = root;
            for (Path segment : root.relativize(target)) {
                current = current.resolve(segment);
                rejectLinkOrReparse(current);
            }
        }

        private static void rejectLinkOrReparse(Path path) throws IOException {
            BasicFileAttributes noFollow = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (Files.isSymbolicLink(path)
                    || noFollow.isSymbolicLink()
                    || noFollow.isOther()) {
                throw new IOException(
                        "Material icon font path contains a link or reparse point");
            }
            BasicFileAttributes followed = Files.readAttributes(
                    path, BasicFileAttributes.class);
            if (!sameFileKey(noFollow.fileKey(), followed.fileKey())) {
                throw new IOException(
                        "Material icon font path resolves through a link or junction");
            }
            try {
                Object rawAttributes = Files.getAttribute(
                        path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
                if (rawAttributes instanceof Number value
                        && (value.intValue() & 0x400) != 0) {
                    throw new IOException(
                            "Material icon font path contains a Windows reparse point");
                }
            } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
                // Non-Windows providers do not expose the DOS reparse bit.
            }
        }

        private static byte[] readStableFont(Path file, long expectedSize)
                throws IOException {
            BasicFileAttributes initial = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!initial.isRegularFile() || initial.size() != expectedSize) {
                throw new IOException("Material icon font size does not match its contract");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) expectedSize);
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            try (InputStream input = Files.newInputStream(
                    file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > expectedSize) {
                        throw new IOException(
                                "Material icon font grew while it was read");
                    }
                    bytes.write(buffer, 0, count);
                }
            }
            BasicFileAttributes after = Files.readAttributes(
                    file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (total != expectedSize
                    || after.size() != initial.size()
                    || !after.lastModifiedTime().equals(initial.lastModifiedTime())
                    || !sameFileKey(initial.fileKey(), after.fileKey())) {
                throw new IOException("Material icon font changed while it was read");
            }
            return bytes.toByteArray();
        }

        private static boolean sameFileKey(Object first, Object second) {
            return first == null ? second == null : first.equals(second);
        }

    }

    static Optional<Font> decodeMaterialFont(byte[] bytes)
            throws IOException, FontFormatException {
        Font font = Font.createFont(Font.TRUETYPE_FONT,
                new ByteArrayInputStream(bytes));
        if (!"MaterialIcons-Regular".equals(font.getFontName(java.util.Locale.ROOT))
                || !"Material Icons".equals(font.getFamily(java.util.Locale.ROOT))) {
            return Optional.empty();
        }
        for (MaterialIconRegistry.MaterialIcon icon : materialIcons().entries()) {
            if (!font.canDisplay(icon.codePoint())) {
                return Optional.empty();
            }
        }
        return Optional.of(font);
    }

    private static Optional<MaterialFontContract>
            materialFontContractIfAvailable() {
        try {
            return Optional.of(materialFontContract());
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.log(Level.FINE,
                    "Material icon font contract is unavailable", failure);
            return Optional.empty();
        }
    }

    private static final class MaterialFontSourceHolder {
        private static final MaterialFontSource SOURCE = create();

        private MaterialFontSourceHolder() {
        }

        private static MaterialFontSource create() {
            MaterialFontSource source = new MaterialFontSource();
            FlutterSettings.getDefault().addChangeListener(source::refresh);
            return source;
        }
    }

    private static MaterialIconRegistry materialIcons() {
        return MaterialIconRegistryHolder.REGISTRY;
    }

    private static final class MaterialIconRegistryHolder {
        private static final MaterialIconRegistry REGISTRY =
                MaterialIconRegistry.bundled();

        private MaterialIconRegistryHolder() {
        }
    }

    private static final class MaterialFontContractHolder {
        private static final String WEB_PATH =
                "assets/fonts/MaterialIcons-Regular.otf";
        private static final MaterialFontContract CONTRACT = readContract();

        private MaterialFontContractHolder() {
        }

        private static MaterialFontContract readContract() {
            byte[] bytes;
            try (InputStream input = CanvasRunnerBundle.openWebArtifactManifest()) {
                bytes = input.readNBytes(64 * 1024 + 1);
            } catch (IOException failure) {
                throw new IllegalStateException(
                        "Cannot read the packaged Web Canvas artifact manifest",
                        failure);
            }
            if (bytes.length == 0 || bytes.length > 64 * 1024) {
                throw new IllegalStateException(
                        "The packaged Web Canvas artifact manifest is invalid");
            }
            String manifest = new String(bytes, StandardCharsets.US_ASCII);
            MaterialFontContract found = null;
            for (String line : manifest.split("\\n", -1)) {
                if (!line.startsWith("file|" + WEB_PATH + "|")) {
                    continue;
                }
                if (found != null || line.indexOf('\r') >= 0) {
                    throw new IllegalStateException(
                            "The Material icon font manifest entry is ambiguous");
                }
                String[] fields = line.split("\\|", -1);
                if (fields.length != 4) {
                    throw new IllegalStateException(
                            "The Material icon font manifest entry is invalid");
                }
                try {
                    found = new MaterialFontContract(
                            Path.of("bin", "cache", "artifacts",
                                    "material_fonts", "MaterialIcons-Regular.otf"),
                            Long.parseLong(fields[2]), fields[3]);
                } catch (IllegalArgumentException failure) {
                    throw new IllegalStateException(
                            "The Material icon font manifest entry is invalid",
                            failure);
                }
            }
            if (found == null) {
                throw new IllegalStateException(
                        "The packaged Web Canvas artifact manifest has no Material icon font");
            }
            return found;
        }
    }
}

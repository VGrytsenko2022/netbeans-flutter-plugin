package io.github.vgrytsenko2022.plugin.designer.assets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlutterAssetResolverTest {
    @TempDir
    Path temporaryDirectory;

    private final FlutterAssetResolver resolver = new FlutterAssetResolver();

    @Test
    void inventoriesAppAndPackageAssetsWithPinnedVariantSelection() throws Exception {
        Path app = newProject("inventory", "sample_app", """
                  assets:
                    - assets/logo.png
                    - assets/only.png
                    - assets/gallery/
                """);
        write(app, "assets/logo.png", png(10, 10));
        write(app, "assets/2.0x/logo.png", png(20, 20));
        write(app, "assets/4x/logo.png", png(40, 40));
        write(app, "assets/2.0x/only.png", png(22, 22));
        write(app, "assets/gallery/a.jpg", jpeg(11, 12));
        write(app, "assets/gallery/b.gif", gif(13, 14));
        write(app, "assets/gallery/c.webp", webp(15, 16));

        Path imagePackage = app.resolve("packages/image_pack");
        Files.createDirectories(imagePackage);
        Files.writeString(imagePackage.resolve("pubspec.yaml"), """
                name: image_pack
                flutter:
                  assets:
                    - path: icons/mark.png
                """);
        write(imagePackage, "icons/mark.png", png(17, 18));
        writePackageConfig(app, List.of(
                new PackageEntry("sample_app", "../"),
                new PackageEntry("image_pack", "../packages/image_pack/")));

        FlutterAssetInventory inventory = resolver.resolve(app);

        assertTrue(inventory.diagnostics().isEmpty(), inventory.diagnostics().toString());
        assertEquals(List.of(
                "app:assets/gallery/a.jpg",
                "app:assets/gallery/b.gif",
                "app:assets/gallery/c.webp",
                "app:assets/logo.png",
                "app:assets/only.png",
                "package:image_pack:icons/mark.png"),
                inventory.assets().stream()
                        .map(asset -> asset.id().wireName())
                        .toList());
        assertEquals(Set.of(
                FlutterImageFormat.PNG,
                FlutterImageFormat.JPEG,
                FlutterImageFormat.GIF,
                FlutterImageFormat.WEBP),
                inventory.assets().stream()
                        .flatMap(asset -> asset.variants().stream())
                        .map(FlutterAssetVariant::format)
                        .collect(Collectors.toSet()));
        assertEquals(
                FlutterAssetInventory.SELECTION_ALGORITHM,
                inventory.selectionAlgorithm());
        assertEquals(64, inventory.fingerprintSha256().length());

        FlutterAsset logo = inventory.find(
                FlutterAssetId.app("assets/logo.png")).orElseThrow();
        assertEquals(List.of(1.0, 2.0, 4.0), logo.variants().stream()
                .map(FlutterAssetVariant::scale)
                .toList());
        assertEquals(1.0, logo.selectVariant(0.5).scale());
        assertEquals(2.0, logo.selectVariant(1.5).scale());
        assertEquals(2.0, logo.selectVariant(2.0).scale());
        assertEquals(2.0, logo.selectVariant(3.0).scale());
        assertEquals(4.0, logo.selectVariant(3.01).scale());
        assertEquals(4.0, logo.selectVariant(9.0).scale());
        assertThrows(IllegalArgumentException.class,
                () -> logo.selectVariant(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> logo.selectVariant(0.0));
        FlutterAsset variantOnly = inventory.find(
                FlutterAssetId.app("assets/only.png")).orElseThrow();
        assertEquals(List.of(2.0), variantOnly.variants().stream()
                .map(FlutterAssetVariant::scale)
                .toList());
        assertEquals(2.0, variantOnly.selectVariant(1.0).scale());
    }

    @Test
    void carriesExternalPackageRootsWithoutFingerprintingTheirPaths()
            throws Exception {
        Path app = newProject(
                "watch-root-app", "watch_root_app", "assets: []");
        Path firstPackage = packageWithMagicDetectedAsset(
                "watch-root-package-a", "image_pack");
        Path secondPackage = packageWithMagicDetectedAsset(
                "watch-root-package-b", "image_pack");
        writePackageConfig(app, List.of(
                new PackageEntry("watch_root_app", "../"),
                new PackageEntry(
                        "image_pack",
                        firstPackage.toUri().toString())));

        FlutterAssetInventory first = resolver.resolve(app);
        assertEquals(
                List.of(firstPackage.toRealPath()),
                first.internalPackageWatchRoots());
        assertEquals(
                List.of("package:image_pack:assets/extensionless"),
                first.assets().stream()
                        .map(asset -> asset.id().wireName())
                        .toList());

        writePackageConfig(app, List.of(
                new PackageEntry("watch_root_app", "../"),
                new PackageEntry(
                        "image_pack",
                        secondPackage.toUri().toString())));
        FlutterAssetInventory second = resolver.resolve(app);

        assertEquals(
                List.of(secondPackage.toRealPath()),
                second.internalPackageWatchRoots());
        assertEquals(first.fingerprintSha256(), second.fingerprintSha256(),
                "process-local package paths leaked into the asset fingerprint");
    }

    @Test
    void exposesOnlyParsedNameMatchedPackagesAsWatchRoots()
            throws Exception {
        Path app = newProject(
                "validated-watch-roots", "validated_watch_app", "assets: []");
        Path valid = packageWithMagicDetectedAsset(
                "valid-watch-package", "valid_pack");
        Path missingPubspec = temporaryDirectory.resolve(
                "missing-pubspec-package");
        Files.createDirectories(missingPubspec);
        Path malformedPubspec = temporaryDirectory.resolve(
                "malformed-pubspec-package");
        Files.createDirectories(malformedPubspec);
        Files.writeString(malformedPubspec.resolve("pubspec.yaml"),
                "name: [not-a-scalar\n");
        Path mismatched = packageWithMagicDetectedAsset(
                "mismatched-watch-package", "actual_pack");
        writePackageConfig(app, List.of(
                new PackageEntry("validated_watch_app", "../"),
                new PackageEntry("valid_pack", valid.toUri().toString()),
                new PackageEntry(
                        "missing_pack", missingPubspec.toUri().toString()),
                new PackageEntry(
                        "malformed_pack", malformedPubspec.toUri().toString()),
                new PackageEntry(
                        "configured_pack", mismatched.toUri().toString())));

        FlutterAssetInventory inventory = resolver.resolve(app);

        assertEquals(
                List.of(valid.toRealPath()),
                inventory.internalPackageWatchRoots());
        assertFalse(inventory.internalPackageWatchRoots().contains(
                missingPubspec.toRealPath()));
        assertFalse(inventory.internalPackageWatchRoots().contains(
                malformedPubspec.toRealPath()));
        assertFalse(inventory.internalPackageWatchRoots().contains(
                mismatched.toRealPath()));
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.target().equals("package:configured_pack")
                && diagnostic.reason().contains("actual_pack")));
    }

    @Test
    void givesIdsUnambiguousWireAndDisplayNames() {
        FlutterAssetId app = FlutterAssetId.app("assets/icon.png");
        FlutterAssetId packageAsset = FlutterAssetId.packageAsset(
                "image_pack", "icons/icon.png");

        assertEquals("app:assets/icon.png", app.wireName());
        assertEquals(app.wireName(), app.displayName());
        assertEquals(
                "package:image_pack:icons/icon.png",
                packageAsset.wireName());
        assertNotEquals(app, packageAsset);
        assertThrows(IllegalArgumentException.class,
                () -> FlutterAssetId.app("../icon.png"));
        assertThrows(IllegalArgumentException.class,
                () -> FlutterAssetId.app("/icon.png"));
        assertThrows(IllegalArgumentException.class,
                () -> FlutterAssetId.app("assets\\icon.png"));
        assertThrows(IllegalArgumentException.class,
                () -> FlutterAssetId.app("C:/icon.png"));
    }

    @Test
    void rejectsUnsafePathsAndUnsupportedMagicWithConcreteDiagnostics()
            throws Exception {
        Path app = newProject("unsafe", "sample_app", """
                  assets:
                    - ../outside.png
                    - C:/secret.png
                    - 'assets\\evil.png'
                    - assets/ok.png
                    - assets/not_image.txt
                """);
        write(temporaryDirectory, "outside.png", png(1, 1));
        write(app, "assets/ok.png", png(2, 3));
        write(app, "assets/not_image.txt", "plain text".getBytes(StandardCharsets.UTF_8));
        writePackageConfig(app, List.of(new PackageEntry("sample_app", "../")));

        FlutterAssetInventory inventory = resolver.resolve(app);

        assertEquals(List.of("app:assets/ok.png"), inventory.assets().stream()
                .map(asset -> asset.id().wireName())
                .toList());
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("normalize asset path")
                && diagnostic.target().contains("../outside.png")
                && diagnostic.reason().contains("traversal")));
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("normalize asset path")
                && diagnostic.target().contains("C:/secret.png")
                && diagnostic.reason().contains("relative POSIX")));
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("normalize asset path")
                && diagnostic.target().contains("assets\\evil.png")
                && diagnostic.reason().contains("relative POSIX")));
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("inspect asset image")
                && diagnostic.target().contains("app:assets/not_image.txt")
                && diagnostic.reason().contains("PNG, JPEG, GIF, and WebP")));
        assertTrue(inventory.diagnostics().stream().allMatch(diagnostic ->
                !diagnostic.operation().isBlank()
                && !diagnostic.target().isBlank()
                && !diagnostic.reason().isBlank()));
    }

    @Test
    void rejectsASymlinkThatEscapesThePackageRoot() throws Exception {
        Path app = newProject("symlink", "sample_app", """
                  assets:
                    - assets/escape.png
                """);
        Path outside = temporaryDirectory.resolve("outside-link-target.png");
        Files.write(outside, png(3, 3));
        Files.createDirectories(app.resolve("assets"));
        try {
            Files.createSymbolicLink(app.resolve("assets/escape.png"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, "symbolic links are unavailable: " + exception.getMessage());
        }
        writePackageConfig(app, List.of(new PackageEntry("sample_app", "../")));

        FlutterAssetInventory inventory = resolver.resolve(app);

        assertTrue(inventory.assets().isEmpty());
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.target().contains("app:assets/escape.png")
                && diagnostic.reason().contains("real path escapes package root")));
    }

    @Test
    void enforcesCountFileTotalDimensionAndPixelBounds() throws Exception {
        Path countApp = newProject("count", "count_app", """
                  assets:
                    - assets/a.png
                    - assets/b.png
                """);
        write(countApp, "assets/a.png", png(1, 1));
        write(countApp, "assets/b.png", png(1, 1));
        writePackageConfig(countApp, List.of(new PackageEntry("count_app", "../")));
        FlutterAssetInventory countInventory = resolver.resolve(
                countApp,
                new FlutterAssetLimits(1, 1_000, 1_000, 100, 10_000));
        assertEquals(List.of("app:assets/a.png"), countInventory.assets().stream()
                .map(asset -> asset.id().wireName()).toList());
        assertDiagnostic(countInventory, "enforce asset count", "exceeds limit 1");

        Path physicalCountApp = newProject("physical-count", "physical_count_app", """
                  assets:
                    - assets/logo.png
                """);
        write(physicalCountApp, "assets/logo.png", png(1, 1));
        write(physicalCountApp, "assets/2x/logo.png", png(2, 2));
        writePackageConfig(physicalCountApp,
                List.of(new PackageEntry("physical_count_app", "../")));
        FlutterAssetInventory physicalCountInventory = resolver.resolve(
                physicalCountApp,
                new FlutterAssetLimits(1, 1_000, 1_000, 100, 10_000));
        assertDiagnostic(
                physicalCountInventory,
                "enforce asset file count",
                "exceeds limit 1");

        Path fileApp = newProject("file", "file_app", """
                  assets:
                    - assets/large.png
                """);
        write(fileApp, "assets/large.png", padded(png(1, 1), 80));
        writePackageConfig(fileApp, List.of(new PackageEntry("file_app", "../")));
        FlutterAssetInventory fileInventory = resolver.resolve(
                fileApp,
                new FlutterAssetLimits(10, 40, 100, 100, 10_000));
        assertDiagnostic(fileInventory, "enforce asset file bytes", "exceeds limit 40");

        Path totalApp = newProject("total", "total_app", """
                  assets:
                    - assets/a.png
                    - assets/b.png
                """);
        write(totalApp, "assets/a.png", padded(png(1, 1), 35));
        write(totalApp, "assets/b.png", padded(png(1, 1), 35));
        writePackageConfig(totalApp, List.of(new PackageEntry("total_app", "../")));
        FlutterAssetInventory totalInventory = resolver.resolve(
                totalApp,
                new FlutterAssetLimits(10, 40, 50, 100, 10_000));
        assertEquals(1, totalInventory.assets().size());
        assertDiagnostic(totalInventory, "enforce total asset bytes", "total limit 50");

        Path dimensionApp = newProject("dimension", "dimension_app", """
                  assets:
                    - assets/wide.png
                """);
        write(dimensionApp, "assets/wide.png", png(101, 1));
        writePackageConfig(dimensionApp,
                List.of(new PackageEntry("dimension_app", "../")));
        FlutterAssetInventory dimensionInventory = resolver.resolve(
                dimensionApp,
                new FlutterAssetLimits(10, 1_000, 1_000, 100, 10_000));
        assertDiagnostic(dimensionInventory, "enforce asset dimensions", "101x1");

        Path pixelApp = newProject("pixels", "pixel_app", """
                  assets:
                    - assets/square.png
                """);
        write(pixelApp, "assets/square.png", png(100, 100));
        writePackageConfig(pixelApp, List.of(new PackageEntry("pixel_app", "../")));
        FlutterAssetInventory pixelInventory = resolver.resolve(
                pixelApp,
                new FlutterAssetLimits(10, 1_000, 1_000, 100, 9_999));
        assertDiagnostic(pixelInventory, "enforce asset pixels", "10000");
    }

    @Test
    void rejectsNonFileAndNonDirectoryPackageRoots() throws Exception {
        Path app = newProject("package-roots", "root_app", "");
        Path ordinaryFile = app.resolve("not-a-package-root");
        Files.writeString(ordinaryFile, "not a directory");
        writePackageConfig(app, List.of(
                new PackageEntry("remote_pack", "https://example.invalid/package/"),
                new PackageEntry("file_pack", ordinaryFile.toUri().toString()),
                new PackageEntry(
                        "private_pack",
                        "file:///C:/Users/Jane Doe/Private Assets/")));

        FlutterAssetInventory inventory = resolver.resolve(app);

        assertTrue(inventory.assets().isEmpty());
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("resolve package root")
                && diagnostic.target().equals("package:remote_pack")
                && diagnostic.reason().contains("non-file scheme 'https'")));
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals("resolve package root")
                && diagnostic.target().equals("package:file_pack")
                && diagnostic.reason().contains("not a filesystem directory")));
        FlutterAssetDiagnostic privateDiagnostic = inventory.diagnostics().stream()
                .filter(diagnostic -> diagnostic.target().equals("package:private_pack"))
                .findFirst()
                .orElseThrow();
        assertFalse(privateDiagnostic.target().contains("Jane"));
        assertFalse(privateDiagnostic.reason().contains("Jane"));
        assertFalse(privateDiagnostic.reason().contains("Private Assets"));
    }

    @Test
    void snapshotsAreDefensiveAndFingerprintIsOrderIndependent() throws Exception {
        Path app = newProject("snapshot", "snapshot_app", """
                  assets:
                    - assets/b.png
                    - assets/a.png
                """);
        byte[] originalA = png(7, 8);
        write(app, "assets/a.png", originalA);
        write(app, "assets/b.png", png(9, 10));
        writePackageConfig(app, List.of(new PackageEntry("snapshot_app", "../")));

        FlutterAssetInventory first = resolver.resolve(app);
        FlutterAssetVariant snapshot = first.find(
                FlutterAssetId.app("assets/a.png"))
                .orElseThrow().variants().getFirst();
        byte[] obtained = snapshot.bytes();
        obtained[0] = 0;
        assertArrayEquals(originalA, snapshot.bytes());

        Files.writeString(app.resolve("pubspec.yaml"), """
                name: snapshot_app
                flutter:
                  assets:
                    - assets/a.png
                    - assets/b.png
                """);
        FlutterAssetInventory reordered = resolver.resolve(app);
        assertEquals(first.fingerprintSha256(), reordered.fingerprintSha256());
        assertEquals(first.assets().stream().map(FlutterAsset::id).toList(),
                reordered.assets().stream().map(FlutterAsset::id).toList());

        write(app, "assets/a.png", png(8, 8));
        assertArrayEquals(originalA, snapshot.bytes());
        FlutterAssetInventory changed = resolver.resolve(app);
        assertNotEquals(first.fingerprintSha256(), changed.fingerprintSha256());
    }

    @Test
    void validatesLimitConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlutterAssetLimits(0, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new FlutterAssetLimits(1, 2, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new FlutterAssetLimits(
                        1, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 1));
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

    private Path packageWithMagicDetectedAsset(
            String directory,
            String packageName) throws IOException {
        Path root = newProject(directory, packageName, """
                  assets:
                    - assets/extensionless
                """);
        write(root, "assets/extensionless", png(5, 6));
        return root;
    }

    private static void writePackageConfig(Path project, List<PackageEntry> packages)
            throws IOException {
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
        Path file = root.resolve(logicalPath.replace('/', root.getFileSystem().getSeparator().charAt(0)));
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }

    private static void assertDiagnostic(
            FlutterAssetInventory inventory,
            String operation,
            String reasonFragment) {
        assertTrue(inventory.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.operation().equals(operation)
                && diagnostic.reason().contains(reasonFragment)),
                inventory.diagnostics().toString());
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

    private static byte[] jpeg(int width, int height) {
        return new byte[]{
            (byte) 0xff, (byte) 0xd8,
            (byte) 0xff, (byte) 0xc0,
            0x00, 0x0b, 0x08,
            (byte) (height >>> 8), (byte) height,
            (byte) (width >>> 8), (byte) width,
            0x01, 0x01, 0x11, 0x00,
            (byte) 0xff, (byte) 0xd9
        };
    }

    private static byte[] gif(int width, int height) {
        byte[] bytes = new byte[10];
        System.arraycopy("GIF89a".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 6);
        putLittleEndian16(bytes, 6, width);
        putLittleEndian16(bytes, 8, height);
        return bytes;
    }

    private static byte[] webp(int width, int height) {
        byte[] bytes = new byte[30];
        putAscii(bytes, 0, "RIFF");
        putLittleEndian32(bytes, 4, bytes.length - 8);
        putAscii(bytes, 8, "WEBP");
        putAscii(bytes, 12, "VP8X");
        putLittleEndian32(bytes, 16, 10);
        putLittleEndian24(bytes, 24, width - 1);
        putLittleEndian24(bytes, 27, height - 1);
        return bytes;
    }

    private static byte[] padded(byte[] source, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, 0, result, 0, Math.min(source.length, length));
        return result;
    }

    private static void putAscii(byte[] bytes, int offset, String value) {
        System.arraycopy(value.getBytes(StandardCharsets.US_ASCII),
                0, bytes, offset, value.length());
    }

    private static void putBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private static void putLittleEndian16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
    }

    private static void putLittleEndian24(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
    }

    private static void putLittleEndian32(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
        bytes[offset + 3] = (byte) (value >>> 24);
    }

    private record PackageEntry(String name, String rootUri) {
    }
}

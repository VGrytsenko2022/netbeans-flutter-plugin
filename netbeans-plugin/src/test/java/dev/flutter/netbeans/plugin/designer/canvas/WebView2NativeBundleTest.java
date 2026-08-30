package dev.flutter.netbeans.plugin.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class WebView2NativeBundleTest {
    private static final String RESOURCE_ROOT =
            "/dev/flutter/netbeans/plugin/designer/canvas/webview2/win-x64/";
    private static final String MANIFEST = RESOURCE_ROOT + "native.manifest";
    private static final String SDK_VERSION = "1.0.4191.47";
    private static final List<String> FILE_NAMES = List.of(
            "Microsoft.Web.WebView2-LICENSE.txt",
            "Microsoft.Web.WebView2-NOTICE.txt",
            "WebView2Loader.dll",
            "nb_flutter_webview2_host.dll");
    private static final Set<String> ADAPTER_EXPORTS = Set.of(
            "nbwv2_create",
            "nbwv2_destroy",
            "nbwv2_get_abi_version",
            "nbwv2_get_runtime_version",
            "nbwv2_post_web_message_json",
            "nbwv2_request_focus",
            "nbwv2_set_bounds",
            "nbwv2_set_visible");
    private static final Set<String> LOADER_EXPORTS = Set.of(
            "CreateCoreWebView2EnvironmentWithOptions",
            "GetAvailableCoreWebView2BrowserVersionString");

    @TempDir
    Path temporaryDirectory;

    @Test
    void exactManifestExtractsAndVerifiesTheExpectedBundleOnce() throws Exception {
        BundleFixture fixture = fixture("win-x64", Map.of());
        Path cacheRoot = temporaryDirectory.resolve("native-cache");
        WebView2NativeBundle bundle = new WebView2NativeBundle(
                fixture.resources(), cacheRoot);

        WebView2NativeBundle.ExtractedBundle first = withPlatform(
                "Windows 11", "amd64", "64", bundle::extract);

        assertTrue(first.root().startsWith(cacheRoot.toAbsolutePath().normalize()));
        assertEquals(
                "win-x64-" + first.identity().substring(0, 24),
                first.root().getFileName().toString());
        assertEquals(SDK_VERSION, first.sdkVersion());
        assertTrue(first.identity().matches("[0-9a-f]{64}"));
        assertEquals(first.root().resolve("WebView2Loader.dll"), first.loader());
        assertEquals(
                first.root().resolve("nb_flutter_webview2_host.dll"),
                first.adapter());
        try (var extracted = Files.list(first.root())) {
            assertEquals(
                    Set.copyOf(FILE_NAMES),
                    Set.copyOf(extracted.map(path -> path.getFileName().toString()).toList()));
        }
        for (String name : FILE_NAMES) {
            assertArrayEquals(
                    fixture.files().get(name),
                    Files.readAllBytes(first.root().resolve(name)),
                    name);
        }
        assertEquals(MANIFEST, fixture.resources().opened().get(0));
        assertEquals(
                FILE_NAMES.stream().map(name -> RESOURCE_ROOT + name)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                Set.copyOf(fixture.resources().opened().subList(1, 5)));

        WebView2NativeBundle.ExtractedBundle second = withPlatform(
                "Windows 11", "x86_64", "64", bundle::extract);

        assertEquals(first, second);
        assertEquals(5, fixture.resources().opened().size(),
                "verified cache reuse must not reopen packaged file resources");
    }

    @Test
    void packagedNativeDllsSatisfyTheStructuralPeContract() throws Exception {
        WebView2NativeBundle.ResourceAccess packaged = resource -> {
            InputStream input = WebView2NativeBundle.class.getResourceAsStream(resource);
            if (input == null) {
                throw new IOException("missing packaged test resource: " + resource);
            }
            return input;
        };
        WebView2NativeBundle bundle = new WebView2NativeBundle(
                packaged, temporaryDirectory.resolve("packaged-cache"));

        WebView2NativeBundle.ExtractedBundle extracted = withPlatform(
                "Windows 11", "amd64", "64", bundle::extract);

        assertEquals(163_680, Files.size(extracted.loader()));
        assertEquals(368_128, Files.size(extracted.adapter()));
    }

    @Test
    void corruptOwnedCacheGenerationIsRemovedAndReextracted() throws Exception {
        BundleFixture fixture = fixture("win-x64", Map.of());
        WebView2NativeBundle bundle = new WebView2NativeBundle(
                fixture.resources(), temporaryDirectory.resolve("cache"));
        WebView2NativeBundle.ExtractedBundle extracted = withPlatform(
                "Windows 11", "amd64", "64", bundle::extract);

        byte[] originalAdapter = fixture.files().get("nb_flutter_webview2_host.dll");
        byte[] tamperedAdapter = originalAdapter.clone();
        tamperedAdapter[0] ^= 0x55;
        Files.write(
                extracted.adapter(),
                tamperedAdapter,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        Files.writeString(extracted.root().resolve("unexpected.dll"), "foreign");

        WebView2NativeBundle.ExtractedBundle recovered = withPlatform(
                "Windows 11", "amd64", "64", bundle::extract);

        assertEquals(extracted, recovered);
        assertFalse(Files.exists(recovered.root().resolve("unexpected.dll")));
        for (String name : FILE_NAMES) {
            assertArrayEquals(
                    fixture.files().get(name),
                    Files.readAllBytes(recovered.root().resolve(name)),
                    name);
        }
        assertEquals(9, fixture.resources().opened().size(),
                "recovery must reopen each packaged file exactly once");
    }

    @Test
    void concurrentBundleInstancesShareOneCacheExtraction() throws Exception {
        BundleFixture firstFixture = fixture("win-x64", Map.of());
        BundleFixture secondFixture = fixture("win-x64", Map.of());
        Path cacheRoot = temporaryDirectory.resolve("shared-cache");
        WebView2NativeBundle firstBundle = new WebView2NativeBundle(
                firstFixture.resources(), cacheRoot);
        WebView2NativeBundle secondBundle = new WebView2NativeBundle(
                secondFixture.resources(), cacheRoot);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<WebView2NativeBundle.ExtractedBundle> extracted;
        try {
            extracted = withPlatform("Windows 11", "amd64", "64", () -> {
                Future<WebView2NativeBundle.ExtractedBundle> first = executor.submit(
                        () -> extractAfterBarrier(firstBundle, ready, start));
                Future<WebView2NativeBundle.ExtractedBundle> second = executor.submit(
                        () -> extractAfterBarrier(secondBundle, ready, start));
                assertTrue(ready.await(5, TimeUnit.SECONDS),
                        "both extraction calls must reach the start barrier");
                start.countDown();
                return List.of(
                        first.get(10, TimeUnit.SECONDS),
                        second.get(10, TimeUnit.SECONDS));
            });
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "concurrent extraction workers did not terminate");
        }

        assertEquals(extracted.get(0), extracted.get(1));
        int firstOpens = firstFixture.resources().opened().size();
        int secondOpens = secondFixture.resources().opened().size();
        assertEquals(6, firstOpens + secondOpens,
                "two manifests and one four-file extraction are expected");
        assertEquals(
                List.of(1, 5),
                java.util.stream.IntStream.of(firstOpens, secondOpens)
                        .sorted().boxed().toList(),
                "only one instance may open the packaged native files");
        try (var cacheEntries = Files.list(cacheRoot)) {
            assertEquals(
                    Set.of(".webview2-native.lock", extracted.get(0).root()
                            .getFileName().toString()),
                    Set.copyOf(cacheEntries
                            .map(path -> path.getFileName().toString()).toList()));
        }
    }

    @Test
    void packagedBytesMustMatchEveryManifestDigest() throws Exception {
        BundleFixture baseline = fixture("win-x64", Map.of());
        String target = "WebView2Loader.dll";
        byte[] wrongDigestBytes = baseline.files().get(target).clone();
        wrongDigestBytes[0] ^= 0x22;
        BundleFixture fixture = fixture(
                "win-x64", Map.of(target, wrongDigestBytes));
        WebView2NativeBundle bundle = new WebView2NativeBundle(
                fixture.resources(), temporaryDirectory.resolve("cache"));

        IOException failure = withPlatform(
                "Windows 11", "amd64", "64",
                () -> assertThrows(IOException.class, bundle::extract));

        assertTrue(failure.getMessage().contains(
                "digest mismatch: WebView2Loader.dll"));
        try (var cacheEntries = Files.list(temporaryDirectory.resolve("cache"))) {
            assertEquals(
                    Set.of(".webview2-native.lock"),
                    Set.copyOf(cacheEntries
                            .map(path -> path.getFileName().toString()).toList()),
                    "failed staging extraction must leave only the cache lock");
        }
    }

    @Test
    void malformedAndWrongArchitecturePeImagesFailClosed() throws Exception {
        byte[] adapter = minimalAdapterPe();
        IOException truncated = structuralFailure(
                "truncated",
                "nb_flutter_webview2_host.dll",
                Arrays.copyOf(adapter, 100));
        assertTrue(truncated.getMessage().contains("truncated"));

        byte[] wrongMachine = adapter.clone();
        int coff = Math.toIntExact(readU32(wrongMachine, 0x3c)) + 4;
        writeU16(wrongMachine, coff, 0x014c);
        IOException machine = structuralFailure(
                "wrong-machine",
                "nb_flutter_webview2_host.dll",
                wrongMachine);
        assertTrue(machine.getMessage().contains("not an x64 machine"));

        byte[] pe32 = adapter.clone();
        writeU16(pe32, coff + 20, 0x10b);
        IOException optionalFormat = structuralFailure(
                "pe32-not-plus",
                "nb_flutter_webview2_host.dll",
                pe32);
        assertTrue(optionalFormat.getMessage().contains("not PE32+"));

        byte[] notDll = adapter.clone();
        writeU16(notDll, coff + 18, readU16(notDll, coff + 18) & ~0x2000);
        IOException characteristic = structuralFailure(
                "not-dll",
                "nb_flutter_webview2_host.dll",
                notDll);
        assertTrue(characteristic.getMessage().contains("DLL characteristic"));
    }

    @Test
    void missingRequiredExportsAndWebView2LoaderImportFailClosed() throws Exception {
        byte[] missingAdapterExport = replaceAscii(
                minimalAdapterPe(), "nbwv2_destroy", "nbwv2_destrox");
        IOException adapterExport = structuralFailure(
                "missing-adapter-export",
                "nb_flutter_webview2_host.dll",
                missingAdapterExport);
        assertTrue(adapterExport.getMessage().contains("missing named exports"));
        assertTrue(adapterExport.getMessage().contains("nbwv2_destroy"));

        byte[] missingLoaderExport = replaceAscii(
                minimalLoaderPe(),
                "CreateCoreWebView2EnvironmentWithOptions",
                "CreateCoreWebView2EnvironmentWithOptionx");
        IOException loaderExport = structuralFailure(
                "missing-loader-export",
                "WebView2Loader.dll",
                missingLoaderExport);
        assertTrue(loaderExport.getMessage().contains("missing named exports"));
        assertTrue(loaderExport.getMessage().contains(
                "CreateCoreWebView2EnvironmentWithOptions"));

        byte[] missingLoaderImport = replaceAscii(
                minimalAdapterPe(), "WebView2Loader.dll", "XebView2Loader.dll");
        IOException loaderImport = structuralFailure(
                "missing-loader-import",
                "nb_flutter_webview2_host.dll",
                missingLoaderImport);
        assertTrue(loaderImport.getMessage().contains("missing import library"));
        assertTrue(loaderImport.getMessage().contains("WebView2Loader.dll"));
    }

    @Test
    void overlappingSectionsAndOutOfFileRvasFailClosed() throws Exception {
        byte[] duplicateExport = minimalAdapterPe();
        int duplicateCoff = Math.toIntExact(readU32(duplicateExport, 0x3c)) + 4;
        int duplicateOptional = duplicateCoff + 20;
        int exportDirectory = fixtureRvaOffset(
                duplicateExport,
                readU32(duplicateExport, duplicateOptional + 112));
        int exportNames = fixtureRvaOffset(
                duplicateExport,
                readU32(duplicateExport, exportDirectory + 32));
        writeU32(
                duplicateExport,
                exportNames + 4,
                readU32(duplicateExport, exportNames));
        IOException duplicate = structuralFailure(
                "duplicate-export",
                "nb_flutter_webview2_host.dll",
                duplicateExport);
        assertTrue(duplicate.getMessage().contains("duplicate named export"));

        byte[] overlapping = minimalAdapterPe();
        int coff = Math.toIntExact(readU32(overlapping, 0x3c)) + 4;
        int optional = coff + 20;
        int section = optional + readU16(overlapping, coff + 16);
        writeU16(overlapping, coff + 2, 2);
        System.arraycopy(overlapping, section, overlapping, section + 40, 40);
        writeFixedAscii(overlapping, section + 40, 8, ".copy");
        IOException overlap = structuralFailure(
                "overlapping-sections",
                "nb_flutter_webview2_host.dll",
                overlapping);
        assertTrue(overlap.getMessage().contains("overlapping PE sections"));

        byte[] outOfFile = minimalLoaderPe();
        int loaderCoff = Math.toIntExact(readU32(outOfFile, 0x3c)) + 4;
        int loaderOptional = loaderCoff + 20;
        writeU32(outOfFile, loaderOptional + 112, 0x1f00);
        IOException rva = structuralFailure(
                "out-of-file-rva",
                "WebView2Loader.dll",
                outOfFile);
        assertTrue(rva.getMessage().contains("out-of-file RVA mapping"));
    }

    @Test
    void manifestAndCurrentJvmArchitectureMustBothBeWindowsX64() throws Exception {
        BundleFixture wrongManifest = fixture("win-arm64", Map.of());
        IOException manifestFailure = assertThrows(
                IOException.class,
                () -> new WebView2NativeBundle(
                        wrongManifest.resources(),
                        temporaryDirectory.resolve("wrong-manifest")));
        assertTrue(manifestFailure.getMessage().contains(
                "incomplete WebView2 native manifest"));
        assertEquals(List.of(MANIFEST), wrongManifest.resources().opened());

        BundleFixture x64Manifest = fixture("win-x64", Map.of());
        WebView2NativeBundle x64Bundle = new WebView2NativeBundle(
                x64Manifest.resources(), temporaryDirectory.resolve("wrong-jvm"));
        IOException platformFailure = withPlatform(
                "Windows 11", "x86", "32",
                () -> assertThrows(IOException.class, x64Bundle::extract));

        assertTrue(platformFailure.getMessage().contains("requires a Windows x64 JVM"));
        assertEquals(List.of(MANIFEST), x64Manifest.resources().opened(),
                "wrong architecture must fail before packaged DLL extraction");
        assertFalse(Files.exists(temporaryDirectory.resolve("wrong-jvm")));
    }

    private static BundleFixture fixture(
            String manifestArchitecture,
            Map<String, byte[]> resourceOverrides) {
        return buildFixture(
                manifestArchitecture,
                defaultManifestFiles(),
                resourceOverrides);
    }

    private static BundleFixture fixtureWithPackagedFiles(
            String manifestArchitecture,
            Map<String, byte[]> replacements) {
        LinkedHashMap<String, byte[]> manifestFiles = defaultManifestFiles();
        replacements.forEach((name, bytes) -> {
            if (!manifestFiles.containsKey(name)) {
                throw new IllegalArgumentException("unknown fixture file: " + name);
            }
            manifestFiles.put(name, bytes.clone());
        });
        return buildFixture(manifestArchitecture, manifestFiles, Map.of());
    }

    private static LinkedHashMap<String, byte[]> defaultManifestFiles() {
        LinkedHashMap<String, byte[]> manifestFiles = new LinkedHashMap<>();
        manifestFiles.put(
                "Microsoft.Web.WebView2-LICENSE.txt",
                "license\n".getBytes(StandardCharsets.UTF_8));
        manifestFiles.put(
                "Microsoft.Web.WebView2-NOTICE.txt",
                "notice\n".getBytes(StandardCharsets.UTF_8));
        manifestFiles.put(
                "WebView2Loader.dll",
                minimalLoaderPe());
        manifestFiles.put(
                "nb_flutter_webview2_host.dll",
                minimalAdapterPe());
        return manifestFiles;
    }

    private static BundleFixture buildFixture(
            String manifestArchitecture,
            LinkedHashMap<String, byte[]> manifestFiles,
            Map<String, byte[]> resourceOverrides) {
        LinkedHashMap<String, byte[]> resources = new LinkedHashMap<>();
        StringBuilder manifest = new StringBuilder()
                .append("# synthetic exact WebView2 native bundle\n")
                .append("format=NETBEANS_FLUTTER_WEBVIEW2_NATIVE|1\n")
                .append("architecture=").append(manifestArchitecture).append('\n')
                .append("sdkVersion=").append(SDK_VERSION).append('\n')
                .append("sdkPackageSha256=")
                .append(sha256("sdk-package".getBytes(StandardCharsets.UTF_8)))
                .append('\n');
        for (Map.Entry<String, byte[]> entry : manifestFiles.entrySet()) {
            manifest.append("file=")
                    .append(entry.getKey())
                    .append('|')
                    .append(entry.getValue().length)
                    .append('|')
                    .append(sha256(entry.getValue()))
                    .append('\n');
        }
        resources.put(MANIFEST, manifest.toString().getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, byte[]> entry : manifestFiles.entrySet()) {
            byte[] source = resourceOverrides.getOrDefault(
                    entry.getKey(), entry.getValue());
            resources.put(RESOURCE_ROOT + entry.getKey(), source.clone());
        }
        return new BundleFixture(
                Map.copyOf(manifestFiles), new RecordingResources(resources));
    }

    private IOException structuralFailure(
            String cacheName,
            String fileName,
            byte[] packagedBytes) throws Exception {
        BundleFixture fixture = fixtureWithPackagedFiles(
                "win-x64", Map.of(fileName, packagedBytes));
        WebView2NativeBundle bundle = new WebView2NativeBundle(
                fixture.resources(), temporaryDirectory.resolve(cacheName));
        return withPlatform(
                "Windows 11", "amd64", "64",
                () -> assertThrows(IOException.class, bundle::extract));
    }

    private static byte[] minimalAdapterPe() {
        return minimalPe(
                "nb_flutter_webview2_host.dll",
                ADAPTER_EXPORTS,
                "WebView2Loader.dll",
                LOADER_EXPORTS);
    }

    private static byte[] minimalLoaderPe() {
        return minimalPe(
                "WebView2Loader.dll",
                LOADER_EXPORTS,
                "KERNEL32.dll",
                Set.of("LoadLibraryW"));
    }

    private static byte[] minimalPe(
            String dllName,
            Set<String> exports,
            String importLibrary,
            Set<String> imports) {
        final int peOffset = 0x80;
        final int optionalSize = 240;
        final int optional = peOffset + 24;
        final int sectionTable = optional + optionalSize;
        final int rawOffset = 0x200;
        final int rawSize = 0xc00;
        final int sectionRva = 0x1000;
        byte[] image = new byte[rawOffset + rawSize];
        writeU16(image, 0, 0x5a4d);
        writeU32(image, 0x3c, peOffset);
        writeU32(image, peOffset, 0x0000_4550L);
        int coff = peOffset + 4;
        writeU16(image, coff, 0x8664);
        writeU16(image, coff + 2, 1);
        writeU16(image, coff + 16, optionalSize);
        writeU16(image, coff + 18, 0x2022);
        writeU16(image, optional, 0x20b);
        writeU32(image, optional + 4, rawSize);
        writeU32(image, optional + 16, sectionRva);
        writeU32(image, optional + 20, sectionRva);
        writeU64(image, optional + 24, 0x0000_0001_8000_0000L);
        writeU32(image, optional + 32, 0x1000);
        writeU32(image, optional + 36, 0x200);
        writeU16(image, optional + 40, 6);
        writeU16(image, optional + 48, 6);
        writeU16(image, optional + 52, 6);
        writeU32(image, optional + 56, 0x2000);
        writeU32(image, optional + 60, rawOffset);
        writeU16(image, optional + 68, 3);
        writeU16(image, optional + 70, 0x8160);
        writeU64(image, optional + 72, 0x100000);
        writeU64(image, optional + 80, 0x1000);
        writeU64(image, optional + 88, 0x100000);
        writeU64(image, optional + 96, 0x1000);
        writeU32(image, optional + 108, 16);
        writeFixedAscii(image, sectionTable, 8, ".text");
        writeU32(image, sectionTable + 8, rawSize);
        writeU32(image, sectionTable + 12, sectionRva);
        writeU32(image, sectionTable + 16, rawSize);
        writeU32(image, sectionTable + 20, rawOffset);
        writeU32(image, sectionTable + 36, 0x6000_0020L);

        PeFixtureWriter writer = new PeFixtureWriter(
                image, rawOffset, rawOffset + rawSize, sectionRva);
        List<String> sortedExports = exports.stream().sorted().toList();
        int exportDirectory = writer.allocate(40, 4);
        int functionTable = writer.allocate(sortedExports.size() * 4, 4);
        int nameTable = writer.allocate(sortedExports.size() * 4, 4);
        int ordinalTable = writer.allocate(sortedExports.size() * 2, 2);
        int exportDllName = writer.ascii(dllName);
        List<Integer> exportNames = new ArrayList<>();
        for (String symbol : sortedExports) {
            exportNames.add(writer.ascii(symbol));
        }
        int exportEnd = writer.position();
        int function = writer.allocate(1, 1);
        image[function] = (byte) 0xc3;
        writeU32(image, exportDirectory + 12, writer.rva(exportDllName));
        writeU32(image, exportDirectory + 16, 1);
        writeU32(image, exportDirectory + 20, sortedExports.size());
        writeU32(image, exportDirectory + 24, sortedExports.size());
        writeU32(image, exportDirectory + 28, writer.rva(functionTable));
        writeU32(image, exportDirectory + 32, writer.rva(nameTable));
        writeU32(image, exportDirectory + 36, writer.rva(ordinalTable));
        for (int index = 0; index < sortedExports.size(); index++) {
            writeU32(image, functionTable + index * 4, writer.rva(function));
            writeU32(image, nameTable + index * 4, writer.rva(exportNames.get(index)));
            writeU16(image, ordinalTable + index * 2, index);
        }

        List<String> sortedImports = imports.stream().sorted().toList();
        int importDirectory = writer.allocate(40, 4);
        int importLookup = writer.allocate((sortedImports.size() + 1) * 8, 8);
        int importAddress = writer.allocate((sortedImports.size() + 1) * 8, 8);
        int importDllName = writer.ascii(importLibrary);
        List<Integer> importNames = new ArrayList<>();
        for (String symbol : sortedImports) {
            int hintAndName = writer.allocate(2, 2);
            writeU16(image, hintAndName, 0);
            writer.asciiAtCurrent(symbol);
            importNames.add(hintAndName);
        }
        writeU32(image, importDirectory, writer.rva(importLookup));
        writeU32(image, importDirectory + 12, writer.rva(importDllName));
        writeU32(image, importDirectory + 16, writer.rva(importAddress));
        for (int index = 0; index < sortedImports.size(); index++) {
            long nameRva = writer.rva(importNames.get(index));
            writeU64(image, importLookup + index * 8, nameRva);
            writeU64(image, importAddress + index * 8, nameRva);
        }

        writeU32(image, optional + 112, writer.rva(exportDirectory));
        writeU32(image, optional + 116, exportEnd - exportDirectory);
        writeU32(image, optional + 120, writer.rva(importDirectory));
        writeU32(image, optional + 124, 40);
        return image;
    }

    private static byte[] replaceAscii(byte[] source, String target, String replacement) {
        if (target.length() != replacement.length()) {
            throw new IllegalArgumentException("replacement must preserve byte length");
        }
        byte[] result = source.clone();
        byte[] needle = target.getBytes(StandardCharsets.US_ASCII);
        byte[] value = replacement.getBytes(StandardCharsets.US_ASCII);
        int found = -1;
        for (int offset = 0; offset <= result.length - needle.length; offset++) {
            if (Arrays.equals(
                    result,
                    offset,
                    offset + needle.length,
                    needle,
                    0,
                    needle.length)) {
                if (found >= 0) {
                    throw new IllegalArgumentException("fixture text is not unique: " + target);
                }
                found = offset;
            }
        }
        if (found < 0) {
            throw new IllegalArgumentException("fixture text is absent: " + target);
        }
        System.arraycopy(value, 0, result, found, value.length);
        return result;
    }

    private static int readU16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private static long readU32(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xff)
                | (((long) bytes[offset + 1] & 0xff) << 8)
                | (((long) bytes[offset + 2] & 0xff) << 16)
                | (((long) bytes[offset + 3] & 0xff) << 24);
    }

    private static int fixtureRvaOffset(byte[] image, long rva) {
        int coff = Math.toIntExact(readU32(image, 0x3c)) + 4;
        int optional = coff + 20;
        int section = optional + readU16(image, coff + 16);
        long sectionRva = readU32(image, section + 12);
        long rawOffset = readU32(image, section + 20);
        return Math.toIntExact(rawOffset + rva - sectionRva);
    }

    private static void writeU16(byte[] bytes, int offset, long value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
    }

    private static void writeU32(byte[] bytes, int offset, long value) {
        for (int index = 0; index < 4; index++) {
            bytes[offset + index] = (byte) (value >>> (index * 8));
        }
    }

    private static void writeU64(byte[] bytes, int offset, long value) {
        for (int index = 0; index < 8; index++) {
            bytes[offset + index] = (byte) (value >>> (index * 8));
        }
    }

    private static void writeFixedAscii(
            byte[] bytes,
            int offset,
            int length,
            String value) {
        byte[] encoded = value.getBytes(StandardCharsets.US_ASCII);
        if (encoded.length > length) {
            throw new IllegalArgumentException("fixed ASCII value is too long");
        }
        Arrays.fill(bytes, offset, offset + length, (byte) 0);
        System.arraycopy(encoded, 0, bytes, offset, encoded.length);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("test JDK has no SHA-256", exception);
        }
    }

    private static WebView2NativeBundle.ExtractedBundle extractAfterBarrier(
            WebView2NativeBundle bundle,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IOException("concurrent extraction start barrier timed out");
        }
        return bundle.extract();
    }

    private static <T> T withPlatform(
            String operatingSystem,
            String architecture,
            String dataModel,
            ThrowingSupplier<T> action) throws Exception {
        String oldOperatingSystem = System.getProperty("os.name");
        String oldArchitecture = System.getProperty("os.arch");
        String oldDataModel = System.getProperty("sun.arch.data.model");
        try {
            System.setProperty("os.name", operatingSystem);
            System.setProperty("os.arch", architecture);
            System.setProperty("sun.arch.data.model", dataModel);
            return action.get();
        } finally {
            restoreProperty("os.name", oldOperatingSystem);
            restoreProperty("os.arch", oldArchitecture);
            restoreProperty("sun.arch.data.model", oldDataModel);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private record BundleFixture(
            Map<String, byte[]> files,
            RecordingResources resources) {}

    private static final class RecordingResources
            implements WebView2NativeBundle.ResourceAccess {
        private final Map<String, byte[]> resources;
        private final List<String> opened = new ArrayList<>();

        private RecordingResources(Map<String, byte[]> resources) {
            this.resources = Map.copyOf(resources);
        }

        @Override
        public synchronized InputStream open(String resource) throws IOException {
            opened.add(resource);
            byte[] bytes = resources.get(resource);
            if (bytes == null) {
                throw new IOException("missing synthetic resource: " + resource);
            }
            return new ByteArrayInputStream(bytes.clone());
        }

        private synchronized List<String> opened() {
            return List.copyOf(opened);
        }
    }

    private static final class PeFixtureWriter {
        private final byte[] image;
        private final int start;
        private final int end;
        private final int sectionRva;
        private int position;

        private PeFixtureWriter(byte[] image, int start, int end, int sectionRva) {
            this.image = image;
            this.start = start;
            this.end = end;
            this.sectionRva = sectionRva;
            position = start;
        }

        private int allocate(int size, int alignment) {
            int aligned = (position + alignment - 1) & -alignment;
            if (size < 0 || aligned < start || aligned > end - size) {
                throw new IllegalArgumentException("minimal PE fixture overflow");
            }
            position = aligned + size;
            return aligned;
        }

        private int ascii(String value) {
            byte[] encoded = value.getBytes(StandardCharsets.US_ASCII);
            int offset = allocate(encoded.length + 1, 1);
            System.arraycopy(encoded, 0, image, offset, encoded.length);
            return offset;
        }

        private void asciiAtCurrent(String value) {
            byte[] encoded = value.getBytes(StandardCharsets.US_ASCII);
            int offset = allocate(encoded.length + 1, 1);
            System.arraycopy(encoded, 0, image, offset, encoded.length);
        }

        private int rva(int fileOffset) {
            if (fileOffset < start || fileOffset >= end) {
                throw new IllegalArgumentException("fixture file offset escapes its section");
            }
            return sectionRva + fileOffset - start;
        }

        private int position() {
            return position;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

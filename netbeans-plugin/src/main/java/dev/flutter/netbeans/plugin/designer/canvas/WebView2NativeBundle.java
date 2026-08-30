package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.openide.modules.Places;

/** Integrity-checked lazy extractor for the architecture-specific native host. */
final class WebView2NativeBundle {
    private static final String ARCHITECTURE = "win-x64";
    private static final String RESOURCE_ROOT =
            "/dev/flutter/netbeans/plugin/designer/canvas/webview2/" + ARCHITECTURE + "/";
    private static final String MANIFEST = RESOURCE_ROOT + "native.manifest";
    private static final String FORMAT = "NETBEANS_FLUTTER_WEBVIEW2_NATIVE|1";
    private static final String CACHE_LOCK = ".webview2-native.lock";
    private static final int MAXIMUM_FILES = 8;
    private static final long MAXIMUM_FILE_BYTES = 4L * 1024 * 1024;
    private static final Object JVM_EXTRACTION_GUARD = new Object();
    private static final Set<String> ADAPTER_EXPORTS = Set.of(
            "nbwv2_create",
            "nbwv2_destroy",
            "nbwv2_get_abi_version",
            "nbwv2_get_runtime_version",
            "nbwv2_post_web_message_json",
            "nbwv2_request_focus",
            "nbwv2_set_bounds",
            "nbwv2_set_visible");
    private static final Set<String> WEBVIEW2_LOADER_EXPORTS = Set.of(
            "CreateCoreWebView2EnvironmentWithOptions",
            "GetAvailableCoreWebView2BrowserVersionString");

    private final ResourceAccess resources;
    private final Path cacheRoot;
    private final Manifest manifest;

    static WebView2NativeBundle packaged() throws IOException {
        return new WebView2NativeBundle(
                new ClasspathResources(),
                Places.getCacheSubdirectory("flutter-webview2-native").toPath());
    }

    WebView2NativeBundle(ResourceAccess resources, Path cacheRoot) throws IOException {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot")
                .toAbsolutePath().normalize();
        manifest = readManifest(resources);
    }

    ExtractedBundle extract() throws IOException {
        requireWindowsX64();
        synchronized (JVM_EXTRACTION_GUARD) {
            return extractUnderJvmGuard();
        }
    }

    private ExtractedBundle extractUnderJvmGuard() throws IOException {
        Path root = cacheRoot.resolve(ARCHITECTURE + "-" + manifest.identity().substring(0, 24));
        requireInside(cacheRoot, root);
        prepareCacheRoot();
        Path lockPath = cacheRoot.resolve(CACHE_LOCK).normalize();
        requireInside(cacheRoot, lockPath);
        rejectLink(lockPath, "WebView2 native cache lock");
        try (FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            verifyLockPath(lockPath);
            try (FileLock ignored = channel.lock()) {
                prepareCacheRoot();
                verifyLockPath(lockPath);
                return extractLocked(root);
            }
        }
    }

    private ExtractedBundle extractLocked(Path root) throws IOException {
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            try {
                verifyExtracted(root);
                return result(root);
            } catch (IOException verificationFailure) {
                try {
                    deleteOwnedFlatDirectory(root);
                } catch (IOException cleanupFailure) {
                    cleanupFailure.addSuppressed(verificationFailure);
                    throw new IOException(
                            "corrupt WebView2 native cache generation could not be removed",
                            cleanupFailure);
                }
            }
        }
        Path staging = Files.createTempDirectory(cacheRoot, ".webview2-native-");
        requireInside(cacheRoot, staging);
        try {
            for (Entry entry : manifest.files().values()) {
                Path target = staging.resolve(entry.name()).normalize();
                requireInside(staging, target);
                try (InputStream input = resources.open(RESOURCE_ROOT + entry.name())) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                verifyFile(target, entry);
            }
            try {
                Files.move(staging, root, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(staging, root);
            }
        } catch (IOException | RuntimeException failure) {
            deleteOwnedFlatDirectory(staging);
            throw failure;
        }
        verifyExtracted(root);
        return result(root);
    }

    private void prepareCacheRoot() throws IOException {
        Files.createDirectories(cacheRoot);
        rejectLink(cacheRoot, "WebView2 native cache root");
        if (!Files.isDirectory(cacheRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("WebView2 native cache root is not a directory");
        }
    }

    private static void verifyLockPath(Path lockPath) throws IOException {
        rejectLink(lockPath, "WebView2 native cache lock");
        if (!Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("WebView2 native cache lock is not a regular file");
        }
    }

    private ExtractedBundle result(Path root) {
        return new ExtractedBundle(
                root,
                root.resolve("WebView2Loader.dll"),
                root.resolve("nb_flutter_webview2_host.dll"),
                manifest.sdkVersion(),
                manifest.identity());
    }

    private void verifyExtracted(Path root) throws IOException {
        rejectLink(root, "WebView2 native bundle");
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("WebView2 native bundle is not a directory");
        }
        try (var entries = Files.list(root)) {
            var names = entries.map(path -> path.getFileName().toString()).sorted().toList();
            if (!names.equals(manifest.files().keySet().stream().sorted().toList())) {
                throw new IOException("WebView2 native bundle contains an unexpected file set");
            }
        }
        for (Entry entry : manifest.files().values()) {
            verifyFile(root.resolve(entry.name()), entry);
        }
    }

    private static void verifyFile(Path path, Entry expected) throws IOException {
        rejectLink(path, "WebView2 native bundle file");
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) != expected.size()) {
            throw new IOException("WebView2 native bundle size mismatch: " + expected.name());
        }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(Math.toIntExact(expected.size() + 1));
        }
        if (bytes.length != expected.size()) {
            throw new IOException("WebView2 native bundle changed while reading: "
                    + expected.name());
        }
        String actual = sha256(bytes);
        if (!expected.sha256().equals(actual)) {
            throw new IOException("WebView2 native bundle digest mismatch: " + expected.name());
        }
        if (expected.name().endsWith(".dll")) {
            validatePortableExecutable(bytes, expected.name());
        }
    }

    private static void validatePortableExecutable(byte[] bytes, String name)
            throws IOException {
        PeImage image = PeImage.read(bytes, name);
        if (name.equals("nb_flutter_webview2_host.dll")) {
            image.requireExports(ADAPTER_EXPORTS);
            image.requireImports(
                    "WebView2Loader.dll",
                    WEBVIEW2_LOADER_EXPORTS);
        } else if (name.equals("WebView2Loader.dll")) {
            image.requireExports(WEBVIEW2_LOADER_EXPORTS);
        } else {
            throw new IOException("unexpected native DLL in manifest: " + name);
        }
    }

    private static Manifest readManifest(ResourceAccess resources) throws IOException {
        Map<String, Entry> files = new LinkedHashMap<>();
        String format = null;
        String architecture = null;
        String sdkVersion = null;
        String packageSha = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resources.open(MANIFEST), java.nio.charset.StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int equals = line.indexOf('=');
                if (equals <= 0) {
                    throw new IOException("invalid WebView2 native manifest line");
                }
                String key = line.substring(0, equals);
                String value = line.substring(equals + 1);
                switch (key) {
                    case "format" -> format = unique(format, value, key);
                    case "architecture" -> architecture = unique(architecture, value, key);
                    case "sdkVersion" -> sdkVersion = unique(sdkVersion, value, key);
                    case "sdkPackageSha256" -> packageSha = unique(packageSha, value, key);
                    case "file" -> {
                        String[] fields = value.split("\\|", -1);
                        if (fields.length != 3 || !fields[0].matches("[A-Za-z0-9._-]+")) {
                            throw new IOException("invalid WebView2 native manifest file entry");
                        }
                        long size;
                        try {
                            size = Long.parseLong(fields[1]);
                        } catch (NumberFormatException failure) {
                            throw new IOException("invalid WebView2 native file size", failure);
                        }
                        if (size <= 0 || size > MAXIMUM_FILE_BYTES
                                || !fields[2].matches("[0-9a-f]{64}")) {
                            throw new IOException("invalid WebView2 native file metadata");
                        }
                        Entry previous = files.put(fields[0],
                                new Entry(fields[0], size, fields[2]));
                        if (previous != null || files.size() > MAXIMUM_FILES) {
                            throw new IOException("duplicate or excessive WebView2 native files");
                        }
                    }
                    default -> throw new IOException(
                            "unknown WebView2 native manifest key: " + key);
                }
            }
        }
        if (!FORMAT.equals(format) || !ARCHITECTURE.equals(architecture)
                || sdkVersion == null || !sdkVersion.matches("[0-9.]{3,32}")
                || packageSha == null || !packageSha.matches("[0-9a-f]{64}")
                || !files.keySet().equals(java.util.Set.of(
                        "Microsoft.Web.WebView2-LICENSE.txt",
                        "Microsoft.Web.WebView2-NOTICE.txt",
                        "WebView2Loader.dll",
                        "nb_flutter_webview2_host.dll"))) {
            throw new IOException("incomplete WebView2 native manifest");
        }
        String identity = sha256((format + "\n" + architecture + "\n" + sdkVersion + "\n"
                + packageSha + "\n" + files).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new Manifest(sdkVersion, packageSha, Map.copyOf(files), identity);
    }

    private static String unique(String current, String value, String key) throws IOException {
        if (current != null || value.isBlank()) {
            throw new IOException("duplicate or empty WebView2 native manifest key: " + key);
        }
        return value;
    }

    private static void requireWindowsX64() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String bits = System.getProperty("sun.arch.data.model", "");
        if (!os.startsWith("windows")) {
            throw new IOException("WebView2 native host is available only on Windows");
        }
        if (!(architecture.equals("amd64") || architecture.equals("x86_64"))
                || !bits.equals("64")) {
            throw new IOException("WebView2 native host currently requires a Windows x64 JVM");
        }
    }

    private static void rejectLink(Path path, String label) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException(label + " must not be a symbolic link or reparse point");
        }
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        var noFollow = Files.readAttributes(
                path, java.nio.file.attribute.BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (noFollow.isSymbolicLink() || noFollow.isOther()) {
            throw new IOException(label + " must not be a symbolic link or reparse point");
        }
        try {
            Object attributes = Files.getAttribute(
                    path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            if (attributes instanceof Number value && (value.intValue() & 0x400) != 0) {
                throw new IOException(label + " must not be a Windows reparse point");
            }
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            // Non-Windows providers do not expose the raw DOS reparse bit.
        }
    }

    private static void requireInside(Path root, Path candidate) throws IOException {
        if (!candidate.toAbsolutePath().normalize().startsWith(
                root.toAbsolutePath().normalize())) {
            throw new IOException("WebView2 native path escapes its cache root");
        }
    }

    private static void deleteOwnedFlatDirectory(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        rejectLink(directory, "WebView2 owned directory");
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("WebView2 owned path is not a directory");
        }
        java.util.List<Path> ownedFiles;
        try (var entries = Files.list(directory)) {
            ownedFiles = entries.toList();
        }
        for (Path entry : ownedFiles) {
            rejectLink(entry, "WebView2 owned file");
            if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "WebView2 owned directory contains a non-flat entry");
            }
        }
        for (Path entry : ownedFiles) {
            Files.deleteIfExists(entry);
        }
        Files.deleteIfExists(directory);
    }

    private record PeImage(
            String name,
            Set<String> exports,
            Map<String, Set<String>> imports) {
        private static final int PE32_PLUS_MAGIC = 0x20b;
        private static final int AMD64_MACHINE = 0x8664;
        private static final int DLL_CHARACTERISTIC = 0x2000;
        private static final int OPTIONAL_FIXED_BYTES = 112;
        private static final int MAXIMUM_SECTIONS = 96;
        private static final int MAXIMUM_SYMBOLS = 4_096;
        private static final int MAXIMUM_ASCII_BYTES = 512;
        private static final long MAXIMUM_IMAGE_BYTES = 64L * 1024 * 1024;
        private static final long MAXIMUM_HEADER_BYTES = 64L * 1024;

        private static PeImage read(byte[] bytes, String name) throws IOException {
            PeReader reader = new PeReader(bytes, name);
            reader.requireBytes(0, 64, "DOS header");
            if (reader.u16(0) != 0x5a4d) {
                throw reader.invalid("missing MZ signature");
            }
            long peOffsetValue = reader.u32(0x3c);
            if (peOffsetValue < 64 || peOffsetValue > Integer.MAX_VALUE) {
                throw reader.invalid("invalid PE header offset");
            }
            int peOffset = (int) peOffsetValue;
            reader.requireBytes(peOffset, 24, "PE and COFF headers");
            if (reader.u32(peOffset) != 0x0000_4550L) {
                throw reader.invalid("missing PE signature");
            }
            int coff = peOffset + 4;
            if (reader.u16(coff) != AMD64_MACHINE) {
                throw reader.invalid("image is not an x64 machine PE");
            }
            int sectionCount = reader.u16(coff + 2);
            if (sectionCount <= 0 || sectionCount > MAXIMUM_SECTIONS) {
                throw reader.invalid("invalid section count");
            }
            int optionalSize = reader.u16(coff + 16);
            int characteristics = reader.u16(coff + 18);
            if ((characteristics & DLL_CHARACTERISTIC) == 0) {
                throw reader.invalid("image is missing the DLL characteristic");
            }
            if (optionalSize < OPTIONAL_FIXED_BYTES + 16 || optionalSize > 4_096) {
                throw reader.invalid("invalid PE32+ optional-header size");
            }
            int optional = coff + 20;
            reader.requireBytes(optional, optionalSize, "optional header");
            if (reader.u16(optional) != PE32_PLUS_MAGIC) {
                throw reader.invalid("image is not PE32+");
            }
            long sectionAlignment = reader.u32(optional + 32);
            long fileAlignment = reader.u32(optional + 36);
            if (!powerOfTwo(fileAlignment)
                    || fileAlignment < 512 || fileAlignment > 65_536
                    || !powerOfTwo(sectionAlignment)
                    || sectionAlignment < fileAlignment) {
                throw reader.invalid("invalid section or file alignment");
            }
            long sizeOfImage = reader.u32(optional + 56);
            long sizeOfHeaders = reader.u32(optional + 60);
            if (sizeOfHeaders <= 0 || sizeOfHeaders > bytes.length
                    || sizeOfHeaders > MAXIMUM_HEADER_BYTES
                    || sizeOfHeaders % fileAlignment != 0
                    || sizeOfImage <= 0 || sizeOfImage > MAXIMUM_IMAGE_BYTES
                    || sizeOfImage % sectionAlignment != 0) {
                throw reader.invalid("invalid image or header size");
            }
            long directoryCount = reader.u32(optional + 108);
            if (directoryCount < 2 || directoryCount > 16
                    || OPTIONAL_FIXED_BYTES + directoryCount * 8 > optionalSize) {
                throw reader.invalid("invalid data-directory count");
            }
            int sectionTable = Math.addExact(optional, optionalSize);
            long sectionTableBytes = (long) sectionCount * 40;
            reader.requireBytes(sectionTable, sectionTableBytes, "section table");
            if ((long) sectionTable + sectionTableBytes > sizeOfHeaders) {
                throw reader.invalid("section table escapes the PE headers");
            }
            List<PeSection> sections = readSections(
                    reader,
                    sectionTable,
                    sectionCount,
                    sectionAlignment,
                    fileAlignment,
                    sizeOfHeaders,
                    sizeOfImage);
            reader.bindSections(sections, sizeOfHeaders);

            PeRange exportDirectory = reader.directory(optional + 112, "export");
            PeRange importDirectory = reader.directory(optional + 120, "import");
            if (exportDirectory.overlaps(importDirectory)) {
                throw reader.invalid("export and import directories overlap");
            }
            Set<String> exports = readExports(reader, exportDirectory, name);
            Map<String, Set<String>> imports = readImports(reader, importDirectory);
            return new PeImage(name, Set.copyOf(exports), Map.copyOf(imports));
        }

        private static List<PeSection> readSections(
                PeReader reader,
                int table,
                int count,
                long sectionAlignment,
                long fileAlignment,
                long sizeOfHeaders,
                long sizeOfImage) throws IOException {
            List<PeSection> sections = new ArrayList<>(count);
            Set<String> names = new LinkedHashSet<>();
            for (int index = 0; index < count; index++) {
                int offset = table + index * 40;
                String name = reader.fixedAscii(offset, 8, "section name");
                if (name.isEmpty() || !names.add(name)) {
                    throw reader.invalid("empty or duplicate section name");
                }
                long virtualSize = reader.u32(offset + 8);
                long virtualAddress = reader.u32(offset + 12);
                long rawSize = reader.u32(offset + 16);
                long rawOffset = reader.u32(offset + 20);
                if (virtualAddress < align(sizeOfHeaders, sectionAlignment)
                        || virtualAddress % sectionAlignment != 0
                        || (virtualSize == 0 && rawSize == 0)) {
                    throw reader.invalid("invalid section virtual range: " + name);
                }
                long mappedSize = Math.max(virtualSize, rawSize);
                long virtualEnd = checkedAdd(
                        virtualAddress,
                        align(mappedSize, sectionAlignment),
                        reader,
                        "section virtual range");
                if (virtualEnd > sizeOfImage) {
                    throw reader.invalid("section exceeds SizeOfImage: " + name);
                }
                if (rawSize == 0) {
                    if (rawOffset != 0) {
                        throw reader.invalid("empty section has a raw offset: " + name);
                    }
                } else {
                    if (rawSize % fileAlignment != 0
                            || rawOffset < sizeOfHeaders
                            || rawOffset % fileAlignment != 0) {
                        throw reader.invalid("invalid section raw range: " + name);
                    }
                    reader.requireBytes(rawOffset, rawSize, "section raw data");
                }
                PeSection section = new PeSection(
                        name,
                        virtualAddress,
                        mappedSize,
                        rawOffset,
                        rawSize,
                        virtualEnd);
                for (PeSection previous : sections) {
                    if (section.virtualRange().overlaps(previous.virtualRange())
                            || section.rawRange().overlaps(previous.rawRange())) {
                        throw reader.invalid("overlapping PE sections");
                    }
                }
                sections.add(section);
            }
            return List.copyOf(sections);
        }

        private static Set<String> readExports(
                PeReader reader,
                PeRange directory,
                String expectedDllName) throws IOException {
            if (directory.size() < 40) {
                throw reader.invalid("truncated export directory");
            }
            int offset = reader.rvaOffset(directory.start(), 40, "export directory");
            String dllName = reader.rvaAscii(
                    reader.u32(offset + 12), MAXIMUM_ASCII_BYTES, "export DLL name");
            if (!dllName.equalsIgnoreCase(expectedDllName)) {
                throw reader.invalid("export DLL name does not match the manifest file");
            }
            long functionCount = reader.u32(offset + 20);
            long nameCount = reader.u32(offset + 24);
            if (functionCount <= 0 || functionCount > MAXIMUM_SYMBOLS
                    || nameCount <= 0 || nameCount > functionCount
                    || nameCount > MAXIMUM_SYMBOLS) {
                throw reader.invalid("invalid export table cardinality");
            }
            long functionsRva = reader.u32(offset + 28);
            long namesRva = reader.u32(offset + 32);
            long ordinalsRva = reader.u32(offset + 36);
            int functions = reader.rvaOffset(
                    functionsRva, functionCount * 4, "export address table");
            int names = reader.rvaOffset(
                    namesRva, nameCount * 4, "export name table");
            int ordinals = reader.rvaOffset(
                    ordinalsRva, nameCount * 2, "export ordinal table");
            Set<String> result = new LinkedHashSet<>();
            String previous = null;
            for (int index = 0; index < nameCount; index++) {
                String symbol = reader.rvaAscii(
                        reader.u32(names + index * 4),
                        MAXIMUM_ASCII_BYTES,
                        "export name");
                if (!result.add(symbol)) {
                    throw reader.invalid("duplicate named export: " + symbol);
                }
                if (previous != null && previous.compareTo(symbol) >= 0) {
                    throw reader.invalid("export name table is not strictly sorted");
                }
                previous = symbol;
                int ordinal = reader.u16(ordinals + index * 2);
                if (ordinal >= functionCount) {
                    throw reader.invalid("export ordinal escapes the address table");
                }
                long functionRva = reader.u32(functions + ordinal * 4);
                if (functionRva == 0) {
                    throw reader.invalid("named export has no function RVA: " + symbol);
                }
                if (directory.contains(functionRva)) {
                    reader.rvaAscii(
                            functionRva,
                            MAXIMUM_ASCII_BYTES,
                            "forwarded export target");
                } else {
                    reader.rvaOffset(functionRva, 1, "export function");
                }
            }
            return result;
        }

        private static Map<String, Set<String>> readImports(
                PeReader reader,
                PeRange directory) throws IOException {
            if (directory.size() < 40 || directory.size() % 20 != 0) {
                throw reader.invalid("invalid import-directory size");
            }
            int offset = reader.rvaOffset(
                    directory.start(), directory.size(), "import directory");
            int descriptorCount = Math.toIntExact(directory.size() / 20);
            Map<String, Set<String>> result = new LinkedHashMap<>();
            boolean terminated = false;
            for (int index = 0; index < descriptorCount; index++) {
                int descriptor = offset + index * 20;
                long originalThunk = reader.u32(descriptor);
                long timeDate = reader.u32(descriptor + 4);
                long forwarder = reader.u32(descriptor + 8);
                long nameRva = reader.u32(descriptor + 12);
                long firstThunk = reader.u32(descriptor + 16);
                if ((originalThunk | timeDate | forwarder | nameRva | firstThunk) == 0) {
                    terminated = true;
                    for (int tail = index + 1; tail < descriptorCount; tail++) {
                        int tailOffset = offset + tail * 20;
                        if (reader.u64(tailOffset) != 0
                                || reader.u64(tailOffset + 8) != 0
                                || reader.u32(tailOffset + 16) != 0) {
                            throw reader.invalid("nonzero import descriptor after terminator");
                        }
                    }
                    break;
                }
                if (nameRva == 0 || firstThunk == 0
                        || (originalThunk == 0 && firstThunk == 0)) {
                    throw reader.invalid("incomplete import descriptor");
                }
                String library = reader.rvaAscii(
                        nameRva, MAXIMUM_ASCII_BYTES, "import library name");
                String key = library.toLowerCase(Locale.ROOT);
                if (result.containsKey(key)) {
                    throw reader.invalid("duplicate import library: " + library);
                }
                long lookup = originalThunk == 0 ? firstThunk : originalThunk;
                Set<String> symbols = readImportThunks(reader, lookup);
                reader.rvaOffset(
                        firstThunk,
                        (long) (symbols.size() + 1) * 8,
                        "import address table");
                result.put(key, Set.copyOf(symbols));
            }
            if (!terminated || result.isEmpty()) {
                throw reader.invalid("unterminated or empty import directory");
            }
            return result;
        }

        private static Set<String> readImportThunks(PeReader reader, long tableRva)
                throws IOException {
            Set<String> symbols = new LinkedHashSet<>();
            for (int index = 0; index <= MAXIMUM_SYMBOLS; index++) {
                long entryRva = checkedAdd(
                        tableRva,
                        (long) index * 8,
                        reader,
                        "import lookup table");
                int entry = reader.rvaOffset(entryRva, 8, "import lookup entry");
                long value = reader.u64(entry);
                if (value == 0) {
                    if (symbols.isEmpty()) {
                        throw reader.invalid("empty import lookup table");
                    }
                    return symbols;
                }
                String symbol;
                if ((value & Long.MIN_VALUE) != 0) {
                    long ordinal = value & 0xffffL;
                    if ((value & 0x7fff_ffff_ffff_0000L) != 0 || ordinal == 0) {
                        throw reader.invalid("invalid ordinal import");
                    }
                    symbol = "#" + ordinal;
                } else {
                    if (value > 0xffff_ffffL) {
                        throw reader.invalid("import-name RVA exceeds PE32+ range");
                    }
                    reader.rvaOffset(value, 2, "import hint");
                    symbol = reader.rvaAscii(
                            checkedAdd(value, 2, reader, "import name"),
                            MAXIMUM_ASCII_BYTES,
                            "import name");
                }
                if (!symbols.add(symbol)) {
                    throw reader.invalid("duplicate imported symbol: " + symbol);
                }
            }
            throw reader.invalid("unterminated or excessive import lookup table");
        }

        private void requireExports(Set<String> required) throws IOException {
            if (!exports.containsAll(required)) {
                Set<String> missing = new TreeSet<>(required);
                missing.removeAll(exports);
                throw new IOException("invalid " + name
                        + " PE image: missing named exports " + missing);
            }
        }

        private void requireImports(String library, Set<String> requiredSymbols)
                throws IOException {
            Set<String> symbols = imports.get(library.toLowerCase(Locale.ROOT));
            if (symbols == null) {
                throw new IOException("invalid " + name
                        + " PE image: missing import library " + library);
            }
            if (!symbols.containsAll(requiredSymbols)) {
                Set<String> missing = new TreeSet<>(requiredSymbols);
                missing.removeAll(symbols);
                throw new IOException("invalid " + name
                        + " PE image: missing imported symbols " + missing);
            }
        }

        private static boolean powerOfTwo(long value) {
            return value > 0 && (value & (value - 1)) == 0;
        }

        private static long align(long value, long alignment) {
            return (value + alignment - 1) & -alignment;
        }

        private static long checkedAdd(
                long left,
                long right,
                PeReader reader,
                String label) throws IOException {
            if (left < 0 || right < 0 || left > 0xffff_ffffL - right) {
                throw reader.invalid(label + " overflows the PE RVA space");
            }
            return left + right;
        }
    }

    private record PeSection(
            String name,
            long virtualAddress,
            long mappedSize,
            long rawOffset,
            long rawSize,
            long virtualEnd) {
        private PeRange virtualRange() {
            return new PeRange(virtualAddress, virtualEnd);
        }

        private PeRange rawRange() {
            return rawSize == 0
                    ? PeRange.EMPTY
                    : new PeRange(rawOffset, rawOffset + rawSize);
        }
    }

    private record PeRange(long start, long end) {
        private static final PeRange EMPTY = new PeRange(0, 0);

        private long size() {
            return end - start;
        }

        private boolean contains(long value) {
            return value >= start && value < end;
        }

        private boolean overlaps(PeRange other) {
            return start < other.end && other.start < end;
        }
    }

    private static final class PeReader {
        private final byte[] bytes;
        private final String name;
        private List<PeSection> sections = List.of();
        private long sizeOfHeaders;

        private PeReader(byte[] bytes, String name) {
            this.bytes = Objects.requireNonNull(bytes, "bytes");
            this.name = Objects.requireNonNull(name, "name");
        }

        private void bindSections(List<PeSection> sections, long sizeOfHeaders) {
            this.sections = List.copyOf(sections);
            this.sizeOfHeaders = sizeOfHeaders;
        }

        private PeRange directory(int offset, String label) throws IOException {
            long rva = u32(offset);
            long size = u32(offset + 4);
            if (rva == 0 || size == 0) {
                throw invalid("missing " + label + " directory");
            }
            long end = PeImage.checkedAdd(rva, size, this, label + " directory");
            rvaOffset(rva, size, label + " directory");
            return new PeRange(rva, end);
        }

        private int rvaOffset(long rva, long length, String label) throws IOException {
            if (length <= 0) {
                throw invalid(label + " has an invalid length");
            }
            if (rva < sizeOfHeaders) {
                long end = PeImage.checkedAdd(rva, length, this, label);
                if (end <= sizeOfHeaders) {
                    requireBytes(rva, length, label);
                    return Math.toIntExact(rva);
                }
                throw invalid(label + " crosses the PE header boundary");
            }
            PeSection match = null;
            long end = PeImage.checkedAdd(rva, length, this, label);
            for (PeSection section : sections) {
                long mappedEnd = section.virtualAddress() + section.mappedSize();
                if (rva >= section.virtualAddress() && end <= mappedEnd) {
                    if (match != null) {
                        throw invalid(label + " maps through duplicate sections");
                    }
                    match = section;
                }
            }
            if (match == null) {
                throw invalid(label + " has an out-of-file RVA mapping");
            }
            long delta = rva - match.virtualAddress();
            if (delta > match.rawSize() || length > match.rawSize() - delta) {
                throw invalid(label + " maps into uninitialized section data");
            }
            long fileOffset = match.rawOffset() + delta;
            requireBytes(fileOffset, length, label);
            return Math.toIntExact(fileOffset);
        }

        private String rvaAscii(long rva, int maximumBytes, String label)
                throws IOException {
            int start = rvaOffset(rva, 1, label);
            StringBuilder value = new StringBuilder();
            for (int index = 0; index < maximumBytes; index++) {
                int offset = rvaOffset(rva + index, 1, label);
                int current = bytes[offset] & 0xff;
                if (current == 0) {
                    if (value.isEmpty()) {
                        throw invalid(label + " is empty");
                    }
                    return value.toString();
                }
                if (current < 0x21 || current > 0x7e) {
                    throw invalid(label + " is not printable ASCII");
                }
                value.append((char) current);
            }
            throw invalid(label + " is not terminated within its bound at " + start);
        }

        private String fixedAscii(int offset, int length, String label)
                throws IOException {
            requireBytes(offset, length, label);
            StringBuilder value = new StringBuilder(length);
            boolean terminated = false;
            for (int index = 0; index < length; index++) {
                int current = bytes[offset + index] & 0xff;
                if (current == 0) {
                    terminated = true;
                } else {
                    if (terminated || current < 0x21 || current > 0x7e) {
                        throw invalid(label + " is not canonical ASCII");
                    }
                    value.append((char) current);
                }
            }
            return value.toString();
        }

        private int u16(int offset) throws IOException {
            requireBytes(offset, 2, "16-bit PE field");
            return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
        }

        private long u32(int offset) throws IOException {
            requireBytes(offset, 4, "32-bit PE field");
            return ((long) bytes[offset] & 0xff)
                    | (((long) bytes[offset + 1] & 0xff) << 8)
                    | (((long) bytes[offset + 2] & 0xff) << 16)
                    | (((long) bytes[offset + 3] & 0xff) << 24);
        }

        private long u64(int offset) throws IOException {
            return u32(offset) | (u32(offset + 4) << 32);
        }

        private void requireBytes(long offset, long length, String label)
                throws IOException {
            if (offset < 0 || length < 0
                    || offset > bytes.length || length > bytes.length - offset) {
                throw invalid("truncated or out-of-file " + label);
            }
        }

        private IOException invalid(String detail) {
            return new IOException("invalid " + name + " PE image: " + detail);
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    record ExtractedBundle(
            Path root,
            Path loader,
            Path adapter,
            String sdkVersion,
            String identity) {}

    private record Manifest(
            String sdkVersion,
            String packageSha256,
            Map<String, Entry> files,
            String identity) {}

    private record Entry(String name, long size, String sha256) {}

    interface ResourceAccess {
        InputStream open(String resource) throws IOException;
    }

    private static final class ClasspathResources implements ResourceAccess {
        @Override
        public InputStream open(String resource) throws IOException {
            InputStream input = WebView2NativeBundle.class.getResourceAsStream(resource);
            if (input == null) {
                throw new IOException("missing packaged WebView2 native resource: " + resource);
            }
            return input;
        }
    }
}

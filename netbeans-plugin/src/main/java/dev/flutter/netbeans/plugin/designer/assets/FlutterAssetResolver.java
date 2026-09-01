package dev.flutter.netbeans.plugin.designer.assets;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

/**
 * Read-only, bounded resolver for application and package-declared Flutter
 * image assets.
 */
public final class FlutterAssetResolver {
    private static final int MAX_METADATA_BYTES = 1024 * 1024;
    private static final int MAX_PACKAGE_CONFIG_ENTRIES = 4_096;
    private static final Pattern VARIANT_DIRECTORY = Pattern.compile(
            "^(\\d+(?:\\.\\d*)?)x$");
    private static final ObjectMapper JSON = new ObjectMapper(
            JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxDocumentLength(MAX_METADATA_BYTES)
                            .maxNestingDepth(32)
                            .maxStringLength(16_384)
                            .maxNumberLength(100)
                            .build())
                    .build());
    private static final LoadSettings YAML_SETTINGS = LoadSettings.builder()
            .setLabel("pubspec.yaml")
            .setAllowDuplicateKeys(false)
            .setAllowRecursiveKeys(false)
            .setMaxAliasesForCollections(32)
            .setCodePointLimit(MAX_METADATA_BYTES)
            .setUseMarks(true)
            .build();

    /** Resolves one immutable inventory without editing or copying project data. */
    public FlutterAssetInventory resolve(
            Path projectRoot,
            FlutterAssetLimits limits) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(limits, "limits");
        Context context = new Context(limits);

        Optional<Path> resolvedProjectRoot = resolveProjectRoot(
                projectRoot, context);
        if (resolvedProjectRoot.isEmpty()) {
            return inventory(List.of(), context.diagnostics);
        }
        Path appRoot = resolvedProjectRoot.get();

        Optional<ParsedPubspec> appPubspec = parsePubspec(
                appRoot, null, context);
        TreeMap<FlutterAssetId, AssetSeed> seeds = new TreeMap<>();
        String appPackageName = null;
        if (appPubspec.isPresent()) {
            appPackageName = appPubspec.get().packageName();
            collectSeeds(
                    appRoot,
                    null,
                    appPubspec.get().declarations(),
                    seeds,
                    context);
        }

        List<PackageRoot> packageRoots = readPackageRoots(
                appRoot, appPackageName, context);
        List<Path> packageWatchRoots = new ArrayList<>();
        for (PackageRoot packageRoot : packageRoots) {
            Optional<ParsedPubspec> packagePubspec = parsePubspec(
                    packageRoot.root(), packageRoot.name(), context);
            if (packagePubspec.isEmpty()) {
                continue;
            }
            if (!packageRoot.name().equals(packagePubspec.get().packageName())) {
                context.diagnostic(
                        "validate package pubspec",
                        "package:" + packageRoot.name(),
                        "pubspec.yaml declares package name '"
                                + packagePubspec.get().packageName()
                                + "' instead of package_config name '"
                                + packageRoot.name() + "'");
                continue;
            }
            // A package_config entry is not watch authority by itself. Only a
            // real package whose bounded pubspec parsed successfully and owns
            // the configured name may become a recursive listener boundary.
            // The pubspec remains relevant metadata even when its current
            // declaration list is empty: adding the first asset must refresh
            // the designer without requiring a package_config rewrite.
            packageWatchRoots.add(packageRoot.root());
            collectSeeds(
                    packageRoot.root(),
                    packageRoot.name(),
                    packagePubspec.get().declarations(),
                    seeds,
                    context);
        }

        List<FlutterAsset> assets = new ArrayList<>();
        for (AssetSeed seed : seeds.values()) {
            resolveAsset(seed, context).ifPresent(assets::add);
        }
        assets.sort(Comparator.comparing(FlutterAsset::id));
        return inventory(
                assets,
                context.diagnostics,
                packageWatchRoots);
    }

    public FlutterAssetInventory resolve(Path projectRoot) {
        return resolve(projectRoot, FlutterAssetLimits.DEFAULT);
    }

    private static Optional<Path> resolveProjectRoot(
            Path supplied,
            Context context) {
        Path target = supplied.toAbsolutePath().normalize();
        try {
            Path real = target.toRealPath();
            if (!Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)) {
                context.diagnostic(
                        "resolve project root",
                        target.toString(),
                        "target is not a filesystem directory");
                return Optional.empty();
            }
            if (real.getParent() == null) {
                context.diagnostic(
                        "resolve project root",
                        target.toString(),
                        "filesystem root is too broad to use as a package boundary");
                return Optional.empty();
            }
            return Optional.of(real);
        } catch (IOException | SecurityException exception) {
            context.diagnostic(
                    "resolve project root",
                    target.toString(),
                    message(exception));
            return Optional.empty();
        }
    }

    private static Optional<ParsedPubspec> parsePubspec(
            Path packageRoot,
            String expectedPackageName,
            Context context) {
        String target = expectedPackageName == null
                ? "app:pubspec.yaml"
                : "package:" + expectedPackageName + ":pubspec.yaml";
        Optional<Path> pubspec = secureRegularFile(
                packageRoot,
                "pubspec.yaml",
                "read pubspec",
                target,
                false,
                context);
        if (pubspec.isEmpty()) {
            return Optional.empty();
        }
        Optional<byte[]> bytes = readMetadata(pubspec.get(), "read pubspec", target, context);
        if (bytes.isEmpty()) {
            return Optional.empty();
        }

        try {
            Optional<Node> document = new Compose(YAML_SETTINGS).composeString(
                    new String(bytes.get(), StandardCharsets.UTF_8));
            if (document.isEmpty() || !(document.get() instanceof MappingNode root)) {
                context.diagnostic(
                        "parse pubspec",
                        target,
                        "document root must be a YAML mapping");
                return Optional.empty();
            }
            Map<String, Node> rootEntries = yamlEntries(root);
            String packageName = yamlScalar(rootEntries.get("name"));
            try {
                packageName = FlutterAssetId.validatePackageName(packageName);
            } catch (NullPointerException | IllegalArgumentException exception) {
                context.diagnostic(
                        "parse pubspec",
                        target,
                        "missing or invalid lowercase Dart package name");
                return Optional.empty();
            }

            List<AssetDeclaration> declarations = new ArrayList<>();
            Node flutterNode = rootEntries.get("flutter");
            if (flutterNode == null) {
                return Optional.of(new ParsedPubspec(packageName, List.of()));
            }
            if (!(flutterNode instanceof MappingNode flutter)) {
                context.diagnostic(
                        "parse pubspec",
                        target,
                        "flutter section must be a YAML mapping");
                return Optional.empty();
            }
            Node assetsNode = yamlEntries(flutter).get("assets");
            if (assetsNode == null) {
                return Optional.of(new ParsedPubspec(packageName, List.of()));
            }
            if (!(assetsNode instanceof SequenceNode sequence)) {
                context.diagnostic(
                        "parse pubspec assets",
                        target,
                        "flutter.assets must be a YAML sequence");
                return Optional.empty();
            }
            int index = 0;
            for (Node assetNode : sequence.getValue()) {
                String declarationTarget = target + "#flutter.assets[" + index + ']';
                index++;
                String rawPath;
                if (assetNode instanceof ScalarNode) {
                    rawPath = yamlScalar(assetNode);
                } else if (assetNode instanceof MappingNode mapping) {
                    Map<String, Node> entry = yamlEntries(mapping);
                    rawPath = yamlScalar(entry.get("path"));
                    if (entry.keySet().stream().anyMatch(
                            key -> !key.equals("path"))) {
                        context.diagnostic(
                                "parse pubspec assets",
                                declarationTarget,
                                "conditional or transformed asset declarations "
                                        + "require a build context and are not inventory-safe");
                        continue;
                    }
                } else {
                    context.diagnostic(
                            "parse pubspec assets",
                            declarationTarget,
                            "asset entry must be a path scalar or a mapping with only path");
                    continue;
                }
                if (rawPath == null || rawPath.isEmpty()) {
                    context.diagnostic(
                            "parse pubspec assets",
                            declarationTarget,
                            "asset path is missing or empty");
                    continue;
                }
                boolean directory = rawPath.endsWith("/");
                String path = directory
                        ? rawPath.substring(0, rawPath.length() - 1)
                        : rawPath;
                try {
                    declarations.add(new AssetDeclaration(
                            FlutterAssetId.normalizeLogicalPath(path),
                            directory));
                } catch (IllegalArgumentException exception) {
                    context.diagnostic(
                            "normalize asset path",
                            declarationTarget + " ('" + rawPath + "')",
                            exception.getMessage());
                }
            }
            declarations.sort(Comparator
                    .comparing(AssetDeclaration::logicalPath)
                    .thenComparing(AssetDeclaration::directory));
            return Optional.of(new ParsedPubspec(
                    packageName,
                    declarations.stream().distinct().toList()));
        } catch (RuntimeException exception) {
            context.diagnostic(
                    "parse pubspec",
                    target,
                    message(exception));
            return Optional.empty();
        }
    }

    private static List<PackageRoot> readPackageRoots(
            Path appRoot,
            String appPackageName,
            Context context) {
        String target = "app:.dart_tool/package_config.json";
        Optional<Path> config = secureRegularFile(
                appRoot,
                ".dart_tool/package_config.json",
                "read package config",
                target,
                false,
                context);
        if (config.isEmpty()) {
            return List.of();
        }
        Optional<byte[]> content = readMetadata(
                config.get(), "read package config", target, context);
        if (content.isEmpty()) {
            return List.of();
        }

        JsonNode document;
        try {
            document = JSON.readTree(content.get());
        } catch (IOException | RuntimeException exception) {
            context.diagnostic(
                    "parse package config",
                    target,
                    message(exception));
            return List.of();
        }
        if (document == null || document.path("configVersion").asInt(-1) != 2
                || !document.path("packages").isArray()) {
            context.diagnostic(
                    "parse package config",
                    target,
                    "configVersion must be 2 and packages must be an array");
            return List.of();
        }
        if (document.path("packages").size() > MAX_PACKAGE_CONFIG_ENTRIES) {
            context.diagnostic(
                    "parse package config",
                    target,
                    "package count " + document.path("packages").size()
                            + " exceeds safe limit " + MAX_PACKAGE_CONFIG_ENTRIES);
            return List.of();
        }

        List<PackageRoot> candidates = new ArrayList<>();
        int index = 0;
        for (JsonNode entry : document.path("packages")) {
            String entryTarget = target + "#packages[" + index + ']';
            index++;
            if (!entry.isObject()) {
                context.diagnostic(
                        "parse package config",
                        entryTarget,
                        "package entry must be an object");
                continue;
            }
            String name = entry.path("name").isTextual()
                    ? entry.path("name").textValue()
                    : null;
            try {
                name = FlutterAssetId.validatePackageName(name);
            } catch (NullPointerException | IllegalArgumentException exception) {
                context.diagnostic(
                        "parse package config",
                        entryTarget,
                        "package name is missing or invalid");
                continue;
            }
            JsonNode rootUriNode = entry.path("rootUri");
            if (!rootUriNode.isTextual() || rootUriNode.textValue().isBlank()) {
                context.diagnostic(
                        "parse package config",
                        "package:" + name,
                        "rootUri is missing or blank");
                continue;
            }
            Optional<Path> root = resolvePackageRoot(
                    config.get(), name, rootUriNode.textValue(), context);
            if (root.isEmpty()) {
                continue;
            }
            if (root.get().equals(appRoot)) {
                if (!name.equals(appPackageName)) {
                    context.diagnostic(
                            "validate package root",
                            "package:" + name,
                            "package root aliases the app root owned by package '"
                                    + appPackageName + "'");
                }
                continue;
            }
            if (name.equals(appPackageName)) {
                context.diagnostic(
                        "validate package root",
                        "package:" + name,
                        "app package name resolves to a different real root: "
                                + root.get());
                continue;
            }
            candidates.add(new PackageRoot(name, root.get()));
        }

        candidates.sort(Comparator.comparing(PackageRoot::name)
                .thenComparing(candidate -> candidate.root().toString()));
        Map<String, PackageRoot> unique = new LinkedHashMap<>();
        for (PackageRoot candidate : candidates) {
            PackageRoot existing = unique.putIfAbsent(candidate.name(), candidate);
            if (existing != null) {
                context.diagnostic(
                        "validate package root",
                        "package:" + candidate.name(),
                        "duplicate package_config entries resolve to '"
                                + existing.root() + "' and '" + candidate.root() + "'");
            }
        }
        return List.copyOf(unique.values());
    }

    private static Optional<Path> resolvePackageRoot(
            Path packageConfig,
            String packageName,
            String rawRootUri,
            Context context) {
        String target = "package:" + packageName;
        try {
            URI declared = new URI(rawRootUri);
            if (declared.getQuery() != null || declared.getFragment() != null) {
                context.diagnostic(
                        "resolve package root",
                        target,
                        "rootUri must not contain query or fragment components");
                return Optional.empty();
            }
            URI resolved = packageConfig.getParent().toUri().resolve(declared);
            if (!"file".equalsIgnoreCase(resolved.getScheme())) {
                context.diagnostic(
                        "resolve package root",
                        target,
                        "rootUri resolves to non-file scheme '"
                                + resolved.getScheme() + "'");
                return Optional.empty();
            }
            Path root = Path.of(resolved).toRealPath();
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                context.diagnostic(
                        "resolve package root",
                        target,
                        "resolved root is not a filesystem directory: " + root);
                return Optional.empty();
            }
            if (root.getParent() == null) {
                context.diagnostic(
                        "resolve package root",
                        target,
                        "filesystem root is too broad to use as a package boundary");
                return Optional.empty();
            }
            return Optional.of(root);
        } catch (IOException | SecurityException | URISyntaxException
                | IllegalArgumentException exception) {
            context.diagnostic(
                    "resolve package root",
                    target,
                    "declared package root is invalid or unavailable ("
                            + exception.getClass().getSimpleName() + ')');
            return Optional.empty();
        }
    }

    private static void collectSeeds(
            Path root,
            String packageName,
            List<AssetDeclaration> declarations,
            TreeMap<FlutterAssetId, AssetSeed> seeds,
            Context context) {
        for (AssetDeclaration declaration : declarations) {
            if (!declaration.directory()) {
                addSeed(root, packageName, declaration.logicalPath(), seeds, context);
                continue;
            }
            String target = scopedName(packageName, declaration.logicalPath() + '/');
            Optional<Path> directory = secureDirectory(
                    root,
                    declaration.logicalPath(),
                    "enumerate asset directory",
                    target,
                    context);
            if (directory.isEmpty()) {
                continue;
            }
            Optional<List<Path>> entries = boundedDirectoryEntries(
                    directory.get(),
                    directoryEntryLimit(context.limits),
                    "enumerate asset directory",
                    target,
                    context);
            if (entries.isEmpty()) {
                continue;
            }
            for (Path entry : entries.get()) {
                if (Files.isSymbolicLink(entry)) {
                    context.diagnostic(
                            "enumerate asset directory",
                            target + entry.getFileName(),
                            "symbolic-link entries are not direct Flutter file assets");
                    continue;
                }
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String path = declaration.logicalPath() + '/'
                        + entry.getFileName();
                try {
                    path = FlutterAssetId.normalizeLogicalPath(path);
                    addSeed(root, packageName, path, seeds, context);
                } catch (IllegalArgumentException exception) {
                    context.diagnostic(
                            "normalize asset path",
                            target + entry.getFileName(),
                            exception.getMessage());
                }
            }
        }
    }

    private static void addSeed(
            Path root,
            String packageName,
            String logicalPath,
            TreeMap<FlutterAssetId, AssetSeed> seeds,
            Context context) {
        FlutterAssetId id = packageName == null
                ? FlutterAssetId.app(logicalPath)
                : FlutterAssetId.packageAsset(packageName, logicalPath);
        if (!seeds.containsKey(id) && seeds.size() >= context.limits.maxAssets()) {
            context.diagnostic(
                    "enforce asset count",
                    id.wireName(),
                    "logical asset count exceeds limit "
                            + context.limits.maxAssets());
            return;
        }
        seeds.putIfAbsent(id, new AssetSeed(id, root, logicalPath));
    }

    private static Optional<FlutterAsset> resolveAsset(
            AssetSeed seed,
            Context context) {
        List<Candidate> discovered = discoverCandidates(seed, context);
        if (discovered.isEmpty()) {
            context.diagnostic(
                    "resolve asset variants",
                    seed.id().wireName(),
                    "no regular base file or resolution variant was found");
            return Optional.empty();
        }

        TreeMap<Double, Candidate> candidatesByScale = new TreeMap<>();
        for (Candidate candidate : discovered) {
            Candidate replaced = candidatesByScale.put(candidate.scale(), candidate);
            if (replaced != null) {
                context.diagnostic(
                        "resolve asset variants",
                        seed.id().wireName(),
                        "duplicate scale " + candidate.scale() + " uses deterministic '"
                                + candidate.logicalPath() + "' instead of '"
                                + replaced.logicalPath() + "'");
            }
        }

        List<FlutterAssetVariant> variants = new ArrayList<>();
        for (Candidate candidate : candidatesByScale.values()) {
            readVariant(seed, candidate, context).ifPresent(variants::add);
        }
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FlutterAsset(seed.id(), variants));
    }

    private static List<Candidate> discoverCandidates(
            AssetSeed seed,
            Context context) {
        Path lexicalBase = resolveLexical(seed.root(), seed.logicalPath());
        String parentLogical = logicalParent(seed.logicalPath());
        Optional<Path> parent = secureDirectory(
                seed.root(),
                parentLogical,
                "resolve asset variants",
                seed.id().wireName(),
                context);
        if (parent.isEmpty()) {
            return List.of();
        }

        List<Candidate> candidates = new ArrayList<>();
        if (Files.exists(lexicalBase) || Files.isSymbolicLink(lexicalBase)) {
            double baseScale;
            try {
                baseScale = scaleFromLogicalPath(seed.logicalPath());
            } catch (IllegalArgumentException exception) {
                context.diagnostic(
                        "resolve asset variants",
                        seed.id().wireName(),
                        exception.getMessage());
                baseScale = Double.NaN;
            }
            if (Double.isFinite(baseScale)) {
                double resolvedBaseScale = baseScale;
                secureRegularFile(
                        seed.root(),
                        seed.logicalPath(),
                        "resolve asset file",
                        seed.id().wireName(),
                        true,
                        context).ifPresent(path -> candidates.add(new Candidate(
                                seed.logicalPath(),
                                path,
                                resolvedBaseScale)));
            }
        }

        Optional<List<Path>> entries = boundedDirectoryEntries(
                parent.get(),
                directoryEntryLimit(context.limits),
                "discover asset variants",
                seed.id().wireName(),
                context);
        if (entries.isEmpty()) {
            return candidates;
        }
        String basename = lexicalBase.getFileName().toString();
        for (Path entry : entries.get()) {
            String directoryName = entry.getFileName().toString();
            Matcher matcher = VARIANT_DIRECTORY.matcher(directoryName);
            if (!matcher.matches()) {
                continue;
            }
            if (Files.isSymbolicLink(entry)) {
                context.diagnostic(
                        "discover asset variants",
                        seed.id().wireName() + " -> " + directoryName,
                        "symbolic-link variant directories are not Flutter variant directories");
                continue;
            }
            if (!Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            double scale;
            try {
                scale = Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException exception) {
                context.diagnostic(
                        "discover asset variants",
                        seed.id().wireName() + " -> " + directoryName,
                        "variant scale is not a finite decimal number");
                continue;
            }
            if (!Double.isFinite(scale) || scale <= 0.0) {
                context.diagnostic(
                        "discover asset variants",
                        seed.id().wireName() + " -> " + directoryName,
                        "variant scale must be finite and positive");
                continue;
            }
            String logicalPath = parentLogical.isEmpty()
                    ? directoryName + '/' + basename
                    : parentLogical + '/' + directoryName + '/' + basename;
            if (!Files.exists(resolveLexical(seed.root(), logicalPath))) {
                continue;
            }
            secureRegularFile(
                    seed.root(),
                    logicalPath,
                    "resolve asset variant",
                    seed.id().wireName() + " -> " + logicalPath,
                    true,
                    context).ifPresent(path -> candidates.add(
                            new Candidate(logicalPath, path, scale)));
        }
        candidates.sort(Comparator.comparing(Candidate::logicalPath));
        return candidates;
    }

    private static Optional<FlutterAssetVariant> readVariant(
            AssetSeed seed,
            Candidate candidate,
            Context context) {
        String target = seed.id().wireName() + " -> " + candidate.logicalPath();
        if (!context.reserveAssetFile(target)) {
            return Optional.empty();
        }
        long reportedSize;
        try {
            reportedSize = Files.size(candidate.realPath());
        } catch (IOException | SecurityException exception) {
            context.diagnostic("read asset file", target, message(exception));
            return Optional.empty();
        }
        if (reportedSize > context.limits.maxFileBytes()) {
            context.diagnostic(
                    "enforce asset file bytes",
                    target,
                    "file size " + reportedSize + " exceeds limit "
                            + context.limits.maxFileBytes());
            return Optional.empty();
        }
        if (wouldExceed(context.acceptedBytes, reportedSize,
                context.limits.maxTotalBytes())) {
            context.diagnostic(
                    "enforce total asset bytes",
                    target,
                    "adding " + reportedSize + " bytes would exceed total limit "
                            + context.limits.maxTotalBytes());
            return Optional.empty();
        }

        byte[] bytes;
        try {
            bytes = readBounded(candidate.realPath(), context.limits.maxFileBytes());
        } catch (IOException | FileLimitException exception) {
            context.diagnostic("read asset file", target, message(exception));
            return Optional.empty();
        }
        if (wouldExceed(context.acceptedBytes, bytes.length,
                context.limits.maxTotalBytes())) {
            context.diagnostic(
                    "enforce total asset bytes",
                    target,
                    "adding " + bytes.length + " bytes would exceed total limit "
                            + context.limits.maxTotalBytes());
            return Optional.empty();
        }

        ImageHeader header;
        try {
            header = ImageHeader.parse(bytes);
        } catch (IllegalArgumentException exception) {
            context.diagnostic(
                    "inspect asset image",
                    target,
                    exception.getMessage());
            return Optional.empty();
        }
        if (header.width() > context.limits.maxDimension()
                || header.height() > context.limits.maxDimension()) {
            context.diagnostic(
                    "enforce asset dimensions",
                    target,
                    "image dimensions " + header.width() + 'x' + header.height()
                            + " exceed per-axis limit " + context.limits.maxDimension());
            return Optional.empty();
        }
        long pixels = (long) header.width() * header.height();
        if (pixels > context.limits.maxPixels()) {
            context.diagnostic(
                    "enforce asset pixels",
                    target,
                    "image pixel count " + pixels + " exceeds limit "
                            + context.limits.maxPixels());
            return Optional.empty();
        }

        context.acceptedBytes += bytes.length;
        return Optional.of(new FlutterAssetVariant(
                candidate.logicalPath(),
                candidate.scale(),
                header.format(),
                header.width(),
                header.height(),
                sha256(bytes),
                bytes));
    }

    private static Optional<Path> secureRegularFile(
            Path root,
            String logicalPath,
            String operation,
            String target,
            boolean allowMissing,
            Context context) {
        Path lexical = resolveLexical(root, logicalPath);
        if (!lexical.startsWith(root)) {
            context.diagnostic(operation, target, "normalized path escapes package root");
            return Optional.empty();
        }
        if (!Files.exists(lexical) && !Files.isSymbolicLink(lexical)) {
            if (!allowMissing) {
                context.diagnostic(operation, target, "file does not exist: " + lexical);
            }
            return Optional.empty();
        }
        try {
            Path real = lexical.toRealPath();
            if (!real.startsWith(root)) {
                context.diagnostic(
                        operation,
                        target,
                        "real path escapes package root: " + real);
                return Optional.empty();
            }
            if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                context.diagnostic(operation, target, "target is not a regular file: " + real);
                return Optional.empty();
            }
            return Optional.of(real);
        } catch (IOException | SecurityException exception) {
            context.diagnostic(operation, target, message(exception));
            return Optional.empty();
        }
    }

    private static Optional<Path> secureDirectory(
            Path root,
            String logicalPath,
            String operation,
            String target,
            Context context) {
        Path lexical = logicalPath.isEmpty()
                ? root
                : resolveLexical(root, logicalPath);
        if (!lexical.startsWith(root)) {
            context.diagnostic(operation, target, "normalized path escapes package root");
            return Optional.empty();
        }
        try {
            Path real = lexical.toRealPath();
            if (!real.startsWith(root)) {
                context.diagnostic(
                        operation,
                        target,
                        "real path escapes package root: " + real);
                return Optional.empty();
            }
            if (!Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)) {
                context.diagnostic(operation, target, "target is not a directory: " + real);
                return Optional.empty();
            }
            return Optional.of(real);
        } catch (IOException | SecurityException exception) {
            context.diagnostic(operation, target, message(exception));
            return Optional.empty();
        }
    }

    private static Optional<List<Path>> boundedDirectoryEntries(
            Path directory,
            int requestedLimit,
            String operation,
            String target,
            Context context) {
        int limit = Math.max(1, Math.min(16_384, requestedLimit));
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                entries.add(entry);
                if (entries.size() > limit) {
                    context.diagnostic(
                            operation,
                            target,
                            "directory entry count exceeds bounded scan limit " + limit);
                    return Optional.empty();
                }
            }
        } catch (IOException | SecurityException exception) {
            context.diagnostic(operation, target, message(exception));
            return Optional.empty();
        }
        entries.sort(Comparator.comparing(
                entry -> entry.getFileName().toString()));
        return Optional.of(entries);
    }

    private static Optional<byte[]> readMetadata(
            Path path,
            String operation,
            String target,
            Context context) {
        try {
            return Optional.of(readBounded(path, MAX_METADATA_BYTES));
        } catch (IOException | FileLimitException exception) {
            context.diagnostic(operation, target, message(exception));
            return Optional.empty();
        }
    }

    private static byte[] readBounded(Path path, long limit)
            throws IOException, FileLimitException {
        int initialCapacity = (int) Math.min(8_192L, limit);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[8_192];
        long count = 0L;
        try (InputStream input = Files.newInputStream(
                path, LinkOption.NOFOLLOW_LINKS)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                count += read;
                if (count > limit) {
                    throw new FileLimitException(
                            "file exceeds byte limit " + limit + " while reading");
                }
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static FlutterAssetInventory inventory(
            List<FlutterAsset> assets,
            List<FlutterAssetDiagnostic> diagnostics) {
        return inventory(assets, diagnostics, List.of());
    }

    private static FlutterAssetInventory inventory(
            List<FlutterAsset> assets,
            List<FlutterAssetDiagnostic> diagnostics,
            List<Path> packageWatchRoots) {
        List<FlutterAssetDiagnostic> sortedDiagnostics = new ArrayList<>(diagnostics);
        sortedDiagnostics.sort(Comparator.naturalOrder());
        return new FlutterAssetInventory(
                assets,
                sortedDiagnostics,
                fingerprint(assets),
                packageWatchRoots);
    }

    private static String fingerprint(List<FlutterAsset> assets) {
        MessageDigest digest = newSha256();
        update(digest, FlutterAssetInventory.SELECTION_ALGORITHM);
        update(digest, assets.size());
        for (FlutterAsset asset : assets) {
            update(digest, asset.id().wireName());
            update(digest, asset.variants().size());
            for (FlutterAssetVariant variant : asset.variants()) {
                update(digest, variant.logicalPath());
                update(digest, Double.doubleToLongBits(variant.scale()));
                update(digest, variant.format().name());
                update(digest, variant.width());
                update(digest, variant.height());
                update(digest, variant.byteLength());
                update(digest, variant.sha256());
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        update(digest, bytes.length);
        digest.update(bytes);
    }

    private static void update(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void update(MessageDigest digest, long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            digest.update((byte) (value >>> shift));
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(newSha256().digest(bytes));
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static Map<String, Node> yamlEntries(MappingNode mapping) {
        Map<String, Node> result = new LinkedHashMap<>();
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode key) {
                result.put(key.getValue(), tuple.getValueNode());
            }
        }
        return result;
    }

    private static String yamlScalar(Node node) {
        return node instanceof ScalarNode scalar ? scalar.getValue() : null;
    }

    private static Path resolveLexical(Path root, String logicalPath) {
        Path result = root;
        if (!logicalPath.isEmpty()) {
            for (String segment : logicalPath.split("/")) {
                result = result.resolve(segment);
            }
        }
        return result.normalize();
    }

    private static String logicalParent(String logicalPath) {
        int slash = logicalPath.lastIndexOf('/');
        return slash < 0 ? "" : logicalPath.substring(0, slash);
    }

    private static double scaleFromLogicalPath(String logicalPath) {
        String parent = logicalParent(logicalPath);
        int slash = parent.lastIndexOf('/');
        String directory = slash < 0 ? parent : parent.substring(slash + 1);
        Matcher matcher = VARIANT_DIRECTORY.matcher(directory);
        if (!matcher.matches()) {
            return 1.0;
        }
        double scale = Double.parseDouble(matcher.group(1));
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException(
                    "asset path has a non-positive resolution scale directory");
        }
        return scale;
    }

    private static int directoryEntryLimit(FlutterAssetLimits limits) {
        return (int) Math.min(16_384L, (long) limits.maxAssets() + 1L);
    }

    private static String scopedName(String packageName, String logicalPath) {
        return packageName == null
                ? "app:" + logicalPath
                : "package:" + packageName + ':' + logicalPath;
    }

    private static boolean wouldExceed(long current, long increment, long limit) {
        return increment > limit || current > limit - increment;
    }

    private static String message(Throwable throwable) {
        String value = throwable.getMessage();
        return value == null || value.isBlank()
                ? throwable.getClass().getSimpleName()
                : value;
    }

    private record AssetDeclaration(String logicalPath, boolean directory) {
    }

    private record ParsedPubspec(
            String packageName,
            List<AssetDeclaration> declarations) {
    }

    private record PackageRoot(String name, Path root) {
    }

    private record AssetSeed(
            FlutterAssetId id,
            Path root,
            String logicalPath) {
    }

    private record Candidate(
            String logicalPath,
            Path realPath,
            double scale) {
    }

    private record ImageHeader(
            FlutterImageFormat format,
            int width,
            int height) {

        static ImageHeader parse(byte[] bytes) {
            if (isPng(bytes)) {
                return png(bytes);
            }
            if (isJpeg(bytes)) {
                return jpeg(bytes);
            }
            if (isGif(bytes)) {
                return gif(bytes);
            }
            if (isWebp(bytes)) {
                return webp(bytes);
            }
            throw new IllegalArgumentException(
                    "unsupported image magic; allowed formats are PNG, JPEG, GIF, and WebP");
        }

        private static boolean isPng(byte[] bytes) {
            byte[] signature = {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            };
            return startsWith(bytes, signature);
        }

        private static ImageHeader png(byte[] bytes) {
            if (bytes.length < 24
                    || bytes[12] != 'I' || bytes[13] != 'H'
                    || bytes[14] != 'D' || bytes[15] != 'R') {
                throw new IllegalArgumentException("malformed PNG IHDR header");
            }
            return dimensions(
                    FlutterImageFormat.PNG,
                    unsignedIntBigEndian(bytes, 16),
                    unsignedIntBigEndian(bytes, 20));
        }

        private static boolean isJpeg(byte[] bytes) {
            return bytes.length >= 2
                    && unsigned(bytes[0]) == 0xff
                    && unsigned(bytes[1]) == 0xd8;
        }

        private static ImageHeader jpeg(byte[] bytes) {
            int offset = 2;
            while (offset + 1 < bytes.length) {
                if (unsigned(bytes[offset]) != 0xff) {
                    offset++;
                    continue;
                }
                while (offset < bytes.length && unsigned(bytes[offset]) == 0xff) {
                    offset++;
                }
                if (offset >= bytes.length) {
                    break;
                }
                int marker = unsigned(bytes[offset++]);
                if (marker == 0xd9 || marker == 0xda) {
                    break;
                }
                if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) {
                    continue;
                }
                if (offset + 2 > bytes.length) {
                    break;
                }
                int length = unsignedShortBigEndian(bytes, offset);
                if (length < 2 || offset + length > bytes.length) {
                    throw new IllegalArgumentException("malformed JPEG segment length");
                }
                if (isStartOfFrame(marker)) {
                    if (length < 7) {
                        throw new IllegalArgumentException("malformed JPEG SOF header");
                    }
                    return dimensions(
                            FlutterImageFormat.JPEG,
                            unsignedShortBigEndian(bytes, offset + 5),
                            unsignedShortBigEndian(bytes, offset + 3));
                }
                offset += length;
            }
            throw new IllegalArgumentException("JPEG has no supported SOF dimensions");
        }

        private static boolean isStartOfFrame(int marker) {
            return switch (marker) {
                case 0xc0, 0xc1, 0xc2, 0xc3,
                        0xc5, 0xc6, 0xc7,
                        0xc9, 0xca, 0xcb,
                        0xcd, 0xce, 0xcf -> true;
                default -> false;
            };
        }

        private static boolean isGif(byte[] bytes) {
            return startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
        }

        private static ImageHeader gif(byte[] bytes) {
            if (bytes.length < 10) {
                throw new IllegalArgumentException("malformed GIF logical screen header");
            }
            return dimensions(
                    FlutterImageFormat.GIF,
                    unsignedShortLittleEndian(bytes, 6),
                    unsignedShortLittleEndian(bytes, 8));
        }

        private static boolean isWebp(byte[] bytes) {
            return bytes.length >= 12
                    && ascii(bytes, 0, "RIFF")
                    && ascii(bytes, 8, "WEBP");
        }

        private static ImageHeader webp(byte[] bytes) {
            int offset = 12;
            while (offset + 8 <= bytes.length) {
                long chunkLength = unsignedIntLittleEndian(bytes, offset + 4);
                long dataOffset = offset + 8L;
                long end = dataOffset + chunkLength;
                if (end > bytes.length || end > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("malformed WebP chunk length");
                }
                int data = (int) dataOffset;
                if (ascii(bytes, offset, "VP8X")) {
                    if (chunkLength < 10) {
                        throw new IllegalArgumentException("malformed WebP VP8X header");
                    }
                    return dimensions(
                            FlutterImageFormat.WEBP,
                            1L + unsigned24LittleEndian(bytes, data + 4),
                            1L + unsigned24LittleEndian(bytes, data + 7));
                }
                if (ascii(bytes, offset, "VP8 ")) {
                    if (chunkLength < 10
                            || unsigned(bytes[data + 3]) != 0x9d
                            || unsigned(bytes[data + 4]) != 0x01
                            || unsigned(bytes[data + 5]) != 0x2a) {
                        throw new IllegalArgumentException("malformed WebP VP8 header");
                    }
                    return dimensions(
                            FlutterImageFormat.WEBP,
                            unsignedShortLittleEndian(bytes, data + 6) & 0x3fff,
                            unsignedShortLittleEndian(bytes, data + 8) & 0x3fff);
                }
                if (ascii(bytes, offset, "VP8L")) {
                    if (chunkLength < 5 || unsigned(bytes[data]) != 0x2f) {
                        throw new IllegalArgumentException("malformed WebP VP8L header");
                    }
                    long width = 1L + unsigned(bytes[data + 1])
                            + ((long) (unsigned(bytes[data + 2]) & 0x3f) << 8);
                    long height = 1L
                            + ((unsigned(bytes[data + 2]) & 0xc0) >> 6)
                            + ((long) unsigned(bytes[data + 3]) << 2)
                            + ((long) (unsigned(bytes[data + 4]) & 0x0f) << 10);
                    return dimensions(FlutterImageFormat.WEBP, width, height);
                }
                offset = (int) end + ((chunkLength & 1L) == 0L ? 0 : 1);
            }
            throw new IllegalArgumentException("WebP has no supported image dimension chunk");
        }

        private static ImageHeader dimensions(
                FlutterImageFormat format,
                long width,
                long height) {
            if (width <= 0 || height <= 0
                    || width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "image dimensions must be positive 32-bit integers");
            }
            return new ImageHeader(format, (int) width, (int) height);
        }

        private static boolean startsWith(byte[] bytes, byte[] prefix) {
            if (bytes.length < prefix.length) {
                return false;
            }
            for (int index = 0; index < prefix.length; index++) {
                if (bytes[index] != prefix[index]) {
                    return false;
                }
            }
            return true;
        }

        private static boolean ascii(byte[] bytes, int offset, String value) {
            if (offset < 0 || offset + value.length() > bytes.length) {
                return false;
            }
            for (int index = 0; index < value.length(); index++) {
                if (unsigned(bytes[offset + index]) != value.charAt(index)) {
                    return false;
                }
            }
            return true;
        }

        private static int unsigned(byte value) {
            return value & 0xff;
        }

        private static int unsignedShortBigEndian(byte[] bytes, int offset) {
            return unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
        }

        private static int unsignedShortLittleEndian(byte[] bytes, int offset) {
            return unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8;
        }

        private static long unsignedIntBigEndian(byte[] bytes, int offset) {
            return (long) unsigned(bytes[offset]) << 24
                    | (long) unsigned(bytes[offset + 1]) << 16
                    | (long) unsigned(bytes[offset + 2]) << 8
                    | unsigned(bytes[offset + 3]);
        }

        private static long unsignedIntLittleEndian(byte[] bytes, int offset) {
            return unsigned(bytes[offset])
                    | (long) unsigned(bytes[offset + 1]) << 8
                    | (long) unsigned(bytes[offset + 2]) << 16
                    | (long) unsigned(bytes[offset + 3]) << 24;
        }

        private static long unsigned24LittleEndian(byte[] bytes, int offset) {
            return unsigned(bytes[offset])
                    | (long) unsigned(bytes[offset + 1]) << 8
                    | (long) unsigned(bytes[offset + 2]) << 16;
        }
    }

    private static final class Context {
        private final FlutterAssetLimits limits;
        private final List<FlutterAssetDiagnostic> diagnostics = new ArrayList<>();
        private int assetFiles;
        private long acceptedBytes;
        private boolean assetCountReported;

        Context(FlutterAssetLimits limits) {
            this.limits = limits;
        }

        void diagnostic(String operation, String target, String reason) {
            diagnostics.add(new FlutterAssetDiagnostic(operation, target, reason));
        }

        boolean reserveAssetFile(String target) {
            if (assetFiles >= limits.maxAssets()) {
                if (!assetCountReported) {
                    diagnostic(
                            "enforce asset file count",
                            target,
                            "physical asset file count exceeds limit "
                                    + limits.maxAssets());
                    assetCountReported = true;
                }
                return false;
            }
            assetFiles++;
            return true;
        }
    }

    private static final class FileLimitException extends Exception {
        FileLimitException(String message) {
            super(message);
        }
    }
}

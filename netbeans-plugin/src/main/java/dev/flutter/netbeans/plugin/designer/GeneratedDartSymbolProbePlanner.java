package dev.flutter.netbeans.plugin.designer;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.dart.DartStaticTypeProbe;
import dev.flutter.netbeans.designer.generation.DartManagedRegionId;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegion;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.generation.GeneratedDartSymbolOccurrence;
import dev.flutter.netbeans.designer.generation.GeneratedDartStaticTypeRequirement;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartDesignerSuperclassOccurrence;
import dev.flutter.netbeans.designer.source.DartManagedRegionSnapshot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

/** Maps generator-owned payload occurrences to one exact Dart candidate. */
final class GeneratedDartSymbolProbePlanner {
    static final String DESIGNER_SUPERCLASS_PROBE_ID =
            "source:designer-superclass";

    private static final String FLUTTER_LIBRARY_PREFIX = "package:flutter/";
    private static final String PROPERTY_REFERENCE_ID_TOKEN =
            ":property-reference:";
    private static final Pattern PROJECT_PACKAGE_LIBRARY_URI = Pattern.compile(
            "package:[a-z][a-z0-9_]*/"
            + "(?:[A-Za-z0-9_-][A-Za-z0-9_.-]*/)*"
            + "[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.dart");
    private static final Pattern PROJECT_PACKAGE_NAME = Pattern.compile(
            "[a-z][a-z0-9_]*");
    private static final int MAXIMUM_PUBSPEC_UTF8_BYTES = 512 * 1024;
    private static final int MAXIMUM_PACKAGE_CONFIG_UTF8_BYTES = 4 * 1024 * 1024;
    private static final int MAXIMUM_DECLARED_PACKAGES = 4096;
    private static final ObjectMapper JSON = new ObjectMapper(
            new JsonFactoryBuilder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                    .build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final LoadSettings PUBSPEC_SETTINGS = LoadSettings.builder()
            .setLabel("trusted project pubspec.yaml")
            .setAllowDuplicateKeys(false)
            .setAllowRecursiveKeys(false)
            .setMaxAliasesForCollections(32)
            .setCodePointLimit(MAXIMUM_PUBSPEC_UTF8_BYTES)
            .setUseMarks(true)
            .build();
    private static final String WIDGETS_LIBRARY_URI =
            "package:flutter/widgets.dart";
    private static final String GENERATED_BUILD_PROOF_PREFIX =
            "  @override\n  Widget build(BuildContext context) {\n";
    private static final String CLASS_TARGET_KIND = "CLASS";

    private GeneratedDartSymbolProbePlanner() {
    }

    static List<DartSymbolProbe> plan(
            PreparedDesignerPair prepared,
            Path trustedFlutterSdkRoot,
            Path trustedProjectRoot) {
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(trustedFlutterSdkRoot,
                "trustedFlutterSdkRoot");
        Objects.requireNonNull(trustedProjectRoot, "trustedProjectRoot");
        if (!trustedFlutterSdkRoot.isAbsolute()) {
            throw new IllegalArgumentException(
                    "trustedFlutterSdkRoot must be absolute");
        }
        if (!trustedProjectRoot.isAbsolute()) {
            throw new IllegalArgumentException(
                    "trustedProjectRoot must be absolute");
        }

        GeneratedDartRegions generated = prepared.dartTransition()
                .generation()
                .generated()
                .orElseThrow(() -> new IllegalArgumentException(
                "The prepared transition has no generated Dart payloads"));
        DartCandidateCapacityBudget capacity =
                generated.candidateCapacityBudget();
        if (capacity.reservedSourceSymbolProbes() != 1) {
            throw new IllegalArgumentException(
                    "The generated capacity profile does not reserve the exact scanner probe");
        }
        if (generated.symbolOccurrences().isEmpty()) {
            throw new IllegalArgumentException(
                    "The generated Dart payload has no symbol-occurrence manifest");
        }
        byte[] candidateBytes = prepared.prospectiveDartBytes();
        if (hasUtf8Bom(candidateBytes)) {
            throw new IllegalArgumentException(
                    "The exact prospective Dart candidate must be BOM-free");
        }
        String candidate = decodeStrict(candidateBytes);
        int importsStart = payloadStartUtf16(
                candidateBytes,
                generated.imports(),
                region(prepared, DartManagedRegionId.IMPORTS));
        int buildStart = payloadStartUtf16(
                candidateBytes,
                generated.build(),
                region(prepared, DartManagedRegionId.BUILD));
        int staticTypeStatementInsertion = generated.symbolOccurrences().stream()
                .anyMatch(occurrence -> occurrence.staticTypeRequirement().isPresent())
                ? Math.addExact(buildStart, staticTypeStatementInsertion(generated))
                : -1;

        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        Path normalizedRoot = trustedFlutterSdkRoot.normalize();
        Path projectLibraryRoot = null;
        Map<String, Path> declaredPackageRoots = null;
        Set<String> referencedPackageNames = new HashSet<>();
        for (GeneratedDartSymbolOccurrence occurrence
                : generated.symbolOccurrences()) {
            if (isProjectReferenceOccurrence(occurrence)
                    && !DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI.equals(
                            occurrence.libraryUri())) {
                if (!isProjectLibraryUri(occurrence.libraryUri())) {
                    throw new IllegalArgumentException(
                            "Project-reference occurrence has an invalid library URI: "
                            + occurrence.id());
                }
                referencedPackageNames.add(packageName(
                        occurrence.libraryUri()));
            }
        }
        probes.add(superclassProbe(
                prepared,
                candidateBytes,
                candidate,
                superclassTargetRoot(normalizedRoot)));
        for (GeneratedDartSymbolOccurrence occurrence
                : generated.symbolOccurrences()) {
            Path expectedTargetRoot;
            if (isProjectReferenceOccurrence(occurrence)) {
                if (!isProjectLibraryUri(occurrence.libraryUri())) {
                    throw new IllegalArgumentException(
                            "Project-reference occurrence has an invalid library URI: "
                            + occurrence.id());
                }
                if (projectLibraryRoot == null) {
                    // A current Dart library may include declarations from part
                    // files, so the trusted scope is the real project lib tree,
                    // not only the paired library's primary Dart file.
                    projectLibraryRoot = projectLibraryRoot(trustedProjectRoot);
                }
                if (!DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI.equals(
                        occurrence.libraryUri())) {
                    if (declaredPackageRoots == null) {
                        declaredPackageRoots = declaredPackageLibraryRoots(
                                trustedProjectRoot,
                                referencedPackageNames);
                    }
                    String packageName = packageName(occurrence.libraryUri());
                    expectedTargetRoot = declaredPackageRoots.get(packageName);
                    if (expectedTargetRoot == null) {
                        throw new IllegalArgumentException(
                                "Project-reference package URI is not declared by the trusted project: "
                                + occurrence.libraryUri());
                    }
                } else {
                    expectedTargetRoot = projectLibraryRoot;
                }
            } else if (occurrence.libraryUri().startsWith(FLUTTER_LIBRARY_PREFIX)) {
                expectedTargetRoot = normalizedRoot;
            } else {
                continue;
            }
            int payloadStart = switch (occurrence.region()) {
                case IMPORTS -> importsStart;
                case BUILD -> buildStart;
            };
            int candidateOffset = Math.addExact(payloadStart, occurrence.offset());
            int candidateEnd = Math.addExact(candidateOffset, occurrence.length());
            if (candidateEnd > candidate.length()
                    || !candidate.substring(candidateOffset, candidateEnd)
                            .equals(occurrence.symbolName())) {
                throw new IllegalArgumentException(
                        "Generated symbol occurrence does not identify the exact candidate: "
                        + occurrence.id());
            }
            Optional<DartStaticTypeProbe> staticTypeProbe = occurrence
                    .staticTypeRequirement()
                    .map(requirement -> staticTypeProbe(
                    requirement,
                    occurrence,
                    candidate,
                    importsStart,
                    buildStart,
                    staticTypeStatementInsertion));
            probes.add(new DartSymbolProbe(
                    occurrence.id(),
                    candidateOffset,
                    occurrence.length(),
                    occurrence.symbolName(),
                    occurrence.libraryUri(),
                    expectedTargetRoot,
                    Optional.empty(),
                    staticTypeProbe));
        }
        if (probes.stream().noneMatch(probe -> probe.expectedSymbolName()
                        .equals("Widget"))
                || probes.stream().noneMatch(probe -> probe.expectedSymbolName()
                        .equals("BuildContext"))) {
            throw new IllegalArgumentException(
                    "Generated Dart symbol probes must include Widget and BuildContext");
        }
        probes.sort(Comparator.comparingInt(DartSymbolProbe::offset)
                .thenComparing(DartSymbolProbe::id));
        validateCombinedManifest(probes, capacity);
        return List.copyOf(probes);
    }

    private static boolean isProjectReferenceOccurrence(
            GeneratedDartSymbolOccurrence occurrence) {
        if (!occurrence.id().contains(PROPERTY_REFERENCE_ID_TOKEN)) {
            return false;
        }
        return (occurrence.id().endsWith(":root")
                    && occurrence.modelPath().endsWith("/rootSymbol"))
                || (occurrence.id().endsWith(":member")
                    && occurrence.modelPath().endsWith("/member"));
    }

    private static int staticTypeStatementInsertion(
            GeneratedDartRegions generated) {
        String build = generated.build().payload();
        if (!build.startsWith(GENERATED_BUILD_PROOF_PREFIX)
                || !build.startsWith("    return ",
                        GENERATED_BUILD_PROOF_PREFIX.length())) {
            throw new IllegalArgumentException(
                    "Generated build payload has no canonical static-type proof scope");
        }
        return GENERATED_BUILD_PROOF_PREFIX.length();
    }

    private static DartStaticTypeProbe staticTypeProbe(
            GeneratedDartStaticTypeRequirement requirement,
            GeneratedDartSymbolOccurrence occurrence,
            String candidate,
            int importsStart,
            int buildStart,
            int statementInsertion) {
        if (occurrence.region() != DartManagedRegionId.BUILD) {
            throw new IllegalArgumentException(
                    "Static-type occurrence is outside the generated build region: "
                    + occurrence.id());
        }
        int expressionOffset = Math.addExact(
                buildStart, requirement.expressionOffset());
        int expressionEnd = Math.addExact(
                expressionOffset, requirement.expressionLength());
        if (expressionEnd > candidate.length()) {
            throw new IllegalArgumentException(
                    "Static-type expression is outside the exact candidate: "
                    + occurrence.id());
        }
        String expression = candidate.substring(expressionOffset, expressionEnd);
        if (expression.indexOf('\r') >= 0 || expression.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "Static-type expression is not a generated single-line value: "
                    + occurrence.id());
        }
        return new DartStaticTypeProbe(
                expressionOffset,
                requirement.expressionLength(),
                importsStart,
                statementInsertion,
                requirement.expectedDartType(),
                WIDGETS_LIBRARY_URI);
    }

    private static boolean isProjectLibraryUri(String value) {
        if (DartRegionGenerator.CURRENT_PROJECT_LIBRARY_URI.equals(value)) {
            return true;
        }
        return PROJECT_PACKAGE_LIBRARY_URI.matcher(value).matches()
                && !value.contains("//")
                && !value.contains("/./")
                && !value.contains("/../");
    }

    private static String packageName(String libraryUri) {
        int slash = libraryUri.indexOf('/', "package:".length());
        if (slash <= "package:".length()) {
            throw new IllegalArgumentException(
                    "Project package URI has no package name: " + libraryUri);
        }
        return libraryUri.substring("package:".length(), slash);
    }

    private static Path projectLibraryRoot(Path projectRoot) {
        try {
            Path projectReal = projectRoot.toRealPath();
            Path libraryReal = projectReal.resolve("lib").toRealPath();
            if (!Files.isDirectory(libraryReal)
                    || !libraryReal.startsWith(projectReal)) {
                throw new IllegalArgumentException(
                        "trusted project lib must resolve below the trusted project root");
            }
            return libraryReal;
        } catch (java.io.IOException | SecurityException unavailable) {
            throw new IllegalArgumentException(
                    "trusted project lib cannot be resolved to a real directory",
                    unavailable);
        }
    }

    private static String projectPackageName(Path projectRoot) {
        try {
            Path projectReal = projectRoot.toRealPath();
            Path pubspecReal = projectReal.resolve("pubspec.yaml").toRealPath();
            if (!pubspecReal.getParent().equals(projectReal)
                    || !Files.isRegularFile(pubspecReal)) {
                throw new IllegalArgumentException(
                        "trusted project pubspec.yaml must be a regular root file");
            }
            byte[] bytes;
            try (InputStream input = Files.newInputStream(pubspecReal)) {
                bytes = input.readNBytes(MAXIMUM_PUBSPEC_UTF8_BYTES + 1);
            }
            if (bytes.length == 0
                    || bytes.length > MAXIMUM_PUBSPEC_UTF8_BYTES) {
                throw new IllegalArgumentException(
                        "trusted project pubspec.yaml exceeds the bounded size contract");
            }
            String source = decodeStrict(bytes);
            Node root = new Compose(PUBSPEC_SETTINGS).composeString(source)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "trusted project pubspec.yaml is empty"));
            if (!(root instanceof MappingNode mapping)) {
                throw new IllegalArgumentException(
                        "trusted project pubspec.yaml root must be a mapping");
            }
            String packageName = null;
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key
                        && "name".equals(key.getValue())) {
                    if (!(tuple.getValueNode() instanceof ScalarNode value)
                            || packageName != null) {
                        throw new IllegalArgumentException(
                                "trusted project pubspec.yaml has no unique scalar name");
                    }
                    packageName = value.getValue();
                }
            }
            if (packageName == null
                    || !PROJECT_PACKAGE_NAME.matcher(packageName).matches()) {
                throw new IllegalArgumentException(
                        "trusted project pubspec.yaml has no canonical package name");
            }
            return packageName;
        } catch (java.io.IOException | SecurityException unavailable) {
            throw new IllegalArgumentException(
                    "trusted project pubspec.yaml cannot be resolved and read",
                    unavailable);
        }
    }

    private static Map<String, Path> declaredPackageLibraryRoots(
            Path projectRoot,
            Set<String> referencedPackageNames) {
        Objects.requireNonNull(referencedPackageNames,
                "referencedPackageNames");
        try {
            Path projectReal = projectRoot.toRealPath();
            String owningPackage = projectPackageName(projectReal);
            HashSet<String> requiredPackageNames = new HashSet<>(
                    referencedPackageNames);
            requiredPackageNames.add(owningPackage);
            Path config = projectReal.resolve(".dart_tool/package_config.json");
            if (!Files.isRegularFile(config, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(config)) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json must be a safe regular file");
            }
            Path configReal = config.toRealPath();
            if (!configReal.startsWith(projectReal)
                    || !configReal.getParent().equals(
                            projectReal.resolve(".dart_tool").normalize())) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json escapes the project");
            }
            byte[] bytes;
            try (InputStream input = Files.newInputStream(configReal)) {
                bytes = input.readNBytes(MAXIMUM_PACKAGE_CONFIG_UTF8_BYTES + 1);
            }
            if (bytes.length == 0
                    || bytes.length > MAXIMUM_PACKAGE_CONFIG_UTF8_BYTES) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json exceeds the bounded size contract");
            }
            if (hasUtf8Bom(bytes)) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json must be BOM-free strict UTF-8");
            }
            final String configSource;
            try {
                configSource = decodeStrict(bytes);
            } catch (IllegalArgumentException invalidUtf8) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json is not valid strict UTF-8",
                        invalidUtf8);
            }
            JsonNode root = JSON.readTree(configSource);
            if (root == null || !root.isObject()
                    || !root.path("configVersion").isIntegralNumber()
                    || !root.path("configVersion").canConvertToInt()
                    || root.path("configVersion").intValue() != 2
                    || !root.path("packages").isArray()) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json has an invalid v2 root");
            }
            JsonNode packages = root.path("packages");
            if (packages.size() > MAXIMUM_DECLARED_PACKAGES) {
                throw new IllegalArgumentException(
                        "trusted project package_config.json exceeds the package-entry limit");
            }
            LinkedHashMap<String, Path> roots = new LinkedHashMap<>();
            HashSet<String> declaredNames = new HashSet<>();
            int index = 0;
            for (JsonNode candidate : packages) {
                if (!candidate.isObject()) {
                    throw new IllegalArgumentException(
                            "package_config entry " + index + " is not an object");
                }
                String name = requiredPackageText(candidate, "name", index);
                if (name.length() > 128
                        || !PROJECT_PACKAGE_NAME.matcher(name).matches()
                        || !declaredNames.add(name)) {
                    throw new IllegalArgumentException(
                            "package_config entry " + index
                            + " has a duplicate or non-canonical name");
                }
                String rootUri = requiredPackageText(
                        candidate, "rootUri", index);
                JsonNode packageUriNode = candidate.get("packageUri");
                String packageUri = null;
                if (packageUriNode != null && !packageUriNode.isNull()) {
                    if (!packageUriNode.isTextual()
                            || packageUriNode.textValue().isBlank()) {
                        throw new IllegalArgumentException(
                                "package_config entry '" + name
                                + "' has an invalid packageUri");
                    }
                    packageUri = packageUriNode.textValue();
                }
                if (!requiredPackageNames.contains(name)) {
                    index++;
                    continue;
                }
                Path packageRoot = resolveConfigRoot(
                        configReal.getParent(), rootUri, name);
                Path packageLibrary = packageUri == null
                        ? packageRoot
                        : resolvePackageLibraryRoot(
                                packageRoot, packageUri, name);
                roots.put(name, packageLibrary);
                index++;
            }

            Path projectLibrary = projectLibraryRoot(projectReal);
            Path configuredOwner = roots.get(owningPackage);
            if (configuredOwner == null
                    || !Files.isSameFile(projectLibrary, configuredOwner)) {
                throw new IllegalArgumentException(
                        "pubspec and package_config self-package library roots disagree");
            }
            for (Map.Entry<String, Path> entry : roots.entrySet()) {
                if (!entry.getKey().equals(owningPackage)
                        && Files.isSameFile(projectLibrary, entry.getValue())) {
                    throw new IllegalArgumentException(
                            "package_config entry '" + entry.getKey()
                            + "' aliases the owning project library root");
                }
            }
            return Collections.unmodifiableMap(roots);
        } catch (java.io.IOException | SecurityException unavailable) {
            throw new IllegalArgumentException(
                    "trusted project package_config.json cannot be resolved and read",
                    unavailable);
        }
    }

    private static String requiredPackageText(
            JsonNode candidate,
            String field,
            int index) {
        JsonNode value = candidate.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(
                    "package_config entry " + index
                    + " has a missing or non-text " + field);
        }
        return value.textValue();
    }

    private static Path resolveConfigRoot(
            Path configFolder,
            String value,
            String packageName) {
        if (value.isBlank() || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' has an ambiguous rootUri");
        }
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException malformed) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' has a malformed rootUri", malformed);
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' rootUri contains unsupported URI components");
        }
        final Path resolved;
        try {
            if (uri.isAbsolute()) {
                if (!"file".equalsIgnoreCase(uri.getScheme())) {
                    throw new IllegalArgumentException(
                            "package_config entry '" + packageName
                            + "' rootUri is not a file URI");
                }
                resolved = Path.of(uri);
            } else {
                if (uri.getRawAuthority() != null
                        || uri.getPath() == null || uri.getPath().isBlank()) {
                    throw new IllegalArgumentException(
                            "package_config entry '" + packageName
                            + "' rootUri has no path");
                }
                resolved = Path.of(configFolder.toUri().resolve(uri));
            }
            if (!Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException(
                        "package_config entry '" + packageName
                        + "' rootUri is not a physical directory");
            }
            return resolved.toRealPath();
        } catch (java.io.IOException | RuntimeException unavailable) {
            if (unavailable instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' rootUri cannot be resolved", unavailable);
        }
    }

    private static Path resolvePackageLibraryRoot(
            Path packageRoot,
            String packageUri,
            String packageName) {
        if (packageUri.indexOf('\\') >= 0 || packageUri.indexOf('%') >= 0) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' has an ambiguous packageUri");
        }
        final URI uri;
        try {
            uri = new URI(packageUri);
        } catch (URISyntaxException malformed) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' has a malformed packageUri", malformed);
        }
        if (uri.isAbsolute()
                || uri.getRawAuthority() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getPath() == null
                || uri.getPath().isBlank()) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' packageUri is not a safe relative path");
        }
        try {
            Path rootReal = packageRoot.toRealPath();
            Path lexicalLibrary = packageRoot.resolve(uri.getPath()).normalize();
            if (!lexicalLibrary.startsWith(packageRoot.normalize())
                    || !Files.isDirectory(
                            lexicalLibrary, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException(
                        "package_config entry '" + packageName
                        + "' packageUri escapes or is unavailable");
            }
            Path libraryReal = lexicalLibrary.toRealPath();
            if (!libraryReal.startsWith(rootReal)) {
                throw new IllegalArgumentException(
                        "package_config entry '" + packageName
                        + "' packageUri escapes through a link");
            }
            return libraryReal;
        } catch (java.io.IOException | SecurityException unavailable) {
            throw new IllegalArgumentException(
                    "package_config entry '" + packageName
                    + "' packageUri cannot be resolved", unavailable);
        }
    }

    private static DartSymbolProbe superclassProbe(
            PreparedDesignerPair prepared,
            byte[] candidateBytes,
            String candidate,
            Path expectedTargetRoot) {
        DartSourceIntegrityResult integrity = prepared.dartTransition()
                .candidateIntegrity();
        if (!integrity.onDiskDeclaredMatch()
                || integrity.original().isEmpty()
                || !integrity.original().orElseThrow()
                        .contentEquals(candidateBytes)) {
            throw new IllegalArgumentException(
                    "The exact candidate does not retain matching scanner evidence");
        }
        DartDesignerSuperclassOccurrence occurrence = integrity
                .superclassOccurrence()
                .orElseThrow(() -> new IllegalArgumentException(
                "The exact candidate has no scanner-owned Designer superclass occurrence"));
        if (!occurrence.className().equals(
                        prepared.dartTransition().prospectiveDescriptor().className())
                || !DartDesignerSuperclassOccurrence.SYMBOL_NAME.equals(
                        occurrence.symbolName())
                || occurrence.endByte() > candidateBytes.length) {
            throw new IllegalArgumentException(
                    "Scanner-owned superclass evidence does not describe the prospective class");
        }

        int startUtf16 = decodeStrict(
                candidateBytes, 0, occurrence.startByte()).length();
        int endUtf16 = decodeStrict(
                candidateBytes, 0, occurrence.endByte()).length();
        if (startUtf16 != occurrence.startUtf16()
                || endUtf16 != occurrence.endUtf16()
                || endUtf16 > candidate.length()
                || !candidate.substring(startUtf16, endUtf16)
                        .equals(occurrence.symbolName())) {
            throw new IllegalArgumentException(
                    "Scanner-owned superclass coordinates do not identify the exact candidate");
        }
        return new DartSymbolProbe(
                DESIGNER_SUPERCLASS_PROBE_ID,
                startUtf16,
                occurrence.lengthUtf16(),
                occurrence.symbolName(),
                WIDGETS_LIBRARY_URI,
                expectedTargetRoot,
                Optional.of(CLASS_TARGET_KIND));
    }

    private static Path superclassTargetRoot(Path trustedRoot) {
        Path frameworkLibrary = trustedRoot.resolve("packages")
                .resolve("flutter")
                .resolve("lib");
        if (!Files.isDirectory(frameworkLibrary)) {
            return trustedRoot;
        }
        try {
            Path trustedReal = trustedRoot.toRealPath();
            Path frameworkReal = frameworkLibrary.toRealPath();
            return frameworkReal.startsWith(trustedReal)
                    ? frameworkReal : trustedRoot;
        } catch (java.io.IOException | SecurityException unavailable) {
            return trustedRoot;
        }
    }

    private static void validateCombinedManifest(
            List<DartSymbolProbe> probes,
            DartCandidateCapacityBudget capacity) {
        if (probes.size() > capacity.maxSymbolProbes()) {
            throw new IllegalArgumentException(
                    "Dart candidate requires " + probes.size()
                    + " distinct Flutter/project symbol probes, exceeding shared capacity profile "
                    + capacity.profileId() + " with maxSymbolProbes="
                    + capacity.maxSymbolProbes());
        }
        Set<String> ids = new HashSet<>();
        int previousEnd = -1;
        for (DartSymbolProbe probe : probes) {
            if (!ids.add(probe.id())) {
                throw new IllegalArgumentException(
                        "Duplicate Dart symbol probe id: " + probe.id());
            }
            if (probe.offset() < previousEnd) {
                throw new IllegalArgumentException(
                        "Dart symbol probe occurrences overlap at " + probe.id());
            }
            previousEnd = Math.addExact(probe.offset(), probe.length());
        }
    }

    private static DartManagedRegionSnapshot region(
            PreparedDesignerPair prepared,
            DartManagedRegionId id) {
        return prepared.dartTransition()
                .candidateIntegrity()
                .region(id.wireName())
                .orElseThrow(() -> new IllegalArgumentException(
                "The exact candidate has no managed region " + id.wireName()));
    }

    private static int payloadStartUtf16(
            byte[] candidate,
            GeneratedDartRegion generated,
            DartManagedRegionSnapshot snapshot) {
        if (!snapshot.id().equals(generated.id().wireName())
                || snapshot.payloadEndByte() > candidate.length
                || !Arrays.equals(
                        Arrays.copyOfRange(
                                candidate,
                                snapshot.payloadStartByte(),
                                snapshot.payloadEndByte()),
                        generated.utf8Bytes())) {
            throw new IllegalArgumentException(
                    "Generated payload does not equal the exact candidate region "
                    + generated.id().wireName());
        }
        return decodeStrict(candidate, 0, snapshot.payloadStartByte()).length();
    }

    private static String decodeStrict(byte[] bytes) {
        return decodeStrict(bytes, 0, bytes.length);
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }

    private static String decodeStrict(byte[] bytes, int offset, int length) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length));
            return decoded.toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IllegalArgumentException(
                    "The exact Dart candidate is not strict UTF-8", invalidUtf8);
        }
    }
}

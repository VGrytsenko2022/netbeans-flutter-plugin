package dev.flutter.netbeans.plugin.pubspec;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;

/** Performs lightweight semantic validation after NetBeans' YAML syntax validation. */
public final class PubspecValidator {
    public static final int MAX_DOCUMENT_LENGTH = 512 * 1024;

    private static final Set<String> DEPENDENCY_SECTIONS = Set.of(
            "dependencies", "dev_dependencies", "dependency_overrides");
    private static final Set<String> DEPENDENCY_SOURCES = Set.of(
            "path", "git", "hosted", "sdk");
    private static final Set<String> FLUTTER_BOOLEAN_FIELDS = Set.of(
            "uses-material-design", "generate");
    private static final Set<String> FLUTTER_LIST_FIELDS = Set.of(
            "assets", "licenses", "shaders", "fonts");
    private static final Set<String> DART_RESERVED_WORDS = Set.of(
            "abstract", "as", "assert", "async", "await", "base", "break", "case",
            "catch", "class", "const", "continue", "covariant", "default", "deferred",
            "do", "dynamic", "else", "enum", "export", "extends", "extension", "external",
            "factory", "false", "final", "finally", "for", "get", "hide", "if",
            "implements", "import", "in", "interface", "is", "late", "library", "mixin",
            "new", "null", "of", "on", "operator", "part", "required", "rethrow",
            "return", "sealed", "set", "show", "static", "super", "switch", "sync",
            "this", "throw", "true", "try", "typedef", "var", "void", "when", "while",
            "with", "yield");

    public List<PubspecDiagnostic> validate(String source, Path packageRoot) {
        return validate(source, packageRoot, () -> false);
    }

    public List<PubspecDiagnostic> validate(
            String source,
            Path packageRoot,
            BooleanSupplier cancelled) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(cancelled, "cancelled");
        if (cancelled.getAsBoolean() || source.length() > MAX_DOCUMENT_LENGTH) {
            return List.of();
        }

        final Node root;
        try {
            root = PubspecYaml.compose(source).orElse(null);
        } catch (RuntimeException ex) {
            // The bundled NetBeans YAML parser owns syntax diagnostics. Avoid duplicates here.
            return List.of();
        }
        if (cancelled.getAsBoolean()) {
            return List.of();
        }

        List<PubspecDiagnostic> diagnostics = new ArrayList<>();
        if (!(root instanceof MappingNode rootMapping)) {
            diagnostics.add(error(
                    "pubspec.root.type",
                    span(root, source),
                    "The pubspec root must be a YAML mapping."));
            return List.copyOf(diagnostics);
        }

        Map<String, PubspecYaml.Entry> rootEntries = PubspecYaml.entries(rootMapping);
        validateName(rootEntries, source, diagnostics);
        validateEnvironment(rootEntries, source, diagnostics);
        for (String section : DEPENDENCY_SECTIONS) {
            if (cancelled.getAsBoolean()) {
                return List.of();
            }
            validateDependencies(
                    section,
                    rootEntries.get(section),
                    source,
                    packageRoot,
                    diagnostics,
                    cancelled);
        }
        if (!cancelled.getAsBoolean()) {
            validateFlutter(
                    rootEntries.get("flutter"),
                    source,
                    packageRoot,
                    diagnostics,
                    cancelled);
        }
        return cancelled.getAsBoolean() ? List.of() : List.copyOf(diagnostics);
    }

    static boolean isValidPackageName(String name) {
        return name != null
                && name.matches("[a-z_][a-z0-9_]*")
                && !DART_RESERVED_WORDS.contains(name);
    }

    private static void validateName(
            Map<String, PubspecYaml.Entry> root,
            String source,
            List<PubspecDiagnostic> diagnostics) {
        PubspecYaml.Entry entry = root.get("name");
        if (entry == null) {
            diagnostics.add(error(
                    "pubspec.name.missing",
                    startOfDocument(source),
                    "The pubspec must declare a package name with 'name:'."));
            return;
        }
        String name = PubspecYaml.scalar(entry.value());
        if (name == null) {
            diagnostics.add(error(
                    "pubspec.name.type",
                    span(entry.value(), source),
                    "The pubspec package name must be a scalar value."));
        } else if (!isValidPackageName(name)) {
            diagnostics.add(error(
                    "pubspec.name.invalid",
                    span(entry.value(), source),
                    "Package name '" + name
                            + "' must be a lowercase Dart identifier using letters, digits, and underscores."));
        }
    }

    private static void validateEnvironment(
            Map<String, PubspecYaml.Entry> root,
            String source,
            List<PubspecDiagnostic> diagnostics) {
        PubspecYaml.Entry environment = root.get("environment");
        if (environment == null) {
            diagnostics.add(error(
                    "pubspec.environment.missing",
                    startOfDocument(source),
                    "The pubspec must declare an 'environment' mapping with an SDK constraint."));
            return;
        }
        if (!(environment.value() instanceof MappingNode mapping)) {
            diagnostics.add(error(
                    "pubspec.environment.type",
                    span(environment.value(), source),
                    "The 'environment' section must be a YAML mapping."));
            return;
        }
        PubspecYaml.Entry sdk = PubspecYaml.entries(mapping).get("sdk");
        if (sdk == null) {
            diagnostics.add(error(
                    "pubspec.environment.sdk.missing",
                    span(environment.key(), source),
                    "The 'environment' section must declare a Dart SDK constraint with 'sdk:'."));
            return;
        }
        String constraint = PubspecYaml.scalar(sdk.value());
        if (constraint == null || constraint.isBlank()) {
            diagnostics.add(error(
                    "pubspec.environment.sdk.type",
                    span(sdk.value(), source),
                    "The Dart SDK constraint must be a non-empty scalar value."));
        }
    }

    private static void validateDependencies(
            String sectionName,
            PubspecYaml.Entry section,
            String source,
            Path packageRoot,
            List<PubspecDiagnostic> diagnostics,
            BooleanSupplier cancelled) {
        if (section == null) {
            return;
        }
        if (!(section.value() instanceof MappingNode dependencies)) {
            diagnostics.add(error(
                    "pubspec.section.type",
                    span(section.value(), source),
                    "The '" + sectionName + "' section must be a YAML mapping."));
            return;
        }

        for (Map.Entry<String, PubspecYaml.Entry> dependency
                : PubspecYaml.entries(dependencies).entrySet()) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            Node descriptor = dependency.getValue().value();
            if (descriptor instanceof ScalarNode) {
                continue;
            }
            if (!(descriptor instanceof MappingNode descriptorMapping)) {
                diagnostics.add(error(
                        "pubspec.dependency.type",
                        span(descriptor, source),
                        "Dependency '" + dependency.getKey()
                                + "' must be a version scalar or a source mapping."));
                continue;
            }
            Map<String, PubspecYaml.Entry> descriptorEntries = PubspecYaml.entries(descriptorMapping);
            Set<String> sources = new TreeSet<>(descriptorEntries.keySet());
            sources.retainAll(DEPENDENCY_SOURCES);
            if (sources.size() > 1) {
                diagnostics.add(error(
                        "pubspec.dependency.source.conflict",
                        span(descriptor, source),
                        "Dependency '" + dependency.getKey()
                                + "' declares conflicting sources: " + String.join(", ", sources) + "."));
            }
            PubspecYaml.Entry path = descriptorEntries.get("path");
            if (path != null) {
                validateLocalPath(
                        "Dependency '" + dependency.getKey() + "'",
                        path.value(),
                        source,
                        packageRoot,
                        "pubspec.path.missing",
                        diagnostics);
            }
        }
    }

    private static void validateFlutter(
            PubspecYaml.Entry flutter,
            String source,
            Path packageRoot,
            List<PubspecDiagnostic> diagnostics,
            BooleanSupplier cancelled) {
        if (flutter == null) {
            return;
        }
        if (!(flutter.value() instanceof MappingNode mapping)) {
            diagnostics.add(error(
                    "pubspec.flutter.type",
                    span(flutter.value(), source),
                    "The 'flutter' section must be a YAML mapping."));
            return;
        }

        Map<String, PubspecYaml.Entry> entries = PubspecYaml.entries(mapping);
        for (String field : FLUTTER_BOOLEAN_FIELDS) {
            PubspecYaml.Entry entry = entries.get(field);
            if (entry != null && !isBoolean(entry.value())) {
                diagnostics.add(error(
                        "pubspec.flutter.boolean.type",
                        span(entry.value(), source),
                        "Flutter field '" + field + "' must be true or false."));
            }
        }
        for (String field : FLUTTER_LIST_FIELDS) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            PubspecYaml.Entry entry = entries.get(field);
            if (entry == null) {
                continue;
            }
            if (!(entry.value() instanceof SequenceNode sequence)) {
                diagnostics.add(error(
                        "pubspec.flutter.list.type",
                        span(entry.value(), source),
                        "Flutter field '" + field + "' must be a YAML list."));
                continue;
            }
            if (Set.of("assets", "licenses", "shaders").contains(field)) {
                validateAssetSequence(field, sequence, source, packageRoot, diagnostics, cancelled);
            } else if ("fonts".equals(field)) {
                validateFonts(sequence, source, packageRoot, diagnostics, cancelled);
            }
        }
    }

    private static boolean isBoolean(Node node) {
        return node instanceof ScalarNode scalar && Tag.BOOL.equals(scalar.getTag());
    }

    private static void validateAssetSequence(
            String field,
            SequenceNode sequence,
            String source,
            Path packageRoot,
            List<PubspecDiagnostic> diagnostics,
            BooleanSupplier cancelled) {
        for (Node item : sequence.getValue()) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            Node pathNode = item;
            if (item instanceof MappingNode mapping) {
                PubspecYaml.Entry path = PubspecYaml.entries(mapping).get("path");
                if (path == null) {
                    continue;
                }
                pathNode = path.value();
            }
            validateLocalPath(
                    "Flutter " + field + " entry",
                    pathNode,
                    source,
                    packageRoot,
                    "pubspec.asset.missing",
                    diagnostics);
        }
    }

    private static void validateFonts(
            SequenceNode fonts,
            String source,
            Path packageRoot,
            List<PubspecDiagnostic> diagnostics,
            BooleanSupplier cancelled) {
        for (Node font : fonts.getValue()) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            if (!(font instanceof MappingNode fontMapping)) {
                diagnostics.add(error(
                        "pubspec.flutter.font.type",
                        span(font, source),
                        "Each Flutter font entry must be a YAML mapping."));
                continue;
            }
            PubspecYaml.Entry assets = PubspecYaml.entries(fontMapping).get("fonts");
            if (assets == null) {
                continue;
            }
            if (!(assets.value() instanceof SequenceNode assetSequence)) {
                diagnostics.add(error(
                        "pubspec.flutter.fonts.type",
                        span(assets.value(), source),
                        "A font family's 'fonts' field must be a YAML list."));
                continue;
            }
            for (Node asset : assetSequence.getValue()) {
                if (cancelled.getAsBoolean()) {
                    return;
                }
                Node assetPath = asset;
                if (asset instanceof MappingNode assetMapping) {
                    PubspecYaml.Entry assetEntry = PubspecYaml.entries(assetMapping).get("asset");
                    if (assetEntry == null) {
                        continue;
                    }
                    assetPath = assetEntry.value();
                }
                validateLocalPath(
                        "Flutter font asset",
                        assetPath,
                        source,
                        packageRoot,
                        "pubspec.asset.missing",
                        diagnostics);
            }
        }
    }

    private static void validateLocalPath(
            String target,
            Node pathNode,
            String source,
            Path packageRoot,
            String code,
            List<PubspecDiagnostic> diagnostics) {
        String value = PubspecYaml.scalar(pathNode);
        if (value == null || value.isBlank()) {
            diagnostics.add(error(
                    code + ".type",
                    span(pathNode, source),
                    target + " must name a non-empty local path."));
            return;
        }
        if (packageRoot == null) {
            return;
        }
        final Path resolved;
        try {
            Path declared = Path.of(value);
            resolved = declared.isAbsolute()
                    ? declared.normalize()
                    : packageRoot.resolve(declared).normalize();
        } catch (InvalidPathException ex) {
            diagnostics.add(warning(
                    code,
                    span(pathNode, source),
                    target + " uses invalid local path '" + value + "'."));
            return;
        }
        if (!Files.exists(resolved)) {
            diagnostics.add(warning(
                    code,
                    span(pathNode, source),
                    target + " path '" + value + "' does not exist."));
        }
    }

    private static PubspecDiagnostic error(String code, Span span, String message) {
        return new PubspecDiagnostic(
                code,
                PubspecDiagnostic.Severity.ERROR,
                span.start(),
                span.end(),
                message);
    }

    private static PubspecDiagnostic warning(String code, Span span, String message) {
        return new PubspecDiagnostic(
                code,
                PubspecDiagnostic.Severity.WARNING,
                span.start(),
                span.end(),
                message);
    }

    private static Span startOfDocument(String source) {
        return new Span(0, Math.min(1, source.length()));
    }

    private static Span span(Node node, String source) {
        if (node == null) {
            return startOfDocument(source);
        }
        int start = node.getStartMark()
                .map(mark -> charOffset(source, mark))
                .orElse(0);
        int end = node.getEndMark()
                .map(mark -> charOffset(source, mark))
                .orElse(start);
        start = Math.max(0, Math.min(start, source.length()));
        end = Math.max(start, Math.min(end, source.length()));
        if (end == start && start < source.length()) {
            end++;
        }
        return new Span(start, end);
    }

    private static int charOffset(String source, Mark mark) {
        int codePoints = source.codePointCount(0, source.length());
        int index = Math.max(0, Math.min(mark.getIndex(), codePoints));
        return source.offsetByCodePoints(0, index);
    }

    private record Span(int start, int end) {
    }
}

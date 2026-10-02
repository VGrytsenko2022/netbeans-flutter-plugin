package io.github.vgrytsenko2022.plugin.pubspec;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tolerant, line-oriented completion model for an in-progress pubspec. */
public final class PubspecCompletionEngine {
    private static final Pattern MAPPING_ENTRY = Pattern.compile(
            "([A-Za-z_][A-Za-z0-9_-]*)\\s*:\\s*(.*)");
    private static final Set<String> DEPENDENCY_SECTIONS = Set.of(
            "dependencies", "dev_dependencies", "dependency_overrides");

    private static final List<KeySpec> ROOT_KEYS = List.of(
            scalar("name", "Required Dart package name"),
            scalar("description", "Package description"),
            scalar("version", "Semantic package version"),
            scalar("publish_to", "Package publishing destination", "none"),
            scalar("homepage", "Package homepage URL"),
            scalar("repository", "Source repository URL"),
            scalar("issue_tracker", "Issue tracker URL"),
            scalar("documentation", "Package documentation URL"),
            mapping("environment", "Dart and Flutter SDK constraints"),
            mapping("dependencies", "Runtime package dependencies"),
            mapping("dev_dependencies", "Development-only package dependencies"),
            mapping("dependency_overrides", "Temporary dependency overrides"),
            mapping("executables", "Command-line executables"),
            mapping("platforms", "Explicitly supported platforms"),
            list("funding", "Package funding URLs"),
            list("false_secrets", "Files excluded from secret detection"),
            list("screenshots", "Screenshots shown on pub.dev"),
            list("topics", "pub.dev package topics"),
            list("ignored_advisories", "Ignored security advisory identifiers"),
            mapping("hooks", "Package hook configuration"),
            mapping("flutter", "Flutter-specific package settings"));

    private static final List<KeySpec> ENVIRONMENT_KEYS = List.of(
            scalar("sdk", "Supported Dart SDK versions"),
            scalar("flutter", "Supported Flutter SDK versions"));

    private static final List<KeySpec> FLUTTER_KEYS = List.of(
            bool("uses-material-design", "Include the Material icon font", "true"),
            bool("generate", "Enable generated localization sources", "true"),
            list("assets", "Asset files and directories"),
            list("licenses", "Additional license files"),
            list("shaders", "Fragment shader files"),
            list("fonts", "Custom font families"),
            mapping("config", "Flutter tool configuration"));

    private static final List<KeySpec> DEPENDENCY_DESCRIPTOR_KEYS = List.of(
            scalar("path", "Local package directory"),
            mapping("git", "Git package source"),
            mapping("hosted", "Hosted package source"),
            scalar("sdk", "SDK package source", "flutter"),
            scalar("version", "Allowed package versions"));

    private static final List<KeySpec> GIT_KEYS = List.of(
            scalar("url", "Git repository URL"),
            scalar("ref", "Git branch, tag, or commit"),
            scalar("path", "Package path inside the repository"));

    private static final List<KeySpec> HOSTED_KEYS = List.of(
            scalar("name", "Package name on the hosted repository"),
            scalar("url", "Hosted package repository URL"));

    private static final List<KeySpec> FONT_FAMILY_KEYS = List.of(
            scalar("family", "Flutter font family name"),
            list("fonts", "Font files in this family"));

    private static final List<KeySpec> FONT_ASSET_KEYS = List.of(
            scalar("asset", "Font file path"),
            scalar("weight", "Font weight from 100 to 900"),
            scalar("style", "Font style", "italic"));

    private static final List<KeySpec> SCREENSHOT_KEYS = List.of(
            scalar("description", "Screenshot description"),
            scalar("path", "Screenshot file path"));

    private static final List<KeySpec> FLUTTER_CONFIG_KEYS = List.of(
            bool("enable-swift-package-manager", "Enable Swift Package Manager", "true"));

    private final LocalPackageScanner localPackageScanner;

    public PubspecCompletionEngine() {
        this(new LocalPackageScanner());
    }

    PubspecCompletionEngine(LocalPackageScanner localPackageScanner) {
        this.localPackageScanner = Objects.requireNonNull(localPackageScanner, "localPackageScanner");
    }

    public List<Suggestion> complete(
            String source,
            int caretOffset,
            Path projectRoot,
            Path packageRoot,
            BooleanSupplier cancelled) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(cancelled, "cancelled");
        if (cancelled.getAsBoolean() || source.length() > PubspecValidator.MAX_DOCUMENT_LENGTH) {
            return List.of();
        }
        int caret = Math.max(0, Math.min(caretOffset, source.length()));
        CompletionContext context = CompletionContext.at(source, caret);
        if (context == null || cancelled.getAsBoolean()) {
            return List.of();
        }

        List<Suggestion> suggestions = new ArrayList<>();
        List<KeySpec> keySpecs = keySpecs(context.path(), context.listItem());
        Set<String> existingKeys = context.path().contains("*")
                ? Set.of()
                : existingKeys(source, context.path(), context.currentLineStart());
        for (KeySpec spec : keySpecs) {
            if (cancelled.getAsBoolean()) {
                return List.of();
            }
            if (!existingKeys.contains(spec.key()) && spec.key().startsWith(context.prefix())) {
                suggestions.add(spec.toSuggestion(context));
            }
        }

        List<String> normalizedPath = normalize(context.path());
        if (normalizedPath.size() == 1
                && DEPENDENCY_SECTIONS.contains(normalizedPath.get(0))) {
            addFlutterSdkPackages(normalizedPath.get(0), context, existingKeys, suggestions);
            if (!cancelled.getAsBoolean()) {
                for (LocalPackageScanner.LocalPackage localPackage
                        : localPackageScanner.scan(projectRoot, packageRoot, cancelled)) {
                    if (cancelled.getAsBoolean()) {
                        return List.of();
                    }
                    if (existingKeys.contains(localPackage.name())
                            || !localPackage.name().startsWith(context.prefix())) {
                        continue;
                    }
                    String insertion = localPackage.name() + ":"
                            + context.newline()
                            + " ".repeat(context.logicalIndent() + 2)
                            + "path: " + yamlQuote(localPackage.relativePath());
                    suggestions.add(new Suggestion(
                            localPackage.name(),
                            insertion,
                            "Local package · " + localPackage.relativePath(),
                            5,
                            context.replaceStart(),
                            context.replaceEnd()));
                }
            }
        }
        suggestions.sort((left, right) -> {
            int priority = Integer.compare(left.priority(), right.priority());
            return priority != 0 ? priority : left.label().compareTo(right.label());
        });
        return List.copyOf(suggestions);
    }

    private static List<KeySpec> keySpecs(List<String> rawPath, boolean currentListItem) {
        List<String> path = normalize(rawPath);
        if (path.isEmpty()) {
            return ROOT_KEYS;
        }
        if (path.equals(List.of("environment"))) {
            return ENVIRONMENT_KEYS;
        }
        if (path.equals(List.of("flutter"))) {
            return FLUTTER_KEYS;
        }
        if (path.equals(List.of("flutter", "config"))) {
            return FLUTTER_CONFIG_KEYS;
        }
        if (path.equals(List.of("flutter", "fonts"))
                && (currentListItem || rawPath.contains("*"))) {
            return FONT_FAMILY_KEYS;
        }
        if (path.equals(List.of("flutter", "fonts", "fonts"))
                && (currentListItem || rawPath.contains("*"))) {
            return FONT_ASSET_KEYS;
        }
        if (path.equals(List.of("screenshots"))
                && (currentListItem || rawPath.contains("*"))) {
            return SCREENSHOT_KEYS;
        }
        if (path.size() == 2 && DEPENDENCY_SECTIONS.contains(path.get(0))) {
            return DEPENDENCY_DESCRIPTOR_KEYS;
        }
        if (path.size() == 3
                && DEPENDENCY_SECTIONS.contains(path.get(0))
                && "git".equals(path.get(2))) {
            return GIT_KEYS;
        }
        if (path.size() == 3
                && DEPENDENCY_SECTIONS.contains(path.get(0))
                && "hosted".equals(path.get(2))) {
            return HOSTED_KEYS;
        }
        return List.of();
    }

    private static void addFlutterSdkPackages(
            String section,
            CompletionContext context,
            Set<String> existingKeys,
            List<Suggestion> suggestions) {
        Map<String, String> packages = new LinkedHashMap<>();
        if ("dependencies".equals(section)) {
            packages.put("flutter", "Core Flutter SDK package");
            packages.put("flutter_localizations", "Flutter localization SDK package");
        } else if ("dev_dependencies".equals(section)) {
            packages.put("flutter_test", "Flutter test SDK package");
            packages.put("integration_test", "Flutter integration test SDK package");
        }
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            String name = entry.getKey();
            if (existingKeys.contains(name) || !name.startsWith(context.prefix())) {
                continue;
            }
            String insertion = name + ":"
                    + context.newline()
                    + " ".repeat(context.logicalIndent() + 2)
                    + "sdk: flutter";
            suggestions.add(new Suggestion(
                    name,
                    insertion,
                    entry.getValue(),
                    3,
                    context.replaceStart(),
                    context.replaceEnd()));
        }
    }

    private static Set<String> existingKeys(
            String source,
            List<String> targetPath,
            int currentLineStart) {
        Set<String> keys = new HashSet<>();
        Deque<StackEntry> stack = new ArrayDeque<>();
        int offset = 0;
        for (String line : source.split("\\r?\\n", -1)) {
            if (offset != currentLineStart) {
                ParsedLine parsed = ParsedLine.parse(line, stack);
                if (parsed != null
                        && parsed.parentPath().equals(targetPath)
                        && !targetPath.contains("*")) {
                    keys.add(parsed.key());
                }
            } else {
                prepareForIndent(stack, leadingSpaces(line));
            }
            offset += line.length() + newlineWidthAt(source, offset + line.length());
        }
        return keys;
    }

    private static int newlineWidthAt(String source, int offset) {
        if (offset >= source.length()) {
            return 0;
        }
        return source.charAt(offset) == '\r'
                && offset + 1 < source.length()
                && source.charAt(offset + 1) == '\n'
                ? 2
                : 1;
    }

    private static List<String> normalize(List<String> path) {
        return path.stream().filter(part -> !"*".equals(part)).toList();
    }

    private static String yamlQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static KeySpec scalar(String key, String detail) {
        return scalar(key, detail, "");
    }

    private static KeySpec scalar(String key, String detail, String defaultValue) {
        return new KeySpec(key, detail, ValueKind.SCALAR, defaultValue);
    }

    private static KeySpec bool(String key, String detail, String defaultValue) {
        return new KeySpec(key, detail, ValueKind.SCALAR, defaultValue);
    }

    private static KeySpec mapping(String key, String detail) {
        return new KeySpec(key, detail, ValueKind.MAPPING, "");
    }

    private static KeySpec list(String key, String detail) {
        return new KeySpec(key, detail, ValueKind.LIST, "");
    }

    public record Suggestion(
            String label,
            String insertText,
            String detail,
            int priority,
            int replaceStart,
            int replaceEnd) {
    }

    private enum ValueKind {
        SCALAR,
        MAPPING,
        LIST
    }

    private record KeySpec(String key, String detail, ValueKind kind, String defaultValue) {
        Suggestion toSuggestion(CompletionContext context) {
            String insertion = switch (kind) {
                case SCALAR -> key + ": " + defaultValue;
                case MAPPING -> key + ":"
                        + context.newline()
                        + " ".repeat(context.logicalIndent() + 2);
                case LIST -> key + ":"
                        + context.newline()
                        + " ".repeat(context.logicalIndent() + 2)
                        + "- ";
            };
            return new Suggestion(
                    key,
                    insertion,
                    detail,
                    10,
                    context.replaceStart(),
                    context.replaceEnd());
        }
    }

    private record StackEntry(int indent, String key) {
    }

    private record ParsedLine(List<String> parentPath, String key) {
        static ParsedLine parse(String line, Deque<StackEntry> stack) {
            int indent = leadingSpaces(line);
            if (indent < 0) {
                return null;
            }
            String content = line.substring(indent);
            if (content.isBlank() || content.startsWith("#") || content.startsWith("%")) {
                return null;
            }
            if (content.startsWith("---") || content.startsWith("...")) {
                stack.clear();
                return null;
            }

            boolean listItem = content.startsWith("-")
                    && (content.length() == 1 || Character.isWhitespace(content.charAt(1)));
            int logicalIndent = indent;
            if (listItem) {
                prepareForIndent(stack, indent);
                stack.addLast(new StackEntry(indent, "*"));
                content = content.substring(1).stripLeading();
                logicalIndent = indent + 2;
                if (content.isBlank()) {
                    return null;
                }
            } else {
                prepareForIndent(stack, logicalIndent);
            }

            Matcher matcher = MAPPING_ENTRY.matcher(content);
            if (!matcher.matches()) {
                return null;
            }
            List<String> parent = stack.stream().map(StackEntry::key).toList();
            String key = matcher.group(1);
            String value = matcher.group(2).strip();
            if (value.isEmpty() || value.startsWith("#")) {
                prepareForIndent(stack, logicalIndent);
                stack.addLast(new StackEntry(logicalIndent, key));
            }
            return new ParsedLine(parent, key);
        }
    }

    private record CompletionContext(
            List<String> path,
            String prefix,
            int logicalIndent,
            boolean listItem,
            int replaceStart,
            int replaceEnd,
            int currentLineStart,
            String newline) {
        static CompletionContext at(String source, int caret) {
            int lineStart = source.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
            int physicalLineStart = lineStart;
            int indent = 0;
            while (lineStart + indent < caret && source.charAt(lineStart + indent) == ' ') {
                indent++;
            }
            if (lineStart + indent < caret && source.charAt(lineStart + indent) == '\t') {
                return null;
            }
            String content = source.substring(lineStart + indent, caret);
            if (content.stripLeading().startsWith("#")) {
                return null;
            }
            boolean listItem = content.startsWith("-")
                    && (content.length() == 1 || Character.isWhitespace(content.charAt(1)));
            int contentStart = lineStart + indent;
            int logicalIndent = indent;
            if (listItem) {
                int afterDash = contentStart + 1;
                while (afterDash < caret && Character.isWhitespace(source.charAt(afterDash))) {
                    afterDash++;
                }
                contentStart = afterDash;
                content = source.substring(contentStart, caret);
                logicalIndent = indent + 2;
            }
            if (!content.matches("[A-Za-z0-9_-]*")) {
                return null;
            }

            Deque<StackEntry> stack = new ArrayDeque<>();
            int offset = 0;
            String before = source.substring(0, physicalLineStart);
            for (String line : before.split("\\r?\\n", -1)) {
                if (offset >= physicalLineStart) {
                    break;
                }
                ParsedLine.parse(line, stack);
                offset += line.length() + newlineWidthAt(source, offset + line.length());
            }
            prepareForIndent(stack, listItem ? indent : logicalIndent);
            if (listItem) {
                stack.addLast(new StackEntry(indent, "*"));
            }
            List<String> path = stack.stream().map(StackEntry::key).toList();
            String newline = source.contains("\r\n") ? "\r\n" : "\n";
            return new CompletionContext(
                    path,
                    content,
                    logicalIndent,
                    listItem,
                    contentStart,
                    caret,
                    physicalLineStart,
                    newline);
        }
    }

    private static void prepareForIndent(Deque<StackEntry> stack, int indent) {
        while (!stack.isEmpty() && stack.peekLast().indent() >= indent) {
            stack.removeLast();
        }
    }

    private static int leadingSpaces(String line) {
        int spaces = 0;
        while (spaces < line.length()) {
            char current = line.charAt(spaces);
            if (current == ' ') {
                spaces++;
            } else if (current == '\t') {
                return -1;
            } else {
                break;
            }
        }
        return spaces;
    }
}

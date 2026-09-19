package io.github.vgrytsenko2022.designer.move;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Pure fail-closed gate for path-sensitive Dart directives before moving a
 * Flutter Designer pair to another mirrored directory.
 */
public final class DesignerPairMoveDependencyPlanner {
    private static final Pattern PACKAGE_NAME = Pattern.compile("[a-z][a-z0-9_]*");
    private static final int MAX_PATH_CHARACTERS = 4096;

    private final DartMoveDependencyLimits limits;
    private final DartDirectiveScanner scanner;

    public DesignerPairMoveDependencyPlanner() {
        this(DartMoveDependencyLimits.defaults());
    }

    public DesignerPairMoveDependencyPlanner(DartMoveDependencyLimits limits) {
        this(limits, new DartDirectiveScanner(limits));
    }

    DesignerPairMoveDependencyPlanner(
            DartMoveDependencyLimits limits,
            DartDirectiveScanner scanner) {
        this.limits = Objects.requireNonNull(limits, "limits");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        if (!limits.equals(scanner.limits())) {
            throw new IllegalArgumentException(
                    "Planner and scanner must share one limits policy");
        }
    }

    public DartMoveDependencyLimits limits() {
        return limits;
    }

    /**
     * Checks an exact project Dart-source inventory.
     *
     * @param packageName current pubspec package name
     * @param originalLibRelativePath current path below {@code lib/}
     * @param targetLibRelativePath target path below {@code lib/}
     * @param sources exact project-relative Dart source snapshots
     */
    public DesignerPairMoveDependencyResult prepare(
            String packageName,
            String originalLibRelativePath,
            String targetLibRelativePath,
            Collection<DartMoveSourceSnapshot> sources) {
        Objects.requireNonNull(sources, "sources");
        if (!validPackageName(packageName)) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.INVALID_PACKAGE_NAME,
                    "",
                    -1,
                    "",
                    "Flutter Designer Move requires the exact canonical pubspec "
                    + "package name before checking Dart dependencies.");
        }

        String original = validatedLibPath(originalLibRelativePath);
        if (original == null) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.INVALID_ORIGINAL_PATH,
                    "",
                    -1,
                    "",
                    "Flutter Designer Move rejected original lib-relative path '"
                    + display(originalLibRelativePath) + "': it is not normalized.");
        }
        String target = validatedLibPath(targetLibRelativePath);
        if (target == null) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.INVALID_TARGET_PATH,
                    "",
                    -1,
                    "",
                    "Flutter Designer Move rejected target lib-relative path '"
                    + display(targetLibRelativePath) + "': it is not normalized.");
        }
        if (samePathIdentity(original, target)) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.SAME_PATH,
                    "lib/" + original,
                    -1,
                    "",
                    "Flutter Designer Move requires a different target directory; "
                    + "lib/" + target + " aliases lib/" + original
                    + " under normalized cross-platform path identity.");
        }
        if (!fileName(original).equals(fileName(target))) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.TARGET_FILENAME_CHANGED,
                    "lib/" + original,
                    -1,
                    "",
                    "Flutter Designer Move may change the mirrored directory only; "
                    + "the Dart filename must remain '" + fileName(original) + "'.");
        }
        if (sources.size() > limits.maxSourceFiles()) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.TOO_MANY_SOURCES,
                    "",
                    -1,
                    "",
                    "Flutter Designer Move dependency inventory contains "
                    + sources.size() + " Dart files; the limit is "
                    + limits.maxSourceFiles() + ".");
        }

        Map<String, DartMoveSourceSnapshot> byPath = new TreeMap<>();
        Map<String, String> byIdentity = new TreeMap<>();
        long totalBytes = 0;
        for (DartMoveSourceSnapshot source : sources) {
            Objects.requireNonNull(source, "sources contains null");
            String path = validatedProjectDartPath(source.projectRelativePath());
            if (path == null) {
                return rejected(
                        DartMoveDependencyDiagnostic.Code.INVALID_SOURCE_PATH,
                        display(source.projectRelativePath()),
                        -1,
                        "",
                        "Flutter Designer Move rejected Dart inventory path '"
                        + display(source.projectRelativePath())
                        + "': project-relative paths must be normalized and end in .dart.");
            }
            if (source.size() > limits.maxSourceBytes()) {
                return rejected(
                        DartMoveDependencyDiagnostic.Code.SOURCE_TOO_LARGE,
                        path,
                        -1,
                        "",
                        "Flutter Designer Move cannot inspect " + path + ": "
                        + source.size() + " bytes exceeds the per-file limit of "
                        + limits.maxSourceBytes() + ".");
            }
            if (byPath.putIfAbsent(path, source) != null) {
                return rejected(
                        DartMoveDependencyDiagnostic.Code.DUPLICATE_SOURCE_PATH,
                        path,
                        -1,
                        "",
                        "Flutter Designer Move dependency inventory contains "
                        + "duplicate source path " + path + ".");
            }
            String identity = pathIdentity(path);
            String aliasedPath = byIdentity.putIfAbsent(identity, path);
            if (aliasedPath != null) {
                return rejected(
                        DartMoveDependencyDiagnostic.Code.DUPLICATE_SOURCE_PATH,
                        path,
                        -1,
                        "",
                        "Flutter Designer Move dependency inventory paths "
                        + aliasedPath + " and " + path
                        + " alias under normalized cross-platform path identity.");
            }
            if (Long.MAX_VALUE - totalBytes < source.size()) {
                totalBytes = Long.MAX_VALUE;
            } else {
                totalBytes += source.size();
            }
            if (totalBytes > limits.maxTotalSourceBytes()) {
                return rejected(
                        DartMoveDependencyDiagnostic.Code.PROJECT_TOO_LARGE,
                        path,
                        -1,
                        "",
                        "Flutter Designer Move Dart inventory exceeds the total "
                        + "scan limit of " + limits.maxTotalSourceBytes() + " bytes.");
            }
        }

        String originalProjectPath = "lib/" + original;
        if (!byPath.containsKey(originalProjectPath)) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.MOVED_SOURCE_MISSING,
                    originalProjectPath,
                    -1,
                    "",
                    "Flutter Designer Move dependency inventory does not contain "
                    + "the exact moved Dart source " + originalProjectPath + ".");
        }
        String targetProjectPath = "lib/" + target;
        String occupiedTarget = byPath.keySet().stream()
                .filter(path -> samePathIdentity(path, targetProjectPath))
                .findFirst()
                .orElse(null);
        if (occupiedTarget != null) {
            return rejected(
                    DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                    occupiedTarget,
                    -1,
                    "",
                    "Flutter Designer Move cannot publish " + targetProjectPath
                    + ": Dart inventory path " + occupiedTarget
                    + " already aliases that destination under normalized "
                    + "cross-platform path identity.");
        }

        int directiveCount = 0;
        for (Map.Entry<String, DartMoveSourceSnapshot> entry : byPath.entrySet()) {
            DartDirectiveScanResult scan = scanner.scan(entry.getValue());
            if (scan instanceof DartDirectiveScanResult.Rejected rejected) {
                return new DesignerPairMoveDependencyResult.Rejected(
                        rejected.diagnostic());
            }
            List<DartDirectiveReference> references =
                    ((DartDirectiveScanResult.Parsed) scan).references();
            directiveCount = Math.addExact(directiveCount, references.size());
            for (DartDirectiveReference reference : references) {
                DartMoveDependencyDiagnostic unsafe = inspectReference(
                        packageName,
                        original,
                        target,
                        originalProjectPath,
                        targetProjectPath,
                        reference);
                if (unsafe != null) {
                    return new DesignerPairMoveDependencyResult.Rejected(unsafe);
                }
            }
        }

        return new DesignerPairMoveDependencyResult.Safe(
                new DesignerPairMoveDependencyPlan(
                        packageName,
                        original,
                        target,
                        new ArrayList<>(byPath.keySet()),
                        totalBytes,
                        directiveCount));
    }

    private DartMoveDependencyDiagnostic inspectReference(
            String packageName,
            String original,
            String target,
            String originalProjectPath,
            String targetProjectPath,
            DartDirectiveReference reference) {
        String raw = reference.uri();
        if (raw.indexOf('\\') >= 0
                || raw.indexOf('%') >= 0
                || raw.indexOf('?') >= 0
                || raw.indexOf('#') >= 0) {
            return unsupported(reference,
                    "URI contains a backslash, query, fragment, or percent-encoded "
                    + "component, so its filesystem target is not unambiguous.");
        }

        URI uri;
        try {
            uri = new URI(raw);
        } catch (URISyntaxException malformed) {
            return unsupported(reference, "URI is not syntactically valid.");
        }
        if (uri.getRawFragment() != null || uri.getRawQuery() != null) {
            return unsupported(reference,
                    "URI queries and fragments are not supported by the move gate.");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            String resolved = resolveProjectRelative(
                    reference.sourceProjectRelativePath(), uri.getRawPath());
            if (resolved != null
                    && samePathIdentity(resolved, targetProjectPath)) {
                return targetBindingChange(reference, targetProjectPath);
            }
            if (reference.sourceProjectRelativePath().equals(originalProjectPath)) {
                return diagnostic(
                        DartMoveDependencyDiagnostic.Code.OUTGOING_RELATIVE_DIRECTIVE,
                        reference,
                        "Flutter Designer Move from lib/" + original + " to lib/"
                        + target + " is unsafe: moved source " + originalProjectPath
                        + " has relative "
                        + reference.kind().name().toLowerCase(Locale.ROOT)
                        + " URI '" + raw + "', whose base directory would change.");
            }
            if (resolved == null) {
                return unsupported(reference,
                        "relative URI escapes the project or is not normalized safely.");
            }
            if (samePathIdentity(resolved, originalProjectPath)) {
                return incoming(reference, originalProjectPath);
            }
            return null;
        }

        if (scheme.equals("dart")) {
            return uri.isOpaque() && !uri.getRawSchemeSpecificPart().isBlank()
                    ? null
                    : unsupported(reference, "dart: URI has no library name.");
        }
        if (!scheme.equals("package")) {
            return unsupported(reference,
                    "URI scheme '" + scheme + "' is not stable for a project file move.");
        }

        String packageTarget = validatedPackageTarget(uri.getRawSchemeSpecificPart());
        if (packageTarget == null) {
            return unsupported(reference,
                    "package: URI must contain a canonical package name and normalized path.");
        }
        int slash = packageTarget.indexOf('/');
        String referencedPackage = packageTarget.substring(0, slash);
        String libPath = packageTarget.substring(slash + 1);
        if (referencedPackage.equals(packageName)) {
            String resolvedProjectPath = "lib/" + libPath;
            if (samePathIdentity(resolvedProjectPath, targetProjectPath)) {
                return targetBindingChange(reference, targetProjectPath);
            }
            if (samePathIdentity(resolvedProjectPath, originalProjectPath)) {
                return incoming(reference, originalProjectPath);
            }
        }
        return null;
    }

    private static DartMoveDependencyDiagnostic targetBindingChange(
            DartDirectiveReference reference,
            String targetProjectPath) {
        return diagnostic(
                DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                reference,
                "Flutter Designer Move is unsafe: "
                + reference.sourceProjectRelativePath() + " has "
                + reference.kind().name().toLowerCase(Locale.ROOT) + " URI '"
                + reference.uri() + "' which aliases Move destination "
                + targetProjectPath + " under normalized cross-platform path "
                + "identity and can acquire or change its binding after publication.");
    }

    private static DartMoveDependencyDiagnostic incoming(
            DartDirectiveReference reference,
            String originalProjectPath) {
        return diagnostic(
                DartMoveDependencyDiagnostic.Code.INCOMING_REFERENCE,
                reference,
                "Flutter Designer Move is unsafe: "
                + reference.sourceProjectRelativePath() + " has "
                + reference.kind().name().toLowerCase(Locale.ROOT) + " URI '"
                + reference.uri() + "' resolving to moved source "
                + originalProjectPath + ". Update/refactor dependencies before moving.");
    }

    private static DartMoveDependencyDiagnostic unsupported(
            DartDirectiveReference reference,
            String reason) {
        return diagnostic(
                DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI,
                reference,
                "Flutter Designer Move cannot prove "
                + reference.sourceProjectRelativePath() + " safe: " + reason
                + " URI: '" + reference.uri() + "'.");
    }

    private static DartMoveDependencyDiagnostic diagnostic(
            DartMoveDependencyDiagnostic.Code code,
            DartDirectiveReference reference,
            String message) {
        return new DartMoveDependencyDiagnostic(
                code,
                reference.sourceProjectRelativePath(),
                reference.utf16Offset(),
                reference.uri(),
                message);
    }

    private static DesignerPairMoveDependencyResult.Rejected rejected(
            DartMoveDependencyDiagnostic.Code code,
            String sourcePath,
            int offset,
            String uri,
            String message) {
        return new DesignerPairMoveDependencyResult.Rejected(
                new DartMoveDependencyDiagnostic(
                        code, sourcePath, offset, uri, message));
    }

    private static String resolveProjectRelative(
            String sourceProjectPath,
            String relativeUriPath) {
        if (relativeUriPath == null
                || relativeUriPath.isEmpty()
                || relativeUriPath.startsWith("/")
                || relativeUriPath.contains("//")) {
            return null;
        }
        ArrayDeque<String> segments = new ArrayDeque<>();
        int slash = sourceProjectPath.lastIndexOf('/');
        if (slash >= 0) {
            for (String segment : sourceProjectPath.substring(0, slash).split("/")) {
                segments.addLast(segment);
            }
        }
        for (String segment : relativeUriPath.split("/", -1)) {
            if (segment.isEmpty()) {
                return null;
            }
            if (segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (segments.isEmpty()) {
                    return null;
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return String.join("/", segments);
    }

    private static String validatedPackageTarget(String value) {
        if (value == null
                || value.indexOf('?') >= 0
                || value.indexOf('#') >= 0
                || value.startsWith("/")
                || value.contains("//")) {
            return null;
        }
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1) {
            return null;
        }
        String packageName = value.substring(0, slash);
        String path = value.substring(slash + 1);
        if (!validPackageName(packageName) || !isNormalizedRelativePath(path, false)) {
            return null;
        }
        return value;
    }

    private static String validatedLibPath(String value) {
        if (!isNormalizedRelativePath(value, true) || value.startsWith("lib/")) {
            return null;
        }
        return value;
    }

    private static String validatedProjectDartPath(String value) {
        return isNormalizedRelativePath(value, true) ? value : null;
    }

    private static boolean isNormalizedRelativePath(
            String value,
            boolean requireDartExtension) {
        if (value == null
                || value.isEmpty()
                || value.length() > MAX_PATH_CHARACTERS
                || value.startsWith("/")
                || value.endsWith("/")
                || value.indexOf('\\') >= 0
                || value.indexOf(':') >= 0
                || value.indexOf('\0') >= 0
                || value.contains("//")
                || requireDartExtension && !value.endsWith(".dart")) {
            return false;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty()
                    || segment.equals(".")
                    || segment.equals("..")
                    || !segment.equals(segment.strip())) {
                return false;
            }
            for (int i = 0; i < segment.length(); i++) {
                if (Character.isISOControl(segment.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean validPackageName(String value) {
        return value != null && PACKAGE_NAME.matcher(value).matches();
    }

    private static boolean samePathIdentity(String first, String second) {
        return pathIdentity(first).equals(pathIdentity(second));
    }

    private static String pathIdentity(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        return Normalizer.normalize(
                normalized.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String display(String value) {
        return value == null ? "<null>" : value;
    }
}

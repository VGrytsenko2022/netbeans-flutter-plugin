package dev.flutter.netbeans.plugin.designer.assets;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable application- or package-scoped logical asset identity. */
public record FlutterAssetId(String packageName, String logicalPath)
        implements Comparable<FlutterAssetId> {

    private static final Pattern PACKAGE_NAME = Pattern.compile(
            "^[a-z][a-z0-9_]{0,63}$");

    public FlutterAssetId {
        if (packageName != null) {
            packageName = validatePackageName(packageName);
        }
        logicalPath = normalizeLogicalPath(logicalPath);
    }

    public static FlutterAssetId app(String logicalPath) {
        return new FlutterAssetId(null, logicalPath);
    }

    public static FlutterAssetId packageAsset(
            String packageName,
            String logicalPath) {
        return new FlutterAssetId(
                Objects.requireNonNull(packageName, "packageName"),
                logicalPath);
    }

    /** Unambiguous wire-safe name, for example {@code app:images/logo.png}. */
    public String wireName() {
        return packageName == null
                ? "app:" + logicalPath
                : "package:" + packageName + ':' + logicalPath;
    }

    /** Concrete user-facing name; deliberately identical to the stable wire name. */
    public String displayName() {
        return wireName();
    }

    @Override
    public int compareTo(FlutterAssetId other) {
        return wireName().compareTo(other.wireName());
    }

    static String normalizeLogicalPath(String value) {
        Objects.requireNonNull(value, "logicalPath");
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(
                    "logicalPath must be non-blank without surrounding whitespace");
        }
        if (value.startsWith("/") || value.startsWith("~")
                || value.indexOf('\\') >= 0 || value.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
                    "logicalPath must be a relative POSIX path");
        }
        if (value.indexOf('%') >= 0) {
            throw new IllegalArgumentException(
                    "logicalPath must not contain percent-encoded path syntax");
        }

        String[] segments = value.split("/", -1);
        StringBuilder normalized = new StringBuilder(value.length());
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(
                        "logicalPath contains an empty, dot, or traversal segment");
            }
            for (int index = 0; index < segment.length();) {
                int codePoint = segment.codePointAt(index);
                if (Character.isISOControl(codePoint)) {
                    throw new IllegalArgumentException(
                            "logicalPath contains a control character");
                }
                index += Character.charCount(codePoint);
            }
            if (!normalized.isEmpty()) {
                normalized.append('/');
            }
            normalized.append(Normalizer.normalize(segment, Normalizer.Form.NFC));
        }
        return normalized.toString();
    }

    static String validatePackageName(String value) {
        Objects.requireNonNull(value, "packageName");
        if (!PACKAGE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "packageName must be a valid lowercase Dart package name");
        }
        return value;
    }
}

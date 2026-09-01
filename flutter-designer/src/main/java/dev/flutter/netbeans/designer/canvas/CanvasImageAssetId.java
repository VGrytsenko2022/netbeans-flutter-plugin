package dev.flutter.netbeans.designer.canvas;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Stable Flutter asset-bundle key, optionally scoped to one Dart package. */
public record CanvasImageAssetId(
        Optional<String> packageName,
        String assetName) implements Comparable<CanvasImageAssetId> {
    private static final Pattern PACKAGE_NAME =
            Pattern.compile("[a-z][a-z0-9_]{0,254}");

    public CanvasImageAssetId {
        Objects.requireNonNull(packageName, "packageName");
        packageName = packageName.map(CanvasImageAssetId::requirePackageName);
        assetName = requireAssetName(assetName);
    }

    public static CanvasImageAssetId application(String assetName) {
        return new CanvasImageAssetId(Optional.empty(), assetName);
    }

    public static CanvasImageAssetId packageAsset(
            String packageName,
            String assetName) {
        return new CanvasImageAssetId(Optional.of(packageName), assetName);
    }

    /** Unambiguous diagnostic/wire identity; never a filesystem path. */
    public String externalName() {
        return packageName
                .map(value -> "package:" + value + ':' + assetName)
                .orElseGet(() -> "app:" + assetName);
    }

    @Override
    public int compareTo(CanvasImageAssetId other) {
        Objects.requireNonNull(other, "other");
        return externalName().compareTo(other.externalName());
    }

    private static String requirePackageName(String value) {
        Objects.requireNonNull(value, "packageName");
        if (!PACKAGE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Flutter package name must use lowercase Dart package syntax");
        }
        return value;
    }

    private static String requireAssetName(String value) {
        Objects.requireNonNull(value, "assetName");
        if (value.isEmpty() || value.length() > 4096
                || value.startsWith("/") || value.endsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "Flutter asset name must be a bounded relative POSIX path");
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(
                        "Flutter asset name must not contain empty, dot, or parent segments");
            }
            for (int index = 0; index < segment.length(); index++) {
                if (Character.isISOControl(segment.charAt(index))) {
                    throw new IllegalArgumentException(
                            "Flutter asset name must not contain control characters");
                }
            }
        }
        return value;
    }
}

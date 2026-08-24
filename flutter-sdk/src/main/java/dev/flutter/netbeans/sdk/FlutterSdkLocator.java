package dev.flutter.netbeans.sdk;

import dev.flutter.netbeans.api.FlutterSdk;
import java.nio.file.*;
import java.util.*;

public final class FlutterSdkLocator {
    public Optional<FlutterSdk> locate() {
        return detect().map(SdkDetection::sdk);
    }

    public Optional<SdkDetection<FlutterSdk>> detect() {
        var configured = detectHome(System.getProperty("flutter.sdk"), "JVM property flutter.sdk");
        if (configured.isPresent()) return configured;

        var flutterHome = detectHome(System.getenv("FLUTTER_HOME"), "FLUTTER_HOME");
        if (flutterHome.isPresent()) return flutterHome;

        var flutterRoot = detectHome(System.getenv("FLUTTER_ROOT"), "FLUTTER_ROOT");
        if (flutterRoot.isPresent()) return flutterRoot;

        return fromPath().map(sdk -> new SdkDetection<>(sdk, "PATH"));
    }

    public Optional<FlutterSdk> fromHome(Path home) {
        if (home == null) return Optional.empty();
        Path normalizedHome = home.toAbsolutePath().normalize();
        Path executable = normalizedHome.resolve("bin").resolve(isWindows() ? "flutter.bat" : "flutter");
        return isUsableExecutable(executable)
                ? Optional.of(new FlutterSdk(normalizedHome, executable))
                : Optional.empty();
    }

    private Optional<SdkDetection<FlutterSdk>> detectHome(String value, String source) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return fromHome(Path.of(cleanPathText(value))).map(sdk -> new SdkDetection<>(sdk, source));
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    private Optional<FlutterSdk> fromPath() {
        String path = System.getenv("PATH");
        if (path == null) return Optional.empty();
        String executable = isWindows() ? "flutter.bat" : "flutter";
        for (String entry : path.split(java.io.File.pathSeparator)) {
            try {
                String cleanEntry = entry.strip().replaceAll("^\"|\"$", "");
                Path candidate = Path.of(cleanEntry).resolve(executable);
                if (!isUsableExecutable(candidate)) continue;

                Path resolvedExecutable = candidate.toRealPath();
                Path bin = resolvedExecutable.getParent();
                if (bin == null || bin.getParent() == null || bin.getFileName() == null
                        || !"bin".equalsIgnoreCase(bin.getFileName().toString())) continue;

                // Only persist a root that passes the same validation used for
                // saved settings. Wrapper/shim directories are not SDK homes.
                Optional<FlutterSdk> sdk = fromHome(bin.getParent());
                if (sdk.isPresent()) return sdk;
            } catch (InvalidPathException | java.io.IOException ex) {
                // Ignore malformed PATH entries and continue discovery.
            }
        }
        return Optional.empty();
    }

    private static boolean isUsableExecutable(Path executable) {
        return Files.isRegularFile(executable) && (isWindows() || Files.isExecutable(executable));
    }

    private static String cleanPathText(String value) {
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return cleaned;
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}

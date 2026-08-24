package dev.flutter.netbeans.sdk;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.api.FlutterSdk;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

public final class DartSdkLocator {
    public Optional<DartSdk> locate() {
        return detect(Optional.empty()).map(SdkDetection::sdk);
    }

    public Optional<DartSdk> locate(FlutterSdk flutterSdk) {
        return detect(Optional.ofNullable(flutterSdk)).map(SdkDetection::sdk);
    }

    public Optional<SdkDetection<DartSdk>> detect(Optional<FlutterSdk> flutterSdk) {
        var configured = detectConfigured();
        if (configured.isPresent()) return configured;

        if (flutterSdk.isPresent()) {
            var bundled = fromFlutterSdk(flutterSdk.get());
            if (bundled.isPresent()) {
                return bundled.map(sdk -> new SdkDetection<>(sdk, "Dart SDK bundled with Flutter"));
            }
        }

        return fromPath(true).map(sdk -> new SdkDetection<>(sdk, "PATH"));
    }

    public Optional<SdkDetection<DartSdk>> detectStandalone() {
        var configured = detectConfigured();
        if (configured.isPresent()) return configured;
        return fromPath(false).map(sdk -> new SdkDetection<>(sdk, "PATH"));
    }

    public Optional<DartSdk> fromHome(Path home) {
        if (home == null) return Optional.empty();
        Path normalizedHome = home.toAbsolutePath().normalize();
        for (String executableName : executableNames()) {
            Path executable = normalizedHome.resolve("bin").resolve(executableName);
            if (isUsableExecutable(executable)) {
                return Optional.of(new DartSdk(normalizedHome, executable));
            }
        }
        return Optional.empty();
    }

    public Optional<DartSdk> fromFlutterSdk(FlutterSdk flutterSdk) {
        if (flutterSdk == null) return Optional.empty();
        Path bundledHome = flutterSdk.home().resolve("bin").resolve("cache").resolve("dart-sdk");
        return fromHome(bundledHome);
    }

    private Optional<SdkDetection<DartSdk>> detectHome(String value, String source) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return fromHome(Path.of(cleanPathText(value))).map(sdk -> new SdkDetection<>(sdk, source));
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    private Optional<SdkDetection<DartSdk>> detectConfigured() {
        var configured = detectHome(System.getProperty("dart.sdk"), "JVM property dart.sdk");
        if (configured.isPresent()) return configured;

        var dartHome = detectHome(System.getenv("DART_HOME"), "DART_HOME");
        if (dartHome.isPresent()) return dartHome;

        return detectHome(System.getenv("DART_SDK"), "DART_SDK");
    }

    private Optional<DartSdk> fromPath(boolean allowFlutterWrapper) {
        String pathValue = System.getenv("PATH");
        if (pathValue == null) return Optional.empty();

        for (String entry : pathValue.split(File.pathSeparator)) {
            try {
                String cleanEntry = entry.strip().replaceAll("^\"|\"$", "");
                Path bin = Path.of(cleanEntry).toAbsolutePath().normalize();
                for (String executableName : executableNames()) {
                    Path candidate = bin.resolve(executableName);
                    if (!isUsableExecutable(candidate)) continue;

                    Path executable = candidate.toRealPath();
                    Path resolvedBin = executable.getParent();
                    if (resolvedBin == null || resolvedBin.getParent() == null
                            || resolvedBin.getFileName() == null
                            || !"bin".equalsIgnoreCase(resolvedBin.getFileName().toString())) continue;

                    if (isFlutterBin(resolvedBin)) {
                        if (!allowFlutterWrapper) continue;
                        Path bundledHome = resolvedBin.resolve("cache").resolve("dart-sdk");
                        var bundled = fromHome(bundledHome);
                        if (bundled.isPresent()) return bundled;
                        continue;
                    }

                    Optional<DartSdk> sdk = fromHome(resolvedBin.getParent());
                    if (sdk.isPresent()) return sdk;
                }
            } catch (InvalidPathException | java.io.IOException ex) {
                // Ignore malformed PATH entries and continue discovery.
            }
        }
        return Optional.empty();
    }

    private static String[] executableNames() {
        return FlutterSdkLocator.isWindows()
                ? new String[]{"dart.exe", "dart.bat"}
                : new String[]{"dart"};
    }

    private static boolean isFlutterBin(Path bin) {
        String flutterExecutable = FlutterSdkLocator.isWindows() ? "flutter.bat" : "flutter";
        return isUsableExecutable(bin.resolve(flutterExecutable));
    }

    private static boolean isUsableExecutable(Path executable) {
        return Files.isRegularFile(executable)
                && (FlutterSdkLocator.isWindows() || Files.isExecutable(executable));
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
}

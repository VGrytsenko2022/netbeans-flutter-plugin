package dev.flutter.netbeans.plugin.settings;

import dev.flutter.netbeans.api.DartSdk;
import dev.flutter.netbeans.api.FlutterSdk;
import dev.flutter.netbeans.sdk.DartSdkLocator;
import dev.flutter.netbeans.sdk.FlutterSdkLocator;
import dev.flutter.netbeans.sdk.SdkDetection;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

public final class FlutterToolchainService {
    private final FlutterSettings settings;
    private final FlutterSdkLocator flutterLocator;
    private final DartSdkLocator dartLocator;

    public FlutterToolchainService() {
        this(FlutterSettings.getDefault(), new FlutterSdkLocator(), new DartSdkLocator());
    }

    FlutterToolchainService(
            FlutterSettings settings,
            FlutterSdkLocator flutterLocator,
            DartSdkLocator dartLocator) {
        this.settings = settings;
        this.flutterLocator = flutterLocator;
        this.dartLocator = dartLocator;
    }

    public FlutterToolchainStatus resolve() {
        return resolve(settings.load());
    }

    public FlutterToolchainStatus resolve(FlutterToolchainConfig config) {
        Resolution<FlutterSdk> flutter = resolveFlutter(config.flutterHome());
        Resolution<DartSdk> dart = resolveDart(config, flutter.sdk());

        boolean flutterExplicitValid = config.flutterHome().isBlank() || flutter.sdk().isPresent();
        boolean dartExplicitValid = config.useBundledDart()
                ? config.flutterHome().isBlank() || dart.sdk().isPresent()
                : config.dartHome().isBlank() || dart.sdk().isPresent();

        return new FlutterToolchainStatus(
                flutter.sdk(),
                dart.sdk(),
                flutter.message(),
                dart.message(),
                flutterExplicitValid && dartExplicitValid);
    }

    private Resolution<FlutterSdk> resolveFlutter(String configuredHome) {
        if (!configuredHome.isBlank()) {
            try {
                Path home = pathFromText(configuredHome);
                var sdk = flutterLocator.fromHome(home);
                if (sdk.isPresent()) {
                    FlutterSdk value = sdk.get();
                    return new Resolution<>(sdk, "Flutter SDK is ready at " + value.home()
                            + " (executable: " + value.flutterExecutable() + "; source: saved setting).");
                }
                return new Resolution<>(Optional.empty(), "Flutter SDK at " + configuredHome
                        + " is invalid: " + expectedFlutterExecutable(home) + " was not found.");
            } catch (InvalidPathException ex) {
                return new Resolution<>(Optional.empty(), "Flutter SDK path is invalid: "
                        + configuredHome + " (" + ex.getReason() + ").");
            }
        }

        Optional<SdkDetection<FlutterSdk>> detection = flutterLocator.detect();
        if (detection.isPresent()) {
            SdkDetection<FlutterSdk> value = detection.get();
            return new Resolution<>(Optional.of(value.sdk()), "Flutter SDK was detected at "
                    + value.sdk().home() + " (source: " + value.source() + ").");
        }
        return new Resolution<>(Optional.empty(), "Flutter SDK was not detected from flutter.sdk, "
                + "FLUTTER_HOME, FLUTTER_ROOT, or PATH. Choose the Flutter SDK folder manually.");
    }

    private Resolution<DartSdk> resolveDart(
            FlutterToolchainConfig config,
            Optional<FlutterSdk> flutterSdk) {
        if (config.useBundledDart()) {
            if (flutterSdk.isEmpty()) {
                return new Resolution<>(Optional.empty(), "Dart SDK cannot be resolved from Flutter: "
                        + "a valid Flutter SDK is not available.");
            }
            var dart = dartLocator.fromFlutterSdk(flutterSdk.get());
            if (dart.isPresent()) {
                DartSdk value = dart.get();
                return new Resolution<>(dart, "Dart SDK bundled with Flutter is ready at "
                        + value.home() + " (executable: " + value.dartExecutable() + ").");
            }
            Path expectedHome = flutterSdk.get().home().resolve("bin").resolve("cache").resolve("dart-sdk");
            return new Resolution<>(Optional.empty(), "Dart SDK bundled with Flutter is unavailable: "
                    + expectedDartExecutable(expectedHome) + " was not found.");
        }

        if (!config.dartHome().isBlank()) {
            try {
                Path home = pathFromText(config.dartHome());
                var dart = dartLocator.fromHome(home);
                if (dart.isPresent()) {
                    DartSdk value = dart.get();
                    return new Resolution<>(dart, "Dart SDK is ready at " + value.home()
                            + " (executable: " + value.dartExecutable() + "; source: saved setting).");
                }
                return new Resolution<>(Optional.empty(), "Dart SDK at " + config.dartHome()
                        + " is invalid: " + expectedDartExecutable(home) + " was not found.");
            } catch (InvalidPathException ex) {
                return new Resolution<>(Optional.empty(), "Dart SDK path is invalid: "
                        + config.dartHome() + " (" + ex.getReason() + ").");
            }
        }

        Optional<SdkDetection<DartSdk>> detection = dartLocator.detectStandalone();
        if (detection.isPresent()) {
            SdkDetection<DartSdk> value = detection.get();
            return new Resolution<>(Optional.of(value.sdk()), "Dart SDK was detected at "
                    + value.sdk().home() + " (source: " + value.source() + ").");
        }
        return new Resolution<>(Optional.empty(), "Dart SDK was not detected from dart.sdk, "
                + "DART_HOME, DART_SDK, Flutter, or PATH. Choose the Dart SDK folder manually.");
    }

    private static Path expectedFlutterExecutable(Path home) {
        return home.toAbsolutePath().normalize().resolve("bin")
                .resolve(isWindows() ? "flutter.bat" : "flutter");
    }

    private static Path expectedDartExecutable(Path home) {
        return home.toAbsolutePath().normalize().resolve("bin")
                .resolve(isWindows() ? "dart.exe" : "dart");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Path pathFromText(String value) {
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return Path.of(cleaned);
    }

    private record Resolution<T>(Optional<T> sdk, String message) {
    }
}

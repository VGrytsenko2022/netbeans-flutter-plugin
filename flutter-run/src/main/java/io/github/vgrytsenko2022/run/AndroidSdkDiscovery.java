package io.github.vgrytsenko2022.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vgrytsenko2022.api.ProcessResult;
import io.github.vgrytsenko2022.sdk.FlutterCli;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** Detects an Android SDK without mutating the user's Flutter or Android configuration. */
public final class AndroidSdkDiscovery {
    private static final Duration FLUTTER_CONFIG_TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, String> environment;
    private final Properties properties;
    private final FlutterCommandExecutor flutterCommand;

    public AndroidSdkDiscovery() {
        this(System.getenv(), System.getProperties(), null);
    }

    public AndroidSdkDiscovery(FlutterCli flutterCli) {
        this(System.getenv(), System.getProperties(),
                Objects.requireNonNull(flutterCli, "flutterCli")::execute);
    }

    AndroidSdkDiscovery(
            Map<String, String> environment,
            Properties properties,
            FlutterCommandExecutor flutterCommand) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
        this.properties = new Properties();
        this.properties.putAll(Objects.requireNonNull(properties, "properties"));
        this.flutterCommand = flutterCommand;
    }

    /**
     * Detects the SDK in deterministic precedence order: JVM property, Android environment,
     * then Flutter's persisted {@code android-sdk} setting.
     */
    public Optional<AndroidSdkInstallation> detect() throws InterruptedException {
        Optional<AndroidSdkInstallation> property = firstConfigured(
                List.of(
                        new ConfiguredPath(properties.getProperty("android.sdk"),
                                "JVM property android.sdk"),
                        new ConfiguredPath(properties.getProperty("android.sdk.path"),
                                "JVM property android.sdk.path")));
        if (property.isPresent()) {
            return property;
        }

        Optional<AndroidSdkInstallation> androidEnvironment = firstConfigured(
                List.of(
                        new ConfiguredPath(environment.get("ANDROID_SDK_ROOT"),
                                "ANDROID_SDK_ROOT"),
                        new ConfiguredPath(environment.get("ANDROID_HOME"), "ANDROID_HOME")));
        if (androidEnvironment.isPresent()) {
            return androidEnvironment;
        }
        Optional<AndroidSdkInstallation> flutter = fromFlutterConfig();
        return flutter.isPresent() ? flutter : fromPlatformDefault();
    }

    public Optional<AndroidSdkInstallation> detect(Path explicitRoot) {
        return inspect(explicitRoot, "explicit Android SDK path");
    }

    /** Inspects a known root and reports individual missing tools in the returned snapshot. */
    public Optional<AndroidSdkInstallation> inspect(Path root, String source) {
        if (root == null) {
            return Optional.empty();
        }
        final Path normalized;
        try {
            normalized = root.toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
        if (!Files.isDirectory(normalized)) {
            return Optional.empty();
        }

        EnumMap<AndroidSdkTool, Path> tools = new EnumMap<>(AndroidSdkTool.class);
        findCommandLineTool(normalized, "sdkmanager")
                .ifPresent(path -> tools.put(AndroidSdkTool.SDK_MANAGER, path));
        findCommandLineTool(normalized, "avdmanager")
                .ifPresent(path -> tools.put(AndroidSdkTool.AVD_MANAGER, path));
        usable(normalized.resolve("emulator").resolve(executableName("emulator")))
                .ifPresent(path -> tools.put(AndroidSdkTool.EMULATOR, path));
        usable(normalized.resolve("platform-tools").resolve(executableName("adb")))
                .ifPresent(path -> tools.put(AndroidSdkTool.ADB, path));

        boolean sdkMarker = Files.isDirectory(normalized.resolve("platforms"))
                || Files.isDirectory(normalized.resolve("build-tools"))
                || Files.isDirectory(normalized.resolve("licenses"));
        if (tools.isEmpty() && !sdkMarker) {
            return Optional.empty();
        }
        return Optional.of(new AndroidSdkInstallation(normalized, tools, source));
    }

    private Optional<AndroidSdkInstallation> firstConfigured(List<ConfiguredPath> candidates) {
        for (ConfiguredPath candidate : candidates) {
            if (candidate.value() == null || candidate.value().isBlank()) {
                continue;
            }
            try {
                String value = stripQuotes(candidate.value());
                Optional<AndroidSdkInstallation> detected = inspect(Path.of(value), candidate.source());
                if (detected.isPresent()) {
                    return detected;
                }
            } catch (InvalidPathException ex) {
                // Ignore a malformed candidate and continue through the documented fallbacks.
            }
        }
        return Optional.empty();
    }

    private Optional<AndroidSdkInstallation> fromFlutterConfig() throws InterruptedException {
        if (flutterCommand == null) {
            return Optional.empty();
        }
        final ProcessResult result;
        try {
            result = flutterCommand.execute(
                    Path.of("").toAbsolutePath().normalize(),
                    FLUTTER_CONFIG_TIMEOUT,
                    "config", "--machine");
        } catch (IOException ex) {
            return Optional.empty();
        }
        if (!result.success() || result.stdout() == null || result.stdout().isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = JSON.readTree(result.stdout());
            JsonNode configured = root.get("android-sdk");
            if (configured == null || !configured.isTextual() || configured.asText().isBlank()) {
                return Optional.empty();
            }
            return inspect(Path.of(stripQuotes(configured.asText())),
                    "Flutter config android-sdk");
        } catch (IOException | InvalidPathException ex) {
            return Optional.empty();
        }
    }

    private Optional<AndroidSdkInstallation> fromPlatformDefault() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String userHome = properties.getProperty("user.home");
        if (os.contains("win")) {
            String localAppData = environment.get("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                try {
                    Optional<AndroidSdkInstallation> sdk = inspect(
                            Path.of(stripQuotes(localAppData)).resolve("Android").resolve("Sdk"),
                            "Windows default Android SDK path");
                    if (sdk.isPresent()) {
                        return sdk;
                    }
                } catch (InvalidPathException ignored) {
                }
            }
        } else if (userHome != null && !userHome.isBlank()) {
            try {
                Path home = Path.of(stripQuotes(userHome));
                Path candidate = os.contains("mac")
                        ? home.resolve("Library").resolve("Android").resolve("sdk")
                        : home.resolve("Android").resolve("Sdk");
                return inspect(candidate, os.contains("mac")
                        ? "macOS default Android SDK path" : "Linux default Android SDK path");
            } catch (InvalidPathException ignored) {
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> findCommandLineTool(Path root, String command) {
        Path commandLineTools = root.resolve("cmdline-tools");
        if (Files.isDirectory(commandLineTools)) {
            List<Path> versions = new ArrayList<>();
            try (var children = Files.list(commandLineTools)) {
                children.filter(Files::isDirectory).forEach(versions::add);
            } catch (IOException ignored) {
                // A legacy tools/bin fallback may still be usable.
            }
            versions.sort(commandLineToolsComparator());
            for (Path version : versions) {
                Optional<Path> candidate = usable(
                        version.resolve("bin").resolve(executableName(command)));
                if (candidate.isPresent()) {
                    return candidate;
                }
            }
        }
        return usable(root.resolve("tools").resolve("bin").resolve(executableName(command)));
    }

    private static Comparator<Path> commandLineToolsComparator() {
        return (left, right) -> {
            String leftName = left.getFileName().toString();
            String rightName = right.getFileName().toString();
            if (leftName.equalsIgnoreCase("latest")) {
                return rightName.equalsIgnoreCase("latest") ? 0 : -1;
            }
            if (rightName.equalsIgnoreCase("latest")) {
                return 1;
            }
            return -compareVersions(leftName, rightName);
        };
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("[._-]");
        String[] rightParts = right.split("[._-]");
        for (int index = 0; index < Math.max(leftParts.length, rightParts.length); index++) {
            String a = index < leftParts.length ? leftParts[index] : "0";
            String b = index < rightParts.length ? rightParts[index] : "0";
            int comparison;
            try {
                comparison = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException ex) {
                comparison = a.compareToIgnoreCase(b);
            }
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static Optional<Path> usable(Path candidate) {
        return Files.isRegularFile(candidate) && (isWindows() || Files.isExecutable(candidate))
                ? Optional.of(candidate.toAbsolutePath().normalize())
                : Optional.empty();
    }

    static String executableName(String command) {
        if (!isWindows()) {
            return command;
        }
        return switch (command) {
            case "sdkmanager", "avdmanager" -> command + ".bat";
            default -> command + ".exe";
        };
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String stripQuotes(String value) {
        String cleaned = value.strip();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return cleaned.substring(1, cleaned.length() - 1).strip();
            }
        }
        return cleaned;
    }

    private record ConfiguredPath(String value, String source) {
    }
}

package dev.flutter.netbeans.plugin.designer.canvas;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Re-probes the packaged native adapter and installed WebView2 Runtime without
 * allowing native code to load on an unsupported platform.
 */
final class WindowsWebView2RuntimeDetector {
    static final int MAXIMUM_REASON_CHARACTERS = 1_024;
    static final int MAXIMUM_RUNTIME_VERSION_CHARACTERS = 256;
    static final String MINIMUM_RUNTIME_VERSION = "100.0.1185.39";

    private static final Pattern RUNTIME_VERSION = Pattern.compile(
            "[0-9]+(?:\\.[0-9]+){2,4}");

    private final PlatformProbe platformProbe;
    private final NativeApiLoader nativeApiLoader;

    WindowsWebView2RuntimeDetector() {
        this(PlatformSnapshot::system, JnaWindowsWebView2NativeApi::loadPackaged);
    }

    WindowsWebView2RuntimeDetector(
            PlatformProbe platformProbe,
            NativeApiLoader nativeApiLoader) {
        this.platformProbe = Objects.requireNonNull(platformProbe, "platformProbe");
        this.nativeApiLoader = Objects.requireNonNull(nativeApiLoader, "nativeApiLoader");
    }

    /**
     * Performs a fresh probe on every call. In particular, an absent Runtime is
     * never cached because the user may install or repair it while NetBeans is
     * running.
     */
    Availability detect() {
        final PlatformSnapshot platform;
        try {
            platform = Objects.requireNonNull(
                    platformProbe.current(), "platform probe result");
        } catch (RuntimeException failure) {
            return Availability.unavailable(
                    "Windows Web Canvas could not inspect the current operating "
                    + "system and JVM: " + failureSummary(failure)
                    + ". Restart NetBeans and retry.");
        }

        if (!platform.isWindows()) {
            return Availability.unavailable(
                    "Windows Web Canvas is unavailable on "
                    + boundedValue(platform.operatingSystem())
                    + ". Use this platform's native Canvas preview, or reopen "
                    + "the project in a Windows x64 NetBeans installation.");
        }
        if (!platform.isX64Jvm()) {
            return Availability.unavailable(
                    "Windows Web Canvas requires a 64-bit x64 NetBeans JVM; "
                    + "the current JVM reports architecture "
                    + boundedValue(platform.architecture()) + " and data model "
                    + boundedValue(platform.dataModel())
                    + ". Run NetBeans with a Windows x64 JDK and retry.");
        }

        try {
            WindowsWebView2NativeApi nativeApi = Objects.requireNonNull(
                    nativeApiLoader.load(), "native WebView2 API");
            String version = nativeApi.runtimeVersion();
            if (!validRuntimeVersion(version)) {
                return Availability.unavailable(
                        "Windows Web Canvas received a malformed Microsoft Edge "
                        + "WebView2 Runtime version. Repair or reinstall the "
                        + "WebView2 Runtime, then retry.");
            }
            if (compareVersions(version, MINIMUM_RUNTIME_VERSION) < 0) {
                return Availability.unavailable(
                        "Windows Web Canvas requires Microsoft Edge WebView2 "
                        + "Runtime " + MINIMUM_RUNTIME_VERSION + " or newer; "
                        + "the installed Runtime is " + version + ". Update "
                        + "WebView2 Runtime, then retry.");
            }
            return Availability.available(version);
        } catch (IOException failure) {
            return Availability.unavailable(
                    "Windows Web Canvas is unavailable: "
                    + failureSummary(failure)
                    + ". Install or repair Microsoft Edge WebView2 Runtime. "
                    + "If it is already installed, reinstall the NetBeans "
                    + "Flutter plugin, then retry.");
        } catch (LinkageError failure) {
            return Availability.unavailable(
                    "Windows Web Canvas could not load its Windows x64 native "
                    + "adapter: " + failureSummary(failure)
                    + ". Run NetBeans with a Windows x64 JDK and reinstall the "
                    + "NetBeans Flutter plugin, then retry.");
        } catch (RuntimeException failure) {
            return Availability.unavailable(
                    "Windows Web Canvas runtime detection failed: "
                    + failureSummary(failure)
                    + ". Restart NetBeans and retry; if the problem persists, "
                    + "repair WebView2 Runtime or reinstall the plugin.");
        }
    }

    private static boolean validRuntimeVersion(String version) {
        return version != null
                && version.length() >= 3
                && version.length() <= MAXIMUM_RUNTIME_VERSION_CHARACTERS
                && RUNTIME_VERSION.matcher(version).matches();
    }

    private static boolean supportedRuntimeVersion(String version) {
        return validRuntimeVersion(version)
                && compareVersions(version, MINIMUM_RUNTIME_VERSION) >= 0;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.", -1);
        String[] rightParts = right.split("\\.", -1);
        int parts = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < parts; index++) {
            String leftPart = normalizedComponent(
                    index < leftParts.length ? leftParts[index] : "0");
            String rightPart = normalizedComponent(
                    index < rightParts.length ? rightParts[index] : "0");
            int lengthComparison = Integer.compare(
                    leftPart.length(), rightPart.length());
            if (lengthComparison != 0) {
                return lengthComparison;
            }
            int lexicalComparison = leftPart.compareTo(rightPart);
            if (lexicalComparison != 0) {
                return lexicalComparison;
            }
        }
        return 0;
    }

    private static String normalizedComponent(String value) {
        int firstNonZero = 0;
        while (firstNonZero < value.length() - 1
                && value.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return value.substring(firstNonZero);
    }

    private static String failureSummary(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return boundedValue(message);
    }

    private static String boundedValue(String value) {
        String source = Objects.requireNonNullElse(value, "unknown");
        int sourceLimit = Math.min(source.length(), 512);
        StringBuilder normalized = new StringBuilder(sourceLimit);
        boolean previousSpace = false;
        for (int index = 0; index < sourceLimit; index++) {
            char character = source.charAt(index);
            boolean space = Character.isWhitespace(character)
                    || Character.isISOControl(character);
            if (space) {
                if (!previousSpace && !normalized.isEmpty()) {
                    normalized.append(' ');
                }
            } else {
                normalized.append(character);
            }
            previousSpace = space;
        }
        String result = normalized.toString().strip();
        return result.isEmpty() ? "unknown" : result;
    }

    private static String boundedReason(String reason) {
        String normalized = boundedText(reason, MAXIMUM_REASON_CHARACTERS);
        return normalized.isEmpty()
                ? "Windows Web Canvas availability could not be determined."
                : normalized;
    }

    private static String boundedText(String source, int maximum) {
        String value = Objects.requireNonNullElse(source, "");
        StringBuilder result = new StringBuilder(Math.min(value.length(), maximum));
        boolean previousSpace = false;
        for (int index = 0; index < value.length() && result.length() < maximum; index++) {
            char character = value.charAt(index);
            boolean space = Character.isWhitespace(character)
                    || Character.isISOControl(character);
            if (space) {
                if (!previousSpace && !result.isEmpty()
                        && result.length() < maximum) {
                    result.append(' ');
                }
            } else {
                result.append(character);
            }
            previousSpace = space;
        }
        return result.toString().strip();
    }

    record Availability(boolean available, String runtimeVersion, String reason) {
        Availability {
            runtimeVersion = Objects.requireNonNull(runtimeVersion, "runtimeVersion");
            reason = boundedReason(reason);
            if (available != supportedRuntimeVersion(runtimeVersion)) {
                throw new IllegalArgumentException(
                        "WebView2 availability and runtime version disagree");
            }
            if (!available && !runtimeVersion.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unavailable WebView2 result must not expose a runtime version");
            }
        }

        static Availability available(String version) {
            return new Availability(
                    true,
                    version,
                    "Microsoft Edge WebView2 Runtime " + version
                    + " is available for Windows Web Canvas.");
        }

        static Availability unavailable(String reason) {
            return new Availability(false, "", reason);
        }
    }

    record PlatformSnapshot(
            String operatingSystem,
            String architecture,
            String dataModel) {
        PlatformSnapshot {
            operatingSystem = Objects.requireNonNullElse(operatingSystem, "");
            architecture = Objects.requireNonNullElse(architecture, "");
            dataModel = Objects.requireNonNullElse(dataModel, "");
        }

        static PlatformSnapshot system() {
            return new PlatformSnapshot(
                    System.getProperty("os.name", ""),
                    System.getProperty("os.arch", ""),
                    System.getProperty("sun.arch.data.model", ""));
        }

        boolean isWindows() {
            return operatingSystem.toLowerCase(Locale.ROOT).startsWith("windows");
        }

        boolean isX64Jvm() {
            String normalized = architecture.toLowerCase(Locale.ROOT);
            return (normalized.equals("amd64") || normalized.equals("x86_64"))
                    && dataModel.equals("64");
        }
    }

    @FunctionalInterface
    interface PlatformProbe {
        PlatformSnapshot current();
    }

    @FunctionalInterface
    interface NativeApiLoader {
        WindowsWebView2NativeApi load() throws IOException;
    }
}

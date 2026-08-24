package dev.flutter.netbeans.plugin.device;

import java.util.Objects;

/** Android SDK discovery result presented by the Flutter Device Manager. */
public record AndroidToolchainStatus(
        State state,
        String androidSdkPath,
        ToolState adb,
        ToolState emulator,
        ToolState avdManager,
        ToolState sdkManager,
        String detail) {

    public AndroidToolchainStatus {
        state = Objects.requireNonNull(state, "state");
        androidSdkPath = normalize(androidSdkPath);
        adb = Objects.requireNonNull(adb, "adb");
        emulator = Objects.requireNonNull(emulator, "emulator");
        avdManager = Objects.requireNonNull(avdManager, "avdManager");
        sdkManager = Objects.requireNonNull(sdkManager, "sdkManager");
        detail = normalize(detail);
    }

    public static AndroidToolchainStatus discovering() {
        return new AndroidToolchainStatus(
                State.DISCOVERING,
                "",
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                "Searching android.sdk, ANDROID_SDK_ROOT, ANDROID_HOME, Flutter configuration "
                        + "and standard Android SDK locations.");
    }

    public static AndroidToolchainStatus unavailable(String detail) {
        return new AndroidToolchainStatus(
                State.UNAVAILABLE,
                "",
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                ToolState.UNKNOWN,
                detail);
    }

    public static AndroidToolchainStatus ready(String androidSdkPath) {
        return new AndroidToolchainStatus(
                State.READY,
                androidSdkPath,
                ToolState.AVAILABLE,
                ToolState.AVAILABLE,
                ToolState.AVAILABLE,
                ToolState.AVAILABLE,
                "Android SDK tools are available.");
    }

    public boolean canQueryDevices() {
        return adb == ToolState.AVAILABLE;
    }

    public boolean canCreateAvd() {
        return avdManager == ToolState.AVAILABLE && sdkManager == ToolState.AVAILABLE;
    }

    public boolean canStartAvd() {
        return emulator == ToolState.AVAILABLE;
    }

    public boolean canStopAvd() {
        return adb == ToolState.AVAILABLE;
    }

    public boolean canWipeAvd() {
        return emulator == ToolState.AVAILABLE;
    }

    public boolean canDeleteAvd() {
        return avdManager == ToolState.AVAILABLE;
    }

    public enum State {
        DISCOVERING,
        READY,
        INCOMPLETE,
        UNAVAILABLE,
        ERROR
    }

    public enum ToolState {
        AVAILABLE,
        MISSING,
        UNKNOWN
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}

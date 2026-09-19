package io.github.vgrytsenko2022.run;

/** Command-line tools that can be present in an Android SDK installation. */
public enum AndroidSdkTool {
    SDK_MANAGER("sdkmanager"),
    AVD_MANAGER("avdmanager"),
    EMULATOR("emulator"),
    ADB("adb");

    private final String displayName;

    AndroidSdkTool(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

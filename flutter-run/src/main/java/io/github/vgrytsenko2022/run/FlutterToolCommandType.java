package io.github.vgrytsenko2022.run;

/** A supported one-shot Flutter project command. */
public enum FlutterToolCommandType {
    CLEAN("Clean"),
    BUILD("Build"),
    ADD_PLATFORMS("Add Platforms"),
    PUB_GET("Pub Get"),
    ANALYZE("Analyze"),
    TEST("Test");

    private final String displayName;

    FlutterToolCommandType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

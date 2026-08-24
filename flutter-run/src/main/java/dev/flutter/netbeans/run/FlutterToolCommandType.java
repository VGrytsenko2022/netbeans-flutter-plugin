package dev.flutter.netbeans.run;

/** A supported one-shot Flutter project command. */
public enum FlutterToolCommandType {
    CLEAN("Clean"),
    BUILD("Build"),
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

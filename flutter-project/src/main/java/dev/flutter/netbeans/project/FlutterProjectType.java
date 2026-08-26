package dev.flutter.netbeans.project;

/** Flutter template kind relevant to platform-scaffolding operations. */
public enum FlutterProjectType {
    APP("app"),
    MODULE("module"),
    PACKAGE("package"),
    PLUGIN("plugin"),
    UNKNOWN("unknown");

    private final String id;

    FlutterProjectType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean supportsPlatformScaffolding() {
        return this == APP;
    }
}

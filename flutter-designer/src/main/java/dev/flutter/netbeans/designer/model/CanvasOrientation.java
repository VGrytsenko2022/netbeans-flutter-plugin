package dev.flutter.netbeans.designer.model;

/** Design-time device orientation. */
public enum CanvasOrientation {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    private final String wireName;

    CanvasOrientation(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

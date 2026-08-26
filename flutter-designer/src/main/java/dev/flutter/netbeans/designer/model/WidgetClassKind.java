package dev.flutter.netbeans.designer.model;

/** Supported Dart class shapes generated for a designer form. */
public enum WidgetClassKind {
    STATELESS("stateless"),
    STATEFUL("stateful");

    private final String wireName;

    WidgetClassKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

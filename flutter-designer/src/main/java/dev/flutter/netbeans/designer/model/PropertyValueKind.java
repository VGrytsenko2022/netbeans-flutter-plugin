package dev.flutter.netbeans.designer.model;

/** Typed property-value discriminators defined by schema version 1. */
public enum PropertyValueKind {
    STRING("string"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
    DOUBLE("double"),
    ENUM("enum"),
    COLOR("color"),
    EDGE_INSETS("edgeInsets"),
    ASSET("asset"),
    CALLBACK("callback"),
    DART_EXPRESSION("dartExpression");

    private final String wireName;

    PropertyValueKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

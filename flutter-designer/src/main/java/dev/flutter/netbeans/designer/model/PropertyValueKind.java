package dev.flutter.netbeans.designer.model;

/** Typed property-value discriminators defined through the current schema version. */
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
    DART_EXPRESSION("dartExpression"),
    ICON_DATA("iconData"),
    THEME_TOKEN("themeToken"),
    PAINT("paint"),
    SHADOW_LIST("shadowList"),
    FONT_FEATURE_LIST("fontFeatureList"),
    FONT_VARIATION_LIST("fontVariationList"),
    ALIGNMENT_GEOMETRY("alignmentGeometry"),
    BOX_CONSTRAINTS("boxConstraints"),
    MATRIX4("matrix4"),
    IMAGE_PROVIDER("imageProvider"),
    BOX_DECORATION("boxDecoration");

    private final String wireName;

    PropertyValueKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

package io.github.vgrytsenko2022.designer.model;

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
    DART_OBJECT_REFERENCE("dartObjectReference"),
    ICON_DATA("iconData"),
    THEME_TOKEN("themeToken"),
    PAINT("paint"),
    SHADOW_LIST("shadowList"),
    FONT_FEATURE_LIST("fontFeatureList"),
    FONT_VARIATION_LIST("fontVariationList"),
    ALIGNMENT_GEOMETRY("alignmentGeometry"),
    OFFSET("offset"),
    SIZE("size"),
    BOX_CONSTRAINTS("boxConstraints"),
    MATRIX4("matrix4"),
    IMAGE_PROVIDER("imageProvider"),
    BORDER_RADIUS("borderRadius"),
    SHAPE_BORDER_CLIPPER("shapeBorderClipper"),
    BOX_DECORATION("boxDecoration"),
    GRADIENT("gradient"),
    POINTER_DEVICE_KIND_SET("pointerDeviceKindSet"),
    NULL("null");

    private final String wireName;

    PropertyValueKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

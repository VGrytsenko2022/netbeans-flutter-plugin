package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete Flutter 3.44.8 ImageIcon constructor with the reviewed typed asset provider domain. */
public final class ImageIconWidgetPropertySchema {
    public static final WidgetTypeId IMAGE_ICON_TYPE = new WidgetTypeId("flutter.widgets.ImageIcon");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 4;
    public static final int SLOT_COUNT = 0;

    public enum Group {
        IMAGE("imageIconImage", "Image", "A nullable image provider used as an icon mask."),
        APPEARANCE("imageIconAppearance", "Appearance", "Local icon size and color overrides."),
        ACCESSIBILITY("imageIconAccessibility", "Accessibility", "The icon's spoken label.");
        private final String setName;
        private final String displayName;
        private final String description;
        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }
        public String setName() { return setName; }
        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    public record Definition(Group group, String displayName, String description, String dartName, int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) throw new IllegalArgumentException("dartOrder must be non-negative");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();
    private ImageIconWidgetPropertySchema() { }
    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name").value()));
    }
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        add(values, "image", Group.IMAGE, "Image",
                "Required positional argument. None emits explicit null and reserves an empty icon-sized box. Otherwise select a declared project or package AssetImage/ExactAssetImage, optionally wrapped in bounded ResizeImage. This argument cannot be reset to omission. Network, file, arbitrary memory and custom providers are not represented by the typed asset editor.", 0);
        add(values, "size", Group.APPEARANCE, "Size",
                "Non-negative square size in logical pixels. Unset inherits IconTheme.size, ultimately 24. ImageIcon does not apply IconTheme text scaling or variable-font properties.", 0);
        add(values, "color", Group.APPEARANCE, "Color",
                "Literal ARGB color or reviewed Material ColorScheme role. Unset inherits IconTheme.color. IconTheme.opacity multiplies the alpha of both explicit and inherited colors; the image uses BoxFit.scaleDown.", 1);
        add(values, "semanticLabel", Group.ACCESSIBILITY, "Semantic label",
                "Alternative spoken label on the outer Semantics, including an empty image. The inner Image is excluded from semantics to avoid a duplicate label.", 2);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("ImageIcon schema/property count mismatch");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String, Definition> values, String name, Group group,
            String displayName, String description, int order) {
        if (values.putIfAbsent(name, new Definition(group, displayName, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate ImageIcon property schema: " + name);
        }
    }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}

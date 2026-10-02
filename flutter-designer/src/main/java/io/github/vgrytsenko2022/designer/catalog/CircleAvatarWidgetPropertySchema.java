package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Complete pinned Flutter 3.44.8 CircleAvatar constructor projection. */
public final class CircleAvatarWidgetPropertySchema {
    public static final WidgetTypeId CIRCLE_AVATAR_TYPE =
            new WidgetTypeId("flutter.material.CircleAvatar");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 9;
    public static final int SLOT_COUNT = 1;
    public static final PropertyValue.EnumValue POSITIVE_INFINITY =
            new PropertyValue.EnumValue("double", "infinity");

    public enum Group {
        APPEARANCE("circleAvatarAppearance", "Appearance", "Inherited or explicit avatar colors."),
        IMAGES("circleAvatarImages", "Images", "Foreground image and background fallback."),
        CALLBACKS("circleAvatarCallbacks", "Image callbacks", "Validated image-error handler identifiers."),
        SIZE("circleAvatarSize", "Size", "Fixed or minimum/maximum radius constraints.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return setName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    public record Definition(
            Group group,
            String displayName,
            String description,
            String dartName,
            int dartOrder) {
        public Definition {
            Objects.requireNonNull(group, "group");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("Negative CircleAvatar property order");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = createDefinitions();

    private CircleAvatarWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        return Optional.ofNullable(DEFINITIONS.get(
                Objects.requireNonNull(name, "name").value()));
    }

    public static Map<String, Definition> definitions() {
        return DEFINITIONS;
    }

    public static boolean isRadiusProperty(PropertyName name) {
        return switch (Objects.requireNonNull(name, "name").value()) {
            case "radius", "minRadius", "maxRadius" -> true;
            default -> false;
        };
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> values = new LinkedHashMap<>();
        add(values, "backgroundColor", Group.APPEARANCE, "Background color",
                "Literal or reviewed semantic color. Unset uses primaryContainer in Material 3; "
                + "Material 2 derives a contrasting primaryColorLight/primaryColorDark from the text color. "
                + "Background changes use the SDK theme-change animation.", 1);
        add(values, "backgroundImage", Group.IMAGES, "Background image",
                "Optional declared project/package AssetImage or ExactAssetImage with reviewed ResizeImage options. "
                + "Paints behind Child and is the fallback for Foreground image. Images use BoxFit.cover and "
                + "circular decoration; arbitrary Child content is not clipped. Restore Default also clears "
                + "On background image error. Network, file, arbitrary memory and custom providers are not "
                + "represented by the existing typed asset editor.", 2);
        add(values, "foregroundImage", Group.IMAGES, "Foreground image",
                "Optional declared project/package asset provider, including exact scale and ResizeImage. "
                + "Paints over Child; when loading fails the background image/color and Child remain visible. "
                + "Restore Default also clears On foreground image error. No provider or asset default is stored.", 3);
        add(values, "onBackgroundImageError", Group.CALLBACKS, "On background image error",
                "Dart ImageErrorListener identifier with signature void Function(Object, StackTrace?). "
                + "Requires Background image; set the provider first. Canvas never executes project callbacks.", 4);
        add(values, "onForegroundImageError", Group.CALLBACKS, "On foreground image error",
                "Dart ImageErrorListener identifier with signature void Function(Object, StackTrace?). "
                + "Requires Foreground image; set the provider first. Canvas never executes project callbacks.", 5);
        add(values, "foregroundColor", Group.APPEARANCE, "Foreground color",
                "Default text and icon color below the avatar. Material 3 defaults to onPrimaryContainer; "
                + "Material 2 uses the primary text theme or contrasts an explicit background. Child uses "
                + "titleMedium with text scaling disabled, not a separately configurable TextStyle.", 6);
        add(values, "radius", Group.SIZE, "Radius",
                "Non-negative number or Infinity. Fixed radius excludes Min radius and Max radius; setting it "
                + "clears both bounds atomically. All three unset means radius 20. Infinite minimum constraints "
                + "require a bounded parent. Stored finite radii use the SDK's double multiplication for diameter.", 7);
        add(values, "minRadius", Group.SIZE, "Min radius",
                "Non-negative number or Infinity. Setting a bound clears Radius. With a bound present, an unset "
                + "minimum is zero; resolved minimum diameter must not exceed maximum diameter. Infinity requires "
                + "an unbounded maximum and a bounded parent when applied.", 8);
        add(values, "maxRadius", Group.SIZE, "Max radius",
                "Non-negative number or Infinity. Setting a bound clears Radius. With a bound present, an unset "
                + "maximum is Infinity. Explicit Infinity is not omission: alone it means minimum zero/unbounded "
                + "maximum, while all three radii unset mean the fixed default radius 20.", 9);
        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("CircleAvatar property count mismatch");
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String display,
            String description,
            int order) {
        if (values.put(name, new Definition(group, display, description, name, order)) != null) {
            throw new IllegalStateException("Duplicate CircleAvatar property: " + name);
        }
    }
}

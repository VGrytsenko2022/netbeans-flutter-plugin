package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed, independently resettable projection of Flutter {@code AppBar}'s
 * constructor properties.
 *
 * <p>The six structured constructor arguments are flattened in the canonical
 * model. Dart generation and the native Canvas must assemble the same closed
 * {@code ShapeBorder}, two {@code IconThemeData}, two {@code TextStyle}, and
 * {@code SystemUiOverlayStyle} values. No property in this contract contains
 * an executable Dart expression.</p>
 */
public final class AppBarWidgetPropertySchema {
    public static final WidgetTypeId APP_BAR_TYPE =
            new WidgetTypeId("flutter.material.AppBar");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 28;
    public static final int SLOT_COUNT = 5;

    public enum Group {
        BEHAVIOR("appBarBehavior", "Behavior",
                "Automatic controls, semantics, clipping, and animation."),
        LAYOUT("appBarLayout", "Layout",
                "Toolbar dimensions, opacity, spacing, and padding."),
        COLORS_AND_ELEVATION("appBarColorsElevation", "Colors and elevation",
                "Theme-aware colors, elevation, and scroll-under appearance."),
        SHAPE("appBarShape", "Shape",
                "Closed, serializable Material ShapeBorder subset."),
        ICON_THEME("appBarIconTheme", "Leading icon theme",
                "Fields assembled into the optional AppBar iconTheme."),
        ACTIONS_ICON_THEME("appBarActionsIconTheme", "Actions icon theme",
                "Fields assembled into the optional AppBar actionsIconTheme."),
        TOOLBAR_TEXT_STYLE("appBarToolbarTextStyle", "Toolbar text style",
                "Fields assembled into the optional toolbarTextStyle."),
        TITLE_TEXT_STYLE("appBarTitleTextStyle", "Title text style",
                "Fields assembled into the optional titleTextStyle."),
        SYSTEM_UI("appBarSystemUi", "System UI overlay",
                "Typed status-bar and system-navigation-bar appearance.");

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

    public enum Target {
        DIRECT,
        NOTIFICATION_PREDICATE,
        SHAPE_KIND,
        SHAPE,
        ICON_THEME,
        ACTIONS_ICON_THEME,
        TOOLBAR_TEXT_STYLE_THEME,
        TOOLBAR_TEXT_STYLE,
        TOOLBAR_TEXT_STYLE_LOCALE,
        TOOLBAR_TEXT_STYLE_DECORATION,
        TITLE_TEXT_STYLE_THEME,
        TITLE_TEXT_STYLE,
        TITLE_TEXT_STYLE_LOCALE,
        TITLE_TEXT_STYLE_DECORATION,
        SYSTEM_UI_OVERLAY_STYLE
    }

    public enum Encoding {
        SCALAR,
        NEWLINE_STRING_LIST,
        DECORATION_FLAG
    }

    public record Definition(
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int dartOrder,
            Encoding encoding) {

        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            Objects.requireNonNull(target, "target");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("dartOrder must be non-negative");
            }
            Objects.requireNonNull(encoding, "encoding");
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private AppBarWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static boolean isCompound(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();

        direct(values, "backgroundColor", Group.COLORS_AND_ELEVATION,
                "Background color", "Literal or semantic theme background color.");
        direct(values, "centerTitle", Group.LAYOUT,
                "Center title", "Unset defers to AppBarTheme and platform adaptation.");
        direct(values, "elevation", Group.COLORS_AND_ELEVATION,
                "Elevation", "Non-negative resting elevation; unset inherits AppBarTheme.");
        direct(values, "automaticallyImplyLeading", Group.BEHAVIOR,
                "Imply leading", "Allow AppBar to infer drawer, back, or close controls.");
        direct(values, "automaticallyImplyActions", Group.BEHAVIOR,
                "Imply actions", "Allow AppBar to infer an end-drawer action.");
        direct(values, "scrolledUnderElevation", Group.COLORS_AND_ELEVATION,
                "Scrolled-under elevation", "Non-negative elevation while content scrolls underneath.");
        add(values, "notificationPredicate", Group.BEHAVIOR,
                "Scroll notifications", "Closed preset used to accept scroll notifications.",
                Target.NOTIFICATION_PREDICATE, "notificationPredicate", 0, Encoding.SCALAR);
        direct(values, "shadowColor", Group.COLORS_AND_ELEVATION,
                "Shadow color", "Literal or semantic theme shadow color.");
        direct(values, "surfaceTintColor", Group.COLORS_AND_ELEVATION,
                "Surface tint color", "Literal or semantic theme surface tint.");
        direct(values, "foregroundColor", Group.COLORS_AND_ELEVATION,
                "Foreground color", "Default text and icon foreground color.");
        direct(values, "primary", Group.BEHAVIOR,
                "Primary", "Inset the toolbar below the ambient system status area.");
        direct(values, "excludeHeaderSemantics", Group.BEHAVIOR,
                "Exclude header semantics", "Do not mark the title as an accessibility header.");
        direct(values, "titleSpacing", Group.LAYOUT,
                "Title spacing", "Finite horizontal title spacing; unset inherits AppBarTheme.");
        direct(values, "toolbarOpacity", Group.LAYOUT,
                "Toolbar opacity", "Toolbar opacity from zero through one.");
        direct(values, "bottomOpacity", Group.LAYOUT,
                "Bottom opacity", "Bottom-slot opacity from zero through one.");
        direct(values, "toolbarHeight", Group.LAYOUT,
                "Toolbar height", "Non-negative toolbar height; unset inherits AppBarTheme.");
        direct(values, "leadingWidth", Group.LAYOUT,
                "Leading width", "Non-negative leading-slot width; unset inherits AppBarTheme.");
        direct(values, "forceMaterialTransparency", Group.BEHAVIOR,
                "Force material transparency", "Use MaterialType.transparency for the AppBar material.");
        direct(values, "useDefaultSemanticsOrder", Group.BEHAVIOR,
                "Default semantics order", "Use AppBar's standard accessibility traversal order.");
        direct(values, "clipBehavior", Group.BEHAVIOR,
                "Clip behavior", "Clip behavior for the toolbar region.");
        direct(values, "actionsPadding", Group.LAYOUT,
                "Actions padding", "Non-negative physical or directional padding around actions.");
        direct(values, "animateColor", Group.BEHAVIOR,
                "Animate color", "Animate Material color changes.");

        add(values, "shapeKind", Group.SHAPE, "Shape kind",
                "Closed ShapeBorder constructor preset.", Target.SHAPE_KIND,
                "kind", 0, Encoding.SCALAR);
        shape(values, "shapeSideColor", "Side color", "Border side color.", "color", 0);
        shape(values, "shapeSideWidth", "Side width", "Non-negative border side width.", "width", 1);
        shape(values, "shapeSideStyle", "Side style", "BorderSide none or solid.", "style", 2);
        shape(values, "shapeSideStrokeAlign", "Stroke align",
                "BorderSide stroke alignment from inside (-1) through outside (1).",
                "strokeAlign", 3);
        shape(values, "shapeRadiusTopLeft", "Top-left radius",
                "Non-negative circular top-left radius.", "topLeft", 0);
        shape(values, "shapeRadiusTopRight", "Top-right radius",
                "Non-negative circular top-right radius.", "topRight", 1);
        shape(values, "shapeRadiusBottomRight", "Bottom-right radius",
                "Non-negative circular bottom-right radius.", "bottomRight", 2);
        shape(values, "shapeRadiusBottomLeft", "Bottom-left radius",
                "Non-negative circular bottom-left radius.", "bottomLeft", 3);
        shape(values, "shapeCircleEccentricity", "Circle eccentricity",
                "CircleBorder eccentricity from zero through one.", "eccentricity", 0);

        iconTheme(values, "iconTheme", Group.ICON_THEME, Target.ICON_THEME);
        iconTheme(values, "actionsIconTheme", Group.ACTIONS_ICON_THEME,
                Target.ACTIONS_ICON_THEME);
        textStyle(values, "toolbarTextStyle", Group.TOOLBAR_TEXT_STYLE, true);
        textStyle(values, "titleTextStyle", Group.TITLE_TEXT_STYLE, false);

        systemUi(values, "systemOverlayStyleSystemNavigationBarColor",
                "Navigation bar color", "System navigation-bar color.",
                "systemNavigationBarColor", 0);
        systemUi(values, "systemOverlayStyleSystemNavigationBarDividerColor",
                "Navigation divider color", "System navigation-bar divider color.",
                "systemNavigationBarDividerColor", 1);
        systemUi(values, "systemOverlayStyleSystemNavigationBarIconBrightness",
                "Navigation icon brightness", "Light or dark system navigation icons.",
                "systemNavigationBarIconBrightness", 2);
        systemUi(values, "systemOverlayStyleSystemNavigationBarContrastEnforced",
                "Navigation contrast", "Override system navigation-bar contrast enforcement.",
                "systemNavigationBarContrastEnforced", 3);
        systemUi(values, "systemOverlayStyleStatusBarColor",
                "Status bar color", "System status-bar color.", "statusBarColor", 4);
        systemUi(values, "systemOverlayStyleStatusBarBrightness",
                "Status bar brightness", "iOS status-bar brightness.",
                "statusBarBrightness", 5);
        systemUi(values, "systemOverlayStyleStatusBarIconBrightness",
                "Status icon brightness", "Android status-bar icon brightness.",
                "statusBarIconBrightness", 6);
        systemUi(values, "systemOverlayStyleSystemStatusBarContrastEnforced",
                "Status contrast", "Override system status-bar contrast enforcement.",
                "systemStatusBarContrastEnforced", 7);

        return Map.copyOf(values);
    }

    private static void iconTheme(
            Map<String, Definition> values,
            String prefix,
            Group group,
            Target target) {
        compound(values, prefix + "Size", group, "Size",
                "Non-negative logical icon size.", target, "size", 0);
        compound(values, prefix + "Fill", group, "Fill",
                "Variable-font FILL axis from zero through one.", target, "fill", 1);
        compound(values, prefix + "Weight", group, "Weight",
                "Variable-font wght axis greater than zero and less than 32768.",
                target, "weight", 2);
        compound(values, prefix + "Grade", group, "Grade",
                "Variable-font GRAD axis from -32768 inclusive to 32768 exclusive.",
                target, "grade", 3);
        compound(values, prefix + "OpticalSize", group, "Optical size",
                "Variable-font opsz axis greater than zero and less than 32768.",
                target, "opticalSize", 4);
        compound(values, prefix + "Color", group, "Color",
                "Literal or semantic theme icon color.", target, "color", 5);
        compound(values, prefix + "Opacity", group, "Opacity",
                "Icon opacity from zero through one.", target, "opacity", 6);
        compound(values, prefix + "Shadows", group, "Shadows",
                "Ordered icon shadows; an explicit empty list suppresses shadows.",
                target, "shadows", 7);
        compound(values, prefix + "ApplyTextScaling", group, "Apply text scaling",
                "Scale icons with ambient text scaling.", target, "applyTextScaling", 8);
    }

    private static void textStyle(
            Map<String, Definition> values,
            String prefix,
            Group group,
            boolean toolbar) {
        for (Map.Entry<String, TextWidgetPropertySchema.Definition> entry
                : TextWidgetPropertySchema.definitions().entrySet()) {
            TextWidgetPropertySchema.Definition source = entry.getValue();
            Target target = switch (source.target()) {
                case TEXT_STYLE_THEME -> toolbar
                        ? Target.TOOLBAR_TEXT_STYLE_THEME : Target.TITLE_TEXT_STYLE_THEME;
                case TEXT_STYLE -> toolbar
                        ? Target.TOOLBAR_TEXT_STYLE : Target.TITLE_TEXT_STYLE;
                case TEXT_STYLE_LOCALE -> toolbar
                        ? Target.TOOLBAR_TEXT_STYLE_LOCALE : Target.TITLE_TEXT_STYLE_LOCALE;
                case TEXT_STYLE_DECORATION -> toolbar
                        ? Target.TOOLBAR_TEXT_STYLE_DECORATION
                        : Target.TITLE_TEXT_STYLE_DECORATION;
                default -> null;
            };
            if (target == null) {
                continue;
            }
            String sourceName = entry.getKey();
            if (!sourceName.startsWith("style")) {
                throw new IllegalStateException("Unexpected TextStyle property: " + sourceName);
            }
            String name = prefix + sourceName.substring("style".length());
            Encoding encoding = switch (source.encoding()) {
                case SCALAR -> Encoding.SCALAR;
                case NEWLINE_STRING_LIST -> Encoding.NEWLINE_STRING_LIST;
                case DECORATION_FLAG -> Encoding.DECORATION_FLAG;
            };
            add(values, name, group, source.displayName(), source.description(),
                    target, source.dartName(), source.dartOrder(), encoding);
        }
    }

    private static void direct(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description) {
        add(values, name, group, displayName, description,
                Target.DIRECT, name, 0, Encoding.SCALAR);
    }

    private static void shape(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, Group.SHAPE, displayName, description,
                Target.SHAPE, dartName, order);
    }

    private static void systemUi(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, Group.SYSTEM_UI, displayName, description,
                Target.SYSTEM_UI_OVERLAY_STYLE, dartName, order);
    }

    private static void compound(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int order) {
        add(values, name, group, displayName, description,
                target, dartName, order, Encoding.SCALAR);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int order,
            Encoding encoding) {
        if (values.putIfAbsent(name, new Definition(
                group, displayName, description, target, dartName, order, encoding)) != null) {
            throw new IllegalStateException("Duplicate AppBar property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}

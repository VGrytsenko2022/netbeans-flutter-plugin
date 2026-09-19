package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed, independently resettable projection of Flutter {@code ElevatedButton}
 * and its optional {@code ButtonStyle}.
 *
 * <p>The state-aware style is deliberately sparse. Each persisted leaf belongs
 * to the enabled/default, disabled, pressed, hovered, or focused bucket, and an
 * unset leaf therefore preserves Flutter's local style to
 * {@code ElevatedButtonTheme} to framework-default fallback. Text foreground
 * is represented by {@code ButtonStyle.foregroundColor}; the two layer builders
 * use strict typed project references. Raw executable expressions, focus nodes,
 * whole-style references and state controllers are outside this closed contract.</p>
 */
public final class ElevatedButtonWidgetPropertySchema {
    public static final WidgetTypeId ELEVATED_BUTTON_TYPE =
            new WidgetTypeId("flutter.material.ElevatedButton");
    public static final int DIRECT_PROPERTY_COUNT = 7;
    public static final int STATE_COUNT = 5;
    public static final int STATE_NON_TEXT_PROPERTY_COUNT = 26;
    public static final int STATE_TEXT_PROPERTY_COUNT = 28;
    public static final int STATE_PROPERTY_COUNT =
            STATE_NON_TEXT_PROPERTY_COUNT + STATE_TEXT_PROPERTY_COUNT;
    public static final int COMMON_STYLE_PROPERTY_COUNT = 11;
    public static final String BUTTON_LAYER_BUILDER_TYPE = "ButtonLayerBuilder";

    public static List<String> layerBuilderProperties() {
        return List.of("styleBackgroundBuilder", "styleForegroundBuilder");
    }
    public static final int FLATTENED_PROPERTY_COUNT =
            DIRECT_PROPERTY_COUNT
            + STATE_COUNT * STATE_PROPERTY_COUNT
            + COMMON_STYLE_PROPERTY_COUNT;
    public static final int SLOT_COUNT = 1;

    public enum Group {
        EVENTS("elevatedButtonEvents", "Events",
                "Validated callback identifiers for button interaction events."),
        BEHAVIOR("elevatedButtonBehavior", "Behavior",
                "Activation, focus, and clipping behavior."),
        ENABLED_STYLE("elevatedButtonEnabledStyle", "Style — enabled/default",
                "Sparse ButtonStyle values for the enabled/default state."),
        DISABLED_STYLE("elevatedButtonDisabledStyle", "Style — disabled",
                "Sparse ButtonStyle values resolved before every enabled state."),
        PRESSED_STYLE("elevatedButtonPressedStyle", "Style — pressed",
                "Sparse ButtonStyle values while the button is pressed."),
        HOVERED_STYLE("elevatedButtonHoveredStyle", "Style — hovered",
                "Sparse ButtonStyle values while a pointer hovers over the button."),
        FOCUSED_STYLE("elevatedButtonFocusedStyle", "Style — focused",
                "Sparse ButtonStyle values while the button has focus."),
        COMMON_STYLE("elevatedButtonCommonStyle", "Style — layout & feedback",
                "Non-state ButtonStyle layout, animation, feedback, splash and layer builders.");

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

    /** Closed assembly targets consumed by Dart generation and Canvas parity. */
    public enum Target {
        DIRECT,
        ACTIVATION,
        STYLE_STATE,
        STYLE_TEXT,
        STYLE_TEXT_LOCALE,
        STYLE_TEXT_DECORATION,
        STYLE_COMMON
    }

    public enum Encoding {
        SCALAR,
        NEWLINE_STRING_LIST,
        DECORATION_FLAG
    }

    /**
     * Metadata for one flattened leaf.
     *
     * <p>{@code dartName} is an assembly path rather than an executable Dart
     * expression. {@code dartOrder} is unique inside each state bucket and
     * deterministic for common and direct values.</p>
     */
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

    private record StateProjection(String prefix, Group group) {
        private StateProjection {
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(group, "group");
        }
    }

    private record TextLeaf(String suffix, String sourceName) {
        private TextLeaf {
            suffix = requireText(suffix, "suffix");
            sourceName = requireText(sourceName, "sourceName");
        }
    }

    private static final List<StateProjection> STATES = List.of(
            new StateProjection("style", Group.ENABLED_STYLE),
            new StateProjection("styleDisabled", Group.DISABLED_STYLE),
            new StateProjection("stylePressed", Group.PRESSED_STYLE),
            new StateProjection("styleHovered", Group.HOVERED_STYLE),
            new StateProjection("styleFocused", Group.FOCUSED_STYLE));

    private static final List<TextLeaf> TEXT_LEAVES = List.of(
            new TextLeaf("TextTheme", "styleThemeTextStyle"),
            new TextLeaf("TextInherit", "styleInherit"),
            new TextLeaf("TextBackgroundColor", "styleBackgroundColor"),
            new TextLeaf("TextFontSize", "styleFontSize"),
            new TextLeaf("TextFontWeight", "styleFontWeight"),
            new TextLeaf("TextFontStyle", "styleFontStyle"),
            new TextLeaf("TextLetterSpacing", "styleLetterSpacing"),
            new TextLeaf("TextWordSpacing", "styleWordSpacing"),
            new TextLeaf("TextTextBaseline", "styleTextBaseline"),
            new TextLeaf("TextHeight", "styleHeight"),
            new TextLeaf("TextLeadingDistribution", "styleLeadingDistribution"),
            new TextLeaf("TextLocaleLanguageCode", "styleLocaleLanguageCode"),
            new TextLeaf("TextLocaleScriptCode", "styleLocaleScriptCode"),
            new TextLeaf("TextLocaleCountryCode", "styleLocaleCountryCode"),
            new TextLeaf("TextBackground", "styleBackground"),
            new TextLeaf("TextShadows", "styleShadows"),
            new TextLeaf("TextFontFeatures", "styleFontFeatures"),
            new TextLeaf("TextFontVariations", "styleFontVariations"),
            new TextLeaf("TextDecorationUnderline", "styleDecorationUnderline"),
            new TextLeaf("TextDecorationOverline", "styleDecorationOverline"),
            new TextLeaf("TextDecorationLineThrough", "styleDecorationLineThrough"),
            new TextLeaf("TextDecorationColor", "styleDecorationColor"),
            new TextLeaf("TextDecorationStyle", "styleDecorationStyle"),
            new TextLeaf("TextDecorationThickness", "styleDecorationThickness"),
            new TextLeaf("TextFontFamily", "styleFontFamily"),
            new TextLeaf("TextFontFamilyFallback", "styleFontFamilyFallback"),
            new TextLeaf("TextPackage", "stylePackage"),
            new TextLeaf("TextOverflow", "styleOverflow"));

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private ElevatedButtonWidgetPropertySchema() {
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

        direct(values, "enabled", Group.BEHAVIOR, "Enabled",
                "Whether activation is enabled. Generation emits null callbacks when disabled "
                + "and owns the deterministic no-op used by a new enabled button.",
                Target.ACTIVATION, 0);
        direct(values, "onPressed", Group.EVENTS, "On pressed",
                "Validated identifier of the zero-argument press callback.",
                Target.ACTIVATION, 1);
        direct(values, "onLongPress", Group.EVENTS, "On long press",
                "Validated identifier of the zero-argument long-press callback.",
                Target.ACTIVATION, 2);
        direct(values, "onHover", Group.EVENTS, "On hover",
                "Validated identifier of the callback that receives the hovered state.",
                Target.DIRECT, 3);
        direct(values, "onFocusChange", Group.EVENTS, "On focus change",
                "Validated identifier of the callback that receives the focused state.",
                Target.DIRECT, 4);
        direct(values, "autofocus", Group.BEHAVIOR, "Autofocus",
                "Request focus when no other node in the focus scope is already focused.",
                Target.DIRECT, 5);
        direct(values, "clipBehavior", Group.BEHAVIOR, "Clip behavior",
                "How content is clipped to the resolved button shape. Omission uses Clip.antiAlias when either effective local/theme layer builder exists, otherwise Clip.none. An explicit Clip value wins.",
                Target.DIRECT, 6);

        for (StateProjection state : STATES) {
            state(values, state);
        }

        common(values, "styleVisualDensityHorizontal", "Horizontal density",
                "Horizontal VisualDensity adjustment from -4 through 4.",
                "visualDensity.horizontal", 0);
        common(values, "styleVisualDensityVertical", "Vertical density",
                "Vertical VisualDensity adjustment from -4 through 4.",
                "visualDensity.vertical", 1);
        common(values, "styleTapTargetSize", "Tap target size",
                "Material tap-target sizing policy.", "tapTargetSize", 2);
        common(values, "styleAnimationDurationMs", "Animation duration",
                "Non-negative ButtonStyle animation duration in milliseconds.",
                "animationDuration.milliseconds", 3);
        common(values, "styleEnableFeedback", "Enable feedback",
                "Whether gestures provide platform acoustic or haptic feedback.",
                "enableFeedback", 4);
        common(values, "styleAlignmentKind", "Alignment kind",
                "Closed AlignmentGeometry constructor kind.", "alignment.kind", 5);
        common(values, "styleAlignmentX", "Alignment X",
                "Finite horizontal coordinate for the selected alignment kind.",
                "alignment.x", 6);
        common(values, "styleAlignmentY", "Alignment Y",
                "Finite vertical coordinate for the selected alignment kind.",
                "alignment.y", 7);
        common(values, "styleSplashFactory", "Splash factory",
                "Closed Material ink-feature factory preset.", "splashFactory", 8);
        common(values, "styleBackgroundBuilder", "Background builder",
                "Strict non-null ButtonLayerBuilder project reference for the background layer. Reset omits this nullable SDK style field and preserves theme inheritance; it does not clear the foreground builder. Canvas cannot execute project builders.",
                "backgroundBuilder", 9);
        common(values, "styleForegroundBuilder", "Foreground builder",
                "Strict non-null ButtonLayerBuilder project reference for the foreground layer. Reset omits this nullable SDK style field and preserves theme inheritance; it does not clear the background builder. Canvas cannot execute project builders.",
                "foregroundBuilder", 10);

        if (values.size() != FLATTENED_PROPERTY_COUNT) {
            throw new IllegalStateException(
                    "ElevatedButton schema must contain exactly "
                    + FLATTENED_PROPERTY_COUNT + " properties, found " + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void state(
            Map<String, Definition> values,
            StateProjection state) {
        int order = 0;
        state(values, state, "BackgroundColor", "Background color",
                "Button material fill color.", "backgroundColor", order++);
        state(values, state, "ForegroundColor", "Foreground color",
                "Color for text and icon descendants.", "foregroundColor", order++);
        state(values, state, "OverlayColor", "Overlay color",
                "Interaction overlay color.", "overlayColor", order++);
        state(values, state, "ShadowColor", "Shadow color",
                "Material elevation shadow color.", "shadowColor", order++);
        state(values, state, "SurfaceTintColor", "Surface tint color",
                "Material surface tint color.", "surfaceTintColor", order++);
        state(values, state, "Elevation", "Elevation",
                "Non-negative Material elevation.", "elevation", order++);
        state(values, state, "Padding", "Padding",
                "Physical or directional padding between the boundary and child.",
                "padding", order++);
        state(values, state, "MinimumWidth", "Minimum width",
                "Non-negative minimum width.", "minimumSize.width", order++);
        state(values, state, "MinimumHeight", "Minimum height",
                "Non-negative minimum height.", "minimumSize.height", order++);
        state(values, state, "FixedWidth", "Fixed width",
                "Non-negative fixed width.", "fixedSize.width", order++);
        state(values, state, "FixedHeight", "Fixed height",
                "Non-negative fixed height.", "fixedSize.height", order++);
        state(values, state, "MaximumWidth", "Maximum width",
                "Non-negative maximum width.", "maximumSize.width", order++);
        state(values, state, "MaximumHeight", "Maximum height",
                "Non-negative maximum height.", "maximumSize.height", order++);
        state(values, state, "IconColor", "Icon color",
                "Color for icon descendants.", "iconColor", order++);
        state(values, state, "IconSize", "Icon size",
                "Non-negative logical icon size.", "iconSize", order++);
        state(values, state, "SideColor", "Side color",
                "Outlined border side color.", "side.color", order++);
        state(values, state, "SideWidth", "Side width",
                "Non-negative outlined border side width.", "side.width", order++);
        state(values, state, "SideStyle", "Side style",
                "BorderSide none or solid.", "side.style", order++);
        state(values, state, "SideStrokeAlign", "Side stroke alignment",
                "Finite BorderSide stroke alignment. Flutter's inside (-1), "
                + "center (0), and outside (1) constants are presets, not bounds.",
                "side.strokeAlign", order++);
        state(values, state, "ShapeKind", "Shape kind",
                "Closed OutlinedBorder constructor preset.", "shape.kind", order++);
        state(values, state, "ShapeRadiusTopLeft", "Shape top-left radius",
                "Non-negative circular top-left radius.",
                "shape.radius.topLeft", order++);
        state(values, state, "ShapeRadiusTopRight", "Shape top-right radius",
                "Non-negative circular top-right radius.",
                "shape.radius.topRight", order++);
        state(values, state, "ShapeRadiusBottomRight", "Shape bottom-right radius",
                "Non-negative circular bottom-right radius.",
                "shape.radius.bottomRight", order++);
        state(values, state, "ShapeRadiusBottomLeft", "Shape bottom-left radius",
                "Non-negative circular bottom-left radius.",
                "shape.radius.bottomLeft", order++);
        state(values, state, "ShapeCircleEccentricity", "Circle eccentricity",
                "CircleBorder eccentricity from zero through one.",
                "shape.circleEccentricity", order++);
        state(values, state, "MouseCursor", "Mouse cursor",
                "Closed system mouse-cursor preset.", "mouseCursor", order++);

        if (order != STATE_NON_TEXT_PROPERTY_COUNT) {
            throw new IllegalStateException("Unexpected ElevatedButton non-text state count");
        }
        for (TextLeaf leaf : TEXT_LEAVES) {
            text(values, state, leaf, order++);
        }
        if (order != STATE_PROPERTY_COUNT) {
            throw new IllegalStateException("Unexpected ElevatedButton state count");
        }
    }

    private static void text(
            Map<String, Definition> values,
            StateProjection state,
            TextLeaf leaf,
            int order) {
        TextWidgetPropertySchema.Definition source = TextWidgetPropertySchema
                .find(leaf.sourceName())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing TextStyle source property: " + leaf.sourceName()));
        Target target = switch (source.target()) {
            case TEXT_STYLE_THEME, TEXT_STYLE -> Target.STYLE_TEXT;
            case TEXT_STYLE_LOCALE -> Target.STYLE_TEXT_LOCALE;
            case TEXT_STYLE_DECORATION -> Target.STYLE_TEXT_DECORATION;
            default -> throw new IllegalStateException(
                    "Unexpected TextStyle source target for " + leaf.sourceName());
        };
        Encoding encoding = switch (source.encoding()) {
            case SCALAR -> Encoding.SCALAR;
            case NEWLINE_STRING_LIST -> Encoding.NEWLINE_STRING_LIST;
            case DECORATION_FLAG -> Encoding.DECORATION_FLAG;
        };
        String path = switch (source.target()) {
            case TEXT_STYLE_THEME -> "textStyle.theme";
            case TEXT_STYLE -> "textStyle." + source.dartName();
            case TEXT_STYLE_LOCALE -> "textStyle.locale." + source.dartName();
            case TEXT_STYLE_DECORATION ->
                "textStyle.decoration." + source.dartName();
            default -> throw new IllegalStateException(
                    "Unexpected TextStyle source target for " + leaf.sourceName());
        };
        add(values, state.prefix() + leaf.suffix(), state.group(),
                source.displayName(), source.description(), target, path, order, encoding);
    }

    private static void direct(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            int order) {
        add(values, name, group, displayName, description,
                target, name, order, Encoding.SCALAR);
    }

    private static void state(
            Map<String, Definition> values,
            StateProjection state,
            String suffix,
            String displayName,
            String description,
            String dartName,
            int order) {
        add(values, state.prefix() + suffix, state.group(), displayName,
                description + " Unset preserves the next Flutter fallback.",
                Target.STYLE_STATE, dartName, order, Encoding.SCALAR);
    }

    private static void common(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        add(values, name, Group.COMMON_STYLE, displayName, description,
                Target.STYLE_COMMON, dartName, order, Encoding.SCALAR);
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
                group, displayName, description, target,
                dartName, order, encoding)) != null) {
            throw new IllegalStateException(
                    "Duplicate ElevatedButton property schema: " + name);
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

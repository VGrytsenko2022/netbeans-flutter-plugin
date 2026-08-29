package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Designer-facing typed projection of the compound values accepted by
 * {@code Text}. The {@code .fd} schema keeps each editable property typed and
 * independently resettable; Dart generation and the native Canvas assemble
 * them into {@code TextStyle}, {@code StrutStyle}, {@code Locale},
 * {@code TextScaler}, and {@code TextHeightBehavior} values.
 */
public final class TextWidgetPropertySchema {
    public static final WidgetTypeId TEXT_TYPE = new WidgetTypeId("flutter.widgets.Text");

    public enum Group {
        CONTENT("textContent", "Text", "Text content and paragraph layout."),
        ACCESSIBILITY("textAccessibility", "Accessibility", "Semantic text exposed to assistive technologies."),
        LOCALE_AND_SCALING("textLocaleScaling", "Locale and scaling", "Locale, text scaling, and line-height behavior."),
        STYLE("textStyle", "Text style", "Fields assembled into the optional Flutter TextStyle."),
        STYLE_PAINT("textStylePaint", "Paint and effects",
                "Foreground and background paints, shadows, and decoration paint."),
        STYLE_TYPOGRAPHY("textStyleTypography", "Advanced typography",
                "OpenType font features and variable-font axes."),
        STRUT("textStrut", "Strut style", "Fields assembled into the optional Flutter StrutStyle.");

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
        TEXT_LOCALE,
        TEXT_SCALER,
        TEXT_HEIGHT_BEHAVIOR,
        TEXT_STYLE_THEME,
        TEXT_STYLE,
        TEXT_STYLE_LOCALE,
        TEXT_STYLE_DECORATION,
        STRUT_STYLE
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

    private TextWidgetPropertySchema() {
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

        direct(values, "data", Group.CONTENT, "Data", "Text displayed by the widget.");
        direct(values, "textAlign", Group.CONTENT, "Text align", "Horizontal alignment of the paragraph.");
        direct(values, "textDirection", Group.CONTENT, "Text direction", "Explicit right-to-left or left-to-right direction.");
        direct(values, "softWrap", Group.CONTENT, "Soft wrap", "Whether text may wrap at soft line breaks.");
        direct(values, "overflow", Group.CONTENT, "Overflow", "How visual overflow is handled.");
        direct(values, "maxLines", Group.CONTENT, "Maximum lines", "Maximum number of displayed lines; must be greater than zero.");
        direct(values, "textWidthBasis", Group.CONTENT, "Width basis", "How paragraph width is measured.");
        direct(values, "selectionColor", Group.CONTENT, "Selection color",
                "Literal ARGB color or semantic Material ColorScheme role for the selection highlight.");
        direct(values, "semanticsLabel", Group.ACCESSIBILITY, "Semantics label", "Alternative spoken label for this text.");
        direct(values, "semanticsIdentifier", Group.ACCESSIBILITY, "Semantics identifier", "Unique identifier for the semantics node.");

        compound(values, "localeLanguageCode", Group.LOCALE_AND_SCALING, "Language code",
                "Unicode locale language subtag; omitted means und when another subtag is set.",
                Target.TEXT_LOCALE, "languageCode", 0);
        compound(values, "localeScriptCode", Group.LOCALE_AND_SCALING, "Script code",
                "Unicode locale script subtag, for example Latn.",
                Target.TEXT_LOCALE, "scriptCode", 1);
        compound(values, "localeCountryCode", Group.LOCALE_AND_SCALING, "Country code",
                "Unicode locale region subtag, for example US.",
                Target.TEXT_LOCALE, "countryCode", 2);
        compound(values, "textScalerFactor", Group.LOCALE_AND_SCALING, "Scale factor",
                "Linear TextScaler factor; zero or greater.",
                Target.TEXT_SCALER, "textScaleFactor", 0);
        compound(values, "textHeightApplyFirstAscent", Group.LOCALE_AND_SCALING,
                "Height on first ascent", "Apply the TextStyle height to the first line ascent.",
                Target.TEXT_HEIGHT_BEHAVIOR, "applyHeightToFirstAscent", 0);
        compound(values, "textHeightApplyLastDescent", Group.LOCALE_AND_SCALING,
                "Height on last descent", "Apply the TextStyle height to the last line descent.",
                Target.TEXT_HEIGHT_BEHAVIOR, "applyHeightToLastDescent", 1);
        compound(values, "textHeightLeadingDistribution", Group.LOCALE_AND_SCALING,
                "Height leading distribution", "Distribution of leading above and below text.",
                Target.TEXT_HEIGHT_BEHAVIOR, "leadingDistribution", 2);

        compound(values, "styleThemeTextStyle", Group.STYLE, "Theme text style",
                "Semantic TextTheme role used as the base style; explicit fields override it.",
                Target.TEXT_STYLE_THEME, "textStyle", 0);
        style(values, "styleInherit", "Inherit", "Merge unspecified fields with DefaultTextStyle.", "inherit", 0);
        style(values, "styleColor", "Color",
                "Literal ARGB color or semantic Material ColorScheme role for glyphs.", "color", 1);
        style(values, "styleBackgroundColor", "Background color",
                "Literal ARGB color or semantic Material ColorScheme role painted behind text.",
                "backgroundColor", 2);
        style(values, "styleFontSize", "Font size", "Font size in logical pixels.", "fontSize", 3);
        style(values, "styleFontWeight", "Font weight", "FontWeight preset from w100 through w900.", "fontWeight", 4);
        style(values, "styleFontStyle", "Font style", "Normal or italic glyph style.", "fontStyle", 5);
        style(values, "styleLetterSpacing", "Letter spacing", "Additional logical pixels between letters; may be negative.", "letterSpacing", 6);
        style(values, "styleWordSpacing", "Word spacing", "Additional logical pixels between words; may be negative.", "wordSpacing", 7);
        style(values, "styleTextBaseline", "Text baseline", "Alphabetic or ideographic baseline.", "textBaseline", 8);
        style(values, "styleHeight", "Line height", "Line height as a multiple of the font size.", "height", 9);
        style(values, "styleLeadingDistribution", "Leading distribution", "Distribution of extra line height.", "leadingDistribution", 10);
        compound(values, "styleLocaleLanguageCode", Group.STYLE, "Locale language code",
                "Language subtag for TextStyle.locale.", Target.TEXT_STYLE_LOCALE, "languageCode", 0);
        compound(values, "styleLocaleScriptCode", Group.STYLE, "Locale script code",
                "Script subtag for TextStyle.locale.", Target.TEXT_STYLE_LOCALE, "scriptCode", 1);
        compound(values, "styleLocaleCountryCode", Group.STYLE, "Locale country code",
                "Region subtag for TextStyle.locale.", Target.TEXT_STYLE_LOCALE, "countryCode", 2);
        style(values, Group.STYLE_PAINT, "styleForeground", "Foreground paint",
                "Paint used for glyph foreground; mutually exclusive with Color.", "foreground", 12);
        style(values, Group.STYLE_PAINT, "styleBackground", "Background paint",
                "Paint used behind glyphs; mutually exclusive with Background color.", "background", 13);
        style(values, Group.STYLE_PAINT, "styleShadows", "Shadows",
                "Ordered Flutter Shadow values applied to the text.", "shadows", 14);
        style(values, Group.STYLE_TYPOGRAPHY, "styleFontFeatures", "Font features",
                "Ordered OpenType feature tags and values.", "fontFeatures", 15);
        style(values, Group.STYLE_TYPOGRAPHY, "styleFontVariations", "Font variations",
                "Ordered variable-font axis tags and values.", "fontVariations", 16);
        decoration(values, "styleDecorationUnderline", "Underline", "Draw an underline decoration.", "underline", 0);
        decoration(values, "styleDecorationOverline", "Overline", "Draw an overline decoration.", "overline", 1);
        decoration(values, "styleDecorationLineThrough", "Line through", "Draw a line-through decoration.", "lineThrough", 2);
        style(values, Group.STYLE_PAINT, "styleDecorationColor", "Decoration color",
                "Literal ARGB color or semantic Material ColorScheme role for decoration lines.",
                "decorationColor", 18);
        style(values, "styleDecorationStyle", "Decoration style", "Stroke style of text decoration lines.", "decorationStyle", 19);
        style(values, "styleDecorationThickness", "Decoration thickness", "Multiplier for decoration line thickness.", "decorationThickness", 20);
        style(values, "styleDebugLabel", "Debug label", "Optional diagnostic label emitted into TextStyle.", "debugLabel", 21);
        style(values, "styleFontFamily", "Font family", "Primary font family name.", "fontFamily", 22);
        compound(values, "styleFontFamilyFallback", Group.STYLE, "Font fallbacks",
                "Fallback font families, one per line.", Target.TEXT_STYLE, "fontFamilyFallback", 23,
                Encoding.NEWLINE_STRING_LIST);
        style(values, "stylePackage", "Font package", "Package that provides the configured font family.", "package", 24);
        style(values, "styleOverflow", "Style overflow", "Overflow preference stored in TextStyle.", "overflow", 25);

        strut(values, "strutFontFamily", "Font family", "Primary strut font family.", "fontFamily", 0);
        compound(values, "strutFontFamilyFallback", Group.STRUT, "Font fallbacks",
                "Fallback strut font families, one per line.", Target.STRUT_STYLE,
                "fontFamilyFallback", 1, Encoding.NEWLINE_STRING_LIST);
        strut(values, "strutFontSize", "Font size", "Strut font size; must be greater than zero.", "fontSize", 2);
        strut(values, "strutHeight", "Line height", "Strut line-height multiplier.", "height", 3);
        strut(values, "strutLeadingDistribution", "Leading distribution", "Distribution of strut leading.", "leadingDistribution", 4);
        strut(values, "strutLeading", "Leading", "Additional leading; zero or greater.", "leading", 5);
        strut(values, "strutFontWeight", "Font weight", "Strut FontWeight preset.", "fontWeight", 6);
        strut(values, "strutFontStyle", "Font style", "Normal or italic strut style.", "fontStyle", 7);
        strut(values, "strutForceHeight", "Force strut height", "Force every line to the strut metrics.", "forceStrutHeight", 8);
        strut(values, "strutDebugLabel", "Debug label", "Optional diagnostic label emitted into StrutStyle.", "debugLabel", 9);
        strut(values, "strutPackage", "Font package", "Package that provides the strut font family.", "package", 10);

        return Map.copyOf(values);
    }

    private static void direct(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description) {
        add(values, name, new Definition(group, displayName, description,
                Target.DIRECT, name, 0, Encoding.SCALAR));
    }

    private static void style(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, Group.STYLE, displayName, description,
                Target.TEXT_STYLE, dartName, order);
    }

    private static void style(
            Map<String, Definition> values,
            Group group,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, group, displayName, description,
                Target.TEXT_STYLE, dartName, order);
    }

    private static void strut(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, Group.STRUT, displayName, description,
                Target.STRUT_STYLE, dartName, order);
    }

    private static void decoration(
            Map<String, Definition> values,
            String name,
            String displayName,
            String description,
            String dartName,
            int order) {
        compound(values, name, Group.STYLE, displayName, description,
                Target.TEXT_STYLE_DECORATION, dartName, order,
                Encoding.DECORATION_FLAG);
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
        compound(values, name, group, displayName, description, target,
                dartName, order, Encoding.SCALAR);
    }

    private static void compound(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int order,
            Encoding encoding) {
        add(values, name, new Definition(group, displayName, description,
                target, dartName, order, encoding));
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Definition definition) {
        if (values.putIfAbsent(name, definition) != null) {
            throw new IllegalStateException("Duplicate Text property schema entry: " + name);
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

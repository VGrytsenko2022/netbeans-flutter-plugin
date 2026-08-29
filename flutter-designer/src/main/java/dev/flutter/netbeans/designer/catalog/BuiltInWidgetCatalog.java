package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The intentionally small, reviewed widget set for the first usable designer slice. */
public final class BuiltInWidgetCatalog {
    public static final String PREFERRED_SIZE_WIDGET_TRAIT = "flutter.widgets.PreferredSizeWidget";

    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final SlotAcceptance ANY_WIDGET = new SlotAcceptance.AnyWidget();
    private static final WidgetCatalog INSTANCE = WidgetCatalog.strict(List.of(
            scaffold(),
            appBar(),
            column(),
            row(),
            padding(),
            center(),
            text(),
            icon(),
            sizedBox(),
            elevatedButton()));

    private BuiltInWidgetCatalog() {
    }

    public static WidgetCatalog getDefault() {
        return INSTANCE;
    }

    private static WidgetDefinition scaffold() {
        return widget(
                "flutter.material.Scaffold",
                "Scaffold",
                true,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT),
                Set.of(),
                palette("flutter.material", 100, 10, "Scaffold"),
                List.of(
                        namedProperty("backgroundColor", 3, false, any(PropertyValueKind.COLOR)),
                        namedProperty("resizeToAvoidBottomInset", 4, false, any(PropertyValueKind.BOOLEAN))),
                List.of(
                        singleSlot("appBar", 0, false, 0,
                                new SlotAcceptance.HasTrait(PREFERRED_SIZE_WIDGET_TRAIT)),
                        singleSlot("body", 1, false, 0, ANY_WIDGET),
                        singleSlot("floatingActionButton", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition appBar() {
        return widget(
                "flutter.material.AppBar",
                "AppBar",
                false,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT),
                Set.of(PREFERRED_SIZE_WIDGET_TRAIT),
                palette("flutter.material", 100, 20, "AppBar"),
                List.of(
                        namedProperty("backgroundColor", 3, false, any(PropertyValueKind.COLOR)),
                        namedProperty("centerTitle", 4, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("elevation", 5, false, nonNegativeNumbers())),
                List.of(
                        singleSlot("leading", 0, false, 0, ANY_WIDGET),
                        singleSlot("title", 1, false, 0, ANY_WIDGET),
                        listSlot("actions", 2, false, ANY_WIDGET)));
    }

    private static WidgetDefinition column() {
        return flexWidget("flutter.widgets.Column", "Column", 10);
    }

    private static WidgetDefinition row() {
        return flexWidget("flutter.widgets.Row", "Row", 20);
    }

    private static WidgetDefinition flexWidget(String typeId, String className, int itemOrder) {
        return widget(
                typeId,
                className,
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, itemOrder, className),
                List.of(
                        namedProperty("mainAxisAlignment", 0, false, enumValues(
                                "MainAxisAlignment", "start", "end", "center",
                                "spaceBetween", "spaceAround", "spaceEvenly")),
                        namedProperty("mainAxisSize", 1, false,
                                enumValues("MainAxisSize", "min", "max")),
                        namedProperty("crossAxisAlignment", 2, false,
                                enumValues("CrossAxisAlignment", "start", "end", "center", "stretch",
                                        "baseline")),
                        namedProperty("textDirection", 3, false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty("verticalDirection", 4, false,
                                enumValues("VerticalDirection", "up", "down")),
                        namedProperty("textBaseline", 5, false,
                                enumValues("TextBaseline", "alphabetic", "ideographic")),
                        namedProperty("spacing", 6, false, nonNegativeDoubles())),
                List.of(listSlot("children", 7, false, ANY_WIDGET)));
    }

    private static WidgetDefinition padding() {
        PropertyValue.EdgeInsetsValue initialPadding = new PropertyValue.EdgeInsetsValue(
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(16));
        return widget(
                "flutter.widgets.Padding",
                "Padding",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 30, "Padding"),
                List.of(new PropertyDefinition(
                        new PropertyName("padding"),
                        DartParameter.named(0, true),
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(true)),
                        Optional.of(initialPadding))),
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition center() {
        return widget(
                "flutter.widgets.Center",
                "Center",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 40, "Center"),
                List.of(
                        namedProperty("widthFactor", 0, false, nonNegativeNumbers()),
                        namedProperty("heightFactor", 1, false, nonNegativeNumbers())),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition text() {
        return widget(
                "flutter.widgets.Text",
                "Text",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 10, "Text"),
                List.of(
                        new PropertyDefinition(
                                new PropertyName("data"),
                                DartParameter.positional(0),
                                any(PropertyValueKind.STRING),
                                Optional.of(new PropertyValue.StringValue("Text"))),
                        namedProperty("textAlign", 0, false,
                                enumValues("TextAlign", "start", "end", "left", "right", "center", "justify")),
                        namedProperty("textDirection", 1, false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty("softWrap", 2, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("overflow", 3, false,
                                enumValues("TextOverflow", "clip", "fade", "ellipsis", "visible")),
                        namedProperty("maxLines", 4, false, List.of(
                                new PropertyValueConstraint.IntegerRange(
                                        BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER))),
                        namedProperty("semanticsLabel", 5, false, any(PropertyValueKind.STRING)),
                        namedProperty("semanticsIdentifier", 6, false, any(PropertyValueKind.STRING)),
                        namedProperty("textWidthBasis", 7, false,
                                enumValues("TextWidthBasis", "parent", "longestLine")),
                        namedProperty("selectionColor", 8, false, colorOrTheme()),

                        // Text.locale, projected as independently editable subtags.
                        namedProperty("localeLanguageCode", 9, false, localeLanguageCode()),
                        namedProperty("localeScriptCode", 10, false, localeScriptCode()),
                        namedProperty("localeCountryCode", 11, false, localeCountryCode()),
                        namedProperty("textScalerFactor", 12, false, nonNegativeDoubles()),
                        namedProperty("textHeightApplyFirstAscent", 13, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("textHeightApplyLastDescent", 14, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("textHeightLeadingDistribution", 15, false,
                                enumValues("TextLeadingDistribution", "proportional", "even")),

                        // Text.style -> a semantic TextTheme base with independently
                        // resettable, typed local overrides.
                        namedProperty("styleThemeTextStyle", 52, false, textStyleTheme()),
                        namedProperty("styleInherit", 16, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("styleColor", 17, false, colorOrTheme()),
                        namedProperty("styleBackgroundColor", 18, false, colorOrTheme()),
                        namedProperty("styleFontSize", 19, false, nonNegativeDoubles()),
                        namedProperty("styleFontWeight", 20, false,
                                enumValues("FontWeight", "w100", "w200", "w300", "w400", "w500",
                                        "w600", "w700", "w800", "w900")),
                        namedProperty("styleFontStyle", 21, false,
                                enumValues("FontStyle", "normal", "italic")),
                        namedProperty("styleLetterSpacing", 22, false, finiteDoubles()),
                        namedProperty("styleWordSpacing", 23, false, finiteDoubles()),
                        namedProperty("styleTextBaseline", 24, false,
                                enumValues("TextBaseline", "alphabetic", "ideographic")),
                        namedProperty("styleHeight", 25, false, finiteDoubles()),
                        namedProperty("styleLeadingDistribution", 26, false,
                                enumValues("TextLeadingDistribution", "proportional", "even")),
                        namedProperty("styleLocaleLanguageCode", 27, false, localeLanguageCode()),
                        namedProperty("styleLocaleScriptCode", 28, false, localeScriptCode()),
                        namedProperty("styleLocaleCountryCode", 29, false, localeCountryCode()),
                        namedProperty("styleDecorationUnderline", 30, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("styleDecorationOverline", 31, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("styleDecorationLineThrough", 32, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("styleForeground", 53, false, paintValues()),
                        namedProperty("styleBackground", 54, false, paintValues()),
                        namedProperty("styleShadows", 55, false, shadowValues()),
                        namedProperty("styleFontFeatures", 56, false,
                                any(PropertyValueKind.FONT_FEATURE_LIST)),
                        namedProperty("styleFontVariations", 57, false,
                                fontVariationValues()),
                        namedProperty("styleDecorationColor", 33, false, colorOrTheme()),
                        namedProperty("styleDecorationStyle", 34, false,
                                enumValues("TextDecorationStyle", "solid", "double", "dotted", "dashed", "wavy")),
                        namedProperty("styleDecorationThickness", 35, false, finiteDoubles()),
                        namedProperty("styleDebugLabel", 36, false, any(PropertyValueKind.STRING)),
                        namedProperty("styleFontFamily", 37, false, stringLength(1, 256)),
                        namedProperty("styleFontFamilyFallback", 38, false, stringLength(0, 4096)),
                        namedProperty("stylePackage", 39, false, stringLength(1, 256)),
                        namedProperty("styleOverflow", 40, false,
                                enumValues("TextOverflow", "clip", "fade", "ellipsis", "visible")),

                        // Text.strutStyle -> StrutStyle(...).
                        namedProperty("strutFontFamily", 41, false, stringLength(1, 256)),
                        namedProperty("strutFontFamilyFallback", 42, false, stringLength(0, 4096)),
                        namedProperty("strutFontSize", 43, false, positiveDoubles()),
                        namedProperty("strutHeight", 44, false, finiteDoubles()),
                        namedProperty("strutLeadingDistribution", 45, false,
                                enumValues("TextLeadingDistribution", "proportional", "even")),
                        namedProperty("strutLeading", 46, false, nonNegativeDoubles()),
                        namedProperty("strutFontWeight", 47, false,
                                enumValues("FontWeight", "w100", "w200", "w300", "w400", "w500",
                                        "w600", "w700", "w800", "w900")),
                        namedProperty("strutFontStyle", 48, false,
                                enumValues("FontStyle", "normal", "italic")),
                        namedProperty("strutForceHeight", 49, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("strutDebugLabel", 50, false, any(PropertyValueKind.STRING)),
                        namedProperty("strutPackage", 51, false, stringLength(1, 256))),
                List.of());
    }

    private static WidgetDefinition icon() {
        return widget(
                "flutter.widgets.Icon",
                "Icon",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 20, "Icon"),
                List.of(
                        new PropertyDefinition(
                                new PropertyName("icon"),
                                DartParameter.positional(0),
                                List.of(new PropertyValueConstraint.MaterialIconValues()),
                                Optional.of(new PropertyValue.IconDataValue(
                                        Optional.of(0xE5F9),
                                        Optional.of("MaterialIcons"),
                                        Optional.empty(),
                                        false,
                                        List.of()))),
                        namedProperty("size", 0, false, nonNegativeNumbers()),
                        namedProperty("fill", 1, false, zeroToOneDoubles()),
                        namedProperty("weight", 2, false, positiveFontAxisDoubles()),
                        namedProperty("grade", 3, false, gradeAxisDoubles()),
                        namedProperty("opticalSize", 4, false, positiveFontAxisDoubles()),
                        namedProperty("color", 5, false, colorOrTheme()),
                        namedProperty("shadows", 6, false, shadowValues()),
                        namedProperty("semanticLabel", 7, false, any(PropertyValueKind.STRING)),
                        namedProperty("textDirection", 8, false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty("applyTextScaling", 9, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("blendMode", 10, false, enumValues(
                                "BlendMode", "clear", "src", "dst", "srcOver", "dstOver",
                                "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                                "xor", "plus", "modulate", "screen", "overlay", "darken",
                                "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                                "difference", "exclusion", "multiply", "hue", "saturation",
                                "color", "luminosity")),
                        namedProperty("fontWeight", 11, false,
                                enumValues("FontWeight", "w100", "w200", "w300", "w400",
                                        "w500", "w600", "w700", "w800", "w900"))),
                List.of());
    }

    private static WidgetDefinition sizedBox() {
        return widget(
                "flutter.widgets.SizedBox",
                "SizedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 50, "SizedBox"),
                List.of(
                        namedProperty("width", 0, false, nonNegativeNumbers()),
                        namedProperty("height", 1, false, nonNegativeNumbers())),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition elevatedButton() {
        return widget(
                "flutter.material.ElevatedButton",
                "ElevatedButton",
                true,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT),
                Set.of(),
                palette("flutter.material", 100, 30, "Elevated Button"),
                List.of(new PropertyDefinition(
                        new PropertyName("onPressed"),
                        DartParameter.named(0, true),
                        List.of(
                                new PropertyValueConstraint.CallbackReference(),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.DART_EXPRESSION)),
                        Optional.of(new PropertyValue.DartExpressionValue("null")))),
                // Flutter marks child as a required named but nullable argument.
                List.of(singleSlot("child", 1, true, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition widget(
            String typeId,
            String dartClassName,
            boolean constConstructor,
            String dartLibraryUri,
            List<String> imports,
            Set<String> traits,
            PaletteMetadata palette,
            List<PropertyDefinition> properties,
            List<SlotDefinition> slots) {
        return new WidgetDefinition(
                new WidgetTypeId(typeId),
                dartClassName,
                Optional.empty(),
                constConstructor,
                dartLibraryUri,
                imports,
                traits,
                palette,
                properties,
                slots);
    }

    private static PaletteMetadata palette(
            String categoryId,
            int categoryOrder,
            int itemOrder,
            String displayName) {
        return new PaletteMetadata(categoryId, categoryOrder, itemOrder, displayName);
    }

    private static PropertyDefinition namedProperty(
            String name,
            int order,
            boolean required,
            List<PropertyValueConstraint> constraints) {
        return new PropertyDefinition(
                new PropertyName(name),
                DartParameter.named(order, required),
                constraints,
                Optional.empty());
    }

    private static SlotDefinition singleSlot(
            String name,
            int order,
            boolean requiredArgument,
            int minimumChildren,
            SlotAcceptance acceptance) {
        return new SlotDefinition(
                new SlotName(name),
                DartParameter.named(order, requiredArgument),
                SlotCardinality.SINGLE,
                minimumChildren,
                1,
                acceptance);
    }

    private static SlotDefinition listSlot(
            String name,
            int order,
            boolean requiredArgument,
            SlotAcceptance acceptance) {
        return new SlotDefinition(
                new SlotName(name),
                DartParameter.named(order, requiredArgument),
                SlotCardinality.LIST,
                0,
                10_000,
                acceptance);
    }

    private static List<PropertyValueConstraint> any(PropertyValueKind kind) {
        return List.of(new PropertyValueConstraint.AnyValue(kind));
    }

    private static List<PropertyValueConstraint> colorOrTheme() {
        return List.of(
                new PropertyValueConstraint.AnyValue(PropertyValueKind.COLOR),
                new PropertyValueConstraint.ThemeTokenValues(
                        MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()));
    }

    private static List<PropertyValueConstraint> textStyleTheme() {
        return List.of(new PropertyValueConstraint.ThemeTokenValues(
                MaterialThemeTokenCatalog.textStyleRoles().keySet().stream().sorted().toList()));
    }

    private static List<PropertyValueConstraint> paintValues() {
        return List.of(new PropertyValueConstraint.PaintValues(
                MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()));
    }

    private static List<PropertyValueConstraint> shadowValues() {
        return List.of(new PropertyValueConstraint.ShadowListValues(
                MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()));
    }

    private static List<PropertyValueConstraint> fontVariationValues() {
        return List.of(new PropertyValueConstraint.FontVariationListValues());
    }

    private static List<PropertyValueConstraint> nonNegativeNumbers() {
        return List.of(
                new PropertyValueConstraint.IntegerRange(
                        BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, true, null, true));
    }

    private static List<PropertyValueConstraint> nonNegativeDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, true, null, true));
    }

    private static List<PropertyValueConstraint> positiveDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, false, null, true));
    }

    private static List<PropertyValueConstraint> zeroToOneDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, true, BigDecimal.ONE, true));
    }

    private static List<PropertyValueConstraint> positiveFontAxisDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.ZERO, false, BigDecimal.valueOf(32768), false));
    }

    private static List<PropertyValueConstraint> gradeAxisDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.valueOf(-32768), true,
                BigDecimal.valueOf(32768), false));
    }

    private static List<PropertyValueConstraint> finiteDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                null, true, null, true));
    }

    private static List<PropertyValueConstraint> stringLength(int minimum, int maximum) {
        return List.of(new PropertyValueConstraint.StringLength(minimum, maximum));
    }

    private static List<PropertyValueConstraint> localeLanguageCode() {
        return stringPattern(
                "(?:[a-z]{2,3}|[a-z]{5,8})",
                "locale language code: two or three, or five through eight lowercase letters");
    }

    private static List<PropertyValueConstraint> localeScriptCode() {
        return stringPattern(
                "[A-Z][a-z]{3}",
                "locale script code: one uppercase letter followed by three lowercase letters");
    }

    private static List<PropertyValueConstraint> localeCountryCode() {
        return stringPattern(
                "(?:[A-Z]{2}|[0-9]{3})",
                "locale country code: two uppercase letters or three digits");
    }

    private static List<PropertyValueConstraint> stringPattern(
            String regularExpression,
            String description) {
        return List.of(new PropertyValueConstraint.StringPattern(
                regularExpression, description));
    }

    private static List<PropertyValueConstraint> enumValues(String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(WIDGETS_IMPORT, type), List.of(values)));
    }
}

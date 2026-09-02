package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The intentionally small, reviewed widget set for the first usable designer slice. */
public final class BuiltInWidgetCatalog {
    public static final String PREFERRED_SIZE_WIDGET_TRAIT = "flutter.widgets.PreferredSizeWidget";

    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String SERVICES_IMPORT = "package:flutter/services.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String DART_UI_IMPORT = "dart:ui";
    private static final SlotAcceptance ANY_WIDGET = new SlotAcceptance.AnyWidget();
    private static final WidgetCatalog INSTANCE = WidgetCatalog.strict(List.of(
            scaffold(),
            appBar(),
            column(),
            row(),
            wrap(),
            padding(),
            center(),
            text(),
            icon(),
            sizedBox(),
            aspectRatio(),
            container(),
            opacity(),
            align(),
            fractionallySizedBox(),
            fittedBox(),
            constrainedBox(),
            unconstrainedBox(),
            limitedBox(),
            overflowBox(),
            stack(),
            expanded(),
            flexible(),
            spacer(),
            baseline(),
            intrinsicHeight(),
            listView(),
            image(),
            elevatedButton(),
            textField()));

    private BuiltInWidgetCatalog() {
    }

    public static WidgetCatalog getDefault() {
        return INSTANCE;
    }

    private static WidgetDefinition scaffold() {
        List<PropertyDefinition> properties = new ArrayList<>();
        int order = 3;
        properties.add(namedProperty("floatingActionButtonLocation", order++, false,
                stringPattern(
                        "(?:startTop|miniStartTop|centerTop|miniCenterTop|endTop|miniEndTop|startFloat|miniStartFloat|centerFloat|miniCenterFloat|endFloat|miniEndFloat|startDocked|miniStartDocked|centerDocked|miniCenterDocked|endDocked|miniEndDocked|endContained)",
                        "reviewed FloatingActionButtonLocation static preset")));
        properties.add(namedProperty("floatingActionButtonAnimator", order++, false,
                stringPattern("(?:scaling|noAnimation)",
                        "reviewed FloatingActionButtonAnimator static preset")));
        properties.add(namedProperty("persistentFooterAlignment", order++, false,
                stringPattern(
                        "(?:topStart|topCenter|topEnd|centerStart|center|centerEnd|bottomStart|bottomCenter|bottomEnd)",
                        "reviewed AlignmentDirectional static preset")));
        properties.add(namedProperty("onDrawerChanged", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("onEndDrawerChanged", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("backgroundColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("resizeToAvoidBottomInset", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("primary", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("drawerDragStartBehavior", order++, false,
                gesturesEnumValues("DragStartBehavior", "down", "start")));
        properties.add(namedProperty("extendBody", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("drawerBarrierDismissible", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("extendBodyBehindAppBar", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("drawerScrimColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("drawerEdgeDragWidth", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty("drawerEnableOpenDragGesture", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("endDrawerEnableOpenDragGesture", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("restorationId", order++, false,
                stringLength(1, 256)));
        if (properties.size() != ScaffoldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Scaffold catalog/property schema count mismatch");
        }
        return widget(
                "flutter.material.Scaffold",
                "Scaffold",
                true,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, GESTURES_IMPORT),
                Set.of(),
                palette("flutter.material", 100, 10, "Scaffold"),
                List.copyOf(properties),
                List.of(
                        singleSlot("appBar", 0, false, 0,
                                new SlotAcceptance.HasTrait(PREFERRED_SIZE_WIDGET_TRAIT)),
                        singleSlot("body", 1, false, 0, ANY_WIDGET),
                        singleSlot("floatingActionButton", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition appBar() {
        List<PropertyDefinition> properties = new ArrayList<>();
        properties.add(namedProperty("backgroundColor", 3, false, colorOrTheme()));
        properties.add(namedProperty("centerTitle", 4, false, any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("elevation", 5, false, nonNegativeNumbers()));

        int order = 6;
        properties.add(namedProperty("automaticallyImplyLeading", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("automaticallyImplyActions", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("scrolledUnderElevation", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty("notificationPredicate", order++, false,
                stringPattern("(?:default|depthZero|all)",
                        "AppBar scroll-notification preset: default, depthZero, or all")));
        properties.add(namedProperty("shadowColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("surfaceTintColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("foregroundColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("primary", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("excludeHeaderSemantics", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("titleSpacing", order++, false, finiteDoubles()));
        properties.add(namedProperty("toolbarOpacity", order++, false, zeroToOneDoubles()));
        properties.add(namedProperty("bottomOpacity", order++, false, zeroToOneDoubles()));
        properties.add(namedProperty("toolbarHeight", order++, false, nonNegativeNumbers()));
        properties.add(namedProperty("leadingWidth", order++, false, nonNegativeNumbers()));
        properties.add(namedProperty("forceMaterialTransparency", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("useDefaultSemanticsOrder", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("clipBehavior", order++, false,
                enumValues("Clip", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer")));
        properties.add(namedProperty("actionsPadding", order++, false,
                List.of(new PropertyValueConstraint.EdgeInsetsValues(true))));
        properties.add(namedProperty("animateColor", order++, false,
                any(PropertyValueKind.BOOLEAN)));

        properties.add(namedProperty("shapeKind", order++, false,
                stringPattern("(?:roundedRectangle|stadium|circle|beveledRectangle|continuousRectangle)",
                        "closed AppBar ShapeBorder kind")));
        properties.add(namedProperty("shapeSideColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("shapeSideWidth", order++, false, nonNegativeDoubles()));
        properties.add(namedProperty("shapeSideStyle", order++, false,
                enumValues("BorderStyle", "none", "solid")));
        properties.add(namedProperty("shapeSideStrokeAlign", order++, false,
                finiteDoubles()));
        properties.add(namedProperty("shapeRadiusTopLeft", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("shapeRadiusTopRight", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("shapeRadiusBottomRight", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("shapeRadiusBottomLeft", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("shapeCircleEccentricity", order++, false,
                zeroToOneDoubles()));

        order = appendIconThemeProperties(properties, "iconTheme", order);
        order = appendIconThemeProperties(properties, "actionsIconTheme", order);
        order = appendTextStyleProperties(properties, "toolbarTextStyle", order);
        order = appendTextStyleProperties(properties, "titleTextStyle", order);

        properties.add(namedProperty("systemOverlayStyleSystemNavigationBarColor",
                order++, false, colorOrTheme()));
        properties.add(namedProperty("systemOverlayStyleSystemNavigationBarDividerColor",
                order++, false, colorOrTheme()));
        properties.add(namedProperty("systemOverlayStyleSystemNavigationBarIconBrightness",
                order++, false, enumValues("Brightness", "light", "dark")));
        properties.add(namedProperty("systemOverlayStyleSystemNavigationBarContrastEnforced",
                order++, false, any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("systemOverlayStyleStatusBarColor",
                order++, false, colorOrTheme()));
        properties.add(namedProperty("systemOverlayStyleStatusBarBrightness",
                order++, false, enumValues("Brightness", "light", "dark")));
        properties.add(namedProperty("systemOverlayStyleStatusBarIconBrightness",
                order++, false, enumValues("Brightness", "light", "dark")));
        properties.add(namedProperty("systemOverlayStyleSystemStatusBarContrastEnforced",
                order++, false, any(PropertyValueKind.BOOLEAN)));

        return widget(
                "flutter.material.AppBar",
                "AppBar",
                false,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, WIDGETS_IMPORT),
                Set.of(PREFERRED_SIZE_WIDGET_TRAIT),
                palette("flutter.material", 100, 20, "AppBar"),
                List.copyOf(properties),
                List.of(
                        singleSlot("leading", 0, false, 0, ANY_WIDGET),
                        singleSlot("title", 1, false, 0, ANY_WIDGET),
                        listSlot("actions", 2, false, ANY_WIDGET),
                        singleSlot("flexibleSpace", order, false, 0, ANY_WIDGET),
                        singleSlot("bottom", order + 1, false, 0,
                                new SlotAcceptance.HasTrait(PREFERRED_SIZE_WIDGET_TRAIT))));
    }

    private static int appendIconThemeProperties(
            List<PropertyDefinition> properties,
            String prefix,
            int order) {
        properties.add(namedProperty(prefix + "Size", order++, false, nonNegativeNumbers()));
        properties.add(namedProperty(prefix + "Fill", order++, false, zeroToOneDoubles()));
        properties.add(namedProperty(prefix + "Weight", order++, false,
                positiveFontAxisDoubles()));
        properties.add(namedProperty(prefix + "Grade", order++, false, gradeAxisDoubles()));
        properties.add(namedProperty(prefix + "OpticalSize", order++, false,
                positiveFontAxisDoubles()));
        properties.add(namedProperty(prefix + "Color", order++, false, colorOrTheme()));
        properties.add(namedProperty(prefix + "Opacity", order++, false, zeroToOneDoubles()));
        properties.add(namedProperty(prefix + "Shadows", order++, false, shadowValues()));
        properties.add(namedProperty(prefix + "ApplyTextScaling", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        return order;
    }

    private static int appendTextStyleProperties(
            List<PropertyDefinition> properties,
            String prefix,
            int order) {
        properties.add(namedProperty(prefix + "ThemeTextStyle", order++, false,
                textStyleTheme()));
        properties.add(namedProperty(prefix + "Inherit", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "Color", order++, false, colorOrTheme()));
        properties.add(namedProperty(prefix + "BackgroundColor", order++, false,
                colorOrTheme()));
        properties.add(namedProperty(prefix + "FontSize", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty(prefix + "FontWeight", order++, false,
                enumValues("FontWeight", "w100", "w200", "w300", "w400", "w500",
                        "w600", "w700", "w800", "w900")));
        properties.add(namedProperty(prefix + "FontStyle", order++, false,
                enumValues("FontStyle", "normal", "italic")));
        properties.add(namedProperty(prefix + "LetterSpacing", order++, false, finiteDoubles()));
        properties.add(namedProperty(prefix + "WordSpacing", order++, false, finiteDoubles()));
        properties.add(namedProperty(prefix + "TextBaseline", order++, false,
                enumValues("TextBaseline", "alphabetic", "ideographic")));
        properties.add(namedProperty(prefix + "Height", order++, false, finiteDoubles()));
        properties.add(namedProperty(prefix + "LeadingDistribution", order++, false,
                enumValues("TextLeadingDistribution", "proportional", "even")));
        properties.add(namedProperty(prefix + "LocaleLanguageCode", order++, false,
                localeLanguageCode()));
        properties.add(namedProperty(prefix + "LocaleScriptCode", order++, false,
                localeScriptCode()));
        properties.add(namedProperty(prefix + "LocaleCountryCode", order++, false,
                localeCountryCode()));
        properties.add(namedProperty(prefix + "DecorationUnderline", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "DecorationOverline", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "DecorationLineThrough", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "Foreground", order++, false, paintValues()));
        properties.add(namedProperty(prefix + "Background", order++, false, paintValues()));
        properties.add(namedProperty(prefix + "Shadows", order++, false, shadowValues()));
        properties.add(namedProperty(prefix + "FontFeatures", order++, false,
                any(PropertyValueKind.FONT_FEATURE_LIST)));
        properties.add(namedProperty(prefix + "FontVariations", order++, false,
                fontVariationValues()));
        properties.add(namedProperty(prefix + "DecorationColor", order++, false,
                colorOrTheme()));
        properties.add(namedProperty(prefix + "DecorationStyle", order++, false,
                enumValues("TextDecorationStyle", "solid", "double", "dotted", "dashed", "wavy")));
        properties.add(namedProperty(prefix + "DecorationThickness", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "DebugLabel", order++, false,
                any(PropertyValueKind.STRING)));
        properties.add(namedProperty(prefix + "FontFamily", order++, false,
                stringLength(1, 256)));
        properties.add(namedProperty(prefix + "FontFamilyFallback", order++, false,
                stringLength(0, 4096)));
        properties.add(namedProperty(prefix + "Package", order++, false,
                stringLength(1, 256)));
        properties.add(namedProperty(prefix + "Overflow", order++, false,
                enumValues("TextOverflow", "clip", "fade", "ellipsis", "visible")));
        return order;
    }

    private static WidgetDefinition column() {
        return flexWidget("flutter.widgets.Column", "Column", 10);
    }

    private static WidgetDefinition row() {
        return flexWidget("flutter.widgets.Row", "Row", 20);
    }

    private static WidgetDefinition wrap() {
        return widget(
                "flutter.widgets.Wrap",
                "Wrap",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 25, "Wrap"),
                List.of(
                        namedProperty("direction", 0, false,
                                enumValues("Axis", "horizontal", "vertical")),
                        namedProperty("alignment", 1, false,
                                enumValues("WrapAlignment", "start", "end", "center",
                                        "spaceBetween", "spaceAround", "spaceEvenly")),
                        namedProperty("spacing", 2, false, finiteDoubles()),
                        namedProperty("runAlignment", 3, false,
                                enumValues("WrapAlignment", "start", "end", "center",
                                        "spaceBetween", "spaceAround", "spaceEvenly")),
                        namedProperty("runSpacing", 4, false, finiteDoubles()),
                        namedProperty("crossAxisAlignment", 5, false,
                                enumValues("WrapCrossAlignment", "start", "end", "center")),
                        namedProperty("textDirection", 6, false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty("verticalDirection", 7, false,
                                enumValues("VerticalDirection", "up", "down")),
                        namedProperty("clipBehavior", 8, false,
                                enumValues("Clip", "none", "hardEdge", "antiAlias",
                                        "antiAliasWithSaveLayer"))),
                List.of(listSlot("children", 9, false, ANY_WIDGET)));
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

    private static WidgetDefinition aspectRatio() {
        return widget(
                "flutter.widgets.AspectRatio",
                "AspectRatio",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 60, "AspectRatio"),
                List.of(namedProperty(
                        "aspectRatio",
                        0,
                        true,
                        positiveDoubles(),
                        new PropertyValue.DoubleValue(BigDecimal.ONE))),
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition container() {
        List<String> reviewedColorThemeTokens = MaterialThemeTokenCatalog.colorRoles()
                .keySet().stream().sorted().toList();
        List<PropertyDefinition> properties = List.of(
                namedProperty("alignment", 0, false,
                        List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                namedProperty("padding", 1, false,
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                namedProperty("color", 2, false, colorOrTheme()),
                namedProperty("isAntiAlias", 3, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("decoration", 4, false,
                        List.of(new PropertyValueConstraint.BoxDecorationValues(
                                reviewedColorThemeTokens))),
                namedProperty("foregroundDecoration", 5, false,
                        List.of(new PropertyValueConstraint.BoxDecorationValues(
                                reviewedColorThemeTokens))),
                namedProperty("width", 6, false, nonNegativeNumbers()),
                namedProperty("height", 7, false, nonNegativeNumbers()),
                namedProperty("constraints", 8, false,
                        List.of(new PropertyValueConstraint.BoxConstraintsValues())),
                namedProperty("margin", 9, false,
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                namedProperty("transform", 10, false,
                        List.of(new PropertyValueConstraint.Matrix4Values())),
                namedProperty("transformAlignment", 11, false,
                        List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                namedProperty("clipBehavior", 13, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size()
                != ContainerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Container catalog/property schema count mismatch");
        }
        return widget(
                ContainerWidgetPropertySchema.CONTAINER_TYPE.value(),
                "Container",
                false,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 70, "Container"),
                properties,
                List.of(singleSlot("child", 12, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition opacity() {
        return widget(
                "flutter.widgets.Opacity",
                "Opacity",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 80, "Opacity"),
                List.of(
                        namedProperty(
                                "opacity",
                                0,
                                true,
                                zeroToOneDoubles(),
                                new PropertyValue.DoubleValue(BigDecimal.ONE)),
                        namedProperty(
                                "alwaysIncludeSemantics",
                                1,
                                false,
                                any(PropertyValueKind.BOOLEAN))),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition align() {
        return widget(
                "flutter.widgets.Align",
                "Align",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 90, "Align"),
                List.of(
                        namedProperty(
                                "alignment",
                                0,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "widthFactor",
                                1,
                                false,
                                nonNegativeNumbers()),
                        namedProperty(
                                "heightFactor",
                                2,
                                false,
                                nonNegativeNumbers())),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition fractionallySizedBox() {
        return widget(
                "flutter.widgets.FractionallySizedBox",
                "FractionallySizedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 100, "FractionallySizedBox"),
                List.of(
                        namedProperty(
                                "alignment",
                                0,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "widthFactor",
                                1,
                                false,
                                nonNegativeNumbers()),
                        namedProperty(
                                "heightFactor",
                                2,
                                false,
                                nonNegativeNumbers())),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition fittedBox() {
        return widget(
                "flutter.widgets.FittedBox",
                "FittedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 105, "FittedBox"),
                List.of(
                        namedProperty(
                                "fit",
                                0,
                                false,
                                enumValues(
                                        "BoxFit", "fill", "contain", "cover",
                                        "fitWidth", "fitHeight", "none", "scaleDown")),
                        namedProperty(
                                "alignment",
                                1,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "clipBehavior",
                                2,
                                false,
                                enumValues(
                                        "Clip", "none", "hardEdge", "antiAlias",
                                        "antiAliasWithSaveLayer"))),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition constrainedBox() {
        PropertyValue.BoxConstraintsValue initialConstraints =
                new PropertyValue.BoxConstraintsValue(
                        BigDecimal.ZERO,
                        Optional.empty(),
                        BigDecimal.ZERO,
                        Optional.empty());
        return widget(
                "flutter.widgets.ConstrainedBox",
                "ConstrainedBox",
                false,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 106, "ConstrainedBox"),
                List.of(namedProperty(
                        "constraints",
                        0,
                        true,
                        List.of(new PropertyValueConstraint.BoxConstraintsValues()),
                        initialConstraints)),
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition unconstrainedBox() {
        return widget(
                "flutter.widgets.UnconstrainedBox",
                "UnconstrainedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 107, "UnconstrainedBox"),
                List.of(
                        namedProperty(
                                "textDirection",
                                1,
                                false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty(
                                "alignment",
                                2,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "constrainedAxis",
                                3,
                                false,
                                enumValues("Axis", "horizontal", "vertical")),
                        namedProperty(
                                "clipBehavior",
                                4,
                                false,
                                enumValues(
                                        "Clip", "none", "hardEdge", "antiAlias",
                                        "antiAliasWithSaveLayer"))),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition limitedBox() {
        return widget(
                "flutter.widgets.LimitedBox",
                "LimitedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 108, "LimitedBox"),
                List.of(
                        namedProperty(
                                "maxWidth",
                                0,
                                false,
                                nonNegativeDoubles()),
                        namedProperty(
                                "maxHeight",
                                1,
                                false,
                                nonNegativeDoubles())),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition overflowBox() {
        return widget(
                "flutter.widgets.OverflowBox",
                "OverflowBox",
                true,
                WIDGETS_IMPORT,
                List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 109, "OverflowBox"),
                List.of(
                        namedProperty(
                                "alignment",
                                0,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "minWidth",
                                1,
                                false,
                                nonNegativeDoubles()),
                        namedProperty(
                                "maxWidth",
                                2,
                                false,
                                nonNegativeDoubles()),
                        namedProperty(
                                "minHeight",
                                3,
                                false,
                                nonNegativeDoubles()),
                        namedProperty(
                                "maxHeight",
                                4,
                                false,
                                nonNegativeDoubles()),
                        namedProperty(
                                "fit",
                                5,
                                false,
                                renderingEnumValues(
                                        "OverflowBoxFit", "max", "deferToChild"))),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition stack() {
        return widget(
                "flutter.widgets.Stack",
                "Stack",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 110, "Stack"),
                List.of(
                        namedProperty(
                                "alignment",
                                0,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "textDirection",
                                1,
                                false,
                                enumValues("TextDirection", "rtl", "ltr")),
                        namedProperty(
                                "fit",
                                2,
                                false,
                                enumValues("StackFit", "loose", "expand", "passthrough")),
                        namedProperty(
                                "clipBehavior",
                                3,
                                false,
                                enumValues("Clip", "none", "hardEdge", "antiAlias",
                                        "antiAliasWithSaveLayer"))),
                List.of(listSlot("children", 4, false, ANY_WIDGET)));
    }

    private static WidgetDefinition expanded() {
        return widget(
                WidgetPlacementRules.EXPANDED_TYPE,
                "Expanded",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 120, "Expanded"),
                List.of(namedProperty("flex", 0, false, nonNegativeIntegers())),
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition flexible() {
        return widget(
                WidgetPlacementRules.FLEXIBLE_TYPE,
                "Flexible",
                true,
                WIDGETS_IMPORT,
                List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 130, "Flexible"),
                List.of(
                        namedProperty("flex", 0, false, nonNegativeIntegers()),
                        namedProperty(
                                "fit",
                                1,
                                false,
                                renderingEnumValues("FlexFit", "loose", "tight"))),
                List.of(singleSlot("child", 2, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition spacer() {
        return widget(
                WidgetPlacementRules.SPACER_TYPE,
                "Spacer",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 140, "Spacer"),
                List.of(namedProperty("flex", 0, false, positiveIntegers())),
                List.of());
    }

    private static WidgetDefinition baseline() {
        return widget(
                "flutter.widgets.Baseline",
                "Baseline",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 150, "Baseline"),
                List.of(
                        namedProperty(
                                "baseline",
                                0,
                                true,
                                finiteDoubles(),
                                new PropertyValue.DoubleValue(BigDecimal.valueOf(24))),
                        namedProperty(
                                "baselineType",
                                1,
                                true,
                                enumValues("TextBaseline", "alphabetic", "ideographic"),
                                new PropertyValue.EnumValue("TextBaseline", "alphabetic"))),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition intrinsicHeight() {
        return widget(
                "flutter.widgets.IntrinsicHeight",
                "IntrinsicHeight",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 160, "IntrinsicHeight"),
                List.of(),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition listView() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("scrollDirection", 0, false,
                        enumValues("Axis", "horizontal", "vertical")),
                namedProperty("reverse", 1, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("primary", 2, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("physics", 3, false, stringPattern(
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)",
                        "reviewed ScrollPhysics preset")),
                namedProperty("shrinkWrap", 4, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("padding", 5, false,
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                namedProperty("itemExtent", 6, false, nonNegativeNumbers()),
                namedProperty("addAutomaticKeepAlives", 7, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("addRepaintBoundaries", 8, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("addSemanticIndexes", 9, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("scrollCacheExtent", 10, false,
                        nonNegativeNumbers()),
                namedProperty("semanticChildCount", 12, false,
                        nonNegativeIntegers()),
                namedProperty("dragStartBehavior", 13, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("keyboardDismissBehavior", 14, false,
                        enumValues("ScrollViewKeyboardDismissBehavior", "manual", "onDrag")),
                namedProperty("restorationId", 15, false,
                        stringLength(1, 256)),
                namedProperty("clipBehavior", 16, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 17, false,
                        renderingEnumValues(
                                "HitTestBehavior", "deferToChild", "opaque", "translucent")));
        if (properties.size() != ListViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ListView catalog/property schema count mismatch");
        }
        return widget(
                ListViewWidgetPropertySchema.LIST_VIEW_TYPE.value(),
                "ListView",
                false,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT),
                Set.of(),
                palette("flutter.scrolling", 250, 10, "ListView"),
                properties,
                List.of(listSlot("children", 11, false, ANY_WIDGET)));
    }

    private static WidgetDefinition image() {
        return widget(
                "flutter.widgets.Image",
                "Image",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 30, "Image"),
                List.of(
                        namedProperty("image", 0, true,
                                List.of(new PropertyValueConstraint.ImageProviderValues())),
                        namedProperty("frameBuilder", 1, false,
                                List.of(new PropertyValueConstraint.CallbackReference())),
                        namedProperty("loadingBuilder", 2, false,
                                List.of(new PropertyValueConstraint.CallbackReference())),
                        namedProperty("errorBuilder", 3, false,
                                List.of(new PropertyValueConstraint.CallbackReference())),
                        namedProperty("semanticLabel", 4, false,
                                any(PropertyValueKind.STRING)),
                        namedProperty("excludeFromSemantics", 5, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("width", 6, false, nonNegativeNumbers()),
                        namedProperty("height", 7, false, nonNegativeNumbers()),
                        namedProperty("color", 8, false, colorOrTheme()),
                        namedProperty("opacity", 9, false, zeroToOneDoubles()),
                        namedProperty("colorBlendMode", 10, false, enumValues(
                                "BlendMode", "clear", "src", "dst", "srcOver", "dstOver",
                                "srcIn", "dstIn", "srcOut", "dstOut", "srcATop", "dstATop",
                                "xor", "plus", "modulate", "screen", "overlay", "darken",
                                "lighten", "colorDodge", "colorBurn", "hardLight", "softLight",
                                "difference", "exclusion", "multiply", "hue", "saturation",
                                "color", "luminosity")),
                        namedProperty("fit", 11, false, enumValues(
                                "BoxFit", "fill", "contain", "cover", "fitWidth",
                                "fitHeight", "none", "scaleDown")),
                        namedProperty("alignment", 12, false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty("repeat", 13, false, enumValues(
                                "ImageRepeat", "repeat", "repeatX", "repeatY", "noRepeat")),
                        namedProperty("centerSliceLeft", 14, false, nonNegativeDoubles()),
                        namedProperty("centerSliceTop", 15, false, nonNegativeDoubles()),
                        namedProperty("centerSliceRight", 16, false, nonNegativeDoubles()),
                        namedProperty("centerSliceBottom", 17, false, nonNegativeDoubles()),
                        namedProperty("matchTextDirection", 18, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("gaplessPlayback", 19, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("isAntiAlias", 20, false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty("filterQuality", 21, false, enumValues(
                                "FilterQuality", "none", "low", "medium", "high"))),
                List.of());
    }

    private static WidgetDefinition textField() {
        List<PropertyDefinition> properties = new ArrayList<>();
        int order = 0;
        properties.add(namedProperty("keyboardType", order++, false, stringPattern(
                "(?:text|multiline|number|numberSigned|numberDecimal|numberSignedDecimal|phone|datetime|emailAddress|url|visiblePassword|name|streetAddress|none|webSearch|twitter)",
                "reviewed TextInputType preset")));
        properties.add(namedProperty("textInputAction", order++, false,
                servicesEnumValues("TextInputAction",
                        "none", "unspecified", "done", "go", "search", "send",
                        "next", "previous", "continueAction", "join", "route",
                        "emergencyCall", "newline")));
        properties.add(namedProperty("textCapitalization", order++, false,
                servicesEnumValues("TextCapitalization",
                        "words", "sentences", "characters", "none")));
        properties.add(namedProperty("textAlign", order++, false,
                enumValues("TextAlign", "left", "right", "center", "justify",
                        "start", "end")));
        properties.add(namedProperty("textAlignVertical", order++, false, stringPattern(
                "(?:top|center|bottom)", "reviewed TextAlignVertical static preset")));
        properties.add(namedProperty("textDirection", order++, false,
                enumValues("TextDirection", "rtl", "ltr")));
        properties.add(namedProperty("readOnly", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("showCursor", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("autofocus", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("obscuringCharacter", order++, false, stringPattern(
                "[\\u0000-\\uD7FF\\uE000-\\uFFFF]",
                "one BMP Unicode scalar (one UTF-16 code unit)")));
        properties.add(namedProperty("obscureText", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("autocorrect", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("smartDashesType", order++, false,
                servicesEnumValues("SmartDashesType", "disabled", "enabled")));
        properties.add(namedProperty("smartQuotesType", order++, false,
                servicesEnumValues("SmartQuotesType", "disabled", "enabled")));
        properties.add(namedProperty("enableSuggestions", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("maxLines", order++, false, positiveIntegers()));
        properties.add(namedProperty("minLines", order++, false, positiveIntegers()));
        properties.add(namedProperty("expands", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("maxLength", order++, false, maxLengthIntegers()));
        properties.add(namedProperty("maxLengthEnforcement", order++, false,
                servicesEnumValues("MaxLengthEnforcement",
                        "none", "enforced", "truncateAfterCompositionEnds")));
        for (String callback : List.of(
                "onChanged", "onEditingComplete", "onSubmitted",
                "onAppPrivateCommand")) {
            properties.add(namedProperty(callback, order++, false,
                    List.of(new PropertyValueConstraint.CallbackReference())));
        }
        properties.add(namedProperty("enabled", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("ignorePointers", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("cursorWidth", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty("cursorHeight", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty("cursorRadiusX", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("cursorRadiusY", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty("cursorOpacityAnimates", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("cursorColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("cursorErrorColor", order++, false, colorOrTheme()));
        properties.add(namedProperty("selectionHeightStyle", order++, false,
                dartUiEnumValues("BoxHeightStyle",
                        "tight", "max", "includeLineSpacingMiddle",
                        "includeLineSpacingTop", "includeLineSpacingBottom", "strut")));
        properties.add(namedProperty("selectionWidthStyle", order++, false,
                dartUiEnumValues("BoxWidthStyle", "tight", "max")));
        properties.add(namedProperty("keyboardAppearance", order++, false,
                enumValues("Brightness", "dark", "light")));
        for (String edge : List.of(
                "scrollPaddingLeft", "scrollPaddingTop",
                "scrollPaddingRight", "scrollPaddingBottom")) {
            properties.add(namedProperty(edge, order++, false, nonNegativeDoubles()));
        }
        properties.add(namedProperty("dragStartBehavior", order++, false,
                gesturesEnumValues("DragStartBehavior", "down", "start")));
        properties.add(namedProperty("enableInteractiveSelection", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("selectAllOnFocus", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("onTap", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("onTapAlwaysCalled", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        for (String callback : List.of("onTapOutside", "onTapUpOutside")) {
            properties.add(namedProperty(callback, order++, false,
                    List.of(new PropertyValueConstraint.CallbackReference())));
        }
        properties.add(namedProperty("mouseCursor", order++, false, stringPattern(
                "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)",
                "reviewed SystemMouseCursors preset")));
        properties.add(namedProperty("clipBehavior", order++, false,
                enumValues("Clip", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer")));
        properties.add(namedProperty("restorationId", order++, false,
                stringLength(1, 256)));
        properties.add(namedProperty("stylusHandwritingEnabled", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("enableIMEPersonalizedLearning", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("enableInlinePrediction", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("canRequestFocus", order++, false,
                any(PropertyValueKind.BOOLEAN)));

        List<String> names = properties.stream()
                .map(property -> property.name().value())
                .toList();
        if (properties.size() != TextFieldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT
                || !names.equals(List.copyOf(
                        TextFieldWidgetPropertySchema.definitions().keySet()))) {
            throw new ExceptionInInitializerError(
                    "TextField catalog/property schema mismatch; actual=" + names);
        }
        return widget(
                TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE.value(),
                "TextField",
                true,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, SERVICES_IMPORT,
                        GESTURES_IMPORT, DART_UI_IMPORT),
                Set.of(),
                palette("flutter.material", 100, 40, "Text Field"),
                List.copyOf(properties),
                List.of());
    }

    private static WidgetDefinition elevatedButton() {
        List<PropertyDefinition> properties = new ArrayList<>();
        int order = 0;
        properties.add(namedProperty(
                "enabled", order++, false,
                any(PropertyValueKind.BOOLEAN),
                new PropertyValue.BooleanValue(true)));
        properties.add(namedProperty("onPressed", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("onLongPress", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("onHover", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("onFocusChange", order++, false,
                List.of(new PropertyValueConstraint.CallbackReference())));
        properties.add(namedProperty("autofocus", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("clipBehavior", order++, false,
                enumValues("Clip", "none", "hardEdge", "antiAlias",
                        "antiAliasWithSaveLayer")));

        for (String prefix : List.of(
                "style", "styleDisabled", "stylePressed",
                "styleHovered", "styleFocused")) {
            order = appendElevatedButtonStateProperties(properties, prefix, order);
            order = appendElevatedButtonTextStyleProperties(properties, prefix, order);
        }

        properties.add(namedProperty("styleVisualDensityHorizontal", order++, false,
                minusFourToFourDoubles()));
        properties.add(namedProperty("styleVisualDensityVertical", order++, false,
                minusFourToFourDoubles()));
        properties.add(namedProperty("styleTapTargetSize", order++, false,
                materialEnumValues("MaterialTapTargetSize", "padded", "shrinkWrap")));
        properties.add(namedProperty("styleAnimationDurationMs", order++, false,
                nonNegativeIntegers()));
        properties.add(namedProperty("styleEnableFeedback", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("styleAlignmentKind", order++, false,
                stringPattern("(?:physical|directional)",
                        "ButtonStyle alignment kind: physical or directional")));
        properties.add(namedProperty("styleAlignmentX", order++, false, finiteDoubles()));
        properties.add(namedProperty("styleAlignmentY", order++, false, finiteDoubles()));
        properties.add(namedProperty("styleSplashFactory", order++, false,
                stringPattern("(?:inkRipple|inkSplash|inkSparkle|noSplash)",
                        "ButtonStyle splash factory preset")));

        if (properties.size() != 286) {
            throw new ExceptionInInitializerError(
                    "ElevatedButton schema must expose exactly 286 properties; actual="
                    + properties.size());
        }
        return widget(
                "flutter.material.ElevatedButton",
                "ElevatedButton",
                false,
                MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.material", 100, 30, "Elevated Button"),
                List.copyOf(properties),
                // The reviewed Designer projection deliberately permits an
                // empty slot and emits the required named argument as null.
                // This keeps a freshly created Palette prototype structurally
                // valid while preserving the exact one-child capacity.
                List.of(singleSlot("child", order, true, 0, ANY_WIDGET)));
    }

    private static int appendElevatedButtonStateProperties(
            List<PropertyDefinition> properties,
            String prefix,
            int order) {
        for (String suffix : List.of(
                "BackgroundColor", "ForegroundColor", "OverlayColor",
                "ShadowColor", "SurfaceTintColor")) {
            properties.add(namedProperty(prefix + suffix, order++, false, colorOrTheme()));
        }
        properties.add(namedProperty(prefix + "Elevation", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty(prefix + "Padding", order++, false,
                List.of(new PropertyValueConstraint.EdgeInsetsValues(true))));
        for (String suffix : List.of(
                "MinimumWidth", "MinimumHeight", "FixedWidth", "FixedHeight",
                "MaximumWidth", "MaximumHeight")) {
            properties.add(namedProperty(prefix + suffix, order++, false,
                    nonNegativeNumbers()));
        }
        properties.add(namedProperty(prefix + "IconColor", order++, false, colorOrTheme()));
        properties.add(namedProperty(prefix + "IconSize", order++, false,
                nonNegativeNumbers()));
        properties.add(namedProperty(prefix + "SideColor", order++, false, colorOrTheme()));
        properties.add(namedProperty(prefix + "SideWidth", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty(prefix + "SideStyle", order++, false,
                enumValues("BorderStyle", "none", "solid")));
        properties.add(namedProperty(prefix + "SideStrokeAlign", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "ShapeKind", order++, false,
                stringPattern(
                        "(?:roundedRectangle|roundedSuperellipse|stadium|circle|beveledRectangle|continuousRectangle)",
                        "closed ButtonStyle OutlinedBorder kind")));
        for (String suffix : List.of(
                "ShapeRadiusTopLeft", "ShapeRadiusTopRight",
                "ShapeRadiusBottomRight", "ShapeRadiusBottomLeft")) {
            properties.add(namedProperty(prefix + suffix, order++, false,
                    nonNegativeDoubles()));
        }
        properties.add(namedProperty(prefix + "ShapeCircleEccentricity", order++, false,
                zeroToOneDoubles()));
        properties.add(namedProperty(prefix + "MouseCursor", order++, false,
                stringPattern(
                        "(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)",
                        "reviewed SystemMouseCursors preset")));
        return order;
    }

    private static int appendElevatedButtonTextStyleProperties(
            List<PropertyDefinition> properties,
            String prefix,
            int order) {
        properties.add(namedProperty(prefix + "TextTheme", order++, false,
                textStyleTheme()));
        properties.add(namedProperty(prefix + "TextInherit", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "TextBackgroundColor", order++, false,
                colorOrTheme()));
        properties.add(namedProperty(prefix + "TextFontSize", order++, false,
                nonNegativeDoubles()));
        properties.add(namedProperty(prefix + "TextFontWeight", order++, false,
                enumValues("FontWeight", "w100", "w200", "w300", "w400",
                        "w500", "w600", "w700", "w800", "w900")));
        properties.add(namedProperty(prefix + "TextFontStyle", order++, false,
                enumValues("FontStyle", "normal", "italic")));
        properties.add(namedProperty(prefix + "TextLetterSpacing", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "TextWordSpacing", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "TextTextBaseline", order++, false,
                enumValues("TextBaseline", "alphabetic", "ideographic")));
        properties.add(namedProperty(prefix + "TextHeight", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "TextLeadingDistribution", order++, false,
                enumValues("TextLeadingDistribution", "proportional", "even")));
        properties.add(namedProperty(prefix + "TextLocaleLanguageCode", order++, false,
                localeLanguageCode()));
        properties.add(namedProperty(prefix + "TextLocaleScriptCode", order++, false,
                localeScriptCode()));
        properties.add(namedProperty(prefix + "TextLocaleCountryCode", order++, false,
                localeCountryCode()));
        properties.add(namedProperty(prefix + "TextBackground", order++, false,
                paintValues()));
        properties.add(namedProperty(prefix + "TextShadows", order++, false,
                shadowValues()));
        properties.add(namedProperty(prefix + "TextFontFeatures", order++, false,
                any(PropertyValueKind.FONT_FEATURE_LIST)));
        properties.add(namedProperty(prefix + "TextFontVariations", order++, false,
                fontVariationValues()));
        properties.add(namedProperty(prefix + "TextDecorationUnderline", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "TextDecorationOverline", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "TextDecorationLineThrough", order++, false,
                any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty(prefix + "TextDecorationColor", order++, false,
                colorOrTheme()));
        properties.add(namedProperty(prefix + "TextDecorationStyle", order++, false,
                enumValues("TextDecorationStyle", "solid", "double", "dotted",
                        "dashed", "wavy")));
        properties.add(namedProperty(prefix + "TextDecorationThickness", order++, false,
                finiteDoubles()));
        properties.add(namedProperty(prefix + "TextFontFamily", order++, false,
                stringLength(1, 256)));
        properties.add(namedProperty(prefix + "TextFontFamilyFallback", order++, false,
                stringLength(0, 4096)));
        properties.add(namedProperty(prefix + "TextPackage", order++, false,
                stringLength(1, 256)));
        properties.add(namedProperty(prefix + "TextOverflow", order++, false,
                enumValues("TextOverflow", "clip", "fade", "ellipsis", "visible")));
        return order;
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

    private static PropertyDefinition namedProperty(
            String name,
            int order,
            boolean required,
            List<PropertyValueConstraint> constraints,
            PropertyValue creationDefault) {
        return new PropertyDefinition(
                new PropertyName(name),
                DartParameter.named(order, required),
                constraints,
                Optional.of(creationDefault));
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

    private static List<PropertyValueConstraint> nonNegativeIntegers() {
        return List.of(new PropertyValueConstraint.IntegerRange(
                BigInteger.ZERO, DartNumericLiterals.MAX_PORTABLE_INTEGER));
    }

    private static List<PropertyValueConstraint> positiveIntegers() {
        return List.of(new PropertyValueConstraint.IntegerRange(
                BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER));
    }

    private static List<PropertyValueConstraint> maxLengthIntegers() {
        return List.of(new PropertyValueConstraint.IntegerRange(
                BigInteger.valueOf(-1), DartNumericLiterals.MAX_PORTABLE_INTEGER));
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

    private static List<PropertyValueConstraint> minusFourToFourDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(
                BigDecimal.valueOf(-4), true, BigDecimal.valueOf(4), true));
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

    private static List<PropertyValueConstraint> materialEnumValues(
            String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(MATERIAL_IMPORT, type), List.of(values)));
    }

    private static List<PropertyValueConstraint> gesturesEnumValues(
            String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(GESTURES_IMPORT, type), List.of(values)));
    }

    private static List<PropertyValueConstraint> renderingEnumValues(
            String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(RENDERING_IMPORT, type), List.of(values)));
    }

    private static List<PropertyValueConstraint> servicesEnumValues(
            String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(SERVICES_IMPORT, type), List.of(values)));
    }

    private static List<PropertyValueConstraint> dartUiEnumValues(
            String type, String... values) {
        return List.of(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference(DART_UI_IMPORT, type), List.of(values)));
    }
}

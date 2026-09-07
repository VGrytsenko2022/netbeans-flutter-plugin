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
            indexedStack(),
            expanded(),
            flexible(),
            spacer(),
            baseline(),
            intrinsicHeight(),
            intrinsicWidth(),
            offstage(),
            sizedOverflowBox(),
            transform(),
            rotatedBox(),
            listBody(),
            overflowBar(),
            safeArea(),
            listView(),
            gridViewCount(),
            singleChildScrollView(),
            image(),
            coloredBox(),
            placeholder(),
            directionality(),
            decoratedBox(),
            clipRect(),
            clipOval(),
            clipRRect(),
            clipPath(),
            clipRSuperellipse(),
            physicalModel(),
            physicalShape(),
            repaintBoundary(),
            ignorePointer(),
            absorbPointer(),
            visibility(),
            tickerMode(),
            defaultTextHeightBehavior(),
            defaultSelectionStyle(),
            iconTheme(),
            imageIcon(),
            excludeSemantics(),
            blockSemantics(),
            mergeSemantics(),
            indexedSemantics(),
            excludeFocus(),
            excludeFocusTraversal(),
            elevatedButton(),
            divider(),
            verticalDivider(),
            card(),
            badge(),
            circleAvatar(),
            linearProgressIndicator(),
            circularProgressIndicator(),
            refreshProgressIndicator(),
            refreshIndicator(),
            textButton(),
            fullStyleButton("OutlinedButton"),
            fullStyleButton("FilledButton"),
            floatingActionButton(),
            iconButton(),
            checkbox(),
            radio(),
            radioGroup(),
            listTile(),
            switchWidget(),
            slider(),
            rangeSlider(),
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

    private static WidgetDefinition refreshIndicator() {
        List<PropertyValueConstraint> predicate = new ArrayList<>(
                stringPattern("(?:default|depthZero|all)", "RefreshIndicator notification predicate preset"));
        predicate.add(new PropertyValueConstraint.DartObjectReferenceValues("ScrollNotificationPredicate"));
        return widget(RefreshIndicatorWidgetPropertySchema.REFRESH_INDICATOR_TYPE.value(),
                "RefreshIndicator", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 130, "RefreshIndicator"),
                List.of(namedProperty("displacement", 0, false, nonNegativeNumbers()),
                        namedProperty("edgeOffset", 1, false, cardNumbers(null, null)),
                        namedProperty("onRefresh", 2, false,
                                List.of(new PropertyValueConstraint.DartObjectReferenceValues("RefreshCallback"))),
                        namedProperty("color", 3, false, colorOrTheme()),
                        namedProperty("backgroundColor", 4, false, colorOrTheme()),
                        namedProperty("notificationPredicate", 5, false, predicate),
                        namedProperty("semanticsLabel", 6, false, any(PropertyValueKind.STRING)),
                        namedProperty("semanticsValue", 7, false, any(PropertyValueKind.STRING)),
                        namedProperty("strokeWidth", 8, false, cardNumbers(null, null)),
                        namedProperty("triggerMode", 9, false, List.of(new PropertyValueConstraint.EnumValues(
                                new DartSymbolReference(MATERIAL_IMPORT, "RefreshIndicatorTriggerMode"),
                                List.of("onEdge", "anywhere")))),
                        namedProperty("elevation", 10, false, nonNegativeNumbers()),
                        namedProperty("onStatusChange", 12, false,
                                List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<RefreshIndicatorStatus?>"))),
                        namedProperty("variant", 13, true,
                                stringPattern("(?:material|adaptive|noSpinner)", "RefreshIndicator constructor variant"),
                                new PropertyValue.StringValue("material"))),
                List.of(singleSlot("child", 11, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition refreshProgressIndicator() {
        List<PropertyValueConstraint> width = new ArrayList<>(cardNumbers(null, null));
        width.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        List<PropertyValueConstraint> valueColor = new ArrayList<>(colorOrTheme());
        valueColor.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        valueColor.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<Color?>"));
        return widget(RefreshProgressIndicatorWidgetPropertySchema.REFRESH_PROGRESS_INDICATOR_TYPE.value(),
                "RefreshProgressIndicator", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 120, "RefreshProgressIndicator"),
                List.of(namedProperty("value", 0, false, cardNumbers(null, null)),
                        namedProperty("backgroundColor", 1, false, colorOrTheme()),
                        namedProperty("color", 2, false, colorOrTheme()),
                        namedProperty("valueColor", 3, false, valueColor),
                        namedProperty("strokeWidth", 4, false, width),
                        namedProperty("strokeAlign", 5, false, cardNumbers(null, null)),
                        namedProperty("semanticsLabel", 6, false, any(PropertyValueKind.STRING)),
                        namedProperty("semanticsValue", 7, false, any(PropertyValueKind.STRING)),
                        namedProperty("strokeCap", 8, false, enumValues("StrokeCap", "butt", "round", "square")),
                        namedProperty("elevation", 9, false, nonNegativeNumbers()),
                        namedProperty("indicatorMargin", 10, false, List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                        namedProperty("indicatorPadding", 11, false, List.of(new PropertyValueConstraint.EdgeInsetsValues(true)))), List.of());
    }

    private static WidgetDefinition circularProgressIndicator() {
        List<PropertyValueConstraint> gap = new ArrayList<>(cardNumbers(null, null));
        gap.add(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference("dart:core", "double"), List.of("infinity")));
        List<PropertyValueConstraint> valueColor = new ArrayList<>(colorOrTheme());
        valueColor.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        valueColor.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<Color?>"));
        return widget(CircularProgressIndicatorWidgetPropertySchema.CIRCULAR_PROGRESS_INDICATOR_TYPE.value(),
                "CircularProgressIndicator", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 110, "CircularProgressIndicator"),
                List.of(namedProperty("value", 0, false, cardNumbers(null, null)),
                        namedProperty("backgroundColor", 1, false, colorOrTheme()),
                        namedProperty("color", 2, false, colorOrTheme()),
                        namedProperty("valueColor", 3, false, valueColor),
                        namedProperty("strokeWidth", 4, false, cardNumbers(null, null)),
                        namedProperty("strokeAlign", 5, false, cardNumbers(null, null)),
                        namedProperty("semanticsLabel", 6, false, any(PropertyValueKind.STRING)),
                        namedProperty("semanticsValue", 7, false, any(PropertyValueKind.STRING)),
                        namedProperty("strokeCap", 8, false, enumValues("StrokeCap", "butt", "round", "square")),
                        namedProperty("constraints", 9, false, List.of(new PropertyValueConstraint.BoxConstraintsValues())),
                        namedProperty("trackGap", 10, false, gap),
                        namedProperty("year2023", 11, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("padding", 12, false, List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                        namedProperty("controller", 13, false,
                                List.of(new PropertyValueConstraint.DartObjectReferenceValues("AnimationController"))),
                        namedProperty("variant", 14, true,
                                stringPattern("(?:material|adaptive)", "CircularProgressIndicator constructor variant"),
                                new PropertyValue.StringValue("material"))), List.of());
    }

    private static WidgetDefinition linearProgressIndicator() {
        var infinity = new PropertyValueConstraint.EnumValues(
                new DartSymbolReference("dart:core", "double"), List.of("infinity"));
        List<PropertyValueConstraint> signedInfinite = new ArrayList<>(cardNumbers(null, null));
        signedInfinite.add(infinity);
        List<PropertyValueConstraint> positiveInfinite = List.of(
                new PropertyValueConstraint.IntegerRange(BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, false, null, true), infinity);
        List<PropertyValueConstraint> valueColor = new ArrayList<>(colorOrTheme());
        valueColor.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        valueColor.add(new PropertyValueConstraint.DartObjectReferenceValues("Animation<Color?>"));
        return widget(LinearProgressIndicatorWidgetPropertySchema.LINEAR_PROGRESS_INDICATOR_TYPE.value(),
                "LinearProgressIndicator", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 100, "LinearProgressIndicator"),
                List.of(namedProperty("value", 0, false, cardNumbers(null, null)),
                        namedProperty("backgroundColor", 1, false, colorOrTheme()),
                        namedProperty("color", 2, false, colorOrTheme()),
                        namedProperty("valueColor", 3, false, valueColor),
                        namedProperty("minHeight", 4, false, positiveInfinite),
                        namedProperty("semanticsLabel", 5, false, any(PropertyValueKind.STRING)),
                        namedProperty("semanticsValue", 6, false, any(PropertyValueKind.STRING)),
                        namedProperty("borderRadius", 7, false, List.of(new PropertyValueConstraint.BorderRadiusValues())),
                        namedProperty("stopIndicatorColor", 8, false, colorOrTheme()),
                        namedProperty("stopIndicatorRadius", 9, false, signedInfinite),
                        namedProperty("trackGap", 10, false, signedInfinite),
                        namedProperty("year2023", 11, false, any(PropertyValueKind.BOOLEAN)),
                        namedProperty("controller", 12, false,
                                List.of(new PropertyValueConstraint.DartObjectReferenceValues("AnimationController")))), List.of());
    }

    private static WidgetDefinition circleAvatar() {
        List<PropertyValueConstraint> radiusValues = new ArrayList<>(nonNegativeNumbers());
        radiusValues.add(new PropertyValueConstraint.EnumValues(
                new DartSymbolReference("dart:core", "double"), List.of("infinity")));
        return widget(CircleAvatarWidgetPropertySchema.CIRCLE_AVATAR_TYPE.value(),
                "CircleAvatar", true, MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 90, "CircleAvatar"),
                List.of(
                        namedProperty("backgroundColor", 1, false, colorOrTheme()),
                        namedProperty("backgroundImage", 2, false,
                                List.of(new PropertyValueConstraint.ImageProviderValues())),
                        namedProperty("foregroundImage", 3, false,
                                List.of(new PropertyValueConstraint.ImageProviderValues())),
                        namedProperty("onBackgroundImageError", 4, false,
                                List.of(new PropertyValueConstraint.CallbackReference())),
                        namedProperty("onForegroundImageError", 5, false,
                                List.of(new PropertyValueConstraint.CallbackReference())),
                        namedProperty("foregroundColor", 6, false, colorOrTheme()),
                        namedProperty("radius", 7, false, radiusValues),
                        namedProperty("minRadius", 8, false, radiusValues),
                        namedProperty("maxRadius", 9, false, radiusValues)),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition badge() {
        List<PropertyDefinition> properties = new ArrayList<>();
        properties.add(namedProperty("backgroundColor", 0, false, colorOrTheme()));
        properties.add(namedProperty("textColor", 1, false, colorOrTheme()));
        properties.add(namedProperty("smallSize", 2, false, nonNegativeNumbers()));
        properties.add(namedProperty("largeSize", 3, false, cardNumbers(null, null)));
        properties.add(namedProperty("padding", 4, false, List.of(new PropertyValueConstraint.EdgeInsetsValues(true))));
        properties.add(namedProperty("alignment", 5, false, List.of(new PropertyValueConstraint.AlignmentGeometryValues())));
        properties.add(namedProperty("offset", 6, false, List.of(new PropertyValueConstraint.OffsetValues())));
        properties.add(namedProperty("isLabelVisible", 7, false, any(PropertyValueKind.BOOLEAN)));
        properties.add(namedProperty("count", 10, false, nonNegativeIntegers()));
        properties.add(namedProperty("maxCount", 11, false, positiveIntegers()));
        appendTextStyleProperties(properties, "textStyle", 12);
        if (properties.size() != BadgeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) throw new ExceptionInInitializerError("Badge property projection mismatch");
        return widget(BadgeWidgetPropertySchema.BADGE_TYPE.value(), "Badge", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 80, "Badge"), properties,
                List.of(singleSlot("label", 8, false, 0, ANY_WIDGET), singleSlot("child", 9, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition listTile() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition text = badge();
        WidgetDefinition shape = card();
        List<PropertyValueConstraint> cursor = checkbox().property(new PropertyName("mouseCursor")).orElseThrow().constraints();
        for (var entry : ListTileWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            var family = ListTileWidgetPropertySchema.textStyleFamily(new PropertyName(name));
            if (family.isPresent()) {
                constraints = text.property(new PropertyName("textStyle" + name.substring(family.orElseThrow().length()))).orElseThrow().constraints();
            } else if (ListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                constraints = shape.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (ListTileWidgetPropertySchema.stateColorFamilies().stream().anyMatch(value -> ListTileWidgetPropertySchema.colorStateProperties(value).contains(name))) {
                constraints = colorOrTheme();
            } else if (ListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name) || name.equals("mouseCursor")) {
                constraints = cursor;
            } else if (ListTileWidgetPropertySchema.colorProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
            } else if (ListTileWidgetPropertySchema.geometryProperties().contains(name)) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity", "negativeInfinity", "nan")));
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (name) {
                    case "visualDensity" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VisualDensity"));
                    case "visualDensityHorizontal", "visualDensityVertical" -> minusFourToFourDoubles();
                    case "shape" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"));
                    case "titleTextStyle", "subtitleTextStyle", "leadingAndTrailingTextStyle" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("TextStyle"));
                    case "contentPadding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(false), new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"));
                    case "style" -> materialEnumValues("ListTileStyle", "list", "drawer");
                    case "titleAlignment" -> materialEnumValues("ListTileTitleAlignment", "threeLine", "titleHeight", "top", "center", "bottom");
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "statesController" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStatesController"));
                    case "isThreeLine", "dense", "enableFeedback" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "onTap", "onLongPress", "onFocusChange" -> List.of(
                            new PropertyValueConstraint.StringPattern("noop", "Explicit controlled no-op callback"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                            new PropertyValueConstraint.DartObjectReferenceValues(name.equals("onFocusChange") ? "ValueChanged<bool>" : "VoidCallback"));
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            properties.add(namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        return widget(ListTileWidgetPropertySchema.LIST_TILE_TYPE.value(), "ListTile", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 250, "ListTile"), properties,
                List.of(singleSlot("leading", 0, false, 0, ANY_WIDGET), singleSlot("title", 1, false, 0, ANY_WIDGET),
                        singleSlot("subtitle", 2, false, 0, ANY_WIDGET), singleSlot("trailing", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition rangeSlider() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var entry : RangeSliderWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (RangeSliderWidgetPropertySchema.overlayColorStateProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (RangeSliderWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                constraints = new ArrayList<>(checkbox().property(new PropertyName("mouseCursor")).orElseThrow().constraints());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (RangeSliderWidgetPropertySchema.rangeProperties().contains(name)) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"),
                        List.of("infinity", "negativeInfinity")));
            } else {
                constraints = switch (name) {
                    case "onChanged", "onChangeStart", "onChangeEnd" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<RangeValues>"));
                    case "semanticFormatterCallback" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("SemanticFormatterCallback"));
                    case "overlayColor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"));
                    case "mouseCursor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<MouseCursor?>"));
                    case "labels" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("RangeLabels"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "activeColor", "inactiveColor" -> colorOrTheme();
                    case "divisions" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE,
                            BigInteger.valueOf(9_007_199_254_740_991L)), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "labelsStart", "labelsEnd" -> any(PropertyValueKind.STRING);
                    case "padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                    case "year2023" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            PropertyValue creation = switch (name) {
                case "valuesStart" -> new PropertyValue.IntegerValue(BigInteger.ZERO);
                case "valuesEnd" -> new PropertyValue.IntegerValue(BigInteger.ONE);
                case "enabled" -> new PropertyValue.BooleanValue(true);
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(RangeSliderWidgetPropertySchema.RANGE_SLIDER_TYPE.value(), "RangeSlider", false,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 220, "RangeSlider"), properties, List.of());
    }

    private static WidgetDefinition slider() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var entry : SliderWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (SliderWidgetPropertySchema.overlayColorStateProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (SliderWidgetPropertySchema.rangeProperties().contains(name)) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"),
                        List.of("infinity", "negativeInfinity")));
                if (name.equals("secondaryTrackValue")) {
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
            } else {
                constraints = switch (name) {
                    case "onChanged", "onChangeStart", "onChangeEnd" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<double>"));
                    case "semanticFormatterCallback" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("SemanticFormatterCallback"));
                    case "overlayColor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"));
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "activeColor", "inactiveColor", "secondaryActiveColor", "thumbColor" -> colorOrTheme();
                    case "divisions" -> List.of(new PropertyValueConstraint.IntegerRange(BigInteger.ONE,
                            BigInteger.valueOf(9_007_199_254_740_991L)), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "label" -> any(PropertyValueKind.STRING);
                    case "mouseCursor" -> checkbox().property(new PropertyName("mouseCursor")).orElseThrow().constraints();
                    case "allowedInteraction" -> List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference(MATERIAL_IMPORT, "SliderInteraction"),
                            List.of("tapAndSlide", "tapOnly", "slideOnly", "slideThumb")));
                    case "padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                    case "showValueIndicator" -> List.of(new PropertyValueConstraint.EnumValues(new DartSymbolReference(MATERIAL_IMPORT, "ShowValueIndicator"),
                            List.of("onlyForDiscrete", "onlyForContinuous", "always", "onDrag", "alwaysVisible", "never")));
                    case "year2023" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "variant" -> stringPattern("(?:standard|adaptive)", "Slider constructor");
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.IntegerValue(BigInteger.ZERO);
                case "enabled" -> new PropertyValue.BooleanValue(true);
                case "variant" -> new PropertyValue.StringValue("standard");
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(SliderWidgetPropertySchema.SLIDER_TYPE.value(), "Slider", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 210, "Slider"), properties, List.of());
    }

    private static WidgetDefinition switchWidget() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition iconDefinition = icon();
        WidgetDefinition checkboxDefinition = checkbox();
        for (var entry : SwitchWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            Optional<String> iconName = SwitchWidgetPropertySchema.iconSourceName(name);
            if (iconName.isPresent()) {
                constraints = iconDefinition.property(new PropertyName(iconName.orElseThrow())).orElseThrow().constraints();
            } else if (SwitchWidgetPropertySchema.thumbIconLocalProperties().contains(name)) {
                constraints = stringPattern("(?:icon|inherit)", "Switch thumb icon state mode");
            } else if (SwitchWidgetPropertySchema.colorFamilies().stream()
                    .anyMatch(family -> SwitchWidgetPropertySchema.colorStateProperties(family).contains(name))) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (SwitchWidgetPropertySchema.outlineWidthStateProperties().contains(name)) {
                constraints = new ArrayList<>(checkboxDefinition.property(new PropertyName("splashRadius")).orElseThrow().constraints());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (name) {
                    case "onChanged", "onFocusChange" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool>"));
                    case "onActiveThumbImageError", "onInactiveThumbImageError" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ImageErrorListener"));
                    case "thumbColor", "trackColor", "trackOutlineColor", "overlayColor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"));
                    case "trackOutlineWidth" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<double?>"));
                    case "thumbIcon" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Icon?>"));
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "activeThumbImage", "inactiveThumbImage" -> List.of(new PropertyValueConstraint.ImageProviderValues());
                    case "activeColor", "activeThumbColor", "activeTrackColor", "inactiveThumbColor", "inactiveTrackColor", "focusColor", "hoverColor" -> colorOrTheme();
                    case "splashRadius", "mouseCursor", "materialTapTargetSize" -> checkboxDefinition.property(new PropertyName(name)).orElseThrow().constraints();
                    case "dragStartBehavior" -> gesturesEnumValues("DragStartBehavior", "down", "start");
                    case "padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                    case "applyCupertinoTheme" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "variant" -> stringPattern("(?:standard|adaptive)", "Switch constructor");
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.BooleanValue(false);
                case "enabled" -> new PropertyValue.BooleanValue(true);
                case "variant" -> new PropertyValue.StringValue("standard");
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(SwitchWidgetPropertySchema.SWITCH_TYPE.value(), "Switch", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, GESTURES_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 200, "Switch"), properties, List.of());
    }

    private static WidgetDefinition radio() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition shared = checkbox();
        for (var entry : RadioWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (List.of("fillColor", "overlayColor", "backgroundColor").stream()
                    .anyMatch(family -> RadioWidgetPropertySchema.colorStateProperties(family).contains(name))) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (RadioWidgetPropertySchema.innerRadiusStateProperties().contains(name) || name.equals("splashRadius")) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"),
                        List.of("infinity", "negativeInfinity")));
                if (!name.equals("splashRadius")) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (RadioWidgetPropertySchema.sideLocalProperties().contains(name)) {
                constraints = shared.property(new PropertyName(name)).orElseThrow().constraints();
            } else {
                constraints = switch (name) {
                    case "value", "groupValue" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(cardNumbers(null, null));
                        values.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"),
                                List.of("infinity", "negativeInfinity", "nan")));
                        values.addAll(any(PropertyValueKind.STRING));
                        values.addAll(any(PropertyValueKind.BOOLEAN));
                        values.addAll(any(PropertyValueKind.NULL));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("Object?"));
                        yield values;
                    }
                    case "valueType" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(stringPattern("(?:String|int|double|num|bool|Object)", "Radio value type"));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("Type"));
                        yield values;
                    }
                    case "onChanged" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(stringPattern("noop", "Radio no-op callback"));
                        values.addAll(any(PropertyValueKind.NULL));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<Object?>"));
                        yield values;
                    }
                    case "groupRegistry" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                            new PropertyValueConstraint.DartObjectReferenceValues("RadioGroupRegistry<Object>"));
                    case "fillColor", "overlayColor", "backgroundColor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"));
                    case "innerRadius" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<double?>"));
                    case "visualDensity" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VisualDensity"));
                    case "enabled" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "variant" -> stringPattern("(?:standard|adaptive)", "Radio constructor");
                    case "toggleable", "useCupertinoCheckmarkStyle", "nullableValueType" -> any(PropertyValueKind.BOOLEAN);
                    default -> shared.property(new PropertyName(name)).orElseThrow().constraints();
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.StringValue("option");
                case "valueType" -> new PropertyValue.StringValue("String");
                case "variant" -> new PropertyValue.StringValue("standard");
                case "onChanged" -> new PropertyValue.StringValue("noop");
                default -> null;
            };
            boolean required = List.of("value", "valueType", "variant").contains(name);
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), required, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), required, constraints, creation));
        }
        return widget(RadioWidgetPropertySchema.RADIO_TYPE.value(), "Radio", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 230, "Radio"), properties, List.of());
    }

    private static WidgetDefinition radioGroup() {
        WidgetDefinition shared = radio();
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var entry : RadioGroupWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints = shared.property(new PropertyName(name)).orElseThrow()
                    .constraints().stream().filter(value -> !name.equals("onChanged")
                            || value.kind() != PropertyValueKind.NULL).toList();
            boolean required = name.equals("valueType") || name.equals("onChanged");
            PropertyValue creation = name.equals("valueType") ? new PropertyValue.StringValue("String")
                    : name.equals("onChanged") ? new PropertyValue.StringValue("noop") : null;
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), required, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), required, constraints, creation));
        }
        return widget(RadioGroupWidgetPropertySchema.RADIO_GROUP_TYPE.value(), "RadioGroup", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 240, "RadioGroup"), properties,
                List.of(singleSlot("child", 2, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition checkbox() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition shapes = card();
        for (var entry : CheckboxWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (CheckboxWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                constraints = shapes.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (CheckboxWidgetPropertySchema.colorStateProperties("fillColor").contains(name)
                    || CheckboxWidgetPropertySchema.colorStateProperties("overlayColor").contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (CheckboxWidgetPropertySchema.sideLocalProperties().contains(name) && !name.equals("sideStateful")) {
                constraints = name.endsWith("Mode") ? stringPattern("(?:border|inherit)", "Checkbox side state mode")
                        : name.endsWith("Color") ? colorOrTheme()
                        : name.endsWith("Width") ? nonNegativeNumbers()
                        : name.endsWith("Style") ? enumValues("BorderStyle", "none", "solid")
                        : cardNumbers(null, null);
            } else {
                constraints = switch (name) {
                    case "value" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "onChanged" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool?>"));
                    case "fillColor", "overlayColor" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"));
                    case "shape" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("OutlinedBorder"));
                    case "side" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("BorderSide"));
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "activeColor", "checkColor", "focusColor", "hoverColor" -> colorOrTheme();
                    case "splashRadius" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(cardNumbers(null, null));
                        values.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity")));
                        yield List.copyOf(values);
                    }
                    case "visualDensityHorizontal", "visualDensityVertical" -> minusFourToFourDoubles();
                    case "materialTapTargetSize" -> materialEnumValues("MaterialTapTargetSize", "padded", "shrinkWrap");
                    case "mouseCursor" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(stringPattern(
                                DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset"));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"));
                        yield List.copyOf(values);
                    }
                    case "variant" -> stringPattern("(?:standard|adaptive)", "Checkbox constructor");
                    case "semanticLabel" -> any(PropertyValueKind.STRING);
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.BooleanValue(false);
                case "enabled" -> new PropertyValue.BooleanValue(true);
                case "variant" -> new PropertyValue.StringValue("standard");
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(CheckboxWidgetPropertySchema.CHECKBOX_TYPE.value(), "Checkbox", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 190, "Checkbox"), properties, List.of());
    }

    private static WidgetDefinition iconButton() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition shared = fullStyleButton("TextButton");
        for (var entry : IconButtonWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (IconButtonWidgetPropertySchema.localStyleProperties().contains(name)) {
                constraints = shared.property(new PropertyName(name)).orElseThrow().constraints();
            } else {
                constraints = switch (name) {
                    case "iconSize", "splashRadius" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(name.equals("iconSize")
                                ? cardNumbers(null, null) : List.of(
                                        new PropertyValueConstraint.IntegerRange(BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                                        new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, false, null, true)));
                        values.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity")));
                        yield List.copyOf(values);
                    }
                    case "visualDensityHorizontal", "visualDensityVertical" -> minusFourToFourDoubles();
                    case "padding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                    case "alignment" -> List.of(new PropertyValueConstraint.AlignmentGeometryValues());
                    case "color", "focusColor", "hoverColor", "highlightColor", "splashColor", "disabledColor" -> colorOrTheme();
                    case "onPressed", "onLongPress" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback"));
                    case "onHover" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool>"));
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "statesController" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStatesController"));
                    case "style" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ButtonStyle"));
                    case "mouseCursor" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(stringPattern(
                                DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset"));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"));
                        yield List.copyOf(values);
                    }
                    case "constraints" -> List.of(new PropertyValueConstraint.BoxConstraintsValues());
                    case "isSelected" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "variant" -> stringPattern("(?:standard|filled|filledTonal|outlined)", "IconButton constructor");
                    case "tooltip" -> any(PropertyValueKind.STRING);
                    default -> any(PropertyValueKind.BOOLEAN);
                };
            }
            properties.add(name.equals("variant")
                    ? namedProperty(name, entry.getValue().dartOrder(), true, constraints, new PropertyValue.StringValue("standard"))
                    : name.equals("enabled")
                            ? namedProperty(name, entry.getValue().dartOrder(), true, constraints, new PropertyValue.BooleanValue(true))
                            : namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        return widget(IconButtonWidgetPropertySchema.ICON_BUTTON_TYPE.value(), "IconButton", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 180, "IconButton"), properties,
                List.of(singleSlot("icon", 25, true, 1, ANY_WIDGET), singleSlot("selectedIcon", 23, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition floatingActionButton() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition shapes = card();
        for (var entry : FloatingActionButtonWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            if (FloatingActionButtonWidgetPropertySchema.isTextStyleProperty(new PropertyName(name))) {
                continue;
            }
            List<PropertyValueConstraint> constraints;
            if (name.equals("shape") || CardWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                constraints = shapes.property(new PropertyName(name)).orElseThrow().constraints();
            } else {
                constraints = switch (name) {
                    case "foregroundColor", "backgroundColor", "focusColor", "hoverColor", "splashColor" -> colorOrTheme();
                    case "enabled", "mini", "autofocus", "isExtended", "enableFeedback" -> any(PropertyValueKind.BOOLEAN);
                    case "variant" -> stringPattern("(?:standard|small|large|extended)", "FloatingActionButton constructor");
                    case "tooltip" -> any(PropertyValueKind.STRING);
                    case "heroTag" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(cardNumbers(null, null));
                        values.addAll(any(PropertyValueKind.NULL));
                        values.addAll(any(PropertyValueKind.STRING));
                        values.addAll(any(PropertyValueKind.BOOLEAN));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("Object"));
                        yield List.copyOf(values);
                    }
                    case "onPressed" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback"));
                    case "focusNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"));
                    case "mouseCursor" -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(stringPattern(
                                DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset"));
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"));
                        yield List.copyOf(values);
                    }
                    case "clipBehavior" -> enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                    case "materialTapTargetSize" -> materialEnumValues("MaterialTapTargetSize", "padded", "shrinkWrap");
                    case "extendedPadding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                    default -> {
                        List<PropertyValueConstraint> values = new ArrayList<>(cardNumbers(
                                name.equals("extendedIconLabelSpacing") ? null : BigDecimal.ZERO, null));
                        values.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity")));
                        yield List.copyOf(values);
                    }
                };
            }
            properties.add(name.equals("variant")
                    ? namedProperty(name, entry.getValue().dartOrder(), true, constraints, new PropertyValue.StringValue("standard"))
                    : name.equals("enabled")
                            ? namedProperty(name, entry.getValue().dartOrder(), true, constraints, new PropertyValue.BooleanValue(true))
                            : namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        appendTextStyleProperties(properties, "extendedTextStyle", 25);
        return widget(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE.value(), "FloatingActionButton", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 170, "FloatingActionButton"), properties,
                List.of(singleSlot("child", 0, true, 0, ANY_WIDGET), singleSlot("icon", 56, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition card() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var entry : CardWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints = switch (name) {
                case "color", "shadowColor", "surfaceTintColor", "shapeSideColor" -> colorOrTheme();
                case "borderOnForeground", "semanticContainer" -> any(PropertyValueKind.BOOLEAN);
                case "margin" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(true));
                case "clipBehavior" -> enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                case "shapeSideStyle" -> enumValues("BorderStyle", "none", "solid");
                case "variant" -> stringPattern("(?:elevated|filled|outlined)", "Card constructor variant");
                case "shapeKind" -> stringPattern("(?:" + String.join("|", CardWidgetPropertySchema.shapeKinds()) + ")", "Card ShapeBorder constructor");
                case "shape" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"));
                case "shapeRadius" -> List.of(new PropertyValueConstraint.BorderRadiusValues());
                case "elevation", "shapeSideWidth" -> nonNegativeNumbers();
                case "shapePoints" -> cardNumbers(BigDecimal.valueOf(2), null);
                case "shapeCircleEccentricity", "shapeInnerRadiusRatio", "shapePointRounding", "shapeValleyRounding", "shapeSquash",
                        "shapeStartSize", "shapeEndSize", "shapeTopSize", "shapeBottomSize" -> cardNumbers(BigDecimal.ZERO, BigDecimal.ONE);
                default -> cardNumbers(null, null);
            };
            properties.add(name.equals("variant")
                    ? namedProperty(name, entry.getValue().dartOrder(), true, constraints, new PropertyValue.StringValue("elevated"))
                    : namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        return widget(CardWidgetPropertySchema.CARD_TYPE.value(), "Card", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 70, "Card"), properties,
                List.of(singleSlot("child", 8, false, 0, ANY_WIDGET)));
    }

    private static List<PropertyValueConstraint> cardNumbers(BigDecimal minimum, BigDecimal maximum) {
        return List.of(new PropertyValueConstraint.IntegerRange(
                minimum == null ? DartNumericLiterals.MIN_PORTABLE_INTEGER : minimum.toBigIntegerExact(),
                maximum == null ? DartNumericLiterals.MAX_PORTABLE_INTEGER : maximum.toBigIntegerExact()),
                new PropertyValueConstraint.DoubleRange(minimum, true, maximum, true));
    }

    private static WidgetDefinition divider() {
        return widget(
                DividerWidgetPropertySchema.DIVIDER_TYPE.value(),
                "Divider", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 50, "Divider"),
                List.of(namedProperty("height", 0, false, nonNegativeNumbers()),
                        namedProperty("thickness", 1, false, nonNegativeNumbers()),
                        namedProperty("indent", 2, false, nonNegativeNumbers()),
                        namedProperty("endIndent", 3, false, nonNegativeNumbers()),
                        namedProperty("color", 4, false, colorOrTheme()),
                        namedProperty("radius", 5, false, List.of(new PropertyValueConstraint.BorderRadiusValues()))),
                List.of());
    }

    private static WidgetDefinition verticalDivider() {
        return widget(
                VerticalDividerWidgetPropertySchema.VERTICAL_DIVIDER_TYPE.value(),
                "VerticalDivider", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 60, "VerticalDivider"),
                List.of(namedProperty("width", 0, false, nonNegativeNumbers()),
                        namedProperty("thickness", 1, false, nonNegativeNumbers()),
                        namedProperty("indent", 2, false, nonNegativeNumbers()),
                        namedProperty("endIndent", 3, false, nonNegativeNumbers()),
                        namedProperty("color", 4, false, colorOrTheme()),
                        namedProperty("radius", 5, false, List.of(new PropertyValueConstraint.BorderRadiusValues()))),
                List.of());
    }

    private static WidgetDefinition imageIcon() {
        return widget(
                ImageIconWidgetPropertySchema.IMAGE_ICON_TYPE.value(),
                "ImageIcon",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 230, "ImageIcon"),
                List.of(
                        new PropertyDefinition(new PropertyName("image"),
                                DartParameter.positional(0),
                                List.of(new PropertyValueConstraint.ImageProviderValues(),
                                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)),
                                Optional.of(new PropertyValue.NullValue())),
                        namedProperty("size", 0, false, nonNegativeNumbers()),
                        namedProperty("color", 1, false, colorOrTheme()),
                        namedProperty("semanticLabel", 2, false, any(PropertyValueKind.STRING))),
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

    private static WidgetDefinition indexedStack() {
        List<PropertyDefinition> properties = List.of(
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
                        "clipBehavior",
                        2,
                        false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")),
                namedProperty(
                        "sizing",
                        3,
                        false,
                        enumValues("StackFit", "loose", "expand", "passthrough")),
                namedProperty(
                        "index",
                        4,
                        false,
                        List.of(
                                new PropertyValueConstraint.IntegerRange(
                                        BigInteger.ZERO,
                                        DartNumericLiterals.MAX_PORTABLE_INTEGER),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        if (properties.size()
                != IndexedStackWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "IndexedStack catalog/property schema count mismatch");
        }
        return widget(
                IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE.value(),
                "IndexedStack",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 115, "IndexedStack"),
                properties,
                List.of(listSlot("children", 5, false, ANY_WIDGET)));
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

    private static WidgetDefinition intrinsicWidth() {
        return widget(
                "flutter.widgets.IntrinsicWidth",
                "IntrinsicWidth",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 170, "IntrinsicWidth"),
                List.of(
                        namedProperty("stepWidth", 0, false, nonNegativeDoubles()),
                        namedProperty("stepHeight", 1, false, nonNegativeDoubles())),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition offstage() {
        return widget(
                "flutter.widgets.Offstage",
                "Offstage",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 180, "Offstage"),
                List.of(namedProperty(
                        "offstage",
                        0,
                        false,
                        any(PropertyValueKind.BOOLEAN))),
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sizedOverflowBox() {
        return widget(
                "flutter.widgets.SizedOverflowBox",
                "SizedOverflowBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 190, "SizedOverflowBox"),
                List.of(
                        namedProperty(
                                "size",
                                0,
                                true,
                                List.of(new PropertyValueConstraint.SizeValues()),
                                new PropertyValue.SizeValue(
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(100))),
                        namedProperty(
                                "alignment",
                                1,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues()))),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition transform() {
        return widget(
                "flutter.widgets.Transform",
                "Transform",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 200, "Transform"),
                List.of(
                        namedProperty(
                                "transform",
                                0,
                                true,
                                List.of(new PropertyValueConstraint.Matrix4Values()),
                                identityMatrix()),
                        namedProperty(
                                "origin",
                                1,
                                false,
                                List.of(new PropertyValueConstraint.OffsetValues())),
                        namedProperty(
                                "alignment",
                                2,
                                false,
                                List.of(new PropertyValueConstraint.AlignmentGeometryValues())),
                        namedProperty(
                                "transformHitTests",
                                3,
                                false,
                                any(PropertyValueKind.BOOLEAN)),
                        namedProperty(
                                "filterQuality",
                                4,
                                false,
                                enumValues(
                                        "FilterQuality", "none", "low", "medium", "high"))),
                List.of(singleSlot("child", 5, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition rotatedBox() {
        return widget(
                "flutter.widgets.RotatedBox",
                "RotatedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 210, "RotatedBox"),
                List.of(namedProperty(
                        "quarterTurns",
                        0,
                        true,
                        portableIntegers(),
                        new PropertyValue.IntegerValue(BigInteger.ONE))),
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition listBody() {
        return widget(
                "flutter.widgets.ListBody",
                "ListBody",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 220, "ListBody"),
                List.of(
                        namedProperty("mainAxis", 0, false,
                                enumValues("Axis", "horizontal", "vertical")),
                        namedProperty("reverse", 1, false,
                                any(PropertyValueKind.BOOLEAN))),
                List.of(listSlot("children", 2, false, ANY_WIDGET)));
    }

    private static WidgetDefinition overflowBar() {
        return widget(
                "flutter.widgets.OverflowBar",
                "OverflowBar",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 230, "OverflowBar"),
                List.of(
                        namedProperty("spacing", 0, false, finiteDoubles()),
                        namedProperty("alignment", 1, false,
                                enumValues(
                                        "MainAxisAlignment", "start", "end", "center",
                                        "spaceBetween", "spaceAround", "spaceEvenly")),
                        namedProperty("overflowSpacing", 2, false, finiteDoubles()),
                        namedProperty("overflowAlignment", 3, false,
                                enumValues(
                                        "OverflowBarAlignment", "start", "end", "center")),
                        namedProperty("overflowDirection", 4, false,
                                enumValues("VerticalDirection", "up", "down")),
                        namedProperty("textDirection", 5, false,
                                enumValues("TextDirection", "rtl", "ltr"))),
                List.of(listSlot("children", 6, false, ANY_WIDGET)));
    }

    private static WidgetDefinition safeArea() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("left", 0, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("top", 1, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("right", 2, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("bottom", 3, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty(
                        "minimum",
                        4,
                        false,
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(
                                false, false))),
                namedProperty(
                        "maintainBottomViewPadding",
                        5,
                        false,
                        any(PropertyValueKind.BOOLEAN)));
        if (properties.size()
                != SafeAreaWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "SafeArea catalog/property schema count mismatch");
        }
        return widget(
                SafeAreaWidgetPropertySchema.SAFE_AREA_TYPE.value(),
                "SafeArea",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.layout", 200, 240, "SafeArea"),
                properties,
                List.of(singleSlot("child", 6, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition clipRect() {
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "clipper",
                        0,
                        false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "CustomClipper<Rect>"))),
                namedProperty(
                        "clipBehavior",
                        1,
                        false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size() != ClipRectWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipRect catalog/property schema count mismatch");
        }
        return widget(
                ClipRectWidgetPropertySchema.CLIP_RECT_TYPE.value(),
                "ClipRect",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 80, "ClipRect"),
                properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition clipOval() {
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "clipper",
                        0,
                        false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "CustomClipper<Rect>"))),
                namedProperty(
                        "clipBehavior",
                        1,
                        false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size() != ClipOvalWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipOval catalog/property schema count mismatch");
        }
        return widget(
                ClipOvalWidgetPropertySchema.CLIP_OVAL_TYPE.value(),
                "ClipOval",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 90, "ClipOval"),
                properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition clipRRect() {
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "borderRadius",
                        0,
                        false,
                        List.of(new PropertyValueConstraint.BorderRadiusValues())),
                namedProperty(
                        "clipper",
                        1,
                        false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "CustomClipper<RRect>"))),
                namedProperty(
                        "clipBehavior",
                        2,
                        false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size()
                != ClipRRectWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipRRect catalog/property schema count mismatch");
        }
        return widget(
                ClipRRectWidgetPropertySchema.CLIP_RRECT_TYPE.value(),
                "ClipRRect",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 100, "ClipRRect"),
                properties,
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition clipPath() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("clipper", 0, false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "CustomClipper<Path>"))),
                namedProperty("shape", 1, false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "ShapeBorder"))),
                namedProperty("clipBehavior", 2, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size() != ClipPathWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ClipPath catalog/property schema count mismatch");
        }
        return widget(
                ClipPathWidgetPropertySchema.CLIP_PATH_TYPE.value(),
                "ClipPath", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 110, "ClipPath"), properties,
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition clipRSuperellipse() {
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "borderRadius",
                        0,
                        false,
                        List.of(new PropertyValueConstraint.BorderRadiusValues())),
                namedProperty(
                        "clipper",
                        1,
                        false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                                "CustomClipper<RSuperellipse>"))),
                namedProperty(
                        "clipBehavior",
                        2,
                        false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")));
        if (properties.size()
                != ClipRSuperellipseWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ClipRSuperellipse catalog/property schema count mismatch");
        }
        return widget(
                ClipRSuperellipseWidgetPropertySchema.CLIP_RSUPERELLIPSE_TYPE.value(),
                "ClipRSuperellipse",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 120, "ClipRSuperellipse"),
                properties,
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition physicalModel() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("shape", 0, false, enumValues("BoxShape", "rectangle", "circle")),
                namedProperty("clipBehavior", 1, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")),
                namedProperty("borderRadius", 2, false,
                        List.of(new PropertyValueConstraint.BorderRadiusValues(false))),
                namedProperty("elevation", 3, false, nonNegativeNumbers()),
                namedProperty("color", 4, true, colorOrTheme(),
                        new PropertyValue.ColorValue(0xFF2196F3L)),
                namedProperty("shadowColor", 5, false, colorOrTheme()));
        if (properties.size() != PhysicalModelWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("PhysicalModel catalog/property schema count mismatch");
        }
        return widget(
                PhysicalModelWidgetPropertySchema.PHYSICAL_MODEL_TYPE.value(),
                "PhysicalModel", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 130, "PhysicalModel"), properties,
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition ignorePointer() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("ignoring", 0, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("ignoringSemantics", 1, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != IgnorePointerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("IgnorePointer catalog/property schema count mismatch");
        }
        return widget(
                IgnorePointerWidgetPropertySchema.IGNORE_POINTER_TYPE.value(),
                "IgnorePointer", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 160, "IgnorePointer"), properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition absorbPointer() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("absorbing", 0, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("ignoringSemantics", 1, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != AbsorbPointerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("AbsorbPointer catalog/property schema count mismatch");
        }
        return widget(
                AbsorbPointerWidgetPropertySchema.ABSORB_POINTER_TYPE.value(),
                "AbsorbPointer", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 170, "AbsorbPointer"), properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition repaintBoundary() {
        return widget(
                "flutter.widgets.RepaintBoundary",
                "RepaintBoundary",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 150, "RepaintBoundary"),
                List.of(),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition physicalShape() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("clipper", 0, true,
                        List.of(new PropertyValueConstraint.ShapeBorderClipperValues(),
                                new PropertyValueConstraint.DartObjectReferenceValues("CustomClipper<Path>")),
                        PropertyValue.ShapeBorderClipperValue.defaultValue()),
                namedProperty("clipBehavior", 1, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")),
                namedProperty("elevation", 2, false, nonNegativeNumbers()),
                namedProperty("color", 3, true, colorOrTheme(), new PropertyValue.ColorValue(0xFF2196F3L)),
                namedProperty("shadowColor", 4, false, colorOrTheme()));
        if (properties.size() != PhysicalShapeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("PhysicalShape catalog/property schema count mismatch");
        }
        return widget(PhysicalShapeWidgetPropertySchema.PHYSICAL_SHAPE_TYPE.value(),
                "PhysicalShape", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 140, "PhysicalShape"), properties,
                List.of(singleSlot("child", 5, false, 0, ANY_WIDGET)));
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

    private static WidgetDefinition gridViewCount() {
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
                namedProperty("crossAxisCount", 6, true,
                        positiveIntegers(),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                namedProperty("mainAxisSpacing", 7, false,
                        nonNegativeDoubles()),
                namedProperty("crossAxisSpacing", 8, false,
                        nonNegativeDoubles()),
                namedProperty("childAspectRatio", 9, false,
                        positiveDoubles()),
                namedProperty("mainAxisExtent", 10, false,
                        nonNegativeDoubles()),
                namedProperty("addAutomaticKeepAlives", 11, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("addRepaintBoundaries", 12, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("addSemanticIndexes", 13, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("scrollCacheExtent", 14, false,
                        nonNegativeNumbers()),
                namedProperty("semanticChildCount", 16, false,
                        nonNegativeIntegers()),
                namedProperty("dragStartBehavior", 17, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("keyboardDismissBehavior", 18, false,
                        enumValues("ScrollViewKeyboardDismissBehavior", "manual", "onDrag")),
                namedProperty("restorationId", 19, false,
                        stringLength(1, 256)),
                namedProperty("clipBehavior", 20, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 21, false,
                        renderingEnumValues(
                                "HitTestBehavior", "deferToChild", "opaque", "translucent")));
        if (properties.size()
                != GridViewCountWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "GridView.count catalog/property schema count mismatch");
        }
        return namedWidget(
                GridViewCountWidgetPropertySchema.GRID_VIEW_COUNT_TYPE.value(),
                "GridView",
                "count",
                false,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT),
                Set.of(),
                palette("flutter.scrolling", 250, 20, "GridView.count"),
                properties,
                List.of(listSlot("children", 15, false, ANY_WIDGET)));
    }

    private static WidgetDefinition singleChildScrollView() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("scrollDirection", 0, false,
                        enumValues("Axis", "horizontal", "vertical")),
                namedProperty("reverse", 1, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("padding", 2, false,
                        List.of(new PropertyValueConstraint.EdgeInsetsValues(true))),
                namedProperty("primary", 3, false,
                        any(PropertyValueKind.BOOLEAN)),
                namedProperty("physics", 4, false, stringPattern(
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)",
                        "reviewed ScrollPhysics preset")),
                namedProperty("dragStartBehavior", 6, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("clipBehavior", 7, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias",
                                "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 8, false,
                        renderingEnumValues(
                                "HitTestBehavior", "deferToChild", "opaque", "translucent")),
                namedProperty("restorationId", 9, false,
                        stringLength(1, 256)),
                namedProperty("keyboardDismissBehavior", 10, false,
                        enumValues("ScrollViewKeyboardDismissBehavior", "manual", "onDrag")));
        if (properties.size()
                != SingleChildScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "SingleChildScrollView catalog/property schema count mismatch");
        }
        return widget(
                SingleChildScrollViewWidgetPropertySchema
                        .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
                "SingleChildScrollView",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT),
                Set.of(),
                palette("flutter.scrolling", 250, 30, "SingleChildScrollView"),
                properties,
                List.of(singleSlot("child", 5, false, 0, ANY_WIDGET)));
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

    private static WidgetDefinition coloredBox() {
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "color",
                        0,
                        true,
                        colorOrTheme(),
                        new PropertyValue.ColorValue(0xFF2196F3L)),
                namedProperty(
                        "isAntiAlias",
                        1,
                        false,
                        any(PropertyValueKind.BOOLEAN)));
        if (properties.size()
                != ColoredBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ColoredBox catalog/property schema count mismatch");
        }
        return widget(
                ColoredBoxWidgetPropertySchema.COLORED_BOX_TYPE.value(),
                "ColoredBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 40, "ColoredBox"),
                properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition placeholder() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("color", 0, false, colorOrTheme()),
                namedProperty("strokeWidth", 1, false, nonNegativeNumbers()),
                namedProperty("fallbackWidth", 2, false, nonNegativeNumbers()),
                namedProperty("fallbackHeight", 3, false, nonNegativeNumbers()));
        if (properties.size()
                != PlaceholderWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Placeholder catalog/property schema count mismatch");
        }
        return widget(
                PlaceholderWidgetPropertySchema.PLACEHOLDER_TYPE.value(),
                "Placeholder",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 50, "Placeholder"),
                properties,
                List.of(singleSlot("child", 4, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition directionality() {
        List<PropertyDefinition> properties = List.of(namedProperty(
                "textDirection",
                0,
                true,
                enumValues("TextDirection", "rtl", "ltr"),
                new PropertyValue.EnumValue("TextDirection", "ltr")));
        if (properties.size()
                != DirectionalityWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "Directionality catalog/property schema count mismatch");
        }
        return widget(
                DirectionalityWidgetPropertySchema.DIRECTIONALITY_TYPE.value(),
                "Directionality",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 60, "Directionality"),
                properties,
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition decoratedBox() {
        List<String> reviewedColorThemeTokens = MaterialThemeTokenCatalog.colorRoles()
                .keySet().stream().sorted().toList();
        List<PropertyDefinition> properties = List.of(
                namedProperty(
                        "decoration",
                        0,
                        true,
                        List.of(new PropertyValueConstraint.BoxDecorationValues(
                                reviewedColorThemeTokens)),
                        emptyBoxDecoration()),
                namedProperty(
                        "position",
                        1,
                        false,
                        renderingEnumValues(
                                "DecorationPosition", "background", "foreground")));
        if (properties.size()
                != DecoratedBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "DecoratedBox catalog/property schema count mismatch");
        }
        return widget(
                DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE.value(),
                "DecoratedBox",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, RENDERING_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 70, "DecoratedBox"),
                properties,
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition excludeSemantics() {
        List<PropertyDefinition> properties = List.of(namedProperty(
                "excluding",
                0,
                false,
                any(PropertyValueKind.BOOLEAN)));
        if (properties.size()
                != ExcludeSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ExcludeSemantics catalog/property schema count mismatch");
        }
        return widget(
                ExcludeSemanticsWidgetPropertySchema.EXCLUDE_SEMANTICS_TYPE.value(),
                "ExcludeSemantics",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.accessibility", 400, 10, "ExcludeSemantics"),
                properties,
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition blockSemantics() {
        List<PropertyDefinition> properties = List.of(namedProperty(
                "blocking", 0, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != BlockSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("BlockSemantics catalog/property schema count mismatch");
        }
        return widget(
                BlockSemanticsWidgetPropertySchema.BLOCK_SEMANTICS_TYPE.value(),
                "BlockSemantics", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.accessibility", 400, 20, "BlockSemantics"), properties,
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition mergeSemantics() {
        return widget(
                "flutter.widgets.MergeSemantics", "MergeSemantics", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.accessibility", 400, 30, "MergeSemantics"), List.of(),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition indexedSemantics() {
        List<PropertyDefinition> properties = List.of(namedProperty("index", 0, true,
                portableIntegers(), new PropertyValue.IntegerValue(BigInteger.ZERO)));
        if (properties.size() != IndexedSemanticsWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("IndexedSemantics catalog/property schema count mismatch");
        }
        return widget(
                IndexedSemanticsWidgetPropertySchema.INDEXED_SEMANTICS_TYPE.value(),
                "IndexedSemantics", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.accessibility", 400, 40, "IndexedSemantics"), properties,
                List.of(singleSlot("child", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition excludeFocus() {
        List<PropertyDefinition> properties = List.of(namedProperty(
                "excluding", 0, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != ExcludeFocusWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ExcludeFocus catalog/property schema count mismatch");
        }
        return widget(
                ExcludeFocusWidgetPropertySchema.EXCLUDE_FOCUS_TYPE.value(),
                "ExcludeFocus", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.accessibility", 400, 50, "ExcludeFocus"), properties,
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition excludeFocusTraversal() {
        List<PropertyDefinition> properties = List.of(namedProperty(
                "excluding", 0, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != ExcludeFocusTraversalWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ExcludeFocusTraversal catalog/property schema count mismatch");
        }
        return widget(
                ExcludeFocusTraversalWidgetPropertySchema.EXCLUDE_FOCUS_TRAVERSAL_TYPE.value(),
                "ExcludeFocusTraversal", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.accessibility", 400, 60, "ExcludeFocusTraversal"), properties,
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition visibility() {
        List<PropertyDefinition> properties = new ArrayList<>();
        VisibilityWidgetPropertySchema.definitions().forEach((name, schema) -> properties.add(
                namedProperty(name, schema.dartOrder(), false, any(PropertyValueKind.BOOLEAN))));
        if (properties.size() != VisibilityWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Visibility catalog/property schema count mismatch");
        }
        return widget(VisibilityWidgetPropertySchema.VISIBILITY_TYPE.value(), "Visibility", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 180, "Visibility"), properties,
                List.of(singleSlot("child", 0, true, 1, ANY_WIDGET),
                        singleSlot("replacement", 1, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition tickerMode() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("enabled", 0, true, any(PropertyValueKind.BOOLEAN),
                        new PropertyValue.BooleanValue(true)),
                namedProperty("forceFrames", 2, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != TickerModeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("TickerMode catalog/property schema count mismatch");
        }
        return widget(TickerModeWidgetPropertySchema.TICKER_MODE_TYPE.value(), "TickerMode", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 190, "TickerMode"), properties,
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition defaultTextHeightBehavior() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("textHeightApplyFirstAscent", 0, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("textHeightApplyLastDescent", 1, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("textHeightLeadingDistribution", 2, false,
                        enumValues("TextLeadingDistribution", "proportional", "even")));
        if (properties.size() != DefaultTextHeightBehaviorWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("DefaultTextHeightBehavior catalog/property schema count mismatch");
        }
        return widget(DefaultTextHeightBehaviorWidgetPropertySchema.DEFAULT_TEXT_HEIGHT_BEHAVIOR_TYPE.value(),
                "DefaultTextHeightBehavior", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 200, "DefaultTextHeightBehavior"), properties,
                List.of(singleSlot("child", 3, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition iconTheme() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("size", 0, false, nonNegativeNumbers()),
                namedProperty("fill", 1, false, zeroToOneDoubles()),
                namedProperty("weight", 2, false, positiveFontAxisDoubles()),
                namedProperty("grade", 3, false, gradeAxisDoubles()),
                namedProperty("opticalSize", 4, false, positiveFontAxisDoubles()),
                namedProperty("color", 5, false, colorOrTheme()),
                namedProperty("opacity", 6, false, finiteDoubles()),
                namedProperty("shadows", 7, false, shadowValues()),
                namedProperty("applyTextScaling", 8, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("merge", 10, true, any(PropertyValueKind.BOOLEAN),
                        new PropertyValue.BooleanValue(false)));
        if (properties.size() != IconThemeWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("IconTheme catalog/property schema count mismatch");
        }
        return widget(IconThemeWidgetPropertySchema.ICON_THEME_TYPE.value(),
                "IconTheme", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 220, "IconTheme"), properties,
                List.of(singleSlot("child", 9, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition defaultSelectionStyle() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("cursorColor", 0, false, colorOrTheme()),
                namedProperty("selectionColor", 1, false, colorOrTheme()),
                namedProperty("mouseCursor", 2, false, stringPattern(
                        DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset")),
                namedProperty("merge", 4, true, any(PropertyValueKind.BOOLEAN),
                        new PropertyValue.BooleanValue(false)));
        if (properties.size() != DefaultSelectionStyleWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("DefaultSelectionStyle catalog/property schema count mismatch");
        }
        return widget(DefaultSelectionStyleWidgetPropertySchema.DEFAULT_SELECTION_STYLE_TYPE.value(),
                "DefaultSelectionStyle", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.basic", 300, 210, "DefaultSelectionStyle"), properties,
                List.of(singleSlot("child", 3, true, 1, ANY_WIDGET)));
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

    private static WidgetDefinition textButton() {
        return fullStyleButton("TextButton");
    }

    private static WidgetDefinition fullStyleButton(String familyName) {
        boolean outlined = !familyName.equals("TextButton");
        boolean filled = familyName.equals("FilledButton");
        List<PropertyDefinition> properties = new ArrayList<>();
        int order = 0;
        properties.add(namedProperty("enabled", order++, true,
                any(PropertyValueKind.BOOLEAN), new PropertyValue.BooleanValue(true)));
        for (String name : List.of("onPressed", "onLongPress", "onHover", "onFocusChange")) {
            properties.add(namedProperty(name, order++, false,
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                            name.equals("onPressed") || name.equals("onLongPress")
                                    ? "VoidCallback" : "ValueChanged<bool>"))));
        }
        properties.add(namedProperty("focusNode", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode"))));
        properties.add(namedProperty("autofocus", order++, false, any(PropertyValueKind.BOOLEAN)));
        List<PropertyValueConstraint> clip = new ArrayList<>(enumValues(
                "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        clip.addAll(any(PropertyValueKind.NULL));
        properties.add(namedProperty("clipBehavior", order++, false, List.copyOf(clip)));
        properties.add(namedProperty("statesController", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStatesController"))));
        if (!outlined) {
            List<PropertyValueConstraint> semantics = new ArrayList<>(any(PropertyValueKind.BOOLEAN));
            semantics.addAll(any(PropertyValueKind.NULL));
            properties.add(namedProperty("isSemanticButton", order++, false, List.copyOf(semantics)));
        }
        properties.add(namedProperty("iconAlignment", order++, false,
                materialEnumValues("IconAlignment", "start", "end")));
        properties.add(namedProperty("variant", order++, true,
                stringPattern(filled ? "(?:standard|icon|tonal|tonalIcon)" : "(?:standard|icon)",
                        familyName + " constructor variant"),
                new PropertyValue.StringValue("standard")));
        for (String prefix : TextButtonWidgetPropertySchema.statePrefixes()) {
            order = appendElevatedButtonStateProperties(properties, prefix, order);
            order = appendElevatedButtonTextStyleProperties(properties, prefix, order);
        }
        WidgetDefinition shared = elevatedButton();
        for (PropertyDefinition property : shared.properties()) {
            if (ElevatedButtonWidgetPropertySchema.find(property.name()).orElseThrow().group()
                    == ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE) {
                properties.add(namedProperty(property.name().value(), order++, false, property.constraints()));
            }
        }
        properties.add(namedProperty("styleIconAlignment", order++, false,
                materialEnumValues("IconAlignment", "start", "end")));
        for (String name : List.of("styleBackgroundBuilder", "styleForegroundBuilder")) {
            properties.add(namedProperty(name, order++, false,
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues("ButtonLayerBuilder"))));
        }
        properties.add(namedProperty("style", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues("ButtonStyle"))));
        if (properties.size() != (outlined ? OutlinedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT
                : TextButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT)) {
            throw new ExceptionInInitializerError(familyName + " catalog property count: " + properties.size());
        }
        return widget("flutter.material." + familyName, familyName, true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, filled ? 160 : outlined ? 150 : 140, familyName), List.copyOf(properties),
                List.of(singleSlot("child", order++, true, filled ? 0 : 1, ANY_WIDGET),
                        singleSlot("icon", order, false, 0, ANY_WIDGET)));
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

    private static WidgetDefinition namedWidget(
            String typeId,
            String dartClassName,
            String namedConstructor,
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
                Optional.of(namedConstructor),
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

    private static PropertyValue.Matrix4Value identityMatrix() {
        return new PropertyValue.Matrix4Value(List.of(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
    }

    private static PropertyValue.BoxDecorationValue emptyBoxDecoration() {
        return new PropertyValue.BoxDecorationValue(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
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

    private static List<PropertyValueConstraint> portableIntegers() {
        return List.of(new PropertyValueConstraint.IntegerRange(
                DartNumericLiterals.MIN_PORTABLE_INTEGER,
                DartNumericLiterals.MAX_PORTABLE_INTEGER));
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

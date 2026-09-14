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
    public static final String SLIVER_WIDGET_TRAIT = "flutter.widgets.Sliver";

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
            sliverAppBar(""), sliverAppBar("medium"), sliverAppBar("large"),
            flexibleSpaceBar(),
                flexibleSpaceBarSettings(),
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
            preferredSize(),
            listBody(),
            overflowBar(),
            safeArea(),
            listView(),
            gridViewCount(),
            gridViewExtent(),
            singleChildScrollView(),
            pageView(),
            listWheelScrollView(),
            customScrollView(),
            sliverToBoxAdapter(),
            sliverChildren(SliverChildrenWidgetPropertySchema.LIST, "SliverList", "list", 80),
            sliverChildren(SliverChildrenWidgetPropertySchema.GRID_COUNT, "SliverGrid", "count", 90),
            sliverChildren(SliverChildrenWidgetPropertySchema.GRID_EXTENT, "SliverGrid", "extent", 100),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.LIST_BUILDER),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.LIST_SEPARATED),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.LIST_DELEGATE),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.GRID_BUILDER),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.GRID_LIST),
            sliverDynamic(SliverDynamicWidgetPropertySchema.Kind.GRID_DELEGATE),
            sliverPadding(),
            sliverFillRemaining(),
            sliverFillViewport(false),
            sliverFillViewport(true),
            sliverFixedExtentList(SliverFixedExtentListWidgetPropertySchema.Kind.LIST),
            sliverFixedExtentList(SliverFixedExtentListWidgetPropertySchema.Kind.BUILDER),
            sliverFixedExtentList(SliverFixedExtentListWidgetPropertySchema.Kind.DELEGATE),
            sliverPrototypeExtentList(SliverPrototypeExtentListWidgetPropertySchema.Kind.LIST),
            sliverPrototypeExtentList(SliverPrototypeExtentListWidgetPropertySchema.Kind.BUILDER),
            sliverPrototypeExtentList(SliverPrototypeExtentListWidgetPropertySchema.Kind.DELEGATE),
            sliverVariedExtentList(SliverVariedExtentListWidgetPropertySchema.Kind.LIST),
            sliverVariedExtentList(SliverVariedExtentListWidgetPropertySchema.Kind.BUILDER),
            sliverVariedExtentList(SliverVariedExtentListWidgetPropertySchema.Kind.DELEGATE),
            sliverMainAxisGroup(),
            sliverCrossAxisGroup(),
            sliverCrossAxisExpanded(),
            sliverConstrainedCrossAxis(),
            sliverOpacity(),
            sliverIgnorePointer(),
            sliverOffstage(),
            sliverVisibility(false),
            sliverVisibility(true),
            sliverSafeArea(),
            sliverAnimatedOpacity(),
            animatedOpacity(),
            animatedAlign(),
            animatedPadding(),
            animatedSlide(),
            animatedScale(),
            animatedRotation(),
            animatedContainer(),
            animatedSize(),
            animatedDefaultTextStyle(),
            defaultTextStyle(false),
            defaultTextStyle(true),
            defaultTextStyleTransition(),
            fadeTransition(false),
            slideTransition(),
            scaleTransition(),
            rotationTransition(),
            sizeTransition(),
            positionedTransition(),
            relativePositionedTransition(),
            decoratedBoxTransition(),
            alignTransition(),
            matrixTransition(),
            modalBarrier(),
            animatedModalBarrier(),
            animatedIcon(),
            fadeInImage(),
            rawImage(),
            colorFiltered(),
            fadeTransition(true),
            animatedPhysicalModel(),
            animatedFractionallySizedBox(),
            animatedCrossFade(),
            animatedSwitcher(),
            animatedTheme(),
            theme(),
            animatedPositioned(AnimatedPositionedWidgetPropertySchema.TYPE, 400),
            animatedPositioned(AnimatedPositionedWidgetPropertySchema.RECT_TYPE, 410),
            animatedPositioned(AnimatedPositionedWidgetPropertySchema.DIRECTIONAL_TYPE, 420),
            sliverLayoutBuilder(),
            sliverPersistentHeader(),
            sliverResizingHeader(),
            pinnedHeaderSliver(),
            sliverFloatingHeader(),
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
            gestureDetector(),
            listener(),
            mouseRegion(),
            focus(),
            notificationListener(),
            visibility(),
            tickerMode(),
            defaultTextHeightBehavior(),
            defaultSelectionStyle(),
            iconTheme(),
            imageIcon(),
            builder(),
            layoutBuilder(),
            orientationBuilder(),
            deviceOrientationBuilder(false),
            deviceOrientationBuilder(true),
            listenableBuilder(false),
            listenableBuilder(true),
            animatedBuilder(false),
            animatedBuilder(true),
            valueListenableBuilder(false),
            valueListenableBuilder(true),
            tweenAnimationBuilder(false),
            tweenAnimationBuilder(true),
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
            checkboxListTile(),
                switchListTile(),
                radioListTile(),
            expansionTile(),
            tooltip(),
            tooltipVisibility(),
            tooltipTheme(),
            menuItemButton(),
            menuAnchor(),
            submenuButton(),
            menuBar(),
            navigationBar(),
            navigationRail(),
            navigationDrawer(),
            drawer(),
            bottomAppBar(),
            bottomNavigationBar(),
            material(),
            scrollbar(),
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
        properties.add(namedProperty("bottomSheetScrimBuilder", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues(ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE))));
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

    static List<PropertyDefinition> appBarProperties() {
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
        List<PropertyValueConstraint> notificationPredicate = new ArrayList<>(
                stringPattern("(?:default|depthZero|all)",
                        "AppBar scroll-notification preset: default, depthZero, or all"));
        notificationPredicate.add(new PropertyValueConstraint.DartObjectReferenceValues(
                AppBarWidgetPropertySchema.NOTIFICATION_PREDICATE_TYPE));
        properties.add(namedProperty("notificationPredicate", order++, false, notificationPredicate));
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

        return List.copyOf(properties);
    }

    private static WidgetDefinition appBar() {
        var properties = appBarProperties();
        int order = properties.stream().mapToInt(p -> p.parameter().order()).max().orElseThrow() + 1;
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

    static WidgetDefinition text() {
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

    private static WidgetDefinition tooltipVisibility() {
        return widget(TooltipVisibilityWidgetPropertySchema.TOOLTIP_VISIBILITY_TYPE.value(),
                "TooltipVisibility", true, MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 310, "TooltipVisibility"),
                List.of(namedProperty("visible", 0, true, any(PropertyValueKind.BOOLEAN),
                        new PropertyValue.BooleanValue(true))),
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition tooltipTheme() {
        WidgetDefinition tooltip = tooltip();
        var properties = new ArrayList<PropertyDefinition>();
        for (var entry : TooltipThemeWidgetPropertySchema.definitions().entrySet()) {
            List<PropertyValueConstraint> constraints = entry.getKey().equals("data")
                    ? List.of(new PropertyValueConstraint.DartObjectReferenceValues("TooltipThemeData"))
                    : tooltip.property(new PropertyName(entry.getKey())).orElseThrow().constraints();
            properties.add(namedProperty(entry.getKey(), entry.getValue().dartOrder(), false, constraints));
        }
        return widget(TooltipThemeWidgetPropertySchema.TOOLTIP_THEME_TYPE.value(), "TooltipTheme", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 320, "TooltipTheme"), properties,
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition tooltip() {
        var properties = new ArrayList<PropertyDefinition>();
        WidgetDefinition badge = badge();
        for (var entry : TooltipWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (TooltipWidgetPropertySchema.isTextStyleProperty(new PropertyName(name))) {
                constraints = badge.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (name.equals("enableTapToDismiss")) {
                constraints = any(PropertyValueKind.BOOLEAN);
            } else {
                constraints = new ArrayList<>(switch (name) {
                    case "message" -> any(PropertyValueKind.STRING);
                    case "richMessage" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("InlineSpan"));
                    case "height", "verticalOffset" -> {
                        var values = new ArrayList<>(cardNumbers(null, null));
                        values.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity", "negativeInfinity", "nan")));
                        yield values;
                    }
                    case "constraints" -> List.of(new PropertyValueConstraint.BoxConstraintsValues(), new PropertyValueConstraint.DartObjectReferenceValues("BoxConstraints"));
                    case "padding", "margin" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(false), new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"));
                    case "preferBelow", "excludeFromSemantics", "enableFeedback", "ignorePointer" -> any(PropertyValueKind.BOOLEAN);
                    case "decoration" -> List.of(new PropertyValueConstraint.BoxDecorationValues(MaterialThemeTokenCatalog.colorRoles().keySet().stream().sorted().toList()), new PropertyValueConstraint.DartObjectReferenceValues("Decoration"));
                    case "textStyle" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("TextStyle"));
                    case "textAlign" -> enumValues("TextAlign", "start", "end", "left", "right", "center", "justify");
                    case "waitDurationUs", "showDurationUs", "exitDurationUs" -> {
                        var values = new ArrayList<>(portableIntegers());
                        values.add(new PropertyValueConstraint.DartObjectReferenceValues("Duration"));
                        yield values;
                    }
                    case "triggerMode" -> enumValues("TooltipTriggerMode", "manual", "longPress", "tap");
                    case "onTriggered" -> List.of(new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"), new PropertyValueConstraint.DartObjectReferenceValues("TooltipTriggeredCallback"));
                    case "mouseCursor" -> List.of(new PropertyValueConstraint.StringPattern(DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset"), new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"));
                    case "positionDelegate" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("TooltipPositionDelegate"));
                    default -> throw new IllegalArgumentException("Unreviewed Tooltip property: " + name);
                });
                if (constraints.stream().noneMatch(value -> value.kind() == PropertyValueKind.NULL)) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            }
            properties.add(name.equals("message")
                    ? new PropertyDefinition(new PropertyName(name), DartParameter.named(entry.getValue().dartOrder(), false), constraints, Optional.of(new PropertyValue.StringValue("Tooltip")))
                    : namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        return widget(TooltipWidgetPropertySchema.TOOLTIP_TYPE.value(), "Tooltip", true, MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 300, "Tooltip"), properties,
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition expansionTile() {
        var properties = new ArrayList<PropertyDefinition>();
        WidgetDefinition tile = listTile();
        WidgetDefinition shape = card();
        for (var entry : ExpansionTileWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            var shapeName = ExpansionTileWidgetPropertySchema.shapeSourceName(name);
            if (shapeName.isPresent()) {
                constraints = shape.property(new PropertyName(shapeName.orElseThrow())).orElseThrow().constraints();
            } else if (ExpansionTileWidgetPropertySchema.nonNullableBooleanProperties().contains(name)) {
                constraints = any(PropertyValueKind.BOOLEAN);
            } else if (ExpansionTileWidgetPropertySchema.animationDurationProperties().contains(name)) {
                constraints = new ArrayList<>(portableIntegers());
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Duration"));
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (ExpansionTileWidgetPropertySchema.animationCurveProperties().contains(name)) {
                constraints = new ArrayList<>(stringPattern("(?:" + String.join("|", ExpansionTileWidgetPropertySchema.curvePresets()) + ")", "reviewed Curves preset"));
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Curve"));
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else if (List.of("visualDensityHorizontal", "visualDensityVertical").contains(name)) {
                constraints = minusFourToFourDoubles();
            } else {
                constraints = new ArrayList<>(ExpansionTileWidgetPropertySchema.colorProperties().contains(name) ? colorOrTheme() : switch (name) {
                    case "onExpansionChanged" -> List.of(new PropertyValueConstraint.StringPattern("noop", "Explicit controlled no-op callback"), new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool>"));
                    case "tilePadding", "childrenPadding" -> List.of(new PropertyValueConstraint.EdgeInsetsValues(false), new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"));
                    case "expandedAlignment" -> List.of(new PropertyValueConstraint.AlignmentGeometryValues(), new PropertyValueConstraint.DartObjectReferenceValues("AlignmentGeometry"));
                    case "expandedCrossAxisAlignment" -> enumValues("CrossAxisAlignment", "start", "end", "center", "stretch");
                    case "controlAffinity" -> materialEnumValues("ListTileControlAffinity", "leading", "trailing", "platform");
                    case "clipBehavior" -> enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                    case "shape", "collapsedShape" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"));
                    case "controller" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("ExpansibleController"));
                    case "statesController" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("WidgetStatesController"));
                    case "visualDensity" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VisualDensity"));
                    case "expansionAnimationStyle" -> List.of(new PropertyValueConstraint.StringPattern("noAnimation", "reviewed AnimationStyle preset"), new PropertyValueConstraint.DartObjectReferenceValues("AnimationStyle"));
                    case "minTileHeight" -> tile.property(new PropertyName(name)).orElseThrow().constraints().stream().filter(value -> value.kind() != PropertyValueKind.NULL).toList();
                    case "dense", "enableFeedback" -> any(PropertyValueKind.BOOLEAN);
                    default -> throw new IllegalArgumentException("Unreviewed ExpansionTile property " + name);
                });
                if (ExpansionTileWidgetPropertySchema.colorProperties().contains(name)) constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            }
            properties.add(namedProperty(name, entry.getValue().dartOrder(), false, constraints));
        }
        return widget(ExpansionTileWidgetPropertySchema.EXPANSION_TILE_TYPE.value(), "ExpansionTile", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 290, "ExpansionTile"), properties,
                List.of(singleSlot("title", 0, true, 1, ANY_WIDGET), singleSlot("leading", 1, false, 0, ANY_WIDGET),
                        singleSlot("subtitle", 2, false, 0, ANY_WIDGET), singleSlot("trailing", 3, false, 0, ANY_WIDGET),
                        listSlot("children", 4, false, ANY_WIDGET)));
    }

    private static WidgetDefinition radioListTile() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition control = radio();
        WidgetDefinition tile = listTile();
        WidgetDefinition shape = card();
        for (var entry : RadioListTileWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (RadioListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                constraints = shape.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (RadioListTileWidgetPropertySchema.colorProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
            } else if (RadioListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                constraints = control.property(new PropertyName("mouseCursor")).orElseThrow().constraints();
            } else if (List.of("splashRadius", "radioScaleFactor").contains(name)) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity", "negativeInfinity", "nan")));
                if (name.equals("splashRadius")) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (name) {
                    case "onFocusChange" -> List.of(new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL), new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool>"));
                    case "variant" -> stringPattern("(?:standard|adaptive)", "RadioListTile constructor");
                    case "controlAffinity" -> materialEnumValues("ListTileControlAffinity", "leading", "trailing", "platform");
                    default -> control.property(new PropertyName(RadioListTileWidgetPropertySchema.radioSourceName(name)))
                            .or(() -> tile.property(new PropertyName(name))).orElseThrow().constraints();
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
        return widget(RadioListTileWidgetPropertySchema.RADIO_LIST_TILE_TYPE.value(), "RadioListTile", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 280, "RadioListTile"), properties,
                List.of(singleSlot("title", 0, false, 0, ANY_WIDGET), singleSlot("subtitle", 1, false, 0, ANY_WIDGET), singleSlot("secondary", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition switchListTile() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition control = switchWidget();
        WidgetDefinition tile = listTile();
        WidgetDefinition shape = card();
        for (var entry : SwitchListTileWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            if (SwitchListTileWidgetPropertySchema.builtInShapePropertyNames().contains(name)) {
                constraints = shape.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (SwitchListTileWidgetPropertySchema.colorProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
            } else if (SwitchListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                constraints = control.property(new PropertyName("mouseCursor")).orElseThrow().constraints();
            } else if (name.equals("splashRadius")) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity", "negativeInfinity", "nan")));
                constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (name) {
                    case "onChanged", "onFocusChange", "onActiveThumbImageError", "onInactiveThumbImageError" -> List.of(
                            new PropertyValueConstraint.StringPattern("noop", "Explicit controlled no-op callback"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL),
                            new PropertyValueConstraint.DartObjectReferenceValues(name.endsWith("ImageError") ? "ImageErrorListener" : "ValueChanged<bool>"));
                    case "variant" -> stringPattern("(?:standard|adaptive)", "SwitchListTile constructor");
                    case "controlAffinity" -> materialEnumValues("ListTileControlAffinity", "leading", "trailing", "platform");
                    default -> control.property(new PropertyName(name)).or(() -> tile.property(new PropertyName(name))).orElseThrow().constraints();
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.BooleanValue(false);
                case "onChanged" -> new PropertyValue.StringValue("noop");
                case "variant" -> new PropertyValue.StringValue("standard");
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(SwitchListTileWidgetPropertySchema.SWITCH_LIST_TILE_TYPE.value(), "SwitchListTile", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, GESTURES_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 270, "SwitchListTile"), properties,
                List.of(singleSlot("title", 0, false, 0, ANY_WIDGET), singleSlot("subtitle", 1, false, 0, ANY_WIDGET), singleSlot("secondary", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition checkboxListTile() {
        List<PropertyDefinition> properties = new ArrayList<>();
        WidgetDefinition checkbox = checkbox();
        WidgetDefinition tile = listTile();
        WidgetDefinition shape = card();
        for (var entry : CheckboxListTileWidgetPropertySchema.definitions().entrySet()) {
            String name = entry.getKey();
            List<PropertyValueConstraint> constraints;
            var shapeName = CheckboxListTileWidgetPropertySchema.shapeSourceName(name);
            if (shapeName.isPresent()) {
                constraints = shape.property(new PropertyName(shapeName.orElseThrow())).orElseThrow().constraints();
            } else if (CheckboxListTileWidgetPropertySchema.colorProperties().contains(name)) {
                constraints = new ArrayList<>(colorOrTheme());
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
            } else if (CheckboxListTileWidgetPropertySchema.mouseCursorStateProperties().contains(name)) {
                constraints = checkbox.property(new PropertyName("mouseCursor")).orElseThrow().constraints();
            } else if (List.of("splashRadius", "checkboxScaleFactor").contains(name)) {
                constraints = new ArrayList<>(cardNumbers(null, null));
                constraints.add(new PropertyValueConstraint.EnumValues(new DartSymbolReference("dart:core", "double"), List.of("infinity", "negativeInfinity", "nan")));
                if (name.equals("splashRadius")) constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (name) {
                    case "onChanged" -> List.of(new PropertyValueConstraint.StringPattern("noop", "Explicit controlled no-op callback"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL), new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<bool?>"));
                    case "checkboxShape" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("OutlinedBorder"));
                    case "enabled" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "controlAffinity" -> materialEnumValues("ListTileControlAffinity", "leading", "trailing", "platform");
                    case "checkboxSemanticLabel" -> any(PropertyValueKind.STRING);
                    case "variant" -> stringPattern("(?:standard|adaptive)", "CheckboxListTile constructor");
                    default -> tile.property(new PropertyName(name)).or(() -> checkbox.property(new PropertyName(name))).orElseThrow().constraints();
                };
            }
            PropertyValue creation = switch (name) {
                case "value" -> new PropertyValue.BooleanValue(false);
                case "onChanged" -> new PropertyValue.StringValue("noop");
                case "variant" -> new PropertyValue.StringValue("standard");
                default -> null;
            };
            properties.add(creation == null ? namedProperty(name, entry.getValue().dartOrder(), false, constraints)
                    : namedProperty(name, entry.getValue().dartOrder(), true, constraints, creation));
        }
        return widget(CheckboxListTileWidgetPropertySchema.CHECKBOX_LIST_TILE_TYPE.value(), "CheckboxListTile", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 260, "CheckboxListTile"), properties,
                List.of(singleSlot("title", 0, false, 0, ANY_WIDGET), singleSlot("subtitle", 1, false, 0, ANY_WIDGET),
                        singleSlot("secondary", 2, false, 0, ANY_WIDGET)));
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

    private static WidgetDefinition builder() {
        return widget(
                BuilderWidgetPropertySchema.BUILDER_TYPE.value(),
                "Builder",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(),
                palette("flutter.basic", 300, 75, "Builder"),
                List.of(namedProperty(
                        "builder",
                        0,
                        true,
                        List.of(new PropertyValueConstraint.CallbackReference()),
                        new PropertyValue.CallbackValue("noop"))),
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

    private static WidgetDefinition preferredSize() {
        return widget(
                PreferredSizeWidgetPropertySchema.PREFERRED_SIZE_TYPE.value(),
                "PreferredSize",
                true,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT),
                Set.of(PREFERRED_SIZE_WIDGET_TRAIT),
                palette("flutter.layout", 200, 215, "PreferredSize"),
                List.of(namedProperty(
                        "preferredSize",
                        0,
                        true,
                        List.of(new PropertyValueConstraint.SizeValues()),
                        new PropertyValue.SizeValue(
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(56)))),
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
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
                                "HitTestBehavior", "deferToChild", "opaque", "translucent")),
                namedProperty("itemExtentBuilder", 18, false,
                        List.of(new PropertyValueConstraint.DartObjectReferenceValues(ListViewWidgetPropertySchema.ITEM_EXTENT_BUILDER_TYPE),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
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

    private static WidgetDefinition gridViewExtent() {
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
                namedProperty("maxCrossAxisExtent", 6, true,
                        positiveDoubles(),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(200.0))),
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
                != GridViewExtentWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "GridView.extent catalog/property schema count mismatch");
        }
        return namedWidget(
                GridViewExtentWidgetPropertySchema.GRID_VIEW_EXTENT_TYPE.value(),
                "GridView",
                "extent",
                false,
                WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT),
                Set.of(),
                palette("flutter.scrolling", 250, 25, "GridView.extent"),
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

    private static WidgetDefinition pageView() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("scrollDirection", 0, false,
                        enumValues("Axis", "horizontal", "vertical")),
                namedProperty("reverse", 1, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("controller", 2, false, List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("PageController"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("physics", 3, false, stringPattern(
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)",
                        "reviewed ScrollPhysics preset")),
                namedProperty("pageSnapping", 4, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("onPageChanged", 5, false, List.of(
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("dragStartBehavior", 7, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("allowImplicitScrolling", 8, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("scrollCacheExtent", 9, false, nullableNonNegativeNumbers()),
                namedProperty("restorationId", 10, false, stringLength(1, 256)),
                namedProperty("clipBehavior", 11, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 12, false,
                        renderingEnumValues("HitTestBehavior", "deferToChild", "opaque", "translucent")),
                namedProperty("scrollBehavior", 13, false, List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ScrollBehavior"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("padEnds", 14, false, any(PropertyValueKind.BOOLEAN)));
        if (properties.size() != PageViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("PageView catalog/property schema count mismatch");
        }
        return widget(PageViewWidgetPropertySchema.PAGE_VIEW_TYPE.value(), "PageView", false,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT), Set.of(),
                palette("flutter.scrolling", 250, 40, "PageView"), properties,
                List.of(listSlot("children", 6, false, ANY_WIDGET)));
    }

    private static WidgetDefinition listWheelScrollView() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("controller", 0, false, List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ScrollController"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("physics", 1, false, stringPattern(
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)",
                        "reviewed ScrollPhysics preset")),
                namedProperty("diameterRatio", 2, false, positiveDoubles()),
                namedProperty("perspective", 3, false, List.of(
                        new PropertyValueConstraint.DoubleRange(
                                BigDecimal.ZERO, false, BigDecimal.valueOf(0.01), true))),
                namedProperty("offAxisFraction", 4, false, unboundedDoubles()),
                namedProperty("useMagnifier", 5, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("magnification", 6, false, positiveDoubles()),
                namedProperty("overAndUnderCenterOpacity", 7, false, zeroToOneDoubles()),
                namedProperty("itemExtent", 8, true, positiveNumbers(),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(50))),
                namedProperty("squeeze", 9, false, positiveDoubles()),
                namedProperty("onSelectedItemChanged", 10, false, List.of(
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("renderChildrenOutsideViewport", 11, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("clipBehavior", 12, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 13, false,
                        renderingEnumValues("HitTestBehavior", "deferToChild", "opaque", "translucent")),
                namedProperty("restorationId", 14, false, stringLength(1, 256)),
                namedProperty("scrollBehavior", 15, false, List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ScrollBehavior"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("dragStartBehavior", 16, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("changeReportingBehavior", 17, false,
                        enumValues("ChangeReportingBehavior", "onScrollEnd", "onScrollUpdate")));
        if (properties.size() != ListWheelScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("ListWheelScrollView catalog/property schema count mismatch");
        }
        return widget(ListWheelScrollViewWidgetPropertySchema.LIST_WHEEL_SCROLL_VIEW_TYPE.value(),
                "ListWheelScrollView", false, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT), Set.of(),
                palette("flutter.scrolling", 250, 50, "ListWheelScrollView"), properties,
                List.of(listSlot("children", 18, false, ANY_WIDGET)));
    }

    private static WidgetDefinition customScrollView() {
        List<PropertyDefinition> properties = List.of(
                namedProperty("scrollDirection", 0, false,
                        enumValues("Axis", "horizontal", "vertical")),
                namedProperty("reverse", 1, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("controller", 2, false, List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ScrollController"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))),
                namedProperty("primary", 3, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("physics", 4, false, stringPattern(
                        "(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)",
                        "reviewed ScrollPhysics preset")),
                namedProperty("shrinkWrap", 6, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("anchor", 8, false, zeroToOneDoubles()),
                namedProperty("scrollCacheExtent", 10, false, nonNegativeNumbers()),
                namedProperty("paintOrder", 11, false,
                        enumValues("SliverPaintOrder", "firstIsTop", "lastIsTop")),
                namedProperty("semanticChildCount", 13, false, nonNegativeIntegers()),
                namedProperty("dragStartBehavior", 14, false,
                        gesturesEnumValues("DragStartBehavior", "down", "start")),
                namedProperty("keyboardDismissBehavior", 15, false,
                        enumValues("ScrollViewKeyboardDismissBehavior", "manual", "onDrag")),
                namedProperty("restorationId", 16, false, stringLength(1, 256)),
                namedProperty("clipBehavior", 17, false,
                        enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")),
                namedProperty("hitTestBehavior", 18, false,
                        renderingEnumValues("HitTestBehavior", "deferToChild", "opaque", "translucent")));
        if (properties.size() != CustomScrollViewWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("CustomScrollView catalog/property schema count mismatch");
        }
        return widget(CustomScrollViewWidgetPropertySchema.CUSTOM_SCROLL_VIEW_TYPE.value(),
                "CustomScrollView", false, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT), Set.of(),
                palette("flutter.scrolling", 250, 60, "CustomScrollView"), properties,
                List.of(listSlot("slivers", 12, false,
                        new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverDynamic(SliverDynamicWidgetPropertySchema.Kind kind) {
        var properties = new java.util.ArrayList<PropertyDefinition>();
        int order = 0;
        for (var field : SliverDynamicWidgetPropertySchema.fields(kind)) {
            var constraints = new java.util.ArrayList<PropertyValueConstraint>();
            if (field.type().equals("bool")) constraints.addAll(any(PropertyValueKind.BOOLEAN));
            else if (field.type().startsWith("int")) {
                constraints.add(new PropertyValueConstraint.IntegerRange(BigInteger.ZERO,
                        new BigInteger(kind == SliverDynamicWidgetPropertySchema.Kind.LIST_SEPARATED
                                && field.name().equals("itemCount") ? "4503599627370496" : "9007199254740991")));
                if (field.type().endsWith("?")) constraints.addAll(any(PropertyValueKind.NULL));
            } else {
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(field.type()));
                if (field.required()) constraints.add(new PropertyValueConstraint.StringPattern(
                        String.join("|", field.presets()), "Reviewed empty/default sliver preset"));
                else constraints.addAll(any(PropertyValueKind.NULL));
            }
            properties.add(field.required()
                    ? namedProperty(field.name(), order++, true, constraints, new PropertyValue.StringValue(field.preset()))
                    : namedProperty(field.name(), order++, false, constraints));
        }
        var slots = kind.hasChildren() ? List.of(listSlot("children", order, true, ANY_WIDGET)) : List.<SlotDefinition>of();
        return new WidgetDefinition(kind.type(), kind.className(), kind.constructor(),
                kind.constructor().isEmpty(), WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, kind.order(), kind.displayName()), properties, slots);
    }

    private static WidgetDefinition sliverChildren(
            WidgetTypeId type, String className, String constructor, int order) {
        boolean list = type.equals(SliverChildrenWidgetPropertySchema.LIST);
        boolean extent = type.equals(SliverChildrenWidgetPropertySchema.GRID_EXTENT);
        List<PropertyDefinition> properties = list ? List.of(
                namedProperty("addAutomaticKeepAlives", 1, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("addRepaintBoundaries", 2, false, any(PropertyValueKind.BOOLEAN)),
                namedProperty("addSemanticIndexes", 3, false, any(PropertyValueKind.BOOLEAN))) : List.of(
                extent ? namedProperty("maxCrossAxisExtent", 0, true, positiveDoubles(),
                        new PropertyValue.DoubleValue(BigDecimal.valueOf(200)))
                       : namedProperty("crossAxisCount", 0, true, positiveIntegers(),
                        new PropertyValue.IntegerValue(BigInteger.valueOf(2))),
                namedProperty("mainAxisSpacing", 1, false, nonNegativeDoubles()),
                namedProperty("crossAxisSpacing", 2, false, nonNegativeDoubles()),
                namedProperty("childAspectRatio", 3, false, positiveDoubles()));
        return namedWidget(type.value(), className, constructor, false, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, order, className + "." + constructor),
                // Named arguments are order-independent; keep children last for Flutter's lint.
                properties, List.of(listSlot("children", 4, list, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverVisibility(boolean maintain) {
        String type = (maintain ? SliverVisibilityWidgetPropertySchema.MAINTAIN_TYPE : SliverVisibilityWidgetPropertySchema.TYPE).value();
        var palette = palette("flutter.scrolling", 250, maintain ? 380 : 370, maintain ? "SliverVisibility.maintain" : "SliverVisibility");
        var properties = SliverVisibilityWidgetPropertySchema.properties(maintain);
        var slots = List.of(singleSlot("sliver", 0, true, 1, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT)),
                singleSlot("replacementSliver", 1, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT)));
        return maintain
                ? namedWidget(type, "SliverVisibility", "maintain", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT),
                        Set.of(SLIVER_WIDGET_TRAIT), palette, properties, slots)
                : widget(type, "SliverVisibility", true, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT),
                        Set.of(SLIVER_WIDGET_TRAIT), palette, properties, slots);
    }

    private static WidgetDefinition sliverSafeArea() {
        return widget(SliverSafeAreaWidgetPropertySchema.TYPE.value(), "SliverSafeArea", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 390, "SliverSafeArea"),
                SliverSafeAreaWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 5, true, 1, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverOffstage() {
        return widget(SliverOffstageWidgetPropertySchema.TYPE.value(), "SliverOffstage", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 360, "SliverOffstage"),
                SliverOffstageWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 1, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverIgnorePointer() {
        return widget(SliverIgnorePointerWidgetPropertySchema.TYPE.value(), "SliverIgnorePointer", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 350, "SliverIgnorePointer"),
                SliverIgnorePointerWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 2, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition layoutBuilder() {
        return widget(LayoutBuilderWidgetPropertySchema.TYPE.value(), "LayoutBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 250, "LayoutBuilder"),
                LayoutBuilderWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition listenableBuilder(boolean sliver) {
        return widget((sliver ? ListenableBuilderWidgetPropertySchema.SLIVER_TYPE
                        : ListenableBuilderWidgetPropertySchema.TYPE).value(), "ListenableBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                sliver ? palette("flutter.scrolling", 250, 480, "ListenableBuilder (sliver)")
                        : palette("flutter.layout", 200, 280, "ListenableBuilder"),
                ListenableBuilderWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 2, false, 0, sliver ? new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT) : ANY_WIDGET)));
    }

    private static WidgetDefinition animatedBuilder(boolean sliver) {
        return widget((sliver ? AnimatedBuilderWidgetPropertySchema.SLIVER_TYPE
                        : AnimatedBuilderWidgetPropertySchema.TYPE).value(), "AnimatedBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                sliver ? palette("flutter.scrolling", 250, 490, "AnimatedBuilder (sliver)")
                        : palette("flutter.layout", 200, 290, "AnimatedBuilder"),
                AnimatedBuilderWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 2, false, 0, sliver ? new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT) : ANY_WIDGET)));
    }

    private static WidgetDefinition valueListenableBuilder(boolean sliver) {
        return widget((sliver ? ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE
                        : ValueListenableBuilderWidgetPropertySchema.TYPE).value(), "ValueListenableBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                sliver ? palette("flutter.scrolling", 250, 500, "ValueListenableBuilder (sliver)")
                        : palette("flutter.layout", 200, 300, "ValueListenableBuilder"),
                ValueListenableBuilderWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 2, false, 0, sliver ? new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT) : ANY_WIDGET)));
    }

    private static WidgetDefinition tweenAnimationBuilder(boolean sliver) {
        return widget((sliver ? TweenAnimationBuilderWidgetPropertySchema.SLIVER_TYPE
                        : TweenAnimationBuilderWidgetPropertySchema.TYPE).value(), "TweenAnimationBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                sliver ? palette("flutter.scrolling", 250, 510, "TweenAnimationBuilder (sliver)")
                        : palette("flutter.layout", 200, 310, "TweenAnimationBuilder"),
                TweenAnimationBuilderWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 5, false, 0, sliver ? new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT) : ANY_WIDGET)));
    }

    private static WidgetDefinition deviceOrientationBuilder(boolean sliver) {
        return widget((sliver ? DeviceOrientationBuilderWidgetPropertySchema.SLIVER_TYPE
                        : DeviceOrientationBuilderWidgetPropertySchema.TYPE).value(), "DeviceOrientationBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT),
                sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                sliver ? palette("flutter.scrolling", 250, 470, "DeviceOrientationBuilder (sliver)")
                        : palette("flutter.layout", 200, 270, "DeviceOrientationBuilder"),
                DeviceOrientationBuilderWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition orientationBuilder() {
        return widget(OrientationBuilderWidgetPropertySchema.TYPE.value(), "OrientationBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 260, "OrientationBuilder"),
                OrientationBuilderWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition flexibleSpaceBarSettings() {
        return widget(FlexibleSpaceBarSettingsWidgetPropertySchema.TYPE.value(), "FlexibleSpaceBarSettings", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 520, "FlexibleSpaceBarSettings"), FlexibleSpaceBarSettingsWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition flexibleSpaceBar() {
        return widget(FlexibleSpaceBarWidgetPropertySchema.TYPE.value(), "FlexibleSpaceBar", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 510, "FlexibleSpaceBar"), FlexibleSpaceBarWidgetPropertySchema.properties(),
                List.of(singleSlot("title", 5, false, 0, ANY_WIDGET), singleSlot("background", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverAppBar(String variant) {
        String suffix = variant.isEmpty() ? "" : "." + variant;
        var properties = SliverAppBarWidgetPropertySchema.properties();
        int order = properties.size();
        var slots = List.of(singleSlot("leading", order, false, 0, ANY_WIDGET),
                singleSlot("title", order + 1, false, 0, ANY_WIDGET), listSlot("actions", order + 2, false, ANY_WIDGET),
                singleSlot("flexibleSpace", order + 3, false, 0, ANY_WIDGET),
                singleSlot("bottom", order + 4, false, 0, new SlotAcceptance.HasTrait(PREFERRED_SIZE_WIDGET_TRAIT)));
        var entry = palette("flutter.material", 100, 500 + SliverAppBarWidgetPropertySchema.TYPES.indexOf("flutter.material.SliverAppBar" + suffix),
                "SliverAppBar" + suffix);
        return variant.isEmpty() ? widget("flutter.material.SliverAppBar", "SliverAppBar", true, MATERIAL_IMPORT,
                List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT), entry, properties, slots)
                : namedWidget("flutter.material.SliverAppBar" + suffix, "SliverAppBar", variant, true, MATERIAL_IMPORT,
                        List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT), entry, properties, slots);
    }

    private static WidgetDefinition sliverFloatingHeader() {
        return widget(SliverFloatingHeaderWidgetPropertySchema.TYPE.value(), "SliverFloatingHeader", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 450, "SliverFloatingHeader"),
                SliverFloatingHeaderWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition pinnedHeaderSliver() {
        return widget(PinnedHeaderSliverWidgetSchema.TYPE.value(), "PinnedHeaderSliver", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 440, "PinnedHeaderSliver"), List.of(),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverResizingHeader() {
        return widget(SliverResizingHeaderWidgetSchema.TYPE.value(), "SliverResizingHeader", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 430, "SliverResizingHeader"), List.of(),
                List.of(singleSlot("minExtentPrototype", 0, false, 0, ANY_WIDGET),
                        singleSlot("maxExtentPrototype", 1, false, 0, ANY_WIDGET),
                        singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverPersistentHeader() {
        return widget(SliverPersistentHeaderWidgetPropertySchema.TYPE.value(), "SliverPersistentHeader", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 420, "SliverPersistentHeader"),
                SliverPersistentHeaderWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition sliverLayoutBuilder() {
        return widget(SliverLayoutBuilderWidgetPropertySchema.TYPE.value(), "SliverLayoutBuilder", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 410, "SliverLayoutBuilder"),
                SliverLayoutBuilderWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition animatedSlide() {
        return widget(AnimatedSlideWidgetPropertySchema.TYPE.value(), "AnimatedSlide", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 350, "AnimatedSlide"),
                AnimatedSlideWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 4, false, 0, ANY_WIDGET)));
    }
    private static WidgetDefinition animatedScale() {
        return widget(AnimatedScaleWidgetPropertySchema.TYPE.value(), "AnimatedScale", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 360, "AnimatedScale"),
                AnimatedScaleWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }
    private static WidgetDefinition animatedPositioned(WidgetTypeId type, int order) {
        var properties = AnimatedPositionedWidgetPropertySchema.properties(type);
        boolean rect = AnimatedPositionedWidgetPropertySchema.RECT_TYPE.equals(type);
        String name = AnimatedPositionedWidgetPropertySchema.directional(type) ? "AnimatedPositionedDirectional" : "AnimatedPositioned";
        return new WidgetDefinition(type, name, rect ? Optional.of("fromRect") : Optional.empty(), !rect,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, order, name + (rect ? ".fromRect" : "")),
                properties, List.of(singleSlot("child", properties.size(), true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedPhysicalModel() {
        var properties=AnimatedPhysicalModelWidgetPropertySchema.properties();
        return widget(AnimatedPhysicalModelWidgetPropertySchema.TYPE.value(),"AnimatedPhysicalModel",true,
                WIDGETS_IMPORT,List.of(WIDGETS_IMPORT,MATERIAL_IMPORT),Set.of(),
                palette("flutter.layout",200,440,"AnimatedPhysicalModel"),properties,
                List.of(singleSlot("child",properties.size(),true,1,ANY_WIDGET)));
    }

    private static WidgetDefinition scaleTransition() {
        return widget(ScaleTransitionWidgetPropertySchema.TYPE.value(), "ScaleTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 510, "ScaleTransition"),
                ScaleTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition positionedTransition() {
        return widget(PositionedTransitionWidgetPropertySchema.TYPE.value(), "PositionedTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 540, "PositionedTransition"),
                PositionedTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 5, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition colorFiltered() {
        return widget(ColorFilteredWidgetPropertySchema.TYPE.value(), "ColorFiltered", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.basic", 300, 310, "ColorFiltered"), ColorFilteredWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 24, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition rawImage() {
        return widget(RawImageWidgetPropertySchema.TYPE.value(), "RawImage", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.basic", 300, 300, "RawImage"), RawImageWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition fadeInImage() {
        return widget(FadeInImageWidgetPropertySchema.TYPE.value(),"FadeInImage",true,
                WIDGETS_IMPORT,List.of(WIDGETS_IMPORT,MATERIAL_IMPORT),Set.of(),
                palette("flutter.basic",300,290,"FadeInImage"),FadeInImageWidgetPropertySchema.properties(),List.of());
    }

    private static WidgetDefinition animatedIcon() {
        return widget(AnimatedIconWidgetPropertySchema.TYPE.value(),"AnimatedIcon",true,
                MATERIAL_IMPORT,List.of(MATERIAL_IMPORT,WIDGETS_IMPORT),Set.of(),
                palette("flutter.material",100,550,"AnimatedIcon"),AnimatedIconWidgetPropertySchema.properties(),List.of());
    }

    private static WidgetDefinition animatedModalBarrier() {
        return widget(AnimatedModalBarrierWidgetPropertySchema.TYPE.value(), "AnimatedModalBarrier", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.basic", 300, 280, "AnimatedModalBarrier"),
                AnimatedModalBarrierWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition modalBarrier() {
        return widget(ModalBarrierWidgetPropertySchema.TYPE.value(), "ModalBarrier", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.basic", 300, 270, "ModalBarrier"),
                ModalBarrierWidgetPropertySchema.properties(), List.of());
    }

    private static WidgetDefinition matrixTransition() {
        return widget(MatrixTransitionWidgetPropertySchema.TYPE.value(), "MatrixTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 580, "MatrixTransition"),
                MatrixTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 4, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition alignTransition() {
        return widget(AlignTransitionWidgetPropertySchema.TYPE.value(), "AlignTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(),
                palette("flutter.layout", 200, 570, "AlignTransition"),
                AlignTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition decoratedBoxTransition() {
        return widget(DecoratedBoxTransitionWidgetPropertySchema.TYPE.value(), "DecoratedBoxTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, RENDERING_IMPORT), Set.of(),
                palette("flutter.layout", 200, 560, "DecoratedBoxTransition"),
                DecoratedBoxTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 2, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition relativePositionedTransition() {
        return widget(RelativePositionedTransitionWidgetPropertySchema.TYPE.value(), "RelativePositionedTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 550, "RelativePositionedTransition"),
                RelativePositionedTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 8, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition sizeTransition() {
        return widget(SizeTransitionWidgetPropertySchema.TYPE.value(), "SizeTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 530, "SizeTransition"),
                SizeTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 5, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition rotationTransition() {
        return widget(RotationTransitionWidgetPropertySchema.TYPE.value(), "RotationTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 520, "RotationTransition"),
                RotationTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition slideTransition() {
        return widget(SlideTransitionWidgetPropertySchema.TYPE.value(), "SlideTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 500, "SlideTransition"),
                SlideTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 3, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition fadeTransition(boolean sliver) {
        var type = sliver ? FadeTransitionWidgetPropertySchema.SLIVER_TYPE : FadeTransitionWidgetPropertySchema.TYPE;
        return widget(type.value(), sliver ? "SliverFadeTransition" : "FadeTransition", true,
                WIDGETS_IMPORT, sliver ? List.of(WIDGETS_IMPORT) : List.of(WIDGETS_IMPORT, MATERIAL_IMPORT),
                sliver ? Set.of(SLIVER_WIDGET_TRAIT) : Set.of(),
                palette(sliver ? "flutter.scrolling" : "flutter.layout", sliver ? 250 : 200, sliver ? 520 : 490,
                        sliver ? "SliverFadeTransition" : "FadeTransition"),
                FadeTransitionWidgetPropertySchema.properties(),
                List.of(singleSlot(sliver ? "sliver" : "child", 2, false, 0,
                        sliver ? new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT) : ANY_WIDGET)));
    }

    private static WidgetDefinition defaultTextStyleTransition() {
        var properties = DefaultTextStyleTransitionWidgetPropertySchema.properties();
        return widget(DefaultTextStyleTransitionWidgetPropertySchema.TYPE.value(), "DefaultTextStyleTransition", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 480, "DefaultTextStyleTransition"), properties,
                List.of(singleSlot("child", properties.size(), true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition defaultTextStyle(boolean merge) {
        var type = merge ? DefaultTextStyleWidgetPropertySchema.MERGE_TYPE : DefaultTextStyleWidgetPropertySchema.TYPE;
        var properties = DefaultTextStyleWidgetPropertySchema.properties(type);
        return widget(type.value(), "DefaultTextStyle", !merge,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.basic", 300, merge ? 250 : 240, merge ? "DefaultTextStyle.merge" : "DefaultTextStyle"), properties,
                List.of(singleSlot("child", properties.size(), true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedDefaultTextStyle() {
        var properties = AnimatedDefaultTextStyleWidgetPropertySchema.properties();
        return widget(AnimatedDefaultTextStyleWidgetPropertySchema.TYPE.value(), "AnimatedDefaultTextStyle", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 430, "AnimatedDefaultTextStyle"), properties,
                List.of(singleSlot("child", properties.size(), true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedSize() {
        return widget(AnimatedSizeWidgetPropertySchema.TYPE.value(), "AnimatedSize", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 390, "AnimatedSize"),
                AnimatedSizeWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedContainer() {
        return widget(AnimatedContainerWidgetPropertySchema.TYPE.value(), "AnimatedContainer", false,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT, "dart:core"), Set.of(),
                palette("flutter.layout", 200, 380, "AnimatedContainer"),
                AnimatedContainerWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 15, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedRotation() {
        return widget(AnimatedRotationWidgetPropertySchema.TYPE.value(), "AnimatedRotation", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 370, "AnimatedRotation"),
                AnimatedRotationWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedPadding() {
        return widget(AnimatedPaddingWidgetPropertySchema.TYPE.value(), "AnimatedPadding", false,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 340, "AnimatedPadding"),
                AnimatedPaddingWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 4, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedAlign() {
        return widget(AnimatedAlignWidgetPropertySchema.TYPE.value(), "AnimatedAlign", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 330, "AnimatedAlign"),
                AnimatedAlignWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedFractionallySizedBox() {
        return widget(AnimatedFractionallySizedBoxWidgetPropertySchema.TYPE.value(), "AnimatedFractionallySizedBox", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 450, "AnimatedFractionallySizedBox"),
                AnimatedFractionallySizedBoxWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition theme() {
        return widget(ThemeWidgetPropertySchema.TYPE.value(), "Theme", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 540, "Theme"),
                ThemeWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 1, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedTheme() {
        return widget(AnimatedThemeWidgetPropertySchema.TYPE.value(), "AnimatedTheme", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT), Set.of(),
                palette("flutter.material", 100, 530, "AnimatedTheme"),
                AnimatedThemeWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 4, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedSwitcher() {
        return widget(AnimatedSwitcherWidgetPropertySchema.TYPE.value(), "AnimatedSwitcher", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 470, "AnimatedSwitcher"),
                AnimatedSwitcherWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedCrossFade() {
        return widget(AnimatedCrossFadeWidgetPropertySchema.TYPE.value(), "AnimatedCrossFade", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 460, "AnimatedCrossFade"),
                AnimatedCrossFadeWidgetPropertySchema.properties(),
                List.of(singleSlot("firstChild", 10, true, 1, ANY_WIDGET), singleSlot("secondChild", 11, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition animatedOpacity() {
        return widget(AnimatedOpacityWidgetPropertySchema.TYPE.value(), "AnimatedOpacity", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, MATERIAL_IMPORT), Set.of(),
                palette("flutter.layout", 200, 320, "AnimatedOpacity"),
                AnimatedOpacityWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 5, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverAnimatedOpacity() {
        return widget(SliverAnimatedOpacityWidgetPropertySchema.TYPE.value(), "SliverAnimatedOpacity", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 400, "SliverAnimatedOpacity"),
                SliverAnimatedOpacityWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 5, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverOpacity() {
        return widget(SliverOpacityWidgetPropertySchema.TYPE.value(), "SliverOpacity", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 340, "SliverOpacity"),
                SliverOpacityWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 2, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverPadding() {
        return widget(SliverPaddingWidgetPropertySchema.TYPE.value(), "SliverPadding", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 170, "SliverPadding"),
                List.of(SliverPaddingWidgetPropertySchema.padding()),
                List.of(singleSlot("sliver", 1, false, 0, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverPrototypeExtentList(SliverPrototypeExtentListWidgetPropertySchema.Kind kind) {
        var properties = new ArrayList<PropertyDefinition>();
        int order = 0;
        for (var field : SliverPrototypeExtentListWidgetPropertySchema.fields(kind)) {
            var constraints = new ArrayList<PropertyValueConstraint>();
            if (field.type().equals("double")) constraints.addAll(nonNegativeDoubles());
            else if (field.type().equals("bool")) constraints.addAll(any(PropertyValueKind.BOOLEAN));
            else if (field.type().startsWith("int")) {
                constraints.addAll(nonNegativeIntegers());
                if (field.type().endsWith("?")) constraints.addAll(any(PropertyValueKind.NULL));
            } else {
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(field.type()));
                if (field.required()) constraints.add(new PropertyValueConstraint.StringPattern("empty", "Reviewed empty sliver preset"));
                else constraints.addAll(any(PropertyValueKind.NULL));
            }
            properties.add(field.required()
                    ? namedProperty(field.name(), order++, true, constraints, field.type().equals("double")
                            ? new PropertyValue.DoubleValue(new BigDecimal(field.preset()))
                            : new PropertyValue.StringValue(field.preset()))
                    : namedProperty(field.name(), order++, false, constraints));
        }
        return new WidgetDefinition(kind.type(), "SliverPrototypeExtentList", kind.constructor(),
                kind == SliverPrototypeExtentListWidgetPropertySchema.Kind.DELEGATE,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, kind.order(), kind.displayName()),
                properties, kind.hasChildren()
                        ? List.of(singleSlot("prototypeItem", order++, true, 0, ANY_WIDGET),
                                listSlot("children", order, true, ANY_WIDGET))
                        : List.of(singleSlot("prototypeItem", order, true, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverFixedExtentList(SliverFixedExtentListWidgetPropertySchema.Kind kind) {
        var properties = new ArrayList<PropertyDefinition>();
        int order = 0;
        for (var field : SliverFixedExtentListWidgetPropertySchema.fields(kind)) {
            var constraints = new ArrayList<PropertyValueConstraint>();
            if (field.type().equals("double")) constraints.addAll(nonNegativeDoubles());
            else if (field.type().equals("bool")) constraints.addAll(any(PropertyValueKind.BOOLEAN));
            else if (field.type().startsWith("int")) {
                constraints.addAll(nonNegativeIntegers());
                if (field.type().endsWith("?")) constraints.addAll(any(PropertyValueKind.NULL));
            } else {
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(field.type()));
                if (field.required()) constraints.add(new PropertyValueConstraint.StringPattern("empty", "Reviewed empty sliver preset"));
                else constraints.addAll(any(PropertyValueKind.NULL));
            }
            properties.add(field.required()
                    ? namedProperty(field.name(), order++, true, constraints, field.type().equals("double")
                            ? new PropertyValue.DoubleValue(new BigDecimal(field.preset()))
                            : new PropertyValue.StringValue(field.preset()))
                    : namedProperty(field.name(), order++, false, constraints));
        }
        return new WidgetDefinition(kind.type(), "SliverFixedExtentList", kind.constructor(),
                kind == SliverFixedExtentListWidgetPropertySchema.Kind.DELEGATE,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, kind.order(), kind.displayName()),
                properties, kind.hasChildren() ? List.of(listSlot("children", order, true, ANY_WIDGET)) : List.of());
    }

    private static WidgetDefinition sliverVariedExtentList(SliverVariedExtentListWidgetPropertySchema.Kind kind) {
        var properties = new ArrayList<PropertyDefinition>();
        int order = 0;
        for (var field : SliverVariedExtentListWidgetPropertySchema.fields(kind)) {
            var constraints = new ArrayList<PropertyValueConstraint>();
            if (field.type().equals("double")) constraints.addAll(nonNegativeDoubles());
            else if (field.type().equals("bool")) constraints.addAll(any(PropertyValueKind.BOOLEAN));
            else if (field.type().startsWith("int")) {
                constraints.addAll(nonNegativeIntegers());
                if (field.type().endsWith("?")) constraints.addAll(any(PropertyValueKind.NULL));
            } else {
                constraints.add(new PropertyValueConstraint.DartObjectReferenceValues(field.type()));
                if (field.required()) constraints.add(new PropertyValueConstraint.StringPattern(String.join("|", field.presets()), "Reviewed sliver preset"));
                else constraints.addAll(any(PropertyValueKind.NULL));
            }
            properties.add(field.required()
                    ? namedProperty(field.name(), order++, true, constraints, field.type().equals("double")
                            ? new PropertyValue.DoubleValue(new BigDecimal(field.preset()))
                            : new PropertyValue.StringValue(field.preset()))
                    : namedProperty(field.name(), order++, false, constraints));
        }
        return new WidgetDefinition(kind.type(), "SliverVariedExtentList", kind.constructor(),
                kind == SliverVariedExtentListWidgetPropertySchema.Kind.DELEGATE,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, kind.order(), kind.displayName()),
                properties, kind.hasChildren() ? List.of(listSlot("children", order, true, ANY_WIDGET)) : List.of());
    }

    private static WidgetDefinition sliverFillViewport(boolean delegate) {
        var type = delegate ? SliverFillViewportWidgetPropertySchema.DELEGATE : SliverFillViewportWidgetPropertySchema.CHILDREN;
        var properties = new java.util.ArrayList<PropertyDefinition>();
        int order = 0;
        for (var field : SliverFillViewportWidgetPropertySchema.fields(type)) {
            String name = field.name();
            if (name.equals("delegate")) properties.add(namedProperty(name, order++, true,
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues("SliverChildDelegate"),
                            new PropertyValueConstraint.StringPattern("empty", "Empty child delegate")),
                    new PropertyValue.StringValue("empty")));
            else properties.add(namedProperty(name, order++, false, switch (name) {
                case "viewportFraction" -> positiveDoubles();
                case "semanticIndexOffset" -> nonNegativeIntegers();
                case "semanticIndexCallback" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("SemanticIndexCallback"));
                default -> any(PropertyValueKind.BOOLEAN);
            }));
        }
        return widget(type.value(), "SliverFillViewport", delegate, WIDGETS_IMPORT, List.of(WIDGETS_IMPORT),
                Set.of(SLIVER_WIDGET_TRAIT), palette("flutter.scrolling", 250, delegate ? 200 : 190,
                        delegate ? "SliverFillViewport.delegate" : "SliverFillViewport"),
                properties, delegate ? List.of() : List.of(listSlot("children", order, true, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverFillRemaining() {
        return widget(SliverFillRemainingWidgetPropertySchema.TYPE.value(), "SliverFillRemaining", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 180, "SliverFillRemaining"),
                SliverFillRemainingWidgetPropertySchema.properties(),
                List.of(singleSlot("child", 2, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition sliverMainAxisGroup() {
        return widget(SliverMainAxisGroupWidgetPropertySchema.TYPE.value(), "SliverMainAxisGroup", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 300, "SliverMainAxisGroup"), List.of(),
                List.of(listSlot("slivers", 0, true, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverConstrainedCrossAxis() {
        return widget(SliverConstrainedCrossAxisWidgetPropertySchema.TYPE.value(), "SliverConstrainedCrossAxis", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT, "dart:core"), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 330, "SliverConstrainedCrossAxis"),
                SliverConstrainedCrossAxisWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 1, true, 1, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverCrossAxisExpanded() {
        return widget(SliverCrossAxisExpandedWidgetPropertySchema.TYPE.value(), "SliverCrossAxisExpanded", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 320, "SliverCrossAxisExpanded"),
                SliverCrossAxisExpandedWidgetPropertySchema.properties(),
                List.of(singleSlot("sliver", 1, true, 1, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverCrossAxisGroup() {
        return widget(SliverCrossAxisGroupWidgetPropertySchema.TYPE.value(), "SliverCrossAxisGroup", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 310, "SliverCrossAxisGroup"), List.of(),
                List.of(listSlot("slivers", 0, true, new SlotAcceptance.HasTrait(SLIVER_WIDGET_TRAIT))));
    }

    private static WidgetDefinition sliverToBoxAdapter() {
        return widget(SliverToBoxAdapterWidgetPropertySchema.SLIVER_TO_BOX_ADAPTER_TYPE.value(),
                "SliverToBoxAdapter", true, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT), Set.of(SLIVER_WIDGET_TRAIT),
                palette("flutter.scrolling", 250, 70, "SliverToBoxAdapter"), List.of(),
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
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

    private static WidgetDefinition gestureDetector() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (GestureDetectorWidgetPropertySchema.Definition definition
                : GestureDetectorWidgetPropertySchema.definitions().values()) {
            List<PropertyValueConstraint> constraints;
            if (definition.callbackType().isPresent()) {
                constraints = List.of(new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues(definition.callbackType().orElseThrow()),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (definition.dartName()) {
                    case "behavior" -> List.of(new PropertyValueConstraint.EnumValues(
                            new DartSymbolReference(RENDERING_IMPORT, "HitTestBehavior"),
                            List.of("deferToChild", "opaque", "translucent")),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "excludeFromSemantics", "trackpadScrollCausesScale" -> any(PropertyValueKind.BOOLEAN);
                    case "dragStartBehavior" -> gesturesEnumValues("DragStartBehavior", "down", "start");
                    case "trackpadScrollToScaleFactor" -> List.of(new PropertyValueConstraint.OffsetValues());
                    case "supportedDevices" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.POINTER_DEVICE_KIND_SET),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    default -> throw new IllegalStateException("Unknown GestureDetector configuration: " + definition.dartName());
                };
            }
            properties.add(namedProperty(definition.dartName(), definition.dartOrder(), false, constraints));
        }
        return widget(GestureDetectorWidgetPropertySchema.GESTURE_DETECTOR_TYPE.value(),
                "GestureDetector", false, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT, DART_UI_IMPORT), Set.of(),
                palette("flutter.interaction", 500, 10, "GestureDetector"), properties,
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition listener() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (ListenerWidgetPropertySchema.Definition definition
                : ListenerWidgetPropertySchema.definitions().values()) {
            List<PropertyValueConstraint> constraints = definition.callbackType().isPresent()
                    ? List.of(new PropertyValueConstraint.CallbackReference(),
                            new PropertyValueConstraint.DartObjectReferenceValues(definition.callbackType().orElseThrow()),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))
                    : List.of(new PropertyValueConstraint.EnumValues(
                            new DartSymbolReference(RENDERING_IMPORT, "HitTestBehavior"),
                            List.of("deferToChild", "opaque", "translucent")));
            properties.add(namedProperty(definition.dartName(), definition.dartOrder(), false, constraints));
        }
        return widget(ListenerWidgetPropertySchema.LISTENER_TYPE.value(), "Listener", true, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT, SERVICES_IMPORT), Set.of(),
                palette("flutter.interaction", 500, 20, "Listener"), properties,
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition mouseRegion() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (MouseRegionWidgetPropertySchema.Definition definition
                : MouseRegionWidgetPropertySchema.definitions().values()) {
            List<PropertyValueConstraint> constraints;
            if (definition.callbackType().isPresent()) {
                constraints = List.of(new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues(definition.callbackType().orElseThrow()),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (definition.dartName()) {
                    case "cursor" -> List.of(new PropertyValueConstraint.StringPattern(
                            DefaultSelectionStyleWidgetPropertySchema.mouseCursorPattern(), "reviewed MouseCursor preset"),
                            new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"));
                    case "opaque" -> any(PropertyValueKind.BOOLEAN);
                    case "hitTestBehavior" -> List.of(new PropertyValueConstraint.EnumValues(
                            new DartSymbolReference(RENDERING_IMPORT, "HitTestBehavior"),
                            List.of("deferToChild", "opaque", "translucent")),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    default -> throw new IllegalStateException("Unknown MouseRegion configuration: " + definition.dartName());
                };
            }
            properties.add(namedProperty(definition.dartName(), definition.dartOrder(), false, constraints));
        }
        return widget(MouseRegionWidgetPropertySchema.MOUSE_REGION_TYPE.value(), "MouseRegion", true, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, GESTURES_IMPORT, RENDERING_IMPORT, SERVICES_IMPORT), Set.of(),
                palette("flutter.interaction", 500, 30, "MouseRegion"), properties,
                List.of(singleSlot("child", 0, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition focus() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (var definition : FocusWidgetPropertySchema.definitions().values()) {
            List<PropertyValueConstraint> constraints;
            if (definition.callbackType().isPresent()) {
                constraints = List.of(new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues(definition.callbackType().orElseThrow()),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
            } else {
                constraints = switch (definition.dartName()) {
                    case "focusNode", "parentNode" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("FocusNode?"),
                            new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "autofocus", "includeSemantics" -> any(PropertyValueKind.BOOLEAN);
                    case "canRequestFocus", "skipTraversal", "descendantsAreFocusable", "descendantsAreTraversable" ->
                            List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "debugLabel" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "variant" -> stringPattern("(?:standard|withExternalFocusNode)", "Focus constructor");
                    default -> throw new IllegalStateException("Unknown Focus property: " + definition.dartName());
                };
            }
            properties.add(definition.dartName().equals("variant")
                    ? namedProperty("variant", definition.dartOrder(), false, constraints, new PropertyValue.StringValue("standard"))
                    : namedProperty(definition.dartName(), definition.dartOrder(), false, constraints));
        }
        return widget(FocusWidgetPropertySchema.FOCUS_TYPE.value(), "Focus", true, WIDGETS_IMPORT,
                List.of(WIDGETS_IMPORT, SERVICES_IMPORT), Set.of(),
                palette("flutter.interaction", 500, 40, "Focus"), properties,
                List.of(singleSlot("child", 0, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition notificationListener() {
        return widget(NotificationListenerWidgetPropertySchema.NOTIFICATION_LISTENER_TYPE.value(), "NotificationListener", true,
                WIDGETS_IMPORT, List.of(WIDGETS_IMPORT), Set.of(), palette("flutter.interaction", 500, 50, "NotificationListener"),
                List.of(namedProperty("notificationType", 1, true,
                        List.of(new PropertyValueConstraint.StringPattern("(?:" + String.join("|", NotificationListenerWidgetPropertySchema.typePresets()) + ")", "Notification subtype preset"),
                                new PropertyValueConstraint.DartObjectReferenceValues("Type")), new PropertyValue.StringValue("Notification")),
                        namedProperty("onNotification", 2, false, List.of(new PropertyValueConstraint.CallbackReference(),
                                new PropertyValueConstraint.DartObjectReferenceValues(NotificationListenerWidgetPropertySchema.CALLBACK_TYPE),
                                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL)))),
                List.of(singleSlot("child", 0, true, 1, ANY_WIDGET)));
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
        properties.add(namedProperty("buildCounter", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues(TextFieldWidgetPropertySchema.INPUT_COUNTER_BUILDER_TYPE),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        properties.add(namedProperty("contextMenuBuilder", order++, false,
                List.of(new PropertyValueConstraint.DartObjectReferenceValues(TextFieldWidgetPropertySchema.CONTEXT_MENU_BUILDER_TYPE),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));

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

    private static WidgetDefinition submenuButton() {
        WidgetDefinition button = textButton();
        WidgetDefinition menu = menuAnchor();
        WidgetDefinition tooltip = tooltip();
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : SubmenuButtonWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            if (SubmenuButtonWidgetPropertySchema.localStyleProperties().contains(name)) {
                constraints = button.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (SubmenuButtonWidgetPropertySchema.menuStyleProperties().contains(name)) {
                constraints = menu.property(new PropertyName(SubmenuButtonWidgetPropertySchema.menuStyleSourceName(name))).orElseThrow().constraints();
            } else if (SubmenuButtonWidgetPropertySchema.submenuIconLocalProperties().contains(name)) {
                var values = new ArrayList<PropertyValueConstraint>(List.of(new PropertyValueConstraint.MaterialIconValues()));
                values.add(new PropertyValueConstraint.DartObjectReferenceValues("Widget"));
                values.addAll(any(PropertyValueKind.NULL)); constraints = List.copyOf(values);
            } else if (name.equals("hoverOpenDelayUs")) {
                constraints = tooltip.property(new PropertyName("waitDurationUs")).orElseThrow().constraints().stream()
                        .filter(value -> value.kind() != PropertyValueKind.NULL).toList();
            } else if (name.equals("useRootOverlay") || name.equals("animated")) constraints = any(PropertyValueKind.BOOLEAN);
            else if (name.equals("clipBehavior")) constraints = enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
            else {
                String type = switch (name) {
                    case "onHover", "onFocusChange" -> "ValueChanged<bool>";
                    case "onOpen", "onClose" -> "VoidCallback";
                    case "controller" -> "MenuController"; case "style" -> "ButtonStyle"; case "menuStyle" -> "MenuStyle";
                    case "alignmentOffset" -> "Offset"; case "focusNode" -> "FocusNode"; case "statesController" -> "WidgetStatesController";
                    case "submenuIcon" -> "WidgetStateProperty<Widget?>";
                    case "onAnimationStatusChanged" -> "ValueChanged<AnimationStatus>";
                    default -> throw new IllegalStateException(name);
                };
                var values = new ArrayList<PropertyValueConstraint>();
                if (name.equals("alignmentOffset")) values.add(new PropertyValueConstraint.OffsetValues());
                values.add(new PropertyValueConstraint.DartObjectReferenceValues(type)); values.addAll(any(PropertyValueKind.NULL)); constraints = List.copyOf(values);
            }
            properties.add(namedProperty(name, properties.size(), false, constraints));
        }
        return widget(SubmenuButtonWidgetPropertySchema.SUBMENU_BUTTON_TYPE.value(), "SubmenuButton", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 350, "SubmenuButton"), properties,
                List.of(singleSlot("child", 721, true, 0, ANY_WIDGET), singleSlot("leadingIcon", 722, false, 0, ANY_WIDGET),
                        singleSlot("trailingIcon", 723, false, 0, ANY_WIDGET), listSlot("menuChildren", 724, true, ANY_WIDGET)));
    }

    private static WidgetDefinition menuBar() {
        WidgetDefinition menu = menuAnchor();
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : MenuBarWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            if (MenuBarWidgetPropertySchema.localStyleProperties().contains(name)) {
                constraints = menu.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (name.equals("clipBehavior")) {
                constraints = enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
            } else if (name.equals("controller")) {
                var values = new ArrayList<PropertyValueConstraint>();
                values.add(new PropertyValueConstraint.DartObjectReferenceValues("MenuController"));
                values.addAll(any(PropertyValueKind.NULL));
                constraints = List.copyOf(values);
            } else if (name.equals("style")) {
                var values = new ArrayList<PropertyValueConstraint>();
                values.add(new PropertyValueConstraint.DartObjectReferenceValues("MenuStyle"));
                values.addAll(any(PropertyValueKind.NULL));
                constraints = List.copyOf(values);
            } else {
                throw new IllegalStateException("Unexpected MenuBar property: " + name);
            }
            properties.add(namedProperty(name, properties.size(), false, constraints));
        }
        return widget(MenuBarWidgetPropertySchema.MENU_BAR_TYPE.value(), "MenuBar", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 360, "MenuBar"), properties,
                List.of(listSlot("children", MenuBarWidgetPropertySchema.FLATTENED_PROPERTY_COUNT,
                        true, ANY_WIDGET)));
    }

    private static WidgetDefinition navigationBar() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : NavigationBarWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "animationDurationUs" -> {
                    constraints = new ArrayList<>(portableIntegers());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Duration"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "selectedIndex" -> constraints = cardNumbers(BigDecimal.ZERO, null).stream()
                        .filter(value -> value.kind() == PropertyValueKind.INTEGER).toList();
                case "onDestinationSelected" -> constraints = List.of(
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "backgroundColor", "shadowColor", "surfaceTintColor", "indicatorColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "elevation", "height" -> {
                    constraints = new ArrayList<>(cardNumbers(null, null));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "indicatorShape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "labelBehavior" -> {
                    constraints = new ArrayList<>(materialEnumValues(
                        "NavigationDestinationLabelBehavior", "alwaysShow", "onlyShowSelected", "alwaysHide"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "overlayColor" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<Color?>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "labelTextStyle" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("WidgetStateProperty<TextStyle?>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "labelPadding" -> constraints = List.of(
                        new PropertyValueConstraint.EdgeInsetsValues(true),
                        new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "maintainBottomViewPadding" -> constraints = any(PropertyValueKind.BOOLEAN);
                default -> throw new IllegalStateException("Unexpected NavigationBar property: " + name);
            }
            PropertyValue creation = name.equals("selectedIndex")
                    ? new PropertyValue.IntegerValue(BigInteger.ZERO) : null;
            properties.add(creation == null
                    ? namedProperty(name, properties.size(), name.equals("selectedIndex"), constraints)
                    : namedProperty(name, properties.size(), true, constraints, creation));
        }
        return widget(NavigationBarWidgetPropertySchema.NAVIGATION_BAR_TYPE.value(), "NavigationBar", false,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, "dart:core"), Set.of(),
                palette("flutter.material", 100, 370, "NavigationBar"), properties,
                List.of(listSlot("destinations", NavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                        true, ANY_WIDGET)));
    }

    private static WidgetDefinition navigationRail() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : NavigationRailWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "backgroundColor", "indicatorColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "extended", "leadingAtTop", "trailingAtBottom", "scrollable" ->
                        constraints = any(PropertyValueKind.BOOLEAN);
                case "selectedIndex" -> {
                    constraints = new ArrayList<>(nonNegativeIntegers());
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "onDestinationSelected" -> constraints = List.of(
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "elevation", "minWidth", "minExtendedWidth" -> {
                    constraints = new ArrayList<>(positiveIntegers());
                    constraints.addAll(positiveDoubles());
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "groupAlignment" -> {
                    constraints = new ArrayList<>(cardNumbers(BigDecimal.ONE.negate(), BigDecimal.ONE));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "labelType" -> {
                    constraints = new ArrayList<>(materialEnumValues(
                            "NavigationRailLabelType", "none", "selected", "all"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "unselectedLabelTextStyle", "selectedLabelTextStyle" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("TextStyle"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "unselectedIconTheme", "selectedIconTheme" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("IconThemeData"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "useIndicator" -> {
                    constraints = new ArrayList<>(any(PropertyValueKind.BOOLEAN));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "indicatorShape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "mainAxisAlignment" -> {
                    constraints = new ArrayList<>(enumValues("MainAxisAlignment", "start", "end", "center",
                            "spaceBetween", "spaceAround", "spaceEvenly"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                default -> throw new IllegalStateException("Unexpected NavigationRail property: " + name);
            }
            PropertyValue creation = name.equals("selectedIndex")
                    ? new PropertyValue.IntegerValue(BigInteger.ZERO) : null;
            properties.add(creation == null
                    ? namedProperty(name,
                            NavigationRailWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false,
                            constraints)
                    : namedProperty(name,
                            NavigationRailWidgetPropertySchema.find(name).orElseThrow().dartOrder(), true,
                            constraints, creation));
        }
        return widget(NavigationRailWidgetPropertySchema.NAVIGATION_RAIL_TYPE.value(), "NavigationRail", false,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 380, "NavigationRail"), properties,
                List.of(singleSlot("leading", 2, false, 0, ANY_WIDGET),
                        singleSlot("trailing", 3, false, 0, ANY_WIDGET),
                        listSlot("destinations", 4, true, ANY_WIDGET)));
    }

    private static WidgetDefinition navigationDrawer() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : NavigationDrawerWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "backgroundColor", "shadowColor", "surfaceTintColor", "indicatorColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "elevation" -> {
                    constraints = new ArrayList<>(cardNumbers(null, null));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "indicatorShape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "onDestinationSelected" -> constraints = List.of(
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "selectedIndex" -> {
                    constraints = new ArrayList<>(nonNegativeIntegers());
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "tilePadding" -> constraints = List.of(
                        new PropertyValueConstraint.EdgeInsetsValues(true),
                        new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                default -> throw new IllegalStateException("Unexpected NavigationDrawer property: " + name);
            }
            PropertyValue creation = name.equals("selectedIndex")
                    ? new PropertyValue.IntegerValue(BigInteger.ZERO) : null;
            properties.add(creation == null
                    ? namedProperty(name,
                            NavigationDrawerWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false,
                            constraints)
                    : namedProperty(name,
                            NavigationDrawerWidgetPropertySchema.find(name).orElseThrow().dartOrder(), true,
                            constraints, creation));
        }
        return widget(NavigationDrawerWidgetPropertySchema.NAVIGATION_DRAWER_TYPE.value(), "NavigationDrawer", false,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 390, "NavigationDrawer"), properties,
                List.of(singleSlot("header", 1, false, 0, ANY_WIDGET),
                        singleSlot("footer", 2, false, 0, ANY_WIDGET),
                        listSlot("children", 0, true, ANY_WIDGET)));
    }

    private static WidgetDefinition drawer() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : DrawerWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "backgroundColor", "shadowColor", "surfaceTintColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "elevation", "width" -> {
                    constraints = new ArrayList<>(cardNumbers(BigDecimal.ZERO, null));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "shape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "semanticLabel" -> {
                    constraints = new ArrayList<>(any(PropertyValueKind.STRING));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "clipBehavior" -> {
                    constraints = new ArrayList<>(enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                default -> throw new IllegalStateException("Unexpected Drawer property: " + name);
            }
            properties.add(namedProperty(name,
                    DrawerWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false,
                    constraints));
        }
        if (properties.size() != DrawerWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Drawer catalog/property schema count mismatch");
        }
        return widget(DrawerWidgetPropertySchema.DRAWER_TYPE.value(), "Drawer", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 400, "Drawer"), properties,
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition bottomAppBar() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : BottomAppBarWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "color", "shadowColor", "surfaceTintColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "elevation", "height", "notchMargin" -> {
                    constraints = new ArrayList<>(cardNumbers(BigDecimal.ZERO, null));
                    if (name.equals("elevation") || name.equals("height")) {
                        constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    }
                }
                case "shape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("NotchedShape"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "clipBehavior" -> constraints = enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                case "padding" -> constraints = List.of(
                        new PropertyValueConstraint.EdgeInsetsValues(false),
                        new PropertyValueConstraint.DartObjectReferenceValues("EdgeInsetsGeometry"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                default -> throw new IllegalStateException("Unexpected BottomAppBar property: " + name);
            }
            properties.add(namedProperty(name,
                    BottomAppBarWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false,
                    constraints));
        }
        if (properties.size() != BottomAppBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("BottomAppBar catalog/property schema count mismatch");
        }
        return widget(BottomAppBarWidgetPropertySchema.BOTTOM_APP_BAR_TYPE.value(), "BottomAppBar", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 410, "BottomAppBar"), properties,
                List.of(singleSlot("child", 6, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition bottomNavigationBar() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : BottomNavigationBarWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "onTap" -> constraints = List.of(
                        new PropertyValueConstraint.StringPattern("noop", "Explicit no-op callback"),
                        new PropertyValueConstraint.CallbackReference(),
                        new PropertyValueConstraint.DartObjectReferenceValues("ValueChanged<int>"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "currentIndex" -> constraints = nonNegativeIntegers();
                case "elevation", "iconSize", "selectedFontSize", "unselectedFontSize" -> {
                    constraints = new ArrayList<>(cardNumbers(BigDecimal.ZERO, null));
                    if (!name.equals("iconSize") && !name.equals("selectedFontSize") && !name.equals("unselectedFontSize")) {
                        constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    }
                }
                case "barType" -> {
                    constraints = new ArrayList<>(materialEnumValues(
                            "BottomNavigationBarType", "fixed", "shifting"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "backgroundColor", "selectedItemColor", "unselectedItemColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "selectedIconTheme", "unselectedIconTheme" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("IconThemeData"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "selectedLabelStyle", "unselectedLabelStyle" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("TextStyle"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "showSelectedLabels", "showUnselectedLabels", "enableFeedback" -> {
                    constraints = new ArrayList<>(any(PropertyValueKind.BOOLEAN));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "mouseCursor" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("MouseCursor"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "landscapeLayout" -> {
                    constraints = new ArrayList<>(materialEnumValues(
                            "BottomNavigationBarLandscapeLayout", "spread", "centered", "linear"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "useLegacyColorScheme" -> constraints = any(PropertyValueKind.BOOLEAN);
                default -> throw new IllegalStateException("Unexpected BottomNavigationBar property: " + name);
            }
            PropertyValue creation = name.equals("currentIndex")
                    ? new PropertyValue.IntegerValue(BigInteger.ZERO) : null;
            properties.add(creation == null
                    ? namedProperty(name, BottomNavigationBarWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false, constraints)
                    : namedProperty(name, BottomNavigationBarWidgetPropertySchema.find(name).orElseThrow().dartOrder(), true, constraints, creation));
        }
        if (properties.size() != BottomNavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("BottomNavigationBar catalog/property schema count mismatch");
        }
        return widget(BottomNavigationBarWidgetPropertySchema.BOTTOM_NAVIGATION_BAR_TYPE.value(), "BottomNavigationBar", false,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 420, "BottomNavigationBar"), properties,
                List.of(listSlot("items", BottomNavigationBarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT, true, ANY_WIDGET)));
    }

    private static WidgetDefinition material() {
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : MaterialWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            switch (name) {
                case "materialType" -> constraints = materialEnumValues(
                        "MaterialType", "canvas", "card", "circle", "button", "transparency");
                case "elevation" -> constraints = nonNegativeNumbers();
                case "color", "shadowColor", "surfaceTintColor" -> {
                    constraints = new ArrayList<>(colorOrTheme());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Color"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                case "textStyle" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("TextStyle"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "borderRadius" -> constraints = List.of(
                        new PropertyValueConstraint.BorderRadiusValues(),
                        new PropertyValueConstraint.DartObjectReferenceValues("BorderRadiusGeometry"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "shape" -> constraints = List.of(
                        new PropertyValueConstraint.DartObjectReferenceValues("ShapeBorder"),
                        new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                case "borderOnForeground", "animateColor" -> constraints = any(PropertyValueKind.BOOLEAN);
                case "clipBehavior" -> constraints = enumValues(
                        "Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                case "animationDurationUs" -> {
                    constraints = new ArrayList<>(portableIntegers());
                    constraints.add(new PropertyValueConstraint.DartObjectReferenceValues("Duration"));
                    constraints.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                }
                default -> throw new IllegalStateException("Unexpected Material property: " + name);
            }
            properties.add(namedProperty(name,
                    MaterialWidgetPropertySchema.find(name).orElseThrow().dartOrder(), false,
                    constraints));
        }
        if (properties.size() != MaterialWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Material catalog/property schema count mismatch");
        }
        return widget(MaterialWidgetPropertySchema.MATERIAL_TYPE.value(), "Material", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 430, "Material"), properties,
                List.of(singleSlot("child", 11, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition scrollbar() {
        List<PropertyDefinition> properties = new ArrayList<>();
        properties.add(namedProperty("controller", 1, false, List.of(
                new PropertyValueConstraint.DartObjectReferenceValues("ScrollController"),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        properties.add(namedProperty("thumbVisibility", 2, false, nullableBoolean()));
        properties.add(namedProperty("trackVisibility", 3, false, nullableBoolean()));
        properties.add(namedProperty("thickness", 4, false, nullableNonNegativeNumbers()));
        properties.add(namedProperty("radius", 5, false, List.of(
                new PropertyValueConstraint.DartObjectReferenceValues("Radius"),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        properties.add(namedProperty("notificationPredicate", 6, false, List.of(
                new PropertyValueConstraint.DartObjectReferenceValues("ScrollNotificationPredicate"),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        properties.add(namedProperty("interactive", 7, false, nullableBoolean()));
        properties.add(namedProperty("scrollbarOrientation", 8, false, List.of(
                new PropertyValueConstraint.EnumValues(
                        new DartSymbolReference(WIDGETS_IMPORT, "ScrollbarOrientation"),
                        List.of("left", "right", "top", "bottom")),
                new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL))));
        if (properties.size() != ScrollbarWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError("Scrollbar catalog/property schema count mismatch");
        }
        return widget(ScrollbarWidgetPropertySchema.SCROLLBAR_TYPE.value(), "Scrollbar", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 440, "Scrollbar"), properties,
                List.of(singleSlot("child", 0, true, 1, ANY_WIDGET)));
    }

    private static WidgetDefinition menuAnchor() {
        WidgetDefinition shared = textButton();
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : MenuAnchorWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            if (MenuAnchorWidgetPropertySchema.localStyleProperties().contains(name)) {
                constraints = shared.property(new PropertyName(name)).orElseThrow().constraints();
            } else if (List.of("anchorTapClosesMenu", "consumeOutsideTap", "crossAxisUnconstrained", "useRootOverlay", "animated").contains(name)) {
                constraints = any(PropertyValueKind.BOOLEAN);
            } else if (name.equals("clipBehavior")) {
                constraints = enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
            } else {
                String type = switch (name) {
                    case "controller" -> "MenuController"; case "childFocusNode" -> "FocusNode";
                    case "style" -> "MenuStyle"; case "alignmentOffset" -> "Offset";
                    case "reservedPadding" -> "EdgeInsetsGeometry"; case "layerLink" -> "LayerLink";
                    case "onOpen", "onClose" -> "VoidCallback";
                    case "onAnimationStatusChanged" -> "ValueChanged<AnimationStatus>";
                    case "builder" -> "MenuAnchorChildBuilder";
                    default -> throw new IllegalStateException(name);
                };
                List<PropertyValueConstraint> values = new ArrayList<>();
                if (name.equals("alignmentOffset")) values.add(new PropertyValueConstraint.OffsetValues());
                if (name.equals("reservedPadding")) values.add(new PropertyValueConstraint.EdgeInsetsValues(false));
                values.add(new PropertyValueConstraint.DartObjectReferenceValues(type));
                values.addAll(any(PropertyValueKind.NULL)); constraints = List.copyOf(values);
            }
            properties.add(namedProperty(name, properties.size(), false, constraints));
        }
        return widget(MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE.value(), "MenuAnchor", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT), Set.of(),
                palette("flutter.material", 100, 340, "MenuAnchor"), properties,
                List.of(listSlot("menuChildren", 219, true, ANY_WIDGET), singleSlot("child", 220, false, 0, ANY_WIDGET)));
    }

    private static WidgetDefinition menuItemButton() {
        WidgetDefinition shared = textButton();
        List<PropertyDefinition> properties = new ArrayList<>();
        for (String name : MenuItemButtonWidgetPropertySchema.definitions().keySet()) {
            List<PropertyValueConstraint> constraints;
            if (MenuItemButtonWidgetPropertySchema.localStyleProperties().contains(name)) {
                constraints = shared.property(new PropertyName(name)).orElseThrow().constraints();
            } else {
                constraints = switch (name) {
                    case "enabled", "autofocus", "requestFocusOnHover", "closeOnActivate",
                            "shortcutControl", "shortcutShift", "shortcutAlt", "shortcutMeta", "shortcutIncludeRepeats" -> any(PropertyValueKind.BOOLEAN);
                    case "onPressed" -> List.of(new PropertyValueConstraint.DartObjectReferenceValues("VoidCallback"));
                    case "onHover", "onFocusChange", "focusNode", "statesController", "style", "shortcut" -> {
                        String type = switch (name) {
                            case "onHover", "onFocusChange" -> "ValueChanged<bool>";
                            case "focusNode" -> "FocusNode";
                            case "statesController" -> "WidgetStatesController";
                            case "style" -> "ButtonStyle";
                            default -> "MenuSerializableShortcut";
                        };
                        List<PropertyValueConstraint> nullable = new ArrayList<>(List.of(new PropertyValueConstraint.DartObjectReferenceValues(type)));
                        nullable.addAll(any(PropertyValueKind.NULL));
                        yield List.copyOf(nullable);
                    }
                    case "semanticsLabel" -> List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.STRING), new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
                    case "shortcutCharacter" -> any(PropertyValueKind.STRING);
                    case "shortcutTrigger" -> List.of(new PropertyValueConstraint.EnumValues(
                            new DartSymbolReference(SERVICES_IMPORT, "LogicalKeyboardKey"), MenuShortcutKeyCatalog.names()));
                    case "shortcutNumLock" -> enumValues("LockState", "ignored", "locked", "unlocked");
                    case "clipBehavior" -> enumValues("Clip", "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer");
                    case "overflowAxis" -> enumValues("Axis", "horizontal", "vertical");
                    default -> throw new IllegalStateException("Unknown MenuItemButton property " + name);
                };
            }
            properties.add(name.equals("enabled")
                    ? namedProperty(name, properties.size(), true, constraints, new PropertyValue.BooleanValue(true))
                    : namedProperty(name, properties.size(), false, constraints));
        }
        return widget(MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE.value(), "MenuItemButton", true,
                MATERIAL_IMPORT, List.of(MATERIAL_IMPORT, WIDGETS_IMPORT, SERVICES_IMPORT), Set.of(),
                palette("flutter.material", 100, 330, "MenuItemButton"), properties,
                List.of(singleSlot("child", 520, false, 0, ANY_WIDGET),
                        singleSlot("leadingIcon", 521, false, 0, ANY_WIDGET),
                        singleSlot("trailingIcon", 522, false, 0, ANY_WIDGET)));
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
                    == ElevatedButtonWidgetPropertySchema.Group.COMMON_STYLE
                    && !ElevatedButtonWidgetPropertySchema.layerBuilderProperties().contains(property.name().value())) {
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

        for (String name : ElevatedButtonWidgetPropertySchema.layerBuilderProperties()) {
            properties.add(namedProperty(name, order++, false,
                    List.of(new PropertyValueConstraint.DartObjectReferenceValues(
                            ElevatedButtonWidgetPropertySchema.BUTTON_LAYER_BUILDER_TYPE))));
        }

        if (properties.size() != ElevatedButtonWidgetPropertySchema.FLATTENED_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "ElevatedButton schema must expose exactly 288 properties; actual="
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

    private static List<PropertyValueConstraint> positiveNumbers() {
        return List.of(
                new PropertyValueConstraint.IntegerRange(
                        BigInteger.ONE, DartNumericLiterals.MAX_PORTABLE_INTEGER),
                new PropertyValueConstraint.DoubleRange(BigDecimal.ZERO, false, null, true));
    }

    private static List<PropertyValueConstraint> unboundedDoubles() {
        return List.of(new PropertyValueConstraint.DoubleRange(null, true, null, true));
    }

    private static List<PropertyValueConstraint> nullableBoolean() {
        List<PropertyValueConstraint> values = new ArrayList<>(any(PropertyValueKind.BOOLEAN));
        values.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        return List.copyOf(values);
    }

    private static List<PropertyValueConstraint> nullableNonNegativeNumbers() {
        List<PropertyValueConstraint> values = new ArrayList<>(nonNegativeNumbers());
        values.add(new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL));
        return List.copyOf(values);
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

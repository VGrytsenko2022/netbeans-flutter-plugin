package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInWidgetCatalogTest {
    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String GESTURES_IMPORT = "package:flutter/gestures.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void containsExactlyTheReviewedSeventeenTypesInCanonicalOrder() {
        assertEquals(List.of(
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.Scaffold",
                "flutter.widgets.Align",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Center",
                "flutter.widgets.Column",
                "flutter.widgets.Container",
                "flutter.widgets.Expanded",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.Icon",
                "flutter.widgets.Opacity",
                "flutter.widgets.Padding",
                "flutter.widgets.Row",
                "flutter.widgets.SizedBox",
                "flutter.widgets.Stack",
                "flutter.widgets.Text"), typeIds(BuiltInWidgetCatalog.getDefault().definitions()));
    }

    @Test
    void exposesTheExactReviewedConstConstructorCapabilities() {
        assertEquals(17, BuiltInWidgetCatalog.getDefault().definitions().size());
        assertEquals(14, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetDefinition::constConstructor)
                .count());
        assertEquals(List.of(
                        "flutter.material.AppBar",
                        "flutter.material.ElevatedButton",
                        "flutter.widgets.Container"),
                BuiltInWidgetCatalog.getDefault().definitions().stream()
                        .filter(value -> !value.constConstructor())
                        .map(value -> value.typeId().value())
                        .toList());
        assertEquals(541, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .mapToInt(value -> value.properties().size())
                .sum(), "Every reviewed writable property is counted exactly once");
        assertEquals(524, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(value -> !value.typeId().value().equals(
                        "flutter.material.Scaffold"))
                .mapToInt(value -> value.properties().size())
                .sum(), "Non-Scaffold writable properties are counted exactly once");
    }

    @Test
    void exposesExactOwningLibrariesAndKeepsEachOwnerImported() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("flutter.material.AppBar", MATERIAL_IMPORT),
                Map.entry("flutter.material.ElevatedButton", MATERIAL_IMPORT),
                Map.entry("flutter.material.Scaffold", MATERIAL_IMPORT),
                Map.entry("flutter.widgets.Align", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.AspectRatio", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Center", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Column", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Container", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Expanded", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.FractionallySizedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Icon", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Opacity", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Padding", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Row", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.SizedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Stack", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Text", WIDGETS_IMPORT));

        Map<String, String> actual = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        value -> value.typeId().value(), WidgetDefinition::dartLibraryUri));

        assertEquals(expected, actual);
        assertTrue(BuiltInWidgetCatalog.getDefault().definitions().stream()
                .allMatch(value -> value.importUris().contains(value.dartLibraryUri())));
        assertEquals(List.of(WIDGETS_IMPORT),
                definition("flutter.widgets.Icon").importUris(),
                "Typed IconData does not require an otherwise-unused material.dart import");
        assertEquals(List.of(MATERIAL_IMPORT, WIDGETS_IMPORT),
                definition("flutter.material.AppBar").importUris(),
                "AppBar owns Material symbols and typed enum/style symbols from widgets.dart");
    }

    @Test
    void everyBuiltInEnumSymbolUsesItsExactFlutterUmbrellaLibrary() {
        List<DartSymbolReference> enumTypes = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .flatMap(value -> value.properties().stream())
                .flatMap(value -> value.constraints().stream())
                .filter(PropertyValueConstraint.EnumValues.class::isInstance)
                .map(PropertyValueConstraint.EnumValues.class::cast)
                .map(PropertyValueConstraint.EnumValues::dartType)
                .distinct()
                .sorted(java.util.Comparator.comparing(DartSymbolReference::name))
                .toList();

        assertEquals(List.of(
                new DartSymbolReference(WIDGETS_IMPORT, "BlendMode"),
                new DartSymbolReference(WIDGETS_IMPORT, "BorderStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "Brightness"),
                new DartSymbolReference(WIDGETS_IMPORT, "Clip"),
                new DartSymbolReference(WIDGETS_IMPORT, "CrossAxisAlignment"),
                new DartSymbolReference(GESTURES_IMPORT, "DragStartBehavior"),
                new DartSymbolReference(WIDGETS_IMPORT, "FontStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "FontWeight"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisAlignment"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisSize"),
                new DartSymbolReference(MATERIAL_IMPORT, "MaterialTapTargetSize"),
                new DartSymbolReference(WIDGETS_IMPORT, "StackFit"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextAlign"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextBaseline"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextDecorationStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextDirection"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextLeadingDistribution"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextOverflow"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextWidthBasis"),
                new DartSymbolReference(WIDGETS_IMPORT, "VerticalDirection")), enumTypes);
    }

    @Test
    void exposesAStablePaletteOrderSeparateFromCanonicalIteration() {
        assertEquals(List.of(
                "flutter.material.Scaffold",
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Container",
                "flutter.widgets.Opacity",
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.Stack",
                "flutter.widgets.Expanded",
                "flutter.widgets.Text",
                "flutter.widgets.Icon"), typeIds(BuiltInWidgetCatalog.getDefault().paletteDefinitions()));
    }

    @Test
    void creationDefaultsAreExplicitTypedValues() {
        WidgetCatalog catalog = BuiltInWidgetCatalog.getDefault();
        PropertyValue text = property(catalog, "flutter.widgets.Text", "data")
                .creationDefault().orElseThrow();
        assertEquals(new PropertyValue.StringValue("Text"), text);

        PropertyValue padding = property(catalog, "flutter.widgets.Padding", "padding")
                .creationDefault().orElseThrow();
        PropertyValue.EdgeInsetsValue insets = assertInstanceOf(PropertyValue.EdgeInsetsValue.class, padding);
        assertEquals(BigDecimal.valueOf(16), insets.left());

        assertEquals(new PropertyValue.IconDataValue(
                        java.util.Optional.of(0xE5F9),
                        java.util.Optional.of("MaterialIcons"),
                        java.util.Optional.empty(),
                        false,
                        List.of()),
                property(catalog, "flutter.widgets.Icon", "icon").creationDefault().orElseThrow());
        assertEquals(new PropertyValue.BooleanValue(true),
                property(catalog, "flutter.material.ElevatedButton", "enabled")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                property(catalog, "flutter.widgets.AspectRatio", "aspectRatio")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                property(catalog, "flutter.widgets.Opacity", "opacity")
                        .creationDefault().orElseThrow());
        assertTrue(property(catalog, "flutter.widgets.Opacity", "alwaysIncludeSemantics")
                .creationDefault().isEmpty());
        for (String type : List.of(
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox")) {
            for (String property : List.of("alignment", "widthFactor", "heightFactor")) {
                assertTrue(property(catalog, type, property)
                        .creationDefault().isEmpty(), type + "." + property);
            }
        }
        for (String property : List.of(
                "alignment", "textDirection", "fit", "clipBehavior")) {
            assertTrue(property(catalog, "flutter.widgets.Stack", property)
                    .creationDefault().isEmpty(), "Stack." + property);
        }
        assertTrue(property(catalog, "flutter.widgets.Expanded", "flex")
                .creationDefault().isEmpty());
        assertTrue(property(catalog, "flutter.material.ElevatedButton", "onPressed")
                .creationDefault().isEmpty());
    }

    @Test
    void requiredAndNullableConstructorSemanticsRemainSeparate() {
        WidgetDefinition button = definition("flutter.material.ElevatedButton");
        SlotDefinition child = button.slot(new SlotName("child")).orElseThrow();
        assertTrue(child.parameter().required());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
    }

    @Test
    void scaffoldAppBarAcceptsPreferredSizeWidgetOnly() {
        SlotDefinition appBarSlot = definition("flutter.material.Scaffold")
                .slot(new SlotName("appBar")).orElseThrow();
        assertTrue(appBarSlot.acceptance().accepts(definition("flutter.material.AppBar")));
        assertFalse(appBarSlot.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void appBarExposesExactReviewedFlutter344FlattenedSurfaceAndSlots() {
        WidgetDefinition appBar = definition("flutter.material.AppBar");
        assertFalse(appBar.constConstructor());
        assertEquals(120, appBar.properties().size());
        assertTrue(appBar.properties().size() <= WidgetDefinition.MAX_PROPERTIES);
        assertEquals(AppBarWidgetPropertySchema.definitions().keySet(),
                appBar.properties().stream()
                        .map(value -> value.name().value())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertTrue(appBar.properties().stream()
                .allMatch(property -> property.creationDefault().isEmpty()));
        assertEquals(Set.of(BuiltInWidgetCatalog.PREFERRED_SIZE_WIDGET_TRAIT),
                appBar.traits());

        assertEquals(List.of("leading", "title", "actions", "flexibleSpace", "bottom"),
                appBar.slots().stream().map(slot -> slot.name().value()).toList());
        SlotDefinition bottom = appBar.slot(new SlotName("bottom")).orElseThrow();
        assertInstanceOf(SlotAcceptance.HasTrait.class, bottom.acceptance());
        assertTrue(bottom.acceptance().accepts(appBar));
        assertFalse(bottom.acceptance().accepts(definition("flutter.widgets.Text")));
        assertEquals(SlotCardinality.LIST,
                appBar.slot(new SlotName("actions")).orElseThrow().cardinality());
    }

    @Test
    void appBarPreservesLegacyParameterOrdersAndReviewedBounds() {
        WidgetDefinition appBar = definition("flutter.material.AppBar");
        assertEquals(DartParameter.named(0, false),
                appBar.slot(new SlotName("leading")).orElseThrow().parameter());
        assertEquals(DartParameter.named(1, false),
                appBar.slot(new SlotName("title")).orElseThrow().parameter());
        assertEquals(DartParameter.named(2, false),
                appBar.slot(new SlotName("actions")).orElseThrow().parameter());
        assertEquals(DartParameter.named(3, false),
                appBar.property(new PropertyName("backgroundColor")).orElseThrow().parameter());
        assertEquals(DartParameter.named(4, false),
                appBar.property(new PropertyName("centerTitle")).orElseThrow().parameter());
        assertEquals(DartParameter.named(5, false),
                appBar.property(new PropertyName("elevation")).orElseThrow().parameter());
        assertTrue(appBar.properties().stream()
                .filter(property -> !Set.of(
                        "backgroundColor", "centerTitle", "elevation")
                        .contains(property.name().value()))
                .allMatch(property -> property.parameter().order() > 5));
        assertTrue(appBar.slots().stream()
                .filter(slot -> !Set.of("leading", "title", "actions")
                        .contains(slot.name().value()))
                .allMatch(slot -> slot.parameter().order() > 5));

        assertDoubleRange(appBar, "toolbarOpacity",
                BigDecimal.ZERO, true, BigDecimal.ONE, true);
        assertDoubleRange(appBar, "shapeSideStrokeAlign",
                null, true, null, true);
        assertDoubleRange(appBar, "iconThemeWeight",
                BigDecimal.ZERO, false, BigDecimal.valueOf(32768), false);
        assertDoubleRange(appBar, "actionsIconThemeGrade",
                BigDecimal.valueOf(-32768), true, BigDecimal.valueOf(32768), false);
        assertStringPattern(appBar, "notificationPredicate", "depthZero", "depth1");
        assertStringPattern(appBar, "shapeKind", "circle", "custom");
    }

    @Test
    void enumAndRangeConstraintsAreNarrowAndTyped() {
        PropertyDefinition alignment = property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Column", "crossAxisAlignment");
        PropertyValueConstraint.EnumValues values =
                assertInstanceOf(PropertyValueConstraint.EnumValues.class, alignment.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "CrossAxisAlignment"), values.dartType());
        assertTrue(values.values().contains("baseline"));
        assertTrue(values.accepts(new PropertyValue.EnumValue("CrossAxisAlignment", "stretch")));

        PropertyDefinition spacing = property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Column", "spacing");
        assertEquals(List.of(PropertyValueKind.DOUBLE), spacing.acceptedKinds().stream().toList());
        PropertyValueConstraint.DoubleRange spacingRange = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                spacing.constraints().getFirst());
        assertTrue(spacingRange.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertFalse(spacingRange.accepts(new PropertyValue.DoubleValue(BigDecimal.ONE.negate())));

        PropertyDefinition textFontSize = property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "styleFontSize");
        PropertyValueConstraint.DoubleRange textFontSizeRange = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                textFontSize.constraints().getFirst());
        assertTrue(textFontSizeRange.accepts(
                new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertFalse(textFontSizeRange.accepts(
                new PropertyValue.DoubleValue(BigDecimal.ONE.negate())));

        PropertyDefinition maxLines = property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "maxLines");
        assertEquals(List.of(PropertyValueKind.INTEGER), maxLines.acceptedKinds().stream().toList());
        PropertyValueConstraint.IntegerRange range = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                maxLines.constraints().getFirst());
        BigInteger portableMaximum = BigInteger.ONE.shiftLeft(53).subtract(BigInteger.ONE);
        assertEquals(BigInteger.ONE, range.minimum());
        assertEquals(portableMaximum, range.maximum());
        assertTrue(range.accepts(new PropertyValue.IntegerValue(portableMaximum)));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(portableMaximum.add(BigInteger.ONE))));
    }

    @Test
    void flexAndTextExposeTheExactReviewedSafePropertySurface() {
        WidgetDefinition column = definition("flutter.widgets.Column");
        assertEquals(List.of(
                "mainAxisAlignment",
                "mainAxisSize",
                "crossAxisAlignment",
                "textDirection",
                "verticalDirection",
                "textBaseline",
                "spacing"), column.properties().stream()
                        .map(value -> value.name().value())
                        .toList());
        assertEquals(7, column.slot(new SlotName("children")).orElseThrow().parameter().order());

        WidgetDefinition text = definition("flutter.widgets.Text");
        assertEquals(List.of(
                "data",
                "textAlign",
                "textDirection",
                "softWrap",
                "overflow",
                "maxLines",
                "semanticsLabel",
                "semanticsIdentifier",
                "textWidthBasis",
                "selectionColor",
                "localeLanguageCode",
                "localeScriptCode",
                "localeCountryCode",
                "textScalerFactor",
                "textHeightApplyFirstAscent",
                "textHeightApplyLastDescent",
                "textHeightLeadingDistribution",
                "styleInherit",
                "styleColor",
                "styleBackgroundColor",
                "styleFontSize",
                "styleFontWeight",
                "styleFontStyle",
                "styleLetterSpacing",
                "styleWordSpacing",
                "styleTextBaseline",
                "styleHeight",
                "styleLeadingDistribution",
                "styleLocaleLanguageCode",
                "styleLocaleScriptCode",
                "styleLocaleCountryCode",
                "styleDecorationUnderline",
                "styleDecorationOverline",
                "styleDecorationLineThrough",
                "styleDecorationColor",
                "styleDecorationStyle",
                "styleDecorationThickness",
                "styleDebugLabel",
                "styleFontFamily",
                "styleFontFamilyFallback",
                "stylePackage",
                "styleOverflow",
                "strutFontFamily",
                "strutFontFamilyFallback",
                "strutFontSize",
                "strutHeight",
                "strutLeadingDistribution",
                "strutLeading",
                "strutFontWeight",
                "strutFontStyle",
                "strutForceHeight",
                "strutDebugLabel",
                "strutPackage",
                "styleThemeTextStyle",
                "styleForeground",
                "styleBackground",
                "styleShadows",
                "styleFontFeatures",
                "styleFontVariations"), text.properties().stream()
                        .map(value -> value.name().value())
                        .toList());
        assertEquals(List.of(PropertyValueKind.STRING),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "semanticsLabel")
                        .acceptedKinds().stream().toList());
        assertEquals(List.of(PropertyValueKind.STRING),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "semanticsIdentifier")
                        .acceptedKinds().stream().toList());
        assertEquals(List.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "selectionColor")
                        .acceptedKinds().stream().toList());
        assertEquals(
                TextWidgetPropertySchema.definitions().keySet(),
                text.properties().stream()
                        .map(value -> value.name().value())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                "Every catalogued Text property must have presentation and composite-generation metadata");

        assertStringPattern(text, "localeLanguageCode", "uk", "EN", "e");
        assertStringPattern(text, "styleLocaleLanguageCode", "fil", "EN", "abcd");
        assertStringPattern(text, "localeScriptCode", "Cyrl", "cyrl", "CYRL");
        assertStringPattern(text, "styleLocaleScriptCode", "Latn", "latin", "latn");
        assertStringPattern(text, "localeCountryCode", "UA", "ua", "UAE");
        assertStringPattern(text, "styleLocaleCountryCode", "419", "42", "Us");
    }

    @Test
    void catalogCollectionsAreImmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> BuiltInWidgetCatalog.getDefault().definitions().clear());
    }

    @Test
    void centerFactorsAndIconSizeAcceptZeroBoundary() {
        assertAcceptsZero(property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Center", "widthFactor"));
        assertAcceptsZero(property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Center", "heightFactor"));
        assertAcceptsZero(property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Icon", "size"));
    }

    @Test
    void iconExposesTheCompleteReviewedFlutter344ConstructorSurface() {
        WidgetDefinition icon = definition("flutter.widgets.Icon");
        assertEquals(List.of(
                "icon", "size", "fill", "weight", "grade", "opticalSize",
                "color", "shadows", "semanticLabel", "textDirection",
                "applyTextScaling", "blendMode", "fontWeight"),
                icon.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(IconWidgetPropertySchema.definitions().keySet(),
                icon.properties().stream().map(value -> value.name().value())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertTrue(icon.slots().isEmpty());

        PropertyDefinition iconData = property(
                BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Icon", "icon");
        assertEquals(DartParameter.positional(0), iconData.parameter());
        assertEquals(List.of(PropertyValueKind.ICON_DATA),
                iconData.acceptedKinds().stream().toList());
        assertInstanceOf(PropertyValueConstraint.MaterialIconValues.class,
                iconData.constraints().getFirst());

        assertDoubleRange(icon, "fill", BigDecimal.ZERO, true, BigDecimal.ONE, true);
        assertDoubleRange(icon, "weight", BigDecimal.ZERO, false,
                BigDecimal.valueOf(32768), false);
        assertDoubleRange(icon, "grade", BigDecimal.valueOf(-32768), true,
                BigDecimal.valueOf(32768), false);
        assertDoubleRange(icon, "opticalSize", BigDecimal.ZERO, false,
                BigDecimal.valueOf(32768), false);
        assertEquals(List.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                icon.property(new PropertyName("color")).orElseThrow()
                        .acceptedKinds().stream().toList());
        assertEquals(List.of(PropertyValueKind.SHADOW_LIST),
                icon.property(new PropertyName("shadows")).orElseThrow()
                        .acceptedKinds().stream().toList());
        assertTrue(IconWidgetPropertySchema.find(new PropertyName("weight")).orElseThrow()
                .description().contains("overrides Font weight"));
        String fontWeightDescription = IconWidgetPropertySchema
                .find(new PropertyName("fontWeight")).orElseThrow().description();
        assertTrue(fontWeightDescription.contains("not inherited from IconTheme"));
        assertTrue(fontWeightDescription.contains("Weight axis overrides"));
        assertTrue(IconWidgetPropertySchema.find(new PropertyName("shadows")).orElseThrow()
                .description().contains("explicit empty list"));
    }

    private static void assertDoubleRange(
            WidgetDefinition definition,
            String name,
            BigDecimal minimum,
            boolean minimumInclusive,
            BigDecimal maximum,
            boolean maximumInclusive) {
        PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                definition.property(new PropertyName(name)).orElseThrow()
                        .constraints().getFirst());
        assertEquals(minimum, range.minimum());
        assertEquals(minimumInclusive, range.minimumInclusive());
        assertEquals(maximum, range.maximum());
        assertEquals(maximumInclusive, range.maximumInclusive());
    }

    @Test
    void sizedBoxExposesExactDimensionsChildContractAndEmptyPrototype() {
        WidgetDefinition sizedBox = definition("flutter.widgets.SizedBox");

        assertEquals(List.of("width", "height"), sizedBox.properties().stream()
                .map(value -> value.name().value())
                .toList());
        assertNonNegativeNumberProperty(sizedBox, "width", 0);
        assertNonNegativeNumberProperty(sizedBox, "height", 1);

        assertEquals(List.of(new SlotName("child")), sizedBox.slots().stream()
                .map(SlotDefinition::name)
                .toList());
        SlotDefinition child = sizedBox.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));

        StableId id = StableId.parse("a9395b70-a774-45ff-8893-f13a15cfe958");
        WidgetNode prototype = WidgetNodePrototypeFactory.create(sizedBox, id);
        assertEquals(id, prototype.id());
        assertEquals(sizedBox.typeId(), prototype.type());
        assertTrue(prototype.properties().isEmpty(),
                "Nullable dimensions must stay absent in a new SizedBox prototype");
        assertEquals(List.of(new SlotName("child")),
                prototype.slots().keySet().stream().toList());
        WidgetSlot.SingleSlot prototypeChild = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child")));
        assertTrue(prototypeChild.child().isEmpty());
    }

    @Test
    void aspectRatioExposesRequiredPositiveDoubleAndOptionalAnyWidgetChild() {
        WidgetDefinition aspectRatio = definition("flutter.widgets.AspectRatio");

        assertEquals("AspectRatio", aspectRatio.dartClassName());
        assertTrue(aspectRatio.constConstructor());
        assertEquals(WIDGETS_IMPORT, aspectRatio.dartLibraryUri());
        assertEquals(List.of("aspectRatio"), aspectRatio.properties().stream()
                .map(value -> value.name().value())
                .toList());

        PropertyDefinition ratio = aspectRatio
                .property(new PropertyName("aspectRatio"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, true), ratio.parameter());
        assertEquals(List.of(PropertyValueKind.DOUBLE),
                ratio.acceptedKinds().stream().toList());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                ratio.creationDefault().orElseThrow());
        PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                ratio.constraints().getFirst());
        assertEquals(BigDecimal.ZERO, range.minimum());
        assertFalse(range.minimumInclusive());
        assertNull(range.maximum());
        assertTrue(range.maximumInclusive());
        assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ONE)));
        assertFalse(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertFalse(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ONE.negate())));

        assertEquals(List.of(new SlotName("child")), aspectRatio.slots().stream()
                .map(SlotDefinition::name)
                .toList());
        SlotDefinition child = aspectRatio.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(aspectRatio));
    }

    @Test
    void opacityExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition opacity = definition("flutter.widgets.Opacity");

        assertEquals("Opacity", opacity.dartClassName());
        assertTrue(opacity.constConstructor());
        assertEquals(WIDGETS_IMPORT, opacity.dartLibraryUri());
        assertEquals(List.of("opacity", "alwaysIncludeSemantics"),
                opacity.properties().stream()
                        .map(value -> value.name().value())
                        .toList());

        PropertyDefinition alpha = opacity.property(new PropertyName("opacity"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, true), alpha.parameter());
        assertEquals(Set.of(PropertyValueKind.DOUBLE), alpha.acceptedKinds());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                alpha.creationDefault().orElseThrow());
        PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                alpha.constraints().getFirst());
        assertEquals(BigDecimal.ZERO, range.minimum());
        assertTrue(range.minimumInclusive());
        assertEquals(BigDecimal.ONE, range.maximum());
        assertTrue(range.maximumInclusive());
        assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.valueOf(0.5))));
        assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ONE)));
        assertFalse(range.accepts(new PropertyValue.DoubleValue(
                BigDecimal.valueOf(-0.001))));
        assertFalse(range.accepts(new PropertyValue.DoubleValue(
                BigDecimal.valueOf(1.001))));

        PropertyDefinition semantics = opacity.property(
                new PropertyName("alwaysIncludeSemantics")).orElseThrow();
        assertEquals(DartParameter.named(1, false), semantics.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), semantics.acceptedKinds());
        assertTrue(semantics.creationDefault().isEmpty());

        SlotDefinition child = opacity.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(opacity));
    }

    @Test
    void alignExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition align = definition("flutter.widgets.Align");

        assertEquals("Align", align.dartClassName());
        assertTrue(align.constConstructor());
        assertEquals(WIDGETS_IMPORT, align.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), align.importUris());
        assertTrue(align.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 90, "Align"),
                align.palette());
        assertEquals(List.of("alignment", "widthFactor", "heightFactor"),
                align.properties().stream()
                        .map(value -> value.name().value())
                        .toList());

        PropertyDefinition alignment = align.property(new PropertyName("alignment"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefault().isEmpty());
        PropertyValueConstraint.AlignmentGeometryValues alignmentValues =
                assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                        alignment.constraints().getFirst());
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ZERO,
                BigDecimal.ZERO)));
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ONE,
                BigDecimal.ONE.negate())));

        assertNonNegativeNumberProperty(align, "widthFactor", 1);
        assertNonNegativeNumberProperty(align, "heightFactor", 2);

        SlotDefinition child = align.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(3, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(align));
    }

    @Test
    void fractionallySizedBoxExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.FractionallySizedBox");

        assertEquals("FractionallySizedBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                "flutter.layout", 200, 100, "FractionallySizedBox"),
                box.palette());
        assertEquals(List.of("alignment", "widthFactor", "heightFactor"),
                box.properties().stream()
                        .map(value -> value.name().value())
                        .toList());

        PropertyDefinition alignment = box.property(new PropertyName("alignment"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefault().isEmpty());
        PropertyValueConstraint.AlignmentGeometryValues alignmentValues =
                assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                        alignment.constraints().getFirst());
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ZERO,
                BigDecimal.ZERO)));
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ONE,
                BigDecimal.ONE.negate())));

        assertNonNegativeNumberProperty(box, "widthFactor", 1);
        assertNonNegativeNumberProperty(box, "heightFactor", 2);

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(3, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(box));
    }

    @Test
    void stackExposesExactFlutter344SurfaceAndOptionalAnyWidgetChildren() {
        WidgetDefinition stack = definition("flutter.widgets.Stack");

        assertEquals("Stack", stack.dartClassName());
        assertTrue(stack.constConstructor());
        assertEquals(WIDGETS_IMPORT, stack.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), stack.importUris());
        assertTrue(stack.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 110, "Stack"),
                stack.palette());
        assertEquals(List.of(
                        "alignment", "textDirection", "fit", "clipBehavior"),
                stack.properties().stream()
                        .map(value -> value.name().value())
                        .toList());

        PropertyDefinition alignment = stack.property(new PropertyName("alignment"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefault().isEmpty());
        PropertyValueConstraint.AlignmentGeometryValues alignmentValues =
                assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                        alignment.constraints().getFirst());
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ONE.negate(), BigDecimal.ONE)));
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ONE, BigDecimal.ONE.negate())));

        Map<String, List<String>> enumValues = Map.of(
                "textDirection", List.of("rtl", "ltr"),
                "fit", List.of("loose", "expand", "passthrough"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        Map<String, String> enumTypes = Map.of(
                "textDirection", "TextDirection",
                "fit", "StackFit",
                "clipBehavior", "Clip");
        int order = 1;
        for (Map.Entry<String, List<String>> entry : enumValues.entrySet().stream()
                .sorted(java.util.Comparator.comparingInt(value -> switch (value.getKey()) {
                    case "textDirection" -> 1;
                    case "fit" -> 2;
                    case "clipBehavior" -> 3;
                    default -> throw new AssertionError(value.getKey());
                })).toList()) {
            PropertyDefinition property = stack.property(new PropertyName(entry.getKey()))
                    .orElseThrow();
            assertEquals(DartParameter.named(order++, false), property.parameter());
            assertEquals(Set.of(PropertyValueKind.ENUM), property.acceptedKinds());
            assertTrue(property.creationDefault().isEmpty());
            PropertyValueConstraint.EnumValues values = assertInstanceOf(
                    PropertyValueConstraint.EnumValues.class,
                    property.constraints().getFirst());
            assertEquals(new DartSymbolReference(
                    WIDGETS_IMPORT, enumTypes.get(entry.getKey())), values.dartType());
            assertEquals(entry.getValue(), values.values());
            for (String value : entry.getValue()) {
                assertTrue(values.accepts(new PropertyValue.EnumValue(
                        enumTypes.get(entry.getKey()), value)), entry.getKey() + '.' + value);
            }
        }

        SlotDefinition children = stack.slot(new SlotName("children")).orElseThrow();
        assertEquals(DartParameter.named(4, false), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, children.acceptance());
        assertTrue(children.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(children.acceptance().accepts(stack));
    }

    @Test
    void expandedExposesExactFlutter344SurfaceAndRequiredAnyWidgetChild() {
        WidgetDefinition expanded = definition("flutter.widgets.Expanded");

        assertEquals("Expanded", expanded.dartClassName());
        assertTrue(expanded.constConstructor());
        assertEquals(WIDGETS_IMPORT, expanded.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), expanded.importUris());
        assertTrue(expanded.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 120, "Expanded"),
                expanded.palette());
        assertEquals(List.of("flex"), expanded.properties().stream()
                .map(value -> value.name().value()).toList());

        PropertyDefinition flex = expanded.property(new PropertyName("flex"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), flex.parameter());
        assertEquals(Set.of(PropertyValueKind.INTEGER), flex.acceptedKinds());
        assertTrue(flex.creationDefault().isEmpty());
        PropertyValueConstraint.IntegerRange range = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                flex.constraints().getFirst());
        assertEquals(BigInteger.ZERO, range.minimum());
        assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, range.maximum());
        assertTrue(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)));
        assertTrue(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER)));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(
                BigInteger.ONE.negate())));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));

        SlotDefinition child = expanded.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, true), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void containerExposesExactStructuredSurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition container = definition("flutter.widgets.Container");

        assertEquals(ContainerWidgetPropertySchema.CONTAINER_TYPE, container.typeId());
        assertEquals("Container", container.dartClassName());
        assertFalse(container.constConstructor());
        assertEquals(WIDGETS_IMPORT, container.dartLibraryUri());
        assertEquals(ContainerWidgetPropertySchema.definitions().keySet(),
                container.properties().stream()
                        .map(value -> value.name().value())
                        .collect(java.util.stream.Collectors.toCollection(
                                java.util.LinkedHashSet::new)));
        assertEquals(13, container.properties().size());
        assertTrue(container.properties().stream()
                .allMatch(value -> value.creationDefault().isEmpty()));

        for (PropertyDefinition property : container.properties()) {
            int expectedOrder = ContainerWidgetPropertySchema.find(property.name())
                    .orElseThrow().dartOrder();
            assertEquals(DartParameter.named(expectedOrder, false), property.parameter(),
                    property.name().value());
        }
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                container.property(new PropertyName("color")).orElseThrow().acceptedKinds());
        assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                container.property(new PropertyName("alignment")).orElseThrow()
                        .constraints().getFirst());
        assertInstanceOf(PropertyValueConstraint.BoxConstraintsValues.class,
                container.property(new PropertyName("constraints")).orElseThrow()
                        .constraints().getFirst());
        assertInstanceOf(PropertyValueConstraint.Matrix4Values.class,
                container.property(new PropertyName("transform")).orElseThrow()
                        .constraints().getFirst());
        assertInstanceOf(PropertyValueConstraint.BoxDecorationValues.class,
                container.property(new PropertyName("decoration")).orElseThrow()
                        .constraints().getFirst());
        assertTrue(assertInstanceOf(PropertyValueConstraint.EdgeInsetsValues.class,
                container.property(new PropertyName("padding")).orElseThrow()
                        .constraints().getFirst()).nonNegative());
        assertTrue(assertInstanceOf(PropertyValueConstraint.EdgeInsetsValues.class,
                container.property(new PropertyName("margin")).orElseThrow()
                        .constraints().getFirst()).nonNegative());

        SlotDefinition child = container.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(12, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(container));
    }

    @Test
    void buttonCallbackUsesReservedWordAwareReferenceConstraint() {
        PropertyDefinition onPressed = property(
                BuiltInWidgetCatalog.getDefault(),
                "flutter.material.ElevatedButton",
                "onPressed");
        PropertyValueConstraint callback = onPressed.constraints().stream()
                .filter(value -> value.kind() == PropertyValueKind.CALLBACK)
                .findFirst().orElseThrow();
        assertInstanceOf(PropertyValueConstraint.CallbackReference.class, callback);
        assertTrue(callback.accepts(new PropertyValue.CallbackValue("onContinue")));
        assertFalse(callback.accepts(new PropertyValue.CallbackValue("class")));
    }

    @Test
    void elevatedButtonExposesExactFlutter344EnterpriseSurfaceAndMaxPresets() {
        WidgetDefinition button = definition("flutter.material.ElevatedButton");
        assertFalse(button.constConstructor());
        assertEquals(286, button.properties().size());
        assertEquals(ElevatedButtonWidgetPropertySchema.definitions().keySet(),
                button.properties().stream()
                        .map(value -> value.name().value())
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of("child"), button.slots().stream()
                .map(value -> value.name().value()).toList());
        assertStringPattern(button, "styleShapeKind", "roundedSuperellipse",
                "superellipse", "rectangle");
        for (String cursor : List.of(
                "none", "resizeColumn", "resizeRow", "zoomIn", "zoomOut")) {
            assertStringPattern(button, "styleMouseCursor", cursor,
                    cursor + "Unknown");
        }
        assertStringPattern(button, "styleSplashFactory", "inkSplash",
                "splash", "inkFeature");
        assertDoubleRange(button, "styleSideStrokeAlign",
                null, true, null, true);
        assertDoubleRange(button, "stylePressedSideStrokeAlign",
                null, true, null, true);
    }

    private static void assertAcceptsZero(PropertyDefinition property) {
        assertTrue(property.constraints().stream()
                .anyMatch(value -> value.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertTrue(property.constraints().stream()
                .anyMatch(value -> value.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO))));
    }

    private static void assertNonNegativeNumberProperty(
            WidgetDefinition definition,
            String propertyName,
            int parameterOrder) {
        PropertyDefinition dimension = definition
                .property(new PropertyName(propertyName))
                .orElseThrow();
        assertEquals(DartParameter.named(parameterOrder, false), dimension.parameter());
        assertTrue(dimension.creationDefault().isEmpty());
        assertEquals(List.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                dimension.acceptedKinds().stream().toList());

        PropertyValueConstraint.IntegerRange integers = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                dimension.constraints().get(0));
        assertEquals(BigInteger.ZERO, integers.minimum());
        assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, integers.maximum());
        assertTrue(integers.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)));
        assertTrue(integers.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER)));
        assertFalse(integers.accepts(new PropertyValue.IntegerValue(BigInteger.ONE.negate())));

        PropertyValueConstraint.DoubleRange doubles = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                dimension.constraints().get(1));
        assertEquals(BigDecimal.ZERO, doubles.minimum());
        assertTrue(doubles.minimumInclusive());
        assertNull(doubles.maximum());
        assertTrue(doubles.maximumInclusive());
        assertTrue(doubles.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertTrue(doubles.accepts(new PropertyValue.DoubleValue(new BigDecimal("1280.5"))));
        assertFalse(doubles.accepts(new PropertyValue.DoubleValue(new BigDecimal("-0.5"))));
    }

    private static void assertStringPattern(
            WidgetDefinition definition,
            String propertyName,
            String accepted,
            String... rejected) {
        PropertyValueConstraint constraint = definition
                .property(new PropertyName(propertyName))
                .orElseThrow()
                .constraints()
                .getFirst();
        assertInstanceOf(PropertyValueConstraint.StringPattern.class, constraint);
        assertTrue(constraint.accepts(new PropertyValue.StringValue(accepted)), propertyName);
        for (String value : rejected) {
            assertFalse(constraint.accepts(new PropertyValue.StringValue(value)),
                    propertyName + " unexpectedly accepted " + value);
        }
    }

    private static PropertyDefinition property(WidgetCatalog catalog, String typeId, String property) {
        return catalog.find(new WidgetTypeId(typeId)).orElseThrow()
                .property(new PropertyName(property)).orElseThrow();
    }

    private static WidgetDefinition definition(String typeId) {
        return BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(typeId)).orElseThrow();
    }

    private static List<String> typeIds(List<WidgetDefinition> definitions) {
        return definitions.stream().map(value -> value.typeId().value()).toList();
    }
}

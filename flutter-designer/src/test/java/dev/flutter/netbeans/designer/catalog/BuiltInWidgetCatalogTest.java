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
import java.util.Optional;
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
    private static final String SERVICES_IMPORT = "package:flutter/services.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";
    private static final String RENDERING_IMPORT = "package:flutter/rendering.dart";
    private static final String DART_UI_IMPORT = "dart:ui";

    @Test
    void containsExactlyTheReviewedFortySixTypesInCanonicalOrder() {
        assertEquals(List.of(
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.Scaffold",
                "flutter.material.TextField",
                "flutter.widgets.Align",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Baseline",
                "flutter.widgets.Center",
                "flutter.widgets.ColoredBox",
                "flutter.widgets.Column",
                "flutter.widgets.ConstrainedBox",
                "flutter.widgets.Container",
                "flutter.widgets.DecoratedBox",
                "flutter.widgets.Directionality",
                "flutter.widgets.ExcludeSemantics",
                "flutter.widgets.Expanded",
                "flutter.widgets.FittedBox",
                "flutter.widgets.Flexible",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.GridView",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.IndexedStack",
                "flutter.widgets.IntrinsicHeight",
                "flutter.widgets.IntrinsicWidth",
                "flutter.widgets.LimitedBox",
                "flutter.widgets.ListBody",
                "flutter.widgets.ListView",
                "flutter.widgets.Offstage",
                "flutter.widgets.Opacity",
                "flutter.widgets.OverflowBar",
                "flutter.widgets.OverflowBox",
                "flutter.widgets.Padding",
                "flutter.widgets.Placeholder",
                "flutter.widgets.RotatedBox",
                "flutter.widgets.Row",
                "flutter.widgets.SafeArea",
                "flutter.widgets.SingleChildScrollView",
                "flutter.widgets.SizedBox",
                "flutter.widgets.SizedOverflowBox",
                "flutter.widgets.Spacer",
                "flutter.widgets.Stack",
                "flutter.widgets.Text",
                "flutter.widgets.Transform",
                "flutter.widgets.UnconstrainedBox",
                "flutter.widgets.Wrap"), typeIds(BuiltInWidgetCatalog.getDefault().definitions()));
    }

    @Test
    void exposesTheExactReviewedConstConstructorCapabilities() {
        assertEquals(46, BuiltInWidgetCatalog.getDefault().definitions().size());
        assertEquals(40, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetDefinition::constConstructor)
                .count());
        assertEquals(List.of(
                        "flutter.material.AppBar",
                        "flutter.material.ElevatedButton",
                        "flutter.widgets.ConstrainedBox",
                        "flutter.widgets.Container",
                        "flutter.widgets.GridView",
                        "flutter.widgets.ListView"),
                BuiltInWidgetCatalog.getDefault().definitions().stream()
                        .filter(value -> !value.constConstructor())
                        .map(value -> value.typeId().value())
                        .toList());
        assertEquals(735, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .mapToInt(value -> value.properties().size())
                .sum(), "Every reviewed writable property is counted exactly once");
        assertEquals(718, BuiltInWidgetCatalog.getDefault().definitions().stream()
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
                Map.entry("flutter.material.TextField", MATERIAL_IMPORT),
                Map.entry("flutter.widgets.Align", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.AspectRatio", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Baseline", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Center", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.ColoredBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Column", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.ConstrainedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Container", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.DecoratedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Directionality", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.ExcludeSemantics", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Expanded", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.FittedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Flexible", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.FractionallySizedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.GridView", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Icon", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Image", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.IndexedStack", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.IntrinsicHeight", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.IntrinsicWidth", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.LimitedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.ListBody", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.ListView", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Offstage", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Opacity", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.OverflowBar", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.OverflowBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Padding", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Placeholder", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Row", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.RotatedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.SafeArea", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.SingleChildScrollView", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.SizedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.SizedOverflowBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Spacer", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Stack", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Text", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Transform", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.UnconstrainedBox", WIDGETS_IMPORT),
                Map.entry("flutter.widgets.Wrap", WIDGETS_IMPORT));

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
        assertEquals(List.of(
                        DART_UI_IMPORT, GESTURES_IMPORT, MATERIAL_IMPORT,
                        SERVICES_IMPORT, WIDGETS_IMPORT),
                definition("flutter.material.TextField").importUris());
        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                definition("flutter.widgets.ListView").importUris());
        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                definition("flutter.widgets.GridView").importUris());
        assertEquals(List.of(GESTURES_IMPORT, RENDERING_IMPORT, WIDGETS_IMPORT),
                definition("flutter.widgets.SingleChildScrollView").importUris());
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                definition("flutter.widgets.OverflowBox").importUris(),
                "OverflowBoxFit is owned by rendering.dart in Flutter 3.44.8");
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT),
                definition("flutter.widgets.DecoratedBox").importUris(),
                "DecorationPosition is owned by rendering.dart in Flutter 3.44.8");
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
                new DartSymbolReference(WIDGETS_IMPORT, "Axis"),
                new DartSymbolReference(WIDGETS_IMPORT, "BlendMode"),
                new DartSymbolReference(WIDGETS_IMPORT, "BorderStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "BoxFit"),
                new DartSymbolReference(DART_UI_IMPORT, "BoxHeightStyle"),
                new DartSymbolReference(DART_UI_IMPORT, "BoxWidthStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "Brightness"),
                new DartSymbolReference(WIDGETS_IMPORT, "Clip"),
                new DartSymbolReference(WIDGETS_IMPORT, "CrossAxisAlignment"),
                new DartSymbolReference(RENDERING_IMPORT, "DecorationPosition"),
                new DartSymbolReference(GESTURES_IMPORT, "DragStartBehavior"),
                new DartSymbolReference(WIDGETS_IMPORT, "FilterQuality"),
                new DartSymbolReference(RENDERING_IMPORT, "FlexFit"),
                new DartSymbolReference(WIDGETS_IMPORT, "FontStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "FontWeight"),
                new DartSymbolReference(RENDERING_IMPORT, "HitTestBehavior"),
                new DartSymbolReference(WIDGETS_IMPORT, "ImageRepeat"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisAlignment"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisSize"),
                new DartSymbolReference(MATERIAL_IMPORT, "MaterialTapTargetSize"),
                new DartSymbolReference(SERVICES_IMPORT, "MaxLengthEnforcement"),
                new DartSymbolReference(WIDGETS_IMPORT, "OverflowBarAlignment"),
                new DartSymbolReference(RENDERING_IMPORT, "OverflowBoxFit"),
                new DartSymbolReference(
                        WIDGETS_IMPORT, "ScrollViewKeyboardDismissBehavior"),
                new DartSymbolReference(SERVICES_IMPORT, "SmartDashesType"),
                new DartSymbolReference(SERVICES_IMPORT, "SmartQuotesType"),
                new DartSymbolReference(WIDGETS_IMPORT, "StackFit"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextAlign"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextBaseline"),
                new DartSymbolReference(SERVICES_IMPORT, "TextCapitalization"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextDecorationStyle"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextDirection"),
                new DartSymbolReference(SERVICES_IMPORT, "TextInputAction"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextLeadingDistribution"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextOverflow"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextWidthBasis"),
                new DartSymbolReference(WIDGETS_IMPORT, "VerticalDirection"),
                new DartSymbolReference(WIDGETS_IMPORT, "WrapAlignment"),
                new DartSymbolReference(WIDGETS_IMPORT, "WrapCrossAlignment")), enumTypes);
    }

    @Test
    void exposesAStablePaletteOrderSeparateFromCanonicalIteration() {
        List<WidgetDefinition> palette =
                BuiltInWidgetCatalog.getDefault().paletteDefinitions();
        assertEquals(List.of(
                "flutter.material.Scaffold",
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.TextField",
                "flutter.widgets.Column",
                "flutter.widgets.Row",
                "flutter.widgets.Wrap",
                "flutter.widgets.Padding",
                "flutter.widgets.Center",
                "flutter.widgets.SizedBox",
                "flutter.widgets.AspectRatio",
                "flutter.widgets.Container",
                "flutter.widgets.Opacity",
                "flutter.widgets.Align",
                "flutter.widgets.FractionallySizedBox",
                "flutter.widgets.FittedBox",
                "flutter.widgets.ConstrainedBox",
                "flutter.widgets.UnconstrainedBox",
                "flutter.widgets.LimitedBox",
                "flutter.widgets.OverflowBox",
                "flutter.widgets.Stack",
                "flutter.widgets.IndexedStack",
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer",
                "flutter.widgets.Baseline",
                "flutter.widgets.IntrinsicHeight",
                "flutter.widgets.IntrinsicWidth",
                "flutter.widgets.Offstage",
                "flutter.widgets.SizedOverflowBox",
                "flutter.widgets.Transform",
                "flutter.widgets.RotatedBox",
                "flutter.widgets.ListBody",
                "flutter.widgets.OverflowBar",
                "flutter.widgets.SafeArea",
                "flutter.widgets.ListView",
                "flutter.widgets.GridView",
                "flutter.widgets.SingleChildScrollView",
                "flutter.widgets.Text",
                "flutter.widgets.Icon",
                "flutter.widgets.Image",
                "flutter.widgets.ColoredBox",
                "flutter.widgets.Placeholder",
                "flutter.widgets.Directionality",
                "flutter.widgets.DecoratedBox",
                "flutter.widgets.ExcludeSemantics"), typeIds(palette));
        assertEquals(4, palette.stream()
                .filter(definition -> definition.palette().categoryId()
                        .equals("flutter.material"))
                .count());
        assertEquals(31, palette.stream()
                .filter(definition -> definition.palette().categoryId()
                        .equals("flutter.layout"))
                .count());
        assertEquals(3, palette.stream()
                .filter(definition -> definition.palette().categoryId()
                        .equals("flutter.scrolling"))
                .count());
        assertEquals(7, palette.stream()
                .filter(definition -> definition.palette().categoryId()
                        .equals("flutter.basic"))
                .count());
        assertEquals(1, palette.stream()
                .filter(definition -> definition.palette().categoryId()
                        .equals("flutter.accessibility"))
                .count());
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
        assertEquals(new PropertyValue.ColorValue(0xFF2196F3L),
                property(catalog, "flutter.widgets.ColoredBox", "color")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.BoxConstraintsValue(
                        BigDecimal.ZERO, java.util.Optional.empty(),
                        BigDecimal.ZERO, java.util.Optional.empty()),
                property(catalog, "flutter.widgets.ConstrainedBox", "constraints")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.SizeValue(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(100)),
                property(catalog, "flutter.widgets.SizedOverflowBox", "size")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.Matrix4Value(List.of(
                        BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE)),
                property(catalog, "flutter.widgets.Transform", "transform")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ONE),
                property(catalog, "flutter.widgets.RotatedBox", "quarterTurns")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(2)),
                property(catalog, "flutter.widgets.GridView", "crossAxisCount")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.EnumValue("TextDirection", "ltr"),
                property(catalog, "flutter.widgets.Directionality", "textDirection")
                        .creationDefault().orElseThrow());
        PropertyValue.BoxDecorationValue emptyDecoration = assertInstanceOf(
                PropertyValue.BoxDecorationValue.class,
                property(catalog, "flutter.widgets.DecoratedBox", "decoration")
                        .creationDefault().orElseThrow());
        assertTrue(emptyDecoration.color().isEmpty());
        assertTrue(emptyDecoration.image().isEmpty());
        assertTrue(emptyDecoration.border().isEmpty());
        assertTrue(emptyDecoration.borderRadius().isEmpty());
        assertTrue(emptyDecoration.boxShadow().isEmpty());
        assertTrue(emptyDecoration.gradient().isEmpty());
        assertTrue(emptyDecoration.backgroundBlendMode().isEmpty());
        assertEquals(PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE,
                emptyDecoration.shape());
        assertTrue(property(catalog, "flutter.widgets.DecoratedBox", "position")
                .creationDefault().isEmpty());
        for (PropertyDefinition property : definition("flutter.widgets.GridView").properties()) {
            if (!property.name().value().equals("crossAxisCount")) {
                assertTrue(property.creationDefault().isEmpty(),
                        "GridView." + property.name().value());
            }
        }
        for (String optional : List.of("mainAxis", "reverse")) {
            assertTrue(property(catalog, "flutter.widgets.ListBody", optional)
                    .creationDefault().isEmpty(), "ListBody." + optional);
        }
        for (String optional : List.of(
                "spacing", "alignment", "overflowSpacing", "overflowAlignment",
                "overflowDirection", "textDirection")) {
            assertTrue(property(catalog, "flutter.widgets.OverflowBar", optional)
                    .creationDefault().isEmpty(), "OverflowBar." + optional);
        }
        for (String optional : List.of(
                "left", "top", "right", "bottom", "minimum",
                "maintainBottomViewPadding")) {
            assertTrue(property(catalog, "flutter.widgets.SafeArea", optional)
                    .creationDefault().isEmpty(), "SafeArea." + optional);
        }
        for (String optional : List.of(
                "origin", "alignment", "transformHitTests", "filterQuality")) {
            assertTrue(property(catalog, "flutter.widgets.Transform", optional)
                    .creationDefault().isEmpty(), optional);
        }
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
        for (String property : List.of(
                "direction", "alignment", "spacing", "runAlignment", "runSpacing",
                "crossAxisAlignment", "textDirection", "verticalDirection", "clipBehavior")) {
            assertTrue(property(catalog, "flutter.widgets.Wrap", property)
                    .creationDefault().isEmpty(), "Wrap." + property);
        }
        for (String property : List.of("fit", "alignment", "clipBehavior")) {
            assertTrue(property(catalog, "flutter.widgets.FittedBox", property)
                    .creationDefault().isEmpty(), "FittedBox." + property);
        }
        for (String property : List.of(
                "textDirection", "alignment", "constrainedAxis", "clipBehavior")) {
            assertTrue(property(catalog, "flutter.widgets.UnconstrainedBox", property)
                    .creationDefault().isEmpty(), "UnconstrainedBox." + property);
        }
        for (String property : List.of("maxWidth", "maxHeight")) {
            assertTrue(property(catalog, "flutter.widgets.LimitedBox", property)
                    .creationDefault().isEmpty(), "LimitedBox." + property);
        }
        for (String property : List.of(
                "alignment", "minWidth", "maxWidth", "minHeight", "maxHeight", "fit")) {
            assertTrue(property(catalog, "flutter.widgets.OverflowBox", property)
                    .creationDefault().isEmpty(), "OverflowBox." + property);
        }
        assertTrue(property(catalog, "flutter.widgets.Expanded", "flex")
                .creationDefault().isEmpty());
        for (String property : List.of("flex", "fit")) {
            assertTrue(property(catalog, "flutter.widgets.Flexible", property)
                    .creationDefault().isEmpty(), "Flexible." + property);
        }
        for (String property : List.of(
                "alignment", "textDirection", "clipBehavior", "sizing", "index")) {
            assertTrue(property(catalog, "flutter.widgets.IndexedStack", property)
                    .creationDefault().isEmpty(), "IndexedStack." + property);
        }
        assertTrue(property(catalog, "flutter.widgets.Spacer", "flex")
                .creationDefault().isEmpty());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.valueOf(24)),
                property(catalog, "flutter.widgets.Baseline", "baseline")
                        .creationDefault().orElseThrow());
        assertEquals(new PropertyValue.EnumValue("TextBaseline", "alphabetic"),
                property(catalog, "flutter.widgets.Baseline", "baselineType")
                        .creationDefault().orElseThrow());
        assertTrue(definition("flutter.material.TextField").properties().stream()
                .allMatch(value -> value.creationDefault().isEmpty()));
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
    void wrapExposesTheExactFlutter344ConstructorSurfaceAndOrderedChildren() {
        WidgetDefinition wrap = definition("flutter.widgets.Wrap");

        assertTrue(wrap.constConstructor());
        assertEquals("Wrap", wrap.dartClassName());
        assertEquals(List.of(
                        "direction", "alignment", "spacing", "runAlignment", "runSpacing",
                        "crossAxisAlignment", "textDirection", "verticalDirection", "clipBehavior"),
                wrap.properties().stream().map(value -> value.name().value()).toList());
        for (int index = 0; index < wrap.properties().size(); index++) {
            assertEquals(DartParameter.named(index, false),
                    wrap.properties().get(index).parameter());
            assertTrue(wrap.properties().get(index).creationDefault().isEmpty());
        }

        SlotDefinition children = wrap.slot(new SlotName("children")).orElseThrow();
        assertEquals(DartParameter.named(9, false), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, children.acceptance());
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
    void fittedBoxExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.FittedBox");

        assertEquals("FittedBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 105, "FittedBox"),
                box.palette());
        assertEquals(List.of("fit", "alignment", "clipBehavior"),
                box.properties().stream().map(value -> value.name().value()).toList());

        PropertyDefinition fit = box.property(new PropertyName("fit")).orElseThrow();
        assertEquals(DartParameter.named(0, false), fit.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), fit.acceptedKinds());
        PropertyValueConstraint.EnumValues fitValues = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                fit.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "BoxFit"),
                fitValues.dartType());
        assertEquals(List.of(
                        "fill", "contain", "cover", "fitWidth", "fitHeight", "none",
                        "scaleDown"),
                fitValues.values());

        PropertyDefinition alignment = box.property(new PropertyName("alignment"))
                .orElseThrow();
        assertEquals(DartParameter.named(1, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                alignment.constraints().getFirst());

        PropertyDefinition clip = box.property(new PropertyName("clipBehavior"))
                .orElseThrow();
        assertEquals(DartParameter.named(2, false), clip.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), clip.acceptedKinds());
        PropertyValueConstraint.EnumValues clipValues = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                clip.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "Clip"),
                clipValues.dartType());
        assertEquals(List.of(
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"),
                clipValues.values());
        assertTrue(box.properties().stream()
                .allMatch(value -> value.creationDefault().isEmpty()));

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
    void constrainedBoxExposesCompleteFlutter344ConstraintsAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.ConstrainedBox");

        assertEquals("ConstrainedBox", box.dartClassName());
        assertFalse(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                "flutter.layout", 200, 106, "ConstrainedBox"), box.palette());
        assertEquals(List.of("constraints"),
                box.properties().stream().map(value -> value.name().value()).toList());

        PropertyDefinition constraints = box.property(new PropertyName("constraints"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, true), constraints.parameter());
        assertEquals(Set.of(PropertyValueKind.BOX_CONSTRAINTS),
                constraints.acceptedKinds());
        PropertyValueConstraint.BoxConstraintsValues values = assertInstanceOf(
                PropertyValueConstraint.BoxConstraintsValues.class,
                constraints.constraints().getFirst());
        PropertyValue.BoxConstraintsValue unconstrained = assertInstanceOf(
                PropertyValue.BoxConstraintsValue.class,
                constraints.creationDefault().orElseThrow());
        assertEquals(Optional.of(BigDecimal.ZERO),
                unconstrained.minWidth().finiteValue());
        assertTrue(unconstrained.maxWidth().infinite());
        assertEquals(Optional.of(BigDecimal.ZERO),
                unconstrained.minHeight().finiteValue());
        assertTrue(unconstrained.maxHeight().infinite());
        assertTrue(values.accepts(unconstrained));
        assertTrue(values.accepts(new PropertyValue.BoxConstraintsValue(
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.Infinity.INSTANCE,
                PropertyValue.BoxConstraintBound.finite(BigDecimal.ZERO),
                PropertyValue.BoxConstraintBound.finite(BigDecimal.valueOf(240)))));

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(box));
    }

    @Test
    void unconstrainedBoxExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.UnconstrainedBox");

        assertEquals("UnconstrainedBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                "flutter.layout", 200, 107, "UnconstrainedBox"), box.palette());
        assertEquals(List.of(
                        "textDirection", "alignment", "constrainedAxis", "clipBehavior"),
                box.properties().stream().map(value -> value.name().value()).toList());

        Map<String, List<String>> enumValues = Map.of(
                "textDirection", List.of("rtl", "ltr"),
                "constrainedAxis", List.of("horizontal", "vertical"),
                "clipBehavior", List.of(
                        "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer"));
        Map<String, String> enumTypes = Map.of(
                "textDirection", "TextDirection",
                "constrainedAxis", "Axis",
                "clipBehavior", "Clip");
        for (int order = 0; order < box.properties().size(); order++) {
            PropertyDefinition property = box.properties().get(order);
            assertEquals(DartParameter.named(order + 1, false), property.parameter(),
                    property.name().value());
            assertTrue(property.creationDefault().isEmpty(), property.name().value());
            if (!property.name().value().equals("alignment")) {
                PropertyValueConstraint.EnumValues values = assertInstanceOf(
                        PropertyValueConstraint.EnumValues.class,
                        property.constraints().getFirst());
                assertEquals(new DartSymbolReference(
                        WIDGETS_IMPORT, enumTypes.get(property.name().value())),
                        values.dartType());
                assertEquals(enumValues.get(property.name().value()), values.values());
            }
        }

        PropertyDefinition alignment = box.property(new PropertyName("alignment"))
                .orElseThrow();
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        PropertyValueConstraint.AlignmentGeometryValues alignmentValues =
                assertInstanceOf(PropertyValueConstraint.AlignmentGeometryValues.class,
                        alignment.constraints().getFirst());
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL,
                BigDecimal.ZERO, BigDecimal.ZERO)));
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ONE, BigDecimal.ONE.negate())));

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(0, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(box));
    }

    @Test
    void limitedBoxExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.LimitedBox");

        assertEquals("LimitedBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                "flutter.layout", 200, 108, "LimitedBox"), box.palette());
        assertEquals(List.of("maxWidth", "maxHeight"),
                box.properties().stream().map(value -> value.name().value()).toList());

        for (int order = 0; order < box.properties().size(); order++) {
            PropertyDefinition property = box.properties().get(order);
            assertEquals(DartParameter.named(order, false), property.parameter(),
                    property.name().value());
            assertEquals(Set.of(PropertyValueKind.DOUBLE), property.acceptedKinds(),
                    property.name().value());
            assertTrue(property.creationDefault().isEmpty(), property.name().value());
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    property.constraints().getFirst());
            assertEquals(BigDecimal.ZERO, range.minimum(), property.name().value());
            assertTrue(range.minimumInclusive(), property.name().value());
            assertNull(range.maximum(), property.name().value());
            assertTrue(range.maximumInclusive(), property.name().value());
            assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                    property.name().value());
            assertTrue(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("2048.5"))), property.name().value());
            assertFalse(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("-0.5"))), property.name().value());
            assertFalse(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)),
                    property.name().value());
        }

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
        assertTrue(child.acceptance().accepts(box));
    }

    @Test
    void overflowBoxExposesExactFlutter344SurfaceAndOptionalAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.OverflowBox");

        assertEquals("OverflowBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                "flutter.layout", 200, 109, "OverflowBox"), box.palette());
        assertEquals(List.of(
                        "alignment", "minWidth", "maxWidth",
                        "minHeight", "maxHeight", "fit"),
                box.properties().stream().map(value -> value.name().value()).toList());

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
                BigDecimal.ZERO, BigDecimal.ZERO)));
        assertTrue(alignmentValues.accepts(new PropertyValue.AlignmentGeometryValue(
                PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                BigDecimal.ONE.negate(), BigDecimal.ONE)));

        for (String name : List.of(
                "minWidth", "maxWidth", "minHeight", "maxHeight")) {
            PropertyDefinition dimension = box.property(new PropertyName(name))
                    .orElseThrow();
            int order = List.of(
                    "alignment", "minWidth", "maxWidth",
                    "minHeight", "maxHeight", "fit").indexOf(name);
            assertEquals(DartParameter.named(order, false), dimension.parameter(), name);
            assertEquals(Set.of(PropertyValueKind.DOUBLE),
                    dimension.acceptedKinds(), name);
            assertTrue(dimension.creationDefault().isEmpty(), name);
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    dimension.constraints().getFirst());
            assertEquals(BigDecimal.ZERO, range.minimum(), name);
            assertTrue(range.minimumInclusive(), name);
            assertNull(range.maximum(), name);
            assertTrue(range.maximumInclusive(), name);
            assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)), name);
            assertTrue(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("2048.5"))), name);
            assertFalse(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("-0.5"))), name);
            assertFalse(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)), name);
        }

        PropertyDefinition fit = box.property(new PropertyName("fit")).orElseThrow();
        assertEquals(DartParameter.named(5, false), fit.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), fit.acceptedKinds());
        assertTrue(fit.creationDefault().isEmpty());
        PropertyValueConstraint.EnumValues fitValues = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class, fit.constraints().getFirst());
        assertEquals(new DartSymbolReference(RENDERING_IMPORT, "OverflowBoxFit"),
                fitValues.dartType());
        assertEquals(List.of("max", "deferToChild"), fitValues.values());

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(6, false), child.parameter());
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
    void flexibleExposesExactFlutter344SurfaceAndRequiredAnyWidgetChild() {
        WidgetDefinition flexible = definition("flutter.widgets.Flexible");

        assertEquals("Flexible", flexible.dartClassName());
        assertTrue(flexible.constConstructor());
        assertEquals(WIDGETS_IMPORT, flexible.dartLibraryUri());
        assertEquals(List.of(RENDERING_IMPORT, WIDGETS_IMPORT), flexible.importUris());
        assertTrue(flexible.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 130, "Flexible"),
                flexible.palette());
        assertEquals(List.of("flex", "fit"), flexible.properties().stream()
                .map(value -> value.name().value()).toList());

        PropertyDefinition flex = flexible.property(new PropertyName("flex"))
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

        PropertyDefinition fit = flexible.property(new PropertyName("fit"))
                .orElseThrow();
        assertEquals(DartParameter.named(1, false), fit.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), fit.acceptedKinds());
        assertTrue(fit.creationDefault().isEmpty());
        PropertyValueConstraint.EnumValues fitValues = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                fit.constraints().getFirst());
        assertEquals(new DartSymbolReference(RENDERING_IMPORT, "FlexFit"),
                fitValues.dartType());
        assertEquals(List.of("loose", "tight"), fitValues.values());

        SlotDefinition child = flexible.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, true), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void spacerExposesExactFlutter344LeafSurfaceAndPositiveFlex() {
        WidgetDefinition spacer = definition("flutter.widgets.Spacer");

        assertEquals("Spacer", spacer.dartClassName());
        assertTrue(spacer.constConstructor());
        assertEquals(WIDGETS_IMPORT, spacer.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), spacer.importUris());
        assertTrue(spacer.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 140, "Spacer"),
                spacer.palette());
        assertEquals(List.of("flex"), spacer.properties().stream()
                .map(value -> value.name().value()).toList());
        assertTrue(spacer.slots().isEmpty());

        PropertyDefinition flex = spacer.property(new PropertyName("flex"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), flex.parameter());
        assertEquals(Set.of(PropertyValueKind.INTEGER), flex.acceptedKinds());
        assertTrue(flex.creationDefault().isEmpty());
        PropertyValueConstraint.IntegerRange range = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                flex.constraints().getFirst());
        assertEquals(BigInteger.ONE, range.minimum());
        assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, range.maximum());
        assertTrue(range.accepts(new PropertyValue.IntegerValue(BigInteger.ONE)));
        assertTrue(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER)));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));
    }

    @Test
    void baselineExposesExactFlutter344ConstSurfaceAndReviewedVisiblePrototype() {
        WidgetDefinition baseline = definition("flutter.widgets.Baseline");

        assertEquals("Baseline", baseline.dartClassName());
        assertTrue(baseline.constConstructor());
        assertEquals(WIDGETS_IMPORT, baseline.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), baseline.importUris());
        assertTrue(baseline.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 150, "Baseline"),
                baseline.palette());
        assertEquals(List.of("baseline", "baselineType"), baseline.properties().stream()
                .map(value -> value.name().value()).toList());

        PropertyDefinition distance = baseline.property(new PropertyName("baseline"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, true), distance.parameter());
        assertEquals(Set.of(PropertyValueKind.DOUBLE), distance.acceptedKinds());
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.valueOf(24)),
                distance.creationDefault().orElseThrow(),
                "The Designer prototype keeps a typical text child visible; Flutter has no default");
        PropertyValueConstraint.DoubleRange distanceRange = assertInstanceOf(
                PropertyValueConstraint.DoubleRange.class,
                distance.constraints().getFirst());
        assertNull(distanceRange.minimum());
        assertTrue(distanceRange.minimumInclusive());
        assertNull(distanceRange.maximum());
        assertTrue(distanceRange.maximumInclusive());
        assertTrue(distanceRange.accepts(new PropertyValue.DoubleValue(
                new BigDecimal("-12.5"))),
                "Flutter 3.44.8 has no non-negative Baseline assertion");
        assertTrue(distanceRange.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
        assertFalse(distanceRange.accepts(new PropertyValue.IntegerValue(BigInteger.ONE)));

        PropertyDefinition type = baseline.property(new PropertyName("baselineType"))
                .orElseThrow();
        assertEquals(DartParameter.named(1, true), type.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), type.acceptedKinds());
        assertEquals(new PropertyValue.EnumValue("TextBaseline", "alphabetic"),
                type.creationDefault().orElseThrow());
        PropertyValueConstraint.EnumValues values = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                type.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "TextBaseline"),
                values.dartType());
        assertEquals(List.of("alphabetic", "ideographic"), values.values());

        SlotDefinition child = baseline.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void intrinsicHeightExposesExactFlutter344ConstStructuralSurface() {
        WidgetDefinition intrinsicHeight = definition(
                "flutter.widgets.IntrinsicHeight");

        assertEquals("IntrinsicHeight", intrinsicHeight.dartClassName());
        assertTrue(intrinsicHeight.constConstructor());
        assertEquals(WIDGETS_IMPORT, intrinsicHeight.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), intrinsicHeight.importUris());
        assertTrue(intrinsicHeight.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 160, "IntrinsicHeight"),
                intrinsicHeight.palette());
        assertTrue(intrinsicHeight.properties().isEmpty());

        SlotDefinition child = intrinsicHeight.slot(new SlotName("child"))
                .orElseThrow();
        assertEquals(DartParameter.named(0, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void intrinsicWidthExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition intrinsicWidth = definition(
                "flutter.widgets.IntrinsicWidth");

        assertEquals("IntrinsicWidth", intrinsicWidth.dartClassName());
        assertTrue(intrinsicWidth.constConstructor());
        assertEquals(WIDGETS_IMPORT, intrinsicWidth.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), intrinsicWidth.importUris());
        assertTrue(intrinsicWidth.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 170, "IntrinsicWidth"),
                intrinsicWidth.palette());
        assertEquals(List.of("stepWidth", "stepHeight"),
                intrinsicWidth.properties().stream()
                        .map(value -> value.name().value()).toList());

        for (int order = 0; order < 2; order++) {
            PropertyDefinition step = intrinsicWidth.properties().get(order);
            assertEquals(DartParameter.named(order, false), step.parameter());
            assertEquals(Set.of(PropertyValueKind.DOUBLE), step.acceptedKinds());
            assertTrue(step.creationDefault().isEmpty());
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    step.constraints().getFirst());
            assertEquals(BigDecimal.ZERO, range.minimum());
            assertTrue(range.minimumInclusive());
            assertNull(range.maximum());
            assertTrue(range.maximumInclusive());
            assertTrue(range.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO)));
            assertTrue(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("12.5"))));
            assertFalse(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("-0.01"))));
            assertFalse(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)));
        }

        SlotDefinition child = intrinsicWidth.slot(new SlotName("child"))
                .orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void offstageExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition offstage = definition("flutter.widgets.Offstage");

        assertEquals("Offstage", offstage.dartClassName());
        assertTrue(offstage.constConstructor());
        assertEquals(WIDGETS_IMPORT, offstage.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), offstage.importUris());
        assertTrue(offstage.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 180, "Offstage"),
                offstage.palette());
        assertEquals(List.of("offstage"),
                offstage.properties().stream()
                        .map(value -> value.name().value()).toList());

        PropertyDefinition hidden = offstage.properties().getFirst();
        assertEquals(DartParameter.named(0, false), hidden.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), hidden.acceptedKinds());
        assertTrue(hidden.creationDefault().isEmpty(),
                "omission must preserve Flutter's true default");
        PropertyValueConstraint booleanValues = hidden.constraints().getFirst();
        assertTrue(booleanValues.accepts(new PropertyValue.BooleanValue(true)));
        assertTrue(booleanValues.accepts(new PropertyValue.BooleanValue(false)));
        assertFalse(booleanValues.accepts(new PropertyValue.StringValue("true")));

        SlotDefinition child = offstage.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void sizedOverflowBoxExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition box = definition("flutter.widgets.SizedOverflowBox");

        assertEquals("SizedOverflowBox", box.dartClassName());
        assertTrue(box.constConstructor());
        assertEquals(WIDGETS_IMPORT, box.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), box.importUris());
        assertTrue(box.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 190, "SizedOverflowBox"),
                box.palette());
        assertEquals(List.of("size", "alignment"),
                box.properties().stream()
                        .map(value -> value.name().value()).toList());

        PropertyDefinition size = box.properties().get(0);
        assertEquals(DartParameter.named(0, true), size.parameter());
        assertEquals(Set.of(PropertyValueKind.SIZE), size.acceptedKinds());
        assertEquals(new PropertyValue.SizeValue(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(100)),
                size.creationDefault().orElseThrow());
        PropertyValueConstraint.SizeValues sizeValues = assertInstanceOf(
                PropertyValueConstraint.SizeValues.class,
                size.constraints().getFirst());
        assertTrue(sizeValues.accepts(new PropertyValue.SizeValue(
                BigDecimal.ZERO, new BigDecimal("12.5"))));
        assertFalse(sizeValues.accepts(new PropertyValue.SizeValue(
                new BigDecimal("1E+10000"), BigDecimal.ONE)));
        assertFalse(sizeValues.accepts(new PropertyValue.StringValue("100x100")));

        PropertyDefinition alignment = box.properties().get(1);
        assertEquals(DartParameter.named(1, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefault().isEmpty(),
                "omission must preserve Flutter's Alignment.center default");

        SlotDefinition child = box.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void transformNewExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition transform = definition("flutter.widgets.Transform");

        assertEquals("Transform", transform.dartClassName());
        assertTrue(transform.namedConstructor().isEmpty(),
                "only Transform.new is in this slice");
        assertTrue(transform.constConstructor());
        assertEquals(WIDGETS_IMPORT, transform.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), transform.importUris());
        assertTrue(transform.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 200, "Transform"),
                transform.palette());
        assertEquals(List.of(
                        "transform", "origin", "alignment", "transformHitTests",
                        "filterQuality"),
                transform.properties().stream()
                        .map(value -> value.name().value()).toList());

        PropertyDefinition matrix = transform.properties().get(0);
        assertEquals(DartParameter.named(0, true), matrix.parameter());
        assertEquals(Set.of(PropertyValueKind.MATRIX4), matrix.acceptedKinds());
        assertEquals(new PropertyValue.Matrix4Value(List.of(
                        BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE)),
                matrix.creationDefault().orElseThrow());

        PropertyDefinition origin = transform.properties().get(1);
        assertEquals(DartParameter.named(1, false), origin.parameter());
        assertEquals(Set.of(PropertyValueKind.OFFSET), origin.acceptedKinds());
        assertTrue(origin.creationDefault().isEmpty());
        PropertyValueConstraint.OffsetValues offsets = assertInstanceOf(
                PropertyValueConstraint.OffsetValues.class,
                origin.constraints().getFirst());
        assertTrue(offsets.accepts(new PropertyValue.OffsetValue(
                new BigDecimal("-12.5"), new BigDecimal("8.25"))));
        assertFalse(offsets.accepts(new PropertyValue.OffsetValue(
                new BigDecimal("1E+10000"), BigDecimal.ZERO)));

        PropertyDefinition alignment = transform.properties().get(2);
        assertEquals(DartParameter.named(2, false), alignment.parameter());
        assertEquals(Set.of(PropertyValueKind.ALIGNMENT_GEOMETRY),
                alignment.acceptedKinds());
        assertTrue(alignment.creationDefault().isEmpty());

        PropertyDefinition hitTests = transform.properties().get(3);
        assertEquals(DartParameter.named(3, false), hitTests.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), hitTests.acceptedKinds());
        assertTrue(hitTests.creationDefault().isEmpty(),
                "omission must preserve Flutter's true default");

        PropertyDefinition quality = transform.properties().get(4);
        assertEquals(DartParameter.named(4, false), quality.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), quality.acceptedKinds());
        PropertyValueConstraint.EnumValues qualities = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                quality.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "FilterQuality"),
                qualities.dartType());
        assertEquals(List.of("none", "low", "medium", "high"),
                qualities.values());
        assertTrue(quality.creationDefault().isEmpty());

        SlotDefinition child = transform.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(5, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void rotatedBoxExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition rotated = definition("flutter.widgets.RotatedBox");

        assertEquals("RotatedBox", rotated.dartClassName());
        assertTrue(rotated.namedConstructor().isEmpty());
        assertTrue(rotated.constConstructor());
        assertEquals(WIDGETS_IMPORT, rotated.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), rotated.importUris());
        assertTrue(rotated.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 210, "RotatedBox"),
                rotated.palette());
        assertEquals(List.of("quarterTurns"), rotated.properties().stream()
                .map(value -> value.name().value()).toList());

        PropertyDefinition quarterTurns = rotated.properties().getFirst();
        assertEquals(DartParameter.named(0, true), quarterTurns.parameter());
        assertEquals(Set.of(PropertyValueKind.INTEGER), quarterTurns.acceptedKinds());
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ONE),
                quarterTurns.creationDefault().orElseThrow());
        PropertyValueConstraint.IntegerRange range = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                quarterTurns.constraints().getFirst());
        assertEquals(DartNumericLiterals.MIN_PORTABLE_INTEGER, range.minimum());
        assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, range.maximum());
        assertTrue(range.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO)));
        assertTrue(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MIN_PORTABLE_INTEGER)));
        assertTrue(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER)));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MIN_PORTABLE_INTEGER.subtract(BigInteger.ONE))));
        assertFalse(range.accepts(new PropertyValue.IntegerValue(
                DartNumericLiterals.MAX_PORTABLE_INTEGER.add(BigInteger.ONE))));

        SlotDefinition child = rotated.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(1, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
        assertTrue(child.acceptance().accepts(definition("flutter.widgets.Text")));
    }

    @Test
    void listBodyExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition listBody = definition("flutter.widgets.ListBody");

        assertEquals("ListBody", listBody.dartClassName());
        assertTrue(listBody.namedConstructor().isEmpty());
        assertTrue(listBody.constConstructor());
        assertEquals(WIDGETS_IMPORT, listBody.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), listBody.importUris());
        assertTrue(listBody.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 220, "ListBody"),
                listBody.palette());
        assertEquals(List.of("mainAxis", "reverse"),
                listBody.properties().stream()
                        .map(value -> value.name().value()).toList());

        PropertyDefinition mainAxis = listBody.properties().get(0);
        assertEquals(DartParameter.named(0, false), mainAxis.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), mainAxis.acceptedKinds());
        assertTrue(mainAxis.creationDefault().isEmpty());
        PropertyValueConstraint.EnumValues axes = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                mainAxis.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "Axis"),
                axes.dartType());
        assertEquals(List.of("horizontal", "vertical"), axes.values());

        PropertyDefinition reverse = listBody.properties().get(1);
        assertEquals(DartParameter.named(1, false), reverse.parameter());
        assertEquals(Set.of(PropertyValueKind.BOOLEAN), reverse.acceptedKinds());
        assertTrue(reverse.creationDefault().isEmpty());

        SlotDefinition children = listBody.slot(
                new SlotName("children")).orElseThrow();
        assertEquals(DartParameter.named(2, false), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, children.acceptance());
        assertTrue(children.acceptance().accepts(
                definition("flutter.widgets.Text")));
    }

    @Test
    void overflowBarExposesExactFlutter344ConstEditableSurface() {
        WidgetDefinition overflowBar = definition("flutter.widgets.OverflowBar");

        assertEquals("OverflowBar", overflowBar.dartClassName());
        assertTrue(overflowBar.namedConstructor().isEmpty());
        assertTrue(overflowBar.constConstructor());
        assertEquals(WIDGETS_IMPORT, overflowBar.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), overflowBar.importUris());
        assertTrue(overflowBar.traits().isEmpty());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 230, "OverflowBar"),
                overflowBar.palette());
        assertEquals(List.of(
                        "spacing", "alignment", "overflowSpacing",
                        "overflowAlignment", "overflowDirection", "textDirection"),
                overflowBar.properties().stream()
                        .map(value -> value.name().value()).toList());

        for (int order : List.of(0, 2)) {
            PropertyDefinition spacing = overflowBar.properties().get(order);
            assertEquals(DartParameter.named(order, false), spacing.parameter());
            assertEquals(Set.of(PropertyValueKind.DOUBLE), spacing.acceptedKinds());
            assertTrue(spacing.creationDefault().isEmpty());
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    spacing.constraints().getFirst());
            assertTrue(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("-12.5"))));
            assertTrue(range.accepts(new PropertyValue.DoubleValue(
                    new BigDecimal("12.5"))));
        }

        assertEnumProperty(overflowBar.properties().get(1), 1,
                "MainAxisAlignment",
                List.of("start", "end", "center", "spaceBetween",
                        "spaceAround", "spaceEvenly"));
        assertEnumProperty(overflowBar.properties().get(3), 3,
                "OverflowBarAlignment", List.of("start", "end", "center"));
        assertEnumProperty(overflowBar.properties().get(4), 4,
                "VerticalDirection", List.of("up", "down"));
        assertEnumProperty(overflowBar.properties().get(5), 5,
                "TextDirection", List.of("rtl", "ltr"));

        SlotDefinition children = overflowBar.slot(
                new SlotName("children")).orElseThrow();
        assertEquals(DartParameter.named(6, false), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, children.acceptance());
        assertTrue(children.acceptance().accepts(
                definition("flutter.widgets.Text")));
    }

    @Test
    void textFieldExposesExactConstLeafSurfaceAndReviewedScalarDomains() {
        WidgetDefinition field = definition("flutter.material.TextField");

        assertEquals(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE, field.typeId());
        assertEquals("TextField", field.dartClassName());
        assertTrue(field.namedConstructor().isEmpty());
        assertTrue(field.constConstructor());
        assertEquals(MATERIAL_IMPORT, field.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.material", 100, 40, "Text Field"),
                field.palette());
        assertTrue(field.traits().isEmpty());
        assertTrue(field.slots().isEmpty());
        assertEquals(List.copyOf(TextFieldWidgetPropertySchema.definitions().keySet()),
                field.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(54, field.properties().size());
        for (int order = 0; order < field.properties().size(); order++) {
            PropertyDefinition property = field.properties().get(order);
            assertEquals(DartParameter.named(order, false), property.parameter(),
                    property.name().value());
            assertTrue(property.creationDefault().isEmpty(), property.name().value());
        }

        for (String name : List.of("maxLines", "minLines")) {
            PropertyValueConstraint.IntegerRange range = assertInstanceOf(
                    PropertyValueConstraint.IntegerRange.class,
                    field.property(new PropertyName(name)).orElseThrow()
                            .constraints().getFirst());
            assertEquals(BigInteger.ONE, range.minimum(), name);
            assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, range.maximum(), name);
        }
        PropertyValueConstraint.IntegerRange maxLength = assertInstanceOf(
                PropertyValueConstraint.IntegerRange.class,
                field.property(new PropertyName("maxLength")).orElseThrow()
                        .constraints().getFirst());
        assertEquals(BigInteger.valueOf(-1), maxLength.minimum());
        assertEquals(DartNumericLiterals.MAX_PORTABLE_INTEGER, maxLength.maximum());

        for (String name : List.of("cursorWidth", "cursorHeight")) {
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    field.property(new PropertyName(name)).orElseThrow().acceptedKinds(), name);
        }
        for (String name : List.of(
                "cursorRadiusX", "cursorRadiusY", "scrollPaddingLeft",
                "scrollPaddingTop", "scrollPaddingRight", "scrollPaddingBottom")) {
            PropertyDefinition property = field.property(new PropertyName(name)).orElseThrow();
            assertEquals(Set.of(PropertyValueKind.DOUBLE), property.acceptedKinds(), name);
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    property.constraints().getFirst());
            assertEquals(BigDecimal.ZERO, range.minimum(), name);
            assertTrue(range.minimumInclusive(), name);
            assertNull(range.maximum(), name);
        }
        for (String name : List.of(
                "onChanged", "onEditingComplete", "onSubmitted",
                "onAppPrivateCommand", "onTap", "onTapOutside", "onTapUpOutside")) {
            assertInstanceOf(PropertyValueConstraint.CallbackReference.class,
                    field.property(new PropertyName(name)).orElseThrow()
                            .constraints().getFirst(), name);
        }
        assertStringPattern(field, "keyboardType", "numberSignedDecimal", "signed");
        assertStringPattern(field, "textAlignVertical", "center", "middle");
        assertStringPattern(field, "mouseCursor", "resizeColumn", "resizeColumns");
    }

    @Test
    void imageExposesExactUnnamedConstFlutter344SurfaceWithoutFabricatedProvider() {
        WidgetDefinition image = definition("flutter.widgets.Image");

        assertEquals("Image", image.dartClassName());
        assertTrue(image.namedConstructor().isEmpty());
        assertTrue(image.constConstructor());
        assertEquals(WIDGETS_IMPORT, image.dartLibraryUri());
        assertEquals(List.of(WIDGETS_IMPORT), image.importUris());
        assertTrue(image.traits().isEmpty());
        assertEquals(new PaletteMetadata("flutter.basic", 300, 30, "Image"),
                image.palette());
        assertTrue(image.slots().isEmpty());
        assertEquals(List.of(
                "image", "frameBuilder", "loadingBuilder", "errorBuilder",
                "semanticLabel", "excludeFromSemantics", "width", "height",
                "color", "opacity", "colorBlendMode", "fit", "alignment",
                "repeat", "centerSliceLeft", "centerSliceTop",
                "centerSliceRight", "centerSliceBottom", "matchTextDirection",
                "gaplessPlayback", "isAntiAlias", "filterQuality"),
                image.properties().stream().map(value -> value.name().value()).toList());
        assertEquals(22, image.properties().size());
        for (int order = 0; order < image.properties().size(); order++) {
            PropertyDefinition property = image.properties().get(order);
            assertEquals(DartParameter.named(order, order == 0), property.parameter(),
                    property.name().value());
            assertTrue(property.creationDefault().isEmpty(), property.name().value());
        }

        assertInstanceOf(PropertyValueConstraint.ImageProviderValues.class,
                image.properties().getFirst().constraints().getFirst());
        for (String name : List.of("frameBuilder", "loadingBuilder", "errorBuilder")) {
            assertInstanceOf(PropertyValueConstraint.CallbackReference.class,
                    property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), name)
                            .constraints().getFirst());
        }
        for (String name : List.of("width", "height")) {
            assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.DOUBLE),
                    property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), name)
                            .acceptedKinds());
        }
        assertEquals(Set.of(PropertyValueKind.COLOR, PropertyValueKind.THEME_TOKEN),
                property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), "color")
                        .acceptedKinds());
        assertEquals(Set.of(PropertyValueKind.DOUBLE),
                property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), "opacity")
                        .acceptedKinds());

        for (String name : List.of(
                "centerSliceLeft", "centerSliceTop",
                "centerSliceRight", "centerSliceBottom")) {
            PropertyValueConstraint.DoubleRange range = assertInstanceOf(
                    PropertyValueConstraint.DoubleRange.class,
                    property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), name)
                            .constraints().getFirst());
            assertEquals(BigDecimal.ZERO, range.minimum(), name);
            assertTrue(range.minimumInclusive(), name);
            assertNull(range.maximum(), name);
        }

        Map<String, List<String>> enums = Map.of(
                "fit", List.of("fill", "contain", "cover", "fitWidth",
                        "fitHeight", "none", "scaleDown"),
                "repeat", List.of("repeat", "repeatX", "repeatY", "noRepeat"),
                "filterQuality", List.of("none", "low", "medium", "high"));
        Map<String, String> enumTypes = Map.of(
                "fit", "BoxFit",
                "repeat", "ImageRepeat",
                "filterQuality", "FilterQuality");
        enums.forEach((name, values) -> {
            PropertyValueConstraint.EnumValues constraint = assertInstanceOf(
                    PropertyValueConstraint.EnumValues.class,
                    property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(), name)
                            .constraints().getFirst());
            assertEquals(new DartSymbolReference(
                    WIDGETS_IMPORT, enumTypes.get(name)), constraint.dartType());
            assertEquals(values, constraint.values());
        });
        PropertyValueConstraint.EnumValues blend = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                property(BuiltInWidgetCatalog.getDefault(), image.typeId().value(),
                        "colorBlendMode").constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, "BlendMode"), blend.dartType());
        assertEquals(29, blend.values().size());
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

    private static void assertEnumProperty(
            PropertyDefinition property,
            int parameterOrder,
            String dartType,
            List<String> values) {
        assertEquals(DartParameter.named(parameterOrder, false), property.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), property.acceptedKinds());
        assertTrue(property.creationDefault().isEmpty());
        PropertyValueConstraint.EnumValues constraint = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                property.constraints().getFirst());
        assertEquals(new DartSymbolReference(WIDGETS_IMPORT, dartType),
                constraint.dartType());
        assertEquals(values, constraint.values());
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

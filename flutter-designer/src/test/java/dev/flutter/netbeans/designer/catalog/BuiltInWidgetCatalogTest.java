package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInWidgetCatalogTest {
    private static final String MATERIAL_IMPORT = "package:flutter/material.dart";
    private static final String WIDGETS_IMPORT = "package:flutter/widgets.dart";

    @Test
    void containsExactlyTheReviewedTenTypesInCanonicalOrder() {
        assertEquals(List.of(
                "flutter.material.AppBar",
                "flutter.material.ElevatedButton",
                "flutter.material.Scaffold",
                "flutter.widgets.Center",
                "flutter.widgets.Column",
                "flutter.widgets.Icon",
                "flutter.widgets.Padding",
                "flutter.widgets.Row",
                "flutter.widgets.SizedBox",
                "flutter.widgets.Text"), typeIds(BuiltInWidgetCatalog.getDefault().definitions()));
    }

    @Test
    void exposesTheExactReviewedConstConstructorCapabilities() {
        assertEquals(10, BuiltInWidgetCatalog.getDefault().definitions().size());
        assertEquals(9, BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetDefinition::constConstructor)
                .count());
        assertEquals(List.of("flutter.material.AppBar"),
                BuiltInWidgetCatalog.getDefault().definitions().stream()
                        .filter(value -> !value.constConstructor())
                        .map(value -> value.typeId().value())
                        .toList());
    }

    @Test
    void exposesExactOwningLibrariesAndKeepsEachOwnerImported() {
        Map<String, String> expected = Map.of(
                "flutter.material.AppBar", MATERIAL_IMPORT,
                "flutter.material.ElevatedButton", MATERIAL_IMPORT,
                "flutter.material.Scaffold", MATERIAL_IMPORT,
                "flutter.widgets.Center", WIDGETS_IMPORT,
                "flutter.widgets.Column", WIDGETS_IMPORT,
                "flutter.widgets.Icon", WIDGETS_IMPORT,
                "flutter.widgets.Padding", WIDGETS_IMPORT,
                "flutter.widgets.Row", WIDGETS_IMPORT,
                "flutter.widgets.SizedBox", WIDGETS_IMPORT,
                "flutter.widgets.Text", WIDGETS_IMPORT);

        Map<String, String> actual = BuiltInWidgetCatalog.getDefault().definitions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        value -> value.typeId().value(), WidgetDefinition::dartLibraryUri));

        assertEquals(expected, actual);
        assertTrue(BuiltInWidgetCatalog.getDefault().definitions().stream()
                .allMatch(value -> value.importUris().contains(value.dartLibraryUri())));
    }

    @Test
    void everyBuiltInEnumSymbolIsOwnedByTheWidgetsLibrary() {
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
                new DartSymbolReference(WIDGETS_IMPORT, "CrossAxisAlignment"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisAlignment"),
                new DartSymbolReference(WIDGETS_IMPORT, "MainAxisSize"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextAlign"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextBaseline"),
                new DartSymbolReference(WIDGETS_IMPORT, "TextDirection"),
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

        assertEquals(new PropertyValue.DartExpressionValue("Icons.star"),
                property(catalog, "flutter.widgets.Icon", "icon").creationDefault().orElseThrow());
        assertEquals(new PropertyValue.DartExpressionValue("null"),
                property(catalog, "flutter.material.ElevatedButton", "onPressed")
                        .creationDefault().orElseThrow());
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
                "selectionColor"), text.properties().stream()
                        .map(value -> value.name().value())
                        .toList());
        assertEquals(List.of(PropertyValueKind.STRING),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "semanticsLabel")
                        .acceptedKinds().stream().toList());
        assertEquals(List.of(PropertyValueKind.STRING),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "semanticsIdentifier")
                        .acceptedKinds().stream().toList());
        assertEquals(List.of(PropertyValueKind.COLOR),
                property(BuiltInWidgetCatalog.getDefault(), "flutter.widgets.Text", "selectionColor")
                        .acceptedKinds().stream().toList());
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

    private static void assertAcceptsZero(PropertyDefinition property) {
        assertTrue(property.constraints().stream()
                .anyMatch(value -> value.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertTrue(property.constraints().stream()
                .anyMatch(value -> value.accepts(new PropertyValue.DoubleValue(BigDecimal.ZERO))));
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

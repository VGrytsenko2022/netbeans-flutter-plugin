package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoratedBoxWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448DefaultConstructorSurface() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(DecoratedBoxWidgetPropertySchema.DECORATED_BOX_TYPE)
                .orElseThrow();

        assertEquals("DecoratedBox", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of(
                        "package:flutter/rendering.dart",
                        "package:flutter/widgets.dart"),
                definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.basic", 300, 70, "DecoratedBox"),
                definition.palette());
        assertEquals(List.of("decoration", "position"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(DecoratedBoxWidgetPropertySchema.definitions().keySet()
                        .stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());

        PropertyDefinition decoration = definition.property(
                new PropertyName("decoration")).orElseThrow();
        assertEquals(DartParameter.named(0, true), decoration.parameter());
        assertEquals(Set.of(PropertyValueKind.BOX_DECORATION),
                decoration.acceptedKinds());
        PropertyValue.BoxDecorationValue empty = assertInstanceOf(
                PropertyValue.BoxDecorationValue.class,
                decoration.creationDefault().orElseThrow());
        assertTrue(empty.color().isEmpty());
        assertTrue(empty.image().isEmpty());
        assertTrue(empty.border().isEmpty());
        assertTrue(empty.borderRadius().isEmpty());
        assertTrue(empty.boxShadow().isEmpty());
        assertTrue(empty.gradient().isEmpty());
        assertTrue(empty.backgroundBlendMode().isEmpty());
        assertEquals(PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE,
                empty.shape());
        assertInstanceOf(PropertyValueConstraint.BoxDecorationValues.class,
                decoration.constraints().getFirst());

        PropertyDefinition position = definition.property(
                new PropertyName("position")).orElseThrow();
        assertEquals(DartParameter.named(1, false), position.parameter());
        assertEquals(Set.of(PropertyValueKind.ENUM), position.acceptedKinds());
        assertTrue(position.creationDefault().isEmpty());
        PropertyValueConstraint.EnumValues values = assertInstanceOf(
                PropertyValueConstraint.EnumValues.class,
                position.constraints().getFirst());
        assertEquals(new DartSymbolReference(
                        "package:flutter/rendering.dart", "DecorationPosition"),
                values.dartType());
        assertEquals(List.of("background", "foreground"), values.values());
        assertTrue(values.accepts(new PropertyValue.EnumValue(
                "DecorationPosition", "foreground")));
        assertFalse(values.accepts(new PropertyValue.EnumValue(
                "DecorationPosition", "middle")));
        assertFalse(values.accepts(new PropertyValue.EnumValue(
                "TextDirection", "rtl")));

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(DartParameter.named(2, false), child.parameter());
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(0, child.minChildren());
        assertEquals(1, child.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, child.acceptance());
    }

    @Test
    void presentationSchemaIsCompleteOrderedAndFailClosed() {
        assertEquals(DecoratedBoxWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                DecoratedBoxWidgetPropertySchema.definitions().size());
        assertEquals(List.of("decoration", "position"),
                DecoratedBoxWidgetPropertySchema.definitions().keySet()
                        .stream().toList());
        assertEquals(List.of(0, 1),
                DecoratedBoxWidgetPropertySchema.definitions().values().stream()
                        .map(DecoratedBoxWidgetPropertySchema.Definition::dartOrder)
                        .toList());
        assertTrue(DecoratedBoxWidgetPropertySchema.definitions().values().stream()
                .allMatch(value -> value.group()
                        == DecoratedBoxWidgetPropertySchema.Group.DECORATION));
        assertEquals("Decoration",
                DecoratedBoxWidgetPropertySchema.definitions()
                        .get("decoration").displayName());
        assertEquals("Position",
                DecoratedBoxWidgetPropertySchema.definitions()
                        .get("position").displayName());
        assertTrue(DecoratedBoxWidgetPropertySchema.definitions().values().stream()
                .noneMatch(value -> value.description().isBlank()));
        assertTrue(DecoratedBoxWidgetPropertySchema.find(
                new PropertyName("decoration")).isPresent());
        assertTrue(DecoratedBoxWidgetPropertySchema.find(
                new PropertyName("position")).isPresent());
        assertTrue(DecoratedBoxWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());
    }
}

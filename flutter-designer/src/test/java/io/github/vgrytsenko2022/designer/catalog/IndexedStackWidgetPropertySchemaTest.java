package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import io.github.vgrytsenko2022.designer.model.SlotCardinality;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexedStackWidgetPropertySchemaTest {

    @Test
    void catalogMatchesThePinnedFlutter3448ConstConstructor() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE)
                .orElseThrow();

        assertEquals("IndexedStack", definition.dartClassName());
        assertTrue(definition.namedConstructor().isEmpty());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(List.of("package:flutter/widgets.dart"), definition.importUris());
        assertEquals(new PaletteMetadata(
                        "flutter.layout", 200, 115, "IndexedStack"),
                definition.palette());
        assertEquals(List.of(
                        "alignment", "textDirection", "clipBehavior", "sizing", "index"),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertEquals(IndexedStackWidgetPropertySchema.definitions().keySet().stream().toList(),
                definition.properties().stream()
                        .map(property -> property.name().value()).toList());
        assertTrue(definition.properties().stream()
                .allMatch(property -> !property.parameter().required()
                        && property.creationDefault().isEmpty()));

        PropertyDefinition index = definition.property(new PropertyName("index"))
                .orElseThrow();
        assertEquals(DartParameter.named(4, false), index.parameter());
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL),
                index.acceptedKinds());
        assertTrue(index.constraints().stream().anyMatch(constraint ->
                constraint.accepts(new PropertyValue.IntegerValue(BigInteger.ZERO))));
        assertTrue(index.constraints().stream().anyMatch(constraint ->
                constraint.accepts(new PropertyValue.NullValue())));
        assertFalse(index.constraints().stream().anyMatch(constraint ->
                constraint.accepts(new PropertyValue.IntegerValue(BigInteger.ONE.negate()))));

        SlotDefinition children = definition.slot(new SlotName("children")).orElseThrow();
        assertEquals(DartParameter.named(5, false), children.parameter());
        assertEquals(SlotCardinality.LIST, children.cardinality());
        assertEquals(0, children.minChildren());
        assertEquals(10_000, children.maxChildren());
        assertInstanceOf(SlotAcceptance.AnyWidget.class, children.acceptance());
    }

    @Test
    void presentationAndCanvasSchemasAreCompleteOrderedAndExact() {
        assertEquals(IndexedStackWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT,
                IndexedStackWidgetPropertySchema.definitions().size());
        assertEquals(List.of(
                        "alignment", "textDirection", "clipBehavior", "sizing", "index"),
                IndexedStackWidgetPropertySchema.definitions().keySet().stream().toList());
        IndexedStackWidgetPropertySchema.definitions().forEach((name, metadata) -> {
            assertEquals(IndexedStackWidgetPropertySchema.Group.LAYOUT, metadata.group());
            assertEquals(name, metadata.dartName());
            assertFalse(metadata.displayName().isBlank());
            assertFalse(metadata.description().isBlank());
            assertTrue(IndexedStackWidgetPropertySchema.find(
                    new PropertyName(name)).isPresent());
        });
        assertTrue(IndexedStackWidgetPropertySchema.find(
                new PropertyName("unknown")).isEmpty());

        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE)
                .orElseThrow();
        assertEquals(Set.of(
                        WidgetCapability.PROPERTIES,
                        WidgetCapability.CANVAS,
                        WidgetCapability.CREATE,
                        WidgetCapability.DND),
                BuiltInWidgetCapabilityCatalog.capabilities(definition));
        BuiltInWidgetCapabilityCatalog.CanvasProjection projection =
                BuiltInWidgetCapabilityCatalog.canvasProjection(definition).orElseThrow();
        assertEquals(definition.properties().stream().collect(
                        java.util.stream.Collectors.toMap(
                                PropertyDefinition::name,
                                PropertyDefinition::acceptedKinds)),
                projection.properties());
        assertEquals(Set.of(new SlotName("children")), projection.slots());
        var index = projection.propertyContracts().get(new PropertyName("index"));
        assertEquals(Set.of(PropertyValueKind.INTEGER, PropertyValueKind.NULL),
                index.acceptedKinds());
        assertEquals("any", index.constraintFingerprints().get(PropertyValueKind.NULL));
        assertEquals(new BuiltInWidgetCapabilityCatalog.CanvasSlotContract(
                        SlotCardinality.LIST, false, 0, 10_000),
                projection.slotContracts().get(new SlotName("children")));
    }

    @Test
    void detachedPrototypeKeepsEveryFlutterDefaultOmitted() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(IndexedStackWidgetPropertySchema.INDEXED_STACK_TYPE)
                .orElseThrow();
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition,
                StableId.parse("d2a4bcdb-c25c-4dc6-b5e9-5ce1d1ab8848"));

        assertTrue(prototype.properties().isEmpty());
        assertEquals(Set.of(new SlotName("children")), prototype.slots().keySet());
        assertTrue(assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children"))).children().isEmpty());
    }
}

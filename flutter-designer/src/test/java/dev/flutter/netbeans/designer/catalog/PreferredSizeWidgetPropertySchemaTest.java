package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferredSizeWidgetPropertySchemaTest {

    @Test
    void catalogExposesRequiredFiniteSizeAndRequiredChild() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(PreferredSizeWidgetPropertySchema.PREFERRED_SIZE_TYPE)
                .orElseThrow();

        assertEquals("PreferredSize", definition.dartClassName());
        assertTrue(definition.constConstructor());
        assertEquals("package:flutter/widgets.dart", definition.dartLibraryUri());
        assertEquals(new PaletteMetadata("flutter.layout", 200, 215, "PreferredSize"),
                definition.palette());
        assertEquals(Map.of("preferredSize", PreferredSizeWidgetPropertySchema.definitions()
                .get("preferredSize")), PreferredSizeWidgetPropertySchema.definitions());
        var property = definition.property(new PropertyName("preferredSize")).orElseThrow();
        assertEquals(java.util.Set.of(PropertyValueKind.SIZE), property.acceptedKinds());
        assertTrue(property.parameter().required());
        assertEquals(new PropertyValue.SizeValue(BigDecimal.valueOf(100), BigDecimal.valueOf(56)),
                property.creationDefault().orElseThrow());

        SlotDefinition child = definition.slot(new SlotName("child")).orElseThrow();
        assertEquals(SlotCardinality.SINGLE, child.cardinality());
        assertEquals(1, child.minChildren());
        assertEquals(1, child.maxChildren());
    }

    @Test
    void prototypeKeepsTypedCreationSizeWithoutFabricatingRequiredChild() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(PreferredSizeWidgetPropertySchema.PREFERRED_SIZE_TYPE)
                .orElseThrow();
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition, StableId.random());
        assertEquals(new PropertyValue.SizeValue(BigDecimal.valueOf(100), BigDecimal.valueOf(56)),
                prototype.properties().get(new PropertyName("preferredSize")));
        assertFalse(((WidgetSlot.SingleSlot) prototype.slots().get(new SlotName("child")))
                .child().isPresent());
    }
}

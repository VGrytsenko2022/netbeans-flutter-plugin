package dev.flutter.netbeans.designer.catalog;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.command.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverPaddingContractTest {
    @Test void completeConstructorIsTypedConstAndAdmitsOnlySlivers() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var d = catalog.find(SliverPaddingWidgetPropertySchema.TYPE).orElseThrow();
        assertEquals(1, d.properties().size()); assertEquals(1, d.slots().size());
        assertTrue(d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(d));
        var p = d.properties().getFirst(); var slot = d.slots().getFirst();
        assertEquals("padding", p.name().value()); assertTrue(p.parameter().required());
        assertEquals(Set.of(PropertyValueKind.EDGE_INSETS, PropertyValueKind.DART_OBJECT_REFERENCE), p.acceptedKinds());
        assertEquals("sliver", slot.name().value()); assertFalse(slot.parameter().required());
        assertEquals(0, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        var node = WidgetNodePrototypeFactory.create(d, StableId.random());
        assertEquals(p.creationDefault().orElseThrow(), node.properties().get(p.name()));
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        for (var candidate : catalog.definitions()) {
            assertEquals(WidgetPlacementRules.isSliverWidget(candidate) && !candidate.typeId().equals(SliverCrossAxisExpandedWidgetPropertySchema.TYPE), WidgetPlacementRules.accepts(d, slot, candidate), candidate.typeId().value());
        }
        assertTrue(WidgetPlacementRules.accepts(d, slot, d), "Nested padding is valid; instance cycles are not.");
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
    }
    @Test void paddingSupportsZeroPhysicalDirectionalAndReferencesButNotNullNegativeOrOverflow() {
        var d = SliverPaddingWidgetPropertySchema.padding();
        var z = BigDecimal.ZERO; var one = BigDecimal.ONE;
        for (PropertyValue value : List.of(
                new PropertyValue.EdgeInsetsValue(z, z, z, z),
                new PropertyValue.EdgeInsetsDirectionalValue(one, z, one, z),
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_padding", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()))) {
            assertTrue(d.constraints().stream().anyMatch(c -> c.accepts(value)), value.toString());
        }
        for (PropertyValue value : List.of(new PropertyValue.NullValue(),
                new PropertyValue.EdgeInsetsValue(one.negate(), z, z, z),
                new PropertyValue.EdgeInsetsDirectionalValue(z, z, one.negate(), z),
                new PropertyValue.EdgeInsetsValue(new BigDecimal("1e309"), z, z, z))) {
            assertFalse(d.constraints().stream().anyMatch(c -> c.accepts(value)), value.toString());
        }
    }
}

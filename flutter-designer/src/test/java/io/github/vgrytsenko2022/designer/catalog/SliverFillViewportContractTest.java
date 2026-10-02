package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverFillViewportContractTest {
    @Test void bothSurfacesHaveCompleteTypedContractsAndOnlySliverPlacement() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        for (var type : SliverFillViewportWidgetPropertySchema.TYPES) {
            var d = catalog.find(type).orElseThrow();
            boolean delegate = type.equals(SliverFillViewportWidgetPropertySchema.DELEGATE);
            assertEquals(delegate, d.constConstructor()); assertTrue(d.namedConstructor().isEmpty());
            assertEquals(delegate ? 4 : 8, d.properties().size());
            assertEquals(SliverFillViewportWidgetPropertySchema.fields(type).stream().map(f -> f.name()).toList(),
                    d.properties().stream().map(p -> p.name().value()).toList());
            var prototype = WidgetNodePrototypeFactory.create(d, StableId.random());
            assertEquals(delegate ? Map.of(new PropertyName("delegate"), new PropertyValue.StringValue("empty")) : Map.of(), prototype.properties());
            assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
            assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
            for (var field : d.properties()) {
                assertEquals(field.name().value().equals("delegate"), field.parameter().required());
                assertFalse(accepts(field, new PropertyValue.NullValue()), field.name().value());
                if (field.acceptedKinds().equals(Set.of(PropertyValueKind.BOOLEAN))) {
                    for (boolean value : List.of(false, true)) assertTrue(accepts(field, new PropertyValue.BooleanValue(value)));
                    assertFalse(accepts(field, new PropertyValue.StringValue("true")));
                }
            }
            var fraction = d.property(new PropertyName("viewportFraction")).orElseThrow();
            for (String value : List.of("0.01", "0.5", "1", "2")) assertTrue(accepts(fraction, new PropertyValue.DoubleValue(new BigDecimal(value))));
            for (String value : List.of("0", "-0.5")) assertFalse(accepts(fraction, new PropertyValue.DoubleValue(new BigDecimal(value))));
            assertFalse(accepts(fraction, new PropertyValue.IntegerValue(BigInteger.ONE)));
            if (delegate) assertTrue(d.slots().isEmpty());
            else {
                var slot = d.slots().getFirst();
                assertEquals(new SlotName("children"), slot.name()); assertTrue(slot.parameter().required());
                assertEquals(SlotCardinality.LIST, slot.cardinality()); assertEquals(0, slot.minChildren());
                for (var source : catalog.definitions()) {
                    boolean invalid = WidgetPlacementRules.isSliverWidget(source) || WidgetPlacementRules.isStackPositionedWidget(source) || Set.of("flutter.widgets.Expanded", "flutter.widgets.Flexible", "flutter.widgets.Spacer", "flutter.widgets.LayoutId", "flutter.widgets.TableRow", "flutter.widgets.TableCell", "flutter.material.DataColumn", "flutter.material.DataRow", "flutter.material.DataRow.byIndex", "flutter.material.DataCell", "flutter.material.DataCell.empty").contains(source.typeId().value());
                    assertEquals(!invalid, WidgetPlacementRules.accepts(d, slot, source), source.typeId().value());
                }
            }
            var padding = catalog.find(SliverPaddingWidgetPropertySchema.TYPE).orElseThrow();
            assertTrue(WidgetPlacementRules.accepts(padding, padding.slots().getFirst(), d));
        }
    }
    private static boolean accepts(PropertyDefinition field, PropertyValue value) { return field.constraints().stream().anyMatch(c -> c.accepts(value)); }
}

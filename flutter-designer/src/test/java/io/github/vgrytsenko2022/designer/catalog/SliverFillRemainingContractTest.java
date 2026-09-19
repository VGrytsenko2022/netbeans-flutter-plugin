package io.github.vgrytsenko2022.designer.catalog;
import io.github.vgrytsenko2022.designer.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliverFillRemainingContractTest {
    @Test void completeConstructorUsesNativeDefaultsOptionalBoxChildAndSliverOnlyPlacement() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var d = catalog.find(SliverFillRemainingWidgetPropertySchema.TYPE).orElseThrow();
        assertTrue(d.constConstructor());
        assertTrue(d.namedConstructor().isEmpty());
        assertEquals(List.of("hasScrollBody", "fillOverscroll"), d.properties().stream().map(p -> p.name().value()).toList());
        for (var p : d.properties()) {
            assertFalse(p.parameter().required()); assertTrue(p.creationDefault().isEmpty());
            assertEquals(Set.of(PropertyValueKind.BOOLEAN), p.acceptedKinds());
            for (boolean value : List.of(false, true)) assertTrue(p.constraints().stream().anyMatch(c -> c.accepts(new PropertyValue.BooleanValue(value))));
            for (PropertyValue value : List.of(new PropertyValue.NullValue(), new PropertyValue.StringValue("true")))
                assertFalse(p.constraints().stream().anyMatch(c -> c.accepts(value)));
        }
        assertEquals(1, d.slots().size());
        var slot = d.slots().getFirst();
        assertEquals(new SlotName("child"), slot.name()); assertFalse(slot.parameter().required());
        assertEquals(0, slot.minChildren()); assertEquals(1, slot.maxChildren());
        assertEquals(SlotCardinality.SINGLE, slot.cardinality());
        var prototype = WidgetNodePrototypeFactory.create(d, StableId.random());
        assertTrue(prototype.properties().isEmpty());
        assertFalse(WidgetPlacementRules.evaluateRoot(d).accepted());
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE, WidgetPlacementRules.creationMode(d));
        for (var source : catalog.definitions()) {
            boolean rejected = WidgetPlacementRules.isSliverWidget(source) || WidgetPlacementRules.isStackPositionedWidget(source) || Set.of(
                    "flutter.widgets.Expanded", "flutter.widgets.Flexible", "flutter.widgets.Spacer", "flutter.widgets.LayoutId", "flutter.widgets.TableRow", "flutter.widgets.TableCell", "flutter.material.DataColumn", "flutter.material.DataRow", "flutter.material.DataRow.byIndex", "flutter.material.DataCell", "flutter.material.DataCell.empty").contains(source.typeId().value());
            assertEquals(!rejected, WidgetPlacementRules.accepts(d, slot, source), source.typeId().value());
        }
        var padding = catalog.find(SliverPaddingWidgetPropertySchema.TYPE).orElseThrow();
        assertTrue(WidgetPlacementRules.accepts(padding, padding.slots().getFirst(), d));
        assertTrue(BuiltInWidgetCapabilityCatalog.supports(d, WidgetCapability.CANVAS));
    }
}

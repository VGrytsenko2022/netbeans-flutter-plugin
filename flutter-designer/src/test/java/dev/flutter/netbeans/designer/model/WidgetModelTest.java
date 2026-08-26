package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WidgetModelTest {
    @Test
    void widgetMapsAreSnapshotCopiesWithStableInsertionOrder() {
        LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>();
        PropertyName second = new PropertyName("second");
        PropertyName first = new PropertyName("first");
        properties.put(second, new PropertyValue.IntegerValue(java.math.BigInteger.TWO));
        properties.put(first, new PropertyValue.IntegerValue(java.math.BigInteger.ONE));

        LinkedHashMap<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        SlotName trailing = new SlotName("trailing");
        SlotName child = new SlotName("child");
        slots.put(trailing, WidgetSlot.SingleSlot.empty());
        slots.put(child, WidgetSlot.SingleSlot.empty());

        WidgetNode widget = new WidgetNode(
                StableId.random(), new WidgetTypeId("flutter.widgets.Example"), properties, slots);
        properties.clear();
        slots.clear();

        assertEquals(List.of(second, first), new ArrayList<>(widget.properties().keySet()));
        assertEquals(List.of(trailing, child), new ArrayList<>(widget.slots().keySet()));
        assertThrows(UnsupportedOperationException.class,
                () -> widget.properties().put(new PropertyName("third"), new PropertyValue.StringValue("x")));
        assertThrows(UnsupportedOperationException.class,
                () -> widget.slots().clear());
    }

    @Test
    void widgetMapsRejectNullKeysAndValues() {
        Map<PropertyName, PropertyValue> nullPropertyKey = new LinkedHashMap<>();
        nullPropertyKey.put(null, new PropertyValue.StringValue("x"));
        Map<PropertyName, PropertyValue> nullPropertyValue = new LinkedHashMap<>();
        nullPropertyValue.put(new PropertyName("data"), null);
        Map<SlotName, WidgetSlot> nullSlotValue = new LinkedHashMap<>();
        nullSlotValue.put(new SlotName("child"), null);

        assertThrows(NullPointerException.class,
                () -> new WidgetNode(StableId.random(), new WidgetTypeId("A"), nullPropertyKey, Map.of()));
        assertThrows(NullPointerException.class,
                () -> new WidgetNode(StableId.random(), new WidgetTypeId("A"), nullPropertyValue, Map.of()));
        assertThrows(NullPointerException.class,
                () -> new WidgetNode(StableId.random(), new WidgetTypeId("A"), Map.of(), nullSlotValue));
    }

    @Test
    void singleSlotRepresentsExplicitJsonNullWithoutJavaNull() {
        WidgetNode child = WidgetNode.empty(StableId.random(), new WidgetTypeId("flutter.widgets.Text"));

        assertEquals(Optional.empty(), WidgetSlot.SingleSlot.empty().child());
        assertEquals(Optional.of(child), WidgetSlot.SingleSlot.of(child).child());
        assertEquals(SlotCardinality.SINGLE, WidgetSlot.SingleSlot.empty().cardinality());
        assertThrows(NullPointerException.class, () -> new WidgetSlot.SingleSlot(null));
        assertThrows(NullPointerException.class, () -> WidgetSlot.SingleSlot.of(null));
    }

    @Test
    void listSlotIsOrderedImmutableAndHonorsSchemaMaximum() {
        WidgetNode child = WidgetNode.empty(StableId.random(), new WidgetTypeId("flutter.widgets.Text"));
        ArrayList<WidgetNode> mutable = new ArrayList<>(List.of(child));
        WidgetSlot.ListSlot slot = new WidgetSlot.ListSlot(mutable);
        mutable.clear();

        assertEquals(List.of(child), slot.children());
        assertEquals(SlotCardinality.LIST, slot.cardinality());
        assertThrows(UnsupportedOperationException.class, () -> slot.children().clear());
        new WidgetSlot.ListSlot(Collections.nCopies(WidgetSlot.MAX_LIST_CHILDREN, child));
        assertThrows(IllegalArgumentException.class,
                () -> new WidgetSlot.ListSlot(
                        Collections.nCopies(WidgetSlot.MAX_LIST_CHILDREN + 1, child)));

        ArrayList<WidgetNode> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new WidgetSlot.ListSlot(withNull));
    }
}

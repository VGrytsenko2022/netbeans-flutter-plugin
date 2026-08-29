package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetNodePrototypeFactoryTest {
    private static final StableId ID = StableId.parse("26a92207-e622-43a3-ac53-211cf86a99e9");

    @Test
    void createsTextWithItsRequiredCreationDefault() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Text"), ID);

        assertEquals(ID, prototype.id());
        assertEquals(new WidgetTypeId("flutter.widgets.Text"), prototype.type());
        assertEquals(List.of(new PropertyName("data")), prototype.properties().keySet().stream().toList());
        assertEquals(new PropertyValue.StringValue("Text"),
                prototype.properties().get(new PropertyName("data")));
        assertTrue(prototype.slots().isEmpty());
        assertTrue(prototype.extensions().values().isEmpty());
    }

    @Test
    void createsColumnWithAnEmptyListSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Column"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("children")), prototype.slots().keySet().stream().toList());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("children")));
        assertTrue(children.children().isEmpty());
    }

    @Test
    void createsCenterWithAnEmptySingleSlot() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(definition("flutter.widgets.Center"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(new SlotName("child")), prototype.slots().keySet().stream().toList());
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                prototype.slots().get(new SlotName("child")));
        assertTrue(child.child().isEmpty());
    }

    @Test
    void createsAppBarWithoutMaterializingThemeDefaultsAndWithFiveEmptySlots() {
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.material.AppBar"), ID);

        assertTrue(prototype.properties().isEmpty());
        assertEquals(List.of(
                new SlotName("leading"),
                new SlotName("title"),
                new SlotName("actions"),
                new SlotName("flexibleSpace"),
                new SlotName("bottom")),
                prototype.slots().keySet().stream().toList());
        assertInstanceOf(WidgetSlot.ListSlot.class,
                prototype.slots().get(new SlotName("actions")));
        assertTrue(((WidgetSlot.ListSlot) prototype.slots()
                .get(new SlotName("actions"))).children().isEmpty());
        for (String slot : List.of("leading", "title", "flexibleSpace", "bottom")) {
            assertTrue(assertInstanceOf(
                    WidgetSlot.SingleSlot.class,
                    prototype.slots().get(new SlotName(slot))).child().isEmpty());
        }
    }

    @Test
    void sameDefinitionAndIdProduceEqualDetachedImmutablePrototypes() {
        WidgetDefinition definition = definition("flutter.widgets.Column");

        WidgetNode first = WidgetNodePrototypeFactory.create(definition, ID);
        WidgetNode second = WidgetNodePrototypeFactory.create(definition, ID);

        assertEquals(first, second);
        assertNotSame(first, second);
        assertNotSame(first.properties(), second.properties());
        assertNotSame(first.slots(), second.slots());
        assertNotSame(
                first.slots().get(new SlotName("children")),
                second.slots().get(new SlotName("children")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.properties().put(new PropertyName("extra"), new PropertyValue.StringValue("value")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.slots().clear());
        WidgetSlot.ListSlot children = assertInstanceOf(
                WidgetSlot.ListSlot.class,
                first.slots().get(new SlotName("children")));
        assertThrows(UnsupportedOperationException.class,
                () -> children.children().add(WidgetNode.empty(StableId.random(), definition.typeId())));
    }

    @Test
    void supplierIsInvokedExactlyOnceAndItsIdIsUsed() {
        AtomicInteger calls = new AtomicInteger();

        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                definition("flutter.widgets.Text"),
                () -> {
                    calls.incrementAndGet();
                    return ID;
                });

        assertEquals(1, calls.get());
        assertEquals(ID, prototype.id());
    }

    @Test
    void rejectsNullDefinitionBeforeInvokingSupplier() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<StableId> supplier = () -> {
            calls.incrementAndGet();
            return ID;
        };

        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(null, supplier));
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsNullIdSupplierAndNullSuppliedOrGeneratedIds() {
        WidgetDefinition definition = definition("flutter.widgets.Text");

        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, (StableId) null));
        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, (Supplier<StableId>) null));
        assertThrows(NullPointerException.class,
                () -> WidgetNodePrototypeFactory.create(definition, () -> null));
    }

    private static WidgetDefinition definition(String typeId) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(typeId))
                .orElseThrow();
    }
}

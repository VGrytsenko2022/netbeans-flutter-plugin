package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Creates detached immutable widget prototypes from exact catalog definitions. */
public final class WidgetNodePrototypeFactory {
    private WidgetNodePrototypeFactory() {
    }

    /**
     * Creates one prototype with the supplied stable identifier.
     *
     * <p>Only explicit catalog creation defaults are materialized. Every
     * declared slot is present and empty with the cardinality declared by its
     * {@link SlotDefinition}. A required slot with a positive minimum therefore
     * remains intentionally incomplete: the detached value is suitable as an
     * atomic wrapper payload, while document validation still rejects inserting
     * it without its required child.</p>
     *
     * @param definition exact immutable catalog definition
     * @param id stable identifier for the new widget
     * @return a detached immutable widget prototype
     */
    public static WidgetNode create(WidgetDefinition definition, StableId id) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(id, "id");

        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(definition.properties().size());
        for (PropertyDefinition property : definition.properties()) {
            property.creationDefault().ifPresent(value -> properties.put(property.name(), value));
        }

        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(definition.slots().size());
        for (SlotDefinition slot : definition.slots()) {
            WidgetSlot empty = switch (slot.cardinality()) {
                case SINGLE -> WidgetSlot.SingleSlot.empty();
                case LIST -> new WidgetSlot.ListSlot(List.of());
            };
            slots.put(slot.name(), empty);
        }

        return new WidgetNode(id, definition.typeId(), properties, slots);
    }

    /**
     * Creates one prototype using exactly one identifier supplied on demand.
     *
     * @param definition exact immutable catalog definition
     * @param idSupplier supplier invoked once for the new widget identifier
     * @return a detached immutable widget prototype
     */
    public static WidgetNode create(
            WidgetDefinition definition,
            Supplier<StableId> idSupplier) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(idSupplier, "idSupplier");
        StableId id = Objects.requireNonNull(idSupplier.get(), "idSupplier returned null");
        return create(definition, id);
    }
}

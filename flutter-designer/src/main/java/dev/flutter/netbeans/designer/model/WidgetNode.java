package dev.flutter.netbeans.designer.model;

import java.util.Map;
import java.util.Objects;

/** Immutable node in the semantic Flutter widget tree. */
public record WidgetNode(
        StableId id,
        WidgetTypeId type,
        Map<PropertyName, PropertyValue> properties,
        Map<SlotName, WidgetSlot> slots,
        Extensions extensions) {

    public WidgetNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        properties = ModelConstraints.immutableLinkedMap(properties, "properties");
        slots = ModelConstraints.immutableLinkedMap(slots, "slots");
        Objects.requireNonNull(extensions, "extensions");
    }

    public WidgetNode(
            StableId id,
            WidgetTypeId type,
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots) {
        this(id, type, properties, slots, Extensions.empty());
    }

    public static WidgetNode empty(StableId id, WidgetTypeId type) {
        return new WidgetNode(id, type, Map.of(), Map.of(), Extensions.empty());
    }
}

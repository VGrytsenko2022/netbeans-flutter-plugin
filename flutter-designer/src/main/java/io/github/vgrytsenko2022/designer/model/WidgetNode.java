package io.github.vgrytsenko2022.designer.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable node in the semantic Flutter widget tree. */
public record WidgetNode(
        StableId id,
        WidgetTypeId type,
        Map<PropertyName, PropertyValue> properties,
        Map<SlotName, WidgetSlot> slots,
        Extensions extensions,
        Optional<StateBinding> stateBinding,
        Map<PropertyName, StatePropertyBinding> propertyBindings) {

    public WidgetNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        properties = ModelConstraints.immutableLinkedMap(properties, "properties");
        slots = ModelConstraints.immutableLinkedMap(slots, "slots");
        Objects.requireNonNull(extensions, "extensions");
        Objects.requireNonNull(stateBinding, "stateBinding");
        propertyBindings = ModelConstraints.immutableLinkedMap(propertyBindings, "propertyBindings");
        if (propertyBindings.size() > 512) throw new IllegalArgumentException("Too many State property bindings");
    }

    public WidgetNode(StableId id, WidgetTypeId type, Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots, Extensions extensions, Optional<StateBinding> stateBinding) {
        this(id, type, properties, slots, extensions, stateBinding, Map.of());
    }

    public WidgetNode(
            StableId id,
            WidgetTypeId type,
            Map<PropertyName, PropertyValue> properties,
            Map<SlotName, WidgetSlot> slots,
            Extensions extensions) {
        this(id, type, properties, slots, extensions, Optional.empty());
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

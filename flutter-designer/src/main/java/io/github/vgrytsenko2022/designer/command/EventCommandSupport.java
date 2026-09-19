package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetDefinition;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.events.WidgetEventDescriptor;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import java.util.ArrayDeque;
import java.util.Optional;

/** Shared bounded semantic lookup; source edits are performed by the proved transition API. */
final class EventCommandSupport {
    private EventCommandSupport() { }

    static WidgetNode widget(WidgetNode root, StableId id) {
        ArrayDeque<WidgetNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            WidgetNode node = pending.pop();
            if (node.id().equals(id)) return node;
            for (WidgetSlot slot : node.slots().values()) {
                if (slot instanceof WidgetSlot.SingleSlot single) single.child().ifPresent(pending::push);
                else pending.addAll(((WidgetSlot.ListSlot) slot).children());
            }
        }
        throw new IllegalArgumentException("The event widget no longer exists: " + id);
    }

    static WidgetEventDescriptor event(WidgetCatalog catalog, WidgetNode widget, PropertyName event) {
        WidgetDefinition definition = catalog.find(widget.type()).orElseThrow();
        return WidgetEventCatalog.eventsFor(definition).stream()
                .filter(value -> value.propertyName().equals(event))
                .filter(WidgetEventDescriptor::supportsHandlerActions)
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Widget '" + widget.type().value() + "' has no admitted handler action for '" + event.value() + "'."));
    }

    static Optional<String> localHandler(PropertyValue value) {
        if (value instanceof PropertyValue.CallbackValue callback) return Optional.of(callback.handler());
        if (value instanceof PropertyValue.DartObjectReferenceValue reference
                && reference.libraryUri().isEmpty() && reference.member().isEmpty()
                && reference.access() == PropertyValue.DartObjectReferenceValue.Access.REFERENCE) {
            return Optional.of(reference.rootSymbol());
        }
        return Optional.empty();
    }
}

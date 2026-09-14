package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Shared implementation behind explicitly reviewed interaction wrapper actions. */
final class FlutterInteractionWrapPlanner {
    private static final SlotName CHILD = new SlotName("child");
    private FlutterInteractionWrapPlanner() { }

    static Result plan(DesignerDocument document, WidgetCatalog catalog, StableId targetId,
            Supplier<StableId> ids, WidgetTypeId wrapperType, String label) {
        Objects.requireNonNull(document); Objects.requireNonNull(catalog); Objects.requireNonNull(targetId); Objects.requireNonNull(ids);
        var validator = new WidgetTreeValidator();
        var baseline = validator.validate(document, catalog);
        if (!baseline.valid()) return rejected("The current widget tree is not valid: " + baseline.errors().getFirst().message());
        var definition = catalog.find(wrapperType);
        if (definition.isEmpty()) return rejected(label + " is unavailable in the current catalog.");
        WidgetNode target = null;
        var pending = new ArrayDeque<WidgetNode>(); pending.add(document.root());
        var used = new java.util.HashSet<StableId>();
        while (!pending.isEmpty()) {
            WidgetNode current = pending.removeFirst(); used.add(current.id());
            if (current.id().equals(targetId)) target = current;
            current.slots().values().forEach(slot -> {
                if (slot instanceof WidgetSlot.SingleSlot single) single.child().ifPresent(pending::addLast);
                else if (slot instanceof WidgetSlot.ListSlot list) pending.addAll(list.children());
            });
        }
        if (target == null) return rejected("Selected widget " + targetId + " is no longer present.");
        StableId id;
        try { id = ids.get(); }
        catch (RuntimeException failure) { return rejected("Cannot allocate a " + label + " identity: " + failure.getMessage()); }
        if (id == null || used.contains(id)) return rejected("The new " + label + " identity is missing or already used.");
        WidgetNode empty = WidgetNodePrototypeFactory.create(definition.orElseThrow(), id);
        var slots = new LinkedHashMap<>(empty.slots()); slots.put(CHILD, WidgetSlot.SingleSlot.of(target));
        WidgetNode wrapper = new WidgetNode(id, empty.type(), empty.properties(), slots, empty.extensions(), empty.stateBinding(), empty.propertyBindings());
        WidgetNode root = replace(document.root(), targetId, wrapper);
        var prospective = new DesignerDocument(document.schemaReference(), document.documentId(), document.source(),
                document.canvas(), root, document.extensions());
        var validation = validator.validate(prospective, catalog);
        if (!validation.valid()) return rejected("Cannot wrap widget " + targetId + " with " + label + ": "
                + validation.errors().getFirst().message());
        return new Result(Optional.of(new WrapWidget(targetId, empty, CHILD, 0)), "");
    }

    private static WidgetNode replace(WidgetNode node, StableId target, WidgetNode wrapper) {
        if (node.id().equals(target)) return wrapper;
        var slots = new LinkedHashMap<SlotName, WidgetSlot>();
        node.slots().forEach((name, slot) -> slots.put(name,
                slot instanceof WidgetSlot.SingleSlot single ? new WidgetSlot.SingleSlot(single.child().map(child -> replace(child, target, wrapper)))
                        : new WidgetSlot.ListSlot(((WidgetSlot.ListSlot) slot).children().stream().map(child -> replace(child, target, wrapper)).toList())));
        return new WidgetNode(node.id(), node.type(), node.properties(), slots, node.extensions(), node.stateBinding(), node.propertyBindings());
    }

    private static Result rejected(String reason) { return new Result(Optional.empty(), reason); }
    record Result(Optional<WrapWidget> command, String reason) { }
}

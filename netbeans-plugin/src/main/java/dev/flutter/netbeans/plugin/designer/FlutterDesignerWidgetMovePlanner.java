package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
import dev.flutter.netbeans.designer.command.MoveWidget;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure catalog-backed planner for moving an existing widget in the flattened
 * Swing tree.
 *
 * <p>An {@link On} target derives a destination only when exactly one catalog
 * slot accepts the source widget. An {@link Insert} target uses the same child
 * index exposed by the flattened widget tree: indexes below the child count
 * mean before that child, while the child count means after the last child.
 * The resulting {@link WidgetPlacement} index always addresses the destination
 * after the source subtree has been removed, as required by {@link MoveWidget}.
 * The planner performs no Swing, session, persistence or model mutation.</p>
 */
public final class FlutterDesignerWidgetMovePlanner {
    /** Plans one move against an immutable document snapshot. */
    public Result plan(
            DesignerDocument document,
            WidgetCatalog catalog,
            StableId sourceId,
            Target target) {
        if (document == null || catalog == null || sourceId == null || target == null) {
            return rejected(
                    RejectionCode.INVALID_REQUEST,
                    "Designer document, catalog, source widget and move target are required.");
        }

        TreeInventory inventory = inventory(document.root());
        if (inventory.duplicateId().isPresent()) {
            return rejected(
                    RejectionCode.DOCUMENT_ID_CONFLICT,
                    "Designer document contains duplicate widget id '"
                    + inventory.duplicateId().orElseThrow() + "'.");
        }

        NodeRef source = inventory.nodes().get(sourceId);
        if (source == null) {
            return rejected(
                    RejectionCode.SOURCE_NOT_FOUND,
                    "Move source '" + sourceId + "' does not exist in the designer document.");
        }
        if (source.parentId() == null) {
            return rejected(
                    RejectionCode.ROOT_MOVE_FORBIDDEN,
                    "The designer document root cannot be moved.");
        }

        Optional<WidgetDefinition> sourceLookup = catalog.find(source.node().type());
        if (sourceLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SOURCE_DEFINITION_MISSING,
                    "Catalog has no definition for move source type '"
                    + source.node().type().value() + "'.");
        }
        WidgetDefinition sourceDefinition = sourceLookup.orElseThrow();

        return switch (target) {
            case On on -> planOn(
                    inventory, catalog, source, sourceDefinition, on);
            case Insert insert -> planInsert(
                    inventory, catalog, source, sourceDefinition, insert);
            case IntoSlot exact -> planIntoSlot(
                    inventory, catalog, source, sourceDefinition, exact);
        };
    }

    private static Result planIntoSlot(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            WidgetDefinition sourceDefinition,
            IntoSlot target) {
        TargetContext context = targetContext(
                inventory, catalog, source, target.parentId());
        if (context.rejection() != null) {
            return context.rejection();
        }

        Optional<SlotDefinition> slotLookup = context.definition()
                .slot(target.slotName());
        if (slotLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SLOT_DEFINITION_MISSING,
                    "Catalog definition '" + context.definition().typeId().value()
                    + "' has no explicit move slot '"
                    + target.slotName().value() + "'.");
        }
        return planPlacement(
                inventory,
                catalog,
                source,
                sourceDefinition,
                context.parent(),
                context.definition(),
                slotLookup.orElseThrow(),
                target.postRemovalIndex());
    }

    private static Result planOn(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            WidgetDefinition sourceDefinition,
            On target) {
        TargetContext context = targetContext(
                inventory, catalog, source, target.containerId());
        if (context.rejection() != null) {
            return context.rejection();
        }

        WidgetDefinition parentDefinition = context.definition();
        List<SlotDefinition> compatible = parentDefinition.slots().stream()
                .filter(slot -> BadgeWidgetPropertySchema.slotUnavailableReason(context.parent().node(), slot.name()).isEmpty())
                .filter(slot -> WidgetPlacementRules.accepts(
                        parentDefinition, slot, sourceDefinition))
                .toList();
        if (compatible.isEmpty()) {
            if (isDirectFlexChild(sourceDefinition)) {
                String widgetName = sourceDefinition.palette().displayName();
                return rejected(
                        RejectionCode.NO_COMPATIBLE_DESTINATION,
                        "Cannot move " + widgetName + " '" + source.node().id()
                        + "' onto '"
                        + parentDefinition.typeId().value()
                        + "': " + widgetName
                        + " must remain a direct child of Row.children "
                        + "or Column.children.");
            }
            return rejected(
                    RejectionCode.NO_COMPATIBLE_DESTINATION,
                    "Widget '" + target.containerId()
                    + "' has no catalog slot compatible with source type '"
                    + sourceDefinition.typeId().value() + "'.");
        }
        if (compatible.size() > 1) {
            String names = compatible.stream()
                    .map(slot -> slot.name().value())
                    .collect(Collectors.joining(", "));
            return rejected(
                    RejectionCode.AMBIGUOUS_DESTINATION,
                    "Widget '" + target.containerId()
                    + "' has multiple catalog-compatible move slots ("
                    + names + "); moving onto the parent is ambiguous.");
        }

        SlotDefinition slot = compatible.getFirst();
        int currentCount = childCount(context.parent().node().slots().get(slot.name()));
        boolean sameSlot = isSameSlot(source, context.parent(), slot.name());
        int destinationIndex = slot.cardinality() == SlotCardinality.LIST
                ? currentCount - (sameSlot ? 1 : 0) : 0;
        return planPlacement(
                inventory,
                catalog,
                source,
                sourceDefinition,
                context.parent(),
                parentDefinition,
                slot,
                destinationIndex);
    }

    private static Result planInsert(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            WidgetDefinition sourceDefinition,
            Insert target) {
        TargetContext context = targetContext(
                inventory, catalog, source, target.flattenedParentId());
        if (context.rejection() != null) {
            return context.rejection();
        }

        List<FlattenedChild> children = flattenedChildren(context.parent().node());
        int requestedIndex = target.flattenedChildIndex();
        if (requestedIndex < 0 || requestedIndex > children.size()) {
            return rejected(
                    RejectionCode.INSERT_INDEX_OUT_OF_BOUNDS,
                    "Flattened insertion index " + requestedIndex + " is outside 0.."
                    + children.size() + " for parent '"
                    + target.flattenedParentId() + "'.");
        }
        if (children.isEmpty()) {
            return rejected(
                    RejectionCode.AMBIGUOUS_DESTINATION,
                    "An empty flattened parent has no child anchor from which to derive "
                    + "a semantic list slot; use an on-container target instead.");
        }

        FlattenedChild anchor;
        int preRemovalBoundary;
        if (requestedIndex < children.size()) {
            anchor = children.get(requestedIndex);
            preRemovalBoundary = anchor.slotIndex();
        } else {
            anchor = children.getLast();
            preRemovalBoundary = anchor.slotIndex() + 1;
        }

        Optional<SlotDefinition> slotLookup = context.definition().slot(anchor.slotName());
        if (slotLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SLOT_DEFINITION_MISSING,
                    "Catalog definition '" + context.definition().typeId().value()
                    + "' has no flattened anchor slot '" + anchor.slotName().value() + "'.");
        }
        SlotDefinition slot = slotLookup.orElseThrow();
        WidgetSlot anchoredModelSlot = context.parent().node().slots()
                .get(anchor.slotName());
        if (anchoredModelSlot == null
                || anchoredModelSlot.cardinality() != slot.cardinality()) {
            return rejected(
                    RejectionCode.SLOT_CARDINALITY_MISMATCH,
                    "Flattened anchor slot '" + context.parent().node().id() + '.'
                    + anchor.slotName().value()
                    + "' disagrees with its catalog cardinality.");
        }
        if (slot.cardinality() != SlotCardinality.LIST) {
            return rejected(
                    RejectionCode.INSERT_REQUIRES_LIST_SLOT,
                    "Flattened insertion beside child '" + anchor.childId()
                    + "' resolves to single slot '" + anchor.slotName().value()
                    + "'; before/after moves require a list slot.");
        }

        boolean sameSlot = isSameSlot(source, context.parent(), anchor.slotName());
        int destinationIndex = preRemovalBoundary;
        if (sameSlot && source.slotIndex() < preRemovalBoundary) {
            destinationIndex--;
        }
        return planPlacement(
                inventory,
                catalog,
                source,
                sourceDefinition,
                context.parent(),
                context.definition(),
                slot,
                destinationIndex);
    }

    private static TargetContext targetContext(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            StableId parentId) {
        NodeRef parent = inventory.nodes().get(parentId);
        if (parent == null) {
            return TargetContext.failed(rejected(
                    RejectionCode.TARGET_NOT_FOUND,
                    "Move target parent '" + parentId
                    + "' does not exist in the designer document."));
        }
        if (isInsideSourceSubtree(inventory.nodes(), source.node().id(), parentId)) {
            return TargetContext.failed(rejected(
                    RejectionCode.DESTINATION_INSIDE_SUBTREE,
                    "Move target parent '" + parentId
                    + "' is the source widget or lies inside its subtree."));
        }

        Optional<WidgetDefinition> definitionLookup = catalog.find(parent.node().type());
        if (definitionLookup.isEmpty()) {
            return TargetContext.failed(rejected(
                    RejectionCode.TARGET_DEFINITION_MISSING,
                    "Catalog has no definition for move target type '"
                    + parent.node().type().value() + "'."));
        }
        return TargetContext.accepted(parent, definitionLookup.orElseThrow());
    }

    private static Result planPlacement(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            WidgetDefinition sourceDefinition,
            NodeRef parent,
            WidgetDefinition parentDefinition,
            SlotDefinition slot,
            int destinationIndex) {
        Optional<String> unavailable = BadgeWidgetPropertySchema.slotUnavailableReason(parent.node(), slot.name());
        if (unavailable.isPresent()) {
            return rejected(RejectionCode.SLOT_REJECTS_WIDGET,
                    "Cannot move " + sourceDefinition.palette().displayName() + " '" + source.node().id()
                            + "' to Badge '" + parent.node().id() + "." + slot.name().value()
                            + "': " + unavailable.orElseThrow());
        }
        if (!WidgetPlacementRules.accepts(
                parentDefinition, slot, sourceDefinition)) {
            if (isDirectFlexChild(sourceDefinition)) {
                String widgetName = sourceDefinition.palette().displayName();
                return rejected(
                        RejectionCode.SLOT_REJECTS_WIDGET,
                        "Cannot move " + widgetName + " '" + source.node().id()
                        + "' to '"
                        + parentDefinition.typeId().value() + '.'
                        + slot.name().value() + "': " + widgetName
                        + " must remain a direct "
                        + "child of Row.children or Column.children.");
            }
            return rejected(
                    RejectionCode.SLOT_REJECTS_WIDGET,
                    "Catalog slot '" + parentDefinition.typeId().value() + '.'
                    + slot.name().value() + "' rejects source type '"
                    + sourceDefinition.typeId().value() + "'.");
        }

        WidgetSlot modelSlot = parent.node().slots().get(slot.name());
        if (modelSlot != null && modelSlot.cardinality() != slot.cardinality()) {
            return rejected(
                    RejectionCode.SLOT_CARDINALITY_MISMATCH,
                    "Document slot '" + parent.node().id() + '.' + slot.name().value()
                    + "' is " + modelSlot.cardinality().wireName()
                    + "; catalog requires " + slot.cardinality().wireName() + ".");
        }

        boolean sameSlot = isSameSlot(source, parent, slot.name());
        Result sourceRemoval = validateSourceRemoval(
                inventory, catalog, source, sameSlot);
        if (sourceRemoval != null) {
            return sourceRemoval;
        }

        int currentCount = childCount(modelSlot);
        int postRemovalCount = currentCount - (sameSlot ? 1 : 0);
        if (postRemovalCount < 0) {
            return rejected(
                    RejectionCode.SLOT_CARDINALITY_MISMATCH,
                    "Move source is not represented consistently in destination slot '"
                    + parent.node().id() + '.' + slot.name().value() + "'.");
        }

        if (slot.cardinality() == SlotCardinality.SINGLE) {
            if (destinationIndex != 0) {
                return rejected(
                        RejectionCode.INSERT_INDEX_OUT_OF_BOUNDS,
                        "Single destination slot accepts only index 0; received "
                        + destinationIndex + ".");
            }
            if (postRemovalCount >= slot.maxChildren()) {
                return rejected(
                        RejectionCode.SLOT_FULL,
                        "Single destination slot '" + parent.node().id() + '.'
                        + slot.name().value() + "' is occupied or permits no child.");
            }
        } else {
            if (destinationIndex < 0 || destinationIndex > postRemovalCount) {
                return rejected(
                        RejectionCode.INSERT_INDEX_OUT_OF_BOUNDS,
                        "Post-removal list index " + destinationIndex + " is outside 0.."
                        + postRemovalCount + " for slot '" + parent.node().id() + '.'
                        + slot.name().value() + "'.");
            }
            int maximum = Math.min(slot.maxChildren(), WidgetSlot.MAX_LIST_CHILDREN);
            if (postRemovalCount >= maximum) {
                return rejected(
                        RejectionCode.SLOT_FULL,
                        "Destination slot '" + parent.node().id() + '.'
                        + slot.name().value() + "' already contains " + postRemovalCount
                        + " children after source removal; its effective maximum is "
                        + maximum + ".");
            }
        }

        if (sameSlot && destinationIndex == source.slotIndex()) {
            return rejected(
                    RejectionCode.NO_CHANGE,
                    "Move keeps widget '" + source.node().id()
                    + "' at the same post-removal slot index.");
        }

        return new Accepted(new MoveWidget(
                source.node().id(),
                new WidgetPlacement(parent.node().id(), slot.name(), destinationIndex)));
    }

    private static Result validateSourceRemoval(
            TreeInventory inventory,
            WidgetCatalog catalog,
            NodeRef source,
            boolean sameSlot) {
        if (sameSlot) {
            return null;
        }

        NodeRef sourceParent = inventory.nodes().get(source.parentId());
        Optional<WidgetDefinition> parentLookup = catalog.find(sourceParent.node().type());
        if (parentLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SOURCE_PARENT_DEFINITION_MISSING,
                    "Catalog has no definition for source parent type '"
                    + sourceParent.node().type().value() + "'.");
        }
        Optional<SlotDefinition> sourceSlotLookup = parentLookup.orElseThrow()
                .slot(source.slotName());
        if (sourceSlotLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SOURCE_SLOT_DEFINITION_MISSING,
                    "Catalog definition '" + sourceParent.node().type().value()
                    + "' has no source slot '" + source.slotName().value() + "'.");
        }

        SlotDefinition sourceSlot = sourceSlotLookup.orElseThrow();
        WidgetSlot modelSlot = sourceParent.node().slots().get(source.slotName());
        if (modelSlot == null || modelSlot.cardinality() != sourceSlot.cardinality()) {
            return rejected(
                    RejectionCode.SLOT_CARDINALITY_MISMATCH,
                    "Source slot '" + sourceParent.node().id() + '.'
                    + source.slotName().value()
                    + "' disagrees with its catalog cardinality.");
        }
        int remaining = childCount(modelSlot) - 1;
        if (remaining < sourceSlot.minChildren()) {
            return rejected(
                    RejectionCode.SOURCE_SLOT_REQUIRED,
                    "Moving widget '" + source.node().id() + "' would leave source slot '"
                    + sourceParent.node().id() + '.' + source.slotName().value()
                    + "' with " + remaining + " children; its minimum is "
                    + sourceSlot.minChildren() + ".");
        }
        return null;
    }

    private static boolean isSameSlot(
            NodeRef source,
            NodeRef parent,
            SlotName slotName) {
        return parent.node().id().equals(source.parentId())
                && slotName.equals(source.slotName());
    }

    private static boolean isInsideSourceSubtree(
            Map<StableId, NodeRef> nodes,
            StableId sourceId,
            StableId candidateParentId) {
        StableId current = candidateParentId;
        while (current != null) {
            if (current.equals(sourceId)) {
                return true;
            }
            NodeRef ref = nodes.get(current);
            current = ref == null ? null : ref.parentId();
        }
        return false;
    }

    private static int childCount(WidgetSlot slot) {
        if (slot == null) {
            return 0;
        }
        if (slot instanceof WidgetSlot.SingleSlot single) {
            return single.child().isPresent() ? 1 : 0;
        }
        return ((WidgetSlot.ListSlot) slot).children().size();
    }

    private static List<FlattenedChild> flattenedChildren(WidgetNode parent) {
        ArrayList<FlattenedChild> result = new ArrayList<>();
        for (Map.Entry<SlotName, WidgetSlot> entry : parent.slots().entrySet()) {
            SlotName slotName = entry.getKey();
            WidgetSlot slot = entry.getValue();
            if (slot instanceof WidgetSlot.SingleSlot single) {
                single.child().ifPresent(child ->
                        result.add(new FlattenedChild(child.id(), slotName, 0)));
            } else {
                List<WidgetNode> children = ((WidgetSlot.ListSlot) slot).children();
                for (int index = 0; index < children.size(); index++) {
                    result.add(new FlattenedChild(
                            children.get(index).id(), slotName, index));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isDirectFlexChild(WidgetDefinition definition) {
        String type = definition.typeId().value();
        return WidgetPlacementRules.EXPANDED_TYPE.equals(type)
                || WidgetPlacementRules.FLEXIBLE_TYPE.equals(type)
                || WidgetPlacementRules.SPACER_TYPE.equals(type);
    }

    private static TreeInventory inventory(WidgetNode root) {
        HashMap<StableId, NodeRef> nodes = new HashMap<>();
        Set<StableId> expanded = new HashSet<>();
        StableId duplicate = null;
        ArrayDeque<NodeRef> pending = new ArrayDeque<>();
        pending.push(new NodeRef(root, null, null, -1));
        while (!pending.isEmpty()) {
            NodeRef ref = pending.pop();
            NodeRef previous = nodes.putIfAbsent(ref.node().id(), ref);
            if (previous != null && duplicate == null) {
                duplicate = ref.node().id();
            }
            if (!expanded.add(ref.node().id())) {
                continue;
            }
            for (Map.Entry<SlotName, WidgetSlot> entry
                    : ref.node().slots().entrySet()) {
                if (entry.getValue() instanceof WidgetSlot.SingleSlot single) {
                    single.child().ifPresent(child -> pending.push(new NodeRef(
                            child, ref.node().id(), entry.getKey(), 0)));
                } else {
                    List<WidgetNode> children = ((WidgetSlot.ListSlot) entry.getValue())
                            .children();
                    for (int index = children.size() - 1; index >= 0; index--) {
                        pending.push(new NodeRef(
                                children.get(index),
                                ref.node().id(),
                                entry.getKey(),
                                index));
                    }
                }
            }
        }
        return new TreeInventory(
                Map.copyOf(nodes), Optional.ofNullable(duplicate));
    }

    private static Rejected rejected(RejectionCode code, String reason) {
        return new Rejected(code, reason);
    }

    /** Semantic target derived from a Swing tree drop location. */
    public sealed interface Target permits On, Insert, IntoSlot {
    }

    /** Move onto a container and derive its unique catalog-compatible slot. */
    public record On(StableId containerId) implements Target {
        public On {
            Objects.requireNonNull(containerId, "containerId");
        }
    }

    /**
     * Move at a flattened child boundary: before {@code child[index]}, or
     * after the last child when {@code index == childCount}.
     */
    public record Insert(
            StableId flattenedParentId,
            int flattenedChildIndex) implements Target {
        public Insert {
            Objects.requireNonNull(flattenedParentId, "flattenedParentId");
        }
    }

    /**
     * Exact semantic destination chosen by a named-slot UI.
     *
     * <p>The index addresses the destination after removing the source
     * subtree, matching {@link MoveWidget}. Unlike {@link On}, this target
     * never derives or guesses a slot from the flattened widget tree.</p>
     */
    public record IntoSlot(
            StableId parentId,
            SlotName slotName,
            int postRemovalIndex) implements Target {
        public IntoSlot {
            Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(slotName, "slotName");
        }
    }

    /** Typed result of pure move planning. */
    public sealed interface Result permits Accepted, Rejected {
    }

    /** A fully admitted immutable command ready for the command session. */
    public record Accepted(MoveWidget command) implements Result {
        public Accepted {
            Objects.requireNonNull(command, "command");
        }
    }

    /** Concrete fail-closed result suitable for status text and tests. */
    public record Rejected(RejectionCode code, String reason) implements Result {
        public Rejected {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }

    /** Stable machine-readable categories for rejected move targets. */
    public enum RejectionCode {
        INVALID_REQUEST,
        DOCUMENT_ID_CONFLICT,
        SOURCE_NOT_FOUND,
        ROOT_MOVE_FORBIDDEN,
        SOURCE_DEFINITION_MISSING,
        SOURCE_PARENT_DEFINITION_MISSING,
        SOURCE_SLOT_DEFINITION_MISSING,
        SOURCE_SLOT_REQUIRED,
        TARGET_NOT_FOUND,
        TARGET_DEFINITION_MISSING,
        DESTINATION_INSIDE_SUBTREE,
        NO_COMPATIBLE_DESTINATION,
        AMBIGUOUS_DESTINATION,
        SLOT_DEFINITION_MISSING,
        SLOT_CARDINALITY_MISMATCH,
        SLOT_REJECTS_WIDGET,
        INSERT_INDEX_OUT_OF_BOUNDS,
        INSERT_REQUIRES_LIST_SLOT,
        SLOT_FULL,
        NO_CHANGE
    }

    private record NodeRef(
            WidgetNode node,
            StableId parentId,
            SlotName slotName,
            int slotIndex) {
    }

    private record FlattenedChild(
            StableId childId,
            SlotName slotName,
            int slotIndex) {
    }

    private record TreeInventory(
            Map<StableId, NodeRef> nodes,
            Optional<StableId> duplicateId) {
    }

    private record TargetContext(
            NodeRef parent,
            WidgetDefinition definition,
            Rejected rejection) {
        private static TargetContext accepted(
                NodeRef parent,
                WidgetDefinition definition) {
            return new TargetContext(parent, definition, null);
        }

        private static TargetContext failed(Rejected rejection) {
            return new TargetContext(null, null, rejection);
        }
    }
}

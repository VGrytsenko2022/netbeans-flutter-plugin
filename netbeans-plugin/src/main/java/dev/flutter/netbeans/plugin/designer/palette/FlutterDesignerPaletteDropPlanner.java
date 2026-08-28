package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Pure semantic planner for the first Palette-to-Canvas drop slice.
 *
 * <p>The slice is intentionally narrow: an authoritative {@code Text} item
 * may only be appended to the terminal position of a {@code Row.children} or
 * {@code Column.children} list. The planner performs no UI, session or
 * protocol work and never mutates the supplied document.</p>
 */
public final class FlutterDesignerPaletteDropPlanner {
    private static final WidgetTypeId TEXT = new WidgetTypeId("flutter.widgets.Text");
    private static final WidgetTypeId COLUMN = new WidgetTypeId("flutter.widgets.Column");
    private static final WidgetTypeId ROW = new WidgetTypeId("flutter.widgets.Row");
    private static final Set<WidgetTypeId> SUPPORTED_PARENTS = Set.of(COLUMN, ROW);
    private static final SlotName CHILDREN = new SlotName("children");

    /** Plans one Palette drop without changing the document. */
    public Result plan(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            Supplier<StableId> stableIdSupplier) {
        Optional<Rejected> invalid = invalidInput(
                document,
                catalog,
                authoritativeWidgetType,
                parentId,
                slotName,
                insertionIndex,
                stableIdSupplier);
        if (invalid.isPresent()) {
            return invalid.orElseThrow();
        }

        if (!TEXT.equals(authoritativeWidgetType)) {
            return rejected(
                    RejectionCode.UNSUPPORTED_WIDGET_TYPE,
                    "Palette drop accepts only widget type '" + TEXT.value()
                    + "'; received '" + authoritativeWidgetType.value() + "'.");
        }

        Optional<WidgetDefinition> sourceLookup = catalog.find(authoritativeWidgetType);
        if (sourceLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SOURCE_DEFINITION_MISSING,
                    "Catalog has no definition for Palette widget type '"
                    + authoritativeWidgetType.value() + "'.");
        }
        WidgetDefinition sourceDefinition = sourceLookup.orElseThrow();
        if (!authoritativeWidgetType.equals(sourceDefinition.typeId())) {
            return rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Catalog resolved Palette widget type '"
                    + authoritativeWidgetType.value() + "' as '"
                    + sourceDefinition.typeId().value() + "'.");
        }

        TreeInventory inventory = inventory(document.root(), parentId);
        if (inventory.duplicateId().isPresent()) {
            return rejected(
                    RejectionCode.DOCUMENT_ID_CONFLICT,
                    "Designer document contains duplicate widget id '"
                    + inventory.duplicateId().orElseThrow() + "'.");
        }
        WidgetNode parent = inventory.parent();
        if (parent == null) {
            return rejected(
                    RejectionCode.PARENT_NOT_FOUND,
                    "Drop parent '" + parentId + "' does not exist in the designer document.");
        }
        if (!SUPPORTED_PARENTS.contains(parent.type())) {
            return rejected(
                    RejectionCode.UNSUPPORTED_PARENT,
                    "Palette Text may be dropped only into Row or Column; parent '"
                    + parentId + "' has type '" + parent.type().value() + "'.");
        }

        Optional<WidgetDefinition> parentLookup = catalog.find(parent.type());
        if (parentLookup.isEmpty()) {
            return rejected(
                    RejectionCode.PARENT_DEFINITION_MISSING,
                    "Catalog has no definition for drop parent type '"
                    + parent.type().value() + "'.");
        }
        WidgetDefinition parentDefinition = parentLookup.orElseThrow();
        if (!parent.type().equals(parentDefinition.typeId())) {
            return rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Catalog resolved parent type '" + parent.type().value()
                    + "' as '" + parentDefinition.typeId().value() + "'.");
        }

        if (!CHILDREN.equals(slotName)) {
            return rejected(
                    RejectionCode.UNSUPPORTED_SLOT,
                    "Palette Text may be dropped only into the 'children' slot; received '"
                    + slotName.value() + "'.");
        }
        Optional<SlotDefinition> slotLookup = parentDefinition.slot(slotName);
        if (slotLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SLOT_DEFINITION_MISSING,
                    "Catalog definition '" + parentDefinition.typeId().value()
                    + "' has no slot '" + slotName.value() + "'.");
        }
        SlotDefinition slotDefinition = slotLookup.orElseThrow();
        if (slotDefinition.cardinality() != SlotCardinality.LIST) {
            return rejected(
                    RejectionCode.CATALOG_SLOT_CARDINALITY_MISMATCH,
                    "Catalog slot '" + parentDefinition.typeId().value() + '.'
                    + slotName.value() + "' is "
                    + slotDefinition.cardinality().wireName() + "; a list slot is required.");
        }
        if (!slotDefinition.acceptance().accepts(sourceDefinition)) {
            return rejected(
                    RejectionCode.SLOT_REJECTS_WIDGET,
                    "Catalog slot '" + parentDefinition.typeId().value() + '.'
                    + slotName.value() + "' rejects widget type '"
                    + authoritativeWidgetType.value() + "'.");
        }

        WidgetSlot modelSlot = parent.slots().get(slotName);
        if (modelSlot != null && !(modelSlot instanceof WidgetSlot.ListSlot)) {
            return rejected(
                    RejectionCode.MODEL_SLOT_CARDINALITY_MISMATCH,
                    "Document slot '" + parent.id() + '.' + slotName.value()
                    + "' is " + modelSlot.cardinality().wireName()
                    + "; catalog requires a list slot.");
        }
        int currentChildren = modelSlot instanceof WidgetSlot.ListSlot list
                ? list.children().size() : 0;
        if (insertionIndex != currentChildren) {
            return rejected(
                    RejectionCode.NON_TERMINAL_INSERTION,
                    "The first Palette drop slice appends only at terminal index "
                    + currentChildren + " of '" + parent.id() + '.'
                    + slotName.value() + "'; received index " + insertionIndex + ".");
        }

        int maximumChildren = Math.min(
                slotDefinition.maxChildren(), WidgetSlot.MAX_LIST_CHILDREN);
        if (currentChildren >= maximumChildren) {
            return rejected(
                    RejectionCode.SLOT_FULL,
                    "Drop slot '" + parent.id() + '.' + slotName.value()
                    + "' already contains " + currentChildren
                    + " children; its effective maximum is " + maximumChildren + ".");
        }

        StableId newId;
        try {
            newId = stableIdSupplier.get();
        } catch (RuntimeException allocationFailure) {
            return rejected(
                    RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                    "Stable id allocation for Palette widget '"
                    + authoritativeWidgetType.value() + "' failed: "
                    + concreteMessage(allocationFailure) + '.');
        }
        if (newId == null) {
            return rejected(
                    RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                    "Stable id allocation for Palette widget '"
                    + authoritativeWidgetType.value() + "' returned null.");
        }
        if (inventory.ids().contains(newId)) {
            return rejected(
                    RejectionCode.STABLE_ID_CONFLICT,
                    "Allocated widget id '" + newId
                    + "' already exists in the designer document.");
        }

        WidgetNode child;
        try {
            child = WidgetNodePrototypeFactory.create(sourceDefinition, newId);
        } catch (RuntimeException invalidDefinition) {
            return rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Catalog definition for Palette widget type '"
                    + authoritativeWidgetType.value()
                    + "' cannot create a prototype: "
                    + concreteMessage(invalidDefinition) + '.');
        }
        return new Accepted(new AddWidget(
                new WidgetPlacement(parentId, slotName, insertionIndex),
                child));
    }

    private static Optional<Rejected> invalidInput(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            Supplier<StableId> stableIdSupplier) {
        if (document == null) {
            return Optional.of(invalid("Designer document is null."));
        }
        if (catalog == null) {
            return Optional.of(invalid("Widget catalog is null."));
        }
        if (authoritativeWidgetType == null) {
            return Optional.of(invalid("Authoritative Palette widget type is null."));
        }
        if (parentId == null) {
            return Optional.of(invalid("Drop parent id is null."));
        }
        if (slotName == null) {
            return Optional.of(invalid("Drop slot name is null."));
        }
        if (insertionIndex < 0) {
            return Optional.of(invalid(
                    "Drop insertion index must be non-negative; received "
                    + insertionIndex + '.'));
        }
        if (stableIdSupplier == null) {
            return Optional.of(invalid("Stable id supplier is null."));
        }
        return Optional.empty();
    }

    private static Rejected invalid(String reason) {
        return rejected(RejectionCode.INVALID_REQUEST, reason);
    }

    private static Rejected rejected(RejectionCode code, String reason) {
        return new Rejected(code, reason);
    }

    private static TreeInventory inventory(WidgetNode root, StableId parentId) {
        Set<StableId> ids = new HashSet<>();
        StableId duplicate = null;
        WidgetNode parent = null;
        ArrayDeque<WidgetNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            WidgetNode node = pending.pop();
            if (!ids.add(node.id()) && duplicate == null) {
                duplicate = node.id();
            }
            if (node.id().equals(parentId) && parent == null) {
                parent = node;
            }
            for (WidgetSlot slot : node.slots().values()) {
                if (slot instanceof WidgetSlot.SingleSlot single) {
                    single.child().ifPresent(pending::push);
                } else {
                    for (WidgetNode child : ((WidgetSlot.ListSlot) slot).children()) {
                        pending.push(child);
                    }
                }
            }
        }
        return new TreeInventory(Set.copyOf(ids), parent, Optional.ofNullable(duplicate));
    }

    private static String concreteMessage(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
    }

    /** Typed result of planning one Palette drop. */
    public sealed interface Result permits Accepted, Rejected {
    }

    /** A fully revalidated immutable command ready for the command session. */
    public record Accepted(AddWidget command) implements Result {
        public Accepted {
            Objects.requireNonNull(command, "command");
        }
    }

    /** A semantic rejection safe to present or log without throwing. */
    public record Rejected(RejectionCode code, String reason) implements Result {
        public Rejected {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }

    /** Stable machine-readable reason for a rejected first-slice drop. */
    public enum RejectionCode {
        INVALID_REQUEST,
        UNSUPPORTED_WIDGET_TYPE,
        SOURCE_DEFINITION_MISSING,
        PARENT_NOT_FOUND,
        UNSUPPORTED_PARENT,
        PARENT_DEFINITION_MISSING,
        UNSUPPORTED_SLOT,
        SLOT_DEFINITION_MISSING,
        CATALOG_SLOT_CARDINALITY_MISMATCH,
        MODEL_SLOT_CARDINALITY_MISMATCH,
        SLOT_REJECTS_WIDGET,
        NON_TERMINAL_INSERTION,
        SLOT_FULL,
        DOCUMENT_ID_CONFLICT,
        STABLE_ID_ALLOCATION_FAILED,
        STABLE_ID_CONFLICT,
        CATALOG_DEFINITION_MISMATCH
    }

    private record TreeInventory(
            Set<StableId> ids,
            WidgetNode parent,
            Optional<StableId> duplicateId) {
    }
}

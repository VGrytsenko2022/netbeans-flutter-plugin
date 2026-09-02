package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.command.WidgetPlacement;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Pure semantic planner for catalog-backed Palette-to-Canvas insertion.
 *
 * <p>Both source and destination authority come exclusively from the bound
 * catalog. A normal Palette widget may be appended to a list slot or inserted
 * into an empty single slot when the combined placement rules accept it.
 * Parent-restricted flex wrappers such as Expanded and Flexible instead
 * interpret the supplied Row/Column tuple as an existing child index and emit
 * one atomic wrapper command; they never create an empty required-child
 * placeholder. The planner performs no UI, session or protocol work and never
 * mutates the supplied document.</p>
 */
public final class FlutterDesignerPaletteDropPlanner {
    private static final WidgetTypeId EXPANDED_TYPE =
            new WidgetTypeId(WidgetPlacementRules.EXPANDED_TYPE);
    private static final WidgetTypeId FLEXIBLE_TYPE =
            new WidgetTypeId(WidgetPlacementRules.FLEXIBLE_TYPE);
    private static final WidgetTypeId SPACER_TYPE =
            new WidgetTypeId(WidgetPlacementRules.SPACER_TYPE);
    private static final Set<WidgetTypeId> FLEX_PARENT_DATA_TYPES =
            Set.of(EXPANDED_TYPE, FLEXIBLE_TYPE, SPACER_TYPE);
    private static final SlotName CHILD_SLOT = new SlotName("child");

    /** Plans one Palette drop without changing the document. */
    public Result plan(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            Supplier<StableId> stableIdSupplier) {
        return plan(
                document,
                catalog,
                authoritativeWidgetType,
                parentId,
                slotName,
                insertionIndex,
                FlutterImageAssetChoices.empty(),
                stableIdSupplier);
    }

    /** Plans one Palette drop using the current declared image inventory. */
    public Result plan(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            FlutterImageAssetChoices imageAssetChoices,
            Supplier<StableId> stableIdSupplier) {
        Optional<Rejected> invalid = invalidInput(
                document,
                catalog,
                authoritativeWidgetType,
                parentId,
                slotName,
                insertionIndex,
                imageAssetChoices,
                stableIdSupplier);
        if (invalid.isPresent()) {
            return invalid.orElseThrow();
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

        Optional<SlotDefinition> slotLookup = parentDefinition.slot(slotName);
        if (slotLookup.isEmpty()) {
            return rejected(
                    RejectionCode.SLOT_DEFINITION_MISSING,
                    "Catalog definition '" + parentDefinition.typeId().value()
                    + "' has no slot '" + slotName.value() + "'.");
        }
        SlotDefinition slotDefinition = slotLookup.orElseThrow();
        if (!WidgetPlacementRules.accepts(
                parentDefinition, slotDefinition, sourceDefinition)) {
            return rejected(
                    RejectionCode.SLOT_REJECTS_WIDGET,
                    placementRejection(parentDefinition, slotName, sourceDefinition));
        }

        WidgetSlot modelSlot = parent.slots().get(slotName);
        if (modelSlot != null
                && modelSlot.cardinality() != slotDefinition.cardinality()) {
            return rejected(
                    RejectionCode.MODEL_SLOT_CARDINALITY_MISMATCH,
                    "Document slot '" + parent.id() + '.' + slotName.value()
                    + "' is " + modelSlot.cardinality().wireName()
                    + "; catalog requires a "
                    + slotDefinition.cardinality().wireName() + " slot.");
        }
        if (WidgetPlacementRules.creationMode(sourceDefinition)
                == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
            return planFlexParentDataWrap(
                    inventory,
                    sourceDefinition,
                    parent,
                    parentDefinition,
                    slotDefinition,
                    modelSlot,
                    insertionIndex,
                    stableIdSupplier);
        }
        if (slotDefinition.cardinality() == SlotCardinality.SINGLE) {
            if (insertionIndex != 0) {
                return rejected(
                        RejectionCode.NON_TERMINAL_INSERTION,
                        "Single drop slot '" + parent.id() + '.'
                        + slotName.value() + "' accepts only index 0; received index "
                        + insertionIndex + ".");
            }
            boolean occupied = modelSlot instanceof WidgetSlot.SingleSlot single
                    && single.child().isPresent();
            if (occupied || slotDefinition.maxChildren() < 1) {
                return rejected(
                        RejectionCode.SLOT_FULL,
                        "Single drop slot '" + parent.id() + '.'
                        + slotName.value() + "' already contains a widget or does not "
                        + "permit a child.");
            }
        } else {
            int currentChildren = modelSlot instanceof WidgetSlot.ListSlot list
                    ? list.children().size() : 0;
            if (insertionIndex != currentChildren) {
                return rejected(
                        RejectionCode.NON_TERMINAL_INSERTION,
                        "Palette drop appends only at terminal index "
                        + currentChildren + " of '" + parent.id() + '.'
                        + slotName.value() + "'; received index "
                        + insertionIndex + ".");
            }

            int maximumChildren = Math.min(
                    slotDefinition.maxChildren(), WidgetSlot.MAX_LIST_CHILDREN);
            if (currentChildren >= maximumChildren) {
                return rejected(
                        RejectionCode.SLOT_FULL,
                        "Drop slot '" + parent.id() + '.' + slotName.value()
                        + "' already contains " + currentChildren
                        + " children; its effective maximum is "
                        + maximumChildren + ".");
            }
        }

        FlutterImageWidgetCreationValues.Result resolvedCreationValues =
                FlutterImageWidgetCreationValues.resolve(
                        sourceDefinition, imageAssetChoices);
        if (resolvedCreationValues
                instanceof FlutterImageWidgetCreationValues.Unavailable unavailable) {
            return rejected(
                    RejectionCode.REQUIRED_CREATION_VALUE_UNAVAILABLE,
                    unavailable.reason() + " Target: '" + parent.id() + '.'
                    + slotName.value() + "' at index " + insertionIndex + '.');
        }
        FlutterImageWidgetCreationValues.Available creationValues =
                (FlutterImageWidgetCreationValues.Available) resolvedCreationValues;

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
            child = WidgetNodePrototypeFactory.create(
                    sourceDefinition, newId, creationValues.creationValues());
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

    private static Result planFlexParentDataWrap(
            TreeInventory inventory,
            WidgetDefinition sourceDefinition,
            WidgetNode parent,
            WidgetDefinition parentDefinition,
            SlotDefinition slotDefinition,
            WidgetSlot modelSlot,
            int childIndex,
            Supplier<StableId> stableIdSupplier) {
        String wrapperName = sourceDefinition.palette().displayName();
        if (slotDefinition.cardinality() != SlotCardinality.LIST
                || !(modelSlot instanceof WidgetSlot.ListSlot list)) {
            return rejected(
                    RejectionCode.WRAP_TARGET_REQUIRED,
                    wrapperName + " requires an existing direct child to wrap in '"
                    + parentDefinition.typeId().value() + '.'
                    + slotDefinition.name().value() + "'.");
        }
        if (childIndex >= list.children().size()) {
            String reason = list.children().isEmpty()
                    ? wrapperName + " requires a child to wrap. "
                            + parentDefinition.palette().displayName() + " '"
                            + parent.id() + "'." + slotDefinition.name().value()
                            + " is empty; add a widget first, then drop " + wrapperName
                            + " on that child."
                    : wrapperName + " requires an existing child index in '"
                            + parent.id() + '.' + slotDefinition.name().value()
                            + "'; received " + childIndex + " for "
                            + list.children().size() + " children.";
            return rejected(RejectionCode.WRAP_TARGET_REQUIRED, reason);
        }

        WidgetNode target = list.children().get(childIndex);
        if (FLEX_PARENT_DATA_TYPES.contains(target.type())) {
            String targetName = flexParentDataDisplayName(target.type());
            return rejected(
                    RejectionCode.WRAP_TARGET_REJECTED,
                    "Cannot wrap " + targetName + " '" + target.id()
                    + "' with " + wrapperName + ": nesting would move the inner "
                    + targetName + " out "
                    + "of its required direct Row.children or Column.children parent.");
        }

        StableId newId;
        try {
            newId = stableIdSupplier.get();
        } catch (RuntimeException allocationFailure) {
            return rejected(
                    RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                    "Stable id allocation for " + wrapperName + " wrapper failed: "
                    + concreteMessage(allocationFailure) + '.');
        }
        if (newId == null) {
            return rejected(
                    RejectionCode.STABLE_ID_ALLOCATION_FAILED,
                    "Stable id allocation for " + wrapperName + " wrapper returned null.");
        }
        if (inventory.ids().contains(newId)) {
            return rejected(
                    RejectionCode.STABLE_ID_CONFLICT,
                    "Allocated " + wrapperName + " wrapper id '" + newId
                    + "' already exists in the designer document.");
        }

        WidgetNode wrapper;
        try {
            wrapper = WidgetNodePrototypeFactory.create(sourceDefinition, newId);
        } catch (RuntimeException invalidDefinition) {
            return rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Catalog definition for " + wrapperName
                    + " cannot create a wrapper prototype: "
                    + concreteMessage(invalidDefinition) + '.');
        }
        return new Wrapped(new WrapWidget(
                target.id(), wrapper, CHILD_SLOT, 0));
    }

    private static String flexParentDataDisplayName(WidgetTypeId type) {
        if (type.equals(EXPANDED_TYPE)) {
            return "Expanded";
        }
        if (type.equals(FLEXIBLE_TYPE)) {
            return "Flexible";
        }
        if (type.equals(SPACER_TYPE)) {
            return "Spacer";
        }
        throw new IllegalArgumentException(
                "Not a reviewed Flex parent-data widget: " + type.value());
    }

    private static String placementRejection(
            WidgetDefinition parentDefinition,
            SlotName slotName,
            WidgetDefinition sourceDefinition) {
        if (WidgetPlacementRules.creationMode(sourceDefinition)
                == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
            String wrapperName = sourceDefinition.palette().displayName();
            return "Cannot place " + wrapperName + " in '"
                    + parentDefinition.typeId().value() + '.' + slotName.value()
                    + "': " + wrapperName
                    + " must be created around an existing direct child "
                    + "of Row.children or Column.children.";
        }
        return "Catalog slot '" + parentDefinition.typeId().value() + '.'
                + slotName.value() + "' rejects widget type '"
                + sourceDefinition.typeId().value() + "'.";
    }

    private static Optional<Rejected> invalidInput(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            FlutterImageAssetChoices imageAssetChoices,
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
        if (imageAssetChoices == null) {
            return Optional.of(invalid(
                    "Current Flutter image asset choices are null."));
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
    public sealed interface Result permits Accepted, Wrapped, Rejected {
    }

    /** A fully revalidated immutable command ready for the command session. */
    public record Accepted(AddWidget command) implements Result {
        public Accepted {
            Objects.requireNonNull(command, "command");
        }
    }

    /** An atomic wrapper command around one existing direct flex child. */
    public record Wrapped(WrapWidget command) implements Result {
        public Wrapped {
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

    /** Stable machine-readable reason for a rejected catalog insertion. */
    public enum RejectionCode {
        INVALID_REQUEST,
        SOURCE_DEFINITION_MISSING,
        PARENT_NOT_FOUND,
        PARENT_DEFINITION_MISSING,
        SLOT_DEFINITION_MISSING,
        MODEL_SLOT_CARDINALITY_MISMATCH,
        SLOT_REJECTS_WIDGET,
        WRAP_TARGET_REQUIRED,
        WRAP_TARGET_REJECTED,
        NON_TERMINAL_INSERTION,
        SLOT_FULL,
        REQUIRED_CREATION_VALUE_UNAVAILABLE,
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

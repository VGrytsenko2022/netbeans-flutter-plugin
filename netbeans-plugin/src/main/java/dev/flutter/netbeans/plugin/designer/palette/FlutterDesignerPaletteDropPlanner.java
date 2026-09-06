package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
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
 * Required-child wrappers interpret the supplied parent/slot/index tuple as
 * one existing child and emit one atomic wrapper command; they never create an
 * empty required-child placeholder. Contextual placement rules still restrict
 * wrappers such as Expanded and Flexible to direct Row/Column children, while
 * ordinary wrappers such as SafeArea may wrap any compatible target. The
 * planner performs no UI, session or protocol work and never mutates the
 * supplied document.</p>
 */
public final class FlutterDesignerPaletteDropPlanner {
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

    /**
     * Plans one required-child wrapper around an exact existing tree target.
     * Unlike the parent/slot API, this form also supports wrapping the Designer
     * root because {@link WrapWidget} has an explicit root-safe contract.
     */
    public Result planWrapTarget(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId targetId,
            Supplier<StableId> stableIdSupplier) {
        return planWrapTarget(
                document,
                catalog,
                authoritativeWidgetType,
                targetId,
                FlutterImageAssetChoices.empty(),
                stableIdSupplier);
    }

    /** Plans an exact wrapper target using the current creation-value context. */
    public Result planWrapTarget(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId authoritativeWidgetType,
            StableId targetId,
            FlutterImageAssetChoices imageAssetChoices,
            Supplier<StableId> stableIdSupplier) {
        if (document == null
                || catalog == null
                || authoritativeWidgetType == null
                || targetId == null
                || imageAssetChoices == null
                || stableIdSupplier == null) {
            return rejected(
                    RejectionCode.INVALID_REQUEST,
                    "Designer document, catalog, wrapper type, existing target, "
                    + "creation-value context and stable-id supplier are required.");
        }
        Optional<WidgetDefinition> sourceLookup = catalog.find(
                authoritativeWidgetType);
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
        if (WidgetPlacementRules.creationMode(sourceDefinition)
                != WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
            return rejected(
                    RejectionCode.WRAP_TARGET_REJECTED,
                    sourceDefinition.palette().displayName()
                    + " is not a required-child Palette wrapper.");
        }

        TargetInventory inventory = targetInventory(document.root(), targetId);
        if (inventory.duplicateId().isPresent()) {
            return rejected(
                    RejectionCode.DOCUMENT_ID_CONFLICT,
                    "Designer document contains duplicate widget id '"
                    + inventory.duplicateId().orElseThrow() + "'.");
        }
        TargetRef target = inventory.target();
        if (target == null) {
            return rejected(
                    RejectionCode.PARENT_NOT_FOUND,
                    "Wrap target '" + targetId
                    + "' does not exist in the designer document.");
        }
        if (target.parentId() != null) {
            return plan(
                    document,
                    catalog,
                    authoritativeWidgetType,
                    target.parentId(),
                    target.slotName(),
                    target.slotIndex(),
                    imageAssetChoices,
                    stableIdSupplier);
        }

        WidgetPlacementRules.Decision rootPlacement =
                WidgetPlacementRules.evaluateRoot(sourceDefinition);
        if (!rootPlacement.accepted()) {
            return rejected(
                    RejectionCode.SLOT_REJECTS_WIDGET,
                    rootPlacement.reason());
        }
        Optional<Rejected> incompatibleTarget = incompatibleWrapTarget(
                catalog, sourceDefinition, target.node());
        if (incompatibleTarget.isPresent()) {
            return incompatibleTarget.orElseThrow();
        }
        return createWrap(
                inventory.ids(),
                sourceDefinition,
                target.node(),
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
        Optional<String> slotUnavailable = BadgeWidgetPropertySchema.slotUnavailableReason(parent, slotName)
                .or(() -> TextButtonWidgetPropertySchema.slotUnavailableReason(parent, slotName));
        if (slotUnavailable.isPresent()) {
            return rejected(RejectionCode.SLOT_REJECTS_WIDGET,
                    "Cannot add " + sourceDefinition.palette().displayName() + " to " + parentDefinition.palette().displayName() + " '"
                            + parent.id() + "." + slotName.value() + "': " + slotUnavailable.orElseThrow());
        }
        WidgetPlacementRules.Decision placement = WidgetPlacementRules.evaluate(
                parentDefinition, slotDefinition, sourceDefinition);
        if (!placement.accepted()) {
            return rejected(
                    RejectionCode.SLOT_REJECTS_WIDGET,
                    placementRejection(
                            parentDefinition, slotName, sourceDefinition, placement.reason()));
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
            return planExistingChildWrap(
                    inventory,
                    catalog,
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

    private static Result planExistingChildWrap(
            TreeInventory inventory,
            WidgetCatalog catalog,
            WidgetDefinition sourceDefinition,
            WidgetNode parent,
            WidgetDefinition parentDefinition,
            SlotDefinition slotDefinition,
            WidgetSlot modelSlot,
            int childIndex,
            Supplier<StableId> stableIdSupplier) {
        String wrapperName = sourceDefinition.palette().displayName();
        Optional<WidgetNode> target = existingChild(
                modelSlot, slotDefinition, childIndex);
        if (target.isEmpty()) {
            int count = switch (modelSlot) {
                case WidgetSlot.SingleSlot single ->
                    single.child().isPresent() ? 1 : 0;
                case WidgetSlot.ListSlot list -> list.children().size();
                case null -> 0;
            };
            String reason = count == 0
                    ? wrapperName + " requires a child to wrap. "
                            + parentDefinition.palette().displayName() + " '"
                            + parent.id() + "'." + slotDefinition.name().value()
                            + " is empty; add a widget first, then drop " + wrapperName
                            + " on that child."
                    : wrapperName + " requires an existing child index in '"
                            + parent.id() + '.' + slotDefinition.name().value()
                            + "'; received " + childIndex + " for " + count
                            + " children.";
            return rejected(RejectionCode.WRAP_TARGET_REQUIRED, reason);
        }

        Optional<Rejected> incompatibleTarget = incompatibleWrapTarget(
                catalog, sourceDefinition, target.orElseThrow());
        if (incompatibleTarget.isPresent()) {
            return incompatibleTarget.orElseThrow();
        }

        return createWrap(
                inventory.ids(),
                sourceDefinition,
                target.orElseThrow(),
                stableIdSupplier);
    }

    private static Result createWrap(
            Set<StableId> documentIds,
            WidgetDefinition sourceDefinition,
            WidgetNode target,
            Supplier<StableId> stableIdSupplier) {
        String wrapperName = sourceDefinition.palette().displayName();
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
        if (documentIds.contains(newId)) {
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

    private static Optional<WidgetNode> existingChild(
            WidgetSlot modelSlot,
            SlotDefinition slotDefinition,
            int childIndex) {
        if (slotDefinition.cardinality() == SlotCardinality.SINGLE) {
            if (childIndex != 0
                    || !(modelSlot instanceof WidgetSlot.SingleSlot single)) {
                return Optional.empty();
            }
            return single.child();
        }
        if (!(modelSlot instanceof WidgetSlot.ListSlot list)
                || childIndex >= list.children().size()) {
            return Optional.empty();
        }
        return Optional.of(list.children().get(childIndex));
    }

    private static Optional<Rejected> incompatibleWrapTarget(
            WidgetCatalog catalog,
            WidgetDefinition wrapperDefinition,
            WidgetNode target) {
        Optional<SlotDefinition> childSlotLookup =
                wrapperDefinition.slot(CHILD_SLOT);
        if (childSlotLookup.isEmpty()) {
            return Optional.of(rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Required-child Palette wrapper '"
                    + wrapperDefinition.typeId().value()
                    + "' has no child slot."));
        }
        SlotDefinition childSlot = childSlotLookup.orElseThrow();
        if (childSlot.cardinality() != SlotCardinality.SINGLE
                || childSlot.minChildren() != 1
                || childSlot.maxChildren() != 1) {
            return Optional.of(rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Required-child Palette wrapper '"
                    + wrapperDefinition.typeId().value()
                    + "' must declare child as one required single slot."));
        }
        Optional<WidgetDefinition> targetLookup = catalog.find(target.type());
        if (targetLookup.isEmpty()) {
            return Optional.of(rejected(
                    RejectionCode.CATALOG_DEFINITION_MISMATCH,
                    "Catalog has no definition for wrap target type '"
                    + target.type().value() + "'."));
        }
        WidgetDefinition targetDefinition = targetLookup.orElseThrow();
        WidgetPlacementRules.Decision innerPlacement =
                WidgetPlacementRules.evaluate(
                        wrapperDefinition, childSlot, targetDefinition);
        if (!innerPlacement.accepted()) {
            return Optional.of(rejected(
                    RejectionCode.WRAP_TARGET_REJECTED,
                    "Cannot wrap '" + target.type().value() + "' ('"
                    + target.id() + "') with "
                    + wrapperDefinition.palette().displayName() + ": "
                    + innerPlacement.reason()));
        }
        return Optional.empty();
    }

    private static String placementRejection(
            WidgetDefinition parentDefinition,
            SlotName slotName,
            WidgetDefinition sourceDefinition,
            String placementReason) {
        if (WidgetPlacementRules.creationMode(sourceDefinition)
                == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
            String wrapperName = sourceDefinition.palette().displayName();
            return "Cannot place " + wrapperName + " in '"
                    + parentDefinition.typeId().value() + '.' + slotName.value()
                    + "': " + placementReason;
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

    private static TargetInventory targetInventory(
            WidgetNode root,
            StableId targetId) {
        Set<StableId> ids = new HashSet<>();
        StableId duplicate = null;
        TargetRef target = null;
        ArrayDeque<TargetRef> pending = new ArrayDeque<>();
        pending.push(new TargetRef(root, null, null, -1));
        while (!pending.isEmpty()) {
            TargetRef ref = pending.pop();
            if (!ids.add(ref.node().id()) && duplicate == null) {
                duplicate = ref.node().id();
            }
            if (ref.node().id().equals(targetId) && target == null) {
                target = ref;
            }
            for (java.util.Map.Entry<SlotName, WidgetSlot> entry
                    : ref.node().slots().entrySet()) {
                if (entry.getValue() instanceof WidgetSlot.SingleSlot single) {
                    single.child().ifPresent(child -> pending.push(new TargetRef(
                            child, ref.node().id(), entry.getKey(), 0)));
                } else {
                    java.util.List<WidgetNode> children =
                            ((WidgetSlot.ListSlot) entry.getValue()).children();
                    for (int index = children.size() - 1; index >= 0; index--) {
                        pending.push(new TargetRef(
                                children.get(index),
                                ref.node().id(),
                                entry.getKey(),
                                index));
                    }
                }
            }
        }
        return new TargetInventory(
                Set.copyOf(ids), target, Optional.ofNullable(duplicate));
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

    private record TargetRef(
            WidgetNode node,
            StableId parentId,
            SlotName slotName,
            int slotIndex) {
    }

    private record TargetInventory(
            Set<StableId> ids,
            TargetRef target,
            Optional<StableId> duplicateId) {
    }
}

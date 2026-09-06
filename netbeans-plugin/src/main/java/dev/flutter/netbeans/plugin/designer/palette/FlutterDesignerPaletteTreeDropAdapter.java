package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.BadgeWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.TextButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.OutlinedButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FilledButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.command.WrapWidget;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Two-phase Palette-to-widget-tree drop admission.
 *
 * <p>The Swing tree supplies only a parent widget node, so this adapter derives
 * a destination exclusively from the current catalog compatibility matrix. A
 * target is previewable only when exactly one catalog slot is compatible and
 * that slot is currently available. List slots use their terminal index;
 * single slots must be empty. Leaves, occupied targets and catalog-ambiguous
 * multi-slot parents fail closed regardless of current occupancy.</p>
 *
 * <p>Preview is non-consuming and never allocates a durable widget id. Commit
 * re-reads the exact Transferable, consumes its local token once, resolves the
 * destination again against the latest immutable snapshot, and delegates the
 * final command to {@link FlutterDesignerPaletteDropPlanner}. No Swing view or
 * painting policy lives here. Required-child wrappers target one exact
 * existing widget (including the Designer root when placement permits), while
 * the prepared semantic destination stores that target's exact parent, slot
 * and index or a stable root sentinel.</p>
 */
public final class FlutterDesignerPaletteTreeDropAdapter {
    private static final SlotName CHILD_SLOT = new SlotName("child");
    private final FlutterDesignerPaletteDragLifecycle lifecycle;
    private final FlutterDesignerPaletteDropPlanner planner;

    /** Creates an adapter owned by the same Designer view as the lifecycle. */
    public FlutterDesignerPaletteTreeDropAdapter(
            FlutterDesignerPaletteDragLifecycle lifecycle) {
        this(lifecycle, new FlutterDesignerPaletteDropPlanner());
    }

    FlutterDesignerPaletteTreeDropAdapter(
            FlutterDesignerPaletteDragLifecycle lifecycle,
            FlutterDesignerPaletteDropPlanner planner) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.planner = Objects.requireNonNull(planner, "planner");
    }

    /**
     * Performs repeatable tree-hover admission without consuming drag
     * authority or allocating a widget id.
     */
    public PreviewResult preview(
            Transferable transferable,
            int action,
            DesignerDocument document,
            WidgetCatalog catalog,
            StableId parentId) {
        return preview(
                transferable,
                action,
                document,
                catalog,
                parentId,
                FlutterImageAssetChoices.empty());
    }

    /** Previews against the current declared image inventory. */
    public PreviewResult preview(
            Transferable transferable,
            int action,
            DesignerDocument document,
            WidgetCatalog catalog,
            StableId parentId,
            FlutterImageAssetChoices imageAssetChoices) {
        Optional<Rejected> invalid = invalidRequest(
                document, catalog, parentId, imageAssetChoices);
        if (invalid.isPresent()) {
            return invalid.orElseThrow();
        }
        if (!offersMove(action)) {
            return rejected(
                    RejectionCode.UNSUPPORTED_ACTION,
                    "Flutter Palette tree insertion requires the MOVE drag action.");
        }
        Optional<FlutterDesignerPaletteDragLifecycle.ResolvedDrag> resolved =
                lifecycle.resolve(transferable);
        if (resolved.isEmpty()) {
            return rejected(
                    RejectionCode.TOKEN_UNAVAILABLE,
                    "The Transferable has no live opaque Palette token owned by this Designer view.");
        }
        FlutterDesignerPaletteDragLifecycle.ResolvedDrag drag =
                resolved.orElseThrow();
        Optional<WidgetDefinition> resolvedSource = catalog.find(drag.widgetType())
                .filter(definition -> drag.widgetType().equals(definition.typeId()));
        boolean wrapExistingChild = resolvedSource.stream().anyMatch(definition ->
                WidgetPlacementRules.creationMode(definition)
                        == WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD);
        DestinationResult destination = wrapExistingChild
                ? resolveWrapDestination(
                        document, catalog, drag.widgetType(), parentId)
                : resolveUniqueDestination(
                        document, catalog, drag.widgetType(), parentId);
        if (destination instanceof DestinationRejected failure) {
            return failure.rejection();
        }
        Destination accepted = (Destination) destination;
        return new PreparedDrop(
                drag.token(),
                drag.widgetType(),
                accepted.parentId(),
                accepted.slotName(),
                accepted.insertionIndex(),
                parentId,
                accepted.wrapTargetId());
    }

    /**
     * Consumes and replans one prepared drop against the latest model snapshot.
     * A semantic rejection after token admission burns the token by design.
     */
    public CommitResult commit(
            PreparedDrop prepared,
            Transferable transferable,
            int action,
            DesignerDocument latestDocument,
            WidgetCatalog latestCatalog,
            Supplier<StableId> stableIdSupplier) {
        return commit(
                prepared,
                transferable,
                action,
                latestDocument,
                latestCatalog,
                FlutterImageAssetChoices.empty(),
                stableIdSupplier);
    }

    /** Commits against the latest declared image inventory. */
    public CommitResult commit(
            PreparedDrop prepared,
            Transferable transferable,
            int action,
            DesignerDocument latestDocument,
            WidgetCatalog latestCatalog,
            FlutterImageAssetChoices imageAssetChoices,
            Supplier<StableId> stableIdSupplier) {
        if (prepared == null
                || latestDocument == null
                || latestCatalog == null
                || imageAssetChoices == null
                || stableIdSupplier == null) {
            return rejected(
                    RejectionCode.INVALID_REQUEST,
                    "Prepared drop, latest document, catalog, current image asset "
                    + "choices and stable-id supplier are required.");
        }
        if (!offersMove(action)) {
            burn(prepared.token());
            return rejected(
                    RejectionCode.UNSUPPORTED_ACTION,
                    "Flutter Palette tree insertion requires the MOVE drag action.");
        }

        Optional<FlutterDesignerPaletteDragLifecycle.ResolvedDrag> live =
                lifecycle.resolve(transferable);
        if (live.isEmpty()) {
            burn(prepared.token());
            return rejected(
                    RejectionCode.TOKEN_UNAVAILABLE,
                    "The prepared Palette token is expired, revoked or already consumed.");
        }
        FlutterDesignerPaletteDragLifecycle.ResolvedDrag resolved =
                live.orElseThrow();
        if (!prepared.token().equals(resolved.token())
                || !prepared.widgetType().equals(
                        resolved.widgetType())) {
            burn(prepared.token());
            return rejected(
                    RejectionCode.TRANSFER_CHANGED,
                    "The committed Transferable no longer carries the exact prepared Palette token and type.");
        }

        Optional<WidgetTypeId> consumed = lifecycle.consume(prepared.token());
        if (consumed.isEmpty()
                || !prepared.widgetType().equals(consumed.orElseThrow())) {
            return rejected(
                    RejectionCode.TOKEN_UNAVAILABLE,
                    "The prepared Palette token is expired, revoked or already consumed.");
        }

        DestinationResult latestDestination = prepared.wrapTargetId().isPresent()
                ? resolveWrapDestination(
                        latestDocument,
                        latestCatalog,
                        prepared.widgetType(),
                        prepared.wrapTargetId().orElseThrow())
                : resolveUniqueDestination(
                        latestDocument,
                        latestCatalog,
                        prepared.widgetType(),
                        prepared.parentId());
        if (latestDestination instanceof DestinationRejected failure) {
            return failure.rejection();
        }
        Destination destination = (Destination) latestDestination;
        if (prepared.wrapTargetId().isPresent()
                && (!prepared.parentId().equals(destination.parentId())
                || !prepared.slotName().equals(destination.slotName())
                || prepared.insertionIndex() != destination.insertionIndex()
                || !prepared.wrapTargetId().equals(destination.wrapTargetId()))) {
            return rejected(
                    RejectionCode.TARGET_CHANGED,
                    "The prepared "
                    + wrapperDisplayName(latestCatalog, prepared.widgetType())
                    + " wrap target changed parent, slot or child index; "
                    + "retry against the current widget tree.");
        }
        FlutterDesignerPaletteDropPlanner.Result planned =
                prepared.wrapTargetId().isPresent()
                        ? planner.planWrapTarget(
                                latestDocument,
                                latestCatalog,
                                prepared.widgetType(),
                                prepared.wrapTargetId().orElseThrow(),
                                imageAssetChoices,
                                stableIdSupplier)
                        : planner.plan(
                                latestDocument,
                                latestCatalog,
                                prepared.widgetType(),
                                destination.parentId(),
                                destination.slotName(),
                                destination.insertionIndex(),
                                imageAssetChoices,
                                stableIdSupplier);
        if (planned instanceof FlutterDesignerPaletteDropPlanner.Accepted accepted) {
            return new Committed(accepted.command());
        }
        if (planned instanceof FlutterDesignerPaletteDropPlanner.Wrapped wrapped) {
            return new Wrapped(wrapped.command());
        }
        FlutterDesignerPaletteDropPlanner.Rejected failure =
                (FlutterDesignerPaletteDropPlanner.Rejected) planned;
        return rejected(
                RejectionCode.PLANNER_REJECTED,
                "Catalog drop planner rejected " + failure.code()
                + ": " + failure.reason());
    }

    private void burn(String token) {
        lifecycle.consume(token);
    }

    private static DestinationResult resolveUniqueDestination(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId widgetType,
            StableId parentId) {
        Optional<WidgetDefinition> sourceLookup = catalog.find(widgetType);
        if (sourceLookup.isEmpty()) {
            return destinationRejected(
                    RejectionCode.SOURCE_DEFINITION_MISSING,
                    "The current catalog has no definition for Palette widget type '"
                    + widgetType.value() + "'.");
        }
        WidgetDefinition source = sourceLookup.orElseThrow();
        if (!widgetType.equals(source.typeId())) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "The current catalog resolved the Palette type inconsistently.");
        }

        TreeInventory inventory = inventory(document.root(), parentId);
        if (inventory.duplicateId().isPresent()) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "The Designer document contains duplicate widget id '"
                    + inventory.duplicateId().orElseThrow() + "'.");
        }
        WidgetNode parent = inventory.parent();
        if (parent == null) {
            return destinationRejected(
                    RejectionCode.TARGET_NOT_FOUND,
                    "Tree drop parent '" + parentId
                    + "' is absent from the current Designer document.");
        }
        Optional<WidgetDefinition> parentLookup = catalog.find(parent.type());
        if (parentLookup.isEmpty()) {
            return destinationRejected(
                    RejectionCode.TARGET_DEFINITION_MISSING,
                    "The current catalog has no definition for tree drop parent type '"
                    + parent.type().value() + "'.");
        }
        WidgetDefinition parentDefinition = parentLookup.orElseThrow();
        if (!parent.type().equals(parentDefinition.typeId())) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "The current catalog resolved the tree drop parent inconsistently.");
        }

        List<SlotDefinition> compatibleSlots = parentDefinition.slots().stream()
                .filter(slot -> BadgeWidgetPropertySchema.slotUnavailableReason(parent, slot.name()).isEmpty())
                .filter(slot -> TextButtonWidgetPropertySchema.slotUnavailableReason(parent, slot.name()).isEmpty())
                .filter(slot -> OutlinedButtonWidgetPropertySchema.slotUnavailableReason(parent, slot.name()).isEmpty())
                .filter(slot -> FilledButtonWidgetPropertySchema.slotUnavailableReason(parent, slot.name()).isEmpty())
                .filter(slot -> FloatingActionButtonWidgetPropertySchema.slotUnavailableReason(parent, slot.name()).isEmpty())
                .filter(slot -> WidgetPlacementRules.accepts(
                        parentDefinition, slot, source))
                .toList();
        if (compatibleSlots.isEmpty()) {
            return destinationRejected(
                    RejectionCode.NO_COMPATIBLE_DESTINATION,
                    "Widget '" + parentId
                    + "' has no catalog slot compatible with Palette type '"
                    + widgetType.value() + "'.");
        }
        if (compatibleSlots.size() > 1) {
            String slots = compatibleSlots.stream()
                    .map(slot -> slot.name().value())
                    .collect(Collectors.joining(", "));
            return destinationRejected(
                    RejectionCode.AMBIGUOUS_DESTINATION,
                    "Widget '" + parentId + "' has multiple catalog-compatible "
                    + "drop slots (" + slots + "); dropping on the parent node "
                    + "is ambiguous regardless of current occupancy.");
        }

        SlotDefinition slot = compatibleSlots.getFirst();
        WidgetSlot modelSlot = parent.slots().get(slot.name());
        if (modelSlot != null
                && modelSlot.cardinality() != slot.cardinality()) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "Tree drop slot '" + parentId + '.' + slot.name().value()
                    + "' disagrees with its catalog cardinality.");
        }
        if (slot.cardinality() == SlotCardinality.SINGLE) {
            boolean occupied = modelSlot instanceof WidgetSlot.SingleSlot single
                    && single.child().isPresent();
            if (!occupied && slot.maxChildren() >= 1) {
                return new Destination(
                        parent.id(), slot.name(), 0, Optional.empty());
            }
        } else {
            int childCount = modelSlot instanceof WidgetSlot.ListSlot list
                    ? list.children().size() : 0;
            int maximum = Math.min(
                    slot.maxChildren(), WidgetSlot.MAX_LIST_CHILDREN);
            if (childCount < maximum) {
                return new Destination(
                        parent.id(), slot.name(), childCount, Optional.empty());
            }
        }
        return destinationRejected(
                RejectionCode.NO_COMPATIBLE_DESTINATION,
                "Widget '" + parentId + "' has no empty single slot or "
                + "terminal list position available for Palette type '"
                + widgetType.value() + "'.");
    }

    private static DestinationResult resolveWrapDestination(
            DesignerDocument document,
            WidgetCatalog catalog,
            WidgetTypeId widgetType,
            StableId targetChildId) {
        Optional<WidgetDefinition> sourceLookup = catalog.find(widgetType);
        if (sourceLookup.isEmpty()) {
            return destinationRejected(
                    RejectionCode.SOURCE_DEFINITION_MISSING,
                    "The current catalog has no definition for Palette widget type '"
                    + widgetType.value() + "'.");
        }
        WidgetDefinition source = sourceLookup.orElseThrow();
        String wrapperName = source.palette().displayName();
        if (WidgetPlacementRules.creationMode(source)
                != WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "Required-child wrapper admission received inconsistent Palette type '"
                    + source.typeId().value() + "'.");
        }

        NodeInventory inventory = nodeInventory(document.root());
        if (inventory.duplicateId().isPresent()) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "The Designer document contains duplicate widget id '"
                    + inventory.duplicateId().orElseThrow() + "'.");
        }
        NodeRef target = inventory.nodes().get(targetChildId);
        if (target == null) {
            return destinationRejected(
                    RejectionCode.TARGET_NOT_FOUND,
                    wrapperName + " wrap target '" + targetChildId
                    + "' is absent from the current Designer document.");
        }
        if (target.parentId() == null) {
            WidgetPlacementRules.Decision rootPlacement =
                    WidgetPlacementRules.evaluateRoot(source);
            if (!rootPlacement.accepted()) {
                return destinationRejected(
                        RejectionCode.NO_COMPATIBLE_DESTINATION,
                        "Cannot wrap root widget '" + targetChildId
                        + "' with " + wrapperName + ": "
                        + rootPlacement.reason());
            }
            Optional<String> incompatible = incompatibleWrapTarget(
                    catalog, source, target.node());
            if (incompatible.isPresent()) {
                return destinationRejected(
                        RejectionCode.NO_COMPATIBLE_DESTINATION,
                        incompatible.orElseThrow());
            }
            return new Destination(
                    targetChildId,
                    CHILD_SLOT,
                    0,
                    Optional.of(targetChildId));
        }
        NodeRef parent = inventory.nodes().get(target.parentId());
        if (parent == null || target.slotName() == null) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "Cannot resolve the direct model parent of " + wrapperName
                    + " wrap target '"
                    + targetChildId + "'.");
        }
        Optional<WidgetDefinition> parentLookup = catalog.find(parent.node().type());
        if (parentLookup.isEmpty()) {
            return destinationRejected(
                    RejectionCode.TARGET_DEFINITION_MISSING,
                    "The current catalog has no definition for direct parent type '"
                    + parent.node().type().value() + "'.");
        }
        WidgetDefinition parentDefinition = parentLookup.orElseThrow();
        Optional<SlotDefinition> slotLookup = parentDefinition.slot(target.slotName());
        if (slotLookup.isEmpty()) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    "Catalog definition '" + parentDefinition.typeId().value()
                    + "' has no direct slot '" + target.slotName().value() + "'.");
        }
        SlotDefinition slot = slotLookup.orElseThrow();
        if (!WidgetPlacementRules.accepts(parentDefinition, slot, source)) {
            return destinationRejected(
                    RejectionCode.NO_COMPATIBLE_DESTINATION,
                    "Cannot wrap '" + target.node().type().value() + "' ('"
                    + targetChildId + "') with " + wrapperName
                    + ": its direct model parent is '"
                    + parentDefinition.typeId().value() + '.'
                    + target.slotName().value()
                    + "', which rejects that wrapper type.");
        }
        WidgetSlot modelSlot = parent.node().slots().get(target.slotName());
        boolean exactTarget = switch (slot.cardinality()) {
            case SINGLE -> modelSlot instanceof WidgetSlot.SingleSlot single
                    && target.slotIndex() == 0
                    && single.child().map(WidgetNode::id)
                            .filter(targetChildId::equals).isPresent();
            case LIST -> modelSlot instanceof WidgetSlot.ListSlot list
                    && target.slotIndex() >= 0
                    && target.slotIndex() < list.children().size()
                    && list.children().get(target.slotIndex()).id().equals(targetChildId);
        };
        if (!exactTarget) {
            return destinationRejected(
                    RejectionCode.TARGET_STATE_INVALID,
                    wrapperName + " wrap target '" + targetChildId
                    + "' is not the exact direct child at '"
                    + parent.node().id() + '.' + target.slotName().value()
                    + "' index " + target.slotIndex() + '.');
        }
        Optional<String> incompatible = incompatibleWrapTarget(
                catalog, source, target.node());
        if (incompatible.isPresent()) {
            return destinationRejected(
                    RejectionCode.NO_COMPATIBLE_DESTINATION,
                    incompatible.orElseThrow());
        }
        return new Destination(
                parent.node().id(),
                target.slotName(),
                target.slotIndex(),
                Optional.of(targetChildId));
    }

    private static Optional<String> incompatibleWrapTarget(
            WidgetCatalog catalog,
            WidgetDefinition wrapper,
            WidgetNode target) {
        Optional<SlotDefinition> childSlotLookup = wrapper.slot(CHILD_SLOT);
        if (childSlotLookup.isEmpty()) {
            return Optional.of(
                    "Required-child Palette wrapper '" + wrapper.typeId().value()
                    + "' has no child slot.");
        }
        SlotDefinition childSlot = childSlotLookup.orElseThrow();
        if (childSlot.cardinality() != SlotCardinality.SINGLE
                || childSlot.minChildren() != 1
                || childSlot.maxChildren() != 1) {
            return Optional.of(
                    "Required-child Palette wrapper '" + wrapper.typeId().value()
                    + "' must declare child as one required single slot.");
        }
        Optional<WidgetDefinition> targetLookup = catalog.find(target.type());
        if (targetLookup.isEmpty()) {
            return Optional.of(
                    "The current catalog has no definition for wrap target type '"
                    + target.type().value() + "'.");
        }
        WidgetPlacementRules.Decision innerPlacement =
                WidgetPlacementRules.evaluate(
                        wrapper, childSlot, targetLookup.orElseThrow());
        if (!innerPlacement.accepted()) {
            return Optional.of(
                    "Cannot wrap '" + target.type().value() + "' ('"
                    + target.id() + "') with " + wrapper.palette().displayName()
                    + ": " + innerPlacement.reason());
        }
        return Optional.empty();
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
                    for (WidgetNode child
                            : ((WidgetSlot.ListSlot) slot).children()) {
                        pending.push(child);
                    }
                }
            }
        }
        return new TreeInventory(parent, Optional.ofNullable(duplicate));
    }

    private static Optional<Rejected> invalidRequest(
            DesignerDocument document,
            WidgetCatalog catalog,
            StableId parentId,
            FlutterImageAssetChoices imageAssetChoices) {
        if (document == null || catalog == null || parentId == null
                || imageAssetChoices == null) {
            return Optional.of(rejected(
                    RejectionCode.INVALID_REQUEST,
                    "Designer document, catalog, tree drop parent and current image "
                    + "asset choices are required."));
        }
        return Optional.empty();
    }

    private static boolean offersMove(int action) {
        return action == DnDConstants.ACTION_MOVE
                || action == DnDConstants.ACTION_COPY_OR_MOVE;
    }

    private static Rejected rejected(RejectionCode code, String reason) {
        return new Rejected(code, reason);
    }

    private static DestinationRejected destinationRejected(
            RejectionCode code,
            String reason) {
        return new DestinationRejected(rejected(code, reason));
    }

    /** Result of repeatable non-consuming drop preview. */
    public sealed interface PreviewResult permits PreparedDrop, Rejected {
    }

    /** Result of one consuming tree drop commit. */
    public sealed interface CommitResult permits Committed, Wrapped, Rejected {
    }

    /** Exact preview authority captured before NetBeans creates a PasteType. */
    public record PreparedDrop(
            String token,
            WidgetTypeId widgetType,
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            StableId treeTargetId,
            Optional<StableId> wrapTargetId) implements PreviewResult {
        public PreparedDrop {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(treeTargetId, "treeTargetId");
            Objects.requireNonNull(wrapTargetId, "wrapTargetId");
            if (token.isBlank()
                    || token.length()
                    != FlutterDesignerPaletteDragRegistry.TOKEN_LENGTH) {
                throw new IllegalArgumentException(
                        "prepared Palette token has an invalid bounded shape");
            }
            if (insertionIndex < 0
                    || insertionIndex > WidgetSlot.MAX_LIST_CHILDREN) {
                throw new IllegalArgumentException(
                        "prepared insertionIndex is outside its bound");
            }
            if (wrapTargetId.isPresent()
                    && !wrapTargetId.orElseThrow().equals(treeTargetId)) {
                throw new IllegalArgumentException(
                        "prepared wrap target must equal the exact tree target");
            }
        }
    }

    /** Exact AddWidget command admitted after latest-snapshot replanning. */
    public record Committed(AddWidget command) implements CommitResult {
        public Committed {
            Objects.requireNonNull(command, "command");
        }
    }

    /** Exact atomic generic required-child wrapper command admitted after replanning. */
    public record Wrapped(WrapWidget command) implements CommitResult {
        public Wrapped {
            Objects.requireNonNull(command, "command");
        }
    }

    /** Concrete fail-closed result safe for status text or diagnostics. */
    public record Rejected(
            RejectionCode code,
            String reason) implements PreviewResult, CommitResult {
        public Rejected {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }

    /** Stable rejection categories for Swing admission and focused tests. */
    public enum RejectionCode {
        INVALID_REQUEST,
        UNSUPPORTED_ACTION,
        TOKEN_UNAVAILABLE,
        TRANSFER_CHANGED,
        SOURCE_DEFINITION_MISSING,
        TARGET_NOT_FOUND,
        TARGET_DEFINITION_MISSING,
        TARGET_STATE_INVALID,
        TARGET_CHANGED,
        REQUIRED_CREATION_VALUE_UNAVAILABLE,
        NO_COMPATIBLE_DESTINATION,
        AMBIGUOUS_DESTINATION,
        PLANNER_REJECTED
    }

    private sealed interface DestinationResult
            permits Destination, DestinationRejected {
    }

    private record Destination(
            StableId parentId,
            SlotName slotName,
            int insertionIndex,
            Optional<StableId> wrapTargetId) implements DestinationResult {
        private Destination {
            Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(slotName, "slotName");
            Objects.requireNonNull(wrapTargetId, "wrapTargetId");
        }
    }

    private record DestinationRejected(
            Rejected rejection) implements DestinationResult {
        private DestinationRejected {
            Objects.requireNonNull(rejection, "rejection");
        }
    }

    private record TreeInventory(
            WidgetNode parent,
            Optional<StableId> duplicateId) {
        private TreeInventory {
            Objects.requireNonNull(duplicateId, "duplicateId");
        }
    }

    private static NodeInventory nodeInventory(WidgetNode root) {
        HashMap<StableId, NodeRef> nodes = new HashMap<>();
        Set<StableId> expanded = new HashSet<>();
        StableId duplicate = null;
        ArrayDeque<NodeRef> pending = new ArrayDeque<>();
        pending.push(new NodeRef(root, null, null, -1));
        while (!pending.isEmpty()) {
            NodeRef ref = pending.pop();
            if (nodes.putIfAbsent(ref.node().id(), ref) != null
                    && duplicate == null) {
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
                                children.get(index), ref.node().id(), entry.getKey(), index));
                    }
                }
            }
        }
        return new NodeInventory(
                Map.copyOf(nodes), Optional.ofNullable(duplicate));
    }

    private static String wrapperDisplayName(
            WidgetCatalog catalog,
            WidgetTypeId widgetType) {
        return catalog.find(widgetType)
                .map(WidgetDefinition::palette)
                .map(metadata -> metadata.displayName())
                .orElse(widgetType.value());
    }

    private record NodeRef(
            WidgetNode node,
            StableId parentId,
            SlotName slotName,
            int slotIndex) {
    }

    private record NodeInventory(
            Map<StableId, NodeRef> nodes,
            Optional<StableId> duplicateId) {
    }
}

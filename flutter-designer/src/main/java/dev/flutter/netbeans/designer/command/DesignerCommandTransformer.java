package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Package-private bounded semantic transformer used by command sessions. */
final class DesignerCommandTransformer {
    private final WidgetCatalog catalog;
    private final ValidationLimits limits;
    private final WidgetTreeValidator validator;

    DesignerCommandTransformer(
            WidgetCatalog catalog,
            ValidationLimits limits) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.validator = new WidgetTreeValidator(limits);
    }

    SemanticResult apply(DesignerDocument current, DesignerCommand command) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(command, "command");
        ValidationResult baseline = validator.validate(current, catalog);
        if (!baseline.valid()) {
            ValidationIssue issue = baseline.errors().getFirst();
            return failure(
                    statusForValidation(issue),
                    DesignerCommandDiagnosticCode.BASELINE_MODEL_INVALID,
                    issue.path(),
                    issue.widgetId(),
                    "The current command revision failed " + issue.code()
                    + ": " + issue.message());
        }

        TreeIndex index = TreeIndex.create(current.root(), limits.maxNodes());
        SemanticResult transformed = switch (command) {
            case AddWidget add -> add(current, index, add);
            case RemoveWidget remove -> remove(current, index, remove);
            case MoveWidget move -> move(current, index, move);
            case ReplaceSlotChild replace -> replaceSlotChild(
                    current, index, replace);
            case ClearSlotChildren clear -> clearSlotChildren(
                    current, index, clear);
            case WrapWidget wrap -> wrap(current, index, wrap);
            case SetProperty set -> setProperty(current, index, set);
            case ResetProperty reset -> resetProperty(current, index, reset);
            case PatchProperties patch -> patchProperties(current, index, patch);
        };
        if (transformed.status() != DesignerCommandStatus.APPLIED) {
            return transformed;
        }
        DesignerDocument candidate = transformed.document().orElseThrow();
        ValidationResult validation = validator.validate(candidate, catalog);
        if (!validation.valid()) {
            ValidationIssue issue = validation.errors().getFirst();
            return failure(
                    statusForValidation(issue),
                    DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    issue.path(),
                    issue.widgetId(),
                    "The command result failed " + issue.code()
                    + ": " + issue.message());
        }
        if (candidate.equals(current)) {
            return noChange("The command leaves the semantic document unchanged.");
        }
        return transformed;
    }

    private SemanticResult add(
            DesignerDocument current,
            TreeIndex index,
            AddWidget command) {
        Optional<DesignerCommandDiagnostic> subtree = validateInsertedSubtree(
                command.widget(), index.ids(), "/command/widget");
        if (subtree.isPresent()) {
            return failure(subtree.orElseThrow());
        }
        Insertion insertion = insert(
                current.root(),
                index,
                command.destination(),
                command.widget());
        return insertion.diagnostic().map(this::failure).orElseGet(() ->
            applied(withRoot(current, insertion.root().orElseThrow())));
    }

    private SemanticResult remove(
            DesignerDocument current,
            TreeIndex index,
            RemoveWidget command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        if (target.parentId().isEmpty()) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN,
                    "/root",
                    Optional.of(command.widgetId()),
                    "The required designer root cannot be removed.");
        }
        Removal removal = remove(current.root(), index, target);
        if (removal.diagnostic().isPresent()) {
            return failure(removal.diagnostic().orElseThrow());
        }
        return applied(withRoot(current, removal.root().orElseThrow()));
    }

    private SemanticResult move(
            DesignerDocument current,
            TreeIndex index,
            MoveWidget command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        if (target.parentId().isEmpty()) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN,
                    "/root",
                    Optional.of(command.widgetId()),
                    "The required designer root cannot be moved.");
        }
        Set<StableId> subtreeIds = collectIds(
                target.node(), limits.maxNodes()).ids();
        if (subtreeIds.contains(command.destination().parentId())) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.DESTINATION_INSIDE_SUBTREE,
                    target.path(),
                    Optional.of(command.widgetId()),
                    "Widget '" + command.widgetId()
                    + "' cannot be moved into its own subtree.");
        }

        boolean sameParentSlot = target.parentId().orElseThrow()
                        .equals(command.destination().parentId())
                && target.parentSlot().orElseThrow()
                        .equals(command.destination().slotName());
        Removal removal = remove(
                current.root(), index, target, !sameParentSlot);
        if (removal.diagnostic().isPresent()) {
            return failure(removal.diagnostic().orElseThrow());
        }
        WidgetNode withoutTarget = removal.root().orElseThrow();
        TreeIndex reduced = TreeIndex.create(withoutTarget, limits.maxNodes());
        Insertion insertion = insert(
                withoutTarget,
                reduced,
                command.destination(),
                target.node());
        if (insertion.diagnostic().isPresent()) {
            return failure(insertion.diagnostic().orElseThrow());
        }
        return applied(withRoot(current, insertion.root().orElseThrow()));
    }

    private SemanticResult replaceSlotChild(
            DesignerDocument current,
            TreeIndex index,
            ReplaceSlotChild command) {
        SlotLookup lookup = lookupSlot(
                index,
                command.ownerId(),
                command.slotName(),
                SlotCardinality.SINGLE);
        if (lookup.diagnostic().isPresent()) {
            return failure(lookup.diagnostic().orElseThrow());
        }
        SlotContext slot = lookup.context().orElseThrow();
        if (!(slot.value() instanceof WidgetSlot.SingleSlot single)
                || single.child().isEmpty()
                || !single.child().orElseThrow().id().equals(
                        command.expectedChildId())) {
            return staleSlot(
                    slot,
                    List.of(command.expectedChildId()),
                    directChildIds(slot.value()));
        }

        WidgetNode replacement;
        WidgetNode root = current.root();
        if (command.replacement() instanceof ReplaceSlotChild.NewSubtree fresh) {
            Optional<DesignerCommandDiagnostic> subtree = validateInsertedSubtree(
                    fresh.widget(), index.ids(), "/command/replacement/widget");
            if (subtree.isPresent()) {
                return failure(subtree.orElseThrow());
            }
            replacement = fresh.widget();
        } else {
            StableId sourceId = ((ReplaceSlotChild.ExistingWidget)
                    command.replacement()).widgetId();
            NodeRef source = index.nodes().get(sourceId);
            if (source == null) {
                return failure(
                        DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.TARGET_NOT_FOUND,
                        "/command/replacement/widgetId",
                        Optional.of(sourceId),
                        "Replacement widget '" + sourceId
                        + "' does not exist in the current revision.");
            }
            if (source.parentId().isEmpty()) {
                return failure(
                        DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN,
                        "/root",
                        Optional.of(sourceId),
                        "The required designer root cannot be moved into a slot.");
            }
            if (sourceId.equals(command.expectedChildId())) {
                return noChange("The requested replacement is already the slot child.");
            }
            Set<StableId> sourceIds = collectIds(
                    source.node(), limits.maxNodes()).ids();
            if (sourceIds.contains(command.ownerId())) {
                return failure(
                        DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.DESTINATION_INSIDE_SUBTREE,
                        source.path(),
                        Optional.of(sourceId),
                        "Widget '" + sourceId
                        + "' cannot replace a child of a widget in its own subtree.");
            }
            replacement = source.node();
        }

        Optional<DesignerCommandDiagnostic> acceptance = accepts(
                slot, replacement, "/command/replacement");
        if (acceptance.isPresent()) {
            return failure(acceptance.orElseThrow());
        }

        if (command.replacement() instanceof ReplaceSlotChild.ExistingWidget existing) {
            NodeRef source = index.nodes().get(existing.widgetId());
            Removal removal = remove(root, index, source);
            if (removal.diagnostic().isPresent()) {
                return failure(removal.diagnostic().orElseThrow());
            }
            root = removal.root().orElseThrow();
            TreeIndex reduced = TreeIndex.create(root, limits.maxNodes());
            SlotLookup refreshed = lookupSlot(
                    reduced,
                    command.ownerId(),
                    command.slotName(),
                    SlotCardinality.SINGLE);
            if (refreshed.diagnostic().isPresent()) {
                return failure(refreshed.diagnostic().orElseThrow());
            }
            slot = refreshed.context().orElseThrow();
            if (!(slot.value() instanceof WidgetSlot.SingleSlot refreshedSingle)
                    || refreshedSingle.child().isEmpty()
                    || !refreshedSingle.child().orElseThrow().id().equals(
                            command.expectedChildId())) {
                return staleSlot(
                        slot,
                        List.of(command.expectedChildId()),
                        directChildIds(slot.value()));
            }
        }

        WidgetNode changedOwner = withSlot(
                slot.owner().node(),
                command.slotName(),
                WidgetSlot.SingleSlot.of(replacement));
        return applied(withRoot(
                current,
                replace(root, command.ownerId(), changedOwner)));
    }

    private SemanticResult clearSlotChildren(
            DesignerDocument current,
            TreeIndex index,
            ClearSlotChildren command) {
        SlotLookup lookup = lookupSlot(
                index,
                command.ownerId(),
                command.slotName(),
                SlotCardinality.LIST);
        if (lookup.diagnostic().isPresent()) {
            return failure(lookup.diagnostic().orElseThrow());
        }
        SlotContext slot = lookup.context().orElseThrow();
        List<StableId> actual = directChildIds(slot.value());
        if (!actual.equals(command.expectedChildIds())) {
            return staleSlot(slot, command.expectedChildIds(), actual);
        }
        if (actual.isEmpty()) {
            return noChange("List slot '" + command.slotName().value()
                    + "' is already empty.");
        }
        if (slot.definition().minChildren() > 0) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.SLOT_REQUIRED,
                    slot.path(),
                    Optional.of(command.ownerId()),
                    "Clearing slot '" + command.slotName().value()
                    + "' would leave it below its minimum of "
                    + slot.definition().minChildren() + " children.");
        }

        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(slot.owner().node().slots());
        if (slot.definition().parameter().required()) {
            slots.put(command.slotName(), new WidgetSlot.ListSlot(List.of()));
        } else {
            slots.remove(command.slotName());
        }
        WidgetNode changedOwner = new WidgetNode(
                slot.owner().node().id(),
                slot.owner().node().type(),
                slot.owner().node().properties(),
                slots,
                slot.owner().node().extensions());
        return applied(withRoot(
                current,
                replace(current.root(), command.ownerId(), changedOwner)));
    }

    private SemanticResult wrap(
            DesignerDocument current,
            TreeIndex index,
            WrapWidget command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        CollectedIds wrapperIds = collectIds(command.wrapper(), limits.maxNodes());
        if (wrapperIds.limitExceeded()) {
            return failure(
                    DesignerCommandStatus.LIMIT_EXCEEDED,
                    DesignerCommandDiagnosticCode.WIDGET_SUBTREE_LIMIT,
                    "/command/wrapper",
                    Optional.of(command.wrapper().id()),
                    "The wrapper subtree exceeds the command node limit of "
                    + limits.maxNodes() + ".");
        }
        if (wrapperIds.duplicate().isPresent()) {
            StableId duplicate = wrapperIds.duplicate().orElseThrow();
            return idConflict(duplicate, "/command/wrapper");
        }
        for (StableId id : wrapperIds.ids()) {
            if (index.ids().contains(id)) {
                return idConflict(id, "/command/wrapper");
            }
        }
        Optional<DesignerCommandDiagnostic> type = knownType(
                command.wrapper(), "/command/wrapper");
        if (type.isPresent()) {
            return failure(type.orElseThrow());
        }

        WidgetPlacement placement = new WidgetPlacement(
                command.wrapper().id(),
                command.wrapperSlot(),
                command.wrapperIndex());
        TreeIndex wrapperIndex = TreeIndex.create(
                command.wrapper(), limits.maxNodes());
        Insertion insertion = insert(
                command.wrapper(), wrapperIndex, placement, target.node());
        if (insertion.diagnostic().isPresent()) {
            return failure(insertion.diagnostic().orElseThrow());
        }
        WidgetNode wrapped = insertion.root().orElseThrow();
        WidgetNode root = replace(current.root(), command.widgetId(), wrapped);
        return applied(withRoot(current, root));
    }

    private SemanticResult setProperty(
            DesignerDocument current,
            TreeIndex index,
            SetProperty command) {
        return patchProperties(current, index, new PatchProperties(
                command.widgetId(),
                List.of(new PatchProperties.SetPatch(
                        command.propertyName(), command.value()))));
    }

    private SemanticResult resetProperty(
            DesignerDocument current,
            TreeIndex index,
            ResetProperty command) {
        return patchProperties(current, index, new PatchProperties(
                command.widgetId(),
                List.of(new PatchProperties.ResetPatch(
                        command.propertyName()))));
    }

    private SemanticResult patchProperties(
            DesignerDocument current,
            TreeIndex index,
            PatchProperties command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        WidgetDefinition definition = catalog.find(target.node().type()).orElseThrow();
        for (PatchProperties.Patch patch : command.patches()) {
            Optional<PropertyDefinition> property = definition.property(
                    patch.propertyName());
            String path = target.path() + "/properties/" + pointer(
                    patch.propertyName().value());
            if (property.isEmpty()) {
                return failure(
                        DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN,
                        path,
                        Optional.of(command.widgetId()),
                        "Widget type '" + target.node().type().value()
                        + "' has no property '" + patch.propertyName().value() + "'.");
            }
            PropertyDefinition declared = property.orElseThrow();
            if (patch instanceof PatchProperties.ResetPatch) {
                if (declared.parameter().required()) {
                    return failure(
                            DesignerCommandStatus.REJECTED,
                            DesignerCommandDiagnosticCode.PROPERTY_REQUIRED,
                            path,
                            Optional.of(command.widgetId()),
                            "Required property '" + patch.propertyName().value()
                            + "' cannot be reset.");
                }
                continue;
            }
            PropertyValue requested = ((PatchProperties.SetPatch) patch).value();
            Optional<PropertyValueConstraint> constraint = declared.constraints().stream()
                    .filter(value -> value.kind() == requested.kind())
                    .findFirst();
            if (constraint.isEmpty()
                    || !constraint.orElseThrow().accepts(requested)) {
                String accepted = constraint.map(PropertyValueConstraint::description)
                        .orElse("accepted kinds " + declared.acceptedKinds());
                return failure(
                        DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED,
                        path,
                        Optional.of(command.widgetId()),
                        "Property '" + patch.propertyName().value()
                        + "' rejects " + requested.kind().wireName()
                        + "; expected " + accepted + ".");
            }
        }

        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(target.node().properties());
        for (PatchProperties.Patch patch : command.patches()) {
            if (patch instanceof PatchProperties.SetPatch set) {
                properties.put(set.propertyName(), set.value());
            } else {
                properties.remove(patch.propertyName());
            }
        }
        if (properties.equals(target.node().properties())) {
            return noChange("The property patch leaves the widget unchanged.");
        }
        WidgetNode replacement = new WidgetNode(
                target.node().id(),
                target.node().type(),
                properties,
                target.node().slots(),
                target.node().extensions());
        return applied(withRoot(
                current, replace(current.root(), command.widgetId(), replacement)));
    }

    private SlotLookup lookupSlot(
            TreeIndex index,
            StableId ownerId,
            SlotName slotName,
            SlotCardinality expectedCardinality) {
        NodeRef owner = index.nodes().get(ownerId);
        if (owner == null) {
            return SlotLookup.failure(diagnostic(
                    DesignerCommandDiagnosticCode.PARENT_NOT_FOUND,
                    "/command/ownerId",
                    Optional.of(ownerId),
                    "Slot owner '" + ownerId
                    + "' does not exist in the current revision."));
        }
        WidgetDefinition ownerDefinition = catalog.find(
                owner.node().type()).orElseThrow();
        Optional<SlotDefinition> declared = ownerDefinition.slot(slotName);
        String path = owner.path() + "/slots/" + pointer(slotName.value());
        if (declared.isEmpty()) {
            return SlotLookup.failure(diagnostic(
                    DesignerCommandDiagnosticCode.SLOT_UNKNOWN,
                    path,
                    Optional.of(ownerId),
                    "Widget type '" + owner.node().type().value()
                    + "' has no slot '" + slotName.value() + "'."));
        }
        SlotDefinition definition = declared.orElseThrow();
        WidgetSlot value = owner.node().slots().get(slotName);
        if (definition.cardinality() != expectedCardinality
                || value != null && value.cardinality() != expectedCardinality) {
            return SlotLookup.failure(cardinalityDiagnostic(owner, path));
        }
        return SlotLookup.success(new SlotContext(
                owner, definition, value, path));
    }

    private Optional<DesignerCommandDiagnostic> accepts(
            SlotContext slot,
            WidgetNode child,
            String childPath) {
        Optional<WidgetDefinition> childDefinition = catalog.find(child.type());
        if (childDefinition.isEmpty()) {
            return Optional.of(diagnostic(
                    DesignerCommandDiagnosticCode.WIDGET_TYPE_UNKNOWN,
                    childPath + "/type",
                    Optional.of(child.id()),
                    "Widget type '" + child.type().value()
                    + "' is not present in the bound catalog."));
        }
        if (!slot.definition().acceptance().accepts(
                childDefinition.orElseThrow())) {
            return Optional.of(diagnostic(
                    DesignerCommandDiagnosticCode.SLOT_REJECTS_WIDGET,
                    slot.path(),
                    Optional.of(child.id()),
                    "Slot '" + slot.definition().name().value()
                    + "' does not accept widget type '"
                    + child.type().value() + "'."));
        }
        return Optional.empty();
    }

    private SemanticResult staleSlot(
            SlotContext slot,
            List<StableId> expected,
            List<StableId> actual) {
        return failure(
                DesignerCommandStatus.REJECTED,
                DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT,
                slot.path(),
                Optional.of(slot.owner().node().id()),
                "Slot '" + slot.definition().name().value()
                + "' changed after the command was planned; expected direct child ids "
                + expected + " but found " + actual + ".");
    }

    private static List<StableId> directChildIds(WidgetSlot slot) {
        if (slot == null) {
            return List.of();
        }
        return children(slot).stream().map(WidgetNode::id).toList();
    }

    private static WidgetNode withSlot(
            WidgetNode owner,
            SlotName slotName,
            WidgetSlot value) {
        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(owner.slots());
        slots.put(slotName, value);
        return new WidgetNode(
                owner.id(),
                owner.type(),
                owner.properties(),
                slots,
                owner.extensions());
    }

    private Insertion insert(
            WidgetNode root,
            TreeIndex index,
            WidgetPlacement destination,
            WidgetNode child) {
        NodeRef parent = index.nodes().get(destination.parentId());
        if (parent == null) {
            return Insertion.failure(diagnostic(
                    DesignerCommandDiagnosticCode.PARENT_NOT_FOUND,
                    "/command/destination/parentId",
                    Optional.of(destination.parentId()),
                    "Destination parent '" + destination.parentId()
                    + "' does not exist."));
        }
        WidgetDefinition parentDefinition = catalog.find(
                parent.node().type()).orElseThrow();
        Optional<SlotDefinition> declaredSlot = parentDefinition.slot(
                destination.slotName());
        String slotPath = parent.path() + "/slots/" + pointer(
                destination.slotName().value());
        if (declaredSlot.isEmpty()) {
            return Insertion.failure(diagnostic(
                    DesignerCommandDiagnosticCode.SLOT_UNKNOWN,
                    slotPath,
                    Optional.of(parent.node().id()),
                    "Widget type '" + parent.node().type().value()
                    + "' has no slot '" + destination.slotName().value() + "'."));
        }
        Optional<WidgetDefinition> childDefinition = catalog.find(child.type());
        if (childDefinition.isEmpty()) {
            return Insertion.failure(diagnostic(
                    DesignerCommandDiagnosticCode.WIDGET_TYPE_UNKNOWN,
                    "/command/widget/type",
                    Optional.of(child.id()),
                    "Widget type '" + child.type().value()
                    + "' is not present in the bound catalog."));
        }
        SlotDefinition slotDefinition = declaredSlot.orElseThrow();
        if (!slotDefinition.acceptance().accepts(childDefinition.orElseThrow())) {
            return Insertion.failure(diagnostic(
                    DesignerCommandDiagnosticCode.SLOT_REJECTS_WIDGET,
                    slotPath,
                    Optional.of(child.id()),
                    "Slot '" + destination.slotName().value()
                    + "' does not accept widget type '" + child.type().value() + "'."));
        }

        WidgetSlot existing = parent.node().slots().get(destination.slotName());
        WidgetSlot replacement;
        if (slotDefinition.cardinality() == SlotCardinality.SINGLE) {
            if (destination.index() != 0) {
                return Insertion.failure(indexDiagnostic(
                        destination, parent, slotPath, 0));
            }
            if (existing != null && existing.cardinality() != SlotCardinality.SINGLE) {
                return Insertion.failure(cardinalityDiagnostic(parent, slotPath));
            }
            if (existing instanceof WidgetSlot.SingleSlot single
                    && single.child().isPresent()) {
                return Insertion.failure(diagnostic(
                        DesignerCommandDiagnosticCode.SLOT_FULL,
                        slotPath,
                        Optional.of(parent.node().id()),
                        "Single slot '" + destination.slotName().value()
                        + "' already contains a widget."));
            }
            if (slotDefinition.maxChildren() < 1) {
                return Insertion.failure(fullDiagnostic(parent, slotPath, slotDefinition));
            }
            replacement = WidgetSlot.SingleSlot.of(child);
        } else {
            if (existing != null && existing.cardinality() != SlotCardinality.LIST) {
                return Insertion.failure(cardinalityDiagnostic(parent, slotPath));
            }
            List<WidgetNode> children = existing instanceof WidgetSlot.ListSlot list
                    ? list.children() : List.of();
            if (destination.index() < 0 || destination.index() > children.size()) {
                return Insertion.failure(indexDiagnostic(
                        destination, parent, slotPath, children.size()));
            }
            if (children.size() >= slotDefinition.maxChildren()) {
                return Insertion.failure(fullDiagnostic(parent, slotPath, slotDefinition));
            }
            ArrayList<WidgetNode> changed = new ArrayList<>(children);
            changed.add(destination.index(), child);
            replacement = new WidgetSlot.ListSlot(changed);
        }

        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(parent.node().slots());
        slots.put(destination.slotName(), replacement);
        WidgetNode newParent = new WidgetNode(
                parent.node().id(),
                parent.node().type(),
                parent.node().properties(),
                slots,
                parent.node().extensions());
        return Insertion.success(replace(root, parent.node().id(), newParent));
    }

    private Removal remove(
            WidgetNode root,
            TreeIndex index,
            NodeRef target) {
        return remove(root, index, target, true);
    }

    private Removal remove(
            WidgetNode root,
            TreeIndex index,
            NodeRef target,
            boolean enforceMinimumChildren) {
        StableId parentId = target.parentId().orElseThrow();
        NodeRef parent = index.nodes().get(parentId);
        SlotName slotName = target.parentSlot().orElseThrow();
        WidgetDefinition parentDefinition = catalog.find(parent.node().type()).orElseThrow();
        SlotDefinition slotDefinition = parentDefinition.slot(slotName).orElseThrow();
        WidgetSlot existing = parent.node().slots().get(slotName);
        String slotPath = parent.path() + "/slots/" + pointer(slotName.value());
        int currentCount = existing instanceof WidgetSlot.SingleSlot single
                ? single.child().isPresent() ? 1 : 0
                : ((WidgetSlot.ListSlot) existing).children().size();
        if (enforceMinimumChildren
                && currentCount - 1 < slotDefinition.minChildren()) {
            return Removal.failure(diagnostic(
                    DesignerCommandDiagnosticCode.SLOT_REQUIRED,
                    slotPath,
                    Optional.of(target.node().id()),
                    "Removing widget '" + target.node().id() + "' would leave slot '"
                    + slotName.value() + "' below its minimum of "
                    + slotDefinition.minChildren() + " children."));
        }

        LinkedHashMap<SlotName, WidgetSlot> slots =
                new LinkedHashMap<>(parent.node().slots());
        if (existing instanceof WidgetSlot.SingleSlot) {
            if (slotDefinition.parameter().required()) {
                slots.put(slotName, WidgetSlot.SingleSlot.empty());
            } else {
                slots.remove(slotName);
            }
        } else {
            ArrayList<WidgetNode> children = new ArrayList<>(
                    ((WidgetSlot.ListSlot) existing).children());
            children.remove(target.childIndex());
            if (children.isEmpty() && !slotDefinition.parameter().required()) {
                slots.remove(slotName);
            } else {
                slots.put(slotName, new WidgetSlot.ListSlot(children));
            }
        }
        WidgetNode newParent = new WidgetNode(
                parent.node().id(),
                parent.node().type(),
                parent.node().properties(),
                slots,
                parent.node().extensions());
        return Removal.success(replace(root, parent.node().id(), newParent));
    }

    private Optional<DesignerCommandDiagnostic> validateInsertedSubtree(
            WidgetNode subtree,
            Set<StableId> existing,
            String path) {
        CollectedIds collected = collectIds(subtree, limits.maxNodes());
        if (collected.limitExceeded()) {
            return Optional.of(diagnostic(
                    DesignerCommandDiagnosticCode.WIDGET_SUBTREE_LIMIT,
                    path,
                    Optional.of(subtree.id()),
                    "The inserted subtree exceeds the command node limit of "
                    + limits.maxNodes() + "."));
        }
        if (collected.duplicate().isPresent()) {
            return Optional.of(idConflict(
                    collected.duplicate().orElseThrow(), path).diagnostic().orElseThrow());
        }
        for (StableId id : collected.ids()) {
            if (existing.contains(id)) {
                return Optional.of(idConflict(id, path).diagnostic().orElseThrow());
            }
        }
        return knownType(subtree, path);
    }

    private Optional<DesignerCommandDiagnostic> knownType(
            WidgetNode widget,
            String path) {
        if (catalog.find(widget.type()).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(diagnostic(
                DesignerCommandDiagnosticCode.WIDGET_TYPE_UNKNOWN,
                path + "/type",
                Optional.of(widget.id()),
                "Widget type '" + widget.type().value()
                + "' is not present in the bound catalog."));
    }

    private SemanticResult targetNotFound(StableId id) {
        return failure(
                DesignerCommandStatus.REJECTED,
                DesignerCommandDiagnosticCode.TARGET_NOT_FOUND,
                "/command/widgetId",
                Optional.of(id),
                "Widget '" + id + "' does not exist in the current revision.");
    }

    private SemanticResult idConflict(StableId id, String path) {
        return failure(
                DesignerCommandStatus.REJECTED,
                DesignerCommandDiagnosticCode.WIDGET_ID_CONFLICT,
                path,
                Optional.of(id),
                "Widget id '" + id + "' is already present or duplicated.");
    }

    private static DesignerCommandDiagnostic indexDiagnostic(
            WidgetPlacement destination,
            NodeRef parent,
            String path,
            int maximum) {
        return diagnostic(
                DesignerCommandDiagnosticCode.SLOT_INDEX_OUT_OF_BOUNDS,
                path,
                Optional.of(parent.node().id()),
                "Insertion index " + destination.index() + " for slot '"
                + destination.slotName().value() + "' must be between 0 and "
                + maximum + ".");
    }

    private static DesignerCommandDiagnostic cardinalityDiagnostic(
            NodeRef parent,
            String path) {
        return diagnostic(
                DesignerCommandDiagnosticCode.SLOT_CARDINALITY_MISMATCH,
                path,
                Optional.of(parent.node().id()),
                "The current slot value has a different cardinality than its catalog definition.");
    }

    private static DesignerCommandDiagnostic fullDiagnostic(
            NodeRef parent,
            String path,
            SlotDefinition definition) {
        return diagnostic(
                DesignerCommandDiagnosticCode.SLOT_FULL,
                path,
                Optional.of(parent.node().id()),
                "Slot '" + definition.name().value() + "' already contains its maximum of "
                + definition.maxChildren() + " children.");
    }

    private SemanticResult applied(DesignerDocument document) {
        return new SemanticResult(
                DesignerCommandStatus.APPLIED,
                Optional.of(document),
                Optional.empty());
    }

    private SemanticResult noChange(String message) {
        return failure(
                DesignerCommandStatus.NO_CHANGE,
                DesignerCommandDiagnosticCode.NO_CHANGE,
                "",
                Optional.empty(),
                message);
    }

    private SemanticResult failure(DesignerCommandDiagnostic diagnostic) {
        DesignerCommandStatus status = switch (diagnostic.code()) {
            case WIDGET_SUBTREE_LIMIT -> DesignerCommandStatus.LIMIT_EXCEEDED;
            default -> DesignerCommandStatus.REJECTED;
        };
        return new SemanticResult(status, Optional.empty(), Optional.of(diagnostic));
    }

    private SemanticResult failure(
            DesignerCommandStatus status,
            DesignerCommandDiagnosticCode code,
            String path,
            Optional<StableId> widgetId,
            String message) {
        return new SemanticResult(
                status,
                Optional.empty(),
                Optional.of(diagnostic(code, path, widgetId, message)));
    }

    private static DesignerCommandStatus statusForValidation(ValidationIssue issue) {
        return switch (issue.code()) {
            case WidgetTreeValidator.DEPTH_LIMIT,
                    WidgetTreeValidator.NODE_LIMIT,
                    WidgetTreeValidator.PROPERTY_LIMIT,
                    WidgetTreeValidator.SLOT_LIMIT,
                    WidgetTreeValidator.ISSUES_TRUNCATED ->
                DesignerCommandStatus.LIMIT_EXCEEDED;
            default -> DesignerCommandStatus.REJECTED;
        };
    }

    private static DesignerDocument withRoot(
            DesignerDocument document,
            WidgetNode root) {
        return new DesignerDocument(
                document.schemaReference(),
                document.documentId(),
                document.source(),
                document.canvas(),
                root,
                document.extensions());
    }

    static DesignerDocument withSource(
            DesignerDocument document,
            dev.flutter.netbeans.designer.model.DartSourceDescriptor source) {
        return new DesignerDocument(
                document.schemaReference(),
                document.documentId(),
                source,
                document.canvas(),
                document.root(),
                document.extensions());
    }

    private static WidgetNode replace(
            WidgetNode node,
            StableId target,
            WidgetNode replacement) {
        if (node.id().equals(target)) {
            return replacement;
        }
        LinkedHashMap<SlotName, WidgetSlot> changedSlots = null;
        for (Map.Entry<SlotName, WidgetSlot> entry : node.slots().entrySet()) {
            WidgetSlot slot = entry.getValue();
            WidgetSlot changed = slot;
            if (slot instanceof WidgetSlot.SingleSlot single
                    && single.child().isPresent()) {
                WidgetNode child = single.child().orElseThrow();
                WidgetNode next = replace(child, target, replacement);
                if (next != child) {
                    changed = WidgetSlot.SingleSlot.of(next);
                }
            } else if (slot instanceof WidgetSlot.ListSlot list) {
                ArrayList<WidgetNode> children = null;
                for (int index = 0; index < list.children().size(); index++) {
                    WidgetNode child = list.children().get(index);
                    WidgetNode next = replace(child, target, replacement);
                    if (next != child) {
                        if (children == null) {
                            children = new ArrayList<>(list.children());
                        }
                        children.set(index, next);
                    }
                }
                if (children != null) {
                    changed = new WidgetSlot.ListSlot(children);
                }
            }
            if (changed != slot) {
                if (changedSlots == null) {
                    changedSlots = new LinkedHashMap<>(node.slots());
                }
                changedSlots.put(entry.getKey(), changed);
            }
        }
        if (changedSlots == null) {
            return node;
        }
        return new WidgetNode(
                node.id(), node.type(), node.properties(), changedSlots, node.extensions());
    }

    private static CollectedIds collectIds(WidgetNode root, int maximum) {
        HashSet<StableId> ids = new HashSet<>();
        Deque<WidgetNode> pending = new ArrayDeque<>();
        pending.push(root);
        StableId duplicate = null;
        int count = 0;
        while (!pending.isEmpty()) {
            WidgetNode node = pending.pop();
            if (++count > maximum) {
                return new CollectedIds(Set.copyOf(ids), Optional.ofNullable(duplicate), true);
            }
            if (!ids.add(node.id()) && duplicate == null) {
                duplicate = node.id();
            }
            pushChildren(node, pending);
        }
        return new CollectedIds(Set.copyOf(ids), Optional.ofNullable(duplicate), false);
    }

    private static void pushChildren(WidgetNode node, Deque<WidgetNode> pending) {
        List<Map.Entry<SlotName, WidgetSlot>> slots = new ArrayList<>(
                node.slots().entrySet());
        slots.sort(Comparator.comparing(
                (Map.Entry<SlotName, WidgetSlot> entry) -> entry.getKey().value())
                .reversed());
        for (Map.Entry<SlotName, WidgetSlot> entry : slots) {
            List<WidgetNode> children = children(entry.getValue());
            for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(children.get(index));
            }
        }
    }

    private static List<WidgetNode> children(WidgetSlot slot) {
        return switch (slot) {
            case WidgetSlot.SingleSlot single -> single.child().stream().toList();
            case WidgetSlot.ListSlot list -> list.children();
        };
    }

    private static DesignerCommandDiagnostic diagnostic(
            DesignerCommandDiagnosticCode code,
            String path,
            Optional<StableId> widgetId,
            String message) {
        return new DesignerCommandDiagnostic(code, path, widgetId, message);
    }

    private static String pointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    record SemanticResult(
            DesignerCommandStatus status,
            Optional<DesignerDocument> document,
            Optional<DesignerCommandDiagnostic> diagnostic) {
        SemanticResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(diagnostic, "diagnostic");
            if ((status == DesignerCommandStatus.APPLIED) != document.isPresent()
                    || (status == DesignerCommandStatus.APPLIED) == diagnostic.isPresent()) {
                throw new IllegalArgumentException("inconsistent semantic command result");
            }
        }
    }

    private record Insertion(
            Optional<WidgetNode> root,
            Optional<DesignerCommandDiagnostic> diagnostic) {
        static Insertion success(WidgetNode root) {
            return new Insertion(Optional.of(root), Optional.empty());
        }

        static Insertion failure(DesignerCommandDiagnostic diagnostic) {
            return new Insertion(Optional.empty(), Optional.of(diagnostic));
        }
    }

    private record Removal(
            Optional<WidgetNode> root,
            Optional<DesignerCommandDiagnostic> diagnostic) {
        static Removal success(WidgetNode root) {
            return new Removal(Optional.of(root), Optional.empty());
        }

        static Removal failure(DesignerCommandDiagnostic diagnostic) {
            return new Removal(Optional.empty(), Optional.of(diagnostic));
        }
    }

    private record SlotLookup(
            Optional<SlotContext> context,
            Optional<DesignerCommandDiagnostic> diagnostic) {
        static SlotLookup success(SlotContext context) {
            return new SlotLookup(Optional.of(context), Optional.empty());
        }

        static SlotLookup failure(DesignerCommandDiagnostic diagnostic) {
            return new SlotLookup(Optional.empty(), Optional.of(diagnostic));
        }
    }

    private record SlotContext(
            NodeRef owner,
            SlotDefinition definition,
            WidgetSlot value,
            String path) {
    }

    private record CollectedIds(
            Set<StableId> ids,
            Optional<StableId> duplicate,
            boolean limitExceeded) {
    }

    private record NodeRef(
            WidgetNode node,
            Optional<StableId> parentId,
            Optional<SlotName> parentSlot,
            int childIndex,
            String path) {
    }

    private record TreeIndex(Map<StableId, NodeRef> nodes, Set<StableId> ids) {
        static TreeIndex create(WidgetNode root, int maximum) {
            LinkedHashMap<StableId, NodeRef> result = new LinkedHashMap<>();
            Deque<IndexFrame> pending = new ArrayDeque<>();
            pending.push(new IndexFrame(
                    root, Optional.empty(), Optional.empty(), 0, "/root"));
            while (!pending.isEmpty() && result.size() < maximum) {
                IndexFrame frame = pending.pop();
                result.put(frame.node().id(), new NodeRef(
                        frame.node(),
                        frame.parentId(),
                        frame.parentSlot(),
                        frame.childIndex(),
                        frame.path()));
                List<Map.Entry<SlotName, WidgetSlot>> slots = new ArrayList<>(
                        frame.node().slots().entrySet());
                slots.sort(Comparator.comparing(
                        (Map.Entry<SlotName, WidgetSlot> entry) ->
                            entry.getKey().value()).reversed());
                for (Map.Entry<SlotName, WidgetSlot> entry : slots) {
                    List<WidgetNode> children = children(entry.getValue());
                    for (int index = children.size() - 1; index >= 0; index--) {
                        WidgetNode child = children.get(index);
                        String suffix = entry.getValue() instanceof WidgetSlot.ListSlot
                                ? "/" + index : "/child";
                        pending.push(new IndexFrame(
                                child,
                                Optional.of(frame.node().id()),
                                Optional.of(entry.getKey()),
                                index,
                                frame.path() + "/slots/"
                                + pointer(entry.getKey().value()) + suffix));
                    }
                }
            }
            return new TreeIndex(Map.copyOf(result), Set.copyOf(result.keySet()));
        }
    }

    private record IndexFrame(
            WidgetNode node,
            Optional<StableId> parentId,
            Optional<SlotName> parentSlot,
            int childIndex,
            String path) {
    }
}

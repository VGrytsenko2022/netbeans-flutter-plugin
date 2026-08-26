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
            case WrapWidget wrap -> wrap(current, index, wrap);
            case SetProperty set -> setProperty(current, index, set);
            case ResetProperty reset -> resetProperty(current, index, reset);
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
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        WidgetDefinition definition = catalog.find(target.node().type()).orElseThrow();
        Optional<PropertyDefinition> property = definition.property(
                command.propertyName());
        String path = target.path() + "/properties/" + pointer(
                command.propertyName().value());
        if (property.isEmpty()) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN,
                    path,
                    Optional.of(command.widgetId()),
                    "Widget type '" + target.node().type().value()
                    + "' has no property '" + command.propertyName().value() + "'.");
        }
        PropertyDefinition declared = property.orElseThrow();
        Optional<PropertyValueConstraint> constraint = declared.constraints().stream()
                .filter(value -> value.kind() == command.value().kind())
                .findFirst();
        if (constraint.isEmpty()
                || !constraint.orElseThrow().accepts(command.value())) {
            String accepted = constraint.map(PropertyValueConstraint::description)
                    .orElse("accepted kinds " + declared.acceptedKinds());
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED,
                    path,
                    Optional.of(command.widgetId()),
                    "Property '" + command.propertyName().value()
                    + "' rejects " + command.value().kind().wireName()
                    + "; expected " + accepted + ".");
        }
        if (command.value().equals(
                target.node().properties().get(command.propertyName()))) {
            return noChange("Property '" + command.propertyName().value()
                    + "' already has the requested value.");
        }
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(target.node().properties());
        properties.put(command.propertyName(), command.value());
        WidgetNode replacement = new WidgetNode(
                target.node().id(),
                target.node().type(),
                properties,
                target.node().slots(),
                target.node().extensions());
        return applied(withRoot(
                current, replace(current.root(), command.widgetId(), replacement)));
    }

    private SemanticResult resetProperty(
            DesignerDocument current,
            TreeIndex index,
            ResetProperty command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) {
            return targetNotFound(command.widgetId());
        }
        WidgetDefinition definition = catalog.find(target.node().type()).orElseThrow();
        Optional<PropertyDefinition> property = definition.property(
                command.propertyName());
        String path = target.path() + "/properties/" + pointer(
                command.propertyName().value());
        if (property.isEmpty()) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.PROPERTY_UNKNOWN,
                    path,
                    Optional.of(command.widgetId()),
                    "Widget type '" + target.node().type().value()
                    + "' has no property '" + command.propertyName().value() + "'.");
        }
        if (property.orElseThrow().parameter().required()) {
            return failure(
                    DesignerCommandStatus.REJECTED,
                    DesignerCommandDiagnosticCode.PROPERTY_REQUIRED,
                    path,
                    Optional.of(command.widgetId()),
                    "Required property '" + command.propertyName().value()
                    + "' cannot be reset.");
        }
        if (!target.node().properties().containsKey(command.propertyName())) {
            return noChange("Optional property '" + command.propertyName().value()
                    + "' is already absent.");
        }
        LinkedHashMap<PropertyName, PropertyValue> properties =
                new LinkedHashMap<>(target.node().properties());
        properties.remove(command.propertyName());
        WidgetNode replacement = new WidgetNode(
                target.node().id(),
                target.node().type(),
                properties,
                target.node().slots(),
                target.node().extensions());
        return applied(withRoot(
                current, replace(current.root(), command.widgetId(), replacement)));
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

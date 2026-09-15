package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetPlacementRules;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
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
            case EditTableGrid grid -> tableGrid(current,index,grid);
            case EditDataTableGrid grid -> dataTableGrid(current,index,grid);
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
            case CreateEventHandler create -> createEventHandler(current, index, create);
            case CreateMenuAnchorBuilder create -> createMenuAnchorBuilder(current, index, create);
            case RenameEventHandler rename -> renameEventHandler(current, index, rename);
            case CreateStateBinding create -> createStateBinding(current, index, create);
            case RenameStateField rename -> renameStateField(current, index, rename);
            case RemoveStateBinding remove -> removeStateBinding(current, index, remove);
            case BindPropertyToState bind -> bindPropertyToState(current, index, bind);
            case RemovePropertyStateBinding remove -> removePropertyStateBinding(current, index, remove);
        };
        if (transformed.status() != DesignerCommandStatus.APPLIED) {
            return transformed;
        }
        DesignerDocument candidate = transformed.document().orElseThrow();
        Optional<SemanticResult> boundRangeConflict = boundSliderRangeConflict(index, candidate);
        if (boundRangeConflict.isPresent()) {
            return boundRangeConflict.orElseThrow();
        }
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
        if (candidate.equals(current) && !(command instanceof CreateEventHandler)) {
            return noChange("The command leaves the semantic document unchanged.");
        }
        return transformed;
    }

    /**
     * Preview edits do not rewrite user-owned State initializers or prove the
     * live runtime value. A changed range could therefore pass preview and Dart
     * type checks but fail a Slider assertion at runtime.
     */
    private Optional<SemanticResult> boundSliderRangeConflict(TreeIndex before, DesignerDocument candidate) {
        List<NodeRef> controlledSliders = before.nodes().values().stream()
                .filter(node -> node.node().stateBinding().isPresent()
                        && Set.of("flutter.material.Slider", "flutter.material.RangeSlider")
                                .contains(node.node().type().value()))
                .toList();
        if (controlledSliders.isEmpty()) return Optional.empty();
        TreeIndex after = TreeIndex.create(candidate.root(), limits.maxNodes());
        for (NodeRef original : controlledSliders) {
            NodeRef replacement = after.nodes().get(original.node().id());
            if (replacement == null || replacement.node().stateBinding().isEmpty()) continue;
            for (String name : List.of("min", "max")) {
                PropertyName property = new PropertyName(name);
                if (!Objects.equals(original.node().properties().get(property), replacement.node().properties().get(property))) {
                    return Optional.of(failure(DesignerCommandStatus.REJECTED,
                            DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED,
                            replacement.path() + "/properties/" + name, Optional.of(replacement.node().id()),
                            "Remove the State binding before changing Minimum or Maximum; "
                                    + "Canvas preview values do not prove the retained runtime field is inside the new bounds."));
                }
            }
        }
        return Optional.empty();
    }

    private SemanticResult dataTableGrid(DesignerDocument current,TreeIndex index,EditDataTableGrid command) {
        var target=index.nodes().get(command.expectedTable().id());
        if (target == null) return targetNotFound(command.expectedTable().id());
        if (!target.node().equals(command.expectedTable()))
            return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT,
                    target.path(),Optional.of(target.node().id()),"DataTable changed while its grid editor was open; reopen the editor.");
        try {
            var replacement=dev.flutter.netbeans.designer.catalog.DataTableGrid.edit(target.node(),command.operation(),
                    command.index(),command.destination(),command.seed());
            return applied(withRoot(current,replace(current.root(),replacement.id(),replacement)));
        } catch (IllegalArgumentException ex) {
            return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    target.path(),Optional.of(target.node().id()),ex.getMessage());
        }
    }

    private SemanticResult tableGrid(DesignerDocument current,TreeIndex index,EditTableGrid command) {
        var target=index.nodes().get(command.expectedTable().id());
        if (target == null) return targetNotFound(command.expectedTable().id());
        if (!target.node().equals(command.expectedTable()))
            return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT,
                    target.path(),Optional.of(target.node().id()),"Table changed while its grid editor was open; reopen the editor.");
        try {
            var replacement=dev.flutter.netbeans.designer.catalog.TableGrid.edit(target.node(),command.operation(),
                    command.index(),command.destination(),command.seed());
            return applied(withRoot(current,replace(current.root(),replacement.id(),replacement)));
        } catch (IllegalArgumentException ex) {
            return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    target.path(),Optional.of(target.node().id()),ex.getMessage());
        }
    }

    private SemanticResult add(
            DesignerDocument current,
            TreeIndex index,
            AddWidget command) {
        WidgetNode inserted = command.widget();
        NodeRef owner = index.nodes().get(command.destination().parentId());
        if(owner!=null&&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.TYPE.equals(owner.node().type())
                &&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.row(inserted.type())
                &&command.destination().slotName().value().equals("rows")) {
            try { inserted=dev.flutter.netbeans.designer.catalog.DataTableGrid.adaptFreshRow(inserted,owner.node()); }
            catch(IllegalArgumentException ex){return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    owner.path(),Optional.of(owner.node().id()),ex.getMessage());}
        }
        if (owner != null && dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.TYPE.equals(owner.node().type())
                && dev.flutter.netbeans.designer.catalog.TableWidgetPropertySchema.ROW.equals(inserted.type())
                && command.destination().slotName().value().equals("children")
                && inserted.equals(dev.flutter.netbeans.designer.catalog.TableGrid.starterRow(inserted.id(),2))) {
            var rows=dev.flutter.netbeans.designer.catalog.TableGrid.children(owner.node());
            if (!rows.isEmpty()) {
                int columns=dev.flutter.netbeans.designer.catalog.TableGrid.children(rows.getFirst()).size();
                if (columns < 1 || columns > 1000) return failure(DesignerCommandStatus.REJECTED,
                        DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,owner.path(),Optional.of(owner.node().id()),
                        "TableRow palette creation supports 1..1000 columns; this Table has " + columns + ".");
                inserted=dev.flutter.netbeans.designer.catalog.TableGrid.starterRow(inserted.id(),columns);
            }
        }
        Optional<DesignerCommandDiagnostic> subtree = validateInsertedSubtree(
                inserted, index.ids(), "/command/widget");
        if (subtree.isPresent()) {
            return failure(subtree.orElseThrow());
        }
        if(owner!=null&&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.table(owner.node().type())
                &&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.COLUMN.equals(inserted.type())
                &&command.destination().slotName().value().equals("columns")) {
            try {
                var table=dev.flutter.netbeans.designer.catalog.DataTableGrid.edit(owner.node(),dev.flutter.netbeans.designer.catalog.TableGrid.Operation.ADD_COLUMN,
                        command.destination().index(),0,inserted.id());
                var columns=new ArrayList<>(dev.flutter.netbeans.designer.catalog.DataTableGrid.children(table,dev.flutter.netbeans.designer.catalog.DataTableGrid.COLUMNS));
                columns.set(command.destination().index(),inserted);
                table=dev.flutter.netbeans.designer.catalog.DataTableGrid.withChildren(table,dev.flutter.netbeans.designer.catalog.DataTableGrid.COLUMNS,columns);
                return applied(withRoot(current,replace(current.root(),table.id(),table)));
            } catch(IllegalArgumentException ex){return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    owner.path(),Optional.of(owner.node().id()),ex.getMessage());}
        }
        if(owner!=null&&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.row(owner.node().type())
                &&dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.cell(inserted.type())
                &&command.destination().slotName().value().equals("cells")) {
            var tableOwner=index.nodes().values().stream().filter(n->dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.TYPE.equals(n.node().type())
                    &&dev.flutter.netbeans.designer.catalog.DataTableGrid.children(n.node(),dev.flutter.netbeans.designer.catalog.DataTableGrid.ROWS).stream()
                        .anyMatch(r->r.id().equals(owner.node().id()))).findFirst().orElse(null);
            if(tableOwner!=null)try {
                var table=dev.flutter.netbeans.designer.catalog.DataTableGrid.edit(tableOwner.node(),dev.flutter.netbeans.designer.catalog.TableGrid.Operation.ADD_COLUMN,
                        command.destination().index(),0,inserted.id());
                var rows=new ArrayList<>(dev.flutter.netbeans.designer.catalog.DataTableGrid.children(table,dev.flutter.netbeans.designer.catalog.DataTableGrid.ROWS));
                for(int r=0;r<rows.size();r++)if(rows.get(r).id().equals(owner.node().id())) {
                    var cells=new ArrayList<>(dev.flutter.netbeans.designer.catalog.DataTableGrid.children(rows.get(r),dev.flutter.netbeans.designer.catalog.DataTableGrid.CELLS));
                    cells.set(command.destination().index(),inserted);
                    rows.set(r,dev.flutter.netbeans.designer.catalog.DataTableGrid.withChildren(rows.get(r),dev.flutter.netbeans.designer.catalog.DataTableGrid.CELLS,cells));
                }
                table=dev.flutter.netbeans.designer.catalog.DataTableGrid.withChildren(table,dev.flutter.netbeans.designer.catalog.DataTableGrid.ROWS,rows);
                return applied(withRoot(current,replace(current.root(),table.id(),table)));
            }catch(IllegalArgumentException ex){return failure(DesignerCommandStatus.REJECTED,DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                    owner.path(),Optional.of(owner.node().id()),ex.getMessage());}
        }
        Insertion insertion = insert(
                current.root(),
                index,
                command.destination(),
                inserted);
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
        if(dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.COLUMN.equals(target.node().type())) {
            var owner=index.nodes().get(target.parentId().orElseThrow()).node();
            if(dev.flutter.netbeans.designer.catalog.PaginatedDataTableWidgetPropertySchema.TYPE.equals(owner.type()))
                return dataTableGrid(current,index,new EditDataTableGrid(owner,dev.flutter.netbeans.designer.catalog.TableGrid.Operation.REMOVE_COLUMN,
                        dev.flutter.netbeans.designer.catalog.DataTableGrid.children(owner,dev.flutter.netbeans.designer.catalog.DataTableGrid.COLUMNS).indexOf(target.node()),0,StableId.random()));
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
        if (sameParentSlot && dev.flutter.netbeans.designer.catalog.DataTableWidgetPropertySchema.COLUMN.equals(target.node().type())) {
            var owner = index.nodes().get(command.destination().parentId()).node();
            var columns = dev.flutter.netbeans.designer.catalog.DataTableGrid.children(owner, dev.flutter.netbeans.designer.catalog.DataTableGrid.COLUMNS);
            return dataTableGrid(current, index, new EditDataTableGrid(owner,
                    dev.flutter.netbeans.designer.catalog.TableGrid.Operation.MOVE_COLUMN,
                    columns.indexOf(target.node()), command.destination().index(), target.node().id()));
        }
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
                slot.owner().node().extensions(), slot.owner().node().stateBinding(), slot.owner().node().propertyBindings());
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

        WidgetDefinition wrapperDefinition = catalog.find(
                command.wrapper().type()).orElseThrow();
        WidgetPlacementRules.Decision outerPlacement;
        String outerPlacementPath;
        if (target.parentId().isEmpty()) {
            outerPlacement = WidgetPlacementRules.evaluateRoot(wrapperDefinition);
            outerPlacementPath = "/root";
        } else {
            NodeRef parent = index.nodes().get(target.parentId().orElseThrow());
            WidgetDefinition parentDefinition = catalog.find(
                    parent.node().type()).orElseThrow();
            SlotName parentSlotName = target.parentSlot().orElseThrow();
            SlotDefinition parentSlot = parentDefinition.slot(parentSlotName)
                    .orElseThrow();
            outerPlacement = WidgetPlacementRules.evaluate(
                    parentDefinition, parentSlot, wrapperDefinition);
            outerPlacementPath = parent.path() + "/slots/"
                    + pointer(parentSlotName.value());
        }
        if (!outerPlacement.accepted()) {
            return failure(placementDiagnostic(
                    outerPlacement,
                    outerPlacementPath,
                    command.wrapper().id()));
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

    private SemanticResult createMenuAnchorBuilder(
            DesignerDocument current, TreeIndex index, CreateMenuAnchorBuilder command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        PropertyName builder = new PropertyName("builder");
        PropertyValue old = target.node().properties().get(builder);
        if (!target.node().type().equals(dev.flutter.netbeans.designer.catalog.MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE)
                || old != null && !(old instanceof PropertyValue.NullValue)) {
            return failure(DesignerCommandStatus.CONFLICT, DesignerCommandDiagnosticCode.MENU_ANCHOR_BUILDER_REJECTED,
                    target.path() + "/properties/builder", Optional.of(command.widgetId()),
                    "Create Menu Builder requires a MenuAnchor with an omitted or explicit-null builder. Existing builders remain user-owned; reset the binding first.");
        }
        return setProperty(current, index, new SetProperty(command.widgetId(), builder,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), command.methodName(), Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
    }

    private SemanticResult createEventHandler(
            DesignerDocument current, TreeIndex index, CreateEventHandler command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        try {
            if (!FocusWidgetPropertySchema.propertyAvailable(target.node(), command.event())) {
                return eventFailure(command.widgetId(), command.event(), "This Focus event belongs to the standard constructor; External uses the node's callback.");
            }
            WidgetEventDescriptor event = EventCommandSupport.event(catalog, target.node(), command.event());
            PropertyDefinition property = catalog.find(target.node().type()).orElseThrow()
                    .property(command.event()).orElseThrow();
            SemanticResult result = setProperty(current, index, new SetProperty(command.widgetId(),
                    command.event(), event.bindingValue(command.handlerName(), property)));
            // An already-bound but missing method can still be created as a source-only transaction.
            return result.status() == DesignerCommandStatus.NO_CHANGE ? applied(current) : result;
        } catch (IllegalArgumentException invalid) {
            return eventFailure(command.widgetId(), command.event(), invalid.getMessage());
        }
    }

    private SemanticResult renameEventHandler(
            DesignerDocument current, TreeIndex index, RenameEventHandler command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        try {
            EventCommandSupport.event(catalog, target.node(), command.event());
            String oldName = EventCommandSupport.localHandler(target.node().properties().get(command.event()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The event is not bound to a local instance handler."));
            if (oldName.equals(command.newName())) return noChange("The handler name is unchanged.");
            WidgetNode root = current.root();
            for (NodeRef node : index.nodes().values()) {
                LinkedHashMap<PropertyName, PropertyValue> properties = new LinkedHashMap<>(node.node().properties());
                boolean changed = false;
                for (Map.Entry<PropertyName, PropertyValue> entry : properties.entrySet()) {
                    if (EventCommandSupport.localHandler(entry.getValue()).filter(oldName::equals).isPresent()) {
                        PropertyValue replacement;
                        if (entry.getValue() instanceof PropertyValue.CallbackValue) {
                            replacement = new PropertyValue.CallbackValue(command.newName());
                        } else {
                            PropertyValue.DartObjectReferenceValue value =
                                    (PropertyValue.DartObjectReferenceValue) entry.getValue();
                            replacement = new PropertyValue.DartObjectReferenceValue(value.libraryUri(),
                                    command.newName(), value.member(), value.access(), value.constant());
                        }
                        entry.setValue(replacement);
                        changed = true;
                    }
                }
                if (changed || node.node().stateBinding().filter(binding -> binding.handlerName().equals(oldName)).isPresent()) {
                    // Re-read the evolving tree so an ancestor replacement does not discard a child rename.
                    WidgetNode live = EventCommandSupport.widget(root, node.node().id());
                    root = replace(root, live.id(), new WidgetNode(live.id(), live.type(), properties,
                            live.slots(), live.extensions(), live.stateBinding().map(binding ->
                                    binding.handlerName().equals(oldName)
                                            ? new dev.flutter.netbeans.designer.model.StateBinding(binding.fieldName(),
                                                    command.newName(), binding.type(), binding.referenceType(), binding.previousOnChanged(),
                                                    binding.action(), binding.selectedValue())
                                            : binding), live.propertyBindings()));
                }
            }
            return applied(withRoot(current, root));
        } catch (IllegalArgumentException invalid) {
            return eventFailure(command.widgetId(), command.event(), invalid.getMessage());
        }
    }

    private SemanticResult eventFailure(StableId widgetId, PropertyName event, String message) {
        return failure(DesignerCommandStatus.REJECTED, DesignerCommandDiagnosticCode.EVENT_HANDLER_REJECTED,
                "/command/event/" + pointer(event.value()), Optional.of(widgetId), message);
    }

    private SemanticResult createStateBinding(
            DesignerDocument current, TreeIndex index, CreateStateBinding command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        try {
            if (current.source().widgetKind() != dev.flutter.netbeans.designer.model.WidgetClassKind.STATEFUL) {
                throw new IllegalArgumentException("Create State binding requires a verified Stateful form. No automatic conversion is performed.");
            }
            var binding = dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.createBinding(
                    target.node(), command.fieldName(), command.handlerName(), command.action(), command.selectedValue(), command.reusedField());
            PropertyName onChanged = dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.find(target.node()).orElseThrow().eventProperty();
            WidgetEventDescriptor event = EventCommandSupport.event(catalog, target.node(), onChanged);
            PropertyDefinition property = catalog.find(target.node().type()).orElseThrow().property(onChanged).orElseThrow();
            var properties = new LinkedHashMap<>(target.node().properties());
            properties.put(onChanged, event.bindingValue(binding.handlerName(), property));
            WidgetNode replacement = new WidgetNode(target.node().id(), target.node().type(), properties,
                    target.node().slots(), target.node().extensions(), Optional.of(binding), target.node().propertyBindings());
            return applied(withRoot(current, replace(current.root(), replacement.id(), replacement)));
        } catch (IllegalArgumentException invalid) {
            return stateBindingFailure(command.widgetId(), invalid.getMessage());
        }
    }

    private SemanticResult renameStateField(DesignerDocument current, TreeIndex index, RenameStateField command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        try {
            var field = StateCommandSupport.field(target.node(), command.fieldName());
            if (command.fieldName().equals(command.newName())) return noChange("The State field name is unchanged.");
            Map<String, String> names = field.type() == dev.flutter.netbeans.designer.model.StateBinding.Type.TEXT_CONTROLLER
                    ? Map.of(command.fieldName(), command.newName(), command.fieldName() + "StateListener", command.newName() + "StateListener")
                    : Map.of(command.fieldName(), command.newName());
            Set<String> replacements = Set.copyOf(names.values());
            Map<String, String> reverse = reverseNames(names);
            for (NodeRef entry : index.nodes().values()) {
                WidgetNode node = entry.node();
                if (node.stateBinding().filter(binding -> replacements.contains(binding.fieldName())
                        || replacements.contains(binding.handlerName())).isPresent()
                        || node.propertyBindings().values().stream().anyMatch(binding -> replacements.contains(binding.fieldName()))) {
                    throw new IllegalArgumentException("The requested State field or controller-listener name is already used by another binding; renaming cannot merge fields.");
                }
                // Dormant saved callbacks may not appear in current generated code, but must not be captured later.
                if (node.stateBinding().flatMap(dev.flutter.netbeans.designer.model.StateBinding::previousOnChanged)
                        .filter(value -> !StateCommandSupport.renameReference(value, reverse).equals(value)).isPresent()
                        || node.properties().values().stream().anyMatch(value ->
                                !StateCommandSupport.renameReference(value, reverse).equals(value))) {
                    throw new IllegalArgumentException("The requested State name is already retained by another callback reference.");
                }
            }
            WidgetNode root = current.root();
            for (NodeRef entry : index.nodes().values()) {
                // Use the evolving subtree so renaming an ancestor cannot restore old child metadata.
                WidgetNode node = EventCommandSupport.widget(root, entry.node().id());
                var properties = new LinkedHashMap<PropertyName, PropertyValue>();
                node.properties().forEach((name, value) -> properties.put(name, StateCommandSupport.renameReference(value, names)));
                var action = node.stateBinding().map(binding -> new dev.flutter.netbeans.designer.model.StateBinding(
                        binding.fieldName().equals(command.fieldName()) ? command.newName() : binding.fieldName(),
                        binding.handlerName(), binding.type(), binding.referenceType(),
                        binding.previousOnChanged().map(value -> StateCommandSupport.renameReference(value, names)),
                        binding.action(), binding.selectedValue()));
                var consumers = new LinkedHashMap<PropertyName, dev.flutter.netbeans.designer.model.StatePropertyBinding>();
                node.propertyBindings().forEach((name, binding) -> consumers.put(name,
                        binding.fieldName().equals(command.fieldName()) ? new dev.flutter.netbeans.designer.model.StatePropertyBinding(
                                command.newName(), binding.type(), binding.referenceType(), binding.transform(), binding.comparisonValue()) : binding));
                WidgetNode replacement = new WidgetNode(node.id(), node.type(), properties, node.slots(), node.extensions(), action, consumers);
                if (!replacement.equals(node)) root = replace(root, node.id(), replacement);
            }
            return applied(withRoot(current, root));
        } catch (IllegalArgumentException invalid) {
            return stateBindingFailure(command.widgetId(), invalid.getMessage());
        }
    }

    private static Map<String, String> reverseNames(Map<String, String> names) {
        var reverse = new LinkedHashMap<String, String>();
        names.forEach((before, after) -> reverse.put(after, before));
        return reverse;
    }

    private SemanticResult removeStateBinding(
            DesignerDocument current, TreeIndex index, RemoveStateBinding command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        if (target.node().stateBinding().isEmpty()) return noChange("The widget has no State binding.");
        var binding = target.node().stateBinding().orElseThrow();
        PropertyName onChanged = dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog.find(target.node()).orElseThrow().eventProperty();
        var properties = new LinkedHashMap<>(target.node().properties());
        if (EventCommandSupport.localHandler(properties.get(onChanged)).filter(binding.handlerName()::equals).isPresent()) {
            if (binding.previousOnChanged().isPresent()) properties.put(onChanged, binding.previousOnChanged().orElseThrow());
            else properties.remove(onChanged);
        }
        WidgetNode replacement = new WidgetNode(target.node().id(), target.node().type(), properties,
                target.node().slots(), target.node().extensions(), Optional.empty(), target.node().propertyBindings());
        return applied(withRoot(current, replace(current.root(), replacement.id(), replacement)));
    }

    private SemanticResult stateBindingFailure(StableId widgetId, String message) {
        return failure(DesignerCommandStatus.REJECTED, DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED,
                "/command/stateBinding", Optional.of(widgetId), message);
    }

    private SemanticResult bindPropertyToState(DesignerDocument current, TreeIndex index, BindPropertyToState command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        if (!FocusWidgetPropertySchema.propertyAvailable(target.node(), command.propertyName())) {
            return failure(DesignerCommandStatus.REJECTED, DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED,
                    target.path(), Optional.of(command.widgetId()), "This Focus property is inactive in the external-node constructor.");
        }
        if (command.binding().equals(target.node().propertyBindings().get(command.propertyName()))) {
            return noChange("The property already has this State binding.");
        }
        var bindings = new LinkedHashMap<>(target.node().propertyBindings());
        bindings.put(command.propertyName(), command.binding());
        WidgetNode replacement = new WidgetNode(target.node().id(), target.node().type(), target.node().properties(),
                target.node().slots(), target.node().extensions(), target.node().stateBinding(), bindings);
        return applied(withRoot(current, replace(current.root(), replacement.id(), replacement)));
    }

    private SemanticResult removePropertyStateBinding(DesignerDocument current, TreeIndex index, RemovePropertyStateBinding command) {
        NodeRef target = index.nodes().get(command.widgetId());
        if (target == null) return targetNotFound(command.widgetId());
        if (!target.node().propertyBindings().containsKey(command.propertyName())) return noChange("The property has no State binding.");
        var bindings = new LinkedHashMap<>(target.node().propertyBindings());
        bindings.remove(command.propertyName());
        WidgetNode replacement = new WidgetNode(target.node().id(), target.node().type(), target.node().properties(),
                target.node().slots(), target.node().extensions(), target.node().stateBinding(), bindings);
        return applied(withRoot(current, replace(current.root(), replacement.id(), replacement)));
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
                target.node().extensions(), target.node().stateBinding(), target.node().propertyBindings());
        for (PatchProperties.Patch patch : command.patches()) {
            if (patch instanceof PatchProperties.SetPatch set
                    && replacement.type().equals(FocusWidgetPropertySchema.FOCUS_TYPE)
                    && Set.of("onKey", "onKeyEvent").contains(set.propertyName().value())
                    && !(set.value() instanceof PropertyValue.NullValue)
                    && !FocusWidgetPropertySchema.propertyAvailable(replacement, set.propertyName())
                    && !set.value().equals(target.node().properties().get(set.propertyName()))) {
                return eventFailure(command.widgetId(), set.propertyName(),
                        "This Focus event cannot be bound in the external-node constructor; stored inactive handlers are preserved.");
            }
        }
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
        WidgetDefinition ownerDefinition = catalog.find(
                slot.owner().node().type()).orElseThrow();
        WidgetPlacementRules.Decision placement = WidgetPlacementRules.evaluate(
                ownerDefinition,
                slot.definition(),
                childDefinition.orElseThrow());
        if (!placement.accepted()) {
            return Optional.of(placementDiagnostic(
                    placement, slot.path(), child.id()));
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
                owner.extensions(), owner.stateBinding(), owner.propertyBindings());
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
        WidgetPlacementRules.Decision placement = WidgetPlacementRules.evaluate(
                parentDefinition,
                slotDefinition,
                childDefinition.orElseThrow());
        if (!placement.accepted()) {
            return Insertion.failure(placementDiagnostic(
                    placement, slotPath, child.id()));
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
                parent.node().extensions(), parent.node().stateBinding(), parent.node().propertyBindings());
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
                parent.node().extensions(), parent.node().stateBinding(), parent.node().propertyBindings());
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
                node.id(), node.type(), node.properties(), changedSlots, node.extensions(), node.stateBinding(), node.propertyBindings());
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

    private static DesignerCommandDiagnostic placementDiagnostic(
            WidgetPlacementRules.Decision decision,
            String path,
            StableId widgetId) {
        DesignerCommandDiagnosticCode code = decision.rejectionKind().orElseThrow()
                        == WidgetPlacementRules.RejectionKind.SLOT_ACCEPTANCE
                ? DesignerCommandDiagnosticCode.SLOT_REJECTS_WIDGET
                : DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED;
        return diagnostic(code, path, Optional.of(widgetId), decision.reason());
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

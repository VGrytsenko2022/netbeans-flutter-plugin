package dev.flutter.netbeans.designer.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Central fail-closed placement rules that cannot be expressed by a slot's
 * reusable {@link SlotAcceptance} contract alone.
 *
 * <p>The rules operate on model parent/slot relationships. They therefore
 * apply equally to validation, command transforms and Palette/DnD admission.</p>
 */
public final class WidgetPlacementRules {
    public static final String EXPANDED_TYPE = "flutter.widgets.Expanded";
    public static final String FLEXIBLE_TYPE = "flutter.widgets.Flexible";
    public static final String SPACER_TYPE = "flutter.widgets.Spacer";
    public static final String SLIVER_CROSS_AXIS_EXPANDED_TYPE = "flutter.widgets.SliverCrossAxisExpanded";

    private static final String COLUMN_TYPE = "flutter.widgets.Column";
    private static final String ROW_TYPE = "flutter.widgets.Row";
    private static final String CHILDREN_SLOT = "children";
    private static final String FLEX_PARENT_DATA_DESTINATIONS =
            "'flutter.widgets.Column.children', 'flutter.widgets.Row.children' or 'flutter.material.DataColumn.label'";
    private static final Decision ACCEPTED =
            new Decision(true, Optional.empty(), "Placement is accepted.");

    private WidgetPlacementRules() {
    }

    /** Rejection identity used to map one decision to stable diagnostics. */
    public enum RejectionKind {
        SLOT_ACCEPTANCE,
        ROOT_PLACEMENT,
        DIRECT_PARENT_SLOT
    }

    /** Palette creation behavior for a canonical widget definition. */
    public enum PaletteCreationMode {
        INSERT_PROTOTYPE,
        WRAP_EXISTING_CHILD
    }

    /** One combined slot-acceptance and placement decision. */
    public record Decision(
            boolean accepted,
            Optional<RejectionKind> rejectionKind,
            String reason) {
        public Decision {
            Objects.requireNonNull(rejectionKind, "rejectionKind");
            Objects.requireNonNull(reason, "reason");
            if (accepted == rejectionKind.isPresent()) {
                throw new IllegalArgumentException(
                        "Accepted placement must not have a rejection kind");
            }
        }
    }

    /** Evaluates whether a widget may be the Designer document root. */
    public static Decision evaluateRoot(WidgetDefinition child) {
        Objects.requireNonNull(child, "child");
        if (DataTableWidgetPropertySchema.descriptor(child.typeId()))
            return rejected(RejectionKind.ROOT_PLACEMENT,child.typeId()+" is a data-table descriptor, not a root Widget.");
        if (TableWidgetPropertySchema.ROW.equals(child.typeId()) || TableWidgetPropertySchema.CELL.equals(child.typeId()))
            return rejected(RejectionKind.ROOT_PLACEMENT, child.typeId() + " requires a direct table parent, not the root.");
        if (LayoutIdWidgetPropertySchema.TYPE.equals(child.typeId()))
            return rejected(RejectionKind.ROOT_PLACEMENT, "LayoutId requires direct CustomMultiChildLayout.children, not the root.");
        if (isStackPositionedWidget(child)) return rejected(RejectionKind.ROOT_PLACEMENT,
                child.typeId().value() + " requires a direct Stack.children parent, not the root.");
        if (SLIVER_CROSS_AXIS_EXPANDED_TYPE.equals(child.typeId().value())) {
            return rejected(RejectionKind.ROOT_PLACEMENT,
                    "SliverCrossAxisExpanded requires a direct SliverCrossAxisGroup.slivers parent, not the root.");
        }
        if (isSliverWidget(child)) {
            return rejected(RejectionKind.ROOT_PLACEMENT,
                    "Widget type '" + child.typeId().value()
                    + "' cannot be the Designer root; slivers require CustomScrollView.slivers.");
        }
        if (!isFlexRestrictedWidget(child)) {
            return ACCEPTED;
        }
        String childType = child.typeId().value();
        return rejected(
                RejectionKind.ROOT_PLACEMENT,
                "Widget type '" + childType
                + "' cannot be the Designer root; it must be a direct child of "
                + FLEX_PARENT_DATA_DESTINATIONS + ".");
    }

    /**
     * Evaluates the reusable slot acceptance first, then the contextual
     * direct-parent rule.
     */
    public static Decision evaluate(
            WidgetDefinition parent,
            SlotDefinition slot,
            WidgetDefinition child) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(child, "child");

        String destination = parent.typeId().value() + '.' + slot.name().value();
        if(DataTableWidgetPropertySchema.descriptor(child.typeId())) {
            boolean valid=DataTableWidgetPropertySchema.COLUMN.equals(child.typeId())
                    ?DataTableWidgetPropertySchema.table(parent.typeId())&&slot.name().value().equals("columns")
                    :DataTableWidgetPropertySchema.row(child.typeId())
                    ?DataTableWidgetPropertySchema.TYPE.equals(parent.typeId())&&slot.name().value().equals("rows")
                    :DataTableWidgetPropertySchema.row(parent.typeId())&&slot.name().value().equals("cells");
            if(!valid)return rejected(RejectionKind.DIRECT_PARENT_SLOT,child.typeId()+" is a descriptor; "+destination+" is not its native data-table slot.");
        }
        if (TableWidgetPropertySchema.ROW.equals(child.typeId())
                && !(TableWidgetPropertySchema.TYPE.equals(parent.typeId()) && CHILDREN_SLOT.equals(slot.name().value())))
            return rejected(RejectionKind.DIRECT_PARENT_SLOT, "TableRow is a descriptor, not a Widget; place it in Table.children.");
        if (TableWidgetPropertySchema.CELL.equals(child.typeId())
                && !(TableWidgetPropertySchema.ROW.equals(parent.typeId()) && CHILDREN_SLOT.equals(slot.name().value())))
            return rejected(RejectionKind.DIRECT_PARENT_SLOT, "TableCell requires direct TableRow.children in Designer.");

        if (LayoutIdWidgetPropertySchema.TYPE.equals(child.typeId())
                && !(CustomMultiChildLayoutWidgetPropertySchema.TYPE.equals(parent.typeId()) && CHILDREN_SLOT.equals(slot.name().value())))
            return rejected(RejectionKind.DIRECT_PARENT_SLOT, "LayoutId cannot be placed in " + destination
                    + "; it requires direct CustomMultiChildLayout.children.");
        if (!slot.acceptance().accepts(child)) {
            return rejected(
                    RejectionKind.SLOT_ACCEPTANCE,
                    "Catalog slot '" + destination + "' rejects widget type '"
                    + child.typeId().value() + "'.");
        }
        if (isSliverWidget(child)
                && !(slot.acceptance() instanceof SlotAcceptance.HasTrait trait
                && trait.trait().equals(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT))) {
            return rejected(RejectionKind.DIRECT_PARENT_SLOT,
                    "Widget type '" + child.typeId().value() + "' cannot be placed in '"
                    + destination + "'; slivers require a sliver slot.");
        }
        if (SLIVER_CROSS_AXIS_EXPANDED_TYPE.equals(child.typeId().value())
                && !(SliverCrossAxisGroupWidgetPropertySchema.TYPE.equals(parent.typeId())
                    && "slivers".equals(slot.name().value()))) {
            return rejected(RejectionKind.DIRECT_PARENT_SLOT,
                    "SliverCrossAxisExpanded cannot be placed in '" + destination
                    + "'; it requires a direct SliverCrossAxisGroup.slivers parent.");
        }
        if (isStackPositionedWidget(child)
                && !(parent.typeId().value().equals("flutter.widgets.Stack") && CHILDREN_SLOT.equals(slot.name().value())))
            return rejected(RejectionKind.DIRECT_PARENT_SLOT, child.typeId().value()
                    + " cannot be placed in '" + destination + "'; it requires direct Stack.children. IndexedStack inserts intervening render wrappers.");
        if (isFlexRestrictedWidget(child)
                && !isFlexParentDataDestination(parent, slot)) {
            return rejected(
                    RejectionKind.DIRECT_PARENT_SLOT,
                    "Widget type '" + child.typeId().value()
                    + "' cannot be placed in '"
                    + destination + "'; it must be a direct child of "
                    + FLEX_PARENT_DATA_DESTINATIONS + ".");
        }
        return ACCEPTED;
    }

    /** Returns the combined admission result without allocating diagnostics. */
    public static boolean accepts(
            WidgetDefinition parent,
            SlotDefinition slot,
            WidgetDefinition child) {
        return evaluate(parent, slot, child).accepted();
    }

    /** Returns the reviewed Palette creation behavior of one widget. */
    public static PaletteCreationMode creationMode(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return requiredWrapperSlot(definition).isPresent()
                ? PaletteCreationMode.WRAP_EXISTING_CHILD
                : PaletteCreationMode.INSERT_PROTOTYPE;
    }

    /** Whether an empty detached prototype may be inserted directly. */
    public static boolean supportsDirectPrototypeInsertion(WidgetDefinition definition) {
        return creationMode(definition) == PaletteCreationMode.INSERT_PROTOTYPE;
    }

    /**
     * Relational and creation lines appended to one widget's reviewed Canvas
     * schema block. Lines are already in their canonical order.
     */
    public static List<String> capabilityFingerprintLines(WidgetDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        ArrayList<String> lines = new ArrayList<>(3);
        String type = definition.typeId().value();
        if (DataTableWidgetPropertySchema.COLUMN.equals(definition.typeId()))
            lines.add("R|"+type+"|directParentSlot|flutter.material.DataTable,flutter.material.PaginatedDataTable|columns");
        if (DataTableWidgetPropertySchema.row(definition.typeId()))
            lines.add("R|"+type+"|directParentSlot|flutter.material.DataTable|rows");
        if (DataTableWidgetPropertySchema.cell(definition.typeId())) {
            lines.add("R|"+type+"|directParentSlot|flutter.material.DataRow|cells");
            lines.add("R|"+type+"|directParentSlot|flutter.material.DataRow.byIndex|cells");
        }
        if (DataTableWidgetPropertySchema.TYPE.equals(definition.typeId()))
            lines.add("R|"+type+"|rectangularRows|nonemptyColumns|equalCellCount|boundedSortColumn|uniqueKnownRowKeys|exclusiveHeightBounds");
        if (PaginatedDataTableWidgetPropertySchema.TYPE.equals(definition.typeId()))
            lines.add("R|"+type+"|sourceOwnedRows|nonemptyColumns|boundedSortColumn|exclusiveHeightBounds|actionsRequireHeader|pageSizes|controllerPrimary");
        if (DataTableWidgetPropertySchema.supports(definition.typeId()))
            lines.add("C|"+type+"|paletteCreate|dataTable.v1|2|2");
        if (TableWidgetPropertySchema.ROW.equals(definition.typeId()))
            lines.add("R|" + type + "|directParentSlot|flutter.widgets.Table|children");
        if (TableWidgetPropertySchema.CELL.equals(definition.typeId()))
            lines.add("R|" + type + "|directParentSlot|flutter.widgets.TableRow|children");
        if (TableWidgetPropertySchema.TYPE.equals(definition.typeId())) {
            lines.add("R|" + type + "|rectangularRows|positiveEqualCellCount|uniqueRowAndCellKeys|baselineRequiresTextBaseline");
            lines.add("R|" + type + "|columnWidths|tableColumnWidths.v1|depth8|nodes256|index9999");
            lines.add("C|" + type + "|paletteCreate|tableGrid|2|2|48");
        }
        if (TableWidgetPropertySchema.ROW.equals(definition.typeId()))
            lines.add("C|" + type + "|paletteCreate|tableRow|2|48");
        if (isStackPositionedWidget(definition))
            lines.add("R|" + type + "|directParentSlot|flutter.widgets.Stack|children");
        if (isFlexRestrictedWidget(definition)) {
            lines.add("R|" + type
                    + "|directParentSlot|flutter.widgets.Column|children");
            lines.add("R|" + type
                    + "|directParentSlot|flutter.widgets.Row|children");
            lines.add("R|"+type+"|directParentSlot|flutter.material.DataColumn|label");
        }
        if (isSliverWidget(definition)) {
            lines.add("R|" + type + "|requiresSlotTrait|flutter.widgets.Sliver");
        }
        if (SLIVER_CROSS_AXIS_EXPANDED_TYPE.equals(type)) {
            lines.add("R|" + type + "|directParentSlot|flutter.widgets.SliverCrossAxisGroup|slivers");
        }
        if (LayoutIdWidgetPropertySchema.TYPE.equals(definition.typeId())) {
            lines.add("R|" + type + "|directParentSlot|flutter.widgets.CustomMultiChildLayout|children");
            lines.add("C|" + type + "|paletteCreate|seedBoxChild|child|48");
            lines.add("C|" + type + "|paletteCreate|stableNodeId|id");
        }
        if (SliverFloatingHeaderWidgetPropertySchema.TYPE.equals(definition.typeId())) {
            lines.add("C|" + type + "|paletteCreate|seedBoxChild|child|48");
        }
        if (AnimatedCrossFadeWidgetPropertySchema.TYPE.equals(definition.typeId())) {
            lines.add("C|" + type + "|paletteCreate|seedBoxChildren|firstChild|48|48|secondChild|48|80");
        }
        requiredWrapperSlot(definition).ifPresent(slot -> lines.add(
                "C|" + type + "|paletteCreate|wrapExistingChild|"
                + slot.name().value()));
        return List.copyOf(lines);
    }

    public static boolean isStackPositionedWidget(WidgetDefinition definition) {
        return AnimatedPositionedWidgetPropertySchema.supports(definition.typeId())
                || PositionedTransitionWidgetPropertySchema.TYPE.equals(definition.typeId())
                || RelativePositionedTransitionWidgetPropertySchema.TYPE.equals(definition.typeId());
    }

    public static boolean isSliverWidget(WidgetDefinition definition) {
        return definition.traits().contains(BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT);
    }

    private static boolean isFlexRestrictedWidget(WidgetDefinition definition) {
        String type = definition.typeId().value();
        return EXPANDED_TYPE.equals(type)
                || FLEXIBLE_TYPE.equals(type)
                || SPACER_TYPE.equals(type);
    }

    /**
     * Compatibility query restricted to AnyWidget wrappers. Use
     * {@link #requiredWrapperSlot} when sliver-trait wrappers are also supported.
     */
    public static Optional<SlotDefinition> requiredAnyWidgetWrapperSlot(
            WidgetDefinition definition) {
        return requiredWrapperSlot(definition).filter(slot -> slot.acceptance() instanceof SlotAcceptance.AnyWidget);
    }

    /**
     * Detects one required single-child slot from catalog semantics, with
     * AnyWidget or Sliver trait acceptance. A detached prototype is incomplete:
     * Palette creation must wrap an existing compatible child atomically.
     */
    public static Optional<SlotDefinition> requiredWrapperSlot(WidgetDefinition definition) {
        if (DataTableWidgetPropertySchema.descriptor(definition.typeId()) || SliverFloatingHeaderWidgetPropertySchema.TYPE.equals(definition.typeId()) || LayoutIdWidgetPropertySchema.TYPE.equals(definition.typeId())) return Optional.empty();
        if (definition.properties().stream().anyMatch(property ->
                property.parameter().required()
                && property.creationDefault().isEmpty())) {
            return Optional.empty();
        }
        SlotDefinition slot = definition.slots().stream()
                .filter(value -> value.parameter().required() && value.minChildren() == 1)
                .findFirst().orElse(null);
        if (slot == null || !slot.parameter().required()
                || slot.cardinality() != dev.flutter.netbeans.designer.model.SlotCardinality.SINGLE
                || slot.minChildren() != 1
                || slot.maxChildren() != 1
                || !(slot.acceptance() instanceof SlotAcceptance.AnyWidget
                    || slot.acceptance() instanceof SlotAcceptance.HasTrait trait
                        && BuiltInWidgetCatalog.SLIVER_WIDGET_TRAIT.equals(trait.trait()))) {
            return Optional.empty();
        }
        // Optional secondary slots may remain empty in the detached wrapper.
        // A second required/minimum-bearing slot cannot be populated by WrapWidget.
        if (definition.slots().stream().anyMatch(value -> value != slot
                && (value.parameter().required() || value.minChildren() != 0))) {
            return Optional.empty();
        }
        return Optional.of(slot);
    }

    private static boolean isFlexParentDataDestination(
            WidgetDefinition parent,
            SlotDefinition slot) {
        String parentType = parent.typeId().value();
        return CHILDREN_SLOT.equals(slot.name().value())
                && (COLUMN_TYPE.equals(parentType) || ROW_TYPE.equals(parentType))
                || DataTableWidgetPropertySchema.COLUMN.equals(parent.typeId())&&slot.name().value().equals("label");
    }

    private static Decision rejected(RejectionKind kind, String reason) {
        return new Decision(false, Optional.of(kind), reason);
    }
}

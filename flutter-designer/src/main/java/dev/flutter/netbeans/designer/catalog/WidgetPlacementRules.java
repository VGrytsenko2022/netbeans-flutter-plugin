package dev.flutter.netbeans.designer.catalog;

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

    private static final String COLUMN_TYPE = "flutter.widgets.Column";
    private static final String ROW_TYPE = "flutter.widgets.Row";
    private static final String CHILDREN_SLOT = "children";
    private static final String FLEX_PARENT_DATA_DESTINATIONS =
            "'flutter.widgets.Column.children' or 'flutter.widgets.Row.children'";
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
        if (!isFlexParentDataWidget(child)) {
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
        if (!slot.acceptance().accepts(child)) {
            return rejected(
                    RejectionKind.SLOT_ACCEPTANCE,
                    "Catalog slot '" + destination + "' rejects widget type '"
                    + child.typeId().value() + "'.");
        }
        if (isFlexParentDataWidget(child)
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
        return isFlexParentDataWidget(definition)
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
        if (!isFlexParentDataWidget(definition)) {
            return List.of();
        }
        String type = definition.typeId().value();
        return List.of(
                "R|" + type + "|directParentSlot|flutter.widgets.Column|children",
                "R|" + type + "|directParentSlot|flutter.widgets.Row|children",
                "C|" + type + "|paletteCreate|wrapExistingChild|child");
    }

    private static boolean isFlexParentDataWidget(WidgetDefinition definition) {
        String type = definition.typeId().value();
        return EXPANDED_TYPE.equals(type) || FLEXIBLE_TYPE.equals(type);
    }

    private static boolean isFlexParentDataDestination(
            WidgetDefinition parent,
            SlotDefinition slot) {
        String parentType = parent.typeId().value();
        return CHILDREN_SLOT.equals(slot.name().value())
                && (COLUMN_TYPE.equals(parentType) || ROW_TYPE.equals(parentType));
    }

    private static Decision rejected(RejectionKind kind, String reason) {
        return new Decision(false, Optional.of(kind), reason);
    }
}

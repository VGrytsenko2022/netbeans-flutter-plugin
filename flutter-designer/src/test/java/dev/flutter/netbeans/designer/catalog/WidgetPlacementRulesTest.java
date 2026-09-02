package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetPlacementRulesTest {
    @Test
    void expandedIsRootForbiddenAndOnlyAcceptedByDirectFlexChildrenSlots() {
        WidgetDefinition expanded = definition("flutter.widgets.Expanded");
        WidgetDefinition row = definition("flutter.widgets.Row");
        WidgetDefinition column = definition("flutter.widgets.Column");
        WidgetDefinition stack = definition("flutter.widgets.Stack");

        WidgetPlacementRules.Decision root =
                WidgetPlacementRules.evaluateRoot(expanded);
        assertFalse(root.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.ROOT_PLACEMENT,
                root.rejectionKind().orElseThrow());
        assertTrue(root.reason().contains("cannot be the Designer root"));

        assertTrue(WidgetPlacementRules.accepts(
                row, slot(row, "children"), expanded));
        assertTrue(WidgetPlacementRules.accepts(
                column, slot(column, "children"), expanded));

        WidgetPlacementRules.Decision wrong = WidgetPlacementRules.evaluate(
                stack, slot(stack, "children"), expanded);
        assertFalse(wrong.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                wrong.rejectionKind().orElseThrow());
        assertTrue(wrong.reason().contains("flutter.widgets.Stack.children"));
        assertTrue(wrong.reason().contains("flutter.widgets.Column.children"));
        assertTrue(wrong.reason().contains("flutter.widgets.Row.children"));
    }

    @Test
    void flexibleIsRootForbiddenAndOnlyAcceptedByDirectFlexChildrenSlots() {
        WidgetDefinition flexible = definition("flutter.widgets.Flexible");
        WidgetDefinition row = definition("flutter.widgets.Row");
        WidgetDefinition column = definition("flutter.widgets.Column");
        WidgetDefinition stack = definition("flutter.widgets.Stack");

        WidgetPlacementRules.Decision root =
                WidgetPlacementRules.evaluateRoot(flexible);
        assertFalse(root.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.ROOT_PLACEMENT,
                root.rejectionKind().orElseThrow());
        assertTrue(root.reason().contains("flutter.widgets.Flexible"));

        assertTrue(WidgetPlacementRules.accepts(
                row, slot(row, "children"), flexible));
        assertTrue(WidgetPlacementRules.accepts(
                column, slot(column, "children"), flexible));

        WidgetPlacementRules.Decision wrong = WidgetPlacementRules.evaluate(
                stack, slot(stack, "children"), flexible);
        assertFalse(wrong.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                wrong.rejectionKind().orElseThrow());
        assertTrue(wrong.reason().contains("flutter.widgets.Flexible"));
        assertTrue(wrong.reason().contains("flutter.widgets.Stack.children"));
    }

    @Test
    void expandedAndFlexibleCannotBeNestedInsideEachOther() {
        WidgetDefinition expanded = definition("flutter.widgets.Expanded");
        WidgetDefinition flexible = definition("flutter.widgets.Flexible");

        WidgetPlacementRules.Decision flexibleUnderExpanded =
                WidgetPlacementRules.evaluate(
                        expanded, slot(expanded, "child"), flexible);
        assertFalse(flexibleUnderExpanded.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                flexibleUnderExpanded.rejectionKind().orElseThrow());
        assertTrue(flexibleUnderExpanded.reason().contains(
                "flutter.widgets.Expanded.child"));

        WidgetPlacementRules.Decision expandedUnderFlexible =
                WidgetPlacementRules.evaluate(
                        flexible, slot(flexible, "child"), expanded);
        assertFalse(expandedUnderFlexible.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                expandedUnderFlexible.rejectionKind().orElseThrow());
        assertTrue(expandedUnderFlexible.reason().contains(
                "flutter.widgets.Flexible.child"));
    }

    @Test
    void reusableSlotAcceptanceRejectsBeforeContextualPlacement() {
        WidgetDefinition appBar = definition("flutter.material.AppBar");
        WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                appBar,
                slot(appBar, "bottom"),
                definition("flutter.widgets.Expanded"));

        assertFalse(decision.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.SLOT_ACCEPTANCE,
                decision.rejectionKind().orElseThrow());
        assertTrue(decision.reason().contains("flutter.material.AppBar.bottom"));
    }

    @Test
    void expandedUsesAtomicWrapCreationAndCanonicalRelationalFingerprint() {
        WidgetDefinition expanded = definition("flutter.widgets.Expanded");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(expanded));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(expanded));
        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(text));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(text));
        assertEquals(List.of(
                "R|flutter.widgets.Expanded|directParentSlot|flutter.widgets.Column|children",
                "R|flutter.widgets.Expanded|directParentSlot|flutter.widgets.Row|children",
                "C|flutter.widgets.Expanded|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(expanded));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(text));
    }

    @Test
    void flexibleUsesAtomicWrapCreationAndCanonicalRelationalFingerprint() {
        WidgetDefinition flexible = definition("flutter.widgets.Flexible");

        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(flexible));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(flexible));
        assertEquals(List.of(
                "R|flutter.widgets.Flexible|directParentSlot|flutter.widgets.Column|children",
                "R|flutter.widgets.Flexible|directParentSlot|flutter.widgets.Row|children",
                "C|flutter.widgets.Flexible|paletteCreate|wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(flexible));
    }

    private static WidgetDefinition definition(String type) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(type))
                .orElseThrow();
    }

    private static SlotDefinition slot(WidgetDefinition definition, String name) {
        return definition.slot(new SlotName(name)).orElseThrow();
    }
}

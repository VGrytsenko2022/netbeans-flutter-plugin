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

    private static WidgetDefinition definition(String type) {
        return BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId(type))
                .orElseThrow();
    }

    private static SlotDefinition slot(WidgetDefinition definition, String name) {
        return definition.slot(new SlotName(name)).orElseThrow();
    }
}

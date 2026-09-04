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

    @Test
    void spacerIsAnInsertableLeafRestrictedToDirectFlexChildrenSlots() {
        WidgetDefinition spacer = definition("flutter.widgets.Spacer");
        WidgetDefinition row = definition("flutter.widgets.Row");
        WidgetDefinition column = definition("flutter.widgets.Column");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition expanded = definition("flutter.widgets.Expanded");

        WidgetPlacementRules.Decision root = WidgetPlacementRules.evaluateRoot(spacer);
        assertFalse(root.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.ROOT_PLACEMENT,
                root.rejectionKind().orElseThrow());
        assertTrue(WidgetPlacementRules.accepts(row, slot(row, "children"), spacer));
        assertTrue(WidgetPlacementRules.accepts(
                column, slot(column, "children"), spacer));

        WidgetPlacementRules.Decision stackDecision = WidgetPlacementRules.evaluate(
                stack, slot(stack, "children"), spacer);
        assertFalse(stackDecision.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                stackDecision.rejectionKind().orElseThrow());
        WidgetPlacementRules.Decision expandedDecision = WidgetPlacementRules.evaluate(
                expanded, slot(expanded, "child"), spacer);
        assertFalse(expandedDecision.accepted());
        assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                expandedDecision.rejectionKind().orElseThrow());

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(spacer));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(spacer));
        assertEquals(List.of(
                "R|flutter.widgets.Spacer|directParentSlot|flutter.widgets.Column|children",
                "R|flutter.widgets.Spacer|directParentSlot|flutter.widgets.Row|children"),
                WidgetPlacementRules.capabilityFingerprintLines(spacer));
    }

    @Test
    void baselineIsAnOrdinaryInsertableWidgetWithARestrictedAnyWidgetChild() {
        WidgetDefinition baseline = definition("flutter.widgets.Baseline");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(baseline).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), baseline));
        assertTrue(WidgetPlacementRules.accepts(
                baseline, slot(baseline, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    baseline,
                    slot(baseline, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.Baseline.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(baseline));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(baseline));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(baseline));
    }

    @Test
    void intrinsicHeightIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition intrinsicHeight = definition(
                "flutter.widgets.IntrinsicHeight");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(intrinsicHeight).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), intrinsicHeight));
        assertTrue(WidgetPlacementRules.accepts(
                intrinsicHeight, slot(intrinsicHeight, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    intrinsicHeight,
                    slot(intrinsicHeight, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.IntrinsicHeight.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(intrinsicHeight));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(
                intrinsicHeight));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(intrinsicHeight));
    }

    @Test
    void intrinsicWidthIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition intrinsicWidth = definition(
                "flutter.widgets.IntrinsicWidth");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(intrinsicWidth).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), intrinsicWidth));
        assertTrue(WidgetPlacementRules.accepts(
                intrinsicWidth, slot(intrinsicWidth, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    intrinsicWidth,
                    slot(intrinsicWidth, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.IntrinsicWidth.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(intrinsicWidth));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(
                intrinsicWidth));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(intrinsicWidth));
    }

    @Test
    void offstageIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition offstage = definition("flutter.widgets.Offstage");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(offstage).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), offstage));
        assertTrue(WidgetPlacementRules.accepts(
                offstage, slot(offstage, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    offstage,
                    slot(offstage, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.Offstage.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(offstage));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(offstage));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(offstage));
    }

    @Test
    void sizedOverflowBoxIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition box = definition("flutter.widgets.SizedOverflowBox");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(box).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), box));
        assertTrue(WidgetPlacementRules.accepts(
                box, slot(box, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    box,
                    slot(box, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.SizedOverflowBox.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(box));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(box));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(box));
    }

    @Test
    void transformIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition transform = definition("flutter.widgets.Transform");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(transform).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), transform));
        assertTrue(WidgetPlacementRules.accepts(
                transform, slot(transform, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    transform,
                    slot(transform, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.Transform.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(transform));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(transform));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(transform));
    }

    @Test
    void rotatedBoxIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChild() {
        WidgetDefinition rotated = definition("flutter.widgets.RotatedBox");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(rotated).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), rotated));
        assertTrue(WidgetPlacementRules.accepts(
                rotated, slot(rotated, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    rotated,
                    slot(rotated, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.RotatedBox.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(rotated));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(rotated));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(rotated));
    }

    @Test
    void listBodyIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChildren() {
        WidgetDefinition listBody = definition("flutter.widgets.ListBody");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(listBody).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), listBody));
        assertTrue(WidgetPlacementRules.accepts(
                listBody, slot(listBody, "children"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    listBody,
                    slot(listBody, "children"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.ListBody.children"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(listBody));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(listBody));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(listBody));
    }

    @Test
    void overflowBarIsAnOrdinaryInsertableWidgetWithRestrictedAnyWidgetChildren() {
        WidgetDefinition overflowBar = definition("flutter.widgets.OverflowBar");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(overflowBar).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), overflowBar));
        assertTrue(WidgetPlacementRules.accepts(
                overflowBar, slot(overflowBar, "children"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    overflowBar,
                    slot(overflowBar, "children"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.OverflowBar.children"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(overflowBar));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(overflowBar));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(overflowBar));
    }

    @Test
    void gridViewIsAnOrdinaryInsertableWidgetWithRestrictedOrderedChildren() {
        WidgetDefinition gridView = definition("flutter.widgets.GridView");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(gridView).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), gridView));
        assertTrue(WidgetPlacementRules.accepts(
                gridView, slot(gridView, "children"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    gridView,
                    slot(gridView, "children"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.GridView.children"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(gridView));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(gridView));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(gridView));
    }

    @Test
    void singleChildScrollViewIsAnOrdinaryInsertableWidgetWithRestrictedChild() {
        WidgetDefinition scrollView = definition(
                "flutter.widgets.SingleChildScrollView");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(scrollView).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), scrollView));
        assertTrue(WidgetPlacementRules.accepts(
                scrollView, slot(scrollView, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    scrollView,
                    slot(scrollView, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.SingleChildScrollView.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(scrollView));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(scrollView));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(scrollView));
    }

    @Test
    void coloredBoxIsAnOrdinaryInsertableWidgetWithRestrictedChild() {
        WidgetDefinition coloredBox = definition("flutter.widgets.ColoredBox");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(coloredBox).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), coloredBox));
        assertTrue(WidgetPlacementRules.accepts(
                coloredBox, slot(coloredBox, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    coloredBox,
                    slot(coloredBox, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.ColoredBox.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(coloredBox));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(coloredBox));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(coloredBox));
    }

    @Test
    void placeholderIsAnOrdinaryInsertableWidgetWithOptionalRestrictedChild() {
        WidgetDefinition placeholder = definition("flutter.widgets.Placeholder");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(placeholder).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), placeholder));
        assertTrue(WidgetPlacementRules.accepts(
                placeholder, slot(placeholder, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    placeholder,
                    slot(placeholder, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.Placeholder.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(placeholder));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(placeholder));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(placeholder));
    }

    @Test
    void decoratedBoxIsAnOrdinaryInsertableWidgetWithOptionalRestrictedChild() {
        WidgetDefinition decoratedBox = definition("flutter.widgets.DecoratedBox");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(decoratedBox).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), decoratedBox));
        assertTrue(WidgetPlacementRules.accepts(
                decoratedBox, slot(decoratedBox, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    decoratedBox,
                    slot(decoratedBox, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.DecoratedBox.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(decoratedBox));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(
                decoratedBox));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(decoratedBox));
    }

    @Test
    void excludeSemanticsIsAnOrdinaryInsertableWidgetWithOptionalRestrictedChild() {
        WidgetDefinition excludeSemantics = definition(
                "flutter.widgets.ExcludeSemantics");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(excludeSemantics).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), excludeSemantics));
        assertTrue(WidgetPlacementRules.accepts(
                excludeSemantics, slot(excludeSemantics, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    excludeSemantics,
                    slot(excludeSemantics, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.ExcludeSemantics.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.INSERT_PROTOTYPE,
                WidgetPlacementRules.creationMode(excludeSemantics));
        assertTrue(WidgetPlacementRules.supportsDirectPrototypeInsertion(
                excludeSemantics));
        assertEquals(List.of(),
                WidgetPlacementRules.capabilityFingerprintLines(excludeSemantics));
    }

    @Test
    void safeAreaUsesGenericRequiredAnyWidgetAtomicWrapperCreation() {
        WidgetDefinition safeArea = definition("flutter.widgets.SafeArea");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(safeArea).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), safeArea));
        assertTrue(WidgetPlacementRules.accepts(
                safeArea, slot(safeArea, "child"), text));
        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    safeArea,
                    slot(safeArea, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.SafeArea.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(safeArea));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(safeArea));
        assertEquals(List.of(
                        "C|flutter.widgets.SafeArea|paletteCreate|"
                        + "wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(safeArea));
    }

    @Test
    void directionalityUsesGenericRequiredAnyWidgetAtomicWrapperCreation() {
        WidgetDefinition directionality = definition(
                "flutter.widgets.Directionality");
        WidgetDefinition stack = definition("flutter.widgets.Stack");
        WidgetDefinition text = definition("flutter.widgets.Text");

        assertTrue(WidgetPlacementRules.evaluateRoot(directionality).accepted());
        assertTrue(WidgetPlacementRules.accepts(
                stack, slot(stack, "children"), directionality));
        assertTrue(WidgetPlacementRules.accepts(
                directionality, slot(directionality, "child"), text));

        for (String restricted : List.of(
                "flutter.widgets.Expanded",
                "flutter.widgets.Flexible",
                "flutter.widgets.Spacer")) {
            WidgetPlacementRules.Decision decision = WidgetPlacementRules.evaluate(
                    directionality,
                    slot(directionality, "child"),
                    definition(restricted));
            assertFalse(decision.accepted(), restricted);
            assertEquals(WidgetPlacementRules.RejectionKind.DIRECT_PARENT_SLOT,
                    decision.rejectionKind().orElseThrow());
            assertTrue(decision.reason().contains(
                    "flutter.widgets.Directionality.child"), decision.reason());
        }

        assertEquals(WidgetPlacementRules.PaletteCreationMode.WRAP_EXISTING_CHILD,
                WidgetPlacementRules.creationMode(directionality));
        assertFalse(WidgetPlacementRules.supportsDirectPrototypeInsertion(
                directionality));
        assertEquals(List.of(
                        "C|flutter.widgets.Directionality|paletteCreate|"
                        + "wrapExistingChild|child"),
                WidgetPlacementRules.capabilityFingerprintLines(directionality));
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

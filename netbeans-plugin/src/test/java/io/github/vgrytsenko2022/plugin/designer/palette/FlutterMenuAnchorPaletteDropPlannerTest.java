package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.plugin.designer.properties.MenuAnchorPropertyContractTest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlutterMenuAnchorPaletteDropPlannerTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private final FlutterDesignerPaletteDropPlanner planner = new FlutterDesignerPaletteDropPlanner();

    @Test void ordinaryAddPreservesEmptyRequiredListAndOptionalChildWithoutFabricatingAnOpener() {
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of()); var id = StableId.random();
        var accepted = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, planner.plan(document(root), CATALOG,
                MenuAnchorWidgetPropertySchema.MENU_ANCHOR_TYPE, root.id(), new SlotName("children"), 0, () -> id));
        var node = accepted.command().widget(); assertEquals(id, node.id()); assertTrue(node.properties().isEmpty());
        assertEquals(Set.of(new SlotName("child"), new SlotName("menuChildren")), node.slots().keySet());
        assertTrue(((WidgetSlot.SingleSlot) node.slots().get(new SlotName("child"))).child().isEmpty());
        assertTrue(((WidgetSlot.ListSlot) node.slots().get(new SlotName("menuChildren"))).children().isEmpty());
    }

    @Test void menuListAppendsInOrderAndRejectsNonterminalOrOccupiedStaleTargetsWithoutAllocating() {
        var base = MenuAnchorPropertyContractTest.prototype(); var first = text("First"); var second = text("Second");
        var slots = new LinkedHashMap<>(base.slots()); slots.put(new SlotName("menuChildren"), new WidgetSlot.ListSlot(List.of(first, second))); slots.put(new SlotName("child"), WidgetSlot.SingleSlot.of(text("Anchor")));
        var root = new WidgetNode(base.id(), base.type(), base.properties(), slots); var snapshot = document(root); var id = StableId.random();
        var planned = planner.plan(snapshot, CATALOG, MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE, root.id(), new SlotName("menuChildren"), 2, () -> id);
        var result = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Accepted.class, planned, planned.toString());
        assertEquals(2, result.command().destination().index()); assertEquals(id, result.command().widget().id());
        assertEquals(List.of(first, second), ((WidgetSlot.ListSlot) root.slots().get(new SlotName("menuChildren"))).children());
        var allocations = new AtomicInteger();
        var nonterminal = assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class, planner.plan(snapshot, CATALOG,
                MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE, root.id(), new SlotName("menuChildren"), 1, () -> { allocations.incrementAndGet(); return id; }));
        assertEquals(FlutterDesignerPaletteDropPlanner.RejectionCode.NON_TERMINAL_INSERTION, nonterminal.code());
        for (StableId target : List.of(root.id(), StableId.random())) assertInstanceOf(FlutterDesignerPaletteDropPlanner.Rejected.class,
                planner.plan(snapshot, CATALOG, MenuItemButtonWidgetPropertySchema.MENU_ITEM_BUTTON_TYPE, target, new SlotName("child"), 0, () -> { allocations.incrementAndGet(); return id; }));
        assertEquals(0, allocations.get()); assertEquals(slots, root.slots());
    }
    private static WidgetNode text(String value) { return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"), Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of()); }
    private static DesignerDocument document(WidgetNode root) { return new DesignerDocument(StableId.random(), new DartSourceDescriptor("screen.dart", "Screen", WidgetClassKind.STATELESS, Optional.empty(),
            new ManagedRegions(new ManagedRegion("0".repeat(64)), new ManagedRegion("0".repeat(64)))), root); }
}

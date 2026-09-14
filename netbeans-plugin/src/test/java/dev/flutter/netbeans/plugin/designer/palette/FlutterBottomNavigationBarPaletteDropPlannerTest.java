package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BottomNavigationBarWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.AddWidget;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Palette placement coverage for the BottomNavigationBar list surface. */
class FlutterBottomNavigationBarPaletteDropPlannerTest {
    private static final WidgetTypeId COLUMN = new WidgetTypeId("flutter.widgets.Column");
    private static final SlotName CHILDREN = new SlotName("children");
    private static final SlotName ITEMS = new SlotName("items");
    private static final WidgetTypeId TYPE = BottomNavigationBarWidgetPropertySchema
            .BOTTOM_NAVIGATION_BAR_TYPE;
    private final FlutterDesignerPaletteDropPlanner planner =
            new FlutterDesignerPaletteDropPlanner();

    @Test
    void ordinaryAddCreatesOnlyReviewedDefaultAndEmptyItemsList() {
        StableId rootId = StableId.random();
        StableId newId = StableId.random();
        WidgetNode root = new WidgetNode(rootId, COLUMN, Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())));

        FlutterDesignerPaletteDropPlanner.Accepted result = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(root), BuiltInWidgetCatalog.getDefault(), TYPE,
                        rootId, CHILDREN, 0, () -> newId));
        WidgetNode created = result.command().widget();
        assertEquals(TYPE, created.type());
        assertEquals(Map.of(new PropertyName("currentIndex"),
                        new PropertyValue.IntegerValue(BigInteger.ZERO)),
                created.properties());
        assertEquals(List.of(), ((WidgetSlot.ListSlot) created.slots().get(ITEMS)).children());
    }

    @Test
    void itemsSlotAppendsChildrenInOrderAndKeepsExistingModelUntouched() {
        StableId rootId = StableId.random();
        WidgetNode first = text("First");
        WidgetNode second = text("Second");
        LinkedHashMap<SlotName, WidgetSlot> slots = new LinkedHashMap<>();
        slots.put(ITEMS, new WidgetSlot.ListSlot(List.of(first, second)));
        WidgetNode bar = new WidgetNode(rootId, TYPE,
                Map.of(new PropertyName("currentIndex"),
                        new PropertyValue.IntegerValue(BigInteger.ZERO)), slots);
        StableId newId = StableId.random();
        FlutterDesignerPaletteDropPlanner.Accepted result = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class,
                planner.plan(document(bar), BuiltInWidgetCatalog.getDefault(),
                        new WidgetTypeId("flutter.widgets.Text"), rootId, ITEMS, 2,
                        () -> newId));
        AddWidget command = result.command();
        assertEquals(2, command.destination().index());
        assertEquals(newId, command.widget().id());
        assertEquals(List.of(first, second), ((WidgetSlot.ListSlot) bar.slots().get(ITEMS)).children());
        assertTrue(command.widget().properties().containsKey(new PropertyName("data")));
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)), Map.of());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion hash = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(StableId.random(),
                new DartSourceDescriptor("screen.dart", "Screen", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(hash, hash)), root);
    }
}

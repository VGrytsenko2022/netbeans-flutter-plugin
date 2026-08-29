package dev.flutter.netbeans.plugin.designer.palette;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for re-adding a child after deleting Center's Text. */
class FlutterDesignerPaletteCenterReaddRegressionTest {
    private static final StableId DOCUMENT_ID = StableId.parse(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId CENTER_ID = StableId.parse(
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final StableId REPLACEMENT_TEXT_ID = StableId.parse(
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final WidgetTypeId CENTER = new WidgetTypeId(
            "flutter.widgets.Center");
    private static final WidgetTypeId TEXT = new WidgetTypeId(
            "flutter.widgets.Text");
    private static final SlotName CHILD = new SlotName("child");
    private static final PropertyName DATA = new PropertyName("data");

    @Test
    void plansTextIntoTheEmptyCenterChildLeftAfterDeletingItsText() {
        // Removing an optional single child removes the model slot entirely.
        // This is the exact semantic Center state observed after Delete.
        DesignerDocument afterDelete = document(WidgetNode.empty(
                CENTER_ID, CENTER));

        FlutterDesignerPaletteDropPlanner.Result result =
                new FlutterDesignerPaletteDropPlanner().plan(
                        afterDelete,
                        BuiltInWidgetCatalog.getDefault(),
                        TEXT,
                        CENTER_ID,
                        CHILD,
                        0,
                        () -> REPLACEMENT_TEXT_ID);

        FlutterDesignerPaletteDropPlanner.Accepted accepted = assertInstanceOf(
                FlutterDesignerPaletteDropPlanner.Accepted.class,
                result,
                () -> result instanceof FlutterDesignerPaletteDropPlanner.Rejected rejected
                        ? rejected.code() + ": " + rejected.reason()
                        : "Expected an accepted Center.child drop");
        AddWidget add = accepted.command();
        assertEquals(CENTER_ID, add.destination().parentId());
        assertEquals(CHILD, add.destination().slotName());
        assertEquals(0, add.destination().index());
        assertEquals(REPLACEMENT_TEXT_ID, add.widget().id());
        assertEquals(TEXT, add.widget().type());
        assertEquals(
                new PropertyValue.StringValue("Text"),
                add.widget().properties().get(DATA));
        assertTrue(add.widget().slots().isEmpty());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion emptyHash = new ManagedRegion("0".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.empty(),
                new ManagedRegions(emptyHash, emptyHash));
        return new DesignerDocument(DOCUMENT_ID, source, root);
    }
}

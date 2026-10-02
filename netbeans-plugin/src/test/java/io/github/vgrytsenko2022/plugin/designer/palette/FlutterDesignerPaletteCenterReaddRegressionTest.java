package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.command.AddWidget;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
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

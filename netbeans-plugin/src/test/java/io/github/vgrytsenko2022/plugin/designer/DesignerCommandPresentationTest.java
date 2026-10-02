package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.command.ClearSlotChildren;
import io.github.vgrytsenko2022.designer.command.CreateMenuAnchorBuilder;
import io.github.vgrytsenko2022.designer.command.DesignerCommandKind;
import io.github.vgrytsenko2022.designer.command.ReplaceSlotChild;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesignerCommandPresentationTest {
    private static final StableId OWNER = id(
            "00000000-0000-4000-8000-000000000010");
    private static final StableId CURRENT = id(
            "00000000-0000-4000-8000-000000000011");
    private static final StableId SOURCE = id(
            "00000000-0000-4000-8000-000000000012");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void compoundSlotCommandsHaveConcreteStablePresentation() {
        ReplaceSlotChild replace = new ReplaceSlotChild(
                OWNER,
                CHILD,
                CURRENT,
                new ReplaceSlotChild.ExistingWidget(SOURCE));
        ClearSlotChildren clear = new ClearSlotChildren(
                OWNER,
                CHILDREN,
                List.of(CURRENT, SOURCE));

        assertEquals("Replace Flutter Slot Child",
                DesignerCommandPresentation.title(replace.kind()));
        assertEquals("Replace Flutter slot child",
                DesignerCommandPresentation.operation(replace));
        assertEquals(OWNER + ".child (expected " + CURRENT + ")",
                DesignerCommandPresentation.target(replace));

        assertEquals("Clear Flutter Slot Children",
                DesignerCommandPresentation.title(clear.kind()));
        assertEquals("Clear Flutter slot children",
                DesignerCommandPresentation.operation(clear));
        assertEquals(OWNER + ".children (2 children)",
                DesignerCommandPresentation.target(clear));
    }

    @Test
    void menuBuilderCreationNamesTheExactWidgetPropertyAndMethod() {
        var command = new CreateMenuAnchorBuilder(OWNER, "_buildMenu");
        assertEquals("Create Menu Anchor Builder", DesignerCommandPresentation.title(command.kind()));
        assertEquals("Create MenuAnchor builder", DesignerCommandPresentation.operation(command));
        assertEquals(OWNER + ".builder -> _buildMenu", DesignerCommandPresentation.target(command));
    }

    @Test
    void titlePresentationCoversEveryClosedCommandKind() {
        assertEquals(DesignerCommandKind.values().length,
                List.of(DesignerCommandKind.values()).stream()
                        .map(DesignerCommandPresentation::title)
                        .distinct()
                        .count());
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }
}

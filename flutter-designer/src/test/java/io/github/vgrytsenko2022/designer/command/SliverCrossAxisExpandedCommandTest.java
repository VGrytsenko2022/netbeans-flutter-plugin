package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.SliverCrossAxisExpandedContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class SliverCrossAxisExpandedCommandTest {
    @Test void wrappingIsAtomicAndRequiredChildCannotBeClearedOrMovedOut() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var transform = new DesignerCommandTransformer(catalog, ValidationLimits.defaults());
        var child = adapter();
        var doc = document(child);
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var result = transform.apply(doc, new WrapWidget(child.id(), prototype, SLIVER, 0));
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.toString());
        var wrapped = result.document().orElseThrow();
        var group = ((WidgetSlot.ListSlot) wrapped.root().slots().get(SLIVERS)).children().getFirst();
        var owner = ((WidgetSlot.ListSlot) group.slots().get(SLIVERS)).children().getFirst();
        assertEquals(prototype.id(), owner.id());
        assertEquals(child, ((WidgetSlot.SingleSlot)owner.slots().get(SLIVER)).child().orElseThrow());
        for (DesignerCommand rejected : java.util.List.of(new RemoveWidget(child.id()),
                new ClearSlotChildren(owner.id(), SLIVER, java.util.List.of(child.id())),
                new MoveWidget(child.id(), new WidgetPlacement(group.id(), SLIVERS, 1)),
                new SetProperty(owner.id(), new PropertyName("flex"), integer(0)),
                new ResetProperty(owner.id(), new PropertyName("flex")),
                new WrapWidget(child.id(), WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random()), SLIVER, 0),
                new MoveWidget(owner.id(), new WidgetPlacement(wrapped.root().id(), SLIVERS, 0)))) {
            assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(wrapped, rejected).status(), rejected.toString());
        }
        var replacement = adapter();
        var replaced = transform.apply(wrapped, new ReplaceSlotChild(owner.id(), SLIVER, child.id(), new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(DesignerCommandStatus.APPLIED, replaced.status(), replaced.toString());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(wrapped,
                new SetProperty(owner.id(), new PropertyName("flex"), integer(7))).status());
        var plain = document(adapter());
        // An ordinary viewport destination cannot acquire this ParentData wrapper.
        var target = ((WidgetSlot.ListSlot) plain.root().slots().get(SLIVERS)).children().getFirst();
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(plain,
                new WrapWidget(target.id(), prototype, SLIVER, 0)).status());
    }
}

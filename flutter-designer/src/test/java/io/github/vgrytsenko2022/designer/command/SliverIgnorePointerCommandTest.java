package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.SliverIgnorePointerContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class SliverIgnorePointerCommandTest {
    @Test void optionalSliverSupportsInsertRemoveReplaceMoveAndNestedWrapping() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var transform = new DesignerCommandTransformer(catalog, ValidationLimits.defaults());
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var doc = document(adapter()); var owner = doc.root().id();
        var added = transform.apply(doc, new AddWidget(new WidgetPlacement(owner, SLIVERS, 1), prototype));
        assertEquals(DesignerCommandStatus.APPLIED, added.status(), added.toString());
        var empty = added.document().orElseThrow(); var child = adapter();
        var inserted = transform.apply(empty, new AddWidget(new WidgetPlacement(prototype.id(), SLIVER, 0), child));
        assertEquals(DesignerCommandStatus.APPLIED, inserted.status(), inserted.toString());
        var full = inserted.document().orElseThrow();
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new RemoveWidget(child.id())).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new MoveWidget(child.id(), new WidgetPlacement(owner, SLIVERS, 2))).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new ReplaceSlotChild(prototype.id(), SLIVER, child.id(), new ReplaceSlotChild.NewSubtree(adapter()))).status());
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new AddWidget(new WidgetPlacement(prototype.id(), SLIVER, 0), adapter())).status());
        var inner = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new WrapWidget(child.id(), inner, SLIVER, 0)).status());
        for (var property : catalog.find(TYPE).orElseThrow().properties()) {
            for (boolean flag : List.of(false, true)) {
                var changed = transform.apply(full, new SetProperty(prototype.id(), property.name(), new PropertyValue.BooleanValue(flag)));
                assertEquals(DesignerCommandStatus.APPLIED, changed.status());
                assertEquals(DesignerCommandStatus.APPLIED, transform.apply(changed.document().orElseThrow(),
                        new ResetProperty(prototype.id(), property.name())).status());
            }
        }
        var explicitNull = transform.apply(full, new SetProperty(prototype.id(), new PropertyName("ignoringSemantics"), new PropertyValue.NullValue()));
        assertEquals(DesignerCommandStatus.APPLIED, explicitNull.status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(explicitNull.document().orElseThrow(),
                new ResetProperty(prototype.id(), new PropertyName("ignoringSemantics"))).status());
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new SetProperty(prototype.id(), new PropertyName("ignoring"), new PropertyValue.NullValue())).status());
    }
}


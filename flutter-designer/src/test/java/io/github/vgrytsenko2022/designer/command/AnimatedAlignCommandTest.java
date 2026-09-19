package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.AnimatedAlignContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedAlignCommandTest {
    @Test void optionalChildSupportsInsertRemoveReplaceMoveAndNestedWrapping() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var transform = new DesignerCommandTransformer(catalog, ValidationLimits.defaults());
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var doc = document(adapter()); var owner = doc.root().id();
        var added = transform.apply(doc, new AddWidget(new WidgetPlacement(owner, CHILDREN, 1), prototype));
        assertEquals(DesignerCommandStatus.APPLIED, added.status(), added.toString());
        var empty = added.document().orElseThrow(); var child = adapter();
        var inserted = transform.apply(empty, new AddWidget(new WidgetPlacement(prototype.id(), CHILD, 0), child));
        assertEquals(DesignerCommandStatus.APPLIED, inserted.status(), inserted.toString());
        var full = inserted.document().orElseThrow();
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new RemoveWidget(child.id())).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new MoveWidget(child.id(), new WidgetPlacement(owner, CHILDREN, 2))).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new ReplaceSlotChild(prototype.id(), CHILD, child.id(), new ReplaceSlotChild.NewSubtree(adapter()))).status());
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new AddWidget(new WidgetPlacement(prototype.id(), CHILD, 0), adapter())).status());
        var inner = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new WrapWidget(child.id(), inner, CHILD, 0)).status());
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new ResetProperty(prototype.id(), new PropertyName("alignment"))).status());
        for (String value : List.of("0", "0.5", "1")) assertEquals(value.equals("0") ? DesignerCommandStatus.NO_CHANGE : DesignerCommandStatus.APPLIED,
                transform.apply(full, new SetProperty(prototype.id(), new PropertyName("alignment"), alignment(false, value, value))).status());
        var semantics = new PropertyName("widthFactor");
        for (boolean value : List.of(true, false)) {
            var changed = transform.apply(full, new SetProperty(prototype.id(), semantics, value ? new PropertyValue.NullValue() : number("2")));
            assertEquals(DesignerCommandStatus.APPLIED, changed.status());
            assertEquals(DesignerCommandStatus.APPLIED, transform.apply(changed.document().orElseThrow(),
                    new ResetProperty(prototype.id(), semantics)).status());
        }
    }
}



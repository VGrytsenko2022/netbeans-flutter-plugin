package dev.flutter.netbeans.designer.command;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.AnimatedSwitcherContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedSwitcherCommandTest {
    @Test void optionalChildSupportsInsertRemoveReplaceMoveAndNestedWrapping() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var transform = new DesignerCommandTransformer(catalog, ValidationLimits.defaults());
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var doc = AnimatedFractionallySizedBoxContractTest.document(AnimatedFractionallySizedBoxContractTest.adapter()); var owner = doc.root().id();
        var added = transform.apply(doc, new AddWidget(new WidgetPlacement(owner, new SlotName("children"), 1), prototype));
        assertEquals(DesignerCommandStatus.APPLIED, added.status(), added.toString());
        var empty = added.document().orElseThrow(); var child = AnimatedFractionallySizedBoxContractTest.adapter();
        var inserted = transform.apply(empty, new AddWidget(new WidgetPlacement(prototype.id(), CHILD, 0), child));
        assertEquals(DesignerCommandStatus.APPLIED, inserted.status(), inserted.toString());
        var full = inserted.document().orElseThrow();
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new RemoveWidget(child.id())).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new MoveWidget(child.id(), new WidgetPlacement(owner, new SlotName("children"), 2))).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new ReplaceSlotChild(prototype.id(), CHILD, child.id(), new ReplaceSlotChild.NewSubtree(AnimatedFractionallySizedBoxContractTest.adapter()))).status());
        assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(full,
                new AddWidget(new WidgetPlacement(prototype.id(), CHILD, 0), AnimatedFractionallySizedBoxContractTest.adapter())).status());
        var inner = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(full, new WrapWidget(child.id(), inner, CHILD, 0)).status());
    }
}

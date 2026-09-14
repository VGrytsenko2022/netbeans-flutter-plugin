package dev.flutter.netbeans.designer.command;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.SliverSafeAreaContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class SliverSafeAreaCommandTest {
    @Test void atomicWrappingReplacementAndRequiredChildProtection() {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var transform = new DesignerCommandTransformer(catalog, ValidationLimits.defaults());
        var child = adapter(); var doc = document(child);
        var prototype = WidgetNodePrototypeFactory.create(catalog.find(TYPE).orElseThrow(), StableId.random());
        var result = transform.apply(doc, new WrapWidget(child.id(), prototype, SLIVER, 0));
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.toString());
        var wrapped = result.document().orElseThrow();
        var group = ((WidgetSlot.ListSlot) wrapped.root().slots().get(SLIVERS)).children().getFirst();
        var owner = ((WidgetSlot.ListSlot) group.slots().get(SLIVERS)).children().getFirst();
        assertEquals(prototype.id(), owner.id());
        assertEquals(child, ((WidgetSlot.SingleSlot)owner.slots().get(SLIVER)).child().orElseThrow());
        for (DesignerCommand rejected : java.util.List.of(new RemoveWidget(child.id()),
                new MoveWidget(child.id(), new WidgetPlacement(group.id(), SLIVERS, 1)),
                new SetProperty(owner.id(), new PropertyName("left"), new PropertyValue.NullValue()))) {
            assertNotEquals(DesignerCommandStatus.APPLIED, transform.apply(wrapped, rejected).status(), rejected.toString());
        }
        for(var property:catalog.find(TYPE).orElseThrow().properties()) {
            PropertyValue value=property.name().value().equals("minimum")?minimum():new PropertyValue.BooleanValue(false);
            var changed=transform.apply(wrapped,new SetProperty(owner.id(),property.name(),value));
            assertEquals(DesignerCommandStatus.APPLIED,changed.status());
            var reset=transform.apply(changed.document().orElseThrow(),new ResetProperty(owner.id(),property.name()));
            assertEquals(DesignerCommandStatus.APPLIED,reset.status());assertEquals(wrapped,reset.document().orElseThrow());
        }
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(wrapped, new ReplaceSlotChild(owner.id(), SLIVER,
                child.id(), new ReplaceSlotChild.NewSubtree(adapter()))).status());
        assertEquals(DesignerCommandStatus.APPLIED, transform.apply(wrapped, new MoveWidget(owner.id(),
                new WidgetPlacement(wrapped.root().id(), SLIVERS, 1))).status());
    }
}


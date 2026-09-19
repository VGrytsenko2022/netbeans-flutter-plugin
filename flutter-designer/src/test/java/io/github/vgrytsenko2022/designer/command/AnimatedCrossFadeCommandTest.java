package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.AnimatedCrossFadeContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedCrossFadeCommandTest {
    @Test void seededPaletteInsertAtomicReplacementAndRequiredChildGuards() {
        var catalog=BuiltInWidgetCatalog.getDefault();var transform=new DesignerCommandTransformer(catalog,ValidationLimits.defaults());
        var prototype=node();var sibling=AnimatedFractionallySizedBoxContractTest.adapter();
        var doc=AnimatedFractionallySizedBoxContractTest.document(sibling);var target=new SlotName("children");
        var added=transform.apply(doc,new AddWidget(new WidgetPlacement(doc.root().id(),target,1),prototype));
        assertEquals(DesignerCommandStatus.APPLIED,added.status(),added.toString());var full=added.document().orElseThrow();
        for(var slot:List.of(FIRST,SECOND)){
            var seed=((WidgetSlot.SingleSlot)prototype.slots().get(slot)).child().orElseThrow();
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new RemoveWidget(seed.id())).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new MoveWidget(seed.id(),new WidgetPlacement(doc.root().id(),target,2))).status());
            assertEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new ReplaceSlotChild(prototype.id(),slot,seed.id(),new ReplaceSlotChild.NewSubtree(AnimatedFractionallySizedBoxContractTest.adapter()))).status());
            var wrapper=WidgetNodePrototypeFactory.create(catalog.find(AnimatedAlignWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
            assertEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new WrapWidget(seed.id(),wrapper,new SlotName("child"),0)).status());
        }
        assertEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new SetProperty(prototype.id(),new PropertyName("crossFadeState"),new PropertyValue.EnumValue("CrossFadeState","showSecond"))).status());
        assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new ResetProperty(prototype.id(),new PropertyName("crossFadeState"))).status());
    }
}

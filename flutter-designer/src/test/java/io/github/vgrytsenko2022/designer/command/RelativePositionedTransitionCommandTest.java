package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.RelativePositionedTransitionContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class RelativePositionedTransitionCommandTest {
    @Test void wrapsOnlyDirectStackChildrenAndPreservesRequiredChildOnMoveRemoveReplace(){
        var transform=new DesignerCommandTransformer(C,ValidationLimits.defaults());
        for(var type:List.of(RelativePositionedTransitionWidgetPropertySchema.TYPE)){
            var text=adapter();var document=doc(text);var prototype=WidgetNodePrototypeFactory.create(C.find(type).orElseThrow(),StableId.random());
            var wrapped=transform.apply(document,new WrapWidget(text.id(),prototype,CHILD,0));
            assertEquals(DesignerCommandStatus.APPLIED,wrapped.status(),wrapped.toString());
            var full=wrapped.document().orElseThrow();
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new RemoveWidget(text.id())).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new MoveWidget(text.id(),new WidgetPlacement(full.root().id(),CHILDREN,1))).status());
            assertEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new ReplaceSlotChild(prototype.id(),CHILD,text.id(),new ReplaceSlotChild.NewSubtree(adapter()))).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new ResetProperty(prototype.id(),new PropertyName("rect"))).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(document,new WrapWidget(document.root().id(),prototype,CHILD,0)).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transform.apply(full,new AddWidget(new WidgetPlacement(prototype.id(),CHILD,0),adapter())).status());
        }
    }
}

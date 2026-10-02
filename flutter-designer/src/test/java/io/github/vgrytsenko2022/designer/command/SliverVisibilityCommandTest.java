package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.SliverVisibilityContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class SliverVisibilityCommandTest {
    @Test void requiredBranchProtectedReplacementEditableAndDependentFlagsAtomic() {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var transformer=new DesignerCommandTransformer(catalog,ValidationLimits.defaults());
        for(var type:List.of(TYPE,MAINTAIN)) {
            var child=adapter(); var doc=document(child);
            var prototype=WidgetNodePrototypeFactory.create(catalog.find(type).orElseThrow(),StableId.random());
            var wrapped=transformer.apply(doc,new WrapWidget(child.id(),prototype,SLIVER,0));
            assertEquals(DesignerCommandStatus.APPLIED,wrapped.status(),wrapped.toString());
            var full=wrapped.document().orElseThrow();
            assertNotEquals(DesignerCommandStatus.APPLIED,transformer.apply(full,new RemoveWidget(child.id())).status());
            assertNotEquals(DesignerCommandStatus.APPLIED,transformer.apply(full,new MoveWidget(child.id(),new WidgetPlacement(full.root().id(),SLIVERS,1))).status());
            assertEquals(DesignerCommandStatus.APPLIED,transformer.apply(full,new ReplaceSlotChild(prototype.id(),SLIVER,child.id(),new ReplaceSlotChild.NewSubtree(adapter()))).status());
            var replacement=adapter();
            var added=transformer.apply(full,new AddWidget(new WidgetPlacement(prototype.id(),REPLACEMENT,0),replacement));
            assertEquals(DesignerCommandStatus.APPLIED,added.status(),added.toString());
            var both=added.document().orElseThrow();
            assertEquals(DesignerCommandStatus.APPLIED,transformer.apply(both,new RemoveWidget(replacement.id())).status());
            assertEquals(DesignerCommandStatus.APPLIED,transformer.apply(both,new MoveWidget(replacement.id(),new WidgetPlacement(full.root().id(),SLIVERS,1))).status());
            assertEquals(DesignerCommandStatus.APPLIED,transformer.apply(both,new ReplaceSlotChild(prototype.id(),REPLACEMENT,replacement.id(),new ReplaceSlotChild.NewSubtree(adapter()))).status());
            assertEquals(DesignerCommandStatus.APPLIED,transformer.apply(both,new WrapWidget(replacement.id(),WidgetNodePrototypeFactory.create(catalog.find(type).orElseThrow(),StableId.random()),SLIVER,0)).status());
            if(type.equals(TYPE)) {
                assertNotEquals(DesignerCommandStatus.APPLIED,transformer.apply(both,new SetProperty(prototype.id(),new PropertyName("maintainSize"),new PropertyValue.BooleanValue(true))).status());
                var current=both;
                for(String field:List.of("maintainState","maintainAnimation","maintainSize","maintainSemantics","maintainInteractivity")) {
                    var result=transformer.apply(current,new SetProperty(prototype.id(),new PropertyName(field),new PropertyValue.BooleanValue(true)));
                    assertEquals(DesignerCommandStatus.APPLIED,result.status(),result.toString());
                    current=result.document().orElseThrow();
                }
                assertNotEquals(DesignerCommandStatus.APPLIED,transformer.apply(current,new ResetProperty(prototype.id(),new PropertyName("maintainState"))).status());
                for(String field:List.of("maintainInteractivity","maintainSemantics","maintainSize","maintainAnimation","maintainState")) {
                    var result=transformer.apply(current,new ResetProperty(prototype.id(),new PropertyName(field)));
                    assertEquals(DesignerCommandStatus.APPLIED,result.status(),result.toString());
                    current=result.document().orElseThrow();
                }
                assertEquals(both,current);
            }
        }
    }
}


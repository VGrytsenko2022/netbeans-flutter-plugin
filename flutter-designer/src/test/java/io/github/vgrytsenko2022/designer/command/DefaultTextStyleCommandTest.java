package io.github.vgrytsenko2022.designer.command;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static io.github.vgrytsenko2022.designer.catalog.DefaultTextStyleContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class DefaultTextStyleCommandTest {
 @Test void nestedAndRootWrappingReplacementAndRequiredChildRemoval(){
   var c=BuiltInWidgetCatalog.getDefault();var t=new DesignerCommandTransformer(c,ValidationLimits.defaults());
   for(var type:TYPES){
     var child=AnimatedSizeContractTest.adapter();var doc=document(child);
     var p=WidgetNodePrototypeFactory.create(c.find(type).orElseThrow(),StableId.random());
     var r=t.apply(doc,new WrapWidget(child.id(),p,CHILD,0));assertEquals(DesignerCommandStatus.APPLIED,r.status(),r.toString());
     var full=r.document().orElseThrow();assertEquals(doc.root().id(),full.root().id());
     assertNotEquals(DesignerCommandStatus.APPLIED,t.apply(full,new RemoveWidget(child.id())).status());
     assertEquals(DesignerCommandStatus.APPLIED,t.apply(full,new ReplaceSlotChild(p.id(),CHILD,child.id(),new ReplaceSlotChild.NewSubtree(AnimatedSizeContractTest.adapter()))).status());
     var root=t.apply(doc,new WrapWidget(doc.root().id(),p,CHILD,0));assertEquals(DesignerCommandStatus.APPLIED,root.status(),root.toString());
     assertEquals(p.id(),root.document().orElseThrow().root().id());
   }
 }
}

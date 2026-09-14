package dev.flutter.netbeans.designer.command;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.AnimatedPhysicalModelContractTest.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimatedPhysicalModelCommandTest {
 @Test void wrapsExistingAndRootPreservesIdentityAndCannotRemoveRequiredChild(){
    var c=BuiltInWidgetCatalog.getDefault();var t=new DesignerCommandTransformer(c,ValidationLimits.defaults());
    var child=AnimatedSizeContractTest.adapter();var doc=document(child);
    var p=WidgetNodePrototypeFactory.create(c.find(TYPE).orElseThrow(),StableId.random());
    var r=t.apply(doc,new WrapWidget(child.id(),p,CHILD,0));assertEquals(DesignerCommandStatus.APPLIED,r.status(),r.toString());
    var full=r.document().orElseThrow();
    assertNotEquals(DesignerCommandStatus.APPLIED,t.apply(full,new RemoveWidget(child.id())).status());
    assertEquals(DesignerCommandStatus.APPLIED,t.apply(full,new ReplaceSlotChild(p.id(),CHILD,child.id(),new ReplaceSlotChild.NewSubtree(AnimatedSizeContractTest.adapter()))).status());
    for(String name:List.of("color","shadowColor","durationUs"))assertNotEquals(DesignerCommandStatus.APPLIED,t.apply(full,new ResetProperty(p.id(),new PropertyName(name))).status());
    var root=t.apply(doc,new WrapWidget(doc.root().id(),p,CHILD,0));assertEquals(DesignerCommandStatus.APPLIED,root.status(),root.toString());
    assertEquals(p.id(),root.document().orElseThrow().root().id());
 }
}



package io.github.vgrytsenko2022.plugin.designer;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MaterialBannerCommandContractTest {
    @Test void requiredChildrenEventsHistoryAndSavedSourceArePreserved()throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();var banner=WidgetNodePrototypeFactory.create(catalog.find(MaterialBannerWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var content=((WidgetSlot.SingleSlot)banner.slots().get(new SlotName("content"))).child().orElseThrow();
        var action=((WidgetSlot.ListSlot)banner.slots().get(new SlotName("actions"))).children().getFirst();
        var seed=StateBindingRealSdkTest.openRoot(banner,"banner.dart","// Keep this member.\nint marker=73;\nvoid existing() {}\n");
        for(var child:List.of(content,action)){
            var rejected=seed.apply(new RemoveWidget(child.id()));assertFalse(rejected.changed());assertArrayEquals(seed.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        }
        var current=apply(seed,new CreateEventHandler(banner.id(),new PropertyName("onVisible"),"_shown"));
        current=apply(current,new CreateEventHandler(action.id(),new PropertyName("onPressed"),"_pressed"));
        current=apply(current,new RenameEventHandler(banner.id(),new PropertyName("onVisible"),"_renamed"));
        var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"existing",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        current=apply(current,new SetProperty(banner.id(),new PropertyName("onVisible"),reference));
        assertArrayEquals(seed.current().dartCandidateBytes(),current.undo().session().undo().session().undo().session().undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready());current=reopened.session().orElseThrow();
        current=apply(current,new ResetProperty(banner.id(),new PropertyName("onVisible")));
        var code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("// Keep this member."));assertTrue(code.contains("int marker=73;"));assertTrue(code.contains("_renamed"));assertFalse(code.contains("onVisible:"));
    }
    private static DesignerCommandSession apply(DesignerCommandSession s,DesignerCommand c){var r=s.apply(c);assertTrue(r.changed(),r.diagnostics().toString());return r.session();}
}

package dev.flutter.netbeans.plugin.designer;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ChipCommandContractTest {
    @Test void requiredLabelOptionalSlotsEventsHistoryAndSavedSourceArePreserved()throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();var chip=WidgetNodePrototypeFactory.create(catalog.find(ChipWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var label=((WidgetSlot.SingleSlot)chip.slots().get(new SlotName("label"))).child().orElseThrow();
        var seed=StateBindingRealSdkTest.openRoot(chip,"chip.dart","// Keep this member.\nint marker=73;\nvoid existing() {}\n");
        var rejected=seed.apply(new RemoveWidget(label.id()));assertFalse(rejected.changed());assertArrayEquals(seed.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        var current=apply(seed,new CreateEventHandler(chip.id(),new PropertyName("onDeleted"),"_deleted"));
        current=apply(current,new RenameEventHandler(chip.id(),new PropertyName("onDeleted"),"_renamed"));
        var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"existing",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        current=apply(current,new SetProperty(chip.id(),new PropertyName("onDeleted"),reference));
        assertArrayEquals(seed.current().dartCandidateBytes(),current.undo().session().undo().session().undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        for(String slot:List.of("avatar","deleteIcon")){
            var child=new WidgetNode(StableId.random(),TextWidgetPropertySchema.TEXT_TYPE,Map.of(new PropertyName("data"),new PropertyValue.StringValue(slot)),Map.of());
            var before=current;current=apply(current,new AddWidget(new WidgetPlacement(chip.id(),new SlotName(slot),0),child));
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);assertTrue(reopened.ready());current=reopened.session().orElseThrow();
            current=apply(current,new RemoveWidget(child.id()));
        }
        current=apply(current,new ResetProperty(chip.id(),new PropertyName("onDeleted")));
        var code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("// Keep this member."));assertTrue(code.contains("int marker=73;"));assertTrue(code.contains("_renamed"));assertFalse(code.contains("onDeleted:"));
    }
    private static DesignerCommandSession apply(DesignerCommandSession s,DesignerCommand c){var r=s.apply(c);assertTrue(r.changed(),r.diagnostics().toString());return r.session();}
}

package io.github.vgrytsenko2022.plugin.designer;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AlertDialogCommandContractTest {
    @Test void bothConstructorsEditChildrenResetUndoRedoAndReopenWithoutTouchingUserCode()throws Exception{
        var catalog=BuiltInWidgetCatalog.getDefault();
        for(boolean full:List.of(false,true)){
            var widget=WidgetNodePrototypeFactory.create(catalog.find(full?AlertDialogWidgetPropertySchema.ADAPTIVE_TYPE:AlertDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
            var baseline=StateBindingRealSdkTest.openRoot(widget,"dialog.dart","// Keep dialog member.\nint marker = 73;\n");
            for(DesignerCommand invalid:List.of(new SetProperty(widget.id(),new PropertyName("actionsOverflowButtonSpacing"),new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(Long.MAX_VALUE))),
                    new SetProperty(widget.id(),new PropertyName("scrollable"),new PropertyValue.NullValue()),
                    new CreateEventHandler(widget.id(),new PropertyName("onConfirm"),"_confirm"))){
                var result=baseline.apply(invalid);assertFalse(result.changed());assertArrayEquals(baseline.current().fdSnapshot().copyBytes(),result.session().current().fdSnapshot().copyBytes());
                assertArrayEquals(baseline.current().dartCandidateBytes(),result.session().current().dartCandidateBytes());
            }
            var current=apply(baseline,new SetProperty(widget.id(),new PropertyName("scrollable"),new PropertyValue.BooleanValue(true)));
            assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
            var text=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
            current=apply(current,new AddWidget(new WidgetPlacement(widget.id(),new SlotName("content"),0),text));
            current=apply(current,new ResetProperty(widget.id(),new PropertyName("scrollable")));
            var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
            assertTrue(reopened.ready(),reopened.diagnostics().toString());var restored=reopened.session().orElseThrow();
            assertArrayEquals(current.current().fdSnapshot().copyBytes(),restored.current().fdSnapshot().copyBytes());assertArrayEquals(current.current().dartCandidateBytes(),restored.current().dartCandidateBytes());
            String source=new String(restored.current().dartCandidateBytes(),StandardCharsets.UTF_8);assertTrue(source.contains("// Keep dialog member."));assertTrue(source.contains("int marker = 73;"));assertTrue(source.contains("content:"));assertFalse(source.contains("scrollable:"));
        }
    }
    private static DesignerCommandSession apply(DesignerCommandSession s,DesignerCommand command){
        var result=s.apply(command);assertTrue(result.changed(),result.diagnostics().toString());return result.session();
    }
}

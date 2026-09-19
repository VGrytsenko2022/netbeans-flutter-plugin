package io.github.vgrytsenko2022.plugin.designer;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BottomSheetCommandContractTest {
    @Test void childEventsHistoryResetAndReopenPreserveCode() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var sheet=WidgetNodePrototypeFactory.create(catalog.find(BottomSheetWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(sheet,"sheet.dart","// Keep sheet member.\nint marker = 73;\n");
        var rejected=baseline.apply(new SetProperty(sheet.id(),new PropertyName("enableDrag"),new PropertyValue.BooleanValue(true)));
        assertFalse(rejected.changed());assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        var text=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(new WidgetPlacement(sheet.id(),new SlotName("child"),0),text));
        var before=current;
        for(String event:List.of("onClosing","onDragStart","onDragEnd"))
            current=apply(current,new CreateEventHandler(sheet.id(),new PropertyName(event),"_"+event));
        String code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("required bool isClosing"));assertTrue(code.contains("DragEndDetails"));
        var undone=current.undo().session().undo().session().undo().session();
        assertArrayEquals(before.current().dartCandidateBytes(),undone.current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),undone.redo().session().redo().session().redo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready(),reopened.diagnostics().toString());current=reopened.session().orElseThrow();
        assertTrue(code.contains("// Keep sheet member."));assertTrue(code.contains("int marker = 73;"));
        current=apply(current,new SetProperty(sheet.id(),new PropertyName("onClosing"),new PropertyValue.StringValue("noop")));
        code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("onClosing: () {}"));assertTrue(code.contains("_onClosing"));
        current=apply(current,new ResetProperty(sheet.id(),new PropertyName("onDragEnd")));
        assertFalse(new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8).contains("onDragEnd:"));
    }
    private static DesignerCommandSession apply(DesignerCommandSession s,DesignerCommand c) {
        var r=s.apply(c);assertTrue(r.changed(),r.diagnostics().toString());return r.session();
    }
}

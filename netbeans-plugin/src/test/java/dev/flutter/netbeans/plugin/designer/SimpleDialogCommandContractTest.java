package dev.flutter.netbeans.plugin.designer;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SimpleDialogCommandContractTest {
    @Test void editOptionsEventHistoryAndReopenPreserveUserCode() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var dialog=WidgetNodePrototypeFactory.create(catalog.find(SimpleDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(dialog,"simple.dart","// Keep dialog member.\nint marker = 73;\n");
        var rejected=baseline.apply(new SetProperty(dialog.id(),new PropertyName("titlePadding"),new PropertyValue.NullValue()));
        assertFalse(rejected.changed());assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        var option=WidgetNodePrototypeFactory.create(catalog.find(SimpleDialogWidgetPropertySchema.OPTION_TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(new WidgetPlacement(dialog.id(),new SlotName("children"),0),option));
        var text=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        current=apply(current,new AddWidget(new WidgetPlacement(option.id(),new SlotName("child"),0),text));
        var beforeEvent=current;
        current=apply(current,new CreateEventHandler(option.id(),new PropertyName("onPressed"),"_choose"));
        assertArrayEquals(beforeEvent.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready(),reopened.diagnostics().toString());var restored=reopened.session().orElseThrow();
        assertArrayEquals(current.current().fdSnapshot().copyBytes(),restored.current().fdSnapshot().copyBytes());
        String source=new String(restored.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(source.contains("// Keep dialog member."));assertTrue(source.contains("int marker = 73;"));assertTrue(source.contains("onPressed:"));assertTrue(source.contains("_choose"));
        current=apply(restored,new ResetProperty(option.id(),new PropertyName("onPressed")));
        assertTrue(new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8).contains("_choose"));
    }
    private static DesignerCommandSession apply(DesignerCommandSession s, DesignerCommand c) {
        var r=s.apply(c);assertTrue(r.changed(),r.diagnostics().toString());return r.session();
    }
}

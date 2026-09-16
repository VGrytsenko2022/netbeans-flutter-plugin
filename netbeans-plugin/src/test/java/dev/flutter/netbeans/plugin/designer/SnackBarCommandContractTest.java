package dev.flutter.netbeans.plugin.designer;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SnackBarCommandContractTest {
    @Test void requiredContentExactActionEventsUndoRedoAndReopen()throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var bar=WidgetNodePrototypeFactory.create(catalog.find(SnackBarWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var action=WidgetNodePrototypeFactory.create(catalog.find(SnackBarWidgetPropertySchema.ACTION).orElseThrow(),StableId.random());
        var seed=StateBindingRealSdkTest.openRoot(bar,"snack.dart","// Keep member.\nint marker=73;\n");
        var content=((WidgetSlot.SingleSlot)bar.slots().get(new SlotName("content"))).child().orElseThrow();
        var removal=seed.apply(new RemoveWidget(content.id()));
        assertFalse(removal.changed());assertArrayEquals(seed.current().dartCandidateBytes(),removal.session().current().dartCandidateBytes());
        var wrong=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
        assertFalse(seed.apply(new AddWidget(new WidgetPlacement(bar.id(),new SlotName("action"),0),wrong)).changed());
        var current=apply(seed,new AddWidget(new WidgetPlacement(bar.id(),new SlotName("action"),0),action));
        var before=current;
        current=apply(current,new CreateEventHandler(bar.id(),new PropertyName("onVisible"),"_shown"));
        current=apply(current,new CreateEventHandler(action.id(),new PropertyName("onPressed"),"_pressed"));
        assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().undo().session().current().dartCandidateBytes());
        var undone=current.undo().session().undo().session();
        assertArrayEquals(current.current().dartCandidateBytes(),undone.redo().session().redo().session().current().dartCandidateBytes());
        var reopened=DesignerCommandSession.open(current.current().fdSnapshot(),current.current().dartCandidateBytes(),catalog);
        assertTrue(reopened.ready(),reopened.diagnostics().toString());current=reopened.session().orElseThrow();
        var code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("// Keep member."));assertTrue(code.contains("int marker=73;"));
        assertFalse(current.apply(new ResetProperty(action.id(),new PropertyName("onPressed"))).changed());
        current=apply(current,new SetProperty(action.id(),new PropertyName("onPressed"),new PropertyValue.StringValue("noop")));
        current=apply(current,new ResetProperty(bar.id(),new PropertyName("onVisible")));
        code=new String(current.current().dartCandidateBytes(),StandardCharsets.UTF_8);
        assertTrue(code.contains("onPressed: () {}"));assertTrue(code.contains("_pressed"));assertTrue(code.contains("_shown"));assertFalse(code.contains("onVisible:"));
    }
    private static DesignerCommandSession apply(DesignerCommandSession s,DesignerCommand c){var r=s.apply(c);assertTrue(r.changed(),r.diagnostics().toString());return r.session();}
}

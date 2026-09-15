package dev.flutter.netbeans.plugin.designer;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TimePickerDialogCommandContractTest {
    @Test void editsEventsIconsUndoRedoAndReopenPreserveUserCodeAndAtomicity() throws Exception {
        var catalog=BuiltInWidgetCatalog.getDefault();
        var picker=WidgetNodePrototypeFactory.create(catalog.find(TimePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=StateBindingRealSdkTest.openRoot(picker,"time.dart","// Keep this member.\nint marker = 73;\n");
        for(DesignerCommand command:List.of(
                new SetProperty(picker.id(),new PropertyName("initialTime"),new PropertyValue.StringValue("24:00")),
                new SetProperty(picker.id(),new PropertyName("initialTime"),new PropertyValue.StringValue("09:00\n")),
                new ResetProperty(picker.id(),new PropertyName("initialTime")),
                new SetProperty(picker.id(),new PropertyName("emptyInitialInput"),new PropertyValue.NullValue()))) {
            var rejected=baseline.apply(command);assertFalse(rejected.changed());
            assertArrayEquals(baseline.current().fdSnapshot().copyBytes(),rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(baseline.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        }
        var created=apply(baseline,new CreateEventHandler(picker.id(),new PropertyName("onEntryModeChanged"),"_modeChanged"));
        var edited=reopen(created,source(created).replace("// TODO: Handle onEntryModeChanged.","marker += mode.index; // User logic."));
        assertNotEquals(source(created),source(edited));
        var renamed=apply(edited,new RenameEventHandler(picker.id(),new PropertyName("onEntryModeChanged"),"_entryChanged"));
        assertTrue(source(renamed).contains("void _entryChanged(TimePickerEntryMode mode)"));
        assertTrue(source(renamed).contains("marker += mode.index; // User logic."));
        assertArrayEquals(edited.current().dartCandidateBytes(),renamed.undo().session().current().dartCandidateBytes());
        assertArrayEquals(renamed.current().dartCandidateBytes(),renamed.undo().session().redo().session().current().dartCandidateBytes());
        var current=apply(renamed,new ResetProperty(picker.id(),new PropertyName("onEntryModeChanged")));
        assertTrue(source(current).contains("void _entryChanged"));assertFalse(source(current).contains("onEntryModeChanged:"));
        current=apply(current,new SetProperty(picker.id(),new PropertyName("initialTime"),new PropertyValue.StringValue("23:59")));
        for(String slot:List.of("switchToInputEntryModeIcon","switchToTimerEntryModeIcon")){
            var icon=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(),StableId.random());
            var text=WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Text")).orElseThrow(),StableId.random());
            var target=new WidgetPlacement(picker.id(),new SlotName(slot),0);
            assertFalse(current.apply(new AddWidget(target,text)).changed());
            current=apply(current,new AddWidget(target,icon));
            assertTrue(source(current).contains(slot+":"));
        }
        var reopened=reopen(current,source(current));
        assertArrayEquals(current.current().fdSnapshot().copyBytes(),reopened.current().fdSnapshot().copyBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("TimeOfDay(hour: 23, minute: 59)"));
        assertTrue(source(reopened).contains("// Keep this member."));
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertTrue(result.changed(), result.diagnostics().toString());
        return result.session();
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session, String source) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
}

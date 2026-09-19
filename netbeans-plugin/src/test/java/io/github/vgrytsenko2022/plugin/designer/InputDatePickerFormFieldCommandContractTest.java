package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputDatePickerFormFieldCommandContractTest {
    @Test void predicateUserCodeHistoryAndRejectedDateEditsRemainAtomic() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var definition = catalog.find(InputDatePickerFormFieldWidgetPropertySchema.TYPE).orElseThrow();
        var picker = WidgetNodePrototypeFactory.create(definition, StableId.random());
        var baseline = StateBindingRealSdkTest.openRoot(picker, "dates.dart", "// Keep this member.\nint marker = 73;\n");
        for (DesignerCommand command : java.util.List.of(
                new SetProperty(picker.id(), new PropertyName("firstDate"), new PropertyValue.StringValue("2025-02-29")),
                new SetProperty(picker.id(), new PropertyName("firstDate"), new PropertyValue.StringValue("2200-01-01")),
                new SetProperty(picker.id(), new PropertyName("initialDate"), new PropertyValue.StringValue("1800-01-01")),
                new ResetProperty(picker.id(), new PropertyName("firstDate")),
                new SetProperty(picker.id(), new PropertyName("acceptEmptyDate"), new PropertyValue.NullValue()))) {
            var rejected = baseline.apply(command);
            assertFalse(rejected.changed(), command.toString());
            assertArrayEquals(baseline.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(baseline.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
        }
        var created = apply(baseline, new CreateEventHandler(picker.id(), new PropertyName("selectableDayPredicate"), "_allowedDate"));
        var edited = reopen(created, source(created).replace("return true;", "return date.weekday != DateTime.sunday; // User rule."));
        var renamed = apply(edited, new RenameEventHandler(picker.id(), new PropertyName("selectableDayPredicate"), "_businessDay"));
        assertTrue(source(renamed).contains("bool _businessDay(DateTime date)"));
        assertTrue(source(renamed).contains("return date.weekday != DateTime.sunday; // User rule."));
        assertFalse(source(renamed).contains("_allowedDate"));
        assertArrayEquals(edited.current().dartCandidateBytes(), renamed.undo().session().current().dartCandidateBytes());
        assertArrayEquals(renamed.current().dartCandidateBytes(), renamed.undo().session().redo().session().current().dartCandidateBytes());
        var unbound = apply(renamed, new ResetProperty(picker.id(), new PropertyName("selectableDayPredicate")));
        assertTrue(source(unbound).contains("bool _businessDay(DateTime date)"), "Disconnect must retain user method");
        var dated = apply(unbound, new SetProperty(picker.id(), new PropertyName("initialDate"), new PropertyValue.StringValue("2024-02-29")));
        var configured=dated;
        for(String event:java.util.List.of("onDateSubmitted","onDateSaved")){
            var createdEvent=apply(configured,new CreateEventHandler(picker.id(),new PropertyName(event),"_"+event));
            String userBody="marker += date.day; // User event logic: "+event;
            String editedSource=source(createdEvent).replace("// TODO: Handle "+event+".",userBody);
            assertNotEquals(source(createdEvent),editedSource,"The fixture must really edit the method body");
            var editedEvent=reopen(createdEvent,editedSource);
            configured=apply(editedEvent,new RenameEventHandler(picker.id(),new PropertyName(event),"_renamed"+event));
            assertTrue(source(configured).contains("void _renamed"+event+"(DateTime date)"));
            var descriptor=io.github.vgrytsenko2022.designer.events.WidgetEventCatalog.find(picker.type(),new PropertyName(event)).orElseThrow();
            configured=apply(configured,descriptor.required()
                ?new SetProperty(picker.id(),new PropertyName(event),descriptor.creationDefault().orElseThrow())
                :new ResetProperty(picker.id(),new PropertyName(event)));
            assertTrue(source(configured).contains("void _renamed"+event));
            assertTrue(source(configured).contains(userBody),"Disconnect and rename must retain user logic");
            assertArrayEquals(editedEvent.current().dartCandidateBytes(),configured.undo().session().undo().session().current().dartCandidateBytes());
        }
        assertFalse(source(configured).contains("onDateSubmitted:"));
        assertFalse(source(configured).contains("onDateSaved:"));
        var reopened = reopen(configured, source(configured));
        assertArrayEquals(configured.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertArrayEquals(configured.current().fdSnapshot().copyBytes(), reopened.current().fdSnapshot().copyBytes());
        assertTrue(source(reopened).contains("// Keep this member."));
        assertTrue(source(reopened).contains("DateTime(2024, 2, 29)"));
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

package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DateRangePickerDialogCommandContractTest {
    @Test void predicateUserCodeHistoryAndRejectedDateEditsRemainAtomic() throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var definition = catalog.find(DateRangePickerDialogWidgetPropertySchema.TYPE).orElseThrow();
        var picker = WidgetNodePrototypeFactory.create(definition, StableId.random());
        var baseline = StateBindingRealSdkTest.openRoot(picker, "dates.dart", "// Keep this member.\nint marker = 73;\n");
        for (DesignerCommand command : java.util.List.of(
                new SetProperty(picker.id(), new PropertyName("firstDate"), new PropertyValue.StringValue("2025-02-29")),
                new SetProperty(picker.id(), new PropertyName("firstDate"), new PropertyValue.StringValue("2200-01-01")),
                new SetProperty(picker.id(), new PropertyName("initialDateRange"), new PropertyValue.StringValue("1800-01-01/1800-01-02")),
                new ResetProperty(picker.id(), new PropertyName("firstDate")))) {
            var rejected = baseline.apply(command);
            assertFalse(rejected.changed(), command.toString());
            assertArrayEquals(baseline.current().fdSnapshot().copyBytes(), rejected.session().current().fdSnapshot().copyBytes());
            assertArrayEquals(baseline.current().dartCandidateBytes(), rejected.session().current().dartCandidateBytes());
        }
        var created = apply(baseline, new CreateEventHandler(picker.id(), new PropertyName("selectableDayPredicate"), "_allowedDate"));
        var edited = reopen(created, source(created).replace("return true;", "return day.weekday != DateTime.sunday; // User rule."));
        var renamed = apply(edited, new RenameEventHandler(picker.id(), new PropertyName("selectableDayPredicate"), "_businessDay"));
        assertTrue(source(renamed).contains("bool _businessDay(DateTime day, DateTime? selectedStartDay, DateTime? selectedEndDay)"));
        assertTrue(source(renamed).contains("return day.weekday != DateTime.sunday; // User rule."));
        assertFalse(source(renamed).contains("_allowedDate"));
        assertArrayEquals(edited.current().dartCandidateBytes(), renamed.undo().session().current().dartCandidateBytes());
        assertArrayEquals(renamed.current().dartCandidateBytes(), renamed.undo().session().redo().session().current().dartCandidateBytes());
        var unbound = apply(renamed, new ResetProperty(picker.id(), new PropertyName("selectableDayPredicate")));
        assertTrue(source(unbound).contains("bool _businessDay(DateTime day, DateTime? selectedStartDay, DateTime? selectedEndDay)"), "Disconnect must retain user method");
        var dated = apply(unbound, new SetProperty(picker.id(), new PropertyName("initialDateRange"), new PropertyValue.StringValue("2024-02-29/2024-03-01")));
        var icon = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(), StableId.random());
        var withIcon = apply(dated, new AddWidget(new WidgetPlacement(picker.id(), new SlotName("switchToInputEntryModeIcon"), 0), icon));
        var reopened = reopen(withIcon, source(withIcon));
        assertArrayEquals(withIcon.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertArrayEquals(withIcon.current().fdSnapshot().copyBytes(), reopened.current().fdSnapshot().copyBytes());
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

package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.RadioListTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioListTileCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void addThreeSlotsMoveRemoveAndRequiredSubtitleRetainExactHistory() throws Exception {
        var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(ListTileTestValues.text("Before")))));
        var initial = open(row, WidgetClassKind.STATELESS);
        var tile = node(Map.of());
        var added = apply(initial, new AddWidget(new WidgetPlacement(row.id(), new SlotName("children"), 1), tile));
        assertExact(initial, added.undo().session());
        StableId subtitle = null;
        for (String name : List.of("title", "subtitle", "secondary")) {
            var child = ListTileTestValues.text(name);
            if (name.equals("subtitle")) subtitle = child.id();
            var childAdded = apply(added, new AddWidget(new WidgetPlacement(tile.id(), new SlotName(name), 0), child));
            assertEquals(child, EventCommandSupport.widget(childAdded.current().document().root(), child.id()));
            assertExact(added, childAdded.undo().session()); added = childAdded;
        }
        var three = apply(added, new SetProperty(tile.id(), p("isThreeLine"), b(true)));
        assertRejected(three, new RemoveWidget(subtitle));
        var moved = apply(three, new MoveWidget(tile.id(), new WidgetPlacement(row.id(), new SlotName("children"), 0)));
        assertExact(three, moved.undo().session()); assertExact(moved, reopen(moved));
        var removed = apply(moved, new RemoveWidget(tile.id())); assertExact(moved, removed.undo().session());
    }

    @Test
    void allDenseStyleFamiliesSaveReopenAndResetWithoutClearingSlotsOrInactiveFlag() throws Exception {
        var tile = fullNode("adaptive", "roundedRectangle");
        var initial = open(tile, WidgetClassKind.STATELESS);
        var standard = apply(initial, new SetProperty(tile.id(), p("variant"), s("standard")));
        assertEquals(b(true), standard.current().document().root().properties().get(p("useCupertinoCheckmarkStyle")));
        assertFalse(source(standard).contains("useCupertinoCheckmarkStyle:")); assertExact(standard, reopen(standard));
        var adaptive = apply(standard, new SetProperty(tile.id(), p("variant"), s("adaptive"))); assertExact(initial, adaptive);
        var resets = tile.properties().keySet().stream().filter(v -> !Set.of("value", "valueType", "variant").contains(v.value()))
                .map(v -> (PatchProperties.Patch) new PatchProperties.ResetPatch(v)).toList();
        var reset = apply(adaptive, new PatchProperties(tile.id(), resets));
        assertEquals(3, reset.current().document().root().properties().size());
        assertEquals(tile.slots(), reset.current().document().root().slots()); assertFalse(source(reset).contains("onChanged:"));
        assertExact(adaptive, reset.undo().session()); assertExact(reset, reopen(reset));
    }

    @Test
    void typeChangesAreAtomicAndInvalidPartialEditsDoNotLoseRedoOrSourceHandlers() throws Exception {
        var tile = node(Map.of(p("groupValue"), s("option"), p("onChanged"), reference("changed")));
        var initial = open(tile, WidgetClassKind.STATELESS);
        assertRejected(initial, new SetProperty(tile.id(), p("valueType"), s("int")));
        var changed = apply(initial, new PatchProperties(tile.id(), List.of(
                new PatchProperties.SetPatch(p("valueType"), s("int")), new PatchProperties.SetPatch(p("value"), i(2)),
                new PatchProperties.SetPatch(p("groupValue"), i(1)))));
        assertTrue(source(changed).contains("RadioListTile<int>"));
        assertEquals(tile.properties().get(p("onChanged")), changed.current().document().root().properties().get(p("onChanged")));
        assertExact(initial, changed.undo().session()); assertExact(changed, changed.undo().session().redo().session()); assertExact(changed, reopen(changed));
        var withRedo = changed.undo().session();
        for (var command : List.of(new SetProperty(tile.id(), p("value"), nil()), new SetProperty(tile.id(), p("valueType"), s("List<String>")),
                new SetProperty(tile.id(), p("onChanged"), new PropertyValue.CallbackValue("_raw")))) assertRejected(withRedo, command);
        assertExact(changed, withRedo.redo().session());
        for (String field : List.of("value", "valueType", "variant")) assertRejected(initial, new ResetProperty(tile.id(), p(field)));
    }

    @Test
    void bothEventsCreateBroadSafeSourceAndResetOptionalCallbacksWithoutDeletingBodies() throws Exception {
        for (var kind : WidgetClassKind.values()) for (var event : WidgetEventCatalog.eventsFor(definition())) {
            var tile = node(Map.of()); var initial = open(tile, kind);
            var created = apply(initial, new CreateEventHandler(tile.id(), event.propertyName(), "_tileEvent"));
            assertTrue(source(created).contains(event.signature().declaration("_tileEvent")));
            assertTrue(source(created).contains(event.propertyName().value() + ": _tileEvent"));
            assertTrue(source(created).contains("// Preserved user source"));
            assertExact(initial, created.undo().session()); assertExact(created, reopen(created));
            var renamed = apply(created, new RenameEventHandler(tile.id(), event.propertyName(), "_renamed"));
            assertTrue(source(renamed).contains(event.signature().declaration("_renamed"))); assertExact(created, renamed.undo().session());
            var omitted = apply(renamed, new ResetProperty(tile.id(), event.propertyName()));
            assertFalse(source(omitted).contains(event.propertyName().value() + ":"));
            assertTrue(source(omitted).contains(event.signature().declaration("_renamed"))); assertExact(omitted, reopen(omitted));
            var nullEvent = apply(omitted, new SetProperty(tile.id(), event.propertyName(), nil()));
            assertTrue(source(nullEvent).contains(event.propertyName().value() + ": null")); assertExact(omitted, nullEvent.undo().session());
        }
    }

    @Test
    void everyBuiltinCreatesNullableLegacyGroupStateAndPreservesTheIndependentOption() throws Exception {
        var samples = Map.of("String", s("x"), "int", i(1), "double", d("1.5"), "num", i(2), "bool", b(true), "Object", s("x"));
        for (var sample : samples.entrySet()) {
            var tile = node(Map.of(p("valueType"), s(sample.getKey()), p("value"), sample.getValue(), p("groupValue"), sample.getValue()));
            var initial = open(tile, WidgetClassKind.STATEFUL);
            var bound = apply(initial, new CreateStateBinding(tile.id(), "_selection", "_selectionChanged"));
            assertTrue(source(bound).contains(sample.getKey() + "? _selection"));
            assertTrue(source(bound).contains("void _selectionChanged(" + sample.getKey() + "? value)"));
            assertTrue(source(bound).contains("groupValue: _selection"));
            assertEquals(tile.properties().get(p("value")), bound.current().document().root().properties().get(p("value")));
            assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound));
            var renamed = apply(bound, new RenameStateField(tile.id(), "_selection", "_renamedSelection"));
            assertTrue(source(renamed).contains("groupValue: _renamedSelection")); assertExact(bound, renamed.undo().session());
            var removed = apply(renamed, new RemoveStateBinding(tile.id()));
            assertTrue(source(removed).contains(sample.getKey() + "? _renamedSelection"));
            assertTrue(source(removed).contains("_selectionChanged")); assertTrue(removed.current().document().root().stateBinding().isEmpty());
            assertExact(removed, reopen(removed)); assertExact(renamed, removed.undo().session());
        }
    }

    @Test
    void allEightConsumersRetainLiteralPreviewsAndInactiveAdaptiveBindingsAcrossReopen() throws Exception {
        var tile = node(Map.of(p("useCupertinoCheckmarkStyle"), b(false), p("selected"), b(false)));
        var initial = open(tile, WidgetClassKind.STATEFUL);
        for (String name : List.of("toggleable", "dense", "selected", "autofocus", "enableFeedback", "enabled", "internalAddSemanticForOnTap", "useCupertinoCheckmarkStyle")) {
            for (var binding : List.of(new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                    new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                    new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS, Optional.of(i(1))))) {
                var bound = apply(initial, new BindPropertyToState(tile.id(), p(name), binding));
                assertEquals(binding, bound.current().document().root().propertyBindings().get(p(name)));
                assertEquals(initial.current().document().root().properties(), bound.current().document().root().properties());
                assertEquals(!name.equals("useCupertinoCheckmarkStyle"), source(bound).contains(name + ":"));
                assertExact(initial, bound.undo().session()); assertExact(bound, reopen(bound));
                if (name.equals("useCupertinoCheckmarkStyle")) {
                    var adaptive = apply(bound, new SetProperty(tile.id(), p("variant"), s("adaptive")));
                    assertTrue(source(adaptive).contains("useCupertinoCheckmarkStyle:"));
                    assertEquals(binding, adaptive.current().document().root().propertyBindings().get(p(name))); assertExact(bound, adaptive.undo().session());
                }
            }
        }
    }

    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command); assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString()); return result.session();
    }
    private static void assertRejected(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command); assertNotEquals(DesignerCommandStatus.APPLIED, result.status()); assertSame(session, result.session()); assertExact(session, result.session());
    }
    private static String source(DesignerCommandSession session) { return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8); }
    private static void assertExact(DesignerCommandSession expected, DesignerCommandSession actual) {
        assertArrayEquals(expected.current().fdBytes(), actual.current().fdBytes()); assertArrayEquals(expected.current().dartCandidateBytes(), actual.current().dartCandidateBytes());
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), session.current().dartCandidateBytes(), CATALOG);
        assertTrue(result.ready(), result.diagnostics().toString()); return result.session().orElseThrow();
    }
    private static DesignerCommandSession open(WidgetNode root, WidgetClassKind kind) throws Exception {
        var id = StableId.random(); var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG); assertTrue(result.successful(), result.diagnostics().toString()); var generated = result.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n  int? _choice = null;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG); assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID), new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}

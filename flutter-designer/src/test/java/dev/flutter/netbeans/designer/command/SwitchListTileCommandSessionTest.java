package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.flutter.netbeans.designer.catalog.SwitchListTileTestValues.*;
import static org.junit.jupiter.api.Assertions.*;

class SwitchListTileCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void addMoveRemoveAndAllThreeSlotsKeepStableIdsAndExactHistory() throws Exception {
        var row = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Row"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(ListTileTestValues.text("Before")))));
        var initial = open(row, WidgetClassKind.STATELESS);
        var tile = node(Map.of());
        var added = apply(initial, new AddWidget(new WidgetPlacement(row.id(), new SlotName("children"), 1), tile));
        assertExact(initial, added.undo().session());
        for (String name : List.of("title", "subtitle", "secondary")) {
            var child = ListTileTestValues.text(name);
            var childAdded = apply(added, new AddWidget(new WidgetPlacement(tile.id(), new SlotName(name), 0), child));
            assertEquals(child, EventCommandSupport.widget(childAdded.current().document().root(), child.id()));
            assertExact(added, childAdded.undo().session());
            added = childAdded;
        }
        var moved = apply(added, new MoveWidget(tile.id(), new WidgetPlacement(row.id(), new SlotName("children"), 0)));
        assertExact(added, moved.undo().session());
        assertExact(moved, reopen(moved));
        var removed = apply(moved, new RemoveWidget(tile.id()));
        assertExact(moved, removed.undo().session());
    }

    @Test
    void denseStylesSaveReopenResetAndVariantTransitionsRetainEveryInactiveValue() throws Exception {
        var tile = fullNode("adaptive", "roundedRectangle");
        var initial = open(tile, WidgetClassKind.STATELESS);
        var standard = apply(initial, new SetProperty(tile.id(), p("variant"), s("standard")));
        assertEquals(tile.properties().get(p("applyCupertinoTheme")), standard.current().document().root().properties().get(p("applyCupertinoTheme")));
        assertFalse(source(standard).contains("applyCupertinoTheme:"));
        assertExact(standard, reopen(standard));
        var adaptive = apply(reopen(standard), new SetProperty(tile.id(), p("variant"), s("adaptive")));
        assertTrue(source(adaptive).contains("applyCupertinoTheme:"));
        assertExact(initial, adaptive);
        assertExact(standard, adaptive.undo().session());
        var resets = tile.properties().keySet().stream().filter(v -> !Set.of("variant", "value", "onChanged").contains(v.value()))
                .map(v -> (PatchProperties.Patch) new PatchProperties.ResetPatch(v)).toList();
        var reset = apply(adaptive, new PatchProperties(tile.id(), resets));
        assertEquals(3, reset.current().document().root().properties().size());
        assertEquals(tile.slots(), reset.current().document().root().slots());
        assertExact(adaptive, reset.undo().session());
        assertExact(reset, reopen(reset));
    }

    @Test
    void missingSubtitleAndThumbImageRejectPartialMutationsWithoutDestroyingRedo() throws Exception {
        var tile = node(Map.of());
        var initial = open(tile, WidgetClassKind.STATELESS);
        var changed = apply(initial, new SetProperty(tile.id(), p("selected"), b(true)));
        var undone = changed.undo().session();
        assertRejected(undone, new SetProperty(tile.id(), p("isThreeLine"), b(true)));
        assertRejected(undone, new SetProperty(tile.id(), p("onActiveThumbImageError"), s("noop")));
        assertExact(changed, undone.redo().session());
        var subtitle = ListTileTestValues.text("Subtitle");
        var withChild = apply(initial, new AddWidget(new WidgetPlacement(tile.id(), new SlotName("subtitle"), 0), subtitle));
        var threeLine = apply(withChild, new SetProperty(tile.id(), p("isThreeLine"), b(true)));
        assertRejected(threeLine, new RemoveWidget(subtitle.id()));
        assertExact(threeLine, reopen(threeLine));
        for (String name : List.of("value", "onChanged", "variant")) assertRejected(initial, new ResetProperty(tile.id(), p(name)));
        var nullError = apply(initial, new SetProperty(tile.id(), p("onActiveThumbImageError"), nil()));
        assertTrue(source(nullError).contains("onActiveThumbImageError: null"));
        var image = apply(initial, new PatchProperties(tile.id(), List.of(
                new PatchProperties.SetPatch(p("activeThumbImage"), PropertyValue.ImageProviderValue.asset("assets/icon.png")),
                new PatchProperties.SetPatch(p("onActiveThumbImageError"), reference("imageError")))));
        assertRejected(image, new ResetProperty(tile.id(), p("activeThumbImage")));
        var cleared = apply(image, new PatchProperties(tile.id(), List.of(new PatchProperties.ResetPatch(p("activeThumbImage")),
                new PatchProperties.ResetPatch(p("onActiveThumbImageError")))));
        assertExact(initial, cleared);
        assertExact(image, cleared.undo().session());
    }

    @Test
    void everyEventCreatesTypedUserOwnedSourceAndNullDisconnectionRetainsItsBody() throws Exception {
        for (var kind : WidgetClassKind.values()) for (var event : WidgetEventCatalog.eventsFor(definition())) {
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            for (var companion : event.requiredCompanionProperties()) values.put(companion, PropertyValue.ImageProviderValue.asset("assets/icon.png"));
            var tile = node(values);
            var initial = open(tile, kind);
            var created = apply(initial, new CreateEventHandler(tile.id(), event.propertyName(), "_tileEvent"));
            assertTrue(source(created).contains(event.signature().declaration("_tileEvent")));
            assertTrue(source(created).contains(event.propertyName().value() + ": _tileEvent"));
            assertTrue(source(created).contains("// Preserved user source"));
            assertExact(initial, created.undo().session());
            assertExact(created, created.undo().session().redo().session());
            assertExact(created, reopen(created));
            var disconnected = apply(created, new SetProperty(tile.id(), event.propertyName(), nil()));
            assertTrue(source(disconnected).contains(event.signature().declaration("_tileEvent")));
            assertTrue(source(disconnected).contains(event.propertyName().value() + ": null"));
            assertExact(created, disconnected.undo().session());
        }
    }

    @Test
    void boolProducerAndAllSixConsumersRetainPreviewHistoryAndInactiveAdaptiveBinding() throws Exception {
        var tile = node(Map.of(p("applyCupertinoTheme"), b(false), p("selected"), b(false)));
        var initial = open(tile, WidgetClassKind.STATEFUL);
        var controlled = apply(initial, new CreateStateBinding(tile.id(), "_value", "_valueChanged"));
        assertTrue(source(controlled).contains("bool _value = false"));
        assertTrue(source(controlled).contains("void _valueChanged(bool value)"));
        assertTrue(source(controlled).contains("value: _value"));
        assertExact(initial, controlled.undo().session());
        assertExact(controlled, reopen(controlled));
        for (String name : List.of("dense", "autofocus", "enableFeedback", "selected", "internalAddSemanticForOnTap", "applyCupertinoTheme")) {
            for (var binding : List.of(new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.DIRECT),
                    new StatePropertyBinding("_flag", StateBinding.Type.BOOL, Optional.empty(), StatePropertyBinding.Transform.NOT),
                    new StatePropertyBinding("_choice", StateBinding.Type.NULLABLE_INT, Optional.empty(), StatePropertyBinding.Transform.EQUALS,
                            Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.ONE))))) {
                var bound = apply(controlled, new BindPropertyToState(tile.id(), p(name), binding));
                assertEquals(binding, bound.current().document().root().propertyBindings().get(p(name)));
                assertEquals(controlled.current().document().root().properties(), bound.current().document().root().properties());
                assertEquals(!name.equals("applyCupertinoTheme"), source(bound).contains(name + ":"));
                assertExact(controlled, bound.undo().session());
                assertExact(bound, reopen(bound));
                if (name.equals("applyCupertinoTheme")) {
                    var adaptive = apply(bound, new SetProperty(tile.id(), p("variant"), s("adaptive")));
                    assertTrue(source(adaptive).contains("applyCupertinoTheme:"));
                    assertEquals(binding, adaptive.current().document().root().propertyBindings().get(p(name)));
                    assertExact(bound, adaptive.undo().session());
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
        var id = StableId.random();
        var provisional = new DesignerDocument(id, descriptor("0".repeat(64), "0".repeat(64), kind), root);
        var result = new DartRegionGenerator().generate(provisional, CATALOG);
        assertTrue(result.successful(), result.diagnostics().toString()); var generated = result.generated().orElseThrow();
        var document = new DesignerDocument(id, descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256(), kind), root);
        String owner = kind == WidgetClassKind.STATELESS ? "class Sample extends StatelessWidget {\n"
                : "class Sample extends StatefulWidget {\n  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\nclass FormLogic extends State<Sample> {\n";
        byte[] source = ("// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\n" + owner + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n  // Preserved user source\n  bool _flag = true;\n  int? _choice = null;\n}\n").getBytes(StandardCharsets.UTF_8);
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document), source, CATALOG);
        assertTrue(opened.ready(), opened.diagnostics().toString()); return opened.session().orElseThrow();
    }
    private static DartSourceDescriptor descriptor(String imports, String build, WidgetClassKind kind) {
        return new DartSourceDescriptor("sample.dart", "Sample", kind, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
